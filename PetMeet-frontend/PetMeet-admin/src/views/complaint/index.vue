<template>
  <div class="complaint-container">
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="笔记标题 / 投诉人 / 作者"
          clearable
          style="width: 220px"
          @input="handleFilter"
        />
        <el-select v-model="statusFilter" placeholder="处理状态" style="width: 160px" clearable @change="handleFilter">
          <el-option label="待处理" :value="0" />
          <el-option label="已处理" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>
        <div class="filter-actions">
          <span v-if="selectedRows.length > 0" class="selected-hint">已选 {{ selectedRows.length }} 项</span>
          <el-button
            type="danger"
            plain
            :disabled="deletableSelectedIds.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-alert
        class="table-tip"
        type="info"
        :closable="false"
        show-icon
        title="点击“投诉详情”可查看完整投诉内容；点击笔记标题可查看笔记内容"
      />
      <el-table :data="tableData" style="width: 100%" row-key="id" v-loading="loading" @selection-change="handleSelectionChange">
      <!-- 不常用字段放进可展开区域，避免表格横向滚动 -->
        <el-table-column type="selection" width="42" />
        <el-table-column type="expand" width="44">
          <template #default="{ row }">
            <div class="expand-panel">
              <el-descriptions border size="small" :column="2">
                <el-descriptions-item label="作者">
                  <UserInfoPopover v-if="row.noteAuthorId" :user-id="row.noteAuthorId" placement="right" :width="340">
                    <template #reference>
                      <span class="user-link">{{ row.noteAuthorName || '-' }}</span>
                    </template>
                  </UserInfoPopover>
                  <span v-else>-</span>
                </el-descriptions-item>
                <el-descriptions-item label="处理时间">{{ row.handleTime || '-' }}</el-descriptions-item>
                <el-descriptions-item label="原因">{{ row.reason || '-' }}</el-descriptions-item>
                <el-descriptions-item label="处理说明">
                  <span class="expand-pre">{{ row.handleRemark || '-' }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="详情">
                  <span class="expand-pre">{{ row.content || '-' }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="用户反馈">
                  <span v-if="row.feedbackStatus === 1" class="feedback-ok">满意</span>
                  <span v-else-if="row.feedbackStatus === 2" class="feedback-bad">不满意</span>
                  <span v-else>-</span>
                </el-descriptions-item>
                <el-descriptions-item v-if="row.feedbackContent" label="反馈内容">
                  <span class="expand-pre">{{ row.feedbackContent }}</span>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="笔记" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="note-cell">
              <el-tag size="small" type="info" effect="plain">#{{ row.noteId }}</el-tag>
              <el-link
                v-if="row.noteId"
                type="primary"
                :underline="false"
                class="note-link"
                @click.stop="openNoteDialog(row.noteId)"
              >
                <span class="note-title-ellipsis">{{ row.noteTitle || '-' }}</span>
              </el-link>
              <span v-else class="note-title-ellipsis">{{ row.noteTitle || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="投诉人" min-width="170">
          <template #default="{ row }">
            <UserInfoPopover v-if="row.reporterId" :user-id="row.reporterId" placement="right" :width="340">
              <template #reference>
                <div class="user-cell">
                  <el-avatar :size="24" :src="resolveImageUrl(row.reporterAvatar)" />
                  <span class="user-name">{{ row.reporterName || '-' }}</span>
                </div>
              </template>
            </UserInfoPopover>
            <div v-else class="user-cell">
              <el-avatar :size="24" :src="resolveImageUrl(row.reporterAvatar)" />
              <span class="user-name">{{ row.reporterName || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="投诉内容" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="complaint-preview" @click.stop="openComplaintDialog(row)">
              <div class="complaint-preview-row1">
                <el-tag size="small" type="warning" effect="plain">{{ row.reason || '未填写原因' }}</el-tag>
                <el-link type="primary" :underline="false" @click.stop.prevent="openComplaintDialog(row)">详情</el-link>
              </div>
              <div class="complaint-preview-row2">{{ oneLineText(row.content) || '（无补充说明）' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button link size="small" @click.stop="openComplaintDialog(row)">投诉详情</el-button>
            <template v-if="row.status === 0">
              <el-button type="success" link size="small" @click.stop="updateStatus(row, 1)">处理</el-button>
              <el-button type="danger" link size="small" @click.stop="updateStatus(row, 2)">驳回</el-button>
            </template>
            <el-button
              v-if="canDeleteComplaint(row)"
              type="danger"
              link
              size="small"
              @click.stop="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          v-model:current-page="currentPage"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="noteDialogVisible"
      title="笔记内容"
      width="780px"
      top="6vh"
      class="note-detail-dialog"
      @closed="resetNoteDialog"
    >
      <div v-loading="noteDetailLoading">
        <div v-if="noteDetail" class="note-detail">
          <div class="note-detail-header">
            <div class="note-author">
              <el-avatar :size="36" :src="resolveImageUrl(noteDetail.userAvatar)" />
              <div class="note-author-info">
                <div class="note-author-name">{{ noteDetail.username || '-' }}</div>
                <div class="note-time">{{ formatDateTime(noteDetail.createTime) }}</div>
              </div>
            </div>
            <el-tag size="small" type="info" effect="plain">#{{ noteDetail.id }}</el-tag>
          </div>

          <h3 class="note-title-full">{{ noteDetail.title || '-' }}</h3>
          <div v-if="noteDetail.category || noteTags.length" class="note-meta">
            <el-tag v-if="noteDetail.category" size="small" type="info">{{ noteDetail.category }}</el-tag>
            <el-tag v-for="tag in noteTags" :key="tag" size="small" effect="plain">#{{ tag }}</el-tag>
          </div>

          <div class="note-media">
            <video
              v-if="noteDetail.type === 'video' && noteDetail.videoUrl"
              class="note-video"
              :src="resolveImageUrl(noteDetail.videoUrl)"
              :poster="resolveImageUrl(noteDetail.cover)"
              controls
              playsinline
            />
            <el-image
              v-else-if="noteDetail.cover"
              class="note-image"
              :src="resolveImageUrl(noteDetail.cover)"
              fit="contain"
            />
            <el-empty v-else description="暂无图片" :image-size="70" />
          </div>

          <div class="note-text">{{ noteDetail.content || '-' }}</div>
        </div>

        <el-empty v-else :description="noteDetailError || '暂无数据'" :image-size="90" />
      </div>
    </el-dialog>

    <el-dialog
      v-model="complaintDialogVisible"
      title="投诉详情"
      width="720px"
      top="6vh"
      class="complaint-detail-dialog"
      @closed="resetComplaintDialog"
    >
      <div v-if="currentComplaint" class="complaint-detail">
        <el-descriptions border size="small" :column="2">
          <el-descriptions-item label="投诉ID">{{ currentComplaint.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(currentComplaint.status)" size="small">{{ statusText(currentComplaint.status) }}</el-tag>
          </el-descriptions-item>

          <el-descriptions-item label="笔记">
            <el-link
              v-if="currentComplaint.noteId"
              type="primary"
              :underline="false"
              @click="openNoteDialog(currentComplaint.noteId)"
            >
              {{ currentComplaint.noteTitle || `#${currentComplaint.noteId}` }}
            </el-link>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="作者">{{ currentComplaint.noteAuthorName || '-' }}</el-descriptions-item>

          <el-descriptions-item label="投诉人">
            <div class="user-cell">
              <el-avatar :size="24" :src="resolveImageUrl(currentComplaint.reporterAvatar)" />
              <span class="user-name">{{ currentComplaint.reporterName || '-' }}</span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ currentComplaint.createTime || '-' }}</el-descriptions-item>

          <el-descriptions-item label="原因">{{ currentComplaint.reason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ currentComplaint.handleTime || '-' }}</el-descriptions-item>

          <el-descriptions-item label="投诉内容" :span="2">
            <div class="expand-pre">{{ currentComplaint.content || '-' }}</div>
          </el-descriptions-item>

          <el-descriptions-item label="处理说明" :span="2">
            <div class="expand-pre">{{ currentComplaint.handleRemark || '-' }}</div>
          </el-descriptions-item>

          <el-descriptions-item label="用户反馈">
            <span v-if="currentComplaint.feedbackStatus === 1" class="feedback-ok">满意</span>
            <span v-else-if="currentComplaint.feedbackStatus === 2" class="feedback-bad">不满意</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="反馈时间">{{ currentComplaint.feedbackTime || '-' }}</el-descriptions-item>

          <el-descriptions-item v-if="currentComplaint.feedbackContent" label="反馈内容" :span="2">
            <div class="expand-pre">{{ currentComplaint.feedbackContent }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <el-empty v-else description="暂无数据" :image-size="90" />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="complaintDialogVisible = false">关闭</el-button>
          <el-button
            v-if="currentComplaint?.noteId"
            type="primary"
            @click="openNoteDialog(currentComplaint.noteId)"
          >
            查看笔记内容
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getComplaintList, updateComplaintStatus, deleteComplaint, batchDeleteComplaints } from '@/api/complaint'
import { getNoteDetail } from '@/api/content'
import { resolveImageUrl } from '@/utils/image'
import UserInfoPopover from '@/components/UserInfoPopover.vue'

const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedRows = ref([])

const noteDialogVisible = ref(false)
const noteDetailLoading = ref(false)
const noteDetail = ref(null)
const noteDetailError = ref('')

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

const canDeleteComplaint = (row) => [1, 2].includes(Number(row?.status))

const deletableSelectedIds = computed(() => {
  return selectedRows.value
    .filter((row) => canDeleteComplaint(row))
    .map((row) => normalizeId(row?.id))
    .filter(Boolean)
})

const resetNoteDialog = () => {
  noteDetailLoading.value = false
  noteDetail.value = null
  noteDetailError.value = ''
}

const openNoteDialog = async (noteId) => {
  if (!noteId) {
    ElMessage.warning('未找到笔记ID')
    return
  }

  noteDialogVisible.value = true
  noteDetailLoading.value = true
  noteDetail.value = null
  noteDetailError.value = ''

  try {
    const res = await getNoteDetail(noteId)
    if (res.code === 200 && res.data) {
      noteDetail.value = res.data
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
  loading.value = true
  try {
    const res = await getComplaintList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      status: statusFilter.value === '' ? undefined : statusFilter.value,
      keyword: keyword.value ? keyword.value.trim() : undefined
    })
    if (res.code === 200 && res.data) {
      const records = res.data.records || []
      tableData.value = records.map((item) => ({
        ...item,
        createTime: formatDateTime(item.createTime),
        handleTime: formatDateTime(item.handleTime),
        feedbackTime: formatDateTime(item.feedbackTime)
      }))
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.message || res.msg || 'Load failed')
    }
  } catch (e) {
    console.error('加载投诉列表失败', e)
  } finally {
    selectedRows.value = []
    loading.value = false
  }
}

const handleFilter = () => {
  currentPage.value = 1
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
  } catch (e) {}
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
  } catch (e) {}
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
  } catch (e) {
    // 用户取消了操作
  }
}

onMounted(() => {
  loadList()
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
}

.note-link {
  min-width: 0;
  flex: 1;
  justify-content: flex-start;
}

.note-title-ellipsis {
  display: inline-block;
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

.complaint-detail-dialog :deep(.el-dialog__body) {
  max-height: calc(100vh - 240px);
  overflow: auto;
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
</style>
