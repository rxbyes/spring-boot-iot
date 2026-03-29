<template>
  <StandardDetailDrawer
    :model-value="modelValue"
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
    <section class="detail-panel detail-panel--hero">
      <div class="detail-section-header">
        <div>
          <h3>事件概览</h3>
          <p>聚合风险等级、处置态势、执行角色和时效要求，让事件详情更符合值班处置场景。</p>
        </div>
      </div>
      <div class="detail-summary-grid">
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">风险等级</span>
          <strong class="detail-summary-card__value">{{ getRiskLevelText(detail?.riskLevel) }}</strong>
          <p class="detail-summary-card__hint">关联告警：{{ detail?.alarmCode || '--' }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">当前状态</span>
          <strong class="detail-summary-card__value">{{ getStatusText(detail?.status) }}</strong>
          <p class="detail-summary-card__hint">发生时间：{{ formatDateTime(detail?.triggerTime) }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">责任人</span>
          <strong class="detail-summary-card__value">{{ detail?.responsibleUser || detail?.dispatchUser || '--' }}</strong>
          <p class="detail-summary-card__hint">紧急程度：{{ detail?.urgencyLevel || '--' }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">处置对象</span>
          <strong class="detail-summary-card__value">{{ detail?.riskPointName || detail?.deviceName || '--' }}</strong>
          <p class="detail-summary-card__hint">区域：{{ detail?.regionName || '--' }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">时效要求</span>
          <strong class="detail-summary-card__value">{{ timeLimitSummary }}</strong>
          <p class="detail-summary-card__hint">最新节点：{{ latestProgressSummary }}</p>
        </article>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>事件上下文</h3>
          <p>统一呈现事件来源、风险对象、设备测点与当前值，方便判断事件影响范围。</p>
        </div>
      </div>
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
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>处置进度</h3>
          <p>以派发、开始、完成、关闭四个时点组织流程信息，保证进度查看更清晰。</p>
        </div>
      </div>
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
      <div class="detail-section-header">
        <div>
          <h3>处置说明</h3>
          <p>将关闭原因、复核说明和补充备注集中展示，避免多段文本分散影响阅读效率。</p>
        </div>
      </div>
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
      <div :class="['detail-notice', { 'detail-notice--danger': detail?.status === 0 || detail?.status === 5 }]">
        <span class="detail-notice__label">处置判断</span>
        <strong class="detail-notice__value">{{ eventAdvice }}</strong>
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
const drawerSubtitle = computed(() => (props.detail?.eventCode ? `事件编号：${props.detail.eventCode}` : '查看事件协同全流程详情'));
const timeLimitSummary = computed(() => {
  if (!props.detail) {
    return '--';
  }
  return `${formatDuration(props.detail.arrivalTimeLimit)} / ${formatDuration(props.detail.completionTimeLimit)}`;
});
const latestProgressSummary = computed(() => {
  if (!props.detail) {
    return '--';
  }
  if (props.detail.closeTime) {
    return `已关闭 ${formatDateTime(props.detail.closeTime)}`;
  }
  if (props.detail.completeTime) {
    return `已完成 ${formatDateTime(props.detail.completeTime)}`;
  }
  if (props.detail.startTime) {
    return `处理中 ${formatDateTime(props.detail.startTime)}`;
  }
  if (props.detail.dispatchTime) {
    return `已派发 ${formatDateTime(props.detail.dispatchTime)}`;
  }
  return `触发 ${formatDateTime(props.detail.triggerTime)}`;
});
const eventAdvice = computed(() => {
  if (!props.detail) {
    return '暂无处置建议';
  }
  switch (props.detail.status) {
    case 0:
      return '事件尚未派发，建议尽快明确执行人并下发工单。';
    case 1:
      return '事件已派发，建议确认执行人是否已接收并按到场时限处理。';
    case 2:
      return '事件处理中，建议持续跟踪处理反馈并准备验收。';
    case 3:
      return '事件待验收，建议结合复核说明确认是否满足关闭条件。';
    case 4:
      return '事件已关闭，可结合关闭原因和复核说明完成归档复盘。';
    case 5:
      return '事件已取消，建议核对取消原因并确认是否需要补充说明。';
    default:
      return '建议结合执行角色、时效要求和复核说明继续跟进处置。';
  }
});
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
