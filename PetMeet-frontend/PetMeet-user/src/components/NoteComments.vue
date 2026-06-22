<template>
  <section class="comments-section">
    <div class="comments-count">评论 {{ commentTotal }} 条</div>

    <div v-if="loading && comments.length === 0" class="comments-loading">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="comments.length === 0" class="comments-empty">
      <el-empty description="暂无评论，快来抢沙发" />
    </div>

    <div v-else class="comment-list">
      <article
        v-for="comment in comments"
        :key="comment.id"
        class="comment-thread"
        :class="{ 'is-deleted': comment.deleted }"
      >
        <div v-if="comment.deleted" class="deleted-root">评论已删除</div>

        <div v-else class="comment-row">
          <el-avatar :size="36" :src="comment.userAvatar" :icon="UserFilled" class="comment-avatar" />
          <div class="comment-main">
            <div class="comment-head">
              <span class="comment-name">{{ comment.userNickname || '用户' }}</span>
              <span v-if="comment.author" class="author-badge">作者</span>
            </div>
            <div class="comment-text">{{ comment.content }}</div>
            <div class="comment-actions">
              <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
              <button class="comment-action" type="button" @click="toggleLike(comment)">
                <HeartIcon :filled="comment.liked" />
                <span>{{ comment.likeCount > 0 ? comment.likeCount : '赞' }}</span>
              </button>
              <button class="comment-action" type="button" @click="selectReply(comment)">回复</button>
            </div>
          </div>
          <el-dropdown trigger="click" @command="(command) => handleMenu(command, comment)">
            <button class="comment-menu" type="button" aria-label="更多操作">
              <el-icon><MoreFilled /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :command="comment.mine ? 'delete' : 'report'">
                  {{ comment.mine ? '删除评论' : '举报评论' }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div v-if="comment.replies.length || comment.replyCount > 0" class="reply-list">
          <div v-for="reply in comment.replies" :key="reply.id" class="reply-row">
            <el-avatar :size="26" :src="reply.userAvatar" :icon="UserFilled" class="reply-avatar" />
            <div class="reply-main">
              <div class="reply-head">
                <span class="comment-name">{{ reply.userNickname || '用户' }}</span>
                <span v-if="reply.author" class="author-badge">作者</span>
              </div>
              <div class="comment-text">
                <span v-if="reply.replyToNickname" class="reply-target">回复 @{{ reply.replyToNickname }}：</span>
                {{ reply.content }}
              </div>
              <div class="comment-actions">
                <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
                <button class="comment-action" type="button" @click="toggleLike(reply)">
                  <HeartIcon :filled="reply.liked" />
                  <span>{{ reply.likeCount > 0 ? reply.likeCount : '赞' }}</span>
                </button>
                <button class="comment-action" type="button" @click="selectReply(reply)">回复</button>
              </div>
            </div>
            <el-dropdown trigger="click" @command="(command) => handleMenu(command, reply)">
              <button class="comment-menu is-reply" type="button" aria-label="更多操作">
                <el-icon><MoreFilled /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="reply.mine ? 'delete' : 'report'">
                    {{ reply.mine ? '删除评论' : '举报评论' }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <button
            v-if="hasMoreReplies(comment)"
            class="reply-more"
            type="button"
            :disabled="comment.replyLoading"
            @click="loadReplies(comment, !comment.repliesExpanded)"
          >
            {{ replyMoreLabel(comment) }}
          </button>
        </div>
      </article>
    </div>

    <div v-if="hasMoreThreads" class="comment-load-more">
      <el-button :loading="loading" @click="fetchComments(false)">加载更多</el-button>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MoreFilled, UserFilled } from '@element-plus/icons-vue'
import HeartIcon from '@/components/HeartIcon.vue'
import { formatTime } from '@/utils/format'
import { getAvatarUrl } from '@/utils/image'
import { useUserStore } from '@/store/user'
import {
  addComment,
  deleteComment,
  getCommentList,
  getCommentReplies,
  toggleCommentLike
} from '@/api/comment'

const props = defineProps({
  noteId: {
    type: [String, Number],
    default: null
  },
  authorId: {
    type: [String, Number],
    default: null
  },
  initialCount: {
    type: Number,
    default: 0
  },
  canInteract: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['reply', 'report', 'count-change'])

const userStore = useUserStore()
const comments = ref([])
const loading = ref(false)
const pageNum = ref(1)
const threadTotal = ref(0)
const commentTotal = ref(Number(props.initialCount || 0))

const pageSize = 10
const replyPageSize = 40

const hasMoreThreads = computed(() => comments.value.length < threadTotal.value)

watch(
  () => props.initialCount,
  (value) => {
    commentTotal.value = Number(value || 0)
  }
)

const resetState = () => {
  comments.value = []
  pageNum.value = 1
  threadTotal.value = 0
  commentTotal.value = Number(props.initialCount || 0)
}

const requireLogin = () => {
  if (!userStore.isLoggedIn) {
    userStore.showLogin()
    return false
  }
  return true
}

const normalizeComment = (item) => {
  const replies = Array.isArray(item.replies) ? item.replies.map(normalizeComment) : []
  return {
    ...item,
    userAvatar: getAvatarUrl(item.userAvatar),
    liked: Boolean(item.liked),
    mine: Boolean(item.mine),
    author: Boolean(item.author),
    deleted: Boolean(item.deleted),
    likeCount: Number(item.likeCount || 0),
    replyCount: Number(item.replyCount || 0),
    replyTotal: Number(item.replyCount || 0),
    replyPage: 1,
    replyLoading: false,
    repliesExpanded: false,
    replies
  }
}

const fetchComments = async (reset = false) => {
  if (!props.noteId || loading.value) return
  if (reset) {
    pageNum.value = 1
    comments.value = []
    threadTotal.value = 0
  }

  loading.value = true
  try {
    const res = await getCommentList({
      noteId: props.noteId,
      pageNum: pageNum.value,
      pageSize
    })
    const records = (res?.records || []).map(normalizeComment)
    threadTotal.value = Number(res?.total || records.length)
    if (reset) {
      comments.value = records
    } else {
      const existing = new Set(comments.value.map((item) => item.id))
      comments.value = comments.value.concat(records.filter((item) => !existing.has(item.id)))
    }
    if (records.length > 0) {
      pageNum.value += 1
    }
  } catch (e) {
    console.error('加载评论失败', e)
    ElMessage.error('评论加载失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => props.noteId,
  () => {
    resetState()
    if (props.noteId) {
      fetchComments(true)
    }
  },
  { immediate: true }
)

const loadReplies = async (comment) => {
  if (!comment?.id || comment.replyLoading) return
  comment.replyLoading = true
  try {
    const res = await getCommentReplies(comment.id, {
      pageNum: comment.replyPage || 1,
      pageSize: replyPageSize
    })
    const records = (res?.records || []).map(normalizeComment)
    comment.replyTotal = Number(res?.total || comment.replyCount || records.length)
    const existing = new Set(comment.replies.map((item) => item.id))
    comment.replies = comment.replies.concat(records.filter((item) => !existing.has(item.id)))
    comment.repliesExpanded = true
    if (records.length > 0) {
      comment.replyPage = (comment.replyPage || 1) + 1
    }
  } catch (e) {
    console.error('加载回复失败', e)
    ElMessage.error('回复加载失败')
  } finally {
    comment.replyLoading = false
  }
}

const hasMoreReplies = (comment) => {
  return Number(comment.replyCount || 0) > (comment.replies?.length || 0)
}

const replyMoreLabel = (comment) => {
  if (comment.replyLoading) return '加载中...'
  const remaining = Math.max(0, Number(comment.replyCount || 0) - (comment.replies?.length || 0))
  return `展开 ${remaining} 条回复`
}

const selectReply = (comment) => {
  if (!props.canInteract) {
    ElMessage.warning('当前笔记暂不可互动')
    return
  }
  if (!requireLogin() || comment?.deleted) return
  emit('reply', {
    id: comment.id,
    parentId: comment.parentId,
    userNickname: comment.userNickname,
    content: comment.content
  })
}

const toggleLike = async (comment) => {
  if (!props.canInteract) {
    ElMessage.warning('当前笔记暂不可互动')
    return
  }
  if (!requireLogin() || !comment?.id || comment.deleted) return

  const originalLiked = comment.liked
  const originalCount = Number(comment.likeCount || 0)
  const nextLiked = !originalLiked
  comment.liked = nextLiked
  comment.likeCount = Math.max(0, originalCount + (nextLiked ? 1 : -1))

  try {
    const liked = await toggleCommentLike(comment.id)
    if (Boolean(liked) !== nextLiked) {
      comment.liked = Boolean(liked)
      comment.likeCount = Math.max(0, originalCount + (liked ? 1 : -1))
    }
  } catch (e) {
    comment.liked = originalLiked
    comment.likeCount = originalCount
  }
}

const handleMenu = (command, comment) => {
  if (command === 'delete') {
    handleDelete(comment)
    return
  }
  if (command === 'report') {
    if (!requireLogin()) return
    emit('report', comment)
  }
}

const handleDelete = async (comment) => {
  if (!requireLogin() || !comment?.id) return
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '提示', { type: 'warning' })
    await deleteComment(comment.id)
    applyDeletedComment(comment)
    ElMessage.success('评论已删除')
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      // Request errors are already handled by the global interceptor.
    }
  }
}

const applyDeletedComment = (comment) => {
  if (comment.parentId) {
    commentTotal.value = Math.max(0, commentTotal.value - 1)
    emit('count-change', -1)

    const root = comments.value.find((item) => item.id === comment.parentId)
    if (root) {
      root.replies = root.replies.filter((item) => item.id !== comment.id)
      root.replyCount = Math.max(0, Number(root.replyCount || 0) - 1)
      root.replyTotal = Math.max(0, Number(root.replyTotal || 0) - 1)
    }
    return
  }

  const deletedCount = 1 + Math.max(0, Number(comment.replyCount || 0))
  commentTotal.value = Math.max(0, commentTotal.value - deletedCount)
  emit('count-change', -deletedCount)

  comments.value = comments.value.filter((item) => item.id !== comment.id)
  threadTotal.value = Math.max(0, threadTotal.value - 1)
}

const submitComment = async (content, replyTarget) => {
  if (!props.noteId || !content?.trim()) return false
  if (!requireLogin()) return false

  const parentId = replyTarget ? (replyTarget.parentId || replyTarget.id) : null
  const replyToId = replyTarget ? replyTarget.id : null
  const id = await addComment({
    noteId: props.noteId,
    parentId,
    replyToId,
    content: content.trim()
  })

  const user = userStore.userInfo || {}
  const created = normalizeComment({
    id,
    noteId: props.noteId,
    parentId,
    replyToId,
    userId: user.id,
    content: content.trim(),
    createTime: new Date().toISOString(),
    userNickname: user.nickname || '用户',
    userAvatar: user.avatar,
    mine: true,
    author: String(user.id) === String(props.authorId),
    liked: false,
    likeCount: 0,
    replyCount: 0,
    replies: [],
    replyToNickname: replyTarget?.parentId ? (replyTarget.userNickname || '') : '',
    replyToContent: replyTarget?.parentId ? (replyTarget.content || '') : '',
    deleted: false
  })

  if (parentId) {
    const root = comments.value.find((item) => item.id === parentId)
    if (root) {
      root.replyCount = Number(root.replyCount || 0) + 1
      root.replyTotal = Number(root.replyTotal || 0) + 1
      root.replies = root.replies.concat(created)
    }
  } else {
    comments.value = comments.value.concat(created)
    threadTotal.value += 1
  }

  commentTotal.value += 1
  emit('count-change', 1)
  return true
}

defineExpose({
  reload: () => fetchComments(true),
  submitComment
})
</script>

<style scoped lang="scss">
.comments-section {
  margin-top: 10px;
}

.comments-count {
  font-size: 14px;
  color: #666;
  margin-bottom: 16px;
}

.comments-loading,
.comments-empty {
  padding: 16px 0;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-bottom: 12px;
}

.comment-thread {
  position: relative;
}

.comment-row,
.reply-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.comment-avatar,
.reply-avatar {
  flex: 0 0 auto;
}

.comment-main,
.reply-main {
  flex: 1;
  min-width: 0;
}

.comment-head,
.reply-head {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 20px;
  margin-bottom: 3px;
}

.comment-name {
  color: #8a8a8a;
  font-size: 14px;
  line-height: 20px;
}

.author-badge {
  height: 18px;
  padding: 0 7px;
  border-radius: 999px;
  background: #f1f1f1;
  color: #777;
  font-size: 12px;
  line-height: 18px;
}

.comment-text {
  color: #333;
  font-size: 15px;
  line-height: 1.55;
  word-break: break-word;
}

.reply-target {
  color: #6a6a6a;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 24px;
  margin-top: 6px;
  color: #8a8a8a;
  font-size: 13px;
}

.comment-time {
  color: #999;
}

.comment-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 24px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #666;
  font-size: 13px;
  cursor: pointer;
}

.comment-action .heart-icon {
  width: 17px;
  height: 17px;
  color: #666;
}

.comment-action:hover,
.comment-action:hover .heart-icon {
  color: #ff6b81;
}

.comment-menu {
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #777;
  cursor: pointer;
}

.comment-menu:hover {
  background: #f6f6f6;
}

.comment-menu.is-reply {
  width: 24px;
  height: 24px;
}

.reply-list {
  margin: 12px 0 0 48px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.reply-row {
  gap: 10px;
}

.reply-row .comment-text {
  font-size: 14px;
}

.reply-row .comment-actions {
  margin-top: 4px;
}

.reply-more {
  align-self: flex-start;
  height: 28px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #28528a;
  font-size: 14px;
  cursor: pointer;
}

.reply-more:disabled {
  color: #aaa;
  cursor: default;
}

.deleted-root {
  margin-left: 48px;
  color: #aaa;
  font-size: 14px;
  line-height: 28px;
}

.comment-load-more {
  display: flex;
  justify-content: center;
  margin-top: 10px;
}
</style>
