import { reactive } from 'vue'
import type { PageResult } from '@/types/api'

export interface ServerPaginationState {
  pageNum: number
  pageSize: number
  total: number
}

export function useServerPagination(initialPageSize = 10) {
  const pagination = reactive<ServerPaginationState>({
    pageNum: 1,
    pageSize: initialPageSize,
    total: 0
  })

  const applyPageResult = <T>(pageResult?: PageResult<T> | null): T[] => {
    pagination.total = Number(pageResult?.total || 0)
    pagination.pageNum = Number(pageResult?.pageNum || pagination.pageNum)
    pagination.pageSize = Number(pageResult?.pageSize || pagination.pageSize)
    return pageResult?.records || []
  }

  const setTotal = (total: number) => {
    pagination.total = Number.isFinite(total) ? Math.max(0, total) : 0
  }

  const applyLocalRecords = <T>(records?: T[] | null): T[] => {
    const source = records || []
    setTotal(source.length)
    if (pagination.total <= 0) {
      pagination.pageNum = 1
      return []
    }
    const maxPage = Math.max(1, Math.ceil(pagination.total / pagination.pageSize))
    if (pagination.pageNum > maxPage) {
      pagination.pageNum = maxPage
    }
    const start = (pagination.pageNum - 1) * pagination.pageSize
    return source.slice(start, start + pagination.pageSize)
  }

  const resetPage = () => {
    pagination.pageNum = 1
  }

  const setPageSize = (size: number) => {
    pagination.pageSize = size
    pagination.pageNum = 1
  }

  const setPageNum = (page: number) => {
    pagination.pageNum = page
  }

  const resetTotal = () => {
    pagination.total = 0
  }

  return {
    pagination,
    applyPageResult,
    applyLocalRecords,
    resetPage,
    setPageSize,
    setPageNum,
    setTotal,
    resetTotal
  }
}
