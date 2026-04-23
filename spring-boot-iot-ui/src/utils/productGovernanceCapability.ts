import type { Product, ProductGovernanceCapabilityType, ProductMetadata } from '@/types/api'

export interface ProductGovernanceApplicability {
  capabilityType: ProductGovernanceCapabilityType
  supportsMetricGovernance: boolean
  supportsDeviceOnlyRiskBinding: boolean
}

type ProductGovernanceCapabilitySource = Partial<Pick<Product, 'productName' | 'metadataJson'>> & {
  productCapabilityType?: string | null
}

const PRODUCT_GOVERNANCE_CAPABILITY_LABELS: Record<ProductGovernanceCapabilityType, string> = {
  MONITORING: '监测型',
  COLLECTING: '采集型',
  WARNING: '预警型',
  VIDEO: '视频型',
  UNKNOWN: '待确认'
}

export function resolveProductGovernanceCapabilityType(
  source?: ProductGovernanceCapabilitySource | null
): ProductGovernanceCapabilityType {
  const explicitCapabilityType =
    normalizeProductGovernanceCapabilityType(source?.productCapabilityType)
    || normalizeProductGovernanceCapabilityType(parseProductMetadata(source?.metadataJson)?.governance?.productCapabilityType)

  if (explicitCapabilityType) {
    return explicitCapabilityType
  }

  return inferProductGovernanceCapabilityType(source?.productName)
}

export function resolveProductGovernanceApplicability(
  source?: ProductGovernanceCapabilitySource | null
): ProductGovernanceApplicability {
  const capabilityType = resolveProductGovernanceCapabilityType(source)
  return {
    capabilityType,
    supportsMetricGovernance: capabilityType === 'MONITORING',
    supportsDeviceOnlyRiskBinding: capabilityType === 'COLLECTING' || capabilityType === 'WARNING' || capabilityType === 'VIDEO'
  }
}

export function getProductGovernanceCapabilityLabel(capabilityType?: string | null) {
  return PRODUCT_GOVERNANCE_CAPABILITY_LABELS[normalizeProductGovernanceCapabilityType(capabilityType) ?? 'UNKNOWN']
}

export function buildProductGovernanceMetadataJson(
  productCapabilityType: ProductGovernanceCapabilityType,
  baseMetadataJson?: string | null
): string | undefined {
  const baseMetadata = parseProductMetadata(baseMetadataJson) ?? {}

  if (!productCapabilityType && !Object.keys(baseMetadata).length) {
    return undefined
  }

  const nextMetadata: ProductMetadata = {
    ...baseMetadata,
    governance: {
      ...(baseMetadata.governance ?? {}),
      productCapabilityType
    }
  }

  return JSON.stringify(nextMetadata)
}

function normalizeProductGovernanceCapabilityType(value?: string | null): ProductGovernanceCapabilityType | null {
  const normalized = (value || '').trim().toUpperCase()
  if (
    normalized === 'MONITORING'
    || normalized === 'COLLECTING'
    || normalized === 'WARNING'
    || normalized === 'VIDEO'
    || normalized === 'UNKNOWN'
  ) {
    return normalized
  }
  return null
}

function inferProductGovernanceCapabilityType(productName?: string | null): ProductGovernanceCapabilityType {
  const normalizedName = (productName || '').trim().toLowerCase()
  if (!normalizedName) {
    return 'UNKNOWN'
  }

  if (/(采集型|采集器|遥测终端|collector|collect|telemetry)/.test(normalizedName)) {
    return 'COLLECTING'
  }

  if (/(预警型|预警|告警|广播|爆闪|喇叭|声光|warning|warn|alarm|strobe|speaker)/.test(normalizedName)) {
    return 'WARNING'
  }

  if (/(视频型|视频|摄像|监控|camera|video|cctv)/.test(normalizedName)) {
    return 'VIDEO'
  }

  if (/(监测型|监测|gnss|位移|裂缝|雨量|激光测距|laser|displacement|tilt|measure|monitor)/.test(normalizedName)) {
    return 'MONITORING'
  }

  return 'UNKNOWN'
}

function parseProductMetadata(metadataJson?: string | null): ProductMetadata | null {
  const text = (metadataJson || '').trim()
  if (!text) {
    return null
  }

  try {
    const parsed = JSON.parse(text)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return null
    }
    return parsed as ProductMetadata
  } catch {
    return null
  }
}
