<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogType === 'create' ? '新增商品' : '编辑商品'"
    width="600px"
    top="50px"
    destroy-on-close
    @closed="resetForm"
  >
    <el-form
      v-loading="productDialogLoading"
      :model="form"
      ref="formRef"
      :rules="rules"
      label-width="90px"
    >
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
            <el-input-number
              v-model="form.price"
              :min="0.01"
              :precision="2"
              :step="1"
              style="width: 100%"
            />
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
            <el-input-number
              v-model="form.warningStock"
              :min="0"
              :precision="0"
              placeholder="默认10"
              style="width: 100%"
            />
            <div class="tip-text">低于此值时，列表显示“库存紧张”标签</div>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="排序权重" prop="sortWeight">
            <el-input-number
              v-model="form.sortWeight"
              :min="0"
              :precision="0"
              placeholder="人工干预分"
              style="width: 100%"
            />
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
        <div v-if="productDialogLoading" class="upload-loading-placeholder"></div>
        <el-upload
          v-else
          :key="`cover-${productDialogRenderKey}`"
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
          :disabled="productDialogLoading"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
        <div class="tip-text" style="width: 100%">
          第一张会作为商品封面（列表展示用），上传后请点击“保存”才会生效
        </div>
      </el-form-item>

      <!-- 详情图 -->
      <el-form-item label="详情长图" prop="detailImgs">
        <div v-if="productDialogLoading" class="upload-loading-placeholder"></div>
        <el-upload
          v-else
          :key="`detail-${productDialogRenderKey}`"
          v-model:file-list="detailFileList"
          action="#"
          list-type="picture-card"
          multiple
          :http-request="customUploadDetail"
          :on-success="handleDetailUploadSuccess"
          :on-preview="handlePreviewDetail"
          :on-remove="handleRemoveDetail"
          :disabled="productDialogLoading"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
        <div class="tip-text" style="width: 100%">推荐上传多张详情图，支持拖拽排序</div>
      </el-form-item>

      <!-- 描述 -->
      <el-form-item label="文字描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="后期待接入富文本编辑器..."
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <div class="save-actions">
          <el-button type="primary" :loading="productDialogLoading" @click="handleSubmit"
            >保存</el-button
          >
        </div>
      </span>
    </template>
  </el-dialog>

  <!-- 图片预览弹窗 -->
  <el-dialog v-model="previewVisible">
    <img w-full :src="previewImageUrl" alt="Preview Image" style="width: 100%" />
  </el-dialog>
</template>

<script setup>
import { inject } from 'vue'

const {
  categoryOptions,
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
} = inject('adminProductPageContext')
</script>

<style scoped>
.upload-loading-placeholder {
  width: 104px;
  height: 104px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
}
</style>
