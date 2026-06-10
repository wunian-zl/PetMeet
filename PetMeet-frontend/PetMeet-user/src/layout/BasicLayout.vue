<template>
  <div class="layout-container">
    <!-- 左侧固定图标栏 -->
    <aside class="sidebar">
      <div class="logo-area">
      <!-- 用粉色爪印图标当站点标识 -->
        <div class="logo-icon">
           🐾
        </div>
      </div>
      
      <nav class="nav-menu">
        <el-tooltip content="发现" placement="right" effect="dark" :offset="10" :show-after="0" transition="none">
          <router-link to="/" class="nav-item" active-class="active">
            <div class="icon-btn">
               <el-icon><Location /></el-icon>
            </div>
          </router-link>
        </el-tooltip>
        

        <el-tooltip content="发布" placement="right" effect="dark" :offset="10" :show-after="0" transition="none">
          <router-link to="/publish" class="nav-item" active-class="active">
            <div class="icon-btn publish-btn">
              <el-icon><Plus /></el-icon>
            </div>
          </router-link>
        </el-tooltip>


        <el-tooltip content="商城" placement="right" effect="dark" :offset="10" :show-after="0" transition="none">
          <router-link to="/shop" class="nav-item" active-class="active">
             <div class="icon-btn">
               <el-icon><Shop /></el-icon>
             </div>
          </router-link>
        </el-tooltip>
        

        <el-tooltip content="购物车" placement="right" effect="dark" :offset="10" :show-after="0" transition="none">
          <router-link to="/cart" class="nav-item" active-class="active">
            <div class="icon-btn" data-cart-icon>
              <el-badge :value="userStore.cartCount" :hidden="userStore.cartCount === 0" :max="99" class="cart-badge">
                <el-icon><ShoppingCart /></el-icon>
              </el-badge>
            </div>
          </router-link>
        </el-tooltip>


        <el-tooltip content="通知" placement="right" effect="dark" :offset="10" :show-after="0" transition="none">
          <router-link to="/notification" class="nav-item" active-class="active">
            <div class="icon-btn">
              <el-badge
                :is-dot="true"
                :hidden="userStore.notificationUnreadCount === 0"
                class="notice-badge"
              >
                <el-icon><Bell /></el-icon>
              </el-badge>
            </div>
          </router-link>
        </el-tooltip>
        

        <el-tooltip content="我" placement="right" effect="dark" :offset="10" :show-after="0" transition="none">
          <router-link to="/profile" class="nav-item" active-class="active">
            <div class="icon-btn">
              <el-badge :is-dot="userStore.unpaidOrderCount > 0" class="profile-badge">
                <el-icon><UserFilled /></el-icon>
              </el-badge>
            </div>
          </router-link>
        </el-tooltip>
      </nav>
    </aside>

      <!-- 右侧主内容区 -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <keep-alive :include="['Home', 'Shop', 'ShopList', 'Profile']">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </main>
    
    <!-- 全局登录弹窗 -->
    <LoginModal />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { Location, Plus, ShoppingCart, UserFilled, Shop, Bell } from '@element-plus/icons-vue'
import LoginModal from '@/components/LoginModal.vue'

const userStore = useUserStore()

// 页面加载时获取购物车数量
onMounted(async () => {
  if (userStore.token) {
    if (!userStore.userInfo?.id) {
      try {
        await userStore.getUserInfo()
      } catch (e) {}
    }
    userStore.fetchCartCount()
    userStore.fetchUnpaidOrderCount()
    userStore.fetchNotificationUnreadCount()
  }
})
</script>

<style scoped lang="scss">
.layout-container {
  display: flex;
  min-height: 100vh;
  background-color: #fff; 
}

.sidebar {
  width: 80px;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  background: rgba(255, 255, 255, 0.85); /* Semi-transparent white */
  backdrop-filter: blur(12px); /* Glass blur effect */
  border-right: 1px solid rgba(255, 255, 255, 0.6); /* Subtle glass border */
  box-shadow: 2px 0 24px rgba(0, 0, 0, 0.04); /* Soft premium shadow */
  display: flex;
  flex-direction: column;
  align-items: center; 
  padding: 24px 0; /* Slightly more padding */
  z-index: 1900;
  
  .logo-area {
    margin-bottom: 32px;
    .logo-icon {
        width: 52px; /* Slightly larger logo */
        height: 52px;
        border-radius: 16px; /* Squircle for modern look */
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 26px;
        color: #FF6B81; 
        background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(255, 240, 243, 0.5));
        box-shadow: 0 4px 12px rgba(255, 107, 129, 0.15);
        cursor: default; /* No pointer cursor implies no action */
        /* 这里去掉过渡效果 */
        
        /* 这里去掉悬停效果 */
    }
  }
}

.nav-menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px; 
  align-items: center;
  margin-top: 10px;
}

.nav-item {
  text-decoration: none;
  position: relative;
  
  &.active {
     .icon-btn {
        background: linear-gradient(135deg, #FF9A9E 0%, #FECFEF 100%);
        box-shadow: 0 4px 12px rgba(255, 107, 129, 0.3);
        color: #fff; /* White icon on active */
     }
  }
}

.icon-btn {
  width: 48px;
  height: 48px;
  border-radius: 14px; /* Squircle radius matching modern OS */
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399; 
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  cursor: pointer;
  background-color: transparent;
  position: relative;
  
  .el-icon {
    font-size: 24px;
    transition: transform 0.3s ease;
  }
  
  &:hover {
    background-color: rgba(255, 107, 129, 0.08); /* Very subtle pink hover */
    color: #FF6B81;
    transform: translateY(-2px);
    
    .el-icon {
       transform: scale(1.1);
    }
  }
}

@keyframes cartBounce {
  0% { transform: scale(1); }
  50% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

.icon-btn.cart-bounce {
  animation: cartBounce 0.4s ease;
}

.nav-item .publish-btn {
   // 这里提高选择器优先级，确保激活态能覆盖通用样式
   background: linear-gradient(135deg, #FF9A9E 0%, #FF6B81 100%);
   color: #fff !important; // Force white icon
   box-shadow: 0 4px 12px rgba(255, 107, 129, 0.4); // Pink Shadow
   z-index: 1; /* Ensure it stays on top */
   
   &:hover {
      transform: translateY(-2px) scale(1.05);
      box-shadow: 0 8px 20px rgba(255, 107, 129, 0.5);
   }
   
   .el-icon {
      color: #fff;
   }
}

.profile-badge :deep(.el-badge__content.is-dot) {
  right: 2px;
  top: 2px;
}

.notice-badge :deep(.el-badge__content.is-dot) {
  right: 2px;
  top: 2px;
  background-color: #ff4d4f;
  border: 2px solid #fff;
}

.main-content {
  margin-left: 80px; 
  flex: 1;
  background-color: #fff; 
  min-height: 100vh;
}
</style>
