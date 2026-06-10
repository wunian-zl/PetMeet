<template>
  <div class="login-container">
    <div class="login-card">
    <!-- 左侧：图片与品牌区 -->
      <div class="login-image">
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
                
                <div class="form-tips">
                  若账号未注册，首次登录会自动注册并完成登录（用户名2-20位，支持汉字/字母/数字/下划线，密码8-64位且包含字母和数字）。
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
                <el-form-item>
                  <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister" round>
                    注 册
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

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)

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
        await userStore.login(loginForm)
        ElMessage.success('登录成功')
        let target = '/'
        const redirect = typeof route.query?.redirect === 'string' ? route.query.redirect : ''
        if (redirect && redirect.startsWith('/')) {
          target = redirect.replace(/\?+$/, '') || '/'
        }
        router.replace(target)
      } catch (error) {
        // 错误提示交给全局拦截器
      } finally {
        loading.value = false
      }
    }
  })
}

// 注册逻辑
const registerFormRef = ref(null)
const registerForm = reactive({
  username: '',
  password: '',
  checkPassword: ''
})

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

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ validator: validateRegisterPassword, trigger: 'blur' }],
  checkPassword: [{ validator: validatePass2, trigger: 'blur' }]
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await request.post('/auth/register', {
          username: registerForm.username,
          password: registerForm.password
        })
        ElMessage.success('注册成功，请登录')
        activeTab.value = 'login'
        registerFormRef.value.resetFields()
      } catch (error) {
        // 这里交给全局拦截器处理
      } finally {
        loading.value = false
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
  background-image: url('https://i.pinimg.com/736x/8d/f3/e6/8df3e67026df33230638575003504a5d.jpg'); /* Cute cat illustration style */
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

.submit-btn {
  width: 100%;
  height: 54px;
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #ff8092 0%, #ff5c7c 100%);
  border: none;
  box-shadow: 0 10px 25px rgba(255, 92, 124, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  letter-spacing: 2px;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 15px 30px rgba(255, 92, 124, 0.4);
    background: linear-gradient(135deg, #ff94a4 0%, #ff6b8b 100%);
  }
  
  &:active {
    transform: translateY(1px);
    box-shadow: 0 5px 15px rgba(255, 92, 124, 0.2);
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
