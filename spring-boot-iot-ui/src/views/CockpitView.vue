<template>
  <div class="page-stack">
    <section class="hero-grid">
      <div class="hero-panel overview-shell">
        <p class="eyebrow">Commercial Product Vision</p>
        <h1 class="headline">把调试平台升级为智能风险监测与处置驾驶舱</h1>
        <p class="lead">
          平台面向一线监测人员、运维维护人员和开发人员协同工作：从传感器数据接入，到风险研判、
          报告输出、远程处置与链路审计，形成一套可落地、可演进、可交付的商业产品。
        </p>
        <div class="button-row" style="margin-top: 1.25rem;">
          <el-button class="primary-button" type="primary" @click="router.push('/insight')">
            进入风险点工作台
          </el-button>
          <el-button class="secondary-button" @click="router.push('/devices')">
            打开设备运维中心
          </el-button>
          <el-button class="ghost-button" @click="router.push('/reporting')">
            查看接入回放台
          </el-button>
        </div>

        <div class="risk-spectrum">
          <article
            v-for="level in riskLevels"
            :key="level.label"
            class="risk-spectrum__card"
            :class="`risk-spectrum__card--${level.tone}`"
          >
            <p>{{ level.label }}</p>
            <strong>{{ level.description }}</strong>
            <span>{{ level.action }}</span>
          </article>
        </div>
      </div>

      <PanelCard
        eyebrow="Command Focus"
        title="今日重点工作"
        description="商业产品首页应该先告诉用户今天最重要的风险、动作和入口，而不是先让用户找接口。"
      >
        <div class="priority-list">
          <article v-for="item in dailyPriorities" :key="item.title" class="priority-list__item">
            <span class="priority-list__badge">{{ item.badge }}</span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
            </div>
          </article>
        </div>
      </PanelCard>
    </section>

    <section class="quad-grid">
      <MetricCard
        v-for="metric in metrics"
        :key="metric.label"
        :label="metric.label"
        :value="metric.value"
        :hint="metric.hint"
        :badge="metric.badge"
      />
    </section>

    <section class="two-column-grid">
      <PanelCard
        eyebrow="Role Workspace"
        title="角色化工作入口"
        description="同一套平台为三类核心用户服务，但每类人进入平台后看到的第一动作和最关心的信息并不相同。"
      >
        <div class="workspace-grid">
          <article v-for="role in roleWorkspaces" :key="role.title" class="workspace-card">
            <header class="workspace-card__header">
              <span class="workspace-card__icon">{{ role.icon }}</span>
              <div>
                <h3>{{ role.title }}</h3>
                <p>{{ role.subtitle }}</p>
              </div>
            </header>
            <ul class="workspace-card__list">
              <li v-for="item in role.items" :key="item">{{ item }}</li>
            </ul>
          </article>
        </div>
      </PanelCard>

      <PanelCard
        eyebrow="Risk Workflow"
        title="风险处置闭环"
        description="商业产品的核心不是设备接入，而是把风险发现、研判、上报、处置和复盘真正串成流程。"
      >
        <div class="flow-rail">
          <div v-for="step in riskWorkflow" :key="step.index" class="flow-rail__item">
            <span class="flow-rail__index">{{ step.index }}</span>
            <div>
              <strong>{{ step.title }}</strong>
              <span>{{ step.description }}</span>
            </div>
          </div>
        </div>
      </PanelCard>
    </section>

    <section class="two-column-grid">
      <PanelCard
        eyebrow="Current Runtime"
        title="现有平台能力映射"
        description="把已经完成的 Phase 1 / Phase 2 能力重新组织成更接近商业产品的业务语言。"
      >
        <div class="capability-board">
          <article v-for="domain in capabilityDomains" :key="domain.title" class="capability-board__item">
            <h3>{{ domain.title }}</h3>
            <p>{{ domain.description }}</p>
            <ul class="phase-ideas">
              <li v-for="item in domain.items" :key="item">{{ item }}</li>
            </ul>
          </article>
        </div>
      </PanelCard>

      <PanelCard
        eyebrow="Recent Activity"
        title="最近调试与运行轨迹"
        description="这里保留研发和实施团队需要的轨迹视图，便于在产品化界面下继续完成联调、定位与验证。"
      >
        <div v-if="activities.length" class="timeline">
          <article v-for="entry in activities" :key="entry.id" class="timeline-item">
            <h3>{{ entry.module }} / {{ entry.action }}</h3>
            <p>{{ entry.detail }}</p>
            <p>{{ formatDateTime(entry.createdAt) }}</p>
          </article>
        </div>
        <div v-else class="empty-state">
          还没有产生调试记录。先去“设备运维中心”或“接入回放台”触发一次调用，这里会自动出现轨迹。
        </div>
      </PanelCard>
    </section>

    <section class="tri-grid">
      <PanelCard
        v-for="feature in evolutionRoadmap"
        :key="feature.title"
        eyebrow="Roadmap"
        :title="feature.title"
        :description="feature.description"
      >
        <ul class="phase-ideas">
          <li v-for="item in feature.items" :key="item">{{ item }}</li>
        </ul>
      </PanelCard>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';

import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import { activityEntries } from '../stores/activity';
import { formatDateTime } from '../utils/format';

const router = useRouter();

const riskLevels = [
  { label: '红色风险', description: '立即上报', action: '启动应急响应与专题分析报告', tone: 'red' },
  { label: '橙色风险', description: '重点跟踪', action: '安排复测与专项巡检', tone: 'orange' },
  { label: '黄色风险', description: '持续观察', action: '提高监测频次并校验阈值', tone: 'yellow' },
  { label: '蓝色风险', description: '常规管理', action: '维持日常巡检与数据留痕', tone: 'blue' }
];

const dailyPriorities = [
  { badge: 'P1', title: '高风险点优先处置', description: '优先查看红橙等级风险点，判断是否需要立即上报领导与生成风险报告。' },
  { badge: 'P2', title: '设备在线健康巡检', description: '关注离线设备、弱信号设备和最近无上报设备，避免风险点监测盲区。' },
  { badge: 'P3', title: '接入链路抽样验证', description: '研发与实施人员通过 HTTP / MQTT 调试台验证主链路是否完整可用。' }
];

const metrics = [
  {
    label: '四色风险模型',
    value: '4',
    hint: '围绕红橙黄蓝形成统一的风险分级和业务处置语言。',
    badge: { label: 'Core', tone: 'danger' as const }
  },
  {
    label: '核心角色工作台',
    value: '3',
    hint: '一线人员、运维人员、开发人员都能在同一平台找到主入口。',
    badge: { label: 'Users', tone: 'brand' as const }
  },
  {
    label: '现有联调接口',
    value: '10',
    hint: '现有接口已经覆盖产品、设备、上报、属性、日志与文件调试能力。',
    badge: { label: 'Ready', tone: 'success' as const }
  },
  {
    label: '业务闭环阶段',
    value: '6',
    hint: '发现、研判、上报、处置、运维、审计构成产品主线。',
    badge: { label: 'Workflow', tone: 'warning' as const }
  }
];

const roleWorkspaces = [
  {
    icon: 'F1',
    title: '一线工作人员',
    subtitle: '快速发现风险、判级、上报、出报告',
    items: ['风险点工作台', '趋势曲线与异常点查看', 'AI 研判与风险建议', '一键生成风险分析报告']
  },
  {
    icon: 'O2',
    title: '运维维护人员',
    subtitle: '远程控制设备、维护阈值、巡检健康状态',
    items: ['设备运维中心', '设备在线 / 离线状态', '参数阈值与远程控制入口', '固件与文件调试入口']
  },
  {
    icon: 'D3',
    title: '开发与实施人员',
    subtitle: '调协议、看链路、验完整性、快速定位问题',
    items: ['接入回放台', 'MQTT / HTTP 链路验证', '文件与固件调试', '日志审计与报文回看']
  }
];

const riskWorkflow = [
  { index: '01', title: '数据接入', description: '传感器通过 HTTP / MQTT 上报数据，进入统一主链路。' },
  { index: '02', title: '协议标准化', description: '明文、加密、C.1-C.4 数据格式统一解码与校验。' },
  { index: '03', title: '风险判定', description: '属性更新后结合规则与 AI 能力形成风险等级建议。' },
  { index: '04', title: '人工上报', description: '一线人员确认风险并上报领导，同时形成报告留痕。' },
  { index: '05', title: '运维处置', description: '运维人员远程控制设备、调整阈值、排查现场问题。' },
  { index: '06', title: '审计复盘', description: '研发与管理层基于日志、操作、趋势完成复盘和优化。' }
];

const capabilityDomains = [
  {
    title: '风险业务层',
    description: '把数据查看能力转换成风险识别、趋势分析和报告产出能力。',
    items: ['风险点工作台', '属性与消息日志聚合', '趋势与预测入口预留']
  },
  {
    title: '运维执行层',
    description: '把设备管理、阈值设置和远程控制汇聚成运维中心。',
    items: ['产品模板中心', '设备运维中心', '设备在线与会话状态']
  },
  {
    title: '研发调试层',
    description: '保留当前联调和协议验证能力，为实施和开发提供快速闭环。',
    items: ['HTTP / MQTT 接入回放', 'C.3 / C.4 文件调试', '协议安全与日志审计']
  }
];

const evolutionRoadmap = [
  {
    title: '风险地图与区域态势',
    description: '把风险点、设备位置、处置状态和等级分布整合成领导可看的态势视图。',
    items: ['区域风险热力', '点位分布与筛选', '重大风险专题图层']
  },
  {
    title: 'AI 分析与报告中心',
    description: '基于多传感器数据自动输出风险分析、处置建议和报告草稿。',
    items: ['AI 风险研判', '自动报告生成', '未来走势预测']
  },
  {
    title: '远程运维与闭环运营',
    description: '从设备控制、阈值策略到事件闭环和审计中心，逐步形成企业级能力。',
    items: ['远程控制', '阈值策略中心', '告警与工单闭环']
  }
];

const activities = computed(() => activityEntries.value.slice(0, 5));
</script>

<style scoped>
.phase-ideas {
  margin: 0;
  padding-left: 1.1rem;
  line-height: 1.9;
}
</style>
