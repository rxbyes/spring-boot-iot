import { mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { describe, expect, it } from 'vitest';

import MessageTraceDetailWorkbench from '@/components/messageTrace/MessageTraceDetailWorkbench.vue';

const detailFixture = {
  id: 1,
  traceId: 'trace-acf4cb63b9c9472990a811b12e160735',
  deviceCode: 'demo-device-01',
  productKey: 'nf-monitor-crack-v1',
  messageType: 'report',
  topic: '/sys/nf-monitor-crack-v1/demo-device-01/thing/property/post',
  reportTime: '2026-04-12 14:20:00',
  createTime: '2026-04-12 14:20:05',
  protocolMetadata: {
    routeType: 'topic-route',
    normalizationStrategy: 'LEGACY_DP',
    childSplitApplied: true,
    templateEvidence: {
      templateCodes: ['crack_child_template'],
      executions: [
        {
          templateCode: 'crack_child_template',
          logicalChannelCode: 'L1_LF_1',
          childDeviceCode: '202018143',
          canonicalizationStrategy: 'LF_VALUE',
          statusMirrorApplied: true,
          parentRemovalKeys: ['L1_LF_1']
        }
      ]
    }
  }
};

function mountWorkbench() {
  return mount(MessageTraceDetailWorkbench, {
    props: {
      detail: detailFixture,
      panels: [],
      timeline: null,
      timelineLoading: false,
      timelineExpired: false,
      timelineLookupError: false,
      timelineEmptyTitle: '当前 trace 尚无可用时间线',
      timelineEmptyDescription: '正在等待时间线生成，或当前 trace 对应的 Redis 时间线不存在。'
    },
    global: {
      stubs: {
        MessageTracePayloadComparisonSection: defineComponent({
          name: 'MessageTracePayloadComparisonSection',
          template: '<section class="payload-comparison-stub">payload</section>'
        }),
        StandardTraceTimeline: defineComponent({
          name: 'StandardTraceTimeline',
          template: '<section class="trace-timeline-stub">timeline</section>'
        })
      }
    }
  });
}

describe('MessageTraceDetailWorkbench', () => {
  it('renders separate chain and access overview sections as a vertical ledger stack with full-width cards for long fields', () => {
    const wrapper = mountWorkbench();

    expect(wrapper.text()).toContain('链路概览');
    expect(wrapper.text()).toContain('接入概览');
    expect(wrapper.find('.message-trace-detail-workbench__ledger-stack').exists()).toBe(true);
    expect(wrapper.find('.message-trace-detail-workbench__overview-pair').exists()).toBe(false);
    expect(wrapper.findAll('.message-trace-detail-workbench__ledger-item--wide').length).toBeGreaterThanOrEqual(4);
  });

  it('renders template execution evidence as labeled multiline notes instead of dense pipe-joined text', () => {
    const wrapper = mountWorkbench();

    expect(wrapper.text()).toContain('模板编码：crack_child_template');
    expect(wrapper.text()).toContain('归一策略：LF_VALUE');
    expect(wrapper.text()).toContain('状态镜像：已应用');
    expect(wrapper.text()).toContain('父字段清理：L1_LF_1');
    expect(wrapper.text()).not.toContain('crack_child_template | 归一: LF_VALUE');
  });

  it('renders timeline summary as status cards before the full timeline is expanded', () => {
    const wrapper = mount(MessageTraceDetailWorkbench, {
      props: {
        detail: detailFixture,
        panels: [],
        timeline: {
          traceId: 'trace-acf4cb63b9c9472990a811b12e160735',
          sessionId: 'session-001',
          flowType: 'MQTT',
          status: 'COMPLETED',
          deviceCode: 'demo-device-01',
          productKey: 'nf-monitor-crack-v1',
          topic: '/sys/nf-monitor-crack-v1/demo-device-01/thing/property/post',
          protocolCode: 'mqtt-json',
          messageType: 'property',
          startedAt: '2026-04-12 14:20:00',
          finishedAt: '2026-04-12 14:20:01',
          totalCostMs: 90,
          steps: [
            {
              stage: 'INGRESS',
              handlerClass: 'UpMessageProcessingPipeline',
              handlerMethod: 'ingress',
              status: 'SUCCESS',
              costMs: 1,
              startedAt: '2026-04-12 14:20:00',
              finishedAt: '2026-04-12 14:20:00',
              summary: {},
              errorClass: '',
              errorMessage: '',
              branch: ''
            }
          ]
        },
        timelineLoading: false,
        timelineExpired: false,
        timelineLookupError: false,
        timelineEmptyTitle: '当前 trace 尚无可用时间线',
        timelineEmptyDescription: '正在等待时间线生成，或当前 trace 对应的 Redis 时间线不存在。'
      },
      global: {
        stubs: {
          MessageTracePayloadComparisonSection: defineComponent({
            name: 'MessageTracePayloadComparisonSection',
            template: '<section class="payload-comparison-stub">payload</section>'
          }),
          StandardTraceTimeline: defineComponent({
            name: 'StandardTraceTimeline',
            template: '<section class="trace-timeline-stub">timeline</section>'
          })
        }
      }
    });

    expect(wrapper.findAll('.message-trace-detail-workbench__timeline-summary').length).toBe(1);
    expect(wrapper.text()).toContain('当前状态');
    expect(wrapper.text()).toContain('处理节点');
    expect(wrapper.text()).toContain('Trace 归属');
    expect(wrapper.text()).toContain('存储提示');
    expect(wrapper.text()).toContain('可展开复盘');
    expect(wrapper.text()).toContain('1 个节点');
    expect(wrapper.text()).toContain('session-001');
  });
});
