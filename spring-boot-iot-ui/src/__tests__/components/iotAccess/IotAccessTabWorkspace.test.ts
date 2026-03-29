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
})
