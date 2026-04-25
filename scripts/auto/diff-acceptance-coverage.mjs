import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

import {
  buildCoverageDiff,
  findLatestCoverageMatrixFiles,
  readCoverageMatrix,
  writeCoverageDiffArtifacts
} from './acceptance-coverage-diff-lib.mjs';

function parseDiffArgs(argv) {
  const options = {};
  argv.forEach((arg) => {
    if (arg.startsWith('--baseline-path=')) {
      options.baselinePath = arg.slice('--baseline-path='.length).trim();
      return;
    }
    if (arg.startsWith('--current-path=')) {
      options.currentPath = arg.slice('--current-path='.length).trim();
      return;
    }
    if (arg.startsWith('--json-out=')) {
      options.jsonOut = arg.slice('--json-out='.length).trim();
      return;
    }
    if (arg.startsWith('--md-out=')) {
      options.mdOut = arg.slice('--md-out='.length).trim();
      return;
    }
    throw new Error(`Unknown argument: ${arg}`);
  });
  return options;
}

function resolveWorkspacePath(workspaceRoot, value) {
  if (!value) {
    return '';
  }
  return path.isAbsolute(value) ? value : path.resolve(workspaceRoot, value);
}

function createTimestamp() {
  const now = new Date();
  return [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, '0'),
    String(now.getDate()).padStart(2, '0'),
    String(now.getHours()).padStart(2, '0'),
    String(now.getMinutes()).padStart(2, '0'),
    String(now.getSeconds()).padStart(2, '0')
  ].join('');
}

async function resolveInputPaths({ workspaceRoot, options }) {
  const baselinePath = resolveWorkspacePath(workspaceRoot, options.baselinePath);
  const currentPath = resolveWorkspacePath(workspaceRoot, options.currentPath);
  if (baselinePath || currentPath) {
    if (!baselinePath || !currentPath) {
      throw new Error(
        'Both --baseline-path and --current-path are required when either is provided.'
      );
    }
    if (path.resolve(baselinePath) === path.resolve(currentPath)) {
      throw new Error('Coverage diff baseline and current paths must be different.');
    }
    return { baselinePath, currentPath };
  }
  return findLatestCoverageMatrixFiles({ workspaceRoot });
}

export async function runCoverageDiffCli({
  argv = process.argv.slice(2),
  workspaceRoot = process.cwd()
} = {}) {
  const options = parseDiffArgs(argv);
  const { baselinePath, currentPath } = await resolveInputPaths({
    workspaceRoot,
    options
  });
  const baseline = await readCoverageMatrix(baselinePath);
  const current = await readCoverageMatrix(currentPath);
  const diff = buildCoverageDiff({
    baseline,
    current,
    baselinePath,
    currentPath
  });
  const timestamp = createTimestamp();
  const jsonPath =
    resolveWorkspacePath(workspaceRoot, options.jsonOut) ||
    path.join(
      workspaceRoot,
      'logs',
      'acceptance',
      `acceptance-coverage-diff-${timestamp}.json`
    );
  const markdownPath =
    resolveWorkspacePath(workspaceRoot, options.mdOut) ||
    path.join(
      workspaceRoot,
      'logs',
      'acceptance',
      `acceptance-coverage-diff-${timestamp}.md`
    );
  await writeCoverageDiffArtifacts({ diff, jsonPath, markdownPath });
  return {
    exitCode: 0,
    jsonPath,
    markdownPath,
    diff
  };
}

const currentFilePath = fileURLToPath(import.meta.url);

if (
  process.argv[1] &&
  path.resolve(process.argv[1]) === path.resolve(currentFilePath)
) {
  try {
    const result = await runCoverageDiffCli();
    console.log(
      JSON.stringify(
        {
          exitCode: result.exitCode,
          summary: result.diff.summary,
          jsonPath: result.jsonPath,
          markdownPath: result.markdownPath
        },
        null,
        2
      )
    );
    process.exitCode = result.exitCode;
  } catch (error) {
    console.error(error?.stack || error);
    process.exitCode = 1;
  }
}
