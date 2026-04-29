import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import RiskObjectSituationPanel from '@/components/RiskObjectSituationPanel.vue';

describe('RiskObjectSituationPanel', () => {
  it('renders risk object situation evidence from nested JSON notes', () => {
    const wrapper = mount(RiskObjectSituationPanel, {
      props: {
        source: JSON.stringify({
          riskObjectSituation: {
            reasonCode: 'SINGLE_SIGNAL_ONLY',
            triggerResponse: false,
            totalBindingCount: 3,
            activeSignalCount: 1,
            responseLevel: 'WATCH',
            activeSignals: [
              {
                deviceName: '裂缝计 A',
                metricName: '裂缝位移',
                identifier: 'value',
                currentValue: 12.4,
                thresholdValue: 10
              }
            ]
          }
        })
      }
    });

    expect(wrapper.text()).toContain('态势判定');
    expect(wrapper.text()).toContain('判定原因');
    expect(wrapper.text()).toContain('SINGLE_SIGNAL_ONLY');
    expect(wrapper.text()).toContain('未触发');
    expect(wrapper.text()).toContain('1 / 3');
    expect(wrapper.text()).toContain('裂缝计 A');
    expect(wrapper.text()).toContain('裂缝位移');
  });

  it('stays hidden when notes do not contain risk object situation evidence', () => {
    const wrapper = mount(RiskObjectSituationPanel, {
      props: {
        source: JSON.stringify({ note: '普通备注' })
      }
    });

    expect(wrapper.html()).toBe('<!--v-if-->');
  });

  it('renders backend signal summary fields', () => {
    const wrapper = mount(RiskObjectSituationPanel, {
      props: {
        source: JSON.stringify({
          riskObjectSituation: {
            reasonCode: 'CONFIRMED_MULTI_SIGNAL',
            triggerResponse: true,
            totalBindingCount: 2,
            activeSignalCount: 2,
            responseLevel: 'red',
            activeSignals: [
              {
                bindingId: 91001,
                deviceId: 3002,
                metricIdentifier: 'dispsX',
                value: '25.6',
                level: 'red'
              }
            ]
          }
        })
      }
    });

    expect(wrapper.text()).toContain('CONFIRMED_MULTI_SIGNAL');
    expect(wrapper.text()).toContain('2 / 2');
    expect(wrapper.text()).toContain('dispsX');
    expect(wrapper.text()).toContain('25.6');
    expect(wrapper.text()).toContain('3002');
  });
});
