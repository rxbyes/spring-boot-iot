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
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="file-debug-workbench-stub">
      <p>{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <slot name="filters" />
      <slot name="notices" />
      <slot name="inline-state" />
      <slot />
    </section>
  `
});

const IotAccessPageShellStub = defineComponent({
  name: 'IotAccessPageShell',
  props: ['breadcrumbs', 'title', 'showTitle'],
  template: `
    <section class="iot-access-page-shell-stub">
      <h1 v-if="showTitle !== false">{{ title }}</h1>
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

const StandardInlineStateStub = defineComponent({
  name: 'StandardInlineState',
  props: ['message'],
  template: '<div class="standard-inline-state-stub">{{ message }}</div>'
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

  it('renders the validation page inside the two-level access shell without the legacy eyebrow tier', () => {
    const wrapper = mount(FilePayloadDebugView, {
      global: {
        stubs: {
          IotAccessPageShell: IotAccessPageShellStub,
          StandardWorkbenchPanel: StandardWorkbenchPanelStub,
          StandardListFilterHeader: true,
          StandardInlineState: StandardInlineStateStub,
          StandardInfoGrid: true,
          PanelCard: PanelCardStub,
          EmptyState: true,
          ResponsePanel: ResponsePanelStub,
          StandardButton: true,
          ElInput: true
        }
      }
    });

    expect(wrapper.find('.iot-access-page-shell-stub').exists()).toBe(true);
    expect(wrapper.text()).toContain('数据校验台');
    expect(wrapper.text()).toContain('文件快照校验');
    expect(wrapper.text()).toContain('固件聚合校验');
    expect(wrapper.text()).toContain('文件快照原始响应');
    expect(wrapper.text()).toContain('固件聚合原始响应');
    expect(wrapper.text()).not.toContain('链路追踪台');
    expect(wrapper.text()).not.toContain('VALIDATION DESK');
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
          IotAccessPageShell: IotAccessPageShellStub,
          StandardWorkbenchPanel: StandardWorkbenchPanelStub,
          StandardListFilterHeader: true,
          StandardInlineState: StandardInlineStateStub,
          StandardInfoGrid: true,
          PanelCard: PanelCardStub,
          EmptyState: true,
          ResponsePanel: ResponsePanelStub,
          StandardButton: true,
          ElInput: true
        }
      }
    });

    expect(wrapper.text()).toContain('来自链路追踪台');
    expect(wrapper.text()).toContain('demo-device-02');
  });
});
