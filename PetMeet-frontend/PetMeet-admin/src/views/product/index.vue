<template>
  <div class="product-container">
    <ProductFilterBar />
    <ProductTable />
    <ProductDialogs />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch, provide } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, InfoFilled, ArrowDown, Picture } from '@element-plus/icons-vue'
import * as productApi from '@/api/product'
import request from '@/api/request'
import { resolveImageUrl } from '@/utils/image'
import { useAdminTable } from '@/composables/useAdminTable'
import ProductFilterBar from './components/ProductFilterBar.vue'
import ProductTable from './components/ProductTable.vue'
import ProductDialogs from './components/ProductDialogs.vue'

const route = useRoute()
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

const emptyText = ref("暂无商品，可点击右上角‘新增商品’进行添加")

// 弹窗状态
const dialogVisible = ref(false)
const dialogType = ref('create') // create | edit
const productDialogLoading = ref(false)
const productDialogRenderKey = ref(0)
let productDialogRequestSeq = 0
const formRef = ref(null)

const getProductFormDefaults = () => ({
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

const form = reactive(getProductFormDefaults())

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
            } catch {
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
    await runWithLoading(async () => {
        const params = {
            pageNum: currentPage.value,
            pageSize: pageSize.value,
            categoryId: filterCategory.value || undefined,
            status: filterStatus.value === '' ? undefined : filterStatus.value,
            keyword: searchKeyword.value || undefined
        }
        const res = await productApi.getProductList(params)
        if (res.code === 200 && res.data) {
            setRows((res.data.records || []).map(mapProductFromApi), res.data.total)
            filterData()
        } else {
            ElMessage.error(res.message || res.msg || '加载商品列表失败')
        }
    }).catch((e) => {
        console.error('加载商品列表失败', e)
    })
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
  resetPage()
  loadProductList()
}

const resetFilter = () => {
    filterCategory.value = ''
    filterStatus.value = ''
    filterStockStatus.value = ''
    filterTag.value = ''
    searchKeyword.value = ''
    sortOption.value = 'default'
    resetPage()
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
  const requestSeq = ++productDialogRequestSeq
  productDialogRenderKey.value += 1
  dialogType.value = type
  Object.assign(form, getProductFormDefaults())
  coverFileList.value = []
  detailFileList.value = []
  productDialogLoading.value = type === 'edit' && !!row
  dialogVisible.value = true
  
  if (type === 'edit' && row) {
    const productId = row.id
    try {
      const res = await productApi.getProductDetail(productId)
      if (
        requestSeq !== productDialogRequestSeq ||
        !dialogVisible.value ||
        dialogType.value !== 'edit'
      ) {
        return
      }
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
      if (requestSeq === productDialogRequestSeq) {
        console.error('加载商品详情失败', e)
      }
    } finally {
      if (requestSeq === productDialogRequestSeq) {
        productDialogLoading.value = false
      }
    }
  } else {
    // 新增时保留一组常用默认值，录入会快一点
    productDialogLoading.value = false
  }
}

const resetForm = () => {
  productDialogRequestSeq += 1
  productDialogLoading.value = false
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

const handleCoverRemove = (_uploadFile, _uploadFiles) => {
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

const handleRemoveDetail = (_uploadFile, _uploadFiles) => {}

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
  if (productDialogLoading.value) return

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
            } catch {
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
provide('adminProductPageContext', {
    searchKeyword,
    filterCategory,
    filterStatus,
    filterStockStatus,
    filterTag,
    sortOption,
    categoryOptions,
    handleFilter,
    resetFilter,
    selectedRows,
    handleBatchCommand,
    ArrowDown,
    openDialog,
    tableData,
    tableRowClassName,
    loading,
    emptyText,
    handleSelectionChange,
    resolveImageUrl,
    Picture,
    beforeStatusChange,
    handleStatusChange,
    InfoFilled,
    handleDelete,
    total,
    currentPage,
    pageSize,
    handlePageChange,
    dialogVisible,
    dialogType,
    productDialogLoading,
    productDialogRenderKey,
    resetForm,
    form,
    formRef,
    rules,
    coverFileList,
    detailFileList,
    customUploadCover,
    handleCoverUploadSuccess,
    handlePreviewDetail,
    handleCoverRemove,
    beforeUpload,
    customUploadDetail,
    handleDetailUploadSuccess,
    handleRemoveDetail,
    handleSubmit,
    previewVisible,
    previewImageUrl,
    Plus
})

</script>

<style>
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
