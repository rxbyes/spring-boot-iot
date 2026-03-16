<template>
  <div class="cockpit-page">
    <section class="hero-panel">
      <div class="hero-panel__copy">
        <p class="hero-panel__eyebrow">监测预警平台</p>
        <h1>{{ activeScenario.heroTitle }}</h1>
        <p class="hero-panel__description">
          {{ activeScenario.heroDescription }}
        </p>

        <div class="hero-panel__tags">
          <span v-for="tag in heroTags" :key="tag">{{ tag }}</span>
        </div>

        <div class="hero-panel__actions">
          <button type="button" class="hero-action hero-action--primary" @click="navigateTo('/alarm-center')">进入告警中心</button>
          <button type="button" class="hero-action" @click="navigateTo('/event-disposal')">查看事件处置</button>
          <button type="button" class="hero-action" @click="navigateTo('/report-analysis')">查看分析报表</button>
        </div>
      </div>

      <div class="hero-panel__brief">
        <div class="brief-head">
          <div>
            <span>当前视角</span>
            <strong>{{ roleSummary }}</strong>
          </div>
          <small>{{ activeScenario.viewCaption }}</small>
        </div>

        <div class="view-switch">
          <button
            v-for="option in viewOptions"
            :key="option.value"
            type="button"
            class="view-switch__option"
            :class="{ 'view-switch__option--active': option.value === viewMode }"
            @click="setViewMode(option.value)"
          >
            <strong>{{ option.label }}</strong>
            <span>{{ option.caption }}</span>
          </button>
        </div>

        <div class="focus-card">
          <span>今日关注</span>
          <strong>{{ activeScenario.focusTitle }}</strong>
          <p>{{ activeScenario.focusDescription }}</p>
        </div>

        <div class="brief-list">
          <div v-for="item in summaryItems" :key="item.label" class="brief-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.caption }}</small>
          </div>
        </div>

        <div class="brief-foot">
          <span>平台时间</span>
          <strong>{{ currentTime }}</strong>
        </div>
      </div>
    </section>

    <div class="metric-grid">
      <MetricCard
        v-for="metric in overviewMetrics"
        :key="metric.label"
        :label="metric.label"
        :value="metric.value"
        :badge="metric.badge"
      />
    </div>

    <div class="insight-grid">
      <PanelCard
        eyebrow="Focus Mix"
        title="当前视角关注构成"
        description="不再把首页做成静态入口墙，而是让值班、运维主管和管理层看到不同的业务重心。"
      >
        <div class="chart-layout">
          <div ref="capabilityChartRef" class="chart-canvas"></div>
          <div class="chart-legend">
            <div v-for="item in focusDistribution" :key="item.name" class="chart-legend__item">
              <span class="chart-legend__dot" :style="{ backgroundColor: item.color }"></span>
              <div>
                <strong>{{ item.name }}</strong>
                <small>{{ item.value }} 权重</small>
              </div>
            </div>
          </div>
        </div>
      </PanelCard>

      <PanelCard
        eyebrow="Delivery"
        title="产品能力进展"
        description="首页只呈现已经具备业务闭环价值的能力口径，实时监测与 GIS 继续按 Phase 4 进度推进。"
      >
        <div ref="deliveryChartRef" class="chart-canvas chart-canvas--compact"></div>
        <p class="progress-note">{{ activeScenario.deliveryNote }}</p>
      </PanelCard>
    </div>

    <div class="content-grid">
      <PanelCard
        eyebrow="Workflow"
        title="风险处置闭环"
        description="围绕真实业务功能，把风险点、规则、告警、事件、报表和治理串成可执行路径。"
      >
        <ol class="workflow-list">
          <li v-for="step in workflowSteps" :key="step.title" class="workflow-step">
            <span class="workflow-step__index">{{ step.index }}</span>
            <div class="workflow-step__content">
              <div class="workflow-step__title">
                <strong>{{ step.title }}</strong>
                <button type="button" @click="navigateTo(step.path)">进入模块</button>
              </div>
              <p>{{ step.description }}</p>
            </div>
          </li>
        </ol>
      </PanelCard>

      <PanelCard
        eyebrow="Workspace"
        title="经营主线入口"
        description="首页主入口收敛为五条业务主线，设备接入与验证下调为实施支撑能力。"
      >
        <div class="module-grid">
          <button v-for="card in primaryModuleCards" :key="card.title" type="button" class="module-card" @click="navigateTo(card.path)">
            <div class="module-card__head">
              <span>{{ card.badge }}</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
            <strong>{{ card.title }}</strong>
            <p>{{ card.description }}</p>
            <small>{{ card.footer }}</small>
          </button>
        </div>

        <button type="button" class="support-card" @click="navigateTo(supportCapability.path)">
          <div class="support-card__head">
            <span>实施支撑能力</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
          <strong>{{ supportCapability.title }}</strong>
          <p>{{ supportCapability.description }}</p>
          <div class="support-card__tags">
            <span v-for="tag in supportCapability.tags" :key="tag">{{ tag }}</span>
          </div>
        </button>
      </PanelCard>
    </div>

    <div class="content-grid">
      <PanelCard
        eyebrow="Governance"
        title="治理底座"
        description="组织、权限、区域、渠道和审计能力支撑预警平台长期运营，而不是只服务页面联调。"
      >
        <div class="governance-grid">
          <button v-for="item in governanceCards" :key="item.title" type="button" class="governance-card" @click="navigateTo(item.path)">
            <strong>{{ item.title }}</strong>
            <p>{{ item.description }}</p>
            <span>{{ item.footer }}</span>
          </button>
        </div>
      </PanelCard>

      <PanelCard
        eyebrow="Execution"
        title="待办中心"
        description="首页右侧改为经营待办，而不是页面访问记录。待办来自角色视角模板，最近处理痕迹只作为辅助参考。"
      >
        <div class="todo-list">
          <article
            v-for="item in todoItems"
            :key="item.id"
            class="todo-card"
            :class="`todo-card--${item.tone}`"
          >
            <div class="todo-card__meta">
              <span class="todo-card__priority">{{ item.priority }}</span>
              <span class="todo-card__window">{{ item.window }}</span>
            </div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.detail }}</p>
            <div class="todo-card__footer">
              <span>{{ item.hint }}</span>
              <button type="button" @click="navigateTo(item.path)">{{ item.actionLabel }}</button>
            </div>
          </article>
        </div>

        <div v-if="latestActivityTrace" class="trace-card">
          <span>最近处理痕迹</span>
          <strong>{{ latestActivityTrace.title }}</strong>
          <p>{{ latestActivityTrace.detail }}</p>
          <small>{{ latestActivityTrace.time }}</small>
        </div>
      </PanelCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import * as echarts from 'echarts/core';
import type { ECharts } from 'echarts/core';
import { install as PieChart } from 'echarts/lib/chart/pie/install.js';
import { install as BarChart } from 'echarts/lib/chart/bar/install.js';
import { install as GridComponent } from 'echarts/lib/component/grid/install.js';
import { install as TooltipComponent } from 'echarts/lib/component/tooltip/install.js';
import { install as LegendComponent } from 'echarts/lib/component/legend/install.js';
import { install as CanvasRenderer } from 'echarts/lib/renderer/installCanvasRenderer.js';
import { ArrowRight } from '@element-plus/icons-vue';

import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import { activityEntries, recordActivity } from '../stores/activity';
import { usePermissionStore } from '../stores/permission';

echarts.use([PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer]);

type ViewMode = 'duty' | 'ops' | 'manager';
type BadgeTone = 'success' | 'warning' | 'danger' | 'muted' | 'brand';
type TodoTone = 'brand' | 'success' | 'warning' | 'danger';
type ModuleKey = 'alarm' | 'event' | 'risk' | 'rules' | 'report';

interface SummaryItem {
  label: string;
  value: string;
  caption: string;
}

interface MetricItem {
  label: string;
  value: string;
  badge?: {
    label: string;
    tone: BadgeTone;
  };
}

interface FocusDistributionItem {
  name: string;
  value: number;
  color: string;
}

interface DeliveryProgressItem {
  name: string;
  value: number;
  color: string;
}

interface WorkflowStep {
  index: string;
  title: string;
  description: string;
  path: string;
}

interface ModuleCard {
  badge: string;
  title: string;
  description: string;
  footer: string;
  path: string;
}

interface GovernanceCard {
  title: string;
  description: string;
  footer: string;
  path: string;
}

interface TodoItem {
  id: string;
  priority: string;
  window: string;
  title: string;
  detail: string;
  hint: string;
  actionLabel: string;
  path: string;
  tone: TodoTone;
}

interface ActivityTraceItem {
  time: string;
  title: string;
  detail: string;
}

interface ScenarioPreset {
  label: string;
  viewCaption: string;
  heroTitle: string;
  heroDescription: string;
  heroTags: string[];
  focusTitle: string;
  focusDescription: string;
  summaryItems: SummaryItem[];
  metrics: MetricItem[];
  focusDistribution: FocusDistributionItem[];
  moduleOrder: ModuleKey[];
  governanceDescriptions: {
    organization: string;
    people: string;
    trace: string;
  };
  todos: TodoItem[];
  deliveryNote: string;
}

const scenarioPresets: Record<ViewMode, ScenarioPreset> = {
  duty: {
    label: '值班人员',
    viewCaption: '面向一线值守与交接班的执行首页',
    heroTitle: '把班次监测、告警确认与事件接力收在一个可执行首页',
    heroDescription:
      '首页先呈现班次必须处理的事项，再落到告警中心、事件处置和风险点巡检，不再堆叠研发演示入口和控制台噪音。',
    heroTags: ['告警确认', '事件接力', '风险点巡检', '班次复盘'],
    focusTitle: '先确认高等级告警，再决定是否升级为事件',
    focusDescription: '值班视角优先保证响应速度，所有内容围绕当前班次的判断、协同和交接动作展开。',
    summaryItems: [
      { label: '执行重点', value: '先响应', caption: '先做告警确认、派单判断和巡检动作，不把首页变成浏览页。' },
      { label: '协同对象', value: '现场 / 运维', caption: '围绕告警、事件和风险点，快速找到接力对象与属地。' },
      { label: '业务节奏', value: '班次闭环', caption: '从告警到事件到交接复盘，强调当班闭环，不遗留悬而未决项。' },
      { label: '当前口径', value: 'Phase 4 基线', caption: '首页仍采用静态能力编排，不伪装为真实聚合统计大屏。' }
    ],
    metrics: [
      { label: '班次关注主线', value: '4', badge: { label: '执行优先', tone: 'brand' } },
      { label: '闭环动作节点', value: '6', badge: { label: '可落地', tone: 'success' } },
      { label: '交接重点清单', value: '3', badge: { label: '需复核', tone: 'warning' } },
      { label: '响应节奏', value: '7x24', badge: { label: '持续值守', tone: 'danger' } }
    ],
    focusDistribution: [
      { name: '预警处置', value: 8, color: '#ff6d6d' },
      { name: '风险点治理', value: 5, color: '#ffb347' },
      { name: '分析复盘', value: 3, color: '#1e80ff' },
      { name: '治理底座', value: 2, color: '#52c41a' },
      { name: '接入支撑', value: 2, color: '#7a8ca6' }
    ],
    moduleOrder: ['alarm', 'event', 'risk', 'report', 'rules'],
    governanceDescriptions: {
      organization: '按组织与区域快速确认属地责任，避免值班判断脱离现场边界。',
      people: '保证班次账号、岗位职责和可见范围清晰，减少交接漏项。',
      trace: '通知触达与审计追溯用于回看本班次关键动作和处置链路。'
    },
    todos: [
      {
        id: 'duty-alarm',
        priority: 'P1',
        window: '当前班次',
        title: '待确认告警',
        detail: '优先核对高等级告警，完成确认、抑制或升级为事件的判断。',
        hint: '落点：告警中心',
        actionLabel: '进入告警中心',
        path: '/alarm-center',
        tone: 'danger'
      },
      {
        id: 'duty-event',
        priority: 'P1',
        window: '2 小时内',
        title: '待派单事件',
        detail: '把已经确认的异常快速转为事件，并明确现场接力人与处置时限。',
        hint: '落点：事件处置',
        actionLabel: '进入事件处置',
        path: '/event-disposal',
        tone: 'warning'
      },
      {
        id: 'duty-risk',
        priority: 'P2',
        window: '今日巡检',
        title: '高风险点巡检提醒',
        detail: '复核高风险点与设备绑定、测点状态和责任区域，避免监测对象失焦。',
        hint: '落点：风险点管理',
        actionLabel: '查看风险点',
        path: '/risk-point',
        tone: 'brand'
      },
      {
        id: 'duty-report',
        priority: 'P2',
        window: '交班前',
        title: '班次复盘提要',
        detail: '用报表快速回看本班次告警、事件和设备健康变化，形成交接依据。',
        hint: '落点：分析报表',
        actionLabel: '查看报表',
        path: '/report-analysis',
        tone: 'success'
      }
    ],
    deliveryNote: '值班视角建议先盯告警、事件和风险点三条链路；实时监测与 GIS 仍以 docs/19 为交付口径。'
  },
  ops: {
    label: '运维主管',
    viewCaption: '面向运维督办、规则复核和台账维护',
    heroTitle: '把督办、策略治理与台账维护收口到一个经营型首页',
    heroDescription:
      '运维主管首页更关注超时处置、规则有效性、风险点台账和实施支撑能力，避免把时间消耗在零散入口跳转上。',
    heroTags: ['超时督办', '规则复核', '风险点台账', '实施支撑'],
    focusTitle: '先盯超时处置，再复核规则与台账是否匹配',
    focusDescription: '运维主管视角强调跨班协调、SLA 和规则配置质量，首页要支持持续督办而不是单次查询。',
    summaryItems: [
      { label: '督办重点', value: '超时处置', caption: '先看处理中的事件是否拖延，再看是否需要跨组织协调。' },
      { label: '策略治理', value: '规则 / 预案', caption: '阈值、联动和预案要能支撑现场执行，而不是停留在配置层。' },
      { label: '台账维护', value: '风险点主数据', caption: '持续校核风险点、设备、测点和责任归属的一致性。' },
      { label: '当前口径', value: '静态经营编排', caption: '首页突出业务经营感，但仍不依赖不稳定聚合接口。' }
    ],
    metrics: [
      { label: '督办视角主线', value: '5', badge: { label: '协同驱动', tone: 'brand' } },
      { label: '策略治理域', value: '3', badge: { label: '需复核', tone: 'warning' } },
      { label: '支撑工作区', value: '1', badge: { label: '实施配套', tone: 'success' } },
      { label: '处置时限', value: '2h', badge: { label: 'SLA 关注', tone: 'danger' } }
    ],
    focusDistribution: [
      { name: '预警处置', value: 7, color: '#ff6d6d' },
      { name: '风险点治理', value: 6, color: '#ffb347' },
      { name: '规则与预案', value: 6, color: '#1e80ff' },
      { name: '治理底座', value: 3, color: '#52c41a' },
      { name: '接入支撑', value: 4, color: '#7a8ca6' }
    ],
    moduleOrder: ['event', 'risk', 'rules', 'alarm', 'report'],
    governanceDescriptions: {
      organization: '组织与区域用于划分督办范围、责任边界和跨班协同对象。',
      people: '用户、角色与权限要支撑排班、职责和运维链路授权。',
      trace: '通知渠道和审计日志用于追踪督办动作、升级记录和关键变更。'
    },
    todos: [
      {
        id: 'ops-event',
        priority: 'P1',
        window: '今日重点',
        title: '超时处置关注',
        detail: '优先查看处理中的事件是否超时，并确认是否需要升级督办。',
        hint: '落点：事件处置',
        actionLabel: '查看事件',
        path: '/event-disposal',
        tone: 'danger'
      },
      {
        id: 'ops-risk',
        priority: 'P1',
        window: '今日校核',
        title: '风险点台账复核',
        detail: '核对风险点与设备绑定、测点映射和责任区域，避免监测链路失真。',
        hint: '落点：风险点管理',
        actionLabel: '核对台账',
        path: '/risk-point',
        tone: 'warning'
      },
      {
        id: 'ops-rules',
        priority: 'P2',
        window: '本周复查',
        title: '规则策略复核',
        detail: '复查阈值、联动和预案配置是否与当前风险分级和现场处置能力一致。',
        hint: '落点：规则与预案',
        actionLabel: '查看规则',
        path: '/rule-definition',
        tone: 'brand'
      },
      {
        id: 'ops-support',
        priority: 'P2',
        window: '实施支撑',
        title: '接入异常核查',
        detail: '必要时回到接入验证、监测对象工作台和数据完整性校验，确认链路质量。',
        hint: '落点：设备接入区',
        actionLabel: '进入接入区',
        path: '/devices',
        tone: 'success'
      }
    ],
    deliveryNote: '运维主管视角会把接入支撑重新拉回首页，但其权重仍低于事件、台账和规则治理主线。'
  },
  manager: {
    label: '管理层',
    viewCaption: '面向风险经营、闭环效率和组织治理',
    heroTitle: '把风险经营、闭环效率与组织治理放到首页第一屏',
    heroDescription:
      '管理层首页更强调趋势、效率和组织责任，不再被技术联调和实施噪音占据版面，让首页真正承担经营驾驶作用。',
    heroTags: ['风险趋势', '闭环效率', '组织治理', '经营复盘'],
    focusTitle: '先看趋势和闭环效率，再判断资源与治理动作是否需要调整',
    focusDescription: '管理层视角把报表、重大告警、跨区域协同和治理底座提到首页最前面。',
    summaryItems: [
      { label: '经营关注', value: '趋势 + 效率', caption: '通过报表、重大告警和事件闭环效率观察经营质量。' },
      { label: '组织杠杆', value: '区域 / 权限 / 审计', caption: '关注责任边界、组织执行力和关键动作是否可追溯。' },
      { label: '决策节奏', value: '日报 / 周报', caption: '首页要服务管理复盘，而不只是单次业务办理。' },
      { label: '当前口径', value: '产品首页', caption: '保持商业产品叙事，但不虚构后端真实聚合统计。' }
    ],
    metrics: [
      { label: '经营关注域', value: '5', badge: { label: '管理视角', tone: 'brand' } },
      { label: '复盘报表类', value: '4', badge: { label: '决策支撑', tone: 'success' } },
      { label: '治理抓手', value: '3', badge: { label: '组织协同', tone: 'warning' } },
      { label: '管理节奏', value: '周/月', badge: { label: '持续复盘', tone: 'danger' } }
    ],
    focusDistribution: [
      { name: '分析复盘', value: 7, color: '#1e80ff' },
      { name: '治理底座', value: 6, color: '#52c41a' },
      { name: '预警处置', value: 5, color: '#ff6d6d' },
      { name: '风险点治理', value: 5, color: '#ffb347' },
      { name: '接入支撑', value: 2, color: '#7a8ca6' }
    ],
    moduleOrder: ['report', 'alarm', 'event', 'risk', 'rules'],
    governanceDescriptions: {
      organization: '组织与区域是管理层观察责任边界、属地态势和资源配置的基础盘。',
      people: '通过用户、角色和权限治理控制执行边界，确保管理动作落地。',
      trace: '通知与审计用于复盘关键决策、重点处置和治理动作是否真正闭环。'
    },
    todos: [
      {
        id: 'manager-report',
        priority: 'P1',
        window: '今日简报',
        title: '风险经营复盘',
        detail: '优先查看趋势、告警统计、事件闭环和设备健康，判断经营面是否有异常抬头。',
        hint: '落点：分析报表',
        actionLabel: '查看报表',
        path: '/report-analysis',
        tone: 'brand'
      },
      {
        id: 'manager-alarm',
        priority: 'P1',
        window: '重大事项',
        title: '重大告警关注',
        detail: '聚焦高等级告警和持续未关闭事项，确认是否需要资源倾斜或升级处置。',
        hint: '落点：告警中心',
        actionLabel: '查看告警',
        path: '/alarm-center',
        tone: 'danger'
      },
      {
        id: 'manager-event',
        priority: 'P2',
        window: '跨区域协同',
        title: '事件闭环协调',
        detail: '关注跨组织事件的派单、接单和完成情况，评估执行链路是否顺畅。',
        hint: '落点：事件处置',
        actionLabel: '查看事件',
        path: '/event-disposal',
        tone: 'warning'
      },
      {
        id: 'manager-governance',
        priority: 'P2',
        window: '治理复查',
        title: '组织权限与审计复查',
        detail: '复查组织、权限、通知和审计能力是否支撑当前风险治理和经营要求。',
        hint: '落点：系统治理',
        actionLabel: '进入治理',
        path: '/organization',
        tone: 'success'
      }
    ],
    deliveryNote: '管理层视角保持产品交付口径透明，首页主要承担经营驾驶作用，不冒充真实 BI 聚合大屏。'
  }
};

const deliveryProgress: DeliveryProgressItem[] = [
  { name: '告警中心', value: 100, color: '#ff6d6d' },
  { name: '事件处置', value: 100, color: '#ff8f3d' },
  { name: '风险点管理', value: 100, color: '#ffb347' },
  { name: '规则与预案', value: 100, color: '#1e80ff' },
  { name: '分析报表', value: 100, color: '#52c41a' },
  { name: '实时监测', value: 70, color: '#7a8ca6' },
  { name: 'GIS 态势', value: 65, color: '#95a3b8' }
];

const workflowSteps: WorkflowStep[] = [
  {
    index: '01',
    title: '风险点建档',
    description: '在风险点管理中完成风险点、设备和测点绑定，建立后续预警处置的监测对象。',
    path: '/risk-point'
  },
  {
    index: '02',
    title: '阈值与联动编排',
    description: '通过阈值规则、联动规则和应急预案定义触发条件与协同动作。',
    path: '/rule-definition'
  },
  {
    index: '03',
    title: '告警触发',
    description: '异常达到阈值后进入告警中心，支持确认、抑制、关闭等标准操作。',
    path: '/alarm-center'
  },
  {
    index: '04',
    title: '事件派单',
    description: '在事件处置中完成派单、接单、处理、反馈和关闭，形成工单流转。',
    path: '/event-disposal'
  },
  {
    index: '05',
    title: '分析复盘',
    description: '通过分析报表查看风险趋势、告警统计、事件闭环和设备健康概况。',
    path: '/report-analysis'
  },
  {
    index: '06',
    title: '组织治理',
    description: '组织、用户、角色、区域、渠道与审计日志共同构成运营治理底座。',
    path: '/organization'
  }
];

const moduleCatalog: Record<ModuleKey, ModuleCard> = {
  alarm: {
    badge: '闭环主线',
    title: '告警中心',
    description: '聚焦告警列表、确认、抑制与关闭，是预警闭环的第一响应入口。',
    footer: '对应 /api/alarm/*',
    path: '/alarm-center'
  },
  event: {
    badge: '工单执行',
    title: '事件处置',
    description: '承接派单、接单、处理、反馈和关闭，连接现场处置动作与协同。',
    footer: '对应 /api/event/*',
    path: '/event-disposal'
  },
  risk: {
    badge: '监测对象',
    title: '风险点管理',
    description: '维护风险点、设备绑定和测点绑定，定义平台真正要监测和治理的对象。',
    footer: '对应 /api/risk-point/*',
    path: '/risk-point'
  },
  rules: {
    badge: '策略治理',
    title: '规则与预案',
    description: '把阈值规则、联动规则和应急预案组织成业务化处置策略。',
    footer: '规则、联动、预案配置',
    path: '/rule-definition'
  },
  report: {
    badge: '决策复盘',
    title: '分析报表',
    description: '用趋势、统计、闭环和设备健康四类视角支撑经营复盘与管理决策。',
    footer: '对应 /api/report/*',
    path: '/report-analysis'
  }
};

const supportCapability = {
  title: '设备接入与验证',
  description: '把产品建模、设备建档、接入验证中心、监测对象工作台和数据完整性校验收口为实施支撑能力。',
  tags: ['产品模板', '设备建档', '接入验证中心', '监测对象工作台', '数据完整性校验'],
  path: '/devices'
};

const router = useRouter();
const permissionStore = usePermissionStore();

const capabilityChartRef = ref<HTMLElement | null>(null);
const deliveryChartRef = ref<HTMLElement | null>(null);
const currentTime = ref('');
const viewMode = ref<ViewMode>(inferViewMode());

let capabilityChart: ECharts | null = null;
let deliveryChart: ECharts | null = null;
let timeTimer: number | null = null;

const viewOptions = [
  { value: 'duty' as const, label: '值班人员', caption: '先响应、先交接' },
  { value: 'ops' as const, label: '运维主管', caption: '督办、策略、台账' },
  { value: 'manager' as const, label: '管理层', caption: '趋势、效率、治理' }
];

const viewerName = computed(() => {
  return permissionStore.userInfo?.displayName || permissionStore.userInfo?.realName || permissionStore.userInfo?.username || '访客预览';
});

const activeScenario = computed(() => scenarioPresets[viewMode.value]);
const heroTags = computed(() => activeScenario.value.heroTags);
const summaryItems = computed(() => activeScenario.value.summaryItems);
const overviewMetrics = computed(() => activeScenario.value.metrics);
const focusDistribution = computed(() => activeScenario.value.focusDistribution);
const primaryModuleCards = computed(() => activeScenario.value.moduleOrder.map((key) => moduleCatalog[key]));
const todoItems = computed(() => activeScenario.value.todos);

const governanceCards = computed<GovernanceCard[]>(() => [
  {
    title: '组织与区域',
    description: activeScenario.value.governanceDescriptions.organization,
    footer: '组织树、区域树',
    path: '/organization'
  },
  {
    title: '人员与权限',
    description: activeScenario.value.governanceDescriptions.people,
    footer: '用户、角色、权限',
    path: '/user'
  },
  {
    title: '通知与审计',
    description: activeScenario.value.governanceDescriptions.trace,
    footer: '渠道、审计日志',
    path: '/channel'
  }
]);

const roleSummary = computed(() => `${activeScenario.value.label}视角 · ${viewerName.value}`);

const latestActivityTrace = computed<ActivityTraceItem | null>(() => {
  const entry = activityEntries.value.find((item) => !(item.module === '平台首页' && item.action.includes('视角')) && item.action !== '访问首页');
  if (!entry) {
    return null;
  }

  return {
    time: formatActivityTime(entry.createdAt),
    title: `${entry.module} · ${entry.action}`,
    detail: entry.detail
  };
});

function inferViewMode(): ViewMode {
  const roleText = [permissionStore.primaryRoleName, ...permissionStore.roleNames, ...permissionStore.roleCodes]
    .join(' ')
    .toLowerCase();

  if (/manager|leader|director|admin|super|管理|领导|总监|负责人/.test(roleText)) {
    return 'manager';
  }

  if (/ops|operation|maint|maintenance|运维|主管|维护|调度/.test(roleText)) {
    return 'ops';
  }

  return 'duty';
}

function setViewMode(nextMode: ViewMode) {
  if (viewMode.value === nextMode) {
    return;
  }
  viewMode.value = nextMode;
}

function updateTime() {
  currentTime.value = new Date().toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}

function formatActivityTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '刚刚';
  }

  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  });
}

function renderCapabilityChart() {
  if (!capabilityChartRef.value) {
    return;
  }

  capabilityChart ??= echarts.init(capabilityChartRef.value);
  capabilityChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: 'rgba(55, 78, 112, 0.16)',
      borderWidth: 1,
      textStyle: { color: '#1f2a3d' }
    },
    series: [
      {
        type: 'pie',
        radius: ['48%', '72%'],
        center: ['42%', '50%'],
        itemStyle: {
          borderWidth: 3,
          borderColor: '#f7f9fc'
        },
        label: {
          show: true,
          formatter: '{b}\n{c}',
          color: '#42546e',
          fontSize: 12
        },
        data: focusDistribution.value.map((item) => ({
          value: item.value,
          name: item.name,
          itemStyle: { color: item.color }
        }))
      }
    ]
  });
}

function renderDeliveryChart() {
  if (!deliveryChartRef.value) {
    return;
  }

  deliveryChart ??= echarts.init(deliveryChartRef.value);
  deliveryChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: 'rgba(55, 78, 112, 0.16)',
      borderWidth: 1,
      textStyle: { color: '#1f2a3d' }
    },
    grid: {
      left: 86,
      right: 24,
      top: 12,
      bottom: 12,
      containLabel: true
    },
    xAxis: {
      type: 'value',
      max: 100,
      splitLine: {
        lineStyle: {
          color: 'rgba(67, 98, 148, 0.12)'
        }
      },
      axisLabel: {
        color: '#728197',
        formatter: '{value}%'
      }
    },
    yAxis: {
      type: 'category',
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { color: '#42546e' },
      data: deliveryProgress.map((item) => item.name)
    },
    series: [
      {
        type: 'bar',
        barWidth: 12,
        data: deliveryProgress.map((item) => ({
          value: item.value,
          itemStyle: {
            color: item.color,
            borderRadius: 999
          }
        })),
        label: {
          show: true,
          position: 'right',
          color: '#52637c',
          formatter: '{c}%'
        }
      }
    ]
  });
}

function resizeCharts() {
  capabilityChart?.resize();
  deliveryChart?.resize();
}

function navigateTo(path: string) {
  router.push(path);
}

watch(viewMode, async (mode, previousMode) => {
  if (mode === previousMode) {
    return;
  }

  await nextTick();
  renderCapabilityChart();
  recordActivity({
    module: '平台首页',
    action: `切换${scenarioPresets[mode].label}视角`,
    request: { path: '/', viewMode: mode },
    ok: true,
    detail: `首页切换到${scenarioPresets[mode].label}视角，重排经营主线入口与待办中心。`
  });
});

onMounted(async () => {
  updateTime();
  timeTimer = window.setInterval(updateTime, 1000);

  await nextTick();
  renderCapabilityChart();
  renderDeliveryChart();
  window.addEventListener('resize', resizeCharts);

  recordActivity({
    module: '平台首页',
    action: '访问首页',
    request: { path: '/', viewMode: viewMode.value },
    ok: true,
    detail: '用户进入监测预警平台首页，查看角色化经营首页与待办中心。'
  });
});

onUnmounted(() => {
  if (timeTimer) {
    clearInterval(timeTimer);
    timeTimer = null;
  }

  capabilityChart?.dispose();
  capabilityChart = null;
  deliveryChart?.dispose();
  deliveryChart = null;
  window.removeEventListener('resize', resizeCharts);
});
</script>

<style scoped>
.cockpit-page {
  display: grid;
  gap: 1rem;
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(340px, 0.85fr);
  gap: 1rem;
  padding: 1.4rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    radial-gradient(circle at top right, rgba(255, 106, 0, 0.16), transparent 34%),
    radial-gradient(circle at bottom left, rgba(30, 128, 255, 0.12), transparent 28%),
    linear-gradient(150deg, rgba(255, 255, 255, 0.99), rgba(244, 248, 255, 0.96));
  box-shadow: var(--shadow-panel);
}

.hero-panel__copy {
  display: grid;
  gap: 1rem;
}

.hero-panel__eyebrow {
  margin: 0;
  color: var(--brand-bright);
  letter-spacing: 0.18em;
  text-transform: uppercase;
  font-size: 0.72rem;
  font-weight: 600;
}

.hero-panel__copy h1 {
  margin: 0;
  font-size: clamp(2rem, 3vw, 2.8rem);
  line-height: 1.16;
  color: var(--text-primary);
  max-width: 13ch;
}

.hero-panel__description {
  margin: 0;
  max-width: 60ch;
  color: var(--text-secondary);
  line-height: 1.8;
  font-size: 0.96rem;
}

.hero-panel__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.hero-panel__tags span {
  display: inline-flex;
  align-items: center;
  padding: 0.45rem 0.85rem;
  border-radius: 999px;
  background: rgba(255, 106, 0, 0.09);
  color: var(--brand-deep);
  border: 1px solid rgba(255, 106, 0, 0.14);
  font-size: 0.82rem;
}

.hero-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem;
}

.hero-panel__brief {
  display: grid;
  gap: 1rem;
  padding: 1.1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(41, 60, 92, 0.12);
  background: rgba(19, 31, 53, 0.94);
  color: rgba(255, 255, 255, 0.88);
}

.brief-head,
.brief-foot {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.brief-head span,
.brief-foot span,
.focus-card span {
  color: rgba(255, 255, 255, 0.58);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-size: 0.72rem;
}

.brief-head strong,
.brief-foot strong,
.focus-card strong {
  color: #fff;
}

.brief-head strong {
  display: block;
  margin-top: 0.22rem;
  font-size: 0.98rem;
}

.brief-head small {
  max-width: 12rem;
  color: rgba(255, 255, 255, 0.68);
  line-height: 1.5;
  text-align: right;
}

.view-switch {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.65rem;
}

.view-switch__option {
  display: grid;
  gap: 0.22rem;
  padding: 0.8rem 0.85rem;
  border-radius: var(--radius-md);
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.72);
  text-align: left;
  cursor: pointer;
  transition:
    transform var(--transition-fast),
    border-color var(--transition-fast),
    background-color var(--transition-fast);
}

.view-switch__option strong,
.view-switch__option span {
  display: block;
}

.view-switch__option strong {
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.88rem;
}

.view-switch__option span {
  color: rgba(255, 255, 255, 0.58);
  font-size: 0.72rem;
}

.view-switch__option:hover {
  transform: translateY(-1px);
  border-color: rgba(255, 255, 255, 0.18);
}

.view-switch__option--active {
  background: linear-gradient(135deg, rgba(255, 123, 26, 0.28), rgba(30, 128, 255, 0.18));
  border-color: rgba(255, 170, 92, 0.48);
}

.focus-card {
  display: grid;
  gap: 0.28rem;
  padding: 0.95rem;
  border-radius: var(--radius-md);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.08), rgba(255, 106, 0, 0.12));
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.focus-card strong {
  font-size: 1.02rem;
}

.focus-card p {
  margin: 0;
  color: rgba(255, 255, 255, 0.74);
  line-height: 1.7;
}

.brief-list {
  display: grid;
  gap: 0.7rem;
}

.brief-item {
  display: grid;
  gap: 0.14rem;
  padding: 0.8rem 0.9rem;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.brief-item span {
  color: rgba(255, 255, 255, 0.56);
  font-size: 0.74rem;
}

.brief-item strong {
  color: #fff;
  font-size: 1rem;
}

.brief-item small {
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

.insight-grid,
.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.chart-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr);
  gap: 1rem;
  align-items: center;
}

.chart-canvas {
  min-height: 300px;
}

.chart-canvas--compact {
  min-height: 310px;
}

.chart-legend {
  display: grid;
  gap: 0.8rem;
}

.chart-legend__item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 0.85rem;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid var(--panel-border);
}

.chart-legend__dot {
  width: 0.75rem;
  height: 0.75rem;
  border-radius: 999px;
  flex: none;
}

.chart-legend__item strong,
.chart-legend__item small {
  display: block;
}

.chart-legend__item strong {
  color: var(--text-primary);
}

.chart-legend__item small {
  margin-top: 0.18rem;
  color: var(--text-secondary);
}

.progress-note {
  margin: 0.6rem 0 0;
  color: var(--text-secondary);
  line-height: 1.7;
  font-size: 0.82rem;
}

.workflow-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 0.9rem;
}

.workflow-step {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.8rem;
  align-items: start;
}

.workflow-step__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.4rem;
  height: 2.4rem;
  border-radius: 0.75rem;
  background: rgba(255, 106, 0, 0.12);
  color: var(--brand-deep);
  font-weight: 700;
  font-family: var(--font-display);
}

.workflow-step__content {
  padding: 0.85rem 0.95rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: rgba(255, 255, 255, 0.78);
}

.workflow-step__title {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.workflow-step__title strong {
  color: var(--text-primary);
  font-size: 0.96rem;
}

.workflow-step__title button,
.todo-card__footer button {
  border: none;
  background: transparent;
  color: var(--brand-bright);
  font-size: 0.82rem;
  cursor: pointer;
}

.workflow-step__content p {
  margin: 0.45rem 0 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.module-card,
.governance-card,
.support-card {
  width: 100%;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.84);
  text-align: left;
  cursor: pointer;
  transition: transform var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.module-card {
  padding: 1rem;
  display: grid;
  gap: 0.55rem;
}

.module-card:hover,
.governance-card:hover,
.support-card:hover {
  transform: translateY(-2px);
  border-color: var(--panel-border-hover);
  box-shadow: var(--shadow-sm);
}

.module-card__head,
.support-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--brand-bright);
  font-size: 0.78rem;
}

.module-card strong,
.governance-card strong,
.support-card strong,
.todo-card strong,
.trace-card strong {
  font-size: 1rem;
  color: var(--text-primary);
}

.module-card p,
.governance-card p,
.support-card p,
.todo-card p,
.trace-card p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.module-card small,
.governance-card span {
  color: var(--text-tertiary);
  font-size: 0.8rem;
}

.support-card {
  margin-top: 0.9rem;
  padding: 1rem 1.05rem;
  display: grid;
  gap: 0.5rem;
  background:
    linear-gradient(150deg, rgba(248, 250, 255, 0.96), rgba(241, 246, 255, 0.92)),
    radial-gradient(circle at top left, rgba(30, 128, 255, 0.1), transparent 40%);
}

.support-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.support-card__tags span {
  display: inline-flex;
  align-items: center;
  padding: 0.3rem 0.65rem;
  border-radius: 999px;
  background: rgba(30, 128, 255, 0.08);
  color: #1e80ff;
  font-size: 0.76rem;
}

.governance-grid,
.todo-list {
  display: grid;
  gap: 0.8rem;
}

.governance-card {
  padding: 1rem 1.05rem;
  display: grid;
  gap: 0.4rem;
}

.todo-card {
  display: grid;
  gap: 0.6rem;
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: rgba(255, 255, 255, 0.84);
}

.todo-card__meta,
.todo-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.8rem;
}

.todo-card__priority,
.todo-card__window {
  display: inline-flex;
  align-items: center;
  min-height: 1.6rem;
  padding: 0 0.6rem;
  border-radius: 999px;
  font-size: 0.74rem;
}

.todo-card__priority {
  background: rgba(19, 31, 53, 0.08);
  color: var(--text-primary);
  font-weight: 700;
}

.todo-card__window {
  background: rgba(30, 128, 255, 0.08);
  color: #3666c5;
}

.todo-card__footer span {
  color: var(--text-tertiary);
  font-size: 0.8rem;
}

.todo-card--danger {
  border-color: rgba(255, 109, 109, 0.22);
}

.todo-card--warning {
  border-color: rgba(255, 179, 71, 0.24);
}

.todo-card--brand {
  border-color: rgba(30, 128, 255, 0.22);
}

.todo-card--success {
  border-color: rgba(82, 196, 26, 0.22);
}

.trace-card {
  margin-top: 0.9rem;
  display: grid;
  gap: 0.3rem;
  padding: 0.95rem 1rem;
  border-radius: var(--radius-md);
  border: 1px dashed rgba(67, 98, 148, 0.24);
  background: rgba(247, 249, 252, 0.8);
}

.trace-card span,
.trace-card small {
  color: var(--text-tertiary);
  font-size: 0.78rem;
}

.hero-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.7rem;
  padding: 0.7rem 1.05rem;
  border: 1px solid rgba(53, 74, 109, 0.14);
  border-radius: 0.7rem;
  background: rgba(255, 255, 255, 0.92);
  color: var(--text-primary);
  font: inherit;
  cursor: pointer;
  transition:
    transform 160ms ease,
    border-color 160ms ease,
    box-shadow 160ms ease,
    background-color 160ms ease;
}

.hero-action:hover {
  transform: translateY(-1px);
  border-color: rgba(255, 106, 0, 0.22);
  box-shadow: 0 10px 20px rgba(37, 58, 99, 0.08);
}

.hero-action:focus-visible,
.view-switch__option:focus-visible,
.module-card:focus-visible,
.support-card:focus-visible,
.governance-card:focus-visible,
.todo-card__footer button:focus-visible,
.workflow-step__title button:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(255, 106, 0, 0.16);
}

.hero-action--primary {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #ff7b1a, #ff9b42);
}

@media (max-width: 1280px) {
  .hero-panel,
  .insight-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .chart-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .metric-grid,
  .module-grid,
  .view-switch {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .cockpit-page {
    gap: 0.85rem;
  }

  .hero-panel,
  .hero-panel__brief {
    padding: 1rem;
  }

  .metric-grid,
  .module-grid,
  .view-switch {
    grid-template-columns: 1fr;
  }

  .hero-panel__actions,
  .todo-card__meta,
  .todo-card__footer,
  .support-card__head,
  .brief-head,
  .brief-foot,
  .workflow-step__title {
    display: grid;
    grid-template-columns: 1fr;
  }

  .hero-panel__actions {
    align-items: stretch;
  }
}
</style>
