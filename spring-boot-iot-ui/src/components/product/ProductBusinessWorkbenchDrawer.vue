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

    <section class="product-business-workbench__header">
      <div class="product-business-workbench__header-top">
        <div class="product-business-workbench__identity">
          <p class="product-business-workbench__kicker product-business-workbench__header-kicker">产品经营工作台</p>
          <h3 class="product-business-workbench__headline">{{ productTitle }}</h3>
          <p class="product-business-workbench__identity-key">{{ productKeyText }}</p>
          <p class="product-business-workbench__description product-business-workbench__brief">{{ headerDescription }}</p>
        </div>
        <span
          class="product-business-workbench__status-badge"
          :class="`product-business-workbench__status-badge--${statusTone}`"
        >
          {{ statusLabel }}
        </span>
      </div>

      <div class="product-business-workbench__meta product-business-workbench__meta-strip">
        <span>{{ productKeyText }}</span>
        <span>{{ protocolText }}</span>
        <span>{{ nodeTypeText }}</span>
        <span>{{ dataFormatText }}</span>
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
const statusLabel = computed(() => {
  if (props.product?.status === 0) {
    return '已停用'
  }
  if ((props.product?.deviceCount ?? 0) > 0) {
    return '稳定使用中'
  }
  return '接入调试中'
})
const statusTone = computed(() => (props.product?.status === 0 ? 'inactive' : 'active'))
const headerDescription = computed(() => {
  if (props.product?.status === 0) {
    return '当前产品已停用，建议结合关联设备和接入链路继续核查存量影响。'
  }
  if ((props.product?.deviceCount ?? 0) > 0) {
    return '统一在同一产品上下文中查看经营状态、模型治理、关联设备和档案变更。'
  }
  return '当前产品仍在接入准备阶段，可继续补齐经营判断、模型治理和基础档案。'
})
</script>

<style scoped>
.product-business-workbench__header {
  display: grid;
  gap: 1rem;
  padding: 1.25rem 1.28rem 1.15rem;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  border-radius: calc(var(--radius-xl) + 4px);
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 7%, transparent), transparent 34%),
    linear-gradient(180deg, rgba(249, 251, 254, 0.99), rgba(255, 255, 255, 0.99));
  box-shadow: 0 18px 42px rgba(28, 53, 87, 0.08);
}

.product-business-workbench__header-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.product-business-workbench__identity,
.product-business-workbench__view,
.product-business-workbench__view-shell {
  display: grid;
  gap: 0.8rem;
}

.product-business-workbench__kicker,
.product-business-workbench__identity-key,
.product-business-workbench__description {
  margin: 0;
  color: var(--text-caption);
  font-size: 0.84rem;
  line-height: 1.7;
}

.product-business-workbench__identity-key {
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.product-business-workbench__kicker {
  color: color-mix(in srgb, var(--brand) 62%, var(--text-caption));
  font-weight: 700;
  letter-spacing: 0.05em;
}

.product-business-workbench__headline {
  margin: 0;
  color: var(--text-heading);
  font-size: clamp(1.68rem, 2.3vw, 2.35rem);
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.product-business-workbench__brief {
  max-width: 52rem;
  color: color-mix(in srgb, var(--text-secondary) 92%, white);
}

.product-business-workbench__status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.4rem;
  padding: 0.34rem 0.98rem;
  border: 1px solid color-mix(in srgb, var(--brand) 22%, transparent);
  border-radius: var(--radius-pill);
  background: linear-gradient(180deg, rgba(255, 250, 247, 0.98), rgba(255, 244, 236, 0.98));
  color: color-mix(in srgb, var(--brand) 78%, var(--text-heading));
  font-size: 0.8rem;
  font-weight: 700;
  white-space: nowrap;
  box-shadow: 0 10px 20px rgba(217, 120, 47, 0.12);
}

.product-business-workbench__status-badge--inactive {
  border-color: color-mix(in srgb, var(--danger, #d84f45) 18%, transparent);
  background: linear-gradient(180deg, rgba(255, 248, 247, 0.98), rgba(255, 240, 239, 0.98));
  color: color-mix(in srgb, var(--danger, #d84f45) 82%, var(--text-heading));
  box-shadow: 0 10px 20px rgba(216, 79, 69, 0.1);
}

.product-business-workbench__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.58rem;
}

.product-business-workbench__meta-strip {
  padding-bottom: 0.1rem;
}

.product-business-workbench__meta span {
  display: inline-flex;
  align-items: center;
  min-height: 1.96rem;
  padding: 0.28rem 0.78rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.96);
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.product-business-workbench__tabs {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.product-business-workbench__tab-rail {
  padding-top: 0.1rem;
}

.product-business-workbench__tab {
  min-height: 2.28rem;
  padding: 0.42rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.96);
  color: var(--text-secondary);
  font-size: 0.92rem;
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
  margin-top: 1.15rem;
}

@media (max-width: 960px) {
  .product-business-workbench__header-top {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
