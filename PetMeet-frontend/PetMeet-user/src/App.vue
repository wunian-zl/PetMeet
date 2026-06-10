<template>
  <router-view />
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'

let hasHidden = false
let reloading = false

const handleVisibilityChange = () => {
  if (document.visibilityState === 'hidden') {
    hasHidden = true
    return
  }
  if (document.visibilityState === 'visible' && hasHidden && !reloading) {
    reloading = true
    window.location.reload()
  }
}

onMounted(() => {
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style>
/* 全局样式或重置样式可以放在 style.css，或者写在这里的不带 scoped 样式里 */
</style>
