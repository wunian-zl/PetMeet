<template>
  <div class="after-sale-container">
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="订单号 / 商品 / 用户"
          clearable
          style="width: 220px"
          @input="handleFilter"
        />
        <el-select v-model="statusFilter" placeholder="状态" style="width: 160px" clearable @change="handleFilter">
          <el-option label="申请中" :value="0" />
          <el-option label="处理中" :value="1" />
          <el-option label="已完成" :value="2" />
          <el-option label="已拒绝" :value="3" />
          <el-option label="已取消" :value="4" />
        </el-select>
        <div class="filter-actions">
          <span v-if="selectedRows.length > 0" class="selected-hint">已选 {{ selectedRows.length }} 项</span>
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
        style="width: 100%"
        row-key="id"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" />
        <el-table-column type="expand" width="44">
          <template #default="{ row }">
            <div class="expand-panel">
              <el-descriptions border size="small" :column="2">
                <el-descriptions-item label="订单号">{{ row.orderSn || '-' }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="类型">{{ typeText(row.type) }}</el-descriptions-item>
                <el-descriptions-item label="原因">{{ reasonText(row) }}</el-descriptions-item>
                <el-descriptions-item label="问题描述" :span="2">
                  <span class="expand-pre">{{ descriptionText(row) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="退款凭证" :span="2">
                  <div v-if="normalizedEvidenceImages(row.evidenceImages).length" class="evidence-list">
                    <el-image
                      v-for="(img, idx) in normalizedEvidenceImages(row.evidenceImages)"
                      :key="`${row.id}-${idx}`"
                      :src="resolveImageUrl(img)"
                      class="evidence-image"
                      fit="cover"
                    />
                  </div>
                  <span v-else>-</span>
                </el-descriptions-item>
                <el-descriptions-item label="处理备注" :span="2">
                  <span class="expand-pre">{{ remarkText(row) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="提交时间">{{ row.createTime || '-' }}</el-descriptions-item>
                <el-descriptions-item label="处理时间">{{ row.handleTime || '-' }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="订单" width="180">
          <template #default="{ row }">
            <div class="order-cell">
              <div class="order-sn">{{ row.orderSn || '-' }}</div>
              <div class="order-time">{{ row.createTime || '-' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="220">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image
                v-if="row.productImg"
                :src="resolveImageUrl(row.productImg)"
                style="width: 40px; height: 40px; border-radius: 6px"
              />
              <div v-else class="img-placeholder"><el-icon><Goods /></el-icon></div>
              <div class="product-meta">
                <div class="product-name">{{ row.productName || '-' }}</div>
                <div class="product-qty">¥{{ Number(row.price || 0).toFixed(2) }} x {{ row.quantity || 0 }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="24" :src="resolveImageUrl(row.userAvatar)" />
              <span class="user-name">{{ row.nickname || row.username || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small" effect="plain">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handleTime" label="处理时间" width="170" />
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button link size="small" @click.stop="openDetail(row)">详情</el-button>
            <template v-if="row.status === 0">
              <el-button type="warning" link size="small" @click.stop="updateStatus(row, 1)">处理中</el-button>
              <el-button type="danger" link size="small" @click.stop="updateStatus(row, 3)">拒绝</el-button>
            </template>
            <template v-else-if="row.status === 1">
              <el-button type="success" link size="small" @click.stop="updateStatus(row, 2)">完成</el-button>
              <el-button type="danger" link size="small" @click.stop="updateStatus(row, 3)">拒绝</el-button>
            </template>
            <el-button
              v-if="canDelete(row)"
              type="danger"
              link
              size="small"
              @click.stop="handleDelete(row)"
            >
              删除
            </el-button>
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

    <el-dialog v-model="detailDialogVisible" title="售后详情" width="720px" top="6vh">
      <div v-if="currentRow" class="detail-box">
        <el-descriptions border size="small" :column="2">
          <el-descriptions-item label="订单号">{{ currentRow.orderSn || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(currentRow.status)" size="small">{{ statusText(currentRow.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="类型">{{ typeText(currentRow.type) }}</el-descriptions-item>
          <el-descriptions-item label="原因">{{ reasonText(currentRow) }}</el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2">
            <div class="expand-pre">{{ descriptionText(currentRow) }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="退款凭证" :span="2">
            <div
              v-if="normalizedEvidenceImages(currentRow.evidenceImages).length"
              class="evidence-list"
            >
              <el-image
                v-for="(img, idx) in normalizedEvidenceImages(currentRow.evidenceImages)"
                :key="`${currentRow.id}-${idx}`"
                :src="resolveImageUrl(img)"
                class="evidence-image"
                fit="cover"
              />
            </div>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="处理备注" :span="2">
            <div class="expand-pre">{{ remarkText(currentRow) }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ currentRow.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ currentRow.handleTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <el-empty v-else description="暂无数据" :image-size="90" />
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Goods } from '@element-plus/icons-vue'
import { getAfterSaleList, updateAfterSaleStatus, deleteAfterSale, batchDeleteAfterSale } from '@/api/afterSale'
import { resolveImageUrl } from '@/utils/image'

const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedRows = ref([])

const detailDialogVisible = ref(false)
const currentRow = ref(null)

const PLACEHOLDER_RE = /^[?？\s]+$/
const KNOWN_TEXT_MAP = {
  'cancel paid order before shipment': '已支付订单发货前取消',
  'system auto-created refund-only request from cancel action': '系统自动创建仅退款申请（由取消订单触发）',
  'rejected by admin': '管理员已拒绝',
  'completed by user': '用户已确认完成',
  'canceled by user': '用户已取消售后申请'
}

const formatDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

const normalizeText = (value) => {
  const raw = String(value ?? '').trim()
  if (!raw || PLACEHOLDER_RE.test(raw)) return ''
  const key = raw.toLowerCase().replace(/\s+/g, ' ')
  return KNOWN_TEXT_MAP[key] || raw
}

const statusText = (status) => {
  const map = { 0: '申请中', 1: '处理中', 2: '已完成', 3: '已拒绝', 4: '已取消' }
  return map[status] || '处理中'
}

const statusType = (status) => {
  if (status === 2) return 'success'
  if (status === 3) return 'danger'
  if (status === 4) return 'info'
  if (status === 1) return 'warning'
  return 'info'
}

const typeText = (type) => {
  const map = { 0: '仅退款', 1: '退货退款', 2: '换货' }
  return map[type] || '售后'
}

const reasonText = (row) => {
  const text = normalizeText(row?.reason)
  if (text) return text
  if (Number(row?.type) === 0) return '已支付订单发货前取消'
  return '-'
}

const descriptionText = (row) => {
  const text = normalizeText(row?.description)
  if (text) return text
  if (Number(row?.type) === 0) return '系统自动创建仅退款申请（由取消订单触发）'
  return '-'
}

const remarkFallbackByStatus = {
  1: '售后处理中',
  2: '售后已完成',
  3: '售后申请已拒绝',
  4: '用户已取消售后申请'
}

const remarkText = (row) => {
  const text = normalizeText(row?.handleRemark)
  if (text) return text
  if (!row?.handleTime) return '-'
  return remarkFallbackByStatus[Number(row?.status)] || '系统已处理'
}

const normalizedEvidenceImages = (images) => {
  if (!Array.isArray(images)) return []
  return images.filter((img) => {
    const raw = String(img ?? '').trim()
    return raw && raw !== '-' && !PLACEHOLDER_RE.test(raw)
  })
}

const canDelete = (row) => [2, 3, 4].includes(Number(row?.status))

const normalizeId = (value) => {
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : null
}

const deletableSelectedIds = computed(() => {
  return selectedRows.value
    .filter((row) => canDelete(row))
    .map((row) => normalizeId(row?.id))
    .filter(Boolean)
})

const loadList = async () => {
  loading.value = true
  try {
    const res = await getAfterSaleList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      status: statusFilter.value === '' ? undefined : statusFilter.value,
      keyword: keyword.value ? keyword.value.trim() : undefined
    })
    if (res.code === 200 && res.data) {
      const records = res.data.records || []
      tableData.value = records.map((item) => ({
        ...item,
        createTime: formatDateTime(item.createTime),
        handleTime: formatDateTime(item.handleTime)
      }))
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.message || res.msg || '加载失败')
    }
  } finally {
    selectedRows.value = []
    loading.value = false
  }
}

const handleFilter = () => {
  currentPage.value = 1
  loadList()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadList()
}

const openDetail = (row) => {
  currentRow.value = row
  detailDialogVisible.value = true
}

const handleSelectionChange = (rows) => {
  selectedRows.value = Array.isArray(rows) ? rows : []
}

const handleDelete = async (row) => {
  const id = normalizeId(row?.id)
  if (!id) return
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
    const res = await deleteAfterSale(id)
    if (res.code !== 200) {
      ElMessage.error(res.message || res.msg || '删除失败')
      return
    }
    ElMessage.success('删除成功')
    loadList()
  } catch (e) {}
}

const handleBatchDelete = async () => {
  const ids = deletableSelectedIds.value
  if (ids.length === 0) {
    ElMessage.warning('请先选择可删除的售后记录')
    return
  }

  const selectedCount = selectedRows.value.length
  const skippedCount = selectedCount - ids.length
  const tip = skippedCount > 0
    ? `已选 ${selectedCount} 项，其中 ${skippedCount} 项状态不可删，将删除 ${ids.length} 项。确定继续？`
    : `确定批量删除已选的 ${ids.length} 项售后记录吗？`

  try {
    await ElMessageBox.confirm(tip, '批量删除售后记录', {
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
  } catch (e) {}
}

const updateStatus = async (row, status) => {
  const actionText = status === 1 ? '处理中' : status === 2 ? '完成' : '拒绝'
  try {
    const { value } = await ElMessageBox.prompt(
      '可选：填写处理备注，将展示给用户。',
      `确认${actionText}`,
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: status === 3 ? '例如：不符合售后条件' : '例如：已安排处理'
      }
    )
    const res = await updateAfterSaleStatus(row.id, status, value || undefined)
    if (res.code !== 200) {
      ElMessage.error(res.message || res.msg || '更新失败')
      return
    }
    ElMessage.success('操作成功')
    loadList()
  } catch (e) {}
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
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
.selected-hint {
  color: #606266;
  font-size: 13px;
}
.pagination-bar {
  margin-top: 15px;
  display: flex;
  justify-content: flex-end;
}
.order-cell {
  display: flex;
  flex-direction: column;
}
.order-sn {
  font-weight: 600;
  font-size: 13px;
}
.order-time {
  font-size: 12px;
  color: #909399;
}
.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.img-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  border: 1px dashed #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}
.product-meta {
  min-width: 0;
}
.product-name {
  font-size: 13px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
}
.product-qty {
  font-size: 12px;
  color: #909399;
}
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.user-name {
  font-size: 13px;
  color: #303133;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.expand-panel {
  padding: 12px 16px;
}
.expand-pre {
  white-space: pre-wrap;
  word-break: break-word;
}

.evidence-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.evidence-image {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}
</style>
