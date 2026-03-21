<template>
  <div class="product-asset-view ops-workbench standard-list-view">
    <PanelCard class="ops-hero-card ops-table-card product-workbench-card">
      <template #header>
        <div class="product-hero-card__header">
          <div class="product-hero-card__heading">
            <h2 class="product-hero-card__title">产品定义中心</h2>
            <p class="product-hero-card__caption">聚焦产品台账维护，支持筛选、查看、编辑、删除、导出和关联设备跳转。</p>
          </div>
        </div>
      </template>

      <div class="product-workbench-card__filters">
        <StandardListFilterHeader :model="searchForm">
          <template #primary>
            <!-- 快速搜索：支持产品名称、厂商关键词搜索 -->
            <el-form-item>
              <el-input
                id="quick-search"
                v-model="quickSearchKeyword"
                placeholder="快速搜索（产品名称、厂商）"
                clearable
                prefix-icon="Search"
                @keyup.enter="handleQuickSearch"
                @clear="handleClearQuickSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.nodeType" placeholder="节点类型" clearable>
                <el-option label="直连设备" :value="1" />
                <el-option label="网关设备" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.status" placeholder="产品状态" clearable>
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
          </template>
          <template #actions>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button v-permission="'iot:products:add'" type="primary" @click="handleAdd">新增产品</el-button>
          </template>
        </StandardListFilterHeader>
        <!-- 快速搜索标签 -->
        <div v-if="quickSearchKeyword" class="product-quick-search-tag">
          <el-tag closable class="product-quick-search-tag__chip" @close="handleClearQuickSearch">
            快速搜索：{{ quickSearchKeyword }}
          </el-tag>
        </div>
      </div>

      <div v-if="hasAppliedFilters" class="product-applied-filters">
        <span class="product-applied-filters__label">已生效筛选</span>
        <div class="product-applied-filters__list">
          <el-tag
            v-for="tag in activeFilterTags"
            :key="tag.key"
            closable
            class="product-applied-filters__tag"
            @close="removeAppliedFilter(tag.key)"
          >
            {{ tag.label }}
          </el-tag>
        </div>
        <el-button link class="product-applied-filters__clear" @click="handleClearAppliedFilters">清空全部</el-button>
      </div>

      <StandardTableToolbar
        :meta-items="[
          `已选 ${selectedRows.length} 项`,
          `启用 ${enabledProductCount} 个`,
          `停用 ${disabledProductCount} 个`
        ]"
      >
        <template #right>
          <!-- 视图切换下拉菜单 -->
          <el-dropdown
            v-permission="'iot:products:view'"
            @command="(command) => handleViewTypeChange(command)"
          >
            <el-button type="primary" link>
              <span>{{ viewTypeName }}</span>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="table">
                  <el-icon><List /></el-icon>
                  <span>表格视图</span>
                </el-dropdown-item>
                <el-dropdown-item command="card">
                  <el-icon><Grid /></el-icon>
                  <span>卡片视图</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <!-- 批量操作下拉菜单 -->
          <el-dropdown
            v-permission="'iot:products:update'"
            :disabled="selectedRows.length === 0"
            trigger="click"
            @command="(command) => handleBatchCommand(command, selectedRows)"
          >
            <el-button type="primary" link :disabled="selectedRows.length === 0">
              <span>批量操作</span>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="enable" divided>
                  <el-icon><Top /></el-icon>
                  <span>启用</span>
                </el-dropdown-item>
                <el-dropdown-item command="disable">
                  <el-icon><Bottom /></el-icon>
                  <span>停用</span>
                </el-dropdown-item>
                <el-dropdown-item command="delete">
                  <el-icon><Delete /></el-icon>
                  <span>删除</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-permission="'iot:products:export'" link @click="openExportColumnSetting">导出列设置</el-button>
          <el-button v-permission="'iot:products:export'" link :disabled="selectedRows.length === 0" @click="handleExportSelected">
            导出选中
          </el-button>
          <el-button v-permission="'iot:products:export'" link :disabled="tableData.length === 0" @click="handleExportCurrent">
            导出当前结果
          </el-button>
          <el-button link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</el-button>
          <el-button link @click="handleRefresh">刷新列表</el-button>
        </template>
      </StandardTableToolbar>

      <div
        v-if="showListInlineState"
        :class="[
          'product-list-inline-state',
          { 'product-list-inline-state--error': listRefreshState === 'error' }
        ]"
      >
        {{ listRefreshMessage }}
      </div>

      <div
        v-loading="loading && hasRecords"
        class="product-result-panel"
        element-loading-text="正在刷新产品列表"
        element-loading-background="rgba(248, 250, 255, 0.78)"
      >
        <div v-if="showListSkeleton" class="product-loading-state" aria-live="polite" aria-busy="true">
          <div class="product-loading-state__summary">
            <span v-for="item in 3" :key="item" class="product-loading-pulse product-loading-pill" />
          </div>

          <div class="product-loading-state__desktop">
            <div class="product-loading-table product-loading-table--header">
              <span v-for="item in 8" :key="`head-${item}`" class="product-loading-pulse product-loading-line product-loading-line--header" />
            </div>
            <div v-for="row in 5" :key="`row-${row}`" class="product-loading-table product-loading-table--row">
              <span class="product-loading-pulse product-loading-square" />
              <span class="product-loading-pulse product-loading-line product-loading-line--key" />
              <span class="product-loading-pulse product-loading-line product-loading-line--title" />
              <span class="product-loading-pulse product-loading-line product-loading-line--short" />
              <span class="product-loading-pulse product-loading-line product-loading-line--short" />
              <span class="product-loading-pulse product-loading-line product-loading-line--meta" />
              <span class="product-loading-pulse product-loading-pill product-loading-pill--status" />
              <span class="product-loading-pulse product-loading-line product-loading-line--time" />
            </div>
          </div>

          <div class="product-loading-state__mobile">
            <article v-for="card in 3" :key="`card-${card}`" class="product-loading-mobile-card">
              <div class="product-loading-mobile-card__header">
                <span class="product-loading-pulse product-loading-square" />
                <div class="product-loading-mobile-card__heading">
                  <span class="product-loading-pulse product-loading-line product-loading-line--title" />
                  <span class="product-loading-pulse product-loading-line product-loading-line--meta" />
                </div>
                <span class="product-loading-pulse product-loading-pill product-loading-pill--status" />
              </div>
              <div class="product-loading-mobile-card__meta">
                <span v-for="item in 3" :key="`meta-${card}-${item}`" class="product-loading-pulse product-loading-pill" />
              </div>
              <div class="product-loading-mobile-card__info">
                <div v-for="item in 4" :key="`field-${card}-${item}`" class="product-loading-mobile-card__field">
                  <span class="product-loading-pulse product-loading-line product-loading-line--label" />
                  <span class="product-loading-pulse product-loading-line product-loading-line--value" />
                </div>
              </div>
            </article>
          </div>
        </div>

        <template v-else-if="hasRecords">
          <!-- 卡片视图 -->
          <div v-if="viewType === 'card'" class="product-card-view">
            <div class="product-mobile-list__grid">
              <article v-for="row in tableData" :key="getProductRowKey(row)" class="product-mobile-card">
                <div class="product-mobile-card__header">
                  <el-checkbox
                    :model-value="isRowSelected(row)"
                    @change="(checked) => handleMobileSelectionChange(row, Boolean(checked))"
                  />
                  <div class="product-mobile-card__heading">
                    <strong class="product-mobile-card__title">{{ row.productName || '--' }}</strong>
                    <span class="product-mobile-card__sub">{{ row.productKey || '--' }}</span>
                  </div>
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" round>{{ getStatusText(row.status) }}</el-tag>
                </div>

                <div class="product-mobile-card__meta">
                  <span class="product-mobile-card__meta-item">{{ getNodeTypeText(row.nodeType) }}</span>
                  <span class="product-mobile-card__meta-item">{{ row.protocolCode || '--' }}</span>
                  <span class="product-mobile-card__meta-item">{{ row.dataFormat || '--' }}</span>
                </div>

                <div class="product-mobile-card__info">
                  <div class="product-mobile-card__field">
                    <span>厂商</span>
                    <strong>{{ formatTextValue(row.manufacturer) }}</strong>
                  </div>
                  <div class="product-mobile-card__field">
                    <span>关联设备</span>
                    <strong>{{ formatCount(row.deviceCount) }}</strong>
                  </div>
                  <div class="product-mobile-card__field">
                    <span>最近上报</span>
                    <strong>{{ formatDateTime(row.lastReportTime) }}</strong>
                  </div>
                  <div class="product-mobile-card__field">
                    <span>更新时间</span>
                    <strong>{{ formatDateTime(row.updateTime) }}</strong>
                  </div>
                </div>

                <div class="product-mobile-card__actions">
                  <el-button type="primary" link @click="handleOpenDetail(row)">详情</el-button>
                  <el-button v-permission="'iot:products:update'" type="primary" link @click="handleEdit(row)">编辑</el-button>
                  <el-dropdown trigger="click" @command="(command) => handleRowAction(command, row)">
                    <el-button type="primary" link>
                      更多
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="devices">查看设备</el-dropdown-item>
                        <el-dropdown-item v-permission="'iot:products:delete'" command="delete">删除</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </article>
            </div>
          </div>

          <!-- 表格视图 -->
          <template v-if="viewType === 'table'">
            <el-table
              ref="tableRef"
              class="product-desktop-table"
              :data="tableData"
              border
              stripe
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="48" />
              <StandardTableTextColumn prop="productKey" label="产品 Key" :min-width="170" />
              <StandardTableTextColumn prop="productName" label="产品名称" :min-width="180" />
              <StandardTableTextColumn prop="protocolCode" label="协议编码" :width="140" />
              <el-table-column prop="nodeType" label="节点类型" width="120">
                <template #default="{ row }">
                  <el-tag round>{{ getNodeTypeText(row.nodeType) }}</el-tag>
                </template>
              </el-table-column>
              <StandardTableTextColumn prop="dataFormat" label="数据格式" :width="120" />
              <StandardTableTextColumn prop="manufacturer" label="厂商" :min-width="150" />
              <el-table-column prop="status" label="产品状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" round>{{ getStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="onlineDeviceCount" label="在线设备数" width="110" align="center" />
              <StandardTableTextColumn prop="lastReportTime" label="最近设备上报" :width="180">
                <template #default="{ row }">{{ formatDateTime(row.lastReportTime) }}</template>
              </StandardTableTextColumn>
              <StandardTableTextColumn prop="updateTime" label="更新时间" :width="180">
                <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
              </StandardTableTextColumn>
              <el-table-column label="操作" width="180" fixed="right" :show-overflow-tooltip="false">
                <template #default="{ row }">
                  <div class="product-table-actions">
                    <el-button type="primary" link @click="handleOpenDetail(row)">详情</el-button>
                    <el-button v-permission="'iot:products:update'" type="primary" link @click="handleEdit(row)">编辑</el-button>
                    <el-dropdown trigger="click" @command="(command) => handleRowAction(command, row)">
                      <el-button type="primary" link>
                        更多
                      </el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="devices">查看设备</el-dropdown-item>
                          <el-dropdown-item v-permission="'iot:products:delete'" command="delete">删除</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </template>

        <div v-else-if="!loading" class="product-empty-state">
          <EmptyState :title="emptyStateTitle" :description="emptyStateDescription" />
          <div class="product-empty-state__actions">
            <el-button v-if="hasAppliedFilters" @click="handleClearAppliedFilters">清空筛选条件</el-button>
            <el-button v-else v-permission="'iot:products:add'" type="primary" @click="handleAdd">新增产品</el-button>
          </div>
        </div>
      </div>

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
    </PanelCard>

    <StandardDetailDrawer
      v-model="detailVisible"
      class="product-detail-drawer"
      size="42rem"
      eyebrow="产品定义详情"
      :title="detailTitle"
      :subtitle="detailSubtitle"
      :loading="detailLoading"
      :error-message="detailErrorMessage"
      :empty="!detailData"
    >
      <template #header-actions>
        <el-button
          v-permission="'iot:products:update'"
          type="primary"
          size="small"
          @click="handleEditFromDetail"
        >
          编辑
        </el-button>
      </template>
      <div v-if="detailData" class="product-detail-layout">
        <section :class="['product-detail-zone', 'product-detail-zone--overview', { 'product-detail-zone--danger': detailData.status === 0 }]">
          <header class="product-detail-zone__header">
            <span class="product-detail-zone__kicker">产品汇总</span>
            <p class="product-detail-zone__intro">先看状态、设备规模和最近上报。</p>
          </header>

          <div class="product-detail-overview-grid">
            <article :class="['product-detail-overview-lead', { 'product-detail-overview-lead--danger': detailData.status === 0 }]">
              <span class="product-detail-overview-lead__eyebrow">当前判断</span>
              <strong class="product-detail-overview-lead__title">{{ detailOperationHeadline }}</strong>
              <p class="product-detail-overview-lead__text">{{ detailOperationSummary }}</p>
              <div class="product-detail-overview-progress">
                <div class="product-detail-overview-progress__track">
                  <span class="product-detail-overview-progress__fill" :style="{ width: `${detailOnlineRatioPercent}%` }" />
                </div>
                <span class="product-detail-overview-progress__caption">在线设备占关联设备的比例</span>
              </div>
              <div class="product-detail-overview-lead__meta">
                <span>产品状态：{{ getStatusText(detailData.status) }}</span>
                <span>当前阶段：{{ detailLifecycleStage }}</span>
              </div>
            </article>

            <div class="product-detail-overview-metrics">
              <article v-for="metric in detailSummaryMetrics" :key="metric.key" class="product-detail-overview-metric">
                <span class="product-detail-overview-metric__label">{{ metric.label }}</span>
                <div class="product-detail-overview-metric__value-wrapper">
                  <strong class="product-detail-overview-metric__value">{{ metric.value }}</strong>
                  <span v-if="metric.trend" :class="['product-detail-overview-metric__trend', `product-detail-overview-metric__trend--${metric.trendType}`]">
                    {{ metric.trend }}
                  </span>
                </div>
                <p class="product-detail-overview-metric__hint">{{ metric.hint }}</p>
              </article>
            </div>
          </div>
        </section>

        <section class="product-detail-zone product-detail-zone--ledger">
          <div class="product-detail-ledger-grid">
            <article class="product-detail-ledger-card product-detail-ledger-card--contract">
              <header class="product-detail-card-header">
                <h3>接入契约</h3>
                <p>核对协议、节点类型和上报格式。</p>
              </header>
              <div class="product-detail-contract-list">
                <article v-for="item in detailContractCards" :key="item.key" class="product-detail-contract-item">
                  <span class="product-detail-contract-item__label">{{ item.label }}</span>
                  <strong class="product-detail-contract-item__value" :title="item.value">{{ item.value }}</strong>
                </article>
              </div>
            </article>

            <article class="product-detail-ledger-card product-detail-ledger-card--archive">
              <header class="product-detail-card-header">
                <h3>产品档案</h3>
                <p>核对编号、Key、厂商和建档时间。</p>
              </header>
              <div class="product-detail-archive-grid">
                <article class="product-detail-archive-item product-detail-archive-item--full">
                  <div class="product-detail-archive-meta-row">
                    <span class="product-detail-archive-item__label">厂商</span>
                    <span class="product-detail-archive-meta-separator">|</span>
                    <span class="product-detail-archive-item__label">产品编号</span>
                  </div>
                  <div class="product-detail-archive-meta-value">
                    <strong class="product-detail-archive-item__value" :title="detailArchiveManufacturerText">{{ detailArchiveManufacturerText }}</strong>
                    <span class="product-detail-archive-meta-separator">/</span>
                    <strong class="product-detail-archive-item__value" :title="detailArchiveIdText">{{ detailArchiveIdText }}</strong>
                  </div>
                </article>
                <article class="product-detail-archive-item product-detail-archive-item--full">
                  <div class="product-detail-archive-meta-row">
                    <span class="product-detail-archive-item__label">产品 Key</span>
                    <span class="product-detail-archive-meta-separator">|</span>
                    <span class="product-detail-archive-item__label">创建时间</span>
                  </div>
                  <div class="product-detail-archive-meta-value">
                    <strong class="product-detail-archive-item__value" :title="detailArchiveProductKeyText">{{ detailArchiveProductKeyText }}</strong>
                    <span class="product-detail-archive-meta-separator">/</span>
                    <strong class="product-detail-archive-item__value">{{ detailArchiveCreateDateText }}</strong>
                  </div>
                </article>
              </div>
              <article class="product-detail-description-card">
                <span class="product-detail-description-card__label">产品说明</span>
                <strong class="product-detail-description-card__value">{{ detailDescriptionText }}</strong>
              </article>
            </article>
          </div>
        </section>

        <section class="product-detail-zone product-detail-zone--governance">
          <header class="product-detail-zone__header">
            <span class="product-detail-zone__kicker">维护与治理</span>
            <p class="product-detail-zone__intro">建议、规则和变更前检查分层展示。</p>
          </header>
          <div class="product-detail-governance-grid">
            <article
              :class="[
                'product-detail-governance-card',
                'product-detail-governance-card--lead',
                { 'product-detail-governance-card--danger': detailData.status === 0 }
              ]"
            >
              <span class="product-detail-governance-card__label">当前建议</span>
              <strong class="product-detail-governance-card__title">{{ detailGovernanceHeadline }}</strong>
              <p class="product-detail-governance-card__text">{{ detailGovernanceNotice }}</p>
            </article>

            <article class="product-detail-governance-card">
              <span class="product-detail-governance-card__label">维护规则</span>
              <ul class="product-detail-governance-list">
                <li v-for="item in detailMaintenanceRules" :key="item">{{ item }}</li>
              </ul>
            </article>

            <article class="product-detail-governance-card">
              <span class="product-detail-governance-card__label">变更前确认</span>
              <ul class="product-detail-governance-list">
                <li v-for="item in detailChangeChecklist" :key="item">{{ item }}</li>
              </ul>
            </article>
          </div>
        </section>
      </div>

      <template #footer>
        <StandardDrawerFooter @cancel="detailVisible = false">
          <el-button class="standard-drawer-footer__button standard-drawer-footer__button--ghost" @click="detailVisible = false">
            关闭
          </el-button>
          <el-button
            type="primary"
            class="standard-drawer-footer__button standard-drawer-footer__button--primary"
            :disabled="!detailData?.productKey"
            @click="handleJumpToDevices(detailData)"
          >
            查看设备
          </el-button>
        </StandardDrawerFooter>
      </template>
    </StandardDetailDrawer>

    <StandardFormDrawer
      v-model="formVisible"
      :title="formTitle"
      size="42rem"
      @close="handleFormClose"
    >
      <div class="ops-drawer-stack">
        <el-form ref="formRef" :model="formData" :rules="formRules" label-position="top" class="ops-drawer-form">
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <h3>基础档案</h3>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="产品 Key" prop="productKey">
                <el-input
                  id="product-key"
                  v-model="formData.productKey"
                  :disabled="Boolean(editingProductId)"
                  placeholder="请输入产品 Key，例如 accept-http-product-01"
                />
              </el-form-item>
              <el-form-item label="产品名称" prop="productName">
                <el-input id="product-name" v-model="formData.productName" placeholder="请输入产品名称" />
              </el-form-item>
              <el-form-item label="厂商">
                <el-input v-model="formData.manufacturer" placeholder="请输入厂商名称" />
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <h3>接入基线</h3>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="协议编码" prop="protocolCode">
                <el-input id="protocol-code" v-model="formData.protocolCode" placeholder="请输入协议编码，例如 mqtt-json" />
              </el-form-item>
              <el-form-item label="节点类型" prop="nodeType">
                <el-select v-model="formData.nodeType" placeholder="请选择节点类型">
                  <el-option label="直连设备" :value="1" />
                  <el-option label="网关设备" :value="2" />
                </el-select>
              </el-form-item>
              <el-form-item label="数据格式">
                <el-input id="data-format" v-model="formData.dataFormat" placeholder="请输入数据格式，例如 JSON" />
              </el-form-item>
              <el-form-item label="产品状态">
                <el-select v-model="formData.status" placeholder="请选择产品状态">
                  <el-option label="启用" :value="1" />
                  <el-option label="停用" :value="0" />
                </el-select>
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <h3>补充说明</h3>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="说明" class="ops-drawer-grid__full">
                <el-input v-model="formData.description" type="textarea" :rows="5" placeholder="请输入产品说明、接入约束或适用场景" />
              </el-form-item>
            </div>
          </section>
        </el-form>
      </div>

      <template #footer>
        <StandardDrawerFooter
          :confirm-loading="submitLoading"
          :confirm-text="submitButtonText"
          @cancel="formVisible = false"
          @confirm="handleSubmit"
        >
          <el-button class="standard-drawer-footer__button standard-drawer-footer__button--ghost" @click="formVisible = false">
            取消
          </el-button>
          <el-button
            id="product-submit-button"
            v-permission="submitPermission"
            type="primary"
            class="standard-drawer-footer__button standard-drawer-footer__button--primary"
            :loading="submitLoading"
            @click="handleSubmit"
          >
            {{ submitButtonText }}
          </el-button>
        </StandardDrawerFooter>
      </template>
    </StandardFormDrawer>

    <CsvColumnSettingDialog
      v-model="exportColumnDialogVisible"
      title="产品导出列设置"
      :options="exportColumnOptions"
      :selected-keys="selectedExportColumnKeys"
      :preset-storage-key="exportColumnStorageKey"
      :presets="exportPresets"
      @confirm="handleExportColumnConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type TableInstance } from 'element-plus'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import PanelCard from '@/components/PanelCard.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import { productApi } from '@/api/product'
import { ElMessageBox } from 'element-plus'
import { useServerPagination } from '@/composables/useServerPagination'
import type { PageResult, Product, ProductAddPayload } from '@/types/api'
import {
  buildProductPageCacheKey,
  cloneProductDetailCacheEntry,
  cloneProductPageCacheEntry,
  createProductDetailCacheEntry,
  createProductPageCacheEntry,
  deserializeProductDetailCacheEntries,
  deserializeProductPageCacheEntries,
  getNextProductPageQuery,
  getProductRowKey,
  isProductDetailCacheFresh,
  isProductPageCacheFresh,
  matchesProductFilters,
  mergeLocalProductRow,
  prependLocalProductRow,
  type ProductDetailCacheEntry,
  type ProductPageCacheEntry,
  type ProductPageQuerySnapshot,
  removeLocalProductRow,
  removeSelectedProductSnapshot,
  resolveProductPageLoadStrategy,
  replaceSelectedProductSnapshot,
  serializeProductDetailCacheEntries,
  serializeProductPageCacheEntries,
  shouldRefreshProductDetail
} from '@/views/productWorkbenchState'
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv'
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns'
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm'
import { formatDateTime } from '@/utils/format'

function formatCount(value?: number | null) {
  const count = Number(value)
  return Number.isFinite(count) ? String(count) : '--'
}

function formatFullDateTime(value?: string | null) {
  if (!value) {
    return '--'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(date)
}

interface ProductSearchForm {
  productName: string
  nodeType: number | undefined
  status: number | undefined
}

type ProductFilterKey = keyof ProductSearchForm

interface ProductFormState extends ProductAddPayload {}

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const formRef = ref<FormInstance>()

const loading = ref(false)
const submitLoading = ref(false)
const formVisible = ref(false)
const formRefreshing = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailRefreshing = ref(false)
const detailErrorMessage = ref('')
const listRefreshMessage = ref('')
const listRefreshState = ref<'info' | 'error' | ''>('')
const formRefreshMessage = ref('')
const formRefreshState = ref<'info' | 'warning' | 'error' | ''>('')
// detailRefreshErrorMessage 已移除，不再
const editingProductId = ref<string | number | null>(null)

const tableData = ref<Product[]>([])
const selectedRows = ref<Product[]>([])
const detailData = ref<Product | null>(null)

const exportColumnDialogVisible = ref(false)
const exportColumnStorageKey = 'product-definition-center'
const defaultPageSize = 10
let latestListRequestId = 0
let latestDetailRequestId = 0
let latestEditRequestId = 0
let listAbortController: AbortController | null = null
let listPrefetchAbortController: AbortController | null = null
let detailAbortController: AbortController | null = null
let editAbortController: AbortController | null = null
const productDetailCache = new Map<string, ProductDetailCacheEntry>()
const productPageCache = new Map<string, ProductPageCacheEntry>()
const productDetailCacheTtlMs = 5 * 60_000
const productDetailCacheLimit = 12
const productDetailCacheSessionStorageKey = 'iot.products.detail-cache'
const productPageCacheTtlMs = 30_000
const productPageCacheLimit = 8
const productPageCacheSessionStorageKey = 'iot.products.page-cache'
let activeEditSessionId = 0
let formDirtySinceOpen = false
let suppressFormDirtyTracking = false

const searchForm = reactive<ProductSearchForm>({
  productName: '',
  nodeType: undefined,
  status: undefined
})
const appliedFilters = reactive<ProductSearchForm>({
  productName: '',
  nodeType: undefined,
  status: undefined
})

// 快速搜索关键词
const quickSearchKeyword = ref('')

// 视图类型：table 或 card
const viewType = ref<'table' | 'card'>('table')

const viewTypeName = computed(() => {
  return viewType.value === 'table' ? '表格视图' : '卡片视图'
})

function handleViewTypeChange(command: string) {
  if (command === 'table' || command === 'card') {
    viewType.value = command
  }
}

const createDefaultFormData = (): ProductFormState => ({
  productKey: '',
  productName: '',
  protocolCode: 'mqtt-json',
  nodeType: 1,
  dataFormat: 'JSON',
  manufacturer: '',
  description: '',
  status: 1
})

const formData = reactive<ProductFormState>(createDefaultFormData())

const { pagination, applyPageResult, resetPage, setPageNum, setPageSize, setTotal } = useServerPagination(defaultPageSize)

const formTitle = computed(() => (editingProductId.value ? '编辑产品' : '新增产品'))
const submitButtonText = computed(() => (editingProductId.value ? '保存' : '新增'))
const submitPermission = computed(() => (editingProductId.value ? 'iot:products:update' : 'iot:products:add'))
const detailTitle = computed(() => detailData.value?.productName || detailData.value?.productKey || '产品详情')
const detailSubtitle = computed(() => '按汇总、接入方式、档案信息和维护建议四个板块查看。')
const enabledProductCount = computed(() => tableData.value.filter((item) => item.status !== 0).length)
const disabledProductCount = computed(() => tableData.value.filter((item) => item.status === 0).length)
const hasRecords = computed(() => tableData.value.length > 0)
const showListSkeleton = computed(() => loading.value && !hasRecords.value)
const showListInlineState = computed(() => Boolean(listRefreshMessage.value) && hasRecords.value)
const activeFilterTags = computed(() => {
  const tags: Array<{ key: ProductFilterKey; label: string }> = []
  const productName = appliedFilters.productName.trim()
  if (productName) {
    tags.push({ key: 'productName', label: `产品名称：${productName}` })
  }
  if (appliedFilters.nodeType !== undefined) {
    tags.push({ key: 'nodeType', label: `节点类型：${getNodeTypeText(appliedFilters.nodeType)}` })
  }
  if (appliedFilters.status !== undefined) {
    tags.push({ key: 'status', label: `产品状态：${getStatusText(appliedFilters.status)}` })
  }
  return tags
})
const hasAppliedFilters = computed(() => activeFilterTags.value.length > 0)
const emptyStateTitle = computed(() => (hasAppliedFilters.value ? '没有符合条件的产品' : '还没有产品定义'))
const emptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? '已生效筛选暂时没有匹配结果，可以调整条件，或者直接清空当前筛选。'
    : '当前还没有产品定义，先新增产品，再继续设备接入、建档和维护。'
)
const detailDescriptionText = computed(
  () =>
    detailData.value?.description?.trim() ||
    '当前没有补充说明，可结合接入方式、设备规模和维护建议判断是否继续使用。'
)
const detailAssociationHint = computed(() => {
  const deviceCount = parseCount(detailData.value?.deviceCount)
  const onlineCount = parseCount(detailData.value?.onlineDeviceCount)
  if (deviceCount === null || deviceCount === 0) {
    return '当前还没有关联设备。'
  }
  if (onlineCount === null) {
    return `当前有 ${deviceCount} 台关联设备。`
  }
  return `当前有 ${deviceCount} 台关联设备，在线 ${onlineCount} 台。`
})
const detailLastReportHint = computed(() =>
  detailData.value?.lastReportTime ? '最近一次设备上报时间。' : '当前还没有收到设备上报。'
)
const detailOnlineRatioText = computed(() => {
  const deviceCount = parseCount(detailData.value?.deviceCount)
  const onlineCount = parseCount(detailData.value?.onlineDeviceCount)
  if (deviceCount === null || deviceCount <= 0 || onlineCount === null) {
    return '--'
  }
  return `${Math.round((onlineCount / deviceCount) * 100)}%`
})
const detailOnlineRatioPercent = computed(() => {
  const deviceCount = parseCount(detailData.value?.deviceCount)
  const onlineCount = parseCount(detailData.value?.onlineDeviceCount)
  if (deviceCount === null || deviceCount <= 0 || onlineCount === null) {
    return 0
  }
  return Math.min(100, Math.max(0, Math.round((onlineCount / deviceCount) * 100)))
})
const detailLifecycleStage = computed(() => {
  if (!detailData.value) {
    return '--'
  }
  const deviceCount = parseCount(detailData.value.deviceCount)
  if (detailData.value.status === 0) {
    return '已停用'
  }
  if ((deviceCount ?? 0) > 0) {
    return '稳定使用中'
  }
  return '接入调试中'
})
const detailOperationHeadline = computed(() => {
  if (!detailData.value) {
    return '正在加载产品信息'
  }
  if (detailData.value.status === 0) {
    return '这个产品当前已停用'
  }
  const deviceCount = parseCount(detailData.value.deviceCount)
  const onlineCount = parseCount(detailData.value.onlineDeviceCount)
  if ((deviceCount ?? 0) === 0) {
    return '这个产品还在接入准备阶段'
  }
  if ((onlineCount ?? 0) > 0) {
    return '这个产品下还有设备在线'
  }
  return '这个产品下有设备，但当前都不在线'
})
const detailGovernanceNotice = computed(() => {
  if (!detailData.value) {
    return '当前没有维护建议。'
  }
  if (detailData.value.status === 0) {
    return '当前产品已停用，新增设备、设备替换、设备上报和指令下发都会被系统拦截。'
  }
  const deviceCount = parseCount(detailData.value.deviceCount)
  const onlineCount = parseCount(detailData.value.onlineDeviceCount)
  if ((deviceCount ?? 0) > 0 || (onlineCount ?? 0) > 0) {
    return '当前已有现场设备在使用这个产品。修改协议、节点类型或数据格式前，请先确认兼容性，避免影响现网设备。'
  }
  return '当前还没有设备正式使用，可以继续做接入联调；如需调整 Product Key 或协议规则，建议先确认命名和边界。'
})
const detailOperationSummary = computed(() => {
  if (!detailData.value) {
    return '正在整理当前产品的状态、接入方式和维护信息。'
  }
  if (detailData.value.status === 0) {
    return '先确认是否还有设备在用，再决定要不要继续保留这条产品定义。'
  }
  const deviceCount = parseCount(detailData.value.deviceCount)
  if ((deviceCount ?? 0) === 0) {
    return '当前还没有设备使用，适合继续做接入联调和模板整理。'
  }
  return '当前已经有设备在用，变更前先评估对现场设备的影响。'
})
const detailSummaryMetrics = computed(() => {
  const baseMetrics = [
    {
      key: 'deviceCount',
      label: '关联设备数',
      value: formatCount(detailData.value?.deviceCount),
      hint: detailAssociationHint.value
    },
    {
      key: 'onlineDeviceCount',
      label: '在线设备数',
      value: formatCount(detailData.value?.onlineDeviceCount),
      hint: '当前在线的设备数量。'
    },
    {
      key: 'onlineRatio',
      label: '在线比例',
      value: detailOnlineRatioText.value,
      hint: parseCount(detailData.value?.deviceCount) ? '在线设备在全部关联设备中的比例' : '当前没有设备，暂不统计'
    },
    {
      key: 'lastReportTime',
      label: '最近上报',
      value: formatDateTime(detailData.value?.lastReportTime),
      hint: detailLastReportHint.value
    }
  ]
  
  if (detailData.value?.deviceCount != null && detailData.value.deviceCount > 0) {
    return [
      ...baseMetrics,
      {
        key: 'offlineRate',
        label: '离线比例',
        value: `${(1 - (parseCount(detailData.value?.onlineDeviceCount) / parseCount(detailData.value?.deviceCount)) * 100).toFixed(1)}%`,
        hint: '离线设备在全部设备中的比例'
      }
    ]
  }
  return baseMetrics
})
const detailContractCards = computed(() => [
  { key: 'protocolCode', label: '协议编码', value: formatTextValue(detailData.value?.protocolCode) },
  { key: 'nodeType', label: '节点类型', value: getNodeTypeText(detailData.value?.nodeType) },
  { key: 'dataFormat', label: '数据格式', value: formatTextValue(detailData.value?.dataFormat) }
])
const detailArchiveIdText = computed(() => formatTextValue(detailData.value?.id))
const detailArchiveProductKeyText = computed(() => formatTextValue(detailData.value?.productKey))
const detailArchiveManufacturerText = computed(() => formatTextValue(detailData.value?.manufacturer))
const detailArchiveCreateDateText = computed(() => formatFullDateTime(detailData.value?.createTime))
const detailGovernanceHeadline = computed(() => {
  if (!detailData.value) {
    return '正在整理维护建议'
  }
  if (detailData.value.status === 0) {
    return '先核查停用对现有设备的影响'
  }
  const deviceCount = parseCount(detailData.value.deviceCount)
  if ((deviceCount ?? 0) === 0) {
    return '当前可继续作为新设备接入模板'
  }
  return '当前已有设备在用，变更前先做影响评估'
})
const detailMaintenanceRules = computed(() => [
  '产品 Key 建立后尽量保持稳定，不建议直接改名。',
  '协议编码、节点类型和数据格式属于接入核心规则。',
  '调整时要兼顾历史日志、设备替换和接入检索的一致性。'
])
const detailChangeChecklist = computed(() => [
  '先确认现场是否已经有设备在使用。',
  '再确认协议或物模型变化是否需要新建产品版本。',
  '最后确认调整后不会影响设备建档和上报链路。'
])

const formRules: FormRules<ProductFormState> = {
  productKey: [{ required: true, message: '请输入产品 Key', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  protocolCode: [{ required: true, message: '请输入协议编码', trigger: 'blur' }],
  nodeType: [{ required: true, message: '请选择节点类型', trigger: 'change' }]
}

const exportColumns: CsvColumn<Product>[] = [
  { key: 'id', label: '产品 ID' },
  { key: 'productKey', label: '产品 Key' },
  { key: 'productName', label: '产品名称' },
  { key: 'protocolCode', label: '协议编码' },
  { key: 'nodeType', label: '节点类型', formatter: (value) => getNodeTypeText(Number(value)) },
  { key: 'status', label: '产品状态', formatter: (value) => getStatusText(Number(value)) },
  { key: 'dataFormat', label: '数据格式' },
  { key: 'manufacturer', label: '厂商' },
  { key: 'onlineDeviceCount', label: '在线设备数' },
  { key: 'lastReportTime', label: '最近设备上报', formatter: (value) => formatDateTime(String(value || '')) },
  { key: 'createTime', label: '创建时间', formatter: (value) => formatDateTime(String(value || '')) },
  { key: 'updateTime', label: '更新时间', formatter: (value) => formatDateTime(String(value || '')) }
]

const exportColumnOptions = toCsvColumnOptions(exportColumns)
const exportPresets = [
  { label: '默认模板', keys: exportColumns.map((column) => String(column.key)) },
  { label: '状态核查', keys: ['productKey', 'productName', 'status', 'onlineDeviceCount', 'lastReportTime'] },
  {
    label: '基础档案',
    keys: ['id', 'productKey', 'productName', 'protocolCode', 'nodeType', 'dataFormat', 'manufacturer', 'createTime', 'updateTime']
  }
]
const selectedExportColumnKeys = ref<string[]>(
  loadCsvColumnSelection(
    exportColumnStorageKey,
    exportColumns.map((column) => String(column.key))
  )
)
const selectedRowKeySet = computed(() => new Set(selectedRows.value.map((item) => getProductRowKey(item)).filter(Boolean)))

function getNodeTypeText(value?: number | null) {
  if (value === 1) {
    return '直连设备'
  }
  if (value === 2) {
    return '网关设备'
  }
  return '--'
}

function getStatusText(value?: number | null) {
  return value === 0 ? '停用' : '启用'
}

function parseCount(value?: number | null) {
  const count = Number(value)
  return Number.isFinite(count) ? count : null
}

function formatTextValue(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '--'
  }
  return String(value)
}

function formatDate(value?: string | null) {
  if (!value) {
    return '--'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(date)
}

function getProductDetailCacheKey(row?: Partial<Product> | null) {
  return getProductRowKey(row)
}

function getCachedProductDetail(row?: Partial<Product> | null) {
  const cacheKey = getProductDetailCacheKey(row)
  if (!cacheKey) {
    return null
  }
  const entry = cloneProductDetailCacheEntry(productDetailCache.get(cacheKey))
  if (!entry) {
    return null
  }
  if (!isProductDetailCacheFresh(entry, productDetailCacheTtlMs)) {
    productDetailCache.delete(cacheKey)
    persistProductDetailCache()
    return null
  }
  return { ...entry.detail }
}

function cacheProductDetail(product?: Product | null) {
  const cacheKey = getProductDetailCacheKey(product)
  if (!cacheKey || !product) {
    return
  }
  const entry = createProductDetailCacheEntry(product)
  productDetailCache.delete(cacheKey)
  productDetailCache.set(cacheKey, entry)

  while (productDetailCache.size > productDetailCacheLimit) {
    const oldestKey = productDetailCache.keys().next().value
    if (!oldestKey) {
      break
    }
    productDetailCache.delete(oldestKey)
  }

  persistProductDetailCache()
}

function removeCachedProductDetail(row?: Partial<Product> | null) {
  const cacheKey = getProductDetailCacheKey(row)
  if (!cacheKey) {
    return
  }
  productDetailCache.delete(cacheKey)
  persistProductDetailCache()
}

function resolveDetailSnapshot(row: Product, cachedDetail: Product | null) {
  // 使用列表返回的 row 数据作为快照，确保详情页始终有数据显示
  return {
    ...row,
    description: cachedDetail?.description ?? row.description ?? null
  }
}

function abortListRequest() {
  listAbortController?.abort()
  listAbortController = null
}

function abortListPrefetchRequest() {
  listPrefetchAbortController?.abort()
  listPrefetchAbortController = null
}

function abortDetailRequest() {
  detailAbortController?.abort()
  detailAbortController = null
}

function abortEditRequest() {
  editAbortController?.abort()
  editAbortController = null
}

function resetFormData(source?: Partial<Product>) {
  Object.assign(formData, createDefaultFormData(), {
    productKey: source?.productKey || '',
    productName: source?.productName || '',
    protocolCode: source?.protocolCode || 'mqtt-json',
    nodeType: source?.nodeType ?? 1,
    dataFormat: source?.dataFormat || 'JSON',
    manufacturer: source?.manufacturer || '',
    description: source?.description || '',
    status: source?.status ?? 1
  })
}

function applyFormDataWithoutDirty(source?: Partial<Product>) {
  suppressFormDirtyTracking = true
  try {
    resetFormData(source)
  } finally {
    suppressFormDirtyTracking = false
  }
}

function clearFormRefreshState() {
  formRefreshing.value = false
  formRefreshMessage.value = ''
  formRefreshState.value = ''
}

function clearListRefreshState() {
  listRefreshMessage.value = ''
  listRefreshState.value = ''
}

function isAbortError(error: unknown) {
  return error instanceof Error && error.name === 'AbortError'
}

function buildCurrentProductPageQuery(): ProductPageQuerySnapshot {
  return {
    productName: searchForm.productName.trim(),
    nodeType: searchForm.nodeType,
    status: searchForm.status,
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  }
}

function getProductPageSessionStorage() {
  if (typeof window === 'undefined') {
    return null
  }
  return window.sessionStorage
}

function hydrateProductPageCache() {
  const storage = getProductPageSessionStorage()
  if (!storage) {
    return
  }

  const entries = deserializeProductPageCacheEntries(
    storage.getItem(productPageCacheSessionStorageKey),
    productPageCacheTtlMs,
    productPageCacheLimit
  )

  productPageCache.clear()
  entries.forEach((entry: ProductPageCacheEntry) => {
    productPageCache.set(entry.key, entry)
  })

  if (entries.length === 0) {
    try {
      storage.removeItem(productPageCacheSessionStorageKey)
    } catch {
      // 忽略浏览器存储异常，避免阻断页面加载
    }
  }
}

function hydrateProductDetailCache() {
  const storage = getProductPageSessionStorage()
  if (!storage) {
    return
  }

  const entries = deserializeProductDetailCacheEntries(
    storage.getItem(productDetailCacheSessionStorageKey),
    productDetailCacheTtlMs,
    productDetailCacheLimit
  )

  productDetailCache.clear()
  entries.forEach((entry: ProductDetailCacheEntry) => {
    productDetailCache.set(entry.key, entry)
  })

  if (entries.length === 0) {
    try {
      storage.removeItem(productDetailCacheSessionStorageKey)
    } catch {
      // 忽略浏览器存储异常，避免阻断页面加载
    }
  }
}

function persistProductPageCache() {
  const storage = getProductPageSessionStorage()
  if (!storage) {
    return
  }

  try {
    if (productPageCache.size === 0) {
      storage.removeItem(productPageCacheSessionStorageKey)
      return
    }
    storage.setItem(
      productPageCacheSessionStorageKey,
      serializeProductPageCacheEntries(productPageCache.values(), productPageCacheLimit)
    )
  } catch {
    // 忽略浏览器存储异常，避免阻断列表主流程
  }
}

function persistProductDetailCache() {
  const storage = getProductPageSessionStorage()
  if (!storage) {
    return
  }

  try {
    if (productDetailCache.size === 0) {
      storage.removeItem(productDetailCacheSessionStorageKey)
      return
    }
    storage.setItem(
      productDetailCacheSessionStorageKey,
      serializeProductDetailCacheEntries(productDetailCache.values(), productDetailCacheLimit)
    )
  } catch {
    // 忽略浏览器存储异常，避免阻断详情主流程
  }
}

function getCachedProductPage(query: ProductPageQuerySnapshot) {
  const cacheKey = buildProductPageCacheKey(query)
  return cloneProductPageCacheEntry(productPageCache.get(cacheKey))
}

function cacheProductPage(query: ProductPageQuerySnapshot, pageResult: PageResult<Product>) {
  const entry = createProductPageCacheEntry(query, pageResult)
  productPageCache.delete(entry.key)
  productPageCache.set(entry.key, entry)

  while (productPageCache.size > productPageCacheLimit) {
    const oldestKey = productPageCache.keys().next().value
    if (!oldestKey) {
      break
    }
    productPageCache.delete(oldestKey)
  }

  persistProductPageCache()
}

function applyCachedProductPage(entry: ProductPageCacheEntry) {
  tableData.value = applyPageResult({
    total: entry.total,
    pageNum: entry.pageNum,
    pageSize: entry.pageSize,
    records: entry.records
  })
  syncAppliedFilters()
  void syncTableSelection()
}

function cacheVisibleProductPage() {
  cacheProductPage(buildCurrentProductPageQuery(), {
    total: pagination.total,
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    records: tableData.value
  })
}

function clearProductPageCache() {
  abortListPrefetchRequest()
  productPageCache.clear()
  persistProductPageCache()
}

function rebuildVisibleProductPageCache() {
  clearProductPageCache()
  if (pagination.total <= 0 || tableData.value.length === 0) {
    return
  }
  cacheVisibleProductPage()
}

async function prefetchNextProductPage(query: ProductPageQuerySnapshot, total: number) {
  const nextQuery = getNextProductPageQuery(query, total)
  if (!nextQuery) {
    return
  }

  const cachedPage = getCachedProductPage(nextQuery)
  if (isProductPageCacheFresh(cachedPage, productPageCacheTtlMs)) {
    return
  }

  abortListPrefetchRequest()
  const controller = new AbortController()
  listPrefetchAbortController = controller

  try {
    const res = await productApi.pageProducts(
      {
        productName: nextQuery.productName || undefined,
        nodeType: nextQuery.nodeType,
        status: nextQuery.status,
        pageNum: nextQuery.pageNum,
        pageSize: nextQuery.pageSize
      },
      {
        signal: controller.signal
      }
    )
    if (res.code === 200 && res.data) {
      cacheProductPage(nextQuery, res.data)
    }
  } catch (error) {
    if (!isAbortError(error)) {
      console.warn('预取产品分页失败', error)
    }
  } finally {
    if (listPrefetchAbortController === controller) {
      listPrefetchAbortController = null
    }
  }
}

function syncAppliedFilters() {
  appliedFilters.productName = searchForm.productName.trim()
  appliedFilters.nodeType = searchForm.nodeType
  appliedFilters.status = searchForm.status
}

function clearSearchForm() {
  searchForm.productName = ''
  searchForm.nodeType = undefined
  searchForm.status = undefined
}

function clearSelection() {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

function matchesCurrentFilters(product: Product) {
  return matchesProductFilters(product, {
    productName: searchForm.productName,
    nodeType: searchForm.nodeType,
    status: searchForm.status
  })
}

// 快速搜索：支持产品名称、厂商关键词搜索
function handleQuickSearch() {
  const keyword = quickSearchKeyword.value.trim()
  if (!keyword) {
    return
  }
  
  // 将快速搜索关键词同步到产品名称搜索框（用于名称和厂商搜索）
  searchForm.productName = keyword
  resetPage()
  clearSelection()
  void syncListRouteQuery()
}

function handleClearQuickSearch() {
  quickSearchKeyword.value = ''
  // 清除快速搜索标签，但保留其他筛选条件
}

function replaceSelectedRowSnapshot(product: Product) {
  selectedRows.value = replaceSelectedProductSnapshot(selectedRows.value, product)
}

function removeSelectedRowSnapshot(row?: Partial<Product> | null) {
  selectedRows.value = removeSelectedProductSnapshot(selectedRows.value, row)
}

function mergeLocalTableRow(product: Product) {
  const nextRows = mergeLocalProductRow(tableData.value, product)
  if (!nextRows) {
    return false
  }

  tableData.value = nextRows
  cacheVisibleProductPage()
  replaceSelectedRowSnapshot(product)
  void syncTableSelection()
  return true
}

function prependLocalTableRow(product: Product) {
  tableData.value = prependLocalProductRow(tableData.value, product, pagination.pageSize)
  cacheVisibleProductPage()
  replaceSelectedRowSnapshot(product)
  void syncTableSelection()
}

function removeLocalTableRow(row?: Partial<Product> | null) {
  const nextRows = removeLocalProductRow(tableData.value, row)
  if (!nextRows) {
    return false
  }

  tableData.value = nextRows
  cacheVisibleProductPage()
  removeSelectedRowSnapshot(row)
  void syncTableSelection()
  return true
}

async function syncTableSelection() {
  await nextTick()
  if (!tableRef.value) {
    return
  }
  tableRef.value.clearSelection()
  const selectedKeys = selectedRowKeySet.value
  tableData.value.forEach((row) => {
    if (selectedKeys.has(getProductRowKey(row))) {
      tableRef.value?.toggleRowSelection(row, true)
    }
  })
}

function handleSelectionChange(rows: Product[]) {
  selectedRows.value = rows
}

function isRowSelected(row: Product) {
  return selectedRowKeySet.value.has(getProductRowKey(row))
}

function handleMobileSelectionChange(row: Product, checked: boolean) {
  const rowKey = getProductRowKey(row)
  const nextRows = checked
    ? [...selectedRows.value.filter((item) => getProductRowKey(item) !== rowKey), row]
    : selectedRows.value.filter((item) => getProductRowKey(item) !== rowKey)
  selectedRows.value = tableData.value.filter((item) => nextRows.some((selected) => getProductRowKey(selected) === getProductRowKey(item)))
  void syncTableSelection()
}

function openExportColumnSetting() {
  exportColumnDialogVisible.value = true
}

function handleExportColumnConfirm(selectedKeys: string[]) {
  selectedExportColumnKeys.value = selectedKeys
  saveCsvColumnSelection(exportColumnStorageKey, selectedKeys)
}

function getResolvedExportColumns() {
  return resolveCsvColumns(exportColumns, selectedExportColumnKeys.value)
}

function handleExportSelected() {
  downloadRowsAsCsv('产品定义中心-选中项.csv', selectedRows.value, getResolvedExportColumns())
}

function handleExportCurrent() {
  downloadRowsAsCsv('产品定义中心-当前结果.csv', tableData.value, getResolvedExportColumns())
}

function applyRouteQueryToFilters() {
  searchForm.productName = typeof route.query.productName === 'string' ? route.query.productName.trim() : ''
  searchForm.nodeType = parseRouteNumberQuery(route.query.nodeType)
  searchForm.status = parseRouteNumberQuery(route.query.status)
  pagination.pageNum = parseRoutePositiveIntQuery(route.query.pageNum, 1)
  pagination.pageSize = parseRoutePositiveIntQuery(route.query.pageSize, defaultPageSize)
}

function parseRouteNumberQuery(value: unknown) {
  const raw = Array.isArray(value) ? value[0] : value
  if (typeof raw !== 'string' || raw.trim() === '') {
    return undefined
  }
  const parsed = Number(raw)
  return Number.isFinite(parsed) ? parsed : undefined
}

function parseRoutePositiveIntQuery(value: unknown, fallback: number) {
  const parsed = parseRouteNumberQuery(value)
  if (!parsed || parsed < 1) {
    return fallback
  }
  return Math.trunc(parsed)
}

function normalizeQueryValue(value: unknown) {
  const raw = Array.isArray(value) ? value[0] : value
  if (raw === undefined || raw === null || raw === '') {
    return undefined
  }
  return String(raw)
}

function assignListQueryValue(
  query: Record<string, unknown>,
  key: 'productName' | 'nodeType' | 'status' | 'pageNum' | 'pageSize',
  value: string | number | undefined
) {
  if (value === undefined || value === '') {
    delete query[key]
    return
  }
  query[key] = String(value)
}

function hasSameListRouteQuery(nextQuery: Record<string, unknown>) {
  return (
    normalizeQueryValue(route.query.productName) === normalizeQueryValue(nextQuery.productName) &&
    normalizeQueryValue(route.query.nodeType) === normalizeQueryValue(nextQuery.nodeType) &&
    normalizeQueryValue(route.query.status) === normalizeQueryValue(nextQuery.status) &&
    normalizeQueryValue(route.query.pageNum) === normalizeQueryValue(nextQuery.pageNum) &&
    normalizeQueryValue(route.query.pageSize) === normalizeQueryValue(nextQuery.pageSize)
  )
}

async function syncListRouteQuery() {
  const nextQuery: Record<string, unknown> = { ...route.query }
  const trimmedProductName = searchForm.productName.trim()

  assignListQueryValue(nextQuery, 'productName', trimmedProductName || undefined)
  assignListQueryValue(nextQuery, 'nodeType', searchForm.nodeType)
  assignListQueryValue(nextQuery, 'status', searchForm.status)
  assignListQueryValue(nextQuery, 'pageNum', pagination.pageNum > 1 ? pagination.pageNum : undefined)
  assignListQueryValue(nextQuery, 'pageSize', pagination.pageSize !== defaultPageSize ? pagination.pageSize : undefined)

  if (hasSameListRouteQuery(nextQuery)) {
    await loadProductPage()
    return
  }

  await router.replace({
    path: route.path,
    query: nextQuery
  })
}

function buildDetailPreview(row: Product) {
  return {
    ...row,
    description: row.description ?? null
  }
}

async function loadProductPage(options: { silent?: boolean; force?: boolean; silentMessage?: string } = {}) {
  const requestId = ++latestListRequestId
  const query = buildCurrentProductPageQuery()
  const cachedPage = getCachedProductPage(query)
  const loadStrategy = resolveProductPageLoadStrategy({
    hasCachedPage: Boolean(cachedPage),
    hasFreshCache: isProductPageCacheFresh(cachedPage, productPageCacheTtlMs),
    force: options.force === true,
    silent: options.silent === true
  })
  const hadVisibleResult = Boolean(cachedPage) || tableData.value.length > 0

  abortListPrefetchRequest()
  abortListRequest()
  if (cachedPage) {
    applyCachedProductPage(cachedPage)
  }

  if (loadStrategy.useFreshCacheOnly) {
    clearListRefreshState()
    loading.value = false
    void prefetchNextProductPage(query, cachedPage.total)
    return
  }

  const controller = new AbortController()
  listAbortController = controller
  const silent = loadStrategy.silentRequest
  const preserveVisibleResult = silent && hadVisibleResult

  if (preserveVisibleResult) {
    listRefreshState.value = 'info'
    listRefreshMessage.value = options.silentMessage || '已先展示当前结果，正在后台校验最新数据。'
  } else {
    clearListRefreshState()
  }

  loading.value = !preserveVisibleResult
  try {
    const res = await productApi.pageProducts(
      {
        productName: query.productName || undefined,
        nodeType: query.nodeType,
        status: query.status,
        pageNum: query.pageNum,
        pageSize: query.pageSize
      },
      {
        signal: controller.signal
      }
    )
    if (requestId !== latestListRequestId) {
      return
    }
    if (res.code === 200 && res.data) {
      clearListRefreshState()
      tableData.value = applyPageResult(res.data)
      syncAppliedFilters()
      cacheProductPage(query, res.data)
      void syncTableSelection()
      void prefetchNextProductPage(query, res.data.total)
    }
  } catch (error) {
    if (requestId !== latestListRequestId || isAbortError(error)) {
      return
    }
    console.error('获取产品分页失败', error)
    if (preserveVisibleResult) {
      listRefreshState.value = 'error'
      listRefreshMessage.value = '最新数据校验失败，当前先展示已有结果。'
    } else {
      clearListRefreshState()
      ElMessage.error('获取产品分页失败')
    }
  } finally {
    if (requestId === latestListRequestId) {
      loading.value = false
    }
    if (listAbortController === controller) {
      listAbortController = null
    }
  }
}

async function openDetail(row: Product) {
  const requestId = ++latestDetailRequestId
  const cachedDetail = getCachedProductDetail(row)
  const detailSnapshot = resolveDetailSnapshot(row, cachedDetail)
  abortDetailRequest()

  detailVisible.value = true
  detailLoading.value = false
  detailErrorMessage.value = ''
  detailData.value = detailSnapshot

  // 如果没有缓存或者需要刷新详情，则发起后台补数请求
  if (!cachedDetail && shouldRefreshProductDetail(row, cachedDetail)) {
    const controller = new AbortController()
    detailAbortController = controller
    detailRefreshing.value = true

    try {
      const res = await productApi.getProductById(row.id, {
        signal: controller.signal
      })
      if (requestId !== latestDetailRequestId) {
        return
      }
      if (res.code === 200 && res.data) {
        detailData.value = res.data
        cacheProductDetail(res.data)
      }
    } catch (error) {
      if (requestId !== latestDetailRequestId || isAbortError(error)) {
        return
      }
      // 静默失败，不显示红色提示，保持现有数据（detailSnapshot）
      console.warn('完整详情补充失败', error)
    } finally {
      if (requestId === latestDetailRequestId) {
        detailLoading.value = false
        detailRefreshing.value = false
      }
      if (detailAbortController === controller) {
        detailAbortController = null
      }
    }
  } else {
    detailRefreshing.value = false
  }
}

async function refreshEditableDetail(row: Product, editSessionId: number, cachedDetail: Product | null) {
  if (!shouldRefreshProductDetail(row, cachedDetail)) {
    clearFormRefreshState()
    return
  }

  const requestId = ++latestEditRequestId
  abortEditRequest()
  const controller = new AbortController()
  editAbortController = controller
  formRefreshing.value = true
  formRefreshState.value = 'info'
  formRefreshMessage.value = ''

  try {
    const res = await productApi.getProductById(row.id, {
      signal: controller.signal
    })
    if (
      requestId !== latestEditRequestId ||
      editSessionId !== activeEditSessionId ||
      editingProductId.value !== row.id
    ) {
      return
    }
    if (res.code === 200 && res.data) {
      cacheProductDetail(res.data)
      if (!formDirtySinceOpen) {
        applyFormDataWithoutDirty(res.data)
        formRef.value?.clearValidate()
        clearFormRefreshState()
      } else {
        formRefreshState.value = 'warning'
        formRefreshMessage.value = '最新档案已取回；你已开始编辑，当前未自动覆盖表单。'
      }
    }
  } catch (error) {
    if (
      requestId !== latestEditRequestId ||
      editSessionId !== activeEditSessionId ||
      editingProductId.value !== row.id ||
      isAbortError(error)
    ) {
      return
    }
    formRefreshState.value = 'error'
    formRefreshMessage.value =
      error instanceof Error ? `最新档案补充失败：${error.message}` : '最新档案补充失败，当前先保留已填入内容。'
  } finally {
    if (requestId === latestEditRequestId) {
      formRefreshing.value = false
    }
    if (editAbortController === controller) {
      editAbortController = null
    }
  }
}

function handleSearch() {
  searchForm.productName = searchForm.productName.trim()
  resetPage()
  clearSelection()
  void syncListRouteQuery()
}

function handleReset() {
  clearSearchForm()
  resetPage()
  clearSelection()
  void syncListRouteQuery()
}

function removeAppliedFilter(key: ProductFilterKey) {
  if (key === 'productName') {
    searchForm.productName = ''
  } else if (key === 'nodeType') {
    searchForm.nodeType = undefined
  } else {
    searchForm.status = undefined
  }
  resetPage()
  clearSelection()
  void syncListRouteQuery()
}

function handleClearAppliedFilters() {
  clearSearchForm()
  resetPage()
  clearSelection()
  void syncListRouteQuery()
}

function handleRefresh() {
  clearSelection()
  clearProductPageCache()
  void loadProductPage({ force: true })
}

function handleAdd() {
  activeEditSessionId += 1
  abortEditRequest()
  editingProductId.value = null
  formDirtySinceOpen = false
  clearFormRefreshState()
  applyFormDataWithoutDirty()
  formVisible.value = true
}

function handleEdit(row: Product) {
  const cachedDetail = getCachedProductDetail(row)
  const editSnapshot = resolveDetailSnapshot(row, cachedDetail)

  activeEditSessionId += 1
  const editSessionId = activeEditSessionId
  abortEditRequest()
  editingProductId.value = row.id
  formDirtySinceOpen = false
  clearFormRefreshState()
  applyFormDataWithoutDirty(editSnapshot)
  formVisible.value = true
  formRef.value?.clearValidate()
  void refreshEditableDetail(row, editSessionId, cachedDetail)
}

function handleOpenDetail(row: Product) {
  void openDetail(row)
}

function handleEditFromDetail() {
  if (!detailData.value?.id) {
    return
  }
  handleEdit(detailData.value)
}

function handleRowAction(command: string | number | object, row: Product) {
  if (command === 'devices') {
    handleJumpToDevices(row)
    return
  }
  if (command === 'delete') {
    void handleDelete(row)
  }
}

function handleJumpToDevices(row?: Product | null) {
  if (!row?.productKey) {
    return
  }
  void router.push({
    path: '/devices',
    query: {
      productKey: row.productKey
    }
  })
}

async function handleBatchCommand(command: string, rows: Product[]) {
  const rowCount = rows.length
  if (rowCount === 0) {
    return
  }

  try {
    if (command === 'enable') {
      // 批量启用确认
      await ElMessageBox.confirm(
        `确定要启用选中的 ${rowCount} 个产品吗？启用后可正常接入设备`,
        '确认启用',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )

      // 辅助函数：将 null 转换为 undefined
      function normalizeProductPayload(row: Product): ProductAddPayload {
        return {
          productKey: row.productKey,
          productName: row.productName,
          protocolCode: row.protocolCode,
          nodeType: row.nodeType,
          dataFormat: row.dataFormat ?? undefined,
          manufacturer: row.manufacturer ?? undefined,
          description: row.description ?? undefined,
          status: row.status ?? 1
        }
      }

      // 批量启用
      for (const row of rows) {
        await productApi.updateProduct(row.id, normalizeProductPayload({ ...row, status: 1 }))
      }
      ElMessage.success(`已启用 ${rowCount} 个产品`)
      rows.forEach((row) => {
        const updatedRow = { ...row, status: 1 }
        mergeLocalTableRow(updatedRow)
        replaceSelectedRowSnapshot(updatedRow)
      })
      void loadProductPage({ silent: true })
    } else if (command === 'disable') {
      // 批量停用确认
      await ElMessageBox.confirm(
        `确定要停用选中的 ${rowCount} 个产品吗？停用后将无法新增设备，但不影响现有设备`,
        '确认停用',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )

      // 辅助函数：将 null 转换为 undefined
      function normalizeProductPayload(row: Product): ProductAddPayload {
        return {
          productKey: row.productKey,
          productName: row.productName,
          protocolCode: row.protocolCode,
          nodeType: row.nodeType,
          dataFormat: row.dataFormat ?? undefined,
          manufacturer: row.manufacturer ?? undefined,
          description: row.description ?? undefined,
          status: row.status ?? 1
        }
      }

      // 批量停用
      for (const row of rows) {
        await productApi.updateProduct(row.id, normalizeProductPayload({ ...row, status: 0 }))
      }
      ElMessage.success(`已停用 ${rowCount} 个产品`)
      rows.forEach((row) => {
        const updatedRow = { ...row, status: 0 }
        mergeLocalTableRow(updatedRow)
        replaceSelectedRowSnapshot(updatedRow)
      })
      void loadProductPage({ silent: true })
    } else if (command === 'delete') {
      await handleDeleteBatch(rows)
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('批量操作失败', error)
    ElMessage.error(error instanceof Error ? error.message : '批量操作失败')
  }
}

async function handleDeleteBatch(rows: Product[]) {
  try {
    // 确认删除
    await ElMessageBox.confirm(
      `确定要删除选中的 ${rows.length} 个产品吗？此操作不可恢复`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 批量删除
    await Promise.all(rows.map((row) => productApi.deleteProduct(row.id)))
    ElMessage.success(`已删除 ${rows.length} 个产品`)

    rows.forEach((row) => {
      removeCachedProductDetail(row)
      removeLocalTableRow(row)
      removeSelectedRowSnapshot(row)
    })

    setTotal(pagination.total - rows.length)
    
    // 如果当前页没有数据了，翻到上一页
    if (tableData.value.length === 0 && pagination.pageNum > 1) {
      clearProductPageCache()
      setPageNum(pagination.pageNum - 1)
      clearSelection()
      await syncListRouteQuery()
      return
    }

    rebuildVisibleProductPageCache()
    if (tableData.value.length === 0) {
      clearSelection()
    }
    void loadProductPage({ silent: true, force: true })
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('批量删除产品失败', error)
    ElMessage.error(error instanceof Error ? error.message : '批量删除产品失败')
  }
}

async function handleDelete(row: Product) {
  try {
    await confirmDelete('产品', row.productName || row.productKey)
    await productApi.deleteProduct(row.id)
    ElMessage.success('删除成功')
    removeCachedProductDetail(row)
    const removedFromCurrentPage = removeLocalTableRow(row)
    setTotal(pagination.total - 1)
    if (tableData.value.length === 0 && pagination.pageNum > 1) {
      clearProductPageCache()
      setPageNum(pagination.pageNum - 1)
      clearSelection()
      await syncListRouteQuery()
      return
    }
    rebuildVisibleProductPageCache()
    if (!removedFromCurrentPage) {
      clearSelection()
    }
    void loadProductPage({ silent: true, force: true })
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('删除产品失败', error)
    ElMessage.error(error instanceof Error ? error.message : '删除产品失败')
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    if (editingProductId.value) {
      const res = await productApi.updateProduct(editingProductId.value, { ...formData })
      cacheProductDetail(res.data)
      if (matchesCurrentFilters(res.data)) {
        mergeLocalTableRow(res.data)
      } else {
        removeLocalTableRow(res.data)
      }
      rebuildVisibleProductPageCache()
      ElMessage.success('更新成功')
      formVisible.value = false
      void loadProductPage({ silent: true, force: true })
    } else {
      const res = await productApi.addProduct({ ...formData })
      cacheProductDetail(res.data)
      ElMessage.success('新增成功')
      formVisible.value = false
      clearSelection()

      if (pagination.pageNum === 1 && matchesCurrentFilters(res.data)) {
        prependLocalTableRow(res.data)
        setTotal(pagination.total + 1)
        rebuildVisibleProductPageCache()
        void loadProductPage({ silent: true, force: true })
      } else if (pagination.pageNum === 1) {
        clearProductPageCache()
        void loadProductPage({ silent: true })
      } else {
        clearProductPageCache()
        resetPage()
        await syncListRouteQuery()
      }
    }
  } catch (error) {
    console.error('提交产品失败', error)
    ElMessage.error(error instanceof Error ? error.message : '提交产品失败')
  } finally {
    submitLoading.value = false
  }
}

function handleFormClose() {
  activeEditSessionId += 1
  abortEditRequest()
  formRef.value?.clearValidate()
  clearFormRefreshState()
  formDirtySinceOpen = false
  applyFormDataWithoutDirty()
  editingProductId.value = null
}

function handleSizeChange(size: number) {
  setPageSize(size)
  clearSelection()
  void syncListRouteQuery()
}

function handlePageChange(page: number) {
  setPageNum(page)
  clearSelection()
  void syncListRouteQuery()
}

watch(
  () => [route.query.productName, route.query.nodeType, route.query.status, route.query.pageNum, route.query.pageSize],
  () => {
    applyRouteQueryToFilters()
    clearSelection()
    void loadProductPage()
  }
)

watch(detailVisible, (visible) => {
  if (visible) {
    return
  }
  latestDetailRequestId += 1
  abortDetailRequest()
  detailLoading.value = false
  detailRefreshing.value = false
  detailErrorMessage.value = ''
  // detailRefreshErrorMessage 已移除
  detailData.value = null
})

watch(
  formData,
  () => {
    if (!formVisible.value || !editingProductId.value || suppressFormDirtyTracking) {
      return
    }
    formDirtySinceOpen = true
  },
  { deep: true, flush: 'sync' }
)

onBeforeUnmount(() => {
  abortListRequest()
  abortListPrefetchRequest()
  abortDetailRequest()
  abortEditRequest()
})

onMounted(async () => {
  hydrateProductDetailCache()
  hydrateProductPageCache()
  applyRouteQueryToFilters()
  await loadProductPage()
})
</script>

<style scoped>
.product-asset-view {
  gap: 16px;
}

:deep(.product-detail-drawer .el-drawer__header) {
  padding: 24px 28px 16px;
  border-bottom: 1px solid var(--panel-border);
}

:deep(.product-detail-drawer .el-drawer__body) {
  padding: 20px 28px 24px;
}

:deep(.product-detail-drawer .detail-drawer__heading h2) {
  margin-top: 0.24rem;
  font-size: clamp(1.5rem, 2vw, 1.75rem);
  letter-spacing: -0.015em;
  font-weight: 600;
  color: var(--text-heading);
}

:deep(.product-detail-drawer .detail-drawer__subtitle) {
  margin-top: 0.4rem;
  max-width: 38rem;
  font-size: 13px;
  line-height: 1.55;
  color: var(--text-caption);
}

.product-detail-layout {
  display: grid;
  gap: 18px;
}

.product-detail-inline-state {
  display: flex;
  align-items: center;
  min-height: 2.6rem;
  padding: 0.8rem 1rem;
  border: 1px solid var(--brand);
  border-radius: 6px;
  background: color-mix(in srgb, var(--brand) 4%, white);
  color: var(--brand);
  font-size: 13px;
  line-height: 1.55;
}

.product-detail-inline-state--error {
  border-color: var(--danger);
  color: var(--danger);
  background: color-mix(in srgb, var(--danger) 4%, white);
}

.product-form-inline-state {
  display: flex;
  align-items: center;
  min-height: 2.6rem;
  padding: 0.8rem 1rem;
  border: 1px solid var(--brand);
  border-radius: 6px;
  background: color-mix(in srgb, var(--brand) 4%, white);
  color: var(--brand);
  font-size: 13px;
  line-height: 1.55;
}

.product-form-inline-state--warning {
  border-color: #d48806;
  color: #d48806;
  background: color-mix(in srgb, #d48806 4%, white);
}

.product-form-inline-state--error {
  border-color: var(--danger);
  color: var(--danger);
  background: color-mix(in srgb, var(--danger) 4%, white);
}

.product-detail-zone {
  position: relative;
  overflow: hidden;
  padding: 1.2rem 1.3rem;
  border: 1px solid var(--panel-border);
  border-radius: 10px;
  background: #ffffff;
  box-shadow:
    0 3px 10px rgba(24, 45, 77, 0.04),
    0 1px 2px rgba(0, 0, 0, 0.04);
}

.product-detail-zone::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 2px;
  background: linear-gradient(
    90deg,
    color-mix(in srgb, var(--brand) 72%, white),
    color-mix(in srgb, var(--accent) 52%, white),
    color-mix(in srgb, var(--brand-bright) 54%, white)
  );
}

.product-detail-zone--overview {
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 8%, transparent), transparent 46%),
    linear-gradient(180deg, rgba(244, 248, 255, 0.97), rgba(255, 255, 255, 0.95));
}

.product-detail-zone--ledger {
  background:
    radial-gradient(circle at top left, color-mix(in srgb, var(--brand) 6%, transparent), transparent 48%),
    linear-gradient(180deg, rgba(250, 252, 255, 0.98), rgba(246, 249, 255, 0.94));
}

.product-detail-zone--governance {
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--accent) 8%, transparent), transparent 45%),
    linear-gradient(180deg, rgba(249, 252, 255, 0.97), rgba(246, 249, 255, 0.94));
}

.product-detail-zone--danger {
  border-color: color-mix(in srgb, var(--danger) 20%, var(--panel-border));
}

.product-detail-zone__header {
  display: grid;
  gap: 0.18rem;
  margin-bottom: 0.9rem;
}

.product-detail-zone__kicker {
  color: var(--text-heading);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.product-detail-zone__intro {
  margin: 0;
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.5;
}

.product-detail-overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.14fr) minmax(0, 0.86fr);
  gap: 14px;
  align-items: start;
}

.product-detail-overview-lead {
  display: grid;
  gap: 0.5rem;
  min-width: 0;
  padding: 1rem 1.1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  border-radius: 8px;
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 12%, transparent), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 249, 255, 0.95));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 8px 20px rgba(24, 55, 92, 0.05);
}

.product-detail-overview-lead--danger {
  border-color: color-mix(in srgb, var(--danger) 18%, transparent);
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--danger) 10%, transparent), transparent 46%),
    linear-gradient(180deg, rgba(255, 249, 249, 0.98), rgba(255, 243, 243, 0.95));
}

.product-detail-overview-lead__eyebrow {
  color: color-mix(in srgb, var(--brand) 64%, var(--text-caption-2));
  font-size: 11px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: 0.08em;
}

.product-detail-overview-lead__title {
  color: var(--text-heading);
  font-size: clamp(1.2rem, 2vw, 1.48rem);
  font-weight: 700;
  line-height: 1.34;
}

.product-detail-overview-lead__text {
  margin: 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.58;
}

.product-detail-overview-progress {
  display: grid;
  gap: 0.3rem;
}

.product-detail-overview-progress__track {
  position: relative;
  overflow: hidden;
  height: 0.42rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--brand) 12%, white);
}

.product-detail-overview-progress__fill {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--brand), color-mix(in srgb, var(--accent) 76%, var(--brand)));
}

.product-detail-overview-progress__caption {
  color: var(--text-caption-2);
  font-size: 11.5px;
  line-height: 1.48;
}

.product-detail-overview-lead__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.36rem 0.48rem;
}

.product-detail-overview-lead__meta span {
  display: inline-flex;
  align-items: center;
  min-height: 1.65rem;
  padding: 0.24rem 0.58rem;
  border-radius: var(--radius-pill);
  border: 1px solid color-mix(in srgb, var(--brand) 10%, transparent);
  background: rgba(255, 255, 255, 0.82);
  color: var(--text-caption);
  font-size: 11.5px;
  font-weight: 600;
  line-height: 1.4;
}

.product-detail-overview-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.product-detail-overview-metric {
  display: grid;
  gap: 0.26rem;
  min-width: 0;
  padding: 0.9rem 1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.76);
}

.product-detail-overview-metric__label {
  color: var(--text-caption-2);
  font-size: 11px;
  font-weight: 600;
  line-height: 1.5;
  text-transform: uppercase;
}

.product-detail-overview-metric__value {
  color: var(--text-heading);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.4;
  word-break: break-word;
}

.product-detail-overview-metric__hint {
  margin: 0;
  color: var(--text-caption);
  font-size: 11px;
  line-height: 1.5;
}

.product-detail-overview-metric__value-wrapper {
  display: flex;
  align-items: baseline;
  gap: 0.4rem;
}

.product-detail-overview-metric__trend {
  font-size: 0.85em;
  font-weight: 700;
  line-height: 1;
  padding: 0.1em 0.3em;
  border-radius: 3px;
}

.product-detail-overview-metric__trend--up {
  color: #52c41a;
  background: color-mix(in srgb, #52c41a 12%, transparent);
}

.product-detail-overview-metric__trend--down {
  color: #ff4d4f;
  background: color-mix(in srgb, #ff4d4f 12%, transparent);
}

.product-detail-overview-metric__trend--same {
  color: #d48806;
  background: color-mix(in srgb, #d48806 12%, transparent);
}

.product-detail-ledger-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.62fr) minmax(0, 1.38fr);
  gap: 14px;
  align-items: start;
}

.product-detail-ledger-card {
  display: grid;
  gap: 0.5rem;
  min-width: 0;
  padding: 1rem 1.1rem;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: #ffffff;
  box-shadow:
    0 2px 8px rgba(24, 45, 77, 0.04),
    0 1px 2px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.product-detail-ledger-card:hover {
  box-shadow:
    0 4px 12px rgba(24, 45, 77, 0.08),
    0 2px 4px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.product-detail-ledger-card--contract {
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 6%, transparent), transparent 44%),
    linear-gradient(180deg, rgba(249, 252, 255, 0.98), rgba(246, 250, 255, 0.93));
}

.product-detail-card-header {
  display: grid;
  gap: 0.2rem;
}

.product-detail-card-header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.product-detail-card-header p {
  margin: 0;
  color: var(--text-caption);
  font-size: 11px;
  line-height: 1.5;
}

.product-detail-contract-list {
  display: grid;
  gap: 10px;
}

.product-detail-contract-item {
  display: grid;
  gap: 0.24rem;
  min-width: 0;
  padding: 0.9rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.95));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.product-detail-contract-item__label {
  color: var(--text-caption-2);
  font-size: 11.5px;
  font-weight: 600;
  line-height: 1.4;
}

.product-detail-contract-item__value {
  color: var(--text-heading);
  font-size: 13.5px;
  font-weight: 700;
  line-height: 1.44;
  letter-spacing: -0.01em;
  word-break: break-word;
}

.product-detail-archive-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.product-detail-archive-item {
  display: grid;
  gap: 0.26rem;
  min-width: 0;
  padding: 0.9rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.93));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.product-detail-archive-item--full {
  grid-column: 1 / -1;
}

.product-detail-archive-item__label {
  color: var(--text-caption-2);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.product-detail-archive-item__value {
  color: var(--text-heading);
  font-size: 13.5px;
  font-weight: 700;
  line-height: 1.44;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-detail-archive-meta-row {
  display: flex;
  align-items: baseline;
  gap: 0.36rem;
  margin-bottom: 0.18rem;
  font-size: 11px;
  color: var(--text-caption-2);
  line-height: 1.4;
}

.product-detail-archive-meta-row .product-detail-archive-item__label {
  font-size: 11px;
  color: var(--text-caption-2);
  font-weight: 600;
}

.product-detail-archive-meta-separator {
  color: var(--brand);
  font-size: 0.9em;
  font-weight: 600;
}

.product-detail-archive-meta-value {
  display: flex;
  align-items: baseline;
  gap: 0.36rem;
  line-height: 1.4;
}

.product-detail-archive-meta-value .product-detail-archive-item__value {
  font-size: 13.5px;
}

.product-detail-archive-meta-value .product-detail-archive-meta-separator {
  color: var(--brand);
  font-size: 0.9em;
  font-weight: 600;
}

.product-detail-description-card {
  display: grid;
  gap: 0.26rem;
  margin-top: 0.3rem;
  padding: 1rem 1.1rem;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.93));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.product-detail-description-card__label {
  color: var(--text-caption-2);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.product-detail-description-card__value {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.54;
  word-break: break-word;
}

.product-detail-governance-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-items: start;
}

.product-detail-governance-card {
  display: grid;
  gap: 0.36rem;
  min-width: 0;
  padding: 0.95rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.93));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.product-detail-governance-card--lead {
  border-color: color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 9%, transparent), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 249, 255, 0.94));
}

.product-detail-governance-card--danger {
  border-color: color-mix(in srgb, var(--danger) 18%, transparent);
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--danger) 10%, transparent), transparent 46%),
    linear-gradient(180deg, rgba(255, 249, 249, 0.98), rgba(255, 243, 243, 0.95));
}

.product-detail-governance-card__label {
  color: color-mix(in srgb, var(--brand) 42%, var(--text-caption-2));
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.product-detail-governance-card__title {
  color: var(--text-heading);
  font-size: 1.28rem;
  font-weight: 700;
  line-height: 1.36;
}

.product-detail-governance-card__text {
  margin: 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.54;
}

.product-detail-governance-list {
  display: grid;
  gap: 0.36rem;
  margin: 0;
  padding-left: 1.1rem;
  color: var(--text-caption);
  font-size: 12.5px;
  line-height: 1.52;
}

.product-hero-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  width: 100%;
}

.product-hero-card__heading {
  min-width: 0;
}

.product-hero-card__title {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.08rem;
}

.product-hero-card__caption {
  margin: 0.35rem 0 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.6;
}

.product-workbench-card__filters {
  margin-bottom: 0.72rem;
}

.product-applied-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.55rem 0.75rem;
  margin-bottom: 0.72rem;
}

.product-applied-filters__label {
  color: var(--text-caption-2);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
}

.product-applied-filters__list {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 0.45rem;
  min-width: 0;
}

.product-applied-filters__tag {
  margin: 0;
}

.product-applied-filters__clear {
  margin-left: auto;
  padding-inline: 0.08rem;
}

/* 快速搜索标签 */
.product-quick-search-tag {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.product-quick-search-tag__chip {
  cursor: pointer;
}

/* ============================================
   卡片视图 - 精致现代风格
   ============================================ */
.product-card-view {
  display: flex;
  flex-direction: column;
  margin-bottom: 1rem;
}

/* 卡片网格布局 */
.product-card-view .product-mobile-list__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

/* 卡片容器 - 精致卡片设计 */
.product-card-view .product-mobile-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;
  border: 1px solid rgba(228, 235, 246, 0.65);
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #fafaff 100%);
  box-shadow:
    0 2px 8px rgba(24, 45, 77, 0.04),
    0 1px 3px rgba(24, 45, 77, 0.02);
  transition:
    box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1),
    transform 0.25s cubic-bezier(0.4, 0, 0.2, 1),
    border-color 0.2s ease;
  cursor: pointer;
}

.product-card-view .product-mobile-card:hover {
  box-shadow:
    0 8px 24px rgba(24, 45, 77, 0.08),
    0 4px 12px rgba(24, 45, 77, 0.04);
  transform: translateY(-2px);
  border-color: rgba(78, 89, 105, 0.15);
}

/* 卡片选中状态 */
.product-card-view .product-mobile-card.selected {
  border-color: var(--brand);
  background: linear-gradient(180deg, #f8fcff 0%, #f0f8ff 100%);
}

/* ============================================
   卡片头部 - 棋盘布局
   ============================================ */
.product-card-view .product-mobile-card__header {
  display: grid;
  grid-template-columns: auto 1fr auto;
  grid-template-rows: auto auto;
  gap: 8px 12px;
  margin-bottom: 12px;
}

/* 卡片复选框 */
.product-card-view .product-mobile-card__header .el-checkbox {
  grid-row: 1 / span 2;
  margin: 0;
}

/* 卡片标题区域 */
.product-card-view .product-mobile-card__heading {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  min-width: 0;
}

.product-card-view .product-mobile-card__title {
  color: #1a1d21;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: -0.02em;
}

.product-card-view .product-mobile-card__sub {
  overflow: hidden;
  color: #7d8692;
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 卡片状态标签 */
.product-card-view .product-mobile-card__status {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ============================================
   卡片视图 - 精致现代风格
   ============================================ */
.product-card-view {
  display: flex;
  flex-direction: column;
  margin-bottom: 1rem;
}

/* 卡片网格布局 */
.product-card-view .product-mobile-list__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

/* 卡片容器 - 精致卡片设计 */
.product-card-view .product-mobile-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;
  border: 1px solid rgba(228, 235, 246, 0.65);
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #fafaff 100%);
  box-shadow:
    0 2px 8px rgba(24, 45, 77, 0.04),
    0 1px 3px rgba(24, 45, 77, 0.02);
  transition:
    box-shadow 0.3s cubic-bezier(0.4, 0, 0.2, 1),
    transform 0.3s cubic-bezier(0.4, 0, 0.2, 1),
    border-color 0.2s ease,
    box-shadow 0.3s ease;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

/* 卡片悬停效果 */
.product-card-view .product-mobile-card:hover {
  box-shadow:
    0 12px 32px rgba(24, 45, 77, 0.08),
    0 6px 16px rgba(24, 45, 77, 0.04);
  transform: translateY(-4px);
  border-color: rgba(78, 89, 105, 0.15);
}

/* 卡片选中状态 */
.product-card-view .product-mobile-card.selected {
  border-color: var(--brand);
  background: linear-gradient(180deg, #f8fcff 0%, #f0f8ff 100%);
}

/* 卡片选中伪元素装饰 */
.product-card-view .product-mobile-card.selected::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  padding: 1px;
  background: linear-gradient(135deg, var(--brand), var(--brand-bright));
  -webkit-mask: 
    linear-gradient(#fff 0 0) content-box, 
    linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}

/* ============================================
   卡片头部 - 棋盘布局
   ============================================ */
.product-card-view .product-mobile-card__header {
  display: grid;
  grid-template-columns: auto 1fr auto;
  grid-template-rows: auto auto;
  gap: 8px 12px;
  margin-bottom: 12px;
}

/* 卡片复选框 */
.product-card-view .product-mobile-card__header .el-checkbox {
  grid-row: 1 / span 2;
  margin: 0;
}

/* 卡片标题区域 */
.product-card-view .product-mobile-card__heading {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  min-width: 0;
}

.product-card-view .product-mobile-card__title {
  color: #1a1d21;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: -0.02em;
}

.product-card-view .product-mobile-card__sub {
  overflow: hidden;
  color: #7d8692;
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 卡片状态标签 */
.product-card-view .product-mobile-card__status {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ============================================
   卡片元数据标签组
   ============================================ */
.product-card-view .product-mobile-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.product-card-view .product-mobile-card__meta-item {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border: 1px solid rgba(228, 235, 246, 0.7);
  border-radius: 12px;
  background: #ffffff;
  color: #525a66;
  font-size: 11px;
  font-weight: 500;
  line-height: 1.4;
}

.product-card-view .product-mobile-card__meta-item:first-child {
  background: rgba(78, 89, 105, 0.04);
  border-color: rgba(78, 89, 105, 0.1);
  color: #3e4651;
}

/* ============================================
   卡片信息网格
   ============================================ */
.product-card-view .product-mobile-card__info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 14px;
  flex-grow: 1;
}

.product-card-view .product-mobile-card__field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid rgba(228, 235, 246, 0.6);
  border-radius: 8px;
  background: #fcfdfd;
}

.product-card-view .product-mobile-card__field span {
  color: #95a0ae;
  font-size: 10.5px;
  font-weight: 500;
  line-height: 1.4;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.product-card-view .product-mobile-card__field strong {
  overflow: hidden;
  color: #1a1d21;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ============================================
   卡片操作区域
   ============================================ */
.product-card-view .product-mobile-card__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid rgba(228, 235, 246, 0.5);
}

/* 操作按钮样式 - 使用系统品牌色 */
.product-card-view .product-mobile-card__actions :deep(.el-button) {
  flex: 1;
  height: 32px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

/* 主按钮 - 使用系统品牌色 */
.product-card-view .product-mobile-card__actions :deep(.el-button.el-button--primary) {
  background: linear-gradient(135deg, var(--brand) 0%, var(--brand-bright) 100%);
  border: none;
  box-shadow: 0 2px 8px color-mix(in srgb, var(--brand) 25%, transparent);
  color: #ffffff;
}

.product-card-view .product-mobile-card__actions :deep(.el-button.el-button--primary:hover) {
  background: linear-gradient(135deg, var(--brand-bright) 0%, var(--brand) 100%);
  box-shadow: 0 4px 12px color-mix(in srgb, var(--brand) 35%, transparent);
}

.product-card-view .product-mobile-card__actions :deep(.el-button.el-button--primary:active) {
  transform: translateY(1px);
  box-shadow: 0 2px 4px color-mix(in srgb, var(--brand) 20%, transparent);
}

/* 次要按钮 - 使用系统边框色 */
.product-card-view .product-mobile-card__actions :deep(.el-button.el-button--ghost) {
  color: var(--text-caption);
  border: 1px solid var(--panel-border);
  background: transparent;
}

.product-card-view .product-mobile-card__actions :deep(.el-button.el-button--ghost:hover) {
  background: color-mix(in srgb, var(--brand) 8%, transparent);
  border-color: var(--brand);
  color: var(--brand);
}

.product-card-view .product-mobile-card__actions :deep(.el-button .el-icon) {
  font-size: 14px;
}

/* 下拉菜单按钮样式 */
.product-card-view .product-mobile-card__actions :deep(.el-button-group) {
  display: flex;
}

.product-card-view .product-mobile-card__actions :deep(.el-dropdown) {
  flex: 1;
}

/* 响应式卡片视图 */
.product-mobile-list {
  display: none;
  margin-bottom: 0.72rem;
}

/* 卡片视图响应式 - 桌面端显示 */
@media (min-width: 721px) {
  .product-card-view {
    display: flex;
  }
  
  .product-mobile-list {
    display: none;
  }
}

/* 卡片视图响应式 - 移动端显示 */
@media (max-width: 720px) {
  .product-card-view {
    display: none;
  }
  
  .product-mobile-list {
    display: block;
  }
}

/* 下拉菜单按钮样式 */
.product-card-view .product-mobile-card__actions :deep(.el-button-group) {
  display: flex;
}

.product-card-view .product-mobile-card__actions :deep(.el-dropdown) {
  flex: 1;
}

/* 响应式卡片视图 */
.product-mobile-list {
  display: none;
  margin-bottom: 0.72rem;
}

.product-result-panel {
  position: relative;
  isolation: isolate;
  min-height: 14rem;
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(247, 250, 255, 0.76));
}

.product-result-panel :deep(.el-loading-mask) {
  border-radius: inherit;
  background: rgba(248, 250, 255, 0.78) !important;
  backdrop-filter: blur(5px);
}

.product-result-panel :deep(.el-loading-spinner .el-loading-text) {
  margin-top: 0.72rem;
  color: color-mix(in srgb, var(--brand) 62%, var(--text-caption));
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.product-result-panel :deep(.el-loading-spinner .path) {
  stroke: var(--brand);
}

.product-empty-state {
  display: grid;
  justify-items: center;
  padding: 0.4rem 0 0.2rem;
}

.product-empty-state :deep(.empty-state) {
  padding-block: 3.25rem 2rem;
}

.product-empty-state__actions {
  display: flex;
  justify-content: center;
}

.product-loading-state {
  display: grid;
  gap: 14px;
  min-height: 14rem;
  padding: 0.72rem 0.1rem 0.2rem;
}

.product-loading-state__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.product-loading-state__desktop {
  display: grid;
  gap: 10px;
}

.product-loading-state__mobile {
  display: none;
  gap: 12px;
}

.product-loading-table {
  display: grid;
  grid-template-columns: 0.38fr 1.35fr 1.55fr 0.96fr 0.9fr 1.1fr 0.78fr 1.18fr;
  gap: 12px;
  align-items: center;
}

.product-loading-table--header {
  padding: 0 0.82rem;
}

.product-loading-table--row {
  padding: 0.92rem 0.82rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.78);
}

.product-loading-mobile-card {
  display: grid;
  gap: 0.8rem;
  padding: 0.92rem 0.96rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.76);
}

.product-loading-mobile-card__header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 0.65rem;
  align-items: start;
}

.product-loading-mobile-card__heading {
  display: grid;
  gap: 0.3rem;
}

.product-loading-mobile-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.product-loading-mobile-card__info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.7rem 0.9rem;
}

.product-loading-mobile-card__field {
  display: grid;
  gap: 0.28rem;
}

.product-loading-pulse {
  position: relative;
  overflow: hidden;
  background: linear-gradient(90deg, rgba(228, 235, 246, 0.8), rgba(244, 248, 255, 0.98), rgba(228, 235, 246, 0.8));
  background-size: 220% 100%;
  animation: product-loading-shimmer 1.35s ease-in-out infinite;
}

.product-loading-pulse::after {
  content: '';
  position: absolute;
  inset: 0;
  border: 1px solid rgba(255, 255, 255, 0.46);
  border-radius: inherit;
}

.product-loading-line {
  display: block;
  height: 0.82rem;
  border-radius: var(--radius-pill);
}

.product-loading-line--header {
  height: 0.72rem;
}

.product-loading-line--key {
  width: 88%;
}

.product-loading-line--title {
  width: 92%;
}

.product-loading-line--short {
  width: 72%;
}

.product-loading-line--meta {
  width: 78%;
}

.product-loading-line--time {
  width: 100%;
}

.product-loading-line--label {
  width: 52%;
  height: 0.68rem;
}

.product-loading-line--value {
  width: 82%;
  height: 0.84rem;
}

.product-loading-pill {
  display: inline-flex;
  width: 6rem;
  height: 1.42rem;
  border-radius: var(--radius-pill);
}

.product-loading-pill--status {
  width: 4.6rem;
}

.product-loading-square {
  display: block;
  width: 1rem;
  height: 1rem;
  border-radius: 0.3rem;
}

.product-mobile-list__grid {
  display: grid;
  gap: 12px;
}

.product-mobile-card {
  display: grid;
  gap: 0.8rem;
  padding: 0.92rem 0.96rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.76);
}

.product-mobile-card__header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 0.65rem;
  align-items: start;
}

.product-mobile-card__heading {
  display: grid;
  gap: 0.2rem;
  min-width: 0;
}

.product-mobile-card__title {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.product-mobile-card__sub {
  overflow: hidden;
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-mobile-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.product-mobile-card__meta-item {
  display: inline-flex;
  align-items: center;
  min-height: 1.6rem;
  padding: 0.2rem 0.58rem;
  border-radius: var(--radius-pill);
  background: rgba(78, 89, 105, 0.08);
  color: var(--text-caption);
  font-size: 11.5px;
  line-height: 1.4;
}

.product-mobile-card__info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.7rem 0.9rem;
}

.product-mobile-card__field {
  display: grid;
  gap: 0.18rem;
  min-width: 0;
}

.product-mobile-card__field span {
  color: var(--text-caption-2);
  font-size: 11.5px;
  line-height: 1.4;
}

.product-mobile-card__field strong {
  overflow: hidden;
  color: var(--text-heading);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.52;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-mobile-card__actions {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  justify-content: flex-start;
}

.product-mobile-card__actions :deep(.el-button) {
  margin-left: 0;
  padding-inline: 0.1rem;
}

.product-desktop-table {
  display: block;
}

.product-table-actions {
  display: inline-flex;
  align-items: center;
  gap: 0.18rem;
  white-space: nowrap;
}

.product-table-actions :deep(.el-button) {
  margin-left: 0;
  padding-inline: 0.08rem;
}

@media (max-width: 1080px) {
  .product-detail-overview-grid,
  .product-detail-ledger-grid {
    grid-template-columns: 1fr;
  }

  .product-detail-overview-metrics,
  .product-detail-governance-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .product-hero-card__header {
    flex-direction: column;
    align-items: stretch;
  }

  .product-detail-archive-grid,
  .product-detail-overview-metrics,
  .product-detail-governance-grid {
    grid-template-columns: 1fr;
  }

  .product-detail-zone {
    padding: 0.82rem 0.84rem;
  }

  .product-detail-zone__kicker,
  .product-detail-card-header h3 {
    font-size: 1.2rem;
  }

  .product-detail-contract-item__value {
    font-size: 1.52rem;
  }

  .product-applied-filters {
    align-items: flex-start;
  }

  .product-applied-filters__clear {
    margin-left: 0;
  }

  .product-mobile-list {
    display: block;
  }

  .product-desktop-table {
    display: none;
  }

  .product-mobile-card__info {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .product-detail-layout {
    gap: 14px;
  }
}

@keyframes product-loading-shimmer {
  0% {
    background-position: 100% 50%;
  }

  100% {
    background-position: -100% 50%;
  }
}
</style>
