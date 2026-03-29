<template>
  <StandardDetailDrawer
    :model-value="modelValue"
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
    <section class="detail-panel detail-panel--hero">
      <div class="detail-section-header">
        <div>
          <h3>监测概览</h3>
          <p>将监测状态、在线情况、当前读数与风险态势聚合到顶部，适配实时巡检和态势研判场景。</p>
        </div>
      </div>
      <div class="detail-summary-grid">
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">监测状态</span>
          <strong class="detail-summary-card__value">{{ monitorStatusText(detail?.monitorStatus) }}</strong>
          <p class="detail-summary-card__hint">风险等级：{{ riskLevelText(detail?.riskLevel) }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">在线状态</span>
          <strong class="detail-summary-card__value">{{ onlineStatusText(detail?.onlineStatus) }}</strong>
          <p class="detail-summary-card__hint">最新上报：{{ formatDateTime(detail?.latestReportTime) }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">当前读数</span>
          <strong class="detail-summary-card__value">{{ formatCurrentValue(detail?.currentValue, detail?.unit) }}</strong>
          <p class="detail-summary-card__hint">测点：{{ detail?.metricName || detail?.metricIdentifier || '--' }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">活跃告警</span>
          <strong class="detail-summary-card__value">{{ detail?.activeAlarmCount ?? 0 }}</strong>
          <p class="detail-summary-card__hint">近期事件：{{ detail?.recentEventCount ?? 0 }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">监测对象</span>
          <strong class="detail-summary-card__value">{{ detail?.riskPointName || detail?.deviceName || '--' }}</strong>
          <p class="detail-summary-card__hint">区域：{{ detail?.regionName || '--' }}</p>
        </article>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>监测对象</h3>
          <p>统一展示设备、风险点、测点和地理位置，帮助快速确认当前监测绑定关系。</p>
        </div>
      </div>
      <div class="detail-grid">
        <div class="detail-field"><span class="detail-field__label">绑定编号</span><strong class="detail-field__value">{{ detail?.bindingId || '--' }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">设备编码</span><strong class="detail-field__value">{{ detail?.deviceCode || '--' }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">设备名称</span><strong class="detail-field__value">{{ detail?.deviceName || '--' }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">产品名称</span><strong class="detail-field__value">{{ detail?.productName || '--' }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">区域</span><strong class="detail-field__value">{{ detail?.regionName || '--' }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">风险点</span><strong class="detail-field__value">{{ detail?.riskPointName || '--' }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">风险点编码</span><strong class="detail-field__value">{{ detail?.riskPointCode || '--' }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">测点</span><strong class="detail-field__value">{{ detail?.metricName || detail?.metricIdentifier || '--' }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">值类型</span><strong class="detail-field__value">{{ detail?.valueType || '--' }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">经纬度</span><strong class="detail-field__value">{{ formatCoordinate(detail?.longitude, detail?.latitude) }}</strong></div>
        <div class="detail-field detail-field--full"><span class="detail-field__label">位置描述</span><strong class="detail-field__value">{{ detail?.address || '--' }}</strong></div>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>态势指标</h3>
          <p>集中展示当前读数、最近上报和近端告警/事件数量，方便进行实时风险判断。</p>
        </div>
      </div>
      <div class="detail-grid">
        <div class="detail-field"><span class="detail-field__label">当前值</span><strong class="detail-field__value">{{ formatCurrentValue(detail?.currentValue, detail?.unit) }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">最新上报</span><strong class="detail-field__value">{{ formatDateTime(detail?.latestReportTime) }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">监测状态</span><strong class="detail-field__value">{{ monitorStatusText(detail?.monitorStatus) }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">在线状态</span><strong class="detail-field__value">{{ onlineStatusText(detail?.onlineStatus) }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">活跃告警</span><strong class="detail-field__value">{{ detail?.activeAlarmCount ?? 0 }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">近期事件</span><strong class="detail-field__value">{{ detail?.recentEventCount ?? 0 }}</strong></div>
        <div class="detail-field"><span class="detail-field__label">趋势点数</span><strong class="detail-field__value">{{ detail?.trendPoints?.length ?? 0 }}</strong></div>
      </div>
      <div :class="['detail-notice', { 'detail-notice--danger': detail?.monitorStatus === 'ALARM' || detail?.onlineStatus !== 1 }]">
        <span class="detail-notice__label">监测建议</span>
        <strong class="detail-notice__value">{{ monitorAdvice }}</strong>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>最近告警</h3>
          <p>展示最新关联告警，便于快速查看当前风险点近期是否持续触发异常。</p>
        </div>
      </div>
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
      <div class="detail-section-header">
        <div>
          <h3>最近事件</h3>
          <p>展示最近处置事件，帮助快速了解该监测对象在近期的处置活跃度与闭环情况。</p>
        </div>
      </div>
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
const monitorAdvice = computed(() => {
  if (!detail.value) {
    return '暂无监测建议';
  }
  if (detail.value.onlineStatus !== 1) {
    return '当前设备离线，建议先恢复链路或确认网关在线状态，再继续判断风险点实时数据。';
  }
  if ((detail.value.monitorStatus || '').toUpperCase() === 'ALARM') {
    return '当前监测对象处于告警中，建议优先查看最近告警与事件，确认是否需要立即处置。';
  }
  if ((detail.value.monitorStatus || '').toUpperCase() === 'NO_DATA') {
    return '当前监测对象暂无有效数据，建议核查采集链路、测点配置与最近上报时间。';
  }
  return '当前监测对象状态稳定，可继续结合最近告警与事件评估风险变化趋势。';
});
const drawerTags = computed(() => {
  if (!detail.value) {
    return [];
  }
  return [
    { label: riskLevelText(detail.value.riskLevel), type: riskLevelTagType(detail.value.riskLevel) },
    { label: monitorStatusText(detail.value.monitorStatus), type: monitorStatusTagType(detail.value.monitorStatus) },
    { label: onlineStatusText(detail.value.onlineStatus), type: detail.value.onlineStatus === 1 ? 'success' : 'info' as const }
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

function onlineStatusText(value?: number | null) {
  return value === 1 ? '在线' : '离线';
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
