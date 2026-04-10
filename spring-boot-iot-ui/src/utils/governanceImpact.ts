import type {
  ProductContractReleaseEmergencyPlanBindingDetail,
  ProductContractReleaseLinkageBindingDetail,
  ProductContractReleaseRiskPointBindingDetail,
  ProductContractReleaseRuleDetail
} from '@/api/product'
import type { RouteLocationRaw } from 'vue-router'

function normalizeQueryText(value?: string | null) {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined
}

export function buildRiskPointContextLocation(
  detail: ProductContractReleaseRiskPointBindingDetail
): RouteLocationRaw {
  return {
    path: '/risk-point',
    query: {
      keyword: normalizeQueryText(detail.riskPointName) || normalizeQueryText(detail.deviceCode)
    }
  }
}

export function buildRuleContextLocation(detail: ProductContractReleaseRuleDetail): RouteLocationRaw {
  return {
    path: '/rule-definition',
    query: {
      ruleName: normalizeQueryText(detail.ruleName),
      metricIdentifier: normalizeQueryText(detail.metricIdentifier)
    }
  }
}

export function buildLinkageContextLocation(
  detail: ProductContractReleaseLinkageBindingDetail
): RouteLocationRaw {
  return {
    path: '/linkage-rule',
    query: {
      ruleName: normalizeQueryText(detail.linkageRuleName)
    }
  }
}

export function buildEmergencyPlanContextLocation(
  detail: ProductContractReleaseEmergencyPlanBindingDetail
): RouteLocationRaw {
  return {
    path: '/emergency-plan',
    query: {
      planName: normalizeQueryText(detail.emergencyPlanName),
      alarmLevel: normalizeQueryText(detail.alarmLevel)
    }
  }
}
