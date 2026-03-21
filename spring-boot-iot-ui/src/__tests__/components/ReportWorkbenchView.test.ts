import { defineComponent, nextTick } from 'vue';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import ReportWorkbenchView from '@/views/ReportWorkbenchView.vue';
import { reportByHttp } from '@/api/iot';

vi.mock('@/api/iot', () => ({
  reportByHttp: vi.fn()
}));

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>();
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn()
    }
  };
});

const ElButtonStub = defineComponent({
  name: 'ElButton',
  props: ['nativeType', 'loading', 'disabled'],
  emits: ['click'],
  template: `
    <button
      class="el-button-stub"
      :type="nativeType || 'button'"
      :disabled="Boolean(disabled) || Boolean(loading)"
      @click="$emit('click')"
    >
      <slot />
    </button>
  `
});

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: ['modelValue', 'type', 'id'],
  emits: ['update:modelValue'],
  template: `
    <textarea
      v-if="type === 'textarea'"
      :id="id"
      class="el-input-stub-textarea"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
    <input
      v-else
      :id="id"
      class="el-input-stub-input"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  `
});

const ElRadioGroupStub = defineComponent({
  name: 'ElRadioGroup',
  template: '<div class="el-radio-group-stub"><slot /></div>'
});

const ElRadioButtonStub = defineComponent({
  name: 'ElRadioButton',
  props: ['label'],
  template: '<button class="el-radio-button-stub" type="button"><slot /></button>'
});

const ElCollapseStub = defineComponent({
  name: 'ElCollapse',
  template: '<div class="el-collapse-stub"><slot /></div>'
});

const ElCollapseItemStub = defineComponent({
  name: 'ElCollapseItem',
  template: '<section class="el-collapse-item-stub"><slot /></section>'
});

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  template: '<section class="panel-card-stub"><slot /></section>'
});

const ResponsePanelStub = defineComponent({
  name: 'ResponsePanel',
  props: ['body'],
  template: '<section class="response-panel-stub">{{ JSON.stringify(body) }}</section>'
});

const StandardInfoGridStub = defineComponent({
  name: 'StandardInfoGrid',
  props: ['items'],
  template: '<section class="standard-info-grid-stub">{{ Array.isArray(items) ? items.length : 0 }}</section>'
});

const StandardActionGroupStub = defineComponent({
  name: 'StandardActionGroup',
  template: '<div class="standard-action-group-stub"><slot /></div>'
});

const StandardFlowRailStub = defineComponent({
  name: 'StandardFlowRail',
  template: '<div class="standard-flow-rail-stub"><slot /></div>'
});

describe('ReportWorkbenchView', () => {
  beforeEach(() => {
    vi.mocked(reportByHttp).mockReset();
  });

  it('blocks submission and shows validation errors when payload is invalid', async () => {
    const wrapper = mount(ReportWorkbenchView, {
      global: {
        stubs: {
          ElButton: ElButtonStub,
          ElInput: ElInputStub,
          ElRadioGroup: ElRadioGroupStub,
          ElRadioButton: ElRadioButtonStub,
          ElCollapse: ElCollapseStub,
          ElCollapseItem: ElCollapseItemStub,
          PanelCard: PanelCardStub,
          ResponsePanel: ResponsePanelStub,
          StandardInfoGrid: StandardInfoGridStub,
          StandardActionGroup: StandardActionGroupStub,
          StandardFlowRail: StandardFlowRailStub
        }
      }
    });

    await wrapper.find('#payload').setValue('{bad-json}');
    await nextTick();

    expect(wrapper.text()).toContain('发送前请先修复以下问题');
    const submitButton = wrapper.find('button[type="submit"]');
    expect(submitButton.attributes('disabled')).toBeDefined();

    await wrapper.find('form').trigger('submit.prevent');
    expect(reportByHttp).not.toHaveBeenCalled();
  });

  it('submits request when input is valid', async () => {
    vi.mocked(reportByHttp).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: null
    });

    const wrapper = mount(ReportWorkbenchView, {
      global: {
        stubs: {
          ElButton: ElButtonStub,
          ElInput: ElInputStub,
          ElRadioGroup: ElRadioGroupStub,
          ElRadioButton: ElRadioButtonStub,
          ElCollapse: ElCollapseStub,
          ElCollapseItem: ElCollapseItemStub,
          PanelCard: PanelCardStub,
          ResponsePanel: ResponsePanelStub,
          StandardInfoGrid: StandardInfoGridStub,
          StandardActionGroup: StandardActionGroupStub,
          StandardFlowRail: StandardFlowRailStub
        }
      }
    });

    await wrapper.find('form').trigger('submit.prevent');

    expect(reportByHttp).toHaveBeenCalledTimes(1);
    const payload = vi.mocked(reportByHttp).mock.calls[0]?.[0];
    expect(payload?.payloadEncoding).toBe('ISO-8859-1');
    expect(typeof payload?.payload).toBe('string');
    expect(payload?.payload.length).toBeGreaterThan(0);
  });
});
