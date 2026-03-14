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
              <input id="insight-device-code" v-model="deviceCode" autocomplete="off" required />
            </div>
          </div>
          <div class="button-row" style="margin-top: 1rem;">
            <button class="primary-button" type="submit" :disabled="isLoading">
              {{ isLoading ? '加载中...' : '刷新设备洞察' }}
            </button>
          </div>
        </form>

        <div v-if="device" class="info-grid" style="margin-top: 1rem;">
          <div class="info-chip">
            <span>设备名称</span>
            <strong>{{ device.deviceName }}</strong>
          </div>
          <div class="info-chip">
            <span>在线状态</span>
            <strong>{{ statusLabel(device.onlineStatus) }}</strong>
          </div>
          <div class="info-chip">
            <span>最近在线时间</span>
            <strong>{{ formatDateTime(device.lastOnlineTime) }}</strong>
          </div>
          <div class="info-chip">
            <span>最近上报时间</span>
            <strong>{{ formatDateTime(device.lastReportTime) }}</strong>
          </div>
          <div class="info-chip">
            <span>固件版本</span>
            <strong>{{ device.firmwareVersion || '--' }}</strong>
          </div>
          <div class="info-chip">
            <span>部署位置</span>
            <strong>{{ device.address || '--' }}</strong>
          </div>
        </div>
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

    <div v-if="errorMessage" class="empty-state">{{ errorMessage }}</div>

    <section class="two-column-grid">
      <PanelCard
        eyebrow="Latest Properties"
        title="设备属性快照"
        description="来自 `GET /device/{deviceCode}/properties`，是图表与设备卡片最直接的渲染数据源。"
      >
        <div v-if="properties.length" class="table-shell">
          <table>
            <thead>
              <tr>
                <th>标识符</th>
                <th>属性名</th>
                <th>值</th>
                <th>类型</th>
                <th>更新时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in properties" :key="item.id">
                <td>{{ item.identifier }}</td>
                <td>{{ item.propertyName || '--' }}</td>
                <td>{{ item.propertyValue || '--' }}</td>
                <td>{{ item.valueType || '--' }}</td>
                <td>{{ formatDateTime(item.updateTime || item.reportTime) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty-state">还没有属性数据。先去“HTTP 上报实验台”发送一条属性报文。</div>
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
        <div v-else class="empty-state">还没有日志数据。发送报文后再回来刷新即可。</div>
      </PanelCard>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

import { getDeviceByCode, getDeviceMessageLogs, getDeviceProperties } from '../api/iot';
import PanelCard from '../components/PanelCard.vue';
import { recordActivity } from '../stores/activity';
import type { Device, DeviceMessageLog, DeviceProperty } from '../types/api';
import { formatDateTime, statusLabel, truncateText } from '../utils/format';

const deviceCode = ref('demo-device-01');
const isLoading = ref(false);
const errorMessage = ref('');
const lastFetchTime = ref<string | null>(null);
const device = ref<Device | null>(null);
const properties = ref<DeviceProperty[]>([]);
const logs = ref<DeviceMessageLog[]>([]);

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
