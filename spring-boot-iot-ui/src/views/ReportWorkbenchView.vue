<template>
  <div class="page-stack">
    <section class="two-column-grid report-workbench-grid">
      <PanelCard
        class="report-card"
        eyebrow="HTTP Simulator"
        title="模拟设备上报"
        description="左侧聚焦输入与发送；发送前先阻断格式错误，避免无效请求进入后端。"
      >
        <StandardActionGroup margin-bottom="sm" gap="sm">
          <el-button
            v-for="template in filteredTemplates"
            :key="template.name"
            class="secondary-button report-template-btn"
            @click="applyTemplate(template)"
          >
            {{ template.name }}
          </el-button>
        </StandardActionGroup>

        <form class="form-grid" @submit.prevent="handleSendReport">
          <div class="field-group" style="grid-column: 1 / -1;">
            <label for="report-mode">上报模式</label>
            <el-radio-group id="report-mode" v-model="reportMode" class="report-mode-group">
              <el-radio-button label="plaintext">
                明文上报
              </el-radio-button>
              <el-radio-button label="encrypted">
                密文上报
              </el-radio-button>
            </el-radio-group>
          </div>
          <div class="field-group" style="grid-column: 1 / -1;">
            <div class="empty-state report-mode-tip">
              {{ reportModeHint }}
            </div>
          </div>
          <div class="field-group">
            <label for="report-protocol">协议编码</label>
            <el-input
              id="report-protocol"
              v-model="reportForm.protocolCode"
              name="report_protocol_code"
              placeholder="例如 mqtt-json..."
              clearable
            />
          </div>
          <div class="field-group">
            <label for="report-product-key">产品 Key</label>
            <el-input
              id="report-product-key"
              v-model="reportForm.productKey"
              name="report_product_key"
              placeholder="例如 demo-product..."
              clearable
            />
          </div>
          <div class="field-group">
            <label for="report-device-code">设备编码</label>
            <el-input
              id="report-device-code"
              v-model="reportForm.deviceCode"
              name="report_device_code"
              placeholder="例如 demo-device-01..."
              clearable
            />
          </div>
          <div class="field-group">
            <label for="report-client-id">客户端 ID</label>
            <el-input id="report-client-id" v-model="reportForm.clientId" name="report_client_id" autocomplete="off" spellcheck="false" clearable />
          </div>
          <div class="field-group">
            <label for="report-tenant">租户 ID</label>
            <el-input id="report-tenant" v-model="reportForm.tenantId" name="report_tenant_id" inputmode="numeric" placeholder="例如 1..." clearable />
          </div>
          <div class="field-group">
            <label for="report-topic">Topic</label>
            <el-input id="report-topic" v-model="reportForm.topic" name="report_topic" autocomplete="off" spellcheck="false" placeholder="例如 /sys/demo-product/demo-device-01/thing/property/post..." clearable />
          </div>
          <div class="field-group" style="grid-column: 1 / -1;">
            <label for="payload">{{ payloadLabel }}</label>
            <el-input id="payload" v-model="reportForm.payload" name="report_payload" type="textarea" :rows="9" spellcheck="false" />
          </div>
          <div v-if="reportMode === 'plaintext' && plaintextFrame?.type === 3" class="field-group" style="grid-column: 1 / -1;">
            <label for="report-type3-binary">类型 3 文件流 Base64（可选）</label>
            <el-input
              id="report-type3-binary"
              v-model="type3BinaryBase64"
              name="report_type3_binary_base64"
              type="textarea"
              :rows="3"
              spellcheck="false"
              placeholder="若 C.3 需要携带文件流，可粘贴 Base64。"
            />
          </div>

          <div v-if="validationIssues.length" class="field-group" style="grid-column: 1 / -1;">
            <div class="empty-state report-validation" aria-live="polite">
              <p class="report-validation__title">发送前请先修复以下问题：</p>
              <ul class="report-validation__list">
                <li v-for="issue in validationIssues" :key="`${issue.field}:${issue.message}`">
                  {{ issue.message }}
                </li>
              </ul>
            </div>
          </div>

          <StandardActionGroup full-width>
            <el-button class="primary-button" type="primary" native-type="submit" :loading="isSending" :disabled="!canSend || isSending">
              {{ isSending ? '发送中...' : '发送上报' }}
            </el-button>
            <el-button class="secondary-button" @click="syncTopic">
              用推荐 Topic 覆盖
            </el-button>
          </StandardActionGroup>
        </form>
      </PanelCard>

      <div class="report-right-stack">
        <PanelCard
          class="report-card"
          eyebrow="Diagnostics"
          title="诊断与预演"
          description="默认只展示关键结论；详细帧和预览按需展开。"
        >
          <StandardInfoGrid :items="diagnosticSummaryItems" />

          <el-collapse v-model="diagnosticCollapseNames" class="report-diagnostics-collapse">
            <el-collapse-item v-if="reportMode === 'plaintext' && plaintextFrame" name="frame" title="明文帧详情（十进制/十六进制）">
              <p class="report-preview__line">判定依据：{{ plaintextFrame.reason }}</p>
              <p class="report-preview__line">十进制帧预览：</p>
              <pre class="report-preview__code">{{ plaintextFrameDecimalPreview }}</pre>
              <p class="report-preview__line">十六进制帧预览：</p>
              <pre class="report-preview__code">{{ plaintextFrameHexPreview }}</pre>
            </el-collapse-item>
            <el-collapse-item v-if="normalizedJsonPreview" name="json" title="归一化 JSON 预览">
              <pre class="report-preview__code">{{ normalizedJsonPreview }}</pre>
            </el-collapse-item>
            <el-collapse-item name="curl" title="curl 预览">
              <pre class="report-preview__code">{{ curlPreview }}</pre>
            </el-collapse-item>
          </el-collapse>
        </PanelCard>

        <ResponsePanel
          eyebrow="Response"
          title="最后一次响应"
          description="请求异常只在这里展示，避免与输入校验混在一起。"
          :body="lastResponse"
        />
      </div>
    </section>

    <PanelCard
      class="report-card"
      eyebrow="Flow Reminder"
      title="发送后建议检查"
      description="先确认上报进入主链路，再核对设备属性、日志和在线状态。"
    >
      <StandardFlowRail :items="followUpSteps" />
    </PanelCard>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';

import { reportByHttp } from '../api/iot';
import StandardActionGroup from '../components/StandardActionGroup.vue';
import StandardFlowRail from '../components/StandardFlowRail.vue';
import PanelCard from '../components/PanelCard.vue';
import ResponsePanel from '../components/ResponsePanel.vue';
import StandardInfoGrid from '../components/StandardInfoGrid.vue';
import { recordActivity } from '../stores/activity';
import type { HttpReportPayload } from '../types/api';
import { formatFrameDecimalPreview, formatFrameHexPreview } from './reportPayloadFrame';
import {
  evaluateReportWorkbenchInput,
  filterTemplatesByMode,
  type ReportMode
} from './reportWorkbenchState';

interface TemplateOption {
  name: string;
  mode: ReportMode;
  payload: string;
  topic?: string;
  type3BinaryBase64?: string;
}

const createDemoReport = (): HttpReportPayload => ({
  protocolCode: 'mqtt-json',
  productKey: 'demo-product',
  deviceCode: 'demo-device-01',
  payload: JSON.stringify({
    messageType: 'property',
    properties: {
      temperature: 26.5,
      humidity: 68
    }
  }, null, 2),
  topic: '/sys/demo-product/demo-device-01/thing/property/post',
  clientId: 'demo-device-01',
  tenantId: '1'
});

const templates: TemplateOption[] = [
  {
    name: '明文 C.1 属性',
    mode: 'plaintext',
    topic: '/sys/demo-product/demo-device-01/thing/property/post',
    payload: JSON.stringify({
      messageType: 'property',
      properties: {
        temperature: 26.5,
        humidity: 68
      }
    }, null, 2)
  },
  {
    name: '明文 C.2 深部位移',
    mode: 'plaintext',
    topic: '$dp',
    payload: JSON.stringify({
      SK00FB0D1310195: {
        L1_SW_1: {
          '2026-03-20T08:07:22.000Z': {
            dispsX: -0.0257,
            dispsY: -0.0605
          }
        }
      }
    }, null, 2)
  },
  {
    name: '密文封包',
    mode: 'encrypted',
    topic: '$dp',
    payload: JSON.stringify({
      header: {
        appId: '62000001'
      },
      bodies: {
        body: 'PTOLy04o/stDufUYFo5s3g=='
      }
    }, null, 2)
  }
];

const followUpSteps = [
  { index: '01', title: '查询设备详情', description: '确认 onlineStatus、lastReportTime 是否变化。' },
  { index: '02', title: '查询属性快照', description: '确认最新属性是否按预期入库。' },
  { index: '03', title: '查询消息日志', description: '确认 topic 与 payload 已保留。' },
  { index: '04', title: '联动后续能力', description: '后续可继续做风险判定、报告和预案触发闭环。' }
];

const reportForm = reactive<HttpReportPayload>(createDemoReport());
const reportMode = ref<ReportMode>('plaintext');
const type3BinaryBase64 = ref('');
const isSending = ref(false);
const lastResponse = ref<unknown>({ tip: '发送上报后，这里会出现统一响应体。' });
const diagnosticCollapseNames = ref<string[]>([]);

const filteredTemplates = computed(() => filterTemplatesByMode(templates, reportMode.value));

const evaluation = computed(() => evaluateReportWorkbenchInput({
  report: reportForm,
  mode: reportMode.value,
  type3BinaryBase64: type3BinaryBase64.value
}));

const plaintextFrame = computed(() => evaluation.value.plaintextFrame);
const plaintextFrameError = computed(() => evaluation.value.plaintextFrameError);
const parsedPayload = computed(() => evaluation.value.parsedPayload);
const recommendedTopic = computed(() => evaluation.value.recommendedTopic);
const validationIssues = computed(() => evaluation.value.validationIssues);
const canSend = computed(() => evaluation.value.canSend);

const reportModeHint = computed(() => {
  if (reportMode.value === 'encrypted') {
    return '密文模式只透传封包，要求 payload 为完整的 header + bodies.body JSON。';
  }
  return '明文模式会自动识别 C.1/C.2/C.3，并计算 Byte2~Byte3 大端长度。';
});

const payloadLabel = computed(() => (reportMode.value === 'encrypted' ? '密文封包 JSON' : '明文 JSON'));

const normalizedJsonPreview = computed(() => {
  if (!parsedPayload.value) {
    return '';
  }
  return JSON.stringify(parsedPayload.value, null, 2);
});

const diagnosticSummaryItems = computed(() => {
  const modeLabel = reportMode.value === 'encrypted' ? '密文（封包透传）' : '明文（自动构造帧）';
  const detectedType = plaintextFrame.value ? `类型 ${plaintextFrame.value.type}（${plaintextFrame.value.label}）` : '--';
  const lengthLabel = plaintextFrame.value
    ? `${plaintextFrame.value.jsonLength}（0x${plaintextFrame.value.lengthHighByte.toString(16).toUpperCase().padStart(2, '0')} ${plaintextFrame.value.lengthLowByte.toString(16).toUpperCase().padStart(2, '0')}）`
    : '--';

  return [
    {
      key: 'mode',
      label: '上报模式',
      value: modeLabel
    },
    {
      key: 'detected-type',
      label: '识别类型',
      value: reportMode.value === 'plaintext' ? detectedType : '--'
    },
    {
      key: 'json-length',
      label: 'Byte2~Byte3 长度',
      value: reportMode.value === 'plaintext' ? lengthLabel : '--'
    },
    {
      key: 'recommended-topic',
      label: '推荐 Topic',
      value: recommendedTopic.value
    },
    {
      key: 'send-status',
      label: '可发送状态',
      value: canSend.value ? '可发送' : '需修复输入问题'
    },
    {
      key: 'diagnostic-reason',
      label: '判定依据',
      value: reportMode.value === 'plaintext' ? (plaintextFrame.value?.reason || plaintextFrameError.value || '--') : '密文模式不做类型判定'
    }
  ];
});

const plaintextFrameDecimalPreview = computed(() => {
  if (!plaintextFrame.value) {
    return plaintextFrameError.value || '--';
  }
  return formatFrameDecimalPreview(plaintextFrame.value.frameBytes);
});

const plaintextFrameHexPreview = computed(() => {
  if (!plaintextFrame.value) {
    return plaintextFrameError.value || '--';
  }
  return formatFrameHexPreview(plaintextFrame.value.frameBytes);
});

const curlPreview = computed(() => {
  const topic = reportForm.topic || recommendedTopic.value;
  if (reportMode.value === 'plaintext') {
    const body = JSON.stringify(
      {
        ...reportForm,
        topic,
        payload: '<自动构造明文二进制帧>',
        payloadEncoding: 'ISO-8859-1'
      },
      null,
      2
    );
    return `curl -X POST http://localhost:9999/api/message/http/report \\\n  -H "Content-Type: application/json" \\\n  -d '${body}'`;
  }

  const body = JSON.stringify(
    {
      ...reportForm,
      topic
    },
    null,
    2
  );

  return `curl -X POST http://localhost:9999/api/message/http/report \\\n  -H "Content-Type: application/json" \\\n  -d '${body}'`;
});

function applyTemplate(template: TemplateOption) {
  reportMode.value = template.mode;
  reportForm.payload = template.payload;
  type3BinaryBase64.value = template.type3BinaryBase64 || '';
  reportForm.topic = template.topic || recommendedTopic.value;
}

function syncTopic() {
  reportForm.topic = recommendedTopic.value;
}

async function handleSendReport() {
  if (!canSend.value) {
    ElMessage.warning(validationIssues.value[0]?.message || '请输入有效 payload。');
    return;
  }

  isSending.value = true;

  const requestPayload: HttpReportPayload = {
    ...reportForm,
    topic: reportForm.topic || recommendedTopic.value
  };

  let successDetail = '';
  if (reportMode.value === 'plaintext') {
    if (!plaintextFrame.value) {
      ElMessage.warning(plaintextFrameError.value || '明文 payload 无法构造标准帧。');
      isSending.value = false;
      return;
    }
    requestPayload.payload = plaintextFrame.value.framedPayload;
    requestPayload.payloadEncoding = 'ISO-8859-1';
    successDetail = `已向设备 ${reportForm.deviceCode} 发送明文类型 ${plaintextFrame.value.type} 报文`;
  } else {
    requestPayload.payload = reportForm.payload;
    successDetail = `已向设备 ${reportForm.deviceCode} 发送密文封包`;
  }

  try {
    const response = await reportByHttp(requestPayload);
    lastResponse.value = response;
    ElMessage.success(`设备 ${reportForm.deviceCode} 模拟上报成功`);
    recordActivity({
      module: '链路验证中心',
      action: '发送模拟上报',
      request: {
        ...requestPayload,
        payload: reportMode.value === 'plaintext' ? '<binary-frame>' : reportForm.payload
      },
      response,
      ok: true,
      detail: successDetail
    });
  } catch (error) {
    const requestError = (error as Error).message;
    lastResponse.value = { ok: false, message: requestError };
    ElMessage.error(requestError);
    recordActivity({
      module: '链路验证中心',
      action: '发送模拟上报',
      request: {
        ...requestPayload,
        payload: reportMode.value === 'plaintext' ? '<binary-frame>' : reportForm.payload
      },
      response: { message: requestError },
      ok: false,
      detail: `发送失败：${requestError}`
    });
  } finally {
    isSending.value = false;
  }
}
</script>

<style scoped>
.report-workbench-grid {
  align-items: start;
}

.report-right-stack {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.report-card {
  position: relative;
  overflow: hidden;
}

.report-card::after {
  content: '';
  position: absolute;
  inset: -8rem -7rem auto auto;
  width: 16rem;
  height: 16rem;
  background: radial-gradient(circle, color-mix(in srgb, var(--brand) 12%, transparent), transparent 65%);
  pointer-events: none;
}

.report-template-btn {
  border-radius: 999px;
  border: 1px solid var(--panel-border);
  background: linear-gradient(130deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand) 4%, white));
  color: var(--text-secondary);
}

.report-template-btn:hover {
  border-color: var(--panel-border-hover);
  color: var(--brand-deep);
}

.report-mode-group {
  width: 100%;
}

.report-mode-tip {
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--brand) 4%, white);
  color: var(--text-secondary);
}

.report-validation {
  border: 1px solid color-mix(in srgb, var(--el-color-danger) 35%, transparent);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--el-color-danger) 10%, white);
  color: var(--el-color-danger);
}

.report-validation__title {
  margin: 0;
  font-weight: 600;
}

.report-validation__list {
  margin: 0.5rem 0 0;
  padding-left: 1.2rem;
}

.report-diagnostics-collapse {
  margin-top: 1rem;
}

.report-preview__line {
  margin: 0.4rem 0;
  color: var(--text-secondary);
}

.report-preview__code {
  margin: 0.75rem 0 0;
  padding: 0.85rem 0.95rem;
  border-radius: var(--radius-sm);
  border: 1px solid color-mix(in srgb, var(--brand) 18%, transparent);
  background: color-mix(in srgb, var(--brand) 6%, white);
  white-space: pre-wrap;
  color: var(--text-secondary);
}
</style>
