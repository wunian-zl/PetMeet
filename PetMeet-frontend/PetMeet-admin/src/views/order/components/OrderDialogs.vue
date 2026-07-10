<template>
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
        <el-input
          v-model="shipForm.remark"
          type="textarea"
          :rows="2"
          placeholder="可选：补充说明，方便买家查看"
        />
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
          <el-step
            title="支付"
            :description="currentOrder.status === 'unpaid' ? '等待支付' : '已支付'"
          />
          <el-step title="发货" :description="currentOrder.logistics?.shipTime || '等待发货'" />
          <el-step
            title="完成"
            :description="currentOrder.status === 'completed' ? '订单已完成' : ''"
          />
        </el-steps>
      </div>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-descriptions :column="1" border>
            <template #title>
              <div style="display: flex; justify-content: space-between; align-items: center">
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
            <el-descriptions-item label="收货人">{{
              currentOrder.address.name
            }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{
              currentOrder.address.phone
            }}</el-descriptions-item>
            <el-descriptions-item label="收货地址">{{
              currentOrder.address.detail
            }}</el-descriptions-item>
            <el-descriptions-item label="用户备注">
              <span :class="{ 'text-minor': !currentOrder.notes?.user }">{{
                currentOrder.notes?.user || '-'
              }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-col>
        <el-col :span="12">
          <el-descriptions title="付款信息" :column="1" border>
            <el-descriptions-item label="支付方式">{{
              currentOrder.payType || '未支付'
            }}</el-descriptions-item>
            <el-descriptions-item label="支付流水">{{
              currentOrder.paySn || '-'
            }}</el-descriptions-item>
            <el-descriptions-item label="第三方交易号">{{
              currentOrder.tradeNo || '-'
            }}</el-descriptions-item>
            <el-descriptions-item label="商品总额"
              >¥{{
                (currentOrder.amount + (currentOrder.discount?.coupon || 0)).toFixed(2)
              }}</el-descriptions-item
            >
            <el-descriptions-item label="优惠金额"
              >- ¥{{ (currentOrder.discount?.coupon || 0).toFixed(2) }}</el-descriptions-item
            >
            <el-descriptions-item label="实付金额">
              <span class="highlight-price">¥{{ currentOrder.amount.toFixed(2) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="已退金额"
              >¥{{ currentOrder.refundAmount.toFixed(2) }}</el-descriptions-item
            >
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
      <div
        v-if="currentOrder.sourceNote"
        class="source-note-section"
        style="
          margin-top: 15px;
          padding: 12px;
          background: linear-gradient(135deg, #ecf1fd 0%, #f0f9eb 100%);
          border-radius: 8px;
          border: 1px dashed var(--admin-professional-primary);
        "
      >
        <div style="display: flex; align-items: center; gap: 8px">
          <el-icon color="var(--admin-professional-primary)"><TrendCharts /></el-icon>
          <span style="font-weight: 600; color: #303133">来源笔记（内容种草）</span>
        </div>
        <div style="margin-top: 8px; font-size: 14px; color: #606266">
          用户通过阅读笔记《{{ currentOrder.sourceNote.title }}》后下单
          <el-button
            type="primary"
            link
            size="small"
            style="margin-left: 8px"
            @click="goToNote(currentOrder.sourceNote.id)"
          >
            查看笔记 ->
          </el-button>
        </div>
      </div>

      <div class="detail-section-title">商品清单</div>
      <el-table :data="currentOrder.products" border size="small">
        <el-table-column label="商品" width="60">
          <template #default="{ row }">
            <el-image
              :src="resolveImageUrl(row.cover)"
              style="width: 30px; height: 30px; border-radius: 2px; cursor: pointer"
              @click="goToProduct(row.id)"
            />
          </template>
        </el-table-column>
        <el-table-column label="名称">
          <template #default="{ row }">
            <span
              style="cursor: pointer; color: var(--admin-professional-primary)"
              @click="goToProduct(row.id)"
              >{{ row.name }}</span
            >
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
              <UserInfoPopover
                v-if="currentOrder.user?.id"
                :user-id="currentOrder.user.id"
                placement="left"
                :width="340"
              >
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
              <el-descriptions-item label="物流公司">{{
                currentOrder.logistics.company
              }}</el-descriptions-item>
              <el-descriptions-item label="运单号">
                {{ currentOrder.logistics.trackingNo }}
                <el-button
                  link
                  type="primary"
                  size="small"
                  style="margin-left: 5px"
                  @click="copyText(currentOrder.logistics.trackingNo)"
                  >复制</el-button
                >
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
        <div
          v-if="refundForm.evidenceImages.length"
          style="display: flex; gap: 8px; flex-wrap: wrap"
        >
          <el-image
            v-for="(img, idx) in refundForm.evidenceImages"
            :key="idx"
            :src="resolveImageUrl(img)"
            style="width: 56px; height: 56px; border-radius: 8px; border: 1px solid #e5e7eb"
            fit="cover"
          />
        </div>
        <span v-else>-</span>
      </el-form-item>
      <el-form-item label="处理备注">
        <el-input
          v-model="refundForm.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入处理备注；若拒绝请填写理由"
        />
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
        <el-input
          v-model="addressForm.detail"
          type="textarea"
          :rows="3"
          placeholder="请输入详细收货地址"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="addressDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveAddress">保存修改</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { inject } from 'vue'

const {
  copyText,
  resolveImageUrl,
  goToProduct,
  UserInfoPopover,
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
} = inject('adminOrderPageContext')
</script>
