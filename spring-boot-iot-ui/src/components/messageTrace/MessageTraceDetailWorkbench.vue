<template>
  <div class="message-trace-detail-workbench">
    <section class="message-trace-detail-workbench__lead-sheet" data-testid="message-trace-detail-lead-sheet">
      <div class="message-trace-detail-workbench__lead-main">
        <span class="message-trace-detail-workbench__section-kicker">消息概览</span>
        <strong class="message-trace-detail-workbench__lead-value">{{ messageTypeLabel }}</strong>
      </div>

      <section class="message-trace-detail-workbench__scale-ledger">
        <article
          v-for="metric in summaryMetrics"
          :key="metric.key"
          class="message-trace-detail-workbench__scale-note"
        >
          <span>{{ metric.label }}</span>
          <strong
            :class="[
              'message-trace-detail-workbench__scale-note-value',
              { 'message-trace-detail-workbench__scale-note-value--truncate': metric.truncate }
            ]"
            :title="metric.value"
          >
            {{ metric.value }}
          </strong>
        </article>
      </section>
    </section>

    <section
      class="message-trace-detail-workbench__stage message-trace-detail-workbench__stage--subtle"
      data-testid="message-trace-detail-chain-stage"
    >
      <div class="message-trace-detail-workbench__stage-header">
        <h3>链路信息</h3>
      </div>

      <div class="message-trace-detail-workbench__journal-grid">
        <section class="message-trace-detail-workbench__sheet">
          <header class="message-trace-detail-workbench__sheet-header">
            <strong class="message-trace-detail-workbench__sheet-title">链路标识</strong>
          </header>

          <div class="message-trace-detail-workbench__ledger-grid">
            <article
              v-for="item in ledgerIdentityItems"
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
                  { 'message-trace-detail-workbench__ledger-value--truncate': item.truncate }
                ]"
                :title="item.value"
              >
                {{ item.value }}
              </span>
            </article>
          </div>
        </section>

        <section class="message-trace-detail-workbench__sheet">
          <header class="message-trace-detail-workbench__sheet-header">
            <strong class="message-trace-detail-workbench__sheet-title">接入上下文</strong>
          </header>

          <div class="message-trace-detail-workbench__ledger-grid">
            <article
              v-for="item in ledgerContextItems"
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
                  { 'message-trace-detail-workbench__ledger-value--truncate': item.truncate }
                ]"
                :title="item.value"
              >
                {{ item.value }}
              </span>
            </article>
          </div>
        </section>
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

      <div class="message-trace-detail-workbench__journal-grid">
        <section class="message-trace-detail-workbench__sheet">
          <header class="message-trace-detail-workbench__sheet-header">
            <strong class="message-trace-detail-workbench__sheet-title">模板摘要</strong>
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
                  { 'message-trace-detail-workbench__ledger-value--truncate': item.truncate }
                ]"
                :title="item.value"
              >
                {{ item.value }}
              </span>
            </article>
          </div>
        </section>

        <section class="message-trace-detail-workbench__sheet">
          <header class="message-trace-detail-workbench__sheet-header">
            <strong class="message-trace-detail-workbench__sheet-title">模板执行</strong>
          </header>

          <div class="message-trace-detail-workbench__ledger-grid">
            <article
              v-for="item in templateExecutionItems"
              :key="item.key"
              class="message-trace-detail-workbench__ledger-item message-trace-detail-workbench__ledger-item--wide"
            >
              <span class="message-trace-detail-workbench__ledger-label">{{ item.label }}</span>
              <span class="message-trace-detail-workbench__ledger-value" :title="item.value">
                {{ item.value }}
              </span>
            </article>
          </div>
        </section>
      </div>
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

      <div
        class="message-trace-detail-workbench__timeline-summary"
        :class="`message-trace-detail-workbench__timeline-summary--${timelineSummaryTone}`"
      >
        <span class="message-trace-detail-workbench__timeline-label">{{ timelineSummaryLabel }}</span>
        <strong class="message-trace-detail-workbench__timeline-title">{{ timelineSummaryTitle }}</strong>
        <p
          v-if="timelineSummaryDescription"
          class="message-trace-detail-workbench__timeline-description"
        >
          {{ timelineSummaryDescription }}
        </p>
      </div>

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
  truncate?: boolean;
}

interface SummaryMetric {
  key: string;
  label: string;
  value: string;
  truncate?: boolean;
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

const summaryMetrics = computed<SummaryMetric[]>(() => [
  {
    key: 'reportTime',
    label: '上报时间',
    value: formatMessageTraceReportTime(props.detail.reportTime, props.detail.createTime),
    truncate: true
  },
  {
    key: 'topicSegments',
    label: 'Topic 节点',
    value: topicSegments.value,
    truncate: true
  },
  {
    key: 'productKey',
    label: '产品标识',
    value: formatValue(props.detail.productKey),
    truncate: true
  },
  {
    key: 'logId',
    label: '日志 ID',
    value: formatValue(props.detail.id),
    truncate: true
  }
]);

const ledgerIdentityItems = computed<LedgerItem[]>(() => [
  { key: 'id', label: '日志 ID', value: formatValue(props.detail.id) },
  { key: 'createTime', label: '创建时间', value: formatDateTime(props.detail.createTime) },
  { key: 'traceId', label: 'TraceId', value: formatValue(props.detail.traceId), wide: true },
]);

const ledgerContextItems = computed<LedgerItem[]>(() => [
  { key: 'deviceCode', label: '设备编码', value: formatValue(props.detail.deviceCode) },
  { key: 'productKey', label: '产品标识', value: formatValue(props.detail.productKey), truncate: true },
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
      wide: true,
      truncate: true
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
    wide: true
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

const timelineSummaryLabel = computed(() => {
  if (props.timelineLookupError) {
    return '存储状态';
  }
  if (props.timelineExpired) {
    return '降级提示';
  }
  if (props.timelineLoading) {
    return '处理中';
  }
  if (timelineStepCount.value > 0) {
    return '处理摘要';
  }
  return '当前状态';
});

const timelineSummaryTitle = computed(() => {
  if (props.timelineLookupError) {
    return 'message-flow 存储异常/Redis 不可用';
  }
  if (props.timelineExpired) {
    return '时间线已过期，但 payload 对照已从消息日志恢复。';
  }
  if (props.timelineLoading) {
    return '正在读取处理时间线';
  }
  if (timelineStepCount.value > 0) {
    return `已记录 ${timelineStepCount.value} 个处理节点`;
  }
  return props.timelineEmptyTitle;
});

const timelineSummaryDescription = computed(() => {
  if (props.timelineLookupError || props.timelineExpired || timelineStepCount.value === 0) {
    return props.timelineEmptyDescription;
  }
  return '';
});

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
    formatValue(execution.templateCode),
    execution.canonicalizationStrategy ? `归一: ${execution.canonicalizationStrategy}` : '',
    execution.statusMirrorApplied === null || execution.statusMirrorApplied === undefined
      ? ''
      : `状态镜像: ${formatBoolean(execution.statusMirrorApplied)}`,
    execution.parentRemovalKeys && execution.parentRemovalKeys.length > 0
      ? `父字段清理: ${execution.parentRemovalKeys.join(' / ')}`
      : ''
  ].filter(Boolean);
  return segments.join(' | ');
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
.message-trace-detail-workbench,
.message-trace-detail-workbench__lead-main,
.message-trace-detail-workbench__scale-ledger,
.message-trace-detail-workbench__sheet {
  display: grid;
}

.message-trace-detail-workbench {
  gap: 1.2rem;
}

.message-trace-detail-workbench__lead-sheet {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(20rem, 1.1fr);
  gap: 1rem;
  padding: 1.1rem 1.2rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 6px);
  background: var(--ops-list-surface-bg-strong);
  box-shadow: var(--shadow-card-soft);
}

.message-trace-detail-workbench__lead-main {
  gap: 0.48rem;
  align-content: center;
}

.message-trace-detail-workbench__section-kicker,
.message-trace-detail-workbench__scale-note span,
.message-trace-detail-workbench__ledger-label,
.message-trace-detail-workbench__timeline-label {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.58;
}

.message-trace-detail-workbench__section-kicker {
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.message-trace-detail-workbench__lead-value,
.message-trace-detail-workbench__sheet-title {
  color: var(--text-heading);
  font-family: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', serif;
}

.message-trace-detail-workbench__lead-value {
  font-size: clamp(2.2rem, 4vw, 3rem);
  font-weight: 700;
  line-height: 1;
}

.message-trace-detail-workbench__scale-ledger {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.78rem;
}

.message-trace-detail-workbench__scale-note {
  display: grid;
  gap: 0.34rem;
  min-width: 0;
  min-height: 5rem;
  padding: 0.95rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-md) + 4px);
  background: color-mix(in srgb, var(--surface-soft) 88%, white);
}

.message-trace-detail-workbench__scale-note-value {
  display: block;
  min-width: 0;
  color: var(--text-heading);
  font-size: 15px;
  line-height: 1.5;
}

.message-trace-detail-workbench__scale-note-value--truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.message-trace-detail-workbench__stage {
  display: grid;
  gap: 0.95rem;
  padding: 1rem 1.05rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: var(--bg-card);
  box-shadow: var(--shadow-card-soft);
}

.message-trace-detail-workbench__stage--subtle {
  background: var(--ops-list-surface-bg);
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

.message-trace-detail-workbench__stage-header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 16px;
  line-height: 1.4;
}

.message-trace-detail-workbench__journal-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.message-trace-detail-workbench__sheet {
  gap: 0.8rem;
  min-width: 0;
  padding: 1rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-md) + 4px);
  background: var(--surface-soft);
}

.message-trace-detail-workbench__sheet-header {
  display: grid;
}

.message-trace-detail-workbench__sheet-title {
  font-size: 1rem;
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
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-md) + 2px);
  background: color-mix(in srgb, var(--bg-card) 92%, var(--surface-soft));
}

.message-trace-detail-workbench__ledger-item--wide {
  grid-column: 1 / -1;
}

.message-trace-detail-workbench__ledger-value {
  color: var(--text-heading);
  font-size: 14px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.message-trace-detail-workbench__ledger-value--truncate {
  overflow: hidden;
  overflow-wrap: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.message-trace-detail-workbench__timeline-summary {
  display: grid;
  gap: 0.36rem;
  padding: 0.95rem 1rem;
  border-radius: calc(var(--radius-md) + 4px);
  border: 1px solid var(--panel-border);
  background: var(--surface-soft);
}

.message-trace-detail-workbench__timeline-summary--ready {
  border-color: color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  background: color-mix(in srgb, var(--brand) 5%, var(--surface-soft));
}

.message-trace-detail-workbench__timeline-summary--warning {
  border-color: color-mix(in srgb, var(--warning) 22%, var(--panel-border));
  background: color-mix(in srgb, var(--warning-bg) 42%, white);
}

.message-trace-detail-workbench__timeline-summary--muted {
  background: var(--surface-muted);
}

.message-trace-detail-workbench__timeline-summary--loading {
  border-color: color-mix(in srgb, var(--accent) 16%, var(--panel-border));
  background: color-mix(in srgb, var(--info-bg) 42%, white);
}

.message-trace-detail-workbench__timeline-title {
  color: var(--text-heading);
  font-size: 14px;
  line-height: 1.6;
}

.message-trace-detail-workbench__timeline-description {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 1024px) {
  .message-trace-detail-workbench__lead-sheet,
  .message-trace-detail-workbench__journal-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .message-trace-detail-workbench__scale-ledger,
  .message-trace-detail-workbench__ledger-grid {
    grid-template-columns: 1fr;
  }

  .message-trace-detail-workbench__stage-header--between {
    align-items: flex-start;
    flex-direction: column;
  }

  .message-trace-detail-workbench__ledger-item--wide {
    grid-column: auto;
  }
}
</style>
