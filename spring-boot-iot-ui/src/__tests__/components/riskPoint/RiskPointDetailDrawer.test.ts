import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import RiskPointDetailDrawer from '@/components/riskPoint/RiskPointDetailDrawer.vue'

const {
  mockGetRiskPointById,
  mockListBindingGroups
} = vi.hoisted(() => ({
  mockGetRiskPointById: vi.fn(),
  mockListBindingGroups: vi.fn()
}))

vi.mock('@/api/riskPoint', () => ({
  getRiskPointById: mockGetRiskPointById,
  listBindingGroups: mockListBindingGroups
}))

vi.mock('@/utils/message', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['modelValue', 'title', 'subtitle', 'tags', 'loading', 'empty'],
  emits: ['update:modelValue'],
  template: `
    <section class="standard-detail-drawer-stub" :data-model-value="modelValue">
      <header>
        <h2>{{ title }}</h2>
        <p>{{ subtitle }}</p>
        <div class="standard-detail-drawer-stub__tags">
          <span v-for="tag in tags || []" :key="tag.label">{{ tag.label }}</span>
        </div>
      </header>
      <div v-if="loading">loading</div>
      <div v-else-if="empty">empty</div>
      <slot v-else />
      <slot name="footer" />
    </section>
  `
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  props: ['disabled'],
  emits: ['click'],
  template: `
    <button type="button" :disabled="Boolean(disabled)" @click="$emit('click')">
      <slot />
    </button>
  `
})

const ElTagStub = defineComponent({
  name: 'ElTag',
  template: '<span class="el-tag-stub"><slot /></span>'
})

const EmptyStateStub = defineComponent({
  name: 'EmptyState',
  props: ['title', 'description'],
  template: '<section class="empty-state-stub">{{ title }}{{ description }}</section>'
})

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function createDeferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void

  const promise = new Promise<T>((innerResolve, innerReject) => {
    resolve = innerResolve
    reject = innerReject
  })

  return { promise, resolve, reject }
}

function createRiskPointRow(overrides: Record<string, unknown> = {}) {
  return {
    id: 1,
    riskPointCode: 'RP-OPSCEN-NORTHS-CRIT-001',
    riskPointName: '示例风险点',
    orgId: 7101,
    orgName: '平台运维中心',
    regionId: 1,
    regionName: '东区',
    responsibleUser: 1,
    responsibleUserName: '张三',
    responsiblePhone: '13800000000',
    riskPointLevel: 'level_1',
    currentRiskLevel: 'red',
    riskLevel: 'red',
    description: '原始描述',
    status: 0,
    tenantId: 1,
    remark: '',
    createBy: 1,
    createTime: '2026-04-01 08:00:00',
    updateBy: 1,
    updateTime: '2026-04-01 09:00:00',
    deleted: 0,
    ...overrides
  }
}

function mountDrawer(propOverrides: Record<string, unknown> = {}) {
  return mount(RiskPointDetailDrawer, {
    props: {
      modelValue: true,
      riskPointId: 1,
      initialRiskPoint: createRiskPointRow(),
      initialSummary: {
        riskPointId: 1,
        boundDeviceCount: 1,
        boundMetricCount: 2,
        pendingBindingCount: 1
      },
      ...propOverrides
    },
    global: {
      stubs: {
        StandardDetailDrawer: StandardDetailDrawerStub,
        StandardButton: StandardButtonStub,
        ElTag: ElTagStub,
        EmptyState: EmptyStateStub
      }
    }
  })
}

describe('RiskPointDetailDrawer', () => {
  beforeEach(() => {
    mockGetRiskPointById.mockReset()
    mockListBindingGroups.mockReset()
    mockGetRiskPointById.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createRiskPointRow({
        description: '最新描述',
        remark: '最新备注',
        updateTime: '2026-04-04 10:00:00'
      })
    })
    mockListBindingGroups.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          deviceId: 21,
          deviceCode: 'DEVICE-021',
          deviceName: '一号倾角仪',
          bindingMode: 'METRIC',
          deviceCapabilityType: 'MONITORING',
          aiEventExpandable: false,
          extensionStatus: null,
          metricCount: 2,
          metrics: [
            {
              bindingId: 301,
              metricIdentifier: 'L1_QJ_1.angle',
              metricName: '倾角',
              bindingSource: 'MANUAL'
            },
            {
              bindingId: 302,
              metricIdentifier: 'L1_QJ_1.AZI',
              metricName: '方位角',
              bindingSource: 'PENDING_PROMOTION'
            }
          ]
        },
        {
          deviceId: 22,
          deviceCode: 'DEVICE-022',
          deviceName: '北坡视频设备',
          bindingMode: 'DEVICE_ONLY',
          deviceCapabilityType: 'VIDEO',
          aiEventExpandable: true,
          extensionStatus: 'AI_EVENT_RESERVED',
          metricCount: 0,
          metrics: []
        }
      ]
    })
  })

  it('renders initial summary immediately and refreshes with latest detail plus binding groups', async () => {
    const wrapper = mountDrawer()
    await flushPromises()

    expect(mockGetRiskPointById).toHaveBeenCalledWith(1)
    expect(mockListBindingGroups).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('示例风险点')
    expect(wrapper.text()).toContain('绑定概况')
    expect(wrapper.text()).toContain('2 台已绑定设备')
    expect(wrapper.text()).toContain('2 个正式测点')
    expect(wrapper.text()).toContain('已绑定 / 待治理')
    expect(wrapper.text()).toContain('风险点档案')
    expect(wrapper.text()).toContain('一号倾角仪')
    expect(wrapper.text()).toContain('L1_QJ_1.AZI')
    expect(wrapper.text()).toContain('北坡视频设备')
    expect(wrapper.text()).toContain('设备级正式绑定')
    expect(wrapper.text()).toContain('AI 事件扩展预留')
    expect(wrapper.text()).toContain('待治理 1 条')
    expect(wrapper.text()).toContain('最新描述')
  })

  it('ignores stale async responses when the risk point switches quickly', async () => {
    const firstDetail = createDeferred<any>()
    const secondDetail = createDeferred<any>()
    const firstGroups = createDeferred<any>()
    const secondGroups = createDeferred<any>()

    mockGetRiskPointById
      .mockReturnValueOnce(firstDetail.promise)
      .mockReturnValueOnce(secondDetail.promise)
    mockListBindingGroups
      .mockReturnValueOnce(firstGroups.promise)
      .mockReturnValueOnce(secondGroups.promise)

    const wrapper = mountDrawer()

    await wrapper.setProps({
      riskPointId: 2,
      initialRiskPoint: createRiskPointRow({ id: 2, riskPointName: '二号风险点', riskPointCode: 'RP-SECOND-002' }),
      initialSummary: {
        riskPointId: 2,
        boundDeviceCount: 0,
        boundMetricCount: 0,
        pendingBindingCount: 0
      }
    })

    firstDetail.resolve({
      code: 200,
      msg: 'success',
      data: createRiskPointRow({ id: 1, riskPointName: '旧风险点' })
    })
    firstGroups.resolve({ code: 200, msg: 'success', data: [] })
    secondDetail.resolve({
      code: 200,
      msg: 'success',
      data: createRiskPointRow({ id: 2, riskPointName: '二号风险点', riskPointCode: 'RP-SECOND-002' })
    })
    secondGroups.resolve({ code: 200, msg: 'success', data: [] })
    await flushPromises()

    expect(wrapper.text()).toContain('二号风险点')
    expect(wrapper.text()).not.toContain('旧风险点')
  })

  it('emits edit and binding-workbench actions from the footer', async () => {
    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="detail-edit-action"]').trigger('click')
    await wrapper.get('[data-testid="detail-binding-workbench-action"]').trigger('click')

    expect(wrapper.emitted('edit')).toHaveLength(1)
    expect(wrapper.emitted('binding-workbench')).toHaveLength(1)
  })
})
