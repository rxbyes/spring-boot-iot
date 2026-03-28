import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import IotAccessTabWorkspace from '@/components/iotAccess/IotAccessTabWorkspace.vue';

const { mockRoute, mockRouter } = vi.hoisted(() => ({
  mockRoute: {
    query: {} as Record<string, unknown>
  },
  mockRouter: {
    replace: vi.fn()
  }
}));

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}));

describe('IotAccessTabWorkspace', () => {
  beforeEach(() => {
    mockRoute.query = {};
    mockRouter.replace.mockReset();
    mockRouter.replace.mockResolvedValue(undefined);
  });

  it('uses the matching route query as active tab and exposes it to the panel slot', () => {
    mockRoute.query = { tab: 'recent' };

    const wrapper = mount(IotAccessTabWorkspace, {
      props: {
        items: [
          { key: 'recommended', label: '推荐处理' },
          { key: 'recent', label: '最近使用' },
          { key: 'catalog', label: '全部能力' }
        ]
      },
      slots: {
        default: ({ activeKey }: { activeKey: string }) => `active:${activeKey}`
      }
    });

    expect(wrapper.find('.iot-access-tab-workspace').exists()).toBe(true);
    expect(wrapper.find('.iot-access-tab-workspace__tab--active').text()).toContain('最近使用');
    expect(wrapper.text()).toContain('active:recent');
  });

  it('syncs tab changes back to route query and preserves other query params', async () => {
    mockRoute.query = { deviceCode: 'demo-device-01', tab: 'recommended' };

    const wrapper = mount(IotAccessTabWorkspace, {
      props: {
        items: [
          { key: 'recommended', label: '推荐处理' },
          { key: 'recent', label: '最近使用' }
        ]
      }
    });

    await wrapper.findAll('button').at(1)?.trigger('click');

    expect(mockRouter.replace).toHaveBeenCalledWith({
      query: {
        deviceCode: 'demo-device-01',
        tab: 'recent'
      }
    });
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['recent']);
  });

  it('marks the tab rail as business-view navigation with the refined minimal shell', () => {
    const wrapper = mount(IotAccessTabWorkspace, {
      props: {
        items: [
          { key: 'asset', label: '资产底座' },
          { key: 'diagnostics', label: '诊断排障' }
        ],
        defaultKey: 'asset',
        syncQuery: false
      }
    });

    expect(wrapper.classes()).toContain('iot-access-tab-workspace--minimal')
    expect(wrapper.find('nav').attributes('aria-label')).toBe('业务视图切换');
    expect(wrapper.find('.iot-access-tab-workspace__tabs').classes()).toContain('iot-access-tab-workspace__tabs--minimal')
  });
});
