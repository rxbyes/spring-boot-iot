import { request } from './request'
import type { ApiEnvelope, IdType } from '@/types/api'
import type { PageResult } from '@/types/api'

export type HelpDocCategory = 'business' | 'technical' | 'faq'

export const HELP_DOC_CATEGORY_OPTIONS = [
  { value: 'business' as const, label: '业务类' },
  { value: 'technical' as const, label: '技术类' },
  { value: 'faq' as const, label: '常见问题' }
]

export interface HelpDocumentRecord {
  id?: IdType
  tenantId?: IdType
  docCategory: HelpDocCategory
  title: string
  summary?: string | null
  content?: string | null
  keywords?: string | null
  relatedPaths?: string | null
  visibleRoleCodes?: string | null
  status: number
  sortNo?: number | null
  createBy?: IdType
  createTime?: string | null
  updateBy?: IdType
  updateTime?: string | null
}

export interface HelpDocumentAccessRecord {
  id: IdType
  docCategory: HelpDocCategory
  sortNo?: number | null
  title: string
  summary?: string | null
  content?: string | null
  keywords?: string | null
  relatedPaths?: string | null
  currentPathMatched?: boolean
  keywordList?: string[] | null
  relatedPathList?: string[] | null
}

export interface HelpDocumentAccessQuery {
  docCategory?: HelpDocCategory
  keyword?: string
  currentPath?: string
  limit?: number
}

export interface HelpDocumentPageQuery {
  title?: string
  docCategory?: HelpDocCategory
  status?: number
  pageNum?: number
  pageSize?: number
}

function buildQuery(params: Record<string, unknown>) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.append(key, String(value))
    }
  })
  return query.toString()
}

export function pageHelpDocuments(params: HelpDocumentPageQuery = {}): Promise<ApiEnvelope<PageResult<HelpDocumentRecord>>> {
  const query = buildQuery(params)
  return request<PageResult<HelpDocumentRecord>>(`/api/system/help-doc/page${query ? `?${query}` : ''}`, {
    method: 'GET'
  })
}

export function getHelpDocument(id: IdType): Promise<ApiEnvelope<HelpDocumentRecord>> {
  return request<HelpDocumentRecord>(`/api/system/help-doc/${id}`, {
    method: 'GET'
  })
}

export function addHelpDocument(data: Partial<HelpDocumentRecord>): Promise<ApiEnvelope<HelpDocumentRecord>> {
  return request<HelpDocumentRecord>('/api/system/help-doc/add', {
    method: 'POST',
    body: data
  })
}

export function updateHelpDocument(data: Partial<HelpDocumentRecord>): Promise<ApiEnvelope<void>> {
  return request<void>('/api/system/help-doc/update', {
    method: 'PUT',
    body: data
  })
}

export function deleteHelpDocument(id: IdType): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/system/help-doc/delete/${id}`, {
    method: 'DELETE'
  })
}

export function listAccessibleHelpDocuments(params: HelpDocumentAccessQuery = {}): Promise<ApiEnvelope<HelpDocumentAccessRecord[]>> {
  const query = buildQuery(params)
  return request<HelpDocumentAccessRecord[]>(`/api/system/help-doc/access/list${query ? `?${query}` : ''}`, {
    method: 'GET'
  })
}

export function getAccessibleHelpDocument(id: IdType, currentPath?: string): Promise<ApiEnvelope<HelpDocumentAccessRecord>> {
  const query = buildQuery({ currentPath })
  return request<HelpDocumentAccessRecord>(`/api/system/help-doc/access/${id}${query ? `?${query}` : ''}`, {
    method: 'GET'
  })
}
