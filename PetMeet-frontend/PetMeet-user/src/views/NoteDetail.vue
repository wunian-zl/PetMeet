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
          <NoteComments
            ref="commentPanelRef"
            :note-id="activeNoteId"
            :author-id="note?.userId"
            :initial-count="Number(note?.commentCount || 0)"
            :can-interact="canInteract"
            @reply="handleReplyComment"
            @report="handleReportComment"
            @count-change="handleCommentCountChange"
          />
        </div>

      <!-- 底部操作区 -->
        <div class="detail-footer">
          <div class="comment-composer">
            <div v-if="replyTarget" class="reply-context">
              <div class="reply-context-copy">
                <span>回复 {{ replyTarget.userNickname || '用户' }}</span>
                <em>{{ replyTarget.content }}</em>
              </div>
              <button type="button" @click="clearReplyTarget">取消</button>
            </div>
            <div class="action-input-wrapper">
              <input
                ref="commentInputRef"
                type="text"
                v-model="commentText"
                :placeholder="commentPlaceholder"
                class="comment-input"
                maxlength="200"
                :disabled="!canInteract || !activeNoteId"
                @keyup.enter="handleSubmitComment"
              />
              <el-button class="comment-send" type="primary" :disabled="!canInteract || !activeNoteId" @click="handleSubmitComment">发送</el-button>
            </div>
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

    <el-dialog
      v-model="complaintDialogVisible"
      :title="reportedComment ? '' : complaintDialogTitle"
      :width="reportedComment ? '520px' : '420px'"
      :show-close="!reportedComment"
      :class="{ 'comment-report-dialog': reportedComment }"
      @closed="handleComplaintDialogClosed"
    >
      <div v-if="reportedComment" class="comment-report-panel">
        <div class="comment-report-header">
          <h3>举报评论</h3>
          <button type="button" class="comment-report-close" aria-label="关闭举报弹窗" @click="closeComplaintDialog">
            <el-icon><Close /></el-icon>
          </button>
        </div>

        <div class="comment-report-list">
          <div
            v-for="reason in commentComplaintReasons"
            :key="reason"
            class="comment-report-reason-block"
          >
            <button
              type="button"
              class="comment-report-option"
              :class="{ active: complaintForm.reason === reason }"
              :aria-pressed="complaintForm.reason === reason"
              @click="selectComplaintReason(reason)"
            >
              <span>{{ reason }}</span>
              <i class="comment-report-radio" :class="{ checked: complaintForm.reason === reason }" aria-hidden="true"></i>
            </button>

            <div
              v-if="complaintForm.reason === reason"
              ref="commentReportExtraRef"
              class="comment-report-extra"
              @paste="handleComplaintEvidencePaste"
            >
              <label for="comment-report-content">补充说明（选填）</label>
              <textarea
                id="comment-report-content"
                v-model="complaintForm.content"
                maxlength="200"
                rows="3"
                placeholder="可以补充对方具体行为、上下文等"
              ></textarea>
              <span>{{ complaintForm.content.length }}/200</span>

              <div class="comment-report-evidence">
                <div class="comment-report-evidence-title">
                  图片凭证（选填，最多3张）
                </div>
                <div
                  class="comment-report-paste-zone"
                  tabindex="0"
                  role="group"
                  aria-label="图片凭证上传区，支持粘贴截图或点击上传"
                  @paste="handleComplaintEvidencePaste"
                >
                  <el-upload
                    action="/api/common/upload/image"
                    name="file"
                    :data="{ biz: 'complaintEvidence' }"
                    :headers="uploadHeaders"
                    v-model:file-list="complaintEvidenceFileList"
                    list-type="picture-card"
                    accept="image/*"
                    :limit="3"
                    :before-upload="beforeComplaintEvidenceUpload"
                    :on-success="handleComplaintEvidenceSuccess"
                    :on-remove="handleComplaintEvidenceRemove"
                    :on-exceed="handleComplaintEvidenceExceed"
                  >
                    <el-icon><Plus /></el-icon>
                  </el-upload>
                  <div class="comment-report-paste-hint">
                    <strong>截图后按Ctrl+V粘贴</strong>
                    <span>或点击加号上传</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="comment-report-submitbar">
          <button
            type="button"
            class="comment-report-submit"
            :disabled="complaintSubmitting"
            @click="submitComplaint"
          >
            {{ complaintSubmitting ? '提交中' : '提交' }}
          </button>
        </div>
      </div>

      <el-form v-else>
        <el-form-item label="原因">
          <el-radio-group v-model="complaintForm.reason">
            <el-radio v-for="r in complaintReasons" :key="r" :label="r">{{ r }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="complaintForm.content" type="textarea" :rows="3" placeholder="补充说明（可选）" />
        </el-form-item>
      </el-form>
      <template v-if="!reportedComment" #footer>
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
import { Close, UserFilled, Star, StarFilled, ArrowRight, Warning, VideoPlay, Plus } from '@element-plus/icons-vue'
import HeartIcon from '@/components/HeartIcon.vue'
import NoteComments from '@/components/NoteComments.vue'
import request from '@/utils/request'
import { submitComplaint as submitComplaintApi } from '@/api/complaint'
import { getImageUrl, getAvatarUrl } from '@/utils/image'
import { useUserStore } from '@/store/user'
import { releaseDocumentScrollIfNoOverlay } from '@/utils/scrollLock'

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

const normalizeInitialNote = (source) => {
  if (!source) return null
  const coverImg = source.coverImg ? getImageUrl(source.coverImg) : ''
  const images = Array.isArray(source.images)
    ? source.images.filter(Boolean).map((src) => getImageUrl(src))
    : []
  return {
    ...source,
    coverImg,
    images: images.length > 0 ? images : (coverImg ? [coverImg] : [])
  }
}

// 有 initialNote 时先秒开，细节数据再补
const note = ref(normalizeInitialNote(props.initialNote))
const pendingNote = ref(null) // { note: object, id: string|number } applied after opening animation
const loading = ref(!props.initialNote)
const commentText = ref('')
const commentInputRef = ref(null)
const commentPanelRef = ref(null)
const commentReportExtraRef = ref(null)
const complaintEvidenceFileList = ref([])
const replyTarget = ref(null)
const containerRef = ref(null)
const overlayVisible = ref(false)
const opening = ref(true)
const closing = ref(false)
const likeBusy = ref(false)
const collectBusy = ref(false)
const likeBounce = ref(false)
const followLoading = ref(false)
const isFollowing = ref(false)
const complaintDialogVisible = ref(false)
const complaintSubmitting = ref(false)
const complaintPasteUploading = ref(false)
const reportedComment = ref(null)
const complaintForm = reactive({
  reason: '',
  content: '',
  evidenceImages: []
})
const complaintReasons = ['侵权', '盗用', '其他']
const commentComplaintReasons = [
  '辱骂攻击',
  '引战挑衅',
  '垃圾广告',
  '虚假误导',
  '低俗不适',
  '违法违规',
  '骚扰诱导',
  '未成年相关',
  '其他问题'
]
const videoSrc = ref('')
let videoTimer = null
let previousBodyOverflow = ''
let previousBodyPaddingRight = ''
let mediaSwapToken = 0
const landscapeMediaMap = reactive({})

const OPEN_DURATION = 620
const CLOSE_DURATION = 300
const EASING_OUT = 'cubic-bezier(0.16, 1, 0.3, 1)'
const EASING_IN = 'cubic-bezier(0.4, 0, 1, 1)'
const FINAL_RADIUS = 20

const commentPlaceholder = computed(() => replyTarget.value ? `回复 ${replyTarget.value.userNickname || '用户'}...` : '说点什么...')
const complaintDialogTitle = computed(() => reportedComment.value ? '举报评论' : '投诉笔记')
const uploadHeaders = computed(() => ({
  Authorization: userStore.token || localStorage.getItem('token') || ''
}))
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

const getFirstMediaSrc = (source) => {
  const images = Array.isArray(source?.images) ? source.images.filter(Boolean) : []
  return images[0] || source?.coverImg || ''
}

const buildHeldMediaNote = (nextNote, currentNote) => {
  const currentImages = Array.isArray(currentNote?.images)
    ? currentNote.images.filter(Boolean).map((src) => getImageUrl(src))
    : []
  const currentCover = currentNote?.coverImg ? getImageUrl(currentNote.coverImg) : ''
  return {
    ...nextNote,
    coverImg: currentCover || nextNote.coverImg,
    images: currentImages
  }
}

const shouldHoldCurrentMedia = (id, nextNote, detailImages) => {
  if (!note.value || nextNote.type === 'video') return false
  if (String(note.value.id) !== String(id)) return false
  const currentSrc = getImageUrl(getFirstMediaSrc(note.value))
  const nextSrc = getImageUrl(detailImages[0] || nextNote.coverImg)
  return Boolean(currentSrc && nextSrc && currentSrc !== nextSrc)
}

const preloadImage = (src) => new Promise((resolve, reject) => {
  if (!src) {
    resolve('')
    return
  }
  const img = new Image()
  let settled = false
  const finish = () => {
    if (settled) return
    settled = true
    const decodeTask = typeof img.decode === 'function'
      ? img.decode().catch(() => {})
      : Promise.resolve()
    decodeTask.then(() => resolve(src))
  }
  img.onload = finish
  img.onerror = reject
  img.src = src
  if (img.complete && img.naturalWidth > 0) {
    finish()
  }
})

const waitForOpeningSettled = () => new Promise((resolve) => {
  if (!opening.value) {
    resolve()
    return
  }
  window.setTimeout(resolve, OPEN_DURATION + 30)
})

const swapDetailMediaWhenReady = async (id, media, token) => {
  const firstSrc = media.images[0] || media.coverImg
  try {
    await Promise.all([
      preloadImage(firstSrc),
      waitForOpeningSettled()
    ])
  } catch (e) {
    return
  }
  media.images.slice(1).forEach((src) => {
    preloadImage(src).catch(() => {})
  })
  if (token !== mediaSwapToken || String(activeNoteId.value) !== String(id) || !note.value) {
    return
  }
  note.value = {
    ...note.value,
    coverImg: media.coverImg,
    images: media.images
  }
}

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
    window.dispatchEvent(new CustomEvent('petmeet:follow-changed', {
      detail: {
        userId: note.value.userId,
        followed: isFollowing.value
      }
    }))
  } catch (e) {
    // 失败就回滚
    isFollowing.value = originalFollowing
  } finally {
    followLoading.value = false
  }
}


const handleSubmitComment = async () => {
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
    const ok = await commentPanelRef.value?.submitComment(content, replyTarget.value)
    if (ok) {
      commentText.value = ''
      replyTarget.value = null
    }
  } catch (e) {
    // 这里交给拦截器统一处理
  }
}

const handleReplyComment = (comment) => {
  if (!requireLogin()) return
  if (!canInteract.value) {
    ElMessage.warning('审核中暂不可互动')
    return
  }
  replyTarget.value = comment
  nextTick(() => {
    commentInputRef.value?.focus()
  })
}

const clearReplyTarget = () => {
  replyTarget.value = null
}

const resetComplaintForm = () => {
  complaintForm.reason = ''
  complaintForm.content = ''
  complaintForm.evidenceImages = []
  complaintEvidenceFileList.value = []
}

const selectComplaintReason = (reason) => {
  complaintForm.reason = reason
  nextTick(() => {
    const extra = Array.isArray(commentReportExtraRef.value)
      ? commentReportExtraRef.value[0]
      : commentReportExtraRef.value
    extra?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  })
}

const syncComplaintEvidence = (files) => {
  const list = Array.isArray(files) ? files : []
  complaintForm.evidenceImages = list
    .filter((file) => file.status !== 'uploading')
    .map((file) => file.rawUrl || file.response?.data || '')
    .filter(Boolean)
}

const beforeComplaintEvidenceUpload = (file) => {
  if (!file?.type?.startsWith('image/')) {
    ElMessage.warning('只能上传图片凭证')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('单张图片不能超过10MB')
    return false
  }
  return true
}

const handleComplaintEvidenceSuccess = (response, uploadFile, uploadFiles) => {
  if (response?.code !== 200 || !response?.data) {
    ElMessage.error(response?.message || response?.msg || '凭证上传失败')
    return
  }
  uploadFile.rawUrl = response.data
  uploadFile.url = getImageUrl(response.data)
  complaintEvidenceFileList.value = Array.isArray(uploadFiles) ? [...uploadFiles] : complaintEvidenceFileList.value
  syncComplaintEvidence(uploadFiles)
}

const handleComplaintEvidenceRemove = (uploadFile, uploadFiles) => {
  complaintEvidenceFileList.value = Array.isArray(uploadFiles) ? [...uploadFiles] : []
  syncComplaintEvidence(uploadFiles)
}

const handleComplaintEvidenceExceed = () => {
  ElMessage.warning('最多上传3张图片凭证')
}

const buildClipboardImageFile = (blob, index = 0) => {
  const extension = blob.type?.split('/')[1] || 'png'
  const filename = `complaint-evidence-${Date.now()}-${index}.${extension}`
  return new File([blob], filename, { type: blob.type || 'image/png' })
}

const uploadComplaintEvidenceFile = async (file) => {
  if (!beforeComplaintEvidenceUpload(file)) return
  if (complaintEvidenceFileList.value.length >= 3) {
    handleComplaintEvidenceExceed()
    return
  }

  const uid = `paste-${Date.now()}-${Math.random().toString(16).slice(2)}`
  const previewUrl = URL.createObjectURL(file)
  const uploadItem = {
    uid,
    name: file.name,
    status: 'uploading',
    percentage: 0,
    url: previewUrl,
    raw: file
  }
  complaintEvidenceFileList.value = [...complaintEvidenceFileList.value, uploadItem]

  try {
    const formData = new FormData()
    formData.append('file', file)
    const imageUrl = await request({
      url: '/common/upload/image',
      method: 'post',
      params: { biz: 'complaintEvidence' },
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    complaintEvidenceFileList.value = complaintEvidenceFileList.value.map((item) => item.uid === uid
      ? {
          ...item,
          status: 'success',
          percentage: 100,
          rawUrl: imageUrl,
          url: getImageUrl(imageUrl)
        }
      : item)
    syncComplaintEvidence(complaintEvidenceFileList.value)
  } catch (e) {
    complaintEvidenceFileList.value = complaintEvidenceFileList.value.filter((item) => item.uid !== uid)
    syncComplaintEvidence(complaintEvidenceFileList.value)
    ElMessage.error(e?.message || '截图上传失败')
  } finally {
    URL.revokeObjectURL(previewUrl)
  }
}

const handleComplaintEvidencePaste = async (event) => {
  const items = Array.from(event.clipboardData?.items || [])
  const imageFiles = items
    .filter((item) => item.type?.startsWith('image/'))
    .map((item) => item.getAsFile())
    .filter(Boolean)
    .sort((a, b) => b.size - a.size)
  if (!imageFiles.length) return

  event.preventDefault()
  event.stopPropagation()
  if (complaintPasteUploading.value || complaintEvidenceFileList.value.some((file) => file.status === 'uploading')) {
    ElMessage.warning('图片凭证上传中，请稍后再粘贴')
    return
  }
  if (complaintEvidenceFileList.value.length >= 3) {
    handleComplaintEvidenceExceed()
    return
  }

  const file = buildClipboardImageFile(imageFiles[0])
  complaintPasteUploading.value = true
  try {
    await uploadComplaintEvidenceFile(file)
  } finally {
    complaintPasteUploading.value = false
  }
}

const closeComplaintDialog = () => {
  complaintDialogVisible.value = false
}

const handleComplaintDialogClosed = () => {
  reportedComment.value = null
  resetComplaintForm()
}

const handleReportComment = (comment) => {
  if (!requireLogin()) return
  reportedComment.value = comment
  resetComplaintForm()
  complaintDialogVisible.value = true
}

const handleCommentCountChange = (delta) => {
  if (!note.value) return
  note.value.commentCount = Math.max(0, Number(note.value.commentCount || 0) + Number(delta || 0))
}

const notifyNoteChanged = (eventName, extra = {}) => {
  if (!note.value) return
  window.dispatchEvent(new CustomEvent(eventName, {
    detail: {
      noteId: note.value.id,
      userId: note.value.userId,
      liked: note.value.liked,
      likeCount: note.value.likeCount,
      collected: note.value.collected,
      collectCount: note.value.collectCount,
      ...extra
    }
  }))
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
    notifyNoteChanged('petmeet:note-like-changed')
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
    notifyNoteChanged('petmeet:note-collect-changed')
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
  reportedComment.value = null
  resetComplaintForm()
  complaintDialogVisible.value = true
}

const submitComplaint = async () => {
  if (!complaintForm.reason) {
    ElMessage.warning(`请选择${reportedComment.value ? '举报' : '投诉'}原因`)
    return
  }
  const noteId = activeNoteId.value
  if (!noteId) {
    ElMessage.warning('笔记信息加载中，请稍后再试')
    return
  }
  if (complaintEvidenceFileList.value.some((file) => file.status === 'uploading')) {
    ElMessage.warning('图片凭证上传中，请稍后提交')
    return
  }
  const isCommentReport = Boolean(reportedComment.value?.id)
  complaintSubmitting.value = true
  try {
    const payload = {
      noteId,
      reason: complaintForm.reason,
      content: complaintForm.content?.trim() || '',
      evidenceImages: complaintForm.evidenceImages || []
    }
    if (isCommentReport) {
      payload.targetType = 'comment'
      payload.commentId = reportedComment.value.id
    }
    await submitComplaintApi(payload)
    ElMessage.success(`${isCommentReport ? '举报' : '投诉'}已提交，正在核查中，可在「通知-投诉」查看进度`)
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
}

const fetchNote = async (id) => {
  if (!id) return
  pendingNote.value = null
  mediaSwapToken += 1
  const token = mediaSwapToken
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

    const holdCurrentMedia = shouldHoldCurrentMedia(id, nextNote, mappedImages)
    if (holdCurrentMedia) {
      const media = {
        coverImg: nextNote.coverImg,
        images: mappedImages
      }
      applyNotePayload(buildHeldMediaNote(nextNote, note.value), id)
      swapDetailMediaWhenReady(id, media, token)
    } else {
      applyNotePayload(nextNote, id)
    }
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
      replyTarget.value = null
      reportedComment.value = null
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

  el.style.opacity = '0.96'
  el.style.transformOrigin = 'center center'
  el.style.transition = 'none'
  el.style.borderRadius = `${FINAL_RADIUS}px`

  if (reduceMotion) {
    el.style.opacity = '1'
    el.style.transform = 'none'
    opening.value = false
    return
  }

  if (props.originRect) {
    const start = props.originRect
    const finalRect = el.getBoundingClientRect()
    const scaleX = start.width / finalRect.width
    const scaleY = start.height / finalRect.height
    const scale = Math.min(scaleX, scaleY)

    const startCx = start.left + start.width / 2
    const startCy = start.top + start.height / 2
    const finalCx = finalRect.left + finalRect.width / 2
    const finalCy = finalRect.top + finalRect.height / 2
    const deltaX = startCx - finalCx
    const deltaY = startCy - finalCy

    el.style.transform = `translate3d(${deltaX}px, ${deltaY}px, 0) scale3d(${scale}, ${scale}, 1)`
  } else {
    el.style.transform = 'translate3d(0, 28px, 0) scale3d(0.82, 0.82, 1)'
  }

  // 保留从卡片自然放大的感觉，但直接显示完整详情，避免先铺满一张图。
  // eslint-disable-next-line no-unused-expressions
  el.offsetHeight

  requestAnimationFrame(() => {
    el.style.transition = `transform ${OPEN_DURATION}ms ${EASING_OUT}, opacity 160ms ease`
    el.style.opacity = '1'
    el.style.transform = 'none'
  })

  // 动画稳定后再把更重的 UI 挂上来，顺手淡入右侧信息区
  setTimeout(() => {
    opening.value = false
  }, OPEN_DURATION)
})

onUnmounted(() => {
  document.body.style.overflow = previousBodyOverflow || ''
  document.body.style.paddingRight = previousBodyPaddingRight || ''
  releaseDocumentScrollIfNoOverlay()
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

:deep(.comment-report-dialog) {
  max-width: calc(100vw - 24px);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 22px 70px rgba(29, 24, 27, 0.2);
}

:deep(.comment-report-dialog .el-dialog__header) {
  display: none;
}

:deep(.comment-report-dialog .el-dialog__body) {
  padding: 0;
  max-height: min(78vh, 690px);
  overflow: hidden;
}

.comment-report-panel {
  max-height: min(78vh, 690px);
  background: #fff;
  display: flex;
  flex-direction: column;
}

.comment-report-header {
  position: relative;
  height: 74px;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #f1f1f1;

  h3 {
    margin: 0;
    color: #2f3033;
    font-size: 22px;
    line-height: 1;
    font-weight: 800;
  }
}

.comment-report-close {
  position: absolute;
  right: 18px;
  top: 50%;
  width: 42px;
  height: 42px;
  transform: translateY(-50%);
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #5f6064;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.18s ease, color 0.18s ease;

  &:hover {
    background: #f6f6f6;
    color: #303033;
  }

  .el-icon {
    font-size: 28px;
  }
}

.comment-report-list {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 14px 6px;
  scrollbar-width: thin;
}

.comment-report-reason-block {
  margin: 0 0 8px;
}

.comment-report-option {
  width: 100%;
  min-height: 58px;
  margin: 0;
  padding: 0 16px 0 18px;
  border: none;
  border-radius: 12px;
  background: #fff;
  color: #62646a;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  text-align: left;
  font-size: 18px;
  line-height: 1.35;
  transition: background-color 0.18s ease, color 0.18s ease;

  &:hover,
  &.active {
    background: #f6f6f6;
    color: #2f3033;
  }
}

.comment-report-radio {
  position: relative;
  width: 25px;
  height: 25px;
  flex: 0 0 auto;
  border-radius: 50%;
  border: 3px solid #e6e6e8;
  box-sizing: border-box;
  transition: border-color 0.18s ease, background-color 0.18s ease;

  &.checked {
    border-color: #ff8da1;
    background: #ff8da1;
  }

  &.checked::after {
    content: '';
    position: absolute;
    left: 50%;
    top: 50%;
    width: 9px;
    height: 9px;
    border-radius: 50%;
    background: #fff;
    transform: translate(-50%, -50%);
  }
}

.comment-report-extra {
  margin: 8px 0 4px;
  padding: 14px;
  border-radius: 14px;
  background: #f7f7f8;
  display: flex;
  flex-direction: column;
  gap: 8px;

  label {
    color: #3f4044;
    font-size: 15px;
    font-weight: 700;
    line-height: 1;
  }

  textarea {
    width: 100%;
    min-height: 74px;
    resize: none;
    border: none;
    outline: none;
    background: transparent;
    color: #333438;
    font-size: 15px;
    line-height: 1.55;
    box-sizing: border-box;
    font-family: inherit;

    &::placeholder {
      color: #a1a3a8;
    }
  }

  > span {
    align-self: flex-end;
    color: #a0a1a6;
    font-size: 12px;
    line-height: 1;
  }
}

.comment-report-evidence {
  margin-top: 4px;
}

.comment-report-evidence-title {
  margin-bottom: 10px;
  color: #4f5259;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.2;
}

.comment-report-paste-zone {
  min-height: 88px;
  padding: 10px;
  border-radius: 14px;
  border: 1px dashed #d9dbe0;
  background: #fff;
  outline: none;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease;

  &:focus,
  &:focus-within {
    border-color: #ff9aae;
    background: #fffafb;
    box-shadow: 0 0 0 3px rgba(255, 141, 161, 0.14);
  }
}

.comment-report-evidence {
  :deep(.el-upload--picture-card),
  :deep(.el-upload-list--picture-card .el-upload-list__item) {
    width: 68px;
    height: 68px;
    border-radius: 12px;
  }

  :deep(.el-upload--picture-card) {
    --el-upload-picture-card-size: 68px;
    background: #fff;
    border: 1px dashed #d9dbe0;
    color: #9da0a6;
  }

  :deep(.el-upload-list--picture-card) {
    --el-upload-list-picture-card-size: 68px;
  }

  :deep(.el-upload-list__item-thumbnail) {
    object-fit: cover;
  }
}

.comment-report-paste-hint {
  min-width: 150px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #a0a3aa;
  font-size: 12px;
  line-height: 1.35;

  strong {
    color: #62666f;
    font-size: 13px;
    font-weight: 700;
  }

  span {
    color: #a0a3aa;
  }
}

.comment-report-submitbar {
  flex: 0 0 auto;
  height: 72px;
  border-top: 1px solid #ececec;
  background: rgba(255, 255, 255, 0.96);
  display: flex;
  align-items: center;
  justify-content: center;
}

.comment-report-submit {
  min-width: 126px;
  height: 48px;
  padding: 0 28px;
  border: none;
  border-radius: 999px;
  background: #ff8da1;
  color: #fff;
  cursor: pointer;
  font-size: 18px;
  font-weight: 800;
  line-height: 1;
  box-shadow: 0 8px 18px rgba(255, 141, 161, 0.25);
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.18s ease;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 12px 24px rgba(255, 141, 161, 0.32);
  }

  &:disabled {
    cursor: default;
    opacity: 0.65;
    box-shadow: none;
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

.detail-footer {
  padding: 16px 24px;
  border-top: 1px solid #f8f8f8;
  display: flex;
  align-items: flex-end;
  gap: 20px;

  .comment-composer {
    flex: 1;
    min-width: 0;
    margin-right: 16px;
  }

  .reply-context {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    min-height: 34px;
    margin-bottom: 8px;
    padding: 7px 12px;
    border-radius: 12px;
    background: #f7f7f7;
    color: #777;
    box-sizing: border-box;
  }

  .reply-context-copy {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
    font-size: 13px;
    line-height: 1.3;

    span,
    em {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    em {
      color: #aaa;
      font-style: normal;
    }
  }

  .reply-context button {
    flex: 0 0 auto;
    border: 0;
    background: transparent;
    color: #666;
    font-size: 13px;
    cursor: pointer;
  }

  .action-input-wrapper {
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

@media (max-width: 640px) {
  :deep(.comment-report-dialog) {
    width: calc(100vw - 24px) !important;
    margin: 0 auto;
    border-radius: 16px;
  }

  :deep(.comment-report-dialog .el-dialog__body),
  .comment-report-panel {
    max-height: 82vh;
  }

  .comment-report-header {
    height: 66px;

    h3 {
      font-size: 20px;
    }
  }

  .comment-report-close {
    right: 12px;
    width: 40px;
    height: 40px;
  }

  .comment-report-list {
    padding: 10px 10px 2px;
  }

  .comment-report-option {
    min-height: 56px;
    padding: 0 14px;
    font-size: 17px;
    border-radius: 11px;
  }

  .comment-report-extra {
    margin-bottom: 2px;
    padding: 13px;

    textarea {
      min-height: 70px;
    }
  }

  .comment-report-submitbar {
    height: 68px;
  }

  .comment-report-submit {
    min-width: 112px;
    height: 44px;
    font-size: 17px;
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
