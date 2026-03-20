<template>
  <div class="ops-workbench emergency-plan-view">
    <PanelCard
      eyebrow="Emergency Plans"
      title="应急预案库"
      description="统一维护风险等级、响应步骤和联系人信息，支撑风险事件在告警触发后的快速响应与闭环执行。"
      class="ops-hero-card"
    >
      <template #actions>
        <el-button type="primary" @click="handleAdd">新增预案</el-button>
      </template>
      <div class="ops-kpi-grid">
        <MetricCard label="预案总数" :value="String(pagination.total)" :badge="{ label: '预案库', tone: 'brand' }" />
        <MetricCard label="当前页启用" :value="String(enabledCount)" :badge="{ label: '可执行', tone: 'success' }" />
        <MetricCard label="严重风险预案" :value="String(criticalCount)" :badge="{ label: '高优先级', tone: 'danger' }" />
        <MetricCard label="警告风险预案" :value="String(warningCount)" :badge="{ label: '常用预案', tone: 'warning' }" />
      </div>
      <div class="ops-inline-note">
        应急预案库与联动编排、阈值策略共同构成风险闭环，当前页面已统一为工作台样式，方便查看、维护和版本化治理。
      </div>
    </PanelCard>

    <PanelCard
      eyebrow="Plan Filters"
      title="筛选条件"
      description="优先关注严重风险预案和启用中的执行方案，确保关键场景下可快速调用并落实响应步骤。"
      class="ops-filter-card"
    >
      <el-form :model="filters" label-position="top" class="ops-filter-form">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="预案名称">
              <el-input v-model="filters.planName" placeholder="请输入预案名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="风险等级">
              <el-select v-model="filters.riskLevel" placeholder="请选择风险等级" clearable>
                <el-option label="严重" value="critical" />
                <el-option label="警告" value="warning" />
                <el-option label="提醒" value="info" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态">
              <el-select v-model="filters.status" placeholder="请选择状态" clearable>
                <el-option label="启用" :value="0" />
                <el-option label="停用" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="治理建议">
              <el-input :model-value="planAdvice" disabled />
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
      eyebrow="Plan List"
      title="应急预案列表"
      :description="`当前 ${pagination.total} 条应急预案，支持按风险等级和状态治理。`"
      class="ops-table-card"
    >
      <StandardTableToolbar
        :meta-items="[`已选 ${selectedRows.length} 项`, `启用 ${enabledCount} 项`, `严重 ${criticalCount} 项`]"
      >
        <template #right>
          <el-button link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</el-button>
          <el-button link @click="handleRefresh">刷新列表</el-button>
        </template>
      </StandardTableToolbar>

      <div v-if="loading" class="ops-state">正在加载应急预案列表...</div>
      <div v-else-if="planList.length === 0" class="ops-state">暂无符合条件的应急预案</div>
      <template v-else>
        <el-table
          ref="tableRef"
          :data="planList"
          border
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <StandardTableTextColumn prop="planName" label="预案名称" :min-width="180" />
          <el-table-column prop="riskLevel" label="风险等级" width="100">
            <template #default="{ row }">
              <el-tag :type="getRiskLevelType(row.riskLevel)" round>{{ getRiskLevelText(row.riskLevel) }}</el-tag>
            </template>
          </el-table-column>
          <StandardTableTextColumn prop="description" label="描述" :min-width="220" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" round>{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <StandardTableTextColumn prop="createTime" label="创建时间" :width="180" />
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
      subtitle="统一通过右侧抽屉维护应急预案与响应步骤。"
      size="44rem"
      @close="handleFormClose"
    >
      <div class="ops-drawer-stack">
        <div class="ops-drawer-note">
          <strong>维护提示</strong>
          <span>建议先明确风险等级和执行状态，再补齐响应步骤与联系人列表，确保应急场景下能够直接复用预案。</span>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="ops-drawer-form">
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>预案基础信息</h3>
                <p>统一维护预案名称、适用风险等级和启停状态，保证不同层级风险场景下快速选用对应预案。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="预案名称" prop="planName">
                <el-input v-model="form.planName" placeholder="请输入预案名称" />
              </el-form-item>
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="form.status">
                  <el-radio :value="0">启用</el-radio>
                  <el-radio :value="1">停用</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="风险等级" prop="riskLevel" class="ops-drawer-grid__full">
                <el-radio-group v-model="form.riskLevel">
                  <el-radio value="critical">严重</el-radio>
                  <el-radio value="warning">警告</el-radio>
                  <el-radio value="info">提醒</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="描述" prop="description" class="ops-drawer-grid__full">
                <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入预案描述、适用范围或启动说明" />
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>响应编排</h3>
                <p>统一维护步骤清单和联系人列表，便于在处置过程中直接按预案执行并找到对应责任人。</p>
              </div>
            </div>
            <div class="ops-drawer-grid ops-drawer-grid--single">
              <el-form-item label="响应步骤" prop="responseSteps">
                <el-input v-model="form.responseSteps" type="textarea" :rows="6" placeholder="请输入响应步骤（JSON格式）" />
              </el-form-item>
              <el-form-item label="联系人列表" prop="contactList">
                <el-input v-model="form.contactList" type="textarea" :rows="4" placeholder="请输入联系人列表（JSON格式）" />
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
import MetricCard from '@/components/MetricCard.vue';
import PanelCard from '@/components/PanelCard.vue';
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue';
import StandardFormDrawer from '@/components/StandardFormDrawer.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import { useServerPagination } from '@/composables/useServerPagination';
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm';
import { pagePlanList, addPlan, updatePlan, deletePlan } from '../api/emergencyPlan';
import type { EmergencyPlan } from '../api/emergencyPlan';

const loading = ref(false);
const formVisible = ref(false);
const planList = ref<EmergencyPlan[]>([]);
const tableRef = ref();
const selectedRows = ref<EmergencyPlan[]>([]);

const filters = reactive({
  planName: '',
  riskLevel: '',
  status: '' as '' | number
});

const { pagination, applyPageResult, resetPage, setPageSize, setPageNum } = useServerPagination();

const formRef = ref();
const formTitle = computed(() => (form.id ? '编辑预案' : '新增预案'));
const form = reactive({
  id: undefined as number | undefined,
  planName: '',
  riskLevel: 'warning',
  description: '',
  responseSteps: '',
  contactList: '',
  status: 0
});

const rules = {
  planName: [{ required: true, message: '请输入预案名称', trigger: 'blur' }],
  riskLevel: [{ required: true, message: '请选择风险等级', trigger: 'change' }],
  responseSteps: [{ required: true, message: '请输入响应步骤', trigger: 'blur' }]
};

const submitLoading = ref(false);
const planAdvice = '优先检查严重风险预案和启用中的执行方案';

const enabledCount = computed(() => planList.value.filter((item) => item.status === 0).length);
const criticalCount = computed(() => planList.value.filter((item) => item.riskLevel === 'critical').length);
const warningCount = computed(() => planList.value.filter((item) => item.riskLevel === 'warning').length);

const getRiskLevelType = (riskLevel: string) => {
  switch (riskLevel) {
    case 'critical':
      return 'danger';
    case 'warning':
      return 'warning';
    case 'info':
      return 'info';
    default:
      return 'info';
  }
};

const getRiskLevelText = (riskLevel: string) => {
  switch (riskLevel) {
    case 'critical':
      return '严重';
    case 'warning':
      return '警告';
    case 'info':
      return '提醒';
    default:
      return riskLevel;
  }
};

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

const loadPlanList = async () => {
  loading.value = true;
  try {
    const res = await pagePlanList({
      planName: filters.planName || undefined,
      riskLevel: filters.riskLevel || undefined,
      status: filters.status === '' ? undefined : Number(filters.status),
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    });
    if (res.code === 200) {
      planList.value = applyPageResult(res.data);
    }
  } catch (error) {
    console.error('查询预案列表失败', error);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  resetPage();
  void loadPlanList();
};

const handleReset = () => {
  filters.planName = '';
  filters.riskLevel = '';
  filters.status = '';
  resetPage();
  void loadPlanList();
};

const handleSizeChange = (size: number) => {
  setPageSize(size);
  void loadPlanList();
};

const handlePageChange = (page: number) => {
  setPageNum(page);
  void loadPlanList();
};

const handleSelectionChange = (rows: EmergencyPlan[]) => {
  selectedRows.value = rows;
};

const clearSelection = () => {
  tableRef.value?.clearSelection?.();
  selectedRows.value = [];
};

const handleRefresh = () => {
  clearSelection();
  void loadPlanList();
};

const resetPlanForm = () => {
  form.id = undefined;
  form.planName = '';
  form.riskLevel = 'warning';
  form.description = '';
  form.responseSteps = '';
  form.contactList = '';
  form.status = 0;
};

const handleAdd = () => {
  resetPlanForm();
  formVisible.value = true;
};

const handleEdit = (row: EmergencyPlan) => {
  form.id = row.id;
  form.planName = row.planName;
  form.riskLevel = row.riskLevel;
  form.description = row.description || '';
  form.responseSteps = row.responseSteps || '';
  form.contactList = row.contactList || '';
  form.status = row.status;
  formVisible.value = true;
};

const handleDelete = async (row: EmergencyPlan) => {
  try {
    await confirmDelete('预案', row.planName);
    const res = await deletePlan(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      void loadPlanList();
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return;
    }
    console.error('删除预案失败', error);
  }
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    submitLoading.value = true;
    const res = form.id ? await updatePlan(form) : await addPlan(form);
    if (res.code === 200) {
      ElMessage.success(form.id ? '更新成功' : '新增成功');
      formVisible.value = false;
      void loadPlanList();
    }
  } catch (error) {
    console.error('提交表单失败', error);
  } finally {
    submitLoading.value = false;
  }
};

const handleFormClose = () => {
  formRef.value?.clearValidate?.();
  resetPlanForm();
};

onMounted(() => {
  void loadPlanList();
});
</script>

<style scoped>
.emergency-plan-view {
  padding: 20px;
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(243, 247, 253, 0.66));
  border: 1px solid rgba(41, 60, 92, 0.1);
}
</style>

