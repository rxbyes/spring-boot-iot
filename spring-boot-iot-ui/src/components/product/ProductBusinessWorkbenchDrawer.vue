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
      <div class="product-business-workbench__journal-head">
        <div class="product-business-workbench__journal-masthead">
          <div class="product-business-workbench__title-column">
            <span class="product-business-workbench__journal-kicker">产品经营页</span>
            <h3 class="product-business-workbench__journal-title">{{ productHeadline }}</h3>
            <p class="product-business-workbench__journal-summary">{{ statusStatement }}</p>
          </div>

          <div class="product-business-workbench__scale-column">
            <div class="product-business-workbench__scale-panel">
              <span class="product-business-workbench__scale-label">核心规模</span>
              <strong class="product-business-workbench__scale-value">{{ scaleValueText }}</strong>
              <span class="product-business-workbench__scale-caption">{{ scaleCaption }}</span>
            </div>
          </div>
        </div>

        <div v-if="metaItems.length" class="product-business-workbench__meta-line">
          <span
            v-for="item in metaItems"
            :key="item.key"
            class="product-business-workbench__meta-point"
          >
            {{ item.value }}
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
const totalDevicesText = computed(() => {
  const count = Number(props.product?.deviceCount ?? 0)
  return Number.isFinite(count) && count > 0 ? String(count) : '--'
})
const scaleCaption = computed(() => (props.product?.status === 0 ? '历史关联设备总量' : '关联设备总量'))
const statusStatement = computed(() => {
  if (!props.product) {
    return '选择产品后进入统一产品经营页。'
  }

  if (props.product.status === 0) {
    return '当前产品处于停用状态，优先核对档案与契约。'
  }

  const totalDevices = Number(props.product.deviceCount ?? 0)
  const onlineDevices = Number(props.product.onlineDeviceCount ?? 0)
  if (totalDevices <= 0) {
    return '当前已完成建档，等待首批设备进入稳定运行。'
  }

  const onlineCoverage = Math.round((onlineDevices / totalDevices) * 100)
  if (onlineCoverage >= 60) {
    return '已进入运行期，当前继续围绕规模、契约与档案校准。'
  }
  if (onlineCoverage > 0) {
    return '运行数据已形成基线，当前继续补齐在线覆盖。'
  }
  return '已有关联设备，当前仍需补齐在线运行基线。'
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
const scaleValueText = computed(() => totalDevicesText.value)
</script>

<style scoped>
.product-business-workbench__header {
  padding: 1.8rem 2rem 1.26rem;
  border-bottom: 1px solid var(--panel-border);
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 7%, transparent), transparent 22rem),
    linear-gradient(180deg, color-mix(in srgb, var(--brand-light) 42%, white), rgba(255, 255, 255, 0.98) 54%);
}

.product-business-workbench__journal-head,
.product-business-workbench__journal-masthead,
.product-business-workbench__title-column,
.product-business-workbench__scale-panel,
.product-business-workbench__view,
.product-business-workbench__view-shell {
  display: grid;
}

.product-business-workbench__journal-head {
  gap: 0.95rem;
}

.product-business-workbench__journal-masthead {
  grid-template-columns: minmax(0, 1.2fr) minmax(11rem, 0.8fr);
  gap: 1.8rem;
  align-items: start;
}

.product-business-workbench__title-column {
  gap: 0.54rem;
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
  font-size: clamp(2.3rem, 3.1vw, 3.15rem);
  font-weight: 700;
  line-height: 1.08;
  letter-spacing: 0.01em;
}

.product-business-workbench__journal-summary {
  margin: 0;
  max-width: 38rem;
  color: var(--text-secondary);
  font-size: 0.92rem;
  line-height: 1.74;
}

.product-business-workbench__scale-column {
  display: flex;
  justify-content: flex-end;
}

.product-business-workbench__scale-panel {
  gap: 0.18rem;
  min-width: 11rem;
  padding: 0.12rem 0 0.12rem 1.4rem;
  border-left: 1px solid var(--panel-border);
  text-align: right;
}

.product-business-workbench__scale-label,
.product-business-workbench__scale-caption,
.product-business-workbench__meta-line {
  color: var(--text-secondary);
}

.product-business-workbench__scale-label {
  font-size: 0.76rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.product-business-workbench__scale-value {
  color: var(--text-heading);
  font-family: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', serif;
  font-size: clamp(2.2rem, 3vw, 3rem);
  line-height: 1;
}

.product-business-workbench__scale-caption {
  font-size: 0.82rem;
  line-height: 1.6;
}

.product-business-workbench__meta-line {
  display: flex;
  flex-wrap: wrap;
  gap: 0.42rem 0.92rem;
  padding-top: 0.72rem;
  border-top: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  font-size: 0.78rem;
  line-height: 1.7;
}

.product-business-workbench__meta-point {
  position: relative;
  padding-right: 0.88rem;
}

.product-business-workbench__meta-point::after {
  content: '';
  position: absolute;
  top: 50%;
  right: 0;
  width: 1px;
  height: 0.74rem;
  background: color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  transform: translateY(-50%);
}

.product-business-workbench__meta-point:last-child {
  padding-right: 0;
}

.product-business-workbench__meta-point:last-child::after {
  display: none;
}

.product-business-workbench__tab-strip {
  padding-top: 0.78rem;
  border-top: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
}

.product-business-workbench__tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.92rem 1.5rem;
}

.product-business-workbench__tab {
  position: relative;
  padding: 0 0 0.58rem;
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
}

.product-business-workbench__tab::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 1px;
  background: transparent;
}

.product-business-workbench__tab--active {
  color: var(--brand);
}

.product-business-workbench__tab--active::after {
  background: color-mix(in srgb, var(--brand) 72%, var(--text-heading));
}

.product-business-workbench__tab-label {
  font-size: 0.96rem;
  font-weight: 600;
  line-height: 1.5;
}

.product-business-workbench__view-shell {
  padding: 1.48rem 2rem 2rem;
}

.product-business-workbench__view {
  min-width: 0;
}

@media (max-width: 900px) {
  .product-business-workbench__header,
  .product-business-workbench__view-shell {
    padding-right: 1.2rem;
    padding-left: 1.2rem;
  }

  .product-business-workbench__journal-masthead {
    grid-template-columns: 1fr;
    gap: 1rem;
  }

  .product-business-workbench__scale-column {
    justify-content: flex-start;
  }

  .product-business-workbench__scale-panel {
    min-width: 0;
    padding-left: 0;
    border-left: 0;
    border-top: 1px solid var(--panel-border);
    padding-top: 0.88rem;
    text-align: left;
  }
}
</style>
