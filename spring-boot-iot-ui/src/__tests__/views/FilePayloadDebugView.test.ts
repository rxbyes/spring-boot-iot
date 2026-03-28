import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import FilePayloadDebugView from '@/views/FilePayloadDebugView.vue';

const { mockRouter } = vi.hoisted(() => ({
  mockRouter: {
    push: vi.fn()
  }
}));

vi.mock('vue-router', () => ({
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
  props: ['title', 'description'],
  template: `
    <section class="file-debug-workbench-stub">
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

describe('FilePayloadDebugView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the compact validation strip and keeps the four-part result layout', () => {
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

    expect(wrapper.text()).toContain('数据校验台');
    expect(wrapper.text()).toContain('先确定设备，再核对文件快照和固件聚合。');
    expect(wrapper.text()).toContain('文件快照校验');
    expect(wrapper.text()).toContain('固件聚合校验');
    expect(wrapper.text()).toContain('文件快照原始响应');
    expect(wrapper.text()).toContain('固件聚合原始响应');
    expect(wrapper.text()).not.toContain('文件消息完整性概况');
  });
});
