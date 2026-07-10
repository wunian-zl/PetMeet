<template>
  <el-popover
    :width="width"
    :placement="placement"
    :trigger="trigger"
    :show-after="150"
    popper-class="user-info-popover"
    @show="handleShow"
  >
    <template #reference>
      <slot name="reference" />
    </template>

    <div class="card">
      <el-skeleton v-if="loading" :rows="4" animated />

      <template v-else-if="user">
        <div class="header">
          <el-avatar :size="44" :src="resolveImageUrl(user.avatar)" />
          <div class="meta">
            <div class="name-row">
              <div class="name">{{ user.nickname || user.username || '用户' }}</div>
              <el-tag size="small" :type="user.role === 'admin' ? 'danger' : 'info'" effect="plain">
                {{ user.role === 'admin' ? '管理员' : '普通用户' }}
              </el-tag>
              <el-tag v-if="user.status === 0" size="small" type="danger" effect="dark">封禁</el-tag>
            </div>
            <div class="sub">ID: {{ user.id }}</div>
          </div>
        </div>

        <el-divider style="margin: 10px 0" />

        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item label="手机号">{{ user.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ user.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="标签">
            <span v-if="user.tags">{{ user.tags }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatDateTime(user.createTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最后登录">{{ formatDateTime(user.lastLoginTime) || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>

      <el-empty v-else description="用户信息加载失败" :image-size="60" />
    </div>
  </el-popover>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getUserDetail } from '@/api/user'
import { resolveImageUrl } from '@/utils/image'

const props = defineProps({
  userId: { type: [Number, String], required: true },
  placement: { type: String, default: 'right' },
  width: { type: Number, default: 340 },
  trigger: { type: String, default: 'click' }
})

const loading = ref(false)
const user = ref(null)
const loadedUserId = ref(null)

const formatDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

const handleShow = async () => {
  const id = props.userId ? Number(props.userId) : null
  if (!id) return
  if (loadedUserId.value === id && user.value) return

  loading.value = true
  try {
    const res = await getUserDetail(id)
    if (res.code === 200) {
      user.value = res.data
      loadedUserId.value = id
    } else {
      user.value = null
      loadedUserId.value = id
    }
  } catch {
    user.value = null
    loadedUserId.value = id
  } finally {
    loading.value = false
  }
}

watch(
  () => props.userId,
  () => {
    user.value = null
    loadedUserId.value = null
  }
)
</script>

<style scoped>
.card {
  padding: 4px;
}
.header {
  display: flex;
  gap: 10px;
  align-items: center;
}
.meta {
  flex: 1;
  min-width: 0;
}
.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.name {
  font-weight: 600;
  color: #303133;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}
</style>
