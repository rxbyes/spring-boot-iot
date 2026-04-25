import fs from 'node:fs/promises';
import path from 'node:path';

function cleanText(value) {
  return String(value || '').trim();
}

function toCount(value) {
  return Number.isFinite(Number(value)) ? Number(value) : 0;
}

function resolveStatus({ policyErrors, policyWarnings, diffStatus }) {
  if (policyErrors > 0 || diffStatus === 'regressed') {
    return 'failed';
  }
  if (policyWarnings > 0 || diffStatus === 'mixed') {
    return 'warning';
  }
  return 'passed';
}

function buildNextActions({
  status,
  policyErrors,
  policyWarnings,
  diffStatus,
  diffSkipped,
  diffSkippedReason
}) {
  const actions = [];

  if (policyErrors > 0) {
    actions.push(`Resolve ${policyErrors} blocking coverage policy error(s).`);
  }
  if (diffStatus === 'regressed') {
    actions.push('Resolve regressed coverage deltas before release readiness.');
  }
  if (status === 'warning' && policyWarnings > 0) {
    actions.push(`Review ${policyWarnings} coverage policy warning(s).`);
  }
  if (status === 'warning' && diffStatus === 'mixed') {
    actions.push('Review mixed coverage changes with the owner before release readiness.');
  }
  if (status === 'passed' && !diffSkipped) {
    actions.push('Coverage policy passed and no coverage regression was detected.');
  }
  if (diffSkipped) {
    actions.push(
      `Coverage diff skipped: ${cleanText(diffSkippedReason) || 'No comparable baseline was available.'}`
    );
  }
  actions.push(
    'Readiness evidence does not replace real-environment acceptance in application-dev.'
  );

  return actions;
}

export function buildAcceptanceReadinessReport({
  generatedAt = new Date().toISOString(),
  inputs = {},
  coverage,
  diff = null,
  skipReason = ''
} = {}) {
  if (!coverage || typeof coverage !== 'object' || !coverage.summary) {
    throw new Error('Acceptance readiness coverage snapshot is required.');
  }

  const policyErrors = toCount(coverage.policyEvaluation?.summary?.errors);
  const policyWarnings = toCount(coverage.policyEvaluation?.summary?.warnings);
  const diffPayload = diff || {
    skipped: true,
    skippedReason: cleanText(skipReason) || 'Coverage diff was skipped.'
  };
  const diffStatus = cleanText(diffPayload.summary?.status);
  const status = resolveStatus({
    policyErrors,
    policyWarnings,
    diffStatus
  });

  return {
    generatedAt,
    status,
    exitCode: status === 'failed' ? 1 : 0,
    inputs,
    coverage,
    diff: diffPayload,
    nextActions: buildNextActions({
      status,
      policyErrors,
      policyWarnings,
      diffStatus,
      diffSkipped: Boolean(diffPayload.skipped),
      diffSkippedReason: diffPayload.skippedReason
    })
  };
}

export function renderAcceptanceReadinessMarkdown(report) {
  const diffLines = report.diff?.skipped
    ? [
        `- Coverage Diff: skipped`,
        `- Skip Reason: ${report.diff.skippedReason || 'No comparable baseline was available.'}`
      ]
    : [
        `- Coverage Diff JSON: \`${report.diff.jsonPath}\``,
        `- Coverage Diff Markdown: \`${report.diff.markdownPath}\``,
        `- Coverage Diff Status: \`${report.diff.summary?.status || 'unknown'}\``
      ];

  const lines = [
    '# Acceptance Readiness',
    '',
    `- Status: \`${report.status}\``,
    `- Exit Code: \`${report.exitCode}\``,
    `- Generated At: \`${report.generatedAt}\``,
    '',
    '## Coverage Summary',
    '',
    `- Coverage JSON: \`${report.coverage.jsonPath}\``,
    `- Coverage Markdown: \`${report.coverage.markdownPath}\``,
    `- Scenarios: \`${toCount(report.coverage.summary?.totalScenarios)}\``,
    `- Packages: \`${toCount(report.coverage.summary?.totalPackages)}\``,
    `- Missing Scenario Refs: \`${toCount(report.coverage.summary?.missingScenarioRefs)}\``,
    `- Metadata Missing Scenarios: \`${toCount(report.coverage.summary?.metadataMissingScenarios)}\``,
    '',
    '## Policy Summary',
    '',
    `- Policy Status: \`${report.coverage.policyEvaluation?.status || 'not-provided'}\``,
    `- Policy Errors: \`${toCount(report.coverage.policyEvaluation?.summary?.errors)}\``,
    `- Policy Warnings: \`${toCount(report.coverage.policyEvaluation?.summary?.warnings)}\``,
    '',
    '## Coverage Diff',
    '',
    ...diffLines,
    '',
    '## Next Actions',
    '',
    ...report.nextActions.map((action) => `- ${action}`),
    ''
  ];

  return `${lines.join('\n')}\n`;
}

export async function writeAcceptanceReadinessArtifacts({
  report,
  jsonPath,
  markdownPath
}) {
  await fs.mkdir(path.dirname(jsonPath), { recursive: true });
  await fs.mkdir(path.dirname(markdownPath), { recursive: true });
  await fs.writeFile(jsonPath, JSON.stringify(report, null, 2), 'utf8');
  await fs.writeFile(markdownPath, renderAcceptanceReadinessMarkdown(report), 'utf8');
}
