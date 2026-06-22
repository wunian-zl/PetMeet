<template>
  <div class="publish-page">
    <div class="publish-container">
      <div class="publish-header">
        <div class="header-main">
          <h2 class="page-title">发布笔记</h2>
          <p class="page-subtitle">记录你和宠物的每个高光瞬间，真实分享更容易被看见。</p>
        </div>
        <div class="header-stats">
          <div class="stat-pill media-pill" aria-label="已选素材数量">
            <span class="label">素材</span>
            <strong>{{ mediaCountLabel }}</strong>
          </div>
          <div class="stat-pill product-pill" aria-label="已关联商品数量">
            <span class="label">关联商品</span>
            <strong>{{ selectedProducts.length }}</strong>
          </div>
        </div>
      </div>

      <div class="publish-content">
    <!-- 左侧：媒体上传 -->
        <div class="media-section">
          <div class="section-label">
            <span>图片/视频</span>
            <div class="type-switch">
              <span 
                class="type-item" 
                :class="{ active: form.type === 'image' }"
                @click="form.type = 'image'"
              >图文</span>
              <span 
                class="type-item" 
                :class="{ active: form.type === 'video' }"
                @click="form.type = 'video'"
              >视频</span>
            </div>
          </div>

          <div v-if="form.type === 'image'" class="uploader-wrapper">
             <el-upload
                action="/api/common/upload/image"
                name="file"
                list-type="picture-card"
                :data="{ biz: 'noteImage' }"
                :file-list="imageFileList"
                :on-preview="handlePictureCardPreview"
                :on-remove="handleImageRemove"
                :on-success="handleImageSuccess"
                :headers="uploadHeaders"
                :limit="9"
                :on-exceed="handleImageExceed"
                class="custom-upload"
             >
                <div class="upload-trigger">
                   <el-icon class="icon"><Plus /></el-icon>
                   <span class="text">上传图片</span>
                </div>
             </el-upload>
             <div class="upload-hint">首图将作为封面</div>
          </div>

          <div v-else class="uploader-wrapper">
             <el-upload
                class="video-uploader"
                drag
                action="/api/common/upload"
                name="file"
                :data="{ biz: 'noteVideo' }"
                :headers="uploadHeaders"
                :limit="1"
                accept="video/*"
                :file-list="videoFileList"
                :on-success="handleVideoUploadSuccess"
                :on-remove="handleVideoRemove"
             >
                <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                <div class="el-upload__text">拖拽视频到此处或 <em>点击上传</em></div>
             </el-upload>
             
             <video v-if="form.videoUrl" class="video-preview" :src="resolveUrl(form.videoUrl)" controls />

             <div class="section-label" style="margin-top: 24px; font-size: 16px;">视频封面</div>
             <div class="cover-mode">
                <el-radio-group v-model="coverMode" size="default">
                   <el-radio-button label="upload">上传封面</el-radio-button>
                   <el-radio-button label="auto">使用视频首帧</el-radio-button>
                </el-radio-group>
             </div>
             <div v-if="coverMode === 'upload'">
               <el-upload
                  action="/api/common/upload/image"
                  name="file"
                  list-type="picture-card"
                  :data="{ biz: 'noteImage' }"
                  :headers="uploadHeaders"
                  :limit="1"
                  :file-list="coverFileList"
                  :on-success="handleCoverUploadSuccess"
                  :on-remove="handleCoverRemove"
                  class="custom-upload cover-upload"
               >
                  <div class="upload-trigger">
                     <el-icon class="icon"><Plus /></el-icon>
                     <span class="text">上传封面</span>
                  </div>
               </el-upload>
             </div>
             <div v-else class="auto-cover">
               <div v-if="autoCoverUrl" class="auto-cover-preview">
                 <img :src="autoCoverUrl" alt="auto-cover" />
               </div>
               <div v-else class="auto-cover-placeholder">未生成封面</div>
               <el-button size="default" :disabled="!videoRawFile" @click="generateAutoCover">
                  生成封面
               </el-button>
             </div>

             <div class="section-label" style="margin-top: 24px; font-size: 16px;">补充图片</div>
             <el-upload
                action="/api/common/upload/image"
                name="file"
                list-type="picture-card"
                :data="{ biz: 'noteImage' }"
                :file-list="imageFileList"
                :on-preview="handlePictureCardPreview"
                :on-remove="handleImageRemove"
                :on-success="handleImageSuccess"
                :headers="uploadHeaders"
                :limit="9"
                :on-exceed="handleImageExceed"
                class="custom-upload"
             >
                <div class="upload-trigger">
                   <el-icon class="icon"><Plus /></el-icon>
                   <span class="text">上传图片</span>
                </div>
             </el-upload>
             <div class="upload-hint">可选上传，最多 9 张</div>
          </div>
        </div>

    <!-- 右侧：表单详情 -->
        <div class="form-section">
           <el-form :model="form" class="clean-form">
              <div class="form-title">标题</div>
              <el-form-item>
                 <el-input
                    v-model="form.title"
                    placeholder="给笔记起个吸引人的标题"
                    class="title-input"
                    maxlength="20"
                 />
              </el-form-item>

              <el-form-item>
                 <el-input 
                    v-model="form.content" 
                    type="textarea" 
                    :rows="6" 
                    placeholder="分享你的真实体验、干货..." 
                    class="content-input"
                    maxlength="1000"
                    show-word-limit
                 />
              </el-form-item>

              <div class="form-row">
                 <el-form-item label="分类" style="flex: 1">
                    <el-select v-model="form.category" placeholder="选择分类" class="clean-select" size="large">
                       <el-option v-for="item in noteCategories" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                    <div class="category-quick-list">
                      <button
                        v-for="item in noteCategories"
                        :key="`quick-${item.value}`"
                        type="button"
                        class="category-chip"
                        :class="{ active: form.category === item.value }"
                        @click="form.category = item.value"
                      >
                        <span class="text">{{ item.label }}</span>
                      </button>
                    </div>
                    <div class="category-tip">仓鼠、鸟类、兔子、爬宠等可选择「异宠日常」</div>
                 </el-form-item>
              </div>

               <el-form-item label="标签">
                  <el-select
                    v-model="form.tags"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="添加标签"
                    class="clean-select"
                    style="width: 100%"
                    size="large"
                  >
                    <el-option v-for="item in tagSuggestions" :key="item" :label="item" :value="item" />
                  </el-select>
               </el-form-item>

               <el-form-item label="关联商品">
                  <el-button v-if="selectedProducts.length === 0" class="add-product-btn" plain icon="Plus" @click="openProductPicker">添加商品</el-button>
                  <div v-else class="selected-product-list">
                     <div v-for="prod in selectedProducts" :key="prod.id" class="mini-product-card">
                        <img :src="prod.coverImg" />
                        <span class="name">{{ prod.name }}</span>
                        <el-icon class="remove-icon" @click="removeProduct(prod.id)"><Close /></el-icon>
                     </div>
                     <el-button size="default" circle icon="Plus" @click="openProductPicker" style="margin-left: 8px;" />
                  </div>
               </el-form-item>

               <div class="form-actions">
                  <el-button type="primary" class="publish-btn" @click="handlePublish" :loading="submitting">发布笔记</el-button>
               </div>
           </el-form>
        </div>
      </div>
    </div>

          <!-- 商品选择器 -->
    <el-dialog v-model="productDialogVisible" title="关联商品" width="800px" align-center destroy-on-close class="clean-dialog">
       <div class="search-bar">
          <el-input v-model="productKeyword" placeholder="搜索商品" @keyup.enter="searchProducts" prefix-icon="Search" size="large" />
          <el-button @click="searchProducts" size="large">搜索</el-button>
       </div>
       <div class="product-grid" v-loading="productLoading">
          <div v-for="prod in productList" :key="prod.id" class="p-card" :class="{ active: isSelected(prod.id) }" @click="toggleSelect(prod)">
             <img :src="prod.coverImg" />
             <div class="p-info">
                <div class="p-name">{{ prod.name }}</div>
                <div class="p-price">¥{{ prod.price }}</div>
             </div>
             <div class="check-mark" v-if="isSelected(prod.id)">
                <el-icon><Select /></el-icon>
             </div>
          </div>
       </div>
       <div class="load-more">
          <el-button text bg @click="loadMoreProducts" :disabled="!productHasMore" size="default">
             {{ productHasMore ? '加载更多' : '到底了' }}
          </el-button>
       </div>
    </el-dialog>

    <el-dialog v-model="previewVisible" align-center>
      <img :src="previewImageUrl" style="width: 100%; border-radius: 12px;" />
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Plus, Close, Search, UploadFilled, Select } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { getImageUrl } from '@/utils/image'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const SEEDED_NOTE_ORDER_IDS_KEY = 'petmeet:seeded-note-order-ids'

const noteCategories = [
  { label: '猫咪日常', value: 'cat' },
  { label: '狗狗生活', value: 'dog' },
  { label: '异宠日常', value: 'other' },
  { label: '好物测评', value: 'review' },
  { label: '科普知识', value: 'knowledge' }
]

const tagSuggestions = ['喂养', '测评', '训练', '洗护', '健康', '日常']

const form = reactive({
  title: '',
  content: '',
  category: '',
  tags: [],
  type: 'image',
  images: [],
  videoUrl: '',
  coverImg: ''
})

const submitting = ref(false)

// 上传相关
const previewImageUrl = ref('')
const previewVisible = ref(false)
const imageFileList = ref([])
const coverFileList = ref([])
const videoFileList = ref([])
const videoRawFile = ref(null)
const coverMode = ref('upload')
const autoCoverGenerated = ref(false)

const uploadHeaders = computed(() => ({
  Authorization: userStore.token || localStorage.getItem('token') || ''
}))

const resolveUrl = (url) => getImageUrl(url)
const autoCoverUrl = computed(() => (form.coverImg ? getImageUrl(form.coverImg) : ''))

const handleImageSuccess = (response, uploadFile, uploadFiles) => {
  if (response.code === 200) {
    uploadFile.url = getImageUrl(response.data)
    uploadFile.rawUrl = response.data
    syncImagesList(uploadFiles)
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleImageRemove = (uploadFile, uploadFiles) => {
  syncImagesList(uploadFiles)
}

const handleImageExceed = () => {
  ElMessage.warning('最多上传 9 张图片')
}

const syncImagesList = (files) => {
  form.images = files
    .map((f) => f.response?.data || f.rawUrl || f.url)
    .filter(Boolean)
}

const handleVideoUploadSuccess = (response, uploadFile, uploadFiles) => {
  if (response.code === 200) {
    uploadFile.url = getImageUrl(response.data)
    uploadFile.rawUrl = response.data
    form.videoUrl = response.data
    videoFileList.value = uploadFiles
    videoRawFile.value = uploadFile.raw || null
    if (coverMode.value === 'auto') {
      generateAutoCover()
    }
    ElMessage.success('视频上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleVideoRemove = () => {
  form.videoUrl = ''
  form.coverImg = ''
  videoFileList.value = []
  videoRawFile.value = null
  autoCoverGenerated.value = false
  coverFileList.value = []
}

const handleCoverUploadSuccess = (response, uploadFile, uploadFiles) => {
  if (response.code === 200) {
    uploadFile.url = getImageUrl(response.data)
    uploadFile.rawUrl = response.data
    form.coverImg = response.data
    coverFileList.value = uploadFiles
    autoCoverGenerated.value = false
  }
}

const handleCoverRemove = () => {
  form.coverImg = ''
  coverFileList.value = []
  autoCoverGenerated.value = false
}

const captureVideoFrame = (file) => {
  return new Promise((resolve, reject) => {
    try {
      const video = document.createElement('video')
      const url = URL.createObjectURL(file)
      video.src = url
      video.muted = true
      video.playsInline = true
      video.preload = 'metadata'
      video.onloadeddata = () => {
        video.currentTime = 0
      }
      video.onseeked = () => {
        const canvas = document.createElement('canvas')
        canvas.width = video.videoWidth || 720
        canvas.height = video.videoHeight || 1280
        const ctx = canvas.getContext('2d')
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
        canvas.toBlob((blob) => {
          URL.revokeObjectURL(url)
          if (blob) {
            resolve(blob)
          } else {
            reject(new Error('capture failed'))
          }
        }, 'image/jpeg', 0.9)
      }
      video.onerror = () => {
        URL.revokeObjectURL(url)
        reject(new Error('video load failed'))
      }
    } catch (e) {
      reject(e)
    }
  })
}

const generateAutoCover = async () => {
  if (!videoRawFile.value) {
    ElMessage.warning('请先上传视频')
    return false
  }
  try {
    const blob = await captureVideoFrame(videoRawFile.value)
    const file = new File([blob], 'video-cover.jpg', { type: blob.type || 'image/jpeg' })
    const formData = new FormData()
    formData.append('file', file)
    const res = await request({
      url: '/common/upload/image',
      method: 'post',
      params: { biz: 'noteImage' },
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === 200 && res.data) {
      form.coverImg = res.data
      coverFileList.value = [{ name: 'auto-cover', url: getImageUrl(res.data), rawUrl: res.data }]
      autoCoverGenerated.value = true
      return true
    }
    ElMessage.error(res.message || res.msg || '封面生成失败')
  } catch (e) {
    ElMessage.error('封面生成失败')
  }
  return false
}

const handlePictureCardPreview = (uploadFile) => {
  previewImageUrl.value = uploadFile.url
  previewVisible.value = true
}

// 关联商品选择
const productDialogVisible = ref(false)
const productList = ref([])
const productLoading = ref(false)
const productKeyword = ref('')
const productPage = ref(1)
const productPageSize = 12
const productHasMore = ref(true)
const selectedProducts = ref([])
const mediaCountLabel = computed(() => {
  if (form.type === 'video') {
    const count = (form.videoUrl ? 1 : 0) + (Array.isArray(form.images) ? form.images.length : 0)
    return `${count}`
  }
  return `${Array.isArray(form.images) ? form.images.length : 0}/9`
})

const isSelected = (id) => selectedProducts.value.some((p) => p.id === id)

const toggleSelect = (product) => {
  if (isSelected(product.id)) {
    selectedProducts.value = selectedProducts.value.filter((p) => p.id !== product.id)
  } else {
    selectedProducts.value.push(product)
  }
}

const removeProduct = (id) => {
  selectedProducts.value = selectedProducts.value.filter((p) => p.id !== id)
}

const fetchProducts = async (reset = false) => {
  if (productLoading.value) return
  if (reset) {
    productPage.value = 1
    productList.value = []
    productHasMore.value = true
  }
  if (!productHasMore.value) return

  productLoading.value = true
  try {
    const res = await request.get('/product/list', {
      params: {
        pageNum: productPage.value,
        pageSize: productPageSize,
        keyword: productKeyword.value ? productKeyword.value.trim() : undefined
      }
    })

    const records = res?.records || []
    const total = res?.total || records.length

    const mapped = records.map((item) => ({
      ...item,
      coverImg: getImageUrl(item.coverImg)
    }))

    productList.value = productList.value.concat(mapped)
    productHasMore.value = productList.value.length < total
    productPage.value += 1
  } finally {
    productLoading.value = false
  }
}

const loadMoreProducts = () => {
  fetchProducts(false)
}

const searchProducts = () => {
  fetchProducts(true)
}

const openProductPicker = () => {
  productDialogVisible.value = true
  if (productList.value.length === 0) {
    fetchProducts(true)
  }
}

const parseProductIdsFromQuery = () => {
  const result = []
  const appendIds = (value) => {
    if (value == null) return
    const rawList = Array.isArray(value) ? value : [value]
    rawList.forEach((raw) => {
      String(raw)
        .split(',')
        .map((part) => Number(part.trim()))
        .filter((id) => Number.isInteger(id) && id > 0)
        .forEach((id) => result.push(id))
    })
  }

  appendIds(route.query.productIds)
  appendIds(route.query.productId)
  return Array.from(new Set(result))
}

const preselectProducts = async () => {
  const productIds = parseProductIdsFromQuery()
  if (productIds.length === 0) return

  try {
    const detailResults = await Promise.allSettled(
      productIds.map((id) => request.get(`/product/detail/${id}`))
    )
    const products = detailResults
      .filter((item) => item.status === 'fulfilled' && item.value)
      .map((item) => item.value)
      .map((res) => ({
        id: res.id,
        name: res.name,
        price: res.price,
        coverImg: getImageUrl(res.coverImg)
      }))

    if (products.length > 0) {
      selectedProducts.value = products
    }
  } catch (e) {}
}

const prefillTagsFromQuery = () => {
  const raw = route.query.tags
  if (!raw) return
  const text = Array.isArray(raw) ? raw.join(',') : String(raw)
  const tags = text.split(',').map((t) => t.trim()).filter(Boolean)
  if (tags.length === 0) return
  const merged = Array.from(new Set([...(Array.isArray(form.tags) ? form.tags : []), ...tags]))
  form.tags = merged.slice(0, 10)
}

const getRouteOrderId = () => {
  const id = Number(route.query.orderId)
  if (!Number.isInteger(id) || id <= 0) return null
  return id
}

const markOrderSeedNotePublished = () => {
  const orderId = getRouteOrderId()
  if (!orderId) return
  try {
    const raw = localStorage.getItem(SEEDED_NOTE_ORDER_IDS_KEY)
    const parsed = raw ? JSON.parse(raw) : []
    const list = Array.isArray(parsed) ? parsed : []
    const ids = new Set(
      list
        .map((id) => Number(id))
        .filter((id) => Number.isInteger(id) && id > 0)
    )
    ids.add(orderId)
    localStorage.setItem(SEEDED_NOTE_ORDER_IDS_KEY, JSON.stringify(Array.from(ids)))
  } catch (e) {
    localStorage.setItem(SEEDED_NOTE_ORDER_IDS_KEY, JSON.stringify([orderId]))
  }
}

onMounted(() => {
  preselectProducts()
  prefillTagsFromQuery()
  fetchProducts(true)
})

watch(() => form.type, (type) => {
  if (type === 'image') {
    form.videoUrl = ''
    form.coverImg = ''
    videoFileList.value = []
    coverFileList.value = []
    videoRawFile.value = null
    autoCoverGenerated.value = false
    coverMode.value = 'upload'
  } else {
    form.coverImg = ''
    coverFileList.value = []
    autoCoverGenerated.value = false
  }
})

watch(coverMode, (mode) => {
  if (mode === 'auto' && form.videoUrl && !form.coverImg) {
    generateAutoCover()
  }
  if (mode === 'upload') {
    if (autoCoverGenerated.value) {
      form.coverImg = ''
      coverFileList.value = []
      autoCoverGenerated.value = false
    }
  }
})

// 发布笔记
const handlePublish = async () => {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写标题和正文')
    return
  }
  if (!form.category) {
    ElMessage.warning('请选择分类')
    return
  }
  if (form.type === 'image' && form.images.length === 0) {
    ElMessage.warning('请上传图片')
    return
  }
  if (form.type === 'video') {
    if (!form.videoUrl) {
      ElMessage.warning('请上传视频')
      return
    }
    if (coverMode.value === 'auto' && !form.coverImg) {
      const ok = await generateAutoCover()
      if (!ok) return
    }
    if (!form.coverImg) {
      ElMessage.warning('请上传或生成视频封面')
      return
    }
  }

  submitting.value = true
  try {
    const tags = Array.isArray(form.tags)
      ? form.tags.map((t) => String(t).trim()).filter(Boolean)
      : []

    const payload = {
      title: form.title.trim(),
      content: form.content.trim(),
      category: form.category,
      tags,
      coverImg: form.type === 'video' ? form.coverImg : (form.images[0] || ''),
      images: form.images,
      type: form.type,
      videoUrl: form.type === 'video' ? form.videoUrl : '',
      productIds: selectedProducts.value.map((p) => p.id)
    }

    await request.post('/note/publish', payload)
    markOrderSeedNotePublished()
    ElMessage.success('已发布！')
    router.push({ path: '/profile', query: { tab: 'notes' } })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.publish-page {
  --love-primary: rgb(255, 107, 129);
  --love-primary-deep: #ff5c74;
  --love-text: #5b4349;
  --love-text-soft: #94727a;
  background: #fff;
  min-height: 100vh;
  padding: 10px 12px;
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: var(--love-text);
}

.publish-container {
  max-width: 920px;
  margin: 0 auto;
  background: #fff;
  border-radius: 18px;
  padding: 18px 20px;
  box-shadow: 0 6px 18px rgba(149, 131, 117, 0.08);
  height: calc(100vh - 20px);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.publish-header {
  margin-bottom: 14px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.header-main {
  min-width: 0;
}

.header-stats {
  display: flex;
  gap: 8px;
  align-items: center;
}

.stat-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-width: 88px;
  height: 34px;
  box-sizing: border-box;
  padding: 0 11px;
  border-radius: 8px;
  border: 1px solid rgba(91, 67, 73, 0.09);
  background: #fff;
  color: #6d5960;
  box-shadow: 0 1px 0 rgba(91, 67, 73, 0.025);
  font-variant-numeric: tabular-nums;
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;

  &::before {
    content: '';
    width: 6px;
    height: 6px;
    border-radius: 50%;
    flex: 0 0 auto;
    background: var(--stat-accent);
    box-shadow: 0 0 0 3px var(--stat-tint);
  }

  &:hover {
    border-color: rgba(91, 67, 73, 0.14);
    background: #fffdfc;
    box-shadow: 0 4px 12px rgba(91, 67, 73, 0.06);
  }
  
  .label {
    font-size: 13px;
    line-height: 1;
    color: #7f6870;
    font-weight: 500;
  }
  strong {
    margin-left: auto;
    font-size: 16px;
    line-height: 1;
    color: #43363a;
    font-weight: 700;
  }
}

.media-pill {
  --stat-accent: #7fa4bd;
  --stat-tint: rgba(127, 164, 189, 0.16);
}

.product-pill {
  --stat-accent: #5ca89a;
  --stat-tint: rgba(92, 168, 154, 0.14);
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--love-text);
  line-height: 1.2;
}

.page-subtitle {
  margin: 4px 0 0;
  color: var(--love-text-soft);
  font-size: 13px;
  line-height: 1.6;
}

.publish-content {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
  overflow: hidden;
  
  @media (max-width: 960px) {
    grid-template-columns: 1fr;
  }
}

/* 公共区块容器样式，这里去掉了边框让画面更轻一点 */
.media-section,
.form-section {
  background: #fff;
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
}

/* 媒体区单独样式 */
.media-section {
  .section-label {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
    font-size: 15px;
    color: var(--love-text);
    font-weight: 600;
  }
}

.type-switch {
  display: flex;
  background: #f5f5f5;
  border-radius: 18px;
  padding: 3px;
  
  .type-item {
     padding: 5px 12px;
     font-size: 13px;
     border-radius: 14px;
     cursor: pointer;
     color: #666;
     transition: all 0.3s ease;
     
     &.active {
       background: #fff;
       color: #333;
       font-weight: 600;
       box-shadow: 0 2px 8px rgba(0,0,0,0.05);
     }
  }
}

.custom-upload {
  :deep(.el-upload--picture-card) {
    width: 88px;
    height: 88px;
    background-color: #fafafa;
    border: 1px dashed #e0e0e0;
    border-radius: 10px;
    transition: all 0.3s;
    
    &:hover {
      border-color: #ccc;
      background-color: #f5f5f5;
    }
  }
  
  :deep(.el-upload-list--picture-card .el-upload-list__item) {
    width: 88px;
    height: 88px;
    border-radius: 10px;
  }
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #999;
  
  .icon { font-size: 28px; margin-bottom: 6px; color: #bbb; }
  .text { font-size: 13px; }
}

.upload-hint {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
  background: #fdfbf8;
  padding: 6px 10px;
  border-radius: 8px;
  display: inline-block;
}

.video-uploader {
  :deep(.el-upload-dragger) {
     padding: 24px 16px;
     background: #fafafa;
     border: 1px dashed #e0e0e0;
     border-radius: 12px;
     transition: all 0.3s;
     
     &:hover {
       border-color: #ccc;
       background-color: #f5f5f5;
     }
  }
  .el-icon--upload {
    font-size: 48px;
    color: #ddd;
    margin-bottom: 10px;
  }
  .el-upload__text {
    color: #666;
    font-size: 14px;
    em {
      color: #333;
      font-weight: 600;
      font-style: normal;
    }
  }
}

.video-preview {
  width: 100%;
  margin-top: 10px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.cover-mode {
  margin: 10px 0;
}

.auto-cover {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.auto-cover-preview,
.auto-cover-placeholder {
  width: 88px;
  height: 88px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #eee;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #999;
}

.auto-cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 表单样式 */
.form-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--love-text);
  margin-bottom: 8px;
}

.clean-form {
  :deep(.el-form-item) {
    margin-bottom: 14px;
  }

  :deep(.el-form-item__label) {
    color: var(--love-text);
    font-weight: 600;
    font-size: 14px; 
    line-height: 20px;
    padding-bottom: 4px;
  }
  
  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner) {
     box-shadow: none !important;
     background: #fafafa;
     border: 1px solid transparent;
     border-radius: 12px;
     padding: 8px 12px;
     transition: all 0.3s ease;
     
     &:hover {
       background: #f5f5f5;
     }
     
     &.is-focus, &:focus {
       background: #fff;
       border-color: #ddd;
       box-shadow: 0 0 0 2px rgba(0,0,0,0.03) !important;
     }
  }
  
  .title-input {
      :deep(.el-input__wrapper) {
         padding: 6px 12px;
         background: transparent;
         border-bottom: 1px solid #eee;
         border-radius: 0;
         
         &:hover {
            border-bottom-color: #ccc;
         }
         &.is-focus {
            border-bottom-color: #333;
         }
      }

      :deep(.el-input__inner) {
         font-size: 16px;
         font-weight: 600;
         color: var(--love-text);
         height: 36px;
         
         &::placeholder {
           color: #bbb;
           font-weight: 500;
         }
      }
  }
  
  .content-input {
     :deep(.el-textarea__inner) {
        font-size: 14px;
        line-height: 1.55;
        min-height: 120px;
        padding: 12px;
        background: #fafafa;
        border-radius: 12px;
        border: none;
        resize: none;
        
        &::placeholder {
           color: #bbb;
        }
        
        &:focus {
           background: #fff;
           box-shadow: 0 0 0 1px #eee inset;
        }
     }
  }
}

.clean-select {
  :deep(.el-input__wrapper) {
     background: #fafafa;
     border-radius: 12px;
     border: none;
     box-shadow: none !important;
     padding: 4px 12px;
     height: 36px;
  }
}

.category-quick-list {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-chip {
  appearance: none;
  border: 1px solid #f0f0f0;
  background: #fff;
  color: var(--love-text-soft);
  border-radius: 20px;
  padding: 6px 12px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  
  .text {
    font-size: 13px;
    font-weight: 500;
  }
  
  &:hover {
    background: #f9f9f9;
    border-color: #e0e0e0;
  }
  
  &.active {
    border-color: var(--love-primary);
    background: var(--love-primary);
    color: #fff;
    box-shadow: 0 4px 10px rgba(228, 107, 132, 0.22);
  }
}

.category-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #999;
}

.add-product-btn {
  border-radius: 12px;
  padding: 8px 14px;
  font-size: 13px;
  height: 34px;
  border-color: #eee;
  color: var(--love-text-soft);
  background: #fff;
  
  &:hover {
    border-color: #ecc6cf;
    color: var(--love-text);
    background: #fafafa;
  }
}

.selected-product-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.mini-product-card {
  display: flex;
  align-items: center;
  background: linear-gradient(180deg, #fff 0%, #fffaf4 100%);
  border: 1px solid #f0e0cf;
  border-radius: 10px;
  padding: 6px 12px 6px 6px;
  box-shadow: 0 2px 8px rgba(191, 112, 44, 0.08);
  
  img {
    width: 36px;
    height: 36px;
    object-fit: cover;
    border-radius: 6px;
    margin-right: 10px;
  }
  .name {
    font-size: 13px;
    color: #674b33;
    max-width: 160px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-right: 8px;
  }
  .remove-icon {
    font-size: 14px;
    cursor: pointer;
    color: #c6a489;
    transition: color 0.2s;
    &:hover {
      color: #bf702c;
    }
  }
}

.form-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  
  .publish-btn {
     width: 180px;
     height: 40px;
     font-size: 15px;
     font-weight: 600;
     border-radius: 20px;
     background: rgb(255, 107, 129);
     border: none;
     color: #fff;
     letter-spacing: 0.5px;
     box-shadow: 0 6px 14px rgba(255, 107, 129, 0.24);
     transition: all 0.3s;
     
     &:hover {
       transform: translateY(-1px);
       box-shadow: 0 8px 16px rgba(255, 107, 129, 0.28);
       background: #ff5c74;
     }
     &:active {
       transform: translateY(0);
       background: #f5536e;
     }
  }
}

/* 弹窗样式 */
.clean-dialog {
  :deep(.el-dialog) {
    border-radius: 20px;
    overflow: hidden;
    box-shadow: 0 16px 48px rgba(0,0,0,0.12);
  }
  :deep(.el-dialog__header) {
    border-bottom: 1px solid #f0f0f0;
    margin-right: 0;
    padding: 20px 24px;
  }
  :deep(.el-dialog__title) {
    font-size: 18px;
    font-weight: 600;
  }
  :deep(.el-dialog__body) {
    padding: 24px;
  }
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  :deep(.el-input__wrapper) {
    border-radius: 12px;
    box-shadow: none;
    border: 1px solid #e0e0e0;
    padding: 8px 12px;
    height: 42px;
  }
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  max-height: 450px;
  overflow-y: auto;
  padding: 4px;
}

.p-card {
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 10px;
  cursor: pointer;
  position: relative;
  background: #fff;
  transition: all 0.2s ease;
  
  &:hover {
    border-color: #ddd;
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0,0,0,0.05);
  }

  &.active {
    border-color: var(--love-primary);
    background: #fff;
    box-shadow: 0 0 0 2px rgba(228, 107, 132, 0.22);
  }
  
  img { width: 100%; height: 140px; object-fit: cover; border-radius: 8px; }
  .p-info { margin-top: 10px; }
  .p-name {
    font-size: 14px;
    margin-bottom: 6px;
    color: #333;
    line-height: 1.4;
    height: 40px; /* Force 2 lines approx */
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
  .p-price { font-size: 15px; font-weight: 600; color: var(--love-primary-deep); }
  
  .check-mark {
    position: absolute;
    top: 10px;
    right: 10px;
    background: var(--love-primary);
    color: #fff;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 2px 6px rgba(0,0,0,0.2);
  }
}

.load-more {
  text-align: center;
  margin-top: 24px;
  .el-button {
    font-size: 14px;
    color: #666;
  }
}

@media (max-width: 960px) {
  .publish-container {
    padding: 16px;
    height: auto;
  }
  
  .publish-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .publish-content {
    grid-template-columns: 1fr;
    gap: 16px;
    overflow: visible;
  }

  .media-section,
  .form-section {
    overflow: visible;
    padding-right: 0;
  }
}

@media (max-width: 600px) {
  .publish-page {
    padding: 8px;
  }
  .publish-container {
    padding: 14px;
    border-radius: 16px;
  }
  .page-title {
    font-size: 20px;
  }
  .header-stats {
    width: 100%;
  }
  .stat-pill {
    flex: 1;
    min-width: 0;
  }
  .form-actions .publish-btn {
    width: 100%;
  }
  .category-quick-list {
     display: grid;
     grid-template-columns: 1fr 1fr;
  }
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}
</style>
