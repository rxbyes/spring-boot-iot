<template>
  <article class="automation-scenario-editor">
    <StandardInlineSectionHeader
      class="automation-scenario-editor__header"
      :title="scenario.name || `场景 ${scenarioIndex + 1}`"
      :description="scenario.businessFlow || '请补充该场景的业务主线。'"
    >
      <template #actions>
        <StandardRowActions variant="editor" gap="comfortable">
          <StandardActionLink :disabled="scenarioIndex === 0" @click="$emit('move-scenario', -1)">上移</StandardActionLink>
          <StandardActionLink :disabled="scenarioIndex === scenarioCount - 1" @click="$emit('move-scenario', 1)">下移</StandardActionLink>
          <StandardActionLink @click="$emit('copy-scenario')">复制</StandardActionLink>
          <StandardActionLink @click="$emit('remove-scenario')">删除</StandardActionLink>
        </StandardRowActions>
      </template>
    </StandardInlineSectionHeader>

    <div class="automation-scenario-editor__grid">
      <label class="automation-scenario-editor__field">
        <span>场景编码</span>
        <el-input v-model="scenario.key" placeholder="scenario-key" />
      </label>
      <label class="automation-scenario-editor__field">
        <span>场景名称</span>
        <el-input v-model="scenario.name" placeholder="请输入场景名称" />
      </label>
      <label class="automation-scenario-editor__field">
        <span>页面路由</span>
        <el-input v-model="scenario.route" placeholder="/replace-me" />
      </label>
      <label class="automation-scenario-editor__field">
        <span>期望路径</span>
        <el-input v-model="scenario.expectedPath" placeholder="可留空，默认跟随页面路由" />
      </label>
      <label class="automation-scenario-editor__field">
        <span>场景范围</span>
        <el-select v-model="scenario.scope" placeholder="选择范围">
          <el-option v-for="scope in scopeOptions" :key="scope" :label="scope" :value="scope" />
        </el-select>
      </label>
      <label class="automation-scenario-editor__field">
        <span>就绪选择器</span>
        <el-input v-model="scenario.readySelector" placeholder="#app / .page-title" />
      </label>
      <label class="automation-scenario-editor__field automation-scenario-editor__field--wide">
        <span>业务主线</span>
        <el-input v-model="scenario.businessFlow" placeholder="例如：页面打开 -> 新增 -> 查询 -> 详情核验" />
      </label>
      <label class="automation-scenario-editor__field automation-scenario-editor__field--wide">
        <span>场景描述</span>
        <el-input
          v-model="scenario.description"
          type="textarea"
          :rows="2"
          placeholder="补充该场景服务的业务目标、前置条件和注意事项"
        />
      </label>
    </div>

    <section class="automation-scenario-editor__block">
      <StandardInlineSectionHeader title="业务点梳理">
        <template #actions>
          <StandardActionLink @click="scenario.featurePoints.push('')">新增业务点</StandardActionLink>
        </template>
      </StandardInlineSectionHeader>
      <div v-if="scenario.featurePoints.length === 0" class="automation-scenario-editor__empty">
        暂无业务点，建议至少整理 2-3 个关键功能点。
      </div>
      <div
        v-for="(point, pointIndex) in scenario.featurePoints"
        :key="`${scenario.key}-point-${pointIndex}`"
        class="automation-scenario-editor__row"
      >
        <el-input
          v-model="scenario.featurePoints[pointIndex]"
          placeholder="例如：新增、查询、详情、导出、状态切换"
        />
        <StandardActionLink @click="scenario.featurePoints.splice(pointIndex, 1)">移除</StandardActionLink>
      </div>
    </section>

    <section class="automation-scenario-editor__block">
      <StandardInlineSectionHeader title="首屏接口">
        <template #actions>
          <StandardActionLink @click="$emit('add-initial-api')">新增接口</StandardActionLink>
        </template>
      </StandardInlineSectionHeader>
      <div v-if="scenario.initialApis.length === 0" class="automation-scenario-editor__empty">
        若页面打开即触发接口，建议在这里补充 matcher 作为首屏证据。
      </div>
      <div
        v-for="(api, apiIndex) in scenario.initialApis"
        :key="`${scenario.key}-api-${apiIndex}`"
        class="automation-scenario-editor__api-row"
      >
        <label class="automation-scenario-editor__field">
          <span>接口说明</span>
          <el-input v-model="api.label" placeholder="例如：列表查询接口" />
        </label>
        <label class="automation-scenario-editor__field">
          <span>Matcher</span>
          <el-input v-model="api.matcher" placeholder="/api/example/list" />
        </label>
        <label class="automation-scenario-editor__field">
          <span>超时(ms)</span>
          <el-input-number v-model="api.timeout" :min="1000" :step="1000" />
        </label>
        <label class="automation-scenario-editor__field automation-scenario-editor__field--switch">
          <span>可选接口</span>
          <el-switch v-model="api.optional" />
        </label>
        <StandardActionLink @click="scenario.initialApis.splice(apiIndex, 1)">移除</StandardActionLink>
      </div>
    </section>

    <section class="automation-scenario-editor__block">
      <StandardInlineSectionHeader title="步骤编排">
        <template #actions>
          <StandardActionLink @click="$emit('add-step')">新增步骤</StandardActionLink>
        </template>
      </StandardInlineSectionHeader>
      <AutomationStepEditor
        v-for="(step, stepIndex) in scenario.steps"
        :key="step.id"
        :step="step"
        :step-index="stepIndex"
        :step-count="scenario.steps.length"
        :locator-type-options="locatorTypeOptions"
        :step-type-options="stepTypeOptions"
        @move="$emit('move-step', { stepIndex, offset: $event })"
        @remove="$emit('remove-step', { stepIndex })"
        @change-type="$emit('change-step-type', { step })"
        @change-screenshot-target="$emit('change-screenshot-target', { step })"
        @add-capture="$emit('add-capture', { step })"
      />
    </section>
  </article>
</template>

<script setup lang="ts">
import AutomationStepEditor from './AutomationStepEditor.vue';
import StandardInlineSectionHeader from './StandardInlineSectionHeader.vue';
import type { AutomationScenarioConfig, AutomationStep } from '../types/automation';

defineProps<{
  scenario: AutomationScenarioConfig;
  scenarioIndex: number;
  scenarioCount: number;
  scopeOptions: string[];
  locatorTypeOptions: string[];
  stepTypeOptions: string[];
}>();

defineEmits<{
  'move-scenario': [offset: number];
  'copy-scenario': [];
  'remove-scenario': [];
  'add-initial-api': [];
  'add-step': [];
  'move-step': [payload: { stepIndex: number; offset: number }];
  'remove-step': [payload: { stepIndex: number }];
  'change-step-type': [payload: { step: AutomationStep }];
  'change-screenshot-target': [payload: { step: AutomationStep }];
  'add-capture': [payload: { step: AutomationStep }];
}>();
</script>

<style scoped>
.automation-scenario-editor {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand) 4%, white));
}

.automation-scenario-editor + .automation-scenario-editor {
  margin-top: 1rem;
}

.automation-scenario-editor__header {
  margin-bottom: 1rem;
}

.automation-scenario-editor__header :deep(.standard-inline-section-header__main strong) {
  font-size: 1.05rem;
}

.automation-scenario-editor__header :deep(.standard-inline-section-header__main p) {
  margin-top: 0.35rem;
}

.automation-scenario-editor__grid {
  display: grid;
  gap: 0.85rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.automation-scenario-editor__field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.automation-scenario-editor__field span {
  color: var(--text-secondary);
  font-size: 0.86rem;
}

.automation-scenario-editor__field--wide {
  grid-column: 1 / -1;
}

.automation-scenario-editor__field--switch {
  justify-content: flex-end;
}

.automation-scenario-editor__block {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px dashed color-mix(in srgb, var(--panel-border) 88%, var(--text-tertiary));
}

.automation-scenario-editor__row,
.automation-scenario-editor__api-row {
  display: grid;
  gap: 0.75rem;
  align-items: center;
}

.automation-scenario-editor__row {
  grid-template-columns: 1fr auto;
}

.automation-scenario-editor__api-row {
  grid-template-columns: repeat(4, minmax(0, 1fr)) auto;
}

.automation-scenario-editor__row + .automation-scenario-editor__row,
.automation-scenario-editor__api-row + .automation-scenario-editor__api-row {
  margin-top: 0.65rem;
}

.automation-scenario-editor__empty {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--brand) 4%, white);
  color: var(--text-secondary);
}

@media (max-width: 1024px) {
  .automation-scenario-editor__grid,
  .automation-scenario-editor__api-row {
    grid-template-columns: 1fr;
  }
}
</style>
