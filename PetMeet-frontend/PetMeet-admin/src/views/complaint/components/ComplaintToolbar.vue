<template>
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
</template>

<script setup>
import { inject } from 'vue'

const {
  keyword,
  statusFilter,
  selectedRows,
  deletableSelectedIds,
  handleFilter,
  handleBatchDelete
} = inject("adminComplaintPageContext")
</script>
