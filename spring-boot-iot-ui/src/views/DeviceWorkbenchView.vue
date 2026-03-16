<template>
  <div class="page-stack">
    <section class="hero-grid">
      <div class="hero-panel operations-shell">
        <p class="eyebrow">Operations Center</p>
        <h1 class="headline">豸ά</h1>
        <div class="button-row" style="margin-top: 1.25rem;">
          <el-button class="primary-button" type="primary" @click="handleQueryByCode">
            ˢ豸״̬
          </el-button>
          <el-button class="secondary-button" @click="jumpToInsight">
            յ㹤̨
          </el-button>
          <el-button class="ghost-button" @click="jumpToReporting">
            ǰط̨
          </el-button>
        </div>

        <div class="ops-status" :class="`ops-status--${healthSummary.tone}`">
          <div>
            <p>豸״̬</p>
            <strong>{{ healthSummary.label }}</strong>
            <span>{{ healthSummary.description }}</span>
          </div>
          <div class="ops-status__score">
            <small>ά</small>
            <strong>{{ healthSummary.score }}</strong>
          </div>
        </div>
      </div>

      <PanelCard
        eyebrow="Maintenance Focus"
        title="ǰά"
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
        title="豸"
      >
        <form class="form-grid" @submit.prevent="handleCreateDevice">
          <div class="field-group">
            <label for="device-product-key">Ʒ Key</label>
            <el-input id="device-product-key" v-model="deviceForm.productKey" name="device_product_key" autocomplete="off" spellcheck="false" placeholder=" demo-product..." clearable />
          </div>
          <div class="field-group">
            <label for="device-name">豸</label>
            <el-input id="device-name" v-model="deviceForm.deviceName" name="device_name" placeholder=" ʾ豸-01..." clearable />
          </div>
          <div class="field-group">
            <label for="device-code">豸</label>
            <el-input id="device-code" v-model="deviceForm.deviceCode" name="device_code" autocomplete="off" spellcheck="false" placeholder=" demo-device-01..." clearable />
          </div>
          <div class="field-group">
            <label for="device-secret">豸Կ</label>
            <el-input id="device-secret" v-model="deviceForm.deviceSecret" type="password" show-password autocomplete="off" />
          </div>
          <div class="field-group">
            <label for="client-id">ͻ ID</label>
            <el-input id="client-id" v-model="deviceForm.clientId" name="client_id" autocomplete="off" spellcheck="false" clearable />
          </div>
          <div class="field-group">
            <label for="username">û</label>
            <el-input id="username" v-model="deviceForm.username" name="username" autocomplete="username" spellcheck="false" clearable />
          </div>
          <div class="field-group">
            <label for="password"></label>
            <el-input id="password" v-model="deviceForm.password" type="password" show-password autocomplete="off" />
          </div>
          <div class="field-group">
            <label for="firmware">̼汾</label>
            <el-input id="firmware" v-model="deviceForm.firmwareVersion" name="firmware_version" autocomplete="off" spellcheck="false" placeholder=" 1.0.0..." clearable />
          </div>
          <div class="field-group">
            <label for="ip-address">IP ַ</label>
            <el-input id="ip-address" v-model="deviceForm.ipAddress" name="ip_address" inputmode="decimal" autocomplete="off" spellcheck="false" placeholder=" 127.0.0.1..." clearable />
          </div>
          <div class="field-group">
            <label for="address">λ</label>
            <el-input id="address" v-model="deviceForm.address" name="device_address" placeholder=" ɽ山..." clearable />
          </div>
          <div class="field-group" style="grid-column: 1 / -1;">
            <label for="metadata">չԪ</label>
            <el-input id="metadata" v-model="deviceForm.metadataJson" name="metadata_json" type="textarea" :rows="5" spellcheck="false" />
          </div>
          <div class="button-row" style="grid-column: 1 / -1;">
            <el-button class="primary-button" type="primary" native-type="submit" :loading="isCreating">
              {{ isCreating ? '豸...' : 'ύ豸' }}
            </el-button>
            <el-button class="secondary-button" @click="resetForm">
              ָʾ
            </el-button>
          </div>
        </form>
      </PanelCard>

      <PanelCard
        eyebrow="Lookup"
        title=" ID / ѯ豸"
      >
        <div class="form-grid">
          <div class="field-group">
            <label for="query-device-id">豸 ID</label>
            <el-input id="query-device-id" v-model="queryId" name="query_device_id" inputmode="numeric" placeholder=" 2001..." clearable />
          </div>
          <div class="field-group">
            <label for="query-device-code">豸</label>
            <el-input id="query-device-code" v-model="queryCode" name="query_device_code" autocomplete="off" spellcheck="false" placeholder=" demo-device-01..." clearable />
          </div>
        </div>
        <div class="button-row" style="margin-top: 1rem;">
          <el-button class="primary-button" type="primary" :loading="isQueryingId" @click="handleQueryById">
            {{ isQueryingId ? 'ѯ...' : ' ID ѯ' }}
          </el-button>
          <el-button class="secondary-button" :loading="isQueryingCode" @click="handleQueryByCode">
            {{ isQueryingCode ? 'ѯ...' : 'ѯ' }}
          </el-button>
        </div>

        <div v-if="currentDevice" class="info-grid" style="margin-top: 1rem;">
          <div class="info-chip">
            <span>豸</span>
            <strong>{{ currentDevice.deviceName }}</strong>
          </div>
          <div class="info-chip">
            <span>豸</span>
            <strong>{{ currentDevice.deviceCode }}</strong>
          </div>
          <div class="info-chip">
            <span>״̬</span>
            <strong>{{ statusLabel(currentDevice.onlineStatus) }}</strong>
          </div>
          <div class="info-chip">
            <span>Э</span>
            <strong>{{ currentDevice.protocolCode || '--' }}</strong>
          </div>
          <div class="info-chip">
            <span>ϱ</span>
            <strong>{{ formatDateTime(currentDevice.lastReportTime) }}</strong>
          </div>
          <div class="info-chip">
            <span></span>
            <strong>{{ formatDateTime(currentDevice.lastOfflineTime) }}</strong>
          </div>
          <div class="info-chip">
            <span>̼汾</span>
            <strong>{{ currentDevice.firmwareVersion || '--' }}</strong>
          </div>
          <div class="info-chip">
            <span>λ</span>
            <strong>{{ currentDevice.address || '--' }}</strong>
          </div>
        </div>
      </PanelCard>
    </section>

    <div v-if="errorMessage" class="empty-state" aria-live="polite">{{ errorMessage }}</div>

    <section class="tri-grid">
      <PanelCard
        eyebrow="Auth Baseline"
        title="豸֤"
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
        title="ԶάԤ"
      >
        <ul class="advice-list">
          <li>Զ̿ƣ豸·Уûִ</li>
          <li>ֵյ豸Ԥֵ</li>
          <li>̼Χ C.4 ְMD5 У״̬ջ</li>
        </ul>
      </PanelCard>

      <PanelCard
        eyebrow="Engineering Trace"
        title="зʾ"
      >
        <ul class="advice-list">
          <li>豸鲻ʱȺ˶ԲƷ Key豸͵ǰԴ</li>
          <li>豸ʱȺ˲ MQTT ֤Redis ỰϢ־</li>
          <li>̼ļ·쳣ʱļ̼ҳ鿴 Redis ۺϽ</li>
        </ul>
      </PanelCard>
    </section>

    <section class="two-column-grid">
      <ResponsePanel
        eyebrow="Request"
        title="һ"
        :body="lastRequest"
      />
      <ResponsePanel
        eyebrow="Response"
        title="һӦ"
        :body="lastResponse"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from '@/utils/message';

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
  deviceName: 'ʾ豸-01',
  deviceCode: 'demo-device-01',
  deviceSecret: '123456',
  clientId: 'demo-device-01',
  username: 'demo-device-01',
  password: '123456',
  firmwareVersion: '1.0.0',
  ipAddress: '127.0.0.1',
  address: 'ɽ山',
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
const lastRequest = ref<unknown>({ tip: '豸ѯ豸ʾ塣' });
const lastResponse = ref<unknown>({ tip: 'ӿӦ' });

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
      label: '˲',
      shortLabel: 'NA',
      tone: 'blue',
      description: 'Ȳѯ豸״ֶ̬֤κϱϢ'
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
      label: '߷ά״̬',
      shortLabel: '',
      tone: 'red',
      description: '豸ǰάգŲ״̬ϱ֤á'
    };
  }
  if (score >= 40) {
    return {
      score: String(score),
      label: 'صע',
      shortLabel: '',
      tone: 'orange',
      description: '豸ǰҪص٣ʱЧ̼һԡ'
    };
  }
  if (score >= 18) {
    return {
      score: String(score),
      label: 'ȹע',
      shortLabel: '',
      tone: 'yellow',
      description: '豸ãԴҪŻάϢʱЧ⡣'
    };
  }
  return {
    score: String(score),
    label: 'ȶ',
    shortLabel: '',
    tone: 'blue',
    description: 'ǰ豸ߡϱʱʺΪԶάЭȶ�����'
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
      label: '豸ά״̬',
      value: healthSummary.value.label,
      hint: healthSummary.value.description,
      badge: {
        label: healthSummary.value.shortLabel,
        tone: getBadgeTone(healthSummary.value.tone)
      }
    },
    {
      label: '״̬',
      value: currentDevice.value ? statusLabel(currentDevice.value.onlineStatus) : '--',
      hint: currentDevice.value?.onlineStatus === 1 ? 'ǰ豸ߣɼԶά·֤' : 'ǰ豸ߣȺ˲Ự硣 ',
      badge: {
        label: currentDevice.value?.onlineStatus === 1 ? '' : '',
        tone: currentDevice.value?.onlineStatus === 1 ? 'success' : 'muted'
      }
    },
    {
      label: 'ϱʱЧ',
      value: lastReportMinutes.value === null ? '--' : `${lastReportMinutes.value} min`,
      hint: lastReportMinutes.value === null ? 'ǰûϱʱ䡣' : 'ж豸·Ƿʱжϡ',
      badge: { label: 'Freshness', tone: 'brand' }
    },
    {
      label: '֤׼',
      value: authReady.value ? 'Ѿ' : '˲',
      hint: authReady.value ? 'MQTT ֶ֤οһ¡' : 'ص˶ clientIdusernamepassworddeviceSecret',
      badge: { label: 'MQTT', tone: authReady.value ? 'success' : 'warning' }
    }
  ] as const;
});

const maintenanceActions = computed(() => {
  const actions = ['ͨ豸ѯȷϵǰ״̬ϱʱ䡣'];
  if (currentDevice.value?.onlineStatus !== 1) {
    actions.push('ǰ豸ߣȼ MQTT Ựӡֳ·');
  }
  if (!authReady.value) {
    actions.push('˶ clientIdusernamepassword  deviceSecret֤ʧܡ');
  }
  if (lastReportMinutes.value !== null && lastReportMinutes.value > 60) {
    actions.push(`ϱѳ ${lastReportMinutes.value} ӣ鸴ⲢŲɼ·`);
  }
  if (!currentDevice.value?.firmwareVersion) {
    actions.push('鲹¼̼汾Ϣںάơ');
  }
  if (actions.length === 1) {
    actions.push('豸ȶɼִԶ̿ơֵ˲֤');
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
    ElMessage.success(`豸 ${response.data.deviceCode} ɹ`);
    recordActivity({
      module: '豸ά',
      action: '豸',
      request: lastRequest.value,
      response,
      ok: true,
      detail: `Ѵ豸 ${response.data.deviceCode}`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    lastResponse.value = { ok: false, message: errorMessage.value };
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: '豸ά',
      action: '豸',
      request: lastRequest.value,
      response: { message: errorMessage.value },
      ok: false,
      detail: `豸ʧܣ${errorMessage.value}`
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
    ElMessage.success(`Ѳѯ豸 ${response.data.deviceCode}`);
    recordActivity({
      module: '豸ά',
      action: ' ID ѯ豸',
      request: lastRequest.value,
      response,
      ok: true,
      detail: `ѯ豸 ${response.data.deviceCode}`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    lastResponse.value = { ok: false, message: errorMessage.value };
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: '豸ά',
      action: ' ID ѯ豸',
      request: lastRequest.value,
      response: { message: errorMessage.value },
      ok: false,
      detail: ` ID ѯʧܣ${errorMessage.value}`
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
    ElMessage.success(`Ѳѯ豸 ${response.data.deviceCode}`);
    recordActivity({
      module: '豸ά',
      action: 'ѯ豸',
      request: lastRequest.value,
      response,
      ok: true,
      detail: `ѯ豸 ${response.data.deviceCode}`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    lastResponse.value = { ok: false, message: errorMessage.value };
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: '豸ά',
      action: 'ѯ豸',
      request: lastRequest.value,
      response: { message: errorMessage.value },
      ok: false,
      detail: `ѯʧܣ${errorMessage.value}`
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


