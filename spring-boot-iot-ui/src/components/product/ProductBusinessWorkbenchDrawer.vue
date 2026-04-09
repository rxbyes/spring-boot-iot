<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    class="product-business-workbench"
    size="72rem"
    :title="drawerTitle"
    @update:modelValue="emit('update:modelValue', $event)"
  >
    <template #header-actions>
      <div class="product-business-workbench__header-action-slot">
        <slot name="header-actions" />
      </div>
    </template>

    <section class="product-business-workbench__header">
      <div class="product-business-workbench__journal-head">
        <div class="product-business-workbench__journal-masthead">
          <article class="product-business-workbench__hero-card" data-testid="product-overview-card">
            <div class="product-business-workbench__title-column">
              <span class="product-business-workbench__journal-kicker">产品经营页</span>
              <h3 class="product-business-workbench__journal-title">{{ productHeadline }}</h3>
              <p class="product-business-workbench__journal-summary">
                统一维护产品档案、关联设备和契约字段，当前工作台已收口到同一套正式治理路径。
              </p>
            </div>
          </article>

          <article class="product-business-workbench__metric-card" data-testid="related-device-card">
            <span class="product-business-workbench__metric-label">关联设备</span>
            <strong class="product-business-workbench__metric-value">{{ totalDevicesText }}</strong>
          </article>
        </div>

        <div class="product-business-workbench__identity-row" role="list" aria-label="产品身份信息">
          <span class="product-business-workbench__identity-item product-business-workbench__identity-item--key" role="listitem">
            <span class="product-business-workbench__identity-label product-business-workbench__copy-label">产品Key</span>
            <span
              class="product-business-workbench__identity-value product-business-workbench__identity-value--key product-business-workbench__copy-value"
              data-testid="product-key-hero"
            >
              {{ productKeyText }}
            </span>
          </span>
          <span class="product-business-workbench__identity-item" role="listitem">
            <span class="product-business-workbench__identity-label product-business-workbench__copy-label">接入协议</span>
            <span class="product-business-workbench__identity-value product-business-workbench__identity-value--detail product-business-workbench__copy-value">{{ protocolText }}</span>
          </span>
          <span class="product-business-workbench__identity-item" role="listitem">
            <span class="product-business-workbench__identity-label product-business-workbench__copy-label">节点类型</span>
            <span class="product-business-workbench__identity-value product-business-workbench__identity-value--detail product-business-workbench__copy-value">{{ nodeTypeText }}</span>
          </span>
          <span class="product-business-workbench__identity-item" role="listitem">
            <span class="product-business-workbench__identity-label product-business-workbench__copy-label">数据格式</span>
            <span class="product-business-workbench__identity-value product-business-workbench__identity-value--detail product-business-workbench__copy-value">{{ dataFormatText }}</span>
          </span>
        </div>

        <div class="product-business-workbench__tab-strip">
          <nav class="product-business-workbench__tabs" aria-label="产品经营工作台视图">
            <button
              v-for="view in viewOptions"
              :key="view.key"
              type="button"
              class="product-business-workbench__tab"
              :class="{ 'product-business-workbench__tab--active': activeView === view.key }"
              @click="emit('update:activeView', view.key)"
            >
              <span class="product-business-workbench__tab-label">{{ view.label }}</span>
            </button>
          </nav>
        </div>
      </div>
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

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    activeView: ProductBusinessWorkbenchView
    product?: Product | null
  }>(),
  {
    product: null
  }
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'update:activeView', value: ProductBusinessWorkbenchView): void
}>()

const viewOptions: Array<{ key: Exclude<ProductBusinessWorkbenchView, 'edit'>; label: string }> = [
  { key: 'overview', label: '产品总览' },
  { key: 'devices', label: '关联设备' },
  { key: 'models', label: '契约字段' }
]

function formatText(value?: string | null) {
  if (!value?.trim()) {
    return '--'
  }
  return value
}

const drawerTitle = '产品经营工作台'
const productKeyText = computed(() => formatText(props.product?.productKey))
const protocolText = computed(() => formatText(props.product?.protocolCode))
const dataFormatText = computed(() => formatText(props.product?.dataFormat))
const productHeadline = computed(() => {
  const text = formatText(props.product?.productName || props.product?.productKey)
  return text === '--' ? '未命名产品' : text
})
const totalDevices = computed(() => {
  const count = Number(props.product?.deviceCount ?? 0)
  return Number.isFinite(count) && count > 0 ? count : 0
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
const totalDevicesText = computed(() => {
  return `${totalDevices.value} 台`
})
</script>

<style scoped>
.product-business-workbench__header {
  padding: 1.26rem 1.4rem 0.9rem;
  border-bottom: 1px solid var(--panel-border);
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 5%, transparent), transparent 20rem),
    linear-gradient(180deg, color-mix(in srgb, var(--brand-light) 30%, white), rgba(255, 255, 255, 0.99) 58%);
}

.product-business-workbench__journal-head,
.product-business-workbench__journal-masthead,
.product-business-workbench__title-column,
.product-business-workbench__header-action-slot,
.product-business-workbench__view,
.product-business-workbench__view-shell {
  display: grid;
}

.product-business-workbench__header-action-slot {
  align-items: center;
}

.product-business-workbench__journal-head {
  gap: 0.78rem;
}

.product-business-workbench__journal-masthead {
  grid-template-columns: minmax(0, 1fr) minmax(12rem, 15rem);
  gap: 0.72rem;
  align-items: stretch;
}

.product-business-workbench__hero-card,
.product-business-workbench__metric-card {
  border: 1px solid color-mix(in srgb, var(--panel-border) 84%, white);
  border-radius: 0.88rem;
  background: rgba(255, 255, 255, 0.92);
  height: 100%;
}

.product-business-workbench__hero-card {
  padding: 0.88rem 0.94rem;
}

.product-business-workbench__title-column {
  gap: 0.42rem;
}

.product-business-workbench__journal-kicker {
  display: inline-flex;
  width: max-content;
  color: color-mix(in srgb, var(--brand) 68%, var(--text-caption));
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.product-business-workbench__journal-title {
  margin: 0;
  color: var(--text-heading);
  font-family: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', serif;
  font-size: clamp(1.52rem, 2.5vw, 2.06rem);
  font-weight: 700;
  line-height: 1.16;
  letter-spacing: 0.01em;
}

.product-business-workbench__journal-summary {
  margin: 0;
  max-width: 33rem;
  color: var(--text-secondary);
  font-size: 0.84rem;
  line-height: 1.52;
}

.product-business-workbench__metric-card {
  gap: 0.22rem;
  padding: 0.82rem 0.94rem;
  display: grid;
  align-content: center;
}

.product-business-workbench__metric-label,
.product-business-workbench__identity-row {
  color: var(--text-secondary);
}

.product-business-workbench__copy-label {
  font-size: 0.8rem;
  line-height: 1.5;
}

.product-business-workbench__copy-value {
  font-size: 1.1rem;
  line-height: 1.34;
}

.product-business-workbench__metric-label {
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.product-business-workbench__metric-value {
  color: var(--text-heading);
  font-family: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', serif;
  font-size: clamp(1.7rem, 2.4vw, 2.1rem);
  line-height: 1;
}

.product-business-workbench__identity-row {
  display: grid;
  grid-template-columns: minmax(0, 1.42fr) repeat(3, minmax(0, 1fr));
  gap: 0.56rem;
}

.product-business-workbench__identity-item {
  display: grid;
  gap: 0.08rem;
  padding: 0.42rem 0.56rem;
  border-radius: 0.65rem;
  background: color-mix(in srgb, var(--brand-light) 26%, white);
}

.product-business-workbench__identity-item--key {
  min-width: 0;
}

.product-business-workbench__identity-label {
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.product-business-workbench__identity-value {
  color: var(--text-primary);
  word-break: break-all;
}

.product-business-workbench__identity-value--key {
  color: var(--text-heading);
  font-family: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', serif;
}

.product-business-workbench__identity-value--detail {
  font-size: 1.02rem;
  line-height: 1.34;
}

.product-business-workbench__tab-strip {
  padding-top: 0.22rem;
  border-top: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
}

.product-business-workbench__tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.46rem 0.56rem;
}

.product-business-workbench__tab {
  position: relative;
  padding: 0.28rem 0.68rem;
  border: 0;
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
}

.product-business-workbench__tab::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -0.2rem;
  height: 2px;
  border-radius: var(--radius-pill);
  background: transparent;
}

.product-business-workbench__tab--active {
  color: color-mix(in srgb, var(--brand) 76%, var(--text-heading));
  background: color-mix(in srgb, var(--brand-light) 18%, white);
}

.product-business-workbench__tab--active::after {
  background: color-mix(in srgb, var(--brand) 34%, transparent);
}

.product-business-workbench__tab-label {
  font-size: 0.88rem;
  font-weight: 600;
  line-height: 1.34;
}

.product-business-workbench__view-shell {
  padding: 1.12rem 1.4rem 1.4rem;
}

.product-business-workbench__view {
  min-width: 0;
}

@media (max-width: 900px) {
  .product-business-workbench__header,
  .product-business-workbench__view-shell {
    padding-right: 0.92rem;
    padding-left: 0.92rem;
  }

  .product-business-workbench__journal-masthead {
    grid-template-columns: 1fr;
    gap: 0.56rem;
  }

  .product-business-workbench__identity-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .product-business-workbench__identity-item--key {
    grid-column: 1 / -1;
  }
}
</style>
