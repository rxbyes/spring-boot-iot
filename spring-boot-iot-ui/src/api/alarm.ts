import { request } from './request';
import { buildQueryString } from './query';
import type { ApiEnvelope, IdType } from '../types/api';

/**
 * 告警中心 API
 */

// 告警记录接口定义
export interface AlarmRecord {
      id: string;
      alarmCode: string;
      alarmTitle: string;
      alarmType: string;
      alarmLevel: string;
      regionId: string;
      regionName: string;
      riskPointId: string;
      riskPointName: string;
      deviceId: string;
      deviceCode: string;
      deviceName: string;
      metricName: string;
      currentValue: string;
      thresholdValue: string;
      status: number; // 0-未确认 1-已确认 2-已抑制 3-已关闭
      triggerTime: string;
      confirmTime: string;
      confirmUser: string;
      suppressTime: string;
      suppressUser: string;
      closeTime: string;
      closeUser: string;
      ruleId: string;
      ruleName: string;
      tenantId: string;
      remark: string;
      createBy: string;
      createTime: string;
      updateBy: string;
      updateTime: string;
      deleted: number;
}

// 事件记录接口定义
export interface EventRecord {
      id: string;
      eventCode: string;
      eventTitle: string;
      alarmId: string;
      alarmCode: string;
      alarmLevel: string;
      riskLevel: string;
      regionId: string;
      regionName: string;
      riskPointId: string;
      riskPointName: string;
      deviceId: string;
      deviceCode: string;
      deviceName: string;
      metricName: string;
      currentValue: string;
      status: number; // 0-待派发 1-已派发 2-处理中 3-待验收 4-已关闭 5-已取消
      responsibleUser: string;
      urgencyLevel: string;
      arrivalTimeLimit: number;
      completionTimeLimit: number;
      triggerTime: string;
      dispatchTime: string;
      dispatchUser: string;
      startTime: string;
      completeTime: string;
      closeTime: string;
      closeUser: string;
      closeReason: string;
      reviewNotes: string;
      tenantId: string;
      remark: string;
      createBy: string;
      createTime: string;
      updateBy: string;
      updateTime: string;
      deleted: number;
}

// 工单记录接口定义
export interface EventWorkOrder {
      id: string;
      eventId: string;
      eventCode: string;
      workOrderCode: string;
      workOrderType: string;
      assignUser: string;
      receiveUser: string;
      receiveTime: string;
      startTime: string;
      completeTime: string;
      status: number; // 0-待接收 1-已接收 2-处理中 3-已完成 4-已取消
      feedback: string;
      photos: string;
      tenantId: string;
      remark: string;
      createBy: string;
      createTime: string;
      updateBy: string;
      updateTime: string;
      deleted: number;
}

// 获取告警列表
export const getAlarmList = (params?: {
      deviceCode?: string;
      status?: number;
      alarmLevel?: string;
}): Promise<ApiEnvelope<AlarmRecord[]>> => {
      const queryString = buildQueryString(params);
      const path = queryString ? `/api/alarm/list?${queryString}` : '/api/alarm/list';
      return request<AlarmRecord[]>(path, { method: 'GET' });
};

// 获取告警详情
export const getAlarmDetail = (id: string): Promise<ApiEnvelope<AlarmRecord>> => {
      return request<AlarmRecord>(`/api/alarm/${id}`, { method: 'GET' });
};

// 确认告警
export const confirmAlarm = (id: string, confirmUser: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/alarm/${id}/confirm?confirmUser=${confirmUser}`, { method: 'POST' });
};

// 抑制告警
export const suppressAlarm = (id: string, suppressUser: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/alarm/${id}/suppress?suppressUser=${suppressUser}`, { method: 'POST' });
};

// 关闭告警
export const closeAlarm = (id: string, closeUser: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/alarm/${id}/close?closeUser=${closeUser}`, { method: 'POST' });
};

// 获取事件列表
export const getEventList = (params?: {
      deviceCode?: string;
      status?: number;
      riskLevel?: string;
}): Promise<ApiEnvelope<EventRecord[]>> => {
      const queryString = buildQueryString(params);
      const path = queryString ? `/api/event/list?${queryString}` : '/api/event/list';
      return request<EventRecord[]>(path, { method: 'GET' });
};

// 获取事件详情
export const getEventDetail = (id: string): Promise<ApiEnvelope<EventRecord>> => {
      return request<EventRecord>(`/api/event/${id}`, { method: 'GET' });
};

// 工单派发
export const dispatchEvent = (id: string, dispatchUser: number, receiveUser: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/event/${id}/dispatch?dispatchUser=${dispatchUser}&receiveUser=${receiveUser}`, { method: 'POST' });
};

// 事件关闭
export const closeEvent = (id: string, closeUser: number, closeReason: string): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/event/${id}/close?closeUser=${closeUser}&closeReason=${encodeURIComponent(closeReason)}`, { method: 'POST' });
};

// 更新现场反馈
export const updateFeedback = (eventId: string, feedback: string): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/event/${eventId}/feedback?feedback=${encodeURIComponent(feedback)}`, { method: 'POST' });
};

// 获取工单列表
export const getWorkOrderList = (params?: {
      receiveUser?: number;
      status?: number;
}): Promise<ApiEnvelope<EventWorkOrder[]>> => {
      const queryString = buildQueryString(params);
      const path = queryString ? `/api/event/work-orders?${queryString}` : '/api/event/work-orders';
      return request<EventWorkOrder[]>(path, { method: 'GET' });
};

// 接收工单
export const receiveWorkOrder = (id: string, receiveUser: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/event/work-orders/${id}/receive?receiveUser=${receiveUser}`, { method: 'POST' });
};

// 开始处理
export const startProcessing = (id: string, receiveUser: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/event/work-orders/${id}/start?receiveUser=${receiveUser}`, { method: 'POST' });
};

// 完成处理
export const completeProcessing = (id: string, feedback: string, photos?: string): Promise<ApiEnvelope<void>> => {
      const queryString = buildQueryString({ feedback, photos });
      const path = queryString ? `/api/event/work-orders/${id}/complete?${queryString}` : `/api/event/work-orders/${id}/complete`;
      return request<void>(path, { method: 'POST' });
};
