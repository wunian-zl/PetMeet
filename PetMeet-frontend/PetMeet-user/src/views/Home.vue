<template>
  <div class="home-container">
  <!-- 吸顶头部 -->
    <div class="sticky-header">
       <div class="search-container">
          <el-input
            v-model="searchKeyword"
            class="search-input"
            :class="{ locked: searchLocked }"
            :placeholder="searchPlaceholder"
            clearable
            :readonly="searchLocked"
            @click="handleSearchClick"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon class="search-icon"><Search /></el-icon>
            </template>
          </el-input>
       </div>

       <div class="category-tabs">
          <div 
            v-for="cat in categories" 
            :key="cat.key" 
            class="tab-item"
            :class="{ active: activeCategory === cat.key }"
            @click="handleSwitch(cat.key)"
          >
             {{ cat.label }}
             <div class="active-dot" v-if="activeCategory === cat.key"></div>
          </div>
       </div>

       <div class="tag-tabs">
          <div
            v-for="tag in tagOptions"
            :key="tag.value"
            class="tag-item"
            :class="{ active: activeTag === tag.value }"
            @click="handleTagSwitch(tag.value)"
          >
            {{ tag.label }}
          </div>
       </div>
    </div>

      <!-- 瀑布流布局 -->
    <div class="waterfall-wrapper">
      <div class="masonry-container" v-loading="loading && noteList.length === 0">
         <div v-for="(col, colIndex) in waterfallCols" :key="colIndex" class="masonry-column">
            <div
              v-for="note in col"
              :key="note.id"
              class="note-card"
              :class="{ 'cover-ready': isNoteCardReady(note) }"
              @click="openDetail(note, $event)"
            >
            <!-- 封面图 -->
              <div class="card-cover" :style="{ '--cover-ratio': note.coverRatio || DEFAULT_COVER_RATIO }">
                <img
                  :src="note.coverImg"
                  :data-note-id="note.id"
                  alt="cover"
                  :loading="note.coverPriority ? 'eager' : 'lazy'"
                  :fetchpriority="note.coverPriority ? 'high' : 'auto'"
                  decoding="async"
                  @load="handleNoteCoverLoad($event, note)"
                  @error="handleNoteCoverError($event, note)"
                />
                <div v-if="!isNoteCardReady(note)" class="cover-placeholder" aria-hidden="true"></div>
                
            <!-- 电商标签 -->
                <div v-if="note.productCount && note.productCount > 0" class="shop-tag">
                  种草
                </div>
                <div v-if="note.type === 'video'" class="video-badge">
                  <el-icon><VideoPlay /></el-icon>
                </div>
              </div>

          <!-- 内容区 -->
              <div class="card-body">
                <div class="card-title">{{ note.title }}</div>
                
                <div class="card-footer">
                  <div class="author-info">
                    <el-avatar :size="20" :src="note.authorAvatar || ''" icon="UserFilled" class="author-avatar" />
                    <span class="nickname">{{ note.authorName || '用户' }}</span>
                  </div>
                  <div class="like-info">
                    <el-icon class="like-icon"><Star /></el-icon>
                    <span class="count">{{ note.likeCount || 0 }}</span>
                  </div>
                </div>
              </div>
            </div>
         </div>
      </div>

      <div v-if="noteList.length > 0" class="load-more-indicator" aria-live="polite">
        <template v-if="loadingMore">
          <el-icon class="spinner"><Loading /></el-icon>
          <span>正在加载更多内容...</span>
        </template>
        <template v-else-if="hasMore && isNearBottom">
          <el-icon class="spinner waiting"><Loading /></el-icon>
          <span>已到达底部，正在为你准备更多内容...</span>
        </template>
        <template v-else-if="hasMore">
          <el-icon class="status-icon"><ArrowDown /></el-icon>
          <span>继续下滑，加载更多内容</span>
        </template>
        <template v-else>
          <el-icon class="status-icon"><CircleCheck /></el-icon>
          <span>已经到底了</span>
        </template>
      </div>

      <div
        v-if="hasMore"
        ref="loadMoreSentinel"
        class="load-more-sentinel"
        aria-hidden="true"
      ></div>
      
  <!-- 空状态 -->
      <el-empty v-if="!loading && noteList.length === 0" description="暂无内容" />
    </div>
      


  <!-- 带过渡效果的详情弹层 -->
  <!-- 去掉外层过渡组件，让详情组件自己处理展开动画 -->
    <NoteDetail
      v-if="showDetail"
      :noteId="currentNoteId"
      :initialNote="currentNote"
      :originRect="originRect"
      @close="closeDetail"
    />

    <el-backtop :visibility-height="400" :right="24" :bottom="24" />
  </div>
</template>

<script>
export default {
  name: 'Home'
}
</script>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Star, UserFilled, Search, VideoPlay, Loading, ArrowDown, CircleCheck } from '@element-plus/icons-vue'
import NoteDetail from './NoteDetail.vue'
import request from '@/utils/request'
import { getImageUrl, getAvatarUrl } from '@/utils/image'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { STALE_REFRESH_MS, useStaleRefresh } from '@/utils/staleRefresh'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const noteList = ref([])
const waterfallCols = ref([[], [], [], []]) // Default 4 cols
const loading = ref(false)
const activeCategory = ref('all')
const activeTag = ref('')
const searchKeyword = ref('')
const searchLocked = computed(() => !userStore.isLoggedIn)
const searchPlaceholder = computed(() =>
  searchLocked.value ? '登录后可搜索更多笔记' : '搜索社区内容（如：狗粮 / 猫砂 / 绝育）'
)

// 分页
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const hasMore = ref(true)
const loadingMore = computed(() => loading.value && noteList.value.length > 0)
const isNearBottom = ref(false)
const loadMoreSentinel = ref(null)
let loadMoreObserver = null
let closeDetailTimer = null
let coverCompletionTimer = null
let lastSearchLoginPromptAt = 0
let searchLoginMessage = null

// 详情弹窗状态
const showDetail = ref(false)
const currentNoteId = ref(null)
const currentNote = ref(null)
const originRect = ref(null)

const categories = [
  { label: '全部', key: 'all' },
  { label: '推荐', key: 'recommend' },
  { label: '猫咪', key: 'cat' },
  { label: '狗狗', key: 'dog' },
  { label: '异宠', key: 'other' },
  { label: '科普', key: 'knowledge' }
]

const tagOptions = [
  { label: "全部", value: "" },
  { label: "喂养", value: "喂养" },
  { label: "训练", value: "训练" },
  { label: "洗护", value: "洗护" },
  { label: "健康", value: "健康" },
  { label: "日常", value: "日常" }
]

const DEFAULT_COVER_RATIO = '4 / 5'
const MIN_COVER_RATIO = 0.72
const MAX_COVER_RATIO = 1.35

const toCoverRatio = (width, height) => {
  const numericWidth = Number(width)
  const numericHeight = Number(height)
  if (!numericWidth || !numericHeight || numericWidth <= 0 || numericHeight <= 0) {
    return DEFAULT_COVER_RATIO
  }
  const ratio = Math.min(MAX_COVER_RATIO, Math.max(MIN_COVER_RATIO, numericWidth / numericHeight))
  return `${ratio.toFixed(3)} / 1`
}

const isNoteCardReady = (note) => Boolean(note?.coverLoaded || note?.coverFailed)

const normalizeComparableUrl = (src) => {
  if (!src) return ''
  try {
    return new URL(src, window.location.origin).href
  } catch (e) {
    return src
  }
}

// 简单节流一下，别让滚动事件打太满
const throttle = (func, delay) => {
  let lastCall = 0
  let timer = null
  let lastArgs = null
  let lastContext = null
  return function(...args) {
    const now = Date.now()
    const remaining = delay - (now - lastCall)
    lastArgs = args
    lastContext = this
    if (remaining <= 0) {
      if (timer) {
        clearTimeout(timer)
        timer = null
      }
      lastCall = now
      func.apply(lastContext, lastArgs)
    } else if (!timer) {
      timer = setTimeout(() => {
        timer = null
        lastCall = Date.now()
        func.apply(lastContext, lastArgs)
      }, remaining)
    }
  }
}

// 瀑布流布局
const calculateColCount = () => {
  const width = window.innerWidth
  if (width > 1400) return 5
  if (width > 1100) return 4
  if (width > 800) return 3
  return 2
}

const distributeNotes = (notes) => {
  // 这里按当前最短列分配，避免一列明显过长
  notes.forEach(note => {
      let targetColIndex = 0
      let minLength = Infinity
      
      // 找出当前元素最少的那一列
      waterfallCols.value.forEach((col, idx) => {
          if (col.length < minLength) {
              minLength = col.length
              targetColIndex = idx
          }
      })
      
      waterfallCols.value[targetColIndex].push(note)
  })
}

const resetWaterfall = (notes) => {
  const count = calculateColCount()
  waterfallCols.value = Array.from({ length: count }, () => [])
  distributeNotes(notes)
}

const buildNoteDedupKey = (note) => {
  const title = (note?.title || '').trim()
  const cover = (note?.coverImg || '').trim()
  return `${title}__${cover}`
}

const handleResize = throttle(() => {
  const newCount = calculateColCount()
  if (newCount !== waterfallCols.value.length) {
     resetWaterfall(noteList.value)
  }
}, 300)


// 获取笔记列表
const fetchNotes = async (append = false, options = {}) => {
  if (loading.value) return
  
  const { silent = false } = options
  let loadedCount = 0
  loading.value = true
  try {
    const isRecommendedScope = activeCategory.value === 'recommend'
    const category = activeCategory.value === 'all' || isRecommendedScope
      ? undefined
      : activeCategory.value
    const res = await request.get('/note/list', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        keyword: searchKeyword.value ? searchKeyword.value.trim() : undefined,
        category,
        tag: activeTag.value || undefined,
        recommended: isRecommendedScope ? true : undefined
      },
      silentError: silent
    })
    
    if (res) {
      const records = res.records || res || []
      total.value = res.total || records.length
      
      // 映射字段并处理图片 URL
      const baseIndex = append ? noteList.value.length : 0
      const priorityLimit = Math.max(8, calculateColCount() * 2)
      const mappedNotes = records.map((note, index) => ({
        ...note,
        coverImgRaw: note.coverImg || '',
        coverThumbRaw: note.coverThumb || '',
        coverImg: getImageUrl(note.coverThumb || note.coverImg),
        coverPriority: baseIndex + index < priorityLimit,
        coverRatio: toCoverRatio(note.coverWidth || note.imageWidth, note.coverHeight || note.imageHeight),
        coverLoaded: false,
        coverFailed: false,
        authorAvatar: getAvatarUrl(note.authorAvatar),
        authorName: note.authorNickname || '用户'
      }))
      let finalNotes = mappedNotes
      if (append) {
        const existingIds = new Set(noteList.value.map((item) => item?.id).filter(Boolean))
        const existingKeys = new Set(noteList.value.map(buildNoteDedupKey))
        finalNotes = mappedNotes.filter((item) => {
          if (!item?.id || existingIds.has(item.id)) return false
          const dedupKey = buildNoteDedupKey(item)
          if (existingKeys.has(dedupKey)) return false
          existingKeys.add(dedupKey)
          return true
        })
      }
      loadedCount = finalNotes.length
      
      if (append) {
        noteList.value.push(...finalNotes)
        distributeNotes(finalNotes)
      } else {
        const seenIds = new Set()
        const seenKeys = new Set()
        finalNotes = mappedNotes.filter((item) => {
          if (!item?.id || seenIds.has(item.id)) return false
          const dedupKey = buildNoteDedupKey(item)
          if (seenKeys.has(dedupKey)) return false
          seenIds.add(item.id)
          seenKeys.add(dedupKey)
          return true
        })
        noteList.value = finalNotes
        resetWaterfall(finalNotes)
      }
      scheduleCoverCompletionCheck()
      
      // 检查是否还有更多
      hasMore.value = noteList.value.length < total.value
    }
  } catch (error) {
    console.error('获取笔记失败', error)
  } finally {
    loading.value = false
    // 到底后无需再回滚触发：请求结束后再检查一次，仍在底部则继续加载下一页。
    if (loadedCount > 0 && hasMore.value) {
      nextTick(() => {
        updateNearBottomState()
        if (isNearBottom.value && !loading.value) {
          loadMore()
        }
      })
    }
  }
}

const syncQueryToUrl = () => {
  const q = searchKeyword.value ? searchKeyword.value.trim() : ''
  router.replace({
    path: '/',
    query: q ? { q } : {}
  })
}

const blurSearchInput = (event) => {
  const target = event?.target
  const input = target?.closest?.('.search-input')?.querySelector?.('input') || target
  input?.blur?.()
}

const closeSearchLoginMessage = () => {
  searchLoginMessage?.close?.()
  searchLoginMessage = null
}

const promptSearchLogin = (event, type = 'warning') => {
  if (searchLocked.value) {
    const now = Date.now()
    if (now - lastSearchLoginPromptAt > 800) {
      closeSearchLoginMessage()
      searchLoginMessage = ElMessage({
        type,
        message: '请登录后搜索更多笔记',
        grouping: true,
        duration: 1200,
        onClose: () => {
          searchLoginMessage = null
        }
      })
      lastSearchLoginPromptAt = now
    }
    blurSearchInput(event)
    userStore.showLogin()
    return true
  }
  return false
}

const handleSearch = (event) => {
  if (promptSearchLogin(event, 'warning')) return
  pageNum.value = 1
  syncQueryToUrl()
  fetchNotes(false)
}

const handleSearchClick = (event) => {
  promptSearchLogin(event, 'info')
}

const handleSwitch = (key) => {
  activeCategory.value = key
  pageNum.value = 1
  fetchNotes(false)
}

const handleTagSwitch = (value) => {
  activeTag.value = value
  pageNum.value = 1
  fetchNotes(false)
}


// 加载更多
const loadMore = () => {
  if (!hasMore.value || loading.value) return
  pageNum.value++
  fetchNotes(true)
}

const updateNearBottomState = () => {
  const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
  const scrollHeight = document.documentElement.scrollHeight
  const clientHeight = document.documentElement.clientHeight
  isNearBottom.value = scrollTop + clientHeight >= scrollHeight - 140
}

// 滚动加载 - 使用节流优化性能
const handleScrollRaw = () => {
  const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
  const scrollHeight = document.documentElement.scrollHeight
  const clientHeight = document.documentElement.clientHeight
  isNearBottom.value = scrollTop + clientHeight >= scrollHeight - 140
  
  if (scrollTop + clientHeight >= scrollHeight - 300) {
    loadMore()
  }
}

// 节流处理 - 每200ms最多触发一次
const handleScroll = throttle(handleScrollRaw, 200)

const setupLoadMoreObserver = () => {
  if (loadMoreObserver) {
    loadMoreObserver.disconnect()
    loadMoreObserver = null
  }
  if (!loadMoreSentinel.value) return

  loadMoreObserver = new IntersectionObserver(
    (entries) => {
      const hit = entries.some((entry) => entry.isIntersecting)
      if (hit) {
        loadMore()
      }
    },
    {
      root: null,
      rootMargin: '360px 0px 360px 0px',
      threshold: 0
    }
  )
  loadMoreObserver.observe(loadMoreSentinel.value)
}

const findNoteById = (id) => {
  if (!id) return null
  return noteList.value.find((item) => String(item?.id) === String(id)) || null
}

const markCompletedCoverImage = (img) => {
  if (!img || !img.complete) return
  const note = findNoteById(img.dataset.noteId)
  if (!note || note.coverLoaded || note.coverFailed) return
  if (img.naturalWidth > 0) {
    handleNoteCoverLoad({ target: img }, note)
  } else {
    handleNoteCoverError({ target: img }, note)
  }
}

const scanCompletedCoverImages = () => {
  document
    .querySelectorAll('.note-card .card-cover img[data-note-id]')
    .forEach(markCompletedCoverImage)
}

const scheduleCoverCompletionCheck = () => {
  if (coverCompletionTimer) {
    clearTimeout(coverCompletionTimer)
    coverCompletionTimer = null
  }
  nextTick(() => {
    scanCompletedCoverImages()
    coverCompletionTimer = setTimeout(scanCompletedCoverImages, 800)
  })
}

const handleNoteCoverError = (event, note) => {
  const target = event?.target
  if (!target) return
  const fallback = note?.coverImgRaw ? getImageUrl(note.coverImgRaw) : ''
  const currentSrc = normalizeComparableUrl(target.currentSrc || target.src)
  if (fallback && currentSrc !== normalizeComparableUrl(fallback)) {
    target.src = fallback
    return
  }
  const placeholder = getImageUrl('')
  if (currentSrc !== normalizeComparableUrl(placeholder)) {
    target.src = placeholder
    return
  }
  note.coverFailed = true
  note.coverLoaded = true
}

const handleNoteCoverLoad = (event, note) => {
  const target = event?.target
  if (!target || !note) return
  note.coverRatio = toCoverRatio(target.naturalWidth, target.naturalHeight)
  note.coverLoaded = true
  note.coverFailed = false
}

// 打开详情弹窗，并把地址推到历史栈里
const openDetail = (note, event) => {
  const id = note?.id
  if (!id) return
  if (closeDetailTimer) {
    clearTimeout(closeDetailTimer)
    closeDetailTimer = null
  }
  
  // 记录卡片起始位置，给转场动画用
  let rectSource = null
  if (event && event.currentTarget) {
    let target = event.currentTarget
    if (!target.classList.contains('note-card')) {
      target = target.closest('.note-card') || target
    }
    rectSource = target.querySelector?.('.card-cover') || target
  }

  if (rectSource) {
    const rect = rectSource.getBoundingClientRect()
    originRect.value = {
      top: rect.top,
      left: rect.left,
      width: rect.width,
      height: rect.height,
      radius: 16
    }
  } else {
    originRect.value = null
  }

  currentNoteId.value = id
  currentNote.value = note || null

  showDetail.value = true
  
  const q = searchKeyword.value ? searchKeyword.value.trim() : ''
  const detailHref = router.resolve({
    name: 'NoteDetail',
    params: { id },
    query: q ? { q } : {}
  }).href
  window.history.pushState({ noteId: id }, '', detailHref)
}

// 关闭详情弹窗，并把地址恢复回列表页
const closeDetail = () => {
  showDetail.value = false
  if (closeDetailTimer) {
    clearTimeout(closeDetailTimer)
  }
  closeDetailTimer = setTimeout(() => {
    if (!showDetail.value) {
      currentNoteId.value = null
      currentNote.value = null
    }
    closeDetailTimer = null
  }, 300)
  const q = searchKeyword.value ? searchKeyword.value.trim() : ''
  const homeHref = router.resolve({
    path: '/',
    query: q ? { q } : {}
  }).href
  window.history.pushState({}, '', homeHref)
}

// 处理浏览器后退
const handlePopState = () => {
   if (showDetail.value) {
      showDetail.value = false
      currentNoteId.value = null
      currentNote.value = null
   }
}

const restoreWindowScroll = async (scrollTop) => {
  await nextTick()
  const maxScrollTop = Math.max(0, document.documentElement.scrollHeight - window.innerHeight)
  window.scrollTo(0, Math.min(scrollTop, maxScrollTop))
}

const refreshCurrentHomeNotes = async () => {
  const scrollTop = window.scrollY || document.documentElement.scrollTop || document.body.scrollTop || 0
  const originalPageNum = pageNum.value
  const originalPageSize = pageSize.value
  const refreshSize = Math.max(originalPageSize, noteList.value.length || originalPageSize)

  pageNum.value = 1
  pageSize.value = refreshSize
  try {
    await fetchNotes(false, { silent: true })
  } finally {
    pageSize.value = originalPageSize
    pageNum.value = Math.max(1, Math.ceil((noteList.value.length || 1) / originalPageSize))
    await restoreWindowScroll(scrollTop)
  }
}

const homeStaleRefresh = useStaleRefresh({
  staleMs: STALE_REFRESH_MS.community,
  refresh: refreshCurrentHomeNotes,
  isRefreshing: () => loading.value,
  shouldSkip: () => showDetail.value
})

onMounted(() => {
  const initQ = typeof route.query.q === 'string' ? route.query.q : ''
  if (initQ) {
    searchKeyword.value = initQ
  }
  // 先把瀑布流列初始化出来
  waterfallCols.value = Array.from({ length: calculateColCount() }, () => [])
  
  fetchNotes(false)
  homeStaleRefresh.markFresh()
  // 使用 passive 选项优化滚动性能
  window.addEventListener('petmeet:login-modal-closing', closeSearchLoginMessage)
  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('popstate', handlePopState)
  window.addEventListener('resize', handleResize)
  nextTick(() => {
    updateNearBottomState()
    setupLoadMoreObserver()
  })
})

onUnmounted(() => {
  if (closeDetailTimer) {
    clearTimeout(closeDetailTimer)
    closeDetailTimer = null
  }
  if (coverCompletionTimer) {
    clearTimeout(coverCompletionTimer)
    coverCompletionTimer = null
  }
  closeSearchLoginMessage()
  window.removeEventListener('petmeet:login-modal-closing', closeSearchLoginMessage)
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('popstate', handlePopState)
  window.removeEventListener('resize', handleResize)
  if (loadMoreObserver) {
    loadMoreObserver.disconnect()
    loadMoreObserver = null
  }
})

watch(
  () => route.query.q,
  (newQ) => {
    const q = typeof newQ === 'string' ? newQ : ''
    if (q !== (searchKeyword.value || '')) {
      searchKeyword.value = q
      pageNum.value = 1
      fetchNotes(false)
    }
  }
)

watch(
  () => userStore.loginVisible,
  (visible) => {
    if (!visible) {
      closeSearchLoginMessage()
    }
  }
)

watch(
  () => hasMore.value,
  async () => {
    await nextTick()
    setupLoadMoreObserver()
    updateNearBottomState()
  }
)
</script>

<style scoped lang="scss">
.home-container {
  height: 100%;
}

// 头部、搜索区和瀑布流样式
.sticky-header {
  position: sticky;
  top: 0;
  z-index: 99;
  background: #ffffff;
  backdrop-filter: blur(10px);
  padding: 10px 40px;
  border-bottom: 2px solid transparent; 
}

.search-container {
  display: flex;
  justify-content: center;
  margin-bottom: 15px;
  
  .search-input {
    width: 320px;
    height: 44px;
    background: #fff; 
    border: 2px solid #f0f0f0; // Default light gray
    border-radius: 99px; 
    color: #666; 
    font-size: 14px;
    font-weight: 500;
    transition: all 0.3s ease;
    
    &:hover {
      border-color: #FF6B81; // Pink border on hover
      box-shadow: 0 4px 12px rgba(255, 107, 129, 0.1); 
      color: #333;
      .search-icon { color: #FF6B81; }
    }
    
    .search-icon {
      color: #999;
    }
  }

  .search-input.locked {
    cursor: not-allowed;
    opacity: 0.7;
  }
}

/* 去掉 Element Plus 输入框默认边框和阴影，复用外层胶囊样式 */
:deep(.search-input .el-input__wrapper) {
  box-shadow: none !important;
  background: transparent !important;
  border: none !important;
  padding: 0 14px;
}

:deep(.search-input .el-input__inner) {
  height: 44px;
  line-height: 44px;
}

.category-tabs {
  display: flex;
  justify-content: center;
  gap: clamp(16px, 4vw, 25px);
  padding-bottom: 5px;
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
  
  .tab-item {
    position: relative;
    flex: 0 0 auto;
    padding: 6px 4px;
    color: #888;
    font-size: 16px;
    cursor: pointer;
    transition: all 0.3s ease;
    
    &:hover {
      color: #FF6B81; 
    }
    
    &.active {
      color: #FF6B81; 
      font-weight: 700;
      font-size: 17px;
    }
    
    .active-dot {
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 6px;
      height: 6px;
      background: #FF6B81; 
      border-radius: 50%;
    }
  }
}

.tag-tabs {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 6px 0 12px;

  .tag-item {
    padding: 4px 12px;
    border-radius: 999px;
    background: #f7f7f7;
    color: #666;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      background: #ffe9ed;
      color: #ff6b81;
    }

    &.active {
      background: #ff6b81;
      color: #fff;
      font-weight: 600;
    }
  }
}

@media (max-width: 720px) {
  .sticky-header {
    padding: 8px 16px;
  }

  .category-tabs {
    justify-content: flex-start;
    gap: 22px;
    padding: 0 2px 6px;

    .tab-item {
      font-size: 15px;

      &.active {
        font-size: 16px;
      }
    }
  }

  .tag-tabs {
    justify-content: flex-start;
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 10px;
    scrollbar-width: none;

    &::-webkit-scrollbar {
      display: none;
    }

    .tag-item {
      flex: 0 0 auto;
    }
  }

}

.waterfall-wrapper {
  padding: 20px 40px 40px;
  max-width: 1600px;
  margin: 0 auto;
}

.load-more-indicator {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #8c8c8c;
  font-size: 14px;

  .spinner {
    font-size: 16px;
    color: #ff6b81;
    animation: spin 0.8s linear infinite;
  }

  .spinner.waiting {
    color: #f39aa9;
    animation-duration: 1.2s;
  }

  .status-icon {
    font-size: 16px;
    color: #b0b0b0;
  }
}

.load-more-sentinel {
  height: 1px;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.masonry-container {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  justify-content: center;
}

.masonry-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-width: 0; /* Prevent flex overflow */
}

.note-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  transform: translateZ(0); /* Force GPU layer */
  will-change: transform;
  /* 去掉 content-visibility: auto，避免滚动时跳动 */
  contain: layout paint; /* Optimized contain */
  
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);

  &.cover-ready {
    .card-cover img {
      opacity: 1;
    }

    .card-title,
    .card-footer {
      animation: noteContentIn 0.18s ease both;
    }
  }

  &:not(.cover-ready) {
    .card-title,
    .card-footer {
      visibility: hidden;
    }

    .card-body::before,
    .card-body::after {
      content: '';
      position: absolute;
      left: 14px;
      border-radius: 999px;
      background: #eceae4;
    }

    .card-body::before {
      top: 16px;
      width: 72%;
      height: 14px;
    }

    .card-body::after {
      bottom: 18px;
      width: 52%;
      height: 12px;
    }
  }
  
  &:hover {
    transform: translateY(-4px) translateZ(0);
    box-shadow: 0 8px 16px rgba(0,0,0,0.08);
    
    .card-cover img {
      transform: scale(1.05);
    }
  }

  .card-cover {
    position: relative;
    width: 100%;
    aspect-ratio: var(--cover-ratio, 4 / 5);
    overflow: hidden;
    background: #f2f1ee;
    
    img {
      width: 100%;
      height: 100%;
      display: block;
      object-fit: cover;
      opacity: 0;
      transition: opacity 0.2s ease, transform 0.3s ease;
      transform: translateZ(0);
    }

    .cover-placeholder {
      position: absolute;
      inset: 0;
      background: #f2f1ee;
      pointer-events: none;
      z-index: 1;

      &::before {
        content: '';
        position: absolute;
        left: 50%;
        top: 50%;
        width: 42px;
        height: 42px;
        transform: translate(-50%, -50%);
        border: 2px solid #d5d2ca;
        border-radius: 16px;
        opacity: 0.65;
      }

      &::after {
        content: '';
        position: absolute;
        left: 50%;
        top: 50%;
        width: 16px;
        height: 16px;
        transform: translate(-50%, -46%) rotate(45deg);
        border-right: 2px solid #d5d2ca;
        border-bottom: 2px solid #d5d2ca;
        opacity: 0.65;
      }
    }
    
    .shop-tag {
      position: absolute;
      top: 10px;
      left: 10px;
      background: rgba(0, 0, 0, 0.5);
      padding: 4px 10px;
      border-radius: 99px;
      font-size: 11px;
      color: #fff;
      font-weight: 600;
      backdrop-filter: blur(4px);
    }

    .video-badge {
      position: absolute;
      top: 10px;
      right: 10px;
      width: 26px;
      height: 26px;
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
      font-size: 16px;
    }
  }

  .card-body {
    position: relative;
    padding: 12px 14px 16px;
  }

  .card-title {
    font-size: 15px;
    font-weight: 600;
    color: #333;
    line-height: 1.5;
    margin-bottom: 12px;
    
    display: -webkit-box;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    
    .author-info {
      display: flex;
      align-items: center;
      gap: 8px;
      width: 70%;
      
      .author-avatar {
        flex-shrink: 0;
        border: 1px solid #f9f9f9;
      }
      
      .nickname {
        font-size: 12px;
        color: #999;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .like-info {
      display: flex;
      align-items: center;
      gap: 3px;
      color: #ccc;
      font-size: 12px;
      
      .like-icon {
        cursor: pointer;
        transition: color 0.2s;
        &:hover { color: #FF6B81; } // Pink Heart Hover
      }
    }
  }
}

@keyframes noteContentIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 720px) {
  .waterfall-wrapper {
    padding: 14px 12px 32px;
  }

  .masonry-container {
    gap: 12px;
  }

  .masonry-column {
    gap: 12px;
  }
}

</style>
