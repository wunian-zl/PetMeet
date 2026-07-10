<template>
  <section class="content-toolbar">
    <div class="metric-strip">
      <button type="button" class="metric-card pending" @click="handleStatsClick('pending')">
        <span class="metric-icon"><el-icon><Timer /></el-icon></span>
        <span class="metric-copy">
          <span class="metric-label">待审核</span>
          <strong>{{ stats.pending }}</strong>
        </span>
      </button>
      <button type="button" class="metric-card new" @click="handleStatsClick('todayNew')">
        <span class="metric-icon"><el-icon><TrendCharts /></el-icon></span>
        <span class="metric-copy">
          <span class="metric-label">今日新增</span>
          <strong>{{ stats.todayNew }}</strong>
        </span>
      </button>
      <button type="button" class="metric-card approved" @click="handleStatsClick('todayApproved')">
        <span class="metric-icon"><el-icon><CircleCheck /></el-icon></span>
        <span class="metric-copy">
          <span class="metric-label">今日已通过</span>
          <strong>{{ stats.todayApproved }}</strong>
        </span>
      </button>
      <button type="button" class="metric-card rejected" @click="handleStatsClick('todayRejected')">
        <span class="metric-icon"><el-icon><CircleClose /></el-icon></span>
        <span class="metric-copy">
          <span class="metric-label">今日已拒绝</span>
          <strong>{{ stats.todayRejected }}</strong>
        </span>
      </button>
    </div>

    <el-card class="filter-card" shadow="never">
      <div class="status-row">
        <span class="filter-label">审核状态</span>
        <el-radio-group v-model="filterStatus" @change="handleFilter" size="small">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="pending">待审核</el-radio-button>
          <el-radio-button label="approved">已通过</el-radio-button>
          <el-radio-button label="shielded">已下架</el-radio-button>
          <el-radio-button label="user_off_shelf">用户下架</el-radio-button>
          <el-radio-button label="user_deleted">用户删除</el-radio-button>
          <el-radio-button label="admin_soft_deleted">删除</el-radio-button>
          <el-radio-button label="rejected">已拒绝</el-radio-button>
        </el-radio-group>
      </div>

      <div class="filter-grid">
        <div class="filter-field type-field">
          <span>内容类型</span>
          <el-select v-model="filterType" placeholder="全部" clearable @change="handleFilter">
            <el-option label="图文" value="image_text" />
            <el-option label="视频" value="video" />
            <el-option label="图文+视频" value="mixed" />
          </el-select>
        </div>

        <div class="filter-field category-field">
          <span>社区分类</span>
          <el-select v-model="filterCategory" placeholder="全部" clearable @change="handleFilter">
            <el-option label="猫咪日常" value="cat" />
            <el-option label="狗狗生活" value="dog" />
            <el-option label="好物测评" value="review" />
            <el-option label="科普知识" value="knowledge" />
          </el-select>
        </div>

        <div class="filter-field tag-field">
          <span>标签</span>
          <el-input v-model="filterTag" placeholder="逗号分隔" clearable @input="handleFilter" />
        </div>

        <div class="filter-field product-field">
          <span>带货情况</span>
          <el-select v-model="filterProduct" placeholder="全部" clearable @change="handleFilter">
            <el-option label="关联商品" value="has" />
            <el-option label="无商品" value="none" />
            <el-option label="已通过且带货" value="approved_has" />
          </el-select>
        </div>

        <div class="filter-field time-field">
          <span>时间</span>
          <div class="date-filter">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="-"
              start-placeholder="开始"
              end-placeholder="结束"
              value-format="YYYY-MM-DD"
              @change="handleFilter"
            />
            <el-button type="primary" link size="small" @click="setLast7Days">最近7天</el-button>
          </div>
        </div>

        <div class="filter-field search-field">
          <span>搜索</span>
          <el-input
            v-model="searchKeyword"
            placeholder="标题/作者/ID"
            clearable
            prefix-icon="Search"
            @input="handleFilter"
          />
        </div>

        <div class="filter-field sort-field">
          <span>排序</span>
          <el-select v-model="sortOption" placeholder="排序" @change="handleFilter">
            <el-option label="默认(最新)" value="default" />
            <el-option label="时间正序" value="time_asc" />
            <el-option label="热度优先" value="hot_desc" />
            <el-option label="点赞最多" value="likes_desc" />
            <el-option label="带货优先" value="product_desc" />
          </el-select>
        </div>

        <div class="filter-actions">
          <el-button @click="resetFilters" icon="RefreshLeft">重置</el-button>
        </div>
      </div>

      <transition name="el-fade-in">
        <div v-if="selectedRows.length > 0" class="batch-bar">
          <div class="batch-left">
            <el-icon class="batch-icon"><Warning /></el-icon>
            <span>已选择<span class="count">{{ selectedRows.length }}</span>项内容</span>
          </div>
          <div class="batch-right">
            <el-button type="success" plain size="small" @click="handleBatchCommand('pass')">批量通过</el-button>
            <el-button type="danger" plain size="small" @click="handleBatchCommand('reject')">批量拒绝</el-button>
            <el-button type="info" plain size="small" @click="handleBatchCommand('shield')">批量下架</el-button>
            <el-button type="danger" plain size="small" @click="handleBatchCommand('softDelete')">批量删除</el-button>
          </div>
        </div>
      </transition>
    </el-card>
  </section>
</template>

<script setup>
import { inject } from 'vue'

const {
  filterStatus,
  filterType,
  filterProduct,
  filterCategory,
  filterTag,
  dateRange,
  searchKeyword,
  sortOption,
  selectedRows,
  stats,
  handleStatsClick,
  handleFilter,
  setLast7Days,
  resetFilters,
  handleBatchCommand,
  Warning,
  Timer,
  TrendCharts,
  CircleCheck,
  CircleClose
} = inject("adminContentPageContext")
</script>

<style scoped>
.content-toolbar {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  height: 56px;
  padding: 0 14px;
  border: 1px solid #e4e9f2;
  border-radius: 8px;
  background: #fff;
  color: #222936;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.metric-card:hover {
  transform: translateY(-1px);
  border-color: #c7d6f6;
  box-shadow: 0 8px 18px rgba(31, 45, 68, 0.08);
}

.metric-icon {
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.pending .metric-icon {
  background: #fff7e8;
  color: #b7791f;
}

.new .metric-icon {
  background: #eef4ff;
  color: #315cb6;
}

.approved .metric-icon {
  background: #ecfdf5;
  color: #047857;
}

.rejected .metric-icon {
  background: #fff1f2;
  color: #be123c;
}

.metric-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-label {
  color: #667085;
  font-size: 13px;
  line-height: 1;
  white-space: nowrap;
}

.metric-copy strong {
  color: #1f2937;
  font-size: 21px;
  line-height: 1;
  font-weight: 750;
}

.filter-card {
  border-radius: 8px;
}

.filter-card :deep(.el-card__body) {
  padding: 12px 14px;
}

.status-row {
  padding-bottom: 10px;
  border-bottom: 1px solid #edf1f6;
  display: flex;
  align-items: center;
  gap: 14px;
}

.filter-label,
.filter-field > span {
  color: #4b5563;
  font-size: 13px;
  font-weight: 650;
  white-space: nowrap;
}

.status-row :deep(.el-radio-group) {
  flex-wrap: wrap;
  gap: 0;
}

.status-row :deep(.el-radio-button__inner) {
  height: 30px;
  padding: 0 15px;
  line-height: 28px;
  white-space: nowrap;
}

.filter-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 132px 150px minmax(160px, 1fr) 148px minmax(330px, 1.35fr) 230px 140px 86px;
  gap: 10px;
  align-items: end;
}

.filter-field {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.filter-field :deep(.el-input__wrapper),
.filter-field :deep(.el-select__wrapper),
.filter-field :deep(.el-date-editor) {
  width: 100%;
  height: 34px;
  box-sizing: border-box;
}

.date-filter {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.date-filter :deep(.el-date-editor) {
  flex: 1;
  min-width: 0;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
}

.filter-actions :deep(.el-button) {
  width: 86px;
  height: 34px;
}

.batch-bar {
  margin-top: 14px;
  padding: 10px 12px;
  border: 1px solid #faecd8;
  border-radius: 8px;
  background: #fff8ed;
  color: #a16207;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.batch-left,
.batch-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-icon,
.count {
  font-weight: 750;
}

.count {
  margin: 0 4px;
}

@media (max-width: 1500px) {
  .filter-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .time-field {
    grid-column: span 2;
  }
}

@media (max-width: 900px) {
  .metric-strip,
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .time-field {
    grid-column: auto;
  }

  .status-row,
  .batch-bar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
