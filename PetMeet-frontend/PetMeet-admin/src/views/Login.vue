<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="login-header">
          <h2>PetMeet 后台管理</h2>
        </div>
      </template>
      <el-form :model="loginForm" :rules="rules" ref="loginFormRef">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="账号">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="密码" show-password @keyup.enter="handleLogin">
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登录</el-button>
      </el-form>

    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/store/admin'
import { ElMessage } from 'element-plus'

const router = useRouter()
const adminStore = useAdminStore()
const loginFormRef = ref(null)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const loading = ref(false)

const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const result = await adminStore.login(loginForm.username, loginForm.password)
        if (result.success) {
          ElMessage.success('登录成功')
          router.push('/admin/dashboard')
        } else {
          ElMessage.error(result.message || '账号或密码错误')
        }
      } catch (error) {
        ElMessage.error(error.message || '登录失败')
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
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #2f3239;
}

.login-card {
  width: 400px;
}

.login-header {
  text-align: center;
  h2 {
    margin: 0;
    color: #333;
  }
}

.login-btn {
  width: 100%;
}


</style>
