<template>
  <StandardPageShell class="report-analysis-view">
    <StandardWorkbenchPanel
      title="运营分析中心"
      description="按时间区间查看风险趋势、告警分布、事件闭环与设备健康，统一保持平台治理页头与卡片节奏。"
      show-header-actions
    >
      <template #header-actions>
        <el-date-picker
          v-model="dateRange"
          class="report-analysis-view__date-picker"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="handleDateChange"
        />
      </template>

      <section class="quad-grid report-analysis-kpis">
        <MetricCard
          v-for="metric in overviewMetrics"
          :key="metric.label"
          class="report-analysis-kpis__card"
          :label="metric.label"
          :value="metric.value"
          :badge="metric.badge"
        />
      </section>

      <section class="report-analysis-section">
        <div class="report-analysis-section__header">
          <h3 class="report-analysis-section__title">风险趋势分析</h3>
          <p class="report-analysis-section__description">按时间范围复盘告警与事件数量变化。</p>
        </div>
        <el-skeleton v-if="loading" :rows="8" />
        <div v-else-if="riskTrendData.length > 0" class="chart-container">
          <div ref="riskTrendChart" class="echart-container"></div>
          <div v-if="!chartVisible.riskTrend" class="chart-lazy-placeholder" aria-live="polite">
            <p>滚动到该区域后自动加载图表</p>
            <button type="button" @click="loadChartNow('riskTrend')">立即加载</button>
          </div>
        </div>
        <el-empty v-else description="暂无风险趋势数据" />
      </section>

      <section class="report-analysis-section">
        <div class="report-analysis-section__header">
          <h3 class="report-analysis-section__title">告警等级分布</h3>
          <p class="report-analysis-section__description">查看当前时间区间内的告警等级占比。</p>
        </div>
        <el-skeleton v-if="loading" :rows="8" />
        <div v-else-if="alarmStatistics" class="chart-container">
          <div ref="alarmLevelChart" class="echart-container"></div>
          <div v-if="!chartVisible.alarmLevel" class="chart-lazy-placeholder" aria-live="polite">
            <p>滚动到该区域后自动加载图表</p>
            <button type="button" @click="loadChartNow('alarmLevel')">立即加载</button>
          </div>
        </div>
        <el-empty v-else description="暂无告警等级数据" />
      </section>

      <section class="report-analysis-section">
        <div class="report-analysis-section__header">
          <h3 class="report-analysis-section__title">事件闭环分析</h3>
          <p class="report-analysis-section__description">复盘事件关闭与积压情况，识别闭环效率变化。</p>
        </div>
        <el-skeleton v-if="loading" :rows="8" />
        <div v-else-if="eventStatistics" class="chart-container">
          <div ref="eventClosureChart" class="echart-container"></div>
          <div v-if="!chartVisible.eventClosure" class="chart-lazy-placeholder" aria-live="polite">
            <p>滚动到该区域后自动加载图表</p>
            <button type="button" @click="loadChartNow('eventClosure')">立即加载</button>
          </div>
        </div>
        <el-empty v-else description="暂无事件闭环数据" />
      </section>

      <section class="report-analysis-section">
        <div class="report-analysis-section__header">
          <h3 class="report-analysis-section__title">设备健康分析</h3>
          <p class="report-analysis-section__description">汇总设备健康状态与在线率，辅助运营复盘。</p>
        </div>
        <el-skeleton v-if="loading" :rows="8" />
        <div v-else-if="deviceHealthStatistics" class="chart-container">
          <div ref="deviceHealthChart" class="echart-container"></div>
          <div v-if="!chartVisible.deviceHealth" class="chart-lazy-placeholder" aria-live="polite">
            <p>滚动到该区域后自动加载图表</p>
            <button type="button" @click="loadChartNow('deviceHealth')">立即加载</button>
          </div>
        </div>
        <el-empty v-else description="暂无设备健康数据" />
      </section>
    </StandardWorkbenchPanel>
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onBeforeUnmount, onMounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts/core'
import type { ECharts } from 'echarts/core'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { ElMessage } from 'element-plus'
import { isHandledRequestError, resolveRequestErrorMessage } from '@/api/request'
import {
  getRiskTrendAnalysis,
  getAlarmStatistics,
  getEventClosureAnalysis,
  getDeviceHealthAnalysis
} from '@/api/report'
import MetricCard from '@/components/MetricCard.vue'
import StandardPageShell from '@/components/StandardPageShell.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'

echarts.use([
  LineChart,
  BarChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  CanvasRenderer
])

// 状态
const loading = ref(false)
const dateRange = ref<string[]>([])
const riskTrendData = ref<any[]>([])
const alarmStatistics = ref<any>({})
const eventStatistics = ref<any>({})
const deviceHealthStatistics = ref<any>({})

// 图表引用
const riskTrendChart = ref<HTMLDivElement | null>(null)
const alarmLevelChart = ref<HTMLDivElement | null>(null)
const eventClosureChart = ref<HTMLDivElement | null>(null)
const deviceHealthChart = ref<HTMLDivElement | null>(null)
const riskTrendChartInstance = ref<ECharts | null>(null)
const alarmLevelChartInstance = ref<ECharts | null>(null)
const eventClosureChartInstance = ref<ECharts | null>(null)
const deviceHealthChartInstance = ref<ECharts | null>(null)
const chartVisible = reactive({
  riskTrend: false,
  alarmLevel: false,
  eventClosure: false,
  deviceHealth: false
})
const chartElements = reactive<Record<string, HTMLElement | null>>({
  riskTrend: null,
  alarmLevel: null,
  eventClosure: null,
  deviceHealth: null
})
let visibilityObserver: IntersectionObserver | null = null

const overviewMetrics = computed(() => [
  {
    label: '告警总数',
    value: String(alarmStatistics.value?.total || 0),
    badge: { label: 'Alarm', tone: 'warning' as const }
  },
  {
    label: '事件总数',
    value: String(eventStatistics.value?.total || 0),
    badge: { label: 'Event', tone: 'brand' as const }
  },
  {
    label: '已关闭事件',
    value: String(eventStatistics.value?.closed || 0),
    badge: { label: 'Closed', tone: 'success' as const }
  },
  {
    label: '设备在线率',
    value: `${deviceHealthStatistics.value?.onlineRate || 0}%`,
    badge: { label: 'Device', tone: 'danger' as const }
  }
])

const ensureChart = (container: HTMLDivElement | null, chartRef: { value: ECharts | null }) => {
  if (!container) return null
  if (!chartRef.value) {
    chartRef.value = echarts.init(container)
  }
  return chartRef.value
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const [riskTrendRes, alarmRes, eventRes, deviceRes] = await Promise.all([
      getRiskTrendAnalysis(
        dateRange.value[0],
        dateRange.value[1]
      ),
      getAlarmStatistics(
        dateRange.value[0],
        dateRange.value[1]
      ),
      getEventClosureAnalysis(
        dateRange.value[0],
        dateRange.value[1]
      ),
      getDeviceHealthAnalysis()
    ])

    if (riskTrendRes.code === 200) {
      riskTrendData.value = riskTrendRes.data || []
    }
    if (alarmRes.code === 200) {
      alarmStatistics.value = alarmRes.data || {}
    }
    if (eventRes.code === 200) {
      eventStatistics.value = eventRes.data || {}
    }
    if (deviceRes.code === 200) {
      deviceHealthStatistics.value = deviceRes.data || {}
    }
  } catch (error) {
    if (!isHandledRequestError(error)) {
      ElMessage.error(resolveRequestErrorMessage(error, '获取数据失败'))
    }
  } finally {
    loading.value = false
  }
}

// 日期变化处理
const handleDateChange = () => {
  fetchData()
}

// 初始化风险趋势图表
const initRiskTrendChart = () => {
  const chart = ensureChart(riskTrendChart.value, riskTrendChartInstance)
  if (!chart) return
  const option = {
    title: {
      text: '风险趋势',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['告警数量', '事件数量'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: riskTrendData.value.map((item: any) => item.date)
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '告警数量',
        type: 'line',
        data: riskTrendData.value.map((item: any) => item.alarmCount),
        smooth: true
      },
      {
        name: '事件数量',
        type: 'line',
        data: riskTrendData.value.map((item: any) => item.eventCount),
        smooth: true
      }
    ]
  }
  chart.setOption(option)
}

// 初始化告警等级分布图表
const initAlarmLevelChart = () => {
  const chart = ensureChart(alarmLevelChart.value, alarmLevelChartInstance)
  if (!chart) return
  const option = {
    title: {
      text: '告警等级分布',
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center'
    },
    series: [
      {
        name: '告警等级',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '16',
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: alarmStatistics.value.critical || 0, name: '严重' },
          { value: alarmStatistics.value.high || 0, name: '重要' },
          { value: alarmStatistics.value.medium || 0, name: '一般' },
          { value: alarmStatistics.value.low || 0, name: '轻微' }
        ]
      }
    ]
  }
  chart.setOption(option)
}

// 初始化事件闭环分析图表
const initEventClosureChart = () => {
  const chart = ensureChart(eventClosureChart.value, eventClosureChartInstance)
  if (!chart) return
  const option = {
    title: {
      text: '事件闭环分析',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['已关闭', '未关闭'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['事件数量']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '已关闭',
        type: 'bar',
        data: [eventStatistics.value.closed || 0]
      },
      {
        name: '未关闭',
        type: 'bar',
        data: [eventStatistics.value.unclosed || 0]
      }
    ]
  }
  chart.setOption(option)
}

// 初始化设备健康分析图表
const initDeviceHealthChart = () => {
  const chart = ensureChart(deviceHealthChart.value, deviceHealthChartInstance)
  if (!chart) return
  const option = {
    title: {
      text: '设备健康分析',
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center'
    },
    series: [
      {
        name: '设备健康度',
        type: 'pie',
        radius: ['40%', '70%'],
        data: [
          { value: deviceHealthStatistics.value.healthy || 0, name: '健康' },
          { value: deviceHealthStatistics.value.warning || 0, name: '预警' },
          { value: deviceHealthStatistics.value.critical || 0, name: '故障' }
        ]
      }
    ]
  }
  chart.setOption(option)
}

const refreshVisibleCharts = () => {
  nextTick(() => {
    if (chartVisible.riskTrend && riskTrendChart.value) {
      initRiskTrendChart()
    }
    if (chartVisible.alarmLevel && alarmLevelChart.value) {
      initAlarmLevelChart()
    }
    if (chartVisible.eventClosure && eventClosureChart.value) {
      initEventClosureChart()
    }
    if (chartVisible.deviceHealth && deviceHealthChart.value) {
      initDeviceHealthChart()
    }
  })
}

const loadChartNow = (key: keyof typeof chartVisible) => {
  chartVisible[key] = true
  const element = chartElements[key]
  if (element && visibilityObserver) {
    visibilityObserver.unobserve(element)
  }
  refreshVisibleCharts()
}

const observeChartVisibility = () => {
  chartElements.riskTrend = riskTrendChart.value
  chartElements.alarmLevel = alarmLevelChart.value
  chartElements.eventClosure = eventClosureChart.value
  chartElements.deviceHealth = deviceHealthChart.value

  if (typeof window === 'undefined' || typeof window.IntersectionObserver === 'undefined') {
    chartVisible.riskTrend = true
    chartVisible.alarmLevel = true
    chartVisible.eventClosure = true
    chartVisible.deviceHealth = true
    refreshVisibleCharts()
    return
  }

  if (!visibilityObserver) {
    visibilityObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) {
            return
          }
          const target = entry.target as HTMLElement
          const key = target.dataset.chartKey as keyof typeof chartVisible
          if (!key) {
            return
          }
          chartVisible[key] = true
          visibilityObserver?.unobserve(target)
          refreshVisibleCharts()
        })
      },
      {
        root: null,
        threshold: 0.15,
        rootMargin: '0px 0px 140px 0px'
      }
    )
  }

  ;([
    ['riskTrend', chartElements.riskTrend],
    ['alarmLevel', chartElements.alarmLevel],
    ['eventClosure', chartElements.eventClosure],
    ['deviceHealth', chartElements.deviceHealth]
  ] as const).forEach(([key, element]) => {
    if (!element) {
      return
    }
    if (chartVisible[key]) {
      return
    }
    element.dataset.chartKey = key
    visibilityObserver?.observe(element)
  })
}

watch([riskTrendData, alarmStatistics, eventStatistics, deviceHealthStatistics], () => {
  refreshVisibleCharts()
})

watch([riskTrendChart, alarmLevelChart, eventClosureChart, deviceHealthChart], () => {
  observeChartVisibility()
})

onMounted(() => {
  fetchData()
  nextTick(() => {
    observeChartVisibility()
  })
})

onBeforeUnmount(() => {
  visibilityObserver?.disconnect()
  visibilityObserver = null
  riskTrendChartInstance.value?.dispose()
  alarmLevelChartInstance.value?.dispose()
  eventClosureChartInstance.value?.dispose()
  deviceHealthChartInstance.value?.dispose()
  riskTrendChartInstance.value = null
  alarmLevelChartInstance.value = null
  eventClosureChartInstance.value = null
  deviceHealthChartInstance.value = null
})
</script>

<style scoped lang="scss">
.report-analysis-view {
  min-width: 0;
}

.report-analysis-view__date-picker {
  width: min(100%, 320px);
}

.report-analysis-kpis {
  margin-bottom: 1rem;
}

.report-analysis-kpis__card {
  min-height: 8.5rem;
}

.report-analysis-section + .report-analysis-section {
  margin-top: 1.15rem;
  padding-top: 1.15rem;
  border-top: 1px solid var(--line-soft);
}

.report-analysis-section__header {
  display: grid;
  gap: 0.28rem;
  margin-bottom: 0.78rem;
}

.report-analysis-section__title {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.98rem;
  font-weight: 700;
  line-height: 1.35;
}

.report-analysis-section__description {
  margin: 0;
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.6;
}

.chart-container {
  position: relative;
  height: 400px;
}

.echart-container {
  width: 100%;
  height: 100%;
}

.chart-lazy-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
  justify-content: center;
  color: #6b7d95;
  font-size: 14px;
  border: 1px dashed #d8e4f5;
  border-radius: calc(var(--radius-md) + 2px);
  background: linear-gradient(180deg, rgba(247, 251, 255, 0.72), rgba(241, 247, 255, 0.62));
}

.chart-lazy-placeholder p {
  margin: 0;
}

.chart-lazy-placeholder button {
  min-height: 32px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  border: 1px solid #bed1ef;
  background: #fff;
  color: #1668dc;
  font-size: 13px;
  cursor: pointer;
}

.chart-lazy-placeholder button:hover {
  border-color: #9dc0ef;
  background: #f3f8ff;
}

@media (max-width: 900px) {
  .report-analysis-view__date-picker {
    width: 100%;
  }
}
</style>
