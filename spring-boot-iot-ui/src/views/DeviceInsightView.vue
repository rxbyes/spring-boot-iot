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
        <div v-if="errorMessage" class="insight-state-banner insight-state-banner--error" aria-live="polite">
          <strong>这次分析暂时被打断了</strong>
          <p>{{ errorMessage }}</p>
        </div>

        <section
          v-if="!hasInsightContent"
          class="insight-state-card"
          :class="`insight-state-card--${insightEntryState.tone}`"
        >
          <div class="insight-state-card__copy">
            <span class="insight-state-card__eyebrow">{{ insightEntryState.eyebrow }}</span>
            <h2>{{ insightEntryState.title }}</h2>
            <p>{{ insightEntryState.description }}</p>
          </div>
          <div class="insight-state-card__aside">
            <span
              v-if="insightEntryState.tone === 'loading'"
              class="insight-state-card__pulse"
              aria-hidden="true"
            />
            <span
              v-for="pill in insightEntryState.pills"
              :key="pill"
              class="insight-state-card__pill"
            >
              {{ pill }}
            </span>
          </div>
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
              <div class="insight-detail-shell">
                <section class="insight-detail-identity">
                  <div class="insight-detail-identity__main">
                    <strong>{{ device?.deviceName || normalizedDeviceCode || '--' }}</strong>
                    <p>{{ device?.deviceCode || normalizedDeviceCode || '--' }} · {{ device?.productName || '--' }}</p>
                  </div>
                  <div class="insight-detail-identity__meta">
                    <span class="insight-meta-pill" :class="onlineStatusClass">{{ onlineStatusLabel }}</span>
                    <span class="insight-meta-pill">{{ objectTypeLabel }}</span>
                  </div>
                </section>

                <div class="insight-detail-grid">
                  <article class="insight-detail-section">
                    <header class="insight-detail-section__header">
                      <div class="insight-detail-section__title">
                        <strong>设备基础档案</strong>
                        <span>设备主档</span>
                      </div>
                      <div class="insight-detail-section__digest">
                        <span
                          v-for="item in deviceArchiveDigest"
                          :key="`device-${item.label}`"
                          class="insight-detail-section__digest-pill"
                        >
                          {{ item.label }} · {{ item.value }}
                        </span>
                      </div>
                    </header>
                    <dl class="insight-fact-list">
                      <div
                        v-for="item in deviceArchiveEntries"
                        :key="item.label"
                        class="insight-fact-row"
                      >
                        <dt>{{ item.label }}</dt>
                        <dd>{{ item.value }}</dd>
                      </div>
                    </dl>
                  </article>

                  <article class="insight-detail-section insight-detail-section--accent">
                    <header class="insight-detail-section__header">
                      <div class="insight-detail-section__title">
                        <strong>风险上下文档案</strong>
                        <span>风险运营口径</span>
                      </div>
                      <div class="insight-detail-section__digest">
                        <span
                          v-for="item in riskArchiveDigest"
                          :key="`risk-${item.label}`"
                          class="insight-detail-section__digest-pill insight-detail-section__digest-pill--accent"
                        >
                          {{ item.label }} · {{ item.value }}
                        </span>
                      </div>
                    </header>
                    <dl class="insight-fact-list">
                      <div
                        v-for="item in riskArchiveEntries"
                        :key="item.label"
                        class="insight-fact-row"
                      >
                        <dt>{{ item.label }}</dt>
                        <dd>{{ item.value }}</dd>
                      </div>
                    </dl>
                  </article>
                </div>
              </div>
            </PanelCard>

            <PanelCard
              title="综合分析"
              description="结合监测快照、设备状态和风险上下文形成可复述结论。"
            >
              <div class="insight-analysis-shell">
                <article v-if="analysisLead" class="insight-analysis-lead">
                  <header class="insight-analysis-lead__header">
                    <small>{{ analysisLead.tag }}</small>
                    <strong>{{ analysisLead.title }}</strong>
                  </header>
                  <p>{{ analysisLead.description }}</p>
                </article>

                <div class="insight-narrative-stack">
                  <article
                    v-for="paragraph in analysisSupportingBlocks"
                    :key="paragraph.title"
                    class="insight-narrative-item"
                  >
                    <header class="insight-narrative-item__header">
                      <div class="insight-narrative-item__heading">
                        <small>{{ paragraph.tag }}</small>
                        <strong>{{ paragraph.title }}</strong>
                      </div>
                    </header>
                    <p>{{ paragraph.description }}</p>
                  </article>
                </div>
              </div>
            </PanelCard>
          </section>

          <section
            v-if="hasCollectorChildren"
            class="collector-insight-bridge"
            aria-labelledby="collector-insight-bridge-title"
          >
            <header class="collector-insight-bridge__header">
              <div class="collector-insight-bridge__heading">
                <span class="collector-insight-bridge__eyebrow">采集器子设备诊断</span>
                <strong id="collector-insight-bridge-title">{{ collectorBridgeTitle }}</strong>
                <p>{{ collectorBridgeDescription }}</p>
              </div>
              <div class="collector-insight-bridge__meta">
                <span class="collector-insight-bridge__pill">{{ collectorOverview?.childCount ?? 0 }} 个子设备</span>
                <span class="collector-insight-bridge__pill">{{ collectorOverview?.reachableChildCount ?? 0 }} 个链路可达</span>
                <span
                  v-if="collectorAttentionCount > 0"
                  class="collector-insight-bridge__pill collector-insight-bridge__pill--attention"
                >
                  {{ collectorAttentionCount }} 个待关注
                </span>
                <span
                  v-if="(collectorOverview?.recommendedMetricCount ?? 0) > 0"
                  class="collector-insight-bridge__pill collector-insight-bridge__pill--brand"
                >
                  {{ collectorOverview?.recommendedMetricCount ?? 0 }} 个建议指标
                </span>
              </div>
            </header>

            <section
              v-if="collectorRecommendationNotice"
              class="collector-insight-bridge__notice"
              aria-live="polite"
            >
              <span class="collector-insight-bridge__notice-label">建议优先项</span>
              <p>{{ collectorRecommendationNotice }}</p>
            </section>

            <CollectorChildInsightPanel
              class="collector-insight-bridge__panel"
              :overview="collectorOverview!"
            />
          </section>

          <section
            class="runtime-insight-bridge"
            aria-labelledby="runtime-insight-bridge-title"
          >
            <header class="runtime-insight-bridge__header">
              <div class="runtime-insight-bridge__heading">
                <span class="runtime-insight-bridge__eyebrow">运行态诊断与治理</span>
                <strong id="runtime-insight-bridge-title">{{ runtimeInsightBridgeTitle }}</strong>
                <p>{{ runtimeInsightBridgeDescription }}</p>
              </div>
              <div class="runtime-insight-bridge__meta">
                <span class="runtime-insight-bridge__pill">{{ getRangeLabel(selectedRange) }}</span>
                <span class="runtime-insight-bridge__pill">{{ trendGroups.length }} 个趋势分组</span>
                <span class="runtime-insight-bridge__pill">{{ propertyTableRows.length }} 条快照属性</span>
                <span
                  v-if="editableSnapshotCount > 0"
                  class="runtime-insight-bridge__pill runtime-insight-bridge__pill--brand"
                >
                  {{ editableSnapshotCount }} 条可直达合同
                </span>
                <span
                  v-if="runtimeGovernanceSnapshotCount > 0"
                  class="runtime-insight-bridge__pill runtime-insight-bridge__pill--muted"
                >
                  {{ runtimeGovernanceSnapshotCount }} 条待补名称/单位
                </span>
              </div>
            </header>

            <section
              class="runtime-diagnosis-strip"
              :class="`runtime-diagnosis-strip--${runtimeDiagnosisConclusion.tone}`"
            >
              <div class="runtime-diagnosis-strip__copy">
                <span class="runtime-diagnosis-strip__eyebrow">本次诊断结论</span>
                <strong>{{ runtimeDiagnosisConclusion.title }}</strong>
                <p>{{ runtimeDiagnosisConclusion.description }}</p>
              </div>
              <div class="runtime-diagnosis-strip__aside">
                <span class="runtime-diagnosis-strip__pill">{{ runtimeDiagnosisConclusion.pill }}</span>
                <div v-if="runtimeDiagnosisActions.length" class="runtime-diagnosis-strip__actions">
                  <StandardButton
                    v-for="action in runtimeDiagnosisActions"
                    :key="`${action.kind}-${action.identifier}`"
                    action="query"
                    :link="action.emphasis === 'secondary'"
                    :data-testid="action.emphasis === 'primary' ? 'runtime-diagnosis-primary-action' : 'runtime-diagnosis-secondary-action'"
                    @click="handleRuntimeDiagnosisAction(action)"
                  >
                    {{ action.label }}
                  </StandardButton>
                </div>
              </div>
            </section>

            <section class="runtime-insight-bridge__sequence" aria-live="polite">
              <span class="runtime-insight-bridge__sequence-pill">排查顺序</span>
              <p>{{ runtimeInsightBridgeSequence }}</p>
            </section>

            <RiskInsightTrendPanel
              class="runtime-insight-bridge__trend"
              :range-code="selectedRange"
              :groups="trendGroups"
              :active-identifier="activeTrendFocus?.identifier || ''"
              :empty-message="trendEmptyMessage"
              @change-range="handleRangeChange"
              @select-series="handleTrendSeriesSelect"
            />

            <PanelCard
              class="runtime-insight-bridge__snapshot"
              title="设备属性快照"
              description="展示设备当前最新运行态值；若要修改正式字段名称、单位等定义，可直接跳到产品契约页处理。"
            >
              <div v-if="propertyTableRows.length" class="snapshot-workbench">
                <header class="snapshot-workbench__header">
                  <div class="snapshot-workbench__heading">
                    <strong>运行态快照</strong>
                    <p>{{ snapshotWorkbenchCopy }}</p>
                  </div>
                  <div class="snapshot-workbench__meta">
                    <span class="snapshot-workbench__pill">{{ propertyTableRows.length }} 条属性</span>
                    <span class="snapshot-workbench__pill">{{ editableSnapshotCount }} 条可直达合同</span>
                    <span class="snapshot-workbench__pill snapshot-workbench__pill--muted">{{ runtimeGovernanceSnapshotCount }} 条待补名称/单位</span>
                  </div>
                </header>

                <section
                  class="snapshot-workbench__focus"
                  :class="`snapshot-workbench__focus--${snapshotWorkbenchFocus.tone}`"
                >
                  <div class="snapshot-workbench__focus-copy">
                    <span class="snapshot-workbench__focus-eyebrow">治理焦点</span>
                    <strong>{{ snapshotWorkbenchFocus.title }}</strong>
                    <p>{{ snapshotWorkbenchFocus.description }}</p>
                  </div>
                  <span class="snapshot-workbench__focus-pill">{{ snapshotWorkbenchFocus.pill }}</span>
                </section>

                <div class="snapshot-workbench__table-shell">
                  <el-table
                    :data="propertyTableRows"
                    class="monitoring-snapshot-table"
                    stripe
                    table-layout="fixed"
                  >
                    <StandardTableTextColumn
                      prop="displayName"
                      label="属性"
                      secondary-prop="identifier"
                      :min-width="220"
                    />
                    <el-table-column label="当前读数" :min-width="176">
                      <template #default="{ row }">
                        <div
                          class="snapshot-reading-cell"
                          :class="{ 'snapshot-reading-cell--focused': isTrendFocusedRow(row) }"
                        >
                          <strong>{{ row.propertyValue }}</strong>
                          <small>{{ row.readingMeta }}</small>
                          <span
                            v-if="isTrendFocusedRow(row)"
                            class="snapshot-reading-cell__focus-badge"
                            :data-testid="buildPropertySnapshotFocusTestId(row.identifier)"
                          >
                            当前关注
                          </span>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column label="字段归属" :min-width="168">
                      <template #default="{ row }">
                        <div
                          class="snapshot-origin-cell"
                          :class="{ 'snapshot-origin-cell--focused': isTrendFocusedRow(row) }"
                        >
                          <span
                            class="snapshot-origin-cell__pill"
                            :class="row.canEditFormalField ? 'snapshot-origin-cell__pill--formal' : 'snapshot-origin-cell__pill--runtime'"
                          >
                            {{ row.governanceLabel }}
                          </span>
                          <small>{{ resolveSnapshotGovernanceHint(row) }}</small>
                        </div>
                      </template>
                    </el-table-column>
                    <StandardTableTextColumn prop="displayTime" label="最近更新时间" :min-width="180" />
                    <el-table-column label="治理操作" :min-width="180" fixed="right">
                      <template #default="{ row }">
                        <div
                          class="snapshot-action-cell"
                          :class="{ 'snapshot-action-cell--focused': isTrendFocusedRow(row) }"
                        >
                          <StandardButton
                            v-if="row.canEditFormalField"
                            v-permission="'iot:products:update'"
                            action="query"
                            link
                            :data-testid="buildPropertySnapshotEditTestId(row.formalIdentifier || row.identifier)"
                            @click="handleEditFormalField(row)"
                          >
                            修改名称/单位
                          </StandardButton>
                          <StandardButton
                            v-else
                            v-permission="'iot:product-contract:govern'"
                            action="query"
                            link
                            :data-testid="`promote-mapping-rule-${row.identifier}`"
                            @click="handlePromoteToMappingRule(row)"
                          >
                            补名称/单位
                          </StandardButton>
                          <small class="monitoring-snapshot-table__action-hint">
                            {{ resolveSnapshotActionHint(row) }}
                          </small>
                        </div>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
              <div v-else class="empty-state">{{ snapshotEmptyMessage }}</div>
            </PanelCard>
          </section>
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
import { getCollectorChildInsightOverview, getDeviceByCode, getDeviceProperties, getDeviceTopologyRole } from '@/api/iot';
import { productApi } from '@/api/product';
import CollectorChildInsightPanel from '@/components/device/CollectorChildInsightPanel.vue';
import PanelCard from '@/components/PanelCard.vue';
import RiskInsightTrendPanel from '@/components/RiskInsightTrendPanel.vue';
import StandardInlineState from '@/components/StandardInlineState.vue';
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardPageShell from '@/components/StandardPageShell.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue';
import type { CollectorChildInsightOverview, Device, DeviceProperty, DeviceTopologyRole, ProductModel } from '@/types/api';
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
import { buildProductWorkbenchSectionPath } from '@/utils/productWorkbenchRoutes';

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

interface PropertySnapshotRow extends Partial<DeviceProperty> {
  identifier: string;
  propertyValue: string;
  valueType: string;
  displayName: string;
  displayUnit: string;
  displayTime: string;
  readingMeta: string;
  formalIdentifier: string;
  canEditFormalField: boolean;
  governanceLabel: string;
  governanceHint: string;
}

interface TrendFocusSelection {
  groupKey: string;
  groupTitle: string;
  identifier: string;
  displayName: string;
}

interface RuntimeDiagnosisAction {
  kind: 'formal' | 'runtime';
  label: string;
  identifier: string;
  emphasis: 'primary' | 'secondary';
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
const topologyRole = ref<DeviceTopologyRole | null>(null);
const riskBindings = ref<RiskMonitoringListItem[]>([]);
const riskDetail = ref<RiskMonitoringDetail | null>(null);
const capabilityProfile = ref<InsightCapabilityProfile>(getInsightCapabilityProfile({}));
const trendGroups = ref<InsightTrendGroup[]>([]);
const activeTrendFocus = ref<TrendFocusSelection | null>(null);
const productModelDisplayNameMap = ref<Map<string, string>>(new Map());
const productModelDataTypeMap = ref<Map<string, string>>(new Map());
const productModelUnitMap = ref<Map<string, string>>(new Map());
const productPropertyIdentifierSet = ref<Set<string>>(new Set());
const lastFetchTime = ref<string | null>(null);
const requestVersion = ref(0);
let syncingRoute = false;

const normalizedDeviceCode = computed(() => deviceCode.value.trim());
const isCollectorParentInsight = computed(() => topologyRole.value === 'COLLECTOR_PARENT');
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

const insightEntryState = computed(() => {
  if (isLoading.value) {
    return {
      tone: 'loading',
      eyebrow: '分析进行中',
      title: `正在整理 ${normalizedDeviceCode.value || '当前设备'} 的洞察线索`,
      description: '我们正在串起设备档案、latest 快照、风险上下文和趋势样本，首屏内容准备好后会自然展开。',
      pills: ['设备档案', 'latest 快照', '趋势样本']
    };
  }
  if (!normalizedDeviceCode.value) {
    return {
      tone: 'idle',
      eyebrow: '开始分析',
      title: '请输入设备编码后开始综合分析',
      description: '我们会按同一条链路整理设备档案、风险上下文、属性快照和趋势样本，帮助你更快判断下一步排查方向。',
      pills: ['基础档案', '属性快照', '趋势样本']
    };
  }
  return {
    tone: 'empty',
    eyebrow: '暂无洞察内容',
    title: `设备 ${normalizedDeviceCode.value} 还没有形成可展示的洞察首屏`,
    description: '先检查设备是否持续上报、latest 属性是否写入成功，以及趋势样本是否已进入历史窗口，再回来继续分析。',
    pills: ['检查上报', '检查 latest', '检查趋势']
  };
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

const configuredMetricMap = computed(() => {
  const map = new Map<string, InsightCapabilityProfile['customMetrics'][number]>();
  capabilityProfile.value.customMetrics.forEach((metric) => {
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
          metric.displayName,
          property?.propertyName,
          resolveProductModelValue(productModelDisplayNameMap.value, metric.identifier)
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

const collectorAttentionCount = computed(
  () => (collectorOverview.value?.missingChildCount ?? 0) + (collectorOverview.value?.staleChildCount ?? 0)
);
const collectorBridgeTitle = computed(() => {
  const childCount = collectorOverview.value?.childCount ?? 0;
  if (collectorAttentionCount.value > 0) {
    return `先从 ${collectorAttentionCount.value} 个待关注子设备开始看链路和状态，再决定是否继续下钻。`;
  }
  return `先确认 ${childCount} 个子设备的链路和最近指标，再决定是否进入单设备对象洞察。`;
});
const collectorBridgeDescription = computed(() => {
  if ((collectorOverview.value?.recommendedMetricCount ?? 0) > 0) {
    return '这一段先回答哪些子设备可达、哪些状态缺失，以及哪些指标已经值得优先纳入对象洞察。';
  }
  return '这一段先收住采集器下挂子设备的链路、状态和最近指标，避免中段像突然插进另一块独立模块。';
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

const deviceArchiveDigest = computed(() => [
  { label: '部署位置', value: device.value?.address || '--' },
  { label: '接入协议', value: device.value?.protocolCode || '--' },
  { label: '最近上报', value: formatDateTime(device.value?.lastReportTime) }
]);

const riskArchiveDigest = computed(() => [
  { label: '风险态势', value: riskDetail.value ? getRiskLevelLabel(riskDetail.value.riskLevel) : '未纳入风险监测' },
  { label: '主监测项', value: riskDetail.value?.metricName || '未命名指标' },
  { label: '绑定状态', value: riskDetail.value ? '已纳入风险监测' : '当前未纳管' }
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

const analysisLead = computed(() => analysisParagraphs.value[0] ?? null);

const analysisSupportingBlocks = computed(() => analysisParagraphs.value.slice(1));

const propertyTableRows = computed<PropertySnapshotRow[]>(() => {
  const orderedIdentifiers = dedupeSnapshotIdentifiers([
    ...Array.from(configuredSnapshotMetricMap.value.keys()),
    ...properties.value.map((item) => item.identifier)
  ]);

  return orderedIdentifiers.map((identifier) => {
    const property = propertyMap.value.get(identifier);
    const series = trendSeriesMap.value.get(identifier);
    const configuredMetric = configuredSnapshotMetricMap.value.get(identifier);
    const latestActualBucket = resolveLatestActualTrendBucket(series);
    const formalIdentifier = resolveFormalPropertyIdentifier(identifier);
    const resolvedUnit = resolveMetricUnit(identifier, property) || '--';
    const resolvedValueType = normalizeText(property?.valueType) || resolveProductModelValue(productModelDataTypeMap.value, identifier) || '--';
    const canEditFormalField = Boolean(device.value?.productId && formalIdentifier);

    return {
      ...(property ?? {}),
      identifier,
      propertyValue: normalizeText(property?.propertyValue) || (latestActualBucket ? String(latestActualBucket.value) : '--'),
      valueType: resolvedValueType,
      displayName: resolveMetricBaseName(
        identifier,
        property?.propertyName,
        configuredMetric?.displayName,
        series?.displayName,
        resolveProductModelValue(productModelDisplayNameMap.value, identifier)
      ),
      displayUnit: resolvedUnit,
      displayTime: formatDateTime(property?.updateTime || property?.reportTime || latestActualBucket?.time),
      readingMeta: buildSnapshotReadingMeta(resolvedValueType, resolvedUnit),
      formalIdentifier,
      canEditFormalField,
      governanceLabel: canEditFormalField ? '正式字段' : '运行态字段',
      governanceHint: canEditFormalField ? '可回到合同页修改名称与单位' : '建议补齐名称与单位后再沉淀'
    };
  });
});

const activeTrendCanonicalIdentifier = computed(() =>
  normalizeText(resolveSnapshotCanonicalIdentifier(activeTrendFocus.value?.identifier ?? '')).toLowerCase()
);

const focusedSnapshotRows = computed(() =>
  propertyTableRows.value.filter((row) => isTrendFocusMatchedRow(row))
);

const editableSnapshotCount = computed(() =>
  propertyTableRows.value.filter((row) => row.canEditFormalField).length
);

const runtimeGovernanceSnapshotCount = computed(() =>
  propertyTableRows.value.length - editableSnapshotCount.value
);

const snapshotWorkbenchCopy = computed(() => {
  if (!propertyTableRows.value.length) {
    return '当前设备暂无可展示的运行态快照。';
  }
  if (runtimeGovernanceSnapshotCount.value === 0) {
    return '当前快照字段都已命中正式合同，可直接回到正式字段治理。';
  }
  if (editableSnapshotCount.value === 0) {
    return '当前快照字段仍以运行态为主，建议优先补齐名称与单位治理。';
  }
  return `当前快照同时包含 ${editableSnapshotCount.value} 条正式字段和 ${runtimeGovernanceSnapshotCount.value} 条运行态字段。`;
});

const runtimeInsightBridgeTitle = computed(() => {
  if (trendGroups.value.length && propertyTableRows.value.length) {
    return `先看 ${getRangeLabel(selectedRange.value)} 的变化，再核对 ${propertyTableRows.value.length} 条最新快照和治理入口。`;
  }
  if (trendGroups.value.length) {
    return `先看 ${getRangeLabel(selectedRange.value)} 的趋势样本，再回头确认 latest 快照为何还没站住。`;
  }
  if (propertyTableRows.value.length) {
    return `这台设备当前更适合先从 ${propertyTableRows.value.length} 条运行态快照入手，再决定是否回查历史趋势。`;
  }
  return '运行态诊断会先回答趋势变化，再把字段治理入口收口到同一段里。';
});

const runtimeInsightBridgeDescription = computed(() => {
  if (trendGroups.value.length && editableSnapshotCount.value > 0) {
    return '上面先看时间范围内的变化，下面直接核对哪些字段已经命中正式合同，哪些仍要走运行态治理。';
  }
  if (runtimeGovernanceSnapshotCount.value > 0) {
    return '这一段会把趋势样本、latest 快照和运行态名称/单位治理放在同一条链路里，减少“先看哪块”的犹豫。';
  }
  return '这一段把历史变化和当前快照放在一个工作区里，先帮助我们判断现象，再决定下一步治理动作。';
});

const runtimeInsightBridgeSequence = computed(() => {
  if (trendGroups.value.length && propertyTableRows.value.length) {
    return '先看趋势里哪些指标真的在变，再回到快照核对字段归属、名称单位和正式合同是否对齐。';
  }
  if (trendGroups.value.length) {
    return '当前先从趋势判断现象，再确认 latest 快照为什么还没有形成可治理的字段台账。';
  }
  if (propertyTableRows.value.length) {
    return '当前先从 latest 快照核对字段语义和治理入口，等趋势样本补齐后再回看时间变化。';
  }
  return '当前还没有足够的趋势或快照样本，先把设备上报链路和 latest 写入链路核实清楚。';
});

const snapshotWorkbenchFocus = computed(() => {
  if (!propertyTableRows.value.length) {
    return {
      tone: 'idle',
      pill: '暂无快照',
      title: '等待设备上报最新运行态样本',
      description: '先确认设备是否持续上报，再回到属性快照定位字段治理入口。'
    };
  }
  if (runtimeGovernanceSnapshotCount.value === 0) {
    return {
      tone: 'formal',
      pill: '合同优先',
      title: '当前快照已和正式合同对齐',
      description: '这批字段都能直达合同页修改名称与单位，适合直接沿正式字段继续排查。'
    };
  }
  if (editableSnapshotCount.value === 0) {
    return {
      tone: 'runtime',
      pill: '运行态优先',
      title: '先补齐运行态名称与单位',
      description: '当前快照主要来自运行态字段，先把字段语义补齐，后续趋势与治理动作会更顺。'
    };
  }
  return {
    tone: 'hybrid',
    pill: '双轨治理',
    title: '正式字段与运行态字段同时存在',
    description: '先沿正式字段排查关键读数，再补齐运行态字段的名称与单位，能更快收拢对象语义。'
  };
});

const runtimeDiagnosisConclusion = computed(() => {
  const focusedMetricName = activeTrendFocus.value?.displayName?.trim();
  if (focusedMetricName && focusedSnapshotRows.value.length) {
    const canEditFormalField = focusedSnapshotRows.value.some((row) => row.canEditFormalField);
    if (canEditFormalField) {
      return {
        tone: 'formal',
        pill: `${focusedSnapshotRows.value.length} 条快照已命中`,
        title: `先核对 ${focusedMetricName} 的正式字段定义`,
        description: '当前趋势已经锁定到对应快照字段，适合先核对 latest 读数、名称单位和正式合同定义，再决定是否继续回查历史样本。'
      };
    }
    return {
      tone: 'runtime',
      pill: `${focusedSnapshotRows.value.length} 条运行态待治理`,
      title: `先补齐 ${focusedMetricName} 的运行态语义`,
      description: '当前趋势已经命中运行态快照，但还没有直达正式合同的入口。建议先补名称和单位，再继续看后续趋势是否稳定。'
    };
  }
  if (focusedMetricName) {
    return {
      tone: 'attention',
      pill: '快照未命中',
      title: `趋势里正在看 ${focusedMetricName}，但 latest 快照还没接住`,
      description: '这通常意味着样本只出现在历史窗口里，或者当前 latest 字段还没完成归一。更适合先核对上报标识和 latest 写入链路。'
    };
  }
  if (snapshotWorkbenchFocus.value.tone === 'formal') {
    return {
      tone: 'formal',
      pill: '合同优先',
      title: '这批快照已经站到正式合同上了',
      description: '当前更适合直接沿正式字段继续排查，把重点放在 latest 读数是否符合预期，以及名称单位定义是否足够清楚。'
    };
  }
  if (snapshotWorkbenchFocus.value.tone === 'runtime') {
    return {
      tone: 'runtime',
      pill: '运行态优先',
      title: '这台设备现在更该先补运行态字段语义',
      description: '趋势和快照都还在运行态侧打转，先把字段名称、单位和归属补清楚，后面的合同治理和趋势判断会顺很多。'
    };
  }
  return {
    tone: 'neutral',
    pill: '先看样本',
    title: '先用趋势锁定现象，再回到快照判断治理入口',
    description: '先点击一条真正关心的趋势指标，我们会把对应快照字段和下一步治理动作一起高亮出来。'
  };
});

const runtimeDiagnosisActions = computed<RuntimeDiagnosisAction[]>(() => {
  const actions: RuntimeDiagnosisAction[] = [];
  const focusedFormalRow = focusedSnapshotRows.value.find((row) => row.canEditFormalField);
  const focusedRuntimeRow = focusedSnapshotRows.value.find((row) => !row.canEditFormalField);
  if (focusedFormalRow) {
    actions.push({
      kind: 'formal',
      label: '去正式字段治理',
      identifier: focusedFormalRow.formalIdentifier || focusedFormalRow.identifier,
      emphasis: 'primary'
    });
  }
  if (!focusedFormalRow && focusedRuntimeRow) {
    actions.push({
      kind: 'runtime',
      label: '去运行态治理',
      identifier: focusedRuntimeRow.identifier,
      emphasis: 'primary'
    });
  }
  if (focusedFormalRow && focusedRuntimeRow) {
    actions.push({
      kind: 'runtime',
      label: '补运行态名称/单位',
      identifier: focusedRuntimeRow.identifier,
      emphasis: 'secondary'
    });
  }
  if (actions.length) {
    return actions;
  }

  const firstFormalRow = propertyTableRows.value.find((row) => row.canEditFormalField);
  const firstRuntimeRow = propertyTableRows.value.find((row) => !row.canEditFormalField);
  if (snapshotWorkbenchFocus.value.tone === 'formal' && firstFormalRow) {
    return [{
      kind: 'formal',
      label: '去正式字段治理',
      identifier: firstFormalRow.formalIdentifier || firstFormalRow.identifier,
      emphasis: 'primary'
    }];
  }
  if (snapshotWorkbenchFocus.value.tone === 'runtime' && firstRuntimeRow) {
    return [{
      kind: 'runtime',
      label: '去运行态治理',
      identifier: firstRuntimeRow.identifier,
      emphasis: 'primary'
    }];
  }
  if (snapshotWorkbenchFocus.value.tone === 'hybrid' && firstFormalRow && firstRuntimeRow) {
    return [
      {
        kind: 'formal',
        label: '先看正式字段',
        identifier: firstFormalRow.formalIdentifier || firstFormalRow.identifier,
        emphasis: 'primary'
      },
      {
        kind: 'runtime',
        label: '再补运行态语义',
        identifier: firstRuntimeRow.identifier,
        emphasis: 'secondary'
      }
    ];
  }
  if (activeTrendFocus.value?.identifier && device.value?.productId) {
    return [{
      kind: 'runtime',
      label: '去运行态治理',
      identifier: activeTrendFocus.value.identifier,
      emphasis: 'primary'
    }];
  }
  return [];
});

function buildPropertySnapshotFocusTestId(identifier: string) {
  return `snapshot-focus-${normalizeText(identifier).replace(/[^0-9A-Za-z]+/g, '_')}`;
}

function isTrendFocusMatchedRow(row: PropertySnapshotRow) {
  if (!activeTrendCanonicalIdentifier.value) {
    return false;
  }
  return [row.identifier, row.formalIdentifier]
    .map((value) => normalizeText(resolveSnapshotCanonicalIdentifier(value)).toLowerCase())
    .filter(Boolean)
    .includes(activeTrendCanonicalIdentifier.value);
}

function isTrendFocusedRow(row: PropertySnapshotRow) {
  return isTrendFocusMatchedRow(row);
}

function resolveSnapshotGovernanceHint(row: PropertySnapshotRow) {
  if (isTrendFocusedRow(row)) {
    return row.canEditFormalField
      ? '当前趋势已经锁定这里，建议先核对正式字段的名称、单位和趋势定义。'
      : '当前趋势已经锁定这里，建议先补齐运行态名称/单位，再确认是否需要沉淀成正式字段。';
  }
  return row.governanceHint;
}

function resolveSnapshotActionHint(row: PropertySnapshotRow) {
  if (isTrendFocusedRow(row)) {
    return '当前趋势正在看这里';
  }
  return row.canEditFormalField ? '直达正式字段治理' : '进入运行态名称/单位治理';
}

function handleTrendSeriesSelect(selection: TrendFocusSelection) {
  const normalizedIdentifier = normalizeText(selection.identifier);
  if (!normalizedIdentifier) {
    return;
  }
  activeTrendFocus.value = {
    ...selection,
    identifier: normalizedIdentifier
  };
}

function resolveTrendFocusSelection(identifier: string) {
  const normalizedIdentifier = normalizeText(resolveSnapshotCanonicalIdentifier(identifier)).toLowerCase();
  if (!normalizedIdentifier) {
    return null;
  }
  for (const group of trendGroups.value) {
    for (const series of group.series) {
      const seriesIdentifier = normalizeText(resolveSnapshotCanonicalIdentifier(series.identifier ?? '')).toLowerCase();
      if (seriesIdentifier && seriesIdentifier === normalizedIdentifier) {
        return {
          groupKey: group.key,
          groupTitle: group.title,
          identifier: series.identifier ?? identifier,
          displayName: series.displayName
        } satisfies TrendFocusSelection;
      }
    }
  }
  return null;
}

function syncActiveTrendFocus() {
  if (!trendGroups.value.length) {
    activeTrendFocus.value = null;
    return;
  }
  const preserved = activeTrendFocus.value ? resolveTrendFocusSelection(activeTrendFocus.value.identifier) : null;
  if (preserved) {
    activeTrendFocus.value = preserved;
    return;
  }
  const riskMetricIdentifier = normalizeText(riskDetail.value?.metricIdentifier);
  if (riskMetricIdentifier) {
    const riskFocus = resolveTrendFocusSelection(riskMetricIdentifier);
    if (riskFocus) {
      activeTrendFocus.value = riskFocus;
      return;
    }
  }
  const firstSeries = trendGroups.value.flatMap((group) =>
    group.series.map((series) => ({
      groupKey: group.key,
      groupTitle: group.title,
      identifier: series.identifier ?? '',
      displayName: series.displayName
    }))
  ).find((item) => item.identifier);
  activeTrendFocus.value = firstSeries ? {
    ...firstSeries,
    identifier: normalizeText(firstSeries.identifier)
  } : null;
}

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

function handleEditFormalField(row: PropertySnapshotRow) {
  const productId = device.value?.productId;
  const formalIdentifier = normalizeText(row.formalIdentifier);
  if (productId === undefined || productId === null || !formalIdentifier) {
    return;
  }
  void router.push({
    path: buildProductWorkbenchSectionPath(productId, 'contracts'),
    query: {
      modelIdentifier: formalIdentifier,
      renameModel: '1',
      source: 'insight'
    }
  });
}

function handlePromoteToMappingRule(row: PropertySnapshotRow) {
  const productId = device.value?.productId;
  if (productId === undefined || productId === null) {
    return;
  }
  void router.push({
    path: buildProductWorkbenchSectionPath(productId, 'mapping-rules'),
    query: {
      rawIdentifier: row.identifier,
      scope: 'PRODUCT',
      source: 'insight'
    }
  });
}

function handleRuntimeDiagnosisAction(action: RuntimeDiagnosisAction) {
  const productId = device.value?.productId;
  if (productId === undefined || productId === null) {
    return;
  }
  if (action.kind === 'formal') {
    const targetRow = propertyTableRows.value.find((row) =>
      normalizeText(row.formalIdentifier || row.identifier) === normalizeText(action.identifier)
    );
    if (targetRow) {
      handleEditFormalField(targetRow);
      return;
    }
    void router.push({
      path: buildProductWorkbenchSectionPath(productId, 'contracts'),
      query: {
        modelIdentifier: normalizeText(action.identifier),
        renameModel: '1',
        source: 'insight'
      }
    });
    return;
  }
  const targetRow = propertyTableRows.value.find((row) =>
    normalizeText(row.identifier) === normalizeText(action.identifier)
  );
  if (targetRow) {
    handlePromoteToMappingRule(targetRow);
    return;
  }
  void router.push({
    path: buildProductWorkbenchSectionPath(productId, 'mapping-rules'),
    query: {
      rawIdentifier: normalizeText(action.identifier),
      scope: 'PRODUCT',
      source: 'insight'
    }
  });
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

    const [propertyResponse, topologyRoleResponse, bindingResponse, productInsightSupplement] = await Promise.all([
      getDeviceProperties(code),
      getDeviceTopologyRole(code),
      getRiskMonitoringList({
        deviceCode: code,
        pageNum: 1,
        pageSize: 50
      }),
      loadProductInsightSupplement(deviceResponse.data?.productId)
    ]);
    if (version !== requestVersion.value) {
      return;
    }

    properties.value = propertyResponse.data ?? [];
    topologyRole.value = topologyRoleResponse.data ?? 'STANDALONE';
    riskBindings.value = bindingResponse.data.records ?? [];
    productModelDisplayNameMap.value = productInsightSupplement.modelDisplayNameMap;
    productModelDataTypeMap.value = productInsightSupplement.modelDataTypeMap;
    productModelUnitMap.value = productInsightSupplement.modelUnitMap;
    productPropertyIdentifierSet.value = productInsightSupplement.propertyIdentifierSet;
    if (shouldLoadCollectorInsightOverview(topologyRole.value)) {
      collectorOverview.value = (await getCollectorChildInsightOverview(code).catch((error) => {
        console.warn('采集器子设备总览加载失败', error);
        return null;
      }))?.data ?? null;
      if (version !== requestVersion.value) {
        return;
      }
    } else {
      collectorOverview.value = null;
    }

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
      topologyRole: mapTopologyRoleToCapabilityRole(topologyRole.value),
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
        syncActiveTrendFocus();
      } catch (error) {
        trendGroups.value = [];
        activeTrendFocus.value = null;
        trendErrorMessage.value = error instanceof Error ? error.message : 'TDengine 趋势查询失败';
      }
    } else {
      trendGroups.value = [];
      activeTrendFocus.value = null;
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
  topologyRole.value = null;
  riskBindings.value = [];
  riskDetail.value = null;
  trendGroups.value = [];
  activeTrendFocus.value = null;
  productModelDisplayNameMap.value = new Map();
  productModelDataTypeMap.value = new Map();
  productModelUnitMap.value = new Map();
  productPropertyIdentifierSet.value = new Set();
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
                profileDisplayName,
                property?.propertyName,
                point.displayName,
                resolveProductModelValue(productModelDisplayNameMap.value, identifier)
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

function shouldLoadCollectorInsightOverview(role?: DeviceTopologyRole | null) {
  return role === 'COLLECTOR_PARENT';
}

function mapTopologyRoleToCapabilityRole(
  role?: DeviceTopologyRole | null
): 'collector_parent' | 'collector_child' | 'standalone' | undefined {
  if (role === 'COLLECTOR_PARENT') {
    return 'collector_parent';
  }
  if (role === 'COLLECTOR_CHILD') {
    return 'collector_child';
  }
  if (role === 'STANDALONE') {
    return 'standalone';
  }
  return undefined;
}

async function loadProductInsightSupplement(productId?: string | number | null) {
  const emptySupplement = {
    metadataJson: null as string | null,
    modelDisplayNameMap: new Map<string, string>(),
    modelDataTypeMap: new Map<string, string>(),
    modelUnitMap: new Map<string, string>(),
    propertyIdentifierSet: new Set<string>()
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
      ),
      propertyIdentifierSet: buildProductPropertyIdentifierSet(
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

function buildProductPropertyIdentifierSet(models: ProductModel[]) {
  return new Set(
    models
      .filter((model) => model.modelType === 'property')
      .map((model) => normalizeText(model.identifier))
      .filter(Boolean)
  );
}

function resolveProductModelUnit(model: ProductModel) {
  return normalizeText(parseSpecsJson(model.specsJson)?.unit);
}

function resolveMetricBaseName(identifier: string, ...candidates: Array<unknown>) {
  let rawFallback = '';
  for (const candidate of candidates) {
    const normalizedCandidate = normalizeText(candidate);
    if (!normalizedCandidate) {
      continue;
    }
    const resolved = resolveInsightMetricDisplayName(identifier, normalizedCandidate);
    if (resolved) {
      if (isAliasLikeMetricLabel(resolved, identifier)) {
        rawFallback = rawFallback || resolved;
        continue;
      }
      return resolved;
    }
  }
  return rawFallback || resolveInsightMetricDisplayName(identifier);
}

function resolveMetricUnit(identifier: string, property?: DeviceProperty | null) {
  return normalizeText(property?.unit)
    || resolveConfiguredMetricValue(configuredMetricMap.value, identifier, (metric) => normalizeText(metric.unit))
    || resolveProductModelValue(productModelUnitMap.value, identifier)
    || '';
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
  const compatibleEntries = Array.from(map.entries()).filter(([key]) =>
    isCompatibleProductModelIdentifier(key, identifier)
  );
  if (compatibleEntries.length === 1) {
    return compatibleEntries[0]?.[1] || '';
  }
  return '';
}

function resolveConfiguredMetricValue(
  map: Map<string, InsightCapabilityProfile['customMetrics'][number]>,
  identifier: string,
  selector: (metric: InsightCapabilityProfile['customMetrics'][number]) => string
) {
  const exactMetric = map.get(identifier);
  const exactValue = exactMetric ? selector(exactMetric) : '';
  if (exactValue) {
    return exactValue;
  }

  const normalizedIdentifier = identifier.trim().toLowerCase();
  for (const [key, metric] of map.entries()) {
    if (key.trim().toLowerCase() === normalizedIdentifier) {
      const resolvedValue = selector(metric);
      if (resolvedValue) {
        return resolvedValue;
      }
    }
  }

  const compatibleEntries = Array.from(map.entries()).filter(([key]) =>
    isCompatibleProductModelIdentifier(key, identifier)
  );
  if (compatibleEntries.length === 1) {
    return selector(compatibleEntries[0][1]);
  }

  return '';
}

function resolveFormalPropertyIdentifier(identifier: string) {
  const normalizedIdentifier = normalizeText(identifier);
  if (!normalizedIdentifier) {
    return '';
  }
  const exactIdentifier = findCaseInsensitiveIdentifier(productPropertyIdentifierSet.value, normalizedIdentifier);
  if (exactIdentifier) {
    return exactIdentifier;
  }
  const compatibleIdentifiers = Array.from(productPropertyIdentifierSet.value).filter((candidate) =>
    isSnapshotAliasCompatibleIdentifier(candidate, normalizedIdentifier)
  );
  return compatibleIdentifiers.length === 1 ? compatibleIdentifiers[0] ?? '' : '';
}

function dedupeSnapshotIdentifiers(values: string[]) {
  const dedupedIdentifiers: string[] = [];
  const indexByCanonicalIdentifier = new Map<string, number>();
  values.forEach((value) => {
    const identifier = normalizeText(value);
    if (!identifier) {
      return;
    }
    const canonicalIdentifier = resolveSnapshotCanonicalIdentifier(identifier).toLowerCase();
    const existingIndex = indexByCanonicalIdentifier.get(canonicalIdentifier);
    if (existingIndex === undefined) {
      indexByCanonicalIdentifier.set(canonicalIdentifier, dedupedIdentifiers.length);
      dedupedIdentifiers.push(identifier);
      return;
    }
    const existingIdentifier = dedupedIdentifiers[existingIndex];
    if (shouldPreferSnapshotIdentifier(identifier, existingIdentifier)) {
      dedupedIdentifiers[existingIndex] = identifier;
    }
  });
  return dedupedIdentifiers;
}

function shouldPreferSnapshotIdentifier(candidateIdentifier: string, currentIdentifier: string) {
  const candidateScore = scoreSnapshotIdentifier(candidateIdentifier);
  const currentScore = scoreSnapshotIdentifier(currentIdentifier);
  if (candidateScore !== currentScore) {
    return candidateScore > currentScore;
  }
  return normalizeText(candidateIdentifier).length > normalizeText(currentIdentifier).length;
}

function scoreSnapshotIdentifier(identifier: string) {
  const normalizedIdentifier = normalizeText(identifier);
  if (!normalizedIdentifier) {
    return Number.MIN_SAFE_INTEGER;
  }
  let score = 0;
  if (normalizeText(resolveSnapshotCanonicalIdentifier(normalizedIdentifier)).toLowerCase() === normalizedIdentifier.toLowerCase()) {
    score += 100;
  }
  if (isManagedSnapshotIdentifier(normalizedIdentifier)) {
    score += 40;
  }
  if (normalizedIdentifier.includes('.')) {
    score += 10;
  }
  const propertyName = propertyMap.value.get(normalizedIdentifier)?.propertyName;
  if (propertyName && !isAliasLikeMetricLabel(propertyName, normalizedIdentifier)) {
    score += 5;
  }
  return score;
}

function resolveSnapshotCanonicalIdentifier(identifier: string) {
  const normalizedIdentifier = normalizeText(identifier);
  if (!normalizedIdentifier) {
    return '';
  }

  const managedIdentifiers = resolveManagedSnapshotIdentifiers();
  const exactManagedIdentifier = findCaseInsensitiveIdentifier(managedIdentifiers, normalizedIdentifier);
  if (exactManagedIdentifier) {
    return exactManagedIdentifier;
  }
  const propertyIdentifiers = uniqueIdentifiers(properties.value.map((item) => item.identifier));
  const mirroredSensorStateIdentifier = resolveMirroredSensorStateIdentifier(normalizedIdentifier, [
    ...managedIdentifiers,
    ...propertyIdentifiers
  ]);
  if (mirroredSensorStateIdentifier) {
    return mirroredSensorStateIdentifier;
  }

  const compatibleManagedIdentifiers = managedIdentifiers.filter((candidate) =>
    isSnapshotAliasCompatibleIdentifier(candidate, normalizedIdentifier)
  );
  if (compatibleManagedIdentifiers.length === 1) {
    return compatibleManagedIdentifiers[0] ?? normalizedIdentifier;
  }

  const exactPropertyIdentifier = findCaseInsensitiveIdentifier(propertyIdentifiers, normalizedIdentifier);
  if (exactPropertyIdentifier?.includes('.')) {
    return exactPropertyIdentifier;
  }

  const compatibleFullPathIdentifiers = propertyIdentifiers.filter((candidate) =>
    candidate.includes('.') && isSnapshotAliasCompatibleIdentifier(candidate, normalizedIdentifier)
  );
  if (compatibleFullPathIdentifiers.length === 1) {
    return compatibleFullPathIdentifiers[0] ?? normalizedIdentifier;
  }

  return exactPropertyIdentifier || normalizedIdentifier;
}

function resolveMirroredSensorStateIdentifier(identifier: string, candidates: string[]) {
  const normalizedIdentifier = normalizeText(identifier);
  if (!normalizedIdentifier || normalizedIdentifier.includes('.')) {
    return '';
  }
  return findCaseInsensitiveIdentifier(candidates, `S1_ZT_1.sensor_state.${normalizedIdentifier}`);
}

function resolveManagedSnapshotIdentifiers() {
  return uniqueIdentifiers([
    ...Array.from(configuredSnapshotMetricMap.value.keys()),
    ...Array.from(productPropertyIdentifierSet.value)
  ]);
}

function isManagedSnapshotIdentifier(identifier: string) {
  return Boolean(findCaseInsensitiveIdentifier(resolveManagedSnapshotIdentifiers(), identifier));
}

function findCaseInsensitiveIdentifier(values: Iterable<string>, target: string) {
  const normalizedTarget = normalizeText(target).toLowerCase();
  if (!normalizedTarget) {
    return '';
  }
  for (const value of values) {
    const normalizedValue = normalizeText(value);
    if (normalizedValue && normalizedValue.toLowerCase() === normalizedTarget) {
      return normalizedValue;
    }
  }
  return '';
}

function buildPropertySnapshotEditTestId(identifier: string) {
  return `property-snapshot-edit-${normalizeText(identifier).replace(/[^0-9A-Za-z]+/g, '_')}`;
}

function isSnapshotAliasCompatibleIdentifier(candidateIdentifier: string, targetIdentifier: string) {
  const normalizedCandidate = normalizeText(candidateIdentifier).toLowerCase();
  const normalizedTarget = normalizeText(targetIdentifier).toLowerCase();
  if (!normalizedCandidate || !normalizedTarget) {
    return false;
  }
  if (normalizedCandidate === normalizedTarget) {
    return true;
  }
  const candidateSegments = normalizedCandidate.split('.');
  const targetSegments = normalizedTarget.split('.');
  if (candidateSegments.length === 1 && targetSegments.length === 2) {
    return targetSegments[1] === normalizedCandidate;
  }
  if (candidateSegments.length === 2 && targetSegments.length === 1) {
    return candidateSegments[1] === normalizedTarget;
  }
  return false;
}

function isCompatibleProductModelIdentifier(candidateIdentifier: string, targetIdentifier: string) {
  const normalizedCandidate = normalizeText(candidateIdentifier).toLowerCase();
  const normalizedTarget = normalizeText(targetIdentifier).toLowerCase();
  if (!normalizedCandidate || !normalizedTarget) {
    return false;
  }
  if (normalizedCandidate === normalizedTarget) {
    return true;
  }
  const targetTail = normalizedTarget.split('.').pop() || normalizedTarget;
  return normalizedCandidate === targetTail
    || normalizedCandidate.endsWith(`.${targetTail}`)
    || normalizedTarget.endsWith(`.${normalizedCandidate}`);
}

function isAliasLikeMetricLabel(label: string, identifier: string) {
  const normalizedLabel = normalizeText(label);
  const normalizedIdentifier = normalizeText(identifier);
  if (!normalizedLabel) {
    return true;
  }
  if (!normalizedIdentifier) {
    return false;
  }
  if (normalizedLabel.trim().toLowerCase() === normalizedIdentifier.trim().toLowerCase()) {
    return true;
  }
  if (/[\u4e00-\u9fff]/.test(normalizedLabel)) {
    return false;
  }
  return !/\s/.test(normalizedLabel);
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

function buildSnapshotReadingMeta(valueType: string, unit: string) {
  const parts: string[] = [];
  const normalizedType = resolveSnapshotValueTypeLabel(valueType);
  if (normalizedType) {
    parts.push(normalizedType);
  }
  parts.push(unit && unit !== '--' ? `单位 ${unit}` : '未标注单位');
  return parts.join(' · ');
}

function resolveSnapshotValueTypeLabel(valueType: string) {
  const normalizedValueType = normalizeText(valueType).toLowerCase();
  if (!normalizedValueType || normalizedValueType === '--') {
    return '';
  }
  if (['double', 'float', 'decimal', 'number'].includes(normalizedValueType)) {
    return '数值';
  }
  if (['int', 'integer', 'long', 'short'].includes(normalizedValueType)) {
    return '整数';
  }
  if (['bool', 'boolean'].includes(normalizedValueType)) {
    return '布尔';
  }
  if (['string', 'text'].includes(normalizedValueType)) {
    return '文本';
  }
  return normalizedValueType.toUpperCase();
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
.insight-state-card,
.insight-hero,
.highlight-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand) 4%, white));
  box-shadow: var(--shadow-sm);
}

.insight-state-card,
.insight-hero {
  padding: 1.4rem;
}

.insight-state-banner {
  display: grid;
  gap: 0.32rem;
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  border: 1px solid color-mix(in srgb, #ef4444 18%, var(--panel-border));
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(255, 243, 242, 0.96));
}

.insight-state-banner strong {
  color: #b42318;
  font-size: 0.92rem;
}

.insight-state-banner p {
  margin: 0;
  color: var(--text-secondary);
}

.insight-state-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  min-height: 11rem;
}

.insight-state-card--loading {
  border-color: color-mix(in srgb, var(--brand) 18%, var(--panel-border));
}

.insight-state-card__copy {
  display: grid;
  gap: 0.44rem;
  max-width: 44rem;
}

.insight-state-card__eyebrow {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 1.7rem;
  padding: 0.1rem 0.56rem;
  border-radius: 999px;
  background: rgba(91, 109, 133, 0.08);
  color: var(--text-secondary);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.insight-state-card__copy h2 {
  margin: 0;
  font-size: clamp(1.42rem, 1.9vw, 1.78rem);
}

.insight-state-card__copy p {
  margin: 0;
  max-width: 38rem;
  line-height: 1.75;
}

.insight-state-card__aside {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
  gap: 0.48rem;
  max-width: 18rem;
}

.insight-state-card__pill {
  display: inline-flex;
  align-items: center;
  min-height: 1.9rem;
  padding: 0.18rem 0.68rem;
  border-radius: 999px;
  background: rgba(91, 109, 133, 0.08);
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
  white-space: nowrap;
}

.insight-state-card__pulse {
  width: 0.78rem;
  height: 0.78rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--brand) 72%, white);
  box-shadow: 0 0 0 0 color-mix(in srgb, var(--brand) 22%, transparent);
  animation: insight-pulse 1.5s ease-out infinite;
}

@keyframes insight-pulse {
  0% {
    box-shadow: 0 0 0 0 color-mix(in srgb, var(--brand) 22%, transparent);
  }
  70% {
    box-shadow: 0 0 0 0.7rem rgba(255, 255, 255, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 255, 255, 0);
  }
}

.collector-insight-bridge {
  display: grid;
  gap: 0.9rem;
}

.collector-insight-bridge__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.9rem;
  padding: 0.1rem 0.2rem 0;
}

.collector-insight-bridge__heading {
  display: grid;
  gap: 0.34rem;
  max-width: 42rem;
}

.collector-insight-bridge__eyebrow {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 1.7rem;
  padding: 0.1rem 0.56rem;
  border-radius: 999px;
  background: rgba(91, 109, 133, 0.08);
  color: var(--text-secondary);
  font-size: 0.76rem;
  font-weight: 700;
}

.collector-insight-bridge__heading strong {
  color: var(--text-heading);
  font-size: 1.05rem;
}

.collector-insight-bridge__heading p,
.collector-insight-bridge__notice p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.65;
}

.collector-insight-bridge__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.45rem;
}

.collector-insight-bridge__pill,
.collector-insight-bridge__notice-label {
  display: inline-flex;
  align-items: center;
  min-height: 1.8rem;
  padding: 0.18rem 0.64rem;
  border-radius: 999px;
  background: rgba(91, 109, 133, 0.08);
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
  white-space: nowrap;
}

.collector-insight-bridge__pill--brand {
  background: color-mix(in srgb, var(--brand) 12%, white);
  color: color-mix(in srgb, var(--brand) 70%, var(--text-primary));
}

.collector-insight-bridge__pill--attention {
  background: rgba(239, 68, 68, 0.1);
  color: #c0392b;
}

.collector-insight-bridge__notice {
  display: grid;
  gap: 0.38rem;
  padding: 0.88rem 1rem;
  border-radius: calc(var(--radius-lg) - 0.25rem);
  border: 1px solid color-mix(in srgb, #f59e0b 22%, var(--panel-border));
  background: linear-gradient(180deg, rgba(255, 251, 235, 0.96), rgba(255, 255, 255, 0.98));
}

.collector-insight-bridge__notice-label {
  width: fit-content;
  background: color-mix(in srgb, var(--brand) 14%, white);
  color: color-mix(in srgb, var(--brand) 72%, var(--text-primary));
}

.runtime-insight-bridge {
  display: grid;
  gap: 0.9rem;
}

.runtime-insight-bridge__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.9rem;
  padding: 0.2rem 0.2rem 0;
}

.runtime-insight-bridge__heading {
  display: grid;
  gap: 0.34rem;
  max-width: 44rem;
}

.runtime-insight-bridge__eyebrow {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 1.7rem;
  padding: 0.1rem 0.56rem;
  border-radius: 999px;
  background: rgba(91, 109, 133, 0.08);
  color: var(--text-secondary);
  font-size: 0.76rem;
  font-weight: 700;
}

.runtime-insight-bridge__heading strong {
  color: var(--text-heading);
  font-size: 1.05rem;
}

.runtime-insight-bridge__heading p,
.runtime-insight-bridge__sequence p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.65;
}

.runtime-insight-bridge__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.45rem;
}

.runtime-insight-bridge__pill,
.runtime-insight-bridge__sequence-pill {
  display: inline-flex;
  align-items: center;
  min-height: 1.8rem;
  padding: 0.18rem 0.64rem;
  border-radius: 999px;
  background: rgba(91, 109, 133, 0.08);
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
  white-space: nowrap;
}

.runtime-insight-bridge__pill--brand {
  background: color-mix(in srgb, var(--brand) 12%, white);
  color: color-mix(in srgb, var(--brand) 70%, var(--text-primary));
}

.runtime-insight-bridge__pill--muted {
  background: color-mix(in srgb, var(--surface-soft) 72%, white);
}

.runtime-insight-bridge__sequence {
  display: grid;
  gap: 0.38rem;
  padding: 0.88rem 1rem;
  border-radius: calc(var(--radius-lg) - 0.25rem);
  border: 1px solid color-mix(in srgb, var(--panel-border) 84%, white);
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.94), rgba(255, 255, 255, 0.98));
}

.runtime-insight-bridge__sequence-pill {
  width: fit-content;
  background: color-mix(in srgb, var(--brand) 10%, white);
  color: color-mix(in srgb, var(--brand) 68%, var(--text-primary));
}

.runtime-diagnosis-strip {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.9rem;
  padding: 0.92rem 1rem;
  border-radius: calc(var(--radius-lg) - 0.24rem);
  border: 1px solid color-mix(in srgb, var(--panel-border) 82%, white);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand) 4%, white));
}

.runtime-diagnosis-strip__copy {
  display: grid;
  gap: 0.26rem;
  min-width: 0;
}

.runtime-diagnosis-strip__eyebrow {
  color: var(--text-tertiary);
  font-size: 0.76rem;
  font-weight: 600;
}

.runtime-diagnosis-strip__copy strong {
  color: var(--text-heading);
}

.runtime-diagnosis-strip__copy p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.58;
}

.runtime-diagnosis-strip__pill {
  display: inline-flex;
  align-items: center;
  min-height: 1.85rem;
  padding: 0.18rem 0.68rem;
  border-radius: 999px;
  background: rgba(91, 109, 133, 0.08);
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 700;
  white-space: nowrap;
}

.runtime-diagnosis-strip__aside {
  display: grid;
  justify-items: flex-end;
  gap: 0.55rem;
}

.runtime-diagnosis-strip__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.45rem;
}

.runtime-diagnosis-strip--formal {
  border-color: color-mix(in srgb, var(--brand) 18%, var(--panel-border));
}

.runtime-diagnosis-strip--runtime {
  border-color: rgba(91, 109, 133, 0.2);
}

.runtime-diagnosis-strip--attention {
  border-color: rgba(255, 122, 26, 0.26);
  background: linear-gradient(180deg, rgba(255, 245, 237, 0.96), rgba(255, 255, 255, 0.98));
}

.runtime-diagnosis-strip--neutral {
  border-color: color-mix(in srgb, var(--line-panel) 78%, white);
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

.insight-detail-shell,
.insight-detail-grid,
.insight-narrative-stack,
.highlight-grid {
  display: grid;
  gap: 0.9rem;
}

.insight-detail-shell {
  gap: 1rem;
}

.insight-detail-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.highlight-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.insight-detail-identity,
.insight-detail-section,
.insight-narrative-item,
.highlight-card {
  border-radius: calc(var(--radius-lg) - 0.2rem);
  border: 1px solid color-mix(in srgb, var(--panel-border) 88%, white);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.92));
}

.insight-detail-identity,
.insight-detail-section,
.highlight-card {
  padding: 1rem;
}

.insight-detail-identity {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.05rem;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(247, 250, 255, 0.95)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 10%, transparent), transparent 45%);
}

.insight-detail-identity__main {
  display: grid;
  gap: 0.28rem;
}

.insight-detail-identity__main strong,
.insight-detail-section__header strong,
.insight-narrative-item strong,
.highlight-card strong {
  color: var(--text-primary);
}

.insight-detail-identity__main p {
  margin: 0;
  color: var(--text-secondary);
}

.insight-detail-identity__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.55rem;
}

.insight-meta-pill {
  display: inline-flex;
  align-items: center;
  padding: 0.38rem 0.72rem;
  border-radius: 999px;
  background: rgba(91, 109, 133, 0.08);
  color: var(--text-secondary);
  font-size: 0.82rem;
  font-weight: 600;
}

.insight-detail-section {
  display: grid;
  gap: 0.75rem;
  min-width: 0;
}

.insight-detail-section--accent {
  border-color: color-mix(in srgb, var(--brand) 18%, var(--panel-border));
}

.insight-detail-section__header,
.insight-narrative-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.insight-detail-section__title {
  display: grid;
  gap: 0.22rem;
}

.insight-detail-section__digest {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.42rem;
}

.insight-detail-section__digest-pill {
  display: inline-flex;
  align-items: center;
  min-height: 1.72rem;
  padding: 0.14rem 0.56rem;
  border-radius: 999px;
  background: rgba(91, 109, 133, 0.08);
  color: var(--text-secondary);
  font-size: 0.76rem;
  font-weight: 600;
  white-space: nowrap;
}

.insight-detail-section__digest-pill--accent {
  background: color-mix(in srgb, var(--brand) 10%, white);
}

.insight-detail-section__header span,
.insight-narrative-item small,
.highlight-card span,
.highlight-card small {
  color: var(--text-tertiary);
}

.insight-fact-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.6rem 0.72rem;
  margin: 0;
}

.insight-fact-row {
  display: grid;
  gap: 0.22rem;
  padding: 0.78rem 0.86rem;
  border-radius: calc(var(--radius-md) - 0.1rem);
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid color-mix(in srgb, var(--line-panel) 72%, white);
  min-width: 0;
}

.insight-fact-row dt,
.insight-fact-row dd {
  margin: 0;
}

.insight-fact-row dt {
  color: var(--text-tertiary);
  font-size: 0.76rem;
  font-weight: 600;
}

.insight-fact-row dd {
  color: var(--text-heading);
  font-size: 0.92rem;
  font-weight: 600;
  line-height: 1.5;
  word-break: break-word;
}

.insight-analysis-shell,
.insight-narrative-stack {
  gap: 0.75rem;
}

.insight-analysis-shell {
  display: grid;
}

.insight-analysis-lead {
  display: grid;
  gap: 0.42rem;
  padding: 1rem 1.05rem;
  border-radius: calc(var(--radius-lg) - 0.22rem);
  border: 1px solid color-mix(in srgb, var(--brand) 16%, var(--panel-border));
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(247, 250, 255, 0.95)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 9%, transparent), transparent 46%);
}

.insight-analysis-lead__header {
  display: grid;
  gap: 0.26rem;
}

.insight-analysis-lead__header small {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 0.18rem 0.5rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--brand) 10%, white);
  color: var(--text-secondary);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.insight-analysis-lead__header strong {
  color: var(--text-heading);
  font-size: 1.06rem;
}

.insight-analysis-lead p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.72;
}

.insight-narrative-item {
  display: grid;
  gap: 0.55rem;
  padding: 0.9rem 0.95rem;
  border: 1px solid color-mix(in srgb, var(--line-panel) 74%, white);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.03);
}

.insight-narrative-item__heading {
  display: grid;
  gap: 0.3rem;
}

.insight-narrative-item small {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 0.18rem 0.5rem;
  border-radius: 999px;
  background: rgba(91, 109, 133, 0.08);
  font-size: 0.78rem;
  letter-spacing: 0.02em;
}

.insight-narrative-item p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.snapshot-workbench {
  display: grid;
  gap: 0.9rem;
}

.snapshot-workbench__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.9rem;
  padding: 0.95rem 1rem;
  border-radius: calc(var(--radius-lg) - 0.2rem);
  border: 1px solid color-mix(in srgb, var(--panel-border) 88%, white);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.92));
}

.snapshot-workbench__heading {
  display: grid;
  gap: 0.28rem;
}

.snapshot-workbench__heading strong {
  color: var(--text-primary);
}

.snapshot-workbench__heading p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.snapshot-workbench__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.45rem;
}

.snapshot-workbench__pill {
  display: inline-flex;
  align-items: center;
  padding: 0.3rem 0.62rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--brand) 10%, white);
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
}

.snapshot-workbench__pill--muted {
  background: rgba(91, 109, 133, 0.08);
}

.snapshot-workbench__focus {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.9rem;
  padding: 0.9rem 1rem;
  border-radius: calc(var(--radius-lg) - 0.28rem);
  border: 1px solid color-mix(in srgb, var(--panel-border) 78%, white);
  background: color-mix(in srgb, var(--surface-soft) 84%, white);
}

.snapshot-workbench__focus--formal {
  border-color: color-mix(in srgb, var(--brand) 16%, var(--panel-border));
  background: linear-gradient(180deg, color-mix(in srgb, var(--brand) 7%, white), rgba(255, 255, 255, 0.96));
}

.snapshot-workbench__focus--runtime {
  border-color: color-mix(in srgb, var(--line-panel) 82%, white);
}

.snapshot-workbench__focus--hybrid {
  border-color: color-mix(in srgb, var(--brand) 12%, var(--line-panel));
}

.snapshot-workbench__focus-copy {
  display: grid;
  gap: 0.28rem;
  min-width: 0;
}

.snapshot-workbench__focus-eyebrow {
  color: var(--text-tertiary);
  font-size: 0.76rem;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.snapshot-workbench__focus-copy strong {
  color: var(--text-heading);
}

.snapshot-workbench__focus-copy p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.58;
}

.snapshot-workbench__focus-pill {
  display: inline-flex;
  align-items: center;
  min-height: 1.9rem;
  padding: 0.2rem 0.72rem;
  border-radius: 999px;
  background: rgba(91, 109, 133, 0.08);
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 700;
  white-space: nowrap;
}

.snapshot-workbench__table-shell {
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--panel-border) 84%, white);
  background: rgba(255, 255, 255, 0.94);
  overflow: hidden;
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

.monitoring-snapshot-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.monitoring-snapshot-table :deep(.el-table__header th) {
  padding-block: 0.72rem;
  background: color-mix(in srgb, var(--surface-soft) 86%, white);
  color: var(--text-tertiary);
  font-size: 0.78rem;
  font-weight: 600;
}

.monitoring-snapshot-table :deep(.el-table__body td) {
  padding-block: 0.82rem;
  vertical-align: top;
}

.monitoring-snapshot-table :deep(.el-table__row:hover > td) {
  background: color-mix(in srgb, var(--brand) 3%, white);
}

.snapshot-reading-cell,
.snapshot-origin-cell,
.snapshot-action-cell {
  display: grid;
  gap: 0.24rem;
}

.snapshot-reading-cell strong,
.snapshot-origin-cell__pill {
  width: fit-content;
}

.snapshot-reading-cell strong {
  color: var(--text-heading);
  font-size: 0.92rem;
  font-weight: 600;
}

.snapshot-reading-cell--focused,
.snapshot-origin-cell--focused,
.snapshot-action-cell--focused {
  padding: 0.18rem 0.24rem;
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--brand) 7%, white);
}

.snapshot-reading-cell__focus-badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 1.5rem;
  padding: 0.1rem 0.48rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--brand) 14%, white);
  color: color-mix(in srgb, var(--brand) 72%, var(--text-primary));
  font-size: 0.72rem;
  font-weight: 700;
}

.snapshot-reading-cell small,
.snapshot-origin-cell small,
.monitoring-snapshot-table__action-hint {
  color: var(--text-tertiary);
  font-size: 0.8rem;
  line-height: 1.5;
}

.snapshot-origin-cell__pill {
  display: inline-flex;
  align-items: center;
  min-height: 1.7rem;
  padding: 0.14rem 0.56rem;
  border-radius: 999px;
  font-size: 0.76rem;
  font-weight: 700;
}

.snapshot-origin-cell__pill--formal {
  background: color-mix(in srgb, var(--brand) 10%, white);
  color: var(--button-action-text);
}

.snapshot-origin-cell__pill--runtime {
  background: rgba(91, 109, 133, 0.08);
  color: var(--text-secondary);
}

.monitoring-snapshot-table__action-hint {
  max-width: 14rem;
}

@media (max-width: 1024px) {
  .insight-hero,
  .insight-detail-grid,
  .insight-fact-list,
  .highlight-grid {
    grid-template-columns: 1fr;
  }

  .insight-state-card,
  .insight-hero,
  .insight-detail-identity {
    flex-direction: column;
  }

  .insight-state-card__aside,
  .insight-detail-identity__meta,
  .insight-detail-section__digest {
    justify-content: flex-start;
    max-width: none;
  }

  .snapshot-workbench__header,
  .snapshot-workbench__focus,
  .runtime-diagnosis-strip,
  .snapshot-workbench__meta,
  .collector-insight-bridge__header,
  .collector-insight-bridge__meta,
  .runtime-insight-bridge__header,
  .runtime-insight-bridge__meta {
    flex-direction: column;
    justify-content: flex-start;
  }
}
</style>
