import type { GovernanceWorkItem } from '@/types/api'
import type { RouteLocationRaw } from 'vue-router'

function parseSnapshot(snapshotJson?: string | null) {
  if (!snapshotJson) {
    return undefined
  }
  try {
    const parsed = JSON.parse(snapshotJson) as Record<string, unknown>
    return typeof parsed === 'object' && parsed != null ? parsed : undefined
  } catch {
    return undefined
  }
}

function normalizeText(value?: string | null) {
  if (typeof value !== 'string') {
    return undefined
  }
  const trimmed = value.trim()
  return trimmed || undefined
}

function snapshotText(snapshotJson: string | null | undefined, key: string) {
  const snapshot = parseSnapshot(snapshotJson)
  const value = snapshot?.[key]
  return typeof value === 'string' || typeof value === 'number' ? String(value).trim() || undefined : undefined
}

function snapshotNumber(snapshotJson: string | null | undefined, key: string) {
  const value = snapshotText(snapshotJson, key)
  if (!value || !/^-?\d+$/.test(value)) {
    return undefined
  }
  return value
}

function directNumber(value: unknown) {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return String(value)
  }
  if (typeof value === 'string') {
    const trimmed = value.trim()
    return /^-?\d+$/.test(trimmed) ? trimmed : undefined
  }
  return undefined
}

function normalizeCoverageType(value?: string) {
  const normalized = normalizeText(value)?.toUpperCase()
  return normalized === 'LINKAGE' || normalized === 'EMERGENCY_PLAN' ? normalized : undefined
}

function governanceContextQuery(item: GovernanceWorkItem) {
  const governanceBoundary = snapshotText(item.snapshotJson, 'governanceBoundary')
  const subjectOwnership = snapshotText(item.snapshotJson, 'subjectOwnership')
  const governanceFocus = snapshotText(item.snapshotJson, 'governanceFocus')
  return {
    ...(governanceBoundary ? { governanceBoundary } : {}),
    ...(subjectOwnership ? { subjectOwnership } : {}),
    ...(governanceFocus ? { governanceFocus } : {})
  }
}

export function buildGovernanceTaskDispatchLocation(item: GovernanceWorkItem): RouteLocationRaw | null {
  switch (item.workItemCode) {
    case 'PENDING_PRODUCT_GOVERNANCE':
    case 'PENDING_CONTRACT_RELEASE': {
      if (item.productId == null || String(item.productId).trim() === '') {
        return null
      }
      return {
        path: '/products',
        query: {
          openProductId: String(item.productId),
          workbenchView: 'models',
          governanceSource: 'task',
          workItemCode: item.workItemCode,
          ...governanceContextQuery(item)
        }
      }
    }
    case 'PENDING_RISK_BINDING': {
      const riskPointId = snapshotNumber(item.snapshotJson, 'riskPointId')
        || (item.subjectId != null && String(item.subjectId).trim() ? String(item.subjectId) : undefined)
      const keyword = snapshotText(item.snapshotJson, 'riskPointName')
        || normalizeText(item.deviceCode)
        || snapshotText(item.snapshotJson, 'deviceCode')
      if (!riskPointId && !keyword) {
        return null
      }
      return {
        path: '/risk-point',
        query: {
          ...(riskPointId ? { openRiskPointId: riskPointId } : {}),
          ...(keyword ? { keyword } : {}),
          bindingAction: 'pending-promotion',
          governanceSource: 'task',
          workItemCode: 'PENDING_RISK_BINDING',
          ...governanceContextQuery(item)
        }
      }
    }
    case 'PENDING_THRESHOLD_POLICY': {
      const riskMetricId = directNumber(item.riskMetricId) || snapshotNumber(item.snapshotJson, 'riskMetricId')
      const metricIdentifier = snapshotText(item.snapshotJson, 'metricIdentifier')
      const metricName = snapshotText(item.snapshotJson, 'metricName')
      if (!riskMetricId && !metricIdentifier && !metricName) {
        return null
      }
      return {
        path: '/rule-definition',
        query: {
          governanceAction: 'create',
          governanceSource: 'task',
          workItemCode: 'PENDING_THRESHOLD_POLICY',
          ...(riskMetricId ? { riskMetricId } : {}),
          ...(metricIdentifier ? { metricIdentifier } : {}),
          ...(metricName ? { metricName } : {}),
          ...governanceContextQuery(item)
        }
      }
    }
    case 'PENDING_LINKAGE_PLAN': {
      const coverageType = normalizeCoverageType(snapshotText(item.snapshotJson, 'coverageType'))
      if (!coverageType) {
        return null
      }
      const dimensionKey = snapshotText(item.snapshotJson, 'dimensionKey')
      const riskMetricId = directNumber(item.riskMetricId) || snapshotNumber(item.snapshotJson, 'riskMetricId')
      const metricIdentifier = snapshotText(item.snapshotJson, 'metricIdentifier')
      const metricName = snapshotText(item.snapshotJson, 'metricName')
      return {
        path: coverageType === 'LINKAGE' ? '/linkage-rule' : '/emergency-plan',
        query: {
          governanceAction: 'create',
          governanceSource: 'task',
          workItemCode: 'PENDING_LINKAGE_PLAN',
          coverageType,
          ...(dimensionKey ? { dimensionKey } : {}),
          ...(riskMetricId ? { riskMetricId } : {}),
          ...(metricIdentifier ? { metricIdentifier } : {}),
          ...(metricName ? { metricName } : {})
        }
      }
    }
    default:
      return null
  }
}
