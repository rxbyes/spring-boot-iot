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
  template: '<article class="metric-card-stub" />'
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
          eyebrow: '先做什么',
          title: '优先处理产品契约阻塞',
          description: '停用库存阻塞和候选待确认都需要先收口。',
          actionLabel: '进入产品定义中心',
          actionTo: '/products'
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
    expect(wrapper.findAll('.metric-card-stub')).toHaveLength(3);
  });
});
