import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import IotAccessSignalDeck from '@/components/iotAccess/IotAccessSignalDeck.vue';

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  template: '<section class="panel-card-stub"><slot /></section>'
});

const MetricCardStub = defineComponent({
  name: 'MetricCard',
  props: ['label', 'value', 'size'],
  template: '<article class="metric-card-stub">{{ label }}|{{ value }}|{{ size }}</article>'
});

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

describe('IotAccessSignalDeck', () => {
  it('renders lead copy and compact metrics', () => {
    const wrapper = mount(IotAccessSignalDeck, {
      props: {
        lead: {
          title: '优先处理产品契约阻塞',
          description: '停用库存阻塞和候选待确认都需要先收口。',
          action: {
            label: '进入产品定义中心',
            to: '/products'
          }
        },
        metrics: [
          { label: '待确认候选', value: '12' },
          { label: '停用阻塞', value: '4' },
          { label: '最近活跃产品', value: '9' }
        ]
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub,
          MetricCard: MetricCardStub,
          RouterLink: RouterLinkStub
        }
      }
    });

    expect(wrapper.text()).toContain('优先处理产品契约阻塞');
    expect(wrapper.find('.iot-access-signal-deck').exists()).toBe(true);
    expect(wrapper.find('.iot-access-signal-deck').classes()).toContain('iot-access-signal-deck--with-metrics');
    expect(wrapper.find('.iot-access-signal-deck__lead').exists()).toBe(true);
    const leadLinks = wrapper.findAll('a.router-link-stub');
    expect(leadLinks).toHaveLength(1);
    expect(leadLinks[0]?.text()).toContain('进入产品定义中心');
    expect(wrapper.findAll('.metric-card-stub')).toHaveLength(3);
    expect(wrapper.findAll('.metric-card-stub').map((item) => item.text())).toEqual([
      '待确认候选|12|compact',
      '停用阻塞|4|compact',
      '最近活跃产品|9|compact'
    ]);
    expect(wrapper.text()).not.toContain('先做什么');
  });

  it('supports lead-only rendering without metrics deck', () => {
    const wrapper = mount(IotAccessSignalDeck, {
      props: {
        lead: {
          title: '优先处理产品契约阻塞'
        }
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub,
          MetricCard: MetricCardStub,
          RouterLink: RouterLinkStub
        }
      }
    });

    expect(wrapper.find('.iot-access-signal-deck__metrics').exists()).toBe(false);
    expect(wrapper.find('.iot-access-signal-deck').classes()).toContain('iot-access-signal-deck--lead-only');
  });

  it('keeps backward compatibility for actionLabel and actionTo', () => {
    const wrapper = mount(IotAccessSignalDeck, {
      props: {
        lead: {
          title: '优先处理产品契约阻塞',
          actionLabel: '进入产品定义中心',
          actionTo: '/products'
        }
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub,
          MetricCard: MetricCardStub,
          RouterLink: RouterLinkStub
        }
      }
    });

    const links = wrapper.findAll('a.router-link-stub');
    expect(links).toHaveLength(1);
    expect(links[0]?.text()).toContain('进入产品定义中心');
    expect(links[0]?.attributes('href')).toBe('/products');
  });
});
