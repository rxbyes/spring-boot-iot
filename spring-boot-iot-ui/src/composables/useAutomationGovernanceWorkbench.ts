import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  buildAutomationGovernanceQuery,
  normalizeAutomationGovernanceQuery,
  type AutomationGovernanceAssetTab,
  type AutomationGovernanceTab
} from '@/utils/automationGovernance';

const governanceTabs = [
  {
    key: 'assets' as const,
    label: '资产编排',
    description: '把页面盘点、场景模板、计划编排和交付打包并入同一资产工作区。'
  },
  {
    key: 'execution' as const,
    label: '执行配置',
    description: '统一维护目标环境、执行范围和阻断口径。'
  },
  {
    key: 'evidence' as const,
    label: '结果证据',
    description: '聚合运行台账、失败明细和证据文件预览。'
  }
];

const assetTabs = [
  {
    key: 'inventory' as const,
    label: '页面盘点',
    description: '先梳理交付对象、页面来源和覆盖边界。'
  },
  {
    key: 'templates' as const,
    label: '场景模板',
    description: '沉淀预置场景、断言模板和用例切片。'
  },
  {
    key: 'plans' as const,
    label: '计划编排',
    description: '组织执行批次、模块范围和依赖顺序。'
  },
  {
    key: 'handoff' as const,
    label: '交付打包',
    description: '维护交付包、回归说明和验收材料。'
  }
];

export function useAutomationGovernanceWorkbench() {
  const route = useRoute();
  const router = useRouter();

  const routeState = computed(() => normalizeAutomationGovernanceQuery(route.query));
  const activeTab = computed(() => routeState.value.tab);
  const activeAssetTab = computed(() => routeState.value.assetTab);
  const activeRunId = computed(() => routeState.value.runId);

  const currentTab = computed(
    () => governanceTabs.find((item) => item.key === activeTab.value) || governanceTabs[0]
  );
  const currentAssetTab = computed(
    () => assetTabs.find((item) => item.key === activeAssetTab.value) || assetTabs[0]
  );

  const governanceMetrics = computed(() => [
    {
      label: '治理主入口',
      value: '1',
      badge: { label: 'Hub', tone: 'brand' as const }
    },
    {
      label: '核心工作区',
      value: String(governanceTabs.length),
      badge: { label: 'Flow', tone: 'success' as const }
    },
    {
      label: '资产子区',
      value: String(assetTabs.length),
      badge: { label: 'Asset', tone: 'warning' as const }
    },
    {
      label: '证据深链',
      value: activeRunId.value ? '已定位' : '待选择',
      badge: {
        label: activeRunId.value ? 'Run' : 'Idle',
        tone: activeRunId.value ? ('danger' as const) : ('brand' as const)
      }
    }
  ]);

  const tabSummary = computed(() => {
    if (activeTab.value === 'assets') {
      return `当前聚焦 ${currentAssetTab.value.label}，适合先整理自动化资产边界。`;
    }
    if (activeTab.value === 'execution') {
      return '当前聚焦执行配置，可统一校准环境、范围和阻断口径。';
    }
    if (activeRunId.value) {
      return `当前聚焦结果证据，已预选 runId ${activeRunId.value}。`;
    }
    return '当前聚焦结果证据，可从历史台账中选择一次运行继续复盘。';
  });

  async function updateRoute(
    partial: Partial<{
      tab: AutomationGovernanceTab;
      assetTab: AutomationGovernanceAssetTab;
      runId: string;
    }>
  ) {
    await router.replace({
      query: buildAutomationGovernanceQuery(partial, route.query)
    });
  }

  async function selectTab(tab: AutomationGovernanceTab) {
    await updateRoute({ tab, runId: tab === 'evidence' ? activeRunId.value : '' });
  }

  async function selectAssetTab(tab: AutomationGovernanceAssetTab) {
    await updateRoute({ tab: 'assets', assetTab: tab, runId: '' });
  }

  async function openEvidenceRun(runId: string) {
    await updateRoute({ tab: 'evidence', runId });
  }

  async function clearEvidenceRun() {
    await updateRoute({ tab: 'evidence', runId: '' });
  }

  return {
    governanceTabs,
    assetTabs,
    activeTab,
    activeAssetTab,
    activeRunId,
    currentTab,
    currentAssetTab,
    governanceMetrics,
    tabSummary,
    selectTab,
    selectAssetTab,
    openEvidenceRun,
    clearEvidenceRun
  };
}
