import { onActivated, onDeactivated, onMounted, onUnmounted } from 'vue'

export const STALE_REFRESH_MS = {
  community: 2 * 60 * 1000,
  commerce: 2 * 60 * 1000,
  detail: 30 * 1000,
  profile: 30 * 1000
}

const hasVisibleOverlay = () => {
  if (typeof window === 'undefined' || typeof document === 'undefined') return false
  return Array.from(document.querySelectorAll('.el-overlay, .modal-overlay'))
    .some((el) => {
      const style = window.getComputedStyle(el)
      return style.display !== 'none' && style.visibility !== 'hidden'
    })
}

export const useStaleRefresh = ({
  refresh,
  staleMs,
  isRefreshing,
  shouldSkip,
  skipWhenOverlay = true,
  coalesceMs = 250
}) => {
  let lastRefreshAt = Date.now()
  let running = false
  let timer = null
  let listening = false

  const markFresh = () => {
    lastRefreshAt = Date.now()
  }

  const clearScheduledCheck = () => {
    if (timer) {
      window.clearTimeout(timer)
      timer = null
    }
  }

  const canRefresh = () => {
    if (typeof document !== 'undefined' && document.visibilityState !== 'visible') return false
    if (running || isRefreshing?.()) return false
    if (skipWhenOverlay && hasVisibleOverlay()) return false
    if (shouldSkip?.()) return false
    return Date.now() - lastRefreshAt >= staleMs
  }

  const check = async () => {
    clearScheduledCheck()
    if (!canRefresh()) return false

    running = true
    lastRefreshAt = Date.now()
    try {
      await refresh()
      return true
    } catch (error) {
      console.warn('stale refresh failed', error)
      return false
    } finally {
      running = false
    }
  }

  const scheduleCheck = () => {
    if (timer || typeof window === 'undefined') return
    timer = window.setTimeout(() => {
      timer = null
      check()
    }, coalesceMs)
  }

  const addListeners = () => {
    if (listening || typeof window === 'undefined' || typeof document === 'undefined') return
    document.addEventListener('visibilitychange', scheduleCheck)
    window.addEventListener('focus', scheduleCheck)
    window.addEventListener('pageshow', scheduleCheck)
    listening = true
  }

  const removeListeners = () => {
    if (!listening || typeof window === 'undefined' || typeof document === 'undefined') return
    clearScheduledCheck()
    document.removeEventListener('visibilitychange', scheduleCheck)
    window.removeEventListener('focus', scheduleCheck)
    window.removeEventListener('pageshow', scheduleCheck)
    listening = false
  }

  onMounted(addListeners)
  onActivated(addListeners)
  onDeactivated(removeListeners)
  onUnmounted(removeListeners)

  return {
    check,
    markFresh
  }
}
