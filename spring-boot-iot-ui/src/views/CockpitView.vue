<template>
  <div class="cockpit-page">
    <section class="cockpit-hero">
      <div class="cockpit-hero__main">
        <p class="cockpit-hero__eyebrow">Risk Data Cockpit</p>
        <h1>监测预警驾驶舱</h1>
        <p class="cockpit-hero__desc">
          首页聚焦关键风险指标、处置效率和系统运行状态；事务工作台入口下沉为执行层，避免“首页即工作台”的信息噪音。
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
        eyebrow="Role Focus"
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
        eyebrow="Queue"
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
        eyebrow="Todo"
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
        eyebrow="Workbench"
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
import { activityEntries, recordActivity } from '../stores/activity';
import { usePermissionStore } from '../stores/permission';

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
      { id: 'fl-1', priority: 'P1', window: '立即处理', title: '确认 3 条一级告警', detail: '涉及 2 个高风险点，需先确认并判定是否派单。', actionLabel: '进入告警中心', tone: 'danger', path: '/alarm-center' },
      { id: 'fl-2', priority: 'P2', window: '30 分钟内', title: '完成事件工单接力', detail: '夜班遗留 2 条事件等待接收与开始。', actionLabel: '进入事件处置', tone: 'warning', path: '/event-disposal' },
      { id: 'fl-3', priority: 'P3', window: '本班次内', title: '核对重点风险点状态', detail: '检查离线设备是否存在持续上报中断。', actionLabel: '进入风险点管理', tone: 'brand', path: '/risk-point' }
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
      { id: 'ops-1', priority: 'P1', window: '今天', title: '排查离线设备聚集区域', detail: '东区出现 16 台设备离线，需确认网络与电源。', actionLabel: '进入设备运维中心', tone: 'warning', path: '/devices' },
      { id: 'ops-2', priority: 'P2', window: '今天', title: '复核高误报阈值规则', detail: '3 条阈值规则命中频率异常偏高。', actionLabel: '进入阈值规则', tone: 'brand', path: '/rule-definition' },
      { id: 'ops-3', priority: 'P2', window: '48 小时', title: '督办超时工单闭环', detail: '2 条事件工单接近 SLA 红线。', actionLabel: '进入事件处置', tone: 'danger', path: '/event-disposal' }
    ]
  },
  {
    key: 'manager',
    label: '管理人员',
    caption: '经营态势、风险趋势与资源调度',
    emphasis: '关注平台总体风险态势、处置效率和组织执行力，支撑管理决策。',
    kpis: [
      { label: '今日新增告警', value: '126', badge: { label: '较昨日 +8%', tone: 'warning' } },
      { label: '今日闭环事件', value: '41', badge: { label: '闭环率 87%', tone: 'success' } },
      { label: '高风险点覆盖率', value: '93%', badge: { label: '持续提升', tone: 'brand' } },
      { label: '组织执行指数', value: '84', badge: { label: '中高水平', tone: 'muted' } }
    ],
    focusDimensions: [
      { title: '风险趋势', value: '近 7 日波动 +5%', description: '风险告警总量短期上升，需关注季节性因素。', trend: '+5%', trendTone: 'down' },
      { title: '事件闭环效率', value: '87%', description: '近 24 小时未闭环比例处于可控区间。', trend: '+3%', trendTone: 'up' },
      { title: '跨组织协同得分', value: '81', description: '组织与区域协同质量评分。', trend: '+2', trendTone: 'up' },
      { title: '报告覆盖率', value: '90%', description: '日报/周报生成与审核覆盖程度。', trend: '稳定', trendTone: 'stable' }
    ],
    queues: [
      { label: '高风险区域告警', value: '29', hint: '需资源倾斜', percent: 72, tag: '决策', tone: 'danger' },
      { label: '跨部门协同事件', value: '14', hint: '需管理协调', percent: 51, tag: '协同', tone: 'warning' },
      { label: '本周复盘事项', value: '6', hint: '纳入经营复盘', percent: 33, tag: '复盘', tone: 'brand' }
    ],
    todos: [
      { id: 'mg-1', priority: 'P1', window: '今日例会前', title: '查看高风险区域态势', detail: '确认重点区域告警攀升原因与资源分配。', actionLabel: '进入分析报表', tone: 'danger', path: '/report-analysis' },
      { id: 'mg-2', priority: 'P2', window: '今日', title: '审阅跨部门协同事件', detail: '对超时事件给出协调决策。', actionLabel: '进入事件处置', tone: 'warning', path: '/event-disposal' },
      { id: 'mg-3', priority: 'P3', window: '本周', title: '复核治理执行情况', detail: '关注业务日志和组织执行情况。', actionLabel: '进入业务日志', tone: 'brand', path: '/audit-log' }
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
      { id: 'rd-1', priority: 'P1', window: '立即', title: '定位 1 条接口 5xx 告警', detail: '优先核查认证、接入与风险监测链路的系统日志。', actionLabel: '进入系统日志', tone: 'danger', path: '/system-log' },
      { id: 'rd-2', priority: 'P2', window: '今天', title: '复盘协议解析异常样本', detail: '检查 legacy 报文兼容解析和安全校验。', actionLabel: '进入接入验证中心', tone: 'warning', path: '/reporting' },
      { id: 'rd-3', priority: 'P3', window: '本周', title: '回归关键业务流程', detail: '覆盖告警、事件、规则、报表与登录链路。', actionLabel: '进入监测对象工作台', tone: 'brand', path: '/insight' }
    ]
  }
];

const workbenchEntries = [
  { title: '告警中心', description: '告警确认、抑制与关闭处理。', caption: '预警处置', path: '/alarm-center' },
  { title: '事件处置', description: '工单派发、接收、闭环与反馈。', caption: '事件闭环', path: '/event-disposal' },
  { title: '风险点管理', description: '风险点台账、设备绑定与等级维护。', caption: '风险配置', path: '/risk-point' },
  { title: '分析报表', description: '风险趋势、告警统计和设备健康分析。', caption: '经营分析', path: '/report-analysis' },
  { title: '设备运维中心', description: '设备建档、在线态与基础运维。', caption: '实施支撑', path: '/devices' },
  { title: '接入验证中心', description: 'HTTP 上报链路与协议联调。', caption: '研发联调', path: '/reporting' }
];

const roleOptions = computed(() => rolePresets.map(({ key, label, caption }) => ({ key, label, caption })));

const activePreset = computed(() => rolePresets.find((item) => item.key === activeRole.value) || rolePresets[0]);

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
    return `数据源状态：真实报表聚合（最近同步：${dashboardUpdatedAt.value || '--'}）`;
  }
  return '数据源状态：真实接口不可用，当前显示稳定兜底口径';
});

function inferRoleFromAuth(): RoleKey {
  const roleNames = permissionStore.roleNames;
  if (roleNames.some((role) => role.includes('开发'))) {
    return 'rd';
  }
  if (roleNames.some((role) => role.includes('管理') || role.includes('超级'))) {
    return 'manager';
  }
  if (roleNames.some((role) => role.includes('运维'))) {
    return 'ops';
  }
  return 'frontline';
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
}) {
  const { riskTrend, alarmStats, eventStats, deviceHealth } = payload;

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
    manager.kpis[0].value = formatInteger(alarmTotal);
    manager.kpis[0].badge = { label: `趋势 ${alarmTrendDelta >= 0 ? '+' : ''}${alarmTrendDelta}`, tone: alarmTrendDelta > 0 ? 'warning' : 'success' };
    manager.kpis[1].value = formatInteger(eventClosed);
    manager.kpis[1].badge = { label: `闭环率 ${formatPercent(closureRate, 0)}`, tone: closureRate >= 85 ? 'success' : 'warning' };
    manager.kpis[2].value = formatPercent(onlineRate, 1);
    manager.kpis[3].value = formatInteger(alarmCritical + alarmHigh + eventUnclosed);
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
    const [riskTrendRes, alarmRes, eventRes, deviceRes] = await Promise.all([
      getRiskTrendAnalysis(startDate, endDate),
      getAlarmStatistics(startDate, endDate),
      getEventClosureAnalysis(startDate, endDate),
      getDeviceHealthAnalysis()
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
      deviceHealth: deviceRes.data || {}
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
  --dash-text: #1f2d43;
  --dash-subtle: #5d7293;
  --dash-line: rgba(92, 120, 164, 0.2);

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
    linear-gradient(135deg, rgba(255, 247, 236, 0.74), rgba(241, 248, 255, 0.9)),
    radial-gradient(circle at top left, rgba(255, 140, 48, 0.12), transparent 38%);
  border-radius: calc(var(--radius-lg) + 4px);
  box-shadow: 0 6px 18px rgba(31, 35, 41, 0.05);
}

.cockpit-hero__eyebrow {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: #6f84a4;
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
  background: rgba(255, 255, 255, 0.84);
  border-radius: calc(var(--radius-lg) + 2px);
  padding: 0.9rem;
  display: grid;
  align-content: start;
  gap: 0.55rem;
  box-shadow: 0 4px 12px rgba(31, 35, 41, 0.04);
}

.cockpit-hero__clock span {
  color: #6e84a4;
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.cockpit-hero__clock strong {
  font-size: 1.3rem;
  color: #223652;
}

.cockpit-hero__clock small {
  color: #7188aa;
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
  background: rgba(255, 255, 255, 0.9);
  padding: 0.78rem 0.9rem;
  text-align: left;
  display: grid;
  gap: 0.2rem;
  transition: all 160ms ease;
}

.role-tabs__item:hover {
  border-color: rgba(39, 114, 240, 0.2);
  background: rgba(255, 255, 255, 0.96);
}

.role-tabs__item strong {
  color: #2a3d5a;
  font-size: 1rem;
}

.role-tabs__item span {
  color: #6d83a4;
  font-size: 0.83rem;
}

.role-tabs__item--active {
  border-color: rgba(39, 114, 240, 0.36);
  background: linear-gradient(130deg, rgba(31, 109, 240, 0.13), rgba(31, 109, 240, 0.04));
  box-shadow: 0 6px 16px rgba(31, 109, 240, 0.1);
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
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--shadow-card-soft);
}

.focus-card span {
  color: #6d83a4;
  font-size: 0.78rem;
}

.focus-card strong {
  display: block;
  margin-top: 0.36rem;
  color: #253753;
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
  color: #166534;
  background: rgba(34, 197, 94, 0.16);
}

.trend--down {
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.14);
}

.trend--stable {
  color: #365475;
  background: rgba(59, 130, 246, 0.14);
}

.queue-list {
  display: grid;
  gap: 0.75rem;
}

.queue-item {
  border: 1px solid var(--dash-line);
  border-radius: calc(var(--radius-md) + 2px);
  padding: 0.85rem;
  background: rgba(255, 255, 255, 0.88);
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
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.16);
}

.badge--warning {
  color: #92400e;
  background: rgba(245, 158, 11, 0.16);
}

.badge--brand {
  color: #1f5ab8;
  background: rgba(59, 130, 246, 0.16);
}

.badge--muted {
  color: #4b5563;
  background: rgba(148, 163, 184, 0.18);
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
  color: #20324d;
}

.queue-item__body small {
  color: #617694;
}

.queue-item__bar {
  margin-top: 0.6rem;
  height: 6px;
  background: rgba(142, 166, 200, 0.18);
  border-radius: var(--radius-pill);
  overflow: hidden;
}

.queue-item__bar i {
  display: block;
  height: 100%;
  border-radius: var(--radius-pill);
  background: linear-gradient(90deg, #2f77ff, #6ca7ff);
}

.todo-list {
  display: grid;
  gap: 0.7rem;
}

.todo-item {
  border: 1px solid var(--dash-line);
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(255, 255, 255, 0.9);
  padding: 0.85rem;
  display: grid;
  gap: 0.5rem;
  box-shadow: var(--shadow-card-soft);
}

.todo-item__meta {
  display: flex;
  justify-content: space-between;
  color: #5e7597;
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
  border: 1px solid color-mix(in srgb, var(--accent) 22%, transparent);
  background: linear-gradient(180deg, var(--bg-card), #f7fbff);
  color: var(--accent-deep);
  border-radius: var(--radius-pill);
  padding: 0 0.82rem;
  font-size: 0.78rem;
  font-weight: 600;
  transition: all 160ms ease;
}

.todo-item button:hover {
  border-color: color-mix(in srgb, var(--accent) 28%, transparent);
  background: color-mix(in srgb, var(--accent) 8%, var(--bg-card));
  box-shadow: 0 4px 10px color-mix(in srgb, var(--accent) 12%, transparent);
}

.todo-item--danger {
  border-left: 3px solid #ef4444;
}

.todo-item--warning {
  border-left: 3px solid #f59e0b;
}

.todo-item--brand {
  border-left: 3px solid #3b82f6;
}

.todo-item--muted {
  border-left: 3px solid #94a3b8;
}

.workbench-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.65rem;
}

.workbench-entry {
  border: 1px solid var(--dash-line);
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(255, 255, 255, 0.9);
  padding: 0.82rem;
  text-align: left;
  box-shadow: var(--shadow-card-soft);
  transition: all 160ms ease;
}

.workbench-entry:hover {
  border-color: rgba(39, 114, 240, 0.2);
  box-shadow: 0 8px 18px rgba(31, 109, 240, 0.08);
  transform: translateY(-1px);
}

.workbench-entry strong {
  color: #243854;
}

.workbench-entry p {
  margin: 0.38rem 0 0;
  color: #5f7698;
  font-size: 0.86rem;
  line-height: 1.6;
}

.workbench-entry span {
  display: inline-block;
  margin-top: 0.5rem;
  color: #3f5f88;
  font-size: 0.75rem;
}

.activity-trace {
  margin-top: 0.8rem;
  border: 1px dashed rgba(78, 117, 179, 0.38);
  border-radius: calc(var(--radius-md) + 2px);
  padding: 0.7rem 0.8rem;
  background: rgba(240, 247, 255, 0.8);
  display: grid;
  gap: 0.34rem;
}

.activity-trace span {
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: #6383b1;
  font-size: 0.72rem;
}

.activity-trace p {
  margin: 0;
  color: #5a7192;
}

.activity-trace small {
  color: #6783ab;
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

