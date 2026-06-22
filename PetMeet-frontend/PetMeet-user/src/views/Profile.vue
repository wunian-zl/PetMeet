<template>
  <div class="profile-page">
    <AuthRequiredState
      v-if="!userStore.isLoggedIn"
      type="profile"
      title="登录后进入个人中心"
      description="你的资料、订单、收藏、地址和互动记录会在登录后展示。"
    />
    <template v-else>
  <!-- 头部区域 -->
    <div class="profile-header">
      <div class="profile-banner">
        <div class="banner-gradient"></div>
      </div>
      
        <div class="user-card">
          <div class="avatar-area">
            <div class="avatar-wrapper" role="button" tabindex="0" @click="openAvatarZoom" @keydown.enter="openAvatarZoom">
              <el-avatar :size="100" :src="displayAvatar" icon="UserFilled" class="avatar" />
            </div>
            <el-button round class="change-avatar-btn" @click="openEditProfile">修改头像</el-button>
          </div>
          <div class="user-info">
          <h1 class="nickname">{{ userStore.userInfo.nickname || '用户' }}</h1>
          <p class="uid">ID: {{ userStore.userInfo.id }}</p>
        </div>
        
        <div class="stats-row">
          <div class="stat-item" @click="goToFollow('following')">
            <span class="value">{{ followStats.following }}</span>
            <span class="label">关注</span>
          </div>
          <div class="divider"></div>
          <div class="stat-item" @click="goToFollow('followers')">
            <span class="value">{{ followStats.followers }}</span>
            <span class="label">粉丝</span>
          </div>
        </div>

        <div class="action-buttons">
            <el-button round class="edit-btn" @click="openEditProfile">编辑资料</el-button>
            <el-button round class="logout-btn" type="danger" plain @click="handleLogout">退出登录</el-button>
        </div>
      </div>
    </div>

  <!-- 内容区域 -->
    <div class="profile-body">
      <div class="content-wrapper">
        <el-tabs
          v-model="activeTab"
          :class="['custom-tabs', { 'custom-tabs--static-bar': !tabBarReady }]"
          @tab-click="handleTabClick"
        >
      <!-- 我的笔记 -->
          <el-tab-pane name="notes">
            <template #label>
              <div class="tab-label">
                <el-icon><Document /></el-icon>
                <span>我的笔记</span>
              </div>
            </template>
            
            <div v-loading="loadingNotes" class="tab-content">
              <el-empty v-if="!loadingNotes && myNotes.length === 0" description="暂无笔记" />
              <div class="notes-grid" v-else>
                <div 
                  v-for="note in myNotes" 
                  :key="note.id" 
                  class="note-card"
                  @click="router.push(`/note/detail/${note.id}`)"
                >
                  <div class="card-cover">
                    <img
                      :src="resolveNoteCardImage(note)"
                      loading="lazy"
                      @error="handleNoteCardImageError($event, note)"
                    />
                    <el-dropdown trigger="click" @command="(cmd) => handleMyNoteCommand(cmd, note)">
                      <el-button class="note-action-btn" circle size="small" @click.stop>
                        <el-icon><MoreFilled /></el-icon>
                      </el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item
                            v-if="canToggleMyNoteShelf(note)"
                            :command="note.status === 4 ? 'restore' : 'shelf'"
                          >
                            {{ note.status === 4 ? '恢复上架' : '下架' }}
                          </el-dropdown-item>
                          <el-dropdown-item
                            divided
                            command="delete"
                            :disabled="!canDeleteMyNote(note)"
                          >
                            删除
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                    <div v-if="note.type === 'video'" class="type-badge">
                      <el-icon><VideoPlay /></el-icon>
                    </div>
                    <div class="status-tag" :class="`status-${note.status}`">
                        {{ note.statusDesc || getNoteStatusText(note.status) }}
                    </div>
                  </div>
                  <div class="card-info">
                    <div class="card-title">{{ note.title }}</div>
                    <div class="card-meta">
                      <span class="date">{{ formatTime(note.createTime, 'MM-DD') }}</span>
                      <span class="likes">
                        <el-icon><Star /></el-icon> {{ note.likeCount }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

      <!-- 我的收藏 -->
          <el-tab-pane name="collections">
            <template #label>
              <div class="tab-label">
                <el-icon><StarFilled /></el-icon>
                <span>收藏的笔记</span>
              </div>
            </template>

            <div v-loading="loadingCollected" class="tab-content">
              <el-empty v-if="!loadingCollected && collectedNotes.length === 0" description="暂无收藏" />
              <div class="notes-grid" v-else>
                <div 
                  v-for="note in collectedNotes" 
                  :key="note.id" 
                  class="note-card"
                  @click="router.push(`/note/detail/${note.id}`)"
                >
                  <div class="card-cover">
                    <img
                      :src="resolveNoteCardImage(note)"
                      loading="lazy"
                      @error="handleNoteCardImageError($event, note)"
                    />
                    <div v-if="note.type === 'video'" class="type-badge">
                      <el-icon><VideoPlay /></el-icon>
                    </div>
                  </div>
                  <div class="card-info">
                    <div class="card-title">{{ note.title }}</div>
                    <div class="card-meta">
                      <span class="date">{{ formatTime(note.createTime, 'MM-DD') }}</span>
                      <span class="likes">
                        <el-icon><Star /></el-icon> {{ note.likeCount }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

      <!-- 我的点赞 -->
          <el-tab-pane name="likes">
            <template #label>
              <div class="tab-label">
                <el-icon><HeartIcon filled /></el-icon>
                <span>点赞的笔记</span>
              </div>
            </template>

            <div v-loading="loadingLiked" class="tab-content">
              <el-empty v-if="!loadingLiked && likedNotes.length === 0" description="暂无点赞" />
              <div class="notes-grid" v-else>
                <div 
                  v-for="note in likedNotes" 
                  :key="note.id" 
                  class="note-card"
                  @click="router.push(`/note/detail/${note.id}`)"
                >
                  <div class="card-cover">
                    <img
                      :src="resolveNoteCardImage(note)"
                      loading="lazy"
                      @error="handleNoteCardImageError($event, note)"
                    />
                    <div v-if="note.type === 'video'" class="type-badge">
                      <el-icon><VideoPlay /></el-icon>
                    </div>
                  </div>
                  <div class="card-info">
                    <div class="card-title">{{ note.title }}</div>
                    <div class="card-meta">
                      <span class="date">{{ formatTime(note.createTime, 'MM-DD') }}</span>
                      <span class="likes">
                        <el-icon><Star /></el-icon> {{ note.likeCount }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

      <!-- 我的订单 -->
          <el-tab-pane name="orders">
            <template #label>
              <div class="tab-label">
                <el-badge :is-dot="unpaidCount > 0" class="custom-badge">
                  <el-icon><Goods /></el-icon>
                </el-badge>
                <span>我的订单</span>
              </div>
            </template>

            <div v-loading="loadingOrders" class="tab-content">
              <el-tabs v-model="orderSubTab" class="order-sub-tabs">
                <el-tab-pane name="pending_pay">
                  <template #label>
                    <div class="order-tab-label">
                      <span>待付款</span>
                      <span v-if="pendingPayOrders.length" class="count">{{ pendingPayOrders.length }}</span>
                    </div>
                  </template>
                </el-tab-pane>
                <el-tab-pane name="pending_receive">
                  <template #label>
                    <div class="order-tab-label">
                      <span>待收货</span>
                      <span v-if="pendingReceiveOrders.length" class="count">{{ pendingReceiveOrders.length }}</span>
                    </div>
                  </template>
                </el-tab-pane>
                <el-tab-pane name="pending_review">
                  <template #label>
                    <div class="order-tab-label">
                      <span>待评价/晒单</span>
                      <span v-if="pendingReviewOrders.length" class="count">{{ pendingReviewOrders.length }}</span>
                    </div>
                  </template>
                </el-tab-pane>
                <el-tab-pane name="after_sale">
                  <template #label>
                    <div class="order-tab-label">
                      <el-badge :is-dot="afterSaleUnreadCount > 0" class="custom-badge">
                        <span>退款/售后</span>
                      </el-badge>
                    </div>
                  </template>
                </el-tab-pane>
                <el-tab-pane name="all">
                  <template #label>
                    <div class="order-tab-label">
                      <span>全部</span>
                      <span v-if="allOrders.length" class="count">{{ allOrders.length }}</span>
                    </div>
                  </template>
                </el-tab-pane>
              </el-tabs>

              <div v-if="orderSubTab !== 'after_sale'">
                <div class="order-toolbar">
                  <div class="toolbar-left">
                    <span>共 {{ currentOrders.length }} 单</span>
                    <div
                      v-if="showOrderDeleteControls && deletableOrderIdsInView.length > 0"
                      class="order-delete-controls delete-selection"
                      @click.stop
                    >
                      <el-checkbox
                        :model-value="allDeletableOrdersSelected"
                        :indeterminate="deletableOrderSelectionIndeterminate"
                        @change="handleToggleSelectAllDeletable"
                      >
                        全选可删
                      </el-checkbox>
                      <span class="delete-selected-count">已选 {{ selectedOrderIds.length }}</span>
                    </div>
                  </div>
                  <div class="toolbar-right">
                    <el-button
                      v-if="showOrderDeleteControls"
                      class="pm-btn-delete-ghost"
                      size="small"
                      :disabled="selectedOrderIds.length === 0"
                      @click.stop="handleBatchDeleteOrders"
                    >
                      批量删除
                    </el-button>
                    <el-select v-model="orderSort" size="small" placeholder="排序">
                      <el-option label="下单时间：最新" value="time_desc">
                        <div class="order-sort-option">
                          <span class="prefix">下单时间</span>
                          <span class="value">最新</span>
                        </div>
                      </el-option>
                      <el-option label="下单时间：最早" value="time_asc">
                        <div class="order-sort-option">
                          <span class="prefix">下单时间</span>
                          <span class="value">最早</span>
                        </div>
                      </el-option>
                      <el-option label="金额：高到低" value="amount_desc">
                        <div class="order-sort-option">
                          <span class="prefix">金额</span>
                          <span class="value">高到低</span>
                        </div>
                      </el-option>
                      <el-option label="金额：低到高" value="amount_asc">
                        <div class="order-sort-option">
                          <span class="prefix">金额</span>
                          <span class="value">低到高</span>
                        </div>
                      </el-option>
                    </el-select>
                  </div>
                </div>
                <el-empty v-if="!loadingOrders && currentOrders.length === 0" description="暂无订单" />
                <div class="order-list" v-else>
                  <div v-for="order in sortedOrderList" :key="order.id" class="order-item" @click="openOrderDetail(order)">
                    <div class="order-header">
                      <div class="order-header-left">
                        <el-checkbox
                          v-if="showOrderDeleteControls && isOrderDeletable(order)"
                          class="order-select-checkbox delete-selection"
                          :model-value="isOrderSelected(order.id)"
                          @change="(checked) => handleToggleOrderSelected(order.id, checked)"
                          @click.stop
                        />
                        <span class="order-sn">订单号: {{ order.orderSn }}</span>
                      </div>
                      <span class="order-status" :class="getOrderStatusClass(order)">
                        {{ getOrderStatusText(order) }}
                      </span>
                    </div>
                    <div class="order-body">
                      <div class="order-main">
                        <div class="order-products">
                          <div v-if="getOrderPreviewItems(order).length > 0" class="product-thumbs">
                            <div
                              v-for="(item, idx) in getOrderPreviewItems(order)"
                              :key="item.id || item.productId || idx"
                              class="thumb"
                            >
                              <img v-if="item.productImg" :src="getImageUrl(item.productImg)" loading="lazy" />
                              <div v-else class="thumb-placeholder">
                                <el-icon><Goods /></el-icon>
                              </div>
                              <div v-if="Number(item.quantity || 0) > 1" class="thumb-qty">x{{ item.quantity }}</div>
                            </div>
                            <div v-if="getOrderMoreCount(order) > 0" class="thumb-more">+{{ getOrderMoreCount(order) }}</div>
                          </div>
                          <div class="product-brief">
                            <div class="product-title">{{ getOrderTitle(order) }}</div>
                            <div class="order-meta">
                              <p>金额: <span class="price">¥{{ Number(order.totalAmount || 0).toFixed(2) }}</span></p>
                              <p class="time">{{ formatTime(order.createTime) }}</p>
                              <p v-if="Number(order.status) === 0" class="pay-countdown">剩余支付时间 {{ formatPayCountdown(order) }}</p>
                            </div>
                          </div>
                        </div>
                      </div>
                      <div v-if="Number(order.status) === 5" class="refund-warning">
                        退款处理中，请暂停发货并优先处理退款。
                      </div>
                      <div class="order-actions">
                        <el-button
                          v-if="order.status === 0"
                          class="pm-btn-primary"
                          round
                          size="small"
                          @click.stop="handlePay(order)"
                        >去支付</el-button>
                        <el-button
                          v-if="order.status === 0"
                          class="pm-btn-ghost"
                          round
                          size="small"
                          @click.stop="handleCancelOrder(order)"
                        >取消订单</el-button>
                        <el-button
                          v-if="order.status === 2"
                          class="pm-btn-ghost"
                          round
                          size="small"
                          @click.stop="openLogisticsDialog(order)"
                        >查看物流</el-button>
                        <el-button
                          v-if="order.status === 2"
                          class="pm-btn-primary"
                          round
                          size="small"
                          @click.stop="handleConfirmReceipt(order)"
                        >确认收货</el-button>
                        <el-button
                          v-if="isPendingReview(order)"
                          class="pm-btn-primary"
                          round
                          size="small"
                          @click.stop="openReviewDialog(order)"
                        >去评价</el-button>
                        <el-button
                          v-if="isPendingReview(order) && orderSubTab === 'pending_review' && !isOrderSeedNotePublished(order)"
                          class="pm-btn-ghost"
                          round
                          size="small"
                          @click.stop="goPublishFromOrder(order)"
                        >发布种草笔记</el-button>
                        <el-button
                          v-if="isPendingReview(order) && orderSubTab === 'pending_review' && isOrderSeedNotePublished(order)"
                          class="pm-btn-ghost"
                          round
                          size="small"
                          disabled
                        >已发布种草笔记</el-button>
                        <el-button
                          v-if="Number(order.status) === 3 && !isPendingReview(order) && !isOrderSeedNotePublished(order)"
                          class="pm-btn-ghost"
                          round
                          size="small"
                          @click.stop="goPublishFromOrder(order)"
                        >发布种草笔记</el-button>
                        <el-button
                          v-if="Number(order.status) === 3 && !isPendingReview(order) && isOrderSeedNotePublished(order)"
                          class="pm-btn-ghost"
                          round
                          size="small"
                          disabled
                        >已发布种草笔记</el-button>
                        <el-button
                          v-if="showOrderDeleteControls && isOrderDeletable(order)"
                          class="pm-btn-delete-ghost"
                          round
                          size="small"
                          @click.stop="handleDeleteOrder(order)"
                        >删除订单</el-button>
                        <el-button link size="small">查看详情 <el-icon><ArrowRight /></el-icon></el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-else class="after-sale-panel" v-loading="loadingAfterSale">
                <el-tabs v-model="afterSaleTab" class="after-sale-tabs">
                  <el-tab-pane name="apply">
                    <template #label>售后申请</template>
                    <el-empty v-if="afterSaleApplyItems.length === 0" description="暂无可申请的订单商品" />
                    <div class="after-sale-list" v-else>
                      <div v-for="entry in afterSaleApplyItems" :key="entry.item.id" class="after-sale-card">
                        <div class="card-left">
                          <img v-if="entry.item.productImg" :src="getImageUrl(entry.item.productImg)" />
                          <div v-else class="img-placeholder"><el-icon><Goods /></el-icon></div>
                          <div class="info">
                            <div class="name">{{ entry.item.productName }}</div>
                            <div class="meta">订单号: {{ entry.order.orderSn }}</div>
                            <div class="meta">¥{{ Number(entry.item.price || 0).toFixed(2) }} x {{ entry.item.quantity }}</div>
                          </div>
                        </div>
                        <div class="card-right">
                          <span v-if="entry.activeRequest" class="after-sale-status" :class="`status-${entry.activeRequest.status}`">
                            {{ getAfterSaleStatusText(entry.activeRequest.status, entry.activeRequest.statusDesc) }}
                          </span>
                          <el-button
                            v-if="!entry.activeRequest"
                            class="pm-btn-primary"
                            round
                            size="small"
                            @click="openAfterSaleDialog(entry)"
                          >申请售后</el-button>
                          <div v-else class="after-sale-actions">
                            <el-button link size="small" @click="switchToAfterSaleRecords">查看记录</el-button>
                            <el-button link size="small" type="danger" @click="handleDeleteAfterSale(entry.activeRequest)">删除</el-button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </el-tab-pane>

                  <el-tab-pane name="processing">
                    <template #label>处理中</template>
                    <el-empty v-if="afterSaleProcessingList.length === 0" description="暂无处理中售后" />
                    <div class="after-sale-list" v-else>
                      <div v-for="req in afterSaleProcessingList" :key="req.id" class="after-sale-card">
                        <div class="card-left">
                          <img v-if="req.productImg" :src="getImageUrl(req.productImg)" />
                          <div v-else class="img-placeholder"><el-icon><Goods /></el-icon></div>
                          <div class="info">
                            <div class="name">{{ req.productName }}</div>
                            <div class="meta">订单号: {{ req.orderSn }}</div>
                            <div class="meta">类型: {{ req.typeDesc || getAfterSaleTypeText(req.type) }}</div>
                            <div class="meta">原因: {{ req.reason || '-' }}</div>
                          </div>
                        </div>
                        <div class="card-right">
                          <span class="after-sale-status" :class="`status-${req.status}`">
                            {{ getAfterSaleStatusText(req.status, req.statusDesc) }}
                          </span>
                          <div class="after-sale-actions">
                            <el-button class="pm-btn-ghost" size="small" @click="handleCancelAfterSale(req)">撤销</el-button>
                            <el-button class="pm-btn-primary" size="small" @click="handleCompleteAfterSale(req)">确认完成</el-button>
                            <el-button link size="small" type="danger" @click="handleDeleteAfterSale(req)">删除</el-button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </el-tab-pane>

                  <el-tab-pane name="records">
                    <template #label>申请记录</template>
                    <el-empty v-if="afterSaleRecordList.length === 0" description="暂无申请记录" />
                    <div class="after-sale-list" v-else>
                      <div v-for="req in afterSaleRecordList" :key="req.id" class="after-sale-card">
                        <div class="card-left">
                          <img v-if="req.productImg" :src="getImageUrl(req.productImg)" />
                          <div v-else class="img-placeholder"><el-icon><Goods /></el-icon></div>
                          <div class="info">
                            <div class="name">{{ req.productName }}</div>
                            <div class="meta">订单号: {{ req.orderSn }}</div>
                            <div class="meta">类型: {{ req.typeDesc || getAfterSaleTypeText(req.type) }}</div>
                            <div class="meta">原因: {{ req.reason || '-' }}</div>
                            <div class="meta" v-if="req.handleRemark">备注: {{ req.handleRemark }}</div>
                            <div class="evidence-row" v-if="Array.isArray(req.evidenceImages) && req.evidenceImages.length">
                              <el-image
                                v-for="(img, idx) in req.evidenceImages"
                                :key="`${req.id}-${idx}`"
                                :src="getImageUrl(img)"
                                class="evidence-thumb"
                                fit="cover"
                              />
                            </div>
                          </div>
                        </div>
                        <div class="card-right">
                          <span class="after-sale-status" :class="`status-${req.status}`">
                            {{ getAfterSaleStatusText(req.status, req.statusDesc) }}
                          </span>
                          <div class="after-sale-actions">
                            <el-button link size="small" type="danger" @click="handleDeleteAfterSale(req)">删除</el-button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </el-tab-pane>
                </el-tabs>
              </div>
            </div>
          </el-tab-pane>

      <!-- 地址管理 -->
          <el-tab-pane name="address">
            <template #label>
              <div class="tab-label">
                <el-icon><Location /></el-icon>
                <span>收货地址</span>
              </div>
            </template>

            <div v-loading="loadingAddress" class="tab-content">
               <div class="address-actions">
                  <el-button class="add-addr-btn" icon="Plus" round @click="openAddressDialog()">新增地址</el-button>
               </div>
               
               <div class="address-grid">
                 <div v-for="addr in addressList" :key="addr.id" class="address-card">
                    <div class="addr-header">
                      <span class="name">{{ addr.name }}</span>
                      <span class="phone">{{ addr.phone }}</span>
                      <el-tag v-if="addr.isDefault === 1" size="small" type="danger" effect="dark" round>默认</el-tag>
                    </div>
                    <div class="addr-body">
                      <div class="location-text">
                        {{ addr.province }} {{ addr.city }} {{ addr.region }}
                        <br/>
                        {{ addr.detailAddress }}
                      </div>
                    </div>
                    <div class="addr-footer">
                       <el-button class="action-btn" text bg size="small" icon="Edit" @click="openAddressDialog(addr)">编辑</el-button>
                       <el-button class="action-btn delete" text bg type="danger" size="small" icon="Delete" @click="handleDeleteAddress(addr.id)">删除</el-button>
                    </div>
                 </div>
               </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

  <!-- 订单详情弹窗 -->
      <el-dialog v-model="orderDetailVisible" title="订单详情" width="600px" destroy-on-close align-center class="custom-dialog">
        <div v-loading="orderDetailLoading" class="detail-container">
          <template v-if="orderDetail">
            <div class="detail-header">
               <div class="status-row">
                 <span class="label">当前状态</span>
                 <span class="status-text" :class="getOrderStatusClass(orderDetail)">
                   {{ getOrderStatusText(orderDetail) }}
                 </span>
               </div>
               <div class="sn-row">编号: {{ orderDetail.orderSn }}</div>
            </div>

            <div class="detail-section">
              <h3>收货信息</h3>
              <div class="info-row"><el-icon><User /></el-icon> {{ orderDetail.receiverName }} {{ orderDetail.receiverPhone }}</div>
              <div class="info-row"><el-icon><LocationInformation /></el-icon> {{ orderDetail.receiverAddress }}</div>
            </div>

            <div class="detail-section">
              <h3>商品清单</h3>
              <div class="product-list-mini">
                <div v-for="item in (orderDetail.items || [])" :key="item.id" class="product-item-mini">
                   <img :src="getImageUrl(item.productImg)" />
                   <div class="p-info">
                     <div class="p-name">{{ item.productName }}</div>
                     <div class="p-meta">¥{{ Number(item.price).toFixed(2) }} x {{ item.quantity }}</div>
                   </div>
                   <div class="p-total">¥{{ Number(item.subtotal).toFixed(2) }}</div>
                </div>
              </div>
            </div>

            <div class="detail-section" v-if="orderDetail.reviewStatus === 1 || orderDetail.reviewContent">
              <h3>评价信息</h3>
              <div class="info-row">评分：{{ orderDetail.reviewScore || '-' }}</div>
              <div class="info-row">内容：{{ orderDetail.reviewContent || '暂无' }}</div>
              <div class="info-row" v-if="orderDetail.reviewTime">时间：{{ formatTime(orderDetail.reviewTime) }}</div>
            </div>
            
            <div class="detail-footer">
              <span class="label">实付金额:</span>
              <span class="amount">¥{{ Number(orderDetail.totalAmount || 0).toFixed(2) }}</span>
            </div>
          </template>
        </div>
      </el-dialog>

  <!-- 订单评价弹窗 -->
      <el-dialog v-model="reviewDialogVisible" title="订单评价" width="520px" destroy-on-close align-center class="custom-dialog review-dialog">
        <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="80px" label-position="left">
          <el-form-item label="评分" prop="score">
            <el-rate v-model="reviewForm.score" :max="5" :colors="['#ff6b81', '#ff6b81', '#ff6b81']" />
          </el-form-item>
          <el-form-item label="评价" prop="content">
            <el-input type="textarea" :rows="3" v-model="reviewForm.content" placeholder="说说你的感受吧" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="reviewDialogVisible = false">取消</el-button>
          <el-button class="pm-btn-primary" :loading="reviewSubmitting" @click="submitReview">提交评价</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="logisticsDialogVisible" title="查看物流" width="520px" destroy-on-close align-center>
        <div v-if="logisticsOrder">
          <div class="info-row">订单号: {{ logisticsOrder.orderSn }}</div>
          <div class="info-row">物流单号: {{ logisticsOrder.trackingNo || '待补充' }}</div>
          <div class="info-row">承运方: {{ logisticsOrder.shipCompany || '模拟物流' }}</div>
          <el-timeline style="margin-top: 12px">
            <el-timeline-item
              v-for="(node, idx) in getLogisticsTimeline(logisticsOrder)"
              :key="idx"
              :timestamp="node.time"
              :type="idx === 0 ? 'primary' : 'info'"
            >
              {{ node.text }}
            </el-timeline-item>
          </el-timeline>
        </div>
      </el-dialog>

  <!-- 售后申请弹窗 -->
      <el-dialog v-model="afterSaleApplyDialogVisible" title="售后申请" width="520px" destroy-on-close align-center class="custom-dialog after-sale-dialog">
        <div v-if="afterSaleTarget" class="after-sale-target">
          <img v-if="afterSaleTarget.item?.productImg" :src="getImageUrl(afterSaleTarget.item.productImg)" />
          <div class="target-info">
            <div class="name">{{ afterSaleTarget.item?.productName }}</div>
            <div class="meta">订单号: {{ afterSaleTarget.order?.orderSn }}</div>
            <div class="meta">¥{{ Number(afterSaleTarget.item?.price || 0).toFixed(2) }} x {{ afterSaleTarget.item?.quantity }}</div>
          </div>
        </div>
        <el-form ref="afterSaleFormRef" :model="afterSaleForm" :rules="afterSaleRules" label-width="80px" label-position="left">
          <el-form-item label="类型" prop="type">
            <el-select v-model="afterSaleForm.type" placeholder="请选择售后类型">
              <el-option v-for="opt in afterSaleTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="原因" prop="reason">
            <el-select v-model="afterSaleForm.reason" placeholder="请选择原因">
              <el-option v-for="reason in afterSaleReasonOptions" :key="reason" :label="reason" :value="reason" />
            </el-select>
          </el-form-item>
          <el-form-item label="描述">
            <el-input type="textarea" :rows="3" v-model="afterSaleForm.description" placeholder="可选：补充问题描述" />
          </el-form-item>
          <el-form-item label="退款凭证">
            <el-upload
              action="/api/common/upload/image"
              name="file"
              :data="{ biz: 'common' }"
              :headers="uploadHeaders"
              :file-list="afterSaleEvidenceFileList"
              list-type="picture-card"
              :on-success="handleAfterSaleEvidenceSuccess"
              :on-remove="handleAfterSaleEvidenceRemove"
              :limit="3"
              :on-exceed="handleAfterSaleEvidenceExceed"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="afterSaleApplyDialogVisible = false">取消</el-button>
          <el-button class="pm-btn-primary" :loading="afterSaleSubmitting" @click="submitAfterSale">提交申请</el-button>
        </template>
      </el-dialog>

  <!-- 地址弹窗 -->
      <el-dialog v-model="addressDialogVisible" :title="addressDialogTitle" width="500px" destroy-on-close align-center class="custom-dialog address-dialog">
        <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-width="80px" label-position="left">
          <el-form-item label="收货人" prop="name">
            <el-input v-model="addressForm.name" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="addressForm.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="地区" prop="areaCodes">
            <el-cascader
              v-model="addressForm.areaCodes"
              :options="chinaAreaOptions"
              :props="{ ...chinaAreaCascaderProps, expandTrigger: 'hover' }"
              popper-class="address-cascader-popper"
              placeholder="选择省/市/区"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="详细地址" prop="detailAddress">
            <el-input type="textarea" :rows="2" v-model="addressForm.detailAddress" placeholder="街道、楼牌号等" />
          </el-form-item>
          <el-form-item label="设为默认">
            <el-switch v-model="addressForm.isDefault" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="addressDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="addressSaving" @click="handleSaveAddress">保存</el-button>
        </template>
      </el-dialog>

  <!-- 编辑资料弹窗 -->
      <el-dialog v-model="editDialogVisible" title="编辑资料" width="520px" destroy-on-close align-center class="custom-dialog">
        <el-form label-width="80px" label-position="left">
          <el-form-item label="头像">
            <el-upload
              ref="avatarUploadRef"
              action="/api/common/upload/image"
              name="file"
              :data="{ biz: 'userAvatar' }"
              :headers="uploadHeaders"
              :show-file-list="false"
              :limit="1"
              :on-success="handleAvatarSuccess"
              :on-change="handleAvatarChange"
              :on-exceed="handleAvatarExceed"
            >
              <div class="avatar-uploader">
                <img v-if="avatarPreview" :src="avatarPreview" class="avatar-preview" />
                <div v-else class="avatar-placeholder">
                  <el-icon><Plus /></el-icon>
                </div>
              </div>
            </el-upload>
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="editForm.nickname" maxlength="20" show-word-limit placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="性别">
            <el-select v-model="editForm.gender" placeholder="请选择">
              <el-option label="男" value="male" />
              <el-option label="女" value="female" />
              <el-option label="保密" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item label="出生年月">
            <el-date-picker
              v-model="editForm.birthDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="星座">
            <el-input :model-value="zodiacSign" readonly />
          </el-form-item>
          <el-form-item label="个性标签">
            <el-input v-model="editForm.tags" placeholder="用逗号分隔标签" />
          </el-form-item>
          <el-form-item label="电子邮箱">
            <el-input v-model="editForm.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="手机号码">
            <el-input v-model="editForm.phone" placeholder="请输入手机号" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="editSaving" @click="handleSaveProfile">保存</el-button>
        </template>
      </el-dialog>

  <!-- 头像放大预览 -->
      <el-dialog v-model="avatarZoomVisible" title="头像" width="420px" destroy-on-close align-center>
        <div class="avatar-zoom">
          <el-image :src="displayAvatar" fit="contain" />
        </div>
      </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated, onBeforeUnmount, watch, computed, reactive, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { formatTime } from '@/utils/format'
import { getImageUrl, getAvatarUrl } from '@/utils/image'
import { chinaAreaOptions, chinaAreaCascaderProps, chinaAreaNameByCode, chinaAreaCodesFromNames } from '@/utils/area'
import { ElMessage, ElMessageBox, genFileId } from 'element-plus'
import { 
  Star, StarFilled, UserFilled, Plus, Delete, Edit, VideoPlay, 
  Document, Goods, Location, ArrowRight, User, LocationInformation, MoreFilled
} from '@element-plus/icons-vue'
import HeartIcon from '@/components/HeartIcon.vue'
import AuthRequiredState from '@/components/AuthRequiredState.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const PROFILE_TABS = ['notes', 'collections', 'likes', 'orders', 'address']
const ORDER_SUB_TABS = ['pending_pay', 'pending_receive', 'pending_review', 'after_sale', 'all']
const AFTER_SALE_TABS = ['apply', 'processing', 'records']
const normalizeProfileTab = (tab) => (PROFILE_TABS.includes(tab) ? tab : 'notes')
const activeTab = ref(normalizeProfileTab(route.query.tab))
const tabBarReady = ref(false)
const displayAvatar = computed(() => getAvatarUrl(userStore.userInfo.avatar))

const resolveNoteCardImage = (note) => {
  if (!note) return getImageUrl('')
  return getImageUrl(note.coverThumb || note.coverImg)
}

const handleNoteCardImageError = (event, note) => {
  const target = event?.target
  if (!target) return
  const fallback = note?.coverImg ? getImageUrl(note.coverImg) : ''
  if (target.dataset.fallbackApplied !== '1' && fallback && target.src !== fallback) {
    target.dataset.fallbackApplied = '1'
    target.src = fallback
    return
  }
  target.src = getImageUrl('')
}

const avatarZoomVisible = ref(false)
const openAvatarZoom = () => {
  avatarZoomVisible.value = true
}

const uploadHeaders = computed(() => ({
  Authorization: userStore.token || localStorage.getItem('token') || ''
}))

// 关注统计
const followStats = reactive({
  followers: 0,
  following: 0
})

const loadFollowStats = async () => {
  if (!userStore.userInfo?.id) return
  try {
    const res = await request.get(`/follow/count/${userStore.userInfo.id}`)
    followStats.followers = res?.followers || 0
    followStats.following = res?.following || 0
  } catch (e) {
    followStats.followers = 0
    followStats.following = 0
  }
}

const goToFollow = (tab) => {
  router.push({ path: '/follows', query: { tab } })
}

const handleLogout = () => {
  ElMessageBox.confirm('确定退出登录吗?', '温馨提示', {
      confirmButtonText: '退出',
      customClass: 'pm-msgbox',
      confirmButtonClass: 'pm-btn-primary',
      cancelButtonText: '取消',
      type: 'warning'
  }).then(() => {
    userStore.logout()
    router.replace('/')
  }).catch(() => {})
}

// 编辑资料
const editDialogVisible = ref(false)
const editSaving = ref(false)
const editForm = reactive({
  nickname: '',
  avatar: '',
  gender: '',
  birthDate: '',
  tags: '',
  email: '',
  phone: ''
})
const avatarPreview = ref('')
const avatarUploadRef = ref(null)
let avatarObjectUrl = ''

const getZodiac = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return ''
  const m = date.getMonth() + 1
  const d = date.getDate()
  const zodiac = [
    { name: '摩羯座', start: [12, 22], end: [1, 19] },
    { name: '水瓶座', start: [1, 20], end: [2, 18] },
    { name: '双鱼座', start: [2, 19], end: [3, 20] },
    { name: '白羊座', start: [3, 21], end: [4, 19] },
    { name: '金牛座', start: [4, 20], end: [5, 20] },
    { name: '双子座', start: [5, 21], end: [6, 21] },
    { name: '巨蟹座', start: [6, 22], end: [7, 22] },
    { name: '狮子座', start: [7, 23], end: [8, 22] },
    { name: '处女座', start: [8, 23], end: [9, 22] },
    { name: '天秤座', start: [9, 23], end: [10, 23] },
    { name: '天蝎座', start: [10, 24], end: [11, 22] },
    { name: '射手座', start: [11, 23], end: [12, 21] }
  ]
  for (const z of zodiac) {
    const [sm, sd] = z.start
    const [em, ed] = z.end
    if (
      (m === sm && d >= sd) ||
      (m === em && d <= ed) ||
      (sm > em && ((m === sm && d >= sd) || (m === em && d <= ed)))
    ) {
      return z.name
    }
  }
  return ''
}

const zodiacSign = computed(() => getZodiac(editForm.birthDate))

const openEditProfile = () => {
  editForm.nickname = userStore.userInfo.nickname || ''
  editForm.avatar = userStore.userInfo.avatar || ''
  editForm.gender = userStore.userInfo.gender || ''
  editForm.birthDate = userStore.userInfo.birthDate || ''
  editForm.tags = userStore.userInfo.tags || ''
  editForm.email = userStore.userInfo.email || ''
  editForm.phone = userStore.userInfo.phone || ''
  if (avatarObjectUrl) {
    URL.revokeObjectURL(avatarObjectUrl)
    avatarObjectUrl = ''
  }
  avatarPreview.value = getAvatarUrl(editForm.avatar)
  editDialogVisible.value = true
}

const handleAvatarChange = (uploadFile) => {
  const raw = uploadFile?.raw
  if (!raw) return
  try {
    if (avatarObjectUrl) URL.revokeObjectURL(avatarObjectUrl)
    avatarObjectUrl = URL.createObjectURL(raw)
    avatarPreview.value = avatarObjectUrl
  } catch (e) {
    // 头像预览失败就算了，不影响后续上传
  }
}

const handleAvatarExceed = (files) => {
  // limit=1 时，重新选择头像会触发 exceed，这里实现“替换上传”
  if (!avatarUploadRef.value) return
  const file = files?.[0]
  if (!file) return
  avatarUploadRef.value.clearFiles()
  file.uid = genFileId()
  avatarUploadRef.value.handleStart(file)
  avatarUploadRef.value.submit()
}

const handleAvatarSuccess = (response) => {
  if (response?.code === 200) {
    if (avatarObjectUrl) {
      URL.revokeObjectURL(avatarObjectUrl)
      avatarObjectUrl = ''
    }
    editForm.avatar = response.data
    avatarPreview.value = getAvatarUrl(response.data)
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(response?.message || response?.msg || '头像上传失败')
  }
}

watch(editDialogVisible, (val) => {
  if (!val && avatarObjectUrl) {
    URL.revokeObjectURL(avatarObjectUrl)
    avatarObjectUrl = ''
  }
})

const handleSaveProfile = async () => {
  if (!editForm.nickname || !editForm.nickname.trim()) {
    ElMessage.warning('请输入昵称')
    return
  }
  editSaving.value = true
  try {
    await request.put('/user/info', {
      nickname: editForm.nickname.trim(),
      avatar: editForm.avatar || '',
      gender: editForm.gender || '',
      birthDate: editForm.birthDate || null,
      tags: editForm.tags || '',
      email: editForm.email || '',
      phone: editForm.phone || ''
    })
    await userStore.getUserInfo()
    editDialogVisible.value = false
    ElMessage.success('保存成功')
  } finally {
    editSaving.value = false
  }
}

// 我的笔记
const myNotes = ref([])
const loadingNotes = ref(false)
const collectedNotes = ref([])
const likedNotes = ref([])
const loadingCollected = ref(false)
const loadingLiked = ref(false)
const getMyNotes = async () => {
  loadingNotes.value = true
  try {
    const res = await request.get('/note/my')
    myNotes.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) {
    myNotes.value = []
  } finally {
    loadingNotes.value = false
  }
}

const getCollectedNotes = async () => {
  loadingCollected.value = true
  try {
    const res = await request.get('/note/my/collect', { params: { pageNum: 1, pageSize: 20 } })
    collectedNotes.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) {
    collectedNotes.value = []
  } finally {
    loadingCollected.value = false
  }
}

const getLikedNotes = async () => {
  loadingLiked.value = true
  try {
    const res = await request.get('/note/my/like', { params: { pageNum: 1, pageSize: 20 } })
    likedNotes.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) {
    likedNotes.value = []
  } finally {
    loadingLiked.value = false
  }
}

const canToggleMyNoteShelf = (note) => {
  const status = Number(note?.status)
  return status === 1 || status === 4
}

const canDeleteMyNote = (note) => {
  const status = Number(note?.status)
  return status !== 5 && status !== 6
}

const handleMyNoteCommand = (command, note) => {
  if (!note?.id) return
  if (command === 'shelf' || command === 'restore') {
    handleMyNoteShelf(note)
    return
  }
  if (command === 'delete') {
    handleMyNoteDelete(note)
  }
}

const handleMyNoteShelf = async (note) => {
  if (!canToggleMyNoteShelf(note)) {
    ElMessage.warning('当前状态不支持下架/恢复')
    return
  }
  const willOffShelf = Number(note.status) === 1
  const actionText = willOffShelf ? '下架' : '恢复上架'
  try {
    await ElMessageBox.confirm(`确定${actionText}该笔记吗？`, actionText, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: willOffShelf ? 'warning' : 'info'
    })
  } catch (_) {
    return
  }

  try {
    const offShelf = await request.post(`/note/my/${note.id}/shelf`)
    ElMessage.success(offShelf ? '笔记已下架' : '笔记已恢复上架')
    await getMyNotes()
  } catch (_) {
    // 这里交给请求拦截器处理
  }
}

const handleMyNoteDelete = async (note) => {
  if (!canDeleteMyNote(note)) {
    ElMessage.warning('笔记已删除')
    return
  }
  try {
    await ElMessageBox.confirm('删除后不可恢复，确定删除该笔记吗？', '删除笔记', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (_) {
    return
  }

  try {
    await request.post(`/note/my/${note.id}/delete`)
    myNotes.value = myNotes.value.filter(item => item.id !== note.id)
    ElMessage.success('笔记已删除')
  } catch (_) {
    // 这里交给请求拦截器处理
  }
}

const getNoteStatusText = (status) => {
  const map = {
    0: '审核中',
    1: '已发布',
    2: '管理员下架',
    3: '已拒绝',
    4: '已下架',
    5: '已删除',
    6: '已删除'
  }
  return map[status] || '未知'
}

// 我的订单
const orderList = ref([])
const loadingOrders = ref(false)
const orderSubTab = ref('all')
const afterSaleTab = ref('apply')
const afterSaleList = ref([])
const loadingAfterSale = ref(false)
const afterSaleUnreadCount = ref(0)
const SEEDED_NOTE_ORDER_IDS_KEY = 'petmeet:seeded-note-order-ids'
const seededNoteOrderIds = ref(new Set())
const afterSaleNoticeBizTypes = new Set(['after_sale', 'order_refund'])

const readSeededNoteOrderIds = () => {
  try {
    const raw = localStorage.getItem(SEEDED_NOTE_ORDER_IDS_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed
      .map((id) => Number(id))
      .filter((id) => Number.isInteger(id) && id > 0)
  } catch (e) {
    return []
  }
}

const syncSeededNoteOrderIds = () => {
  seededNoteOrderIds.value = new Set(readSeededNoteOrderIds())
  syncSelectedOrderIds()
}

const handleSeededOrderStorageChange = (event) => {
  if (!event || !event.key || event.key === SEEDED_NOTE_ORDER_IDS_KEY) {
    syncSeededNoteOrderIds()
  }
}

const normalizeOrderSubTab = (value) => (
  ORDER_SUB_TABS.includes(value) ? value : null
)

const normalizeAfterSaleTab = (value) => (
  AFTER_SALE_TABS.includes(value) ? value : null
)

const listUnreadAfterSaleNoticeIds = async () => {
  if (!userStore.token) {
    return []
  }
  try {
    const res = await request.get('/notification/list', {
      params: { pageNum: 1, pageSize: 500, unreadOnly: 1 }
    })
    const list = Array.isArray(res) ? res : (res?.records || [])
    return list
      .map((item) => ({
        id: Number(item?.id),
        bizType: String(item?.bizType || '').trim()
      }))
      .filter((item) => Number.isInteger(item.id) && item.id > 0 && afterSaleNoticeBizTypes.has(item.bizType))
      .map((item) => item.id)
  } catch (e) {
    return []
  }
}

const refreshAfterSaleUnreadCount = async () => {
  const ids = await listUnreadAfterSaleNoticeIds()
  afterSaleUnreadCount.value = ids.length
}

const markAfterSaleNoticesRead = async () => {
  const ids = await listUnreadAfterSaleNoticeIds()
  if (ids.length === 0) {
    afterSaleUnreadCount.value = 0
    return
  }
  await Promise.all(ids.map((id) => request.put(`/notification/${id}/read`).catch(() => null)))
  await refreshAfterSaleUnreadCount()
  userStore.fetchNotificationUnreadCount()
}

const handleProfileFocus = () => {
  syncSeededNoteOrderIds()
  if (activeTab.value === 'orders' && orderSubTab.value === 'after_sale') {
    markAfterSaleNoticesRead()
    return
  }
  refreshAfterSaleUnreadCount()
}

const isOrderSeedNotePublished = (order) => {
  const orderId = Number(order?.id)
  if (!Number.isInteger(orderId) || orderId <= 0) return false
  return seededNoteOrderIds.value.has(orderId)
}

const getOrders = async () => {
    loadingOrders.value = true
    try {
        const res = await request.get('/order/list', { params: { pageNum: 1, pageSize: 200 } })
        orderList.value = Array.isArray(res) ? res : (res?.records || [])
        userStore.fetchUnpaidOrderCount()
    } catch (e) {
        orderList.value = []
    } finally {
        syncSelectedOrderIds()
        loadingOrders.value = false
    }
}

const getAfterSales = async () => {
  loadingAfterSale.value = true
  try {
    const res = await request.get('/after-sale/my/list', { params: { pageNum: 1, pageSize: 200 } })
    afterSaleList.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) {
    afterSaleList.value = []
  } finally {
    loadingAfterSale.value = false
  }
}

const unpaidCount = computed(() => userStore.unpaidOrderCount || 0)

const pendingPayOrders = computed(() => (Array.isArray(orderList.value)
  ? orderList.value.filter(order => order.status === 0) : []))
const pendingReceiveOrders = computed(() => (Array.isArray(orderList.value)
  ? orderList.value.filter(order => [1, 2].includes(Number(order.status))) : []))
const pendingReviewOrders = computed(() => (Array.isArray(orderList.value)
  ? orderList.value.filter(order => Number(order.status) === 3 && !isOrderSeedNotePublished(order)) : []))
const allOrders = computed(() => (Array.isArray(orderList.value) ? orderList.value : []))
const selectedOrderIds = ref([])

const normalizeOrderId = (value) => {
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : null
}

const isOrderDeletable = (order) => [3, 4].includes(Number(order?.status))
const showOrderDeleteControls = computed(() => ['pending_review', 'all'].includes(orderSubTab.value))

const currentOrders = computed(() => {
  switch (orderSubTab.value) {
    case 'pending_pay':
      return pendingPayOrders.value
    case 'pending_receive':
      return pendingReceiveOrders.value
    case 'pending_review':
      return pendingReviewOrders.value
    case 'all':
    default:
      return allOrders.value
  }
})

const orderSort = ref('time_desc')
const sortedOrderList = computed(() => {
  const list = Array.isArray(currentOrders.value) ? [...currentOrders.value] : []
  const byTime = (a, b) => new Date(a.createTime).getTime() - new Date(b.createTime).getTime()
  const byAmount = (a, b) => Number(a.totalAmount || 0) - Number(b.totalAmount || 0)
  switch (orderSort.value) {
    case 'time_asc':
      return list.sort(byTime)
    case 'amount_desc':
      return list.sort((a, b) => byAmount(b, a))
    case 'amount_asc':
      return list.sort(byAmount)
    default:
      return list.sort((a, b) => byTime(b, a))
  }
})

const deletableOrderIdsInView = computed(() => {
  if (!showOrderDeleteControls.value) return []
  return sortedOrderList.value
    .map((order) => (isOrderDeletable(order) ? normalizeOrderId(order?.id) : null))
    .filter(Boolean)
})

const selectedOrderIdSet = computed(() => new Set(selectedOrderIds.value))

const allDeletableOrdersSelected = computed(() => {
  return deletableOrderIdsInView.value.length > 0
    && deletableOrderIdsInView.value.every((id) => selectedOrderIdSet.value.has(id))
})

const deletableOrderSelectionIndeterminate = computed(() => {
  if (!showOrderDeleteControls.value || deletableOrderIdsInView.value.length === 0) return false
  const selectedCount = deletableOrderIdsInView.value
    .filter((id) => selectedOrderIdSet.value.has(id))
    .length
  return selectedCount > 0 && selectedCount < deletableOrderIdsInView.value.length
})

const syncSelectedOrderIds = () => {
  if (!showOrderDeleteControls.value) {
    selectedOrderIds.value = []
    return
  }
  const allowed = new Set(deletableOrderIdsInView.value)
  selectedOrderIds.value = selectedOrderIds.value.filter((id) => allowed.has(id))
}

const nowTimestamp = ref(Date.now())
let countdownTimer = null

const getOrderExpireTime = (order) => {
  if (!order) return null
  if (order.payExpireTime) {
    const ts = new Date(order.payExpireTime).getTime()
    return Number.isNaN(ts) ? null : ts
  }
  const createTs = new Date(order.createTime).getTime()
  if (Number.isNaN(createTs)) return null
  return createTs + 30 * 60 * 1000
}

const formatPayCountdown = (order) => {
  const expireTs = getOrderExpireTime(order)
  if (!expireTs) return '00:00'
  const remain = Math.max(0, Math.floor((expireTs - nowTimestamp.value) / 1000))
  const mm = String(Math.floor(remain / 60)).padStart(2, '0')
  const ss = String(remain % 60).padStart(2, '0')
  return `${mm}:${ss}`
}

const normalizeOrderItems = (order) => (Array.isArray(order?.items) ? order.items : [])

const getOrderPreviewItems = (order) => normalizeOrderItems(order).slice(0, 3)

const getOrderMoreCount = (order) => {
  const items = normalizeOrderItems(order)
  return items.length > 3 ? items.length - 3 : 0
}

const getOrderTitle = (order) => {
  const items = normalizeOrderItems(order)
  if (items.length === 0) return '订单商品'
  const first = items[0] || {}
  const firstName = first.productName || '商品'

  if (items.length === 1) {
    const qty = Number(first.quantity || 0)
    return qty > 1 ? `${firstName} x${qty}` : firstName
  }

  const totalQty = items.reduce((sum, item) => sum + Number(item?.quantity || 0), 0)
  return totalQty > 0 ? `${firstName} x${totalQty}` : `${firstName} x${items.length}`
}

const isPendingReview = (order) => order?.status === 3 && Number(order?.reviewStatus ?? 0) === 0

const localizeStatusDesc = (statusDesc, fallbackText = '') => {
  const raw = String(statusDesc || '').trim()
  if (!raw) return fallbackText

  const key = raw
    .toLowerCase()
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')

  const map = {
    closed: '已关闭',
    cancelled: '已取消',
    canceled: '已取消',
    completed: '已完成',
    complete: '已完成',
    finished: '已完成',
    'pending payment': '待支付',
    unpaid: '待支付',
    'awaiting payment': '待支付',
    'pending shipment': '待发货',
    'to ship': '待发货',
    shipped: '待收货',
    'in transit': '待收货',
    'pending receipt': '待收货',
    'pending review': '待评价',
    refunding: '退款中',
    'refund processing': '退款中',
    'refund in progress': '退款中',
    applied: '申请中',
    submitted: '申请中',
    processing: '处理中',
    'in progress': '处理中',
    rejected: '已拒绝',
    denied: '已拒绝'
  }

  if (map[key]) return map[key]
  return /[A-Za-z]/.test(raw) ? (fallbackText || raw) : raw
}

const getOrderStatusText = (order) => {
  if (!order) return '未知'
  const map = { 0: '待支付', 1: '待发货', 2: '待收货', 3: isPendingReview(order) ? '待评价' : '已完成', 4: '已关闭', 5: '退款中' }
  return localizeStatusDesc(order.statusDesc, map[order.status] || '未知')
}


const getOrderStatusClass = (order) => {
  const base = `status-${order?.status ?? ''}`
  return isPendingReview(order) ? `${base} status-review` : base
}

const handlePay = async (row) => {
    router.push({ path: '/pay', query: { orderId: row.id, amount: row.totalAmount } })
}

const handleCancelOrder = async (row) => {
  if (!row?.id) return
  try {
    await ElMessageBox.confirm('确定取消该订单吗?', '提示', {
      type: 'warning',
      customClass: 'pm-msgbox',
      confirmButtonClass: 'pm-btn-primary',
    })
    await request.post(`/order/cancel/${row.id}`)
    ElMessage.success('订单已取消')
    orderDetailVisible.value = false
    orderDetail.value = null
    await getOrders()
    await userStore.fetchUnpaidOrderCount()
  } catch (e) {}
}

const handleConfirmReceipt = async (row) => {
  if (!row?.id) return
  try {
    await ElMessageBox.confirm('确认已收到商品吗?', '确认收货', {
      type: 'warning',
      customClass: 'pm-msgbox',
      confirmButtonClass: 'pm-btn-primary',
    })
    await request.post(`/order/confirm/${row.id}`)
    ElMessage.success('已确认收货')
    getOrders()
  } catch (e) {}
}

const isOrderSelected = (orderId) => {
  const id = normalizeOrderId(orderId)
  if (!id) return false
  return selectedOrderIdSet.value.has(id)
}

const handleToggleOrderSelected = (orderId, checked) => {
  const id = normalizeOrderId(orderId)
  if (!id) return
  if (!checked) {
    selectedOrderIds.value = selectedOrderIds.value.filter((item) => item !== id)
    return
  }
  if (!selectedOrderIdSet.value.has(id)) {
    selectedOrderIds.value = [...selectedOrderIds.value, id]
  }
}

const handleToggleSelectAllDeletable = (checked) => {
  if (!checked) {
    selectedOrderIds.value = []
    return
  }
  selectedOrderIds.value = [...deletableOrderIdsInView.value]
}

const handleDeleteOrder = async (order) => {
  const orderId = normalizeOrderId(order?.id)
  if (!orderId) return
  try {
    await ElMessageBox.confirm('确定删除该订单吗？删除后仅你自己不可见。', '删除订单', {
      type: 'warning',
      customClass: 'pm-msgbox',
      confirmButtonClass: 'pm-btn-danger',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await request.delete(`/order/${orderId}`)
    selectedOrderIds.value = selectedOrderIds.value.filter((id) => id !== orderId)
    ElMessage.success('订单已删除')
    getOrders()
  } catch (e) {}
}

const handleBatchDeleteOrders = async () => {
  const ids = selectedOrderIds.value
  if (!Array.isArray(ids) || ids.length === 0) {
    ElMessage.warning('请先选择订单')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除已选的 ${ids.length} 个订单吗？删除后仅你自己不可见。`, '批量删除订单', {
      type: 'warning',
      customClass: 'pm-msgbox',
      confirmButtonClass: 'pm-btn-danger',
      confirmButtonText: '批量删除',
      cancelButtonText: '取消'
    })
    await request.post('/order/batch-delete', ids)
    selectedOrderIds.value = []
    ElMessage.success('订单已批量删除')
    getOrders()
  } catch (e) {}
}

const logisticsDialogVisible = ref(false)
const logisticsOrder = ref(null)

const openLogisticsDialog = (order) => {
  logisticsOrder.value = order || null
  logisticsDialogVisible.value = true
}

const getLogisticsTimeline = (order) => {
  if (!order) return []
  const now = new Date()
  const oneDayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000)
  const shipAt = order.shipTime ? formatTime(order.shipTime) : formatTime(order.createTime)
  return [
    { time: formatTime(now), text: '包裹运输中，正在派送' },
    { time: formatTime(oneDayAgo), text: '到达成都转运中心' },
    { time: shipAt, text: '商家已发货' }
  ]
}

const extractOrderProductIds = (order) => {
  const ids = normalizeOrderItems(order)
    .map((item) => Number(item?.productId))
    .filter((id) => Number.isInteger(id) && id > 0)
  return Array.from(new Set(ids))
}

const getOrderProductsForSeedNote = async (order) => {
  const localItems = normalizeOrderItems(order)
  const localIds = extractOrderProductIds(order)
  if (localIds.length > 0) {
    return { ids: localIds, items: localItems }
  }

  const orderId = Number(order?.id)
  if (!Number.isInteger(orderId) || orderId <= 0) {
    return { ids: [], items: [] }
  }

  try {
    const detail = await request.get(`/order/detail/${orderId}`)
    const detailItems = normalizeOrderItems(detail)
    return {
      ids: extractOrderProductIds(detail),
      items: detailItems
    }
  } catch (e) {
    return { ids: [], items: [] }
  }
}

const goPublishFromOrder = async (order) => {
  const { ids, items } = await getOrderProductsForSeedNote(order)
  if (ids.length === 0) {
    ElMessage.warning('当前订单没有可关联商品')
    return
  }

  const firstName = String(
    items.find((item) => Number(item?.productId) === ids[0])?.productName || ''
  ).trim()
  const tags = ['开箱实测', '已购反馈']
  if (firstName) {
    tags.unshift(firstName.slice(0, 12))
  }

  router.push({
    path: '/publish',
    query: {
      orderId: order.id,
      productId: ids[0],
      productIds: ids.join(','),
      tags: tags.join(',')
    }
  })
}

// 订单详情
const orderDetailVisible = ref(false)
const orderDetailLoading = ref(false)
const orderDetail = ref(null)

const refreshOrdersAfterExternalChange = async () => {
  activeTab.value = 'orders'
  orderSubTab.value = normalizeOrderSubTab(route.query.orderSubTab) || 'all'
  orderDetailVisible.value = false
  orderDetail.value = null
  await getOrders()
  await userStore.fetchUnpaidOrderCount()
}

const openOrderDetail = async (row) => {
  if (!row?.id) return
  orderDetailVisible.value = true
  orderDetailLoading.value = true
  try {
    const res = await request.get(`/order/detail/${row.id}`)
    orderDetail.value = res
  } finally {
    orderDetailLoading.value = false
  }
}

const parseRouteOrderId = (value) => {
  const n = Number(value)
  if (!Number.isInteger(n) || n <= 0) return null
  return n
}

const openOrderDetailFromRoute = async (routeOrderId) => {
  const orderId = parseRouteOrderId(routeOrderId)
  if (!orderId) return
  activeTab.value = 'orders'
  if (orderSubTab.value === 'after_sale') {
    orderSubTab.value = 'all'
  }
  await getOrders()
  const targetOrder = (Array.isArray(orderList.value) ? orderList.value : [])
    .find((item) => Number(item?.id) === orderId)
  if (targetOrder) {
    await openOrderDetail(targetOrder)
    return
  }
  await openOrderDetail({ id: orderId })
}

// 订单评价
const reviewDialogVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewFormRef = ref(null)
const reviewForm = reactive({
  orderId: null,
  score: 5,
  content: ''
})

const reviewRules = {
  score: [{ required: true, message: '请评分', trigger: 'change' }]
}

const openReviewDialog = (order) => {
  if (!order?.id) return
  reviewForm.orderId = order.id
  reviewForm.score = 5
  reviewForm.content = ''
  reviewDialogVisible.value = true
}

const submitReview = async () => {
  if (!reviewFormRef.value) return
  await reviewFormRef.value.validate(async (valid) => {
    if (!valid) return
    reviewSubmitting.value = true
    try {
      await request.post(`/order/review/${reviewForm.orderId}`, {
        score: reviewForm.score,
        content: reviewForm.content
      })
      ElMessage.success('评价成功')
      reviewDialogVisible.value = false
      getOrders()
    } finally {
      reviewSubmitting.value = false
    }
  })
}

// 售后
const afterSaleApplyDialogVisible = ref(false)
const afterSaleSubmitting = ref(false)
const afterSaleFormRef = ref(null)
const afterSaleTarget = ref(null)
const afterSaleForm = reactive({
  orderId: null,
  orderItemId: null,
  type: 0,
  reason: '',
  description: '',
  evidenceImages: []
})
const afterSaleEvidenceFileList = ref([])

const afterSaleRules = {
  type: [{ required: true, message: '请选择售后类型', trigger: 'change' }],
  reason: [{ required: true, message: '请选择原因', trigger: 'change' }]
}

const afterSaleTypeOptions = [
  { label: '仅退款', value: 0 },
  { label: '退货退款', value: 1 },
  { label: '换货', value: 2 }
]

const afterSaleReasonOptions = [
  '不想要了',
  '质量问题',
  '商品破损',
  '少件/漏发',
  '买错/拍错',
  '其他'
]

const afterSaleActiveMap = computed(() => {
  const map = new Map()
  const list = Array.isArray(afterSaleList.value) ? [...afterSaleList.value] : []
  list.sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime())
  list.forEach(req => {
    const key = Number(req.orderItemId)
    if ([0, 1].includes(Number(req.status)) && !Number.isNaN(key)) {
      if (!map.has(key)) {
        map.set(key, req)
      }
    }
  })
  return map
})

const afterSaleApplyItems = computed(() => {
  const list = []
  const orders = Array.isArray(orderList.value) ? orderList.value : []
  orders.forEach(order => {
    if (![1, 2, 3].includes(Number(order.status))) return
    const items = normalizeOrderItems(order)
    items.forEach(item => {
      const key = Number(item.id)
      list.push({
        order,
        item,
        activeRequest: Number.isNaN(key) ? null : afterSaleActiveMap.value.get(key)
      })
    })
  })
  return list
})

const afterSaleProcessingList = computed(() => {
  const list = Array.isArray(afterSaleList.value) ? afterSaleList.value : []
  return list.filter(req => [0, 1].includes(Number(req.status)))
})

const afterSaleRecordList = computed(() => {
  const list = Array.isArray(afterSaleList.value) ? [...afterSaleList.value] : []
  return list.sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime())
})

const getAfterSaleStatusText = (status, statusDesc = '') => {
  const map = { 0: '申请中', 1: '处理中', 2: '已完成', 3: '已拒绝', 4: '已取消' }
  return localizeStatusDesc(statusDesc, map[status] || '处理中')
}

const getAfterSaleTypeText = (type) => {
  const map = { 0: '仅退款', 1: '退货退款', 2: '换货' }
  return map[type] || '售后'
}

const openAfterSaleDialog = (row) => {
  if (!row?.order?.id || !row?.item?.id) return
  afterSaleTarget.value = row
  afterSaleForm.orderId = row.order.id
  afterSaleForm.orderItemId = row.item.id
  afterSaleForm.type = 0
  afterSaleForm.reason = ''
  afterSaleForm.description = ''
  afterSaleForm.evidenceImages = []
  afterSaleEvidenceFileList.value = []
  afterSaleApplyDialogVisible.value = true
}

const syncAfterSaleEvidence = (files) => {
  const list = Array.isArray(files) ? files : []
  afterSaleForm.evidenceImages = list
    .map((file) => file.rawUrl || file.response?.data || '')
    .filter(Boolean)
}

const handleAfterSaleEvidenceSuccess = (response, uploadFile, uploadFiles) => {
  if (response?.code !== 200 || !response?.data) {
    ElMessage.error(response?.message || response?.msg || '凭证上传失败')
    return
  }
  uploadFile.rawUrl = response.data
  uploadFile.url = getImageUrl(response.data)
  syncAfterSaleEvidence(uploadFiles)
}

const handleAfterSaleEvidenceRemove = (uploadFile, uploadFiles) => {
  syncAfterSaleEvidence(uploadFiles)
}

const handleAfterSaleEvidenceExceed = () => {
  ElMessage.warning('最多上传 3 张退款凭证')
}

const submitAfterSale = async () => {
  if (!afterSaleFormRef.value) return
  await afterSaleFormRef.value.validate(async (valid) => {
    if (!valid) return
    afterSaleSubmitting.value = true
    try {
      await request.post('/after-sale/apply', { ...afterSaleForm })
      ElMessage.success('售后申请已提交')
      afterSaleApplyDialogVisible.value = false
      getAfterSales()
      refreshAfterSaleUnreadCount()
    } finally {
      afterSaleSubmitting.value = false
    }
  })
}

const handleCancelAfterSale = async (row) => {
  if (!row?.id) return
  try {
    await ElMessageBox.confirm('确定撤销该售后申请吗?', '提示', {
      type: 'warning',
      customClass: 'pm-msgbox',
      confirmButtonClass: 'pm-btn-primary',
    })
    await request.post(`/after-sale/cancel/${row.id}`)
    ElMessage.success('已撤销')
    getAfterSales()
  } catch (e) {}
}

const handleCompleteAfterSale = async (row) => {
  if (!row?.id) return
  try {
    await ElMessageBox.confirm('确认该售后已完成?', '确认完成', {
      type: 'warning',
      customClass: 'pm-msgbox',
      confirmButtonClass: 'pm-btn-primary',
    })
    await request.post(`/after-sale/complete/${row.id}`)
    ElMessage.success('已完成')
    getAfterSales()
  } catch (e) {}
}

const handleDeleteAfterSale = async (row) => {
  if (!row?.id) return
  try {
    await ElMessageBox.confirm(
      '删除后该售后申请仅你自己不可见，确定删除吗？',
      '删除售后申请',
      {
        type: 'warning',
        customClass: 'pm-msgbox',
        confirmButtonClass: 'pm-btn-danger',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
    await request.delete(`/after-sale/${row.id}`)
    ElMessage.success('售后申请已删除')
    await getAfterSales()
    await refreshAfterSaleUnreadCount()
  } catch (e) {}
}

const switchToAfterSaleRecords = () => {
  afterSaleTab.value = 'records'
}

watch(orderSubTab, (val) => {
  syncSelectedOrderIds()
  if (!userStore.isLoggedIn) return
  if (val === 'after_sale') {
    const targetAfterSaleTab = normalizeAfterSaleTab(route.query.afterSaleTab)
    if (targetAfterSaleTab) {
      afterSaleTab.value = targetAfterSaleTab
    }
    getAfterSales()
    markAfterSaleNoticesRead()
  }
})

// 地址管理
const addressList = ref([])
const loadingAddress = ref(false)
const getAddressList = async () => {
    loadingAddress.value = true
    try {
        const res = await request.get('/user/address/list')
        addressList.value = res || []
    } finally {
        loadingAddress.value = false
    }
}

const handleDeleteAddress = async (id) => {
    try {
        await ElMessageBox.confirm('确定删除该地址吗?', '提示', {
            type: 'warning',
            customClass: 'pm-msgbox',
            confirmButtonClass: 'pm-btn-primary',
        })
        await request.delete(`/user/address/${id}`)
        ElMessage.success('已删除')
        getAddressList()
    } catch (e) {}
}

// 地址表单
const addressDialogVisible = ref(false)
const addressSaving = ref(false)
const addressMode = ref('create') 
const addressFormRef = ref(null)
const addressForm = reactive({
  id: null,
  name: '',
  phone: '',
  areaCodes: [],
  province: '',
  city: '',
  region: '',
  detailAddress: '',
  isDefault: 0
})

const addressDialogTitle = computed(() => (addressMode.value === 'create' ? '新增地址' : '编辑地址'))

const resetAddressForm = () => {
  Object.assign(addressForm, {
    id: null, name: '', phone: '', areaCodes: [], 
    province: '', city: '', region: '', detailAddress: '', isDefault: 0
  })
}

const openAddressDialog = (row) => {
  if (row) {
    addressMode.value = 'edit'
    Object.assign(addressForm, {
      id: row.id,
      name: row.name || '',
      phone: row.phone || '',
      areaCodes: chinaAreaCodesFromNames(row.province, row.city, row.region),
      province: row.province || '',
      city: row.city || '',
      region: row.region || '',
      detailAddress: row.detailAddress || '',
      isDefault: row.isDefault ?? 0
    })
  } else {
    addressMode.value = 'create'
    resetAddressForm()
  }
  addressDialogVisible.value = true
}

const addressRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '格式不正确', trigger: 'blur' }
  ],
  areaCodes: [{
    validator: (rule, value, callback) => {
      if (!Array.isArray(value) || value.length !== 3) {
        callback(new Error('请选择地区'))
        return
      }
      callback()
    },
    trigger: 'change'
  }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

watch(() => addressForm.areaCodes, (val) => {
  if (Array.isArray(val) && val.length === 3) {
    addressForm.province = chinaAreaNameByCode(val[0]) || ''
    addressForm.city = chinaAreaNameByCode(val[1]) || ''
    addressForm.region = chinaAreaNameByCode(val[2]) || ''
  }
}, { deep: true })

const handleSaveAddress = async () => {
  if (!addressFormRef.value) return
  await addressFormRef.value.validate(async (valid) => {
    if (!valid) return
    addressSaving.value = true
    try {
      const payload = { ...addressForm }
      if (addressMode.value === 'create') {
        await request.post('/user/address', payload)
        ElMessage.success('新增成功')
      } else {
        await request.put('/user/address', payload)
        ElMessage.success('保存成功')
      }
      addressDialogVisible.value = false
      getAddressList()
    } finally {
      addressSaving.value = false
    }
  })
}

const resetProfileData = () => {
    myNotes.value = []
    collectedNotes.value = []
    likedNotes.value = []
    orderList.value = []
    afterSaleList.value = []
    addressList.value = []
    selectedOrderIds.value = []
    afterSaleUnreadCount.value = 0
    followStats.followers = 0
    followStats.following = 0
    loadingNotes.value = false
    loadingCollected.value = false
    loadingLiked.value = false
    loadingOrders.value = false
    loadingAfterSale.value = false
    loadingAddress.value = false
}

// 切换标签页时，按需加载对应数据
const loadTab = (tab) => {
    if (!userStore.isLoggedIn) {
        resetProfileData()
        return
    }
    if (tab === 'notes') getMyNotes()
    else if (tab === 'collections') getCollectedNotes()
    else if (tab === 'likes') getLikedNotes()
    else if (tab === 'orders') {
        syncSeededNoteOrderIds()
        getOrders()
        getAfterSales()
        if (orderSubTab.value === 'after_sale') {
          markAfterSaleNoticesRead()
        } else {
          refreshAfterSaleUnreadCount()
        }
    }
    else if (tab === 'address') getAddressList()
}

const handleTabClick = (pane) => {
  const tab = pane?.paneName
  if (!PROFILE_TABS.includes(tab)) return
  loadTab(tab)
}

watch(activeTab, (val) => {
    if (!PROFILE_TABS.includes(val)) return
    if (route.query.tab === val) return
    router.replace({
      name: 'Profile',
      query: { ...route.query, tab: val }
    }).catch(() => {})
})

watch(() => route.query.tab, (tab) => {
    if (PROFILE_TABS.includes(tab) && tab !== activeTab.value) {
        activeTab.value = tab
        if (tab === 'orders') {
          const subTab = normalizeOrderSubTab(route.query.orderSubTab)
          if (subTab) {
            orderSubTab.value = subTab
          }
          if (orderSubTab.value === 'after_sale') {
            const targetAfterSaleTab = normalizeAfterSaleTab(route.query.afterSaleTab)
            if (targetAfterSaleTab) {
              afterSaleTab.value = targetAfterSaleTab
            }
          }
        }
        loadTab(tab)
    }
})

watch(() => route.query.orderSubTab, (tab) => {
    const normalized = normalizeOrderSubTab(tab)
    if (!normalized) return
    if (activeTab.value !== 'orders') return
    if (orderSubTab.value === normalized) return
    orderSubTab.value = normalized
})

watch(() => route.query.afterSaleTab, (tab) => {
    const normalized = normalizeAfterSaleTab(tab)
    if (!normalized) return
    if (activeTab.value !== 'orders' || orderSubTab.value !== 'after_sale') return
    if (afterSaleTab.value === normalized) return
    afterSaleTab.value = normalized
})

watch(() => route.query.orderId, (orderId) => {
    if (!userStore.isLoggedIn) return
    if (!orderId) return
    openOrderDetailFromRoute(orderId)
})

watch(() => route.query.refresh, async (refresh) => {
    if (!userStore.isLoggedIn) return
    if (!refresh) return
    await refreshOrdersAfterExternalChange()
})

watch(() => userStore.token, async (token) => {
    if (!token) {
      resetProfileData()
      return
    }
    if (!userStore.userInfo?.id) {
      try {
        await userStore.getUserInfo()
      } catch (e) {}
    }
    userStore.fetchUnpaidOrderCount()
    refreshAfterSaleUnreadCount()
    loadTab(activeTab.value)
    loadFollowStats()
})

let hasActivatedOnce = false

onActivated(() => {
    if (!hasActivatedOnce) {
      hasActivatedOnce = true
      return
    }
    loadTab(activeTab.value)
})

onMounted(async () => {
    syncSeededNoteOrderIds()
    window.addEventListener('storage', handleSeededOrderStorageChange)
    window.addEventListener('focus', handleProfileFocus)
    countdownTimer = window.setInterval(() => {
      nowTimestamp.value = Date.now()
    }, 1000)
    if (userStore.token) {
        userStore.fetchUnpaidOrderCount()
        refreshAfterSaleUnreadCount()
    }
    if (activeTab.value === 'orders') {
      const subTab = normalizeOrderSubTab(route.query.orderSubTab)
      if (subTab) {
        orderSubTab.value = subTab
      }
      if (orderSubTab.value === 'after_sale') {
        const targetAfterSaleTab = normalizeAfterSaleTab(route.query.afterSaleTab)
        if (targetAfterSaleTab) {
          afterSaleTab.value = targetAfterSaleTab
        }
      }
    }
    await nextTick()
    requestAnimationFrame(() => {
      tabBarReady.value = true
    })
    loadTab(activeTab.value)
    loadFollowStats()
    if (userStore.isLoggedIn && route.query?.orderId) {
      await openOrderDetailFromRoute(route.query.orderId)
    }
})

onBeforeUnmount(() => {
  window.removeEventListener('storage', handleSeededOrderStorageChange)
  window.removeEventListener('focus', handleProfileFocus)
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
    countdownTimer = null
  }
})
</script>

<style scoped lang="scss">
/* 全局布局 */
.profile-page {
  --accent: #ff5c5c;
  --danger-accent: rgb(255, 122, 138);
  --ink: #111827;
  --muted: #6b7280;
  --el-color-primary: rgb(255, 92, 92);
  --el-color-primary-light-3: rgba(255, 92, 92, 0.75);
  --el-color-primary-light-5: rgba(255, 92, 92, 0.45);
  --el-color-primary-light-7: rgba(255, 92, 92, 0.25);
  --el-color-primary-light-9: rgba(255, 92, 92, 0.12);
  --el-color-primary-dark-2: #ff4757;
  font-family: "Manrope", "Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif;
  background: #ffffff;
  min-height: 100vh;
  padding-bottom: 60px;
}

/* 头部和横幅 */
.profile-header {
  position: relative;
  background: #fff;
  margin-bottom: 12px;
  padding-bottom: 4px;
}

.profile-banner {
  height: 128px;
  background: #f5f5f5;
  position: relative;
  overflow: hidden;
  
  .banner-gradient {
    position: absolute;
    inset: 0;
    background: linear-gradient(to bottom, rgba(255,255,255,0.65), rgba(255,255,255,0));
  }
}

.user-card {
  max-width: 960px;
  margin: 0 auto;
  position: relative;
  padding: 0 20px 18px;
  margin-top: -72px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.avatar-wrapper {
  padding: 6px;
  background: #fff;
  border-radius: 50%;
  border: 1px solid #ececec;
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
  cursor: pointer;
  transition: transform 0.15s ease;

  &:hover {
    transform: translateY(-1px);
  }
}

.avatar-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.change-avatar-btn {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
  font-weight: 600;

  &:hover {
    background: #ff7878;
    border-color: #ff7878;
    color: #fff;
  }
}

.avatar-zoom {
  display: flex;
  justify-content: center;
  padding: 6px 0 10px;

  :deep(img) {
    max-width: 360px;
    max-height: 360px;
    border-radius: 14px;
    background: #fff;
  }
}



:deep(.address-cascader-popper) {
  --el-color-primary: rgb(255, 92, 92);
  --el-color-primary-light-3: rgba(255, 92, 92, 0.75);
  --el-color-primary-light-5: rgba(255, 92, 92, 0.45);
  --el-color-primary-light-7: rgba(255, 92, 92, 0.25);
  --el-color-primary-light-9: rgba(255, 92, 92, 0.12);
  --el-color-primary-dark-2: #ff4757;
}



.user-info {
  margin-top: 12px;
  
  .nickname {
    margin: 0;
    font-size: 28px;
    font-weight: 700;
    color: var(--ink);
  }
  
  .uid {
    margin: 6px 0 0;
    font-size: 12px;
    color: #9ca3af;
    font-family: "JetBrains Mono", "SF Mono", monospace;
  }

}

.stats-row {
  display: flex;
  align-items: center;
  gap: 18px;
  margin: 14px 0 10px;
  
  .stat-item {
    cursor: pointer;
    text-align: center;
    transition: transform 0.2s;
    
    &:hover {
      transform: translateY(-1px);
      .value { color: var(--accent); }
    }
    
    .value {
      display: block;
      font-size: 18px;
      font-weight: 700;
      color: var(--ink);
    }
    .label {
      font-size: 13px;
      color: #9ca3af;
    }
  }
  
  .divider {
    width: 1px;
    height: 18px;
    background: #e5e7eb;
    border-radius: 1px;
  }
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
  
  .el-button {
    font-weight: 500;
    min-width: 100px;
  }

  .edit-btn {
    border-color: #ffd1d1;
    color: var(--accent);
    background: #fff;
  }

  .logout-btn {
    border-color: #f3f4f6;
  }
}

.avatar-uploader {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: #f5f6f7;
  border: 1px dashed #d7dadd;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: #ff6b6b;
    background: #fff5f5;
  }
}

.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-placeholder {
  color: #999;
  font-size: 20px;
}

@media (max-width: 720px) {
  .user-card {
    padding: 0 16px 16px;
  }

  .stats-row {
    gap: 12px;
  }
}

/* 内容区 */
.profile-body {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 20px;
  padding-top: 8px;
}

.content-wrapper {
  background: #fff;
  border-radius: 16px;
  padding: 0 8px 24px;
}

.custom-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 18px;
  }

  :deep(.el-tabs__nav-wrap) {
    display: flex;
    justify-content: center;
  }
  
  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }
  
  :deep(.el-tabs__item) {
    font-size: 16px;
    height: 46px;
    color: #9ca3af;
    
    &.is-active {
      color: #111827;
      font-weight: 600;
      .tab-label { transform: scale(1.05); }
    }
  }
  
  :deep(.el-tabs__active-bar) {
    background-color: var(--accent); 
    height: 2px;
    border-radius: 3px;
  }
}

.custom-tabs--static-bar {
  :deep(.el-tabs__active-bar) {
    transition: none !important;
  }

  .tab-label {
    transition: none;
  }
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s;
  
  .el-icon { font-size: 20px; margin-top: -2px; }
  .custom-badge { margin-right: 6px; }
}

/* 笔记宫格 */
.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.note-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #f3f4f6;
  box-shadow: none;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 10px 24px rgba(0,0,0,0.08);
    border-color: transparent;
  }
  
  .card-cover {
    height: 200px; /* Taller covers */
    background: #eaeaea;
    position: relative;
    img { width: 100%; height: 100%; object-fit: cover; }
    
    .type-badge {
      position: absolute;
      top: 46px; right: 10px;
      width: 28px; height: 28px;
      background: rgba(0,0,0,0.5);
      border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      color: #fff;
    }

    .note-action-btn {
      position: absolute;
      top: 10px;
      right: 10px;
      width: 28px;
      height: 28px;
      padding: 0;
      z-index: 3;
      border: none;
      color: #fff;
      background: rgba(0,0,0,0.5);
    }

    .note-action-btn:hover,
    .note-action-btn:focus {
      color: #fff;
      background: rgba(0,0,0,0.65);
    }

    .status-tag {
        position: absolute;
        top: 10px; left: 10px;
        font-size: 12px;
        padding: 2px 8px;
        border-radius: 4px;
        color: #fff;
        background: rgba(0,0,0,0.6);
        
        &.status-0 { background: #e6a23c; } /* Auditing */
        &.status-1 { display: none; } /* Published - hide to be clean */
        &.status-2 { background: #909399; }
        &.status-3 { background: #f56c6c; }
        &.status-4 { background: #e6a23c; }
        &.status-5 { background: #f56c6c; }
        &.status-6 { background: #f56c6c; }
    }
  }
  
  .card-info {
    padding: 12px;
    
    .card-title {
       font-size: 15px;
       font-weight: 500;
       color: #333;
       margin-bottom: 8px;
       display: -webkit-box;
       -webkit-line-clamp: 2;
       -webkit-box-orient: vertical;
       overflow: hidden;
       text-overflow: ellipsis;
       line-height: 1.4;
    }
    
    .card-meta {
      display: flex;
      justify-content: space-between;
      color: #999;
      font-size: 12px;
      
      .likes {
        display: flex; align-items: center; gap: 4px;
      }
    }
  }
}

/* 订单列表 */
.order-sub-tabs {
  margin-bottom: 10px;

  :deep(.el-tabs__header) {
    margin-bottom: 6px;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__item) {
    font-size: 14px;
    color: #9ca3af;
  }

  :deep(.el-tabs__item.is-active) {
    color: #111827;
    font-weight: 600;
  }

  :deep(.el-tabs__active-bar) {
    background-color: var(--accent);
  }
}

.order-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;

  .count {
    padding: 0 8px;
    height: 18px;
    line-height: 18px;
    border-radius: 999px;
    background: rgba(255, 92, 92, 0.15);
    color: var(--accent);
    font-size: 12px;
    font-weight: 600;
  }
}

.pm-btn-primary {
  background-color: var(--accent);
  border-color: var(--accent);
  color: #fff;

  &:hover,
  &:focus {
    background-color: #ff7878;
    border-color: #ff7878;
    color: #fff;
  }
}

.pm-btn-ghost {
  background-color: #fff;
  border-color: #ffd1d1;
  color: var(--accent);

  &:hover,
  &:focus {
    background-color: #fff5f5;
    border-color: #ffb3b3;
    color: #ff6b81;
  }
}

.pm-btn-delete-ghost {
  background-color: #fff;
  border-color: rgba(255, 122, 138, 0.45);
  color: var(--danger-accent);

  &:hover,
  &:focus {
    background-color: rgba(255, 122, 138, 0.1);
    border-color: rgba(255, 122, 138, 0.75);
    color: var(--danger-accent);
  }
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
  color: #9ca3af;
  font-size: 12px;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
  }

  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
  }

  :deep(.el-select) {
    width: 150px;
  }
}

.order-delete-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.delete-selected-count {
  color: #6b7280;
}

.delete-selection {
  --el-color-primary: rgb(255, 122, 138);
  --el-color-primary-light-3: rgba(255, 122, 138, 0.75);
  --el-color-primary-light-5: rgba(255, 122, 138, 0.45);
  --el-color-primary-light-7: rgba(255, 122, 138, 0.25);
  --el-color-primary-light-9: rgba(255, 122, 138, 0.12);
  --el-checkbox-checked-bg-color: rgb(255, 122, 138);
  --el-checkbox-checked-input-border-color: rgb(255, 122, 138);
  --el-checkbox-input-border-color-hover: rgb(255, 122, 138);
}

.order-sort-option {
  display: flex;
  align-items: center;
  gap: 10px;
}

.order-sort-option .prefix {
  width: 70px;
  color: #6b7280;
  text-align: left;
}

.order-sort-option .value {
  color: #111827;
}

.order-item {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: border-color 0.2s;
  
  &:hover {
    border-color: #ffb8b8;
  }
  
  .order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    padding-bottom: 12px;
    border-bottom: none;

    .order-header-left {
      display: flex;
      align-items: center;
      gap: 10px;
      min-width: 0;
    }
    
    .order-sn { font-size: 13px; color: #999; }
    .order-status {
       font-weight: 600;
       font-size: 14px;
       &.status-0 { color: #f56c6c; } /* Unpaid */
       &.status-1 { color: #e6a23c; }
       &.status-2 { color: #ff6b81; } /* Shipped - Changed from Blue to Theme Pink */
       &.status-3 { color: #67c23a; }
       &.status-4 { color: #909399; }
       &.status-5 { color: #f56c6c; }
       &.status-review { color: #ff7aa2; }
    }
  }
  
  .order-body {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 14px;
    flex-wrap: wrap;

    .order-main {
      flex: 1;
      min-width: 0;
    }

    .order-products {
      display: flex;
      align-items: center;
      gap: 12px;
      min-width: 0;
    }

    .product-thumbs {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-shrink: 0;

      .thumb {
        width: 46px;
        height: 46px;
        border-radius: 10px;
        overflow: hidden;
        border: 1px solid #f0f0f0;
        background: #f8fafc;
        position: relative;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
          display: block;
        }

        .thumb-placeholder {
          width: 100%;
          height: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #c0c4cc;
          font-size: 18px;
        }

        .thumb-qty {
          position: absolute;
          right: 4px;
          bottom: 4px;
          padding: 0 6px;
          height: 18px;
          line-height: 18px;
          border-radius: 9px;
          font-size: 11px;
          color: #fff;
          background: rgba(0, 0, 0, 0.55);
          backdrop-filter: blur(2px);
        }
      }

      .thumb-more {
        width: 46px;
        height: 46px;
        border-radius: 10px;
        border: 1px solid #e5e7eb;
        background: #fafafa;
        color: #6b7280;
        font-size: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }
    }

    .product-brief {
      min-width: 0;

      .product-title {
        font-size: 14px;
        font-weight: 600;
        color: #111827;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
    
    .order-meta {
      p { margin: 0; }
      .price { font-size: 18px; font-weight: 700; color: #333; font-family: 'DIN Alternate', sans-serif; }
      .time { margin-top: 4px; font-size: 12px; color: #aaa; }
      .pay-countdown { margin-top: 4px; font-size: 12px; color: #f56c6c; font-weight: 600; }
    }

    .refund-warning {
      margin-top: 8px;
      padding: 6px 10px;
      border-radius: 8px;
      font-size: 12px;
      color: #b91c1c;
      background: #fef2f2;
      border: 1px solid #fecaca;
    }
    
    .order-actions {
      display: flex; gap: 10px;
      align-items: center;
      flex-shrink: 0;
      flex-wrap: wrap;
      justify-content: flex-end;

      :deep(.el-button) {
        margin-left: 0;
        height: 30px;
        line-height: 1;
        display: inline-flex;
        align-items: center;
        justify-content: center;
      }

      :deep(.el-button + .el-button) {
        margin-left: 0;
      }

      :deep(.el-button--link.el-button--small) {
        padding: 0 8px;
      }
      
      .el-button--link {
        color: #606266;
        &:hover {
           color: #ff6b81;
        }
      }
    }
  }
}

.after-sale-panel {
  background: #fff;
  border: 1px solid #f3f4f6;
  border-radius: 12px;
  padding: 12px;
}

.after-sale-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 10px;
  }

  :deep(.el-tabs__active-bar) {
    background-color: var(--accent);
  }
}

.after-sale-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.after-sale-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  background: #fff;
}

.after-sale-card .card-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.after-sale-card img {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  object-fit: cover;
}

.img-placeholder {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  border: 1px dashed #e5e7eb;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.after-sale-card .info {
  min-width: 0;
}

.after-sale-card .info .name {
  font-weight: 600;
  color: #111827;
}

.after-sale-card .info .meta {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.evidence-row {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}

.evidence-thumb {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.after-sale-card .card-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.after-sale-status {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--accent);
  background: #fff5f6;
  border: 1px solid #ffd1d1;

  &.status-2 {
    background: #ecfdf3;
    color: #16a34a;
    border-color: #bbf7d0;
  }
  &.status-3 {
    background: #fef2f2;
    color: #ef4444;
    border-color: #fecaca;
  }
  &.status-4 {
    background: #f3f4f6;
    color: #6b7280;
    border-color: #e5e7eb;
  }
}

.after-sale-actions {
  display: flex;
  gap: 8px;
}

.after-sale-target {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  margin-bottom: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  background: #fafafa;
}

.after-sale-target img {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  border: 1px solid #eee;
  object-fit: cover;
}

.after-sale-target .name {
  font-weight: 600;
  color: #111827;
}

.add-addr-btn {
  background-color: var(--accent);
  border-color: var(--accent);
  color: #fff;
  padding: 10px 24px;
  font-weight: 600;
  letter-spacing: 0.5px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  
  &:hover {
    background-color: #ff7878;
    border-color: #ff7878;
    color: #fff;
    transform: translateY(-2px);
    box-shadow: 0 8px 16px rgba(255, 92, 92, 0.25);
  }
  
  &:active {
    transform: translateY(0);
    box-shadow: 0 4px 8px rgba(255, 92, 92, 0.15);
  }
}

/* 地址宫格 */
.address-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  margin-top: 24px;
}

.address-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: none;
  border: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 180px;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.08), 0 10px 10px -5px rgba(0, 0, 0, 0.02);
    border-color: rgba(255, 92, 92, 0.2);
    
    .addr-footer {
      opacity: 1;
      transform: translateY(0);
    }
  }
  
  .addr-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
    
    .name { 
      font-weight: 700; 
      font-size: 18px; 
      color: #1f2937;
    }
    .phone { 
      color: #6b7280; 
      font-size: 14px; 
      font-family: 'DIN Alternate', sans-serif;
      letter-spacing: 0.5px;
    }
  }
  
  .addr-body {
    flex: 1;
    color: #4b5563;
    font-size: 14px;
    line-height: 1.6;
    margin-bottom: 20px;
    
    .location-text {
       display: -webkit-box;
       -webkit-line-clamp: 2;
       -webkit-box-orient: vertical;
       overflow: hidden;
    }
  }
  
  .addr-footer {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding-top: 16px;
    border-top: 1px dashed #e5e7eb;
    opacity: 0;
    transform: translateY(10px);
    transition: all 0.3s;
    
      .action-btn {
        padding: 6px 16px;
        border-radius: 8px;
        font-size: 13px;
        transition: all 0.2s;
        
        &:hover {
          background: #f9fafb;
          color: var(--accent);
        }
        
        &.delete:hover {
          background: #fef2f2;
          color: #ef4444;
        }
      }
  }
}

/* 弹窗细节调整 */
.custom-dialog {
  border-radius: 12px;
  overflow: hidden;
}

.detail-header {
  background: #fdfdfd;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  border: 1px solid #eee;
  
  .status-row {
     display: flex; justify-content: space-between; align-items: center;
     margin-bottom: 8px;
     .status-text { font-size: 18px; font-weight: 700; 
       &.status-0 { color: #f56c6c; }
       &.status-1 { color: #e6a23c; }
       &.status-2 { color: #ff6b81; }
       &.status-3 { color: #67c23a; }
       &.status-4 { color: #909399; }
       &.status-5 { color: #f56c6c; }
       &.status-review { color: #ff7aa2; }
     }
  }
  .sn-row { font-size: 12px; color: #999; }
}

.detail-section {
  margin-bottom: 20px;
  h3 { font-size: 15px; font-weight: 600; margin-bottom: 10px; color: #333; }
  .info-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; color: #555; font-size: 13px; }
}

.product-list-mini {
  .product-item-mini {
    display: flex; gap: 10px; margin-bottom: 10px;
    img { width: 50px; height: 50px; border-radius: 4px; border: 1px solid #eee; }
    .p-info { flex: 1; 
       .p-name { font-size: 13px; color: #333; margin-bottom: 4px; }
       .p-meta { font-size: 12px; color: #999; }
    }
    .p-total { font-weight: 600; font-size: 14px; }
  }
}

.detail-footer {
  text-align: right;
  border-top: 1px solid #eee;
  padding-top: 16px;
  .label { color: #666; margin-right: 10px; }
  .amount { font-size: 20px; color: #f56c6c; font-weight: 700; }
}
</style>

<style lang="scss">
/* 给弹窗和浮层补一层非 scoped 样式 */
.address-dialog {
  --el-color-primary: rgb(255, 92, 92);
  --el-color-primary-light-3: rgba(255, 92, 92, 0.75);
  --el-color-primary-light-5: rgba(255, 92, 92, 0.45);
  --el-color-primary-light-7: rgba(255, 92, 92, 0.25);
  --el-color-primary-light-9: rgba(255, 92, 92, 0.12);
  --el-color-primary-dark-2: #ff4757;
  --el-input-focus-border-color: rgba(255, 92, 92, 0.95);
  --el-switch-on-color: rgb(255, 92, 92);
  --el-switch-border-color: #ffffff;
  --el-switch-hover-border-color: #ffffff;

  .el-button--primary {
    background-color: rgb(255, 92, 92);
    border-color: rgb(255, 92, 92);
  }

  .el-button--primary:hover,
  .el-button--primary:focus {
    background-color: #ff4757;
    border-color: #ff4757;
  }

  .el-input__wrapper.is-focus,
  .el-textarea__inner:focus {
    box-shadow: 0 0 0 1px rgba(255, 92, 92, 0.75) inset;
  }

  .el-input__wrapper.is-focus {
    border-color: rgba(255, 92, 92, 0.9);
  }

  .el-select .el-input__wrapper.is-focus {
    box-shadow: 0 0 0 1px rgba(255, 92, 92, 0.75) inset;
  }

  .el-switch.is-checked .el-switch__core {
    background-color: rgb(255, 92, 92);
    border-color: rgb(255, 92, 92);
  }

  .el-form-item.is-required .el-form-item__label:before {
    color: rgb(255, 92, 92);
  }

  .el-cascader .el-input__wrapper {
    border-radius: 10px;
  }

  /* 有些按钮吃不到主色变量，这里补一层通用样式 */
  .el-button--primary {
    --el-button-bg-color: rgb(255, 92, 92);
    --el-button-border-color: rgb(255, 92, 92);
    --el-button-hover-bg-color: #ff4757;
    --el-button-hover-border-color: #ff4757;
    --el-button-active-bg-color: #ff4757;
    --el-button-active-border-color: #ff4757;
  }
}

.review-dialog,
.after-sale-dialog {
  --el-color-primary: rgb(255, 92, 92);
  --el-color-primary-light-3: rgba(255, 92, 92, 0.75);
  --el-color-primary-light-5: rgba(255, 92, 92, 0.45);
  --el-color-primary-light-7: rgba(255, 92, 92, 0.25);
  --el-color-primary-light-9: rgba(255, 92, 92, 0.12);
  --el-color-primary-dark-2: #ff4757;
  --el-input-focus-border-color: rgba(255, 92, 92, 0.95);
}
</style>
