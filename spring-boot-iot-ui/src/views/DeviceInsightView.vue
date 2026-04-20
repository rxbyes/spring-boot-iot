<template>
  <StandardPageShell class="page-stack device-insight-view" :show-title="false">
  <StandardWorkbenchPanel
      title="对象洞察台"
      description="围绕单台设备展示基础档案、监测快照、TDengine 时序折线与综合分析。"
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
                placeholder="请输入设备编码后开始综合分析"
                clearable
                prefix-icon="Search"
                @keyup.enter="handleQuery"
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
              description="结合监测快照、设备状态和风险上下文形成可复述结论。"
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

          <CollectorChildInsightPanel
            v-if="collectorOverview?.children?.length"
            :overview="collectorOverview"
          />

          <section
            v-if="collectorRecommendationNotice"
            class="insight-recommendation-banner"
            aria-live="polite"
          >
            {{ collectorRecommendationNotice }}
          </section>

          <RiskInsightTrendPanel
            :range-code="selectedRange"
            :groups="trendGroups"
            :empty-message="trendEmptyMessage"
            @change-range="handleRangeChange"
          />

          <PanelCard
            title="设备属性快照"
            description="展示设备当前最新运行态值，名称与单位优先沿用正式字段配置，不用物模型定义替代真实值。"
          >
            <el-table
              v-if="propertyTableRows.length"
              :data="propertyTableRows"
              class="monitoring-snapshot-table"
              stripe
              table-layout="fixed"
            >
              <StandardTableTextColumn prop="identifier" label="标识符" :min-width="180" />
              <StandardTableTextColumn prop="displayName" label="属性名称" :min-width="160" />
              <StandardTableTextColumn prop="propertyValue" label="当前值" :min-width="140" />
              <StandardTableTextColumn prop="displayUnit" label="单位" :min-width="100" />
              <StandardTableTextColumn prop="valueType" label="类型" :min-width="120" />
              <StandardTableTextColumn prop="displayTime" label="更新时间" :min-width="180" />
            </el-table>
            <div v-else class="empty-state">{{ snapshotEmptyMessage }}</div>
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
import { getCollectorChildInsightOverview, getDeviceByCode, getDeviceProperties } from '@/api/iot';
import { productApi } from '@/api/product';
import CollectorChildInsightPanel from '@/components/device/CollectorChildInsightPanel.vue';
import PanelCard from '@/components/PanelCard.vue';
import RiskInsightTrendPanel from '@/components/RiskInsightTrendPanel.vue';
import StandardInlineState from '@/components/StandardInlineState.vue';
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardPageShell from '@/components/StandardPageShell.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue';
import type { CollectorChildInsightOverview, Device, DeviceProperty, ProductModel } from '@/types/api';
import { formatDateTime } from '@/utils/format';
import {
  DEFAULT_INSIGHT_RANGE,
  INSIGHT_RANGE_OPTIONS,
  buildInsightHistoryRequest,
  getInsightCapabilityProfile,
  type InsightCapabilityProfile
} from '@/utils/deviceInsightCapability';
import { getInsightObjectTypeLabel, getRiskLevelLabel, pickPrimaryBinding } from '@/utils/deviceInsight';
import { resolveInsightMetricDisplayName } from '@/utils/deviceInsightNaming';

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

interface NarrativeBlock {
  title: string;
  tag: string;
  description: string;
}

function resolveRouteDeviceCode(value: unknown) {
  return typeof value === 'string' ? value.trim() : '';
}

function resolveRouteRangeCode(value: unknown): InsightRangeCode {
  if (typeof value !== 'string') {
    return DEFAULT_INSIGHT_RANGE;
  }
  return INSIGHT_RANGE_OPTIONS.some((item) => item.value === value)
    ? value as InsightRangeCode
    : DEFAULT_INSIGHT_RANGE;
}

const route = useRoute();
const router = useRouter();
const deviceCode = ref(resolveRouteDeviceCode(route.query.deviceCode));
const selectedRange = ref<InsightRangeCode>(resolveRouteRangeCode(route.query.rangeCode));
const isLoading = ref(false);
const errorMessage = ref('');
const trendErrorMessage = ref('');
const device = ref<Device | null>(null);
const properties = ref<DeviceProperty[]>([]);
const collectorOverview = ref<CollectorChildInsightOverview | null>(null);
const riskBindings = ref<RiskMonitoringListItem[]>([]);
const riskDetail = ref<RiskMonitoringDetail | null>(null);
const capabilityProfile = ref<InsightCapabilityProfile>(getInsightCapabilityProfile({}));
const trendGroups = ref<InsightTrendGroup[]>([]);
const productModelDisplayNameMap = ref<Map<string, string>>(new Map());
const productModelDataTypeMap = ref<Map<string, string>>(new Map());
const productModelUnitMap = ref<Map<string, string>>(new Map());
const lastFetchTime = ref<string | null>(null);
const requestVersion = ref(0);
let syncingRoute = false;

const normalizedDeviceCode = computed(() => deviceCode.value.trim());
const isCollectorParentInsight = computed(() => Number(device.value?.nodeType) === 2);
const hasCollectorChildren = computed(() => Boolean(collectorOverview.value?.children?.length));
const hasInsightContent = computed(() =>
  Boolean(device.value || properties.value.length || riskDetail.value || trendGroups.value.length || collectorOverview.value?.children?.length)
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

const configuredSnapshotMetricMap = computed(() => {
  const map = new Map<string, InsightCapabilityProfile['customMetrics'][number]>();
  capabilityProfile.value.customMetrics
    .filter((metric) => metric.enabled !== false && metric.includeInTrend !== false)
    .sort((left, right) => {
      const sortDiff = (left.sortNo ?? Number.MAX_SAFE_INTEGER) - (right.sortNo ?? Number.MAX_SAFE_INTEGER);
      if (sortDiff !== 0) {
        return sortDiff;
      }
      return left.identifier.localeCompare(right.identifier);
    })
    .forEach((metric) => {
      map.set(metric.identifier, metric);
    });
  return map;
});

const snapshotMetrics = computed(() =>
  capabilityProfile.value.heroMetrics.map((metric) => {
    const property = propertyMap.value.get(metric.identifier);
    const series = trendSeriesMap.value.get(metric.identifier);
    return {
      label: appendUnitToDisplayName(
        resolveMetricBaseName(
          metric.identifier,
          resolveProductModelValue(productModelDisplayNameMap.value, metric.identifier),
          metric.displayName,
          property?.propertyName
        ),
        resolveMetricUnit(metric.identifier, property)
      ),
      value: property?.propertyValue || resolveLatestTrendValue(series)
    };
  })
);

const trendEmptyMessage = computed(() => {
  if (!normalizedDeviceCode.value) {
    return '请输入设备编码后开始综合分析';
  }
  if (trendErrorMessage.value) {
    return trendErrorMessage.value;
  }
  if (!capabilityProfile.value.historyIdentifiers.length) {
    return isCollectorParentInsight.value && hasCollectorChildren.value
      ? '当前采集器父设备未配置可展示的父设备趋势指标；子设备指标请查看子设备总览，并到 /products 为父设备或对应子产品单独配置对象洞察。'
      : '当前产品未配置对象洞察重点趋势指标，请到 /products 先将正式字段加入对象洞察后再查看趋势。';
  }
  return '当前范围暂无可展示的 TDengine 趋势数据';
});

const snapshotEmptyMessage = computed(() => {
  if (!normalizedDeviceCode.value) {
    return '请输入设备编码后开始综合分析';
  }
  if (isCollectorParentInsight.value && hasCollectorChildren.value) {
    return '当前采集器父设备暂无自身运行态属性快照；子设备监测值与 sensor_state 请查看子设备总览或进入子设备对象洞察。';
  }
  return '当前设备暂无最新属性快照，请检查设备上报与 latest 属性写入链路。';
});

const collectorRecommendationNotice = computed(() => {
  const entries = (collectorOverview.value?.children ?? [])
    .flatMap((child) => (child.metrics ?? [])
      .filter((metric) => metric.recommended)
      .map((metric) => `${child.childDeviceName || child.logicalChannelCode} / ${metric.displayName || metric.identifier}`));
  if (!entries.length) {
    return '';
  }
  return `建议优先纳入对象洞察：${entries.join('；')}`;
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
  const metricSummary = snapshotMetrics.value.length
    ? snapshotMetrics.value.map((metric) => `${metric.label}当前为${metric.value || '--'}`).join('，')
    : '当前暂无可直接引用的监测快照';
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
      title: '监测研判',
      tag: '综合研判',
      description: `${metricSummary}。`
    }
  ];
  return blocks;
});

const propertyTableRows = computed(() => {
  const orderedIdentifiers = uniqueIdentifiers([
    ...Array.from(configuredSnapshotMetricMap.value.keys()),
    ...properties.value.map((item) => item.identifier)
  ]);

  return orderedIdentifiers.map((identifier) => {
    const property = propertyMap.value.get(identifier);
    const series = trendSeriesMap.value.get(identifier);
    const configuredMetric = configuredSnapshotMetricMap.value.get(identifier);
    const latestActualBucket = resolveLatestActualTrendBucket(series);

    return {
      ...(property ?? {}),
      identifier,
      propertyValue: normalizeText(property?.propertyValue) || (latestActualBucket ? String(latestActualBucket.value) : '--'),
      valueType: normalizeText(property?.valueType) || resolveProductModelValue(productModelDataTypeMap.value, identifier) || '--',
      displayName: resolveMetricBaseName(
        identifier,
        resolveProductModelValue(productModelDisplayNameMap.value, identifier),
        property?.propertyName,
        configuredMetric?.displayName,
        series?.displayName
      ),
      displayUnit: resolveMetricUnit(identifier, property) || '--',
      displayTime: formatDateTime(property?.updateTime || property?.reportTime || latestActualBucket?.time)
    };
  });
});

watch(
  () => [route.query.deviceCode, route.query.rangeCode],
  ([deviceCodeValue, rangeCodeValue]) => {
    if (syncingRoute) {
      return;
    }
    const nextCode = resolveRouteDeviceCode(deviceCodeValue);
    const nextRange = resolveRouteRangeCode(rangeCodeValue);
    const codeChanged = nextCode !== deviceCode.value;
    const rangeChanged = nextRange !== selectedRange.value;
    if (!codeChanged && !rangeChanged) {
      return;
    }
    deviceCode.value = nextCode;
    selectedRange.value = nextRange;
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
  collectorOverview.value = null;

  try {
    const deviceResponse = await getDeviceByCode(code);
    if (version !== requestVersion.value) {
      return;
    }

    device.value = deviceResponse.data;

    const collectorOverviewRequest = shouldLoadCollectorInsightOverview(deviceResponse.data)
      ? getCollectorChildInsightOverview(code).catch((error) => {
        console.warn('采集器子设备总览加载失败', error);
        return null;
      })
      : Promise.resolve(null);

    const [propertyResponse, bindingResponse, productInsightSupplement, collectorOverviewResponse] = await Promise.all([
      getDeviceProperties(code),
      getRiskMonitoringList({
        deviceCode: code,
        pageNum: 1,
        pageSize: 50
      }),
      loadProductInsightSupplement(deviceResponse.data?.productId),
      collectorOverviewRequest
    ]);
    if (version !== requestVersion.value) {
      return;
    }

    properties.value = propertyResponse.data ?? [];
    riskBindings.value = bindingResponse.data.records ?? [];
    productModelDisplayNameMap.value = productInsightSupplement.modelDisplayNameMap;
    productModelDataTypeMap.value = productInsightSupplement.modelDataTypeMap;
    productModelUnitMap.value = productInsightSupplement.modelUnitMap;
    collectorOverview.value = collectorOverviewResponse?.data ?? null;

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

    capabilityProfile.value = getInsightCapabilityProfile({
      deviceCode: device.value?.deviceCode,
      productName: device.value?.productName,
      metricIdentifier: riskDetail.value?.metricIdentifier,
      metricName: riskDetail.value?.metricName,
      riskPointName: riskDetail.value?.riskPointName,
      properties: properties.value,
      deviceMetadataJson: device.value?.metadataJson,
      productMetadataJson: productInsightSupplement.metadataJson
    });

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
  collectorOverview.value = null;
  riskBindings.value = [];
  riskDetail.value = null;
  trendGroups.value = [];
  productModelDisplayNameMap.value = new Map();
  productModelDataTypeMap.value = new Map();
  productModelUnitMap.value = new Map();
  lastFetchTime.value = null;
}

function syncRoute() {
  syncingRoute = true;
  void router.replace({
    query: {
      ...route.query,
      deviceCode: normalizedDeviceCode.value || undefined,
      rangeCode: normalizedDeviceCode.value ? selectedRange.value : undefined
    }
  }).finally(() => {
    syncingRoute = false;
  });
}

function buildTrendGroups(data: TelemetryHistoryBatchResponse, profile: InsightCapabilityProfile) {
  const points = data.points ?? [];
  const resolvedGroups = profile.trendGroups
    .map((group) => ({
      key: resolveTrendGroupViewKey(group.key),
      title: group.title,
      description: resolveTrendGroupDescription(group.key),
      series: group.identifiers
        .map((identifier) => {
          const point = resolveTelemetryHistoryPoint(points, identifier);
          if (!point) {
            return null;
          }
          const property = propertyMap.value.get(identifier);
          const profileDisplayName = resolveDisplayName(profile, identifier);
          return {
            identifier,
            displayName: appendUnitToDisplayName(
              resolveMetricBaseName(
                identifier,
                resolveProductModelValue(productModelDisplayNameMap.value, identifier),
                profileDisplayName,
                property?.propertyName,
                point.displayName
              ),
              resolveMetricUnit(identifier, property)
            ),
            seriesType: resolveTrendSeriesType(point.seriesType, group.key),
            buckets: point.buckets ?? []
          };
        })
        .filter((item): item is InsightTrendSeries => Boolean(item))
    }))
    .filter((group) => group.series.length > 0);

  return resolvedGroups.flatMap((group) => splitTrendGroup(group));
}

function resolveTelemetryHistoryPoint(
  points: TelemetryHistoryBatchResponse['points'],
  identifier: string
) {
  const normalizedIdentifier = normalizeText(identifier);
  if (!normalizedIdentifier) {
    return null;
  }

  const exactPoint = points.find((item) => normalizeText(item.identifier) === normalizedIdentifier);
  if (exactPoint) {
    return exactPoint;
  }

  const lowerCaseIdentifier = normalizedIdentifier.toLowerCase();
  const caseInsensitivePoint = points.find((item) => normalizeText(item.identifier).toLowerCase() === lowerCaseIdentifier);
  if (caseInsensitivePoint) {
    return caseInsensitivePoint;
  }

  if (normalizedIdentifier.includes('.')) {
    const suffixIdentifier = normalizedIdentifier.split('.').pop();
    if (!suffixIdentifier) {
      return null;
    }
    return resolveUniqueTelemetryHistoryPoint(points, (itemIdentifier) => itemIdentifier.toLowerCase() === suffixIdentifier.toLowerCase());
  }

  return resolveUniqueTelemetryHistoryPoint(points, (itemIdentifier) =>
    itemIdentifier.toLowerCase().endsWith(`.${lowerCaseIdentifier}`)
  );
}

function resolveUniqueTelemetryHistoryPoint(
  points: TelemetryHistoryBatchResponse['points'],
  matcher: (identifier: string) => boolean
) {
  const matchedPoints = points.filter((item) => {
    const itemIdentifier = normalizeText(item.identifier);
    return itemIdentifier ? matcher(itemIdentifier) : false;
  });
  return matchedPoints.length === 1 ? matchedPoints[0] : null;
}

function splitTrendGroup(group: InsightTrendGroup) {
  if (group.key !== 'status') {
    return [group];
  }

  const eventSeries = group.series
    .filter((series) => isStatusEventSeries(series))
    .map((series) => ({
      ...series,
      seriesType: 'event'
    }));
  const runtimeSeries = group.series.filter((series) => !isStatusEventSeries(series));
  const nextGroups: InsightTrendGroup[] = [];

  if (eventSeries.length) {
    nextGroups.push({
      key: 'status-event',
      title: '状态事件',
      description: '展示设备状态码与离散状态变化。',
      series: eventSeries
    });
  }

  if (runtimeSeries.length) {
    nextGroups.push({
      key: 'status-runtime',
      title: '运行参数',
      description: '展示电量、信号等连续运行参数变化。',
      series: runtimeSeries
    });
  }

  return nextGroups.length ? nextGroups : [group];
}

function resolveTrendGroupViewKey(groupKey: string) {
  if (groupKey === 'statusEvent') {
    return 'status-event';
  }
  if (groupKey === 'runtime') {
    return 'status-runtime';
  }
  return groupKey;
}

function resolveTrendGroupDescription(groupKey: string) {
  if (groupKey === 'measure') {
    return '展示设备本体的监测值折线变化。';
  }
  if (groupKey === 'statusEvent') {
    return '展示设备状态码与离散状态变化。';
  }
  if (groupKey === 'runtime') {
    return '展示电量、信号等连续运行参数变化。';
  }
  return '展示设备趋势变化。';
}

function resolveTrendSeriesType(seriesType: string | null | undefined, groupKey: string) {
  if (seriesType?.trim()) {
    return seriesType;
  }
  if (groupKey === 'statusEvent') {
    return 'event';
  }
  if (groupKey === 'runtime') {
    return 'status';
  }
  return groupKey;
}

function isStatusEventSeries(series: InsightTrendSeries) {
  const semanticSource = `${series.identifier} ${series.displayName}`.toLowerCase();
  if (/(battery|signal|humidity|temperature|temp|voltage|current|power|energy|network|4g|rssi|snr|dbm|湿度|温度|电量|信号|电压|电流|功率|能量)/.test(semanticSource)) {
    return false;
  }
  if (/(sensor_state|online|alarm|warn|status|state|switch|enable|relay|valve|pump|door|light|horn|开关|启停|开启|关闭|阀|泵|门|声光|告警|预警|报警|在线|状态)/.test(semanticSource)) {
    return true;
  }

  const actualValues = series.buckets
    .filter((bucket) => bucket.filled === false)
    .map((bucket) => Number(bucket.value))
    .filter((value) => Number.isFinite(value));

  if (!actualValues.length) {
    return false;
  }

  return actualValues.every((value) => Number.isInteger(value) && value >= -3 && value <= 1);
}

function resolveDisplayName(profile: InsightCapabilityProfile, identifier: string) {
  const heroMetric = profile.heroMetrics.find((item) => item.identifier === identifier);
  if (heroMetric) {
    return resolveMetricBaseName(identifier, heroMetric.displayName);
  }
  const extensionMetric = profile.extensionParameters.find((item) => item.identifier === identifier);
  if (extensionMetric) {
    return resolveMetricBaseName(identifier, extensionMetric.displayName);
  }
  const productModelDisplayName = resolveProductModelValue(productModelDisplayNameMap.value, identifier);
  if (productModelDisplayName) {
    return resolveMetricBaseName(identifier, productModelDisplayName);
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

function resolveLatestActualTrendBucket(series?: InsightTrendSeries) {
  return [...(series?.buckets ?? [])]
    .reverse()
    .find((bucket) => bucket.filled !== true && bucket.value !== null && bucket.value !== undefined);
}

function resolvePropertyUnit(item: DeviceProperty) {
  return resolveMetricUnit(item.identifier, item) || '--';
}

function shouldLoadCollectorInsightOverview(currentDevice?: Device | null) {
  if (!currentDevice) {
    return false;
  }
  if (Number(currentDevice.nodeType) === 2) {
    return true;
  }
  // `nf-collect-rtu-v1` 仍按 nodeType=1 建档，但对象洞察读侧要继续按采集器父设备处理。
  return (currentDevice.productKey || '').trim().toLowerCase() === 'nf-collect-rtu-v1';
}

async function loadProductInsightSupplement(productId?: string | number | null) {
  const emptySupplement = {
    metadataJson: null as string | null,
    modelDisplayNameMap: new Map<string, string>(),
    modelDataTypeMap: new Map<string, string>(),
    modelUnitMap: new Map<string, string>()
  };
  if (productId === undefined || productId === null || productId === '') {
    return emptySupplement;
  }
  try {
    const [productResult, modelResult] = await Promise.allSettled([
      productApi.getProductById(productId),
      productApi.listProductModels(productId)
    ]);
    return {
      metadataJson: productResult.status === 'fulfilled' && productResult.value.code === 200
        ? productResult.value.data?.metadataJson ?? null
        : null,
      modelDisplayNameMap: buildProductModelDisplayNameMap(
        modelResult.status === 'fulfilled' && modelResult.value.code === 200
          ? modelResult.value.data ?? []
          : []
      ),
      modelDataTypeMap: buildProductModelDataTypeMap(
        modelResult.status === 'fulfilled' && modelResult.value.code === 200
          ? modelResult.value.data ?? []
          : []
      ),
      modelUnitMap: buildProductModelUnitMap(
        modelResult.status === 'fulfilled' && modelResult.value.code === 200
          ? modelResult.value.data ?? []
          : []
      )
    };
  } catch (error) {
    console.warn('对象洞察产品配置补充失败', error);
    return emptySupplement;
  }
}

function buildProductModelDisplayNameMap(models: ProductModel[]) {
  const map = new Map<string, string>();
  models.forEach((model) => {
    const displayName = normalizeText(model.modelName);
    if (displayName) {
      map.set(model.identifier, displayName);
    }
  });
  return map;
}

function buildProductModelDataTypeMap(models: ProductModel[]) {
  const map = new Map<string, string>();
  models.forEach((model) => {
    const dataType = normalizeText(model.dataType);
    if (dataType) {
      map.set(model.identifier, dataType);
    }
  });
  return map;
}

function buildProductModelUnitMap(models: ProductModel[]) {
  const map = new Map<string, string>();
  models.forEach((model) => {
    const unit = resolveProductModelUnit(model);
    if (unit) {
      map.set(model.identifier, unit);
    }
  });
  return map;
}

function resolveProductModelUnit(model: ProductModel) {
  return normalizeText(parseSpecsJson(model.specsJson)?.unit);
}

function resolveMetricBaseName(identifier: string, ...candidates: Array<unknown>) {
  for (const candidate of candidates) {
    const normalizedCandidate = normalizeText(candidate);
    if (!normalizedCandidate) {
      continue;
    }
    const resolved = resolveInsightMetricDisplayName(identifier, normalizedCandidate);
    if (resolved) {
      return resolved;
    }
  }
  return resolveInsightMetricDisplayName(identifier);
}

function resolveMetricUnit(identifier: string, property?: DeviceProperty | null) {
  return normalizeText(property?.unit) || resolveProductModelValue(productModelUnitMap.value, identifier) || '';
}

function resolveProductModelValue(map: Map<string, string>, identifier: string) {
  const exactValue = map.get(identifier);
  if (exactValue) {
    return exactValue;
  }
  const normalizedIdentifier = identifier.trim().toLowerCase();
  for (const [key, value] of map.entries()) {
    if (key.trim().toLowerCase() === normalizedIdentifier) {
      return value;
    }
  }
  return '';
}

function appendUnitToDisplayName(displayName: string, unit?: string) {
  const normalizedDisplayName = normalizeText(displayName);
  const normalizedUnit = normalizeText(unit);
  if (!normalizedDisplayName || !normalizedUnit || normalizedDisplayName.includes(normalizedUnit)) {
    return normalizedDisplayName;
  }
  return `${normalizedDisplayName}（${normalizedUnit}）`;
}

function parseSpecsJson(specsJson?: string | null) {
  if (!specsJson) {
    return null;
  }
  try {
    const parsed = JSON.parse(specsJson);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? parsed as Record<string, unknown>
      : null;
  } catch {
    return null;
  }
}

function normalizeText(value: unknown) {
  return typeof value === 'string' && value.trim() ? value.trim() : '';
}

function uniqueIdentifiers(values: string[]) {
  return values.filter((value, index) => Boolean(value) && values.indexOf(value) === index);
}

function getRangeLabel(rangeCode: InsightRangeCode) {
  const option = INSIGHT_RANGE_OPTIONS.find((item) => item.value === rangeCode);
  return option?.label || '近一天';
}
</script>

<style scoped>
.empty-state--hero,
.insight-hero,
.archive-card,
.narrative-card,
.highlight-card,
.insight-recommendation-banner {
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand) 4%, white));
  box-shadow: var(--shadow-sm);
}

.empty-state--hero,
.insight-hero {
  padding: 1.4rem;
}

.insight-recommendation-banner {
  padding: 1rem 1.15rem;
  color: #9a3412;
  border-color: color-mix(in srgb, #f59e0b 24%, var(--panel-border));
  background: linear-gradient(140deg, rgba(255, 251, 235, 0.98), rgba(255, 255, 255, 0.98));
  line-height: 1.7;
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

.archive-stack,
.narrative-list,
.highlight-grid {
  display: grid;
  gap: 0.9rem;
}

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

.monitoring-snapshot-table :deep(.cell) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 1024px) {
  .insight-hero,
  .highlight-grid {
    grid-template-columns: 1fr;
  }

  .insight-hero {
    flex-direction: column;
  }
}
</style>
