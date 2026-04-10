<template>
  <div class="cockpit-page">
    <section class="cockpit-hero">
      <div class="cockpit-hero__main">
        <h1>风险运营驾驶舱</h1>
        <p class="cockpit-hero__desc">
          首页聚焦关键风险指标、处置效率和系统运行状态；当前账号按“{{ permissionStore.roleProfile.focusLabel }}”优先组织入口，执行动作下沉到 `接入智维 / 风险运营 / 风险策略 / 平台治理 / 质量工场` 五大工作台，避免“首页即操作台”的信息噪音。
        </p>
      </div>
      <div class="cockpit-hero__clock">
        <span>平台时间</span>
        <strong>{{ currentTime }}</strong>
        <small>{{ dataSourceHint }}</small>
      </div>
    </section>

    <section class="role-tabs" aria-label="角色视角">
      <button
        v-for="role in roleOptions"
        :key="role.key"
        type="button"
        class="role-tabs__item"
        :class="{ 'role-tabs__item--active': role.key === activeRole }"
        @click="switchRole(role.key)"
      >
        <strong>{{ role.label }}</strong>
        <span>{{ role.caption }}</span>
      </button>
    </section>

    <section class="kpi-grid">
      <MetricCard
        v-for="metric in activePreset.kpis"
        :key="metric.label"
        :label="metric.label"
        :value="metric.value"
        :badge="metric.badge"
      />
    </section>

    <section class="dashboard-grid">
      <PanelCard
        title="角色关键维度"
        :description="activePreset.emphasis"
      >
        <div class="focus-grid">
          <article v-for="item in activePreset.focusDimensions" :key="item.title" class="focus-card">
            <span>{{ item.title }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.description }}</p>
            <small :class="`trend trend--${item.trendTone}`">{{ item.trend }}</small>
          </article>
        </div>
      </PanelCard>

      <PanelCard
        title="关键队列状态"
        description="按风险等级和处置时效展示当前最需要关注的队列。"
      >
        <div class="queue-list">
          <article v-for="item in activePreset.queues" :key="item.label" class="queue-item">
            <div class="queue-item__head">
              <strong>{{ item.label }}</strong>
              <span :class="`badge badge--${item.tone}`">{{ item.tag }}</span>
            </div>
            <div class="queue-item__body">
              <span class="queue-item__value">{{ item.value }}</span>
              <small>{{ item.hint }}</small>
            </div>
            <div class="queue-item__bar">
              <i :style="{ width: `${item.percent}%` }" />
            </div>
          </article>
        </div>
      </PanelCard>
    </section>

    <section class="dashboard-grid">
      <PanelCard
        title="角色待办"
        description="把首页当作值守与管理调度入口，优先处理高风险与超时事项。"
      >
        <div class="todo-list">
          <article
            v-for="todo in activePreset.todos"
            :key="todo.id"
            class="todo-item"
            :class="`todo-item--${todo.tone}`"
          >
            <div class="todo-item__meta">
              <span>{{ todo.priority }}</span>
              <small>{{ todo.window }}</small>
            </div>
            <strong>{{ todo.title }}</strong>
            <p>{{ todo.detail }}</p>
            <button type="button" @click="navigate(todo.path)">{{ todo.actionLabel }}</button>
          </article>
        </div>
      </PanelCard>

      <PanelCard
        title="事务工作台入口"
        description="工作台保留为执行层窗口：调试、联调、实施和治理操作在这里展开。"
      >
        <div class="workbench-grid">
          <button
            v-for="entry in workbenchEntries"
            :key="entry.title"
            type="button"
            class="workbench-entry"
            @click="navigate(entry.path)"
          >
            <strong>{{ entry.title }}</strong>
            <p>{{ entry.description }}</p>
            <span>{{ entry.caption }}</span>
          </button>
        </div>

        <div v-if="latestActivity" class="activity-trace">
          <span>最近操作痕迹</span>
          <strong>{{ latestActivity.title }}</strong>
          <p>{{ latestActivity.detail }}</p>
          <small>{{ latestActivity.time }}</small>
        </div>
      </PanelCard>
    </section>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import {
  getAlarmStatistics,
  getDeviceHealthAnalysis,
  getEventClosureAnalysis,
  getRiskTrendAnalysis
} from '../api/report';
import { getRiskGovernanceDashboardOverview, type RiskGovernanceDashboardOverview } from '../api/riskGovernance';
import { activityEntries, recordActivity } from '../stores/activity';
import { usePermissionStore } from '../stores/permission';
import { sortByPreferredPaths } from '../utils/sectionWorkspaces';

type BadgeTone = 'success' | 'warning' | 'danger' | 'muted' | 'brand';
type RoleKey = 'frontline' | 'ops' | 'manager' | 'rd';
type TrendTone = 'up' | 'down' | 'stable';
type QueueTone = 'danger' | 'warning' | 'brand' | 'muted';
type TodoTone = 'danger' | 'warning' | 'brand' | 'muted';

interface MetricItem {
  label: string;
  value: string;
  badge?: {
    label: string;
    tone: BadgeTone;
  };
}

interface FocusDimension {
  title: string;
  value: string;
  description: string;
  trend: string;
  trendTone: TrendTone;
}

interface QueueItem {
  label: string;
  value: string;
  hint: string;
  percent: number;
  tag: string;
  tone: QueueTone;
}

interface TodoItem {
  id: string;
  priority: string;
  window: string;
  title: string;
  detail: string;
  actionLabel: string;
  tone: TodoTone;
  path: string;
}

interface RolePreset {
  key: RoleKey;
  label: string;
  caption: string;
  emphasis: string;
  kpis: MetricItem[];
  focusDimensions: FocusDimension[];
  queues: QueueItem[];
  todos: TodoItem[];
}

const router = useRouter();
const permissionStore = usePermissionStore();
const activeRole = ref<RoleKey>('frontline');
const currentTime = ref(formatNow());
const dataSourceState = ref<'loading' | 'live' | 'fallback'>('loading');
const dashboardUpdatedAt = ref('');
let timer: number | null = null;

const rolePresets: RolePreset[] = [
  {
    key: 'frontline',
    label: '一线工作人员',
    caption: '当班响应、确认与接力',
    emphasis: '关注告警响应速度、事件接力完整性与现场闭环效率。',
    kpis: [
      { label: '待确认告警', value: '18', badge: { label: '紧急', tone: 'danger' } },
      { label: '待接收工单', value: '7', badge: { label: '优先', tone: 'warning' } },
      { label: '巡检完成率', value: '92%', badge: { label: '达标', tone: 'success' } },
      { label: '平均响应时长', value: '4.8 分钟', badge: { label: '较昨日 -6%', tone: 'brand' } }
    ],
    focusDimensions: [
      { title: '高风险点实时状态', value: '12 / 15 在线', description: '重点区域风险点在线与最新上报状态。', trend: '稳定', trendTone: 'stable' },
      { title: '超时未确认告警', value: '3', description: '超过 10 分钟仍未确认的告警条目。', trend: '-1', trendTone: 'up' },
      { title: '事件接力成功率', value: '96%', description: '派发到接收的链路完成率。', trend: '+2%', trendTone: 'up' },
      { title: '现场反馈完整率', value: '88%', description: '反馈文本/图片/结果字段完整度。', trend: '+4%', trendTone: 'up' }
    ],
    queues: [
      { label: '告警确认队列', value: '18', hint: '需 10 分钟内确认', percent: 74, tag: '高优先', tone: 'danger' },
      { label: '事件接力队列', value: '11', hint: '待派发/待接收', percent: 58, tag: '处理中', tone: 'warning' },
      { label: '巡检异常队列', value: '5', hint: '未完成复核', percent: 36, tag: '跟进', tone: 'brand' }
    ],
    todos: [
      { id: 'fl-1', priority: 'P1', window: '立即处理', title: '确认 3 条一级告警', detail: '涉及 2 个高风险点，需先确认并判定是否派单。', actionLabel: '进入告警运营台', tone: 'danger', path: '/alarm-center' },
      { id: 'fl-2', priority: 'P2', window: '30 分钟内', title: '完成事件工单接力', detail: '夜班遗留 2 条事件等待接收与开始。', actionLabel: '进入事件协同台', tone: 'warning', path: '/event-disposal' },
      { id: 'fl-3', priority: 'P3', window: '本班次内', title: '核对重点风险点状态', detail: '检查离线设备是否存在持续上报中断。', actionLabel: '进入风险对象中心', tone: 'brand', path: '/risk-point' }
    ]
  },
  {
    key: 'ops',
    label: '运维人员',
    caption: '稳定性、规则质量与 SLA',
    emphasis: '关注系统稳定、规则有效和跨班协同效率，保证处置链路连续。',
    kpis: [
      { label: '设备在线率', value: '94.6%', badge: { label: '目标 95%', tone: 'warning' } },
      { label: '规则命中有效率', value: '89%', badge: { label: '可优化', tone: 'brand' } },
      { label: '平均闭环时长', value: '2.3 小时', badge: { label: '较昨日 -12%', tone: 'success' } },
      { label: 'SLA 违约事件', value: '2', badge: { label: '需督办', tone: 'danger' } }
    ],
    focusDimensions: [
      { title: '设备可用率', value: '1,284 / 1,358', description: '当前在线设备与离线设备分布。', trend: '+0.7%', trendTone: 'up' },
      { title: '规则误报比例', value: '7.8%', description: '需复盘阈值配置与告警抑制规则。', trend: '-0.9%', trendTone: 'up' },
      { title: '工单超时率', value: '4.2%', description: '超时工单比例，反映协同效率。', trend: '-0.5%', trendTone: 'up' },
      { title: '处置回填完整率', value: '91%', description: '工单回填字段完整度。', trend: '+1.6%', trendTone: 'up' }
    ],
    queues: [
      { label: '离线设备排查', value: '74', hint: '按区域聚合处置', percent: 68, tag: '重点', tone: 'warning' },
      { label: '规则复核队列', value: '9', hint: '误报偏高规则', percent: 46, tag: '优化', tone: 'brand' },
      { label: 'SLA 违约队列', value: '2', hint: '跨班督办', percent: 24, tag: '风险', tone: 'danger' }
    ],
    todos: [
      { id: 'ops-1', priority: 'P1', window: '今天', title: '排查离线设备聚集区域', detail: '东区出现 16 台设备离线，需确认网络与电源。', actionLabel: '进入设备资产中心', tone: 'warning', path: '/devices' },
      { id: 'ops-2', priority: 'P2', window: '今天', title: '复核高误报阈值策略', detail: '3 条阈值策略命中频率异常偏高。', actionLabel: '进入阈值策略', tone: 'brand', path: '/rule-definition' },
      { id: 'ops-3', priority: 'P2', window: '48 小时', title: '督办超时工单闭环', detail: '2 条事件工单接近 SLA 红线。', actionLabel: '进入事件协同台', tone: 'danger', path: '/event-disposal' }
    ]
  },
  {
    key: 'manager',
    label: '管理人员',
    caption: '治理经营、闭环覆盖与积压调度',
    emphasis: '关注治理覆盖率、发布进度与待办积压，支撑经营视角的资源决策。',
    kpis: [
      { label: '治理完成率', value: '--', badge: { label: '治理产品 / 总产品', tone: 'brand' } },
      { label: '风险指标绑定覆盖率', value: '--', badge: { label: '目录指标 -> 风险点绑定', tone: 'success' } },
      { label: '联动预案覆盖率', value: '--', badge: { label: '绑定测点 -> 联动 + 预案', tone: 'warning' } },
      { label: '平均接入耗时', value: '--', badge: { label: '建档到首发合同', tone: 'muted' } }
    ],
    focusDimensions: [
      { title: '已治理产品', value: '--', description: '至少进入发布批次或风险指标目录的产品。', trend: '待同步', trendTone: 'stable' },
      { title: '平均接入耗时', value: '--', description: '新产品从建档到首个合同发布的平均耗时。', trend: '待同步', trendTone: 'stable' },
      { title: '卡点分布', value: '--', description: '按六类治理任务统计的主要卡点占比。', trend: '待同步', trendTone: 'stable' },
      { title: '待运营复盘', value: '--', description: '按风险指标维度汇总的缺策略复盘事项。', trend: '待同步', trendTone: 'stable' }
    ],
    queues: [
      { label: '待治理产品', value: '--', hint: '尚未进入治理主链路', percent: 0, tag: '治理', tone: 'warning' },
      { label: '待发布合同', value: '--', hint: '样本已治理但未形成正式合同', percent: 0, tag: '发布', tone: 'warning' },
      { label: '待绑定风险点', value: '--', hint: '设备已上报但未绑定风险对象', percent: 0, tag: '纳管', tone: 'danger' },
      { label: '待补阈值策略', value: '--', hint: '风险点已绑定但缺阈值策略覆盖', percent: 0, tag: '策略', tone: 'brand' },
      { label: '待补联动预案', value: '--', hint: '联动编排/应急预案覆盖不足', percent: 0, tag: '编排', tone: 'brand' },
      { label: '待运营复盘', value: '--', hint: '风险指标维度复盘未收口', percent: 0, tag: '复盘', tone: 'warning' }
    ],
    todos: [
      { id: 'mg-1', priority: 'P1', window: '今日例会前', title: '处理待治理产品', detail: '优先补齐治理积压产品的契约发布与目录发布。', actionLabel: '进入产品定义中心', tone: 'warning', path: '/products' },
      { id: 'mg-2', priority: 'P1', window: '今日例会前', title: '推进待发布合同', detail: '优先让已完成样本治理的产品进入正式发布批次。', actionLabel: '进入治理任务台', tone: 'warning', path: '/governance-task?workStatus=OPEN&workItemCode=PENDING_CONTRACT_RELEASE' },
      { id: 'mg-3', priority: 'P1', window: '今日', title: '收口风险绑定缺口', detail: '推进已上报设备完成风险点绑定，进入正式纳管。', actionLabel: '进入治理任务台', tone: 'danger', path: '/governance-task?workStatus=OPEN&workItemCode=PENDING_RISK_BINDING' },
      { id: 'mg-4', priority: 'P2', window: '今日', title: '补齐阈值策略', detail: '优先处理已绑定风险点但尚未配置阈值策略的指标。', actionLabel: '进入阈值策略', tone: 'brand', path: '/rule-definition' },
      { id: 'mg-5', priority: 'P2', window: '本周', title: '补齐联动与预案', detail: '联动规则和应急预案都需覆盖到已纳管指标。', actionLabel: '进入联动编排', tone: 'brand', path: '/linkage-rule' },
      { id: 'mg-6', priority: 'P2', window: '本周', title: '完成运营复盘', detail: '按 releaseBatch/trace 维度复盘缺策略事项并闭环。', actionLabel: '进入应急预案库', tone: 'warning', path: '/emergency-plan' }
    ]
  },
  {
    key: 'rd',
    label: '研发人员',
    caption: '链路质量、协议解析与发布稳定性',
    emphasis: '关注接入链路、解析稳定性和版本发布质量，为业务持续可用提供技术保障。',
    kpis: [
      { label: '上报成功率', value: '99.2%', badge: { label: '稳定', tone: 'success' } },
      { label: '协议解析异常', value: '6', badge: { label: '待排查', tone: 'warning' } },
      { label: '接口 5xx 告警', value: '1', badge: { label: '紧急', tone: 'danger' } },
      { label: '发布稳定性', value: '86', badge: { label: '可提升', tone: 'brand' } }
    ],
    focusDimensions: [
      { title: '接入主链路健康度', value: '98.9%', description: 'HTTP/MQTT 主链路端到端可用性。', trend: '稳定', trendTone: 'stable' },
      { title: '协议兼容通过率', value: '97.4%', description: '历史兼容报文解析成功率。', trend: '+0.8%', trendTone: 'up' },
      { title: '异常日志密度', value: '1.3 / 千次请求', description: '重点观察登录、风险监测与报表接口。', trend: '-0.2', trendTone: 'up' },
      { title: '回归通过率', value: '94%', description: '核心模块自动化回归通过比例。', trend: '+1.4%', trendTone: 'up' }
    ],
    queues: [
      { label: '协议异常队列', value: '6', hint: '含 legacy 报文', percent: 43, tag: '兼容性', tone: 'warning' },
      { label: '接口错误队列', value: '1', hint: '优先止损', percent: 22, tag: '高优先', tone: 'danger' },
      { label: '发布后观察项', value: '8', hint: '灰度监控', percent: 58, tag: '观察', tone: 'brand' }
    ],
    todos: [
      { id: 'rd-1', priority: 'P1', window: '立即', title: '定位 1 条接口 5xx 告警', detail: '优先核查认证、接入与风险监测链路的系统日志。', actionLabel: '进入异常观测台', tone: 'danger', path: '/system-log' },
      { id: 'rd-2', priority: 'P2', window: '今天', title: '复盘协议解析异常样本', detail: '检查 legacy 报文兼容解析和安全校验。', actionLabel: '进入链路验证中心', tone: 'warning', path: '/reporting' },
      { id: 'rd-3', priority: 'P3', window: '本周', title: '回归关键业务流程', detail: '覆盖告警、事件、规则、报表与登录链路。', actionLabel: '进入对象洞察台', tone: 'brand', path: '/insight' }
    ]
  }
];

const workbenchCatalog = [
  { title: '告警运营台', description: '告警确认、抑制与关闭处理。', caption: '风险运营', path: '/alarm-center' },
  { title: '事件协同台', description: '工单派发、接收、闭环与反馈。', caption: '事件闭环', path: '/event-disposal' },
  { title: '治理任务台', description: '统一查看待发布合同、风险绑定等控制面待办。', caption: '控制面', path: '/governance-task' },
  { title: '治理运维台', description: '统一查看字段漂移、合同差异和指标缺失告警。', caption: '控制面', path: '/governance-ops' },
  { title: '风险对象中心', description: '风险点台账、设备绑定与等级维护。', caption: '风险策略', path: '/risk-point' },
  { title: '运营分析中心', description: '风险趋势、告警统计和设备健康分析。', caption: '经营分析', path: '/report-analysis' },
  { title: '设备资产中心', description: '设备建档、在线态与基础运维。', caption: '实施支撑', path: '/devices' },
  { title: '链路验证中心', description: 'HTTP 上报链路与协议联调。', caption: '研发联调', path: '/reporting' }
];

const roleOptions = computed(() => rolePresets.map(({ key, label, caption }) => ({ key, label, caption })));

const activePreset = computed(() => rolePresets.find((item) => item.key === activeRole.value) || rolePresets[0]);
const workbenchEntries = computed(() => {
  const accessibleEntries = workbenchCatalog.filter((entry) => permissionStore.hasRoutePermission(entry.path));
  return sortByPreferredPaths(accessibleEntries, permissionStore.roleProfile.featuredPaths).slice(0, 6);
});

const latestActivity = computed(() => {
  const latest = activityEntries.value[0];
  if (!latest) {
    return null;
  }
  return {
    title: latest.title,
    detail: latest.detail,
    time: formatActivityTime(latest.createdAt)
  };
});

const dataSourceHint = computed(() => {
  if (dataSourceState.value === 'loading') {
    return '数据源状态：正在加载真实统计数据...';
  }
  if (dataSourceState.value === 'live') {
    return `数据源状态：真实报表与治理聚合（最近同步：${dashboardUpdatedAt.value || '--'}）`;
  }
  return '数据源状态：真实接口不可用，当前显示稳定兜底口径';
});

function inferRoleFromAuth(): RoleKey {
  return permissionStore.roleProfile.cockpitRole;
}

function switchRole(role: RoleKey) {
  activeRole.value = role;
}

function navigate(path: string) {
  recordActivity({
    title: `驾驶舱跳转 · ${path}`,
    detail: `${activePreset.value.label}视角进入 ${path}`,
    tag: 'cockpit'
  });
  router.push(path);
}

function formatNow() {
  const now = new Date();
  const date = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
  const time = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
  return `${date} ${time}`;
}

function formatActivityTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function pad(value: number) {
  return value < 10 ? `0${value}` : String(value);
}

function formatPercent(value?: number | null, digits = 1) {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return '--';
  }
  return `${value.toFixed(digits)}%`;
}

function formatInteger(value?: number | null) {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return '--';
  }
  return String(Math.round(value));
}

function formatDurationHours(value?: number | null) {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return '--';
  }
  return `${value.toFixed(1)} 小时`;
}

function formatNamePreview(values?: Array<string | null | undefined> | null) {
  if (!Array.isArray(values)) {
    return '';
  }
  return values
    .map((item) => (typeof item === 'string' ? item.trim() : ''))
    .filter((item) => item.length > 0)
    .join(' / ');
}

function formatDateYmd(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function getDateRangeOfLast7Days() {
  const end = new Date();
  const start = new Date(end);
  start.setDate(end.getDate() - 6);
  return {
    startDate: formatDateYmd(start),
    endDate: formatDateYmd(end)
  };
}

function applyLiveMetrics(payload: {
  riskTrend: any[];
  alarmStats: any;
  eventStats: any;
  deviceHealth: any;
  governanceOverview: RiskGovernanceDashboardOverview | null;
}) {
  const { riskTrend, alarmStats, eventStats, deviceHealth, governanceOverview } = payload;

  const alarmTotal = Number(alarmStats?.total ?? 0);
  const alarmCritical = Number(alarmStats?.critical ?? 0);
  const alarmHigh = Number(alarmStats?.high ?? 0);
  const eventTotal = Number(eventStats?.total ?? 0);
  const eventClosed = Number(eventStats?.closed ?? 0);
  const eventUnclosed = Number(eventStats?.unclosed ?? 0);
  const closureRate = eventTotal > 0 ? (eventClosed / eventTotal) * 100 : 0;
  const onlineRate = Number(deviceHealth?.onlineRate ?? 0);

  const latestTrend = riskTrend[riskTrend.length - 1] || {};
  const previousTrend = riskTrend[riskTrend.length - 2] || {};
  const alarmTrendDelta = Number(latestTrend.alarmCount ?? 0) - Number(previousTrend.alarmCount ?? 0);
  const eventTrendDelta = Number(latestTrend.eventCount ?? 0) - Number(previousTrend.eventCount ?? 0);

  const frontline = rolePresets.find((item) => item.key === 'frontline');
  if (frontline) {
    frontline.kpis[0].value = formatInteger(alarmCritical + alarmHigh);
    frontline.kpis[1].value = formatInteger(eventUnclosed);
    frontline.kpis[2].value = formatPercent(closureRate, 0);
    frontline.kpis[3].value = `${formatPercent(onlineRate, 1)}`;
    frontline.kpis[3].badge = { label: '设备在线率', tone: 'brand' };

    frontline.queues[0].value = formatInteger(alarmTotal);
    frontline.queues[1].value = formatInteger(eventUnclosed);
    frontline.queues[2].value = formatInteger(Math.max(0, alarmTotal - eventClosed));
  }

  const ops = rolePresets.find((item) => item.key === 'ops');
  if (ops) {
    ops.kpis[0].value = formatPercent(onlineRate, 1);
    ops.kpis[1].value = formatPercent(closureRate, 0);
    ops.kpis[2].value = formatInteger(eventClosed);
    ops.kpis[3].value = formatInteger(eventUnclosed);
    ops.kpis[3].badge = { label: '待督办', tone: eventUnclosed > 0 ? 'danger' : 'success' };

    ops.queues[0].value = formatInteger(Math.max(0, Math.round((Number(deviceHealth?.total ?? 0) * (100 - onlineRate)) / 100)));
    ops.queues[1].value = formatInteger(alarmHigh + alarmCritical);
    ops.queues[2].value = formatInteger(eventUnclosed);
  }

  const manager = rolePresets.find((item) => item.key === 'manager');
  if (manager) {
    if (governanceOverview) {
      const totalProductCount = Number(governanceOverview.totalProductCount ?? 0);
      const governedProductCount = Number(governanceOverview.governedProductCount ?? 0);
      const releasedProductCount = Number(governanceOverview.releasedProductCount ?? 0);
      const pendingProductGovernanceCount = Number(governanceOverview.pendingProductGovernanceCount ?? 0);
      const pendingContractReleaseCount = Number(governanceOverview.pendingContractReleaseCount ?? 0);
      const pendingRiskBindingCount = Number(governanceOverview.pendingRiskBindingCount ?? 0);
      const pendingThresholdPolicyCount = Number(governanceOverview.pendingThresholdPolicyCount ?? governanceOverview.pendingPolicyCount ?? 0);
      const pendingLinkageCount = Number(governanceOverview.pendingLinkageCount ?? 0);
      const pendingEmergencyPlanCount = Number(governanceOverview.pendingEmergencyPlanCount ?? 0);
      const pendingLinkagePlanCount = Number(
        governanceOverview.pendingLinkagePlanCount ?? (pendingLinkageCount + pendingEmergencyPlanCount)
      );
      const pendingReplayCount = Number(governanceOverview.pendingReplayCount ?? 0);
      const rawStageVendorCount = Number(governanceOverview.rawStageVendorCount ?? 0);
      const rawStageProductCount = Number(governanceOverview.rawStageProductCount ?? 0);
      const rawStageVendorPreview = formatNamePreview(governanceOverview.rawStageVendorNames);
      const rawStageProductPreview = formatNamePreview(governanceOverview.rawStageProductNames);
      const publishedRiskMetricCount = Number(governanceOverview.publishedRiskMetricCount ?? 0);
      const boundRiskMetricCount = Number(governanceOverview.boundRiskMetricCount ?? 0);
      const ruleCoveredRiskMetricCount = Number(governanceOverview.ruleCoveredRiskMetricCount ?? 0);
      const governanceCompletionRate = Number(governanceOverview.governanceCompletionRate ?? 0);
      const metricBindingCoverageRate = Number(governanceOverview.metricBindingCoverageRate ?? 0);
      const linkagePlanCoverageRate = Number(governanceOverview.linkagePlanCoverageRate ?? 0);
      const averageOnboardingDurationHours = Number(governanceOverview.averageOnboardingDurationHours ?? 0);
      const backlogTotal = pendingProductGovernanceCount
        + pendingContractReleaseCount
        + pendingRiskBindingCount
        + pendingThresholdPolicyCount
        + pendingLinkagePlanCount
        + pendingReplayCount;
      const bottleneckBase = backlogTotal > 0 ? backlogTotal : 1;
      const bottleneckPendingProductGovernanceRate = Number(
        governanceOverview.bottleneckPendingProductGovernanceRate ?? ((pendingProductGovernanceCount * 100) / bottleneckBase)
      );
      const bottleneckPendingContractReleaseRate = Number(
        governanceOverview.bottleneckPendingContractReleaseRate ?? ((pendingContractReleaseCount * 100) / bottleneckBase)
      );
      const bottleneckPendingRiskBindingRate = Number(
        governanceOverview.bottleneckPendingRiskBindingRate ?? ((pendingRiskBindingCount * 100) / bottleneckBase)
      );
      const bottleneckPendingThresholdPolicyRate = Number(
        governanceOverview.bottleneckPendingThresholdPolicyRate ?? ((pendingThresholdPolicyCount * 100) / bottleneckBase)
      );
      const bottleneckPendingLinkagePlanRate = Number(
        governanceOverview.bottleneckPendingLinkagePlanRate ?? ((pendingLinkagePlanCount * 100) / bottleneckBase)
      );
      const bottleneckPendingReplayRate = Number(
        governanceOverview.bottleneckPendingReplayRate ?? ((pendingReplayCount * 100) / bottleneckBase)
      );
      const bottleneckEntries = [
        { label: '待治理产品', count: pendingProductGovernanceCount, rate: bottleneckPendingProductGovernanceRate },
        { label: '待发布合同', count: pendingContractReleaseCount, rate: bottleneckPendingContractReleaseRate },
        { label: '待绑定风险点', count: pendingRiskBindingCount, rate: bottleneckPendingRiskBindingRate },
        { label: '待补阈值策略', count: pendingThresholdPolicyCount, rate: bottleneckPendingThresholdPolicyRate },
        { label: '待补联动预案', count: pendingLinkagePlanCount, rate: bottleneckPendingLinkagePlanRate },
        { label: '待运营复盘', count: pendingReplayCount, rate: bottleneckPendingReplayRate }
      ].sort((left, right) => right.rate - left.rate || right.count - left.count);
      const dominantBottleneck = bottleneckEntries[0] || null;
      const secondBottleneck = bottleneckEntries[1] || null;

      manager.kpis[0].value = formatPercent(governanceCompletionRate, 1);
      manager.kpis[0].badge = {
        label: `${formatInteger(governedProductCount)} / ${formatInteger(totalProductCount)} 产品`,
        tone: governanceCompletionRate >= 80 ? 'success' : 'warning'
      };
      manager.kpis[1].value = formatPercent(metricBindingCoverageRate, 1);
      manager.kpis[1].badge = {
        label: `${formatInteger(boundRiskMetricCount)} / ${formatInteger(publishedRiskMetricCount)} 指标`,
        tone: metricBindingCoverageRate >= 70 ? 'success' : 'warning'
      };
      manager.kpis[2].value = formatPercent(linkagePlanCoverageRate, 1);
      manager.kpis[2].badge = {
        label: `待补联动预案 ${formatInteger(pendingLinkagePlanCount)} 项`,
        tone: linkagePlanCoverageRate >= 70 ? 'success' : 'warning'
      };
      manager.kpis[3].value = formatDurationHours(averageOnboardingDurationHours);
      manager.kpis[3].badge = {
        label: `已发布 ${formatInteger(releasedProductCount)} 个产品`,
        tone: averageOnboardingDurationHours > 72 ? 'warning' : 'success'
      };

      manager.focusDimensions[0].value = `${formatInteger(governedProductCount)} / ${formatInteger(totalProductCount)}`;
      manager.focusDimensions[0].description = rawStageProductCount > 0
        ? `已进入合同发布或风险指标目录的产品规模。原始字段阶段 ${formatInteger(rawStageVendorCount)} 个厂商 / ${formatInteger(rawStageProductCount)} 个产品`
        : '已进入合同发布或风险指标目录的产品规模。';
      manager.focusDimensions[0].trend = rawStageVendorPreview || formatPercent(governanceCompletionRate, 1);
      manager.focusDimensions[0].trendTone = rawStageProductCount > 0 ? 'down' : (governanceCompletionRate >= 80 ? 'up' : 'down');

      manager.focusDimensions[1].value = formatDurationHours(averageOnboardingDurationHours);
      manager.focusDimensions[1].description = '新产品从建档到首个合同发布的平均耗时。';
      manager.focusDimensions[1].trend = rawStageProductPreview || `待发布合同 ${formatInteger(pendingContractReleaseCount)} 个`;
      manager.focusDimensions[1].trendTone = rawStageProductPreview ? 'down' : (averageOnboardingDurationHours > 72 ? 'down' : 'up');

      manager.focusDimensions[2].value = dominantBottleneck
        ? `${dominantBottleneck.label} ${formatPercent(dominantBottleneck.rate, 1)}`
        : '--';
      manager.focusDimensions[2].description = '按六类任务聚合的主卡点分布。';
      manager.focusDimensions[2].trend = secondBottleneck
        ? `${secondBottleneck.label} ${formatPercent(secondBottleneck.rate, 1)}`
        : '无显著次级卡点';
      manager.focusDimensions[2].trendTone = (dominantBottleneck?.count ?? 0) > 0 ? 'down' : 'stable';

      manager.focusDimensions[3].value = formatInteger(pendingReplayCount);
      manager.focusDimensions[3].description = '按风险指标维度聚合出的待复盘事项。';
      manager.focusDimensions[3].trend = `待补阈值 ${formatInteger(pendingThresholdPolicyCount)} / 联动预案 ${formatInteger(pendingLinkagePlanCount)}`;
      manager.focusDimensions[3].trendTone = pendingReplayCount > 0 ? 'down' : 'stable';

      manager.queues[0].value = formatInteger(pendingProductGovernanceCount);
      manager.queues[0].hint = rawStageProductCount > 0
        ? `原始字段阶段 ${formatInteger(rawStageVendorCount)} 个厂商 / ${formatInteger(rawStageProductCount)} 个产品`
        : '尚未进入治理主链路';
      manager.queues[0].percent = Math.round(Math.max(0, Math.min(100, bottleneckPendingProductGovernanceRate)));
      manager.queues[0].tag = pendingProductGovernanceCount > 0 ? '待治理' : '已收口';
      manager.queues[0].tone = pendingProductGovernanceCount > 0 ? 'warning' : 'muted';

      manager.queues[1].value = formatInteger(pendingContractReleaseCount);
      manager.queues[1].hint = '样本已治理但未形成正式合同';
      manager.queues[1].percent = Math.round(Math.max(0, Math.min(100, bottleneckPendingContractReleaseRate)));
      manager.queues[1].tag = pendingContractReleaseCount > 0 ? '待发布' : '已收口';
      manager.queues[1].tone = pendingContractReleaseCount > 0 ? 'warning' : 'muted';

      manager.queues[2].value = formatInteger(pendingRiskBindingCount);
      manager.queues[2].hint = '设备已上报但未完成风险点绑定';
      manager.queues[2].percent = Math.round(Math.max(0, Math.min(100, bottleneckPendingRiskBindingRate)));
      manager.queues[2].tag = pendingRiskBindingCount > 0 ? '待纳管' : '已收口';
      manager.queues[2].tone = pendingRiskBindingCount > 0 ? 'danger' : 'muted';

      manager.queues[3].value = formatInteger(pendingThresholdPolicyCount);
      manager.queues[3].hint = `已覆盖 ${formatInteger(ruleCoveredRiskMetricCount)} / ${formatInteger(boundRiskMetricCount)} 指标`;
      manager.queues[3].percent = Math.round(Math.max(0, Math.min(100, bottleneckPendingThresholdPolicyRate)));
      manager.queues[3].tag = pendingThresholdPolicyCount > 0 ? '待策略' : '已收口';
      manager.queues[3].tone = pendingThresholdPolicyCount > 0 ? 'brand' : 'muted';

      manager.queues[4].value = formatInteger(pendingLinkagePlanCount);
      manager.queues[4].hint = `联动 ${formatInteger(pendingLinkageCount)} / 预案 ${formatInteger(pendingEmergencyPlanCount)}`;
      manager.queues[4].percent = Math.round(Math.max(0, Math.min(100, bottleneckPendingLinkagePlanRate)));
      manager.queues[4].tag = pendingLinkagePlanCount > 0 ? '待编排' : '已收口';
      manager.queues[4].tone = pendingLinkagePlanCount > 0 ? 'brand' : 'muted';

      manager.queues[5].value = formatInteger(pendingReplayCount);
      manager.queues[5].hint = '按 releaseBatchId/traceId 聚合复盘';
      manager.queues[5].percent = Math.round(Math.max(0, Math.min(100, bottleneckPendingReplayRate)));
      manager.queues[5].tag = pendingReplayCount > 0 ? '待复盘' : '已收口';
      manager.queues[5].tone = pendingReplayCount > 0 ? 'warning' : 'muted';

      manager.todos[0].title = pendingProductGovernanceCount > 0
        ? `处理 ${formatInteger(pendingProductGovernanceCount)} 个待治理产品`
        : '复核产品治理进度';
      manager.todos[0].detail = rawStageProductCount > 0
        ? `厂商：${rawStageVendorPreview || '--'}；产品：${rawStageProductPreview || '--'}。${pendingContractReleaseCount > 0
          ? `当前还有 ${formatInteger(pendingContractReleaseCount)} 个产品待发布合同。`
          : '建议优先推动这些产品进入正式治理。'}`
        : (pendingContractReleaseCount > 0
          ? `当前还有 ${formatInteger(pendingContractReleaseCount)} 个产品待发布合同。`
          : '当前无待发布合同，建议抽查最新发布批次。');
      manager.todos[0].tone = pendingProductGovernanceCount > 0 ? 'warning' : 'muted';

      manager.todos[1].title = pendingContractReleaseCount > 0
        ? `推进 ${formatInteger(pendingContractReleaseCount)} 个待发布合同`
        : '维持合同发布节奏';
      manager.todos[1].detail = averageOnboardingDurationHours > 72
        ? `平均接入耗时 ${formatDurationHours(averageOnboardingDurationHours)}，建议优先打通发布卡点。`
        : '平均接入耗时处于可控区间，建议继续固化发布模板。';
      manager.todos[1].tone = pendingContractReleaseCount > 0 ? 'warning' : 'muted';

      manager.todos[2].title = pendingRiskBindingCount > 0
        ? `收口 ${formatInteger(pendingRiskBindingCount)} 个待绑定风险点`
        : '复核风险绑定链路';
      manager.todos[2].detail = pendingRiskBindingCount > 0
        ? '优先处理已有上报但未纳管设备，缩短闭环断点。'
        : '当前无待绑定设备，建议复核新接入设备抽样。';
      manager.todos[2].tone = pendingRiskBindingCount > 0 ? 'danger' : 'muted';

      manager.todos[3].title = pendingThresholdPolicyCount > 0
        ? `补齐 ${formatInteger(pendingThresholdPolicyCount)} 个待补阈值策略`
        : '维持策略覆盖基线';
      manager.todos[3].detail = pendingThresholdPolicyCount > 0
        ? `当前策略覆盖 ${formatInteger(ruleCoveredRiskMetricCount)} / ${formatInteger(boundRiskMetricCount)}，需优先补齐高风险指标。`
        : '阈值策略已收口，建议持续做命中质量复验。';
      manager.todos[3].tone = pendingThresholdPolicyCount > 0 ? 'brand' : 'muted';

      manager.todos[4].title = pendingLinkagePlanCount > 0
        ? `补齐 ${formatInteger(pendingLinkagePlanCount)} 项联动预案`
        : '维持联动预案覆盖基线';
      manager.todos[4].detail = pendingLinkagePlanCount > 0
        ? `联动缺口 ${formatInteger(pendingLinkageCount)}，预案缺口 ${formatInteger(pendingEmergencyPlanCount)}。`
        : '联动与预案覆盖已收口，建议按场景轮训演练。';
      manager.todos[4].tone = pendingLinkagePlanCount > 0 ? 'brand' : 'muted';

      manager.todos[5].title = pendingReplayCount > 0
        ? `收口 ${formatInteger(pendingReplayCount)} 个待运营复盘`
        : '维持运营复盘节奏';
      manager.todos[5].detail = pendingReplayCount > 0
        ? '按 traceId/deviceCode/productKey/releaseBatchId 复核闭环链路。'
        : '当前无待复盘事项，建议保留周度抽样复验。';
      manager.todos[5].tone = pendingReplayCount > 0 ? 'warning' : 'muted';
    } else {
      manager.kpis[0].value = '--';
      manager.kpis[1].value = '--';
      manager.kpis[2].value = '--';
      manager.kpis[3].value = '--';
      manager.kpis[0].badge = { label: '治理接口暂不可用', tone: 'warning' };
      manager.kpis[1].badge = { label: '治理接口暂不可用', tone: 'warning' };
      manager.kpis[2].badge = { label: '治理接口暂不可用', tone: 'warning' };
      manager.kpis[3].badge = { label: '治理接口暂不可用', tone: 'warning' };
    }
  }

  const rd = rolePresets.find((item) => item.key === 'rd');
  if (rd) {
    const stabilityScore = Math.max(0, Math.min(100, Math.round((onlineRate * 0.5) + (closureRate * 0.5))));
    rd.kpis[0].value = formatPercent(onlineRate, 1);
    rd.kpis[0].badge = { label: '链路在线率', tone: 'success' };
    rd.kpis[1].value = formatInteger(alarmCritical + alarmHigh);
    rd.kpis[1].badge = { label: '高优告警', tone: 'warning' };
    rd.kpis[2].value = formatInteger(Math.max(0, alarmTrendDelta + eventTrendDelta));
    rd.kpis[2].badge = { label: '波动值', tone: 'danger' };
    rd.kpis[3].value = formatInteger(stabilityScore);
    rd.kpis[3].badge = { label: '稳定性', tone: stabilityScore >= 85 ? 'success' : 'warning' };

    rd.queues[0].value = formatInteger(alarmCritical + alarmHigh);
    rd.queues[1].value = formatInteger(Math.max(0, alarmTrendDelta + eventTrendDelta));
    rd.queues[2].value = formatInteger(eventUnclosed);
  }
}

async function loadDashboardMetrics() {
  dataSourceState.value = 'loading';
  try {
    const { startDate, endDate } = getDateRangeOfLast7Days();
    const [riskTrendRes, alarmRes, eventRes, deviceRes, governanceRes] = await Promise.all([
      getRiskTrendAnalysis(startDate, endDate),
      getAlarmStatistics(startDate, endDate),
      getEventClosureAnalysis(startDate, endDate),
      getDeviceHealthAnalysis(),
      getRiskGovernanceDashboardOverview().catch(() => null)
    ]);

    const allSuccess = [riskTrendRes, alarmRes, eventRes, deviceRes].every((item: any) => item?.code === 200);
    if (!allSuccess) {
      dataSourceState.value = 'fallback';
      return;
    }

    applyLiveMetrics({
      riskTrend: Array.isArray(riskTrendRes.data) ? riskTrendRes.data : [],
      alarmStats: alarmRes.data || {},
      eventStats: eventRes.data || {},
      deviceHealth: deviceRes.data || {},
      governanceOverview: governanceRes && governanceRes.code === 200 ? governanceRes.data || null : null
    });

    dataSourceState.value = 'live';
    dashboardUpdatedAt.value = formatNow();
  } catch {
    dataSourceState.value = 'fallback';
  }
}

onMounted(() => {
  activeRole.value = inferRoleFromAuth();
  loadDashboardMetrics();
  timer = window.setInterval(() => {
    currentTime.value = formatNow();
  }, 1000);
});

onUnmounted(() => {
  if (timer !== null) {
    window.clearInterval(timer);
  }
});
</script>
<style scoped>
.cockpit-page {
  --dash-text: var(--text-heading);
  --dash-subtle: var(--text-secondary);
  --dash-line: color-mix(in srgb, var(--line-panel) 82%, transparent);
  --dash-surface: color-mix(in srgb, var(--bg-card) 90%, white);

  display: grid;
  gap: 1rem;
}

.cockpit-hero {
  display: grid;
  grid-template-columns: 1fr minmax(260px, 320px);
  gap: 1rem;
  padding: 1.2rem;
  border: 1px solid var(--dash-line);
  background:
    linear-gradient(
      135deg,
      color-mix(in srgb, var(--brand) 10%, white),
      color-mix(in srgb, var(--accent) 8%, white)
    ),
    radial-gradient(circle at top left, color-mix(in srgb, var(--brand) 12%, transparent), transparent 38%);
  border-radius: calc(var(--radius-lg) + 4px);
  box-shadow: var(--shadow-sm);
}

.cockpit-hero__eyebrow {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--text-caption-2);
  font-size: 0.72rem;
}

.cockpit-hero h1 {
  margin: 0.45rem 0 0;
  font-size: clamp(1.8rem, 2.5vw, 2.5rem);
  line-height: 1.2;
  color: var(--dash-text);
}

.cockpit-hero__desc {
  margin: 0.8rem 0 0;
  color: var(--dash-subtle);
  line-height: 1.8;
}

.cockpit-hero__clock {
  border: 1px solid var(--dash-line);
  background: var(--dash-surface);
  border-radius: calc(var(--radius-lg) + 2px);
  padding: 0.9rem;
  display: grid;
  align-content: start;
  gap: 0.55rem;
  box-shadow: var(--shadow-card-soft);
}

.cockpit-hero__clock span {
  color: var(--text-caption-2);
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.cockpit-hero__clock strong {
  font-size: 1.3rem;
  color: var(--text-heading);
}

.cockpit-hero__clock small {
  color: var(--text-caption);
  line-height: 1.6;
}

.role-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.8rem;
}

.role-tabs__item {
  border: 1px solid var(--dash-line);
  border-radius: calc(var(--radius-lg) + 2px);
  background: var(--dash-surface);
  padding: 0.78rem 0.9rem;
  text-align: left;
  display: grid;
  gap: 0.2rem;
  transition: all 160ms ease;
}

.role-tabs__item:hover {
  border-color: color-mix(in srgb, var(--accent) 20%, var(--panel-border));
  background: var(--bg-card);
}

.role-tabs__item strong {
  color: var(--text-heading);
  font-size: 1rem;
}

.role-tabs__item span {
  color: var(--text-caption);
  font-size: 0.83rem;
}

.role-tabs__item--active {
  border-color: color-mix(in srgb, var(--accent) 34%, var(--panel-border));
  background: linear-gradient(130deg, color-mix(in srgb, var(--accent) 12%, white), color-mix(in srgb, var(--accent) 4%, white));
  box-shadow: 0 6px 16px color-mix(in srgb, var(--accent) 10%, transparent);
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.8rem;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 0.8rem;
}

.focus-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.focus-card {
  border: 1px solid var(--dash-line);
  border-radius: calc(var(--radius-md) + 2px);
  padding: 0.9rem;
  background: var(--dash-surface);
  box-shadow: var(--shadow-card-soft);
}

.focus-card span {
  color: var(--text-caption-2);
  font-size: 0.78rem;
}

.focus-card strong {
  display: block;
  margin-top: 0.36rem;
  color: var(--text-heading);
  font-size: 1.2rem;
}

.focus-card p {
  margin: 0.5rem 0 0;
  color: var(--text-caption);
  line-height: 1.65;
  font-size: 0.88rem;
}

.trend {
  display: inline-block;
  margin-top: 0.5rem;
  font-size: 0.8rem;
  padding: 0.2rem 0.5rem;
  border-radius: var(--radius-pill);
}

.trend--up {
  color: var(--success);
  background: var(--success-bg);
}

.trend--down {
  color: var(--danger);
  background: var(--danger-bg);
}

.trend--stable {
  color: var(--accent-deep);
  background: var(--info-bg);
}

.queue-list {
  display: grid;
  gap: 0.75rem;
}

.queue-item {
  border: 1px solid var(--dash-line);
  border-radius: calc(var(--radius-md) + 2px);
  padding: 0.85rem;
  background: var(--dash-surface);
  box-shadow: var(--shadow-card-soft);
}

.queue-item__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.badge {
  border-radius: var(--radius-pill);
  font-size: 0.75rem;
  padding: 0.17rem 0.48rem;
}

.badge--danger {
  color: var(--danger);
  background: var(--danger-bg);
}

.badge--warning {
  color: var(--warning);
  background: var(--warning-bg);
}

.badge--brand {
  color: var(--accent-deep);
  background: var(--info-bg);
}

.badge--muted {
  color: var(--text-secondary);
  background: color-mix(in srgb, var(--text-tertiary) 14%, transparent);
}

.queue-item__body {
  margin-top: 0.35rem;
  display: flex;
  align-items: baseline;
  gap: 0.55rem;
}

.queue-item__value {
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--text-heading);
}

.queue-item__body small {
  color: var(--text-caption);
}

.queue-item__bar {
  margin-top: 0.6rem;
  height: 6px;
  background: color-mix(in srgb, var(--text-tertiary) 18%, transparent);
  border-radius: var(--radius-pill);
  overflow: hidden;
}

.queue-item__bar i {
  display: block;
  height: 100%;
  border-radius: var(--radius-pill);
  background: linear-gradient(90deg, var(--accent), color-mix(in srgb, var(--accent-bright) 86%, white));
}

.todo-list {
  display: grid;
  gap: 0.7rem;
}

.todo-item {
  border: 1px solid var(--dash-line);
  border-radius: calc(var(--radius-md) + 2px);
  background: var(--dash-surface);
  padding: 0.85rem;
  display: grid;
  gap: 0.5rem;
  box-shadow: var(--shadow-card-soft);
}

.todo-item__meta {
  display: flex;
  justify-content: space-between;
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.todo-item p {
  margin: 0;
  color: var(--text-caption);
  line-height: 1.65;
  font-size: 0.88rem;
}

.todo-item button {
  justify-self: start;
  min-height: 1.9rem;
  border: 1px solid color-mix(in srgb, var(--brand) 18%, transparent);
  background: color-mix(in srgb, var(--brand) 8%, white);
  color: var(--brand);
  border-radius: var(--radius-pill);
  padding: 0 0.82rem;
  font-size: 0.78rem;
  font-weight: 600;
  transition: all 160ms ease;
}

.todo-item button:hover {
  border-color: color-mix(in srgb, var(--brand) 24%, transparent);
  background: color-mix(in srgb, var(--brand) 12%, white);
  box-shadow: 0 4px 10px color-mix(in srgb, var(--brand) 12%, transparent);
}

.todo-item--danger {
  border-left: 3px solid var(--danger);
}

.todo-item--warning {
  border-left: 3px solid var(--warning);
}

.todo-item--brand {
  border-left: 3px solid var(--accent);
}

.todo-item--muted {
  border-left: 3px solid var(--text-tertiary);
}

.workbench-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.65rem;
}

.workbench-entry {
  border: 1px solid var(--dash-line);
  border-radius: calc(var(--radius-md) + 2px);
  background: var(--dash-surface);
  padding: 0.82rem;
  text-align: left;
  box-shadow: var(--shadow-card-soft);
  transition: all 160ms ease;
}

.workbench-entry:hover {
  border-color: color-mix(in srgb, var(--accent) 20%, var(--panel-border));
  box-shadow: 0 8px 18px color-mix(in srgb, var(--accent) 8%, transparent);
  transform: translateY(-1px);
}

.workbench-entry strong {
  color: var(--text-heading);
}

.workbench-entry p {
  margin: 0.38rem 0 0;
  color: var(--text-secondary);
  font-size: 0.86rem;
  line-height: 1.6;
}

.workbench-entry span {
  display: inline-block;
  margin-top: 0.5rem;
  color: var(--accent-deep);
  font-size: 0.75rem;
}

.activity-trace {
  margin-top: 0.8rem;
  border: 1px dashed color-mix(in srgb, var(--accent) 28%, transparent);
  border-radius: calc(var(--radius-md) + 2px);
  padding: 0.7rem 0.8rem;
  background: color-mix(in srgb, var(--accent) 6%, white);
  display: grid;
  gap: 0.34rem;
}

.activity-trace span {
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--accent-deep);
  font-size: 0.72rem;
}

.activity-trace p {
  margin: 0;
  color: var(--text-secondary);
}

.activity-trace small {
  color: var(--text-caption);
}

@media (max-width: 1200px) {
  .role-tabs,
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid,
  .cockpit-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .role-tabs,
  .kpi-grid,
  .focus-grid,
  .workbench-grid {
    grid-template-columns: 1fr;
  }
}
</style>
