<template>
  <div class="banner-container">
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <div class="left">
          <el-button type="primary" :icon="Plus" @click="openCreate">新增广告</el-button>
          <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px" @change="fetchList">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" link icon="Refresh" @click="resetFilter">重置</el-button>
        </div>
        <div class="right">
          <el-tag type="info" effect="plain">商城广告栏</el-tag>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="图片" width="110">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="resolveImageUrl(row.imageUrl)"
              style="width: 72px; height: 48px; border-radius: 10px"
              fit="cover"
              :preview-src-list="[resolveImageUrl(row.imageUrl)]"
              preview-teleported
            />
            <el-tag v-else size="small" type="info" effect="plain">无</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题/内容" min-width="280">
          <template #default="{ row }">
            <div class="title-cell">
              <div class="title">{{ row.title || '-' }}</div>
              <div class="content">{{ row.content || '-' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="跳转配置" min-width="240">
          <template #default="{ row }">
            <div class="jump-cell">
              <div class="jump-line">
                <el-tag size="small" type="success" effect="plain">{{ row.categoryName || '未选择分类' }}</el-tag>
                <el-tag v-if="Number(row.recentDays || 0) > 0" size="small" type="warning" effect="plain">近{{ row.recentDays }}天</el-tag>
                <el-tag v-else size="small" type="info" effect="plain">不限制</el-tag>
              </div>
              <div class="jump-actions">
                <span class="jump-summary">商城筛选结果</span>
                <el-button type="primary" link :icon="View" @click="openPreview(row.linkUrl)">预览</el-button>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              inline-prompt
              active-text="启用"
              inactive-text="禁用"
              @change="(val) => handleToggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该广告吗？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next, sizes"
          :page-sizes="[10, 20, 50]"
          @current-change="fetchList"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" destroy-on-close align-center>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="30" show-word-limit placeholder="对应商城广告大标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" maxlength="60" show-word-limit placeholder="对应商城广告小标题" />
        </el-form-item>
        <el-form-item label="图片" prop="imageUrl">
          <el-upload
            class="banner-uploader"
            :class="{ 'is-full': imageFileList.length >= 1 }"
            :file-list="imageFileList"
            list-type="picture-card"
            :limit="1"
            :on-success="handleImageUploadSuccess"
            :on-remove="handleImageRemove"
            :http-request="customUploadImage"
            :before-upload="beforeUpload"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">建议尺寸：600x600，图片会显示在商城广告栏右侧倾斜框内</div>
        </el-form-item>
        <el-form-item label="商品类型" prop="categoryIds">
          <el-select
            v-model="form.categoryIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="可选择多个分类，如：猫粮、狗粮"
            style="width: 100%"
          >
            <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="上架日期" prop="recentDays">
          <el-radio-group v-model="form.recentDays">
            <el-radio-button :label="0">不限制</el-radio-button>
            <el-radio-button :label="3">近3天</el-radio-button>
            <el-radio-button :label="7">近7天</el-radio-button>
            <el-radio-button :label="15">近15天</el-radio-button>
            <el-radio-button :label="30">近30天</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" style="width: 180px" />
          <span class="sort-tip">数值越小越靠前</span>
        </el-form-item>
        <el-form-item label="启用" prop="enabled">
          <el-switch v-model="form.enabled" inline-prompt active-text="启用" inactive-text="禁用" />
        </el-form-item>

        <el-form-item label="跳转目标">
          <div class="preview-target">
            <div class="preview-copy">
              <div class="preview-title">{{ previewSummary }}</div>
              <div class="preview-description">点击广告后进入商城筛选结果</div>
            </div>
            <el-button :icon="View" @click="openPreview(previewLinkUrl)">打开预览</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { getCategoryList } from '@/api/product'
import { createBanner, deleteBanner, getBannerList, updateBanner, changeBannerStatus } from '@/api/banner'
import { resolveImageUrl } from '@/utils/image'
import { Plus, View } from '@element-plus/icons-vue'

const POSITION = 'SHOP_HERO'
const SLOT = 'hero'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const statusFilter = ref(undefined)

const categoryOptions = ref([])
const categoryNameById = computed(() => {
  const map = new Map()
  for (const c of categoryOptions.value) {
    map.set(Number(c.id), c.name)
  }
  return map
})

const parseLink = (linkUrl) => {
  if (!linkUrl) return { categoryId: null, categoryIds: null, recentDays: null, recentDaysUntil: null }
  const idx = linkUrl.indexOf('?')
  const qs = idx >= 0 ? linkUrl.slice(idx + 1) : linkUrl
  const params = new URLSearchParams(qs)
  const categoryIdRaw = params.get('categoryId')
  const categoryIdsRaw = params.get('categoryIds')
  const recentDaysRaw = params.get('recentDays')
  const recentDaysUntil = params.get('recentDaysUntil')
  return {
    categoryId: categoryIdRaw ? Number(categoryIdRaw) : null,
    categoryIds: categoryIdsRaw
      ? categoryIdsRaw.split(',').map((v) => Number(v)).filter((v) => Number.isFinite(v))
      : null,
    recentDays: recentDaysRaw ? Number(recentDaysRaw) : null,
    recentDaysUntil: recentDaysUntil || null
  }
}

const formatDateYYYYMMDD = (d) => {
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

const buildLinkUrl = (categoryIds, recentDays) => {
  const params = new URLSearchParams()
  const ids = Array.isArray(categoryIds) ? categoryIds.map((v) => Number(v)).filter((v) => Number.isFinite(v)) : []
  if (ids.length > 0) {
    if (ids.length === 1) {
      params.set('categoryId', String(ids[0]))
    }
    params.set('categoryIds', ids.join(','))
    const names = ids.map((id) => categoryNameById.value.get(Number(id))).filter(Boolean)
    if (names.length > 0) params.set('category', names.join('、'))
  }
  const n = Number(recentDays)
  if (Number.isFinite(n) && n > 0) {
    params.set('recentDays', String(n))
    const until = new Date()
    until.setDate(until.getDate() + n)
    params.set('recentDaysUntil', formatDateYYYYMMDD(until))
  }
  return `/mall/list?${params.toString()}`
}

const hydrateRow = (row) => {
  const parsed = parseLink(row.linkUrl)
  const categoryIds = (Array.isArray(parsed.categoryIds) && parsed.categoryIds.length > 0)
    ? parsed.categoryIds
    : (parsed.categoryId != null ? [parsed.categoryId] : [])
  return {
    ...row,
    content: row.keyword || '',
    categoryId: parsed.categoryId,
    categoryIds,
    recentDays: parsed.recentDays,
    recentDaysUntil: parsed.recentDaysUntil,
    categoryName: categoryIds.length > 0
      ? categoryIds.map((id) => categoryNameById.value.get(Number(id))).filter(Boolean).join('、')
      : ''
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getBannerList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      position: POSITION,
      status: statusFilter.value
    })
    if (res.code === 200 && res.data) {
      const records = res.data.records || []
      total.value = res.data.total || 0
      tableData.value = records.map(hydrateRow)
    } else {
      tableData.value = []
      total.value = 0
    }
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSizeChange = () => {
  pageNum.value = 1
  fetchList()
}

const resetFilter = () => {
  statusFilter.value = undefined
  pageNum.value = 1
  fetchList()
}

// 弹窗和表单
const dialogVisible = ref(false)
const dialogMode = ref('create') // create/edit
const saving = ref(false)
const formRef = ref(null)
const imageFileList = ref([])

const form = reactive({
  id: null,
  title: '',
  content: '',
  imageUrl: '',
  categoryIds: [],
  recentDays: 0,
  sort: 0,
  enabled: true
})

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增广告' : '编辑广告'))

const previewLinkUrl = computed(() => buildLinkUrl(form.categoryIds, form.recentDays))

const formatTargetSummary = (categoryIds, recentDays) => {
  const ids = Array.isArray(categoryIds)
    ? categoryIds.map((id) => Number(id)).filter((id) => Number.isFinite(id))
    : []
  const names = ids.map((id) => categoryNameById.value.get(id)).filter(Boolean)
  const categoryText = ids.length === 0
    ? '全部商品'
    : (names.length > 0 ? names.join('、') : `已选${ids.length}个分类`)
  const days = Number(recentDays)
  const dateText = Number.isFinite(days) && days > 0 ? `近${days}天上架` : '不限上架时间'
  return `${categoryText}，${dateText}`
}

const previewSummary = computed(() => formatTargetSummary(form.categoryIds, form.recentDays))

const resolvePreviewUrl = (linkUrl) => {
  const value = String(linkUrl || '').trim()
  if (/^https?:\/\//i.test(value)) return value

  const configuredOrigin = String(import.meta.env.VITE_USER_APP_ORIGIN || '').trim().replace(/\/$/, '')
  const userAppOrigin = configuredOrigin || (
    window.location.port === '5174'
      ? `${window.location.protocol}//${window.location.hostname}:5173`
      : window.location.origin
  )
  const path = value.startsWith('/') ? value : `/${value}`
  return `${userAppOrigin}${path}`
}

const openPreview = (linkUrl) => {
  window.open(resolvePreviewUrl(linkUrl), '_blank', 'noopener,noreferrer')
}

const rules = {
  title: [{ required: true, message: '请填写标题', trigger: 'blur' }],
  content: [{ required: true, message: '请填写内容', trigger: 'blur' }],
  imageUrl: [{ required: true, message: '请上传图片', trigger: 'change' }],
  categoryIds: [{
    validator: (rule, value, callback) => {
      if (!Array.isArray(value) || value.length === 0) {
        callback(new Error('请选择商品类型(分类)'))
        return
      }
      callback()
    },
    trigger: 'change'
  }],
  recentDays: [{
    validator: (rule, value, callback) => {
      const n = Number(value)
      if (!Number.isFinite(n) || n < 0) {
        callback(new Error('请选择上架日期范围'))
        return
      }
      callback()
    },
    trigger: 'change'
  }]
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    title: '',
    content: '',
    imageUrl: '',
    categoryIds: [],
    recentDays: 0,
    sort: 0,
    enabled: true
  })
  imageFileList.value = []
}

const openCreate = () => {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, {
    id: row.id,
    title: row.title || '',
    content: row.keyword || '',
    imageUrl: row.imageUrl || '',
    categoryIds: Array.isArray(row.categoryIds) ? row.categoryIds : (row.categoryId != null ? [row.categoryId] : []),
    recentDays: row.recentDays ?? 0,
    sort: row.sort || 0,
    enabled: row.status === 1
  })
  if (form.imageUrl) {
    imageFileList.value = [
      {
        name: 'image',
        url: resolveImageUrl(form.imageUrl),
        rawUrl: form.imageUrl
      }
    ]
  }
  dialogVisible.value = true
}

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isImage) {
    ElMessage.warning('仅支持上传图片')
    return false
  }
  if (!isLt10M) {
    ElMessage.warning('图片大小不能超过10MB')
    return false
  }
  return true
}

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

const customUploadImage = async (options) => {
  const { file, onSuccess, onError } = options
  try {
    const url = await uploadImage(file, 'shopBannerImage')
    onSuccess({ code: 200, data: url })
  } catch {
    onError(e)
  }
}

const handleImageUploadSuccess = (response, uploadFile) => {
  const url = response?.data
  if (typeof url === 'string' && url.startsWith('/images/')) {
    uploadFile.rawUrl = url
    uploadFile.url = resolveImageUrl(url)
    imageFileList.value = [uploadFile]
    form.imageUrl = url
  }
}

const handleImageRemove = () => {
  form.imageUrl = ''
  imageFileList.value = []
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const categoryIds = Array.isArray(form.categoryIds) ? form.categoryIds.map((v) => Number(v)).filter((v) => Number.isFinite(v)) : []
      const payload = {
        title: form.title,
        position: POSITION,
        slot: SLOT,
        imageUrl: form.imageUrl,
        keyword: form.content,
        linkType: 'internal',
        linkUrl: buildLinkUrl(categoryIds, form.recentDays),
        sort: form.sort,
        status: form.enabled ? 1 : 0
      }
      if (dialogMode.value === 'create') {
        const res = await createBanner(payload)
        if (res.code === 200) {
          ElMessage.success('创建成功')
          dialogVisible.value = false
          fetchList()
        }
      } else {
        const res = await updateBanner(form.id, payload)
        if (res.code === 200) {
          ElMessage.success('更新成功')
          dialogVisible.value = false
          fetchList()
        }
      }
    } catch {
      // 这里交给拦截器统一处理
    } finally {
      saving.value = false
    }
  })
}

const handleToggleStatus = async (row, enabled) => {
  try {
    const res = await changeBannerStatus(row.id, enabled ? 1 : 0)
    if (res.code === 200) {
      ElMessage.success(enabled ? '已启用' : '已禁用')
      fetchList()
    }
  } catch {}
}

const handleDelete = async (row) => {
  try {
    const res = await deleteBanner(row.id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      fetchList()
    }
  } catch {}
}

const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    if (res.code === 200 && Array.isArray(res.data)) {
      categoryOptions.value = res.data
    } else {
      categoryOptions.value = []
    }
  } catch {
    categoryOptions.value = []
  }
}

onMounted(async () => {
  await loadCategories()
  await fetchList()
})
</script>

<style scoped lang="scss">
.banner-container {
  .filter-card {
    margin-bottom: 16px;
  }

  .filter-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;

    .left {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }
  }

  .table-card {
    .pagination-bar {
      margin-top: 16px;
      display: flex;
      justify-content: flex-end;
    }
  }

  .title-cell {
    .title {
      font-weight: 700;
      font-size: 14px;
      color: #111827;
      margin-bottom: 4px;
    }
    .content {
      font-size: 13px;
      color: #6b7280;
    }
  }

  .jump-cell {
    .jump-line {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 6px;
      flex-wrap: wrap;
    }

    .jump-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .jump-summary {
      font-size: 12px;
      color: #6b7280;
    }
  }

  .preview-target {
    width: 100%;
    min-height: 58px;
    padding: 10px 12px;
    border: 1px solid #dcdfe6;
    border-radius: 6px;
    background: #f8fafc;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    box-sizing: border-box;
  }

  .preview-copy {
    min-width: 0;
  }

  .preview-title {
    color: #303133;
    font-size: 14px;
    line-height: 20px;
    word-break: break-word;
  }

  .preview-description {
    margin-top: 2px;
    color: #909399;
    font-size: 12px;
    line-height: 18px;
  }

  .banner-uploader {
    &.is-full {
      :deep(.el-upload--picture-card) {
        display: none;
      }
    }

    :deep(.el-upload--picture-card) {
      width: 96px;
      height: 96px;
      border-radius: 12px;
    }
    :deep(.el-upload-list--picture-card .el-upload-list__item) {
      width: 96px;
      height: 96px;
      border-radius: 12px;
    }
  }

  .upload-tip {
    margin-top: 6px;
    font-size: 12px;
    color: #9ca3af;
  }

  .sort-tip {
    margin-left: 10px;
    font-size: 12px;
    color: #9ca3af;
  }
}
</style>
