<script lang="ts">
import { computed, defineComponent, h, resolveComponent } from 'vue'

function readPathValue(source: Record<string, unknown> | null | undefined, path?: string) {
  if (!source || !path) {
    return undefined
  }

  return path.split('.').reduce<unknown>((current, segment) => {
    if (current && typeof current === 'object' && segment in (current as Record<string, unknown>)) {
      return (current as Record<string, unknown>)[segment]
    }
    return undefined
  }, source)
}

function normalizeSlotContent(content: unknown) {
  if (Array.isArray(content)) {
    return content.filter((item) => item !== null && item !== undefined && item !== false)
  }

  if (content === null || content === undefined || content === false) {
    return []
  }

  return [content]
}

export default defineComponent({
  name: 'StandardTableTextColumn',
  inheritAttrs: false,
  props: {
    prop: {
      type: String,
      default: undefined
    },
    label: {
      type: String,
      default: undefined
    },
    minWidth: {
      type: [String, Number],
      default: undefined
    },
    width: {
      type: [String, Number],
      default: undefined
    },
    fixed: {
      type: [Boolean, String],
      default: undefined
    },
    align: {
      type: String,
      default: 'left'
    },
    headerAlign: {
      type: String,
      default: undefined
    },
    secondaryProp: {
      type: String,
      default: undefined
    },
    emptyText: {
      type: String,
      default: '--'
    },
    secondaryEmptyText: {
      type: String,
      default: ''
    },
    secondaryPrefix: {
      type: String,
      default: ''
    },
    showOverflowTooltip: {
      type: [Boolean, Object],
      default: true
    }
  },
  setup(props, { attrs, slots }) {
    const resolvedShowOverflowTooltip = computed(() => {
      if (props.showOverflowTooltip === false) {
        return false
      }

      if (props.showOverflowTooltip === true) {
        return {
          effect: 'light',
          placement: 'top-start'
        }
      }

      return {
        effect: 'light',
        placement: 'top-start',
        ...(props.showOverflowTooltip as Record<string, unknown>)
      }
    })

    const formatCellText = (value: unknown, fallback: string) => {
      if (value === null || value === undefined) {
        return fallback
      }

      const text = String(value)
      return text.trim() ? text : fallback
    }

    const resolvePrimaryText = (scope: Record<string, unknown>) =>
      formatCellText(readPathValue(scope.row as Record<string, unknown> | undefined, props.prop), props.emptyText)

    const resolveSecondaryText = (scope: Record<string, unknown>) => {
      if (!props.secondaryProp) {
        return ''
      }

      const secondaryText = formatCellText(
        readPathValue(scope.row as Record<string, unknown> | undefined, props.secondaryProp),
        props.secondaryEmptyText
      )

      if (!secondaryText) {
        return ''
      }

      return `${props.secondaryPrefix}${secondaryText}`
    }

    const renderDefaultCell = (scope: Record<string, unknown>) => {
      const primaryContent = slots.default
        ? normalizeSlotContent(slots.default(scope))
        : [resolvePrimaryText(scope)]
      const secondaryContent = slots.secondary
        ? normalizeSlotContent(slots.secondary(scope))
        : resolveSecondaryText(scope)
          ? [resolveSecondaryText(scope)]
          : []

      if (secondaryContent.length === 0 && slots.default) {
        return primaryContent
      }

      const primaryTitle = !slots.default ? resolvePrimaryText(scope) : undefined
      const secondaryTitle = !slots.secondary && secondaryContent.length > 0
        ? resolveSecondaryText(scope)
        : undefined

      return h(
        'div',
        {
          class: ['standard-table-text', secondaryContent.length === 0 ? 'standard-table-text--single' : '']
        },
        [
          h(
            'span',
            {
              class: [
                'standard-table-text__primary',
                secondaryContent.length === 0 ? 'standard-table-text__primary--solo' : ''
              ],
              title: primaryTitle
            },
            primaryContent
          ),
          ...(secondaryContent.length > 0
            ? [
                h(
                  'span',
                  {
                    class: 'standard-table-text__secondary',
                    title: secondaryTitle
                  },
                  secondaryContent
                )
              ]
            : [])
        ]
      )
    }

    return () => {
      const tableColumnSlots: Record<string, (scope: Record<string, unknown>) => unknown> = {}

      tableColumnSlots.default = renderDefaultCell

      if (slots.header) {
        tableColumnSlots.header = (scope: Record<string, unknown>) => slots.header?.(scope)
      }

      return h(
        resolveComponent('el-table-column'),
        {
          ...attrs,
          prop: props.prop,
          label: props.label,
          minWidth: props.minWidth,
          width: props.width,
          fixed: props.fixed,
          align: props.align,
          headerAlign: props.headerAlign,
          showOverflowTooltip: resolvedShowOverflowTooltip.value
        },
        tableColumnSlots
      )
    }
  }
})
</script>
