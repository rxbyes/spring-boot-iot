import { defineComponent, nextTick } from 'vue';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import ReportWorkbenchView from '@/views/ReportWorkbenchView.vue';
import { getDeviceByCode, reportByHttp, reportByMqtt } from '@/api/iot';

vi.mock('@/api/iot', () => ({
  getDeviceByCode: vi.fn(),
  reportByHttp: vi.fn(),
  reportByMqtt: vi.fn()
}));

vi.mock('@/stores/activity', () => ({
  recordActivity: vi.fn()
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

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0));

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
  props: ['modelValue', 'type', 'id', 'disabled'],
  emits: ['update:modelValue'],
  template: `
    <textarea
      v-if="type === 'textarea'"
      :id="id"
      class="el-input-stub-textarea"
      :value="modelValue"
      :disabled="Boolean(disabled)"
      @input="$emit('update:modelValue', $event.target.value)"
    />
    <input
      v-else
      :id="id"
      class="el-input-stub-input"
      :value="modelValue"
      :disabled="Boolean(disabled)"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  `
});

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="panel-card-stub">
      <header class="panel-card-stub__header">
        <slot name="header" />
        <template v-if="!$slots.header">
          <p v-if="eyebrow">{{ eyebrow }}</p>
          <h2 v-if="title">{{ title }}</h2>
          <p v-if="description">{{ description }}</p>
        </template>
      </header>
      <div class="panel-card-stub__body">
        <slot />
      </div>
    </section>
  `
});

const StandardInlineSectionHeaderStub = defineComponent({
  name: 'StandardInlineSectionHeader',
  props: ['title', 'description'],
  template: `
    <header class="section-header-stub">
      <h3>{{ title }}</h3>
      <p>{{ description }}</p>
      <div class="section-header-stub__actions">
        <slot name="actions" />
      </div>
    </header>
  `
});

const StandardInfoGridStub = defineComponent({
  name: 'StandardInfoGrid',
  props: ['items'],
  template: `
    <dl class="standard-info-grid-stub">
      <template v-for="item in items" :key="item.key">
        <dt>{{ item.label }}</dt>
        <dd>{{ item.value || item.fallback || '--' }}</dd>
      </template>
    </dl>
  `
});

const StandardActionGroupStub = defineComponent({
  name: 'StandardActionGroup',
  template: '<div class="standard-action-group-stub"><slot /></div>'
});

const StandardFlowRailStub = defineComponent({
  name: 'StandardFlowRail',
  props: ['items'],
  template: `
    <ol class="standard-flow-rail-stub">
      <li v-for="item in items" :key="item.index">{{ item.title }}</li>
    </ol>
  `
});

function installLocalStorageMock() {
  Object.defineProperty(window, 'localStorage', {
    configurable: true,
    value: {
      getItem: vi.fn(() => null),
      setItem: vi.fn(),
      removeItem: vi.fn()
    }
  });
}

function mountView() {
  return mount(ReportWorkbenchView, {
    global: {
      stubs: {
        ElButton: ElButtonStub,
        ElInput: ElInputStub,
        PanelCard: PanelCardStub,
        StandardInfoGrid: StandardInfoGridStub,
        StandardActionGroup: StandardActionGroupStub,
        StandardFlowRail: StandardFlowRailStub,
        StandardInlineSectionHeader: StandardInlineSectionHeaderStub
      }
    }
  });
}

function findButtonByText(wrapper: ReturnType<typeof mountView>, text: string) {
  return wrapper.findAll('button').find((button) => button.text().includes(text));
}

async function queryDevice(wrapper: ReturnType<typeof mountView>, deviceCode = 'demo-device-01') {
  await wrapper.find('#report-device-code').setValue(deviceCode);
  const queryButton = findButtonByText(wrapper, '查询设备');
  expect(queryButton).toBeTruthy();
  await queryButton!.trigger('click');
  await flushPromises();
  await nextTick();
}

describe('ReportWorkbenchView', () => {
  beforeEach(() => {
    vi.mocked(getDeviceByCode).mockReset();
    vi.mocked(reportByHttp).mockReset();
    vi.mocked(reportByMqtt).mockReset();
    installLocalStorageMock();
    window.localStorage.removeItem('reporting:lastTemplate');
  });

  it('keeps a neutral initial state and only shows validation feedback after submit', async () => {
    const wrapper = mountView();

    expect(wrapper.text()).toContain('请输入设备编码后点击“查询设备”，加载产品 Key、协议编码和客户端 ID。');
    expect(wrapper.text()).not.toContain('发送前请修复以下问题');
    expect(findButtonByText(wrapper, '套用推荐')?.attributes('disabled')).toBeDefined();
    expect(findButtonByText(wrapper, '查询设备')?.attributes('disabled')).toBeDefined();

    await wrapper.find('form').trigger('submit.prevent');
    await nextTick();

    expect(wrapper.text()).toContain('发送前请修复以下问题');
    expect(wrapper.text()).toContain('协议编码不能为空。');
    expect(wrapper.text()).toContain('产品 Key 不能为空。');
    expect(wrapper.text()).toContain('设备编码不能为空。');
    expect(reportByHttp).not.toHaveBeenCalled();
    expect(reportByMqtt).not.toHaveBeenCalled();
  });

  it('loads the device contract summary only after an explicit query', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 1,
        deviceCode: 'demo-device-01',
        deviceName: '演示设备',
        productKey: 'demo-product',
        protocolCode: 'mqtt-json'
      }
    });

    const wrapper = mountView();

    await queryDevice(wrapper);

    expect(getDeviceByCode).toHaveBeenCalledWith('demo-device-01');
    expect(wrapper.text()).toContain('已加载设备接入契约，可继续配置 Topic、模式与 payload。');
    expect(wrapper.text()).toContain('demo-product');
    expect(wrapper.text()).toContain('mqtt-json');
    expect(wrapper.text()).toContain('demo-device-01');
    expect(wrapper.text()).toContain('演示设备');
    expect(findButtonByText(wrapper, '套用推荐')?.attributes('disabled')).toBeUndefined();
  });

  it('switches transport and report mode while keeping the diagnostic summary in sync', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 1,
        deviceCode: 'demo-device-01',
        deviceName: '演示设备',
        productKey: 'demo-product',
        protocolCode: 'mqtt-json'
      }
    });

    const wrapper = mountView();
    await queryDevice(wrapper);

    await findButtonByText(wrapper, 'MQTT')!.trigger('click');
    await findButtonByText(wrapper, '密文')!.trigger('click');
    await nextTick();

    expect(wrapper.text()).toContain('MQTT 模拟');
    expect(wrapper.text()).toContain('密文透传');
    expect(wrapper.text()).toContain('实际发送封包');
    expect(wrapper.text()).toContain('当前 Topic');
  });

  it('submits valid input and keeps response and actual payload preview aligned', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 1,
        deviceCode: 'demo-device-01',
        deviceName: '演示设备',
        productKey: 'demo-product',
        protocolCode: 'mqtt-json'
      }
    });
    vi.mocked(reportByHttp).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: null
    });

    const wrapper = mountView();
    await queryDevice(wrapper);

    expect(wrapper.text()).toContain('实际发送 JSON');
    expect(wrapper.text()).toContain('"messageType": "property"');

    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();
    await nextTick();

    expect(reportByHttp).toHaveBeenCalledTimes(1);
    const payload = vi.mocked(reportByHttp).mock.calls[0]?.[0];
    expect(payload?.deviceCode).toBe('demo-device-01');
    expect(payload?.productKey).toBe('demo-product');
    expect(payload?.protocolCode).toBe('mqtt-json');
    expect(payload?.payloadEncoding).toBe('ISO-8859-1');
    expect(typeof payload?.payload).toBe('string');
    expect(payload?.payload.length).toBeGreaterThan(0);
    expect(wrapper.text()).toContain('"msg": "success"');
  });
});
