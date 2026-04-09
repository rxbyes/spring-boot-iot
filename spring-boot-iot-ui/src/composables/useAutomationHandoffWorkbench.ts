import { computed } from 'vue';

import { useAutomationPlanBuilder } from './useAutomationPlanBuilder';

export function useAutomationHandoffWorkbench() {
  const planBuilder = useAutomationPlanBuilder();

  const executionAdvice = computed(() => [
    `建议优先执行范围：${planBuilder.plan.value.target.scenarioScopes.join(' / ') || '未设置'}`,
    `当前失败阻断范围：${planBuilder.plan.value.target.failScopes.join(' / ') || '未设置'}`,
    `执行命令沿用既有浏览器计划执行器，建议先导出计划再进入执行中心复核注册表。`
  ]);

  const deliveryNotes = computed(() => [
    `交付计划：${planBuilder.plan.value.target.planName || '未命名计划'}`,
    `输出前缀：${planBuilder.plan.value.target.outputPrefix || '未设置输出前缀'}`,
    `问题文档：${planBuilder.plan.value.target.issueDocPath || '未设置问题文档路径'}`
  ]);

  const handoffSummary = computed(() => ({
    planName: planBuilder.plan.value.target.planName,
    scenarioCount: planBuilder.scenarioPreviews.value.length,
    scopeBreakdown: planBuilder.plan.value.target.scenarioScopes,
    failScopes: planBuilder.plan.value.target.failScopes,
    warningSuggestions: planBuilder.suggestions.value
      .filter((item) => item.level === 'warning')
      .map((item) => item.title),
    outputPrefix: planBuilder.plan.value.target.outputPrefix,
    baselineDir: planBuilder.plan.value.target.baselineDir
  }));

  const focusScenarios = computed(() => planBuilder.scenarioPreviews.value.slice(0, 6));

  return {
    plan: planBuilder.plan,
    planMetrics: planBuilder.planMetrics,
    scenarioPreviews: planBuilder.scenarioPreviews,
    suggestions: planBuilder.suggestions,
    commandPreview: planBuilder.commandPreview,
    executionAdvice,
    deliveryNotes,
    handoffSummary,
    focusScenarios,
    copyCommand: planBuilder.copyCommand,
    downloadPlan: planBuilder.downloadPlan
  };
}
