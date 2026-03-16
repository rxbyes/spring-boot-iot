<template>
  <div class="event-disposal-view">
    <div class="event-header">
      <h1>事件处置</h1>
      <div class="event-stats">
        <el-statistic title="待派发事�? :value="stats.pendingEvents" />
        <el-statistic title="已派发事�? :value="stats.dispatchedEvents" />
        <el-statistic title="处理中事�? :value="stats.processingEvents" />
        <el-statistic title="已关闭事�? :value="stats.closedEvents" />
      </div>
    </div>

    <div class="event-filters">
      <el-form :model="filters" label-position="left">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="设备编码">
              <el-input v-model="filters.deviceCode" placeholder="请输入设备编�? clearable />
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
                <el-option label="待派�? :value="0" />
                <el-option label="已派�? :value="1" />
                <el-option label="处理�? :value="2" />
                <el-option label="待验�? :value="3" />
                <el-option label="已关�? :value="4" />
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

    <div class="event-list">
      <el-table :data="eventList" v-loading="loading" border>
        <el-table-column prop="eventCode" label="事件编号" width="180" />
        <el-table-column prop="eventTitle" label="事件标题" />
        <el-table-column prop="riskLevel" label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getRiskLevelType(row.riskLevel)">{{ getRiskLevelText(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="regionName" label="区域" width="120" />
        <el-table-column prop="riskPointName" label="风险�? width="150" />
        <el-table-column prop="deviceName" label="设备名称" width="150" />
        <el-table-column prop="metricName" label="测点名称" width="150" />
        <el-table-column prop="currentValue" label="当前�? width="120" />
        <el-table-column prop="status" label="状�? width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="triggerTime" label="触发时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewDetail(row)">详情</el-button>
            <el-button type="primary" link @click="handleDispatch(row)" v-if="row.status === 0">派发</el-button>
            <el-button type="primary" link @click="handleClose(row)" v-if="row.status !== 4">关闭</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="event-pagination">
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

    <!-- 事件详情对话�?-->
    <el-dialog v-model="detailVisible" title="事件详情" width="800px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="事件编号">{{ detail.eventCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事件标题">{{ detail.eventTitle || '-' }}</el-descriptions-item>
        <el-descriptions-item label="风险等级">{{ getRiskLevelText(detail.riskLevel) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="区域">{{ detail.regionName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="风险�?>{{ detail.riskPointName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备编码">{{ detail.deviceCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detail.deviceName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="测点名称">{{ detail.metricName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前�?>{{ detail.currentValue || '-' }}</el-descriptions-item>
        <el-descriptions-item label="触发时间">{{ detail.triggerTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状�?>{{ getStatusText(detail.status) || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无数据" />
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 工单派发对话�?-->
    <el-dialog v-model="dispatchVisible" title="工单派发" width="500px">
      <el-form :model="dispatchForm" label-position="left">
        <el-form-item label="派发�?>
          <el-input v-model="dispatchForm.dispatchUserName" disabled />
        </el-form-item>
        <el-form-item label="接收�?>
          <el-select v-model="dispatchForm.receiveUser" placeholder="请选择接收�? style="width: 100%">
            <el-option label="张三" :value="1" />
            <el-option label="李四" :value="2" />
            <el-option label="王五" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="到场时限(小时)">
          <el-input-number v-model="dispatchForm.arrivalTimeLimit" :min="1" :max="24" />
        </el-form-item>
        <el-form-item label="完成时限(小时)">
          <el-input-number v-model="dispatchForm.completionTimeLimit" :min="1" :max="72" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDispatchConfirm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 事件关闭对话�?-->
    <el-dialog v-model="closeVisible" title="事件关闭" width="500px">
      <el-form :model="closeForm" label-position="left">
        <el-form-item label="关闭原因">
          <el-input v-model="closeForm.closeReason" type="textarea" :rows="3" placeholder="请输入关闭原�? />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCloseConfirm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from '@/utils/message';
import { ElMessageBox } from '@/utils/messageBox';
import { getEventList, closeEvent, dispatchEvent, getEventDetail } from '../api/alarm';
import type { EventRecord } from '../api/alarm';

// 状�?
const loading = ref(false);
const detailVisible = ref(false);
const dispatchVisible = ref(false);
const closeVisible = ref(false);
const eventList = ref<EventRecord[]>([]);
const detail = ref<EventRecord | null>(null);

// 统计数据
const stats = ref({
  pendingEvents: 0,
  dispatchedEvents: 0,
  processingEvents: 0,
  closedEvents: 0
});

// 查询条件
const filters = reactive({
  deviceCode: '',
  riskLevel: '',
  status: ''
});

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
});

// 派发表单
const dispatchForm = reactive({
  dispatchUserName: '系统管理�?,
  receiveUser: 1,
  arrivalTimeLimit: 2,
  completionTimeLimit: 24
});

// 关闭表单
const closeForm = reactive({
  closeReason: ''
});

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
      return 'danger';
    case 1:
      return 'warning';
    case 2:
      return 'primary';
    case 3:
      return 'info';
    case 4:
      return 'success';
    default:
      return 'info';
  }
};

// 获取状态文�?
const getStatusText = (status: number) => {
  switch (status) {
    case 0:
      return '待派�?;
    case 1:
      return '已派�?;
    case 2:
      return '处理�?;
    case 3:
      return '待验�?;
    case 4:
      return '已关�?;
    default:
      return status.toString();
  }
};

// 查询事件列表
const loadEventList = async () => {
  loading.value = true;
  try {
    const params: { deviceCode?: string; riskLevel?: string; status?: number } = {};
    if (filters.deviceCode) params.deviceCode = filters.deviceCode;
    if (filters.riskLevel) params.riskLevel = filters.riskLevel;
    if (filters.status) params.status = parseInt(filters.status);
    
    const res = await getEventList(params);
    if (res.code === 200) {
      eventList.value = res.data || [];
      // 计算统计数据
      stats.value.pendingEvents = eventList.value.filter(e => e.status === 0).length;
      stats.value.dispatchedEvents = eventList.value.filter(e => e.status === 1).length;
      stats.value.processingEvents = eventList.value.filter(e => e.status === 2).length;
      stats.value.closedEvents = eventList.value.filter(e => e.status === 4).length;
    }
  } catch (error) {
    console.error('查询事件列表失败', error);
  } finally {
    loading.value = false;
  }
};

// 处理搜索
const handleSearch = () => {
  pagination.page = 1;
  loadEventList();
};

// 处理重置
const handleReset = () => {
  filters.deviceCode = '';
  filters.riskLevel = '';
  filters.status = '';
  pagination.page = 1;
  loadEventList();
};

// 处理大小变化
const handleSizeChange = () => {
  loadEventList();
};

// 处理页码变化
const handlePageChange = () => {
  loadEventList();
};

// 查看详情
const handleViewDetail = async (row: EventRecord) => {
  loading.value = true;
  try {
    const res = await getEventDetail(row.id);
    if (res.code === 200) {
      detail.value = res.data;
      detailVisible.value = true;
    }
  } catch (error) {
    console.error('查询事件详情失败', error);
  } finally {
    loading.value = false;
  }
};

// 工单派发
const handleDispatch = (row: EventRecord) => {
  detail.value = row;
  dispatchVisible.value = true;
};

// 确认派发
const handleDispatchConfirm = async () => {
  if (!detail.value) return;
  try {
    const res = await dispatchEvent(detail.value.id, 1, dispatchForm.receiveUser);
    if (res.code === 200) {
      ElMessage.success('派发成功');
      dispatchVisible.value = false;
      loadEventList();
    }
  } catch (error) {
    console.error('派发事件失败', error);
  }
};

// 关闭事件
const handleClose = (row: EventRecord) => {
  detail.value = row;
  closeVisible.value = true;
};

// 确认关闭
const handleCloseConfirm = async () => {
  if (!detail.value) return;
  try {
    const res = await closeEvent(detail.value.id, 1, closeForm.closeReason);
    if (res.code === 200) {
      ElMessage.success('关闭成功');
      closeVisible.value = false;
      loadEventList();
    }
  } catch (error) {
    console.error('关闭事件失败', error);
  }
};

// 初始�?
onMounted(() => {
  loadEventList();
});
</script>

<style scoped>
.event-disposal-view {
  padding: 20px;
}

.event-header {
  margin-bottom: 20px;
}

.event-header h1 {
  font-size: 24px;
  margin-bottom: 20px;
}

.event-stats {
  display: flex;
  gap: 20px;
}

.event-filters {
  margin-bottom: 20px;
  padding: 15px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.event-list {
  margin-bottom: 20px;
}

.event-pagination {
  display: flex;
  justify-content: flex-end;
}
</style>

