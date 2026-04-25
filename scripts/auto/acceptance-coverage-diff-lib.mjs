import fs from 'node:fs/promises';
import path from 'node:path';

function asArray(value) {
  return Array.isArray(value) ? value : [];
}

function cleanText(value) {
  return String(value || '').trim();
}

function requireCoverageMatrix(matrix, label) {
  if (!matrix || typeof matrix !== 'object' || Array.isArray(matrix)) {
    throw new Error(`${label} coverage matrix must be a JSON object.`);
  }
  if (!matrix.summary || typeof matrix.summary !== 'object') {
    throw new Error(`${label} coverage matrix is missing summary.`);
  }
  if (!Array.isArray(matrix.scenarios)) {
    throw new Error(`${label} coverage matrix is missing scenarios array.`);
  }
  if (!Array.isArray(matrix.packages)) {
    throw new Error(`${label} coverage matrix is missing packages array.`);
  }
}

function scenarioIdOf(row) {
  return cleanText(row.scenarioId || row.id);
}

function packageCodeOf(row) {
  return cleanText(row.packageCode);
}

function sortedSetDifference(left, right) {
  const rightSet = new Set(right);
  return left.filter((item) => !rightSet.has(item)).sort();
}

function extractScenarioIds(matrix) {
  return asArray(matrix.scenarios).map(scenarioIdOf).filter(Boolean).sort();
}

function extractPackageCodes(matrix) {
  return asArray(matrix.packages).map(packageCodeOf).filter(Boolean).sort();
}

function bucketTotal(index, key) {
  const value = index?.[key]?.total;
  return Number.isFinite(Number(value)) ? Number(value) : 0;
}

function diffCountIndex(baselineIndex = {}, currentIndex = {}) {
  const keys = Array.from(
    new Set([...Object.keys(baselineIndex || {}), ...Object.keys(currentIndex || {})])
  ).sort();
  return Object.fromEntries(
    keys.map((key) => {
      const baseline = bucketTotal(baselineIndex, key);
      const current = bucketTotal(currentIndex, key);
      return [key, { baseline, current, delta: current - baseline }];
    })
  );
}

function summaryNumber(matrix, key) {
  const value = matrix.summary?.[key];
  return Number.isFinite(Number(value)) ? Number(value) : 0;
}

function policySnapshot(matrix) {
  if (!matrix.policyEvaluation?.summary) {
    return {
      status: 'not-provided',
      errors: null,
      warnings: null
    };
  }
  return {
    status: cleanText(matrix.policyEvaluation.status) || 'unknown',
    errors: Number(matrix.policyEvaluation.summary.errors || 0),
    warnings: Number(matrix.policyEvaluation.summary.warnings || 0)
  };
}

function nullableDelta(left, right) {
  return left === null || right === null ? null : right - left;
}

function isPositive(value) {
  return value !== null && value > 0;
}

function isNegative(value) {
  return value !== null && value < 0;
}

function classifyStatus(summary, changes) {
  const p0Delta = changes.coverageByPriority.P0?.delta || 0;
  const regressed =
    isPositive(summary.policyErrorsDelta) ||
    summary.missingScenarioRefsDelta > 0 ||
    summary.metadataMissingScenariosDelta > 0 ||
    p0Delta < 0;
  if (regressed) {
    return 'regressed';
  }
  const improved =
    isNegative(summary.policyErrorsDelta) ||
    isNegative(summary.policyWarningsDelta) ||
    summary.missingScenarioRefsDelta < 0 ||
    summary.metadataMissingScenariosDelta < 0 ||
    summary.scenarioDelta > 0 ||
    summary.packageDelta > 0;
  const nonBlockingRegression =
    isPositive(summary.policyWarningsDelta) ||
    summary.unreferencedScenariosDelta > 0 ||
    changes.scenarios.removed.length > 0 ||
    changes.packages.removed.length > 0;
  if (improved && nonBlockingRegression) {
    return 'mixed';
  }
  if (improved) {
    return 'improved';
  }
  return 'unchanged';
}

export function buildCoverageDiff({
  baseline,
  current,
  baselinePath = '',
  currentPath = '',
  generatedAt = new Date().toISOString()
} = {}) {
  requireCoverageMatrix(baseline, 'Baseline');
  requireCoverageMatrix(current, 'Current');
  const baselineScenarios = extractScenarioIds(baseline);
  const currentScenarios = extractScenarioIds(current);
  const baselinePackages = extractPackageCodes(baseline);
  const currentPackages = extractPackageCodes(current);
  const baselinePolicy = policySnapshot(baseline);
  const currentPolicy = policySnapshot(current);
  const changes = {
    scenarios: {
      added: sortedSetDifference(currentScenarios, baselineScenarios),
      removed: sortedSetDifference(baselineScenarios, currentScenarios)
    },
    packages: {
      added: sortedSetDifference(currentPackages, baselinePackages),
      removed: sortedSetDifference(baselinePackages, currentPackages)
    },
    coverageByPriority: diffCountIndex(
      baseline.coverageByPriority,
      current.coverageByPriority
    ),
    coverageByRunnerType: diffCountIndex(
      baseline.coverageByRunnerType,
      current.coverageByRunnerType
    ),
    coverageByOwnerDomain: diffCountIndex(
      baseline.coverageByOwnerDomain,
      current.coverageByOwnerDomain
    )
  };
  const summary = {
    scenarioDelta: currentScenarios.length - baselineScenarios.length,
    packageDelta: currentPackages.length - baselinePackages.length,
    missingScenarioRefsDelta:
      summaryNumber(current, 'missingScenarioRefs') -
      summaryNumber(baseline, 'missingScenarioRefs'),
    unreferencedScenariosDelta:
      summaryNumber(current, 'unreferencedScenarios') -
      summaryNumber(baseline, 'unreferencedScenarios'),
    metadataMissingScenariosDelta:
      summaryNumber(current, 'metadataMissingScenarios') -
      summaryNumber(baseline, 'metadataMissingScenarios'),
    policyErrorsDelta: nullableDelta(baselinePolicy.errors, currentPolicy.errors),
    policyWarningsDelta: nullableDelta(
      baselinePolicy.warnings,
      currentPolicy.warnings
    )
  };
  summary.status = classifyStatus(summary, changes);
  return {
    generatedAt,
    baselinePath,
    currentPath,
    summary,
    changes,
    policyEvaluation: {
      baseline: baselinePolicy,
      current: currentPolicy
    }
  };
}

function escapeTableText(value) {
  return cleanText(value).replace(/\|/g, '\\|');
}

function renderDelta(value) {
  if (value === null) {
    return 'n/a';
  }
  return value > 0 ? `+${value}` : String(value);
}

function renderList(items) {
  return items.length === 0
    ? ['- None']
    : items.map((item) => `- \`${escapeTableText(item)}\``);
}

function renderIndexDiff(title, index, keyLabel) {
  const lines = [
    `## ${title}`,
    '',
    `| ${keyLabel} | Baseline | Current | Delta |`,
    '|---|---:|---:|---:|'
  ];
  Object.entries(index).forEach(([key, value]) => {
    lines.push(
      `| ${escapeTableText(key)} | ${value.baseline} | ${value.current} | ${renderDelta(
        value.delta
      )} |`
    );
  });
  if (Object.keys(index).length === 0) {
    lines.push('| None | 0 | 0 | 0 |');
  }
  return lines;
}

function nextActionFor(status) {
  if (status === 'regressed') {
    return 'Coverage regressed. Resolve blocking deltas before release readiness.';
  }
  if (status === 'mixed') {
    return 'Coverage changed in both directions. Confirm non-blocking regressions with the owner.';
  }
  if (status === 'improved') {
    return 'Coverage improved. Keep this diff as release readiness evidence.';
  }
  return 'Coverage is unchanged. Keep this diff as a baseline comparison record.';
}

export function renderCoverageDiffMarkdown(diff) {
  const lines = [
    '# Acceptance Coverage Diff',
    '',
    `- Generated At: \`${diff.generatedAt}\``,
    `- Baseline: \`${diff.baselinePath || 'not provided'}\``,
    `- Current: \`${diff.currentPath || 'not provided'}\``,
    `- Status: \`${diff.summary.status}\``,
    '',
    '## Summary',
    '',
    '| Metric | Delta |',
    '|---|---:|',
    `| Scenario count | ${renderDelta(diff.summary.scenarioDelta)} |`,
    `| Package count | ${renderDelta(diff.summary.packageDelta)} |`,
    `| Missing scenario refs | ${renderDelta(
      diff.summary.missingScenarioRefsDelta
    )} |`,
    `| Unreferenced scenarios | ${renderDelta(
      diff.summary.unreferencedScenariosDelta
    )} |`,
    `| Metadata missing scenarios | ${renderDelta(
      diff.summary.metadataMissingScenariosDelta
    )} |`,
    `| Policy errors | ${renderDelta(diff.summary.policyErrorsDelta)} |`,
    `| Policy warnings | ${renderDelta(diff.summary.policyWarningsDelta)} |`,
    '',
    '## Added Scenarios',
    '',
    ...renderList(diff.changes.scenarios.added),
    '',
    '## Removed Scenarios',
    '',
    ...renderList(diff.changes.scenarios.removed),
    '',
    '## Added Packages',
    '',
    ...renderList(diff.changes.packages.added),
    '',
    '## Removed Packages',
    '',
    ...renderList(diff.changes.packages.removed),
    '',
    ...renderIndexDiff(
      'Coverage By Priority',
      diff.changes.coverageByPriority,
      'Priority'
    ),
    '',
    ...renderIndexDiff(
      'Coverage By Runner Type',
      diff.changes.coverageByRunnerType,
      'Runner Type'
    ),
    '',
    ...renderIndexDiff(
      'Coverage By Owner Domain',
      diff.changes.coverageByOwnerDomain,
      'Owner Domain'
    ),
    '',
    '## Policy Evaluation',
    '',
    '| Side | Status | Errors | Warnings |',
    '|---|---|---:|---:|',
    `| Baseline | ${escapeTableText(
      diff.policyEvaluation.baseline.status
    )} | ${diff.policyEvaluation.baseline.errors ?? 'n/a'} | ${
      diff.policyEvaluation.baseline.warnings ?? 'n/a'
    } |`,
    `| Current | ${escapeTableText(diff.policyEvaluation.current.status)} | ${
      diff.policyEvaluation.current.errors ?? 'n/a'
    } | ${diff.policyEvaluation.current.warnings ?? 'n/a'} |`,
    '',
    '## Next Actions',
    '',
    `- ${nextActionFor(diff.summary.status)}`,
    ''
  ];
  return `${lines.join('\n')}\n`;
}

export async function readCoverageMatrix(filePath) {
  try {
    return JSON.parse(await fs.readFile(filePath, 'utf8'));
  } catch (error) {
    throw new Error(`Unable to read coverage matrix ${filePath}: ${error.message}`);
  }
}

export async function findLatestCoverageMatrixFiles({
  workspaceRoot = process.cwd(),
  logsDir = path.join(workspaceRoot, 'logs', 'acceptance')
} = {}) {
  const entries = await fs.readdir(logsDir);
  const files = entries
    .filter((entry) => /^acceptance-coverage-\d{14}\.json$/.test(entry))
    .sort()
    .map((entry) => path.join(logsDir, entry));
  if (files.length < 2) {
    throw new Error(
      'At least two acceptance coverage matrix JSON files are required. Run generate-acceptance-coverage.mjs twice or pass explicit paths.'
    );
  }
  return {
    baselinePath: files[files.length - 2],
    currentPath: files[files.length - 1]
  };
}

export async function writeCoverageDiffArtifacts({
  diff,
  jsonPath,
  markdownPath
}) {
  await fs.mkdir(path.dirname(jsonPath), { recursive: true });
  await fs.mkdir(path.dirname(markdownPath), { recursive: true });
  await fs.writeFile(jsonPath, JSON.stringify(diff, null, 2), 'utf8');
  await fs.writeFile(markdownPath, renderCoverageDiffMarkdown(diff), 'utf8');
}
