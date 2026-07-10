<template>
  <div class="order-container">
    <OrderFilterBar />
    <OrderTable />
    <OrderDialogs />
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted, provide } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Download, CopyDocument, Warning, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as orderApi from '@/api/order'
import { resolveImageUrl } from '@/utils/image'
import UserInfoPopover from '@/components/UserInfoPopover.vue'
import { useAdminTable } from '@/composables/useAdminTable'
import OrderFilterBar from './components/OrderFilterBar.vue'
import OrderTable from './components/OrderTable.vue'
import OrderDialogs from './components/OrderDialogs.vue'


const router = useRouter()
const route = useRoute()
const activeTab = ref('all')
const searchKeyword = ref('')
const filterPayType = ref('')
const sortOrder = ref('time_desc')
const dateRange = ref([])
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

onMounted(async () => {
    // 处理从看板跳过来的筛选参数
    if (route.query.startDate && route.query.endDate) {
        dateRange.value = [route.query.startDate, route.query.endDate]
        ElMessage.success(`已自动筛选 ${route.query.startDate} 至 ${route.query.endDate}`)
    }

    await loadOrderList()
})

const loadOrderList = async () => {
    await runWithLoading(async () => {
        const statusMap = { all: undefined, unpaid: 0, pending: 1, shipped: 2, completed: 3, refunding: 5 }
        const params = {
            pageNum: currentPage.value,
            pageSize: pageSize.value,
            status: statusMap[activeTab.value],
            orderNo: searchKeyword.value || undefined,
            startTime: dateRange.value?.[0] ? dateRange.value[0] + 'T00:00:00' : undefined,
            endTime: dateRange.value?.[1] ? dateRange.value[1] + 'T23:59:59' : undefined
        }
        const res = await orderApi.getOrderList(params)
        if (res.code === 200 && res.data) {
            setRows((res.data.records || []).map(mapOrderFromApi), res.data.total)
            filterData()
        } else {
            ElMessage.error(res.message || res.msg || '加载订单列表失败')
        }
    }, () => {
        selectedRows.value = []
    }).catch((e) => {
        console.error('加载订单列表失败', e)
    })
}

const formatDateTime = (value) => {
    if (!value) return ''
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return String(value)
    return date.toLocaleString('zh-CN', { hour12: false })
}

const mapStatusKey = (status) => {
    const map = { 0: 'unpaid', 1: 'pending', 2: 'shipped', 3: 'completed', 4: 'closed', 5: 'refunding' }
    const numericStatus = Number(status)
    if (Number.isNaN(numericStatus)) {
        return 'unpaid'
    }
    return map[numericStatus] || 'unpaid'
}

const resolveOrderStatus = (order) => {
    const rawStatus = mapStatusKey(order?.status)
    const hasShipInfo = Boolean(order?.shipCompany || order?.trackingNo || order?.shipTime)
    const hasPayInfo = Boolean(order?.payTime || order?.payType)

    if ((rawStatus === 'unpaid' || rawStatus === 'pending') && hasShipInfo) {
        return 'shipped'
    }
    if (rawStatus === 'unpaid' && hasPayInfo) {
        return 'pending'
    }
    return rawStatus
}

const mapOrderFromApi = (order) => ({
    id: order.id,
    orderNo: order.orderNo,
    createTime: formatDateTime(order.createTime),
    payType: order.payType,
    paySn: order.paySn,
    tradeNo: order.tradeNo,
    refundAmount: Number(order.refundAmount || 0),
    status: resolveOrderStatus(order),
    amount: Number(order.totalAmount || 0),
    products: order.items?.map(item => ({
        id: item.productId,
        name: item.productName,
        cover: item.productImage,
        price: Number(item.price || 0),
        count: item.quantity
    })) || [],
    user: { id: order.userId, nickname: order.nickname || order.username || '用户', avatar: '' },
    address: {
        name: order.receiver,
        phone: order.phone,
        detail: order.address
    },
    refund: order.refund ? {
        ...order.refund,
        refundAmount: Number(order.refund.refundAmount || 0),
        refundTime: formatDateTime(order.refund.refundTime)
    } : null,
    logistics: order.shipCompany || order.trackingNo ? {
        company: order.shipCompany,
        trackingNo: order.trackingNo,
        shipTime: formatDateTime(order.shipTime)
    } : null,
    notes: { user: order.remark || '' },
    logs: []
})

const filterData = () => {
    let res = allData.value
    
    // 支付方式筛选
    if (filterPayType.value) {
        res = res.filter(item => item.payType === filterPayType.value)
    }

    // 排序
    if (sortOrder.value === 'time_desc') {
        res.sort((a,b) => new Date(b.createTime) - new Date(a.createTime))
    } else if (sortOrder.value === 'time_asc') {
        res.sort((a,b) => new Date(a.createTime) - new Date(b.createTime))
    } else if (sortOrder.value === 'amount_desc') {
        res.sort((a,b) => b.amount - a.amount)
    }

    tableData.value = res
}

const handleTabChange = () => {
    resetPage()
    loadOrderList()
}

const handleFilter = () => {
    resetPage()
    loadOrderList()
}

const resetFilter = () => {
    searchKeyword.value = ''
    filterPayType.value = ''
    sortOrder.value = 'time_desc'
    dateRange.value = []
    resetPage()
    loadOrderList()
    ElMessage.success('已重置筛选条件')
}

const handlePageChange = (page) => {
    currentPage.value = page
    loadOrderList()
}

// 批量操作
const selectedRows = ref([])
const handleSelectionChange = (selection) => {
    selectedRows.value = selection
}

const normalizeId = (value) => {
    const id = Number(value)
    return Number.isInteger(id) && id > 0 ? id : null
}

const canDeleteOrder = (row) => normalizeId(row?.id) != null

const deletableSelectedIds = computed(() => {
    return selectedRows.value
        .filter(item => canDeleteOrder(item))
        .map(item => normalizeId(item?.id))
        .filter(Boolean)
})

const handleBatchExport = () => {
    if (selectedRows.value.length === 0) {
        return ElMessage.warning('请先选择要导出的订单')
    }
    exportOrdersToCsv(selectedRows.value)
    const ids = selectedRows.value.map(item => item.orderNo).join(', ')
    ElMessage.success(`已导出${selectedRows.value.length}个订单：${ids}`)
}

const handleDeleteOrder = (row) => {
    const id = normalizeId(row?.id)
    if (!id) {
        return
    }

    ElMessageBox.confirm('删除后该订单仅在管理端隐藏，确定继续？', '删除订单', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
    }).then(() => {
        orderApi.deleteOrder(id).then((res) => {
            if (res.code !== 200) {
                ElMessage.error(res.message || res.msg || '删除失败')
                return
            }
            ElMessage.success('删除成功')
            loadOrderList()
        }).catch((e) => {
            console.error('删除订单失败', e)
        })
    }).catch(() => {})
}

const handleBatchDeleteOrders = () => {
    const ids = deletableSelectedIds.value
    if (ids.length === 0) {
        ElMessage.warning('请先选择可删除的订单')
        return
    }

    const selectedCount = selectedRows.value.length
    const skippedCount = selectedCount - ids.length
    const tip = skippedCount > 0
        ? `已选 ${selectedCount} 项，其中 ${skippedCount} 项状态不可删，将删除 ${ids.length} 项。确定继续？`
        : `确定批量删除已选的 ${ids.length} 条订单记录吗？`

    ElMessageBox.confirm(tip, '批量删除订单', {
        type: 'warning',
        confirmButtonText: '批量删除',
        cancelButtonText: '取消'
    }).then(() => {
        orderApi.batchDeleteOrders(ids).then((res) => {
            if (res.code !== 200) {
                ElMessage.error(res.message || res.msg || '批量删除失败')
                return
            }
            ElMessage.success('批量删除成功')
            loadOrderList()
        }).catch((e) => {
            console.error('批量删除订单失败', e)
        })
    }).catch(() => {})
}

// CSV 导出辅助函数
const exportOrdersToCsv = (orders) => {
    const headers = ['订单号', '创建时间', '买家昵称', '收货人', '联系电话', '收货地址', '商品信息', '实付金额', '支付方式', '订单状态']
    const rows = orders.map(order => [
        order.orderNo,
        order.createTime,
        order.user.nickname,
        order.address.name,
        order.address.phone,
        `"${order.address.detail.replace(/"/g, '""')}"`, // 转义双引号
        `"${order.products.map(p => `${p.name} x${p.count}`).join('; ').replace(/"/g, '""')}"`,
        order.amount.toFixed(2),
        order.payType || '未支付',
        getStatusText(order.status)
    ])
    
    const csvContent = '\uFEFF' + [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `订单导出_${new Date().toISOString().slice(0,10)}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
}

// 公共辅助函数
const getStatusType = (status) => {
    const map = { 
        unpaid: 'info',
        pending: 'warning', 
        shipped: 'primary', 
        completed: 'success', 
        closed: 'info',
        refunding: 'danger' 
    }
    return map[status] || 'info'
}

const getStatusText = (status) => {
    const map = { 
        unpaid: '待支付',
        pending: '待发货', 
        shipped: '已发货', 
        completed: '已完成', 
        closed: '已关闭', 
        refunding: '退款中' 
    }
    return map[status] || status
}

const copyText = (text) => {
    navigator.clipboard.writeText(text).then(() => {
        ElMessage.success('已复制到剪贴板')
    })
}

const handleExport = () => {
    const statusMap = { all: undefined, unpaid: 0, pending: 1, shipped: 2, completed: 3, refunding: 5 }
    orderApi.exportOrders({
        status: statusMap[activeTab.value],
        startTime: dateRange.value?.[0] ? dateRange.value[0] + 'T00:00:00' : undefined,
        endTime: dateRange.value?.[1] ? dateRange.value[1] + 'T23:59:59' : undefined
    }).then((res) => {
        if (res.code !== 200 || !res.data) {
            ElMessage.error(res.message || res.msg || '导出失败')
            return
        }
        window.open(res.data, '_blank')
        ElMessage.success('导出任务已生成')
    }).catch((e) => {
        console.error('导出失败', e)
    })
}

const getStepActive = (status) => {
    if (status === 'unpaid') return 1
    if (status === 'pending') return 2
    if (status === 'shipped') return 3
    if (status === 'completed') return 4
    if (status === 'closed') return 2
    if (status === 'refunding') return 2
    return 1
}

// 发货弹窗
const shipDialogVisible = ref(false)
const shipForm = reactive({
    id: null,
    company: '',
    trackingNo: '',
    remark: ''
})

const openShipDialog = (row) => {
    shipForm.id = row.id
    shipForm.company = ''
    shipForm.trackingNo = ''
    shipForm.remark = ''
    shipDialogVisible.value = true
}

const confirmShip = () => {
    if (!shipForm.company || !shipForm.trackingNo) {
        return ElMessage.warning('请填写完整的发货信息')
    }
    
    orderApi.shipOrder(shipForm.id, {
        company: shipForm.company,
        trackingNo: shipForm.trackingNo,
        remark: shipForm.remark
    }).then((res) => {
        if (res.code !== 200) {
            ElMessage.error(res.message || res.msg || '发货失败')
            return
        }
        ElMessage.success('发货成功')
        shipDialogVisible.value = false
        loadOrderList()
    }).catch((e) => {
        console.error('发货失败', e)
    })
}


// 收货信息编辑弹窗
const addressDialogVisible = ref(false)
const addressForm = reactive({
    name: '',
    phone: '',
    detail: ''
})

const openAddressEdit = () => {
    if (!currentOrder.value) return
    addressForm.name = currentOrder.value.address.name
    addressForm.phone = currentOrder.value.address.phone
    addressForm.detail = currentOrder.value.address.detail
    addressDialogVisible.value = true
}

const saveAddress = () => {
    if (!addressForm.name || !addressForm.phone || !addressForm.detail) {
        ElMessage.warning('请填写完整收货信息')
        return
    }

    orderApi.updateOrderAddress(currentOrder.value.id, {
        receiver: addressForm.name,
        phone: addressForm.phone,
        address: addressForm.detail
    }).then((res) => {
        if (res.code !== 200) {
            ElMessage.error(res.message || res.msg || '更新失败')
            return
        }
        ElMessage.success('收货信息修改成功')
        addressDialogVisible.value = false
        loadOrderList()
    }).catch((e) => {
        console.error('更新收货信息失败', e)
    })
}

// 退款处理弹窗
const refundDialogVisible = ref(false)
const refundForm = reactive({
    id: null,
    orderNo: '',
    amount: 0,
    reason: '',
    remark: '',
    evidenceImages: []
})

const openRefundDialog = (row) => {
    if (row.refund?.afterSaleId) {
        ElMessage.info('该订单存在售后单，请前往售后工作台处理')
        router.push({ name: 'AfterSale', query: { keyword: row.orderNo } })
        return
    }
    refundForm.id = row.id
    refundForm.orderNo = row.orderNo
    refundForm.amount = row.amount
    refundForm.reason = row.refund?.reason || 'No reason'
    refundForm.remark = ''
    refundForm.evidenceImages = Array.isArray(row.refund?.evidenceImages) ? row.refund.evidenceImages : []
    refundDialogVisible.value = true
}

const handleRefundAction = (success) => {
    // 拒绝退款时必须填写原因
    if (!success && !refundForm.remark.trim()) {
        ElMessage.warning('拒绝退款时必须填写拒绝理由')
        return
    }
    
    orderApi.refundOrder(refundForm.id, {
        success,
        remark: refundForm.remark,
        reason: refundForm.reason
    }).then((res) => {
        if (res.code !== 200) {
            ElMessage.error(res.message || res.msg || '退款处理失败')
            return
        }
        ElMessage.success(success ? '已同意退款' : '已拒绝退款')
        refundDialogVisible.value = false
        loadOrderList()
    }).catch((e) => {
        console.error('退款处理失败', e)
    })
}

// 取消未付款订单
const handleCancelOrder = (row) => {
    ElMessageBox.confirm(
        `Confirm cancel order ${row.orderNo}? Stock will be rolled back.`,
        'Cancel order',
        {
            confirmButtonText: '确认取消',
            cancelButtonText: '再想想',
            type: 'warning'
        }
    ).then(() => {
        orderApi.cancelOrder(row.id).then((res) => {
            if (res.code !== 200) {
                ElMessage.error(res.message || res.msg || '取消失败')
                return
            }
            ElMessage.success('订单已取消')
            loadOrderList()
        }).catch((e) => {
            console.error('取消订单失败', e)
        })
    }).catch(() => {})
}

// 详情弹窗
const detailDialogVisible = ref(false)
const currentOrder = ref(null)
const userOrderStats = ref(null)

const openDetail = async (row) => {
    try {
        const res = await orderApi.getOrderDetail(row.id)
        if (res.code === 200 && res.data) {
            currentOrder.value = mapOrderFromApi(res.data)
            const userOrders = allData.value.filter(o => o.user.id === currentOrder.value.user.id)
            userOrderStats.value = {
                count: userOrders.length,
                total: userOrders.reduce((sum, o) => sum + o.amount, 0).toFixed(2)
            }
            detailDialogVisible.value = true
        } else {
            ElMessage.error(res.message || res.msg || '加载订单详情失败')
        }
    } catch (e) {
        console.error('加载订单详情失败', e)
    }
}

const goToProduct = (productId) => {
    detailDialogVisible.value = false
    ElMessage.success('Jumping to product management...')
    router.push({ path: '/admin/product', query: { productId: productId } })
}

const goToNote = (noteId) => {
    detailDialogVisible.value = false // Close the dialog first
    ElMessage.success('Jumping to note detail...')
    router.push({ path: '/admin/content', query: { noteId: noteId } })
}

provide('adminOrderPageContext', {
    activeTab,
    handleTabChange,
    selectedRows,
    handleBatchExport,
    deletableSelectedIds,
    handleBatchDeleteOrders,
    Download,
    handleExport,
    searchKeyword,
    filterPayType,
    sortOrder,
    dateRange,
    handleFilter,
    resetFilter,
    tableData,
    loading,
    handleSelectionChange,
    copyText,
    CopyDocument,
    resolveImageUrl,
    goToProduct,
    UserInfoPopover,
    Warning,
    getStatusType,
    getStatusText,
    openDetail,
    openShipDialog,
    handleCancelOrder,
    openRefundDialog,
    canDeleteOrder,
    handleDeleteOrder,
    total,
    pageSize,
    currentPage,
    handlePageChange,
    shipDialogVisible,
    shipForm,
    confirmShip,
    detailDialogVisible,
    currentOrder,
    getStepActive,
    openAddressEdit,
    TrendCharts,
    goToNote,
    userOrderStats,
    refundDialogVisible,
    refundForm,
    handleRefundAction,
    addressDialogVisible,
    addressForm,
    saveAddress
})

</script>

<style>
.filter-card :deep(.el-card__body) {
    padding: 15px 20px;
}
.header-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;
}
.header-actions {
    display: flex;
    align-items: center;
    gap: 10px;
}
.selected-hint {
    color: #606266;
    font-size: 13px;
}
.header-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 15px;
    border-top: 1px dashed #ebeef5;
}
.search-group {
    display: flex;
    align-items: center;
    gap: 12px;
}

.order-info {
    display: flex;
    flex-direction: column;
}
.order-no-row {
    display: flex;
    align-items: center;
}
.order-no {
    font-weight: bold;
    font-size: 13px;
    color: var(--admin-professional-primary);
}
.copy-icon {
    margin-left: 5px;
    cursor: pointer;
    font-size: 12px;
    color: #909399;
}
.copy-icon:hover {
    color: var(--admin-professional-primary);
}
.create-time {
    font-size: 12px;
    color: #909399;
    margin: 2px 0;
}
.order-source-tag {
    margin-top: 4px;
}
.pay-sn-line {
    margin-top: 4px;
    font-size: 11px;
    color: #909399;
    word-break: break-all;
}
.more-products {
    font-size: 12px;
    color: #909399;
    padding-left: 48px;
    margin-top: -2px;
}

.product-item {
    display: flex;
    align-items: center;
    margin-bottom: 5px;
}
.product-item:last-child {
    margin-bottom: 0;
}
.product-meta {
    flex: 1;
    overflow: hidden;
}
.product-name {
    font-size: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.product-price {
    font-size: 12px;
    color: #909399;
}

.user-info {
    display: flex;
    align-items: center;
}

.pagination-bar {
    margin-top: 15px;
    display: flex;
    justify-content: flex-end;
}

/* 详情样式 */
.steps-container {
    padding: 20px 0 40px 0;
}
.detail-section-title {
    font-weight: bold;
    margin: 25px 0 15px 0;
    padding-left: 10px;
    border-left: 4px solid var(--admin-professional-primary);
    font-size: 15px;
}
.highlight-price {
    color: #f56c6c;
    font-weight: bold;
    font-size: 20px;
}
.text-minor {
    color: #909399;
}
.user-summary-card {
    background: #f8f9fb;
    padding: 15px;
    border-radius: 8px;
}
.section-small-title {
    font-size: 14px;
    font-weight: bold;
    margin-bottom: 12px;
    color: #303133;
}
.user-brief {
    display: flex;
    align-items: center;
}
.u-info {
    flex: 1;
    margin-left: 12px;
}
.u-name {
    font-weight: bold;
    font-size: 14px;
}
.u-stats {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
}
</style>
