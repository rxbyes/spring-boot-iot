import type { RiskPointBindingDeviceGroup } from '@/api/riskPoint'
import type { DeviceOption } from '@/types/api'

export type DeviceBindingCapabilityType = 'MONITORING' | 'WARNING' | 'VIDEO' | 'UNKNOWN'

const normalizeCapabilityType = (value?: string | null): DeviceBindingCapabilityType => {
  const normalized = (value || '').trim().toUpperCase()
  if (normalized === 'MONITORING' || normalized === 'WARNING' || normalized === 'VIDEO') {
    return normalized
  }
  return 'UNKNOWN'
}

export const resolveDeviceCapabilityType = (device?: Partial<DeviceOption> | null): DeviceBindingCapabilityType =>
  normalizeCapabilityType(device?.deviceCapabilityType)

export const resolveBindingGroupCapabilityType = (
  group?: Partial<RiskPointBindingDeviceGroup> | null
): DeviceBindingCapabilityType => normalizeCapabilityType(group?.deviceCapabilityType)

export const supportsMetricBinding = (device?: Partial<DeviceOption> | null) => {
  if (!device) {
    return false
  }
  if (typeof device.supportsMetricBinding === 'boolean') {
    return device.supportsMetricBinding
  }
  const capabilityType = resolveDeviceCapabilityType(device)
  if (capabilityType === 'WARNING' || capabilityType === 'VIDEO') {
    return false
  }
  return true
}

export const isDeviceOnlyBindingMode = (group?: Partial<RiskPointBindingDeviceGroup> | null) =>
  (group?.bindingMode || '').trim().toUpperCase() === 'DEVICE_ONLY'

export const getDeviceCapabilityLabel = (capabilityType?: string | null) => {
  switch (normalizeCapabilityType(capabilityType)) {
    case 'MONITORING':
      return '监测型'
    case 'WARNING':
      return '预警型'
    case 'VIDEO':
      return '视频类'
    default:
      return '设备级'
  }
}

export const getDeviceOnlyBindingHint = (device?: Partial<DeviceOption> | null) => {
  const capabilityType = resolveDeviceCapabilityType(device)
  if (capabilityType === 'VIDEO') {
    return '该设备当前按设备级正式绑定收口，并预留 AI 事件分析扩展位。'
  }
  return '该设备无正式测点能力，将按设备级正式绑定收口，仅参与被动处置关联。'
}

export const getDeviceOnlyBindingButtonLabel = (device?: Partial<DeviceOption> | null) =>
  !device || supportsMetricBinding(device) ? '新增正式绑定' : '新增设备级正式绑定'

export const isAiEventReserved = (group?: Partial<RiskPointBindingDeviceGroup> | null) =>
  Boolean(group?.aiEventExpandable) || (group?.extensionStatus || '').trim().toUpperCase() === 'AI_EVENT_RESERVED'
