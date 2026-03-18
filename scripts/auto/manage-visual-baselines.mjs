import path from 'node:path';
import process from 'node:process';
import { access, copyFile, mkdir, readFile, writeFile } from 'node:fs/promises';

import {
  collectVisualAssertionRecords,
  resolveVisualRecordCategory,
  summarizeVisualAssertionRecords
} from './visual-regression-report.mjs';

function pad(value) {
  return String(value).padStart(2, '0');
}

function formatTimestamp(date) {
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
    pad(date.getHours()),
    pad(date.getMinutes()),
    pad(date.getSeconds())
  ].join('');
}

function toWorkspaceRelative(workspaceRoot, targetPath) {
  return path.relative(workspaceRoot, targetPath).replace(/\\/g, '/');
}

async function pathExists(targetPath) {
  try {
    await access(targetPath);
    return true;
  } catch {
    return false;
  }
}

function parseCsv(value, fallback = []) {
  if (!value) {
    return [...fallback];
  }
  return String(value)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function parseArgs(argv) {
  const args = {
    input: '',
    mode: 'audit',
    statuses: [],
    scenario: '',
    label: '',
    apply: false,
    help: false
  };

  for (const arg of argv) {
    if (arg === '--help' || arg === '-h') {
      args.help = true;
      continue;
    }
    if (arg === '--apply') {
      args.apply = true;
      continue;
    }
    if (arg.startsWith('--input=')) {
      args.input = arg.slice('--input='.length).trim();
      continue;
    }
    if (arg.startsWith('--mode=')) {
      args.mode = arg.slice('--mode='.length).trim() || 'audit';
      continue;
    }
    if (arg.startsWith('--status=')) {
      args.statuses = parseCsv(arg.slice('--status='.length));
      continue;
    }
    if (arg.startsWith('--scenario=')) {
      args.scenario = arg.slice('--scenario='.length).trim();
      continue;
    }
    if (arg.startsWith('--label=')) {
      args.label = arg.slice('--label='.length).trim();
      continue;
    }
    throw new Error(`Unknown argument: ${arg}`);
  }

  return args;
}

function printHelp() {
  process.stdout.write(
    [
      'Usage:',
      '  node scripts/auto/manage-visual-baselines.mjs --input=logs/acceptance/config-browser-visual-manifest-20260317120000.json [--mode=audit|promote] [--status=missing,mismatch] [--scenario=device-workbench] [--label=首屏] [--apply]',
      '',
      'Options:',
      '  --input=...      Read a visual manifest JSON or browser results JSON.',
      '  --mode=...       audit (default) or promote.',
      '  --status=...     Comma-separated categories: passed,mismatch,missing,updated.',
      '  --scenario=...   Filter by scenario key/name substring.',
      '  --label=...      Filter by step label substring.',
      '  --apply          Required for mode=promote to actually copy actual -> baseline.',
      '  --help           Print this message.',
      '',
      'Examples:',
      '  node scripts/auto/manage-visual-baselines.mjs --input=logs/acceptance/config-browser-visual-manifest-20260317120000.json',
      '  node scripts/auto/manage-visual-baselines.mjs --input=logs/acceptance/config-browser-visual-manifest-20260317120000.json --mode=promote --status=missing,mismatch --apply'
    ].join('\n')
  );
}

function normalizeVisualRecords(payload) {
  if (Array.isArray(payload?.visualResults)) {
    return payload.visualResults.map((record) => ({
      ...record,
      category: record.category || resolveVisualRecordCategory(record)
    }));
  }

  if (Array.isArray(payload?.scenarios)) {
    return collectVisualAssertionRecords(payload.scenarios).map((record) => ({
      ...record,
      category: record.category || resolveVisualRecordCategory(record)
    }));
  }

  return [];
}

function filterRecords(records, args) {
  const scenarioKeyword = args.scenario.trim().toLowerCase();
  const labelKeyword = args.label.trim().toLowerCase();
  const statuses = args.statuses.length
    ? new Set(args.statuses.map((item) => item.toLowerCase()))
    : new Set(args.mode === 'promote' ? ['missing', 'mismatch'] : []);

  return records.filter((record) => {
    if (statuses.size > 0 && !statuses.has(String(record.category || '').toLowerCase())) {
      return false;
    }

    const scenarioText = `${record.scenarioKey || ''} ${record.scenarioName || ''}`.toLowerCase();
    if (scenarioKeyword && !scenarioText.includes(scenarioKeyword)) {
      return false;
    }

    const labelText = `${record.stepId || ''} ${record.label || ''}`.toLowerCase();
    if (labelKeyword && !labelText.includes(labelKeyword)) {
      return false;
    }

    return true;
  });
}

function createGovernanceArtifacts(workspaceRoot, inputPath, runTimestamp) {
  const logsRoot = path.join(workspaceRoot, 'logs', 'acceptance');
  const inputStem = path.basename(inputPath, path.extname(inputPath));

  return {
    logsRoot,
    jsonPath: path.join(logsRoot, `${inputStem}-baseline-governance-${runTimestamp}.json`),
    reportPath: path.join(logsRoot, `${inputStem}-baseline-governance-${runTimestamp}.md`)
  };
}

function buildMarkdownReport(report) {
  const lines = [
    '# Visual Baseline Governance Report',
    '',
    `- Run timestamp: \`${report.runTimestamp}\``,
    `- Input: \`${report.inputPath}\``,
    `- Mode: \`${report.mode}\``,
    `- Apply changes: \`${report.apply}\``,
    `- Selected statuses: \`${report.selectedStatuses.join(', ') || 'all'}\``,
    '',
    '## Summary',
    '',
    `- Total visual records: \`${report.totalVisualRecords}\``,
    `- Selected records: \`${report.selectedRecords}\``,
    `- Promoted records: \`${report.promotedRecords}\``,
    `- Skipped records: \`${report.skippedRecords}\``,
    '',
    '## Records',
    '',
    '| Scenario | Step | Category | Baseline | Actual | Result |',
    '|---|---|---|---|---|---|'
  ];

  for (const item of report.records) {
    lines.push(
      `| ${item.scenarioKey || item.scenarioName || 'n/a'} | ${item.stepId || item.label || 'n/a'} | ${item.category} | \`${item.baselinePath || 'n/a'}\` | \`${item.actualPath || 'n/a'}\` | ${item.result} |`
    );
  }

  lines.push(
    '',
    '## Files',
    '',
    `- JSON: \`${report.output.jsonPath}\``,
    `- Markdown: \`${report.output.reportPath}\``
  );

  return lines.join('\n');
}

export async function runBaselineManager(argv = process.argv.slice(2)) {
  const args = parseArgs(argv);
  if (args.help || !args.input) {
    printHelp();
    return {
      exitCode: args.help ? 0 : 1
    };
  }

  const workspaceRoot = process.cwd();
  const inputAbsolutePath = path.isAbsolute(args.input) ? args.input : path.resolve(workspaceRoot, args.input);
  const raw = await readFile(inputAbsolutePath, 'utf8');
  const payload = JSON.parse(raw);
  const visualRecords = normalizeVisualRecords(payload);
  const selectedRecords = filterRecords(visualRecords, args);
  const runTimestamp = formatTimestamp(new Date());
  const artifacts = createGovernanceArtifacts(workspaceRoot, inputAbsolutePath, runTimestamp);

  await mkdir(artifacts.logsRoot, { recursive: true });

  const governanceRecords = [];
  let promotedRecords = 0;
  let skippedRecords = 0;

  for (const record of selectedRecords) {
    const baselineAbsolutePath = record.baselinePath
      ? path.resolve(workspaceRoot, record.baselinePath)
      : '';
    const actualAbsolutePath = record.actualPath
      ? path.resolve(workspaceRoot, record.actualPath)
      : '';

    if (args.mode !== 'promote') {
      governanceRecords.push({
        ...record,
        result: 'audited'
      });
      continue;
    }

    if (!baselineAbsolutePath || !actualAbsolutePath) {
      skippedRecords += 1;
      governanceRecords.push({
        ...record,
        result: 'skipped: missing baselinePath or actualPath'
      });
      continue;
    }

    if (!(await pathExists(actualAbsolutePath))) {
      skippedRecords += 1;
      governanceRecords.push({
        ...record,
        result: 'skipped: actual screenshot not found'
      });
      continue;
    }

    const baselineExisted = await pathExists(baselineAbsolutePath);
    if (args.apply) {
      await mkdir(path.dirname(baselineAbsolutePath), { recursive: true });
      await copyFile(actualAbsolutePath, baselineAbsolutePath);
      promotedRecords += 1;
      governanceRecords.push({
        ...record,
        result: baselineExisted ? 'promoted: updated baseline' : 'promoted: created baseline'
      });
    } else {
      governanceRecords.push({
        ...record,
        result: baselineExisted ? 'dry-run: would update baseline' : 'dry-run: would create baseline'
      });
    }
  }

  const report = {
    runTimestamp,
    inputPath: toWorkspaceRelative(workspaceRoot, inputAbsolutePath),
    mode: args.mode,
    apply: args.apply,
    selectedStatuses: args.statuses.length ? args.statuses : args.mode === 'promote' ? ['missing', 'mismatch'] : [],
    totalVisualRecords: visualRecords.length,
    selectedRecords: selectedRecords.length,
    promotedRecords,
    skippedRecords,
    visualSummary: summarizeVisualAssertionRecords(visualRecords),
    records: governanceRecords,
    output: {
      jsonPath: toWorkspaceRelative(workspaceRoot, artifacts.jsonPath),
      reportPath: toWorkspaceRelative(workspaceRoot, artifacts.reportPath)
    }
  };

  await writeFile(artifacts.jsonPath, JSON.stringify(report, null, 2), 'utf8');
  await writeFile(artifacts.reportPath, buildMarkdownReport(report), 'utf8');
  process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);

  return {
    exitCode: 0,
    report
  };
}

if (path.resolve(process.argv[1] || '') === path.resolve(new URL(import.meta.url).pathname)) {
  try {
    const result = await runBaselineManager();
    process.exitCode = result.exitCode || 0;
  } catch (error) {
    process.stderr.write(`${error instanceof Error ? error.stack || error.message : String(error)}\n`);
    process.exitCode = 1;
  }
}
