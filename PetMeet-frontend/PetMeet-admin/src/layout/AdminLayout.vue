<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <div class="sidebar">
      <div class="logo-area">
        <img src="/brand/petmeet-admin-logo.svg" alt="PetMeet Admin" class="admin-logo" />
      </div>
      <el-menu
        :default-active="$route.path"
        class="el-menu-vertical"
        background-color="#2f3239"
        text-color="#d5d8de"
        active-text-color="var(--admin-sidebar-active)"
        router
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </div>

      <!-- 主体容器 -->
    <div class="main-container">
        <!-- 顶部栏 -->
      <div class="header">
        <div class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item
              v-for="item in breadcrumbs"
              :key="item.path || item.title"
              :to="item.path ? { path: item.path } : undefined"
            >
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="user-info">
          <el-dropdown command="logout" @command="handleCommand">
            <span class="el-dropdown-link">
              Admin <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

        <!-- 主内容区 -->
      <div class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" :key="$route.fullPath" />
          </transition>
        </router-view>
      </div>
      <BeianFooter />
    </div>
  </div>
</template>

<script setup>
import { Odometer, User, DocumentChecked, Goods, List, Menu, Warning, Picture, Service } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '@/store/admin'
import BeianFooter from '@/components/BeianFooter.vue'

const router = useRouter()
const route = useRoute()
const adminStore = useAdminStore()

const iconMap = {
  Odometer,
  User,
  DocumentChecked,
  Goods,
  List,
  Menu,
  Warning,
  Picture,
  Service
}

const adminRoute = router.options.routes.find(item => item.path === '/admin')

const menuItems = computed(() => {
  return (adminRoute?.children || [])
    .filter(item => item.meta?.title && item.path)
    .map(item => ({
      path: `/admin/${item.path}`,
      title: item.meta.title,
      icon: iconMap[item.meta.icon] || Menu
    }))
})

const breadcrumbs = computed(() => {
  return route.matched
    .filter(item => item.path !== '/admin' && item.meta?.title)
    .map(item => ({
      path: item.path.includes(':') ? '' : item.path,
      title: item.meta.title
    }))
})

const handleCommand = (command) => {
  if (command === 'logout') {
    adminStore.logout()
    router.push('/admin/login')
  }
}
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  width: 100%;
  height: 100vh;
}

.sidebar {
  width: 210px;
  background-color: #2f3239;
  height: 100%;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;

  .logo-area {
    height: 50px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 18px;
    font-weight: bold;
    border-bottom: 1px solid #24262b;
    
    .admin-logo {
      width: 178px;
      height: auto;
      display: block;
    }
  }

  .el-menu-vertical {
    border-right: none;
    flex: 1;
  }
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  height: 50px;
  background: #fff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.app-main {
  flex: 1;
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
  min-height: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
