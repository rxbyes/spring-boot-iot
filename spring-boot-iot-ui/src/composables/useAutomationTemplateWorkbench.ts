import { computed } from 'vue';

import { useAutomationPlanBuilder } from './useAutomationPlanBuilder';

const templatePrefixMap = {
  pageSmoke: 'page-smoke-',
  formSubmit: 'form-submit-',
  listDetail: 'list-detail-'
} as const;

export function useAutomationTemplateWorkbench() {
  const planBuilder = useAutomationPlanBuilder();

  const templateCards = [
    {
      type: 'pageSmoke' as const,
      title: '页面冒烟模板',
      description: '用于快速建立页面进入、基础断言与截图基线的最小脚手架。',
      scope: 'baseline',
      focus: ['页面可进入', '就绪选择器', '基础截图']
    },
    {
      type: 'formSubmit' as const,
      title: '表单提交模板',
      description: '用于表单填写、提交和接口回执校验，适合研发自测新增表单页。',
      scope: 'delivery',
      focus: ['表单录入', '提交动作', '接口回执']
    },
    {
      type: 'listDetail' as const,
      title: '列表详情模板',
      description: '用于列表查询、行级动作和详情断言，适合台账类页面回归起步。',
      scope: 'baseline',
      focus: ['列表筛选', '行级动作', '详情抽屉']
    }
  ];

  const templateMetrics = computed(() => [
    {
      label: '模板入口',
      value: String(templateCards.length),
      badge: { label: 'Tpl', tone: 'brand' as const }
    },
    {
      label: '当前计划场景',
      value: String(planBuilder.scenarioPreviews.value.length),
      badge: { label: 'Plan', tone: 'success' as const }
    },
    {
      label: '待补齐建议',
      value: String(planBuilder.suggestions.value.filter((item) => item.level === 'warning').length),
      badge: { label: 'Gap', tone: 'warning' as const }
    },
    {
      label: '盘点页面',
      value: String(planBuilder.pageInventory.value.length),
      badge: { label: 'Page', tone: 'danger' as const }
    }
  ]);

  const templateUsage = computed(() =>
    templateCards.map((card) => ({
      ...card,
      count: planBuilder.scenarioPreviews.value.filter((preview) =>
        preview.key.startsWith(templatePrefixMap[card.type]) || preview.name.includes(card.title)
      ).length
    }))
  );

  const latestScenarioPreviews = computed(() => planBuilder.scenarioPreviews.value.slice(0, 8));

  return {
    templateCards,
    templateMetrics,
    templateUsage,
    latestScenarioPreviews,
    addScenario: planBuilder.addScenario
  };
}
