<template>
  <div class="report-analysis-view">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>分析报表</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="handleDateChange"
          />
        </div>
      </template>

      <!-- KPI 指标卡片 -->
      <el-row :gutter="20" class="kpi-row">
        <el-col :span="6">
          <el-card shadow="never" class="kpi-card">
            <div class="kpi-content">
              <div class="kpi-value">{{ alarmStatistics?.total || 0 }}</div>
              <div class="kpi-label">告警总数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="kpi-card">
            <div class="kpi-content">
              <div class="kpi-value">{{ eventStatistics?.total || 0 }}</div>
              <div class="kpi-label">事件总数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="kpi-card">
            <div class="kpi-content">
              <div class="kpi-value">{{ eventStatistics?.closed || 0 }}</div>
              <div class="kpi-label">已关闭事件</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="kpi-card">
            <div class="kpi-content">
              <div class="kpi-value">{{ deviceHealthStatistics?.onlineRate || 0 }}%</div>
              <div class="kpi-label">设备在线率</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 风险趋势分析 -->
      <div class="section-title">风险趋势分析</div>
      <el-skeleton v-if="loading" :rows="8" />
      <div v-else-if="riskTrendData.length > 0" class="chart-container">
        <div ref="riskTrendChart" class="echart-container"></div>
        <div v-if="!chartVisible.riskTrend" class="chart-lazy-placeholder" aria-live="polite">
          <p>滚动到该区域后自动加载图表</p>
          <button type="button" @click="loadChartNow('riskTrend')">立即加载</button>
        </div>
      </div>
      <el-empty v-else description="暂无风险趋势数据" />

      <!-- 告警等级分布 -->
      <div class="section-title">告警等级分布</div>
      <el-skeleton v-if="loading" :rows="8" />
      <div v-else-if="alarmStatistics" class="chart-container">
        <div ref="alarmLevelChart" class="echart-container"></div>
        <div v-if="!chartVisible.alarmLevel" class="chart-lazy-placeholder" aria-live="polite">
          <p>滚动到该区域后自动加载图表</p>
          <button type="button" @click="loadChartNow('alarmLevel')">立即加载</button>
        </div>
      </div>
      <el-empty v-else description="暂无告警等级数据" />

      <!-- 事件闭环分析 -->
      <div class="section-title">事件闭环分析</div>
      <el-skeleton v-if="loading" :rows="8" />
      <div v-else-if="eventStatistics" class="chart-container">
        <div ref="eventClosureChart" class="echart-container"></div>
        <div v-if="!chartVisible.eventClosure" class="chart-lazy-placeholder" aria-live="polite">
          <p>滚动到该区域后自动加载图表</p>
          <button type="button" @click="loadChartNow('eventClosure')">立即加载</button>
        </div>
      </div>
      <el-empty v-else description="暂无事件闭环数据" />

      <!-- 设备健康分析 -->
      <div class="section-title">设备健康分析</div>
      <el-skeleton v-if="loading" :rows="8" />
      <div v-else-if="deviceHealthStatistics" class="chart-container">
        <div ref="deviceHealthChart" class="echart-container"></div>
        <div v-if="!chartVisible.deviceHealth" class="chart-lazy-placeholder" aria-live="polite">
          <p>滚动到该区域后自动加载图表</p>
          <button type="button" @click="loadChartNow('deviceHealth')">立即加载</button>
        </div>
      </div>
      <el-empty v-else description="暂无设备健康数据" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onBeforeUnmount, onMounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts/core'
import type { ECharts } from 'echarts/core'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { ElMessage } from 'element-plus'
import {
  getRiskTrendAnalysis,
  getAlarmStatistics,
  getEventClosureAnalysis,
  getDeviceHealthAnalysis
} from '@/api/report'

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
    ElMessage.error('获取数据失败')
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
  padding: 20px;

  .box-card {
    max-width: 1400px;
    margin: 0 auto;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .el-date-editor {
        width: 300px;
      }
    }
  }

  .kpi-row {
    margin-bottom: 20px;

    .kpi-card {
      .kpi-content {
        text-align: center;

        .kpi-value {
          font-size: 24px;
          font-weight: bold;
          color: #303133;
        }

        .kpi-label {
          font-size: 14px;
          color: #606266;
          margin-top: 8px;
        }
      }
    }
  }

  .section-title {
    font-size: 16px;
    font-weight: bold;
    margin: 20px 0 10px;
    color: #303133;
  }

  .chart-container {
    position: relative;
    height: 400px;
    margin-bottom: 20px;

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
      border-radius: 8px;
      background: linear-gradient(180deg, rgba(247, 251, 255, 0.72), rgba(241, 247, 255, 0.62));

      p {
        margin: 0;
      }

      button {
        min-height: 32px;
        padding: 0 14px;
        border-radius: 6px;
        border: 1px solid #bed1ef;
        background: #fff;
        color: #1668dc;
        font-size: 13px;
        cursor: pointer;
      }

      button:hover {
        border-color: #9dc0ef;
        background: #f3f8ff;
      }
    }
  }
}
</style>
