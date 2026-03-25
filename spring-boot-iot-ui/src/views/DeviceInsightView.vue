<template>
  <div class="page-stack">
    <section class="hero-grid">
      <div class="hero-panel risk-workbench">
        <p class="eyebrow">Risk Workbench</p>
        <h1 class="headline">围绕风险点完成监测、判级、上报与联调</h1>
        <p class="lead">
          当前工作台基于现有设备、属性和消息日志接口，先形成一线人员、运维人员和开发人员都能直接使用的
          风险点视图。后续可继续接入 AI 分析、告警工单和报告导出。
        </p>

        <form @submit.prevent="refreshAll">
          <div class="form-grid">
            <div class="field-group">
              <label for="insight-device-code">风险点设备编码</label>
              <el-input
                id="insight-device-code"
                v-model="deviceCode"
                name="insight_device_code"
                placeholder="例如 demo-device-01..."
                clearable
              />
            </div>
          </div>
          <StandardActionGroup margin-top="sm">
            <StandardButton action="confirm" native-type="submit" :loading="isLoading">
              {{ isLoading ? '刷新中...' : '刷新对象洞察' }}
            </StandardButton>
            <StandardButton action="reset" @click="jumpToReporting">
              进入链路验证中心
            </StandardButton>
            <StandardButton action="reset" @click="goToMessageTrace">
              链路追踪台
            </StandardButton>
            <StandardButton action="reset" plain @click="jumpToDevices">
              查看设备资产
            </StandardButton>
          </StandardActionGroup>
        </form>

        <div class="risk-banner" :class="`risk-banner--${riskSummary.tone}`">
          <div>
            <p>当前风险等级</p>
            <strong>{{ riskSummary.label }}</strong>
            <span>{{ riskSummary.description }}</span>
          </div>
          <div class="risk-banner__score">
            <small>风险评分</small>
            <strong>{{ riskSummary.score }}</strong>
          </div>
        </div>
      </div>

      <PanelCard
        eyebrow="Instant Focus"
        title="当前建议动作"
        description="优先把数据结果翻译成现场能执行的动作，帮助一线人员第一时间判断是否需要上报。"
      >
        <div class="focus-list">
          <article v-for="item in riskSummary.actions" :key="item" class="focus-list__item">
            <span class="focus-list__badge">{{ riskSummary.shortLabel }}</span>
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
        :hint="metric.hint"
        :badge="metric.badge"
      />
    </section>

    <div v-if="errorMessage" class="empty-state" aria-live="polite">{{ errorMessage }}</div>

    <section class="two-column-grid">
      <PanelCard
        eyebrow="Point Profile"
        title="风险点基础档案"
        description="把设备基础信息整理成风险点视图，方便一线、运维和研发在同一页交流。"
      >
        <el-descriptions v-if="device" :column="2" border class="descriptions-block">
          <el-descriptions-item label="设备名称">{{ device.deviceName }}</el-descriptions-item>
          <el-descriptions-item label="设备编码">{{ device.deviceCode }}</el-descriptions-item>
          <el-descriptions-item label="在线状态">
            <el-tag :type="device.onlineStatus === 1 ? 'success' : 'info'">
              {{ statusLabel(device.onlineStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="接入协议">{{ device.protocolCode || '--' }}</el-descriptions-item>
          <el-descriptions-item label="最近在线时间">{{ formatDateTime(device.lastOnlineTime) }}</el-descriptions-item>
          <el-descriptions-item label="最近离线时间">{{ formatDateTime(device.lastOfflineTime) }}</el-descriptions-item>
          <el-descriptions-item label="最近上报时间">{{ formatDateTime(device.lastReportTime) }}</el-descriptions-item>
          <el-descriptions-item label="固件版本">{{ device.firmwareVersion || '--' }}</el-descriptions-item>
          <el-descriptions-item label="部署位置">{{ device.address || '--' }}</el-descriptions-item>
          <el-descriptions-item label="最近抓取时间">{{ formatDateTime(lastFetchTime) }}</el-descriptions-item>
        </el-descriptions>
        <div v-else class="empty-state">请输入设备编码并刷新，查看该风险点的设备档案。</div>
      </PanelCard>

      <PanelCard
        eyebrow="Risk Reasons"
        title="风险研判依据"
        description="先基于现有平台数据做轻量规则判定，后续可以平滑接入 AI 风险分析和趋势模型。"
      >
        <div class="reason-list">
          <article v-for="reason in riskSummary.reasons" :key="reason.title" class="reason-list__item">
            <header>
              <strong>{{ reason.title }}</strong>
              <span>{{ reason.tag }}</span>
            </header>
            <p>{{ reason.description }}</p>
          </article>
        </div>
      </PanelCard>
    </section>

    <PropertyTrendPanel :logs="logs" />

    <section class="tri-grid">
      <PanelCard
        eyebrow="Field Action"
        title="一线人员建议"
        description="帮助现场人员快速决定是否上报、是否复测、是否形成书面报告。"
      >
        <ul class="advice-list">
          <li v-for="item in fieldActions" :key="item">{{ item }}</li>
        </ul>
      </PanelCard>

      <PanelCard
        eyebrow="O&M Action"
        title="运维维护建议"
        description="帮助运维人员判断设备、网络、阈值与远程控制的下一步动作。"
      >
        <ul class="advice-list">
          <li v-for="item in operationActions" :key="item">{{ item }}</li>
        </ul>
      </PanelCard>

      <PanelCard
        eyebrow="Dev Action"
        title="研发调试建议"
        description="帮助开发与实施人员快速回看协议、日志与设备接入链路。"
      >
        <ul class="advice-list">
          <li v-for="item in engineeringActions" :key="item">{{ item }}</li>
        </ul>
      </PanelCard>
    </section>

    <section class="two-column-grid">
      <PanelCard
        eyebrow="Key Properties"
        title="关键监测指标"
        description="优先展示最值得关注的最新属性，方便风险判断和现场汇报。"
      >
        <div v-if="propertyHighlights.length" class="highlight-grid">
          <article v-for="item in propertyHighlights" :key="item.identifier" class="highlight-card">
            <span>{{ item.identifier }}</span>
            <strong>{{ item.propertyValue || '--' }}</strong>
            <small>{{ item.valueType || 'unknown' }}</small>
            <p>{{ formatDateTime(item.updateTime || item.reportTime) }}</p>
          </article>
        </div>
        <el-empty v-else description="还没有属性数据。先通过 HTTP 或 MQTT 发送一条上报。" />
      </PanelCard>

      <PanelCard
        eyebrow="Report Draft"
        title="风险分析报告草稿"
        description="先给出一份可复制的报告提纲，后续可以升级成 Word / PDF 正式报告。"
      >
        <div class="report-draft">
          <p><strong>风险点：</strong>{{ device?.deviceName || deviceCode || '--' }}</p>
          <p><strong>当前等级：</strong>{{ riskSummary.label }}</p>
          <p><strong>判定摘要：</strong>{{ reportDraft.summary }}</p>
          <p><strong>建议动作：</strong>{{ reportDraft.actions }}</p>
          <p><strong>后续关注：</strong>{{ reportDraft.followUp }}</p>
        </div>
      </PanelCard>
    </section>

    <section class="two-column-grid">
      <PanelCard
        eyebrow="Latest Properties"
        title="设备属性快照"
        description="来自 `GET /api/device/{deviceCode}/properties`，既可服务业务判断，也可服务调试核查。"
      >
        <el-table v-if="properties.length" :data="properties" stripe>
          <StandardTableTextColumn prop="identifier" label="标识符" :min-width="140" />
          <StandardTableTextColumn prop="propertyName" label="属性名" :min-width="140">
            <template #default="{ row }">{{ row.propertyName || '--' }}</template>
          </StandardTableTextColumn>
          <StandardTableTextColumn prop="propertyValue" label="值" :min-width="140">
            <template #default="{ row }">{{ row.propertyValue || '--' }}</template>
          </StandardTableTextColumn>
          <StandardTableTextColumn prop="valueType" label="类型" :min-width="120">
            <template #default="{ row }">{{ row.valueType || '--' }}</template>
          </StandardTableTextColumn>
          <StandardTableTextColumn label="更新时间" :min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.updateTime || row.reportTime) }}</template>
          </StandardTableTextColumn>
        </el-table>
        <el-empty v-else description="还没有属性数据。先去“链路验证中心”发送一条属性报文。" />
      </PanelCard>

      <PanelCard
        eyebrow="Message Logs"
        title="消息日志与审计回看"
        description="来自 `GET /api/device/{deviceCode}/message-logs`，可快速回看 topic、payload、TraceId 与最近链路行为。"
      >
        <div v-if="logs.length" class="timeline">
          <article v-for="item in logs" :key="item.id" class="timeline-item">
            <h3>{{ item.messageType || 'unknown' }}</h3>
            <p>{{ item.topic || '--' }}</p>
            <p>TraceId：{{ item.traceId || '--' }}</p>
            <p>{{ truncateText(item.payload || '--', 160) }}</p>
            <p>{{ formatDateTime(item.reportTime || item.createTime) }}</p>
          </article>
        </div>
        <el-empty v-else description="还没有日志数据。发送报文后再回来刷新即可。" />
      </PanelCard>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';

import { getDeviceByCode, getDeviceMessageLogs, getDeviceProperties } from '../api/iot';
import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import PropertyTrendPanel from '../components/PropertyTrendPanel.vue';
import StandardActionGroup from '../components/StandardActionGroup.vue';
import StandardTableTextColumn from '../components/StandardTableTextColumn.vue';
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

const riskSummary = computed<RiskSummary>(() => {
  let score = 0;
  const reasons: RiskReason[] = [];

  if (!device.value) {
    return {
      score: '--',
      label: '待加载',
      shortLabel: '待',
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

const overviewMetrics = computed(() => [
  {
    label: '当前风险等级',
    value: riskSummary.value.label,
    hint: riskSummary.value.description,
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
    hint: device.value?.onlineStatus === 1 ? '当前设备在线，可持续获取实时数据。' : '当前设备离线，建议优先核查设备和网络状态。',
    badge: {
      label: device.value?.onlineStatus === 1 ? '在线' : '离线',
      tone: device.value?.onlineStatus === 1 ? 'success' : 'muted'
    }
  },
  {
    label: '属性快照数',
    value: String(properties.value.length),
    hint: properties.value.length ? '当前已具备最新属性，可用于风险判定与展示。' : '当前没有属性快照，建议先验证上报链路。',
    badge: { label: 'Property', tone: 'brand' }
  },
  {
    label: '消息日志数',
    value: String(logs.value.length),
    hint: logs.value.length ? '当前可回看最新 topic 与 payload。' : '当前没有可用日志，研发和实施侧审计信息不足。',
    badge: { label: 'Audit', tone: 'success' }
  }
]);

const propertyHighlights = computed(() => properties.value.slice(0, 6));

const fieldActions = computed(() => {
  const base = [...riskSummary.value.actions];
  if (riskSummary.value.tone === 'red' || riskSummary.value.tone === 'orange') {
    base.push('建议立即整理当前属性、趋势图与现场照片，形成专题风险报告。');
  } else {
    base.push('继续关注风险趋势，必要时提高巡检频率并补充人工巡检记录。');
  }
  return base;
});

const operationActions = computed(() => {
  const actions = ['核查设备在线、供电、网络和最近上报时效。'];
  if (suspiciousPropertySignals.value.length) {
    actions.push('根据异常属性评估是否需要调整阈值或远程下发配置。');
  }
  if (device.value?.onlineStatus !== 1) {
    actions.push('优先排查现场供电与链路问题，必要时安排设备重启或替换。');
  } else {
    actions.push('设备在线稳定，可继续观察文件、固件和会话状态。');
  }
  return actions;
});

const engineeringActions = computed(() => {
  const actions = ['回看消息日志中的 topic 与 payload，确认协议解析是否符合预期。'];
  if (!logs.value.length) {
    actions.push('当前无日志，建议先通过链路验证中心发送模拟报文验证主链路。');
  } else {
    actions.push('可进入“链路追踪台”按 TraceId 串联异常观测台与消息日志，快速定位接入异常。');
  }
  actions.push('若涉及文件或固件数据，请继续进入文件与固件调试页查看 Redis 聚合结果。');
  return actions;
});

const reportDraft = computed(() => ({
  summary: riskSummary.value.description,
  actions: riskSummary.value.actions.join('；'),
  followUp: riskSummary.value.tone === 'red' || riskSummary.value.tone === 'orange'
    ? '建议 24 小时内持续复测，并跟踪风险等级是否进一步提升。'
    : '建议维持日常观测，重点关注新的异常属性或上报时效变化。'
}));

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

function jumpToReporting() {
  router.push({
    path: '/reporting',
    query: {
      deviceCode: deviceCode.value
    }
  });
}

function jumpToDevices() {
  router.push({
    path: '/devices',
    query: {
      deviceCode: deviceCode.value
    }
  });
}

function goToMessageTrace() {
  router.push({
    path: '/message-trace',
    query: {
      deviceCode: deviceCode.value || undefined
    }
  });
}

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
</script>

<style scoped>
.risk-workbench {
  display: grid;
  gap: 1.35rem;
}

.risk-banner {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  padding: 1rem 1.1rem;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--brand) 22%, transparent);
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand) 6%, white));
  box-shadow: var(--shadow-inset-outline-52);
}

.risk-banner p,
.risk-banner strong,
.risk-banner span,
.risk-banner small {
  display: block;
}

.risk-banner p,
.risk-banner small {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 0.76rem;
  text-transform: uppercase;
  letter-spacing: 0.14em;
}

.risk-banner strong {
  margin-top: 0.45rem;
  font-size: 1.55rem;
  color: var(--text-primary);
}

.risk-banner span {
  margin-top: 0.35rem;
  color: var(--text-secondary);
  line-height: 1.7;
}

.risk-banner__score {
  text-align: right;
}

.risk-banner__score strong {
  font-size: 2.2rem;
}

.risk-banner--red {
  border-color: color-mix(in srgb, var(--danger) 32%, transparent);
}

.risk-banner--orange {
  border-color: color-mix(in srgb, var(--warning) 32%, transparent);
}

.risk-banner--yellow {
  border-color: color-mix(in srgb, #faad14 32%, transparent);
}

.risk-banner--blue {
  border-color: color-mix(in srgb, var(--accent) 32%, transparent);
}

.focus-list,
.reason-list,
.highlight-grid {
  display: grid;
  gap: 0.85rem;
}

.focus-list__item,
.reason-list__item,
.highlight-card,
.report-draft {
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand) 4%, white));
  box-shadow: var(--shadow-sm);
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
  background: color-mix(in srgb, var(--brand) 12%, transparent);
  color: var(--brand-deep);
  font-family: var(--font-mono);
}

.focus-list__item p {
  margin: 0;
  line-height: 1.7;
  color: var(--text-secondary);
}

.descriptions-block {
  margin-top: 0.2rem;
}

.reason-list__item header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.reason-list__item header span {
  color: var(--text-tertiary);
  font-size: 0.76rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.reason-list__item p {
  margin: 0.55rem 0 0;
  line-height: 1.7;
  color: var(--text-secondary);
}

.advice-list {
  margin: 0;
  padding-left: 1.1rem;
  line-height: 1.9;
}

.highlight-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.highlight-card span,
.highlight-card strong,
.highlight-card small,
.highlight-card p {
  display: block;
}

.highlight-card span,
.highlight-card small {
  color: var(--text-tertiary);
}

.highlight-card strong {
  margin-top: 0.45rem;
  font-size: 1.35rem;
  color: var(--text-primary);
}

.highlight-card p {
  margin: 0.55rem 0 0;
  color: var(--text-secondary);
}

.report-draft p {
  margin: 0;
  line-height: 1.8;
  color: var(--text-secondary);
}

.report-draft p + p {
  margin-top: 0.5rem;
}

@media (max-width: 1200px) {
  .highlight-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .risk-banner {
    flex-direction: column;
    align-items: flex-start;
  }

  .risk-banner__score {
    text-align: left;
  }

  .highlight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
