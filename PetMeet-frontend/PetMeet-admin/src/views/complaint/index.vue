<template>
  <div class="complaint-container">
    <ComplaintToolbar />
    <ComplaintTable />
    <ComplaintDialogs />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick, provide } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getComplaintList, updateComplaintStatus, deleteComplaint, batchDeleteComplaints } from '@/api/complaint'
import { getNoteDetail, getCommentList, getCommentReplies } from '@/api/content'
import { resolveImageUrl } from '@/utils/image'
import { useAdminTable } from '@/composables/useAdminTable'
import UserInfoPopover from '@/components/UserInfoPopover.vue'
import ComplaintToolbar from './components/ComplaintToolbar.vue'
import ComplaintTable from './components/ComplaintTable.vue'
import ComplaintDialogs from './components/ComplaintDialogs.vue'

const keyword = ref('')
const statusFilter = ref('')
const {
  loading,
  tableData,
  currentPage,
  pageSize,
  total,
  runWithLoading,
  resetPage,
  setRows
} = useAdminTable()
const selectedRows = ref([])

const noteDialogVisible = ref(false)
const noteDetailLoading = ref(false)
const noteDetail = ref(null)
const noteDetailError = ref('')
const noteComments = ref([])
const noteCommentsLoading = ref(false)
const noteCommentPage = ref(1)
const noteCommentPageSize = 20
const noteCommentActivePageSize = ref(noteCommentPageSize)
const noteCommentTotal = ref(0)
const noteCommentThreadTotal = ref(0)
const noteCommentError = ref('')
const noteHighlightCommentId = ref(null)
const noteHighlightParentId = ref(null)
const noteHighlightActive = ref(false)

const complaintDialogVisible = ref(false)
const currentComplaint = ref(null)

const formatDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

const statusText = (status) => {
  if (status === 1) return '已处理'
  if (status === 2) return '已驳回'
  return '待处理'
}

const statusType = (status) => {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'warning'
}

const noteTags = computed(() => {
  const val = noteDetail.value?.tags
  if (!val) return []
  return String(val)
    .split(',')
    .map((t) => t.trim())
    .filter(Boolean)
})

const normalizeId = (value) => {
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : null
}

const isCommentComplaint = (row) => row?.targetType === 'comment' || !!row?.commentId

const complaintTargetText = (row) => isCommentComplaint(row) ? '评论举报' : '笔记投诉'

const commentKindText = (row) => row?.commentParentId ? '楼中楼回复' : '一级评论'

const evidenceImagesOf = (row) => {
  return Array.isArray(row?.evidenceImages) ? row.evidenceImages.filter(Boolean) : []
}

const resolvedEvidenceImages = (row) => {
  return evidenceImagesOf(row).map((url) => resolveImageUrl(url))
}

const canDeleteComplaint = (row) => [1, 2].includes(Number(row?.status))

const noteCommentHasMore = computed(() => noteComments.value.length < noteCommentThreadTotal.value)

const deletableSelectedIds = computed(() => {
  return selectedRows.value
    .filter((row) => canDeleteComplaint(row))
    .map((row) => normalizeId(row?.id))
    .filter(Boolean)
})

const resetNoteCommentState = () => {
  noteComments.value = []
  noteCommentsLoading.value = false
  noteCommentPage.value = 1
  noteCommentActivePageSize.value = noteCommentPageSize
  noteCommentTotal.value = 0
  noteCommentThreadTotal.value = 0
  noteCommentError.value = ''
  noteHighlightCommentId.value = null
  noteHighlightParentId.value = null
  noteHighlightActive.value = false
}

const commentDisplayName = (comment) => comment?.userNickname || '已注销用户'

const isTargetComment = (comment) => {
  return !!noteHighlightCommentId.value && Number(comment?.id) === Number(noteHighlightCommentId.value)
}

const fetchNoteCommentPage = async (pageNum, pageSize) => {
  const res = await getCommentList({
    noteId: noteDetail.value.id,
    pageNum,
    pageSize
  })
  if (res.code !== 200 || !res.data) {
    throw new Error(res.message || res.msg || '加载评论失败')
  }
  return {
    records: res.data.records || [],
    total: Number(res.data.total || 0)
  }
}

const findLoadedComment = (commentId) => {
  const id = normalizeId(commentId)
  if (!id) return null
  for (const root of noteComments.value) {
    if (Number(root.id) === id) return root
    const reply = (root.replies || []).find((item) => Number(item.id) === id)
    if (reply) return reply
  }
  return null
}

const hydrateTargetReplies = async (parentId, targetId) => {
  const rootId = normalizeId(parentId)
  if (!rootId) return
  const root = noteComments.value.find((item) => Number(item.id) === rootId)
  if (!root) return

  const pageSize = 100
  let pageNum = 1
  let allReplies = []
  let total = 0
  let foundTarget = false

  do {
    const res = await getCommentReplies(rootId, { pageNum, pageSize })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.message || res.msg || '加载评论回复失败')
    }
    const records = res.data.records || []
    total = Number(res.data.total || records.length)
    allReplies = allReplies.concat(records)
    foundTarget = !targetId || allReplies.some((item) => Number(item.id) === Number(targetId))
    pageNum += 1
  } while (!foundTarget && allReplies.length < total)

  root.replies = allReplies
  root.replyCount = Math.max(Number(root.replyCount || 0), total, allReplies.length)
}

const scrollToTargetComment = async () => {
  const targetId = noteHighlightCommentId.value
  if (!targetId) return
  await nextTick()
  const targetEl = document.querySelector(`.note-detail-dialog [data-comment-id="${targetId}"]`)
  if (!targetEl) {
    noteCommentError.value = '已打开笔记，但未在当前评论列表中定位到被投诉评论'
    return
  }

  targetEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
  noteHighlightActive.value = false
  await nextTick()
  window.setTimeout(() => {
    noteHighlightActive.value = true
  }, 60)
}

const loadNoteComments = async ({ reset = false, targetMode = false } = {}) => {
  if (!noteDetail.value?.id) return
  if (reset) {
    noteComments.value = []
    noteCommentPage.value = 1
    noteCommentActivePageSize.value = targetMode ? 100 : noteCommentPageSize
    noteCommentError.value = ''
  }

  noteCommentsLoading.value = true
  let shouldScroll = false
  try {
    const targetId = noteHighlightCommentId.value
    const targetRootId = noteHighlightParentId.value || targetId
    const pageSize = noteCommentActivePageSize.value
    let pageNum = noteCommentPage.value
    let records = reset ? [] : [...noteComments.value]
    let total = noteCommentThreadTotal.value
    let foundTargetRoot = !targetMode || !targetRootId

    do {
      const page = await fetchNoteCommentPage(pageNum, pageSize)
      total = page.total
      records = records.concat(page.records)
      foundTargetRoot = !targetMode || !targetRootId || records.some((item) => Number(item.id) === Number(targetRootId))
      pageNum += 1
    } while (targetMode && !foundTargetRoot && records.length < total)

    noteComments.value = records
    noteCommentPage.value = pageNum - 1
    noteCommentThreadTotal.value = total
    noteCommentTotal.value = Number(noteDetail.value.commentCount || total || records.length)

    if (targetMode && noteHighlightParentId.value) {
      await hydrateTargetReplies(noteHighlightParentId.value, targetId)
    }

    shouldScroll = targetMode && !!targetId
    if (shouldScroll && !findLoadedComment(targetId)) {
      noteCommentError.value = '已打开笔记，但被投诉评论可能已删除或不在当前评论线程中'
    }
  } catch (e) {
    noteCommentError.value = e?.message || '加载评论失败'
  } finally {
    noteCommentsLoading.value = false
  }

  if (shouldScroll) {
    await scrollToTargetComment()
  }
}

const loadMoreNoteComments = async () => {
  if (noteCommentsLoading.value || !noteCommentHasMore.value) return
  noteCommentPage.value += 1
  await loadNoteComments({ reset: false, targetMode: false })
}

const resetNoteDialog = () => {
  noteDetailLoading.value = false
  noteDetail.value = null
  noteDetailError.value = ''
  resetNoteCommentState()
}

const openNoteDialog = async (noteId, targetComplaint = null) => {
  if (!noteId) {
    ElMessage.warning('未找到笔记ID')
    return
  }

  resetNoteCommentState()
  if (targetComplaint && isCommentComplaint(targetComplaint)) {
    noteHighlightCommentId.value = normalizeId(targetComplaint.commentId)
    noteHighlightParentId.value = normalizeId(targetComplaint.commentParentId)
  }

  noteDialogVisible.value = true
  noteDetailLoading.value = true
  noteDetail.value = null
  noteDetailError.value = ''

  try {
    const res = await getNoteDetail(noteId)
    if (res.code === 200 && res.data) {
      noteDetail.value = res.data
      noteDetailLoading.value = false
      await loadNoteComments({ reset: true, targetMode: !!noteHighlightCommentId.value })
      return
    }
    noteDetailError.value = res.message || res.msg || '加载笔记内容失败'
  } catch (e) {
    noteDetailError.value = e?.message || '加载笔记内容失败'
  } finally {
    noteDetailLoading.value = false
  }
}

const oneLineText = (val) => {
  if (!val) return ''
  return String(val).replace(/\s+/g, ' ').trim()
}

const openComplaintDialog = (row) => {
  currentComplaint.value = row || null
  complaintDialogVisible.value = true
}

const resetComplaintDialog = () => {
  currentComplaint.value = null
}

const loadList = async () => {
  await runWithLoading(async () => {
    const res = await getComplaintList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      status: statusFilter.value === '' ? undefined : statusFilter.value,
      keyword: keyword.value ? keyword.value.trim() : undefined
    })
    if (res.code === 200 && res.data) {
      const records = res.data.records || []
      setRows(records.map((item) => ({
        ...item,
        createTime: formatDateTime(item.createTime),
        handleTime: formatDateTime(item.handleTime),
        feedbackTime: formatDateTime(item.feedbackTime)
      })), res.data.total)
    } else {
      ElMessage.error(res.message || res.msg || 'Load failed')
    }
  }, () => {
    selectedRows.value = []
  }).catch((e) => {
    console.error('加载投诉列表失败', e)
  })
}

const handleFilter = () => {
  resetPage()
  loadList()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadList()
}

const handleSelectionChange = (rows) => {
  selectedRows.value = Array.isArray(rows) ? rows : []
}

const handleDelete = async (row) => {
  const id = normalizeId(row?.id)
  if (!id) return
  if (!canDeleteComplaint(row)) {
    ElMessage.warning('仅已处理或已驳回投诉可删除')
    return
  }

  try {
    await ElMessageBox.confirm('删除后记录仅在管理端隐藏，确定继续？', '删除投诉记录', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    const res = await deleteComplaint(id)
    if (res.code !== 200) {
      ElMessage.error(res.message || res.msg || '删除失败')
      return
    }
    ElMessage.success('删除成功')
    loadList()
  } catch {}
}

const handleBatchDelete = async () => {
  const ids = deletableSelectedIds.value
  if (ids.length === 0) {
    ElMessage.warning('请先选择可删除的投诉记录')
    return
  }

  const selectedCount = selectedRows.value.length
  const skippedCount = selectedCount - ids.length
  const tip = skippedCount > 0
    ? `已选 ${selectedCount} 项，其中 ${skippedCount} 项状态不可删，将删除 ${ids.length} 项。确定继续？`
    : `确定批量删除已选的 ${ids.length} 条投诉记录吗？`

  try {
    await ElMessageBox.confirm(tip, '批量删除投诉记录', {
      type: 'warning',
      confirmButtonText: '批量删除',
      cancelButtonText: '取消'
    })
    const res = await batchDeleteComplaints(ids)
    if (res.code !== 200) {
      ElMessage.error(res.message || res.msg || '批量删除失败')
      return
    }
    ElMessage.success('批量删除成功')
    loadList()
  } catch {}
}

const updateStatus = async (row, status) => {
  const actionText = status === 1 ? '处理' : '驳回'
  try {
    const { value } = await ElMessageBox.prompt(
      status === 1 ? '可选：请输入处理说明（会通知投诉人）' : '可选：请输入驳回说明（会通知投诉人）',
      `确认${actionText}`,
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: status === 1 ? '例如：已下架该笔记/已警告作者等（可不填）' : '例如：证据不足/不符合投诉条件等（可不填）'
      }
    )
    const res = await updateComplaintStatus(row.id, status, value || undefined)
    if (res.code !== 200) {
      ElMessage.error(res.message || res.msg || 'Update failed')
      return
    }
    ElMessage.success('操作成功')
    loadList()
    return true
  } catch {
    // 用户取消了操作
    return false
  }
}

onMounted(() => {
  loadList()
})
provide('adminComplaintPageContext', {
    keyword,
    statusFilter,
    loading,
    tableData,
    currentPage,
    pageSize,
    total,
    selectedRows,
    noteDialogVisible,
    noteDetailLoading,
    noteDetail,
    noteDetailError,
    noteComments,
    noteCommentsLoading,
    noteCommentTotal,
    noteCommentError,
    noteHighlightCommentId,
    noteHighlightActive,
    complaintDialogVisible,
    currentComplaint,
    formatDateTime,
    statusText,
    statusType,
    noteTags,
    isCommentComplaint,
    complaintTargetText,
    commentKindText,
    evidenceImagesOf,
    resolvedEvidenceImages,
    canDeleteComplaint,
    noteCommentHasMore,
    deletableSelectedIds,
    commentDisplayName,
    isTargetComment,
    loadMoreNoteComments,
    resetNoteDialog,
    openNoteDialog,
    oneLineText,
    openComplaintDialog,
    resetComplaintDialog,
    handleFilter,
    handlePageChange,
    handleSelectionChange,
    handleDelete,
    handleBatchDelete,
    updateStatus,
    UserInfoPopover,
    resolveImageUrl
})

</script>

<style scoped>
.table-tip {
  margin-bottom: 12px;
}
.filter-card {
  margin-bottom: 16px;
}
.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
.filter-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}
.selected-hint {
  color: #606266;
  font-size: 13px;
}
.pagination-bar {
  margin-top: 15px;
  display: flex;
  justify-content: flex-end;
}
.expand-panel {
  padding: 12px 16px;
}
.expand-pre {
  white-space: pre-wrap;
  word-break: break-word;
}
:deep(.el-descriptions__content) {
  word-break: break-word;
}
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.user-name {
  font-size: 13px;
  color: #303133;
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.user-link {
  display: inline-block;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--admin-professional-primary);
  cursor: pointer;
}

.note-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  width: 100%;
}

.note-cell :deep(.el-tag) {
  flex: 0 0 auto;
}

.note-link {
  min-width: 0;
  flex: 1 1 auto;
  justify-content: flex-start;
  overflow: hidden;
}

.note-link :deep(.el-link__inner) {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-title-ellipsis {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.note-detail-dialog :deep(.el-dialog__body) {
  max-height: calc(100vh - 220px);
  overflow: auto;
}

.note-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.note-detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.note-author {
  display: flex;
  align-items: center;
  gap: 10px;
}

.note-author-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.note-author-name {
  font-weight: 600;
  color: #303133;
}

.note-time {
  font-size: 12px;
  color: #909399;
}

.note-title-full {
  margin: 0;
  font-size: 18px;
  line-height: 1.4;
  color: #303133;
}

.note-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.note-media {
  border-radius: 8px;
  overflow: hidden;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
}

.note-image {
  width: 100%;
  height: 360px;
  background: #000;
}

.note-video {
  width: 100%;
  max-height: 360px;
  background: #000;
}

.note-text {
  white-space: pre-wrap;
  word-break: break-word;
  color: #303133;
  line-height: 1.6;
}

.note-comments-panel {
  margin-top: 6px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.note-comments-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.note-comments-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  color: #303133;
  font-weight: 700;
}

.note-comments-title em {
  color: #909399;
  font-size: 13px;
  font-style: normal;
  font-weight: 500;
}

.note-comments-body {
  position: relative;
  min-height: 86px;
}

.note-comment-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.note-comment-thread {
  min-width: 0;
}

.note-comment-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

.note-comment-row--reply {
  margin-left: 40px;
  padding-top: 8px;
  padding-bottom: 8px;
  background: #fafafa;
}

.note-comment-row.is-highlight-target {
  border-color: #ffb3c2;
  background: #fff6f8;
}

.note-comment-row.is-highlight-active {
  animation: complaint-comment-flash 0.72s ease-in-out 0s 2;
}

.note-comment-avatar {
  flex: 0 0 auto;
}

.note-comment-body {
  min-width: 0;
  flex: 1 1 auto;
}

.note-comment-meta {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  color: #909399;
  font-size: 12px;
  line-height: 20px;
}

.note-comment-meta strong {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #303133;
  font-size: 13px;
  font-weight: 700;
}

.note-comment-content {
  margin-top: 3px;
  color: #303133;
  font-size: 14px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.note-comment-deleted {
  color: #a8abb2;
  font-size: 14px;
  line-height: 24px;
}

.note-reply-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.note-reply-target {
  color: #606266;
}

.note-reply-more {
  margin: 8px 0 0 52px;
  color: #909399;
  font-size: 12px;
}

.note-comment-load-more {
  display: flex;
  justify-content: center;
  margin-top: 14px;
}

.note-comment-error {
  margin-top: 10px;
  color: #c45656;
  font-size: 13px;
  text-align: center;
}

@keyframes complaint-comment-flash {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(255, 126, 154, 0);
  }
  45% {
    box-shadow: 0 0 0 5px rgba(255, 126, 154, 0.2);
  }
}

.complaint-preview {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.complaint-preview-row1 {
  display: flex;
  align-items: center;
  gap: 10px;
}

.complaint-preview-row2 {
  font-size: 13px;
  color: #606266;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.complaint-target {
  font-size: 12px;
  color: #c45656;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.complaint-preview-evidence {
  width: fit-content;
  padding: 2px 8px;
  border-radius: 999px;
  background: #f4f4f5;
  color: #909399;
  font-size: 12px;
  line-height: 20px;
}

.evidence-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.evidence-grid--compact {
  gap: 8px;
}

.evidence-image {
  width: 92px;
  height: 92px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  background: #f5f7fa;
  cursor: pointer;
}

.evidence-grid--compact .evidence-image {
  width: 64px;
  height: 64px;
  border-radius: 6px;
}

.evidence-grid--large {
  gap: 14px;
}

.evidence-image--large {
  width: 132px;
  height: 132px;
  border-radius: 10px;
}

.complaint-detail-dialog :deep(.el-dialog) {
  max-width: calc(100vw - 48px);
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
}

.complaint-detail-dialog :deep(.el-dialog__header) {
  padding: 24px 30px 12px;
  margin-right: 0;
  border-bottom: 1px solid #ebeef5;
}

.complaint-detail-dialog :deep(.el-dialog__title) {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.complaint-detail-dialog :deep(.el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  padding: 18px 28px 20px;
  background: #f7f8fa;
}

.complaint-detail-dialog :deep(.el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 28px 16px;
  border-top: 1px solid #ebeef5;
  background: #fff;
}

.complaint-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.complaint-detail-hero {
  display: flex;
  justify-content: space-between;
  align-items: stretch;
  gap: 18px;
  padding: 18px 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
}

.complaint-hero-main {
  min-width: 0;
  flex: 1 1 auto;
}

.complaint-hero-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  color: #909399;
  font-size: 13px;
}

.complaint-hero-main h2 {
  margin: 0 0 12px;
  color: #1f2937;
  font-size: 22px;
  line-height: 1.25;
  font-weight: 700;
}

.complaint-hero-sub {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  max-width: 100%;
  color: #606266;
  font-size: 14px;
}

.complaint-hero-sub :deep(.el-link) {
  min-width: 0;
  max-width: min(460px, 100%);
  overflow: hidden;
}

.complaint-hero-sub :deep(.el-link__inner) {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.complaint-hero-side {
  flex: 0 0 250px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.complaint-hero-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  color: #909399;
  font-size: 13px;
}

.complaint-hero-action {
  min-width: 150px;
}

.complaint-detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 14px;
  align-items: start;
}

.complaint-detail-main,
.complaint-detail-side {
  min-width: 0;
}

.complaint-detail-main {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.complaint-section,
.side-panel {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
}

.complaint-section {
  padding: 16px 18px;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
}

.section-heading h3,
.side-panel h3 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  line-height: 1.2;
  font-weight: 700;
}

.section-heading span {
  color: #909399;
  font-size: 13px;
  white-space: nowrap;
}

.complaint-text-block,
.reported-comment-block {
  min-height: 46px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f7f8fa;
  color: #303133;
  font-size: 15px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.reported-comment-block {
  border-left: 3px solid #f56c6c;
  color: #606266;
}

.reported-comment-card {
  padding: 14px;
  border-radius: 8px;
  background: #f7f8fa;
}

.reported-comment-card .reported-comment-block {
  background: #fff;
}

.reported-comment-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
  color: #909399;
  font-size: 12px;
}

.reported-comment-author {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #909399;
  font-size: 13px;
}

.reported-comment-author strong {
  color: #303133;
  font-size: 14px;
  font-weight: 700;
}

.reported-comment-context {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.reported-comment-context div {
  min-width: 0;
  padding: 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #ebeef5;
}

.reported-comment-context span {
  display: block;
  margin-bottom: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 1;
}

.reported-comment-context p {
  margin: 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}

.reported-comment-context strong {
  color: #303133;
}

.complaint-detail-side {
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: sticky;
  top: 0;
}

.side-panel {
  padding: 14px;
}

.side-panel h3 {
  margin-bottom: 14px;
}

.side-person,
.side-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-top: 1px solid #f0f2f5;
}

.side-person {
  align-items: center;
}

.side-label,
.side-row span {
  color: #909399;
  font-size: 13px;
}

.side-person-main {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.side-person-main strong,
.side-row strong {
  min-width: 0;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.timeline-row {
  position: relative;
  display: grid;
  grid-template-columns: 14px minmax(0, 1fr);
  gap: 10px;
  padding: 0 0 16px;
}

.timeline-row:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 14px;
  bottom: 2px;
  width: 1px;
  background: #dcdfe6;
}

.timeline-dot {
  width: 10px;
  height: 10px;
  margin-top: 4px;
  border-radius: 50%;
  background: #409eff;
  box-shadow: 0 0 0 3px #ecf5ff;
}

.timeline-row.muted .timeline-dot {
  background: #c0c4cc;
  box-shadow: 0 0 0 3px #f4f4f5;
}

.timeline-row strong,
.timeline-row span {
  display: block;
}

.timeline-row strong {
  color: #303133;
  font-size: 14px;
  line-height: 1.3;
}

.timeline-row span {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

.feedback-status {
  color: #606266;
  font-size: 14px;
}

.feedback-content {
  margin: 12px 0 0;
  padding: 12px;
  border-radius: 8px;
  background: #f7f8fa;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.compact-empty {
  min-height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #f7f8fa;
  color: #909399;
  font-size: 14px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.feedback-ok {
  color: #67C23A;
}

.feedback-bad {
  color: #F56C6C;
}

@media (max-width: 1100px) {
  .complaint-detail-layout {
    grid-template-columns: 1fr;
  }

  .complaint-detail-side {
    position: static;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .complaint-detail-dialog :deep(.el-dialog) {
    width: calc(100vw - 24px) !important;
    max-width: calc(100vw - 24px);
  }

  .complaint-detail-dialog :deep(.el-dialog__header),
  .complaint-detail-dialog :deep(.el-dialog__body),
  .complaint-detail-dialog :deep(.el-dialog__footer) {
    padding-left: 16px;
    padding-right: 16px;
  }

  .complaint-detail-hero,
  .complaint-hero-sub,
  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .complaint-hero-side {
    width: 100%;
    flex-basis: auto;
    align-items: stretch;
  }

  .complaint-hero-status {
    align-items: flex-start;
  }

  .complaint-hero-action {
    width: 100%;
  }

  .complaint-detail-side {
    position: static;
    display: flex;
  }

  .evidence-image--large {
    width: 104px;
    height: 104px;
  }

  .reported-comment-context {
    grid-template-columns: 1fr;
  }
}
</style>
