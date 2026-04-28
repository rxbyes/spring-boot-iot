import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import DeviceDetailWorkbench from '@/components/device/DeviceDetailWorkbench.vue'
import type { Device } from '@/types/api'

const registeredDevice: Device = {
  id: 2001,
  productId: 1001,
  gatewayId: 3001,
  parentDeviceId: 3002,
  orgId: 7101,
  orgName: '平台运维中心',
  productKey: 'north-monitor-gnss-v1',
  productName: '北斗监测终端',
  gatewayDeviceCode: 'GATEWAY-001',
  gatewayDeviceName: '边坡网关 01',
  parentDeviceCode: 'PARENT-009',
  parentDeviceName: '边坡主控 09',
  deviceName: '东侧位移计 01',
  deviceCode: 'DEV-00182',
  deviceSecret: 'secret-182',
  clientId: 'client-00182',
  username: 'north-monitor-gnss-v1&DEV-00182',
  password: 'mqtt-password',
  protocolCode: 'mqtt-json',
  nodeType: 1,
  onlineStatus: 1,
  activateStatus: 1,
  deviceStatus: 1,
  registrationStatus: 1,
  assetSourceType: 'device_registry',
  firmwareVersion: 'v2.8.1',
  ipAddress: '10.23.8.17',
  address: '一号厂房东侧监测带',
  metadataJson: '{"site":"一号厂房","owner":"巡检一班"}',
  lastReportTopic: '/sys/north-monitor-gnss-v1/DEV-00182/thing/event/property/post',
  lastTraceId: 'trace-20260401-001',
  lastOnlineTime: '2026-04-01T09:16:00',
  lastOfflineTime: '2026-03-31T18:40:00',
  lastReportTime: '2026-04-01T09:18:00',
  createTime: '2026-03-01T10:00:00',
  updateTime: '2026-04-01T09:19:00'
}

const unregisteredDevice: Device = {
  deviceName: '',
  deviceCode: 'TEMP-UNREG-009',
  productKey: 'south-radar-v2',
  protocolCode: 'mqtt-json',
  registrationStatus: 0,
  assetSourceType: 'invalid_report',
  lastFailureStage: 'DEVICE_CONTRACT',
  lastErrorMessage: '设备未登记，无法完成设备契约匹配。',
  lastReportTopic: '/sys/south-radar-v2/TEMP-UNREG-009/thing/event/property/post',
  lastTraceId: 'trace-unregistered-001',
  sourceRecordId: 8801,
  lastPayload: '{"temp":18.5,"battery":92}',
  lastReportTime: '2026-04-01T08:50:00',
  updateTime: '2026-04-01T08:51:00',
  createTime: '2026-04-01T08:50:00'
}

const sparseRegisteredDevice: Device = {
  registrationStatus: 1,
  id: 3001,
  deviceCode: 'SPARSE-001',
  deviceName: '精简档案设备'
}

describe('DeviceDetailWorkbench', () => {
  it('renders the registered device with an identity-first summary strip and split ledgers', () => {
    const wrapper = mount(DeviceDetailWorkbench, {
      props: {
        device: registeredDevice
      }
    })

    expect(wrapper.find('[data-testid="device-detail-hero"]').exists()).toBe(false)
    expect(wrapper.findAll('.device-detail-workbench__stage-header p')).toHaveLength(0)
    expect(wrapper.find('[data-testid="device-detail-section-archive"]').exists()).toBe(false)

    const summaryStage = wrapper.get('[data-testid="device-detail-summary-stage"]')
    expect(summaryStage.text()).toContain('设备身份')
    expect(summaryStage.text()).toContain('在线状态')
    expect(summaryStage.text()).toContain('激活状态')
    expect(summaryStage.text()).toContain('设备状态')
    expect(summaryStage.text()).toContain('最近上报')

    const summaryPrimary = wrapper.get('[data-testid="device-detail-summary-primary"]')
    expect(summaryPrimary.text()).toContain('东侧位移计 01')
    expect(summaryPrimary.text()).toContain('DEV-00182')
    expect(summaryPrimary.text()).toContain('北斗监测终端')

    const overviewStage = wrapper.get('[data-testid="device-detail-overview-stage"]')
    expect(overviewStage.text()).toContain('资产概览')
    expect(overviewStage.text()).toContain('运行概览')
    expect(overviewStage.text()).toContain('所属机构')
    expect(overviewStage.text()).toContain('部署位置')
    expect(overviewStage.text()).toContain('最近在线')

    const identityStage = wrapper.get('[data-testid="device-detail-identity-stage"]')
    expect(identityStage.text()).toContain('建档与接入台账')
    expect(identityStage.text()).toContain('设备 ID')
    expect(identityStage.text()).toContain('接入协议')
    expect(identityStage.text()).toContain('节点类型')

    const runtimeStage = wrapper.get('[data-testid="device-detail-runtime-stage"]')
    expect(runtimeStage.text()).toContain('认证与运行台账')
    expect(runtimeStage.text()).toContain('Client ID')
    expect(runtimeStage.text()).toContain('设备密钥')

    const supportStage = wrapper.get('[data-testid="device-detail-support-stage"]')
    expect(supportStage.text()).toContain('关系与建档补充')
    expect(supportStage.text()).toContain('父设备')
    expect(supportStage.text()).toContain('网关设备')

    expect(wrapper.get('[data-testid="device-detail-metadata-stage"]').text()).toContain('扩展元数据快照')
    expect(wrapper.find('[data-testid="device-detail-capability-stage"]').exists()).toBe(false)
    expect(wrapper.findAll('.device-detail-workbench__ledger-item--wide').length).toBeGreaterThan(0)

    expect(wrapper.text()).toContain('北斗监测终端')
    expect(wrapper.text()).toContain('平台运维中心')
    expect(wrapper.text()).toContain('一号厂房东侧监测带')
    expect(wrapper.text()).toContain('client-00182')
  })

  it('keeps the unregistered detail in the same workbench syntax with an intake-first summary strip', () => {
    const wrapper = mount(DeviceDetailWorkbench, {
      props: {
        device: unregisteredDevice
      }
    })

    expect(wrapper.find('[data-testid="device-detail-hero"]').exists()).toBe(false)
    expect(wrapper.findAll('.device-detail-workbench__stage-header p')).toHaveLength(0)
    expect(wrapper.find('[data-testid="device-detail-section-archive"]').exists()).toBe(false)

    const summaryStage = wrapper.get('[data-testid="device-detail-summary-stage"]')
    expect(summaryStage.text()).toContain('待建档线索')
    expect(summaryStage.text()).toContain('登记状态')
    expect(summaryStage.text()).toContain('来源类型')
    expect(summaryStage.text()).toContain('最近上报')
    expect(summaryStage.text()).toContain('失败阶段')

    const summaryPrimary = wrapper.get('[data-testid="device-detail-summary-primary"]')
    expect(summaryPrimary.text()).toContain('TEMP-UNREG-009')
    expect(summaryPrimary.text()).toContain('south-radar-v2')
    expect(summaryPrimary.text()).toContain('mqtt-json')

    const sourceStage = wrapper.get('[data-testid="device-detail-source-stage"]')
    expect(sourceStage.text()).toContain('来源档案与失败摘要')
    expect(sourceStage.text()).toContain('设备编码')
    expect(sourceStage.text()).toContain('来源记录')
    expect(sourceStage.text()).toContain('Topic')

    expect(wrapper.get('[data-testid="device-detail-payload-stage"]').text()).toContain('最近载荷')
    expect(wrapper.text()).toContain('DEVICE_CONTRACT')
    expect(wrapper.text()).toContain('设备未登记，无法完成设备契约匹配。')
    expect(wrapper.text()).toContain('"battery": 92')
  })

  it('uses the flat workbench stages instead of the old archive topology status sections', () => {
    const source = readFileSync(resolve(import.meta.dirname, '../../../components/device/DeviceDetailWorkbench.vue'), 'utf8')

    expect(source).toContain('device-detail-summary-stage')
    expect(source).toContain('device-detail-overview-stage')
    expect(source).toContain('device-detail-identity-stage')
    expect(source).toContain('device-detail-runtime-stage')
    expect(source).toContain('device-detail-support-stage')
    expect(source).toContain('device-detail-metadata-stage')
    expect(source).toContain('device-detail-workbench__summary-card--primary')
    expect(source).toContain('device-detail-workbench__summary-meta')
    expect(source).not.toContain('device-detail-section-archive')
    expect(source).not.toContain('device-detail-section-topology')
    expect(source).not.toContain('device-detail-section-status')
    expect(source).not.toContain('device-detail-workbench__hero')
    expect(source).not.toContain('设备能力与命令')
  })

  it('hides registered sections whose fields are all empty', () => {
    const wrapper = mount(DeviceDetailWorkbench, {
      props: {
        device: sparseRegisteredDevice
      }
    })

    expect(wrapper.get('[data-testid="device-detail-summary-stage"]').text()).toContain('设备身份')
    expect(wrapper.get('[data-testid="device-detail-identity-stage"]').text()).toContain('建档与接入台账')
    expect(wrapper.find('[data-testid="device-detail-runtime-stage"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="device-detail-support-stage"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="device-detail-metadata-stage"]').exists()).toBe(false)
  })

  it('keeps ledger values in the same non-bold text rhythm even after introducing summary cards', () => {
    const wrapper = mount(DeviceDetailWorkbench, {
      props: {
        device: registeredDevice
      }
    })

    expect(wrapper.findAll('strong.device-detail-workbench__ledger-value')).toHaveLength(0)
    expect(wrapper.findAll('.device-detail-workbench__ledger-value').length).toBeGreaterThan(0)

    const source = readFileSync(resolve(import.meta.dirname, '../../../components/device/DeviceDetailWorkbench.vue'), 'utf8')
    expect(source).toContain('.device-detail-workbench__ledger-value')
    expect(source).toContain('font-size: 14px;')
    expect(source).toContain('font-weight: 400;')
  })

  it('keeps the detail workbench focused on asset and ledger content only', () => {
    const wrapper = mount(DeviceDetailWorkbench, {
      props: {
        device: registeredDevice
      }
    })

    expect(wrapper.text()).toContain('资产状态与接入概况')
    expect(wrapper.text()).toContain('建档与接入台账')
    expect(wrapper.text()).toContain('扩展元数据快照')
    expect(wrapper.text()).not.toContain('设备能力与命令')
    expect(wrapper.find('[data-testid="device-detail-capability-stage"]').exists()).toBe(false)
  })
})
