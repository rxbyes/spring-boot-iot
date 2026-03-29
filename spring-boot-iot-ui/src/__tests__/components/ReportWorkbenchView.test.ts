import { defineComponent, nextTick } from 'vue';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import ReportWorkbenchView from '@/views/ReportWorkbenchView.vue';
import { getDeviceByCode, reportByHttp, reportByMqtt } from '@/api/iot';
import { messageApi } from '@/api/message';

const { mockRouter, mockRoute } = vi.hoisted(() => ({
  mockRouter: {
    push: vi.fn(),
    replace: vi.fn()
  },
  mockRoute: {
    query: {}
  }
}));

vi.mock('vue-router', () => ({
  useRouter: () => mockRouter,
  useRoute: () => mockRoute
}));

vi.mock('@/api/iot', () => ({
  getDeviceByCode: vi.fn(),
  reportByHttp: vi.fn(),
  reportByMqtt: vi.fn()
}));

vi.mock('@/api/message', () => ({
  messageApi: {
    getMessageFlowSession: vi.fn(),
    getMessageFlowTrace: vi.fn(),
    getMessageFlowRecentSessions: vi.fn()
  }
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

const StandardTraceTimelineStub = defineComponent({
  name: 'StandardTraceTimeline',
  props: ['timeline', 'loading', 'emptyTitle', 'emptyDescription'],
  template: `
    <section class="standard-trace-timeline-stub">
      <p v-if="loading">loading</p>
      <template v-else-if="timeline">
        <strong>{{ timeline.traceId }}</strong>
        <span v-for="step in timeline.steps" :key="step.stage">{{ step.stage }}</span>
      </template>
      <template v-else>
        <strong>{{ emptyTitle }}</strong>
        <p>{{ emptyDescription }}</p>
      </template>
    </section>
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
  Object.defineProperty(window, 'sessionStorage', {
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
        IotAccessPageShell: IotAccessPageShellStub,
        ElButton: ElButtonStub,
        ElInput: ElInputStub,
        PanelCard: PanelCardStub,
        StandardInfoGrid: StandardInfoGridStub,
        StandardActionGroup: StandardActionGroupStub,
        StandardFlowRail: StandardFlowRailStub,
        StandardInlineSectionHeader: StandardInlineSectionHeaderStub,
        StandardTraceTimeline: StandardTraceTimelineStub
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
    vi.useRealTimers();
    vi.mocked(getDeviceByCode).mockReset();
    vi.mocked(reportByHttp).mockReset();
    vi.mocked(reportByMqtt).mockReset();
    vi.mocked(messageApi.getMessageFlowSession).mockReset();
    vi.mocked(messageApi.getMessageFlowRecentSessions).mockReset();
    vi.mocked(messageApi.getMessageFlowRecentSessions).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    });
    mockRouter.push.mockReset();
    mockRouter.replace.mockReset();
    mockRoute.query = {};
    installLocalStorageMock();
    window.localStorage.removeItem('reporting:lastTemplate');
  });

  it('renders the reporting page inside the two-level access shell', () => {
    const wrapper = mountView();

    expect(wrapper.find('.iot-access-page-shell-stub').exists()).toBe(true);
    expect(wrapper.text()).toContain('链路验证中心');
    expect(wrapper.text()).toContain('SIMULATION LAB');
    expect(wrapper.text()).toContain('模拟上报');
  });

  it('defaults to replay tab and renders the diagnosis header', () => {
    const wrapper = mountView();

    expect(wrapper.find('[aria-current="page"]').text()).toContain('结果复盘');
    expect(wrapper.text()).toContain('当前尚未验证');
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

  it('shows the timeline immediately after a successful http report', async () => {
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
      data: {
        sessionId: 'session-http-001',
        traceId: 'trace-http-001',
        status: 'COMPLETED',
        timelineAvailable: true,
        correlationPending: false
      }
    });
    vi.mocked(messageApi.getMessageFlowSession).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        sessionId: 'session-http-001',
        transportMode: 'HTTP',
        status: 'COMPLETED',
        submittedAt: '2026-03-23 10:00:00',
        traceId: 'trace-http-001',
        deviceCode: 'demo-device-01',
        topic: '/message/http/report',
        correlationPending: false,
        timeline: {
          traceId: 'trace-http-001',
          sessionId: 'session-http-001',
          flowType: 'HTTP',
          status: 'COMPLETED',
          deviceCode: 'demo-device-01',
          productKey: 'demo-product',
          topic: '/message/http/report',
          protocolCode: 'mqtt-json',
          messageType: 'property',
          startedAt: '2026-03-23 10:00:00',
          finishedAt: '2026-03-23 10:00:01',
          totalCostMs: 88,
          steps: [
            {
              stage: 'INGRESS',
              handlerClass: 'UpMessageProcessingPipeline',
              handlerMethod: 'ingress',
              status: 'SUCCESS',
              costMs: 1,
              startedAt: '2026-03-23 10:00:00',
              finishedAt: '2026-03-23 10:00:00',
              summary: {},
              errorClass: '',
              errorMessage: '',
              branch: ''
            }
          ]
        }
      }
    });

    const wrapper = mountView();
    await queryDevice(wrapper);
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();
    await nextTick();

    expect(messageApi.getMessageFlowSession).toHaveBeenCalledWith('session-http-001');
    expect(wrapper.text()).toContain('处理时间线已就绪，可直接查看阶段顺序，或跳转链路追踪台继续联动排查。');
    expect(wrapper.text()).toContain('trace-http-001');
    expect(wrapper.text()).toContain('INGRESS');
  });

  it('shows a Redis or TTL hint when http submit expects timeline but session lookup returns empty', async () => {
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
      data: {
        sessionId: 'session-http-empty',
        traceId: 'trace-http-empty',
        status: 'COMPLETED',
        timelineAvailable: true,
        correlationPending: false
      }
    });
    vi.mocked(messageApi.getMessageFlowSession).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: null
    });

    const wrapper = mountView();
    await queryDevice(wrapper);
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).toContain('时间线不可用，优先排查 Redis/TTL。');
    expect(wrapper.text()).toContain('HTTP 提交已返回 timelineAvailable=true');
    expect(wrapper.text()).toContain('已拿到 trace，可进入链路追踪');

    const traceButton = findButtonByText(wrapper, '继续链路追踪');
    expect(traceButton).toBeTruthy();
    await traceButton!.trigger('click');

    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/message-trace',
      query: {
        deviceCode: 'demo-device-01',
        traceId: 'trace-http-empty',
        productKey: 'demo-product',
        topic: '$dp'
      }
    });
    const persisted = vi.mocked(window.sessionStorage.setItem).mock.calls
      .filter((call) => call[0] === 'iot-access:diagnostic-context')
      .at(-1);
    expect(persisted).toBeTruthy();
    const persistedPayload = JSON.parse((persisted?.[1] as string) || '{}');
    expect(persistedPayload.context).toMatchObject({
      traceId: 'trace-http-empty',
      transportMode: 'http'
    });
  });

  it('polls the mqtt session until the trace is bound', async () => {
    const recentSubmittedAt = new Date(Date.now() - 30_000).toISOString();
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
    vi.mocked(reportByMqtt).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        sessionId: 'session-mqtt-001',
        status: 'PUBLISHED',
        timelineAvailable: false,
        correlationPending: true
      }
    });
    vi.mocked(messageApi.getMessageFlowSession)
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: {
          sessionId: 'session-mqtt-001',
          transportMode: 'MQTT',
          status: 'PUBLISHED',
          submittedAt: recentSubmittedAt,
          traceId: '',
          deviceCode: 'demo-device-01',
          topic: '$dp',
          correlationPending: true,
          timeline: null
        }
      })
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: {
          sessionId: 'session-mqtt-001',
          transportMode: 'MQTT',
          status: 'COMPLETED',
          submittedAt: recentSubmittedAt,
          traceId: 'trace-mqtt-001',
          deviceCode: 'demo-device-01',
          topic: '$dp',
          correlationPending: false,
          timeline: {
            traceId: 'trace-mqtt-001',
            sessionId: 'session-mqtt-001',
            flowType: 'MQTT',
            status: 'COMPLETED',
            deviceCode: 'demo-device-01',
            productKey: 'demo-product',
            topic: '$dp',
            protocolCode: 'mqtt-json',
            messageType: 'property',
            startedAt: '2026-03-23 10:05:00',
            finishedAt: '2026-03-23 10:05:01',
            totalCostMs: 96,
            steps: [
              {
                stage: 'PROTOCOL_DECODE',
                handlerClass: 'MqttJsonProtocolAdapter',
                handlerMethod: 'decode',
                status: 'SUCCESS',
                costMs: 12,
                startedAt: '2026-03-23 10:05:00',
                finishedAt: '2026-03-23 10:05:00',
                summary: {},
                errorClass: '',
                errorMessage: '',
                branch: ''
              }
            ]
          }
        }
    });

    const wrapper = mountView();
    await queryDevice(wrapper);
    await findButtonByText(wrapper, 'MQTT')!.trigger('click');
    await nextTick();
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).toContain('MQTT 模拟已发布，正在等待消费回流绑定 traceId。');
    expect(messageApi.getMessageFlowSession).toHaveBeenCalledTimes(1);

    await new Promise((resolve) => setTimeout(resolve, 1600));
    await nextTick();

    expect(messageApi.getMessageFlowSession).toHaveBeenCalledTimes(2);
    expect(wrapper.text()).toContain('trace-mqtt-001');
    expect(wrapper.text()).toContain('PROTOCOL_DECODE');
  });

  it('shows correlation miss when mqtt pending session exceeds the match window', async () => {
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
    vi.mocked(reportByMqtt).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        sessionId: 'session-mqtt-timeout',
        status: 'PUBLISHED',
        timelineAvailable: false,
        correlationPending: true
      }
    });
    vi.mocked(messageApi.getMessageFlowSession).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        sessionId: 'session-mqtt-timeout',
        transportMode: 'MQTT',
        status: 'PUBLISHED',
        submittedAt: '2024-03-23T10:05:00',
        traceId: '',
        deviceCode: 'demo-device-01',
        topic: '$dp',
        correlationPending: true,
        timeline: null
      }
    });

    const wrapper = mountView();
    await queryDevice(wrapper);
    await findButtonByText(wrapper, 'MQTT')!.trigger('click');
    await nextTick();
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).toContain('MQTT 模拟已超出关联窗口，判定为未命中消费回流关联。');
    expect(wrapper.text()).toContain('MQTT 模拟发布已超过 120 秒匹配窗口');
  });

  it('persists diagnostic context and surfaces trace-ready summary after successful send', async () => {
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
      data: {
        sessionId: 'session-http-ctx-001',
        traceId: 'trace-http-ctx-001',
        status: 'COMPLETED',
        timelineAvailable: true,
        correlationPending: false
      }
    });
    vi.mocked(messageApi.getMessageFlowSession).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        sessionId: 'session-http-ctx-001',
        transportMode: 'HTTP',
        status: 'COMPLETED',
        submittedAt: '2026-03-23 10:00:00',
        traceId: 'trace-http-ctx-001',
        deviceCode: 'demo-device-01',
        topic: '$dp',
        correlationPending: false,
        timeline: null
      }
    });

    const wrapper = mountView();
    await queryDevice(wrapper);
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();
    await nextTick();

    expect(window.sessionStorage.setItem).toHaveBeenCalled();
    const persisted = vi.mocked(window.sessionStorage.setItem).mock.calls.find(
      (call) => call[0] === 'iot-access:diagnostic-context'
    );
    expect(persisted).toBeTruthy();
    const persistedPayload = JSON.parse((persisted?.[1] as string) || '{}');
    expect(persistedPayload.context).toMatchObject({
      sourcePage: 'reporting',
      deviceCode: 'demo-device-01',
      traceId: 'trace-http-ctx-001',
      productKey: 'demo-product',
      topic: '$dp',
      sessionId: 'session-http-ctx-001',
      transportMode: 'http'
    });
    expect(wrapper.text()).toContain('已拿到 trace，可进入链路追踪');
    expect(wrapper.find('.reporting-diagnostic-links').exists()).toBe(false);
  });

  it('forwards current diagnostic query when continuing to message trace', async () => {
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
      data: {
        sessionId: 'session-http-trace-jump',
        traceId: 'trace-http-trace-jump',
        status: 'COMPLETED',
        timelineAvailable: true,
        correlationPending: false
      }
    });
    vi.mocked(messageApi.getMessageFlowSession).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        sessionId: 'session-http-trace-jump',
        transportMode: 'HTTP',
        status: 'COMPLETED',
        submittedAt: '2026-03-23 10:00:00',
        traceId: 'trace-http-trace-jump',
        deviceCode: 'demo-device-01',
        topic: '$dp',
        correlationPending: false,
        timeline: null
      }
    });

    const wrapper = mountView();
    await queryDevice(wrapper);
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();
    await nextTick();

    const traceButton = findButtonByText(wrapper, '继续链路追踪');
    expect(traceButton).toBeTruthy();
    await traceButton!.trigger('click');

    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/message-trace',
      query: {
        deviceCode: 'demo-device-01',
        traceId: 'trace-http-trace-jump',
        productKey: 'demo-product',
        topic: '$dp'
      }
    });
  });

  it('uses HTTP request method when jumping to system log from http diagnostic context', async () => {
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
      data: {
        sessionId: 'session-http-log-jump',
        traceId: 'trace-http-log-jump',
        status: 'COMPLETED',
        timelineAvailable: true,
        correlationPending: false
      }
    });
    vi.mocked(messageApi.getMessageFlowSession).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        sessionId: 'session-http-log-jump',
        transportMode: 'HTTP',
        status: 'COMPLETED',
        submittedAt: '2026-03-23 10:00:00',
        traceId: 'trace-http-log-jump',
        deviceCode: 'demo-device-01',
        topic: '/message/http/report',
        correlationPending: false,
        timeline: null
      }
    });

    const wrapper = mountView();
    await queryDevice(wrapper);
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();
    await nextTick();

    const systemLogButton = findButtonByText(wrapper, '查看异常观测');
    expect(systemLogButton).toBeTruthy();
    await systemLogButton!.trigger('click');

    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/system-log',
      query: {
        deviceCode: 'demo-device-01',
        traceId: 'trace-http-log-jump',
        productKey: 'demo-product',
        topic: '/message/http/report',
        requestUrl: '/message/http/report',
        requestMethod: 'HTTP'
      }
    });
  });

  it('restores recent session without device lookup and keeps trace-ready context for handoff and persistence', async () => {
    vi.mocked(messageApi.getMessageFlowRecentSessions).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          sessionId: 'session-restore-001',
          transportMode: 'MQTT',
          status: 'COMPLETED',
          submittedAt: '2026-03-23 10:00:00',
          traceId: 'trace-restore-001',
          deviceCode: 'restore-device-01',
          topic: '$dp/restore',
          correlationPending: false,
          timelineAvailable: true
        }
      ]
    });
    vi.mocked(messageApi.getMessageFlowSession).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        sessionId: 'session-restore-001',
        transportMode: 'MQTT',
        status: 'COMPLETED',
        submittedAt: '2026-03-23 10:00:00',
        traceId: 'trace-restore-001',
        deviceCode: 'restore-device-01',
        topic: '$dp/restore',
        correlationPending: false,
        timeline: null
      }
    });

    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    const restoreButton = findButtonByText(wrapper, 'session-restore-001');
    expect(restoreButton).toBeTruthy();
    await restoreButton!.trigger('click');
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).toContain('已拿到 trace，可进入链路追踪');

    const traceButton = findButtonByText(wrapper, '继续链路追踪');
    expect(traceButton).toBeTruthy();
    await traceButton!.trigger('click');

    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/message-trace',
      query: {
        deviceCode: 'restore-device-01',
        traceId: 'trace-restore-001',
        topic: '$dp/restore'
      }
    });

    const persisted = vi.mocked(window.sessionStorage.setItem).mock.calls
      .filter((call) => call[0] === 'iot-access:diagnostic-context')
      .at(-1);
    expect(persisted).toBeTruthy();
    const persistedPayload = JSON.parse((persisted?.[1] as string) || '{}');
    expect(persistedPayload.context).toMatchObject({
      sourcePage: 'reporting',
      deviceCode: 'restore-device-01',
      traceId: 'trace-restore-001',
      topic: '$dp/restore',
      sessionId: 'session-restore-001',
      transportMode: 'mqtt'
    });
  });

  it('keeps restore baseline for mqtt pending when follow-up session is partial', async () => {
    vi.mocked(messageApi.getMessageFlowRecentSessions).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          sessionId: 'session-restore-pending-001',
          transportMode: 'MQTT',
          status: 'PUBLISHED',
          submittedAt: new Date().toISOString(),
          traceId: '',
          deviceCode: 'restore-pending-device-01',
          topic: '$dp/pending',
          correlationPending: true,
          timelineAvailable: false
        }
      ]
    });
    vi.mocked(messageApi.getMessageFlowSession).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        sessionId: 'session-restore-pending-001',
        status: 'PUBLISHED',
        submittedAt: new Date().toISOString(),
        traceId: '',
        deviceCode: '',
        topic: '',
        correlationPending: true,
        timeline: null
      }
    });

    const wrapper = mountView();
    await flushPromises();
    await nextTick();

    const restoreButton = findButtonByText(wrapper, 'session-restore-pending-001');
    expect(restoreButton).toBeTruthy();
    await restoreButton!.trigger('click');
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).toContain('MQTT 已发布，等待消费回流');

    const logButton = findButtonByText(wrapper, '查看异常观测');
    expect(logButton).toBeTruthy();
    await logButton!.trigger('click');
    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/system-log',
      query: {
        deviceCode: 'restore-pending-device-01',
        topic: '$dp/pending',
        requestUrl: '$dp/pending',
        requestMethod: 'MQTT'
      }
    });

    const persisted = vi.mocked(window.sessionStorage.setItem).mock.calls
      .filter((call) => call[0] === 'iot-access:diagnostic-context')
      .at(-1);
    expect(persisted).toBeTruthy();
    const persistedPayload = JSON.parse((persisted?.[1] as string) || '{}');
    expect(persistedPayload.context).toMatchObject({
      sourcePage: 'reporting',
      deviceCode: 'restore-pending-device-01',
      topic: '$dp/pending',
      sessionId: 'session-restore-pending-001',
      transportMode: 'mqtt',
      reportStatus: 'pending'
    });
  });
});
