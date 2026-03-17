<template>
  <div class="page-stack automation-test-view">
    <section class="hero-panel">
      <p class="eyebrow">Automation Studio</p>
      <h1 class="headline">自动化测试中心</h1>
      <p class="hero-description">
        以现有 Playwright 骨架为执行底座，通过前端可视化配置生成声明式测试计划，覆盖登录、页面交互、接口回执、断言、报告与测试建议。
      </p>
      <div class="hero-chip-list">
        <span>配置驱动</span>
        <span>可插拔步骤</span>
        <span>任意 Web 接入</span>
        <span>测试报告输出</span>
        <span>改进建议生成</span>
      </div>
    </section>

    <section class="tri-grid">
      <PanelCard eyebrow="Plan Metrics" title="计划概况" description="先用场景粒度组织业务，再按步骤粒度沉淀可复用自动化模板。">
        <div class="metric-list">
          <div class="metric-item">
            <strong>{{ scenarioPreviews.length }}</strong>
            <span>场景数</span>
          </div>
          <div class="metric-item">
            <strong>{{ totalSteps }}</strong>
            <span>步骤数</span>
          </div>
          <div class="metric-item">
            <strong>{{ totalApiChecks }}</strong>
            <span>接口检查数</span>
          </div>
          <div class="metric-item">
            <strong>{{ assertedScenarios }}</strong>
            <span>断言场景数</span>
          </div>
        </div>
      </PanelCard>

      <PanelCard eyebrow="Runtime" title="执行方式" description="导出的 JSON 计划可直接交给 `scripts/auto` 执行器运行。">
        <div class="command-box">
          <code>{{ commandPreview }}</code>
        </div>
        <ul class="phase-ideas">
          <li>支持 `--plan` 按任意 JSON 计划执行，不再局限于仓库内置页面。</li>
          <li>支持 `--dry-run` 预览执行计划，适合测试负责人先做编排审查。</li>
          <li>失败结果继续落盘到 `logs/acceptance`，可复用现有报告归档链路。</li>
        </ul>
        <div class="action-row">
          <el-button type="primary" @click="copyCommand">复制命令</el-button>
          <el-button @click="downloadPlan">导出 JSON</el-button>
        </div>
      </PanelCard>

      <PanelCard eyebrow="Roadmap" title="能力边界" description="本轮先建设平台骨架，后续可以逐步叠加更多模板、插件与 AI 辅助能力。">
        <ul class="phase-ideas">
          <li>当前已支持页面可达、交互动作、接口断言、变量捕获、报告建议。</li>
          <li>后续可继续扩展截图对比、表格比对、爬取式页面盘点、AI 用例补全。</li>
          <li>通过菜单与 JSON 计划解耦，可复用到任意带浏览器界面的业务系统。</li>
        </ul>
      </PanelCard>
    </section>

    <section class="two-column-grid">
      <PanelCard eyebrow="Execution Target" title="执行配置" description="这里定义目标系统、账号、输出策略与阻断范围。">
        <el-form label-width="110px" class="automation-form">
          <el-form-item label="计划名称">
            <el-input v-model="plan.target.planName" placeholder="请输入计划名称" />
          </el-form-item>
          <el-form-item label="前端地址">
            <el-input v-model="plan.target.frontendBaseUrl" placeholder="例如：http://127.0.0.1:5174" />
          </el-form-item>
          <el-form-item label="后端地址">
            <el-input v-model="plan.target.backendBaseUrl" placeholder="例如：http://127.0.0.1:9999" />
          </el-form-item>
          <el-form-item label="登录路由">
            <el-input v-model="plan.target.loginRoute" placeholder="/login" />
          </el-form-item>
          <el-form-item label="账号">
            <el-input v-model="plan.target.username" placeholder="admin" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="plan.target.password" type="password" show-password placeholder="123456" />
          </el-form-item>
          <el-form-item label="浏览器路径">
            <el-input v-model="plan.target.browserPath" placeholder="可留空，走自动识别" />
          </el-form-item>
          <el-form-item label="问题文档">
            <el-input v-model="plan.target.issueDocPath" placeholder="docs/22-automation-test-issues-20260316.md" />
          </el-form-item>
          <el-form-item label="输出前缀">
            <el-input v-model="plan.target.outputPrefix" placeholder="config-browser" />
          </el-form-item>
          <el-form-item label="执行模式">
            <el-switch
              v-model="plan.target.headless"
              inline-prompt
              active-text="无头"
              inactive-text="有头"
            />
          </el-form-item>
        </el-form>

        <div class="scope-grid">
          <label class="scope-card">
            <span>场景范围</span>
            <el-select v-model="plan.target.scenarioScopes" multiple collapse-tags placeholder="选择执行范围">
              <el-option v-for="scope in scopeOptions" :key="scope" :label="scope" :value="scope" />
            </el-select>
          </label>

          <label class="scope-card">
            <span>阻断范围</span>
            <el-select v-model="plan.target.failScopes" multiple collapse-tags placeholder="选择阻断范围">
              <el-option v-for="scope in scopeOptions" :key="scope" :label="scope" :value="scope" />
            </el-select>
          </label>
        </div>
      </PanelCard>

      <PanelCard eyebrow="Suggestion Engine" title="测试建议" description="按计划结构自动提示薄弱点，帮助持续完善测试质量。">
        <ul class="suggestion-list">
          <li
            v-for="item in suggestions"
            :key="`${item.level}-${item.title}`"
            class="suggestion-item"
            :class="`suggestion-item--${item.level}`"
          >
            <strong>{{ item.title }}</strong>
            <p>{{ item.detail }}</p>
          </li>
        </ul>
      </PanelCard>
    </section>

    <section>
      <PanelCard
        eyebrow="Scenario Builder"
        title="场景编排"
        description="先通过模板快速起步，再替换页面路由、选择器、接口匹配与断言规则。"
      >
        <template #actions>
          <div class="action-row action-row--wrap">
            <el-button type="primary" @click="addScenario('pageSmoke')">新增页面冒烟模板</el-button>
            <el-button @click="addScenario('formSubmit')">新增表单提交模板</el-button>
            <el-button @click="addScenario('listDetail')">新增列表详情模板</el-button>
            <el-button @click="showImportDialog = true">导入计划</el-button>
            <el-button @click="resetPlan">恢复默认计划</el-button>
          </div>
        </template>

        <div v-if="plan.scenarios.length === 0" class="empty-block">
          当前暂无场景，请先选择一个模板开始编排。
        </div>

        <article
          v-for="(scenario, scenarioIndex) in plan.scenarios"
          :key="scenario.key"
          class="scenario-card"
        >
          <header class="scenario-card__header">
            <div>
              <h3>{{ scenario.name || `场景 ${scenarioIndex + 1}` }}</h3>
              <p>{{ scenario.businessFlow || '请补充该场景的业务主线。' }}</p>
            </div>
            <div class="action-row action-row--wrap">
              <el-button text @click="moveScenario(scenarioIndex, -1)" :disabled="scenarioIndex === 0">上移</el-button>
              <el-button text @click="moveScenario(scenarioIndex, 1)" :disabled="scenarioIndex === plan.scenarios.length - 1">下移</el-button>
              <el-button text @click="copyScenario(scenarioIndex)">复制</el-button>
              <el-button text type="danger" @click="removeScenario(scenarioIndex)">删除</el-button>
            </div>
          </header>

          <div class="scenario-grid">
            <label class="field-card">
              <span>场景编码</span>
              <el-input v-model="scenario.key" placeholder="scenario-key" />
            </label>
            <label class="field-card">
              <span>场景名称</span>
              <el-input v-model="scenario.name" placeholder="请输入场景名称" />
            </label>
            <label class="field-card">
              <span>页面路由</span>
              <el-input v-model="scenario.route" placeholder="/replace-me" />
            </label>
            <label class="field-card">
              <span>期望路径</span>
              <el-input v-model="scenario.expectedPath" placeholder="可留空，默认跟随页面路由" />
            </label>
            <label class="field-card">
              <span>场景范围</span>
              <el-select v-model="scenario.scope" placeholder="选择范围">
                <el-option v-for="scope in scopeOptions" :key="scope" :label="scope" :value="scope" />
              </el-select>
            </label>
            <label class="field-card">
              <span>就绪选择器</span>
              <el-input v-model="scenario.readySelector" placeholder="#app / .page-title" />
            </label>
            <label class="field-card field-card--wide">
              <span>业务主线</span>
              <el-input v-model="scenario.businessFlow" placeholder="例如：页面打开 -> 新增 -> 查询 -> 详情核验" />
            </label>
            <label class="field-card field-card--wide">
              <span>场景描述</span>
              <el-input
                v-model="scenario.description"
                type="textarea"
                :rows="2"
                placeholder="补充该场景服务的业务目标、前置条件和注意事项"
              />
            </label>
          </div>

          <section class="inline-block">
            <div class="inline-block__header">
              <strong>业务点梳理</strong>
              <el-button text @click="scenario.featurePoints.push('')">新增业务点</el-button>
            </div>
            <div v-if="scenario.featurePoints.length === 0" class="empty-inline">暂无业务点，建议至少整理 2-3 个关键功能点。</div>
            <div v-for="(point, pointIndex) in scenario.featurePoints" :key="`${scenario.key}-point-${pointIndex}`" class="row-editor">
              <el-input v-model="scenario.featurePoints[pointIndex]" placeholder="例如：新增、查询、详情、导出、状态切换" />
              <el-button text type="danger" @click="scenario.featurePoints.splice(pointIndex, 1)">移除</el-button>
            </div>
          </section>

          <section class="inline-block">
            <div class="inline-block__header">
              <strong>首屏接口</strong>
              <el-button text @click="addInitialApi(scenario)">新增接口</el-button>
            </div>
            <div v-if="scenario.initialApis.length === 0" class="empty-inline">若页面打开即触发接口，建议在这里补充 matcher 作为首屏证据。</div>
            <div v-for="(api, apiIndex) in scenario.initialApis" :key="`${scenario.key}-api-${apiIndex}`" class="api-editor">
              <label class="field-card">
                <span>接口说明</span>
                <el-input v-model="api.label" placeholder="例如：列表查询接口" />
              </label>
              <label class="field-card">
                <span>Matcher</span>
                <el-input v-model="api.matcher" placeholder="/api/example/list" />
              </label>
              <label class="field-card">
                <span>超时(ms)</span>
                <el-input-number v-model="api.timeout" :min="1000" :step="1000" />
              </label>
              <label class="field-card field-card--switch">
                <span>可选接口</span>
                <el-switch v-model="api.optional" />
              </label>
              <el-button text type="danger" @click="scenario.initialApis.splice(apiIndex, 1)">移除</el-button>
            </div>
          </section>

          <section class="inline-block">
            <div class="inline-block__header">
              <strong>步骤编排</strong>
              <el-button text @click="addStep(scenario)">新增步骤</el-button>
            </div>
            <div v-for="(step, stepIndex) in scenario.steps" :key="step.id" class="step-editor">
              <div class="step-editor__header">
                <strong>步骤 {{ stepIndex + 1 }}</strong>
                <div class="action-row">
                  <el-button text @click="moveStep(scenario, stepIndex, -1)" :disabled="stepIndex === 0">上移</el-button>
                  <el-button text @click="moveStep(scenario, stepIndex, 1)" :disabled="stepIndex === scenario.steps.length - 1">下移</el-button>
                  <el-button text type="danger" @click="scenario.steps.splice(stepIndex, 1)">删除</el-button>
                </div>
              </div>

              <div class="step-grid">
                <label class="field-card">
                  <span>步骤名称</span>
                  <el-input v-model="step.label" placeholder="请输入步骤名称" />
                </label>
                <label class="field-card">
                  <span>步骤类型</span>
                  <el-select v-model="step.type" placeholder="选择步骤类型">
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

                <template v-if="step.type !== 'sleep' && step.type !== 'assertUrlIncludes' && step.locator">
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

                <label v-if="step.type === 'selectOption'" class="field-card field-card--wide">
                  <span>选项文案</span>
                  <el-input v-model="step.optionText" placeholder="请选择下拉项文案" />
                </label>

                <template v-if="step.type === 'triggerApi' && step.action">
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

                  <div class="capture-block">
                    <div class="inline-block__header">
                      <strong>变量捕获</strong>
                      <el-button text @click="addCapture(step)">新增捕获</el-button>
                    </div>
                    <div v-if="!step.captures || step.captures.length === 0" class="empty-inline">
                      可从接口响应中提取主键、编码等变量，供后续步骤引用。
                    </div>
                    <div
                      v-for="(capture, captureIndex) in step.captures"
                      :key="`${step.id}-capture-${captureIndex}`"
                      class="row-editor"
                    >
                      <el-input v-model="capture.variable" placeholder="变量名，如 productId" />
                      <el-input v-model="capture.path" placeholder="响应路径，如 payload.data.id" />
                      <el-button text type="danger" @click="step.captures?.splice(captureIndex, 1)">移除</el-button>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </section>
        </article>
      </PanelCard>
    </section>

    <section class="two-column-grid">
      <PanelCard eyebrow="Preview" title="场景预览" description="这里用于快速查看每个场景的覆盖粒度。">
        <el-table :data="scenarioPreviews" size="small" border>
          <el-table-column prop="key" label="编码" min-width="160" />
          <el-table-column prop="scope" label="范围" width="110" />
          <el-table-column prop="stepCount" label="步骤" width="90" />
          <el-table-column prop="apiCount" label="接口" width="90" />
          <el-table-column prop="featureCount" label="业务点" width="100" />
          <el-table-column label="断言" width="90">
            <template #default="{ row }">
              <el-tag :type="row.hasAssertion ? 'success' : 'warning'">
                {{ row.hasAssertion ? '已覆盖' : '待补齐' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </PanelCard>

      <ResponsePanel
        eyebrow="Plan Export"
        title="导出计划 JSON"
        description="可直接交给 `node scripts/auto/run-browser-acceptance.mjs --plan=...` 执行。"
        :body="plan"
      />
    </section>

    <el-dialog v-model="showImportDialog" title="导入自动化计划" width="760px">
      <el-input
        v-model="importText"
        type="textarea"
        :rows="18"
        placeholder="请粘贴导出的 JSON 计划"
      />
      <template #footer>
        <div class="action-row">
          <el-button @click="showImportDialog = false">取消</el-button>
          <el-button type="primary" @click="applyImport">导入并替换当前计划</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage } from '@/utils/message';

import PanelCard from '../components/PanelCard.vue';
import ResponsePanel from '../components/ResponsePanel.vue';
import type { AutomationPlanDocument, AutomationScenarioConfig, AutomationStep } from '../types/automation';
import {
  buildAutomationCommand,
  buildPlanSuggestions,
  buildScenarioPreviews,
  createDefaultAutomationPlan,
  createFormSubmitScenario,
  createListDetailScenario,
  createPageSmokeScenario,
  createAutomationId,
  duplicateScenario,
  loadSavedAutomationPlan,
  normalizeAutomationPlan,
  saveAutomationPlan
} from '../utils/automationPlan';

type ScenarioTemplateType = 'pageSmoke' | 'formSubmit' | 'listDetail';

const scopeOptions = ['delivery', 'baseline', 'regression', 'smoke'];
const locatorTypeOptions = ['css', 'placeholder', 'role', 'text', 'label', 'testId'];
const stepTypeOptions = [
  'fill',
  'click',
  'press',
  'selectOption',
  'waitVisible',
  'triggerApi',
  'assertText',
  'assertUrlIncludes',
  'sleep'
];

const plan = ref<AutomationPlanDocument>(normalizeAutomationPlan(loadSavedAutomationPlan()));
const showImportDialog = ref(false);
const importText = ref('');

const scenarioPreviews = computed(() => buildScenarioPreviews(plan.value));
const suggestions = computed(() => buildPlanSuggestions(plan.value));
const totalSteps = computed(() =>
  plan.value.scenarios.reduce((sum, scenario) => sum + scenario.steps.length, 0)
);
const totalApiChecks = computed(() =>
  scenarioPreviews.value.reduce((sum, scenario) => sum + scenario.apiCount, 0)
);
const assertedScenarios = computed(() =>
  scenarioPreviews.value.filter((scenario) => scenario.hasAssertion).length
);
const commandPreview = computed(() => buildAutomationCommand('config/automation/sample-web-smoke-plan.json'));

function ensureStepShape(step: AutomationStep): void {
  if (!step.locator && step.type !== 'sleep' && step.type !== 'assertUrlIncludes') {
    step.locator = {
      type: 'css',
      value: ''
    };
  }
  if (step.type === 'triggerApi' && !step.action) {
    step.action = {
      type: 'click',
      locator: {
        type: 'css',
        value: ''
      }
    };
  }
  if (step.type !== 'triggerApi') {
    step.captures = [];
  }
}

plan.value.scenarios.forEach((scenario) => scenario.steps.forEach(ensureStepShape));

watch(
  plan,
  (value) => {
    const normalized = normalizeAutomationPlan(value);
    normalized.scenarios.forEach((scenario) => scenario.steps.forEach(ensureStepShape));
    saveAutomationPlan(normalized);
  },
  {
    deep: true
  }
);

function needsValue(stepType: string): boolean {
  return ['fill', 'press', 'assertText', 'assertUrlIncludes'].includes(stepType);
}

function addScenario(type: ScenarioTemplateType) {
  const scenario =
    type === 'formSubmit'
      ? createFormSubmitScenario()
      : type === 'listDetail'
        ? createListDetailScenario()
        : createPageSmokeScenario();
  scenario.steps.forEach(ensureStepShape);
  plan.value.scenarios.push(scenario);
}

function copyScenario(index: number) {
  const scenario = plan.value.scenarios[index];
  if (!scenario) {
    return;
  }
  const duplicated = duplicateScenario(scenario);
  duplicated.steps.forEach(ensureStepShape);
  plan.value.scenarios.splice(index + 1, 0, duplicated);
}

function removeScenario(index: number) {
  plan.value.scenarios.splice(index, 1);
}

function moveScenario(index: number, offset: number) {
  const targetIndex = index + offset;
  if (targetIndex < 0 || targetIndex >= plan.value.scenarios.length) {
    return;
  }
  const [item] = plan.value.scenarios.splice(index, 1);
  plan.value.scenarios.splice(targetIndex, 0, item);
}

function addInitialApi(scenario: AutomationScenarioConfig) {
  scenario.initialApis.push({
    label: '页面接口',
    matcher: '/api/example/list',
    optional: true,
    timeout: 15000
  });
}

function addStep(scenario: AutomationScenarioConfig) {
  const step: AutomationStep = {
    id: createAutomationId('step'),
    label: '新增步骤',
    type: 'waitVisible',
    locator: {
      type: 'css',
      value: 'body'
    },
    optional: false,
    timeout: 15000
  };
  ensureStepShape(step);
  scenario.steps.push(step);
}

function addCapture(step: AutomationStep) {
  if (!step.captures) {
    step.captures = [];
  }
  step.captures.push({
    variable: '',
    path: ''
  });
}

function moveStep(scenario: AutomationScenarioConfig, index: number, offset: number) {
  const targetIndex = index + offset;
  if (targetIndex < 0 || targetIndex >= scenario.steps.length) {
    return;
  }
  const [item] = scenario.steps.splice(index, 1);
  scenario.steps.splice(targetIndex, 0, item);
}

async function copyCommand() {
  try {
    await navigator.clipboard.writeText(commandPreview.value);
    ElMessage.success('执行命令已复制');
  } catch {
    ElMessage.warning('当前环境不支持剪贴板复制，请手动复制命令');
  }
}

function downloadTextFile(fileName: string, content: string) {
  const blob = new Blob([content], { type: 'application/json;charset=utf-8' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  window.URL.revokeObjectURL(url);
}

function downloadPlan() {
  const normalized = normalizeAutomationPlan(plan.value);
  downloadTextFile('automation-plan.json', JSON.stringify(normalized, null, 2));
  ElMessage.success('自动化计划已导出');
}

function resetPlan() {
  plan.value = createDefaultAutomationPlan();
  plan.value.scenarios.forEach((scenario) => scenario.steps.forEach(ensureStepShape));
  ElMessage.success('已恢复默认计划模板');
}

function applyImport() {
  try {
    const nextPlan = normalizeAutomationPlan(JSON.parse(importText.value) as AutomationPlanDocument);
    nextPlan.scenarios.forEach((scenario) => scenario.steps.forEach(ensureStepShape));
    plan.value = nextPlan;
    importText.value = '';
    showImportDialog.value = false;
    ElMessage.success('自动化计划已导入');
  } catch {
    ElMessage.error('导入失败，请检查 JSON 格式是否正确');
  }
}

watch(
  () => showImportDialog.value,
  (visible) => {
    if (!visible) {
      importText.value = '';
    }
  }
);
</script>

<style scoped>
.automation-test-view {
  padding-bottom: 1rem;
}

.hero-description {
  margin: 0.85rem 0 0;
  max-width: 72rem;
  color: var(--text-secondary);
  line-height: 1.8;
}

.hero-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-top: 1.2rem;
}

.hero-chip-list span {
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.6);
  color: var(--text-primary);
  font-size: 0.88rem;
}

.phase-ideas {
  margin: 0.9rem 0 0;
  padding-left: 1.1rem;
  line-height: 1.8;
  color: var(--text-secondary);
}

.metric-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
}

.metric-item {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-md);
  background: linear-gradient(180deg, #fbfdff, #f3f7fd);
  border: 1px solid rgba(81, 102, 136, 0.16);
}

.metric-item strong {
  display: block;
  font-size: 1.4rem;
  line-height: 1.2;
}

.metric-item span {
  display: block;
  margin-top: 0.35rem;
  color: var(--text-secondary);
}

.command-box {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  border: 1px solid rgba(81, 102, 136, 0.16);
  background: #f8fbff;
  overflow: auto;
}

.command-box code {
  font-family: var(--font-mono);
  color: #1f2a3d;
  white-space: nowrap;
}

.action-row {
  display: flex;
  gap: 0.6rem;
  align-items: center;
}

.action-row--wrap {
  flex-wrap: wrap;
}

.automation-form {
  margin-bottom: 1rem;
}

.scope-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.scope-card {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.scope-card span {
  color: var(--text-secondary);
  font-size: 0.88rem;
}

.suggestion-list {
  display: grid;
  gap: 0.85rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.suggestion-item {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  border: 1px solid rgba(81, 102, 136, 0.16);
  background: #fff;
}

.suggestion-item strong {
  display: block;
  margin-bottom: 0.35rem;
}

.suggestion-item p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.suggestion-item--warning {
  background: rgba(255, 245, 235, 0.8);
  border-color: rgba(255, 153, 0, 0.22);
}

.suggestion-item--info {
  background: rgba(240, 247, 255, 0.92);
  border-color: rgba(24, 144, 255, 0.2);
}

.suggestion-item--success {
  background: rgba(241, 255, 247, 0.92);
  border-color: rgba(82, 196, 26, 0.22);
}

.scenario-card {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(81, 102, 136, 0.16);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.96));
}

.scenario-card + .scenario-card {
  margin-top: 1rem;
}

.scenario-card__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
  align-items: flex-start;
}

.scenario-card__header h3 {
  margin: 0;
  font-size: 1.05rem;
}

.scenario-card__header p {
  margin: 0.35rem 0 0;
  color: var(--text-secondary);
}

.scenario-grid,
.step-grid {
  display: grid;
  gap: 0.85rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-card {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.field-card span {
  color: var(--text-secondary);
  font-size: 0.86rem;
}

.field-card--wide,
.capture-block {
  grid-column: 1 / -1;
}

.field-card--switch {
  justify-content: flex-end;
}

.inline-block {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px dashed rgba(81, 102, 136, 0.18);
}

.inline-block__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-bottom: 0.75rem;
}

.row-editor,
.api-editor {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: 1fr auto;
  align-items: center;
}

.api-editor {
  grid-template-columns: repeat(4, minmax(0, 1fr)) auto;
}

.row-editor + .row-editor,
.api-editor + .api-editor {
  margin-top: 0.65rem;
}

.step-editor {
  padding: 0.9rem;
  border-radius: var(--radius-md);
  border: 1px solid rgba(81, 102, 136, 0.14);
  background: rgba(255, 255, 255, 0.85);
}

.step-editor + .step-editor {
  margin-top: 0.85rem;
}

.step-editor__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-bottom: 0.85rem;
}

.empty-block,
.empty-inline {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  background: rgba(248, 251, 255, 0.95);
  color: var(--text-secondary);
}

@media (max-width: 1024px) {
  .metric-list,
  .scope-grid,
  .scenario-grid,
  .step-grid,
  .api-editor {
    grid-template-columns: 1fr;
  }

  .scenario-card__header,
  .inline-block__header,
  .step-editor__header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
