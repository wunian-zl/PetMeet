<template>
  <div class="category-container">
    <el-card shadow="never">
      <div class="header-action">
        <h3>商品分类管理 (Category)</h3>
        <el-button type="primary" icon="Plus" @click="openDialog('create')">新增分类</el-button>
      </div>
      
      <el-table :data="tableData" style="width: 100%; margin-top: 20px" row-key="id" v-loading="loading">
        <el-table-column prop="name" label="分类名称" />
        <el-table-column label="图标" width="90">
          <template #default="{ row }">
            <div class="icon-cell">
              <el-icon v-if="isIconComponent(row.icon)" :size="18">
                <component :is="row.icon" />
              </el-icon>
              <img
                v-else-if="row.icon && !isCategoryIconFailed(row.icon)"
                :src="resolveCategoryIcon(row.icon)"
                class="icon-img"
                alt=""
                @error="handleCategoryIconError(row.icon)"
              />
              <span v-else>-</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="sort" label="排序" width="100" sortable />
         <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog('edit', row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogType==='create'?'新增分类':'编辑分类'" width="500px">
        <el-form :model="form" ref="formRef" :rules="rules" label-width="80px">
            <el-form-item label="分类名称" prop="name">
                <el-input v-model="form.name" placeholder="如：狗粮" />
            </el-form-item>

            <el-form-item label="小图标" prop="icon">
                <el-select v-model="form.icon" placeholder="请选择图标" filterable clearable>
                    <el-option
                        v-for="item in iconOptions"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                    >
                        <div class="icon-option">
                            <el-icon v-if="!isIconUrl(item.value)">
                              <component :is="item.value" />
                            </el-icon>
                            <img
                              v-else-if="!isCategoryIconFailed(item.value)"
                              :src="resolveCategoryIcon(item.value)"
                              class="icon-img"
                              alt=""
                              @error="handleCategoryIconError(item.value)"
                            />
                            <span v-else class="icon-placeholder">-</span>
                            <span>{{ item.label }}</span>
                        </div>
                    </el-option>
                </el-select>
            </el-form-item>

            <el-form-item label="排序" prop="sort">
                <el-input-number v-model="form.sort" :min="1" />
            </el-form-item>
            <el-form-item label="状态">
                <el-switch v-model="form.status" />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleSubmit">保存</el-button>
        </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategoryList, createCategory, updateCategory, deleteCategory } from '@/api/product'
import { resolveImageUrl } from '@/utils/image'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogType = ref('create')
const formRef = ref(null)

const form = reactive({
    id: null,
    name: '',
    icon: '',
    sort: 10,
    status: true
})

const iconOptions = [
    { label: '零食（骨头+鱼干）', value: '/category-icons/snack.svg' },
    { label: '狗粮（食盆+骨头）', value: '/category-icons/dog-food.svg' },
    { label: '猫粮（食盆+鱼）', value: '/category-icons/cat-food.svg' },
    { label: '清洁（便便袋）', value: '/category-icons/poop-bag.svg' },
    { label: '玩具（飞盘）', value: '/category-icons/frisbee.svg' },
    { label: '居家（房屋/狗窝）', value: '/category-icons/home.svg' },
    { label: '居家（软垫/枕头）', value: '/category-icons/cushion.svg' },
    { label: '出行（牵引绳/项圈）', value: '/category-icons/leash.svg' },
    { label: '出行（航空箱/提笼）', value: '/category-icons/carrier.svg' },
    { label: '服饰（T恤）', value: '/category-icons/tshirt.svg' },
    { label: '服饰（领结）', value: '/category-icons/bowtie.svg' },
    { label: '医疗保健（急救箱）', value: '/category-icons/medical.svg' },
    { label: '医疗保健（药瓶/胶囊）', value: '/category-icons/pill.svg' },
    { label: '医疗保健（听诊器）', value: '/category-icons/stethoscope.svg' },
    { label: '智能设备（芯片/机器人）', value: '/category-icons/chip.svg' },
    { label: '智能设备（Wi-Fi碗）', value: '/category-icons/wifi-bowl.svg' }
]
const publicBase = import.meta.env.BASE_URL || '/'
const legacyIconPathMap = new Map([
    ['/images/cat-food.png', '/category-icons/cat-food.svg'],
    ['/images/dog-food.png', '/category-icons/dog-food.svg'],
    ['/images/snack.png', '/category-icons/snack.svg'],
    ['/images/clean.png', '/category-icons/poop-bag.svg'],
    ['/images/smart.png', '/category-icons/chip.svg'],
    ['/images/travel.png', '/category-icons/carrier.svg']
])

const failedCategoryIcons = ref(new Set())
const isIconUrl = (icon) => typeof icon === 'string' && (/^(https?:|data:|blob:)/.test(icon) || icon.startsWith('/'))
const iconNameSet = new Set(iconOptions.map(i => i.value))
const isIconComponent = (icon) => icon && iconNameSet.has(icon) && !isIconUrl(icon)

const resolvePublicAsset = (path) => {
    const base = publicBase.endsWith('/') ? publicBase.slice(0, -1) : publicBase
    const normalizedPath = path.startsWith('/') ? path : `/${path}`
    return `${base}${normalizedPath}`
}

const normalizeLocalCategoryIconPath = (icon) => {
    let path = icon.split(/[?#]/)[0].replace(/\\/g, '/')
    path = path.replace(/^\/api(?=\/)/, '').replace(/^\/admin(?=\/)/, '')
    if (!path.startsWith('/')) {
        path = `/${path}`
    }

    return legacyIconPathMap.get(path) || path
}

const isLocalIconHost = (url) => {
    const currentHost = window.location.hostname
    return url.hostname === currentHost || ['localhost', '127.0.0.1', '::1'].includes(url.hostname)
}

const toCanonicalCategoryIcon = (icon) => {
    if (typeof icon !== 'string') return ''

    const value = icon.trim()
    if (!value) return ''

    if (/^(data:|blob:)/.test(value)) return value

    if (/^https?:\/\//.test(value)) {
        try {
            const url = new URL(value)
            const canonicalPath = normalizeLocalCategoryIconPath(url.pathname)
            if (isLocalIconHost(url) && canonicalPath.startsWith('/category-icons/')) {
                return canonicalPath
            }
            return value
        } catch {
            return value
        }
    }

    return normalizeLocalCategoryIconPath(value)
}

const resolveCategoryIcon = (icon) => {
    const canonicalIcon = toCanonicalCategoryIcon(icon)
    if (!canonicalIcon) return ''
    if (/^(https?:|data:|blob:)/.test(canonicalIcon)) return canonicalIcon
    if (canonicalIcon.startsWith('/category-icons/')) return resolvePublicAsset(canonicalIcon)
    if (canonicalIcon.startsWith('/')) return resolveImageUrl(canonicalIcon)
    return canonicalIcon
}

const getCategoryIconKey = (icon) => toCanonicalCategoryIcon(icon) || String(icon || '')
const isCategoryIconFailed = (icon) => failedCategoryIcons.value.has(getCategoryIconKey(icon))
const handleCategoryIconError = (icon) => {
    const next = new Set(failedCategoryIcons.value)
    next.add(getCategoryIconKey(icon))
    failedCategoryIcons.value = next
}

const rules = {
    name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

onMounted(() => {
    loadData()
})

const normalizeRows = (rows) => {
    return rows
        .map(item => ({
            ...item,
            icon: toCanonicalCategoryIcon(item.icon || ''),
            status: item.status === 1
        }))
        .sort((a, b) => (a.sort || 0) - (b.sort || 0))
}

const loadData = async () => {
    loading.value = true
    try {
        const res = await getCategoryList()
        if (res.code === 200 && Array.isArray(res.data)) {
            tableData.value = normalizeRows(res.data)
        } else {
            ElMessage.error(res.message || res.msg || '加载分类失败')
        }
    } catch (e) {
        console.error('加载分类失败', e)
    } finally {
        loading.value = false
    }
}

const openDialog = (type, row) => {
    dialogType.value = type
    if (type === 'edit' && row) {
        Object.assign(form, {
            id: row.id,
            name: row.name,
            icon: row.icon || '',
            sort: row.sort ?? 10,
            status: !!row.status
        })
    } else {
        form.id = null
        form.name = ''
        form.icon = ''
        form.sort = 10
        form.status = true
    }
    dialogVisible.value = true
}

const handleSubmit = () => {
    formRef.value.validate(async (valid) => {
        if (!valid) return
        const payload = {
            id: form.id,
            name: form.name,
            icon: form.icon || '',
            sort: form.sort,
            status: form.status ? 1 : 0
        }

        try {
            if (dialogType.value === 'create') {
                const res = await createCategory(payload)
                if (res.code !== 200) {
                    ElMessage.error(res.message || res.msg || '新增失败')
                    return
                }
                ElMessage.success('新增成功')
            } else {
                const res = await updateCategory(payload)
                if (res.code !== 200) {
                    ElMessage.error(res.message || res.msg || '更新失败')
                    return
                }
                ElMessage.success('更新成功')
            }
            dialogVisible.value = false
            loadData()
        } catch (e) {
            console.error('保存分类失败', e)
        }
    })
}

const handleDelete = (row) => {
    ElMessageBox.confirm('确定删除该分类吗?', '提示', { type: 'warning' })
    .then(async () => {
        try {
            const res = await deleteCategory(row.id)
            if (res.code !== 200) {
                ElMessage.error(res.message || res.msg || '删除失败')
                return
            }
            ElMessage.success('已删除')
            loadData()
        } catch (e) {
            console.error('删除分类失败', e)
        }
    })
}

</script>

<style scoped>
.header-action {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.icon-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 24px;
}

.icon-img {
    width: 18px;
    height: 18px;
    object-fit: contain;
}

.icon-placeholder {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
}

.icon-option {
    display: flex;
    align-items: center;
    gap: 8px;
}
</style>
