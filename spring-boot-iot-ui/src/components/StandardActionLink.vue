<template>
  <StandardButton
    v-bind="forwardedAttrs"
    :action="resolvedAction"
    :disabled="disabled"
    :loading="loading"
    :icon="icon"
    :native-type="nativeType"
    link
    class="standard-action-link"
  >
    <slot />
  </StandardButton>
</template>

<script setup lang="ts">
import { computed, useAttrs } from 'vue'
import type { Component } from 'vue'

defineOptions({
  inheritAttrs: false
})

type ButtonAction = 'default' | 'query' | 'add' | 'reset' | 'delete' | 'batch' | 'refresh' | 'confirm' | 'cancel'

const props = withDefaults(
  defineProps<{
    action?: ButtonAction
    disabled?: boolean
    loading?: boolean
    icon?: string | Component
    nativeType?: 'button' | 'submit' | 'reset'
  }>(),
  {
    action: undefined,
    disabled: false,
    loading: false,
    icon: undefined,
    nativeType: 'button'
  }
)

const attrs = useAttrs()
const blockedAttrKeys = new Set(['plain', 'text', 'tone', 'type'])

const resolvedAction = computed<ButtonAction>(() => props.action ?? 'default')
const forwardedAttrs = computed(() =>
  Object.fromEntries(Object.entries(attrs).filter(([key]) => !blockedAttrKeys.has(key)))
)
</script>
