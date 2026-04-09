import type { RouteLocationNormalizedLoaded, RouterScrollBehavior } from 'vue-router'

export function resolveAppScrollBehavior(
  to: Pick<RouteLocationNormalizedLoaded, 'path' | 'fullPath' | 'hash'>,
  from: Pick<RouteLocationNormalizedLoaded, 'path' | 'fullPath' | 'hash'>,
  savedPosition?: { left: number; top: number } | null
) {
  if (savedPosition) {
    return savedPosition
  }

  if (to.hash) {
    return { el: to.hash, top: 88 }
  }

  if (to.path === from.path && to.fullPath !== from.fullPath) {
    return false
  }

  return { top: 0 }
}

export const appScrollBehavior: RouterScrollBehavior = (to, from, savedPosition) =>
  resolveAppScrollBehavior(to, from, savedPosition)
