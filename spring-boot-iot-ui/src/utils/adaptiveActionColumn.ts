type ActionGap = 'compact' | 'comfortable' | 'wide'
type WorkbenchRowActionCommand = string | number | object

interface WorkbenchDirectActionItem {
  key?: string
  command: WorkbenchRowActionCommand
  label: string
  disabled?: boolean
}

interface WorkbenchMenuActionItem {
  key?: string
  command: WorkbenchRowActionCommand
  label: string
  disabled?: boolean
  divided?: boolean
}

interface AdaptiveActionColumnWidthOptions {
  directLabels: string[]
  menuLabel?: string | null
  gap?: ActionGap
  minWidth?: number
}

interface SplitWorkbenchRowActionsOptions {
  directItems?: WorkbenchDirectActionItem[]
  menuItems?: WorkbenchMenuActionItem[]
  maxDirectItems?: number
}

interface ResolveWorkbenchActionColumnWidthOptions extends SplitWorkbenchRowActionsOptions {
  menuLabel?: string | null
  gap?: ActionGap
  minWidth?: number
}

const ACTION_CELL_PADDING_PX = 32
const ACTION_TRIGGER_PADDING_PX = 4
const ACTION_GAP_PX: Record<ActionGap, number> = {
  compact: 4,
  comfortable: 8,
  wide: 12
}
const ACTION_MIN_WIDTH_PX = 96
const ACTION_WIDTH_STEP_PX = 8
const DEFAULT_MAX_DIRECT_ITEMS = 2

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

export function splitWorkbenchRowActions({
  directItems = [],
  menuItems = [],
  maxDirectItems = DEFAULT_MAX_DIRECT_ITEMS
}: SplitWorkbenchRowActionsOptions) {
  const safeMaxDirectItems = Math.max(0, maxDirectItems)
  const visibleDirectItems = directItems.slice(0, safeMaxDirectItems)
  const overflowMenuItems = directItems.slice(safeMaxDirectItems).map((item) => ({
    key: item.key,
    command: item.command,
    label: item.label,
    disabled: item.disabled
  }))

  return {
    directItems: visibleDirectItems,
    menuItems: [...overflowMenuItems, ...menuItems]
  }
}

export function resolveWorkbenchActionColumnWidth({
  directItems = [],
  menuItems = [],
  maxDirectItems = DEFAULT_MAX_DIRECT_ITEMS,
  menuLabel = '更多',
  gap = 'compact',
  minWidth = ACTION_MIN_WIDTH_PX
}: ResolveWorkbenchActionColumnWidthOptions) {
  const resolvedActions = splitWorkbenchRowActions({
    directItems,
    menuItems,
    maxDirectItems
  })

  return resolveAdaptiveActionColumnWidth({
    directLabels: resolvedActions.directItems.map((item) => item.label),
    menuLabel: resolvedActions.menuItems.length > 0 ? menuLabel : undefined,
    gap,
    minWidth
  })
}
