<template>
  <el-drawer
    :model-value="modelValue"
    size="48rem"
    direction="rtl"
    destroy-on-close
    @close="emit('update:modelValue', false)"
  >
    <template #header>
      <div class="drawer-header">
        <div>
          <p class="drawer-eyebrow">Risk Monitoring Detail</p>
          <h2>{{ detail?.riskPointName || detail?.deviceName || '监测详情' }}</h2>
        </div>
        <div class="drawer-statuses" v-if="detail">
          <el-tag :type="riskLevelTagType(detail.riskLevel)">{{ riskLevelText(detail.riskLevel) }}</el-tag>
          <el-tag :type="monitorStatusTagType(detail.monitorStatus)">{{ monitorStatusText(detail.monitorStatus) }}</el-tag>
          <el-tag :type="detail.onlineStatus === 1 ? 'success' : 'info'">
            {{ detail.onlineStatus === 1 ? '在线' : '离线' }}
          </el-tag>
        </div>
      </div>
    </template>

    <div class="drawer-body">
      <div v-if="loading" class="drawer-state">正在加载监测详情...</div>
      <div v-else-if="errorMessage" class="drawer-state drawer-state--error">{{ errorMessage }}</div>
      <div v-else-if="!detail" class="drawer-state">暂无详情数据</div>
      <template v-else>
        <section class="panel">
          <h3>当前监测信息</h3>
          <div class="snapshot-grid">
            <div class="snapshot-item"><span>设备编码</span><strong>{{ detail.deviceCode || '--' }}</strong></div>
            <div class="snapshot-item"><span>设备名称</span><strong>{{ detail.deviceName || '--' }}</strong></div>
            <div class="snapshot-item"><span>产品名称</span><strong>{{ detail.productName || '--' }}</strong></div>
            <div class="snapshot-item"><span>区域</span><strong>{{ detail.regionName || '--' }}</strong></div>
            <div class="snapshot-item"><span>风险点</span><strong>{{ detail.riskPointName || '--' }}</strong></div>
            <div class="snapshot-item"><span>测点</span><strong>{{ detail.metricName || detail.metricIdentifier || '--' }}</strong></div>
            <div class="snapshot-item"><span>当前值</span><strong>{{ formatCurrentValue(detail.currentValue, detail.unit) }}</strong></div>
            <div class="snapshot-item"><span>最新上报</span><strong>{{ formatDateTime(detail.latestReportTime) }}</strong></div>
            <div class="snapshot-item"><span>活跃告警</span><strong>{{ detail.activeAlarmCount ?? 0 }}</strong></div>
            <div class="snapshot-item"><span>近期事件</span><strong>{{ detail.recentEventCount ?? 0 }}</strong></div>
            <div class="snapshot-item"><span>经纬度</span><strong>{{ formatCoordinate(detail.longitude, detail.latitude) }}</strong></div>
            <div class="snapshot-item"><span>位置描述</span><strong>{{ detail.address || '--' }}</strong></div>
          </div>
        </section>

        <section class="panel">
          <h3>最近告警</h3>
          <div v-if="recentAlarms.length" class="summary-list">
            <article v-for="alarm in recentAlarms" :key="alarm.id" class="summary-card">
              <div class="summary-card__header">
                <strong>{{ alarm.alarmTitle || alarm.alarmCode || `告警 ${alarm.id}` }}</strong>
                <el-tag :type="riskLevelTagType(alarm.alarmLevel)">{{ riskLevelText(alarm.alarmLevel) }}</el-tag>
              </div>
              <div class="summary-card__meta">
                <span>当前值 {{ alarm.currentValue || '--' }}</span>
                <span>阈值 {{ alarm.thresholdValue || '--' }}</span>
                <span>{{ formatDateTime(alarm.triggerTime) }}</span>
              </div>
            </article>
          </div>
          <div v-else class="empty-block">暂无最近告警</div>
        </section>

        <section class="panel">
          <h3>最近事件</h3>
          <div v-if="recentEvents.length" class="summary-list">
            <article v-for="event in recentEvents" :key="event.id" class="summary-card">
              <div class="summary-card__header">
                <strong>{{ event.eventTitle || event.eventCode || `事件 ${event.id}` }}</strong>
                <el-tag :type="riskLevelTagType(event.riskLevel)">{{ riskLevelText(event.riskLevel) }}</el-tag>
              </div>
              <div class="summary-card__meta">
                <span>当前值 {{ event.currentValue || '--' }}</span>
                <span>状态 {{ eventStatusText(event.status) }}</span>
                <span>{{ formatDateTime(event.triggerTime) }}</span>
              </div>
            </article>
          </div>
          <div v-else class="empty-block">暂无最近事件</div>
        </section>
      </template>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage } from '@/utils/message';

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

<style scoped>
.drawer-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.drawer-eyebrow {
  margin: 0;
  color: #6b7a92;
  font-size: 12px;
}

.drawer-header h2 {
  margin: 0.25rem 0 0;
  font-size: 1.2rem;
}

.drawer-statuses {
  display: flex;
  gap: 0.5rem;
}

.drawer-body {
  display: grid;
  gap: 1rem;
}

.panel {
  border: 1px solid #e8edf5;
  border-radius: 10px;
  padding: 0.9rem;
}

.panel h3 {
  margin: 0 0 0.8rem;
}

.snapshot-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.7rem;
}

.snapshot-item span {
  display: block;
  color: #6b7a92;
  font-size: 12px;
}

.snapshot-item strong {
  display: block;
  margin-top: 0.15rem;
}

.summary-list {
  display: grid;
  gap: 0.7rem;
}

.summary-card {
  border: 1px solid #edf1f7;
  border-radius: 8px;
  padding: 0.7rem;
}

.summary-card__header {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
}

.summary-card__meta {
  margin-top: 0.5rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem;
  color: #5f6f88;
  font-size: 12px;
}

.drawer-state,
.empty-block {
  color: #6b7a92;
}

.drawer-state--error {
  color: #d94848;
}
</style>
