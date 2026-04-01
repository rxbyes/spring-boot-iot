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
      <div class="product-business-workbench__exhibit-head">
        <div class="product-business-workbench__exhibit-copy">
          <span class="product-business-workbench__eyebrow">产品展陈</span>
          <h3 class="product-business-workbench__headline">{{ productHeadline }}</h3>
          <p class="product-business-workbench__status-statement">{{ statusStatement }}</p>
        </div>

        <div v-if="metaItems.length" class="product-business-workbench__meta-inline">
          <span
            v-for="item in metaItems"
            :key="item.key"
            class="product-business-workbench__meta-item"
          >
            {{ item.value }}
          </span>
        </div>

        <div class="product-business-workbench__tab-index">
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
const statusStatement = computed(() => {
  if (!props.product) {
    return '选择产品后进入统一经营工作台。'
  }

  if (props.product.status === 0) {
    return '产品已停用，当前以档案核对为主。'
  }

  const totalDevices = Number(props.product.deviceCount ?? 0)
  const onlineDevices = Number(props.product.onlineDeviceCount ?? 0)
  if (totalDevices <= 0) {
    return '产品已建档，待首批设备进入运行。'
  }

  const onlineCoverage = Math.round((onlineDevices / totalDevices) * 100)
  if (onlineCoverage >= 60) {
    return '接入运行稳定，当前聚焦契约与台账。'
  }
  if (onlineCoverage > 0) {
    return '已进入运行期，当前继续提升在线覆盖。'
  }
  return '已有设备接入，在线覆盖尚未形成稳定基线。'
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
</script>

<style scoped>
.product-business-workbench__header {
  padding: 1.9rem 2rem 1.3rem;
  border-bottom: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  background:
    radial-gradient(circle at top left, rgba(236, 229, 220, 0.58), transparent 28%),
    linear-gradient(180deg, rgba(248, 245, 239, 0.96), rgba(255, 255, 255, 0.98) 38%),
    linear-gradient(180deg, rgba(252, 253, 255, 0.99), rgba(255, 255, 255, 0.99));
}

.product-business-workbench__exhibit-head,
.product-business-workbench__exhibit-copy,
.product-business-workbench__view,
.product-business-workbench__view-shell {
  display: grid;
}

.product-business-workbench__exhibit-head {
  gap: 1.24rem;
}

.product-business-workbench__exhibit-copy {
  gap: 0.6rem;
}

.product-business-workbench__eyebrow {
  display: inline-flex;
  align-items: center;
  width: max-content;
  padding-bottom: 0.4rem;
  border-bottom: 1px solid color-mix(in srgb, var(--brand) 20%, var(--panel-border));
  color: color-mix(in srgb, var(--brand) 68%, var(--text-caption));
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.product-business-workbench__status-statement {
  margin: 0;
  max-width: 36rem;
  color: var(--text-secondary);
  font-size: 0.92rem;
  line-height: 1.78;
}

.product-business-workbench__headline {
  margin: 0;
  color: var(--text-heading);
  font-size: clamp(2.1rem, 2.9vw, 2.9rem);
  line-height: 1.02;
  letter-spacing: -0.05em;
}

.product-business-workbench__meta-inline {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.44rem 0.96rem;
  color: var(--text-secondary);
  font-size: 0.8rem;
  line-height: 1.7;
}

.product-business-workbench__meta-item {
  position: relative;
  padding-right: 0.92rem;
}

.product-business-workbench__meta-item:last-child {
  padding-right: 0;
}

.product-business-workbench__meta-item + .product-business-workbench__meta-item::before {
  content: '';
  position: absolute;
  left: -0.52rem;
  top: 50%;
  width: 1px;
  height: 0.64rem;
  background: color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  transform: translateY(-50%);
}

.product-business-workbench__tab-index {
  display: grid;
  gap: 0.42rem;
  padding-top: 0.82rem;
  border-top: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
}

.product-business-workbench__tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
}

.product-business-workbench__tab {
  min-height: 2rem;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: var(--text-secondary);
  font-size: 0.94rem;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s ease;
}

.product-business-workbench__tab-label {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding-bottom: 0.26rem;
  border-bottom: 1px solid transparent;
}

.product-business-workbench__tab--active {
  color: var(--brand);
}

.product-business-workbench__tab--active .product-business-workbench__tab-label {
  border-bottom-color: color-mix(in srgb, var(--brand) 22%, var(--panel-border));
}

.product-business-workbench__view-shell {
  gap: 0.56rem;
  margin-top: 1.42rem;
  max-width: 65rem;
  margin-inline: auto;
}

@media (max-width: 960px) {
  .product-business-workbench__tabs {
    gap: 1rem 1.24rem;
  }
}

@media (max-width: 720px) {
  .product-business-workbench__header {
    padding-inline: 1.2rem;
  }

  .product-business-workbench__tabs {
    gap: 0.88rem 1rem;
  }
}
</style>
