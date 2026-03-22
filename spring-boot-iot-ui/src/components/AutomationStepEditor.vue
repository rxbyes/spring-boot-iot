<template>
  <div class="automation-step-editor">
    <StandardInlineSectionHeader :title="`步骤 ${stepIndex + 1}`">
      <template #actions>
        <StandardRowActions variant="editor" gap="comfortable">
          <StandardActionLink :disabled="stepIndex === 0" @click="$emit('move', -1)">上移</StandardActionLink>
          <StandardActionLink :disabled="stepIndex === stepCount - 1" @click="$emit('move', 1)">下移</StandardActionLink>
          <StandardActionLink @click="$emit('remove')">删除</StandardActionLink>
        </StandardRowActions>
      </template>
    </StandardInlineSectionHeader>

    <div class="automation-step-editor__grid">
      <label class="field-card">
        <span>步骤名称</span>
        <el-input v-model="step.label" placeholder="请输入步骤名称" />
      </label>
      <label class="field-card">
        <span>步骤类型</span>
        <el-select v-model="step.type" placeholder="选择步骤类型" @change="$emit('change-type')">
          <el-option v-for="type in stepTypeOptions" :key="type" :label="type" :value="type" />
        </el-select>
      </label>
      <label class="field-card">
        <span>超时(ms)</span>
        <el-input-number v-model="step.timeout" :min="0" :step="1000" />
      </label>
      <label class="field-card field-card--switch">
        <span>可选步骤</span>
        <el-switch v-model="step.optional" />
      </label>

      <template v-if="stepUsesLocator(step) && step.locator">
        <label class="field-card">
          <span>定位方式</span>
          <el-select v-model="step.locator.type" placeholder="选择定位方式">
            <el-option v-for="type in locatorTypeOptions" :key="type" :label="type" :value="type" />
          </el-select>
        </label>
        <label class="field-card">
          <span>定位值</span>
          <el-input v-model="step.locator.value" placeholder="#id / 请输入占位符 / 关键文本" />
        </label>
        <label v-if="step.locator.type === 'role'" class="field-card">
          <span>角色</span>
          <el-input v-model="step.locator.role" placeholder="button / textbox / link" />
        </label>
        <label v-if="step.locator.type === 'role'" class="field-card">
          <span>角色名称</span>
          <el-input v-model="step.locator.name" placeholder="按钮文案或角色名称" />
        </label>
      </template>

      <label v-if="needsValue(step.type)" class="field-card field-card--wide">
        <span>{{ step.type === 'press' ? '按键值' : step.type === 'assertUrlIncludes' ? 'URL 片段' : '输入/断言值' }}</span>
        <el-input
          v-model="step.value"
          :placeholder="step.type === 'press' ? 'Enter' : '支持模板变量，如 ${runToken} / ${variables.productId}'"
        />
      </label>

      <label v-if="step.type === 'setChecked'" class="field-card field-card--switch">
        <span>目标状态</span>
        <el-switch v-model="step.checked" active-text="选中" inactive-text="取消" />
      </label>

      <label v-if="step.type === 'selectOption'" class="field-card field-card--wide">
        <span>选项文案</span>
        <el-input v-model="step.optionText" placeholder="请选择下拉项文案" />
      </label>

      <label v-if="step.type === 'uploadFile'" class="field-card field-card--wide">
        <span>文件路径</span>
        <el-input
          v-model="step.filePath"
          placeholder="相对仓库根目录或绝对路径，支持模板变量与 JSON 数组"
        />
      </label>

      <template v-if="step.type === 'assertScreenshot'">
        <label class="field-card">
          <span>截图目标</span>
          <el-select v-model="step.screenshotTarget" @change="$emit('change-screenshot-target')">
            <el-option label="page" value="page" />
            <el-option label="locator" value="locator" />
          </el-select>
        </label>
        <label class="field-card">
          <span>基线名称</span>
          <el-input v-model="step.baselineName" placeholder="留空时默认使用步骤名称" />
        </label>
        <label class="field-card">
          <span>差异阈值</span>
          <el-input-number v-model="step.threshold" :min="0" :max="1" :step="0.001" :precision="4" />
        </label>
        <label class="field-card field-card--switch">
          <span>整页截图</span>
          <el-switch v-model="step.fullPage" :disabled="step.screenshotTarget === 'locator'" />
        </label>
      </template>

      <label v-if="step.type === 'tableRowAction'" class="field-card field-card--wide">
        <span>目标行文本</span>
        <el-input v-model="step.rowText" placeholder="用于定位表格行的关键文本" />
      </label>

      <template v-if="(step.type === 'triggerApi' || step.type === 'tableRowAction') && step.action">
        <label class="field-card field-card--wide">
          <span>接口 Matcher</span>
          <el-input v-model="step.matcher" placeholder="/api/example/add" />
        </label>
        <label class="field-card">
          <span>触发动作</span>
          <el-select v-model="step.action.type">
            <el-option label="click" value="click" />
            <el-option label="press" value="press" />
          </el-select>
        </label>
        <label class="field-card">
          <span>动作定位方式</span>
          <el-select v-model="step.action.locator.type">
            <el-option v-for="type in locatorTypeOptions" :key="type" :label="type" :value="type" />
          </el-select>
        </label>
        <label class="field-card">
          <span>动作定位值</span>
          <el-input v-model="step.action.locator.value" placeholder="#submit-button / 提交按钮文案" />
        </label>
        <label v-if="step.action.locator.type === 'role'" class="field-card">
          <span>动作角色</span>
          <el-input v-model="step.action.locator.role" placeholder="button / link" />
        </label>
        <label v-if="step.action.locator.type === 'role'" class="field-card">
          <span>动作名称</span>
          <el-input v-model="step.action.locator.name" placeholder="按钮名称" />
        </label>
        <label v-if="step.action.type === 'press'" class="field-card">
          <span>按键值</span>
          <el-input v-model="step.action.value" placeholder="Enter / Tab" />
        </label>

        <AutomationCaptureEditor
          class="automation-step-editor__capture"
          :captures="step.captures"
          :item-key-prefix="`${step.id}-capture`"
          empty-text="可从接口响应中提取主键、编码等变量，供后续步骤引用。"
          variable-placeholder="变量名，如 productId"
          path-placeholder="响应路径，如 payload.data.id"
          @add="$emit('add-capture')"
          @remove="removeCapture"
        />
      </template>

      <template v-if="step.type === 'dialogAction'">
        <label class="field-card">
          <span>弹窗标题</span>
          <el-input v-model="step.dialogTitle" placeholder="为空时默认匹配最后一个可见弹窗" />
        </label>
        <label class="field-card">
          <span>弹窗动作</span>
          <el-select v-model="step.dialogAction">
            <el-option label="waitVisible" value="waitVisible" />
            <el-option label="confirm" value="confirm" />
            <el-option label="cancel" value="cancel" />
            <el-option label="close" value="close" />
            <el-option label="custom" value="custom" />
          </el-select>
        </label>
        <label v-if="step.dialogAction !== 'waitVisible' && step.dialogAction !== 'close'" class="field-card">
          <span>按钮文案</span>
          <el-input v-model="step.actionText" placeholder="可留空，执行器会按内置按钮文案尝试匹配" />
        </label>
        <label v-if="step.dialogAction !== 'waitVisible'" class="field-card field-card--wide">
          <span>接口 Matcher</span>
          <el-input v-model="step.matcher" placeholder="可留空，仅执行弹窗动作不等待接口" />
        </label>

        <template v-if="step.dialogAction === 'custom' && step.action">
          <label class="field-card">
            <span>自定义动作</span>
            <el-select v-model="step.action.type">
              <el-option label="click" value="click" />
              <el-option label="press" value="press" />
            </el-select>
          </label>
          <label class="field-card">
            <span>动作定位方式</span>
            <el-select v-model="step.action.locator.type">
              <el-option v-for="type in locatorTypeOptions" :key="type" :label="type" :value="type" />
            </el-select>
          </label>
          <label class="field-card">
            <span>动作定位值</span>
            <el-input v-model="step.action.locator.value" placeholder="按钮或输入控件定位表达式" />
          </label>
          <label v-if="step.action.locator.type === 'role'" class="field-card">
            <span>动作角色</span>
            <el-input v-model="step.action.locator.role" placeholder="button / link" />
          </label>
          <label v-if="step.action.locator.type === 'role'" class="field-card">
            <span>动作名称</span>
            <el-input v-model="step.action.locator.name" placeholder="按钮名称" />
          </label>
          <label v-if="step.action.type === 'press'" class="field-card">
            <span>按键值</span>
            <el-input v-model="step.action.value" placeholder="Enter / Escape" />
          </label>
        </template>

        <AutomationCaptureEditor
          v-if="step.dialogAction !== 'waitVisible'"
          class="automation-step-editor__capture"
          :captures="step.captures"
          :item-key-prefix="`${step.id}-dialog-capture`"
          empty-text="若弹窗动作会触发接口，可在这里提取响应中的主键、编码等变量。"
          variable-placeholder="变量名，如 userId"
          path-placeholder="响应路径，如 payload.data.id"
          @add="$emit('add-capture')"
          @remove="removeCapture"
        />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import AutomationCaptureEditor from './AutomationCaptureEditor.vue';
import StandardInlineSectionHeader from './StandardInlineSectionHeader.vue';
import type { AutomationStep } from '../types/automation';

const props = defineProps<{
  step: AutomationStep;
  stepIndex: number;
  stepCount: number;
  locatorTypeOptions: string[];
  stepTypeOptions: string[];
}>();

defineEmits<{
  move: [offset: number];
  remove: [];
  'change-type': [];
  'change-screenshot-target': [];
  'add-capture': [];
}>();

function needsValue(stepType: string): boolean {
  return ['fill', 'press', 'assertText', 'assertUrlIncludes'].includes(stepType);
}

function stepUsesLocator(step: AutomationStep | string): boolean {
  const stepType = typeof step === 'string' ? step : step.type;
  if (stepType === 'assertScreenshot') {
    return typeof step === 'string' ? true : step.screenshotTarget !== 'page';
  }
  return !['sleep', 'assertUrlIncludes', 'dialogAction'].includes(stepType);
}

function removeCapture(index: number) {
  props.step.captures?.splice(index, 1);
}
</script>

<style scoped>
.automation-step-editor {
  padding: 0.9rem;
  border-radius: var(--radius-md);
  border: 1px solid color-mix(in srgb, var(--panel-border) 92%, var(--brand));
  background: color-mix(in srgb, var(--brand) 3%, white);
}

.automation-step-editor + .automation-step-editor {
  margin-top: 0.85rem;
}

.automation-step-editor__grid {
  display: grid;
  gap: 0.85rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.automation-step-editor__capture {
  grid-column: 1 / -1;
}

@media (max-width: 1024px) {
  .automation-step-editor__grid {
    grid-template-columns: 1fr;
  }
}
</style>
