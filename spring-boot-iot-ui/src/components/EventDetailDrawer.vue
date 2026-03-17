<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    eyebrow="Event Detail"
    :title="drawerTitle"
    :subtitle="drawerSubtitle"
    :tags="drawerTags"
    :loading="loading"
    loading-text="正在加载事件详情..."
    :error-message="errorMessage"
    :empty="!detail"
    empty-text="暂无事件详情"
    @update:modelValue="emit('update:modelValue', $event)"
  >
    <section class="detail-panel">
      <h3>事件概览</h3>
      <div class="detail-grid">
        <div class="detail-field">
          <span class="detail-field__label">事件编号</span>
          <strong class="detail-field__value">{{ detail?.eventCode || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">事件标题</span>
          <strong class="detail-field__value">{{ detail?.eventTitle || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">风险等级</span>
          <strong class="detail-field__value">{{ getRiskLevelText(detail?.riskLevel) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">当前状态</span>
          <strong class="detail-field__value">{{ getStatusText(detail?.status) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">关联告警</span>
          <strong class="detail-field__value">{{ detail?.alarmCode || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">紧急程度</span>
          <strong class="detail-field__value">{{ detail?.urgencyLevel || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">区域</span>
          <strong class="detail-field__value">{{ detail?.regionName || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">风险点</span>
          <strong class="detail-field__value">{{ detail?.riskPointName || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">设备编码</span>
          <strong class="detail-field__value">{{ detail?.deviceCode || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">设备名称</span>
          <strong class="detail-field__value">{{ detail?.deviceName || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">测点名称</span>
          <strong class="detail-field__value">{{ detail?.metricName || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">当前值</span>
          <strong class="detail-field__value">{{ detail?.currentValue || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">触发时间</span>
          <strong class="detail-field__value">{{ formatDateTime(detail?.triggerTime) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">责任人</span>
          <strong class="detail-field__value">{{ detail?.responsibleUser || '--' }}</strong>
        </div>
      </div>
    </section>

    <section class="detail-panel">
      <h3>处置进度</h3>
      <div class="detail-grid">
        <div class="detail-field">
          <span class="detail-field__label">派发人</span>
          <strong class="detail-field__value">{{ detail?.dispatchUser || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">派发时间</span>
          <strong class="detail-field__value">{{ formatDateTime(detail?.dispatchTime) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">开始处理</span>
          <strong class="detail-field__value">{{ formatDateTime(detail?.startTime) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">处理完成</span>
          <strong class="detail-field__value">{{ formatDateTime(detail?.completeTime) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">关闭时间</span>
          <strong class="detail-field__value">{{ formatDateTime(detail?.closeTime) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">关闭人</span>
          <strong class="detail-field__value">{{ detail?.closeUser || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">到场时限</span>
          <strong class="detail-field__value">{{ formatDuration(detail?.arrivalTimeLimit) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">完成时限</span>
          <strong class="detail-field__value">{{ formatDuration(detail?.completionTimeLimit) }}</strong>
        </div>
      </div>
    </section>

    <section class="detail-panel">
      <h3>处置说明</h3>
      <div class="detail-grid">
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">关闭原因</span>
          <strong class="detail-field__value detail-field__value--plain">{{ detail?.closeReason || '--' }}</strong>
        </div>
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">复核说明</span>
          <strong class="detail-field__value detail-field__value--plain">{{ detail?.reviewNotes || '--' }}</strong>
        </div>
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">备注</span>
          <strong class="detail-field__value detail-field__value--plain">{{ detail?.remark || '--' }}</strong>
        </div>
      </div>
    </section>
  </StandardDetailDrawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import type { EventRecord } from '@/api/alarm';
import { formatDateTime } from '@/utils/format';
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue';

const props = defineProps<{
  modelValue: boolean;
  detail: EventRecord | null;
  loading?: boolean;
  errorMessage?: string;
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
}>();

const drawerTitle = computed(() => props.detail?.eventTitle || props.detail?.eventCode || '事件详情');
const drawerSubtitle = computed(() => (props.detail?.eventCode ? `事件编号：${props.detail.eventCode}` : '查看事件处置全流程详情'));
const drawerTags = computed(() => {
  if (!props.detail) {
    return [];
  }
  return [
    { label: getRiskLevelText(props.detail.riskLevel), type: getRiskLevelType(props.detail.riskLevel) },
    { label: getStatusText(props.detail.status), type: getStatusType(props.detail.status) },
    { label: props.detail.urgencyLevel || '普通', type: 'info' as const }
  ];
});

function getRiskLevelText(level?: string | null) {
  switch ((level || '').toLowerCase()) {
    case 'critical':
      return '严重';
    case 'warning':
      return '警告';
    case 'info':
      return '提醒';
    default:
      return level || '--';
  }
}

function getRiskLevelType(level?: string | null): 'danger' | 'warning' | 'info' {
  switch ((level || '').toLowerCase()) {
    case 'critical':
      return 'danger';
    case 'warning':
      return 'warning';
    default:
      return 'info';
  }
}

function getStatusText(status?: number | null) {
  switch (status) {
    case 0:
      return '待派发';
    case 1:
      return '已派发';
    case 2:
      return '处理中';
    case 3:
      return '待验收';
    case 4:
      return '已关闭';
    case 5:
      return '已取消';
    default:
      return '--';
  }
}

function getStatusType(status?: number | null): 'danger' | 'warning' | 'primary' | 'success' | 'info' {
  switch (status) {
    case 0:
      return 'danger';
    case 1:
      return 'warning';
    case 2:
      return 'primary';
    case 4:
      return 'success';
    default:
      return 'info';
  }
}

function formatDuration(hours?: number | null) {
  if (hours === null || hours === undefined) {
    return '--';
  }
  return `${hours} 小时`;
}
</script>
