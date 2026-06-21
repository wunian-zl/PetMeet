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
                <el-descriptions-item v-if="evidenceImagesOf(row).length" label="凭证图片" :span="2">
                  <div class="evidence-grid evidence-grid--compact">
                    <el-image
                      v-for="(img, idx) in resolvedEvidenceImages(row)"
                      :key="`${row.id}-expand-evidence-${idx}`"
                      class="evidence-image"
                      :src="img"
                      :preview-src-list="resolvedEvidenceImages(row)"
                      :initial-index="idx"
                      fit="cover"
                      preview-teleported
                    />
                  </div>
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
              <el-tag v-if="isCommentComplaint(row)" size="small" type="danger" effect="plain">评论举报</el-tag>
              <el-link
                v-if="row.noteId"
                type="primary"
                :underline="false"
                class="note-link"
                @click.stop="openNoteDialog(row.noteId, isCommentComplaint(row) ? row : null)"
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
              <div v-if="isCommentComplaint(row)" class="complaint-target">
                被举报评论：{{ oneLineText(row.commentContent) || '评论已删除或不存在' }}
              </div>
              <div class="complaint-preview-row2">{{ oneLineText(row.content) || '（无补充说明）' }}</div>
              <div v-if="evidenceImagesOf(row).length" class="complaint-preview-evidence">
                图片凭证{{ evidenceImagesOf(row).length }}张
              </div>
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

          <section class="note-comments-panel">
            <div class="note-comments-header">
              <div class="note-comments-title">
                <span>评论</span>
                <em>{{ noteCommentTotal }}条</em>
              </div>
              <el-tag v-if="noteHighlightCommentId" size="small" type="danger" effect="plain">
                已定位举报评论
              </el-tag>
            </div>

            <div v-loading="noteCommentsLoading" class="note-comments-body">
              <el-empty
                v-if="!noteCommentsLoading && !noteComments.length"
                description="暂无评论"
                :image-size="70"
              />
              <div v-else class="note-comment-list">
                <div v-for="comment in noteComments" :key="comment.id" class="note-comment-thread">
                  <div
                    class="note-comment-row"
                    :class="{
                      'is-highlight-target': isTargetComment(comment),
                      'is-highlight-active': isTargetComment(comment) && noteHighlightActive
                    }"
                    :data-comment-id="comment.id"
                  >
                    <el-avatar
                      v-if="!comment.deleted"
                      :size="30"
                      :src="resolveImageUrl(comment.userAvatar)"
                      class="note-comment-avatar"
                    />
                    <div class="note-comment-body">
                      <div v-if="comment.deleted" class="note-comment-deleted">评论已删除</div>
                      <template v-else>
                        <div class="note-comment-meta">
                          <strong>{{ commentDisplayName(comment) }}</strong>
                          <el-tag v-if="comment.author" size="small" effect="plain">作者</el-tag>
                          <span>{{ formatDateTime(comment.createTime) }}</span>
                        </div>
                        <div class="note-comment-content">{{ comment.content || '-' }}</div>
                      </template>
                    </div>
                  </div>

                  <div v-if="comment.replies && comment.replies.length" class="note-reply-list">
                    <div
                      v-for="reply in comment.replies"
                      :key="reply.id"
                      class="note-comment-row note-comment-row--reply"
                      :class="{
                        'is-highlight-target': isTargetComment(reply),
                        'is-highlight-active': isTargetComment(reply) && noteHighlightActive
                      }"
                      :data-comment-id="reply.id"
                    >
                      <el-avatar
                        :size="24"
                        :src="resolveImageUrl(reply.userAvatar)"
                        class="note-comment-avatar"
                      />
                      <div class="note-comment-body">
                        <div class="note-comment-meta">
                          <strong>{{ commentDisplayName(reply) }}</strong>
                          <el-tag v-if="reply.author" size="small" effect="plain">作者</el-tag>
                          <span v-if="reply.replyToNickname" class="note-reply-target">
                            回复@{{ reply.replyToNickname }}
                          </span>
                          <span>{{ formatDateTime(reply.createTime) }}</span>
                        </div>
                        <div class="note-comment-content">{{ reply.content || '-' }}</div>
                      </div>
                    </div>
                  </div>

                  <div v-if="comment.replyCount > (comment.replies?.length || 0)" class="note-reply-more">
                    还有{{ comment.replyCount - (comment.replies?.length || 0) }}条回复未展开
                  </div>
                </div>
              </div>

              <div v-if="noteCommentHasMore" class="note-comment-load-more">
                <el-button size="small" :loading="noteCommentsLoading" @click="loadMoreNoteComments">
                  加载更多评论
                </el-button>
              </div>
              <div v-if="noteCommentError" class="note-comment-error">{{ noteCommentError }}</div>
            </div>
          </section>
        </div>

        <el-empty v-else :description="noteDetailError || '暂无数据'" :image-size="90" />
      </div>
    </el-dialog>

    <el-dialog
      v-model="complaintDialogVisible"
      title="投诉详情"
      width="1080px"
      top="3vh"
      class="complaint-detail-dialog"
      @closed="resetComplaintDialog"
    >
      <div v-if="currentComplaint" class="complaint-detail">
        <div class="complaint-detail-hero">
          <div class="complaint-hero-main">
            <div class="complaint-hero-meta">
              <el-tag :type="isCommentComplaint(currentComplaint) ? 'danger' : 'info'" effect="plain">
                {{ complaintTargetText(currentComplaint) }}
              </el-tag>
              <span>#{{ currentComplaint.id }}</span>
            </div>
            <h2>{{ currentComplaint.reason || '未填写原因' }}</h2>
            <div class="complaint-hero-sub">
              <span>关联笔记</span>
              <el-link
                v-if="currentComplaint.noteId"
                type="primary"
                :underline="false"
                @click="openNoteDialog(currentComplaint.noteId, isCommentComplaint(currentComplaint) ? currentComplaint : null)"
              >
                {{ currentComplaint.noteTitle || `#${currentComplaint.noteId}` }}
              </el-link>
              <span v-else>-</span>
            </div>
          </div>
          <div class="complaint-hero-side">
            <div class="complaint-hero-status">
              <el-tag :type="statusType(currentComplaint.status)" effect="light" size="large">
                {{ statusText(currentComplaint.status) }}
              </el-tag>
              <span>提交于{{ currentComplaint.createTime || '-' }}</span>
            </div>
            <el-button
              v-if="currentComplaint?.noteId"
              type="primary"
              class="complaint-hero-action"
              @click="openNoteDialog(currentComplaint.noteId, isCommentComplaint(currentComplaint) ? currentComplaint : null)"
            >
              {{ isCommentComplaint(currentComplaint) ? '查看笔记的评论' : '查看笔记内容' }}
            </el-button>
          </div>
        </div>

        <div class="complaint-detail-layout">
          <main class="complaint-detail-main">
            <section class="complaint-section">
              <div class="section-heading">
                <h3>投诉说明</h3>
                <span>{{ currentComplaint.content ? '用户补充内容' : '用户未填写补充说明' }}</span>
              </div>
              <div class="complaint-text-block">
                {{ currentComplaint.content || '（无补充说明）' }}
              </div>
            </section>

            <section v-if="isCommentComplaint(currentComplaint)" class="complaint-section">
              <div class="section-heading">
                <h3>被举报评论</h3>
                <span>{{ commentKindText(currentComplaint) }}</span>
              </div>
              <div class="reported-comment-card">
                <div class="reported-comment-meta">
                  <span>#{{ currentComplaint.commentId || '-' }}</span>
                  <el-tag :type="currentComplaint.commentDeleted ? 'info' : 'success'" size="small" effect="plain">
                    {{ currentComplaint.commentDeleted ? '已删除' : '正常' }}
                  </el-tag>
                  <span>{{ currentComplaint.commentCreateTime || '-' }}</span>
                </div>
                <div class="reported-comment-author">
                  <span>评论作者</span>
                  <strong>{{ currentComplaint.commentAuthorName || '-' }}</strong>
                </div>
                <div class="reported-comment-block">
                  {{ currentComplaint.commentContent || '评论已删除或不存在' }}
                </div>
                <div
                  v-if="currentComplaint.replyToContent || currentComplaint.parentCommentContent"
                  class="reported-comment-context"
                >
                  <div v-if="currentComplaint.replyToContent">
                    <span>回复对象</span>
                    <p>
                      <strong>{{ currentComplaint.replyToAuthorName || '用户' }}：</strong>
                      {{ currentComplaint.replyToContent }}
                    </p>
                  </div>
                  <div v-if="currentComplaint.parentCommentContent">
                    <span>所属一级评论</span>
                    <p>
                      <strong>{{ currentComplaint.parentCommentAuthorName || '用户' }}：</strong>
                      {{ currentComplaint.parentCommentContent }}
                    </p>
                  </div>
                </div>
              </div>
            </section>

            <section class="complaint-section">
              <div class="section-heading">
                <h3>图片凭证</h3>
                <span>{{ evidenceImagesOf(currentComplaint).length ? `${evidenceImagesOf(currentComplaint).length}张` : '无图片凭证' }}</span>
              </div>
              <div v-if="evidenceImagesOf(currentComplaint).length" class="evidence-grid evidence-grid--large">
                <el-image
                  v-for="(img, idx) in resolvedEvidenceImages(currentComplaint)"
                  :key="`complaint-evidence-${idx}`"
                  class="evidence-image evidence-image--large"
                  :src="img"
                  :preview-src-list="resolvedEvidenceImages(currentComplaint)"
                  :initial-index="idx"
                  fit="cover"
                  preview-teleported
                />
              </div>
              <div v-else class="compact-empty">暂无图片凭证</div>
            </section>

            <section class="complaint-section">
              <div class="section-heading">
                <h3>处理结果</h3>
                <span>{{ currentComplaint.handleTime || '尚未处理' }}</span>
              </div>
              <div class="complaint-text-block">
                {{ currentComplaint.handleRemark || '（暂无处理说明）' }}
              </div>
            </section>
          </main>

          <aside class="complaint-detail-side">
            <section class="side-panel">
              <h3>相关人员</h3>
              <div class="side-person">
                <span class="side-label">投诉人</span>
                <div class="side-person-main">
                  <el-avatar :size="32" :src="resolveImageUrl(currentComplaint.reporterAvatar)" />
                  <strong>{{ currentComplaint.reporterName || '-' }}</strong>
                </div>
              </div>
              <div class="side-row">
                <span>笔记作者</span>
                <strong>{{ currentComplaint.noteAuthorName || '-' }}</strong>
              </div>
              <div class="side-row" v-if="isCommentComplaint(currentComplaint)">
                <span>评论作者</span>
                <strong>{{ currentComplaint.commentAuthorName || '-' }}</strong>
              </div>
            </section>

            <section class="side-panel">
              <h3>时间线</h3>
              <div class="timeline-row">
                <span class="timeline-dot"></span>
                <div>
                  <strong>提交投诉</strong>
                  <span>{{ currentComplaint.createTime || '-' }}</span>
                </div>
              </div>
              <div class="timeline-row" :class="{ muted: !currentComplaint.handleTime }">
                <span class="timeline-dot"></span>
                <div>
                  <strong>处理完成</strong>
                  <span>{{ currentComplaint.handleTime || '待处理' }}</span>
                </div>
              </div>
              <div class="timeline-row" :class="{ muted: !currentComplaint.feedbackTime }">
                <span class="timeline-dot"></span>
                <div>
                  <strong>用户反馈</strong>
                  <span>{{ currentComplaint.feedbackTime || '暂无反馈' }}</span>
                </div>
              </div>
            </section>

            <section class="side-panel">
              <h3>反馈状态</h3>
              <div class="feedback-status">
                <span v-if="currentComplaint.feedbackStatus === 1" class="feedback-ok">满意</span>
                <span v-else-if="currentComplaint.feedbackStatus === 2" class="feedback-bad">不满意</span>
                <span v-else>暂无反馈</span>
              </div>
              <p v-if="currentComplaint.feedbackContent" class="feedback-content">
                {{ currentComplaint.feedbackContent }}
              </p>
            </section>
          </aside>
        </div>
      </div>
      <el-empty v-else description="暂无数据" :image-size="90" />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="complaintDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getComplaintList, updateComplaintStatus, deleteComplaint, batchDeleteComplaints } from '@/api/complaint'
import { getNoteDetail, getCommentList, getCommentReplies } from '@/api/content'
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
