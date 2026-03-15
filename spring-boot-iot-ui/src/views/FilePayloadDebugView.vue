<template>
  <div class="page-stack">
    <section class="hero-grid">
      <PanelCard
        eyebrow="File Payload Debug"
        title="文件与固件调试"
      >
        <form @submit.prevent="refreshAll">
          <div class="form-grid">
            <div class="field-group">
              <label for="file-debug-device-code">设备编码</label>
              <el-input id="file-debug-device-code" v-model="deviceCode" clearable placeholder="例如 demo-device-01..." />
            </div>
          </div>
          <div class="button-row" style="margin-top: 1rem;">
            <el-button class="primary-button" type="primary" native-type="submit" :loading="isLoading">
              {{ isLoading ? '加载中...' : '刷新数据' }}
            </el-button>
          </div>
        </form>
      </PanelCard>

      <PanelCard
        eyebrow="Debug Summary"
        title="文件消息消费概况"
      >
        <div class="quad-grid">
          <div class="info-chip">
            <span>文件快照数量</span>
            <strong>{{ fileSnapshots.length }}</strong>
          </div>
          <div class="info-chip">
            <span>固件聚合数量</span>
            <strong>{{ firmwareAggregates.length }}</strong>
          </div>
          <div class="info-chip">
            <span>最近文件类型</span>
            <strong>{{ fileSnapshots[0]?.fileType || firmwareAggregates[0]?.fileType || '--' }}</strong>
          </div>
          <div class="info-chip">
            <span>最近抓取时间</span>
            <strong>{{ formatDateTime(lastFetchTime) }}</strong>
          </div>
        </div>
      </PanelCard>
    </section>

    <div v-if="errorMessage" class="empty-state" aria-live="polite">{{ errorMessage }}</div>

    <section class="two-column-grid">
      <PanelCard
        eyebrow="Type C.3"
        title="文件快照"
      >
        <div v-if="fileSnapshots.length" class="timeline">
          <article v-for="item in fileSnapshots" :key="item.transferId" class="timeline-item">
            <h3>{{ item.dataSetId || item.transferId }}</h3>
            <p>类型：{{ item.fileType || '--' }} / 长度：{{ item.binaryLength ?? '--' }}</p>
            <p>更新时间：{{ formatDateTime(item.updatedTime || item.timestamp) }}</p>
          </article>
        </div>
      </PanelCard>

      <PanelCard
        eyebrow="Type C.4"
        title="固件聚合"
      >
        <div v-if="firmwareAggregates.length" class="timeline">
          <article v-for="item in firmwareAggregates" :key="item.transferId" class="timeline-item">
            <h3>{{ item.dataSetId || item.transferId }}</h3>
            <p>
              分包：{{ item.receivedPacketCount ?? 0 }} / {{ item.totalPackets ?? '--' }}
              <span v-if="item.receivedPacketIndexes?.length">（{{ item.receivedPacketIndexes.join(', ') }}）</span>
            </p>
            <p>聚合状态：{{ item.completed ? '已完成' : '进行中' }}</p>
            <p>MD5 校验：{{ formatMd5(item.md5Matched) }}</p>
            <p>更新时间：{{ formatDateTime(item.updatedTime || item.timestamp) }}</p>
          </article>
        </div>
      </PanelCard>
    </section>

    <section class="two-column-grid">
      <ResponsePanel
        eyebrow="Snapshot JSON"
        title="文件快照原始响应"
        :body="fileSnapshots"
      />
      <ResponsePanel
        eyebrow="Aggregate JSON"
        title="固件聚合原始响应"
        :body="firmwareAggregates"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

import { getDeviceFileSnapshots, getDeviceFirmwareAggregates } from '../api/iot';
import PanelCard from '../components/PanelCard.vue';
import ResponsePanel from '../components/ResponsePanel.vue';
import { recordActivity } from '../stores/activity';
import type { DeviceFileSnapshot, DeviceFirmwareAggregate } from '../types/api';
import { formatDateTime } from '../utils/format';

const deviceCode = ref('demo-device-01');
const isLoading = ref(false);
const errorMessage = ref('');
const lastFetchTime = ref<string | null>(null);
const fileSnapshots = ref<DeviceFileSnapshot[]>([]);
const firmwareAggregates = ref<DeviceFirmwareAggregate[]>([]);

function formatMd5(value?: boolean | null) {
  if (value === true) {
    return '匹配';
  }
  if (value === false) {
    return '不匹配';
  }
  return '--';
}

async function refreshAll() {
  isLoading.value = true;
  errorMessage.value = '';

  try {
    const [snapshotResponse, firmwareResponse] = await Promise.all([
      getDeviceFileSnapshots(deviceCode.value),
      getDeviceFirmwareAggregates(deviceCode.value)
    ]);

    fileSnapshots.value = snapshotResponse.data;
    firmwareAggregates.value = firmwareResponse.data;
    lastFetchTime.value = new Date().toISOString();

    recordActivity({
      module: '文件调试台',
      action: '刷新文件调试数据',
      request: { deviceCode: deviceCode.value },
      response: {
        fileSnapshots: snapshotResponse.data.length,
        firmwareAggregates: firmwareResponse.data.length
      },
      ok: true,
      detail: `设备 ${deviceCode.value} 文件快照 ${snapshotResponse.data.length} 条，固件聚合 ${firmwareResponse.data.length} 条`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    recordActivity({
      module: '文件调试台',
      action: '刷新文件调试数据',
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
