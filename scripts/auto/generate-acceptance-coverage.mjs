import fs from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

import {
  buildCoverageMatrix,
  evaluateCoveragePolicy,
  renderCoverageMarkdown
} from './acceptance-coverage-lib.mjs';

const DEFAULT_REGISTRY_PATH = 'config/automation/acceptance-registry.json';
const DEFAULT_PACKAGES_PATH = 'config/automation/business-acceptance-packages.json';

function parseCoverageArgs(argv) {
  const options = {
    failOnGaps: false
  };

  argv.forEach((arg) => {
    if (arg === '--fail-on-gaps') {
      options.failOnGaps = true;
      return;
    }
    if (arg.startsWith('--registry-path=')) {
      options.registryPath = arg.slice('--registry-path='.length).trim();
      return;
    }
    if (arg.startsWith('--packages-path=')) {
      options.packagesPath = arg.slice('--packages-path='.length).trim();
      return;
    }
    if (arg.startsWith('--policy-path=')) {
      options.policyPath = arg.slice('--policy-path='.length).trim();
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
  const parts = [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, '0'),
    String(now.getDate()).padStart(2, '0'),
    String(now.getHours()).padStart(2, '0'),
    String(now.getMinutes()).padStart(2, '0'),
    String(now.getSeconds()).padStart(2, '0')
  ];
  return parts.join('');
}

async function readJsonFile(filePath) {
  return JSON.parse(await fs.readFile(filePath, 'utf8'));
}

async function writeCoverageArtifacts({ workspaceRoot, options, matrix }) {
  const timestamp = createTimestamp();
  const defaultJsonOut = path.join(
    workspaceRoot,
    'logs',
    'acceptance',
    `acceptance-coverage-${timestamp}.json`
  );
  const defaultMdOut = path.join(
    workspaceRoot,
    'logs',
    'acceptance',
    `acceptance-coverage-${timestamp}.md`
  );
  const jsonPath = resolveWorkspacePath(workspaceRoot, options.jsonOut) || defaultJsonOut;
  const mdPath = resolveWorkspacePath(workspaceRoot, options.mdOut) || defaultMdOut;

  await fs.mkdir(path.dirname(jsonPath), { recursive: true });
  await fs.mkdir(path.dirname(mdPath), { recursive: true });
  await fs.writeFile(jsonPath, JSON.stringify(matrix, null, 2), 'utf8');
  await fs.writeFile(mdPath, renderCoverageMarkdown(matrix), 'utf8');

  return {
    jsonPath,
    mdPath
  };
}

export async function runCoverageCli({
  argv = process.argv.slice(2),
  workspaceRoot = process.cwd()
} = {}) {
  const options = parseCoverageArgs(argv);
  const registryPath =
    resolveWorkspacePath(workspaceRoot, options.registryPath) ||
    path.resolve(workspaceRoot, DEFAULT_REGISTRY_PATH);
  const packagesPath =
    resolveWorkspacePath(workspaceRoot, options.packagesPath) ||
    path.resolve(workspaceRoot, DEFAULT_PACKAGES_PATH);
  const registry = await readJsonFile(registryPath);
  const packages = await readJsonFile(packagesPath);
  const matrix = buildCoverageMatrix({ registry, packages });
  if (options.policyPath) {
    const policyPath = resolveWorkspacePath(workspaceRoot, options.policyPath);
    const policy = await readJsonFile(policyPath);
    matrix.policyEvaluation = evaluateCoveragePolicy(matrix, policy);
  }
  const artifacts = await writeCoverageArtifacts({
    workspaceRoot,
    options,
    matrix
  });
  const policyHasErrors = (matrix.policyEvaluation?.summary?.errors || 0) > 0;
  const exitCode =
    (options.failOnGaps && matrix.summary.hasGaps) || policyHasErrors ? 1 : 0;

  return {
    exitCode,
    summary: matrix.summary,
    jsonPath: artifacts.jsonPath,
    markdownPath: artifacts.mdPath,
    matrix
  };
}

const currentFilePath = fileURLToPath(import.meta.url);

if (
  process.argv[1] &&
  path.resolve(process.argv[1]) === path.resolve(currentFilePath)
) {
  try {
    const result = await runCoverageCli();
    console.log(
      JSON.stringify(
        {
          exitCode: result.exitCode,
          summary: result.summary,
          policyEvaluation: result.matrix.policyEvaluation?.summary,
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
