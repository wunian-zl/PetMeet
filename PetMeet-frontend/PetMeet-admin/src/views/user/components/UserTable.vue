<template>
  <el-card shadow="never" class="table-card">
    <el-table :data="tableData" class="user-table" style="width: 100%" v-loading="loading">
      <el-table-column label="用户" min-width="190">
        <template #default="{ row }">
          <div class="user-info-cell">
            <el-avatar
              :src="resolveImageUrl(row.avatar)"
              :size="40"
              fit="cover"
              class="user-avatar"
              style="cursor: pointer"
              @click="openDetailDialog(row)"
            />
            <div class="info-text">
              <div
                class="nickname"
                style="cursor: pointer; color: var(--admin-professional-primary)"
                @click="openDetailDialog(row)"
              >
                {{ row.username }}
              </div>
              <div class="email">{{ row.email }}</div>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="phone" label="手机号" width="120" />

      <el-table-column label="角色" width="96" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.role === 'admin'" type="danger" effect="dark">管理员</el-tag>
          <el-tag v-else type="info" effect="plain">普通用户</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="用户标签" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">
          <div style="display: flex; flex-wrap: wrap; gap: 4px">
            <!-- 自动标签（带提示） -->
            <el-tooltip
              v-for="tag in getAutoTags(row)"
              :key="tag.label"
              :content="tag.desc"
              placement="top"
              effect="dark"
            >
              <el-tag
                :type="tag.type"
                size="small"
                effect="plain"
                :style="{ cursor: tag.clickable ? 'pointer' : 'help' }"
                @click="tag.clickable ? tag.onClick(row) : null"
              >
                {{ tag.label }}
              </el-tag>
            </el-tooltip>
            <!-- 手动标签 -->
            <el-tag
              v-for="tag in row.tags ? row.tags.split(',') : []"
              :key="tag"
              size="small"
              effect="light"
            >
              {{ tag }}
            </el-tag>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="84" align="center">
        <template #default="{ row }">
          <el-tooltip
            v-if="!row.status"
            :content="'封禁原因: ' + (row.banReason || '未填写')"
            placement="top"
          >
            <el-tag type="danger" size="small" effect="plain" style="cursor: help">已封禁</el-tag>
          </el-tooltip>
          <el-tag v-else type="success" size="small" effect="plain">正常</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="最后登录" width="145" sortable prop="lastLoginTime">
        <template #default="{ row }">
          <div v-if="row.lastLoginTime">
            <div>{{ row.lastLoginTime }}</div>
            <div style="font-size: 12px; color: #909399">
              {{ formatTimeAgo(row.lastLoginTime) }}
            </div>
          </div>
          <span v-else>-</span>
        </template>
      </el-table-column>

      <el-table-column prop="createTime" label="注册时间" width="150" sortable />

      <el-table-column label="操作" width="128" fixed="right" align="center">
        <template #default="{ row }">
          <div class="row-actions">
            <el-button type="primary" link size="small" @click="openEditDialog(row)"
              >编辑</el-button
            >
            <el-button
              :type="row.status ? 'danger' : 'success'"
              link
              size="small"
              @click="handleStatusAction(row)"
            >
              {{ row.status ? '封禁' : '解封' }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </div>
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
  tableData,
  loading,
  resolveImageUrl,
  openDetailDialog,
  getAutoTags,
  formatTimeAgo,
  openEditDialog,
  handleStatusAction,
  handleDelete,
  total,
  pageSize,
  currentPage,
  handlePageChange
} = inject('adminUserPageContext')
</script>
