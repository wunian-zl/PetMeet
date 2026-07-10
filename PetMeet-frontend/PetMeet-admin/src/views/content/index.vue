<template>
  <div class="content-container">
    <ContentToolbar />
    <ContentTable />
    <ContentDialogs />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, watch, provide } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { VideoCamera, Warning, Timer, TrendCharts, CircleCheck, CircleClose, Hide, ChatDotRound, Close } from '@element-plus/icons-vue'
import * as contentApi from '@/api/content'
import { resolveImageUrl } from '@/utils/image'
import { useAdminTable } from '@/composables/useAdminTable'
import UserInfoPopover from '@/components/UserInfoPopover.vue'
import ContentToolbar from './components/ContentToolbar.vue'
import ContentTable from './components/ContentTable.vue'
import ContentDialogs from './components/ContentDialogs.vue'

const router = useRouter()
const filterStatus = ref('all')
const filterType = ref('')
const filterProduct = ref('')
const filterCategory = ref('')
const filterTag = ref('')
const dateRange = ref([])
const searchKeyword = ref('')
const sortOption = ref('default')

// 统计卡片带出的日期类型：create 对应今日新增，audit 对应今日通过/拒绝
const filterDateType = ref('create')

const {
  loading,
  allData,
  tableData,
  currentPage,
  pageSize,
  total,
  runWithLoading,
  resetPage,
  setRows
} = useAdminTable()
const selectedRows = ref([])

// 统计数据
const stats = reactive({
    pending: 0,
    todayNew: 0,
    todayApproved: 0,
    todayRejected: 0
})

// 快捷驳回原因
const quickReasons = ['内容涉嫌广告', '图片引起不适', '内容质量较低', '与宠物无关', '标题党/虚假宣传']

// 驳回弹窗
const rejectDialogVisible = ref(false)
const rejectFormRef = ref(null)
const rejectForm = reactive({
  id: null,
  reason: '',
  isViolation: false,
  violationType: '广告垃圾'
})

// 辅助函数：获取用户违规次数，这里先做简化处理
const getUserViolationCount = (_username) => {
    return 0 // 这里先写死，后面如果要精确统计再补接口
}

const handleQuickReason = (reason) => {
    rejectForm.reason = reason
}

// 详情弹窗
const detailDialogVisible = ref(false)
const currentNote = ref(null)

// 评论列表（管理端视角）
const comments = ref([])
const commentPage = ref(1)
const commentTotal = ref(0)
const commentThreadTotal = ref(0)
const commentMode = ref('preview') // preview -> show a few, full -> paged 20
const commentPreviewSize = 3
const commentFullSize = 20
const replyPageSize = 20
const commentPageSize = computed(() => (commentMode.value === 'preview' ? commentPreviewSize : commentFullSize))
const commentLoading = ref(false)
const commentHasMore = computed(() => comments.value.length < commentThreadTotal.value)
const commentLoadLabel = computed(() => (commentMode.value === 'preview' ? '展开更多' : '加载更多'))

const normalizeCommentRecord = (comment) => {
  const replies = Array.isArray(comment.replies) ? comment.replies : []
  const replyCount = Number(comment.replyCount || replies.length || 0)
  return {
    ...comment,
    replies: replies.slice(),
    previewReplies: replies.slice(),
    replyCount,
    replyExpanded: false,
    replyLoading: false,
    replyPage: 1,
    replyTotal: replyCount
  }
}

const resetCommentState = () => {
  comments.value = []
  commentPage.value = 1
  commentTotal.value = 0
  commentThreadTotal.value = 0
  commentLoading.value = false
  commentMode.value = 'preview'
}

const fetchComments = async (reset = false) => {
  if (!currentNote.value) return
  if (currentNote.value.status !== 'approved') {
    resetCommentState()
    return
  }
  if (reset) {
    commentPage.value = 1
    comments.value = []
    commentTotal.value = currentNote.value.commentCount || 0
  }
  commentLoading.value = true
  try {
    const res = await contentApi.getCommentList({
      noteId: currentNote.value.id,
      pageNum: commentPage.value,
      pageSize: commentPageSize.value
    })
    if (res.code === 200 && res.data) {
      const records = (res.data.records || []).map(normalizeCommentRecord)
      commentThreadTotal.value = res.data.total || records.length
      if (reset) {
        comments.value = records
      } else {
        comments.value = comments.value.concat(records)
      }
    } else {
      ElMessage.error(res.message || res.msg || '加载评论失败')
    }
  } catch (e) {
    console.error('加载评论失败', e)
  } finally {
    commentLoading.value = false
  }
}

const loadMoreComments = () => {
  if (commentLoading.value) return
  if (commentMode.value === 'preview') {
    commentMode.value = 'full'
    fetchComments(true)
    return
  }
  if (!commentHasMore.value) return
  commentPage.value += 1
  fetchComments(false)
}

const reloadComments = () => {
  commentMode.value = 'preview'
  fetchComments(true)
}

const getHiddenReplyCount = (comment) => {
  if (!comment) return 0
  const visibleCount = Array.isArray(comment.replies) ? comment.replies.length : 0
  return Math.max(0, (comment.replyCount || 0) - visibleCount)
}

const getRemainingReplyCount = (comment) => {
  if (!comment) return 0
  const visibleCount = Array.isArray(comment.replies) ? comment.replies.length : 0
  return Math.max(0, (comment.replyTotal || comment.replyCount || 0) - visibleCount)
}

const canLoadMoreReplies = (comment) => {
  return !!comment?.replyExpanded && getRemainingReplyCount(comment) > 0
}

const loadCommentReplies = async (comment, reset = false) => {
  if (!comment || comment.replyLoading) return
  comment.replyLoading = true
  const nextPage = reset ? 1 : (comment.replyPage || 1) + 1
  try {
    const res = await contentApi.getCommentReplies(comment.id, {
      pageNum: nextPage,
      pageSize: replyPageSize
    })
    if (res.code === 200 && res.data) {
      const records = res.data.records || []
      const existingIds = new Set((reset ? [] : (comment.replies || [])).map(item => item.id))
      const nextReplies = reset
        ? records
        : (comment.replies || []).concat(records.filter(item => !existingIds.has(item.id)))

      comment.replies = nextReplies
      comment.replyPage = nextPage
      comment.replyTotal = res.data.total ?? records.length
      comment.replyCount = Math.max(comment.replyCount || 0, comment.replyTotal || 0)
      comment.replyExpanded = true
    } else {
      ElMessage.error(res.message || res.msg || '加载回复失败')
    }
  } catch (e) {
    console.error('加载回复失败', e)
  } finally {
    comment.replyLoading = false
  }
}

const toggleCommentReplies = async (comment) => {
  if (!comment || (comment.replyCount || 0) === 0) return
  if (comment.replyExpanded) {
    comment.replyExpanded = false
    comment.replyPage = 1
    comment.replyTotal = comment.replyCount || 0
    comment.replies = Array.isArray(comment.previewReplies) ? comment.previewReplies.slice() : []
    return
  }
  await loadCommentReplies(comment, true)
}

const loadMoreCommentReplies = (comment) => {
  if (!canLoadMoreReplies(comment)) return
  loadCommentReplies(comment, false)
}

const removeReplyFromParent = (parentComment, reply) => {
  parentComment.replies = (parentComment.replies || []).filter(item => item.id !== reply.id)
  parentComment.previewReplies = (parentComment.previewReplies || []).filter(item => item.id !== reply.id)
  parentComment.replyCount = Math.max(0, (parentComment.replyCount || 0) - 1)
  parentComment.replyTotal = Math.max(0, (parentComment.replyTotal || 0) - 1)
}

const handleDeleteComment = (comment, parentComment = null) => {
  ElMessageBox.confirm('确定删除评论吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    const res = await contentApi.deleteComment(comment.id)
    if (res.code !== 200) {
      ElMessage.error(res.message || res.msg || '删除评论失败')
      return
    }
    const deletedCount = parentComment ? 1 : 1 + (comment.replyCount || 0)
    if (parentComment) {
      removeReplyFromParent(parentComment, comment)
    } else {
      comments.value = comments.value.filter(item => item.id !== comment.id)
      commentThreadTotal.value = Math.max(0, commentThreadTotal.value - 1)
    }
    commentTotal.value = Math.max(0, commentTotal.value - deletedCount)
    if (currentNote.value) {
      currentNote.value.commentCount = Math.max(0, (currentNote.value.commentCount || 0) - deletedCount)
    }
    ElMessage.success('评论已删除')
  })
}

// 点击图片本体时顺手关闭预览器
const closeViewerOnImgClick = (e) => {
    if (e.target.classList.contains('el-image-viewer__img')) {
        const closeBtn = document.querySelector('.el-image-viewer__close')
        if (closeBtn) closeBtn.click()
    }
}

// 分页
onMounted(async () => {
  document.addEventListener('click', closeViewerOnImgClick)
  await loadNoteList()
  await loadStats()
})

const loadNoteList = async () => {
    await runWithLoading(async () => {
        const statusMap = {
            all: undefined,
            pending: 0,
            approved: 1,
            shielded: 2,
            rejected: 3,
            user_off_shelf: 4,
            user_deleted: 5,
            admin_soft_deleted: 6
        }
        const params = {
            pageNum: currentPage.value,
            pageSize: pageSize.value,
            status: statusMap[filterStatus.value],
            keyword: searchKeyword.value || undefined,
            category: filterCategory.value || undefined,
            tag: filterTag.value || undefined
        }
        const res = await contentApi.getNoteList(params)
        if (res.code === 200 && res.data) {
            setRows((res.data.records || []).map(mapNoteFromApi), res.data.total)
            filterData()
        } else {
            ElMessage.error(res.message || res.msg || '加载内容列表失败')
        }
    }).catch((e) => {
        console.error('加载内容列表失败', e)
    })
}

const loadStats = async () => {
    try {
        const res = await contentApi.getNoteStats()
        if (res.code === 200 && res.data) {
            stats.pending = res.data.pendingCount || 0
            stats.todayNew = res.data.todayCount || 0
            stats.todayApproved = res.data.todayApprovedCount ?? res.data.publishedCount ?? 0
            stats.todayRejected = res.data.todayRejectedCount ?? res.data.rejectedCount ?? 0
        }
    } catch (e) {
        console.error('加载统计失败', e)
    }
}

onUnmounted(() => {
  document.removeEventListener('click', closeViewerOnImgClick)
})

const formatDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

const mapStatusKey = (status) => {
  if (status === 0) return 'pending'
  if (status === 1) return 'approved'
  if (status === 2) return 'shielded'
  if (status === 3) return 'rejected'
  if (status === 4) return 'user_off_shelf'
  if (status === 5) return 'user_deleted'
  if (status === 6) return 'admin_soft_deleted'
  return 'pending'
}

const normalizeText = (val) => {
  if (!val) return ''
  return String(val)
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .replace(/\r\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

const normalizeType = (val) => {
  const t = String(val || 'image').toLowerCase()
  if (t === 'video') return 'video'
  if (t === 'mixed') return 'mixed'
  return 'image_text'
}

const normalizeImages = (val, cover) => {
  let raw = []
  if (Array.isArray(val)) {
    raw = val
  } else if (typeof val === 'string' && val.trim()) {
    try {
      raw = JSON.parse(val)
    } catch {
      raw = []
    }
  }

  const images = raw
    .filter(Boolean)
    .map(item => String(item).trim())
    .filter(Boolean)

  if (images.length === 0 && cover) {
    images.push(cover)
  }

  return [...new Set(images)]
}

const mapNoteFromApi = (note) => {
  const statusKey = mapStatusKey(note.status)
  const createTime = formatDateTime(note.createTime)
  const auditTime = formatDateTime(note.auditTime)
  const auditTimeDisplay = auditTime || ((statusKey === 'approved' || statusKey === 'rejected') ? createTime : '')
  const auditIsFallback = !auditTime && !!auditTimeDisplay && (statusKey === 'approved' || statusKey === 'rejected')
  const authorUsername = note.username || ''
  const authorNickname = note.nickname || ''

  return {
    id: note.id,
    title: note.title,
    category: note.category || '',
    tags: note.tags ? note.tags.split(',').map(t => t.trim()).filter(Boolean) : [],
    cover: note.cover,
    images: normalizeImages(note.images, note.cover),
    content: normalizeText(note.content),
    userId: note.userId,
    author: authorUsername || authorNickname || '用户',
    authorUsername,
    authorNickname,
    avatar: note.userAvatar || '',
    type: normalizeType(note.type),
    videoUrl: note.videoUrl,
    status: statusKey,
    isShielded: note.status === 2,
    isSticky: !!note.isSticky,
    isRecommended: !!note.isRecommended,
    likes: note.likeCount || 0,
    views: 0,
    productCount: note.productCount ?? (note.products ? note.products.length : 0),
    products: note.products || [],
    commentCount: note.commentCount || 0,
    createTime,
    auditTime,
    auditTimeDisplay,
    auditIsFallback,
    auditor: note.auditUserName || (statusKey !== 'pending' ? '系统' : ''),
    rejectReason: normalizeText(note.rejectReason)
  }
}

const buildAuditTitle = (note, kind) => {
  if (!note) return ''
  const base = kind === 'approved' ? '审核通过' : '已拒绝'
  const time = note.auditTime || note.auditTimeDisplay
  const timePart = time ? ` (${time})` : ''
  const operator = note.auditor || '系统'
  const opPart = operator ? ` - 操作人: ${operator}` : ''
  const fallback = note.auditIsFallback ? '（历史数据）' : ''
  return `${base}${timePart}${opPart}${fallback}`
}

const filterData = () => {
  let res = allData.value

  // 筛选条件
  
  if (filterStatus.value === 'all') {
    // “全部”视图里默认不展示已删除内容。
    res = res.filter(item => item.status !== 'user_deleted' && item.status !== 'admin_soft_deleted')
  } else {
    res = res.filter(item => item.status === filterStatus.value)
  }
  
  if (filterType.value) {
      res = res.filter(item => item.type === filterType.value)
  }
  
  if (filterProduct.value) {
      if (filterProduct.value === 'has') {
          res = res.filter(item => item.productCount > 0)
      } else if (filterProduct.value === 'none') {
          res = res.filter(item => item.productCount === 0)
      } else if (filterProduct.value === 'approved_has') {
          // “已通过且挂商品”这个筛选要同时满足两个条件
          res = res.filter(item => item.status === 'approved' && item.productCount > 0)
      }
  }
  
  if (dateRange.value && dateRange.value.length === 2) {
      const start = dayjs(dateRange.value[0]).startOf('day').valueOf()
      const end = dayjs(dateRange.value[1]).endOf('day').valueOf()
      
      res = res.filter(item => {
          // 已通过/已拒绝按 auditTime 算，其余情况按 createTime 算
          const timeField = filterDateType.value === 'audit'
            ? (item.auditTime || item.createTime)
            : item.createTime
          if (!timeField) return false
          const time = dayjs(timeField).valueOf()
          return time >= start && time <= end
      })
  }

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    const includesKeyword = (text) => String(text || '').toLowerCase().includes(keyword)
    res = res.filter(item => 
      includesKeyword(item.title) ||
      includesKeyword(item.author) ||
      includesKeyword(item.authorUsername) ||
      includesKeyword(item.authorNickname) ||
      String(item.id).includes(keyword)
    )
  }
  
  // 排序
  if (sortOption.value === 'time_asc') {
     res.sort((a, b) => new Date(a.createTime) - new Date(b.createTime))
  } else if (sortOption.value === 'product_desc') {
     res.sort((a, b) => b.productCount - a.productCount)
  } else if (sortOption.value === 'hot_desc') {
      res.sort((a, b) => (b.views + b.likes) - (a.views + a.likes))
  } else if (sortOption.value === 'likes_desc') {
      res.sort((a, b) => b.likes - a.likes)
  } else {
     // 默认先看置顶（仅已通过内容），再按时间倒序
     res.sort((a, b) => {
         const aSticky = (a.status === 'approved' && !a.isShielded && a.isSticky) ? 1 : 0
         const bSticky = (b.status === 'approved' && !b.isShielded && b.isSticky) ? 1 : 0
         if (aSticky !== bSticky) return bSticky - aSticky
         
         return new Date(b.createTime) - new Date(a.createTime)
     })
  }

  tableData.value = res
}

const handleStatsClick = (type) => {
    const todayStr = dayjs().format('YYYY-MM-DD')
    
    if (type === 'pending') {
        filterStatus.value = 'pending'
        dateRange.value = []
        filterDateType.value = 'create' // Reset
    } else if (type === 'todayNew') {
        filterStatus.value = 'all'
        dateRange.value = [todayStr, todayStr]
        filterDateType.value = 'create' // Today New uses createTime
    } else if (type === 'todayApproved') {
        filterStatus.value = 'approved'
        dateRange.value = [todayStr, todayStr]
        filterDateType.value = 'audit' // Today Approved uses auditTime
    } else if (type === 'todayRejected') {
        filterStatus.value = 'rejected'
        dateRange.value = [todayStr, todayStr]
        filterDateType.value = 'audit' // Today Rejected uses auditTime
    }
    
    handleFilter()
}

const handleFilter = () => {
  resetPage()
  loadNoteList()
}

// 时间快捷筛选
const setLast7Days = () => {
    const end = new Date()
    const start = new Date()
    start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
    // 格式化成 YYYY-MM-DD
    const formatDate = (d) => d.toISOString().split('T')[0]
    dateRange.value = [formatDate(start), formatDate(end)]
    handleFilter()
    ElMessage.success('已筛选最近7天数据')
}

const goToProduct = (productId) => {
    router.push({
        path: '/admin/product',
        query: { productId: productId }
    })
    detailDialogVisible.value = false
    ElMessage.success(`正在跳转到商品 ID: ${productId}`)
}

const resetFilters = () => {
    filterStatus.value = 'all'
    filterType.value = ''
    filterProduct.value = ''
    filterCategory.value = ''
    filterTag.value = ''
    dateRange.value = []
    searchKeyword.value = ''
    sortOption.value = 'default'
    filterDateType.value = 'create' // 重置回默认的创建时间筛选
    handleFilter()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadNoteList()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  resetPage()
  loadNoteList()
}

// 批量操作
const handleSelectionChange = (val) => {
  selectedRows.value = val
}

const handleBatchCommand = (command) => {
    const ids = selectedRows.value.map(row => row.id)
    if (command === 'pass') {
        const allowPass = selectedRows.value.filter(row => row.status === 'pending' || row.status === 'rejected')
        if (allowPass.length === 0) {
            return ElMessage.warning('仅待审核/已拒绝内容支持批量通过')
        }
        ElMessageBox.confirm(`确定批量通过这 ${allowPass.length} 条内容吗?`, '提示', { type: 'success' })
        .then(async () => {
            const res = await contentApi.batchNoteAction('approve', allowPass.map(r => r.id))
            if (res.code !== 200) {
                ElMessage.error(res.message || res.msg || '批量通过失败')
                return
            }
            ElMessage.success('批量通过成功')
            loadNoteList()
            loadStats()
        })
    } else if (command === 'reject') {
         ElMessageBox.prompt('请输入拒绝原因', '批量拒绝', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputPattern: /\S+/,
            inputErrorMessage: '原因不能为空'
          }).then(({ value }) => {
            Promise.all(ids.map(id => contentApi.rejectNote(id, value)))
              .then(() => {
                ElMessage.warning('批量拒绝完成')
                loadNoteList()
                loadStats()
              })
          })
    } else if (command === 'shield') {
        const allowDown = selectedRows.value.filter(row => row.status === 'approved')
        if (allowDown.length === 0) {
            return ElMessage.warning('仅已通过的内容支持批量下架')
        }
        ElMessageBox.confirm(`确定批量下架这 ${allowDown.length} 条内容吗？下架后前台不可见。`, '批量下架', {
            type: 'warning'
        }).then(() => {
            contentApi.batchNoteAction('shield', allowDown.map(r => r.id)).then((res) => {
                if (res.code !== 200) {
                    ElMessage.error(res.message || res.msg || '批量下架失败')
                    return
                }
                ElMessage.success('批量下架成功')
                loadNoteList()
                loadStats()
            })
        })
    } else if (command === 'softDelete') {
        const allowSoftDelete = selectedRows.value.filter(row => row.status !== 'admin_soft_deleted')
        if (allowSoftDelete.length === 0) {
            return ElMessage.warning('选中的内容已是删除状态')
        }
        ElMessageBox.confirm(`确定批量删除这 ${allowSoftDelete.length} 条内容吗？`, '批量删除', {
            type: 'warning'
        }).then(() => {
            contentApi.batchNoteAction('softDelete', allowSoftDelete.map(r => r.id)).then((res) => {
                if (res.code !== 200) {
                    ElMessage.error(res.message || res.msg || '批量删除失败')
                    return
                }
                ElMessage.success('批量删除成功')
                loadNoteList()
                loadStats()
            })
        })
    }
}


const getStatusType = (status) => {
  const map = {
    pending: 'warning',
    approved: 'success',
    shielded: 'info',
    rejected: 'danger',
    user_off_shelf: 'warning',
    user_deleted: 'danger',
    admin_soft_deleted: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    pending: '待审核',
    approved: '已通过',
    shielded: '管理员下架',
    rejected: '已拒绝',
    user_off_shelf: '用户下架',
    user_deleted: '用户删除',
    admin_soft_deleted: '删除'
  }
  return map[status] || status
}

const handleStickyChange = (row) => {
    if (!row.isSticky) {
        // 关闭置顶
        ElMessageBox.confirm('确定取消该内容的置顶推荐吗？', '取消置顶', {
            type: 'warning'
        }).then(() => {
            contentApi.toggleSticky(row.id).then((res) => {
                if (res.code !== 200) {
                    ElMessage.error(res.message || res.msg || '取消置顶失败')
                    row.isSticky = true
                    return
                }
                row.isSticky = !!res.data
                ElMessage.info('已取消置顶')
            })
        }).catch(() => {
            row.isSticky = true // 用户取消后把开关拨回去
        })
    } else {
        // 开启置顶
        contentApi.toggleSticky(row.id).then((res) => {
            if (res.code !== 200) {
                ElMessage.error(res.message || res.msg || '置顶失败')
                row.isSticky = false
                return
            }
            row.isSticky = !!res.data
            ElMessage.success('已设为置顶')
        })
    }
}

const handleRecommendChange = (row) => {
    if (!row.isRecommended) {
        // 关闭推荐
        contentApi.toggleRecommend(row.id).then((res) => {
            if (res.code !== 200) {
                ElMessage.error(res.message || res.msg || '取消推荐失败')
                row.isRecommended = true
                return
            }
            row.isRecommended = !!res.data
            ElMessage.info('已取消推荐')
        })
    } else {
        // 开启推荐
        contentApi.toggleRecommend(row.id).then((res) => {
            if (res.code !== 200) {
                ElMessage.error(res.message || res.msg || '推荐失败')
                row.isRecommended = false
                return
            }
            row.isRecommended = !!res.data
            ElMessage.success('已设为推荐')
        })
    }
}

const openDetailDialog = (row) => {
  contentApi.getNoteDetail(row.id).then((res) => {
      if (res.code === 200 && res.data) {
          currentNote.value = mapNoteFromApi(res.data)
          resetCommentState()
          commentTotal.value = currentNote.value.commentCount || 0
          if (currentNote.value.status === 'approved') {
            fetchComments(true)
          } else {
            resetCommentState()
          }
          detailDialogVisible.value = true
      } else {
          ElMessage.error(res.message || res.msg || '加载内容详情失败')
      }
  }).catch((e) => {
      console.error('加载内容详情失败', e)
  })
}

watch(detailDialogVisible, (visible) => {
  if (!visible) {
    resetCommentState()
    currentNote.value = null
  }
})

const handleApprove = (row) => {
  ElMessageBox.confirm(`确定通过 "${row.title}" 吗？`, '审核通过', {
    confirmButtonText: '通过',
    cancelButtonText: '取消',
    type: 'success'
  }).then(() => {
    contentApi.approveNote(row.id).then((res) => {
        if (res.code !== 200) {
            ElMessage.error(res.message || res.msg || '审核失败')
            return
        }
        ElMessage.success('审核通过')
        loadNoteList()
        loadStats()
    })
  })
}

const openRejectDialog = (row) => {
  rejectForm.id = row.id
  rejectForm.reason = ''
  rejectForm.isViolation = false
  rejectForm.violationType = '广告垃圾'
  rejectDialogVisible.value = true
}

const confirmReject = () => {
  rejectFormRef.value.validate((valid) => {
    if (valid) {
      contentApi.rejectNote(rejectForm.id, rejectForm.reason).then((res) => {
          if (res.code !== 200) {
              ElMessage.error(res.message || res.msg || '拒绝失败')
              return
          }
          ElMessage.warning('内容已拒绝')
          rejectDialogVisible.value = false
          loadNoteList()
          loadStats()
      })
    }
  })
}

// 单条内容的下架/恢复
const handleToggleShield = (row) => {
    if (row.status !== 'approved' && row.status !== 'shielded') return

    const doToggle = async (reason) => {
        const res = await contentApi.toggleShield(row.id, reason)
        if (res.code !== 200) {
            ElMessage.error(res.message || res.msg || '操作失败')
            return
        }
        row.isShielded = !!res.data
        if (row.isShielded && row.isSticky) {
            row.isSticky = false
        }
        ElMessage.success(row.isShielded ? '内容已下架' : '内容已恢复上架')
        loadNoteList()
        loadStats()
    }

    if (row.isShielded) {
        ElMessageBox.confirm(
            '恢复上架后，前台将重新展示该内容。确定恢复？',
            '恢复上架',
            { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' }
        ).then(() => doToggle()).catch(() => {})
        return
    }

    ElMessageBox.prompt(
        '可选：请输入下架原因（会通知作者）',
        '下架内容',
        {
            confirmButtonText: '确认下架',
            cancelButtonText: '取消',
            inputType: 'textarea',
            inputPlaceholder: '例如：涉嫌侵权/盗用、广告营销、低俗内容等（可不填）'
        }
    ).then(({ value }) => doToggle(value || undefined)).catch(() => {})
}

const handleSoftDelete = (row) => {
    if (!row || row.status === 'admin_soft_deleted') return
    ElMessageBox.prompt(
        '可选：请输入删除原因（会通知作者）',
        '删除内容',
        {
            confirmButtonText: '确认删除',
            cancelButtonText: '取消',
            inputType: 'textarea',
            inputPlaceholder: '例如：严重违规、侵权投诉成立等（可不填）'
        }
    ).then(async ({ value }) => {
        const res = await contentApi.softDeleteNote(row.id, value || undefined)
        if (res.code !== 200) {
            ElMessage.error(res.message || res.msg || '删除失败')
            return
        }
        ElMessage.success('已删除')
        loadNoteList()
        loadStats()
    }).catch(() => {})
}


provide('adminContentPageContext', {
    filterStatus,
    filterType,
    filterProduct,
    filterCategory,
    filterTag,
    dateRange,
    searchKeyword,
    sortOption,
    loading,
    tableData,
    selectedRows,
    stats,
    quickReasons,
    rejectDialogVisible,
    rejectFormRef,
    rejectForm,
    getUserViolationCount,
    handleQuickReason,
    detailDialogVisible,
    currentNote,
    comments,
    commentTotal,
    commentLoading,
    commentHasMore,
    commentLoadLabel,
    loadMoreComments,
    reloadComments,
    getHiddenReplyCount,
    getRemainingReplyCount,
    canLoadMoreReplies,
    toggleCommentReplies,
    loadMoreCommentReplies,
    handleDeleteComment,
    currentPage,
    pageSize,
    total,
    formatDateTime,
    buildAuditTitle,
    handleStatsClick,
    handleFilter,
    setLast7Days,
    goToProduct,
    resetFilters,
    handlePageChange,
    handleSizeChange,
    handleSelectionChange,
    handleBatchCommand,
    getStatusType,
    getStatusText,
    handleStickyChange,
    handleRecommendChange,
    openDetailDialog,
    handleApprove,
    openRejectDialog,
    confirmReject,
    handleToggleShield,
    handleSoftDelete,
    UserInfoPopover,
    VideoCamera,
    Warning,
    Timer,
    TrendCharts,
    CircleCheck,
    CircleClose,
    Hide,
    ChatDotRound,
    Close,
    resolveImageUrl
})

</script>

<style scoped>
.content-container {
  font-size: 14px;
}
</style>
