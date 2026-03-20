import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import EmptyState from '@/components/EmptyState.vue';

describe('EmptyState', () => {
      it('renders with default props', () => {
            const wrapper = mount(EmptyState);
            expect(wrapper.find('.empty-state').exists()).toBe(true);
            expect(wrapper.find('.empty-state__icon').exists()).toBe(true);
            expect(wrapper.find('.empty-state__title').exists()).toBe(true);
            expect(wrapper.find('.empty-state__description').exists()).toBe(true);
      });

      it('renders with custom title and description', () => {
            const wrapper = mount(EmptyState, {
                  props: {
                        title: '自定义标题',
                        description: '自定义描述'
                  }
            });
            expect(wrapper.find('.empty-state__title').text()).toBe('自定义标题');
            expect(wrapper.find('.empty-state__description').text()).toBe('自定义描述');
      });

      it('renders action button when action is provided', () => {
            const wrapper = mount(EmptyState, {
                  props: {
                        title: '测试标题',
                        description: '测试描述',
                        action: {
                              label: '测试按钮',
                              callback: () => { }
                        }
                  }
            });
            expect(wrapper.find('.empty-state__action').exists()).toBe(true);
            expect(wrapper.find('button').exists()).toBe(true);
            expect(wrapper.find('button').text()).toBe('测试按钮');
      });

      it('calls callback when action button is clicked', async () => {
            const callback = vi.fn();
            const wrapper = mount(EmptyState, {
                  props: {
                        title: '测试标题',
                        description: '测试描述',
                        action: {
                              label: '测试按钮',
                              callback
                        }
                  }
            });
            await wrapper.find('button').trigger('click');
            expect(callback).toHaveBeenCalledTimes(1);
      });
});
