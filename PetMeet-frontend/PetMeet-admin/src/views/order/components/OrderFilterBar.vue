<template>
  <el-card shadow="never" class="filter-card">
    <div class="header-top">
      <el-radio-group v-model="activeTab" @change="handleTabChange">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="unpaid">待支付</el-radio-button>
        <el-radio-button label="pending">待发货</el-radio-button>
        <el-radio-button label="shipped">已发货</el-radio-button>
        <el-radio-button label="completed">已完成</el-radio-button>
        <el-radio-button label="refunding">退款中</el-radio-button>
      </el-radio-group>
      <div class="header-actions">
        <span v-if="selectedRows.length > 0" class="selected-hint"
          >已选 {{ selectedRows.length }} 项</span
        >
        <el-button v-if="selectedRows.length > 0" type="primary" plain @click="handleBatchExport"
          >导出选中 ({{ selectedRows.length }})</el-button
        >
        <el-button
          type="danger"
          plain
          :disabled="deletableSelectedIds.length === 0"
          @click="handleBatchDeleteOrders"
        >
          批量删除
        </el-button>
        <el-button type="success" :icon="Download" @click="handleExport">导出全部</el-button>
      </div>
    </div>
    <div class="header-bottom">
      <div class="search-group">
        <el-input
          v-model="searchKeyword"
          placeholder="订单号 / 用户名 / 手机号"
          style="width: 220px"
          clearable
          prefix-icon="Search"
          @input="handleFilter"
        />
        <el-select
          v-model="filterPayType"
          placeholder="支付方式"
          clearable
          style="width: 120px"
          @change="handleFilter"
        >
          <el-option label="微信支付" value="微信支付" />
          <el-option label="支付宝" value="支付宝" />
        </el-select>
        <el-select
          v-model="sortOrder"
          placeholder="排序方式"
          style="width: 130px"
          @change="handleFilter"
        >
          <el-option label="时间倒序" value="time_desc" />
          <el-option label="金额倒序" value="amount_desc" />
          <el-option label="时间正序" value="time_asc" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
          @change="handleFilter"
        />
      </div>
      <el-button type="primary" link icon="Refresh" @click="resetFilter">重置筛选</el-button>
    </div>
  </el-card>
</template>

<script setup>
import { inject } from 'vue'

const {
  activeTab,
  handleTabChange,
  selectedRows,
  handleBatchExport,
  deletableSelectedIds,
  handleBatchDeleteOrders,
  Download,
  handleExport,
  searchKeyword,
  filterPayType,
  sortOrder,
  dateRange,
  handleFilter,
  resetFilter
} = inject('adminOrderPageContext')
</script>
