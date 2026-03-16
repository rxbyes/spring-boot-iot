<template>
  <div class="user-view">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button v-permission="'system:user:add'" type="primary" @click="handleAdd" :icon="Plus">新增</el-button>
        </div>
      </template>

      <el-form :model="searchForm" label-width="100px" class="search-form">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="用户�?>
              <el-input
                v-model="searchForm.username"
                placeholder="请输入用户名"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="手机�?>
              <el-input
                v-model="searchForm.phone"
                placeholder="请输入手机号"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="邮箱">
              <el-input
                v-model="searchForm.email"
                placeholder="请输入邮�?
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
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
        <el-table-column prop="username" label="用户�? min-width="140" />
        <el-table-column prop="realName" label="真实姓名" min-width="120" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            {{ (row.roleNames || []).join(' / ') || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机�? min-width="150" />
        <el-table-column prop="email" label="邮箱" min-width="200" />
        <el-table-column prop="status" label="状�? width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录时�? min-width="180" />
        <el-table-column prop="lastLoginIp" label="最后登录IP" min-width="150" />
        <el-table-column prop="createTime" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:user:update'" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'system:user:reset-password'" type="primary" link @click="handleResetPassword(row)">重置密码</el-button>
            <el-button v-permission="'system:user:delete'" type="danger" link @click="handleDelete(row)">删除</el-button>
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
        width="640px"
        @close="handleDialogClose"
      >
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="100px"
        >
          <el-form-item label="用户�? prop="username">
            <el-input v-model="formData.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="formData.realName" placeholder="请输入真实姓�? />
          </el-form-item>
          <el-form-item label="手机�? prop="phone">
            <el-input v-model="formData.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="formData.email" placeholder="请输入邮�? />
          </el-form-item>
          <el-form-item label="角色" prop="roleIds">
            <el-select v-model="formData.roleIds" multiple clearable placeholder="请选择角色">
              <el-option
                v-for="role in roleOptions"
                :key="role.id"
                :label="role.roleName"
                :value="role.id!"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="!formData.id" label="密码" prop="password">
            <el-input
              v-model="formData.password"
              type="password"
              show-password
              placeholder="请输入密�?
            />
          </el-form-item>
          <el-form-item label="状�? prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="0">禁用</el-radio>
            </el-radio-group>
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
import { onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from '@/utils/message';
import { ElMessageBox } from '@/utils/messageBox';
import { Plus } from '@element-plus/icons-vue';

import { listRoles, type Role } from '@/api/role';
import {
  addUser,
  deleteUser,
  getUser,
  listUsers,
  resetPassword,
  updateUser,
  type User
} from '@/api/user';

interface UserSearchForm {
  username: string;
  phone: string;
  email: string;
  status?: number;
}

interface UserFormData {
  id?: number;
  username: string;
  realName: string;
  phone: string;
  email: string;
  password: string;
  status: number;
  roleIds: number[];
}

const formRef = ref<FormInstance>();

const searchForm = reactive<UserSearchForm>({
  username: '',
  phone: '',
  email: '',
  status: undefined
});

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

const tableData = ref<User[]>([]);
const roleOptions = ref<Role[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const dialogTitle = ref('新增用户');
const submitLoading = ref(false);

function createDefaultFormData(): UserFormData {
  return {
    id: undefined,
    username: '',
    realName: '',
    phone: '',
    email: '',
    password: '',
    status: 1,
    roleIds: []
  };
}

const formData = reactive<UserFormData>(createDefaultFormData());

const formRules: FormRules<UserFormData> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓�?, trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  roleIds: [{ required: true, message: '请至少选择一个角�?, trigger: 'change' }],
  password: [{ required: true, message: '请输入密�?, trigger: 'blur' }]
};

async function getUsers() {
  loading.value = true;
  try {
    const res = await listUsers({
      username: searchForm.username || undefined,
      phone: searchForm.phone || undefined,
      email: searchForm.email || undefined,
      status: searchForm.status
    });
    tableData.value = res.data || [];
    pagination.total = tableData.value.length;
  } finally {
    loading.value = false;
  }
}

async function getRoleOptions() {
  const res = await listRoles({ status: 1 });
  roleOptions.value = res.data || [];
}

onMounted(async () => {
  await Promise.all([getUsers(), getRoleOptions()]);
});

function handleSearch() {
  getUsers();
}

function handleReset() {
  searchForm.username = '';
  searchForm.phone = '';
  searchForm.email = '';
  searchForm.status = undefined;
  getUsers();
}

function handleAdd() {
  dialogTitle.value = '新增用户';
  Object.assign(formData, createDefaultFormData());
  dialogVisible.value = true;
}

async function handleEdit(row: User) {
  if (!row.id) {
    return;
  }
  dialogTitle.value = '编辑用户';
  const res = await getUser(row.id);
  Object.assign(formData, createDefaultFormData(), res.data, {
    password: '',
    roleIds: res.data.roleIds || []
  });
  dialogVisible.value = true;
}

async function handleDelete(row: User) {
  if (!row.id) {
    return;
  }
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？', '警告', {
      type: 'warning'
    });
    await deleteUser(row.id);
    ElMessage.success('删除成功');
    await getUsers();
  } catch {
    // noop
  }
}

async function handleResetPassword(row: User) {
  if (!row.id) {
    return;
  }
  try {
    await ElMessageBox.confirm(`确定要重置用�?"${row.username}" 的密码吗？`, '警告', {
      type: 'warning'
    });
    await resetPassword(row.id);
    ElMessage.success('密码重置成功，默认密码为 123456');
  } catch {
    // noop
  }
}

async function handleSubmit() {
  if (!formRef.value) {
    return;
  }

  await formRef.value.validate();

  submitLoading.value = true;
  try {
    const payload: Partial<User> = {
      ...formData,
      roleIds: [...formData.roleIds]
    };
    if (payload.id) {
      delete payload.password;
      await updateUser(payload);
    } else {
      await addUser(payload);
    }
    ElMessage.success(payload.id ? '更新成功' : '新增成功');
    dialogVisible.value = false;
    await getUsers();
  } finally {
    submitLoading.value = false;
  }
}

function handleDialogClose() {
  formRef.value?.clearValidate();
  Object.assign(formData, createDefaultFormData());
}

function handleSizeChange(size: number) {
  pagination.pageSize = size;
}

function handlePageChange(page: number) {
  pagination.pageNum = page;
}
</script>

<style scoped>
.user-view {
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
</style>

