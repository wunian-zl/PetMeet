<template>
  <el-card shadow="never">
    <el-table
      :data="tableData"
      style="width: 100%"
      :row-class-name="tableRowClassName"
      v-loading="loading"
      :empty-text="emptyText"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column label="主图" width="100">
        <template #default="{ row }">
          <el-image
            :src="resolveImageUrl(row.cover)"
            style="width: 60px; height: 60px; border-radius: 4px"
            fit="cover"
            :preview-src-list="[resolveImageUrl(row.cover)]"
            :hide-on-click-modal="true"
            preview-teleported
          >
            <template #error>
              <div
                style="
                  width: 100%;
                  height: 100%;
                  background: #f5f7fa;
                  display: flex;
                  justify-content: center;
                  align-items: center;
                  color: #909399;
                "
              >
                <el-icon><Picture /></el-icon>
              </div>
            </template>
          </el-image>
        </template>
      </el-table-column>
      <el-table-column label="商品信息" min-width="250">
        <template #default="{ row }">
          <div class="product-name" style="cursor: pointer" @click="openDialog('edit', row)">
            <div class="main-title">
              {{ row.name }}
              <span style="font-size: 12px; color: #909399; font-weight: normal; margin-left: 5px">
                (ID: {{ row.id }})
              </span>
            </div>
            <div class="sub-title">{{ row.subtitle }}</div>
            <!-- 3. 标签 -->
            <div class="tags-row" style="margin-top: 5px">
              <el-tag
                v-if="row.sales > 500"
                size="small"
                type="danger"
                effect="plain"
                style="margin-right: 5px"
                >热卖</el-tag
              >
              <el-tag
                v-if="row.stock === 0"
                size="small"
                type="danger"
                effect="dark"
                style="margin-right: 5px"
                >无库存</el-tag
              >
              <el-tag
                v-else-if="row.stock <= (row.warningStock || 10)"
                size="small"
                type="warning"
                effect="plain"
                style="margin-right: 5px"
                >库存紧张</el-tag
              >
              <el-tag v-if="!row.status" size="small" type="info" effect="plain">已下架</el-tag>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="数据概览" width="150">
        <template #default="{ row }">
          <div style="font-size: 13px; line-height: 1.6">
            <div style="color: #f56c6c; font-weight: bold; font-size: 15px">
              ¥{{ row.price }}
              <span style="font-size: 12px; font-weight: normal; color: #909399"
                >/ {{ row.unit || '件' }}</span
              >
            </div>
            <div style="color: #606266">库存: {{ row.stock }}</div>
            <div style="color: #909399; font-size: 12px; display: flex; gap: 8px">
              <span>销量: {{ row.sales || 0 }}</span>
              <span>浏览: {{ row.views || 0 }}</span>
            </div>
            <div style="color: #909399; font-size: 12px">
              笔记关联: {{ row.relatedNoteCount || 0 }}
              <span v-if="row.relatedNoteCount > 10" style="color: #f56c6c">🔥</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="上架状态" width="100">
        <template #default="{ row }">
          <!-- 4. 上下架切换（下架前二次确认） -->
          <el-switch
            v-model="row.status"
            :before-change="() => beforeStatusChange(row)"
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDialog('edit', row)">编辑</el-button>
          <!-- 5. 删除确认（带提醒） -->
          <el-popconfirm
            title="确定删除该商品吗?"
            confirm-button-type="danger"
            width="220"
            :icon="InfoFilled"
            icon-color="#F56C6C"
            cancel-button-text="取消"
            confirm-button-text="确认删除"
            @confirm="handleDelete(row)"
          >
            <template #reference>
              <el-button type="danger" link>删除</el-button>
            </template>
            <div style="font-size: 12px; color: #909399; margin-top: 5px">
              删除后无法恢复，仅保留历史订单
            </div>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 6. 分页 -->
    <div class="pagination-container" v-if="tableData.length > 0">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        v-model:current-page="currentPage"
        :page-size="pageSize"
        @current-change="handlePageChange"
      />
    </div>
  </el-card>
</template>

<script setup>
import { inject } from 'vue'

const {
  openDialog,
  tableData,
  tableRowClassName,
  loading,
  emptyText,
  handleSelectionChange,
  resolveImageUrl,
  Picture,
  beforeStatusChange,
  handleStatusChange,
  InfoFilled,
  handleDelete,
  total,
  currentPage,
  pageSize,
  handlePageChange
} = inject('adminProductPageContext')
</script>
