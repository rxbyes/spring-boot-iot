<template>
  <div class="page-stack reporting-view ops-workbench">
    <section class="reporting-command-strip">
      <div class="reporting-command-strip__copy">
        <h1 class="reporting-command-strip__title">链路验证中心</h1>
        <p class="reporting-command-strip__judgement">先校准设备身份，再发报文，再看时间线。</p>
        <p class="reporting-command-strip__meta">{{ reportingStripStatus }}</p>
      </div>
      <div class="reporting-command-strip__actions">
        <StandardButton action="refresh" plain @click="handleOpenTraceWorkbench">链路追踪台</StandardButton>
      </div>
    </section>

    <section class="reporting-main-layout">
      <PanelCard class="reporting-surface reporting-surface--compose">
        <template #header>
          <div class="reporting-surface__header">
            <div class="reporting-surface__heading">
              <p class="reporting-surface__eyebrow">左侧模拟上报</p>
              <h2 class="reporting-surface__title">模拟上报</h2>
              <p class="reporting-surface__description">
                按设备编码加载接入契约后，完成 HTTP / MQTT 双通道模拟上报。
              </p>
            </div>
            <span class="reporting-surface__badge">左侧模拟上报</span>
          </div>
        </template>

        <form class="reporting-compose-form" @submit.prevent="handleSendReport">
          <section class="reporting-section">
            <StandardInlineSectionHeader
                title="设备查询"
                description="设备编码是唯一查询入口；查询成功后只读反显产品 Key、协议编码和客户端 ID。"
            />

            <div class="reporting-query-row">
              <el-input
                  id="report-device-code"
                  v-model="reportForm.deviceCode"
                  name="report_device_code"
                  placeholder="请输入设备编码"
                  clearable
                  @keyup.enter="handleQueryDevice"
              />
              <StandardButton
                  action="query"
                  :loading="isQueryingDevice"
                  :disabled="!normalizedText(reportForm.deviceCode)"
                  @click="handleQueryDevice"
              >
                {{ isQueryingDevice ? '查询中...' : '查询设备' }}
              </StandardButton>
            </div>

            <StandardInlineState :message="deviceLookupMessage" :tone="deviceLookupInlineTone" />

            <StandardInfoGrid
                v-if="resolvedDevice"
                :items="deviceIdentityItems"
                :columns="2"
            />
          </section>

          <section class="reporting-section">
            <StandardInlineSectionHeader
                title="模拟配置"
                description="先确定传输方式与上报模式，再校准 Topic 和租户参数。"
            />

            <div class="reporting-control-grid">
              <article class="reporting-control-card">
                <div class="reporting-control-card__header">
                  <span class="reporting-control-card__title">传输方式</span>
                  <span class="reporting-control-card__helper">HTTP / MQTT</span>
                </div>
                <StandardChoiceGroup
                  v-model="transportMode"
                  :options="transportModeOptions"
                  responsive
                />
              </article>

              <article class="reporting-control-card">
                <div class="reporting-control-card__header">
                  <span class="reporting-control-card__title">上报模式</span>
                  <span class="reporting-control-card__helper">明文 / 密文</span>
                </div>
                <StandardChoiceGroup
                  v-model="reportMode"
                  :options="reportModeOptions"
                  responsive
                />
              </article>

              <article class="reporting-control-card">
                <div class="reporting-control-card__header">
                  <span class="reporting-control-card__title">租户 ID</span>
                  <span class="reporting-control-card__helper">仅 HTTP 生效</span>
                </div>
                <el-input
                    id="report-tenant"
                    v-model="reportForm.tenantId"
                    name="report_tenant_id"
                    inputmode="numeric"
                    placeholder="1"
                    clearable
                />
              </article>

              <article class="reporting-control-card">
                <div class="reporting-control-card__header">
                  <span class="reporting-control-card__title">Topic</span>
                  <span class="reporting-control-card__helper">默认 `$dp`，可手工覆盖</span>
                </div>
                <div class="reporting-topic-row">
                  <el-input
                      id="report-topic"
                      v-model="reportForm.topic"
                      name="report_topic"
                      autocomplete="off"
                      spellcheck="false"
                      placeholder="$dp"
                      clearable
                  />
                  <StandardButton action="reset" plain :disabled="!resolvedDevice" @click="syncTopic">套用推荐</StandardButton>
                </div>
              </article>
            </div>

            <div class="reporting-quick-fill">
              <StandardButton action="reset" plain @click="oneClickFill">一键补全</StandardButton>
              <span class="reporting-quick-fill__hint">
                自动查询设备后套用推荐 Topic，并在密文协议场景下切换建议模式。
              </span>
            </div>
          </section>

          <section class="reporting-section">
            <StandardInlineSectionHeader
                title="Payload 编辑"
                description="支持原始文本、JSON、XML。只有可识别 JSON 才会参与 C.1 / C.2 / C.3 明文帧判定。"
            >
              <template #actions>
                <span class="reporting-format-chip">当前格式 {{ payloadFormatLabel }}</span>
              </template>
            </StandardInlineSectionHeader>

            <div class="reporting-toolbar">
              <div class="reporting-toolbar__group">
                <span class="reporting-toolbar__label">模板</span>
                <StandardActionGroup gap="sm">
                  <StandardButton
                      v-for="template in filteredTemplates"
                      :key="template.name"
                      action="reset"
                      plain
                      size="small"
                      @click="applyTemplate(template)"
                  >
                    {{ template.name }}
                  </StandardButton>
                </StandardActionGroup>
              </div>

              <div class="reporting-toolbar__group">
                <span class="reporting-toolbar__label">格式化</span>
                <StandardActionGroup gap="sm">
                  <StandardButton action="reset" plain size="small" @click="formatPayloadAsJson">JSON</StandardButton>
                  <StandardButton action="reset" plain size="small" @click="formatPayloadAsXml">XML</StandardButton>
                </StandardActionGroup>
              </div>
            </div>

            <el-input
                id="payload"
                v-model="reportForm.payload"
                name="report_payload"
                type="textarea"
                :rows="11"
                spellcheck="false"
                :placeholder="payloadPlaceholder"
                class="reporting-textarea"
            />

            <div
                v-if="reportMode === 'plaintext' && plaintextFrame?.type === 3"
                class="reporting-type3-panel"
            >
              <StandardInlineSectionHeader
                  title="类型 3 文件流 Base64"
                  description="若 C.3 需要携带文件流，可在此粘贴 Base64 内容。"
              />
              <el-input
                  id="report-type3-binary"
                  v-model="type3BinaryBase64"
                  name="report_type3_binary_base64"
                  type="textarea"
                  :rows="3"
                  spellcheck="false"
                  placeholder="若 C.3 需要携带文件流，可在此粘贴 Base64。"
                  class="reporting-textarea"
              />
            </div>
          </section>

          <section class="reporting-section reporting-section--submit">
            <div v-if="showValidationBanner" class="reporting-validation-banner">
              <strong>发送前请修复以下问题：</strong>
              <ul class="reporting-validation-list">
                <li v-for="issue in validationIssues" :key="`${issue.field}:${issue.message}`">
                  {{ issue.message }}
                </li>
              </ul>
            </div>

            <div class="reporting-submit-row">
              <div class="reporting-submit-copy">
                <span class="reporting-submit-label">当前发送状态</span>
                <strong>{{ sendStatusText }}</strong>
                <p>发送前会基于当前输入实时推导实际 payload、Topic 建议和字节编码。</p>
              </div>
              <StandardActionGroup>
                <StandardButton action="confirm" native-type="submit" :loading="isSending">
                  {{ isSending ? '发送中...' : sendButtonText }}
                </StandardButton>
              </StandardActionGroup>
            </div>
          </section>
        </form>
      </PanelCard>

      <PanelCard class="reporting-surface reporting-surface--diagnosis">
        <template #header>
          <div class="reporting-surface__header">
            <div class="reporting-surface__heading">
              <p class="reporting-surface__eyebrow">右侧诊断复盘</p>
              <h2 class="reporting-surface__title">诊断复盘</h2>
              <p class="reporting-surface__description">
                右侧统一查看诊断摘要、实际发送内容、帧预演和最近一次响应结果。
              </p>
            </div>
            <span class="reporting-surface__badge reporting-surface__badge--accent">右侧诊断复盘</span>
          </div>
        </template>

        <section class="reporting-section">
          <StandardInlineSectionHeader
              title="诊断摘要"
              description="根据当前输入实时推导传输方式、Topic 建议、字节编码和发送就绪状态。"
          />

          <StandardInfoGrid
              :items="diagnosticSummaryItems"
              :columns="2"
          />

          <ul class="reporting-note-list">
            <li v-for="note in diagnosticNotes" :key="note" class="reporting-note-item">
              {{ note }}
            </li>
          </ul>
        </section>

        <section class="reporting-section">
          <StandardInlineSectionHeader
              :title="actualPayloadSectionTitle"
              description="发送前的最终文本或 JSON 预演，可直接复制用于链路复核。"
          >
            <template #actions>
              <StandardActionLink @click="copyActualPayloadPreview">复制</StandardActionLink>
            </template>
          </StandardInlineSectionHeader>
          <pre class="reporting-code-block">{{ actualPayloadPreviewText }}</pre>
        </section>

        <section v-if="plaintextFrame" class="reporting-section">
          <StandardInlineSectionHeader
              title="明文帧预演"
              description="仅在明文 JSON 被识别为 C.1 / C.2 / C.3 时展示。"
          >
            <template #actions>
              <StandardActionLink @click="toggleFramePanel">{{ framePanelExpanded ? '收起' : '展开' }}</StandardActionLink>
            </template>
          </StandardInlineSectionHeader>

          <div v-if="framePanelExpanded" class="reporting-frame-panel">
            <StandardInfoGrid
                :items="frameMetaItems"
                :columns="1"
            />

            <div class="reporting-frame-grid">
              <article class="reporting-code-surface">
                <h3 class="reporting-code-surface__title">十进制</h3>
                <pre class="reporting-code-block reporting-code-block--compact">
{{ plaintextFrameDecimalPreview }}</pre>
              </article>
              <article class="reporting-code-surface">
                <h3 class="reporting-code-surface__title">十六进制</h3>
                <pre class="reporting-code-block reporting-code-block--compact">
{{ plaintextFrameHexPreview }}</pre>
              </article>
            </div>
          </div>
        </section>

        <section class="reporting-section reporting-section--stretch">
          <StandardInlineSectionHeader
              title="响应"
              description="保留最近一次页面响应，便于模拟发送后的联调复盘。"
          >
            <template #actions>
              <StandardActionLink @click="copyResponse">复制</StandardActionLink>
            </template>
          </StandardInlineSectionHeader>
          <pre class="reporting-code-block reporting-code-block--response" aria-live="polite">{{ responsePreview }}</pre>
        </section>
      </PanelCard>
    </section>

    <PanelCard
        class="reporting-card reporting-card--timeline"
        eyebrow="链路验证中心"
        title="处理时间线"
        description="以 session/trace 复盘固定 Pipeline 阶段，HTTP 直接展示，MQTT 在回流绑定后展示完整处理链路。"
    >
      <div class="reporting-timeline-toolbar">
        <StandardInlineState
            :tone="messageFlowInlineTone"
            :message="messageFlowStatusMessage"
        />
        <StandardActionGroup>
          <StandardButton action="refresh" link :disabled="!messageFlowSessionId" @click="handleRefreshMessageFlow">
            刷新时间线
          </StandardButton>
          <StandardButton action="refresh" link :disabled="!messageFlowTraceId" @click="jumpToMessageTrace">
            跳转链路追踪台
          </StandardButton>
        </StandardActionGroup>
      </div>

      <StandardTraceTimeline
          :timeline="messageFlowTimeline"
          :loading="messageFlowLoading"
          :empty-title="messageFlowEmptyTitle"
          :empty-description="messageFlowEmptyDescription"
      />
    </PanelCard>

    <PanelCard
        class="reporting-card reporting-card--recent"
        eyebrow="链路验证中心"
        title="最近提交"
        description="保留最近一批 message-flow session，支持直接恢复时间线复盘，不必先手工记录 sessionId。"
    >
      <div class="reporting-recent-toolbar">
        <StandardInlineState
            tone="info"
            :message="messageFlowRecentLoading ? '正在同步最近提交...' : '点击任一 session 可恢复对应时间线；MQTT pending 会继续按窗口轮询。'"
        />
        <StandardActionGroup>
          <StandardButton action="refresh" link @click="loadRecentMessageFlows">
            刷新最近提交
          </StandardButton>
        </StandardActionGroup>
      </div>

      <div v-if="!messageFlowRecentSessions.length" class="reporting-recent-empty">
        {{ messageFlowRecentEmptyText }}
      </div>

      <div v-else class="reporting-recent-list">
        <button
            v-for="session in messageFlowRecentSessions"
            :key="session.sessionId || `${session.deviceCode}-${session.submittedAt}`"
            type="button"
            class="reporting-recent-item"
            :data-active="(session.sessionId || '') === messageFlowSessionId"
            @click="restoreRecentSession(session)"
        >
          <div class="reporting-recent-item__header">
            <strong>{{ session.sessionId || '--' }}</strong>
            <span>{{ session.transportMode || '--' }} / {{ session.status || '--' }}</span>
          </div>
          <div class="reporting-recent-item__meta">
            <span>设备 {{ session.deviceCode || '--' }}</span>
            <span>{{ formatDateTime(session.submittedAt) }}</span>
          </div>
          <div class="reporting-recent-item__meta">
            <span>{{ session.topic || '--' }}</span>
            <span>
              {{
                session.traceId
                    ? `Trace ${session.traceId}`
                    : session.correlationPending
                        ? '等待回流'
                        : session.timelineAvailable
                            ? 'Timeline 可用'
                            : 'Timeline 缺失'
              }}
            </span>
          </div>
        </button>
      </div>
    </PanelCard>

    <PanelCard
        class="reporting-card reporting-card--follow-up"
        eyebrow="链路验证中心"
        title="发送后建议检查"
        description="先确认报文进入主链路，再核对属性、日志、在线状态和后续闭环结果。"
    >
      <StandardFlowRail :items="followUpSteps" />
    </PanelCard>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

import { getDeviceByCode, reportByHttp, reportByMqtt } from '../api/iot';
import { messageApi } from '../api/message';
import PanelCard from '../components/PanelCard.vue';
import StandardActionGroup from '../components/StandardActionGroup.vue';
import StandardFlowRail from '../components/StandardFlowRail.vue';
import StandardInfoGrid from '../components/StandardInfoGrid.vue';
import StandardInlineState from '../components/StandardInlineState.vue';
import StandardInlineSectionHeader from '../components/StandardInlineSectionHeader.vue';
import StandardTraceTimeline from '../components/StandardTraceTimeline.vue';
import { recordActivity } from '../stores/activity';
import type {
  Device,
  HttpReportPayload,
  MessageFlowRecentSession,
  MessageFlowSession,
  MessageFlowTimeline,
  MqttReportPublishPayload
} from '../types/api';
import { formatDateTime, looksLikeXml, parseJsonSafely, prettyJson, prettyXml } from '../utils/format';
import { formatFrameDecimalPreview, formatFrameHexPreview } from './reportPayloadFrame';
import {
  evaluateReportWorkbenchInput,
  filterTemplatesByMode,
  type ReportMode,
  type TransportMode
} from './reportWorkbenchState';

interface TemplateOption {
  name: string;
  mode: ReportMode;
  payload: string;
  topic?: string;
  type3BinaryBase64?: string;
}

interface ReportFormState {
  deviceCode: string;
  topic: string;
  tenantId: string;
  payload: string;
}

type FeedbackTone = 'neutral' | 'info' | 'success' | 'danger';

const MESSAGE_FLOW_MATCH_WINDOW_MS = 120 * 1000;

const router = useRouter();

const createDefaultForm = (): ReportFormState => ({
  deviceCode: '',
  topic: '$dp',
  tenantId: '1',
  payload: JSON.stringify(
      {
        messageType: 'property',
        properties: {
          temperature: 26.5,
          humidity: 68
        }
      },
      null,
      2
  )
});

const templates: TemplateOption[] = [
  {
    name: '明文 C.1 属性',
    mode: 'plaintext',
    topic: '$dp',
    payload: JSON.stringify(
        {
          messageType: 'property',
          properties: {
            temperature: 26.5,
            humidity: 68
          }
        },
        null,
        2
    )
  },
  {
    name: '明文 C.2 深部位移',
    mode: 'plaintext',
    topic: '$dp',
    payload: JSON.stringify(
        {
          SK00FB0D1310195: {
            L1_SW_1: {
              '2026-03-20T08:07:22.000Z': {
                dispsX: -0.0257,
                dispsY: -0.0605
              }
            }
          }
        },
        null,
        2
    )
  },
  {
    name: '密文封包',
    mode: 'encrypted',
    topic: '$dp',
    payload: JSON.stringify(
        {
          header: {
            appId: '62000001'
          },
          bodies: {
            body: 'PTOLy04o/stDufUYFo5s3g=='
          }
        },
        null,
        2
    )
  }
];

const followUpSteps = [
  { index: '01', title: '查看设备档案', description: '确认 onlineStatus、lastReportTime 是否刷新。' },
  { index: '02', title: '核对属性快照', description: '确认最新属性是否按预期落到设备属性表。' },
  { index: '03', title: '核对消息日志', description: '确认 topic 与 payload 已进入消息日志。' },
  { index: '04', title: '继续联动排查', description: '必要时继续查看链路追踪台、异常观测台和风险闭环结果。' }
];

const INITIAL_RESPONSE = {
  tip: '查询设备后，可通过 HTTP 或 MQTT 发起模拟上报。'
};

const transportModeOptions = [
  { label: 'HTTP', value: 'http' },
  { label: 'MQTT', value: 'mqtt' }
] as const;

const reportModeOptions = [
  { label: '明文', value: 'plaintext' },
  { label: '密文', value: 'encrypted' }
] as const;

const reportForm = reactive<ReportFormState>(createDefaultForm());
const transportMode = ref<TransportMode>('http');
const reportMode = ref<ReportMode>('plaintext');
const type3BinaryBase64 = ref('');
const isSending = ref(false);
const isQueryingDevice = ref(false);
const resolvedDevice = ref<Device | null>(null);
const deviceLookupError = ref('');
const hasAttemptedSubmit = ref(false);
const lastResponse = ref<unknown>(INITIAL_RESPONSE);
const framePanelExpanded = ref(true);
const messageFlowSessionId = ref('');
const messageFlowSession = ref<MessageFlowSession | null>(null);
const messageFlowRecentSessions = ref<MessageFlowRecentSession[]>([]);
const messageFlowLoading = ref(false);
const messageFlowRecentLoading = ref(false);
const messageFlowLookupError = ref('');
const messageFlowLookupMissing = ref(false);
const messageFlowSubmittedTransportMode = ref<TransportMode | ''>('');
const messageFlowExpectedTimeline = ref(false);
let messageFlowPollTimer: number | null = null;

const filteredTemplates = computed(() => filterTemplatesByMode(templates, reportMode.value));
const resolvedProductKey = computed(() => normalizedText(resolvedDevice.value?.productKey));
const resolvedProtocolCode = computed(() => normalizedText(resolvedDevice.value?.protocolCode));
const resolvedClientId = computed(() => (resolvedDevice.value ? normalizedText(reportForm.deviceCode) : ''));

const evaluation = computed(() =>
    evaluateReportWorkbenchInput({
      report: {
        protocolCode: resolvedProtocolCode.value,
        productKey: resolvedProductKey.value,
        deviceCode: normalizedText(reportForm.deviceCode),
        topic: reportForm.topic,
        payload: reportForm.payload
      },
      mode: reportMode.value,
      transportMode: transportMode.value,
      type3BinaryBase64: type3BinaryBase64.value
    })
);

const plaintextFrame = computed(() => evaluation.value.plaintextFrame);
const validationIssues = computed(() => evaluation.value.validationIssues);
const canSend = computed(() => evaluation.value.canSend);
const showValidationBanner = computed(() => hasAttemptedSubmit.value && validationIssues.value.length > 0);

const deviceLookupTone = computed<FeedbackTone>(() => {
  if (isQueryingDevice.value) {
    return 'info';
  }
  if (deviceLookupError.value) {
    return 'danger';
  }
  if (resolvedDevice.value) {
    return 'success';
  }
  return 'neutral';
});

const deviceLookupMessage = computed(() => {
  if (isQueryingDevice.value) {
    return '正在查询设备接入契约，请稍候。';
  }
  if (deviceLookupError.value) {
    return deviceLookupError.value;
  }
  if (resolvedDevice.value) {
    return '已加载设备接入契约，可继续配置 Topic、模式与 payload。';
  }
  return '请输入设备编码后点击“查询设备”，加载产品 Key、协议编码和客户端 ID。';
});

const deviceIdentityItems = computed(() => [
  {
    key: 'product-key',
    label: '产品 Key',
    value: resolvedProductKey.value,
    fallback: '查询后自动回显'
  },
  {
    key: 'protocol-code',
    label: '协议编码',
    value: resolvedProtocolCode.value,
    fallback: '查询后自动回显'
  },
  {
    key: 'client-id',
    label: '客户端 ID',
    value: resolvedClientId.value,
    fallback: '查询后自动回显'
  },
  {
    key: 'device-name',
    label: '设备名称',
    value: normalizedText(resolvedDevice.value?.deviceName),
    fallback: '查询后自动回显',
    multiline: true
  }
]);

const payloadFormatLabel = computed(() => {
  if (evaluation.value.payloadFormat === 'json') {
    return 'JSON';
  }
  if (evaluation.value.payloadFormat === 'xml') {
    return 'XML';
  }
  return '原始文本';
});

const recognitionStatusText = computed(() => {
  if (plaintextFrame.value) {
    return `类型 ${plaintextFrame.value.type}`;
  }
  if (reportMode.value === 'encrypted') {
    return '密文透传';
  }
  return payloadFormatLabel.value;
});

const sendStatusText = computed(() => {
  if (!resolvedDevice.value) {
    return '待查询设备';
  }
  if (isSending.value) {
    return '发送中';
  }
  if (hasAttemptedSubmit.value && validationIssues.value.length > 0) {
    return '需先修复输入';
  }
  if (canSend.value) {
    return '可发送';
  }
  return '待确认输入';
});

const deviceLookupInlineTone = computed<'info' | 'error'>(() =>
    deviceLookupTone.value === 'danger' ? 'error' : 'info'
);

const reportingStripStatus = computed(() => {
  const deviceLabel = normalizedText(resolvedDevice.value?.deviceCode) || '未查询';
  const transportLabel = transportMode.value === 'mqtt' ? 'MQTT' : 'HTTP';
  return `当前设备 ${deviceLabel} · ${transportLabel} · ${sendStatusText.value}`;
});

const diagnosticSummaryItems = computed(() => {
  const plaintextType = plaintextFrame.value
      ? `类型 ${plaintextFrame.value.type}（${plaintextFrame.value.label}）`
      : reportMode.value === 'plaintext'
          ? '原始文本'
          : '--';

  return [
    {
      key: 'transport',
      label: '传输方式',
      value: transportMode.value === 'mqtt' ? 'MQTT 模拟' : 'HTTP 模拟'
    },
    {
      key: 'mode',
      label: '上报模式',
      value: reportMode.value === 'encrypted' ? '密文透传' : '明文发送'
    },
    {
      key: 'format',
      label: '内容格式',
      value: payloadFormatLabel.value
    },
    {
      key: 'recognition',
      label: '识别结果',
      value: plaintextType,
      multiline: true
    },
    {
      key: 'topic',
      label: '当前 Topic',
      value: normalizedText(reportForm.topic) || '--',
      multiline: true
    },
    {
      key: 'recommended-topic',
      label: '建议 Topic',
      value: resolvedDevice.value ? evaluation.value.recommendedTopic : '查询设备后生成',
      multiline: true
    },
    {
      key: 'encoding',
      label: '实际字节编码',
      value: evaluation.value.actualPayloadEncoding || 'UTF-8'
    },
    {
      key: 'send-status',
      label: '发送状态',
      value: sendStatusText.value
    }
  ];
});

const diagnosticNotes = computed(() => {
  const notes: string[] = [];
  if (!resolvedDevice.value) {
    notes.push('先输入设备编码并点击“查询设备”，再执行发送模拟。');
  }
  if (transportMode.value === 'mqtt') {
    notes.push('MQTT 模拟会把原始 payload 直接发布到 Broker，再由现有 consumer 回流主链路。');
  } else {
    notes.push('HTTP 模拟会直接调用 `/api/message/http/report`，并按当前租户 ID 生效。');
  }
  if (reportMode.value === 'encrypted') {
    notes.push('密文模式会直接透传 header + bodies.body 封包，不再计算类型和长度。');
  }
  notes.push(...evaluation.value.diagnosticNotes);
  return Array.from(new Set(notes));
});

const frameMetaItems = computed(() => {
  if (!plaintextFrame.value) {
    return [];
  }
  return [
    {
      key: 'type',
      label: '识别类型',
      value: `类型 ${plaintextFrame.value.type}（${plaintextFrame.value.label}）`
    },
    {
      key: 'reason',
      label: '判定依据',
      value: plaintextFrame.value.reason,
      multiline: true
    }
  ];
});

const payloadPlaceholder = computed(() =>
    reportMode.value === 'encrypted'
        ? '请输入完整的 header + bodies.body 密文封包 JSON'
        : '请输入原始文本、JSON 或 XML；只有可识别 JSON 才会构造 C.1 / C.2 / C.3 明文帧'
);

const sendButtonText = computed(() =>
    transportMode.value === 'mqtt' ? '发送 MQTT 模拟' : '发送 HTTP 模拟'
);

const actualPayloadSectionTitle = computed(() => {
  if (reportMode.value === 'encrypted') {
    return '实际发送封包';
  }
  if (evaluation.value.autoInjectedDeviceCode) {
    return '实际发送内容（已补 deviceCode）';
  }
  if (evaluation.value.actualPayloadFormat === 'xml') {
    return '实际发送 XML';
  }
  if (evaluation.value.actualPayloadFormat === 'json') {
    return '实际发送 JSON';
  }
  return '实际发送文本';
});

const actualPayloadPreviewText = computed(() => evaluation.value.actualPayloadPreview || '--');
const plaintextFrameDecimalPreview = computed(() => {
  if (!plaintextFrame.value) {
    return evaluation.value.plaintextFrameError || '--';
  }
  return formatFrameDecimalPreview(plaintextFrame.value.frameBytes);
});
const plaintextFrameHexPreview = computed(() => {
  if (!plaintextFrame.value) {
    return evaluation.value.plaintextFrameError || '--';
  }
  return formatFrameHexPreview(plaintextFrame.value.frameBytes);
});
const responsePreview = computed(() => prettyJson(lastResponse.value));
const messageFlowTimeline = computed<MessageFlowTimeline | null>(() => messageFlowSession.value?.timeline || null);
const messageFlowTraceId = computed(() =>
    normalizedText(messageFlowSession.value?.traceId) || normalizedText(messageFlowTimeline.value?.traceId)
);
const messageFlowPendingTimedOut = computed(() => {
  if (!messageFlowSession.value?.correlationPending || normalizedText(messageFlowTraceId.value)) {
    return false;
  }
  const submittedAt = parseMessageFlowSubmittedAt(messageFlowSession.value?.submittedAt);
  return submittedAt !== null && Date.now() - submittedAt >= MESSAGE_FLOW_MATCH_WINDOW_MS;
});
const messageFlowInlineTone = computed<'info' | 'error'>(() =>
    messageFlowLookupError.value || messageFlowSession.value?.status === 'FAILED' ? 'error' : 'info'
);
const messageFlowStatusMessage = computed(() => {
  if (!messageFlowSessionId.value) {
    return '发送成功后，这里会展示固定 Pipeline 的阶段状态、耗时、处理类/方法和关键摘要。';
  }
  if (messageFlowLoading.value) {
    return '正在同步最新处理时间线，请稍候。';
  }
  if (messageFlowLookupError.value) {
    return 'message-flow 存储异常/Redis 不可用。';
  }
  if (messageFlowLookupMissing.value && messageFlowSubmittedTransportMode.value === 'http' && messageFlowExpectedTimeline.value) {
    return '时间线不可用，优先排查 Redis/TTL。';
  }
  if (messageFlowSession.value?.correlationPending && !messageFlowTraceId.value) {
    if (messageFlowPendingTimedOut.value) {
      return 'MQTT 模拟已超出关联窗口，判定为未命中消费回流关联。';
    }
    return 'MQTT 模拟已发布，正在等待消费回流绑定 traceId。';
  }
  if (messageFlowSession.value?.status === 'FAILED') {
    return '本次处理链路已失败，可在下方时间线查看失败阶段与异常摘要。';
  }
  if (messageFlowTimeline.value) {
    return '处理时间线已就绪，可直接查看阶段顺序，或跳转链路追踪台继续联动排查。';
  }
  if (messageFlowLookupMissing.value) {
    return '当前 session 对应的时间线不存在或已过期。';
  }
  return '当前 session 还没有可展示的处理时间线。';
});
const messageFlowEmptyTitle = computed(() => {
  if (messageFlowLookupError.value) {
    return 'message-flow 存储异常';
  }
  if (messageFlowLookupMissing.value && messageFlowSubmittedTransportMode.value === 'http' && messageFlowExpectedTimeline.value) {
    return '时间线不可用';
  }
  if (messageFlowSession.value?.correlationPending && !messageFlowTraceId.value) {
    if (messageFlowPendingTimedOut.value) {
      return '未命中消费回流关联';
    }
    return '等待消费回流';
  }
  return '暂无处理时间线';
});
const messageFlowEmptyDescription = computed(() => {
  if (messageFlowLookupError.value) {
    return '当前 session/trace 查询失败，优先排查 Redis 可用性和后端 message-flow 存储日志。';
  }
  if (messageFlowLookupMissing.value && messageFlowSubmittedTransportMode.value === 'http' && messageFlowExpectedTimeline.value) {
    return 'HTTP 提交已返回 timelineAvailable=true，但后续 session 查询为空，优先排查 Redis TTL、key 可写性和 message-flow 存储异常。';
  }
  if (messageFlowSession.value?.correlationPending && !messageFlowTraceId.value) {
    if (messageFlowPendingTimedOut.value) {
      return 'MQTT 模拟发布已超过 120 秒匹配窗口，当前判定 fingerprint 未命中真实消费回流。';
    }
    return 'MQTT 模拟发布成功后，会在消费链路完成 decode 并命中 fingerprint 绑定后展示完整 trace timeline。';
  }
  if (messageFlowSessionId.value && !messageFlowTimeline.value) {
    return '当前 session 还没有 timeline，可能仍在处理、已过期，或尚未完成 trace 绑定。';
  }
  return '发送成功后，这里会显示阶段状态、耗时、处理类/方法和关键摘要。';
});
const messageFlowRecentEmptyText = computed(() => {
  if (messageFlowRecentLoading.value) {
    return '正在加载最近提交...';
  }
  return '最近还没有可恢复的 message-flow session。';
});

watch(
    () => reportForm.deviceCode,
    (value, oldValue) => {
      if (value === oldValue) {
        return;
      }
      resetResolvedDeviceState();
      hasAttemptedSubmit.value = false;
    }
);

onMounted(() => {
  loadRecentMessageFlows().catch(() => undefined);
  try {
    const lastTemplate = window.localStorage.getItem('reporting:lastTemplate');
    if (!lastTemplate) {
      return;
    }
    const template = JSON.parse(lastTemplate) as TemplateOption;
    if (template && typeof template === 'object') {
      applyTemplate(template);
    }
  } catch {
    // 忽略本地模板恢复失败
  }
});

function resetResolvedDeviceState() {
  resolvedDevice.value = null;
  deviceLookupError.value = '';
  resetMessageFlowState();
}

function applyTemplate(template: TemplateOption) {
  reportMode.value = template.mode;
  reportForm.payload = template.payload;
  reportForm.topic = template.topic || '$dp';
  type3BinaryBase64.value = template.type3BinaryBase64 || '';

  try {
    window.localStorage.setItem('reporting:lastTemplate', JSON.stringify(template));
  } catch {
    // 忽略本地缓存写入失败
  }

  if (resolvedDevice.value) {
    syncTopic();
  }
}

async function oneClickFill() {
  if (!normalizedText(reportForm.deviceCode)) {
    ElMessage.warning('请先输入设备编码。');
    return;
  }

  if (!resolvedDevice.value) {
    const loaded = await handleQueryDevice();
    if (!loaded) {
      return;
    }
  }

  syncTopic();

  if (resolvedProtocolCode.value && resolvedProtocolCode.value.includes('encrypt')) {
    reportMode.value = 'encrypted';
  }

  ElMessage.success('已自动补全推荐配置。');
}

function syncTopic() {
  if (!resolvedDevice.value) {
    ElMessage.warning('请先查询设备，再套用推荐 Topic。');
    return;
  }
  reportForm.topic = evaluation.value.recommendedTopic;
}

async function handleQueryDevice() {
  const deviceCode = normalizedText(reportForm.deviceCode);
  if (!deviceCode) {
    ElMessage.warning('请先输入设备编码。');
    return false;
  }

  isQueryingDevice.value = true;
  deviceLookupError.value = '';
  const queryingCode = deviceCode;

  try {
    const response = await getDeviceByCode(queryingCode);
    if (!response.data) {
      throw new Error(`未查询到设备 ${queryingCode}`);
    }

    if (normalizedText(reportForm.deviceCode) !== queryingCode) {
      return false;
    }

    resolvedDevice.value = response.data;
    ElMessage.success(`已加载设备 ${queryingCode} 的接入契约。`);
    return true;
  } catch (error) {
    if (normalizedText(reportForm.deviceCode) === queryingCode) {
      resolvedDevice.value = null;
      deviceLookupError.value = (error as Error).message;
      ElMessage.error(deviceLookupError.value);
    }
    return false;
  } finally {
    isQueryingDevice.value = false;
  }
}

function formatPayloadAsJson() {
  const payload = reportForm.payload;
  if (parseJsonSafely<unknown>(payload) === null) {
    ElMessage.warning('当前内容不是有效 JSON，无法格式化。');
    return;
  }
  reportForm.payload = prettyJson(payload);
}

function formatPayloadAsXml() {
  const payload = reportForm.payload.trim();
  if (!looksLikeXml(payload)) {
    ElMessage.warning('当前内容不是 XML 文本，无法格式化。');
    return;
  }
  reportForm.payload = prettyXml(payload);
}

async function handleSendReport() {
  hasAttemptedSubmit.value = true;

  if (!canSend.value) {
    ElMessage.warning(validationIssues.value[0]?.message || '请输入有效内容后再发送。');
    return;
  }

  isSending.value = true;
  resetMessageFlowState();
  const deviceCode = normalizedText(reportForm.deviceCode);
  const topic = normalizedText(reportForm.topic);
  const requestPayloadPreview =
      reportMode.value === 'plaintext' && plaintextFrame.value
          ? '<binary-frame>'
          : evaluation.value.actualPayloadPreview;

  try {
    let response;
    let detail = '';

    if (transportMode.value === 'http') {
      const payload: HttpReportPayload = {
        protocolCode: resolvedProtocolCode.value,
        productKey: resolvedProductKey.value,
        deviceCode,
        topic,
        clientId: deviceCode,
        tenantId: normalizedText(reportForm.tenantId) || '1',
        payload: evaluation.value.actualPayload
      };
      if (evaluation.value.actualPayloadEncoding) {
        payload.payloadEncoding = evaluation.value.actualPayloadEncoding;
      }
      response = await reportByHttp(payload);
      detail = `已通过 HTTP 向设备 ${deviceCode} 发送 ${reportMode.value === 'encrypted' ? '密文' : '明文'} 模拟报文`;
    } else {
      const payload: MqttReportPublishPayload = {
        protocolCode: resolvedProtocolCode.value,
        productKey: resolvedProductKey.value,
        deviceCode,
        topic,
        payload: evaluation.value.actualPayload
      };
      if (evaluation.value.actualPayloadEncoding) {
        payload.payloadEncoding = evaluation.value.actualPayloadEncoding;
      }
      response = await reportByMqtt(payload);
      detail = `已通过 MQTT 向设备 ${deviceCode} 发布原始上行模拟报文`;
    }

    lastResponse.value = response;
    messageFlowSubmittedTransportMode.value = transportMode.value;
    messageFlowExpectedTimeline.value = Boolean(response.data?.timelineAvailable);
    const submitSessionId = normalizedText(response.data?.sessionId);
    if (submitSessionId) {
      messageFlowSessionId.value = submitSessionId;
      await loadMessageFlowSession(submitSessionId, transportMode.value === 'mqtt');
    }
    await loadRecentMessageFlows();
    ElMessage.success(`${transportMode.value === 'mqtt' ? 'MQTT' : 'HTTP'} 模拟上报成功`);
    recordActivity({
      module: '链路验证中心',
      action: transportMode.value === 'mqtt' ? '发送 MQTT 模拟上报' : '发送 HTTP 模拟上报',
      request: {
        deviceCode,
        topic,
        transportMode: transportMode.value,
        reportMode: reportMode.value,
        payload: requestPayloadPreview
      },
      response,
      ok: true,
      detail
    });
  } catch (error) {
    const requestError = (error as Error).message;
    lastResponse.value = { ok: false, message: requestError };
    ElMessage.error(requestError);
    recordActivity({
      module: '链路验证中心',
      action: transportMode.value === 'mqtt' ? '发送 MQTT 模拟上报' : '发送 HTTP 模拟上报',
      request: {
        deviceCode,
        topic,
        transportMode: transportMode.value,
        reportMode: reportMode.value,
        payload: requestPayloadPreview
      },
      response: { message: requestError },
      ok: false,
      detail: `发送失败：${requestError}`
    });
  } finally {
    isSending.value = false;
  }
}

function resetMessageFlowState() {
  clearMessageFlowTimer();
  messageFlowSessionId.value = '';
  messageFlowSession.value = null;
  messageFlowLookupError.value = '';
  messageFlowLookupMissing.value = false;
  messageFlowSubmittedTransportMode.value = '';
  messageFlowExpectedTimeline.value = false;
}

function clearMessageFlowTimer() {
  if (messageFlowPollTimer !== null) {
    window.clearTimeout(messageFlowPollTimer);
    messageFlowPollTimer = null;
  }
}

function scheduleMessageFlowPoll(sessionId: string) {
  clearMessageFlowTimer();
  messageFlowPollTimer = window.setTimeout(() => {
    loadMessageFlowSession(sessionId, true).catch(() => undefined);
  }, 1500);
}

async function loadMessageFlowSession(sessionId: string, allowPolling = false) {
  if (!sessionId) {
    return;
  }
  messageFlowLoading.value = true;
  messageFlowLookupError.value = '';
  messageFlowLookupMissing.value = false;
  try {
    const response = await messageApi.getMessageFlowSession(sessionId);
    messageFlowSession.value = response.data || null;
    messageFlowLookupMissing.value = !response.data;
    if (response.data?.transportMode && isTransportMode(response.data.transportMode)) {
      messageFlowSubmittedTransportMode.value = response.data.transportMode.toLowerCase() as TransportMode;
    }
    if (allowPolling && shouldContinueMessageFlowPolling(response.data || null)) {
      scheduleMessageFlowPoll(sessionId);
    } else {
      clearMessageFlowTimer();
    }
  } catch (error) {
    clearMessageFlowTimer();
    messageFlowSession.value = null;
    messageFlowLookupError.value = error instanceof Error ? error.message : '获取处理时间线失败';
    ElMessage.error(error instanceof Error ? error.message : '获取处理时间线失败');
  } finally {
    messageFlowLoading.value = false;
  }
}

async function handleRefreshMessageFlow() {
  if (!messageFlowSessionId.value) {
    return;
  }
  await loadMessageFlowSession(
      messageFlowSessionId.value,
      Boolean(messageFlowSession.value?.correlationPending && !messageFlowTraceId.value)
  );
}

function jumpToMessageTrace() {
  if (!messageFlowTraceId.value) {
    return;
  }
  router.push({
    path: '/message-trace',
    query: {
      traceId: messageFlowTraceId.value
    }
  });
}

function handleOpenTraceWorkbench() {
  router.push({
    path: '/message-trace'
  });
}

async function loadRecentMessageFlows() {
  messageFlowRecentLoading.value = true;
  try {
    const response = await messageApi.getMessageFlowRecentSessions({ size: 6 });
    messageFlowRecentSessions.value = response.data || [];
  } catch {
    messageFlowRecentSessions.value = [];
  } finally {
    messageFlowRecentLoading.value = false;
  }
}

async function restoreRecentSession(session: MessageFlowRecentSession) {
  const sessionId = normalizedText(session.sessionId);
  if (!sessionId) {
    return;
  }
  messageFlowSessionId.value = sessionId;
  messageFlowSubmittedTransportMode.value = isTransportMode(session.transportMode)
      ? session.transportMode.toLowerCase() as TransportMode
      : '';
  messageFlowExpectedTimeline.value = Boolean(session.timelineAvailable)
      || messageFlowSubmittedTransportMode.value === 'http';
  await loadMessageFlowSession(
      sessionId,
      messageFlowSubmittedTransportMode.value === 'mqtt'
          && Boolean(session.correlationPending)
          && !normalizedText(session.traceId)
  );
}

function toggleFramePanel() {
  framePanelExpanded.value = !framePanelExpanded.value;
}

async function copyActualPayloadPreview() {
  await copyText(actualPayloadPreviewText.value, '已复制实际发送内容。');
}

async function copyResponse() {
  await copyText(responsePreview.value, '已复制最近一次响应。');
}

async function copyText(value: string, successMessage: string) {
  if (!navigator.clipboard) {
    ElMessage.warning('当前浏览器环境不支持剪贴板复制。');
    return;
  }
  await navigator.clipboard.writeText(value);
  ElMessage.success(successMessage);
}

function normalizedText(value: unknown): string {
  return typeof value === 'string' ? value.trim() : '';
}

function parseMessageFlowSubmittedAt(value?: string | null): number | null {
  const normalized = normalizedText(value);
  if (!normalized) {
    return null;
  }
  const parsed = Date.parse(normalized.includes('T') ? normalized : normalized.replace(' ', 'T'));
  return Number.isNaN(parsed) ? null : parsed;
}

function shouldContinueMessageFlowPolling(session: MessageFlowSession | null) {
  if (!session?.correlationPending || normalizedText(session.traceId)) {
    return false;
  }
  const submittedAt = parseMessageFlowSubmittedAt(session.submittedAt);
  if (submittedAt === null) {
    return true;
  }
  return Date.now() - submittedAt < MESSAGE_FLOW_MATCH_WINDOW_MS;
}

function isTransportMode(value: unknown): value is TransportMode {
  const normalized = normalizedText(value);
  return normalized === 'HTTP' || normalized === 'MQTT' || normalized === 'http' || normalized === 'mqtt';
}

onBeforeUnmount(() => {
  clearMessageFlowTimer();
});
</script>

<style scoped>
.reporting-view {
  min-width: 0;
}

.reporting-command-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.2rem 0 0.35rem;
}

.reporting-command-strip__copy {
  min-width: 0;
}

.reporting-command-strip__title {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.25rem;
}

.reporting-command-strip__judgement {
  margin: 0.38rem 0 0;
  color: var(--text-heading);
  font-weight: 600;
}

.reporting-command-strip__meta {
  margin: 0.3rem 0 0;
  color: var(--text-caption);
  line-height: 1.6;
}

.reporting-command-strip__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.65rem;
}

.reporting-main-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.14fr) minmax(0, 0.96fr);
  gap: var(--spacing-md);
  align-items: stretch;
  min-width: 0;
}

.reporting-surface {
  height: 100%;
  min-width: 0;
}

.reporting-surface :deep(.el-card__body) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.reporting-surface__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  width: 100%;
}

.reporting-surface__heading {
  min-width: 0;
}

.reporting-surface__eyebrow {
  margin: 0 0 0.28rem;
  color: var(--brand);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.reporting-surface__title {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.08rem;
}

.reporting-surface__description {
  margin: 0.38rem 0 0;
  color: var(--text-caption);
  line-height: 1.6;
  font-size: 0.86rem;
}

.reporting-surface__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2rem;
  padding: 0 0.78rem;
  border-radius: var(--radius-pill);
  border: 1px solid color-mix(in srgb, var(--brand) 18%, var(--panel-border));
  background: color-mix(in srgb, var(--brand) 8%, white);
  color: var(--brand-deep);
  font-size: 0.76rem;
  font-weight: 700;
  white-space: nowrap;
}

.reporting-surface__badge--accent {
  border-color: color-mix(in srgb, var(--accent) 18%, var(--panel-border));
  background: color-mix(in srgb, var(--accent) 8%, white);
  color: var(--accent-deep);
}

.reporting-compose-form {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.reporting-section {
  display: grid;
  gap: 0.8rem;
  min-width: 0;
}

.reporting-section + .reporting-section {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--line-soft);
}

.reporting-section--submit {
  margin-top: auto;
}

.reporting-section--stretch {
  flex: 1;
}

.reporting-query-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.75rem;
}

.reporting-control-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

.reporting-control-card {
  display: grid;
  gap: 0.72rem;
  padding: 0.95rem 1rem;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-lg);
  background: linear-gradient(180deg, var(--surface-subtle), rgba(255, 255, 255, 0.96));
}

.reporting-control-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.reporting-control-card__title {
  color: var(--text-heading);
  font-size: 0.86rem;
  font-weight: 700;
}

.reporting-control-card__helper {
  color: var(--text-tertiary);
  font-size: 0.78rem;
}

.reporting-topic-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.75rem;
}

.reporting-quick-fill {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  padding: 0.8rem 0.9rem;
  border-radius: var(--radius-lg);
  border: 1px dashed color-mix(in srgb, var(--brand) 24%, var(--panel-border));
  background: color-mix(in srgb, var(--brand) 4%, white);
}

.reporting-quick-fill__hint {
  color: var(--text-caption);
  font-size: 0.85rem;
  line-height: 1.6;
}

.reporting-format-chip {
  display: inline-flex;
  align-items: center;
  min-height: 1.9rem;
  padding: 0 0.72rem;
  border-radius: var(--radius-pill);
  border: 1px solid color-mix(in srgb, var(--accent) 16%, var(--panel-border));
  background: color-mix(in srgb, var(--accent) 7%, white);
  color: var(--accent-deep);
  font-size: 0.78rem;
  font-weight: 700;
}

.reporting-toolbar {
  display: grid;
  gap: 0.72rem;
}

.reporting-toolbar__group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.72rem;
  padding: 0.8rem 0.9rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line-soft);
  background: var(--surface-subtle);
}

.reporting-toolbar__label {
  color: var(--text-secondary);
  font-size: 0.82rem;
  font-weight: 700;
  white-space: nowrap;
}

.reporting-textarea :deep(.el-textarea__inner) {
  min-height: 17rem;
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border-color: var(--panel-border);
  background: rgba(255, 255, 255, 0.98);
  color: var(--text-heading);
  font-family: var(--font-mono);
  font-size: 0.84rem;
  line-height: 1.65;
  box-shadow: var(--shadow-input-inset-soft);
}

.reporting-textarea :deep(.el-textarea__inner:focus) {
  border-color: color-mix(in srgb, var(--brand) 38%, var(--panel-border));
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--brand) 18%, transparent);
}

.reporting-type3-panel {
  display: grid;
  gap: 0.72rem;
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line-soft);
  background: var(--surface-subtle);
}

.reporting-validation-banner {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--danger) 22%, var(--panel-border));
  background: linear-gradient(180deg, color-mix(in srgb, var(--danger) 6%, white), rgba(255, 255, 255, 0.98));
}

.reporting-validation-banner strong {
  display: block;
  color: color-mix(in srgb, var(--danger) 78%, var(--text-primary));
  font-size: 0.85rem;
}

.reporting-validation-list {
  margin: 0.45rem 0 0;
  padding-left: 1rem;
  color: color-mix(in srgb, var(--danger) 74%, var(--text-secondary));
  font-size: 0.84rem;
  line-height: 1.6;
}

.reporting-submit-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
}

.reporting-submit-copy {
  min-width: 0;
}

.reporting-submit-label {
  display: block;
  color: var(--text-tertiary);
  font-size: 0.76rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.reporting-submit-copy strong {
  display: block;
  margin-top: 0.28rem;
  color: var(--text-heading);
  font-size: 1rem;
}

.reporting-submit-copy p {
  margin: 0.42rem 0 0;
  color: var(--text-caption);
  font-size: 0.84rem;
  line-height: 1.6;
}

.reporting-timeline-toolbar {
  display: grid;
  gap: 0.8rem;
  margin-bottom: 1rem;
}

.reporting-recent-toolbar {
  display: grid;
  gap: 0.8rem;
  margin-bottom: 1rem;
}

.reporting-recent-empty {
  padding: 1rem 1.1rem;
  border-radius: var(--radius-lg);
  border: 1px dashed var(--line-soft);
  background: var(--surface-subtle);
  color: var(--text-secondary);
  font-size: 0.84rem;
}

.reporting-recent-list {
  display: grid;
  gap: 0.8rem;
}

.reporting-recent-item {
  display: grid;
  gap: 0.42rem;
  width: 100%;
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(247, 249, 252, 0.98));
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.reporting-recent-item:hover {
  border-color: color-mix(in srgb, var(--brand) 22%, var(--line-soft));
  transform: translateY(-1px);
  box-shadow: var(--shadow-report-card-hover);
}

.reporting-recent-item[data-active='true'] {
  border-color: color-mix(in srgb, var(--brand) 28%, var(--panel-border));
  background: color-mix(in srgb, var(--brand) 7%, white);
}

.reporting-recent-item__header,
.reporting-recent-item__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.7rem;
}

.reporting-recent-item__header strong {
  color: var(--text-heading);
  font-size: 0.88rem;
}

.reporting-recent-item__header span,
.reporting-recent-item__meta span {
  color: var(--text-secondary);
  font-size: 0.8rem;
  line-height: 1.6;
}

.reporting-note-list {
  display: grid;
  gap: 0.55rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.reporting-note-item {
  position: relative;
  padding-left: 1rem;
  color: var(--text-secondary);
  font-size: 0.84rem;
  line-height: 1.6;
}

.reporting-note-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.56rem;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--brand) 72%, white);
}

.reporting-frame-panel {
  display: grid;
  gap: 0.8rem;
}

.reporting-frame-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

.reporting-code-surface {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--surface-subtle);
}

.reporting-code-surface__title {
  margin: 0;
  padding: 0.75rem 0.9rem;
  border-bottom: 1px solid var(--line-soft);
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.reporting-code-block {
  flex: 1;
  min-height: 12.5rem;
  margin: 0;
  padding: 1rem 1.05rem;
  overflow: auto;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(247, 249, 252, 0.98));
  color: var(--text-heading);
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--font-mono);
  font-size: 0.83rem;
  line-height: 1.7;
}

.reporting-code-block--compact {
  min-height: 9.5rem;
  border: none;
  border-radius: 0;
  background: transparent;
}

.reporting-code-block--response {
  height: 100%;
  min-height: 16rem;
}

.reporting-card--follow-up {
  min-width: 0;
}

@media (max-width: 1280px) {
  .reporting-command-strip {
    flex-direction: column;
    align-items: stretch;
  }

  .reporting-command-strip__actions {
    justify-content: flex-start;
  }

  .reporting-main-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .reporting-control-grid,
  .reporting-frame-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .reporting-surface__header,
  .reporting-submit-row {
    flex-direction: column;
    align-items: stretch;
  }

  .reporting-query-row,
  .reporting-topic-row {
    grid-template-columns: 1fr;
  }

  .reporting-toolbar__group,
  .reporting-quick-fill {
    align-items: flex-start;
  }

  .reporting-recent-item__header,
  .reporting-recent-item__meta {
    flex-direction: column;
    align-items: flex-start;
  }

}
</style>
