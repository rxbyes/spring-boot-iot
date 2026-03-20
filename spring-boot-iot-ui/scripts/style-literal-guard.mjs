import { promises as fs } from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const workspaceRoot = process.cwd();
const sourceRoot = path.join(workspaceRoot, 'src');
const baselinePath = path.join(workspaceRoot, 'scripts', 'style-literal-baseline.json');

const targetExtensions = new Set(['.vue', '.css', '.scss']);
const ignoredFileSuffixes = ['tokens.css', 'element-overrides.css'];

const colorDeclPattern = /^[\t ]*[a-zA-Z-]+\s*:\s*(#[0-9a-fA-F]{3,8}|rgba?\(|hsla?\()/;
const radiusDeclPattern = /border-radius:\s*[0-9]+px/;
const shadowDeclPattern = /box-shadow:\s*[^;]*rgba?\(/;

const forbiddenBrandPatterns = [
  /#1677ff/i,
  /#4096ff/i,
  /#ff6a00/i,
  /#ff8833/i,
  /rgba\(\s*22\s*,\s*119\s*,\s*255/i,
  /rgba\(\s*255\s*,\s*106\s*,\s*0/i
];

function isIgnoredFile(filePath) {
  return ignoredFileSuffixes.some((suffix) => filePath.endsWith(suffix));
}

async function collectFiles(dir) {
  const entries = await fs.readdir(dir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await collectFiles(fullPath)));
      continue;
    }
    const extension = path.extname(entry.name);
    if (!targetExtensions.has(extension)) {
      continue;
    }
    if (isIgnoredFile(fullPath)) {
      continue;
    }
    files.push(fullPath);
  }
  return files;
}

function scanStyleLines(filePath, fileContent, onLine) {
  const lines = fileContent.split(/\r?\n/);
  const isVue = filePath.endsWith('.vue');
  let inStyleBlock = !isVue;

  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i];
    if (isVue && /^\s*<style(\s|>)/.test(line)) {
      inStyleBlock = true;
      continue;
    }
    if (!inStyleBlock) {
      continue;
    }

    onLine(line, i + 1);

    if (isVue && /^\s*<\/style>/.test(line)) {
      inStyleBlock = false;
    }
  }
}

function toTopFiles(fileMetricMap, topN = 10) {
  return Object.entries(fileMetricMap)
    .sort((a, b) => b[1] - a[1])
    .slice(0, topN)
    .map(([file, count]) => ({ file, count }));
}

async function scanCurrentMetrics() {
  const files = await collectFiles(sourceRoot);

  let colorDeclCount = 0;
  let radiusDeclCount = 0;
  let shadowDeclCount = 0;
  const colorDeclByFile = {};
  const radiusDeclByFile = {};
  const shadowDeclByFile = {};
  const forbiddenHits = [];

  for (const file of files) {
    const content = await fs.readFile(file, 'utf8');
    scanStyleLines(file, content, (line, lineNumber) => {
      if (colorDeclPattern.test(line)) {
        colorDeclCount += 1;
        colorDeclByFile[file] = (colorDeclByFile[file] ?? 0) + 1;
      }
      if (radiusDeclPattern.test(line)) {
        radiusDeclCount += 1;
        radiusDeclByFile[file] = (radiusDeclByFile[file] ?? 0) + 1;
      }
      if (shadowDeclPattern.test(line)) {
        shadowDeclCount += 1;
        shadowDeclByFile[file] = (shadowDeclByFile[file] ?? 0) + 1;
      }

      for (const pattern of forbiddenBrandPatterns) {
        if (pattern.test(line)) {
          forbiddenHits.push({
            file,
            lineNumber,
            line: line.trim()
          });
          break;
        }
      }
    });
  }

  return {
    generatedAt: new Date().toISOString(),
    colorDeclCount,
    radiusDeclCount,
    shadowDeclCount,
    topColorFiles: toTopFiles(colorDeclByFile),
    topRadiusFiles: toTopFiles(radiusDeclByFile),
    topShadowFiles: toTopFiles(shadowDeclByFile),
    forbiddenHits
  };
}

function printSummary(summary, title) {
  console.log(`\n${title}`);
  console.log(`colorDeclCount: ${summary.colorDeclCount}`);
  console.log(`radiusDeclCount: ${summary.radiusDeclCount}`);
  console.log(`shadowDeclCount: ${summary.shadowDeclCount}`);

  const printTop = (label, list) => {
    console.log(`\n${label}:`);
    if (!list.length) {
      console.log('- none');
      return;
    }
    for (const item of list) {
      console.log(`- ${item.count}\t${path.relative(workspaceRoot, item.file)}`);
    }
  };

  printTop('topColorFiles', summary.topColorFiles);
  printTop('topRadiusFiles', summary.topRadiusFiles);
  printTop('topShadowFiles', summary.topShadowFiles);
}

async function loadBaseline() {
  const raw = await fs.readFile(baselinePath, 'utf8');
  return JSON.parse(raw);
}

function compareToBaseline(current, baseline) {
  const errors = [];
  if (current.colorDeclCount > baseline.colorDeclCount) {
    errors.push(
      `colorDeclCount regressed: current=${current.colorDeclCount}, baseline=${baseline.colorDeclCount}`
    );
  }
  if (current.radiusDeclCount > baseline.radiusDeclCount) {
    errors.push(
      `radiusDeclCount regressed: current=${current.radiusDeclCount}, baseline=${baseline.radiusDeclCount}`
    );
  }
  if (current.shadowDeclCount > baseline.shadowDeclCount) {
    errors.push(
      `shadowDeclCount regressed: current=${current.shadowDeclCount}, baseline=${baseline.shadowDeclCount}`
    );
  }
  if (current.forbiddenHits.length > 0) {
    errors.push(`forbidden brand literals detected: ${current.forbiddenHits.length}`);
  }
  return errors;
}

async function main() {
  const updateBaseline = process.argv.includes('--update-baseline');
  const current = await scanCurrentMetrics();

  if (updateBaseline) {
    const baselinePayload = {
      generatedAt: current.generatedAt,
      colorDeclCount: current.colorDeclCount,
      radiusDeclCount: current.radiusDeclCount,
      shadowDeclCount: current.shadowDeclCount
    };
    await fs.writeFile(baselinePath, `${JSON.stringify(baselinePayload, null, 2)}\n`, 'utf8');
    printSummary(current, 'Updated style literal baseline');
    return;
  }

  const baseline = await loadBaseline();
  printSummary(current, 'Current style literal metrics');
  console.log('\nBaseline metrics:');
  console.log(`colorDeclCount: ${baseline.colorDeclCount}`);
  console.log(`radiusDeclCount: ${baseline.radiusDeclCount}`);
  console.log(`shadowDeclCount: ${baseline.shadowDeclCount}`);

  if (current.forbiddenHits.length > 0) {
    console.log('\nForbidden brand literal hits:');
    for (const hit of current.forbiddenHits.slice(0, 20)) {
      console.log(`- ${path.relative(workspaceRoot, hit.file)}:${hit.lineNumber} ${hit.line}`);
    }
  }

  const errors = compareToBaseline(current, baseline);
  if (errors.length > 0) {
    console.error('\nStyle guard failed:');
    for (const error of errors) {
      console.error(`- ${error}`);
    }
    process.exitCode = 1;
    return;
  }

  console.log('\nStyle guard passed.');
}

main().catch((error) => {
  console.error('style-literal-guard failed with error:');
  console.error(error);
  process.exitCode = 1;
});
