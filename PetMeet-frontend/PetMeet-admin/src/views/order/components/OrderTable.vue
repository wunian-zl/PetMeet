<template>
  <el-card shadow="never" class="table-card">
    <el-table
      :data="tableData"
      style="width: 100%"
      v-loading="loading"
      @selection-change="handleSelectionChange"
    >
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
              <el-tag size="small" type="primary" effect="plain">{{
                row.payType || '未支付'
              }}</el-tag>
            </div>
            <div v-if="row.paySn" class="pay-sn-line">{{ row.paySn }}</div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="商品信息" min-width="220">
        <template #default="{ row }">
          <div class="product-list-wrapper">
            <div v-for="(prod, idx) in row.products.slice(0, 1)" :key="idx" class="product-item">
              <el-image
                :src="resolveImageUrl(prod.cover)"
                style="
                  width: 40px;
                  height: 40px;
                  border-radius: 4px;
                  margin-right: 8px;
                  cursor: pointer;
                "
                @click="goToProduct(prod.id)"
              />
              <div class="product-meta">
                <div
                  class="product-name"
                  style="font-size: 14px; cursor: pointer; color: var(--admin-professional-primary)"
                  @click="goToProduct(prod.id)"
                >
                  {{ prod.name }}
                </div>
                <div class="product-price" style="font-size: 13px">
                  ¥{{ prod.price }} x {{ prod.count }}
                </div>
              </div>
            </div>
            <el-popover
              v-if="row.products.length > 1"
              placement="bottom-start"
              :width="280"
              trigger="hover"
              :show-after="200"
            >
              <template #reference>
                <div
                  class="more-products"
                  style="
                    cursor: pointer;
                    color: var(--admin-professional-primary);
                    font-size: 14px;
                    margin-top: 4px;
                    display: inline-block;
                  "
                >
                  共{{ row.products.length }} 个商品...
                </div>
              </template>
              <div class="popover-product-list">
                <div
                  v-for="(prod, idx) in row.products"
                  :key="idx"
                  style="
                    display: flex;
                    align-items: center;
                    padding: 6px 0;
                    border-bottom: 1px solid #eee;
                  "
                >
                  <el-image
                    :src="resolveImageUrl(prod.cover)"
                    style="
                      width: 36px;
                      height: 36px;
                      border-radius: 4px;
                      margin-right: 8px;
                      flex-shrink: 0;
                    "
                  />
                  <div style="flex: 1; overflow: hidden">
                    <div
                      style="
                        font-size: 14px;
                        white-space: nowrap;
                        overflow: hidden;
                        text-overflow: ellipsis;
                      "
                    >
                      {{ prod.name }}
                    </div>
                    <div style="font-size: 13px; color: #f56c6c">
                      ¥{{ prod.price }} x {{ prod.count }}
                    </div>
                  </div>
                </div>
              </div>
            </el-popover>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="买家信息" width="160">
        <template #default="{ row }">
          <UserInfoPopover
            v-if="row.user?.id"
            :user-id="row.user.id"
            placement="right"
            :width="340"
          >
            <template #reference>
              <div class="user-info" style="cursor: pointer">
                <el-avatar :size="24" :src="resolveImageUrl(row.user.avatar)" />
                <span style="margin-left: 8px">{{ row.user.nickname }}</span>
                <el-tooltip
                  v-if="row.user.risk"
                  content="风险用户：曾有频繁退款行为"
                  placement="top"
                >
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
          <div style="display: flex; flex-direction: column; gap: 4px; align-items: center">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
            <el-tag v-if="row.status === 'refunding'" type="danger" effect="dark" size="small">
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
            {{ row.refund?.afterSaleId ? '售后处理' : '退款处理' }}
          </el-button>
          <el-button v-if="canDeleteOrder(row)" link type="danger" @click="handleDeleteOrder(row)">
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
</template>

<script setup>
import { inject } from 'vue'

const {
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
  handlePageChange
} = inject('adminOrderPageContext')
</script>
