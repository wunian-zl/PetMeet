<template>
  <el-card shadow="never">
    <el-table
      :data="tableData"
      style="width: 100%"
      v-loading="loading"
      size="default"
      table-layout="fixed"
      @selection-change="handleSelectionChange"
      :empty-text="
        searchKeyword ||
        filterStatus !== 'all' ||
        filterType ||
        filterProduct ||
        (dateRange && dateRange.length > 0)
          ? '未找到相关内容，请调整筛选条件'
          : '暂无数据'
      "
    >
      <el-table-column type="selection" width="42" />
      <el-table-column prop="title" label="内容信息" min-width="430">
        <template #default="{ row }">
          <div class="note-content-cell">
            <div class="cover-frame">
              <el-image
                :src="resolveImageUrl(row.cover)"
                :preview-src-list="[resolveImageUrl(row.cover)]"
                class="cover-image"
                fit="cover"
                :hide-on-click-modal="true"
                preview-teleported
              />
              <div v-if="row.type === 'video'" class="cover-type-badge">
                <el-icon><VideoCamera /></el-icon>
              </div>
            </div>
            <div class="note-info-cell">
              <div class="note-title-row">
                <div class="note-title-main" @click="openDetailDialog(row)">
                  <el-tag
                    v-if="row.isSticky && row.status === 'approved'"
                    type="danger"
                    size="small"
                    effect="dark"
                    >置顶</el-tag
                  >
                  <el-tag
                    v-if="row.isRecommended && row.status === 'approved'"
                    type="warning"
                    size="small"
                    effect="dark"
                    >推荐</el-tag
                  >
                  <el-tooltip :content="row.title" placement="top" effect="dark" :show-after="200">
                    <span class="title-text">{{ row.title }}</span>
                  </el-tooltip>
                </div>
                <el-tooltip
                  v-if="row.status === 'rejected'"
                  class="box-item"
                  effect="dark"
                  :content="'拒绝原因: ' + (row.rejectReason || '未填写')"
                  placement="top"
                >
                  <el-tag type="danger" size="small" effect="plain" class="reject-tip"
                    >已拒绝</el-tag
                  >
                </el-tooltip>
              </div>
              <div v-if="row.content" class="note-excerpt">{{ row.content }}</div>
              <div class="note-meta-grid">
                <span class="meta-pill">ID{{ row.id }}</span>
                <span class="meta-pill">{{ getTypeLabel(row.type) }}</span>
                <span v-if="row.category" class="meta-pill meta-category"
                  >分类{{ row.category }}</span
                >
                <el-tooltip
                  v-if="row.tags && row.tags.length > 0"
                  :content="row.tags.join('、')"
                  placement="top"
                  effect="dark"
                  :show-after="200"
                >
                  <span class="meta-pill meta-tags"> 标签{{ getCompactTags(row.tags) }} </span>
                </el-tooltip>
                <el-popover
                  v-if="row.productCount > 0"
                  placement="right"
                  :width="300"
                  trigger="click"
                  popper-class="product-preview-popover"
                >
                  <template #reference>
                    <span class="product-link">
                      <el-icon><Goods /></el-icon>关联{{ row.productCount }}件商品
                    </span>
                  </template>
                  <div class="product-preview-list">
                    <div
                      v-for="prod in row.products"
                      :key="prod.id"
                      class="product-mini-card"
                      @click.stop="goToProduct(prod.id)"
                    >
                      <el-image
                        :src="resolveImageUrl(prod.cover)"
                        style="width: 40px; height: 40px; border-radius: 4px"
                        fit="cover"
                      />
                      <div class="prod-info">
                        <div class="prod-name">{{ prod.name }}</div>
                        <div class="prod-price">¥{{ prod.price }}</div>
                      </div>
                    </div>
                  </div>
                </el-popover>
              </div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="运营信息" width="250">
        <template #default="{ row }">
          <div class="ops-cell">
            <div class="ops-row">
              <el-tag :type="getStatusType(row.status)" size="small">{{
                getStatusText(row.status)
              }}</el-tag>
              <el-tag v-if="row.isShielded" type="info" size="small" effect="dark">
                <el-icon><Hide /></el-icon>已下架
              </el-tag>
            </div>
            <UserInfoPopover v-if="row.userId" :user-id="row.userId" placement="left" :width="340">
              <template #reference>
                <div class="publisher-cell">
                  <el-avatar :size="24" :src="resolveImageUrl(row.avatar)" />
                  <span>{{ row.author }}</span>
                </div>
              </template>
            </UserInfoPopover>
            <div v-else class="publisher-cell">
              <el-avatar :size="24" :src="resolveImageUrl(row.avatar)" />
              <span>{{ row.author }}</span>
            </div>
            <div class="ops-row muted">
              <span>发布{{ getShortDateTime(row.createTime) }}</span>
              <span>浏览{{ row.views || 0 }}</span>
              <span>赞{{ row.likes || 0 }}</span>
            </div>
            <div v-if="row.auditTimeDisplay" class="ops-row muted">
              <span>审核{{ row.auditTimeDisplay.split(' ')[0] }}</span>
              <span>{{ row.auditor || '系统' }}</span>
              <el-tag v-if="row.auditIsFallback" size="small" type="info" effect="plain"
                >历史</el-tag
              >
            </div>
            <div v-else class="ops-row muted">暂无审核记录</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="推荐位" width="96">
        <template #default="{ row }">
          <div class="switch-cell">
            <label>
              <span>置顶</span>
              <el-switch
                v-model="row.isSticky"
                :disabled="row.status !== 'approved' || row.isShielded"
                size="small"
                @change="handleStickyChange(row)"
              />
            </label>
            <label>
              <span>推荐</span>
              <el-switch
                v-model="row.isRecommended"
                :disabled="row.status !== 'approved' || row.isShielded"
                size="small"
                active-color="#E6A23C"
                @change="handleRecommendChange(row)"
              />
            </label>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="96">
        <template #default="{ row }">
          <div
            class="action-buttons"
            :class="{ 'action-buttons-pending': row.status === 'pending' }"
          >
            <template v-if="row.status === 'pending'">
              <el-button type="primary" size="small" @click="openDetailDialog(row)">审核</el-button>
              <el-button type="danger" link @click="handleSoftDelete(row)">删除</el-button>
            </template>
            <template v-else>
              <el-button type="info" link @click="openDetailDialog(row)">详情</el-button>
              <el-button
                v-if="row.status === 'approved' || row.status === 'shielded'"
                :type="row.isShielded ? 'success' : 'danger'"
                link
                @click="handleToggleShield(row)"
              >
                {{ row.isShielded ? '恢复' : '下架' }}
              </el-button>
              <el-button
                v-if="row.status !== 'admin_soft_deleted'"
                class="delete-action"
                type="danger"
                link
                @click="handleSoftDelete(row)"
              >
                删除
              </el-button>
            </template>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-container">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        v-model:current-page="currentPage"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </el-card>
</template>

<script setup>
import { inject } from 'vue'

const {
  filterStatus,
  filterType,
  filterProduct,
  dateRange,
  searchKeyword,
  loading,
  tableData,
  currentPage,
  pageSize,
  total,
  goToProduct,
  handlePageChange,
  handleSizeChange,
  handleSelectionChange,
  getStatusType,
  getStatusText,
  handleStickyChange,
  handleRecommendChange,
  openDetailDialog,
  handleToggleShield,
  handleSoftDelete,
  UserInfoPopover,
  VideoCamera,
  Hide,
  resolveImageUrl
} = inject('adminContentPageContext')

const getTypeLabel = (type) => {
  if (type === 'video') return '视频'
  if (type === 'mixed') return '图文+视频'
  return '图文'
}

const getCompactTags = (tags) => {
  const list = Array.isArray(tags) ? tags.filter(Boolean) : []
  if (list.length <= 2) return list.join('、')
  return `${list.slice(0, 2).join('、')}等${list.length}个`
}

const getShortDateTime = (value) => {
  if (!value) return '-'
  const text = String(value)
  const parts = text.split(' ')
  if (parts.length < 2) return text
  return `${parts[0].slice(5)} ${parts[1].slice(0, 5)}`
}
</script>

<style scoped>
.note-info-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.note-content-cell {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}

.cover-frame {
  position: relative;
  width: 60px;
  height: 60px;
  flex: 0 0 auto;
  overflow: hidden;
  border-radius: 8px;
  background: #eef1f6;
}

.cover-image {
  width: 100%;
  height: 100%;
  display: block;
  border-radius: 8px;
}

.cover-type-badge {
  position: absolute;
  right: 4px;
  bottom: 4px;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: rgba(21, 28, 42, 0.72);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}

.note-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.note-title-main {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #232936;
  cursor: pointer;
}

.title-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 650;
  line-height: 22px;
}

.note-title-main:hover .title-text {
  color: var(--admin-professional-primary);
}

.reject-tip {
  flex: 0 0 auto;
  cursor: help;
}

.note-excerpt {
  max-width: 100%;
  overflow: hidden;
  color: #606a7a;
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-meta-grid {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.meta-pill,
.product-link {
  max-width: 100%;
  min-width: 0;
  height: 24px;
  padding: 0 8px;
  border-radius: 6px;
  background: #f5f7fb;
  color: #667085;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
  font-size: 12px;
  line-height: 24px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.meta-category,
.meta-tags {
  max-width: min(300px, 100%);
}

.product-link {
  color: #b7791f;
  background: #fff7e8;
  cursor: pointer;
  transition:
    background-color 0.2s,
    color 0.2s;
}

.product-link:hover {
  color: #8a560d;
  background: #ffefd1;
}

.product-mini-card {
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: background-color 0.18s ease;
}

.product-mini-card:hover {
  background: #f6f8fb;
}

.product-mini-card .prod-info {
  min-width: 0;
  flex: 1;
}

.product-mini-card .prod-name {
  overflow: hidden;
  color: #2f3746;
  font-size: 13px;
  font-weight: 650;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.product-mini-card .prod-price {
  margin-top: 2px;
  color: #e05252;
  font-size: 12px;
  font-weight: 700;
}

.pagination-container {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.publisher-cell {
  min-width: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}

.publisher-cell span {
  min-width: 0;
  overflow: hidden;
  color: #4b5563;
  font-size: 13px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.ops-cell {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ops-row {
  min-width: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px 8px;
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.ops-row.muted {
  color: #7b8494;
}

.switch-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.switch-cell label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #5f6877;
  font-size: 12px;
  line-height: 20px;
}

:deep(.el-table .cell) {
  padding-left: 8px;
  padding-right: 8px;
}

:deep(.el-table .el-table__header .cell) {
  padding-top: 6px;
  padding-bottom: 6px;
}

:deep(.el-table .el-table__body .cell) {
  padding-top: 8px;
  padding-bottom: 8px;
}

.action-buttons {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 4px;
}

.action-buttons :deep(.el-button) {
  min-width: 0;
  height: 22px;
  margin-left: 0;
  padding-left: 0;
  padding-right: 0;
  font-size: 13px;
}

.action-buttons .delete-action {
  color: #f56c6c;
}

.action-buttons-pending {
  align-items: flex-start;
  gap: 4px;
}

:deep(.el-table__body tr) {
  --el-table-tr-bg-color: #fff;
}

:deep(.el-table__body tr:hover > td.el-table__cell) {
  background-color: #f8fbff;
}
</style>
