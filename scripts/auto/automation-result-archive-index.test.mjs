import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';

import {
  buildAutomationResultArchiveIndex,
  renderAutomationResultArchiveIndexMarkdown
} from './automation-result-archive-index-lib.mjs';
import { runAutomationResultArchiveIndexCli } from './generate-automation-result-archive-index.mjs';

async function writeJson(filePath, value) {
  await fs.mkdir(path.dirname(filePath), { recursive: true });
  await fs.writeFile(filePath, JSON.stringify(value, null, 2), 'utf8');
}

async function writeText(filePath, value) {
  await fs.mkdir(path.dirname(filePath), { recursive: true });
  await fs.writeFile(filePath, value, 'utf8');
}

function createRun({
  runId,
  packageCode,
  environmentCode,
  updatedAt = '2026-04-25T23:10:00+08:00',
  status = 'passed',
  runnerType = 'browserPlan',
  failedScenarioId = '',
  relatedEvidenceFiles = [],
  scenarioEvidenceFiles = []
}) {
  const failed = status === 'failed' ? 1 : 0;
  return {
    runId,
    updatedAt,
    options: {
      packageCode,
      environmentCode
    },
    summary: {
      total: 1,
      passed: status === 'failed' ? 0 : 1,
      failed
    },
    results: [
      {
        scenarioId: failedScenarioId || `${runId}.scenario`,
        runnerType,
        status,
        blocking: failed ? 'blocker' : 'warning',
        summary: failed ? '场景失败' : '场景通过',
        evidenceFiles: scenarioEvidenceFiles
      }
    ],
    relatedEvidenceFiles
  };
}

test('buildAutomationResultArchiveIndex builds run-level facets and skips invalid files', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'automation-result-archive-index-build-')
  );
  const resultsDir = path.join(workspaceRoot, 'logs', 'acceptance');

  await writeJson(
    path.join(resultsDir, 'registry-run-20260425231000.json'),
    createRun({
      runId: '20260425231000',
      packageCode: 'quality-factory-p0',
      environmentCode: 'dev',
      runnerType: 'browserPlan',
      relatedEvidenceFiles: ['logs/acceptance/browser-summary-20260425231000.md'],
      scenarioEvidenceFiles: ['logs/acceptance/browser-shot-20260425231000.json']
    })
  );
  await writeJson(
    path.join(resultsDir, 'registry-run-20260425232000.json'),
    createRun({
      runId: '20260425232000',
      packageCode: 'product-governance-p1',
      environmentCode: 'sit',
      runnerType: 'riskDrill',
      status: 'failed',
      failedScenarioId: 'product-governance.publish'
    })
  );
  await writeText(
    path.join(resultsDir, 'registry-run-20260425233000.json'),
    '{broken-json'
  );
  await writeText(
    path.join(resultsDir, 'browser-summary-20260425231000.md'),
    '# browser summary'
  );
  await writeText(
    path.join(resultsDir, 'browser-shot-20260425231000.json'),
    '{"ok":true}'
  );

  const index = await buildAutomationResultArchiveIndex({
    workspaceRoot,
    resultsDir
  });

  assert.equal(index.sourceSummary.registryRunFiles, 3);
  assert.equal(index.sourceSummary.indexedRuns, 2);
  assert.equal(index.sourceSummary.skippedFiles, 1);
  assert.deepEqual(index.facets.packageCodes, ['product-governance-p1', 'quality-factory-p0']);
  assert.deepEqual(index.facets.environmentCodes, ['dev', 'sit']);
  assert.deepEqual(index.facets.runnerTypes, ['browserPlan', 'riskDrill']);
  assert.deepEqual(index.facets.statuses, ['failed', 'passed']);
  assert.equal(index.runs[0].runId, '20260425232000');
  assert.equal(index.runs[0].status, 'failed');
  assert.equal(index.runs[1].evidenceItems.length, 3);
  assert.equal(index.skippedFiles[0].fileName, 'registry-run-20260425233000.json');
  assert.equal(index.skippedFiles[0].reason, 'invalid-json');
});

test('renderAutomationResultArchiveIndexMarkdown includes summary and skipped files', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'automation-result-archive-index-md-')
  );
  const resultsDir = path.join(workspaceRoot, 'logs', 'acceptance');

  await writeJson(
    path.join(resultsDir, 'registry-run-20260425234000.json'),
    createRun({
      runId: '20260425234000',
      packageCode: 'quality-factory-p0',
      environmentCode: 'dev'
    })
  );
  await writeText(
    path.join(resultsDir, 'registry-run-20260425235000.json'),
    '{broken-json'
  );

  const markdown = renderAutomationResultArchiveIndexMarkdown(
    await buildAutomationResultArchiveIndex({ workspaceRoot, resultsDir })
  );

  assert.match(markdown, /# Automation Result Archive Index/);
  assert.match(markdown, /quality-factory-p0/);
  assert.match(markdown, /registry-run-20260425235000\.json/);
  assert.match(markdown, /invalid-json/);
});

test('runAutomationResultArchiveIndexCli writes latest and timestamped artifacts', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'automation-result-archive-index-cli-')
  );
  const resultsDir = path.join(workspaceRoot, 'logs', 'acceptance');

  await writeJson(
    path.join(resultsDir, 'registry-run-20260426000000.json'),
    createRun({
      runId: '20260426000000',
      packageCode: 'quality-factory-p0',
      environmentCode: 'dev'
    })
  );

  const result = await runAutomationResultArchiveIndexCli({
    workspaceRoot,
    argv: []
  });

  assert.match(result.latestJsonPath, /automation-result-index\.latest\.json$/);
  assert.match(result.jsonPath, /automation-result-index-\d+\.json$/);
  assert.match(result.markdownPath, /automation-result-index-\d+\.md$/);
  assert.equal(result.index.runs.length, 1);

  const latest = JSON.parse(await fs.readFile(result.latestJsonPath, 'utf8'));
  assert.equal(latest.runs[0].packageCode, 'quality-factory-p0');
});
