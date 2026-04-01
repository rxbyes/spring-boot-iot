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
      <div class="product-business-workbench__header-main">
        <div class="product-business-workbench__identity">
          <h3 class="product-business-workbench__headline">{{ productHeadline }}</h3>
          <p class="product-business-workbench__status-statement">{{ statusStatement }}</p>

          <div v-if="metaItems.length" class="product-business-workbench__meta-inline">
            <span
              v-for="item in metaItems"
              :key="item.key"
              class="product-business-workbench__meta-item"
            >
              {{ item.value }}
            </span>
          </div>
        </div>
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
    return '当前状态：等待选择产品后进入经营工作台。'
  }

  if (props.product.status === 0) {
    return '当前状态：产品已停用，当前工作台仅保留历史档案与治理核对。'
  }

  const totalDevices = Number(props.product.deviceCount ?? 0)
  const onlineDevices = Number(props.product.onlineDeviceCount ?? 0)
  if (totalDevices <= 0) {
    return '当前状态：产品已完成建档，等待首批设备进入接入运行。'
  }

  const onlineCoverage = Math.round((onlineDevices / totalDevices) * 100)
  if (onlineCoverage >= 60) {
    return '当前状态：设备接入已形成稳定运行，可继续围绕契约与台账治理收口。'
  }
  if (onlineCoverage > 0) {
    return '当前状态：产品已进入运行期，在线覆盖仍有继续提升空间。'
  }
  return '当前状态：设备已接入，但在线覆盖尚未形成稳定基线。'
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
  padding: 1.32rem 1.48rem 1rem;
  border-bottom: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  background: linear-gradient(180deg, rgba(252, 253, 255, 0.99), rgba(255, 255, 255, 0.99));
}

.product-business-workbench__header-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.2rem 1.4rem;
}

.product-business-workbench__identity,
.product-business-workbench__view,
.product-business-workbench__view-shell {
  display: grid;
  gap: 0.56rem;
}

.product-business-workbench__status-statement {
  margin: 0;
  max-width: 38rem;
  color: var(--text-secondary);
  font-size: 0.9rem;
  line-height: 1.72;
}

.product-business-workbench__headline {
  margin: 0;
  color: var(--text-heading);
  font-size: clamp(1.72rem, 2.25vw, 2.22rem);
  line-height: 1.1;
  letter-spacing: -0.04em;
}

.product-business-workbench__meta-inline {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.38rem 0.86rem;
  color: var(--text-secondary);
  font-size: 0.84rem;
  line-height: 1.64;
}

.product-business-workbench__meta-item {
  position: relative;
}

.product-business-workbench__meta-item + .product-business-workbench__meta-item::before {
  content: '';
  position: absolute;
  left: -0.48rem;
  top: 50%;
  width: 1px;
  height: 0.72rem;
  background: color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  transform: translateY(-50%);
}

.product-business-workbench__tabs {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.58rem;
  min-width: max-content;
}

.product-business-workbench__tab {
  min-height: 2.32rem;
  padding: 0.28rem 0.94rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.98);
  color: var(--text-secondary);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease, background-color 0.2s ease;
}

.product-business-workbench__tab--active {
  border-color: color-mix(in srgb, var(--brand) 24%, var(--panel-border));
  background: linear-gradient(180deg, rgba(255, 248, 241, 0.98), rgba(255, 252, 248, 0.98));
  color: var(--brand);
}

.product-business-workbench__view-shell {
  margin-top: 1.08rem;
}

@media (max-width: 960px) {
  .product-business-workbench__header-main {
    flex-direction: column;
    align-items: stretch;
  }

  .product-business-workbench__tabs {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .product-business-workbench__header {
    padding-inline: 1.16rem;
  }

  .product-business-workbench__tabs {
    gap: 0.5rem;
  }
}
</style>
