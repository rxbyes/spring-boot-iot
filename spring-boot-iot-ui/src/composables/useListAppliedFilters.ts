import { computed } from 'vue'

export interface ListAppliedFilterTag {
  key: string
  label: string
}

export interface ListAppliedFilterField<T extends Record<string, unknown>, K extends Extract<keyof T, string> = Extract<keyof T, string>> {
  key: K
  label: string | ((value: T[K], state: T) => string)
  format?: (value: T[K], state: T) => string | undefined
  isActive?: (value: T[K], state: T) => boolean
  clearValue?: T[K] | (() => T[K])
  advanced?: boolean
}

interface UseListAppliedFiltersOptions<T extends Record<string, unknown>> {
  form: T
  applied: T
  fields: ListAppliedFilterField<T>[]
  defaults?: Partial<T> | (() => Partial<T>)
  reset?: () => void
}

const cloneValue = <T>(value: T): T => {
  if (Array.isArray(value)) {
    return [...value] as T
  }
  if (value && typeof value === 'object' && !(value instanceof Date)) {
    return { ...(value as Record<string, unknown>) } as T
  }
  return value
}

const hasFilledFilter = (value: unknown): boolean => {
  if (Array.isArray(value)) {
    return value.some((item) => hasFilledFilter(item))
  }
  if (typeof value === 'string') {
    return value.trim() !== ''
  }
  return value !== undefined && value !== null
}

const formatFallbackValue = (value: unknown): string => {
  if (Array.isArray(value)) {
    return value
      .map((item) => String(item).trim())
      .filter(Boolean)
      .join(' / ')
  }
  return String(value ?? '').trim()
}

const resolveValue = <T>(value: T | (() => T)): T => (typeof value === 'function' ? (value as () => T)() : value)

export function useListAppliedFilters<T extends Record<string, unknown>>(options: UseListAppliedFiltersOptions<T>) {
  const fieldMap = new Map(options.fields.map((field) => [String(field.key), field] as const))

  const resolvedDefaults = computed<Partial<T>>(() => {
    if (typeof options.defaults === 'function') {
      return options.defaults()
    }
    return options.defaults || {}
  })

  const isFieldActive = <K extends Extract<keyof T, string>>(field: ListAppliedFilterField<T, K>) => {
    const value = options.applied[field.key]
    if (field.isActive) {
      return field.isActive(value, options.applied)
    }
    return hasFilledFilter(value)
  }

  const tags = computed<ListAppliedFilterTag[]>(() =>
    options.fields.flatMap((field) => {
      if (!isFieldActive(field)) {
        return []
      }

      const value = options.applied[field.key]
      const label = typeof field.label === 'function'
        ? field.label(value, options.applied)
        : `${field.label}：${field.format ? field.format(value, options.applied) : formatFallbackValue(value)}`

      return label.trim() ? [{ key: String(field.key), label: label.trim() }] : []
    })
  )

  const hasAppliedFilters = computed(() => tags.value.length > 0)

  const advancedAppliedCount = computed(() =>
    options.fields.reduce((count, field) => (field.advanced && isFieldActive(field) ? count + 1 : count), 0)
  )

  const resolveClearValue = <K extends Extract<keyof T, string>>(field: ListAppliedFilterField<T, K>): T[K] | undefined => {
    if (field.clearValue !== undefined) {
      return resolveValue(field.clearValue)
    }

    const defaultValue = resolvedDefaults.value[field.key]
    if (defaultValue !== undefined) {
      return cloneValue(defaultValue as T[K])
    }

    const currentValue = options.form[field.key]
    if (Array.isArray(currentValue)) {
      return [] as T[K]
    }
    if (typeof currentValue === 'string') {
      return '' as T[K]
    }
    return undefined
  }

  const setFieldValue = <K extends Extract<keyof T, string>>(key: K, value: T[K] | undefined) => {
    options.form[key] = cloneValue(value) as T[K]
    options.applied[key] = cloneValue(value) as T[K]
  }

  const syncAppliedFilters = (source = options.form) => {
    options.fields.forEach((field) => {
      options.applied[field.key] = cloneValue(source[field.key]) as T[typeof field.key]
    })
  }

  const removeFilter = (key: string) => {
    const field = fieldMap.get(key)
    if (!field) {
      return
    }
    setFieldValue(field.key, resolveClearValue(field))
  }

  const clearFilters = () => {
    if (options.reset) {
      options.reset()
      syncAppliedFilters()
      return
    }

    options.fields.forEach((field) => {
      setFieldValue(field.key, resolveClearValue(field))
    })
  }

  return {
    tags,
    hasAppliedFilters,
    advancedAppliedCount,
    syncAppliedFilters,
    removeFilter,
    clearFilters
  }
}
