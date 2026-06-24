const hasActiveOverlay = () => {
  if (typeof document === 'undefined') return false
  return Array.from(document.querySelectorAll('.el-overlay, .modal-overlay'))
    .some((el) => {
      const style = window.getComputedStyle(el)
      return style.display !== 'none' && style.visibility !== 'hidden'
    })
}

export const releaseDocumentScrollIfNoOverlay = () => {
  if (typeof window === 'undefined' || typeof document === 'undefined') return

  window.requestAnimationFrame(() => {
    if (hasActiveOverlay()) return
    document.body.classList.remove('el-popup-parent--hidden')
    document.body.style.overflow = ''
    document.body.style.paddingRight = ''
  })
}
