<template>
  <div class="follow-page">
    <div class="page-header">
      <div class="title">关注与粉丝</div>
      <el-button text @click="router.back()">返回</el-button>
    </div>

    <el-tabs v-model="activeTab" class="follow-tabs">
      <el-tab-pane label="关注" name="following" />
      <el-tab-pane label="粉丝" name="followers" />
    </el-tabs>

    <div class="list-area" v-loading="loading">
      <el-empty v-if="!loading && list.length === 0" description="暂无数据" />
      <div v-else class="user-list">
        <div v-for="user in list" :key="user.id" class="user-card">
          <el-avatar :size="48" :src="user.avatar" icon="UserFilled" />
          <div class="info">
            <div class="name">{{ user.nickname || '用户' }}</div>
            <div class="sub">ID: {{ user.id }}</div>
          </div>
          <el-button
            v-if="user.id !== userStore.userInfo?.id"
            size="small"
            :type="user.followed ? 'info' : 'primary'"
            plain
            @click="toggleFollow(user)"
          >
            {{ user.followed ? '已关注' : '关注' }}
          </el-button>
        </div>
      </div>

      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          v-model:current-page="pageNum"
          @current-change="loadList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { getAvatarUrl } from '@/utils/image'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('following')
const list = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 12
const total = ref(0)

const loadList = async () => {
  if (!userStore.userInfo?.id) return
  loading.value = true
  try {
    const api = activeTab.value === 'followers' ? 'followers' : 'following'
    const res = await request.get(`/follow/${api}/${userStore.userInfo.id}`, {
      params: { pageNum: pageNum.value, pageSize }
    })
    const records = res?.records || []
    list.value = records.map((u) => ({
      ...u,
      avatar: getAvatarUrl(u.avatar)
    }))
    total.value = res?.total || records.length
  } finally {
    loading.value = false
  }
}

const toggleFollow = async (user) => {
  try {
    const followed = await request.post(`/follow/${user.id}`)
    user.followed = Boolean(followed)
    ElMessage.success(followed ? '关注成功' : '已取消关注')
  } catch (e) {
    // 这里交给拦截器统一处理
  }
}

watch(activeTab, () => {
  pageNum.value = 1
  loadList()
})

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'following' || tab === 'followers') {
      activeTab.value = tab
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadList()
})
</script>

<style scoped lang="scss">
.follow-page {
  max-width: 900px;
  margin: 20px auto;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.list-area {
  margin-top: 10px;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.info {
  flex: 1;
}

.name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.sub {
  font-size: 12px;
  color: #909399;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
