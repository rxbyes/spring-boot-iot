import { request } from './request';
import type { ApiEnvelope, IdType } from '../types/api';

export type InsightRangeCode = '1d' | '7d' | '30d' | '365d';

export interface TelemetryHistoryBatchRequest {
  deviceId: IdType;
  identifiers: string[];
  rangeCode: InsightRangeCode;
  fillPolicy: 'ZERO';
}

export interface TelemetryHistoryBucketPoint {
  time: string;
  value: number;
  filled: boolean;
}

export interface TelemetryHistoryBatchSeries {
  identifier: string;
  displayName?: string | null;
  seriesType?: 'measure' | 'status' | 'event' | string | null;
  buckets: TelemetryHistoryBucketPoint[];
}

export interface TelemetryHistoryBatchResponse {
  deviceId: IdType;
  rangeCode?: string | null;
  bucket?: string | null;
  points: TelemetryHistoryBatchSeries[];
}

export function getTelemetryHistoryBatch(
  payload: TelemetryHistoryBatchRequest
): Promise<ApiEnvelope<TelemetryHistoryBatchResponse>> {
  return request<TelemetryHistoryBatchResponse>('/api/telemetry/history/batch', {
    method: 'POST',
    body: payload
  });
}
