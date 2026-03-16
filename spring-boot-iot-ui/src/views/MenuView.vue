<template>
  <div class="menu-view">
    <el-card>
      <template #header>
        <div class="menu-view__header">
          <span>菜单管理</span>
          <el-button type="primary" @click="openAddRoot">新增菜单</el-button>
        </div>
      </template>

      <el-form :model="filters" label-width="90px" class="menu-view__filters">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="菜单名称">
              <el-input v-model="filters.menuName" clearable placeholder="请输入菜单名称" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="菜单编码">
              <el-input v-model="filters.menuCode" clearable placeholder="请输入菜单编码" />
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
              <el-button @click="resetFilters">重置</el-button>
              <el-button type="primary" @click="loadMenus">查询</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        row-key="id"
        :tree-props="{ children: 'children' }"
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
            <el-button type="primary" link @click="openEdit(row.id)">编辑</el-button>
            <el-button type="primary" link @click="openAddChild(row.id)">新增子级</el-button>
            <el-button type="danger" link @click="removeMenu(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="父级ID">
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
            <el-radio :label="0">目录</el-radio>
            <el-radio :label="1">页面</el-radio>
            <el-radio :label="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
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
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';

import { addMenu, deleteMenu, getMenu, listMenuTree, updateMenu, type Menu } from '../api/menu';

const loading = ref(false);
const submitLoading = ref(false);
const tableData = ref<Menu[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增菜单');
const dialogMode = ref<'add' | 'edit'>('add');
const formRef = ref();

const filters = reactive({
  menuName: '',
  menuCode: '',
  type: undefined as number | undefined
});

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
});

const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
};

function typeText(type?: number) {
  if (type === 0) return '目录';
  if (type === 1) return '页面';
  if (type === 2) return '按钮';
  return '--';
}

function resetForm(parentId = 0) {
  form.id = undefined;
  form.parentId = parentId;
  form.menuName = '';
  form.menuCode = '';
  form.path = '';
  form.component = '';
  form.icon = '';
  form.metaJson = '';
  form.type = 1;
  form.status = 1;
  form.sort = 0;
}

async function loadMenus() {
  loading.value = true;
  try {
    const res = await listMenuTree();
    if (res.code === 200) {
      const keywordName = filters.menuName.trim().toLowerCase();
      const keywordCode = filters.menuCode.trim().toLowerCase();
      const filterType = filters.type;
      const filterTree = (nodes: Menu[]): Menu[] =>
        nodes
          .map((node) => ({ ...node, children: filterTree((node.children || []) as Menu[]) }))
          .filter((node) => {
            const matchName = !keywordName || (node.menuName || '').toLowerCase().includes(keywordName);
            const matchCode = !keywordCode || (node.menuCode || '').toLowerCase().includes(keywordCode);
            const matchType = filterType === undefined || node.type === filterType;
            return (matchName && matchCode && matchType) || (node.children && node.children.length > 0);
          });
      tableData.value = filterTree((res.data || []) as Menu[]);
    }
  } catch (error) {
    console.error('查询菜单失败', error);
    ElMessage.error((error as Error).message || '查询菜单失败');
  } finally {
    loading.value = false;
  }
}

function resetFilters() {
  filters.menuName = '';
  filters.menuCode = '';
  filters.type = undefined;
  loadMenus();
}

function openAddRoot() {
  dialogMode.value = 'add';
  dialogTitle.value = '新增菜单';
  resetForm(0);
  dialogVisible.value = true;
}

function openAddChild(parentId: number) {
  dialogMode.value = 'add';
  dialogTitle.value = '新增子菜单';
  resetForm(parentId);
  dialogVisible.value = true;
}

async function openEdit(id: number) {
  dialogMode.value = 'edit';
  dialogTitle.value = '编辑菜单';
  try {
    const res = await getMenu(id);
    if (res.code === 200 && res.data) {
      Object.assign(form, res.data);
      dialogVisible.value = true;
    }
  } catch (error) {
    console.error('加载菜单详情失败', error);
    ElMessage.error((error as Error).message || '加载菜单详情失败');
  }
}

async function submitForm() {
  if (!formRef.value) return;
  await formRef.value.validate();
  submitLoading.value = true;
  try {
    if (dialogMode.value === 'add') {
      await addMenu(form);
      ElMessage.success('新增成功');
    } else {
      await updateMenu(form);
      ElMessage.success('更新成功');
    }
    dialogVisible.value = false;
    loadMenus();
  } catch (error) {
    console.error('提交菜单失败', error);
    ElMessage.error((error as Error).message || '提交菜单失败');
  } finally {
    submitLoading.value = false;
  }
}

async function removeMenu(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该菜单吗？', '删除菜单', { type: 'warning' });
    await deleteMenu(id);
    ElMessage.success('删除成功');
    loadMenus();
  } catch (error) {
    if ((error as Error).message === 'cancel') {
      return;
    }
    console.error('删除菜单失败', error);
    ElMessage.error((error as Error).message || '删除菜单失败');
  }
}

onMounted(() => {
  loadMenus();
});
</script>

<style scoped>
.menu-view__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.menu-view__filters {
  margin-bottom: 12px;
}
</style>
