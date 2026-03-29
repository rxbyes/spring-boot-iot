import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import AuditLogDetailDrawer from '@/components/AuditLogDetailDrawer.vue';

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['modelValue', 'eyebrow', 'title', 'subtitle'],
  emits: ['update:modelValue'],
  template: `
    <section v-if="modelValue" class="audit-log-detail-drawer-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p>{{ subtitle }}</p>
      <slot />
    </section>
  `
});

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
});

describe('AuditLogDetailDrawer', () => {
  it('renders reverse diagnostic actions in system-error mode', async () => {
    const wrapper = mount(AuditLogDetailDrawer, {
      props: {
        modelValue: true,
        title: '异常详情',
        detail: {
          operationType: 'system_error',
          operationResult: 0,
          traceId: 'trace-001',
          deviceCode: 'demo-device-01',
          productKey: 'demo-product'
        },
        showTraceAction: true,
        showAccessErrorAction: true
      },
      global: {
        stubs: {
          StandardDetailDrawer: StandardDetailDrawerStub,
          StandardButton: StandardButtonStub
        }
      }
    });

    const drawer = wrapper.findComponent(StandardDetailDrawerStub);

    expect(drawer.props('eyebrow')).toBeUndefined();
    expect(wrapper.text()).not.toContain('Audit Log Detail');
    expect(wrapper.text()).toContain('返回链路追踪');
    expect(wrapper.text()).toContain('回看失败归档');

    const buttons = wrapper.findAll('button');
    await buttons[0]!.trigger('click');
    await buttons[1]!.trigger('click');

    expect(wrapper.emitted('jump-message-trace')).toHaveLength(1);
    expect(wrapper.emitted('jump-access-error')).toHaveLength(1);
  });
});
