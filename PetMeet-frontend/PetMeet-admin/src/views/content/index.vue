<template>
  <div class="content-container">
  <!-- 0. 统计栏 -->
    <div class="stats-bar">
        <el-row :gutter="20">
            <el-col :span="6">
                <el-card shadow="hover" class="stat-card clickable" :body-style="{ padding: '15px' }" @click="handleStatsClick('pending')">
                    <div class="stat-item">
                        <div class="icon-wrapper warning-bg">
                            <el-icon><Timer /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="label">待审核</div>
                            <div class="value warning">{{ stats.pending }}</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                 <el-card shadow="hover" class="stat-card clickable" :body-style="{ padding: '15px' }" @click="handleStatsClick('todayNew')">
                    <div class="stat-item">
                        <div class="icon-wrapper primary-bg">
                            <el-icon><TrendCharts /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="label">今日新增</div>
                            <div class="value">{{ stats.todayNew }}</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
             <el-col :span="6">
                 <el-card shadow="hover" class="stat-card clickable" :body-style="{ padding: '15px' }" @click="handleStatsClick('todayApproved')">
                    <div class="stat-item">
                        <div class="icon-wrapper success-bg">
                            <el-icon><CircleCheck /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="label">今日已通过</div>
                            <div class="value success">{{ stats.todayApproved }}</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                 <el-card shadow="hover" class="stat-card clickable" :body-style="{ padding: '15px' }" @click="handleStatsClick('todayRejected')">
                    <div class="stat-item">
                        <div class="icon-wrapper danger-bg">
                            <el-icon><CircleClose /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="label">今日已拒绝</div>
                            <div class="value danger">{{ stats.todayRejected }}</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
        </el-row>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-container">
      <!-- 上排：状态标签 -->
          <div class="filter-top">
              <span class="filter-label">审核状态：</span>
              <el-radio-group v-model="filterStatus" @change="handleFilter" size="default">
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

          <el-divider style="margin: 15px 0" />

      <!-- 下排：细分筛选 -->
          <div class="filter-bottom">
              <div class="left-filters">
                  <el-form inline>
                    <el-form-item label="内容类型">
                         <el-select v-model="filterType" placeholder="全部" clearable @change="handleFilter" style="width: 120px">
                             <el-option label="图文" value="image_text" />
                             <el-option label="视频" value="video" />
                             <el-option label="图文+视频" value="mixed" />
                         </el-select>
                    </el-form-item>

                    <el-form-item label="社区分类">
                         <el-select v-model="filterCategory" placeholder="全部" clearable @change="handleFilter" style="width: 120px">
                             <el-option label="猫咪日常" value="cat" />
                             <el-option label="狗狗生活" value="dog" />
                             <el-option label="好物测评" value="review" />
                             <el-option label="科普知识" value="knowledge" />
                         </el-select>
                    </el-form-item>

                    <el-form-item label="标签">
                         <el-input
                            v-model="filterTag"
                            placeholder="标签(逗号分隔)"
                            style="width: 150px"
                            clearable
                            @input="handleFilter"
                         />
                    </el-form-item>
                    
                    <el-form-item label="带货情况">
                         <el-select v-model="filterProduct" placeholder="全部" clearable @change="handleFilter" style="width: 110px">
                             <el-option label="关联商品" value="has" />
                             <el-option label="无商品" value="none" />
                             <el-option label="已通过且带货" value="approved_has" />
                         </el-select>
                    </el-form-item>
                    
                    <el-form-item label="时间">
                         <el-date-picker
                            v-model="dateRange"
                            type="daterange"
                            range-separator="-"
                            start-placeholder="开始"
                            end-placeholder="结束"
                            value-format="YYYY-MM-DD"
                            @change="handleFilter"
                            style="width: 210px"
                         />
                         <el-button type="primary" link size="small" @click="setLast7Days" style="margin-left: 8px">最近7天</el-button>
                    </el-form-item>
                    
                    <el-form-item style="margin-left: 10px">
                        <el-input 
                            v-model="searchKeyword" 
                            placeholder="标题 / 作者 / ID" 
                            style="width: 200px" 
                            clearable 
                            prefix-icon="Search"
                            @input="handleFilter"
                        />
                    </el-form-item>
                    <el-form-item>
                        <el-button @click="resetFilters" icon="RefreshLeft">重置</el-button>
                    </el-form-item>
                  </el-form>
              </div>
              
              <div class="right-filters">
                   <el-select v-model="sortOption" placeholder="排序" style="width: 130px" @change="handleFilter">
                      <el-option label="默认 (最新)" value="default" />
                      <el-option label="时间正序" value="time_asc" />
                      <el-option label="热度优先 (浏览)" value="hot_desc" />
                      <el-option label="点赞最多" value="likes_desc" />
                      <el-option label="带货优先" value="product_desc" />
                   </el-select>
              </div>
          </div>
      </div>
      
    <!-- 批量操作 -->
      <transition name="el-fade-in">
        <div v-if="selectedRows.length > 0" class="batch-bar">
             <div class="batch-left">
                 <el-icon class="batch-icon"><Warning /></el-icon>
                 <span class="label">已选择 <span class="count">{{ selectedRows.length }}</span> 项内容</span>
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

    <!-- 数据表格 -->
    <el-card shadow="never">
      <el-table 
        :data="tableData" 
        style="width: 100%" 
        v-loading="loading"
        size="default"
        table-layout="fixed"
        @selection-change="handleSelectionChange"
        :empty-text="searchKeyword || filterStatus !== 'all' || filterType || filterProduct || (dateRange && dateRange.length > 0) ? '未找到相关内容，请调整筛选条件' : '暂无数据'"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="cover" label="封面" width="70">
          <template #default="{ row }">
            <div style="position: relative; width: 60px; height: 60px;">
                <el-image 
                  :src="resolveImageUrl(row.cover)" 
                  :preview-src-list="[resolveImageUrl(row.cover)]" 
                  style="width: 100%; height: 100%; border-radius: 4px" 
                  fit="cover" 
                  :hide-on-click-modal="true"
                  preview-teleported 
                />
                <!-- 类型图标浮层 -->
                <div v-if="row.type === 'video'" style="position: absolute; bottom: 0; right: 0; background: rgba(0,0,0,0.5); color: #fff; padding: 2px; border-top-left-radius: 4px;">
                     <el-icon><VideoCamera /></el-icon>
                </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="内容信息" min-width="220">
          <template #default="{ row }">
             <div class="note-info-cell">
                <div class="note-title" @click="openDetailDialog(row)">
                  <el-tag v-if="row.isSticky && row.status==='approved'" type="danger" size="small" effect="dark" style="margin-right: 5px">置顶</el-tag>
                  <el-tag v-if="row.isRecommended && row.status==='approved'" type="warning" size="small" effect="dark" style="margin-right: 5px">推荐</el-tag>
                  <el-tooltip :content="row.title" placement="top" effect="dark" :show-after="200">
                    <span class="title-text">{{ row.title }}</span>
                  </el-tooltip>
                <!-- 驳回原因提示 -->
                  <el-tooltip v-if="row.status === 'rejected'" class="box-item" effect="dark" :content="'拒绝原因: ' + (row.rejectReason || '未填写')" placement="top">
                      <el-tag type="danger" size="small" effect="plain" style="margin-left: 5px; cursor: help">已拒绝</el-tag>
                  </el-tooltip>
                </div>
                <div class="note-meta">
                   <span>ID: {{ row.id }}</span>
                   <el-divider direction="vertical" />
                   <span>{{ row.type === 'video' ? '视频' : '图文' }}</span>
                   <el-divider direction="vertical" />
                   <span v-if="row.category">分类: {{ row.category }}</span>
                   <span v-if="row.tags && row.tags.length > 0">标签: {{ row.tags.join('、') }}</span>
                   <el-popover
                      v-if="row.productCount > 0"
                      placement="right"
                      :width="300"
                      trigger="click"
                      popper-class="product-preview-popover"
                   >
                       <template #reference>
                           <span class="product-link">
                               <el-icon style="vertical-align: middle"><Goods /></el-icon> 关联 {{ row.productCount }} 商品
                           </span>
                       </template>
                       <div class="product-preview-list">
                           <div v-for="prod in row.products" :key="prod.id" class="product-mini-card" @click.stop="goToProduct(prod.id)">
                               <el-image :src="resolveImageUrl(prod.cover)" style="width: 40px; height: 40px; border-radius: 4px" fit="cover" />
                               <div class="prod-info">
                                   <div class="prod-name">{{ prod.name }}</div>
                                   <div class="prod-price">¥{{ prod.price }}</div>
                               </div>
                           </div>
                       </div>
                   </el-popover>
                </div>
             </div>
          </template>
        </el-table-column>
        <el-table-column label="发布者" width="110">
             <template #default="{ row }">
                 <UserInfoPopover v-if="row.userId" :user-id="row.userId" placement="right" :width="340">
                   <template #reference>
                     <div class="publisher-cell" style="display: flex; align-items: center; cursor: pointer;">
                       <el-avatar :size="24" :src="resolveImageUrl(row.avatar)" style="margin-right: 8px" />
                       <span style="font-size: 13px">{{ row.author }}</span>
                     </div>
                   </template>
                 </UserInfoPopover>
                 <div v-else style="display: flex; align-items: center;">
                   <el-avatar :size="24" :src="resolveImageUrl(row.avatar)" style="margin-right: 8px" />
                   <span style="font-size: 13px">{{ row.author }}</span>
                 </div>
             </template>
        </el-table-column>
        <el-table-column label="互动数据" width="100">
             <template #default="{ row }">
                 <div style="font-size: 12px; color: #909399">
                     <div>浏览: {{ row.views || 0 }}</div>
                     <div>点赞: {{ row.likes || 0 }}</div>
                 </div>
             </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="140" sortable />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column; gap: 4px">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusText(row.status) }}
                </el-tag>
                <el-tag v-if="row.isShielded" type="info" size="small" effect="dark">
                  <el-icon><Hide /></el-icon> 已下架
                </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="60">
          <template #default="{ row }">
            <el-switch 
              v-model="row.isSticky" 
              :disabled="row.status !== 'approved' || row.isShielded"
              size="small"
              @change="handleStickyChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="推荐" width="60">
          <template #default="{ row }">
            <el-switch 
              v-model="row.isRecommended" 
              :disabled="row.status !== 'approved' || row.isShielded"
              size="small"
              active-color="#E6A23C"
              @change="handleRecommendChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="审核记录" width="150">
             <template #default="{ row }">
                 <div v-if="row.auditTimeDisplay" style="font-size: 12px; color: #606266">
                     <div style="display: flex; align-items: center; gap: 6px">
                       <span><el-icon><User /></el-icon> {{ row.auditor || '系统' }}</span>
                       <el-tag v-if="row.auditIsFallback" size="small" type="info" effect="plain">历史</el-tag>
                     </div>
                     <div style="color: #909399">{{ row.auditTimeDisplay.split(' ')[0] }}</div>
                 </div>
                 <div v-else style="color: #C0C4CC; font-size: 12px"> - </div>
             </template>
        </el-table-column>

        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <div class="action-buttons" :class="{ 'action-buttons-pending': row.status === 'pending' }">
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

  <!-- 驳回弹窗（增强版） -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝理由" width="450px">
      <el-form :model="rejectForm" ref="rejectFormRef">
        <el-form-item prop="reason" :rules="[{ required: true, message: '请填写拒绝理由', trigger: 'blur' }]">
          <el-input 
            v-model="rejectForm.reason" 
            type="textarea" 
            :rows="3" 
            placeholder="请填写具体违规点 (如:图片不适/广告营销)..." 
          />
        </el-form-item>

      <!-- 违规分类 -->
        <el-form-item label="违规定性">
            <el-checkbox v-model="rejectForm.isViolation">计入用户违规记录</el-checkbox>
            <el-select v-show="rejectForm.isViolation" v-model="rejectForm.violationType" placeholder="违规类型" size="small" style="width: 150px; margin-left:10px">
                <el-option label="广告垃圾" value="广告垃圾" />
                <el-option label="违规推广" value="违规推广" />
                <el-option label="色情低俗" value="色情低俗" />
                <el-option label="辱骂攻击" value="辱骂攻击" />
                <el-option label="政治敏感" value="政治敏感" />
                <el-option label="其他违规" value="其他违规" />
            </el-select>
        </el-form-item>
      <!-- 快捷理由 -->
        <div class="quick-reasons">
            <div style="font-size: 12px; color: #909399; margin-bottom: 5px">常用语:</div>
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

  <!-- 详情与审核弹窗（增强版） -->
    <el-dialog v-model="detailDialogVisible" title="内容详情" width="750px" top="5vh" class="note-detail-dialog">
      <div class="detail-floating-close" @click="detailDialogVisible = false">
        <el-icon><Close /></el-icon>
      </div>
      <div v-if="currentNote" class="detail-container">
      <!-- 机器审核占位 -->
        <div v-if="currentNote.status === 'pending'" class="machine-check-bar" style="display: flex; gap: 10px; margin-bottom: 10px">
             <el-alert title="机器检测：图片正常 | 文本疑似含微量广告词 (置信度 60%)" type="warning" :closable="false" show-icon style="flex: 1" />
        </div>
      
      <!-- 审核结果提示条 -->
        <el-alert v-if="currentNote.status === 'rejected'" type="error" :closable="false" show-icon style="margin-bottom: 15px">
            <template #title>
                {{ buildAuditTitle(currentNote, 'rejected') }}
            </template>
            <div>原因：{{ currentNote.rejectReason || '未填写' }}</div>
        </el-alert>
        <el-alert v-if="currentNote.status === 'approved'" type="success" :closable="false" show-icon style="margin-bottom: 15px">
             <template #title>
                 {{ buildAuditTitle(currentNote, 'approved') }}
             </template>
        </el-alert>
        <el-alert v-if="currentNote.isShielded" type="info" :closable="false" show-icon style="margin-bottom: 15px; background-color: #f4f4f5; color: #909399">
             <template #title>
                 🚫 该内容已下架，前台暂不可见
             </template>
        </el-alert>
        <el-alert v-if="currentNote.status === 'user_off_shelf'" type="warning" :closable="false" show-icon style="margin-bottom: 15px">
             <template #title>
                 👤 用户已主动下架该内容
             </template>
        </el-alert>
        <el-alert v-if="currentNote.status === 'user_deleted'" type="error" :closable="false" show-icon style="margin-bottom: 15px">
             <template #title>
                 🗑️ 用户已删除该内容
             </template>
        </el-alert>
        <el-alert v-if="currentNote.status === 'admin_soft_deleted'" type="error" :closable="false" show-icon style="margin-bottom: 15px">
             <template #title>
                        🛡️ 管理员已删除该内容
             </template>
        </el-alert>

        <!-- 用户信息 -->
        <div class="user-info">
          <UserInfoPopover v-if="currentNote.userId" :user-id="currentNote.userId" placement="right" :width="340">
            <template #reference>
              <el-avatar :size="40" :src="resolveImageUrl(currentNote.avatar)" style="cursor: pointer" />
            </template>
          </UserInfoPopover>
          <el-avatar v-else :size="40" :src="resolveImageUrl(currentNote.avatar)" />
          <div class="info-text">
            <div class="author-name" style="display: flex; align-items: center; gap: 8px">
                {{ currentNote.author }}
                <el-tag v-if="getUserViolationCount(currentNote.author) > 0" type="danger" size="small" effect="dark" round>
                    历史违规: {{ getUserViolationCount(currentNote.author) }}
                </el-tag>
            </div>
            <div class="publish-time">{{ currentNote.createTime }} · 
                <span v-if="currentNote.type==='video'">视频</span>
                <span v-else-if="currentNote.type==='mixed'">图文+视频</span>
                <span v-else>图文</span>
            </div>
          </div>
        </div>

        <!-- 媒体内容 -->
        <div class="media-area">
          <div v-if="currentNote.type === 'video' && currentNote.videoUrl" class="media-video">
            <video
              class="media-video-player"
              :src="resolveImageUrl(currentNote.videoUrl)"
              :poster="resolveImageUrl(currentNote.cover)"
              controls
              playsinline
            />
          </div>
          <el-carousel v-else trigger="click" height="350px" :autoplay="false">
            <!-- 复用封面图模拟多图展示 -->
            <el-carousel-item v-for="item in 3" :key="item">
              <el-image :src="resolveImageUrl(currentNote.cover)" fit="contain" style="width: 100%; height: 100%; background: #000" />
            </el-carousel-item>
          </el-carousel>
        </div>

        <!-- 内容区 -->
        <div class="note-content">
          <h3 class="detail-title">{{ currentNote.title }}</h3>
          <div v-if="currentNote.category || (currentNote.tags && currentNote.tags.length)" class="detail-meta">
            <el-tag v-if="currentNote.category" size="small" type="info">{{ currentNote.category }}</el-tag>
            <el-tag v-for="tag in (currentNote.tags || [])" :key="tag" size="small" effect="plain">#{{ tag }}</el-tag>
          </div>
          <p class="detail-desc">{{ currentNote.content }}</p>
        </div>

        <!-- 关联商品 -->
        <div v-if="currentNote.productCount > 0" class="products-area">
           <el-alert type="warning" :closable="false" effect="light">
             <template #title>
               <div style="display: flex; align-items: center">
                 <el-icon style="margin-right: 5px"><Goods /></el-icon>
                 关联推荐商品 ({{ currentNote.productCount }})
               </div>
             </template>
             <div class="detail-product-list">
                 <div v-for="prod in currentNote.products" :key="prod.id" class="detail-product-card" @click="goToProduct(prod.id)">
                     <el-image :src="resolveImageUrl(prod.cover)" style="width: 50px; height: 50px; border-radius: 4px; flex-shrink: 0" fit="cover" />
                     <div class="prod-detail">
                         <div class="p-name">{{ prod.name }}</div>
                         <div class="p-price">¥{{ prod.price }}</div>
                     </div>
                     <el-button type="primary" link size="small" @click.stop="goToProduct(prod.id)">详情</el-button>
                 </div>
             </div>
           </el-alert>
        </div>

        <!-- 评论区（仅审核通过的笔记显示） -->
        <div class="comments-area" v-if="currentNote.status === 'approved'">
          <div class="comments-header">
            <div class="comments-title">
              <el-icon><ChatDotRound /></el-icon>
              评论 ({{ commentTotal }})
            </div>
            <div class="comments-actions">
              <el-button size="small" @click="reloadComments">刷新</el-button>
            </div>
          </div>

          <div v-if="commentLoading && comments.length === 0" class="comments-loading">
            <el-skeleton :rows="3" animated />
          </div>

          <div v-else-if="comments.length === 0" class="comments-empty">
            <el-empty description="暂无评论" :image-size="80" />
          </div>

          <div v-else class="comment-list">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <el-avatar v-if="!comment.deleted" :size="28" :src="resolveImageUrl(comment.userAvatar)" class="comment-avatar" />
              <div class="comment-body">
                <div v-if="comment.deleted" class="comment-deleted">评论已删除</div>
                <template v-else>
                <div class="comment-meta">
                  <span class="comment-user">{{ comment.userNickname }}</span>
                  <el-tag v-if="comment.author" size="small" effect="plain">作者</el-tag>
                  <span class="comment-time">{{ formatDateTime(comment.createTime) }}</span>
                </div>
                <div class="comment-text">{{ comment.content }}</div>
                </template>
                <div v-if="comment.replies && comment.replies.length" class="comment-replies">
                  <div v-for="reply in comment.replies" :key="reply.id" class="comment-reply">
                    <span class="comment-user">{{ reply.userNickname }}</span>
                    <el-tag v-if="reply.author" size="small" effect="plain">作者</el-tag>
                    <span v-if="reply.replyToNickname" class="reply-target">回复 @{{ reply.replyToNickname }}：</span>
                    <span>{{ reply.content }}</span>
                  </div>
                  <div v-if="comment.replyCount > comment.replies.length" class="reply-more-tip">
                    还有 {{ comment.replyCount - comment.replies.length }} 条回复
                  </div>
                </div>
              </div>
              <el-button v-if="!comment.deleted" type="danger" link size="small" @click="handleDeleteComment(comment)">删除</el-button>
            </div>
          </div>

          <div class="comment-load-more" v-if="commentHasMore">
            <el-button size="small" :loading="commentLoading" @click="loadMoreComments">{{ commentLoadLabel }}</el-button>
          </div>
        </div>
        <div v-else class="comments-area comments-compact">
          <div class="comments-empty-compact">未审核通过，暂无评论</div>
        </div>

        <!-- 申诉占位区（仅驳回时显示） -->
        <div v-if="currentNote.status === 'rejected'" class="appeal-area" style="margin-top: 20px; border-top: 1px dashed #eee; padding-top: 10px">
            <div style="font-size: 13px; color: #909399; display: flex; justify-content: space-between">
                <span>用户申诉记录 (0)</span>
                <el-button link type="primary" size="small">查看详情</el-button>
            </div>
            <div style="font-size: 12px; color: #C0C4CC; margin-top: 5px">暂无申诉</div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer" style="display: flex; justify-content: space-between; align-items: center">
           <div style="font-size: 12px; color: #909399">
               ID: {{ currentNote?.id }}
           </div>
           <div>
              <template v-if="currentNote?.status === 'pending'">
                 <el-button @click="detailDialogVisible = false">取消</el-button>
                 <el-button type="danger" @click="openRejectDialog(currentNote)">拒绝</el-button>
                 <el-button type="success" @click="handleApprove(currentNote); detailDialogVisible = false">
                     通过 & 发布
                 </el-button>
              </template>
              <template v-else>
                 <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
              </template>
           </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>



<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { Search, ArrowDown, VideoCamera, Picture, Warning, Timer, TrendCharts, CircleCheck, CircleClose, Hide, RefreshLeft, Menu, ChatDotRound, Close } from '@element-plus/icons-vue'
import * as contentApi from '@/api/content'
import { resolveImageUrl } from '@/utils/image'
import UserInfoPopover from '@/components/UserInfoPopover.vue'

const router = useRouter()
const filterStatus = ref('all')
const filterType = ref('')
const filterProduct = ref('')
const filterCategory = ref('')
const filterTag = ref('')
const dateRange = ref([])
const searchKeyword = ref('')
const sortOption = ref('default')

// 统计卡片带出的日期类型：create 对应今日新增，audit 对应今日通过/拒绝
const filterDateType = ref('create')

const loading = ref(false)
const tableData = ref([])
const allData = ref([])
const selectedRows = ref([])

// 统计数据
const stats = reactive({
    pending: 0,
    todayNew: 0,
    todayApproved: 0,
    todayRejected: 0
})

// 快捷驳回原因
const quickReasons = ['内容涉嫌广告', '图片引起不适', '内容质量较低', '与宠物无关', '标题党/虚假宣传']

// 驳回弹窗
const rejectDialogVisible = ref(false)
const rejectFormRef = ref(null)
const rejectForm = reactive({
  id: null,
  reason: '',
  isViolation: false,
  violationType: '广告垃圾'
})

// 辅助函数：获取用户违规次数，这里先做简化处理
const getUserViolationCount = (username) => {
    return 0 // 这里先写死，后面如果要精确统计再补接口
}

const handleQuickReason = (reason) => {
    rejectForm.reason = reason
}

// 详情弹窗
const detailDialogVisible = ref(false)
const currentNote = ref(null)

// 评论列表（管理端视角）
const comments = ref([])
const commentPage = ref(1)
const commentTotal = ref(0)
const commentThreadTotal = ref(0)
const commentMode = ref('preview') // preview -> show a few, full -> paged 20
const commentPreviewSize = 3
const commentFullSize = 20
const commentPageSize = computed(() => (commentMode.value === 'preview' ? commentPreviewSize : commentFullSize))
const commentLoading = ref(false)
const commentHasMore = computed(() => comments.value.length < commentThreadTotal.value)
const commentLoadLabel = computed(() => (commentMode.value === 'preview' ? '展开更多' : '加载更多'))

const resetCommentState = () => {
  comments.value = []
  commentPage.value = 1
  commentTotal.value = 0
  commentThreadTotal.value = 0
  commentLoading.value = false
  commentMode.value = 'preview'
}

const fetchComments = async (reset = false) => {
  if (!currentNote.value) return
  if (currentNote.value.status !== 'approved') {
    resetCommentState()
    return
  }
  if (reset) {
    commentPage.value = 1
    comments.value = []
    commentTotal.value = currentNote.value.commentCount || 0
  }
  commentLoading.value = true
  try {
    const res = await contentApi.getCommentList({
      noteId: currentNote.value.id,
      pageNum: commentPage.value,
      pageSize: commentPageSize.value
    })
    if (res.code === 200 && res.data) {
      const records = res.data.records || []
      commentThreadTotal.value = res.data.total || records.length
      if (reset) {
        comments.value = records
      } else {
        comments.value = comments.value.concat(records)
      }
    } else {
      ElMessage.error(res.message || res.msg || '加载评论失败')
    }
  } catch (e) {
    console.error('加载评论失败', e)
  } finally {
    commentLoading.value = false
  }
}

const loadMoreComments = () => {
  if (commentLoading.value) return
  if (commentMode.value === 'preview') {
    commentMode.value = 'full'
    fetchComments(true)
    return
  }
  if (!commentHasMore.value) return
  commentPage.value += 1
  fetchComments(false)
}

const reloadComments = () => {
  fetchComments(true)
}

const handleDeleteComment = (comment) => {
  ElMessageBox.confirm('确定删除评论吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    const res = await contentApi.deleteComment(comment.id)
    if (res.code !== 200) {
      ElMessage.error(res.message || res.msg || '删除评论失败')
      return
    }
    if (comment.replyCount > 0) {
      comment.deleted = true
      comment.content = ''
      comment.userNickname = ''
      comment.userAvatar = ''
    } else {
      comments.value = comments.value.filter(item => item.id !== comment.id)
      commentThreadTotal.value = Math.max(0, commentThreadTotal.value - 1)
    }
    commentTotal.value = Math.max(0, commentTotal.value - 1)
    if (currentNote.value) {
      currentNote.value.commentCount = Math.max(0, (currentNote.value.commentCount || 0) - 1)
    }
    ElMessage.success('评论已删除')
  })
}

// 点击图片本体时顺手关闭预览器
const closeViewerOnImgClick = (e) => {
    if (e.target.classList.contains('el-image-viewer__img')) {
        const closeBtn = document.querySelector('.el-image-viewer__close')
        if (closeBtn) closeBtn.click()
    }
}

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

onMounted(async () => {
  document.addEventListener('click', closeViewerOnImgClick)
  await loadNoteList()
  await loadStats()
})

const loadNoteList = async () => {
    loading.value = true
    try {
        const statusMap = {
            all: undefined,
            pending: 0,
            approved: 1,
            shielded: 2,
            rejected: 3,
            user_off_shelf: 4,
            user_deleted: 5,
            admin_soft_deleted: 6
        }
        const params = {
            pageNum: currentPage.value,
            pageSize: pageSize.value,
            status: statusMap[filterStatus.value],
            keyword: searchKeyword.value || undefined,
            category: filterCategory.value || undefined,
            tag: filterTag.value || undefined
        }
        const res = await contentApi.getNoteList(params)
        if (res.code === 200 && res.data) {
            allData.value = (res.data.records || []).map(mapNoteFromApi)
            filterData()
            total.value = res.data.total || 0
        } else {
            ElMessage.error(res.message || res.msg || '加载内容列表失败')
        }
    } catch (e) {
        console.error('加载内容列表失败', e)
    } finally {
        loading.value = false
    }
}

const loadStats = async () => {
    try {
        const res = await contentApi.getNoteStats()
        if (res.code === 200 && res.data) {
            stats.pending = res.data.pendingCount || 0
            stats.todayNew = res.data.todayCount || 0
            stats.todayApproved = res.data.todayApprovedCount ?? res.data.publishedCount ?? 0
            stats.todayRejected = res.data.todayRejectedCount ?? res.data.rejectedCount ?? 0
        }
    } catch (e) {
        console.error('加载统计失败', e)
    }
}

onUnmounted(() => {
  document.removeEventListener('click', closeViewerOnImgClick)
})

const formatDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

const mapStatusKey = (status) => {
  if (status === 0) return 'pending'
  if (status === 1) return 'approved'
  if (status === 2) return 'shielded'
  if (status === 3) return 'rejected'
  if (status === 4) return 'user_off_shelf'
  if (status === 5) return 'user_deleted'
  if (status === 6) return 'admin_soft_deleted'
  return 'pending'
}

const normalizeText = (val) => {
  if (!val) return ''
  return String(val)
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .replace(/\r\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

const normalizeType = (val) => {
  const t = String(val || 'image').toLowerCase()
  if (t === 'video') return 'video'
  if (t === 'mixed') return 'mixed'
  return 'image_text'
}

const mapNoteFromApi = (note) => {
  const statusKey = mapStatusKey(note.status)
  const createTime = formatDateTime(note.createTime)
  const auditTime = formatDateTime(note.auditTime)
  const auditTimeDisplay = auditTime || ((statusKey === 'approved' || statusKey === 'rejected') ? createTime : '')
  const auditIsFallback = !auditTime && !!auditTimeDisplay && (statusKey === 'approved' || statusKey === 'rejected')
  const authorUsername = note.username || ''
  const authorNickname = note.nickname || ''

  return {
    id: note.id,
    title: note.title,
    category: note.category || '',
    tags: note.tags ? note.tags.split(',').map(t => t.trim()).filter(Boolean) : [],
    cover: note.cover,
    content: normalizeText(note.content),
    userId: note.userId,
    author: authorUsername || authorNickname || '用户',
    authorUsername,
    authorNickname,
    avatar: note.userAvatar || '',
    type: normalizeType(note.type),
    videoUrl: note.videoUrl,
    status: statusKey,
    isShielded: note.status === 2,
    isSticky: !!note.isSticky,
    isRecommended: !!note.isRecommended,
    likes: note.likeCount || 0,
    views: 0,
    productCount: note.productCount ?? (note.products ? note.products.length : 0),
    products: note.products || [],
    commentCount: note.commentCount || 0,
    createTime,
    auditTime,
    auditTimeDisplay,
    auditIsFallback,
    auditor: note.auditUserName || (statusKey !== 'pending' ? '系统' : ''),
    rejectReason: normalizeText(note.rejectReason)
  }
}

const buildAuditTitle = (note, kind) => {
  if (!note) return ''
  const base = kind === 'approved' ? '审核通过' : '已拒绝'
  const time = note.auditTime || note.auditTimeDisplay
  const timePart = time ? ` (${time})` : ''
  const operator = note.auditor || '系统'
  const opPart = operator ? ` - 操作人: ${operator}` : ''
  const fallback = note.auditIsFallback ? '（历史数据）' : ''
  return `${base}${timePart}${opPart}${fallback}`
}

const filterData = () => {
  let res = allData.value

  // 筛选条件
  
  if (filterStatus.value === 'all') {
    // “全部”视图里默认不展示已删除内容。
    res = res.filter(item => item.status !== 'user_deleted' && item.status !== 'admin_soft_deleted')
  } else {
    res = res.filter(item => item.status === filterStatus.value)
  }
  
  if (filterType.value) {
      res = res.filter(item => item.type === filterType.value)
  }
  
  if (filterProduct.value) {
      if (filterProduct.value === 'has') {
          res = res.filter(item => item.productCount > 0)
      } else if (filterProduct.value === 'none') {
          res = res.filter(item => item.productCount === 0)
      } else if (filterProduct.value === 'approved_has') {
          // “已通过且挂商品”这个筛选要同时满足两个条件
          res = res.filter(item => item.status === 'approved' && item.productCount > 0)
      }
  }
  
  if (dateRange.value && dateRange.value.length === 2) {
      const start = dayjs(dateRange.value[0]).startOf('day').valueOf()
      const end = dayjs(dateRange.value[1]).endOf('day').valueOf()
      
      res = res.filter(item => {
          // 已通过/已拒绝按 auditTime 算，其余情况按 createTime 算
          const timeField = filterDateType.value === 'audit'
            ? (item.auditTime || item.createTime)
            : item.createTime
          if (!timeField) return false
          const time = dayjs(timeField).valueOf()
          return time >= start && time <= end
      })
  }

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    const includesKeyword = (text) => String(text || '').toLowerCase().includes(keyword)
    res = res.filter(item => 
      includesKeyword(item.title) ||
      includesKeyword(item.author) ||
      includesKeyword(item.authorUsername) ||
      includesKeyword(item.authorNickname) ||
      String(item.id).includes(keyword)
    )
  }
  
  // 排序
  if (sortOption.value === 'time_asc') {
     res.sort((a, b) => new Date(a.createTime) - new Date(b.createTime))
  } else if (sortOption.value === 'product_desc') {
     res.sort((a, b) => b.productCount - a.productCount)
  } else if (sortOption.value === 'hot_desc') {
      res.sort((a, b) => (b.views + b.likes) - (a.views + a.likes))
  } else if (sortOption.value === 'likes_desc') {
      res.sort((a, b) => b.likes - a.likes)
  } else {
     // 默认先看置顶（仅已通过内容），再按时间倒序
     res.sort((a, b) => {
         const aSticky = (a.status === 'approved' && !a.isShielded && a.isSticky) ? 1 : 0
         const bSticky = (b.status === 'approved' && !b.isShielded && b.isSticky) ? 1 : 0
         if (aSticky !== bSticky) return bSticky - aSticky
         
         return new Date(b.createTime) - new Date(a.createTime)
     })
  }

  tableData.value = res
}

const handleStatsClick = (type) => {
    const todayStr = dayjs().format('YYYY-MM-DD')
    
    if (type === 'pending') {
        filterStatus.value = 'pending'
        dateRange.value = []
        filterDateType.value = 'create' // Reset
    } else if (type === 'todayNew') {
        filterStatus.value = 'all'
        dateRange.value = [todayStr, todayStr]
        filterDateType.value = 'create' // Today New uses createTime
    } else if (type === 'todayApproved') {
        filterStatus.value = 'approved'
        dateRange.value = [todayStr, todayStr]
        filterDateType.value = 'audit' // Today Approved uses auditTime
    } else if (type === 'todayRejected') {
        filterStatus.value = 'rejected'
        dateRange.value = [todayStr, todayStr]
        filterDateType.value = 'audit' // Today Rejected uses auditTime
    }
    
    handleFilter()
}

const handleFilter = () => {
  currentPage.value = 1
  loadNoteList()
}

// 时间快捷筛选
const setLast7Days = () => {
    const end = new Date()
    const start = new Date()
    start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
    // 格式化成 YYYY-MM-DD
    const formatDate = (d) => d.toISOString().split('T')[0]
    dateRange.value = [formatDate(start), formatDate(end)]
    handleFilter()
    ElMessage.success('已筛选最近7天数据')
}

const goToProduct = (productId) => {
    router.push({
        path: '/admin/product',
        query: { productId: productId }
    })
    detailDialogVisible.value = false
    ElMessage.success(`正在跳转到商品 ID: ${productId}`)
}

const resetFilters = () => {
    filterStatus.value = 'all'
    filterType.value = ''
    filterProduct.value = ''
    filterCategory.value = ''
    filterTag.value = ''
    dateRange.value = []
    searchKeyword.value = ''
    sortOption.value = 'default'
    filterDateType.value = 'create' // 重置回默认的创建时间筛选
    handleFilter()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadNoteList()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadNoteList()
}

// 批量操作
const handleSelectionChange = (val) => {
  selectedRows.value = val
}

const handleBatchCommand = (command) => {
    const ids = selectedRows.value.map(row => row.id)
    if (command === 'pass') {
        const allowPass = selectedRows.value.filter(row => row.status === 'pending' || row.status === 'rejected')
        if (allowPass.length === 0) {
            return ElMessage.warning('仅待审核/已拒绝内容支持批量通过')
        }
        ElMessageBox.confirm(`确定批量通过这 ${allowPass.length} 条内容吗?`, '提示', { type: 'success' })
        .then(async () => {
            const res = await contentApi.batchNoteAction('approve', allowPass.map(r => r.id))
            if (res.code !== 200) {
                ElMessage.error(res.message || res.msg || '批量通过失败')
                return
            }
            ElMessage.success('批量通过成功')
            loadNoteList()
            loadStats()
        })
    } else if (command === 'reject') {
         ElMessageBox.prompt('请输入拒绝原因', '批量拒绝', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputPattern: /\S+/,
            inputErrorMessage: '原因不能为空'
          }).then(({ value }) => {
            Promise.all(ids.map(id => contentApi.rejectNote(id, value)))
              .then(() => {
                ElMessage.warning('批量拒绝完成')
                loadNoteList()
                loadStats()
              })
          })
    } else if (command === 'shield') {
        const allowDown = selectedRows.value.filter(row => row.status === 'approved')
        if (allowDown.length === 0) {
            return ElMessage.warning('仅已通过的内容支持批量下架')
        }
        ElMessageBox.confirm(`确定批量下架这 ${allowDown.length} 条内容吗？下架后前台不可见。`, '批量下架', {
            type: 'warning'
        }).then(() => {
            contentApi.batchNoteAction('shield', allowDown.map(r => r.id)).then((res) => {
                if (res.code !== 200) {
                    ElMessage.error(res.message || res.msg || '批量下架失败')
                    return
                }
                ElMessage.success('批量下架成功')
                loadNoteList()
                loadStats()
            })
        })
    } else if (command === 'softDelete') {
        const allowSoftDelete = selectedRows.value.filter(row => row.status !== 'admin_soft_deleted')
        if (allowSoftDelete.length === 0) {
            return ElMessage.warning('选中的内容已是删除状态')
        }
        ElMessageBox.confirm(`确定批量删除这 ${allowSoftDelete.length} 条内容吗？`, '批量删除', {
            type: 'warning'
        }).then(() => {
            contentApi.batchNoteAction('softDelete', allowSoftDelete.map(r => r.id)).then((res) => {
                if (res.code !== 200) {
                    ElMessage.error(res.message || res.msg || '批量删除失败')
                    return
                }
                ElMessage.success('批量删除成功')
                loadNoteList()
                loadStats()
            })
        })
    }
}


const getStatusType = (status) => {
  const map = {
    pending: 'warning',
    approved: 'success',
    shielded: 'info',
    rejected: 'danger',
    user_off_shelf: 'warning',
    user_deleted: 'danger',
    admin_soft_deleted: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    pending: '待审核',
    approved: '已通过',
    shielded: '管理员下架',
    rejected: '已拒绝',
    user_off_shelf: '用户下架',
    user_deleted: '用户删除',
    admin_soft_deleted: '删除'
  }
  return map[status] || status
}

const handleStickyChange = (row) => {
    if (!row.isSticky) {
        // 关闭置顶
        ElMessageBox.confirm('确定取消该内容的置顶推荐吗？', '取消置顶', {
            type: 'warning'
        }).then(() => {
            contentApi.toggleSticky(row.id).then((res) => {
                if (res.code !== 200) {
                    ElMessage.error(res.message || res.msg || '取消置顶失败')
                    row.isSticky = true
                    return
                }
                row.isSticky = !!res.data
                ElMessage.info('已取消置顶')
            })
        }).catch(() => {
            row.isSticky = true // 用户取消后把开关拨回去
        })
    } else {
        // 开启置顶
        contentApi.toggleSticky(row.id).then((res) => {
            if (res.code !== 200) {
                ElMessage.error(res.message || res.msg || '置顶失败')
                row.isSticky = false
                return
            }
            row.isSticky = !!res.data
            ElMessage.success('已设为置顶')
        })
    }
}

const handleRecommendChange = (row) => {
    if (!row.isRecommended) {
        // 关闭推荐
        contentApi.toggleRecommend(row.id).then((res) => {
            if (res.code !== 200) {
                ElMessage.error(res.message || res.msg || '取消推荐失败')
                row.isRecommended = true
                return
            }
            row.isRecommended = !!res.data
            ElMessage.info('已取消推荐')
        })
    } else {
        // 开启推荐
        contentApi.toggleRecommend(row.id).then((res) => {
            if (res.code !== 200) {
                ElMessage.error(res.message || res.msg || '推荐失败')
                row.isRecommended = false
                return
            }
            row.isRecommended = !!res.data
            ElMessage.success('已设为推荐')
        })
    }
}

const openDetailDialog = (row) => {
  contentApi.getNoteDetail(row.id).then((res) => {
      if (res.code === 200 && res.data) {
          currentNote.value = mapNoteFromApi(res.data)
          resetCommentState()
          commentTotal.value = currentNote.value.commentCount || 0
          if (currentNote.value.status === 'approved') {
            fetchComments(true)
          } else {
            resetCommentState()
          }
          detailDialogVisible.value = true
      } else {
          ElMessage.error(res.message || res.msg || '加载内容详情失败')
      }
  }).catch((e) => {
      console.error('加载内容详情失败', e)
  })
}

watch(detailDialogVisible, (visible) => {
  if (!visible) {
    resetCommentState()
    currentNote.value = null
  }
})

const handleApprove = (row) => {
  ElMessageBox.confirm(`确定通过 "${row.title}" 吗？`, '审核通过', {
    confirmButtonText: '通过',
    cancelButtonText: '取消',
    type: 'success'
  }).then(() => {
    contentApi.approveNote(row.id).then((res) => {
        if (res.code !== 200) {
            ElMessage.error(res.message || res.msg || '审核失败')
            return
        }
        ElMessage.success('审核通过')
        loadNoteList()
        loadStats()
    })
  })
}

const openRejectDialog = (row) => {
  rejectForm.id = row.id
  rejectForm.reason = ''
  rejectForm.isViolation = false
  rejectForm.violationType = '广告垃圾'
  rejectDialogVisible.value = true
}

const confirmReject = () => {
  rejectFormRef.value.validate((valid) => {
    if (valid) {
      contentApi.rejectNote(rejectForm.id, rejectForm.reason).then((res) => {
          if (res.code !== 200) {
              ElMessage.error(res.message || res.msg || '拒绝失败')
              return
          }
          ElMessage.warning('内容已拒绝')
          rejectDialogVisible.value = false
          loadNoteList()
          loadStats()
      })
    }
  })
}

// 单条内容的下架/恢复
const handleToggleShield = (row) => {
    if (row.status !== 'approved' && row.status !== 'shielded') return

    const doToggle = async (reason) => {
        const res = await contentApi.toggleShield(row.id, reason)
        if (res.code !== 200) {
            ElMessage.error(res.message || res.msg || '操作失败')
            return
        }
        row.isShielded = !!res.data
        if (row.isShielded && row.isSticky) {
            row.isSticky = false
        }
        ElMessage.success(row.isShielded ? '内容已下架' : '内容已恢复上架')
        loadNoteList()
        loadStats()
    }

    if (row.isShielded) {
        ElMessageBox.confirm(
            '恢复上架后，前台将重新展示该内容。确定恢复？',
            '恢复上架',
            { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' }
        ).then(() => doToggle()).catch(() => {})
        return
    }

    ElMessageBox.prompt(
        '可选：请输入下架原因（会通知作者）',
        '下架内容',
        {
            confirmButtonText: '确认下架',
            cancelButtonText: '取消',
            inputType: 'textarea',
            inputPlaceholder: '例如：涉嫌侵权/盗用、广告营销、低俗内容等（可不填）'
        }
    ).then(({ value }) => doToggle(value || undefined)).catch(() => {})
}

const handleSoftDelete = (row) => {
    if (!row || row.status === 'admin_soft_deleted') return
    ElMessageBox.prompt(
        '可选：请输入删除原因（会通知作者）',
        '删除内容',
        {
            confirmButtonText: '确认删除',
            cancelButtonText: '取消',
            inputType: 'textarea',
            inputPlaceholder: '例如：严重违规、侵权投诉成立等（可不填）'
        }
    ).then(async ({ value }) => {
        const res = await contentApi.softDeleteNote(row.id, value || undefined)
        if (res.code !== 200) {
            ElMessage.error(res.message || res.msg || '删除失败')
            return
        }
        ElMessage.success('已删除')
        loadNoteList()
        loadStats()
    }).catch(() => {})
}


</script>

<style scoped>
/* 基础布局 */
.content-container {
  font-size: 14px;
}
:deep(.el-table) {
  font-size: 14px;
}

/* 统计条 */
.stats-bar {
    margin-bottom: 20px;
}
.stat-card {
    border: none;
    border-radius: 8px;
    transition: all 0.3s;
}
.stat-card.clickable {
    cursor: pointer;
}
.stat-card.clickable:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.stat-item {
    display: flex;
    align-items: center;
}
.icon-wrapper {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    margin-right: 15px;
}
.warning-bg {
    background-color: #fdf6ec;
    color: #e6a23c;
}
.success-bg {
    background-color: #f0f9eb;
    color: #67c23a;
}
.stat-info .label {
    font-size: 14px;
    color: #909399;
    margin-bottom: 4px;
}
.stat-info .value {
    font-size: 20px;
    font-weight: bold;
    color: #303133;
}
.stat-info .value.warning {
    color: #E6A23C;
}

.filter-card {
  margin-bottom: 20px;
}
.filter-top {
    display: flex;
    align-items: center;
}
.filter-label {
    font-size: 14px;
    color: #606266;
    margin-right: 15px;
    font-weight: 500;
}
.filter-bottom {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
    flex-wrap: nowrap;
}
.left-filters {
    display: flex;
    align-items: center;
}
.left-filters :deep(.el-form) {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    row-gap: 8px;
}
.left-filters :deep(.el-form-item) {
    margin-bottom: 0 !important;
    margin-right: 15px !important;
    display: flex !important;
    align-items: center !important;
    height: 34px;
}
.left-filters :deep(.el-form-item__label) {
    height: 34px !important;
    line-height: 34px !important;
    padding: 0 12px 0 0 !important;
    margin-bottom: 0 !important;
}
.left-filters :deep(.el-form-item__content) {
    display: flex !important;
    align-items: center !important;
    line-height: 34px !important;
    flex-wrap: nowrap !important; /* 关键：防止“最近7天”换行 */
}
.left-filters :deep(.el-input__wrapper),
.left-filters :deep(.el-select__wrapper),
.left-filters :deep(.el-date-editor) {
    height: 34px !important;
    vertical-align: middle !important;
}
.right-filters {
    display: flex;
    align-items: flex-start;
    padding-top: 2px;
}
/* 批量操作条 */
.batch-bar {
    background-color: #fdf6ec;
    border: 1px solid #faecd8;
    color: #e6a23c;
    padding: 8px 15px;
    border-radius: 4px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 15px;
}
.batch-left {
    display: flex;
    align-items: center;
}
.batch-icon {
    margin-right: 8px;
    font-size: 16px;
}
.batch-left .count {
    font-weight: bold;
    margin: 0 4px;
}

.note-info-cell {
    display: flex;
    flex-direction: column;
    padding: 5px 0;
}
.note-title {
  font-weight: 500;
  color: #303133;
  cursor: pointer;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.title-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}
.note-title:hover {
  color: var(--admin-professional-primary);
}
.note-meta {
    font-size: 12px;
    color: #909399;
    display: flex;
    align-items: center;
}
.note-meta span {
    margin-right: 8px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 收紧表格间距，尽量把列都放下 */
:deep(.el-table .cell) {
  padding-left: 6px;
  padding-right: 6px;
}
:deep(.el-table .el-table__header .cell) {
  padding-top: 6px;
  padding-bottom: 6px;
}
:deep(.el-table .el-table__body .cell) {
  padding-top: 6px;
  padding-bottom: 6px;
}
.action-buttons {
  display: grid;
  grid-template-columns: repeat(2, 34px);
  justify-content: center;
  justify-items: center;
  align-items: center;
  column-gap: 10px;
  row-gap: 6px;
}
.action-buttons :deep(.el-button) {
  min-width: 34px;
  margin-left: 0;
  padding-left: 0;
  padding-right: 0;
}
.action-buttons .delete-action {
  grid-column: 1 / -1;
}
.action-buttons-pending {
  display: flex;
  justify-content: center;
  gap: 10px;
}

/* 详情弹窗样式 */
.detail-container {
  font-size: 15px;
  max-height: 75vh;
  overflow-y: auto;
  padding-right: 6px;
}
.user-info {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}
.info-text {
  margin-left: 10px;
}
.author-name {
  font-weight: bold;
  font-size: 16px;
}
.publish-time {
  font-size: 13px;
  color: #909399;
}
.media-area {
  margin-bottom: 15px;
  background-color: #000;
  border-radius: 4px;
  overflow: hidden;
}
.media-video {
  width: 100%;
  height: 350px;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
}
.media-video-player {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
}
.detail-title {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 20px;
}
.detail-desc {
  font-size: 15px;
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
}
.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}
.products-area {
  margin-top: 20px;
}
.product-link {
    color: #E6A23C;
    margin-left: 10px;
    cursor: pointer;
    text-decoration: underline dotted;
    transition: opacity 0.2s;
}
.product-link:hover {
    opacity: 0.8;
}
.product-mini-card {
    display: flex;
    align-items: center;
    padding: 8px 0;
    border-bottom: 1px solid #f0f2f5;
    cursor: pointer;
    transition: background 0.2s;
}
.product-mini-card:hover {
    background-color: #f5f7fa;
}
.product-mini-card:last-child {
    border-bottom: none;
}
.product-mini-card .prod-info {
    margin-left: 12px;
    flex: 1;
}
.product-mini-card .prod-name {
    font-size: 13px;
    font-weight: 500;
}
.product-mini-card .prod-price {
    font-size: 12px;
    color: #f56c6c;
    margin-top: 2px;
}
.detail-product-list {
    margin-top: 8px;
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
}
.detail-product-card {
    display: flex;
    background: #fff;
    padding: 8px;
    border-radius: 4px;
    align-items: center;
    width: calc(50% - 5px);
    box-shadow: 0 2px 4px rgba(0,0,0,0.05);
    cursor: pointer;
    transition: transform 0.2s;
}
.detail-product-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}
.prod-detail {
    margin: 0 10px;
    flex: 1;
    overflow: hidden;
}
.p-name {
    font-weight: bold;
    font-size: 13px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
.p-price {
    color: #f56c6c;
    font-size: 12px;
}
.comments-area {
  margin-top: 20px;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}
.comments-compact {
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px dashed #ebeef5;
}
.comments-empty-compact {
  font-size: 13px;
  color: #909399;
}
.comments-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.comments-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.comment-item {
  display: flex;
  gap: 10px;
  padding: 10px;
  background: #fafafa;
  border-radius: 6px;
  align-items: flex-start;
}
.comment-body {
  flex: 1;
}
.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}
.comment-user {
  color: #303133;
  font-weight: 500;
}
.comment-text {
  font-size: 14px;
  color: #606266;
  white-space: pre-wrap;
}
.comment-deleted {
  color: #a8abb2;
  font-size: 13px;
  line-height: 24px;
}
.comment-replies {
  margin-top: 8px;
  padding-left: 10px;
  border-left: 2px solid #ebeef5;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.comment-reply {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}
.reply-target,
.reply-more-tip {
  color: #909399;
}
.comments-loading,
.comments-empty {
  padding: 10px 0;
}
.comment-load-more {
  display: flex;
  justify-content: center;
  margin-top: 10px;
}

/* 悬浮关闭按钮 */
:deep(.note-detail-dialog .el-dialog) {
  position: relative;
  overflow: visible;
}
.detail-floating-close {
  position: absolute;
  right: -52px;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
}
.detail-floating-close :deep(.el-icon) {
  font-size: 18px;
  color: #606266;
}
.detail-floating-close:hover {
  background: #f5f7fa;
}
</style>
