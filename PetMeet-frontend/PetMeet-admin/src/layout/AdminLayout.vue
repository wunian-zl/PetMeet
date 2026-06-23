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
        <el-menu-item index="/admin/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/user">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/content">
          <el-icon><DocumentChecked /></el-icon>
          <span>内容管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/complaint">
          <el-icon><Warning /></el-icon>
          <span>投诉管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/category">
          <el-icon><Menu /></el-icon>
          <span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/product">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/order">
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/after-sale">
          <el-icon><Service /></el-icon>
          <span>售后管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/banner">
          <el-icon><Picture /></el-icon>
          <span>广告管理</span>
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
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
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
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/store/admin'
import BeianFooter from '@/components/BeianFooter.vue'

const router = useRouter()
const adminStore = useAdminStore()

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
