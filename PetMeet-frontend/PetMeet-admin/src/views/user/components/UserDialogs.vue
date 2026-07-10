<template>
  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="500px">
    <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
      <el-form-item label="用户头像" style="margin-bottom: 20px">
        <div style="display: flex; align-items: center; gap: 12px">
          <el-avatar
            :src="resolveImageUrl(form.avatar)"
            :size="60"
            fit="cover"
            class="user-avatar"
          />

          <template v-if="form.id && isEditingSelf">
            <el-upload
              :action="avatarUploadAction"
              :headers="avatarUploadHeaders"
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              :on-success="handleAvatarUploadSuccess"
              :on-error="handleAvatarUploadError"
            >
              <el-button size="small">更换头像</el-button>
            </el-upload>
            <el-button size="small" type="danger" plain @click="form.avatar = ''"
              >移除头像</el-button
            >
          </template>

          <template v-else>
            <el-button size="small" :disabled="!form.id" @click="handleHarmonizeAvatar"
              >和谐头像</el-button
            >
          </template>
        </div>
        <div
          v-if="form.id && !isEditingSelf"
          style="margin-top: 6px; font-size: 12px; color: #909399; line-height: 1.2"
        >
          管理端仅支持“和谐头像”（清空头像），不能直接替用户更换头像。
        </div>
      </el-form-item>
      <el-form-item label="用户昵称" prop="username">
        <el-input v-model="form.username" />
      </el-form-item>
      <el-form-item v-if="!form.id" label="登录密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          placeholder="8-18位，同时包含字母和数字"
        />
      </el-form-item>
      <el-form-item label="手机号码" prop="phone">
        <el-input v-model="form.phone" :disabled="!!form.id && !isEditingSelf" />
      </el-form-item>
      <el-form-item label="电子邮箱" prop="email">
        <el-input v-model="form.email" :disabled="!!form.id && !isEditingSelf" />
      </el-form-item>
      <el-form-item label="用户角色" prop="role">
        <el-radio-group v-model="form.role">
          <el-radio value="user">普通用户</el-radio>
          <el-radio value="admin">管理员</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="账号状态">
        <el-radio-group v-model="form.status">
          <el-radio :label="true">正常</el-radio>
          <el-radio :label="false">封禁</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="用户标签" prop="tags">
        <el-input v-model="form.tags" placeholder="多个标签请用逗号分隔 (e.g. 活跃,爱猫)" />
        <div style="font-size: 12px; color: #909399; line-height: 1.2; margin-top: 4px">
          提示: 可参考详情页的数据来打标
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确认</el-button>
      </span>
    </template>
  </el-dialog>
  <!-- 封禁原因弹窗 -->
  <el-dialog v-model="banDialogVisible" title="封禁用户" width="400px">
    <el-form>
      <el-form-item label="封禁原因">
        <el-input
          v-model="banForm.reason"
          type="textarea"
          :rows="3"
          placeholder="请输入封禁原因（如：发布违规广告）"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="banDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitBan">确认封禁</el-button>
      </span>
    </template>
  </el-dialog>

  <!-- 用户详情弹窗 -->
  <el-dialog v-model="detailDialogVisible" title="用户详情" width="600px">
    <div v-if="currentUser" class="user-detail-content">
      <!-- 顶部信息 -->
      <div class="detail-header">
        <el-avatar
          :src="resolveImageUrl(currentUser.avatar)"
          :size="80"
          fit="cover"
          class="user-avatar"
        />
        <div class="detail-basic">
          <div class="d-name">
            {{ currentUser.username }}
            <el-tag size="small" :type="currentUser.role === 'admin' ? 'danger' : 'info'">{{
              currentUser.role === 'admin' ? 'admin' : 'user'
            }}</el-tag>
          </div>
          <div class="d-info">
            ID: {{ currentUser.id }} | Phone: {{ currentUser.phone }} | Email:
            {{ currentUser.email || '-' }}
          </div>
          <div class="d-tags" v-if="currentUser.tags">
            <el-tag
              v-for="t in currentUser.tags.split(',')"
              :key="t"
              size="small"
              type="success"
              style="margin-right: 5px"
              >{{ t }}</el-tag
            >
          </div>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="detail-stats">
        <div class="stat-item">
          <div class="s-val">{{ currentUser.stats?.postCount || 0 }}</div>
          <div class="s-label">发布内容</div>
        </div>
        <div class="stat-item">
          <div class="s-val">{{ currentUser.stats?.orderCount || 0 }}</div>
          <div class="s-label">订单数</div>
        </div>
        <div class="stat-item">
          <div class="s-val">￥{{ currentUser.stats?.totalSpent || 0 }}</div>
          <div class="s-label">总消费</div>
        </div>
        <!-- 新增统计项 -->
        <div class="stat-item">
          <div class="s-val">{{ currentUser.stats?.totalLikeCount || 0 }}</div>
          <div class="s-label">{{ labelText.totalLikes }}</div>
        </div>
        <div class="stat-item">
          <div class="s-val">{{ currentUser.stats?.collectNoteCount || 0 }}</div>
          <div class="s-label">{{ labelText.collections }}</div>
        </div>
        <div class="stat-item">
          <div class="s-val">{{ currentUser.stats?.commentCount || 0 }}</div>
          <div class="s-label">评论数</div>
        </div>
        <div class="stat-item">
          <div class="s-val">{{ currentUser.stats?.commentLikeCount || 0 }}</div>
          <div class="s-label">评论获赞</div>
        </div>
        <div class="stat-item">
          <div class="s-val" style="color: #f56c6c">
            {{ currentUser.stats?.violationCount || 0 }}
          </div>
          <div class="s-label">违规次数</div>
        </div>
      </div>

      <!-- 活动占位 -->
      <div class="detail-activity">
        <div class="section-title">最近动态(Mock)</div>
        <ul class="activity-list">
          <li><span class="time">2023-06-12 10:00</span> <span class="action">登录了系统</span></li>
          <li>
            <span class="time">2023-06-11 15:30</span>
            <span class="action">发布了笔记《我家猫咪真可爱》</span>
          </li>
          <li>
            <span class="time">2023-06-10 09:20</span>
            <span class="action">购买了商品[皇家猫粮 2kg]</span>
          </li>
        </ul>
      </div>

      <!-- 安全操作 -->
      <div
        class="detail-actions"
        style="margin-top: 20px; border-top: 1px solid #eee; padding-top: 15px"
      >
        <div class="section-title" style="margin-bottom: 10px">安全操作</div>
        <el-button type="warning" size="small" plain @click="handleResetPwd">重置密码</el-button>
        <el-button type="danger" size="small" plain @click="handleForceLogout">强制下线</el-button>
      </div>
    </div>
  </el-dialog>

  <!-- 标签规则配置弹窗 -->
  <el-dialog v-model="tagConfigDialogVisible" title="自动化标签规则配置" width="450px">
    <el-form :model="tagConfig" label-width="120px">
      <el-form-item label="萌新判定 (天)">
        <el-input-number v-model="tagConfig.newDay" :min="1" :max="365" />
        <div class="form-tip">注册于该天数内的用户展示“萌新”标签</div>
      </el-form-item>
      <el-form-item label="活跃判定 (天)">
        <el-input-number v-model="tagConfig.activeDay" :min="1" :max="30" />
        <div class="form-tip">在该天数内有登录记录的用户展示“活跃”标签</div>
      </el-form-item>
      <el-form-item label="内容达人 (条)">
        <el-input-number v-model="tagConfig.influencerPost" :min="1" :max="9999" />
        <div class="form-tip">发布内容超过此数量的用户展示“内容达人”标签</div>
      </el-form-item>
      <el-form-item label="消费达人 (元)">
        <el-input-number v-model="tagConfig.bigSpenderAmount" :min="1" :max="100000" :step="100" />
        <div class="form-tip">累计消费超过此金额的用户展示“消费达人”标签</div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="tagConfigDialogVisible = false">完成配置</el-button>
    </template>
  </el-dialog>

  <!-- 违规详情弹窗 -->
  <el-dialog v-model="violationDialogVisible" title="违规记录明细" width="700px">
    <div v-if="currentViolationUser" style="margin-bottom: 20px">
      <div
        style="
          display: flex;
          align-items: center;
          margin-bottom: 15px;
          background: #fef0f0;
          padding: 10px;
          border-radius: 4px;
          border-left: 4px solid #f56c6c;
        "
      >
        <el-icon color="#f56c6c" :size="20" style="margin-right: 10px"><Warning /></el-icon>
        <div>
          <span style="font-weight: bold">{{ currentViolationUser.username }}</span>
          <span style="margin-left: 10px; font-size: 13px; color: #606266"
            >累计违规: {{ currentViolationUser.stats?.violationCount || 0 }} 次</span
          >
        </div>
      </div>

      <el-table
        :data="currentViolationUser.violationRecords"
        stripe
        style="width: 100%"
        size="small"
        border
      >
        <el-table-column prop="time" label="违规时间" width="160" />
        <el-table-column prop="type" label="违规类型" width="100">
          <template #default="{ row }">
            <el-tag type="danger" size="small">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="content"
          label="违规内容/证据"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column prop="auditor" label="审核人" width="100" />
        <el-table-column prop="status" label="处理状态" width="90">
          <template #default="{ row }">
            <span style="color: #67c23a"
              ><el-icon><CircleCheck /></el-icon> {{ row.status }}</span
            >
          </template>
        </el-table-column>
      </el-table>
    </div>
    <template #footer>
      <el-button @click="violationDialogVisible = false">关闭</el-button>
      <el-button
        type="danger"
        plain
        @click="handleStatusAction(currentViolationUser)"
        v-if="currentViolationUser?.status"
        >立即封禁</el-button
      >
    </template>
  </el-dialog>
</template>

<script setup>
import { inject } from 'vue'

const {
  tagConfigDialogVisible,
  resolveImageUrl,
  handleStatusAction,
  dialogVisible,
  form,
  rules,
  formRef,
  isEditingSelf,
  avatarUploadAction,
  avatarUploadHeaders,
  beforeAvatarUpload,
  handleAvatarUploadSuccess,
  handleAvatarUploadError,
  handleHarmonizeAvatar,
  handleSubmit,
  banDialogVisible,
  banForm,
  submitBan,
  detailDialogVisible,
  currentUser,
  labelText,
  handleResetPwd,
  handleForceLogout,
  tagConfig,
  violationDialogVisible,
  currentViolationUser,
  Warning,
  CircleCheck
} = inject('adminUserPageContext')
</script>
