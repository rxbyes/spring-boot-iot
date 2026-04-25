import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { computed, defineComponent, inject, nextTick, provide, ref } from 'vue';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import AuditLogView from '@/views/AuditLogView.vue';
import {
  deleteAuditLog,
  getAuditLogById,
  getBusinessAuditStats,
  getSystemErrorStats,
  pageLogs
} from '@/api/auditLog';
import { getTraceEvidence, listObservabilitySlowSpanSummaries } from '@/api/observability';
import { splitWorkbenchRowActions } from '@/utils/adaptiveActionColumn';

const { mockRoute, mockRouter } = vi.hoisted(() => ({
  mockRoute: {
    path: '/system-log',
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

vi.mock('@/api/auditLog', () => ({
  pageLogs: vi.fn(),
  getAuditLogById: vi.fn(),
  deleteAuditLog: vi.fn(),
  getSystemErrorStats: vi.fn(),
  getBusinessAuditStats: vi.fn()
}));

vi.mock('@/api/observability', () => ({
  listObservabilitySlowSpanSummaries: vi.fn(),
  getTraceEvidence: vi.fn()
}));

vi.mock('@/utils/confirm', () => ({
  confirmAction: vi.fn(),
  isConfirmCancelled: vi.fn(() => false)
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

function installSessionStorageMock(value?: Record<string, string>) {
  const store = new Map<string, string>(Object.entries(value || {}));
  Object.defineProperty(window, 'sessionStorage', {
    configurable: true,
    value: {
      getItem: vi.fn((key: string) => store.get(key) ?? null),
      setItem: vi.fn((key: string, next: string) => {
        store.set(key, next);
      }),
      removeItem: vi.fn((key: string) => {
        store.delete(key);
      })
    }
  });
}

function findButtonByText(wrapper: ReturnType<typeof mountView>, text: string) {
  return wrapper.findAll('button').find((button) => button.text().includes(text));
}

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="audit-log-workbench-stub">
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

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  props: ['breadcrumbs', 'title', 'showTitle'],
  template: `
    <section class="standard-page-shell-stub">
      <h1 v-if="showTitle !== false">{{ title }}</h1>
      <slot />
    </section>
  `
});

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section class="audit-log-filter-stub">
      <div><slot name="primary" /></div>
      <div><slot name="advanced" /></div>
      <div><slot name="actions" /></div>
    </section>
  `
});

const StandardAppliedFiltersBarStub = defineComponent({
  name: 'StandardAppliedFiltersBar',
  template: '<div class="audit-log-applied-filters-stub" />'
});

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  template: `
    <div class="audit-log-toolbar-stub">
      <slot />
      <slot name="right" />
    </div>
  `
});

const StandardChoiceGroupStub = defineComponent({
  name: 'StandardChoiceGroup',
  props: ['options', 'modelValue'],
  emits: ['update:modelValue'],
  template: `
    <div class="audit-log-choice-group-stub">
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

const StandardActionMenuStub = defineComponent({
  name: 'StandardActionMenu',
  props: ['label', 'items', 'disabled'],
  emits: ['command'],
  template: `
    <div
      class="audit-log-action-menu-stub"
      :data-label="label"
      :data-disabled="Boolean(disabled)"
      :data-items="JSON.stringify(items || [])"
    >
      <button type="button">{{ label }}</button>
    </div>
  `
});

const StandardWorkbenchRowActionsStub = defineComponent({
  name: 'StandardWorkbenchRowActions',
  props: ['variant', 'gap', 'directItems', 'menuItems', 'menuLabel'],
  emits: ['command'],
  setup(props) {
    const resolvedActions = computed(() =>
      splitWorkbenchRowActions({
        directItems: props.directItems || [],
        menuItems: props.menuItems || []
      })
    );
    const resolvedDirectItems = computed(() => resolvedActions.value.directItems);
    const resolvedMenuItems = computed(() => resolvedActions.value.menuItems);
    const resolvedMenuLabel = computed(() => props.menuLabel || '更多');
    return {
      resolvedDirectItems,
      resolvedMenuItems,
      resolvedMenuLabel
    };
  },
  template: `
    <div class="audit-log-row-actions-stub" :data-variant="variant" :data-menu-label="resolvedMenuLabel">
      <button
        v-for="item in resolvedDirectItems"
        :key="item.key || item.command"
        type="button"
        :disabled="Boolean(item.disabled)"
        @click="$emit('command', item.command)"
      >
        {{ item.label }}
      </button>
      <button v-if="resolvedMenuItems.length > 0" type="button">{{ resolvedMenuLabel }}</button>
      <button
        v-for="item in resolvedMenuItems"
        :key="item.key || item.command"
        type="button"
        :disabled="Boolean(item.disabled)"
        @click="$emit('command', item.command)"
      >
        {{ item.label }}
      </button>
      <span class="audit-log-row-actions-stub__menu-count">{{ resolvedMenuItems.length }}</span>
    </div>
  `
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

const StandardTableTextColumnStub = defineComponent({
  name: 'StandardTableTextColumn',
  props: ['label'],
  template: '<div class="audit-log-text-column-stub">{{ label }}</div>'
});

const StandardPaginationStub = defineComponent({
  name: 'StandardPagination',
  template: '<div class="audit-log-pagination-stub" />'
});

const AuditLogDetailDrawerStub = defineComponent({
  name: 'AuditLogDetailDrawer',
  props: ['title'],
  template: '<section class="audit-log-detail-stub">{{ title }}</section>'
});

const CsvColumnSettingDialogStub = defineComponent({
  name: 'CsvColumnSettingDialog',
  props: ['title'],
  template: '<section class="audit-log-csv-dialog-stub">{{ title }}</section>'
});

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['title', 'modelValue', 'loading', 'errorMessage', 'empty', 'emptyText'],
  template: `
    <section v-if="modelValue" class="observability-evidence-drawer-stub">
      <h2>{{ title }}</h2>
      <p v-if="loading">loading</p>
      <p v-else-if="errorMessage">{{ errorMessage }}</p>
      <p v-else-if="empty">{{ emptyText }}</p>
      <slot v-else />
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
  template: '<section class="audit-log-table-stub"><slot /></section>'
});

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['label', 'className', 'width'],
  setup() {
    const rows = inject('tableRows', ref([]));
    return { rows };
  },
  template: `
    <div class="audit-log-column-stub" :data-label="label" :data-class-name="className" :data-width="width">
      <div v-for="(row, index) in rows" :key="index">
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
          operationModule: 'mqtt-consumer',
          operationMethod: 'consume',
          requestUrl: '$dp',
          requestMethod: 'MQTT',
          traceId: 'trace-001',
          deviceCode: 'demo-device-01',
          productKey: 'demo-product',
          errorCode: 'MQTT_TIMEOUT',
          exceptionClass: 'java.lang.IllegalStateException',
          resultMessage: 'timeout',
          operationTime: '2026-03-28 10:00:00',
          operationResult: 0
        }
      ]
    }
  };
}

function mountView() {
  return mount(AuditLogView, {
    global: {
      directives: {
        loading: () => undefined
      },
      stubs: {
        StandardPageShell: StandardPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: StandardListFilterHeaderStub,
        StandardAppliedFiltersBar: StandardAppliedFiltersBarStub,
        StandardTableToolbar: StandardTableToolbarStub,
        StandardChoiceGroup: StandardChoiceGroupStub,
        StandardButton: StandardButtonStub,
        StandardActionMenu: StandardActionMenuStub,
        StandardWorkbenchRowActions: StandardWorkbenchRowActionsStub,
        StandardActionLink: StandardActionLinkStub,
        StandardTableTextColumn: StandardTableTextColumnStub,
        StandardPagination: StandardPaginationStub,
        AuditLogDetailDrawer: AuditLogDetailDrawerStub,
        CsvColumnSettingDialog: CsvColumnSettingDialogStub,
        StandardDetailDrawer: StandardDetailDrawerStub,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub,
        ElInput: true,
        ElSelect: true,
        ElOption: true,
        ElTag: true,
        ElAlert: true
      }
    }
  });
}

describe('AuditLogView', () => {
  beforeEach(() => {
    installSessionStorageMock();
    mockRoute.path = '/system-log';
    mockRoute.query = {};
    mockRouter.push.mockReset();
    mockRouter.replace.mockReset();
    vi.mocked(pageLogs).mockReset();
    vi.mocked(getAuditLogById).mockReset();
    vi.mocked(deleteAuditLog).mockReset();
    vi.mocked(getSystemErrorStats).mockReset();
    vi.mocked(getBusinessAuditStats).mockReset();
    vi.mocked(listObservabilitySlowSpanSummaries).mockReset();
    vi.mocked(getTraceEvidence).mockReset();
    vi.mocked(pageLogs).mockResolvedValue(createPageResponse());
    vi.mocked(getSystemErrorStats).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 4,
        todayCount: 1,
        mqttCount: 2,
        systemCount: 2,
        distinctTraceCount: 2,
        distinctDeviceCount: 1,
        topModules: [{ label: 'mqtt-consumer', count: 2 }],
        topExceptionClasses: [],
        topErrorCodes: []
      }
    });
    vi.mocked(getBusinessAuditStats).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 8,
        todayCount: 2,
        successCount: 6,
        failureCount: 2,
        distinctUserCount: 3,
        topModules: [{ label: 'device', count: 4 }],
        topUsers: [{ label: 'admin', count: 3 }],
        topOperationTypes: []
      }
    });
    vi.mocked(listObservabilitySlowSpanSummaries).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          spanType: 'SLOW_SQL',
          domainCode: 'system',
          eventCode: 'system.error.archive',
          objectType: 'sql',
          objectId: 'iot_message_log',
          totalCount: 3,
          avgDurationMs: 1280,
          maxDurationMs: 2400,
          latestTraceId: 'trace-slow-1',
          latestStartedAt: '2026-04-25 10:08:00'
        }
      ]
    });
    vi.mocked(getTraceEvidence).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        traceId: 'trace-001',
        businessEvents: [
          {
            id: 10,
            traceId: 'trace-001',
            eventCode: 'product.contract.apply',
            eventName: '合同生效',
            domainCode: 'product',
            actionCode: 'apply',
            objectType: 'product_contract',
            objectId: '20430001',
            resultStatus: 'SUCCESS',
            occurredAt: '2026-04-25 10:05:00'
          }
        ],
        spans: [
          {
            id: 20,
            traceId: 'trace-001',
            spanType: 'SLOW_SQL',
            spanName: 'Slow SQL',
            status: 'SUCCESS',
            durationMs: 1200,
            startedAt: '2026-04-25 10:04:00'
          }
        ],
        timeline: [
          {
            itemType: 'SPAN',
            itemId: 20,
            traceId: 'trace-001',
            code: 'SLOW_SQL',
            name: 'Slow SQL',
            status: 'SUCCESS',
            durationMs: 1200,
            occurredAt: '2026-04-25 10:04:00'
          },
          {
            itemType: 'BUSINESS_EVENT',
            itemId: 10,
            traceId: 'trace-001',
            code: 'product.contract.apply',
            name: '合同生效',
            status: 'SUCCESS',
            occurredAt: '2026-04-25 10:05:00'
          }
        ]
      }
    });
  });

  it('renders the anomaly page list-first without toolbar jump shortcuts or legacy eyebrow tiers', async () => {
    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    expect(wrapper.find('.standard-page-shell-stub').exists()).toBe(true);
    expect(wrapper.text()).toContain('异常观测台');
    expect(wrapper.text()).toContain('后台异常核对');
    expect(wrapper.text()).toContain('追踪');
    expect(wrapper.text()).toContain('删除');
    expect(wrapper.text()).not.toContain('链路追踪台');
    expect(wrapper.text()).not.toContain('失败归档');
    expect(wrapper.text()).not.toContain('OBSERVABILITY DESK');

    const rowActions = wrapper.findAll('.audit-log-row-actions-stub');

    expect(rowActions.length).toBeGreaterThan(0);
    rowActions.forEach((item) => {
      expect(item.text()).toContain('详情');
      expect(item.text()).toContain('证据');
      expect(item.text()).toContain('追踪');
      expect(item.text()).toContain('删除');
      expect(item.text()).toContain('更多');
      expect(item.find('.audit-log-row-actions-stub__menu-count').text()).toBe('1');
      expect(item.attributes('data-menu-label')).toBe('更多');
    });
  });

  it('opens trace evidence drawer from system-log row actions', async () => {
    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    const rowActionEvidenceButton = wrapper
      .find('.audit-log-row-actions-stub')
      .findAll('button')
      .find((button) => button.text().includes('证据'));
    await rowActionEvidenceButton!.trigger('click');
    await flushPromises();
    await nextTick();

    expect(getTraceEvidence).toHaveBeenCalledWith('trace-001');
    const drawer = wrapper.find('.observability-evidence-drawer-stub');
    expect(drawer.exists()).toBe(true);
    expect(drawer.text()).toContain('TraceId 证据包');
    expect(drawer.text()).toContain('product.contract.apply');
    expect(drawer.text()).toContain('SLOW_SQL');
    expect(drawer.text()).toContain('1200 ms');
  });

  it('renders slow performance hotspots and opens evidence from the latest trace', async () => {
    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    expect(listObservabilitySlowSpanSummaries).toHaveBeenCalledWith({
      limit: 5,
      minDurationMs: 1
    });

    const panel = wrapper.find('.audit-log-slow-summary');
    expect(panel.exists()).toBe(true);
    expect(panel.text()).toContain('性能慢点 Top');
    expect(panel.text()).toContain('SLOW_SQL');
    expect(panel.text()).toContain('system.error.archive');
    expect(panel.text()).toContain('iot_message_log');
    expect(panel.text()).toContain('2400 ms');
    expect(panel.text()).toContain('3 次');

    vi.mocked(getTraceEvidence).mockClear();
    await panel.find('button').trigger('click');
    await flushPromises();
    await nextTick();

    expect(getTraceEvidence).toHaveBeenCalledWith('trace-slow-1');
    expect(wrapper.find('.observability-evidence-drawer-stub').exists()).toBe(true);
  });

  it('uses anomaly-oriented detail and export titles in system mode', async () => {
    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    expect(wrapper.findComponent(AuditLogDetailDrawerStub).props('title')).toBe('异常详情');
    expect(wrapper.findComponent(CsvColumnSettingDialogStub).props('title')).toBe('异常观测台导出列设置');
  });

  it('keeps refresh as the only direct toolbar action and moves export utilities into more actions', async () => {
    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    const toolbarText = wrapper.find('.audit-log-toolbar-stub').text();

    expect(toolbarText).toContain('刷新列表');
    expect(toolbarText).toContain('更多操作');
    expect(toolbarText).not.toContain('导出列设置');
    expect(toolbarText).not.toContain('导出选中');
    expect(toolbarText).not.toContain('导出当前结果');
    expect(toolbarText).not.toContain('清空选中');

    const actionMenu = wrapper.findComponent(StandardActionMenuStub);
    expect(actionMenu.exists()).toBe(true);
    expect(actionMenu.props('label')).toBe('更多操作');
    expect(actionMenu.props('items')).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ command: 'export-config', label: '导出列设置' }),
        expect.objectContaining({ command: 'export-selected', label: '导出选中' }),
        expect.objectContaining({ command: 'export-current', label: '导出当前结果' }),
        expect.objectContaining({ command: 'clear-selection', label: '清空选中' })
      ])
    );
  });

  it('marks the system action column with the shared row-action class to prevent clipped trailing dots', async () => {
    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    const actionColumn = wrapper
      .findAll('.audit-log-column-stub')
      .find((column) => column.attributes('data-label') === '操作');

    expect(actionColumn?.attributes('data-class-name')).toBe('standard-row-actions-column');
    expect(actionColumn?.attributes('data-width')).toBe('200');
  });

  it('keeps business mode list-first without the anomaly strip', async () => {
    mockRoute.path = '/audit-log';
    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).toContain('审计中心');
    expect(wrapper.text()).not.toContain('先看 system_error，再决定追踪链路还是回看失败归档。');
    expect(wrapper.find('.audit-log-command-strip').exists()).toBe(false);
  });

  it('restores system-log filters from persisted diagnostic context', async () => {
    const now = new Date().toISOString();
    installSessionStorageMock({
      'iot-access:diagnostic-context': JSON.stringify({
        storedAt: Date.now(),
        context: {
          sourcePage: 'message-trace',
          traceId: 'trace-001',
          deviceCode: 'demo-device-01',
          productKey: 'demo-product',
          topic: '$dp',
          capturedAt: now
        }
      })
    });
    mockRoute.path = '/system-log';
    mockRoute.query = { traceId: 'trace-001' };
    vi.mocked(pageLogs).mockResolvedValue(createPageResponse());
    vi.mocked(getSystemErrorStats).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        todayCount: 1,
        mqttCount: 1,
        systemCount: 0,
        distinctTraceCount: 1,
        distinctDeviceCount: 1,
        topModules: [],
        topExceptionClasses: [],
        topErrorCodes: []
      }
    });

    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).toContain('来自链路追踪台');
    expect(wrapper.text()).toContain('当前节点：后台异常核对');
    expect(wrapper.text()).toContain('下一步回链路追踪台或治理页继续排查。');
    expect(wrapper.text()).not.toContain('当前异常 1 条');
    expect(wrapper.text()).not.toContain('关联链路 1 条');
    expect(wrapper.text()).not.toContain('可回链路追踪继续复盘。');
    expect(pageLogs).toHaveBeenCalledWith(expect.objectContaining({
      traceId: 'trace-001',
      deviceCode: 'demo-device-01',
      productKey: 'demo-product',
      requestMethod: 'MQTT',
      requestUrl: '$dp'
    }));
  });

  it('restores diagnostic source when system-log only carries MQTT requestUrl', async () => {
    const now = new Date().toISOString();
    installSessionStorageMock({
      'iot-access:diagnostic-context': JSON.stringify({
        storedAt: Date.now(),
        context: {
          sourcePage: 'message-trace',
          topic: '$dp',
          capturedAt: now
        }
      })
    });
    mockRoute.path = '/system-log';
    mockRoute.query = {
      requestMethod: 'MQTT',
      requestUrl: '$dp'
    };
    vi.mocked(getSystemErrorStats).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        todayCount: 1,
        mqttCount: 1,
        systemCount: 0,
        distinctTraceCount: 0,
        distinctDeviceCount: 0,
        topModules: [],
        topExceptionClasses: [],
        topErrorCodes: []
      }
    });

    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).toContain('来自链路追踪台');
    expect(wrapper.text()).toContain('当前节点：后台异常核对');
    expect(pageLogs).toHaveBeenCalledWith(expect.objectContaining({
      requestMethod: 'MQTT',
      requestUrl: '$dp'
    }));
  });

  it('persists system-log diagnostic context before jumping back to message trace from row actions', async () => {
    mockRoute.query = {
      traceId: 'trace-001',
      deviceCode: 'demo-device-01',
      productKey: 'demo-product',
      requestMethod: 'MQTT',
      requestUrl: '$dp'
    };
    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    await findButtonByText(wrapper, '追踪')!.trigger('click');
    await flushPromises();

    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/message-trace',
      query: {
        traceId: 'trace-001',
        deviceCode: 'demo-device-01',
        productKey: 'demo-product',
        topic: '$dp'
      }
    });

    const persistedRaw = window.sessionStorage.getItem('iot-access:diagnostic-context');
    expect(persistedRaw).toBeTruthy();
    const persisted = JSON.parse(persistedRaw as string);
    expect(persisted.context.sourcePage).toBe('system-log');
    expect(persisted.context.topic).toBe('$dp');
    expect(persisted.context.reportStatus).toBe('failed');
  });

  it('uses shared workbench row actions and mobile list grammar in system mode', () => {
    const source = readFileSync(resolve(import.meta.dirname, '../../views/AuditLogView.vue'), 'utf8');

    expect(source).toContain('<StandardWorkbenchRowActions');
    expect(source).toContain('standard-list-surface');
    expect(source).toContain('standard-mobile-record-grid');
    expect(source).not.toContain('gap="compact"');
    expect(source).not.toContain("gap: 'compact'");
    expect(source).not.toContain('menu-label="更多"');
  });
});
