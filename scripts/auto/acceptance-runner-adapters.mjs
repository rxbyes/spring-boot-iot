import { spawn } from 'node:child_process';

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

  return {
    scenarioId: context.scenario.id,
    runnerType: context.scenario.runnerType,
    status: completed.code === 0 ? 'passed' : 'failed',
    blocking: context.scenario.blocking,
    summary:
      meta.SUMMARY ||
      meta.STATUS ||
      completed.stderr.trim() ||
      completed.stdout.trim().split(/\r?\n/).at(-1) ||
      `${context.scenario.id} ${completed.code === 0 ? 'passed' : 'failed'}`,
    evidenceFiles,
    details: {
      executable,
      args,
      exitCode: completed.code,
      stdout: completed.stdout,
      stderr: completed.stderr
    }
  };
}

export function createRunnerAdapters({ workspaceRoot, overrides = {} }) {
  return {
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
