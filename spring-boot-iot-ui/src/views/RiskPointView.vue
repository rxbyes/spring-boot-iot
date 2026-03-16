<template>
  <div class="risk-point-view">
    <div class="risk-point-header">
      <h1>风险点管�?/h1>
      <el-button type="primary" @click="handleAdd">新增风险�?/el-button>
    </div>

    <div class="risk-point-filters">
      <el-form :model="filters" label-position="left">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="风险点编�?>
              <el-input v-model="filters.riskPointCode" placeholder="请输入风险点编号" clearable />
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
            <el-form-item label="状�?>
              <el-select v-model="filters.status" placeholder="请选择状�? clearable>
                <el-option label="启用" :value="0" />
                <el-option label="停用" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="">
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <div class="risk-point-list">
      <el-table :data="riskPointList" v-loading="loading" border>
        <el-table-column prop="riskPointCode" label="风险点编�? width="150" />
        <el-table-column prop="riskPointName" label="风险点名�? />
        <el-table-column prop="regionName" label="区域" width="120" />
        <el-table-column prop="riskLevel" label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getRiskLevelType(row.riskLevel)">{{ getRiskLevelText(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responsiblePhone" label="负责人电�? width="120" />
        <el-table-column prop="status" label="状�? width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleBindDevice(row)">绑定设备</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="risk-point-pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 风险点表单对话框 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="风险点编�? prop="riskPointCode">
          <el-input v-model="form.riskPointCode" placeholder="请输入风险点编号" />
        </el-form-item>
        <el-form-item label="风险点名�? prop="riskPointName">
          <el-input v-model="form.riskPointName" placeholder="请输入风险点名称" />
        </el-form-item>
        <el-form-item label="区域" prop="regionName">
          <el-input v-model="form.regionName" placeholder="请输入区域名�? />
        </el-form-item>
        <el-form-item label="风险等级" prop="riskLevel">
          <el-select v-model="form.riskLevel" placeholder="请选择风险等级" style="width: 100%">
            <el-option label="严重" value="critical" />
            <el-option label="警告" value="warning" />
            <el-option label="提醒" value="info" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人电�? prop="responsiblePhone">
          <el-input v-model="form.responsiblePhone" placeholder="请输入负责人电话" />
        </el-form-item>
        <el-form-item label="状�? prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="0">启用</el-radio>
            <el-radio :label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描�? />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 绑定设备对话�?-->
    <el-dialog v-model="bindDeviceVisible" title="绑定设备" width="600px">
      <el-form :model="bindForm" label-width="100px" v-loading="bindDialogLoading">
        <el-form-item label="风险�?>
          <el-input v-model="bindForm.riskPointName" disabled />
        </el-form-item>
        <el-form-item label="设备">
          <el-select
            v-model="bindForm.deviceId"
            placeholder="请选择设备"
            style="width: 100%"
            filterable
            clearable
            @change="handleDeviceChange"
          >
            <el-option v-for="device in deviceList" :key="device.id" :label="device.deviceName" :value="device.id">
              {{ device.deviceCode }} - {{ device.deviceName }}
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="测点">
          <el-select v-model="bindForm.metricIdentifier" placeholder="请选择测点" style="width: 100%" clearable>
            <el-option v-for="metric in metricList" :key="metric.identifier" :label="metric.name" :value="metric.identifier">
              {{ metric.name }}
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindDeviceVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBindSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage } from '@/utils/message';
import { ElMessageBox } from '@/utils/messageBox';
import { bindDevice as bindRiskPointDevice, getBoundDevices, getRiskPointList, addRiskPoint, updateRiskPoint, deleteRiskPoint } from '../api/riskPoint';
import type { RiskPoint, RiskPointDevice } from '../api/riskPoint';
import { getDeviceMetricOptions, listDeviceOptions } from '../api/iot';
import type { DeviceMetricOption, DeviceOption } from '../types/api';

// 状�?
const loading = ref(false);
const formVisible = ref(false);
const bindDeviceVisible = ref(false);
const riskPointList = ref<RiskPoint[]>([]);
const deviceList = ref<DeviceOption[]>([]);
const metricList = ref<DeviceMetricOption[]>([]);
const bindDialogLoading = ref(false);

// 查询条件
const filters = reactive({
  riskPointCode: '',
  riskLevel: '',
  status: ''
});

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
});

// 表单
const formRef = ref();
const formTitle = computed(() => form.id ? '编辑风险�? : '新增风险�?);
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

// 绑定设备表单
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

// 获取风险等级类型
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

// 获取风险等级文本
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

// 获取状态类�?
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

// 获取状态文�?
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

// 获取风险点列�?
const loadRiskPointList = async () => {
  loading.value = true;
  try {
    const res = await getRiskPointList({
      riskPointCode: filters.riskPointCode || undefined,
      riskLevel: filters.riskLevel || undefined,
      status: filters.status ? parseInt(filters.status) : undefined
    });
    if (res.code === 200) {
      riskPointList.value = res.data || [];
      pagination.total = res.data?.length || 0;
    }
  } catch (error) {
    console.error('查询风险点列表失�?, error);
  } finally {
    loading.value = false;
  }
};

// 处理搜索
const handleSearch = () => {
  pagination.page = 1;
  loadRiskPointList();
};

// 处理重置
const handleReset = () => {
  filters.riskPointCode = '';
  filters.riskLevel = '';
  filters.status = '';
  pagination.page = 1;
  loadRiskPointList();
};

// 处理大小变化
const handleSizeChange = () => {
  loadRiskPointList();
};

// 处理页码变化
const handlePageChange = () => {
  loadRiskPointList();
};

// 新增风险�?
const handleAdd = () => {
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
  formVisible.value = true;
};

// 编辑风险�?
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

// 删除风险�?
const handleDelete = async (row: RiskPoint) => {
  try {
    await ElMessageBox.confirm('确定要删除该风险点吗�?, '删除风险�?, {
      type: 'warning'
    });
    const res = await deleteRiskPoint(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      loadRiskPointList();
    }
  } catch (error) {
    console.error('删除风险点失�?, error);
  }
};

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    submitLoading.value = true;
    const res = form.id ? await updateRiskPoint(form) : await addRiskPoint(form);
    if (res.code === 200) {
      ElMessage.success(form.id ? '更新成功' : '新增成功');
      formVisible.value = false;
      loadRiskPointList();
    }
  } catch (error) {
    console.error('提交表单失败', error);
  } finally {
    submitLoading.value = false;
  }
};

// 绑定设备
const handleBindDevice = (row: RiskPoint) => {
  resetBindForm();
  bindForm.riskPointId = row.id;
  bindForm.riskPointName = row.riskPointName;
  bindDeviceVisible.value = true;
  void loadBindableDevices(row.id);
};

// 提交绑定
const handleBindSubmit = async () => {
  if (!bindForm.deviceId || !bindForm.metricIdentifier) {
    ElMessage.warning('请选择设备和测�?);
    return;
  }
  try {
    submitLoading.value = true;
    const selectedMetric = metricList.value.find((metric) => metric.identifier === bindForm.metricIdentifier);
    bindForm.metricName = selectedMetric?.name || bindForm.metricIdentifier;
    const res = await bindRiskPointDevice({
      riskPointId: bindForm.riskPointId,
      deviceId: bindForm.deviceId,
      deviceCode: bindForm.deviceCode,
      deviceName: bindForm.deviceName,
      metricIdentifier: bindForm.metricIdentifier,
      metricName: bindForm.metricName
    });
    if (res.code === 200) {
      ElMessage.success('绑定成功');
      bindDeviceVisible.value = false;
      resetBindForm();
      loadRiskPointList();
    }
  } catch (error) {
    console.error('绑定设备失败', error);
  } finally {
    submitLoading.value = false;
  }
};

const loadBindableDevices = async (riskPointId: number) => {
  bindDialogLoading.value = true;
  try {
    const [deviceRes, boundRes] = await Promise.all([listDeviceOptions(), getBoundDevices(riskPointId)]);
    const boundDeviceIds = new Set((boundRes.data || []).map((item: RiskPointDevice) => item.deviceId));
    deviceList.value = (deviceRes.data || []).filter((device) => !boundDeviceIds.has(device.id));
    metricList.value = [];
    if (deviceList.value.length === 0) {
      ElMessage.info('当前没有可绑定的设备');
    }
  } catch (error) {
    deviceList.value = [];
    metricList.value = [];
    ElMessage.error(error instanceof Error ? error.message : '加载绑定设备选项失败');
  } finally {
    bindDialogLoading.value = false;
  }
};

const handleDeviceChange = async (deviceId?: number) => {
  bindForm.metricIdentifier = '';
  bindForm.metricName = '';
  metricList.value = [];
  const selectedDevice = deviceList.value.find((device) => device.id === deviceId);
  bindForm.deviceCode = selectedDevice?.deviceCode || '';
  bindForm.deviceName = selectedDevice?.deviceName || '';
  if (!deviceId) {
    return;
  }
  bindDialogLoading.value = true;
  try {
    const res = await getDeviceMetricOptions(deviceId);
    metricList.value = res.data || [];
    if (metricList.value.length === 0) {
      ElMessage.warning('当前设备没有可绑定的测点，请先完善物模型或产生属性数�?);
    }
  } catch (error) {
    metricList.value = [];
    ElMessage.error(error instanceof Error ? error.message : '加载设备测点失败');
  } finally {
    bindDialogLoading.value = false;
  }
};

const resetBindForm = () => {
  bindForm.riskPointId = 0;
  bindForm.riskPointName = '';
  bindForm.deviceId = 0;
  bindForm.deviceCode = '';
  bindForm.deviceName = '';
  bindForm.metricIdentifier = '';
  bindForm.metricName = '';
  deviceList.value = [];
  metricList.value = [];
};

// 初始�?
onMounted(() => {
  loadRiskPointList();
});
</script>

<style scoped>
.risk-point-view {
  padding: 20px;
}

.risk-point-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.risk-point-header h1 {
  font-size: 24px;
  margin: 0;
}

.risk-point-filters {
  margin-bottom: 20px;
  padding: 15px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.risk-point-list {
  margin-bottom: 20px;
}

.risk-point-pagination {
  display: flex;
  justify-content: flex-end;
}
</style>

