import { request } from './request'
import type {
  ApiEnvelope,
  NormativeMetricDefinitionImportPayload,
  NormativeMetricDefinitionImportResult
} from '@/types/api'

export function previewNormativeMetricImport(
  payload: NormativeMetricDefinitionImportPayload
): Promise<ApiEnvelope<NormativeMetricDefinitionImportResult>> {
  return request<NormativeMetricDefinitionImportResult>('/api/device/normative-metrics/import/preview', {
    method: 'POST',
    body: payload
  })
}

export function applyNormativeMetricImport(
  payload: NormativeMetricDefinitionImportPayload
): Promise<ApiEnvelope<NormativeMetricDefinitionImportResult>> {
  return request<NormativeMetricDefinitionImportResult>('/api/device/normative-metrics/import/apply', {
    method: 'POST',
    body: payload
  })
}
