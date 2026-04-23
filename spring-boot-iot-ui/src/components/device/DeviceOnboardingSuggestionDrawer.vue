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
        <h3>线索摘要</h3>
        <dl class="device-onboarding-suggestion-drawer__grid">
          <div>
            <dt>设备编码</dt>
            <dd>{{ displayText(sourceRow?.deviceCode || suggestion?.deviceCode) }}</dd>
          </div>
          <div>
            <dt>Trace</dt>
            <dd>{{ displayText(suggestion?.traceId || sourceRow?.lastTraceId) }}</dd>
          </div>
          <div>
            <dt>产品 Key</dt>
            <dd>{{ displayText(suggestion?.productKey || sourceRow?.productKey) }}</dd>
          </div>
          <div>
            <dt>协议</dt>
            <dd>{{ displayText(suggestion?.protocolCode || sourceRow?.protocolCode) }}</dd>
          </div>
          <div>
            <dt>失败阶段</dt>
            <dd>{{ displayText(suggestion?.lastFailureStage || sourceRow?.lastFailureStage) }}</dd>
          </div>
          <div>
            <dt>来源</dt>
            <dd>{{ displayText(suggestion?.assetSourceType || sourceRow?.assetSourceType) }}</dd>
          </div>
        </dl>
      </section>

      <section class="device-onboarding-suggestion-drawer__section">
        <h3>系统建议</h3>
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
          <div>
            <dt>建议状态</dt>
            <dd>{{ displayText(suggestion?.suggestionStatus) }}</dd>
          </div>
        </dl>
      </section>

      <section class="device-onboarding-suggestion-drawer__section">
        <h3>规则缺口</h3>
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
    ? '当前线索已具备较完整的接入建议，可继续人工确认。'
    : '当前先给出推荐产品、协议族、模板与仍需补齐的治理缺口。'
)
const ruleGaps = computed(() => props.suggestion?.ruleGaps || [])

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
</script>

<style scoped>
.device-onboarding-suggestion-drawer__stack {
  display: grid;
  gap: 1rem;
}

.device-onboarding-suggestion-drawer__section {
  border: 1px solid var(--el-border-color-light);
  border-radius: 1rem;
  padding: 1rem;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 0.98));
}

.device-onboarding-suggestion-drawer__section h3 {
  margin: 0 0 0.75rem;
  font-size: 1rem;
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
  color: var(--el-text-color-secondary);
  font-size: 0.875rem;
}

.device-onboarding-suggestion-drawer__grid dd {
  margin: 0;
  color: var(--el-text-color-primary);
  font-weight: 600;
  word-break: break-all;
}

.device-onboarding-suggestion-drawer__gap-list {
  margin: 0;
  padding-left: 1rem;
  display: grid;
  gap: 0.5rem;
}

.device-onboarding-suggestion-drawer__empty {
  margin: 0;
  color: var(--el-text-color-secondary);
}

@media (max-width: 720px) {
  .device-onboarding-suggestion-drawer__grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
