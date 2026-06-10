<template>
  <div class="checkout-container">
    <div class="left">
  <!-- 地址区 -->
      <section class="card address-section">
        <div class="section-header">
          <h3>确认收货地址</h3>
          <div class="header-actions">
            <el-button link class="header-btn" @click="openAddressDialog()">使用新地址</el-button>
            <el-button link class="header-btn" @click="goManageAddress">管理地址</el-button>
          </div>
        </div>

        <div v-loading="loadingAddress" class="address-content">
          <el-empty v-if="!loadingAddress && addressList.length === 0" description="暂无地址" />
          <div v-else class="address-grid">
            <div
              v-for="addr in addressList"
              :key="addr.id"
              class="address-card"
              :class="{ active: addr.id === selectedAddressId }"
              @click="selectAddress(addr)"
            >
              <div class="addr-top">
                <span v-if="addr.isDefault === 1" class="default-tag">默认</span>
                <el-button link class="edit-btn" @click.stop="openAddressDialog(addr)">编辑</el-button>
              </div>
              <div class="addr-line">{{ addr.province }}{{ addr.city }}{{ addr.region }}</div>
              <div class="addr-detail">{{ addr.detailAddress }}</div>
              <div class="addr-user">{{ addr.name }} {{ addr.phone }}</div>
            </div>
          </div>
        </div>
      </section>

  <!-- 订单区 -->
      <section class="card order-section">
        <div class="section-header">
          <h3>确认订单信息</h3>
        </div>

        <div class="order-list" v-loading="loadingCart">
          <div class="order-header">
            <span class="col product">商品信息</span>
            <span class="col attrs">商品属性</span>
            <span class="col qty">数量</span>
            <span class="col price">价格</span>
          </div>
          <div v-if="!loadingCart && cartList.length === 0" class="order-empty">
            <el-empty description="购物车为空" />
          </div>
          <div v-for="item in cartList" :key="item.id" class="order-row">
            <div class="col product">
              <img :src="getImageUrl(item.productImg)" class="thumb" />
              <div class="name">{{ item.productName }}</div>
            </div>
            <div class="col attrs">默认规格</div>
            <div class="col qty">{{ item.quantity }}</div>
            <div class="col price">¥{{ Number(item.price).toFixed(2) }}</div>
          </div>
        </div>
      </section>
    </div>

    <div class="right">
      <section class="card summary-section">
        <div class="section-header">
          <h3>付款详情</h3>
          <div class="sub">共 {{ cartList.length }} 件商品</div>
        </div>

        <div class="remark-input-area">
           <div class="label">订单备注</div>
           <el-input
              v-model="remark"
              type="textarea"
              :rows="2"
              maxlength="200"
              show-word-limit
              placeholder="选填：请填写您对订单的特殊要求"
              class="warm-input"
            />
        </div>

        <div class="service-info-area">
           <div class="line">
              <span class="label">配送服务</span>
              <span class="value">快递 · 包邮</span>
           </div>
           <div class="line">
              <span class="label">运费险</span>
              <span class="value">¥0.00</span>
           </div>
        </div>

        <div class="divider"></div>

        <div class="summary-lines">
          <div class="line">
            <span>商品总价</span>
            <span>¥{{ totalPrice }}</span>
          </div>
          <div class="line">
            <span>运费</span>
            <span>¥0.00</span>
          </div>
        </div>

        <div class="divider"></div>

        <div class="total">
          <span class="label">合计</span>
          <span class="price">¥{{ totalPrice }}</span>
        </div>

        <div class="pay-methods">
          <div class="method-title">支付方式</div>
          <el-radio-group v-model="payType" class="pay-group">
            <el-radio label="alipay">
               <span class="pay-label">
                  <span class="icon alipay">支</span> 支付宝
               </span>
            </el-radio>
            <el-radio label="wechat">
               <span class="pay-label">
                  <span class="icon wechat">微</span> 微信支付
               </span>
            </el-radio>
          </el-radio-group>
        </div>

        <div class="actions">
          <el-button class="back-btn" @click="router.push('/cart')">返回</el-button>
          <el-button type="primary" class="submit-btn" :loading="submitting" @click="submitOrder">提交订单</el-button>
        </div>
      </section>
    </div>

  <!-- 地址弹窗 -->
    <el-dialog v-model="addressDialogVisible" :title="addressDialogTitle" width="520px" destroy-on-close align-center>
      <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-width="90px">
        <el-form-item label="收货人" prop="name">
          <el-input v-model="addressForm.name" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addressForm.phone" placeholder="请输入手机号" />
        </el-form-item>
          <el-form-item label="省市区" prop="areaCodes">
          <el-cascader
            v-model="addressForm.areaCodes"
            :options="chinaAreaOptions"
            :props="{ ...chinaAreaCascaderProps, expandTrigger: 'hover' }"
            placeholder="请选择省/市/区"
            filterable
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input v-model="addressForm.detailAddress" placeholder="如：科技园路XX号" />
        </el-form-item>
        <el-form-item label="默认地址" prop="isDefault">
          <el-switch v-model="addressForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="addressDialogVisible = false">取消</el-button>
          <el-button class="save-btn" :loading="addressSaving" @click="handleSaveAddress">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { getImageUrl } from '@/utils/image'
import { chinaAreaOptions, chinaAreaCascaderProps, chinaAreaNameByCode, chinaAreaCodesFromNames } from '@/utils/area'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const cartList = ref([])
const loadingCart = ref(false)
const addressList = ref([])
const loadingAddress = ref(false)
const selectedAddressId = ref(null)
const remark = ref('')
const submitting = ref(false)
const payType = ref('alipay')

const totalPrice = computed(() => {
  return cartList.value
    .reduce((sum, item) => sum + Number(item.price) * item.quantity, 0)
    .toFixed(2)
})

const loadCart = async () => {
  loadingCart.value = true
  try {
    const res = await request.get('/cart/list')
    cartList.value = res || []
  } finally {
    loadingCart.value = false
  }
}

const loadAddress = async () => {
  loadingAddress.value = true
  try {
    const res = await request.get('/user/address/list')
    addressList.value = res || []
    const defaultAddr = addressList.value.find(a => a.isDefault === 1)
    if (defaultAddr) {
      selectedAddressId.value = defaultAddr.id
    } else if (addressList.value.length > 0) {
      selectedAddressId.value = addressList.value[0].id
    }
  } finally {
    loadingAddress.value = false
  }
}

const selectAddress = (addr) => {
  selectedAddressId.value = addr.id
}

const submitOrder = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再下单')
    userStore.showLogin()
    return
  }
  if (cartList.value.length === 0) {
    ElMessage.warning('购物车为空')
    return
  }
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  submitting.value = true
  try {
    const cartItemIds = cartList.value.map(i => i.id)
    const orderId = await request.post('/order/submit', {
      cartItemIds,
      addressId: selectedAddressId.value,
      remark: remark.value || ''
    })
    ElMessage.success('下单成功，正在跳转支付')
    await userStore.fetchCartCount()
    router.push({
      path: '/pay',
      query: {
        orderId,
        amount: totalPrice.value
      }
    })
  } finally {
    submitting.value = false
  }
}

const goManageAddress = () => {
  router.push({ path: '/profile', query: { tab: 'address' } })
}

// 新增或编辑地址
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
  name: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  areaCodes: [{
    validator: (rule, value, callback) => {
      if (!Array.isArray(value) || value.length !== 3) {
        callback(new Error('请选择省/市/区'))
        return
      }
      callback()
    },
    trigger: 'change'
  }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  isDefault: [{ required: true, message: '请选择是否为默认地址', trigger: 'change' }]
}

watch(() => addressForm.areaCodes, (val) => {
  if (Array.isArray(val) && val.length === 3) {
    addressForm.province = chinaAreaNameByCode(val[0]) || ''
    addressForm.city = chinaAreaNameByCode(val[1]) || ''
    addressForm.region = chinaAreaNameByCode(val[2]) || ''
  } else {
    addressForm.province = ''
    addressForm.city = ''
    addressForm.region = ''
  }
}, { deep: true })

const handleSaveAddress = async () => {
  if (!addressFormRef.value) return
  await addressFormRef.value.validate(async (valid) => {
    if (!valid) return
    addressSaving.value = true
    try {
      const payload = {
        id: addressForm.id,
        name: addressForm.name,
        phone: addressForm.phone,
        province: addressForm.province,
        city: addressForm.city,
        region: addressForm.region,
        detailAddress: addressForm.detailAddress,
        isDefault: addressForm.isDefault
      }
      if (addressMode.value === 'create') {
        await request.post('/user/address', payload)
        ElMessage.success('新增成功')
      } else {
        await request.put('/user/address', payload)
        ElMessage.success('保存成功')
      }
      addressDialogVisible.value = false
      await loadAddress()
    } finally {
      addressSaving.value = false
    }
  })
}

onMounted(async () => {
  await Promise.all([loadCart(), loadAddress()])
})
</script>

<style scoped lang="scss">
.checkout-container {
  max-width: 1200px;
  margin: 30px auto;
  display: flex;
  gap: 24px;
  padding: 0 20px;
  
  @media (max-width: 992px) {
    flex-direction: column;
    padding: 20px;
  }
}

.left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.right {
  width: 320px;
  flex-shrink: 0;
}

.card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.03);
  border: 1px solid #f9f9f9;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #333;
  }
  .sub {
    font-size: 13px;
    color: #999;
  }
}

.header-actions {
  display: flex;
  gap: 16px;
}

.header-btn, .edit-btn {
  color: #ff6b81;
  
  &:hover {
    color: #ff4757;
  }
}

/* 地址卡片 */
.address-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.address-card {
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.25s ease;
  background: #fff;
  position: relative;

  &:hover {
    border-color: #ffb1c1;
    box-shadow: 0 8px 20px rgba(255, 107, 129, 0.1);
     transform: translateY(-2px);
  }

  &.active {
    border-color: #ff6b81;
    background: #fff9fa;
    box-shadow: 0 0 0 2px rgba(255, 107, 129, 0.2);
    
    &::after {
      content: '✓';
      position: absolute;
      right: 0;
      bottom: 0;
      background: #ff6b81;
      color: #fff;
      font-size: 12px;
      padding: 2px 6px;
      border-top-left-radius: 8px;
      border-bottom-right-radius: 10px;
    }
  }

  .addr-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }

  .default-tag {
    font-size: 12px;
    color: #fff;
    background: #ff6b81;
    padding: 1px 6px;
    border-radius: 4px;
  }

  .addr-line {
    font-weight: 600;
    margin-bottom: 6px;
    color: #333;
    font-size: 15px;
  }

  .addr-detail {
    color: #666;
    margin-bottom: 8px;
    font-size: 13px;
    line-height: 1.4;
    height: 36px;
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  .addr-user {
    color: #999;
    font-size: 13px;
  }
}

/* 订单列表 */
.order-list {
  border-top: 1px solid #f0f0f0;
}

.order-header,
.order-row {
  display: grid;
  grid-template-columns: 2fr 1fr 0.8fr 1fr;
  gap: 16px;
  align-items: center;
}

.order-header {
  font-size: 13px;
  color: #999;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.order-row {
  padding: 20px 0;
  border-bottom: 1px solid #f9f9f9;
  
  &:last-child {
    border-bottom: none;
  }
}

.col.product {
  display: flex;
  align-items: center;
  gap: 12px;

  .thumb {
    width: 60px;
    height: 60px;
    border-radius: 8px;
    object-fit: cover;
    border: 1px solid #f0f0f0;
  }

  .name {
    font-size: 14px;
    color: #333;
    line-height: 1.4;
  }
}

.col.price {
  color: #333;
  font-weight: 600;
  font-family: Arial, sans-serif;
}

/* 吸顶汇总区 */
.summary-section {
  position: sticky;
  top: 20px;
}

.remark-input-area {
  margin-bottom: 16px;
  
  .label {
    font-size: 14px;
    font-weight: 600;
    color: #333;
    margin-bottom: 8px;
  }
  
  .warm-input :deep(.el-textarea__inner) {
    border-color: #eee;
    background: #fafafa;
    border-radius: 8px;
    font-size: 13px;
    
    &:focus {
      border-color: #ffb1c1;
      background: #fff;
    }
  }
}

.service-info-area {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  
  .line {
    display: flex;
    justify-content: space-between;
    font-size: 13px;
    
    .label {
      color: #333;
    }
    
    .value {
      color: #666;
    }
  }
}

.summary-lines {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 10px 0 20px;
}

.line {
  display: flex;
  justify-content: space-between;
  color: #666;
  font-size: 14px;
}

.divider {
  height: 1px;
  background: #f0f0f0;
  margin: 10px 0 20px;
}

.total {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 24px;
  
  .label {
    font-size: 16px;
    color: #333;
    font-weight: 600;
  }

  .price {
    font-size: 28px;
    color: #ff5000;
    font-weight: 700;
    font-family: Arial, sans-serif;
  }
}

.pay-methods {
  margin-bottom: 30px;

  .method-title {
    font-weight: 600;
    margin-bottom: 12px;
    font-size: 14px;
    color: #333;
  }

  .pay-group {
    display: flex;
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .pay-label {
    display: flex;
    align-items: center;
    gap: 6px;
    
    .icon {
      width: 20px;
      height: 20px;
      border-radius: 4px;
      color: #fff;
      font-size: 12px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      
      &.alipay { background: #1677ff; }
      &.wechat { background: #07c160; }
    }
  }
}

.save-btn {
  background-color: #ff6b81;
  border-color: #ff6b81;
  color: #fff;
  
  &:hover {
    background-color: #ff4757;
    border-color: #ff4757;
  }
}

.actions {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 12px;

  :deep(.el-button) {
    width: 100%;
    margin-left: 0;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  :deep(.el-button + .el-button) {
    margin-left: 0;
  }
  
  .submit-btn {
    height: 48px;
    background: linear-gradient(135deg, #FF6B6B 0%, #FF8E53 100%);
    border: none;
    border-radius: 24px;
    color: #fff;
    font-size: 18px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 6px 16px rgba(255, 107, 129, 0.25);
    transition: all 0.2s;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(255, 107, 129, 0.35);
    }
  }
  
  .back-btn {
    height: 48px;
    border-radius: 24px;
    border: 1px solid #f0f0f0;
    background: #fff;
    color: #666;
    font-size: 16px;
    font-weight: 500;
    transition: all 0.2s;
    
    &:hover {
      color: #333;
      background: #f5f7fa;
      border-color: #e0e0e0;
    }
  }
}
</style>
