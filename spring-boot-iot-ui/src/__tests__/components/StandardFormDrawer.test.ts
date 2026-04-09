import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';

import StandardFormDrawer from '@/components/StandardFormDrawer.vue';

const DrawerStub = {
  props: ['modelValue'],
  emits: ['close'],
  template: `
    <div class="el-drawer-stub">
      <div class="el-drawer-stub__header"><slot name="header" /></div>
      <div class="el-drawer-stub__body"><slot /></div>
      <div class="el-drawer-stub__footer"><slot name="footer" /></div>
      <button class="el-drawer-stub__close" @click="$emit('close')">close</button>
    </div>
  `
};

describe('StandardFormDrawer', () => {
  it('renders title, subtitle and slot content without the legacy eyebrow tier', () => {
    const wrapper = mount(StandardFormDrawer, {
      props: {
        modelValue: true,
        eyebrow: 'System Form',
        title: '新增用户',
        subtitle: '通过抽屉维护用户信息'
      },
      slots: {
        default: '<div class="inner-form">form body</div>',
        footer: '<button class="submit-btn">确定</button>'
      },
      global: {
        stubs: {
          'el-drawer': DrawerStub
        }
      }
    });

    expect(wrapper.text()).toContain('新增用户');
    expect(wrapper.text()).toContain('通过抽屉维护用户信息');
    expect(wrapper.text()).not.toContain('System Form');
    expect(wrapper.find('.inner-form').exists()).toBe(true);
    expect(wrapper.find('.submit-btn').exists()).toBe(true);
  });

  it('emits close events when drawer requests close', async () => {
    const wrapper = mount(StandardFormDrawer, {
      props: {
        modelValue: true,
        title: '编辑角色'
      },
      global: {
        stubs: {
          'el-drawer': DrawerStub
        }
      }
    });

    await wrapper.find('.el-drawer-stub__close').trigger('click');

    expect(wrapper.emitted('update:modelValue')).toEqual([[false]]);
    expect(wrapper.emitted('close')).toEqual([[]]);
  });
});
