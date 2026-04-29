import acceptanceRegistrySource from '../../../config/automation/acceptance-registry.json';
import type {
  AcceptanceRegistryDocument,
  AutomationFailureDiagnosis,
  AutomationResultFailedModule,
  AutomationResultFailedScenario,
  AutomationResultFailureSummary,
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

function normalizeDiagnosis(source?: Partial<AutomationFailureDiagnosis> | null): AutomationFailureDiagnosis | undefined {
  if (!source) {
    return undefined;
  }
  const category = String(source.category || '').trim();
  const reason = String(source.reason || '').trim();
  const evidenceSummary = String(source.evidenceSummary || '').trim();
  if (!category && !reason && !evidenceSummary) {
    return undefined;
  }
  return {
    category,
    reason,
    evidenceSummary
  };
}

function normalizeFailureSummary(
  source?: Partial<AutomationResultFailureSummary> | null
): AutomationResultFailureSummary | undefined {
  if (!source) {
    return undefined;
  }
  return {
    primaryCategory: String(source.primaryCategory || '').trim(),
    countsByCategory:
      source.countsByCategory && typeof source.countsByCategory === 'object'
        ? Object.entries(source.countsByCategory).reduce<Record<string, number>>((accumulator, [key, value]) => {
            accumulator[String(key)] = Number(value || 0);
            return accumulator;
          }, {})
        : {}
  };
}

function normalizeFailedModule(source: Partial<AutomationResultFailedModule>): AutomationResultFailedModule {
  return {
    moduleCode: String(source.moduleCode || ''),
    moduleName: String(source.moduleName || ''),
    failedScenarioCount: Number(source.failedScenarioCount || 0),
    diagnosis: normalizeDiagnosis(source.diagnosis)
  };
}

function normalizeFailedScenario(
  source: Partial<AutomationResultFailedScenario>
): AutomationResultFailedScenario {
  return {
    scenarioId: String(source.scenarioId || ''),
    scenarioTitle: source.scenarioTitle ? String(source.scenarioTitle) : undefined,
    moduleCode: source.moduleCode ? String(source.moduleCode) : undefined,
    moduleName: source.moduleName ? String(source.moduleName) : undefined,
    runnerType: source.runnerType,
    stepLabel: source.stepLabel ? String(source.stepLabel) : undefined,
    apiRef: source.apiRef ? String(source.apiRef) : undefined,
    pageAction: source.pageAction ? String(source.pageAction) : undefined,
    diagnosis: normalizeDiagnosis(source.diagnosis)
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
    details: source.details || {},
    diagnosis: normalizeDiagnosis(source.diagnosis)
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
    runId: payload.runId,
    summary: {
      total: Number(payload.summary?.total || results.length),
      passed: Number(payload.summary?.passed || 0),
      failed: Number(payload.summary?.failed || failedResults.length)
    },
    results,
    updatedAt: payload.updatedAt,
    registryVersion: payload.registryVersion,
    options: payload.options || {},
    relatedEvidenceFiles: Array.isArray(payload.relatedEvidenceFiles)
      ? payload.relatedEvidenceFiles.map((item) => String(item))
      : [],
    reportPath: payload.reportPath,
    exitCode: payload.exitCode,
    failureSummary: normalizeFailureSummary(payload.failureSummary),
    failedModules: Array.isArray(payload.failedModules)
      ? payload.failedModules.map((item) => normalizeFailedModule(item))
      : [],
    failedScenarios: Array.isArray(payload.failedScenarios)
      ? payload.failedScenarios.map((item) => normalizeFailedScenario(item))
      : [],
    failedScenarioIds: failedResults.map((item) => item.scenarioId),
    failedResults
  };
}

export function parseRegistryRunSummaryText(text: string): ParsedAcceptanceRegistryRunSummary {
  return parseRegistryRunSummary(JSON.parse(text) as AcceptanceRegistryRunSummary);
}
