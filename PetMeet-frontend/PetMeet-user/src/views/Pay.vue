<template>
  <div class="pay-page">
    <section class="pay-shell" v-loading="loading">
      <div class="pay-main">
        <div class="pay-header">
          <div>
            <div class="eyebrow">PetMeet收银台</div>
            <h1>确认支付</h1>
          </div>
          <el-tag :type="statusTagType" effect="dark">{{ statusText }}</el-tag>
        </div>

        <div class="order-strip">
          <div>
            <span>订单编号</span>
            <strong>{{ order?.orderSn || '-' }}</strong>
          </div>
          <div>
            <span>应付金额</span>
            <strong class="money">¥{{ amountText }}</strong>
          </div>
          <div>
            <span>剩余时间</span>
            <strong>{{ remainingText }}</strong>
          </div>
        </div>

        <div class="selected-method">
          <div class="method-current">
            <span class="method-icon" :class="{ alipay: payType === 'ALIPAY', wechat: payType === 'WECHAT_MOCK' }">
              {{ payType === 'ALIPAY' ? '支' : '微' }}
            </span>
            <div class="method-copy">
              <span>当前支付方式</span>
              <strong>{{ currentMethodLabel }}</strong>
            </div>
          </div>
          <el-dropdown trigger="click" :disabled="refreshingPay" @command="handlePayTypeCommand">
            <el-button class="switch-button" text type="primary" :disabled="refreshingPay">切换</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="ALIPAY" :disabled="payType === 'ALIPAY'">支付宝</el-dropdown-item>
                <el-dropdown-item command="WECHAT_MOCK" :disabled="payType === 'WECHAT_MOCK'">微信支付</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div class="qr-panel">
          <div class="qr-stage">
            <div v-if="qrDataUrl" class="qr-frame">
              <img :src="qrDataUrl" alt="支付二维码" />
            </div>
            <el-empty v-else :image-size="90" description="等待生成二维码" />
          </div>

          <div class="channel-copy">
            <h2>{{ currentMethodLabel }}</h2>
            <p>{{ channelHint }}</p>
            <el-button
              v-if="payType === 'WECHAT_MOCK'"
              type="success"
              size="large"
              :loading="mockPaying"
              :disabled="mockPaying || refreshingPay"
              @click="confirmMockPay"
            >
              模拟支付成功
            </el-button>
            <el-button
              v-else
              type="primary"
              plain
              size="large"
              class="pay-action"
              :loading="refreshingPay"
              :disabled="refreshingPay"
              @click="refreshPay(true)"
            >
              重新生成二维码
            </el-button>
            <el-alert
              v-if="payType === 'ALIPAY' && isSandboxAlipay"
              class="sandbox-alert"
              type="warning"
              :closable="false"
              show-icon
              title="当前是支付宝沙箱通道，请使用支付宝沙箱版APP扫码，支付成功后页面会自动跳转。"
            />
          </div>
        </div>
      </div>

      <aside class="pay-side">
        <div class="side-head">
          <div>
            <span>订单摘要</span>
            <strong>订单商品</strong>
          </div>
          <em>{{ order?.items?.length || 0 }}件</em>
        </div>
        <div class="item-list">
          <div v-for="item in order?.items || []" :key="item.id" class="item-row">
            <img :src="getImageUrl(item.productImg)" alt="" />
            <div>
              <strong>{{ item.productName }}</strong>
              <span>¥{{ Number(item.price || 0).toFixed(2) }}×{{ item.quantity }}</span>
            </div>
          </div>
        </div>
        <div class="side-actions">
          <el-button class="side-button" @click="goOrders">返回订单</el-button>
          <el-button class="side-button danger-button" :loading="cancelling" :disabled="cancelling" @click="cancelOrder">取消订单</el-button>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import QRCode from 'qrcode'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { createPay, getPayStatus, mockConfirmPay } from '@/api/pay'
import { getImageUrl } from '@/utils/image'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const orderId = ref(route.query.orderId || '')
const order = ref(null)
const payType = ref(normalizePayType(route.query.payType || 'ALIPAY'))
const payInfo = ref(null)
const qrDataUrl = ref('')
const loading = ref(false)
const cancelling = ref(false)
const mockPaying = ref(false)
const refreshingPay = ref(false)
const remainingSeconds = ref(0)
const countdownTimer = ref(null)
const pollTimer = ref(null)

const methodLabelMap = {
  ALIPAY: '支付宝',
  WECHAT_MOCK: '微信支付'
}

const amountText = computed(() => Number(order.value?.totalAmount || payInfo.value?.amount || 0).toFixed(2))
const currentMethodLabel = computed(() => methodLabelMap[payType.value] || '支付')
const isSandboxAlipay = computed(() => payType.value === 'ALIPAY' && payInfo.value?.sandbox !== false)
const statusText = computed(() => {
  const status = payInfo.value?.payStatus
  if (status === 'SUCCESS') return '支付成功'
  if (status === 'CLOSED') return '已关闭'
  if (status === 'FAILED') return '支付失败'
  return '待支付'
})
const statusTagType = computed(() => {
  if (payInfo.value?.payStatus === 'SUCCESS') return 'success'
  if (payInfo.value?.payStatus === 'FAILED') return 'danger'
  if (payInfo.value?.payStatus === 'CLOSED') return 'info'
  return 'warning'
})
const remainingText = computed(() => {
  if (!remainingSeconds.value || remainingSeconds.value <= 0) return '00:00'
  const minutes = String(Math.floor(remainingSeconds.value / 60)).padStart(2, '0')
  const seconds = String(remainingSeconds.value % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
})
const channelHint = computed(() => {
  if (payType.value === 'WECHAT_MOCK') {
    return '当前为本地Mock通道，用于演示统一支付策略和状态轮询。'
  }
  if (isSandboxAlipay.value) {
    return '请使用支付宝沙箱版APP扫描二维码完成付款，正式支付宝APP无法识别沙箱二维码。'
  }
  return '请使用支付宝APP扫描二维码完成付款。'
})

function normalizePayType(value) {
  const normalized = String(value || '').toUpperCase()
  if (normalized === 'WECHAT' || normalized === 'WECHAT_MOCK') return 'WECHAT_MOCK'
  return 'ALIPAY'
}

async function fetchOrder() {
  if (!orderId.value) {
    ElMessage.warning('缺少订单编号')
    router.replace('/cart')
    return false
  }
  order.value = await request.get(`/order/detail/${orderId.value}`)
  if (order.value?.status === 1) {
    router.replace({ path: '/pay/result', query: { orderId: orderId.value } })
    return false
  }
  return true
}

async function refreshPay(forceRefresh = false) {
  if (!order.value || refreshingPay.value) return
  refreshingPay.value = true
  try {
    clearTimers()
    qrDataUrl.value = ''
    payInfo.value = await createPay({
      orderId: Number(orderId.value),
      payType: payType.value,
      payMode: 'QR_CODE',
      forceRefresh
    })
    await renderQr(payInfo.value.qrCodeUrl)
    startCountdown()
    startPolling()
  } finally {
    refreshingPay.value = false
  }
}

async function renderQr(content) {
  if (!content) {
    qrDataUrl.value = ''
    return
  }
  qrDataUrl.value = await QRCode.toDataURL(content, {
    width: 228,
    margin: 1,
    color: {
      dark: '#222222',
      light: '#ffffff'
    }
  })
}

function startCountdown() {
  updateRemaining()
  countdownTimer.value = window.setInterval(() => {
    updateRemaining()
    if (remainingSeconds.value <= 0) {
      clearTimers()
      ElMessage.warning('支付已超时')
    }
  }, 1000)
}

function updateRemaining() {
  const expireTime = payInfo.value?.expireTime
  if (!expireTime) {
    remainingSeconds.value = 0
    return
  }
  const diff = Math.floor((new Date(expireTime).getTime() - Date.now()) / 1000)
  remainingSeconds.value = Math.max(0, diff)
}

function startPolling() {
  if (!payInfo.value?.paySn) return
  pollTimer.value = window.setInterval(() => checkPayStatus(false), 3000)
}

async function checkPayStatus(syncChannel = false) {
  if (!payInfo.value?.paySn) return
  const status = await getPayStatus(
    payInfo.value.paySn,
    { syncChannel },
    syncChannel ? { timeout: 20000, silentError: true } : undefined
  )
  payInfo.value.payStatus = status.status
  if (status.status === 'SUCCESS') {
    clearTimers()
    await userStore.fetchUnpaidOrderCount()
    router.replace({
      path: '/pay/result',
      query: { orderId: orderId.value, paySn: status.paySn }
    })
  } else if (status.status === 'CLOSED' || status.status === 'FAILED') {
    clearTimers()
  }
}

async function switchPayType() {
  try {
    await refreshPay()
  } catch (error) {
    payInfo.value = null
    qrDataUrl.value = ''
  }
}

async function handlePayTypeCommand(nextType) {
  const normalized = normalizePayType(nextType)
  if (normalized === payType.value || refreshingPay.value) return
  payType.value = normalized
  await switchPayType()
}

async function confirmMockPay() {
  if (!payInfo.value?.paySn || mockPaying.value || refreshingPay.value) return
  mockPaying.value = true
  try {
    await mockConfirmPay(payInfo.value.paySn)
    await checkPayStatus(false)
  } finally {
    mockPaying.value = false
  }
}

async function cancelOrder() {
  if (!orderId.value || cancelling.value) return
  cancelling.value = true
  try {
    await request.post(`/order/cancel/${orderId.value}`)
    clearTimers()
    await userStore.fetchUnpaidOrderCount()
    ElMessage.success('订单已取消')
    router.replace({
      name: 'Profile',
      query: {
        tab: 'orders',
        orderSubTab: 'all',
        refresh: Date.now()
      }
    })
  } finally {
    cancelling.value = false
  }
}

function goOrders() {
  router.push('/profile?tab=orders')
}

function clearTimers() {
  if (countdownTimer.value) {
    window.clearInterval(countdownTimer.value)
    countdownTimer.value = null
  }
  if (pollTimer.value) {
    window.clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const shouldCreatePay = await fetchOrder()
    if (shouldCreatePay) {
      await refreshPay()
    }
  } catch (error) {
    payInfo.value = null
    qrDataUrl.value = ''
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(clearTimers)
</script>

<style scoped lang="scss">
.pay-page {
  min-height: calc(100vh - 80px);
  padding: 32px 24px;
  background: #fff;
}

.pay-shell {
  max-width: 1280px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 24px;
}

.pay-main,
.pay-side {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid #edf0f3;
  box-shadow: 0 18px 40px rgba(35, 43, 55, 0.07);
}

.pay-main {
  border-radius: 18px;
  padding: 28px;
}

.pay-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;

  .eyebrow {
    color: #ff6b81;
    font-weight: 700;
    font-size: 13px;
    margin-bottom: 6px;
  }

  h1 {
    margin: 0;
    color: #242328;
    font-size: 30px;
    letter-spacing: 0;
  }
}

.order-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 22px;

  div {
    background: #f8faf9;
    border: 1px solid #eef0ee;
    border-radius: 12px;
    padding: 14px;
  }

  span {
    display: block;
    color: #8a8f91;
    font-size: 12px;
    margin-bottom: 6px;
  }

  strong {
    color: #242328;
    font-size: 15px;
    word-break: break-all;
  }

  .money {
    color: #ff5000;
    font-size: 20px;
  }
}

.selected-method {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
  padding: 12px 14px;
  background: #f8faf9;
  border: 1px solid #eef0ee;
  border-radius: 14px;
}

.method-current {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.method-icon {
  flex: 0 0 auto;
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  color: #fff;
  font-weight: 800;
  font-size: 18px;

  &.alipay {
    background: #1677ff;
  }

  &.wechat {
    background: #07c160;
  }
}

.method-copy {
  min-width: 0;

  span {
    display: block;
    color: #8a8f91;
    font-size: 12px;
    margin-bottom: 3px;
  }

  strong {
    display: block;
    color: #242328;
    font-size: 16px;
  }
}

.switch-button {
  flex: 0 0 auto;
}

.qr-panel {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 28px;
  align-items: center;
  min-height: 320px;
}

.qr-stage {
  min-height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fdfdfb;
  border: 1px dashed #dfe5df;
  border-radius: 18px;
}

.qr-frame {
  padding: 18px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 14px 32px rgba(0, 0, 0, 0.08);

  img {
    display: block;
    width: 228px;
    height: 228px;
  }
}

.channel-copy {
  h2 {
    margin: 0 0 10px;
    color: #242328;
    font-size: 24px;
  }

  p {
    margin: 0 0 22px;
    color: #66706b;
    line-height: 1.7;
  }
}

.sandbox-alert {
  max-width: 420px;
  margin-top: 16px;
  border-radius: 10px;
}

.pay-side {
  border-radius: 18px;
  padding: 24px;
  align-self: start;
}

.side-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 18px;
  margin-bottom: 18px;
  border-bottom: 1px solid #eef0f3;

  span {
    display: block;
    color: #8a8f91;
    font-size: 12px;
    margin-bottom: 5px;
  }

  strong {
    display: block;
    color: #20242a;
    font-size: 22px;
    line-height: 1.2;
  }

  em {
    flex: 0 0 auto;
    padding: 5px 10px;
    border-radius: 999px;
    background: #f5f7fa;
    color: #77808a;
    font-size: 12px;
    font-style: normal;
  }
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: 360px;
  overflow: auto;
}

.item-row {
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  padding: 12px;
  border-radius: 14px;
  background: #fafbfc;
  border: 1px solid #f0f2f5;

  img {
    width: 74px;
    height: 74px;
    border-radius: 12px;
    object-fit: cover;
    background: #f4f4f4;
  }

  strong {
    display: block;
    color: #20242a;
    font-size: 16px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  span {
    display: block;
    color: #909399;
    font-size: 12px;
    margin-top: 4px;
  }
}

.side-actions {
  display: grid;
  gap: 12px;
  margin-top: 24px;
}

.side-button {
  width: 100%;
  height: 44px;
  margin: 0;
  border-radius: 10px;
  font-weight: 700;
}

.danger-button {
  color: #ff5a5f;
  background: #fff7f7;
  border-color: #ffc9cb;

  &:hover,
  &:focus {
    color: #ff4d52;
    background: #fff1f1;
    border-color: #ff9ea2;
  }
}

.pay-action {
  min-width: 154px;
}

@media (max-width: 900px) {
  .pay-shell,
  .qr-panel,
  .order-strip {
    grid-template-columns: 1fr;
  }

  .pay-page {
    padding: 18px 14px;
  }
}
</style>
