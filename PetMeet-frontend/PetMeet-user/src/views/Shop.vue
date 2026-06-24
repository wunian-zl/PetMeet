<template>
  <div class="shop-container">
  <!-- 顶部：搜索与操作 -->
  <!-- 顶部：搜索与操作 -->
    <div class="shop-header-bar" :class="{ 'is-sticky': isSticky }">
      <div class="search-box">
        <el-icon class="search-icon"><Search /></el-icon>
        <input
          v-model="searchKeyword"
          placeholder="搜索好物 (如: 零食 / 玩具 / 驱虫)"
          @keyup.enter="handleSearch"
        />
        <button class="search-btn" @click="handleSearch">搜索</button>
      </div>
    <!-- 右侧图标（通知/购物车） -->
      <div class="header-actions">
        <div
          class="action-item"
          role="button"
          tabindex="0"
          aria-label="查看通知"
          @click="goNotification"
          @keydown.enter="goNotification"
          @keydown.space.prevent="goNotification"
        >
          <el-icon :size="22"><Bell /></el-icon>
        </div>
        <div
          class="action-item"
          role="button"
          tabindex="0"
          aria-label="查看购物车"
          @click="goCart"
          @keydown.enter="goCart"
          @keydown.space.prevent="goCart"
        >
          <el-icon :size="22"><ShoppingCart /></el-icon>
        </div>
      </div>
    </div>

  <!-- 顶部横幅 -->
  <!-- 顶部横幅 -->
    <div class="shop-banner">
      <el-carousel trigger="click" height="420px" :interval="5000" arrow="hover">
        <el-carousel-item v-for="(item, index) in banners" :key="item.id || index">
          <div class="banner-item" :style="{ background: item.bg }">
            <div class="banner-content">
              <div class="banner-text">
                <h3>{{ item.title }}</h3>
                <p>{{ item.sub }}</p>
                <button class="banner-btn" @click="handleBannerClick(item)">立即选购</button>
              </div>
              <img :src="item.img" class="banner-img" />
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

  <!-- 分类导航 -->
    <div class="category-nav">
      <div v-for="cat in categories" :key="cat.id" class="cat-item" @click="goToCategory(cat)">
        <div class="cat-icon">
          <el-icon v-if="cat.icon && !isIconUrl(cat.icon)" :size="28" color="#444">
            <component :is="cat.icon" />
          </el-icon>
          <img v-else-if="cat.icon" :src="cat.icon" class="cat-icon-img" alt="" />
        </div>
        <span class="cat-name">{{ cat.name }}</span>
      </div>
    </div>

  <!-- 精选分区：猜你喜欢 -->
    <section class="curated-section">
      <div class="section-header">
        <h2>猜你喜欢 <span class="subtitle">For You</span></h2>
        <button class="view-all" @click="refreshNewArrivals">换一批 ></button>
      </div>
      <div class="horizontal-scroll snap-scroll">
        <div v-for="product in newArrivals" :key="product.id" class="scroll-item snap-item">
          <ShopProductCard :product="product" />
        </div>
      </div>
    </section>

  <!-- 精选分区：热销推荐 -->
    <section class="curated-section">
      <div class="section-header">
        <h2>大家都在买 <span class="subtitle">Best Sellers</span></h2>
      </div>
      <div class="grid-layout" v-if="!isLoading || bestSellers.length > 0">
        <ShopProductCard 
          v-for="product in bestSellers" 
          :key="product.id" 
          :product="product" 
        />
      </div>
      
      <!-- 骨架屏加载 -->
      <div class="grid-layout" v-if="isLoading && bestSellers.length === 0">
         <div v-for="n in 8" :key="n" class="skeleton-card">
           <el-skeleton animated>
             <template #template>
               <el-skeleton-item variant="image" style="width: 100%; height: 200px; border-radius: 8px;" />
               <div style="padding: 14px;">
                 <el-skeleton-item variant="p" style="width: 50%" />
                 <div style="display: flex; align-items: center; justify-content: space-between; margin-top: 10px;">
                   <el-skeleton-item variant="text" style="width: 30%" />
                   <el-skeleton-item variant="text" style="width: 20%" />
                 </div>
               </div>
             </template>
           </el-skeleton>
         </div>
      </div>
      <!-- 加载状态 -->
      <div class="load-more-area">
        <div v-if="isLoading" class="loading-indicator">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        <div v-else-if="!hasMore" class="no-more">
          <span>—— 已经到底啦 ——</span>
        </div>
      </div>
    </section>

  <!-- 底部留白 -->
    <div class="footer-spacer"></div>

    <el-backtop :visibility-height="400" :right="24" :bottom="24" />
  </div>
</template>

<script>
export default {
  name: 'Shop'
}
</script>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Loading, Bell, ShoppingCart } from '@element-plus/icons-vue'
import ShopProductCard from '@/components/ShopProductCard.vue'
import request from '@/utils/request'
import { getImageUrl } from '@/utils/image'
import { STALE_REFRESH_MS, useStaleRefresh } from '@/utils/staleRefresh'

const BANNER_POSITION = 'SHOP_HERO'
const bannerBgPresets = [
  'linear-gradient(135deg, #fdfbfb 0%, #ebedee 100%)', // 1. 经典灰白 (Classic Mist)
  'linear-gradient(135deg, #ffffff 0%, #f0f0f0 100%)', // 2. 纯净白灰 (Pure White-Gray)
  'linear-gradient(135deg, #f5f7fa 0%, #e8d7dc 100%)'  // 3. 暖调银灰 (Warm Silver)
]

// 管理端没配轮播时，先用这组兜底数据
const fallbackBanners = [
  { id: 'fallback-1', title: '春季爱宠焕新', sub: '精选洗护、服饰 满199减50', bg: bannerBgPresets[0], img: '/images/banners/spring_sale.svg', linkType: 'internal', linkUrl: '/mall/list' },
  { id: 'fallback-2', title: '健康喂养指南', sub: '进口粮/冻干/罐头 低至5折', bg: bannerBgPresets[1], img: '/images/banners/healthy_food.svg', linkType: 'internal', linkUrl: '/mall/list' },
  { id: 'fallback-3', title: '快乐拆家计划', sub: '耐咬玩具/益智漏食球 上新', bg: bannerBgPresets[2], img: '/images/banners/happy_play.svg', linkType: 'internal', linkUrl: '/mall/list' }
]

const banners = ref([...fallbackBanners])

const searchKeyword = ref('')
const handleSearch = () => {
  if (!searchKeyword.value.trim()) return
  router.push({ path: '/mall/list', query: { keyword: searchKeyword.value } })
}

const goNotification = () => {
  router.push('/notification')
}

const goCart = () => {
  router.push('/cart')
}

// 默认分类图标映射
const defaultCategoryIcons = [
  '/category-icons/snack.svg',
  '/category-icons/poop-bag.svg',
  '/category-icons/frisbee.svg',
  '/category-icons/dog-food.svg',
  '/category-icons/cat-food.svg',
  '/category-icons/home.svg',
  '/category-icons/leash.svg',
  '/category-icons/tshirt.svg',
  '/category-icons/medical.svg',
  '/category-icons/chip.svg'
]
const categoryIconByName = {
  零食: '/category-icons/snack.svg',
  清洁卫生: '/category-icons/poop-bag.svg',
  玩具: '/category-icons/frisbee.svg',
  狗粮: '/category-icons/dog-food.svg',
  猫粮: '/category-icons/cat-food.svg',
  居家: '/category-icons/home.svg',
  出行: '/category-icons/leash.svg',
  出行装备: '/category-icons/leash.svg',
  服饰: '/category-icons/tshirt.svg',
  医疗保健: '/category-icons/medical.svg',
  宠物医疗: '/category-icons/medical.svg',
  智能设备: '/category-icons/chip.svg'
}
const isIconUrl = (icon) => typeof icon === 'string' && (icon.startsWith('http') || icon.startsWith('/'))
const resolveCategoryIcon = (cat, index) => {
  if (cat?.icon) return cat.icon
  return categoryIconByName[cat?.name] || defaultCategoryIcons[index % defaultCategoryIcons.length]
}
const categories = ref([])
const router = useRouter()

const fetchShopBanners = async (options = {}) => {
  const { silent = false } = options
  try {
    const res = await request.get(`/banner/position/${BANNER_POSITION}`, { silentError: silent })
    if (Array.isArray(res) && res.length > 0) {
      banners.value = res.map((b, idx) => ({
        id: b.id,
        title: b.title,
        sub: b.keyword || '',
        bg: bannerBgPresets[idx % bannerBgPresets.length],
        img: getImageUrl(b.imageUrl),
        linkType: b.linkType || 'internal',
        linkUrl: b.linkUrl || ''
      }))
    } else {
      banners.value = [...fallbackBanners]
    }
  } catch (e) {
    if (!silent) {
      banners.value = [...fallbackBanners]
    }
  }
}

const handleBannerClick = (banner) => {
  if (!banner) return
  let linkUrl = banner.linkUrl || ''
  const linkType = banner.linkType || ''
  if (!linkUrl) return

  if (linkType === 'url' || linkUrl.startsWith('http://') || linkUrl.startsWith('https://')) {
    window.open(linkUrl, '_blank')
    return
  }
  // 若“近N天”筛选已过期，则自动按“不限制”跳转
  if (linkUrl.includes('recentDaysUntil')) {
    const [path, qs] = linkUrl.split('?')
    const params = new URLSearchParams(qs || '')
    const until = params.get('recentDaysUntil')
    if (until) {
      const d = new Date(`${until}T23:59:59`)
      if (!Number.isNaN(d.getTime()) && Date.now() > d.getTime()) {
        params.delete('recentDays')
        params.delete('recentDaysUntil')
      }
    }
    const query = params.toString()
    linkUrl = query ? `${path}?${query}` : path
  }

  if (typeof linkUrl !== 'string') return
  linkUrl = linkUrl.trim()
  if (!linkUrl || linkUrl === 'undefined' || linkUrl.startsWith('undefined')) {
    return
  }
  if (!linkUrl.startsWith('/')) {
    linkUrl = `/${linkUrl}`
  }

  router.push(linkUrl)
}

// 商品数据
const newArrivals = ref([])
const newArrivalPool = ref([])
const bestSellers = ref([])

// 加载状态
const isLoading = ref(false)
const hasMore = ref(true)
const pageNum = ref(1)
const total = ref(0)
let bestSellersRequesting = false
// 商城首页把分类、新品和热销分开拉取，任何一块失败都不影响另外两块。
const fetchCategories = async (options = {}) => {
  const { silent = false } = options
  try {
    const res = await request.get('/product/category/list', { silentError: silent })
    if (res && res.length > 0) {
      categories.value = res.slice(0, 6).map((cat, index) => ({
        id: cat.id,
        name: cat.name,
        icon: resolveCategoryIcon(cat, index)
      }))
    } else {
      // 使用默认分类
      categories.value = [
        { id: 1, name: '狗粮', icon: '/category-icons/dog-food.svg' },
        { id: 2, name: '猫粮', icon: '/category-icons/cat-food.svg' },
        { id: 3, name: '零食', icon: '/category-icons/snack.svg' },
        { id: 4, name: '清洁卫生', icon: '/category-icons/poop-bag.svg' },
        { id: 5, name: '玩具', icon: '/category-icons/frisbee.svg' },
        { id: 6, name: '宠物医疗', icon: '/category-icons/medical.svg' },
      ]
    }
  } catch (error) {
    console.error('获取分类失败', error)
  }
}

// 映射后端商品数据
const mapProduct = (item) => ({
  id: item.id,
  name: item.name,
  price: item.price,
  sales: item.sales || 0,
  image: item.coverImg || `/petmeetImage/Main-Commodity-image/1${String(Math.floor(Math.random() * 88) + 1).padStart(4, '0')}.jpg`
})

// 获取“猜你喜欢”商品
const shuffleArray = (arr) => {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
  }
  return arr
}

const refreshNewArrivals = () => {
  if (!newArrivalPool.value.length) return
  const shuffled = shuffleArray([...newArrivalPool.value])
  newArrivals.value = shuffled.slice(0, 5)
}

const fetchNewArrivals = async (options = {}) => {
  const { silent = false } = options
  try {
    const res = await request.get('/product/list', { 
      params: { pageNum: 1, pageSize: 20 },
      silentError: silent
    })
    if (res && res.records) {
      newArrivalPool.value = res.records.map(mapProduct)
      refreshNewArrivals()
    }
  } catch (error) {
    console.error('获取新品失败', error)
  }
}

// 获取热销商品
const fetchBestSellers = async (append = false, options = {}) => {
  if (bestSellersRequesting) return
  
  const { silent = false, pageSize: fetchPageSize = 8 } = options
  bestSellersRequesting = true
  if (!silent) {
    isLoading.value = true
  }
  try {
    const res = await request.get('/product/list', { 
      params: { pageNum: pageNum.value, pageSize: fetchPageSize },
      silentError: silent
    })
    
    if (res && res.records) {
      const products = res.records.map(mapProduct)
      total.value = res.total || 0
      
      if (append) {
        bestSellers.value.push(...products)
      } else {
        bestSellers.value = products
      }
      
      // 检查是否还有更多
      hasMore.value = bestSellers.value.length < total.value
    }
  } catch (error) {
    console.error('获取热销商品失败', error)
  } finally {
    bestSellersRequesting = false
    if (!silent) {
      isLoading.value = false
    }
  }
}

// 加载更多商品
const loadMoreProducts = () => {
  if (bestSellersRequesting || isLoading.value || !hasMore.value) return
  pageNum.value++
  fetchBestSellers(true)
}

// 触底检测和吸顶头部
let scrollTicking = false
const isSticky = ref(false)

const handleScroll = () => {
  if (scrollTicking) return
  scrollTicking = true
  requestAnimationFrame(() => {
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
    
    // 头部滚动一段距离后进入吸顶态
    isSticky.value = scrollTop > 20

    const scrollHeight = document.documentElement.scrollHeight
    const clientHeight = document.documentElement.clientHeight
    
    if (scrollTop + clientHeight >= scrollHeight - 200) {
      loadMoreProducts()
    }
    scrollTicking = false
  })
}

const goToCategory = (cat) => {
  router.push({
    path: '/mall/list',
    query: {
      categoryId: cat.id,
      category: cat.name
    }
  })
}

const restoreWindowScroll = async (scrollTop) => {
  await new Promise((resolve) => requestAnimationFrame(resolve))
  const maxScrollTop = Math.max(0, document.documentElement.scrollHeight - window.innerHeight)
  window.scrollTo(0, Math.min(scrollTop, maxScrollTop))
}

const refreshShopHomeData = async () => {
  const scrollTop = window.scrollY || document.documentElement.scrollTop || document.body.scrollTop || 0
  const originalPageSize = 8
  const refreshSize = Math.max(originalPageSize, bestSellers.value.length || originalPageSize)

  pageNum.value = 1
  await Promise.all([
    fetchShopBanners({ silent: true }),
    fetchCategories({ silent: true }),
    fetchNewArrivals({ silent: true }),
    fetchBestSellers(false, { silent: true, pageSize: refreshSize })
  ])
  pageNum.value = Math.max(1, Math.ceil((bestSellers.value.length || 1) / originalPageSize))
  await restoreWindowScroll(scrollTop)
}

const shopStaleRefresh = useStaleRefresh({
  staleMs: STALE_REFRESH_MS.commerce,
  refresh: refreshShopHomeData,
  isRefreshing: () => bestSellersRequesting || isLoading.value
})

onMounted(async () => {
  window.addEventListener('scroll', handleScroll)
  // 并行加载数据
  await Promise.all([
    fetchShopBanners(),
    fetchCategories(),
    fetchNewArrivals(),
    fetchBestSellers(false)
  ])
  shopStaleRefresh.markFresh()
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})</script>

<style scoped lang="scss">
.shop-container {
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 80px;
}

/* 头部和搜索框 */
.shop-header-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px 20px;
  position: sticky;
  top: 0;
  z-index: 999;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: transparent;
  width: 100%;
  box-sizing: border-box;

  &.is-sticky {
    background: linear-gradient(90deg, rgba(255,255,255,0) 0%, #ffffff 20%, #ffffff 80%, rgba(255,255,255,0) 100%);
    box-shadow: none; // Remove sharp shadow to blend with sides
    padding: 12px 20px;
  }
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 600px;
  background: rgba(255,255,255,0.9);
  border-radius: 99px;
  padding: 6px 6px 6px 20px;
  transition: all 0.3s;
  border: 1px solid rgba(0,0,0,0.05);
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);

  .is-sticky & {
    background: #f1f2f3;
  }

  &:focus-within {
    background: #fff;
    border-color: #ff6b81;
    box-shadow: 0 4px 12px rgba(255, 107, 129, 0.15);
  }

  .search-icon {
    font-size: 18px;
    color: #999;
    margin-right: 10px;
  }

  input {
    flex: 1;
    border: none;
    background: transparent;
    font-size: 15px;
    color: #333;
    outline: none;
    height: 36px;
    
    &::placeholder { color: #bbb; }
  }

  .search-btn {
    background: #ff6b81;
    color: #fff;
    border: none;
    border-radius: 99px;
    padding: 8px 24px;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.2s;
    flex-shrink: 0;
    
    &:hover {
      background: #ff4757;
    }
  }
}

.header-actions {
  display: flex;
  align-items: center;
  margin-left: 16px;
  gap: 12px;

  .action-item {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: rgba(255,255,255,0.8);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #333;
    transition: all 0.2s;

    &:focus-visible {
      outline: 2px solid rgba(255, 107, 129, 0.4);
      outline-offset: 3px;
    }
    
    &:hover {
      background: #fff;
      color: #ff6b81;
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    }
  }
}

/* 轮播横幅 */
.shop-banner {
  margin: 10px auto 40px; // adjust margin for sticky header
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 16px 40px -10px rgba(0,0,0,0.15);
  transform: translateZ(0); // Fix safari flickers

  // 把轮播箭头做大一点
  :deep(.el-carousel__arrow) {
    width: 56px;
    height: 56px;
    font-size: 24px;
    background-color: rgba(31, 45, 61, 0.15);
    
    &:hover {
      background-color: rgba(31, 45, 61, 0.3);
    }

    /* 箭头图标也一起放大 */
    i {
      font-weight: bold;
    }
  }
}

.banner-item {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 80px;
  position: relative;
}

.banner-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
}

.banner-text {
  color: #333; // Dark text for light pastel backgrounds
  text-shadow: none;
  
  h3 {
    font-size: 42px;
    margin: 0 0 12px;
    font-weight: 800;
    letter-spacing: -1px;
    color: #1a1a1a;
  }
  
  p {
    font-size: 20px;
    margin: 0 0 30px;
    opacity: 0.8;
    font-weight: 500;
    color: #444;
  }
  
  .banner-btn {
    background: #1a1a1a;
    color: #fff;
    border: none;
    padding: 12px 32px;
    font-size: 16px;
    font-weight: 700;
    border-radius: 99px;
    cursor: pointer;
    box-shadow: 0 8px 20px rgba(0,0,0,0.08);
    transition: all 0.2s ease;
    
    &:hover {
      transform: translateY(-2px);
      background: #333;
      box-shadow: 0 12px 24px rgba(0,0,0,0.12);
    }
  }
}

.banner-img {
  height: 300px;
  width: 300px;
  object-fit: cover;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.2);
  transform: rotate(-5deg);
  transition: transform 0.3s;
  
  &:hover {
    transform: rotate(0deg) scale(1.05);
  }
}

.category-nav {
  display: flex;
  justify-content: center;
  gap: 40px; 
  margin-top: 24px;
  margin-bottom: 60px;
  padding: 0 20px;
  flex-wrap: wrap; // safe wrap

  .cat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    cursor: pointer;
    transition: transform 0.2s ease;
    width: 80px;

    &:hover {
      transform: translateY(-5px);
      
      .cat-icon {
        box-shadow: 0 4px 12px rgba(0,0,0,0.08); // Hover shadow
        border-color: transparent; // Optional effect
        background: #fff;
      }
      
      .cat-name {
        color: #FF6B81; // Brand Pink
      }
    }

    .cat-icon {
      width: 60px;
      height: 60px;
      border-radius: 50%;
      background: linear-gradient(135deg, #fff 0%, #f9f9f9 100%);
      box-shadow: 0 6px 16px rgba(0,0,0,0.06);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 12px;
      transition: all 0.3s ease;
      position: relative;
      overflow: hidden;

      // 这里补一个浅色装饰圈
      &::after {
        content: '';
        position: absolute;
        width: 100%;
        height: 100%;
        background: radial-gradient(circle at center, rgba(255, 107, 129, 0.08), transparent 70%);
        opacity: 0;
        transition: opacity 0.3s;
      }
      
      // 图标尺寸交给 Element Plus 组件自己处理
    }

    .cat-icon-img {
      width: 28px;
      height: 28px;
      object-fit: contain;
    }

    .cat-name {
      font-size: 14px;
      color: #666; // Softer grey
      transition: color 0.3s;
    }
  }
}

.curated-section {
  padding: 0 20px;
  margin-bottom: 60px;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-bottom: 24px;
    
    h2 {
      font-size: 24px;
      font-weight: 600;
      color: #333;
      .subtitle {
        font-size: 16px;
        color: #999;
        font-weight: 400;
        margin-left: 8px;
      }
    }

    .view-all {
      font-size: 14px;
      color: #666;
      text-decoration: none;
      background: transparent;
      border: none;
      cursor: pointer;
      padding: 0;
      &:hover {
        color: #FF6B81;
      }
    }
  }
}

.horizontal-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  
  margin-left: -20px;
  margin-right: -20px;
  padding-left: 20px;
  padding-right: 20px;
  padding-bottom: 20px; // Extra padding for shadow
  
  scrollbar-width: none; 
  &::-webkit-scrollbar {
    display: none;
  }

  &.snap-scroll {
    scroll-snap-type: x mandatory;
    scroll-behavior: smooth;
  }
  
  .scroll-item {
    flex: 0 0 220px;
    min-width: 220px;
    
    &.snap-item {
      scroll-snap-align: start;
    }
  }
}

.grid-layout {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  
  @media (max-width: 992px) {
    grid-template-columns: repeat(3, 1fr);
  }
  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

// 加载更多区域
.load-more-area {
  text-align: center;
  padding: 32px 0;
  
  .loading-indicator {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: #999;
    font-size: 14px;
    
    .is-loading {
      animation: rotate 1s linear infinite;
    }
  }
  
  .no-more {
    color: #ccc;
    font-size: 13px;
  }
  
  @keyframes rotate {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
  }
}

.footer-spacer {
  height: 40px;
}
</style>
