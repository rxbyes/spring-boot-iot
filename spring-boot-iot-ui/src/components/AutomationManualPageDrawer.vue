<template>
  <StandardFormDrawer
    v-model="visible"
    eyebrow="Page Inventory"
    title="新增自定义页面"
    subtitle="统一通过右侧抽屉补充未纳入菜单树的页面盘点信息，并生成推荐测试模板。"
    size="46rem"
  >
    <div class="automation-manual-page-drawer__grid">
      <label class="automation-manual-page-drawer__field">
        <span>页面名称</span>
        <el-input v-model="draft.title" placeholder="例如：外部采购门户" />
      </label>
      <label class="automation-manual-page-drawer__field">
        <span>页面路由</span>
        <el-input v-model="draft.route" placeholder="/external-dashboard" />
      </label>
      <label class="automation-manual-page-drawer__field automation-manual-page-drawer__field--wide">
        <span>页面说明</span>
        <el-input
          v-model="draft.caption"
          type="textarea"
          :rows="2"
          placeholder="说明该页面的业务目标、页面职责或首屏特征"
        />
      </label>
      <label class="automation-manual-page-drawer__field">
        <span>推荐模板</span>
        <el-select v-model="draft.recommendedTemplate">
          <el-option
            v-for="type in templateOptions"
            :key="type"
            :label="buildTemplateLabel(type)"
            :value="type"
          />
        </el-select>
      </label>
      <label class="automation-manual-page-drawer__field">
        <span>执行范围</span>
        <el-select v-model="draft.scope">
          <el-option v-for="scope in scopeOptions" :key="scope" :label="scope" :value="scope" />
        </el-select>
      </label>
      <label class="automation-manual-page-drawer__field">
        <span>就绪选择器</span>
        <el-input v-model="draft.readySelector" placeholder="[data-testid=&quot;console-page-title&quot;]" />
      </label>
      <label class="automation-manual-page-drawer__field">
        <span>首屏接口 Matcher</span>
        <el-input v-model="draft.matcher" placeholder="/api/external/dashboard" />
      </label>
      <label class="automation-manual-page-drawer__field automation-manual-page-drawer__field--switch">
        <span>需要登录</span>
        <el-switch v-model="draft.requiresLogin" />
      </label>
    </div>
    <template #footer>
      <StandardDrawerFooter
        confirm-text="保存并加入页面盘点"
        @cancel="visible = false"
        @confirm="handleSave"
      />
    </template>
  </StandardFormDrawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import StandardDrawerFooter from './StandardDrawerFooter.vue';
import StandardFormDrawer from './StandardFormDrawer.vue';
import type { AutomationPageInventoryItem, AutomationScenarioTemplateType } from '../types/automation';
import { createManualInventoryItem } from '../utils/automationPlan';

const props = defineProps<{
  modelValue: boolean;
  scopeOptions: string[];
  templateOptions: AutomationScenarioTemplateType[];
  buildTemplateLabel: (template: AutomationScenarioTemplateType) => string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  save: [item: AutomationPageInventoryItem];
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
});

const draft = ref<AutomationPageInventoryItem>(createManualInventoryItem());

watch(
  () => props.modelValue,
  () => {
    draft.value = createManualInventoryItem();
  }
);

function handleSave() {
  emit('save', createManualInventoryItem(draft.value));
}
</script>

<style scoped>
.automation-manual-page-drawer__grid {
  display: grid;
  gap: 0.85rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.automation-manual-page-drawer__field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.automation-manual-page-drawer__field span {
  color: var(--text-secondary);
  font-size: 0.86rem;
}

.automation-manual-page-drawer__field--wide {
  grid-column: 1 / -1;
}

.automation-manual-page-drawer__field--switch {
  justify-content: flex-end;
}

@media (max-width: 1024px) {
  .automation-manual-page-drawer__grid {
    grid-template-columns: 1fr;
  }
}
</style>
