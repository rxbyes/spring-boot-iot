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
        key: 'latest',
        label: '最近异常批次',
        value: 'iot_message_log-20260426090100',
        meta: '2026-04-26 09:01:00',
        clickable: true,
        active: true,
        testId: 'archive-batch-overview-latest'
      }
    ],
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
  it('renders an archive focus strip and highlights the selected batch row', () => {
    const wrapper = mount(AuditLogArchiveTabPanel, {
      props: archivePropsFactory(),
      global: {
        stubs: {
          StandardButton: StandardButtonStub
        }
      }
    });

    expect(wrapper.find('[data-testid="archive-governance-focus-strip"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="archive-governance-focus-strip"]').text()).toContain(
      'iot_message_log-20260426090100'
    );
    expect(wrapper.find('[data-testid="archive-batch-master-table"]').exists()).toBe(true);
    expect(wrapper.findAll('[data-testid="archive-batch-master-row"]')[1]?.classes()).toContain('is-selected');
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
