<template>
  <div class="risk-workbench-page">
    <!--  -->
    <div class="workbench-header">
      <div class="header-left">
        <h1 class="page-title">̨</h1>
        <span class="timestamp">{{ currentTime }}</span>
      </div>
      <div class="header-right">
        <el-radio-group v-model="currentRole" size="large">
          <el-radio-button value="field">һԱ</el-radio-button>
          <el-radio-button value="ops">άԱ</el-radio-button>
          <el-radio-button value="manager">Ա</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- յȼ -->
    <div class="risk-banner" :class="`risk-banner--${riskSummary.tone}`">
      <div class="banner-content">
        <p class="banner-label">ǰյȼ</p>
        <strong class="banner-value">{{ riskSummary.label }}</strong>
        <p class="banner-desc">{{ riskSummary.description }}</p>
      </div>
      <div class="banner-score">
        <small></small>
        <strong>{{ riskSummary.score }}</strong>
      </div>
    </div>

    <!-- ؼָ꿨Ƭ -->
    <div class="quad-grid">
      <MetricCard
        v-for="metric in roleMetrics[currentRole]"
        :key="metric.label"
        :label="metric.label"
        :value="metric.value"
        :badge="metric.badge"
      />
    </div>

    <!-- 빤 -->
    <div class="main-workarea">
      <!-- ҵ񵼺 -->
      <div class="role-quick-access">
        <h3 class="section-title">ҵ񵼺</h3>
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
            </div>
            <el-icon class="action-arrow"><arrow-right /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- Ԥ -->
    <div class="property-trend-section">
      <PropertyTrendPanel :logs="logs" />
    </div>

    <!-- ײϢ -->
    <div class="workbench-footer">
      <div class="footer-section">
        <div class="action-list">
          <div
            v-for="item in riskSummary.actions"
            :key="item"
            class="action-item"
          >
            <span class="action-badge">{{ riskSummary.shortLabel }}</span>
            <p class="action-text">{{ item }}</p>
          </div>
        </div>
      </div>
      <div class="footer-section">
        <div v-if="device" class="device-info-grid">
          <div class="info-chip">
            <span>豸</span>
            <strong>{{ device.deviceName }}</strong>
          </div>
          <div class="info-chip">
            <span>豸</span>
            <strong>{{ device.deviceCode }}</strong>
          </div>
          <div class="info-chip">
            <span>״̬</span>
            <strong>{{ statusLabel(device.onlineStatus) }}</strong>
          </div>
          <div class="info-chip">
            <span>Э</span>
            <strong>{{ device.protocolCode || '--' }}</strong>
          </div>
          <div class="info-chip">
            <span>ϱ</span>
            <strong>{{ formatDateTime(device.lastReportTime) }}</strong>
          </div>
          <div class="info-chip">
            <span>̼汾</span>
            <strong>{{ device.firmwareVersion || '--' }}</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- ؼ -->
    <div class="data-panels">
      <PanelCard
        eyebrow="Key Properties"
        title="ؼָ"
      >
        <div v-if="propertyHighlights.length" class="highlight-grid">
          <article v-for="item in propertyHighlights" :key="item.identifier" class="highlight-card">
            <span>{{ item.identifier }}</span>
            <strong>{{ item.propertyValue || '--' }}</strong>
            <small>{{ item.valueType || 'unknown' }}</small>
            <p>{{ formatDateTime(item.updateTime || item.reportTime) }}</p>
          </article>
        </div>
      </PanelCard>

      <PanelCard
        eyebrow="Report Draft"
        title="շ����ݸ"
      >
        <div class="report-draft">
          <p><strong>յ㣺</strong>{{ device?.deviceName || deviceCode || '--' }}</p>
          <p><strong>ǰȼ</strong>{{ riskSummary.label }}</p>
          <p><strong>жժҪ</strong>{{ reportDraft.summary }}</p>
          <p><strong>鶯</strong>{{ reportDraft.actions }}</p>
          <p><strong>ע</strong>{{ reportDraft.followUp }}</p>
        </div>
      </PanelCard>
    </div>

    <!-- Ϣ־ -->
    <div class="message-logs-panel">
      <PanelCard
        eyebrow="Message Logs"
        title="Ϣ־ƻؿ"
      >
        <div v-if="logs.length" class="timeline">
          <article v-for="item in logs" :key="item.id" class="timeline-item">
            <h3>{{ item.messageType || 'unknown' }}</h3>
            <p>{{ item.topic || '--' }}</p>
            <p>{{ truncateText(item.payload || '--', 160) }}</p>
            <p>{{ formatDateTime(item.reportTime || item.createTime) }}</p>
          </article>
        </div>
      </PanelCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from '@/utils/message';
import { ArrowRight } from '@element-plus/icons-vue';

import { getDeviceByCode, getDeviceMessageLogs, getDeviceProperties } from '../api/iot';
import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import PropertyTrendPanel from '../components/PropertyTrendPanel.vue';
import { recordActivity } from '../stores/activity';
import type { Device, DeviceMessageLog, DeviceProperty } from '../types/api';
import { formatDateTime, statusLabel, truncateText } from '../utils/format';

interface RiskReason {
  title: string;
  tag: string;
  description: string;
}

interface RiskSummary {
  score: string;
  label: string;
  shortLabel: string;
  tone: 'red' | 'orange' | 'yellow' | 'blue';
  description: string;
  reasons: RiskReason[];
  actions: string[];
}

const route = useRoute();
const router = useRouter();

// ɫл
const currentRole = ref<'field' | 'ops' | 'manager'>('field');

// ʱ
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

// 豸
const deviceCode = ref(typeof route.query.deviceCode === 'string' ? route.query.deviceCode : 'demo-device-01');
const isLoading = ref(false);
const errorMessage = ref('');
const lastFetchTime = ref<string | null>(null);
const device = ref<Device | null>(null);
const properties = ref<DeviceProperty[]>([]);
const logs = ref<DeviceMessageLog[]>([]);

watch(deviceCode, (value) => {
  router.replace({
    query: {
      ...route.query,
      deviceCode: value
    }
  });
});

watch(
  () => route.query.deviceCode,
  (value) => {
    if (typeof value === 'string' && value !== deviceCode.value) {
      deviceCode.value = value;
    }
  }
);

onMounted(() => {
  refreshAll();
});

// ϱʱ
const latestReportDate = computed(() => {
  const value = device.value?.lastReportTime || logs.value[0]?.reportTime || logs.value[0]?.createTime;
  if (!value) {
    return null;
  }
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
});

const freshnessMinutes = computed(() => {
  if (!latestReportDate.value) {
    return null;
  }
  return Math.max(0, Math.round((Date.now() - latestReportDate.value.getTime()) / 60000));
});

// 쳣źż
const suspiciousPropertySignals = computed(() => {
  const findings: Array<{ title: string; description: string }> = [];

  for (const property of properties.value) {
    const identifier = (property.identifier || '').toLowerCase();
    const numericValue = Number(property.propertyValue);
    if (Number.isNaN(numericValue)) {
      continue;
    }

    if (identifier.includes('signal') && numericValue < -90) {
      findings.push({
        title: 'źƫ',
        description: `${property.identifier} ǰֵ ${numericValue}˲߻`
      });
    }

    if ((identifier.includes('battery') || identifier.includes('volt')) && numericValue < 3.3) {
      findings.push({
        title: '״̬ƫ',
        description: `${property.identifier} ǰֵ ${numericValue}ֳ˲鹩̫״̬`
      });
    }

    if (identifier.includes('angle') && Math.abs(numericValue) >= 5) {
      findings.push({
        title: 'Ǳ仯Ҫע',
        description: `${property.identifier} ǰֵ ${numericValue}ͼ۲Ƿ仯`
      });
    }

    if ((identifier.includes('gps') || identifier.includes('trend')) && Math.abs(numericValue) >= 10) {
      findings.push({
        title: 'λƽ',
        description: `${property.identifier} ǰֵ ${numericValue}ע𲢽и⡣`
      });
    }
  }

  return findings.slice(0, 4);
});

// ժҪ
const riskSummary = computed<RiskSummary>(() => {
  let score = 0;
  const reasons: RiskReason[] = [];

  if (!device.value) {
    return {
      score: '--',
      label: '',
      shortLabel: 'NA',
      tone: 'blue',
      description: '豸벢ˢ£ظ÷յݡ',
      reasons: [{ title: 'δ豸', tag: '׼', description: 'ǰû豸ͼݡ' }],
      actions: ['豸벢ˢ¼̨']
    };
  }

  if (device.value.onlineStatus !== 1) {
    score += 35;
    reasons.push({
      title: '豸ǰ',
      tag: '豸״̬',
      description: '豸߻ᵼ·յּäҪȷϹ硢豸״̬'
    });
  }

  if (freshnessMinutes.value === null) {
    score += 22;
    reasons.push({
      title: 'δյϱ',
      tag: 'ʱЧ',
      description: 'ǰûпڷжϵϱʱ䣬֤·'
    });
  } else if (freshnessMinutes.value > 180) {
    score += 28;
    reasons.push({
      title: 'ʱЧͺ',
      tag: 'ʱЧ',
      description: `ϱԼ ${freshnessMinutes.value} ӣ˲ɼ·`
    });
  } else if (freshnessMinutes.value > 60) {
    score += 16;
    reasons.push({
      title: 'ʱЧҪע',
      tag: 'ʱЧ',
      description: `ϱԼ ${freshnessMinutes.value} ӣ߹עƵʡ`
    });
  }

  if (!properties.value.length) {
    score += 20;
    reasons.push({
      title: 'ȱԿ',
      tag: '',
      description: 'ǰûݣһԱжϷձ仯'
    });
  }

  if (!logs.value.length) {
    score += 12;
    reasons.push({
      title: 'ȱ·־',
      tag: '·',
      description: 'ǰûϢ־зʵʩԱٻؿġ'
    });
  }

  for (const signal of suspiciousPropertySignals.value) {
    score += 10;
    reasons.push({
      title: signal.title,
      tag: '쳣',
      description: signal.description
    });
  }

  score = Math.min(score, 100);

  let tone: RiskSummary['tone'] = 'blue';
  let label = 'ɫ';
  let shortLabel = '';
  let description = 'ǰյ崦ڳ۲״̬ɼάճѲ졣';

  if (score >= 70) {
    tone = 'red';
    label = 'ɫ';
    shortLabel = '';
    description = 'ǰյ߱ϸ߽ԣϱֳ֯ר';
  } else if (score >= 45) {
    tone = 'orange';
    label = 'ɫ';
    shortLabel = '';
    description = 'ǰյҪص٣Ƶʲר顣';
  } else if (score >= 20) {
    tone = 'yellow';
    label = 'ɫ';
    shortLabel = '';
    description = 'ǰյҪעļ󣬽Ƽ۲졣';
  }

  const actions = buildActions(tone);
  return {
    score: String(score),
    label,
    shortLabel,
    tone,
    description,
    reasons: reasons.length
      ? reasons
      : [{ title: '״̬ƽ', tag: '', description: 'ǰûз쳣źţάм⡣' }],
    actions
  };
});

// ɫָ
const roleMetrics = computed(() => [
  {
    label: 'ǰյȼ',
    value: riskSummary.value.label,
    badge: {
      label: riskSummary.value.shortLabel,
      tone: riskSummary.value.tone === 'red'
        ? 'danger'
        : riskSummary.value.tone === 'orange'
          ? 'warning'
          : riskSummary.value.tone === 'yellow'
            ? 'warning'
            : 'brand'
    }
  },
  {
    label: '״̬',
    value: device.value ? statusLabel(device.value.onlineStatus) : '--',
    badge: {
      label: device.value?.onlineStatus === 1 ? '' : '',
      tone: device.value?.onlineStatus === 1 ? 'success' : 'muted'
    }
  },
  {
    label: 'Կ',
    value: String(properties.value.length),
    badge: { label: 'Property', tone: 'brand' }
  },
  {
    label: 'Ϣ־',
    value: String(logs.value.length),
    badge: { label: 'Audit', tone: 'success' }
  }
]);

// Ը
const propertyHighlights = computed(() => properties.value.slice(0, 6));

// ҵ񵼺
const roleActions = {
  field: [
    { icon: '??', title: '̨', path: '/insight' },
    { icon: '??', title: '߲鿴', path: '/insight' },
    { icon: '??', title: 'һɱ', path: '/insight' },
    { icon: '??', title: 'ͼ', path: '/future-lab' }
  ],
  ops: [
    { icon: '??', title: '豸ά', path: '/devices' },
    { icon: '??', title: 'ֵ', path: '/devices' },
    { icon: '??', title: '豸Ѳ', path: '/devices' },
    { icon: '??', title: 'У', path: '/file-debug' }
  ],
  manager: [
    { icon: '??', title: '̬', path: '/future-lab' },
    { icon: '??', title: 'רⱨ', path: '/insight' },
    { icon: '??', title: 'ʷ', path: '/reporting' },
    { icon: '??', title: 'ݿ', path: '/future-lab' }
  ]
};

// ݸ
const reportDraft = computed(() => ({
  summary: riskSummary.value.description,
  actions: riskSummary.value.actions.join(''),
  followUp: riskSummary.value.tone === 'red' || riskSummary.value.tone === 'orange'
    ? ' 24 Сʱڳ⣬ٷյȼǷһ'
    : 'άճ۲⣬صעµ쳣ԻϱʱЧ仯'
}));

// 
function buildActions(tone: RiskSummary['tone']) {
  const actions = ['Ⱥ˲ԡͼϢ־ȷǷ'];

  if (tone === 'red') {
    actions.push('ϱϼ쵼γɸ߷ר����档');
    actions.push('ֳͬ˺ά飬ȷǷҪʱܿشʩ');
  } else if (tone === 'orange') {
    actions.push('鰲רصѲ죬γɷոټ¼');
    actions.push('άͬ˲豸ֵԶ̿');
  } else if (tone === 'yellow') {
    actions.push('߹۲ƵʣǷ񻯡');
  } else {
    actions.push('ǰάճ⣬Ӧ־Կա');
  }

  return actions;
}

// 
const navigateTo = (path: string) => {
  router.push(path);
};

// ˢ
async function refreshAll() {
  isLoading.value = true;
  errorMessage.value = '';

  try {
    const [deviceResponse, propertyResponse, logResponse] = await Promise.all([
      getDeviceByCode(deviceCode.value),
      getDeviceProperties(deviceCode.value),
      getDeviceMessageLogs(deviceCode.value)
    ]);

    device.value = deviceResponse.data;
    properties.value = propertyResponse.data;
    logs.value = logResponse.data;
    lastFetchTime.value = new Date().toISOString();
    ElMessage.success(` ${deviceCode.value} ˢ³ɹ`);

    recordActivity({
      module: '̨',
      action: 'ˢҳ',
      request: { deviceCode: deviceCode.value },
      response: {
        device: deviceResponse.data,
        properties: propertyResponse.data.length,
        logs: logResponse.data.length
      },
      ok: true,
      detail: ` ${deviceCode.value} ˢɣ ${propertyResponse.data.length} ־ ${logResponse.data.length} `
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: '̨',
      action: 'ˢҳ',
      request: { deviceCode: deviceCode.value },
      response: { message: errorMessage.value },
      ok: false,
      detail: `ˢʧܣ${errorMessage.value}`
    });
  } finally {
    isLoading.value = false;
  }
}

// 
onMounted(() => {
  recordActivity({
    module: '̨',
    action: 'ҳ',
    request: { path: '/insight' },
    ok: true,
    detail: 'ûʼ̨'
  });
});
</script>

<style scoped>
.risk-workbench-page {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

/*  */
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

/* ɫл */
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

/* յȼ */
.risk-banner {
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

.risk-banner--red {
  border-color: rgba(255, 109, 109, 0.28);
}

.risk-banner--red .banner-value {
  color: #ff6d6d;
}

.risk-banner--orange {
  border-color: rgba(255, 179, 71, 0.28);
}

.risk-banner--orange .banner-value {
  color: #ffb347;
}

.risk-banner--yellow {
  border-color: rgba(255, 214, 102, 0.28);
}

.risk-banner--yellow .banner-value {
  color: #ffd666;
}

.risk-banner--blue {
  border-color: rgba(82, 174, 255, 0.24);
}

.risk-banner--blue .banner-value {
  color: #52aaff;
}

/* Ĺָ */
.quad-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

/* 빤 */
.main-workarea {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 1rem;
}

/*  */
.risk-analysis {
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

.reason-grid {
  display: grid;
  gap: 1rem;
}

.reason-card {
  padding: 1.25rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
}

.reason-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.reason-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.reason-tag {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--text-tertiary);
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  background: rgba(255, 106, 0, 0.08);
  color: var(--brand-bright);
}

.reason-desc {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

/* ҵ񵼺 */
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

/* Ԥ */
.property-trend-section {
  padding: 1rem;
}

/* ײϢ */
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

.action-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.action-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
}

.action-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 2.2rem;
  height: 2.2rem;
  border-radius: 0.9rem;
  background: rgba(30, 128, 255, 0.12);
  color: var(--brand-bright);
  font-family: var(--font-mono);
  flex-shrink: 0;
}

.action-text {
  margin: 0;
  font-size: 0.9rem;
  color: var(--text-secondary);
  line-height: 1.7;
}

.device-info-grid {
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

/* ؼ */
.data-panels {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.highlight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.highlight-card {
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
}

.highlight-card span,
.highlight-card small {
  display: block;
  color: var(--text-tertiary);
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.highlight-card strong {
  margin: 0.45rem 0;
  font-family: var(--font-display);
  font-size: 1.35rem;
  color: var(--text-primary);
}

.highlight-card p {
  margin: 0;
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.report-draft {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.report-draft p {
  margin: 0;
  line-height: 1.8;
}

.report-draft p strong {
  color: var(--brand-bright);
}

/* Ϣ־ */
.message-logs-panel {
  padding: 1rem;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.timeline-item {
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
}

.timeline-item h3 {
  margin: 0 0 0.5rem;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--brand-bright);
}

.timeline-item p {
  margin: 0.25rem 0;
  font-size: 0.85rem;
  color: var(--text-secondary);
  line-height: 1.6;
}

.timeline-item p:first-of-type {
  font-family: var(--font-mono);
  color: var(--text-primary);
}

/* Ӧʽ */
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

  .highlight-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
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

  .highlight-grid {
    grid-template-columns: 1fr;
  }
}
</style>


