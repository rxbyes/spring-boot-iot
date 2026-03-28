import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import IotAccessFilterBar from '@/components/iotAccess/IotAccessFilterBar.vue';

describe('IotAccessFilterBar', () => {
  it('renders slot content inside the shared filter bar shell', () => {
    const wrapper = mount(IotAccessFilterBar, {
      slots: {
        default: '<div class="filter-body">筛选项</div>',
        footer: '<div class="filter-footer">更多条件已生效</div>'
      }
    });

    expect(wrapper.find('.iot-access-filter-bar').exists()).toBe(true);
    expect(wrapper.find('.filter-body').text()).toBe('筛选项');
    expect(wrapper.find('.filter-footer').text()).toBe('更多条件已生效');
  });
});
