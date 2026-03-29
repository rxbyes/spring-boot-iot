import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AutomationManualPageDrawer from '@/components/AutomationManualPageDrawer.vue'
import AutomationPlanImportDrawer from '@/components/AutomationPlanImportDrawer.vue'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import DeviceBatchImportDrawer from '@/components/DeviceBatchImportDrawer.vue'
import DeviceReplaceDrawer from '@/components/DeviceReplaceDrawer.vue'

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  props: ['modelValue', 'eyebrow', 'title', 'subtitle'],
  emits: ['update:modelValue', 'close'],
  template: `
    <section class="drawer-eyebrow-cleanup-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p v-if="subtitle">{{ subtitle }}</p>
      <slot />
      <slot name="footer" />
    </section>
  `
})

const StandardDrawerFooterStub = defineComponent({
  name: 'StandardDrawerFooter',
  template: '<footer class="drawer-eyebrow-cleanup-footer-stub"><slot /></footer>'
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  props: ['disabled', 'loading'],
  emits: ['click'],
  template: `
    <button
      type="button"
      :disabled="Boolean(disabled) || Boolean(loading)"
      @click="$emit('click')"
    >
      <slot />
    </button>
  `
})

const StandardActionLinkStub = defineComponent({
  name: 'StandardActionLink',
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
})

const StandardRowActionsStub = defineComponent({
  name: 'StandardRowActions',
  template: '<div class="drawer-eyebrow-cleanup-actions-stub"><slot /></div>'
})

function buildGlobalStubs() {
  return {
    StandardFormDrawer: StandardFormDrawerStub,
    StandardDrawerFooter: StandardDrawerFooterStub,
    StandardButton: StandardButtonStub,
    StandardActionLink: StandardActionLinkStub,
    StandardRowActions: StandardRowActionsStub,
    ElInput: true,
    ElForm: true,
    ElFormItem: true,
    ElSelect: true,
    ElOption: true,
    ElSwitch: true,
    ElCheckbox: true,
    ElTag: true,
    ElTable: true,
    ElTableColumn: true,
    ElAlert: true,
    ElEmpty: true,
    ElRadioGroup: true,
    ElRadio: true,
    ElTabs: true,
    ElTabPane: true,
    ElScrollbar: true
  }
}

describe('drawer eyebrow cleanup', () => {
  it('keeps the batch import drawer in Chinese without the legacy English eyebrow tier', () => {
    const wrapper = mount(DeviceBatchImportDrawer, {
      props: {
        modelValue: true,
        submitting: false,
        result: null
      },
      global: {
        stubs: buildGlobalStubs()
      }
    })

    const drawer = wrapper.findComponent(StandardFormDrawerStub)

    expect(drawer.props('eyebrow')).toBeUndefined()
    expect(wrapper.text()).toContain('批量导入设备')
    expect(wrapper.text()).not.toContain('Device Batch Import')
  })

  it('keeps the device replace drawer in a two-tier Chinese hierarchy without repeated eyebrow labels', () => {
    const wrapper = mount(DeviceReplaceDrawer, {
      props: {
        modelValue: true,
        device: {
          id: 1,
          productKey: 'south-monitor',
          deviceName: '旧设备',
          deviceCode: 'device-001',
          deviceStatus: 1,
          onlineStatus: 1,
          parentDeviceId: null,
          parentDeviceName: '',
          parentDeviceCode: '',
          gatewayDeviceName: '',
          gatewayDeviceCode: '',
          deviceSecret: '',
          clientId: '',
          username: '',
          password: '',
          firmwareVersion: '',
          ipAddress: '',
          address: '',
          metadataJson: ''
        },
        productOptions: [],
        deviceOptions: [],
        productLoading: false,
        deviceOptionsLoading: false,
        refreshing: false,
        refreshMessage: '',
        refreshState: '',
        submitting: false
      },
      global: {
        stubs: {
          ...buildGlobalStubs(),
          ElInputNumber: true
        }
      }
    })

    const drawer = wrapper.findComponent(StandardFormDrawerStub)

    expect(drawer.props('eyebrow')).toBeUndefined()
    expect(wrapper.text()).toContain('更换设备')
    expect(wrapper.text()).not.toContain('设备替换操作')
  })

  it('keeps automation utility drawers in Chinese without English eyebrow tiers', () => {
    const manualWrapper = mount(AutomationManualPageDrawer, {
      props: {
        modelValue: true,
        scopeOptions: ['全量执行'],
        templateOptions: [],
        buildTemplateLabel: (template: string) => template
      },
      global: {
        stubs: buildGlobalStubs()
      }
    })
    const importWrapper = mount(AutomationPlanImportDrawer, {
      props: {
        modelValue: true
      },
      global: {
        stubs: buildGlobalStubs()
      }
    })

    const manualDrawer = manualWrapper.findComponent(StandardFormDrawerStub)
    const importDrawer = importWrapper.findComponent(StandardFormDrawerStub)

    expect(manualDrawer.props('eyebrow')).toBeUndefined()
    expect(importDrawer.props('eyebrow')).toBeUndefined()
    expect(manualWrapper.text()).toContain('新增自定义页面')
    expect(importWrapper.text()).toContain('导入自动化计划')
    expect(manualWrapper.text()).not.toContain('Page Inventory')
    expect(importWrapper.text()).not.toContain('Automation Import')
  })

  it('keeps csv setting drawers on title and subtitle only without legacy English eyebrow tiers', () => {
    const wrapper = mount(CsvColumnSettingDialog, {
      props: {
        modelValue: true,
        title: '导出列设置',
        options: [
          { key: 'deviceCode', label: '设备编码' },
          { key: 'productKey', label: '产品标识' }
        ],
        selectedKeys: ['deviceCode'],
        presets: []
      },
      global: {
        stubs: buildGlobalStubs()
      }
    })

    const drawers = wrapper.findAllComponents(StandardFormDrawerStub)

    expect(drawers.length).toBe(4)
    expect(drawers.every((item) => item.props('eyebrow') === undefined)).toBe(true)
    expect(wrapper.text()).toContain('导出列设置')
    expect(wrapper.text()).not.toContain('Export Columns')
    expect(wrapper.text()).not.toContain('Import Preview')
    expect(wrapper.text()).not.toContain('Preset Template')
    expect(wrapper.text()).not.toContain('Conflict Strategy')
  })
})
