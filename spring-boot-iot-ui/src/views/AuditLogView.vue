<template>
  <StandardPageShell class="page-stack audit-log-view" :show-title="false">
    <StandardWorkbenchPanel
      :title="panelTitle"
      :description="pageDescription"
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-toolbar
      :show-inline-state="showSystemInlineState"
      show-pagination
    >
      <template #filters>
        <StandardListFilterHeader
          :model="searchForm"
          :show-advanced="showAdvancedFilters"
          show-advanced-toggle
          :advanced-hint="advancedFilterHint"
          @toggle-advanced="toggleAdvancedFilters"
        >
          <template #primary>
            <el-form-item>
              <el-input
                id="quick-search"
                v-model="quickSearchKeyword"
                :placeholder="quickSearchPlaceholder"
                clearable
                prefix-icon="Search"
                @keyup.enter="handleQuickSearch"
                @clear="handleClearQuickSearch"
              />
            </el-form-item>
            <el-form-item v-if="isBusinessMode">
              <el-select v-model="searchForm.operationType" placeholder="操作类型" clearable>
                <el-option
                  v-for="item in businessOperationTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="searchForm.operationModule"
                :placeholder="isSystemMode ? '异常模块' : '操作模块'"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.operationResult" placeholder="操作结果" clearable>
                <el-option label="成功" :value="1" />
                <el-option label="失败" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="isSystemMode">
              <el-select v-model="searchForm.requestMethod" :placeholder="isSystemMode ? '请求通道' : '请求方法'" clearable>
                <el-option
                  v-for="item in systemRequestMethodOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </template>
          <template #advanced>
            <el-form-item v-if="isBusinessMode">
              <el-input
                v-model="searchForm.traceId"
                placeholder="TraceId"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item v-if="isSystemMode">
              <el-input
                v-model="searchForm.requestUrl"
                placeholder="目标 / URL"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <template v-if="isSystemMode">
              <el-form-item>
                <el-input
                  v-model="searchForm.deviceCode"
                  placeholder="设备编码"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="searchForm.productKey"
                  placeholder="产品标识"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="searchForm.errorCode"
                  placeholder="异常编码"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="searchForm.exceptionClass"
                  placeholder="异常类型"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
            </template>
          </template>
          <template #actions>
            <StandardButton action="query" @click="handleSearch">查询</StandardButton>
            <StandardButton action="reset" @click="handleReset">重置</StandardButton>
          </template>
        </StandardListFilterHeader>
        <div v-if="appliedQuickSearchValue" class="audit-log-quick-search-tag">
          <el-tag closable class="audit-log-quick-search-tag__chip" @close="handleClearQuickSearch">
            快速搜索：{{ appliedQuickSearchValue }}
          </el-tag>
        </div>
      </template>

      <template #applied-filters>
        <StandardAppliedFiltersBar
          :tags="activeFilterTags"
          @remove="handleRemoveAppliedFilter"
          @clear="handleClearAppliedFilters"
        />
      </template>

      <template v-if="showSystemInlineState" #inline-state>
        <StandardInlineState :message="systemInlineMessage" tone="info" />
      </template>

      <section
        v-if="isSystemMode"
        v-loading="slowSummaryLoading"
        class="audit-log-slow-summary standard-list-surface"
        aria-label="性能慢点热点"
        element-loading-text="正在刷新慢点热点"
        element-loading-background="var(--loading-mask-bg)"
      >
        <header class="audit-log-slow-summary__header">
          <div>
            <h3>性能慢点 Top</h3>
          </div>
          <span>{{ slowSummaryRows.length }} 项</span>
        </header>
        <div v-if="slowSummaryErrorMessage" class="audit-log-slow-summary__empty">
          {{ slowSummaryErrorMessage }}
        </div>
        <div v-else-if="slowSummaryRows.length === 0" class="audit-log-slow-summary__empty">
          暂无慢点热点
        </div>
        <div v-else class="audit-log-slow-summary__grid">
          <article
            v-for="row in slowSummaryRows"
            :key="`${row.spanType || 'span'}-${row.domainCode || 'domain'}-${row.eventCode || 'event'}-${row.objectType || 'object'}-${row.objectId || 'id'}`"
            class="audit-log-slow-summary__item"
          >
            <div class="audit-log-slow-summary__title">
              <strong>{{ formatSlowSummaryTitle(row) }}</strong>
              <span>{{ formatValue(row.latestStartedAt) }}</span>
            </div>
            <p>{{ formatSlowSummaryTarget(row) }}</p>
            <div class="audit-log-slow-summary__metrics">
              <span>最大 {{ formatDuration(row.maxDurationMs) }}</span>
              <span>平均 {{ formatDuration(row.avgDurationMs) }}</span>
              <span>{{ formatCount(row.totalCount) }} 次</span>
            </div>
            <div class="audit-log-slow-summary__footer">
              <span>{{ formatValue(row.latestTraceId) }}</span>
              <StandardButton
                action="view"
                link
                :disabled="!row.latestTraceId"
                @click="openTraceEvidenceByTraceId(row.latestTraceId)"
              >
                证据
              </StandardButton>
              <StandardButton
                action="view"
                link
                @click="loadSlowSpanDrilldown(row)"
              >
                明细
              </StandardButton>
              <StandardButton
                action="view"
                link
                @click="loadSlowTrendDrilldown(row, defaultSlowTrendWindow)"
              >
                趋势
              </StandardButton>
            </div>
          </article>
        </div>
        <section
          v-if="activeSlowSummary"
          v-loading="slowSpanLoading"
          class="audit-log-slow-span-drilldown"
          aria-label="慢点调用片段明细"
          element-loading-text="正在刷新慢点明细"
          element-loading-background="var(--loading-mask-bg)"
        >
          <header class="audit-log-slow-span-drilldown__header">
            <div>
              <h3>慢点明细</h3>
              <p>{{ formatSlowSummaryTitle(activeSlowSummary) }} · {{ formatSlowSummaryTarget(activeSlowSummary) }}</p>
            </div>
            <span>{{ slowSpanTotal }} 条</span>
          </header>
          <div v-if="slowSpanErrorMessage" class="audit-log-slow-summary__empty">
            {{ slowSpanErrorMessage }}
          </div>
          <div v-else-if="slowSpanRows.length === 0" class="audit-log-slow-summary__empty">
            暂无慢点明细
          </div>
          <div v-else class="audit-log-slow-span-drilldown__list">
            <article
              v-for="span in slowSpanRows"
              :key="`slow-span-${span.id || span.traceId || span.startedAt}`"
              class="audit-log-slow-span-drilldown__item"
            >
              <div class="audit-log-slow-span-drilldown__title">
                <strong>{{ formatValue(span.spanName || span.spanType) }}</strong>
                <span>{{ formatDuration(span.durationMs) }}</span>
              </div>
              <div class="audit-log-slow-span-drilldown__meta">
                <span>{{ formatValue(span.traceId) }}</span>
                <span>{{ formatValue(span.status) }}</span>
                <span>{{ formatValue(span.startedAt) }}</span>
              </div>
              <div class="audit-log-slow-span-drilldown__footer">
                <span>{{ formatValue(span.eventCode) }} / {{ formatValue(span.objectId) }}</span>
                <StandardButton
                  action="view"
                  link
                  :disabled="!span.traceId"
                  @click="openTraceEvidenceByTraceId(span.traceId)"
                >
                  证据
                </StandardButton>
              </div>
            </article>
          </div>
        </section>
        <section
          v-if="activeSlowTrendSummary"
          v-loading="slowTrendLoading"
          class="audit-log-slow-trend-drilldown"
          aria-label="慢点趋势"
          element-loading-text="正在刷新慢点趋势"
          element-loading-background="var(--loading-mask-bg)"
        >
          <header class="audit-log-slow-trend-drilldown__header">
            <div>
              <h3>慢点趋势</h3>
              <p>{{ formatSlowSummaryTitle(activeSlowTrendSummary) }} · {{ formatSlowSummaryTarget(activeSlowTrendSummary) }}</p>
            </div>
            <div class="audit-log-slow-trend-drilldown__actions">
              <span>{{ slowTrendRows.length }} 桶</span>
              <StandardChoiceGroup
                :model-value="slowTrendWindow"
                :options="slowTrendWindowOptions"
                @update:model-value="handleSlowTrendWindowChange"
              />
            </div>
          </header>
          <div v-if="slowTrendErrorMessage" class="audit-log-slow-summary__empty">
            {{ slowTrendErrorMessage }}
          </div>
          <div v-else-if="slowTrendRows.length === 0" class="audit-log-slow-summary__empty">
            暂无慢点趋势
          </div>
          <div v-else class="audit-log-slow-trend-drilldown__list">
            <article
              v-for="item in slowTrendRows"
              :key="`slow-trend-${item.bucket || 'bucket'}-${item.bucketStart || item.bucketEnd}`"
              class="audit-log-slow-trend-drilldown__item"
            >
              <div class="audit-log-slow-trend-drilldown__title">
                <strong>{{ formatSlowTrendBucketLabel(item) }}</strong>
                <span>{{ formatCount(item.totalCount) }} 次</span>
              </div>
              <div class="audit-log-slow-trend-drilldown__metrics">
                <span>P95 {{ formatDuration(item.p95DurationMs) }}</span>
                <span>P99 {{ formatDuration(item.p99DurationMs) }}</span>
                <span>平均 {{ formatDuration(item.avgDurationMs) }}</span>
                <span>最大 {{ formatDuration(item.maxDurationMs) }}</span>
                <span>错误率 {{ formatPercentage(item.errorRate) }}</span>
              </div>
              <div class="audit-log-slow-trend-drilldown__footer">
                <span>成功 {{ formatCount(item.successCount) }} / 异常 {{ formatCount(item.errorCount) }}</span>
              </div>
            </article>
          </div>
        </section>
      </section>

      <section
        v-if="isSystemMode"
        v-loading="scheduledTaskLoading"
        class="audit-log-scheduled-task-ledger standard-list-surface"
        aria-label="调度任务台账"
        element-loading-text="正在刷新调度任务台账"
        element-loading-background="var(--loading-mask-bg)"
      >
        <header class="audit-log-scheduled-task-ledger__header">
          <div>
            <h3>调度任务台账</h3>
          </div>
          <span>{{ scheduledTaskRows.length }} / {{ scheduledTaskTotal }}</span>
        </header>
        <div v-if="scheduledTaskErrorMessage" class="audit-log-slow-summary__empty">
          {{ scheduledTaskErrorMessage }}
        </div>
        <div v-else-if="scheduledTaskRows.length === 0" class="audit-log-slow-summary__empty">
          暂无调度任务记录
        </div>
        <div v-else class="audit-log-scheduled-task-ledger__list">
          <article
            v-for="row in scheduledTaskRows"
            :key="`scheduled-task-${row.id || row.traceId || row.taskCode}`"
            class="audit-log-scheduled-task-ledger__item"
          >
            <div class="audit-log-scheduled-task-ledger__title">
              <strong>{{ formatScheduledTaskName(row) }}</strong>
              <span>{{ formatDuration(row.durationMs) }}</span>
            </div>
            <div class="audit-log-scheduled-task-ledger__meta">
              <span>{{ formatValue(row.triggerType) }}</span>
              <span>{{ formatScheduledTaskTrigger(row) }}</span>
              <span>{{ formatValue(row.status) }}</span>
              <span>{{ formatValue(row.startedAt) }}</span>
            </div>
            <div class="audit-log-scheduled-task-ledger__footer">
              <span>{{ formatValue(row.traceId) }}</span>
              <span v-if="row.errorMessage">{{ formatValue(row.errorMessage) }}</span>
              <StandardButton
                action="view"
                link
                :disabled="!row.traceId"
                @click="openTraceEvidenceByTraceId(row.traceId)"
              >
                证据
              </StandardButton>
            </div>
          </article>
        </div>
      </section>

      <section
        v-if="isSystemMode"
        v-loading="messageArchiveBatchLoading"
        class="audit-log-archive-batch-ledger standard-list-surface"
        aria-label="归档批次台账"
        element-loading-text="正在刷新归档批次台账"
        element-loading-background="var(--loading-mask-bg)"
      >
        <header class="audit-log-archive-batch-ledger__header">
          <div class="audit-log-archive-batch-ledger__header-main">
            <div>
              <h3>归档批次台账</h3>
            </div>
            <div class="audit-log-archive-batch-ledger__filters">
              <label class="audit-log-archive-batch-ledger__filter-field">
                <span>批次号</span>
                <input
                  v-model.trim="messageArchiveBatchFilters.batchNo"
                  data-testid="archive-batch-filter-batch-no"
                  type="text"
                  placeholder="按批次号筛选"
                  @keyup.enter="handleMessageArchiveBatchSearch"
                >
              </label>
              <label class="audit-log-archive-batch-ledger__filter-field">
                <span>状态</span>
                <select
                  v-model="messageArchiveBatchFilters.status"
                  data-testid="archive-batch-filter-status"
                >
                  <option value="">全部状态</option>
                  <option
                    v-for="option in messageArchiveBatchStatusOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </option>
                </select>
              </label>
              <label class="audit-log-archive-batch-ledger__filter-field">
                <span>对比结论</span>
                <select
                  v-model="messageArchiveBatchFilters.compareStatus"
                  data-testid="archive-batch-filter-compare-status"
                >
                  <option value="">全部结论</option>
                  <option
                    v-for="option in messageArchiveBatchCompareStatusOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </option>
                </select>
              </label>
              <label class="audit-log-archive-batch-ledger__filter-field">
                <span>开始日期</span>
                <input
                  v-model="messageArchiveBatchFilters.dateFrom"
                  data-testid="archive-batch-filter-date-from"
                  type="date"
                >
              </label>
              <label class="audit-log-archive-batch-ledger__filter-field">
                <span>结束日期</span>
                <input
                  v-model="messageArchiveBatchFilters.dateTo"
                  data-testid="archive-batch-filter-date-to"
                  type="date"
                >
              </label>
              <div class="audit-log-archive-batch-ledger__filter-field audit-log-archive-batch-ledger__filter-field--checkbox">
                <span>异常筛选</span>
                <label class="audit-log-archive-batch-ledger__checkbox">
                  <input
                    v-model="messageArchiveBatchFilters.onlyAbnormal"
                    data-testid="archive-batch-filter-only-abnormal"
                    type="checkbox"
                  >
                  <span>仅看异常</span>
                </label>
              </div>
              <div class="audit-log-archive-batch-ledger__filter-actions">
                <StandardButton
                  data-testid="archive-batch-search-button"
                  @click="handleMessageArchiveBatchSearch"
                >
                  筛选
                </StandardButton>
                <StandardButton
                  data-testid="archive-batch-reset-button"
                  @click="resetMessageArchiveBatchFilters"
                >
                  重置
                </StandardButton>
              </div>
            </div>
          </div>
          <span>{{ messageArchiveBatchRows.length }} / {{ messageArchiveBatchTotal }}</span>
        </header>
        <div class="audit-log-archive-batch-ledger__overview">
          <article
            v-for="item in messageArchiveBatchOverviewCards"
            :key="item.key"
            :class="[
              'audit-log-archive-batch-ledger__overview-card',
              {
                'is-clickable': item.clickable,
                'is-active': item.active
              }
            ]"
            :data-testid="item.testId"
            role="button"
            tabindex="0"
            @click="handleMessageArchiveBatchOverviewClick(item.key)"
            @keydown.enter.prevent="handleMessageArchiveBatchOverviewClick(item.key)"
            @keydown.space.prevent="handleMessageArchiveBatchOverviewClick(item.key)"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.meta }}</p>
          </article>
        </div>
        <div v-if="messageArchiveBatchFocusHint" class="audit-log-archive-batch-ledger__focus-hint">
          {{ messageArchiveBatchFocusHint }}
        </div>
        <div v-if="messageArchiveBatchOverviewLoading" class="audit-log-slow-summary__empty">
          正在汇总异常摘要
        </div>
        <div v-else-if="messageArchiveBatchOverviewErrorMessage" class="audit-log-slow-summary__empty">
          {{ messageArchiveBatchOverviewErrorMessage }}
        </div>
        <div v-if="messageArchiveBatchErrorMessage" class="audit-log-slow-summary__empty">
          {{ messageArchiveBatchErrorMessage }}
        </div>
        <div v-else-if="messageArchiveBatchRows.length === 0" class="audit-log-slow-summary__empty">
          暂无归档批次记录
        </div>
        <div v-else class="audit-log-archive-batch-ledger__list">
          <article
            v-for="row in messageArchiveBatchRows"
            :key="`message-archive-batch-${row.id || row.batchNo || row.createTime}`"
            :class="[
              'audit-log-archive-batch-ledger__item',
              resolveArchiveBatchCompareStatusClass(row),
              { 'is-abnormal': isArchiveBatchAbnormalStatus(row.compareStatus) }
            ]"
          >
            <div class="audit-log-archive-batch-ledger__title">
              <strong>{{ formatArchiveBatchName(row) }}</strong>
              <span>{{ formatValue(row.createTime || row.updateTime) }}</span>
            </div>
            <div class="audit-log-archive-batch-ledger__meta">
              <span>{{ formatValue(row.status) }}</span>
              <span>{{ formatValue(row.compareStatusLabel || formatArchiveBatchCompareStatus(row.compareStatus)) }}</span>
              <span>{{ formatValue(row.sourceTable) }}</span>
              <span>{{ formatRetentionDays(row.retentionDays) }}</span>
              <span>截止 {{ formatValue(row.cutoffAt) }}</span>
            </div>
            <div class="audit-log-archive-batch-ledger__metrics">
              <span>确认 {{ formatCount(row.confirmedExpiredRows) }}</span>
              <span>候选 {{ formatCount(row.candidateRows) }}</span>
              <span>归档 {{ formatCount(row.archivedRows) }}</span>
              <span>删除 {{ formatCount(row.deletedRows) }}</span>
            </div>
            <div class="audit-log-archive-batch-ledger__insights">
              <span>确认差值 {{ formatSignedCount(row.deltaConfirmedVsDeleted) }}</span>
              <span>dry-run 差值 {{ formatSignedCount(row.deltaDryRunVsDeleted) }}</span>
              <span>剩余过期 {{ formatOptionalCount(row.remainingExpiredRows) }}</span>
              <span>报告 {{ formatArchiveBatchPreviewAvailability(row) }}</span>
            </div>
            <div class="audit-log-archive-batch-ledger__footer">
              <span>{{ formatArchiveBatchFooter(row) }}</span>
              <StandardButton
                action="view"
                link
                @click="openMessageArchiveBatchDetail(row)"
              >
                详情
              </StandardButton>
            </div>
          </article>
        </div>
      </section>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[
            `已选 ${selectedRows.length} 项`,
            isSystemMode ? `异常 ${systemStats.total}` : `审计 ${businessStats.total}`,
            isSystemMode
              ? `今日 ${systemStats.todayCount}`
              : `成功 ${businessStats.successCount}`,
            isSystemMode
              ? `链路 ${systemStats.distinctTraceCount}`
              : `失败 ${businessStats.failureCount}`
          ]"
        >
          <template #right>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
            <StandardActionMenu
              label="更多操作"
              :items="auditToolbarActions"
              @command="handleToolbarAction"
            />
          </template>
        </StandardTableToolbar>
      </template>

      <div
        v-loading="loading"
        class="audit-log-table-wrap standard-list-surface"
        element-loading-text="正在刷新审计列表"
        element-loading-background="var(--loading-mask-bg)"
      >
        <div v-if="tableData.length > 0" class="audit-log-mobile-list standard-mobile-record-list">
          <div class="audit-log-mobile-list__grid standard-mobile-record-grid">
            <article
              v-for="row in tableData"
              :key="row.id || row.traceId || row.operationTime || row.operationModule"
              class="audit-log-mobile-card standard-mobile-record-card"
            >
              <div class="audit-log-mobile-card__header">
                <div class="audit-log-mobile-card__heading">
                  <strong class="audit-log-mobile-card__title">
                    {{ isSystemMode ? formatValue(row.traceId || row.operationModule) : formatValue(row.operationModule || row.userName) }}
                  </strong>
                  <span class="audit-log-mobile-card__sub">
                    {{ isSystemMode ? formatValue(row.deviceCode) : formatValue(row.userName) }}
                  </span>
                </div>
                <span class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item">
                  {{ getOperationResultName(row.operationResult) }}
                </span>
              </div>

              <div class="audit-log-mobile-card__meta">
                <span
                  v-if="isBusinessMode"
                  class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item"
                >
                  {{ getOperationTypeName(row.operationType || '') }}
                </span>
                <span class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item">
                  {{ formatValue(row.requestMethod) }}
                </span>
                <span
                  v-if="isSystemMode"
                  class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item"
                >
                  {{ formatValue(row.errorCode) }}
                </span>
                <span
                  v-else
                  class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item"
                >
                  {{ formatValue(row.ipAddress) }}
                </span>
              </div>

              <div class="audit-log-mobile-card__info">
                <div class="audit-log-mobile-card__field">
                  <span class="standard-mobile-record-card__field-label">
                    {{ isSystemMode ? '异常模块' : '操作模块' }}
                  </span>
                  <strong class="standard-mobile-record-card__field-value">
                    {{ formatValue(row.operationModule) }}
                  </strong>
                </div>
                <div class="audit-log-mobile-card__field">
                  <span class="standard-mobile-record-card__field-label">
                    {{ isSystemMode ? '设备编码' : '操作方法' }}
                  </span>
                  <strong class="standard-mobile-record-card__field-value">
                    {{ isSystemMode ? formatValue(row.deviceCode) : formatValue(row.operationMethod) }}
                  </strong>
                </div>
                <div class="audit-log-mobile-card__field">
                  <span class="standard-mobile-record-card__field-label">
                    {{ isSystemMode ? '产品标识' : '操作时间' }}
                  </span>
                  <strong class="standard-mobile-record-card__field-value">
                    {{ isSystemMode ? formatValue(row.productKey) : formatValue(row.operationTime) }}
                  </strong>
                </div>
                <div class="audit-log-mobile-card__field">
                  <span class="standard-mobile-record-card__field-label">
                    {{ isSystemMode ? '异常类型' : '操作结果' }}
                  </span>
                  <strong class="standard-mobile-record-card__field-value">
                    {{ isSystemMode ? formatValue(row.exceptionClass) : getOperationResultName(row.operationResult) }}
                  </strong>
                </div>
                <div class="audit-log-mobile-card__field audit-log-mobile-card__field--full">
                  <span class="standard-mobile-record-card__field-label">
                    {{ isSystemMode ? '异常摘要' : '请求目标' }}
                  </span>
                  <strong class="standard-mobile-record-card__field-value">
                    {{ isSystemMode ? formatValue(row.resultMessage) : formatValue(row.requestUrl) }}
                  </strong>
                </div>
              </div>

              <StandardWorkbenchRowActions
                variant="card"
                :direct-items="getAuditDirectActions(row)"
                @command="(command) => handleAuditRowAction(command, row)"
              />
            </article>
          </div>
        </div>

        <el-table
          ref="tableRef"
          class="audit-log-table"
          :data="tableData"
          border
          stripe
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column v-if="isBusinessMode" prop="operationType" label="操作类型" width="100">
            <template #default="{ row }">
              <el-tag :type="getOperationTypeTag(row.operationType)">
                {{ getOperationTypeName(row.operationType) }}
              </el-tag>
            </template>
          </el-table-column>
          <StandardTableTextColumn prop="operationModule" label="操作模块" :width="150" />
          <StandardTableTextColumn prop="operationMethod" label="操作方法" :min-width="180" />
          <StandardTableTextColumn prop="requestUrl" label="请求URL/目标" :min-width="220" />
          <el-table-column prop="requestMethod" label="请求方法/通道" width="120" />
          <StandardTableTextColumn v-if="isSystemMode" prop="traceId" label="TraceId" :min-width="180" />
          <StandardTableTextColumn v-if="isSystemMode" prop="deviceCode" label="设备编码" :min-width="140" />
          <StandardTableTextColumn v-if="isSystemMode" prop="productKey" label="产品标识" :min-width="140" />
          <StandardTableTextColumn v-if="isSystemMode" prop="errorCode" label="异常编码" :min-width="120" />
          <StandardTableTextColumn v-if="isSystemMode" prop="exceptionClass" label="异常类型" :min-width="180" />
          <StandardTableTextColumn v-if="isBusinessMode" prop="userName" label="操作用户" :width="120" />
          <StandardTableTextColumn v-if="isBusinessMode" prop="ipAddress" label="操作IP" :width="150" />
          <StandardTableTextColumn v-if="isSystemMode" prop="resultMessage" label="异常摘要" :min-width="220" />
          <StandardTableTextColumn prop="operationTime" label="操作时间" :width="180" />
          <el-table-column prop="operationResult" label="操作结果" width="100">
            <template #default="{ row }">
              <el-tag :type="getOperationResultTag(row.operationResult)" round>
                {{ getOperationResultName(row.operationResult) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            label="操作"
            :width="auditActionColumnWidth"
            fixed="right"
            class-name="standard-row-actions-column"
            :show-overflow-tooltip="false"
          >
            <template #default="{ row }">
              <StandardWorkbenchRowActions
                variant="table"
                :direct-items="getAuditDirectActions(row)"
                @command="(command) => handleAuditRowAction(command, row)"
              />
            </template>
          </el-table-column>
        </el-table>
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

    <AuditLogDetailDrawer
      v-model="detailVisible"
      :title="detailDialogTitle"
      :detail="detailData"
      :loading="detailLoading"
      :error-message="detailErrorMessage"
      :show-trace-action="isSystemMode && canJumpToMessageTrace(detailData)"
      :show-access-error-action="isSystemMode && canJumpToMessageTrace(detailData)"
      @jump-message-trace="handleJumpToMessageTrace(detailData)"
      @jump-access-error="handleJumpToAccessError(detailData)"
    />

    <StandardDetailDrawer
      v-model="evidenceDrawerVisible"
      title="TraceId 证据包"
      :subtitle="evidenceDrawerSubtitle"
      :loading="evidenceLoading"
      loading-text="正在加载证据链"
      :error-message="evidenceErrorMessage"
      :empty="evidenceTimeline.length === 0 && !evidenceLoading"
      empty-text="当前 TraceId 暂无业务事件或调用片段证据"
      size="56rem"
      tag-layout="title-inline"
      :tags="evidenceDrawerTags"
    >
      <div class="observability-evidence-drawer">
        <section class="observability-evidence-summary" aria-label="证据链摘要">
          <div
            v-for="item in evidenceSummaryCards"
            :key="item.label"
            class="observability-evidence-summary__item"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </section>

        <section class="observability-evidence-section">
          <header class="observability-evidence-section__header">
            <h3>合并时间线</h3>
          </header>
          <ol class="observability-evidence-timeline">
            <li
              v-for="item in evidenceTimeline"
              :key="`${item.itemType || 'item'}-${item.itemId || item.code || item.occurredAt}`"
              class="observability-evidence-timeline__item"
            >
              <span class="observability-evidence-timeline__type">
                {{ getEvidenceItemTypeName(item.itemType) }}
              </span>
              <div class="observability-evidence-timeline__body">
                <div class="observability-evidence-timeline__title">
                  <strong>{{ formatValue(item.code) }}</strong>
                  <span>{{ formatValue(item.status) }}</span>
                </div>
                <p>{{ formatValue(item.name) }}</p>
                <div class="observability-evidence-timeline__meta">
                  <span>{{ formatValue(item.occurredAt) }}</span>
                  <span v-if="item.durationMs !== undefined && item.durationMs !== null">
                    {{ formatDuration(item.durationMs) }}
                  </span>
                  <span v-if="item.objectType || item.objectId">
                    {{ formatValue(item.objectType) }} / {{ formatValue(item.objectId) }}
                  </span>
                </div>
              </div>
            </li>
          </ol>
        </section>

        <section class="observability-evidence-split">
          <article class="observability-evidence-section">
            <header class="observability-evidence-section__header">
              <h3>业务事件</h3>
            </header>
            <div v-if="evidenceBusinessEvents.length === 0" class="observability-evidence-empty">暂无业务事件</div>
            <div
              v-for="event in evidenceBusinessEvents"
              :key="`event-${event.id || event.eventCode}`"
              class="observability-evidence-record"
            >
              <strong>{{ formatValue(event.eventCode) }}</strong>
              <span>{{ formatValue(event.eventName) }}</span>
              <small>{{ formatValue(event.domainCode) }} · {{ formatValue(event.resultStatus) }}</small>
            </div>
          </article>

          <article class="observability-evidence-section">
            <header class="observability-evidence-section__header">
              <h3>调用片段</h3>
            </header>
            <div v-if="evidenceSpans.length === 0" class="observability-evidence-empty">暂无调用片段</div>
            <div
              v-for="span in evidenceSpans"
              :key="`span-${span.id || span.spanType}`"
              class="observability-evidence-record"
            >
              <strong>{{ formatValue(span.spanType) }}</strong>
              <span>{{ formatValue(span.spanName) }}</span>
              <small>{{ formatDuration(span.durationMs) }} · {{ formatValue(span.status) }}</small>
            </div>
          </article>
        </section>
      </div>
    </StandardDetailDrawer>

    <StandardDetailDrawer
      v-model="messageArchiveBatchDrawerVisible"
      title="归档批次详情"
      :subtitle="messageArchiveBatchDrawerSubtitle"
      :empty="!activeMessageArchiveBatch"
      empty-text="当前未选择归档批次"
      size="56rem"
      tag-layout="title-inline"
      :tags="messageArchiveBatchDrawerTags"
    >
      <div class="observability-archive-batch-drawer">
        <section class="observability-evidence-summary" aria-label="归档批次摘要">
          <div
            v-for="item in messageArchiveBatchSummaryCards"
            :key="item.label"
            class="observability-evidence-summary__item"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </section>

        <section class="observability-evidence-split">
          <article class="observability-evidence-section">
            <header class="observability-evidence-section__header">
              <h3>批次结果</h3>
            </header>
            <dl class="observability-archive-batch-kv">
              <div
                v-for="item in messageArchiveBatchResultItems"
                :key="item.label"
                class="observability-archive-batch-kv__item"
              >
                <dt>{{ item.label }}</dt>
                <dd>{{ item.value }}</dd>
              </div>
            </dl>
          </article>

          <article class="observability-evidence-section">
            <header class="observability-evidence-section__header">
              <h3>确认报告</h3>
            </header>
            <dl class="observability-archive-batch-kv">
              <div
                v-for="item in messageArchiveBatchReportItems"
                :key="item.label"
                class="observability-archive-batch-kv__item"
              >
                <dt>{{ item.label }}</dt>
                <dd>{{ item.value }}</dd>
              </div>
            </dl>
          </article>
        </section>

        <section class="observability-evidence-section">
          <header class="observability-evidence-section__header">
            <h3>附加产物</h3>
          </header>
          <div v-if="messageArchiveBatchArtifacts.length === 0" class="observability-evidence-empty">
            暂无附加产物
          </div>
          <dl v-else class="observability-archive-batch-kv">
            <div
              v-for="item in messageArchiveBatchArtifacts"
              :key="item.label"
              class="observability-archive-batch-kv__item"
            >
              <dt>{{ item.label }}</dt>
              <dd>{{ item.value }}</dd>
            </div>
          </dl>
        </section>

        <section class="observability-evidence-section">
          <header class="observability-evidence-section__header">
            <h3>批次对比</h3>
          </header>
          <div v-if="messageArchiveBatchCompareLoading" class="observability-evidence-empty">
            正在加载批次对比
          </div>
          <div v-else-if="messageArchiveBatchCompareErrorMessage" class="observability-evidence-empty">
            {{ messageArchiveBatchCompareErrorMessage }}
          </div>
          <div v-else-if="!activeMessageArchiveBatchCompare" class="observability-evidence-empty">
            暂无批次对比
          </div>
          <div v-else class="observability-archive-batch-compare">
            <article
              class="observability-archive-batch-compare__status"
              :class="messageArchiveBatchCompareStatusClass"
            >
              <div class="observability-archive-batch-compare__status-copy">
                <strong>{{ messageArchiveBatchCompareHeadline }}</strong>
                <p>{{ formatArchiveBatchCompareMessage(activeMessageArchiveBatchCompare) }}</p>
              </div>
              <span>{{ messageArchiveBatchCompareStatusName }}</span>
            </article>

            <dl class="observability-archive-batch-kv">
              <div
                v-for="item in messageArchiveBatchCompareSourceItems"
                :key="item.label"
                class="observability-archive-batch-kv__item"
              >
                <dt>{{ item.label }}</dt>
                <dd>{{ item.value }}</dd>
              </div>
            </dl>

            <div
              v-if="messageArchiveBatchCompareSummaryItems.length > 0"
              class="observability-archive-batch-preview__summary"
            >
              <article
                v-for="item in messageArchiveBatchCompareSummaryItems"
                :key="item.label"
                class="observability-archive-batch-preview__summary-item"
              >
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </article>
            </div>

            <div v-if="messageArchiveBatchCompareTableComparisons.length === 0" class="observability-evidence-empty">
              暂无分表对比
            </div>
            <div v-else class="observability-archive-batch-compare__tables">
              <article
                v-for="item in messageArchiveBatchCompareTableComparisons"
                :key="item.tableName || item.label"
                class="observability-archive-batch-compare__table"
                :class="{
                  'is-drifted': item.matched === false,
                  'is-partial': item.matched === null
                }"
              >
                <div class="observability-archive-batch-compare__table-title">
                  <strong>{{ formatValue(item.label || item.tableName) }}</strong>
                  <span>{{ formatArchiveBatchCompareRowStatus(item.matched) }}</span>
                </div>
                <div class="observability-archive-batch-compare__table-metrics">
                  <span>dry-run 过期 {{ formatOptionalCount(item.dryRunExpiredRows) }}</span>
                  <span>apply 归档 {{ formatOptionalCount(item.applyArchivedRows) }}</span>
                  <span>apply 删除 {{ formatOptionalCount(item.applyDeletedRows) }}</span>
                  <span>剩余 {{ formatOptionalCount(item.applyRemainingExpiredRows) }}</span>
                </div>
                <div class="observability-archive-batch-compare__table-meta">
                  <span>{{ formatValue(item.tableName) }}</span>
                  <span>差值 {{ formatOptionalCount(item.deltaDryRunVsDeleted) }}</span>
                  <span v-if="item.reason">{{ item.reason }}</span>
                </div>
              </article>
            </div>
          </div>
        </section>

        <section class="observability-evidence-section">
          <header class="observability-evidence-section__header">
            <h3>确认报告预览</h3>
          </header>
          <div v-if="messageArchiveBatchReportPreviewLoading" class="observability-evidence-empty">
            正在加载确认报告预览
          </div>
          <div v-else-if="messageArchiveBatchReportPreviewErrorMessage" class="observability-evidence-empty">
            {{ messageArchiveBatchReportPreviewErrorMessage }}
          </div>
          <div v-else-if="!activeMessageArchiveBatchReportPreview" class="observability-evidence-empty">
            暂无确认报告预览
          </div>
          <div
            v-else-if="activeMessageArchiveBatchReportPreview.available === false"
            class="observability-evidence-empty"
          >
            {{ formatArchiveBatchReportPreviewReason(activeMessageArchiveBatchReportPreview) }}
          </div>
          <div v-else class="observability-archive-batch-preview">
            <dl class="observability-archive-batch-kv">
              <div
                v-for="item in messageArchiveBatchReportPreviewMetaItems"
                :key="item.label"
                class="observability-archive-batch-kv__item"
              >
                <dt>{{ item.label }}</dt>
                <dd>{{ item.value }}</dd>
              </div>
            </dl>

            <div
              v-if="messageArchiveBatchReportPreviewSummaryItems.length > 0"
              class="observability-archive-batch-preview__summary"
            >
              <article
                v-for="item in messageArchiveBatchReportPreviewSummaryItems"
                :key="item.label"
                class="observability-archive-batch-preview__summary-item"
              >
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </article>
            </div>

            <div v-if="messageArchiveBatchReportPreviewTableSummaries.length === 0" class="observability-evidence-empty">
              暂无表级摘要
            </div>
            <div v-else class="observability-archive-batch-preview__tables">
              <article
                v-for="item in messageArchiveBatchReportPreviewTableSummaries"
                :key="item.tableName || item.label"
                class="observability-archive-batch-preview__table"
              >
                <div class="observability-archive-batch-preview__table-title">
                  <strong>{{ formatValue(item.label || item.tableName) }}</strong>
                  <span>{{ formatValue(item.tableName) }}</span>
                </div>
                <div class="observability-archive-batch-preview__table-metrics">
                  <span>过期 {{ formatCount(item.expiredRows) }}</span>
                  <span>删除 {{ formatCount(item.deletedRows) }}</span>
                  <span>剩余 {{ formatCount(item.remainingExpiredRows) }}</span>
                </div>
                <div class="observability-archive-batch-preview__table-meta">
                  <span>保留 {{ formatRetentionDays(item.retentionDays) }}</span>
                  <span>截止 {{ formatValue(item.cutoffAt) }}</span>
                  <span>窗口 {{ formatValue(item.earliestRecordAt) }} - {{ formatValue(item.latestRecordAt) }}</span>
                </div>
              </article>
            </div>

            <article class="observability-archive-batch-preview__markdown">
              <header class="observability-evidence-section__header">
                <h3>Markdown 摘要</h3>
                <small v-if="activeMessageArchiveBatchReportPreview.markdownTruncated">
                  仅展示前 80 行 / 6000 字符
                </small>
              </header>
              <div
                v-if="!activeMessageArchiveBatchReportPreview.markdownAvailable || !activeMessageArchiveBatchReportPreview.markdownPreview"
                class="observability-evidence-empty"
              >
                当前仅保留 JSON 摘要，未生成 Markdown 预览
              </div>
              <pre v-else class="observability-archive-batch-preview__markdown-body">{{ activeMessageArchiveBatchReportPreview.markdownPreview }}</pre>
            </article>
          </div>
        </section>
      </div>
    </StandardDetailDrawer>

    <CsvColumnSettingDialog
      v-model="exportColumnDialogVisible"
      :title="exportDialogTitle"
      :options="exportColumnOptions"
      :selected-keys="selectedExportColumnKeys"
      :preset-storage-key="exportColumnStorageKey"
      :presets="exportPresets"
      @confirm="handleExportColumnConfirm"
    />
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pageLogs, getAuditLogById, deleteAuditLog, getSystemErrorStats, getBusinessAuditStats, type AuditLogRecord } from '@/api/auditLog'
import {
  getObservabilityMessageArchiveBatchCompare,
  getObservabilityMessageArchiveBatchOverview,
  getObservabilityMessageArchiveBatchReportPreview,
  getTraceEvidence,
  listObservabilitySlowSpanSummaries,
  listObservabilitySlowSpanTrends,
  pageObservabilityMessageArchiveBatches,
  pageObservabilityScheduledTasks,
  pageObservabilitySpans,
  type ObservabilityMessageArchiveBatchCompare,
  type ObservabilityMessageArchiveBatchCompareTable,
  type ObservabilityMessageArchiveBatch,
  type ObservabilityMessageArchiveBatchOverview,
  type ObservabilityMessageArchiveBatchOverviewQuery,
  type ObservabilityMessageArchiveBatchPageQuery,
  type ObservabilityMessageArchiveBatchReportPreview,
  type ObservabilityMessageArchiveBatchReportTableSummary,
  type ObservabilityScheduledTask,
  type ObservabilityScheduledTaskPageQuery,
  type ObservabilitySpan,
  type ObservabilitySpanPageQuery,
  type ObservabilitySlowSpanSummary,
  type ObservabilitySlowSpanSummaryQuery,
  type ObservabilitySlowSpanTrend,
  type ObservabilitySlowSpanTrendQuery,
  type ObservabilityTraceEvidence
} from '@/api/observability'
import { isHandledRequestError } from '@/api/request'
import type { BusinessAuditStats, SystemErrorStats } from '@/types/api'
import AuditLogDetailDrawer from '@/components/AuditLogDetailDrawer.vue'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue'
import StandardActionMenu from '@/components/StandardActionMenu.vue'
import StandardChoiceGroup from '@/components/StandardChoiceGroup.vue'
import StandardInlineState from '@/components/StandardInlineState.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import StandardPageShell from '@/components/StandardPageShell.vue'
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue'
import { useListAppliedFilters } from '@/composables/useListAppliedFilters'
import { useServerPagination } from '@/composables/useServerPagination'
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv'
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns'
import { confirmAction, isConfirmCancelled } from '@/utils/confirm'
import {
  buildDiagnosticRouteQuery,
  describeDiagnosticSource,
  persistDiagnosticContext,
  resolveDiagnosticContext,
  type DiagnosticContext
} from '@/utils/iotAccessDiagnostics'
import { resolveWorkbenchActionColumnWidth } from '@/utils/adaptiveActionColumn'

type AuditLogViewMode = 'business' | 'system'
type SlowTrendWindowKey = 'LAST_24_HOURS' | 'LAST_7_DAYS'
type ArchiveBatchDetailItem = { label: string; value: string }
type ArchiveBatchCompareStatus = 'MATCHED' | 'DRIFTED' | 'PARTIAL' | 'UNAVAILABLE'
type ArchiveBatchOverviewSelectionKey = 'abnormal' | 'drifted' | 'remaining' | 'latest'
type ArchiveBatchOverviewCard = {
  key: ArchiveBatchOverviewSelectionKey
  label: string
  value: string
  meta: string
  testId: string
  clickable: boolean
  active: boolean
}

const route = useRoute()
const router = useRouter()
const viewMode = computed<AuditLogViewMode>(() => (route.path === '/system-log' ? 'system' : 'business'))
const isSystemMode = computed(() => viewMode.value === 'system')
const isBusinessMode = computed(() => viewMode.value === 'business')
const auditActionColumnWidth = computed(() =>
  resolveWorkbenchActionColumnWidth({
    directItems: isSystemMode.value
      ? [
          { command: 'detail', label: '详情' },
          { command: 'evidence', label: '证据' },
          { command: 'trace', label: '追踪' },
          { command: 'delete', label: '删除', permission: 'system:audit:delete' }
        ]
      : [
          { command: 'detail', label: '详情' },
          { command: 'delete', label: '删除', permission: 'system:audit:delete' }
        ]
  })
)
const pageTitle = computed(() => (isSystemMode.value ? '异常观测台' : '审计中心'))
const panelTitle = computed(() => pageTitle.value)
const pageDescription = computed(() =>
  isSystemMode.value
    ? '后台异常核对：按异常模块、TraceId、设备编码与请求通道筛查 system_error，并判断下一步回链路追踪还是治理修正。'
    : '按用户、模块与结果查看审计留痕。'
)
const detailDialogTitle = computed(() => (isSystemMode.value ? '异常详情' : `${pageTitle.value}详情`))
const exportDialogTitle = computed(() => (isSystemMode.value ? '异常观测台导出列设置' : `${pageTitle.value}导出列设置`))
const recordLabel = computed(() => (isSystemMode.value ? '异常记录' : '审计记录'))
const businessOperationTypeOptions = [
  { label: '新增', value: 'insert' },
  { label: '修改', value: 'update' },
  { label: '删除', value: 'delete' },
  { label: '查询', value: 'select' }
]
const systemRequestMethodOptions = [
  { label: 'MQTT', value: 'MQTT' },
  { label: 'SYSTEM', value: 'SYSTEM' },
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' }
]

// 搜索表单
const searchForm = reactive({
  userName: '',
  operationType: undefined as string | undefined,
  traceId: '',
  deviceCode: '',
  productKey: '',
  operationModule: '',
  requestMethod: '',
  requestUrl: '',
  errorCode: '',
  exceptionClass: '',
  operationResult: undefined as number | undefined
})
const appliedFilters = reactive({
  userName: '',
  operationType: undefined as string | undefined,
  traceId: '',
  deviceCode: '',
  productKey: '',
  operationModule: '',
  requestMethod: '',
  requestUrl: '',
  errorCode: '',
  exceptionClass: '',
  operationResult: undefined as number | undefined
})
const quickSearchKeyword = ref('')
const showAdvancedFilters = ref(false)

// 分页

// 表格数据
const tableData = ref<AuditLogRecord[]>([])
const tableRef = ref()
const selectedRows = ref<AuditLogRecord[]>([])
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum, resetTotal } = useServerPagination()
const exportColumns: CsvColumn<any>[] = [
  { key: 'operationType', label: '操作类型', formatter: (value) => getOperationTypeName(String(value || '')) },
  { key: 'operationModule', label: '操作模块' },
  { key: 'operationMethod', label: '操作方法' },
  { key: 'requestUrl', label: '请求URL' },
  { key: 'requestMethod', label: '请求方法' },
  { key: 'traceId', label: 'TraceId' },
  { key: 'deviceCode', label: '设备编码' },
  { key: 'productKey', label: '产品标识' },
  { key: 'errorCode', label: '异常编码' },
  { key: 'exceptionClass', label: '异常类型' },
  { key: 'userName', label: '操作用户' },
  { key: 'ipAddress', label: '操作IP' },
  { key: 'resultMessage', label: '结果消息' },
  { key: 'operationTime', label: '操作时间' },
  { key: 'operationResult', label: '操作结果', formatter: (value) => (Number(value) === 1 ? '成功' : '失败') }
]
const exportColumnStorageKey = computed(() => (isSystemMode.value ? 'system-log-view' : 'business-log-view'))
const exportColumnOptions = toCsvColumnOptions(exportColumns)
const exportPresets = computed(() =>
  isSystemMode.value
    ? [
        { label: '默认模板', keys: ['operationModule', 'operationMethod', 'requestUrl', 'requestMethod', 'traceId', 'deviceCode', 'productKey', 'resultMessage', 'operationTime', 'operationResult'] },
        { label: '运维模板', keys: ['operationModule', 'requestUrl', 'requestMethod', 'deviceCode', 'productKey', 'resultMessage', 'operationTime'] },
        { label: '研发模板', keys: ['operationModule', 'operationMethod', 'requestUrl', 'requestMethod', 'traceId', 'deviceCode', 'productKey', 'errorCode', 'exceptionClass', 'resultMessage', 'operationResult', 'operationTime'] }
      ]
    : [
        { label: '默认模板', keys: ['operationType', 'operationModule', 'operationMethod', 'requestUrl', 'requestMethod', 'userName', 'ipAddress', 'operationTime', 'operationResult'] },
        { label: '运维模板', keys: ['operationType', 'operationModule', 'requestMethod', 'userName', 'ipAddress', 'operationTime', 'operationResult'] },
        { label: '管理模板', keys: ['operationType', 'operationModule', 'operationMethod', 'userName', 'operationTime', 'operationResult'] }
      ]
)
const selectedExportColumnKeys = ref<string[]>([])
const exportColumnDialogVisible = ref(false)

// 加载状态
const loading = ref(false)
const statsLoading = ref(false)

const createEmptySystemStats = (): SystemErrorStats => ({
  total: 0,
  todayCount: 0,
  mqttCount: 0,
  systemCount: 0,
  distinctTraceCount: 0,
  distinctDeviceCount: 0,
  topModules: [],
  topExceptionClasses: [],
  topErrorCodes: []
})

const createEmptyBusinessStats = (): BusinessAuditStats => ({
  total: 0,
  todayCount: 0,
  successCount: 0,
  failureCount: 0,
  distinctUserCount: 0,
  topModules: [],
  topUsers: [],
  topOperationTypes: []
})

const systemStats = ref<SystemErrorStats>(createEmptySystemStats())
const businessStats = ref<BusinessAuditStats>(createEmptyBusinessStats())
const scheduledTaskRows = ref<ObservabilityScheduledTask[]>([])
const scheduledTaskLoading = ref(false)
const scheduledTaskErrorMessage = ref('')
const scheduledTaskTotal = ref(0)
const messageArchiveBatchRows = ref<ObservabilityMessageArchiveBatch[]>([])
const messageArchiveBatchLoading = ref(false)
const messageArchiveBatchErrorMessage = ref('')
const messageArchiveBatchTotal = ref(0)
const messageArchiveBatchOverview = ref<ObservabilityMessageArchiveBatchOverview | null>(null)
const messageArchiveBatchOverviewLoading = ref(false)
const messageArchiveBatchOverviewErrorMessage = ref('')
const activeMessageArchiveBatchOverviewSelection = ref<ArchiveBatchOverviewSelectionKey | ''>('')
const messageArchiveBatchFocusedBatchNo = ref('')
const messageArchiveBatchPendingAutoOpen = ref(false)
const messageArchiveBatchFocusHint = ref('')
const messageArchiveBatchFilters = reactive({
  batchNo: '',
  status: '',
  compareStatus: '',
  onlyAbnormal: false,
  dateFrom: '',
  dateTo: ''
})
const messageArchiveBatchStatusOptions = [
  { label: '成功', value: 'SUCCEEDED' },
  { label: '失败', value: 'FAILED' },
  { label: '运行中', value: 'RUNNING' }
]
const messageArchiveBatchCompareStatusOptions = [
  { label: '已对齐', value: 'MATCHED' },
  { label: '有偏差', value: 'DRIFTED' },
  { label: '部分可比', value: 'PARTIAL' },
  { label: '不可用', value: 'UNAVAILABLE' }
]
const slowSummaryRows = ref<ObservabilitySlowSpanSummary[]>([])
const slowSummaryLoading = ref(false)
const slowSummaryErrorMessage = ref('')
const activeSlowSummary = ref<ObservabilitySlowSpanSummary | null>(null)
const slowSpanRows = ref<ObservabilitySpan[]>([])
const slowSpanLoading = ref(false)
const slowSpanErrorMessage = ref('')
const slowSpanTotal = ref(0)
const defaultSlowTrendWindow: SlowTrendWindowKey = 'LAST_24_HOURS'
const slowTrendWindowOptions = [
  { label: '24小时', value: 'LAST_24_HOURS' },
  { label: '7天', value: 'LAST_7_DAYS' }
] as const
const slowTrendWindow = ref<SlowTrendWindowKey>(defaultSlowTrendWindow)
const activeSlowTrendSummary = ref<ObservabilitySlowSpanSummary | null>(null)
const slowTrendRows = ref<ObservabilitySlowSpanTrend[]>([])
const slowTrendLoading = ref(false)
const slowTrendErrorMessage = ref('')
const quickSearchPlaceholder = computed(() => (isSystemMode.value ? '快速搜索（TraceId）' : '快速搜索（操作用户）'))
const advancedFilterKeys = computed<
  Array<'traceId' | 'deviceCode' | 'productKey' | 'requestUrl' | 'errorCode' | 'exceptionClass'>
>(() =>
  isSystemMode.value
    ? ['requestUrl', 'deviceCode', 'productKey', 'errorCode', 'exceptionClass']
    : ['traceId']
)
const {
  tags: activeFilterTags,
  hasAppliedFilters,
  advancedAppliedCount,
  syncAppliedFilters,
  removeFilter: removeAppliedFilter
} = useListAppliedFilters({
  form: searchForm,
  applied: appliedFilters,
  fields: [
    { key: 'userName', label: '操作用户', isActive: (value) => isBusinessMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'operationType', label: (value) => `操作类型：${getOperationTypeName(String(value || ''))}`, clearValue: undefined, isActive: (value) => isBusinessMode.value && value !== undefined },
    { key: 'traceId', label: 'TraceId', advanced: true },
    { key: 'operationModule', label: (value) => `${isSystemMode.value ? '异常模块' : '操作模块'}：${String(value || '').trim()}` },
    { key: 'requestMethod', label: (value) => `请求通道：${String(value || '')}`, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'requestUrl', label: '目标 / URL', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'deviceCode', label: '设备编码', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'productKey', label: '产品标识', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'errorCode', label: '异常编码', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'exceptionClass', label: '异常类型', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'operationResult', label: (value) => `操作结果：${getOperationResultName(Number(value))}`, clearValue: undefined, isActive: (value) => value !== undefined }
  ],
  defaults: {
    userName: '',
    operationType: undefined,
    traceId: '',
    deviceCode: '',
    productKey: '',
    operationModule: '',
    requestMethod: '',
    requestUrl: '',
    errorCode: '',
    exceptionClass: '',
    operationResult: undefined
  }
})
const appliedQuickSearchValue = computed(() => (isSystemMode.value ? appliedFilters.traceId.trim() : appliedFilters.userName.trim()))
const auditToolbarActions = computed(() => [
  {
    key: 'export-config',
    command: 'export-config',
    label: '导出列设置'
  },
  {
    key: 'export-selected',
    command: 'export-selected',
    label: '导出选中',
    disabled: selectedRows.value.length === 0
  },
  {
    key: 'export-current',
    command: 'export-current',
    label: '导出当前结果',
    disabled: tableData.value.length === 0
  },
  {
    key: 'clear-selection',
    command: 'clear-selection',
    label: '清空选中',
    disabled: selectedRows.value.length === 0
  }
])
const advancedFilterHint = computed(() => {
  if (showAdvancedFilters.value || advancedAppliedCount.value === 0) {
    return ''
  }
  return `更多条件已生效 ${advancedAppliedCount.value} 项`
})
const restoredDiagnosticContext = computed(() => {
  if (!isSystemMode.value) {
    return null
  }
  const requestMethod = typeof route.query.requestMethod === 'string' ? route.query.requestMethod : ''
  const requestUrl = typeof route.query.requestUrl === 'string' ? route.query.requestUrl : ''
  return resolveDiagnosticContext({
    ...route.query,
    topic: requestMethod === 'MQTT' ? requestUrl || route.query.topic : route.query.topic
  } as Record<string, unknown>)
})
const systemInlineMessage = computed(() =>
  restoredDiagnosticContext.value
    ? [
        `来自${describeDiagnosticSource(restoredDiagnosticContext.value.sourcePage)}`,
        '当前节点：后台异常核对',
        '下一步回链路追踪台或治理页继续排查。'
      ].join(' · ')
    : ''
)
const showSystemInlineState = computed(() => isSystemMode.value && Boolean(systemInlineMessage.value))

// 详情对话框
const detailVisible = ref(false)
const detailData = ref<Partial<AuditLogRecord>>({})
const detailLoading = ref(false)
const detailErrorMessage = ref('')
const evidenceDrawerVisible = ref(false)
const evidenceLoading = ref(false)
const evidenceErrorMessage = ref('')
const evidenceTrace = ref<ObservabilityTraceEvidence | null>(null)
const evidenceTraceId = ref('')
const messageArchiveBatchDrawerVisible = ref(false)
const activeMessageArchiveBatch = ref<ObservabilityMessageArchiveBatch | null>(null)
const messageArchiveBatchCompareLoading = ref(false)
const messageArchiveBatchCompareErrorMessage = ref('')
const activeMessageArchiveBatchCompare = ref<ObservabilityMessageArchiveBatchCompare | null>(null)
const messageArchiveBatchReportPreviewLoading = ref(false)
const messageArchiveBatchReportPreviewErrorMessage = ref('')
const activeMessageArchiveBatchReportPreview = ref<ObservabilityMessageArchiveBatchReportPreview | null>(null)

const defaultExportKeys = exportColumns.map((column) => String(column.key))
const evidenceBusinessEvents = computed(() => evidenceTrace.value?.businessEvents ?? [])
const evidenceSpans = computed(() => evidenceTrace.value?.spans ?? [])
const evidenceTimeline = computed(() => evidenceTrace.value?.timeline ?? [])
const evidenceDrawerSubtitle = computed(() =>
  evidenceTraceId.value ? `TraceId：${evidenceTraceId.value}` : '按 TraceId 汇总业务事件与调用片段'
)
const evidenceDrawerTags = computed(() => [
  { label: `事件 ${evidenceBusinessEvents.value.length}`, type: 'primary' as const },
  { label: `片段 ${evidenceSpans.value.length}`, type: 'info' as const }
])
const evidenceSummaryCards = computed(() => [
  { label: 'TraceId', value: formatValue(evidenceTrace.value?.traceId || evidenceTraceId.value) },
  { label: '业务事件', value: String(evidenceBusinessEvents.value.length) },
  { label: '调用片段', value: String(evidenceSpans.value.length) },
  { label: '时间线节点', value: String(evidenceTimeline.value.length) }
])
const messageArchiveBatchDrawerSubtitle = computed(() =>
  activeMessageArchiveBatch.value?.batchNo
    ? `批次号：${activeMessageArchiveBatch.value.batchNo}`
    : '查看消息热表归档批次的确认、归档与删除结果'
)
const messageArchiveBatchDrawerTags = computed(() => {
  const row = activeMessageArchiveBatch.value
  return [
    { label: `状态 ${formatValue(row?.status)}`, type: 'primary' as const },
    { label: `来源 ${formatValue(row?.sourceTable)}`, type: 'info' as const }
  ]
})
const messageArchiveBatchSummaryCards = computed(() => {
  const row = activeMessageArchiveBatch.value
  return [
    { label: '批次号', value: formatArchiveBatchName(row) },
    { label: '状态', value: formatValue(row?.status) },
    { label: '确认行数', value: formatCount(row?.confirmedExpiredRows) },
    { label: '删除行数', value: formatCount(row?.deletedRows) }
  ]
})
const messageArchiveBatchResultItems = computed<ArchiveBatchDetailItem[]>(() => {
  const row = activeMessageArchiveBatch.value
  return [
    { label: '来源表', value: formatValue(row?.sourceTable) },
    { label: '治理模式', value: formatValue(row?.governanceMode) },
    { label: '保留期', value: formatRetentionDays(row?.retentionDays) },
    { label: '候选行数', value: formatCount(row?.candidateRows) },
    { label: '归档行数', value: formatCount(row?.archivedRows) },
    { label: '删除行数', value: formatCount(row?.deletedRows) }
  ]
})
const messageArchiveBatchReportItems = computed<ArchiveBatchDetailItem[]>(() => {
  const row = activeMessageArchiveBatch.value
  return [
    { label: '确认报告', value: formatValue(row?.confirmReportPath) },
    { label: '报告生成时间', value: formatValue(row?.confirmReportGeneratedAt) },
    { label: '截止时间', value: formatValue(row?.cutoffAt) },
    { label: '失败原因', value: formatValue(row?.failedReason) }
  ]
})
const messageArchiveBatchArtifacts = computed<ArchiveBatchDetailItem[]>(() =>
  parseArchiveBatchArtifacts(activeMessageArchiveBatch.value?.artifactsJson)
)
const messageArchiveBatchCompareStatus = computed<ArchiveBatchCompareStatus>(() => {
  const normalized = String(activeMessageArchiveBatchCompare.value?.compareStatus || '').trim().toUpperCase()
  if (normalized === 'MATCHED' || normalized === 'DRIFTED' || normalized === 'PARTIAL') {
    return normalized
  }
  return 'UNAVAILABLE'
})
const messageArchiveBatchCompareStatusName = computed(() =>
  formatArchiveBatchCompareStatus(messageArchiveBatchCompareStatus.value)
)
const messageArchiveBatchCompareHeadline = computed(() => {
  switch (messageArchiveBatchCompareStatus.value) {
    case 'MATCHED':
      return '已按确认结果落地'
    case 'DRIFTED':
      return '执行结果与确认结果存在偏差'
    case 'PARTIAL':
      return '仅完成部分比对'
    default:
      return '当前缺少可信对比证据'
  }
})
const messageArchiveBatchCompareStatusClass = computed(
  () => `is-${messageArchiveBatchCompareStatus.value.toLowerCase()}`
)
const messageArchiveBatchCompareSourceItems = computed<ArchiveBatchDetailItem[]>(() => {
  const compare = activeMessageArchiveBatchCompare.value
  const sources = compare?.sources
  if (!compare || !sources) {
    return []
  }
  return [
    { label: '确认报告', value: formatValue(sources.confirmReportPath) },
    {
      label: 'dry-run JSON',
      value: formatValue(sources.resolvedDryRunJsonPath || sources.confirmReportPath)
    },
    { label: 'apply JSON', value: formatValue(sources.resolvedApplyJsonPath) },
    { label: 'dry-run 可用', value: sources.dryRunAvailable ? '是' : '否' },
    { label: 'apply 可用', value: sources.applyAvailable ? '是' : '否' }
  ]
})
const messageArchiveBatchCompareSummaryItems = computed<ArchiveBatchDetailItem[]>(() => {
  const summary = activeMessageArchiveBatchCompare.value?.summaryCompare
  if (!summary) {
    return []
  }
  return [
    { label: '确认过期', value: formatOptionalCount(summary.confirmedExpiredRows) },
    { label: 'dry-run 过期', value: formatOptionalCount(summary.dryRunExpiredRows) },
    { label: 'apply 归档', value: formatOptionalCount(summary.applyArchivedRows) },
    { label: 'apply 删除', value: formatOptionalCount(summary.applyDeletedRows) },
    { label: '剩余过期', value: formatOptionalCount(summary.remainingExpiredRows) },
    { label: '确认差值', value: formatOptionalCount(summary.deltaConfirmedVsDeleted) },
    { label: 'dry-run 差值', value: formatOptionalCount(summary.deltaDryRunVsDeleted) }
  ]
})
const messageArchiveBatchCompareTableComparisons = computed<ObservabilityMessageArchiveBatchCompareTable[]>(
  () => activeMessageArchiveBatchCompare.value?.tableComparisons ?? []
)
const messageArchiveBatchReportPreviewMetaItems = computed<ArchiveBatchDetailItem[]>(() => {
  const preview = activeMessageArchiveBatchReportPreview.value
  if (!preview || preview.available === false) {
    return []
  }
  return [
    { label: 'JSON 路径', value: formatValue(preview.resolvedJsonPath || preview.confirmReportPath) },
    {
      label: 'Markdown 路径',
      value: preview.markdownAvailable ? formatValue(preview.resolvedMarkdownPath) : '未生成'
    },
    { label: '文件更新时间', value: formatValue(preview.fileLastModifiedAt) },
    { label: '报告生成时间', value: formatValue(preview.confirmReportGeneratedAt) }
  ]
})
const messageArchiveBatchReportPreviewSummaryItems = computed<ArchiveBatchDetailItem[]>(() => {
  const preview = activeMessageArchiveBatchReportPreview.value
  const summary = preview?.summary
  if (!summary || typeof summary !== 'object') {
    return []
  }
  return Object.entries(summary).map(([key, value]) => ({
    label: formatArchiveBatchReportSummaryLabel(key),
    value: normalizeArchiveBatchDetailValue(value)
  }))
})
const messageArchiveBatchReportPreviewTableSummaries = computed<ObservabilityMessageArchiveBatchReportTableSummary[]>(
  () => activeMessageArchiveBatchReportPreview.value?.tableSummaries ?? []
)
const messageArchiveBatchOverviewCards = computed<ArchiveBatchOverviewCard[]>(() => {
  const overview = messageArchiveBatchOverview.value
  return [
    {
      key: 'abnormal',
      label: '异常批次',
      value: formatOptionalCount(overview?.abnormalBatches),
      meta: `总批次 ${formatOptionalCount(overview?.totalBatches)}`,
      testId: 'archive-batch-overview-abnormal',
      clickable: true,
      active: activeMessageArchiveBatchOverviewSelection.value === 'abnormal'
    },
    {
      key: 'drifted',
      label: '执行偏差总量',
      value: formatSignedCount(overview?.totalDeltaConfirmedVsDeleted),
      meta: `已对齐 ${formatOptionalCount(overview?.matchedBatches)}`,
      testId: 'archive-batch-overview-drifted',
      clickable: true,
      active: activeMessageArchiveBatchOverviewSelection.value === 'drifted'
    },
    {
      key: 'remaining',
      label: '剩余过期总量',
      value: formatOptionalCount(overview?.totalRemainingExpiredRows),
      meta: `部分可比 ${formatOptionalCount(overview?.partialBatches)}`,
      testId: 'archive-batch-overview-remaining',
      clickable: true,
      active: activeMessageArchiveBatchOverviewSelection.value === 'remaining'
    },
    {
      key: 'latest',
      label: '最近异常批次',
      value: formatValue(overview?.latestAbnormalBatch),
      meta: formatValue(overview?.latestAbnormalOccurredAt),
      testId: 'archive-batch-overview-latest',
      clickable: true,
      active: activeMessageArchiveBatchOverviewSelection.value === 'latest'
    }
  ]
})

const reloadExportSelection = () => {
  selectedExportColumnKeys.value = loadCsvColumnSelection(exportColumnStorageKey.value, defaultExportKeys)
}

const hasFilledFilter = (value: string | number | undefined) => {
  if (typeof value === 'string') {
    return value.trim() !== ''
  }
  return value !== undefined
}

const syncQuickSearchKeywordFromFilters = () => {
  quickSearchKeyword.value = isSystemMode.value ? searchForm.traceId : searchForm.userName
}

const applyQuickSearchKeywordToFilters = () => {
  const keyword = quickSearchKeyword.value.trim()
  if (isSystemMode.value) {
    searchForm.traceId = keyword
    return
  }
  searchForm.userName = keyword
}

const syncAdvancedFilterState = () => {
  showAdvancedFilters.value = advancedFilterKeys.value.some((key) => hasFilledFilter(searchForm[key]))
}

const resetSearchForm = () => {
  quickSearchKeyword.value = ''
  searchForm.userName = ''
  searchForm.operationType = undefined
  searchForm.traceId = ''
  searchForm.deviceCode = ''
  searchForm.productKey = ''
  searchForm.operationModule = ''
  searchForm.requestMethod = ''
  searchForm.requestUrl = ''
  searchForm.errorCode = ''
  searchForm.exceptionClass = ''
  searchForm.operationResult = undefined
  showAdvancedFilters.value = false
}

const readRouteQueryValue = (key: string) => {
  const value = route.query[key]
  return typeof value === 'string' ? value : ''
}

const parseOptionalNumber = (value: string) => {
  if (!value) {
    return undefined
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

const applySystemRouteQuery = () => {
  if (!isSystemMode.value) {
    return
  }
  const context = restoredDiagnosticContext.value
  searchForm.traceId = readRouteQueryValue('traceId') || context?.traceId || ''
  searchForm.deviceCode = readRouteQueryValue('deviceCode') || context?.deviceCode || ''
  searchForm.productKey = readRouteQueryValue('productKey') || context?.productKey || ''
  searchForm.operationModule = readRouteQueryValue('operationModule')
  searchForm.requestMethod = readRouteQueryValue('requestMethod') || (context?.topic ? 'MQTT' : '')
  searchForm.requestUrl = readRouteQueryValue('requestUrl') || context?.topic || ''
  searchForm.errorCode = readRouteQueryValue('errorCode')
  searchForm.exceptionClass = readRouteQueryValue('exceptionClass')
  searchForm.operationResult = parseOptionalNumber(readRouteQueryValue('operationResult'))
  syncQuickSearchKeywordFromFilters()
  syncAdvancedFilterState()
}

// 获取审计日志查询条件
const buildAuditLogQueryParams = () => ({
  traceId: appliedFilters.traceId,
  operationModule: appliedFilters.operationModule,
  operationResult: appliedFilters.operationResult,
  ...(isBusinessMode.value
    ? {
        userName: appliedFilters.userName,
        operationType: appliedFilters.operationType,
        excludeSystemError: true
      }
    : {
        operationType: 'system_error',
        deviceCode: appliedFilters.deviceCode,
        productKey: appliedFilters.productKey,
        requestMethod: appliedFilters.requestMethod,
        requestUrl: appliedFilters.requestUrl,
        errorCode: appliedFilters.errorCode,
        exceptionClass: appliedFilters.exceptionClass
      })
})

const logPageError = (context: string, error: unknown) => {
  if (!isHandledRequestError(error)) {
    console.error(context, error)
  }
}

// 获取审计日志列表
const getAuditLogList = async () => {
  loading.value = true
  try {
    const res = await pageLogs({
      ...buildAuditLogQueryParams(),
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res.code === 200) {
      tableData.value = applyPageResult(res.data)
    }
  } catch (error) {
    logPageError('获取审计日志列表失败', error)
  } finally {
    loading.value = false
  }
}

// 获取日志统计
const getAuditLogStats = async () => {
  statsLoading.value = true
  try {
    if (isSystemMode.value) {
      systemStats.value = createEmptySystemStats()
      const res = await getSystemErrorStats(buildAuditLogQueryParams())
      if (res.code === 200 && res.data) {
        systemStats.value = { ...createEmptySystemStats(), ...res.data }
      }
      return
    }

    businessStats.value = createEmptyBusinessStats()
    const res = await getBusinessAuditStats(buildAuditLogQueryParams())
    if (res.code === 200 && res.data) {
      businessStats.value = { ...createEmptyBusinessStats(), ...res.data }
    }
  } catch (error) {
    logPageError('获取日志统计失败', error)
  } finally {
    statsLoading.value = false
  }
}

const buildSlowSummaryQueryParams = (): ObservabilitySlowSpanSummaryQuery => ({
  limit: 5,
  minDurationMs: 1
})

const buildScheduledTaskQueryParams = (): ObservabilityScheduledTaskPageQuery => ({
  pageNum: 1,
  pageSize: 5
})

const buildMessageArchiveBatchBoundary = (date: string, mode: 'start' | 'end') => {
  const normalized = date.trim()
  if (!normalized) {
    return undefined
  }
  return `${normalized} ${mode === 'start' ? '00:00:00' : '23:59:59'}`
}

const buildMessageArchiveBatchQueryParams = (): ObservabilityMessageArchiveBatchPageQuery => ({
  sourceTable: 'iot_message_log',
  batchNo: messageArchiveBatchFilters.batchNo.trim() || undefined,
  status: messageArchiveBatchFilters.status.trim() || undefined,
  compareStatus: messageArchiveBatchFilters.compareStatus.trim() || undefined,
  onlyAbnormal: messageArchiveBatchFilters.onlyAbnormal || undefined,
  dateFrom: buildMessageArchiveBatchBoundary(messageArchiveBatchFilters.dateFrom, 'start'),
  dateTo: buildMessageArchiveBatchBoundary(messageArchiveBatchFilters.dateTo, 'end'),
  pageNum: 1,
  pageSize: 5
})

const buildMessageArchiveBatchOverviewQueryParams = (): ObservabilityMessageArchiveBatchOverviewQuery => ({
  sourceTable: 'iot_message_log',
  dateFrom: buildMessageArchiveBatchBoundary(messageArchiveBatchFilters.dateFrom, 'start'),
  dateTo: buildMessageArchiveBatchBoundary(messageArchiveBatchFilters.dateTo, 'end')
})

const clearScheduledTaskLedger = () => {
  scheduledTaskRows.value = []
  scheduledTaskLoading.value = false
  scheduledTaskErrorMessage.value = ''
  scheduledTaskTotal.value = 0
}

const clearMessageArchiveBatchLedger = () => {
  messageArchiveBatchRows.value = []
  messageArchiveBatchLoading.value = false
  messageArchiveBatchErrorMessage.value = ''
  messageArchiveBatchTotal.value = 0
}

const clearMessageArchiveBatchOverview = () => {
  messageArchiveBatchOverview.value = null
  messageArchiveBatchOverviewLoading.value = false
  messageArchiveBatchOverviewErrorMessage.value = ''
}

const clearMessageArchiveBatchOverviewFocus = () => {
  messageArchiveBatchFocusedBatchNo.value = ''
  messageArchiveBatchPendingAutoOpen.value = false
  messageArchiveBatchFocusHint.value = ''
}

const resetMessageArchiveBatchSummarySelection = () => {
  activeMessageArchiveBatchOverviewSelection.value = ''
  clearMessageArchiveBatchOverviewFocus()
}

const clearMessageArchiveBatchReportPreview = () => {
  messageArchiveBatchReportPreviewLoading.value = false
  messageArchiveBatchReportPreviewErrorMessage.value = ''
  activeMessageArchiveBatchReportPreview.value = null
}

const clearMessageArchiveBatchCompare = () => {
  messageArchiveBatchCompareLoading.value = false
  messageArchiveBatchCompareErrorMessage.value = ''
  activeMessageArchiveBatchCompare.value = null
}

const getScheduledTaskLedger = async () => {
  if (!isSystemMode.value) {
    clearScheduledTaskLedger()
    return
  }

  scheduledTaskLoading.value = true
  scheduledTaskErrorMessage.value = ''
  try {
    const res = await pageObservabilityScheduledTasks(buildScheduledTaskQueryParams())
    if (res.code === 200 && res.data) {
      scheduledTaskRows.value = Array.isArray(res.data.records) ? res.data.records : []
      scheduledTaskTotal.value = Number(res.data.total || scheduledTaskRows.value.length)
    }
  } catch (error) {
    clearScheduledTaskLedger()
    scheduledTaskErrorMessage.value = error instanceof Error ? error.message : '获取调度任务台账失败'
    logPageError('获取调度任务台账失败', error)
  } finally {
    scheduledTaskLoading.value = false
  }
}

const getMessageArchiveBatchLedger = async () => {
  if (!isSystemMode.value) {
    clearMessageArchiveBatchLedger()
    return
  }

  messageArchiveBatchLoading.value = true
  messageArchiveBatchErrorMessage.value = ''
  try {
    const res = await pageObservabilityMessageArchiveBatches(buildMessageArchiveBatchQueryParams())
    if (res.code === 200 && res.data) {
      messageArchiveBatchRows.value = Array.isArray(res.data.records) ? res.data.records : []
      messageArchiveBatchTotal.value = Number(res.data.total || messageArchiveBatchRows.value.length)
    }
  } catch (error) {
    clearMessageArchiveBatchLedger()
    messageArchiveBatchErrorMessage.value = error instanceof Error ? error.message : '获取归档批次台账失败'
    logPageError('获取归档批次台账失败', error)
  } finally {
    messageArchiveBatchLoading.value = false
  }
}

const getMessageArchiveBatchOverview = async () => {
  if (!isSystemMode.value) {
    clearMessageArchiveBatchOverview()
    return
  }

  messageArchiveBatchOverviewLoading.value = true
  messageArchiveBatchOverviewErrorMessage.value = ''
  try {
    const res = await getObservabilityMessageArchiveBatchOverview(buildMessageArchiveBatchOverviewQueryParams())
    if (res.code === 200 && res.data) {
      messageArchiveBatchOverview.value = res.data
    } else {
      messageArchiveBatchOverview.value = null
    }
  } catch (error) {
    clearMessageArchiveBatchOverview()
    messageArchiveBatchOverviewErrorMessage.value =
      error instanceof Error ? error.message : '获取归档批次异常摘要失败'
    logPageError('获取归档批次异常摘要失败', error)
  } finally {
    messageArchiveBatchOverviewLoading.value = false
  }
}

const resolveMessageArchiveBatchSummaryFocus = async () => {
  if (!messageArchiveBatchPendingAutoOpen.value) {
    return
  }

  messageArchiveBatchPendingAutoOpen.value = false
  messageArchiveBatchFocusHint.value = ''
  messageArchiveBatchFocusedBatchNo.value = String(
    messageArchiveBatchOverview.value?.latestAbnormalBatch || ''
  ).trim()
  if (!messageArchiveBatchFocusedBatchNo.value) {
    return
  }

  const matchedRow = messageArchiveBatchRows.value.find(
    (row) => String(row.batchNo || '').trim() === messageArchiveBatchFocusedBatchNo.value
  )
  if (matchedRow) {
    await openMessageArchiveBatchDetail(matchedRow)
    return
  }

  messageArchiveBatchFocusHint.value = '最近异常批次不在当前结果中，请调整时间范围后重试'
}

const refreshMessageArchiveBatchLedger = async () => {
  await Promise.all([getMessageArchiveBatchLedger(), getMessageArchiveBatchOverview()])
  await resolveMessageArchiveBatchSummaryFocus()
}

const handleMessageArchiveBatchSearch = () => {
  messageArchiveBatchFocusHint.value = ''
  if (activeMessageArchiveBatchOverviewSelection.value === 'latest') {
    messageArchiveBatchPendingAutoOpen.value = true
  }
  void refreshMessageArchiveBatchLedger()
}

const applyMessageArchiveBatchOverviewSelection = (
  selection: ArchiveBatchOverviewSelectionKey
) => {
  activeMessageArchiveBatchOverviewSelection.value = selection
  messageArchiveBatchFocusHint.value = ''
  messageArchiveBatchFocusedBatchNo.value = ''
  messageArchiveBatchPendingAutoOpen.value = selection === 'latest'
  if (selection === 'drifted') {
    messageArchiveBatchFilters.compareStatus = 'DRIFTED'
    messageArchiveBatchFilters.onlyAbnormal = false
    return
  }
  messageArchiveBatchFilters.compareStatus = ''
  messageArchiveBatchFilters.onlyAbnormal = true
}

const handleMessageArchiveBatchOverviewClick = (selection: ArchiveBatchOverviewSelectionKey) => {
  applyMessageArchiveBatchOverviewSelection(selection)
  void refreshMessageArchiveBatchLedger()
}

const resetMessageArchiveBatchFilters = () => {
  messageArchiveBatchFilters.batchNo = ''
  messageArchiveBatchFilters.status = ''
  messageArchiveBatchFilters.compareStatus = ''
  messageArchiveBatchFilters.onlyAbnormal = false
  messageArchiveBatchFilters.dateFrom = ''
  messageArchiveBatchFilters.dateTo = ''
  resetMessageArchiveBatchSummarySelection()
  void refreshMessageArchiveBatchLedger()
}

const loadMessageArchiveBatchReportPreview = async (row: ObservabilityMessageArchiveBatch) => {
  const batchNo = String(row.batchNo || '').trim()
  if (!batchNo) {
    clearMessageArchiveBatchReportPreview()
    messageArchiveBatchReportPreviewErrorMessage.value = '当前批次缺少批次号，无法加载确认报告预览'
    return
  }

  messageArchiveBatchReportPreviewLoading.value = true
  messageArchiveBatchReportPreviewErrorMessage.value = ''
  activeMessageArchiveBatchReportPreview.value = null
  try {
    const res = await getObservabilityMessageArchiveBatchReportPreview(batchNo)
    if (res.code === 200 && res.data) {
      activeMessageArchiveBatchReportPreview.value = res.data
    } else {
      messageArchiveBatchReportPreviewErrorMessage.value = '确认报告预览未返回有效内容'
    }
  } catch (error) {
    clearMessageArchiveBatchReportPreview()
    messageArchiveBatchReportPreviewErrorMessage.value =
      error instanceof Error ? error.message : '加载确认报告预览失败'
    logPageError('加载确认报告预览失败', error)
  } finally {
    messageArchiveBatchReportPreviewLoading.value = false
  }
}

const loadMessageArchiveBatchCompare = async (row: ObservabilityMessageArchiveBatch) => {
  const batchNo = String(row.batchNo || '').trim()
  if (!batchNo) {
    clearMessageArchiveBatchCompare()
    messageArchiveBatchCompareErrorMessage.value = '当前批次缺少批次号，无法加载批次对比'
    return
  }

  messageArchiveBatchCompareLoading.value = true
  messageArchiveBatchCompareErrorMessage.value = ''
  activeMessageArchiveBatchCompare.value = null
  try {
    const res = await getObservabilityMessageArchiveBatchCompare(batchNo)
    if (res.code === 200 && res.data) {
      activeMessageArchiveBatchCompare.value = res.data
    } else {
      messageArchiveBatchCompareErrorMessage.value = '批次对比未返回有效内容'
    }
  } catch (error) {
    clearMessageArchiveBatchCompare()
    messageArchiveBatchCompareErrorMessage.value =
      error instanceof Error ? error.message : '加载批次对比失败'
    logPageError('加载批次对比失败', error)
  } finally {
    messageArchiveBatchCompareLoading.value = false
  }
}

const clearSlowSpanDrilldown = () => {
  activeSlowSummary.value = null
  slowSpanRows.value = []
  slowSpanLoading.value = false
  slowSpanErrorMessage.value = ''
  slowSpanTotal.value = 0
}

const clearSlowTrendDrilldown = () => {
  activeSlowTrendSummary.value = null
  slowTrendRows.value = []
  slowTrendLoading.value = false
  slowTrendErrorMessage.value = ''
  slowTrendWindow.value = defaultSlowTrendWindow
}

const getSlowSpanSummaries = async () => {
  if (!isSystemMode.value) {
    slowSummaryRows.value = []
    slowSummaryErrorMessage.value = ''
    slowSummaryLoading.value = false
    clearSlowSpanDrilldown()
    clearSlowTrendDrilldown()
    return
  }

  slowSummaryLoading.value = true
  slowSummaryErrorMessage.value = ''
  clearSlowSpanDrilldown()
  clearSlowTrendDrilldown()
  try {
    const res = await listObservabilitySlowSpanSummaries(buildSlowSummaryQueryParams())
    if (res.code === 200) {
      slowSummaryRows.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (error) {
    slowSummaryRows.value = []
    slowSummaryErrorMessage.value = error instanceof Error ? error.message : '获取性能慢点汇总失败'
    logPageError('获取性能慢点汇总失败', error)
  } finally {
    slowSummaryLoading.value = false
  }
}

const setSlowSpanQueryValue = (
  params: ObservabilitySpanPageQuery,
  key: 'spanType' | 'eventCode' | 'domainCode' | 'objectType' | 'objectId',
  value?: string | null
) => {
  const normalized = (value || '').trim()
  if (normalized) {
    params[key] = normalized
  }
}

const buildSlowSpanQueryParams = (row: ObservabilitySlowSpanSummary): ObservabilitySpanPageQuery => {
  const params: ObservabilitySpanPageQuery = {
    minDurationMs: 1,
    pageNum: 1,
    pageSize: 5
  }
  setSlowSpanQueryValue(params, 'spanType', row.spanType)
  setSlowSpanQueryValue(params, 'domainCode', row.domainCode)
  setSlowSpanQueryValue(params, 'eventCode', row.eventCode)
  setSlowSpanQueryValue(params, 'objectType', row.objectType)
  setSlowSpanQueryValue(params, 'objectId', row.objectId)
  return params
}

const padDatePart = (value: number) => String(value).padStart(2, '0')

const formatQueryDateTime = (date: Date) => [
  date.getFullYear(),
  padDatePart(date.getMonth() + 1),
  padDatePart(date.getDate())
].join('-') + ` ${[padDatePart(date.getHours()), padDatePart(date.getMinutes()), padDatePart(date.getSeconds())].join(':')}`

const resolveSlowTrendWindowRange = (windowKey: SlowTrendWindowKey): Pick<
  ObservabilitySlowSpanTrendQuery,
  'bucket' | 'dateFrom' | 'dateTo'
> => {
  const now = new Date()
  if (windowKey === 'LAST_7_DAYS') {
    const start = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 6, 0, 0, 0)
    return {
      bucket: 'DAY',
      dateFrom: formatQueryDateTime(start),
      dateTo: formatQueryDateTime(now)
    }
  }
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate(), now.getHours(), 0, 0)
  start.setHours(start.getHours() - 23)
  return {
    bucket: 'HOUR',
    dateFrom: formatQueryDateTime(start),
    dateTo: formatQueryDateTime(now)
  }
}

const buildSlowTrendQueryParams = (
  row: ObservabilitySlowSpanSummary,
  windowKey: SlowTrendWindowKey
): ObservabilitySlowSpanTrendQuery => {
  const params: ObservabilitySlowSpanTrendQuery = {
    minDurationMs: 1,
    ...resolveSlowTrendWindowRange(windowKey)
  }
  setSlowSpanQueryValue(params, 'spanType', row.spanType)
  setSlowSpanQueryValue(params, 'domainCode', row.domainCode)
  setSlowSpanQueryValue(params, 'eventCode', row.eventCode)
  setSlowSpanQueryValue(params, 'objectType', row.objectType)
  setSlowSpanQueryValue(params, 'objectId', row.objectId)
  return params
}

const loadSlowSpanDrilldown = async (row: ObservabilitySlowSpanSummary) => {
  if (!isSystemMode.value) {
    return
  }
  activeSlowSummary.value = row
  slowSpanRows.value = []
  slowSpanTotal.value = 0
  slowSpanErrorMessage.value = ''
  slowSpanLoading.value = true
  try {
    const res = await pageObservabilitySpans(buildSlowSpanQueryParams(row))
    if (res.code === 200 && res.data) {
      slowSpanRows.value = Array.isArray(res.data.records) ? res.data.records : []
      slowSpanTotal.value = Number(res.data.total || slowSpanRows.value.length)
    }
  } catch (error) {
    slowSpanRows.value = []
    slowSpanErrorMessage.value = error instanceof Error ? error.message : '获取慢点明细失败'
    logPageError('获取慢点明细失败', error)
  } finally {
    slowSpanLoading.value = false
  }
}

const loadSlowTrendDrilldown = async (
  row: ObservabilitySlowSpanSummary,
  windowKey: SlowTrendWindowKey = defaultSlowTrendWindow
) => {
  if (!isSystemMode.value) {
    return
  }
  activeSlowTrendSummary.value = row
  slowTrendWindow.value = windowKey
  slowTrendRows.value = []
  slowTrendErrorMessage.value = ''
  slowTrendLoading.value = true
  try {
    const res = await listObservabilitySlowSpanTrends(buildSlowTrendQueryParams(row, windowKey))
    if (res.code === 200) {
      slowTrendRows.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (error) {
    slowTrendRows.value = []
    slowTrendErrorMessage.value = error instanceof Error ? error.message : '获取慢点趋势失败'
    logPageError('获取慢点趋势失败', error)
  } finally {
    slowTrendLoading.value = false
  }
}

const handleSlowTrendWindowChange = (value: string | number | boolean) => {
  if (!activeSlowTrendSummary.value) {
    return
  }
  const nextWindow: SlowTrendWindowKey =
    String(value) === 'LAST_7_DAYS' ? 'LAST_7_DAYS' : defaultSlowTrendWindow
  void loadSlowTrendDrilldown(activeSlowTrendSummary.value, nextWindow)
}

// 初始化
onMounted(() => {
  reloadExportSelection()
  applySystemRouteQuery()
  syncQuickSearchKeywordFromFilters()
  syncAdvancedFilterState()
  syncAppliedFilters()
  getAuditLogList()
  getAuditLogStats()
  getScheduledTaskLedger()
  refreshMessageArchiveBatchLedger()
  getSlowSpanSummaries()
})

watch(viewMode, (newMode, oldMode) => {
  if (newMode === oldMode) {
    return
  }
  resetSearchForm()
  clearSelection()
  resetPage()
  resetTotal()
  detailVisible.value = false
  detailData.value = {}
  detailLoading.value = false
  detailErrorMessage.value = ''
  evidenceDrawerVisible.value = false
  evidenceTrace.value = null
  evidenceTraceId.value = ''
  evidenceLoading.value = false
  evidenceErrorMessage.value = ''
  clearScheduledTaskLedger()
  clearMessageArchiveBatchLedger()
  clearMessageArchiveBatchOverview()
  resetMessageArchiveBatchSummarySelection()
  slowSummaryRows.value = []
  slowSummaryLoading.value = false
  slowSummaryErrorMessage.value = ''
  clearSlowSpanDrilldown()
  clearSlowTrendDrilldown()
  messageArchiveBatchDrawerVisible.value = false
  activeMessageArchiveBatch.value = null
  exportColumnDialogVisible.value = false
  reloadExportSelection()
  applySystemRouteQuery()
  syncQuickSearchKeywordFromFilters()
  syncAdvancedFilterState()
  syncAppliedFilters()
  getAuditLogList()
  getAuditLogStats()
  getScheduledTaskLedger()
  refreshMessageArchiveBatchLedger()
  getSlowSpanSummaries()
})

watch(
  () => [
    route.query.traceId,
    route.query.deviceCode,
    route.query.productKey,
    route.query.operationModule,
    route.query.requestMethod,
    route.query.requestUrl,
    route.query.errorCode,
    route.query.exceptionClass,
    route.query.operationResult
  ],
  (current, previous) => {
    if (!isSystemMode.value) {
      return
    }
    if (JSON.stringify(current) === JSON.stringify(previous)) {
      return
    }
    applySystemRouteQuery()
    resetPage()
    clearSelection()
    syncAppliedFilters()
    getAuditLogList()
    getAuditLogStats()
    getScheduledTaskLedger()
    refreshMessageArchiveBatchLedger()
    getSlowSpanSummaries()
  }
)

const triggerSearch = (resetPageFirst = false) => {
  applyQuickSearchKeywordToFilters()
  syncAdvancedFilterState()
  syncAppliedFilters()
  if (resetPageFirst) {
    resetPage()
  }
  clearSelection()
  getAuditLogList()
  getAuditLogStats()
  getScheduledTaskLedger()
  refreshMessageArchiveBatchLedger()
  getSlowSpanSummaries()
}

// 处理搜索
const handleSearch = () => {
  triggerSearch(true)
}

// 重置搜索
const handleReset = () => {
  resetSearchForm()
  resetMessageArchiveBatchSummarySelection()
  triggerSearch(true)
}

const handleQuickSearch = () => {
  triggerSearch(true)
}

const handleClearQuickSearch = () => {
  quickSearchKeyword.value = ''
  triggerSearch(true)
}

const toggleAdvancedFilters = () => {
  showAdvancedFilters.value = !showAdvancedFilters.value
}

const handleClearAppliedFilters = () => {
  handleReset()
}

const handleRemoveAppliedFilter = (key: string) => {
  removeAppliedFilter(key)
  syncQuickSearchKeywordFromFilters()
  syncAdvancedFilterState()
  triggerSearch(true)
}

const handleSelectionChange = (rows: AuditLogRecord[]) => {
  selectedRows.value = rows
}

const clearSelection = () => {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

const handleRefresh = () => {
  triggerSearch(false)
}

const formatValue = (value: unknown) => {
  if (value === undefined || value === null) {
    return '--'
  }
  const text = String(value).trim()
  return text ? text : '--'
}

const buildSystemDiagnosticContext = (source?: Partial<AuditLogRecord>): DiagnosticContext => {
  const traceId = source?.traceId || quickSearchKeyword.value.trim() || searchForm.traceId || undefined
  const deviceCode = source?.deviceCode || searchForm.deviceCode || undefined
  const productKey = source?.productKey || searchForm.productKey || undefined
  const requestMethod = source?.requestMethod || searchForm.requestMethod
  const requestUrl = source?.requestUrl || searchForm.requestUrl
  return {
    sourcePage: 'system-log',
    traceId,
    deviceCode,
    productKey,
    topic: requestMethod === 'MQTT' ? requestUrl || undefined : undefined,
    reportStatus: systemStats.value.total > 0 ? 'failed' : 'timeline-missing',
    capturedAt: new Date().toISOString()
  }
}

const persistSystemContext = (source?: Partial<AuditLogRecord>) => {
  persistDiagnosticContext(buildSystemDiagnosticContext(source))
}

const canJumpToMessageTrace = (row?: AuditLogRecord) => {
  const context = buildSystemDiagnosticContext(row)
  return Boolean(context.traceId || context.deviceCode || context.productKey || context.topic)
}

const resolveEvidenceTraceId = (row?: Partial<AuditLogRecord>) =>
  (row?.traceId || searchForm.traceId || appliedFilters.traceId).trim()

const canOpenTraceEvidence = (row?: Partial<AuditLogRecord>) => Boolean(resolveEvidenceTraceId(row))

const openTraceEvidence = async (row?: Partial<AuditLogRecord>) => {
  const traceId = resolveEvidenceTraceId(row)
  if (!traceId) {
    ElMessage.warning('当前记录缺少 TraceId，无法查看证据包')
    return
  }
  evidenceDrawerVisible.value = true
  evidenceTraceId.value = traceId
  evidenceTrace.value = null
  evidenceErrorMessage.value = ''
  evidenceLoading.value = true
  try {
    const res = await getTraceEvidence(traceId)
    if (res.code === 200) {
      evidenceTrace.value = res.data || { traceId, businessEvents: [], spans: [], timeline: [] }
    }
  } catch (error) {
    if (!isHandledRequestError(error)) {
      ElMessage.error('获取 TraceId 证据包失败')
    }
    evidenceErrorMessage.value = error instanceof Error ? error.message : '获取 TraceId 证据包失败'
    logPageError('获取 TraceId 证据包失败', error)
  } finally {
    evidenceLoading.value = false
  }
}

const openTraceEvidenceByTraceId = async (traceId?: string | null) => {
  const normalizedTraceId = (traceId || '').trim()
  if (!normalizedTraceId) {
    ElMessage.warning('当前慢点缺少 TraceId，无法查看证据包')
    return
  }
  await openTraceEvidence({ traceId: normalizedTraceId })
}

const handleJumpToMessageTrace = (row?: AuditLogRecord) => {
  const context = buildSystemDiagnosticContext(row)
  persistSystemContext(row)
  router.push({
    path: '/message-trace',
    query: buildDiagnosticRouteQuery(context)
  })
}

const handleJumpToAccessError = (row?: AuditLogRecord) => {
  const context = buildSystemDiagnosticContext(row)
  persistSystemContext(row)
  router.push({
    path: '/message-trace',
    query: {
      mode: 'access-error',
      ...buildDiagnosticRouteQuery(context),
      errorCode: row?.errorCode || searchForm.errorCode || undefined,
      exceptionClass: row?.exceptionClass || searchForm.exceptionClass || undefined
    }
  })
}

const openExportColumnSetting = () => {
  exportColumnDialogVisible.value = true
}

const handleExportColumnConfirm = (selectedKeys: string[]) => {
  selectedExportColumnKeys.value = selectedKeys
  saveCsvColumnSelection(exportColumnStorageKey.value, selectedKeys)
}

const getResolvedExportColumns = () => resolveCsvColumns(exportColumns, selectedExportColumnKeys.value)

const handleExportSelected = () => {
  downloadRowsAsCsv(`${pageTitle.value}-选中项.csv`, selectedRows.value, getResolvedExportColumns())
}

const handleExportCurrent = () => {
  downloadRowsAsCsv(`${pageTitle.value}-当前结果.csv`, tableData.value, getResolvedExportColumns())
}

const handleToolbarAction = (command: string | number | object) => {
  switch (command) {
    case 'export-config':
      openExportColumnSetting()
      break
    case 'export-selected':
      handleExportSelected()
      break
    case 'export-current':
      handleExportCurrent()
      break
    case 'clear-selection':
      clearSelection()
      break
    default:
      break
  }
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  setPageSize(size)
  getAuditLogList()
}

// 当前页变化
const handlePageChange = (page: number) => {
  setPageNum(page)
  getAuditLogList()
}

const getAuditDirectActions = (row: AuditLogRecord) => {
  if (isSystemMode.value) {
    return [
      { command: 'detail', label: '详情' },
      { command: 'evidence', label: '证据', disabled: !canOpenTraceEvidence(row) },
      { command: 'trace', label: '追踪', disabled: !canJumpToMessageTrace(row) },
      { command: 'delete', label: '删除', permission: 'system:audit:delete' }
    ]
  }

  return [
    { command: 'detail', label: '详情' },
    { command: 'delete', label: '删除', permission: 'system:audit:delete' }
  ]
}

const handleAuditRowAction = (command: string | number | object, row: AuditLogRecord) => {
  if (command === 'detail') {
    void handleDetail(row)
    return
  }
  if (command === 'trace') {
    if (!canJumpToMessageTrace(row)) {
      return
    }
    handleJumpToMessageTrace(row)
    return
  }
  if (command === 'evidence') {
    void openTraceEvidence(row)
    return
  }
  if (command === 'delete') {
    void handleDelete(row)
  }
}

// 查看详情
const handleDetail = async (row: AuditLogRecord) => {
  if (row.id === undefined || row.id === null || row.id === '') {
    ElMessage.warning('当前日志缺少主键，无法查看详情')
    return
  }

  detailVisible.value = true
  detailLoading.value = true
  detailErrorMessage.value = ''
  detailData.value = { ...row }
  try {
    const res = await getAuditLogById(String(row.id))
    if (!res.data || Array.isArray(res.data)) {
      ElMessage.warning(`${recordLabel.value}不存在或已删除`)
      detailVisible.value = false
      return
    }
    detailData.value = { ...row, ...res.data }
  } catch (error) {
    if (!isHandledRequestError(error)) {
      ElMessage.error(`获取${detailDialogTitle.value}失败`)
    }
    detailErrorMessage.value = error instanceof Error ? error.message : `获取${detailDialogTitle.value}失败`
    logPageError('获取日志详情失败', error)
  } finally {
    detailLoading.value = false
  }
}

// 删除
const handleDelete = async (row: AuditLogRecord) => {
  try {
    await confirmAction({
      title: `删除${recordLabel.value}`,
      message: `确认删除当前${recordLabel.value}吗？删除后不可恢复。`,
      type: 'warning',
      confirmButtonText: '确认删除'
    })
    const res = await deleteAuditLog(String(row.id))
    if (res.code === 200) {
      ElMessage.success('删除成功')
      getAuditLogList()
      getAuditLogStats()
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    logPageError('删除失败', error)
  }
}

// 获取操作类型名称
const getOperationTypeName = (type: string) => {
  const map: Record<string, string> = {
    insert: '新增',
    update: '修改',
    delete: '删除',
    select: '查询',
    system_error: '系统异常'
  }
  return map[type] || type
}

// 获取操作类型标签
const getOperationTypeTag = (type: string) => {
  const map: Record<string, string> = {
    insert: 'primary',
    update: 'warning',
    delete: 'danger',
    select: 'info',
    system_error: 'danger'
  }
  return map[type] || 'info'
}

const getOperationResultName = (result?: number | null) => {
  if (result === 1) return '成功'
  if (result === 0) return '失败'
  return '-'
}

const getOperationResultTag = (result?: number | null) => {
  if (result === 1) return 'success'
  if (result === 0) return 'danger'
  return 'info'
}

const formatDuration = (durationMs?: number | null) => {
  if (durationMs === undefined || durationMs === null) {
    return '--'
  }
  return `${durationMs} ms`
}

const formatCount = (value?: number | null) => {
  if (value === undefined || value === null) {
    return '0'
  }
  return String(value)
}

const formatOptionalCount = (value?: number | null) => {
  if (value === undefined || value === null) {
    return '--'
  }
  return String(value)
}

const formatSignedCount = (value?: number | null) => {
  if (value === undefined || value === null) {
    return '--'
  }
  if (value > 0) {
    return `+${value}`
  }
  return String(value)
}

const formatRetentionDays = (value?: number | null) => {
  if (value === undefined || value === null) {
    return '--'
  }
  return `${value} 天`
}

const formatPercentage = (value?: number | null) => {
  if (value === undefined || value === null) {
    return '--'
  }
  return `${value}%`
}

const formatScheduledTaskName = (row: ObservabilityScheduledTask) =>
  formatValue(row.taskName || row.taskCode)

const formatScheduledTaskTrigger = (row: ObservabilityScheduledTask) =>
  formatValue(row.triggerExpression || row.initialDelayExpression)

const formatArchiveBatchName = (row?: Partial<ObservabilityMessageArchiveBatch> | null) =>
  formatValue(row?.batchNo || row?.sourceTable)

const formatArchiveBatchFooter = (row: ObservabilityMessageArchiveBatch) => {
  const failedReason = String(row.failedReason || '').trim()
  if (failedReason) {
    return `失败原因：${failedReason}`
  }
  const confirmReportPath = String(row.confirmReportPath || '').trim()
  return confirmReportPath ? `确认报告：${confirmReportPath}` : '确认报告：--'
}

const formatArchiveBatchReportSummaryLabel = (key: string) => {
  const map: Record<string, string> = {
    generatedAt: '报告生成时间',
    mode: '治理模式',
    expiredRows: '过期行数',
    deletedRows: '删除行数',
    tablesWithExpiredRows: '命中过期表'
  }
  return map[key] || key
}

const formatArchiveBatchReportPreviewReason = (preview?: Partial<ObservabilityMessageArchiveBatchReportPreview> | null) =>
  formatValue(preview?.reasonMessage || preview?.reasonCode || '当前确认报告暂不可预览')

const formatArchiveBatchCompareStatus = (status?: ArchiveBatchCompareStatus | string | null) => {
  switch (String(status || '').trim().toUpperCase()) {
    case 'MATCHED':
      return '已对齐'
    case 'DRIFTED':
      return '有偏差'
    case 'PARTIAL':
      return '部分可比'
    default:
      return '不可比对'
  }
}

const formatArchiveBatchCompareMessage = (compare?: Partial<ObservabilityMessageArchiveBatchCompare> | null) =>
  formatValue(compare?.compareMessage || messageArchiveBatchCompareHeadline.value)

const formatArchiveBatchCompareRowStatus = (matched?: boolean | null) => {
  if (matched === true) {
    return '已对齐'
  }
  if (matched === false) {
    return '有偏差'
  }
  return '部分可比'
}

const isArchiveBatchAbnormalStatus = (status?: string | null) => {
  const normalized = String(status || '').trim().toUpperCase()
  return normalized === 'DRIFTED' || normalized === 'PARTIAL' || normalized === 'UNAVAILABLE'
}

const resolveArchiveBatchCompareStatusClass = (row?: Partial<ObservabilityMessageArchiveBatch> | null) => {
  const normalized = String(row?.compareStatus || '').trim().toUpperCase()
  if (normalized === 'MATCHED' || normalized === 'DRIFTED' || normalized === 'PARTIAL') {
    return `is-${normalized.toLowerCase()}`
  }
  return 'is-unavailable'
}

const formatArchiveBatchPreviewAvailability = (row?: Partial<ObservabilityMessageArchiveBatch> | null) => {
  if (row?.previewAvailable) {
    return '可预览'
  }
  return formatValue(row?.previewReasonCode || '不可预览')
}

const formatSlowSummaryTitle = (row: ObservabilitySlowSpanSummary) =>
  [row.spanType, row.domainCode].map(formatValue).filter((value) => value !== '--').join(' / ') || '--'

const formatSlowSummaryTarget = (row: ObservabilitySlowSpanSummary) =>
  [row.eventCode, row.objectType, row.objectId].map(formatValue).filter((value) => value !== '--').join(' / ') || '--'

const formatSlowTrendBucketLabel = (row: ObservabilitySlowSpanTrend) => {
  if (row.bucket === 'DAY' && row.bucketStart) {
    return String(row.bucketStart).slice(0, 10)
  }
  return formatValue(row.bucketStart)
}

const getEvidenceItemTypeName = (type?: string | null) => {
  if (type === 'BUSINESS_EVENT') {
    return '业务事件'
  }
  if (type === 'SPAN') {
    return '调用片段'
  }
  return formatValue(type)
}

const normalizeArchiveBatchDetailValue = (value: unknown) => {
  if (value === undefined || value === null) {
    return '--'
  }
  if (typeof value === 'string') {
    const normalized = value.trim()
    return normalized || '--'
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  try {
    return JSON.stringify(value)
  } catch (_error) {
    return String(value)
  }
}

const parseArchiveBatchArtifacts = (artifactsJson?: string | null): ArchiveBatchDetailItem[] => {
  const raw = String(artifactsJson || '').trim()
  if (!raw) {
    return []
  }
  try {
    const parsed = JSON.parse(raw) as unknown
    if (Array.isArray(parsed)) {
      return parsed.map((item, index) => ({
        label: `item[${index}]`,
        value: normalizeArchiveBatchDetailValue(item)
      }))
    }
    if (parsed && typeof parsed === 'object') {
      return Object.entries(parsed as Record<string, unknown>).map(([key, value]) => ({
        label: key,
        value: normalizeArchiveBatchDetailValue(value)
      }))
    }
    return [{ label: 'value', value: normalizeArchiveBatchDetailValue(parsed) }]
  } catch (_error) {
    return [{ label: 'raw', value: raw }]
  }
}

const openMessageArchiveBatchDetail = async (row: ObservabilityMessageArchiveBatch) => {
  activeMessageArchiveBatch.value = row
  messageArchiveBatchDrawerVisible.value = true
  await Promise.all([
    loadMessageArchiveBatchCompare(row),
    loadMessageArchiveBatchReportPreview(row)
  ])
}

watch(detailVisible, (visible) => {
  if (!visible) {
    detailData.value = {}
    detailLoading.value = false
    detailErrorMessage.value = ''
  }
})

watch(evidenceDrawerVisible, (visible) => {
  if (!visible) {
    evidenceTrace.value = null
    evidenceTraceId.value = ''
    evidenceLoading.value = false
    evidenceErrorMessage.value = ''
  }
})

watch(messageArchiveBatchDrawerVisible, (visible) => {
  if (!visible) {
    activeMessageArchiveBatch.value = null
    clearMessageArchiveBatchCompare()
    clearMessageArchiveBatchReportPreview()
  }
})
</script>

<style scoped>
.audit-log-view {
  min-width: 0;
}

.audit-log-archive-batch-ledger__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.audit-log-archive-batch-ledger__header-main {
  flex: 1;
  min-width: 18rem;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.audit-log-archive-batch-ledger__filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: 0.75rem;
  align-items: end;
}

.audit-log-archive-batch-ledger__filter-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.88rem;
  color: var(--el-text-color-secondary);
}

.audit-log-archive-batch-ledger__filter-field--checkbox {
  justify-content: space-between;
}

.audit-log-archive-batch-ledger__filter-field input,
.audit-log-archive-batch-ledger__filter-field select {
  width: 100%;
  min-height: 2.25rem;
  border: 1px solid var(--el-border-color);
  border-radius: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
}

.audit-log-archive-batch-ledger__checkbox {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  min-height: 2.25rem;
  color: var(--el-text-color-primary);
}

.audit-log-archive-batch-ledger__checkbox input {
  width: 1rem;
  height: 1rem;
}

.audit-log-archive-batch-ledger__filter-actions {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
  align-items: center;
}

.audit-log-archive-batch-ledger__overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.72rem;
}

.audit-log-archive-batch-ledger__overview-card {
  display: grid;
  gap: 0.35rem;
  min-width: 0;
  padding: 0.85rem 0.95rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 66%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 88%, transparent);
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.audit-log-archive-batch-ledger__overview-card.is-clickable {
  cursor: pointer;
}

.audit-log-archive-batch-ledger__overview-card.is-clickable:hover,
.audit-log-archive-batch-ledger__overview-card.is-clickable:focus-visible {
  border-color: color-mix(in srgb, var(--el-color-primary) 42%, var(--panel-border));
  background: color-mix(in srgb, var(--el-color-primary-light-9) 80%, var(--panel-bg));
}

.audit-log-archive-batch-ledger__overview-card.is-clickable:focus-visible {
  outline: none;
}

.audit-log-archive-batch-ledger__overview-card.is-active {
  border-color: color-mix(in srgb, var(--el-color-primary) 52%, var(--panel-border));
  background: color-mix(in srgb, var(--el-color-primary-light-9) 88%, var(--panel-bg));
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--el-color-primary) 18%, transparent);
}

.audit-log-archive-batch-ledger__overview-card span,
.audit-log-archive-batch-ledger__overview-card p {
  margin: 0;
  color: var(--text-caption);
  font-size: 0.78rem;
}

.audit-log-archive-batch-ledger__overview-card strong {
  overflow: hidden;
  color: var(--text-heading);
  font-size: 1rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-archive-batch-ledger__focus-hint {
  color: var(--el-color-warning-dark-2);
  font-size: 0.78rem;
}

.observability-archive-batch-preview {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.observability-archive-batch-compare {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.observability-archive-batch-compare__status {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.9rem;
  padding: 0.9rem 1rem;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 0.5rem;
  background: var(--el-fill-color-extra-light);
}

.observability-archive-batch-compare__status.is-matched {
  border-color: color-mix(in srgb, var(--el-color-success) 42%, var(--el-border-color-lighter));
  background: color-mix(in srgb, var(--el-color-success-light-9) 88%, white);
}

.observability-archive-batch-compare__status.is-drifted {
  border-color: color-mix(in srgb, var(--el-color-danger) 42%, var(--el-border-color-lighter));
  background: color-mix(in srgb, var(--el-color-danger-light-9) 88%, white);
}

.observability-archive-batch-compare__status.is-partial,
.observability-archive-batch-compare__status.is-unavailable {
  border-color: color-mix(in srgb, var(--el-color-warning) 38%, var(--el-border-color-lighter));
  background: color-mix(in srgb, var(--el-color-warning-light-9) 86%, white);
}

.observability-archive-batch-compare__status-copy {
  display: grid;
  gap: 0.35rem;
  min-width: 0;
}

.observability-archive-batch-compare__status-copy strong {
  color: var(--text-heading);
  font-size: 0.94rem;
}

.observability-archive-batch-compare__status-copy p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.84rem;
  line-height: 1.5;
}

.observability-archive-batch-compare__status > span {
  flex: 0 0 auto;
  color: var(--text-caption);
  font-size: 0.78rem;
}

.observability-archive-batch-preview__summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr));
  gap: 0.75rem;
}

.observability-archive-batch-preview__summary-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.85rem 0.95rem;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 0.5rem;
  background: var(--el-fill-color-extra-light);
}

.observability-archive-batch-preview__summary-item span {
  font-size: 0.82rem;
  color: var(--el-text-color-secondary);
}

.observability-archive-batch-preview__tables {
  display: grid;
  gap: 0.75rem;
}

.observability-archive-batch-preview__table {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.9rem 1rem;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 0.5rem;
}

.observability-archive-batch-preview__table-title,
.observability-archive-batch-preview__table-metrics,
.observability-archive-batch-preview__table-meta {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  align-items: center;
}

.observability-archive-batch-preview__table-title span,
.observability-archive-batch-preview__table-meta {
  color: var(--el-text-color-secondary);
  font-size: 0.88rem;
}

.observability-archive-batch-compare__tables {
  display: grid;
  gap: 0.75rem;
}

.observability-archive-batch-compare__table {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.9rem 1rem;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 0.5rem;
  background: var(--el-bg-color);
}

.observability-archive-batch-compare__table.is-drifted {
  border-color: color-mix(in srgb, var(--el-color-danger) 42%, var(--el-border-color-lighter));
}

.observability-archive-batch-compare__table.is-partial {
  border-color: color-mix(in srgb, var(--el-color-warning) 42%, var(--el-border-color-lighter));
}

.observability-archive-batch-compare__table-title,
.observability-archive-batch-compare__table-metrics,
.observability-archive-batch-compare__table-meta {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  align-items: center;
}

.observability-archive-batch-compare__table-title strong {
  color: var(--text-heading);
  font-size: 0.92rem;
}

.observability-archive-batch-compare__table-title span,
.observability-archive-batch-compare__table-metrics,
.observability-archive-batch-compare__table-meta {
  color: var(--text-caption);
  font-size: 0.82rem;
}

.observability-archive-batch-preview__markdown {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.observability-archive-batch-preview__markdown-body {
  margin: 0;
  padding: 0.9rem 1rem;
  max-height: 18rem;
  overflow: auto;
  border-radius: 0.5rem;
  background: var(--el-fill-color-dark);
  color: var(--el-color-white);
  font-size: 0.82rem;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.audit-log-quick-search-tag {
  margin-top: 0.72rem;
}

.audit-log-quick-search-tag__chip {
  margin: 0;
}

.audit-log-table-wrap {
  min-width: 0;
}

.audit-log-mobile-list {
  display: none;
  margin-bottom: 0.72rem;
}

.audit-log-mobile-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.72rem;
}

.audit-log-mobile-card__heading {
  display: grid;
  gap: 0.2rem;
  min-width: 0;
}

.audit-log-mobile-card__title,
.audit-log-mobile-card__sub {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-mobile-card__title {
  color: var(--text-heading);
  font-size: 0.96rem;
}

.audit-log-mobile-card__sub {
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.audit-log-mobile-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.audit-log-mobile-card__info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.72rem;
}

.audit-log-mobile-card__field {
  min-width: 0;
}

.audit-log-mobile-card__field--full {
  grid-column: 1 / -1;
}

.audit-log-mobile-card__field .standard-mobile-record-card__field-value {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-table {
  display: block;
}

.audit-log-slow-summary {
  display: grid;
  gap: 0.82rem;
  margin-bottom: 0.82rem;
  padding: 1rem;
}

.audit-log-slow-summary__header,
.audit-log-slow-summary__title,
.audit-log-slow-summary__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  min-width: 0;
}

.audit-log-slow-summary__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.98rem;
  line-height: 1.3;
}

.audit-log-slow-summary__header > span,
.audit-log-slow-summary__title span,
.audit-log-slow-summary__metrics,
.audit-log-slow-summary__footer > span,
.audit-log-slow-summary__empty {
  color: var(--text-caption);
  font-size: 0.78rem;
}

.audit-log-slow-summary__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.72rem;
}

.audit-log-slow-summary__item {
  display: grid;
  gap: 0.55rem;
  min-width: 0;
  padding: 0.82rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 72%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 90%, transparent);
}

.audit-log-slow-summary__title strong,
.audit-log-slow-summary__item p,
.audit-log-slow-summary__footer > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-slow-summary__title strong {
  color: var(--text-heading);
  font-size: 0.92rem;
}

.audit-log-slow-summary__item p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.86rem;
}

.audit-log-slow-summary__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.audit-log-slow-span-drilldown {
  display: grid;
  gap: 0.72rem;
  min-width: 0;
  padding-top: 0.82rem;
  border-top: 1px solid color-mix(in srgb, var(--panel-border) 72%, transparent);
}

.audit-log-slow-span-drilldown__header,
.audit-log-slow-span-drilldown__title,
.audit-log-slow-span-drilldown__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  min-width: 0;
}

.audit-log-slow-span-drilldown__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.94rem;
  line-height: 1.3;
}

.audit-log-slow-span-drilldown__header p {
  margin: 0.25rem 0 0;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 0.82rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-slow-span-drilldown__header > span,
.audit-log-slow-span-drilldown__meta,
.audit-log-slow-span-drilldown__footer > span {
  color: var(--text-caption);
  font-size: 0.78rem;
}

.audit-log-slow-span-drilldown__list {
  display: grid;
  gap: 0.62rem;
}

.audit-log-slow-span-drilldown__item {
  display: grid;
  gap: 0.45rem;
  min-width: 0;
  padding: 0.72rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 66%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 86%, transparent);
}

.audit-log-slow-span-drilldown__title strong,
.audit-log-slow-span-drilldown__footer > span {
  overflow: hidden;
  color: var(--text-heading);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-slow-span-drilldown__title span {
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.audit-log-slow-span-drilldown__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.audit-log-slow-trend-drilldown {
  display: grid;
  gap: 0.72rem;
  min-width: 0;
  padding-top: 0.82rem;
  border-top: 1px solid color-mix(in srgb, var(--panel-border) 72%, transparent);
}

.audit-log-slow-trend-drilldown__header,
.audit-log-slow-trend-drilldown__title,
.audit-log-slow-trend-drilldown__footer,
.audit-log-slow-trend-drilldown__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  min-width: 0;
}

.audit-log-slow-trend-drilldown__header {
  align-items: flex-start;
}

.audit-log-slow-trend-drilldown__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.94rem;
  line-height: 1.3;
}

.audit-log-slow-trend-drilldown__header p {
  margin: 0.25rem 0 0;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 0.82rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-slow-trend-drilldown__actions {
  flex: 0 0 auto;
  flex-direction: column;
  align-items: flex-end;
}

.audit-log-slow-trend-drilldown__actions > span,
.audit-log-slow-trend-drilldown__metrics,
.audit-log-slow-trend-drilldown__footer > span {
  color: var(--text-caption);
  font-size: 0.78rem;
}

.audit-log-slow-trend-drilldown__list {
  display: grid;
  gap: 0.62rem;
}

.audit-log-slow-trend-drilldown__item {
  display: grid;
  gap: 0.45rem;
  min-width: 0;
  padding: 0.72rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 66%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 86%, transparent);
}

.audit-log-slow-trend-drilldown__title strong,
.audit-log-slow-trend-drilldown__footer > span {
  overflow: hidden;
  color: var(--text-heading);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-slow-trend-drilldown__title span {
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.audit-log-slow-trend-drilldown__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.audit-log-scheduled-task-ledger {
  display: grid;
  gap: 0.82rem;
  margin-top: 0.88rem;
  padding: 1rem;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: var(--panel-bg);
}

.audit-log-scheduled-task-ledger__header,
.audit-log-scheduled-task-ledger__title,
.audit-log-scheduled-task-ledger__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  min-width: 0;
}

.audit-log-scheduled-task-ledger__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.98rem;
  line-height: 1.3;
}

.audit-log-scheduled-task-ledger__header > span,
.audit-log-scheduled-task-ledger__meta,
.audit-log-scheduled-task-ledger__footer > span {
  color: var(--text-caption);
  font-size: 0.78rem;
}

.audit-log-scheduled-task-ledger__list {
  display: grid;
  gap: 0.62rem;
}

.audit-log-scheduled-task-ledger__item {
  display: grid;
  gap: 0.45rem;
  min-width: 0;
  padding: 0.72rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 66%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 86%, transparent);
}

.audit-log-scheduled-task-ledger__title strong,
.audit-log-scheduled-task-ledger__footer > span {
  overflow: hidden;
  color: var(--text-heading);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-scheduled-task-ledger__title span {
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.audit-log-scheduled-task-ledger__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.audit-log-archive-batch-ledger {
  display: grid;
  gap: 0.82rem;
  margin-top: 0.88rem;
  padding: 1rem;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: var(--panel-bg);
}

.audit-log-archive-batch-ledger__header,
.audit-log-archive-batch-ledger__title,
.audit-log-archive-batch-ledger__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  min-width: 0;
}

.audit-log-archive-batch-ledger__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.98rem;
  line-height: 1.3;
}

.audit-log-archive-batch-ledger__header > span,
.audit-log-archive-batch-ledger__meta,
.audit-log-archive-batch-ledger__metrics,
.audit-log-archive-batch-ledger__footer > span {
  color: var(--text-caption);
  font-size: 0.78rem;
}

.audit-log-archive-batch-ledger__list {
  display: grid;
  gap: 0.62rem;
}

.audit-log-archive-batch-ledger__item {
  display: grid;
  gap: 0.45rem;
  min-width: 0;
  padding: 0.72rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 66%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 86%, transparent);
}

.audit-log-archive-batch-ledger__item.is-drifted {
  border-color: color-mix(in srgb, var(--el-color-danger) 42%, var(--panel-border));
}

.audit-log-archive-batch-ledger__item.is-partial,
.audit-log-archive-batch-ledger__item.is-unavailable {
  border-color: color-mix(in srgb, var(--el-color-warning) 42%, var(--panel-border));
}

.audit-log-archive-batch-ledger__title strong,
.audit-log-archive-batch-ledger__footer > span {
  overflow: hidden;
  color: var(--text-heading);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-archive-batch-ledger__title span {
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.audit-log-archive-batch-ledger__meta,
.audit-log-archive-batch-ledger__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.audit-log-archive-batch-ledger__insights {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.observability-evidence-drawer {
  display: grid;
  gap: 1rem;
}

.observability-archive-batch-drawer {
  display: grid;
  gap: 1rem;
}

.observability-archive-batch-kv {
  display: grid;
  gap: 0.62rem;
  margin: 0;
}

.observability-archive-batch-kv__item {
  display: grid;
  gap: 0.24rem;
  min-width: 0;
  padding: 0.75rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 72%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 86%, transparent);
}

.observability-archive-batch-kv__item dt {
  color: var(--text-caption);
  font-size: 0.78rem;
}

.observability-archive-batch-kv__item dd {
  margin: 0;
  overflow: hidden;
  color: var(--text-heading);
  font-size: 0.86rem;
  line-height: 1.5;
  text-overflow: ellipsis;
  word-break: break-all;
}

.observability-evidence-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.72rem;
}

.observability-evidence-summary__item,
.observability-evidence-section {
  min-width: 0;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 94%, transparent);
}

.observability-evidence-summary__item {
  display: grid;
  gap: 0.28rem;
  padding: 0.82rem 0.9rem;
}

.observability-evidence-summary__item span,
.observability-evidence-record small,
.observability-evidence-timeline__meta,
.observability-evidence-timeline__type {
  color: var(--text-caption);
  font-size: 0.78rem;
}

.observability-evidence-summary__item strong {
  overflow: hidden;
  color: var(--text-heading);
  font-size: 0.98rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.observability-evidence-section {
  padding: 1rem;
}

.observability-evidence-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.82rem;
}

.observability-evidence-section__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.98rem;
  line-height: 1.3;
}

.observability-evidence-timeline {
  display: grid;
  gap: 0.72rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.observability-evidence-timeline__item {
  display: grid;
  grid-template-columns: 4.8rem minmax(0, 1fr);
  gap: 0.75rem;
  min-width: 0;
}

.observability-evidence-timeline__type {
  padding-top: 0.1rem;
}

.observability-evidence-timeline__body,
.observability-evidence-record {
  min-width: 0;
  padding: 0.75rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 72%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 86%, transparent);
}

.observability-evidence-timeline__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  min-width: 0;
}

.observability-evidence-timeline__title strong,
.observability-evidence-record strong {
  overflow: hidden;
  color: var(--text-heading);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.observability-evidence-timeline__title span {
  flex: 0 0 auto;
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.observability-evidence-timeline__body p {
  margin: 0.34rem 0 0;
  color: var(--text-secondary);
  line-height: 1.5;
}

.observability-evidence-timeline__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-top: 0.45rem;
}

.observability-evidence-split {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.observability-evidence-record {
  display: grid;
  gap: 0.25rem;
}

.observability-evidence-record + .observability-evidence-record {
  margin-top: 0.62rem;
}

.observability-evidence-record span {
  overflow: hidden;
  color: var(--text-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.observability-evidence-empty {
  color: var(--text-caption);
  font-size: 0.86rem;
}

@media (max-width: 640px) {
  .audit-log-mobile-list {
    display: block;
  }

  .audit-log-table {
    display: none;
  }

  .audit-log-slow-summary__grid {
    grid-template-columns: 1fr;
  }

  .audit-log-archive-batch-ledger__overview {
    grid-template-columns: 1fr;
  }

  .audit-log-mobile-card__info {
    grid-template-columns: 1fr;
  }

  .observability-evidence-summary,
  .observability-evidence-split,
  .observability-evidence-timeline__item {
    grid-template-columns: 1fr;
  }
}

</style>
