<template>
  <svg
    class="heart-icon"
    viewBox="0 0 1024 1024"
    :fill="filled ? 'currentColor' : 'none'"
    stroke="currentColor"
    stroke-width="60"
    stroke-linecap="round"
    stroke-linejoin="round"
  >
    <path
      d="M512 888L144 520c-96-96-96-252 0-348 96-96 252-96 348 0l20 20 20-20c96-96 252-96 348 0 96 96 96 252 0 348L512 888z"
    />
  </svg>
</template>

<script setup>
defineProps({
  filled: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.heart-icon {
  width: 1em;
  height: 1em;
}
</style>
