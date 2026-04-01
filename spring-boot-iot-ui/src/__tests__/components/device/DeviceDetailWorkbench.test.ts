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
  it('renders the registered device as the same sectioned detail language used by the edit drawer', () => {
    const wrapper = mount(DeviceDetailWorkbench, {
      props: {
        device: registeredDevice
      }
    })

    expect(wrapper.find('[data-testid="device-detail-summary-stage"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="device-detail-identity-stage"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="device-detail-runtime-stage"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="device-detail-support-pair"]').exists()).toBe(false)

    expect(wrapper.get('[data-testid="device-detail-section-archive"]').text()).toContain('基础档案')
    expect(wrapper.get('[data-testid="device-detail-section-archive"]').text()).toContain('设备 ID')
    expect(wrapper.get('[data-testid="device-detail-section-archive"]').text()).toContain('产品归属')
    expect(wrapper.get('[data-testid="device-detail-section-archive"]').text()).toContain('接入协议')

    expect(wrapper.get('[data-testid="device-detail-section-topology"]').text()).toContain('父子拓扑')
    expect(wrapper.get('[data-testid="device-detail-section-topology"]').text()).toContain('父设备')
    expect(wrapper.get('[data-testid="device-detail-section-topology"]').text()).toContain('网关设备')

    expect(wrapper.get('[data-testid="device-detail-section-status"]').text()).toContain('状态与维护属性')
    expect(wrapper.get('[data-testid="device-detail-section-status"]').text()).toContain('最近在线')
    expect(wrapper.get('[data-testid="device-detail-section-status"]').text()).toContain('固件版本')
    expect(wrapper.get('[data-testid="device-detail-section-status"]').text()).toContain('部署位置')

    expect(wrapper.get('[data-testid="device-detail-section-auth"]').text()).toContain('认证字段')
    expect(wrapper.get('[data-testid="device-detail-section-auth"]').text()).toContain('Client ID')
    expect(wrapper.get('[data-testid="device-detail-section-auth"]').text()).toContain('用户名')
    expect(wrapper.get('[data-testid="device-detail-section-auth"]').text()).toContain('设备密钥')

    expect(wrapper.get('[data-testid="device-detail-section-metadata"]').text()).toContain('扩展信息')

    expect(wrapper.text()).toContain('北斗监测终端')
    expect(wrapper.text()).toContain('一号厂房东侧监测带')
    expect(wrapper.text()).toContain('client-00182')
  })

  it('keeps the unregistered detail in the same sectioned syntax with source, failure, and payload blocks', () => {
    const wrapper = mount(DeviceDetailWorkbench, {
      props: {
        device: unregisteredDevice
      }
    })

    expect(wrapper.find('[data-testid="device-detail-section-archive"]').exists()).toBe(false)
    expect(wrapper.get('[data-testid="device-detail-section-source"]').text()).toContain('来源档案')
    expect(wrapper.get('[data-testid="device-detail-section-source"]').text()).toContain('设备编码')
    expect(wrapper.get('[data-testid="device-detail-section-source"]').text()).toContain('来源记录')
    expect(wrapper.get('[data-testid="device-detail-section-failure"]').text()).toContain('失败摘要')
    expect(wrapper.get('[data-testid="device-detail-section-failure"]').text()).toContain('失败阶段')
    expect(wrapper.get('[data-testid="device-detail-section-failure"]').text()).toContain('Topic')
    expect(wrapper.get('[data-testid="device-detail-section-payload"]').text()).toContain('最近载荷')
    expect(wrapper.text()).toContain('DEVICE_CONTRACT')
    expect(wrapper.text()).toContain('设备未登记，无法完成设备契约匹配。')
    expect(wrapper.text()).toContain('"battery": 92')
  })

  it('uses section cards instead of the old ledger-pair layout', () => {
    const source = readFileSync(resolve(import.meta.dirname, '../../../components/device/DeviceDetailWorkbench.vue'), 'utf8')

    expect(source).toContain('device-detail-section-archive')
    expect(source).toContain('device-detail-section-topology')
    expect(source).toContain('device-detail-section-status')
    expect(source).toContain('device-detail-section-auth')
    expect(source).toContain('device-detail-section-metadata')
    expect(source).not.toContain('device-detail-identity-stage')
    expect(source).not.toContain('device-detail-runtime-stage')
  })

  it('hides registered sections whose fields are all empty', () => {
    const wrapper = mount(DeviceDetailWorkbench, {
      props: {
        device: sparseRegisteredDevice
      }
    })

    expect(wrapper.get('[data-testid="device-detail-section-archive"]').text()).toContain('基础档案')
    expect(wrapper.find('[data-testid="device-detail-section-topology"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="device-detail-section-status"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="device-detail-section-auth"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="device-detail-section-metadata"]').exists()).toBe(false)
  })

  it('keeps detail values in the same non-bold text rhythm as the edit drawer', () => {
    const wrapper = mount(DeviceDetailWorkbench, {
      props: {
        device: registeredDevice
      }
    })

    expect(wrapper.findAll('strong.device-detail-workbench__field-value')).toHaveLength(0)
    expect(wrapper.findAll('.device-detail-workbench__field-value').length).toBeGreaterThan(0)
    expect(wrapper.findAll('.device-detail-workbench__relation-card strong')).toHaveLength(0)

    const source = readFileSync(resolve(import.meta.dirname, '../../../components/device/DeviceDetailWorkbench.vue'), 'utf8')
    expect(source).toContain('.device-detail-workbench__field-value')
    expect(source).toContain('font-size: 14px;')
    expect(source).toContain('font-weight: 400;')
  })
})
