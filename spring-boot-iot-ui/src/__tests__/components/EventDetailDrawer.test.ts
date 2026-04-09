import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import EventDetailDrawer from '@/components/EventDetailDrawer.vue';

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['modelValue', 'eyebrow', 'title', 'subtitle'],
  emits: ['update:modelValue'],
  template: `
    <section v-if="modelValue" class="event-detail-drawer-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p>{{ subtitle }}</p>
      <slot />
    </section>
  `
});

describe('EventDetailDrawer', () => {
  it('keeps the drawer heading in Chinese without the legacy English eyebrow tier', () => {
    const wrapper = mount(EventDetailDrawer, {
      props: {
        modelValue: true,
        detail: {
          eventTitle: '泵房异常事件',
          eventCode: 'EVENT-001',
          alarmLevel: 'orange',
          currentRiskLevel: 'yellow',
          riskLevel: 'yellow',
          status: 1,
          triggerTime: '2026-03-29 11:00:00'
        }
      },
      global: {
        stubs: {
          StandardDetailDrawer: StandardDetailDrawerStub
        }
      }
    });

    const drawer = wrapper.findComponent(StandardDetailDrawerStub);

    expect(drawer.props('eyebrow')).toBeUndefined();
    expect(drawer.props('title')).toBe('泵房异常事件');
    expect(wrapper.text()).toContain('事件概览');
    expect(wrapper.text()).toContain('当前风险态势');
    expect(wrapper.text()).toContain('告警等级');
    expect(wrapper.text()).not.toContain('Event Detail');
  });

  it('avoids repeating owner and status labels across the summary and context sections', () => {
    const wrapper = mount(EventDetailDrawer, {
      props: {
        modelValue: true,
        detail: {
          eventTitle: '泵房异常事件',
          eventCode: 'EVENT-001',
          alarmLevel: 'orange',
          currentRiskLevel: 'yellow',
          riskLevel: 'yellow',
          status: 1,
          responsibleUser: '值班员A',
          triggerTime: '2026-03-29 11:00:00'
        }
      },
      global: {
        stubs: {
          StandardDetailDrawer: StandardDetailDrawerStub
        }
      }
    });

    expect(wrapper.text().match(/责任人/g)?.length ?? 0).toBe(1);
    expect(wrapper.text().match(/当前状态/g)?.length ?? 0).toBe(1);
    expect(wrapper.text()).toContain('橙色');
  });
});
