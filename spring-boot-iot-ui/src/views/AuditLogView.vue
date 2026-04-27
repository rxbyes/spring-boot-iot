<template>
  <StandardPageShell class="page-stack audit-log-view" :show-title="false">
    <StandardWorkbenchPanel
      :title="panelTitle"
      :description="pageDescription"
      :show-filters="isBusinessMode"
      :show-applied-filters="isBusinessMode && hasAppliedFilters"
      :show-toolbar="isBusinessMode"
      :show-inline-state="false"
      :show-pagination="isBusinessMode"
    >
      <template v-if="isBusinessMode" #filters>
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
              <el-select v-model="searchForm.operationType" placeholder="鎿嶄綔绫诲瀷" clearable>
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
                :placeholder="isSystemMode ? '寮傚父妯″潡' : '鎿嶄綔妯″潡'"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.operationResult" placeholder="鎿嶄綔缁撴灉" clearable>
                <el-option label="鎴愬姛" :value="1" />
                <el-option label="澶辫触" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="isSystemMode">
              <el-select v-model="searchForm.requestMethod" :placeholder="isSystemMode ? '璇锋眰閫氶亾' : '璇锋眰鏂规硶'" clearable>
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
                placeholder="鐩爣 / URL"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <template v-if="isSystemMode">
              <el-form-item>
                <el-input
                  v-model="searchForm.deviceCode"
                  placeholder="璁惧缂栫爜"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="searchForm.productKey"
                  placeholder="浜у搧鏍囪瘑"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="searchForm.errorCode"
                  placeholder="寮傚父缂栫爜"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="searchForm.exceptionClass"
                  placeholder="寮傚父绫诲瀷"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
            </template>
          </template>
          <template #actions>
            <StandardButton action="query" @click="handleSearch">鏌ヨ</StandardButton>
            <StandardButton action="reset" @click="handleReset">閲嶇疆</StandardButton>
          </template>
        </StandardListFilterHeader>
        <div v-if="appliedQuickSearchValue" class="audit-log-quick-search-tag">
          <el-tag closable class="audit-log-quick-search-tag__chip" @close="handleClearQuickSearch">
            蹇€熸悳绱細{{ appliedQuickSearchValue }}
          </el-tag>
        </div>
      </template>

      <template v-if="isBusinessMode" #applied-filters>
        <StandardAppliedFiltersBar
          :tags="activeFilterTags"
          @remove="handleRemoveAppliedFilter"
          @clear="handleClearAppliedFilters"
        />
      </template>

      <template v-if="isBusinessMode" #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="businessToolbarMetaItems"
        >
          <template #right>
            <StandardButton action="refresh" link @click="handleRefresh">鍒锋柊鍒楄〃</StandardButton>
            <StandardActionMenu
              label="鏇村鎿嶄綔"
              :items="auditToolbarActions"
              @command="handleToolbarAction"
            />
          </template>
        </StandardTableToolbar>
      </template>
      <div v-if="isSystemMode" class="audit-log-system-workbench">
        <section class="audit-log-system-header standard-list-surface">
          <div class="audit-log-system-header__summary">
            <article
              v-for="item in systemWorkbenchSummaryCards"
              :key="item.key"
              class="audit-log-system-header__summary-card"
              :class="[
                `audit-log-system-header__summary-card--${item.tone}`,
                { 'audit-log-system-header__summary-card--emphasis': item.emphasis }
              ]"
              :data-testid="`system-log-summary-${item.key}`"
            >
              <span class="audit-log-system-header__summary-label">{{ item.label }}</span>
              <strong class="audit-log-system-header__summary-value">{{ item.value }}</strong>
              <span class="audit-log-system-header__summary-meta">{{ item.meta }}</span>
            </article>
          </div>
          <div class="audit-log-system-header__actions">
            <StandardButton action="refresh" link @click="handleSystemTabRefresh">鍒锋柊鍒楄〃</StandardButton>
            <StandardActionMenu
              v-if="activeSystemLogTab === 'errors'"
              label="鏇村鎿嶄綔"
              :items="auditToolbarActions"
              @command="handleToolbarAction"
            />
          </div>
        </section>

        <div>
          <IotAccessTabWorkspace
            :model-value="activeSystemLogTab"
            :items="systemLogTabItems"
            default-key="errors"
            query-key="systemLogTab"
            :sync-query="false"
            variant="workbench"
            @update:model-value="handleSystemLogTabChange"
          >






          >
            <template #default="{ activeKey }">
              <AuditLogErrorTabPanel
                v-if="activeKey === 'errors'"
                :search-form="searchForm"
                :quick-search-keyword="quickSearchKeyword"
                :show-advanced-filters="showAdvancedFilters"
                :advanced-filter-hint="advancedFilterHint"
                :request-method-options="systemRequestMethodOptions"
                :applied-quick-search-value="appliedQuickSearchValue"
                :active-filter-tags="activeFilterTags"
                :has-applied-filters="hasAppliedFilters"
                :show-inline-state="showSystemInlineState"
                :inline-message="systemInlineMessage"
                :cluster-loading="clusterLoading"
                :cluster-error-message="clusterErrorMessage"
                :cluster-rows="clusterRows"
                :error-view-mode="errorViewMode"
                :cluster-context-summary="clusterContextSummary"
                :can-return-to-cluster-results="canReturnToClusterResults"
                :loading="loading"
                :table-data="tableData"
                :pagination="pagination"
                :audit-action-column-width="auditActionColumnWidth"
                :format-value="formatValue"
                :get-operation-result-name="getOperationResultName"
                :get-operation-result-tag="getOperationResultTag"
                :get-audit-direct-actions="getAuditDirectActions"
                @update:quick-search-keyword="quickSearchKeyword = $event"
                @update-search-field="handleSystemErrorSearchFieldUpdate"
                @search="handleSearch"
                @reset="handleReset"
                @quick-search="handleQuickSearch"
                @clear-quick-search="handleClearQuickSearch"
                @toggle-advanced="toggleAdvancedFilters"
                @clear-applied-filters="handleClearAppliedFilters"
                @remove-applied-filter="handleRemoveAppliedFilter"
                @open-clusters="handleOpenSystemErrorClusters"
                @return-to-details="handleReturnToSystemErrorDetails"
                @retry-clusters="handleRetrySystemErrorClusters"
                @apply-cluster="handleSystemErrorClusterApply"
                @clear-cluster-refiner="handleClearSystemErrorClusterRefiner"
                @return-to-clusters="handleReturnToSystemErrorClusters"
                @selection-change="handleSelectionChange"
                @audit-row-action="handleAuditPanelRowAction"
                @size-change="handleSizeChange"
                @page-change="handlePageChange"
              />

              <AuditLogHotspotTabPanel
                v-else-if="activeKey === 'hotspots'"
                :slow-summary-loading="slowSummaryLoading"
                :slow-summary-rows="slowSummaryRows"
                :slow-summary-error-message="slowSummaryErrorMessage"
                :format-slow-summary-title="formatSlowSummaryTitle"
                :format-slow-summary-target="formatSlowSummaryTarget"
                :format-value="formatValue"
                :format-duration="formatDuration"
                :format-count="formatCount"
                :active-slow-summary="resolvedActiveSlowSummary"
                :slow-span-loading="slowSpanLoading"
                :slow-span-total="slowSpanTotal"
                :slow-span-rows="slowSpanRows"
                :slow-span-error-message="slowSpanErrorMessage"
                :active-slow-trend-summary="resolvedActiveSlowTrendSummary"
                :selected-slow-summary-key="selectedSlowSummaryKey"
                :hotspot-drilldown-view="hotspotDrilldownView"
                :hotspot-drilldown-options="hotspotDrilldownOptions"
                :slow-trend-loading="slowTrendLoading"
                :slow-trend-rows="slowTrendRows"
                :slow-trend-error-message="slowTrendErrorMessage"
                :slow-trend-window="slowTrendWindow"
                :slow-trend-window-options="slowTrendWindowOptions"
                :default-slow-trend-window="defaultSlowTrendWindow"
                :format-slow-trend-bucket-label="formatSlowTrendBucketLabel"
                :format-percentage="formatPercentage"
                :scheduled-task-loading="scheduledTaskLoading"
                :scheduled-task-rows="scheduledTaskRows"
                :scheduled-task-total="scheduledTaskTotal"
                :scheduled-task-error-message="scheduledTaskErrorMessage"
                :format-scheduled-task-name="formatScheduledTaskName"
                :format-scheduled-task-trigger="formatScheduledTaskTrigger"
                @select-slow-summary="handleSlowSummarySelect"
                @change-hotspot-drilldown-view="handleHotspotDrilldownViewChange"
                @open-trace-evidence="openTraceEvidenceByTraceId"
                @open-slow-span-detail="loadSlowSpanDrilldown"
                @open-slow-trend="loadSlowTrendDrilldown"
                @change-slow-trend-window="handleSlowTrendWindowChange"
              />

              <AuditLogArchiveTabPanel
                v-else
                :loading="messageArchiveBatchLoading"
                :total="messageArchiveBatchTotal"
                :rows="messageArchiveBatchRows"
                :error-message="messageArchiveBatchErrorMessage"
                :overview-loading="messageArchiveBatchOverviewLoading"
                :overview-error-message="messageArchiveBatchOverviewErrorMessage"
                :filters="messageArchiveBatchFilters"
                :status-options="messageArchiveBatchStatusOptions"
                :compare-status-options="messageArchiveBatchCompareStatusOptions"
                :overview-cards="messageArchiveBatchOverviewCards"
                :latest-abnormal-focus="messageArchiveBatchLatestFocus"
                :active-row="selectedMessageArchiveBatchRow"
                :selected-batch-key="selectedMessageArchiveBatchKey"
                :format-value="formatValue"
                :format-count="formatCount"
                :format-optional-count="formatOptionalCount"
                :format-signed-count="formatSignedCount"
                :format-retention-days="formatRetentionDays"
                :format-archive-batch-name="formatArchiveBatchName"
                :format-archive-batch-compare-status="formatArchiveBatchCompareStatus"
                :format-archive-batch-preview-availability="formatArchiveBatchPreviewAvailability"
                :format-archive-batch-footer="formatArchiveBatchFooter"
                :resolve-archive-batch-compare-status-class="resolveArchiveBatchCompareStatusClass"
                :is-archive-batch-abnormal-status="isArchiveBatchAbnormalStatus"
                @update-filter="handleMessageArchiveBatchFilterUpdate"
                @search="handleMessageArchiveBatchSearch"
                @reset="resetMessageArchiveBatchFilters"
                @select-overview-card="handleMessageArchiveBatchOverviewClick"
                @select-latest-abnormal="handleMessageArchiveBatchLatestFocus"
                @select-row="handleMessageArchiveBatchRowSelect"
                @open-detail="openMessageArchiveBatchDetail"
              />
            </template>
          </IotAccessTabWorkspace>
        </div>
      </div>

      <template v-else>
        <div
          v-loading="loading"
          class="audit-log-table-wrap standard-list-surface"
          element-loading-text="姝ｅ湪鍒锋柊瀹¤鍒楄〃"
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
                      {{ formatValue(row.operationModule || row.userName) }}
                    </strong>
                    <span class="audit-log-mobile-card__sub">
                      {{ formatValue(row.userName) }}
                    </span>
                  </div>
                  <span class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item">
                    {{ getOperationResultName(row.operationResult) }}
                  </span>
                </div>

                <div class="audit-log-mobile-card__meta">
                  <span class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item">
                    {{ getOperationTypeName(row.operationType || '') }}
                  </span>
                  <span class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item">
                    {{ formatValue(row.requestMethod) }}
                  </span>
                  <span class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item">
                    {{ formatValue(row.ipAddress) }}
                  </span>
                </div>

                <div class="audit-log-mobile-card__info">
                  <div class="audit-log-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">鎿嶄綔妯″潡</span>
                    <strong class="standard-mobile-record-card__field-value">
                      {{ formatValue(row.operationModule) }}
                    </strong>
                  </div>
                  <div class="audit-log-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">鎿嶄綔鏂规硶</span>
                    <strong class="standard-mobile-record-card__field-value">
                      {{ formatValue(row.operationMethod) }}
                    </strong>
                  </div>
                  <div class="audit-log-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">鎿嶄綔鏃堕棿</span>
                    <strong class="standard-mobile-record-card__field-value">
                      {{ formatValue(row.operationTime) }}
                    </strong>
                  </div>
                  <div class="audit-log-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">鎿嶄綔缁撴灉</span>
                    <strong class="standard-mobile-record-card__field-value">
                      {{ getOperationResultName(row.operationResult) }}
                    </strong>
                  </div>
                  <div class="audit-log-mobile-card__field audit-log-mobile-card__field--full">
                    <span class="standard-mobile-record-card__field-label">璇锋眰鐩爣</span>
                    <strong class="standard-mobile-record-card__field-value">
                      {{ formatValue(row.requestUrl) }}
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
            <el-table-column prop="operationType" label="鎿嶄綔绫诲瀷" width="100">
              <template #default="{ row }">
                <el-tag :type="getOperationTypeTag(row.operationType)">
                  {{ getOperationTypeName(row.operationType) }}
                </el-tag>
              </template>
            </el-table-column>
            <StandardTableTextColumn prop="operationModule" label="鎿嶄綔妯″潡" :width="150" />
            <StandardTableTextColumn prop="operationMethod" label="鎿嶄綔鏂规硶" :min-width="180" />
            <StandardTableTextColumn prop="requestUrl" label="璇锋眰URL/鐩爣" :min-width="220" />
            <el-table-column prop="requestMethod" label="璇锋眰鏂规硶/閫氶亾" width="120" />
            <StandardTableTextColumn prop="userName" label="鎿嶄綔鐢ㄦ埛" :width="120" />
            <StandardTableTextColumn prop="ipAddress" label="鎿嶄綔IP" :width="150" />
            <StandardTableTextColumn prop="operationTime" label="鎿嶄綔鏃堕棿" :width="180" />
            <el-table-column prop="operationResult" label="鎿嶄綔缁撴灉" width="100">
              <template #default="{ row }">
                <el-tag :type="getOperationResultTag(row.operationResult)" round>
                  {{ getOperationResultName(row.operationResult) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              label="鎿嶄綔"
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
      </template>

      <template v-if="isBusinessMode" #pagination>
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
              <h3>涓氬姟浜嬩欢</h3>
            </header>
            <div v-if="evidenceBusinessEvents.length === 0" class="observability-evidence-empty">鏆傛棤涓氬姟浜嬩欢</div>
            <div
              v-for="event in evidenceBusinessEvents"
              :key="`event-${event.id || event.eventCode}`"
              class="observability-evidence-record"
            >
              <strong>{{ formatValue(event.eventCode) }}</strong>
              <span>{{ formatValue(event.eventName) }}</span>
              <small>{{ formatValue(event.domainCode) }} 路 {{ formatValue(event.resultStatus) }}</small>
            </div>
          </article>

          <article class="observability-evidence-section">
            <header class="observability-evidence-section__header">
              <h3>璋冪敤鐗囨</h3>
            </header>
            <div v-if="evidenceSpans.length === 0" class="observability-evidence-empty">鏆傛棤璋冪敤鐗囨</div>
            <div
              v-for="span in evidenceSpans"
              :key="`span-${span.id || span.spanType}`"
              class="observability-evidence-record"
            >
              <strong>{{ formatValue(span.spanType) }}</strong>
              <span>{{ formatValue(span.spanName) }}</span>
              <small>{{ formatDuration(span.durationMs) }} 路 {{ formatValue(span.status) }}</small>
            </div>
          </article>
        </section>
      </div>
    </StandardDetailDrawer>

    <StandardDetailDrawer
      v-model="messageArchiveBatchDrawerVisible"
      title="褰掓。鎵规璇︽儏"
      :subtitle="messageArchiveBatchDrawerSubtitle"
      :empty="!activeMessageArchiveBatch"
      empty-text="褰撳墠鏈€夋嫨褰掓。鎵规"
      size="56rem"
      tag-layout="title-inline"
      :tags="messageArchiveBatchDrawerTags"
    >
      <div class="observability-archive-batch-drawer">
        <section class="observability-evidence-summary" aria-label="褰掓。鎵规鎽樿">
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
              <h3>鎵规缁撴灉</h3>
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
              <h3>纭鎶ュ憡</h3>
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
            <h3>闄勫姞浜х墿</h3>
          </header>
          <div v-if="messageArchiveBatchArtifacts.length === 0" class="observability-evidence-empty">
            鏆傛棤闄勫姞浜х墿
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
            <h3>鎵规瀵规瘮</h3>
          </header>
          <div v-if="messageArchiveBatchCompareLoading" class="observability-evidence-empty">
            姝ｅ湪鍔犺浇鎵规瀵规瘮
          </div>
          <div v-else-if="messageArchiveBatchCompareErrorMessage" class="observability-evidence-empty">
            {{ messageArchiveBatchCompareErrorMessage }}
          </div>
          <div v-else-if="!activeMessageArchiveBatchCompare" class="observability-evidence-empty">
            鏆傛棤鎵规瀵规瘮
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
              鏆傛棤鍒嗚〃瀵规瘮
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
                  <span>dry-run 杩囨湡 {{ formatOptionalCount(item.dryRunExpiredRows) }}</span>
                  <span>apply 褰掓。 {{ formatOptionalCount(item.applyArchivedRows) }}</span>
                  <span>apply 鍒犻櫎 {{ formatOptionalCount(item.applyDeletedRows) }}</span>
                  <span>鍓╀綑 {{ formatOptionalCount(item.applyRemainingExpiredRows) }}</span>
                </div>
                <div class="observability-archive-batch-compare__table-meta">
                  <span>{{ formatValue(item.tableName) }}</span>
                  <span>宸€?{{ formatOptionalCount(item.deltaDryRunVsDeleted) }}</span>
                  <span v-if="item.reason">{{ item.reason }}</span>
                </div>
              </article>
            </div>
          </div>
        </section>

        <section class="observability-evidence-section">
          <header class="observability-evidence-section__header">
            <h3>纭鎶ュ憡棰勮</h3>
          </header>
          <div v-if="messageArchiveBatchReportPreviewLoading" class="observability-evidence-empty">
            姝ｅ湪鍔犺浇纭鎶ュ憡棰勮
          </div>
          <div v-else-if="messageArchiveBatchReportPreviewErrorMessage" class="observability-evidence-empty">
            {{ messageArchiveBatchReportPreviewErrorMessage }}
          </div>
          <div v-else-if="!activeMessageArchiveBatchReportPreview" class="observability-evidence-empty">
            鏆傛棤纭鎶ュ憡棰勮
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
              鏆傛棤琛ㄧ骇鎽樿
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
                  <span>杩囨湡 {{ formatCount(item.expiredRows) }}</span>
                  <span>鍒犻櫎 {{ formatCount(item.deletedRows) }}</span>
                  <span>鍓╀綑 {{ formatCount(item.remainingExpiredRows) }}</span>
                </div>
                <div class="observability-archive-batch-preview__table-meta">
                  <span>淇濈暀 {{ formatRetentionDays(item.retentionDays) }}</span>
                  <span>鎴 {{ formatValue(item.cutoffAt) }}</span>
                  <span>绐楀彛 {{ formatValue(item.earliestRecordAt) }} - {{ formatValue(item.latestRecordAt) }}</span>
                </div>
              </article>
            </div>

            <article class="observability-archive-batch-preview__markdown">
              <header class="observability-evidence-section__header">
                <h3>Markdown 鎽樿</h3>
                <small v-if="activeMessageArchiveBatchReportPreview.markdownTruncated">
                  浠呭睍绀哄墠 80 琛?/ 6000 瀛楃
                </small>
              </header>
              <div
                v-if="!activeMessageArchiveBatchReportPreview.markdownAvailable || !activeMessageArchiveBatchReportPreview.markdownPreview"
                class="observability-evidence-empty"
              >
                褰撳墠浠呬繚鐣?JSON 鎽樿锛屾湭鐢熸垚 Markdown 棰勮
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
import {
  deleteAuditLog,
  getAuditLogById,
  getBusinessAuditStats,
  getSystemErrorStats,
  pageLogs,
  pageSystemErrorClusters,
  type AuditLogRecord,
  type SystemErrorClusterRow
} from '@/api/auditLog'
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
import AuditLogArchiveTabPanel from '@/components/auditLog/AuditLogArchiveTabPanel.vue'
import AuditLogErrorTabPanel from '@/components/auditLog/AuditLogErrorTabPanel.vue'
import AuditLogHotspotTabPanel from '@/components/auditLog/AuditLogHotspotTabPanel.vue'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import IotAccessTabWorkspace from '@/components/iotAccess/IotAccessTabWorkspace.vue'
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue'
import StandardActionMenu from '@/components/StandardActionMenu.vue'
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
type HotspotDrilldownView = 'samples' | 'trends' | 'tasks'
type SystemLogTabKey = 'errors' | 'hotspots' | 'archives'
type ErrorViewMode = 'detail' | 'clusters'
type SystemErrorSearchSnapshot = {
  traceId: string
  deviceCode: string
  productKey: string
  operationModule: string
  requestMethod: string
  requestUrl: string
  errorCode: string
  exceptionClass: string
  operationResult: number | undefined
}
type ArchiveBatchDetailItem = { label: string; value: string }
type ArchiveBatchCompareStatus = 'MATCHED' | 'DRIFTED' | 'PARTIAL' | 'UNAVAILABLE'
type ArchiveBatchOverviewSelectionKey = 'abnormal' | 'drifted' | 'remaining' | 'latest'
type SystemWorkbenchSummaryTone = 'neutral' | 'warning' | 'danger'
type SystemLogTabItem = {
  key: SystemLogTabKey
  label: string
  meta?: string
  testId: string
  buttonAttrs: Record<string, string>
  activeButtonAttrs: Record<string, string>
}
type SystemWorkbenchSummaryCard = {
  key: string
  label: string
  value: string
  meta: string
  tone: SystemWorkbenchSummaryTone
  emphasis?: boolean
}
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
const activeSystemLogTab = ref<SystemLogTabKey>('errors')
const systemLogTabItems = computed<SystemLogTabItem[]>(() => [
  {
    key: 'errors',
    label: '寮傚父鎺掓煡',
    testId: 'system-log-tab-errors',
    meta: '閺勫海绮忔稉搴ょ槈閹?',
    buttonAttrs: { 'data-testid': 'system-log-tab-errors', 'data-active': 'false' },
    activeButtonAttrs: { 'data-active': 'true' }
  },
  {
    key: 'hotspots',
    label: '瑙傛祴鐑偣',
    testId: 'system-log-tab-hotspots',
    meta: '閻戭厾鍋ｆ稉搴ょЪ閸?',
    buttonAttrs: { 'data-testid': 'system-log-tab-hotspots', 'data-active': 'false' },
    activeButtonAttrs: { 'data-active': 'true' }
  },
  {
    key: 'archives',
    label: '褰掓。娌荤悊',
    testId: 'system-log-tab-archives',
    meta: '閹佃顐兼稉搴☆嚠濮?',
    buttonAttrs: { 'data-testid': 'system-log-tab-archives', 'data-active': 'false' },
    activeButtonAttrs: { 'data-active': 'true' }
  }
])
const auditActionColumnWidth = computed(() =>
  resolveWorkbenchActionColumnWidth({
    directItems: isSystemMode.value
      ? [
          { command: 'detail', label: '璇︽儏' },
          { command: 'evidence', label: '璇佹嵁' },
          { command: 'trace', label: '杩借釜' },
          { command: 'copy-trace-id', label: '澶嶅埗 TraceId' },
          { command: 'copy-target', label: '澶嶅埗鐩爣' },
          { command: 'delete', label: '鍒犻櫎', permission: 'system:audit:delete' }
        ]
      : [
          { command: 'detail', label: '璇︽儏' },
          { command: 'delete', label: '鍒犻櫎', permission: 'system:audit:delete' }
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
const detailDialogTitle = computed(() => (isSystemMode.value ? '寮傚父璇︽儏' : `${pageTitle.value}璇︽儏`))
const exportDialogTitle = computed(() => (isSystemMode.value ? '异常观测台导出列设置' : pageTitle.value + '导出列设置'))
const recordLabel = computed(() => (isSystemMode.value ? '寮傚父璁板綍' : '瀹¤璁板綍'))
const businessOperationTypeOptions = [
  { label: '鏂板', value: 'insert' },
  { label: '淇敼', value: 'update' },
  { label: '鍒犻櫎', value: 'delete' },
  { label: '鏌ヨ', value: 'select' }
]
const systemRequestMethodOptions = [
  { label: 'MQTT', value: 'MQTT' },
  { label: 'SYSTEM', value: 'SYSTEM' },
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' }
]

// 鎼滅储琛ㄥ崟
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

// 鍒嗛〉

// 琛ㄦ牸鏁版嵁
const tableData = ref<AuditLogRecord[]>([])
const tableRef = ref()
const selectedRows = ref<AuditLogRecord[]>([])
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum, resetTotal } = useServerPagination()
const clusterRows = ref<SystemErrorClusterRow[]>([])
const clusterLoading = ref(false)
const clusterErrorMessage = ref('')
const errorViewMode = ref<ErrorViewMode>('detail')
const clusterQuerySignature = ref('')
const clusterQueryFormSnapshot = ref<SystemErrorSearchSnapshot | null>(null)
const clusterRefinerBaseSnapshot = ref<SystemErrorSearchSnapshot | null>(null)
const selectedClusterRefiner = ref<SystemErrorClusterRow | null>(null)
const clusterContextSummary = computed(() => {
  const cluster = selectedClusterRefiner.value
  if (!cluster) {
    return ''
  }
  return [cluster.operationModule, cluster.exceptionClass, cluster.errorCode]
    .map((value) => formatValue(value))
    .join(' / ')
})
const canReturnToClusterResults = computed(
  () => errorViewMode.value === 'detail' && Boolean(clusterQuerySignature.value) && clusterRows.value.length > 0
)
const exportColumns: CsvColumn<any>[] = [
  { key: 'operationType', label: '鎿嶄綔绫诲瀷', formatter: (value) => getOperationTypeName(String(value || '')) },
  { key: 'operationModule', label: '鎿嶄綔妯″潡' },
  { key: 'operationMethod', label: '鎿嶄綔鏂规硶' },
  { key: 'requestUrl', label: '璇锋眰URL' },
  { key: 'requestMethod', label: '璇锋眰鏂规硶' },
  { key: 'traceId', label: 'TraceId' },
  { key: 'deviceCode', label: '璁惧缂栫爜' },
  { key: 'productKey', label: '浜у搧鏍囪瘑' },
  { key: 'errorCode', label: '寮傚父缂栫爜' },
  { key: 'exceptionClass', label: '寮傚父绫诲瀷' },
  { key: 'userName', label: '鎿嶄綔鐢ㄦ埛' },
  { key: 'ipAddress', label: '鎿嶄綔IP' },
  { key: 'resultMessage', label: '缁撴灉娑堟伅' },
  { key: 'operationTime', label: '鎿嶄綔鏃堕棿' },
  { key: 'operationResult', label: '鎿嶄綔缁撴灉', formatter: (value) => (Number(value) === 1 ? '鎴愬姛' : '澶辫触') }
]
const exportColumnStorageKey = computed(() => (isSystemMode.value ? 'system-log-view' : 'business-log-view'))
const exportColumnOptions = toCsvColumnOptions(exportColumns)
const exportPresets = computed(() =>
  isSystemMode.value
    ? [
        { label: '榛樿妯℃澘', keys: ['operationModule', 'operationMethod', 'requestUrl', 'requestMethod', 'traceId', 'deviceCode', 'productKey', 'resultMessage', 'operationTime', 'operationResult'] },
        { label: '杩愮淮妯℃澘', keys: ['operationModule', 'requestUrl', 'requestMethod', 'deviceCode', 'productKey', 'resultMessage', 'operationTime'] },
        { label: '鐮斿彂妯℃澘', keys: ['operationModule', 'operationMethod', 'requestUrl', 'requestMethod', 'traceId', 'deviceCode', 'productKey', 'errorCode', 'exceptionClass', 'resultMessage', 'operationResult', 'operationTime'] }
      ]
    : [
        { label: '榛樿妯℃澘', keys: ['operationType', 'operationModule', 'operationMethod', 'requestUrl', 'requestMethod', 'userName', 'ipAddress', 'operationTime', 'operationResult'] },
        { label: '杩愮淮妯℃澘', keys: ['operationType', 'operationModule', 'requestMethod', 'userName', 'ipAddress', 'operationTime', 'operationResult'] },
        { label: '绠＄悊妯℃澘', keys: ['operationType', 'operationModule', 'operationMethod', 'userName', 'operationTime', 'operationResult'] }
      ]
)
const selectedExportColumnKeys = ref<string[]>([])
const exportColumnDialogVisible = ref(false)

// 鍔犺浇鐘舵€?
const loading = ref(false)
const statsLoading = ref(false)
let systemErrorClusterRequestToken = 0

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
const messageArchiveBatchSummarySelectionFilterContext = ref('')
const messageArchiveBatchFocusedBatchNo = ref('')
const messageArchiveBatchFocusHint = ref('')
const selectedMessageArchiveBatchKey = ref('')
const messageArchiveBatchFilters = reactive({
  batchNo: '',
  status: '',
  compareStatus: '',
  onlyAbnormal: false,
  dateFrom: '',
  dateTo: ''
})
const messageArchiveBatchStatusOptions = [
  { label: '鎴愬姛', value: 'SUCCEEDED' },
  { label: '澶辫触', value: 'FAILED' },
  { label: '运行中', value: 'RUNNING' }
]
const messageArchiveBatchCompareStatusOptions = [
  { label: '已对齐', value: 'MATCHED' },
  { label: '有偏差', value: 'DRIFTED' },
  { label: '閮ㄥ垎鍙瘮', value: 'PARTIAL' },
  { label: '不可用', value: 'UNAVAILABLE' }
]
const slowSummaryRows = ref<ObservabilitySlowSpanSummary[]>([])
const slowSummaryLoading = ref(false)
const slowSummaryErrorMessage = ref('')
const selectedSlowSummaryKey = ref('')
const activeSlowSummary = ref<ObservabilitySlowSpanSummary | null>(null)
const slowSpanRows = ref<ObservabilitySpan[]>([])
const slowSpanLoading = ref(false)
const slowSpanErrorMessage = ref('')
const slowSpanTotal = ref(0)
const hotspotDrilldownView = ref<HotspotDrilldownView>('samples')
const hotspotDrilldownOptions = [
  { label: '最近样本', value: 'samples' },
  { label: '瓒嬪娍', value: 'trends' },
  { label: '鐩稿叧浠诲姟', value: 'tasks' }
] as const
const defaultSlowTrendWindow: SlowTrendWindowKey = 'LAST_24_HOURS'
const slowTrendWindowOptions = [
  { label: '24灏忔椂', value: 'LAST_24_HOURS' },
  { label: '7天', value: 'LAST_7_DAYS' }
] as const
const slowTrendWindow = ref<SlowTrendWindowKey>(defaultSlowTrendWindow)
const activeSlowTrendSummary = ref<ObservabilitySlowSpanSummary | null>(null)
const slowTrendRows = ref<ObservabilitySlowSpanTrend[]>([])
const slowTrendLoading = ref(false)
const slowTrendErrorMessage = ref('')
const buildSlowSummaryKey = (row?: Partial<ObservabilitySlowSpanSummary> | null) =>
  [
    row?.spanType || 'span',
    row?.domainCode || 'domain',
    row?.eventCode || 'event',
    row?.objectType || 'object',
    row?.objectId || 'id'
  ].join('-')
const buildArchiveBatchKey = (row?: Partial<ObservabilityMessageArchiveBatch> | null) =>
  String(row?.batchNo || row?.id || row?.createTime || '')
const selectedMessageArchiveBatchRow = computed<ObservabilityMessageArchiveBatch | null>(() => {
  const matched = messageArchiveBatchRows.value.find(
    (row) => buildArchiveBatchKey(row) === selectedMessageArchiveBatchKey.value
  )
  return matched || messageArchiveBatchRows.value[0] || null
})
const resolvedActiveSlowSummary = computed<ObservabilitySlowSpanSummary | null>(() => {
  const matched = slowSummaryRows.value.find((row) => buildSlowSummaryKey(row) === selectedSlowSummaryKey.value)
  return matched || slowSummaryRows.value[0] || null
})
const resolvedActiveSlowTrendSummary = computed<ObservabilitySlowSpanSummary | null>(() => {
  const matched = slowSummaryRows.value.find(
    (row) => buildSlowSummaryKey(row) === buildSlowSummaryKey(activeSlowTrendSummary.value)
  )
  return matched || resolvedActiveSlowSummary.value
})
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
    { key: 'userName', label: '鎿嶄綔鐢ㄦ埛', isActive: (value) => isBusinessMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'operationType', label: (value) => `鎿嶄綔绫诲瀷锛?{getOperationTypeName(String(value || ''))}`, clearValue: undefined, isActive: (value) => isBusinessMode.value && value !== undefined },
    { key: 'traceId', label: 'TraceId', advanced: true },
    { key: 'operationModule', label: (value) => `${isSystemMode.value ? '寮傚父妯″潡' : '鎿嶄綔妯″潡'}锛?{String(value || '').trim()}` },
    { key: 'requestMethod', label: (value) => `璇锋眰閫氶亾锛?{String(value || '')}`, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'requestUrl', label: '鐩爣 / URL', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'deviceCode', label: '璁惧缂栫爜', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'productKey', label: '浜у搧鏍囪瘑', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'errorCode', label: '寮傚父缂栫爜', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'exceptionClass', label: '寮傚父绫诲瀷', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'operationResult', label: (value) => `鎿嶄綔缁撴灉锛?{getOperationResultName(Number(value))}`, clearValue: undefined, isActive: (value) => value !== undefined }
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
    label: '瀵煎嚭閫変腑',
    disabled: selectedRows.value.length === 0
  },
  {
    key: 'export-current',
    command: 'export-current',
    label: '瀵煎嚭褰撳墠缁撴灉',
    disabled: tableData.value.length === 0
  },
  {
    key: 'clear-selection',
    command: 'clear-selection',
    label: '娓呯┖閫変腑',
    disabled: selectedRows.value.length === 0
  }
])
const businessToolbarMetaItems = computed(() => [
  '已选 ' + selectedRows.value.length + ' 项',
  '审计 ' + businessStats.value.total,
  '成功 ' + businessStats.value.successCount,
  '失败 ' + businessStats.value.failureCount
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
        `鏉ヨ嚜${describeDiagnosticSource(restoredDiagnosticContext.value.sourcePage)}`,
        '当前节点：后台异常核对',
        '下一步可回链路追踪台或治理页继续排查。'
      ].join(' 路 ')
    : ''
)
const showSystemInlineState = computed(() => isSystemMode.value && Boolean(systemInlineMessage.value))

// 璇︽儏瀵硅瘽妗?
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
  { label: `浜嬩欢 ${evidenceBusinessEvents.value.length}`, type: 'primary' as const },
  { label: `鐗囨 ${evidenceSpans.value.length}`, type: 'info' as const }
])
const evidenceSummaryCards = computed(() => [
  { label: 'TraceId', value: formatValue(evidenceTrace.value?.traceId || evidenceTraceId.value) },
  { label: '涓氬姟浜嬩欢', value: String(evidenceBusinessEvents.value.length) },
  { label: '璋冪敤鐗囨', value: String(evidenceSpans.value.length) },
  { label: '时间线节点', value: String(evidenceTimeline.value.length) }
])
const messageArchiveBatchDrawerSubtitle = computed(() =>
  activeMessageArchiveBatch.value?.batchNo
    ? `鎵规鍙凤細${activeMessageArchiveBatch.value.batchNo}`
    : '鏌ョ湅娑堟伅鐑〃褰掓。鎵规鐨勭‘璁ゃ€佸綊妗ｄ笌鍒犻櫎缁撴灉'
)
const messageArchiveBatchDrawerTags = computed(() => {
  const row = activeMessageArchiveBatch.value
  return [
    { label: `鐘舵€?${formatValue(row?.status)}`, type: 'primary' as const },
    { label: `鏉ユ簮 ${formatValue(row?.sourceTable)}`, type: 'info' as const }
  ]
})
const messageArchiveBatchSummaryCards = computed(() => {
  const row = activeMessageArchiveBatch.value
  return [
    { label: '批次号', value: formatArchiveBatchName(row) },
    { label: '状态', value: formatValue(row?.status) },
    { label: '纭琛屾暟', value: formatCount(row?.confirmedExpiredRows) },
    { label: '鍒犻櫎琛屾暟', value: formatCount(row?.deletedRows) }
  ]
})
const messageArchiveBatchResultItems = computed<ArchiveBatchDetailItem[]>(() => {
  const row = activeMessageArchiveBatch.value
  return [
    { label: '来源表', value: formatValue(row?.sourceTable) },
    { label: '娌荤悊妯″紡', value: formatValue(row?.governanceMode) },
    { label: '保留期', value: formatRetentionDays(row?.retentionDays) },
    { label: '候选行数', value: formatCount(row?.candidateRows) },
    { label: '褰掓。琛屾暟', value: formatCount(row?.archivedRows) },
    { label: '鍒犻櫎琛屾暟', value: formatCount(row?.deletedRows) }
  ]
})
const messageArchiveBatchReportItems = computed<ArchiveBatchDetailItem[]>(() => {
  const row = activeMessageArchiveBatch.value
  return [
    { label: '纭鎶ュ憡', value: formatValue(row?.confirmReportPath) },
    { label: '鎶ュ憡鐢熸垚鏃堕棿', value: formatValue(row?.confirmReportGeneratedAt) },
    { label: '鎴鏃堕棿', value: formatValue(row?.cutoffAt) },
    { label: '澶辫触鍘熷洜', value: formatValue(row?.failedReason) }
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
      return '宸叉寜纭缁撴灉钀藉湴'
    case 'DRIFTED':
      return '执行结果与确认结果存在偏差'
    case 'PARTIAL':
      return '仅完成部分比对'
    default:
      return '褰撳墠缂哄皯鍙俊瀵规瘮璇佹嵁'
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
    { label: '纭鎶ュ憡', value: formatValue(sources.confirmReportPath) },
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
    { label: '纭杩囨湡', value: formatOptionalCount(summary.confirmedExpiredRows) },
    { label: 'dry-run 杩囨湡', value: formatOptionalCount(summary.dryRunExpiredRows) },
    { label: 'apply 褰掓。', value: formatOptionalCount(summary.applyArchivedRows) },
    { label: 'apply 鍒犻櫎', value: formatOptionalCount(summary.applyDeletedRows) },
    { label: '鍓╀綑杩囨湡', value: formatOptionalCount(summary.remainingExpiredRows) },
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
    { label: 'JSON 璺緞', value: formatValue(preview.resolvedJsonPath || preview.confirmReportPath) },
    {
      label: 'Markdown 璺緞',
      value: preview.markdownAvailable ? formatValue(preview.resolvedMarkdownPath) : '未生成'
    },
    { label: '鏂囦欢鏇存柊鏃堕棿', value: formatValue(preview.fileLastModifiedAt) },
    { label: '鎶ュ憡鐢熸垚鏃堕棿', value: formatValue(preview.confirmReportGeneratedAt) }
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
      label: '寮傚父鎵规',
      value: formatOptionalCount(overview?.abnormalBatches),
      meta: `鎬绘壒娆?${formatOptionalCount(overview?.totalBatches)}`,
      testId: 'archive-batch-overview-abnormal',
      clickable: true,
      active: activeMessageArchiveBatchOverviewSelection.value === 'abnormal'
    },
    {
      key: 'drifted',
      label: '鎵ц鍋忓樊鎬婚噺',
      value: formatSignedCount(overview?.totalDeltaConfirmedVsDeleted),
      meta: `宸插榻?${formatOptionalCount(overview?.matchedBatches)}`,
      testId: 'archive-batch-overview-drifted',
      clickable: true,
      active: activeMessageArchiveBatchOverviewSelection.value === 'drifted'
    },
    {
      key: 'remaining',
      label: '鍓╀綑杩囨湡鎬婚噺',
      value: formatOptionalCount(overview?.totalRemainingExpiredRows),
      meta: `閮ㄥ垎鍙瘮 ${formatOptionalCount(overview?.partialBatches)}`,
      testId: 'archive-batch-overview-remaining',
      clickable: true,
      active: activeMessageArchiveBatchOverviewSelection.value === 'remaining'
    }
  ]
})
const messageArchiveBatchLatestFocus = computed(() => {
  const latestBatch = String(messageArchiveBatchOverview.value?.latestAbnormalBatch || '').trim()
  if (!latestBatch) {
    return null
  }
  return {
    batchNo: latestBatch,
    occurredAt: formatValue(messageArchiveBatchOverview.value?.latestAbnormalOccurredAt),
    active: activeMessageArchiveBatchOverviewSelection.value === 'latest'
  }
})
const systemOverviewItems = computed(() => [
  {
    key: 'errors',
    label: '寮傚父',
    value: formatCount(systemStats.value.total),
    targetTab: 'errors' as SystemLogTabKey
  },
  {
    key: 'hotspots',
    label: '鎱㈢偣',
    value: formatCount(slowSummaryRows.value.length),
    targetTab: 'hotspots' as SystemLogTabKey
  },
  {
    key: 'tasks',
    label: '璋冨害',
    value: formatCount(scheduledTaskTotal.value),
    targetTab: 'hotspots' as SystemLogTabKey
  },
  {
    key: 'archives',
    label: '寮傚父鎵规',
    value: formatOptionalCount(messageArchiveBatchOverview.value?.abnormalBatches),
    targetTab: 'archives' as SystemLogTabKey
  }
])
const activeSystemOverviewItemKey = computed(() => {
  if (activeSystemLogTab.value === 'hotspots') {
    return 'hotspots'
  }
  return activeSystemLogTab.value
})
const systemToolbarMetaItems = computed<string[]>(() => [])

const systemWorkbenchSummaryCards = computed<SystemWorkbenchSummaryCard[]>(() => {
  if (activeSystemLogTab.value === 'hotspots') {
    return [
      {
        key: 'hotspots',
        label: '\u70ed\u70b9\u5bf9\u8c61',
        value: formatCount(slowSummaryRows.value.length),
        meta: '\u5f53\u524d\u5de5\u4f5c\u53f0\u5185\u7684\u70ed\u70b9\u4e3b\u5bf9\u8c61',
        tone: 'warning',
        emphasis: true
      },
      {
        key: 'tasks',
        label: '\u8c03\u5ea6\u53f0\u8d26',
        value: formatCount(scheduledTaskTotal.value),
        meta: '\u76f8\u5173\u4efb\u52a1\u53f0\u8d26',
        tone: 'neutral'
      },
      {
        key: 'samples',
        label: '\u6700\u8fd1\u6837\u672c',
        value: activeSlowSummary.value ? formatCount(slowSpanTotal.value) : '0',
        meta: activeSlowSummary.value ? '\u5f53\u524d\u70ed\u70b9\u7684\u6700\u65b0\u7247\u6bb5' : '\u5c1a\u672a\u9009\u4e2d\u70ed\u70b9',
        tone: 'neutral'
      },
      {
        key: 'trends',
        label: '\u8d8b\u52bf\u6876',
        value: activeSlowTrendSummary.value ? formatCount(slowTrendRows.value.length) : '0',
        meta: slowTrendWindow.value === 'LAST_7_DAYS' ? '7 \u5929\u89c2\u6d4b\u7a97\u53e3' : '24 \u5c0f\u65f6\u89c2\u6d4b\u7a97\u53e3',
        tone: 'neutral'
      }
    ]
  }
  if (activeSystemLogTab.value === 'archives') {
    return [
      {
        key: 'abnormal',
        label: '\u5f02\u5e38\u6279\u6b21',
        value: formatOptionalCount(messageArchiveBatchOverview.value?.abnormalBatches),
        meta: '\u9700\u4f18\u5148\u5904\u7406\u7684\u6279\u6b21\u6570',
        tone: 'danger',
        emphasis: true
      },
      {
        key: 'batches',
        label: '\u6279\u6b21\u603b\u6570',
        value: formatCount(messageArchiveBatchTotal.value),
        meta: '\u5f52\u6863\u53f0\u7684\u5f53\u524d\u7ed3\u679c',
        tone: 'neutral'
      },
      {
        key: 'drifted',
        label: '\u6267\u884c\u504f\u5dee',
        value: formatSignedCount(messageArchiveBatchOverview.value?.totalDeltaConfirmedVsDeleted),
        meta: 'dry-run \u4e0e apply \u7684\u5bf9\u6bd4\u7ed3\u679c',
        tone: 'warning'
      },
      {
        key: 'remaining',
        label: '\u5269\u4f59\u8fc7\u671f',
        value: formatOptionalCount(messageArchiveBatchOverview.value?.totalRemainingExpiredRows),
        meta: '\u4ecd\u5f85\u7ee7\u7eed\u6cbb\u7406\u7684\u70ed\u884c\u91cf',
        tone: 'neutral'
      }
    ]
  }
  return [
    {
      key: 'errors',
      label: '\u5f02\u5e38\u8bb0\u5f55',
      value: formatCount(systemStats.value.total),
      meta: '\u5f53\u524d\u7b5b\u67e5\u6761\u4ef6\u4e0b\u7684\u5f02\u5e38\u603b\u6570',
      tone: 'danger',
      emphasis: true
    },
    {
      key: 'today',
      label: '\u4eca\u65e5\u65b0\u589e',
      value: formatCount(systemStats.value.todayCount),
      meta: '\u5f53\u524d\u81ea\u7136\u65e5\u7684\u589e\u91cf\u53d8\u5316',
      tone: 'neutral'
    },
    {
      key: 'trace',
      label: 'Trace \u94fe\u8def',
      value: formatCount(systemStats.value.distinctTraceCount),
      meta: '\u53ef\u56de\u94fe\u7684\u6709\u6548\u94fe\u8def\u6570\u91cf',
      tone: 'neutral'
    },
    {
      key: 'selected',
      label: '\u5df2\u9009\u4e2d',
      value: formatCount(selectedRows.value.length),
      meta: '\u53ef\u7528\u4e8e\u5bfc\u51fa\u6216\u6279\u91cf\u64cd\u4f5c',
      tone: 'neutral'
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

const captureSystemErrorSearchSnapshot = (): SystemErrorSearchSnapshot => ({
  traceId: searchForm.traceId.trim(),
  deviceCode: searchForm.deviceCode.trim(),
  productKey: searchForm.productKey.trim(),
  operationModule: searchForm.operationModule.trim(),
  requestMethod: searchForm.requestMethod.trim(),
  requestUrl: searchForm.requestUrl.trim(),
  errorCode: searchForm.errorCode.trim(),
  exceptionClass: searchForm.exceptionClass.trim(),
  operationResult: searchForm.operationResult
})

const applySystemErrorSearchSnapshot = (snapshot: SystemErrorSearchSnapshot) => {
  searchForm.traceId = snapshot.traceId
  searchForm.deviceCode = snapshot.deviceCode
  searchForm.productKey = snapshot.productKey
  searchForm.operationModule = snapshot.operationModule
  searchForm.requestMethod = snapshot.requestMethod
  searchForm.requestUrl = snapshot.requestUrl
  searchForm.errorCode = snapshot.errorCode
  searchForm.exceptionClass = snapshot.exceptionClass
  searchForm.operationResult = snapshot.operationResult
  syncQuickSearchKeywordFromFilters()
  syncAdvancedFilterState()
  syncAppliedFilters()
}

const resetSystemErrorClusterNavigation = () => {
  systemErrorClusterRequestToken += 1
  clusterRows.value = []
  clusterLoading.value = false
  clusterErrorMessage.value = ''
  errorViewMode.value = 'detail'
  clusterQuerySignature.value = ''
  clusterQueryFormSnapshot.value = null
  clusterRefinerBaseSnapshot.value = null
  selectedClusterRefiner.value = null
}

const buildSystemErrorQueryParams = (
  source: Pick<
    SystemErrorSearchSnapshot,
    | 'traceId'
    | 'deviceCode'
    | 'productKey'
    | 'operationModule'
    | 'requestMethod'
    | 'requestUrl'
    | 'errorCode'
    | 'exceptionClass'
    | 'operationResult'
  >,
  refiner: SystemErrorClusterRow | null = null
) => ({
  traceId: source.traceId,
  operationType: 'system_error',
  operationModule: refiner ? refiner.operationModule ?? '' : source.operationModule,
  operationResult: source.operationResult,
  deviceCode: source.deviceCode,
  productKey: source.productKey,
  requestMethod: source.requestMethod,
  requestUrl: source.requestUrl,
  errorCode: refiner ? refiner.errorCode ?? '' : source.errorCode,
  exceptionClass: refiner ? refiner.exceptionClass ?? '' : source.exceptionClass
})

const buildSystemErrorQuerySignature = (snapshot: SystemErrorSearchSnapshot) =>
  JSON.stringify(buildSystemErrorQueryParams(snapshot))

const buildSystemErrorClusterQueryParams = (snapshot = captureSystemErrorSearchSnapshot()) =>
  buildSystemErrorQueryParams(snapshot)

const loadAuditWorkbenchData = () => {
  if (isSystemMode.value) {
    void getAuditLogStats()
    void getAuditLogList()
    return
  }
  void getAuditLogList()
  void getAuditLogStats()
}

const loadSystemTabWorkbenchData = () => {
  getScheduledTaskLedger()
  refreshMessageArchiveBatchLedger()
  getSlowSpanSummaries()
}

const loadCurrentViewData = () => {
  loadAuditWorkbenchData()
  if (isSystemMode.value) {
    loadSystemTabWorkbenchData()
  }
}

// 鑾峰彇瀹¤鏃ュ織鏌ヨ鏉′欢
const buildAuditLogQueryParams = () => {
  if (isBusinessMode.value) {
    return {
      traceId: appliedFilters.traceId,
      operationModule: appliedFilters.operationModule,
      operationResult: appliedFilters.operationResult,
      userName: appliedFilters.userName,
      operationType: appliedFilters.operationType,
      excludeSystemError: true
    }
  }

  return buildSystemErrorQueryParams(
    {
      traceId: appliedFilters.traceId,
      deviceCode: appliedFilters.deviceCode,
      productKey: appliedFilters.productKey,
      operationModule: appliedFilters.operationModule,
      requestMethod: appliedFilters.requestMethod,
      requestUrl: appliedFilters.requestUrl,
      errorCode: appliedFilters.errorCode,
      exceptionClass: appliedFilters.exceptionClass,
      operationResult: appliedFilters.operationResult
    },
    selectedClusterRefiner.value
  )
}

const logPageError = (context: string, error: unknown) => {
  if (!isHandledRequestError(error)) {
    console.error(context, error)
  }
}

const loadSystemErrorClusters = async (snapshot = clusterQueryFormSnapshot.value) => {
  if (!snapshot) {
    clusterRows.value = []
    clusterErrorMessage.value = ''
    return
  }

  const requestToken = ++systemErrorClusterRequestToken
  clusterLoading.value = true
  clusterErrorMessage.value = ''
  try {
    const res = await pageSystemErrorClusters({
      ...buildSystemErrorClusterQueryParams(snapshot),
      pageNum: 1,
      pageSize: 10
    })
    if (requestToken !== systemErrorClusterRequestToken) {
      return
    }
    clusterRows.value = res.code === 200 ? res.data?.records || [] : []
  } catch (error) {
    if (requestToken !== systemErrorClusterRequestToken) {
      return
    }
    clusterRows.value = []
    clusterErrorMessage.value = '寮傚父鍒嗙粍鍔犺浇澶辫触锛岃閲嶈瘯'
    logPageError('鍔犺浇寮傚父鍒嗙粍澶辫触', error)
  } finally {
    if (requestToken === systemErrorClusterRequestToken) {
      clusterLoading.value = false
    }
  }
}

// 鑾峰彇瀹¤鏃ュ織鍒楄〃
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
    logPageError('鑾峰彇瀹¤鏃ュ織鍒楄〃澶辫触', error)
  } finally {
    loading.value = false
  }
}

// 鑾峰彇鏃ュ織缁熻
const getAuditLogStats = async () => {
  statsLoading.value = true
  try {
    if (isSystemMode.value) {
      systemStats.value = createEmptySystemStats()
      const res = await getSystemErrorStats(
        buildSystemErrorQueryParams(
          {
            traceId: appliedFilters.traceId,
            deviceCode: appliedFilters.deviceCode,
            productKey: appliedFilters.productKey,
            operationModule: appliedFilters.operationModule,
            requestMethod: appliedFilters.requestMethod,
            requestUrl: appliedFilters.requestUrl,
            errorCode: appliedFilters.errorCode,
            exceptionClass: appliedFilters.exceptionClass,
            operationResult: appliedFilters.operationResult
          },
          errorViewMode.value === 'detail' ? selectedClusterRefiner.value : null
        )
      )
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
    logPageError('鑾峰彇鏃ュ織缁熻澶辫触', error)
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

const buildMessageArchiveBatchFilterContext = () =>
  JSON.stringify({
    batchNo: messageArchiveBatchFilters.batchNo.trim(),
    status: messageArchiveBatchFilters.status.trim(),
    compareStatus: String(messageArchiveBatchFilters.compareStatus || '').trim().toUpperCase(),
    onlyAbnormal: messageArchiveBatchFilters.onlyAbnormal,
    dateFrom: messageArchiveBatchFilters.dateFrom.trim(),
    dateTo: messageArchiveBatchFilters.dateTo.trim()
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
  selectedMessageArchiveBatchKey.value = ''
}

const clearMessageArchiveBatchOverview = () => {
  messageArchiveBatchOverview.value = null
  messageArchiveBatchOverviewLoading.value = false
  messageArchiveBatchOverviewErrorMessage.value = ''
}

const clearMessageArchiveBatchOverviewFocus = () => {
  messageArchiveBatchFocusedBatchNo.value = ''
  messageArchiveBatchFocusHint.value = ''
}

const resetMessageArchiveBatchSummarySelection = () => {
  activeMessageArchiveBatchOverviewSelection.value = ''
  messageArchiveBatchSummarySelectionFilterContext.value = ''
  clearMessageArchiveBatchOverviewFocus()
}

const syncSelectedMessageArchiveBatch = () => {
  const preferredRow =
    messageArchiveBatchRows.value.find((row) => buildArchiveBatchKey(row) === selectedMessageArchiveBatchKey.value) ||
    messageArchiveBatchRows.value.find(
      (row) => String(row.batchNo || '').trim() === messageArchiveBatchFocusedBatchNo.value
    ) ||
    messageArchiveBatchRows.value[0] ||
    null
  selectedMessageArchiveBatchKey.value = preferredRow ? buildArchiveBatchKey(preferredRow) : ''
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
    scheduledTaskErrorMessage.value = error instanceof Error ? error.message : '鑾峰彇璋冨害浠诲姟鍙拌处澶辫触'
    logPageError('鑾峰彇璋冨害浠诲姟鍙拌处澶辫触', error)
  } finally {
    scheduledTaskLoading.value = false
  }
}

const getMessageArchiveBatchLedger = async (refreshSequence?: number) => {
  if (!isSystemMode.value) {
    clearMessageArchiveBatchLedger()
    return
  }

  messageArchiveBatchLoading.value = true
  messageArchiveBatchErrorMessage.value = ''
  try {
    const res = await pageObservabilityMessageArchiveBatches(buildMessageArchiveBatchQueryParams())
    if (
      refreshSequence !== undefined &&
      refreshSequence !== messageArchiveBatchRefreshSequence
    ) {
      return
    }
    if (res.code === 200 && res.data) {
      messageArchiveBatchRows.value = Array.isArray(res.data.records) ? res.data.records : []
      messageArchiveBatchTotal.value = Number(res.data.total || messageArchiveBatchRows.value.length)
      syncSelectedMessageArchiveBatch()
    }
  } catch (error) {
    if (
      refreshSequence !== undefined &&
      refreshSequence !== messageArchiveBatchRefreshSequence
    ) {
      return
    }
    clearMessageArchiveBatchLedger()
    messageArchiveBatchErrorMessage.value = error instanceof Error ? error.message : '鑾峰彇褰掓。鎵规鍙拌处澶辫触'
    logPageError('鑾峰彇褰掓。鎵规鍙拌处澶辫触', error)
  } finally {
    if (
      refreshSequence !== undefined &&
      refreshSequence !== messageArchiveBatchRefreshSequence
    ) {
      return
    }
    messageArchiveBatchLoading.value = false
  }
}

const getMessageArchiveBatchOverview = async (refreshSequence?: number) => {
  if (!isSystemMode.value) {
    clearMessageArchiveBatchOverview()
    return
  }

  messageArchiveBatchOverviewLoading.value = true
  messageArchiveBatchOverviewErrorMessage.value = ''
  try {
    const res = await getObservabilityMessageArchiveBatchOverview(buildMessageArchiveBatchOverviewQueryParams())
    if (
      refreshSequence !== undefined &&
      refreshSequence !== messageArchiveBatchRefreshSequence
    ) {
      return
    }
    if (res.code === 200 && res.data) {
      messageArchiveBatchOverview.value = res.data
    } else {
      messageArchiveBatchOverview.value = null
    }
  } catch (error) {
    if (
      refreshSequence !== undefined &&
      refreshSequence !== messageArchiveBatchRefreshSequence
    ) {
      return
    }
    clearMessageArchiveBatchOverview()
    messageArchiveBatchOverviewErrorMessage.value =
      error instanceof Error ? error.message : '鑾峰彇褰掓。鎵规寮傚父鎽樿澶辫触'
    logPageError('鑾峰彇褰掓。鎵规寮傚父鎽樿澶辫触', error)
  } finally {
    if (
      refreshSequence !== undefined &&
      refreshSequence !== messageArchiveBatchRefreshSequence
    ) {
      return
    }
    messageArchiveBatchOverviewLoading.value = false
  }
}

let messageArchiveBatchRefreshSequence = 0

const isMessageArchiveBatchSummarySelectionCurrent = () =>
  Boolean(messageArchiveBatchSummarySelectionFilterContext.value) &&
  messageArchiveBatchSummarySelectionFilterContext.value === buildMessageArchiveBatchFilterContext()

const resolveMessageArchiveBatchSummaryFocus = async (
  refreshSequence: number,
  shouldAutoOpenSummaryFocus: boolean
) => {
  if (
    !shouldAutoOpenSummaryFocus ||
    refreshSequence !== messageArchiveBatchRefreshSequence ||
    activeMessageArchiveBatchOverviewSelection.value !== 'latest' ||
    !isMessageArchiveBatchSummarySelectionCurrent()
  ) {
    return
  }

  messageArchiveBatchFocusHint.value = ''
  messageArchiveBatchFocusedBatchNo.value = String(
    messageArchiveBatchOverview.value?.latestAbnormalBatch || ''
  ).trim()
  if (!messageArchiveBatchFocusedBatchNo.value || refreshSequence !== messageArchiveBatchRefreshSequence) {
    return
  }

  const matchedRow = messageArchiveBatchRows.value.find(
    (row) => String(row.batchNo || '').trim() === messageArchiveBatchFocusedBatchNo.value
  )
  if (refreshSequence !== messageArchiveBatchRefreshSequence) {
    return
  }
  if (matchedRow) {
    await openMessageArchiveBatchDetail(matchedRow)
    return
  }

  messageArchiveBatchFocusHint.value = '最近异常批次不在当前结果中，请调整时间范围后重试'
}

const refreshMessageArchiveBatchLedger = async () => {
  const refreshSequence = ++messageArchiveBatchRefreshSequence
  const shouldAutoOpenSummaryFocus = activeMessageArchiveBatchOverviewSelection.value === 'latest'
  await Promise.all([
    getMessageArchiveBatchLedger(refreshSequence),
    getMessageArchiveBatchOverview(refreshSequence)
  ])
  await resolveMessageArchiveBatchSummaryFocus(refreshSequence, shouldAutoOpenSummaryFocus)
}

const syncMessageArchiveBatchSummarySelectionWithFilters = () => {
  const selection = activeMessageArchiveBatchOverviewSelection.value
  if (!selection || isMessageArchiveBatchSummarySelectionCurrent()) {
    return
  }
  resetMessageArchiveBatchSummarySelection()
}

const handleMessageArchiveBatchFilterEdit = () => {
  syncMessageArchiveBatchSummarySelectionWithFilters()
}

const handleMessageArchiveBatchFilterUpdate = ({
  field,
  value
}: {
  field: 'batchNo' | 'status' | 'compareStatus' | 'dateFrom' | 'dateTo' | 'onlyAbnormal'
  value: string | boolean
}) => {
  if (field === 'onlyAbnormal') {
    messageArchiveBatchFilters.onlyAbnormal = value === true
  } else {
    messageArchiveBatchFilters[field] = typeof value === 'string' ? value : ''
  }
  handleMessageArchiveBatchFilterEdit()
}

const handleMessageArchiveBatchSearch = () => {
  syncMessageArchiveBatchSummarySelectionWithFilters()
  messageArchiveBatchFocusHint.value = ''
  void refreshMessageArchiveBatchLedger()
}

const applyMessageArchiveBatchOverviewSelection = (
  selection: ArchiveBatchOverviewSelectionKey
) => {
  messageArchiveBatchFocusHint.value = ''
  messageArchiveBatchFocusedBatchNo.value = ''
  if (selection === 'drifted') {
    messageArchiveBatchFilters.compareStatus = 'DRIFTED'
    messageArchiveBatchFilters.onlyAbnormal = false
  } else {
    messageArchiveBatchFilters.compareStatus = ''
    messageArchiveBatchFilters.onlyAbnormal = true
  }
  activeMessageArchiveBatchOverviewSelection.value = selection
  messageArchiveBatchSummarySelectionFilterContext.value = buildMessageArchiveBatchFilterContext()
}

const handleMessageArchiveBatchOverviewClick = (selection: ArchiveBatchOverviewSelectionKey) => {
  applyMessageArchiveBatchOverviewSelection(selection)
  void refreshMessageArchiveBatchLedger()
}

const handleMessageArchiveBatchLatestFocus = () => {
  applyMessageArchiveBatchOverviewSelection('latest')
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
    messageArchiveBatchReportPreviewErrorMessage.value = '褰撳墠鎵规缂哄皯鎵规鍙凤紝鏃犳硶鍔犺浇纭鎶ュ憡棰勮'
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
      error instanceof Error ? error.message : '鍔犺浇纭鎶ュ憡棰勮澶辫触'
    logPageError('鍔犺浇纭鎶ュ憡棰勮澶辫触', error)
  } finally {
    messageArchiveBatchReportPreviewLoading.value = false
  }
}

const loadMessageArchiveBatchCompare = async (row: ObservabilityMessageArchiveBatch) => {
  const batchNo = String(row.batchNo || '').trim()
  if (!batchNo) {
    clearMessageArchiveBatchCompare()
    messageArchiveBatchCompareErrorMessage.value = '褰撳墠鎵规缂哄皯鎵规鍙凤紝鏃犳硶鍔犺浇鎵规瀵规瘮'
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
      error instanceof Error ? error.message : '鍔犺浇鎵规瀵规瘮澶辫触'
    logPageError('鍔犺浇鎵规瀵规瘮澶辫触', error)
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
    selectedSlowSummaryKey.value = ''
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
      await syncSelectedSlowSummary()
    }
  } catch (error) {
    slowSummaryRows.value = []
    selectedSlowSummaryKey.value = ''
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

const syncSelectedSlowSummary = async () => {
  const nextRow =
    slowSummaryRows.value.find((row) => buildSlowSummaryKey(row) === selectedSlowSummaryKey.value) ||
    slowSummaryRows.value[0] ||
    null

  if (!nextRow) {
    selectedSlowSummaryKey.value = ''
    clearSlowSpanDrilldown()
    clearSlowTrendDrilldown()
    return
  }

  selectedSlowSummaryKey.value = buildSlowSummaryKey(nextRow)
  await Promise.all([
    loadSlowSpanDrilldown(nextRow),
    loadSlowTrendDrilldown(nextRow, slowTrendWindow.value)
  ])
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
    slowSpanErrorMessage.value = error instanceof Error ? error.message : '鑾峰彇鎱㈢偣鏄庣粏澶辫触'
    logPageError('鑾峰彇鎱㈢偣鏄庣粏澶辫触', error)
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
    slowTrendErrorMessage.value = error instanceof Error ? error.message : '鑾峰彇鎱㈢偣瓒嬪娍澶辫触'
    logPageError('鑾峰彇鎱㈢偣瓒嬪娍澶辫触', error)
  } finally {
    slowTrendLoading.value = false
  }
}

const handleSlowTrendWindowChange = (value: string | number | boolean) => {
  const targetRow = resolvedActiveSlowTrendSummary.value
  if (!targetRow) {
    return
  }
  const nextWindow: SlowTrendWindowKey =
    String(value) === 'LAST_7_DAYS' ? 'LAST_7_DAYS' : defaultSlowTrendWindow
  void loadSlowTrendDrilldown(targetRow, nextWindow)
}

// 鍒濆鍖?
onMounted(() => {
  reloadExportSelection()
  applySystemRouteQuery()
  syncQuickSearchKeywordFromFilters()
  syncAdvancedFilterState()
  syncAppliedFilters()
  loadCurrentViewData()
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
  selectedSlowSummaryKey.value = ''
  slowSummaryLoading.value = false
  slowSummaryErrorMessage.value = ''
  clearSlowSpanDrilldown()
  clearSlowTrendDrilldown()
  messageArchiveBatchDrawerVisible.value = false
  activeMessageArchiveBatch.value = null
  exportColumnDialogVisible.value = false
  activeSystemLogTab.value = 'errors'
  reloadExportSelection()
  applySystemRouteQuery()
  syncQuickSearchKeywordFromFilters()
  syncAdvancedFilterState()
  syncAppliedFilters()
  loadCurrentViewData()
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
    resetSystemErrorClusterNavigation()
    resetPage()
    clearSelection()
    syncAppliedFilters()
    loadAuditWorkbenchData()
  }
)

const triggerSearch = (resetPageFirst = false) => {
  applyQuickSearchKeywordToFilters()
  syncAdvancedFilterState()
  syncAppliedFilters()
  if (isSystemMode.value) {
    resetSystemErrorClusterNavigation()
  }
  if (resetPageFirst) {
    resetPage()
  }
  clearSelection()
  loadAuditWorkbenchData()
}

// 澶勭悊鎼滅储
const handleSearch = () => {
  triggerSearch(true)
}

// 閲嶇疆鎼滅储
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

const handleSlowSummarySelect = (row: ObservabilitySlowSpanSummary) => {
  if (!isSystemMode.value) {
    return
  }
  const nextKey = buildSlowSummaryKey(row)
  if (selectedSlowSummaryKey.value === nextKey) {
    return
  }
  selectedSlowSummaryKey.value = nextKey
  void loadSlowSpanDrilldown(row)
  void loadSlowTrendDrilldown(row, slowTrendWindow.value)
}

const handleHotspotDrilldownViewChange = (view: HotspotDrilldownView) => {
  hotspotDrilldownView.value = view
}

const handleOpenSystemErrorClusters = async () => {
  if (!isSystemMode.value) {
    return
  }

  applyQuickSearchKeywordToFilters()
  syncAdvancedFilterState()
  const snapshot = captureSystemErrorSearchSnapshot()
  const nextSignature = buildSystemErrorQuerySignature(snapshot)
  const canReuse =
    clusterQuerySignature.value === nextSignature && clusterRows.value.length > 0 && !clusterErrorMessage.value

  clusterQuerySignature.value = nextSignature
  clusterQueryFormSnapshot.value = { ...snapshot }
  errorViewMode.value = 'clusters'

  if (canReuse) {
    return
  }
  await loadSystemErrorClusters(snapshot)
}

const handleReturnToSystemErrorDetails = () => {
  if (!isSystemMode.value) {
    return
  }
  errorViewMode.value = 'detail'
}

const handleRetrySystemErrorClusters = () => {
  if (!isSystemMode.value || !clusterQueryFormSnapshot.value) {
    return
  }
  void loadSystemErrorClusters(clusterQueryFormSnapshot.value)
}

const handleSystemErrorClusterApply = (clusterKey: string) => {
  if (!isSystemMode.value || !clusterKey || !clusterQueryFormSnapshot.value) {
    return
  }

  const cluster = clusterRows.value.find((item) => item.clusterKey === clusterKey)
  if (!cluster) {
    return
  }

  clusterRefinerBaseSnapshot.value = { ...clusterQueryFormSnapshot.value }
  selectedClusterRefiner.value = cluster
  applySystemErrorSearchSnapshot({
    ...clusterQueryFormSnapshot.value,
    operationModule: cluster.operationModule ?? '',
    errorCode: cluster.errorCode ?? '',
    exceptionClass: cluster.exceptionClass ?? ''
  })
  errorViewMode.value = 'detail'
  resetPage()
  clearSelection()
  void getAuditLogStats()
  void getAuditLogList()
}

const handleClearSystemErrorClusterRefiner = () => {
  if (!isSystemMode.value || !clusterRefinerBaseSnapshot.value) {
    return
  }

  selectedClusterRefiner.value = null
  applySystemErrorSearchSnapshot(clusterRefinerBaseSnapshot.value)
  clusterRefinerBaseSnapshot.value = null
  errorViewMode.value = 'detail'
  resetPage()
  clearSelection()
  void getAuditLogStats()
  void getAuditLogList()
}

const handleReturnToSystemErrorClusters = () => {
  if (!isSystemMode.value || !canReturnToClusterResults.value) {
    return
  }
  errorViewMode.value = 'clusters'
}

const handleSystemErrorSearchFieldUpdate = ({
  field,
  value
}: {
  field:
    | 'deviceCode'
    | 'productKey'
    | 'operationModule'
    | 'requestMethod'
    | 'requestUrl'
    | 'errorCode'
    | 'exceptionClass'
    | 'operationResult'
  value: string | number | undefined
}) => {
  if (field === 'operationResult') {
    searchForm.operationResult = typeof value === 'number' ? value : undefined
    return
  }
  searchForm[field] = typeof value === 'string' ? value : ''
}

const handleRefresh = () => {
  triggerSearch(false)
}

const handleSystemLogTabChange = (tabKey: string) => {
  if (tabKey !== 'hotspots' && tabKey !== 'archives' && tabKey !== 'errors') {
    return
  }
  if (tabKey === activeSystemLogTab.value) {
    return
  }
  clearSelection()
  activeSystemLogTab.value = tabKey
}

const handleSystemTabRefresh = () => {
  if (activeSystemLogTab.value === 'hotspots') {
    getSlowSpanSummaries()
    getScheduledTaskLedger()
    return
  }
  if (activeSystemLogTab.value === 'archives') {
    void refreshMessageArchiveBatchLedger()
    return
  }
  if (errorViewMode.value === 'clusters') {
    void loadSystemErrorClusters()
    return
  }
  clearSelection()
  void getAuditLogStats()
  void getAuditLogList()
}

const handleAuditPanelRowAction = ({
  command,
  row
}: {
  command: string | number | object
  row: AuditLogRecord
}) => {
  handleAuditRowAction(command, row)
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
    ElMessage.warning('褰撳墠璁板綍缂哄皯 TraceId锛屾棤娉曟煡鐪嬭瘉鎹寘')
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
    ElMessage.warning('褰撳墠鎱㈢偣缂哄皯 TraceId锛屾棤娉曟煡鐪嬭瘉鎹寘')
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
  downloadRowsAsCsv(`${pageTitle.value}-閫変腑椤?csv`, selectedRows.value, getResolvedExportColumns())
}

const handleExportCurrent = () => {
  downloadRowsAsCsv(`${pageTitle.value}-褰撳墠缁撴灉.csv`, tableData.value, getResolvedExportColumns())
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

// 鍒嗛〉澶у皬鍙樺寲
const handleSizeChange = (size: number) => {
  setPageSize(size)
  getAuditLogList()
}

// 褰撳墠椤靛彉鍖?
const handlePageChange = (page: number) => {
  setPageNum(page)
  getAuditLogList()
}

const getAuditDirectActions = (row: AuditLogRecord) => {
  if (isSystemMode.value) {
    return [
      { command: 'detail', label: '璇︽儏' },
      { command: 'evidence', label: '璇佹嵁', disabled: !canOpenTraceEvidence(row) },
      { command: 'trace', label: '杩借釜', disabled: !canJumpToMessageTrace(row) },
      { command: 'copy-trace-id', label: '澶嶅埗 TraceId', disabled: !resolveEvidenceTraceId(row) },
      { command: 'copy-target', label: '澶嶅埗鐩爣', disabled: !resolveAuditTarget(row) },
      { command: 'delete', label: '鍒犻櫎', permission: 'system:audit:delete' }
    ]
  }

  return [
    { command: 'detail', label: '璇︽儏' },
    { command: 'delete', label: '鍒犻櫎', permission: 'system:audit:delete' }
  ]
}

const resolveAuditTarget = (row: AuditLogRecord) => {
  return String(row.requestUrl || row.operationMethod || row.requestMethod || '').trim()
}

const copyAuditText = async (value: string, missingMessage: string, successMessage: string) => {
  const normalizedValue = value.trim()
  if (!normalizedValue) {
    ElMessage.warning(missingMessage)
    return
  }
  if (!navigator.clipboard?.writeText) {
    ElMessage.warning('当前浏览器环境不支持剪贴板复制。')
    return
  }
  await navigator.clipboard.writeText(normalizedValue)
  ElMessage.success(successMessage)
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
  if (command === 'copy-trace-id') {
    void copyAuditText(resolveEvidenceTraceId(row), '当前记录缺少 TraceId，无法复制', 'TraceId 已复制')
    return
  }
  if (command === 'copy-target') {
    void copyAuditText(resolveAuditTarget(row), '当前记录缺少请求目标，无法复制', '请求目标已复制')
    return
  }
  if (command === 'delete') {
    void handleDelete(row)
  }
}

// 鏌ョ湅璇︽儏
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
      ElMessage.warning(${recordLabel.value}不存在或已删除)
      detailVisible.value = false
      return
    }
    detailData.value = { ...row, ...res.data }
  } catch (error) {
    if (!isHandledRequestError(error)) {
      ElMessage.error(`鑾峰彇${detailDialogTitle.value}澶辫触`)
    }
    detailErrorMessage.value = error instanceof Error ? error.message : `鑾峰彇${detailDialogTitle.value}澶辫触`
    logPageError('鑾峰彇鏃ュ織璇︽儏澶辫触', error)
  } finally {
    detailLoading.value = false
  }
}

// 鍒犻櫎
const handleDelete = async (row: AuditLogRecord) => {
  try {
    await confirmAction({
      title: `鍒犻櫎${recordLabel.value}`,
      message: 确认删除当前吗？删除后不可恢复。,
      type: 'warning',
      confirmButtonText: '纭鍒犻櫎'
    })
    const res = await deleteAuditLog(String(row.id))
    if (res.code === 200) {
      ElMessage.success('鍒犻櫎鎴愬姛')
      getAuditLogList()
      getAuditLogStats()
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    logPageError('鍒犻櫎澶辫触', error)
  }
}

// 鑾峰彇鎿嶄綔绫诲瀷鍚嶇О
const getOperationTypeName = (type: string) => {
  const map: Record<string, string> = {
    insert: '鏂板',
    update: '淇敼',
    delete: '鍒犻櫎',
    select: '鏌ヨ',
    system_error: '绯荤粺寮傚父'
  }
  return map[type] || type
}

// 鑾峰彇鎿嶄綔绫诲瀷鏍囩
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
  if (result === 1) return '鎴愬姛'
  if (result === 0) return '澶辫触'
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
    return `澶辫触鍘熷洜锛?{failedReason}`
  }
  const confirmReportPath = String(row.confirmReportPath || '').trim()
  return confirmReportPath ? `确认报告：${confirmReportPath}` : '确认报告：-'
}

const formatArchiveBatchReportSummaryLabel = (key: string) => {
  const map: Record<string, string> = {
    generatedAt: '鎶ュ憡鐢熸垚鏃堕棿',
    mode: '娌荤悊妯″紡',
    expiredRows: '杩囨湡琛屾暟',
    deletedRows: '鍒犻櫎琛屾暟',
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
      return '閮ㄥ垎鍙瘮'
    default:
      return '涓嶅彲姣斿'
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
  return '閮ㄥ垎鍙瘮'
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
  return formatValue(row?.previewReasonCode || '涓嶅彲棰勮')
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
    return '涓氬姟浜嬩欢'
  }
  if (type === 'SPAN') {
    return '璋冪敤鐗囨'
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
  selectedMessageArchiveBatchKey.value = buildArchiveBatchKey(row)
  activeMessageArchiveBatch.value = row
  messageArchiveBatchDrawerVisible.value = true
  await Promise.all([
    loadMessageArchiveBatchCompare(row),
    loadMessageArchiveBatchReportPreview(row)
  ])
}

const handleMessageArchiveBatchRowSelect = (row: ObservabilityMessageArchiveBatch) => {
  selectedMessageArchiveBatchKey.value = buildArchiveBatchKey(row)
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

.audit-log-system-workbench {
  display: grid;
  gap: 0.96rem;
}

.audit-log-system-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.96rem;
  align-items: start;
  padding: 0.96rem 1rem;
}

.audit-log-system-header__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.72rem;
  min-width: 0;
}

.audit-log-system-header__summary-card {
  display: grid;
  gap: 0.18rem;
  min-width: 0;
  padding: 0.78rem 0.88rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 78%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 94%, white 6%);
}

.audit-log-system-header__summary-card--emphasis {
  box-shadow: 0 18px 34px -28px color-mix(in srgb, var(--brand) 45%, transparent);
}

.audit-log-system-header__summary-card--danger.audit-log-system-header__summary-card--emphasis {
  border-color: color-mix(in srgb, var(--el-color-danger) 38%, var(--panel-border) 62%);
  background: color-mix(in srgb, var(--el-color-danger-light-9) 78%, white 22%);
}

.audit-log-system-header__summary-card--warning.audit-log-system-header__summary-card--emphasis {
  border-color: color-mix(in srgb, var(--el-color-warning) 38%, var(--panel-border) 62%);
  background: color-mix(in srgb, var(--el-color-warning-light-9) 74%, white 26%);
}

.audit-log-system-header__summary-label,
.audit-log-system-header__summary-meta {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-system-header__summary-label {
  color: var(--text-caption);
  font-size: 0.74rem;
  line-height: 1.3;
}

.audit-log-system-header__summary-value {
  color: var(--text-heading);
  font-size: 1.04rem;
  line-height: 1.2;
}

.audit-log-system-header__summary-meta {
  color: var(--text-secondary);
  font-size: 0.78rem;
  line-height: 1.3;
}

.audit-log-system-header__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.4rem;
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

@media (max-width: 1180px) {
  .audit-log-system-header {
    grid-template-columns: 1fr;
  }

  .audit-log-system-header__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 960px) {
  .audit-log-system-header__summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .audit-log-system-header {
    padding-inline: 0.86rem;
  }

  .audit-log-system-header__summary {
    grid-template-columns: 1fr;
  }
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



