<template>
  <div class="notice-container">
    <div class="notice-header">
      <div class="title">通知</div>
      <div class="actions">
        <el-button @click="reload" :loading="headerLoading">刷新</el-button>
        <el-button
          v-if="activeTab !== 'complaint'"
          class="read-all-btn"
          @click="handleReadAll"
          :disabled="noticeTotal === 0"
        >
          全部已读
        </el-button>
        <el-button
          v-if="activeTab !== 'complaint'"
          type="danger"
          plain
          @click="handleBatchDelete"
          :disabled="selectedNoticeIds.length === 0"
        >
          批量删除{{ selectedNoticeIds.length > 0 ? ` (${selectedNoticeIds.length})` : '' }}
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="notice-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="未读" name="unread" />
      <el-tab-pane label="投诉" name="complaint" />
    </el-tabs>

    <template v-if="activeTab !== 'complaint'">
      <div v-if="noticeList.length > 0" class="notice-batch-bar">
        <el-checkbox
          :model-value="allNoticesSelected"
          :indeterminate="noticeSelectionIndeterminate"
          @change="handleToggleSelectAll"
        >
          全选当前页
        </el-checkbox>
        <span class="selected-text">已选 {{ selectedNoticeIds.length }} 条</span>
      </div>

      <div class="notice-list" v-loading="noticeLoading">
        <div
          v-for="item in noticeList"
          :key="item.id"
          class="notice-item"
          :class="{ unread: item.isRead === 0 }"
          @click="handleOpen(item)"
        >
          <div class="select-col" @click.stop>
            <el-checkbox
              :model-value="isNoticeSelected(item.id)"
              @change="(checked) => handleNoticeSelect(item, checked)"
            />
          </div>
          <div class="dot" v-if="item.isRead === 0" />
          <div class="main">
            <div class="row1">
              <div class="ntitle">{{ item.title }}</div>
              <div class="time">{{ formatDateTime(item.createTime) }}</div>
            </div>
            <div v-if="item.content" class="content">{{ item.content }}</div>
          </div>
          <div class="item-actions">
            <el-button
              v-if="item.isRead === 0"
              link
              class="mark-read-btn"
              size="small"
              @click.stop="handleMarkRead(item)"
            >
              标记已读
            </el-button>
            <el-button
              link
              type="danger"
              class="delete-btn"
              size="small"
              @click.stop="handleDelete(item)"
            >
              删除
            </el-button>
          </div>
        </div>

        <el-empty v-if="!noticeLoading && noticeList.length === 0" description="暂无通知" :image-size="90" />
      </div>

      <div class="pager" v-if="noticeTotal > noticePageSize">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="noticeTotal"
          :page-size="noticePageSize"
          v-model:current-page="noticePageNum"
          @current-change="handleNoticePageChange"
        />
      </div>
    </template>

    <template v-else>
      <div class="complaint-panel" v-loading="complaintLoading">
        <el-alert v-if="hasPendingComplaint"
          title="投诉核查中请耐心等待，处理完成后可在此反馈满意度"
          type="info"
          :closable="false"
          show-icon
          class="complaint-tip"
        />

        <div class="complaint-filters">
          <el-radio-group v-model="complaintStatusFilter" @change="handleComplaintFilter">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="pending">核查中</el-radio-button>
            <el-radio-button label="handled">已处理</el-radio-button>
            <el-radio-button label="rejected">已驳回</el-radio-button>
          </el-radio-group>
        </div>

        <div
          v-for="item in complaintList"
          :key="item.id"
          class="complaint-item"
          @click="openComplaintDetail(item.id)"
        >
          <div class="c-row1">
            <div class="c-title">
              <el-tag :type="complaintStatusTagType(item.status)" size="small" effect="plain">
                {{ complaintStatusText(item.status) }}
              </el-tag>
              <span class="c-note" @click.stop="goToNote(item.noteId)">{{ item.noteTitle || '笔记' }}</span>
            </div>
            <div class="time">{{ formatDateTime(item.createTime) }}</div>
          </div>
          <div class="c-body">
            <div class="c-line"><span class="label">原因：</span>{{ item.reason || '-' }}</div>
            <div v-if="item.content" class="c-line"><span class="label">说明：</span>{{ item.content }}</div>
            <div v-if="item.status !== 0" class="c-line">
              <span class="label">处理说明：</span>{{ item.handleRemark || '（无）' }}
            </div>
          </div>
          <div class="c-actions">
            <template v-if="item.status === 0">
              <el-tag type="warning" effect="plain">核查中，请耐心等待</el-tag>
            </template>
            <template v-else-if="!item.feedbackStatus || item.feedbackStatus === 0">
              <template v-if="isLatestComplaint(item)">
                <el-button size="small" type="success" @click.stop="handleFeedback(item, 1)">满意</el-button>
                <el-button size="small" type="warning" :loading="feedbackSubmitting" @click.stop="handleFeedback(item, 2)">
                  不满意
                </el-button>
                <el-button size="small" type="danger" link @click.stop="openReComplaint(item)">不满意，再投诉</el-button>
              </template>
              <template v-else>
                <el-tag type="info" effect="plain">历史记录</el-tag>
              </template>
            </template>
            <template v-else-if="item.feedbackStatus === 1">
              <el-tag type="success" effect="plain">已反馈：满意</el-tag>
            </template>
            <template v-else>
              <el-tag type="danger" effect="plain">已反馈：不满意</el-tag>
              <el-button v-if="isLatestComplaint(item)" size="small" type="primary" @click.stop="openReComplaint(item)">
                再次投诉
              </el-button>
            </template>
            <el-button
              v-if="canDeleteComplaint(item)"
              size="small"
              type="danger"
              link
              @click.stop="handleDeleteComplaint(item)"
            >
              删除
            </el-button>
          </div>
        </div>

        <el-empty v-if="!complaintLoading && complaintList.length === 0" description="暂无投诉记录" :image-size="90" />
      </div>

      <div class="pager" v-if="complaintTotal > complaintPageSize">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="complaintTotal"
          :page-size="complaintPageSize"
          v-model:current-page="complaintPageNum"
          @current-change="handleComplaintPageChange"
        />
      </div>
    </template>

    <el-dialog v-model="complaintDetailVisible" title="投诉详情" width="560px" @closed="resetComplaintDetail">
      <div v-loading="complaintDetailLoading">
        <div v-if="complaintDetail" class="complaint-detail">
          <div class="detail-row">
            <span class="label">笔记：</span>
            <el-link type="primary" :underline="false" @click="goToNote(complaintDetail.noteId)">
              {{ complaintDetail.noteTitle || `#${complaintDetail.noteId}` }}
            </el-link>
          </div>
          <div class="detail-row"><span class="label">原因：</span>{{ complaintDetail.reason || '-' }}</div>
          <div v-if="complaintDetail.content" class="detail-row pre"><span class="label">说明：</span>{{ complaintDetail.content }}</div>
          <div v-if="complaintEvidenceImages(complaintDetail).length" class="detail-row evidence-detail-row">
            <span class="label">凭证：</span>
            <div class="complaint-evidence-grid">
              <el-image
                v-for="(img, idx) in complaintEvidenceImages(complaintDetail)"
                :key="`complaint-evidence-${idx}`"
                class="complaint-evidence-image"
                :src="img"
                :preview-src-list="complaintEvidenceImages(complaintDetail)"
                :initial-index="idx"
                fit="cover"
                preview-teleported
              />
            </div>
          </div>
          <div class="detail-row"><span class="label">状态：</span>{{ complaintStatusText(complaintDetail.status) }}</div>
          <div class="detail-row"><span class="label">提交时间：</span>{{ formatDateTime(complaintDetail.createTime) }}</div>
          <div v-if="complaintDetail.handleTime" class="detail-row"><span class="label">处理时间：</span>{{ formatDateTime(complaintDetail.handleTime) }}</div>
          <div v-if="complaintDetail.status !== 0" class="detail-row pre">
            <span class="label">处理说明：</span>{{ complaintDetail.handleRemark || '（无）' }}
          </div>
        </div>
        <el-empty v-else description="暂无数据" :image-size="90" />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <template v-if="complaintDetail && complaintDetail.status !== 0">
            <template v-if="!complaintDetail.feedbackStatus || complaintDetail.feedbackStatus === 0">
              <el-button @click="complaintDetailVisible = false">稍后再说</el-button>
              <el-button type="success" :loading="feedbackSubmitting" @click="handleFeedback(complaintDetail, 1)">
                满意
              </el-button>
              <el-button type="warning" :loading="feedbackSubmitting" @click="handleFeedback(complaintDetail, 2)">
                不满意
              </el-button>
              <el-button type="danger" @click="openReComplaint(complaintDetail)">不满意，再投诉</el-button>
            </template>
            <template v-else>
              <el-button type="primary" @click="complaintDetailVisible = false">关闭</el-button>
            </template>
            <el-button
              v-if="canDeleteComplaint(complaintDetail)"
              type="danger"
              @click="handleDeleteComplaint(complaintDetail)"
            >
              删除投诉
            </el-button>
          </template>
          <template v-else>
            <el-button type="primary" @click="complaintDetailVisible = false">关闭</el-button>
          </template>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="reComplaintVisible" title="再次投诉" width="520px" @closed="resetReComplaint">
      <el-form label-width="90px" class="re-complaint-form">
        <el-form-item label="投诉原因">
          <el-radio-group v-model="reComplaintForm.reason">
            <el-radio v-for="r in complaintReasons" :key="r" :label="r">{{ r }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="reComplaintForm.content" type="textarea" :rows="3" placeholder="补充说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reComplaintVisible = false">取消</el-button>
        <el-button type="primary" :loading="reComplaintSubmitting" @click="submitReComplaint">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="orderDetailVisible" title="订单详情" width="760px" destroy-on-close align-center class="order-detail-dialog">
      <div v-loading="orderDetailLoading" class="order-detail-container">
        <template v-if="orderDetail">
          <div class="order-detail-header">
            <div class="status-row">
              <span class="label">当前状态</span>
              <span class="status-text" :class="getOrderStatusClass(orderDetail)">
                {{ getOrderStatusText(orderDetail) }}
              </span>
            </div>
            <div class="sn-row">编号: {{ orderDetail.orderSn }}</div>
          </div>

          <div class="order-detail-section">
            <h3>收货信息</h3>
            <div class="info-row">{{ orderDetail.receiverName || '-' }} {{ orderDetail.receiverPhone || '' }}</div>
            <div class="info-row">{{ orderDetail.receiverAddress || '-' }}</div>
          </div>

          <div class="order-detail-section">
            <h3>商品清单</h3>
            <div class="product-list-mini">
              <div v-for="item in (orderDetail.items || [])" :key="item.id" class="product-item-mini">
                <img class="p-thumb" :src="getImageUrl(item.productImg)" />
                <div class="p-info">
                  <div class="p-name">{{ item.productName }}</div>
                  <div class="p-meta">¥{{ Number(item.price || 0).toFixed(2) }} x {{ item.quantity }}</div>
                </div>
                <div class="p-total">¥{{ Number(item.subtotal ?? Number(item.price || 0) * Number(item.quantity || 0)).toFixed(2) }}</div>
              </div>
            </div>
          </div>

          <div class="order-detail-footer">
            <span class="label">实付金额:</span>
            <span class="amount">¥{{ Number(orderDetail.totalAmount || 0).toFixed(2) }}</span>
          </div>
        </template>
        <el-empty v-else description="暂无订单信息" :image-size="90" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  getNotificationList,
  markNotificationRead,
  markAllNotificationsRead,
  deleteNotification,
  deleteNotifications
} from '@/api/notification'
import {
  getMyComplaintList,
  getMyComplaintDetail,
  deleteMyComplaint,
  submitComplaint,
  feedbackComplaint
} from '@/api/complaint'
import request from '@/utils/request'
import { getImageUrl } from '@/utils/image'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('all') // all/unread/complaint

const noticeLoading = ref(false)
const noticeList = ref([])
const noticePageNum = ref(1)
const noticePageSize = 20
const noticeTotal = ref(0)
const selectedNoticeIds = ref([])

const complaintLoading = ref(false)
const complaintList = ref([])
const complaintPageNum = ref(1)
const complaintPageSize = 10
const complaintTotal = ref(0)
const complaintStatusFilter = ref('all') // all/pending/handled/rejected

const complaintDetailVisible = ref(false)
const complaintDetailLoading = ref(false)
const complaintDetail = ref(null)
const feedbackSubmitting = ref(false)

const reComplaintVisible = ref(false)
const reComplaintSubmitting = ref(false)
const reComplaintForm = reactive({
  noteId: null,
  parentId: null,
  reason: '',
  content: ''
})
const orderDetailVisible = ref(false)
const orderDetailLoading = ref(false)
const orderDetail = ref(null)
const complaintReasons = ['侵权', '盗用', '其他']

const headerLoading = computed(() =>
  activeTab.value === 'complaint' ? complaintLoading.value : noticeLoading.value
)

const hasPendingComplaint = computed(() =>
  (complaintList.value || []).some((item) => item?.status === 0)
)

const complaintEvidenceImages = (item) => {
  if (!Array.isArray(item?.evidenceImages)) return []
  return item.evidenceImages.filter(Boolean).map((img) => getImageUrl(img))
}

const latestComplaintIdByNote = computed(() => {
  const map = {}
  for (const item of complaintList.value || []) {
    const noteId = item?.noteId
    if (!noteId) continue
    if (map[noteId] == null) {
      map[noteId] = item.id
    }
  }
  return map
})

const isLatestComplaint = (item) => {
  const noteId = item?.noteId
  if (!noteId) return true
  return latestComplaintIdByNote.value[noteId] === item.id
}

const formatDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

const isPendingReviewOrder = (order) => (
  Number(order?.status) === 3 && Number(order?.reviewStatus ?? 0) === 0
)

const getOrderStatusText = (order) => {
  if (!order) return '未知'
  const status = Number(order.status)
  if (status === 0) return '待支付'
  if (status === 1) return '待发货'
  if (status === 2) return '待收货'
  if (status === 3) return isPendingReviewOrder(order) ? '待评价' : '已完成'
  if (status === 4) return '已关闭'
  if (status === 5) return '退款中'
  return order.statusDesc || '未知'
}

const getOrderStatusClass = (order) => {
  const status = Number(order?.status)
  if (status === 2) return 'status-pending-receipt'
  if (status === 3 && !isPendingReviewOrder(order)) return 'status-completed'
  if (status === 4) return 'status-closed'
  if (status === 5) return 'status-refunding'
  return ''
}

const openOrderDetailDialog = async (orderId) => {
  if (!orderId) return
  orderDetailVisible.value = true
  orderDetailLoading.value = true
  orderDetail.value = null
  try {
    const res = await request.get(`/order/detail/${orderId}`)
    orderDetail.value = res
  } catch (e) {
    ElMessage.error(e?.message || '加载订单详情失败')
  } finally {
    orderDetailLoading.value = false
  }
}

const LEGACY_NOTICE_TITLE_MAP = {
  'Order shipped': '\u8BA2\u5355\u5DF2\u53D1\u8D27',
  'Order closed': '\u8BA2\u5355\u5DF2\u5173\u95ED',
  'Refund approved': '\u9000\u6B3E\u5DF2\u901A\u8FC7',
  'Refund rejected': '\u9000\u6B3E\u5DF2\u9A73\u56DE',
  'Cancel request submitted': '\u53D6\u6D88\u7533\u8BF7\u5DF2\u63D0\u4EA4',
  'Refunding order requires priority handling': '\u9000\u6B3E\u8BA2\u5355\u5F85\u4F18\u5148\u5904\u7406'
}

const extractOrderSnFromLegacyContent = (content) => {
  if (!content) return ''
  const match = String(content).match(/(?:Your order |Order )([0-9A-Z]+)/)
  return match?.[1] || ''
}

const localizeLegacyNotification = (item) => {
  if (!item) return item
  const title = String(item.title || '')
  const content = String(item.content || '')
  const orderSn = extractOrderSnFromLegacyContent(content)

  let localizedContent = item.content
  if (title === 'Order shipped') {
    localizedContent = orderSn
      ? `\u4F60\u7684\u8BA2\u5355${orderSn}\u5DF2\u53D1\u8D27`
      : '\u4F60\u7684\u8BA2\u5355\u5DF2\u53D1\u8D27'
  } else if (title === 'Order closed') {
    localizedContent = orderSn
      ? `\u8BA2\u5355${orderSn}\u56E0\u8D85\u65F6\u672A\u652F\u4ED8\uFF0C\u7CFB\u7EDF\u5DF2\u81EA\u52A8\u5173\u95ED`
      : '\u8BA2\u5355\u56E0\u8D85\u65F6\u672A\u652F\u4ED8\uFF0C\u7CFB\u7EDF\u5DF2\u81EA\u52A8\u5173\u95ED'
  } else if (title === 'Refund approved') {
    localizedContent = orderSn
      ? `\u8BA2\u5355${orderSn}\u9000\u6B3E\u7533\u8BF7\u5DF2\u901A\u8FC7`
      : '\u8BA2\u5355\u9000\u6B3E\u7533\u8BF7\u5DF2\u901A\u8FC7'
  } else if (title === 'Refund rejected') {
    const reasonMatch = content.match(/refund was rejected:\s*(.+)$/i)
    const reason = reasonMatch?.[1] || ''
    if (orderSn) {
      localizedContent = reason
        ? `\u8BA2\u5355${orderSn}\u9000\u6B3E\u7533\u8BF7\u88AB\u9A73\u56DE\uFF0C\u539F\u56E0\uFF1A${reason}`
        : `\u8BA2\u5355${orderSn}\u9000\u6B3E\u7533\u8BF7\u88AB\u9A73\u56DE`
    } else {
      localizedContent = reason
        ? `\u8BA2\u5355\u9000\u6B3E\u7533\u8BF7\u88AB\u9A73\u56DE\uFF0C\u539F\u56E0\uFF1A${reason}`
        : '\u8BA2\u5355\u9000\u6B3E\u7533\u8BF7\u88AB\u9A73\u56DE'
    }
  } else if (title === 'Cancel request submitted') {
    localizedContent = orderSn
      ? `\u8BA2\u5355${orderSn}\u5DF2\u8FDB\u5165\u9000\u6B3E\u5904\u7406\u4E2D\u72B6\u6001`
      : '\u8BA2\u5355\u5DF2\u8FDB\u5165\u9000\u6B3E\u5904\u7406\u4E2D\u72B6\u6001'
  } else if (title === 'Refunding order requires priority handling') {
    localizedContent = orderSn
      ? `\u8BA2\u5355${orderSn}\u6B63\u5728\u9000\u6B3E\u4E2D\uFF0C\u8BF7\u5148\u6682\u505C\u53D1\u8D27\u5E76\u4F18\u5148\u5904\u7406\u9000\u6B3E`
      : '\u8BA2\u5355\u6B63\u5728\u9000\u6B3E\u4E2D\uFF0C\u8BF7\u5148\u6682\u505C\u53D1\u8D27\u5E76\u4F18\u5148\u5904\u7406\u9000\u6B3E'
  }

  return {
    ...item,
    title: LEGACY_NOTICE_TITLE_MAP[title] || item.title,
    content: localizedContent
  }
}

const loadNotices = async () => {
  noticeLoading.value = true
  try {
    const res = await getNotificationList({
      pageNum: noticePageNum.value,
      pageSize: noticePageSize,
      unreadOnly: activeTab.value === 'unread' ? 1 : undefined
    })
    noticeList.value = (res?.records || []).map(localizeLegacyNotification)
    noticeTotal.value = res?.total || 0
    selectedNoticeIds.value = []
  } catch (e) {
    ElMessage.error(e?.message || '加载通知失败')
  } finally {
    noticeLoading.value = false
  }
}

const allNoticesSelected = computed(() => {
  const ids = (noticeList.value || [])
    .map((item) => Number(item?.id))
    .filter((id) => Number.isInteger(id) && id > 0)
  return ids.length > 0 && ids.every((id) => selectedNoticeIds.value.includes(id))
})

const noticeSelectionIndeterminate = computed(() => {
  const ids = (noticeList.value || [])
    .map((item) => Number(item?.id))
    .filter((id) => Number.isInteger(id) && id > 0)
  if (ids.length === 0) return false
  const selectedCount = ids.filter((id) => selectedNoticeIds.value.includes(id)).length
  return selectedCount > 0 && selectedCount < ids.length
})

const isNoticeSelected = (id) => selectedNoticeIds.value.includes(Number(id))

const handleNoticeSelect = (item, checked) => {
  const id = Number(item?.id)
  if (!Number.isInteger(id) || id <= 0) return
  if (checked) {
    if (!selectedNoticeIds.value.includes(id)) {
      selectedNoticeIds.value.push(id)
    }
    return
  }
  selectedNoticeIds.value = selectedNoticeIds.value.filter((x) => x !== id)
}

const handleToggleSelectAll = (checked) => {
  if (!checked) {
    selectedNoticeIds.value = []
    return
  }
  selectedNoticeIds.value = (noticeList.value || [])
    .map((item) => Number(item?.id))
    .filter((id) => Number.isInteger(id) && id > 0)
}

const mapStatusFilter = (val) => {
  if (val === 'pending') return 0
  if (val === 'handled') return 1
  if (val === 'rejected') return 2
  return undefined
}

const complaintStatusText = (status) => {
  if (status === 1) return '已处理'
  if (status === 2) return '已驳回'
  return '核查中'
}

const complaintStatusTagType = (status) => {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'warning'
}

const canDeleteComplaint = (item) => {
  const id = Number(item?.id)
  if (!Number.isInteger(id) || id <= 0) return false
  return Number(item?.status) !== 0
}

const loadComplaints = async () => {
  complaintLoading.value = true
  try {
    const res = await getMyComplaintList({
      pageNum: complaintPageNum.value,
      pageSize: complaintPageSize,
      status: mapStatusFilter(complaintStatusFilter.value)
    })
    complaintList.value = res?.records || []
    complaintTotal.value = res?.total || 0
  } catch (e) {
    ElMessage.error(e?.message || '加载投诉记录失败')
  } finally {
    complaintLoading.value = false
  }
}

const reload = async () => {
  if (activeTab.value === 'complaint') {
    await loadComplaints()
    return
  }
  await loadNotices()
  userStore.fetchNotificationUnreadCount()
}

const handleTabChange = () => {
  if (activeTab.value === 'complaint') {
    complaintPageNum.value = 1
  } else {
    noticePageNum.value = 1
  }
  selectedNoticeIds.value = []
  reload()
}

const handleNoticePageChange = () => {
  reload()
}

const handleComplaintFilter = () => {
  complaintPageNum.value = 1
  loadComplaints()
}

const handleComplaintPageChange = () => {
  loadComplaints()
}

const handleDeleteComplaint = async (item) => {
  const id = Number(item?.id)
  if (!Number.isInteger(id) || id <= 0) return
  if (!canDeleteComplaint(item)) {
    ElMessage.info('投诉核查中，暂不能删除')
    return
  }
  try {
    await ElMessageBox.confirm('删除后不可恢复，是否继续？', '删除投诉记录', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteMyComplaint(id)
    if (complaintDetail.value && Number(complaintDetail.value.id) === id) {
      complaintDetailVisible.value = false
    }
    if (complaintList.value.length === 1 && complaintTotal.value > 1 && complaintPageNum.value > 1) {
      complaintPageNum.value -= 1
    }
    await loadComplaints()
    ElMessage.success('投诉记录已删除')
  } catch (e) {
    if (e === 'cancel' || e?.message === 'cancel' || e === 'close' || e?.message === 'close') {
      return
    }
    ElMessage.error(e?.message || '删除投诉失败')
  }
}

const handleMarkRead = async (item) => {
  if (!item || item.isRead === 1) return
  try {
    await markNotificationRead(item.id)
    item.isRead = 1
    item.readTime = new Date().toISOString()
    userStore.fetchNotificationUnreadCount()
    if (activeTab.value === 'unread') {
      noticeList.value = noticeList.value.filter((x) => x.id !== item.id)
      noticeTotal.value = Math.max(0, noticeTotal.value - 1)
    }
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const handleReadAll = async () => {
  try {
    await markAllNotificationsRead()
    ElMessage.success('已全部标记为已读')
    noticePageNum.value = 1
    await reload()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const handleDelete = async (item) => {
  if (!item?.id) return
  try {
    await ElMessageBox.confirm('删除后不可恢复，是否继续？', '删除通知', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteNotification(item.id)
    noticeList.value = noticeList.value.filter((x) => x.id !== item.id)
    noticeTotal.value = Math.max(0, noticeTotal.value - 1)
    selectedNoticeIds.value = selectedNoticeIds.value.filter((id) => id !== Number(item.id))

    if (noticeList.value.length === 0 && noticeTotal.value > 0 && noticePageNum.value > 1) {
      noticePageNum.value -= 1
      await loadNotices()
    }

    await userStore.fetchNotificationUnreadCount()
    ElMessage.success('删除成功')
  } catch (e) {
    if (e === 'cancel' || e?.message === 'cancel' || e === 'close' || e?.message === 'close') {
      return
    }
    ElMessage.error(e?.message || '删除失败')
  }
}

const handleBatchDelete = async () => {
  const ids = Array.from(
    new Set(selectedNoticeIds.value.filter((id) => Number.isInteger(id) && id > 0))
  )
  if (ids.length === 0) {
    ElMessage.warning('请先选择通知')
    return
  }

  try {
    await ElMessageBox.confirm(`确认删除选中的 ${ids.length} 条通知？删除后不可恢复。`, '批量删除通知', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteNotifications(ids)
    const idSet = new Set(ids)
    noticeList.value = noticeList.value.filter((item) => !idSet.has(Number(item.id)))
    noticeTotal.value = Math.max(0, noticeTotal.value - ids.length)
    selectedNoticeIds.value = []

    if (noticeList.value.length === 0 && noticeTotal.value > 0 && noticePageNum.value > 1) {
      noticePageNum.value -= 1
      await loadNotices()
    }

    await userStore.fetchNotificationUnreadCount()
    ElMessage.success(`已删除 ${ids.length} 条通知`)
  } catch (e) {
    if (e === 'cancel' || e?.message === 'cancel' || e === 'close' || e?.message === 'close') {
      return
    }
    ElMessage.error(e?.message || '批量删除失败')
  }
}

const goToNote = (noteId) => {
  if (!noteId) return
  router.push(`/note/detail/${noteId}`)
}

const resetComplaintDetail = () => {
  complaintDetailLoading.value = false
  complaintDetail.value = null
  feedbackSubmitting.value = false
}

const openComplaintDetail = async (id) => {
  if (!id) return
  complaintDetailVisible.value = true
  complaintDetailLoading.value = true
  complaintDetail.value = null
  try {
    complaintDetail.value = await getMyComplaintDetail(id)
  } catch (e) {
    ElMessage.error(e?.message || '加载投诉详情失败')
  } finally {
    complaintDetailLoading.value = false
  }
}

const handleFeedback = async (item, feedbackStatus) => {
  if (!item || item.status === 0) return
  if (feedbackStatus !== 1 && feedbackStatus !== 2) return
  feedbackSubmitting.value = true
  try {
    let feedbackContent = ''
    if (feedbackStatus === 2) {
      const { value } = await ElMessageBox.prompt('可选：补充不满意原因（管理员可见）', '反馈不满意', {
        confirmButtonText: '提交',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：处理不充分/仍存在违规内容等（可不填）'
      })
      feedbackContent = (value || '').trim()
    }

    await feedbackComplaint(item.id, {
      feedbackStatus,
      content: feedbackContent
    })

    item.feedbackStatus = feedbackStatus
    if (feedbackStatus === 2) {
      item.feedbackContent = feedbackContent || item.feedbackContent
      item.feedbackTime = new Date().toISOString()
    }
    if (complaintDetail.value && complaintDetail.value.id === item.id) {
      complaintDetail.value.feedbackStatus = feedbackStatus
      if (feedbackStatus === 2) {
        complaintDetail.value.feedbackContent = feedbackContent || complaintDetail.value.feedbackContent
        complaintDetail.value.feedbackTime = new Date().toISOString()
      }
    }

    ElMessage.success(feedbackStatus === 1 ? '已反馈：满意' : '已反馈：不满意')
    await loadComplaints()
  } catch (e) {
    if (e === 'cancel' || e?.message === 'cancel') {
      return
    }
    ElMessage.error(e?.message || '反馈失败')
  } finally {
    feedbackSubmitting.value = false
  }
}

const resetReComplaint = () => {
  reComplaintSubmitting.value = false
  reComplaintForm.noteId = null
  reComplaintForm.parentId = null
  reComplaintForm.reason = ''
  reComplaintForm.content = ''
}

const openReComplaint = (item) => {
  if (!item) return
  if (item.status === 0) {
    ElMessage.info('投诉正在核查中，请耐心等待')
    return
  }
  if (!isLatestComplaint(item)) {
    ElMessage.info('请在该笔记的最新投诉记录上再次投诉')
    return
  }
  reComplaintForm.noteId = item.noteId
  reComplaintForm.parentId = item.id
  reComplaintForm.reason = ''
  reComplaintForm.content = ''
  reComplaintVisible.value = true
}

const submitReComplaint = async () => {
  if (!reComplaintForm.noteId || !reComplaintForm.parentId) return
  if (!reComplaintForm.reason) {
    ElMessage.warning('请选择投诉原因')
    return
  }
  reComplaintSubmitting.value = true
  try {
    await submitComplaint({
      noteId: Number(reComplaintForm.noteId),
      parentId: Number(reComplaintForm.parentId),
      reason: reComplaintForm.reason,
      content: reComplaintForm.content?.trim() || ''
    })
    ElMessage.success('已再次投诉，正在核查中')
    reComplaintVisible.value = false
    complaintPageNum.value = 1
    await loadComplaints()
    await nextTick()
  } catch (e) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    reComplaintSubmitting.value = false
  }
}

const handleOpen = async (item) => {
  if (!item) return
  if (item.isRead === 0) {
    await handleMarkRead(item)
  }
  if (item.bizType === 'note' && item.bizId) {
    router.push(`/note/detail/${item.bizId}`)
    return
  }
  if (item.bizType === 'after_sale') {
    router.push({
      path: '/profile',
      query: {
        tab: 'orders',
        orderSubTab: 'after_sale',
        afterSaleTab: 'records',
        afterSaleId: item.bizId
      }
    })
    return
  }
  if (['order', 'order_timeout', 'order_refund'].includes(item.bizType) && item.bizId) {
    await openOrderDetailDialog(item.bizId)
    return
  }
  if (item.bizType === 'profile') {
    router.push('/profile')
    return
  }
  if (item.bizType === 'complaint' && item.bizId) {
    activeTab.value = 'complaint'
    complaintPageNum.value = 1
    complaintStatusFilter.value = 'all'
    await loadComplaints()
    await openComplaintDetail(item.bizId)
  }
}

onMounted(() => {
  const tab = route.query?.tab
  if (tab === 'complaint') {
    activeTab.value = 'complaint'
  } else if (tab === 'unread') {
    activeTab.value = 'unread'
  }
  reload()
})
</script>

<style scoped lang="scss">
.notice-container {
  max-width: 960px;
  margin: 30px auto;
  padding: 0 20px;
  min-height: 80vh;

  /* 让这个页面里的 Element Plus 主色跟着品牌色走 */
  --el-color-primary: rgb(255, 92, 92);
  --el-color-primary-light-3: rgba(255, 92, 92, 0.75);
  --el-color-primary-light-5: rgba(255, 92, 92, 0.45);
  --el-color-primary-light-7: rgba(255, 92, 92, 0.25);
  --el-color-primary-light-9: rgba(255, 92, 92, 0.12);
  --el-color-primary-dark-2: #ff4757;
}

.notice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;

  .title {
    font-size: 26px;
    font-weight: 700;
    color: #1a1a1a;
  }
  .actions {
    display: flex;
    gap: 10px;
    align-items: center;
  }
}

/* 覆盖当前页的 Element Plus 主色 */
:deep(.el-button--primary) {
  --el-button-bg-color: #ff6b81;
  --el-button-border-color: #ff6b81;
  --el-button-hover-bg-color: #ff8da1;
  --el-button-hover-border-color: #ff8da1;
  --el-button-active-bg-color: #ff4757;
  --el-button-active-border-color: #ff4757;
}

:deep(.el-link.el-link--primary) {
  --el-link-text-color: #ff6b81;
  &:hover {
    color: #ff4757;
  }
}

.notice-tabs {
  :deep(.el-tabs__active-bar) {
    background-color: rgb(255, 92, 92);
  }
  :deep(.el-tabs__item.is-active) {
    color: rgb(255, 92, 92);
  }
  :deep(.el-tabs__item:hover) {
    color: rgb(255, 92, 92);
  }
}

.notice-list {
  min-height: 220px;
}

.notice-batch-bar {
  margin-bottom: 12px;
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid #ffd6d9;
  background: #fff5f6;
  display: flex;
  align-items: center;
  justify-content: space-between;

  .selected-text {
    font-size: 13px;
    color: #909399;
  }
}

.notice-item {
  position: relative;
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding: 24px;
  border-radius: 16px;
  border: 1px solid #f0f0f0;
  background: #fff;
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);

  &:hover {
    border-color: #ffcbd2;
    box-shadow: 0 8px 24px rgba(255, 107, 129, 0.12);
    transform: translateY(-2px);
  }

  &.unread {
    border-color: #ffeff2;
    background: #fff9fa;
    
    .ntitle {
       color: #ff6b81;
    }
  }

  .dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #ff6b81;
    margin-top: 6px;
    flex: 0 0 auto;
    box-shadow: 0 2px 6px rgba(255, 107, 129, 0.4);
  }

  .select-col {
    padding-top: 2px;
    flex: 0 0 auto;
  }

  .main {
    flex: 1;
    min-width: 0;
  }

  .item-actions {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 4px;
    flex: 0 0 auto;
  }

  .row1 {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .ntitle {
    font-size: 17px;
    font-weight: 700;
    color: #333;
    margin-bottom: 6px;
    line-height: 1.4;
  }

  .time {
    font-size: 13px;
    color: #999;
    flex: 0 0 auto;
  }

  .content {
    margin-top: 8px;
    font-size: 15px;
    color: #555;
    line-height: 1.6;
    white-space: pre-line;
  }
}

.read-all-btn {
  background-color: #ff6b81;
  border-color: #ff6b81;
  color: #fff;
  
  &:hover {
    background-color: #ff4757;
    border-color: #ff4757;
  }
  
  &.is-disabled {
    background-color: #fab6b6;
    border-color: #fab6b6;
  }
}

.mark-read-btn {
  color: #ff6b81;
  font-weight: 500;
  &:hover {
    color: #ff4757;
  }
}

.delete-btn {
  font-weight: 500;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.complaint-tip {
  margin-bottom: 14px;
  border-radius: 14px;
  background: #fff5f6;
  border: 1px solid #ffd6d9;

  :deep(.el-alert__icon) {
    color: rgb(255, 92, 92);
  }
}

.complaint-filters {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 14px;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: rgb(255, 92, 92);
  border-color: rgb(255, 92, 92);
  box-shadow: -1px 0 0 0 rgb(255, 92, 92);
}
:deep(.el-radio-button__inner:hover) {
  color: rgb(255, 92, 92);
}

:deep(.el-pagination.is-background .el-pager li.is-active) {
  background-color: rgb(255, 92, 92);
}
:deep(.el-pagination.is-background .el-pager li:not(.is-active):hover) {
  color: rgb(255, 92, 92);
}

.complaint-item {
  padding: 18px 20px;
  border-radius: 16px;
  border: 1px solid #f0f0f0;
  background: #fff;
  margin-bottom: 14px;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);

  &:hover {
    border-color: #ffcbd2;
    box-shadow: 0 8px 24px rgba(255, 107, 129, 0.12);
    transform: translateY(-2px);
  }

  .c-row1 {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
  }

  .c-title {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
  }

  .c-note {
    color: #ff6b81;
    font-weight: 600;
    max-width: 420px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .c-body {
    margin-top: 10px;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .c-line {
    color: #555;
    line-height: 1.55;

    .label {
      color: #909399;
    }
  }

  .c-actions {
    margin-top: 12px;
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    align-items: center;
  }
}

.complaint-detail {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-row {
  color: #303133;
  line-height: 1.6;

  .label {
    color: #909399;
    margin-right: 6px;
  }
}

.detail-row.pre {
  white-space: pre-wrap;
  word-break: break-word;
}

.evidence-detail-row {
  display: flex;
  align-items: flex-start;
}

.complaint-evidence-grid {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 8px;
}

.complaint-evidence-image {
  width: 76px;
  height: 76px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  background: #f5f7fa;
  cursor: pointer;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.order-detail-container {
  min-height: 260px;
}

.order-detail-header {
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 18px;

  .status-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }

  .label {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }

  .status-text {
    font-size: 16px;
    font-weight: 700;
    color: #ff5c5c;
  }

  .sn-row {
    color: #909399;
    font-size: 13px;
  }
}

.order-detail-section {
  margin-bottom: 18px;

  h3 {
    margin: 0 0 12px;
    font-size: 18px;
    line-height: 1.2;
    font-weight: 700;
    color: #1f2937;
  }

  .info-row {
    font-size: 14px;
    color: #4b5563;
    line-height: 1.6;
  }
}

.product-list-mini {
  border-top: 1px solid #f2f2f2;
}

.product-item-mini {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid #f2f2f2;

  .p-thumb {
    width: 66px;
    height: 66px;
    border-radius: 10px;
    object-fit: cover;
    flex: 0 0 66px;
    border: 1px solid #f0f0f0;
  }

  .p-info {
    flex: 1;
    min-width: 0;
  }

  .p-name {
    color: #1f2937;
    font-size: 15px;
    line-height: 1.4;
  }

  .p-meta {
    color: #9ca3af;
    margin-top: 4px;
    font-size: 13px;
  }

  .p-total {
    color: #4b5563;
    font-size: 17px;
    font-weight: 700;
  }
}

.order-detail-footer {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  align-items: baseline;
  gap: 10px;

  .label {
    color: #4b5563;
    font-size: 16px;
  }

  .amount {
    color: #ff5c5c;
    font-size: 32px;
    font-weight: 800;
    line-height: 1;
  }
}

.status-pending-receipt {
  color: #ff5c5c !important;
}

.status-completed {
  color: #67c23a !important;
}

.status-closed {
  color: #909399 !important;
}

.status-refunding {
  color: #e6a23c !important;
}

@media (max-width: 768px) {
  .order-detail-section h3 {
    font-size: 16px;
  }

  .product-item-mini .p-name {
    font-size: 14px;
  }

  .product-item-mini .p-total {
    font-size: 15px;
  }

  .order-detail-footer .amount {
    font-size: 24px;
  }
}
</style>
