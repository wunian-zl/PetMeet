<template>
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
</template>

<script setup>
import { inject } from 'vue'

const {
  loading,
  tableData,
  currentPage,
  pageSize,
  total,
  statusText,
  statusType,
  isCommentComplaint,
  evidenceImagesOf,
  resolvedEvidenceImages,
  canDeleteComplaint,
  openNoteDialog,
  oneLineText,
  openComplaintDialog,
  handlePageChange,
  handleSelectionChange,
  handleDelete,
  updateStatus,
  UserInfoPopover,
  resolveImageUrl
} = inject("adminComplaintPageContext")
</script>
