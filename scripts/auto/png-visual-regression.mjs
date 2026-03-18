import { readFile, writeFile } from 'node:fs/promises';
import { deflateSync, inflateSync } from 'node:zlib';

const PNG_SIGNATURE = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);

let crcTable;

function getCrcTable() {
  if (crcTable) {
    return crcTable;
  }

  crcTable = new Uint32Array(256);
  for (let index = 0; index < 256; index += 1) {
    let value = index;
    for (let bit = 0; bit < 8; bit += 1) {
      value = (value & 1) === 1 ? 0xedb88320 ^ (value >>> 1) : value >>> 1;
    }
    crcTable[index] = value >>> 0;
  }
  return crcTable;
}

function crc32(buffer) {
  let value = 0xffffffff;
  const table = getCrcTable();
  for (let index = 0; index < buffer.length; index += 1) {
    value = table[(value ^ buffer[index]) & 0xff] ^ (value >>> 8);
  }
  return (value ^ 0xffffffff) >>> 0;
}

function paethPredictor(left, up, upperLeft) {
  const prediction = left + up - upperLeft;
  const distanceLeft = Math.abs(prediction - left);
  const distanceUp = Math.abs(prediction - up);
  const distanceUpperLeft = Math.abs(prediction - upperLeft);

  if (distanceLeft <= distanceUp && distanceLeft <= distanceUpperLeft) {
    return left;
  }
  if (distanceUp <= distanceUpperLeft) {
    return up;
  }
  return upperLeft;
}

function bytesPerPixel(colorType) {
  switch (colorType) {
    case 0:
      return 1;
    case 2:
      return 3;
    case 4:
      return 2;
    case 6:
      return 4;
    default:
      throw new Error(`Unsupported PNG color type: ${colorType}`);
  }
}

function convertToRgba(raw, width, height, colorType) {
  const rgba = Buffer.alloc(width * height * 4);
  const sourcePixelBytes = bytesPerPixel(colorType);

  for (let pixelIndex = 0; pixelIndex < width * height; pixelIndex += 1) {
    const sourceOffset = pixelIndex * sourcePixelBytes;
    const targetOffset = pixelIndex * 4;

    switch (colorType) {
      case 0: {
        const gray = raw[sourceOffset];
        rgba[targetOffset] = gray;
        rgba[targetOffset + 1] = gray;
        rgba[targetOffset + 2] = gray;
        rgba[targetOffset + 3] = 255;
        break;
      }
      case 2:
        rgba[targetOffset] = raw[sourceOffset];
        rgba[targetOffset + 1] = raw[sourceOffset + 1];
        rgba[targetOffset + 2] = raw[sourceOffset + 2];
        rgba[targetOffset + 3] = 255;
        break;
      case 4: {
        const gray = raw[sourceOffset];
        rgba[targetOffset] = gray;
        rgba[targetOffset + 1] = gray;
        rgba[targetOffset + 2] = gray;
        rgba[targetOffset + 3] = raw[sourceOffset + 1];
        break;
      }
      case 6:
        rgba[targetOffset] = raw[sourceOffset];
        rgba[targetOffset + 1] = raw[sourceOffset + 1];
        rgba[targetOffset + 2] = raw[sourceOffset + 2];
        rgba[targetOffset + 3] = raw[sourceOffset + 3];
        break;
      default:
        throw new Error(`Unsupported PNG color type: ${colorType}`);
    }
  }

  return rgba;
}

function parsePng(buffer) {
  if (buffer.subarray(0, PNG_SIGNATURE.length).compare(PNG_SIGNATURE) !== 0) {
    throw new Error('Invalid PNG signature.');
  }

  let offset = PNG_SIGNATURE.length;
  let width = 0;
  let height = 0;
  let bitDepth = 0;
  let colorType = 0;
  let interlaceMethod = 0;
  const idatChunks = [];

  while (offset < buffer.length) {
    const length = buffer.readUInt32BE(offset);
    const type = buffer.toString('ascii', offset + 4, offset + 8);
    const dataStart = offset + 8;
    const dataEnd = dataStart + length;
    const data = buffer.subarray(dataStart, dataEnd);

    if (type === 'IHDR') {
      width = data.readUInt32BE(0);
      height = data.readUInt32BE(4);
      bitDepth = data[8];
      colorType = data[9];
      interlaceMethod = data[12];
    } else if (type === 'IDAT') {
      idatChunks.push(data);
    } else if (type === 'IEND') {
      break;
    }

    offset = dataEnd + 4;
  }

  if (!width || !height) {
    throw new Error('PNG is missing IHDR width/height.');
  }
  if (bitDepth !== 8) {
    throw new Error(`Unsupported PNG bit depth: ${bitDepth}.`);
  }
  if (interlaceMethod !== 0) {
    throw new Error('Unsupported interlaced PNG.');
  }

  const pixelBytes = bytesPerPixel(colorType);
  const inflated = inflateSync(Buffer.concat(idatChunks));
  const stride = width * pixelBytes;
  const raw = Buffer.alloc(stride * height);
  let previousRow = null;

  for (let rowIndex = 0; rowIndex < height; rowIndex += 1) {
    const sourceRowOffset = rowIndex * (stride + 1);
    const filterType = inflated[sourceRowOffset];
    const source = inflated.subarray(sourceRowOffset + 1, sourceRowOffset + 1 + stride);
    const target = Buffer.alloc(stride);

    for (let column = 0; column < stride; column += 1) {
      const left = column >= pixelBytes ? target[column - pixelBytes] : 0;
      const up = previousRow ? previousRow[column] : 0;
      const upperLeft = previousRow && column >= pixelBytes ? previousRow[column - pixelBytes] : 0;

      switch (filterType) {
        case 0:
          target[column] = source[column];
          break;
        case 1:
          target[column] = (source[column] + left) & 0xff;
          break;
        case 2:
          target[column] = (source[column] + up) & 0xff;
          break;
        case 3:
          target[column] = (source[column] + Math.floor((left + up) / 2)) & 0xff;
          break;
        case 4:
          target[column] = (source[column] + paethPredictor(left, up, upperLeft)) & 0xff;
          break;
        default:
          throw new Error(`Unsupported PNG filter type: ${filterType}.`);
      }
    }

    target.copy(raw, rowIndex * stride);
    previousRow = target;
  }

  return {
    width,
    height,
    rgba: convertToRgba(raw, width, height, colorType)
  };
}

function createChunk(type, data = Buffer.alloc(0)) {
  const typeBuffer = Buffer.from(type, 'ascii');
  const lengthBuffer = Buffer.alloc(4);
  lengthBuffer.writeUInt32BE(data.length, 0);

  const crcBuffer = Buffer.alloc(4);
  crcBuffer.writeUInt32BE(crc32(Buffer.concat([typeBuffer, data])), 0);

  return Buffer.concat([lengthBuffer, typeBuffer, data, crcBuffer]);
}

function encodePng({ width, height, rgba }) {
  const stride = width * 4;
  const raw = Buffer.alloc((stride + 1) * height);

  for (let rowIndex = 0; rowIndex < height; rowIndex += 1) {
    const rowOffset = rowIndex * (stride + 1);
    raw[rowOffset] = 0;
    rgba.copy(raw, rowOffset + 1, rowIndex * stride, (rowIndex + 1) * stride);
  }

  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;
  ihdr[9] = 6;
  ihdr[10] = 0;
  ihdr[11] = 0;
  ihdr[12] = 0;

  return Buffer.concat([
    PNG_SIGNATURE,
    createChunk('IHDR', ihdr),
    createChunk('IDAT', deflateSync(raw)),
    createChunk('IEND')
  ]);
}

function createDiffImage(baseline, actual) {
  const totalPixels = baseline.width * baseline.height;
  const diff = Buffer.alloc(totalPixels * 4);
  let mismatchPixels = 0;

  for (let pixelIndex = 0; pixelIndex < totalPixels; pixelIndex += 1) {
    const offset = pixelIndex * 4;
    const dr = Math.abs(baseline.rgba[offset] - actual.rgba[offset]);
    const dg = Math.abs(baseline.rgba[offset + 1] - actual.rgba[offset + 1]);
    const db = Math.abs(baseline.rgba[offset + 2] - actual.rgba[offset + 2]);
    const da = Math.abs(baseline.rgba[offset + 3] - actual.rgba[offset + 3]);
    const mismatch = dr > 0 || dg > 0 || db > 0 || da > 0;

    if (mismatch) {
      mismatchPixels += 1;
      diff[offset] = 255;
      diff[offset + 1] = 0;
      diff[offset + 2] = 255;
      diff[offset + 3] = 255;
      continue;
    }

    const average = Math.round(
      (actual.rgba[offset] + actual.rgba[offset + 1] + actual.rgba[offset + 2]) / 3
    );
    const softened = Math.min(255, average + 80);
    diff[offset] = softened;
    diff[offset + 1] = softened;
    diff[offset + 2] = softened;
    diff[offset + 3] = 255;
  }

  return {
    mismatchPixels,
    diffPngBuffer: encodePng({
      width: baseline.width,
      height: baseline.height,
      rgba: diff
    })
  };
}

export async function comparePngFiles({
  baselinePath,
  actualPath,
  diffPath,
  maxDiffRatio = 0
}) {
  const [baselineBuffer, actualBuffer] = await Promise.all([readFile(baselinePath), readFile(actualPath)]);
  const baseline = parsePng(baselineBuffer);
  const actual = parsePng(actualBuffer);

  if (baseline.width !== actual.width || baseline.height !== actual.height) {
    return {
      pass: false,
      baselineWidth: baseline.width,
      baselineHeight: baseline.height,
      actualWidth: actual.width,
      actualHeight: actual.height,
      totalPixels: Math.max(baseline.width * baseline.height, actual.width * actual.height),
      mismatchPixels: Math.max(baseline.width * baseline.height, actual.width * actual.height),
      mismatchRatio: 1,
      reason: 'dimension_mismatch'
    };
  }

  const { mismatchPixels, diffPngBuffer } = createDiffImage(baseline, actual);
  const totalPixels = baseline.width * baseline.height;
  const mismatchRatio = totalPixels > 0 ? mismatchPixels / totalPixels : 0;

  if (diffPath && mismatchPixels > 0) {
    await writeFile(diffPath, diffPngBuffer);
  }

  return {
    pass: mismatchRatio <= maxDiffRatio,
    baselineWidth: baseline.width,
    baselineHeight: baseline.height,
    actualWidth: actual.width,
    actualHeight: actual.height,
    totalPixels,
    mismatchPixels,
    mismatchRatio,
    reason: mismatchPixels > 0 ? 'pixel_mismatch' : 'matched'
  };
}
