import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import AlarmDetailDrawer from '@/components/AlarmDetailDrawer.vue';

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['modelValue', 'eyebrow', 'title', 'subtitle'],
  emits: ['update:modelValue'],
  template: `
    <section v-if="modelValue" class="alarm-detail-drawer-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p>{{ subtitle }}</p>
      <slot />
    </section>
  `
});

describe('AlarmDetailDrawer', () => {
  it('keeps the drawer heading in Chinese without the legacy English eyebrow tier', () => {
    const wrapper = mount(AlarmDetailDrawer, {
      props: {
        modelValue: true,
        detail: {
          alarmTitle: '温度越限告警',
          alarmCode: 'ALARM-001',
          alarmLevel: 'critical',
          status: 0,
          triggerTime: '2026-03-29 10:00:00'
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
    expect(drawer.props('title')).toBe('温度越限告警');
    expect(wrapper.text()).toContain('告警概览');
    expect(wrapper.text()).not.toContain('Alarm Detail');
  });
});
