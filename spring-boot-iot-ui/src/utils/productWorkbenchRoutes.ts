import type { IdType } from '@/types/api'

export type ProductWorkbenchSection =
  | 'overview'
  | 'devices'
  | 'contracts'
  | 'mapping-rules'
  | 'releases'

export type LegacyProductWorkbenchView = 'overview' | 'devices' | 'models' | 'edit'

function normalizeId(productId: IdType | null | undefined) {
  if (productId == null) {
    return ''
  }
  return String(productId).trim()
}

export function normalizeProductWorkbenchSection(section?: string | null): ProductWorkbenchSection {
  switch ((section || '').trim()) {
    case 'devices':
      return 'devices'
    case 'contracts':
      return 'contracts'
    case 'mapping-rules':
      return 'mapping-rules'
    case 'releases':
      return 'releases'
    default:
      return 'overview'
  }
}

export function mapLegacyProductWorkbenchView(
  view?: string | null
): ProductWorkbenchSection | 'edit' {
  switch ((view || '').trim()) {
    case 'devices':
      return 'devices'
    case 'models':
      return 'contracts'
    case 'edit':
      return 'edit'
    default:
      return 'overview'
  }
}

export function buildProductWorkbenchSectionPath(
  productId: IdType | null | undefined,
  section: ProductWorkbenchSection = 'overview'
) {
  const normalizedId = normalizeId(productId)
  if (!normalizedId) {
    return '/products'
  }
  return `/products/${normalizedId}/${section}`
}

export function buildLegacyProductWorkbenchPath(
  productId: IdType | null | undefined,
  view: LegacyProductWorkbenchView = 'models'
) {
  const normalizedId = normalizeId(productId)
  const query = new URLSearchParams()
  if (normalizedId) {
    query.set('openProductId', normalizedId)
  }
  query.set('workbenchView', view)
  return `/products?${query.toString()}`
}

export function buildProductWorkbenchPathFromLegacyView(
  productId: IdType | null | undefined,
  view: LegacyProductWorkbenchView = 'models'
) {
  const mappedView = mapLegacyProductWorkbenchView(view)
  if (mappedView === 'edit') {
    return buildLegacyProductWorkbenchPath(productId, 'edit')
  }
  return buildProductWorkbenchSectionPath(productId, mappedView)
}
