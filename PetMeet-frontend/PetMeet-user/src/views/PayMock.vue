<template>
  <div class="pay-container" v-loading="loading">
    <div class="pay-card">
      <h2>模拟支付</h2>
      <p class="desc">订单号：{{ orderSnDisplay }}</p>
      <p class="amount">应付金额：<span>¥{{ amountDisplay }}</span></p>

      <div class="actions">
        <el-button @click="goBack">返回</el-button>
        <el-button type="primary" :loading="paying" @click="handlePay">支付成功</el-button>
        <el-button type="danger" plain :loading="cancelling" @click="handleCancel">取消订单</el-button>
      </div>
    </div>

    <el-empty v-if="!orderId" description="缺少订单号，无法支付">
      <el-button type="primary" @click="router.push('/cart')">去购物车</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const orderId = ref(route.query.orderId || '')
const amount = ref(route.query.amount || '')
const orderSn = ref('')
const loading = ref(false)
const paying = ref(false)
const cancelling = ref(false)

const amountDisplay = computed(() => {
  if (amount.value) return Number(amount.value).toFixed(2)
  return '--'
})
const orderSnDisplay = computed(() => orderSn.value || '加载中...')

const fetchOrder = async () => {
  if (!orderId.value) return
  loading.value = true
  try {
    const detail = await request.get(`/order/detail/${orderId.value}`)
    orderSn.value = detail?.orderSn || ''
    if (!amount.value && detail?.totalAmount) {
      amount.value = detail.totalAmount
    }
  } finally {
    loading.value = false
  }
}

const handlePay = async () => {
  if (!orderId.value) {
    ElMessage.warning('缺少订单号')
    return
  }
  paying.value = true
  try {
    await request.post(`/order/pay/${orderId.value}`)
    ElMessage.success('支付成功')
    await userStore.fetchUnpaidOrderCount()
    router.replace('/profile?tab=orders')
  } finally {
    paying.value = false
  }
}

const handleCancel = async () => {
  if (!orderId.value) {
    ElMessage.warning('缺少订单号')
    return
  }
  cancelling.value = true
  try {
    await request.post(`/order/cancel/${orderId.value}`)
    ElMessage.success('订单已取消')
    await userStore.fetchUnpaidOrderCount()
    router.replace('/profile?tab=orders')
  } finally {
    cancelling.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchOrder()
})
</script>

<style scoped lang="scss">
.pay-container {
  max-width: 600px;
  margin: 60px auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.pay-card {
  width: 100%;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 32px;
  text-align: center;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.05);

  h2 {
    margin: 0 0 8px;
  }

  .desc {
    color: #666;
    margin: 0 0 16px;
  }

  .amount {
    font-size: 18px;
    margin-bottom: 24px;

    span {
      color: #f56c6c;
      font-weight: 700;
      font-size: 22px;
    }
  }
}

.actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
