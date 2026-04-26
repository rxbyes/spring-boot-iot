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
    total: 1,
    rows: [
      {
        id: 1,
        batchNo: 'batch-20260426-01',
        createTime: '2026-04-26 10:00:00',
        updateTime: '2026-04-26 10:10:00',
        status: 'DONE',
        compareStatus: 'DRIFTED',
        compareStatusLabel: '有偏差',
        sourceTable: 'iot_message_log',
        retentionDays: 30,
        cutoffAt: '2026-03-27 00:00:00',
        confirmedExpiredRows: 18,
        candidateRows: 20,
        archivedRows: 18,
        deletedRows: 17,
        deltaConfirmedVsDeleted: -1,
        deltaDryRunVsDeleted: -3,
        remainingExpiredRows: 1
      }
    ],
    errorMessage: '',
    overviewLoading: false,
    overviewErrorMessage: '',
    focusHint: '最近异常批次不在当前结果中，请调整时间范围后重试',
    filters: {
      batchNo: '',
      status: '',
      compareStatus: '',
      dateFrom: '',
      dateTo: '',
      onlyAbnormal: false
    },
    statusOptions: [{ label: '已完成', value: 'DONE' }],
    compareStatusOptions: [{ label: '有偏差', value: 'DRIFTED' }],
    overviewCards: [
      {
        key: 'abnormal',
        label: '最近异常批次',
        value: '3',
        meta: '最近 24 小时',
        clickable: true,
        active: true,
        testId: 'archive-batch-overview-abnormal'
      }
    ],
    formatValue: (value: string | null | undefined) => value ?? '--',
    formatCount: (value: number | null | undefined) => `${value ?? 0}`,
    formatOptionalCount: (value: number | null | undefined) => `${value ?? 0}`,
    formatSignedCount: (value: number | null | undefined) => `${value ?? 0}`,
    formatRetentionDays: (value: number | null | undefined) => `${value ?? 0} 天`,
    formatArchiveBatchName: () => '批次 batch-20260426-01',
    formatArchiveBatchCompareStatus: () => '有偏差',
    formatArchiveBatchPreviewAvailability: () => 'JSON + Markdown',
    formatArchiveBatchFooter: () => 'dry-run 与 apply 存在偏差',
    resolveArchiveBatchCompareStatusClass: () => 'is-drifted',
    isArchiveBatchAbnormalStatus: () => true
  };
}

describe('AuditLogArchiveTabPanel', () => {
  it('renders archive overview cards and emits row actions without showing hotspot sections', async () => {
    const wrapper = mount(AuditLogArchiveTabPanel, {
      props: archivePropsFactory(),
      global: {
        stubs: {
          StandardButton: StandardButtonStub
        }
      }
    });

    expect(wrapper.find('[data-testid="system-log-archive-panel"]').exists()).toBe(true);
    expect(wrapper.text()).toContain('归档批次台账');
    expect(wrapper.text()).not.toContain('性能慢点 Top');
    await wrapper.get('[data-testid="archive-batch-open-detail"]').trigger('click');
    expect(wrapper.emitted('open-detail')).toHaveLength(1);
  });

  it('emits filter edits and overview-card selections with focused payloads', async () => {
    const wrapper = mount(AuditLogArchiveTabPanel, {
      props: archivePropsFactory(),
      global: {
        stubs: {
          StandardButton: StandardButtonStub
        }
      }
    });

    await wrapper.get('[data-testid="archive-batch-filter-batch-no"]').setValue('batch-20260426');
    await wrapper.get('[data-testid="archive-batch-filter-status"]').setValue('DONE');
    await wrapper.get('[data-testid="archive-batch-filter-compare-status"]').setValue('DRIFTED');
    await wrapper.get('[data-testid="archive-batch-overview-abnormal"]').trigger('click');

    expect(wrapper.emitted('update-filter')).toEqual([
      [{ field: 'batchNo', value: 'batch-20260426' }],
      [{ field: 'status', value: 'DONE' }],
      [{ field: 'compareStatus', value: 'DRIFTED' }]
    ]);
    expect(wrapper.emitted('select-overview-card')?.[0]).toEqual(['abnormal']);
  });

  it('renders the archive overview error branch when overview aggregation fails', () => {
    const wrapper = mount(AuditLogArchiveTabPanel, {
      props: {
        ...archivePropsFactory(),
        rows: [],
        overviewErrorMessage: '批次摘要汇总失败'
      },
      global: {
        stubs: {
          StandardButton: StandardButtonStub
        }
      }
    });

    expect(wrapper.text()).toContain('批次摘要汇总失败');
  });
});
