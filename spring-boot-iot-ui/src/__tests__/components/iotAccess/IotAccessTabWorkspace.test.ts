import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { vi } from 'vitest'

import IotAccessTabWorkspace from '@/components/iotAccess/IotAccessTabWorkspace.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: {}
  }),
  useRouter: () => ({
    replace: vi.fn()
  })
}))

describe('IotAccessTabWorkspace', () => {
  it('marks the tab rail as business-view navigation', () => {
    const wrapper = mount(IotAccessTabWorkspace, {
      props: {
        items: [
          { key: 'asset', label: '资产底座' },
          { key: 'diagnostics', label: '诊断排障' }
        ],
        defaultKey: 'asset',
        syncQuery: false
      }
    })

    expect(wrapper.find('nav').attributes('aria-label')).toBe('业务视图切换')
    expect(wrapper.find('.iot-access-tab-workspace__tab--active').text()).toBe('资产底座')
  })

  it('applies stable per-tab button attrs and active-state attrs from item config', async () => {
    const wrapper = mount(IotAccessTabWorkspace, {
      props: {
        items: [
          {
            key: 'asset',
            label: '资产底座',
            buttonAttrs: { 'data-testid': 'workspace-tab-asset', 'data-active': 'false' },
            activeButtonAttrs: { 'data-active': 'true' }
          },
          {
            key: 'diagnostics',
            label: '诊断排障',
            buttonAttrs: { 'data-testid': 'workspace-tab-diagnostics', 'data-active': 'false' },
            activeButtonAttrs: { 'data-active': 'true' }
          }
        ],
        defaultKey: 'asset',
        syncQuery: false
      }
    })

    expect(wrapper.get('[data-testid="workspace-tab-asset"]').attributes('data-active')).toBe('true')
    expect(wrapper.get('[data-testid="workspace-tab-diagnostics"]').attributes('data-active')).toBe('false')

    await wrapper.get('[data-testid="workspace-tab-diagnostics"]').trigger('click')

    expect(wrapper.get('[data-testid="workspace-tab-asset"]').attributes('data-active')).toBe('false')
    expect(wrapper.get('[data-testid="workspace-tab-diagnostics"]').attributes('data-active')).toBe('true')
  })
})
