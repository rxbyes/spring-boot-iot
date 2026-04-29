<template>
  <StandardPageShell class="rule-definition-view">
    <StandardWorkbenchPanel
      title="阈值策略"
      :description="`当前 ${pagination.total} 条阈值策略，支持告警触发和转事件配置。`"
      show-header-actions
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-notices
      show-toolbar
      show-pagination
    >
      <template #header-actions>
        <StandardButton
          v-if="isAdvancedScopeView"
          v-permission="'risk:rule-definition:edit'"
          action="batch"
          @click="handleProductTypeTemplateAdd('MONITORING')"
        >
          新增系统模板
        </StandardButton>
        <StandardButton v-permission="'risk:rule-definition:edit'" action="add" @click="handleAdd">新增规则</StandardButton>
      </template>

      <template #filters>
        <StandardListFilterHeader :model="filters">
          <template #primary>
            <el-form-item>
              <el-input v-model="filters.ruleName" placeholder="规则名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="filters.metricIdentifier" placeholder="测点标识符" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.scopeView" placeholder="策略视图" @change="handleScopeViewChange">
                <el-option
                  v-for="option in scopeViewOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.ruleScope" placeholder="策略范围" clearable>
                <el-option
                  v-for="option in currentRuleScopeFilterOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.productType" placeholder="产品类型" clearable>
                <el-option
                  v-for="option in productTypeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.alarmLevel" placeholder="告警等级" clearable>
                <el-option
                  v-for="option in alarmLevelOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.status" placeholder="状态" clearable>
                <el-option label="启用" :value="0" />
                <el-option label="停用" :value="1" />
              </el-select>
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="query" @click="handleSearch">查询</StandardButton>
            <StandardButton action="reset" @click="handleReset">重置</StandardButton>
          </template>
        </StandardListFilterHeader>
      </template>

      <template #applied-filters>
        <StandardAppliedFiltersBar
          :tags="activeFilterTags"
          @remove="handleRemoveAppliedFilter"
          @clear="handleClearAppliedFilters"
        />
      </template>

      <template #notices>
        <div class="rule-definition-notice-stack">
          <el-alert
            title="优先核查红色告警和已开启转事件的规则，确保风险触发策略与处置流程保持一致。"
            type="info"
            :closable="false"
            show-icon
            class="view-alert"
          />
          <el-alert
            v-if="missingPolicyTotal > 0"
            :title="`待配置阈值策略 ${missingPolicyTotal} 项，已绑定测点还没有进入统一判级。`"
            type="warning"
            :closable="false"
            show-icon
            class="view-alert"
          >
            <div v-if="missingPolicySummaryItems.length > 0" class="rule-definition-governance-summary">
              <div class="rule-definition-governance-summary__header">
                <div>
                  <strong>按产品测点聚合 {{ missingPolicySummaryTotal }} 组</strong>
                  <span>优先给绑定量最高的组配置产品默认阈值。</span>
                </div>
                <div class="rule-definition-governance-summary__actions">
                  <StandardButton action="view" link @click="handleSelectVisibleMissingPolicySummaries">选择当前组</StandardButton>
                  <StandardButton action="reset" link :disabled="selectedMissingPolicySummaryKeys.length === 0" @click="handleClearMissingPolicySummarySelection">
                    清空选择
                  </StandardButton>
                  <StandardButton
                    v-permission="'risk:rule-definition:edit'"
                    action="batch"
                    link
                    :disabled="selectedMissingPolicySummaryKeys.length === 0"
                    @click="handleGenerateSelectedDefaultDrafts"
                  >
                    生成草稿 {{ selectedMissingPolicySummaryKeys.length }}
                  </StandardButton>
                </div>
              </div>
              <ul class="rule-definition-governance-summary-list">
                <li
                  v-for="item in missingPolicySummaryItems"
                  :key="`${item.productId || 'product'}-${item.riskMetricId || item.metricIdentifier}`"
                >
                  <el-checkbox
                    :model-value="isMissingPolicySummarySelected(item)"
                    :disabled="!canCreateProductDefaultDraft(item)"
                    @change="(checked) => handleMissingPolicySummarySelectionChange(item, Boolean(checked))"
                  />
                  <div class="rule-definition-governance-summary-list__main">
                    <div>
                      <strong>{{ item.productName || item.productKey || '未识别产品' }}</strong>
                      <span>{{ item.metricName || item.metricIdentifier || '--' }}</span>
                    </div>
                    <span>{{ item.bindingCount || 0 }} 个绑定 · {{ item.riskPointCount || 0 }} 个风险点 · {{ item.deviceCount || 0 }} 台设备</span>
                    <span v-if="resolveRecommendedExpression(item)" class="rule-definition-threshold-recommendation">
                      建议 {{ resolveRecommendedExpression(item) }} · {{ item.recommendationWindowDays || 15 }} 天 · {{ item.recommendationSampleCount || 0 }} 条
                    </span>
                    <span v-else-if="item.recommendationStatus" class="rule-definition-threshold-recommendation is-muted">
                      {{ formatRecommendationStatus(item) }}
                    </span>
                  </div>
                  <StandardButton
                    v-permission="'risk:rule-definition:edit'"
                    action="add"
                    link
                    :disabled="!canCreateProductDefaultDraft(item)"
                    @click="handleAddSingleProductDefaultDraft(item)"
                  >
                    加入草稿
                  </StandardButton>
                </li>
              </ul>
              <div v-if="productDefaultDrafts.length > 0" class="rule-definition-default-drafts">
                <div class="rule-definition-default-drafts__header">
                  <div>
                    <strong>产品默认策略草稿 {{ productDefaultDrafts.length }} 项</strong>
                    <span>补充表达式后可批量提交，仍走阈值策略原校验与审批链路。</span>
                  </div>
                  <div class="rule-definition-default-drafts__actions">
                    <StandardButton
                      v-permission="'risk:rule-definition:edit'"
                      action="batch"
                      link
                      :loading="batchDraftSubmitLoading"
                      :disabled="submittableProductDefaultDrafts.length === 0"
                      @click="handleSubmitProductDefaultDrafts"
                    >
                      批量提交 {{ submittableProductDefaultDrafts.length }}
                    </StandardButton>
                    <StandardButton action="reset" link @click="handleClearProductDefaultDrafts">清空草稿</StandardButton>
                  </div>
                </div>
                <div class="rule-definition-default-draft-template">
                  <el-input v-model="productDefaultDraftTemplate.expression" placeholder="批量表达式，例如：value >= 10" />
                  <el-input-number v-model="productDefaultDraftTemplate.duration" :min="0" :max="3600" placeholder="持续秒数" />
                  <el-select v-model="productDefaultDraftTemplate.alarmLevel" placeholder="告警等级">
                    <el-option
                      v-for="option in alarmLevelOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                  <el-radio-group v-model="productDefaultDraftTemplate.convertToEvent">
                    <el-radio :value="0">不转事件</el-radio>
                    <el-radio :value="1">转事件</el-radio>
                  </el-radio-group>
                  <el-radio-group v-model="productDefaultDraftTemplate.applyMode">
                    <el-radio value="EMPTY_ONLY">仅空表达式</el-radio>
                    <el-radio value="ALL">覆盖全部</el-radio>
                  </el-radio-group>
                  <StandardButton action="batch" link @click="handleApplyProductDefaultDraftTemplate">套用到草稿</StandardButton>
                </div>
                <ul class="rule-definition-default-draft-list">
                  <li v-for="draft in productDefaultDrafts" :key="getMissingPolicySummaryKey(draft)">
                    <div class="rule-definition-default-draft-list__main">
                      <div>
                        <strong>{{ draft.productName || draft.productKey || '未识别产品' }}</strong>
                        <span>{{ draft.metricName || draft.metricIdentifier || '--' }}</span>
                      </div>
                      <span v-if="draft.recommendationStatus" class="rule-definition-threshold-recommendation">
                        {{ formatRecommendationStatus(draft) }}
                      </span>
                      <div v-if="draft.submitStatus === 'FAILED'" class="rule-definition-default-draft-error">
                        <strong>提交失败</strong>
                        <span>{{ draft.submitMessage || '请检查草稿配置后重试' }}</span>
                      </div>
                      <div class="rule-definition-default-draft-editor">
                        <el-input v-model="draft.ruleName" placeholder="规则名称" @input="() => clearProductDefaultDraftSubmitState(draft)" />
                        <el-input v-model="draft.expression" placeholder="例如：value >= 10" @input="() => clearProductDefaultDraftSubmitState(draft)" />
                        <el-input-number v-model="draft.duration" :min="0" :max="3600" placeholder="持续秒数" />
                        <el-select v-model="draft.alarmLevel" placeholder="告警等级">
                          <el-option
                            v-for="option in alarmLevelOptions"
                            :key="option.value"
                            :label="option.label"
                            :value="option.value"
                          />
                        </el-select>
                        <el-radio-group v-model="draft.convertToEvent">
                          <el-radio :value="0">不转事件</el-radio>
                          <el-radio :value="1">转事件</el-radio>
                        </el-radio-group>
                      </div>
                    </div>
                    <div class="rule-definition-default-draft-list__actions">
                      <StandardButton v-permission="'risk:rule-definition:edit'" action="add" link @click="handleProductDefaultAdd(draft)">
                        配置
                      </StandardButton>
                      <StandardButton action="delete" link @click="handleRemoveProductDefaultDraft(draft)">移除</StandardButton>
                    </div>
                  </li>
                </ul>
              </div>
            </div>
            <ul class="rule-definition-governance-list">
              <li v-for="item in missingPolicyItems" :key="`${item.riskPointId || 'rp'}-${item.metricIdentifier || item.deviceCode}`">
                <strong>{{ item.metricName || item.metricIdentifier || '--' }}</strong>
                <span>{{ item.productName || item.productKey || '未识别产品' }}</span>
                <span>{{ item.riskPointName || '未命名风险点' }}</span>
                <span>{{ item.deviceCode || '--' }} · {{ item.deviceName || '未命名设备' }}</span>
              </li>
            </ul>
            <div class="rule-definition-governance-actions">
              <StandardButton v-permission="'risk:rule-definition:edit'" action="add" link @click="handleProductDefaultAdd()">
                按产品默认策略配置
              </StandardButton>
              <StandardButton action="query" link @click="handleProductDefaultFilter">查看产品默认策略</StandardButton>
            </div>
          </el-alert>
        </div>
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[`已选 ${selectedRows.length} 项`, `启用 ${enabledCount} 项`, `转事件 ${convertToEventCount} 项`, `待配 ${missingPolicyTotal} 项`, `红色 ${redRuleCount} 项`]"
        >
          <template #right>
            <StandardButton action="reset" link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</StandardButton>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <div
        v-loading="loading && hasRecords"
        class="ops-list-result-panel standard-list-surface"
        element-loading-text="正在刷新阈值策略列表"
        element-loading-background="var(--loading-mask-bg)"
      >
        <div v-if="showListSkeleton" class="ops-list-loading-state" aria-live="polite" aria-busy="true">
          <div class="ops-list-loading-state__summary">
            <span v-for="item in 3" :key="item" class="ops-list-loading-pulse ops-list-loading-pill" />
          </div>
          <div class="ops-list-loading-table ops-list-loading-table--header">
            <span v-for="item in 6" :key="`rule-head-${item}`" class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--header" />
          </div>
          <div v-for="row in 5" :key="`rule-row-${row}`" class="ops-list-loading-table ops-list-loading-table--row">
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-pill ops-list-loading-pill--status" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--short" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--short" />
          </div>
        </div>

        <template v-else-if="hasRecords">
          <el-table
            ref="tableRef"
            :data="ruleList"
            border
            stripe
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="48" />
            <StandardTableTextColumn prop="ruleName" label="规则名称" :min-width="200">
              <template #default="{ row }">
                <span class="rule-definition-table-ellipsis" :title="getRuleNameTitle(row)">
                  {{ getRuleDisplayName(row) }}
                </span>
              </template>
            </StandardTableTextColumn>
            <el-table-column prop="ruleScope" label="策略范围" width="120">
              <template #default="{ row }">
                <el-tag :type="getRuleScopeTagType(row.ruleScope)" round>{{ getRuleScopeText(row.ruleScope) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="适用对象" width="180">
              <template #default="{ row }">
                <span class="rule-definition-table-ellipsis" :title="getRuleScopeTargetText(row)">
                  {{ getRuleScopeTargetText(row) }}
                </span>
              </template>
            </el-table-column>
            <StandardTableTextColumn prop="metricName" label="测点" :min-width="220">
              <template #default="{ row }">
                <span class="rule-definition-table-ellipsis" :title="getMetricDisplayText(row)">
                  {{ getMetricDisplayText(row) }}
                </span>
              </template>
            </StandardTableTextColumn>
            <StandardTableTextColumn prop="expression" label="表达式" :min-width="220" />
            <StandardTableTextColumn prop="duration" label="持续时间(秒)" :width="120" />
            <el-table-column prop="alarmLevel" label="告警等级" width="100">
              <template #default="{ row }">
                <el-tag :type="getAlarmLevelType(row.alarmLevel)" round>{{ getAlarmLevelText(row.alarmLevel) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="convertToEvent" label="转事件" width="100">
              <template #default="{ row }">
                <el-tag :type="row.convertToEvent === 1 ? 'success' : 'info'" round>
                  {{ row.convertToEvent === 1 ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" round>{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <StandardTableTextColumn prop="createTime" label="创建时间" :width="180">
              <template #default="{ row }">
                <span class="rule-definition-table-ellipsis" :title="formatDateTime(row.createTime)">
                  {{ formatDateTime(row.createTime) }}
                </span>
              </template>
            </StandardTableTextColumn>
            <el-table-column
              label="操作"
              :width="ruleActionColumnWidth"
              fixed="right"
              class-name="standard-row-actions-column"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row }">
                <StandardWorkbenchRowActions
                  variant="table"
                  :direct-items="getRuleRowActions()"
                  @command="(command) => handleRuleRowAction(command, row)"
                />
              </template>
            </el-table-column>
          </el-table>
        </template>

        <div v-else-if="!loading" class="standard-list-empty-state">
          <EmptyState :title="emptyStateTitle" :description="emptyStateDescription" />
          <div class="standard-list-empty-state__actions">
            <StandardButton v-if="hasAppliedFilters" action="reset" @click="handleClearAppliedFilters">清空筛选条件</StandardButton>
            <StandardButton v-else v-permission="'risk:rule-definition:edit'" action="add" @click="handleAdd">新增规则</StandardButton>
          </div>
        </div>
      </div>

      <template #pagination>
        <div v-if="pagination.total > 0" class="ops-pagination">
          <StandardPagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </StandardWorkbenchPanel>

    <StandardFormDrawer
      v-model="formVisible"
      :title="formTitle"
      subtitle="统一通过右侧抽屉维护阈值策略与告警配置。"
      size="44rem"
      @close="handleFormClose"
    >
      <div class="ops-drawer-stack">
        <div class="ops-drawer-note">
          <strong>配置提示</strong>
          <span>建议先确认测点标识、阈值表达式和持续时间，再决定是否转事件，以保持告警触发和处置链路的一致性。</span>
        </div>
        <div v-if="governanceContextNote" class="ops-drawer-note">
          <strong>边界提示</strong>
          <span>{{ governanceContextNote }}</span>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="ops-drawer-form">
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>策略适用范围</h3>
                <p>默认按产品生效；设备个性和绑定个性用于少量覆盖，系统模板仅作为高级兜底。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="策略范围" prop="ruleScope" class="ops-drawer-grid__full">
                <el-radio-group v-model="form.ruleScope">
                  <el-radio
                    v-for="option in formRuleScopeOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="form.ruleScope === 'PRODUCT'" label="适用产品" prop="productId">
                <el-select v-model="form.productId" placeholder="请选择产品" filterable clearable>
                  <el-option
                    v-for="product in productOptions"
                    :key="product.id"
                    :label="product.productName || product.productKey"
                    :value="product.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item v-if="form.ruleScope === 'PRODUCT_TYPE'" label="产品类型" prop="productType">
                <el-select v-model="form.productType" placeholder="请选择产品类型" filterable>
                  <el-option
                    v-for="option in productTypeOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item v-if="form.ruleScope === 'DEVICE'" label="设备ID" prop="deviceId">
                <el-input v-model="form.deviceId" placeholder="请输入设备ID" />
              </el-form-item>
              <el-form-item v-if="form.ruleScope === 'BINDING'" label="风险绑定ID" prop="riskPointDeviceId">
                <el-input v-model="form.riskPointDeviceId" placeholder="请输入风险绑定ID" />
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>规则基础信息</h3>
                <p>先确认规则名称、测点标识和阈值表达式，保证同类策略具备清晰可读的维护口径。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="规则名称" prop="ruleName">
                <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
              </el-form-item>
              <el-form-item label="测点标识符" prop="metricIdentifier">
                <el-input v-model="form.metricIdentifier" placeholder="请输入测点标识符" />
              </el-form-item>
              <el-form-item label="测点名称" prop="metricName">
                <el-input v-model="form.metricName" placeholder="请输入测点名称" />
              </el-form-item>
              <el-form-item label="表达式" prop="expression">
                <el-input v-model="form.expression" placeholder="例如：value > 100" />
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>触发策略</h3>
                <p>统一配置触发持续时间、告警等级与转事件开关，确保告警策略与处置闭环匹配。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="持续时间(秒)" prop="duration">
                <el-input-number v-model="form.duration" :min="0" :max="3600" placeholder="请输入持续时间" />
              </el-form-item>
              <el-form-item label="告警等级" prop="alarmLevel">
                <el-select v-model="form.alarmLevel" placeholder="请选择告警等级">
                  <el-option
                    v-for="option in alarmLevelOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="转事件">
                <el-radio-group v-model="form.convertToEvent">
                  <el-radio :value="0">否</el-radio>
                  <el-radio :value="1">是</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="form.status">
                  <el-radio :value="0">启用</el-radio>
                  <el-radio :value="1">停用</el-radio>
                </el-radio-group>
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>通知与说明</h3>
                <p>维护通知通道和补充说明，便于后续排障、回顾和跨岗位协同。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="通知方式" class="ops-drawer-grid__full">
                <el-checkbox-group v-model="form.notificationMethods">
                  <el-checkbox label="email">邮件</el-checkbox>
                  <el-checkbox label="sms">短信</el-checkbox>
                  <el-checkbox label="wechat">微信</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="描述" prop="remark" class="ops-drawer-grid__full">
                <el-input v-model="form.remark" type="textarea" :rows="4" placeholder="请输入规则说明、适用范围或维护备注" />
              </el-form-item>
            </div>
          </section>
        </el-form>
      </div>
      <template #footer>
        <StandardDrawerFooter
          :confirm-loading="submitLoading"
          @cancel="formVisible = false"
          @confirm="handleSubmit"
        />
      </template>
    </StandardFormDrawer>

    <StandardFormDrawer
      v-model="effectivePreviewVisible"
      title="生效策略预览"
      subtitle="按绑定个性、设备个性、产品默认、产品类型模板、测点通用的顺序解释最终命中。"
      size="42rem"
    >
      <div class="ops-drawer-stack">
        <div v-if="effectivePreviewLoading" class="ops-drawer-note">
          <strong>正在预览</strong>
          <span>正在按当前策略上下文计算最终生效规则。</span>
        </div>
        <template v-else-if="effectivePreview">
          <div class="ops-drawer-note">
            <strong>{{ effectivePreview.hasMatchedRule ? '已命中策略' : '未命中策略' }}</strong>
            <span>{{ effectivePreview.decision || '当前上下文暂无可生效阈值策略。' }}</span>
          </div>
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>预览上下文</h3>
                <p>{{ getEffectivePreviewContextText(effectivePreview) }}</p>
              </div>
            </div>
            <div class="rule-definition-preview-context">
              <span>测点：{{ effectivePreview.metricIdentifier || '--' }}</span>
              <span>产品：{{ effectivePreview.productId || '--' }}</span>
              <span>设备：{{ effectivePreview.deviceId || '--' }}</span>
              <span>绑定：{{ effectivePreview.riskPointDeviceId || '--' }}</span>
            </div>
          </section>
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>候选优先级</h3>
                <p>仅启用策略参与预览，未匹配当前对象的候选会保留原因。</p>
              </div>
            </div>
            <ul class="rule-definition-preview-list">
              <li
                v-for="candidate in effectivePreview.candidates || []"
                :key="`${candidate.ruleId || candidate.ruleName}-${candidate.ruleScope}`"
                :class="{ 'is-selected': candidate.selected }"
              >
                <div>
                  <strong>{{ candidate.ruleName || '--' }}</strong>
                  <span>{{ candidate.ruleScopeText || getRuleScopeText(candidate.ruleScope) }} · {{ candidate.scopeTarget || '--' }}</span>
                </div>
                <div>
                  <el-tag :type="candidate.selected ? 'success' : candidate.matchedContext ? 'warning' : 'info'" round>
                    {{ candidate.selected ? '最终生效' : candidate.matchedContext ? '候选匹配' : '上下文不匹配' }}
                  </el-tag>
                  <span>{{ candidate.expression || '--' }}</span>
                </div>
                <p>{{ candidate.reason || '--' }}</p>
              </li>
            </ul>
          </section>
        </template>
      </div>
    </StandardFormDrawer>
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from '@/utils/message';
import EmptyState from '@/components/EmptyState.vue';
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue';
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue';
import StandardFormDrawer from '@/components/StandardFormDrawer.vue';
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardPageShell from '@/components/StandardPageShell.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue';
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue';
import { useListAppliedFilters } from '@/composables/useListAppliedFilters';
import { useServerPagination } from '@/composables/useServerPagination';
import { resolveWorkbenchActionColumnWidth } from '@/utils/adaptiveActionColumn';
import {
  fetchAlarmLevelOptions,
  getAlarmLevelTagType,
  getAlarmLevelText,
  normalizeAlarmLevel,
  type AlarmLevelOption
} from '@/utils/alarmLevel';
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm';
import { formatDateTime } from '@/utils/format';
import { normalizeOptionalId, sameId } from '@/utils/id';
import {
  listMissingPolicies,
  pageMissingPolicyProductMetricSummaries,
  type RiskGovernanceGapItem,
  type RiskGovernanceMissingPolicyProductMetricSummary
} from '@/api/riskGovernance';
import { productApi } from '@/api/product';
import type { IdType, Product } from '@/types/api';
import { pageRuleList, addRule, addRuleBatch, updateRule, deleteRule, previewEffectiveRule } from '../api/ruleDefinition';
import type {
  RuleDefinition,
  RuleDefinitionEffectivePreview,
  RuleDefinitionScope,
  RuleDefinitionScopeView
} from '../api/ruleDefinition';

type RuleRowActionCommand = 'preview' | 'edit' | 'delete';
type RuleScopeFilterValue = '' | RuleDefinitionScope;
type RuleScopeViewFilterValue = RuleDefinitionScopeView;
type DraftTemplateApplyMode = 'EMPTY_ONLY' | 'ALL';
type DraftSubmitStatus = 'IDLE' | 'FAILED';
type ProductDefaultDraft = RiskGovernanceMissingPolicyProductMetricSummary & {
  ruleName: string;
  expression: string;
  duration: number;
  alarmLevel: string;
  convertToEvent: number;
  status: number;
  submitStatus: DraftSubmitStatus;
  submitMessage: string;
};

const loading = ref(false);
const formVisible = ref(false);
const effectivePreviewVisible = ref(false);
const effectivePreviewLoading = ref(false);
const effectivePreview = ref<RuleDefinitionEffectivePreview | null>(null);
const ruleList = ref<RuleDefinition[]>([]);
const alarmLevelOptions = ref<AlarmLevelOption[]>([]);
const productOptions = ref<Product[]>([]);
const missingPolicyItems = ref<RiskGovernanceGapItem[]>([]);
const missingPolicySummaryItems = ref<RiskGovernanceMissingPolicyProductMetricSummary[]>([]);
const selectedMissingPolicySummaryKeys = ref<string[]>([]);
const productDefaultDrafts = ref<ProductDefaultDraft[]>([]);
const activeProductDefaultDraftKey = ref('');
const batchDraftSubmitLoading = ref(false);
const productDefaultDraftTemplate = reactive({
  expression: '',
  duration: 0,
  alarmLevel: 'blue',
  convertToEvent: 0,
  applyMode: 'EMPTY_ONLY' as DraftTemplateApplyMode
});
const tableRef = ref();
const selectedRows = ref<RuleDefinition[]>([]);
const ruleActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: [
    { command: 'preview', label: '预览' },
    { command: 'edit', label: '编辑' },
    { command: 'delete', label: '删除' }
  ],
  minWidth: 200
});

const filters = reactive({
  ruleName: '',
  metricIdentifier: '',
  scopeView: 'BUSINESS' as RuleScopeViewFilterValue,
  ruleScope: '' as RuleScopeFilterValue,
  productType: '',
  alarmLevel: '',
  status: '' as '' | number
});
const appliedFilters = reactive({
  ruleName: '',
  metricIdentifier: '',
  scopeView: 'BUSINESS' as RuleScopeViewFilterValue,
  ruleScope: '' as RuleScopeFilterValue,
  productType: '',
  alarmLevel: '',
  status: '' as '' | number
});

const route = useRoute();
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum } = useServerPagination();

const formRef = ref();
const formTitle = computed(() => (form.id ? '编辑规则' : '新增规则'));
const form = reactive({
  id: undefined as IdType | undefined,
  riskMetricId: undefined as IdType | undefined,
  ruleScope: 'PRODUCT' as RuleDefinitionScope,
  productType: 'MONITORING',
  productId: undefined as IdType | undefined,
  deviceId: '',
  riskPointDeviceId: '',
  ruleName: '',
  metricIdentifier: '',
  metricName: '',
  expression: '',
  duration: 0,
  alarmLevel: 'blue',
  notificationMethods: [] as string[],
  convertToEvent: 0,
  status: 0,
  remark: ''
});

const rules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleScope: [{ required: true, message: '请选择策略范围', trigger: 'change' }],
  productType: [{ required: true, message: '请选择产品类型', trigger: 'change' }],
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  deviceId: [{ required: true, message: '请输入设备ID', trigger: 'blur' }],
  riskPointDeviceId: [{ required: true, message: '请输入风险绑定ID', trigger: 'blur' }],
  metricIdentifier: [{ required: true, message: '请输入测点标识符', trigger: 'blur' }],
  expression: [{ required: true, message: '请输入表达式', trigger: 'blur' }],
  alarmLevel: [{ required: true, message: '请选择告警等级', trigger: 'change' }]
};

const submitLoading = ref(false);
const missingPolicyTotal = ref(0);
const missingPolicySummaryTotal = ref(0);
let latestListRequestId = 0;
let governanceCreateHandled = false;

const enabledCount = computed(() => ruleList.value.filter((item) => item.status === 0).length);
const convertToEventCount = computed(() => ruleList.value.filter((item) => item.convertToEvent === 1).length);
const redRuleCount = computed(() => ruleList.value.filter((item) => normalizeAlarmLevel(item.alarmLevel) === 'red').length);
const hasRecords = computed(() => ruleList.value.length > 0);
const showListSkeleton = computed(() => loading.value && !hasRecords.value);
const submittableProductDefaultDrafts = computed(() =>
  productDefaultDrafts.value.filter((draft) =>
    canCreateProductDefaultDraft(draft)
    && Boolean(draft.ruleName?.trim())
    && Boolean(draft.expression?.trim())
    && Boolean(draft.alarmLevel)
  )
);
const emptyStateTitle = computed(() => (hasAppliedFilters.value ? '没有符合条件的阈值策略' : '还没有阈值策略'));
const emptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? '已生效筛选暂时没有匹配结果，可以调整条件，或者直接清空当前筛选。'
    : '当前还没有阈值策略，先新增规则，再继续告警触发和事件转化治理。'
);
const governanceContextNote = computed(() => {
  if (!formVisible.value) {
    return '';
  }
  if (parseRouteStringQuery(route.query.governanceBoundary) !== 'collector-child') {
    return '';
  }
  if (parseRouteStringQuery(route.query.subjectOwnership) !== 'child') {
    return '';
  }
  return '当前规则针对子设备正式测点，采集器仅承担状态采集，不应作为阈值策略主体。';
});

const scopeViewOptions: Array<{ label: string; value: RuleDefinitionScopeView }> = [
  { label: '业务策略', value: 'BUSINESS' },
  { label: '系统模板', value: 'SYSTEM' },
  { label: '全部策略', value: 'ALL' }
];

const ruleScopeOptions: Array<{ label: string; value: RuleDefinitionScope; tagType: 'info' | 'success' | 'warning' | 'danger' }> = [
  { label: '测点通用（兼容）', value: 'METRIC', tagType: 'info' },
  { label: '产品类型模板（系统）', value: 'PRODUCT_TYPE', tagType: 'success' },
  { label: '产品默认', value: 'PRODUCT', tagType: 'success' },
  { label: '设备个性', value: 'DEVICE', tagType: 'warning' },
  { label: '绑定个性', value: 'BINDING', tagType: 'danger' }
];
const businessRuleScopeValues: RuleDefinitionScope[] = ['PRODUCT', 'DEVICE', 'BINDING'];
const systemRuleScopeValues: RuleDefinitionScope[] = ['METRIC', 'PRODUCT_TYPE'];
const businessRuleScopeOptions = ruleScopeOptions.filter((option) => businessRuleScopeValues.includes(option.value));
const systemRuleScopeOptions = ruleScopeOptions.filter((option) => systemRuleScopeValues.includes(option.value));
const isAdvancedScopeView = computed(() => filters.scopeView === 'SYSTEM' || filters.scopeView === 'ALL');
const currentRuleScopeFilterOptions = computed(() => {
  if (filters.scopeView === 'BUSINESS') {
    return businessRuleScopeOptions;
  }
  if (filters.scopeView === 'SYSTEM') {
    return systemRuleScopeOptions;
  }
  return ruleScopeOptions;
});
const formRuleScopeOptions = computed(() => {
  if (systemRuleScopeValues.includes(form.ruleScope) || isAdvancedScopeView.value) {
    return ruleScopeOptions;
  }
  return businessRuleScopeOptions;
});

const productTypeOptions = [
  { label: '监测型设备', value: 'MONITORING' },
  { label: '预警型设备', value: 'WARNING' },
  { label: '视频型设备', value: 'VIDEO' },
  { label: '未识别类型', value: 'UNKNOWN' }
];

const getRuleScopeOption = (scope?: string | null) => {
  const normalized = String(scope || 'METRIC').toUpperCase();
  return ruleScopeOptions.find((option) => option.value === normalized) || ruleScopeOptions[0];
};

const getScopeViewText = (scopeView?: string | null) => {
  const normalized = String(scopeView || 'BUSINESS').toUpperCase();
  return scopeViewOptions.find((option) => option.value === normalized)?.label || '业务策略';
};

const getRuleScopeText = (scope?: string | null) => getRuleScopeOption(scope).label;
const getRuleScopeTagType = (scope?: string | null) => getRuleScopeOption(scope).tagType;

const getCompactRuleScopeText = (scope?: string | null) =>
  getRuleScopeText(scope).replace(/（.*?）/g, '');

const getMetricNameText = (row: RuleDefinition) =>
  String(row.metricName || row.metricIdentifier || '未命名测点').trim();

const getMetricDisplayText = (row: RuleDefinition) => {
  const metricName = String(row.metricName || '').trim();
  const metricIdentifier = String(row.metricIdentifier || '').trim();
  if (metricName && metricIdentifier && metricName !== metricIdentifier) {
    return `${metricName}（${metricIdentifier}）`;
  }
  return metricName || metricIdentifier || '--';
};

const getRuleDisplayName = (row: RuleDefinition) => {
  const metricName = getMetricNameText(row);
  const alarmLevelText = getAlarmLevelText(row.alarmLevel, alarmLevelOptions.value);
  const normalizedAlarmLevelText = alarmLevelText && alarmLevelText !== '未标注' ? ` ${alarmLevelText}` : '';
  return `${getCompactRuleScopeText(row.ruleScope)}：${metricName}${normalizedAlarmLevelText}阈值`;
};

const getRuleNameTitle = (row: RuleDefinition) => {
  const displayName = getRuleDisplayName(row);
  const originalName = String(row.ruleName || '').trim();
  return originalName && originalName !== displayName
    ? `${displayName}（原名称：${originalName}）`
    : displayName;
};

const getProductTypeText = (type?: string | null) => {
  const normalized = String(type || '').toUpperCase();
  return productTypeOptions.find((option) => option.value === normalized)?.label || normalized;
};

const getProductDisplayName = (productId?: IdType | null) => {
  if (productId == null || productId === '') {
    return '';
  }
  const productIdText = String(productId);
  const product = productOptions.value.find((item) => sameId(item.id, productId));
  if (!product) {
    return productIdText;
  }
  return product.productName || product.productKey || productIdText;
};

const getRuleScopeTargetText = (row: RuleDefinition) => {
  const scope = getRuleScopeOption(row.ruleScope).value;
  if (scope === 'PRODUCT') {
    return getProductDisplayName(row.productId) || '--';
  }
  if (scope === 'PRODUCT_TYPE') {
    return row.productType ? getProductTypeText(row.productType) : '--';
  }
  if (scope === 'DEVICE') {
    return row.deviceId == null ? '--' : `设备 ${row.deviceId}`;
  }
  if (scope === 'BINDING') {
    return row.riskPointDeviceId == null ? '--' : `绑定 ${row.riskPointDeviceId}`;
  }
  return '通用';
};

const getEffectivePreviewContextText = (preview: RuleDefinitionEffectivePreview) => {
  const parts = [
    preview.productType ? `产品类型 ${getProductTypeText(preview.productType)}` : '',
    preview.productId == null ? '' : `产品 ${getProductDisplayName(preview.productId) || preview.productId}`,
    preview.deviceId == null ? '' : `设备 ${preview.deviceId}`,
    preview.riskPointDeviceId == null ? '' : `绑定 ${preview.riskPointDeviceId}`
  ].filter(Boolean);
  return parts.length > 0 ? parts.join(' / ') : '仅按测点通用上下文预览';
};

const getAlarmLevelType = (level: string) => getAlarmLevelTagType(level);

const getStatusType = (status: number) => {
  switch (status) {
    case 0:
      return 'success';
    case 1:
      return 'info';
    default:
      return 'info';
  }
};

const getStatusText = (status: number) => {
  switch (status) {
    case 0:
      return '启用';
    case 1:
      return '停用';
    default:
      return status.toString();
  }
};

const {
  tags: activeFilterTags,
  hasAppliedFilters,
  syncAppliedFilters,
  removeFilter: removeAppliedFilter
} = useListAppliedFilters({
  form: filters,
  applied: appliedFilters,
  fields: [
    { key: 'ruleName', label: '规则名称' },
    { key: 'metricIdentifier', label: '测点标识符' },
    {
      key: 'scopeView',
      label: (value) => `策略视图：${getScopeViewText(String(value || 'BUSINESS'))}`,
      isActive: (value) => String(value || 'BUSINESS').toUpperCase() !== 'BUSINESS',
      clearValue: 'BUSINESS' as RuleScopeViewFilterValue
    },
    { key: 'ruleScope', label: (value) => `策略范围：${getRuleScopeText(String(value || ''))}` },
    { key: 'productType', label: (value) => `产品类型：${getProductTypeText(String(value || ''))}` },
    { key: 'alarmLevel', label: (value) => `告警等级：${getAlarmLevelText(String(value || ''))}` },
    { key: 'status', label: (value) => `状态：${getStatusText(Number(value))}`, clearValue: '' as '' | number }
  ],
  defaults: {
    ruleName: '',
    metricIdentifier: '',
    scopeView: 'BUSINESS' as RuleScopeViewFilterValue,
    ruleScope: '' as RuleScopeFilterValue,
    productType: '',
    alarmLevel: '',
    status: '' as '' | number
  }
});

const loadRuleList = async () => {
  const requestId = ++latestListRequestId;
  loading.value = true;
  try {
    const [listResult, governanceResult, summaryResult] = await Promise.allSettled([
      pageRuleList({
        ruleName: appliedFilters.ruleName || undefined,
        metricIdentifier: appliedFilters.metricIdentifier || undefined,
        scopeView: appliedFilters.scopeView || 'BUSINESS',
        ruleScope: appliedFilters.ruleScope || undefined,
        productType: appliedFilters.productType || undefined,
        alarmLevel: appliedFilters.alarmLevel || undefined,
        status: appliedFilters.status === '' ? undefined : Number(appliedFilters.status),
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      }),
      listMissingPolicies({
        pageNum: 1,
        pageSize: 3
      }),
      pageMissingPolicyProductMetricSummaries({
        pageNum: 1,
        pageSize: 5
      })
    ]);
    if (requestId !== latestListRequestId) {
      return;
    }
    if (listResult.status === 'fulfilled' && listResult.value?.code === 200) {
      ruleList.value = applyPageResult(listResult.value.data);
    } else {
      ruleList.value = [];
    }

    if (governanceResult.status === 'fulfilled' && governanceResult.value?.code === 200) {
      missingPolicyItems.value = governanceResult.value.data.records ?? [];
      missingPolicyTotal.value = governanceResult.value.data.total ?? 0;
    } else {
      missingPolicyItems.value = [];
      missingPolicyTotal.value = 0;
    }

    if (summaryResult.status === 'fulfilled' && summaryResult.value?.code === 200) {
      missingPolicySummaryItems.value = summaryResult.value.data.records ?? [];
      missingPolicySummaryTotal.value = summaryResult.value.data.total ?? 0;
    } else {
      missingPolicySummaryItems.value = [];
      missingPolicySummaryTotal.value = 0;
    }
  } catch (error) {
    if (requestId !== latestListRequestId) {
      return;
    }
    console.error('查询规则列表失败', error);
  } finally {
    if (requestId === latestListRequestId) {
      loading.value = false;
    }
  }
};

const handleSearch = () => {
  syncAppliedFilters();
  resetPage();
  clearSelection();
  void loadRuleList();
};

const handleReset = () => {
  filters.ruleName = '';
  filters.metricIdentifier = '';
  filters.scopeView = 'BUSINESS';
  filters.ruleScope = '';
  filters.productType = '';
  filters.alarmLevel = '';
  filters.status = '';
  syncAppliedFilters();
  resetPage();
  clearSelection();
  void loadRuleList();
};

const handleSizeChange = (size: number) => {
  setPageSize(size);
  void loadRuleList();
};

const handlePageChange = (page: number) => {
  setPageNum(page);
  void loadRuleList();
};

const handleSelectionChange = (rows: RuleDefinition[]) => {
  selectedRows.value = rows;
};

const clearSelection = () => {
  tableRef.value?.clearSelection?.();
  selectedRows.value = [];
};

const handleRefresh = () => {
  clearSelection();
  void loadRuleList();
};

function getRuleRowActions() {
  return [
    { command: 'preview' as const, label: '生效预览' },
    { command: 'edit' as const, label: '编辑', permission: 'risk:rule-definition:edit' },
    { command: 'delete' as const, label: '删除', permission: 'risk:rule-definition:edit' }
  ];
}

function handleRuleRowAction(command: RuleRowActionCommand, row: RuleDefinition) {
  if (command === 'preview') {
    void handlePreviewEffectiveRule(row);
    return;
  }
  if (command === 'edit') {
    handleEdit(row);
    return;
  }
  void handleDelete(row);
}

const handlePreviewEffectiveRule = async (row: RuleDefinition) => {
  effectivePreviewVisible.value = true;
  effectivePreviewLoading.value = true;
  effectivePreview.value = null;
  try {
    const result = await previewEffectiveRule({
      tenantId: normalizeOptionalId(row.tenantId) || undefined,
      riskMetricId: normalizeOptionalId(row.riskMetricId) || undefined,
      metricIdentifier: row.metricIdentifier || undefined,
      productId: normalizeOptionalId(row.productId) || undefined,
      productType: row.productType || undefined,
      deviceId: normalizeOptionalId(row.deviceId) || undefined,
      riskPointDeviceId: normalizeOptionalId(row.riskPointDeviceId) || undefined
    });
    if (result.code === 200) {
      effectivePreview.value = result.data;
      return;
    }
    ElMessage.error(result.msg || '生效策略预览失败');
  } catch (error) {
    console.error('生效策略预览失败', error);
    ElMessage.error(error instanceof Error ? error.message : '生效策略预览失败');
  } finally {
    effectivePreviewLoading.value = false;
  }
};

const handleRemoveAppliedFilter = (key: string) => {
  removeAppliedFilter(key);
  resetPage();
  clearSelection();
  void loadRuleList();
};

const handleClearAppliedFilters = () => {
  handleReset();
};

function applyRouteQueryToFilters() {
  filters.ruleName = parseRouteStringQuery(route.query.ruleName);
  filters.metricIdentifier = parseRouteStringQuery(route.query.metricIdentifier);
  filters.scopeView = parseScopeViewQuery(route.query.scopeView);
  filters.ruleScope = parseRuleScopeQuery(route.query.ruleScope);
  handleScopeViewChange();
  filters.productType = parseProductTypeQuery(route.query.productType);
  filters.alarmLevel = parseRouteStringQuery(route.query.alarmLevel);
  filters.status = parseRouteNumberQuery(route.query.status) ?? '';
}

function parseRouteStringQuery(value: unknown) {
  const raw = Array.isArray(value) ? value[0] : value;
  return typeof raw === 'string' ? raw.trim() : '';
}

function parseRouteNumberQuery(value: unknown) {
  const text = parseRouteStringQuery(value);
  if (!text) {
    return undefined;
  }
  const parsed = Number(text);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function parseRouteIdQuery(value: unknown) {
  return normalizeOptionalId(parseRouteStringQuery(value));
}

function parseRuleScopeQuery(value: unknown): RuleScopeFilterValue {
  const scope = parseRouteStringQuery(value).toUpperCase();
  return ruleScopeOptions.some((option) => option.value === scope) ? scope as RuleDefinitionScope : '';
}

function parseScopeViewQuery(value: unknown): RuleScopeViewFilterValue {
  const scopeView = parseRouteStringQuery(value).toUpperCase();
  return scopeViewOptions.some((option) => option.value === scopeView)
    ? scopeView as RuleDefinitionScopeView
    : 'BUSINESS';
}

function parseProductTypeQuery(value: unknown) {
  const productType = parseRouteStringQuery(value).toUpperCase();
  return productTypeOptions.some((option) => option.value === productType) ? productType : '';
}

function parseGovernanceCreateContext() {
  if (parseRouteStringQuery(route.query.governanceAction) !== 'create') {
    return null;
  }
  if (parseRouteStringQuery(route.query.governanceSource) !== 'task') {
    return null;
  }
  if (parseRouteStringQuery(route.query.workItemCode) !== 'PENDING_THRESHOLD_POLICY') {
    return null;
  }
  return {
    riskMetricId: parseRouteIdQuery(route.query.riskMetricId),
    metricIdentifier: parseRouteStringQuery(route.query.metricIdentifier),
    metricName: parseRouteStringQuery(route.query.metricName)
  };
}

const loadAlarmLevelOptionList = async () => {
  try {
    alarmLevelOptions.value = await fetchAlarmLevelOptions();
    if (!form.alarmLevel) {
      form.alarmLevel = alarmLevelOptions.value[0]?.value || 'blue';
    }
    if (!productDefaultDraftTemplate.alarmLevel) {
      productDefaultDraftTemplate.alarmLevel = alarmLevelOptions.value[0]?.value || 'blue';
    }
  } catch (error) {
    console.error('加载告警等级字典失败', error);
    ElMessage.error(error instanceof Error ? error.message : '加载告警等级字典失败');
  }
};

const loadProductOptions = async () => {
  try {
    const res = await productApi.getAllProducts();
    productOptions.value = res.code === 200 ? res.data || [] : [];
  } catch (error) {
    productOptions.value = [];
    console.error('加载产品列表失败', error);
  }
};

const resetRuleForm = () => {
  form.id = undefined;
  form.riskMetricId = undefined;
  form.ruleScope = 'PRODUCT';
  form.productType = 'MONITORING';
  form.productId = undefined;
  form.deviceId = '';
  form.riskPointDeviceId = '';
  form.ruleName = '';
  form.metricIdentifier = '';
  form.metricName = '';
  form.expression = '';
  form.duration = 0;
  form.alarmLevel = alarmLevelOptions.value[0]?.value || 'blue';
  form.notificationMethods = [];
  form.convertToEvent = 0;
  form.status = 0;
  form.remark = '';
};

const handleAdd = () => {
  resetRuleForm();
  activeProductDefaultDraftKey.value = '';
  formVisible.value = true;
};

const handleScopeViewChange = () => {
  if (!currentRuleScopeFilterOptions.value.some((option) => option.value === filters.ruleScope)) {
    filters.ruleScope = '';
  }
};

const handleProductTypeTemplateAdd = (productType = 'MONITORING') => {
  resetRuleForm();
  activeProductDefaultDraftKey.value = '';
  const firstMissingItem = missingPolicySummaryItems.value[0] || missingPolicyItems.value[0];
  form.ruleScope = 'PRODUCT_TYPE';
  form.productType = productType;
  form.riskMetricId = normalizeOptionalId(firstMissingItem?.riskMetricId);
  form.metricIdentifier = firstMissingItem?.metricIdentifier || '';
  form.metricName = firstMissingItem?.metricName || '';
  form.ruleName = firstMissingItem?.metricName
    ? `${firstMissingItem.metricName} 监测型模板阈值`
    : '监测型设备阈值模板';
  formVisible.value = true;
};

const getMissingPolicySummaryKey = (
  item?: RiskGovernanceGapItem | RiskGovernanceMissingPolicyProductMetricSummary | null
) => {
  const productKey = item?.productId == null ? 'product' : String(item.productId);
  const metricKey = item?.riskMetricId == null
    ? (item?.metricIdentifier || 'metric')
    : String(item.riskMetricId);
  return `${productKey}:${metricKey}`;
};

const canCreateProductDefaultDraft = (
  item?: RiskGovernanceGapItem | RiskGovernanceMissingPolicyProductMetricSummary | null
) => Boolean(item?.productId != null && (item?.riskMetricId != null || item?.metricIdentifier));

const resolveRecommendedExpression = (item?: RiskGovernanceMissingPolicyProductMetricSummary | null) =>
  item?.recommendedExpression?.trim()
  || item?.recommendedUpperExpression?.trim()
  || item?.recommendedLowerExpression?.trim()
  || '';

const formatRecommendationStatus = (item?: RiskGovernanceMissingPolicyProductMetricSummary | null) => {
  const expression = resolveRecommendedExpression(item);
  if (expression) {
    return `建议 ${expression} · ${item?.recommendationWindowDays || 15} 天 · ${item?.recommendationSampleCount || 0} 条`;
  }
  const status = String(item?.recommendationStatus || '').trim().toUpperCase();
  if (status === 'FLAT_ZERO_REVIEW') {
    return '近况全 0，需人工复核';
  }
  if (status === 'INSUFFICIENT_SAMPLE') {
    return `样本不足 · ${item?.recommendationSampleCount || 0} 条`;
  }
  if (status === 'NO_NUMERIC_SAMPLE') {
    return '无近况数值样本';
  }
  if (status === 'REQUIRES_MANUAL_REVIEW') {
    return '双向波动，需人工复核';
  }
  if (status === 'UNSUPPORTED_PRODUCT_TYPE') {
    return '非监测型产品';
  }
  if (status === 'UNAVAILABLE') {
    return '近况推荐暂不可用';
  }
  return status || '';
};

const isMissingPolicySummarySelected = (item: RiskGovernanceMissingPolicyProductMetricSummary) =>
  selectedMissingPolicySummaryKeys.value.includes(getMissingPolicySummaryKey(item));

const handleMissingPolicySummarySelectionChange = (
  item: RiskGovernanceMissingPolicyProductMetricSummary,
  checked: boolean
) => {
  if (!canCreateProductDefaultDraft(item)) {
    return;
  }
  const key = getMissingPolicySummaryKey(item);
  if (checked) {
    if (!selectedMissingPolicySummaryKeys.value.includes(key)) {
      selectedMissingPolicySummaryKeys.value = [...selectedMissingPolicySummaryKeys.value, key];
    }
    return;
  }
  selectedMissingPolicySummaryKeys.value = selectedMissingPolicySummaryKeys.value.filter((itemKey) => itemKey !== key);
};

const handleSelectVisibleMissingPolicySummaries = () => {
  selectedMissingPolicySummaryKeys.value = missingPolicySummaryItems.value
    .filter((item) => canCreateProductDefaultDraft(item))
    .map((item) => getMissingPolicySummaryKey(item));
};

const handleClearMissingPolicySummarySelection = () => {
  selectedMissingPolicySummaryKeys.value = [];
};

const buildProductDefaultDraft = (item: RiskGovernanceMissingPolicyProductMetricSummary): ProductDefaultDraft => ({
  ...item,
  ruleName: item.metricName ? `${item.metricName} 产品默认阈值` : '产品默认阈值策略',
  expression: resolveRecommendedExpression(item),
  duration: 0,
  alarmLevel: alarmLevelOptions.value[0]?.value || 'blue',
  convertToEvent: 0,
  status: 0,
  submitStatus: 'IDLE',
  submitMessage: ''
});

const addProductDefaultDrafts = (items: RiskGovernanceMissingPolicyProductMetricSummary[]) => {
  const existingKeys = new Set(productDefaultDrafts.value.map((item) => getMissingPolicySummaryKey(item)));
  const nextDrafts = items.filter((item) => canCreateProductDefaultDraft(item) && !existingKeys.has(getMissingPolicySummaryKey(item)));
  if (nextDrafts.length === 0) {
    ElMessage.warning('没有可新增的产品默认策略草稿');
    return;
  }
  productDefaultDrafts.value = [...productDefaultDrafts.value, ...nextDrafts.map((item) => buildProductDefaultDraft(item))];
  selectedMissingPolicySummaryKeys.value = selectedMissingPolicySummaryKeys.value.filter((key) => !nextDrafts.some((item) => getMissingPolicySummaryKey(item) === key));
  ElMessage.success(`已生成 ${nextDrafts.length} 项产品默认策略草稿`);
};

const handleGenerateSelectedDefaultDrafts = () => {
  const selectedKeys = new Set(selectedMissingPolicySummaryKeys.value);
  const selectedItems = missingPolicySummaryItems.value.filter((item) => selectedKeys.has(getMissingPolicySummaryKey(item)));
  addProductDefaultDrafts(selectedItems);
};

const handleAddSingleProductDefaultDraft = (item: RiskGovernanceMissingPolicyProductMetricSummary) => {
  addProductDefaultDrafts([item]);
};

const handleRemoveProductDefaultDraft = (item: RiskGovernanceMissingPolicyProductMetricSummary) => {
  const key = getMissingPolicySummaryKey(item);
  productDefaultDrafts.value = productDefaultDrafts.value.filter((draft) => getMissingPolicySummaryKey(draft) !== key);
  if (activeProductDefaultDraftKey.value === key) {
    activeProductDefaultDraftKey.value = '';
  }
};

const handleClearProductDefaultDrafts = () => {
  productDefaultDrafts.value = [];
  activeProductDefaultDraftKey.value = '';
};

const clearProductDefaultDraftSubmitState = (draft: ProductDefaultDraft) => {
  draft.submitStatus = 'IDLE';
  draft.submitMessage = '';
};

const handleApplyProductDefaultDraftTemplate = () => {
  if (productDefaultDrafts.value.length === 0) {
    ElMessage.warning('请先生成产品默认策略草稿');
    return;
  }
  const expression = productDefaultDraftTemplate.expression.trim();
  let appliedCount = 0;
  productDefaultDrafts.value.forEach((draft) => {
    const shouldApply = productDefaultDraftTemplate.applyMode === 'ALL' || !draft.expression?.trim();
    if (!shouldApply) {
      return;
    }
    if (expression) {
      draft.expression = expression;
    }
    draft.duration = Number(productDefaultDraftTemplate.duration || 0);
    draft.alarmLevel = productDefaultDraftTemplate.alarmLevel || alarmLevelOptions.value[0]?.value || 'blue';
    draft.convertToEvent = Number(productDefaultDraftTemplate.convertToEvent || 0);
    clearProductDefaultDraftSubmitState(draft);
    appliedCount += 1;
  });
  if (appliedCount === 0) {
    ElMessage.warning('没有符合套用条件的草稿');
    return;
  }
  ElMessage.success(`已套用到 ${appliedCount} 项草稿`);
};

const buildProductDefaultDraftSubmitData = (draft: ProductDefaultDraft) => ({
  riskMetricId: normalizeOptionalId(draft.riskMetricId),
  ruleScope: 'PRODUCT' as RuleDefinitionScope,
  productType: undefined,
  productId: draft.productId == null ? undefined : draft.productId,
  deviceId: undefined,
  riskPointDeviceId: undefined,
  ruleName: draft.ruleName?.trim() || '产品默认阈值策略',
  metricIdentifier: draft.metricIdentifier || '',
  metricName: draft.metricName || '',
  expression: draft.expression?.trim() || '',
  duration: Number(draft.duration || 0),
  alarmLevel: draft.alarmLevel || alarmLevelOptions.value[0]?.value || 'blue',
  notificationMethods: undefined,
  convertToEvent: Number(draft.convertToEvent || 0),
  status: Number(draft.status ?? 0),
  remark: ''
});

const handleSubmitProductDefaultDrafts = async () => {
  const readyDrafts = submittableProductDefaultDrafts.value;
  if (readyDrafts.length === 0) {
    ElMessage.warning('请先补充草稿表达式');
    return;
  }
  batchDraftSubmitLoading.value = true;
  try {
    const res = await addRuleBatch(readyDrafts.map((draft) => buildProductDefaultDraftSubmitData(draft)));
    if (res.code !== 200) {
      throw new Error(res.msg || '批量提交产品默认策略草稿失败');
    }
    readyDrafts.forEach((draft) => clearProductDefaultDraftSubmitState(draft));
    const successKeys = (res.data.items || [])
      .filter((item) => item.success === true)
      .map((item) => readyDrafts[item.index])
      .filter((draft): draft is ProductDefaultDraft => Boolean(draft))
      .map((draft) => getMissingPolicySummaryKey(draft));
    (res.data.items || [])
      .filter((item) => item.success !== true)
      .forEach((item) => {
        const draft = readyDrafts[item.index];
        if (!draft) {
          return;
        }
        draft.submitStatus = 'FAILED';
        draft.submitMessage = item.message || '提交失败，请检查后重试';
      });
    if (successKeys.length > 0) {
      const successKeySet = new Set(successKeys);
      productDefaultDrafts.value = productDefaultDrafts.value.filter((draft) => !successKeySet.has(getMissingPolicySummaryKey(draft)));
      ElMessage.success(`已提交 ${successKeys.length} 项产品默认策略`);
      void loadRuleList();
    }
    const failedCount = Number(res.data.failedCount || 0);
    if (failedCount > 0) {
      ElMessage.warning(`${failedCount} 项产品默认策略提交失败，已保留在草稿中`);
    }
  } catch (error) {
    console.error('批量提交产品默认策略草稿失败', error);
    ElMessage.error(error instanceof Error ? error.message : '批量提交产品默认策略草稿失败');
  } finally {
    batchDraftSubmitLoading.value = false;
  }
};

const handleProductDefaultAdd = (
  source?: RiskGovernanceGapItem | RiskGovernanceMissingPolicyProductMetricSummary
) => {
  resetRuleForm();
  const firstMissingItem = source || missingPolicySummaryItems.value[0] || missingPolicyItems.value[0];
  activeProductDefaultDraftKey.value = source && productDefaultDrafts.value.some((item) => getMissingPolicySummaryKey(item) === getMissingPolicySummaryKey(source))
    ? getMissingPolicySummaryKey(source)
    : '';
  form.ruleScope = 'PRODUCT';
  form.productId = firstMissingItem?.productId == null ? undefined : firstMissingItem.productId;
  form.riskMetricId = normalizeOptionalId(firstMissingItem?.riskMetricId);
  form.metricIdentifier = firstMissingItem?.metricIdentifier || '';
  form.metricName = firstMissingItem?.metricName || '';
  form.ruleName = 'ruleName' in (firstMissingItem || {}) && firstMissingItem?.ruleName
    ? String(firstMissingItem.ruleName)
    : firstMissingItem?.metricName
      ? `${firstMissingItem.metricName} 产品默认阈值`
      : '产品默认阈值策略';
  if ('expression' in (firstMissingItem || {}) && firstMissingItem?.expression) {
    form.expression = String(firstMissingItem.expression);
  } else {
    form.expression = resolveRecommendedExpression(firstMissingItem as RiskGovernanceMissingPolicyProductMetricSummary);
  }
  if ('duration' in (firstMissingItem || {}) && firstMissingItem?.duration != null) {
    form.duration = Number(firstMissingItem.duration);
  }
  if ('alarmLevel' in (firstMissingItem || {}) && firstMissingItem?.alarmLevel) {
    form.alarmLevel = String(firstMissingItem.alarmLevel);
  }
  if ('convertToEvent' in (firstMissingItem || {}) && firstMissingItem?.convertToEvent != null) {
    form.convertToEvent = Number(firstMissingItem.convertToEvent);
  }
  formVisible.value = true;
};

const handleProductDefaultFilter = () => {
  filters.scopeView = 'BUSINESS';
  filters.ruleScope = 'PRODUCT';
  filters.productType = '';
  syncAppliedFilters();
  resetPage();
  clearSelection();
  void loadRuleList();
};

function applyGovernanceCreateContext() {
  if (governanceCreateHandled) {
    return;
  }
  const context = parseGovernanceCreateContext();
  if (!context) {
    return;
  }
  governanceCreateHandled = true;
  handleAdd();
  form.riskMetricId = context.riskMetricId;
  form.metricIdentifier = context.metricIdentifier;
  form.metricName = context.metricName;
}

const handleEdit = (row: RuleDefinition) => {
  form.id = row.id;
  form.riskMetricId = normalizeOptionalId(row.riskMetricId);
  form.ruleScope = getRuleScopeOption(row.ruleScope).value;
  form.productType = row.productType || 'MONITORING';
  form.productId = row.productId == null ? undefined : row.productId;
  form.deviceId = row.deviceId == null ? '' : String(row.deviceId);
  form.riskPointDeviceId = row.riskPointDeviceId == null ? '' : String(row.riskPointDeviceId);
  form.ruleName = row.ruleName;
  form.metricIdentifier = row.metricIdentifier;
  form.metricName = row.metricName;
  form.expression = row.expression;
  form.duration = row.duration;
  form.alarmLevel = normalizeAlarmLevel(row.alarmLevel) || alarmLevelOptions.value[0]?.value || 'blue';
  form.notificationMethods = row.notificationMethods ? row.notificationMethods.split(',') : [];
  form.convertToEvent = row.convertToEvent;
  form.status = row.status;
  form.remark = row.remark || '';
  formVisible.value = true;
};

const handleDelete = async (row: RuleDefinition) => {
  try {
    await confirmDelete('规则', row.ruleName);
    const res = await deleteRule(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      void loadRuleList();
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return;
    }
    console.error('删除规则失败', error);
  }
};

function buildRuleSubmitData() {
  const formData = {
    ...form,
    notificationMethods: form.notificationMethods.length > 0 ? form.notificationMethods.join(',') : undefined
  };
  if (formData.ruleScope !== 'PRODUCT') {
    formData.productId = undefined;
  }
  if (formData.ruleScope !== 'PRODUCT_TYPE') {
    formData.productType = undefined;
  } else {
    formData.productType = String(form.productType || '').toUpperCase();
  }
  if (formData.ruleScope !== 'DEVICE') {
    formData.deviceId = undefined as unknown as string;
  } else {
    formData.deviceId = normalizeOptionalId(form.deviceId) as unknown as string;
  }
  if (formData.ruleScope !== 'BINDING') {
    formData.riskPointDeviceId = undefined as unknown as string;
  } else {
    formData.riskPointDeviceId = normalizeOptionalId(form.riskPointDeviceId) as unknown as string;
  }
  return formData;
}

const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    submitLoading.value = true;
    const formData = buildRuleSubmitData();
    const submittedDraftKey = activeProductDefaultDraftKey.value;
    const res = form.id ? await updateRule(formData) : await addRule(formData);
    if (res.code === 200) {
      ElMessage.success(form.id ? '更新成功' : '新增成功');
      formVisible.value = false;
      if (submittedDraftKey) {
        productDefaultDrafts.value = productDefaultDrafts.value.filter((draft) => getMissingPolicySummaryKey(draft) !== submittedDraftKey);
        activeProductDefaultDraftKey.value = '';
      }
      void loadRuleList();
    }
  } catch (error) {
    console.error('提交表单失败', error);
  } finally {
    submitLoading.value = false;
  }
};

const handleFormClose = () => {
  formRef.value?.clearValidate?.();
  activeProductDefaultDraftKey.value = '';
  resetRuleForm();
};

onMounted(() => {
  applyRouteQueryToFilters();
  syncAppliedFilters();
  applyGovernanceCreateContext();
  void loadAlarmLevelOptionList();
  void loadProductOptions();
  void loadRuleList();
});
</script>

<style scoped>
.rule-definition-view {
  min-width: 0;
}

.rule-definition-notice-stack,
.rule-definition-governance-list {
  display: grid;
  gap: 0.75rem;
}

.rule-definition-governance-list {
  margin: 0;
  padding-left: 1rem;
}

.rule-definition-governance-list li {
  display: grid;
  gap: 0.15rem;
  color: var(--text-secondary);
}

.rule-definition-governance-list strong {
  color: var(--text-primary);
}

.rule-definition-table-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.rule-definition-governance-summary {
  display: grid;
  gap: 0.65rem;
}

.rule-definition-governance-summary__header,
.rule-definition-governance-summary-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.rule-definition-governance-summary__header > div:first-child,
.rule-definition-default-drafts__header > div,
.rule-definition-governance-summary-list__main,
.rule-definition-default-draft-list li > div:first-child {
  display: grid;
  gap: 0.15rem;
  min-width: 0;
}

.rule-definition-governance-summary__actions,
.rule-definition-default-drafts__actions,
.rule-definition-default-draft-list__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.4rem;
}

.rule-definition-governance-summary__header span,
.rule-definition-governance-summary-list span,
.rule-definition-default-drafts span,
.rule-definition-default-draft-list span {
  color: var(--text-secondary);
}

.rule-definition-governance-summary-list {
  display: grid;
  gap: 0.5rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.rule-definition-governance-summary-list li {
  padding: 0.55rem 0.7rem;
  border: 1px solid var(--border-color-lighter);
  border-radius: 0.375rem;
  background: var(--surface-bg);
}

.rule-definition-governance-summary-list__main {
  flex: 1;
}

.rule-definition-threshold-recommendation {
  width: fit-content;
  max-width: 100%;
  padding: 0.12rem 0.45rem;
  border: 1px solid var(--el-color-success-light-5);
  border-radius: 0.25rem;
  color: var(--el-color-success);
  background: var(--el-color-success-light-9);
  overflow-wrap: anywhere;
}

.rule-definition-threshold-recommendation.is-muted {
  border-color: var(--border-color-lighter);
  color: var(--text-secondary);
  background: var(--fill-color-light);
}

.rule-definition-governance-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.rule-definition-default-drafts {
  display: grid;
  gap: 0.55rem;
  padding: 0.65rem 0.75rem;
  border: 1px dashed var(--border-color);
  border-radius: 0.375rem;
  background: var(--surface-bg);
}

.rule-definition-default-drafts__header,
.rule-definition-default-draft-list li {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.rule-definition-default-draft-list {
  display: grid;
  gap: 0.45rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.rule-definition-default-draft-list li {
  padding-top: 0.45rem;
  border-top: 1px solid var(--border-color-lighter);
}

.rule-definition-default-draft-error {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem 0.6rem;
  padding: 0.4rem 0.55rem;
  border: 1px solid var(--el-color-danger-light-7);
  border-radius: 0.375rem;
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
}

.rule-definition-default-draft-error span {
  color: var(--el-color-danger);
}

.rule-definition-default-draft-template {
  display: grid;
  grid-template-columns: minmax(12rem, 1.5fr) minmax(7rem, 0.8fr) minmax(8rem, 0.8fr) minmax(9rem, 0.9fr) minmax(11rem, 1fr) auto;
  gap: 0.5rem;
  align-items: center;
  padding: 0.55rem;
  border: 1px solid var(--border-color-lighter);
  border-radius: 0.375rem;
  background: var(--fill-color-light);
}

.rule-definition-default-draft-list__main {
  display: grid;
  flex: 1;
  gap: 0.5rem;
  min-width: 0;
}

.rule-definition-default-draft-editor {
  display: grid;
  grid-template-columns: minmax(10rem, 1.2fr) minmax(12rem, 1.4fr) minmax(7rem, 0.8fr) minmax(8rem, 0.8fr) minmax(9rem, 0.8fr);
  gap: 0.5rem;
  align-items: center;
}

.rule-definition-preview-context {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem 0.75rem;
  color: var(--text-secondary);
}

.rule-definition-preview-list {
  display: grid;
  gap: 0.55rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.rule-definition-preview-list li {
  display: grid;
  gap: 0.45rem;
  padding: 0.65rem 0.75rem;
  border: 1px solid var(--border-color-lighter);
  border-radius: 0.375rem;
  background: var(--surface-bg);
}

.rule-definition-preview-list li.is-selected {
  border-color: var(--el-color-success-light-5);
  background: var(--el-color-success-light-9);
}

.rule-definition-preview-list li > div {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.rule-definition-preview-list span,
.rule-definition-preview-list p {
  margin: 0;
  color: var(--text-secondary);
}

@media (max-width: 980px) {
  .rule-definition-governance-summary__header,
  .rule-definition-default-drafts__header,
  .rule-definition-default-draft-list li {
    align-items: stretch;
    flex-direction: column;
  }

  .rule-definition-default-draft-template,
  .rule-definition-default-draft-editor {
    grid-template-columns: 1fr;
  }
}
</style>
