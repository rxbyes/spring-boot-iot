import fs from 'node:fs/promises';
import path from 'node:path';

const REGISTRY_RUN_FILE_RE = /^registry-run-(.+)\.json$/;
const ACCEPTANCE_SEGMENTS = ['logs', 'acceptance'];

function cleanText(value) {
  return String(value || '').trim();
}

function asArray(value) {
  return Array.isArray(value) ? value : [];
}

function toCount(value) {
  return Number.isFinite(Number(value)) ? Number(value) : 0;
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

function compareStrings(left, right) {
  return cleanText(left).localeCompare(cleanText(right), 'en');
}

function toDisplayPath(filePath) {
  const normalized = path.resolve(filePath);
  const segments = normalized.split(path.sep);
  const startIndex = segments.findIndex(
    (segment, index) =>
      segment === ACCEPTANCE_SEGMENTS[0] &&
      segments[index + 1] === ACCEPTANCE_SEGMENTS[1]
  );
  if (startIndex >= 0) {
    return segments.slice(startIndex).join('/');
  }
  return normalized.replaceAll(path.sep, '/');
}

function normalizeSummary(summary = {}, results = []) {
  const failedFromResults = results.filter(
    (item) => cleanText(item.status).toLowerCase() !== 'passed'
  ).length;
  const total = toCount(summary.total) || results.length;
  const failed = toCount(summary.failed) || failedFromResults;
  const passed = toCount(summary.passed) || Math.max(0, total - failed);
  return { total, passed, failed };
}

function resolveStatus(summary) {
  return summary.failed > 0 ? 'failed' : 'passed';
}

function resolveRunId(payload = {}, fileName = '') {
  if (cleanText(payload.runId)) {
    return cleanText(payload.runId);
  }
  const match = fileName.match(REGISTRY_RUN_FILE_RE);
  return match ? cleanText(match[1]) : fileName;
}

async function pathExists(filePath) {
  try {
    await fs.access(filePath);
    return true;
  } catch {
    return false;
  }
}

function resolveEvidenceCategory(displayPath, reportPath) {
  if (displayPath === reportPath) {
    return 'run-summary';
  }
  const normalized = displayPath.toLowerCase();
  if (normalized.endsWith('.json')) {
    return 'json';
  }
  if (normalized.endsWith('.md') || normalized.endsWith('.markdown')) {
    return 'markdown';
  }
  if (
    normalized.endsWith('.txt') ||
    normalized.endsWith('.log') ||
    normalized.endsWith('.csv') ||
    normalized.endsWith('.yml') ||
    normalized.endsWith('.yaml')
  ) {
    return 'text';
  }
  return 'unknown';
}

async function resolveEvidenceItems({ payload, reportFilePath, resultsDir }) {
  const reportPath = toDisplayPath(reportFilePath);
  const items = [];
  const seen = new Set();

  async function addItem(rawPath, source) {
    const candidatePath = cleanText(rawPath);
    if (!candidatePath) {
      return;
    }
    const resolvedPath = path.isAbsolute(candidatePath)
      ? candidatePath
      : path.resolve(resultsDir, candidatePath.replace(/^logs\/acceptance\//, ''));
    if (!(await pathExists(resolvedPath))) {
      return;
    }
    const displayPath = toDisplayPath(resolvedPath);
    if (seen.has(displayPath)) {
      return;
    }
    seen.add(displayPath);
    items.push({
      path: displayPath,
      fileName: path.basename(displayPath),
      category: resolveEvidenceCategory(displayPath, reportPath),
      source
    });
  }

  await addItem(reportFilePath, 'report');
  for (const evidencePath of asArray(payload.relatedEvidenceFiles)) {
    await addItem(evidencePath, 'related');
  }
  for (const result of asArray(payload.results)) {
    for (const evidencePath of asArray(result?.evidenceFiles)) {
      await addItem(evidencePath, 'scenario');
    }
  }

  return items;
}

async function normalizeRunRecord({ filePath, resultsDir }) {
  const fileName = path.basename(filePath);
  const raw = await fs.readFile(filePath, 'utf8');
  const payload = JSON.parse(raw);
  const results = asArray(payload.results);
  const summary = normalizeSummary(payload.summary, results);
  const failedScenarioIds = results
    .filter((item) => cleanText(item.status).toLowerCase() !== 'passed')
    .map((item) => cleanText(item.scenarioId))
    .filter(Boolean);
  const runnerTypes = Array.from(
    new Set(results.map((item) => cleanText(item.runnerType)).filter(Boolean))
  ).sort(compareStrings);
  const evidenceItems = await resolveEvidenceItems({
    payload,
    reportFilePath: filePath,
    resultsDir
  });

  return {
    runId: resolveRunId(payload, fileName),
    updatedAt: cleanText(payload.updatedAt) || new Date().toISOString(),
    reportPath: toDisplayPath(filePath),
    status: resolveStatus(summary),
    summary,
    packageCode: cleanText(payload.options?.packageCode),
    environmentCode: cleanText(payload.options?.environmentCode),
    runnerTypes,
    failedScenarioIds,
    evidenceItems
  };
}

function buildFacets(runs) {
  const statuses = new Set();
  const runnerTypes = new Set();
  const packageCodes = new Set();
  const environmentCodes = new Set();

  runs.forEach((run) => {
    if (run.status) {
      statuses.add(run.status);
    }
    run.runnerTypes.forEach((item) => runnerTypes.add(item));
    if (run.packageCode) {
      packageCodes.add(run.packageCode);
    }
    if (run.environmentCode) {
      environmentCodes.add(run.environmentCode);
    }
  });

  return {
    statuses: Array.from(statuses).sort(compareStrings),
    runnerTypes: Array.from(runnerTypes).sort(compareStrings),
    packageCodes: Array.from(packageCodes).sort(compareStrings),
    environmentCodes: Array.from(environmentCodes).sort(compareStrings)
  };
}

export async function buildAutomationResultArchiveIndex({
  workspaceRoot = process.cwd(),
  resultsDir,
  generatedAt = new Date().toISOString()
} = {}) {
  const resolvedResultsDir = path.resolve(
    workspaceRoot,
    cleanText(resultsDir) || path.join('logs', 'acceptance')
  );
  const entries = (await fs.readdir(resolvedResultsDir, { withFileTypes: true }))
    .filter((entry) => entry.isFile() && REGISTRY_RUN_FILE_RE.test(entry.name))
    .map((entry) => entry.name)
    .sort(compareStrings);

  const runs = [];
  const skippedFiles = [];

  for (const fileName of entries) {
    const filePath = path.join(resolvedResultsDir, fileName);
    try {
      runs.push(await normalizeRunRecord({ filePath, resultsDir: resolvedResultsDir }));
    } catch (error) {
      skippedFiles.push({
        fileName,
        reason: error instanceof SyntaxError ? 'invalid-json' : 'invalid-structure'
      });
    }
  }

  runs.sort((left, right) => compareStrings(right.runId, left.runId));

  return {
    generatedAt,
    resultsDir: toDisplayPath(resolvedResultsDir),
    sourceSummary: {
      registryRunFiles: entries.length,
      indexedRuns: runs.length,
      skippedFiles: skippedFiles.length
    },
    facets: buildFacets(runs),
    runs,
    skippedFiles
  };
}

export function renderAutomationResultArchiveIndexMarkdown(index) {
  const recentRows = index.runs.length
    ? index.runs
        .slice(0, 10)
        .map(
          (run) =>
            `| ${run.runId} | ${run.status} | ${run.packageCode || '--'} | ${run.environmentCode || '--'} | ${run.runnerTypes.join(' / ') || '--'} | ${run.summary.total}/${run.summary.failed} |`
        )
    : ['| -- | -- | -- | -- | -- | -- |'];

  const skippedRows = index.skippedFiles.length
    ? index.skippedFiles.map((item) => `| ${item.fileName} | ${item.reason} |`)
    : ['| -- | -- |'];

  return [
    '# Automation Result Archive Index',
    '',
    `- Generated At: \`${index.generatedAt}\``,
    `- Results Dir: \`${index.resultsDir}\``,
    `- Registry Run Files: \`${index.sourceSummary.registryRunFiles}\``,
    `- Indexed Runs: \`${index.sourceSummary.indexedRuns}\``,
    `- Skipped Files: \`${index.sourceSummary.skippedFiles}\``,
    '',
    '## Facets',
    '',
    `- Statuses: ${index.facets.statuses.join(', ') || '--'}`,
    `- Runner Types: ${index.facets.runnerTypes.join(', ') || '--'}`,
    `- Package Codes: ${index.facets.packageCodes.join(', ') || '--'}`,
    `- Environment Codes: ${index.facets.environmentCodes.join(', ') || '--'}`,
    '',
    '## Recent Runs',
    '',
    '| Run ID | Status | Package | Environment | Runner Types | Summary |',
    '| --- | --- | --- | --- | --- | --- |',
    ...recentRows,
    '',
    '## Skipped Files',
    '',
    '| File | Reason |',
    '| --- | --- |',
    ...skippedRows,
    ''
  ].join('\n');
}

export async function writeAutomationResultArchiveArtifacts({
  outputDir,
  index,
  timestamp = createTimestamp()
}) {
  const resolvedOutputDir = path.resolve(outputDir);
  const latestJsonPath = path.join(
    resolvedOutputDir,
    'automation-result-index.latest.json'
  );
  const jsonPath = path.join(
    resolvedOutputDir,
    `automation-result-index-${timestamp}.json`
  );
  const markdownPath = path.join(
    resolvedOutputDir,
    `automation-result-index-${timestamp}.md`
  );

  await fs.mkdir(resolvedOutputDir, { recursive: true });
  await fs.writeFile(latestJsonPath, JSON.stringify(index, null, 2), 'utf8');
  await fs.writeFile(jsonPath, JSON.stringify(index, null, 2), 'utf8');
  await fs.writeFile(
    markdownPath,
    renderAutomationResultArchiveIndexMarkdown(index),
    'utf8'
  );

  return {
    latestJsonPath,
    jsonPath,
    markdownPath
  };
}
