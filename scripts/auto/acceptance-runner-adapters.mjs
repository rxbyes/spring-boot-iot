import { spawn } from 'node:child_process';
import fs from 'node:fs/promises';
import fsSync from 'node:fs';
import path from 'node:path';

function listPathEntries(envPath) {
  if (!envPath) {
    return [];
  }
  return envPath.split(path.delimiter).filter(Boolean);
}

function fileExists(filePath) {
  try {
    fsSync.accessSync(filePath, fsSync.constants.X_OK);
    return true;
  } catch {
    return false;
  }
}

function hasCommand(command, platform = process.platform, envPath = process.env.PATH ?? '') {
  const entries = listPathEntries(envPath);
  if (entries.length === 0) {
    return false;
  }

  const extensions = platform === 'win32'
    ? (process.env.PATHEXT || '.EXE;.CMD;.BAT;.COM')
        .split(';')
        .filter(Boolean)
    : [''];

  for (const entry of entries) {
    for (const extension of extensions) {
      const candidatePath = path.join(
        entry,
        platform === 'win32' ? `${command}${extension}` : command
      );
      if (fileExists(candidatePath)) {
        return true;
      }
    }
  }

  return false;
}

function resolveApiSmokeCommand({
  workspaceRoot,
  backendBaseUrl,
  pointFilters = [],
  moduleFilters = [],
  platform = process.platform,
  availableCommands
}) {
  const baseUrl = backendBaseUrl || 'http://127.0.0.1:9999';
  const commands = availableCommands
    ? new Set(availableCommands)
    : new Set(['pwsh', 'powershell'].filter((command) => hasCommand(command, platform)));

  const pointArgs = pointFilters.flatMap((item) => ['-PointFilter', item]);
  const moduleArgs = moduleFilters.flatMap((item) => ['-ModuleFilter', item]);

  if (platform === 'win32' || commands.has('pwsh') || commands.has('powershell')) {
    return {
      executable:
        platform === 'win32'
          ? (commands.has('pwsh') ? 'pwsh' : 'powershell')
          : (commands.has('pwsh') ? 'pwsh' : 'powershell'),
      args: [
        '-NoProfile',
        '-ExecutionPolicy',
        'Bypass',
        '-File',
        'scripts/run-business-function-smoke.ps1',
        '-BaseUrl',
        baseUrl,
        ...pointArgs,
        ...moduleArgs
      ]
    };
  }

  return {
    executable: process.execPath,
    args: [
      'scripts/run-business-function-smoke.mjs',
      '-BaseUrl',
      baseUrl,
      ...pointArgs,
      ...moduleArgs
    ]
  };
}

function resolvePythonExecutable({
  platform = process.platform,
  availableCommands
} = {}) {
  const commands = availableCommands
    ? new Set(availableCommands)
    : new Set(['python3', 'python'].filter((command) => hasCommand(command, platform)));

  if (platform === 'win32') {
    return commands.has('python') ? 'python' : 'python3';
  }
  if (commands.has('python3')) {
    return 'python3';
  }
  return 'python';
}

function buildMessageFlowCommand({
  backendBaseUrl,
  expiredTraceId,
  platform = process.platform,
  availableCommands
} = {}) {
  const args = ['scripts/run-message-flow-acceptance.py'];
  if (backendBaseUrl) {
    args.push(`--base-url=${backendBaseUrl}`);
  }
  if (expiredTraceId) {
    args.push('--expired-trace-id', expiredTraceId);
  }
  return {
    executable: resolvePythonExecutable({ platform, availableCommands }),
    args
  };
}

function buildKeyValueMap(stdout = '') {
  return stdout
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .reduce((acc, line) => {
      const eqIndex = line.indexOf('=');
      if (eqIndex <= 0) {
        return acc;
      }
      const key = line.slice(0, eqIndex).trim();
      const value = line.slice(eqIndex + 1).trim();
      if (key) {
        acc[key] = value;
      }
      return acc;
    }, {});
}

function normalizeEvidenceFiles(values, workspaceRoot) {
  return values
    .filter(Boolean)
    .map((value) =>
      value.startsWith(workspaceRoot)
        ? value.slice(workspaceRoot.length + 1).replace(/\\/g, '/')
        : value.replace(/\\/g, '/')
    );
}

function resolveWorkspaceFile(filePath, workspaceRoot) {
  if (!filePath) {
    return '';
  }
  return path.isAbsolute(filePath)
    ? filePath
    : path.resolve(workspaceRoot, filePath);
}

function isSmokeFailureRow(item) {
  const status = String(item?.status || '').trim().toUpperCase();
  return (
    item &&
    typeof item === 'object' &&
    status &&
    status !== 'PASS'
  );
}

function normalizeSmokeSummaryRows(payload) {
  if (Array.isArray(payload)) {
    return payload;
  }
  if (!payload || typeof payload !== 'object') {
    return [];
  }
  for (const key of ['results', 'summary', 'rows', 'cases']) {
    if (Array.isArray(payload[key])) {
      return payload[key];
    }
  }
  return [payload];
}

async function readApiSmokeFailure(meta, workspaceRoot) {
  for (const summaryFile of [meta.REPORT_JSON, meta.DETAIL_JSON, meta.REPORT_SUMMARY, meta.SUMMARY_JSON]) {
    if (!summaryFile) {
      continue;
    }
    try {
      const summaryText = await fs.readFile(
        resolveWorkspaceFile(summaryFile, workspaceRoot),
        'utf8'
      );
      const payload = JSON.parse(summaryText.replace(/^\uFEFF/, ''));
      const failed = normalizeSmokeSummaryRows(payload).find(isSmokeFailureRow);
      if (failed) {
        return failed;
      }
    } catch {
      // The command result remains the source of truth if optional metadata is unreadable.
    }
  }
  return null;
}

function buildApiSmokeFailureDetails(failed) {
  if (!failed) {
    return {};
  }
  const stepLabel = [failed.point, failed.case].filter(Boolean).join('/');
  const apiRef = [failed.method, failed.path].filter(Boolean).join(' ');

  return {
    ...(stepLabel ? { stepLabel } : {}),
    ...(apiRef ? { apiRef } : {}),
    ...(stepLabel
      ? { pageAction: `执行业务烟测点 ${stepLabel}` }
      : {}),
    ...(failed.detail ? { failureDetail: failed.detail, smokeDetail: failed.detail } : {})
  };
}

async function runProcess(executable, args, { cwd, env }) {
  return new Promise((resolve, reject) => {
    const child = spawn(executable, args, {
      cwd,
      env: {
        ...process.env,
        ...env
      },
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

async function runCommandRunner({ executable, args, context, env = {} }) {
  const completed = await runProcess(executable, args, {
    cwd: context.workspaceRoot,
    env
  });
  const meta = buildKeyValueMap(completed.stdout);
  const evidenceFiles = normalizeEvidenceFiles(
    [
      meta.REPORT_JSON,
      meta.REPORT_SUMMARY,
      meta.REPORT_MD,
      meta.DETAIL_JSON,
      meta.SUMMARY_JSON,
      meta.REPORT_PATH,
      meta.JSON_PATH,
      meta.MD_PATH
    ],
    context.workspaceRoot
  );
  const status = completed.code === 0 ? 'passed' : 'failed';
  const failedApiSmokeRow =
    status === 'failed' && context.scenario.runnerType === 'apiSmoke'
      ? await readApiSmokeFailure(meta, context.workspaceRoot)
      : null;
  const fallbackSummary =
    meta.SUMMARY ||
    meta.STATUS ||
    completed.stderr.trim() ||
    completed.stdout.trim().split(/\r?\n/).at(-1) ||
    `${context.scenario.id} ${status}`;

  return {
    scenarioId: context.scenario.id,
    runnerType: context.scenario.runnerType,
    status,
    blocking: context.scenario.blocking,
    summary: failedApiSmokeRow?.detail || fallbackSummary,
    evidenceFiles,
    details: {
      executable,
      args,
      exitCode: completed.code,
      stdout: completed.stdout,
      stderr: completed.stderr,
      ...buildApiSmokeFailureDetails(failedApiSmokeRow)
    }
  };
}

export const __runCommandForTest = runCommandRunner;
export const resolveApiSmokeCommandForTest = resolveApiSmokeCommand;
export const resolvePythonExecutableForTest = resolvePythonExecutable;
export const buildMessageFlowCommandForTest = buildMessageFlowCommand;

export function createRunnerAdapters({ workspaceRoot, overrides = {} }) {
  return {
    __runCommandForTest: runCommandRunner,
    browserPlan:
      overrides.browserPlan ||
      ((context) =>
        runCommandRunner({
          executable: process.execPath,
          args: [
            'scripts/auto/run-browser-acceptance.mjs',
            `--plan=${context.scenario.runner.planRef}`,
            ...(context.scenario.runner.scenarioScopes || []).length
              ? [`--scopes=${context.scenario.runner.scenarioScopes.join(',')}`]
              : [],
            ...(context.scenario.runner.failScopes || []).length
              ? [`--fail-scopes=${context.scenario.runner.failScopes.join(',')}`]
              : [],
            ...(context.scenario.runner.scenarioKeys || []).length
              ? [`--scenario-keys=${context.scenario.runner.scenarioKeys.join(',')}`]
              : []
          ],
          context,
          env: {
            IOT_ACCEPTANCE_FRONTEND_URL:
              context.options.frontendBaseUrl || '',
            IOT_ACCEPTANCE_BACKEND_URL:
              context.options.backendBaseUrl || ''
          }
        })),
    apiSmoke:
      overrides.apiSmoke ||
      ((context) => {
        const command = resolveApiSmokeCommand({
          workspaceRoot,
          backendBaseUrl:
            context.options.backendBaseUrl ||
            context.registry.defaultTarget?.backendBaseUrl ||
            'http://127.0.0.1:9999',
          pointFilters: context.scenario.runner.pointFilters || [],
          moduleFilters: context.scenario.runner.moduleFilters || []
        });
        return runCommandRunner({
          executable: command.executable,
          args: command.args,
          context
        });
      }),
    messageFlow:
      overrides.messageFlow ||
      ((context) => {
        const expiredTraceId = context.options.expiredTraceId || context.scenario.inputs?.expiredTraceId;
        const command = buildMessageFlowCommand({
          backendBaseUrl: context.options.backendBaseUrl,
          expiredTraceId
        });
        return runCommandRunner({
          executable: command.executable,
          args: command.args,
          context
        });
      }),
    riskDrill:
      overrides.riskDrill ||
      ((context) =>
        runCommandRunner({
          executable: process.execPath,
          args: [
            'scripts/auto/run-risk-closure-drill.mjs',
            `--scenario=${context.scenario.id}`,
            ...(context.options.backendBaseUrl
              ? [`--backend-base-url=${context.options.backendBaseUrl}`]
              : [])
          ],
          context
        }))
  };
}
