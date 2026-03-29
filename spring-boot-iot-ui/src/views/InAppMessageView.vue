<template>
  <div class="in-app-message-view sys-mgmt-view standard-list-view">
    <PanelCard
      title="站内消息管理"
      description="统一治理通知中心的手工广播、系统自动消息来源与消费效果。"
      class="ops-hero-card"
    >
      <section v-loading="statsLoading" class="in-app-message-view__stats">
        <article class="in-app-message-view__stats-card">
          <span>投放总量</span>
          <strong>{{ formatCount(statsRecord?.totalDeliveryCount) }}</strong>
          <small>当前筛选范围内的累计送达人数</small>
        </article>
        <article class="in-app-message-view__stats-card">
          <span>未读总量</span>
          <strong>{{ formatCount(statsRecord?.totalUnreadCount) }}</strong>
          <small>便于识别需要继续跟进的消息</small>
        </article>
        <article class="in-app-message-view__stats-card">
          <span>已读率</span>
          <strong>{{ formatPercent(statsRecord?.readRate) }}</strong>
          <small>已读率越低越需要优化标题、范围和时机</small>
        </article>
        <article class="in-app-message-view__stats-card">
          <span>主要来源</span>
          <strong>{{ topSourceTypeLabel }}</strong>
          <small>{{ topSourceTypeHint }}</small>
        </article>
      </section>

      <section class="in-app-message-view__insight-grid">
        <article class="in-app-message-view__insight-card">
          <div class="in-app-message-view__insight-header">
            <div>
              <h3>来源分布</h3>
              <p>区分手工广播与系统自动消息，便于治理来源收敛。</p>
            </div>
          </div>
          <ul v-if="sourceTypeBuckets.length > 0" class="in-app-message-view__insight-list">
            <li v-for="bucket in sourceTypeBuckets" :key="bucket.key">
              <span>{{ bucket.label }}</span>
              <strong>{{ bucket.deliveryCount }} / {{ formatPercent(bucket.readRate) }}</strong>
            </li>
          </ul>
          <el-empty v-else description="暂无来源统计" :image-size="64" />
        </article>
        <article class="in-app-message-view__insight-card">
          <div class="in-app-message-view__insight-header">
            <div>
              <h3>高未读消息</h3>
              <p>优先关注已发出但未形成阅读闭环的消息。</p>
            </div>
          </div>
          <ul v-if="topUnreadMessages.length > 0" class="in-app-message-view__insight-list">
            <li v-for="item in topUnreadMessages" :key="String(item.messageId)">
              <span>{{ item.title }}</span>
              <strong>{{ item.unreadCount }} / {{ formatPercent(item.unreadRate) }}</strong>
            </li>
          </ul>
          <el-empty v-else description="暂无高未读消息" :image-size="64" />
        </article>
      </section>
    </PanelCard>

    <StandardWorkbenchPanel
      title="消息列表"
      description="统一治理手工广播、系统消息来源与投放范围，支持筛选后直接查看详情、编辑和停用。"
      show-header-actions
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-toolbar
      show-pagination
    >
      <template #header-actions>
        <StandardButton v-permission="'system:in-app-message:add'" action="add" :icon="Plus" @click="handleAdd">
          新增消息
        </StandardButton>
      </template>

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
                v-model="searchForm.title"
                clearable
                placeholder="消息标题"
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.messageType" clearable placeholder="消息分类">
                <el-option
                  v-for="item in IN_APP_MESSAGE_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.priority" clearable placeholder="优先级">
                <el-option
                  v-for="item in IN_APP_MESSAGE_PRIORITY_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.sourceType" clearable placeholder="来源类型">
                <el-option
                  v-for="item in IN_APP_MESSAGE_SOURCE_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </template>
          <template #advanced>
            <el-form-item>
              <el-select v-model="searchForm.targetType" clearable placeholder="推送范围">
                <el-option
                  v-for="item in IN_APP_MESSAGE_TARGET_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.status" clearable placeholder="状态">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="reset" @click="handleReset">重置</StandardButton>
            <StandardButton action="query" @click="handleSearch">查询</StandardButton>
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

      <template #toolbar>
        <StandardTableToolbar compact :meta-items="[ `当前结果 ${pagination.total} 条`, `已选 ${selectedRows.length} 项` ]">
          <template #right>
            <StandardButton action="reset" link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</StandardButton>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <StandardTableTextColumn prop="title" label="消息标题" :min-width="220" />
        <el-table-column prop="messageType" label="消息分类" width="110">
          <template #default="{ row }">
            <el-tag :type="messageTypeTagType(row.messageType)">
              {{ getMessageTypeLabel(row.messageType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" effect="plain">
              {{ getPriorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="推送范围" width="150">
          <template #default="{ row }">
            <div class="in-app-message-view__scope">
              <el-tag size="small" :type="targetTypeTagType(row.targetType)">
                {{ getTargetTypeLabel(row.targetType) }}
              </el-tag>
              <span class="in-app-message-view__scope-text">{{ getTargetSummary(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <StandardTableTextColumn prop="relatedPath" label="关联页面" :min-width="180">
          <template #default="{ row }">
            {{ getPathLabel(row.relatedPath) }}
          </template>
        </StandardTableTextColumn>
        <el-table-column prop="sourceType" label="来源类型" width="130">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="sourceTypeTagType(row.sourceType)">
              {{ getSourceTypeLabel(row.sourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <StandardTableTextColumn prop="publishTime" label="发布时间" :width="180" />
        <StandardTableTextColumn prop="expireTime" label="失效时间" :width="180" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <StandardTableTextColumn prop="summary" label="摘要" :min-width="220" />
        <el-table-column label="操作" width="220" fixed="right" :show-overflow-tooltip="false">
          <template #default="{ row }">
            <StandardRowActions variant="table" gap="wide" wrap>
              <StandardActionLink @click="handleView(row)">详情</StandardActionLink>
              <StandardActionLink
                v-if="canEditMessage(row)"
                v-permission="'system:in-app-message:update'"
                @click="handleEdit(row)"
              >
                编辑
              </StandardActionLink>
              <StandardActionLink
                v-else-if="canDeactivateMessage(row)"
                v-permission="'system:in-app-message:update'"
                @click="handleDeactivate(row)"
              >
                停用
              </StandardActionLink>
              <StandardActionLink
                v-if="canDeleteMessage(row)"
                v-permission="'system:in-app-message:delete'"
                @click="handleDelete(row)"
              >
                删除
              </StandardActionLink>
            </StandardRowActions>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <StandardPagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </template>
    </StandardWorkbenchPanel>

    <StandardWorkbenchPanel
      title="桥接效果运营"
      description="统一查看最新桥接结果、待重试情况，以及每条桥接记录的逐次尝试明细。"
      show-filters
      :show-applied-filters="hasBridgeAppliedFilters"
      show-notices
      show-toolbar
      show-pagination
    >
      <template #filters>
        <StandardListFilterHeader
          :model="bridgeSearchForm"
          :show-advanced="showBridgeAdvancedFilters"
          show-advanced-toggle
          :advanced-hint="bridgeAdvancedFilterHint"
          :primary-columns="'minmax(340px, 1.5fr) repeat(2, minmax(220px, 1fr))'"
          :primary-visible-count="3"
          @toggle-advanced="toggleBridgeAdvancedFilters"
        >
          <template #primary>
            <el-form-item>
              <el-date-picker
                v-model="bridgeSearchForm.timeRange"
                type="datetimerange"
                unlink-panels
                clearable
                value-format="YYYY-MM-DD HH:mm:ss"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="bridgeSearchForm.channelCode" clearable filterable placeholder="渠道">
                <el-option
                  v-for="channel in bridgeChannelOptions"
                  :key="channel.channelCode"
                  :label="`${channel.channelName} (${channel.channelCode})`"
                  :value="channel.channelCode"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="bridgeSearchForm.bridgeStatus" clearable placeholder="桥接状态">
                <el-option label="桥接成功" :value="1" />
                <el-option label="待重试" :value="0" />
              </el-select>
            </el-form-item>
          </template>
          <template #advanced>
            <el-form-item>
              <el-select v-model="bridgeSearchForm.messageType" clearable placeholder="消息分类">
                <el-option
                  v-for="item in IN_APP_MESSAGE_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="bridgeSearchForm.sourceType" clearable placeholder="来源类型">
                <el-option
                  v-for="item in IN_APP_MESSAGE_SOURCE_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="bridgeSearchForm.priority" clearable placeholder="优先级">
                <el-option
                  v-for="item in IN_APP_MESSAGE_PRIORITY_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="reset" @click="handleBridgeReset">重置</StandardButton>
            <StandardButton action="query" @click="handleBridgeSearch">查询</StandardButton>
          </template>
        </StandardListFilterHeader>
      </template>

      <template #applied-filters>
        <StandardAppliedFiltersBar
          :tags="bridgeFilterTags"
          @remove="handleRemoveBridgeAppliedFilter"
          @clear="handleClearBridgeAppliedFilters"
        />
      </template>

      <template #notices>
        <el-alert
          v-if="bridgeErrorMessage"
          class="in-app-message-view__bridge-alert"
          type="error"
          :closable="false"
          show-icon
          :title="bridgeErrorMessage"
        />

        <section v-loading="bridgeStatsLoading" class="in-app-message-view__bridge-stats">
          <article class="in-app-message-view__stats-card">
            <span>桥接记录数</span>
            <strong>{{ formatCount(bridgeStatsRecord?.totalBridgeCount) }}</strong>
            <small>按最近尝试时间统计的桥接汇总记录</small>
          </article>
          <article class="in-app-message-view__stats-card">
            <span>桥接成功数</span>
            <strong>{{ formatCount(bridgeStatsRecord?.successCount) }}</strong>
            <small>累计尝试 {{ formatCount(bridgeStatsRecord?.totalAttemptCount) }} 次</small>
          </article>
          <article class="in-app-message-view__stats-card">
            <span>待重试数</span>
            <strong>{{ formatCount(bridgeStatsRecord?.pendingRetryCount) }}</strong>
            <small>按最新桥接状态仍需继续跟进</small>
          </article>
          <article class="in-app-message-view__stats-card">
            <span>桥接成功率</span>
            <strong>{{ formatPercent(bridgeStatsRecord?.successRate) }}</strong>
            <small>{{ bridgeTrendHint }}</small>
          </article>
        </section>

        <section class="in-app-message-view__insight-grid in-app-message-view__bridge-insight-grid">
          <article class="in-app-message-view__insight-card">
            <div class="in-app-message-view__insight-header">
              <div>
                <h3>渠道效果分布</h3>
                <p>结合渠道类型和成功率，快速识别待重点治理的外部桥接渠道。</p>
              </div>
            </div>
            <ul v-if="bridgeChannelBuckets.length > 0" class="in-app-message-view__insight-list">
              <li v-for="bucket in bridgeChannelBuckets" :key="bucket.key">
                <span>{{ bucket.label }} · {{ getChannelTypeLabel(bucket.channelType) }}</span>
                <strong>{{ bucket.bridgeCount }} / {{ formatPercent(bucket.successRate) }}</strong>
              </li>
            </ul>
            <el-empty v-else description="暂无渠道效果分布" :image-size="64" />
          </article>
          <article class="in-app-message-view__insight-card">
            <div class="in-app-message-view__insight-header">
              <div>
                <h3>来源效果分布</h3>
                <p>区分自动消息来源，便于判断哪类事件更需要桥接补偿。</p>
              </div>
            </div>
            <ul v-if="bridgeSourceTypeBuckets.length > 0" class="in-app-message-view__insight-list">
              <li v-for="bucket in bridgeSourceTypeBuckets" :key="bucket.key">
                <span>{{ bucket.label }}</span>
                <strong>{{ bucket.bridgeCount }} / {{ formatPercent(bucket.successRate) }}</strong>
              </li>
            </ul>
            <el-empty v-else description="暂无来源效果分布" :image-size="64" />
          </article>
        </section>
      </template>

      <template #toolbar>
        <StandardTableToolbar compact :meta-items="[ `桥接结果 ${bridgePagination.total} 条`, bridgeRangeSummary ]">
          <template #right>
            <StandardButton action="refresh" link @click="handleBridgeRefresh">刷新桥接结果</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <el-table
        v-loading="bridgeTableLoading"
        :data="bridgeTableData"
        border
        stripe
        empty-text="暂无桥接记录"
        style="width: 100%"
      >
        <StandardTableTextColumn prop="title" label="消息标题" :min-width="220" />
        <el-table-column prop="messageType" label="消息分类" width="110">
          <template #default="{ row }">
            <el-tag :type="messageTypeTagType(row.messageType)">
              {{ getMessageTypeLabel(row.messageType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" effect="plain">
              {{ getPriorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源类型" width="130">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="sourceTypeTagType(row.sourceType)">
              {{ getSourceTypeLabel(row.sourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <StandardTableTextColumn prop="channelName" label="渠道名称" :min-width="140">
          <template #default="{ row }">
            {{ getBridgeChannelName(row) }}
          </template>
        </StandardTableTextColumn>
        <StandardTableTextColumn prop="channelCode" label="渠道编码" :min-width="140" />
        <el-table-column prop="channelType" label="渠道类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">
              {{ getChannelTypeLabel(row.channelType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bridgeStatus" label="桥接状态" width="110">
          <template #default="{ row }">
            <el-tag :type="bridgeStatusTagType(row.bridgeStatus)">
              {{ getBridgeStatusLabel(row.bridgeStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="unreadCount" label="未读人数" width="100" align="right">
          <template #default="{ row }">
            {{ formatCount(row.unreadCount) }}
          </template>
        </el-table-column>
        <el-table-column prop="attemptCount" label="尝试次数" width="100" align="right">
          <template #default="{ row }">
            {{ formatCount(row.attemptCount) }}
          </template>
        </el-table-column>
        <el-table-column prop="lastAttemptTime" label="最近尝试时间" width="180">
          <template #default="{ row }">
            {{ formatDateTimeValue(row.lastAttemptTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="successTime" label="成功时间" width="180">
          <template #default="{ row }">
            {{ formatDateTimeValue(row.successTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="responseStatusCode" label="响应码" width="100" align="right">
          <template #default="{ row }">
            {{ formatValue(row.responseStatusCode) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" :show-overflow-tooltip="false">
          <template #default="{ row }">
            <StandardRowActions variant="table" gap="wide">
              <StandardActionLink @click="handleViewBridge(row)">桥接详情</StandardActionLink>
            </StandardRowActions>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <StandardPagination
          v-model:current-page="bridgePagination.pageNum"
          v-model:page-size="bridgePagination.pageSize"
          :total="bridgePagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="handleBridgeSizeChange"
          @current-change="handleBridgePageChange"
        />
      </template>
    </StandardWorkbenchPanel>

    <StandardDetailDrawer
      v-model="detailVisible"
      :title="detailTitle"
      :subtitle="detailSubtitle"
      :tags="detailTags"
      :empty="!detailRecord"
    >
        <section class="detail-panel detail-panel--hero">
          <div class="detail-section-header">
            <div>
              <h3>壳层消费预览</h3>
              <p>直接预览摘要卡会如何呈现，以及详情抽屉会承接哪些动作。</p>
            </div>
          </div>
          <div class="detail-summary-grid">
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">消息分类</span>
              <strong class="detail-summary-card__value">{{ getMessageTypeLabel(detailRecord?.messageType) }}</strong>
              <p class="detail-summary-card__hint">与壳层 `系统 / 业务 / 错误` 三类直接对齐</p>
            </article>
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">优先级</span>
              <strong class="detail-summary-card__value">{{ getPriorityLabel(detailRecord?.priority) }}</strong>
              <p class="detail-summary-card__hint">发布顺序：{{ detailRecord?.sortNo ?? 0 }}</p>
            </article>
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">推送范围</span>
              <strong class="detail-summary-card__value">{{ getTargetTypeLabel(detailRecord?.targetType) }}</strong>
              <p class="detail-summary-card__hint">{{ detailTargetSummary }}</p>
            </article>
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">关联页面</span>
              <strong class="detail-summary-card__value">{{ getPathLabel(detailRecord?.relatedPath) }}</strong>
              <p class="detail-summary-card__hint">无路径时仅在通知中心展示，不绑定快捷跳转</p>
            </article>
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">消费动作</span>
              <strong class="detail-summary-card__value">{{ detailActionLabel }}</strong>
              <p class="detail-summary-card__hint">摘要卡查看详情，详情态支持显式已读与进入页面</p>
            </article>
          </div>
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>投放信息</h3>
              <p>便于快速复核角色定向、用户定向和发布时间窗口是否符合预期。</p>
            </div>
          </div>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="detail-field__label">发布时间</span>
              <strong class="detail-field__value">{{ formatValue(detailRecord?.publishTime) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">失效时间</span>
              <strong class="detail-field__value">{{ formatValue(detailRecord?.expireTime) }}</strong>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">目标角色</span>
              <strong class="detail-field__value">{{ detailRoleNames }}</strong>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">目标用户</span>
              <strong class="detail-field__value detail-field__value--plain">{{ detailUserNames }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">来源类型</span>
              <strong class="detail-field__value">{{ getSourceTypeLabel(detailRecord?.sourceType) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">来源标识</span>
              <strong class="detail-field__value detail-field__value--plain">{{ formatValue(detailRecord?.sourceId) }}</strong>
            </div>
          </div>
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>消息正文</h3>
              <p>摘要用于通知面板卡片，正文用于后续扩展“查看更多 / 详情查看”场景。</p>
            </div>
          </div>
          <div class="detail-grid">
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">摘要</span>
              <strong class="detail-field__value detail-field__value--plain">{{ formatValue(detailRecord?.summary) }}</strong>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">正文</span>
              <div class="detail-field__value detail-field__value--pre">{{ formatValue(detailRecord?.content) }}</div>
            </div>
          </div>
        </section>
      </StandardDetailDrawer>

      <StandardDetailDrawer
        v-model="bridgeDetailVisible"
        :title="bridgeDetailTitle"
        :subtitle="bridgeDetailSubtitle"
        :tags="bridgeDetailTags"
        :loading="bridgeDetailLoading"
        :empty="!bridgeDetailRecord"
      >
        <section class="detail-panel detail-panel--hero">
          <div class="detail-section-header">
            <div>
              <h3>桥接结果摘要</h3>
              <p>基于最新桥接汇总结果，快速定位当前状态、最近响应和待跟进未读人数。</p>
            </div>
          </div>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="detail-field__label">桥接状态</span>
              <strong class="detail-field__value">{{ getBridgeStatusLabel(bridgeDetailRecord?.bridgeStatus) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">渠道</span>
              <strong class="detail-field__value">{{ getBridgeChannelName(bridgeDetailRecord) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">未读人数</span>
              <strong class="detail-field__value">{{ formatCount(bridgeDetailRecord?.unreadCount) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">尝试次数</span>
              <strong class="detail-field__value">{{ formatCount(bridgeDetailRecord?.attemptCount) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">最近尝试时间</span>
              <strong class="detail-field__value">{{ formatDateTimeValue(bridgeDetailRecord?.lastAttemptTime) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">成功时间</span>
              <strong class="detail-field__value">{{ formatDateTimeValue(bridgeDetailRecord?.successTime) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">响应码</span>
              <strong class="detail-field__value">{{ formatValue(bridgeDetailRecord?.responseStatusCode) }}</strong>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">最近响应摘要</span>
              <div class="detail-field__value detail-field__value--pre">{{ formatResponseSummary(bridgeDetailRecord?.responseBody, 220) }}</div>
            </div>
          </div>
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>消息原文</h3>
              <p>通过现有消息详情接口回放原始标题、摘要、正文、来源和关联页面。</p>
            </div>
          </div>
          <el-alert
            v-if="bridgeDetailMessageError"
            class="in-app-message-view__bridge-detail-alert"
            type="error"
            :closable="false"
            show-icon
            :title="bridgeDetailMessageError"
          />
          <div v-else-if="bridgeDetailMessageRecord" class="detail-grid">
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">消息标题</span>
              <strong class="detail-field__value">{{ formatValue(bridgeDetailMessageRecord.title) }}</strong>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">摘要</span>
              <strong class="detail-field__value detail-field__value--plain">{{ formatValue(bridgeDetailMessageRecord.summary) }}</strong>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">正文</span>
              <div class="detail-field__value detail-field__value--pre">{{ formatValue(bridgeDetailMessageRecord.content) }}</div>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">来源类型</span>
              <strong class="detail-field__value">{{ getSourceTypeLabel(bridgeDetailMessageRecord.sourceType) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">来源标识</span>
              <strong class="detail-field__value">{{ formatValue(bridgeDetailMessageRecord.sourceId) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">关联页面</span>
              <strong class="detail-field__value">{{ getPathLabel(bridgeDetailMessageRecord.relatedPath) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">发布时间</span>
              <strong class="detail-field__value">{{ formatDateTimeValue(bridgeDetailMessageRecord.publishTime) }}</strong>
            </div>
          </div>
          <el-empty v-else description="暂无消息原文" :image-size="72" />
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>尝试明细</h3>
              <p>按尝试序号倒序展示每次桥接的结果、目标摘要和最近响应内容。</p>
            </div>
          </div>
          <el-alert
            v-if="bridgeAttemptError"
            class="in-app-message-view__bridge-detail-alert"
            type="error"
            :closable="false"
            show-icon
            :title="bridgeAttemptError"
          />
          <el-table
            :data="bridgeAttemptRecords"
            border
            stripe
            empty-text="暂无尝试明细"
            style="width: 100%"
          >
            <el-table-column prop="attemptNo" label="尝试序号" width="100" align="right" />
            <el-table-column prop="bridgeStatus" label="结果" width="110">
              <template #default="{ row }">
                <el-tag :type="bridgeStatusTagType(row.bridgeStatus)">
                  {{ getBridgeStatusLabel(row.bridgeStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="unreadCount" label="未读人数" width="100" align="right">
              <template #default="{ row }">
                {{ formatCount(row.unreadCount) }}
              </template>
            </el-table-column>
            <el-table-column prop="recipientSnapshot" label="目标摘要" min-width="220">
              <template #default="{ row }">
                <span class="in-app-message-view__multiline-cell">
                  {{ formatRecipientSummary(row.recipientSnapshot) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="responseStatusCode" label="响应码" width="100" align="right">
              <template #default="{ row }">
                {{ formatValue(row.responseStatusCode) }}
              </template>
            </el-table-column>
            <el-table-column prop="responseBody" label="响应摘要" min-width="220">
              <template #default="{ row }">
                <span class="in-app-message-view__multiline-cell">
                  {{ formatResponseSummary(row.responseBody, 160) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="attemptTime" label="尝试时间" width="180">
              <template #default="{ row }">
                {{ formatDateTimeValue(row.attemptTime) }}
              </template>
            </el-table-column>
          </el-table>
        </section>
      </StandardDetailDrawer>

      <StandardFormDrawer
        v-model="dialogVisible"
        :title="dialogTitle"
        subtitle="通过右侧抽屉维护站内消息的标题、范围、来源和发布时间。"
        size="56rem"
        @close="handleDialogClose"
      >
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="消息分类必须继续保持 system / business / error；否则壳层通知中心不会按既有三段式分类消费。"
        />

        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" class="in-app-message-view__form">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="消息标题" prop="title">
                <el-input v-model="formData.title" placeholder="请输入消息标题" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="消息分类" prop="messageType">
                <el-select v-model="formData.messageType" placeholder="请选择消息分类">
                  <el-option
                    v-for="item in IN_APP_MESSAGE_TYPE_OPTIONS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="优先级" prop="priority">
                <el-select v-model="formData.priority" placeholder="请选择优先级">
                  <el-option
                    v-for="item in IN_APP_MESSAGE_PRIORITY_OPTIONS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="formData.status">
                  <el-radio :value="1">启用</el-radio>
                  <el-radio :value="0">停用</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="消息摘要">
            <el-input v-model="formData.summary" type="textarea" :rows="2" placeholder="用于通知面板摘要展示" />
          </el-form-item>
          <el-form-item label="消息正文" prop="content">
            <el-input v-model="formData.content" type="textarea" :rows="6" placeholder="请输入完整消息正文" />
          </el-form-item>
          <el-form-item label="推送范围" prop="targetType">
            <el-radio-group v-model="formData.targetType">
              <el-radio
                v-for="item in IN_APP_MESSAGE_TARGET_TYPE_OPTIONS"
                :key="item.value"
                :value="item.value"
              >
                {{ item.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="formData.targetType === 'role'" label="目标角色">
            <el-select
              v-model="formData.targetRoleCodes"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择角色"
            >
              <el-option
                v-for="role in roleOptions"
                :key="role.roleCode"
                :label="`${role.roleName} (${role.roleCode})`"
                :value="role.roleCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="formData.targetType === 'user'" label="目标用户">
            <el-select
              v-model="formData.targetUserIds"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择用户"
            >
              <el-option
                v-for="user in userOptions"
                :key="String(user.id)"
                :label="buildUserLabel(user)"
                :value="user.id!"
              />
            </el-select>
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="关联页面">
                <el-select v-model="formData.relatedPath" clearable filterable placeholder="请选择关联页面">
                  <el-option
                    v-for="item in pathOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="排序">
                <el-input-number v-model="formData.sortNo" :min="0" :max="999" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="发布时间">
                <el-date-picker
                  v-model="formData.publishTime"
                  type="datetime"
                  clearable
                  placeholder="请选择发布时间"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="失效时间">
                <el-date-picker
                  v-model="formData.expireTime"
                  type="datetime"
                  clearable
                  placeholder="请选择失效时间"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="来源类型">
                <el-select v-model="formData.sourceType" placeholder="请选择来源类型">
                  <el-option
                    v-for="item in editableSourceTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="来源标识">
                <el-input v-model="formData.sourceId" placeholder="例如 alarm-1001 / help-review" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <template #footer>
          <StandardDrawerFooter :confirm-loading="submitLoading" @cancel="dialogVisible = false" @confirm="handleSubmit" />
        </template>
    </StandardFormDrawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { ChannelRecord } from '@/api/channel'
import { CHANNEL_TYPES, listChannels } from '@/api/channel'
import type { User } from '@/api/user'
import type { Role } from '@/api/role'
import {
  IN_APP_MESSAGE_PRIORITY_OPTIONS,
  IN_APP_MESSAGE_SOURCE_TYPE_OPTIONS,
  IN_APP_MESSAGE_TARGET_TYPE_OPTIONS,
  IN_APP_MESSAGE_TYPE_OPTIONS,
  addInAppMessage,
  deleteInAppMessage,
  getInAppMessage,
  getInAppMessageBridgeStats,
  getInAppMessageStats,
  listInAppMessageBridgeAttempts,
  pageInAppMessageBridgeLogs,
  pageInAppMessages,
  updateInAppMessage,
  type InAppMessageBridgeAttemptRecord,
  type InAppMessageBridgeChannelBucket,
  type InAppMessageBridgeLogRecord,
  type InAppMessageBridgeSourceTypeBucket,
  type InAppMessageBridgeStatsRecord,
  type InAppMessagePriority,
  type InAppMessageRecord,
  type InAppMessageSourceType,
  type InAppMessageStatsBucket,
  type InAppMessageStatsRecord,
  type InAppMessageTopUnreadRecord,
  type InAppMessageTargetType,
  type InAppMessageType
} from '@/api/inAppMessage'
import { listRoles } from '@/api/role'
import { listUsers } from '@/api/user'
import PanelCard from '@/components/PanelCard.vue'
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import { useListAppliedFilters } from '@/composables/useListAppliedFilters'
import { useServerPagination } from '@/composables/useServerPagination'
import { isHandledRequestError } from '@/api/request'
import type { ApiEnvelope, IdType } from '@/types/api'
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm'
import { listWorkspaceCommandEntries } from '@/utils/sectionWorkspaces'
import { formatDateTime, truncateText } from '@/utils/format'

interface SearchFormState {
  title: string
  messageType: InAppMessageType | undefined
  priority: InAppMessagePriority | undefined
  sourceType: InAppMessageSourceType | undefined
  targetType: InAppMessageTargetType | undefined
  status: number | undefined
}

interface BridgeSearchFormState {
  timeRange: string[]
  channelCode: string
  bridgeStatus: number | undefined
  messageType: InAppMessageType | undefined
  sourceType: InAppMessageSourceType | undefined
  priority: InAppMessagePriority | undefined
}

interface MessageFormState {
  id?: IdType
  title: string
  summary: string
  content: string
  messageType: InAppMessageType
  priority: InAppMessagePriority
  targetType: InAppMessageTargetType
  targetRoleCodes: string[]
  targetUserIds: IdType[]
  relatedPath: string
  sourceType: 'manual' | 'governance'
  sourceId: string
  publishTime: Date | null
  expireTime: Date | null
  status: number
  sortNo: number
}

const ALLOWED_BRIDGE_CHANNEL_TYPES = new Set(['webhook', 'wechat', 'feishu', 'dingtalk'])

const formRef = ref()
const tableRef = ref()
const loading = ref(false)
const statsLoading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增站内消息')
const detailVisible = ref(false)
const detailRecord = ref<InAppMessageRecord | null>(null)
const tableData = ref<InAppMessageRecord[]>([])
const selectedRows = ref<InAppMessageRecord[]>([])
const roleOptions = ref<Role[]>([])
const userOptions = ref<User[]>([])
const channelOptions = ref<ChannelRecord[]>([])
const statsRecord = ref<InAppMessageStatsRecord | null>(null)
const bridgeStatsLoading = ref(false)
const bridgeTableLoading = ref(false)
const bridgeStatsErrorMessage = ref('')
const bridgeTableErrorMessage = ref('')
const bridgeStatsRecord = ref<InAppMessageBridgeStatsRecord | null>(null)
const bridgeTableData = ref<InAppMessageBridgeLogRecord[]>([])
const bridgeDetailVisible = ref(false)
const bridgeDetailLoading = ref(false)
const bridgeDetailRecord = ref<InAppMessageBridgeLogRecord | null>(null)
const bridgeDetailMessageRecord = ref<InAppMessageRecord | null>(null)
const bridgeAttemptRecords = ref<InAppMessageBridgeAttemptRecord[]>([])
const bridgeDetailMessageError = ref('')
const bridgeAttemptError = ref('')
const { pagination, applyPageResult, resetPage, setPageNum, setPageSize } = useServerPagination()
const {
  pagination: bridgePagination,
  applyPageResult: applyBridgePageResult,
  resetPage: resetBridgePage,
  setPageNum: setBridgePageNum,
  setPageSize: setBridgePageSize,
  resetTotal: resetBridgeTotal
} = useServerPagination()

const searchForm = reactive<SearchFormState>({
  title: '',
  messageType: undefined,
  priority: undefined,
  sourceType: undefined,
  targetType: undefined,
  status: undefined
})
const appliedFilters = reactive<SearchFormState>({
  title: '',
  messageType: undefined,
  priority: undefined,
  sourceType: undefined,
  targetType: undefined,
  status: undefined
})

const bridgeDefaultTimeRange = createDefaultBridgeTimeRange()
const bridgeSearchForm = reactive<BridgeSearchFormState>(createEmptyBridgeSearchForm())
const bridgeAppliedFilters = reactive<BridgeSearchFormState>(createEmptyBridgeSearchForm())
const showAdvancedFilters = ref(false)
const showBridgeAdvancedFilters = ref(false)

const pathOptions = listWorkspaceCommandEntries()
  .filter((item) => item.type === 'page')
  .map((item) => ({
    value: item.path,
    label: `${item.workspaceLabel} / ${item.title}`
  }))

const formData = reactive<MessageFormState>(createEmptyForm())

const pathLabelMap = computed(() => new Map(pathOptions.map((item) => [item.value, item.label])))
const roleLabelMap = computed(() => new Map(roleOptions.value.map((item) => [item.roleCode, item.roleName])))
const userLabelMap = computed(() => new Map(
  userOptions.value
    .filter((item) => item.id !== undefined)
    .map((item) => [String(item.id), buildUserLabel(item)])
))
const channelTypeLabelMap = computed(() => new Map(CHANNEL_TYPES.map((item) => [item.value, item.label])))
const editableSourceTypeOptions = computed(() =>
  IN_APP_MESSAGE_SOURCE_TYPE_OPTIONS.filter((item) => item.value === 'manual' || item.value === 'governance')
)
const bridgeChannelOptions = computed(() =>
  channelOptions.value.filter((item) => ALLOWED_BRIDGE_CHANNEL_TYPES.has(String(item.channelType || '').toLowerCase()))
)
const sourceTypeBuckets = computed<InAppMessageStatsBucket[]>(() => statsRecord.value?.sourceTypeBuckets || [])
const topUnreadMessages = computed<InAppMessageTopUnreadRecord[]>(() => statsRecord.value?.topUnreadMessages || [])
const topSourceTypeBucket = computed(() => sourceTypeBuckets.value[0] || null)
const topSourceTypeLabel = computed(() => getSourceTypeLabel(topSourceTypeBucket.value?.key))
const topSourceTypeHint = computed(() => {
  if (!topSourceTypeBucket.value) {
    return '暂无来源数据'
  }
  return `${topSourceTypeBucket.value.deliveryCount} 次投放，已读率 ${formatPercent(topSourceTypeBucket.value.readRate)}`
})
const bridgeChannelBuckets = computed<InAppMessageBridgeChannelBucket[]>(() => bridgeStatsRecord.value?.channelBuckets || [])
const bridgeSourceTypeBuckets = computed<InAppMessageBridgeSourceTypeBucket[]>(() => bridgeStatsRecord.value?.sourceTypeBuckets || [])
const latestBridgeTrend = computed(() => {
  const trend = bridgeStatsRecord.value?.trend || []
  return trend.length > 0 ? trend[trend.length - 1] : null
})
const bridgeTrendHint = computed(() => {
  if (!latestBridgeTrend.value) {
    return '暂无最近桥接趋势'
  }
  return `${latestBridgeTrend.value.date} 成功 ${formatCount(latestBridgeTrend.value.successCount)} / 待重试 ${formatCount(latestBridgeTrend.value.pendingRetryCount)}`
})
const bridgeRangeSummary = computed(() => {
  const [startTime, endTime] = bridgeAppliedFilters.timeRange
  if (startTime || endTime) {
    return `统计范围 ${startTime || '不限'} 至 ${endTime || '不限'}`
  }
  if (bridgeStatsRecord.value?.startTime || bridgeStatsRecord.value?.endTime) {
    return `统计范围 ${bridgeStatsRecord.value?.startTime || '不限'} 至 ${bridgeStatsRecord.value?.endTime || '不限'}`
  }
  return '统计范围 最近 7 天'
})
const bridgeErrorMessage = computed(() => {
  const messages = [...new Set([bridgeStatsErrorMessage.value, bridgeTableErrorMessage.value].filter(Boolean))]
  return messages.join('；')
})
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
    { key: 'title', label: '消息标题' },
    { key: 'messageType', label: (value) => `消息分类：${getMessageTypeLabel(value)}`, clearValue: undefined, isActive: (value) => value !== undefined },
    { key: 'priority', label: (value) => `优先级：${getPriorityLabel(value)}`, clearValue: undefined, isActive: (value) => value !== undefined },
    { key: 'sourceType', label: (value) => `来源类型：${getSourceTypeLabel(value)}`, clearValue: undefined, isActive: (value) => value !== undefined },
    { key: 'targetType', label: (value) => `推送范围：${getTargetTypeLabel(value)}`, clearValue: undefined, isActive: (value) => value !== undefined, advanced: true },
    { key: 'status', label: (value) => `状态：${Number(value) === 1 ? '启用' : '停用'}`, clearValue: undefined, isActive: (value) => value !== undefined, advanced: true }
  ],
  defaults: {
    title: '',
    messageType: undefined,
    priority: undefined,
    sourceType: undefined,
    targetType: undefined,
    status: undefined
  }
})
const {
  tags: bridgeFilterTags,
  hasAppliedFilters: hasBridgeAppliedFilters,
  advancedAppliedCount: bridgeAdvancedAppliedCount,
  syncAppliedFilters: syncBridgeAppliedFilters,
  removeFilter: removeBridgeAppliedFilter
} = useListAppliedFilters({
  form: bridgeSearchForm,
  applied: bridgeAppliedFilters,
  fields: [
    {
      key: 'timeRange',
      label: '统计范围',
      format: (value) => `${value[0] || '不限'} 至 ${value[1] || '不限'}`,
      isActive: (value) => value.join('|') !== bridgeDefaultTimeRange.join('|'),
      clearValue: () => [...bridgeDefaultTimeRange]
    },
    {
      key: 'channelCode',
      label: (value) => {
        const channelCode = String(value || '').trim()
        const channelName = bridgeChannelOptions.value.find((item) => item.channelCode === channelCode)?.channelName
        return `渠道：${channelName ? `${channelName} (${channelCode})` : channelCode}`
      },
      isActive: (value) => Boolean(value),
      clearValue: ''
    },
    {
      key: 'bridgeStatus',
      label: (value) => `桥接状态：${Number(value) === 1 ? '桥接成功' : '待重试'}`,
      clearValue: undefined,
      isActive: (value) => value !== undefined
    },
    { key: 'messageType', label: (value) => `消息分类：${getMessageTypeLabel(value)}`, clearValue: undefined, isActive: (value) => value !== undefined, advanced: true },
    { key: 'sourceType', label: (value) => `来源类型：${getSourceTypeLabel(value)}`, clearValue: undefined, isActive: (value) => value !== undefined, advanced: true },
    { key: 'priority', label: (value) => `优先级：${getPriorityLabel(value)}`, clearValue: undefined, isActive: (value) => value !== undefined, advanced: true }
  ],
  defaults: () => createEmptyBridgeSearchForm()
})
const advancedFilterHint = computed(() => {
  if (showAdvancedFilters.value || advancedAppliedCount.value === 0) {
    return ''
  }
  return `更多条件已生效 ${advancedAppliedCount.value} 项`
})
const bridgeAdvancedFilterHint = computed(() => {
  if (showBridgeAdvancedFilters.value || bridgeAdvancedAppliedCount.value === 0) {
    return ''
  }
  return `更多条件已生效 ${bridgeAdvancedAppliedCount.value} 项`
})

const formRules = {
  title: [{ required: true, message: '请输入消息标题', trigger: 'blur' }],
  messageType: [{ required: true, message: '请选择消息分类', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  targetType: [{ required: true, message: '请选择推送范围', trigger: 'change' }],
  sourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入消息正文', trigger: 'blur' }]
}

watch(
  () => formData.targetType,
  (value) => {
    if (value !== 'role') {
      formData.targetRoleCodes = []
    }
    if (value !== 'user') {
      formData.targetUserIds = []
    }
  }
)

watch(bridgeDetailVisible, (value) => {
  if (!value) {
    clearBridgeDetailState()
  }
})

const detailTitle = computed(() => detailRecord.value?.title || '消息详情')
const detailSubtitle = computed(() => detailRecord.value?.summary || '统一预览消息内容、推送范围和发布时间。')
const detailActionLabel = computed(() => detailRecord.value?.relatedPath ? '查看详情 / 进入页面' : '查看详情 / 标记已读')
const detailTags = computed(() => {
  if (!detailRecord.value) {
    return []
  }
  return [
    { label: getMessageTypeLabel(detailRecord.value.messageType), type: messageTypeTagType(detailRecord.value.messageType) },
    { label: getPriorityLabel(detailRecord.value.priority), type: priorityTagType(detailRecord.value.priority) },
    { label: getSourceTypeLabel(detailRecord.value.sourceType), type: sourceTypeTagType(detailRecord.value.sourceType) },
    { label: getTargetTypeLabel(detailRecord.value.targetType), type: targetTypeTagType(detailRecord.value.targetType) },
    { label: detailRecord.value.status === 1 ? '启用中' : '已停用', type: detailRecord.value.status === 1 ? 'success' : 'danger' }
  ]
})
const detailTargetSummary = computed(() => getTargetSummary(detailRecord.value))
const detailRoleNames = computed(() => resolveRoleNames(detailRecord.value?.targetRoleCodes))
const detailUserNames = computed(() => resolveUserNames(detailRecord.value?.targetUserIds))
const bridgeDetailTitle = computed(() => bridgeDetailRecord.value?.title || '桥接详情')
const bridgeDetailSubtitle = computed(() => {
  if (!bridgeDetailRecord.value) {
    return '统一查看桥接结果汇总、消息原文和逐次尝试明细。'
  }
  return `${getBridgeChannelName(bridgeDetailRecord.value)} · 最近尝试 ${formatDateTimeValue(bridgeDetailRecord.value.lastAttemptTime)}`
})
const bridgeDetailTags = computed(() => {
  if (!bridgeDetailRecord.value) {
    return []
  }
  return [
    { label: getBridgeStatusLabel(bridgeDetailRecord.value.bridgeStatus), type: bridgeStatusTagType(bridgeDetailRecord.value.bridgeStatus) },
    { label: getMessageTypeLabel(bridgeDetailRecord.value.messageType), type: messageTypeTagType(bridgeDetailRecord.value.messageType) },
    { label: getPriorityLabel(bridgeDetailRecord.value.priority), type: priorityTagType(bridgeDetailRecord.value.priority) },
    { label: getChannelTypeLabel(bridgeDetailRecord.value.channelType), type: 'info' as const }
  ]
})

function createEmptyForm(): MessageFormState {
  return {
    id: undefined,
    title: '',
    summary: '',
    content: '',
    messageType: 'system',
    priority: 'medium',
    targetType: 'all',
    targetRoleCodes: [],
    targetUserIds: [],
    relatedPath: '',
    sourceType: 'manual',
    sourceId: '',
    publishTime: null,
    expireTime: null,
    status: 1,
    sortNo: 0
  }
}

function createEmptyBridgeSearchForm(): BridgeSearchFormState {
  return {
    timeRange: [...bridgeDefaultTimeRange],
    channelCode: '',
    bridgeStatus: undefined,
    messageType: undefined,
    sourceType: undefined,
    priority: undefined
  }
}

function createDefaultBridgeTimeRange(): string[] {
  const endTime = new Date()
  const startTime = new Date(endTime.getTime() - (7 * 24 * 60 * 60 * 1000))
  return [formatDateTimeQuery(startTime), formatDateTimeQuery(endTime)]
}

function formatDateTimeQuery(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

function resetForm() {
  Object.assign(formData, createEmptyForm())
}

function clearBridgeDetailState() {
  bridgeDetailRecord.value = null
  bridgeDetailMessageRecord.value = null
  bridgeAttemptRecords.value = []
  bridgeDetailMessageError.value = ''
  bridgeAttemptError.value = ''
  bridgeDetailLoading.value = false
}

function buildUserLabel(user: User) {
  const displayName = String(user.realName || user.username || '')
  const username = String(user.username || '')
  return username && username !== displayName ? `${displayName} (${username})` : displayName
}

function getMessageTypeLabel(value?: string | null) {
  return IN_APP_MESSAGE_TYPE_OPTIONS.find((item) => item.value === value)?.label || '--'
}

function getPriorityLabel(value?: string | null) {
  return IN_APP_MESSAGE_PRIORITY_OPTIONS.find((item) => item.value === value)?.label || '--'
}

function getTargetTypeLabel(value?: string | null) {
  return IN_APP_MESSAGE_TARGET_TYPE_OPTIONS.find((item) => item.value === value)?.label || '--'
}

function getSourceTypeLabel(value?: string | null) {
  if (value === 'system_maintenance' || value === 'daily_report') {
    return '手工广播'
  }
  if (value === 'governance_task') {
    return '治理任务'
  }
  return IN_APP_MESSAGE_SOURCE_TYPE_OPTIONS.find((item) => item.value === value)?.label || formatValue(value)
}

function getPathLabel(path?: string | null) {
  const normalizedPath = String(path || '').trim()
  if (!normalizedPath) {
    return '未绑定页面'
  }
  return pathLabelMap.value.get(normalizedPath) || normalizedPath
}

function getChannelTypeLabel(value?: string | null) {
  const normalized = String(value || '').trim().toLowerCase()
  if (!normalized) {
    return '--'
  }
  return channelTypeLabelMap.value.get(normalized) || normalized
}

function getBridgeStatusLabel(value?: number | null) {
  return Number(value) === 1 ? '桥接成功' : '待重试'
}

function messageTypeTagType(value?: string | null): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (value === 'system') {
    return 'primary'
  }
  if (value === 'business') {
    return 'success'
  }
  if (value === 'error') {
    return 'danger'
  }
  return 'info'
}

function priorityTagType(value?: string | null): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (value === 'critical') {
    return 'danger'
  }
  if (value === 'high') {
    return 'warning'
  }
  if (value === 'medium') {
    return 'primary'
  }
  if (value === 'low') {
    return 'success'
  }
  return 'info'
}

function targetTypeTagType(value?: string | null): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (value === 'all') {
    return 'primary'
  }
  if (value === 'role') {
    return 'warning'
  }
  if (value === 'user') {
    return 'success'
  }
  return 'info'
}

function sourceTypeTagType(value?: string | null): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (value === 'manual') {
    return 'primary'
  }
  if (value === 'governance') {
    return 'warning'
  }
  if (value === 'system_error') {
    return 'danger'
  }
  if (value === 'event_dispatch' || value === 'work_order') {
    return 'success'
  }
  return 'info'
}

function bridgeStatusTagType(value?: number | null): 'success' | 'warning' {
  return Number(value) === 1 ? 'success' : 'warning'
}

function splitCsvValue(value?: string | null): string[] {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function formatValue(value?: string | number | null) {
  if (value === undefined || value === null) {
    return '--'
  }
  const content = String(value).trim()
  return content || '--'
}

function formatCount(value?: number | null) {
  return Number(value || 0).toLocaleString()
}

function formatPercent(value?: number | null) {
  return `${(Number(value || 0) * 100).toFixed(1)}%`
}

function formatDateTimeValue(value?: string | null) {
  return formatDateTime(value)
}

function formatResponseSummary(value?: string | null, maxLength = 140) {
  const normalized = value === undefined || value === null ? '' : String(value).trim()
  if (!normalized) {
    return '--'
  }
  return truncateText(normalized.replace(/\s+/g, ' '), maxLength)
}

function formatRecipientSummary(value?: string | null) {
  const normalized = value === undefined || value === null ? '' : String(value).trim()
  if (!normalized) {
    return '--'
  }
  return truncateText(normalized.replace(/\s+/g, ' '), 120)
}

function resolveRoleNames(value?: string | null) {
  const roleCodes = splitCsvValue(value)
  if (roleCodes.length === 0) {
    return '全部角色'
  }
  return roleCodes.map((code) => roleLabelMap.value.get(code) || code).join('、')
}

function resolveUserNames(value?: string | null) {
  const userIds = splitCsvValue(value)
  if (userIds.length === 0) {
    return '全部用户'
  }
  return userIds.map((id) => userLabelMap.value.get(id) || id).join('、')
}

function getTargetSummary(row?: InAppMessageRecord | null) {
  if (!row) {
    return '--'
  }
  if (row.targetType === 'all') {
    return '全部账号'
  }
  if (row.targetType === 'role') {
    const roleCodes = splitCsvValue(row.targetRoleCodes)
    return roleCodes.length > 0 ? `${roleCodes.length} 个角色` : '未配置角色'
  }
  if (row.targetType === 'user') {
    const userIds = splitCsvValue(row.targetUserIds)
    return userIds.length > 0 ? `${userIds.length} 个用户` : '未配置用户'
  }
  return '--'
}

function getBridgeChannelName(row?: Partial<InAppMessageBridgeLogRecord> | null) {
  if (!row) {
    return '--'
  }
  return formatValue(row.channelName) !== '--' ? formatValue(row.channelName) : formatValue(row.channelCode)
}

function isAutomaticMessage(sourceType?: string | null) {
  return sourceType === 'system_error' || sourceType === 'event_dispatch' || sourceType === 'work_order'
}

function canEditMessage(row: InAppMessageRecord) {
  return !isAutomaticMessage(row.sourceType)
}

function canDeleteMessage(row: InAppMessageRecord) {
  return !isAutomaticMessage(row.sourceType)
}

function canDeactivateMessage(row: InAppMessageRecord) {
  return isAutomaticMessage(row.sourceType) && Number(row.status ?? 1) === 1
}

function buildPayload() {
  return {
    id: formData.id,
    title: formData.title.trim(),
    summary: formData.summary.trim() || undefined,
    content: formData.content.trim(),
    messageType: formData.messageType,
    priority: formData.priority,
    targetType: formData.targetType,
    targetRoleCodes: formData.targetType === 'role' && formData.targetRoleCodes.length > 0
      ? formData.targetRoleCodes.join(',')
      : undefined,
    targetUserIds: formData.targetType === 'user' && formData.targetUserIds.length > 0
      ? formData.targetUserIds.map((item) => String(item)).join(',')
      : undefined,
    relatedPath: formData.relatedPath || undefined,
    sourceType: formData.sourceType || 'manual',
    sourceId: formData.sourceId.trim() || undefined,
    publishTime: formData.publishTime ? formData.publishTime.toISOString() : undefined,
    expireTime: formData.expireTime ? formData.expireTime.toISOString() : undefined,
    status: formData.status,
    sortNo: formData.sortNo
  }
}

function buildUpdatePayloadFromRecord(row: InAppMessageRecord, status: number) {
  return {
    id: row.id,
    title: row.title,
    summary: row.summary || undefined,
    content: row.content || undefined,
    messageType: row.messageType,
    priority: row.priority,
    targetType: row.targetType,
    targetRoleCodes: row.targetRoleCodes || undefined,
    targetUserIds: row.targetUserIds || undefined,
    relatedPath: row.relatedPath || undefined,
    sourceType: row.sourceType || undefined,
    sourceId: row.sourceId || undefined,
    publishTime: row.publishTime || undefined,
    expireTime: row.expireTime || undefined,
    status,
    sortNo: row.sortNo ?? 0
  }
}

function buildBridgeQuery() {
  const [startTime, endTime] = bridgeAppliedFilters.timeRange
  return {
    startTime: startTime || undefined,
    endTime: endTime || undefined,
    channelCode: bridgeAppliedFilters.channelCode || undefined,
    bridgeStatus: bridgeAppliedFilters.bridgeStatus,
    messageType: bridgeAppliedFilters.messageType,
    sourceType: bridgeAppliedFilters.sourceType,
    priority: bridgeAppliedFilters.priority
  }
}

function ensureSuccess<T>(response: ApiEnvelope<T>, fallbackMessage: string): T {
  if (response.code === 200 && response.data !== undefined && response.data !== null) {
    return response.data
  }
  throw new Error(response.msg || fallbackMessage)
}

function resolvePageErrorMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error) {
    return error.message || fallbackMessage
  }
  return fallbackMessage
}

function showPageError(error: unknown, fallbackMessage: string) {
  const message = resolvePageErrorMessage(error, fallbackMessage)
  if (!isHandledRequestError(error)) {
    ElMessage.error(message)
  }
  return message
}

function logPageError(context: string, error: unknown) {
  if (!isHandledRequestError(error)) {
    console.error(context, error)
  }
}

async function loadRoleOptions() {
  try {
    const response = await listRoles({ status: 1 })
    if (response.code === 200 && response.data) {
      roleOptions.value = response.data
    }
  } catch (error) {
    logPageError('加载角色列表失败', error)
  }
}

async function loadUserOptions() {
  try {
    const response = await listUsers({ status: 1 })
    if (response.code === 200 && response.data) {
      userOptions.value = response.data
    }
  } catch (error) {
    logPageError('加载用户列表失败', error)
  }
}

async function loadChannelOptions() {
  try {
    const response = await listChannels()
    if (response.code === 200 && response.data) {
      channelOptions.value = response.data
    }
  } catch (error) {
    logPageError('加载渠道列表失败', error)
  }
}

async function loadMessagePage() {
  loading.value = true
  try {
    const response = await pageInAppMessages({
      title: appliedFilters.title || undefined,
      messageType: appliedFilters.messageType,
      priority: appliedFilters.priority,
      sourceType: appliedFilters.sourceType,
      targetType: appliedFilters.targetType,
      status: appliedFilters.status,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (response.code === 200 && response.data) {
      tableData.value = applyPageResult(response.data)
    }
  } catch (error) {
    logPageError('获取站内消息分页失败', error)
    showPageError(error, '获取站内消息分页失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  statsLoading.value = true
  try {
    const response = await getInAppMessageStats({
      messageType: appliedFilters.messageType,
      sourceType: appliedFilters.sourceType
    })
    if (response.code === 200 && response.data) {
      statsRecord.value = response.data
    }
  } catch (error) {
    logPageError('获取站内消息统计失败', error)
    showPageError(error, '获取站内消息统计失败')
  } finally {
    statsLoading.value = false
  }
}

async function loadBridgeStats() {
  bridgeStatsLoading.value = true
  bridgeStatsErrorMessage.value = ''
  try {
    const response = await getInAppMessageBridgeStats(buildBridgeQuery())
    bridgeStatsRecord.value = ensureSuccess(response, '获取桥接统计失败')
  } catch (error) {
    logPageError('获取桥接统计失败', error)
    bridgeStatsRecord.value = null
    bridgeStatsErrorMessage.value = resolvePageErrorMessage(error, '获取桥接统计失败')
  } finally {
    bridgeStatsLoading.value = false
  }
}

async function loadBridgePage() {
  bridgeTableLoading.value = true
  bridgeTableErrorMessage.value = ''
  try {
    const response = await pageInAppMessageBridgeLogs({
      ...buildBridgeQuery(),
      pageNum: bridgePagination.pageNum,
      pageSize: bridgePagination.pageSize
    })
    bridgeTableData.value = applyBridgePageResult(ensureSuccess(response, '获取桥接日志失败'))
  } catch (error) {
    logPageError('获取桥接日志失败', error)
    bridgeTableData.value = []
    resetBridgeTotal()
    bridgeTableErrorMessage.value = resolvePageErrorMessage(error, '获取桥接日志失败')
  } finally {
    bridgeTableLoading.value = false
  }
}

async function refreshBridgeSection() {
  await Promise.allSettled([loadBridgeStats(), loadBridgePage()])
}

function clearSelection() {
  selectedRows.value = []
  tableRef.value?.clearSelection?.()
}

function handleSelectionChange(rows: InAppMessageRecord[]) {
  selectedRows.value = rows
}

function syncAdvancedFilterState() {
  showAdvancedFilters.value = searchForm.targetType !== undefined || searchForm.status !== undefined
}

function toggleAdvancedFilters() {
  showAdvancedFilters.value = !showAdvancedFilters.value
}

function handleSearch() {
  syncAdvancedFilterState()
  syncAppliedFilters()
  resetPage()
  clearSelection()
  loadMessagePage()
  loadStats()
}

function handleReset() {
  searchForm.title = ''
  searchForm.messageType = undefined
  searchForm.priority = undefined
  searchForm.sourceType = undefined
  searchForm.targetType = undefined
  searchForm.status = undefined
  syncAdvancedFilterState()
  syncAppliedFilters()
  resetPage()
  clearSelection()
  loadMessagePage()
  loadStats()
}

function handleRemoveAppliedFilter(key: string) {
  removeAppliedFilter(key)
  syncAdvancedFilterState()
  resetPage()
  clearSelection()
  loadMessagePage()
  loadStats()
}

function handleClearAppliedFilters() {
  handleReset()
}

function handlePageChange(page: number) {
  setPageNum(page)
  clearSelection()
  loadMessagePage()
}

function handleSizeChange(pageSize: number) {
  setPageSize(pageSize)
  clearSelection()
  loadMessagePage()
}

function handleRefresh() {
  clearSelection()
  loadMessagePage()
  loadStats()
}

function syncBridgeAdvancedFilterState() {
  showBridgeAdvancedFilters.value = Boolean(
    bridgeSearchForm.messageType
      || bridgeSearchForm.sourceType
      || bridgeSearchForm.priority
  )
}

function toggleBridgeAdvancedFilters() {
  showBridgeAdvancedFilters.value = !showBridgeAdvancedFilters.value
}

function handleBridgeSearch() {
  syncBridgeAdvancedFilterState()
  syncBridgeAppliedFilters()
  resetBridgePage()
  refreshBridgeSection()
}

function handleBridgeReset() {
  Object.assign(bridgeSearchForm, createEmptyBridgeSearchForm())
  syncBridgeAdvancedFilterState()
  syncBridgeAppliedFilters()
  resetBridgePage()
  refreshBridgeSection()
}

function handleRemoveBridgeAppliedFilter(key: string) {
  removeBridgeAppliedFilter(key)
  syncBridgeAdvancedFilterState()
  resetBridgePage()
  refreshBridgeSection()
}

function handleClearBridgeAppliedFilters() {
  handleBridgeReset()
}

function handleBridgePageChange(page: number) {
  setBridgePageNum(page)
  loadBridgePage()
}

function handleBridgeSizeChange(pageSize: number) {
  setBridgePageSize(pageSize)
  loadBridgePage()
}

function handleBridgeRefresh() {
  refreshBridgeSection()
}

function handleDialogClose() {
  resetForm()
  formRef.value?.clearValidate?.()
}

function handleAdd() {
  dialogTitle.value = '新增站内消息'
  resetForm()
  dialogVisible.value = true
}

function handleView(row: InAppMessageRecord) {
  detailRecord.value = row
  detailVisible.value = true
}

async function handleViewBridge(row: InAppMessageBridgeLogRecord) {
  bridgeDetailRecord.value = row
  bridgeDetailVisible.value = true
  bridgeDetailLoading.value = true
  bridgeDetailMessageRecord.value = null
  bridgeAttemptRecords.value = []
  bridgeDetailMessageError.value = ''
  bridgeAttemptError.value = ''

  const [messageResult, attemptResult] = await Promise.allSettled([
    getInAppMessage(row.messageId),
    listInAppMessageBridgeAttempts(row.id)
  ])

  if (messageResult.status === 'fulfilled') {
    try {
      bridgeDetailMessageRecord.value = ensureSuccess(messageResult.value, '加载消息原文失败')
    } catch (error) {
      bridgeDetailMessageError.value = resolvePageErrorMessage(error, '加载消息原文失败')
    }
  } else {
    bridgeDetailMessageError.value = resolvePageErrorMessage(messageResult.reason, '加载消息原文失败')
  }

  if (attemptResult.status === 'fulfilled') {
    try {
      const attempts = ensureSuccess(attemptResult.value, '加载桥接尝试明细失败')
      bridgeAttemptRecords.value = [...attempts].sort((left, right) => Number(right.attemptNo || 0) - Number(left.attemptNo || 0))
    } catch (error) {
      bridgeAttemptError.value = resolvePageErrorMessage(error, '加载桥接尝试明细失败')
    }
  } else {
    bridgeAttemptError.value = resolvePageErrorMessage(attemptResult.reason, '加载桥接尝试明细失败')
  }

  bridgeDetailLoading.value = false
}

async function handleEdit(row: InAppMessageRecord) {
  if (isAutomaticMessage(row.sourceType)) {
    detailRecord.value = row
    detailVisible.value = true
    ElMessage.info('系统自动消息仅支持查看或停用')
    return
  }
  try {
    const response = await getInAppMessage(row.id!)
    const record = ensureSuccess(response, '加载消息详情失败')
    dialogTitle.value = '编辑站内消息'
    Object.assign(formData, {
      id: record.id,
      title: record.title || '',
      summary: record.summary || '',
      content: record.content || '',
      messageType: record.messageType || 'system',
      priority: record.priority || 'medium',
      targetType: record.targetType || 'all',
      targetRoleCodes: splitCsvValue(record.targetRoleCodes),
      targetUserIds: splitCsvValue(record.targetUserIds),
      relatedPath: record.relatedPath || '',
      sourceType: record.sourceType === 'governance' ? 'governance' : 'manual',
      sourceId: record.sourceId || '',
      publishTime: record.publishTime ? new Date(record.publishTime) : null,
      expireTime: record.expireTime ? new Date(record.expireTime) : null,
      status: Number(record.status ?? 1),
      sortNo: Number(record.sortNo ?? 0)
    })
    dialogVisible.value = true
  } catch (error) {
    logPageError('加载站内消息详情失败', error)
    showPageError(error, '加载站内消息详情失败')
  }
}

async function handleDeactivate(row: InAppMessageRecord) {
  try {
    await updateInAppMessage(buildUpdatePayloadFromRecord(row, 0))
    ElMessage.success('停用成功')
    clearSelection()
    await loadMessagePage()
    await loadStats()
  } catch (error) {
    logPageError('停用站内消息失败', error)
    showPageError(error, '停用站内消息失败')
  }
}

async function handleDelete(row: InAppMessageRecord) {
  try {
    await confirmDelete(`确定删除站内消息“${row.title}”吗？删除后不会再出现在通知中心。`)
    await deleteInAppMessage(row.id!)
    ElMessage.success('删除成功')
    clearSelection()
    await loadMessagePage()
    await loadStats()
  } catch (error) {
    if (!isConfirmCancelled(error)) {
      logPageError('删除站内消息失败', error)
      showPageError(error, '删除站内消息失败')
    }
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate?.()
    submitLoading.value = true
    const payload = buildPayload()
    if (formData.id) {
      await updateInAppMessage(payload)
      ElMessage.success('更新成功')
    } else {
      await addInAppMessage(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    clearSelection()
    await loadMessagePage()
    await loadStats()
  } catch (error) {
    if (error instanceof Error) {
      logPageError('提交站内消息失败', error)
      showPageError(error, '提交站内消息失败')
    }
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  syncAppliedFilters()
  syncBridgeAppliedFilters()
  loadMessagePage()
  loadStats()
  loadRoleOptions()
  loadUserOptions()
  loadChannelOptions()
  refreshBridgeSection()
})
</script>

<style scoped>
.in-app-message-view__header {
  align-items: flex-start;
}

.in-app-message-view__header-copy {
  display: grid;
  gap: 0.35rem;
}

.in-app-message-view__header-copy small {
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.5;
}

.in-app-message-view__scope {
  display: grid;
  gap: 0.25rem;
}

.in-app-message-view__scope-text {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.4;
}

.in-app-message-view__stats,
.in-app-message-view__bridge-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.in-app-message-view__stats-card,
.in-app-message-view__insight-card,
.in-app-message-view__bridge-filter-card {
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-3xl);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--brand-50) 48%, white) 0%, white 100%);
  padding: 1rem 1.1rem;
  display: grid;
  gap: 0.4rem;
}

.in-app-message-view__stats-card span,
.in-app-message-view__insight-card p {
  color: var(--text-caption);
}

.in-app-message-view__stats-card strong {
  font-size: 1.6rem;
  line-height: 1.1;
  color: var(--text-primary);
}

.in-app-message-view__stats-card small {
  color: var(--text-caption);
  line-height: 1.5;
}

.in-app-message-view__insight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.in-app-message-view__bridge-insight-grid {
  margin-top: 0.25rem;
}

.in-app-message-view__insight-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.in-app-message-view__insight-header h3 {
  margin: 0 0 0.2rem;
  font-size: 1rem;
}

.in-app-message-view__insight-header p {
  margin: 0;
  line-height: 1.5;
}

.in-app-message-view__insight-list {
  list-style: none;
  display: grid;
  gap: 0.75rem;
  padding: 0;
  margin: 0;
}

.in-app-message-view__insight-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  border-top: 1px dashed var(--panel-border);
  padding-top: 0.75rem;
}

.in-app-message-view__insight-list li:first-child {
  border-top: none;
  padding-top: 0;
}

.in-app-message-view__insight-list span {
  color: var(--text-primary);
  line-height: 1.5;
}

.in-app-message-view__insight-list strong {
  color: var(--text-secondary);
  white-space: nowrap;
}

.in-app-message-view__bridge-collapse {
  margin-top: 1.5rem;
}

.in-app-message-view__bridge-collapse :deep(.el-collapse) {
  border: none;
}

.in-app-message-view__bridge-collapse :deep(.el-collapse-item__header) {
  height: auto;
  padding: 1rem 1.15rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-3xl) + 2px);
  background:
    linear-gradient(135deg, rgba(247, 250, 255, 0.96), rgba(255, 255, 255, 0.98)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--accent) 10%, transparent), transparent 30%);
}

.in-app-message-view__bridge-collapse :deep(.el-collapse-item__wrap) {
  margin-top: 0.85rem;
  border: none;
  background: transparent;
}

.in-app-message-view__bridge-collapse :deep(.el-collapse-item__content) {
  padding-bottom: 0;
}

.in-app-message-view__bridge-title {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.in-app-message-view__bridge-title-copy {
  display: grid;
  gap: 0.35rem;
}

.in-app-message-view__bridge-title-copy span {
  color: var(--text-heading);
  font-size: 1rem;
  font-weight: 700;
}

.in-app-message-view__bridge-title-copy small {
  color: var(--text-caption);
  line-height: 1.5;
}

.in-app-message-view__bridge-filter-card {
  margin-bottom: 1rem;
}

.in-app-message-view__bridge-filter-card :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.in-app-message-view__bridge-alert,
.in-app-message-view__bridge-detail-alert {
  margin-bottom: 1rem;
}

.in-app-message-view__multiline-cell {
  display: block;
  line-height: 1.5;
  white-space: normal;
  word-break: break-word;
}

.in-app-message-view__form {
  margin-top: 1rem;
}

.in-app-message-view__form :deep(.el-alert) {
  margin-bottom: 1rem;
}

.detail-field__value--pre {
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1200px) {
  .in-app-message-view__stats,
  .in-app-message-view__bridge-stats,
  .in-app-message-view__insight-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .in-app-message-view__stats,
  .in-app-message-view__bridge-stats,
  .in-app-message-view__insight-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .in-app-message-view__bridge-title {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
