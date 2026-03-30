<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    class="product-business-workbench"
    size="72rem"
    :title="productTitle"
    :subtitle="drawerSubtitle"
    @update:modelValue="emit('update:modelValue', $event)"
  >
    <template #header-actions>
      <slot name="header-actions" />
    </template>

    <section class="product-business-workbench__hero">
      <div class="product-business-workbench__hero-copy">
        <p class="product-business-workbench__eyebrow">产品经营工作台</p>
        <h3>{{ productTitle }}</h3>
        <p class="product-business-workbench__summary">
          <span>{{ productKeyText }}</span>
          <span>{{ protocolText }}</span>
          <span>{{ nodeTypeText }}</span>
          <span>{{ dataFormatText }}</span>
        </p>
      </div>

      <div class="product-business-workbench__hero-metrics">
        <article
          v-for="metric in heroMetrics"
          :key="metric.key"
          class="product-business-workbench__metric"
        >
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <small>{{ metric.hint }}</small>
        </article>
      </div>
    </section>

    <nav class="product-business-workbench__tabs" aria-label="产品经营工作台视图">
      <button
        v-for="view in viewOptions"
        :key="view.key"
        type="button"
        class="product-business-workbench__tab"
        :class="{ 'product-business-workbench__tab--active': activeView === view.key }"
        @click="emit('update:activeView', view.key)"
      >
        {{ view.label }}
      </button>
    </nav>

    <section class="product-business-workbench__stage">
      <div
        data-view="overview"
        class="product-business-workbench__view"
        :class="{ 'product-business-workbench__view--active': activeView === 'overview' }"
        v-show="activeView === 'overview'"
      >
        <slot name="overview" />
      </div>
      <div
        data-view="models"
        class="product-business-workbench__view"
        :class="{ 'product-business-workbench__view--active': activeView === 'models' }"
        v-show="activeView === 'models'"
      >
        <slot name="models" />
      </div>
      <div
        data-view="devices"
        class="product-business-workbench__view"
        :class="{ 'product-business-workbench__view--active': activeView === 'devices' }"
        v-show="activeView === 'devices'"
      >
        <slot name="devices" />
      </div>
      <div
        data-view="edit"
        class="product-business-workbench__view"
        :class="{ 'product-business-workbench__view--active': activeView === 'edit' }"
        v-show="activeView === 'edit'"
      >
        <slot name="edit" />
      </div>
    </section>

    <template #footer>
      <slot name="footer" />
    </template>
  </StandardDetailDrawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import type { Product } from '@/types/api'

export type ProductBusinessWorkbenchView = 'overview' | 'models' | 'devices' | 'edit'

const props = withDefaults(defineProps<{
  modelValue: boolean
  activeView: ProductBusinessWorkbenchView
  product?: Product | null
}>(), {
  product: null
})

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'update:activeView', value: ProductBusinessWorkbenchView): void
}>()

const viewOptions: Array<{ key: ProductBusinessWorkbenchView; label: string }> = [
  { key: 'overview', label: '经营总览' },
  { key: 'models', label: '物模型治理' },
  { key: 'devices', label: '关联设备' },
  { key: 'edit', label: '编辑治理' }
]

function formatCount(value?: number | null) {
  const count = Number(value)
  return Number.isFinite(count) ? String(count) : '--'
}

function formatText(value?: string | null) {
  if (!value?.trim()) {
    return '--'
  }
  return value
}

const productTitle = computed(() => props.product?.productName || props.product?.productKey || '产品经营工作台')
const productKeyText = computed(() => formatText(props.product?.productKey))
const protocolText = computed(() => formatText(props.product?.protocolCode))
const dataFormatText = computed(() => formatText(props.product?.dataFormat))
const drawerSubtitle = computed(() => '把经营总览、物模型治理、关联设备和编辑治理收口到同一产品上下文中。')
const nodeTypeText = computed(() => {
  if (props.product?.nodeType === 1) {
    return '直连设备'
  }
  if (props.product?.nodeType === 2) {
    return '网关设备'
  }
  return '--'
})

const heroMetrics = computed(() => [
  {
    key: 'deviceCount',
    label: '关联设备',
    value: formatCount(props.product?.deviceCount),
    hint: '当前产品已建档设备总量'
  },
  {
    key: 'onlineDeviceCount',
    label: '在线设备',
    value: formatCount(props.product?.onlineDeviceCount),
    hint: '当前仍在线的设备数量'
  },
  {
    key: 'thirtyDaysActiveCount',
    label: '30 日活跃',
    value: formatCount(props.product?.thirtyDaysActiveCount),
    hint: '近 30 天有上报的设备数'
  }
])
</script>

<style scoped>
.product-business-workbench__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(0, 0.82fr);
  gap: 1rem;
  padding: 1.05rem 1.1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 14%, var(--panel-border));
  border-radius: calc(var(--radius-xl) + 2px);
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 10%, transparent), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 249, 255, 0.95));
  box-shadow: var(--shadow-surface-soft-sm);
}

.product-business-workbench__hero-copy,
.product-business-workbench__hero-metrics,
.product-business-workbench__stage {
  display: grid;
  gap: 0.8rem;
}

.product-business-workbench__eyebrow {
  margin: 0;
  color: color-mix(in srgb, var(--brand) 60%, var(--text-caption));
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.product-business-workbench__hero-copy h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: clamp(1.28rem, 2vw, 1.72rem);
  line-height: 1.18;
}

.product-business-workbench__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
  margin: 0;
}

.product-business-workbench__summary span {
  display: inline-flex;
  align-items: center;
  min-height: 1.9rem;
  padding: 0.3rem 0.7rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.9);
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.product-business-workbench__hero-metrics {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.product-business-workbench__metric {
  display: grid;
  gap: 0.28rem;
  padding: 0.92rem 0.95rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.94);
}

.product-business-workbench__metric span,
.product-business-workbench__metric small {
  color: var(--text-caption);
}

.product-business-workbench__metric strong {
  color: var(--text-heading);
  font-size: 1.18rem;
}

.product-business-workbench__tabs {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.55rem;
  margin-top: 1rem;
}

.product-business-workbench__tab {
  min-height: 2.2rem;
  padding: 0.45rem 0.9rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.9);
  color: var(--text-caption);
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.product-business-workbench__tab--active {
  border-color: color-mix(in srgb, var(--brand) 34%, white);
  background: linear-gradient(180deg, rgba(245, 248, 255, 0.98), rgba(237, 244, 255, 0.96));
  color: var(--brand);
  box-shadow: var(--shadow-surface-soft-sm);
}

.product-business-workbench__stage {
  margin-top: 1rem;
}

.product-business-workbench__view {
  display: grid;
  gap: 0.8rem;
}

@media (max-width: 960px) {
  .product-business-workbench__hero,
  .product-business-workbench__hero-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
