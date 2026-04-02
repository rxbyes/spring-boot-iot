import { computed } from 'vue';
import { useAutomationPlanBuilder } from './useAutomationPlanBuilder';
import { useAutomationRegistryWorkbench } from './useAutomationRegistryWorkbench';

export function useAutomationExecutionWorkbench() {
  const {
    plan,
    scopeOptions,
    scenarioPreviews,
    commandPreview,
    copyCommand
  } = useAutomationPlanBuilder();
  const {
    registryScenarios,
    registrySummary
  } = useAutomationRegistryWorkbench();

  const executionMetrics = computed(() => [
    {
      label: '计划场景',
      value: String(scenarioPreviews.value.length),
      badge: { label: 'Scenario', tone: 'brand' as const }
    },
    {
      label: '执行范围',
      value: String(plan.value.target.scenarioScopes.length),
      badge: { label: 'Scope', tone: 'success' as const }
    },
    {
      label: '注册场景',
      value: String(registrySummary.value.total),
      badge: { label: 'Registry', tone: 'warning' as const }
    },
    {
      label: '阻断项',
      value: String(registrySummary.value.blockerCount),
      badge: { label: 'Blocker', tone: 'danger' as const }
    }
  ]);

  const registryBlockers = computed(() =>
    registryScenarios.value
      .filter((scenario) => scenario.blocking === 'blocker')
      .slice(0, 6)
      .map((scenario) => ({
        id: scenario.id,
        title: scenario.title,
        scope: scenario.scope,
        runnerType: scenario.runnerType,
        docRef: scenario.docRef || '未映射文档章节'
      }))
  );

  const scopeBreakdown = computed(() => {
    const knownScopes = scopeOptions.map((scope) => ({
      scope,
      scenarioCount: scenarioPreviews.value.filter((item) => item.scope === scope).length,
      enabled: plan.value.target.scenarioScopes.includes(scope),
      blocking: plan.value.target.failScopes.includes(scope)
    }));

    const extraScopeSet = new Set(
      [...plan.value.target.scenarioScopes, ...plan.value.target.failScopes].filter(
        (scope) => !scopeOptions.includes(scope)
      )
    );

    const extraScopes = Array.from(extraScopeSet).map((scope) => ({
      scope,
      scenarioCount: scenarioPreviews.value.filter((item) => item.scope === scope).length,
      enabled: plan.value.target.scenarioScopes.includes(scope),
      blocking: plan.value.target.failScopes.includes(scope)
    }));

    return [...knownScopes, ...extraScopes];
  });

  return {
    plan,
    scopeOptions,
    scenarioPreviews,
    registryScenarios,
    registrySummary,
    executionMetrics,
    commandPreview,
    registryBlockers,
    scopeBreakdown,
    copyCommand
  };
}
