import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';

import {
  loadRunbookConfig,
  runObservabilityLogGovernanceCli,
  validateApplyConfirmation
} from './run-observability-log-governance.mjs';

test('loadRunbookConfig exposes default dry-run and apply confirmation window', async () => {
  const workspaceRoot = process.cwd();
  const config = await loadRunbookConfig({
    workspaceRoot,
    runbookPath: 'config/automation/observability-log-governance-runbook.json'
  });

  assert.equal(config.defaultMode, 'dry-run');
  assert.equal(config.maxApplyReportAgeHours, 24);
  assert.match(
    config.policyPath,
    /config[\\/]+automation[\\/]+observability-log-governance-policy\.json$/
  );
});

test('validateApplyConfirmation rejects stale dry-run reports', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'observability-governance-stale-')
  );
  const reportPath = path.join(workspaceRoot, 'stale-report.json');
  await fs.writeFile(
    reportPath,
    JSON.stringify(
      {
        generatedAt: '2026-04-24T00:00:00',
        mode: 'DRY_RUN',
        summary: {
          expiredRows: 18
        }
      },
      null,
      2
    ),
    'utf8'
  );

  await assert.rejects(
    () =>
      validateApplyConfirmation({
        confirmReportPath: reportPath,
        confirmExpiredRows: 18,
        maxReportAgeHours: 24,
        now: new Date('2026-04-25T12:30:00')
      }),
    /older than 24 hours/i
  );
});

test('runObservabilityLogGovernanceCli uses dry-run by default', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'observability-governance-dry-run-')
  );
  const runbookPath = path.join(workspaceRoot, 'observability-runbook.json');
  await fs.writeFile(
    runbookPath,
    JSON.stringify(
      {
        policyPath: 'config/automation/observability-log-governance-policy.json',
        outputDir: 'logs/observability',
        defaultMode: 'dry-run',
        maxApplyReportAgeHours: 24
      },
      null,
      2
    ),
    'utf8'
  );

  let capturedArgv = null;
  const result = await runObservabilityLogGovernanceCli({
    workspaceRoot,
    argv: [`--runbook-path=${runbookPath}`, '--timestamp=20260425-230000'],
    runGovernance: async ({ argv: runnerArgv }) => {
      capturedArgv = runnerArgv;
      const jsonPath = path.join(
        workspaceRoot,
        'logs/observability/observability-log-governance-20260425-230000.json'
      );
      const markdownPath = path.join(
        workspaceRoot,
        'logs/observability/observability-log-governance-20260425-230000.md'
      );
      await fs.mkdir(path.dirname(jsonPath), { recursive: true });
      await fs.writeFile(
        jsonPath,
        JSON.stringify(
          {
            generatedAt: '2026-04-25T23:00:00',
            mode: 'DRY_RUN',
            summary: {
              expiredRows: 6,
              deletedRows: 0
            }
          },
          null,
          2
        ),
        'utf8'
      );
      await fs.writeFile(markdownPath, '# report\n', 'utf8');
      return {
        exitCode: 0,
        jsonPath,
        markdownPath,
        report: JSON.parse(await fs.readFile(jsonPath, 'utf8'))
      };
    }
  });

  assert.ok(capturedArgv);
  assert.equal(capturedArgv.includes('--apply'), false);
  assert.equal(result.mode, 'DRY_RUN');
  assert.equal(result.confirmation.required, false);
});

test('runObservabilityLogGovernanceCli requires matching dry-run evidence before apply', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'observability-governance-apply-')
  );
  const reportPath = path.join(workspaceRoot, 'dry-run-report.json');
  await fs.writeFile(
    reportPath,
    JSON.stringify(
      {
        generatedAt: '2026-04-25T09:00:00',
        mode: 'DRY_RUN',
        summary: {
          expiredRows: 8
        }
      },
      null,
      2
    ),
    'utf8'
  );

  await assert.rejects(
    () =>
      runObservabilityLogGovernanceCli({
        workspaceRoot,
        argv: [
          '--mode=apply',
          `--confirm-report=${reportPath}`,
          '--confirm-expired-rows=7'
        ],
        now: () => new Date('2026-04-25T10:00:00'),
        runGovernance: async () => {
          throw new Error('runner should not execute');
        }
      }),
    /does not match report expiredRows/i
  );
});

test('runObservabilityLogGovernanceCli allows apply with fresh matching dry-run evidence', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'observability-governance-apply-ok-')
  );
  const runbookPath = path.join(workspaceRoot, 'observability-runbook.json');
  const reportPath = path.join(workspaceRoot, 'dry-run-report.json');
  await fs.writeFile(
    runbookPath,
    JSON.stringify(
      {
        policyPath: 'config/automation/observability-log-governance-policy.json',
        outputDir: 'logs/observability',
        defaultMode: 'dry-run',
        maxApplyReportAgeHours: 24
      },
      null,
      2
    ),
    'utf8'
  );
  await fs.writeFile(
    reportPath,
    JSON.stringify(
      {
        generatedAt: '2026-04-25T09:00:00',
        mode: 'DRY_RUN',
        summary: {
          expiredRows: 8
        }
      },
      null,
      2
    ),
    'utf8'
  );

  let capturedArgv = null;
  const result = await runObservabilityLogGovernanceCli({
    workspaceRoot,
    argv: [
      `--runbook-path=${runbookPath}`,
      '--mode=apply',
      `--confirm-report=${reportPath}`,
      '--confirm-expired-rows=8',
      '--timestamp=20260425-231500'
    ],
    now: () => new Date('2026-04-25T10:00:00'),
    runGovernance: async ({ argv: runnerArgv }) => {
      capturedArgv = runnerArgv;
      const jsonPath = path.join(
        workspaceRoot,
        'logs/observability/observability-log-governance-20260425-231500.json'
      );
      const markdownPath = path.join(
        workspaceRoot,
        'logs/observability/observability-log-governance-20260425-231500.md'
      );
      await fs.mkdir(path.dirname(jsonPath), { recursive: true });
      await fs.writeFile(
        jsonPath,
        JSON.stringify(
          {
            generatedAt: '2026-04-25T23:15:00',
            mode: 'APPLY',
            summary: {
              expiredRows: 8,
              deletedRows: 8
            }
          },
          null,
          2
        ),
        'utf8'
      );
      await fs.writeFile(markdownPath, '# apply report\n', 'utf8');
      return {
        exitCode: 0,
        jsonPath,
        markdownPath,
        report: JSON.parse(await fs.readFile(jsonPath, 'utf8'))
      };
    }
  });

  assert.ok(capturedArgv);
  assert.equal(capturedArgv.includes('--apply'), true);
  assert.equal(result.mode, 'APPLY');
  assert.equal(result.confirmation.required, true);
  assert.equal(result.confirmation.confirmedExpiredRows, 8);
});
