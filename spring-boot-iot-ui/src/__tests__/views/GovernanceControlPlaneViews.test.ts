import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import GovernanceTaskView from '@/views/GovernanceTaskView.vue'
import GovernanceOpsWorkbenchView from '@/views/GovernanceOpsWorkbenchView.vue'

const { mockPageWorkItems, mockPageOpsAlerts } = vi.hoisted(() => ({
  mockPageWorkItems: vi.fn(),
  mockPageOpsAlerts: vi.fn()
}))

vi.mock('@/api/governanceWorkItem', () => ({
  pageGovernanceWorkItems: mockPageWorkItems
}))

vi.mock('@/api/governanceOpsAlert', () => ({
  pageGovernanceOpsAlerts: mockPageOpsAlerts
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: {}
  }),
  useRouter: () => ({
    push: vi.fn()
  })
}))

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0))

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  template: '<section class="standard-page-shell-stub"><slot /></section>'
})

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['title', 'description'],
  template: `
    <section class="governance-control-plane-workbench-stub">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <div><slot name="toolbar" /></div>
      <div><slot /></div>
      <div><slot name="pagination" /></div>
    </section>
  `
})

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  props: ['metaItems'],
  template: `
    <section class="governance-control-plane-toolbar-stub">
      <div>{{ (metaItems || []).join(' | ') }}</div>
      <slot />
      <slot name="right" />
    </section>
  `
})

const StandardPaginationStub = defineComponent({
  name: 'StandardPagination',
  template: '<div class="governance-control-plane-pagination-stub" />'
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button type="button" @click="$emit(`click`)"><slot /></button>'
})

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  props: ['title', 'description'],
  template: `
    <section class="governance-control-plane-panel-card-stub">
      <h3>{{ title }}</h3>
      <p>{{ description }}</p>
      <slot />
    </section>
  `
})

function mountWithStubs(component: Parameters<typeof mount>[0]) {
  return mount(component, {
    global: {
      stubs: {
        StandardPageShell: StandardPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardTableToolbar: StandardTableToolbarStub,
        StandardPagination: StandardPaginationStub,
        StandardButton: StandardButtonStub,
        PanelCard: PanelCardStub
      }
    }
  })
}

describe('governance control plane views', () => {
  beforeEach(() => {
    mockPageWorkItems.mockReset()
    mockPageOpsAlerts.mockReset()
  })

  it('renders governance task rows from backend work items', async () => {
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1,
            workItemCode: 'PENDING_CONTRACT_RELEASE',
            workStatus: 'OPEN',
            blockingReason: '合同尚未发布'
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceTaskView)
    await flushPromises()

    expect(wrapper.text()).toContain('待发布合同')
    expect(wrapper.text()).toContain('合同尚未发布')
  })

  it('renders governance ops rows from backend alerts', async () => {
    mockPageOpsAlerts.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 11,
            alertType: 'FIELD_DRIFT',
            alertTitle: '字段漂移告警',
            alertMessage: 'value 已偏离正式合同'
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceOpsWorkbenchView)
    await flushPromises()

    expect(wrapper.text()).toContain('字段漂移告警')
    expect(wrapper.text()).toContain('value 已偏离正式合同')
  })
})
