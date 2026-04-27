import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import AuditLogArchiveTabPanel from '@/components/auditLog/AuditLogArchiveTabPanel.vue';

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
});

function archivePropsFactory() {
  return {
    loading: false,
    total: 2,
    rows: [
      {
        id: 1,
        batchNo: 'iot_message_log-20260426000119',
        sourceTable: 'iot_message_log',
        status: 'SUCCEEDED',
        compareStatus: 'MATCHED',
        compareStatusLabel: '已对齐',
        retentionDays: 30,
        cutoffAt: '2026-03-27 00:00:00',
        confirmedExpiredRows: 16098,
        candidateRows: 16098,
        archivedRows: 16098,
        deletedRows: 16098,
        deltaConfirmedVsDeleted: 0,
        deltaDryRunVsDeleted: 0,
        remainingExpiredRows: 0,
        createTime: '2026-04-26 00:01:19',
        updateTime: '2026-04-26 00:02:00'
      },
      {
        id: 2,
        batchNo: 'iot_message_log-20260426090100',
        sourceTable: 'iot_message_log',
        status: 'FAILED',
        compareStatus: 'DRIFTED',
        compareStatusLabel: '有偏差',
        retentionDays: 30,
        cutoffAt: '2026-03-27 09:00:00',
        confirmedExpiredRows: 320,
        candidateRows: 320,
        archivedRows: 0,
        deletedRows: 12,
        deltaConfirmedVsDeleted: 308,
        deltaDryRunVsDeleted: 308,
        remainingExpiredRows: 308,
        createTime: '2026-04-26 09:01:00',
        updateTime: '2026-04-26 09:03:00'
      }
    ],
    errorMessage: '',
    overviewLoading: false,
    overviewErrorMessage: '',
    focusHint: '',
    filters: {
      batchNo: '',
      status: '',
      compareStatus: '',
      dateFrom: '',
      dateTo: '',
      onlyAbnormal: false
    },
    statusOptions: [{ label: '成功', value: 'SUCCEEDED' }],
    compareStatusOptions: [{ label: '已对齐', value: 'MATCHED' }],
    overviewCards: [
      {
        key: 'abnormal',
        label: '异常批次',
        value: '2',
        meta: '总批次 4',
        clickable: true,
        active: false,
        testId: 'archive-batch-overview-abnormal'
      },
      {
        key: 'drifted',
        label: '执行偏差总量',
        value: '308',
        meta: '已对齐 1',
        clickable: true,
        active: false,
        testId: 'archive-batch-overview-drifted'
      },
      {
        key: 'remaining',
        label: '剩余过期总量',
        value: '308',
        meta: '部分可比 1',
        clickable: true,
        active: false,
        testId: 'archive-batch-overview-remaining'
      }
    ],
    latestAbnormalFocus: {
      batchNo: 'iot_message_log-20260426090100',
      occurredAt: '2026-04-26 09:01:00',
      active: true
    },
    activeRow: {
      id: 2,
      batchNo: 'iot_message_log-20260426090100',
      sourceTable: 'iot_message_log',
      status: 'FAILED',
      compareStatus: 'DRIFTED',
      compareStatusLabel: '有偏差',
      retentionDays: 30,
      cutoffAt: '2026-03-27 09:00:00',
      confirmedExpiredRows: 320,
      candidateRows: 320,
      archivedRows: 0,
      deletedRows: 12,
      deltaConfirmedVsDeleted: 308,
      deltaDryRunVsDeleted: 308,
      remainingExpiredRows: 308,
      createTime: '2026-04-26 09:01:00',
      updateTime: '2026-04-26 09:03:00'
    },
    selectedBatchKey: 'iot_message_log-20260426090100',
    formatValue: (value: string | number | null | undefined) => String(value ?? '--'),
    formatCount: (value: number | null | undefined) => String(value ?? 0),
    formatOptionalCount: (value: number | null | undefined) => String(value ?? 0),
    formatSignedCount: (value: number | null | undefined) => String(value ?? 0),
    formatRetentionDays: (value: number | null | undefined) => `${value ?? 0} 天`,
    formatArchiveBatchName: (row: { batchNo?: string | null }) => String(row.batchNo ?? '--'),
    formatArchiveBatchCompareStatus: (value: string | number | null | undefined) => String(value ?? '--'),
    formatArchiveBatchPreviewAvailability: () => '可预览',
    formatArchiveBatchFooter: () => 'apply 已完成',
    resolveArchiveBatchCompareStatusClass: (row: { compareStatus?: string | null }) =>
      `is-${String(row.compareStatus || '').toLowerCase()}`,
    isArchiveBatchAbnormalStatus: (status: string | null | undefined) => status === 'DRIFTED'
  };
}

describe('AuditLogArchiveTabPanel', () => {
  it('renders three summary metrics and a scan-first archive master table', () => {
    const wrapper = mount(AuditLogArchiveTabPanel, {
      props: archivePropsFactory(),
      global: {
        stubs: {
          StandardButton: StandardButtonStub
        }
      }
    });

    expect(wrapper.findAll('.audit-log-archive-batch-ledger__overview-card')).toHaveLength(3);
    expect(wrapper.find('[data-testid="archive-batch-latest-focus"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="archive-batch-latest-focus"]').text()).toContain(
      'iot_message_log-20260426090100'
    );
    expect(wrapper.find('[data-testid="archive-governance-focus-strip"]').exists()).toBe(false);
    expect(wrapper.find('[data-testid="archive-batch-master-table"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="archive-batch-master-header"]').text()).toContain('归档批次');
    expect(wrapper.find('[data-testid="archive-batch-master-header"]').text()).toContain('执行状态');
    expect(wrapper.find('[data-testid="archive-batch-master-header"]').text()).toContain('对比结论');
    expect(wrapper.find('[data-testid="archive-batch-master-header"]').text()).toContain('风险信号');
    expect(wrapper.find('[data-testid="archive-batch-master-header"]').text()).toContain('最近时间');
    expect(wrapper.find('[data-testid="archive-batch-master-header"]').text()).toContain('操作');
    expect(wrapper.findAll('[data-testid="archive-batch-master-row"]')[1]?.classes()).toContain('is-selected');
    expect(wrapper.findAll('[data-testid="archive-batch-master-row"]')[1]?.text()).toContain('iot_message_log');
    expect(wrapper.findAll('[data-testid="archive-batch-master-row"]')[1]?.text()).toContain('30 天');
    expect(wrapper.findAll('[data-testid="archive-batch-master-row"]')[1]?.text()).toContain('偏差 308');
    expect(wrapper.findAll('[data-testid="archive-batch-master-row"]')[1]?.text()).toContain('剩余 308');
    expect(wrapper.findAll('[data-testid="archive-batch-master-row"]')[1]?.text()).toContain('截止 2026-03-27 09:00:00');
  });

  it('emits select-latest-abnormal when choosing the lightweight latest focus entry', async () => {
    const wrapper = mount(AuditLogArchiveTabPanel, {
      props: archivePropsFactory(),
      global: {
        stubs: {
          StandardButton: StandardButtonStub
        }
      }
    });

    await wrapper.get('[data-testid="archive-batch-latest-focus"]').trigger('click');

    expect(wrapper.emitted('select-latest-abnormal')?.[0]).toEqual([]);
  });

  it('emits select-row when choosing a batch from the master table', async () => {
    const wrapper = mount(AuditLogArchiveTabPanel, {
      props: archivePropsFactory(),
      global: {
        stubs: {
          StandardButton: StandardButtonStub
        }
      }
    });

    await wrapper.findAll('[data-testid="archive-batch-master-row"]')[0]?.trigger('click');

    expect(wrapper.emitted('select-row')?.[0]?.[0]).toMatchObject({
      batchNo: 'iot_message_log-20260426000119'
    });
  });
});
