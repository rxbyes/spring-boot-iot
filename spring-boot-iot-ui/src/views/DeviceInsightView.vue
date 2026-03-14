<template>
  <div class="page-stack">
    <section class="hero-grid">
      <PanelCard
        eyebrow="Device Insight"
        title="统一回看设备状态、属性和消息日志"
        description="这是未来图表、告警和数字孪生最直接的数据入口，当前聚焦 Phase 1 已落地能力。"
      >
        <form @submit.prevent="refreshAll">
          <div class="form-grid">
            <div class="field-group">
              <label for="insight-device-code">设备编码</label>
              <el-input
                id="insight-device-code"
                v-model="deviceCode"
                name="insight_device_code"
                placeholder="例如 demo-device-01..."
                clearable
              />
            </div>
          </div>
          <div class="button-row" style="margin-top: 1rem;">
            <el-button class="primary-button" type="primary" native-type="submit" :loading="isLoading">
              {{ isLoading ? '加载中...' : '刷新设备洞察' }}
            </el-button>
          </div>
        </form>

        <el-descriptions v-if="device" :column="2" border style="margin-top: 1rem;">
          <el-descriptions-item label="设备名称">{{ device.deviceName }}</el-descriptions-item>
          <el-descriptions-item label="在线状态">
            <el-tag :type="device.onlineStatus === 1 ? 'success' : 'info'">
              {{ statusLabel(device.onlineStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="最近在线时间">{{ formatDateTime(device.lastOnlineTime) }}</el-descriptions-item>
          <el-descriptions-item label="最近上报时间">{{ formatDateTime(device.lastReportTime) }}</el-descriptions-item>
          <el-descriptions-item label="固件版本">{{ device.firmwareVersion || '--' }}</el-descriptions-item>
          <el-descriptions-item label="部署位置">{{ device.address || '--' }}</el-descriptions-item>
        </el-descriptions>
      </PanelCard>

      <PanelCard
        eyebrow="Insight Summary"
        title="属性与日志概况"
        description="这里先聚合 Phase 1 的静态查询，后续可在同区域接折线图、卡片图层和 3D 设备实体。"
      >
        <div class="quad-grid">
          <div class="info-chip">
            <span>属性数量</span>
            <strong>{{ properties.length }}</strong>
          </div>
          <div class="info-chip">
            <span>日志数量</span>
            <strong>{{ logs.length }}</strong>
          </div>
          <div class="info-chip">
            <span>最近 messageType</span>
            <strong>{{ logs[0]?.messageType || '--' }}</strong>
          </div>
          <div class="info-chip">
            <span>最近抓取时间</span>
            <strong>{{ formatDateTime(lastFetchTime) }}</strong>
          </div>
        </div>
      </PanelCard>
    </section>

    <div v-if="errorMessage" class="empty-state" aria-live="polite">{{ errorMessage }}</div>

    <PropertyTrendPanel :logs="logs" />

    <section class="two-column-grid">
      <PanelCard
        eyebrow="Latest Properties"
        title="设备属性快照"
        description="来自 `GET /device/{deviceCode}/properties`，是图表与设备卡片最直接的渲染数据源。"
      >
        <el-table v-if="properties.length" :data="properties" stripe>
          <el-table-column prop="identifier" label="标识符" min-width="140" />
          <el-table-column prop="propertyName" label="属性名" min-width="140">
            <template #default="{ row }">{{ row.propertyName || '--' }}</template>
          </el-table-column>
          <el-table-column prop="propertyValue" label="值" min-width="120">
            <template #default="{ row }">{{ row.propertyValue || '--' }}</template>
          </el-table-column>
          <el-table-column prop="valueType" label="类型" min-width="100">
            <template #default="{ row }">{{ row.valueType || '--' }}</template>
          </el-table-column>
          <el-table-column label="更新时间" min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.updateTime || row.reportTime) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="还没有属性数据。先去“HTTP 上报实验台”发送一条属性报文。" />
      </PanelCard>

      <PanelCard
        eyebrow="Message Logs"
        title="消息日志"
        description="来自 `GET /device/{deviceCode}/message-logs`，便于审计 topic 与原始 payload。"
      >
        <div v-if="logs.length" class="timeline">
          <article v-for="item in logs" :key="item.id" class="timeline-item">
            <h3>{{ item.messageType || 'unknown' }}</h3>
            <p>{{ item.topic || '--' }}</p>
            <p>{{ truncateText(item.payload || '--', 140) }}</p>
            <p>{{ formatDateTime(item.reportTime || item.createTime) }}</p>
          </article>
        </div>
        <el-empty v-else description="还没有日志数据。发送报文后再回来刷新即可。" />
      </PanelCard>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';

import { getDeviceByCode, getDeviceMessageLogs, getDeviceProperties } from '../api/iot';
import PanelCard from '../components/PanelCard.vue';
import PropertyTrendPanel from '../components/PropertyTrendPanel.vue';
import { recordActivity } from '../stores/activity';
import type { Device, DeviceMessageLog, DeviceProperty } from '../types/api';
import { formatDateTime, statusLabel, truncateText } from '../utils/format';

const route = useRoute();
const router = useRouter();
const deviceCode = ref(typeof route.query.deviceCode === 'string' ? route.query.deviceCode : 'demo-device-01');
const isLoading = ref(false);
const errorMessage = ref('');
const lastFetchTime = ref<string | null>(null);
const device = ref<Device | null>(null);
const properties = ref<DeviceProperty[]>([]);
const logs = ref<DeviceMessageLog[]>([]);

watch(deviceCode, (value) => {
  router.replace({
    query: {
      ...route.query,
      deviceCode: value
    }
  });
});

watch(
  () => route.query.deviceCode,
  (value) => {
    if (typeof value === 'string' && value !== deviceCode.value) {
      deviceCode.value = value;
    }
  }
);

onMounted(() => {
  refreshAll();
});

async function refreshAll() {
  isLoading.value = true;
  errorMessage.value = '';

  try {
    const [deviceResponse, propertyResponse, logResponse] = await Promise.all([
      getDeviceByCode(deviceCode.value),
      getDeviceProperties(deviceCode.value),
      getDeviceMessageLogs(deviceCode.value)
    ]);

    device.value = deviceResponse.data;
    properties.value = propertyResponse.data;
    logs.value = logResponse.data;
    lastFetchTime.value = new Date().toISOString();
    ElMessage.success(`设备 ${deviceCode.value} 洞察刷新成功`);

    recordActivity({
      module: '设备洞察',
      action: '刷新洞察',
      request: { deviceCode: deviceCode.value },
      response: {
        device: deviceResponse.data,
        properties: propertyResponse.data.length,
        logs: logResponse.data.length
      },
      ok: true,
      detail: `设备 ${deviceCode.value} 刷新完成，属性 ${propertyResponse.data.length} 条，日志 ${logResponse.data.length} 条`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: '设备洞察',
      action: '刷新洞察',
      request: { deviceCode: deviceCode.value },
      response: { message: errorMessage.value },
      ok: false,
      detail: `刷新失败：${errorMessage.value}`
    });
  } finally {
    isLoading.value = false;
  }
}
</script>
