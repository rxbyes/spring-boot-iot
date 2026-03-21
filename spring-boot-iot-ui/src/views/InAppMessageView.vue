<template>
  <div class="in-app-message-view sys-mgmt-view standard-list-view">
    <PanelCard class="box-card">
      <template #header>
        <div class="card-header in-app-message-view__header">
          <div class="in-app-message-view__header-copy">
            <span>站内消息管理</span>
            <small>统一治理通知中心的手工广播、系统自动消息来源与消费效果。</small>
          </div>
          <el-button v-permission="'system:in-app-message:add'" type="primary" :icon="Plus" @click="handleAdd">
            新增消息
          </el-button>
        </div>
      </template>

      <section v-loading="statsLoading" class="in-app-message-view__stats">
        <article class="in-app-message-view__stats-card">
          <span>投放总量</span>
          <strong>{{ formatCount(statsRecord?.totalDeliveryCount) }}</strong>
          <small>当前筛选范围内的累计送达人数</small>
        </article>
        <article class="in-app-message-view__stats-card">
          <span>未读总量</span>
          <strong>{{ formatCount(statsRecord?.totalUnreadCount) }}</strong>
          <small>便于识别需要继续跟进的消息</small>
        </article>
        <article class="in-app-message-view__stats-card">
          <span>已读率</span>
          <strong>{{ formatPercent(statsRecord?.readRate) }}</strong>
          <small>已读率越低越需要优化标题、范围和时机</small>
        </article>
        <article class="in-app-message-view__stats-card">
          <span>主要来源</span>
          <strong>{{ topSourceTypeLabel }}</strong>
          <small>{{ topSourceTypeHint }}</small>
        </article>
      </section>

      <section class="in-app-message-view__insight-grid">
        <article class="in-app-message-view__insight-card">
          <div class="in-app-message-view__insight-header">
            <div>
              <h3>来源分布</h3>
              <p>区分手工广播与系统自动消息，便于治理来源收敛。</p>
            </div>
          </div>
          <ul v-if="sourceTypeBuckets.length > 0" class="in-app-message-view__insight-list">
            <li v-for="bucket in sourceTypeBuckets" :key="bucket.key">
              <span>{{ bucket.label }}</span>
              <strong>{{ bucket.deliveryCount }} / {{ formatPercent(bucket.readRate) }}</strong>
            </li>
          </ul>
          <el-empty v-else description="暂无来源统计" :image-size="64" />
        </article>
        <article class="in-app-message-view__insight-card">
          <div class="in-app-message-view__insight-header">
            <div>
              <h3>高未读消息</h3>
              <p>优先关注已发出但未形成阅读闭环的消息。</p>
            </div>
          </div>
          <ul v-if="topUnreadMessages.length > 0" class="in-app-message-view__insight-list">
            <li v-for="item in topUnreadMessages" :key="String(item.messageId)">
              <span>{{ item.title }}</span>
              <strong>{{ item.unreadCount }} / {{ formatPercent(item.unreadRate) }}</strong>
            </li>
          </ul>
          <el-empty v-else description="暂无高未读消息" :image-size="64" />
        </article>
      </section>

      <el-form :model="searchForm" label-width="100px" class="search-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="消息标题">
              <el-input
                v-model="searchForm.title"
                clearable
                placeholder="请输入消息标题"
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="消息分类">
              <el-select v-model="searchForm.messageType" clearable placeholder="请选择分类">
                <el-option
                  v-for="item in IN_APP_MESSAGE_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="优先级">
              <el-select v-model="searchForm.priority" clearable placeholder="请选择优先级">
                <el-option
                  v-for="item in IN_APP_MESSAGE_PRIORITY_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="来源类型">
              <el-select v-model="searchForm.sourceType" clearable placeholder="请选择来源类型">
                <el-option
                  v-for="item in IN_APP_MESSAGE_SOURCE_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="推送范围">
              <el-select v-model="searchForm.targetType" clearable placeholder="请选择推送范围">
                <el-option
                  v-for="item in IN_APP_MESSAGE_TARGET_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" clearable placeholder="请选择状态">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <div />
          </el-col>
          <el-col :span="8">
            <div />
          </el-col>
          <el-col :span="8" class="text-right">
            <el-form-item label="">
              <el-button @click="handleReset">重置</el-button>
              <el-button type="primary" @click="handleSearch">查询</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <StandardTableToolbar :meta-items="[ `当前结果 ${pagination.total} 条`, `已选 ${selectedRows.length} 项` ]">
        <template #right>
          <el-button link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</el-button>
          <el-button link @click="handleRefresh">刷新列表</el-button>
        </template>
      </StandardTableToolbar>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <StandardTableTextColumn prop="title" label="消息标题" :min-width="220" />
        <el-table-column prop="messageType" label="消息分类" width="110">
          <template #default="{ row }">
            <el-tag :type="messageTypeTagType(row.messageType)">
              {{ getMessageTypeLabel(row.messageType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" effect="plain">
              {{ getPriorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="推送范围" width="150">
          <template #default="{ row }">
            <div class="in-app-message-view__scope">
              <el-tag size="small" :type="targetTypeTagType(row.targetType)">
                {{ getTargetTypeLabel(row.targetType) }}
              </el-tag>
              <span class="in-app-message-view__scope-text">{{ getTargetSummary(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <StandardTableTextColumn prop="relatedPath" label="关联页面" :min-width="180">
          <template #default="{ row }">
            {{ getPathLabel(row.relatedPath) }}
          </template>
        </StandardTableTextColumn>
        <el-table-column prop="sourceType" label="来源类型" width="130">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="sourceTypeTagType(row.sourceType)">
              {{ getSourceTypeLabel(row.sourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <StandardTableTextColumn prop="publishTime" label="发布时间" :width="180" />
        <StandardTableTextColumn prop="expireTime" label="失效时间" :width="180" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <StandardTableTextColumn prop="summary" label="摘要" :min-width="220" />
        <el-table-column label="操作" width="220" fixed="right" :show-overflow-tooltip="false">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button
              v-if="canEditMessage(row)"
              v-permission="'system:in-app-message:update'"
              type="primary"
              link
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-else-if="canDeactivateMessage(row)"
              v-permission="'system:in-app-message:update'"
              type="warning"
              link
              @click="handleDeactivate(row)"
            >
              停用
            </el-button>
            <el-button
              v-if="canDeleteMessage(row)"
              v-permission="'system:in-app-message:delete'"
              type="danger"
              link
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <StandardPagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />

      <StandardDetailDrawer
        v-model="detailVisible"
        eyebrow="System Content"
        :title="detailTitle"
        :subtitle="detailSubtitle"
        :tags="detailTags"
        :empty="!detailRecord"
      >
        <section class="detail-panel detail-panel--hero">
          <div class="detail-section-header">
            <div>
              <h3>壳层消费预览</h3>
              <p>直接预览摘要卡会如何呈现，以及详情抽屉会承接哪些动作。</p>
            </div>
          </div>
          <div class="detail-summary-grid">
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">消息分类</span>
              <strong class="detail-summary-card__value">{{ getMessageTypeLabel(detailRecord?.messageType) }}</strong>
              <p class="detail-summary-card__hint">与壳层 `系统 / 业务 / 错误` 三类直接对齐</p>
            </article>
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">优先级</span>
              <strong class="detail-summary-card__value">{{ getPriorityLabel(detailRecord?.priority) }}</strong>
              <p class="detail-summary-card__hint">发布顺序：{{ detailRecord?.sortNo ?? 0 }}</p>
            </article>
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">推送范围</span>
              <strong class="detail-summary-card__value">{{ getTargetTypeLabel(detailRecord?.targetType) }}</strong>
              <p class="detail-summary-card__hint">{{ detailTargetSummary }}</p>
            </article>
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">关联页面</span>
              <strong class="detail-summary-card__value">{{ getPathLabel(detailRecord?.relatedPath) }}</strong>
              <p class="detail-summary-card__hint">无路径时仅在通知中心展示，不绑定快捷跳转</p>
            </article>
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">消费动作</span>
              <strong class="detail-summary-card__value">{{ detailActionLabel }}</strong>
              <p class="detail-summary-card__hint">摘要卡查看详情，详情态支持显式已读与进入页面</p>
            </article>
          </div>
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>投放信息</h3>
              <p>便于快速复核角色定向、用户定向和发布时间窗口是否符合预期。</p>
            </div>
          </div>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="detail-field__label">发布时间</span>
              <strong class="detail-field__value">{{ formatValue(detailRecord?.publishTime) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">失效时间</span>
              <strong class="detail-field__value">{{ formatValue(detailRecord?.expireTime) }}</strong>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">目标角色</span>
              <strong class="detail-field__value">{{ detailRoleNames }}</strong>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">目标用户</span>
              <strong class="detail-field__value detail-field__value--plain">{{ detailUserNames }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">来源类型</span>
              <strong class="detail-field__value">{{ getSourceTypeLabel(detailRecord?.sourceType) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">来源标识</span>
              <strong class="detail-field__value detail-field__value--plain">{{ formatValue(detailRecord?.sourceId) }}</strong>
            </div>
          </div>
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>消息正文</h3>
              <p>摘要用于通知面板卡片，正文用于后续扩展“查看更多 / 详情查看”场景。</p>
            </div>
          </div>
          <div class="detail-grid">
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">摘要</span>
              <strong class="detail-field__value detail-field__value--plain">{{ formatValue(detailRecord?.summary) }}</strong>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">正文</span>
              <div class="detail-field__value detail-field__value--pre">{{ formatValue(detailRecord?.content) }}</div>
            </div>
          </div>
        </section>
      </StandardDetailDrawer>

      <StandardFormDrawer
        v-model="dialogVisible"
        eyebrow="System Form"
        :title="dialogTitle"
        subtitle="统一通过右侧抽屉维护通知中心消费的站内消息编排。"
        size="56rem"
        @close="handleDialogClose"
      >
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="消息分类必须继续保持 system / business / error；否则壳层通知中心不会按既有三段式分类消费。"
        />

        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" class="in-app-message-view__form">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="消息标题" prop="title">
                <el-input v-model="formData.title" placeholder="请输入消息标题" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="消息分类" prop="messageType">
                <el-select v-model="formData.messageType" placeholder="请选择消息分类">
                  <el-option
                    v-for="item in IN_APP_MESSAGE_TYPE_OPTIONS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="优先级" prop="priority">
                <el-select v-model="formData.priority" placeholder="请选择优先级">
                  <el-option
                    v-for="item in IN_APP_MESSAGE_PRIORITY_OPTIONS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="formData.status">
                  <el-radio :value="1">启用</el-radio>
                  <el-radio :value="0">停用</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="消息摘要">
            <el-input v-model="formData.summary" type="textarea" :rows="2" placeholder="用于通知面板摘要展示" />
          </el-form-item>
          <el-form-item label="消息正文" prop="content">
            <el-input v-model="formData.content" type="textarea" :rows="6" placeholder="请输入完整消息正文" />
          </el-form-item>
          <el-form-item label="推送范围" prop="targetType">
            <el-radio-group v-model="formData.targetType">
              <el-radio
                v-for="item in IN_APP_MESSAGE_TARGET_TYPE_OPTIONS"
                :key="item.value"
                :value="item.value"
              >
                {{ item.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="formData.targetType === 'role'" label="目标角色">
            <el-select
              v-model="formData.targetRoleCodes"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择角色"
            >
              <el-option
                v-for="role in roleOptions"
                :key="role.roleCode"
                :label="`${role.roleName} (${role.roleCode})`"
                :value="role.roleCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="formData.targetType === 'user'" label="目标用户">
            <el-select
              v-model="formData.targetUserIds"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择用户"
            >
              <el-option
                v-for="user in userOptions"
                :key="String(user.id)"
                :label="buildUserLabel(user)"
                :value="user.id!"
              />
            </el-select>
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="关联页面">
                <el-select v-model="formData.relatedPath" clearable filterable placeholder="请选择关联页面">
                  <el-option
                    v-for="item in pathOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="排序">
                <el-input-number v-model="formData.sortNo" :min="0" :max="999" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="发布时间">
                <el-date-picker
                  v-model="formData.publishTime"
                  type="datetime"
                  clearable
                  placeholder="请选择发布时间"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="失效时间">
                <el-date-picker
                  v-model="formData.expireTime"
                  type="datetime"
                  clearable
                  placeholder="请选择失效时间"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="来源类型">
                <el-select v-model="formData.sourceType" placeholder="请选择来源类型">
                  <el-option
                    v-for="item in editableSourceTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="来源标识">
                <el-input v-model="formData.sourceId" placeholder="例如 alarm-1001 / help-review" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <template #footer>
          <StandardDrawerFooter :confirm-loading="submitLoading" @cancel="dialogVisible = false" @confirm="handleSubmit" />
        </template>
      </StandardFormDrawer>
    </PanelCard>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { User } from '@/api/user'
import type { Role } from '@/api/role'
import {
  IN_APP_MESSAGE_PRIORITY_OPTIONS,
  IN_APP_MESSAGE_SOURCE_TYPE_OPTIONS,
  IN_APP_MESSAGE_TARGET_TYPE_OPTIONS,
  IN_APP_MESSAGE_TYPE_OPTIONS,
  addInAppMessage,
  deleteInAppMessage,
  getInAppMessage,
  getInAppMessageStats,
  pageInAppMessages,
  updateInAppMessage,
  type InAppMessagePriority,
  type InAppMessageRecord,
  type InAppMessageSourceType,
  type InAppMessageStatsBucket,
  type InAppMessageStatsRecord,
  type InAppMessageTopUnreadRecord,
  type InAppMessageTargetType,
  type InAppMessageType
} from '@/api/inAppMessage'
import { listRoles } from '@/api/role'
import { listUsers } from '@/api/user'
import PanelCard from '@/components/PanelCard.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import { useServerPagination } from '@/composables/useServerPagination'
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm'
import { listWorkspaceCommandEntries } from '@/utils/sectionWorkspaces'
import type { IdType } from '@/types/api'

interface SearchFormState {
  title: string
  messageType: InAppMessageType | undefined
  priority: InAppMessagePriority | undefined
  sourceType: InAppMessageSourceType | undefined
  targetType: InAppMessageTargetType | undefined
  status: number | undefined
}

interface MessageFormState {
  id?: IdType
  title: string
  summary: string
  content: string
  messageType: InAppMessageType
  priority: InAppMessagePriority
  targetType: InAppMessageTargetType
  targetRoleCodes: string[]
  targetUserIds: IdType[]
  relatedPath: string
  sourceType: 'manual' | 'governance'
  sourceId: string
  publishTime: Date | null
  expireTime: Date | null
  status: number
  sortNo: number
}

const formRef = ref()
const tableRef = ref()
const loading = ref(false)
const statsLoading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增站内消息')
const detailVisible = ref(false)
const detailRecord = ref<InAppMessageRecord | null>(null)
const tableData = ref<InAppMessageRecord[]>([])
const selectedRows = ref<InAppMessageRecord[]>([])
const roleOptions = ref<Role[]>([])
const userOptions = ref<User[]>([])
const statsRecord = ref<InAppMessageStatsRecord | null>(null)
const { pagination, applyPageResult, resetPage, setPageNum, setPageSize } = useServerPagination()

const searchForm = reactive<SearchFormState>({
  title: '',
  messageType: undefined,
  priority: undefined,
  sourceType: undefined,
  targetType: undefined,
  status: undefined
})

const pathOptions = listWorkspaceCommandEntries()
  .filter((item) => item.type === 'page')
  .map((item) => ({
    value: item.path,
    label: `${item.workspaceLabel} / ${item.title}`
  }))

const formData = reactive<MessageFormState>(createEmptyForm())

const pathLabelMap = computed(() => new Map(pathOptions.map((item) => [item.value, item.label])))
const roleLabelMap = computed(() => new Map(roleOptions.value.map((item) => [item.roleCode, item.roleName])))
const userLabelMap = computed(() => new Map(
  userOptions.value
    .filter((item) => item.id !== undefined)
    .map((item) => [String(item.id), buildUserLabel(item)])
))
const editableSourceTypeOptions = computed(() =>
  IN_APP_MESSAGE_SOURCE_TYPE_OPTIONS.filter((item) => item.value === 'manual' || item.value === 'governance')
)
const sourceTypeBuckets = computed<InAppMessageStatsBucket[]>(() => statsRecord.value?.sourceTypeBuckets || [])
const topUnreadMessages = computed<InAppMessageTopUnreadRecord[]>(() => statsRecord.value?.topUnreadMessages || [])
const topSourceTypeBucket = computed(() => sourceTypeBuckets.value[0] || null)
const topSourceTypeLabel = computed(() => getSourceTypeLabel(topSourceTypeBucket.value?.key))
const topSourceTypeHint = computed(() => {
  if (!topSourceTypeBucket.value) {
    return '暂无来源数据'
  }
  return `${topSourceTypeBucket.value.deliveryCount} 次投放，已读率 ${formatPercent(topSourceTypeBucket.value.readRate)}`
})

const formRules = {
  title: [{ required: true, message: '请输入消息标题', trigger: 'blur' }],
  messageType: [{ required: true, message: '请选择消息分类', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  targetType: [{ required: true, message: '请选择推送范围', trigger: 'change' }],
  sourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入消息正文', trigger: 'blur' }]
}

watch(
  () => formData.targetType,
  (value) => {
    if (value !== 'role') {
      formData.targetRoleCodes = []
    }
    if (value !== 'user') {
      formData.targetUserIds = []
    }
  }
)

const detailTitle = computed(() => detailRecord.value?.title || '消息详情')
const detailSubtitle = computed(() => detailRecord.value?.summary || '统一预览消息内容、推送范围和发布时间。')
const detailActionLabel = computed(() => detailRecord.value?.relatedPath ? '查看详情 / 进入页面' : '查看详情 / 标记已读')
const detailTags = computed(() => {
  if (!detailRecord.value) {
    return []
  }
  return [
    { label: getMessageTypeLabel(detailRecord.value.messageType), type: messageTypeTagType(detailRecord.value.messageType) },
    { label: getPriorityLabel(detailRecord.value.priority), type: priorityTagType(detailRecord.value.priority) },
    { label: getSourceTypeLabel(detailRecord.value.sourceType), type: sourceTypeTagType(detailRecord.value.sourceType) },
    { label: getTargetTypeLabel(detailRecord.value.targetType), type: targetTypeTagType(detailRecord.value.targetType) },
    { label: detailRecord.value.status === 1 ? '启用中' : '已停用', type: detailRecord.value.status === 1 ? 'success' : 'danger' }
  ]
})
const detailTargetSummary = computed(() => getTargetSummary(detailRecord.value))
const detailRoleNames = computed(() => resolveRoleNames(detailRecord.value?.targetRoleCodes))
const detailUserNames = computed(() => resolveUserNames(detailRecord.value?.targetUserIds))

function createEmptyForm(): MessageFormState {
  return {
    id: undefined,
    title: '',
    summary: '',
    content: '',
    messageType: 'system',
    priority: 'medium',
    targetType: 'all',
    targetRoleCodes: [],
    targetUserIds: [],
    relatedPath: '',
    sourceType: 'manual',
    sourceId: '',
    publishTime: null,
    expireTime: null,
    status: 1,
    sortNo: 0
  }
}

function resetForm() {
  Object.assign(formData, createEmptyForm())
}

function buildUserLabel(user: User) {
  const displayName = String(user.realName || user.username || '')
  const username = String(user.username || '')
  return username && username !== displayName ? `${displayName} (${username})` : displayName
}

function getMessageTypeLabel(value?: string | null) {
  return IN_APP_MESSAGE_TYPE_OPTIONS.find((item) => item.value === value)?.label || '--'
}

function getPriorityLabel(value?: string | null) {
  return IN_APP_MESSAGE_PRIORITY_OPTIONS.find((item) => item.value === value)?.label || '--'
}

function getTargetTypeLabel(value?: string | null) {
  return IN_APP_MESSAGE_TARGET_TYPE_OPTIONS.find((item) => item.value === value)?.label || '--'
}

function getSourceTypeLabel(value?: string | null) {
  if (value === 'system_maintenance' || value === 'daily_report') {
    return '手工广播'
  }
  if (value === 'governance_task') {
    return '治理任务'
  }
  return IN_APP_MESSAGE_SOURCE_TYPE_OPTIONS.find((item) => item.value === value)?.label || formatValue(value)
}

function getPathLabel(path?: string | null) {
  const normalizedPath = String(path || '').trim()
  if (!normalizedPath) {
    return '未绑定页面'
  }
  return pathLabelMap.value.get(normalizedPath) || normalizedPath
}

function messageTypeTagType(value?: string | null): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (value === 'system') {
    return 'primary'
  }
  if (value === 'business') {
    return 'success'
  }
  if (value === 'error') {
    return 'danger'
  }
  return 'info'
}

function priorityTagType(value?: string | null): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (value === 'critical') {
    return 'danger'
  }
  if (value === 'high') {
    return 'warning'
  }
  if (value === 'medium') {
    return 'primary'
  }
  if (value === 'low') {
    return 'success'
  }
  return 'info'
}

function targetTypeTagType(value?: string | null): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (value === 'all') {
    return 'primary'
  }
  if (value === 'role') {
    return 'warning'
  }
  if (value === 'user') {
    return 'success'
  }
  return 'info'
}

function sourceTypeTagType(value?: string | null): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (value === 'manual') {
    return 'primary'
  }
  if (value === 'governance') {
    return 'warning'
  }
  if (value === 'system_error') {
    return 'danger'
  }
  if (value === 'event_dispatch' || value === 'work_order') {
    return 'success'
  }
  return 'info'
}

function splitCsvValue(value?: string | null): string[] {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function formatValue(value?: string | null) {
  const content = String(value || '').trim()
  return content || '--'
}

function formatCount(value?: number | null) {
  return Number(value || 0).toLocaleString()
}

function formatPercent(value?: number | null) {
  return `${(Number(value || 0) * 100).toFixed(1)}%`
}

function resolveRoleNames(value?: string | null) {
  const roleCodes = splitCsvValue(value)
  if (roleCodes.length === 0) {
    return '全部角色'
  }
  return roleCodes.map((code) => roleLabelMap.value.get(code) || code).join('、')
}

function resolveUserNames(value?: string | null) {
  const userIds = splitCsvValue(value)
  if (userIds.length === 0) {
    return '全部用户'
  }
  return userIds.map((id) => userLabelMap.value.get(id) || id).join('、')
}

function getTargetSummary(row?: InAppMessageRecord | null) {
  if (!row) {
    return '--'
  }
  if (row.targetType === 'all') {
    return '全部账号'
  }
  if (row.targetType === 'role') {
    const roleCodes = splitCsvValue(row.targetRoleCodes)
    return roleCodes.length > 0 ? `${roleCodes.length} 个角色` : '未配置角色'
  }
  if (row.targetType === 'user') {
    const userIds = splitCsvValue(row.targetUserIds)
    return userIds.length > 0 ? `${userIds.length} 个用户` : '未配置用户'
  }
  return '--'
}

function isAutomaticMessage(sourceType?: string | null) {
  return sourceType === 'system_error' || sourceType === 'event_dispatch' || sourceType === 'work_order'
}

function canEditMessage(row: InAppMessageRecord) {
  return !isAutomaticMessage(row.sourceType)
}

function canDeleteMessage(row: InAppMessageRecord) {
  return !isAutomaticMessage(row.sourceType)
}

function canDeactivateMessage(row: InAppMessageRecord) {
  return isAutomaticMessage(row.sourceType) && Number(row.status ?? 1) === 1
}

function buildPayload() {
  return {
    id: formData.id,
    title: formData.title.trim(),
    summary: formData.summary.trim() || undefined,
    content: formData.content.trim(),
    messageType: formData.messageType,
    priority: formData.priority,
    targetType: formData.targetType,
    targetRoleCodes: formData.targetType === 'role' && formData.targetRoleCodes.length > 0
      ? formData.targetRoleCodes.join(',')
      : undefined,
    targetUserIds: formData.targetType === 'user' && formData.targetUserIds.length > 0
      ? formData.targetUserIds.map((item) => String(item)).join(',')
      : undefined,
    relatedPath: formData.relatedPath || undefined,
    sourceType: formData.sourceType || 'manual',
    sourceId: formData.sourceId.trim() || undefined,
    publishTime: formData.publishTime ? formData.publishTime.toISOString() : undefined,
    expireTime: formData.expireTime ? formData.expireTime.toISOString() : undefined,
    status: formData.status,
    sortNo: formData.sortNo
  }
}

function buildUpdatePayloadFromRecord(row: InAppMessageRecord, status: number) {
  return {
    id: row.id,
    title: row.title,
    summary: row.summary || undefined,
    content: row.content || undefined,
    messageType: row.messageType,
    priority: row.priority,
    targetType: row.targetType,
    targetRoleCodes: row.targetRoleCodes || undefined,
    targetUserIds: row.targetUserIds || undefined,
    relatedPath: row.relatedPath || undefined,
    sourceType: row.sourceType || undefined,
    sourceId: row.sourceId || undefined,
    publishTime: row.publishTime || undefined,
    expireTime: row.expireTime || undefined,
    status,
    sortNo: row.sortNo ?? 0
  }
}

async function loadRoleOptions() {
  try {
    const response = await listRoles({ status: 1 })
    if (response.code === 200 && response.data) {
      roleOptions.value = response.data
    }
  } catch (error) {
    console.error('加载角色列表失败', error)
  }
}

async function loadUserOptions() {
  try {
    const response = await listUsers({ status: 1 })
    if (response.code === 200 && response.data) {
      userOptions.value = response.data
    }
  } catch (error) {
    console.error('加载用户列表失败', error)
  }
}

async function loadMessagePage() {
  loading.value = true
  try {
    const response = await pageInAppMessages({
      title: searchForm.title || undefined,
      messageType: searchForm.messageType,
      priority: searchForm.priority,
      sourceType: searchForm.sourceType,
      targetType: searchForm.targetType,
      status: searchForm.status,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (response.code === 200 && response.data) {
      tableData.value = applyPageResult(response.data)
    }
  } catch (error) {
    console.error('获取站内消息分页失败', error)
    ElMessage.error((error as Error).message || '获取站内消息分页失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  statsLoading.value = true
  try {
    const response = await getInAppMessageStats({
      messageType: searchForm.messageType,
      sourceType: searchForm.sourceType
    })
    if (response.code === 200 && response.data) {
      statsRecord.value = response.data
    }
  } catch (error) {
    console.error('获取站内消息统计失败', error)
    ElMessage.error((error as Error).message || '获取站内消息统计失败')
  } finally {
    statsLoading.value = false
  }
}

function clearSelection() {
  selectedRows.value = []
  tableRef.value?.clearSelection?.()
}

function handleSelectionChange(rows: InAppMessageRecord[]) {
  selectedRows.value = rows
}

function handleSearch() {
  resetPage()
  clearSelection()
  loadMessagePage()
  loadStats()
}

function handleReset() {
  searchForm.title = ''
  searchForm.messageType = undefined
  searchForm.priority = undefined
  searchForm.sourceType = undefined
  searchForm.targetType = undefined
  searchForm.status = undefined
  handleSearch()
}

function handlePageChange(page: number) {
  setPageNum(page)
  clearSelection()
  loadMessagePage()
}

function handleSizeChange(pageSize: number) {
  setPageSize(pageSize)
  clearSelection()
  loadMessagePage()
}

function handleRefresh() {
  clearSelection()
  loadMessagePage()
  loadStats()
}

function handleDialogClose() {
  resetForm()
  formRef.value?.clearValidate?.()
}

function handleAdd() {
  dialogTitle.value = '新增站内消息'
  resetForm()
  dialogVisible.value = true
}

function handleView(row: InAppMessageRecord) {
  detailRecord.value = row
  detailVisible.value = true
}

async function handleEdit(row: InAppMessageRecord) {
  if (isAutomaticMessage(row.sourceType)) {
    detailRecord.value = row
    detailVisible.value = true
    ElMessage.info('系统自动消息仅支持查看或停用')
    return
  }
  try {
    const response = await getInAppMessage(row.id!)
    if (response.code !== 200 || !response.data) {
      ElMessage.error('加载消息详情失败')
      return
    }
    const record = response.data
    dialogTitle.value = '编辑站内消息'
    Object.assign(formData, {
      id: record.id,
      title: record.title || '',
      summary: record.summary || '',
      content: record.content || '',
      messageType: record.messageType || 'system',
      priority: record.priority || 'medium',
      targetType: record.targetType || 'all',
      targetRoleCodes: splitCsvValue(record.targetRoleCodes),
      targetUserIds: splitCsvValue(record.targetUserIds),
      relatedPath: record.relatedPath || '',
      sourceType: record.sourceType === 'governance' ? 'governance' : 'manual',
      sourceId: record.sourceId || '',
      publishTime: record.publishTime ? new Date(record.publishTime) : null,
      expireTime: record.expireTime ? new Date(record.expireTime) : null,
      status: Number(record.status ?? 1),
      sortNo: Number(record.sortNo ?? 0)
    })
    dialogVisible.value = true
  } catch (error) {
    console.error('加载站内消息详情失败', error)
    ElMessage.error((error as Error).message || '加载站内消息详情失败')
  }
}

async function handleDeactivate(row: InAppMessageRecord) {
  try {
    await updateInAppMessage(buildUpdatePayloadFromRecord(row, 0))
    ElMessage.success('停用成功')
    clearSelection()
    await loadMessagePage()
    await loadStats()
  } catch (error) {
    console.error('停用站内消息失败', error)
    ElMessage.error((error as Error).message || '停用站内消息失败')
  }
}

async function handleDelete(row: InAppMessageRecord) {
  try {
    await confirmDelete(`确定删除站内消息“${row.title}”吗？删除后不会再出现在通知中心。`)
    await deleteInAppMessage(row.id!)
    ElMessage.success('删除成功')
    clearSelection()
    await loadMessagePage()
    await loadStats()
  } catch (error) {
    if (!isConfirmCancelled(error)) {
      console.error('删除站内消息失败', error)
      ElMessage.error((error as Error).message || '删除站内消息失败')
    }
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate?.()
    submitLoading.value = true
    const payload = buildPayload()
    if (formData.id) {
      await updateInAppMessage(payload)
      ElMessage.success('更新成功')
    } else {
      await addInAppMessage(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    clearSelection()
    await loadMessagePage()
    await loadStats()
  } catch (error) {
    if (error instanceof Error) {
      console.error('提交站内消息失败', error)
      ElMessage.error(error.message || '提交站内消息失败')
    }
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadMessagePage()
  loadStats()
  loadRoleOptions()
  loadUserOptions()
})
</script>

<style scoped>
.in-app-message-view__header {
  align-items: flex-start;
}

.in-app-message-view__header-copy {
  display: grid;
  gap: 0.35rem;
}

.in-app-message-view__header-copy small {
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.5;
}

.in-app-message-view__scope {
  display: grid;
  gap: 0.25rem;
}

.in-app-message-view__scope-text {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.4;
}

.in-app-message-view__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.in-app-message-view__stats-card,
.in-app-message-view__insight-card {
  border: 1px solid var(--panel-border);
  border-radius: 20px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--brand-50) 48%, white) 0%, white 100%);
  padding: 1rem 1.1rem;
  display: grid;
  gap: 0.4rem;
}

.in-app-message-view__stats-card span,
.in-app-message-view__insight-card p {
  color: var(--text-caption);
}

.in-app-message-view__stats-card strong {
  font-size: 1.6rem;
  line-height: 1.1;
  color: var(--text-primary);
}

.in-app-message-view__stats-card small {
  color: var(--text-caption);
  line-height: 1.5;
}

.in-app-message-view__insight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.in-app-message-view__insight-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.in-app-message-view__insight-header h3 {
  margin: 0 0 0.2rem;
  font-size: 1rem;
}

.in-app-message-view__insight-header p {
  margin: 0;
  line-height: 1.5;
}

.in-app-message-view__insight-list {
  list-style: none;
  display: grid;
  gap: 0.75rem;
  padding: 0;
  margin: 0;
}

.in-app-message-view__insight-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  border-top: 1px dashed var(--panel-border);
  padding-top: 0.75rem;
}

.in-app-message-view__insight-list li:first-child {
  border-top: none;
  padding-top: 0;
}

.in-app-message-view__insight-list span {
  color: var(--text-primary);
  line-height: 1.5;
}

.in-app-message-view__insight-list strong {
  color: var(--text-secondary);
  white-space: nowrap;
}

.in-app-message-view__form {
  margin-top: 1rem;
}

.in-app-message-view__form :deep(.el-alert) {
  margin-bottom: 1rem;
}

.detail-field__value--pre {
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1200px) {
  .in-app-message-view__stats,
  .in-app-message-view__insight-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .in-app-message-view__stats,
  .in-app-message-view__insight-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
