<template>
  <div class="ops-workbench linkage-rule-view">
    <PanelCard
      eyebrow="Linkage Workflow"
      title="联动规则"
      description="统一维护联动触发条件与动作编排，让阈值告警、通知与应急处置能够在同一工作台中连贯配置。"
      class="ops-hero-card"
    >
      <template #actions>
        <el-button type="primary" @click="handleAdd">新增规则</el-button>
      </template>
      <div class="ops-kpi-grid">
        <MetricCard label="规则总数" :value="String(pagination.total)" :badge="{ label: '联动编排', tone: 'brand' }" />
        <MetricCard label="当前页启用" :value="String(enabledCount)" :badge="{ label: '已生效', tone: 'success' }" />
        <MetricCard label="已配触发条件" :value="String(triggerConfiguredCount)" :badge="{ label: '触发链路', tone: 'warning' }" />
        <MetricCard label="已配动作列表" :value="String(actionConfiguredCount)" :badge="{ label: '执行动作', tone: 'danger' }" />
      </div>
      <div class="ops-inline-note">
        联动规则负责把告警触发、动作执行和通知联动串起来，当前列表页已与其他风险平台页面统一为同一套工作台视觉骨架。
      </div>
    </PanelCard>

    <PanelCard
      eyebrow="Linkage Filters"
      title="筛选条件"
      description="优先检查启用中的联动规则和动作编排完整性，避免触发后无法执行后续动作。"
      class="ops-filter-card"
    >
      <el-form :model="filters" label-position="top" class="ops-filter-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="规则名称">
              <el-input v-model="filters.ruleName" placeholder="请输入规则名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="filters.status" placeholder="请选择状态" clearable>
                <el-option label="启用" :value="0" />
                <el-option label="停用" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="治理建议">
              <el-input :model-value="linkageAdvice" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="ops-filter-actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </el-form>
    </PanelCard>

    <PanelCard
      eyebrow="Linkage List"
      title="联动规则列表"
      :description="`当前 ${pagination.total} 条联动规则，支持动作编排与启停管理。`"
      class="ops-table-card"
    >
      <div class="table-action-bar">
        <div class="table-action-bar__left">
          <span class="table-action-bar__meta">已选 {{ selectedRows.length }} 项</span>
          <span class="table-action-bar__meta">启用 {{ enabledCount }} 项</span>
          <span class="table-action-bar__meta">已配动作 {{ actionConfiguredCount }} 项</span>
        </div>
        <div class="table-action-bar__right">
          <el-button link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</el-button>
          <el-button link @click="handleRefresh">刷新列表</el-button>
        </div>
      </div>

      <div v-if="loading" class="ops-state">正在加载联动规则列表...</div>
      <div v-else-if="ruleList.length === 0" class="ops-state">暂无符合条件的联动规则</div>
      <template v-else>
        <el-table
          ref="tableRef"
          :data="ruleList"
          border
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column prop="ruleName" label="规则名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
          <el-table-column label="触发条件" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.triggerCondition || '--' }}
            </template>
          </el-table-column>
          <el-table-column label="动作列表" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.actionList || '--' }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" round>{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

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
    </PanelCard>

    <StandardFormDrawer
      v-model="formVisible"
      eyebrow="Risk Platform Form"
      :title="formTitle"
      subtitle="统一通过右侧抽屉维护联动规则与动作编排。"
      size="44rem"
      @close="handleFormClose"
    >
      <div class="ops-drawer-stack">
        <div class="ops-drawer-note">
          <strong>编排提示</strong>
          <span>联动规则建议先定义清晰的触发条件，再补齐动作列表；如果采用 JSON 结构，请保持字段口径稳定，便于后续联调与复用。</span>
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
        <el-button class="sys-dialog__btn sys-dialog__btn--ghost" @click="formVisible = false">取消</el-button>
        <el-button type="primary" class="sys-dialog__btn sys-dialog__btn--primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </StandardFormDrawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from '@/utils/message';
import { ElMessageBox } from '@/utils/messageBox';
import MetricCard from '@/components/MetricCard.vue';
import PanelCard from '@/components/PanelCard.vue';
import StandardFormDrawer from '@/components/StandardFormDrawer.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import { useServerPagination } from '@/composables/useServerPagination';
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

const loadRuleList = async () => {
  loading.value = true;
  try {
    const res = await pageRuleList({
      ruleName: filters.ruleName || undefined,
      status: filters.status === '' ? undefined : Number(filters.status),
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
  resetPage();
  void loadRuleList();
};

const handleReset = () => {
  filters.ruleName = '';
  filters.status = '';
  resetPage();
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
    await ElMessageBox.confirm('确定要删除该规则吗？', '删除规则', {
      type: 'warning'
    });
    const res = await deleteRule(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      void loadRuleList();
    }
  } catch (error) {
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

