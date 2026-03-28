import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import IotAccessPageShell from '@/components/iotAccess/IotAccessPageShell.vue';

const RouterLinkStub = defineComponent({
  name: 'RouterLink',
  props: {
    to: {
      type: String,
      required: true
    }
  },
  template: '<a class="router-link-stub" :href="to"><slot /></a>'
});

describe('IotAccessPageShell', () => {
  it('renders lightweight breadcrumbs, title, actions and status strip', () => {
    const wrapper = mount(IotAccessPageShell, {
      props: {
        title: '产品定义中心',
        breadcrumbs: [
          { label: '接入智维', to: '/device-access' },
          { label: '产品定义中心' }
        ],
        status: '来自异常观测台 · 优先核对产品契约'
      },
      slots: {
        actions: '<button type="button" class="shell-action">新增产品</button>'
      },
      global: {
        stubs: {
          RouterLink: RouterLinkStub
        }
      }
    });

    expect(wrapper.find('.iot-access-page-shell').exists()).toBe(true);
    expect(wrapper.findAll('.iot-access-page-shell__breadcrumb-item')).toHaveLength(2);
    expect(wrapper.find('a.router-link-stub').attributes('href')).toBe('/device-access');
    expect(wrapper.find('.iot-access-page-shell__title').text()).toBe('产品定义中心');
    expect(wrapper.text()).toContain('来自异常观测台 · 优先核对产品契约');
    expect(wrapper.find('.shell-action').text()).toBe('新增产品');
  });

  it('keeps the header minimal when optional areas are missing', () => {
    const wrapper = mount(IotAccessPageShell, {
      props: {
        title: '链路验证中心'
      },
      global: {
        stubs: {
          RouterLink: RouterLinkStub
        }
      }
    });

    expect(wrapper.find('.iot-access-page-shell__breadcrumbs').exists()).toBe(false);
    expect(wrapper.find('.iot-access-page-shell__actions').exists()).toBe(false);
    expect(wrapper.find('.iot-access-page-shell__status').exists()).toBe(false);
  });
});
