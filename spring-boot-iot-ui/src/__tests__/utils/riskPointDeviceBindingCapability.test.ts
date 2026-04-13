import { describe, expect, it } from 'vitest'

import {
  getDeviceCapabilityLabel,
  getDeviceOnlyBindingButtonLabel,
  getDeviceOnlyBindingHint,
  resolveDeviceCapabilityType,
  supportsMetricBinding
} from '@/utils/riskPointDeviceBindingCapability'

describe('riskPointDeviceBindingCapability', () => {
  it('treats collecting devices as device-only binding mode', () => {
    const device = {
      deviceCapabilityType: 'COLLECTING'
    }

    expect(resolveDeviceCapabilityType(device)).toBe('COLLECTING')
    expect(supportsMetricBinding(device)).toBe(false)
    expect(getDeviceCapabilityLabel(device.deviceCapabilityType)).toBe('采集型')
    expect(getDeviceOnlyBindingButtonLabel(device)).toBe('新增设备级正式绑定')
    expect(getDeviceOnlyBindingHint(device)).toContain('按设备级正式绑定收口')
  })
})
