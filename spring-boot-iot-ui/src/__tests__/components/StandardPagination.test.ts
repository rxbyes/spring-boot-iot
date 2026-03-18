import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import StandardPagination from '@/components/StandardPagination.vue';

const ElPaginationStub = {
  name: 'ElPagination',
  props: ['currentPage', 'pageSize', 'total', 'pageSizes', 'layout', 'disabled'],
  emits: ['update:current-page', 'update:page-size', 'size-change', 'current-change'],
  template: `
    <div
      class="el-pagination-stub"
      :data-current-page="currentPage"
      :data-page-size="pageSize"
      :data-total="total"
      :data-layout="layout"
      :data-page-sizes="(pageSizes || []).join(',')"
      :data-disabled="disabled"
    >
      <button class="emit-page" type="button" @click="$emit('update:current-page', 3); $emit('current-change', 3)" />
      <button class="emit-size" type="button" @click="$emit('update:page-size', 20); $emit('size-change', 20)" />
    </div>
  `
};

describe('StandardPagination', () => {
  it('passes normalized props to el-pagination', () => {
    const wrapper = mount(StandardPagination, {
      props: {
        currentPage: 2,
        pageSize: 10,
        total: 88
      },
      global: {
        stubs: {
          ElPagination: ElPaginationStub
        }
      }
    });

    const stub = wrapper.find('.el-pagination-stub');
    expect(stub.exists()).toBe(true);
    expect(stub.attributes('data-current-page')).toBe('2');
    expect(stub.attributes('data-page-size')).toBe('10');
    expect(stub.attributes('data-total')).toBe('88');
    expect(stub.attributes('data-layout')).toBe('total, sizes, prev, pager, next, jumper');
    expect(stub.attributes('data-page-sizes')).toBe('10,20,50,100');
  });

  it('re-emits current page and page size change events', async () => {
    const wrapper = mount(StandardPagination, {
      props: {
        currentPage: 1,
        pageSize: 10,
        total: 30
      },
      global: {
        stubs: {
          ElPagination: ElPaginationStub
        }
      }
    });

    await wrapper.find('button.emit-page').trigger('click');
    await wrapper.find('button.emit-size').trigger('click');

    expect(wrapper.emitted('update:current-page')?.[0]).toEqual([3]);
    expect(wrapper.emitted('current-change')?.[0]).toEqual([3]);
    expect(wrapper.emitted('update:page-size')?.[0]).toEqual([20]);
    expect(wrapper.emitted('size-change')?.[0]).toEqual([20]);
  });
});
