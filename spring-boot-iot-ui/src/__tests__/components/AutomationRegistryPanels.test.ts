import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import AutomationRegistryPanel from '@/components/AutomationRegistryPanel.vue';
import AutomationResultImportPanel from '@/components/AutomationResultImportPanel.vue';

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  props: ['title', 'description'],
  template: `
    <section class="panel-card-stub">
      <h2>{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
      <slot />
      <slot name="actions" />
    </section>
  `
});

describe('AutomationRegistry panels', () => {
  it('renders runner type, blocking level, and doc ref for each scenario', () => {
    const wrapper = mount(AutomationRegistryPanel, {
      props: {
        scenarios: [
          {
            id: 'risk.full-drill.red-chain',
            title: '风险闭环红链路演练',
            runnerType: 'riskDrill',
            blocking: 'blocker',
            docRef: 'docs/21#风险闭环主链路',
            dependsOn: ['auth.browser-smoke']
          }
        ],
        summary: {
          total: 1,
          blockerCount: 1,
          byRunner: {
            riskDrill: 1
          }
        }
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub,
          StandardTableToolbar: true,
          StandardTableTextColumn: true,
          ElTable: true,
          ElTableColumn: true,
          ElTag: true
        }
      }
    });

    expect(wrapper.text()).toContain('风险闭环红链路演练');
    expect(wrapper.text()).toContain('riskDrill');
    expect(wrapper.text()).toContain('blocker');
    expect(wrapper.text()).toContain('docs/21#风险闭环主链路');
  });

  it('renders imported run summary and failed scenario ids', () => {
    const wrapper = mount(AutomationResultImportPanel, {
      props: {
        importedRun: {
          summary: {
            total: 2,
            passed: 1,
            failed: 1
          },
          failedScenarioIds: ['risk.full-drill.red-chain']
        }
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub,
          StandardActionGroup: true,
          StandardButton: true,
          ElTag: true,
          ElInput: true
        }
      }
    });

    expect(wrapper.text()).toContain('risk.full-drill.red-chain');
    expect(wrapper.text()).toContain('失败 1');
  });
});
