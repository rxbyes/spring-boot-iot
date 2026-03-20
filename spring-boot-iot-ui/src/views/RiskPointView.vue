<template>
  <div class="ops-workbench risk-point-view">
    <PanelCard
      eyebrow="Risk Point Workspace"
      title="风险对象中心"
      description="统一管理风险点档案、风险等级、启停状态与设备绑定，支撑后续监测、阈值和联动配置。"
      class="ops-hero-card"
    >
      <template #actions>
        <el-button type="primary" @click="handleAdd">新增风险点</el-button>
      </template>
      <div class="ops-kpi-grid">
        <MetricCard label="风险点总数" :value="String(pagination.total)" :badge="{ label: '配置基线', tone: 'brand' }" />
        <MetricCard label="当前页启用" :value="String(enabledCount)" :badge="{ label: '生效中', tone: 'success' }" />
        <MetricCard label="当前页严重" :value="String(criticalCount)" :badge="{ label: '优先排查', tone: 'danger' }" />
        <MetricCard label="当前页停用" :value="String(disabledCount)" :badge="{ label: '待复核', tone: 'warning' }" />
      </div>
      <div class="ops-inline-note">
        风险点作为风险平台的基础对象，列表、维护抽屉和绑定设备抽屉已统一为同一套工作台风格，方便值班与治理人员连续操作。
      </div>
    </PanelCard>

    <PanelCard
      eyebrow="Risk Filters"
      title="筛选条件"
      description="优先核查高风险且已启用的风险点，快速定位需要补录、整改或重新绑定设备的对象。"
      class="ops-filter-card"
    >
      <el-form :model="filters" label-position="top" class="ops-filter-form">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="风险点编号">
              <el-input v-model="filters.riskPointCode" placeholder="请输入风险点编号" clearable @keyup.enter="handleSearch" />
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
              <el-input :model-value="riskPointAdvice" disabled />
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
      eyebrow="Risk Point List"
      title="风险点列表"
      :description="`当前 ${pagination.total} 条风险点记录，支持档案维护和设备绑定。`"
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

      <div v-if="loading" class="ops-state">正在加载风险点列表...</div>
      <div v-else-if="riskPointList.length === 0" class="ops-state">暂无符合条件的风险点记录</div>
      <template v-else>
        <el-table
          ref="tableRef"
          :data="riskPointList"
          border
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <StandardTableTextColumn prop="riskPointCode" label="风险点编号" :width="150" />
          <StandardTableTextColumn prop="riskPointName" label="风险点名称" :min-width="180" />
          <StandardTableTextColumn prop="regionName" label="区域" :width="120" />
          <el-table-column prop="riskLevel" label="风险等级" width="100">
            <template #default="{ row }">
              <el-tag :type="getRiskLevelType(row.riskLevel)" round>{{ getRiskLevelText(row.riskLevel) }}</el-tag>
            </template>
          </el-table-column>
          <StandardTableTextColumn prop="responsiblePhone" label="负责人电话" :width="140" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" round>{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <StandardTableTextColumn prop="createTime" label="创建时间" :width="180" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
              <el-button type="primary" link @click="handleBindDevice(row)">绑定设备</el-button>
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
      subtitle="统一通过右侧抽屉维护风险点基础信息。"
      size="42rem"
      @close="handleFormClose"
    >
      <div class="ops-drawer-stack">
        <div class="ops-drawer-note">
          <strong>配置提示</strong>
          <span>风险点编号、等级和状态会直接影响监测绑定、告警展示和处置优先级，建议按现场对象口径统一命名。</span>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="ops-drawer-form">
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>基础信息</h3>
                <p>维护风险点主档、归属区域与风险等级，为后续监测和处置流程提供统一标识。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="风险点编号" prop="riskPointCode">
                <el-input v-model="form.riskPointCode" placeholder="请输入风险点编号" />
              </el-form-item>
              <el-form-item label="风险点名称" prop="riskPointName">
                <el-input v-model="form.riskPointName" placeholder="请输入风险点名称" />
              </el-form-item>
              <el-form-item label="区域" prop="regionName">
                <el-input v-model="form.regionName" placeholder="请输入区域名称" />
              </el-form-item>
              <el-form-item label="风险等级" prop="riskLevel">
                <el-select v-model="form.riskLevel" placeholder="请选择风险等级">
                  <el-option label="严重" value="critical" />
                  <el-option label="警告" value="warning" />
                  <el-option label="提醒" value="info" />
                </el-select>
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>治理信息</h3>
                <p>补齐责任电话、启停状态和风险说明，便于值班与治理人员快速确认风险点责任归属。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="负责人电话" prop="responsiblePhone">
                <el-input v-model="form.responsiblePhone" placeholder="请输入负责人电话" />
              </el-form-item>
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="form.status">
                  <el-radio :value="0">启用</el-radio>
                  <el-radio :value="1">停用</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="描述" prop="description" class="ops-drawer-grid__full">
                <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入风险点描述、场景说明或治理备注" />
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

    <StandardFormDrawer
      v-model="bindDeviceVisible"
      eyebrow="Risk Platform Form"
      title="绑定设备"
      subtitle="统一通过右侧抽屉为风险点绑定设备与测点。"
      size="42rem"
      @close="handleBindDrawerClose"
    >
      <div class="ops-drawer-stack">
        <div class="ops-drawer-note">
          <strong>绑定提示</strong>
          <span>绑定完成后，风险对象会直接联动实时监测台、阈值策略和告警运营台，请确认设备与测点归属关系准确。</span>
        </div>
        <el-form :model="bindForm" label-position="top" class="ops-drawer-form">
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>绑定对象</h3>
                <p>确认当前风险点并选择要关联的设备、测点，形成后续监测链路。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="风险点" class="ops-drawer-grid__full ops-drawer-readonly">
                <el-input v-model="bindForm.riskPointName" disabled />
              </el-form-item>
              <el-form-item label="设备">
                <el-select v-model="bindForm.deviceId" placeholder="请选择设备">
                  <el-option v-for="device in deviceList" :key="device.id" :label="device.deviceName" :value="device.id">
                    {{ device.deviceCode }} - {{ device.deviceName }}
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="测点">
                <el-select v-model="bindForm.metricIdentifier" placeholder="请选择测点">
                  <el-option v-for="metric in metricList" :key="metric.identifier" :label="metric.name" :value="metric.identifier">
                    {{ metric.name }}
                  </el-option>
                </el-select>
              </el-form-item>
            </div>
          </section>
        </el-form>
      </div>
      <template #footer>
        <StandardDrawerFooter
          :confirm-loading="submitLoading"
          @cancel="bindDeviceVisible = false"
          @confirm="handleBindSubmit"
        />
      </template>
    </StandardFormDrawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from '@/utils/message';
import MetricCard from '@/components/MetricCard.vue';
import PanelCard from '@/components/PanelCard.vue';
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue';
import StandardFormDrawer from '@/components/StandardFormDrawer.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import { useServerPagination } from '@/composables/useServerPagination';
import { listDeviceOptions, getDeviceMetricOptions } from '@/api/iot';
import type { DeviceMetricOption, DeviceOption } from '@/types/api';
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm';
import { pageRiskPointList, addRiskPoint, updateRiskPoint, deleteRiskPoint, bindDevice } from '../api/riskPoint';
import type { RiskPoint } from '../api/riskPoint';

const loading = ref(false);
const formVisible = ref(false);
const bindDeviceVisible = ref(false);
const riskPointList = ref<RiskPoint[]>([]);
const deviceList = ref<DeviceOption[]>([]);
const metricList = ref<DeviceMetricOption[]>([]);
const tableRef = ref();
const selectedRows = ref<RiskPoint[]>([]);

const filters = reactive({
  riskPointCode: '',
  riskLevel: '',
  status: '' as '' | number
});

const { pagination, applyPageResult, resetPage, setPageSize, setPageNum } = useServerPagination();

const formRef = ref();
const formTitle = computed(() => (form.id ? '编辑风险点' : '新增风险点'));
const form = reactive({
  id: undefined as number | undefined,
  riskPointCode: '',
  riskPointName: '',
  regionId: 0,
  regionName: '',
  responsibleUser: 0,
  responsiblePhone: '',
  riskLevel: 'info',
  description: '',
  status: 0
});

const rules = {
  riskPointCode: [{ required: true, message: '请输入风险点编号', trigger: 'blur' }],
  riskPointName: [{ required: true, message: '请输入风险点名称', trigger: 'blur' }],
  riskLevel: [{ required: true, message: '请选择风险等级', trigger: 'change' }]
};

const bindForm = reactive({
  riskPointId: 0,
  riskPointName: '',
  deviceId: 0,
  deviceCode: '',
  deviceName: '',
  metricIdentifier: '',
  metricName: ''
});
const submitLoading = ref(false);
const riskPointAdvice = '优先核查高风险且已启用的风险点';

const enabledCount = computed(() => riskPointList.value.filter((item) => item.status === 0).length);
const criticalCount = computed(() => riskPointList.value.filter((item) => item.riskLevel === 'critical').length);
const disabledCount = computed(() => riskPointList.value.filter((item) => item.status === 1).length);

const loadDeviceOptions = async () => {
  try {
    const res = await listDeviceOptions();
    if (res.code === 200) {
      deviceList.value = res.data || [];
    }
  } catch (error) {
    console.error('加载设备选项失败', error);
    ElMessage.error('加载设备列表失败');
  }
};

const loadMetricOptions = async (deviceId: string | number) => {
  try {
    const res = await getDeviceMetricOptions(deviceId);
    if (res.code === 200) {
      metricList.value = res.data || [];
    }
  } catch (error) {
    console.error('加载测点选项失败', error);
    ElMessage.error('加载测点列表失败');
  }
};

const getRiskLevelType = (level: string) => {
  switch (level) {
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

const getRiskLevelText = (level: string) => {
  switch (level) {
    case 'critical':
      return '严重';
    case 'warning':
      return '警告';
    case 'info':
      return '提醒';
    default:
      return level;
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

const loadRiskPointList = async () => {
  loading.value = true;
  try {
    const res = await pageRiskPointList({
      riskPointCode: filters.riskPointCode || undefined,
      riskLevel: filters.riskLevel || undefined,
      status: filters.status === '' ? undefined : Number(filters.status),
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    });
    if (res.code === 200) {
      riskPointList.value = applyPageResult(res.data);
    }
  } catch (error) {
    console.error('查询风险点列表失败', error);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  resetPage();
  void loadRiskPointList();
};

const handleReset = () => {
  filters.riskPointCode = '';
  filters.riskLevel = '';
  filters.status = '';
  resetPage();
  void loadRiskPointList();
};

const handleSizeChange = (size: number) => {
  setPageSize(size);
  void loadRiskPointList();
};

const handlePageChange = (page: number) => {
  setPageNum(page);
  void loadRiskPointList();
};

const handleSelectionChange = (rows: RiskPoint[]) => {
  selectedRows.value = rows;
};

const clearSelection = () => {
  tableRef.value?.clearSelection?.();
  selectedRows.value = [];
};

const handleRefresh = () => {
  clearSelection();
  void loadRiskPointList();
};

const resetRiskPointForm = () => {
  form.id = undefined;
  form.riskPointCode = '';
  form.riskPointName = '';
  form.regionId = 0;
  form.regionName = '';
  form.responsibleUser = 0;
  form.responsiblePhone = '';
  form.riskLevel = 'info';
  form.description = '';
  form.status = 0;
};

const resetBindForm = () => {
  bindForm.riskPointId = 0;
  bindForm.riskPointName = '';
  bindForm.deviceId = 0;
  bindForm.deviceCode = '';
  bindForm.deviceName = '';
  bindForm.metricIdentifier = '';
  bindForm.metricName = '';
  metricList.value = [];
};

const handleAdd = () => {
  resetRiskPointForm();
  formVisible.value = true;
};

const handleEdit = (row: RiskPoint) => {
  form.id = row.id;
  form.riskPointCode = row.riskPointCode;
  form.riskPointName = row.riskPointName;
  form.regionId = row.regionId;
  form.regionName = row.regionName;
  form.responsibleUser = row.responsibleUser;
  form.responsiblePhone = row.responsiblePhone;
  form.riskLevel = row.riskLevel;
  form.description = row.description || '';
  form.status = row.status;
  formVisible.value = true;
};

const handleDelete = async (row: RiskPoint) => {
  try {
    await confirmDelete('风险点', row.riskPointName);
    const res = await deleteRiskPoint(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      void loadRiskPointList();
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return;
    }
    console.error('删除风险点失败', error);
  }
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    submitLoading.value = true;
    const res = form.id ? await updateRiskPoint(form) : await addRiskPoint(form);
    if (res.code === 200) {
      ElMessage.success(form.id ? '更新成功' : '新增成功');
      formVisible.value = false;
      void loadRiskPointList();
    }
  } catch (error) {
    console.error('提交表单失败', error);
  } finally {
    submitLoading.value = false;
  }
};

const handleBindDevice = async (row: RiskPoint) => {
  resetBindForm();
  bindForm.riskPointId = Number(row.id);
  bindForm.riskPointName = row.riskPointName;
  await loadDeviceOptions();
  bindDeviceVisible.value = true;
};

const handleBindSubmit = async () => {
  if (!bindForm.deviceId || !bindForm.metricIdentifier) {
    ElMessage.warning('请选择设备和测点');
    return;
  }
  try {
    submitLoading.value = true;
    const selectedDevice = deviceList.value.find((device) => String(device.id) === String(bindForm.deviceId));
    const selectedMetric = metricList.value.find((metric) => metric.identifier === bindForm.metricIdentifier);
    if (!selectedDevice || !selectedMetric) {
      ElMessage.warning('请选择有效的设备和测点');
      return;
    }
    const res = await bindDevice({
      riskPointId: bindForm.riskPointId,
      deviceId: bindForm.deviceId,
      deviceCode: selectedDevice.deviceCode,
      deviceName: selectedDevice.deviceName,
      metricIdentifier: selectedMetric.identifier,
      metricName: selectedMetric.name
    });
    if (res.code === 200) {
      ElMessage.success('绑定成功');
      bindDeviceVisible.value = false;
      void loadRiskPointList();
    }
  } catch (error) {
    console.error('绑定设备失败', error);
  } finally {
    submitLoading.value = false;
  }
};

const handleFormClose = () => {
  formRef.value?.clearValidate?.();
  resetRiskPointForm();
};

const handleBindDrawerClose = () => {
  resetBindForm();
};

watch(
  () => bindForm.deviceId,
  async (deviceId) => {
    bindForm.deviceCode = '';
    bindForm.deviceName = '';
    bindForm.metricIdentifier = '';
    bindForm.metricName = '';
    metricList.value = [];
    if (!deviceId) {
      return;
    }
    const selectedDevice = deviceList.value.find((device) => String(device.id) === String(deviceId));
    if (selectedDevice) {
      bindForm.deviceCode = selectedDevice.deviceCode;
      bindForm.deviceName = selectedDevice.deviceName;
    }
    await loadMetricOptions(deviceId);
  }
);

watch(
  () => bindForm.metricIdentifier,
  (metricIdentifier) => {
    const selectedMetric = metricList.value.find((metric) => metric.identifier === metricIdentifier);
    bindForm.metricName = selectedMetric?.name || '';
  }
);

onMounted(() => {
  void loadRiskPointList();
});
</script>

<style scoped>
.risk-point-view {
  padding: 20px;
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(243, 247, 253, 0.66));
  border: 1px solid rgba(41, 60, 92, 0.1);
}
</style>

