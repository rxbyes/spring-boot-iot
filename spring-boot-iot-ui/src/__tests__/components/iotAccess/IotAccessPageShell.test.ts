import { RouterLinkStub } from '@vue/test-utils'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import IotAccessPageShell from '@/components/iotAccess/IotAccessPageShell.vue'

describe('IotAccessPageShell', () => {
  it('hides in-page breadcrumbs by default to avoid duplicating shell navigation', () => {
    const wrapper = mount(IotAccessPageShell, {
      props: {
        title: '产品定义中心',
        showTitle: false,
        breadcrumbs: [
          { label: '接入智维', to: '/device-access' },
          { label: '产品定义中心' }
        ]
      },
      slots: {
        default: '<div class="shell-body">body</div>'
      },
      global: {
        stubs: {
          RouterLink: RouterLinkStub
        }
      }
    })

    expect(wrapper.find('.iot-access-page-shell').exists()).toBe(true)
    expect(wrapper.find('.iot-access-page-shell__breadcrumbs').exists()).toBe(false)
    expect(wrapper.find('.iot-access-page-shell__headline').exists()).toBe(false)
    expect(wrapper.find('.shell-body').exists()).toBe(true)
  })

  it('can render a single breadcrumb row when an overview page explicitly opts in', () => {
    const wrapper = mount(IotAccessPageShell, {
      props: {
        showBreadcrumbs: true,
        showTitle: false,
        breadcrumbs: [{ label: '接入智维' }]
      },
      global: {
        stubs: {
          RouterLink: RouterLinkStub
        }
      }
    })

    expect(wrapper.find('.iot-access-page-shell__breadcrumbs').exists()).toBe(true)
    expect(wrapper.text()).toContain('接入智维')
  })
})
