<script lang="ts">
import { computed, defineComponent, h, resolveComponent } from 'vue'

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

    return () => {
      const tableColumnSlots: Record<string, (scope: Record<string, unknown>) => unknown> = {}

      if (slots.default) {
        tableColumnSlots.default = (scope: Record<string, unknown>) => slots.default?.(scope)
      }

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
