<template>
  <StandardDetailDrawer
    v-model="visible"
    title="查看设备阈值"
    :subtitle="drawerSubtitle"
    :loading="loading"
    :error-message="errorMessage"
    destroy-on-close
    size="52rem"
  >
    <div v-if="overview" class="device-threshold-drawer">
      <section class="device-threshold-drawer__summary">
        <article class="device-threshold-drawer__summary-card">
          <span class="device-threshold-drawer__summary-label">设备名称</span>
          <strong class="device-threshold-drawer__summary-value">{{ toDisplayText(overview.deviceName) }}</strong>
        </article>
        <article class="device-threshold-drawer__summary-card">
          <span class="device-threshold-drawer__summary-label">设备编号</span>
          <strong class="device-threshold-drawer__summary-value">{{ toDisplayText(overview.deviceCode) }}</strong>
        </article>
        <article class="device-threshold-drawer__summary-card">
          <span class="device-threshold-drawer__summary-label">产品名称</span>
          <strong class="device-threshold-drawer__summary-value">{{ toDisplayText(overview.productName) }}</strong>
        </article>
        <article class="device-threshold-drawer__summary-card">
          <span class="device-threshold-drawer__summary-label">已命中阈值数</span>
          <strong class="device-threshold-drawer__summary-value">{{ overview.matchedMetricCount }}</strong>
        </article>
        <article class="device-threshold-drawer__summary-card">
          <span class="device-threshold-drawer__summary-label">未配置数</span>
          <strong class="device-threshold-drawer__summary-value">{{ overview.missingMetricCount }}</strong>
        </article>
      </section>

      <section v-if="overview.items.length === 0" class="device-threshold-drawer__empty">
        <h3>当前设备暂无可展示阈值</h3>
        <p>未找到正式可绑定监测指标，或当前产品暂未形成可展示的阈值规则。</p>
      </section>

      <section
        v-for="item in overview.items"
        :key="`${item.riskMetricId ?? 'none'}-${item.metricIdentifier}`"
        class="device-threshold-drawer__metric"
      >
        <header class="device-threshold-drawer__metric-header">
          <div>
            <h3>{{ item.metricName || item.metricIdentifier }}</h3>
            <p>{{ item.metricIdentifier }}</p>
          </div>
        </header>

        <section
          v-for="section in sectionGroups(item)"
          v-show="section.items.length"
          :key="section.key"
          class="device-threshold-drawer__group"
        >
          <h4>{{ section.title }}</h4>
          <article
            v-for="rule in section.items"
            :key="`${rule.ruleId ?? 'none'}-${rule.riskPointDeviceId ?? 'none'}-${rule.targetLabel ?? 'none'}`"
            class="device-threshold-drawer__rule"
          >
            <div class="device-threshold-drawer__rule-main">
              <strong>{{ rule.ruleName || '--' }}</strong>
              <div class="device-threshold-drawer__rule-side">
                <em v-if="formatAlarmLevel(rule.alarmLevel)" class="device-threshold-drawer__rule-level">
                  {{ formatAlarmLevel(rule.alarmLevel) }}
                </em>
                <span>{{ rule.expression || '--' }}</span>
              </div>
            </div>
            <small>{{ toDisplayText(rule.sourceLabel) }} / {{ toDisplayText(rule.targetLabel) }}</small>
          </article>
        </section>
      </section>
    </div>
  </StandardDetailDrawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import type { DeviceThresholdMetricItem, DeviceThresholdOverview } from '@/types/api'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    loading?: boolean
    errorMessage?: string
    overview?: DeviceThresholdOverview | null
  }>(),
  {
    loading: false,
    errorMessage: '',
    overview: null
  }
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const drawerSubtitle = computed(() =>
  props.overview?.deviceCode ? `设备编号：${props.overview.deviceCode}` : '按来源查看设备最终生效阈值'
)

function sectionGroups(item: DeviceThresholdMetricItem) {
  return [
    { key: 'effective', title: '当前生效', items: item.effectiveRules || [] },
    { key: 'binding', title: '绑定个性', items: item.bindingRules || [] },
    { key: 'device', title: '设备个性', items: item.deviceRules || [] },
    { key: 'product', title: '产品默认', items: item.productRules || [] },
    { key: 'fallback', title: '通用兜底', items: item.fallbackRules || [] }
  ]
}

function toDisplayText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '--'
  }
  return String(value)
}

function formatAlarmLevel(value?: string | null) {
  if (!value) {
    return ''
  }
  switch (value.trim().toLowerCase()) {
    case 'red':
      return '红色告警'
    case 'orange':
      return '橙色告警'
    case 'yellow':
      return '黄色告警'
    case 'blue':
      return '蓝色告警'
    default:
      return value
  }
}
</script>

<style scoped>
.device-threshold-drawer {
  display: grid;
  gap: 1rem;
}

.device-threshold-drawer__summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: 0.75rem;
}

.device-threshold-drawer__summary-card,
.device-threshold-drawer__metric,
.device-threshold-drawer__empty {
  padding: 0.95rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-md) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 249, 255, 0.94));
}

.device-threshold-drawer__summary-label,
.device-threshold-drawer__metric-header p,
.device-threshold-drawer__rule small,
.device-threshold-drawer__empty p {
  color: var(--text-caption);
}

.device-threshold-drawer__summary-card {
  display: grid;
  gap: 0.3rem;
}

.device-threshold-drawer__summary-value,
.device-threshold-drawer__metric h3,
.device-threshold-drawer__group h4,
.device-threshold-drawer__rule strong {
  color: var(--text-heading);
}

.device-threshold-drawer__summary-value {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.5;
}

.device-threshold-drawer__metric {
  display: grid;
  gap: 0.9rem;
}

.device-threshold-drawer__metric-header h3,
.device-threshold-drawer__empty h3 {
  margin: 0;
  font-size: 15px;
  line-height: 1.5;
}

.device-threshold-drawer__metric-header p,
.device-threshold-drawer__empty p {
  margin: 0.2rem 0 0;
  font-size: 12px;
  line-height: 1.5;
}

.device-threshold-drawer__group {
  display: grid;
  gap: 0.6rem;
}

.device-threshold-drawer__group h4 {
  margin: 0;
  font-size: 13px;
  line-height: 1.4;
}

.device-threshold-drawer__rule {
  display: grid;
  gap: 0.3rem;
  padding: 0.75rem 0.8rem;
  border-radius: var(--radius-md);
  background: rgba(244, 247, 252, 0.82);
}

.device-threshold-drawer__rule-main {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.6rem;
}

.device-threshold-drawer__rule-side {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.5rem;
}

.device-threshold-drawer__rule-level {
  color: var(--text-heading);
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
  line-height: 1.5;
}

.device-threshold-drawer__rule-main span {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}
</style>
