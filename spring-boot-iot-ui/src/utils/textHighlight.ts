export interface HighlightSegment {
  text: string
  matched: boolean
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

export function splitHighlightedText(text: string | null | undefined, keyword: string | null | undefined): HighlightSegment[] {
  const source = String(text || '')
  const normalizedKeyword = String(keyword || '').trim()
  if (!source || !normalizedKeyword) {
    return source ? [{ text: source, matched: false }] : []
  }

  const matcher = new RegExp(`(${escapeRegExp(normalizedKeyword)})`, 'gi')
  return source
    .split(matcher)
    .filter(Boolean)
    .map((segment) => ({
      text: segment,
      matched: segment.toLowerCase() === normalizedKeyword.toLowerCase()
    }))
}
