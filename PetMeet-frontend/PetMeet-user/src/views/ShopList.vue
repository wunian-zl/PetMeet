<template>
  <div class="shop-list-container">
  <!-- 面包屑 -->
    <div class="breadcrumb-area">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/shop' }">商城</el-breadcrumb-item>
        <el-breadcrumb-item>{{ categoryName }}</el-breadcrumb-item>
      </el-breadcrumb>
      <el-tag v-if="effectiveRecentDays" class="recent-days-tag" size="small" type="warning" effect="plain">
        近{{ effectiveRecentDays }}天上新
      </el-tag>
    </div>

  <!-- 筛选与排序栏 -->
    <div class="filter-bar">
      <div class="filters">
        <el-dropdown trigger="click" @command="handlePriceCommand">
          <span class="filter-item">
            价格区间 <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="all">全部</el-dropdown-item>
              <el-dropdown-item command="0-50">0-50元</el-dropdown-item>
              <el-dropdown-item command="50-200">50-200元</el-dropdown-item>
              <el-dropdown-item command="200+">200元以上</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <div class="sorts">
        <span 
          class="sort-item" 
          :class="{ active: currentSort === 'default' }"
          @click="currentSort = 'default'"
        >综合</span>
        <span 
          class="sort-item" 
          :class="{ active: currentSort === 'sales' }"
          @click="currentSort = 'sales'"
        >销量</span>
        <span 
          class="sort-item" 
          :class="{ active: currentSort === 'price' }"
          @click="togglePriceSort"
        >
          价格 
          <el-icon v-if="currentSort === 'price' && priceOrder === 'asc'"><Top /></el-icon>
          <el-icon v-else-if="currentSort === 'price' && priceOrder === 'desc'"><Bottom /></el-icon>
        </span>
      </div>
    </div>

  <!-- 商品网格 -->
    <div class="product-list" v-infinite-scroll="loadMore" :infinite-scroll-disabled="disabled">
      <ShopProductCard 
        v-for="product in displayList" 
        :key="product.id" 
        :product="product" 
      />
    </div>
    
    <div v-if="loading" class="loading-text">加载中...</div>
    <div v-if="noMore" class="no-more-text">没有更多了</div>

    <el-backtop :visibility-height="400" :right="24" :bottom="24" />
  </div>
</template>

<script>
export default {
  name: 'ShopList'
}
</script>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Top, Bottom } from '@element-plus/icons-vue'
import ShopProductCard from '@/components/ShopProductCard.vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const categoryIds = computed(() => {
  const idsRaw = route.query.categoryIds
  if (typeof idsRaw === 'string' && idsRaw.trim()) {
    return idsRaw
      .split(',')
      .map((v) => Number(v))
      .filter((v) => Number.isFinite(v) && v > 0)
  }
  const single = route.query.categoryId
  const n = single == null ? null : Number(single)
  return Number.isFinite(n) && n > 0 ? [n] : []
})
const keyword = computed(() => route.query.keyword || '')
const categoryName = computed(() => route.query.category || '全部商品')

const isExpired = (untilStr) => {
  if (!untilStr || typeof untilStr !== 'string') return false
  const d = new Date(`${untilStr}T23:59:59`)
  if (Number.isNaN(d.getTime())) return false
  return Date.now() > d.getTime()
}

const effectiveRecentDays = computed(() => {
  const until = route.query.recentDaysUntil
  if (typeof until === 'string' && until && isExpired(until)) {
    return null
  }
  const v = route.query.recentDays
  const n = v == null ? null : Number(v)
  return Number.isFinite(n) && n > 0 ? n : null
})

// 筛选状态
const currentPriceRange = ref('all')

// 排序状态
const currentSort = ref('default')
const priceOrder = ref('desc')

const togglePriceSort = () => {
  if (currentSort.value === 'price') {
    priceOrder.value = priceOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    currentSort.value = 'price'
    priceOrder.value = 'asc'
  }
}

const handlePriceCommand = (command) => {
  currentPriceRange.value = command
}

// 分页状态
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const loading = ref(false)
const hasLoaded = ref(false)
const productList = ref([])

// 计算属性
const noMore = computed(() => hasLoaded.value && productList.value.length >= total.value)
const disabled = computed(() => !hasLoaded.value || loading.value || noMore.value)

const matchPriceRange = (product, range) => {
  if (range === 'all') return true
  const price = Number(product.price) || 0
  if (range === '0-50') return price >= 0 && price <= 50
  if (range === '50-200') return price > 50 && price <= 200
  if (range === '200+') return price > 200
  return true
}

const displayList = computed(() => {
  const filtered = productList.value.filter((item) => {
    return matchPriceRange(item, currentPriceRange.value)
  })

  if (currentSort.value === 'sales') {
    return [...filtered].sort((a, b) => (b.sales || 0) - (a.sales || 0))
  }

  if (currentSort.value === 'price') {
    const factor = priceOrder.value === 'asc' ? 1 : -1
    return [...filtered].sort((a, b) => (Number(a.price) - Number(b.price)) * factor)
  }

  return filtered
})

// 从后端拉商品列表
const fetchProducts = async (append = false) => {
  if (loading.value) return
  
  loading.value = true
  if (!append) {
    hasLoaded.value = false
  }
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    
    // 可选筛选参数
    if (keyword.value) {
      params.keyword = keyword.value
    }
    if (categoryIds.value.length > 0) {
      params.categoryIds = categoryIds.value.join(',')
    }
    if (effectiveRecentDays.value) {
      params.recentDays = effectiveRecentDays.value
    }
    
    const res = await request.get('/product/list', { params })
    
    if (res) {
      // 后端返回 Page 对象
      const records = res.records || []
      total.value = res.total || 0
      
      // 将后端字段映射为前端需要的格式
      const mappedProducts = records.map(item => ({
        id: item.id,
        name: item.name,
        price: item.price,
        sales: item.sales || 0,
        image: item.coverImg || 'https://picsum.photos/id/237/300/375'
      }))
      
      if (append) {
        productList.value.push(...mappedProducts)
      } else {
        productList.value = mappedProducts
      }
    }
  } catch (error) {
    console.error('获取商品列表失败', error)
  } finally {
    loading.value = false
    hasLoaded.value = true
  }
}

// 加载更多（无限滚动）
const loadMore = () => {
  if (!hasLoaded.value || noMore.value || loading.value) return
  pageNum.value++
  fetchProducts(true)
}

// 路由参数变化后，重新拉取商品
watch([categoryIds, keyword, effectiveRecentDays], () => {
  pageNum.value = 1
  hasLoaded.value = false
  fetchProducts(false)
})

onMounted(() => {
  // 如果“近N天”已过期，则自动去掉筛选（体验上相当于自动变成“不限制”）
  const until = route.query.recentDaysUntil
  if (typeof until === 'string' && until && isExpired(until) && route.query.recentDays) {
    const nextQuery = { ...route.query }
    delete nextQuery.recentDays
    delete nextQuery.recentDaysUntil
    router.replace({ query: nextQuery })
  }
  fetchProducts(false)

  // 切回页面时自动刷新一次（方案一）
  const refreshIfVisible = () => {
    if (document.visibilityState === 'visible') {
      pageNum.value = 1
      fetchProducts(false)
    }
  }
  const handleVisibilityChange = () => refreshIfVisible()
  const handleWindowFocus = () => refreshIfVisible()

  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('focus', handleWindowFocus)

  onUnmounted(() => {
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    window.removeEventListener('focus', handleWindowFocus)
  })
})
</script>

<style scoped lang="scss">
.shop-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.breadcrumb-area {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.recent-days-tag {
  margin-left: 4px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;

  .filters {
    display: flex;
    gap: 24px;
    
    .filter-item {
      cursor: pointer;
      display: flex;
      align-items: center;
      font-size: 14px;
      color: #333;
      gap: 4px;
      
      &:hover {
        color: #FF6B81;
      }
    }
  }

  .sorts {
    display: flex;
    gap: 30px;
    
    .sort-item {
      cursor: pointer;
      font-size: 14px;
      color: #666;
      display: flex;
      align-items: center;
      gap: 2px;
      
      &.active {
        color: #FF6B81;
        font-weight: 600;
      }
      
      &:hover {
        color: #FF6B81;
      }
    }
  }
}

.product-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  margin-bottom: 40px;
  
  @media (max-width: 992px) {
    grid-template-columns: repeat(3, 1fr);
  }
  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.loading-text, .no-more-text {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 14px;
}
</style>
