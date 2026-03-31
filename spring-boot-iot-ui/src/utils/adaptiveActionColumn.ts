type ActionGap = 'compact' | 'comfortable' | 'wide'

interface AdaptiveActionColumnWidthOptions {
  directLabels: string[]
  menuLabel?: string | null
  gap?: ActionGap
  minWidth?: number
}

const ACTION_CELL_PADDING_PX = 40
const ACTION_TRIGGER_PADDING_PX = 16
const ACTION_GAP_PX: Record<ActionGap, number> = {
  compact: 4,
  comfortable: 8,
  wide: 12
}
const ACTION_MIN_WIDTH_PX = 96
const ACTION_WIDTH_STEP_PX = 8

function estimateActionLabelWidth(label: string) {
  const textWidth = Array.from(label).reduce((sum, character) => {
    if (/\s/.test(character)) {
      return sum + 4
    }
    if (/[A-Za-z0-9]/.test(character)) {
      return sum + 8
    }
    return sum + 14
  }, 0)

  return textWidth + ACTION_TRIGGER_PADDING_PX
}

function roundUpToStep(value: number, step: number) {
  return Math.ceil(value / step) * step
}

export function resolveAdaptiveActionColumnWidth({
  directLabels,
  menuLabel,
  gap = 'compact',
  minWidth = ACTION_MIN_WIDTH_PX
}: AdaptiveActionColumnWidthOptions) {
  const labels = [...directLabels, ...(menuLabel ? [menuLabel] : [])].filter((label) => label.trim().length > 0)
  if (labels.length === 0) {
    return minWidth
  }

  const gapWidth = Math.max(0, labels.length - 1) * ACTION_GAP_PX[gap]
  const labelWidth = labels.reduce((sum, label) => sum + estimateActionLabelWidth(label), 0)
  const resolvedWidth = ACTION_CELL_PADDING_PX + gapWidth + labelWidth

  return roundUpToStep(Math.max(minWidth, resolvedWidth), ACTION_WIDTH_STEP_PX)
}
