<template>
  <div class="user-container">
  <!-- 筛选区 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <div class="left-panel">
            <el-form :inline="true" class="user-filter-form">
                <el-form-item>
                    <el-input 
                        v-model="searchKeyword" 
                        placeholder="搜索昵称或手机号" 
                        style="width: 200px" 
                        clearable 
                        prefix-icon="Search"
                        @input="handleFilter"
                    />
                </el-form-item>
                <el-form-item label="角色">
                    <el-select v-model="filterRole" placeholder="全部" style="width: 110px" clearable @change="handleFilter">
                        <el-option label="普通用户" value="user" />
                        <el-option label="管理员" value="admin" />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="filterStatus" placeholder="全部" style="width: 100px" clearable @change="handleFilter">
                        <el-option label="正常" :value="1" />
                        <el-option label="封禁" :value="0" />
                    </el-select>
                </el-form-item>
                <el-form-item label="排序">
                    <el-select v-model="sortOption" placeholder="请选择" style="width: 150px" @change="handleFilter">
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
                        style="width: 240px"
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

  <!-- 表格区 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-info-cell">
              <el-avatar 
                :src="resolveImageUrl(row.avatar)" 
                :size="40" 
                style="cursor: pointer" 
                @click="openDetailDialog(row)" 
              />
              <div class="info-text">
                <div class="nickname" style="cursor: pointer; color: var(--admin-professional-primary)" @click="openDetailDialog(row)">
                    {{ row.username }}
                </div>
                <div class="email">{{ row.email }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="phone" label="手机号" width="115" />
 
        <el-table-column label="角色" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.role === 'admin'" type="danger" effect="dark">管理员</el-tag>
            <el-tag v-else type="info" effect="plain">普通用户</el-tag>
          </template>
        </el-table-column>
 
        <el-table-column label="用户标签" min-width="180" show-overflow-tooltip>
             <template #default="{ row }">
                 <div style="display: flex; flex-wrap: wrap; gap: 4px;">
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
                        v-for="tag in (row.tags ? row.tags.split(',') : [])" 
                        :key="tag" 
                        size="small" 
                        effect="light"
                     >
                        {{ tag }}
                     </el-tag>
                 </div>
             </template>
         </el-table-column>
 
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
             <el-tooltip v-if="!row.status" :content="'封禁原因: ' + (row.banReason || '未填写')" placement="top">
                <el-tag type="danger" size="small" effect="plain" style="cursor: help">已封禁</el-tag>
             </el-tooltip>
             <el-tag v-else type="success" size="small" effect="plain">正常</el-tag>
          </template>
        </el-table-column>
 
        <el-table-column label="最后登录" width="160" sortable prop="lastLoginTime">
            <template #default="{ row }">
                <div v-if="row.lastLoginTime">
                    <div>{{ row.lastLoginTime }}</div>
                    <div style="font-size: 12px; color: #909399">{{ formatTimeAgo(row.lastLoginTime) }}</div>
                </div>
                <span v-else>-</span>
            </template>
        </el-table-column>
 
        <el-table-column prop="createTime" label="注册时间" width="165" sortable />
 
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button 
                :type="row.status ? 'danger' : 'success'" 
                link 
                size="small"
                @click="handleStatusAction(row)"
            >
                {{ row.status ? '封禁' : '解封' }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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



  <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="用户头像" style="margin-bottom: 20px;">
           <div style="display: flex; align-items: center; gap: 12px">
               <el-avatar :src="resolveImageUrl(form.avatar)" :size="60" />

               <template v-if="form.id && isEditingSelf">
                 <el-upload
                   :action="avatarUploadAction"
                   :headers="avatarUploadHeaders"
                   :show-file-list="false"
                   :before-upload="beforeAvatarUpload"
                   :on-success="handleAvatarUploadSuccess"
                   :on-error="handleAvatarUploadError"
                 >
                   <el-button size="small">更换头像</el-button>
                 </el-upload>
                 <el-button size="small" type="danger" plain @click="form.avatar = ''">移除头像</el-button>
               </template>

               <template v-else>
                 <el-button size="small" :disabled="!form.id" @click="handleHarmonizeAvatar">和谐头像</el-button>
               </template>
           </div>
           <div v-if="form.id && !isEditingSelf" style="margin-top: 6px; font-size: 12px; color: #909399; line-height: 1.2">
             管理端仅支持“和谐头像”（清空头像），不能直接替用户更换头像。
           </div>
        </el-form-item>
        <el-form-item label="用户昵称" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="登录密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="8-64位，同时包含字母和数字"
          />
        </el-form-item>
        <el-form-item label="手机号码" prop="phone">
          <el-input v-model="form.phone" :disabled="!!form.id && !isEditingSelf" />
        </el-form-item>
        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="form.email" :disabled="!!form.id && !isEditingSelf" />
        </el-form-item>
        <el-form-item label="用户角色" prop="role">
           <el-radio-group v-model="form.role">
              <el-radio value="user">普通用户</el-radio>
              <el-radio value="admin">管理员</el-radio>
           </el-radio-group>
        </el-form-item>
        <el-form-item label="账号状态">
           <el-radio-group v-model="form.status">
              <el-radio :label="true">正常</el-radio>
              <el-radio :label="false">封禁</el-radio>
           </el-radio-group>
        </el-form-item>
        <el-form-item label="用户标签" prop="tags">
            <el-input 
                v-model="form.tags" 
                placeholder="多个标签请用逗号分隔 (e.g. 活跃,爱猫)"
            />
            <div style="font-size: 12px; color: #909399; line-height: 1.2; margin-top: 4px">
                提示: 可参考详情页的数据来打标
            </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确认</el-button>
        </span>
      </template>
    </el-dialog>
  <!-- 封禁原因弹窗 -->
    <el-dialog v-model="banDialogVisible" title="封禁用户" width="400px">
        <el-form>
            <el-form-item label="封禁原因">
                <el-input 
                    v-model="banForm.reason" 
                    type="textarea" 
                    :rows="3" 
                    placeholder="请输入封禁原因（如：发布违规广告）"
                />
            </el-form-item>
        </el-form>
        <template #footer>
            <span class="dialog-footer">
                <el-button @click="banDialogVisible = false">取消</el-button>
                <el-button type="danger" @click="submitBan">确认封禁</el-button>
            </span>
        </template>
    </el-dialog>

  <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="用户详情" width="600px">
        <div v-if="currentUser" class="user-detail-content">
      <!-- 顶部信息 -->
            <div class="detail-header">
                <el-avatar :src="resolveImageUrl(currentUser.avatar)" :size="80" />
                <div class="detail-basic">
                    <div class="d-name">{{ currentUser.username }} 
                        <el-tag size="small" :type="currentUser.role==='admin'?'danger':'info'">{{ currentUser.role === 'admin' ? 'admin' : 'user' }}</el-tag>
                    </div>
                    <div class="d-info">ID: {{ currentUser.id }} | Phone: {{ currentUser.phone }} | Email: {{ currentUser.email || '-' }}</div>
                    <div class="d-tags" v-if="currentUser.tags">
                        <el-tag v-for="t in currentUser.tags.split(',')" :key="t" size="small" type="success" style="margin-right: 5px">{{ t }}</el-tag>
                    </div>
                </div>
            </div>
            
      <!-- 统计卡片 -->
            <div class="detail-stats">
                <div class="stat-item">
                    <div class="s-val">{{ currentUser.stats?.postCount || 0 }}</div>
                    <div class="s-label">发布内容</div>
                </div>
                 <div class="stat-item">
                    <div class="s-val">{{ currentUser.stats?.orderCount || 0 }}</div>
                    <div class="s-label">订单数</div>
                </div>
                 <div class="stat-item">
                    <div class="s-val">￥{{ currentUser.stats?.totalSpent || 0 }}</div>
                    <div class="s-label">总消费</div>
                </div>
      <!-- 新增统计项 -->
                <div class="stat-item">
                    <div class="s-val">{{ currentUser.stats?.totalLikeCount || 0 }}</div>
                    <div class="s-label">{{ labelText.totalLikes }}</div>
                </div>
                <div class="stat-item">
                    <div class="s-val">{{ currentUser.stats?.collectNoteCount || 0 }}</div>
                    <div class="s-label">{{ labelText.collections }}</div>
                </div>
                <div class="stat-item">
                    <div class="s-val">{{ currentUser.stats?.commentCount || 0 }}</div>
                    <div class="s-label">评论数</div>
                </div>
                <div class="stat-item">
                    <div class="s-val">{{ currentUser.stats?.commentLikeCount || 0 }}</div>
                    <div class="s-label">评论获赞</div>
                </div>
                 <div class="stat-item">
                    <div class="s-val" style="color: #F56C6C">{{ currentUser.stats?.violationCount || 0 }}</div>
                    <div class="s-label">违规次数</div>
                </div>
            </div>

      <!-- 活动占位 -->
            <div class="detail-activity">
                <div class="section-title">最近动态(Mock)</div>
                <ul class="activity-list">
                    <li><span class="time">2023-06-12 10:00</span> <span class="action">登录了系统</span></li>
                    <li><span class="time">2023-06-11 15:30</span> <span class="action">发布了笔记《我家猫咪真可爱》</span></li>
                    <li><span class="time">2023-06-10 09:20</span> <span class="action">购买了商品[皇家猫粮 2kg]</span></li>
                </ul>
            </div>

      <!-- 安全操作 -->
             <div class="detail-actions" style="margin-top: 20px; border-top: 1px solid #eee; padding-top: 15px;">
                <div class="section-title" style="margin-bottom: 10px;">安全操作</div>
                <el-button type="warning" size="small" plain @click="handleResetPwd">重置密码</el-button>
                <el-button type="danger" size="small" plain @click="handleForceLogout">强制下线</el-button>
             </div>
        </div>
    </el-dialog>

  <!-- 标签规则配置弹窗 -->
    <el-dialog v-model="tagConfigDialogVisible" title="自动化标签规则配置" width="450px">
        <el-form :model="tagConfig" label-width="120px">
            <el-form-item label="萌新判定 (天)">
                <el-input-number v-model="tagConfig.newDay" :min="1" :max="365" />
                <div class="form-tip">注册于该天数内的用户展示“萌新”标签</div>
            </el-form-item>
            <el-form-item label="活跃判定 (天)">
                <el-input-number v-model="tagConfig.activeDay" :min="1" :max="30" />
                <div class="form-tip">在该天数内有登录记录的用户展示“活跃”标签</div>
            </el-form-item>
            <el-form-item label="内容达人 (条)">
                <el-input-number v-model="tagConfig.influencerPost" :min="1" :max="9999" />
                <div class="form-tip">发布内容超过此数量的用户展示“内容达人”标签</div>
            </el-form-item>
            <el-form-item label="消费达人 (元)">
                <el-input-number v-model="tagConfig.bigSpenderAmount" :min="1" :max="100000" :step="100" />
                <div class="form-tip">累计消费超过此金额的用户展示“消费达人”标签</div>
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button type="primary" @click="tagConfigDialogVisible = false">完成配置</el-button>
        </template>
    </el-dialog>

  <!-- 违规详情弹窗 -->
    <el-dialog v-model="violationDialogVisible" title="违规记录明细" width="700px">
        <div v-if="currentViolationUser" style="margin-bottom: 20px;">
            <div style="display: flex; align-items: center; margin-bottom: 15px; background: #fef0f0; padding: 10px; border-radius: 4px; border-left: 4px solid #f56c6c;">
                <el-icon color="#f56c6c" :size="20" style="margin-right: 10px"><Warning /></el-icon>
                <div>
                   <span style="font-weight: bold">{{ currentViolationUser.username }}</span>
                   <span style="margin-left: 10px; font-size: 13px; color: #606266">累计违规: {{ currentViolationUser.stats?.violationCount || 0 }} 次</span>
                </div>
            </div>
            
            <el-table :data="currentViolationUser.violationRecords" stripe style="width: 100%" size="small" border>
                <el-table-column prop="time" label="违规时间" width="160" />
                <el-table-column prop="type" label="违规类型" width="100">
                    <template #default="{ row }">
                        <el-tag type="danger" size="small">{{ row.type }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="content" label="违规内容/证据" min-width="200" show-overflow-tooltip />
                <el-table-column prop="auditor" label="审核人" width="100" />
                <el-table-column prop="status" label="处理状态" width="90">
                    <template #default="{ row }">
                        <span style="color: #67c23a"><el-icon><CircleCheck /></el-icon> {{ row.status }}</span>
                    </template>
                </el-table-column>
            </el-table>
        </div>
        <template #footer>
            <el-button @click="violationDialogVisible = false">关闭</el-button>
            <el-button type="danger" plain @click="handleStatusAction(currentViolationUser)" v-if="currentViolationUser?.status">立即封禁</el-button>
        </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { Search, Plus, Warning, Setting, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import * as userApi from '@/api/user'
import { resolveImageUrl } from '@/utils/image'
import { useAdminStore } from '@/store/admin'

import { useRoute } from 'vue-router'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const labelText = {
    totalLikes: '\u4f5c\u54c1\u83b7\u8d5e\u603b\u6570',
    collections: '\u6536\u85cf'
}


const route = useRoute()
const adminStore = useAdminStore()

// 状态
const searchKeyword = ref('')
const filterRole = ref('')
const filterStatus = ref('')
const sortOption = ref('createTime_desc')
const dateRange = ref([]) // 日期范围
const loading = ref(false)

// 自动标签配置
const tagConfigDialogVisible = ref(false)
const tagConfig = reactive({
    newDay: 7,
    activeDay: 2,
    influencerPost: 50,
    bigSpenderAmount: 1000
})

const allData = ref([])
const tableData = ref([])

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

onMounted(async () => {
    // 处理从看板带过来的查询参数
    if (route.query.startDate && route.query.endDate) {
        dateRange.value = [route.query.startDate, route.query.endDate]
    }
    
    await loadUserList()
})

const loadUserList = async () => {
    loading.value = true
    try {
        const params = {
            pageNum: currentPage.value,
            pageSize: pageSize.value,
            keyword: searchKeyword.value || undefined,
            role: filterRole.value || undefined,
            status: filterStatus.value === '' ? undefined : filterStatus.value
        }
        const res = await userApi.getUserList(params)
        if (res.code === 200 && res.data) {
            allData.value = (res.data.records || []).map(mapUserFromApi)
            filterData()
            total.value = res.data.total || 0
        } else {
            ElMessage.error(res.message || res.msg || '加载用户列表失败')
        }
    } catch (e) {
        console.error('加载用户列表失败', e)
    } finally {
        loading.value = false
    }
}

const formatDateTime = (value) => {
    if (!value) return ''
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return String(value)
    return date.toLocaleString('zh-CN', { hour12: false })
}

const mapUserFromApi = (user) => ({
    id: user.id,
    username: user.nickname || user.username,
    email: user.email || '',
    phone: user.phone,
    avatar: user.avatar || '',
    status: user.status === 1,
    role: (user.role === 'influencer' ? 'user' : (user.role || 'user')),
    tags: user.tags || '',
    banReason: user.banReason,
    createTime: formatDateTime(user.createTime),
    lastLoginTime: formatDateTime(user.lastLoginTime),
    stats: {
        postCount: user.noteCount || 0,
        orderCount: user.orderCount || 0,
        totalSpent: Number(user.totalSpent || 0),
        totalLikeCount: user.totalLikeCount || 0,
        collectNoteCount: user.collectNoteCount || 0,
        commentCount: 0,
        commentLikeCount: 0,
        violationCount: 0
    }
})

const filterData = () => {
    let res = allData.value
    
    // 日期范围筛选
    if (dateRange.value && dateRange.value.length === 2) {
        const start = dayjs(dateRange.value[0]).startOf('day')
        const end = dayjs(dateRange.value[1]).endOf('day')
        res = res.filter(item => {
            const t = dayjs(item.createTime)
            return t.isValid() && (t.isAfter(start) || t.isSame(start)) && (t.isBefore(end) || t.isSame(end))
        })
    }

    // 排序逻辑
    if (sortOption.value === 'createTime_desc') {
        res.sort((a,b) => new Date(b.createTime) - new Date(a.createTime))
    } else if (sortOption.value === 'createTime_asc') {
        res.sort((a,b) => new Date(a.createTime) - new Date(b.createTime))
    } else if (sortOption.value === 'totalSpent_desc') {
        res.sort((a,b) => (b.stats?.totalSpent || 0) - (a.stats?.totalSpent || 0))
    } else if (sortOption.value === 'postCount_desc') {
        res.sort((a,b) => (b.stats?.postCount || 0) - (a.stats?.postCount || 0))
    } else if (sortOption.value === 'violationCount_desc') {
        res.sort((a,b) => (b.stats?.violationCount || 0) - (a.stats?.violationCount || 0))
    } else if (sortOption.value === 'totalLikeCount_desc') {
        res.sort((a,b) => (b.stats?.totalLikeCount || 0) - (a.stats?.totalLikeCount || 0))
    } else if (sortOption.value === 'collectNoteCount_desc') {
        res.sort((a,b) => (b.stats?.collectNoteCount || 0) - (a.stats?.collectNoteCount || 0))
    } else if (sortOption.value === 'status_asc') {
        // 被封禁的用户排在前面
        res.sort((a,b) => (a.status === b.status ? 0 : (a.status ? 1 : -1)))
    }

    tableData.value = res
}

const getAutoTags = (row) => {
    const tags = []
    const now = dayjs()
    const joinDate = dayjs(row.createTime)
    
    // 规则 0：管理员只保留必要标签，界面更干净
    if (row.role === 'admin') {
         if (now.diff(dayjs(row.lastLoginTime), 'day') < tagConfig.activeDay) {
             tags.push({ label: '活跃', type: 'primary', desc: `最近${tagConfig.activeDay} 天内有登录记录` })
         }
         return tags
    }

    // 规则 1：萌新用户
    if (now.diff(joinDate, 'day') < tagConfig.newDay) {
        tags.push({ label: '萌新', type: 'success', desc: `注册时间在${tagConfig.newDay} 天内` })
    }
    
    // 规则 2：活跃用户
    if (row.lastLoginTime && now.diff(dayjs(row.lastLoginTime), 'day') < tagConfig.activeDay) {
        tags.push({ label: '活跃', type: 'primary', desc: `最近${tagConfig.activeDay} 天内有登录记录` })
    }

    // 规则 3：内容达人
    if (row.role === 'influencer' || (row.stats && row.stats.postCount > tagConfig.influencerPost)) {
        tags.push({ label: '内容达人', type: 'warning', desc: `角色为达人或发布内容超过 ${tagConfig.influencerPost} 条` })
    }

    // 规则 4：消费达人
    if (row.stats && row.stats.totalSpent > tagConfig.bigSpenderAmount) {
        tags.push({ label: '消费达人', type: 'danger', desc: `累计消费超过 ￥${tagConfig.bigSpenderAmount}` })
    }
    
    // 规则 5：有违规记录
    if (row.stats && row.stats.violationCount > 0) {
         tags.push({ 
             label: '有违规',
             type: 'info', 
             desc: `点击查看明细 (当前: ${row.stats.violationCount} 次)`,
             clickable: true,
             onClick: (r) => openViolationDialog(r)
         })
    }

    return tags
}

const handleFilter = () => {
    currentPage.value = 1
    loadUserList()
}

const handlePageChange = (page) => {
    currentPage.value = page
    loadUserList()
}

const formatTimeAgo = (timeStr) => {
    if (!timeStr) return ''
    return dayjs(timeStr).fromNow()
}

// 操作区

// 统一的状态切换逻辑
const banDialogVisible = ref(false)
const banForm = reactive({
    userId: null,
    reason: ''
})

const handleStatusAction = async (row) => {
    if (row.status) {
        // 进入封禁流程
        banForm.userId = row.id
        banForm.reason = ''
        banDialogVisible.value = true
    } else {
        // 进入解封流程
        try {
            await ElMessageBox.confirm(`确定要解封用户 "${row.username}" 吗？`, '提示', {
                type: 'success',
                confirmButtonText: '确定解封',
                cancelButtonText: '取消'
            })
            await userApi.unbanUser(row.id)
            ElMessage.success('用户已解封')
            await loadUserList()
        } catch (e) {
            if (e !== 'cancel') console.error(e)
        }
    }
}

const submitBan = async () => {
    if (!banForm.reason) {
        ElMessage.warning('请填写封禁原因')
        return
    }
    try {
        await userApi.banUser(banForm.userId, banForm.reason)
        ElMessage.success('用户已封禁')
        banDialogVisible.value = false
        await loadUserList()
    } catch (e) {
        console.error('封禁失败', e)
    }
}

// 用户详情逻辑
const detailDialogVisible = ref(false)
const currentUser = ref(null)

const openDetailDialog = (row) => {
    userApi.getUserDetail(row.id).then((res) => {
        if (res.code === 200 && res.data) {
            currentUser.value = mapUserFromApi(res.data)
            detailDialogVisible.value = true
        } else {
            ElMessage.error(res.message || res.msg || '加载用户详情失败')
        }
    }).catch((e) => {
        console.error('加载用户详情失败', e)
    })
}

// 违规记录弹窗
const violationDialogVisible = ref(false)
const currentViolationUser = ref(null)

const openViolationDialog = (row) => {
    currentViolationUser.value = row
    violationDialogVisible.value = true
}

const handleResetPwd = () => {
    if (!currentUser.value) return
    userApi.resetPassword(currentUser.value.id).then((res) => {
        if (res.code !== 200) {
            ElMessage.error(res.message || res.msg || '重置密码失败')
            return
        }
        ElMessage.success(`密码已重置，新密码：${res.data}`)
    }).catch((e) => {
        console.error('重置密码失败', e)
    })
}
const handleForceLogout = () => {
    if (!currentUser.value) return
    ElMessageBox.confirm(`确定强制下线用户 "${currentUser.value.username}" 吗？`, '强制下线', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        userApi.forceLogout(currentUser.value.id).then((res) => {
            if (res.code !== 200) {
                ElMessage.error(res.message || res.msg || '强制下线失败')
                return
            }
            ElMessage.success('已强制下线')
        }).catch((e) => {
            console.error('强制下线失败', e)
        })
    }).catch(() => {})
}

// 和谐头像
const handleHarmonizeAvatar = async () => {
    if (!form.id) {
        ElMessage.warning('仅适用于已存在用户')
        return
    }
    try {
        const res = await userApi.harmonizeAvatar(form.id)
        if (res.code !== 200) {
            ElMessage.error(res.message || res.msg || '和谐头像失败')
            return
        }
        form.avatar = ''
        ElMessage.success('头像已和谐')
        loadUserList()
    } catch (e) {
        console.error('头像和谐失败', e)
    }
}

// toggleBan 已废弃，逻辑已经收口到 handleStatusAction

const handleDelete = (row) => {
    ElMessageBox.confirm(
        `确定要删除用户 "${row.username}" 吗？此操作不可恢复！`,
        '警告',
        {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'warning',
        }
    ).then(() => {
        userApi.deleteUser(row.id).then((res) => {
            if (res.code !== 200) {
                ElMessage.error(res.message || res.msg || '删除失败')
                return
            }
            ElMessage.success('删除成功')
            loadUserList()
        }).catch((e) => {
            console.error('删除用户失败', e)
        })
    }).catch(() => {})
}

// 新增 / 编辑
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({
    id: null,
    username: '',
    password: '',
    phone: '',
    email: '',
    avatar: '',
    role: 'user',
    tags: '',
    status: true
})

const isEditingSelf = computed(() => {
    if (!form.id) return false
    const me = adminStore.userInfo?.userId
    if (!me) return false
    return Number(me) === Number(form.id)
})

const avatarUploadAction = '/api/common/upload/image?biz=userAvatar'
const avatarUploadHeaders = computed(() => ({
    Authorization: adminStore.token || localStorage.getItem('adminToken') || ''
}))

const beforeAvatarUpload = (file) => {
    const isImage = file.type?.startsWith('image/')
    if (!isImage) {
        ElMessage.error('只能上传图片文件')
        return false
    }
    const isLt2M = file.size / 1024 / 1024 < 2
    if (!isLt2M) {
        ElMessage.error('头像图片大小不能超过 2MB')
        return false
    }
    return true
}

const handleAvatarUploadSuccess = (response) => {
    if (response?.code === 200) {
        form.avatar = response.data
        ElMessage.success('头像已更新')
        return
    }
    ElMessage.error(response?.message || response?.msg || '头像上传失败')
}

const handleAvatarUploadError = () => {
    ElMessage.error('头像上传失败，请稍后重试')
}

const validateInitialPassword = (_rule, value, callback) => {
    if (form.id) {
        callback()
        return
    }
    if (!value) {
        callback(new Error('请输入登录密码'))
        return
    }
    if (value.length < 8 || value.length > 64 || !/[A-Za-z]/.test(value) || !/\d/.test(value)) {
        callback(new Error('密码必须为8-64位，且同时包含字母和数字'))
        return
    }
    callback()
}

const rules = {
    username: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
    password: [{ validator: validateInitialPassword, trigger: 'blur' }]
}

const handleCreate = () => {
    form.id = null
    form.username = ''
    form.password = ''
    form.phone = ''
    form.email = ''
    form.avatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
    form.role = 'user'
    form.tags = ''
    form.status = true
    dialogVisible.value = true
}

const openEditDialog = (row) => {
    userApi.getUserDetail(row.id).then((res) => {
        if (res.code === 200 && res.data) {
            const mapped = mapUserFromApi(res.data)
            Object.assign(form, {
                id: mapped.id,
                username: mapped.username,
                phone: mapped.phone,
                email: mapped.email,
                avatar: mapped.avatar,
                role: mapped.role,
                tags: mapped.tags,
                status: mapped.status
            })
            dialogVisible.value = true
        } else {
            ElMessage.error(res.message || res.msg || '加载用户详情失败')
        }
    }).catch((e) => {
        console.error('加载用户详情失败', e)
    })
}

const handleSubmit = () => {
    formRef.value.validate((valid) => {
        if (valid) {
            const payload = {
                nickname: form.username,
                role: form.role,
                tags: form.tags,
                status: form.status ? 1 : 0
            }
            if (!form.id) {
                // 新增用户：账号(username)与昵称(nickname)默认同值，避免后端 username 为空
                payload.username = form.username
                payload.password = form.password
                payload.phone = form.phone
                payload.email = form.email
                payload.avatar = form.avatar
            } else if (isEditingSelf.value) {
                // 编辑自己的管理员账号：允许改手机号/邮箱/头像
                payload.phone = form.phone
                payload.email = form.email
                payload.avatar = form.avatar
            }
            if (form.id) {
                userApi.updateUser(form.id, payload).then((res) => {
                    if (res.code !== 200) {
                        ElMessage.error(res.message || res.msg || '更新失败')
                        return
                    }
                    ElMessage.success('更新成功')
                    dialogVisible.value = false
                    loadUserList()
                }).catch((e) => {
                    console.error('更新用户失败', e)
                })
            } else {
                userApi.createUser(payload).then((res) => {
                    if (res.code !== 200) {
                        ElMessage.error(res.message || res.msg || '创建失败')
                        return
                    }
                    ElMessage.success('创建成功')
                    dialogVisible.value = false
                    loadUserList()
                }).catch((e) => {
                    console.error('创建用户失败', e)
                })
            }
        }
    })
}

</script>

<style scoped>
.user-container {
    /* 外层留白交给父容器控制 */
}
.filter-card {
    margin-bottom: 20px;
}
.filter-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.left-panel {
    display: flex;
    align-items: center;
}
.user-filter-form {
    display: flex;
    align-items: center;
}
.user-filter-form :deep(.el-form-item) {
    margin-bottom: 0px;
    margin-right: 15px;
}
.user-filter-form :deep(.el-form-item__label) {
    padding-right: 8px;
    font-weight: normal;
}
.right-panel {
    display: flex;
    align-items: center;
    gap: 12px;
}

.user-info-cell {
    display: flex;
    align-items: center;
}
.info-text {
    margin-left: 10px;
}
.nickname {
    font-weight: 500;
    font-size: 14px;
}
.email {
    font-size: 12px;
    color: #909399;
}


.pagination-bar {
    margin-top: 15px;
    display: flex;
    justify-content: flex-end;
}

/* 用户详情样式 */
.detail-header {
    display: flex;
    align-items: center;
    padding-bottom: 20px;
    border-bottom: 1px solid #eee;
}
.detail-basic {
    margin-left: 20px;
}
.d-name {
    font-size: 18px;
    font-weight: bold;
    margin-bottom: 5px;
    display: flex;
    align-items: center;
    gap: 10px;
}
.d-info {
    font-size: 13px;
    color: #909399;
    margin-bottom: 8px;
}
.detail-stats {
    display: flex;
    justify-content: flex-start;
    flex-wrap: wrap;
    padding: 15px 0;
    background: #f8f9fa;
    border-radius: 8px;
    margin: 20px 0;
}
.stat-item {
    text-align: center;
    width: 25%; /* 4 items per row */
    margin-bottom: 15px;
}
.s-val {
    font-size: 20px;
    font-weight: bold;
    color: var(--admin-professional-primary);
}
.s-label {
    font-size: 12px;
    color: #606266;
}
.section-title {
    font-size: 14px;
    font-weight: bold;
    margin-bottom: 10px;
    border-left: 3px solid var(--admin-professional-primary);
    padding-left: 8px;
}
.activity-list {
    list-style: none;
    padding: 0;
    margin: 0;
}
.activity-list li {
    font-size: 13px;
    padding: 8px 0;
    border-bottom: 1px dashed #eee;
    display: flex;
    justify-content: space-between;
}
.activity-list .time {
    color: #909399;
}
</style>
