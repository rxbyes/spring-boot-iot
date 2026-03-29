<template>
  <el-button
    v-bind="attrs"
    :type="resolvedType"
    :plain="resolvedPlain"
    :link="resolvedLink"
    :text="resolvedText"
    :size="resolvedSize"
    :loading="loading"
    :disabled="disabled"
    :icon="icon"
    :native-type="nativeType"
    :circle="circle"
    :round="round"
    :autofocus="autofocus"
    class="standard-button"
    :class="classes"
  >
    <span class="standard-button__content">
      <slot />
    </span>
  </el-button>
</template>

<script setup lang="ts">
import { computed, useAttrs } from 'vue'
import type { Component } from 'vue'

defineOptions({
  inheritAttrs: false
})

type ButtonType = '' | 'primary' | 'success' | 'warning' | 'info' | 'danger'
type ButtonSize = '' | 'large' | 'default' | 'small'
type ButtonAction = 'default' | 'query' | 'add' | 'reset' | 'delete' | 'batch' | 'refresh' | 'confirm' | 'cancel'
type ButtonTone = 'solid' | 'secondary' | 'link' | 'text'

const attrs = useAttrs()

const props = withDefaults(
  defineProps<{
    action?: ButtonAction
    type?: ButtonType
    size?: ButtonSize
    loading?: boolean
    disabled?: boolean
    plain?: boolean | null
    link?: boolean | null
    text?: boolean | null
    icon?: string | Component
    nativeType?: 'button' | 'submit' | 'reset'
    circle?: boolean
    round?: boolean
    autofocus?: boolean
  }>(),
  {
    action: 'default',
    type: '',
    size: 'default',
    loading: false,
    disabled: false,
    plain: null,
    link: null,
    text: null,
    icon: undefined,
    nativeType: 'button',
    circle: false,
    round: false,
    autofocus: false
  }
)

const actionDefaults: Record<ButtonAction, { type: ButtonType; tone: ButtonTone; plain?: boolean; link?: boolean; text?: boolean }> = {
  default: { type: '', tone: 'secondary' },
  query: { type: 'primary', tone: 'solid' },
  add: { type: 'primary', tone: 'solid' },
  reset: { type: '', tone: 'secondary' },
  delete: { type: 'primary', tone: 'solid' },
  batch: { type: 'primary', tone: 'solid' },
  refresh: { type: '', tone: 'secondary' },
  confirm: { type: 'primary', tone: 'solid' },
  cancel: { type: '', tone: 'secondary' }
}

const resolvedType = computed<ButtonType>(() => props.type || actionDefaults[props.action].type)
const resolvedPlain = computed(() => props.plain ?? actionDefaults[props.action].plain ?? false)
const resolvedLink = computed(() => props.link ?? actionDefaults[props.action].link ?? false)
const resolvedText = computed(() => props.text ?? actionDefaults[props.action].text ?? false)
const resolvedSize = computed<ButtonSize>(() => props.size || 'default')
const resolvedTone = computed<ButtonTone>(() => {
  if (resolvedLink.value) {
    return 'link'
  }
  if (resolvedText.value) {
    return 'text'
  }
  return actionDefaults[props.action].tone
})

const resolvedPalette = computed<'query' | 'brand' | 'neutral'>(() => {
  if (props.action === 'query') {
    return 'query'
  }

  if (['add', 'batch', 'confirm', 'delete'].includes(props.action)) {
    return 'brand'
  }

  return 'neutral'
})

const classes = computed(() => [
  `standard-button--${props.action}`,
  `standard-button--tone-${resolvedTone.value}`,
  `standard-button--palette-${resolvedPalette.value}`,
  {
    'standard-button--plain': resolvedPlain.value,
    'standard-button--link': resolvedLink.value,
    'standard-button--text': resolvedText.value
  }
])
</script>

<style scoped>
.standard-button {
  --standard-button-shadow: none;
}

.standard-button__content {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  white-space: nowrap;
}

.standard-button--tone-solid:not(.standard-button--plain):not(.standard-button--link):not(.standard-button--text) {
  box-shadow: var(--standard-button-shadow);
}

.standard-button--query,
.standard-button--add,
.standard-button--batch,
.standard-button--confirm,
.standard-button--delete {
  --standard-button-shadow: var(--shadow-brand);
}

.standard-button--palette-query {
  --standard-button-shadow: var(--button-query-shadow);
}

.standard-button--palette-query.el-button--primary:not(.is-plain):not(.is-link):not(.is-text) {
  background: var(--button-query-bg);
  color: var(--button-query-text);
}

.standard-button--palette-query.el-button--primary:not(.is-plain):not(.is-link):not(.is-text):hover {
  background: var(--button-query-hover-bg);
  color: var(--button-query-hover-text);
}

.standard-button--palette-query.el-button--primary:not(.is-plain):not(.is-link):not(.is-text):active {
  background: var(--button-query-active-bg);
}

.standard-button--palette-query.el-button--primary.is-disabled,
.standard-button--palette-query.el-button--primary.is-disabled:hover {
  background: var(--button-query-disabled-bg);
  color: var(--button-query-disabled-text);
}
</style>
