import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import DeviceDetailWorkbench from '@/components/device/DeviceDetailWorkbench.vue'
import type { Device } from '@/types/api'

const registeredDevice: Device = {
  id: 2001,
  productId: 1001,
  gatewayId: 3001,
  parentDeviceId: 3002,
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

describe('DeviceDetailWorkbench', () => {
  it('renders the registered device detail as a flat summary band with paired ledgers', () => {
    const wrapper = mount(DeviceDetailWorkbench, {
      props: {
        device: registeredDevice
      }
    })

    expect(wrapper.get('[data-testid="device-detail-summary-stage"]').text()).toContain('资产状态与接入概况')
    expect(wrapper.get('[data-testid="device-detail-identity-stage"]').text()).toContain('身份与部署台账')
    expect(wrapper.get('[data-testid="device-detail-runtime-stage"]').text()).toContain('运行与认证台账')
    expect(wrapper.get('[data-testid="device-detail-support-pair"]').text()).toContain('关系与建档补充')
    expect(wrapper.get('[data-testid="device-detail-support-pair"]').text()).toContain('扩展元数据快照')
    expect(wrapper.findAll('.device-detail-workbench__summary-cell')).toHaveLength(4)
    expect(wrapper.findAll('.device-detail-workbench__identity-grid .device-detail-workbench__ledger-cell')).toHaveLength(9)
    expect(wrapper.findAll('.device-detail-workbench__runtime-grid .device-detail-workbench__ledger-cell')).toHaveLength(9)
    expect(wrapper.text()).toContain('北斗监测终端')
    expect(wrapper.text()).toContain('一号厂房东侧监测带')
    expect(wrapper.text()).toContain('client-00182')
    expect(wrapper.text()).not.toContain('资产概览')
    expect(wrapper.text()).not.toContain('资产档案')
    expect(wrapper.text()).not.toContain('拓扑关系')
    expect(wrapper.text()).not.toContain('运维信息')
    expect(wrapper.text()).not.toContain('认证信息')
    expect(wrapper.text()).not.toContain('扩展元数据用于保存库存')
  })

  it('keeps the unregistered detail on the same flat syntax with source and payload stages', () => {
    const wrapper = mount(DeviceDetailWorkbench, {
      props: {
        device: unregisteredDevice
      }
    })

    expect(wrapper.get('[data-testid="device-detail-summary-stage"]').text()).toContain('资产状态与接入概况')
    expect(wrapper.get('[data-testid="device-detail-source-stage"]').text()).toContain('来源档案与失败摘要')
    expect(wrapper.get('[data-testid="device-detail-payload-stage"]').text()).toContain('最近载荷')
    expect(wrapper.find('[data-testid="device-detail-identity-stage"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="device-detail-runtime-stage"]').exists()).toBe(false)
    expect(wrapper.findAll('.device-detail-workbench__summary-cell')).toHaveLength(4)
    expect(wrapper.text()).toContain('TEMP-UNREG-009')
    expect(wrapper.text()).toContain('DEVICE_CONTRACT')
    expect(wrapper.text()).toContain('设备未登记，无法完成设备契约匹配。')
    expect(wrapper.text()).toContain('"battery": 92')
    expect(wrapper.text()).not.toContain('上报档案')
    expect(wrapper.text()).not.toContain('资产档案')
  })
})
