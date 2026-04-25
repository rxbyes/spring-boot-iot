import { spawn } from 'node:child_process';
import fs from 'node:fs/promises';
import path from 'node:path';

function resolvePowerShellExecutable() {
  return process.platform === 'win32' ? 'powershell' : 'pwsh';
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
  return (
    item &&
    typeof item === 'object' &&
    String(item.status || '').trim().toUpperCase() !== 'PASS'
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
  for (const summaryFile of [meta.REPORT_SUMMARY, meta.SUMMARY_JSON]) {
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
    ...(failed.detail ? { failureDetail: failed.detail } : {})
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
            `--plan=${context.scenario.runner.planRef}`
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
      ((context) =>
        runCommandRunner({
          executable: resolvePowerShellExecutable(),
          args: [
            '-NoProfile',
            '-ExecutionPolicy',
            'Bypass',
            '-File',
            'scripts/run-business-function-smoke.ps1',
            '-BaseUrl',
            context.options.backendBaseUrl || context.registry.defaultTarget?.backendBaseUrl || 'http://127.0.0.1:9999',
            ...(context.scenario.runner.pointFilters || []).flatMap((item) => ['-PointFilter', item])
          ],
          context
        })),
    messageFlow:
      overrides.messageFlow ||
      ((context) => {
        const expiredTraceId = context.options.expiredTraceId || context.scenario.inputs?.expiredTraceId;
        const args = ['scripts/run-message-flow-acceptance.py'];
        if (expiredTraceId) {
          args.push('--expired-trace-id', expiredTraceId);
        }
        return runCommandRunner({
          executable: 'python',
          args,
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
