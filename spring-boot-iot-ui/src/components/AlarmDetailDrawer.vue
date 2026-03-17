<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    eyebrow="Alarm Detail"
    :title="drawerTitle"
    :subtitle="drawerSubtitle"
    :tags="drawerTags"
    :loading="loading"
    loading-text="正在加载告警详情..."
    :error-message="errorMessage"
    :empty="!detail"
    empty-text="暂无告警详情"
    @update:modelValue="emit('update:modelValue', $event)"
  >
    <section class="detail-panel">
      <h3>告警概览</h3>
      <div class="detail-grid">
        <div class="detail-field">
          <span class="detail-field__label">告警编号</span>
          <strong class="detail-field__value">{{ detail?.alarmCode || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">告警标题</span>
          <strong class="detail-field__value">{{ detail?.alarmTitle || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">告警等级</span>
          <strong class="detail-field__value">{{ getAlarmLevelText(detail?.alarmLevel) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">告警类型</span>
          <strong class="detail-field__value">{{ detail?.alarmType || '--' }}</strong>
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
          <span class="detail-field__label">规则名称</span>
          <strong class="detail-field__value">{{ detail?.ruleName || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">当前值</span>
          <strong class="detail-field__value">{{ detail?.currentValue || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">阈值</span>
          <strong class="detail-field__value">{{ detail?.thresholdValue || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">触发时间</span>
          <strong class="detail-field__value">{{ formatDateTime(detail?.triggerTime) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">当前状态</span>
          <strong class="detail-field__value">{{ getStatusText(detail?.status) }}</strong>
        </div>
      </div>
    </section>

    <section class="detail-panel">
      <h3>处置记录</h3>
      <div class="detail-grid">
        <div class="detail-field">
          <span class="detail-field__label">确认时间</span>
          <strong class="detail-field__value">{{ formatDateTime(detail?.confirmTime) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">确认人</span>
          <strong class="detail-field__value">{{ detail?.confirmUser || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">抑制时间</span>
          <strong class="detail-field__value">{{ formatDateTime(detail?.suppressTime) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">抑制人</span>
          <strong class="detail-field__value">{{ detail?.suppressUser || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">关闭时间</span>
          <strong class="detail-field__value">{{ formatDateTime(detail?.closeTime) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">关闭人</span>
          <strong class="detail-field__value">{{ detail?.closeUser || '--' }}</strong>
        </div>
      </div>
    </section>

    <section class="detail-panel">
      <h3>补充信息</h3>
      <div class="detail-grid">
        <div class="detail-field">
          <span class="detail-field__label">创建时间</span>
          <strong class="detail-field__value">{{ formatDateTime(detail?.createTime) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">更新时间</span>
          <strong class="detail-field__value">{{ formatDateTime(detail?.updateTime) }}</strong>
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

import type { AlarmRecord } from '@/api/alarm';
import { formatDateTime } from '@/utils/format';
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue';

const props = defineProps<{
  modelValue: boolean;
  detail: AlarmRecord | null;
  loading?: boolean;
  errorMessage?: string;
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
}>();

const drawerTitle = computed(() => props.detail?.alarmTitle || props.detail?.alarmCode || '告警详情');
const drawerSubtitle = computed(() => (props.detail?.alarmCode ? `告警编号：${props.detail.alarmCode}` : '查看告警触发与处置详情'));
const drawerTags = computed(() => {
  if (!props.detail) {
    return [];
  }
  return [
    { label: getAlarmLevelText(props.detail.alarmLevel), type: getAlarmLevelType(props.detail.alarmLevel) },
    { label: getStatusText(props.detail.status), type: getStatusType(props.detail.status) },
    { label: props.detail.alarmType || '告警', type: 'info' as const }
  ];
});

function getAlarmLevelText(level?: string | null) {
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

function getAlarmLevelType(level?: string | null): 'danger' | 'warning' | 'info' {
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
      return '未确认';
    case 1:
      return '已确认';
    case 2:
      return '已抑制';
    case 3:
      return '已关闭';
    default:
      return '--';
  }
}

function getStatusType(status?: number | null): 'danger' | 'success' | 'info' {
  switch (status) {
    case 0:
      return 'danger';
    case 1:
      return 'success';
    default:
      return 'info';
  }
}
</script>
