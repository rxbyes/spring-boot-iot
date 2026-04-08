<template>
  <StandardPageShell class="page-stack device-insight-view" :show-title="false">
    <StandardWorkbenchPanel
      title="对象洞察台"
      description="围绕单台设备展示基础档案、核心指标、TDengine 时序折线与综合分析。"
      show-filters
      :show-inline-state="showInlineState"
    >
      <template #filters>
        <StandardListFilterHeader :model="{ deviceCode, selectedRange }">
          <template #primary>
            <el-form-item>
              <el-input
                id="insight-device-code"
                v-model="deviceCode"
                name="insight_device_code"
                placeholder="请输入设备编码后开始综合分析"
                clearable
                prefix-icon="Search"
                @keyup.enter="handleQuery"
              />
            </el-form-item>
          </template>
          <template #secondary>
            <el-form-item>
              <el-segmented
                v-model="selectedRange"
                :options="rangeOptions"
                @change="handleRangeChange"
              />
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="query" :loading="isLoading" @click="handleQuery">
              {{ isLoading ? '分析中...' : '开始分析' }}
            </StandardButton>
            <StandardButton action="reset" :disabled="isLoading" @click="handleReset">重置</StandardButton>
          </template>
        </StandardListFilterHeader>
      </template>

      <template #inline-state>
        <StandardInlineState :message="inlineStateMessage" :tone="inlineStateTone" />
      </template>

      <div class="page-stack">
        <div v-if="errorMessage" class="empty-state" aria-live="polite">{{ errorMessage }}</div>

        <section v-if="!hasInsightContent" class="empty-state empty-state--hero">
          请输入设备编码后开始综合分析
        </section>

        <template v-else>
          <section class="insight-hero">
            <div class="insight-hero__content">
              <p class="insight-hero__eyebrow">单设备对象洞察</p>
              <h2>{{ device?.deviceName || normalizedDeviceCode || '--' }}</h2>
              <div class="insight-hero__meta">
                <span>设备编码：{{ device?.deviceCode || normalizedDeviceCode || '--' }}</span>
                <span>产品名称：{{ device?.productName || '--' }}</span>
                <span>设备类型：{{ objectTypeLabel }}</span>
              </div>
            </div>
            <div class="insight-hero__badges">
              <span class="insight-badge" :class="onlineStatusClass">{{ onlineStatusLabel }}</span>
              <span class="insight-badge insight-badge--risk">{{ riskContextBadge }}</span>
            </div>
          </section>

          <section class="core-metric-grid">
            <MetricCard
              v-for="metric in coreMetrics"
              :key="metric.label"
              :label="metric.label"
              :value="metric.value"
              :hint="metric.hint"
              :badge="metric.badge"
            />
          </section>

          <section class="two-column-grid">
            <PanelCard
              title="基础档案信息"
              description="从客户视角先确认这台设备是谁、装在哪里、归属哪个对象。"
            >
              <div class="archive-stack">
                <article class="archive-card">
                  <header class="archive-card__header">
                    <strong>设备基础档案</strong>
                    <span>设备主档</span>
                  </header>
                  <el-descriptions :column="2" border>
                    <el-descriptions-item
                      v-for="item in deviceArchiveEntries"
                      :key="item.label"
                      :label="item.label"
                    >
                      {{ item.value }}
                    </el-descriptions-item>
                  </el-descriptions>
                </article>

                <article class="archive-card">
                  <header class="archive-card__header">
                    <strong>风险上下文档案</strong>
                    <span>风险运营口径</span>
                  </header>
                  <el-descriptions :column="2" border>
                    <el-descriptions-item
                      v-for="item in riskArchiveEntries"
                      :key="item.label"
                      :label="item.label"
                    >
                      {{ item.value }}
                    </el-descriptions-item>
                  </el-descriptions>
                </article>
              </div>
            </PanelCard>

            <PanelCard
              title="综合分析"
              description="结合设备当前值、状态值、风险上下文和扩展参数形成可复述结论。"
            >
              <div class="narrative-list">
                <article v-for="paragraph in analysisParagraphs" :key="paragraph.title" class="narrative-card">
                  <header>
                    <strong>{{ paragraph.title }}</strong>
                    <span>{{ paragraph.tag }}</span>
                  </header>
                  <p>{{ paragraph.description }}</p>
                </article>
              </div>
            </PanelCard>
          </section>

          <RiskInsightTrendPanel
            :range-code="selectedRange"
            :groups="trendGroups"
            :summary="trendSummaryCards"
            :empty-message="trendEmptyMessage"
          />

          <section class="two-column-grid">
            <PanelCard
              title="核心指标"
              description="仅使用中文业务名称展示当前最关心的监测值、状态值和关键状态项。"
            >
              <div class="highlight-grid">
                <article v-for="metric in highlightedMetrics" :key="metric.label" class="highlight-card">
                  <span>{{ metric.label }}</span>
                  <strong>{{ metric.value }}</strong>
                  <small>{{ metric.hint }}</small>
                </article>
              </div>
            </PanelCard>

            <PanelCard
              title="系统自定义参数"
              description="为后续湿度、4G 信号等扩展项预留统一展示位。"
            >
              <div v-if="extensionMetrics.length" class="highlight-grid">
                <article v-for="metric in extensionMetrics" :key="metric.label" class="highlight-card">
                  <span>{{ metric.label }}</span>
                  <strong>{{ metric.value }}</strong>
                  <small>{{ metric.hint }}</small>
                </article>
              </div>
              <div v-else class="empty-state">当前设备暂无已接入的系统自定义参数。</div>
            </PanelCard>
          </section>

          <PanelCard
            title="设备属性快照"
            description="保留设备当前最新快照，便于继续核对时序指标与现场状态。"
          >
            <el-table v-if="propertyTableRows.length" :data="propertyTableRows" stripe>
              <StandardTableTextColumn prop="identifier" label="标识符" :min-width="180" />
              <StandardTableTextColumn prop="propertyName" label="属性名称" :min-width="160" />
              <StandardTableTextColumn prop="propertyValue" label="当前值" :min-width="140" />
              <StandardTableTextColumn prop="valueType" label="类型" :min-width="120" />
              <StandardTableTextColumn prop="displayTime" label="更新时间" :min-width="180" />
            </el-table>
            <div v-else class="empty-state">当前设备暂无属性快照。</div>
          </PanelCard>
        </template>
      </div>
    </StandardWorkbenchPanel>
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';

import { getTelemetryHistoryBatch, type InsightRangeCode, type TelemetryHistoryBatchResponse } from '@/api/telemetry';
import { getRiskMonitoringDetail, getRiskMonitoringList, type RiskMonitoringDetail, type RiskMonitoringListItem } from '@/api/riskMonitoring';
import { getDeviceByCode, getDeviceProperties } from '@/api/iot';
import MetricCard from '@/components/MetricCard.vue';
import PanelCard from '@/components/PanelCard.vue';
import RiskInsightTrendPanel from '@/components/RiskInsightTrendPanel.vue';
import StandardInlineState from '@/components/StandardInlineState.vue';
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardPageShell from '@/components/StandardPageShell.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue';
import type { Device, DeviceProperty } from '@/types/api';
import { formatDateTime } from '@/utils/format';
import {
  DEFAULT_INSIGHT_RANGE,
  INSIGHT_RANGE_OPTIONS,
  buildInsightHistoryRequest,
  getInsightCapabilityProfile,
  type InsightCapabilityProfile
} from '@/utils/deviceInsightCapability';
import { getInsightObjectTypeLabel, getRiskLevelLabel, pickPrimaryBinding } from '@/utils/deviceInsight';

interface InsightTrendBucket {
  time: string;
  value: number;
  filled?: boolean;
}

interface InsightTrendSeries {
  identifier: string;
  displayName: string;
  seriesType: string;
  buckets: InsightTrendBucket[];
}

interface InsightTrendGroup {
  key: string;
  title: string;
  description: string;
  series: InsightTrendSeries[];
}

interface SummaryCard {
  label: string;
  value: string;
  hint: string;
}

interface NarrativeBlock {
  title: string;
  tag: string;
  description: string;
}

function resolveRouteDeviceCode(value: unknown) {
  return typeof value === 'string' ? value.trim() : '';
}

const route = useRoute();
const router = useRouter();
const deviceCode = ref(resolveRouteDeviceCode(route.query.deviceCode));
const selectedRange = ref<InsightRangeCode>(DEFAULT_INSIGHT_RANGE);
const isLoading = ref(false);
const errorMessage = ref('');
const trendErrorMessage = ref('');
const device = ref<Device | null>(null);
const properties = ref<DeviceProperty[]>([]);
const riskBindings = ref<RiskMonitoringListItem[]>([]);
const riskDetail = ref<RiskMonitoringDetail | null>(null);
const capabilityProfile = ref<InsightCapabilityProfile>(getInsightCapabilityProfile({}));
const trendGroups = ref<InsightTrendGroup[]>([]);
const lastFetchTime = ref<string | null>(null);
const requestVersion = ref(0);
let syncingRoute = false;

const rangeOptions = INSIGHT_RANGE_OPTIONS.map((item) => ({
  label: item.label,
  value: item.value
}));

const normalizedDeviceCode = computed(() => deviceCode.value.trim());
const hasInsightContent = computed(() =>
  Boolean(device.value || properties.value.length || riskDetail.value || trendGroups.value.length)
);
const objectTypeLabel = computed(() => getInsightObjectTypeLabel(capabilityProfile.value.objectType));
const onlineStatusLabel = computed(() => (device.value?.onlineStatus === 1 ? '在线' : device.value ? '离线' : '--'));
const onlineStatusClass = computed(() => (device.value?.onlineStatus === 1 ? 'insight-badge--online' : 'insight-badge--offline'));
const riskContextBadge = computed(() => riskDetail.value ? getRiskLevelLabel(riskDetail.value.riskLevel) : '未纳入风险监测');
const showInlineState = computed(() => Boolean(inlineStateMessage.value));
const inlineStateTone = computed<'info' | 'error'>(() => (errorMessage.value ? 'error' : 'info'));
const inlineStateMessage = computed(() => {
  if (errorMessage.value) {
    return errorMessage.value;
  }
  if (!normalizedDeviceCode.value) {
    return '请输入设备编码后开始综合分析';
  }
  if (isLoading.value) {
    return `设备 ${normalizedDeviceCode.value} 正在加载单设备洞察...`;
  }
  if (!lastFetchTime.value) {
    return `设备 ${normalizedDeviceCode.value} 已就绪，等待开始分析。`;
  }
  return `${device.value?.deviceName || normalizedDeviceCode.value} · ${getRangeLabel(selectedRange.value)} · 最近刷新 ${formatDateTime(lastFetchTime.value)}`;
});

const propertyMap = computed(() => {
  const map = new Map<string, DeviceProperty>();
  properties.value.forEach((item) => {
    map.set(item.identifier, item);
  });
  return map;
});

const trendSeriesMap = computed(() => {
  const map = new Map<string, InsightTrendSeries>();
  trendGroups.value.forEach((group) => {
    group.series.forEach((series) => {
      map.set(series.identifier, series);
    });
  });
  return map;
});

const coreMetrics = computed(() =>
  capabilityProfile.value.heroMetrics.map((metric) => {
    const property = propertyMap.value.get(metric.identifier);
    const series = trendSeriesMap.value.get(metric.identifier);
    return {
      label: metric.displayName,
      value: property?.propertyValue || resolveLatestTrendValue(series),
      hint: metric.group === 'measure' ? '关键监测指标' : '关键状态指标',
      badge: {
        label: metric.group === 'measure' ? '监测' : '状态',
        tone: metric.group === 'measure' ? 'brand' : 'success'
      }
    };
  })
);

const highlightedMetrics = computed(() =>
  coreMetrics.value.map((metric) => ({
    label: metric.label,
    value: metric.value || '--',
    hint: metric.hint
  }))
);

const extensionMetrics = computed(() =>
  capabilityProfile.value.extensionParameters
    .map((parameter) => {
      const property = propertyMap.value.get(parameter.identifier);
      return property
        ? {
            label: parameter.displayName,
            value: property.propertyValue || '--',
            hint: '系统自定义参数'
          }
        : null;
    })
    .filter((item): item is { label: string; value: string; hint: string } => Boolean(item))
);

const trendSummaryCards = computed<SummaryCard[]>(() => [
  {
    label: '默认范围',
    value: getRangeLabel(selectedRange.value),
    hint: '按固定时间维度从 TDengine 取数'
  },
  {
    label: '趋势分组',
    value: String(trendGroups.value.length),
    hint: '监测数据 / 状态数据'
  },
  {
    label: '最新上报',
    value: formatDateTime(riskDetail.value?.latestReportTime || device.value?.lastReportTime),
    hint: '优先显示设备最近上报时间'
  },
  {
    label: '风险态势',
    value: riskDetail.value ? getRiskLevelLabel(riskDetail.value.riskLevel) : '未纳入风险监测',
    hint: riskDetail.value?.riskPointName || '当前未纳管'
  }
]);

const trendEmptyMessage = computed(() => {
  if (!normalizedDeviceCode.value) {
    return '请输入设备编码后开始综合分析';
  }
  if (trendErrorMessage.value) {
    return trendErrorMessage.value;
  }
  return '当前范围暂无可展示的 TDengine 趋势数据';
});

const deviceArchiveEntries = computed(() => [
  { label: '设备名称', value: device.value?.deviceName || '--' },
  { label: '设备编码', value: device.value?.deviceCode || normalizedDeviceCode.value || '--' },
  { label: '产品名称', value: device.value?.productName || '--' },
  { label: '设备类型', value: objectTypeLabel.value },
  { label: '所属机构', value: device.value?.orgName || '--' },
  { label: '部署位置', value: device.value?.address || '--' },
  { label: '接入协议', value: device.value?.protocolCode || '--' },
  { label: '固件版本', value: device.value?.firmwareVersion || '--' },
  { label: '最近在线时间', value: formatDateTime(device.value?.lastOnlineTime) },
  { label: '最近上报时间', value: formatDateTime(device.value?.lastReportTime) }
]);

const riskArchiveEntries = computed(() => [
  { label: '关联风险点名称', value: riskDetail.value?.riskPointName || '当前未纳管' },
  { label: '风险点编码', value: riskDetail.value?.riskPointCode || '--' },
  { label: '当前主监测项', value: riskDetail.value?.metricName || '未绑定监测项' },
  { label: '当前风险态势', value: riskDetail.value ? getRiskLevelLabel(riskDetail.value.riskLevel) : '未纳入风险监测' },
  { label: '当前绑定状态', value: riskDetail.value ? '已纳入风险监测' : '当前未纳管' },
  { label: '风险等级来源说明', value: riskDetail.value ? '来自风险运营实时绑定明细' : '暂无风险运营绑定数据' }
]);

const analysisParagraphs = computed<NarrativeBlock[]>(() => {
  const firstMetric = coreMetrics.value[0];
  const statusMetric = coreMetrics.value[1];
  const batteryMetric = coreMetrics.value[2];
  const blocks: NarrativeBlock[] = [
    {
      title: '设备现状',
      tag: '当前状态',
      description: `${device.value?.deviceName || normalizedDeviceCode.value || '当前设备'}当前${onlineStatusLabel.value}，最近上报时间为${formatDateTime(riskDetail.value?.latestReportTime || device.value?.lastReportTime)}。`
    },
    {
      title: '风险上下文',
      tag: '风险运营',
      description: riskDetail.value
        ? `当前已纳入“${riskDetail.value.riskPointName || '--'}”的风险监测，主监测项为${riskDetail.value.metricName || '未命名指标'}，当前风险态势为${getRiskLevelLabel(riskDetail.value.riskLevel)}。`
        : '当前设备尚未纳入风险监测绑定，可先从设备本体趋势与状态指标开展单设备分析。'
    },
    {
      title: '核心指标解读',
      tag: '综合研判',
      description: `${firstMetric?.label || '监测值'}当前为${firstMetric?.value || '--'}，${statusMetric?.label || '状态值'}当前为${statusMetric?.value || '--'}，${batteryMetric?.label || '关键状态项'}当前为${batteryMetric?.value || '--'}。`
    }
  ];
  if (extensionMetrics.value.length) {
    blocks.push({
      title: '系统自定义参数',
      tag: '扩展位',
      description: extensionMetrics.value.map((item) => `${item.label}${item.value}`).join('，')
    });
  }
  return blocks;
});

const propertyTableRows = computed(() =>
  properties.value.map((item) => ({
    ...item,
    displayTime: formatDateTime(item.updateTime || item.reportTime)
  }))
);

watch(
  () => route.query.deviceCode,
  (value) => {
    if (syncingRoute) {
      return;
    }
    const nextCode = resolveRouteDeviceCode(value);
    if (nextCode === deviceCode.value) {
      return;
    }
    deviceCode.value = nextCode;
    if (!nextCode) {
      resetInsightState();
      return;
    }
    void loadInsight('route-change');
  }
);

onMounted(() => {
  if (normalizedDeviceCode.value) {
    void loadInsight('route-change');
  }
});

function handleQuery() {
  if (!normalizedDeviceCode.value) {
    resetInsightState();
    return;
  }
  void loadInsight('manual-query');
}

function handleReset() {
  deviceCode.value = '';
  selectedRange.value = DEFAULT_INSIGHT_RANGE;
  resetInsightState();
  syncRoute();
}

function handleRangeChange(value: string | number | boolean) {
  if (typeof value !== 'string') {
    return;
  }
  selectedRange.value = value as InsightRangeCode;
  if (normalizedDeviceCode.value) {
    void loadInsight('range-change');
  }
}

async function loadInsight(_source: 'route-change' | 'manual-query' | 'range-change') {
  const code = normalizedDeviceCode.value;
  if (!code) {
    resetInsightState();
    return;
  }

  const version = ++requestVersion.value;
  isLoading.value = true;
  errorMessage.value = '';
  trendErrorMessage.value = '';

  try {
    const deviceResponse = await getDeviceByCode(code);
    if (version !== requestVersion.value) {
      return;
    }

    device.value = deviceResponse.data;
    capabilityProfile.value = getInsightCapabilityProfile(deviceResponse.data);

    const [propertyResponse, bindingResponse] = await Promise.all([
      getDeviceProperties(code),
      getRiskMonitoringList({
        deviceCode: code,
        pageNum: 1,
        pageSize: 50
      })
    ]);
    if (version !== requestVersion.value) {
      return;
    }

    properties.value = propertyResponse.data ?? [];
    riskBindings.value = bindingResponse.data.records ?? [];

    const primaryBinding = pickPrimaryBinding(riskBindings.value);
    if (primaryBinding) {
      const detailResponse = await getRiskMonitoringDetail(primaryBinding.bindingId);
      if (version !== requestVersion.value) {
        return;
      }
      riskDetail.value = detailResponse.data;
    } else {
      riskDetail.value = null;
    }

    if (device.value?.id && capabilityProfile.value.historyIdentifiers.length) {
      try {
        const historyResponse = await getTelemetryHistoryBatch(
          buildInsightHistoryRequest(device.value.id, capabilityProfile.value, selectedRange.value)
        );
        if (version !== requestVersion.value) {
          return;
        }
        trendGroups.value = buildTrendGroups(historyResponse.data, capabilityProfile.value);
      } catch (error) {
        trendGroups.value = [];
        trendErrorMessage.value = error instanceof Error ? error.message : 'TDengine 趋势查询失败';
      }
    } else {
      trendGroups.value = [];
    }

    lastFetchTime.value = new Date().toISOString();
    syncRoute();
    ElMessage.success(`设备 ${code} 洞察加载成功`);
  } catch (error) {
    if (version !== requestVersion.value) {
      return;
    }
    errorMessage.value = error instanceof Error ? error.message : '对象洞察加载失败';
  } finally {
    if (version === requestVersion.value) {
      isLoading.value = false;
    }
  }
}

function resetInsightState() {
  errorMessage.value = '';
  trendErrorMessage.value = '';
  device.value = null;
  properties.value = [];
  riskBindings.value = [];
  riskDetail.value = null;
  trendGroups.value = [];
  lastFetchTime.value = null;
}

function syncRoute() {
  syncingRoute = true;
  void router.replace({
    query: {
      ...route.query,
      deviceCode: normalizedDeviceCode.value || undefined
    }
  }).finally(() => {
    syncingRoute = false;
  });
}

function buildTrendGroups(data: TelemetryHistoryBatchResponse, profile: InsightCapabilityProfile) {
  const pointMap = new Map((data.points ?? []).map((item) => [item.identifier, item]));
  return profile.trendGroups
    .map((group) => ({
      key: group.key,
      title: group.title,
      description: group.key === 'measure'
        ? '展示设备本体的监测值折线变化。'
        : '展示在线状态、电量等状态值折线变化。',
      series: group.identifiers
        .map((identifier) => {
          const point = pointMap.get(identifier);
          if (!point) {
            return null;
          }
          return {
            identifier,
            displayName: point.displayName || resolveDisplayName(profile, identifier),
            seriesType: point.seriesType || group.key,
            buckets: point.buckets ?? []
          };
        })
        .filter((item): item is InsightTrendSeries => Boolean(item))
    }))
    .filter((group) => group.series.length > 0);
}

function resolveDisplayName(profile: InsightCapabilityProfile, identifier: string) {
  const heroMetric = profile.heroMetrics.find((item) => item.identifier === identifier);
  if (heroMetric) {
    return heroMetric.displayName;
  }
  const extensionMetric = profile.extensionParameters.find((item) => item.identifier === identifier);
  if (extensionMetric) {
    return extensionMetric.displayName;
  }
  return identifier;
}

function resolveLatestTrendValue(series?: InsightTrendSeries) {
  const latestBucket = series?.buckets?.length ? series.buckets[series.buckets.length - 1] : null;
  if (!latestBucket) {
    return '--';
  }
  return String(latestBucket.value);
}

function getRangeLabel(rangeCode: InsightRangeCode) {
  const option = INSIGHT_RANGE_OPTIONS.find((item) => item.value === rangeCode);
  return option?.label || '近一周';
}
</script>

<style scoped>
.empty-state--hero,
.insight-hero,
.archive-card,
.narrative-card,
.highlight-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand) 4%, white));
  box-shadow: var(--shadow-sm);
}

.empty-state--hero,
.insight-hero {
  padding: 1.4rem;
}

.insight-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.insight-hero__eyebrow {
  margin: 0 0 0.35rem;
  color: var(--text-tertiary);
  letter-spacing: 0.08em;
}

.insight-hero__content h2 {
  margin: 0;
  color: var(--text-primary);
}

.insight-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 0.8rem;
  color: var(--text-secondary);
}

.insight-hero__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.insight-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.45rem 0.8rem;
  border-radius: 999px;
  font-weight: 600;
}

.insight-badge--online {
  background: rgba(19, 179, 139, 0.12);
  color: #0e8d6d;
}

.insight-badge--offline {
  background: rgba(112, 128, 144, 0.12);
  color: #5a6d85;
}

.insight-badge--risk {
  background: rgba(255, 122, 26, 0.12);
  color: #d76610;
}

.core-metric-grid,
.archive-stack,
.narrative-list,
.highlight-grid {
  display: grid;
  gap: 0.9rem;
}

.core-metric-grid,
.highlight-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.archive-card,
.narrative-card,
.highlight-card {
  padding: 1rem;
}

.archive-stack {
  gap: 1rem;
}

.archive-card__header,
.narrative-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.8rem;
}

.archive-card__header strong,
.narrative-card strong,
.highlight-card strong {
  color: var(--text-primary);
}

.archive-card__header span,
.narrative-card span,
.highlight-card span,
.highlight-card small {
  color: var(--text-tertiary);
}

.narrative-card p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.highlight-card {
  display: grid;
  gap: 0.35rem;
}

.highlight-card strong {
  font-size: 1.2rem;
  font-family: var(--font-display);
}

@media (max-width: 1024px) {
  .insight-hero,
  .core-metric-grid,
  .highlight-grid {
    grid-template-columns: 1fr;
  }

  .insight-hero {
    flex-direction: column;
  }
}
</style>
