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
          <div class="field-block">
            <div class="input-group">
              <el-icon class="input-icon"><User /></el-icon>
              <input
                v-model.trim="loginForm.username"
                type="text"
                placeholder="请输入用户名"
                class="capsule-input"
              />
            </div>
            <p class="field-hint">用户名2-20位，仅支持字母、数字、下划线</p>
          </div>
          <div class="field-block">
            <div class="input-group">
              <el-icon class="input-icon"><Lock /></el-icon>
              <input
                v-model="loginForm.password"
                :type="passwordVisible ? 'text' : 'password'"
                placeholder="请输入密码"
                class="capsule-input password-input"
                @keyup.enter="handleLogin"
              />
              <button
                class="password-toggle"
                type="button"
                :aria-label="passwordVisible ? '隐藏密码' : '显示密码'"
                @click="passwordVisible = !passwordVisible"
              >
                <el-icon><View v-if="!passwordVisible" /><Hide v-else /></el-icon>
              </button>
            </div>
            <p class="field-hint">密码8-18位，需同时包含字母和数字</p>
          </div>
          <div class="login-actions">
            <button class="forgot-btn" type="button" @click="openResetDialog">忘记密码?</button>
          </div>
          <div class="auto-register-tip">
            若账号未注册，会先让你完善绑定信息，提交后完成注册并登录。
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

  <el-dialog
    v-model="resetDialogVisible"
    title="找回密码"
    width="min(390px, 92vw)"
    destroy-on-close
    align-center
    class="reset-password-dialog"
  >
    <el-form label-position="top" class="reset-form">
      <el-form-item label="用户名">
        <el-input v-model.trim="resetForm.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="已绑定手机号或邮箱">
        <el-input v-model.trim="resetForm.contact" placeholder="请输入手机号或邮箱" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input
          v-model="resetForm.newPassword"
          type="password"
          show-password
          placeholder="8-18位，包含字母和数字"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="resetDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="resetLoading" @click="handleResetPassword">重置密码</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="registerDialogVisible"
    title="完善注册资料"
    width="min(400px, 92vw)"
    destroy-on-close
    align-center
    class="register-profile-dialog"
    @closed="resetRegisterForm"
  >
    <div class="register-notice">
      这是一个未注册的新账号。请绑定手机号或邮箱，提交后会直接完成注册并登录。
    </div>
    <el-form label-position="top" class="register-profile-form">
      <el-form-item label="登录用户名">
        <el-input v-model.trim="registerForm.username" placeholder="2-20位，仅支持字母/数字/下划线" />
      </el-form-item>
      <el-form-item label="登录密码">
        <el-input
          v-model="registerForm.password"
          type="password"
          show-password
          placeholder="8-18位，包含字母和数字"
        />
      </el-form-item>
      <el-form-item label="展示昵称">
        <el-input v-model.trim="registerForm.nickname" maxlength="20" show-word-limit placeholder="别人看到的昵称" />
      </el-form-item>
      <el-form-item label="绑定手机号">
        <el-input v-model.trim="registerForm.phone" placeholder="手机号或邮箱至少填一个" />
      </el-form-item>
      <el-form-item label="绑定邮箱">
        <el-input v-model.trim="registerForm.email" placeholder="手机号或邮箱至少填一个" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="register-dialog-footer">
        <el-button
          link
          type="primary"
          :disabled="registerLoading"
          @click="returnToLoginFromRegister"
        >
          已有账号，返回登录
        </el-button>
        <div class="register-dialog-actions">
          <el-button type="primary" :loading="registerLoading" @click="handleCompleteRegister">完成注册并登录</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close, User, Lock, View, Hide } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import loginPosterUrl from '@/assets/login-poster.jpg'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const resetLoading = ref(false)
const registerLoading = ref(false)
const passwordVisible = ref(false)
const resetDialogVisible = ref(false)
const registerDialogVisible = ref(false)
const authEntryPaths = ['/cart', '/publish', '/profile']
const posterStyle = {
  backgroundImage: `url(${loginPosterUrl})`
}
const reservedUsernames = [
  'admin',
  'administrator',
  'root',
  'system',
  'official',
  'petmeet',
  '客服',
  '管理员',
  '系统'
]

const loginForm = ref({
  username: '',
  password: ''
})

const resetForm = ref({
  username: '',
  contact: '',
  newPassword: ''
})

const registerForm = ref({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: ''
})

const handleCloseClick = () => {
  userStore.loginRedirect = ''
  userStore.hideLogin()
}

const handleClosed = () => {
  if (!userStore.isLoggedIn && authEntryPaths.includes(route.path) && !userStore.loginRedirect) {
    router.replace('/')
  }
}

const goAfterAuth = () => {
  const target = userStore.loginRedirect || ''
  userStore.loginRedirect = ''
  if (target && target.startsWith('/') && target !== route.fullPath) {
    router.replace(target)
  }
}

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请填写完整信息')
    return
  }

  loading.value = true
  try {
    await userStore.login(loginForm.value, { silentError: true })
    userStore.hideLogin()
    goAfterAuth()
    ElMessage.success('登录成功')
  } catch (error) {
    if (error?.code === 404 || error?.response?.code === 404) {
      openRegisterDialog()
    } else {
      ElMessage.error(error?.message || '登录失败')
    }
  } finally {
    loading.value = false
  }
}

const openRegisterDialog = () => {
  if (!isValidUsername(loginForm.value.username || '')) {
    ElMessage.warning('用户名需为2-20位，仅支持字母、数字或下划线')
    return
  }
  if (!isStrongPassword(loginForm.value.password || '')) {
    ElMessage.warning('密码必须为8-18位，且同时包含字母和数字')
    return
  }
  registerForm.value.username = loginForm.value.username || ''
  registerForm.value.password = loginForm.value.password || ''
  registerForm.value.nickname = loginForm.value.username || ''
  registerForm.value.phone = ''
  registerForm.value.email = ''
  registerDialogVisible.value = true
}

const resetRegisterForm = () => {
  registerForm.value.username = ''
  registerForm.value.password = ''
  registerForm.value.nickname = ''
  registerForm.value.phone = ''
  registerForm.value.email = ''
}

const returnToLoginFromRegister = () => {
  loginForm.value.username = registerForm.value.username || loginForm.value.username
  loginForm.value.password = ''
  passwordVisible.value = false
  registerDialogVisible.value = false
}

const openResetDialog = () => {
  resetForm.value.username = loginForm.value.username || ''
  resetForm.value.contact = ''
  resetForm.value.newPassword = ''
  resetDialogVisible.value = true
}

const handleResetPassword = async () => {
  const { username, contact, newPassword } = resetForm.value
  if (!username || !contact || !newPassword) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (newPassword.length < 8 || newPassword.length > 18 || !/[A-Za-z]/.test(newPassword) || !/\d/.test(newPassword)) {
    ElMessage.warning('密码必须为8-18位，且同时包含字母和数字')
    return
  }

  resetLoading.value = true
  try {
    await request.post('/auth/reset-password', {
      username,
      contact,
      newPassword
    })
    loginForm.value.username = username
    loginForm.value.password = ''
    resetDialogVisible.value = false
    ElMessage.success('密码已重置，请重新登录')
  } finally {
    resetLoading.value = false
  }
}

const isValidUsername = (value) => value.length >= 2
  && value.length <= 20
  && /^[A-Za-z0-9_]+$/.test(value)
const isReservedUsername = (value) => reservedUsernames.includes(value.trim().toLowerCase())

const isStrongPassword = (value) => value.length >= 8
  && value.length <= 18
  && /[A-Za-z]/.test(value)
  && /\d/.test(value)

const isValidPhone = (value) => !value || /^1[3-9]\d{9}$/.test(value)
const isValidEmail = (value) => !value || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)

const handleCompleteRegister = async () => {
  const form = registerForm.value
  if (!isValidUsername(form.username)) {
    ElMessage.warning('用户名需为2-20位，仅支持字母、数字或下划线')
    return
  }
  if (isReservedUsername(form.username)) {
    ElMessage.warning('该用户名不可使用，请换一个')
    return
  }
  if (!isStrongPassword(form.password)) {
    ElMessage.warning('密码必须为8-18位，且同时包含字母和数字')
    return
  }
  if (!form.phone && !form.email) {
    ElMessage.warning('请至少绑定手机号或邮箱')
    return
  }
  if (!isValidPhone(form.phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  if (!isValidEmail(form.email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }

  registerLoading.value = true
  try {
    await userStore.register({
      username: form.username.trim(),
      password: form.password,
      nickname: form.nickname?.trim() || form.username.trim(),
      phone: form.phone || null,
      email: form.email || null
    })
    loginForm.value.username = form.username.trim()
    loginForm.value.password = ''
    registerDialogVisible.value = false
    userStore.hideLogin()
    goAfterAuth()
    ElMessage.success('注册并登录成功')
  } finally {
    registerLoading.value = false
  }
}
</script>

<style lang="scss">
.login-overlay {
  background: rgba(17, 24, 39, 0.48) !important;
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
  gap: 12px;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-hint {
  margin: 0;
  padding-left: 18px;
  color: #9aa1ad;
  font-size: 12px;
  line-height: 1.45;
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

.password-input {
  padding-right: 48px;
}

.password-toggle {
  position: absolute;
  right: 16px;
  top: 50%;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: translateY(-50%);
  transition: color 0.2s ease;

  &:hover {
    color: #ff6b81;
  }
}

.login-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: -8px;
}

.forgot-btn {
  border: none;
  background: transparent;
  color: #ff6b81;
  cursor: pointer;
  font-size: 13px;
  padding: 0;

  &:hover {
    text-decoration: underline;
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
  background: #f06478;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    background: #e7576c;
    box-shadow: 0 10px 18px rgba(240, 100, 120, 0.22);
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

<style lang="scss">
.reset-password-dialog,
.register-profile-dialog {
  --el-color-primary: #f06478;
  --el-color-primary-dark-2: #d94e62;
  --el-color-primary-light-3: #f58b99;
  --el-color-primary-light-5: #f8aebb;
  --el-color-primary-light-7: #fbd0d7;
  --el-color-primary-light-8: #fde1e5;
  --el-color-primary-light-9: #fff3f5;
  --el-input-focus-border-color: #f06478;
  --el-button-outline-color: rgba(240, 100, 120, 0.18);
  border-radius: 18px !important;
  border: 1px solid #eceff3;
  box-shadow: 0 22px 60px rgba(15, 23, 42, 0.18) !important;
  overflow: hidden;

  .el-dialog__header {
    padding: 22px 24px 8px;
    margin: 0;
  }

  .el-dialog__title {
    color: #2f343d;
    font-size: 22px;
    font-weight: 650;
    line-height: 1.25;
  }

  .el-dialog__headerbtn {
    top: 18px;
    right: 18px;
    width: 30px;
    height: 30px;

    .el-dialog__close {
      color: #9aa1ad;
      font-size: 22px;
      transition: color 0.2s ease;
    }

    &:hover .el-dialog__close {
      color: #4b5563;
    }
  }

  .el-dialog__body {
    padding: 8px 24px 12px;
  }

  .el-dialog__footer {
    padding: 6px 24px 24px;
  }

  .el-form-item {
    margin-bottom: 14px;
  }

  .el-form-item__label {
    color: #4b5563;
    font-size: 14px;
    font-weight: 600;
    line-height: 1.2;
    margin-bottom: 8px;
  }

  .el-input__wrapper {
    height: 40px;
    border-radius: 10px;
    background: #fff;
    box-shadow: 0 0 0 1px #d8dde6 inset;
    padding: 0 14px;
    transition: box-shadow 0.18s ease, background 0.18s ease;
  }

  .el-input__wrapper:hover {
    box-shadow: 0 0 0 1px #cbd2dd inset;
  }

  .el-input__wrapper.is-focus {
    box-shadow: 0 0 0 1px #f06478 inset;
  }

  .el-input__inner {
    color: #333842;
    font-size: 15px;
  }

  .el-input__inner::placeholder {
    color: #a2a9b5;
  }

  .el-button--primary {
    --el-button-bg-color: #ff6b81;
    --el-button-border-color: #ff6b81;
    --el-button-hover-bg-color: #e7576c;
    --el-button-hover-border-color: #e7576c;
    --el-button-active-bg-color: #d94e62;
    --el-button-active-border-color: #d94e62;
    --el-button-disabled-bg-color: #f6a5b1;
    --el-button-disabled-border-color: #f6a5b1;
    border-radius: 10px;
    font-weight: 600;
    min-height: 38px;
    padding: 9px 18px;
    box-shadow: none;
  }

  .el-button--primary:focus,
  .el-button--primary:focus-visible {
    background-color: #f06478;
    border-color: #f06478;
    color: #fff;
    outline: 2px solid rgba(240, 100, 120, 0.2);
    outline-offset: 2px;
  }
}

.register-notice {
  margin: 0 0 14px;
  padding: 10px 12px;
  border-radius: 12px;
  color: #626b78;
  background: #fafbfc;
  border: 1px solid #edf0f4;
  line-height: 1.5;
  font-size: 13px;
}

.register-profile-dialog {
  .el-dialog__body {
    padding-bottom: 2px;
  }

  .el-dialog__footer {
    padding-top: 0;
    padding-bottom: 18px;
  }

  .el-form-item {
    margin-bottom: 10px;
  }

  .el-form-item__label {
    margin-bottom: 6px;
  }

  .register-profile-form .el-form-item:last-child {
    margin-bottom: 12px;
  }
}

.register-dialog-footer {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.register-dialog-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.register-dialog-footer > .el-button.is-link {
  --el-button-text-color: #626b78;
  --el-button-hover-text-color: #e7576c;
  --el-button-active-text-color: #d94e62;
  color: #626b78;
  font-weight: 600;
  padding: 0;
}

.register-dialog-footer > .el-button.is-link:hover,
.register-dialog-footer > .el-button.is-link:focus {
  color: #e7576c;
  background: transparent;
}

@media (max-width: 520px) {
  .register-dialog-footer {
    flex-direction: column-reverse;
    align-items: stretch;
  }

  .register-dialog-actions {
    justify-content: flex-end;
  }
}
</style>
