<template>
  <el-card class="filter-card" shadow="never">
    <div class="filter-wrapper">
      <!-- 左侧：筛选项 -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="关键词">
          <el-input
            v-model="searchKeyword"
            placeholder="商品名称 / ID"
            prefix-icon="Search"
            clearable
            @input="handleFilter"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="商品分类">
          <el-select
            v-model="filterCategory"
            placeholder="全部分类"
            style="width: 140px"
            clearable
            @change="handleFilter"
          >
            <el-option
              v-for="item in categoryOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="filterStatus"
            placeholder="全部"
            style="width: 100px"
            clearable
            @change="handleFilter"
          >
            <el-option label="已上架" :value="1" />
            <el-option label="已下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存状态">
          <el-select
            v-model="filterStockStatus"
            placeholder="全部"
            style="width: 110px"
            clearable
            @change="handleFilter"
          >
            <el-option label="库存紧张" value="warning" />
            <el-option label="无库存" value="empty" />
            <el-option label="充足" value="normal" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="filterTag"
            placeholder="全部"
            style="width: 100px"
            clearable
            @change="handleFilter"
          >
            <el-option label="热卖" value="hot" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-select
            v-model="sortOption"
            placeholder="默认排序"
            style="width: 140px"
            @change="handleFilter"
          >
            <el-option label="默认 (权重+ID)" value="default" />
            <el-option label="权重优先" value="weight_desc" />
            <el-option label="销量高到低" value="sales_desc" />
            <el-option label="价格低到高" value="price_asc" />
            <el-option label="价格高到低" value="price_desc" />
            <el-option label="库存少到多" value="stock_asc" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" link @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 右侧：操作区 -->
      <div class="action-group">
        <el-dropdown
          v-if="selectedRows.length > 0"
          @command="handleBatchCommand"
          style="margin-right: 12px"
        >
          <el-button type="primary" plain>
            批量操作 ({{ selectedRows.length }})
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="on">批量上架</el-dropdown-item>
              <el-dropdown-item command="off">批量下架</el-dropdown-item>
              <el-dropdown-item command="set_hot">批量设为热卖</el-dropdown-item>
              <el-dropdown-item command="delete" divided style="color: #f56c6c"
                >批量删除</el-dropdown-item
              >
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button type="primary" icon="Plus" @click="openDialog('create')">新增商品</el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { inject } from 'vue'

const {
  searchKeyword,
  filterCategory,
  filterStatus,
  filterStockStatus,
  filterTag,
  sortOption,
  categoryOptions,
  handleFilter,
  resetFilter,
  selectedRows,
  handleBatchCommand,
  ArrowDown,
  openDialog
} = inject('adminProductPageContext')
</script>
