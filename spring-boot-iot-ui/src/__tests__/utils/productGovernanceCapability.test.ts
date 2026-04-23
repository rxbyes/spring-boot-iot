import { describe, expect, it } from 'vitest'

import {
  getProductGovernanceCapabilityLabel,
  resolveProductGovernanceApplicability,
  resolveProductGovernanceCapabilityType
} from '@/utils/productGovernanceCapability'

describe('productGovernanceCapability', () => {
  it('prefers explicit metadata governance capability over product-name fallback', () => {
    expect(
      resolveProductGovernanceCapabilityType({
        productName: '南方测绘 监测型 遥测终端',
        metadataJson: JSON.stringify({
          governance: {
            productCapabilityType: 'COLLECTING'
          }
        })
      })
    ).toBe('COLLECTING')
  })

  it('falls back to product-name keywords when explicit governance capability is missing', () => {
    expect(
      resolveProductGovernanceCapabilityType({
        productName: '南方测绘 采集型 遥测终端'
      })
    ).toBe('COLLECTING')

    expect(
      resolveProductGovernanceCapabilityType({
        productName: '中海达 预警型 爆闪灯终端'
      })
    ).toBe('WARNING')

    expect(
      resolveProductGovernanceCapabilityType({
        productName: '边坡 AI 视频球机'
      })
    ).toBe('VIDEO')
  })

  it('treats unknown capability as pending confirmation instead of not applicable', () => {
    const applicability = resolveProductGovernanceApplicability({
      productName: '通用设备'
    })

    expect(applicability.capabilityType).toBe('UNKNOWN')
    expect(applicability.supportsMetricGovernance).toBe(false)
    expect(applicability.supportsDeviceOnlyRiskBinding).toBe(false)
    expect(getProductGovernanceCapabilityLabel(applicability.capabilityType)).toBe('待确认')
  })

  it('derives governance applicability by capability type', () => {
    expect(
      resolveProductGovernanceApplicability({
        metadataJson: JSON.stringify({
          governance: {
            productCapabilityType: 'MONITORING'
          }
        })
      })
    ).toEqual(
      expect.objectContaining({
        capabilityType: 'MONITORING',
        supportsMetricGovernance: true,
        supportsDeviceOnlyRiskBinding: false
      })
    )

    expect(
      resolveProductGovernanceApplicability({
        metadataJson: JSON.stringify({
          governance: {
            productCapabilityType: 'VIDEO'
          }
        })
      })
    ).toEqual(
      expect.objectContaining({
        capabilityType: 'VIDEO',
        supportsMetricGovernance: false,
        supportsDeviceOnlyRiskBinding: true
      })
    )
  })
})
