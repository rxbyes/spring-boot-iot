<template>
  <div class="risk-workbench-page">
    <!-- 顶部导航栏 -->
    <div class="workbench-header">
      <div class="header-left">
        <h1 class="page-title">风险点工作台</h1>
        <span class="timestamp">{{ currentTime }}</span>
      </div>
      <div class="header-right">
        <el-radio-group v-model="currentRole" size="large">
          <el-radio-button value="field">一线人员</el-radio-button>
          <el-radio-button value="ops">运维人员</el-radio-button>
          <el-radio-button value="manager">管理人员</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 风险等级横幅 -->
    <div class="risk-banner" :class="`risk-banner--${riskSummary.tone}`">
      <div class="banner-content">
        <p class="banner-label">当前风险等级</p>
        <strong class="banner-value">{{ riskSummary.label }}</strong>
        <p class="banner-desc">{{ riskSummary.description }}</p>
      </div>
      <div class="banner-score">
        <small>风险评分</small>
        <strong>{{ riskSummary.score }}</strong>
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
            </div>
            <el-icon class="action-arrow"><arrow-right /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 属性趋势预览 -->
    <div class="property-trend-section">
      <PropertyTrendPanel :logs="logs" />
    </div>

    <!-- 底部信息 -->
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
            <span>设备名称</span>
            <strong>{{ device.deviceName }}</strong>
          </div>
          <div class="info-chip">
            <span>设备编码</span>
            <strong>{{ device.deviceCode }}</strong>
          </div>
          <div class="info-chip">
            <span>在线状态</span>
            <strong>{{ statusLabel(device.onlineStatus) }}</strong>
          </div>
          <div class="info-chip">
            <span>接入协议</span>
            <strong>{{ device.protocolCode || '--' }}</strong>
          </div>
          <div class="info-chip">
            <span>最近上报</span>
            <strong>{{ formatDateTime(device.lastReportTime) }}</strong>
          </div>
          <div class="info-chip">
            <span>固件版本</span>
            <strong>{{ device.firmwareVersion || '--' }}</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- 关键数据面板 -->
    <div class="data-panels">
      <PanelCard
        eyebrow="Key Properties"
        title="关键监测指标"
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
        title="风险分析报告草稿"
      >
        <div class="report-draft">
          <p><strong>风险点：</strong>{{ device?.deviceName || deviceCode || '--' }}</p>
          <p><strong>当前等级：</strong>{{ riskSummary.label }}</p>
          <p><strong>判定摘要：</strong>{{ reportDraft.summary }}</p>
          <p><strong>建议动作：</strong>{{ reportDraft.actions }}</p>
          <p><strong>后续关注：</strong>{{ reportDraft.followUp }}</p>
        </div>
      </PanelCard>
    </div>

    <!-- 消息日志面板 -->
    <div class="message-logs-panel">
      <PanelCard
        eyebrow="Message Logs"
        title="消息日志与审计回看"
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
import { ElMessage } from 'element-plus';
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

// 角色切换
const currentRole = ref<'field' | 'ops' | 'manager'>('field');

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

// 设备编码
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

// 最新上报时间计算
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

// 异常属性信号检测
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
        title: '信号质量偏弱',
        description: `${property.identifier} 当前值 ${numericValue}，建议核查网络和天线环境。`
      });
    }

    if ((identifier.includes('battery') || identifier.includes('volt')) && numericValue < 3.3) {
      findings.push({
        title: '供电状态偏低',
        description: `${property.identifier} 当前值 ${numericValue}，建议现场核查供电或太阳能状态。`
      });
    }

    if (identifier.includes('angle') && Math.abs(numericValue) >= 5) {
      findings.push({
        title: '倾角变化需要关注',
        description: `${property.identifier} 当前值 ${numericValue}，建议结合趋势图观察是否持续变化。`
      });
    }

    if ((identifier.includes('gps') || identifier.includes('trend')) && Math.abs(numericValue) >= 10) {
      findings.push({
        title: '位移趋势较明显',
        description: `${property.identifier} 当前值 ${numericValue}，建议提升关注级别并进行复测。`
      });
    }
  }

  return findings.slice(0, 4);
});

// 风险摘要计算
const riskSummary = computed<RiskSummary>(() => {
  let score = 0;
  const reasons: RiskReason[] = [];

  if (!device.value) {
    return {
      score: '--',
      label: '待加载',
      shortLabel: 'NA',
      tone: 'blue',
      description: '请输入设备编码并刷新，加载该风险点的最新数据。',
      reasons: [{ title: '未加载设备', tag: '数据准备', description: '当前还没有设备详情和监测数据。' }],
      actions: ['输入设备编码并刷新风险点工作台。']
    };
  }

  if (device.value.onlineStatus !== 1) {
    score += 35;
    reasons.push({
      title: '设备当前离线',
      tag: '设备状态',
      description: '设备离线会导致风险点出现监测盲区，需要优先确认供电、网络或设备健康状态。'
    });
  }

  if (freshnessMinutes.value === null) {
    score += 22;
    reasons.push({
      title: '暂未收到最新上报',
      tag: '数据时效',
      description: '当前没有可用于风险判断的最新上报时间，建议先验证接入链路。'
    });
  } else if (freshnessMinutes.value > 180) {
    score += 28;
    reasons.push({
      title: '数据时效严重滞后',
      tag: '数据时效',
      description: `最近上报距离现在约 ${freshnessMinutes.value} 分钟，建议立即核查采集链路。`
    });
  } else if (freshnessMinutes.value > 60) {
    score += 16;
    reasons.push({
      title: '数据时效需要关注',
      tag: '数据时效',
      description: `最近上报距离现在约 ${freshnessMinutes.value} 分钟，建议提高关注频率。`
    });
  }

  if (!properties.value.length) {
    score += 20;
    reasons.push({
      title: '缺少最新属性快照',
      tag: '数据完整性',
      description: '当前没有属性数据，不利于一线人员快速判断风险变化。'
    });
  }

  if (!logs.value.length) {
    score += 12;
    reasons.push({
      title: '缺少链路审计日志',
      tag: '审计链路',
      description: '当前没有消息日志，不利于研发和实施人员快速回看报文。'
    });
  }

  for (const signal of suspiciousPropertySignals.value) {
    score += 10;
    reasons.push({
      title: signal.title,
      tag: '属性异常',
      description: signal.description
    });
  }

  score = Math.min(score, 100);

  let tone: RiskSummary['tone'] = 'blue';
  let label = '蓝色风险';
  let shortLabel = '蓝';
  let description = '当前风险点整体处于常规观察状态，可继续维持日常监测与巡检。';

  if (score >= 70) {
    tone = 'red';
    label = '红色风险';
    shortLabel = '红';
    description = '当前风险点具备较高紧急性，建议立即上报并组织现场复核与专题分析。';
  } else if (score >= 45) {
    tone = 'orange';
    label = '橙色风险';
    shortLabel = '橙';
    description = '当前风险点需要重点跟踪，建议提升复测频率并安排专项检查。';
  } else if (score >= 20) {
    tone = 'yellow';
    label = '黄色风险';
    shortLabel = '黄';
    description = '当前风险点存在需要持续关注的迹象，建议结合趋势继续观察。';
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
      : [{ title: '监测状态平稳', tag: '正常', description: '当前没有发现明显异常信号，可维持例行监测。' }],
    actions
  };
});

// 角色指标
const roleMetrics = computed(() => [
  {
    label: '当前风险等级',
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
    label: '在线状态',
    value: device.value ? statusLabel(device.value.onlineStatus) : '--',
    badge: {
      label: device.value?.onlineStatus === 1 ? '在线' : '离线',
      tone: device.value?.onlineStatus === 1 ? 'success' : 'muted'
    }
  },
  {
    label: '属性快照数',
    value: String(properties.value.length),
    badge: { label: 'Property', tone: 'brand' }
  },
  {
    label: '消息日志数',
    value: String(logs.value.length),
    badge: { label: 'Audit', tone: 'success' }
  }
]);

// 属性高亮
const propertyHighlights = computed(() => properties.value.slice(0, 6));

// 角色快捷入口
const roleActions = {
  field: [
    { icon: '⚠️', title: '风险点工作台', path: '/insight' },
    { icon: '📈', title: '趋势曲线查看', path: '/insight' },
    { icon: '📄', title: '一键生成报告', path: '/insight' },
    { icon: '📊', title: '风险热力图', path: '/future-lab' }
  ],
  ops: [
    { icon: '📡', title: '设备运维中心', path: '/devices' },
    { icon: '⚙️', title: '阈值管理', path: '/devices' },
    { icon: '🔋', title: '设备巡检', path: '/devices' },
    { icon: '💾', title: '固件调试', path: '/file-debug' }
  ],
  manager: [
    { icon: '🌍', title: '区域态势', path: '/future-lab' },
    { icon: '📋', title: '专题报告', path: '/insight' },
    { icon: '🔍', title: '历史回溯', path: '/reporting' },
    { icon: '📈', title: '数据看板', path: '/future-lab' }
  ]
};

// 报告草稿
const reportDraft = computed(() => ({
  summary: riskSummary.value.description,
  actions: riskSummary.value.actions.join('；'),
  followUp: riskSummary.value.tone === 'red' || riskSummary.value.tone === 'orange'
    ? '建议 24 小时内持续复测，并跟踪风险等级是否进一步提升。'
    : '建议维持日常观测，重点关注新的异常属性或上报时效变化。'
}));

// 构建动作
function buildActions(tone: RiskSummary['tone']) {
  const actions = ['先核查最新属性、趋势图与消息日志，确认数据是否完整。'];

  if (tone === 'red') {
    actions.push('建议立即上报上级领导，并形成高风险专题分析报告。');
    actions.push('同步安排现场复核和运维检查，确认是否需要临时管控措施。');
  } else if (tone === 'orange') {
    actions.push('建议安排专项复测和重点巡检，形成风险跟踪记录。');
    actions.push('运维侧同步核查设备健康、阈值与远程控制能力。');
  } else if (tone === 'yellow') {
    actions.push('建议提高观测频率，继续跟踪趋势是否持续恶化。');
  } else {
    actions.push('当前可维持日常监测，但应继续保留完整日志和属性快照。');
  }

  return actions;
}

// 导航
const navigateTo = (path: string) => {
  router.push(path);
};

// 刷新所有数据
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
    ElMessage.success(`风险点 ${deviceCode.value} 工作台刷新成功`);

    recordActivity({
      module: '风险点工作台',
      action: '刷新工作台',
      request: { deviceCode: deviceCode.value },
      response: {
        device: deviceResponse.data,
        properties: propertyResponse.data.length,
        logs: logResponse.data.length
      },
      ok: true,
      detail: `风险点 ${deviceCode.value} 刷新完成，属性 ${propertyResponse.data.length} 条，日志 ${logResponse.data.length} 条`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: '风险点工作台',
      action: '刷新工作台',
      request: { deviceCode: deviceCode.value },
      response: { message: errorMessage.value },
      ok: false,
      detail: `刷新失败：${errorMessage.value}`
    });
  } finally {
    isLoading.value = false;
  }
}

// 生命周期
onMounted(() => {
  recordActivity({
    module: '风险点工作台',
    action: '访问工作台',
    request: { path: '/insight' },
    ok: true,
    detail: '用户访问风险点工作台'
  });
});
</script>

<style scoped>
.risk-workbench-page {
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
    linear-gradient(140deg, rgba(8, 13, 28, 0.95), rgba(5, 9, 18, 0.88)),
    radial-gradient(circle at 85% 20%, rgba(57, 241, 255, 0.16), transparent 28%);
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
  --el-radio-button-checked-bg-color: rgba(57, 241, 255, 0.1);
  --el-radio-button-checked-border-color: var(--brand-bright);
}

:deep(.el-radio-button__inner) {
  background: rgba(8, 13, 26, 0.9);
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
  background: rgba(57, 241, 255, 0.1);
  border-color: var(--brand-bright);
  box-shadow: 0 0 12px rgba(57, 241, 255, 0.3);
}

/* 风险等级横幅 */
.risk-banner {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(82, 174, 255, 0.24);
  background:
    linear-gradient(160deg, rgba(10, 18, 38, 0.94), rgba(7, 12, 25, 0.88)),
    radial-gradient(circle at top right, rgba(44, 227, 255, 0.12), transparent 50%);
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

/* 风险研判区域 */
.risk-analysis {
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(160deg, rgba(10, 18, 38, 0.94), rgba(7, 12, 25, 0.88)),
    radial-gradient(circle at top right, rgba(44, 227, 255, 0.12), transparent 50%);
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
  background: rgba(7, 12, 22, 0.88);
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
  background: rgba(57, 241, 255, 0.08);
  color: var(--brand-bright);
}

.reason-desc {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

/* 角色快捷入口 */
.role-quick-access {
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(140deg, rgba(8, 13, 28, 0.95), rgba(5, 9, 18, 0.88)),
    radial-gradient(circle at 85% 20%, rgba(57, 241, 255, 0.16), transparent 28%);
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
  background: rgba(7, 12, 22, 0.88);
  cursor: pointer;
  transition: all 180ms ease;
}

.action-card:hover {
  border-color: var(--brand-bright);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(57, 241, 255, 0.15);
}

.action-icon {
  width: 3rem;
  height: 3rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.75rem;
  background: rgba(57, 241, 255, 0.12);
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

/* 属性趋势预览 */
.property-trend-section {
  padding: 1rem;
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
    linear-gradient(140deg, rgba(8, 13, 28, 0.95), rgba(5, 9, 18, 0.88)),
    radial-gradient(circle at 85% 20%, rgba(57, 241, 255, 0.16), transparent 28%);
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
  background: rgba(7, 12, 22, 0.88);
}

.action-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 2.2rem;
  height: 2.2rem;
  border-radius: 0.9rem;
  background: rgba(43, 227, 255, 0.12);
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
  background: rgba(7, 12, 22, 0.88);
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

.highlight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.highlight-card {
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: rgba(7, 12, 22, 0.88);
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

/* 消息日志面板 */
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
  background: rgba(7, 12, 22, 0.88);
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
