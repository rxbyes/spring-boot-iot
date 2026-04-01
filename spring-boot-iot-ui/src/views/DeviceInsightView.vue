<template>
  <StandardPageShell class="page-stack device-insight-view" :show-title="false">
    <StandardWorkbenchPanel
      title="对象洞察台"
      description="按设备编码或监测绑定查看当前风险等级、监测对象档案与趋势线索。"
      show-filters
      :show-inline-state="showInlineState"
    >
      <template #filters>
        <StandardListFilterHeader :model="{ deviceCode }">
          <template #primary>
            <el-form-item>
              <el-input
                id="insight-device-code"
                v-model="deviceCode"
                name="insight_device_code"
                placeholder="设备编码，例如 demo-device-01"
                clearable
                prefix-icon="Search"
                @keyup.enter="refreshAll"
              />
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="query" :loading="isLoading" @click="refreshAll">
              {{ isLoading ? '刷新中...' : '刷新洞察' }}
            </StandardButton>
            <StandardButton action="reset" :disabled="isLoading" @click="handleReset">重置</StandardButton>
          </template>
        </StandardListFilterHeader>
      </template>

      <template #inline-state>
        <StandardInlineState :message="inlineStateMessage" :tone="inlineStateTone" />
      </template>

      <div class="page-stack">
        <section v-if="showBindingSwitcher" class="insight-binding-switcher">
          <div>
            <strong>监测对象切换</strong>
            <p>当前设备命中多个风险监测绑定时，可直接切换查看不同风险点和测点口径。</p>
          </div>
          <el-segmented
            v-model="selectedBindingId"
            :options="bindingSegmentOptions"
            @change="handleBindingChange"
          />
        </section>

        <section class="quad-grid">
          <MetricCard
            v-for="metric in overviewMetrics"
            :key="metric.label"
            :label="metric.label"
            :value="metric.value"
            :hint="metric.hint"
            :badge="metric.badge"
          />
        </section>

        <div v-if="errorMessage" class="empty-state" aria-live="polite">{{ errorMessage }}</div>

        <section class="two-column-grid">
          <PanelCard
            title="基础档案"
            description="优先展示风险点、测点、区域和设备档案，让风险运营口径与设备补充信息保持同页对齐。"
          >
            <el-descriptions v-if="riskDetail || device" :column="2" border class="descriptions-block">
              <el-descriptions-item label="风险点">{{ riskDetail?.riskPointName || '未绑定' }}</el-descriptions-item>
              <el-descriptions-item label="风险点编码">{{ riskDetail?.riskPointCode || '--' }}</el-descriptions-item>
              <el-descriptions-item label="风险等级">{{ riskLevelLabel }}</el-descriptions-item>
              <el-descriptions-item label="监测状态">{{ monitorStatusLabel }}</el-descriptions-item>
              <el-descriptions-item label="设备名称">{{ riskDetail?.deviceName || device?.deviceName || '--' }}</el-descriptions-item>
              <el-descriptions-item label="设备编码">{{ riskDetail?.deviceCode || device?.deviceCode || '--' }}</el-descriptions-item>
              <el-descriptions-item label="产品名称">{{ riskDetail?.productName || device?.productName || '--' }}</el-descriptions-item>
              <el-descriptions-item label="测点">{{ riskDetail?.metricName || riskDetail?.metricIdentifier || '未绑定' }}</el-descriptions-item>
              <el-descriptions-item label="区域">{{ riskDetail?.regionName || '--' }}</el-descriptions-item>
              <el-descriptions-item label="部署位置">{{ riskDetail?.address || device?.address || '--' }}</el-descriptions-item>
              <el-descriptions-item label="接入协议">{{ device?.protocolCode || '--' }}</el-descriptions-item>
              <el-descriptions-item label="固件版本">{{ device?.firmwareVersion || '--' }}</el-descriptions-item>
              <el-descriptions-item label="最近在线时间">{{ formatDateTime(device?.lastOnlineTime) }}</el-descriptions-item>
              <el-descriptions-item label="最近上报时间">{{ formatDateTime(riskDetail?.latestReportTime || device?.lastReportTime) }}</el-descriptions-item>
            </el-descriptions>
            <div v-else class="empty-state">请输入设备编码并刷新，查看当前监测对象档案。</div>
          </PanelCard>

          <PanelCard
            title="研判依据"
            description="统一使用风险监测详情和设备补充数据，给出可复述的判断依据。"
          >
            <div v-if="reasons.length" class="reason-list">
              <article v-for="reason in reasons" :key="`${reason.tag}-${reason.title}`" class="reason-list__item">
                <header>
                  <strong>{{ reason.title }}</strong>
                  <span>{{ reason.tag }}</span>
                </header>
                <p>{{ reason.description }}</p>
              </article>
            </div>
            <div v-else class="empty-state">加载风险监测详情后，这里会展示当前对象的研判依据。</div>
          </PanelCard>
        </section>

        <RiskInsightTrendPanel :detail="riskDetail" :logs="logs" :object-type="objectType" />

        <section class="two-column-grid">
          <PanelCard
            title="关键监测指标"
            description="优先展示当前绑定测点和可用于现场复核的最新设备属性。"
          >
            <div v-if="keyMetrics.length" class="highlight-grid">
              <article v-for="item in keyMetrics" :key="item.identifier" class="highlight-card">
                <span>{{ item.identifier }}</span>
                <strong>{{ item.value }}</strong>
                <small>{{ item.type }}</small>
                <p>{{ item.time }}</p>
              </article>
            </div>
            <el-empty v-else description="当前没有可用于展示的关键监测指标。" />
          </PanelCard>

          <PanelCard
            title="风险分析草稿"
            description="提供一份可直接复述的运营分析草稿，便于继续扩展成正式汇报。"
          >
            <div class="report-draft">
              <p><strong>对象：</strong>{{ riskDetail?.riskPointName || riskDetail?.deviceName || device?.deviceName || normalizedDeviceCode || '--' }}</p>
              <p><strong>当前等级：</strong>{{ riskLevelLabel }}</p>
              <p><strong>判定摘要：</strong>{{ draft.summary }}</p>
              <p><strong>建议动作：</strong>{{ draft.actions }}</p>
              <p><strong>后续关注：</strong>{{ draft.followUp }}</p>
            </div>
          </PanelCard>
        </section>

        <PanelCard
          title="设备属性快照"
          description="来自设备侧属性快照接口，用于补充核查当前监测对象关联设备的最新状态。"
        >
          <el-table v-if="properties.length" :data="properties" stripe>
            <StandardTableTextColumn prop="identifier" label="标识符" :min-width="140" />
            <StandardTableTextColumn prop="propertyName" label="属性名" :min-width="140">
              <template #default="{ row }">{{ row.propertyName || '--' }}</template>
            </StandardTableTextColumn>
            <StandardTableTextColumn prop="propertyValue" label="值" :min-width="140">
              <template #default="{ row }">{{ row.propertyValue || '--' }}</template>
            </StandardTableTextColumn>
            <StandardTableTextColumn prop="valueType" label="类型" :min-width="120">
              <template #default="{ row }">{{ row.valueType || '--' }}</template>
            </StandardTableTextColumn>
            <StandardTableTextColumn label="更新时间" :min-width="180">
              <template #default="{ row }">{{ formatDateTime(row.updateTime || row.reportTime) }}</template>
            </StandardTableTextColumn>
          </el-table>
          <el-empty v-else description="还没有属性数据。先通过 HTTP 或 MQTT 发送一条上报。" />
        </PanelCard>
      </div>
    </StandardWorkbenchPanel>
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';

import { getRiskMonitoringDetail, getRiskMonitoringList, type RiskMonitoringDetail, type RiskMonitoringListItem } from '@/api/riskMonitoring';
import { getDeviceByCode, getDeviceMessageLogs, getDeviceProperties } from '../api/iot';
import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import RiskInsightTrendPanel from '../components/RiskInsightTrendPanel.vue';
import StandardInlineState from '../components/StandardInlineState.vue';
import StandardListFilterHeader from '../components/StandardListFilterHeader.vue';
import StandardPageShell from '../components/StandardPageShell.vue';
import StandardTableTextColumn from '../components/StandardTableTextColumn.vue';
import StandardWorkbenchPanel from '../components/StandardWorkbenchPanel.vue';
import { recordActivity } from '../stores/activity';
import type { Device, DeviceMessageLog, DeviceProperty } from '../types/api';
import { formatDateTime } from '../utils/format';
import {
  buildInsightDraft,
  buildInsightReasons,
  getInsightObjectTypeLabel,
  getRiskLevelLabel,
  pickPrimaryBinding,
  resolveInsightObjectType
} from '../utils/deviceInsight';

function parseBindingId(value: unknown) {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }

  return null;
}

const route = useRoute();
const router = useRouter();
const initialDeviceCode = ref(typeof route.query.deviceCode === 'string' && route.query.deviceCode.trim()
  ? route.query.deviceCode.trim()
  : 'demo-device-01');
const initialBindingId = ref(parseBindingId(route.query.bindingId));
const deviceCode = ref(initialDeviceCode.value);
const selectedBindingId = ref<number | null>(initialBindingId.value);
const isLoading = ref(false);
const errorMessage = ref('');
const fallbackMessage = ref('');
const lastFetchTime = ref<string | null>(null);
const device = ref<Device | null>(null);
const properties = ref<DeviceProperty[]>([]);
const logs = ref<DeviceMessageLog[]>([]);
const riskDetail = ref<RiskMonitoringDetail | null>(null);
const bindingOptions = ref<RiskMonitoringListItem[]>([]);
const normalizedDeviceCode = computed(() => deviceCode.value.trim());
let syncingRoute = false;

watch(
  () => [route.query.deviceCode, route.query.bindingId] as const,
  ([deviceCodeQuery, bindingIdQuery]) => {
    if (syncingRoute) {
      return;
    }

    const nextDeviceCode = typeof deviceCodeQuery === 'string' && deviceCodeQuery.trim()
      ? deviceCodeQuery.trim()
      : 'demo-device-01';
    const nextBindingId = parseBindingId(bindingIdQuery);

    if (nextDeviceCode === deviceCode.value && nextBindingId === selectedBindingId.value) {
      return;
    }

    initialDeviceCode.value = nextDeviceCode;
    initialBindingId.value = nextBindingId;
    deviceCode.value = nextDeviceCode;
    selectedBindingId.value = nextBindingId;
    void refreshAll();
  }
);

onMounted(() => {
  void refreshAll();
});

const inlineStateMessage = computed(() => {
  if (errorMessage.value) {
    return errorMessage.value;
  }

  if (!normalizedDeviceCode.value && !selectedBindingId.value) {
    return '请输入设备编码或传入监测绑定后再刷新对象洞察。';
  }

  if (!lastFetchTime.value) {
    return `设备 ${normalizedDeviceCode.value || '--'} · 等待刷新风险监测对象。`;
  }

  if (fallbackMessage.value) {
    return `${device.value?.deviceName || normalizedDeviceCode.value} · ${fallbackMessage.value} · 属性 ${properties.value.length} 条 · 日志 ${logs.value.length} 条 · 最近刷新 ${formatDateTime(lastFetchTime.value)}`;
  }

  return `对象 ${riskDetail.value?.riskPointName || riskDetail.value?.deviceName || normalizedDeviceCode.value} · ${riskLevelLabel.value} · 属性 ${properties.value.length} 条 · 日志 ${logs.value.length} 条 · 最近刷新 ${formatDateTime(lastFetchTime.value)}`;
});
const inlineStateTone = computed<'info' | 'error'>(() => (errorMessage.value ? 'error' : 'info'));
const showInlineState = computed(() => Boolean(inlineStateMessage.value));

const showBindingSwitcher = computed(() => bindingOptions.value.length > 1);
const bindingSegmentOptions = computed(() =>
  bindingOptions.value.map((item) => ({
    label: item.riskPointName || item.metricName || `绑定 ${item.bindingId}`,
    value: Number(item.bindingId)
  }))
);

const objectType = computed(() => resolveInsightObjectType(riskDetail.value ?? {}));
const objectTypeLabel = computed(() => getInsightObjectTypeLabel(objectType.value));
const riskLevelLabel = computed(() => getRiskLevelLabel(riskDetail.value?.riskLevel));
const monitorStatusLabel = computed(() => {
  if (!riskDetail.value) {
    return device.value ? '未绑定' : '未标注';
  }

  switch ((riskDetail.value?.monitorStatus || '').toUpperCase()) {
    case 'ALARM':
      return '告警中';
    case 'OFFLINE':
      return '离线';
    case 'NO_DATA':
      return '无数据';
    case 'NORMAL':
      return '正常';
    default:
      return riskDetail.value?.monitorStatus || '未标注';
  }
});

const reasons = computed(() => buildInsightReasons(riskDetail.value, properties.value, logs.value, device.value));
const draft = computed(() => buildInsightDraft(riskDetail.value, reasons.value, device.value));

const overviewMetrics = computed(() => [
  {
    label: '当前风险等级',
    value: riskLevelLabel.value,
    hint: riskDetail.value?.riskPointName ? `风险点：${riskDetail.value.riskPointName}` : '当前未加载风险监测对象。',
    badge: {
      label: riskLevelLabel.value.replace('风险', ''),
      tone: resolveRiskBadgeTone(riskDetail.value?.riskLevel)
    }
  },
  {
    label: '在线状态',
    value: (riskDetail.value?.onlineStatus ?? device.value?.onlineStatus) === 1
      ? '在线'
      : (riskDetail.value || device.value) ? '离线' : '--',
    hint: riskDetail.value?.latestReportTime || device.value?.lastReportTime
      ? `最新上报：${formatDateTime(riskDetail.value?.latestReportTime || device.value?.lastReportTime)}`
      : '当前暂无最近上报时间。',
    badge: {
      label: (riskDetail.value?.onlineStatus ?? device.value?.onlineStatus) === 1 ? '在线' : '离线',
      tone: (riskDetail.value?.onlineStatus ?? device.value?.onlineStatus) === 1 ? 'success' : 'muted'
    }
  },
  {
    label: '属性快照数',
    value: String(properties.value.length),
    hint: properties.value.length ? '当前已具备设备属性快照，可用于补充核查。' : '当前没有设备属性快照。',
    badge: { label: 'Property', tone: 'brand' }
  },
  {
    label: '消息日志数',
    value: String(logs.value.length),
    hint: logs.value.length ? '当前可回看最近 topic 与 payload。' : '当前没有可用于回看的消息日志。',
    badge: { label: 'Audit', tone: 'success' }
  }
]);

const keyMetrics = computed(() => {
  const metrics: Array<{ identifier: string; value: string; type: string; time: string }> = [];

  if (riskDetail.value) {
    metrics.push({
      identifier: riskDetail.value.metricName || riskDetail.value.metricIdentifier || '当前测点',
      value: riskDetail.value.unit ? `${riskDetail.value.currentValue || '--'} ${riskDetail.value.unit}` : riskDetail.value.currentValue || '--',
      type: objectTypeLabel.value,
      time: formatDateTime(riskDetail.value.latestReportTime)
    });
  }

  for (const item of properties.value) {
    const duplicateMetric = riskDetail.value?.metricIdentifier && item.identifier === riskDetail.value.metricIdentifier;
    if (duplicateMetric) {
      continue;
    }

    metrics.push({
      identifier: item.propertyName || item.identifier,
      value: item.propertyValue || '--',
      type: item.valueType || '--',
      time: formatDateTime(item.updateTime || item.reportTime)
    });
  }

  return metrics.slice(0, 6);
});

function resolveRiskBadgeTone(value?: string | null) {
  switch ((value || '').toUpperCase()) {
    case 'CRITICAL':
      return 'danger';
    case 'WARNING':
    case 'MEDIUM':
      return 'warning';
    case 'INFO':
    case 'LOW':
      return 'success';
    default:
      return 'brand';
  }
}

function syncRoute() {
  syncingRoute = true;
  void router.replace({
    query: {
      ...route.query,
      deviceCode: normalizedDeviceCode.value || undefined,
      bindingId: selectedBindingId.value || undefined
    }
  }).finally(() => {
    syncingRoute = false;
  });
}

function handleReset() {
  deviceCode.value = initialDeviceCode.value;
  selectedBindingId.value = initialBindingId.value;
  errorMessage.value = '';
  fallbackMessage.value = '';
  void refreshAll();
}

function handleBindingChange(value: string | number | boolean) {
  const nextBindingId = Number(value);
  if (!Number.isFinite(nextBindingId)) {
    return;
  }

  selectedBindingId.value = nextBindingId;
  void refreshAll();
}

async function refreshAll() {
  if (!normalizedDeviceCode.value && !selectedBindingId.value) {
    errorMessage.value = '请输入设备编码或传入监测绑定后再刷新对象洞察。';
    return;
  }

  isLoading.value = true;
  errorMessage.value = '';
  fallbackMessage.value = '';

  try {
    let currentDetail: RiskMonitoringDetail | null = null;
    let resolvedDeviceCode = normalizedDeviceCode.value;
    let currentFallbackMessage = '';

    if (normalizedDeviceCode.value) {
      const listResponse = await getRiskMonitoringList({
        deviceCode: normalizedDeviceCode.value,
        pageNum: 1,
        pageSize: 50
      });

      bindingOptions.value = listResponse.data.records ?? [];

      const matchedBinding = selectedBindingId.value
        ? bindingOptions.value.find((item) => Number(item.bindingId) === selectedBindingId.value)
        : null;
      const primaryBinding = matchedBinding ?? pickPrimaryBinding(bindingOptions.value);

      if (!primaryBinding) {
        selectedBindingId.value = null;
        currentFallbackMessage = '当前设备未纳入风险监测绑定，以下内容基于设备上报数据补充展示';
      } else {
        selectedBindingId.value = Number(primaryBinding.bindingId);
        const detailResponse = await getRiskMonitoringDetail(selectedBindingId.value);
        currentDetail = detailResponse.data;
        resolvedDeviceCode = currentDetail.deviceCode || normalizedDeviceCode.value;
      }
    } else if (selectedBindingId.value) {
      const detailResponse = await getRiskMonitoringDetail(selectedBindingId.value);
      currentDetail = detailResponse.data;
      resolvedDeviceCode = currentDetail.deviceCode || normalizedDeviceCode.value;

      if (resolvedDeviceCode) {
        deviceCode.value = resolvedDeviceCode;
        const listResponse = await getRiskMonitoringList({
          deviceCode: resolvedDeviceCode,
          pageNum: 1,
          pageSize: 50
        });
        bindingOptions.value = listResponse.data.records ?? [];
      }
    }

    if (!resolvedDeviceCode) {
      riskDetail.value = null;
      device.value = null;
      properties.value = [];
      logs.value = [];
      errorMessage.value = '当前监测对象缺少可用的设备编码。';
      lastFetchTime.value = new Date().toISOString();
      syncRoute();
      return;
    }

    const [deviceResponse, propertyResponse, logResponse] = await Promise.all([
      getDeviceByCode(resolvedDeviceCode),
      getDeviceProperties(resolvedDeviceCode),
      getDeviceMessageLogs(resolvedDeviceCode)
    ]);

    riskDetail.value = currentDetail;
    device.value = deviceResponse.data;
    properties.value = propertyResponse.data;
    logs.value = logResponse.data;
    fallbackMessage.value = currentFallbackMessage;
    lastFetchTime.value = new Date().toISOString();
    syncRoute();

    ElMessage.success(`对象 ${resolvedDeviceCode} 洞察刷新成功`);
    recordActivity({
      module: '对象洞察台',
      action: '刷新对象洞察',
      request: {
        deviceCode: resolvedDeviceCode,
        bindingId: selectedBindingId.value
      },
      response: {
        riskPointName: currentDetail?.riskPointName,
        riskLevel: currentDetail?.riskLevel,
        properties: propertyResponse.data.length,
        logs: logResponse.data.length
      },
      ok: true,
      detail: currentDetail
        ? `对象 ${resolvedDeviceCode} 刷新完成，当前风险点 ${currentDetail.riskPointName || '--'}，属性 ${propertyResponse.data.length} 条，日志 ${logResponse.data.length} 条`
        : `对象 ${resolvedDeviceCode} 未命中风险监测绑定，已退化为设备上报分析视图，属性 ${propertyResponse.data.length} 条，日志 ${logResponse.data.length} 条`
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '对象洞察刷新失败';
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: '对象洞察台',
      action: '刷新对象洞察',
      request: {
        deviceCode: normalizedDeviceCode.value,
        bindingId: selectedBindingId.value
      },
      response: {
        message: errorMessage.value
      },
      ok: false,
      detail: `刷新失败：${errorMessage.value}`
    });
  } finally {
    isLoading.value = false;
  }
}
</script>

<style scoped>
.insight-binding-switcher,
.reason-list,
.highlight-grid {
  display: grid;
  gap: 0.9rem;
}

.insight-binding-switcher {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  padding: 1rem 1.1rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-lg);
  background: var(--surface-1);
}

.insight-binding-switcher strong {
  color: var(--text-primary);
  font-size: 1rem;
}

.insight-binding-switcher p {
  margin: 0.3rem 0 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.reason-list__item,
.highlight-card,
.report-draft {
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand) 4%, white));
  box-shadow: var(--shadow-sm);
}

.reason-list__item header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.reason-list__item strong,
.highlight-card strong {
  color: var(--text-primary);
}

.reason-list__item span,
.highlight-card span,
.highlight-card small {
  color: var(--text-tertiary);
}

.reason-list__item p,
.highlight-card p {
  margin: 0.5rem 0 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.highlight-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.highlight-card {
  display: grid;
  gap: 0.35rem;
}

.highlight-card strong {
  font-family: var(--font-display);
  font-size: 1.2rem;
}

.report-draft p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

@media (max-width: 1024px) {
  .insight-binding-switcher,
  .highlight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
