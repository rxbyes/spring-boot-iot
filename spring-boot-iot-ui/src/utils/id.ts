import type { IdType } from '@/types/api';

export function normalizeOptionalId(value: unknown): IdType | undefined {
  if (value === undefined || value === null) {
    return undefined;
  }
  if (typeof value === 'string') {
    const trimmed = value.trim();
    return trimmed || undefined;
  }
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : undefined;
  }
  return undefined;
}

export function normalizeRequiredId(value: unknown): IdType | undefined {
  return normalizeOptionalId(value);
}

export function sameId(left: unknown, right: unknown): boolean {
  const normalizedLeft = normalizeOptionalId(left);
  const normalizedRight = normalizeOptionalId(right);
  return normalizedLeft !== undefined
    && normalizedRight !== undefined
    && String(normalizedLeft) === String(normalizedRight);
}

export function compareIdDesc(left: unknown, right: unknown): number {
  const leftText = String(normalizeOptionalId(left) ?? '');
  const rightText = String(normalizeOptionalId(right) ?? '');
  if (leftText === rightText) {
    return 0;
  }
  if (/^-?\d+$/.test(leftText) && /^-?\d+$/.test(rightText)) {
    if (leftText.length !== rightText.length) {
      return rightText.length - leftText.length;
    }
    return rightText.localeCompare(leftText);
  }
  return rightText.localeCompare(leftText);
}
