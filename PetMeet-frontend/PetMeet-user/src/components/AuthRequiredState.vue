<template>
  <section class="auth-required-state" :class="`auth-required-state--${type}`">
    <div class="auth-visual" aria-hidden="true">
      <el-icon>
        <component :is="iconComponent" />
      </el-icon>
    </div>

    <div class="auth-copy">
      <p class="eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
    </div>

    <div class="auth-actions">
      <el-button type="primary" class="login-action" @click="openLogin">立即登录</el-button>
      <el-button class="explore-action" text @click="goExplore">先逛逛</el-button>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { EditPen, Lock, ShoppingCart, UserFilled } from '@element-plus/icons-vue'

const props = defineProps({
  type: {
    type: String,
    default: 'default'
  },
  eyebrow: {
    type: String,
    default: '需要登录'
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    required: true
  }
})

const router = useRouter()
const userStore = useUserStore()

const iconMap = {
  cart: ShoppingCart,
  publish: EditPen,
  profile: UserFilled,
  default: Lock
}

const iconComponent = computed(() => iconMap[props.type] || iconMap.default)

const openLogin = () => {
  userStore.showLogin()
}

const goExplore = () => {
  router.push('/')
}
</script>

<style scoped lang="scss">
.auth-required-state {
  --auth-accent: #ff6b81;
  --auth-accent-deep: #f45a72;
  --auth-ink: #2b2326;
  --auth-muted: #7d6870;
  max-width: 760px;
  min-height: min(560px, calc(100vh - 120px));
  margin: 42px auto;
  padding: 54px 28px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  border-radius: 18px;
  background:
    radial-gradient(circle at 24% 18%, rgba(255, 205, 105, 0.2), transparent 28%),
    linear-gradient(140deg, #fff 0%, #fff8f9 48%, #f7fffb 100%);
  border: 1px solid rgba(255, 107, 129, 0.12);
  box-shadow: 0 18px 48px rgba(54, 36, 42, 0.08);
}

.auth-required-state--publish {
  background:
    radial-gradient(circle at 80% 20%, rgba(92, 168, 154, 0.16), transparent 30%),
    linear-gradient(140deg, #fff 0%, #fff9f4 45%, #f9fffd 100%);
}

.auth-required-state--profile {
  background:
    radial-gradient(circle at 18% 22%, rgba(127, 164, 189, 0.18), transparent 32%),
    linear-gradient(140deg, #fff 0%, #fff8f9 45%, #f7fbff 100%);
}

.auth-visual {
  width: 88px;
  height: 88px;
  border-radius: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, var(--auth-accent) 0%, #ff936f 100%);
  box-shadow: 0 16px 34px rgba(255, 107, 129, 0.26);

  .el-icon {
    font-size: 38px;
  }
}

.auth-copy {
  margin-top: 26px;
  max-width: 520px;

  .eyebrow {
    margin: 0 0 10px;
    font-size: 13px;
    font-weight: 700;
    color: var(--auth-accent-deep);
    letter-spacing: 0;
  }

  h1 {
    margin: 0;
    font-size: 28px;
    line-height: 1.25;
    color: var(--auth-ink);
    font-weight: 750;
    letter-spacing: 0;
  }

  p {
    margin: 12px 0 0;
    font-size: 15px;
    line-height: 1.8;
    color: var(--auth-muted);
  }
}

.auth-actions {
  margin-top: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.login-action {
  min-width: 132px;
  height: 42px;
  border: none;
  border-radius: 999px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--auth-accent) 0%, #ff7a66 100%);
  box-shadow: 0 10px 22px rgba(255, 107, 129, 0.22);

  &:hover,
  &:focus {
    background: linear-gradient(135deg, #ff5c74 0%, #ff6f5e 100%);
  }
}

.explore-action {
  height: 42px;
  padding: 0 18px;
  color: #6b5a61;
  border-radius: 999px;

  &:hover,
  &:focus {
    color: var(--auth-accent-deep);
    background: rgba(255, 107, 129, 0.08);
  }
}

@media (max-width: 720px) {
  .auth-required-state {
    min-height: calc(100vh - 96px);
    margin: 24px 12px;
    padding: 42px 20px;
  }

  .auth-copy h1 {
    font-size: 24px;
  }
}
</style>
