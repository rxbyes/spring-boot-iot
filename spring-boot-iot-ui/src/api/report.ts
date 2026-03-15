import { request } from './request'

// 获取风险趋势分析数据
export function getRiskTrendAnalysis(startDate?: string, endDate?: string) {
      const params = new URLSearchParams()
      if (startDate) params.append('startDate', startDate)
      if (endDate) params.append('endDate', endDate)
      return request(`/api/report/risk-trend?${params.toString()}`, { method: 'GET' })
}

// 获取告警统计分析数据
export function getAlarmStatistics(startDate?: string, endDate?: string) {
      const params = new URLSearchParams()
      if (startDate) params.append('startDate', startDate)
      if (endDate) params.append('endDate', endDate)
      return request(`/api/report/alarm-statistics?${params.toString()}`, { method: 'GET' })
}

// 获取事件闭环分析数据
export function getEventClosureAnalysis(startDate?: string, endDate?: string) {
      const params = new URLSearchParams()
      if (startDate) params.append('startDate', startDate)
      if (endDate) params.append('endDate', endDate)
      return request(`/api/report/event-closure?${params.toString()}`, { method: 'GET' })
}

// 获取设备健康分析数据
export function getDeviceHealthAnalysis() {
      return request('/api/report/device-health', { method: 'GET' })
}
