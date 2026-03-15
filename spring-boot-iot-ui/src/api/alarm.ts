import { request } from './request';
import type { ApiEnvelope } from '../types/api';

/**
 * 告警中心 API
 */

// 告警记录接口定义
export interface AlarmRecord {
      id: number;
      alarmCode: string;
      alarmTitle: string;
      alarmType: string;
      alarmLevel: string;
      regionId: number;
      regionName: string;
      riskPointId: number;
      riskPointName: string;
      deviceId: number;
      deviceCode: string;
      deviceName: string;
      metricName: string;
      currentValue: string;
      thresholdValue: string;
      status: number; // 0-未确认 1-已确认 2-已抑制 3-已关闭
      triggerTime: string;
      confirmTime: string;
      confirmUser: number;
      suppressTime: string;
      suppressUser: number;
      closeTime: string;
      closeUser: number;
      ruleId: number;
      ruleName: string;
      tenantId: number;
      remark: string;
      createBy: number;
      createTime: string;
      updateBy: number;
      updateTime: string;
      deleted: number;
}

// 事件记录接口定义
export interface EventRecord {
      id: number;
      eventCode: string;
      eventTitle: string;
      alarmId: number;
      alarmCode: string;
      alarmLevel: string;
      riskLevel: string;
      regionId: number;
      regionName: string;
      riskPointId: number;
      riskPointName: string;
      deviceId: number;
      deviceCode: string;
      deviceName: string;
      metricName: string;
      currentValue: string;
      status: number; // 0-待派发 1-已派发 2-处理中 3-待验收 4-已关闭 5-已取消
      responsibleUser: number;
      urgencyLevel: string;
      arrivalTimeLimit: number;
      completionTimeLimit: number;
      triggerTime: string;
      dispatchTime: string;
      dispatchUser: number;
      startTime: string;
      completeTime: string;
      closeTime: string;
      closeUser: number;
      closeReason: string;
      reviewNotes: string;
      tenantId: number;
      remark: string;
      createBy: number;
      createTime: string;
      updateBy: number;
      updateTime: string;
      deleted: number;
}

// 工单记录接口定义
export interface EventWorkOrder {
      id: number;
      eventId: number;
      eventCode: string;
      workOrderCode: string;
      workOrderType: string;
      assignUser: number;
      receiveUser: number;
      receiveTime: string;
      startTime: string;
      completeTime: string;
      status: number; // 0-待接收 1-已接收 2-处理中 3-已完成 4-已取消
      feedback: string;
      photos: string;
      tenantId: number;
      remark: string;
      createBy: number;
      createTime: string;
      updateBy: number;
      updateTime: string;
      deleted: number;
}

// 获取告警列表
export const getAlarmList = (params?: {
      deviceCode?: string;
      status?: number;
      alarmLevel?: string;
}): Promise<ApiEnvelope<AlarmRecord[]>> => {
      const queryString = params ? new URLSearchParams(params as any).toString() : '';
      const path = queryString ? `/api/alarm/list?${queryString}` : '/api/alarm/list';
      return request<AlarmRecord[]>(path, { method: 'GET' });
};

// 获取告警详情
export const getAlarmDetail = (id: number): Promise<ApiEnvelope<AlarmRecord>> => {
      return request<AlarmRecord>(`/api/alarm/${id}`, { method: 'GET' });
};

// 确认告警
export const confirmAlarm = (id: number, confirmUser: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/alarm/${id}/confirm`, { method: 'POST', body: { confirmUser } });
};

// 抑制告警
export const suppressAlarm = (id: number, suppressUser: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/alarm/${id}/suppress`, { method: 'POST', body: { suppressUser } });
};

// 关闭告警
export const closeAlarm = (id: number, closeUser: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/alarm/${id}/close`, { method: 'POST', body: { closeUser } });
};

// 获取事件列表
export const getEventList = (params?: {
      deviceCode?: string;
      status?: number;
      riskLevel?: string;
}): Promise<ApiEnvelope<EventRecord[]>> => {
      const queryString = params ? new URLSearchParams(params as any).toString() : '';
      const path = queryString ? `/api/event/list?${queryString}` : '/api/event/list';
      return request<EventRecord[]>(path, { method: 'GET' });
};

// 获取事件详情
export const getEventDetail = (id: number): Promise<ApiEnvelope<EventRecord>> => {
      return request<EventRecord>(`/api/event/${id}`, { method: 'GET' });
};

// 工单派发
export const dispatchEvent = (id: number, dispatchUser: number, receiveUser: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/event/${id}/dispatch?dispatchUser=${dispatchUser}&receiveUser=${receiveUser}`, { method: 'POST' });
};

// 事件关闭
export const closeEvent = (id: number, closeUser: number, closeReason: string): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/event/${id}/close?closeUser=${closeUser}&closeReason=${encodeURIComponent(closeReason)}`, { method: 'POST' });
};

// 更新现场反馈
export const updateFeedback = (eventId: number, feedback: string): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/event/${eventId}/feedback`, { method: 'POST', body: { feedback } });
};

// 获取工单列表
export const getWorkOrderList = (params?: {
      receiveUser?: number;
      status?: number;
}): Promise<ApiEnvelope<EventWorkOrder[]>> => {
      const queryString = params ? new URLSearchParams(params as any).toString() : '';
      const path = queryString ? `/api/event/work-orders?${queryString}` : '/api/event/work-orders';
      return request<EventWorkOrder[]>(path, { method: 'GET' });
};

// 接收工单
export const receiveWorkOrder = (id: number, receiveUser: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/event/work-orders/${id}/receive`, { method: 'POST', body: { receiveUser } });
};

// 开始处理
export const startProcessing = (id: number, receiveUser: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/event/work-orders/${id}/start`, { method: 'POST', body: { receiveUser } });
};

// 完成处理
export const completeProcessing = (id: number, feedback: string, photos?: string): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/event/work-orders/${id}/complete`, { method: 'POST', body: { feedback, photos } });
};
