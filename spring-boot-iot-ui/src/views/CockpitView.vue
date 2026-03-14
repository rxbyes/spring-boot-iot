<template>
  <div class="page-stack">
    <section class="hero-grid">
      <div class="hero-panel">
        <p class="eyebrow">Phase 1 Debug Mission</p>
        <h1 class="headline">把后端主链路变成一块可视化的物联网调试台</h1>
        <p class="lead">
          当前前端以产品、设备、HTTP 模拟上报、属性快照与消息日志为核心，布局借鉴 `vue-element-admin`
          的后台工作台结构，视觉上则强化工业感、协议感和实时联调氛围。
        </p>
        <div class="button-row" style="margin-top: 1.25rem;">
          <button class="primary-button" type="button" @click="router.push('/reporting')">
            进入上报实验台
          </button>
          <button class="secondary-button" type="button" @click="router.push('/insight')">
            查看设备洞察
          </button>
        </div>
      </div>

      <PanelCard
        eyebrow="Design Direction"
        title="Retro-Future Admin"
        description="保留后台系统的导航效率，同时把协议链路、设备状态和未来扩展面板组织进同一块驾驶舱。"
      >
        <div class="flow-rail">
          <div v-for="item in northStar" :key="item.title" class="flow-rail__item">
            <span class="flow-rail__index">{{ item.index }}</span>
            <div>
              <strong>{{ item.title }}</strong>
              <span>{{ item.description }}</span>
            </div>
          </div>
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
        eyebrow="Verified Flow"
        title="主链路分层"
        description="来源于项目文档，便于在联调时快速定位“接入层 / 协议层 / 业务层”的责任边界。"
      >
        <div class="flow-rail">
          <div v-for="step in messageFlow" :key="step.title" class="flow-rail__item">
            <span class="flow-rail__index">{{ step.index }}</span>
            <div>
              <strong>{{ step.title }}</strong>
              <span>{{ step.description }}</span>
            </div>
          </div>
        </div>
      </PanelCard>

      <PanelCard
        eyebrow="Recent Activity"
        title="最近调试记录"
        description="这里显示当前前端会话里最近触发的接口调用，便于回看联调顺序和返回结果。"
      >
        <div v-if="activities.length" class="timeline">
          <article v-for="entry in activities" :key="entry.id" class="timeline-item">
            <h3>{{ entry.module }} / {{ entry.action }}</h3>
            <p>{{ entry.detail }}</p>
            <p>{{ formatDateTime(entry.createdAt) }}</p>
          </article>
        </div>
        <div v-else class="empty-state">
          还没有产生调试记录。先去“产品工作台”或“HTTP 上报实验台”触发一次调用，这里会自动出现轨迹。
        </div>
      </PanelCard>
    </section>

    <section class="tri-grid">
      <PanelCard
        v-for="feature in futureFeatures"
        :key="feature.title"
        eyebrow="Future Slot"
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

const northStar = [
  { index: 'A1', title: '后台骨架', description: '借鉴 vue-element-admin 的导航效率与工作台节奏。' },
  { index: 'A2', title: 'IoT 科技感', description: '深海色域、流体高亮、协议感网格与数字面板。' },
  { index: 'A3', title: '面向扩展', description: '为图表、拓扑、数字孪生和规则链提前留钩子。' }
];

const metrics = [
  {
    label: '已接入接口',
    value: '8',
    hint: '对齐 docs/04-api.md 中 Phase 1 已验证接口。',
    badge: { label: 'Phase 1', tone: 'success' as const }
  },
  {
    label: '活跃模块',
    value: '8',
    hint: '与父 POM 当前激活 reactor 模块保持一致。',
    badge: { label: 'Modular', tone: 'brand' as const }
  },
  {
    label: '主链路节点',
    value: '6',
    hint: '从 HTTP 入口到属性和在线状态刷新形成闭环。',
    badge: { label: 'Traceable', tone: 'warning' as const }
  },
  {
    label: '前瞻入口',
    value: '6',
    hint: '图表、孪生、拓扑、规则、告警、OTA 已预留入口。',
    badge: { label: 'Future', tone: 'brand' as const }
  }
];

const messageFlow = [
  { index: '01', title: 'DeviceHttpController', description: 'HTTP 模拟设备上报入口。' },
  { index: '02', title: 'UpMessageDispatcher', description: '把统一原始消息分发到协议层。' },
  { index: '03', title: 'ProtocolAdapterRegistry', description: '按 protocolCode 选择协议适配器。' },
  { index: '04', title: 'MqttJsonProtocolAdapter', description: '执行 mqtt-json 解码和标准化。' },
  { index: '05', title: 'DeviceMessageServiceImpl', description: '写消息日志、最新属性和在线状态。' },
  { index: '06', title: 'Query APIs', description: '再由属性与日志查询接口回看结果。' }
];

const futureFeatures = [
  {
    title: '点位图表中心',
    description: '未来把 `iot_device_property` 和时序库结果汇聚成点位趋势、健康分值和告警热力图。',
    items: ['折线趋势图', '多点位对比', '属性异常标记']
  },
  {
    title: '数字孪生场景',
    description: '把设备坐标、状态、属性和消息流映射到 2D / 3D 场景，形成资产态势感知。',
    items: ['设备位姿绑定', '实时状态光效', '场景告警联动']
  },
  {
    title: '协议与网关拓扑',
    description: '围绕后续 MQTT / TCP / Gateway 能力，展示边缘接入、子设备拓扑和协议适配情况。',
    items: ['网关树结构', '协议适配视图', '上下行链路追踪']
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
