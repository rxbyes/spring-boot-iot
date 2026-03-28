import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import IotAccessResultSection from '@/components/iotAccess/IotAccessResultSection.vue';

describe('IotAccessResultSection', () => {
  it('renders result header, toolbar and body within the shared section shell', () => {
    const wrapper = mount(IotAccessResultSection, {
      props: {
        title: '产品台账',
        description: '统一承接工具条、列表和分页。'
      },
      slots: {
        meta: '<div class="result-meta">当前 12 条</div>',
        toolbar: '<button type="button" class="result-toolbar">刷新</button>',
        default: '<div class="result-body">主结果区</div>'
      }
    });

    expect(wrapper.find('.iot-access-result-section').exists()).toBe(true);
    expect(wrapper.find('.iot-access-result-section__title').text()).toBe('产品台账');
    expect(wrapper.find('.result-meta').text()).toBe('当前 12 条');
    expect(wrapper.find('.result-toolbar').text()).toBe('刷新');
    expect(wrapper.find('.result-body').text()).toBe('主结果区');
  });
});
