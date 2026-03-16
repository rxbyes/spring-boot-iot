<template>
  <div class="page-stack">
    <section class="hero-grid">
      <div class="hero-panel operations-shell">
        <p class="eyebrow">Operations Center</p>
        <h1 class="headline">设备运维中心</h1>
        <div class="button-row" style="margin-top: 1.25rem;">
          <el-button class="primary-button" type="primary" @click="handleQueryByCode">
            快速刷新设备状态
          </el-button>
          <el-button class="secondary-button" @click="jumpToInsight">
            进入风险点工作台
          </el-button>
          <el-button class="ghost-button" @click="jumpToReporting">
            前往接入回放台
          </el-button>
        </div>

        <div class="ops-status" :class="`ops-status--${healthSummary.tone}`">
          <div>
            <p>设备健康状态</p>
            <strong>{{ healthSummary.label }}</strong>
            <span>{{ healthSummary.description }}</span>
          </div>
          <div class="ops-status__score">
            <small>运维评分</small>
            <strong>{{ healthSummary.score }}</strong>
          </div>
        </div>
      </div>

      <PanelCard
        eyebrow="Maintenance Focus"
        title="当前运维建议"
      >
        <div class="focus-list">
          <article v-for="item in maintenanceActions" :key="item" class="focus-list__item">
            <span class="focus-list__badge">{{ healthSummary.shortLabel }}</span>
            <p>{{ item }}</p>
          </article>
        </div>
      </PanelCard>
    </section>

    <section class="quad-grid">
      <MetricCard
        v-for="metric in overviewMetrics"
        :key="metric.label"
        :label="metric.label"
        :value="metric.value"
        :badge="metric.badge"
      />
    </section>

    <section class="two-column-grid">
      <PanelCard
        eyebrow="Provisioning"
        title="设备建档"
      >
        <form class="form-grid" @submit.prevent="handleCreateDevice">
          <div class="field-group">
            <label for="device-product-key">产品 Key</label>
            <el-input id="device-product-key" v-model="deviceForm.productKey" name="device_product_key" autocomplete="off" spellcheck="false" placeholder="例如 demo-product..." clearable />
          </div>
          <div class="field-group">
            <label for="device-name">设备名称</label>
            <el-input id="device-name" v-model="deviceForm.deviceName" name="device_name" placeholder="例如 演示设备-01..." clearable />
          </div>
          <div class="field-group">
            <label for="device-code">设备编码</label>
            <el-input id="device-code" v-model="deviceForm.deviceCode" name="device_code" autocomplete="off" spellcheck="false" placeholder="例如 demo-device-01..." clearable />
          </div>
          <div class="field-group">
            <label for="device-secret">设备密钥</label>
            <el-input id="device-secret" v-model="deviceForm.deviceSecret" type="password" show-password autocomplete="off" />
          </div>
          <div class="field-group">
            <label for="client-id">客户端 ID</label>
            <el-input id="client-id" v-model="deviceForm.clientId" name="client_id" autocomplete="off" spellcheck="false" clearable />
          </div>
          <div class="field-group">
            <label for="username">用户名</label>
            <el-input id="username" v-model="deviceForm.username" name="username" autocomplete="username" spellcheck="false" clearable />
          </div>
          <div class="field-group">
            <label for="password">密码</label>
            <el-input id="password" v-model="deviceForm.password" type="password" show-password autocomplete="off" />
          </div>
          <div class="field-group">
            <label for="firmware">固件版本</label>
            <el-input id="firmware" v-model="deviceForm.firmwareVersion" name="firmware_version" autocomplete="off" spellcheck="false" placeholder="例如 1.0.0..." clearable />
          </div>
          <div class="field-group">
            <label for="ip-address">IP 地址</label>
            <el-input id="ip-address" v-model="deviceForm.ipAddress" name="ip_address" inputmode="decimal" autocomplete="off" spellcheck="false" placeholder="例如 127.0.0.1..." clearable />
          </div>
          <div class="field-group">
            <label for="address">部署位置</label>
            <el-input id="address" v-model="deviceForm.address" name="device_address" placeholder="例如 山体北侧监测点..." clearable />
          </div>
          <div class="field-group" style="grid-column: 1 / -1;">
            <label for="metadata">扩展元数据</label>
            <el-input id="metadata" v-model="deviceForm.metadataJson" name="metadata_json" type="textarea" :rows="5" spellcheck="false" />
          </div>
          <div class="button-row" style="grid-column: 1 / -1;">
            <el-button class="primary-button" type="primary" native-type="submit" :loading="isCreating">
              {{ isCreating ? '创建设备中...' : '提交设备建档' }}
            </el-button>
            <el-button class="secondary-button" @click="resetForm">
              恢复演示数据
            </el-button>
          </div>
        </form>
      </PanelCard>

      <PanelCard
        eyebrow="Lookup"
        title="按 ID / 编码查询设备"
      >
        <div class="form-grid">
          <div class="field-group">
            <label for="query-device-id">设备 ID</label>
            <el-input id="query-device-id" v-model="queryId" name="query_device_id" inputmode="numeric" placeholder="例如 2001..." clearable />
          </div>
          <div class="field-group">
            <label for="query-device-code">设备编码</label>
            <el-input id="query-device-code" v-model="queryCode" name="query_device_code" autocomplete="off" spellcheck="false" placeholder="例如 demo-device-01..." clearable />
          </div>
        </div>
        <div class="button-row" style="margin-top: 1rem;">
          <el-button class="primary-button" type="primary" :loading="isQueryingId" @click="handleQueryById">
            {{ isQueryingId ? '查询中...' : '按 ID 查询' }}
          </el-button>
          <el-button class="secondary-button" :loading="isQueryingCode" @click="handleQueryByCode">
            {{ isQueryingCode ? '查询中...' : '按编码查询' }}
          </el-button>
        </div>

        <div v-if="currentDevice" class="info-grid" style="margin-top: 1rem;">
          <div class="info-chip">
            <span>设备名称</span>
            <strong>{{ currentDevice.deviceName }}</strong>
          </div>
          <div class="info-chip">
            <span>设备编码</span>
            <strong>{{ currentDevice.deviceCode }}</strong>
          </div>
          <div class="info-chip">
            <span>在线状态</span>
            <strong>{{ statusLabel(currentDevice.onlineStatus) }}</strong>
          </div>
          <div class="info-chip">
            <span>接入协议</span>
            <strong>{{ currentDevice.protocolCode || '--' }}</strong>
          </div>
          <div class="info-chip">
            <span>最近上报</span>
            <strong>{{ formatDateTime(currentDevice.lastReportTime) }}</strong>
          </div>
          <div class="info-chip">
            <span>最近离线</span>
            <strong>{{ formatDateTime(currentDevice.lastOfflineTime) }}</strong>
          </div>
          <div class="info-chip">
            <span>固件版本</span>
            <strong>{{ currentDevice.firmwareVersion || '--' }}</strong>
          </div>
          <div class="info-chip">
            <span>部署位置</span>
            <strong>{{ currentDevice.address || '--' }}</strong>
          </div>
        </div>
      </PanelCard>
    </section>

    <div v-if="errorMessage" class="empty-state" aria-live="polite">{{ errorMessage }}</div>

    <section class="tri-grid">
      <PanelCard
        eyebrow="Auth Baseline"
        title="设备认证基线"
      >
        <div class="baseline-list">
          <article class="baseline-list__item">
            <span>clientId</span>
            <strong>{{ currentDevice?.clientId || deviceForm.clientId || '--' }}</strong>
          </article>
          <article class="baseline-list__item">
            <span>username</span>
            <strong>{{ currentDevice?.username || deviceForm.username || '--' }}</strong>
          </article>
          <article class="baseline-list__item">
            <span>password</span>
            <strong>{{ maskSecret(currentDevice?.password || deviceForm.password) }}</strong>
          </article>
          <article class="baseline-list__item">
            <span>deviceSecret</span>
            <strong>{{ maskSecret(currentDevice?.deviceSecret || deviceForm.deviceSecret) }}</strong>
          </article>
        </div>
      </PanelCard>

      <PanelCard
        eyebrow="Remote O&M"
        title="远程运维预留"
      >
        <ul class="advice-list">
          <li>远程控制：重启设备、下发参数、校验配置回执。</li>
          <li>阈值管理：按风险点和设备类型配置预警阈值。</li>
          <li>固件升级：围绕 C.4 分包、MD5 校验和升级状态构建完整闭环。</li>
        </ul>
      </PanelCard>

      <PanelCard
        eyebrow="Engineering Trace"
        title="研发排障提示"
      >
        <ul class="advice-list">
          <li>设备查不到时，先核对产品 Key、设备编码和当前数据源环境。</li>
          <li>设备离线时，优先核查 MQTT 认证、Redis 会话和最近消息日志。</li>
          <li>固件或文件链路异常时，继续进入文件与固件调试页查看 Redis 聚合结果。</li>
        </ul>
      </PanelCard>
    </section>

    <section class="two-column-grid">
      <ResponsePanel
        eyebrow="Request"
        title="最后一次请求"
        :body="lastRequest"
      />
      <ResponsePanel
        eyebrow="Response"
        title="最后一次响应"
        :body="lastResponse"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';

import { addDevice, getDeviceByCode, getDeviceById } from '../api/iot';
import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import ResponsePanel from '../components/ResponsePanel.vue';
import { recordActivity } from '../stores/activity';
import type { Device, DeviceAddPayload } from '../types/api';
import { formatDateTime, statusLabel } from '../utils/format';

interface HealthSummary {
  score: string;
  label: string;
  shortLabel: string;
  tone: 'red' | 'orange' | 'yellow' | 'blue';
  description: string;
}

const router = useRouter();

const createDemoDevice = (): DeviceAddPayload => ({
  productKey: 'demo-product',
  deviceName: '演示设备-01',
  deviceCode: 'demo-device-01',
  deviceSecret: '123456',
  clientId: 'demo-device-01',
  username: 'demo-device-01',
  password: '123456',
  firmwareVersion: '1.0.0',
  ipAddress: '127.0.0.1',
  address: '山体北侧监测点',
  metadataJson: JSON.stringify({ zone: 'north-slope', protocol: 'mqtt-json', owner: 'ops-center' }, null, 2)
});

const deviceForm = reactive<DeviceAddPayload>(createDemoDevice());
const queryId = ref('2001');
const queryCode = ref('demo-device-01');

const isCreating = ref(false);
const isQueryingId = ref(false);
const isQueryingCode = ref(false);
const errorMessage = ref('');
const currentDevice = ref<Device | null>(null);
const lastRequest = ref<unknown>({ tip: '创建设备或查询设备后会显示请求体。' });
const lastResponse = ref<unknown>({ tip: '接口响应会出现在这里。' });

const lastReportDate = computed(() => {
  const value = currentDevice.value?.lastReportTime;
  if (!value) {
    return null;
  }
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
});

const lastReportMinutes = computed(() => {
  if (!lastReportDate.value) {
    return null;
  }
  return Math.max(0, Math.round((Date.now() - lastReportDate.value.getTime()) / 60000));
});

const authReady = computed(() => {
  const clientId = currentDevice.value?.clientId || deviceForm.clientId;
  const username = currentDevice.value?.username || deviceForm.username;
  const password = currentDevice.value?.password || deviceForm.password;
  const secret = currentDevice.value?.deviceSecret || deviceForm.deviceSecret;
  return Boolean(clientId && username && password && secret && clientId === username);
});

const healthSummary = computed<HealthSummary>(() => {
  if (!currentDevice.value) {
    return {
      score: '--',
      label: '待核查',
      shortLabel: 'NA',
      tone: 'blue',
      description: '请先查询设备，加载在线状态、认证字段和最近上报信息。'
    };
  }

  let score = 0;
  if (currentDevice.value.onlineStatus !== 1) {
    score += 40;
  }
  if (lastReportMinutes.value === null) {
    score += 20;
  } else if (lastReportMinutes.value > 180) {
    score += 25;
  } else if (lastReportMinutes.value > 60) {
    score += 12;
  }
  if (!authReady.value) {
    score += 18;
  }
  if (!currentDevice.value.firmwareVersion) {
    score += 8;
  }
  score = Math.min(score, 100);

  if (score >= 70) {
    return {
      score: String(score),
      label: '高风险运维状态',
      shortLabel: '高',
      tone: 'red',
      description: '设备当前存在明显运维风险，建议优先排查在线状态、最近上报和认证配置。'
    };
  }
  if (score >= 40) {
    return {
      score: String(score),
      label: '重点关注',
      shortLabel: '重',
      tone: 'orange',
      description: '设备当前需要重点跟踪，建议检查时效、固件和配置一致性。'
    };
  }
  if (score >= 18) {
    return {
      score: String(score),
      label: '轻度关注',
      shortLabel: '轻',
      tone: 'yellow',
      description: '设备整体可用，但仍存在需要优化的运维信息或时效性问题。'
    };
  }
  return {
    score: String(score),
    label: '运行稳定',
    shortLabel: '稳',
    tone: 'blue',
    description: '当前设备在线、上报及时，适合作为远程运维和协议联调的稳定样本。'
  };
});

const overviewMetrics = computed(() => {
  const getBadgeTone = (tone: 'red' | 'orange' | 'yellow' | 'blue'): 'danger' | 'warning' | 'muted' | 'success' | 'brand' => {
    if (tone === 'red') return 'danger';
    if (tone === 'orange') return 'warning';
    if (tone === 'yellow') return 'warning';
    return 'muted';
  };

  return [
    {
      label: '设备运维状态',
      value: healthSummary.value.label,
      hint: healthSummary.value.description,
      badge: {
        label: healthSummary.value.shortLabel,
        tone: getBadgeTone(healthSummary.value.tone)
      }
    },
    {
      label: '在线状态',
      value: currentDevice.value ? statusLabel(currentDevice.value.onlineStatus) : '--',
      hint: currentDevice.value?.onlineStatus === 1 ? '当前设备在线，可继续进行远程维护和链路验证。' : '当前设备离线，建议优先核查会话与网络。 ',
      badge: {
        label: currentDevice.value?.onlineStatus === 1 ? '在线' : '离线',
        tone: currentDevice.value?.onlineStatus === 1 ? 'success' : 'muted'
      }
    },
    {
      label: '最近上报时效',
      value: lastReportMinutes.value === null ? '--' : `${lastReportMinutes.value} min`,
      hint: lastReportMinutes.value === null ? '当前没有最近上报时间。' : '用于判断设备链路是否长时间中断。',
      badge: { label: 'Freshness', tone: 'brand' }
    },
    {
      label: '认证准备度',
      value: authReady.value ? '已就绪' : '待核查',
      hint: authReady.value ? 'MQTT 基础认证字段看起来一致。' : '请重点核对 clientId、username、password、deviceSecret。',
      badge: { label: 'MQTT', tone: authReady.value ? 'success' : 'warning' }
    }
  ] as const;
});

const maintenanceActions = computed(() => {
  const actions = ['先通过设备编码查询，确认当前在线状态和最近上报时间。'];
  if (currentDevice.value?.onlineStatus !== 1) {
    actions.push('当前设备离线，优先检查 MQTT 会话、网络连接、供电和现场链路。');
  }
  if (!authReady.value) {
    actions.push('核对 clientId、username、password 与 deviceSecret，避免认证失败。');
  }
  if (lastReportMinutes.value !== null && lastReportMinutes.value > 60) {
    actions.push(`最近上报已超过 ${lastReportMinutes.value} 分钟，建议复测并排查采集链路。`);
  }
  if (!currentDevice.value?.firmwareVersion) {
    actions.push('建议补录固件版本信息，便于后续升级和运维审计。');
  }
  if (actions.length === 1) {
    actions.push('设备整体运行稳定，可继续执行远程控制、阈值核查和联调验证。');
  }
  return actions;
});

function maskSecret(value?: string | null) {
  if (!value) {
    return '--';
  }
  if (value.length <= 4) {
    return '*'.repeat(value.length);
  }
  return `${value.slice(0, 2)}****${value.slice(-2)}`;
}

function resetForm() {
  Object.assign(deviceForm, createDemoDevice());
}

function jumpToInsight() {
  router.push({
    path: '/insight',
    query: { deviceCode: currentDevice.value?.deviceCode || deviceForm.deviceCode }
  });
}

function jumpToReporting() {
  router.push({
    path: '/reporting',
    query: { deviceCode: currentDevice.value?.deviceCode || deviceForm.deviceCode }
  });
}

async function handleCreateDevice() {
  isCreating.value = true;
  errorMessage.value = '';
  lastRequest.value = { method: 'POST', path: '/device/add', body: { ...deviceForm } };

  try {
    const response = await addDevice({ ...deviceForm });
    currentDevice.value = response.data;
    lastResponse.value = response;
    queryId.value = response.data?.id ? String(response.data.id) : queryId.value;
    queryCode.value = response.data.deviceCode;
    ElMessage.success(`设备 ${response.data.deviceCode} 创建成功`);
    recordActivity({
      module: '设备运维中心',
      action: '新增设备',
      request: lastRequest.value,
      response,
      ok: true,
      detail: `已创建设备 ${response.data.deviceCode}`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    lastResponse.value = { ok: false, message: errorMessage.value };
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: '设备运维中心',
      action: '新增设备',
      request: lastRequest.value,
      response: { message: errorMessage.value },
      ok: false,
      detail: `创建设备失败：${errorMessage.value}`
    });
  } finally {
    isCreating.value = false;
  }
}

async function handleQueryById() {
  isQueryingId.value = true;
  errorMessage.value = '';
  lastRequest.value = { method: 'GET', path: `/device/${queryId.value}` };

  try {
    const response = await getDeviceById(queryId.value);
    currentDevice.value = response.data;
    lastResponse.value = response;
    queryCode.value = response.data.deviceCode;
    ElMessage.success(`已查询到设备 ${response.data.deviceCode}`);
    recordActivity({
      module: '设备运维中心',
      action: '按 ID 查询设备',
      request: lastRequest.value,
      response,
      ok: true,
      detail: `查询到设备 ${response.data.deviceCode}`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    lastResponse.value = { ok: false, message: errorMessage.value };
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: '设备运维中心',
      action: '按 ID 查询设备',
      request: lastRequest.value,
      response: { message: errorMessage.value },
      ok: false,
      detail: `按 ID 查询失败：${errorMessage.value}`
    });
  } finally {
    isQueryingId.value = false;
  }
}

async function handleQueryByCode() {
  isQueryingCode.value = true;
  errorMessage.value = '';
  lastRequest.value = { method: 'GET', path: `/device/code/${queryCode.value}` };

  try {
    const response = await getDeviceByCode(queryCode.value);
    currentDevice.value = response.data;
    lastResponse.value = response;
    queryId.value = response.data?.id ? String(response.data.id) : queryId.value;
    ElMessage.success(`已查询到设备 ${response.data.deviceCode}`);
    recordActivity({
      module: '设备运维中心',
      action: '按编码查询设备',
      request: lastRequest.value,
      response,
      ok: true,
      detail: `查询到设备 ${response.data.deviceCode}`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    lastResponse.value = { ok: false, message: errorMessage.value };
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: '设备运维中心',
      action: '按编码查询设备',
      request: lastRequest.value,
      response: { message: errorMessage.value },
      ok: false,
      detail: `按编码查询失败：${errorMessage.value}`
    });
  } finally {
    isQueryingCode.value = false;
  }
}
</script>

<style scoped>
.operations-shell {
  display: grid;
  gap: 1.35rem;
}

.ops-status {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  padding: 1rem 1.1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(82, 174, 255, 0.24);
  background: #f8fbff;
}

.ops-status p,
.ops-status strong,
.ops-status span,
.ops-status small {
  display: block;
}

.ops-status p,
.ops-status small {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 0.76rem;
  text-transform: uppercase;
  letter-spacing: 0.14em;
}

.ops-status strong {
  margin-top: 0.45rem;
  font-size: 1.55rem;
}

.ops-status span {
  margin-top: 0.35rem;
  color: var(--text-secondary);
  line-height: 1.7;
}

.ops-status__score {
  text-align: right;
}

.ops-status__score strong {
  font-size: 2.2rem;
}

.ops-status--red {
  border-color: rgba(255, 109, 109, 0.28);
}

.ops-status--orange {
  border-color: rgba(255, 179, 71, 0.28);
}

.ops-status--yellow {
  border-color: rgba(255, 214, 102, 0.28);
}

.ops-status--blue {
  border-color: rgba(82, 174, 255, 0.28);
}

.focus-list,
.baseline-list {
  display: grid;
  gap: 0.85rem;
}

.focus-list__item,
.baseline-list__item {
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
}

.focus-list__item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.85rem;
  align-items: start;
}

.focus-list__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 2.2rem;
  height: 2.2rem;
  border-radius: 0.9rem;
  background: rgba(30, 128, 255, 0.12);
  color: var(--brand-bright);
  font-family: var(--font-mono);
}

.focus-list__item p {
  margin: 0;
  line-height: 1.7;
}

.baseline-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.baseline-list__item span,
.baseline-list__item strong {
  display: block;
}

.baseline-list__item span {
  color: var(--text-tertiary);
  font-size: 0.76rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.baseline-list__item strong {
  margin-top: 0.45rem;
  font-size: 1.05rem;
}

.advice-list {
  margin: 0;
  padding-left: 1.1rem;
  line-height: 1.9;
}

@media (max-width: 1200px) {
  .baseline-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .ops-status {
    flex-direction: column;
    align-items: flex-start;
  }

  .ops-status__score {
    text-align: left;
  }
}
</style>

