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

export function buildGovernanceTaskDispatchLocation(item: GovernanceWorkItem): RouteLocationRaw | null {
  switch (item.workItemCode) {
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
          workItemCode: 'PENDING_CONTRACT_RELEASE'
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
          workItemCode: 'PENDING_RISK_BINDING'
        }
      }
    }
    default:
      return null
  }
}
