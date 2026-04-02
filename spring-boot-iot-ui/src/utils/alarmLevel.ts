export type AlarmLevelTagType = 'danger' | 'warning' | 'success' | 'info'

export function normalizeAlarmLevel(value?: string | null) {
  switch ((value || '').trim().toLowerCase()) {
    case 'red':
    case 'critical':
      return 'red'
    case 'orange':
    case 'high':
    case 'warning':
      return 'orange'
    case 'yellow':
    case 'medium':
      return 'yellow'
    case 'blue':
    case 'low':
    case 'info':
      return 'blue'
    default:
      return (value || '').trim().toLowerCase()
  }
}

export function getAlarmLevelText(value?: string | null) {
  switch (normalizeAlarmLevel(value)) {
    case 'red':
      return '红色'
    case 'orange':
      return '橙色'
    case 'yellow':
      return '黄色'
    case 'blue':
      return '蓝色'
    default:
      return value || ''
  }
}

export function getAlarmLevelTagType(value?: string | null): AlarmLevelTagType {
  switch (normalizeAlarmLevel(value)) {
    case 'red':
      return 'danger'
    case 'orange':
      return 'warning'
    case 'yellow':
      return 'success'
    default:
      return 'info'
  }
}
