<template>
  <div class="message-trace-detail-workbench">
    <section class="message-trace-detail-workbench__stage" data-testid="message-trace-detail-summary-stage">
      <div class="message-trace-detail-workbench__stage-header">
        <h3>消息态势与处理概况</h3>
      </div>

      <div class="message-trace-detail-workbench__summary-grid">
        <article
          v-for="card in summaryCards"
          :key="card.key"
          class="message-trace-detail-workbench__summary-card"
        >
          <span class="message-trace-detail-workbench__summary-label">{{ card.label }}</span>
          <span class="message-trace-detail-workbench__summary-value">{{ card.value }}</span>
          <span v-if="card.hint" class="message-trace-detail-workbench__summary-hint">{{ card.hint }}</span>
        </article>
      </div>
    </section>

    <section
      class="message-trace-detail-workbench__stage message-trace-detail-workbench__stage--subtle"
      data-testid="message-trace-detail-ledger-stage"
    >
      <div class="message-trace-detail-workbench__stage-header">
        <h3>链路与接入台账</h3>
      </div>

      <div class="message-trace-detail-workbench__ledger-stack">
        <article class="message-trace-detail-workbench__overview-panel">
          <div class="message-trace-detail-workbench__stage-header">
            <h3>链路概览</h3>
          </div>

          <div class="message-trace-detail-workbench__ledger-grid">
            <article
              v-for="item in chainOverviewItems"
              :key="item.key"
              :class="[
                'message-trace-detail-workbench__ledger-item',
                { 'message-trace-detail-workbench__ledger-item--wide': item.wide }
              ]"
            >
              <span class="message-trace-detail-workbench__ledger-label">{{ item.label }}</span>
              <span
                :class="[
                  'message-trace-detail-workbench__ledger-value',
                  { 'message-trace-detail-workbench__ledger-value--multiline': item.multiline }
                ]"
              >
                {{ item.value }}
              </span>
            </article>
          </div>
        </article>

        <article class="message-trace-detail-workbench__overview-panel">
          <div class="message-trace-detail-workbench__stage-header">
            <h3>接入概览</h3>
          </div>

          <div class="message-trace-detail-workbench__ledger-grid">
            <article
              v-for="item in accessOverviewItems"
              :key="item.key"
              :class="[
                'message-trace-detail-workbench__ledger-item',
                { 'message-trace-detail-workbench__ledger-item--wide': item.wide }
              ]"
            >
              <span class="message-trace-detail-workbench__ledger-label">{{ item.label }}</span>
              <span
                :class="[
                  'message-trace-detail-workbench__ledger-value',
                  { 'message-trace-detail-workbench__ledger-value--multiline': item.multiline }
                ]"
              >
                {{ item.value }}
              </span>
            </article>
          </div>
        </article>
      </div>
    </section>

    <section
      v-if="showTemplateEvidenceStage"
      class="message-trace-detail-workbench__stage message-trace-detail-workbench__stage--subtle"
      data-testid="message-trace-detail-template-stage"
    >
      <div class="message-trace-detail-workbench__stage-header">
        <h3>协议模板证据</h3>
      </div>

      <article v-if="templateSummaryItems.length > 0" class="message-trace-detail-workbench__subsection">
        <header class="message-trace-detail-workbench__subsection-header">
          <strong>模板摘要</strong>
        </header>

        <div class="message-trace-detail-workbench__ledger-grid">
          <article
            v-for="item in templateSummaryItems"
            :key="item.key"
            :class="[
              'message-trace-detail-workbench__ledger-item',
              { 'message-trace-detail-workbench__ledger-item--wide': item.wide }
            ]"
          >
            <span class="message-trace-detail-workbench__ledger-label">{{ item.label }}</span>
            <span
              :class="[
                'message-trace-detail-workbench__ledger-value',
                { 'message-trace-detail-workbench__ledger-value--multiline': item.multiline }
              ]"
            >
              {{ item.value }}
            </span>
          </article>
        </div>
      </article>

      <article v-if="templateExecutionItems.length > 0" class="message-trace-detail-workbench__subsection">
        <header class="message-trace-detail-workbench__subsection-header">
          <strong>模板执行</strong>
        </header>

        <div class="message-trace-detail-workbench__ledger-grid">
          <article
            v-for="item in templateExecutionItems"
            :key="item.key"
            class="message-trace-detail-workbench__ledger-item message-trace-detail-workbench__ledger-item--wide"
          >
            <span class="message-trace-detail-workbench__ledger-label">{{ item.label }}</span>
            <span
              :class="[
                'message-trace-detail-workbench__ledger-value',
                { 'message-trace-detail-workbench__ledger-value--multiline': item.multiline }
              ]"
            >
              {{ item.value }}
            </span>
          </article>
        </div>
      </article>
    </section>

    <section class="message-trace-detail-workbench__stage" data-testid="message-trace-detail-payload-stage">
      <div class="message-trace-detail-workbench__stage-header">
        <h3>Payload 对照</h3>
      </div>

      <MessageTracePayloadComparisonSection :panels="panels" />
    </section>

    <section class="message-trace-detail-workbench__stage" data-testid="message-trace-detail-timeline-stage">
      <div class="message-trace-detail-workbench__stage-header message-trace-detail-workbench__stage-header--between">
        <h3>处理时间线</h3>
        <button
          type="button"
          class="message-trace-detail-workbench__toggle"
          data-testid="message-trace-timeline-toggle"
          @click="toggleTimeline"
        >
          {{ timelineExpanded ? '收起' : '展开' }}
        </button>
      </div>

      <div class="message-trace-detail-workbench__timeline-summary-grid">
        <article
          v-for="card in timelineSummaryCards"
          :key="card.key"
          :class="[
            'message-trace-detail-workbench__timeline-card',
            `message-trace-detail-workbench__timeline-card--${card.tone ?? 'plain'}`,
            { 'message-trace-detail-workbench__timeline-card--wide': card.wide }
          ]"
        >
          <span class="message-trace-detail-workbench__timeline-card-label">{{ card.label }}</span>
          <strong class="message-trace-detail-workbench__timeline-card-value">{{ card.value }}</strong>
          <p v-if="card.hint" class="message-trace-detail-workbench__timeline-card-hint">
            {{ card.hint }}
          </p>
        </article>
      </div>

      <p
        v-if="timelineSummaryDescription"
        class="message-trace-detail-workbench__timeline-note"
        :class="`message-trace-detail-workbench__timeline-note--${timelineSummaryTone}`"
      >
        {{ timelineSummaryDescription }}
      </p>

      <div v-if="timelineExpanded" class="message-trace-detail-workbench__timeline-body">
        <StandardTraceTimeline
          :timeline="timeline"
          :loading="timelineLoading"
          :empty-title="timelineEmptyTitle"
          :empty-description="timelineEmptyDescription"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import MessageTracePayloadComparisonSection from '@/components/messageTrace/MessageTracePayloadComparisonSection.vue';
import StandardTraceTimeline from '@/components/StandardTraceTimeline.vue';
import type { MessageFlowTimeline, MessageTraceDetail } from '@/types/api';
import type { MessageTracePayloadComparisonPanel } from '@/utils/messageTracePayloadComparison';
import { formatDateTime, formatMessageTraceReportTime } from '@/utils/format';

interface LedgerItem {
  key: string;
  label: string;
  value: string;
  wide?: boolean;
  multiline?: boolean;
}

interface SummaryCard {
  key: string;
  label: string;
  value: string;
  hint?: string;
}

interface TimelineSummaryCard {
  key: string;
  label: string;
  value: string;
  hint?: string;
  tone?: 'plain' | 'ready' | 'warning' | 'muted' | 'loading';
  wide?: boolean;
}

const props = defineProps<{
  detail: Partial<MessageTraceDetail>;
  panels: MessageTracePayloadComparisonPanel[];
  timeline: MessageFlowTimeline | null;
  timelineLoading: boolean;
  timelineExpired: boolean;
  timelineLookupError: boolean;
  timelineEmptyTitle: string;
  timelineEmptyDescription: string;
}>();

const timelineExpanded = ref(false);

watch(
  () => [props.detail.id, props.detail.traceId, props.detail.createTime, props.timeline?.traceId],
  () => {
    timelineExpanded.value = false;
  },
  { immediate: true }
);

const messageTypeLabel = computed(() => getMessageTypeLabel(props.detail.messageType));
const topicSegments = computed(() => {
  if (!props.detail.topic) {
    return '--';
  }
  return String(props.detail.topic).split('/').filter(Boolean).length.toString();
});
const timelineStepCount = computed(() => props.timeline?.steps?.length ?? 0);
const timelineStatusValue = computed(() => {
  if (props.timelineLookupError) {
    return '存储异常';
  }
  if (props.timelineExpired) {
    return '已过期';
  }
  if (props.timelineLoading) {
    return '读取中';
  }
  if (timelineStepCount.value > 0) {
    return '可展开复盘';
  }
  if (props.detail.traceId) {
    return '等待生成';
  }
  return '暂无 trace';
});
const timelineStatusHint = computed(() => {
  if (props.timelineLookupError) {
    return '优先排查 Redis / message-flow';
  }
  if (props.timelineExpired) {
    return '时间线已过期，Payload 仍可继续复盘';
  }
  if (props.timelineLoading) {
    return '后台正在补数';
  }
  if (timelineStepCount.value > 0) {
    return '可继续展开完整时间线';
  }
  if (props.detail.traceId) {
    return '当前 trace 还没有可用时间线';
  }
  return '当前记录未携带 traceId';
});
const timelineNodeValue = computed(() => {
  if (props.timelineLookupError || props.timelineExpired || props.timelineLoading) {
    return '--';
  }
  if (timelineStepCount.value > 0) {
    return `${timelineStepCount.value} 个节点`;
  }
  if (props.detail.traceId) {
    return '0 个节点';
  }
  return '--';
});
const timelineNodeHint = computed(() => {
  if (props.timelineLookupError) {
    return '当前无法读取节点数量';
  }
  if (props.timelineExpired) {
    return '节点明细已过期';
  }
  if (props.timelineLoading) {
    return '正在同步节点数量';
  }
  if (timelineStepCount.value > 0) {
    return '展开后查看每个节点耗时';
  }
  if (props.detail.traceId) {
    return '当前 trace 尚未归档节点';
  }
  return '暂无节点信息';
});
const timelineOwnerValue = computed(() => {
  if (props.timeline?.sessionId) {
    return props.timeline.sessionId;
  }
  if (props.timeline?.traceId) {
    return props.timeline.traceId;
  }
  if (props.detail.traceId) {
    return formatValue(props.detail.traceId);
  }
  return '--';
});
const timelineOwnerHint = computed(() => {
  if (props.timeline?.sessionId) {
    return '当前处理会话';
  }
  if (props.timeline?.traceId || props.detail.traceId) {
    return '当前 Trace 标识';
  }
  return '暂无归属信息';
});
const timelineStorageValue = computed(() => {
  if (props.timelineLookupError) {
    return 'message-flow / Redis 读取异常';
  }
  if (props.timelineExpired) {
    return '时间线已过期';
  }
  if (props.timelineLoading) {
    return 'Redis 时间线补数中';
  }
  if (timelineStepCount.value > 0) {
    return '完整时间线可用';
  }
  if (props.detail.traceId) {
    return '等待时间线入库';
  }
  return '当前记录未携带 traceId';
});
const timelineStorageHint = computed(() => {
  if (timelineStepCount.value > 0) {
    return '展开后查看完整处理轨迹';
  }
  if (props.timelineLoading) {
    return '后台正在补数，读取完成后可在此展开完整时间线';
  }
  if (props.timelineLookupError || props.timelineExpired || props.detail.traceId) {
    return props.timelineEmptyDescription;
  }
  return '当前记录没有可复盘链路';
});

const summaryCards = computed<SummaryCard[]>(() => [
  {
    key: 'messageType',
    label: '消息类型',
    value: messageTypeLabel.value,
    hint: '确认当前上报语义'
  },
  {
    key: 'reportTime',
    label: '上报时间',
    value: formatMessageTraceReportTime(props.detail.reportTime, props.detail.createTime),
    hint: '按中国时间口径展示'
  },
  {
    key: 'topicSegments',
    label: 'Topic 节点',
    value: topicSegments.value,
    hint: '帮助判断接入路径深度'
  },
  {
    key: 'timelineStatus',
    label: '时间线状态',
    value: timelineStatusValue.value,
    hint: timelineStatusHint.value
  }
]);

const chainOverviewItems = computed<LedgerItem[]>(() => [
  { key: 'traceId', label: 'TraceId', value: formatValue(props.detail.traceId), wide: true },
  { key: 'id', label: '日志 ID', value: formatValue(props.detail.id) },
  { key: 'createTime', label: '创建时间', value: formatDateTime(props.detail.createTime) },
  { key: 'reportTime', label: '上报时间', value: formatMessageTraceReportTime(props.detail.reportTime, props.detail.createTime), wide: true }
]);

const accessOverviewItems = computed<LedgerItem[]>(() => [
  { key: 'deviceCode', label: '设备编码', value: formatValue(props.detail.deviceCode) },
  {
    key: 'routeType',
    label: '路由类型',
    value: formatValue(props.detail.protocolMetadata?.routeType)
  },
  { key: 'productKey', label: '产品标识', value: formatValue(props.detail.productKey), wide: true },
  { key: 'topic', label: 'Topic', value: formatValue(props.detail.topic), wide: true }
]);

const templateEvidence = computed(() => props.detail.protocolMetadata?.templateEvidence);
const templateCodes = computed(() => templateEvidence.value?.templateCodes?.filter((code): code is string => Boolean(code)) ?? []);
const templateExecutions = computed(() => templateEvidence.value?.executions ?? []);
const showTemplateEvidenceStage = computed(() => templateCodes.value.length > 0 || templateExecutions.value.length > 0);

const templateSummaryItems = computed<LedgerItem[]>(() => {
  const items: LedgerItem[] = [];
  if (templateCodes.value.length > 0) {
    items.push({
      key: 'templateCodes',
      label: '模板编码',
      value: templateCodes.value.join(' / '),
      wide: true
    });
  }
  if (props.detail.protocolMetadata?.normalizationStrategy) {
    items.push({
      key: 'normalizationStrategy',
      label: '归一策略',
      value: formatValue(props.detail.protocolMetadata.normalizationStrategy)
    });
  }
  if (props.detail.protocolMetadata?.childSplitApplied !== undefined && props.detail.protocolMetadata?.childSplitApplied !== null) {
    items.push({
      key: 'childSplitApplied',
      label: '子拆分',
      value: formatBoolean(props.detail.protocolMetadata.childSplitApplied)
    });
  }
  items.push({
    key: 'templateExecutionCount',
    label: '执行条目',
    value: String(templateExecutions.value.length)
  });
  return items;
});

const templateExecutionItems = computed<LedgerItem[]>(() =>
  templateExecutions.value.map((execution, index) => ({
    key: `template-execution-${index}`,
    label: formatTemplateExecutionLabel(execution.logicalChannelCode, execution.childDeviceCode),
    value: formatTemplateExecutionValue(execution),
    wide: true,
    multiline: true
  }))
);

const timelineSummaryTone = computed(() => {
  if (props.timelineLookupError) {
    return 'warning';
  }
  if (props.timelineExpired) {
    return 'muted';
  }
  if (props.timelineLoading) {
    return 'loading';
  }
  if (timelineStepCount.value > 0) {
    return 'ready';
  }
  return 'plain';
});

const timelineSummaryDescription = computed(() => {
  if (props.timelineLoading) {
    return '后台正在补数，读取完成后可在此展开完整时间线。';
  }
  if (
    props.timelineLookupError ||
    props.timelineExpired ||
    (timelineStepCount.value === 0 && Boolean(props.detail.traceId))
  ) {
    return props.timelineEmptyDescription;
  }
  return '';
});
const timelineSummaryCards = computed<TimelineSummaryCard[]>(() => [
  {
    key: 'status',
    label: '当前状态',
    value: timelineStatusValue.value,
    hint: timelineStatusHint.value,
    tone: timelineSummaryTone.value
  },
  {
    key: 'steps',
    label: '处理节点',
    value: timelineNodeValue.value,
    hint: timelineNodeHint.value
  },
  {
    key: 'owner',
    label: 'Trace 归属',
    value: timelineOwnerValue.value,
    hint: timelineOwnerHint.value,
    wide: !props.timeline?.sessionId && timelineOwnerValue.value !== '--'
  },
  {
    key: 'storage',
    label: '存储提示',
    value: timelineStorageValue.value,
    hint: timelineStorageHint.value
  }
]);

function toggleTimeline() {
  timelineExpanded.value = !timelineExpanded.value;
}

function formatValue(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '--';
  }
  return String(value);
}

function formatBoolean(value?: boolean | null) {
  if (value === undefined || value === null) {
    return '--';
  }
  return value ? '已应用' : '未应用';
}

function formatTemplateExecutionLabel(logicalChannelCode?: string | null, childDeviceCode?: string | null) {
  const logicalCode = formatValue(logicalChannelCode);
  const childCode = formatValue(childDeviceCode);
  return `${logicalCode} -> ${childCode}`;
}

function formatTemplateExecutionValue(execution: {
  templateCode?: string | null;
  canonicalizationStrategy?: string | null;
  statusMirrorApplied?: boolean | null;
  parentRemovalKeys?: string[] | null;
}) {
  const segments = [
    `模板编码：${formatValue(execution.templateCode)}`,
    execution.canonicalizationStrategy ? `归一策略：${execution.canonicalizationStrategy}` : '',
    execution.statusMirrorApplied === null || execution.statusMirrorApplied === undefined
      ? ''
      : `状态镜像：${formatBoolean(execution.statusMirrorApplied)}`,
    execution.parentRemovalKeys && execution.parentRemovalKeys.length > 0
      ? `父字段清理：${execution.parentRemovalKeys.join(' / ')}`
      : ''
  ].filter(Boolean);
  return segments.join('\n');
}

function getMessageTypeLabel(value?: string | null) {
  switch (value) {
    case 'report':
      return '属性上报';
    case 'event':
      return '事件上报';
    case 'status':
      return '状态上报';
    case 'reply':
      return '命令回执';
    case 'service':
      return '服务调用';
    case 'online':
      return '上线消息';
    case 'offline':
      return '离线消息';
    case 'property':
      return '属性上报';
    default:
      return formatValue(value);
  }
}
</script>

<style scoped>
.message-trace-detail-workbench {
  display: grid;
  gap: 1rem;
}

.message-trace-detail-workbench__stage,
.message-trace-detail-workbench__subsection,
.message-trace-detail-workbench__overview-panel {
  display: grid;
  gap: 0.9rem;
  padding: 1rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-inset-highlight-78);
}

.message-trace-detail-workbench__stage--subtle,
.message-trace-detail-workbench__subsection,
.message-trace-detail-workbench__overview-panel {
  background: rgba(255, 255, 255, 0.9);
}

.message-trace-detail-workbench__stage-header,
.message-trace-detail-workbench__stage-header--between {
  display: flex;
  align-items: center;
}

.message-trace-detail-workbench__stage-header {
  justify-content: flex-start;
}

.message-trace-detail-workbench__stage-header--between {
  justify-content: space-between;
  gap: 12px;
}

.message-trace-detail-workbench__stage-header h3,
.message-trace-detail-workbench__subsection-header strong {
  margin: 0;
  color: var(--text-heading);
  font-size: 16px;
  line-height: 1.4;
}

.message-trace-detail-workbench__summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
}

.message-trace-detail-workbench__summary-card {
  display: grid;
  gap: 0.38rem;
  min-width: 0;
  padding: 1rem 1.05rem;
  border: 1px solid rgba(203, 213, 225, 0.86);
  border-radius: calc(var(--radius-md) + 2px);
  background:
    linear-gradient(180deg, rgba(248, 251, 255, 0.98) 0%, rgba(244, 248, 255, 0.94) 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.message-trace-detail-workbench__summary-label,
.message-trace-detail-workbench__ledger-label,
.message-trace-detail-workbench__timeline-card-label {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.45;
}

.message-trace-detail-workbench__summary-value {
  color: var(--text-heading);
  font-size: 15px;
  font-weight: 500;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.message-trace-detail-workbench__summary-hint {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.5;
}

.message-trace-detail-workbench__ledger-stack {
  display: grid;
  gap: 0.9rem;
}

.message-trace-detail-workbench__ledger-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem 1rem;
}

.message-trace-detail-workbench__ledger-item {
  display: grid;
  gap: 0.34rem;
  min-width: 0;
  padding: 0.92rem 1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(248, 251, 255, 0.92);
}

.message-trace-detail-workbench__ledger-item--wide {
  grid-column: 1 / -1;
}

.message-trace-detail-workbench__ledger-value {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 400;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.message-trace-detail-workbench__ledger-value--multiline {
  white-space: pre-wrap;
}

.message-trace-detail-workbench__toggle {
  min-width: 72px;
  height: 34px;
  padding: 0 14px;
  border: 1px solid var(--panel-border);
  border-radius: 999px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast),
    background var(--transition-fast);
}

.message-trace-detail-workbench__toggle:hover {
  color: var(--brand);
  border-color: color-mix(in srgb, var(--brand) 22%, var(--panel-border));
  background: color-mix(in srgb, var(--brand) 6%, white);
}

.message-trace-detail-workbench__timeline-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
}

.message-trace-detail-workbench__timeline-card {
  display: grid;
  gap: 0.4rem;
  min-width: 0;
  padding: 1rem 1.05rem;
  border-radius: calc(var(--radius-md) + 4px);
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  background: rgba(248, 251, 255, 0.92);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.message-trace-detail-workbench__timeline-card--wide {
  grid-column: 1 / -1;
}

.message-trace-detail-workbench__timeline-card--ready {
  border-color: color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  background: color-mix(in srgb, var(--brand) 5%, var(--surface-soft));
}

.message-trace-detail-workbench__timeline-card--warning {
  border-color: color-mix(in srgb, var(--warning) 22%, var(--panel-border));
  background: color-mix(in srgb, var(--warning-bg) 42%, white);
}

.message-trace-detail-workbench__timeline-card--muted {
  background: var(--surface-muted);
}

.message-trace-detail-workbench__timeline-card--loading {
  border-color: color-mix(in srgb, var(--accent) 16%, var(--panel-border));
  background: color-mix(in srgb, var(--info-bg) 42%, white);
}

.message-trace-detail-workbench__timeline-card-value {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.message-trace-detail-workbench__timeline-card-hint {
  margin: 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.7;
}

.message-trace-detail-workbench__timeline-note {
  margin: 0;
  padding: 0.95rem 1rem;
  border: 1px dashed var(--panel-border);
  border-radius: calc(var(--radius-md) + 2px);
  background: var(--surface-soft);
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.message-trace-detail-workbench__timeline-note--ready {
  border-color: color-mix(in srgb, var(--brand) 12%, var(--panel-border));
}

.message-trace-detail-workbench__timeline-note--warning {
  border-color: color-mix(in srgb, var(--warning) 22%, var(--panel-border));
  background: color-mix(in srgb, var(--warning-bg) 36%, white);
}

.message-trace-detail-workbench__timeline-note--muted {
  background: var(--surface-muted);
}

.message-trace-detail-workbench__timeline-note--loading {
  border-color: color-mix(in srgb, var(--accent) 16%, var(--panel-border));
  background: color-mix(in srgb, var(--info-bg) 32%, white);
}

@media (max-width: 960px) {
  .message-trace-detail-workbench__summary-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .message-trace-detail-workbench__ledger-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .message-trace-detail-workbench__timeline-summary-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .message-trace-detail-workbench__ledger-item--wide {
    grid-column: auto;
  }

  .message-trace-detail-workbench__timeline-card--wide {
    grid-column: auto;
  }
}

@media (max-width: 720px) {
  .message-trace-detail-workbench__stage-header--between {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 520px) {
  .message-trace-detail-workbench__summary-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
