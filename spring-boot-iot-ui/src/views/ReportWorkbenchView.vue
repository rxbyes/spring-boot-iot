<template>
  <div class="page-stack">
    <section class="two-column-grid">
      <PanelCard
        eyebrow="HTTP Simulator"
        title="模拟设备上报"
        description="这里直接对应 `POST /message/http/report`，是当前 Phase 1 最关键的联调入口。"
      >
        <div class="button-row" style="margin-bottom: 1rem;">
          <button
            v-for="template in templates"
            :key="template.name"
            class="secondary-button"
            type="button"
            @click="applyTemplate(template)"
          >
            {{ template.name }}
          </button>
        </div>

        <form class="form-grid" @submit.prevent="handleSendReport">
          <div class="field-group">
            <label for="report-protocol">协议编码</label>
            <input id="report-protocol" v-model="reportForm.protocolCode" autocomplete="off" required />
          </div>
          <div class="field-group">
            <label for="report-product-key">产品 Key</label>
            <input id="report-product-key" v-model="reportForm.productKey" autocomplete="off" required />
          </div>
          <div class="field-group">
            <label for="report-device-code">设备编码</label>
            <input id="report-device-code" v-model="reportForm.deviceCode" autocomplete="off" required />
          </div>
          <div class="field-group">
            <label for="report-client-id">客户端 ID</label>
            <input id="report-client-id" v-model="reportForm.clientId" autocomplete="off" />
          </div>
          <div class="field-group">
            <label for="report-tenant">租户 ID</label>
            <input id="report-tenant" v-model="reportForm.tenantId" autocomplete="off" />
          </div>
          <div class="field-group">
            <label for="report-topic">Topic</label>
            <input id="report-topic" v-model="reportForm.topic" autocomplete="off" />
          </div>
          <div class="field-group" style="grid-column: 1 / -1;">
            <label for="payload">Payload</label>
            <textarea id="payload" v-model="reportForm.payload" />
          </div>
          <div class="button-row" style="grid-column: 1 / -1;">
            <button class="primary-button" type="submit" :disabled="isSending">
              {{ isSending ? '发送中...' : '发送上报' }}
            </button>
            <button class="secondary-button" type="button" @click="syncTopic">
              用推荐 Topic 覆盖
            </button>
          </div>
        </form>
      </PanelCard>

      <PanelCard
        eyebrow="Protocol Preview"
        title="报文预演"
        description="在真正调用接口前，先看 topic、messageType 和 curl 命令是否合理。"
      >
        <div class="info-grid">
          <div class="info-chip">
            <span>推荐 Topic</span>
            <strong>{{ recommendedTopic }}</strong>
          </div>
          <div class="info-chip">
            <span>messageType</span>
            <strong>{{ inferredMessageType }}</strong>
          </div>
          <div class="info-chip">
            <span>Payload 状态</span>
            <strong>{{ parsedPayload ? 'JSON 有效' : 'JSON 无法解析' }}</strong>
          </div>
          <div class="info-chip">
            <span>模拟入口</span>
            <strong>POST /message/http/report</strong>
          </div>
        </div>
        <div class="empty-state" style="margin-top: 1rem;">
          当前 curl 预览：
          <pre style="margin: 0.75rem 0 0; white-space: pre-wrap;">{{ curlPreview }}</pre>
        </div>
      </PanelCard>
    </section>

    <div v-if="errorMessage" class="empty-state">{{ errorMessage }}</div>

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
      eyebrow="Flow Reminder"
      title="发送后建议检查"
      description="按照文档推荐的 Phase 1 验证顺序，确认报文已进入主链路并更新设备状态。"
    >
      <div class="flow-rail">
        <div v-for="step in followUpSteps" :key="step.title" class="flow-rail__item">
          <span class="flow-rail__index">{{ step.index }}</span>
          <div>
            <strong>{{ step.title }}</strong>
            <span>{{ step.description }}</span>
          </div>
        </div>
      </div>
    </PanelCard>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';

import { reportByHttp } from '../api/iot';
import PanelCard from '../components/PanelCard.vue';
import ResponsePanel from '../components/ResponsePanel.vue';
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
