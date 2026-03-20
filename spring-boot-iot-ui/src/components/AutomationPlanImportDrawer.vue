<template>
  <StandardFormDrawer
    v-model="visible"
    eyebrow="Automation Import"
    title="导入自动化计划"
    subtitle="统一通过右侧抽屉粘贴并导入 JSON 计划，导入后会替换当前编排内容。"
    size="48rem"
  >
    <el-input
      v-model="importText"
      type="textarea"
      :rows="18"
      placeholder="请粘贴导出的 JSON 计划"
    />
    <template #footer>
      <StandardDrawerFooter
        confirm-text="导入并替换当前计划"
        @cancel="visible = false"
        @confirm="handleConfirm"
      />
    </template>
  </StandardFormDrawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import StandardDrawerFooter from './StandardDrawerFooter.vue';
import StandardFormDrawer from './StandardFormDrawer.vue';

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  confirm: [text: string];
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
});

const importText = ref('');

watch(
  () => props.modelValue,
  (value) => {
    if (!value) {
      importText.value = '';
    }
  }
);

function handleConfirm() {
  emit('confirm', importText.value);
}
</script>
