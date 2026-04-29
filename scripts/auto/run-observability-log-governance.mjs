import { spawn } from 'node:child_process';
import fs from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const CURRENT_FILE = fileURLToPath(import.meta.url);
const REPO_ROOT = path.resolve(path.dirname(CURRENT_FILE), '..', '..');
const DEFAULT_RUNBOOK_PATH = path.join(
  REPO_ROOT,
  'config',
  'automation',
  'observability-log-governance-runbook.json'
);

function resolveWorkspacePath(workspaceRoot, value) {
  if (!value) {
    return '';
  }
  return path.isAbsolute(value) ? value : path.resolve(workspaceRoot, value);
}

function normalizeMode(value, defaultMode = 'dry-run') {
  const normalized = String(value || defaultMode).trim().toLowerCase();
  if (normalized === 'dry-run' || normalized === 'dryrun') {
    return 'dry-run';
  }
  if (normalized === 'apply') {
    return 'apply';
  }
  throw new Error(`Unsupported mode: ${value}`);
}

function parseInteger(value, label) {
  const parsed = Number.parseInt(String(value ?? '').trim(), 10);
  if (!Number.isInteger(parsed)) {
    throw new Error(`${label} must be an integer.`);
  }
  return parsed;
}

function parseArgs(argv) {
  const options = {};

  argv.forEach((arg) => {
    if (arg.startsWith('--mode=')) {
      options.mode = arg.slice('--mode='.length).trim();
      return;
    }
    if (arg.startsWith('--runbook-path=')) {
      options.runbookPath = arg.slice('--runbook-path='.length).trim();
      return;
    }
    if (arg.startsWith('--policy-path=')) {
      options.policyPath = arg.slice('--policy-path='.length).trim();
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
    if (arg.startsWith('--confirm-report=')) {
      options.confirmReportPath = arg.slice('--confirm-report='.length).trim();
      return;
    }
    if (arg.startsWith('--confirm-expired-rows=')) {
      options.confirmExpiredRows = arg
        .slice('--confirm-expired-rows='.length)
        .trim();
      return;
    }
    if (arg.startsWith('--max-report-age-hours=')) {
      options.maxReportAgeHours = arg
        .slice('--max-report-age-hours='.length)
        .trim();
      return;
    }
    if (arg.startsWith('--python=')) {
      options.pythonExecutable = arg.slice('--python='.length).trim();
      return;
    }
    if (arg.startsWith('--jdbc-url=')) {
      options.jdbcUrl = arg.slice('--jdbc-url='.length).trim();
      return;
    }
    if (arg.startsWith('--user=')) {
      options.user = arg.slice('--user='.length).trim();
      return;
    }
    if (arg.startsWith('--password=')) {
      options.password = arg.slice('--password='.length).trim();
      return;
    }
    throw new Error(`Unknown argument: ${arg}`);
  });

  return options;
}

function defaultPythonExecutable() {
  return process.platform === 'win32' ? 'python' : 'python3';
}

function createTimestamp(value = '') {
  if (value) {
    return value;
  }
  const now = new Date();
  return [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, '0'),
    String(now.getDate()).padStart(2, '0')
  ].join('') +
    '-' +
    [
      String(now.getHours()).padStart(2, '0'),
      String(now.getMinutes()).padStart(2, '0'),
      String(now.getSeconds()).padStart(2, '0')
    ].join('');
}

export async function loadRunbookConfig({
  workspaceRoot = process.cwd(),
  runbookPath = DEFAULT_RUNBOOK_PATH
} = {}) {
  const resolvedRunbookPath = resolveWorkspacePath(workspaceRoot, runbookPath);
  const payload = JSON.parse(
    (await fs.readFile(resolvedRunbookPath, 'utf8')).replace(/^\uFEFF/, '')
  );
  const defaultMode = normalizeMode(payload.defaultMode || 'dry-run');
  const maxApplyReportAgeHours = parseInteger(
    payload.maxApplyReportAgeHours ?? 24,
    'maxApplyReportAgeHours'
  );

  return {
    runbookPath: resolvedRunbookPath,
    policyPath: resolveWorkspacePath(
      workspaceRoot,
      payload.policyPath || 'config/automation/observability-log-governance-policy.json'
    ),
    outputDir: resolveWorkspacePath(
      workspaceRoot,
      payload.outputDir || 'logs/observability'
    ),
    defaultMode,
    maxApplyReportAgeHours,
    governanceScriptPath: resolveWorkspacePath(
      workspaceRoot,
      payload.governanceScriptPath || 'scripts/govern-observability-logs.py'
    ),
    pythonExecutable: payload.pythonExecutable || defaultPythonExecutable()
  };
}

function parseReportTimestamp(value) {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    throw new Error(`Invalid report generatedAt timestamp: ${value}`);
  }
  return parsed;
}

async function readJsonFile(filePath) {
  return JSON.parse((await fs.readFile(filePath, 'utf8')).replace(/^\uFEFF/, ''));
}

export async function validateApplyConfirmation({
  workspaceRoot = process.cwd(),
  confirmReportPath,
  confirmExpiredRows,
  maxReportAgeHours,
  now = new Date()
}) {
  if (!confirmReportPath) {
    throw new Error('Apply mode requires --confirm-report.');
  }
  const resolvedReportPath = resolveWorkspacePath(workspaceRoot, confirmReportPath);
  const confirmedExpiredRows = parseInteger(
    confirmExpiredRows,
    'confirmExpiredRows'
  );
  const report = await readJsonFile(resolvedReportPath);
  if (String(report?.mode || '').trim().toUpperCase() !== 'DRY_RUN') {
    throw new Error('Apply confirmation report must be a DRY_RUN report.');
  }

  const reportExpiredRows = parseInteger(
    report?.summary?.expiredRows,
    'report expiredRows'
  );
  if (reportExpiredRows !== confirmedExpiredRows) {
    throw new Error(
      `Confirmed expiredRows ${confirmedExpiredRows} does not match report expiredRows ${reportExpiredRows}.`
    );
  }

  const reportGeneratedAt = parseReportTimestamp(report.generatedAt);
  const currentTime = now instanceof Date ? now : new Date(now);
  const ageMilliseconds = currentTime.getTime() - reportGeneratedAt.getTime();
  const maxAgeMilliseconds = parseInteger(
    maxReportAgeHours,
    'maxReportAgeHours'
  ) * 60 * 60 * 1000;
  if (ageMilliseconds > maxAgeMilliseconds) {
    throw new Error(
      `Confirmation report ${resolvedReportPath} is older than ${maxReportAgeHours} hours.`
    );
  }

  return {
    required: true,
    confirmReportPath: resolvedReportPath,
    reportGeneratedAt: report.generatedAt,
    reportExpiredRows,
    confirmedExpiredRows,
    maxReportAgeHours: parseInteger(maxReportAgeHours, 'maxReportAgeHours')
  };
}

function createArtifactPaths({ outputDir, timestamp }) {
  const basePath = path.join(
    outputDir,
    `observability-log-governance-${timestamp}`
  );
  return {
    jsonPath: `${basePath}.json`,
    markdownPath: `${basePath}.md`
  };
}

function buildGovernanceArgv({
  config,
  options,
  mode,
  artifactPaths,
  confirmation
}) {
  const argv = [
    `--policy-path=${options.policyPath || config.policyPath}`,
    `--json-out=${artifactPaths.jsonPath}`,
    `--md-out=${artifactPaths.markdownPath}`
  ];

  if (mode === 'apply') {
    argv.push('--apply');
    if (confirmation?.confirmReportPath) {
      argv.push(`--confirm-report-path=${confirmation.confirmReportPath}`);
    }
    if (confirmation?.reportGeneratedAt) {
      argv.push(`--confirm-report-generated-at=${confirmation.reportGeneratedAt}`);
    }
    if (Number.isInteger(confirmation?.confirmedExpiredRows)) {
      argv.push(
        `--confirmed-expired-rows=${confirmation.confirmedExpiredRows}`
      );
    }
  }
  if (options.jdbcUrl) {
    argv.push(`--jdbc-url=${options.jdbcUrl}`);
  }
  if (options.user) {
    argv.push(`--user=${options.user}`);
  }
  if (options.password) {
    argv.push(`--password=${options.password}`);
  }

  return argv;
}

async function executeProcess({ executable, args, cwd }) {
  return new Promise((resolve, reject) => {
    const child = spawn(executable, args, {
      cwd,
      stdio: ['ignore', 'pipe', 'pipe']
    });
    let stdout = '';
    let stderr = '';

    child.stdout.on('data', (chunk) => {
      stdout += chunk.toString();
    });
    child.stderr.on('data', (chunk) => {
      stderr += chunk.toString();
    });
    child.on('error', reject);
    child.on('close', (code) => {
      resolve({
        code: code ?? 1,
        stdout,
        stderr
      });
    });
  });
}

async function defaultRunGovernance({
  workspaceRoot,
  config,
  argv,
  artifactPaths,
  options
}) {
  await fs.mkdir(path.dirname(artifactPaths.jsonPath), { recursive: true });
  const completed = await executeProcess({
    executable: options.pythonExecutable || config.pythonExecutable,
    args: [config.governanceScriptPath, ...argv],
    cwd: workspaceRoot
  });
  if (completed.code !== 0) {
    throw new Error(
      completed.stderr.trim() ||
        completed.stdout.trim() ||
        `Observability log governance failed with exit code ${completed.code}.`
    );
  }

  return {
    exitCode: completed.code,
    jsonPath: artifactPaths.jsonPath,
    markdownPath: artifactPaths.markdownPath,
    report: await readJsonFile(artifactPaths.jsonPath),
    stdout: completed.stdout,
    stderr: completed.stderr
  };
}

export async function runObservabilityLogGovernanceCli({
  argv = process.argv.slice(2),
  workspaceRoot = process.cwd(),
  now = () => new Date(),
  runGovernance = defaultRunGovernance
} = {}) {
  const options = parseArgs(argv);
  const config = await loadRunbookConfig({
    workspaceRoot,
    runbookPath: options.runbookPath || DEFAULT_RUNBOOK_PATH
  });
  const mode = normalizeMode(options.mode, config.defaultMode);
  const timestamp = createTimestamp(options.timestamp);
  const outputDir = resolveWorkspacePath(
    workspaceRoot,
    options.outputDir || config.outputDir
  );
  const artifactPaths = createArtifactPaths({
    outputDir,
    timestamp
  });

  const confirmation =
    mode === 'apply'
      ? await validateApplyConfirmation({
          workspaceRoot,
          confirmReportPath: options.confirmReportPath,
          confirmExpiredRows: options.confirmExpiredRows,
          maxReportAgeHours:
            options.maxReportAgeHours || config.maxApplyReportAgeHours,
          now: typeof now === 'function' ? now() : now
        })
      : {
          required: false
        };

  const runnerArgv = buildGovernanceArgv({
    config,
    options,
    mode,
    artifactPaths,
    confirmation
  });
  const result = await runGovernance({
    workspaceRoot,
    config,
    argv: runnerArgv,
    artifactPaths,
    options
  });
  const archiveBatch = result.report?.tables?.iot_message_log?.archiveBatch || null;

  return {
    exitCode: Number(result.exitCode || 0),
    status: mode === 'apply' ? 'applied' : 'dry-run',
    mode: String(result?.report?.mode || '').trim().toUpperCase() || (mode === 'apply' ? 'APPLY' : 'DRY_RUN'),
    jsonPath: result.jsonPath,
    markdownPath: result.markdownPath,
    report: result.report,
    confirmation,
    archiveBatch
  };
}

if (process.argv[1] && path.resolve(process.argv[1]) === path.resolve(CURRENT_FILE)) {
  try {
    const result = await runObservabilityLogGovernanceCli();
    console.log(
      JSON.stringify(
        {
          exitCode: result.exitCode,
          status: result.status,
          mode: result.mode,
          jsonPath: result.jsonPath,
          markdownPath: result.markdownPath,
          confirmation: result.confirmation,
          archiveBatch: result.archiveBatch,
          summary: result.report?.summary || {}
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
