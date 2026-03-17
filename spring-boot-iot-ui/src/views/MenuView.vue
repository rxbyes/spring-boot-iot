<template>
  <div class="menu-view">
    <el-card>
      <template #header>
        <div class="menu-view__header">
          <div class="menu-view__header-content">
            <span>菜单管理</span>
            <small>菜单页负责维护树结构与元数据，角色菜单范围请在角色管理页授权。</small>
          </div>
          <div class="menu-view__header-actions">
            <el-button v-permission="'system:role:update'" @click="goToRolePage">前往角色授权</el-button>
            <el-button v-permission="'system:menu:add'" type="primary" @click="openAddRoot">新增菜单</el-button>
          </div>
        </div>
      </template>

      <el-form :model="filters" label-width="90px" class="menu-view__filters">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="菜单名称">
              <el-input v-model="filters.menuName" clearable placeholder="请输入菜单名称" @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="菜单编码">
              <el-input v-model="filters.menuCode" clearable placeholder="请输入菜单编码" @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="类型">
              <el-select v-model="filters.type" clearable placeholder="全部类型">
                <el-option label="目录" :value="0" />
                <el-option label="页面" :value="1" />
                <el-option label="按钮" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="">
              <el-button @click="handleReset">重置</el-button>
              <el-button type="primary" @click="handleSearch">查询</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-alert
        v-if="!isFilterMode"
        title="默认仅分页加载根菜单，展开行时按需加载子菜单。"
        type="info"
        :closable="false"
        show-icon
        class="menu-view__alert"
      />
      <el-alert
        v-else
        title="搜索模式返回扁平分页结果，不再全量加载菜单树。"
        type="info"
        :closable="false"
        show-icon
        class="menu-view__alert"
      />

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        row-key="id"
        :lazy="!isFilterMode"
        :load="loadChildren"
        :tree-props="treeProps"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="160" />
        <el-table-column prop="menuCode" label="菜单编码" min-width="180" />
        <el-table-column prop="path" label="路由路径" min-width="160" />
        <el-table-column prop="component" label="组件" min-width="140" />
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
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:menu:update'" type="primary" link @click="openEdit(row.id)">编辑</el-button>
            <el-button v-permission="'system:menu:add'" type="primary" link @click="openAddChild(row.id)">新增子级</el-button>
            <el-button v-permission="'system:menu:delete'" type="danger" link @click="removeMenu(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="menu-view__pagination"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <StandardFormDrawer
      v-model="dialogVisible"
      eyebrow="System Form"
      :title="dialogTitle"
      subtitle="统一通过右侧抽屉维护菜单树与路由元数据。"
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
        <el-button class="sys-dialog__btn sys-dialog__btn--ghost" @click="dialogVisible = false">取消</el-button>
        <el-button
          v-permission="dialogMode === 'edit' ? 'system:menu:update' : 'system:menu:add'"
          type="primary"
          class="sys-dialog__btn sys-dialog__btn--primary"
          :loading="submitLoading"
          @click="submitForm"
        >
          确定
        </el-button>
      </template>
    </StandardFormDrawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

import { addMenu, deleteMenu, getMenu, listMenus, pageMenus, updateMenu, type Menu } from '@/api/menu'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import { useServerPagination } from '@/composables/useServerPagination'

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

const isFilterMode = computed(() => Boolean(filters.menuName.trim() || filters.menuCode.trim() || filters.type !== undefined))

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
      menuName: filters.menuName || undefined,
      menuCode: filters.menuCode || undefined,
      type: filters.type,
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
  resetPage()
  loadMenuPage()
}

function handleReset() {
  filters.menuName = ''
  filters.menuCode = ''
  filters.type = undefined
  resetPage()
  loadMenuPage()
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
    await ElMessageBox.confirm('确认删除该菜单吗？', '删除菜单', { type: 'warning' })
    await deleteMenu(id)
    ElMessage.success('删除成功')
    loadMenuPage()
  } catch (error) {
    if ((error as Error).message === 'cancel') {
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
  loadMenuPage()
})
</script>

<style scoped>
.menu-view__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.menu-view__header-content {
  display: grid;
  gap: 4px;
}

.menu-view__header-content small {
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.menu-view__header-actions {
  display: flex;
  gap: 12px;
}

.menu-view__filters {
  margin-bottom: 12px;
}

.menu-view__alert {
  margin-bottom: 12px;
}

.menu-view__pagination {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
