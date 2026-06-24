<template>
  <router-view />
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { releaseDocumentScrollIfNoOverlay } from '@/utils/scrollLock'

const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    releaseDocumentScrollIfNoOverlay()
  }
}

onMounted(() => {
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('pageshow', releaseDocumentScrollIfNoOverlay)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('pageshow', releaseDocumentScrollIfNoOverlay)
})
</script>

<style>
/* 全局样式或重置样式可以放在 style.css，或者写在这里的不带 scoped 样式里 */
</style>
