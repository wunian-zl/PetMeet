<template>
  <div class="login-container">
    <div class="login-card">
    <!-- 左侧：图片与品牌区 -->
      <div class="login-image" :style="loginImageStyle">
        <div class="image-content">
          <div class="logo-text">PetMeet</div>
          <p class="slogan">发现身边的美好宠物生活</p>
        </div>
      </div>
      
    <!-- 右侧：表单区 -->
      <div class="login-form-container">
        <div class="form-inner">
          <div class="form-header">
            <h2>欢迎回来</h2>
            <p class="sub-title">登录账号，解锁完整功能</p>
          </div>
          
          <el-tabs v-model="activeTab" class="custom-tabs" stretch>
        <!-- 登录页签 -->
            <el-tab-pane label="登录" name="login">
              <el-form
                ref="loginFormRef"
                :model="loginForm"
                :rules="loginRules"
                class="auth-form"
                size="large"
              >
                <el-form-item prop="username">
                  <el-input 
                    v-model="loginForm.username" 
                    placeholder="请输入用户名" 
                    prefix-icon="User"
                    class="rounded-input" 
                  />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input 
                    v-model="loginForm.password" 
                    type="password" 
                    placeholder="请输入密码" 
                    prefix-icon="Lock" 
                    show-password 
                    class="rounded-input"
                    @keyup.enter="handleLogin"
                  />
                </el-form-item>
                
                <div class="login-tools">
                  <el-button link type="primary" @click="activeTab = 'reset'">忘记密码?</el-button>
                </div>

                <div class="form-tips">
                  若账号未注册，会先让你完善绑定信息，提交后完成注册并登录。
                </div>

                <el-form-item>
                  <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin" round>
                    登 录
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
    
        <!-- 注册页签 -->
            <el-tab-pane label="注册" name="register">
              <div v-if="registerFromLogin" class="new-account-notice">
                <span>这是一个未注册的新账号。请确认用户名和密码，并绑定手机号或邮箱。</span>
                <el-button link type="primary" @click="returnToLoginFromRegister">
                  已有账号，返回登录
                </el-button>
              </div>
              <el-form
                ref="registerFormRef"
                :model="registerForm"
                :rules="registerRules"
                class="auth-form"
                size="large"
              >
                <el-form-item prop="username">
                  <el-input 
                    v-model="registerForm.username" 
                    placeholder="设置用户名" 
                    prefix-icon="User" 
                    class="rounded-input"
                  />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input 
                    v-model="registerForm.password" 
                    type="password" 
                    placeholder="设置密码" 
                    prefix-icon="Lock" 
                    show-password 
                    class="rounded-input"
                  />
                </el-form-item>
                <el-form-item prop="checkPassword">
                  <el-input 
                    v-model="registerForm.checkPassword" 
                    type="password" 
                    placeholder="确认密码" 
                    prefix-icon="Lock" 
                    show-password 
                    class="rounded-input"
                  />
                </el-form-item>
                <el-form-item prop="nickname">
                  <el-input
                    v-model.trim="registerForm.nickname"
                    placeholder="展示昵称（别人看到的昵称）"
                    prefix-icon="UserFilled"
                    maxlength="20"
                    show-word-limit
                    class="rounded-input"
                  />
                </el-form-item>
                <el-form-item prop="phone">
                  <el-input
                    v-model.trim="registerForm.phone"
                    placeholder="绑定手机号（手机号或邮箱填一个）"
                    prefix-icon="Iphone"
                    class="rounded-input"
                  />
                </el-form-item>
                <el-form-item prop="email">
                  <el-input
                    v-model.trim="registerForm.email"
                    placeholder="绑定邮箱（手机号或邮箱填一个）"
                    prefix-icon="Message"
                    class="rounded-input"
                  />
                </el-form-item>
                <div class="form-tips register-tip">
                  绑定信息只用于找回密码和账号识别，至少填写手机号或邮箱其中一项。
                </div>
                <el-form-item>
                  <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister" round>
                    注 册
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="找回密码" name="reset">
              <el-form
                ref="resetFormRef"
                :model="resetForm"
                :rules="resetRules"
                class="auth-form"
                size="large"
              >
                <el-form-item prop="username">
                  <el-input
                    v-model.trim="resetForm.username"
                    placeholder="请输入用户名"
                    prefix-icon="User"
                    class="rounded-input"
                  />
                </el-form-item>
                <el-form-item prop="contact">
                  <el-input
                    v-model.trim="resetForm.contact"
                    placeholder="已绑定手机号或邮箱"
                    prefix-icon="Message"
                    class="rounded-input"
                  />
                </el-form-item>
                <el-form-item prop="newPassword">
                  <el-input
                    v-model="resetForm.newPassword"
                    type="password"
                    placeholder="设置新密码"
                    prefix-icon="Lock"
                    show-password
                    class="rounded-input"
                  />
                </el-form-item>
                <el-form-item prop="checkPassword">
                  <el-input
                    v-model="resetForm.checkPassword"
                    type="password"
                    placeholder="确认新密码"
                    prefix-icon="Lock"
                    show-password
                    class="rounded-input"
                    @keyup.enter="handleResetPassword"
                  />
                </el-form-item>
                <div class="form-tips">
                  需要填写账号已绑定的手机号或邮箱。没有绑定信息的旧账号，需要先登录后到个人中心补充。
                </div>
                <el-form-item>
                  <el-button type="primary" class="submit-btn" :loading="resetLoading" @click="handleResetPassword" round>
                    重置密码
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>

          <div class="form-footer">
            登录即代表同意 <span class="link">《用户协议》</span> 和 <span class="link">《隐私政策》</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
  
<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import loginPosterUrl from '@/assets/login-poster.jpg'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loginImageStyle = {
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

const activeTab = ref('login')
const loading = ref(false)
const resetLoading = ref(false)
const registerFromLogin = ref(false)

// 其余逻辑保持不变
// 登录逻辑
const loginFormRef = ref(null)
const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.login(loginForm, { silentError: true })
        ElMessage.success('登录成功')
        router.replace(getLoginRedirect())
      } catch (error) {
        if (error?.code === 404 || error?.response?.code === 404) {
          openRegisterFromLogin()
        } else {
          ElMessage.error(error?.message || '登录失败')
        }
      } finally {
        loading.value = false
      }
    }
  })
}

const getLoginRedirect = () => {
  let target = '/'
  const redirect = typeof route.query?.redirect === 'string' ? route.query.redirect : ''
  if (redirect && redirect.startsWith('/')) {
    target = redirect.replace(/\?+$/, '') || '/'
  }
  return target
}

// 注册逻辑
const registerFormRef = ref(null)
const registerForm = reactive({
  username: '',
  password: '',
  checkPassword: '',
  nickname: '',
  phone: '',
  email: ''
})
const isReservedUsername = (value) => reservedUsernames.includes((value || '').trim().toLowerCase())

const openRegisterFromLogin = () => {
  registerForm.username = loginForm.username
  registerForm.password = loginForm.password
  registerForm.checkPassword = loginForm.password
  registerForm.nickname = loginForm.username
  registerForm.phone = ''
  registerForm.email = ''
  registerFromLogin.value = true
  activeTab.value = 'register'
}

const returnToLoginFromRegister = () => {
  loginForm.username = registerForm.username || loginForm.username
  loginForm.password = ''
  registerForm.password = ''
  registerForm.checkPassword = ''
  registerForm.phone = ''
  registerForm.email = ''
  registerFromLogin.value = false
  activeTab.value = 'login'
  registerFormRef.value?.clearValidate()
  loginFormRef.value?.clearValidate()
}

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const validateRegisterPassword = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入密码'))
  } else if (value.length < 8 || value.length > 64 || !/[A-Za-z]/.test(value) || !/\d/.test(value)) {
    callback(new Error('密码必须为8-64位，且同时包含字母和数字'))
  } else {
    callback()
  }
}

const validatePhone = (_rule, value, callback) => {
  if (!value) {
    callback()
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

const validateEmail = (_rule, value, callback) => {
  if (!value) {
    callback()
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
    callback(new Error('邮箱格式不正确'))
  } else {
    callback()
  }
}

const validateRegisterUsername = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入用户名'))
  } else if (isReservedUsername(value)) {
    callback(new Error('该用户名不可使用，请换一个'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [{ validator: validateRegisterUsername, trigger: 'blur' }],
  password: [{ validator: validateRegisterPassword, trigger: 'blur' }],
  checkPassword: [{ validator: validatePass2, trigger: 'blur' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  email: [{ validator: validateEmail, trigger: 'blur' }]
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      if (isReservedUsername(registerForm.username)) {
        ElMessage.warning('该用户名不可使用，请换一个')
        return
      }
      if (!registerForm.phone && !registerForm.email) {
        ElMessage.warning('请至少绑定手机号或邮箱')
        return
      }
      loading.value = true
      try {
        await userStore.register({
          username: registerForm.username,
          password: registerForm.password,
          nickname: registerForm.nickname || registerForm.username,
          phone: registerForm.phone || null,
          email: registerForm.email || null
        })
        ElMessage.success('注册并登录成功')
        registerFormRef.value.resetFields()
        registerFromLogin.value = false
        router.replace(getLoginRedirect())
      } catch (error) {
        // 这里交给全局拦截器处理
      } finally {
        loading.value = false
      }
    }
  })
}

const resetFormRef = ref(null)
const resetForm = reactive({
  username: '',
  contact: '',
  newPassword: '',
  checkPassword: ''
})

const validateResetPass2 = (_rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== resetForm.newPassword) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const resetRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入已绑定手机号或邮箱', trigger: 'blur' }],
  newPassword: [{ validator: validateRegisterPassword, trigger: 'blur' }],
  checkPassword: [{ validator: validateResetPass2, trigger: 'blur' }]
}

const handleResetPassword = async () => {
  if (!resetFormRef.value) return

  await resetFormRef.value.validate(async (valid) => {
    if (valid) {
      resetLoading.value = true
      try {
        await request.post('/auth/reset-password', {
          username: resetForm.username,
          contact: resetForm.contact,
          newPassword: resetForm.newPassword
        })
        ElMessage.success('密码已重置，请重新登录')
        loginForm.username = resetForm.username
        loginForm.password = ''
        activeTab.value = 'login'
        resetFormRef.value.resetFields()
      } catch (error) {
        // 这里交给全局拦截器处理
      } finally {
        resetLoading.value = false
      }
    }
  })
}
</script>
  
<style scoped lang="scss">
.login-container {
  height: 100vh;
  width: 100vw;
  display: flex;
  overflow: hidden;
  background: white;
}

.login-card {
  display: flex;
  width: 100%;
  height: 100%;
  border-radius: 0;
  box-shadow: none;
  background: white;
  
  @media (max-width: 768px) {
    flex-direction: column;
    
    .login-image {
      display: none;
    }
    
    .login-form-container {
      width: 100%;
      padding: 40px 20px;
    }
  }
}

.login-image {
  flex: 0 0 60%; /* Take up 60% of the screen */
  background-color: #d1d5db;
  background-size: cover;
  background-position: center;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 60px;
  
  &::before {
    content: '';
    position: absolute;
    top: 0; 
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(to right, rgba(0,0,0,0.4) 0%, rgba(0,0,0,0) 100%);
  }

  .image-content {
    position: relative;
    z-index: 1;
    color: white;
    
    .logo-text {
      font-size: 48px;
      font-weight: 800;
      margin-bottom: 20px;
      letter-spacing: 2px;
      text-shadow: 0 2px 10px rgba(0,0,0,0.3);
    }
    
    .slogan {
      font-size: 24px;
      opacity: 0.95;
      font-weight: 300;
      letter-spacing: 1px;
      text-shadow: 0 2px 10px rgba(0,0,0,0.3);
    }
  }
}

.login-form-container {
  flex: 1; /* Take up remaining 40% */
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 0; /* Vertical centering handled by flex */
  
  /* 内层容器负责收口宽度 */
  .form-inner {
    width: 420px;
    max-width: 90%;
    margin: 0 auto;
  }

  .form-header {
    margin-bottom: 40px;
    
    h2 {
      font-size: 36px;
      color: #333;
      margin: 0 0 12px;
      font-weight: 700;
    }
    
    .sub-title {
      color: #888;
      font-size: 16px;
    }
  }
}

.auth-form {
  
  :deep(.el-input__wrapper) {
    border-radius: 50px;
    box-shadow: none;
    background-color: #f7f8fa;
    border: 1px solid transparent;
    padding: 12px 24px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    height: 50px;
    
    &:hover, &.is-focus {
      background-color: white;
      border-color: #ff6b81; 
      box-shadow: 0 4px 12px rgba(255, 107, 129, 0.1);
    }
  }
  
  :deep(.el-input__inner) {
    font-size: 16px;
  }
}

.form-tips {
  font-size: 13px;
  color: #999;
  line-height: 1.6;
  margin: 16px 0 32px;
  padding: 0 4px;
}

.login-tools {
  display: flex;
  justify-content: flex-end;
  margin: -10px 0 8px;

  :deep(.el-button.is-link) {
    color: #626b78;
    font-weight: 600;

    &:hover,
    &:focus {
      color: #e7576c;
      background: transparent;
    }
  }
}

.register-tip {
  margin-top: -4px;
  margin-bottom: 20px;
}

.new-account-notice {
  margin: 0 0 16px;
  padding: 12px 14px;
  border-radius: 12px;
  color: #626b78;
  background: #fafbfc;
  border: 1px solid #edf0f4;
  line-height: 1.5;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  span {
    flex: 1;
  }

  :deep(.el-button.is-link) {
    color: #626b78;
    font-weight: 600;

    &:hover,
    &:focus {
      color: #e7576c;
      background: transparent;
    }
  }

  @media (max-width: 520px) {
    flex-direction: column;
    align-items: flex-start;
  }
}

.submit-btn {
  width: 100%;
  height: 54px;
  font-size: 18px;
  font-weight: 600;
  background: #f06478;
  border: none;
  box-shadow: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  letter-spacing: 2px;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 20px rgba(240, 100, 120, 0.18);
    background: #e7576c;
  }
  
  &:active {
    transform: translateY(1px);
    box-shadow: none;
    background: #d94e62;
  }

  &:focus,
  &:focus-visible {
    background: #f06478;
    outline: 2px solid rgba(240, 100, 120, 0.2);
    outline-offset: 2px;
  }
}

.form-footer {
  margin-top: 40px;
  text-align: center;
  color: #bbb;
  font-size: 13px;
  
  .link {
    color: #ff6b81;
    cursor: pointer;
    transition: opacity 0.2s;
    font-weight: 500;
    
    &:hover {
      opacity: 0.8;
      text-decoration: underline;
    }
  }
}

/* 自定义标签页样式 */
:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: #eee;
}

:deep(.el-tabs__item) {
  font-size: 18px;
  color: #999;
  height: 50px;
  
  &.is-active {
    color: #333;
    font-weight: 700;
  }
}

:deep(.el-tabs__active-bar) {
  background-color: #ff6b81;
  height: 3px;
  border-radius: 3px;
}

/* 清掉 Element Plus 的默认外边距 */
:deep(.el-form-item) {
  margin-bottom: 24px;
}
</style>
