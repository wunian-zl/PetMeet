<template>
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
              <el-tag v-if="noteHighlightCommentId" size="small" type="danger" effect="plain" round>
                已定位#{{ noteHighlightCommentId }}被举报评论
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
                        <div v-if="isTargetComment(comment)" class="note-comment-target-banner">
                          <el-icon><WarningFilled /></el-icon>
                          <span>当前投诉指向这条评论</span>
                        </div>
                        <div class="note-comment-meta">
                          <strong>{{ commentDisplayName(comment) }}</strong>
                          <el-tag v-if="isTargetComment(comment)" size="small" type="danger" effect="dark" round>
                            被举报评论
                          </el-tag>
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
                        <div v-if="isTargetComment(reply)" class="note-comment-target-banner">
                          <el-icon><WarningFilled /></el-icon>
                          <span>当前投诉指向这条回复</span>
                        </div>
                        <div class="note-comment-meta">
                          <strong>{{ commentDisplayName(reply) }}</strong>
                          <el-tag v-if="isTargetComment(reply)" size="small" type="danger" effect="dark" round>
                            被举报回复
                          </el-tag>
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
      width="1120px"
      top="4vh"
      class="complaint-detail-dialog"
      @closed="resetComplaintDialog"
    >
      <template #header>
        <div class="complaint-dialog-title">
          <div>
            <div class="complaint-dialog-eyebrow">
              <el-icon><Tickets /></el-icon>
              <span>投诉工单</span>
            </div>
            <div class="complaint-dialog-heading">
              <h2>投诉详情</h2>
              <span v-if="currentComplaint">#{{ currentComplaint.id || '-' }}</span>
            </div>
          </div>
          <el-tag
            v-if="currentComplaint"
            :type="statusType(currentComplaint.status)"
            effect="light"
            size="large"
            round
          >
            {{ statusText(currentComplaint.status) }}
          </el-tag>
        </div>
      </template>

      <div v-if="currentComplaint" class="complaint-detail">
        <main class="complaint-detail-main">
          <section class="complaint-overview">
            <div class="complaint-overview-main">
              <div class="complaint-overview-meta">
                <el-tag :type="isCommentComplaint(currentComplaint) ? 'danger' : 'info'" effect="plain" round>
                  <el-icon>
                    <ChatDotRound v-if="isCommentComplaint(currentComplaint)" />
                    <Document v-else />
                  </el-icon>
                  {{ complaintTargetText(currentComplaint) }}
                </el-tag>
                <span>提交于{{ currentComplaint.createTime || '-' }}</span>
              </div>
              <h1>{{ currentComplaint.reason || '未填写原因' }}</h1>
              <div class="complaint-linked-note">
                <el-icon><Link /></el-icon>
                <span>关联笔记</span>
                <el-link
                  v-if="currentComplaint.noteId"
                  type="primary"
                  :underline="false"
                  @click="openNoteDialog(currentComplaint.noteId, isCommentComplaint(currentComplaint) ? currentComplaint : null)"
                >
                  {{ currentComplaint.noteTitle || `#${currentComplaint.noteId}` }}
                </el-link>
                <strong v-else>-</strong>
              </div>
            </div>
            <div class="complaint-overview-aside">
              <div class="complaint-metric">
                <span>凭证</span>
                <strong>{{ evidenceImagesOf(currentComplaint).length }}张</strong>
              </div>
              <div class="complaint-metric">
                <span>反馈</span>
                <strong :class="feedbackClass(currentComplaint)">{{ feedbackText(currentComplaint) }}</strong>
              </div>
              <el-button
                v-if="currentComplaint?.noteId"
                type="primary"
                :icon="View"
                @click="openNoteDialog(currentComplaint.noteId, isCommentComplaint(currentComplaint) ? currentComplaint : null)"
              >
                {{ isCommentComplaint(currentComplaint) ? '查看被举报笔记上下文' : '查看被投诉笔记上下文' }}
              </el-button>
            </div>
          </section>

          <section v-if="canHandleComplaint(currentComplaint)" class="complaint-decision-bar">
            <div class="complaint-decision-copy">
              <el-icon><WarningFilled /></el-icon>
              <div>
                <strong>待处理</strong>
                <span>处理结果会同步通知投诉人，请先核对内容、评论和凭证。</span>
              </div>
            </div>
          </section>

            <section v-if="isCommentComplaint(currentComplaint)" class="complaint-section">
              <div class="section-heading">
                <div>
                  <el-icon><ChatDotRound /></el-icon>
                  <h3>被举报对象</h3>
                </div>
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
                    <span>被举报回复的上文</span>
                    <p>
                      <strong>{{ currentComplaint.replyToAuthorName || '用户' }}：</strong>
                      {{ currentComplaint.replyToContent }}
                    </p>
                  </div>
                  <div v-if="currentComplaint.parentCommentContent">
                    <span>所在一级评论</span>
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
                <div>
                  <el-icon><MessageBox /></el-icon>
                  <h3>投诉说明</h3>
                </div>
                <span>{{ currentComplaint.content ? '用户补充内容' : '用户未填写补充说明' }}</span>
              </div>
              <div class="complaint-text-block">
                {{ currentComplaint.content || '（无补充说明）' }}
              </div>
            </section>

            <section class="complaint-section">
              <div class="section-heading">
                <div>
                  <el-icon><Picture /></el-icon>
                  <h3>图片凭证</h3>
                </div>
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
              <div v-else class="compact-empty">
                <el-icon><Picture /></el-icon>
                <span>暂无图片凭证</span>
              </div>
            </section>

            <section class="complaint-section">
              <div class="section-heading">
                <div>
                    <el-icon v-if="Number(currentComplaint.status) === 1"><CircleCheckFilled /></el-icon>
                    <el-icon v-else-if="Number(currentComplaint.status) === 2"><CircleCloseFilled /></el-icon>
                    <el-icon v-else><InfoFilled /></el-icon>
                  <h3>处理结果</h3>
                </div>
                <span>{{ currentComplaint.handleTime || '尚未处理' }}</span>
              </div>
              <div class="complaint-text-block">
                {{ currentComplaint.handleRemark || '（暂无处理说明）' }}
              </div>
            </section>
        </main>

        <aside class="complaint-detail-side">
            <section class="side-panel">
              <h3>案件信息</h3>
              <div class="side-row">
                <span>投诉ID</span>
                <strong>#{{ currentComplaint.id || '-' }}</strong>
              </div>
              <div class="side-row">
                <span>笔记ID</span>
                <strong>{{ currentComplaint.noteId ? `#${currentComplaint.noteId}` : '-' }}</strong>
              </div>
              <div v-if="isCommentComplaint(currentComplaint)" class="side-row">
                <span>评论ID</span>
                <strong>{{ currentComplaint.commentId ? `#${currentComplaint.commentId}` : '-' }}</strong>
              </div>
              <div class="side-row">
                <span>当前状态</span>
                <el-tag :type="statusType(currentComplaint.status)" size="small" effect="light">
                  {{ statusText(currentComplaint.status) }}
                </el-tag>
              </div>
              <div class="side-row">
                <span>反馈状态</span>
                <strong :class="feedbackClass(currentComplaint)">{{ feedbackText(currentComplaint) }}</strong>
              </div>
              <p v-if="currentComplaint.feedbackContent" class="side-feedback-content">
                {{ currentComplaint.feedbackContent }}
              </p>
            </section>

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
              <h3>处理时间线</h3>
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

        </aside>
      </div>
      <el-empty v-else description="暂无数据" :image-size="90" />
      <template #footer>
        <div class="dialog-footer">
          <template v-if="currentComplaint && canHandleComplaint(currentComplaint)">
            <el-button type="success" :icon="Check" @click="handleComplaintAction(1)">处理</el-button>
            <el-button type="danger" plain :icon="Close" @click="handleComplaintAction(2)">驳回</el-button>
          </template>
          <el-button @click="complaintDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
</template>

<script setup>
import { inject } from 'vue'
import {
  ChatDotRound,
  Check,
  CircleCheckFilled,
  CircleCloseFilled,
  Close,
  Document,
  InfoFilled,
  Link,
  MessageBox,
  Picture,
  Tickets,
  View,
  WarningFilled
} from '@element-plus/icons-vue'

const {
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
  noteCommentHasMore,
  commentDisplayName,
  isTargetComment,
  loadMoreNoteComments,
  resetNoteDialog,
  openNoteDialog,
  resetComplaintDialog,
  updateStatus,
  resolveImageUrl
} = inject("adminComplaintPageContext")

const feedbackText = (row) => {
  if (row?.feedbackStatus === 1) return '满意'
  if (row?.feedbackStatus === 2) return '不满意'
  return '暂无'
}

const feedbackClass = (row) => {
  if (row?.feedbackStatus === 1) return 'is-good'
  if (row?.feedbackStatus === 2) return 'is-bad'
  return ''
}

const canHandleComplaint = (row) => Number(row?.status) === 0

const handleComplaintAction = async (status) => {
  if (!currentComplaint.value) return
  const ok = await updateStatus(currentComplaint.value, status)
  if (ok) complaintDialogVisible.value = false
}
</script>

<style scoped>
.complaint-detail-dialog :deep(.el-dialog) {
  width: min(1080px, calc(100vw - 48px));
  max-height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fb;
  box-shadow: 0 24px 64px rgba(20, 31, 56, 0.22);
}

.complaint-detail-dialog :deep(.el-dialog__header) {
  flex: 0 0 auto;
  margin-right: 0;
  padding: 18px 24px 16px;
  border-bottom: 1px solid #e6eaf2;
  background: #fff;
}

.complaint-detail-dialog :deep(.el-dialog__headerbtn) {
  top: 18px;
  right: 20px;
}

.complaint-detail-dialog :deep(.el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  padding: 16px 18px;
  background:
    linear-gradient(180deg, rgba(63, 111, 217, 0.08), rgba(63, 111, 217, 0) 220px),
    #f5f7fb;
}

.complaint-detail-dialog :deep(.el-dialog__footer) {
  flex: 0 0 auto;
  padding: 12px 18px;
  border-top: 1px solid #e6eaf2;
  background: rgba(255, 255, 255, 0.94);
}

.complaint-dialog-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding-right: 42px;
}

.complaint-dialog-eyebrow,
.complaint-dialog-heading,
.complaint-overview-meta,
.complaint-linked-note,
.section-heading div,
.complaint-decision-copy,
.dialog-footer,
.feedback-status {
  display: flex;
  align-items: center;
}

.complaint-dialog-eyebrow {
  gap: 6px;
  margin-bottom: 4px;
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
}

.complaint-dialog-heading {
  gap: 10px;
}

.complaint-dialog-heading h2 {
  margin: 0;
  color: #111827;
  font-size: 20px;
  line-height: 1.25;
  font-weight: 700;
}

.complaint-dialog-heading span {
  color: #8a94a6;
  font-size: 13px;
  font-weight: 700;
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

.note-author-name {
  font-weight: 700;
  color: #202938;
}

.note-time {
  font-size: 12px;
  color: #8a94a6;
}

.note-title-full {
  margin: 0;
  color: #202938;
  font-size: 18px;
  line-height: 1.4;
}

.note-meta,
.note-comments-header,
.note-comments-title,
.note-comment-meta {
  display: flex;
  align-items: center;
}

.note-meta {
  gap: 8px;
  flex-wrap: wrap;
}

.note-media {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  overflow: hidden;
  background: #10131a;
}

.note-image {
  width: 100%;
  height: 320px;
  background: #10131a;
}

.note-video {
  width: 100%;
  max-height: 340px;
  background: #10131a;
}

.note-text {
  color: #303846;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.note-comments-panel {
  margin-top: 4px;
  padding-top: 14px;
  border-top: 1px solid #e6eaf2;
}

.note-comments-header {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.note-comments-title {
  gap: 8px;
  color: #202938;
  font-weight: 800;
}

.note-comments-title em {
  color: #8a94a6;
  font-size: 13px;
  font-style: normal;
  font-weight: 500;
}

.note-comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.note-comment-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.note-comment-row--reply {
  margin: 8px 0 0 40px;
  background: #f8faff;
}

.note-comment-avatar {
  flex: 0 0 auto;
}

.note-comment-body {
  min-width: 0;
  flex: 1 1 auto;
}

.note-comment-row.is-highlight-target {
  position: relative;
  border-color: #f56c6c;
  background: #fff4f5;
  box-shadow: inset 4px 0 0 #f56c6c, 0 8px 18px rgba(245, 108, 108, 0.12);
}

.note-comment-meta {
  gap: 7px;
  min-width: 0;
  flex-wrap: wrap;
  color: #8a94a6;
  font-size: 12px;
  line-height: 20px;
}

.note-comment-meta strong {
  max-width: 140px;
  overflow: hidden;
  color: #202938;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-comment-target-banner {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 6px;
  padding: 3px 8px;
  border-radius: 999px;
  background: #f56c6c;
  color: #fff;
  font-size: 12px;
  line-height: 18px;
  font-weight: 700;
}

.note-comment-content {
  margin-top: 4px;
  color: #303846;
  font-size: 14px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.note-comment-deleted,
.note-reply-more,
.note-comment-error {
  color: #8a94a6;
  font-size: 13px;
}

.note-reply-list {
  display: flex;
  flex-direction: column;
}

.note-reply-target {
  color: #4b5563;
}

.note-reply-more {
  margin: 8px 0 0 52px;
}

.note-comment-load-more {
  display: flex;
  justify-content: center;
  margin-top: 14px;
}

.note-comment-error {
  margin-top: 10px;
  color: #c73535;
  text-align: center;
}

.complaint-detail {
  min-height: 0;
  height: calc(100vh - 176px);
  display: grid;
  grid-template-columns: minmax(0, 1fr) 286px;
  gap: 12px;
  align-items: stretch;
}

.complaint-overview,
.complaint-decision-bar,
.complaint-section,
.side-panel {
  border: 1px solid #e4e8f0;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 10px 28px rgba(20, 31, 56, 0.06);
}

.complaint-overview {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 292px;
  gap: 16px;
  align-items: stretch;
  padding: 18px;
  border-color: rgba(63, 111, 217, 0.18);
}

.complaint-overview-main {
  min-width: 0;
}

.complaint-overview-meta {
  gap: 10px;
  margin-bottom: 12px;
  color: #7b8496;
  font-size: 13px;
}

.complaint-overview-meta :deep(.el-tag__content),
.section-heading :deep(.el-icon) {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.complaint-overview h1 {
  margin: 0 0 14px;
  color: #121826;
  font-size: 24px;
  line-height: 1.25;
  font-weight: 800;
  letter-spacing: 0;
  word-break: break-word;
}

.complaint-linked-note {
  min-width: 0;
  gap: 8px;
  color: #6b7280;
  font-size: 14px;
}

.complaint-linked-note :deep(.el-link) {
  min-width: 0;
  max-width: min(560px, 100%);
  overflow: hidden;
}

.complaint-linked-note :deep(.el-link__inner) {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.complaint-linked-note strong {
  color: #303846;
  font-weight: 600;
}

.complaint-overview-aside {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  align-content: center;
}

.complaint-overview-aside :deep(.el-button) {
  grid-column: 1 / -1;
}

.complaint-metric {
  min-width: 0;
  padding: 12px;
  border: 1px solid #edf0f6;
  border-radius: 8px;
  background: #f8faff;
}

.complaint-metric span,
.complaint-metric strong {
  display: block;
}

.complaint-metric span {
  margin-bottom: 6px;
  color: #7b8496;
  font-size: 12px;
}

.complaint-metric strong {
  color: #1f2937;
  font-size: 19px;
  line-height: 1.2;
  font-weight: 800;
}

.complaint-metric strong.is-good {
  color: #2f8f43;
}

.complaint-metric strong.is-bad {
  color: #c73535;
}

.complaint-decision-bar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  padding: 12px 14px;
  border-color: #f1d6a4;
  background: #fffaf0;
  box-shadow: none;
}

.complaint-decision-copy {
  min-width: 0;
  gap: 10px;
  color: #9a5b11;
}

.complaint-decision-copy .el-icon {
  flex: 0 0 auto;
  font-size: 22px;
}

.complaint-decision-copy strong,
.complaint-decision-copy span {
  display: block;
}

.complaint-decision-copy strong {
  color: #7a410b;
  font-size: 14px;
  line-height: 1.35;
}

.complaint-decision-copy span {
  margin-top: 2px;
  color: #9a5b11;
  font-size: 13px;
  line-height: 1.45;
}

.complaint-detail-main,
.complaint-detail-side {
  min-width: 0;
}

.complaint-detail-main,
.complaint-detail-side {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.complaint-detail-main {
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 0 4px 78px 0;
  scrollbar-width: thin;
}

.complaint-detail-main::-webkit-scrollbar {
  width: 6px;
}

.complaint-detail-main::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #cfd6e3;
}

.complaint-detail-side {
  min-height: 0;
  overflow: hidden;
  padding-bottom: 78px;
}

.complaint-section {
  padding: 16px;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.section-heading div {
  min-width: 0;
  gap: 8px;
}

.section-heading h3,
.side-panel h3 {
  margin: 0;
  color: #202938;
  font-size: 15px;
  line-height: 1.25;
  font-weight: 800;
}

.section-heading .el-icon {
  flex: 0 0 auto;
  color: var(--admin-professional-primary);
  font-size: 17px;
}

.section-heading > span {
  flex: 0 0 auto;
  color: #8a94a6;
  font-size: 13px;
  white-space: nowrap;
}

.complaint-text-block,
.reported-comment-block {
  min-height: 58px;
  padding: 14px 16px;
  border: 1px solid #edf0f6;
  border-radius: 8px;
  background: #f8faff;
  color: #303846;
  font-size: 15px;
  line-height: 1.72;
  white-space: pre-wrap;
  word-break: break-word;
}

.reported-comment-card {
  padding: 16px;
  border: 1px solid #f0d2d6;
  border-radius: 8px;
  background: #fff8f8;
}

.reported-comment-card .reported-comment-block {
  border-color: #f4c4cb;
  border-left: 4px solid #e95f70;
  background: #fff;
}

.reported-comment-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
  color: #8a94a6;
  font-size: 12px;
}

.reported-comment-author {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #7b8496;
  font-size: 13px;
}

.reported-comment-author strong {
  color: #202938;
  font-size: 14px;
  font-weight: 800;
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
  border: 1px solid #edf0f6;
  border-radius: 8px;
  background: #fff;
}

.reported-comment-context span {
  width: fit-content;
  display: block;
  margin-bottom: 7px;
  padding: 2px 7px;
  border-radius: 999px;
  background: #f3f6fb;
  color: #8a94a6;
  font-size: 12px;
  line-height: 1;
}

.reported-comment-context p {
  margin: 0;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.62;
  word-break: break-word;
}

.reported-comment-context strong {
  color: #202938;
}

.evidence-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.evidence-grid--large {
  gap: 12px;
}

.evidence-image {
  width: 92px;
  height: 92px;
  border: 1px solid #e4e8f0;
  border-radius: 8px;
  overflow: hidden;
  background: #f3f6fb;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.evidence-image:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(20, 31, 56, 0.12);
}

.evidence-image--large {
  width: 128px;
  height: 128px;
}

.compact-empty {
  min-height: 92px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px dashed #d7dde8;
  border-radius: 8px;
  background: #fafbfe;
  color: #8a94a6;
  font-size: 14px;
}

.compact-empty .el-icon {
  font-size: 18px;
}

.side-panel {
  padding: 14px;
  box-shadow: none;
}

.side-panel h3 {
  margin: 0 0 10px;
}

.side-person,
.side-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-top: 1px solid #eef2f7;
}

.side-person {
  align-items: center;
}

.side-label,
.side-row span {
  flex: 0 0 auto;
  color: #7b8496;
  font-size: 12px;
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
  color: #202938;
  font-size: 13px;
  font-weight: 700;
  text-align: right;
  overflow-wrap: anywhere;
}

.side-row strong.is-good {
  color: #2f8f43;
}

.side-row strong.is-bad {
  color: #c73535;
}

.timeline-row {
  position: relative;
  display: grid;
  grid-template-columns: 14px minmax(0, 1fr);
  gap: 9px;
  padding: 0 0 14px;
}

.timeline-row:last-child {
  padding-bottom: 0;
}

.timeline-row:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 15px;
  bottom: 2px;
  width: 1px;
  background: #dbe2ec;
}

.timeline-dot {
  width: 10px;
  height: 10px;
  margin-top: 4px;
  border-radius: 50%;
  background: var(--admin-professional-primary);
  box-shadow: 0 0 0 4px var(--admin-professional-soft);
}

.timeline-row.muted .timeline-dot {
  background: #b9c0cc;
  box-shadow: 0 0 0 4px #f1f3f6;
}

.timeline-row strong,
.timeline-row span {
  display: block;
}

.timeline-row strong {
  color: #202938;
  font-size: 13px;
  line-height: 1.3;
}

.timeline-row span {
  margin-top: 4px;
  color: #8a94a6;
  font-size: 12px;
  line-height: 1.45;
}

.feedback-status {
  min-height: 32px;
  justify-content: center;
  border: 1px solid #edf0f6;
  border-radius: 8px;
  background: #f8faff;
  color: #4b5563;
  font-size: 13px;
  font-weight: 700;
}

.side-feedback-content,
.feedback-content {
  margin: 12px 0 0;
  padding: 12px;
  border: 1px solid #edf0f6;
  border-radius: 8px;
  background: #fafbfe;
  color: #4b5563;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.side-feedback-content {
  font-size: 12px;
}

.feedback-ok {
  color: #2f8f43;
}

.feedback-bad {
  color: #c73535;
}

.dialog-footer {
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 1100px) {
  .complaint-detail {
    grid-template-columns: 1fr;
    height: auto;
    max-height: none;
    overflow: visible;
  }

  .complaint-overview {
    grid-template-columns: 1fr;
  }

  .complaint-detail-main {
    overflow: visible;
    padding-right: 0;
  }

  .complaint-detail-side {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    overflow: visible;
  }
}

@media (max-width: 760px) {
  .complaint-detail-dialog :deep(.el-dialog) {
    width: calc(100vw - 20px) !important;
    max-width: calc(100vw - 20px);
    max-height: calc(100vh - 24px);
  }

  .complaint-detail-dialog :deep(.el-dialog__header),
  .complaint-detail-dialog :deep(.el-dialog__body),
  .complaint-detail-dialog :deep(.el-dialog__footer) {
    padding-left: 14px;
    padding-right: 14px;
  }

  .complaint-dialog-title,
  .complaint-overview,
  .complaint-decision-bar,
  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .complaint-dialog-title {
    gap: 10px;
  }

  .complaint-overview {
    padding: 16px;
  }

  .complaint-overview h1 {
    font-size: 22px;
  }

  .complaint-overview-aside {
    width: 100%;
  }

  .complaint-detail-side,
  .reported-comment-context {
    display: flex;
    flex-direction: column;
  }

  .section-heading > span {
    white-space: normal;
  }

  .evidence-image--large {
    width: 104px;
    height: 104px;
  }
}
</style>
