<template>
  <div class="help-doc-view sys-mgmt-view standard-list-view">
    <StandardWorkbenchPanel
      title="帮助文档管理"
      description="统一编排业务类、技术类、常见问题资料，并按角色和页面范围过滤给帮助中心。"
      show-header-actions
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-toolbar
      show-pagination
    >
      <template #header-actions>
        <StandardButton v-permission="'system:help-doc:add'" action="add" :icon="Plus" @click="handleAdd">
          新增文档
        </StandardButton>
      </template>

      <template #filters>
        <StandardListFilterHeader :model="searchForm">
          <template #primary>
            <el-form-item>
              <el-input
                v-model="searchForm.title"
                clearable
                placeholder="文档标题"
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.docCategory" clearable placeholder="文档分类">
                <el-option
                  v-for="item in HELP_DOC_CATEGORY_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.status" clearable placeholder="状态">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="reset" @click="handleReset">重置</StandardButton>
            <StandardButton action="query" @click="handleSearch">查询</StandardButton>
          </template>
        </StandardListFilterHeader>
      </template>

      <template #applied-filters>
        <StandardAppliedFiltersBar
          :tags="activeFilterTags"
          @remove="handleRemoveAppliedFilter"
          @clear="handleClearAppliedFilters"
        />
      </template>

      <template #toolbar>
        <StandardTableToolbar compact :meta-items="[ `当前结果 ${pagination.total} 条`, `已选 ${selectedRows.length} 项` ]">
          <template #right>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
            <StandardActionMenu
              label="更多操作"
              :items="helpDocToolbarActions"
              @command="handleToolbarAction"
            />
          </template>
        </StandardTableToolbar>
      </template>

      <div
        v-loading="loading && hasRecords"
        class="ops-list-result-panel standard-list-surface"
        element-loading-text="正在刷新帮助文档列表"
        element-loading-background="var(--loading-mask-bg)"
      >
        <div v-if="showListSkeleton" class="ops-list-loading-state" aria-live="polite" aria-busy="true">
          <div class="ops-list-loading-state__summary">
            <span v-for="item in 3" :key="item" class="ops-list-loading-pulse ops-list-loading-pill" />
          </div>
          <div class="ops-list-loading-table ops-list-loading-table--header">
            <span v-for="item in 6" :key="`help-doc-head-${item}`" class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--header" />
          </div>
          <div v-for="row in 5" :key="`help-doc-row-${row}`" class="ops-list-loading-table ops-list-loading-table--row">
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-pill ops-list-loading-pill--status" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--short" />
          </div>
        </div>

        <template v-else-if="hasRecords">
          <el-table
            ref="tableRef"
            :data="tableData"
            border
            stripe
            style="width: 100%"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="48" />
            <StandardTableTextColumn prop="title" label="文档标题" :min-width="220" />
            <el-table-column prop="docCategory" label="文档分类" width="110">
              <template #default="{ row }">
                <el-tag :type="categoryTagType(row.docCategory)">
                  {{ getCategoryLabel(row.docCategory) }}
                </el-tag>
              </template>
            </el-table-column>
            <StandardTableTextColumn label="可见角色" :min-width="200">
              <template #default="{ row }">
                {{ getRoleSummary(row.visibleRoleCodes) }}
              </template>
            </StandardTableTextColumn>
            <StandardTableTextColumn label="关联页面" :min-width="220">
              <template #default="{ row }">
                {{ getRelatedPathSummary(row.relatedPaths) }}
              </template>
            </StandardTableTextColumn>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <StandardTableTextColumn prop="sortNo" label="排序" :width="80" />
            <StandardTableTextColumn prop="updateTime" label="更新时间" :width="180" />
            <StandardTableTextColumn prop="summary" label="摘要" :min-width="220" />
            <el-table-column
              label="操作"
              :width="helpDocActionColumnWidth"
              fixed="right"
              class-name="standard-row-actions-column"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row }">
                <StandardWorkbenchRowActions
                  variant="table"
                  :direct-items="getHelpDocRowActions()"
                  @command="(command) => handleHelpDocRowAction(command, row)"
                />
              </template>
            </el-table-column>
          </el-table>
        </template>

        <div v-else-if="!loading" class="standard-list-empty-state">
          <EmptyState :title="emptyStateTitle" :description="emptyStateDescription" />
          <div class="standard-list-empty-state__actions">
            <StandardButton v-if="hasAppliedFilters" action="reset" @click="handleClearAppliedFilters">清空筛选条件</StandardButton>
            <StandardButton v-else action="refresh" @click="handleRefresh">刷新列表</StandardButton>
          </div>
        </div>
      </div>

      <template #pagination>
        <div v-if="pagination.total > 0" class="ops-pagination">
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
        </div>
      </template>

      <StandardDetailDrawer
        v-model="detailVisible"
        :title="detailTitle"
        :subtitle="detailSubtitle"
        :tags="detailTags"
        :empty="!detailRecord"
      >
        <section class="detail-panel detail-panel--hero">
          <div class="detail-section-header">
            <div>
              <h3>文档概览</h3>
              <p>统一预览帮助中心消费的分类、角色范围和页面关联规则。</p>
            </div>
          </div>
          <div class="detail-summary-grid">
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">文档分类</span>
              <strong class="detail-summary-card__value">{{ getCategoryLabel(detailRecord?.docCategory) }}</strong>
              <p class="detail-summary-card__hint">与壳层 `业务 / 技术 / FAQ` 三类直接对齐</p>
            </article>
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">可见角色</span>
              <strong class="detail-summary-card__value">{{ detailRoleCount }}</strong>
              <p class="detail-summary-card__hint">{{ detailRoleSummary }}</p>
            </article>
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">关联页面</span>
              <strong class="detail-summary-card__value">{{ detailPathCount }}</strong>
              <p class="detail-summary-card__hint">{{ detailPathSummary }}</p>
            </article>
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">排序</span>
              <strong class="detail-summary-card__value">{{ detailRecord?.sortNo ?? 0 }}</strong>
              <p class="detail-summary-card__hint">当前状态：{{ detailRecord?.status === 1 ? '启用' : '停用' }}</p>
            </article>
          </div>
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>命中规则</h3>
              <p>帮助中心会结合关键词、角色范围和当前页面路径决定文档是否命中与如何排序。</p>
            </div>
          </div>
          <div class="detail-grid">
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">检索关键词</span>
              <strong class="detail-field__value detail-field__value--plain">{{ formatValue(detailRecord?.keywords) }}</strong>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">角色命中范围</span>
              <strong class="detail-field__value">{{ detailRoleSummary }}</strong>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">页面命中范围</span>
              <strong class="detail-field__value detail-field__value--plain">{{ detailPathSummary }}</strong>
            </div>
          </div>
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>文档正文</h3>
              <p>摘要用于帮助中心列表卡片，正文用于后续扩展详情页与检索增强。</p>
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
        :title="dialogTitle"
        subtitle="通过右侧抽屉维护帮助中心消费的文档编排与范围。"
        size="56rem"
        @close="handleDialogClose"
      >
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="帮助文档分类必须继续保持 business / technical / faq；帮助中心会按当前页面路径提升关联资料排序。"
        />

        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" class="help-doc-view__form">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="文档标题" prop="title">
                <el-input v-model="formData.title" placeholder="请输入文档标题" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="文档分类" prop="docCategory">
                <el-select v-model="formData.docCategory" placeholder="请选择分类">
                  <el-option
                    v-for="item in HELP_DOC_CATEGORY_OPTIONS"
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
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="formData.status">
                  <el-radio :value="1">启用</el-radio>
                  <el-radio :value="0">停用</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="排序">
                <el-input-number v-model="formData.sortNo" :min="0" :max="999" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="文档摘要">
            <el-input v-model="formData.summary" type="textarea" :rows="2" placeholder="用于帮助中心摘要展示" />
          </el-form-item>
          <el-form-item label="文档正文" prop="content">
            <el-input v-model="formData.content" type="textarea" :rows="8" placeholder="请输入完整帮助内容" />
          </el-form-item>
          <el-form-item label="关键词">
            <el-input
              v-model="formData.keywords"
              placeholder="多个关键词用英文逗号分隔，例如 告警,事件,FAQ"
            />
          </el-form-item>
          <el-form-item label="可见角色">
            <el-select
              v-model="formData.visibleRoleCodes"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="不选则默认全角色可见"
            >
              <el-option
                v-for="role in roleOptions"
                :key="role.roleCode"
                :label="`${role.roleName} (${role.roleCode})`"
                :value="role.roleCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="关联页面">
            <el-select
              v-model="formData.relatedPaths"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="不选则不限制页面范围"
            >
              <el-option
                v-for="item in pathOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-form>

        <template #footer>
          <StandardDrawerFooter :confirm-loading="submitLoading" @cancel="dialogVisible = false" @confirm="handleSubmit" />
        </template>
      </StandardFormDrawer>
    </StandardWorkbenchPanel>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  HELP_DOC_CATEGORY_OPTIONS,
  addHelpDocument,
  deleteHelpDocument,
  getHelpDocument,
  pageHelpDocuments,
  updateHelpDocument,
  type HelpDocCategory,
  type HelpDocumentRecord
} from '@/api/helpDoc'
import { listRoles, type Role } from '@/api/role'
import EmptyState from '@/components/EmptyState.vue'
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue'
import StandardActionMenu from '@/components/StandardActionMenu.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue'
import { useListAppliedFilters } from '@/composables/useListAppliedFilters'
import { useServerPagination } from '@/composables/useServerPagination'
import { resolveWorkbenchActionColumnWidth } from '@/utils/adaptiveActionColumn'
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm'
import { listWorkspaceCommandEntries } from '@/utils/sectionWorkspaces'
import { usePermissionStore } from '@/stores/permission'
import type { IdType } from '@/types/api'

interface SearchFormState {
  title: string
  docCategory: HelpDocCategory | undefined
  status: number | undefined
}

interface HelpDocFormState {
  id?: IdType
  docCategory: HelpDocCategory
  title: string
  summary: string
  content: string
  keywords: string
  relatedPaths: string[]
  visibleRoleCodes: string[]
  status: number
  sortNo: number
}

type HelpDocRowActionCommand = 'view' | 'edit' | 'delete'

const formRef = ref()
const tableRef = ref()
const permissionStore = usePermissionStore()
const helpDocActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: [
    { command: 'view', label: '详情' },
    { command: 'edit', label: '编辑' },
    { command: 'delete', label: '删除' }
  ],
})
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增帮助文档')
const detailVisible = ref(false)
const detailRecord = ref<HelpDocumentRecord | null>(null)
const tableData = ref<HelpDocumentRecord[]>([])
const selectedRows = ref<HelpDocumentRecord[]>([])
const roleOptions = ref<Role[]>([])
const { pagination, applyPageResult, resetPage, setPageNum, setPageSize, resetTotal } = useServerPagination()
let latestListRequestId = 0

const searchForm = reactive<SearchFormState>({
  title: '',
  docCategory: undefined,
  status: undefined
})
const appliedFilters = reactive<SearchFormState>({
  title: '',
  docCategory: undefined,
  status: undefined
})

const pathOptions = listWorkspaceCommandEntries()
  .filter((item) => item.type === 'page')
  .map((item) => ({
    value: item.path,
    label: `${item.workspaceLabel} / ${item.title}`
  }))

const formData = reactive<HelpDocFormState>(createEmptyForm())

const roleLabelMap = computed(() => new Map(roleOptions.value.map((item) => [item.roleCode, item.roleName])))
const pathLabelMap = computed(() => new Map(pathOptions.map((item) => [item.value, item.label])))

const formRules = {
  title: [{ required: true, message: '请输入文档标题', trigger: 'blur' }],
  docCategory: [{ required: true, message: '请选择文档分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入文档正文', trigger: 'blur' }]
}

const detailTitle = computed(() => detailRecord.value?.title || '帮助文档详情')
const detailSubtitle = computed(() => detailRecord.value?.summary || '统一预览帮助中心消费的角色范围、页面范围和文档正文。')
const detailTags = computed(() => {
  if (!detailRecord.value) {
    return []
  }
  return [
    { label: getCategoryLabel(detailRecord.value.docCategory), type: categoryTagType(detailRecord.value.docCategory) },
    { label: detailRecord.value.status === 1 ? '启用中' : '已停用', type: detailRecord.value.status === 1 ? 'success' : 'danger' }
  ]
})
const detailRoleSummary = computed(() => getRoleSummary(detailRecord.value?.visibleRoleCodes))
const detailPathSummary = computed(() => getRelatedPathSummary(detailRecord.value?.relatedPaths))
const detailRoleCount = computed(() => {
  const count = splitCsvValue(detailRecord.value?.visibleRoleCodes).length
  return count > 0 ? `${count} 个角色` : '全角色'
})
const detailPathCount = computed(() => {
  const count = splitCsvValue(detailRecord.value?.relatedPaths).length
  return count > 0 ? `${count} 个页面` : '不限制'
})
const helpDocToolbarActions = computed(() => [
  {
    key: 'clear-selection',
    command: 'clear-selection',
    label: '清空选中',
    disabled: selectedRows.value.length === 0
  }
])
const {
  tags: activeFilterTags,
  hasAppliedFilters,
  syncAppliedFilters,
  removeFilter: removeAppliedFilter
} = useListAppliedFilters({
  form: searchForm,
  applied: appliedFilters,
  fields: [
    { key: 'title', label: '文档标题' },
    { key: 'docCategory', label: (value) => `文档分类：${getCategoryLabel(value)}`, clearValue: undefined, isActive: (value) => value !== undefined },
    { key: 'status', label: (value) => `状态：${Number(value) === 1 ? '启用' : '停用'}`, clearValue: undefined, isActive: (value) => value !== undefined }
  ],
  defaults: {
    title: '',
    docCategory: undefined,
    status: undefined
  }
})
const hasRecords = computed(() => tableData.value.length > 0)
const showListSkeleton = computed(() => loading.value && !hasRecords.value)
const emptyStateTitle = computed(() => (hasAppliedFilters.value ? '没有符合条件的帮助文档' : '当前还没有帮助文档'))
const emptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? '已生效筛选暂时没有匹配结果，可以调整筛选条件，或者直接清空当前筛选。'
    : '当前还没有可展示的帮助文档记录，建议稍后刷新，或先新增文档。'
)

function createEmptyForm(): HelpDocFormState {
  return {
    id: undefined,
    docCategory: 'business',
    title: '',
    summary: '',
    content: '',
    keywords: '',
    relatedPaths: [],
    visibleRoleCodes: [],
    status: 1,
    sortNo: 0
  }
}

function resetForm() {
  Object.assign(formData, createEmptyForm())
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

function getCategoryLabel(value?: string | null) {
  return HELP_DOC_CATEGORY_OPTIONS.find((item) => item.value === value)?.label || '--'
}

function categoryTagType(value?: string | null): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (value === 'business') {
    return 'primary'
  }
  if (value === 'technical') {
    return 'warning'
  }
  if (value === 'faq') {
    return 'success'
  }
  return 'info'
}

function getRoleSummary(value?: string | null) {
  const roleCodes = splitCsvValue(value)
  if (roleCodes.length === 0) {
    return '全角色可见'
  }
  return roleCodes.map((code) => roleLabelMap.value.get(code) || code).join('、')
}

function getRelatedPathSummary(value?: string | null) {
  const paths = splitCsvValue(value)
  if (paths.length === 0) {
    return '不限制页面范围'
  }
  return paths.map((path) => pathLabelMap.value.get(path) || path).join('、')
}

function buildPayload() {
  return {
    id: formData.id,
    docCategory: formData.docCategory,
    title: formData.title.trim(),
    summary: formData.summary.trim() || undefined,
    content: formData.content.trim(),
    keywords: formData.keywords.trim() || undefined,
    relatedPaths: formData.relatedPaths.length > 0 ? formData.relatedPaths.join(',') : undefined,
    visibleRoleCodes: formData.visibleRoleCodes.length > 0 ? formData.visibleRoleCodes.join(',') : undefined,
    status: formData.status,
    sortNo: formData.sortNo
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

async function loadHelpDocPage() {
  const requestId = ++latestListRequestId
  loading.value = true
  try {
    const response = await pageHelpDocuments({
      title: appliedFilters.title || undefined,
      docCategory: appliedFilters.docCategory,
      status: appliedFilters.status,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (requestId !== latestListRequestId) {
      return
    }
    if (response.code === 200 && response.data) {
      tableData.value = applyPageResult(response.data)
      return
    }
    tableData.value = []
    resetTotal()
  } catch (error) {
    if (requestId !== latestListRequestId) {
      return
    }
    tableData.value = []
    resetTotal()
    console.error('获取帮助文档分页失败', error)
    ElMessage.error((error as Error).message || '获取帮助文档分页失败')
  } finally {
    if (requestId === latestListRequestId) {
      loading.value = false
    }
  }
}

function clearSelection() {
  selectedRows.value = []
  tableRef.value?.clearSelection?.()
}

function handleSelectionChange(rows: HelpDocumentRecord[]) {
  selectedRows.value = rows
}

function getHelpDocRowActions() {
  const actions: Array<{ command: HelpDocRowActionCommand; label: string }> = [{ command: 'view', label: '详情' }]
  if (permissionStore.hasPermission('system:help-doc:update')) {
    actions.push({ command: 'edit', label: '编辑' })
  }
  if (permissionStore.hasPermission('system:help-doc:delete')) {
    actions.push({ command: 'delete', label: '删除' })
  }
  return actions
}

function handleHelpDocRowAction(command: HelpDocRowActionCommand, row: HelpDocumentRecord) {
  if (command === 'view') {
    handleView(row)
    return
  }
  if (command === 'edit') {
    handleEdit(row)
    return
  }
  handleDelete(row)
}

function handleSearch() {
  syncAppliedFilters()
  resetPage()
  clearSelection()
  loadHelpDocPage()
}

function handleReset() {
  searchForm.title = ''
  searchForm.docCategory = undefined
  searchForm.status = undefined
  syncAppliedFilters()
  resetPage()
  clearSelection()
  loadHelpDocPage()
}

function handleRemoveAppliedFilter(key: string) {
  removeAppliedFilter(key)
  resetPage()
  clearSelection()
  loadHelpDocPage()
}

function handleClearAppliedFilters() {
  handleReset()
}

function handlePageChange(page: number) {
  setPageNum(page)
  clearSelection()
  loadHelpDocPage()
}

function handleSizeChange(pageSize: number) {
  setPageSize(pageSize)
  clearSelection()
  loadHelpDocPage()
}

function handleRefresh() {
  clearSelection()
  loadHelpDocPage()
}

function handleToolbarAction(command: string | number | object) {
  switch (command) {
    case 'clear-selection':
      clearSelection()
      break
    default:
      break
  }
}

function handleDialogClose() {
  resetForm()
  formRef.value?.clearValidate?.()
}

function handleAdd() {
  dialogTitle.value = '新增帮助文档'
  resetForm()
  dialogVisible.value = true
}

function handleView(row: HelpDocumentRecord) {
  detailRecord.value = row
  detailVisible.value = true
}

async function handleEdit(row: HelpDocumentRecord) {
  try {
    const response = await getHelpDocument(row.id!)
    if (response.code !== 200 || !response.data) {
      ElMessage.error('加载帮助文档详情失败')
      return
    }
    const record = response.data
    dialogTitle.value = '编辑帮助文档'
    Object.assign(formData, {
      id: record.id,
      docCategory: record.docCategory || 'business',
      title: record.title || '',
      summary: record.summary || '',
      content: record.content || '',
      keywords: record.keywords || '',
      relatedPaths: splitCsvValue(record.relatedPaths),
      visibleRoleCodes: splitCsvValue(record.visibleRoleCodes),
      status: Number(record.status ?? 1),
      sortNo: Number(record.sortNo ?? 0)
    })
    dialogVisible.value = true
  } catch (error) {
    console.error('加载帮助文档详情失败', error)
    ElMessage.error((error as Error).message || '加载帮助文档详情失败')
  }
}

async function handleDelete(row: HelpDocumentRecord) {
  try {
    await confirmDelete(`确定删除帮助文档“${row.title}”吗？删除后帮助中心将不再返回该资料。`)
    await deleteHelpDocument(row.id!)
    ElMessage.success('删除成功')
    clearSelection()
    loadHelpDocPage()
  } catch (error) {
    if (!isConfirmCancelled(error)) {
      console.error('删除帮助文档失败', error)
      ElMessage.error((error as Error).message || '删除帮助文档失败')
    }
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate?.()
    submitLoading.value = true
    const payload = buildPayload()
    if (formData.id) {
      await updateHelpDocument(payload)
      ElMessage.success('更新成功')
    } else {
      await addHelpDocument(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    clearSelection()
    loadHelpDocPage()
  } catch (error) {
    if (error instanceof Error) {
      console.error('提交帮助文档失败', error)
      ElMessage.error(error.message || '提交帮助文档失败')
    }
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  syncAppliedFilters()
  loadHelpDocPage()
  loadRoleOptions()
})
</script>

<style scoped>
.help-doc-view__form {
  margin-top: 1rem;
}

.help-doc-view__form :deep(.el-alert) {
  margin-bottom: 1rem;
}

.detail-field__value--pre {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
