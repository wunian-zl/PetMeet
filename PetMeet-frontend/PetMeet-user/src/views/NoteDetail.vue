<template>
  <div class="modal-overlay" :class="{ show: overlayVisible }" @click.self="handleClose">
  <!-- 关闭按钮 -->
    <div class="close-btn" @click="handleClose">
      <el-icon><Close /></el-icon>
    </div>

  <!-- 主弹层内容 -->
    <div
      ref="containerRef"
      class="detail-container"
      :class="{ 'is-opening': isOpening, 'is-closing': closing }"
    >
    <!-- 左侧：图片轮播 -->
      <div class="left-section">
      <!-- 顶层交互组件 -->
        <div v-if="note" class="interactive-layer" style="position: relative; width:100%; height:100%; z-index:2;">
          <template v-if="note.type === 'video'">
            <div class="carousel-img-wrapper">
              <video
                v-if="videoSrc"
                class="carousel-video"
                :src="videoSrc"
                :poster="displayCover"
                preload="metadata"
                controls
                playsinline
              />
              <img
                v-else-if="displayCover"
                :src="displayCover"
                :class="['carousel-img', { 'carousel-img--landscape': isLandscapeMedia(displayCover) }]"
                alt="cover"
                decoding="async"
                @load="(e) => handleMediaLoad(e, displayCover)"
              />
              <div v-else class="media-placeholder">
                <el-icon><VideoPlay /></el-icon>
                <span>视频加载中</span>
              </div>
            </div>
          </template>
          <template v-else>
            <el-carousel
              v-if="hasImages || displayCover"
              :autoplay="false"
              :arrow="hasMultipleImages ? 'hover' : 'never'"
              indicator-position="outside"
              height="100%"
              class="image-carousel"
            >
              <template v-if="hasImages">
                <el-carousel-item v-for="(img, index) in note.images" :key="index">
                  <div class="carousel-img-wrapper">
                    <img
                      :src="img"
                      :class="['carousel-img', { 'carousel-img--landscape': isLandscapeMedia(img) }]"
                      alt="note-img"
                      decoding="async"
                      loading="lazy"
                      @load="(e) => handleMediaLoad(e, img)"
                    />
                  </div>
                </el-carousel-item>
              </template>
              <template v-else-if="displayCover">
                <el-carousel-item>
                  <div class="carousel-img-wrapper">
                    <img
                      :src="displayCover"
                      :class="['carousel-img', { 'carousel-img--landscape': isLandscapeMedia(displayCover) }]"
                      alt="cover"
                      decoding="async"
                      @load="(e) => handleMediaLoad(e, displayCover)"
                    />
                  </div>
                </el-carousel-item>
              </template>
            </el-carousel>
            <div v-else class="media-placeholder">
              <span>暂无图片</span>
            </div>
          </template>
        </div>
      </div>

    <!-- 右侧：内容与互动区 -->
       <div class="right-section">
      <!-- 顶部：作者信息 -->
        <div class="detail-header">
          <div class="author-info">
            <el-avatar :size="40" :src="note?.authorAvatar" icon="UserFilled" />
            <span class="nickname">{{ note?.authorName || '用户' }}</span>
          </div>

          <button
            v-if="showFollowBtn"
            class="follow-btn"
            :class="{ active: isFollowing }"
            :disabled="followLoading"
            @click="handleFollow"
          >
            {{ isFollowing ? '已关注' : '关注' }}
          </button>
        </div>

      <!-- 可滚动内容区 -->
        <div class="detail-scroll-content">
          <h2 class="note-title">{{ note?.title }}</h2>
          <div v-if="note?.status !== 1" class="note-status">
            <el-tag :type="getNoteStatusType(note?.status)">{{ noteStatusText }}</el-tag>
            <span v-if="note?.status === 0" class="note-status-hint">审核中，暂不对其他用户展示</span>
          </div>
          <div v-if="note?.tags && note.tags.length > 0" class="note-tags">
            <span v-for="tag in note.tags" :key="tag" class="tag-chip">#{{ tag }}</span>
          </div>
          <p class="note-desc">{{ note?.content || '暂无描述...' }}</p>

          <p class="publish-time">{{ formatTime(note?.createTime) }}</p>

        <!-- 商品胶囊卡 -->
          <div v-if="note?.products && note.products.length > 0" class="product-capsule-group">
            <div
              v-for="prod in note.products"
              :key="prod.id"
              class="product-capsule"
              @click="goToProduct(prod.id)"
            >
              <div class="capsule-left">
                <img :src="prod.coverImg" class="thumb" alt="p" loading="lazy" decoding="async" />
                <span class="prefix">文中同款：</span>
                <span class="name">{{ prod.name }}</span>
                <span class="price">¥{{ prod.price }}</span>
              </div>
              <div class="capsule-right">
                Go <el-icon><ArrowRight /></el-icon>
              </div>
            </div>
          </div>

          <el-divider />

        <!-- 评论区 -->
          <div class="comments-section">
            <div class="comments-count">评论 {{ commentCount }} 条</div>

            <div v-if="commentLoading && comments.length === 0" class="comments-loading">
              <el-skeleton :rows="3" animated />
            </div>

            <div v-else-if="comments.length === 0" class="comments-empty">
              <el-empty description="暂无评论，快来抢沙发" />
            </div>

            <div v-else class="comment-list">
              <div v-for="comment in comments" :key="comment.id" class="comment-item">
                <el-avatar :size="32" :src="comment.userAvatar" icon="UserFilled" class="comment-avatar" />
                <div class="comment-content">
                  <div class="comment-user">
                    <span>{{ comment.userNickname }}</span>
                    <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                  </div>
                  <div class="comment-text">{{ comment.content }}</div>
                </div>
                <el-button
                  v-if="comment.mine"
                  type="danger"
                  link
                  size="small"
                  @click="handleDeleteComment(comment)"
                >
                  删除
                </el-button>
              </div>
            </div>

            <div class="comment-load-more" v-if="commentHasMore">
              <el-button :loading="commentLoading" @click="loadMoreComments">加载更多</el-button>
            </div>
          </div>
        </div>

      <!-- 底部操作区 -->
        <div class="detail-footer">
          <div class="action-input-wrapper">
            <input
              type="text"
              v-model="commentText"
              placeholder="说点什么..."
              class="comment-input"
              maxlength="200"
              :disabled="!canInteract || !activeNoteId"
              @keyup.enter="handleAddComment"
            />
            <el-button class="comment-send" type="primary" :disabled="!canInteract || !activeNoteId" @click="handleAddComment">发送</el-button>
          </div>
          <div class="action-icons">
        <!-- 点赞 -->
            <div class="action-item" :class="{ bounce: likeBounce }" @click="handleLike">
              <el-icon :class="{ starred: note?.liked }">
                <HeartIcon :filled="note?.liked" />
              </el-icon>
              <span>{{ note?.likeCount || 0 }}</span>
            </div>
        <!-- 收藏 -->
            <div class="action-item" @click="handleCollect">
              <el-icon :class="{ starred: note?.collected }">
                <StarFilled v-if="note?.collected" />
                <Star v-else />
              </el-icon>
              <span>{{ note?.collectCount || 0 }}</span>
            </div>
        <!-- 投诉 -->
            <div class="action-item" @click="handleComplaint">
              <el-icon><Warning /></el-icon>
              <span>投诉</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="complaintDialogVisible" title="投诉笔记" width="420px">
      <el-form>
        <el-form-item label="原因">
          <el-radio-group v-model="complaintForm.reason">
            <el-radio v-for="r in complaintReasons" :key="r" :label="r">{{ r }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="complaintForm.content" type="textarea" :rows="3" placeholder="补充说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="complaintDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="complaintSubmitting" @click="submitComplaint">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick, computed, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { formatTime } from '@/utils/format'
import { ElMessage } from 'element-plus'
import { Close, UserFilled, Star, StarFilled, ArrowRight, Warning, VideoPlay } from '@element-plus/icons-vue'
import HeartIcon from '@/components/HeartIcon.vue'
import request from '@/utils/request'
import { submitComplaint as submitComplaintApi } from '@/api/complaint'
import { getImageUrl, getAvatarUrl } from '@/utils/image'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 组件入参
const props = defineProps({
  noteId: {
    type: [String, Number],
    default: null
  },
  initialNote: {
    type: Object,
    default: null
  },
  originRect: {
    type: Object,
    default: null
  }
})

// 组件事件
const emit = defineEmits(['close'])

// 有 initialNote 时先秒开，细节数据再补
const note = ref(props.initialNote ? { ...props.initialNote } : null)
const pendingNote = ref(null) // { note: object, id: string|number } applied after opening animation
const loading = ref(!props.initialNote)
const commentText = ref('')
const containerRef = ref(null)
const overlayVisible = ref(false)
const opening = ref(true)
const closing = ref(false)
const likeBusy = ref(false)
const collectBusy = ref(false)
const likeBounce = ref(false)
const followLoading = ref(false)
const isFollowing = ref(false)
const comments = ref([])
const complaintDialogVisible = ref(false)
const complaintSubmitting = ref(false)
const complaintForm = reactive({
  reason: '',
  content: ''
})
const complaintReasons = ['侵权', '盗用', '其他']
const commentPage = ref(1)
const commentSize = 10
const commentTotal = ref(0)
const commentLoading = ref(false)
const videoSrc = ref('')
let videoTimer = null
let previousBodyOverflow = ''
let previousBodyPaddingRight = ''
const landscapeMediaMap = reactive({})

const OPEN_DURATION = 750
const CLOSE_DURATION = 300
const EASING_OUT = 'cubic-bezier(0.16, 1, 0.3, 1)'
const EASING_IN = 'cubic-bezier(0.4, 0, 1, 1)'
const FINAL_RADIUS = 20

const commentHasMore = computed(() => comments.value.length < commentTotal.value)
const commentCount = computed(() => Math.max(Number(note.value?.commentCount || 0), commentTotal.value))
const getNoteStatusText = (status) => {
  const map = { 0: '审核中', 1: '已发布', 2: '已屏蔽', 3: '已拒绝' }
  return map[status] || '未知'
}

const getNoteStatusType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }
  return map[status] || 'info'
}

const canInteract = computed(() => note.value?.status === 1)
const noteStatusText = computed(() => note.value?.statusDesc || getNoteStatusText(note.value?.status))
const showFollowBtn = computed(() => {
  const myId = userStore.userInfo?.id
  const authorId = note.value?.userId
  return !!authorId && (!myId || authorId !== myId)
})
const normalizeNoteId = (value) => {
  const id = Number(value)
  if (!Number.isInteger(id) || id <= 0) return null
  return id
}

const activeNoteId = computed(() => {
  const byProp = normalizeNoteId(props.noteId)
  if (byProp) return byProp
  const byRoute = normalizeNoteId(route.params.id)
  if (byRoute) return byRoute
  return normalizeNoteId(note.value?.id)
})
const hasImages = computed(() => Array.isArray(note.value?.images) && note.value.images.length > 0)
const hasMultipleImages = computed(() => Array.isArray(note.value?.images) && note.value.images.length > 1)
const displayCover = computed(() => {
  if (note.value?.coverImg) return note.value.coverImg
  if (hasImages.value) return note.value.images[0]
  if (props.initialNote?.coverImg) return props.initialNote.coverImg
  return ''
})

const isOpening = computed(() => opening.value)

const handleMediaLoad = (event, src) => {
  if (!src) return
  const target = event?.target
  const width = Number(target?.naturalWidth || 0)
  const height = Number(target?.naturalHeight || 0)
  if (!width || !height) return
  landscapeMediaMap[src] = width / height > 1.2
}

const isLandscapeMedia = (src) => Boolean(src && landscapeMediaMap[src])

const handleClose = () => {
  if (closing.value) return
  closing.value = true
  overlayVisible.value = false

  const done = () => {
    if (props.noteId && !route.params.id) {
      emit('close')
    } else {
      const currentPath = route.fullPath
      router.back()
      // 兜底一下：如果 history.back() 没离开当前页，就直接回首页
      window.setTimeout(() => {
        if (route.fullPath === currentPath) {
          router.replace('/')
        }
      }, 120)
    }
  }

  const el = containerRef.value
  const reduceMotion =
    typeof window !== 'undefined' &&
    window.matchMedia &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches

  if (!el || reduceMotion) {
    done()
    return
  }

  let settled = false
  let fallbackTimer = null
  const settle = () => {
    if (settled) return
    settled = true
    if (fallbackTimer) {
      clearTimeout(fallbackTimer)
      fallbackTimer = null
    }
    el.removeEventListener('transitionend', onTransitionEnd)
    done()
  }
  const onTransitionEnd = (event) => {
    if (event.target !== el) return
    if (event.propertyName !== 'transform' && event.propertyName !== 'opacity') return
    settle()
  }
  el.addEventListener('transitionend', onTransitionEnd)
  fallbackTimer = window.setTimeout(settle, CLOSE_DURATION + 60)

  // 有 originRect 时，按卡片原位置做反向 FLIP 动画
  if (props.originRect) {
    const finalRect = el.getBoundingClientRect()
    const start = props.originRect

    // 关闭时回到原卡片尺寸，宽高分别按源卡片比例缩回去
    const scaleX = start.width / finalRect.width
    const scaleY = start.height / finalRect.height

    const startCx = start.left + start.width / 2
    const startCy = start.top + start.height / 2
    const finalCx = finalRect.left + finalRect.width / 2
    const finalCy = finalRect.top + finalRect.height / 2
    const deltaX = startCx - finalCx
    const deltaY = startCy - finalCy

    el.style.transition = `transform ${CLOSE_DURATION}ms ${EASING_IN}`
    el.style.transformOrigin = 'center center'
    el.style.borderRadius = `${FINAL_RADIUS}px`
    el.style.transform = `translate3d(${deltaX}px, ${deltaY}px, 0) scale3d(${scaleX}, ${scaleY}, 1)`
    el.style.opacity = '1'
  } else {
    // 如果是路由直进来的，就走缩小加淡出的关闭动画
    el.style.transition = `transform ${CLOSE_DURATION}ms ${EASING_IN}, opacity ${Math.min(220, CLOSE_DURATION)}ms ease`
    el.style.transformOrigin = 'center center'
    el.style.borderRadius = `${FINAL_RADIUS}px`
    el.style.transform = 'translate3d(0, 40px, 0) scale3d(0.7, 0.7, 1)'
    el.style.opacity = '0'
  }
}

const scheduleVideoLoad = (noteId) => {
  if (videoTimer) {
    clearTimeout(videoTimer)
    videoTimer = null
  }
  videoSrc.value = ''

  if (!note.value || note.value.type !== 'video' || !note.value.videoUrl) {
    return
  }

  // 不再额外加延迟，拿到地址就直接挂载视频
  videoSrc.value = note.value.videoUrl
}

const goToProduct = (id) => {
  const productId = Number(id)
  if (!Number.isFinite(productId) || productId <= 0) {
    ElMessage.warning('关联商品信息异常，请稍后重试')
    return
  }
  router.push({ name: 'ProductDetail', params: { id: productId } }).catch(() => {})
}

const requireLogin = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后操作')
    userStore.showLogin()
    return false
  }
  return true
}

const fetchFollowStatus = async () => {
  const myId = userStore.userInfo?.id
  if (!note.value?.userId || !userStore.isLoggedIn || note.value.userId === myId) {
    isFollowing.value = false
    return
  }
  try {
    const followed = await request.get(`/follow/status/${note.value.userId}`)
    isFollowing.value = Boolean(followed)
  } catch (e) {
    isFollowing.value = false
  }
}

const handleFollow = async () => {
  if (!requireLogin()) return
  if (!note.value?.userId) return
  
  // 先做乐观更新
  const originalFollowing = isFollowing.value
  isFollowing.value = !originalFollowing
  
  followLoading.value = true
  try {
    const followed = await request.post(`/follow/${note.value.userId}`)
    
    // 如果后端结果和本地预期不一致，再纠正回来
    if (Boolean(followed) !== isFollowing.value) {
       isFollowing.value = Boolean(followed)
    }
    
    if (isFollowing.value) {
       ElMessage.success("关注成功")
    } else {
       ElMessage.success("已取消关注")
    }
  } catch (e) {
    // 失败就回滚
    isFollowing.value = originalFollowing
  } finally {
    followLoading.value = false
  }
}


const fetchComments = async (reset = false) => {
  const noteId = activeNoteId.value
  if (!noteId) return
  if (commentLoading.value) return
  if (reset) {
    commentPage.value = 1
    comments.value = []
    commentTotal.value = 0
  }

  commentLoading.value = true
  try {
    const res = await request.get('/comment/list', {
      params: {
        noteId,
        pageNum: commentPage.value,
        pageSize: commentSize
      }
    })

    const records = res?.records || []
    const total = res?.total || records.length

    const mapped = records.map((item) => ({
      ...item,
      userAvatar: getAvatarUrl(item.userAvatar)
    }))

    comments.value = comments.value.concat(mapped)
    commentTotal.value = total
    if (mapped.length > 0) {
      commentPage.value += 1
    }
  } finally {
    commentLoading.value = false
  }
}

const loadMoreComments = () => {
  if (!commentHasMore.value) return
  fetchComments(false)
}

const handleAddComment = async () => {
  if (!requireLogin()) return
  if (!canInteract.value) {
    ElMessage.warning('审核中暂不可互动')
    return
  }
  const noteId = activeNoteId.value
  if (!noteId) {
    ElMessage.warning('笔记信息加载中，请稍后再试')
    return
  }
  const content = commentText.value.trim()
  if (!content) {
    ElMessage.warning('请输入评论内容')
    return
  }

  try {
    const id = await request.post('/comment/add', {
      noteId,
      content
    })

    const user = userStore.userInfo || {}
    comments.value.unshift({
      id,
      noteId,
      userId: user.id,
      content,
      createTime: new Date().toISOString(),
      userNickname: user.nickname || '用户',
      userAvatar: getAvatarUrl(user.avatar),
      mine: true
    })
    commentTotal.value += 1
    if (note.value) {
      note.value.commentCount = (note.value.commentCount || 0) + 1
    }
    commentText.value = ''
  } catch (e) {
    // 这里交给拦截器统一处理
  }
}

const handleDeleteComment = async (comment) => {
  if (!comment?.id) return
  if (!requireLogin()) return
  try {
    await request.delete(`/comment/${comment.id}`)
    comments.value = comments.value.filter((item) => item.id !== comment.id)
    commentTotal.value = Math.max(0, commentTotal.value - 1)
    if (note.value) {
      note.value.commentCount = Math.max(0, (note.value.commentCount || 0) - 1)
    }
    ElMessage.success('已删除评论')
  } catch (e) {
    // 这里交给拦截器统一处理
  }
}

const handleLike = async () => {
  if (!note.value || likeBusy.value) return
  if (!requireLogin()) return
  if (!canInteract.value) {
    ElMessage.warning('审核中暂不可互动')
    return
  }

  // 先做乐观更新
  const originalLiked = note.value.liked
  const originalCount = note.value.likeCount
  
  const newLiked = !originalLiked
  note.value.liked = newLiked
  note.value.likeCount = Math.max(0, (originalCount || 0) + (newLiked ? 1 : -1))
  
  if (newLiked) {
      likeBounce.value = true
      setTimeout(() => {
        likeBounce.value = false
      }, 300)
  }

  likeBusy.value = true
  try {
    const liked = await request.post(`/note/like/${note.value.id}`)
    // 后端结果和预期不一致时再对齐
    if (Boolean(liked) !== newLiked) {
        note.value.liked = Boolean(liked)
        note.value.likeCount = Math.max(0, (originalCount || 0) + (liked ? 1 : -1))
    }
  } catch (e) {
    // 失败就回滚
    note.value.liked = originalLiked
    note.value.likeCount = originalCount
  } finally {
    likeBusy.value = false
  }
}

const handleCollect = async () => {
  if (!note.value || collectBusy.value) return
  if (!requireLogin()) return
  if (!canInteract.value) {
    ElMessage.warning('审核中暂不可互动')
    return
  }

  // 先做乐观更新
  const originalCollected = note.value.collected
  const originalCount = note.value.collectCount
  
  const newCollected = !originalCollected
  note.value.collected = newCollected
  note.value.collectCount = Math.max(0, (originalCount || 0) + (newCollected ? 1 : -1))
  
  collectBusy.value = true
  try {
    const collected = await request.post(`/note/collect/${note.value.id}`)
    if (Boolean(collected) !== newCollected) {
        note.value.collected = Boolean(collected)
        note.value.collectCount = Math.max(0, (originalCount || 0) + (collected ? 1 : -1))
    }
  } catch (e) {
    // 失败就回滚
    note.value.collected = originalCollected
    note.value.collectCount = originalCount
  } finally {
    collectBusy.value = false
  }
}

const handleComplaint = () => {
  if (!requireLogin()) return
  complaintForm.reason = ''
  complaintForm.content = ''
  complaintDialogVisible.value = true
}

const submitComplaint = async () => {
  if (!complaintForm.reason) {
    ElMessage.warning('请选择投诉原因')
    return
  }
  const noteId = activeNoteId.value
  if (!noteId) {
    ElMessage.warning('笔记信息加载中，请稍后再试')
    return
  }
  complaintSubmitting.value = true
  try {
    await submitComplaintApi({
      noteId,
      reason: complaintForm.reason,
      content: complaintForm.content?.trim() || ''
    })
    ElMessage.success('投诉已提交，正在核查中，可在「通知-投诉」查看进度')
    complaintDialogVisible.value = false
  } finally {
    complaintSubmitting.value = false
  }
}

const applyNotePayload = (nextNote, id) => {
  note.value = nextNote
  scheduleVideoLoad(id)
  // 关注状态和评论这类稍重一点的请求，尽量放到打开后再补
  fetchFollowStatus()
  commentTotal.value = Number(nextNote?.commentCount || 0)
  fetchComments(true)
}

const fetchNote = async (id) => {
  if (!id) return
  pendingNote.value = null
  loading.value = true
  try {
    const res = await request.get(`/note/detail/${id}`)
    const products = Array.isArray(res?.products)
      ? res.products.map((p) => ({
          ...p,
          coverImg: getImageUrl(p.coverImg)
        }))
      : []

    const mappedImages = Array.isArray(res.images) ? res.images.map((x) => getImageUrl(x)) : []
    const coverRaw = res.coverImg || (mappedImages.length > 0 ? mappedImages[0] : '')
    const nextNote = {
      ...res,
      products,
      coverImg: coverRaw ? getImageUrl(coverRaw) : '',
      images: mappedImages,
      videoUrl: res.videoUrl ? getImageUrl(res.videoUrl) : '',
      authorAvatar: getAvatarUrl(res.authorAvatar),
      authorName: res.authorNickname || '用户',
      liked: Boolean(res.liked),
      collected: Boolean(res.collected)
    }

    // 直接应用数据，避免动画结束时再闪一下
    applyNotePayload(nextNote, id)
  } finally {
    loading.value = false
  }
}

// 这里不再走 pendingNote，那样收尾时容易闪一下

watch(
  () => activeNoteId.value,
  (newId) => {
    if (newId) {
      commentText.value = ''
      fetchNote(newId)
    }
  },
  { immediate: true }
)

onMounted(() => {
  previousBodyOverflow = document.body.style.overflow
  previousBodyPaddingRight = document.body.style.paddingRight
  const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth
  document.body.style.overflow = 'hidden'
  if (scrollbarWidth > 0) {
    document.body.style.paddingRight = `${scrollbarWidth}px`
  }

  // 第一帧之后再把遮罩显出来，CSS 过渡才能顺利跑起来
  requestAnimationFrame(() => {
    overlayVisible.value = true
  })

  const el = containerRef.value
  const reduceMotion =
    typeof window !== 'undefined' &&
    window.matchMedia &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches

  if (!el) {
    opening.value = false
    return
  }

  // 先把透明度和圆角摆好，避免倒放位移前先闪到终点
  el.style.opacity = '1'
  el.style.borderRadius = `${FINAL_RADIUS}px`

  if (reduceMotion) {
    opening.value = false
    return
  }

  // 这里走类似小红书的缩放展开：从卡片封面 FLIP 到详情弹窗
  if (props.originRect) {
    const start = props.originRect
    const finalRect = el.getBoundingClientRect()

    // 宽高比不一致时用统一缩放，避免看起来像被硬拉伸
    const scaleX = start.width / finalRect.width
    const scaleY = start.height / finalRect.height
    const scale = Math.min(scaleX, scaleY)

    const startCx = start.left + start.width / 2
    const startCy = start.top + start.height / 2
    const finalCx = finalRect.left + finalRect.width / 2
    const finalCy = finalRect.top + finalRect.height / 2
    const deltaX = startCx - finalCx
    const deltaY = startCy - finalCy

    el.style.transformOrigin = 'center center'
    el.style.transition = 'none'
    el.style.borderRadius = `${FINAL_RADIUS}px`
    el.style.transform = `translate3d(${deltaX}px, ${deltaY}px, 0) scale3d(${scale}, ${scale}, 1)`

    // 强制触发布局，让下一帧动画能稳定生效
    // eslint-disable-next-line no-unused-expressions
    el.offsetHeight

    requestAnimationFrame(() => {
      el.style.transition = `transform ${OPEN_DURATION}ms ${EASING_OUT}`
      el.style.transform = 'none'
    })
  } else {
    // 路由直进来的情况，走一个简单的放大入场
    const scale = 0.7
    el.style.transformOrigin = 'center center'
    el.style.transition = 'none'
    el.style.borderRadius = `${FINAL_RADIUS}px`
    el.style.transform = `translate3d(0, 40px, 0) scale3d(${scale}, ${scale}, 1)`

    // eslint-disable-next-line no-unused-expressions
    el.offsetHeight

    requestAnimationFrame(() => {
      el.style.transition = `transform ${OPEN_DURATION}ms ${EASING_OUT}`
      el.style.transform = 'none'
    })
  }

  // 动画稳定后再把更重的 UI 挂上来，顺手淡入右侧信息区
  setTimeout(() => {
    opening.value = false
  }, OPEN_DURATION)
})

onUnmounted(() => {
  document.body.style.overflow = previousBodyOverflow || ''
  document.body.style.paddingRight = previousBodyPaddingRight || ''
  if (videoTimer) {
    clearTimeout(videoTimer)
    videoTimer = null
  }
})
</script>

<style scoped lang="scss">
// 固定遮罩层
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0);
  transition: background-color 280ms ease;
  z-index: 1800;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;

  &.show {
    background-color: rgba(0, 0, 0, 0.6);
    pointer-events: auto;
    /* 这里不加 backdrop-filter，省一点性能开销 */
  }
}

// 关闭按钮
.close-btn {
  position: absolute;
  top: 20px;
  right: 40px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.4);
  transition: all 0.2s;
  z-index: 1801;

  &:hover {
    background: rgba(255, 255, 255, 0.4);
    transform: rotate(90deg);
  }

  .el-icon {
    font-size: 24px;
  }
}

.detail-container {
  width: 1100px;
  height: 85vh;
  background: #fff;
  opacity: 0;
  border-radius: 20px;
  overflow: hidden;
  display: flex;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.2);
  transform-origin: center center;
  will-change: transform, opacity;
  backface-visibility: hidden; // 尽量走 GPU 合成层
  isolation: isolate;

  @media (max-width: 1200px) {
    width: 90vw;
  }
}

.detail-container.is-opening {
  /* 打开时尽量少做重绘，让缩放过程更顺一点 */
}

.detail-container.is-opening .right-section {
  /* 缩放过程中保持文字可见，别等动画结束才突然出现 */
  opacity: 1;
  transform: none;
}

.detail-container.is-closing .right-section,
.detail-container.is-closing .close-btn {
  opacity: 0;
  transition: opacity 0.2s ease;
}

.detail-container.is-opening .bg-layer {
  /* 打开时让右侧背景保持一致，避免落稳后观感突然一跳 */
  filter: blur(10px);
  opacity: 0.3;
}

.left-section {
  width: 60%;
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative; // Needed for absolute positioning of layers


  .image-carousel {
    width: 100%;
    height: 100%;
  }

  .carousel-img-wrapper {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative; // For absolute background
    overflow: hidden;   // Clip blur
    background: #fff;
  }

  .opening-video-badge {
    position: absolute;
    top: 14px;
    right: 14px;
    width: 44px;
    height: 44px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.75);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 2;
    color: #333;
    border: 1px solid rgba(255, 255, 255, 0.85);
  }

  .opening-video-badge .el-icon {
    font-size: 22px;
  }

  .carousel-img {
    /* 缩略图切到大图时直接撑满容器，宽高比交给 object-fit 处理，避免布局抖动 */
    width: 100%;
    height: 100%;
    display: block;
    object-fit: contain;
    z-index: 1; // Above blur
    position: relative;
    transition: transform 0.22s ease;
  }

  .carousel-img--landscape {
    transform: scale(1.06);
  }

  .carousel-video {
    width: 100%;
    height: 100%;
    object-fit: contain;
    background: transparent; // Allow blur to show through (though video covers most)
    z-index: 1;
    position: relative;
  }

  .media-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: #999;
    font-size: 14px;
    background: #f2f3f5;
  }

  .media-placeholder .el-icon {
    font-size: 28px;
    color: #c0c4cc;
  }

  .opening-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: #fff;
    z-index: 10;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: opacity 0.4s ease;
    opacity: 1;
    pointer-events: none;

    &.is-hidden {
      opacity: 0;
    }

    .cover-img {
      width: 100%;
      height: 100%;
      object-fit: contain;
      display: block;
    }
  }

  .static-layer,
  .interactive-layer {
    background: #fff;
  }
}

:deep(.el-carousel__arrow) {
  width: 48px;
  height: 48px;
}

:deep(.el-carousel__arrow i) {
  font-size: 20px;
}

.right-section {
  width: 40%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-left: 1px solid #f0f0f0;
  opacity: 1;
  transform: translate3d(0, 0, 0);
  transition: opacity 220ms ease, transform 220ms ease;
}

.detail-header {
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f8f8f8;

  .author-info {
    display: flex;
    align-items: center;
    gap: 12px;

    .nickname {
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
  }

  .follow-btn {
    border: 1px solid #ff6b81;
    background: transparent;
    color: #ff6b81;
    border-radius: 20px;
    padding: 6px 18px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #ff6b81;
      color: #fff;
    }

    &.active {
      background: #ff6b81;
      color: #fff;
    }

    &:disabled {
      cursor: not-allowed;
      opacity: 0.6;
    }
  }
}

.detail-scroll-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;

  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-thumb {
    background: #eee;
    border-radius: 3px;
  }

  .note-title {
    font-size: 20px;
    font-weight: 700;
    color: #333;
    margin-bottom: 12px;
    line-height: 1.4;
  }

  .note-status {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
  }

  .note-status-hint {
    font-size: 12px;
    color: #909399;
  }

  .note-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 12px;
  }

  .tag-chip {
    font-size: 12px;
    color: #ff6b81;
    background: #fff1f4;
    padding: 2px 8px;
    border-radius: 999px;
  }

  .note-desc {
    font-size: 15px;
    color: #333;
    line-height: 1.6;
    margin-bottom: 16px;
    white-space: pre-line;
  }

  .publish-time {
    font-size: 12px;
    color: #999;
    margin-bottom: 24px;
  }
}

.product-capsule-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.product-capsule {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f8f8f8;
  border-radius: 99px;
  padding: 0 16px;
  height: 40px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #fff0f3;

    .capsule-left .name {
      color: #ff6b81;
    }
  }

  .capsule-left {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #333;

    .thumb {
      width: 24px;
      height: 24px;
      border-radius: 4px;
      object-fit: cover;
      border: 1px solid #eee;
    }

    .prefix {
      color: #999;
      font-size: 12px;
    }

    .name {
      font-weight: 500;
      max-width: 120px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .price {
      color: #ff6b81;
      font-weight: 700;
    }
  }

  .capsule-right {
    font-size: 12px;
    color: #999;
    display: flex;
    align-items: center;
    gap: 2px;
  }
}

.comments-section {
  margin-top: 10px;

  .comments-count {
    font-size: 14px;
    color: #666;
    margin-bottom: 16px;
  }

  .comment-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-bottom: 12px;
  }

  .comment-item {
    display: flex;
    gap: 12px;
    align-items: flex-start;
  }

  .comment-content {
    flex: 1;
    min-width: 0;

    .comment-user {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 12px;
      color: #999;
      margin-bottom: 4px;
    }

    .comment-text {
      font-size: 14px;
      color: #333;
      line-height: 1.5;
      word-break: break-word;
    }
  }

  .comment-load-more {
    display: flex;
    justify-content: center;
    margin-top: 8px;
  }
}

.detail-footer {
  padding: 16px 24px;
  border-top: 1px solid #f8f8f8;
  display: flex;
  align-items: center;
  gap: 20px;

  .action-input-wrapper {
    flex: 1;
    min-width: 0;
    margin-right: 16px;
    display: flex;
    align-items: center;
    gap: 10px;

    .comment-input {
      width: 100%;
      height: 40px;
      border: 1px solid #f0f0f0;
      background: #f8f8f8;
      border-radius: 20px;
      padding: 0 16px;
      font-size: 14px;
      color: #666;
      outline: none;
      transition: all 0.2s;
      box-sizing: border-box;
    }

    .comment-send {
      height: 40px;
      border-radius: 20px;
      padding: 0 18px;
      font-size: 14px;
      color: #fff;
      --el-button-bg-color: #ff6b81;
      --el-button-border-color: #ff6b81;
      --el-button-hover-bg-color: #ff8093;
      --el-button-hover-border-color: #ff8093;
      --el-button-active-bg-color: #f45a72;
      --el-button-active-border-color: #f45a72;
      --el-button-disabled-bg-color: #ffd2db;
      --el-button-disabled-border-color: #ffd2db;
      --el-button-disabled-text-color: #ffffff;
    }
  }

  .action-icons {
    display: flex;
    gap: 20px;
    flex-shrink: 0;
    align-items: center;

    .action-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      color: #333;
      font-size: 11px;
      cursor: pointer;
      position: relative;
      transition: transform 0.15s ease;

      &.bounce {
        animation: bounce 0.3s ease;
      }

      .el-icon {
        font-size: 24px;
        margin-bottom: 2px;
        transition: all 0.2s;
        color: #333;

        &.starred {
          color: #ff6b81;
        }
      }

      &:hover .el-icon {
        transform: scale(1.1);
      }

      &:active .el-icon {
        transform: scale(0.95);
      }
    }
  }
}

@keyframes bounce {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.4);
  }
}
</style>
