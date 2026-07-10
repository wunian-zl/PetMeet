<template>
  <el-dialog v-model="rejectDialogVisible" title="拒绝理由" width="450px">
    <el-form :model="rejectForm" ref="rejectFormRef">
      <el-form-item prop="reason" :rules="[{ required: true, message: '请填写拒绝理由', trigger: 'blur' }]">
        <el-input
          v-model="rejectForm.reason"
          type="textarea"
          :rows="3"
          placeholder="请填写具体违规点(如:图片不适/广告营销)..."
        />
      </el-form-item>

      <el-form-item label="违规定性">
        <el-checkbox v-model="rejectForm.isViolation">计入用户违规记录</el-checkbox>
        <el-select
          v-show="rejectForm.isViolation"
          v-model="rejectForm.violationType"
          placeholder="违规类型"
          size="small"
          style="width:150px;margin-left:10px"
        >
          <el-option label="广告垃圾" value="广告垃圾" />
          <el-option label="违规推广" value="违规推广" />
          <el-option label="色情低俗" value="色情低俗" />
          <el-option label="辱骂攻击" value="辱骂攻击" />
          <el-option label="政治敏感" value="政治敏感" />
          <el-option label="其他违规" value="其他违规" />
        </el-select>
      </el-form-item>

      <div class="quick-reasons">
        <div class="quick-reasons-title">常用语:</div>
        <div class="tags">
          <el-tag
            v-for="tag in quickReasons"
            :key="tag"
            type="info"
            size="small"
            effect="plain"
            class="reason-tag"
            @click="handleQuickReason(tag)"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确认拒绝</el-button>
      </span>
    </template>
  </el-dialog>

  <el-dialog
    v-model="detailDialogVisible"
    width="1040px"
    top="4vh"
    class="note-detail-dialog"
    :show-close="false"
    destroy-on-close
  >
    <template #header>
      <div class="detail-dialog-header">
        <div class="header-copy">
          <span class="header-title">内容详情</span>
          <span v-if="currentNote" class="header-subtitle">ID{{ currentNote.id }}</span>
        </div>
        <button type="button" class="dialog-close" aria-label="关闭详情弹窗" @click="detailDialogVisible = false">
          <el-icon><Close /></el-icon>
        </button>
      </div>
    </template>

    <div v-if="currentNote" class="detail-container">
      <div class="status-stack">
        <el-alert
          v-if="currentNote.status === 'pending'"
          title="机器检测:图片正常，文本疑似含少量广告词(置信度60%)"
          type="warning"
          :closable="false"
          show-icon
        />
        <el-alert v-if="currentNote.status === 'rejected'" type="error" :closable="false" show-icon>
          <template #title>{{ buildAuditTitle(currentNote, 'rejected') }}</template>
          <div>原因:{{ currentNote.rejectReason || '未填写' }}</div>
        </el-alert>
        <el-alert v-if="currentNote.status === 'approved'" type="success" :closable="false" show-icon>
          <template #title>{{ buildAuditTitle(currentNote, 'approved') }}</template>
        </el-alert>
        <el-alert v-if="currentNote.isShielded" title="该内容已下架，前台暂不可见" type="info" :closable="false" show-icon />
        <el-alert v-if="currentNote.status === 'user_off_shelf'" title="用户已主动下架该内容" type="warning" :closable="false" show-icon />
        <el-alert v-if="currentNote.status === 'user_deleted'" title="用户已删除该内容" type="error" :closable="false" show-icon />
        <el-alert v-if="currentNote.status === 'admin_soft_deleted'" title="管理员已删除该内容" type="error" :closable="false" show-icon />
      </div>

      <div class="detail-layout">
        <section class="media-column">
          <div class="media-stage">
            <div v-if="currentNote.type === 'video' && currentNote.videoUrl" class="media-video">
              <video
                class="media-video-player"
                :src="resolveImageUrl(currentNote.videoUrl)"
                :poster="resolveImageUrl(currentNote.cover)"
                controls
                playsinline
              />
            </div>

            <template v-else>
              <el-carousel
                v-if="mediaItems.length > 1"
                trigger="click"
                height="100%"
                :autoplay="false"
                arrow="hover"
                indicator-position="outside"
                class="media-carousel"
              >
                <el-carousel-item v-for="(img, index) in mediaItems" :key="`${img}-${index}`">
                  <el-image
                    class="detail-image"
                    :src="resolveImageUrl(img)"
                    :preview-src-list="previewImageList"
                    :initial-index="index"
                    fit="contain"
                    preview-teleported
                    :hide-on-click-modal="true"
                  />
                </el-carousel-item>
              </el-carousel>

              <div v-else-if="mediaItems.length === 1" class="single-image-wrap">
                <el-image
                  class="detail-image"
                  :src="resolveImageUrl(mediaItems[0])"
                  :preview-src-list="previewImageList"
                  fit="contain"
                  preview-teleported
                  :hide-on-click-modal="true"
                />
              </div>

              <div v-else class="media-empty">暂无图片</div>
            </template>

            <div v-if="mediaItems.length > 1" class="media-count">共{{ mediaItems.length }}张</div>
          </div>
        </section>

        <aside class="info-column">
          <div class="author-panel">
            <UserInfoPopover v-if="currentNote.userId" :user-id="currentNote.userId" placement="left" :width="340">
              <template #reference>
                <el-avatar :size="44" :src="resolveImageUrl(currentNote.avatar)" class="author-avatar" />
              </template>
            </UserInfoPopover>
            <el-avatar v-else :size="44" :src="resolveImageUrl(currentNote.avatar)" />
            <div class="author-copy">
              <div class="author-name">
                {{ currentNote.author }}
                <el-tag v-if="getUserViolationCount(currentNote.author) > 0" type="danger" size="small" effect="dark" round>
                  历史违规{{ getUserViolationCount(currentNote.author) }}
                </el-tag>
              </div>
              <div class="publish-time">{{ currentNote.createTime }} · {{ typeLabel }}</div>
            </div>
          </div>

          <section class="note-panel">
            <h3 class="detail-title">{{ currentNote.title }}</h3>
            <div v-if="currentNote.category || (currentNote.tags && currentNote.tags.length)" class="detail-meta">
              <span v-if="currentNote.category" class="meta-chip category-chip">分类{{ currentNote.category }}</span>
              <span v-for="tag in (currentNote.tags || [])" :key="tag" class="meta-chip tag-chip">#{{ tag }}</span>
            </div>
            <p class="detail-desc">{{ currentNote.content || '暂无正文' }}</p>
          </section>

          <section v-if="currentNote.productCount > 0" class="products-panel">
            <div class="section-head">
              <div class="section-title">
                <el-icon><Goods /></el-icon>
                关联商品({{ currentNote.productCount }})
              </div>
            </div>
            <div class="detail-product-list">
              <button
                v-for="prod in currentNote.products"
                :key="prod.id"
                type="button"
                class="detail-product-card"
                @click="goToProduct(prod.id)"
              >
                <el-image :src="resolveImageUrl(prod.cover)" class="product-cover" fit="cover" />
                <span class="prod-detail">
                  <span class="p-name">{{ prod.name }}</span>
                  <span class="p-price">¥{{ prod.price }}</span>
                </span>
                <span class="product-go">查看</span>
              </button>
            </div>
          </section>

          <section class="comments-panel" v-if="currentNote.status === 'approved'">
            <div class="section-head">
              <div class="section-title">
                <el-icon><ChatDotRound /></el-icon>
                评论({{ commentTotal }})
              </div>
              <el-button size="small" link @click="reloadComments">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>

            <div v-if="commentLoading && comments.length === 0" class="comments-loading">
              <el-skeleton :rows="3" animated />
            </div>

            <div v-else-if="comments.length === 0" class="comments-empty">
              <el-empty description="暂无评论" :image-size="72" />
            </div>

            <div v-else class="comment-list">
              <article v-for="comment in comments" :key="comment.id" class="comment-item">
                <el-avatar
                  v-if="!comment.deleted"
                  :size="34"
                  :src="resolveImageUrl(comment.userAvatar)"
                  class="comment-avatar"
                />
                <div class="comment-body">
                  <div v-if="comment.deleted" class="comment-deleted">评论已删除</div>
                  <template v-else>
                    <div class="comment-meta">
                      <span class="comment-user">{{ comment.userNickname || '用户' }}</span>
                      <el-tag v-if="comment.author" size="small" effect="plain">作者</el-tag>
                      <span class="comment-time">{{ formatDateTime(comment.createTime) }}</span>
                    </div>
                    <div class="comment-text">{{ comment.content }}</div>
                  </template>

                  <div v-if="comment.replyCount > 0 || (comment.replies && comment.replies.length)" class="comment-replies">
                    <div v-for="reply in comment.replies" :key="reply.id" class="comment-reply">
                      <div class="reply-line">
                        <span class="reply-user">{{ reply.userNickname || '用户' }}</span>
                        <el-tag v-if="reply.author" size="small" effect="plain">作者</el-tag>
                        <span v-if="reply.replyToNickname" class="reply-target">回复@{{ reply.replyToNickname }}:</span>
                        <span class="reply-content">{{ reply.content }}</span>
                      </div>
                      <button
                        v-if="!reply.deleted"
                        type="button"
                        class="reply-delete"
                        @click="handleDeleteComment(reply, comment)"
                      >
                        删除
                      </button>
                    </div>

                    <div class="reply-actions" v-if="comment.replyCount > 0">
                      <el-button
                        v-if="!comment.replyExpanded && getHiddenReplyCount(comment) > 0"
                        type="primary"
                        link
                        size="small"
                        :loading="comment.replyLoading"
                        @click="toggleCommentReplies(comment)"
                      >
                        展开剩余{{ getHiddenReplyCount(comment) }}条回复
                      </el-button>
                      <template v-else-if="comment.replyExpanded">
                        <el-button
                          v-if="canLoadMoreReplies(comment)"
                          type="primary"
                          link
                          size="small"
                          :loading="comment.replyLoading"
                          @click="loadMoreCommentReplies(comment)"
                        >
                          继续加载{{ getRemainingReplyCount(comment) }}条回复
                        </el-button>
                        <el-button link size="small" @click="toggleCommentReplies(comment)">收起回复</el-button>
                      </template>
                    </div>
                  </div>
                </div>
                <el-button v-if="!comment.deleted" type="danger" link size="small" @click="handleDeleteComment(comment)">
                  删除
                </el-button>
              </article>
            </div>

            <div class="comment-load-more" v-if="commentHasMore">
              <el-button size="small" :loading="commentLoading" @click="loadMoreComments">{{ commentLoadLabel }}</el-button>
            </div>
          </section>

          <section v-else class="comments-panel comments-compact">
            <div class="comments-empty-compact">未审核通过，暂无评论</div>
          </section>

          <section v-if="currentNote.status === 'rejected'" class="appeal-panel">
            <div class="appeal-row">
              <span>用户申诉记录(0)</span>
              <el-button link type="primary" size="small">查看详情</el-button>
            </div>
            <div class="appeal-empty">暂无申诉</div>
          </section>
        </aside>
      </div>
    </div>

    <template #footer>
      <div class="detail-footer">
        <div class="footer-id">ID{{ currentNote?.id }}</div>
        <div>
          <template v-if="currentNote?.status === 'pending'">
            <el-button @click="detailDialogVisible = false">取消</el-button>
            <el-button type="danger" @click="openRejectDialog(currentNote)">拒绝</el-button>
            <el-button type="success" @click="handleApprove(currentNote); detailDialogVisible = false">
              通过&发布
            </el-button>
          </template>
          <template v-else>
            <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
          </template>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, inject } from 'vue'

const {
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
  formatDateTime,
  buildAuditTitle,
  goToProduct,
  handleApprove,
  openRejectDialog,
  confirmReject,
  UserInfoPopover,
  ChatDotRound,
  Close,
  resolveImageUrl
} = inject("adminContentPageContext")

const mediaItems = computed(() => {
  const note = currentNote.value
  if (!note || note.type === 'video') return []
  const rawImages = Array.isArray(note.images) ? note.images : []
  const images = rawImages.filter(Boolean)
  if (images.length === 0 && note.cover) images.push(note.cover)
  return [...new Set(images)]
})

const previewImageList = computed(() => mediaItems.value.map((img) => resolveImageUrl(img)))

const typeLabel = computed(() => {
  if (currentNote.value?.type === 'video') return '视频'
  if (currentNote.value?.type === 'mixed') return '图文+视频'
  return '图文'
})
</script>

<style scoped>
.quick-reasons-title {
  margin-bottom: 5px;
  color: #909399;
  font-size: 12px;
}

.reason-tag {
  margin: 0 6px 6px 0;
  cursor: pointer;
}

:deep(.note-detail-dialog .el-dialog) {
  max-width: calc(100vw - 48px);
  border-radius: 14px;
  overflow: hidden;
}

:deep(.note-detail-dialog .el-dialog__header) {
  margin: 0;
  padding: 0;
  border-bottom: 1px solid #edf0f5;
}

:deep(.note-detail-dialog .el-dialog__body) {
  padding: 0;
  background: #f6f8fb;
}

:deep(.note-detail-dialog .el-dialog__footer) {
  padding: 12px 18px;
  border-top: 1px solid #edf0f5;
}

.detail-dialog-header {
  height: 58px;
  padding: 0 18px 0 20px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-copy {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.header-title {
  color: #232936;
  font-size: 16px;
  font-weight: 700;
}

.header-subtitle,
.footer-id {
  color: #8a93a3;
  font-size: 12px;
}

.dialog-close {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #606a7a;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.dialog-close:hover {
  background: #f0f3f8;
  color: #232936;
}

.detail-container {
  height: min(720px, calc(92vh - 122px));
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.status-stack {
  padding: 14px 16px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-layout {
  min-height: 0;
  padding: 16px;
  flex: 1;
  display: grid;
  grid-template-columns: minmax(430px, 1fr) 410px;
  gap: 16px;
}

.media-column,
.info-column {
  min-height: 0;
}

.media-stage {
  position: relative;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border-radius: 10px;
  background: #111827;
  display: flex;
  align-items: center;
  justify-content: center;
}

.media-video,
.single-image-wrap,
.media-carousel,
.media-carousel :deep(.el-carousel__container) {
  width: 100%;
  height: 100%;
}

.media-video-player,
.detail-image {
  width: 100%;
  height: 100%;
  background: #111827;
}

.detail-image :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
}

.media-count {
  position: absolute;
  right: 14px;
  bottom: 14px;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.76);
  color: #fff;
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  backdrop-filter: blur(8px);
}

.media-empty {
  color: #cbd5e1;
  font-size: 14px;
}

.info-column {
  overflow-y: auto;
  padding-right: 4px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-column::-webkit-scrollbar {
  width: 6px;
}

.info-column::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #d7dde7;
}

.author-panel,
.note-panel,
.products-panel,
.comments-panel,
.appeal-panel {
  border: 1px solid #edf0f5;
  border-radius: 10px;
  background: #fff;
}

.author-panel {
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-avatar {
  cursor: pointer;
}

.author-copy {
  min-width: 0;
}

.author-name {
  min-width: 0;
  color: #232936;
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.publish-time {
  margin-top: 3px;
  color: #8a93a3;
  font-size: 12px;
}

.note-panel {
  padding: 16px;
}

.detail-title {
  margin: 0 0 10px;
  color: #1f2937;
  font-size: 20px;
  font-weight: 750;
  line-height: 1.35;
}

.detail-meta {
  margin-bottom: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.meta-chip {
  max-width: 100%;
  min-height: 24px;
  padding: 2px 8px;
  border-radius: 6px;
  background: #f3f6fb;
  color: #64748b;
  display: inline-flex;
  align-items: center;
  overflow: hidden;
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
}

.category-chip {
  background: #ecfdf5;
  color: #047857;
}

.tag-chip {
  background: #eff6ff;
  color: #315cb6;
}

.detail-desc {
  margin: 0;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.75;
  white-space: pre-wrap;
}

.products-panel,
.comments-panel,
.appeal-panel {
  padding: 14px;
}

.section-head {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-title {
  color: #232936;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 700;
}

.detail-product-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-product-card {
  width: 100%;
  min-height: 62px;
  padding: 8px;
  border: 1px solid #eef1f5;
  border-radius: 8px;
  background: #fbfcff;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  transition: border-color 0.18s ease, background-color 0.18s ease;
}

.detail-product-card:hover {
  border-color: #bdd0ff;
  background: #f5f8ff;
}

.product-cover {
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  border-radius: 7px;
  overflow: hidden;
}

.prod-detail {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.p-name {
  overflow: hidden;
  color: #2f3746;
  font-size: 13px;
  font-weight: 650;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.p-price {
  color: #e05252;
  font-size: 12px;
  font-weight: 700;
}

.product-go {
  color: var(--admin-professional-primary);
  font-size: 12px;
  font-weight: 700;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.comment-item {
  padding: 12px;
  border: 1px solid #edf0f5;
  border-radius: 9px;
  background: #fbfcff;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.comment-body {
  min-width: 0;
  flex: 1;
}

.comment-meta {
  margin-bottom: 5px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  color: #8a93a3;
  font-size: 12px;
}

.comment-user,
.reply-user {
  color: #2f3746;
  font-weight: 700;
}

.comment-text {
  color: #4b5563;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.comment-deleted {
  color: #a8abb2;
  font-size: 13px;
  line-height: 24px;
}

.comment-replies {
  margin-top: 10px;
  padding: 9px 10px;
  border-radius: 8px;
  background: #f4f7fb;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.comment-reply {
  color: #5f6877;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  line-height: 1.55;
}

.reply-line {
  min-width: 0;
  flex: 1;
}

.reply-target {
  color: #8a93a3;
}

.reply-content {
  word-break: break-word;
}

.reply-delete {
  margin-top: 1px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #b2bac8;
  cursor: pointer;
  flex: 0 0 auto;
  font-size: 12px;
  line-height: 20px;
}

.reply-delete:hover {
  color: #d94f4f;
}

.reply-actions {
  padding-top: 2px;
  display: flex;
  align-items: center;
  gap: 10px;
  line-height: 20px;
}

.reply-actions :deep(.el-button) {
  height: 20px;
  padding: 0;
}

.comments-loading,
.comments-empty {
  padding: 8px 0;
}

.comment-load-more {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}

.comments-compact {
  color: #8a93a3;
  font-size: 13px;
}

.appeal-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #7b8494;
  font-size: 13px;
}

.appeal-empty {
  margin-top: 6px;
  color: #b0b7c3;
  font-size: 12px;
}

.detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

@media (max-width: 900px) {
  .detail-container {
    height: auto;
    max-height: calc(94vh - 122px);
    overflow-y: auto;
  }

  .detail-layout {
    max-height: none;
    grid-template-columns: 1fr;
  }

  .media-stage {
    min-height: 360px;
    height: min(58vh, 520px);
  }

  .info-column {
    overflow: visible;
  }
}
</style>
