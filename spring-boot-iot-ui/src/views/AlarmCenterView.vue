<template>
  <div class="alarm-center-view">
    <div class="alarm-header">
      <h1>告警中心</h1>
      <div class="alarm-stats">
        <el-statistic title="今日告警" :value="stats.todayAlarms" />
        <el-statistic title="未确认告警" :value="stats.unconfirmedAlarms" />
        <el-statistic title="已确认告警" :value="stats.confirmedAlarms" />
        <el-statistic title="已关闭告警" :value="stats.closedAlarms" />
      </div>
    </div>

    <div class="alarm-filters">
      <el-form :model="filters" label-position="left">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="设备编码">
              <el-input v-model="filters.deviceCode" placeholder="请输入设备编码" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="告警等级">
              <el-select v-model="filters.alarmLevel" placeholder="请选择告警等级" clearable>
                <el-option label="严重" value="critical" />
                <el-option label="警告" value="warning" />
                <el-option label="提醒" value="info" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态">
              <el-select v-model="filters.status" placeholder="请选择状态" clearable>
                <el-option label="未确认" :value="0" />
                <el-option label="已确认" :value="1" />
                <el-option label="已抑制" :value="2" />
                <el-option label="已关闭" :value="3" />
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

    <div class="alarm-list">
      <el-table :data="alarmList" v-loading="loading" border>
        <el-table-column prop="alarmCode" label="告警编号" width="180" />
        <el-table-column prop="alarmTitle" label="告警标题" />
        <el-table-column prop="alarmLevel" label="告警等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getAlarmLevelType(row.alarmLevel)">{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="regionName" label="区域" width="120" />
        <el-table-column prop="riskPointName" label="风险点" width="150" />
        <el-table-column prop="deviceName" label="设备名称" width="150" />
        <el-table-column prop="metricName" label="测点名称" width="150" />
        <el-table-column prop="currentValue" label="当前值" width="120" />
        <el-table-column prop="thresholdValue" label="阈值" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="triggerTime" label="触发时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" type="primary" link @click="handleConfirm(row)">确认</el-button>
            <el-button v-if="row.status === 0" type="primary" link @click="handleSuppress(row)">抑制</el-button>
            <el-button v-if="row.status !== 3" type="primary" link @click="handleClose(row)">关闭</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="alarm-pagination">
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

    <el-dialog v-model="detailVisible" title="告警详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="告警编号">{{ detail?.alarmCode }}</el-descriptions-item>
        <el-descriptions-item label="告警标题">{{ detail?.alarmTitle }}</el-descriptions-item>
        <el-descriptions-item label="告警等级">{{ getAlarmLevelText(detail?.alarmLevel || '') }}</el-descriptions-item>
        <el-descriptions-item label="告警类型">{{ detail?.alarmType }}</el-descriptions-item>
        <el-descriptions-item label="区域">{{ detail?.regionName }}</el-descriptions-item>
        <el-descriptions-item label="风险点">{{ detail?.riskPointName }}</el-descriptions-item>
        <el-descriptions-item label="设备编码">{{ detail?.deviceCode }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detail?.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="测点名称">{{ detail?.metricName }}</el-descriptions-item>
        <el-descriptions-item label="当前值">{{ detail?.currentValue }}</el-descriptions-item>
        <el-descriptions-item label="阈值">{{ detail?.thresholdValue }}</el-descriptions-item>
        <el-descriptions-item label="触发时间">{{ detail?.triggerTime }}</el-descriptions-item>
        <el-descriptions-item label="规则名称">{{ detail?.ruleName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getStatusText(detail?.status ?? -1) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from '@/utils/message';
import { ElMessageBox } from '@/utils/messageBox';

import { closeAlarm, confirmAlarm, getAlarmDetail, getAlarmList, suppressAlarm } from '../api/alarm';
import type { AlarmRecord } from '../api/alarm';

const loading = ref(false);
const detailVisible = ref(false);
const alarmList = ref<AlarmRecord[]>([]);
const detail = ref<AlarmRecord | null>(null);

const stats = ref({
  todayAlarms: 0,
  unconfirmedAlarms: 0,
  confirmedAlarms: 0,
  closedAlarms: 0
});

const filters = reactive({
  deviceCode: '',
  alarmLevel: '',
  status: ''
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
});

const getAlarmLevelType = (level: string) => {
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

const getAlarmLevelText = (level: string) => {
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
      return 'danger';
    case 1:
      return 'success';
    case 2:
    case 3:
      return 'info';
    default:
      return 'info';
  }
};

const getStatusText = (status: number) => {
  switch (status) {
    case 0:
      return '未确认';
    case 1:
      return '已确认';
    case 2:
      return '已抑制';
    case 3:
      return '已关闭';
    default:
      return String(status);
  }
};

const loadAlarmList = async () => {
  loading.value = true;
  try {
    const res = await getAlarmList({
      deviceCode: filters.deviceCode || undefined,
      alarmLevel: filters.alarmLevel || undefined,
      status: filters.status ? parseInt(filters.status, 10) : undefined
    });

    if (res.code === 200) {
      alarmList.value = res.data || [];
      pagination.total = alarmList.value.length;
      stats.value.todayAlarms = alarmList.value.length;
      stats.value.unconfirmedAlarms = alarmList.value.filter((a) => a.status === 0).length;
      stats.value.confirmedAlarms = alarmList.value.filter((a) => a.status === 1).length;
      stats.value.closedAlarms = alarmList.value.filter((a) => a.status === 3).length;
    }
  } catch (error) {
    console.error('查询告警列表失败', error);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.page = 1;
  void loadAlarmList();
};

const handleReset = () => {
  filters.deviceCode = '';
  filters.alarmLevel = '';
  filters.status = '';
  pagination.page = 1;
  void loadAlarmList();
};

const handleSizeChange = () => {
  void loadAlarmList();
};

const handlePageChange = () => {
  void loadAlarmList();
};

const handleViewDetail = async (row: AlarmRecord) => {
  loading.value = true;
  try {
    const res = await getAlarmDetail(row.id);
    if (res.code === 200) {
      detail.value = res.data;
      detailVisible.value = true;
    }
  } catch (error) {
    console.error('查询告警详情失败', error);
  } finally {
    loading.value = false;
  }
};

const handleConfirm = async (row: AlarmRecord) => {
  try {
    await ElMessageBox.confirm('确定要确认该告警吗？', '确认告警', { type: 'warning' });
    const res = await confirmAlarm(row.id, 1);
    if (res.code === 200) {
      ElMessage.success('确认成功');
      void loadAlarmList();
    }
  } catch (error) {
    console.error('确认告警失败', error);
  }
};

const handleSuppress = async (row: AlarmRecord) => {
  try {
    await ElMessageBox.confirm('确定要抑制该告警吗？', '抑制告警', { type: 'warning' });
    const res = await suppressAlarm(row.id, 1);
    if (res.code === 200) {
      ElMessage.success('抑制成功');
      void loadAlarmList();
    }
  } catch (error) {
    console.error('抑制告警失败', error);
  }
};

const handleClose = async (row: AlarmRecord) => {
  try {
    await ElMessageBox.confirm('确定要关闭该告警吗？', '关闭告警', { type: 'warning' });
    const res = await closeAlarm(row.id, 1);
    if (res.code === 200) {
      ElMessage.success('关闭成功');
      void loadAlarmList();
    }
  } catch (error) {
    console.error('关闭告警失败', error);
  }
};

onMounted(() => {
  void loadAlarmList();
});
</script>

<style scoped>
.alarm-center-view {
  padding: 20px;
}

.alarm-header {
  margin-bottom: 20px;
}

.alarm-header h1 {
  font-size: 24px;
  margin-bottom: 20px;
}

.alarm-stats {
  display: flex;
  gap: 20px;
}

.alarm-filters {
  margin-bottom: 20px;
  padding: 15px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgb(0 0 0 / 10%);
}

.alarm-list {
  margin-bottom: 20px;
}

.alarm-pagination {
  display: flex;
  justify-content: flex-end;
}
</style>
