<template>
  <div class="menu-view sys-mgmt-view standard-list-view">
    <StandardWorkbenchPanel
      title="导航编排"
      description="导航编排页负责维护树结构与元数据，角色菜单范围请在角色权限页授权。"
      show-header-actions
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-notices
      show-toolbar
      show-pagination
    >
      <template #header-actions>
        <StandardButton v-permission="'system:role:update'" action="refresh" @click="goToRolePage">前往角色授权</StandardButton>
        <StandardButton v-permission="'system:menu:add'" action="add" @click="openAddRoot">新增菜单</StandardButton>
      </template>

      <template #filters>
        <StandardListFilterHeader :model="filters">
          <template #primary>
            <el-form-item>
              <el-input v-model="filters.menuName" clearable placeholder="菜单名称" @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="filters.menuCode" clearable placeholder="菜单编码" @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.type" clearable placeholder="类型">
                <el-option label="目录" :value="0" />
                <el-option label="页面" :value="1" />
                <el-option label="按钮" :value="2" />
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

      <template #notices>
        <el-alert
          :title="menuModeNotice"
          type="info"
          :closable="false"
          show-icon
          class="menu-view__alert"
        />
      </template>

      <template #toolbar>
        <StandardTableToolbar compact :meta-items="[ `当前结果 ${pagination.total} 条` ]">
          <template #right>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        show-overflow-tooltip
        row-key="id"
        :lazy="!isFilterMode"
        :load="loadChildren"
        :tree-props="treeProps"
      >
        <StandardTableTextColumn prop="menuName" label="菜单名称" :min-width="160" />
        <StandardTableTextColumn prop="menuCode" label="菜单编码" :min-width="180" />
        <StandardTableTextColumn prop="path" label="路由路径" :min-width="160" />
        <StandardTableTextColumn prop="component" label="组件" :min-width="140" />
        <el-table-column prop="type" label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.type === 2 ? 'warning' : row.type === 1 ? 'success' : 'info'">
              {{ typeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <StandardTableTextColumn prop="sort" label="排序" :width="80" />
        <el-table-column label="操作" width="240" fixed="right" :show-overflow-tooltip="false">
          <template #default="{ row }">
            <StandardRowActions variant="table" gap="wide">
              <StandardActionLink v-permission="'system:menu:update'" @click="openEdit(row.id)">编辑</StandardActionLink>
              <StandardActionLink v-permission="'system:menu:add'" @click="openAddChild(row.id)">新增子级</StandardActionLink>
              <StandardActionLink v-permission="'system:menu:delete'" @click="removeMenu(row.id)">删除</StandardActionLink>
            </StandardRowActions>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
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
      </template>
    </StandardWorkbenchPanel>

    <StandardFormDrawer
      v-model="dialogVisible"
      :title="dialogTitle"
      subtitle="通过右侧抽屉维护菜单树结构、路由元数据与启停状态。"
      size="44rem"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="父级 ID">
          <el-input v-model.number="form.parentId" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" />
        </el-form-item>
        <el-form-item label="菜单编码" prop="menuCode">
          <el-input v-model="form.menuCode" />
        </el-form-item>
        <el-form-item label="路由路径">
          <el-input v-model="form.path" placeholder="例如 /menu" />
        </el-form-item>
        <el-form-item label="组件路径">
          <el-input v-model="form.component" placeholder="例如 MenuView" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="0">目录</el-radio>
            <el-radio :value="1">页面</el-radio>
            <el-radio :value="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="Meta JSON">
          <el-input v-model="form.metaJson" type="textarea" :rows="3" placeholder='例如 {"caption":"菜单描述"}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <StandardDrawerFooter
          :confirm-text="dialogMode === 'edit' ? '确认保存' : '确认新增'"
          :confirm-loading="submitLoading"
          @cancel="dialogVisible = false"
          @confirm="submitForm"
        >
          <template #default>
            <StandardButton action="cancel" class="standard-drawer-footer__button standard-drawer-footer__button--ghost" @click="dialogVisible = false">
              取消
            </StandardButton>
            <StandardButton
              v-permission="dialogMode === 'edit' ? 'system:menu:update' : 'system:menu:add'"
              action="confirm"
              class="standard-drawer-footer__button standard-drawer-footer__button--primary"
              :loading="submitLoading"
              @click="submitForm"
            >
              {{ dialogMode === 'edit' ? '确认保存' : '确认新增' }}
            </StandardButton>
          </template>
        </StandardDrawerFooter>
      </template>
    </StandardFormDrawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

import { addMenu, deleteMenu, getMenu, listMenus, pageMenus, updateMenu, type Menu } from '@/api/menu'
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue'
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import { useListAppliedFilters } from '@/composables/useListAppliedFilters'
import { useServerPagination } from '@/composables/useServerPagination'
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm'

const router = useRouter()
const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<Menu[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const dialogMode = ref<'add' | 'edit'>('add')
const formRef = ref()
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum } = useServerPagination()

const filters = reactive({
  menuName: '',
  menuCode: '',
  type: undefined as number | undefined
})
const appliedFilters = reactive({
  menuName: '',
  menuCode: '',
  type: undefined as number | undefined
})

const form = reactive<Partial<Menu>>({
  id: undefined,
  parentId: 0,
  menuName: '',
  menuCode: '',
  path: '',
  component: '',
  icon: '',
  metaJson: '',
  type: 1,
  status: 1,
  sort: 0
})

const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const {
  tags: activeFilterTags,
  hasAppliedFilters,
  syncAppliedFilters,
  removeFilter: removeAppliedFilter
} = useListAppliedFilters({
  form: filters,
  applied: appliedFilters,
  fields: [
    { key: 'menuName', label: '菜单名称' },
    { key: 'menuCode', label: '菜单编码' },
    { key: 'type', label: (value) => `类型：${typeText(value)}`, clearValue: undefined, isActive: (value) => value !== undefined }
  ],
  defaults: {
    menuName: '',
    menuCode: '',
    type: undefined
  }
})

const isFilterMode = computed(() => Boolean(appliedFilters.menuName.trim() || appliedFilters.menuCode.trim() || appliedFilters.type !== undefined))
const menuModeNotice = computed(() =>
  isFilterMode.value
    ? '搜索模式返回扁平分页结果，不再全量加载菜单树。'
    : '默认仅分页加载根菜单，展开行时按需加载子菜单。'
)

const treeProps = {
  children: 'children',
  hasChildren: 'hasChildren'
}

function typeText(type?: number) {
  if (type === 0) return '目录'
  if (type === 1) return '页面'
  if (type === 2) return '按钮'
  return '--'
}

function resetForm(parentId = 0) {
  form.id = undefined
  form.parentId = parentId
  form.menuName = ''
  form.menuCode = ''
  form.path = ''
  form.component = ''
  form.icon = ''
  form.metaJson = ''
  form.type = 1
  form.status = 1
  form.sort = 0
}

async function loadMenuPage() {
  loading.value = true
  try {
    const res = await pageMenus({
      menuName: appliedFilters.menuName || undefined,
      menuCode: appliedFilters.menuCode || undefined,
      type: appliedFilters.type,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res.code === 200 && res.data) {
      tableData.value = applyPageResult(res.data)
    }
  } catch (error) {
    console.error('查询菜单分页失败', error)
    ElMessage.error((error as Error).message || '查询菜单失败')
  } finally {
    loading.value = false
  }
}

async function loadChildren(row: Menu, _treeNode: unknown, resolve: (data: Menu[]) => void) {
  try {
    const res = await listMenus({ parentId: row.id })
    const children = (res.data || []) as Menu[]
    row.children = children
    row.hasChildren = children.length > 0
    resolve(children)
  } catch (error) {
    console.error('查询子菜单失败', error)
    resolve([])
  }
}

function handleSearch() {
  syncAppliedFilters()
  resetPage()
  loadMenuPage()
}

function handleReset() {
  filters.menuName = ''
  filters.menuCode = ''
  filters.type = undefined
  syncAppliedFilters()
  resetPage()
  loadMenuPage()
}

function handleRefresh() {
  loadMenuPage()
}

function handleRemoveAppliedFilter(key: string) {
  removeAppliedFilter(key)
  resetPage()
  loadMenuPage()
}

function handleClearAppliedFilters() {
  handleReset()
}

function goToRolePage() {
  router.push('/role')
}

function openAddRoot() {
  dialogMode.value = 'add'
  dialogTitle.value = '新增菜单'
  resetForm(0)
  dialogVisible.value = true
}

function openAddChild(parentId: number | string) {
  dialogMode.value = 'add'
  dialogTitle.value = '新增子菜单'
  resetForm(Number(parentId))
  dialogVisible.value = true
}

async function openEdit(id: number | string) {
  dialogMode.value = 'edit'
  dialogTitle.value = '编辑菜单'
  try {
    const res = await getMenu(id)
    if (res.code === 200 && res.data) {
      Object.assign(form, res.data)
      dialogVisible.value = true
    }
  } catch (error) {
    console.error('加载菜单详情失败', error)
    ElMessage.error((error as Error).message || '加载菜单详情失败')
  }
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    if (dialogMode.value === 'add') {
      await addMenu(form)
      ElMessage.success('新增成功')
    } else {
      await updateMenu(form)
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    loadMenuPage()
  } catch (error) {
    console.error('提交菜单失败', error)
    ElMessage.error((error as Error).message || '提交菜单失败')
  } finally {
    submitLoading.value = false
  }
}

async function removeMenu(id: number | string) {
  try {
    const target = tableData.value.find(item => String(item.id) === String(id))
    await confirmDelete('菜单', target?.menuName)
    await deleteMenu(id)
    ElMessage.success('删除成功')
    loadMenuPage()
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('删除菜单失败', error)
    ElMessage.error((error as Error).message || '删除菜单失败')
  }
}

function handleDialogClose() {
  formRef.value?.clearValidate?.()
}

function handleSizeChange(size: number) {
  setPageSize(size)
  loadMenuPage()
}

function handlePageChange(page: number) {
  setPageNum(page)
  loadMenuPage()
}

onMounted(() => {
  syncAppliedFilters()
  loadMenuPage()
})
</script>

<style scoped>
.menu-view :deep(.standard-workbench-panel__pagination) {
  margin-top: 1rem;
}

</style>
