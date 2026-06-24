<template>
  <div class="product-detail-container" v-loading="loading">
  <!-- 面包屑 -->
    <div class="breadcrumb-area">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/shop' }">商城</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/mall/list' }">全部商品</el-breadcrumb-item>
        <el-breadcrumb-item>{{ product.name || '商品详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

  <!-- 顶部区域 -->
    <div class="product-main">
    <!-- 左侧：图库 -->
      <div class="gallery-section">
        <div class="main-image" ref="mainImageRef">
          <img :src="currentImage" alt="Main" loading="lazy" decoding="async" />
        </div>
        <div class="thumbnail-list">
          <div 
            v-for="(img, index) in productImages" 
            :key="index"
            class="thumb-item"
            :class="{ active: currentImage === img }"
            @mouseenter="currentImage = img"
          >
            <img :src="img" alt="thumb" loading="lazy" decoding="async" />
          </div>
        </div>
      </div>

    <!-- 右侧：信息区 -->
      <div class="info-section">
        <h1 class="product-title">{{ product.name }}</h1>
        <p class="product-subtitle">{{ product.subTitle || product.description }}</p>
        
        <div class="price-box">
          <span class="currency">¥</span>
          <span class="price">{{ displayPrice }}</span>
          <span class="stock-info" v-if="displayStock">库存: {{ displayStock }}</span>
        </div>

      <!-- 分类信息 -->
        <div class="spec-group" v-if="product.categoryName">
          <div class="label">分类</div>
          <div class="options">
            <span class="option-pill selected">{{ product.categoryName }}</span>
          </div>
        </div>

      <!-- 数量 -->
        <div class="spec-group">
          <div class="label">数量</div>
          <el-input-number v-model="quantity" :min="1" :max="99" size="large" />
        </div>

      <!-- 操作区 -->
        <div class="action-buttons">
          <button class="btn btn-cart" @click="addToCart" :disabled="addingToCart">
            {{ addingToCart ? '添加中...' : '加入购物车' }}
          </button>
          <button class="btn btn-buy" @click="buyNow">立即购买</button>
        </div>
        
        <div class="service-guarantee">
          <span><el-icon><CircleCheck /></el-icon> 正品保证</span>
          <span><el-icon><CircleCheck /></el-icon> 极速发货</span>
          <span><el-icon><CircleCheck /></el-icon> 七天无理由退换</span>
        </div>
      </div>
    </div>

  <!-- 底部区域：标签页 -->
    <div class="product-tabs">
      <el-tabs v-model="activeTab">
        
        <el-tab-pane label="商品详情" name="detail">
          <div class="detail-content">
             <img v-for="(img, idx) in safeDetailImgs" :key="idx" :src="img" alt="Detail Image" loading="lazy" decoding="async" />
             <p v-if="product.description">{{ product.description }}</p>
             <p v-else class="empty-hint">暂无详细介绍</p>
          </div>
        </el-tab-pane>
        <el-tab-pane name="reviews">
          <template #label>
            <span>{{ reviewTabTitle }}</span>
          </template>

          <div class="reviews-panel">
            <div class="review-summary">
              <div class="summary-score">
                <span class="score-value">{{ reviewAvgDisplay }}</span>
                <span class="score-unit">分</span>
              </div>
              <div class="summary-meta">
                <el-rate
                  :model-value="reviewAvgScore"
                  disabled
                  :max="5"
                  :allow-half="true"
                  :colors="['#ff6b81', '#ff6b81', '#ff6b81']"
                />
                <span class="meta-count">共 {{ reviewTotal }} 条真实评价</span>
              </div>
            </div>

            <div v-if="productReviews.length === 0" class="reviews-empty">
              <el-empty description="暂无评价，欢迎首评" />
            </div>

            <div v-else class="review-list">
              <div v-for="review in productReviews" :key="review.orderId" class="review-item">
                <div class="review-header">
                  <div class="review-user">
                    <el-avatar :size="32" :src="getAvatarUrl(review.userAvatar)" />
                    <div class="user-meta">
                      <span class="name">{{ review.nickname || review.username || '匿名用户' }}</span>
                      <span class="time">{{ formatReviewTime(review.reviewTime) }}</span>
                    </div>
                  </div>
                  <div class="review-actions">
                    <el-rate
                      :model-value="Number(review.score || 0)"
                      disabled
                      :max="5"
                      :colors="['#ff6b81', '#ff6b81', '#ff6b81']"
                    />
                    <el-button
                      v-if="canDeleteReview(review)"
                      link
                      type="danger"
                      size="small"
                      @click.stop="handleDeleteReview(review)"
                    >
                      删除评价
                    </el-button>
                  </div>
                </div>
                <p class="review-content">{{ review.content || '该用户没有填写文字评价。' }}</p>
                <div class="review-foot">
                  <span>购买数量：x{{ review.quantity || 1 }}</span>
                  <span>订单号：{{ review.orderSn || '-' }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane name="notes">
          <template #label>
            <span>{{ notesTitle }}</span>
          </template>
          
          <!-- 写笔记引导 -->
          <div class="notes-header">
            <span class="notes-count">共 {{ relatedNotes.length }} 条笔记</span>
            <el-button type="primary" size="small" @click="goPublish" round>
              <el-icon style="margin-right: 4px;"><Edit /></el-icon>
              写笔记
            </el-button>
          </div>
          
          <div class="notes-grid" v-if="relatedNotes.length > 0">
             <div v-for="note in relatedNotes" :key="note.id" class="note-card" @click="openNote(note.id)">
                <div class="note-cover">
                   <img :src="getImageUrl(note.coverThumb || note.coverImg)" alt="cover" loading="lazy" decoding="async" />
                   <div v-if="note.type === 'video'" class="video-badge">
                     <el-icon><VideoPlay /></el-icon>
                   </div>
                </div>
                <div class="note-info">
                   <div class="note-title">{{ note.title }}</div>
                   <div class="note-user">
                      <el-avatar :size="20" :src="getAvatarUrl(note.authorAvatar)" />
                      <span>{{ note.authorName || note.authorNickname }}</span>
                      <span class="likes"><el-icon><Star /></el-icon> {{ note.likeCount || 0 }}</span>
                   </div>
                </div>
             </div>
          </div>
          
          <!-- 空状态 -->
          <div v-else class="notes-empty">
            <el-empty description="暂无关联笔记，快来发布第一篇吧！">
              <el-button type="primary" @click="goPublish">
                <el-icon><Edit /></el-icon>
                立即发布
              </el-button>
            </el-empty>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCheck, Star, Edit, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { getImageUrl, getAvatarUrl } from '@/utils/image'
import { useUserStore } from '@/store/user'
import { STALE_REFRESH_MS, useStaleRefresh } from '@/utils/staleRefresh'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const currentProductId = computed(() => {
  const id = Number(route.params.id)
  return Number.isFinite(id) && id > 0 ? id : null
})

// 加载状态
const loading = ref(false)
const addingToCart = ref(false)

// 商品数据 - 从后端获取
const product = ref({
  id: null,
  name: '',
  subTitle: '',
  price: 0,
  stock: 0,
  coverImg: '',
  detailImgs: [],
  description: '',
  categoryId: null,
  categoryName: '',
  reviewCount: 0,
  reviewAvgScore: 0,
  reviewList: []
})

const safeCoverImg = computed(() => (product.value.coverImg ? getImageUrl(product.value.coverImg) : ''))

const safeCoverImgs = computed(() =>
  normalizeDetailImgs(product.value.coverImgs || product.value.coverImg)
    .filter(Boolean)
    .filter((x) => typeof x === 'string' && !x.startsWith('blob:'))
    .map((x) => getImageUrl(x))
)

const safeDetailImgs = computed(() =>
  normalizeDetailImgs(product.value.detailImgs)
    .filter(Boolean)
    .filter((x) => typeof x === 'string' && !x.startsWith('blob:')) // 防御：历史脏数据（blob）不展示
    .map((x) => getImageUrl(x))
)

// 计算属性：图片列表（用于主图+缩略图）
const productImages = computed(() => {
  const images = []
  if (safeCoverImgs.value.length > 0) {
    images.push(...safeCoverImgs.value)
  } else if (safeCoverImg.value) {
    images.push(safeCoverImg.value)
  }
  // 如果没有图片，返回占位图
  return images.length > 0 ? images : ['https://picsum.photos/id/237/600/600']
})

const currentImage = ref('')
const mainImageRef = ref(null)
const quantity = ref(1)

const activeTab = ref('detail')

const displayPrice = computed(() => product.value.price ?? 0)
const displayStock = computed(() => product.value.stock ?? 0)
const productReviews = computed(() => (Array.isArray(product.value.reviewList) ? product.value.reviewList : []))
const reviewTotal = computed(() => {
  const n = Number(product.value.reviewCount)
  return Number.isFinite(n) ? n : productReviews.value.length
})
const reviewAvgScore = computed(() => {
  const n = Number(product.value.reviewAvgScore || 0)
  return Number.isFinite(n) ? n : 0
})
const reviewAvgDisplay = computed(() => reviewAvgScore.value.toFixed(1))
const reviewTabTitle = computed(() => `用户评价 (${reviewTotal.value})`)
const currentUserId = computed(() => {
  const id = Number(userStore.userInfo?.id)
  return Number.isInteger(id) && id > 0 ? id : null
})

// 笔记数据
const relatedNotes = ref([])
const notesLoading = ref(false)

// 动态标题
const notesTitle = computed(() => {
  if (notesLoading.value) return '加载中...'
  return `大家怎么说 (${relatedNotes.value.length})`
})

let detailRequestToken = 0
let notesRequestToken = 0

const normalizeDetailImgs = (detailImgs) => {
  if (!detailImgs) return []
  if (Array.isArray(detailImgs)) return detailImgs
  if (typeof detailImgs === 'string') {
    const str = detailImgs.trim()
    if (!str) return []
    // 后端可能返回 JSON 字符串：'["/images/...","/images/..."]'
    if (str.startsWith('[')) {
      try {
        const parsed = JSON.parse(str)
        return Array.isArray(parsed) ? parsed : []
      } catch (e) {
        return []
      }
    }
  }
  return []
}

const formatReviewTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

const canDeleteReview = (review) => {
  const myId = currentUserId.value
  const reviewUserId = Number(review?.userId)
  const orderId = Number(review?.orderId)
  if (!myId) return false
  return Number.isInteger(reviewUserId)
    && reviewUserId === myId
    && Number.isInteger(orderId)
    && orderId > 0
}

const handleDeleteReview = async (review) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再操作')
    userStore.showLogin()
    return
  }
  if (!canDeleteReview(review)) {
    ElMessage.warning('仅可删除自己的评价')
    return
  }

  try {
    await ElMessageBox.confirm('删除后该评价将不再显示在商品页，确定继续吗？', '删除评价', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await request.delete(`/order/review/${review.orderId}`)
    ElMessage.success('评价已删除')
    await fetchProductDetail()
  } catch (e) {
    // 用户取消了操作
  }
}

// 获取商品详情
const fetchProductDetail = async (id = currentProductId.value, options = {}) => {
  if (!id) return
  const { silent = false } = options
  const requestToken = ++detailRequestToken
  if (!silent) {
    loading.value = true
  }
  try {
    const res = await request.get(`/product/detail/${id}`, { silentError: silent })
    if (requestToken !== detailRequestToken) return
    if (res) {
      // 注意：detailImgs 可能是 JSON 字符串或数组；这里保持原样交给 computed 做统一处理
      product.value = { ...res }
      currentImage.value = productImages.value[0]
    }
  } catch (error) {
    if (!silent && requestToken === detailRequestToken) {
      ElMessage.error('获取商品详情失败')
    }
  } finally {
    if (!silent && requestToken === detailRequestToken) {
      loading.value = false
    }
  }
}

const flyToCart = (retry = 0) => {
  const targetEl = document.querySelector('[data-cart-icon]')
  const startImgEl =
    (mainImageRef.value && mainImageRef.value.querySelector && mainImageRef.value.querySelector('img')) ||
    document.querySelector('.product-detail-container .main-image img')

  const startEl = startImgEl || mainImageRef.value || document.querySelector('.btn-cart')
  const imgSrc =
    (startImgEl && (startImgEl.currentSrc || startImgEl.src)) ||
    currentImage.value ||
    productImages.value[0] ||
    safeCoverImg.value

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

  const startSize = Math.min(160, Math.max(80, startRect.width * 0.3))
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
  flyImg.style.transition = 'transform 0.8s cubic-bezier(0.65, -0.1, 0.2, 1.2), opacity 0.8s ease'
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
    // 强制回流一下，低性能设备上过渡更容易稳定触发
    flyImg.getBoundingClientRect()
    requestAnimationFrame(() => {
      flyImg.style.transform = `translate(${deltaX}px, ${deltaY}px) scale(0.1)`
      flyImg.style.opacity = '0.2'
    })

    targetEl.classList.add('cart-bounce')
    window.setTimeout(() => {
      targetEl.classList.remove('cart-bounce')
    }, 400)

    flyImg.addEventListener('transitionend', cleanup)
    window.setTimeout(cleanup, 900)
  }

  // 刚进页就点加入购物车时，图片可能还没解码完。
  // 这里稍微等一下飞行动画用图，避免动画跑完了图才绘制出来。
  try {
    if (typeof flyImg.decode === 'function') {
      flyImg.decode().then(run).catch(run)
    } else if (flyImg.complete) {
      run()
    } else {
      flyImg.onload = run
      window.setTimeout(run, 220)
    }
  } catch (e) {
    run()
  }
}

// 加入购物车
const addToCart = async () => {
  // 检查登录状态
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再加入购物车')
    userStore.showLogin()
    return
  }
  
  addingToCart.value = true
  try {
    await request.post('/cart/add', {
      productId: product.value.id,
      quantity: quantity.value
    })
    ElMessage.success('已加入购物车')
    flyToCart()
    // 刷新购物车数量
    userStore.fetchCartCount()
  } catch (error) {
    // request.js 拦截器已处理错误提示
  } finally {
    addingToCart.value = false
  }
}

// 立即购买
const buyNow = async () => {
  // 先加入购物车，然后跳转结算
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再购买')
    userStore.showLogin()
    return
  }
  
  try {
    await request.post('/cart/add', {
      productId: product.value.id,
      quantity: quantity.value
    })
    router.push('/cart')
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 打开笔记详情
const openNote = (id) => {
  router.push({ name: 'NoteDetail', params: { id } })
}

// 获取关联笔记（含兜底策略）
const fetchRelatedNotes = async (id = currentProductId.value, options = {}) => {
  const { silent = false } = options
  if (!id) {
    relatedNotes.value = []
    notesLoading.value = false
    return
  }
  const requestToken = ++notesRequestToken
  if (!silent) {
    notesLoading.value = true
  }
  
  try {
    // 仅获取该商品关联笔记
    const res = await request.get('/note/list', {
      params: { productId: id, pageNum: 1, pageSize: 4 },
      silentError: silent
    })
    if (requestToken !== notesRequestToken) return
    
    const notes = res?.records || res || []
    relatedNotes.value = notes
  } catch (error) {
    if (requestToken !== notesRequestToken) return
    console.error('获取笔记失败', error)
    if (!silent) {
      relatedNotes.value = []
    }
  } finally {
    if (!silent && requestToken === notesRequestToken) {
      notesLoading.value = false
    }
  }
}

// 跳转发布页面（带商品 ID）
const goPublish = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再发布内容')
    userStore.showLogin()
    return
  }
  if (!currentProductId.value) {
    ElMessage.warning('商品信息异常，请返回后重试')
    return
  }
  router.push({
    path: '/publish',
    query: { productId: currentProductId.value }
  })
}

const restoreWindowScroll = async (scrollTop) => {
  await new Promise((resolve) => requestAnimationFrame(resolve))
  const maxScrollTop = Math.max(0, document.documentElement.scrollHeight - window.innerHeight)
  window.scrollTo(0, Math.min(scrollTop, maxScrollTop))
}

const refreshProductDetailSilently = async () => {
  const id = currentProductId.value
  if (!id) return
  const scrollTop = window.scrollY || document.documentElement.scrollTop || document.body.scrollTop || 0

  await Promise.all([
    fetchProductDetail(id, { silent: true }),
    fetchRelatedNotes(id, { silent: true })
  ])
  await restoreWindowScroll(scrollTop)
}

const productStaleRefresh = useStaleRefresh({
  staleMs: STALE_REFRESH_MS.detail,
  refresh: refreshProductDetailSilently,
  isRefreshing: () => loading.value || notesLoading.value,
  shouldSkip: () => !currentProductId.value
})

watch(
  () => currentProductId.value,
  (newId, oldId) => {
    if (!newId) return
    if (newId !== oldId) {
      activeTab.value = 'detail'
      quantity.value = 1
      relatedNotes.value = []
    }
    fetchProductDetail(newId)
    fetchRelatedNotes(newId)
    productStaleRefresh.markFresh()
  },
  { immediate: true }
)
</script>

<style scoped lang="scss">
.product-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.breadcrumb-area {
  margin-bottom: 24px;
}

.product-main {
  display: flex;
  gap: 40px;
  margin-bottom: 60px;
  
  @media(max-width: 768px) {
    flex-direction: column;
  }
}

.gallery-section {
  width: 50%;
  
  @media(max-width: 768px) {
    width: 100%;
  }
  
  .main-image {
    width: 100%;
    aspect-ratio: 1;
    border-radius: 12px;
    overflow: hidden;
    margin-bottom: 16px;
    border: 1px solid #f0f0f0;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  
  .thumbnail-list {
    display: flex;
    gap: 12px;
    
    .thumb-item {
      width: 80px;
      height: 80px;
      border-radius: 8px;
      overflow: hidden;
      cursor: pointer;
      border: 2px solid transparent;
      transition: all 0.2s;
      
      &.active, &:hover {
        border-color: #FF6B81;
      }
      
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
    }
  }
}

.info-section {
  width: 50%;
  padding-top: 20px;

  @media(max-width: 768px) {
    width: 100%;
  }
  
  .product-title {
    font-size: 28px;
    font-weight: 700;
    color: #333;
    margin-bottom: 12px;
    line-height: 1.3;
  }
  
  .product-subtitle {
    font-size: 14px;
    color: #666;
    margin-bottom: 24px;
    line-height: 1.5;
  }
  
  .price-box {
    margin-bottom: 30px;
    display: flex;
    align-items: baseline;
    
    .currency {
      color: #ff2442;
      font-size: 18px;
      font-weight: 600;
    }
    
    .price {
      color: #ff2442;
      font-size: 36px;
      font-weight: 700;
      margin-right: 12px;
    }
    
    .original-price {
      color: #999;
      text-decoration: line-through;
      margin-right: 12px;
    }
    
    .sales-tag {
      background: #FFF0F3;
      color: #FF6B81;
      font-size: 12px;
      padding: 2px 8px;
      border-radius: 4px;
    }
  }
  
  .spec-group {
    margin-bottom: 24px;
    
    .label {
      font-size: 14px;
      color: #333;
      margin-bottom: 12px;
      font-weight: 500;
    }
    
    .options {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      
      .option-pill {
        padding: 8px 20px;
        border: 1px solid #ddd;
        border-radius: 4px;
        color: #666;
        cursor: pointer;
        transition: all 0.2s;
        
        &:hover {
          color: #FF6B81;
          border-color: #FF6B81;
        }
        
        &.selected {
          color: #FF6B81;
          border-color: #FF6B81;
          background: #FFF0F3;
        }
      }
    }
  }
  
  .action-buttons {
    display: flex;
    gap: 20px;
    margin-bottom: 30px;
    margin-top: 40px;
    
    .btn {
      flex: 1;
      height: 48px;
      border-radius: 24px;
      font-size: 16px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      
      &.btn-cart {
        background: #fff;
        border: 1px solid #333;
        color: #333;
        
        &:hover {
          background: #f8f8f8;
        }
      }
      
      &.btn-buy {
        background: #FF6B81;
        border: 1px solid #FF6B81;
        color: #fff; // Brand Pink
        
        &:hover {
           background: #FF5A72;
           border-color: #FF5A72;
        }
      }
    }
  }
  
  .service-guarantee {
    display: flex;
    gap: 24px;
    color: #666;
    font-size: 13px;
    
    span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .el-icon {
      color: #FF6B81;
    }
  }
}

.product-tabs {
  margin-top: 40px;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  
  .reviews-panel {
    margin-top: 8px;
  }

  .review-summary {
    display: flex;
    align-items: center;
    gap: 20px;
    padding: 16px 20px;
    background: #fff3f6;
    border: 1px solid #ffe1e9;
    border-radius: 12px;
    margin-bottom: 16px;

    .summary-score {
      color: #ff5a72;
      display: flex;
      align-items: baseline;
      gap: 4px;
      min-width: 72px;
    }

    .score-value {
      font-size: 34px;
      line-height: 1;
      font-weight: 700;
    }

    .score-unit {
      font-size: 14px;
      font-weight: 600;
    }

    .summary-meta {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .meta-count {
      color: #7a7a7a;
      font-size: 13px;
    }
  }

  .reviews-empty {
    padding: 28px 0;
  }

  .review-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .review-item {
    border: 1px solid #f1f1f1;
    border-radius: 12px;
    padding: 14px 16px;
    background: #fff;
  }

  .review-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .review-actions {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .review-user {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .user-meta {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .user-meta .name {
    font-size: 14px;
    font-weight: 600;
    color: #333;
  }

  .user-meta .time {
    font-size: 12px;
    color: #9a9a9a;
  }

  .review-content {
    margin: 12px 0 10px;
    color: #444;
    font-size: 14px;
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-word;
  }

  .review-foot {
    display: flex;
    gap: 16px;
    color: #9a9a9a;
    font-size: 12px;
  }

  .notes-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    
    .notes-count {
      font-size: 14px;
      color: #999;
    }
  }
  
  .notes-empty {
    padding: 40px 0;
    text-align: center;
  }
  
  .notes-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    margin-top: 20px;
    
    @media(max-width: 992px) {
      grid-template-columns: repeat(3, 1fr);
    }
    @media(max-width: 768px) {
      grid-template-columns: repeat(2, 1fr);
    }

    .note-card {
      cursor: pointer;
      border-radius: 8px;
      overflow: hidden;
      
      &:hover .note-cover img {
        transform: scale(1.05);
      }
      
      .note-cover {
        aspect-ratio: 3/4;
        width: 100%;
        overflow: hidden;
        border-radius: 8px;
        margin-bottom: 8px;
        position: relative;
        
        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
          transition: transform 0.3s;
        }

        .video-badge {
          position: absolute;
          top: 8px;
          right: 8px;
          width: 24px;
          height: 24px;
          border-radius: 50%;
          background: rgba(255, 255, 255, 0.75);
          display: flex;
          align-items: center;
          justify-content: center;
          color: #333;
          backdrop-filter: blur(4px);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
        }

        .video-badge .el-icon {
          font-size: 14px;
        }
      }
      
      .note-info {
        .note-title {
          font-size: 14px;
          color: #333;
          margin-bottom: 6px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        
        .note-user {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 12px;
          color: #999;
          
          .likes {
            margin-left: auto;
            display: flex;
            align-items: center;
            gap: 2px;
          }
        }
      }
    }
  }
  
  .detail-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 40px 0;
    
    img {
      max-width: 800px;
      width: 100%;
      margin-bottom: 20px;
    }
    
    p {
      color: #666;
      font-size: 14px;
    }
  }
}
</style>
