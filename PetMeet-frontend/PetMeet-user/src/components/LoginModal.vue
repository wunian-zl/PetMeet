<template>
  <el-dialog
    v-model="userStore.loginVisible"
    width="min(900px, 92vw)"
    :show-close="false"
    class="login-dialog"
    modal-class="login-overlay"
    align-center
    destroy-on-close
    @closed="handleClosed"
  >
    <div class="login-container">
      <div class="poster-section">
        <div class="poster-content-wrapper" :style="posterStyle">
          <div class="poster-overlay"></div>
          <div class="poster-text">
            <div class="logo">PetMeet</div>
            <div class="slogan">发现身边的美好宠物生活</div>
          </div>
        </div>
      </div>

      <div class="form-section">
        <div class="close-btn" @click="handleCloseClick">
          <el-icon><Close /></el-icon>
        </div>

        <div class="welcome-header">
          <h1 class="welcome-title">欢迎回来</h1>
          <p class="welcome-subtitle">登录账号，解锁完整功能</p>
        </div>

        <div class="form-content">
          <div class="input-group">
            <el-icon class="input-icon"><User /></el-icon>
            <input
              v-model.trim="loginForm.username"
              type="text"
              placeholder="请输入用户名"
              class="capsule-input"
            />
          </div>
          <div class="input-group">
            <el-icon class="input-icon"><Lock /></el-icon>
            <input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              class="capsule-input"
              @keyup.enter="handleLogin"
            />
          </div>
          <div class="auto-register-tip">
            若账号未注册，首次登录会自动注册并完成登录（用户名2-20位，支持汉字/字母/数字/下划线，密码8-64位且包含字母和数字）。
          </div>
          <button class="submit-btn" @click="handleLogin" :disabled="loading">
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </div>

        <div class="agreement">
          登录即代表同意 <a href="#">《用户协议》</a> 和 <a href="#">《隐私政策》</a>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close, User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import loginPosterUrl from '@/assets/login-poster.jpg'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const authEntryPaths = ['/cart', '/publish', '/profile']
const posterStyle = {
  backgroundImage: `url(${loginPosterUrl})`
}

const loginForm = ref({
  username: '',
  password: ''
})

const handleCloseClick = () => {
  userStore.hideLogin()
}

const handleClosed = () => {
  if (!userStore.isLoggedIn && authEntryPaths.includes(route.path)) {
    router.replace('/')
  }
}

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请填写完整信息')
    return
  }

  loading.value = true
  try {
    await userStore.login(loginForm.value)
    userStore.hideLogin()
    ElMessage.success('登录成功')
  } catch (error) {
    // 错误由请求拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss">
.login-overlay {
  background: rgba(17, 24, 39, 0.42) !important;
  backdrop-filter: blur(5px);
}

.el-dialog.login-dialog,
.login-dialog .el-dialog {
  border-radius: 40px !important;
  overflow: hidden !important;
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow: 0 30px 80px rgba(15, 23, 42, 0.34) !important;
  background: #fff;
  animation: loginDialogPop 0.24s cubic-bezier(0.22, 1, 0.36, 1);
}

.el-dialog.login-dialog .el-dialog__header,
.login-dialog .el-dialog__header {
  display: none;
}

.el-dialog.login-dialog .el-dialog__body,
.login-dialog .el-dialog__body {
  padding: 0 !important;
  position: relative;
  overflow: hidden;
  border-radius: inherit;
}

@keyframes loginDialogPop {
  from {
    opacity: 0;
    transform: translateY(14px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
</style>

<style scoped lang="scss">
.login-container {
  display: flex;
  border-radius: 36px;
  overflow: hidden;
  background: #fff;
  min-height: 540px;
  box-sizing: border-box;
}

.poster-section {
  width: 40%;
  padding: 10px;
  box-sizing: border-box;
  background: #fff;
}

.poster-content-wrapper {
  position: relative;
  height: 100%;
  border-radius: 30px;
  overflow: hidden;
  background-color: #d1d5db;
  background-size: cover;
  background-position: center;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.35);
}

.poster-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.08), rgba(0, 0, 0, 0.38));
}

.poster-text {
  position: absolute;
  left: 20px;
  right: 20px;
  bottom: 24px;
  color: #fff;
  z-index: 1;
}

.logo {
  font-size: 30px;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.slogan {
  margin-top: 8px;
  font-size: 14px;
  opacity: 0.95;
}

.form-section {
  flex: 1;
  padding: 42px 48px 36px;
  display: flex;
  flex-direction: column;
  position: relative;
  background: #fff;
  border-radius: 30px;
}

.close-btn {
  position: absolute;
  top: 18px;
  right: 18px;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a0a0a0;
  cursor: pointer;
  border: 1px solid #eceff4;
  background: rgba(255, 255, 255, 0.95);
  transition: all 0.2s ease;

  &:hover {
    background: #fff;
    color: #666;
    border-color: #dce2eb;
    transform: rotate(90deg);
  }
}

.welcome-header {
  margin-bottom: 20px;
}

.welcome-title {
  margin: 0;
  font-size: 30px;
  color: #1f2937;
}

.welcome-subtitle {
  margin: 10px 0 0;
  color: #8a8f99;
  font-size: 14px;
}

.form-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.input-group {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 18px;
  top: 50%;
  transform: translateY(-50%);
  color: #b7bcc5;
}

.capsule-input {
  width: 100%;
  height: 50px;
  border: 1px solid #e6e8eb;
  background: #f9fafb;
  border-radius: 999px;
  padding: 0 18px 0 48px;
  box-sizing: border-box;
  outline: none;
  transition: all 0.22s ease;

  &:focus {
    border-color: #ff9aaa;
    background: #fff;
    box-shadow: 0 0 0 4px rgba(255, 107, 129, 0.12);
  }
}

.auto-register-tip {
  margin-top: -2px;
  color: #8a8f99;
  font-size: 12px;
  line-height: 1.5;
}

.submit-btn {
  width: 100%;
  height: 50px;
  border: none;
  border-radius: 999px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #ff8d96 0%, #ff5c5c 100%);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 10px 18px rgba(255, 92, 92, 0.25);
  }

  &:disabled {
    opacity: 0.75;
    cursor: not-allowed;
    transform: none;
    box-shadow: none;
  }
}

.agreement {
  margin-top: auto;
  text-align: center;
  color: #9ca3af;
  font-size: 12px;

  a {
    color: #ff6b81;
    text-decoration: none;
  }
}

@media (max-width: 900px) {
  .login-container {
    flex-direction: column;
    min-height: auto;
    border-radius: 26px;
  }

  .poster-section {
    width: 100%;
    height: 180px;
  }

  .form-section {
    padding: 24px 20px 26px;
  }
}
</style>
