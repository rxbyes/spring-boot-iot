<template>
  <section v-if="situation" class="detail-panel risk-object-situation">
    <div class="detail-section-header">
      <div>
        <h3>态势判定</h3>
        <p>展示风险对象绑定信号与联动响应判定，帮助值班人员理解单一阈值信号为何触发或未触发处置。</p>
      </div>
    </div>

    <div class="detail-grid">
      <div class="detail-field">
        <span class="detail-field__label">触发联动</span>
        <strong class="detail-field__value">{{ triggerResponseText }}</strong>
      </div>
      <div class="detail-field">
        <span class="detail-field__label">活跃信号</span>
        <strong class="detail-field__value">{{ activeSignalSummary }}</strong>
      </div>
      <div class="detail-field">
        <span class="detail-field__label">响应级别</span>
        <strong class="detail-field__value">{{ displayValue(situation.responseLevel) }}</strong>
      </div>
      <div class="detail-field detail-field--full">
        <span class="detail-field__label">判定原因</span>
        <strong class="detail-field__value detail-field__value--plain">{{ displayValue(situation.reasonCode) }}</strong>
      </div>
    </div>

    <div v-if="activeSignals.length" class="risk-object-situation__signals">
      <span class="risk-object-situation__signals-title">活跃信号明细</span>
      <div class="risk-object-situation__signal-list">
        <article
          v-for="(signal, index) in activeSignals"
          :key="`${displayValue(signal.identifier)}-${index}`"
          class="risk-object-situation__signal"
        >
          <strong>{{ signalTitle(signal) }}</strong>
          <span>{{ signalMeta(signal) }}</span>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface RiskObjectSignal {
  bindingId?: unknown;
  deviceId?: unknown;
  deviceName?: unknown;
  deviceCode?: unknown;
  metricName?: unknown;
  metricIdentifier?: unknown;
  identifier?: unknown;
  value?: unknown;
  currentValue?: unknown;
  thresholdValue?: unknown;
}

interface RiskObjectSituation {
  reasonCode?: unknown;
  triggerResponse?: unknown;
  totalBindingCount?: unknown;
  activeSignalCount?: unknown;
  responseLevel?: unknown;
  activeSignals?: unknown;
}

const props = defineProps<{
  source?: string | null;
}>();

const situation = computed(() => parseRiskObjectSituation(props.source));
const activeSignals = computed<RiskObjectSignal[]>(() => {
  const signals = situation.value?.activeSignals;
  if (!Array.isArray(signals)) {
    return [];
  }
  return signals.filter((signal): signal is RiskObjectSignal => isPlainObject(signal)).slice(0, 6);
});

const triggerResponseText = computed(() => {
  const value = situation.value?.triggerResponse;
  if (value === true) {
    return '已触发';
  }
  if (value === false) {
    return '未触发';
  }
  return displayValue(value);
});

const activeSignalSummary = computed(() => {
  const active = displayValue(situation.value?.activeSignalCount);
  const total = displayValue(situation.value?.totalBindingCount);
  if (active === '--' && total === '--') {
    return '--';
  }
  return `${active} / ${total}`;
});

function parseRiskObjectSituation(source?: string | null): RiskObjectSituation | null {
  if (!source || typeof source !== 'string') {
    return null;
  }
  try {
    const parsed = JSON.parse(source);
    if (!isPlainObject(parsed)) {
      return null;
    }
    const nested = parsed.riskObjectSituation;
    if (isPlainObject(nested)) {
      return nested as RiskObjectSituation;
    }
    if ('reasonCode' in parsed || 'triggerResponse' in parsed || 'activeSignalCount' in parsed) {
      return parsed as RiskObjectSituation;
    }
  } catch {
    return null;
  }
  return null;
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function displayValue(value: unknown) {
  if (value === null || value === undefined || value === '') {
    return '--';
  }
  return String(value);
}

function signalTitle(signal: RiskObjectSignal) {
  return displayValue(
    signal.metricName ||
      signal.metricIdentifier ||
      signal.identifier ||
      signal.deviceName ||
      signal.deviceCode ||
      signal.deviceId
  );
}

function signalMeta(signal: RiskObjectSignal) {
  const source = displayValue(signal.deviceName || signal.deviceCode || signal.deviceId);
  const identifier = displayValue(signal.metricIdentifier || signal.identifier);
  const value = displayValue(signal.currentValue ?? signal.value);
  const threshold = displayValue(signal.thresholdValue);
  const parts = [source !== '--' ? source : null, identifier !== '--' ? identifier : null].filter(Boolean);
  const valueText = value !== '--' || threshold !== '--' ? `当前 ${value} / 阈值 ${threshold}` : null;
  return [...parts, valueText].filter(Boolean).join(' · ') || '--';
}
</script>

<style scoped>
.risk-object-situation {
  border-color: rgba(245, 158, 11, 0.24);
}

.risk-object-situation__signals {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
}

.risk-object-situation__signals-title {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.risk-object-situation__signal-list {
  display: grid;
  gap: 10px;
}

.risk-object-situation__signal {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 8px;
  background: rgba(248, 250, 252, 0.78);
}

.risk-object-situation__signal strong {
  color: #0f172a;
  font-size: 14px;
}

.risk-object-situation__signal span {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}
</style>
