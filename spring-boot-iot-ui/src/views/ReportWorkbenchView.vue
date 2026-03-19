<template>
  <div class="page-stack">
    <section class="two-column-grid">
      <PanelCard
        class="report-card"
        eyebrow="HTTP Simulator"
        title="模拟设备上报"
        description="这里直接对应 `POST /message/http/report`，是当前 Phase 1 最关键的联调入口。"
      >
        <StandardActionGroup margin-bottom="sm" gap="sm">
          <el-button
            v-for="template in templates"
            :key="template.name"
            class="secondary-button report-template-btn"
            @click="applyTemplate(template)"
          >
            {{ template.name }}
          </el-button>
        </StandardActionGroup>

        <form class="form-grid" @submit.prevent="handleSendReport">
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
            <label for="payload">Payload</label>
            <el-input id="payload" v-model="reportForm.payload" name="report_payload" type="textarea" :rows="9" spellcheck="false" />
          </div>
          <StandardActionGroup full-width>
            <el-button class="primary-button" type="primary" native-type="submit" :loading="isSending">
              {{ isSending ? '发送中...' : '发送上报' }}
            </el-button>
            <el-button class="secondary-button" @click="syncTopic">
              用推荐 Topic 覆盖
            </el-button>
          </StandardActionGroup>
        </form>
      </PanelCard>

      <PanelCard
        class="report-card"
        eyebrow="Protocol Preview"
        title="报文预演"
        description="在真正调用接口前，先看 topic、messageType 和 curl 命令是否合理。"
      >
        <StandardInfoGrid :items="previewInfoItems" />
        <div class="empty-state report-preview" style="margin-top: 1rem;">
          当前 curl 预览：
          <pre class="report-preview__code">{{ curlPreview }}</pre>
        </div>
      </PanelCard>
    </section>

    <div v-if="errorMessage" class="empty-state" aria-live="polite">{{ errorMessage }}</div>

    <section class="two-column-grid">
      <ResponsePanel
        eyebrow="Parsed Payload"
        title="Payload 解析预览"
        description="便于确认 messageType、属性结构与 JSON 字符串转义是否正确。"
        :body="parsedPayload || { warning: '当前 payload 不是有效 JSON。' }"
      />
      <ResponsePanel
        eyebrow="Response"
        title="最后一次响应"
        description="请求成功后，可继续到“设备洞察”页面查看属性和消息日志。"
        :body="lastResponse"
      />
    </section>

    <PanelCard
      class="report-card"
      eyebrow="Flow Reminder"
      title="发送后建议检查"
      description="按照文档推荐的 Phase 1 验证顺序，确认报文已进入主链路并更新设备状态。"
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
import { parseJsonSafely, prettyJson } from '../utils/format';

interface TemplateOption {
  name: string;
  payload: Record<string, unknown>;
}

const createDemoReport = (): HttpReportPayload => ({
  protocolCode: 'mqtt-json',
  productKey: 'demo-product',
  deviceCode: 'demo-device-01',
  payload: prettyJson({
    messageType: 'property',
    properties: {
      temperature: 26.5,
      humidity: 68
    }
  }),
  topic: '/sys/demo-product/demo-device-01/thing/property/post',
  clientId: 'demo-device-01',
  tenantId: '1'
});

const templates: TemplateOption[] = [
  {
    name: '温湿度属性',
    payload: {
      messageType: 'property',
      properties: {
        temperature: 26.5,
        humidity: 68
      }
    }
  },
  {
    name: '设备状态',
    payload: {
      messageType: 'status',
      properties: {
        voltage: 3.54,
        signal4g: -51,
        batteryDumpEnergy: 1
      }
    }
  },
  {
    name: '事件占位',
    payload: {
      messageType: 'event',
      eventCode: 'overheat',
      properties: {
        temperature: 88.8
      }
    }
  }
];

const followUpSteps = [
  { index: '01', title: '查询设备详情', description: '确认 onlineStatus、lastReportTime 是否变化。' },
  { index: '02', title: '查询属性快照', description: '确认 `temperature` / `humidity` 等属性已写入。' },
  { index: '03', title: '查询消息日志', description: '确认 topic 与 payload 已保留。' },
  { index: '04', title: '衔接未来图表', description: '这些属性将直接成为后续图表与数字孪生的数据源。' }
];

const reportForm = reactive<HttpReportPayload>(createDemoReport());
const isSending = ref(false);
const errorMessage = ref('');
const lastResponse = ref<unknown>({ tip: '发送上报后，这里会出现统一响应体。' });

const parsedPayload = computed(() => parseJsonSafely<Record<string, unknown>>(reportForm.payload));
const inferredMessageType = computed(() => String(parsedPayload.value?.messageType || 'property'));
const recommendedTopic = computed(() => {
  const suffix =
    inferredMessageType.value === 'status'
      ? 'thing/status/post'
      : inferredMessageType.value === 'event'
        ? 'thing/event/post'
        : 'thing/property/post';

  return `/sys/${reportForm.productKey}/${reportForm.deviceCode}/${suffix}`;
});

const previewInfoItems = computed(() => [
  {
    key: 'recommended-topic',
    label: '推荐 Topic',
    value: recommendedTopic.value
  },
  {
    key: 'message-type',
    label: 'messageType',
    value: inferredMessageType.value
  },
  {
    key: 'payload-status',
    label: 'Payload 状态',
    value: parsedPayload.value ? 'JSON 有效' : 'JSON 无法解析'
  },
  {
    key: 'request-path',
    label: '模拟入口',
    value: 'POST /message/http/report'
  }
]);

const curlPreview = computed(() => {
  const body = JSON.stringify(
    {
      ...reportForm,
      topic: reportForm.topic || recommendedTopic.value
    },
    null,
    2
  );

  return `curl -X POST http://localhost:9999/message/http/report \\
  -H "Content-Type: application/json" \\
  -d '${body}'`;
});

function applyTemplate(template: TemplateOption) {
  reportForm.payload = prettyJson(template.payload);
  syncTopic();
}

function syncTopic() {
  reportForm.topic = recommendedTopic.value;
}

async function handleSendReport() {
  isSending.value = true;
  errorMessage.value = '';

  const requestPayload = {
    ...reportForm,
    topic: reportForm.topic || recommendedTopic.value
  };

  try {
    const response = await reportByHttp(requestPayload);
    lastResponse.value = response;
    ElMessage.success(`设备 ${reportForm.deviceCode} 模拟上报成功`);
    recordActivity({
      module: 'HTTP 上报实验台',
      action: '发送模拟上报',
      request: requestPayload,
      response,
      ok: true,
      detail: `已向设备 ${reportForm.deviceCode} 发送 ${inferredMessageType.value} 报文`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    lastResponse.value = { ok: false, message: errorMessage.value };
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: 'HTTP 上报实验台',
      action: '发送模拟上报',
      request: requestPayload,
      response: { message: errorMessage.value },
      ok: false,
      detail: `发送失败：${errorMessage.value}`
    });
  } finally {
    isSending.value = false;
  }
}
</script>

<style scoped>
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

.report-preview {
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-md);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand) 5%, white));
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
