<template>
  <div class="after-sale-container">
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          placeholder="订单号 / 商品 / 用户"
          clearable
          style="width: 220px"
          @input="handleFilter"
        />
        <el-select v-model="filters.status" placeholder="状态" style="width: 170px" clearable @change="handleFilter">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.type" placeholder="类型" style="width: 150px" clearable @change="handleFilter">
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <div class="filter-actions">
          <span v-if="selectedRows.length > 0" class="selected-hint">已选{{ selectedRows.length }}项</span>
          <el-button
            type="danger"
            plain
            :disabled="deletableSelectedIds.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        :data="tableData"
        class="after-sale-table"
        style="width: 100%"
        scrollbar-always-on
        row-key="id"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" />
        <el-table-column prop="id" label="售后单" width="92" />
        <el-table-column label="订单 / 时间" width="210">
          <template #default="{ row }">
            <div class="order-cell">
              <div class="order-sn">{{ row.orderSn || '-' }}</div>
              <div class="minor">{{ row.createTimeText }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="280">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image v-if="row.productImg" :src="resolveImageUrl(row.productImg)" class="product-image" fit="cover" />
              <div v-else class="img-placeholder"><el-icon><Goods /></el-icon></div>
              <div class="product-meta">
                <div class="product-name">{{ row.productName || '-' }}</div>
                <div class="minor">¥{{ formatMoney(row.price) }} x {{ row.quantity || 0 }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="26" :src="resolveImageUrl(row.userAvatar)" />
              <span class="user-name">{{ row.nickname || row.username || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="112" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="150" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="退款金额" width="120" align="right">
          <template #default="{ row }">
            <strong class="money">¥{{ formatMoney(refundAmount(row)) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="物流" min-width="180">
          <template #default="{ row }">
            <div class="minor">{{ logisticsSummary(row) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="138" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
              <el-button v-if="canQuickRefund(row)" link type="success" @click.stop="openAction(row, 'approveRefund')">同意退款</el-button>
              <el-button v-if="canApproveReturn(row)" link type="warning" @click.stop="openAction(row, 'approveReturn')">同意退货</el-button>
              <el-button v-if="canConfirmReturnRefund(row)" link type="success" @click.stop="openAction(row, 'confirmReturnRefund')">收货退款</el-button>
              <el-button v-if="canConfirmReturnExchange(row)" link type="warning" @click.stop="openAction(row, 'confirmReturnExchange')">确认收货</el-button>
              <el-button v-if="canShipExchange(row)" link type="success" @click.stop="openAction(row, 'shipExchange')">换货发货</el-button>
              <el-button v-if="canReject(row)" link type="danger" @click.stop="openAction(row, 'reject')">拒绝</el-button>
              <el-button v-if="canDelete(row)" link type="danger" @click.stop="handleDelete(row)">删除</el-button>
            </div>
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

    <el-dialog v-model="detailDialogVisible" title="售后详情" width="920px" top="3vh" class="after-sale-dialog">
      <div v-if="currentRow" class="detail-box">
        <div class="detail-hero" :class="statusToneClass(currentRow.status)">
          <div>
            <div class="detail-kicker">售后单#{{ currentRow.id }}</div>
            <div class="detail-title-row">
              <span class="detail-title">{{ currentRow.orderSn || '-' }}</span>
              <el-tag :type="statusType(currentRow.status)" size="large">{{ statusText(currentRow.status) }}</el-tag>
            </div>
            <div class="detail-subline">
              <span>{{ typeText(currentRow.type) }}</span>
              <span>{{ reasonText(currentRow) }}</span>
            </div>
          </div>
          <div class="amount-panel">
            <span>售后金额</span>
            <strong>¥{{ formatMoney(refundAmount(currentRow)) }}</strong>
          </div>
        </div>

        <div class="detail-grid">
          <section class="detail-section is-span-2">
            <div class="section-title">申请说明</div>
            <div class="description-panel">{{ descriptionText(currentRow) }}</div>
          </section>

          <section class="detail-section">
            <div class="section-title">商品信息</div>
            <div class="detail-product">
              <el-image v-if="currentRow.productImg" :src="resolveImageUrl(currentRow.productImg)" class="detail-product-image" fit="cover" />
              <div v-else class="detail-product-image is-empty"><el-icon><Goods /></el-icon></div>
              <div>
                <div class="detail-product-name">{{ currentRow.productName || '-' }}</div>
                <div class="minor">单价¥{{ formatMoney(currentRow.price) }} x {{ currentRow.quantity || 0 }}</div>
              </div>
            </div>
          </section>

          <section class="detail-section">
            <div class="section-title">买家信息</div>
            <div class="detail-user">
              <el-avatar :size="42" :src="resolveImageUrl(currentRow.userAvatar)" />
              <div>
                <div class="detail-user-name">{{ currentRow.nickname || currentRow.username || '-' }}</div>
                <div class="minor">用户ID:{{ currentRow.userId || '-' }}</div>
              </div>
            </div>
            <div class="detail-line"><span>订单ID</span><strong>{{ currentRow.orderId || '-' }}</strong></div>
            <div class="detail-line"><span>明细ID</span><strong>{{ currentRow.orderItemId || '-' }}</strong></div>
          </section>

          <section class="detail-section">
            <div class="section-title">退款信息</div>
            <div class="detail-line"><span>退款金额</span><strong>¥{{ formatMoney(refundAmount(currentRow)) }}</strong></div>
            <div class="detail-line"><span>退款流水</span><strong>{{ currentRow.refundSn || '-' }}</strong></div>
            <div class="detail-line"><span>退款状态</span><strong>{{ currentRow.refundStatusDesc || '-' }}</strong></div>
            <div class="detail-line"><span>完成时间</span><strong>{{ formatDateTime(currentRow.refundTime) || '-' }}</strong></div>
            <div v-if="currentRow.refundErrorMsg" class="error-line">{{ currentRow.refundErrorMsg }}</div>
          </section>

          <section class="detail-section">
            <div class="section-title">退换货物流</div>
            <div class="detail-line"><span>退货地址</span><strong>{{ currentRow.returnAddress || '-' }}</strong></div>
            <div class="detail-line"><span>退货物流</span><strong>{{ joinLogistics(currentRow.returnCompany, currentRow.returnTrackingNo) }}</strong></div>
            <div class="detail-line"><span>换货物流</span><strong>{{ joinLogistics(currentRow.exchangeCompany, currentRow.exchangeTrackingNo) }}</strong></div>
          </section>

          <section class="detail-section is-span-2">
            <div class="section-title">凭证图片</div>
            <div v-if="normalizedEvidenceImages(currentRow.evidenceImages).length" class="evidence-list">
              <el-image
                v-for="(img, idx) in normalizedEvidenceImages(currentRow.evidenceImages)"
                :key="`${currentRow.id}-${idx}`"
                :src="resolveImageUrl(img)"
                :preview-src-list="evidencePreviewList(currentRow)"
                class="evidence-image"
                fit="cover"
                preview-teleported
              />
            </div>
            <div v-else class="empty-line">暂无凭证</div>
          </section>

          <section class="detail-section is-span-2">
            <div class="section-title">处理时间线</div>
            <el-timeline v-if="currentLogs.length">
              <el-timeline-item
                v-for="log in currentLogs"
                :key="log.id"
                :timestamp="formatDateTime(log.createTime)"
                placement="top"
              >
                <strong>{{ log.toStatusDesc || statusText(log.toStatus) }}</strong>
                <p>{{ actionText(log.action) }}<span v-if="log.remark">：{{ log.remark }}</span></p>
              </el-timeline-item>
            </el-timeline>
            <div v-else class="empty-line">暂无处理记录</div>
          </section>
        </div>
      </div>
      <el-empty v-else description="暂无数据" />
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="actionDialogVisible" :title="actionMeta.title" width="520px" destroy-on-close>
      <el-form :model="actionForm" label-width="90px">
        <el-form-item label="售后单">{{ actionTarget?.id || '-' }}</el-form-item>
        <el-form-item label="订单号">{{ actionTarget?.orderSn || '-' }}</el-form-item>
        <el-form-item label="退款金额" v-if="actionMeta.showAmount">
          <strong class="money">¥{{ formatMoney(refundAmount(actionTarget)) }}</strong>
        </el-form-item>
        <el-form-item v-if="actionMeta.needReturnAddress" label="退货地址" required>
          <el-input v-model="actionForm.returnAddress" type="textarea" :rows="3" placeholder="填写买家退货地址" />
        </el-form-item>
        <el-form-item v-if="actionMeta.needExchangeLogistics" label="物流公司" required>
          <el-input v-model="actionForm.exchangeCompany" placeholder="例如：顺丰速运" />
        </el-form-item>
        <el-form-item v-if="actionMeta.needExchangeLogistics" label="物流单号" required>
          <el-input v-model="actionForm.exchangeTrackingNo" placeholder="填写换货物流单号" />
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input v-model="actionForm.remark" type="textarea" :rows="3" :placeholder="actionMeta.placeholder" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionDialogVisible = false">取消</el-button>
        <el-button :type="actionMeta.buttonType" :loading="actionSubmitting" @click="submitAction">
          {{ actionMeta.confirmText }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Goods } from '@element-plus/icons-vue'
import {
  getAfterSaleList,
  getAfterSaleDetail,
  approveRefund,
  approveReturn,
  confirmReturnRefund,
  confirmReturnExchange,
  shipExchange,
  rejectAfterSale,
  deleteAfterSale,
  batchDeleteAfterSale
} from '@/api/afterSale'
import { resolveImageUrl } from '@/utils/image'

const TYPE_REFUND_ONLY = 0
const TYPE_RETURN_REFUND = 1
const TYPE_EXCHANGE = 2

const STATUS_PENDING = 0
const STATUS_PROCESSING = 1
const STATUS_COMPLETED = 2
const STATUS_REJECTED = 3
const STATUS_CANCELED = 4
const STATUS_WAIT_BUYER_RETURN = 5
const STATUS_WAIT_MERCHANT_RECEIVE = 6
const STATUS_REFUNDING = 7
const STATUS_EXCHANGE_SHIPPED = 8

const route = useRoute()
const filters = reactive({ keyword: '', status: '', type: '' })
const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedRows = ref([])

const detailDialogVisible = ref(false)
const currentRow = ref(null)
const actionDialogVisible = ref(false)
const actionTarget = ref(null)
const actionKind = ref('')
const actionSubmitting = ref(false)
const actionForm = reactive({
  remark: '',
  returnAddress: '',
  exchangeCompany: '',
  exchangeTrackingNo: ''
})

const statusOptions = [
  { label: '申请中', value: STATUS_PENDING },
  { label: '处理中', value: STATUS_PROCESSING },
  { label: '待买家退货', value: STATUS_WAIT_BUYER_RETURN },
  { label: '待商家收货', value: STATUS_WAIT_MERCHANT_RECEIVE },
  { label: '退款中', value: STATUS_REFUNDING },
  { label: '换货已发货', value: STATUS_EXCHANGE_SHIPPED },
  { label: '已完成', value: STATUS_COMPLETED },
  { label: '已拒绝', value: STATUS_REJECTED },
  { label: '已取消', value: STATUS_CANCELED }
]

const typeOptions = [
  { label: '仅退款', value: TYPE_REFUND_ONLY },
  { label: '退货退款', value: TYPE_RETURN_REFUND },
  { label: '换货', value: TYPE_EXCHANGE }
]

const actionConfig = {
  approveRefund: {
    title: '同意退款',
    confirmText: '同意退款',
    buttonType: 'primary',
    showAmount: true,
    placeholder: '可选：填写退款处理说明',
    handler: approveRefund
  },
  approveReturn: {
    title: '同意退货',
    confirmText: '同意退货',
    buttonType: 'warning',
    needReturnAddress: true,
    placeholder: '可选：提示买家退货注意事项',
    handler: approveReturn
  },
  confirmReturnRefund: {
    title: '确认收货并退款',
    confirmText: '确认退款',
    buttonType: 'primary',
    showAmount: true,
    placeholder: '可选：填写收货与退款备注',
    handler: confirmReturnRefund
  },
  confirmReturnExchange: {
    title: '确认退货已签收',
    confirmText: '确认收货',
    buttonType: 'warning',
    placeholder: '可选：填写收货备注',
    handler: confirmReturnExchange
  },
  shipExchange: {
    title: '换货发货',
    confirmText: '确认发货',
    buttonType: 'primary',
    needExchangeLogistics: true,
    placeholder: '可选：填写换货说明',
    handler: shipExchange
  },
  reject: {
    title: '拒绝售后',
    confirmText: '确认拒绝',
    buttonType: 'danger',
    placeholder: '请填写拒绝理由',
    handler: rejectAfterSale
  }
}

const actionMeta = computed(() => actionConfig[actionKind.value] || {})
const currentLogs = computed(() => Array.isArray(currentRow.value?.logs) ? currentRow.value.logs : [])
const deletableSelectedIds = computed(() => selectedRows.value.filter(canDelete).map((row) => Number(row.id)).filter(Boolean))

const formatDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

const formatMoney = (value) => Number(value || 0).toFixed(2)
const productTotal = (row) => Number(row?.price || 0) * Number(row?.quantity || 0)
const refundAmount = (row) => Number(row?.refundAmount || productTotal(row) || 0)

const typeText = (type) => ({ 0: '仅退款', 1: '退货退款', 2: '换货' }[Number(type)] || '售后')
const statusText = (status) => ({
  0: '申请中',
  1: '处理中',
  2: '已完成',
  3: '已拒绝',
  4: '已取消',
  5: '待买家退货',
  6: '待商家收货',
  7: '退款中',
  8: '换货已发货'
}[Number(status)] || '处理中')

const statusType = (status) => {
  if (status === STATUS_COMPLETED) return 'success'
  if (status === STATUS_REJECTED) return 'danger'
  if (status === STATUS_CANCELED) return 'info'
  if ([STATUS_WAIT_BUYER_RETURN, STATUS_WAIT_MERCHANT_RECEIVE, STATUS_EXCHANGE_SHIPPED].includes(Number(status))) return 'warning'
  if (status === STATUS_REFUNDING) return 'danger'
  return 'info'
}

const statusToneClass = (status) => {
  if (status === STATUS_COMPLETED) return 'is-success'
  if (status === STATUS_REJECTED || status === STATUS_REFUNDING) return 'is-danger'
  if (status === STATUS_CANCELED) return 'is-muted'
  if ([STATUS_WAIT_BUYER_RETURN, STATUS_WAIT_MERCHANT_RECEIVE, STATUS_EXCHANGE_SHIPPED].includes(Number(status))) return 'is-warning'
  return 'is-pending'
}

const reasonText = (row) => row?.reason || '-'
const descriptionText = (row) => row?.description || '-'
const normalizedEvidenceImages = (images) => Array.isArray(images) ? images.filter(Boolean) : []
const evidencePreviewList = (row) => normalizedEvidenceImages(row?.evidenceImages).map(resolveImageUrl)
const joinLogistics = (company, trackingNo) => company || trackingNo ? `${company || '-'} ${trackingNo || ''}`.trim() : '-'
const logisticsSummary = (row) => {
  if (row.exchangeCompany || row.exchangeTrackingNo) return `换货：${joinLogistics(row.exchangeCompany, row.exchangeTrackingNo)}`
  if (row.returnCompany || row.returnTrackingNo) return `退货：${joinLogistics(row.returnCompany, row.returnTrackingNo)}`
  if (row.returnAddress) return '已给退货地址'
  return '-'
}

const actionText = (action) => ({
  apply: '提交申请',
  auto_refund_apply: '自动创建退款申请',
  approve_return: '同意退货',
  return_logistics: '提交退货物流',
  confirm_return_refund: '确认收货并退款',
  confirm_return_exchange: '确认收货',
  ship_exchange: '换货发货',
  refund_start: '开始退款',
  refund_success: '退款成功',
  refund_failed: '退款失败',
  reject: '拒绝售后',
  cancel: '取消售后',
  complete: '确认完成'
}[action] || action || '状态更新')

const canQuickRefund = (row) => Number(row.type) === TYPE_REFUND_ONLY && [STATUS_PENDING, STATUS_PROCESSING].includes(Number(row.status))
const canApproveReturn = (row) => [TYPE_RETURN_REFUND, TYPE_EXCHANGE].includes(Number(row.type)) && [STATUS_PENDING, STATUS_PROCESSING].includes(Number(row.status))
const canConfirmReturnRefund = (row) => Number(row.type) === TYPE_RETURN_REFUND && Number(row.status) === STATUS_WAIT_MERCHANT_RECEIVE
const canConfirmReturnExchange = (row) => Number(row.type) === TYPE_EXCHANGE && Number(row.status) === STATUS_WAIT_MERCHANT_RECEIVE
const canShipExchange = (row) => Number(row.type) === TYPE_EXCHANGE && Number(row.status) === STATUS_PROCESSING && row.returnReceiveTime
const canReject = (row) => ![STATUS_COMPLETED, STATUS_REJECTED, STATUS_CANCELED, STATUS_REFUNDING].includes(Number(row.status))
const canDelete = (row) => [STATUS_COMPLETED, STATUS_REJECTED, STATUS_CANCELED].includes(Number(row.status))

const loadList = async () => {
  loading.value = true
  try {
    const res = await getAfterSaleList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      status: filters.status === '' ? undefined : filters.status,
      type: filters.type === '' ? undefined : filters.type,
      keyword: filters.keyword ? filters.keyword.trim() : undefined
    })
    if (res.code === 200 && res.data) {
      tableData.value = (res.data.records || []).map(normalizeRow)
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.message || res.msg || '加载失败')
    }
  } finally {
    selectedRows.value = []
    loading.value = false
  }
}

const normalizeRow = (item) => ({
  ...item,
  createTimeText: formatDateTime(item.createTime),
  handleTimeText: formatDateTime(item.handleTime)
})

const handleFilter = () => {
  currentPage.value = 1
  loadList()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadList()
}

const openDetail = async (row) => {
  const res = await getAfterSaleDetail(row.id)
  if (res.code !== 200 || !res.data) {
    ElMessage.error(res.message || res.msg || '加载详情失败')
    return
  }
  currentRow.value = normalizeRow(res.data)
  detailDialogVisible.value = true
}

const openAction = (row, kind) => {
  actionTarget.value = row
  actionKind.value = kind
  actionForm.remark = ''
  actionForm.returnAddress = row.returnAddress || ''
  actionForm.exchangeCompany = ''
  actionForm.exchangeTrackingNo = ''
  actionDialogVisible.value = true
}

const submitAction = async () => {
  const target = actionTarget.value
  const meta = actionMeta.value
  if (!target || !meta.handler) return
  if (meta.needReturnAddress && !actionForm.returnAddress.trim()) {
    ElMessage.warning('请填写退货地址')
    return
  }
  if (meta.needExchangeLogistics && (!actionForm.exchangeCompany.trim() || !actionForm.exchangeTrackingNo.trim())) {
    ElMessage.warning('请填写换货物流公司和单号')
    return
  }
  if (actionKind.value === 'reject' && !actionForm.remark.trim()) {
    ElMessage.warning('请填写拒绝理由')
    return
  }
  actionSubmitting.value = true
  try {
    const payload = {
      remark: actionForm.remark.trim() || undefined,
      returnAddress: actionForm.returnAddress.trim() || undefined,
      exchangeCompany: actionForm.exchangeCompany.trim() || undefined,
      exchangeTrackingNo: actionForm.exchangeTrackingNo.trim() || undefined
    }
    const res = await meta.handler(target.id, payload)
    if (res.code !== 200) {
      ElMessage.error(res.message || res.msg || '操作失败')
      return
    }
    ElMessage.success('操作成功')
    actionDialogVisible.value = false
    await loadList()
    if (detailDialogVisible.value && currentRow.value?.id === target.id) {
      await openDetail({ id: target.id })
    }
  } finally {
    actionSubmitting.value = false
  }
}

const handleSelectionChange = (rows) => {
  selectedRows.value = Array.isArray(rows) ? rows : []
}

const handleDelete = async (row) => {
  if (!canDelete(row)) {
    ElMessage.warning('仅已完成、已拒绝或已取消的售后记录可删除')
    return
  }
  try {
    await ElMessageBox.confirm('删除后记录仅在管理端隐藏，确定继续？', '删除售后记录', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    const res = await deleteAfterSale(row.id)
    if (res.code !== 200) {
      ElMessage.error(res.message || res.msg || '删除失败')
      return
    }
    ElMessage.success('删除成功')
    loadList()
  } catch {}
}

const handleBatchDelete = async () => {
  const ids = deletableSelectedIds.value
  if (!ids.length) {
    ElMessage.warning('请先选择可删除的售后记录')
    return
  }
  try {
    await ElMessageBox.confirm(`确定批量删除${ids.length}项售后记录吗？`, '批量删除售后记录', {
      type: 'warning',
      confirmButtonText: '批量删除',
      cancelButtonText: '取消'
    })
    const res = await batchDeleteAfterSale(ids)
    if (res.code !== 200) {
      ElMessage.error(res.message || res.msg || '批量删除失败')
      return
    }
    ElMessage.success('批量删除成功')
    loadList()
  } catch {}
}

const applyRouteFilters = () => {
  filters.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  filters.status = route.query.status !== undefined ? Number(route.query.status) : ''
  filters.type = route.query.type !== undefined ? Number(route.query.type) : ''
}

onMounted(() => {
  applyRouteFilters()
  loadList()
})
</script>

<style scoped>
.after-sale-container {
  --as-text: #1f2937;
  --as-muted: #6b7280;
  --as-border: #e5e7eb;
  --as-soft: #f8fafc;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-card :deep(.el-card__body),
.table-card :deep(.el-card__body) {
  padding: 18px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.selected-hint,
.minor {
  color: var(--as-muted);
  font-size: 13px;
}

.after-sale-table :deep(.el-table__header th) {
  color: #4b5563;
  font-size: 14px;
  font-weight: 700;
  background: #f9fafb;
}

.pagination-bar {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

.order-sn,
.product-name,
.detail-product-name,
.detail-user-name {
  color: var(--as-text);
  font-weight: 700;
}

.product-cell,
.user-cell,
.detail-product,
.detail-user {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.product-image,
.img-placeholder {
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  border-radius: 8px;
}

.img-placeholder,
.detail-product-image.is-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  border: 1px dashed var(--as-border);
  background: #f9fafb;
}

.product-name {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-name {
  max-width: 95px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.money {
  color: #b45309;
}

.row-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
}

.row-actions :deep(.el-button) {
  margin-left: 0;
  padding: 0;
}

.after-sale-dialog :deep(.el-dialog) {
  max-width: calc(100vw - 48px);
  max-height: 94vh;
  display: flex;
  flex-direction: column;
}

.after-sale-dialog :deep(.el-dialog__body) {
  flex: 1;
  min-height: 0;
  max-height: calc(94vh - 128px);
  overflow-y: auto;
  padding: 12px 22px 14px;
}

.detail-box {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 15px 16px;
  border: 1px solid var(--as-border);
  border-radius: 8px;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 70%);
}

.detail-hero.is-pending { border-left: 4px solid #64748b; }
.detail-hero.is-warning { border-left: 4px solid #f59e0b; }
.detail-hero.is-success { border-left: 4px solid #10b981; }
.detail-hero.is-danger { border-left: 4px solid #ef4444; }
.detail-hero.is-muted { border-left: 4px solid #94a3b8; }

.detail-kicker {
  margin-bottom: 6px;
  color: var(--as-muted);
  font-size: 13px;
}

.detail-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-title {
  color: var(--as-text);
  font-size: 20px;
  font-weight: 800;
}

.detail-subline {
  display: flex;
  gap: 10px;
  margin-top: 8px;
  color: #4b5563;
}

.detail-subline span {
  padding: 3px 8px;
  border-radius: 999px;
  background: #f1f5f9;
}

.amount-panel {
  min-width: 145px;
  padding: 11px 12px;
  border-radius: 8px;
  border: 1px solid var(--as-border);
  background: #ffffff;
}

.amount-panel span {
  display: block;
  color: var(--as-muted);
  font-size: 12px;
}

.amount-panel strong {
  display: block;
  margin-top: 5px;
  color: #b45309;
  font-size: 20px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
}

.detail-section {
  padding: 13px 14px;
  border: 1px solid var(--as-border);
  border-radius: 8px;
  background: #ffffff;
}

.detail-section.is-span-2 {
  grid-column: 1 / -1;
}

.section-title {
  margin-bottom: 10px;
  color: #111827;
  font-size: 15px;
  font-weight: 800;
}

.description-panel,
.empty-line {
  padding: 11px 12px;
  border-radius: 8px;
  background: var(--as-soft);
  color: var(--as-text);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-product-image {
  width: 64px;
  height: 64px;
  flex: 0 0 64px;
  border-radius: 8px;
  border: 1px solid var(--as-border);
}

.detail-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-top: 1px solid #f1f5f9;
  color: var(--as-muted);
  font-size: 13px;
}

.detail-line strong {
  color: var(--as-text);
  text-align: right;
  overflow-wrap: anywhere;
}

.error-line {
  margin-top: 8px;
  color: #b91c1c;
  font-size: 13px;
}

.evidence-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.evidence-image {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  border: 1px solid var(--as-border);
  overflow: hidden;
}

@media (max-width: 920px) {
  .detail-hero,
  .detail-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
