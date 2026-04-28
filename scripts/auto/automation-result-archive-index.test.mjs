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
  scenarioEvidenceFiles = [],
  summaryText,
  details
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
        summary: summaryText || (failed ? '场景失败' : '场景通过'),
        evidenceFiles: scenarioEvidenceFiles,
        details
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

test('buildAutomationResultArchiveIndex expands screenshot directories into image evidence items', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'automation-result-archive-index-screenshots-')
  );
  const resultsDir = path.join(workspaceRoot, 'logs', 'acceptance');
  const screenshotsDir = path.join(resultsDir, 'quality-factory-browser-screenshots-20260428134458');

  await writeJson(
    path.join(resultsDir, 'registry-run-20260428134455.json'),
    createRun({
      runId: '20260428134455',
      packageCode: 'automation-results-p1',
      environmentCode: 'dev',
      scenarioEvidenceFiles: [
        'logs/acceptance/quality-factory-browser-screenshots-20260428134458'
      ]
    })
  );
  await fs.mkdir(screenshotsDir, { recursive: true });
  await fs.writeFile(path.join(screenshotsDir, 'automation-results-workbench-pass.png'), new Uint8Array([1, 2, 3]));
  await writeText(path.join(screenshotsDir, 'readme.txt'), 'not a screenshot');

  const index = await buildAutomationResultArchiveIndex({
    workspaceRoot,
    resultsDir
  });

  const evidenceItems = index.runs[0].evidenceItems;
  assert.equal(
    evidenceItems.some(
      (item) =>
        item.path === 'logs/acceptance/quality-factory-browser-screenshots-20260428134458'
    ),
    false
  );
  assert.deepEqual(
    evidenceItems
      .filter((item) => item.category === 'image')
      .map((item) => item.path),
    [
      'logs/acceptance/quality-factory-browser-screenshots-20260428134458/automation-results-workbench-pass.png'
    ]
  );
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

test('buildAutomationResultArchiveIndex adds scenario and module diagnosis fields', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'automation-result-archive-index-diagnosis-')
  );
  const resultsDir = path.join(workspaceRoot, 'logs', 'acceptance');

  await writeJson(path.join(resultsDir, 'registry-run-20260426010000.json'), {
    runId: '20260426010000',
    updatedAt: '2026-04-26T01:00:00+08:00',
    options: {
      packageCode: 'quality-factory-p0',
      environmentCode: 'dev'
    },
    summary: {
      total: 3,
      passed: 0,
      failed: 3
    },
    results: [
      {
        scenarioId: 'product.contract.compare',
        runnerType: 'apiSmoke',
        status: 'failed',
        blocking: 'blocker',
        summary: 'compare 接口返回 500',
        evidenceFiles: ['logs/acceptance/compare-500.log'],
        details: {
          stepLabel: '调用 compare 接口',
          apiRef: 'POST /api/device/product/1/compare'
        }
      },
      {
        scenarioId: 'product.version.ledger',
        runnerType: 'apiSmoke',
        status: 'failed',
        blocking: 'blocker',
        summary: '版本台账接口响应缺字段',
        evidenceFiles: ['logs/acceptance/version-ledger.log'],
        details: {
          stepLabel: '读取版本台账',
          apiRef: 'GET /api/device/product/1/releases'
        }
      },
      {
        scenarioId: 'quality-workbench.dialog',
        runnerType: 'browserPlan',
        status: 'failed',
        blocking: 'warning',
        summary: '发布弹窗未出现',
        evidenceFiles: ['logs/acceptance/dialog-missing.md'],
        details: {
          pageAction: '点击发布按钮'
        }
      }
    ]
  });
  await writeText(path.join(resultsDir, 'compare-500.log'), 'HTTP 500 compare failed');
  await writeText(path.join(resultsDir, 'version-ledger.log'), 'response missing releaseBatchId field');
  await writeText(path.join(resultsDir, 'dialog-missing.md'), 'selector not found for publish dialog');

  const index = await buildAutomationResultArchiveIndex({ workspaceRoot, resultsDir });

  assert.equal(index.runs[0].failureSummary.primaryCategory, '接口');
  assert.deepEqual(index.runs[0].failureSummary.countsByCategory, {
    接口: 2,
    UI: 1
  });
  assert.equal(index.runs[0].failedModules[0].diagnosis.category, '接口');
  assert.match(index.runs[0].failedModules[0].diagnosis.reason, /命中接口问题|接口问题占多数/);
  assert.equal(index.runs[0].failedScenarios[0].diagnosis.category, '接口');
  assert.match(index.runs[0].failedScenarios[0].diagnosis.reason, /500|响应异常/);
  assert.equal(index.runs[0].failedScenarios[2].diagnosis.category, 'UI');
});

test('buildAutomationResultArchiveIndex falls back to 其他 when no diagnosis rule matches', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'automation-result-archive-index-other-')
  );
  const resultsDir = path.join(workspaceRoot, 'logs', 'acceptance');

  await writeJson(
    path.join(resultsDir, 'registry-run-20260426013000.json'),
    createRun({
      runId: '20260426013000',
      packageCode: 'quality-factory-p0',
      environmentCode: 'dev',
      status: 'failed',
      failedScenarioId: 'custom.unmatched',
      summaryText: 'unexpected mismatch happened',
      scenarioEvidenceFiles: ['logs/acceptance/custom-unmatched.txt']
    })
  );
  await writeText(path.join(resultsDir, 'custom-unmatched.txt'), 'free form message without known keyword');

  const index = await buildAutomationResultArchiveIndex({ workspaceRoot, resultsDir });

  assert.equal(index.runs[0].failedScenarios[0].diagnosis.category, '其他');
  assert.match(index.runs[0].failedScenarios[0].diagnosis.reason, /未命中已知规则/);
});
