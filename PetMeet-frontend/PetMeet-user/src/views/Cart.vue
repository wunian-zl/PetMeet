<template>
  <div class="cart-container">
    <div class="cart-header">
      <h2>我的购物车</h2>
      <div v-if="cartList.length" class="cart-sub">共 {{ cartList.length }} 件宝贝</div>
    </div>

  <!-- 空状态 -->
    <div v-if="!loading && cartList.length === 0" class="empty-cart">
      <el-empty description="购物车还是空的，去逛逛吧~">
        <el-button type="primary" class="go-home-btn" @click="router.push('/')">去首页看看</el-button>
      </el-empty>
    </div>

  <!-- 双栏布局 -->
    <div v-else class="cart-wrapper">
    <!-- 左侧：商品列表 -->
      <div class="cart-main">
        <el-table 
          :data="cartList" 
          style="width: 100%" 
          v-loading="loading"
          class="cart-table"
          :header-cell-style="{ background: '#fafafa', color: '#666', fontWeight: '500' }"
        >
          <el-table-column label="商品信息" min-width="300">
            <template #default="{ row }">
              <div class="product-cell" role="button" tabindex="0" @click="openProduct(row)" @keydown.enter="openProduct(row)">
                <div class="thumb-wrapper">
                  <img :src="row.productImg" class="thumb" loading="lazy" decoding="async" />
                </div>
                <div class="info">
                  <div class="name">{{ row.productName }}</div>
                  <div class="tags" v-if="row.attributes">
                    <el-tag size="small" type="info" effect="plain">{{ row.attributes }}</el-tag>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="100" align="center">
            <template #default="{ row }">
              <span class="price">¥{{ row.price }}</span>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="140" align="center">
            <template #default="{ row }">
              <div class="qty-control">
                <button
                  class="qty-btn"
                  :disabled="row.quantity <= 1"
                  type="button"
                  @click="bumpQuantity(row, -1)"
                >
                  <el-icon :size="12"><Minus /></el-icon>
                </button>
                <input
                  class="qty-input"
                  :value="row.quantity"
                  readonly
                />
                <button
                  class="qty-btn"
                  :disabled="row.quantity >= 99"
                  type="button"
                  @click="bumpQuantity(row, 1)"
                >
                  <el-icon :size="12"><Plus /></el-icon>
                </button>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="小计" width="110" align="center">
            <template #default="{ row }">
              <span class="subtotal">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ row }">
              <el-button 
                type="danger" 
                link 
                class="delete-btn"
                @click="deleteItem(row.id)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

    <!-- 右侧：汇总卡片 -->
      <aside class="cart-aside">
        <div class="summary-card">
          <div class="summary-header">
            <h3>结算明细</h3>
          </div>
          
          <div class="summary-content">
            <div class="summary-row">
              <span class="label">商品总数</span>
              <span class="value">{{ cartList.length }} 件</span>
            </div>
             <div class="divider"></div>
            <div class="total-row">
              <span class="label">合计</span>
              <span class="total-price">
                <small>¥</small>{{ totalPrice }}
              </span>
            </div>
            
            <button
              class="checkout-btn"
              :disabled="cartList.length === 0"
              @click="openCheckout"
            >
              立即结算
            </button>
             <p class="summary-tip">请在下单前确认商品信息</p>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Minus, Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const cartList = ref([])
const loading = ref(false)

const openProduct = (row) => {
  const productId = row?.productId
  if (!productId) {
    ElMessage.warning('商品信息不完整，无法打开详情页')
    return
  }
  router.push({ name: 'ProductDetail', params: { id: productId } })
}

// 拉取购物车
const getCartList = async () => {
  loading.value = true
  try {
    const res = await request.get('/cart/list')
    cartList.value = res || []
  } finally {
    loading.value = false
  }
}

const clampInt = (value, min, max) => {
  const n = Number.parseInt(String(value), 10)
  if (Number.isNaN(n)) return null
  return Math.min(max, Math.max(min, n))
}

// 修改数量时先乐观更新，失败再回滚
const updateQuantity = async (row, nextQty) => {
  const prev = row.quantity
  row.quantity = nextQty
  try {
    await request.put(`/cart/update?cartItemId=${row.id}&quantity=${row.quantity}`)
  } catch (error) {
    row.quantity = prev
    ElMessage.error('数量更新失败，请稍后重试')
  }
}

const bumpQuantity = async (row, delta) => {
  const next = clampInt((row.quantity || 1) + delta, 1, 99)
  if (next == null || next === row.quantity) return
  updateQuantity(row, next)
}

// 删除商品
const deleteItem = async (id) => {
  try {
     await ElMessageBox.confirm('确定要移出购物车吗?', '操作提示', { 
       confirmButtonText: '确定',
       cancelButtonText: '取消',
       type: 'warning',
       center: true
     })
     await request.delete(`/cart/delete/${id}`)
     ElMessage.success('已移出购物车')
     getCartList()
  } catch (e) {
     // 用户取消了，或者请求本身报错了
  }
}

// 计算总价
const totalPrice = computed(() => {
  return cartList.value.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0).toFixed(2)
})

const openCheckout = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再结算')
    userStore.showLogin()
    return
  }
  router.push('/checkout')
}

onMounted(() => {
  getCartList()
})
</script>

<style scoped lang="scss">
.cart-container {
  max-width: 1200px;
  margin: 30px auto;
  padding: 0 20px;
  
  h2 {
    font-size: 24px;
    font-weight: 600;
    color: #333;
    margin: 0;
  }
  
  .cart-header {
    display: flex;
    align-items: baseline;
    gap: 12px;
    margin-bottom: 24px;
    padding-left: 10px;
    
    .cart-sub {
      font-size: 14px;
      color: #888;
    }
  }

  /* 覆盖购物车页里的 Element Plus 主按钮样式 */
  :deep(.el-button--primary) {
    --el-button-bg-color: #ff6b81;
    --el-button-border-color: #ff6b81;
    --el-button-hover-bg-color: #ff8da1;
    --el-button-hover-border-color: #ff8da1;
    --el-button-active-bg-color: #ff4757;
    --el-button-active-border-color: #ff4757;
  }
}

.empty-cart {
  background: #fff;
  border-radius: 16px;
  padding: 80px 0;
  display: flex;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(0,0,0,0.03);
  
  .go-home-btn {
    border-radius: 20px;
    padding: 10px 30px;
  }
}

.cart-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  
  @media (max-width: 992px) {
    flex-direction: column;
  }
}

.cart-main {
  flex: 1;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.03);
  width: 100%; /* Ensure full width on mobile or flex resizing */
}

/* 侧边汇总卡片 */
.cart-aside {
  width: 320px;
  flex-shrink: 0;
  position: sticky;
  top: 20px;
  
  @media (max-width: 992px) {
    width: 100%;
    position: static;
  }
}

.summary-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.04);
  
  .summary-header {
    margin-bottom: 20px;
    h3 {
      margin: 0;
      font-size: 18px;
      color: #333;
    }
  }
  
  .summary-content {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  
  .summary-row {
     display: flex;
     justify-content: space-between;
     font-size: 14px;
     color: #666;
     
     .price-minus {
        color: #ff4d4f;
     }
  }
  
  .divider {
    height: 1px;
    background: #f0f0f0;
    margin: 4px 0;
  }
  
  .total-row {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    margin-bottom: 8px;
    
    .label {
       font-size: 16px;
       font-weight: 600;
       color: #333;
    }
    
    .total-price {
      font-size: 28px;
      color: #ff5000;
      font-weight: 700;
      font-family: Arial, sans-serif;
      
      small {
        font-size: 16px;
        margin-right: 2px;
      }
    }
  }
  
  .checkout-btn {
    width: 100%;
    height: 44px;
    /* 这里改成偏暖的粉色渐变 */
    background: linear-gradient(135deg, #FF6B6B 0%, #FF8E53 100%);
    border: none;
    border-radius: 22px;
    color: #fff;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 6px 16px rgba(255, 107, 107, 0.25);
    transition: all 0.2s;
    
    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(255, 107, 107, 0.35);
    }
    
    &:disabled {
      background: #ccc;
      box-shadow: none;
      cursor: not-allowed;
      transform: none;
    }
  }
  
  .summary-tip {
    font-size: 12px;
    color: #999;
    text-align: center;
    margin: 0;
  }
}


/* 商品单元格 */
.product-cell {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  cursor: pointer;
  user-select: none;

  &:focus-visible {
    outline: 2px solid rgba(255, 107, 107, 0.45);
    outline-offset: 2px;
    border-radius: 8px;
  }
  
  .thumb-wrapper {
    width: 72px;
    height: 72px;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid #f0f0f0;
    flex-shrink: 0;
    
    .thumb {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  
  .info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 6px;
    
    .name {
      font-size: 14px;
      color: #333;
      line-height: 1.4;
      font-weight: 400;
    }
  }

  &:hover .info .name {
    color: #ff6b6b;
  }
}

.price {
  color: #333;
  font-weight: 600;
}

.subtotal {
  color: #ff6b00;
  font-weight: 700;
}

.delete-btn {
  color: #999;
  font-size: 13px;
  
  &:hover {
    color: #ff4d4f;
  }
}

/* 新版数量调节器样式 */
.qty-control {
  display: flex;
  align-items: center;
  width: 90px;
  height: 28px;
  border: 1px solid #ddd;
  border-radius: 4px; 
  background: #fff;
  margin: 0 auto;

  .qty-btn {
    width: 26px;
    height: 100%;
    border: none;
    background: #fff;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #666;
    transition: background 0.2s;
    
    &:hover:not(:disabled) {
      background: #f7f7f7;
    }
    
    &:disabled {
      cursor: not-allowed;
      opacity: 0.5;
      background: #fafafa;
    }
  }
  
  .qty-input {
    flex: 1;
    height: 100%;
    border: none;
    border-left: 1px solid #eee;
    border-right: 1px solid #eee;
    text-align: center;
    font-size: 13px;
    color: #333;
    outline: none;
    background: #fff;
    padding: 0;
    width: 30px; 
  }
}

/* Element Plus 局部覆盖 */
:deep(.cart-table) {
  --el-table-border-color: #f0f0f0;
  
  .el-table__inner-wrapper::before {
    display: none;
  }
  
  th.el-table__cell {
    background: #fcfcfc !important;
    border-bottom: 1px solid #f0f0f0;
  }
  
  .el-table__cell {
    padding: 16px 0;
  }
}
</style>
