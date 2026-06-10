<template>
  <div class="product-card" @click="goToDetail">
    <!-- 1:1 正方形图片区域 -->
    <div class="card-image">
      <img :src="product.image" :alt="product.name" loading="lazy" decoding="async" />

      <!-- 悬浮操作栏 -->
      <div class="hover-actions">
        <button class="action-btn" @click.stop="addToCart">
          <el-icon><ShoppingCart /></el-icon>
        </button>
      </div>
    </div>
    
    <!-- 信息区域 -->
    <div class="card-info">
      <h3 class="product-name">{{ product.name }}</h3>
      <div class="product-meta">
        <span class="price">
          <span class="symbol">¥</span>
          <span class="integer">{{ priceInteger }}</span>
          <span class="decimal" v-if="priceDecimal">.{{ priceDecimal }}</span>
        </span>
        <span class="sales">{{ product.sales }}人付款</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
})

const router = useRouter()

const goToDetail = () => {
  router.push(`/product/${props.product.id}`)
}

const addToCart = () => {
  ElMessage.success('已加入购物车')
  // 待办：这里后面要接入真正的加入购物车逻辑，统一走 Pinia 状态管理。
}

// 价格格式化
const priceInteger = computed(() => Math.floor(props.product.price))
const priceDecimal = computed(() => {
  const decimal = (props.product.price % 1).toFixed(2).substring(2)
  return decimal === '00' ? '' : decimal
})
</script>

<style scoped lang="scss">
.product-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  border: 1px solid transparent;
  
  &:hover {
    box-shadow: 0 12px 24px rgba(0,0,0,0.08);
    transform: translateY(-4px);
    border-color: rgba(0,0,0,0.03);

    .hover-actions {
      opacity: 1;
      transform: translateY(0);
    }
  }

  // 正方形图片容器
  .card-image {
    aspect-ratio: 1 / 1;
    width: 100%;
    overflow: hidden;
    background: #f8f8f8;
    position: relative;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.5s ease;
    }

    &:hover img {
      transform: scale(1.08);
    }
  }

  // 悬浮操作栏
  .hover-actions {
    position: absolute;
    bottom: 12px;
    right: 12px;
    opacity: 0;
    transform: translateY(10px);
    transition: all 0.3s ease;
    z-index: 3;
    
    .action-btn {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: #fff;
      border: none;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
      color: #333;
      transition: all 0.2s;
      
      &:hover {
        background: #FF6B81;
        color: #fff;
        transform: scale(1.1);
      }
    }
  }

  // 商品信息
  .card-info {
    padding: 12px;
    
    .product-name {
      font-size: 14px;
      color: #333;
      line-height: 1.5;
      height: 3em; // 2 lines
      overflow: hidden;
      margin-bottom: 8px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      line-clamp: 2;
      -webkit-box-orient: vertical;
      font-weight: 500;
    }
    
    .product-meta {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      
      .price {
        color: #ff4757;
        font-weight: 700;
        line-height: 1;
        
        .symbol {
          font-size: 12px;
          margin-right: 1px;
        }
        
        .integer {
          font-size: 18px;
        }
        
        .decimal {
          font-size: 13px;
        }
      }
      
      .sales {
        font-size: 12px;
        color: #aaa;
        transform: translateY(-2px);
      }
    }
  }
}
</style>
