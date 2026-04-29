<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    class="device-onboarding-suggestion-drawer"
    size="40rem"
    :title="drawerTitle"
    :subtitle="drawerSubtitle"
    :loading="loading"
    :error-message="errorMessage"
    :empty="!suggestion && !sourceRow"
    @update:modelValue="emit('update:modelValue', $event)"
  >
    <div v-if="sourceRow || suggestion" class="device-onboarding-suggestion-drawer__stack">
      <section class="device-onboarding-suggestion-drawer__section">
        <div class="device-onboarding-suggestion-drawer__section-header">
          <div>
            <h3>线索摘要</h3>
            <p>先确认当前线索的设备身份、Trace 和最近失败来源，再决定是否继续人工转正。</p>
          </div>
          <el-tag
            v-if="statusLabel"
            :type="statusTagType"
            effect="plain"
            round
            class="device-onboarding-suggestion-drawer__status-pill"
          >
            {{ statusLabel }}
          </el-tag>
        </div>

        <article class="device-onboarding-suggestion-drawer__identity-card">
          <span class="device-onboarding-suggestion-drawer__identity-label">设备线索</span>
          <strong class="device-onboarding-suggestion-drawer__identity-title">{{ identityName }}</strong>
          <div class="device-onboarding-suggestion-drawer__identity-meta">
            <small>设备编码 {{ identityDeviceCode }}</small>
            <small>Trace {{ identityTraceId }}</small>
          </div>
        </article>

        <dl class="device-onboarding-suggestion-drawer__grid">
          <div>
            <dt>产品 Key</dt>
            <dd>{{ displayText(suggestion?.productKey || sourceRow?.productKey) }}</dd>
          </div>
          <div>
            <dt>接入协议</dt>
            <dd>{{ displayText(suggestion?.protocolCode || sourceRow?.protocolCode) }}</dd>
          </div>
          <div>
            <dt>失败阶段</dt>
            <dd>{{ displayText(suggestion?.lastFailureStage || sourceRow?.lastFailureStage) }}</dd>
          </div>
          <div>
            <dt>来源</dt>
            <dd>{{ displayText(sourceTypeLabel) }}</dd>
          </div>
        </dl>
      </section>

      <section class="device-onboarding-suggestion-drawer__section">
        <div class="device-onboarding-suggestion-drawer__section-header">
          <div>
            <h3>系统建议</h3>
            <p>这里集中展示推荐产品、协议族、解密档案和模板，方便确认是否具备转正前提。</p>
          </div>
        </div>

        <dl class="device-onboarding-suggestion-drawer__grid">
          <div>
            <dt>推荐产品</dt>
            <dd>{{ displayText(formatProductSuggestion(suggestion)) }}</dd>
          </div>
          <div>
            <dt>推荐协议族</dt>
            <dd>{{ displayText(formatFamilySuggestion(suggestion)) }}</dd>
          </div>
          <div>
            <dt>推荐解密档案</dt>
            <dd>{{ displayText(suggestion?.recommendedDecryptProfileCode) }}</dd>
          </div>
          <div>
            <dt>推荐模板</dt>
            <dd>{{ displayText(formatTemplateSuggestion(suggestion)) }}</dd>
          </div>
        </dl>
      </section>

      <section class="device-onboarding-suggestion-drawer__section">
        <div class="device-onboarding-suggestion-drawer__section-header">
          <div>
            <h3>规则缺口</h3>
            <p>如果这里仍有缺口，建议先完成治理再转为正式设备，避免把不完整线索写进正式台账。</p>
          </div>
        </div>

        <ul v-if="ruleGaps.length" class="device-onboarding-suggestion-drawer__gap-list">
          <li v-for="gap in ruleGaps" :key="gap">{{ gap }}</li>
        </ul>
        <p v-else class="device-onboarding-suggestion-drawer__empty">当前建议未发现额外规则缺口。</p>
      </section>
    </div>
  </StandardDetailDrawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import type { Device, DeviceOnboardingSuggestion } from '@/types/api'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    suggestion?: DeviceOnboardingSuggestion | null
    loading?: boolean
    errorMessage?: string
    sourceRow?: Device | null
  }>(),
  {
    suggestion: null,
    loading: false,
    errorMessage: '',
    sourceRow: null
  }
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

const drawerTitle = computed(() => props.sourceRow?.deviceCode || props.suggestion?.deviceCode || '接入建议')
const drawerSubtitle = computed(() =>
  props.suggestion?.suggestionStatus === 'READY'
    ? '当前线索已具备较完整的接入建议，可以继续人工确认。'
    : '当前先给出推荐产品、协议族、模板与仍需补齐的治理缺口。'
)
const ruleGaps = computed(() => props.suggestion?.ruleGaps || [])
const identityName = computed(() =>
  displayText(props.sourceRow?.deviceName || props.suggestion?.deviceName || props.sourceRow?.deviceCode || props.suggestion?.deviceCode)
)
const identityDeviceCode = computed(() => displayText(props.sourceRow?.deviceCode || props.suggestion?.deviceCode))
const identityTraceId = computed(() => displayText(props.suggestion?.traceId || props.sourceRow?.lastTraceId))
const statusLabel = computed(() => formatSuggestionStatus(props.suggestion?.suggestionStatus))
const statusTagType = computed(() => resolveSuggestionStatusTagType(props.suggestion?.suggestionStatus))
const sourceTypeLabel = computed(() => formatSourceType(props.suggestion?.assetSourceType || props.sourceRow?.assetSourceType))

function displayText(value?: string | null) {
  return value && value.trim() ? value : '--'
}

function formatProductSuggestion(suggestion?: DeviceOnboardingSuggestion | null) {
  if (!suggestion) {
    return null
  }
  const productKey = suggestion.recommendedProductKey?.trim()
  const productName = suggestion.recommendedProductName?.trim()
  if (productKey && productName) {
    return `${productKey} / ${productName}`
  }
  return productKey || productName || null
}

function formatFamilySuggestion(suggestion?: DeviceOnboardingSuggestion | null) {
  if (!suggestion) {
    return null
  }
  const familyCode = suggestion.recommendedFamilyCode?.trim()
  const familyName = suggestion.recommendedFamilyName?.trim()
  if (familyCode && familyName) {
    return `${familyCode} / ${familyName}`
  }
  return familyCode || familyName || null
}

function formatTemplateSuggestion(suggestion?: DeviceOnboardingSuggestion | null) {
  if (!suggestion) {
    return null
  }
  const templateCode = suggestion.recommendedTemplateCode?.trim()
  const templateName = suggestion.recommendedTemplateName?.trim()
  if (templateCode && templateName && templateCode !== templateName) {
    return `${templateCode} / ${templateName}`
  }
  return templateCode || templateName || null
}

function formatSuggestionStatus(value?: string | null) {
  if (!value) {
    return ''
  }
  if (value === 'READY') {
    return '可转正式'
  }
  if (value === 'BLOCKED') {
    return '待补治理'
  }
  if (value === 'PARTIAL') {
    return '建议不完整'
  }
  return value
}

function resolveSuggestionStatusTagType(value?: string | null) {
  if (value === 'READY') {
    return 'success'
  }
  if (value === 'BLOCKED') {
    return 'warning'
  }
  if (value === 'PARTIAL') {
    return 'info'
  }
  return 'info'
}

function formatSourceType(value?: string | null) {
  if (!value) {
    return '--'
  }
  if (value === 'access_error' || value === 'invalid_report_state') {
    return '失败归档'
  }
  if (value === 'dispatch_failed') {
    return '派发失败'
  }
  if (value === 'registry') {
    return '设备台账'
  }
  return value
}
</script>

<style scoped>
.device-onboarding-suggestion-drawer__stack {
  display: grid;
  gap: 1rem;
}

.device-onboarding-suggestion-drawer__section {
  display: grid;
  gap: 0.9rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  padding: 1rem;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 0.98));
}

.device-onboarding-suggestion-drawer__section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.8rem;
}

.device-onboarding-suggestion-drawer__section-header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 1rem;
}

.device-onboarding-suggestion-drawer__section-header p {
  margin: 0.35rem 0 0;
  color: var(--text-caption);
  font-size: 0.82rem;
  line-height: 1.65;
}

.device-onboarding-suggestion-drawer__status-pill {
  flex: none;
}

.device-onboarding-suggestion-drawer__identity-card {
  display: grid;
  gap: 0.28rem;
  padding: 0.92rem 1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, transparent);
  border-radius: 0.9rem;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 249, 255, 0.94)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 10%, transparent), transparent 42%);
}

.device-onboarding-suggestion-drawer__identity-label {
  color: var(--text-caption);
  font-size: 0.78rem;
}

.device-onboarding-suggestion-drawer__identity-title {
  color: var(--text-heading);
  font-size: 1.05rem;
  line-height: 1.45;
}

.device-onboarding-suggestion-drawer__identity-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 0.9rem;
}

.device-onboarding-suggestion-drawer__identity-meta small {
  color: var(--text-secondary);
  font-size: 0.78rem;
  line-height: 1.4;
}

.device-onboarding-suggestion-drawer__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem 1rem;
  margin: 0;
}

.device-onboarding-suggestion-drawer__grid div {
  min-width: 0;
}

.device-onboarding-suggestion-drawer__grid dt {
  margin: 0 0 0.25rem;
  color: var(--text-caption);
  font-size: 0.82rem;
}

.device-onboarding-suggestion-drawer__grid dd {
  margin: 0;
  color: var(--text-heading);
  font-weight: 600;
  word-break: break-all;
}

.device-onboarding-suggestion-drawer__gap-list {
  margin: 0;
  padding-left: 1rem;
  display: grid;
  gap: 0.5rem;
  color: var(--text-body);
}

.device-onboarding-suggestion-drawer__empty {
  margin: 0;
  color: var(--text-caption);
}

@media (max-width: 720px) {
  .device-onboarding-suggestion-drawer__section-header {
    flex-direction: column;
  }

  .device-onboarding-suggestion-drawer__grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
