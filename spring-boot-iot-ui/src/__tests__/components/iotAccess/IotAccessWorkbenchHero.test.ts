import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import IotAccessWorkbenchHero from '@/components/iotAccess/IotAccessWorkbenchHero.vue';

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  template: '<section class="panel-card-stub"><slot /></section>'
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

describe('IotAccessWorkbenchHero', () => {
  it('renders judgement, tags, summary items and actions', () => {
    const wrapper = mount(IotAccessWorkbenchHero, {
      props: {
        eyebrow: '接入智维 / 资产治理',
        title: '产品定义中心',
        judgement: '先补齐产品契约，再处理库存治理。',
        description: '统一承接产品台账、物模型治理提醒与强相关跳转。',
        tags: [
          { label: '工作母版', value: 'A 指挥甲板型' },
          { label: '页面倾向', value: 'B 资产治理型' }
        ],
        actions: [
          { label: '新增产品', to: '/products', variant: 'primary' },
          { label: '查看设备资产', to: '/devices', variant: 'secondary' }
        ],
        summaryItems: [
          { label: '治理主判断', value: '契约完整性优先' },
          { label: '主战区', value: '产品台账' }
        ]
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub,
          RouterLink: RouterLinkStub
        }
      }
    });

    expect(wrapper.text()).toContain('先补齐产品契约，再处理库存治理。');
    expect(wrapper.find('.iot-access-workbench-hero').exists()).toBe(true);
    expect(wrapper.find('.iot-access-workbench-hero__layout').exists()).toBe(true);
    expect(wrapper.find('.iot-access-workbench-hero__actions').exists()).toBe(true);
    expect(wrapper.findAll('.iot-access-workbench-hero__tag')).toHaveLength(2);
    expect(wrapper.findAll('.iot-access-workbench-hero__summary-item')).toHaveLength(2);
    expect(wrapper.findAll('a.router-link-stub')).toHaveLength(2);
    expect(wrapper.findAll('.iot-access-workbench-hero__action--primary')).toHaveLength(1);
    expect(wrapper.findAll('.iot-access-workbench-hero__action--secondary')).toHaveLength(1);
  });

  it('uses defaults for optional sections and action variant', () => {
    const wrapper = mount(IotAccessWorkbenchHero, {
      props: {
        judgement: '先补齐产品契约，再处理库存治理。',
        actions: [{ label: '查看设备资产', to: '/devices' }]
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub,
          RouterLink: RouterLinkStub
        }
      }
    });

    expect(wrapper.find('.iot-access-workbench-hero__tags').exists()).toBe(false);
    expect(wrapper.find('.iot-access-workbench-hero__summary').exists()).toBe(false);
    expect(wrapper.findAll('.iot-access-workbench-hero__action--primary')).toHaveLength(0);
    expect(wrapper.findAll('.iot-access-workbench-hero__action--secondary')).toHaveLength(1);
  });
});
