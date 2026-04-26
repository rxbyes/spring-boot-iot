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
import {
  getObservabilityMessageArchiveBatchCompare,
  getObservabilityMessageArchiveBatchOverview,
  getObservabilityMessageArchiveBatchReportPreview,
  getTraceEvidence,
  listObservabilitySlowSpanSummaries,
  listObservabilitySlowSpanTrends,
  pageObservabilityMessageArchiveBatches,
  pageObservabilityScheduledTasks,
  pageObservabilitySpans
} from '@/api/observability';
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
  getObservabilityMessageArchiveBatchCompare: vi.fn(),
  getObservabilityMessageArchiveBatchOverview: vi.fn(),
  getObservabilityMessageArchiveBatchReportPreview: vi.fn(),
  pageObservabilityMessageArchiveBatches: vi.fn(),
  pageObservabilityScheduledTasks: vi.fn(),
  listObservabilitySlowSpanSummaries: vi.fn(),
  listObservabilitySlowSpanTrends: vi.fn(),
  pageObservabilitySpans: vi.fn(),
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
    vi.mocked(getObservabilityMessageArchiveBatchCompare).mockReset();
    vi.mocked(getObservabilityMessageArchiveBatchOverview).mockReset();
    vi.mocked(getObservabilityMessageArchiveBatchReportPreview).mockReset();
    vi.mocked(pageObservabilityMessageArchiveBatches).mockReset();
    vi.mocked(pageObservabilityScheduledTasks).mockReset();
    vi.mocked(listObservabilitySlowSpanSummaries).mockReset();
    vi.mocked(listObservabilitySlowSpanTrends).mockReset();
    vi.mocked(pageObservabilitySpans).mockReset();
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
    vi.mocked(pageObservabilityMessageArchiveBatches).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 5,
        records: [
          {
            id: 51,
            batchNo: 'iot_message_log-20260426000119',
            sourceTable: 'iot_message_log',
            governanceMode: 'APPLY',
            status: 'SUCCEEDED',
            retentionDays: 30,
            cutoffAt: '2026-03-27 00:00:00',
            confirmReportPath: 'logs/observability/observability-log-governance-20260425-235900.json',
            confirmReportGeneratedAt: '2026-04-25 23:59:00',
            confirmedExpiredRows: 16098,
            candidateRows: 16098,
            archivedRows: 16098,
            deletedRows: 16098,
            compareStatus: 'MATCHED',
            compareStatusLabel: '已对齐',
            deltaConfirmedVsDeleted: 0,
            deltaDryRunVsDeleted: 0,
            remainingExpiredRows: 0,
            previewAvailable: true,
            artifactsJson: '{"reportJsonPath":"logs/observability/observability-log-governance-20260426-000200.json","reportMarkdownPath":"logs/observability/observability-log-governance-20260426-000200.md"}',
            createTime: '2026-04-26 00:01:19',
            updateTime: '2026-04-26 00:02:00'
          }
        ]
      }
    });
    vi.mocked(getObservabilityMessageArchiveBatchOverview).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        totalBatches: 4,
        matchedBatches: 1,
        driftedBatches: 1,
        partialBatches: 1,
        unavailableBatches: 1,
        abnormalBatches: 3,
        totalDeltaConfirmedVsDeleted: 18,
        totalRemainingExpiredRows: 18,
        latestAbnormalBatch: 'iot_message_log-20260426090100',
        latestAbnormalOccurredAt: '2026-04-26 09:01:00'
      }
    });
    vi.mocked(getObservabilityMessageArchiveBatchCompare).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        batchNo: 'iot_message_log-20260426000119',
        sourceTable: 'iot_message_log',
        status: 'SUCCEEDED',
        compareStatus: 'MATCHED',
        compareMessage: '已按确认结果落地',
        sources: {
          confirmReportPath: 'logs/observability/observability-log-governance-20260425-235900.json',
          resolvedDryRunJsonPath: 'logs/observability/observability-log-governance-20260425-235900.json',
          resolvedApplyJsonPath: 'logs/observability/observability-log-governance-20260426-000200.json',
          dryRunAvailable: true,
          applyAvailable: true
        },
        summaryCompare: {
          confirmedExpiredRows: 16098,
          dryRunExpiredRows: 16098,
          applyArchivedRows: 16098,
          applyDeletedRows: 16098,
          remainingExpiredRows: 0,
          deltaConfirmedVsDeleted: 0,
          deltaDryRunVsDeleted: 0,
          matched: true
        },
        tableComparisons: [
          {
            tableName: 'iot_message_log',
            label: '消息热表',
            dryRunExpiredRows: 16098,
            applyArchivedRows: 16098,
            applyDeletedRows: 16098,
            applyRemainingExpiredRows: 0,
            deltaDryRunVsDeleted: 0,
            matched: true
          }
        ]
      }
    });
    vi.mocked(getObservabilityMessageArchiveBatchReportPreview).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        batchNo: 'iot_message_log-20260426000119',
        sourceTable: 'iot_message_log',
        status: 'SUCCEEDED',
        confirmReportPath: 'logs/observability/observability-log-governance-20260425-235900.json',
        confirmReportGeneratedAt: '2026-04-25 23:59:00',
        available: true,
        resolvedJsonPath: 'logs/observability/observability-log-governance-20260425-235900.json',
        resolvedMarkdownPath: 'logs/observability/observability-log-governance-20260425-235900.md',
        markdownAvailable: true,
        markdownTruncated: false,
        markdownPreview: '# 归档报告\n- APPLY succeeded',
        fileLastModifiedAt: '2026-04-26 00:02:00',
        summary: {
          generatedAt: '2026-04-26T00:02:00',
          mode: 'APPLY',
          expiredRows: 16098,
          deletedRows: 16098,
          tablesWithExpiredRows: 1
        },
        tableSummaries: [
          {
            tableName: 'iot_message_log',
            label: '消息热表',
            retentionDays: 30,
            cutoffAt: '2026-03-27 00:00:00',
            expiredRows: 16098,
            deletedRows: 16098,
            remainingExpiredRows: 0,
            earliestRecordAt: '2026-03-01 00:00:00',
            latestRecordAt: '2026-03-26 23:59:59'
          }
        ]
      }
    });
    vi.mocked(pageObservabilityScheduledTasks).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 2,
        pageNum: 1,
        pageSize: 5,
        records: [
          {
            id: 41,
            traceId: 'trace-scheduled-1',
            domainCode: 'device',
            taskCode: 'DeviceSessionTimeoutScheduler#closeTimedOutSessions',
            taskName: 'DeviceSessionTimeoutScheduler#closeTimedOutSessions',
            triggerType: 'FIXED_DELAY',
            triggerExpression: '${iot.device.online-timeout-check-delay-millis:30000}',
            initialDelayExpression: '${iot.device.online-timeout-check-delay-millis:30000}',
            status: 'SUCCESS',
            durationMs: 420,
            startedAt: '2026-04-25 10:10:00'
          },
          {
            id: 42,
            traceId: 'trace-scheduled-2',
            domainCode: 'admin',
            taskCode: 'ObservabilityAlertingScheduler#evaluateAlerts',
            taskName: 'ObservabilityAlertingScheduler#evaluateAlerts',
            triggerType: 'FIXED_DELAY',
            triggerExpression: "#{T(java.lang.Math).max(@iotProperties.observability.alerting.evaluateIntervalSeconds, 60) * 1000L}",
            status: 'FAILURE',
            durationMs: 1600,
            startedAt: '2026-04-25 10:05:00',
            errorMessage: 'rule snapshot missing'
          }
        ]
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
    vi.mocked(pageObservabilitySpans).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 2,
        pageNum: 1,
        pageSize: 5,
        records: [
          {
            id: 31,
            traceId: 'trace-slow-1',
            spanType: 'SLOW_SQL',
            spanName: 'Slow SQL iot_message_log',
            domainCode: 'system',
            eventCode: 'system.error.archive',
            objectType: 'sql',
            objectId: 'iot_message_log',
            status: 'SUCCESS',
            durationMs: 2400,
            startedAt: '2026-04-25 10:08:00'
          },
          {
            id: 32,
            traceId: 'trace-slow-2',
            spanType: 'SLOW_SQL',
            spanName: 'Slow SQL iot_message_log',
            domainCode: 'system',
            eventCode: 'system.error.archive',
            objectType: 'sql',
            objectId: 'iot_message_log',
            status: 'ERROR',
            durationMs: 1800,
            startedAt: '2026-04-25 10:06:00'
          }
        ]
      }
    });
    vi.mocked(listObservabilitySlowSpanTrends).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          bucket: 'HOUR',
          bucketStart: '2026-04-25 09:00:00',
          bucketEnd: '2026-04-25 10:00:00',
          totalCount: 3,
          successCount: 2,
          errorCount: 1,
          errorRate: 33,
          avgDurationMs: 2667,
          maxDurationMs: 5000,
          p95DurationMs: 5000,
          p99DurationMs: 5000
        },
        {
          bucket: 'HOUR',
          bucketStart: '2026-04-25 10:00:00',
          bucketEnd: '2026-04-25 11:00:00',
          totalCount: 2,
          successCount: 1,
          errorCount: 1,
          errorRate: 50,
          avgDurationMs: 2250,
          maxDurationMs: 3000,
          p95DurationMs: 3000,
          p99DurationMs: 3000
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

  it('renders scheduled task ledger and opens evidence from a task run', async () => {
    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    expect(pageObservabilityScheduledTasks).toHaveBeenCalledWith({
      pageNum: 1,
      pageSize: 5
    });

    const ledger = wrapper.find('.audit-log-scheduled-task-ledger');
    expect(ledger.exists()).toBe(true);
    expect(ledger.text()).toContain('调度任务台账');
    expect(ledger.text()).toContain('DeviceSessionTimeoutScheduler#closeTimedOutSessions');
    expect(ledger.text()).toContain('FIXED_DELAY');
    expect(ledger.text()).toContain('SUCCESS');
    expect(ledger.text()).toContain('420 ms');

    vi.mocked(getTraceEvidence).mockClear();
    const evidenceButton = ledger.findAll('button').find((button) => button.text().includes('证据'));
    await evidenceButton!.trigger('click');
    await flushPromises();
    await nextTick();

    expect(getTraceEvidence).toHaveBeenCalledWith('trace-scheduled-1');
  });

  it('renders archive batch ledger and opens batch detail from the same workbench', async () => {
    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    expect(getObservabilityMessageArchiveBatchOverview).toHaveBeenCalledWith({
      sourceTable: 'iot_message_log'
    });
    expect(pageObservabilityMessageArchiveBatches).toHaveBeenCalledWith({
      sourceTable: 'iot_message_log',
      pageNum: 1,
      pageSize: 5
    });

    const ledger = wrapper.find('.audit-log-archive-batch-ledger');
    expect(ledger.exists()).toBe(true);
    expect(ledger.text()).toContain('归档批次台账');
    expect(ledger.text()).toContain('异常批次');
    expect(ledger.text()).toContain('3');
    expect(ledger.text()).toContain('执行偏差总量');
    expect(ledger.text()).toContain('+18');
    expect(ledger.text()).toContain('剩余过期总量');
    expect(ledger.text()).toContain('最近异常批次');
    expect(ledger.text()).toContain('iot_message_log-20260426090100');
    expect(ledger.text()).toContain('iot_message_log-20260426000119');
    expect(ledger.text()).toContain('SUCCEEDED');
    expect(ledger.text()).toContain('已对齐');
    expect(ledger.text()).toContain('确认 16098');
    expect(ledger.text()).toContain('归档 16098');
    expect(ledger.text()).toContain('删除 16098');
    expect(ledger.text()).toContain('确认差值 0');
    expect(ledger.text()).toContain('dry-run 差值 0');
    expect(ledger.text()).toContain('剩余过期 0');
    expect(ledger.text()).toContain('报告 可预览');
    expect(ledger.text()).toContain('logs/observability/observability-log-governance-20260425-235900.json');

    const detailButton = ledger.findAll('button').find((button) => button.text().includes('详情'));
    await detailButton!.trigger('click');
    await flushPromises();
    await nextTick();

    expect(getObservabilityMessageArchiveBatchReportPreview).toHaveBeenCalledWith(
      'iot_message_log-20260426000119'
    );
    expect(getObservabilityMessageArchiveBatchCompare).toHaveBeenCalledWith(
      'iot_message_log-20260426000119'
    );

    const drawer = wrapper
      .findAll('.observability-evidence-drawer-stub')
      .find((item) => item.text().includes('归档批次详情'));
    expect(drawer?.exists()).toBe(true);
    expect(drawer?.text()).toContain('iot_message_log-20260426000119');
    expect(drawer?.text()).toContain('logs/observability/observability-log-governance-20260425-235900.json');
    expect(drawer?.text()).toContain('logs/observability/observability-log-governance-20260426-000200.json');
    expect(drawer?.text()).toContain('logs/observability/observability-log-governance-20260426-000200.md');
    expect(drawer?.text()).toContain('批次对比');
    expect(drawer?.text()).toContain('已按确认结果落地');
    expect(drawer?.text()).toContain('确认过期');
    expect(drawer?.text()).toContain('dry-run 过期');
    expect(drawer?.text()).toContain('apply 删除');
    expect(drawer?.text()).toContain('已对齐');
    expect(drawer?.text()).toContain('确认报告预览');
    expect(drawer?.text()).toContain('消息热表');
    expect(drawer?.text()).toContain('过期 16098');
    expect(drawer?.text()).toContain('# 归档报告');
  });

  it('filters archive batch ledger by batch number, status and create date window', async () => {
    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    vi.mocked(pageObservabilityMessageArchiveBatches).mockClear();
    vi.mocked(getObservabilityMessageArchiveBatchOverview).mockClear();

    await wrapper.get('[data-testid="archive-batch-filter-batch-no"]').setValue(
      'iot_message_log-20260426000119'
    );
    await wrapper.get('[data-testid="archive-batch-filter-status"]').setValue('SUCCEEDED');
    await wrapper.get('[data-testid="archive-batch-filter-compare-status"]').setValue('DRIFTED');
    await wrapper.get('[data-testid="archive-batch-filter-only-abnormal"]').setValue(true);
    await wrapper.get('[data-testid="archive-batch-filter-date-from"]').setValue('2026-04-26');
    await wrapper.get('[data-testid="archive-batch-filter-date-to"]').setValue('2026-04-26');

    await wrapper.get('[data-testid="archive-batch-search-button"]').trigger('click');
    await flushPromises();
    await nextTick();

    expect(pageObservabilityMessageArchiveBatches).toHaveBeenCalledWith({
      batchNo: 'iot_message_log-20260426000119',
      sourceTable: 'iot_message_log',
      status: 'SUCCEEDED',
      compareStatus: 'DRIFTED',
      onlyAbnormal: true,
      dateFrom: '2026-04-26 00:00:00',
      dateTo: '2026-04-26 23:59:59',
      pageNum: 1,
      pageSize: 5
    });
    expect(getObservabilityMessageArchiveBatchOverview).toHaveBeenCalledWith({
      sourceTable: 'iot_message_log',
      dateFrom: '2026-04-26 00:00:00',
      dateTo: '2026-04-26 23:59:59'
    });
  });

  it('drills slow hotspot into recent span records and opens evidence from a span row', async () => {
    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    const panel = wrapper.find('.audit-log-slow-summary');
    const detailButton = panel.findAll('button').find((button) => button.text().includes('明细'));
    await detailButton!.trigger('click');
    await flushPromises();
    await nextTick();

    expect(pageObservabilitySpans).toHaveBeenCalledWith({
      spanType: 'SLOW_SQL',
      domainCode: 'system',
      eventCode: 'system.error.archive',
      objectType: 'sql',
      objectId: 'iot_message_log',
      minDurationMs: 1,
      pageNum: 1,
      pageSize: 5
    });

    const drilldown = wrapper.find('.audit-log-slow-span-drilldown');
    expect(drilldown.exists()).toBe(true);
    expect(drilldown.text()).toContain('慢点明细');
    expect(drilldown.text()).toContain('Slow SQL iot_message_log');
    expect(drilldown.text()).toContain('trace-slow-1');
    expect(drilldown.text()).toContain('2400 ms');
    expect(drilldown.text()).toContain('SUCCESS');

    vi.mocked(getTraceEvidence).mockClear();
    const evidenceButton = drilldown.findAll('button').find((button) => button.text().includes('证据'));
    await evidenceButton!.trigger('click');
    await flushPromises();
    await nextTick();

    expect(getTraceEvidence).toHaveBeenCalledWith('trace-slow-1');
    expect(wrapper.find('.observability-evidence-drawer-stub').exists()).toBe(true);
  });

  it('drills slow hotspot into trend buckets and allows switching trend windows', async () => {
    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    const panel = wrapper.find('.audit-log-slow-summary');
    const trendButton = panel.findAll('button').find((button) => button.text().includes('趋势'));
    await trendButton!.trigger('click');
    await flushPromises();
    await nextTick();

    expect(listObservabilitySlowSpanTrends).toHaveBeenCalledWith(expect.objectContaining({
      spanType: 'SLOW_SQL',
      domainCode: 'system',
      eventCode: 'system.error.archive',
      objectType: 'sql',
      objectId: 'iot_message_log',
      minDurationMs: 1,
      bucket: 'HOUR'
    }));

    const drilldown = wrapper.find('.audit-log-slow-trend-drilldown');
    expect(drilldown.exists()).toBe(true);
    expect(drilldown.text()).toContain('慢点趋势');
    expect(drilldown.text()).toContain('P95');
    expect(drilldown.text()).toContain('P99');
    expect(drilldown.text()).toContain('错误率');
    expect(drilldown.text()).toContain('2026-04-25 10:00:00');
    expect(drilldown.text()).toContain('5000 ms');
    expect(drilldown.text()).toContain('33%');

    vi.mocked(listObservabilitySlowSpanTrends).mockClear();
    const dayButton = drilldown.findAll('button').find((button) => button.text().includes('7天'));
    await dayButton!.trigger('click');
    await flushPromises();
    await nextTick();

    expect(listObservabilitySlowSpanTrends).toHaveBeenCalledWith(expect.objectContaining({
      spanType: 'SLOW_SQL',
      domainCode: 'system',
      eventCode: 'system.error.archive',
      objectType: 'sql',
      objectId: 'iot_message_log',
      minDurationMs: 1,
      bucket: 'DAY'
    }));
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
