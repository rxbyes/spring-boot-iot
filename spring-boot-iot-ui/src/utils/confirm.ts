import { ElMessageBox } from '@/utils/messageBox'

export interface ConfirmActionOptions {
  message: string
  title?: string
  type?: '' | 'primary' | 'success' | 'warning' | 'info' | 'error'
  confirmButtonText?: string
  cancelButtonText?: string
  closeOnClickModal?: boolean
  closeOnPressEscape?: boolean
}

export function confirmAction(options: ConfirmActionOptions) {
  return ElMessageBox.confirm(options.message, options.title ?? '确认操作', {
    type: options.type ?? 'warning',
    confirmButtonText: options.confirmButtonText ?? '确认',
    cancelButtonText: options.cancelButtonText ?? '取消',
    autofocus: false,
    customClass: 'standard-confirm-box',
    closeOnClickModal: options.closeOnClickModal ?? false,
    closeOnPressEscape: options.closeOnPressEscape ?? true
  })
}

export function confirmDelete(entityLabel: string, entityName?: string) {
  const normalizedName = entityName?.trim()
  const message = normalizedName
    ? `确认删除${entityLabel}“${normalizedName}”吗？删除后不可恢复。`
    : `确认删除该${entityLabel}吗？删除后不可恢复。`

  return confirmAction({
    title: `删除${entityLabel}`,
    message,
    type: 'warning',
    confirmButtonText: '确认删除'
  })
}

export function isConfirmCancelled(error: unknown) {
  return error === 'cancel' || error === 'close' || (error instanceof Error && ['cancel', 'close'].includes(error.message))
}
