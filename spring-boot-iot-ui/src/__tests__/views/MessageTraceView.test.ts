import { computed, defineComponent, inject, nextTick, provide, ref } from 'vue';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import MessageTraceView from '@/views/MessageTraceView.vue';
import { messageApi } from '@/api/message';

const { mockRoute, mockRouter } = vi.hoisted(() => ({
  mockRoute: {
    query: {} as Record<string, unknown>
  },
  mockRouter: {
    push: vi.fn(),
    replace: vi.fn()
  }
}));

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}));

vi.mock('@/api/message', () => ({
  messageApi: {
    pageMessageTraceLogs: vi.fn(),
    pageMessageTraceStats: vi.fn(),
    getMessageFlowTrace: vi.fn(),
    getMessageFlowOpsOverview: vi.fn(),
    getMessageFlowRecentSessions: vi.fn()
  }
}));

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>();
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn()
    }
  };
});

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0));

function createDeferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="message-trace-workbench-stub">
      <header>
        <p>{{ eyebrow }}</p>
        <h2>{{ title }}</h2>
        <p>{{ description }}</p>
        <slot name="header-actions" />
      </header>
      <div><slot name="filters" /></div>
      <div><slot name="applied-filters" /></div>
      <div><slot name="notices" /></div>
      <div><slot name="toolbar" /></div>
      <div><slot name="inline-state" /></div>
      <div><slot /></div>
      <div><slot name="pagination" /></div>
    </section>
  `
});

const IotAccessPageShellStub = defineComponent({
  name: 'IotAccessPageShell',
  props: ['breadcrumbs', 'title', 'showTitle'],
  template: `
    <section class="iot-access-page-shell-stub">
      <h1 v-if="showTitle !== false">{{ title }}</h1>
      <slot />
    </section>
  `
});

const IotAccessTabWorkspaceStub = defineComponent({
  name: 'IotAccessTabWorkspace',
  props: ['items'],
  template: `
    <section class="iot-access-tab-workspace-stub">
      <button
        v-for="item in items || []"
        :key="item.key"
        type="button"
      >
        {{ item.label }}
      </button>
      <slot :active-key="items?.[0]?.key" />
    </section>
  `
});

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section class="message-trace-filter-stub">
      <div><slot name="primary" /></div>
      <div><slot name="advanced" /></div>
      <div><slot name="actions" /></div>
    </section>
  `
});

const StandardAppliedFiltersBarStub = defineComponent({
  name: 'StandardAppliedFiltersBar',
  template: '<div class="message-trace-applied-filters-stub" />'
});

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  template: `
    <div class="message-trace-toolbar-stub">
      <slot />
      <slot name="right" />
    </div>
  `
});

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="panel-card-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h3 v-if="title">{{ title }}</h3>
      <p v-if="description">{{ description }}</p>
      <slot />
    </section>
  `
});

const MetricCardStub = defineComponent({
  name: 'MetricCard',
  props: ['label', 'value'],
  template: '<div class="metric-card-stub">{{ label }} {{ value }}</div>'
});

const StandardPaginationStub = defineComponent({
  name: 'StandardPagination',
  template: '<div class="message-trace-pagination-stub" />'
});

const StandardChoiceGroupStub = defineComponent({
  name: 'StandardChoiceGroup',
  props: ['options', 'modelValue'],
  emits: ['update:modelValue'],
  template: `
    <div class="message-trace-choice-group-stub">
      <button
        v-for="option in options"
        :key="option.value"
        type="button"
        @click="$emit('update:modelValue', option.value)"
      >
        {{ option.label }}
      </button>
    </div>
  `
});

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  props: ['disabled'],
  emits: ['click'],
  template: `
    <button type="button" :disabled="Boolean(disabled)" @click="$emit('click')">
      <slot />
    </button>
  `
});

const StandardRowActionsStub = defineComponent({
  name: 'StandardRowActions',
  template: '<div class="message-trace-row-actions-stub"><slot /></div>'
});

const StandardActionLinkStub = defineComponent({
  name: 'StandardActionLink',
  props: ['disabled'],
  emits: ['click'],
  template: `
    <button type="button" :disabled="Boolean(disabled)" @click="$emit('click')">
      <slot />
    </button>
  `
});

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['modelValue', 'title', 'subtitle', 'empty'],
  emits: ['update:modelValue'],
  template: `
    <section v-if="modelValue" class="message-trace-detail-drawer-stub">
      <h3>{{ title }}</h3>
      <p>{{ subtitle }}</p>
      <slot />
    </section>
  `
});

const StandardTraceTimelineStub = defineComponent({
  name: 'StandardTraceTimeline',
  props: ['timeline', 'loading', 'emptyTitle', 'emptyDescription'],
  template: `
    <section class="message-trace-timeline-stub">
      <p v-if="loading">loading</p>
      <template v-else-if="timeline">
        <strong>{{ timeline.traceId }}</strong>
        <span v-for="step in timeline.steps" :key="step.stage">{{ step.stage }}</span>
      </template>
      <template v-else>
        <strong>{{ emptyTitle }}</strong>
        <p>{{ emptyDescription }}</p>
      </template>
    </section>
  `
});

const StandardTableTextColumnStub = defineComponent({
  name: 'StandardTableTextColumn',
  props: ['label'],
  template: '<div class="standard-table-text-column-stub">{{ label }}</div>'
});

const AccessErrorArchivePanelStub = defineComponent({
  name: 'AccessErrorArchivePanel',
  template: '<section class="access-error-archive-panel-stub" />'
});

const IotAccessWorkbenchHeroStub = defineComponent({
  name: 'IotAccessWorkbenchHero',
  props: ['title', 'judgement'],
  template: `
    <section class="iot-access-workbench-hero-stub">
      <h2>{{ title }}</h2>
      <p>{{ judgement }}</p>
    </section>
  `
});

const IotAccessSignalDeckStub = defineComponent({
  name: 'IotAccessSignalDeck',
  props: ['lead', 'metrics'],
  template: `
    <section class="iot-access-signal-deck-stub">
      <p>{{ lead?.eyebrow }}</p>
      <h3>{{ lead?.title }}</h3>
      <p>{{ lead?.description }}</p>
      <p>{{ lead?.action?.label }}</p>
      <p v-for="item in metrics" :key="item.label">{{ item.label }} {{ item.value }}</p>
    </section>
  `
});

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: ['data'],
  setup(props) {
    provide('tableRows', computed(() => props.data ?? []));
    return {};
  },
  template: '<section class="el-table-stub"><slot /></section>'
});

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  setup() {
    const rows = inject('tableRows', ref([]));
    return { rows };
  },
  template: `
    <div class="el-table-column-stub">
      <div v-for="(row, index) in rows" :key="index" class="el-table-column-stub__row">
        <slot :row="row" />
      </div>
    </div>
  `
});

function createPageResponse() {
  return {
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 10,
      records: [
        {
          id: 1,
          traceId: 'trace-001',
          deviceCode: 'demo-device-01',
          productKey: 'demo-product',
          messageType: 'report',
          topic: '/sys/demo-product/demo-device-01/thing/property/post',
          payload: '{"temperature":26.5}',
          reportTime: '2026-03-23 10:00:00',
          createTime: '2026-03-23 10:00:00'
        }
      ]
    }
  };
}

function createStatsResponse() {
  return {
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      recentHourCount: 1,
      recent24HourCount: 1,
      distinctTraceCount: 1,
      distinctDeviceCount: 1,
      dispatchFailureCount: 0,
      topMessageTypes: [{ value: 'report', label: 'report', count: 1 }],
      topProductKeys: [{ value: 'demo-product', label: 'demo-product', count: 1 }],
      topDeviceCodes: [{ value: 'demo-device-01', label: 'demo-device-01', count: 1 }],
      topTopics: [{ value: '/sys/demo-product/demo-device-01/thing/property/post', label: '/sys/demo-product/demo-device-01/thing/property/post', count: 1 }]
    }
  };
}

function createOpsOverviewResponse() {
  return {
    code: 200,
    msg: 'success',
    data: {
      runtimeStartedAt: '2026-03-23 09:30:00',
      sessionCounts: [{ transportMode: 'MQTT', status: 'COMPLETED', count: 3 }],
      correlationCounts: [
        { result: 'published', count: 4 },
        { result: 'matched', count: 3 }
      ],
      lookupCounts: [{ target: 'trace', result: 'hit', count: 5 }],
      stageMetrics: [
        {
          stage: 'INGRESS',
          count: 3,
          failureCount: 0,
          skippedCount: 0,
          avgCostMs: 10,
          p95CostMs: 15,
          maxCostMs: 18
        }
      ]
    }
  };
}

function createRecentSessionsResponse() {
  return {
    code: 200,
    msg: 'success',
    data: [
      {
        sessionId: 'session-recent-001',
        traceId: 'trace-recent-001',
        transportMode: 'MQTT',
        status: 'COMPLETED',
        submittedAt: '2026-03-23 10:05:00',
        deviceCode: 'demo-device-01',
        topic: '$dp',
        correlationPending: false,
        timelineAvailable: true
      }
    ]
  };
}

function mountView() {
  return mount(MessageTraceView, {
    global: {
      directives: {
        loading: () => undefined
      },
      stubs: {
        AccessErrorArchivePanel: AccessErrorArchivePanelStub,
        IotAccessPageShell: IotAccessPageShellStub,
        IotAccessTabWorkspace: IotAccessTabWorkspaceStub,
        IotAccessWorkbenchHero: IotAccessWorkbenchHeroStub,
        IotAccessSignalDeck: IotAccessSignalDeckStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        PanelCard: PanelCardStub,
        MetricCard: MetricCardStub,
        StandardListFilterHeader: StandardListFilterHeaderStub,
        StandardAppliedFiltersBar: StandardAppliedFiltersBarStub,
        StandardTableToolbar: StandardTableToolbarStub,
        StandardPagination: StandardPaginationStub,
        StandardChoiceGroup: StandardChoiceGroupStub,
        StandardButton: StandardButtonStub,
        StandardRowActions: StandardRowActionsStub,
        StandardActionLink: StandardActionLinkStub,
        StandardDetailDrawer: StandardDetailDrawerStub,
        StandardTraceTimeline: StandardTraceTimelineStub,
        StandardTableTextColumn: StandardTableTextColumnStub,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub
      }
    }
  });
}

function findButtonByText(wrapper: ReturnType<typeof mountView>, text: string) {
  return wrapper.findAll('button').find((button) => button.text().includes(text));
}

describe('MessageTraceView', () => {
  beforeEach(() => {
    mockRoute.query = {};
    window.sessionStorage.clear();
    mockRouter.push.mockReset();
    mockRouter.replace.mockReset();
    vi.mocked(messageApi.pageMessageTraceLogs).mockReset();
    vi.mocked(messageApi.pageMessageTraceStats).mockReset();
    vi.mocked(messageApi.getMessageFlowTrace).mockReset();
    vi.mocked(messageApi.getMessageFlowOpsOverview).mockReset();
    vi.mocked(messageApi.getMessageFlowRecentSessions).mockReset();
    vi.mocked(messageApi.pageMessageTraceLogs).mockResolvedValue(createPageResponse());
    vi.mocked(messageApi.pageMessageTraceStats).mockResolvedValue(createStatsResponse());
    vi.mocked(messageApi.getMessageFlowOpsOverview).mockResolvedValue(createOpsOverviewResponse());
    vi.mocked(messageApi.getMessageFlowRecentSessions).mockResolvedValue(createRecentSessionsResponse());
  });

  it('restores diagnostic source from sessionStorage when route query is partial', async () => {
    const now = new Date().toISOString();
    window.sessionStorage.setItem('iot-access:diagnostic-context', JSON.stringify({
      storedAt: Date.now(),
      context: {
        sourcePage: 'reporting',
        deviceCode: 'stored-device-01',
        productKey: 'stored-product',
        topic: '/sys/stored-product/stored-device-01/thing/property/post',
        capturedAt: now
      }
    }));
    mockRoute.query = {
      traceId: 'trace-route-001'
    };

    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).toContain('来自链路验证中心');
    expect(messageApi.pageMessageTraceLogs).toHaveBeenCalledWith(expect.objectContaining({
      traceId: 'trace-route-001',
      deviceCode: 'stored-device-01',
      productKey: 'stored-product',
      topic: '/sys/stored-product/stored-device-01/thing/property/post'
    }));
  });

  it('loads and renders the trace timeline in the detail drawer', async () => {
    vi.mocked(messageApi.getMessageFlowTrace).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        traceId: 'trace-001',
        sessionId: 'session-001',
        flowType: 'MQTT',
        status: 'COMPLETED',
        deviceCode: 'demo-device-01',
        productKey: 'demo-product',
        topic: '/sys/demo-product/demo-device-01/thing/property/post',
        protocolCode: 'mqtt-json',
        messageType: 'property',
        startedAt: '2026-03-23 10:00:00',
        finishedAt: '2026-03-23 10:00:01',
        totalCostMs: 90,
        steps: [
          {
            stage: 'INGRESS',
            handlerClass: 'UpMessageProcessingPipeline',
            handlerMethod: 'ingress',
            status: 'SUCCESS',
            costMs: 1,
            startedAt: '2026-03-23 10:00:00',
            finishedAt: '2026-03-23 10:00:00',
            summary: {},
            errorClass: '',
            errorMessage: '',
            branch: ''
          }
        ]
      }
    });

    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    await findButtonByText(wrapper, '详情')!.trigger('click');
    await flushPromises();
    await nextTick();

    expect(messageApi.getMessageFlowTrace).toHaveBeenCalledWith('trace-001');
    expect(wrapper.text()).toContain('trace-001');
    expect(wrapper.text()).toContain('INGRESS');
    expect(wrapper.text()).not.toContain('时间线已过期，仅保留消息日志。');
  });

  it('shows the degraded hint when the trace timeline has expired', async () => {
    vi.mocked(messageApi.getMessageFlowTrace).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: null
    });

    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    await findButtonByText(wrapper, '详情')!.trigger('click');
    await flushPromises();
    await nextTick();

    expect(messageApi.getMessageFlowTrace).toHaveBeenCalledWith('trace-001');
    expect(wrapper.text()).toContain('时间线已过期，仅保留消息日志。');
    expect(wrapper.text()).toContain('Redis 中的短期时间线已过期，但消息日志、Payload 和基础链路信息仍可继续排查。');
  });

  it('renders the trace page with only real trace tabs and without support zones', async () => {
    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    expect(wrapper.find('.iot-access-page-shell-stub').exists()).toBe(true);
    expect(wrapper.find('.iot-access-tab-workspace-stub').exists()).toBe(true);
    expect(messageApi.getMessageFlowOpsOverview).not.toHaveBeenCalled();
    expect(messageApi.getMessageFlowRecentSessions).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('链路追踪台');
    expect(wrapper.text()).toContain('失败归档');
    expect(wrapper.text()).toContain('TraceId');
    expect(wrapper.text()).toContain('按 TraceId、设备编码、产品标识与 Topic 串联同一条接入链路。');
    expect(wrapper.text()).not.toContain('TRACE CENTER');
    expect(wrapper.text()).not.toContain('运维看板');
    expect(wrapper.text()).not.toContain('最近会话');
    expect(wrapper.text()).not.toContain('异常观测台');
    expect(wrapper.text()).not.toContain('数据校验台');
  });

  it('shows storage error copy when timeline lookup fails', async () => {
    vi.mocked(messageApi.getMessageFlowTrace).mockRejectedValue(new Error('redis down'));

    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    await findButtonByText(wrapper, '详情')!.trigger('click');
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).toContain('message-flow 存储异常/Redis 不可用');
    expect(wrapper.text()).toContain('当前 trace 查询返回异常，优先排查 Redis 可用性与 message-flow 存储日志。');
  });

  it('treats resolved non-200 timeline responses as storage errors instead of expiration', async () => {
    vi.mocked(messageApi.getMessageFlowTrace).mockResolvedValue({
      code: 500,
      msg: 'redis down',
      data: null
    });

    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    await findButtonByText(wrapper, '详情')!.trigger('click');
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).toContain('message-flow 存储异常/Redis 不可用');
    expect(wrapper.text()).toContain('时间线查询异常，优先排查 Redis / message-flow 存储');
    expect(wrapper.text()).not.toContain('时间线已过期');
  });

  it('ignores stale timeline responses when switching between detail rows quickly', async () => {
    vi.mocked(messageApi.pageMessageTraceLogs).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 2,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1,
            traceId: 'trace-001',
            deviceCode: 'demo-device-01',
            productKey: 'demo-product',
            messageType: 'report',
            topic: '/sys/demo-product/demo-device-01/thing/property/post',
            payload: '{"temperature":26.5}',
            reportTime: '2026-03-23 10:00:00',
            createTime: '2026-03-23 10:00:00'
          },
          {
            id: 2,
            traceId: 'trace-002',
            deviceCode: 'demo-device-02',
            productKey: 'demo-product-02',
            messageType: 'reply',
            topic: '/sys/demo-product-02/demo-device-02/thing/property/post',
            payload: '{"result":"ok"}',
            reportTime: '2026-03-23 10:00:02',
            createTime: '2026-03-23 10:00:02'
          }
        ]
      }
    });
    const firstTimeline = createDeferred<{
      code: number;
      msg: string;
      data: null;
    }>();
    const secondTimeline = createDeferred<{
      code: number;
      msg: string;
      data: {
        traceId: string;
        sessionId: string;
        flowType: string;
        status: string;
        deviceCode: string;
        productKey: string;
        topic: string;
        protocolCode: string;
        messageType: string;
        startedAt: string;
        finishedAt: string;
        totalCostMs: number;
        steps: Array<{
          stage: string;
          handlerClass: string;
          handlerMethod: string;
          status: string;
          costMs: number;
          startedAt: string;
          finishedAt: string;
          summary: Record<string, unknown>;
          errorClass: string;
          errorMessage: string;
          branch: string;
        }>;
      };
    }>();
    vi.mocked(messageApi.getMessageFlowTrace).mockImplementation((traceId: string) => {
      if (traceId === 'trace-001') {
        return firstTimeline.promise;
      }
      if (traceId === 'trace-002') {
        return secondTimeline.promise;
      }
      return Promise.reject(new Error(`unexpected traceId: ${traceId}`));
    });

    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    const detailButtons = wrapper.findAll('button').filter((button) => button.text().includes('详情'));
    await detailButtons[0]!.trigger('click');
    await nextTick();
    await detailButtons[1]!.trigger('click');
    await nextTick();

    secondTimeline.resolve({
      code: 200,
      msg: 'success',
      data: {
        traceId: 'trace-002',
        sessionId: 'session-002',
        flowType: 'MQTT',
        status: 'COMPLETED',
        deviceCode: 'demo-device-02',
        productKey: 'demo-product-02',
        topic: '/sys/demo-product-02/demo-device-02/thing/property/post',
        protocolCode: 'mqtt-json',
        messageType: 'reply',
        startedAt: '2026-03-23 10:00:02',
        finishedAt: '2026-03-23 10:00:03',
        totalCostMs: 66,
        steps: [
          {
            stage: 'DEVICE_CONTRACT',
            handlerClass: 'UpMessageProcessingPipeline',
            handlerMethod: 'contract',
            status: 'SUCCESS',
            costMs: 8,
            startedAt: '2026-03-23 10:00:02',
            finishedAt: '2026-03-23 10:00:02',
            summary: {},
            errorClass: '',
            errorMessage: '',
            branch: ''
          }
        ]
      }
    });
    await flushPromises();
    await nextTick();

    firstTimeline.resolve({
      code: 500,
      msg: 'redis down',
      data: null
    });
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).toContain('trace-002');
    expect(wrapper.text()).toContain('DEVICE_CONTRACT');
    expect(wrapper.text()).not.toContain('message-flow 存储异常/Redis 不可用');
    expect(wrapper.text()).not.toContain('时间线查询异常，优先排查 Redis / message-flow 存储');
  });

  it('does not render recent-session restore affordances in the simplified trace layout', async () => {
    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).not.toContain('session-recent-001');
    expect(messageApi.getMessageFlowRecentSessions).not.toHaveBeenCalled();
  });

  it('shows a storage-specific rule summary after timeline lookup errors', async () => {
    vi.mocked(messageApi.getMessageFlowTrace).mockRejectedValue(new Error('redis down'));

    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    await findButtonByText(wrapper, '详情')!.trigger('click');
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).toContain('时间线查询异常，优先排查 Redis / message-flow 存储');
    expect(wrapper.text()).not.toContain('时间线已过期');
  });

  it('carries row context when jumping to anomaly observability from the action column', async () => {
    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    await findButtonByText(wrapper, '观测')!.trigger('click');
    await flushPromises();

    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/system-log',
      query: {
        traceId: 'trace-001',
        deviceCode: 'demo-device-01',
        productKey: 'demo-product',
        requestUrl: '/sys/demo-product/demo-device-01/thing/property/post',
        requestMethod: 'MQTT'
      }
    });
    const persistedRaw = window.sessionStorage.getItem('iot-access:diagnostic-context');
    expect(persistedRaw).toBeTruthy();
    const persisted = JSON.parse(persistedRaw as string);
    expect(persisted.context.sourcePage).toBe('message-trace');
  });

});
