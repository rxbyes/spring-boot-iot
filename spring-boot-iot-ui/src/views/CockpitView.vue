<template>
  <div class="cockpit-page">
    <!-- 顶部导航栏 -->
    <div class="cockpit-header">
      <div class="header-left">
        <h1 class="page-title">风险监测驾驶舱</h1>
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

    <!-- 中央可视化区域 -->
    <div class="main-visualization">
      <div class="risk-trend-chart">
        <h3 class="chart-title">风险趋势（近 24 小时）</h3>
        <div ref="trendChartRef" class="chart-container"></div>
      </div>
      <div class="risk-distribution">
        <h3 class="chart-title">风险构成</h3>
        <div ref="distributionChartRef" class="chart-container"></div>
      </div>
    </div>

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

    <!-- 预警处置看板 -->
    <div class="early-warning-board">
      <div class="ew-header">
        <h3 class="section-title">预警处置进度</h3>
        <el-button type="primary" size="small">查看全部</el-button>
      </div>
      <div class="ew-grid">
        <div v-for="status in warningStatuses" :key="status.type" class="ew-card">
          <div class="ew-card__header">
            <span class="ew-card__label">{{ status.label }}</span>
            <el-icon :name="status.icon" :color="status.color" />
          </div>
          <div class="ew-card__value">{{ status.count }}</div>
          <div class="ew-card__progress">
            <el-progress
              :percentage="status.percentage"
              :color="status.color"
              :stroke-width="6"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 底部信息 -->
    <div class="cockpit-footer">
      <div class="footer-section">
        <div class="capability-tags">
          <span v-for="cap in capabilities" :key="cap" class="cap-tag">{{ cap }}</span>
        </div>
      </div>
      <div class="footer-section">
        <div class="activity-list">
          <div v-for="activity in recentActivities" :key="activity.id" class="activity-item">
            <span class="activity-time">{{ activity.time }}</span>
            <span class="activity-desc">{{ activity.desc }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import * as echarts from 'echarts/core';
import type { ECharts } from 'echarts/core';
import { LineChart, PieChart } from 'echarts/charts';
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { ElMessage } from 'element-plus';
import { ArrowRight } from '@element-plus/icons-vue';
import MetricCard from '../components/MetricCard.vue';
import { recordActivity } from '../stores/activity';
import { getCockpitData, getRiskTrendData, getRiskDistributionData, getWarningStatusData, getRecentActivities } from '@/api/cockpit';
import { usePermissionStore } from '../stores/permission';

echarts.use([LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer]);

const router = useRouter();
const permissionStore = usePermissionStore();

// 角色切换
const currentRole = ref<'field' | 'ops' | 'manager'>('field');

// 加载状态
const loading = ref(false);
const errorMessage = ref('');

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

// 数据刷新函数
const refreshData = async () => {
  loading.value = true;
  errorMessage.value = '';
  
  try {
    // 这里可以添加 API 调用
    // const data = await getCockpitData();
    // 更新图表数据
    if (trendChart) {
      trendChart.setOption({
        series: [
          {
            name: '红色风险',
            data: [3, 5, 8, 6, 4, 7, 3]
          },
          {
            name: '橙色风险',
            data: [8, 12, 15, 11, 9, 13, 8]
          }
        ]
      });
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '数据加载失败';
    ElMessage.error(errorMessage.value);
  } finally {
    loading.value = false;
  }
};

// 角色指标
const roleMetrics = {
  field: [
    { label: '红色风险', value: '3', hint: '红色风险需立即处置', badge: { label: '紧急', tone: 'danger' as const } },
    { label: '橙色风险', value: '8', hint: '橙色风险需重点跟踪', badge: { label: '关注', tone: 'warning' as const } },
    { label: '今日上报', value: '15', hint: '今日已上报风险数量', badge: { label: '进行中', tone: 'brand' as const } },
    { label: '处置完成', value: '22', hint: '累计处置完成数量', badge: { label: '成功率', tone: 'success' as const } }
  ],
  ops: [
    { label: '在线设备', value: '128', hint: '当前在线设备数量', badge: { label: '96%', tone: 'success' as const } },
    { label: '离线告警', value: '4', hint: '需要处理的离线设备', badge: { label: '需处理', tone: 'danger' as const } },
    { label: '阈值异常', value: '11', hint: '阈值异常设备数量', badge: { label: '关注', tone: 'warning' as const } },
    { label: '远程控制', value: '36', hint: '远程控制成功率', badge: { label: '成功率', tone: 'brand' as const } }
  ],
  manager: [
    { label: '风险态势', value: '中', hint: '当前风险态势等级', badge: { label: '稳定', tone: 'success' as const } },
    { label: '预警处置率', value: '94%', hint: '预警处置完成率', badge: { label: '达标', tone: 'brand' as const } },
    { label: '设备在线率', value: '98%', hint: '设备在线率', badge: { label: '优秀', tone: 'success' as const } },
    { label: '报告生成', value: '12', hint: '今日生成报告数量', badge: { label: '今日', tone: 'brand' as const } }
  ]
};

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

// 预警处置状态
const warningStatuses = [
  { type: 'pending', label: '待处置', count: 15, percentage: 30, color: '#ff6d6d', icon: 'Warning' },
  { type: 'processing', label: '处置中', count: 8, percentage: 55, color: '#ffb347', icon: 'Loading' },
  { type: 'completed', label: '已处置', count: 22, percentage: 100, color: '#58d377', icon: 'Check' },
  { type: 'timeout', label: '超时预警', count: 3, percentage: 15, color: '#ff854d', icon: 'Timer' }
];

// 平台能力标签
const capabilities = ['风险监测', '协议解析', '设备管理', '远程控制', 'AI研判', '报告生成'];

// 最近活动
const recentActivities = [
  { id: 1, time: '10:23', desc: '设备#1024 上报温度异常' },
  { id: 2, time: '09:45', desc: '生成风险分析报告#032' },
  { id: 3, time: '09:12', desc: '远程控制设备#089' },
  { id: 4, time: '08:30', desc: '新预警#045 红色等级' }
];

// 图表引用
const trendChartRef = ref<HTMLElement | null>(null);
const distributionChartRef = ref<HTMLElement | null>(null);
let trendChart: ECharts | null = null;
let distributionChart: ECharts | null = null;

// 初始化风险趋势图
const initTrendChart = () => {
  if (!trendChartRef.value) return;
  
  trendChart = echarts.init(trendChartRef.value);
  
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: 'rgba(55, 78, 112, 0.2)',
      borderWidth: 1,
      textStyle: { color: '#1f2a3d' }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00'],
      axisLine: { lineStyle: { color: 'rgba(67, 98, 148, 0.24)' } },
      axisLabel: { color: '#6c7e97' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(67, 98, 148, 0.14)' } },
      axisLine: { lineStyle: { color: 'rgba(67, 98, 148, 0.24)' } },
      axisLabel: { color: '#6c7e97' }
    },
    series: [
      {
        name: '红色风险',
        type: 'line',
        smooth: true,
        data: [3, 5, 8, 6, 4, 7, 3],
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: { color: '#ff6d6d' },
        lineStyle: { width: 2, color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#ff6d6d' }, { offset: 1, color: 'rgba(255, 109, 109, 0.3)' }]) },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 109, 109, 0.3)' },
            { offset: 1, color: 'rgba(255, 109, 109, 0.05)' }
          ])
        }
      },
      {
        name: '橙色风险',
        type: 'line',
        smooth: true,
        data: [8, 12, 15, 11, 9, 13, 8],
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: { color: '#ffb347' },
        lineStyle: { width: 2, color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#ffb347' }, { offset: 1, color: 'rgba(255, 179, 71, 0.3)' }]) },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 179, 71, 0.3)' },
            { offset: 1, color: 'rgba(255, 179, 71, 0.05)' }
          ])
        }
      }
    ]
  };
  
  trendChart.setOption(option);
};

// 初始化风险构成图
const initDistributionChart = () => {
  if (!distributionChartRef.value) return;
  
  distributionChart = echarts.init(distributionChartRef.value);
  
  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: 'rgba(55, 78, 112, 0.2)',
      borderWidth: 1,
      textStyle: { color: '#1f2a3d' }
    },
    legend: {
      orient: 'vertical',
      right: '10%',
      itemGap: 12,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#6c7e97', fontSize: 12 }
    },
    series: [
      {
        name: '风险构成',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: 'rgba(0, 0, 0, 0)',
          borderWidth: 2
        },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#1f2a3d' } },
        data: [
          { value: 35, name: '红色风险', itemStyle: { color: '#ff6d6d' } },
          { value: 28, name: '橙色风险', itemStyle: { color: '#ffb347' } },
          { value: 22, name: '黄色风险', itemStyle: { color: '#ffd666' } },
          { value: 15, name: '蓝色风险', itemStyle: { color: '#1e80ff' } }
        ]
      }
    ]
  };
  
  distributionChart.setOption(option);
};

// 导航
const navigateTo = (path: string) => {
  router.push(path);
};

// 自动刷新定时器
let refreshTimer: number | null = null;

// 生命周期
onMounted(() => {
  initTrendChart();
  initDistributionChart();
  
  recordActivity({
    module: '驾驶舱',
    action: '访问首页',
    request: { path: '/' },
    ok: true,
    detail: '用户访问风险监测驾驶舱首页'
  });
  
  // 启动自动刷新（每 30 秒刷新一次）
  refreshTimer = window.setInterval(() => {
    refreshData();
  }, 30000);
});

onUnmounted(() => {
  if (trendChart) {
    trendChart.dispose();
    trendChart = null;
  }
  if (distributionChart) {
    distributionChart.dispose();
    distributionChart = null;
  }
  
  // 清除自动刷新定时器
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
});
</script>

<style scoped>
.cockpit-page {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

/* 顶部导航栏 */
.cockpit-header {
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

/* 角色切换 */
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

/* 四宫格指标 */
.quad-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

/* 中央可视化区域 */
.main-visualization {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 1rem;
}

.risk-trend-chart,
.risk-distribution {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(165deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.94)),
    radial-gradient(circle at top right, rgba(30, 128, 255, 0.1), transparent 52%);
}

.chart-title {
  margin: 0 0 1rem;
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.chart-container {
  height: 240px;
  width: 100%;
}

/* 角色快捷入口 */
.role-quick-access {
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.97), rgba(247, 251, 255, 0.95)),
    radial-gradient(circle at 85% 20%, rgba(255, 106, 0, 0.12), transparent 30%);
}

.section-title {
  margin: 0 0 1.25rem;
  font-size: 1.1rem;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.access-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
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

/* 预警处置看板 */
.early-warning-board {
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(165deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.94)),
    radial-gradient(circle at top right, rgba(30, 128, 255, 0.1), transparent 52%);
}

.ew-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
}

.ew-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

.ew-card {
  padding: 1.25rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
}

.ew-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.ew-card__label {
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--text-tertiary);
}

.ew-card__value {
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-primary);
}

.ew-card__progress {
  margin-top: 0.75rem;
}

/* 底部信息 */
.cockpit-footer {
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

.capability-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.cap-tag {
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
  border: 1px solid var(--panel-border);
  background: rgba(255, 106, 0, 0.08);
  color: var(--brand-bright);
  font-size: 0.8rem;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.activity-time {
  font-family: var(--font-mono);
  font-size: 0.8rem;
  color: var(--brand-bright);
  min-width: 4rem;
}

.activity-desc {
  font-size: 0.9rem;
  color: var(--text-secondary);
}

/* 响应式 */
@media (max-width: 1400px) {
  .quad-grid,
  .access-grid,
  .ew-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .cockpit-header {
    flex-direction: column;
    gap: 1rem;
  }
  
  .main-visualization {
    grid-template-columns: 1fr;
  }
  
  .quad-grid,
  .access-grid,
  .ew-grid {
    grid-template-columns: 1fr;
  }
  
  .cockpit-footer {
    grid-template-columns: 1fr;
  }
}
</style>

