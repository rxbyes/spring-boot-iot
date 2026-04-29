import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AutomationManualPageDrawer from '@/components/AutomationManualPageDrawer.vue'
import AutomationPlanImportDrawer from '@/components/AutomationPlanImportDrawer.vue'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import DeviceBatchImportDrawer from '@/components/DeviceBatchImportDrawer.vue'
import DeviceOnboardingSuggestionDrawer from '@/components/device/DeviceOnboardingSuggestionDrawer.vue'
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
    StandardTableTextColumn: true,
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

function readComponentSource(relativePath: string) {
  return readFileSync(resolve(import.meta.dirname, relativePath), 'utf8')
}

function buildDeviceReplaceProps(overrides: Record<string, unknown> = {}) {
  return {
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
    submitting: false,
    ...overrides
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

  it('keeps the batch import preview on the shared stacked first-column grammar', () => {
    const source = readComponentSource('../../components/DeviceBatchImportDrawer.vue')

    expect(source).toContain('StandardTableTextColumn')
    expect(source).toContain('secondary-prop="deviceCode"')
    expect(source).toContain('device-import-summary__pill')
    expect(source).not.toContain('<el-table-column prop="deviceCode" label="设备编码" min-width="170"')
  })

  it('keeps the device replace drawer in a two-tier Chinese hierarchy without repeated eyebrow labels', () => {
    const wrapper = mount(DeviceReplaceDrawer, {
      props: buildDeviceReplaceProps(),
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

  it('keeps the device replace source summary on a single stacked identity card', () => {
    const source = readComponentSource('../../components/DeviceReplaceDrawer.vue')

    expect(source).toContain('device-replace-summary-card--identity')
    expect(source).toContain('device-replace-summary-card__status-pills')
    expect(source).toContain("device.deviceName || device.deviceCode || '--'")
    expect(source).not.toContain('<span>设备编码</span>')
    expect(source).not.toContain('<span>设备名称</span>')
  })

  it('keeps the onboarding suggestion summary on a stacked identity card', () => {
    const source = readComponentSource('../../components/device/DeviceOnboardingSuggestionDrawer.vue')

    expect(source).toContain('device-onboarding-suggestion-drawer__identity-card')
    expect(source).toContain('device-onboarding-suggestion-drawer__status-pill')
    expect(source).toContain('{{ identityName }}')
    expect(source).toContain('{{ identityDeviceCode }}')
    expect(source).toContain('{{ identityTraceId }}')
    expect(source).not.toContain('<dt>设备编码</dt>')
    expect(source).not.toContain('<dt>Trace</dt>')
  })

  it('keeps the device capability execute drawer summary on an identity-first card', () => {
    const source = readComponentSource('../../components/device/DeviceCapabilityExecuteDrawer.vue')

    expect(source).toContain('device-capability-execute-drawer__summary-card--identity')
    expect(source).toContain('device-capability-execute-drawer__summary-meta')
    expect(source).toContain("{{ capability?.name || '--' }}")
  })

  it('keeps the device capability workbench drawer summary on an identity-first card', () => {
    const source = readComponentSource('../../components/device/DeviceCapabilityWorkbenchDrawer.vue')

    expect(source).toContain('device-capability-workbench-drawer__summary-card--identity')
    expect(source).toContain('device-capability-workbench-drawer__summary-meta')
    expect(source).toContain('{{ identityTitle }}')
  })

  it('does not show the old automatic refresh hint in the device replace drawer', () => {
    const wrapper = mount(DeviceReplaceDrawer, {
      props: buildDeviceReplaceProps({
        refreshing: true,
        refreshState: 'info'
      }),
      global: {
        stubs: {
          ...buildGlobalStubs(),
          ElInputNumber: true
        }
      }
    })

    expect(wrapper.text()).not.toContain('已先填入当前设备摘要，正在补全最新设备档案。')
    expect(wrapper.html()).not.toContain('device-replace-inline-state')
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
