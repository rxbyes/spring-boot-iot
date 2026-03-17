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
          <el-form-item label="基线路径">
            <el-input v-model="plan.target.baselineDir" placeholder="config/automation/baselines" />
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
        eyebrow="Page Discovery"
        title="页面盘点与脚手架生成"
        description="优先读取当前授权菜单盘点页面，可一键补齐未覆盖页面的自动化脚手架，也支持手工登记外部系统页面。"
      >
        <template #actions>
          <div class="action-row action-row--wrap">
            <el-button @click="refreshPageInventory">刷新盘点</el-button>
            <el-button @click="selectUncoveredPages">勾选未覆盖</el-button>
            <el-button type="primary" @click="generateSelectedInventoryScenarios">生成勾选场景</el-button>
            <el-button @click="generateUncoveredInventoryScenarios">一键生成全部未覆盖</el-button>
            <el-button @click="openManualPageDialog">新增自定义页面</el-button>
          </div>
        </template>

        <div class="metric-list inventory-metric-list">
          <div class="metric-item">
            <strong>{{ coverageSummary.totalPages }}</strong>
            <span>盘点页面数</span>
          </div>
          <div class="metric-item">
            <strong>{{ coverageSummary.coveredPages }}</strong>
            <span>已覆盖页面</span>
          </div>
          <div class="metric-item">
            <strong>{{ coverageSummary.uncoveredPages }}</strong>
            <span>待补齐页面</span>
          </div>
          <div class="metric-item">
            <strong>{{ selectedInventoryRows.length }}</strong>
            <span>当前勾选数</span>
          </div>
        </div>

        <p class="inventory-caption">
          盘点来源：{{ inventorySourceText }}。当前会按路由去重，并把“当前计划未覆盖”的页面标记出来。
        </p>

        <el-table
          ref="inventoryTableRef"
          :data="pageInventory"
          row-key="id"
          size="small"
          border
          @selection-change="handleInventorySelectionChange"
        >
          <el-table-column type="selection" width="52" reserve-selection />
          <el-table-column prop="title" label="页面" min-width="170" />
          <el-table-column prop="route" label="路由" min-width="170" />
          <el-table-column label="来源" width="100">
            <template #default="{ row }">
              <el-tag :type="row.source === 'manual' ? 'warning' : row.source === 'menu' ? 'success' : 'info'">
                {{ buildInventorySourceLabel(row.source) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="推荐模板" width="120">
            <template #default="{ row }">
              <el-tag effect="plain">{{ buildTemplateLabel(row.recommendedTemplate) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="范围" width="100">
            <template #default="{ row }">
              <el-tag effect="plain" type="info">{{ row.scope }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="覆盖" width="100">
            <template #default="{ row }">
              <el-tag :type="isRouteCovered(row.route) ? 'success' : 'warning'">
                {{ isRouteCovered(row.route) ? '已覆盖' : '待补齐' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="readySelector" label="就绪选择器" min-width="180" show-overflow-tooltip />
          <el-table-column prop="matcher" label="首屏接口" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span>{{ row.matcher || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.source === 'manual'"
                text
                type="danger"
                @click="removeManualPage(row.id)"
              >
                删除
              </el-button>
              <span v-else>—</span>
            </template>
          </el-table-column>
        </el-table>
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
                  <el-select v-model="step.type" placeholder="选择步骤类型" @change="handleStepTypeChange(step)">
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
                    <el-select v-model="step.screenshotTarget" @change="handleScreenshotTargetChange(step)">
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

                  <div v-if="step.dialogAction !== 'waitVisible'" class="capture-block">
                    <div class="inline-block__header">
                      <strong>变量捕获</strong>
                      <el-button text @click="addCapture(step)">新增捕获</el-button>
                    </div>
                    <div v-if="!step.captures || step.captures.length === 0" class="empty-inline">
                      若弹窗动作会触发接口，可在这里提取响应中的主键、编码等变量。
                    </div>
                    <div
                      v-for="(capture, captureIndex) in step.captures"
                      :key="`${step.id}-dialog-capture-${captureIndex}`"
                      class="row-editor"
                    >
                      <el-input v-model="capture.variable" placeholder="变量名，如 userId" />
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

    <StandardFormDrawer
      v-model="showImportDialog"
      eyebrow="Automation Import"
      title="导入自动化计划"
      subtitle="统一通过右侧抽屉粘贴并导入 JSON 计划，导入后会替换当前编排内容。"
      size="48rem"
      @close="handleImportDialogClose"
    >
      <el-input
        v-model="importText"
        type="textarea"
        :rows="18"
        placeholder="请粘贴导出的 JSON 计划"
      />
      <template #footer>
        <div class="action-row">
          <el-button @click="handleImportDialogClose">取消</el-button>
          <el-button type="primary" @click="applyImport">导入并替换当前计划</el-button>
        </div>
      </template>
    </StandardFormDrawer>

    <StandardFormDrawer
      v-model="showManualPageDialog"
      eyebrow="Page Inventory"
      title="新增自定义页面"
      subtitle="统一通过右侧抽屉补充未纳入菜单树的页面盘点信息，并生成推荐测试模板。"
      size="46rem"
      @close="handleManualPageDialogClose"
    >
      <div class="scenario-grid">
        <label class="field-card">
          <span>页面名称</span>
          <el-input v-model="manualPageDraft.title" placeholder="例如：外部采购门户" />
        </label>
        <label class="field-card">
          <span>页面路由</span>
          <el-input v-model="manualPageDraft.route" placeholder="/external-dashboard" />
        </label>
        <label class="field-card field-card--wide">
          <span>页面说明</span>
          <el-input
            v-model="manualPageDraft.caption"
            type="textarea"
            :rows="2"
            placeholder="说明该页面的业务目标、页面职责或首屏特征"
          />
        </label>
        <label class="field-card">
          <span>推荐模板</span>
          <el-select v-model="manualPageDraft.recommendedTemplate">
            <el-option
              v-for="type in inventoryTemplateOptions"
              :key="type"
              :label="buildTemplateLabel(type)"
              :value="type"
            />
          </el-select>
        </label>
        <label class="field-card">
          <span>执行范围</span>
          <el-select v-model="manualPageDraft.scope">
            <el-option v-for="scope in scopeOptions" :key="scope" :label="scope" :value="scope" />
          </el-select>
        </label>
        <label class="field-card">
          <span>就绪选择器</span>
          <el-input v-model="manualPageDraft.readySelector" placeholder="[data-testid=&quot;console-page-title&quot;]" />
        </label>
        <label class="field-card">
          <span>首屏接口 Matcher</span>
          <el-input v-model="manualPageDraft.matcher" placeholder="/api/external/dashboard" />
        </label>
        <label class="field-card field-card--switch">
          <span>需要登录</span>
          <el-switch v-model="manualPageDraft.requiresLogin" />
        </label>
      </div>
      <template #footer>
        <div class="action-row">
          <el-button @click="handleManualPageDialogClose">取消</el-button>
          <el-button type="primary" @click="saveManualPage">保存并加入页面盘点</el-button>
        </div>
      </template>
    </StandardFormDrawer>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';
import { ElMessage } from '@/utils/message';
import { usePermissionStore } from '../stores/permission';

import PanelCard from '../components/PanelCard.vue';
import ResponsePanel from '../components/ResponsePanel.vue';
import StandardFormDrawer from '../components/StandardFormDrawer.vue';
import type {
  AutomationPageInventoryItem,
  AutomationPlanDocument,
  AutomationScenarioConfig,
  AutomationScenarioTemplateType,
  AutomationStep
} from '../types/automation';
import {
  buildAutomationPageInventory,
  buildAutomationCommand,
  buildPageCoverageSummary,
  buildPlanSuggestions,
  buildScenarioPreviews,
  collectScenarioRoutes,
  createManualInventoryItem,
  createScenarioFromInventory,
  createDefaultAutomationPlan,
  createFormSubmitScenario,
  createListDetailScenario,
  createPageSmokeScenario,
  createAutomationId,
  duplicateScenario,
  loadSavedAutomationInventory,
  loadSavedAutomationPlan,
  normalizeAutomationPlan,
  saveAutomationInventory,
  saveAutomationPlan
} from '../utils/automationPlan';

type ScenarioTemplateType = 'pageSmoke' | 'formSubmit' | 'listDetail';
type InventoryTableInstance = {
  clearSelection?: () => void;
  toggleRowSelection?: (row: AutomationPageInventoryItem, selected?: boolean) => void;
};

const scopeOptions = ['delivery', 'baseline', 'regression', 'smoke'];
const locatorTypeOptions = ['css', 'placeholder', 'role', 'text', 'label', 'testId'];
const stepTypeOptions = [
  'fill',
  'click',
  'press',
  'setChecked',
  'selectOption',
  'uploadFile',
  'waitVisible',
  'triggerApi',
  'tableRowAction',
  'dialogAction',
  'assertText',
  'assertUrlIncludes',
  'assertScreenshot',
  'sleep'
];
const inventoryTemplateOptions: AutomationScenarioTemplateType[] = [
  'pageSmoke',
  'formSubmit',
  'listDetail',
  'login'
];

const permissionStore = usePermissionStore();

const plan = ref<AutomationPlanDocument>(normalizeAutomationPlan(loadSavedAutomationPlan()));
const inventoryTableRef = ref<InventoryTableInstance | null>(null);
const manualPages = ref<AutomationPageInventoryItem[]>(loadSavedAutomationInventory());
const selectedInventoryRows = ref<AutomationPageInventoryItem[]>([]);
const showImportDialog = ref(false);
const showManualPageDialog = ref(false);
const importText = ref('');
const manualPageDraft = ref<AutomationPageInventoryItem>(createManualInventoryItem());

const scenarioPreviews = computed(() => buildScenarioPreviews(plan.value));
const suggestions = computed(() => buildPlanSuggestions(plan.value));
const coveredRouteSet = computed(() => new Set(collectScenarioRoutes(plan.value)));
const pageInventory = computed(() =>
  buildAutomationPageInventory({
    menus: permissionStore.menus || [],
    manualPages: manualPages.value,
    includeStaticFallback: !permissionStore.isLoggedIn || (permissionStore.menus || []).length === 0
  })
);
const coverageSummary = computed(() => buildPageCoverageSummary(plan.value, pageInventory.value));
const uncoveredInventoryRows = computed(() =>
  pageInventory.value.filter((item) => !coveredRouteSet.value.has(item.route))
);
const inventorySourceText = computed(() =>
  permissionStore.isLoggedIn && (permissionStore.menus || []).length > 0
    ? '已授权菜单 + 自定义页面'
    : '静态路由种子 + 自定义页面'
);
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
  if (!step.locator && stepUsesLocator(step)) {
    step.locator = {
      type: 'css',
      value: ''
    };
  }
  if ((step.type === 'triggerApi' || step.type === 'tableRowAction') && !step.action) {
    step.action = {
      type: 'click',
      locator: {
        type: 'css',
        value: ''
      }
    };
  }
  if (step.type === 'dialogAction') {
    step.dialogAction = step.dialogAction || 'waitVisible';
    if (step.dialogAction === 'custom' && !step.action) {
      step.action = {
        type: 'click',
        locator: {
          type: 'css',
          value: ''
        }
      };
    }
  }
  if (step.type === 'assertScreenshot') {
    step.screenshotTarget = step.screenshotTarget || 'page';
    step.threshold = step.threshold ?? 0;
    step.fullPage = step.fullPage ?? true;
    if (step.screenshotTarget === 'locator' && !step.locator) {
      step.locator = {
        type: 'css',
        value: ''
      };
    }
  }
  if (!stepSupportsCaptures(step.type)) {
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

watch(
  manualPages,
  (value) => {
    saveAutomationInventory(value);
  },
  {
    deep: true
  }
);

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

function stepSupportsCaptures(stepType: string): boolean {
  return ['triggerApi', 'tableRowAction', 'dialogAction'].includes(stepType);
}

function buildTemplateLabel(template: AutomationScenarioTemplateType): string {
  switch (template) {
    case 'formSubmit':
      return '表单提交';
    case 'listDetail':
      return '列表详情';
    case 'login':
      return '登录前置';
    default:
      return '页面冒烟';
  }
}

function buildInventorySourceLabel(source: AutomationPageInventoryItem['source']): string {
  switch (source) {
    case 'menu':
      return '授权菜单';
    case 'manual':
      return '手工补充';
    default:
      return '静态种子';
  }
}

function isRouteCovered(route: string): boolean {
  return coveredRouteSet.value.has(route);
}

function handleInventorySelectionChange(rows: AutomationPageInventoryItem[]) {
  selectedInventoryRows.value = rows;
}

async function refreshPageInventory() {
  if (permissionStore.isLoggedIn) {
    try {
      await permissionStore.ensureInitialized(true);
    } catch {
      ElMessage.warning('菜单权限刷新失败，已保留当前本地页面盘点结果');
    }
  }
  await nextTick();
  ElMessage.success(`页面盘点已刷新，共识别 ${pageInventory.value.length} 个页面`);
}

async function selectUncoveredPages() {
  await nextTick();
  inventoryTableRef.value?.clearSelection?.();
  uncoveredInventoryRows.value.forEach((item) => inventoryTableRef.value?.toggleRowSelection?.(item, true));

  if (uncoveredInventoryRows.value.length === 0) {
    ElMessage.success('当前页面盘点已全部覆盖');
    return;
  }
  ElMessage.success(`已勾选 ${uncoveredInventoryRows.value.length} 个待补齐页面`);
}

function appendInventoryScenarios(items: AutomationPageInventoryItem[]) {
  if (items.length === 0) {
    ElMessage.warning('请先选择要生成脚手架的页面');
    return;
  }

  const routeSet = new Set(coveredRouteSet.value);
  let appendedCount = 0;
  let skippedCount = 0;

  items.forEach((item) => {
    if (routeSet.has(item.route)) {
      skippedCount += 1;
      return;
    }

    const scenario = createScenarioFromInventory(item);
    scenario.steps.forEach(ensureStepShape);
    plan.value.scenarios.push(scenario);
    routeSet.add(item.route);
    appendedCount += 1;
  });

  if (appendedCount === 0) {
    ElMessage.success('所选页面已全部在当前计划中覆盖');
    return;
  }

  if (skippedCount > 0) {
    ElMessage.success(`已新增 ${appendedCount} 个页面脚手架，跳过 ${skippedCount} 个已覆盖页面`);
    return;
  }

  ElMessage.success(`已新增 ${appendedCount} 个页面脚手架`);
}

function generateSelectedInventoryScenarios() {
  appendInventoryScenarios(selectedInventoryRows.value);
}

function generateUncoveredInventoryScenarios() {
  appendInventoryScenarios(uncoveredInventoryRows.value);
}

function openManualPageDialog() {
  manualPageDraft.value = createManualInventoryItem();
  showManualPageDialog.value = true;
}

function saveManualPage() {
  const nextItem = createManualInventoryItem(manualPageDraft.value);
  const existingIndex = manualPages.value.findIndex((item) => item.route === nextItem.route);

  if (existingIndex >= 0) {
    manualPages.value.splice(existingIndex, 1, nextItem);
    ElMessage.success('已更新自定义页面盘点项');
  } else {
    manualPages.value.push(nextItem);
    ElMessage.success('已新增自定义页面盘点项');
  }

  handleManualPageDialogClose();
}

function removeManualPage(id: string) {
  const index = manualPages.value.findIndex((item) => item.id === id);
  if (index < 0) {
    return;
  }
  manualPages.value.splice(index, 1);
  ElMessage.success('已移除自定义页面盘点项');
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

function handleStepTypeChange(step: AutomationStep) {
  ensureStepShape(step);
}

function handleScreenshotTargetChange(step: AutomationStep) {
  ensureStepShape(step);
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
    handleImportDialogClose();
    ElMessage.success('自动化计划已导入');
  } catch {
    ElMessage.error('导入失败，请检查 JSON 格式是否正确');
  }
}

function handleImportDialogClose() {
  showImportDialog.value = false;
  importText.value = '';
}

function handleManualPageDialogClose() {
  showManualPageDialog.value = false;
  manualPageDraft.value = createManualInventoryItem();
}

watch(
  () => showImportDialog.value,
  (visible) => {
    if (!visible) {
      importText.value = '';
    }
  }
);

watch(
  () => showManualPageDialog.value,
  (visible) => {
    if (!visible) {
      manualPageDraft.value = createManualInventoryItem();
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

.inventory-metric-list {
  margin-bottom: 1rem;
}

.inventory-caption {
  margin: 0 0 1rem;
  color: var(--text-secondary);
  line-height: 1.7;
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
