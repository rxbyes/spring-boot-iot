import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

import { runBrowserAcceptance } from './browser-acceptance-core.mjs';
import { appendBrowserIssues } from './browser-issue-log.mjs';
import {
  createExecutableScenarios,
  plannedScenarioBacklog
} from './browser-acceptance-scenarios.mjs';

const __filename = fileURLToPath(import.meta.url);

function parseArgs(argv) {
  const args = {
    dryRun: false,
    noAppendIssues: false,
    scopes: undefined,
    failScopes: undefined,
    help: false
  };

  for (const arg of argv) {
    if (arg === '--help' || arg === '-h') {
      args.help = true;
      continue;
    }
    if (arg === '--dry-run') {
      args.dryRun = true;
      continue;
    }
    if (arg === '--no-append-issues') {
      args.noAppendIssues = true;
      continue;
    }
    if (arg.startsWith('--scopes=')) {
      args.scopes = arg
        .slice('--scopes='.length)
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean);
      continue;
    }
    if (arg.startsWith('--fail-scopes=')) {
      args.failScopes = arg
        .slice('--fail-scopes='.length)
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean);
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
      '  node scripts/auto/run-browser-acceptance.mjs [--dry-run] [--no-append-issues] [--scopes=delivery,baseline] [--fail-scopes=delivery]',
      '',
      'Options:',
      '  --dry-run           Print the active scenario plan without opening a browser.',
      '  --no-append-issues  Do not append this run to docs/22-automation-test-issues-20260316.md.',
      '  --scopes=...        Comma-separated scenario scopes to execute. Default: delivery,baseline.',
      '  --fail-scopes=...   Comma-separated scopes that should set a non-zero exit code. Default: delivery.'
    ].join('\n')
  );
}

export async function runCli(argv = process.argv.slice(2)) {
  const args = parseArgs(argv);
  if (args.help) {
    printHelp();
    return {
      dryRun: true,
      exitCode: 0
    };
  }

  const result = await runBrowserAcceptance({
    createScenarios: createExecutableScenarios,
    plannedScenarios: plannedScenarioBacklog,
    options: {
      dryRun: args.dryRun,
      appendIssues: !args.noAppendIssues && !args.dryRun,
      scenarioScopes: args.scopes,
      failScopes: args.failScopes
    }
  });

  if (result.dryRun) {
    process.stdout.write(
      `${JSON.stringify(
        {
          dryRun: true,
          scopes: result.options.scenarioScopes,
          scenarios: result.executableScenarios,
          plannedScenarios: result.plannedScenarios
        },
        null,
        2
      )}\n`
    );
    return {
      ...result,
      exitCode: 0
    };
  }

  if (result.options.appendIssues) {
    await appendBrowserIssues({
      issueDocPath: result.options.issueDocPath,
      summary: result.summary,
      scenarioResults: result.scenarioResults,
      commandHint: result.options.commandHint,
      workspaceRoot: result.options.workspaceRoot
    });
  }

  return result;
}

if (path.resolve(process.argv[1] || '') === path.resolve(__filename)) {
  try {
    const result = await runCli();
    process.exitCode = result.exitCode || 0;
  } catch (error) {
    process.stderr.write(`${error instanceof Error ? error.stack || error.message : String(error)}\n`);
    process.exitCode = 1;
  }
}
