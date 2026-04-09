import fs from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

import { createRunnerAdapters } from './acceptance-runner-adapters.mjs';
import {
  filterRegistryScenarios,
  loadAcceptanceRegistry
} from './acceptance-registry-lib.mjs';

function parseRegistryArgs(argv) {
  const options = {
    includeDeps: false,
    list: false
  };

  argv.forEach((arg) => {
    if (arg === '--list') {
      options.list = true;
      return;
    }
    if (arg === '--include-deps') {
      options.includeDeps = true;
      return;
    }
    if (arg.startsWith('--id=')) {
      options.id = arg.slice('--id='.length).trim();
      return;
    }
    if (arg.startsWith('--registry-path=')) {
      options.registryPath = arg.slice('--registry-path='.length).trim();
      return;
    }
    if (arg.startsWith('--module=')) {
      options.module = arg.slice('--module='.length).trim();
      return;
    }
    if (arg.startsWith('--scope=')) {
      options.scope = arg.slice('--scope='.length).trim();
      return;
    }
    if (arg.startsWith('--package-code=')) {
      options.packageCode = arg.slice('--package-code='.length).trim();
      return;
    }
    if (arg.startsWith('--environment-code=')) {
      options.environmentCode = arg.slice('--environment-code='.length).trim();
      return;
    }
    if (arg.startsWith('--account-template=')) {
      options.accountTemplate = arg.slice('--account-template='.length).trim();
      return;
    }
    if (arg.startsWith('--selected-modules=')) {
      options.selectedModules = arg.slice('--selected-modules='.length).trim();
      return;
    }
    if (arg.startsWith('--frontend-base-url=')) {
      options.frontendBaseUrl = arg.slice('--frontend-base-url='.length).trim();
      return;
    }
    if (arg.startsWith('--backend-base-url=')) {
      options.backendBaseUrl = arg.slice('--backend-base-url='.length).trim();
      return;
    }
    if (arg.startsWith('--expired-trace-id=')) {
      options.expiredTraceId = arg.slice('--expired-trace-id='.length).trim();
      return;
    }
    if (arg.startsWith('--output-prefix=')) {
      options.outputPrefix = arg.slice('--output-prefix='.length).trim();
      return;
    }
    throw new Error(`Unknown argument: ${arg}`);
  });

  return options;
}

function createRunId() {
  const now = new Date();
  const parts = [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, '0'),
    String(now.getDate()).padStart(2, '0'),
    String(now.getHours()).padStart(2, '0'),
    String(now.getMinutes()).padStart(2, '0'),
    String(now.getSeconds()).padStart(2, '0')
  ];
  return parts.join('');
}

function buildSummary(results) {
  return {
    total: results.length,
    passed: results.filter((item) => item.status === 'passed').length,
    failed: results.filter((item) => item.status !== 'passed').length
  };
}

function buildMarkdownReport({ runId, options, registry, results, summary }) {
  const lines = [
    '# Acceptance Registry Run',
    '',
    `- Run ID: \`${runId}\``,
    `- Scope: \`${options.scope || 'all'}\``,
    `- Module: \`${options.module || 'all'}\``,
    `- Registry version: \`${registry.version}\``,
    '',
    '## Summary',
    '',
    `- Total: \`${summary.total}\``,
    `- Passed: \`${summary.passed}\``,
    `- Failed: \`${summary.failed}\``,
    '',
    '## Results',
    '',
    '| Scenario | Runner | Status | Blocking | Summary |',
    '|---|---|---|---|---|'
  ];

  results.forEach((result) => {
    lines.push(
      `| ${result.scenarioId} | ${result.runnerType} | ${result.status} | ${result.blocking} | ${String(result.summary || '').replace(/\|/g, '\\|')} |`
    );
  });

  return lines.join('\n');
}

async function writeRunArtifacts({
  workspaceRoot,
  runId,
  options,
  registry,
  results,
  summary
}) {
  const outputPrefix = options.outputPrefix || 'registry-run';
  const logsDir = path.join(workspaceRoot, 'logs', 'acceptance');
  const jsonPath = path.join(logsDir, `${outputPrefix}-${runId}.json`);
  const mdPath = path.join(logsDir, `${outputPrefix}-${runId}.md`);
  await fs.mkdir(logsDir, { recursive: true });

  const payload = {
    runId,
    options,
    registryVersion: registry.version,
    summary,
    results
  };
  await fs.writeFile(jsonPath, JSON.stringify(payload, null, 2), 'utf8');
  await fs.writeFile(
    mdPath,
    buildMarkdownReport({ runId, options, registry, results, summary }),
    'utf8'
  );

  return {
    jsonPath,
    mdPath
  };
}

export async function runRegistryCli({
  argv = process.argv.slice(2),
  workspaceRoot = process.cwd(),
  registrySource,
  adapterOverrides
} = {}) {
  const options = parseRegistryArgs(argv);
  const registry = await loadAcceptanceRegistry({
    workspaceRoot,
    registryPath: options.registryPath,
    source: registrySource
  });

  if (options.list) {
    return {
      exitCode: 0,
      listed: filterRegistryScenarios(registry, options)
    };
  }

  const scenarios = filterRegistryScenarios(registry, options);
  const adapters = createRunnerAdapters({
    workspaceRoot,
    overrides: adapterOverrides
  });
  const runId = createRunId();
  const results = [];

  for (const scenario of scenarios) {
    const adapter = adapters[scenario.runnerType];
    if (typeof adapter !== 'function') {
      throw new Error(`Missing runner adapter for ${scenario.runnerType}`);
    }
    results.push(
      await adapter({
        workspaceRoot,
        scenario,
        registry,
        runId,
        options
      })
    );
  }

  const summary = buildSummary(results);
  const hasBlockingFailure = results.some(
    (item) => item.status !== 'passed' && item.blocking === 'blocker'
  );
  const artifacts = await writeRunArtifacts({
    workspaceRoot,
    runId,
    options,
    registry,
    results,
    summary
  });

  return {
    exitCode: hasBlockingFailure ? 1 : 0,
    runId,
    summary,
    results,
    reportPath: artifacts.jsonPath,
    markdownReportPath: artifacts.mdPath
  };
}

const currentFilePath = fileURLToPath(import.meta.url);

if (
  process.argv[1] &&
  path.resolve(process.argv[1]) === path.resolve(currentFilePath)
) {
  try {
    const result = await runRegistryCli();
    if (result.listed) {
      process.stdout.write(`${JSON.stringify(result.listed, null, 2)}\n`);
    } else {
      process.stdout.write(
        `${JSON.stringify(
          {
            runId: result.runId,
            summary: result.summary,
            reportPath: result.reportPath,
            markdownReportPath: result.markdownReportPath
          },
          null,
          2
        )}\n`
      );
    }
    process.exitCode = result.exitCode;
  } catch (error) {
    process.stderr.write(
      `${error instanceof Error ? error.stack || error.message : String(error)}\n`
    );
    process.exitCode = 1;
  }
}
