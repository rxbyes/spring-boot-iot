<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    class="product-business-workbench"
    size="72rem"
    :title="drawerTitle"
    @update:modelValue="emit('update:modelValue', $event)"
  >
    <template #header-actions>
      <slot name="header-actions" />
    </template>

    <section class="product-business-workbench__header">
      <div class="product-business-workbench__header-top">
        <div class="product-business-workbench__identity">
          <p class="product-business-workbench__kicker product-business-workbench__header-kicker">产品经营工作台</p>
          <h3 class="product-business-workbench__headline">{{ productHeadline }}</h3>
        </div>

        <div class="product-business-workbench__header-summary">
          <article
            v-for="card in headerSummaryCards"
            :key="card.key"
            class="product-business-workbench__summary-card"
            :class="{ 'product-business-workbench__summary-card--primary': card.primary }"
            :data-testid="`product-workbench-summary-${card.key}`"
          >
            <span class="product-business-workbench__summary-label">{{ card.label }}</span>
            <span class="product-business-workbench__summary-value">{{ card.value }}</span>
          </article>
        </div>
      </div>

      <div v-if="metaItems.length" class="product-business-workbench__meta product-business-workbench__meta-strip">
        <span v-for="item in metaItems" :key="item.key">{{ item.value }}</span>
      </div>

      <nav class="product-business-workbench__tabs product-business-workbench__tab-rail" aria-label="产品经营工作台视图">
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
    </section>

    <section class="product-business-workbench__view-shell">
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

function formatText(value?: string | null) {
  if (!value?.trim()) {
    return '--'
  }
  return value
}

function formatCount(value?: number | null) {
  if (value === undefined || value === null) {
    return '--'
  }
  const count = Number(value)
  return Number.isFinite(count) ? String(count) : '--'
}

const drawerTitle = '产品经营工作台'
const productKeyText = computed(() => formatText(props.product?.productKey))
const protocolText = computed(() => formatText(props.product?.protocolCode))
const dataFormatText = computed(() => formatText(props.product?.dataFormat))
const productHeadline = computed(() => {
  const text = formatText(props.product?.productName || props.product?.productKey)
  return text === '--' ? '未命名产品' : text
})
const nodeTypeText = computed(() => {
  if (props.product?.nodeType === 1) {
    return '直连设备'
  }
  if (props.product?.nodeType === 2) {
    return '网关设备'
  }
  return '--'
})
const metaItems = computed(() => {
  const items = [
    {
      key: 'productKey',
      value: productKeyText.value,
      visible: productKeyText.value !== '--' && productKeyText.value !== productHeadline.value
    },
    {
      key: 'protocolCode',
      value: protocolText.value,
      visible: protocolText.value !== '--'
    },
    {
      key: 'nodeType',
      value: nodeTypeText.value,
      visible: nodeTypeText.value !== '--'
    },
    {
      key: 'dataFormat',
      value: dataFormatText.value,
      visible: dataFormatText.value !== '--'
    }
  ]

  return items.filter((item) => item.visible)
})
const headerSummaryCards = computed(() => [
  {
    key: 'deviceCount',
    label: '关联设备',
    value: formatCount(props.product?.deviceCount),
    primary: true
  },
  {
    key: 'onlineDeviceCount',
    label: '在线设备',
    value: formatCount(props.product?.onlineDeviceCount),
    primary: false
  },
  {
    key: 'thirtyDaysActiveCount',
    label: '30 日活跃',
    value: formatCount(props.product?.thirtyDaysActiveCount),
    primary: false
  }
])
</script>

<style scoped>
.product-business-workbench__header {
  display: grid;
  gap: 1rem;
  padding: 1.4rem 1.44rem 1.16rem;
  border: 1px solid color-mix(in srgb, var(--brand) 13%, var(--panel-border));
  border-radius: calc(var(--radius-xl) + 6px);
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 8%, transparent), transparent 34%),
    linear-gradient(180deg, rgba(249, 251, 254, 0.99), rgba(255, 255, 255, 0.99));
  box-shadow: 0 20px 44px rgba(28, 53, 87, 0.08);
}

.product-business-workbench__header-top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(20rem, 23rem);
  gap: 1.4rem;
  align-items: stretch;
}

.product-business-workbench__identity,
.product-business-workbench__view,
.product-business-workbench__view-shell {
  display: grid;
  gap: 0.56rem;
}

.product-business-workbench__kicker,
.product-business-workbench__summary-label {
  margin: 0;
  color: var(--text-caption);
  font-size: 0.84rem;
  line-height: 1.72;
}

.product-business-workbench__kicker {
  color: color-mix(in srgb, var(--brand) 62%, var(--text-caption));
  font-weight: 650;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.product-business-workbench__headline {
  margin: 0;
  color: var(--text-heading);
  font-size: clamp(1.76rem, 2.4vw, 2.46rem);
  line-height: 1.1;
  letter-spacing: -0.03em;
}

.product-business-workbench__header-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-auto-rows: minmax(5.1rem, auto);
  gap: 0.82rem;
  align-self: stretch;
}

.product-business-workbench__summary-card {
  display: grid;
  gap: 0.16rem;
  align-content: center;
  min-height: 5.1rem;
  padding: 0.92rem 0.96rem;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 2px);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 12px 24px rgba(28, 53, 87, 0.05);
}

.product-business-workbench__summary-card--primary {
  grid-column: 1 / -1;
  min-height: 6.2rem;
  padding-block: 1.02rem;
  background:
    linear-gradient(180deg, rgba(255, 249, 244, 0.98), rgba(255, 255, 255, 0.98));
}

.product-business-workbench__summary-label {
  font-size: 0.76rem;
  font-weight: 500;
  letter-spacing: 0.04em;
}

.product-business-workbench__summary-value {
  color: var(--text-heading);
  font-size: 1.16rem;
  font-weight: 500;
  line-height: 1.35;
}

.product-business-workbench__summary-card--primary .product-business-workbench__summary-value {
  font-size: 1.58rem;
}

.product-business-workbench__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.58rem;
}

.product-business-workbench__meta-strip {
  padding-bottom: 0.14rem;
}

.product-business-workbench__meta span {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0.28rem 0.82rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.96);
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.product-business-workbench__tabs {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.58rem;
}

.product-business-workbench__tab-rail {
  padding-top: 0.12rem;
}

.product-business-workbench__tab {
  min-height: 2.38rem;
  padding: 0.44rem 1.06rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.96);
  color: var(--text-secondary);
  font-size: 0.93rem;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.product-business-workbench__tab--active {
  border-color: color-mix(in srgb, var(--brand) 28%, white);
  background: linear-gradient(180deg, rgba(255, 249, 244, 0.98), rgba(255, 245, 236, 0.98));
  color: var(--brand);
  box-shadow: 0 12px 20px rgba(217, 120, 47, 0.12);
}

.product-business-workbench__view-shell {
  margin-top: 1.22rem;
}

@media (max-width: 960px) {
  .product-business-workbench__header-top {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .product-business-workbench__header-summary {
    grid-template-columns: 1fr;
  }
}
</style>
