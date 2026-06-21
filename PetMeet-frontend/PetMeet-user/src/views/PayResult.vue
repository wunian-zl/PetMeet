<template>
  <div class="result-page" v-loading="loading">
    <section class="result-card">
      <div class="result-mark">
        <el-icon><CircleCheckFilled /></el-icon>
      </div>
      <h1>支付成功</h1>
      <p>订单{{ order?.orderSn || '-' }}已完成支付</p>
      <div class="result-meta">
        <span>实付金额</span>
        <strong>¥{{ Number(order?.totalAmount || 0).toFixed(2) }}</strong>
        <span>支付方式</span>
        <strong>{{ order?.payType || '-' }}</strong>
        <span>支付时间</span>
        <strong>{{ formatTime(order?.payTime) }}</strong>
      </div>
      <div class="result-actions">
        <el-button type="primary" @click="router.replace('/profile?tab=orders')">查看订单</el-button>
        <el-button @click="router.replace('/shop')">继续购物</el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const loading = ref(false)

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(async () => {
  const orderId = route.query.orderId
  if (!orderId) {
    router.replace('/profile?tab=orders')
    return
  }
  loading.value = true
  try {
    order.value = await request.get(`/order/detail/${orderId}`)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.result-page {
  min-height: calc(100vh - 80px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  background: #fff;
}

.result-card {
  width: min(520px, 100%);
  background: #fff;
  border-radius: 18px;
  padding: 34px;
  text-align: center;
  box-shadow: 0 18px 45px rgba(43, 31, 35, 0.08);

  h1 {
    margin: 12px 0 8px;
    font-size: 30px;
    color: #242328;
    letter-spacing: 0;
  }

  p {
    margin: 0 0 22px;
    color: #6f7672;
  }
}

.result-mark {
  width: 70px;
  height: 70px;
  margin: 0 auto;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ecf9f1;
  color: #21b36b;
  font-size: 42px;
}

.result-meta {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr);
  gap: 12px;
  text-align: left;
  padding: 18px;
  background: #f8faf9;
  border-radius: 12px;

  span {
    color: #8a8f91;
  }

  strong {
    color: #303133;
    word-break: break-all;
  }
}

.result-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
}
</style>
