<template>
  <div class="product-container">
  <!-- 筛选与操作 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-wrapper">
    <!-- 左侧：筛选项 -->
        <el-form :inline="true" class="filter-form">
          <el-form-item label="关键词">
             <el-input v-model="searchKeyword" placeholder="商品名称 / ID" prefix-icon="Search" clearable @input="handleFilter" style="width: 180px"/>
          </el-form-item>
          <el-form-item label="商品分类">
            <el-select v-model="filterCategory" placeholder="全部分类" style="width: 140px" clearable @change="handleFilter">
              <el-option 
                v-for="item in categoryOptions" 
                :key="item.id" 
                :label="item.name" 
                :value="item.id" 
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
             <el-select v-model="filterStatus" placeholder="全部" style="width: 100px" clearable @change="handleFilter">
                <el-option label="已上架" :value="1" />
                <el-option label="已下架" :value="0" />
             </el-select>
          </el-form-item>
          <el-form-item label="库存状态">
             <el-select v-model="filterStockStatus" placeholder="全部" style="width: 110px" clearable @change="handleFilter">
                <el-option label="库存紧张" value="warning" />
                <el-option label="无库存" value="empty" />
                <el-option label="充足" value="normal" />
             </el-select>
          </el-form-item>
          <el-form-item label="标签">
             <el-select v-model="filterTag" placeholder="全部" style="width: 100px" clearable @change="handleFilter">
                <el-option label="热卖" value="hot" />
             </el-select>
          </el-form-item>
          <el-form-item label="排序">
             <el-select v-model="sortOption" placeholder="默认排序" style="width: 140px" @change="handleFilter">
                <el-option label="默认 (权重+ID)" value="default" />
                <el-option label="权重优先" value="weight_desc" />
                <el-option label="销量高到低" value="sales_desc" />
                <el-option label="价格低到高" value="price_asc" />
                <el-option label="价格高到低" value="price_desc" />
                <el-option label="库存少到多" value="stock_asc" />
             </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" link @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
        
    <!-- 右侧：操作区 -->
        <div class="action-group">
            <el-dropdown v-if="selectedRows.length > 0" @command="handleBatchCommand" style="margin-right: 12px">
              <el-button type="primary" plain>
                批量操作 ({{ selectedRows.length }}) <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="on">批量上架</el-dropdown-item>
                  <el-dropdown-item command="off">批量下架</el-dropdown-item>
                  <el-dropdown-item command="set_hot">批量设为热卖</el-dropdown-item>
                  <el-dropdown-item command="delete" divided style="color: #F56C6C">批量删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button type="primary" icon="Plus" @click="openDialog('create')">新增商品</el-button>
        </div>
      </div>
    </el-card>

  <!-- 数据表格 -->
    <el-card shadow="never">
      <el-table 
        :data="tableData" 
        style="width: 100%" 
        :row-class-name="tableRowClassName" 
        v-loading="loading"
        :empty-text="emptyText"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="主图" width="100">
          <template #default="{ row }">
            <el-image 
              :src="resolveImageUrl(row.cover)" 
              style="width: 60px; height: 60px; border-radius: 4px" 
              fit="cover" 
              :preview-src-list="[resolveImageUrl(row.cover)]" 
              :hide-on-click-modal="true"
              preview-teleported 
            >
                <template #error>
                    <div style="width: 100%; height: 100%; background: #f5f7fa; display: flex; justify-content: center; align-items: center; color: #909399">
                        <el-icon><Picture /></el-icon>
                    </div>
                </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column label="商品信息" min-width="250">
          <template #default="{ row }">
            <div class="product-name" style="cursor: pointer" @click="openDialog('edit', row)">
              <div class="main-title">
                  {{ row.name }} 
                  <span style="font-size: 12px; color: #909399; font-weight: normal; margin-left: 5px">
                      (ID: {{ row.id }})
                  </span>
              </div>
              <div class="sub-title">{{ row.subtitle }}</div>
      <!-- 3. 标签 -->
              <div class="tags-row" style="margin-top: 5px;">
                  <el-tag v-if="row.sales > 500" size="small" type="danger" effect="plain" style="margin-right: 5px">热卖</el-tag>
                  <el-tag v-if="row.stock === 0" size="small" type="danger" effect="dark" style="margin-right: 5px">无库存</el-tag>
                  <el-tag v-else-if="row.stock <= (row.warningStock || 10)" size="small" type="warning" effect="plain" style="margin-right: 5px">库存紧张</el-tag>
                  <el-tag v-if="!row.status" size="small" type="info" effect="plain">已下架</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="数据概览" width="150">
           <template #default="{ row }">
               <div style="font-size: 13px; line-height: 1.6;">
                   <div style="color: #F56C6C; font-weight: bold; font-size: 15px;">
                     ¥{{ row.price }} <span style="font-size: 12px; font-weight: normal; color: #909399">/ {{ row.unit || '件' }}</span>
                   </div>
                    <div style="color: #606266;">库存: {{ row.stock }}</div>
                    <div style="color: #909399; font-size: 12px; display: flex; gap: 8px;">
                        <span>销量: {{ row.sales || 0 }}</span>
                        <span>浏览: {{ row.views || 0 }}</span>
                    </div>
                    <div style="color: #909399; font-size: 12px;">
                        笔记关联: {{ row.relatedNoteCount || 0 }} <span v-if="row.relatedNoteCount > 10" style="color: #F56C6C">🔥</span>
                    </div>
                </div>
           </template>
        </el-table-column>
        <el-table-column label="上架状态" width="100">
          <template #default="{ row }">
      <!-- 4. 上下架切换（下架前二次确认） -->
            <el-switch 
                v-model="row.status" 
                :before-change="() => beforeStatusChange(row)"
                @change="handleStatusChange(row)" 
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog('edit', row)">编辑</el-button>
      <!-- 5. 删除确认（带提醒） -->
            <el-popconfirm 
                title="确定删除该商品吗?" 
                confirm-button-type="danger"
                width="220"
                :icon="InfoFilled"
                icon-color="#F56C6C"
                cancel-button-text="取消"
                confirm-button-text="确认删除"
                @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
              <div style="font-size: 12px; color: #909399; margin-top: 5px;">删除后无法恢复，仅保留历史订单</div>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

  <!-- 6. 分页 -->
      <div class="pagination-container" v-if="tableData.length > 0">
        <el-pagination 
          background 
          layout="total, prev, pager, next" 
          :total="total" 
          v-model:current-page="currentPage"
          :page-size="pageSize"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

  <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogType === 'create' ? '新增商品' : '编辑商品'" width="600px" top="50px" @closed="resetForm">
      <el-form :model="form" ref="formRef" :rules="rules" label-width="90px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="副标题" prop="subtitle">
          <el-input v-model="form.subtitle" placeholder="请输入卖点/副标题" />
        </el-form-item>
        <el-row :gutter="20">
            <el-col :span="12">
                <el-form-item label="商品分类" prop="categoryId">
                  <el-select v-model="form.categoryId" placeholder="请选择" style="width: 100%">
                    <el-option 
                        v-for="item in categoryOptions" 
                        :key="item.id" 
                        :label="item.name" 
                        :value="item.id" 
                    />
                  </el-select>
                </el-form-item>
            </el-col>
      <!-- 7. 适用宠物筛选 -->
            <el-col :span="12">
                 <el-form-item label="适用宠物" prop="petType">
                     <el-select v-model="form.petType" placeholder="请选择" style="width: 100%">
                        <el-option label="猫猫" value="cat" />
                        <el-option label="狗狗" value="dog" />
                        <el-option label="通用" value="general" />
                     </el-select>
                 </el-form-item>
            </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="价格" prop="price">
      <!-- 8. 价格校验规则 -->
              <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="如: 包, kg" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
             <el-form-item label="库存" prop="stock">
               <el-input-number v-model="form.stock" :min="0" :precision="0" style="width: 100%" />
             </el-form-item>
          </el-col>
          <el-col :span="12">
             <el-form-item label="预警库存" prop="warningStock">
               <el-input-number v-model="form.warningStock" :min="0" :precision="0" placeholder="默认10" style="width: 100%" />
               <div class="tip-text">低于此值时，列表显示“库存紧张”标签</div>
             </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
           <el-col :span="24">
             <el-form-item label="排序权重" prop="sortWeight">
               <el-input-number v-model="form.sortWeight" :min="0" :precision="0" placeholder="人工干预分" style="width: 100%" />
               <div class="tip-text">用于控制列表默认排序，数值越大越靠前</div>
             </el-form-item>
          </el-col>
        </el-row>

      <!-- 统计信息（只读） -->
        <el-row :gutter="20" v-if="dialogType === 'edit'">
             <el-col :span="12">
                 <el-form-item label="浏览量">
                     <el-input :model-value="form.views" disabled />
                 </el-form-item>
             </el-col>
             <el-col :span="12">
                 <el-form-item label="笔记关联">
                     <el-input :model-value="form.relatedNoteCount" disabled />
                 </el-form-item>
             </el-col>
        </el-row>
        
      <!-- 主图 -->
        <el-form-item label="商品主图（≤10张）" prop="cover">
          <el-upload
            v-model:file-list="coverFileList"
            action="#"
            list-type="picture-card"
            :limit="10"
            multiple
            :http-request="customUploadCover"
            :on-success="handleCoverUploadSuccess"
            :on-preview="handlePreviewDetail"
            :on-remove="handleCoverRemove"
            :before-upload="beforeUpload"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="tip-text" style="width: 100%">第一张会作为商品封面（列表展示用），上传后请点击“保存”才会生效</div>
        </el-form-item>

      <!-- 详情图 -->
        <el-form-item label="详情长图" prop="detailImgs">
          <el-upload
            v-model:file-list="detailFileList"
            action="#"
            list-type="picture-card"
            multiple
            :http-request="customUploadDetail"
            :on-success="handleDetailUploadSuccess"
            :on-preview="handlePreviewDetail"
            :on-remove="handleRemoveDetail"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="tip-text" style="width: 100%">推荐上传多张详情图，支持拖拽排序</div>
        </el-form-item>

      <!-- 描述 -->
        <el-form-item label="文字描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="后期待接入富文本编辑器..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <div class="save-actions">
               <el-button type="primary" @click="handleSubmit">保存</el-button>
          </div>
        </span>
      </template>
    </el-dialog>

  <!-- 图片预览弹窗 -->
    <el-dialog v-model="previewVisible">
      <img w-full :src="previewImageUrl" alt="Preview Image" style="width: 100%" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, InfoFilled, ArrowDown, Picture } from '@element-plus/icons-vue'
import * as productApi from '@/api/product'
import request from '@/api/request'
import { resolveImageUrl } from '@/utils/image'

const loading = ref(false)
const route = useRoute()
const allData = ref([])
const tableData = ref([])
const selectedRows = ref([]) // 批量选择的行

// 筛选条件
const filterCategory = ref('')
const filterStatus = ref('')
const filterStockStatus = ref('') // 库存状态筛选
const filterTag = ref('')         // 标签筛选
const searchKeyword = ref('')
const sortOption = ref('default')
const categoryOptions = ref([])

const loadCategories = async () => {
    try {
        const res = await productApi.getCategoryList()
        if (res.code === 200 && Array.isArray(res.data)) {
            categoryOptions.value = res.data
        } else {
            ElMessage.error(res.message || res.msg || '加载分类失败')
        }
    } catch (e) {
        console.error('加载分类失败', e)
    }
}

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const emptyText = ref("暂无商品，可点击右上角‘新增商品’进行添加")

// 弹窗状态
const dialogVisible = ref(false)
const dialogType = ref('create') // create | edit
const formRef = ref(null)
const form = reactive({
  id: null,
  name: '',
  subtitle: '',
  categoryId: null,
  petType: 'general',
  price: 32,
  unit: '2.5kg',
  stock: 200,
  warningStock: 10,
  sortWeight: 0,
  cover: '',
  coverImgs: [],
  detailImgs: [],
  description: '',
  status: true,
  sales: 0,
  views: 0,
  relatedNoteCount: 0
})

// 图片列表状态
const coverFileList = ref([])
const detailFileList = ref([])
const previewVisible = ref(false)
const previewImageUrl = ref('')

// 把上传后的主图同步回 form.cover，方便走表单校验
const syncCoverToForm = () => {
  const urls = coverFileList.value
    .map(f => f.rawUrl || f.response?.data || f.url)
    .filter(u => typeof u === 'string' && u.startsWith('/images/'))
  form.cover = urls[0] || ''
}

watch(coverFileList, () => {
  syncCoverToForm()
}, { deep: true })
 
const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [
      { required: true, message: '请输入价格', trigger: 'blur' },
      { type: 'number', min: 0.01, message: '价格必须大于0', trigger: 'blur' }
  ],
  cover: [{ required: true, message: '请至少上传1张主图', trigger: 'change' }]
}

// 数据处理
// 点击图片本体时顺手关闭预览器
const closeViewerOnImgClick = (e) => {
    if (e.target.classList.contains('el-image-viewer__img')) {
        const closeBtn = document.querySelector('.el-image-viewer__close')
        if (closeBtn) closeBtn.click()
    }
}

onMounted(async () => {
  document.addEventListener('click', closeViewerOnImgClick)
  await loadCategories()
  
  // 处理跨模块跳转带来的商品参数
  if (route.query.productId) {
    searchKeyword.value = `ID:${route.query.productId}`
    ElMessage.success(`已自动搜索商品 ID: ${route.query.productId}`)
  }
  
  await loadProductList()
})

const parseDetailImgs = (value) => {
    if (!value) return []
    if (Array.isArray(value)) return value
    if (typeof value === 'string') {
        const trimmed = value.trim()
        if (!trimmed) return []
        if (trimmed.startsWith('[')) {
            try {
                const parsed = JSON.parse(trimmed)
                return Array.isArray(parsed) ? parsed : []
            } catch (e) {
                return []
            }
        }
        return [trimmed]
    }
    return []
}

const mapProductFromApi = (p) => ({
    id: p.id,
    name: p.name,
    subtitle: p.subTitle,
    categoryId: p.categoryId,
    categoryName: p.categoryName,
    petType: p.petType || 'general',
    price: p.price,
    unit: p.unit || '件',
    stock: p.stock ?? 0,
    warningStock: p.warningStock ?? 10,
    sortWeight: p.sortWeight ?? 0,
    cover: p.coverImg,
    coverImgs: parseDetailImgs(p.coverImgs || p.coverImg),
    detailImgs: parseDetailImgs(p.detailImgs),
    description: p.description,
    status: p.status === 1,
    sales: p.sales || 0,
    views: p.views || 0,
    relatedNoteCount: p.relatedNoteCount || 0
})

const buildPayloadFromRow = (row) => ({
    categoryId: row.categoryId,
    name: row.name,
    subTitle: row.subtitle,
    price: row.price,
    unit: row.unit,
    stock: row.stock,
    warningStock: row.warningStock,
    sortWeight: row.sortWeight,
    coverImg: row.cover,
    coverImgs: JSON.stringify((row.coverImgs && row.coverImgs.length > 0) ? row.coverImgs : (row.cover ? [row.cover] : [])),
    detailImgs: JSON.stringify(row.detailImgs || []),
    description: row.description,
    status: row.status ? 1 : 0,
    petType: row.petType,
    sales: row.sales,
    views: row.views,
    relatedNoteCount: row.relatedNoteCount
})

const loadProductList = async () => {
    loading.value = true
    try {
        const params = {
            pageNum: currentPage.value,
            pageSize: pageSize.value,
            categoryId: filterCategory.value || undefined,
            status: filterStatus.value === '' ? undefined : filterStatus.value,
            keyword: searchKeyword.value || undefined
        }
        const res = await productApi.getProductList(params)
        if (res.code === 200 && res.data) {
            allData.value = (res.data.records || []).map(mapProductFromApi)
            filterData()
            total.value = res.data.total || 0
        } else {
            ElMessage.error(res.message || res.msg || '加载商品列表失败')
        }
    } catch (e) {
        console.error('加载商品列表失败', e)
    } finally {
        loading.value = false
    }
}

// 已经在商品页时，也要响应新的路由参数
watch(() => route.query.productId, (newId) => {
    if (newId) {
        searchKeyword.value = `ID:${newId}`
        handleFilter()
    }
})

onUnmounted(() => {
   document.removeEventListener('click', closeViewerOnImgClick)
})

const filterData = () => {
    let res = allData.value.slice()

    // 分类筛选
    if (filterCategory.value) {
        res = res.filter(item => item.categoryId === filterCategory.value)
    }

    // 状态筛选
    if (filterStatus.value !== '' && filterStatus.value !== null) {
        res = res.filter(item => (item.status ? 1 : 0) === filterStatus.value)
    }

    // 关键词筛选
    if (searchKeyword.value) {
        const keyword = searchKeyword.value.trim()
        
        // 严格的 ID 匹配模式，比如从跳转参数进来时
        if (keyword.toUpperCase().startsWith('ID:') || keyword.toUpperCase().startsWith('ID：')) {
            const idVal = keyword.substring(3).trim()
            if (idVal) {
                 res = res.filter(item => String(item.id) === idVal)
            }
        } else {
             // 普通模糊匹配
             const lowerKeyword = keyword.toLowerCase()
             res = res.filter(item => 
                item.name.toLowerCase().includes(lowerKeyword) || 
                String(item.id).includes(lowerKeyword)
             )
        }
    }

    // 库存状态筛选
    if (filterStockStatus.value) {
        if (filterStockStatus.value === 'empty') {
            res = res.filter(item => item.stock === 0)
        } else if (filterStockStatus.value === 'warning') {
            res = res.filter(item => item.stock > 0 && item.stock <= (item.warningStock || 10))
        } else if (filterStockStatus.value === 'normal') {
            res = res.filter(item => item.stock > (item.warningStock || 10))
        }
    }

    // 标签筛选
    if (filterTag.value === 'hot') {
        res = res.filter(item => item.sales > 500)
    }
    
    // 排序逻辑
    if (sortOption.value === 'weight_desc') {
        res.sort((a, b) => (b.sortWeight || 0) - (a.sortWeight || 0))
    } else if (sortOption.value === 'sales_desc') {
        res.sort((a, b) => (b.sales || 0) - (a.sales || 0))
    } else if (sortOption.value === 'price_asc') {
        res.sort((a, b) => a.price - b.price)
    } else if (sortOption.value === 'price_desc') {
        res.sort((a, b) => b.price - a.price)
    } else if (sortOption.value === 'stock_asc') {
        res.sort((a, b) => a.stock - b.stock)
    } else {
        // 默认按综合分倒序
        // 综合分 = 销量 + 浏览量 * 0.5 + 关联笔记数 * 20 + 权重
        res.sort((a, b) => {
            const scoreA = (a.sales || 0) + (a.views || 0) * 0.5 + (a.relatedNoteCount || 0) * 20 + (a.sortWeight || 0)
            const scoreB = (b.sales || 0) + (b.views || 0) * 0.5 + (b.relatedNoteCount || 0) * 20 + (b.sortWeight || 0)
            if (scoreA !== scoreB) return scoreB - scoreA
            return b.id - a.id
        })
    }

    tableData.value = res
}

const handleFilter = () => {
  currentPage.value = 1
  loadProductList()
}

const resetFilter = () => {
    filterCategory.value = ''
    filterStatus.value = ''
    filterStockStatus.value = ''
    filterTag.value = ''
    searchKeyword.value = ''
    sortOption.value = 'default'
    currentPage.value = 1
    loadProductList()
}

const handlePageChange = (val) => {
    currentPage.value = val
    // 翻页后回到顶部
    window.scrollTo({ top: 0, behavior: 'smooth' })
    loadProductList()
}

// 交互守卫

// 上下架前置确认
const beforeStatusChange = (row) => {
    if (row.status) {
        // 从上架切到下架时要二次确认
        return new Promise((resolve) => {
             ElMessageBox.confirm(
                '下架后商品将无法购买，确认下架吗？',
                '风险提示',
                {
                  confirmButtonText: '确认下架',
                  cancelButtonText: '取消',
                  type: 'warning',
                }
              )
                .then(() => resolve(true))
                .catch(() => resolve(false))
        })
    } else {
        // 从下架切回上架时直接放行
        return true
    }
}

const handleStatusChange = (row) => {
    const nextStatus = row.status ? 1 : 0
    productApi.changeProductStatus(row.id, nextStatus)
        .then((res) => {
            if (res.code !== 200) {
                row.status = !row.status
                ElMessage.error(res.message || res.msg || '状态更新失败')
                return
            }
            const statusText = row.status ? '已上架' : '已下架'
            ElMessage.success(`商品 ID:${row.id} ${statusText}`)
        })
        .catch((e) => {
            row.status = !row.status
            console.error('更新状态失败', e)
        })
}

// 增删改查

const openDialog = async (type, row) => {
  dialogType.value = type
  dialogVisible.value = true
  
  if (type === 'edit' && row) {
    try {
      const res = await productApi.getProductDetail(row.id)
      if (res.code === 200 && res.data) {
        const mapped = mapProductFromApi(res.data)
        Object.assign(form, mapped)
        coverFileList.value = (mapped.coverImgs || []).map((url, index) => ({
          name: `cover-${index}`,
          url: resolveImageUrl(url),
          rawUrl: url
        }))
        detailFileList.value = mapped.detailImgs.map((url, index) => ({
          name: `img-${index}`,
          url: resolveImageUrl(url),
          rawUrl: url
        }))
      } else {
        ElMessage.error(res.message || res.msg || '加载商品详情失败')
      }
    } catch (e) {
      console.error('加载商品详情失败', e)
    }
  } else {
    // 重置表单。新增时给一组常用默认值，录入会快一点
    Object.assign(form, {
      id: null,
      name: '',
      subtitle: '',
      categoryId: null,
      petType: 'general',
      price: 32,
      unit: '2.5kg',
      stock: 200,
      warningStock: 10,
      sortWeight: 0,
      cover: '',
      coverImgs: [],
      detailImgs: [],
      description: '',
      status: true,
      sales: 0,
      views: 0,
      relatedNoteCount: 0
    })
    coverFileList.value = []
    detailFileList.value = []
  }
}

const resetForm = () => {
  if (formRef.value) formRef.value.resetFields()
}

// 上传相关
const uploadImage = async (file, biz) => {
  const formData = new FormData()
  formData.append('file', file)
  const res = await request({
    url: '/common/upload/image',
    method: 'post',
    params: { biz },
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  if (res.code === 200 && res.data) {
    return res.data
  }
  throw new Error(res.message || res.msg || '上传失败')
}

const customUploadCover = async (options) => {
  const { file, onSuccess, onError } = options
  try {
    const url = await uploadImage(file, 'productCover')
    onSuccess({ code: 200, data: url })
  } catch (e) {
    onError(e)
  }
}

const handleCoverUploadSuccess = (response, uploadFile) => {
  const url = response?.data
  if (typeof url === 'string' && url.startsWith('/images/')) {
    uploadFile.rawUrl = url
    uploadFile.url = resolveImageUrl(url)
    ElMessage.success('主图上传成功')
    syncCoverToForm()
  }
}

const handleCoverRemove = (uploadFile, uploadFiles) => {
  // 这里不用额外处理，提交时会重新从 coverFileList 计算
  syncCoverToForm()
}

const customUploadDetail = async (options) => {
  const { file, onSuccess, onError } = options
  try {
    const url = await uploadImage(file, 'productDetail')
    // 把后端真实路径回传给 on-success，避免把 blob 地址写进库
    onSuccess({ code: 200, data: url })
  } catch (e) {
    onError(e)
  }
}

const handleDetailUploadSuccess = (response, uploadFile) => {
  // Element Plus 会先给 uploadFile.url 塞一个 blob 预览地址
  // 这里把它换成后端返回的真实地址，并保存 rawUrl 供提交时使用
  const url = response?.data
  if (typeof url === 'string' && url.startsWith('/images/')) {
    uploadFile.rawUrl = url
    uploadFile.url = resolveImageUrl(url)
  }
}

const handleRemoveDetail = (uploadFile, uploadFiles) => {}

const handlePreviewDetail = (file) => {
  previewImageUrl.value = resolveImageUrl(file.rawUrl || file.url)
  previewVisible.value = true
}

const beforeUpload = (file) => {
  const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isJPG) {
    ElMessage.error('上传图片只能是 JPG/PNG 格式!')
  }
  if (!isLt10M) {
    ElMessage.error('上传图片大小不能超过 10MB!')
  }
  return isJPG && isLt10M
}

const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      // 只保留后端真实路径，避免 blob 地址被写进数据库
      const detailUrls = detailFileList.value
        .map(f => f.rawUrl || f.response?.data || f.url)
        .filter(u => typeof u === 'string' && u.startsWith('/images/'))

      const coverUrls = coverFileList.value
        .map(f => f.rawUrl || f.response?.data || f.url)
        .filter(u => typeof u === 'string' && u.startsWith('/images/'))

      if (coverUrls.length === 0) {
        ElMessage.error('请至少上传1张主图')
        return
      }
      
      const payload = {
        categoryId: form.categoryId,
        name: form.name,
        subTitle: form.subtitle,
        price: form.price,
        unit: form.unit,
        stock: form.stock,
        warningStock: form.warningStock,
        sortWeight: form.sortWeight,
        coverImg: coverUrls[0] || '',
        coverImgs: JSON.stringify(coverUrls.slice(0, 10)),
        detailImgs: JSON.stringify(detailUrls),
        description: form.description,
        status: form.status ? 1 : 0,
        petType: form.petType
      }

      if (dialogType.value === 'create') {
        // 库存为 0 时，再多确认一次
        if (form.stock === 0 && form.status) {
            try {
                await ElMessageBox.confirm('当前库存为 0，确认仍要上架商品吗？', '库存预警', {
                    confirmButtonText: '确认上架',
                    cancelButtonText: '取消',
                    type: 'warning'
                })
            } catch (e) {
                return // User cancelled
            }
        }
        
        try {
          const res = await productApi.createProduct(payload)
          if (res.code !== 200) {
            ElMessage.error(res.message || res.msg || '商品创建失败')
            return
          }
          ElMessage.success('商品创建成功')
        } catch (e) {
          console.error('创建商品失败', e)
          return
        }
      } else {
        try {
          const res = await productApi.updateProduct(form.id, payload)
          if (res.code !== 200) {
            ElMessage.error(res.message || res.msg || '更新失败')
            return
          }
          ElMessage.success('更新成功')
        } catch (e) {
          console.error('更新商品失败', e)
          return
        }
      }
      dialogVisible.value = false
      await loadProductList()

      // 操作完成后高亮刚处理的商品
      if (form.id) {
          highLightRow(form.id)
      } else if (allData.value.length > 0) {
          highLightRow(allData.value[0].id)
      }
    }
  })
}

const highLightRow = (id) => {
    const item = allData.value.find(i => i.id === id)
    if (item) {
        item._highlight = true
        setTimeout(() => { item._highlight = false }, 2000)
    }
}
const tableRowClassName = ({ row }) => {
  if (row._highlight) {
    return 'success-row'
  }
  return ''
}

const handleDelete = (row) => {
  productApi.deleteProduct(row.id)
    .then((res) => {
      if (res.code !== 200) {
        ElMessage.error(res.message || res.msg || '删除失败')
        return
      }
      ElMessage.success('商品已删除')
      loadProductList()
    })
    .catch((e) => {
      console.error('删除商品失败', e)
    })
}

// 批量操作
const handleSelectionChange = (val) => {
  selectedRows.value = val
}

const handleBatchCommand = (command) => {
  const ids = selectedRows.value.map(row => row.id)
  const names = selectedRows.value.map(row => row.name).join(', ')
  
  if (command === 'on') {
     ElMessageBox.confirm(`确定批量上架这 ${ids.length} 个商品吗?`, '提示', { type: 'warning' })
      .then(async () => {
          const res = await productApi.batchProductAction('online', ids)
          if (res.code !== 200) {
            ElMessage.error(res.message || res.msg || '批量上架失败')
            return
          }
          ElMessage.success('批量上架成功')
          loadProductList()
      })
  } else if (command === 'off') {
      ElMessageBox.confirm(`确定批量下架这 ${ids.length} 个商品吗?`, '风险提示', { type: 'warning' })
      .then(async () => {
          const res = await productApi.batchProductAction('offline', ids)
          if (res.code !== 200) {
            ElMessage.error(res.message || res.msg || '批量下架失败')
            return
          }
          ElMessage.success('批量下架成功')
          loadProductList()
      })
  } else if (command === 'set_hot') {
        ElMessageBox.confirm(`确定将这 ${ids.length} 个商品设为热卖吗?`, '提示', { type: 'warning' })
        .then(async () => {
            try {
              const tasks = selectedRows.value.map(row => {
                if (row.sales >= 500) return Promise.resolve()
                const payload = buildPayloadFromRow({ ...row, sales: 501 })
                return productApi.updateProduct(row.id, payload)
              })
              await Promise.all(tasks)
              ElMessage.success(`已将 ${ids.length} 个商品设为热卖`)
              loadProductList()
            } catch (e) {
              console.error('设置热卖失败', e)
            }
        })
  } else if (command === 'delete') {
      ElMessageBox.confirm(`确定删除以下${ids.length}个商品吗？\n${names}`, '危险操作', { 
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'error' 
      })
      .then(async () => {
          const res = await productApi.batchProductAction('delete', ids)
          if (res.code !== 200) {
            ElMessage.error(res.message || res.msg || '批量删除失败')
            return
          }
          ElMessage.success('批量删除成功')
          loadProductList()
      })
  }
}
</script>

<style scoped>
.filter-card {
  margin-bottom: 20px;
}
.filter-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: flex-start; /* Apply top alignment in case of wrapping */
  gap: 20px;
}
.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px; /* Uniform gap between items */
  flex: 1;
}
.filter-form .el-form-item {
  margin-bottom: 0; /* Let gap handle spacing */
  margin-right: 0;
}
.action-group {
  display: flex;
  align-items: center;
  flex-shrink: 0; /* Prevent shrinking */
}

/* 表格样式 */
.product-name .main-title {
  font-weight: 500;
  font-size: 14px;
  color: #303133;
}
.product-name .sub-title {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
/* 复用旧版上传样式 */
.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}
.avatar-uploader .el-upload:hover {
  border-color: var(--admin-professional-primary);
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  text-align: center;
  line-height: 100px; /* Centering fix */
  border: 1px dashed #d9d9d9; /* Explicit border for icon container */
  border-radius: 6px;
}
.avatar {
  width: 100px;
  height: 100px;
  display: block;
}
.tip-text {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
.save-actions {
    display: inline-block; 
    margin-left: 10px;
}
:deep(.el-table .success-row) {
  --el-table-tr-bg-color: var(--el-color-success-light-9);
}
</style>
