<template>
  <footer class="beian-footer" aria-label="网站备案信息">
    <a
      class="beian-link"
      href="https://beian.miit.gov.cn/"
      target="_blank"
      rel="noopener noreferrer"
    >
      {{ icpBeianNo }}
    </a>
    <template v-if="policeBeianNo">
      <span class="beian-separator">|</span>
      <a
        class="beian-link"
        :href="policeBeianUrl"
        target="_blank"
        rel="noopener noreferrer"
      >
        {{ policeBeianNo }}
      </a>
    </template>
  </footer>
</template>

<script setup>
const icpBeianNo = (import.meta.env.VITE_ICP_BEIAN_NO || '蜀ICP备2026033981号-1').trim()
const policeBeianNo = (import.meta.env.VITE_POLICE_BEIAN_NO || '').trim()
const policeBeianUrl = (import.meta.env.VITE_POLICE_BEIAN_URL || 'https://beian.mps.gov.cn/#/query/webSearch').trim()
</script>

<style scoped lang="scss">
.beian-footer {
  width: 100%;
  box-sizing: border-box;
  padding: 18px 24px 22px;
  color: #9aa1ad;
  font-size: 12px;
  line-height: 1.7;
  text-align: center;
  background: #fff;
}

.beian-link {
  color: inherit;
  text-decoration: none;
  transition: color 0.2s ease;

  &:hover,
  &:focus-visible {
    color: #ff6b81;
    text-decoration: underline;
  }
}

.beian-separator {
  display: inline-block;
  margin: 0 8px;
  color: #d0d5dd;
}
</style>
