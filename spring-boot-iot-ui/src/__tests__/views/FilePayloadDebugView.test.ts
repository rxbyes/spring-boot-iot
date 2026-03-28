import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import FilePayloadDebugView from '@/views/FilePayloadDebugView.vue';

const { mockRoute, mockRouter } = vi.hoisted(() => ({
  mockRoute: {
    query: {} as Record<string, unknown>
  },
  mockRouter: {
    push: vi.fn()
  }
}));

vi.mock('vue-router', () => ({
  RouterLink: defineComponent({
    name: 'RouterLink',
    props: ['to'],
    template: '<a class="router-link-stub" :href="typeof to === \'string\' ? to : \'#\'"><slot /></a>'
  }),
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}));

vi.mock('@/api/iot', () => ({
  getDeviceFileSnapshots: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: [] }),
  getDeviceFirmwareAggregates: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: [] })
}));

vi.mock('@/stores/activity', () => ({
  recordActivity: vi.fn()
}));

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['title', 'description', 'titleVariant'],
  template: `
    <section class="file-debug-workbench-stub" :data-title-variant="titleVariant || 'default'">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <slot name="filters" />
      <slot name="inline-state" />
      <slot />
    </section>
  `
});

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="panel-card-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h3 v-if="title">{{ title }}</h3>
      <p v-if="description">{{ description }}</p>
      <slot />
    </section>
  `
});

const ResponsePanelStub = defineComponent({
  name: 'ResponsePanel',
  props: ['eyebrow', 'title'],
  template: `
    <section class="response-panel-stub">
      <p>{{ eyebrow }}</p>
      <h3>{{ title }}</h3>
    </section>
  `
});

function installSessionStorageMock(value?: Record<string, string>) {
  const store = new Map<string, string>(Object.entries(value || {}));
  Object.defineProperty(window, 'sessionStorage', {
    configurable: true,
    value: {
      getItem: vi.fn((key: string) => store.get(key) ?? null),
      setItem: vi.fn((key: string, next: string) => {
        store.set(key, next);
      }),
      removeItem: vi.fn((key: string) => {
        store.delete(key);
      })
    }
  });
}

describe('FilePayloadDebugView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockRoute.query = {};
    installSessionStorageMock();
  });

  it('keeps validation and raw-response tabs only', () => {
    const wrapper = mount(FilePayloadDebugView, {
      global: {
        stubs: {
          StandardWorkbenchPanel: StandardWorkbenchPanelStub,
          StandardListFilterHeader: defineComponent({
            name: 'StandardListFilterHeader',
            template: '<section class="file-debug-filter-header-stub"><slot name="primary" /><slot name="actions" /></section>'
          }),
          StandardInlineState: true,
          StandardInfoGrid: true,
          PanelCard: PanelCardStub,
          EmptyState: true,
          ResponsePanel: ResponsePanelStub,
          StandardButton: true,
          ElInput: true
        }
      }
    });

    expect(wrapper.find('.iot-access-page-shell').exists()).toBe(false);
    expect(wrapper.find('.iot-access-tab-workspace').exists()).toBe(true);
    expect(wrapper.find('.file-debug-workbench-stub').attributes('data-title-variant')).toBe('section');
    expect(wrapper.text()).not.toContain('数据校验台');
    expect(wrapper.find('.file-debug-workbench-stub h2').text()).toBe('设备校验');
    expect(wrapper.text()).toContain('设备校验');
    expect(wrapper.text()).toContain('原始响应');
    expect(wrapper.text()).not.toContain('链路追踪台');
    expect(wrapper.text()).not.toContain('设备查询与校验结果');
    expect(wrapper.text()).not.toContain('历史快照');
  });

  it('restores deviceCode from persisted diagnostic context', () => {
    installSessionStorageMock({
      'iot-access:diagnostic-context': JSON.stringify({
        storedAt: Date.now(),
        context: {
          sourcePage: 'message-trace',
          deviceCode: 'demo-device-02',
          traceId: 'trace-001',
          productKey: 'demo-product',
          capturedAt: new Date().toISOString()
        }
      })
    });
    mockRoute.query = { traceId: 'trace-001' };

    const wrapper = mount(FilePayloadDebugView, {
      global: {
        stubs: {
          StandardWorkbenchPanel: StandardWorkbenchPanelStub,
          StandardListFilterHeader: true,
          StandardInlineState: true,
          StandardInfoGrid: true,
          PanelCard: PanelCardStub,
          EmptyState: true,
          ResponsePanel: ResponsePanelStub,
          StandardButton: true,
          ElInput: true
        }
      }
    });

    expect((wrapper.vm as { deviceCode: string }).deviceCode).toBe('demo-device-02');
  });

  it('uses the refined minimal diagnostic-shell classes for file validation', () => {
    const wrapper = mount(FilePayloadDebugView, {
      global: {
        stubs: {
          StandardWorkbenchPanel: StandardWorkbenchPanelStub,
          StandardListFilterHeader: true,
          StandardInlineState: true,
          StandardInfoGrid: true,
          PanelCard: PanelCardStub,
          EmptyState: true,
          ResponsePanel: ResponsePanelStub,
          StandardButton: true,
          ElInput: true
        }
      }
    });

    expect(wrapper.classes()).toContain('file-payload-debug-view--minimal');
  });
});
