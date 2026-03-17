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
    <section class="detail-panel detail-panel--hero">
      <div class="detail-section-header">
        <div>
          <h3>告警概览</h3>
          <p>突出告警等级、当前状态、监测对象与处置进展，值班人员打开抽屉即可快速完成判断。</p>
        </div>
      </div>
      <div class="detail-summary-grid">
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">告警等级</span>
          <strong class="detail-summary-card__value">{{ getAlarmLevelText(detail?.alarmLevel) }}</strong>
          <p class="detail-summary-card__hint">告警类型：{{ detail?.alarmType || '--' }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">当前状态</span>
          <strong class="detail-summary-card__value">{{ getStatusText(detail?.status) }}</strong>
          <p class="detail-summary-card__hint">触发时间：{{ formatDateTime(detail?.triggerTime) }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">监测对象</span>
          <strong class="detail-summary-card__value">{{ detail?.riskPointName || detail?.deviceName || '--' }}</strong>
          <p class="detail-summary-card__hint">区域：{{ detail?.regionName || '--' }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">当前读数</span>
          <strong class="detail-summary-card__value">{{ valueSnapshot }}</strong>
          <p class="detail-summary-card__hint">测点：{{ detail?.metricName || '--' }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">最新处置</span>
          <strong class="detail-summary-card__value">{{ latestActionSummary }}</strong>
          <p class="detail-summary-card__hint">规则：{{ detail?.ruleName || '--' }}</p>
        </article>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>监测上下文</h3>
          <p>统一展示告警来源、设备对象、规则与测点信息，帮助快速还原告警触发背景。</p>
        </div>
      </div>
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
      <div class="detail-section-header">
        <div>
          <h3>处置记录</h3>
          <p>按确认、抑制、关闭三个阶段记录处置责任人和时间节点，便于追溯处理链路。</p>
        </div>
      </div>
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
      <div class="detail-section-header">
        <div>
          <h3>补充信息</h3>
          <p>保留基础审计时间与值班建议，让详情抽屉既能看数据，也能直接指导下一步处置。</p>
        </div>
      </div>
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
      <div :class="['detail-notice', { 'detail-notice--danger': detail?.status === 0 }]">
        <span class="detail-notice__label">处置建议</span>
        <strong class="detail-notice__value">{{ disposalAdvice }}</strong>
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
const valueSnapshot = computed(() => {
  if (!props.detail) {
    return '--';
  }
  if (props.detail.currentValue && props.detail.thresholdValue) {
    return `${props.detail.currentValue} / 阈值 ${props.detail.thresholdValue}`;
  }
  return props.detail.currentValue || props.detail.thresholdValue || '--';
});
const latestActionSummary = computed(() => {
  if (!props.detail) {
    return '--';
  }
  if (props.detail.status === 3) {
    return props.detail.closeUser || formatDateTime(props.detail.closeTime);
  }
  if (props.detail.status === 2) {
    return props.detail.suppressUser || formatDateTime(props.detail.suppressTime);
  }
  if (props.detail.status === 1) {
    return props.detail.confirmUser || formatDateTime(props.detail.confirmTime);
  }
  return '待值班确认';
});
const disposalAdvice = computed(() => {
  if (!props.detail) {
    return '暂无处置建议';
  }
  switch (props.detail.status) {
    case 0:
      return '当前告警尚未确认，建议优先核查现场状态并完成确认或抑制。';
    case 1:
      return '当前告警已确认，建议结合设备状态继续跟踪是否需要抑制或关闭。';
    case 2:
      return '当前告警已抑制，建议确认抑制窗口是否仍然有效，避免遗漏重复风险。';
    case 3:
      return '当前告警已关闭，可回顾备注和处理记录确认是否需要形成复盘结论。';
    default:
      return '建议结合设备、风险点和规则配置继续核查告警来源。';
  }
});
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
