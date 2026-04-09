import fs from 'node:fs/promises';
import path from 'node:path';

export const DEFAULT_ACCEPTANCE_REGISTRY_PATH = 'config/automation/acceptance-registry.json';
export const VALID_RUNNER_TYPES = new Set([
  'browserPlan',
  'apiSmoke',
  'messageFlow',
  'riskDrill'
]);

function normalizeScenario(source = {}) {
  return {
    id: String(source.id || '').trim(),
    title: String(source.title || '').trim(),
    module: String(source.module || '').trim(),
    docRef: String(source.docRef || '').trim(),
    runnerType: String(source.runnerType || '').trim(),
    scope: String(source.scope || '').trim(),
    blocking: String(source.blocking || '').trim(),
    dependsOn: Array.isArray(source.dependsOn)
      ? source.dependsOn.map((item) => String(item || '').trim()).filter(Boolean)
      : [],
    inputs: source.inputs && typeof source.inputs === 'object' ? { ...source.inputs } : {},
    evidence: Array.isArray(source.evidence)
      ? source.evidence.map((item) => String(item || '').trim()).filter(Boolean)
      : [],
    timeouts: source.timeouts && typeof source.timeouts === 'object' ? { ...source.timeouts } : {},
    runner: source.runner && typeof source.runner === 'object' ? { ...source.runner } : {}
  };
}

export async function loadAcceptanceRegistry({
  workspaceRoot = process.cwd(),
  registryPath = DEFAULT_ACCEPTANCE_REGISTRY_PATH,
  source
} = {}) {
  const raw =
    source ??
    JSON.parse(
      await fs.readFile(path.resolve(workspaceRoot, registryPath), 'utf8')
    );
  const scenarios = Array.isArray(raw?.scenarios)
    ? raw.scenarios.map(normalizeScenario)
    : [];
  const seenIds = new Set();

  scenarios.forEach((scenario) => {
    if (!scenario.id) {
      throw new Error('Registry scenario id is required.');
    }
    if (seenIds.has(scenario.id)) {
      throw new Error(`Duplicate registry scenario id: ${scenario.id}`);
    }
    if (!VALID_RUNNER_TYPES.has(scenario.runnerType)) {
      throw new Error(`Unsupported runnerType: ${scenario.runnerType}`);
    }
    seenIds.add(scenario.id);
  });

  return {
    version: String(raw?.version || '').trim() || '1.0.0',
    generatedAt: String(raw?.generatedAt || '').trim(),
    defaultTarget:
      raw?.defaultTarget && typeof raw.defaultTarget === 'object'
        ? { ...raw.defaultTarget }
        : {},
    scenarios
  };
}

export function orderRegistryScenarios(scenarios) {
  const normalized = scenarios.map((item) => ({
    ...item,
    dependsOn: Array.isArray(item?.dependsOn)
      ? item.dependsOn.map((depId) => String(depId || '').trim()).filter(Boolean)
      : []
  }));
  const scenarioMap = new Map(normalized.map((item) => [item.id, item]));
  const visiting = new Set();
  const visited = new Set();
  const ordered = [];

  const visit = (scenario) => {
    if (visited.has(scenario.id)) {
      return;
    }
    if (visiting.has(scenario.id)) {
      throw new Error(`Dependency cycle detected at ${scenario.id}`);
    }

    visiting.add(scenario.id);
    scenario.dependsOn.forEach((depId) => {
      const dependency = scenarioMap.get(depId);
      if (dependency) {
        visit(dependency);
      }
    });
    visiting.delete(scenario.id);
    visited.add(scenario.id);
    ordered.push(scenario);
  };

  normalized.forEach(visit);
  return ordered;
}

export function filterRegistryScenarios(registry, options = {}) {
  const selected = registry.scenarios.filter((scenario) => {
    if (options.id && scenario.id !== options.id) {
      return false;
    }
    if (options.module && scenario.module !== options.module) {
      return false;
    }
    if (options.scope && scenario.scope !== options.scope) {
      return false;
    }
    return true;
  });

  if (!options.includeDeps) {
    return orderRegistryScenarios(selected);
  }

  const scenarioMap = new Map(registry.scenarios.map((item) => [item.id, item]));
  const required = new Map();

  const visit = (scenario) => {
    required.set(scenario.id, scenario);
    (scenario.dependsOn || []).forEach((depId) => {
      const dependency = scenarioMap.get(depId);
      if (!dependency) {
        throw new Error(`Missing dependency: ${depId}`);
      }
      if (!required.has(depId)) {
        visit(dependency);
      }
    });
  };

  selected.forEach(visit);
  return orderRegistryScenarios([...required.values()]);
}
