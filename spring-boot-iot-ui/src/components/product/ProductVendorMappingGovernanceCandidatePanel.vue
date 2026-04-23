<template>
  <PanelCard class="governance-candidate-panel">
    <div v-if="loading" class="governance-candidate-panel__state">
      正在加载待治理候选...
    </div>
    <div v-else-if="errorMessage" class="governance-candidate-panel__state">
      <strong>待治理候选加载失败</strong>
      <p>{{ errorMessage }}</p>
    </div>
    <div v-else-if="!candidates.length" class="governance-candidate-panel__state">
      <strong>暂无待治理候选</strong>
      <p>所有高频字段已正式化或已纳入建议。</p>
    </div>

    <template v-else>
      <div class="governance-candidate-panel__list">
        <article
          v-for="item in candidates"
          :key="item.rawIdentifier"
          class="governance-candidate-panel__item"
        >
          <div class="governance-candidate-panel__item-main">
            <strong>{{ item.rawIdentifier }}</strong>
            <span>{{ item.targetNormativeIdentifier || '--' }}</span>
            <small>
              证据 {{ item.evidenceCount ?? 0 }} · 置信度 {{ formatConfidence(item.confidence) }} · 最近出现 {{ item.lastSeenTime || '--' }}
            </small>
          </div>
          <StandardButton
            action="query"
            link
            @click="emit('goAccept', item)"
          >
            去采纳
          </StandardButton>
        </article>
      </div>
      <p
        v-if="totalSuggestionCount > candidates.length"
        class="governance-candidate-panel__more"
      >
        <StandardButton action="query" link @click="emit('viewAllSuggestions')">
          查看全部 {{ totalSuggestionCount }} 条建议
        </StandardButton>
      </p>
    </template>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  listVendorMetricMappingRuleSuggestions
} from '@/api/vendorMetricMappingRule'
import { isHandledRequestError, resolveRequestErrorMessage } from '@/api/request'
import PanelCard from '@/components/PanelCard.vue'
import StandardButton from '@/components/StandardButton.vue'
import type { IdType, VendorMetricMappingRuleSuggestion } from '@/types/api'

const MAX_VISIBLE_CANDIDATES = 10

const props = defineProps<{
  productId?: IdType | null
}>()

const emit = defineEmits<{
  (event: 'goAccept', item: VendorMetricMappingRuleSuggestion): void
  (event: 'viewAllSuggestions'): void
}>()

const loading = ref(false)
const errorMessage = ref('')
const allSuggestions = ref<VendorMetricMappingRuleSuggestion[]>([])

const candidates = computed(() => {
  return allSuggestions.value
    .filter((s) => s.status === 'READY_TO_CREATE')
    .sort((a, b) => (b.evidenceCount ?? 0) - (a.evidenceCount ?? 0))
    .slice(0, MAX_VISIBLE_CANDIDATES)
})

const totalSuggestionCount = computed(() => allSuggestions.value.length)

watch(
  () => props.productId,
  () => { void loadCandidates() },
  { immediate: true }
)

function formatConfidence(confidence?: number | string | null) {
  const numeric = typeof confidence === 'string' && confidence.trim() !== ''
    ? Number(confidence)
    : confidence
  if (typeof numeric !== 'number' || Number.isNaN(numeric)) return '--'
  return numeric.toFixed(2)
}

async function loadCandidates() {
  if (props.productId === null || props.productId === undefined || props.productId === '') {
    allSuggestions.value = []
    errorMessage.value = ''
    loading.value = false
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await listVendorMetricMappingRuleSuggestions(props.productId, {
      includeCovered: false,
      includeIgnored: false,
      minEvidenceCount: 2
    })
    allSuggestions.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    if (!isHandledRequestError(error)) {
      errorMessage.value = resolveRequestErrorMessage(error, '待治理候选加载失败')
    }
    allSuggestions.value = []
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.governance-candidate-panel__state {
  padding: 1rem;
  color: var(--text-secondary);
}

.governance-candidate-panel__state p {
  margin: 0.45rem 0 0;
}

.governance-candidate-panel__list {
  display: grid;
  gap: 0.6rem;
}

.governance-candidate-panel__item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.6rem;
  align-items: center;
  padding: 0.7rem 0.8rem;
  border: 1px solid var(--panel-border);
  border-radius: 0.7rem;
}

.governance-candidate-panel__item-main {
  display: grid;
  gap: 0.15rem;
}

.governance-candidate-panel__item-main span,
.governance-candidate-panel__item-main small {
  color: var(--text-secondary);
  margin: 0;
}

.governance-candidate-panel__more {
  margin-top: 0.6rem;
  text-align: center;
  color: var(--text-secondary);
  font-size: 0.82rem;
}
</style>
