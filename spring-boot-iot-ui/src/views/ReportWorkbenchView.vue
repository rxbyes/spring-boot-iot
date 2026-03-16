<template>
  <div class="report-workbench-page">
    <!-- 顶部导航栏 -->
    <div class="workbench-header">
      <div class="header-left">
        <h1 class="page-title">HTTP 上报实验台</h1>
        <span class="timestamp">{{ currentTime }}</span>
      </div>
      <div class="header-right">
        <el-radio-group v-model="currentRole" size="large">
          <el-radio-button value="field">研发</el-radio-button>
          <el-radio-button value="ops">运维</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 上报状态横幅 -->
    <div class="report-banner" :class="`report-banner--${reportSummary.tone}`">
      <div class="banner-content">
        <p class="banner-label">当前上报状态</p>
        <strong class="banner-value">{{ reportSummary.label }}</strong>
        <p class="banner-desc">{{ reportSummary.description }}</p>
      </div>
      <div class="banner-score">
        <small>上报评分</small>
        <strong>{{ reportSummary.score }}</strong>
      </div>
    </div>

    <!-- 关键指标卡片 -->
    <div class="quad-grid">
      <MetricCard
        v-for="metric in roleMetrics[currentRole]"
        :key="metric.label"
        :label="metric.label"
        :value="metric.value"
        :badge="metric.badge"
      />
    </div>

    <!-- 中央工作区域 -->
    <div class="main-workarea">
      <!-- 上报配置区域 -->
      <div class="report-config">
        <h3 class="section-title">上报配置</h3>
        <div class="config-grid">
          <div class="config-card">
            <div class="config-header">
              <strong class="config-title">模拟设备上报</strong>
              <span class="config-tag">HTTP Report</span>
            </div>
            
            <div class="button-row" style="margin-bottom: 1rem;">
              <el-button
                v-for="template in templates"
                :key="template.name"
                class="secondary-button"
                text
                @click="applyTemplate(template)"
              >
                {{ template.name }}
              </el-button>
            </div>

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
                <el-input id="payload" v-model="reportForm.payload" name="report_payload" type="textarea" :rows="6" spellcheck="false" />
              </div>
              <div class="button-row" style="grid-column: 1 / -1;">
                <el-button class="primary-button" type="primary" native-type="submit" :loading="isSending">
                  {{ isSending ? '发送中...' : '发送上报' }}
                </el-button>
                <el-button class="secondary-button" @click="syncTopic">
                  用推荐 Topic 覆盖
                </el-button>
              </div>
            </form>
          </div>

          <div class="config-card">
            <div class="config-header">
              <strong class="config-title">报文预演</strong>
              <span class="config-tag">Preview</span>
            </div>
            
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
            <div class="curl-preview" style="margin-top: 1rem;">
              <p class="curl-label">当前 curl 预览：</p>
              <pre>{{ curlPreview }}</pre>
            </div>
          </div>
        </div>
      </div>

      <!-- 角色快捷入口 -->
      <div class="role-quick-access">
        <h3 class="section-title">角色快捷入口</h3>
        <div class="access-grid">
          <div
            v-for="action in roleActions[currentRole]"
            :key="action.title"
            class="action-card"
            @click="navigateTo(action.path)"
          >
            <div class="action-icon">{{ action.icon }}</div>
            <div class="action-content">
              <h4 class="action-title">{{ action.title }}</h4>
              <p class="action-desc">{{ action.desc }}</p>
            </div>
            <el-icon class="action-arrow"><arrow-right /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部信息 -->
    <div class="workbench-footer">
      <div class="footer-section">
        <h4>发送后建议检查</h4>
        <div class="flow-rail">
          <div v-for="step in followUpSteps" :key="step.title" class="flow-rail__item">
            <span class="flow-rail__index">{{ step.index }}</span>
            <div>
              <strong>{{ step.title }}</strong>
              <span>{{ step.description }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="footer-section">
        <h4>上报基础档案</h4>
        <div class="info-grid">
          <div class="info-chip">
            <span>协议编码</span>
            <strong>{{ reportForm.protocolCode || '--' }}</strong>
          </div>
          <div class="info-chip">
            <span>产品 Key</span>
            <strong>{{ reportForm.productKey || '--' }}</strong>
          </div>
          <div class="info-chip">
            <span>设备编码</span>
            <strong>{{ reportForm.deviceCode || '--' }}</strong>
          </div>
          <div class="info-chip">
            <span>上报入口</span>
            <strong>POST /message/http/report</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- 关键数据面板 -->
    <div class="data-panels">
      <PanelCard
        eyebrow="Parsed Payload"
        title="Payload 解析预览"
        :body="parsedPayload || { warning: '当前 payload 不是有效 JSON。' }"
      />

      <PanelCard
        eyebrow="Response"
        title="最后一次响应"
        :body="lastResponse"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowRight } from '@element-plus/icons-vue';

import { reportByHttp } from '../api/iot';
import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import { recordActivity } from '../stores/activity';
import type { HttpReportPayload } from '../types/api';
import { parseJsonSafely, prettyJson } from '../utils/format';

interface TemplateOption {
  name: string;
  payload: Record<string, unknown>;
}

interface ReportSummary {
  score: string;
  label: string;
  shortLabel: string;
  tone: 'red' | 'orange' | 'yellow' | 'blue';
  description: string;
}

const router = useRouter();

// 角色切换
const currentRole = ref<'field' | 'ops'>('field');

// 时间戳
const currentTime = ref('');
const updateTime = () => {
  const now = new Date();
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};
setInterval(updateTime, 1000);
updateTime();

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

  return `curl -X POST http://localhost:9999/message/http/report \\\n  -H "Content-Type: application/json" \\\n  -d '${body}'`;
});

// 上报摘要计算
const reportSummary = computed<ReportSummary>(() => {
  if (!reportForm.deviceCode || !reportForm.productKey) {
    return {
      score: '--',
      label: '待配置',
      shortLabel: 'NA',
      tone: 'blue',
      description: '请先配置设备编码和产品 Key，准备上报。'
    };
  }

  let score = 0;
  if (!reportForm.protocolCode) {
    score += 25;
  }
  if (!reportForm.topic) {
    score += 20;
  }
  if (!parsedPayload.value) {
    score += 30;
  }
  score = Math.min(score, 100);

  let tone: ReportSummary['tone'] = 'blue';
  let label = '蓝色上报';
  let shortLabel = '蓝';
  let description = '当前上报配置完整，可进行模拟上报测试。';

  if (score >= 40) {
    tone = 'yellow';
    label = '黄色上报';
    shortLabel = '黄';
    description = '当前上报配置存在部分缺失，建议补充关键字段后再进行上报。';
  } else if (score >= 15) {
    tone = 'orange';
    label = '橙色上报';
    shortLabel = '橙';
    description = '当前上报配置需要重点关注，建议补充协议和格式配置。';
  }

  return {
    score: String(score),
    label,
    shortLabel,
    tone,
    description
  };
});

// 角色指标
const roleMetrics = computed(() => [
  {
    label: '当前上报状态',
    value: reportSummary.value.label,
    hint: reportSummary.value.description,
    badge: {
      label: reportSummary.value.shortLabel,
      tone: reportSummary.value.tone === 'red'
        ? 'danger'
        : reportSummary.value.tone === 'orange'
          ? 'warning'
          : reportSummary.value.tone === 'yellow'
            ? 'warning'
            : 'brand'
    }
  },
  {
    label: '设备编码',
    value: reportForm.deviceCode || '--',
    hint: reportForm.deviceCode ? '当前设备唯一标识符。' : '当前没有设备编码。',
    badge: { label: 'Device', tone: 'brand' }
  },
  {
    label: '产品 Key',
    value: reportForm.productKey || '--',
    hint: reportForm.productKey ? '当前产品唯一标识符。' : '当前没有产品 Key。',
    badge: { label: 'Product', tone: 'brand' }
  },
  {
    label: '协议编码',
    value: reportForm.protocolCode || '--',
    hint: reportForm.protocolCode ? '当前使用的协议编码。' : '当前没有协议编码。',
    badge: { label: 'Protocol', tone: reportForm.protocolCode ? 'success' : 'warning' }
  }
]);

// 角色快捷入口
const roleActions = {
  field: [
    { icon: '🔧', title: '设备运维中心', desc: '设备建档与远程运维', path: '/devices' },
    { icon: '📊', title: '风险点工作台', desc: '风险监测与处置', path: '/insight' },
    { icon: '📈', title: '趋势曲线查看', desc: '分析属性与历史趋势', path: '/insight' },
    { icon: '📋', title: '专题报告', desc: '风险分析与处置报告', path: '/insight' }
  ],
  ops: [
    { icon: '⚙️', title: '阈值管理', desc: '参数配置与远程调整', path: '/devices' },
    { icon: '🔋', title: '设备巡检', desc: '离线与弱信号设备', path: '/devices' },
    { icon: '💾', title: '固件调试', desc: '文件与固件升级', path: '/file-debug' },
    { icon: '📡', title: 'HTTP 上报实验', desc: '模拟设备上报测试', path: '/reporting' }
  ]
};

// 导航
const navigateTo = (path: string) => {
  router.push(path);
};

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

// 生命周期
onMounted(() => {
  recordActivity({
    module: 'HTTP 上报实验台',
    action: '访问实验台',
    request: { path: '/reporting' },
    ok: true,
    detail: '用户访问 HTTP 上报实验台'
  });
});
</script>

<style scoped>
.report-workbench-page {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

/* 顶部导航栏 */
.workbench-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.97), rgba(247, 251, 255, 0.95)),
    radial-gradient(circle at 85% 20%, rgba(255, 106, 0, 0.12), transparent 30%);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.page-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.timestamp {
  font-family: var(--font-mono);
  font-size: 0.85rem;
  color: var(--brand-bright);
}

/* 角色切换 */
:deep(.el-radio-group) {
  --el-radio-button-checked-text-color: var(--brand-bright);
  --el-radio-button-checked-bg-color: rgba(255, 106, 0, 0.1);
  --el-radio-button-checked-border-color: var(--brand-bright);
}

:deep(.el-radio-button__inner) {
  background: #ffffff;
  border: 1px solid var(--panel-border);
  border-radius: 0.75rem;
  padding: 0.6rem 1.2rem;
  font-weight: 500;
  transition: all 180ms ease;
}

:deep(.el-radio-button__inner:hover) {
  border-color: var(--brand-bright);
  transform: translateY(-1px);
}

:deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
  background: rgba(255, 106, 0, 0.1);
  border-color: var(--brand-bright);
  box-shadow: 0 0 0 3px rgba(255, 106, 0, 0.12);
}

/* 上报状态横幅 */
.report-banner {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(82, 174, 255, 0.24);
  background:
    linear-gradient(165deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.94)),
    radial-gradient(circle at top right, rgba(30, 128, 255, 0.1), transparent 52%);
}

.banner-content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
}

.banner-label {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--text-tertiary);
  font-size: 0.76rem;
}

.banner-value {
  margin: 0;
  font-family: var(--font-display);
  font-size: 2.2rem;
  font-weight: 700;
  color: var(--text-primary);
}

.banner-desc {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.banner-score {
  text-align: right;
  min-width: 120px;
}

.banner-score small {
  display: block;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--text-tertiary);
  font-size: 0.76rem;
}

.banner-score strong {
  font-family: var(--font-display);
  font-size: 2.8rem;
  font-weight: 700;
  color: var(--text-primary);
}

.report-banner--red {
  border-color: rgba(255, 109, 109, 0.28);
}

.report-banner--red .banner-value {
  color: #ff6d6d;
}

.report-banner--orange {
  border-color: rgba(255, 179, 71, 0.28);
}

.report-banner--orange .banner-value {
  color: #ffb347;
}

.report-banner--yellow {
  border-color: rgba(255, 214, 102, 0.28);
}

.report-banner--yellow .banner-value {
  color: #ffd666;
}

.report-banner--blue {
  border-color: rgba(82, 174, 255, 0.24);
}

.report-banner--blue .banner-value {
  color: #52aaff;
}

/* 四宫格指标 */
.quad-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

/* 中央工作区域 */
.main-workarea {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 1rem;
}

/* 上报配置区域 */
.report-config {
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(165deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.94)),
    radial-gradient(circle at top right, rgba(30, 128, 255, 0.1), transparent 52%);
}

.section-title {
  margin: 0 0 1.25rem;
  font-size: 1.1rem;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.config-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.config-card {
  padding: 1.25rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.config-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.config-tag {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--text-tertiary);
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  background: rgba(255, 106, 0, 0.08);
  color: var(--brand-bright);
}

.config-desc {
  margin: 0 0 1rem;
  color: var(--text-secondary);
  line-height: 1.7;
}

/* 角色快捷入口 */
.role-quick-access {
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.97), rgba(247, 251, 255, 0.95)),
    radial-gradient(circle at 85% 20%, rgba(255, 106, 0, 0.12), transparent 30%);
}

.access-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
  cursor: pointer;
  transition: all 180ms ease;
}

.action-card:hover {
  border-color: var(--brand-bright);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 106, 0, 0.16);
}

.action-icon {
  width: 3rem;
  height: 3rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.75rem;
  background: rgba(255, 106, 0, 0.12);
  font-size: 1.5rem;
}

.action-content {
  flex: 1;
}

.action-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.action-desc {
  margin: 0.25rem 0 0;
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.action-arrow {
  color: var(--brand-bright);
  font-size: 1.2rem;
}

/* 底部信息 */
.workbench-footer {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.97), rgba(247, 251, 255, 0.95)),
    radial-gradient(circle at 85% 20%, rgba(255, 106, 0, 0.12), transparent 30%);
}

.footer-section h4 {
  margin: 0 0 1rem;
  font-size: 0.9rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--text-tertiary);
}

.flow-rail {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.flow-rail__item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
}

.flow-rail__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 2rem;
  height: 2rem;
  border-radius: 0.9rem;
  background: rgba(30, 128, 255, 0.12);
  color: var(--brand-bright);
  font-family: var(--font-mono);
  flex-shrink: 0;
}

.flow-rail__item strong {
  color: var(--text-primary);
}

.flow-rail__item span {
  color: var(--text-secondary);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.info-chip {
  padding: 0.9rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
}

.info-chip span {
  display: block;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--text-tertiary);
  margin-bottom: 0.25rem;
}

.info-chip strong {
  font-size: 0.95rem;
  color: var(--text-primary);
}

/* 关键数据面板 */
.data-panels {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

/* 响应式 */
@media (max-width: 1400px) {
  .main-workarea {
    grid-template-columns: 1fr;
  }

  .workbench-footer {
    grid-template-columns: 1fr;
  }

  .data-panels {
    grid-template-columns: 1fr;
  }

  .config-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .workbench-header {
    flex-direction: column;
    gap: 1rem;
  }

  .quad-grid,
  .access-grid {
    grid-template-columns: 1fr;
  }

  .banner-score {
    text-align: left;
  }
}
</style>

