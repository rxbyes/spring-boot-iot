import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

import {
  buildAutomationResultArchiveIndex,
  writeAutomationResultArchiveArtifacts
} from './automation-result-archive-index-lib.mjs';

function cleanText(value) {
  return String(value || '').trim();
}

function parseArgs(argv) {
  const options = {};

  argv.forEach((arg) => {
    if (arg.startsWith('--results-dir=')) {
      options.resultsDir = arg.slice('--results-dir='.length).trim();
      return;
    }
    if (arg.startsWith('--output-dir=')) {
      options.outputDir = arg.slice('--output-dir='.length).trim();
      return;
    }
    if (arg.startsWith('--timestamp=')) {
      options.timestamp = arg.slice('--timestamp='.length).trim();
      return;
    }
    throw new Error(`Unknown argument: ${arg}`);
  });

  return options;
}

function resolveWorkspacePath(workspaceRoot, value, fallback) {
  const candidate = cleanText(value) || fallback;
  return path.isAbsolute(candidate)
    ? candidate
    : path.resolve(workspaceRoot, candidate);
}

export async function runAutomationResultArchiveIndexCli({
  workspaceRoot = process.cwd(),
  argv = process.argv.slice(2)
} = {}) {
  const options = parseArgs(argv);
  const resultsDir = resolveWorkspacePath(
    workspaceRoot,
    options.resultsDir,
    path.join('logs', 'acceptance')
  );
  const outputDir = resolveWorkspacePath(
    workspaceRoot,
    options.outputDir,
    path.join('logs', 'acceptance')
  );
  const index = await buildAutomationResultArchiveIndex({
    workspaceRoot,
    resultsDir
  });
  const artifacts = await writeAutomationResultArchiveArtifacts({
    outputDir,
    index,
    timestamp: options.timestamp
  });

  return {
    index,
    ...artifacts
  };
}

const modulePath = fileURLToPath(import.meta.url);
if (process.argv[1] && path.resolve(process.argv[1]) === modulePath) {
  runAutomationResultArchiveIndexCli()
    .then((result) => {
      process.stdout.write(
        `${JSON.stringify(
          {
            latestJsonPath: result.latestJsonPath,
            jsonPath: result.jsonPath,
            markdownPath: result.markdownPath,
            indexedRuns: result.index.runs.length,
            skippedFiles: result.index.skippedFiles.length
          },
          null,
          2
        )}\n`
      );
    })
    .catch((error) => {
      process.stderr.write(`${error instanceof Error ? error.message : String(error)}\n`);
      process.exitCode = 1;
    });
}
