import fs from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

import {
  evaluateCoveragePolicy,
  renderCoverageMarkdown
} from './acceptance-coverage-lib.mjs';
import {
  runCoverageDiffCli
} from './diff-acceptance-coverage.mjs';
import {
  findLatestCoverageMatrixFiles,
  readCoverageMatrix
} from './acceptance-coverage-diff-lib.mjs';
import { runCoverageCli } from './generate-acceptance-coverage.mjs';
import {
  buildAcceptanceReadinessReport,
  writeAcceptanceReadinessArtifacts
} from './acceptance-readiness-lib.mjs';

function parseReadinessArgs(argv) {
  const options = {
    skipDiff: false
  };

  argv.forEach((arg) => {
    if (arg === '--skip-diff') {
      options.skipDiff = true;
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
    if (arg.startsWith('--coverage-policy-path=')) {
      options.coveragePolicyPath = arg
        .slice('--coverage-policy-path='.length)
        .trim();
      return;
    }
    if (arg.startsWith('--baseline-coverage-path=')) {
      options.baselineCoveragePath = arg
        .slice('--baseline-coverage-path='.length)
        .trim();
      return;
    }
    if (arg.startsWith('--current-coverage-path=')) {
      options.currentCoveragePath = arg
        .slice('--current-coverage-path='.length)
        .trim();
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

  if (
    options.skipDiff &&
    (options.baselineCoveragePath || options.currentCoveragePath)
  ) {
    throw new Error(
      'Cannot combine --skip-diff with explicit baseline or current coverage paths.'
    );
  }
  if (options.currentCoveragePath && !options.baselineCoveragePath) {
    throw new Error(
      '--current-coverage-path requires --baseline-coverage-path.'
    );
  }

  return options;
}

function resolveWorkspacePath(workspaceRoot, value) {
  if (!value) {
    return '';
  }
  return path.isAbsolute(value) ? value : path.resolve(workspaceRoot, value);
}

function createTimestamp(value = '') {
  if (value) {
    return value;
  }
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

async function readJsonFile(filePath) {
  return JSON.parse(await fs.readFile(filePath, 'utf8'));
}

function resolveOutputDir({ workspaceRoot, options }) {
  return (
    resolveWorkspacePath(workspaceRoot, options.outputDir) ||
    path.join(workspaceRoot, 'logs', 'acceptance')
  );
}

function createArtifactPaths({ outputDir, timestamp }) {
  return {
    coverageJsonPath: path.join(
      outputDir,
      `acceptance-coverage-${timestamp}.json`
    ),
    coverageMarkdownPath: path.join(
      outputDir,
      `acceptance-coverage-${timestamp}.md`
    ),
    diffJsonPath: path.join(outputDir, `acceptance-coverage-diff-${timestamp}.json`),
    diffMarkdownPath: path.join(
      outputDir,
      `acceptance-coverage-diff-${timestamp}.md`
    ),
    readinessJsonPath: path.join(
      outputDir,
      `acceptance-readiness-${timestamp}.json`
    ),
    readinessMarkdownPath: path.join(
      outputDir,
      `acceptance-readiness-${timestamp}.md`
    )
  };
}

async function loadCoverageSnapshotFromCurrent({
  workspaceRoot,
  options,
  artifactPaths
}) {
  const currentCoveragePath = resolveWorkspacePath(
    workspaceRoot,
    options.currentCoveragePath
  );
  const matrix = await readCoverageMatrix(currentCoveragePath);

  if (options.coveragePolicyPath) {
    const policy = await readJsonFile(
      resolveWorkspacePath(workspaceRoot, options.coveragePolicyPath)
    );
    matrix.policyEvaluation = evaluateCoveragePolicy(matrix, policy);
  }

  await fs.mkdir(path.dirname(artifactPaths.coverageJsonPath), { recursive: true });
  await fs.writeFile(
    artifactPaths.coverageJsonPath,
    JSON.stringify(matrix, null, 2),
    'utf8'
  );
  await fs.writeFile(
    artifactPaths.coverageMarkdownPath,
    renderCoverageMarkdown(matrix),
    'utf8'
  );

  return {
    matrix,
    jsonPath: artifactPaths.coverageJsonPath,
    markdownPath: artifactPaths.coverageMarkdownPath,
    diffCurrentPath: currentCoveragePath
  };
}

async function generateCoverageSnapshot({
  workspaceRoot,
  options,
  artifactPaths
}) {
  const argv = [
    `--json-out=${artifactPaths.coverageJsonPath}`,
    `--md-out=${artifactPaths.coverageMarkdownPath}`
  ];

  if (options.registryPath) {
    argv.push(
      `--registry-path=${resolveWorkspacePath(workspaceRoot, options.registryPath)}`
    );
  }
  if (options.packagesPath) {
    argv.push(
      `--packages-path=${resolveWorkspacePath(workspaceRoot, options.packagesPath)}`
    );
  }
  if (options.coveragePolicyPath) {
    argv.push(
      `--policy-path=${resolveWorkspacePath(
        workspaceRoot,
        options.coveragePolicyPath
      )}`
    );
  }

  const result = await runCoverageCli({ workspaceRoot, argv });
  return {
    matrix: result.matrix,
    jsonPath: result.jsonPath,
    markdownPath: result.markdownPath,
    diffCurrentPath: result.jsonPath
  };
}

async function resolveCoverageSnapshot({ workspaceRoot, options, artifactPaths }) {
  if (options.currentCoveragePath) {
    return loadCoverageSnapshotFromCurrent({
      workspaceRoot,
      options,
      artifactPaths
    });
  }
  return generateCoverageSnapshot({ workspaceRoot, options, artifactPaths });
}

async function resolveDiffSnapshot({
  workspaceRoot,
  options,
  artifactPaths,
  currentCoveragePath
}) {
  if (options.skipDiff) {
    return {
      skipped: true,
      skippedReason: 'Coverage diff skipped by option.'
    };
  }

  const explicitBaselinePath = resolveWorkspacePath(
    workspaceRoot,
    options.baselineCoveragePath
  );
  if (explicitBaselinePath) {
    const explicitCurrentPath =
      resolveWorkspacePath(workspaceRoot, options.currentCoveragePath) ||
      currentCoveragePath;
    const result = await runCoverageDiffCli({
      workspaceRoot,
      argv: [
        `--baseline-path=${explicitBaselinePath}`,
        `--current-path=${explicitCurrentPath}`,
        `--json-out=${artifactPaths.diffJsonPath}`,
        `--md-out=${artifactPaths.diffMarkdownPath}`
      ]
    });
    return {
      jsonPath: result.jsonPath,
      markdownPath: result.markdownPath,
      ...result.diff
    };
  }

  try {
    const latest = await findLatestCoverageMatrixFiles({
      workspaceRoot,
      logsDir: path.dirname(currentCoveragePath)
    });
    const result = await runCoverageDiffCli({
      workspaceRoot,
      argv: [
        `--baseline-path=${latest.baselinePath}`,
        `--current-path=${latest.currentPath}`,
        `--json-out=${artifactPaths.diffJsonPath}`,
        `--md-out=${artifactPaths.diffMarkdownPath}`
      ]
    });
    return {
      jsonPath: result.jsonPath,
      markdownPath: result.markdownPath,
      ...result.diff
    };
  } catch (error) {
    return {
      skipped: true,
      skippedReason: error.message
    };
  }
}

export async function runAcceptanceReadinessCli({
  argv = process.argv.slice(2),
  workspaceRoot = process.cwd()
} = {}) {
  const options = parseReadinessArgs(argv);
  const timestamp = createTimestamp(options.timestamp);
  const outputDir = resolveOutputDir({ workspaceRoot, options });
  const artifactPaths = createArtifactPaths({ outputDir, timestamp });
  const coverage = await resolveCoverageSnapshot({
    workspaceRoot,
    options,
    artifactPaths
  });
  const diff = await resolveDiffSnapshot({
    workspaceRoot,
    options,
    artifactPaths,
    currentCoveragePath: coverage.diffCurrentPath
  });

  const report = buildAcceptanceReadinessReport({
    inputs: {
      coveragePolicyPath: options.coveragePolicyPath || '',
      baselineCoveragePath: options.baselineCoveragePath || '',
      currentCoveragePath: options.currentCoveragePath || coverage.diffCurrentPath,
      skipDiff: options.skipDiff
    },
    coverage: {
      jsonPath: coverage.jsonPath,
      markdownPath: coverage.markdownPath,
      summary: coverage.matrix.summary,
      policyEvaluation: coverage.matrix.policyEvaluation
    },
    diff: diff.skipped ? null : diff,
    skipReason: diff.skippedReason || ''
  });

  await writeAcceptanceReadinessArtifacts({
    report,
    jsonPath: artifactPaths.readinessJsonPath,
    markdownPath: artifactPaths.readinessMarkdownPath
  });

  return {
    exitCode: report.exitCode,
    jsonPath: artifactPaths.readinessJsonPath,
    markdownPath: artifactPaths.readinessMarkdownPath,
    report
  };
}

const currentFilePath = fileURLToPath(import.meta.url);

if (
  process.argv[1] &&
  path.resolve(process.argv[1]) === path.resolve(currentFilePath)
) {
  try {
    const result = await runAcceptanceReadinessCli();
    console.log(
      JSON.stringify(
        {
          exitCode: result.exitCode,
          status: result.report.status,
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
