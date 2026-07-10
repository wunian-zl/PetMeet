<template>
  <div class="user-container">
    <UserFilterBar />
    <UserTable />
    <UserDialogs />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, provide } from 'vue'
import { Plus, Warning, Setting, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import * as userApi from '@/api/user'
import { resolveImageUrl } from '@/utils/image'
import { useAdminStore } from '@/store/admin'
import { useAdminTable } from '@/composables/useAdminTable'

import { useRoute } from 'vue-router'
import UserFilterBar from './components/UserFilterBar.vue'
import UserTable from './components/UserTable.vue'
import UserDialogs from './components/UserDialogs.vue'

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
const {
    loading,
    allData,
    tableData,
    currentPage,
    pageSize,
    total,
    runWithLoading,
    resetPage,
    setRows
} = useAdminTable()

// 自动标签配置
const tagConfigDialogVisible = ref(false)
const tagConfig = reactive({
    newDay: 7,
    activeDay: 2,
    influencerPost: 50,
    bigSpenderAmount: 1000
})

onMounted(async () => {
    // 处理从看板带过来的查询参数
    if (route.query.startDate && route.query.endDate) {
        dateRange.value = [route.query.startDate, route.query.endDate]
    }
    
    await loadUserList()
})

const loadUserList = async () => {
    await runWithLoading(async () => {
        const params = {
            pageNum: currentPage.value,
            pageSize: pageSize.value,
            keyword: searchKeyword.value || undefined,
            role: filterRole.value || undefined,
            status: filterStatus.value === '' ? undefined : filterStatus.value
        }
        const res = await userApi.getUserList(params)
        if (res.code === 200 && res.data) {
            setRows((res.data.records || []).map(mapUserFromApi), res.data.total)
            filterData()
        } else {
            ElMessage.error(res.message || res.msg || '加载用户列表失败')
        }
    }).catch((e) => {
        console.error('加载用户列表失败', e)
    })
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
    resetPage()
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
    if (value.length < 8 || value.length > 18 || !/[A-Za-z]/.test(value) || !/\d/.test(value)) {
        callback(new Error('密码必须为8-18位，且同时包含字母和数字'))
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

provide('adminUserPageContext', {
    searchKeyword,
    filterRole,
    filterStatus,
    sortOption,
    dateRange,
    handleFilter,
    handleCreate,
    tagConfigDialogVisible,
    Plus,
    Setting,
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
    handlePageChange,
    dialogVisible,
    form,
    rules,
    formRef,
    isEditingSelf,
    avatarUploadAction,
    avatarUploadHeaders,
    beforeAvatarUpload,
    handleAvatarUploadSuccess,
    handleAvatarUploadError,
    handleHarmonizeAvatar,
    handleSubmit,
    banDialogVisible,
    banForm,
    submitBan,
    detailDialogVisible,
    currentUser,
    labelText,
    handleResetPwd,
    handleForceLogout,
    tagConfig,
    violationDialogVisible,
    currentViolationUser,
    Warning,
    CircleCheck
})

</script>

<style>
.user-container {
    /* 外层留白交给父容器控制 */
    min-width: 0;
}
.filter-card {
    margin-bottom: 20px;
}
.filter-card :deep(.el-card__body) {
    padding: 16px 18px;
}
.filter-bar {
    display: flex;
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 12px 16px;
    min-width: 0;
}
.left-panel {
    display: flex;
    align-items: flex-start;
    flex: 1 1 720px;
    min-width: 0;
}
.user-filter-form {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px 14px;
    min-width: 0;
}
.user-filter-form :deep(.el-form-item) {
    margin: 0;
}
.user-filter-form :deep(.el-form-item__label) {
    padding-right: 8px;
    font-weight: normal;
    white-space: nowrap;
}
.filter-search {
    width: 200px;
}
.filter-select-role {
    width: 112px;
}
.filter-select-status {
    width: 104px;
}
.filter-sort {
    width: 168px;
}
.filter-date-range {
    width: 280px;
    max-width: 100%;
}
.right-panel {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex: 0 0 auto;
    margin-left: auto;
    gap: 12px;
    white-space: nowrap;
}
.right-panel :deep(.el-button) {
    margin-left: 0;
}
.table-card {
    min-width: 0;
}
.table-card :deep(.el-card__body) {
    min-width: 0;
    overflow: hidden;
}
.user-table {
    min-width: 0;
}
.user-table :deep(.el-table__cell) {
    padding: 10px 0;
}
.user-table :deep(.el-table__fixed-right) {
    box-shadow: -8px 0 12px -12px rgba(31, 45, 61, 0.45);
}
.row-actions {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    white-space: nowrap;
}
.row-actions :deep(.el-button) {
    margin-left: 0;
}

.user-info-cell {
    display: flex;
    align-items: center;
    min-width: 0;
    gap: 10px;
}
.user-avatar {
    flex: 0 0 auto;
    border-radius: 50%;
    overflow: hidden;
}
.user-avatar :deep(img) {
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: center;
}
.info-text {
    min-width: 0;
}
.nickname {
    font-weight: 500;
    font-size: 14px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.email {
    font-size: 12px;
    color: #909399;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

@media (max-width: 1280px) {
    .left-panel {
        flex-basis: 100%;
    }
    .right-panel {
        margin-left: 0;
    }
}

@media (max-width: 760px) {
    .filter-search,
    .filter-sort,
    .filter-date-range {
        width: min(100%, 320px);
    }
    .right-panel {
        width: 100%;
        justify-content: flex-start;
    }
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
