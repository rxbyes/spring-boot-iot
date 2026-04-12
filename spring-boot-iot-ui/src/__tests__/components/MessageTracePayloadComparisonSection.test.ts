import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import MessageTracePayloadComparisonSection from '@/components/messageTrace/MessageTracePayloadComparisonSection.vue';

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    warning: vi.fn()
  }
}));

const panels = [
  {
    key: 'raw',
    title: '原始 Payload',
    description: '保留消息日志中的原始报文。',
    content: '{\"cipher\":true}',
    emptyText: '当前无原始 Payload',
    available: true
  },
  {
    key: 'decrypted',
    title: '解密后明文',
    description: '展示协议解码阶段拿到的明文快照。',
    content: '',
    emptyText: '当前无解密后明文',
    available: false
  },
  {
    key: 'decoded',
    title: '解析结果',
    description: '展示归一后的结构化内容。',
    content: '{\"deviceCode\":\"demo-device-01\"}',
    emptyText: '当前无解析结果',
    available: true
  }
] as const;

describe('MessageTracePayloadComparisonSection', () => {
  beforeEach(() => {
    Object.defineProperty(window.navigator, 'clipboard', {
      configurable: true,
      value: {
        writeText: vi.fn().mockResolvedValue(undefined)
      }
    });
  });

  it('renders payload panels as readable detail cards with status and description visible before expansion', () => {
    const wrapper = mount(MessageTracePayloadComparisonSection, {
      props: {
        panels
      }
    });

    expect(wrapper.findAll('.detail-card').length).toBe(3);
    expect(wrapper.text()).toContain('当前状态');
    expect(wrapper.text()).toContain('已恢复');
    expect(wrapper.text()).toContain('暂缺');
    expect(wrapper.text()).toContain('保留消息日志中的原始报文。');
    expect(wrapper.text()).toContain('展示协议解码阶段拿到的明文快照。');
  });

  it('keeps code content collapsed by default and reveals it inside the same detail card after expansion', async () => {
    const wrapper = mount(MessageTracePayloadComparisonSection, {
      props: {
        panels
      }
    });

    expect(wrapper.text()).not.toContain('{\"cipher\":true}');

    await wrapper.find('[data-testid="message-trace-payload-toggle-raw"]').trigger('click');

    expect(wrapper.text()).toContain('{\"cipher\":true}');
    expect(wrapper.find('.message-trace-payload-comparison__code').exists()).toBe(true);
  });
});
