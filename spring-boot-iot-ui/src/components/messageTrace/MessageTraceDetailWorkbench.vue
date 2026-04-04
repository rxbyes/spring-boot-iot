<template>
  <div class="message-trace-detail-workbench">
    <section class="message-trace-detail-workbench__lead-sheet" data-testid="message-trace-detail-lead-sheet">
      <div class="message-trace-detail-workbench__lead-main">
        <span class="message-trace-detail-workbench__section-kicker">消息概览</span>
        <strong class="message-trace-detail-workbench__lead-value">{{ messageTypeLabel }}</strong>
        <span class="message-trace-detail-workbench__lead-caption">
          先从消息类型、上报时间与 Topic 拓扑建立判断，再决定往 Payload 对照还是处理时间线继续钻。
        </span>
      </div>

      <section class="message-trace-detail-workbench__scale-ledger">
        <article
          v-for="metric in summaryMetrics"
          :key="metric.key"
          class="message-trace-detail-workbench__scale-note"
        >
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </article>
      </section>
    </section>

    <section
      class="message-trace-detail-workbench__stage message-trace-detail-workbench__stage--subtle"
      data-testid="message-trace-detail-chain-stage"
    >
      <div class="message-trace-detail-workbench__stage-header">
        <div>
          <h3>链路信息</h3>
          <p>继续完整保留链路章节，再在章内拆分链路标识与接入上下文，避免信息散排。</p>
        </div>
      </div>

      <div class="message-trace-detail-workbench__journal-grid">
        <section class="message-trace-detail-workbench__sheet">
          <header class="message-trace-detail-workbench__sheet-header">
            <strong class="message-trace-detail-workbench__sheet-title">链路标识</strong>
            <p>先确认日志主键、TraceId、消息类型与处理锚点。</p>
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
              <span class="message-trace-detail-workbench__ledger-value">{{ item.value }}</span>
            </article>
          </div>
        </section>

        <section class="message-trace-detail-workbench__sheet">
          <header class="message-trace-detail-workbench__sheet-header">
            <strong class="message-trace-detail-workbench__sheet-title">接入上下文</strong>
            <p>把设备、产品和 Topic 收在同一组，便于继续联动异常观测与接入智维页面。</p>
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
              <span class="message-trace-detail-workbench__ledger-value">{{ item.value }}</span>
            </article>
          </div>
        </section>
      </div>
    </section>

    <section class="message-trace-detail-workbench__stage" data-testid="message-trace-detail-payload-stage">
      <div class="message-trace-detail-workbench__stage-header">
        <div>
          <h3>Payload 对照</h3>
          <p>固定并排查看原始报文、恢复后的明文和解析结果，直接判断偏差出现在协议前还是协议后。</p>
        </div>
      </div>

      <MessageTracePayloadComparisonSection :panels="panels" />

      <div class="message-trace-detail-workbench__notice">
        <span class="message-trace-detail-workbench__notice-label">排查建议</span>
        <strong class="message-trace-detail-workbench__notice-value">{{ routeAdvice }}</strong>
      </div>
    </section>

    <section class="message-trace-detail-workbench__stage" data-testid="message-trace-detail-timeline-stage">
      <div class="message-trace-detail-workbench__stage-header">
        <div>
          <h3>处理时间线</h3>
          <p>处理阶段继续沿用当前时间线语法，只负责复盘 Pipeline，不再决定 Payload 是否可读。</p>
        </div>
      </div>

      <StandardTraceTimeline
        :timeline="timeline"
        :loading="timelineLoading"
        :empty-title="timelineEmptyTitle"
        :empty-description="timelineEmptyDescription"
      />

      <div v-if="timelineLookupError" class="message-trace-detail-workbench__notice">
        <span class="message-trace-detail-workbench__notice-label">存储提示</span>
        <strong class="message-trace-detail-workbench__notice-value">当前时间线查询异常，但 payload 对照仍可继续排查。</strong>
      </div>
      <div v-else-if="timelineExpired" class="message-trace-detail-workbench__notice">
        <span class="message-trace-detail-workbench__notice-label">降级提示</span>
        <strong class="message-trace-detail-workbench__notice-value">时间线已过期，但 payload 对照已从消息日志恢复。</strong>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import MessageTracePayloadComparisonSection from '@/components/messageTrace/MessageTracePayloadComparisonSection.vue';
import StandardTraceTimeline from '@/components/StandardTraceTimeline.vue';
import type { MessageFlowTimeline, MessageTraceDetail } from '@/types/api';
import type { MessageTracePayloadComparisonPanel } from '@/utils/messageTracePayloadComparison';
import { formatDateTime } from '@/utils/format';

interface LedgerItem {
  key: string;
  label: string;
  value: string;
  wide?: boolean;
}

interface SummaryMetric {
  key: string;
  label: string;
  value: string;
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
  routeAdvice: string;
}>();

const messageTypeLabel = computed(() => getMessageTypeLabel(props.detail.messageType));
const topicSegments = computed(() => {
  if (!props.detail.topic) {
    return '--';
  }
  return String(props.detail.topic).split('/').filter(Boolean).length.toString();
});

const summaryMetrics = computed<SummaryMetric[]>(() => [
  {
    key: 'reportTime',
    label: '上报时间',
    value: formatDateTime(props.detail.reportTime || props.detail.createTime)
  },
  {
    key: 'topicSegments',
    label: 'Topic 节点',
    value: topicSegments.value
  },
  {
    key: 'productKey',
    label: '产品标识',
    value: formatValue(props.detail.productKey)
  },
  {
    key: 'logId',
    label: '日志 ID',
    value: formatValue(props.detail.id)
  }
]);

const ledgerIdentityItems = computed<LedgerItem[]>(() => [
  { key: 'id', label: '日志 ID', value: formatValue(props.detail.id) },
  { key: 'traceId', label: 'TraceId', value: formatValue(props.detail.traceId), wide: true },
  { key: 'messageType', label: '消息类型', value: messageTypeLabel.value },
  { key: 'createTime', label: '创建时间', value: formatDateTime(props.detail.createTime) }
]);

const ledgerContextItems = computed<LedgerItem[]>(() => [
  { key: 'deviceCode', label: '设备编码', value: formatValue(props.detail.deviceCode) },
  { key: 'productKey', label: '产品标识', value: formatValue(props.detail.productKey) },
  { key: 'topic', label: 'Topic', value: formatValue(props.detail.topic), wide: true }
]);

function formatValue(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '--';
  }
  return String(value);
}

function getMessageTypeLabel(value?: string | null) {
  switch (value) {
    case 'report':
      return '属性上报';
    case 'reply':
      return '命令回执';
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
  gap: 1.35rem;
}

.message-trace-detail-workbench__lead-sheet {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(18rem, 0.92fr);
  gap: 1.8rem;
  align-items: start;
  padding-top: 0.92rem;
  border-top: 1px solid var(--panel-border-strong);
}

.message-trace-detail-workbench__lead-main {
  gap: 0.42rem;
  padding-right: 1.45rem;
  border-right: 1px solid var(--panel-border);
}

.message-trace-detail-workbench__section-kicker,
.message-trace-detail-workbench__scale-note span,
.message-trace-detail-workbench__sheet-header p,
.message-trace-detail-workbench__ledger-label,
.message-trace-detail-workbench__notice-label {
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
  font-size: clamp(2.2rem, 3.8vw, 3.1rem);
  font-weight: 700;
  line-height: 1;
}

.message-trace-detail-workbench__lead-caption {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.68;
}

.message-trace-detail-workbench__scale-ledger {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  align-content: start;
}

.message-trace-detail-workbench__scale-note {
  display: grid;
  gap: 0.36rem;
  min-height: 5rem;
  padding: 0.34rem 0 0.72rem 1rem;
  border-left: 1px solid var(--panel-border);
}

.message-trace-detail-workbench__scale-note strong {
  color: var(--text-heading);
  font-size: 1rem;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.message-trace-detail-workbench__stage {
  display: grid;
  gap: 0.95rem;
  padding: 1rem 1.05rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-inset-highlight-78);
}

.message-trace-detail-workbench__stage--subtle {
  background: rgba(255, 255, 255, 0.9);
}

.message-trace-detail-workbench__stage-header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 16px;
  line-height: 1.4;
}

.message-trace-detail-workbench__stage-header p {
  margin: 0.35rem 0 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.65;
}

.message-trace-detail-workbench__journal-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.98fr) minmax(0, 1.02fr);
  gap: 1.2rem;
}

.message-trace-detail-workbench__sheet {
  gap: 0.78rem;
  padding-top: 0.88rem;
  border-top: 1px solid var(--panel-border-strong);
}

.message-trace-detail-workbench__sheet-header {
  display: grid;
  gap: 0.24rem;
}

.message-trace-detail-workbench__sheet-title {
  font-size: 1.05rem;
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
  line-height: 1.58;
  overflow-wrap: anywhere;
}

.message-trace-detail-workbench__notice {
  display: grid;
  gap: 0.25rem;
  padding: 0.9rem 1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(248, 251, 255, 0.92);
}

.message-trace-detail-workbench__notice-value {
  color: var(--text-heading);
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 1024px) {
  .message-trace-detail-workbench__lead-sheet,
  .message-trace-detail-workbench__journal-grid {
    grid-template-columns: 1fr;
  }

  .message-trace-detail-workbench__lead-main {
    padding-right: 0;
    padding-bottom: 0.9rem;
    border-right: 0;
    border-bottom: 1px solid var(--panel-border);
  }

  .message-trace-detail-workbench__scale-note {
    min-height: auto;
    padding-left: 0.82rem;
  }
}

@media (max-width: 720px) {
  .message-trace-detail-workbench__scale-ledger,
  .message-trace-detail-workbench__ledger-grid {
    grid-template-columns: 1fr;
  }

  .message-trace-detail-workbench__scale-note {
    padding-left: 0;
    border-left: 0;
    border-bottom: 1px solid color-mix(in srgb, var(--brand) 7%, var(--panel-border));
  }

  .message-trace-detail-workbench__ledger-item--wide {
    grid-column: auto;
  }
}
</style>
