<template>
  <div class="report-analysis-view">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>分析报表</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="?
            start-placeholder="开始日?
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
              <div class="kpi-label">已关闭事?/div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="kpi-card">
            <div class="kpi-content">
              <div class="kpi-value">{{ deviceHealthStatistics?.onlineRate || 0 }}%</div>
              <div class="kpi-label">设备在线?/div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 风险趋势分析 -->
      <div class="section-title">风险趋势分析</div>
      <el-skeleton v-if="loading" :rows="8" />
      <div v-else-if="riskTrendData.length > 0" class="chart-container">
        <div ref="riskTrendChart" class="echart-container"></div>
      </div>
      <el-empty v-else description="暂无风险趋势数据" />

      <!-- 告警等级分布 -->
      <div class="section-title">告警等级分布</div>
      <el-skeleton v-if="loading" :rows="8" />
      <div v-else-if="alarmStatistics" class="chart-container">
        <div ref="alarmLevelChart" class="echart-container"></div>
      </div>
      <el-empty v-else description="暂无告警等级数据" />

      <!-- 事件闭环分析 -->
      <div class="section-title">事件闭环分析</div>
      <el-skeleton v-if="loading" :rows="8" />
      <div v-else-if="eventStatistics" class="chart-container">
        <div ref="eventClosureChart" class="echart-container"></div>
      </div>
      <el-empty v-else description="暂无事件闭环数据" />

      <!-- 设备健康分析 -->
      <div class="section-title">设备健康分析</div>
      <el-skeleton v-if="loading" :rows="8" />
      <div v-else-if="deviceHealthStatistics" class="chart-container">
        <div ref="deviceHealthChart" class="echart-container"></div>
      </div>
      <el-empty v-else description="暂无设备健康数据" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { ElMessage } from '@/utils/message'
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

// 状?
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

// 初始化风险趋势图?
const initRiskTrendChart = () => {
  if (!riskTrendChart.value) return
  const chart = echarts.init(riskTrendChart.value)
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

// 初始化告警等级分布图?
const initAlarmLevelChart = () => {
  if (!alarmLevelChart.value) return
  const chart = echarts.init(alarmLevelChart.value)
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
          { value: alarmStatistics.value.medium || 0, name: '一? },
          { value: alarmStatistics.value.low || 0, name: '轻微' }
        ]
      }
    ]
  }
  chart.setOption(option)
}

// 初始化事件闭环分析图?
const initEventClosureChart = () => {
  if (!eventClosureChart.value) return
  const chart = echarts.init(eventClosureChart.value)
  const option = {
    title: {
      text: '事件闭环分析',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['已关?, '未关?],
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
        name: '已关?,
        type: 'bar',
        data: [eventStatistics.value.closed || 0]
      },
      {
        name: '未关?,
        type: 'bar',
        data: [eventStatistics.value.unclosed || 0]
      }
    ]
  }
  chart.setOption(option)
}

// 初始化设备健康分析图?
const initDeviceHealthChart = () => {
  if (!deviceHealthChart.value) return
  const chart = echarts.init(deviceHealthChart.value)
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
        name: '设备健康?,
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

// 监听数据变化更新图表
watch([riskTrendData, alarmStatistics, eventStatistics, deviceHealthStatistics], () => {
  if (riskTrendChart.value && alarmLevelChart.value && eventClosureChart.value && deviceHealthChart.value) {
    nextTick(() => {
      initRiskTrendChart()
      initAlarmLevelChart()
      initEventClosureChart()
      initDeviceHealthChart()
    })
  }
})

// 组件挂载
onMounted(() => {
  fetchData()
  nextTick(() => {
    if (riskTrendChart.value) initRiskTrendChart()
    if (alarmLevelChart.value) initAlarmLevelChart()
    if (eventClosureChart.value) initEventClosureChart()
    if (deviceHealthChart.value) initDeviceHealthChart()
  })
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
    height: 400px;
    margin-bottom: 20px;

    .echart-container {
      width: 100%;
      height: 100%;
    }
  }
}
</style>

