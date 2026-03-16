<template>
  <div class="page-stack">
    <section class="hero-panel">
      <p class="eyebrow">Future Lab</p>
      <h1 class="headline">未来能力实验室</h1>
    </section>

    <section class="tri-grid">
      <PanelCard
        v-for="card in futureCards"
        :key="card.title"
        eyebrow="Planned Capability"
        :title="card.title"
      >
        <div class="flow-rail">
          <div v-for="point in card.contracts" :key="point.title" class="flow-rail__item">
            <span class="flow-rail__index">{{ point.index }}</span>
            <div>
              <strong>{{ point.title }}</strong>
            </div>
          </div>
        </div>
      </PanelCard>
    </section>

    <section class="two-column-grid">
      <PanelCard
        eyebrow="Integration Notes"
        title="当前页面已经预留的扩展点"
      >
        <ul class="phase-ideas">
          <li>设备洞察页已聚合 `device + properties + messageLogs`，适合作为图表画布的数据源。</li>
          <li>HTTP 上报实验台可继续扩展成协议报文回放器、Mock 场景器和规则链输入器。</li>
          <li>左侧导航与顶部环境栏已经是稳定的调试骨架，后续继续加模块不会破坏布局。</li>
          <li>视觉 token 已统一，后续增加 2D/3D 画布时可直接复用品牌色和状态色。</li>
        </ul>
      </PanelCard>

      <PanelCard
        eyebrow="Roadmap Bridge"
        title="与项目文档的衔接"
      >
        <div class="flow-rail">
          <div class="flow-rail__item">
            <span class="flow-rail__index">T1</span>
            <div>
              <strong>Telemetry</strong>
            </div>
          </div>
          <div class="flow-rail__item">
            <span class="flow-rail__index">T2</span>
            <div>
              <strong>Gateway</strong>
            </div>
          </div>
          <div class="flow-rail__item">
            <span class="flow-rail__index">T3</span>
            <div>
              <strong>Rule / Alarm / OTA</strong>
            </div>
          </div>
        </div>
      </PanelCard>
    </section>
  </div>
</template>

<script setup lang="ts">
import PanelCard from '../components/PanelCard.vue';

const futureCards = [
  {
    title: '点位图表中枢',
    description: '为温湿度、能耗、震动、状态点位构建同屏趋势、阈值和对比曲线。',
    contracts: [
      { index: '01', title: '属性快照', description: '复用当前 `GET /api/device/{deviceCode}/properties` 做首屏摘要。' },
      { index: '02', title: '时序查询', description: '后续接 telemetry 模块，补足历史趋势和时间范围检索。' },
      { index: '03', title: '异常着色', description: '按 valueType、阈值和告警等级统一上色。' }
    ]
  },
  {
    title: '数字孪生画布',
    description: '让设备拥有位置、状态、动画和事件轨迹，形成更直观的运维视图。',
    contracts: [
      { index: '01', title: '设备坐标', description: '基于 device 的经纬度和地址扩展孪生位姿。' },
      { index: '02', title: '状态光效', description: '在线、告警、离线状态映射不同视觉反馈。' },
      { index: '03', title: '消息回放', description: '消息日志可以驱动事件时间线和轨迹重演。' }
    ]
  },
  {
    title: '网关与协议拓扑',
    description: '把协议适配、上下行链路、网关与子设备关系可视化。',
    contracts: [
      { index: '01', title: '协议维度', description: '按 mqtt-json、modbus、tcp-hex 等协议归类设备。' },
      { index: '02', title: '拓扑维度', description: '后续接 gateway 模块展示网关和子设备树。' },
      { index: '03', title: '链路追踪', description: '结合 message log 做报文路径回溯。' }
    ]
  }
];
</script>

<style scoped>
.phase-ideas {
  margin: 0;
  padding-left: 1.1rem;
  line-height: 1.9;
}
</style>
