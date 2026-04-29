type ActionGap = 'compact' | 'comfortable' | 'wide'
type WorkbenchRowActionCommand = string | number | object
export const WORKBENCH_TABLE_ACTION_GAP: ActionGap = 'wide'

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

interface ResolveWorkbenchActionColumnWidthByRowsOptions {
  rows?: Array<SplitWorkbenchRowActionsOptions>
  fallback?: SplitWorkbenchRowActionsOptions
  maxDirectItems?: number
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
const WORKBENCH_TABLE_MIN_WIDTH_BY_VISIBLE_COUNT: Record<number, number> = {
  1: ACTION_MIN_WIDTH_PX,
  2: 112,
  3: 160
}
const ACTION_WIDTH_STEP_PX = 8
const DEFAULT_MAX_DIRECT_ITEMS = 3

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

function resolveWorkbenchTableMinWidth(visibleActionCount: number, fallbackMinWidth: number) {
  return Math.max(WORKBENCH_TABLE_MIN_WIDTH_BY_VISIBLE_COUNT[visibleActionCount] ?? fallbackMinWidth, fallbackMinWidth)
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
  gap: _gap = WORKBENCH_TABLE_ACTION_GAP,
  minWidth = ACTION_MIN_WIDTH_PX
}: ResolveWorkbenchActionColumnWidthOptions) {
  const resolvedActions = splitWorkbenchRowActions({
    directItems,
    menuItems,
    maxDirectItems
  })
  const visibleActionCount =
    resolvedActions.directItems.length + (resolvedActions.menuItems.length > 0 ? 1 : 0)

  return resolveAdaptiveActionColumnWidth({
    directLabels: resolvedActions.directItems.map((item) => item.label),
    menuLabel: resolvedActions.menuItems.length > 0 ? menuLabel : undefined,
    gap: WORKBENCH_TABLE_ACTION_GAP,
    minWidth: resolveWorkbenchTableMinWidth(visibleActionCount, minWidth)
  })
}

export function resolveWorkbenchActionColumnWidthByRows({
  rows = [],
  fallback,
  maxDirectItems = DEFAULT_MAX_DIRECT_ITEMS,
  menuLabel = '更多',
  gap = WORKBENCH_TABLE_ACTION_GAP,
  minWidth = ACTION_MIN_WIDTH_PX
}: ResolveWorkbenchActionColumnWidthByRowsOptions) {
  if (rows.length === 0) {
    return resolveWorkbenchActionColumnWidth({
      directItems: fallback?.directItems,
      menuItems: fallback?.menuItems,
      maxDirectItems,
      menuLabel,
      gap,
      minWidth
    })
  }

  return rows.reduce((maxWidth, row) => {
    const rowWidth = resolveWorkbenchActionColumnWidth({
      directItems: row.directItems,
      menuItems: row.menuItems,
      maxDirectItems,
      menuLabel,
      gap,
      minWidth
    })

    return Math.max(maxWidth, rowWidth)
  }, minWidth)
}
