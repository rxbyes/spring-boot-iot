<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    eyebrow="Risk Monitoring Detail"
    :title="detail?.riskPointName || detail?.deviceName || '监测详情'"
    :subtitle="detail?.deviceCode ? `设备编码：${detail.deviceCode}` : '查看风险监测对象当前态势'"
    :tags="drawerTags"
    :loading="loading"
    loading-text="正在加载监测详情..."
    :error-message="errorMessage"
    :empty="!detail"
    empty-text="暂无监测详情"
    @update:modelValue="emit('update:modelValue', $event)"
  >
    <section class="detail-panel">
      <h3>当前监测信息</h3>
      <div class="detail-grid">
        <div class="detail-field"><span class="detail-field__label">设备编码</span><strong class="detail-field__value">{{ detail?.deviceCode || '--' }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">设备名称</span><strong class="detail-field__value">{{ detail?.deviceName || '--' }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">产品名称</span><strong class="detail-field__value">{{ detail?.productName || '--' }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">区域</span><strong class="detail-field__value">{{ detail?.regionName || '--' }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">风险点</span><strong class="detail-field__value">{{ detail?.riskPointName || '--' }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">测点</span><strong class="detail-field__value">{{ detail?.metricName || detail?.metricIdentifier || '--' }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">当前值</span><strong class="detail-field__value">{{ formatCurrentValue(detail?.currentValue, detail?.unit) }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">最新上报</span><strong class="detail-field__value">{{ formatDateTime(detail?.latestReportTime) }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">活跃告警</span><strong class="detail-field__value">{{ detail?.activeAlarmCount ?? 0 }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">近期事件</span><strong class="detail-field__value">{{ detail?.recentEventCount ?? 0 }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">经纬度</span><strong class="detail-field__value">{{ formatCoordinate(detail?.longitude, detail?.latitude) }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">位置描述</span><strong class="detail-field__value">{{ detail?.address || '--' }}</strong></div>
      </div>
    </section>

    <section class="detail-panel">
      <h3>最近告警</h3>
      <div v-if="recentAlarms.length" class="detail-card-list">
        <article v-for="alarm in recentAlarms" :key="alarm.id" class="detail-card">
          <div class="detail-card__header">
            <strong>{{ alarm.alarmTitle || alarm.alarmCode || `告警 ${alarm.id}` }}</strong>
            <el-tag :type="riskLevelTagType(alarm.alarmLevel)" round>{{ riskLevelText(alarm.alarmLevel) }}</el-tag>
          </div>
          <div class="detail-card__meta">
            <span>当前值 {{ alarm.currentValue || '--' }}</span>
            <span>阈值 {{ alarm.thresholdValue || '--' }}</span>
            <span>{{ formatDateTime(alarm.triggerTime) }}</span>
          </div>
        </article>
      </div>
      <div v-else class="detail-empty">暂无最近告警</div>
    </section>

    <section class="detail-panel">
      <h3>最近事件</h3>
      <div v-if="recentEvents.length" class="detail-card-list">
        <article v-for="event in recentEvents" :key="event.id" class="detail-card">
          <div class="detail-card__header">
            <strong>{{ event.eventTitle || event.eventCode || `事件 ${event.id}` }}</strong>
            <el-tag :type="riskLevelTagType(event.riskLevel)" round>{{ riskLevelText(event.riskLevel) }}</el-tag>
          </div>
          <div class="detail-card__meta">
            <span>当前值 {{ event.currentValue || '--' }}</span>
            <span>状态 {{ eventStatusText(event.status) }}</span>
            <span>{{ formatDateTime(event.triggerTime) }}</span>
          </div>
        </article>
      </div>
      <div v-else class="detail-empty">暂无最近事件</div>
    </section>
  </StandardDetailDrawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage } from '@/utils/message';

import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue';
import {
  getRiskMonitoringDetail,
  type RiskMonitoringAlarmSummary,
  type RiskMonitoringDetail,
  type RiskMonitoringEventSummary
} from '../api/riskMonitoring';
import { formatDateTime } from '../utils/format';

const props = defineProps<{
  modelValue: boolean;
  bindingId: number | null;
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
}>();

const loading = ref(false);
const errorMessage = ref('');
const detail = ref<RiskMonitoringDetail | null>(null);

const recentAlarms = computed<RiskMonitoringAlarmSummary[]>(() => detail.value?.recentAlarms ?? []);
const recentEvents = computed<RiskMonitoringEventSummary[]>(() => detail.value?.recentEvents ?? []);
const drawerTags = computed(() => {
  if (!detail.value) {
    return [];
  }
  return [
    { label: riskLevelText(detail.value.riskLevel), type: riskLevelTagType(detail.value.riskLevel) },
    { label: monitorStatusText(detail.value.monitorStatus), type: monitorStatusTagType(detail.value.monitorStatus) },
    { label: detail.value.onlineStatus === 1 ? '在线' : '离线', type: detail.value.onlineStatus === 1 ? 'success' : 'info' as const }
  ];
});

watch(
  () => [props.modelValue, props.bindingId] as const,
  async ([visible, bindingId]) => {
    if (!visible || !bindingId) return;
    await loadDetail(bindingId);
  },
  { immediate: true }
);

async function loadDetail(bindingId: number) {
  loading.value = true;
  errorMessage.value = '';
  try {
    const response = await getRiskMonitoringDetail(bindingId);
    detail.value = response.data;
  } catch (error) {
    detail.value = null;
    errorMessage.value = error instanceof Error ? error.message : '加载详情失败';
    ElMessage.error(errorMessage.value);
  } finally {
    loading.value = false;
  }
}

function riskLevelText(value?: string | null) {
  switch ((value || '').toUpperCase()) {
    case 'CRITICAL':
      return '严重';
    case 'WARNING':
    case 'MEDIUM':
      return '警告';
    case 'INFO':
    case 'LOW':
      return '提醒';
    default:
      return value || '未标注';
  }
}

function riskLevelTagType(value?: string | null): 'danger' | 'warning' | 'success' | 'info' {
  switch ((value || '').toUpperCase()) {
    case 'CRITICAL':
      return 'danger';
    case 'WARNING':
    case 'MEDIUM':
      return 'warning';
    case 'INFO':
    case 'LOW':
      return 'success';
    default:
      return 'info';
  }
}

function monitorStatusText(value?: string | null) {
  switch ((value || '').toUpperCase()) {
    case 'ALARM':
      return '告警中';
    case 'OFFLINE':
      return '离线';
    case 'NO_DATA':
      return '无数据';
    case 'NORMAL':
      return '正常';
    default:
      return value || '未识别';
  }
}

function monitorStatusTagType(value?: string | null): 'danger' | 'warning' | 'success' | 'info' {
  switch ((value || '').toUpperCase()) {
    case 'ALARM':
      return 'danger';
    case 'OFFLINE':
    case 'NO_DATA':
      return 'warning';
    case 'NORMAL':
      return 'success';
    default:
      return 'info';
  }
}

function eventStatusText(status?: number | null) {
  switch (status) {
    case 0:
      return '待处置';
    case 1:
      return '处理中';
    case 2:
      return '已完成';
    case 3:
      return '已关闭';
    default:
      return status === null || status === undefined ? '--' : String(status);
  }
}

function formatCurrentValue(value?: string | null, unit?: string | null) {
  if (!value) return '--';
  return unit ? `${value} ${unit}` : value;
}

function formatCoordinate(longitude?: number | null, latitude?: number | null) {
  if (longitude === null || longitude === undefined || latitude === null || latitude === undefined) {
    return '--';
  }
  return `${longitude.toFixed(6)}, ${latitude.toFixed(6)}`;
}
</script>
