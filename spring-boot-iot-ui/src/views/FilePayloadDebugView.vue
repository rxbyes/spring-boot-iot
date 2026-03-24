<template>
  <div class="file-payload-debug-view">
    <StandardWorkbenchPanel
      title="数据校验台"
      description="按设备编码查看文件快照、固件聚合结果和原始响应，统一完成接入数据完整性核查。"
      show-filters
      :show-inline-state="showInlineState"
    >
      <template #filters>
        <StandardListFilterHeader :model="{ deviceCode }">
          <template #primary>
            <el-form-item>
              <el-input
                id="file-debug-device-code"
                v-model="deviceCode"
                clearable
                placeholder="设备编码，例如 demo-device-01"
                prefix-icon="Search"
                @keyup.enter="refreshAll"
              />
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="query" :loading="isLoading" :disabled="!normalizedDeviceCode" @click="refreshAll">
              {{ isLoading ? '加载中...' : '刷新数据' }}
            </StandardButton>
            <StandardButton action="reset" :disabled="isLoading" @click="handleReset">重置</StandardButton>
          </template>
        </StandardListFilterHeader>
      </template>

      <template #inline-state>
        <StandardInlineState :message="inlineStateMessage" :tone="inlineStateTone" />
      </template>

      <div class="page-stack">
        <PanelCard
          eyebrow="数据校验台"
          title="文件消息完整性概况"
          description="统一汇总文件快照数量、固件聚合数量、最近文件类型和最近一次抓取时间。"
        >
          <StandardInfoGrid :items="validationSummaryItems" :columns="4" />
        </PanelCard>

        <section class="two-column-grid file-payload-debug-view__results">
          <PanelCard
            eyebrow="文件快照 C.3"
            title="文件快照校验"
            description="按时间线核对最近一次 C.3 文件消息是否完整落地。"
          >
            <div v-if="fileSnapshots.length" class="timeline">
              <article v-for="item in fileSnapshots" :key="item.transferId" class="timeline-item">
                <h3>{{ item.dataSetId || item.transferId }}</h3>
                <p>类型：{{ item.fileType || '--' }} / 长度：{{ item.binaryLength ?? '--' }}</p>
                <p>更新时间：{{ formatDateTime(item.updatedTime || item.timestamp) }}</p>
              </article>
            </div>
            <EmptyState
              v-else
              title="暂无文件快照"
              description="当前设备还没有返回 C.3 文件快照，可刷新后继续核查。"
            />
          </PanelCard>

          <PanelCard
            eyebrow="固件聚合 C.4"
            title="固件聚合校验"
            description="按分包进度、完成状态和 MD5 校验结果查看当前聚合情况。"
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
            <EmptyState
              v-else
              title="暂无固件聚合记录"
              description="当前设备还没有返回 C.4 聚合结果，可刷新后继续核查。"
            />
          </PanelCard>
        </section>

        <section class="two-column-grid">
          <ResponsePanel
            eyebrow="文件快照响应"
            title="文件快照原始响应"
            :body="fileSnapshots"
          />
          <ResponsePanel
            eyebrow="固件聚合响应"
            title="固件聚合原始响应"
            :body="firmwareAggregates"
          />
        </section>
      </div>
    </StandardWorkbenchPanel>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';

import { getDeviceFileSnapshots, getDeviceFirmwareAggregates } from '../api/iot';
import EmptyState from '../components/EmptyState.vue';
import PanelCard from '../components/PanelCard.vue';
import ResponsePanel from '../components/ResponsePanel.vue';
import StandardInfoGrid from '../components/StandardInfoGrid.vue';
import StandardInlineState from '../components/StandardInlineState.vue';
import StandardListFilterHeader from '../components/StandardListFilterHeader.vue';
import StandardWorkbenchPanel from '../components/StandardWorkbenchPanel.vue';
import { recordActivity } from '../stores/activity';
import type { DeviceFileSnapshot, DeviceFirmwareAggregate } from '../types/api';
import { formatDateTime } from '../utils/format';

const defaultDeviceCode = 'demo-device-01';
const deviceCode = ref(defaultDeviceCode);
const isLoading = ref(false);
const errorMessage = ref('');
const lastFetchTime = ref<string | null>(null);
const fileSnapshots = ref<DeviceFileSnapshot[]>([]);
const firmwareAggregates = ref<DeviceFirmwareAggregate[]>([]);
const normalizedDeviceCode = computed(() => deviceCode.value.trim());
const inlineStateMessage = computed(() => {
  if (errorMessage.value) {
    return errorMessage.value;
  }
  if (lastFetchTime.value) {
    return `最近一次抓取：${formatDateTime(lastFetchTime.value)}，文件快照 ${fileSnapshots.value.length} 条，固件聚合 ${firmwareAggregates.value.length} 条。`;
  }
  return '';
});
const inlineStateTone = computed<'info' | 'error'>(() => (errorMessage.value ? 'error' : 'info'));
const showInlineState = computed(() => Boolean(inlineStateMessage.value));

const validationSummaryItems = computed(() => [
  {
    key: 'file-snapshot-count',
    label: '文件快照数量',
    value: fileSnapshots.value.length
  },
  {
    key: 'firmware-aggregate-count',
    label: '固件聚合数量',
    value: firmwareAggregates.value.length
  },
  {
    key: 'latest-file-type',
    label: '最近文件类型',
    value: fileSnapshots.value[0]?.fileType || firmwareAggregates.value[0]?.fileType
  },
  {
    key: 'last-fetch-time',
    label: '最近抓取时间',
    value: formatDateTime(lastFetchTime.value)
  }
]);

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
  if (!normalizedDeviceCode.value) {
    errorMessage.value = '请输入设备编码后再刷新。';
    return;
  }

  isLoading.value = true;
  errorMessage.value = '';

  try {
    const [snapshotResponse, firmwareResponse] = await Promise.all([
      getDeviceFileSnapshots(normalizedDeviceCode.value),
      getDeviceFirmwareAggregates(normalizedDeviceCode.value)
    ]);

    fileSnapshots.value = snapshotResponse.data;
    firmwareAggregates.value = firmwareResponse.data;
    lastFetchTime.value = new Date().toISOString();

    recordActivity({
      module: '数据校验台',
      action: '刷新校验数据',
      request: { deviceCode: normalizedDeviceCode.value },
      response: {
        fileSnapshots: snapshotResponse.data.length,
        firmwareAggregates: firmwareResponse.data.length
      },
      ok: true,
      detail: `设备 ${normalizedDeviceCode.value} 文件快照 ${snapshotResponse.data.length} 条，固件聚合 ${firmwareResponse.data.length} 条`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    recordActivity({
      module: '数据校验台',
      action: '刷新校验数据',
      request: { deviceCode: normalizedDeviceCode.value },
      response: { message: errorMessage.value },
      ok: false,
      detail: `刷新失败：${errorMessage.value}`
    });
  } finally {
    isLoading.value = false;
  }
}

function handleReset() {
  deviceCode.value = defaultDeviceCode;
  errorMessage.value = '';
  lastFetchTime.value = null;
  fileSnapshots.value = [];
  firmwareAggregates.value = [];
}
</script>

<style scoped>
.file-payload-debug-view {
  min-width: 0;
}

.file-payload-debug-view__results {
  align-items: start;
}
</style>
