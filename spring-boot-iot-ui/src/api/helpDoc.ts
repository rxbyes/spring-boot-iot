import { request, type RequestOptions } from './request'
import { getDictByCode, type DictItem } from './dict'
import type { ApiEnvelope, IdType } from '@/types/api'
import type { PageResult } from '@/types/api'

export type HelpDocCategory = 'business' | 'technical' | 'faq'

export interface HelpDocCategoryOption {
  label: string
  value: HelpDocCategory
  sortNo: number
}

export const HELP_DOC_CATEGORY_OPTIONS: HelpDocCategoryOption[] = [
  { value: 'business', label: '业务类', sortNo: 1 },
  { value: 'technical', label: '技术类', sortNo: 2 },
  { value: 'faq', label: '常见问题', sortNo: 3 }
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

type HelpDocRequestOptions = Pick<RequestOptions, 'signal'>

function cloneHelpDocCategoryOptions(): HelpDocCategoryOption[] {
  return HELP_DOC_CATEGORY_OPTIONS.map((item) => ({ ...item }))
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

export function buildHelpDocCategoryOptions(items: Partial<DictItem>[] = []): HelpDocCategoryOption[] {
  const normalized = items
    .filter((item) => item && item.status !== 0)
    .map((item, index) => ({
      label: item.itemName || String(item.itemValue || ''),
      value: String(item.itemValue || '').trim().toLowerCase() as HelpDocCategory,
      sortNo: Number(item.sortNo ?? index)
    }))
    .filter((item) => Boolean(item.value))

  const unique = new Map<HelpDocCategory, HelpDocCategoryOption>()
  normalized
    .sort((left, right) => left.sortNo - right.sortNo)
    .forEach((item) => {
      if (!unique.has(item.value)) {
        unique.set(item.value, item)
      }
    })

  return unique.size > 0 ? Array.from(unique.values()) : cloneHelpDocCategoryOptions()
}

export async function fetchHelpDocCategoryOptions(): Promise<HelpDocCategoryOption[]> {
  try {
    const response = await getDictByCode('help_doc_category')
    return response.code === 200
      ? buildHelpDocCategoryOptions(response.data?.items || [])
      : cloneHelpDocCategoryOptions()
  } catch {
    return cloneHelpDocCategoryOptions()
  }
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

export function listAccessibleHelpDocuments(
  params: HelpDocumentAccessQuery = {},
  options: HelpDocRequestOptions = {}
): Promise<ApiEnvelope<HelpDocumentAccessRecord[]>> {
  const query = buildQuery(params)
  return request<HelpDocumentAccessRecord[]>(`/api/system/help-doc/access/list${query ? `?${query}` : ''}`, {
    method: 'GET',
    ...options
  })
}

export function pageAccessibleHelpDocuments(
  params: HelpDocumentAccessQuery & { pageNum?: number; pageSize?: number } = {},
  options: HelpDocRequestOptions = {}
): Promise<ApiEnvelope<PageResult<HelpDocumentAccessRecord>>> {
  const query = buildQuery(params)
  return request<PageResult<HelpDocumentAccessRecord>>(`/api/system/help-doc/access/page${query ? `?${query}` : ''}`, {
    method: 'GET',
    ...options
  })
}

export function getAccessibleHelpDocument(
  id: IdType,
  currentPath?: string,
  options: HelpDocRequestOptions = {}
): Promise<ApiEnvelope<HelpDocumentAccessRecord>> {
  const query = buildQuery({ currentPath })
  return request<HelpDocumentAccessRecord>(`/api/system/help-doc/access/${id}${query ? `?${query}` : ''}`, {
    method: 'GET',
    ...options
  })
}
