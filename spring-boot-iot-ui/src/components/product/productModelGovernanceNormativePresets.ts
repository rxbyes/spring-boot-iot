import type { Product } from '@/types/api'

export interface ProductModelGovernanceNormativePresetItem {
  identifier: string
  label: string
}

export interface ProductModelGovernanceNormativePresetMatcher {
  productKeys?: string[]
  productKeyKeywords?: string[]
  productNameKeywords?: string[]
  excludedKeywords?: string[]
}

export interface ProductModelGovernanceNormativePreset {
  code: string
  title: string
  description: string
  helperText: string
  defaultIdentifiers: string[]
  availableIdentifiers: ProductModelGovernanceNormativePresetItem[]
  matcher: ProductModelGovernanceNormativePresetMatcher
}

export const INTEGRATED_NORMATIVE_PRESET: ProductModelGovernanceNormativePreset = {
  code: 'landslide-integrated-tilt-accel-crack-v1',
  title: '倾角 / 加速度 / 裂缝一体机',
  description: '首批只治理 L1 变形监测与设备状态参数，正式模型统一采用规范化 identifier。',
  helperText: '默认先勾选倾角核心字段，样本 JSON 继续保留为辅助核对工具，可逐步扩展到加速度、裂缝与设备状态参数。',
  defaultIdentifiers: ['L1_QJ_1.X', 'L1_QJ_1.Y', 'L1_QJ_1.Z', 'L1_QJ_1.angle', 'L1_QJ_1.AZI'],
  availableIdentifiers: [
    { identifier: 'L1_QJ_1.X', label: '倾角 X 轴' },
    { identifier: 'L1_QJ_1.Y', label: '倾角 Y 轴' },
    { identifier: 'L1_QJ_1.Z', label: '倾角 Z 轴' },
    { identifier: 'L1_QJ_1.angle', label: '倾角平面夹角' },
    { identifier: 'L1_QJ_1.AZI', label: '倾角方位角' },
    { identifier: 'L1_JS_1.gX', label: '加速度 X 轴' },
    { identifier: 'L1_JS_1.gY', label: '加速度 Y 轴' },
    { identifier: 'L1_JS_1.gZ', label: '加速度 Z 轴' },
    { identifier: 'L1_LF_1.value', label: '裂缝张开度' },
    { identifier: 'S1_ZT_1.signal_4g', label: '4G 信号强度' }
  ],
  matcher: {
    productKeys: ['south-survey-multi-detector-v1'],
    productKeyKeywords: ['multi-detector', 'tilt-accel-crack'],
    productNameKeywords: ['多维检测仪', '一体机'],
    excludedKeywords: ['warning', '预警', '声光报警', '广播', '爆闪灯', '情报板']
  }
}

export const PRODUCT_MODEL_GOVERNANCE_NORMATIVE_PRESETS = [INTEGRATED_NORMATIVE_PRESET]

function normalize(value?: string | null) {
  return value?.trim().toLowerCase() ?? ''
}

export function resolveApplicableNormativePreset(
  product?: Pick<Product, 'productKey' | 'productName'> | null
) {
  const productKey = normalize(product?.productKey)
  const productName = normalize(product?.productName)
  return PRODUCT_MODEL_GOVERNANCE_NORMATIVE_PRESETS.find((preset) => {
    const excluded = preset.matcher.excludedKeywords?.some((keyword) =>
      productKey.includes(normalize(keyword)) || productName.includes(normalize(keyword))
    )
    if (excluded) {
      return false
    }
    const exactMatch = preset.matcher.productKeys?.map(normalize).includes(productKey)
    const keyKeywordMatch = preset.matcher.productKeyKeywords?.some((keyword) =>
      productKey.includes(normalize(keyword))
    )
    const nameKeywordMatch = preset.matcher.productNameKeywords?.some((keyword) =>
      productName.includes(normalize(keyword))
    )
    return Boolean(exactMatch || keyKeywordMatch || nameKeywordMatch)
  }) ?? null
}
