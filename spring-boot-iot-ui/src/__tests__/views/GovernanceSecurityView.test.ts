import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { computed, defineComponent, inject, provide } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import GovernanceSecurityView from '@/views/GovernanceSecurityView.vue'

const governanceSecurityViewSource = readFileSync(
  resolve(import.meta.dirname, '../../views/GovernanceSecurityView.vue'),
  'utf8'
)

const { mockGetGovernancePermissionMatrix, mockPageDeviceSecretRotationLogs } = vi.hoisted(() => ({
  mockGetGovernancePermissionMatrix: vi.fn(),
  mockPageDeviceSecretRotationLogs: vi.fn()
}))

vi.mock('@/api/governanceSecurity', () => ({
  getGovernancePermissionMatrix: mockGetGovernancePermissionMatrix,
  pageDeviceSecretRotationLogs: mockPageDeviceSecretRotationLogs
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      error: vi.fn(),
      success: vi.fn(),
      warning: vi.fn()
    }
  }
})

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0))

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  template: '<section class="governance-security-page-shell"><slot /></section>'
})

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['title', 'description'],
  template: `
    <section class="governance-security-workbench">
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
      <div><slot name="filters" /></div>
      <div><slot name="toolbar" /></div>
      <div><slot /></div>
      <div><slot name="pagination" /></div>
    </section>
  `
})

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  props: ['title', 'description'],
  template: `
    <section class="governance-security-panel-card">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <slot />
    </section>
  `
})

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section class="governance-security-filter">
      <div><slot name="primary" /></div>
      <div><slot name="actions" /></div>
    </section>
  `
})

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  props: ['metaItems'],
  template: `
    <section class="governance-security-toolbar">
      <div class="governance-security-toolbar__meta">{{ (metaItems || []).join(' | ') }}</div>
      <slot />
      <slot name="right" />
    </section>
  `
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
})

const StandardPaginationStub = defineComponent({
  name: 'StandardPagination',
  template: '<div class="governance-security-pagination" />'
})

const StandardInlineStateStub = defineComponent({
  name: 'StandardInlineState',
  props: ['message'],
  template: `
    <div class="governance-security-inline-state">
      <span>{{ message }}</span>
    </div>
  `
})

const EmptyStateStub = defineComponent({
  name: 'EmptyState',
  props: ['title', 'description'],
  template: `
    <div class="governance-security-empty-state">
      <strong>{{ title }}</strong>
      <span>{{ description }}</span>
    </div>
  `
})

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: ['modelValue', 'placeholder'],
  emits: ['update:modelValue'],
  template: `
    <input
      class="el-input-stub"
      :placeholder="placeholder"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  `
})

const ElFormItemStub = defineComponent({
  name: 'ElFormItem',
  template: '<div class="el-form-item-stub"><slot /></div>'
})

const ElTagStub = defineComponent({
  name: 'ElTag',
  template: '<span class="el-tag-stub"><slot /></span>'
})

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: ['data'],
  setup(props) {
    provide('governanceSecurityTableRows', computed(() => props.data ?? []))
    return {}
  },
  template: '<section class="governance-security-table"><slot /></section>'
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['label'],
  setup() {
    const rows = inject<any[]>('governanceSecurityTableRows', [])
    return { rows }
  },
  template: `
    <div class="governance-security-column" :data-label="label">
      <div v-for="(row, index) in rows" :key="index">
        <slot :row="row" />
      </div>
    </div>
  `
})

const StandardTableTextColumnStub = defineComponent({
  name: 'StandardTableTextColumn',
  props: ['label', 'prop'],
  setup() {
    const rows = inject<any[]>('governanceSecurityTableRows', [])
    return { rows }
  },
  template: `
    <div class="governance-security-text-column" :data-label="label">
      <div v-for="(row, index) in rows" :key="index">
        {{ prop ? row?.[prop] : '' }}
      </div>
    </div>
  `
})

function mountView() {
  return mount(GovernanceSecurityView, {
    global: {
      directives: {
        loading: () => undefined
      },
      stubs: {
        StandardPageShell: StandardPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        PanelCard: PanelCardStub,
        StandardListFilterHeader: StandardListFilterHeaderStub,
        StandardTableToolbar: StandardTableToolbarStub,
        StandardButton: StandardButtonStub,
        StandardPagination: StandardPaginationStub,
        StandardInlineState: StandardInlineStateStub,
        EmptyState: EmptyStateStub,
        ElInput: ElInputStub,
        ElFormItem: ElFormItemStub,
        ElTag: ElTagStub,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub,
        StandardTableTextColumn: StandardTableTextColumnStub
      }
    }
  })
}

describe('GovernanceSecurityView', () => {
  beforeEach(() => {
    mockGetGovernancePermissionMatrix.mockReset()
    mockPageDeviceSecretRotationLogs.mockReset()

    mockGetGovernancePermissionMatrix.mockResolvedValue({
      code: 200,
      msg: 'ok',
      data: [
        {
          domainName: '合同治理',
          actionName: '合同发布',
          operatorPermissionCode: 'iot:product-contract:release',
          approverPermissionCode: 'iot:product-contract:approve',
          dualControlRequired: true,
          auditModule: 'governance-approval'
        }
      ]
    })
    mockPageDeviceSecretRotationLogs.mockResolvedValue({
      code: 200,
      msg: 'ok',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1,
            deviceCode: 'device-3001',
            productKey: 'prod-9001',
            rotationBatchId: 'ROT-3001-AAAA0001',
            previousSecretDigest: 'prev-digest',
            currentSecretDigest: 'next-digest',
            rotatedBy: 1001,
            approvedBy: 2002,
            rotateTime: '2026-04-09 10:00:00',
            reason: 'routine-rotation'
          }
        ]
      }
    })
  })

  it('renders governance permission matrix and secret rotation log ledger', async () => {
    const wrapper = mountView()

    await flushPromises()
    await flushPromises()

    expect(mockGetGovernancePermissionMatrix).toHaveBeenCalledTimes(1)
    expect(mockPageDeviceSecretRotationLogs).toHaveBeenCalledWith({
      deviceCode: '',
      productKey: '',
      rotationBatchId: '',
      pageNum: 1,
      pageSize: 10
    })
    expect(wrapper.text()).toContain('权限与密钥治理')
    expect(wrapper.text()).toContain('治理权限矩阵')
    expect(wrapper.text()).toContain('密钥轮换台账')
    expect(wrapper.text()).toContain('合同发布')
    expect(wrapper.text()).toContain('device-3001')
    expect(wrapper.text()).toContain('ROT-3001-AAAA0001')
  })

  it('keeps governance and device identity columns on the shared stacked grammar', () => {
    expect(governanceSecurityViewSource).toContain('secondary-prop="domainName"')
    expect(governanceSecurityViewSource).not.toContain('<StandardTableTextColumn prop="domainName" label="治理域"')
    expect(governanceSecurityViewSource).toContain('secondary-prop="productKey"')
    expect(governanceSecurityViewSource).not.toContain('<StandardTableTextColumn prop="productKey" label="产品 Key"')
  })
})
