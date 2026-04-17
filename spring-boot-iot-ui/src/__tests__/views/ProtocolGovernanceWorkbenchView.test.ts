import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ProtocolGovernanceWorkbenchView from '@/views/ProtocolGovernanceWorkbenchView.vue'

const {
  mockPageProtocolFamilies,
  mockPageProtocolDecryptProfiles
} = vi.hoisted(() => ({
  mockPageProtocolFamilies: vi.fn(),
  mockPageProtocolDecryptProfiles: vi.fn()
}))

vi.mock('@/api/protocolGovernance', () => ({
  pageProtocolFamilies: mockPageProtocolFamilies,
  pageProtocolDecryptProfiles: mockPageProtocolDecryptProfiles
}))

const IotAccessPageShellStub = defineComponent({
  name: 'IotAccessPageShell',
  props: ['title', 'description'],
  template: `
    <section class="protocol-governance-page-shell">
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
      <slot />
    </section>
  `
})

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['title', 'description'],
  template: `
    <section class="protocol-governance-panel">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <slot />
    </section>
  `
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
})

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function mountView() {
  return mount(ProtocolGovernanceWorkbenchView, {
    global: {
      stubs: {
        IotAccessPageShell: IotAccessPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardButton: StandardButtonStub
      }
    }
  })
}

describe('ProtocolGovernanceWorkbenchView', () => {
  beforeEach(() => {
    mockPageProtocolFamilies.mockReset()
    mockPageProtocolDecryptProfiles.mockReset()

    mockPageProtocolFamilies.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 9101,
            familyCode: 'legacy-dp-crack',
            protocolCode: 'mqtt-json',
            displayName: '裂缝 legacy $dp',
            status: 'DRAFT',
            versionNo: 2,
            publishedStatus: 'PUBLISHED',
            publishedVersionNo: 1
          }
        ]
      }
    })
    mockPageProtocolDecryptProfiles.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 9201,
            profileCode: 'des-62000001',
            algorithm: 'DES',
            merchantSource: 'tenant-default',
            status: 'DRAFT',
            versionNo: 3,
            publishedStatus: 'PUBLISHED',
            publishedVersionNo: 2
          }
        ]
      }
    })
  })

  it('shows families and decrypt profiles under the access workbench without replacing /device-access', async () => {
    const wrapper = mountView()

    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('协议治理工作台')
    expect(wrapper.text()).toContain('legacy-dp-crack')
    expect(wrapper.text()).toContain('des-62000001')
    expect(wrapper.text()).toContain('协议族定义')
    expect(wrapper.text()).toContain('解密档案')
  })
})
