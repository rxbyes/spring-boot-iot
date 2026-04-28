<template>
  <div class="message-trace-detail-workbench">
    <section
      v-if="detailContinuityNotice"
      class="message-trace-detail-workbench__continuity"
      data-testid="message-trace-detail-continuity"
    >
      <div class="message-trace-detail-workbench__continuity-copy">
        <span class="message-trace-detail-workbench__continuity-eyebrow">
          {{ detailContinuityNotice.eyebrow }}
        </span>
        <strong>{{ detailContinuityNotice.title }}</strong>
        <p>{{ detailContinuityNotice.description }}</p>
      </div>

      <div class="message-trace-detail-workbench__continuity-meta">
        <span
          class="message-trace-detail-workbench__continuity-signal"
          :class="`message-trace-detail-workbench__continuity-signal--${detailContinuityNotice.tone}`"
        >
          {{ detailContinuityNotice.signal }}
        </span>
        <span
          v-for="pill in detailContinuityNotice.pills"
          :key="pill"
          class="message-trace-detail-workbench__continuity-pill"
        >
          {{ pill }}
        </span>
      </div>

      <div class="message-trace-detail-workbench__continuity-actions">
        <button
          v-if="detailContinuityNotice.showReturnAction"
          type="button"
          class="message-trace-detail-workbench__continuity-action message-trace-detail-workbench__continuity-action--primary"
          @click="emit('return-to-insight')"
        >
          回对象洞察继续排查
        </button>
        <button
          v-if="detailContinuityNotice.showCopyAction"
          type="button"
          class="message-trace-detail-workbench__continuity-action"
          @click="emit('copy-trace-clues')"
        >
          复制链路线索
        </button>
      </div>

      <div
        v-if="detailPriorityGuide"
        class="message-trace-detail-workbench__priority-guide"
        data-testid="message-trace-detail-priority-guide"
      >
        <article class="message-trace-detail-workbench__priority-step">
          <span class="message-trace-detail-workbench__priority-step-label">先看</span>
          <strong>时间线状态</strong>
          <p>{{ detailPriorityGuide.timeline }}</p>
        </article>
        <article class="message-trace-detail-workbench__priority-step">
          <span class="message-trace-detail-workbench__priority-step-label">再看</span>
          <strong>Payload 对照</strong>
          <p>{{ detailPriorityGuide.payload }}</p>
        </article>
      </div>
    </section>

    <section
      :class="[
        'message-trace-detail-workbench__stage',
        {
          'message-trace-detail-workbench__stage--contextual': hasInboundEvidencePriority
        }
      ]"
      data-testid="message-trace-detail-summary-stage"
    >
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

    <section
      :class="[
        'message-trace-detail-workbench__stage',
        {
          'message-trace-detail-workbench__stage--evidence-secondary': hasInboundEvidencePriority
        }
      ]"
      data-testid="message-trace-detail-payload-stage"
    >
      <div class="message-trace-detail-workbench__stage-header">
        <div class="message-trace-detail-workbench__stage-header-copy">
          <span
            v-if="hasInboundEvidencePriority"
            class="message-trace-detail-workbench__stage-header-kicker message-trace-detail-workbench__stage-header-kicker--secondary"
          >
            再看
          </span>
          <h3>Payload 对照</h3>
          <p v-if="detailPriorityGuide" class="message-trace-detail-workbench__stage-note">
            {{ detailPriorityGuide.payload }}
          </p>
        </div>
      </div>

      <MessageTracePayloadComparisonSection :panels="panels" />
    </section>

    <section
      :class="[
        'message-trace-detail-workbench__stage',
        {
          'message-trace-detail-workbench__stage--evidence-primary': hasInboundEvidencePriority
        }
      ]"
      data-testid="message-trace-detail-timeline-stage"
    >
      <div class="message-trace-detail-workbench__stage-header message-trace-detail-workbench__stage-header--between">
        <div class="message-trace-detail-workbench__stage-header-copy">
          <span
            v-if="hasInboundEvidencePriority"
            class="message-trace-detail-workbench__stage-header-kicker message-trace-detail-workbench__stage-header-kicker--primary"
          >
            先看
          </span>
          <h3>处理时间线</h3>
          <p v-if="detailPriorityGuide" class="message-trace-detail-workbench__stage-note">
            {{ detailPriorityGuide.timeline }}
          </p>
        </div>
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
          :data-testid="`message-trace-timeline-card-${card.key}`"
          :class="[
            'message-trace-detail-workbench__timeline-card',
            `message-trace-detail-workbench__timeline-card--${card.tone ?? 'plain'}`,
            card.emphasis ? `message-trace-detail-workbench__timeline-card--${card.emphasis}` : '',
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
import type { DiagnosticContext } from '@/utils/iotAccessDiagnostics';
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
  emphasis?: 'spotlight' | 'context';
  wide?: boolean;
}

interface DetailContinuityNotice {
  eyebrow: string;
  title: string;
  description: string;
  signal: string;
  tone: 'ready' | 'warning' | 'muted' | 'attention' | 'loading';
  pills: string[];
  showReturnAction: boolean;
  showCopyAction: boolean;
}

interface DetailPriorityGuide {
  timeline: string;
  payload: string;
}

const emit = defineEmits<{
  (event: 'return-to-insight'): void;
  (event: 'copy-trace-clues'): void;
}>();

const props = defineProps<{
  detail: Partial<MessageTraceDetail>;
  panels: MessageTracePayloadComparisonPanel[];
  timeline: MessageFlowTimeline | null;
  timelineLoading: boolean;
  timelineExpired: boolean;
  timelineLookupError: boolean;
  timelineEmptyTitle: string;
  timelineEmptyDescription: string;
  inboundContext?: DiagnosticContext | null;
  canReturnToInsight?: boolean;
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

const detailContinuityNotice = computed<DetailContinuityNotice | null>(() => {
  const inboundContext = props.inboundContext;
  if (!inboundContext || inboundContext.sourcePage !== 'insight' || inboundContext.reportStatus !== 'timeline-missing') {
    return null;
  }

  const pills = [
    props.detail.deviceCode || inboundContext.deviceCode ? `设备 ${formatValue(props.detail.deviceCode ?? inboundContext.deviceCode)}` : '',
    props.detail.traceId || inboundContext.traceId ? `Trace ${formatValue(props.detail.traceId ?? inboundContext.traceId)}` : '',
    props.detail.topic || inboundContext.topic ? '带 Topic 回看' : ''
  ].filter(Boolean);

  if (props.timelineLookupError) {
    return {
      eyebrow: '来自对象洞察台',
      title: '当前正在补 latest 链路',
      description: '这次是沿着 latest 缺口回到主链路复盘。message-flow / Redis 读取异常时，先用消息日志、Payload 和 Topic 确认上报是否进入主链路，再决定是否回对象洞察继续排查。',
      signal: '主链路证据暂未接住',
      tone: 'warning',
      pills,
      showReturnAction: Boolean(props.canReturnToInsight),
      showCopyAction: pills.length > 0
    };
  }

  if (props.timelineExpired) {
    return {
      eyebrow: '来自对象洞察台',
      title: '当前正在补 latest 链路',
      description: '这次是沿着 latest 缺口回到主链路复盘。短期时间线已过期时，优先复盘消息日志、Payload 和 Topic，确认 latest 写入前后的证据是否还完整。',
      signal: '短期时间线已过期',
      tone: 'muted',
      pills,
      showReturnAction: Boolean(props.canReturnToInsight),
      showCopyAction: pills.length > 0
    };
  }

  if (props.timelineLoading) {
    return {
      eyebrow: '来自对象洞察台',
      title: '当前正在补 latest 链路',
      description: '这次是沿着 latest 缺口回到主链路复盘。后台正在补主链路证据，先保留当前 Trace / Topic 线索，等时间线加载完成后再确认 latest 写入落在哪一段。',
      signal: '正在补主链路证据',
      tone: 'loading',
      pills,
      showReturnAction: Boolean(props.canReturnToInsight),
      showCopyAction: pills.length > 0
    };
  }

  if (timelineStepCount.value > 0) {
    return {
      eyebrow: '来自对象洞察台',
      title: '当前正在补 latest 链路',
      description: '主链路证据已经接住。先看时间线和 Payload 对照，确认 latest 写入是在进入主链路前掉线，还是在处理中途出现缺口。',
      signal: '主链路证据已接住',
      tone: 'ready',
      pills,
      showReturnAction: Boolean(props.canReturnToInsight),
      showCopyAction: pills.length > 0
    };
  }

  return {
    eyebrow: '来自对象洞察台',
    title: '当前正在补 latest 链路',
    description: '这次是沿着 latest 缺口回到主链路复盘。先核对当前 Trace、Topic 和设备编码是否齐全，再继续补主链路证据。',
    signal: '先核对 Trace / Topic',
    tone: 'attention',
    pills,
    showReturnAction: Boolean(props.canReturnToInsight),
    showCopyAction: pills.length > 0
  };
});

const detailPriorityGuide = computed<DetailPriorityGuide | null>(() => {
  if (!detailContinuityNotice.value) {
    return null;
  }

  if (props.timelineLookupError) {
    return {
      timeline: '先确认 Redis / message-flow 读取是否异常，再判断 latest 缺口是存储缺证还是链路断点。',
      payload: '再核对原始、解密和解析结果，确认 latest 缺口前后的字段是否已经进入主链路。'
    };
  }

  if (props.timelineExpired) {
    return {
      timeline: '先用当前 Trace 和过期提示判断 latest 缺口是否已越过短期时间线窗口。',
      payload: '再核对原始、解密和解析结果，补齐 latest 写入前后还能复盘到的报文证据。'
    };
  }

  if (props.timelineLoading) {
    return {
      timeline: '先等主链路证据加载完成，再确认 latest 写入落在哪一段处理节点上。',
      payload: '再对照原始、解密和解析结果，避免在时间线未补齐前过早判断字段是否丢失。'
    };
  }

  if (timelineStepCount.value > 0) {
    return {
      timeline: '先看时间线状态和处理节点，确认 latest 写入落在哪一段。',
      payload: '再核对 latest 写入前后的原始、解密和解析差异。'
    };
  }

  return {
    timeline: '先核对当前 Trace、Topic 和设备编码是否足够定位主链路。',
    payload: '再核对当前报文内容，确认 latest 缺口是否来自字段缺失或解析未命中。'
  };
});

const hasInboundEvidencePriority = computed(() => Boolean(detailPriorityGuide.value));

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

const timelinePriorityCardKeys = computed(() => {
  if (!hasInboundEvidencePriority.value) {
    return {
      spotlight: [] as string[],
      context: [] as string[]
    };
  }

  if (props.timelineLoading || props.timelineLookupError || props.timelineExpired || timelineStepCount.value === 0) {
    return {
      spotlight: ['status', 'storage'],
      context: ['steps', 'owner']
    };
  }

  return {
    spotlight: ['status', 'steps'],
    context: ['owner', 'storage']
  };
});

function getTimelineCardEmphasis(key: string): TimelineSummaryCard['emphasis'] {
  if (timelinePriorityCardKeys.value.spotlight.includes(key)) {
    return 'spotlight';
  }
  if (timelinePriorityCardKeys.value.context.includes(key)) {
    return 'context';
  }
  return undefined;
}

const timelineSummaryCards = computed<TimelineSummaryCard[]>(() => [
  {
    key: 'status',
    label: '当前状态',
    value: timelineStatusValue.value,
    hint: timelineStatusHint.value,
    tone: timelineSummaryTone.value,
    emphasis: getTimelineCardEmphasis('status')
  },
  {
    key: 'steps',
    label: '处理节点',
    value: timelineNodeValue.value,
    hint: timelineNodeHint.value,
    emphasis: getTimelineCardEmphasis('steps')
  },
  {
    key: 'owner',
    label: 'Trace 归属',
    value: timelineOwnerValue.value,
    hint: timelineOwnerHint.value,
    emphasis: getTimelineCardEmphasis('owner'),
    wide: !props.timeline?.sessionId && timelineOwnerValue.value !== '--'
  },
  {
    key: 'storage',
    label: '存储提示',
    value: timelineStorageValue.value,
    hint: timelineStorageHint.value,
    emphasis: getTimelineCardEmphasis('storage')
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

.message-trace-detail-workbench__continuity {
  display: grid;
  gap: 0.9rem;
  padding: 1rem 1.05rem;
  border: 1px solid color-mix(in srgb, var(--warning) 16%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 4px);
  background: color-mix(in srgb, var(--warning-bg) 28%, white);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.message-trace-detail-workbench__continuity-copy {
  display: grid;
  gap: 0.3rem;
  min-width: 0;
}

.message-trace-detail-workbench__continuity-eyebrow {
  color: color-mix(in srgb, var(--warning) 72%, var(--text-secondary));
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.message-trace-detail-workbench__continuity-copy strong {
  color: var(--text-heading);
  font-size: 15px;
  line-height: 1.5;
}

.message-trace-detail-workbench__continuity-copy p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.message-trace-detail-workbench__continuity-meta,
.message-trace-detail-workbench__continuity-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
}

.message-trace-detail-workbench__continuity-pill,
.message-trace-detail-workbench__continuity-signal {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 0.8rem;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1;
}

.message-trace-detail-workbench__continuity-pill {
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  background: rgba(255, 255, 255, 0.9);
  color: var(--text-secondary);
}

.message-trace-detail-workbench__continuity-signal {
  border: 1px solid color-mix(in srgb, var(--warning) 18%, var(--panel-border));
  background: rgba(255, 255, 255, 0.95);
  color: var(--text-heading);
  font-weight: 600;
}

.message-trace-detail-workbench__continuity-signal--ready {
  border-color: color-mix(in srgb, var(--success) 20%, var(--panel-border));
  color: color-mix(in srgb, var(--success) 74%, var(--text-heading));
}

.message-trace-detail-workbench__continuity-signal--warning {
  border-color: color-mix(in srgb, var(--warning) 26%, var(--panel-border));
  color: color-mix(in srgb, var(--warning) 82%, var(--text-heading));
}

.message-trace-detail-workbench__continuity-signal--muted {
  border-color: color-mix(in srgb, var(--text-secondary) 20%, var(--panel-border));
  color: var(--text-secondary);
}

.message-trace-detail-workbench__continuity-signal--attention {
  border-color: color-mix(in srgb, var(--brand) 16%, var(--panel-border));
  color: color-mix(in srgb, var(--brand) 72%, var(--text-heading));
}

.message-trace-detail-workbench__continuity-signal--loading {
  border-color: color-mix(in srgb, var(--accent) 18%, var(--panel-border));
  color: color-mix(in srgb, var(--accent) 72%, var(--text-heading));
}

.message-trace-detail-workbench__continuity-action {
  min-height: 34px;
  padding: 0 0.95rem;
  border: 1px solid var(--panel-border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast),
    background var(--transition-fast);
}

.message-trace-detail-workbench__continuity-action:hover {
  border-color: color-mix(in srgb, var(--brand) 18%, var(--panel-border));
  color: var(--brand);
  background: color-mix(in srgb, var(--brand) 6%, white);
}

.message-trace-detail-workbench__continuity-action--primary {
  border-color: color-mix(in srgb, var(--brand) 18%, var(--panel-border));
  background: color-mix(in srgb, var(--brand) 8%, white);
  color: var(--brand);
}

.message-trace-detail-workbench__priority-guide {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.message-trace-detail-workbench__priority-step {
  display: grid;
  gap: 0.3rem;
  min-width: 0;
  padding: 0.88rem 0.95rem;
  border: 1px dashed color-mix(in srgb, var(--warning) 18%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(255, 255, 255, 0.82);
}

.message-trace-detail-workbench__priority-step-label {
  color: color-mix(in srgb, var(--warning) 72%, var(--text-secondary));
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.message-trace-detail-workbench__priority-step strong {
  color: var(--text-heading);
  font-size: 14px;
  line-height: 1.5;
}

.message-trace-detail-workbench__priority-step p,
.message-trace-detail-workbench__stage-note {
  margin: 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.7;
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

.message-trace-detail-workbench__stage--contextual {
  border-color: color-mix(in srgb, var(--panel-border) 88%, white);
  background:
    linear-gradient(180deg, rgba(250, 252, 255, 0.94) 0%, rgba(247, 250, 255, 0.92) 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.88);
}

.message-trace-detail-workbench__stage--evidence-primary,
.message-trace-detail-workbench__stage--evidence-secondary {
  border-color: color-mix(in srgb, var(--brand) 16%, var(--panel-border));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.92),
    0 0 0 1px rgba(255, 255, 255, 0.36);
}

.message-trace-detail-workbench__stage--evidence-primary {
  background:
    linear-gradient(180deg, rgba(255, 250, 245, 0.98) 0%, rgba(255, 255, 255, 0.96) 100%);
}

.message-trace-detail-workbench__stage--evidence-secondary {
  background:
    linear-gradient(180deg, rgba(250, 252, 255, 0.98) 0%, rgba(255, 255, 255, 0.96) 100%);
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

.message-trace-detail-workbench__stage-header-copy {
  display: grid;
  gap: 0.22rem;
  min-width: 0;
}

.message-trace-detail-workbench__stage-header-kicker {
  display: inline-flex;
  align-items: center;
  justify-self: flex-start;
  min-height: 22px;
  padding: 0 0.55rem;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
}

.message-trace-detail-workbench__stage-header-kicker--primary {
  background: color-mix(in srgb, var(--brand) 12%, white);
  color: color-mix(in srgb, var(--brand) 86%, var(--text-heading));
}

.message-trace-detail-workbench__stage-header-kicker--secondary {
  background: color-mix(in srgb, var(--info) 10%, white);
  color: color-mix(in srgb, var(--info) 72%, var(--text-heading));
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

.message-trace-detail-workbench__stage--contextual .message-trace-detail-workbench__summary-card {
  border-color: color-mix(in srgb, var(--panel-border) 92%, white);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: none;
}

.message-trace-detail-workbench__stage--contextual .message-trace-detail-workbench__summary-value {
  font-size: 14px;
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

.message-trace-detail-workbench__timeline-card--spotlight {
  border-color: color-mix(in srgb, var(--brand) 20%, var(--panel-border));
  background:
    linear-gradient(180deg, rgba(255, 249, 243, 0.98) 0%, rgba(255, 255, 255, 0.96) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.94),
    0 0 0 1px rgba(255, 244, 236, 0.78);
}

.message-trace-detail-workbench__timeline-card--context {
  border-color: color-mix(in srgb, var(--panel-border) 92%, white);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: none;
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

  .message-trace-detail-workbench__priority-guide {
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
