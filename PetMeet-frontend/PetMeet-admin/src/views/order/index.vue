<template>
  <div class="order-container">
     <el-card shadow="never" class="filter-card">
        <div class="header-top">
            <el-radio-group v-model="activeTab" @change="handleTabChange">
                <el-radio-button label="all">全部</el-radio-button>
                <el-radio-button label="unpaid">待支付</el-radio-button>
                <el-radio-button label="pending">待发货</el-radio-button>
                <el-radio-button label="shipped">已发货</el-radio-button>
                <el-radio-button label="completed">已完成</el-radio-button>
                <el-radio-button label="refunding">退款中</el-radio-button>
            </el-radio-group>
            <div class="header-actions">
                <span v-if="selectedRows.length > 0" class="selected-hint">已选 {{ selectedRows.length }} 项</span>
                <el-button v-if="selectedRows.length > 0" type="primary" plain @click="handleBatchExport">导出选中 ({{ selectedRows.length }})</el-button>
                <el-button
                    type="danger"
                    plain
                    :disabled="deletableSelectedIds.length === 0"
                    @click="handleBatchDeleteOrders"
                >
                    批量删除
                </el-button>
                <el-button type="success" :icon="Download" @click="handleExport">导出全部</el-button>
            </div>
        </div>
        <div class="header-bottom">
            <div class="search-group">
                <el-input 
                    v-model="searchKeyword" 
                    placeholder="订单号 / 用户名 / 手机号"
                    style="width: 220px" 
                    clearable
                    prefix-icon="Search"
                    @input="handleFilter"
                />
                <el-select v-model="filterPayType" placeholder="支付方式" clearable style="width: 120px" @change="handleFilter">
                    <el-option label="微信支付" value="微信支付" />
                    <el-option label="支付宝" value="支付宝" />

                </el-select>
                <el-select v-model="sortOrder" placeholder="排序方式" style="width: 130px" @change="handleFilter">
                    <el-option label="时间倒序" value="time_desc" />
                    <el-option label="金额倒序" value="amount_desc" />
                    <el-option label="时间正序" value="time_asc" />
                </el-select>
                 <el-date-picker
                    v-model="dateRange"
                    type="daterange"
                    range-separator="-"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    value-format="YYYY-MM-DD"
                    style="width: 240px"
                    @change="handleFilter"
                 />
            </div>
            <el-button type="primary" link icon="Refresh" @click="resetFilter">重置筛选</el-button>
        </div>
    </el-card>

    <el-card shadow="never" class="table-card">
        <el-table :data="tableData" style="width: 100%" v-loading="loading" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="45" />
            <el-table-column label="订单信息" width="180">
                <template #default="{ row }">
                   <div class="order-info">
                       <div class="order-no-row">
                           <span class="order-no">{{ row.orderNo }}</span>
                           <el-icon class="copy-icon" @click="copyText(row.orderNo)"><CopyDocument /></el-icon>
                       </div>
                       <div class="create-time">{{ row.createTime }}</div>
                       <div class="order-source-tag">
                           <el-tag size="small" type="primary" effect="plain">{{ row.payType || '未支付' }}</el-tag>
                       </div>
                       <div v-if="row.paySn" class="pay-sn-line">{{ row.paySn }}</div>
                   </div>
                </template>
            </el-table-column>
            
            <el-table-column label="商品信息" min-width="220">
                <template #default="{ row }">
                    <div class="product-list-wrapper">
                        <div v-for="(prod, idx) in row.products.slice(0, 1)" :key="idx" class="product-item">
                            <el-image :src="resolveImageUrl(prod.cover)" style="width: 40px; height: 40px; border-radius: 4px; margin-right: 8px; cursor: pointer" @click="goToProduct(prod.id)" />
                            <div class="product-meta">
                                <div class="product-name" style="font-size: 14px; cursor: pointer; color: var(--admin-professional-primary);" @click="goToProduct(prod.id)">{{ prod.name }}</div>
                                <div class="product-price" style="font-size: 13px;">¥{{ prod.price }} x {{ prod.count }}</div>
                            </div>
                        </div>
                        <el-popover v-if="row.products.length > 1" placement="bottom-start" :width="280" trigger="hover" :show-after="200">
                            <template #reference>
                                <div class="more-products" style="cursor: pointer; color: var(--admin-professional-primary); font-size: 14px; margin-top: 4px; display: inline-block;">
                                    共{{ row.products.length }} 个商品...
                                </div>
                            </template>
                            <div class="popover-product-list">
                                <div v-for="(prod, idx) in row.products" :key="idx" style="display: flex; align-items: center; padding: 6px 0; border-bottom: 1px solid #eee;">
                                    <el-image :src="resolveImageUrl(prod.cover)" style="width: 36px; height: 36px; border-radius: 4px; margin-right: 8px; flex-shrink: 0" />
                                    <div style="flex: 1; overflow: hidden;">
                                        <div style="font-size: 14px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">{{ prod.name }}</div>
                                        <div style="font-size: 13px; color: #F56C6C;">¥{{ prod.price }} x {{ prod.count }}</div>
                                    </div>
                                </div>
                            </div>
                        </el-popover>
                    </div>
                </template>
            </el-table-column>

            <el-table-column label="买家信息" width="160">
                 <template #default="{ row }">
                     <UserInfoPopover v-if="row.user?.id" :user-id="row.user.id" placement="right" :width="340">
                        <template #reference>
                            <div class="user-info" style="cursor: pointer">
                                <el-avatar :size="24" :src="resolveImageUrl(row.user.avatar)" />
                                <span style="margin-left: 8px">{{ row.user.nickname }}</span>
                                <el-tooltip v-if="row.user.risk" content="风险用户：曾有频繁退款行为" placement="top">
                                    <el-icon color="#F56C6C" style="margin-left: 4px"><Warning /></el-icon>
                                </el-tooltip>
                            </div>
                        </template>
                     </UserInfoPopover>
                     <div v-else class="user-info">
                        <el-avatar :size="24" :src="resolveImageUrl(row.user.avatar)" />
                         <span style="margin-left: 8px">{{ row.user.nickname }}</span>
                         <el-tooltip v-if="row.user.risk" content="风险用户：曾有频繁退款行为" placement="top">
                             <el-icon color="#F56C6C" style="margin-left: 4px"><Warning /></el-icon>
                         </el-tooltip>
                     </div>
                 </template>
            </el-table-column>

            <el-table-column label="实付金额" width="120" align="right">
                <template #default="{ row }">
                    <span style="color: #f56c6c; font-weight: bold">¥{{ row.amount.toFixed(2) }}</span>
                </template>
            </el-table-column>

            <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                     <div style="display: flex; flex-direction: column; gap: 4px; align-items: center;">
                        <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
                        <el-tag
                          v-if="row.status === 'refunding'"
                          type="danger"
                          effect="dark"
                          size="small"
                        >
                          暂停发货
                        </el-tag>
                     </div>
                </template>
            </el-table-column>

            <el-table-column label="操作" width="260" fixed="right">
                 <template #default="{ row }">
                     <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                     <el-button 
                        v-if="row.status === 'pending'" 
                        link 
                        type="success" 
                        @click="openShipDialog(row)"
                     >
                        发货
                     </el-button>
                     <el-button 
                        v-if="row.status === 'unpaid'" 
                        link 
                        type="warning" 
                        @click="handleCancelOrder(row)"
                     >
                        取消订单
                     </el-button>
                     <el-button 
                        v-if="row.status === 'refunding'" 
                        link 
                        type="danger" 
                        @click="openRefundDialog(row)"
                     >
                        退款处理
                     </el-button>
                     <el-button
                        v-if="canDeleteOrder(row)"
                        link
                        type="danger"
                        @click="handleDeleteOrder(row)"
                     >
                        删除
                     </el-button>
                 </template>
             </el-table-column>
            <template #empty>
                <el-empty description="没有符合条件的订单记录" :image-size="100" />
            </template>
        </el-table>
        
        <div class="pagination-bar">
             <el-pagination 
                background 
                layout="prev, pager, next" 
                :total="total" 
                :page-size="pageSize"
                v-model:current-page="currentPage"
                @current-change="handlePageChange"
             />
        </div>
    </el-card>

  <!-- 发货弹窗 -->
    <el-dialog v-model="shipDialogVisible" title="订单发货" width="400px">
        <el-form :model="shipForm" label-width="80px">
            <el-form-item label="物流公司">
                 <el-select v-model="shipForm.company" placeholder="请选择物流公司" style="width: 100%">
                     <el-option label="顺丰速运" value="顺丰速运" />
                     <el-option label="中通快递" value="中通快递" />
                     <el-option label="圆通速递" value="圆通速递" />
                     <el-option label="EMS" value="EMS" />
                 </el-select>
            </el-form-item>
            <el-form-item label="物流单号">
                <el-input v-model="shipForm.trackingNo" placeholder="请输入物流单号" />
            </el-form-item>
            <el-form-item label="发货备注">
                <el-input v-model="shipForm.remark" type="textarea" :rows="2" placeholder="可选：补充说明，方便买家查看" />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="shipDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="confirmShip">确认发货</el-button>
        </template>
    </el-dialog>

  <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="订单详情" width="750px" top="5vh">
        <div v-if="currentOrder" class="detail-content">
      <!-- 状态步骤 -->
             <div class="steps-container">
                <el-steps :active="getStepActive(currentOrder.status)" finish-status="success" align-center>
                    <el-step title="下单" :description="currentOrder.createTime" />
                    <el-step title="支付" :description="currentOrder.status === 'unpaid' ? '等待支付' : '已支付'" />
                    <el-step title="发货" :description="currentOrder.logistics?.shipTime || '等待发货'" />
                    <el-step title="完成" :description="currentOrder.status === 'completed' ? '订单已完成' : ''" />
                </el-steps>
             </div>

             <el-row :gutter="20">
                <el-col :span="12">
                    <el-descriptions :column="1" border>
                         <template #title>
                             <div style="display: flex; justify-content: space-between; align-items: center;">
                                 <span>收货信息</span>
                                 <el-button 
                                     v-if="['unpaid', 'pending'].includes(currentOrder.status)" 
                                     type="primary" 
                                     link 
                                     size="small" 
                                     @click="openAddressEdit"
                                 >
                                     修改
                                 </el-button>
                              </div>
                          </template>
                         <el-descriptions-item label="收货人">{{ currentOrder.address.name }}</el-descriptions-item>
                         <el-descriptions-item label="联系电话">{{ currentOrder.address.phone }}</el-descriptions-item>
                         <el-descriptions-item label="收货地址">{{ currentOrder.address.detail }}</el-descriptions-item>
                         <el-descriptions-item label="用户备注">
                            <span :class="{'text-minor': !currentOrder.notes?.user}">{{ currentOrder.notes?.user || '-' }}</span>
                         </el-descriptions-item>
                    </el-descriptions>
                </el-col>
                <el-col :span="12">
                    <el-descriptions title="付款信息" :column="1" border>
                         <el-descriptions-item label="支付方式">{{ currentOrder.payType || '未支付' }}</el-descriptions-item>
                         <el-descriptions-item label="支付流水">{{ currentOrder.paySn || '-' }}</el-descriptions-item>
                         <el-descriptions-item label="第三方交易号">{{ currentOrder.tradeNo || '-' }}</el-descriptions-item>
                         <el-descriptions-item label="商品总额">¥{{ (currentOrder.amount + (currentOrder.discount?.coupon || 0)).toFixed(2) }}</el-descriptions-item>
                         <el-descriptions-item label="优惠金额">- ¥{{ (currentOrder.discount?.coupon || 0).toFixed(2) }}</el-descriptions-item>
                         <el-descriptions-item label="实付金额">
                            <span class="highlight-price">¥{{ currentOrder.amount.toFixed(2) }}</span>
                         </el-descriptions-item>
                         <el-descriptions-item label="已退金额">¥{{ currentOrder.refundAmount.toFixed(2) }}</el-descriptions-item>
                         <el-descriptions-item v-if="currentOrder.refund?.refundSn" label="退款流水">
                            {{ currentOrder.refund.refundSn }}
                         </el-descriptions-item>
                         <el-descriptions-item v-if="currentOrder.refund?.refundStatusDesc" label="退款状态">
                            {{ currentOrder.refund.refundStatusDesc }}
                         </el-descriptions-item>
                    </el-descriptions>
                </el-col>
             </el-row>
             
      <!-- 来源笔记（内容电商链路） -->
             <div v-if="currentOrder.sourceNote" class="source-note-section" style="margin-top: 15px; padding: 12px; background: linear-gradient(135deg, #ecf1fd 0%, #f0f9eb 100%); border-radius: 8px; border: 1px dashed var(--admin-professional-primary);">
                 <div style="display: flex; align-items: center; gap: 8px;">
                     <el-icon color="var(--admin-professional-primary)"><TrendCharts /></el-icon>
                     <span style="font-weight: 600; color: #303133;">来源笔记（内容种草）</span>
                 </div>
                 <div style="margin-top: 8px; font-size: 14px; color: #606266;">
                     用户通过阅读笔记《{{ currentOrder.sourceNote.title }}》后下单
                     <el-button type="primary" link size="small" style="margin-left: 8px;" @click="goToNote(currentOrder.sourceNote.id)">
                         查看笔记 ->
                     </el-button>
                  </div>
              </div>
             
             <div class="detail-section-title">商品清单</div>
             <el-table :data="currentOrder.products" border size="small">
                 <el-table-column label="商品" width="60">
                     <template #default="{ row }">
                         <el-image :src="resolveImageUrl(row.cover)" style="width: 30px; height: 30px; border-radius: 2px; cursor: pointer" @click="goToProduct(row.id)" />
                     </template>
                 </el-table-column>
                 <el-table-column label="名称">
                     <template #default="{ row }">
                         <span style="cursor: pointer; color: var(--admin-professional-primary);" @click="goToProduct(row.id)">{{ row.name }}</span>
                     </template>
                 </el-table-column>
                 <el-table-column label="单价" width="100" align="right">
                     <template #default="{ row }">¥{{ row.price.toFixed(2) }}</template>
                 </el-table-column>
                 <el-table-column prop="count" label="数量" width="80" align="center" />
                 <el-table-column label="小计" align="right" width="120">
                      <template #default="{ row }">¥{{ (row.price * row.count).toFixed(2) }}</template>
                 </el-table-column>
             </el-table>

             <el-row :gutter="20" style="margin-top: 20px">
                <el-col :span="12">
                    <div class="user-summary-card">
                        <div class="section-small-title">买家信息</div>
                        <div class="user-brief">
                            <el-avatar :size="40" :src="resolveImageUrl(currentOrder.user.avatar)" />
                            <div class="u-info">
                                <div class="u-name">{{ currentOrder.user.nickname }}</div>
                                <div class="u-stats" v-if="userOrderStats">
                                    历史订单: {{ userOrderStats.count }} | 总额: ¥{{ userOrderStats.total }}
                                </div>
                            </div>
                            <UserInfoPopover v-if="currentOrder.user?.id" :user-id="currentOrder.user.id" placement="left" :width="340">
                                <template #reference>
                                    <el-button link type="primary" size="small">查看用户详情</el-button>
                                </template>
                            </UserInfoPopover>
                        </div>
                    </div>
                </el-col>
                <el-col :span="12">
                    <div v-if="currentOrder.logistics">
                         <div class="section-small-title">物流信息</div>
                         <el-descriptions :column="1" border size="small">
                             <el-descriptions-item label="物流公司">{{ currentOrder.logistics.company }}</el-descriptions-item>
                             <el-descriptions-item label="运单号">
                                {{ currentOrder.logistics.trackingNo }}
                                <el-button link type="primary" size="small" style="margin-left: 5px" @click="copyText(currentOrder.logistics.trackingNo)">复制</el-button>
                             </el-descriptions-item>
                         </el-descriptions>
                    </div>
                </el-col>
             </el-row>

             <div class="detail-section-title">操作日志</div>
             <el-table :data="currentOrder.logs" border size="small" style="width: 100%">
                <template #empty>
                    <div style="color: #909399; padding: 10px">暂无操作日志</div>
                </template>
                <el-table-column prop="time" label="时间" width="160" />
                <el-table-column prop="operator" label="操作人" width="100" />
                <el-table-column prop="content" label="操作内容" />
                <el-table-column prop="remark" label="备注" />
             </el-table>
        </div>
    </el-dialog>

  <!-- 退款处理弹窗 -->
    <el-dialog v-model="refundDialogVisible" title="退款申请处理" width="450px">
        <el-form :model="refundForm" label-width="80px">
            <el-form-item label="订单号">{{ refundForm.orderNo }}</el-form-item>
            <el-form-item label="退款金额">
                <span style="color: #f56c6c; font-weight: bold">¥{{ refundForm.amount.toFixed(2) }}</span>
            </el-form-item>
            <el-form-item label="申请原因">{{ refundForm.reason }}</el-form-item>
            <el-form-item label="退款凭证">
                <div v-if="refundForm.evidenceImages.length" style="display:flex; gap:8px; flex-wrap:wrap;">
                    <el-image
                      v-for="(img, idx) in refundForm.evidenceImages"
                      :key="idx"
                      :src="resolveImageUrl(img)"
                      style="width:56px; height:56px; border-radius:8px; border:1px solid #e5e7eb;"
                      fit="cover"
                    />
                </div>
                <span v-else>-</span>
            </el-form-item>
            <el-form-item label="处理备注">
                <el-input v-model="refundForm.remark" type="textarea" :rows="3" placeholder="请输入处理备注；若拒绝请填写理由" />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="handleRefundAction(false)">拒绝退款</el-button>
            <el-button type="primary" @click="handleRefundAction(true)">同意退款</el-button>
        </template>
    </el-dialog>
    
  <!-- 修改地址弹窗 -->
    <el-dialog v-model="addressDialogVisible" title="修改收货信息" width="500px">
        <el-form :model="addressForm" label-width="80px">
            <el-form-item label="收货人" required>
                <el-input v-model="addressForm.name" placeholder="请输入收货人姓名" />
            </el-form-item>
            <el-form-item label="联系电话" required>
                <el-input v-model="addressForm.phone" placeholder="请输入联系电话" />
            </el-form-item>
            <el-form-item label="收货地址" required>
                <el-input v-model="addressForm.detail" type="textarea" :rows="3" placeholder="请输入详细收货地址" />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="addressDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="saveAddress">保存修改</el-button>
        </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search, Download, CopyDocument, Warning, Refresh, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as orderApi from '@/api/order'
import { resolveImageUrl } from '@/utils/image'
import UserInfoPopover from '@/components/UserInfoPopover.vue'


const router = useRouter()
const route = useRoute()
const activeTab = ref('all')
const searchKeyword = ref('')
const filterPayType = ref('')
const sortOrder = ref('time_desc')
const dateRange = ref([])
const loading = ref(false)

const allData = ref([])
const tableData = ref([])

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

onMounted(async () => {
    // 处理从看板跳过来的筛选参数
    if (route.query.startDate && route.query.endDate) {
        dateRange.value = [route.query.startDate, route.query.endDate]
        ElMessage.success(`已自动筛选 ${route.query.startDate} 至 ${route.query.endDate}`)
    }

    await loadOrderList()
})

const loadOrderList = async () => {
    loading.value = true
    try {
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
            allData.value = (res.data.records || []).map(mapOrderFromApi)
            filterData()
            total.value = res.data.total || 0
        } else {
            ElMessage.error(res.message || res.msg || '加载订单列表失败')
        }
    } catch (e) {
        console.error('加载订单列表失败', e)
    } finally {
        selectedRows.value = []
        loading.value = false
    }
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
    currentPage.value = 1
    loadOrderList()
}

const handleFilter = () => {
    currentPage.value = 1
    loadOrderList()
}

const resetFilter = () => {
    searchKeyword.value = ''
    filterPayType.value = ''
    sortOrder.value = 'time_desc'
    dateRange.value = []
    currentPage.value = 1
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

const goToUser = (userId) => {
    detailDialogVisible.value = false
    ElMessage.success('Jumping to user management...')
    router.push({ path: '/admin/user', query: { userId: userId } })
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

</script>

<style scoped>
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
