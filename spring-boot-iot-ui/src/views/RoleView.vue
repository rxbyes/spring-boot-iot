<template>
  <div class="role-view">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button v-permission="'system:role:add'" type="primary" @click="handleAdd" :icon="Plus">新增</el-button>
        </div>
      </template>

      <el-form :model="searchForm" label-width="100px" class="search-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="角色名称">
              <el-input
                v-model="searchForm.roleName"
                placeholder="请输入角色名�?
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="角色编码">
              <el-input
                v-model="searchForm.roleCode"
                placeholder="请输入角色编�?
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状�?>
              <el-select v-model="searchForm.status" placeholder="请选择状�? clearable>
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24" class="text-right">
            <el-button @click="handleReset">重置</el-button>
            <el-button type="primary" @click="handleSearch">查询</el-button>
          </el-col>
        </el-row>
      </el-form>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="roleName" label="角色名称" min-width="150" />
        <el-table-column prop="roleCode" label="角色编码" min-width="150" />
        <el-table-column prop="description" label="角色描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状�? width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="180" />
        <el-table-column prop="updateTime" label="更新时间" min-width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:role:update'" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'system:role:delete'" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        class="pagination"
      />

      <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="760px"
        @close="handleDialogClose"
      >
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="100px"
        >
          <el-form-item label="角色名称" prop="roleName">
            <el-input v-model="formData.roleName" placeholder="请输入角色名�? />
          </el-form-item>
          <el-form-item label="角色编码" prop="roleCode">
            <el-input v-model="formData.roleCode" placeholder="请输入角色编�? />
          </el-form-item>
          <el-form-item label="角色描述" prop="description">
            <el-input
              v-model="formData.description"
              type="textarea"
              placeholder="请输入角色描�?
            />
          </el-form-item>
          <el-form-item label="状�? prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="菜单权限">
            <div class="menu-tree-wrapper">
              <el-tree
                ref="menuTreeRef"
                :data="menuTree"
                node-key="id"
                show-checkbox
                default-expand-all
                check-on-click-node
                :props="menuTreeProps"
              />
            </div>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from '@/utils/message';
import { ElMessageBox } from '@/utils/messageBox';
import { Plus } from '@element-plus/icons-vue';

import { listMenuTree } from '@/api/menu';
import { addRole, deleteRole, getRole, listRoles, updateRole, type Role } from '@/api/role';
import type { MenuTreeNode } from '@/types/auth';

interface RoleSearchForm {
  roleName: string;
  roleCode: string;
  status?: number;
}

interface RoleFormData {
  id?: number;
  roleName: string;
  roleCode: string;
  description: string;
  status: number;
  menuIds: number[];
}

const formRef = ref<FormInstance>();
const menuTreeRef = ref<any>();

const searchForm = reactive<RoleSearchForm>({
  roleName: '',
  roleCode: '',
  status: undefined
});

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

const tableData = ref<Role[]>([]);
const menuTree = ref<MenuTreeNode[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const dialogTitle = ref('新增角色');
const submitLoading = ref(false);

const menuTreeProps = {
  label: 'menuName',
  children: 'children'
};

function createDefaultFormData(): RoleFormData {
  return {
    id: undefined,
    roleName: '',
    roleCode: '',
    description: '',
    status: 1,
    menuIds: []
  };
}

const formData = reactive<RoleFormData>(createDefaultFormData());

const formRules: FormRules<RoleFormData> = {
  roleName: [{ required: true, message: '请输入角色名�?, trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编�?, trigger: 'blur' }]
};

async function getRoles() {
  loading.value = true;
  try {
    const res = await listRoles({
      roleName: searchForm.roleName || undefined,
      roleCode: searchForm.roleCode || undefined,
      status: searchForm.status
    });
    tableData.value = res.data || [];
    pagination.total = tableData.value.length;
  } finally {
    loading.value = false;
  }
}

async function getMenuOptions() {
  const res = await listMenuTree();
  menuTree.value = res.data || [];
}

onMounted(async () => {
  await Promise.all([getRoles(), getMenuOptions()]);
});

function handleSearch() {
  getRoles();
}

function handleReset() {
  searchForm.roleName = '';
  searchForm.roleCode = '';
  searchForm.status = undefined;
  getRoles();
}

async function handleAdd() {
  dialogTitle.value = '新增角色';
  Object.assign(formData, createDefaultFormData());
  dialogVisible.value = true;
  await nextTick();
  menuTreeRef.value?.setCheckedKeys([]);
}

async function handleEdit(row: Role) {
  if (!row.id) {
    return;
  }
  dialogTitle.value = '编辑角色';
  const res = await getRole(row.id);
  Object.assign(formData, createDefaultFormData(), res.data, {
    menuIds: res.data.menuIds || []
  });
  dialogVisible.value = true;
  await nextTick();
  menuTreeRef.value?.setCheckedKeys(formData.menuIds);
}

async function handleDelete(row: Role) {
  if (!row.id) {
    return;
  }
  try {
    await ElMessageBox.confirm('确定要删除该角色吗？', '警告', {
      type: 'warning'
    });
    await deleteRole(row.id);
    ElMessage.success('删除成功');
    await getRoles();
  } catch {
    // noop
  }
}

function collectCheckedMenuIds(): number[] {
  const checked = (menuTreeRef.value?.getCheckedKeys?.() || []) as number[];
  const halfChecked = (menuTreeRef.value?.getHalfCheckedKeys?.() || []) as number[];
  return Array.from(new Set([...checked, ...halfChecked].map((item) => Number(item))));
}

async function handleSubmit() {
  if (!formRef.value) {
    return;
  }

  await formRef.value.validate();
  formData.menuIds = collectCheckedMenuIds();

  submitLoading.value = true;
  try {
    const payload = {
      ...formData,
      menuIds: [...formData.menuIds]
    };
    if (payload.id) {
      await updateRole(payload);
    } else {
      await addRole(payload);
    }
    ElMessage.success(payload.id ? '更新成功' : '新增成功');
    dialogVisible.value = false;
    await getRoles();
  } finally {
    submitLoading.value = false;
  }
}

function handleDialogClose() {
  formRef.value?.clearValidate();
  Object.assign(formData, createDefaultFormData());
  menuTreeRef.value?.setCheckedKeys([]);
}

function handleSizeChange(size: number) {
  pagination.pageSize = size;
}

function handlePageChange(page: number) {
  pagination.pageNum = page;
}
</script>

<style scoped>
.role-view {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.text-right {
  text-align: right;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.menu-tree-wrapper {
  width: 100%;
  max-height: 360px;
  overflow: auto;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 12px;
  background: var(--el-fill-color-light);
}
</style>

