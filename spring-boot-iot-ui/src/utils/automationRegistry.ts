import acceptanceRegistrySource from '../../../config/automation/acceptance-registry.json';
import type {
  AcceptanceRegistryDocument,
  AcceptanceRegistryRunResult,
  AcceptanceRegistryRunSummary,
  AcceptanceRegistryScenario,
  AcceptanceRegistrySummary,
  ParsedAcceptanceRegistryRunSummary
} from '../types/automation';

function normalizeScenario(source: Partial<AcceptanceRegistryScenario>): AcceptanceRegistryScenario {
  return {
    id: String(source.id || ''),
    title: String(source.title || ''),
    module: source.module,
    docRef: source.docRef,
    runnerType: source.runnerType || 'browserPlan',
    scope: String(source.scope || 'delivery'),
    blocking: source.blocking || 'warning',
    dependsOn: Array.isArray(source.dependsOn) ? source.dependsOn.map((item) => String(item)) : [],
    inputs: source.inputs || {},
    evidence: Array.isArray(source.evidence) ? source.evidence : [],
    timeouts: source.timeouts || {},
    runner: source.runner || {}
  };
}

export function loadAcceptanceRegistryDocument(): AcceptanceRegistryDocument {
  const source = acceptanceRegistrySource as AcceptanceRegistryDocument;
  return {
    version: String(source.version || '1.0.0'),
    generatedAt: source.generatedAt,
    defaultTarget: source.defaultTarget || {},
    scenarios: Array.isArray(source.scenarios)
      ? source.scenarios.map((item) => normalizeScenario(item))
      : []
  };
}

export function buildRegistrySummary(scenarios: AcceptanceRegistryScenario[]): AcceptanceRegistrySummary {
  return {
    total: scenarios.length,
    blockerCount: scenarios.filter((item) => item.blocking === 'blocker').length,
    byRunner: scenarios.reduce<Record<string, number>>((accumulator, item) => {
      accumulator[item.runnerType] = (accumulator[item.runnerType] || 0) + 1;
      return accumulator;
    }, {})
  };
}

function normalizeRunResult(source: Partial<AcceptanceRegistryRunResult>): AcceptanceRegistryRunResult {
  return {
    scenarioId: String(source.scenarioId || ''),
    runnerType: source.runnerType,
    status: String(source.status || 'unknown'),
    blocking: source.blocking || 'warning',
    summary: source.summary,
    evidenceFiles: Array.isArray(source.evidenceFiles) ? source.evidenceFiles : [],
    details: source.details || {}
  };
}

export function parseRegistryRunSummary(
  payload: AcceptanceRegistryRunSummary
): ParsedAcceptanceRegistryRunSummary {
  const results = Array.isArray(payload.results)
    ? payload.results.map((item) => normalizeRunResult(item))
    : [];
  const failedResults = results.filter((item) => item.status !== 'passed');

  return {
    summary: {
      total: Number(payload.summary?.total || results.length),
      passed: Number(payload.summary?.passed || 0),
      failed: Number(payload.summary?.failed || failedResults.length)
    },
    results,
    reportPath: payload.reportPath,
    exitCode: payload.exitCode,
    failedScenarioIds: failedResults.map((item) => item.scenarioId),
    failedResults
  };
}

export function parseRegistryRunSummaryText(text: string): ParsedAcceptanceRegistryRunSummary {
  return parseRegistryRunSummary(JSON.parse(text) as AcceptanceRegistryRunSummary);
}
