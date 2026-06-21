<template>
  <div class="product-card" @click="goToDetail">
    <!-- 1:1 正方形图片区域 -->
    <div ref="cardImageRef" class="card-image">
      <img ref="productImageRef" :src="product.image" :alt="product.name" loading="lazy" decoding="async" />

      <!-- 悬浮操作栏 -->
      <div class="hover-actions">
        <button class="action-btn" :disabled="addingToCart" @click.stop="addToCart">
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
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
})

const router = useRouter()
const userStore = useUserStore()
const addingToCart = ref(false)
const cardImageRef = ref(null)
const productImageRef = ref(null)

const goToDetail = () => {
  router.push(`/product/${props.product.id}`)
}

const flyToCart = (retry = 0) => {
  const targetEl = document.querySelector('[data-cart-icon]')
  const startImgEl = productImageRef.value
  const startEl = startImgEl || cardImageRef.value
  const imgSrc = (startImgEl && (startImgEl.currentSrc || startImgEl.src)) || props.product.image

  if (!startEl || !targetEl || !imgSrc) {
    if (retry < 6) {
      window.setTimeout(() => flyToCart(retry + 1), 80)
    }
    return
  }

  const startRect = startEl.getBoundingClientRect()
  if (!startRect.width || !startRect.height) {
    if (retry < 6) {
      window.setTimeout(() => flyToCart(retry + 1), 80)
    }
    return
  }

  const targetRect = targetEl.getBoundingClientRect()
  const startSize = Math.min(120, Math.max(56, startRect.width * 0.28))
  const startLeft = startRect.left + startRect.width / 2 - startSize / 2
  const startTop = startRect.top + startRect.height / 2 - startSize / 2

  const flyImg = document.createElement('img')
  flyImg.src = imgSrc
  flyImg.alt = 'cart-fly'
  flyImg.decoding = 'async'
  flyImg.loading = 'eager'
  flyImg.style.position = 'fixed'
  flyImg.style.left = `${startLeft}px`
  flyImg.style.top = `${startTop}px`
  flyImg.style.width = `${startSize}px`
  flyImg.style.height = `${startSize}px`
  flyImg.style.borderRadius = '10px'
  flyImg.style.objectFit = 'cover'
  flyImg.style.pointerEvents = 'none'
  flyImg.style.zIndex = '9999'
  flyImg.style.boxShadow = '0 8px 20px rgba(0,0,0,0.15)'
  flyImg.style.transition = 'transform 0.72s cubic-bezier(0.65, -0.1, 0.2, 1.2), opacity 0.72s ease'
  flyImg.style.transform = 'translate(0, 0) scale(1)'
  flyImg.style.opacity = '1'

  document.body.appendChild(flyImg)

  const startX = startLeft + startSize / 2
  const startY = startTop + startSize / 2
  const endX = targetRect.left + targetRect.width / 2
  const endY = targetRect.top + targetRect.height / 2
  const deltaX = endX - startX
  const deltaY = endY - startY

  const cleanup = () => {
    flyImg.removeEventListener('transitionend', cleanup)
    if (flyImg.parentNode) flyImg.parentNode.removeChild(flyImg)
  }

  const run = () => {
    flyImg.getBoundingClientRect()
    requestAnimationFrame(() => {
      flyImg.style.transform = `translate(${deltaX}px, ${deltaY}px) scale(0.12)`
      flyImg.style.opacity = '0.2'
    })

    targetEl.classList.add('cart-bounce')
    window.setTimeout(() => {
      targetEl.classList.remove('cart-bounce')
    }, 400)

    flyImg.addEventListener('transitionend', cleanup)
    window.setTimeout(cleanup, 850)
  }

  try {
    if (typeof flyImg.decode === 'function') {
      flyImg.decode().then(run).catch(run)
    } else if (flyImg.complete) {
      run()
    } else {
      flyImg.onload = run
      window.setTimeout(run, 220)
    }
  } catch (error) {
    run()
  }
}

const addToCart = async () => {
  if (addingToCart.value) return
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再加入购物车')
    userStore.showLogin()
    return
  }

  addingToCart.value = true
  try {
    await request.post('/cart/add', {
      productId: props.product.id,
      quantity: 1
    })
    ElMessage.success('已加入购物车')
    flyToCart()
    await userStore.fetchCartCount()
  } catch (error) {
    // request.js 会统一展示接口错误，避免这里再重复提示。
  } finally {
    addingToCart.value = false
  }
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

      &:disabled {
        cursor: not-allowed;
        opacity: 0.7;
        transform: none;
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
