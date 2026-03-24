import { effectScope, reactive } from 'vue'
import { describe, expect, it } from 'vitest'

import { useListAppliedFilters } from '@/composables/useListAppliedFilters'

describe('useListAppliedFilters', () => {
  it('builds applied filter tags and counts advanced filters from shared config', () => {
    const scope = effectScope()

    scope.run(() => {
      const form = reactive({
        keyword: 'trace-001',
        status: 1 as number | undefined,
        topic: '/sys/demo/topic'
      })
      const applied = reactive({
        keyword: '',
        status: undefined as number | undefined,
        topic: ''
      })

      const { tags, hasAppliedFilters, advancedAppliedCount, syncAppliedFilters } = useListAppliedFilters({
        form,
        applied,
        fields: [
          { key: 'keyword', label: '关键字' },
          { key: 'status', label: (value) => `状态：${value === 1 ? '启用' : '停用'}`, clearValue: undefined },
          { key: 'topic', label: 'Topic', advanced: true }
        ],
        defaults: {
          keyword: '',
          status: undefined,
          topic: ''
        }
      })

      syncAppliedFilters()

      expect(tags.value).toEqual([
        { key: 'keyword', label: '关键字：trace-001' },
        { key: 'status', label: '状态：启用' },
        { key: 'topic', label: 'Topic：/sys/demo/topic' }
      ])
      expect(hasAppliedFilters.value).toBe(true)
      expect(advancedAppliedCount.value).toBe(1)
    })

    scope.stop()
  })

  it('removes a single filter from both form and applied state', () => {
    const scope = effectScope()

    scope.run(() => {
      const form = reactive({
        keyword: 'trace-001',
        status: 1 as number | undefined
      })
      const applied = reactive({
        keyword: 'trace-001',
        status: 1 as number | undefined
      })

      const { removeFilter } = useListAppliedFilters({
        form,
        applied,
        fields: [
          { key: 'keyword', label: '关键字' },
          { key: 'status', label: (value) => `状态：${value === 1 ? '启用' : '停用'}`, clearValue: undefined }
        ],
        defaults: {
          keyword: '',
          status: undefined
        }
      })

      removeFilter('status')

      expect(form.status).toBeUndefined()
      expect(applied.status).toBeUndefined()
      expect(form.keyword).toBe('trace-001')
    })

    scope.stop()
  })

  it('clears all filters through the provided reset callback and resyncs applied values', () => {
    const scope = effectScope()

    scope.run(() => {
      const form = reactive({
        keyword: 'trace-001',
        timeRange: ['2026-03-20 00:00:00', '2026-03-22 00:00:00']
      })
      const applied = reactive({
        keyword: 'trace-001',
        timeRange: ['2026-03-20 00:00:00', '2026-03-22 00:00:00']
      })

      const { clearFilters } = useListAppliedFilters({
        form,
        applied,
        fields: [
          { key: 'keyword', label: '关键字' },
          { key: 'timeRange', label: '时间范围', format: (value) => value.join(' 至 '), advanced: true }
        ],
        reset: () => {
          form.keyword = ''
          form.timeRange = []
        }
      })

      clearFilters()

      expect(form.keyword).toBe('')
      expect(applied.keyword).toBe('')
      expect(form.timeRange).toEqual([])
      expect(applied.timeRange).toEqual([])
    })

    scope.stop()
  })
})
