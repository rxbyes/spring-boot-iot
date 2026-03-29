<template>
  <div class="ops-workbench linkage-rule-view">
    <StandardWorkbenchPanel
      title="联动编排"
      :description="`当前 ${pagination.total} 条联动编排，支持动作编排与启停管理。`"
      show-header-actions
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-notices
      show-toolbar
      show-pagination
    >
      <template #header-actions>
        <StandardButton action="add" @click="handleAdd">新增规则</StandardButton>
      </template>

      <template #filters>
        <StandardListFilterHeader :model="filters">
          <template #primary>
            <el-form-item>
              <el-input v-model="filters.ruleName" placeholder="规则名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.status" placeholder="状态" clearable>
                <el-option label="启用" :value="0" />
                <el-option label="停用" :value="1" />
              </el-select>
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="query" @click="handleSearch">查询</StandardButton>
            <StandardButton action="reset" @click="handleReset">重置</StandardButton>
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
          :title="linkageAdvice"
          type="info"
          :closable="false"
          show-icon
          class="view-alert"
        />
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[`已选 ${selectedRows.length} 项`, `启用 ${enabledCount} 项`, `触发条件 ${triggerConfiguredCount} 项`, `已配动作 ${actionConfiguredCount} 项`]"
        >
          <template #right>
            <StandardButton action="reset" link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</StandardButton>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <div v-if="loading" class="ops-state">正在加载联动编排列表...</div>
      <div v-else-if="ruleList.length === 0" class="ops-state">暂无符合条件的联动编排</div>
      <template v-else>
        <el-table
          ref="tableRef"
          :data="ruleList"
          border
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <StandardTableTextColumn prop="ruleName" label="规则名称" :min-width="180" />
          <StandardTableTextColumn prop="description" label="描述" :min-width="220" />
          <StandardTableTextColumn label="触发条件" :min-width="180">
            <template #default="{ row }">
              {{ row.triggerCondition || '--' }}
            </template>
          </StandardTableTextColumn>
          <StandardTableTextColumn label="动作列表" :min-width="180">
            <template #default="{ row }">
              {{ row.actionList || '--' }}
            </template>
          </StandardTableTextColumn>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" round>{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <StandardTableTextColumn prop="createTime" label="创建时间" :width="180" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <StandardRowActions variant="table" gap="wide">
                <StandardActionLink @click="handleEdit(row)">编辑</StandardActionLink>
                <StandardActionLink @click="handleDelete(row)">删除</StandardActionLink>
              </StandardRowActions>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template #pagination>
        <div class="ops-pagination">
          <StandardPagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </StandardWorkbenchPanel>

    <StandardFormDrawer
      v-model="formVisible"
      :title="formTitle"
      subtitle="统一通过右侧抽屉维护联动编排与动作编排。"
      size="44rem"
      @close="handleFormClose"
    >
      <div class="ops-drawer-stack">
        <div class="ops-drawer-note">
          <strong>编排提示</strong>
          <span>联动编排建议先定义清晰的触发条件，再补齐动作列表；如果采用 JSON 结构，请保持字段口径稳定，便于后续联调与复用。</span>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="ops-drawer-form">
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>规则基础信息</h3>
                <p>先确认规则名称、启停状态和适用范围，方便区分联动策略用途并减少误触发。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="规则名称" prop="ruleName">
                <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
              </el-form-item>
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="form.status">
                  <el-radio :value="0">启用</el-radio>
                  <el-radio :value="1">停用</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="描述" prop="description" class="ops-drawer-grid__full">
                <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入规则描述、适用场景或维护备注" />
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>触发与动作编排</h3>
                <p>触发条件和动作列表统一收拢到同一区域，便于联调时对照检查整条执行链路。</p>
              </div>
            </div>
            <div class="ops-drawer-grid ops-drawer-grid--single">
              <el-form-item label="触发条件" prop="triggerCondition">
                <el-input v-model="form.triggerCondition" type="textarea" :rows="6" placeholder="请输入触发条件（JSON格式）" />
              </el-form-item>
              <el-form-item label="动作列表" prop="actionList">
                <el-input v-model="form.actionList" type="textarea" :rows="6" placeholder="请输入动作列表（JSON格式）" />
              </el-form-item>
            </div>
          </section>
        </el-form>
      </div>
      <template #footer>
        <StandardDrawerFooter
          :confirm-loading="submitLoading"
          @cancel="formVisible = false"
          @confirm="handleSubmit"
        />
      </template>
    </StandardFormDrawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from '@/utils/message';
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue';
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue';
import StandardFormDrawer from '@/components/StandardFormDrawer.vue';
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue';
import { useListAppliedFilters } from '@/composables/useListAppliedFilters';
import { useServerPagination } from '@/composables/useServerPagination';
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm';
import { pageRuleList, addRule, updateRule, deleteRule } from '../api/linkageRule';
import type { LinkageRule } from '../api/linkageRule';

const loading = ref(false);
const formVisible = ref(false);
const ruleList = ref<LinkageRule[]>([]);
const tableRef = ref();
const selectedRows = ref<LinkageRule[]>([]);

const filters = reactive({
  ruleName: '',
  status: '' as '' | number
});
const appliedFilters = reactive({
  ruleName: '',
  status: '' as '' | number
});

const { pagination, applyPageResult, resetPage, setPageSize, setPageNum } = useServerPagination();

const formRef = ref();
const formTitle = computed(() => (form.id ? '编辑规则' : '新增规则'));
const form = reactive({
  id: undefined as number | undefined,
  ruleName: '',
  description: '',
  triggerCondition: '',
  actionList: '',
  status: 0
});

const rules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  triggerCondition: [{ required: true, message: '请输入触发条件', trigger: 'blur' }],
  actionList: [{ required: true, message: '请输入动作列表', trigger: 'blur' }]
};

const submitLoading = ref(false);
const linkageAdvice = '优先检查启用规则的触发条件与动作编排完整性';

const enabledCount = computed(() => ruleList.value.filter((item) => item.status === 0).length);
const triggerConfiguredCount = computed(() => ruleList.value.filter((item) => Boolean(item.triggerCondition)).length);
const actionConfiguredCount = computed(() => ruleList.value.filter((item) => Boolean(item.actionList)).length);

const getStatusType = (status: number) => {
  switch (status) {
    case 0:
      return 'success';
    case 1:
      return 'info';
    default:
      return 'info';
  }
};

const getStatusText = (status: number) => {
  switch (status) {
    case 0:
      return '启用';
    case 1:
      return '停用';
    default:
      return status.toString();
  }
};

const {
  tags: activeFilterTags,
  hasAppliedFilters,
  syncAppliedFilters,
  removeFilter: removeAppliedFilter
} = useListAppliedFilters({
  form: filters,
  applied: appliedFilters,
  fields: [
    { key: 'ruleName', label: '规则名称' },
    { key: 'status', label: (value) => `状态：${getStatusText(Number(value))}`, clearValue: '' as '' | number }
  ],
  defaults: {
    ruleName: '',
    status: '' as '' | number
  }
});

const loadRuleList = async () => {
  loading.value = true;
  try {
    const res = await pageRuleList({
      ruleName: appliedFilters.ruleName || undefined,
      status: appliedFilters.status === '' ? undefined : Number(appliedFilters.status),
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    });
    if (res.code === 200) {
      ruleList.value = applyPageResult(res.data);
    }
  } catch (error) {
    console.error('查询规则列表失败', error);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  syncAppliedFilters();
  resetPage();
  clearSelection();
  void loadRuleList();
};

const handleReset = () => {
  filters.ruleName = '';
  filters.status = '';
  syncAppliedFilters();
  resetPage();
  clearSelection();
  void loadRuleList();
};

const handleSizeChange = (size: number) => {
  setPageSize(size);
  void loadRuleList();
};

const handlePageChange = (page: number) => {
  setPageNum(page);
  void loadRuleList();
};

const handleSelectionChange = (rows: LinkageRule[]) => {
  selectedRows.value = rows;
};

const clearSelection = () => {
  tableRef.value?.clearSelection?.();
  selectedRows.value = [];
};

const handleRefresh = () => {
  clearSelection();
  void loadRuleList();
};

const handleRemoveAppliedFilter = (key: string) => {
  removeAppliedFilter(key);
  resetPage();
  clearSelection();
  void loadRuleList();
};

const handleClearAppliedFilters = () => {
  handleReset();
};

const resetRuleForm = () => {
  form.id = undefined;
  form.ruleName = '';
  form.description = '';
  form.triggerCondition = '';
  form.actionList = '';
  form.status = 0;
};

const handleAdd = () => {
  resetRuleForm();
  formVisible.value = true;
};

const handleEdit = (row: LinkageRule) => {
  form.id = row.id;
  form.ruleName = row.ruleName;
  form.description = row.description || '';
  form.triggerCondition = row.triggerCondition || '';
  form.actionList = row.actionList || '';
  form.status = row.status;
  formVisible.value = true;
};

const handleDelete = async (row: LinkageRule) => {
  try {
    await confirmDelete('联动编排', row.ruleName);
    const res = await deleteRule(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      void loadRuleList();
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return;
    }
    console.error('删除规则失败', error);
  }
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    submitLoading.value = true;
    const res = form.id ? await updateRule(form) : await addRule(form);
    if (res.code === 200) {
      ElMessage.success(form.id ? '更新成功' : '新增成功');
      formVisible.value = false;
      void loadRuleList();
    }
  } catch (error) {
    console.error('提交表单失败', error);
  } finally {
    submitLoading.value = false;
  }
};

const handleFormClose = () => {
  formRef.value?.clearValidate?.();
  resetRuleForm();
};

onMounted(() => {
  syncAppliedFilters();
  void loadRuleList();
});
</script>

<style scoped>
.linkage-rule-view {
  padding: 20px;
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(243, 247, 253, 0.66));
  border: 1px solid rgba(41, 60, 92, 0.1);
}
</style>
