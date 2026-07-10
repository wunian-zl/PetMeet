<template>
  <el-card shadow="never" class="filter-card">
    <div class="filter-bar">
      <div class="left-panel">
        <el-form :inline="true" class="user-filter-form">
          <el-form-item>
            <el-input
              v-model="searchKeyword"
              placeholder="搜索昵称或手机号"
              class="filter-search"
              clearable
              prefix-icon="Search"
              @input="handleFilter"
            />
          </el-form-item>
          <el-form-item label="角色">
            <el-select
              v-model="filterRole"
              placeholder="全部"
              class="filter-select filter-select-role"
              clearable
              @change="handleFilter"
            >
              <el-option label="普通用户" value="user" />
              <el-option label="管理员" value="admin" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select
              v-model="filterStatus"
              placeholder="全部"
              class="filter-select filter-select-status"
              clearable
              @change="handleFilter"
            >
              <el-option label="正常" :value="1" />
              <el-option label="封禁" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序">
            <el-select
              v-model="sortOption"
              placeholder="请选择"
              class="filter-sort"
              @change="handleFilter"
            >
              <el-option label="注册时间（最新）" value="createTime_desc" />
              <el-option label="注册时间（最早）" value="createTime_asc" />
              <el-option label="总消费（高到低）" value="totalSpent_desc" />
              <el-option label="发布内容（多到少）" value="postCount_desc" />
              <el-option label="违规次数（多到少）" value="violationCount_desc" />
              <el-option label="作品获赞总数（多到少）" value="totalLikeCount_desc" />
              <el-option label="收藏（多到少）" value="collectNoteCount_desc" />
              <el-option label="账号状态" value="status_asc" />
            </el-select>
          </el-form-item>
          <el-form-item label="注册时间">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="-"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              class="filter-date-range"
              @change="handleFilter"
            />
          </el-form-item>
        </el-form>
      </div>
      <div class="right-panel">
        <el-button type="primary" :icon="Plus" @click="handleCreate">新增用户</el-button>
        <el-button :icon="Setting" @click="tagConfigDialogVisible = true">标签规则</el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { inject } from 'vue'

const {
  searchKeyword,
  filterRole,
  filterStatus,
  sortOption,
  dateRange,
  handleFilter,
  handleCreate,
  tagConfigDialogVisible,
  Plus,
  Setting
} = inject('adminUserPageContext')
</script>
