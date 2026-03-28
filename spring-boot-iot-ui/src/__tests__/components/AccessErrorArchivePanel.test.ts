import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import AccessErrorArchivePanel from '@/components/AccessErrorArchivePanel.vue';
import { accessErrorApi } from '@/api/accessError';

const { mockRoute, mockRouter } = vi.hoisted(() => ({
  mockRoute: {
    query: {
      mode: 'access-error',
      traceId: 'trace-001',
      deviceCode: 'demo-device-01',
      productKey: 'demo-product'
    } as Record<string, unknown>
  },
  mockRouter: {
    push: vi.fn()
  }
}));

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}));

vi.mock('@/api/accessError', () => ({
  accessErrorApi: {
    pageAccessErrors: vi.fn(),
    getAccessErrorStats: vi.fn(),
    getAccessErrorById: vi.fn()
  }
}));

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  template: '<section><slot name="filters" /><slot name="toolbar" /><slot /><slot name="pagination" /></section>'
});

describe('AccessErrorArchivePanel', () => {
  beforeEach(() => {
    mockRouter.push.mockReset();
    vi.mocked(accessErrorApi.pageAccessErrors).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1,
            traceId: 'trace-001',
            deviceCode: 'demo-device-01',
            productKey: 'demo-product',
            topic: '$dp',
            errorMessage: 'contract mismatch',
            createTime: '2026-03-28 10:00:00'
          }
        ]
      }
    });
    vi.mocked(accessErrorApi.getAccessErrorStats).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        recentHourCount: 1,
        recent24HourCount: 1,
        distinctTraceCount: 1,
        distinctDeviceCount: 1,
        topFailureStages: [],
        topErrorCodes: [],
        topExceptionClasses: [],
        topProtocolCodes: [],
        topTopics: []
      }
    });
    vi.mocked(accessErrorApi.getAccessErrorById).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 1,
        traceId: 'trace-001',
        deviceCode: 'demo-device-01',
        productKey: 'demo-product',
        contractSnapshot: '{"expected":"demo-product"}'
      }
    });
  });

  it('offers governance jump actions from failure detail', async () => {
    const wrapper = mount(AccessErrorArchivePanel, {
      props: {
        viewMode: 'access-error',
        viewModeOptions: [
          { label: '链路追踪', value: 'message-trace' },
          { label: '失败归档', value: 'access-error' }
        ]
      },
      global: {
        stubs: {
          StandardWorkbenchPanel: StandardWorkbenchPanelStub,
          StandardListFilterHeader: true,
          StandardAppliedFiltersBar: true,
          StandardTableToolbar: true,
          StandardPagination: true,
          StandardTableTextColumn: true,
          StandardRowActions: true,
          StandardActionLink: true,
          StandardDetailDrawer: defineComponent({
            name: 'StandardDetailDrawer',
            props: ['modelValue'],
            template: '<section v-if="modelValue"><slot /></section>'
          }),
          StandardChoiceGroup: true,
          StandardButton: defineComponent({
            name: 'StandardButton',
            emits: ['click'],
            template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
          }),
          ElTable: true,
          ElTableColumn: true,
          ElInput: true,
          ElFormItem: true,
          ElTag: true,
          ElAlert: true
        }
      }
    });

    await (wrapper.vm as any).handleDetail({
      id: 1,
      traceId: 'trace-001',
      deviceCode: 'demo-device-01',
      productKey: 'demo-product'
    });
    await (wrapper.vm as any).jumpToProductGovernance();

    expect(mockRouter.push).toHaveBeenLastCalledWith({
      path: '/products',
      query: {
        productKey: 'demo-product',
        traceId: 'trace-001'
      }
    });
  });
});
