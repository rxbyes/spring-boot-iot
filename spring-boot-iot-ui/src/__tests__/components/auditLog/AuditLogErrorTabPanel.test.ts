import { computed, defineComponent, inject, provide, ref } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AuditLogErrorTabPanel from '@/components/auditLog/AuditLogErrorTabPanel.vue'
import { splitWorkbenchRowActions } from '@/utils/adaptiveActionColumn'

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section>
      <div><slot name="primary" /></div>
      <div><slot name="advanced" /></div>
      <div><slot name="actions" /></div>
    </section>
  `
})

const StandardAppliedFiltersBarStub = defineComponent({
  name: 'StandardAppliedFiltersBar',
  template: '<div class="applied-filters-stub" />'
})

const StandardInlineStateStub = defineComponent({
  name: 'StandardInlineState',
  props: ['message'],
  template: '<div class="inline-state-stub">{{ message }}</div>'
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
})

const StandardActionMenuStub = defineComponent({
  name: 'StandardActionMenu',
  props: ['label', 'items'],
  emits: ['command'],
  template: '<button type="button">{{ label }}</button>'
})

const StandardPaginationStub = defineComponent({
  name: 'StandardPagination',
  template: '<div class="pagination-stub" />'
})

const StandardTableTextColumnStub = defineComponent({
  name: 'StandardTableTextColumn',
  props: ['label'],
  template: '<div class="text-column-stub">{{ label }}</div>'
})

const StandardWorkbenchRowActionsStub = defineComponent({
  name: 'StandardWorkbenchRowActions',
  props: ['directItems', 'menuItems', 'maxDirectItems'],
  emits: ['command'],
  setup(props) {
    const resolvedActions = computed(() =>
      splitWorkbenchRowActions({
        directItems: props.directItems || [],
        menuItems: props.menuItems || [],
        maxDirectItems: Number(props.maxDirectItems || 3)
      })
    )
    return {
      resolvedActions
    }
  },
  template: `
    <div class="row-actions-stub">
      <button
        v-for="item in resolvedActions.directItems"
        :key="item.key || item.command"
        type="button"
        @click="$emit('command', item.command)"
      >
        {{ item.label }}
      </button>
    </div>
  `
})

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: ['modelValue', 'id', 'placeholder'],
  template: '<input :id="id" :value="modelValue || \'\'" :placeholder="placeholder" />'
})

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: ['data'],
  setup(props) {
    provide('tableRows', computed(() => props.data ?? []))
    return {}
  },
  template: '<section class="el-table-stub"><slot /></section>'
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['label'],
  setup() {
    const rows = inject('tableRows', ref([]))
    return { rows }
  },
  template: `
    <div class="el-table-column-stub" :data-label="label">
      <div v-for="(row, index) in rows" :key="index">
        <slot :row="row" />
      </div>
    </div>
  `
})

const createClusterRow = () => ({
  clusterKey: 'message.mqtt|com.ghlzm.iot.common.exception.BizException|500',
  operationModule: 'message.mqtt',
  exceptionClass: 'com.ghlzm.iot.common.exception.BizException',
  errorCode: '500',
  count: 12,
  distinctTraceCount: 4,
  distinctDeviceCount: 3,
  latestOperationTime: '2026-04-27 09:00:00',
  latestRequestUrl: '$dp',
  latestRequestMethod: 'MQTT',
  latestResultMessage: 'BizException: mqtt-json-decrypted MQTT 负载不能为空'
})

function mountPanel(propOverrides: Record<string, unknown> = {}) {
  return mount(AuditLogErrorTabPanel, {
    props: {
      searchForm: {
        traceId: '',
        deviceCode: '',
        productKey: '',
        operationModule: '',
        requestMethod: '',
        requestUrl: '',
        errorCode: '',
        exceptionClass: '',
        operationResult: undefined
      },
      quickSearchKeyword: '',
      showAdvancedFilters: false,
      advancedFilterHint: '',
      requestMethodOptions: [],
      toolbarActions: [{ command: 'export-config', label: '导出列设置' }],
      appliedQuickSearchValue: '',
      activeFilterTags: [],
      hasAppliedFilters: false,
      showInlineState: false,
      inlineMessage: '',
      clusterLoading: false,
      clusterErrorMessage: '',
      clusterRows: [createClusterRow()],
      errorViewMode: 'detail',
      clusterContextSummary: '',
      canReturnToClusterResults: false,
      loading: false,
      tableData: [],
      pagination: { pageNum: 1, pageSize: 10, total: 0 },
      auditActionColumnWidth: 160,
      formatValue: (value: unknown) => String(value ?? '--'),
      getOperationResultName: () => '失败',
      getOperationResultTag: () => 'danger',
      getAuditDirectActions: () => [],
      getAuditMenuActions: () => [],
      ...propOverrides
    },
    global: {
      directives: {
        loading: () => undefined
      },
      stubs: {
        StandardListFilterHeader: StandardListFilterHeaderStub,
        StandardAppliedFiltersBar: StandardAppliedFiltersBarStub,
        StandardActionMenu: StandardActionMenuStub,
        StandardInlineState: StandardInlineStateStub,
        StandardButton: StandardButtonStub,
        StandardPagination: StandardPaginationStub,
        StandardTableTextColumn: StandardTableTextColumnStub,
        StandardWorkbenchRowActions: StandardWorkbenchRowActionsStub,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub,
        ElInput: ElInputStub,
        ElFormItem: true,
        ElSelect: true,
        ElOption: true,
        ElTag: true
      }
    }
  })
}

describe('AuditLogErrorTabPanel', () => {
  it('shows the detail table first and keeps refresh utilities after reset', async () => {
    const wrapper = mountPanel({
      tableData: [
        {
          id: 1,
          operationModule: 'message.mqtt',
          requestMethod: 'MQTT',
          errorCode: '500',
          resultMessage: 'BizException: mqtt-json-decrypted MQTT 负载不能为空',
          operationTime: '2026-04-27 12:00:00',
          operationResult: 0
        }
      ],
      pagination: { pageNum: 1, pageSize: 10, total: 1 }
    })

    const panelText = wrapper.text()
    expect(panelText).toContain('按异常分组查看')
    expect(panelText).toContain('刷新列表')
    expect(panelText).toContain('更多操作')
    expect(panelText.indexOf('重置')).toBeLessThan(panelText.indexOf('刷新列表'))
    expect(panelText.indexOf('刷新列表')).toBeLessThan(panelText.indexOf('更多操作'))

    const button = wrapper.findAll('button').find((item) => item.text().includes('按异常分组查看'))
    await button?.trigger('click')

    expect(wrapper.emitted('open-clusters')).toHaveLength(1)
  })

  it('shows the cluster stage with return controls and applies a cluster row', async () => {
    const wrapper = mountPanel({
      errorViewMode: 'clusters'
    })

    expect(wrapper.text()).toContain('返回异常明细')
    expect(wrapper.text()).toContain('当前筛选条件下的异常分组')
    expect(wrapper.text()).toContain('message.mqtt')

    const clusterButton = wrapper.findAll('button').find((item) => item.text().includes('message.mqtt'))
    await clusterButton?.trigger('click')

    expect(wrapper.emitted('apply-cluster')).toEqual([
      ['message.mqtt|com.ghlzm.iot.common.exception.BizException|500']
    ])
  })

  it('shows the detail refiner context and its recovery actions after cluster selection', async () => {
    const wrapper = mountPanel({
      clusterContextSummary: 'message.mqtt / BizException / 500',
      canReturnToClusterResults: true
    })

    expect(wrapper.text()).toContain('当前按分组定位')
    expect(wrapper.text()).toContain('清除分组定位')
    expect(wrapper.text()).toContain('返回异常分组结果')

    const clearButton = wrapper.findAll('button').find((item) => item.text().includes('清除分组定位'))
    const backButton = wrapper.findAll('button').find((item) => item.text().includes('返回异常分组结果'))
    await clearButton?.trigger('click')
    await backButton?.trigger('click')

    expect(wrapper.emitted('clear-cluster-refiner')).toHaveLength(1)
    expect(wrapper.emitted('return-to-clusters')).toHaveLength(1)
  })

  it('keeps the detail list compact instead of exposing low-signal identity fields inline', () => {
    const wrapper = mountPanel({
      tableData: [
        {
          id: 1,
          operationModule: 'message.mqtt',
          requestMethod: 'MQTT',
          errorCode: '500',
          resultMessage: 'BizException: mqtt-json-decrypted MQTT 负载不能为空',
          operationTime: '2026-04-27 12:00:00',
          operationResult: 0,
          traceId: 'a60d94338fc3448eaa3e4b5eec70a84b',
          deviceCode: '',
          productKey: '',
          exceptionClass: 'com.ghlzm.iot.common.exception.BizException',
          requestUrl: '$dp',
          operationMethod: 'MqttMessageConsumer#messageArrived'
        }
      ],
      pagination: { pageNum: 1, pageSize: 10, total: 1 }
    })

    expect(wrapper.find('.audit-log-mobile-list').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('操作方法')
    expect(wrapper.text()).not.toContain('请求URL/目标')
    expect(wrapper.text()).not.toContain('TraceId')
    expect(wrapper.text()).not.toContain('设备编码')
    expect(wrapper.text()).not.toContain('产品标识')
    expect(wrapper.text()).not.toContain('异常类型')
    expect(wrapper.text()).toContain('异常摘要')
  })
})
