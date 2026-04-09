import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AutomationExecutionConfigPanel from '@/components/AutomationExecutionConfigPanel.vue'
import AutomationPageDiscoveryPanel from '@/components/AutomationPageDiscoveryPanel.vue'
import AutomationSuggestionPanel from '@/components/AutomationSuggestionPanel.vue'
import PropertyTrendPanel from '@/components/PropertyTrendPanel.vue'

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="automation-panel-card-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
      <slot />
      <slot name="actions" />
    </section>
  `
})

describe('automation panel eyebrow cleanup', () => {
  it('keeps execution config and suggestion panels on title/description only', () => {
    const executionWrapper = mount(AutomationExecutionConfigPanel, {
      props: {
        target: {
          planName: '默认计划',
          frontendBaseUrl: 'http://127.0.0.1:5174',
          backendBaseUrl: 'http://127.0.0.1:9999',
          loginRoute: '/login',
          username: 'admin',
          password: '123456',
          browserPath: '',
          issueDocPath: 'docs/issues.md',
          outputPrefix: 'config-browser',
          baselineDir: 'config/automation/baselines',
          headless: true,
          scenarioScopes: [],
          failScopes: []
        },
        scopeOptions: ['delivery', 'baseline']
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub,
          ElForm: true,
          ElFormItem: true,
          ElInput: true,
          ElSelect: true,
          ElOption: true,
          ElSwitch: true
        }
      }
    })

    const suggestionWrapper = mount(AutomationSuggestionPanel, {
      props: {
        suggestions: [
          {
            level: 'warning',
            title: '补齐断言',
            detail: '当前计划里还有未覆盖断言的页面。'
          }
        ]
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub
        }
      }
    })

    expect(executionWrapper.findComponent(PanelCardStub).props('eyebrow')).toBeUndefined()
    expect(suggestionWrapper.findComponent(PanelCardStub).props('eyebrow')).toBeUndefined()
    expect(executionWrapper.text()).not.toContain('Execution Target')
    expect(suggestionWrapper.text()).not.toContain('Suggestion Engine')
  })

  it('keeps page discovery and property trend panels without the legacy English eyebrow tier', () => {
    const discoveryWrapper = mount(AutomationPageDiscoveryPanel, {
      props: {
        metrics: [],
        inventorySourceText: '菜单盘点',
        pageInventory: [],
        buildInventorySourceLabel: (source: string) => source,
        buildTemplateLabel: (template: string) => template,
        isRouteCovered: () => false
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub,
          MetricCard: true,
          StandardActionGroup: true,
          StandardButton: true,
          StandardTableToolbar: true,
          StandardTableTextColumn: true,
          StandardRowActions: true,
          StandardActionLink: true,
          ElTable: true,
          ElTableColumn: true,
          ElTag: true
        }
      }
    })

    const trendWrapper = mount(PropertyTrendPanel, {
      props: {
        logs: []
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub
        }
      }
    })

    expect(discoveryWrapper.findComponent(PanelCardStub).props('eyebrow')).toBeUndefined()
    expect(trendWrapper.findComponent(PanelCardStub).props('eyebrow')).toBeUndefined()
    expect(discoveryWrapper.text()).not.toContain('Page Discovery')
    expect(trendWrapper.text()).not.toContain('Telemetry Preview')
  })
})
