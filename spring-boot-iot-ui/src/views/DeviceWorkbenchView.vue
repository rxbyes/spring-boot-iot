<template>
  <div class="device-asset-view ops-workbench standard-list-view">
    <PanelCard
      eyebrow="设备资产台账"
      title="设备资产中心"
      description="聚焦设备台账维护，支持筛选、查看、编辑、更换、导入导出和设备洞察跳转。"
      class="ops-hero-card"
    >
      <template #actions>
        <StandardActionGroup gap="sm" class="ops-hero-actions">
          <el-button v-permission="'iot:devices:add'" type="primary" @click="handleAdd">新增设备</el-button>
          <el-button v-permission="'iot:devices:import'" plain @click="handleOpenBatchImport">批量导入</el-button>
        </StandardActionGroup>
      </template>
      <div class="ops-kpi-grid">
        <MetricCard label="设备总数" :value="String(pagination.total)" :badge="{ label: '台账', tone: 'brand' }" />
        <MetricCard label="当前页在线" :value="String(onlineCount)" :badge="{ label: '在线', tone: 'success' }" />
        <MetricCard label="当前页已激活" :value="String(activatedCount)" :badge="{ label: '可用', tone: 'brand' }" />
        <MetricCard label="当前页停用" :value="String(disabledCount)" :badge="{ label: '停用', tone: 'warning' }" />
      </div>
      <div class="ops-inline-note">
        当前交付已覆盖设备台账、详情查看、增改删、批量导入、设备更换和导出闭环；本轮优先继续优化列表恢复速度与查询操作体验。
      </div>
    </PanelCard>

    <PanelCard
      eyebrow="设备筛选"
      title="筛选条件"
      description="默认先展示高频条件，更多条件可展开补充精确筛选。"
      class="ops-filter-card"
    >
      <el-form :model="searchForm" label-position="top" class="ops-filter-form" @submit.prevent>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="设备编码">
              <el-input id="query-device-code" v-model="searchForm.deviceCode" placeholder="请输入设备编码" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="设备名称">
              <el-input id="filter-device-name" v-model="searchForm.deviceName" placeholder="请输入设备名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="产品 Key">
              <el-input id="device-product-key" v-model="searchForm.productKey" placeholder="请输入产品 Key" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="在线状态">
              <el-select v-model="searchForm.onlineStatus" placeholder="请选择在线状态" clearable>
                <el-option label="在线" :value="1" />
                <el-option label="离线" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="device-filter-toggle-row">
          <div class="device-filter-toggle-row__left">
            <el-button link class="device-filter-toggle" @click="toggleAdvancedFilters">
              {{ showAdvancedFilters ? '收起更多条件' : '更多条件' }}
            </el-button>
            <span v-if="advancedFilterHint" class="device-filter-toggle-row__hint">{{ advancedFilterHint }}</span>
          </div>
          <div class="ops-filter-actions">
            <StandardActionGroup gap="sm">
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </StandardActionGroup>
          </div>
        </div>

        <el-collapse-transition>
          <div v-show="showAdvancedFilters" class="device-filter-advanced">
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12" :md="8" :lg="6">
                <el-form-item label="设备 ID">
                  <el-input id="query-device-id" v-model="searchForm.deviceId" placeholder="请输入设备 ID" clearable @keyup.enter="handleSearch" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8" :lg="6">
                <el-form-item label="激活状态">
                  <el-select v-model="searchForm.activateStatus" placeholder="请选择激活状态" clearable>
                    <el-option label="已激活" :value="1" />
                    <el-option label="未激活" :value="0" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8" :lg="6">
                <el-form-item label="设备状态">
                  <el-select v-model="searchForm.deviceStatus" placeholder="请选择设备状态" clearable>
                    <el-option label="启用" :value="1" />
                    <el-option label="禁用" :value="0" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </el-collapse-transition>
      </el-form>
    </PanelCard>

    <PanelCard
      eyebrow="设备列表"
      title="设备资产台账"
      description="支持详情、编辑、更换、删除、批量导入和导出。"
      class="ops-table-card"
    >
      <div v-if="hasAppliedFilters" class="device-applied-filters">
        <span class="device-applied-filters__label">已生效筛选</span>
        <div class="device-applied-filters__list">
          <el-tag
            v-for="tag in activeFilterTags"
            :key="tag.key"
            closable
            class="device-applied-filters__tag"
            @close="removeAppliedFilter(tag.key)"
          >
            {{ tag.label }}
          </el-tag>
        </div>
        <el-button link class="device-applied-filters__clear" @click="handleClearAppliedFilters">清空全部</el-button>
      </div>

      <StandardTableToolbar
        :meta-items="[
          `已选 ${selectedRows.length} 项`,
          `在线 ${onlineCount} 台`,
          `已激活 ${activatedCount} 台`,
          `停用 ${disabledCount} 台`
        ]"
      >
        <template #right>
          <el-button v-permission="'iot:devices:delete'" link :disabled="selectedRows.length === 0" @click="handleBatchDelete">批量删除</el-button>
          <el-button v-permission="'iot:devices:export'" link @click="openExportColumnSetting">导出列设置</el-button>
          <el-button v-permission="'iot:devices:export'" link :disabled="selectedRows.length === 0" @click="handleExportSelected">导出选中</el-button>
          <el-button v-permission="'iot:devices:export'" link :disabled="tableData.length === 0" @click="handleExportCurrent">导出当前结果</el-button>
          <el-button link :disabled="selectedRows.length === 0" @click="clearSelection">清空选中</el-button>
          <el-button link @click="handleRefresh">刷新列表</el-button>
        </template>
      </StandardTableToolbar>

      <div
        v-if="showListInlineState"
        :class="[
          'device-list-inline-state',
          { 'device-list-inline-state--error': listRefreshState === 'error' }
        ]"
      >
        {{ listRefreshMessage }}
      </div>

      <div
        v-loading="loading && hasRecords"
        class="device-result-panel"
        element-loading-text="正在刷新设备列表"
        element-loading-background="rgba(248, 250, 255, 0.78)"
      >
        <div v-if="showListSkeleton" class="device-loading-state" aria-live="polite" aria-busy="true">
          <div class="device-loading-state__summary">
            <span v-for="item in 4" :key="item" class="device-loading-pulse device-loading-pill" />
          </div>

          <div class="device-loading-state__desktop">
            <div class="device-loading-table device-loading-table--header">
              <span v-for="item in 10" :key="`head-${item}`" class="device-loading-pulse device-loading-line device-loading-line--header" />
            </div>
            <div v-for="row in 5" :key="`row-${row}`" class="device-loading-table device-loading-table--row">
              <span class="device-loading-pulse device-loading-square" />
              <span class="device-loading-pulse device-loading-line device-loading-line--key" />
              <span class="device-loading-pulse device-loading-line device-loading-line--title" />
              <span class="device-loading-pulse device-loading-line device-loading-line--meta" />
              <span class="device-loading-pulse device-loading-pill device-loading-pill--status" />
              <span class="device-loading-pulse device-loading-pill device-loading-pill--status" />
              <span class="device-loading-pulse device-loading-pill device-loading-pill--status" />
              <span class="device-loading-pulse device-loading-line device-loading-line--short" />
              <span class="device-loading-pulse device-loading-line device-loading-line--time" />
              <span class="device-loading-pulse device-loading-line device-loading-line--time" />
            </div>
          </div>

          <div class="device-loading-state__mobile">
            <article v-for="card in 3" :key="`card-${card}`" class="device-loading-mobile-card">
              <div class="device-loading-mobile-card__header">
                <span class="device-loading-pulse device-loading-square" />
                <div class="device-loading-mobile-card__heading">
                  <span class="device-loading-pulse device-loading-line device-loading-line--title" />
                  <span class="device-loading-pulse device-loading-line device-loading-line--meta" />
                </div>
                <span class="device-loading-pulse device-loading-pill device-loading-pill--status" />
              </div>
              <div class="device-loading-mobile-card__meta">
                <span v-for="item in 4" :key="`meta-${card}-${item}`" class="device-loading-pulse device-loading-pill" />
              </div>
              <div class="device-loading-mobile-card__info">
                <div v-for="item in 5" :key="`field-${card}-${item}`" class="device-loading-mobile-card__field">
                  <span class="device-loading-pulse device-loading-line device-loading-line--label" />
                  <span class="device-loading-pulse device-loading-line device-loading-line--value" />
                </div>
              </div>
            </article>
          </div>
        </div>

        <template v-else-if="hasRecords">
          <div class="device-mobile-list">
            <div class="device-mobile-list__grid">
              <article v-for="row in tableData" :key="getDeviceRowKey(row)" class="device-mobile-card">
                <div class="device-mobile-card__header">
                  <el-checkbox
                    :model-value="isRowSelected(row)"
                    @change="(checked) => handleMobileSelectionChange(row, Boolean(checked))"
                  />
                  <div class="device-mobile-card__heading">
                    <strong class="device-mobile-card__title">{{ row.deviceName || '--' }}</strong>
                    <span class="device-mobile-card__sub">{{ row.deviceCode || '--' }}</span>
                  </div>
                  <el-tag :type="row.onlineStatus === 1 ? 'success' : 'info'" round>{{ getOnlineStatusText(row.onlineStatus) }}</el-tag>
                </div>

                <div class="device-mobile-card__meta">
                  <span class="device-mobile-card__meta-item" :title="formatTextValue(row.productKey)">{{ formatTextValue(row.productKey) }}</span>
                  <span class="device-mobile-card__meta-item">{{ getNodeTypeText(row.nodeType) }}</span>
                  <span
                    :class="[
                      'device-mobile-card__meta-item',
                      row.activateStatus === 1
                        ? 'device-mobile-card__meta-item--success'
                        : 'device-mobile-card__meta-item--warning'
                    ]"
                  >
                    {{ getActivateStatusText(row.activateStatus) }}
                  </span>
                  <span
                    :class="[
                      'device-mobile-card__meta-item',
                      row.deviceStatus === 1
                        ? 'device-mobile-card__meta-item--success'
                        : 'device-mobile-card__meta-item--danger'
                    ]"
                  >
                    {{ getDeviceStatusText(row.deviceStatus) }}
                  </span>
                </div>

                <div class="device-mobile-card__info">
                  <div class="device-mobile-card__field">
                    <span>产品名称</span>
                    <strong>{{ formatTextValue(row.productName) }}</strong>
                  </div>
                  <div class="device-mobile-card__field">
                    <span>接入协议</span>
                    <strong>{{ formatTextValue(row.protocolCode) }}</strong>
                  </div>
                  <div class="device-mobile-card__field">
                    <span>固件版本</span>
                    <strong>{{ formatTextValue(row.firmwareVersion) }}</strong>
                  </div>
                  <div class="device-mobile-card__field">
                    <span>最近上报</span>
                    <strong>{{ formatDateTime(row.lastReportTime) }}</strong>
                  </div>
                  <div class="device-mobile-card__field device-mobile-card__field--full">
                    <span>部署位置</span>
                    <strong class="device-mobile-card__address">{{ formatTextValue(row.address) }}</strong>
                  </div>
                </div>

                <div class="device-mobile-card__actions">
                  <el-button type="primary" link @click="handleOpenDetail(row)">详情</el-button>
                  <el-button v-permission="'iot:devices:update'" type="primary" link @click="handleEdit(row)">编辑</el-button>
                  <el-dropdown trigger="click" @command="(command) => handleMobileRowAction(command, row)">
                    <el-button type="primary" link>
                      更多
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item v-permission="'iot:devices:replace'" command="replace">更换</el-dropdown-item>
                        <el-dropdown-item command="insight">洞察</el-dropdown-item>
                        <el-dropdown-item v-permission="'iot:devices:delete'" command="delete">删除</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </article>
            </div>
          </div>

          <el-table ref="tableRef" class="device-desktop-table" :data="tableData" border stripe @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="48" />
            <StandardTableTextColumn prop="deviceCode" label="设备编码" :min-width="170" />
            <StandardTableTextColumn prop="deviceName" label="设备名称" :min-width="160" />
            <StandardTableTextColumn prop="productKey" label="产品 Key" :min-width="160" />
            <StandardTableTextColumn prop="productName" label="产品名称" :min-width="160" />
            <StandardTableTextColumn prop="protocolCode" label="协议" :width="120" />
            <el-table-column prop="onlineStatus" label="在线状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.onlineStatus === 1 ? 'success' : 'info'" round>{{ getOnlineStatusText(row.onlineStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="activateStatus" label="激活状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.activateStatus === 1 ? 'success' : 'warning'" round>{{ getActivateStatusText(row.activateStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deviceStatus" label="设备状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.deviceStatus === 1 ? 'success' : 'danger'" round>{{ getDeviceStatusText(row.deviceStatus) }}</el-tag>
              </template>
            </el-table-column>
            <StandardTableTextColumn prop="firmwareVersion" label="固件版本" :width="130" />
            <StandardTableTextColumn prop="lastReportTime" label="最近上报" :width="180">
              <template #default="{ row }">{{ formatDateTime(row.lastReportTime) }}</template>
            </StandardTableTextColumn>
            <StandardTableTextColumn prop="address" label="部署位置" :min-width="180" />
            <StandardTableTextColumn prop="createTime" label="创建时间" :width="180">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </StandardTableTextColumn>
            <el-table-column label="操作" width="320" fixed="right" :show-overflow-tooltip="false">
              <template #default="{ row }">
                <el-button type="primary" link @click="handleOpenDetail(row)">详情</el-button>
                <el-button v-permission="'iot:devices:update'" type="primary" link @click="handleEdit(row)">编辑</el-button>
                <el-button v-permission="'iot:devices:replace'" type="primary" link @click="handleOpenReplace(row)">更换</el-button>
                <el-button type="primary" link @click="handleJumpToInsight(row)">洞察</el-button>
                <el-button v-permission="'iot:devices:delete'" type="danger" link @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <div v-else-if="!loading" class="device-empty-state">
          <EmptyState :title="emptyStateTitle" :description="emptyStateDescription" />
          <div class="device-empty-state__actions">
            <el-button v-if="hasAppliedFilters" @click="handleClearAppliedFilters">清空筛选条件</el-button>
            <el-button v-else v-permission="'iot:devices:add'" type="primary" @click="handleAdd">新增设备</el-button>
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
      eyebrow="设备资产详情"
      :title="detailTitle"
      subtitle="统一查看设备资产主档、维护状态、认证字段与扩展元数据。"
      :tags="detailTags"
      :loading="detailLoading"
      :error-message="detailErrorMessage"
      :empty="!detailData"
    >
      <div v-if="detailData" class="device-detail-stack">
        <section class="detail-panel detail-panel--hero">
          <div class="detail-section-header">
            <div>
              <h3>资产概览</h3>
              <p>设备台账先回答“是否已入库、当前是否在线、是否已激活、是否可继续运维”。</p>
            </div>
          </div>
          <div class="detail-summary-grid">
            <div class="detail-summary-card">
              <span class="detail-summary-card__label">产品归属</span>
              <strong class="detail-summary-card__value">{{ detailData.productName || '--' }}</strong>
              <p class="detail-summary-card__hint">{{ detailData.productKey || '--' }}</p>
            </div>
            <div class="detail-summary-card">
              <span class="detail-summary-card__label">在线状态</span>
              <strong class="detail-summary-card__value">{{ getOnlineStatusText(detailData.onlineStatus) }}</strong>
              <p class="detail-summary-card__hint">{{ formatDateTime(detailData.lastReportTime) }}</p>
            </div>
            <div class="detail-summary-card">
              <span class="detail-summary-card__label">激活状态</span>
              <strong class="detail-summary-card__value">{{ getActivateStatusText(detailData.activateStatus) }}</strong>
              <p class="detail-summary-card__hint">设备可用性基线</p>
            </div>
            <div class="detail-summary-card">
              <span class="detail-summary-card__label">设备状态</span>
              <strong class="detail-summary-card__value">{{ getDeviceStatusText(detailData.deviceStatus) }}</strong>
              <p class="detail-summary-card__hint">是否允许继续使用</p>
            </div>
          </div>
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>资产档案</h3>
              <p>展示设备基础标识、协议、节点类型和部署位置，方便业务与现场核实库存。</p>
            </div>
          </div>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="detail-field__label">设备 ID</span>
              <strong class="detail-field__value">{{ detailData.id }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">设备编码</span>
              <strong class="detail-field__value">{{ detailData.deviceCode || '--' }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">设备名称</span>
              <strong class="detail-field__value">{{ detailData.deviceName || '--' }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">节点类型</span>
              <strong class="detail-field__value">{{ getNodeTypeText(detailData.nodeType) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">接入协议</span>
              <strong class="detail-field__value">{{ detailData.protocolCode || '--' }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">固件版本</span>
              <strong class="detail-field__value">{{ detailData.firmwareVersion || '--' }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">IP 地址</span>
              <strong class="detail-field__value">{{ detailData.ipAddress || '--' }}</strong>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">部署位置</span>
              <strong class="detail-field__value detail-field__value--plain">{{ detailData.address || '--' }}</strong>
            </div>
          </div>
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>运维信息</h3>
              <p>帮助运维与实施快速判断最近在线、离线和上报情况，确认现场是否需要介入。</p>
            </div>
          </div>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="detail-field__label">最近在线时间</span>
              <strong class="detail-field__value">{{ formatDateTime(detailData.lastOnlineTime) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">最近离线时间</span>
              <strong class="detail-field__value">{{ formatDateTime(detailData.lastOfflineTime) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">最近上报时间</span>
              <strong class="detail-field__value">{{ formatDateTime(detailData.lastReportTime) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">更新时间</span>
              <strong class="detail-field__value">{{ formatDateTime(detailData.updateTime) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">创建时间</span>
              <strong class="detail-field__value">{{ formatDateTime(detailData.createTime) }}</strong>
            </div>
          </div>
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>认证信息</h3>
              <p>面向接入与排障场景展示 MQTT 基础认证字段，敏感值按掩码形式展示。</p>
            </div>
          </div>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="detail-field__label">Client ID</span>
              <strong class="detail-field__value">{{ detailData.clientId || '--' }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">用户名</span>
              <strong class="detail-field__value">{{ detailData.username || '--' }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">密码</span>
              <strong class="detail-field__value">{{ maskSecret(detailData.password) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">设备密钥</span>
              <strong class="detail-field__value">{{ maskSecret(detailData.deviceSecret) }}</strong>
            </div>
          </div>
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>扩展元数据</h3>
              <p>用于保存库存、站点、维护责任、批次等可扩展信息，后续批量导入和设备更换也可复用该结构。</p>
            </div>
          </div>
          <div class="detail-field detail-field--full">
            <span class="detail-field__label">metadataJson</span>
            <pre class="detail-field__value detail-field__value--pre">{{ metadataPreview }}</pre>
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
            :disabled="!detailData?.deviceCode"
            @click="handleJumpToInsight(detailData)"
          >
            进入对象洞察台
          </el-button>
        </StandardDrawerFooter>
      </template>
    </StandardDetailDrawer>

    <StandardFormDrawer
      v-model="formVisible"
      eyebrow="设备台账表单"
      :title="formTitle"
      subtitle="统一通过右侧抽屉维护设备主数据、状态、认证字段和部署信息。"
      size="44rem"
      @close="handleFormClose"
    >
      <div class="ops-drawer-stack">
        <div class="ops-drawer-note">
          <strong>维护提示</strong>
          <span>设备列表先服务“库存可见、责任清晰、操作可追踪”。建议至少补齐产品归属、设备编码、激活状态、设备状态和部署位置。</span>
        </div>

        <el-form ref="formRef" :model="formData" :rules="formRules" label-position="top" class="ops-drawer-form">
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>基础档案</h3>
                <p>维护产品归属、设备编码与命名，确保库存、风险绑定与链路追踪都能找到同一台设备。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="产品" prop="productKey">
                <el-select id="device-form-product-key" v-model="formData.productKey" filterable placeholder="请选择产品" :loading="productLoading">
                  <el-option
                    v-for="product in productOptions"
                    :key="String(product.id)"
                    :label="`${product.productKey} - ${product.productName}`"
                    :value="product.productKey"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="设备名称" prop="deviceName">
                <el-input id="device-name" v-model="formData.deviceName" placeholder="请输入设备名称" />
              </el-form-item>
              <el-form-item label="设备编码" prop="deviceCode">
                <el-input id="device-code" v-model="formData.deviceCode" placeholder="请输入设备编码" />
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>状态与维护属性</h3>
                <p>统一维护激活状态、设备状态、固件版本和部署信息，为日常维护和资产核对提供基线。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="激活状态" prop="activateStatus">
                <el-radio-group v-model="formData.activateStatus">
                  <el-radio :value="1">已激活</el-radio>
                  <el-radio :value="0">未激活</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="设备状态" prop="deviceStatus">
                <el-radio-group v-model="formData.deviceStatus">
                  <el-radio :value="1">启用</el-radio>
                  <el-radio :value="0">禁用</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="固件版本" prop="firmwareVersion">
                <el-input id="firmware" v-model="formData.firmwareVersion" placeholder="请输入固件版本" />
              </el-form-item>
              <el-form-item label="IP 地址" prop="ipAddress">
                <el-input id="ip-address" v-model="formData.ipAddress" placeholder="请输入 IP 地址" />
              </el-form-item>
              <el-form-item label="部署位置" prop="address" class="ops-drawer-grid__full">
                <el-input id="address" v-model="formData.address" placeholder="请输入部署位置" />
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>认证字段</h3>
                <p>当前先维护最常用的 MQTT 认证字段，后续远程控制、升级与网关拓扑也可复用这些主数据。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="设备密钥" prop="deviceSecret">
                <el-input id="device-secret" v-model="formData.deviceSecret" placeholder="请输入设备密钥" />
              </el-form-item>
              <el-form-item label="Client ID" prop="clientId">
                <el-input id="client-id" v-model="formData.clientId" placeholder="请输入 Client ID" />
              </el-form-item>
              <el-form-item label="用户名" prop="username">
                <el-input id="username" v-model="formData.username" placeholder="请输入设备用户名" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input id="password" v-model="formData.password" type="password" show-password placeholder="请输入设备密码" />
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>扩展信息</h3>
                <p>建议使用 JSON 保存设备批次、站点、责任人、资产标签等额外字段，便于后续批量维护。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="metadataJson" prop="metadataJson" class="ops-drawer-grid__full">
                <el-input
                  id="metadata"
                  v-model="formData.metadataJson"
                  type="textarea"
                  :rows="5"
                  placeholder="请输入合法 JSON，例如 {&quot;site&quot;:&quot;北坡监测点&quot;}"
                />
              </el-form-item>
            </div>
          </section>
        </el-form>
      </div>

      <template #footer>
        <StandardDrawerFooter
          :confirm-loading="submitLoading"
          :confirm-text="editingDeviceId ? '保存设备变更' : '提交设备建档'"
          @cancel="formVisible = false"
          @confirm="handleSubmit"
        >
          <el-button class="standard-drawer-footer__button standard-drawer-footer__button--ghost" @click="formVisible = false">
            取消
          </el-button>
          <el-button
            v-permission="submitPermission"
            type="primary"
            class="standard-drawer-footer__button standard-drawer-footer__button--primary"
            :loading="submitLoading"
            @click="handleSubmit"
          >
            {{ editingDeviceId ? '保存设备变更' : '提交设备建档' }}
          </el-button>
        </StandardDrawerFooter>
      </template>
    </StandardFormDrawer>

    <DeviceBatchImportDrawer
      v-model="batchImportVisible"
      :submitting="batchImportSubmitting"
      :result="batchImportResult"
      @submit="handleBatchImportSubmit"
    />

    <DeviceReplaceDrawer
      v-model="replaceVisible"
      :device="replacingDevice"
      :product-options="productOptions"
      :product-loading="productLoading"
      :submitting="replaceSubmitting"
      @submit="handleReplaceSubmit"
    />

    <CsvColumnSettingDialog
      v-model="exportColumnDialogVisible"
      title="设备资产中心导出列设置"
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
import DeviceBatchImportDrawer from '@/components/DeviceBatchImportDrawer.vue'
import DeviceReplaceDrawer from '@/components/DeviceReplaceDrawer.vue'
import EmptyState from '@/components/EmptyState.vue'
import MetricCard from '@/components/MetricCard.vue'
import PanelCard from '@/components/PanelCard.vue'
import StandardActionGroup from '@/components/StandardActionGroup.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import { deviceApi } from '@/api/device'
import { productApi } from '@/api/product'
import { useServerPagination } from '@/composables/useServerPagination'
import type {
  Device,
  DeviceAddPayload,
  DeviceBatchAddPayload,
  DeviceBatchAddResult,
  DeviceReplacePayload,
  Product
} from '@/types/api'
import {
  buildDevicePageCacheKey,
  cloneDevicePageCacheEntry,
  createDevicePageCacheEntry,
  deserializeDevicePageCacheEntries,
  getDeviceRowKey,
  getNextDevicePageQuery,
  isDevicePageCacheFresh,
  type DevicePageCacheEntry,
  type DevicePageQuerySnapshot,
  resolveDevicePageLoadStrategy,
  serializeDevicePageCacheEntries
} from '@/views/deviceWorkbenchState'
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv'
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns'
import { confirmAction, confirmDelete, isConfirmCancelled } from '@/utils/confirm'
import { formatDateTime, prettyJson } from '@/utils/format'

interface DeviceSearchForm {
  deviceId: string
  productKey: string
  deviceCode: string
  deviceName: string
  onlineStatus: number | undefined
  activateStatus: number | undefined
  deviceStatus: number | undefined
}

type DeviceFilterKey = keyof DeviceSearchForm

interface DeviceFormState extends DeviceAddPayload {}

interface DevicePageLoadOptions {
  silent?: boolean
  force?: boolean
  silentMessage?: string
}

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const formRef = ref<FormInstance>()

const loading = ref(false)
const submitLoading = ref(false)
const productLoading = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const batchImportVisible = ref(false)
const batchImportSubmitting = ref(false)
const replaceVisible = ref(false)
const replaceSubmitting = ref(false)
const detailLoading = ref(false)
const detailErrorMessage = ref('')
const listRefreshMessage = ref('')
const listRefreshState = ref<'info' | 'error' | ''>('')
const editingDeviceId = ref<string | number | null>(null)

const tableData = ref<Device[]>([])
const selectedRows = ref<Device[]>([])
const productOptions = ref<Product[]>([])
const detailData = ref<Device | null>(null)
const batchImportResult = ref<DeviceBatchAddResult | null>(null)
const replacingDevice = ref<Device | null>(null)

const exportColumnDialogVisible = ref(false)
const exportColumnStorageKey = 'device-asset-view'
const defaultPageSize = 10
const advancedFilterKeys: readonly DeviceFilterKey[] = ['deviceId', 'activateStatus', 'deviceStatus']
let latestListRequestId = 0
let listAbortController: AbortController | null = null
let listPrefetchAbortController: AbortController | null = null
let detailAbortController: AbortController | null = null
let routeLoadOptions: DevicePageLoadOptions | null = null
const devicePageCache = new Map<string, DevicePageCacheEntry>()
const devicePageCacheTtlMs = 30_000
const devicePageCacheLimit = 8
const devicePageCacheSessionStorageKey = 'iot.devices.page-cache'

const searchForm = reactive<DeviceSearchForm>({
  deviceId: '',
  productKey: '',
  deviceCode: '',
  deviceName: '',
  onlineStatus: undefined,
  activateStatus: undefined,
  deviceStatus: undefined
})
const appliedFilters = reactive<DeviceSearchForm>({
  deviceId: '',
  productKey: '',
  deviceCode: '',
  deviceName: '',
  onlineStatus: undefined,
  activateStatus: undefined,
  deviceStatus: undefined
})
const showAdvancedFilters = ref(false)

const createDefaultFormData = (): DeviceFormState => ({
  productKey: '',
  deviceName: '',
  deviceCode: '',
  deviceSecret: '',
  clientId: '',
  username: '',
  password: '',
  activateStatus: 1,
  deviceStatus: 1,
  firmwareVersion: '',
  ipAddress: '',
  address: '',
  metadataJson: ''
})

const formData = reactive<DeviceFormState>(createDefaultFormData())

const { pagination, applyPageResult, resetPage, setPageNum, setPageSize } = useServerPagination(defaultPageSize)

const formTitle = computed(() => (editingDeviceId.value ? '编辑设备' : '新增设备'))
const submitPermission = computed(() => (editingDeviceId.value ? 'iot:devices:update' : 'iot:devices:add'))
const detailTitle = computed(() => detailData.value?.deviceName || detailData.value?.deviceCode || '设备详情')
const onlineCount = computed(() => tableData.value.filter((item) => item.onlineStatus === 1).length)
const activatedCount = computed(() => tableData.value.filter((item) => item.activateStatus === 1).length)
const disabledCount = computed(() => tableData.value.filter((item) => item.deviceStatus === 0).length)
const metadataPreview = computed(() => prettyJson(detailData.value?.metadataJson || '{}'))
const selectedRowKeySet = computed(() => new Set(selectedRows.value.map((item) => getDeviceRowKey(item)).filter(Boolean)))
const hasRecords = computed(() => tableData.value.length > 0)
const showListSkeleton = computed(() => loading.value && !hasRecords.value)
const showListInlineState = computed(() => Boolean(listRefreshMessage.value) && hasRecords.value)
const advancedFilterFilledCount = computed(() => countFilledFilters(searchForm, advancedFilterKeys))
const advancedFilterHint = computed(() => {
  if (showAdvancedFilters.value || advancedFilterFilledCount.value === 0) {
    return ''
  }
  return `更多条件中已填写 ${advancedFilterFilledCount.value} 项，查询时会一并生效。`
})
const activeFilterTags = computed(() => {
  const tags: Array<{ key: DeviceFilterKey; label: string }> = []
  const deviceId = appliedFilters.deviceId.trim()
  if (deviceId) {
    tags.push({ key: 'deviceId', label: `设备 ID：${deviceId}` })
  }
  const productKey = appliedFilters.productKey.trim()
  if (productKey) {
    tags.push({ key: 'productKey', label: `产品 Key：${productKey}` })
  }
  const deviceCode = appliedFilters.deviceCode.trim()
  if (deviceCode) {
    tags.push({ key: 'deviceCode', label: `设备编码：${deviceCode}` })
  }
  const deviceName = appliedFilters.deviceName.trim()
  if (deviceName) {
    tags.push({ key: 'deviceName', label: `设备名称：${deviceName}` })
  }
  if (appliedFilters.onlineStatus !== undefined) {
    tags.push({ key: 'onlineStatus', label: `在线状态：${getOnlineStatusText(appliedFilters.onlineStatus)}` })
  }
  if (appliedFilters.activateStatus !== undefined) {
    tags.push({ key: 'activateStatus', label: `激活状态：${getActivateStatusText(appliedFilters.activateStatus)}` })
  }
  if (appliedFilters.deviceStatus !== undefined) {
    tags.push({ key: 'deviceStatus', label: `设备状态：${getDeviceStatusText(appliedFilters.deviceStatus)}` })
  }
  return tags
})
const hasAppliedFilters = computed(() => activeFilterTags.value.length > 0)
const emptyStateTitle = computed(() => (hasAppliedFilters.value ? '没有符合条件的设备' : '还没有设备资产'))
const emptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? '已生效筛选暂时没有匹配结果，可以调整条件，或者直接清空当前筛选。'
    : '当前还没有设备资产，先新增设备或批量导入，再继续做台账维护和状态核查。'
)

const detailTags = computed(() => {
  if (!detailData.value) {
    return []
  }
  return [
    { label: getOnlineStatusText(detailData.value.onlineStatus), type: detailData.value.onlineStatus === 1 ? 'success' : 'info' as const },
    { label: getActivateStatusText(detailData.value.activateStatus), type: detailData.value.activateStatus === 1 ? 'success' : 'warning' as const },
    { label: getDeviceStatusText(detailData.value.deviceStatus), type: detailData.value.deviceStatus === 1 ? 'success' : 'danger' as const }
  ]
})

const formRules: FormRules<DeviceFormState> = {
  productKey: [{ required: true, message: '请选择产品', trigger: 'change' }],
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  deviceCode: [{ required: true, message: '请输入设备编码', trigger: 'blur' }],
  metadataJson: [
    {
      validator: (_rule, value: string, callback) => {
        if (!value) {
          callback()
          return
        }
        try {
          JSON.parse(value)
          callback()
        } catch {
          callback(new Error('metadataJson 必须是合法 JSON'))
        }
      },
      trigger: 'blur'
    }
  ]
}

const exportColumns: CsvColumn<Device>[] = [
  { key: 'id', label: '设备 ID' },
  { key: 'deviceCode', label: '设备编码' },
  { key: 'deviceName', label: '设备名称' },
  { key: 'productKey', label: '产品 Key' },
  { key: 'productName', label: '产品名称' },
  { key: 'protocolCode', label: '接入协议' },
  { key: 'nodeType', label: '节点类型', formatter: (value) => getNodeTypeText(Number(value)) },
  { key: 'onlineStatus', label: '在线状态', formatter: (value) => getOnlineStatusText(Number(value)) },
  { key: 'activateStatus', label: '激活状态', formatter: (value) => getActivateStatusText(Number(value)) },
  { key: 'deviceStatus', label: '设备状态', formatter: (value) => getDeviceStatusText(Number(value)) },
  { key: 'firmwareVersion', label: '固件版本' },
  { key: 'ipAddress', label: 'IP 地址' },
  { key: 'address', label: '部署位置' },
  { key: 'lastReportTime', label: '最近上报', formatter: (value) => formatDateTime(String(value || '')) },
  { key: 'createTime', label: '创建时间', formatter: (value) => formatDateTime(String(value || '')) }
]

const exportColumnOptions = toCsvColumnOptions(exportColumns)
const exportPresets = [
  { label: '默认模板', keys: exportColumns.map((column) => String(column.key)) },
  { label: '运维模板', keys: ['deviceCode', 'deviceName', 'productKey', 'onlineStatus', 'activateStatus', 'deviceStatus', 'lastReportTime', 'address'] },
  { label: '库存模板', keys: ['id', 'deviceCode', 'deviceName', 'productKey', 'productName', 'deviceStatus', 'createTime'] }
]
const selectedExportColumnKeys = ref<string[]>(
  loadCsvColumnSelection(
    exportColumnStorageKey,
    exportColumns.map((column) => String(column.key))
  )
)

function getOnlineStatusText(value?: number | null) {
  return value === 1 ? '在线' : '离线'
}

function getActivateStatusText(value?: number | null) {
  return value === 1 ? '已激活' : '未激活'
}

function getDeviceStatusText(value?: number | null) {
  return value === 1 ? '启用' : '禁用'
}

function getNodeTypeText(value?: number | null) {
  if (value === 1) {
    return '直连设备'
  }
  if (value === 2) {
    return '网关设备'
  }
  return '--'
}

function formatTextValue(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '--'
  }
  return String(value)
}

function hasFilledFilter(filters: DeviceSearchForm, key: DeviceFilterKey) {
  const value = filters[key]
  if (typeof value === 'string') {
    return value.trim() !== ''
  }
  return value !== undefined
}

function countFilledFilters(filters: DeviceSearchForm, keys: readonly DeviceFilterKey[]) {
  return keys.reduce((count, key) => count + (hasFilledFilter(filters, key) ? 1 : 0), 0)
}

function maskSecret(value?: string | null) {
  if (!value) {
    return '--'
  }
  if (value.length <= 4) {
    return '*'.repeat(value.length)
  }
  return `${value.slice(0, 2)}****${value.slice(-2)}`
}

function resetFormData(source?: Partial<Device>) {
  Object.assign(formData, createDefaultFormData(), {
    productKey: source?.productKey || '',
    deviceName: source?.deviceName || '',
    deviceCode: source?.deviceCode || '',
    deviceSecret: source?.deviceSecret || '',
    clientId: source?.clientId || '',
    username: source?.username || '',
    password: source?.password || '',
    activateStatus: source?.activateStatus ?? 1,
    deviceStatus: source?.deviceStatus ?? 1,
    firmwareVersion: source?.firmwareVersion || '',
    ipAddress: source?.ipAddress || '',
    address: source?.address || '',
    metadataJson: source?.metadataJson || ''
  })
}

function clearSelection() {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

function handleSelectionChange(rows: Device[]) {
  selectedRows.value = rows
}

function isRowSelected(row: Device) {
  return selectedRowKeySet.value.has(getDeviceRowKey(row))
}

function handleMobileSelectionChange(row: Device, checked: boolean) {
  const rowKey = getDeviceRowKey(row)
  const nextRows = checked
    ? [...selectedRows.value.filter((item) => getDeviceRowKey(item) !== rowKey), row]
    : selectedRows.value.filter((item) => getDeviceRowKey(item) !== rowKey)
  selectedRows.value = tableData.value.filter((item) => nextRows.some((selected) => getDeviceRowKey(selected) === getDeviceRowKey(item)))
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
  downloadRowsAsCsv('设备资产中心-选中项.csv', selectedRows.value, getResolvedExportColumns())
}

function handleExportCurrent() {
  downloadRowsAsCsv('设备资产中心-当前结果.csv', tableData.value, getResolvedExportColumns())
}

function syncAppliedFilters() {
  appliedFilters.deviceId = searchForm.deviceId.trim()
  appliedFilters.productKey = searchForm.productKey.trim()
  appliedFilters.deviceCode = searchForm.deviceCode.trim()
  appliedFilters.deviceName = searchForm.deviceName.trim()
  appliedFilters.onlineStatus = searchForm.onlineStatus
  appliedFilters.activateStatus = searchForm.activateStatus
  appliedFilters.deviceStatus = searchForm.deviceStatus
}

function clearSearchForm() {
  searchForm.deviceId = ''
  searchForm.productKey = ''
  searchForm.deviceCode = ''
  searchForm.deviceName = ''
  searchForm.onlineStatus = undefined
  searchForm.activateStatus = undefined
  searchForm.deviceStatus = undefined
}

function clearListRefreshState() {
  listRefreshMessage.value = ''
  listRefreshState.value = ''
}

function toggleAdvancedFilters() {
  showAdvancedFilters.value = !showAdvancedFilters.value
}

async function syncTableSelection() {
  await nextTick()
  if (!tableRef.value) {
    return
  }
  tableRef.value.clearSelection()
  const selectedKeys = selectedRowKeySet.value
  tableData.value.forEach((row) => {
    if (selectedKeys.has(getDeviceRowKey(row))) {
      tableRef.value?.toggleRowSelection(row, true)
    }
  })
}

function applyRouteQueryToFilters() {
  searchForm.deviceId = typeof route.query.deviceId === 'string' ? route.query.deviceId.trim() : ''
  searchForm.productKey = typeof route.query.productKey === 'string' ? route.query.productKey.trim() : ''
  searchForm.deviceCode = typeof route.query.deviceCode === 'string' ? route.query.deviceCode.trim() : ''
  searchForm.deviceName = typeof route.query.deviceName === 'string' ? route.query.deviceName.trim() : ''
  searchForm.onlineStatus = parseRouteNumberQuery(route.query.onlineStatus)
  searchForm.activateStatus = parseRouteNumberQuery(route.query.activateStatus)
  searchForm.deviceStatus = parseRouteNumberQuery(route.query.deviceStatus)
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
  key: 'deviceId' | 'productKey' | 'deviceCode' | 'deviceName' | 'onlineStatus' | 'activateStatus' | 'deviceStatus' | 'pageNum' | 'pageSize',
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
    normalizeQueryValue(route.query.deviceId) === normalizeQueryValue(nextQuery.deviceId) &&
    normalizeQueryValue(route.query.productKey) === normalizeQueryValue(nextQuery.productKey) &&
    normalizeQueryValue(route.query.deviceCode) === normalizeQueryValue(nextQuery.deviceCode) &&
    normalizeQueryValue(route.query.deviceName) === normalizeQueryValue(nextQuery.deviceName) &&
    normalizeQueryValue(route.query.onlineStatus) === normalizeQueryValue(nextQuery.onlineStatus) &&
    normalizeQueryValue(route.query.activateStatus) === normalizeQueryValue(nextQuery.activateStatus) &&
    normalizeQueryValue(route.query.deviceStatus) === normalizeQueryValue(nextQuery.deviceStatus) &&
    normalizeQueryValue(route.query.pageNum) === normalizeQueryValue(nextQuery.pageNum) &&
    normalizeQueryValue(route.query.pageSize) === normalizeQueryValue(nextQuery.pageSize)
  )
}

async function syncListRouteQuery(options: DevicePageLoadOptions = {}) {
  const nextQuery: Record<string, unknown> = { ...route.query }
  const trimmedDeviceId = searchForm.deviceId.trim()
  const trimmedProductKey = searchForm.productKey.trim()
  const trimmedDeviceCode = searchForm.deviceCode.trim()
  const trimmedDeviceName = searchForm.deviceName.trim()

  assignListQueryValue(nextQuery, 'deviceId', trimmedDeviceId || undefined)
  assignListQueryValue(nextQuery, 'productKey', trimmedProductKey || undefined)
  assignListQueryValue(nextQuery, 'deviceCode', trimmedDeviceCode || undefined)
  assignListQueryValue(nextQuery, 'deviceName', trimmedDeviceName || undefined)
  assignListQueryValue(nextQuery, 'onlineStatus', searchForm.onlineStatus)
  assignListQueryValue(nextQuery, 'activateStatus', searchForm.activateStatus)
  assignListQueryValue(nextQuery, 'deviceStatus', searchForm.deviceStatus)
  assignListQueryValue(nextQuery, 'pageNum', pagination.pageNum > 1 ? pagination.pageNum : undefined)
  assignListQueryValue(nextQuery, 'pageSize', pagination.pageSize !== defaultPageSize ? pagination.pageSize : undefined)

  if (hasSameListRouteQuery(nextQuery)) {
    await loadDevicePage(options)
    return
  }

  routeLoadOptions = options
  await router.replace({
    path: route.path,
    query: nextQuery
  })
}

async function loadProducts() {
  productLoading.value = true
  try {
    const res = await productApi.getAllProducts()
    if (res.code === 200) {
      productOptions.value = res.data || []
    }
  } catch (error) {
    console.error('加载产品列表失败', error)
    ElMessage.error('加载产品列表失败')
  } finally {
    productLoading.value = false
  }
}

function buildCurrentDevicePageQuery(): DevicePageQuerySnapshot {
  return {
    deviceId: searchForm.deviceId.trim(),
    productKey: searchForm.productKey.trim(),
    deviceCode: searchForm.deviceCode.trim(),
    deviceName: searchForm.deviceName.trim(),
    onlineStatus: searchForm.onlineStatus,
    activateStatus: searchForm.activateStatus,
    deviceStatus: searchForm.deviceStatus,
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  }
}

function getDevicePageSessionStorage() {
  if (typeof window === 'undefined') {
    return null
  }
  return window.sessionStorage
}

function hydrateDevicePageCache() {
  const storage = getDevicePageSessionStorage()
  if (!storage) {
    return
  }

  const entries = deserializeDevicePageCacheEntries(
    storage.getItem(devicePageCacheSessionStorageKey),
    devicePageCacheTtlMs,
    devicePageCacheLimit
  )

  devicePageCache.clear()
  entries.forEach((entry) => {
    devicePageCache.set(entry.key, entry)
  })

  if (entries.length === 0) {
    try {
      storage.removeItem(devicePageCacheSessionStorageKey)
    } catch {
      // 忽略浏览器存储异常，避免阻断页面加载
    }
  }
}

function persistDevicePageCache() {
  const storage = getDevicePageSessionStorage()
  if (!storage) {
    return
  }

  try {
    if (devicePageCache.size === 0) {
      storage.removeItem(devicePageCacheSessionStorageKey)
      return
    }

    storage.setItem(
      devicePageCacheSessionStorageKey,
      serializeDevicePageCacheEntries(devicePageCache.values(), devicePageCacheLimit)
    )
  } catch {
    // 忽略浏览器存储异常，避免阻断分页主流程
  }
}

function getCachedDevicePage(query: DevicePageQuerySnapshot) {
  const cacheKey = buildDevicePageCacheKey(query)
  return cloneDevicePageCacheEntry(devicePageCache.get(cacheKey))
}

function cacheDevicePage(query: DevicePageQuerySnapshot, pageResult: {
  total: number
  pageNum: number
  pageSize: number
  records: Device[]
}) {
  const entry = createDevicePageCacheEntry(query, pageResult)
  devicePageCache.delete(entry.key)
  devicePageCache.set(entry.key, entry)

  while (devicePageCache.size > devicePageCacheLimit) {
    const oldestKey = devicePageCache.keys().next().value
    if (!oldestKey) {
      break
    }
    devicePageCache.delete(oldestKey)
  }

  persistDevicePageCache()
}

function applyCachedDevicePage(entry: DevicePageCacheEntry) {
  tableData.value = applyPageResult({
    total: entry.total,
    pageNum: entry.pageNum,
    pageSize: entry.pageSize,
    records: entry.records
  })
  syncAppliedFilters()
  void syncTableSelection()
}

function clearDevicePageCache() {
  abortListPrefetchRequest()
  devicePageCache.clear()
  persistDevicePageCache()
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

function isAbortError(error: unknown) {
  return error instanceof Error && error.name === 'AbortError'
}

async function prefetchNextDevicePage(query: DevicePageQuerySnapshot, total: number) {
  const nextQuery = getNextDevicePageQuery(query, total)
  if (!nextQuery) {
    return
  }

  const cachedPage = getCachedDevicePage(nextQuery)
  if (isDevicePageCacheFresh(cachedPage, devicePageCacheTtlMs)) {
    return
  }

  abortListPrefetchRequest()
  const controller = new AbortController()
  listPrefetchAbortController = controller

  try {
    const res = await deviceApi.pageDevices(
      {
        deviceId: nextQuery.deviceId || undefined,
        productKey: nextQuery.productKey || undefined,
        deviceCode: nextQuery.deviceCode || undefined,
        deviceName: nextQuery.deviceName || undefined,
        onlineStatus: nextQuery.onlineStatus,
        activateStatus: nextQuery.activateStatus,
        deviceStatus: nextQuery.deviceStatus,
        pageNum: nextQuery.pageNum,
        pageSize: nextQuery.pageSize
      },
      {
        signal: controller.signal
      }
    )
    if (res.code === 200 && res.data) {
      cacheDevicePage(nextQuery, res.data)
    }
  } catch (error) {
    if (!isAbortError(error)) {
      console.warn('预取设备分页失败', error)
    }
  } finally {
    if (listPrefetchAbortController === controller) {
      listPrefetchAbortController = null
    }
  }
}

async function loadDevicePage(options: DevicePageLoadOptions = {}) {
  const requestId = ++latestListRequestId
  const query = buildCurrentDevicePageQuery()
  const cachedPage = getCachedDevicePage(query)
  const loadStrategy = resolveDevicePageLoadStrategy({
    hasCachedPage: Boolean(cachedPage),
    hasFreshCache: isDevicePageCacheFresh(cachedPage, devicePageCacheTtlMs),
    force: options.force === true,
    silent: options.silent === true
  })
  const hadVisibleResult = Boolean(cachedPage) || tableData.value.length > 0

  abortListPrefetchRequest()
  abortListRequest()
  if (cachedPage) {
    applyCachedDevicePage(cachedPage)
  }

  if (loadStrategy.useFreshCacheOnly) {
    clearListRefreshState()
    loading.value = false
    void prefetchNextDevicePage(query, cachedPage.total)
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
    const res = await deviceApi.pageDevices(
      {
        deviceId: query.deviceId || undefined,
        productKey: query.productKey || undefined,
        deviceCode: query.deviceCode || undefined,
        deviceName: query.deviceName || undefined,
        onlineStatus: query.onlineStatus,
        activateStatus: query.activateStatus,
        deviceStatus: query.deviceStatus,
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
      cacheDevicePage(query, res.data)
      void syncTableSelection()
      void prefetchNextDevicePage(query, res.data.total)
    }
  } catch (error) {
    if (requestId !== latestListRequestId || isAbortError(error)) {
      return
    }
    console.error('获取设备分页失败', error)
    if (preserveVisibleResult) {
      listRefreshState.value = 'error'
      listRefreshMessage.value = '最新数据校验失败，当前先展示已有结果。'
    } else {
      clearListRefreshState()
      ElMessage.error('获取设备分页失败')
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

async function openDetail(id: string | number) {
  abortDetailRequest()
  const controller = new AbortController()
  detailAbortController = controller
  detailVisible.value = true
  detailLoading.value = true
  detailErrorMessage.value = ''
  detailData.value = null
  try {
    const res = await deviceApi.getDeviceById(id, {
      signal: controller.signal
    })
    if (res.code === 200) {
      detailData.value = res.data
    }
  } catch (error) {
    if (isAbortError(error)) {
      return
    }
    detailErrorMessage.value = error instanceof Error ? error.message : '加载设备详情失败'
  } finally {
    if (detailAbortController === controller) {
      detailLoading.value = false
      detailAbortController = null
    }
  }
}

async function loadEditableDetail(id: string | number) {
  const res = await deviceApi.getDeviceById(id)
  if (res.code === 200 && res.data) {
    resetFormData(res.data)
  }
}

function handleSearch() {
  searchForm.deviceId = searchForm.deviceId.trim()
  searchForm.productKey = searchForm.productKey.trim()
  searchForm.deviceCode = searchForm.deviceCode.trim()
  searchForm.deviceName = searchForm.deviceName.trim()
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

function removeAppliedFilter(key: DeviceFilterKey) {
  if (key === 'deviceId') {
    searchForm.deviceId = ''
  } else if (key === 'productKey') {
    searchForm.productKey = ''
  } else if (key === 'deviceCode') {
    searchForm.deviceCode = ''
  } else if (key === 'deviceName') {
    searchForm.deviceName = ''
  } else if (key === 'onlineStatus') {
    searchForm.onlineStatus = undefined
  } else if (key === 'activateStatus') {
    searchForm.activateStatus = undefined
  } else {
    searchForm.deviceStatus = undefined
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
  clearDevicePageCache()
  void loadDevicePage({
    silent: true,
    force: true,
    silentMessage: '已保留当前结果，正在后台刷新最新设备数据。'
  })
}

function handleOpenBatchImport() {
  batchImportResult.value = null
  batchImportVisible.value = true
}

async function handleAdd() {
  if (productOptions.value.length === 0) {
    await loadProducts()
  }
  editingDeviceId.value = null
  resetFormData()
  formVisible.value = true
}

async function handleEdit(row: Device) {
  try {
    if (productOptions.value.length === 0) {
      await loadProducts()
    }
    editingDeviceId.value = row.id
    await loadEditableDetail(row.id)
    formVisible.value = true
  } catch (error) {
    console.error('加载设备编辑详情失败', error)
    ElMessage.error('加载设备详情失败')
  }
}

function handleOpenDetail(row: Device) {
  void openDetail(row.id)
}

async function handleOpenReplace(row: Device) {
  try {
    if (productOptions.value.length === 0) {
      await loadProducts()
    }
    const res = await deviceApi.getDeviceById(row.id)
    if (res.code === 200 && res.data) {
      replacingDevice.value = res.data
      replaceVisible.value = true
      return
    }
    ElMessage.error(res.msg || '加载待更换设备失败')
  } catch (error) {
    console.error('加载待更换设备失败', error)
    ElMessage.error('加载待更换设备失败')
  }
}

function handleJumpToInsight(row?: Device | null) {
  if (!row?.deviceCode) {
    return
  }
  void router.push({
    path: '/insight',
    query: {
      deviceCode: row.deviceCode
    }
  })
}

function handleMobileRowAction(command: string | number | object, row: Device) {
  if (command === 'replace') {
    void handleOpenReplace(row)
    return
  }
  if (command === 'insight') {
    handleJumpToInsight(row)
    return
  }
  if (command === 'delete') {
    void handleDelete(row)
  }
}

async function handleDelete(row: Device) {
  try {
    await confirmDelete('设备', row.deviceName || row.deviceCode)
    await deviceApi.deleteDevice(row.id)
    ElMessage.success('删除成功')
    clearSelection()
    clearDevicePageCache()
    if (tableData.value.length === 1 && pagination.pageNum > 1) {
      setPageNum(pagination.pageNum - 1)
    }
    await syncListRouteQuery({
      silent: true,
      force: true,
      silentMessage: '已删除设备，正在后台刷新列表。'
    })
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('删除设备失败', error)
  }
}

async function handleBatchDelete() {
  if (selectedRows.value.length === 0) {
    return
  }
  const deletingCount = selectedRows.value.length
  try {
    await confirmAction({
      title: '批量删除设备',
      message: `确认删除选中的 ${deletingCount} 台设备吗？删除后不可恢复。`,
      type: 'warning',
      confirmButtonText: '确认删除'
    })
    await deviceApi.batchDeleteDevices(selectedRows.value.map((item) => item.id))
    ElMessage.success('批量删除成功')
    clearSelection()
    clearDevicePageCache()
    if (deletingCount === tableData.value.length && pagination.pageNum > 1) {
      setPageNum(pagination.pageNum - 1)
    }
    await syncListRouteQuery({
      silent: true,
      force: true,
      silentMessage: '已删除选中设备，正在后台刷新列表。'
    })
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('批量删除设备失败', error)
  }
}

async function handleBatchImportSubmit(payload: DeviceBatchAddPayload) {
  batchImportSubmitting.value = true
  try {
    const res = await deviceApi.batchAddDevices(payload)
    if (res.code !== 200 || !res.data) {
      ElMessage.error(res.msg || '批量导入设备失败')
      return
    }
    batchImportResult.value = res.data
    if (res.data.successCount > 0) {
      clearDevicePageCache()
      resetPage()
      clearSelection()
      await syncListRouteQuery({
        silent: true,
        force: true,
        silentMessage: '已写入新设备，正在后台刷新列表。'
      })
    }
    if (res.data.failureCount === 0) {
      ElMessage.success(`批量导入完成，共新增 ${res.data.successCount} 台设备`)
      return
    }
    if (res.data.successCount === 0) {
      ElMessage.warning(`本次导入未成功写入设备，共 ${res.data.failureCount} 条失败`)
      return
    }
    ElMessage.warning(`批量导入完成，成功 ${res.data.successCount} 条，失败 ${res.data.failureCount} 条`)
  } catch (error) {
    console.error('批量导入设备失败', error)
    ElMessage.error('批量导入设备失败')
  } finally {
    batchImportSubmitting.value = false
  }
}

async function handleReplaceSubmit(payload: DeviceReplacePayload) {
  if (!replacingDevice.value) {
    return
  }
  try {
    await confirmAction({
      title: '确认更换设备',
      message: `确认将设备“${replacingDevice.value.deviceCode}”更换为新设备“${payload.deviceCode}”吗？提交后旧设备会自动停用并记录替换关系。`,
      type: 'warning',
      confirmButtonText: '确认更换'
    })
    replaceSubmitting.value = true
    const res = await deviceApi.replaceDevice(replacingDevice.value.id, payload)
    if (res.code !== 200 || !res.data) {
      ElMessage.error(res.msg || '设备更换失败')
      return
    }
    ElMessage.success(`设备更换成功，新设备编码：${res.data.targetDeviceCode}`)
    replaceVisible.value = false
    replacingDevice.value = null
    clearDevicePageCache()
    resetPage()
    clearSelection()
    await syncListRouteQuery({
      silent: true,
      force: true,
      silentMessage: '已提交设备更换，正在后台刷新列表。'
    })
    await openDetail(res.data.targetDeviceId)
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('设备更换失败', error)
    ElMessage.error('设备更换失败')
  } finally {
    replaceSubmitting.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    if (editingDeviceId.value) {
      await deviceApi.updateDevice(editingDeviceId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await deviceApi.addDevice(formData)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    clearSelection()
    clearDevicePageCache()
    resetPage()
    await syncListRouteQuery({
      silent: true,
      force: true,
      silentMessage: editingDeviceId.value ? '已提交设备更新，正在后台刷新列表。' : '已新增设备，正在后台刷新列表。'
    })
  } catch (error) {
    console.error('提交设备失败', error)
    ElMessage.error(error instanceof Error ? error.message : '提交设备失败')
  } finally {
    submitLoading.value = false
  }
}

function handleFormClose() {
  formRef.value?.clearValidate()
  resetFormData()
  editingDeviceId.value = null
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
  () =>
    [
      route.query.deviceId,
      route.query.productKey,
      route.query.deviceCode,
      route.query.deviceName,
      route.query.onlineStatus,
      route.query.activateStatus,
      route.query.deviceStatus,
      route.query.pageNum,
      route.query.pageSize
    ] as const,
  () => {
    applyRouteQueryToFilters()
    clearSelection()
    const nextLoadOptions = routeLoadOptions || {}
    routeLoadOptions = null
    void loadDevicePage(nextLoadOptions)
  }
)

watch(detailVisible, (visible) => {
  if (visible) {
    return
  }
  abortDetailRequest()
  detailLoading.value = false
  detailErrorMessage.value = ''
  detailData.value = null
})

watch(batchImportVisible, (value) => {
  if (!value) {
    batchImportResult.value = null
  }
})

watch(replaceVisible, (value) => {
  if (!value) {
    replacingDevice.value = null
  }
})

onBeforeUnmount(() => {
  abortListRequest()
  abortListPrefetchRequest()
  abortDetailRequest()
})

onMounted(async () => {
  hydrateDevicePageCache()
  applyRouteQueryToFilters()
  await loadDevicePage()
})
</script>

<style scoped>
.device-asset-view {
  padding: 20px;
  display: grid;
  gap: 16px;
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(243, 247, 253, 0.66));
  border: 1px solid rgba(41, 60, 92, 0.1);
}

.ops-kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.ops-hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.ops-inline-note {
  padding: 12px 14px;
  border-radius: calc(var(--radius-lg) + 2px);
  border: 1px solid rgba(42, 63, 95, 0.1);
  background: rgba(255, 255, 255, 0.82);
  color: var(--text-caption);
  line-height: 1.7;
}

.ops-filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.device-filter-toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 4px;
}

.device-filter-toggle-row__left {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.device-filter-toggle {
  padding: 0;
  font-weight: 600;
}

.device-filter-toggle-row__hint {
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.6;
}

.device-filter-advanced {
  margin-top: 12px;
  padding-top: 14px;
  border-top: 1px dashed rgba(42, 63, 95, 0.12);
}

.device-applied-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.device-applied-filters__label {
  color: var(--text-caption);
  font-size: 13px;
  font-weight: 600;
}

.device-applied-filters__list {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.device-applied-filters__tag {
  max-width: 100%;
}

.device-applied-filters__clear {
  padding: 0;
}

.device-list-inline-state {
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid color-mix(in srgb, var(--brand) 18%, transparent);
  border-radius: calc(var(--radius-md) + 2px);
  background: color-mix(in srgb, var(--brand) 8%, white);
  color: color-mix(in srgb, var(--brand) 68%, var(--text-caption));
  font-size: 13px;
  line-height: 1.6;
}

.device-list-inline-state--error {
  border-color: color-mix(in srgb, var(--danger, #d84f45) 22%, transparent);
  background: color-mix(in srgb, var(--danger, #d84f45) 7%, white);
  color: color-mix(in srgb, var(--danger, #d84f45) 72%, var(--text-body));
}

.device-result-panel {
  position: relative;
  isolation: isolate;
  min-height: 14rem;
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(247, 250, 255, 0.76));
}

.device-result-panel :deep(.el-loading-mask) {
  border-radius: inherit;
  background: rgba(248, 250, 255, 0.78) !important;
  backdrop-filter: blur(5px);
}

.device-result-panel :deep(.el-loading-spinner .el-loading-text) {
  margin-top: 0.72rem;
  color: color-mix(in srgb, var(--brand) 62%, var(--text-caption));
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.device-result-panel :deep(.el-loading-spinner .path) {
  stroke: var(--brand);
}

.device-empty-state {
  display: grid;
  justify-items: center;
  padding: 0.4rem 0 0.2rem;
}

.device-empty-state :deep(.empty-state) {
  padding-block: 3.25rem 2rem;
}

.device-empty-state__actions {
  display: flex;
  justify-content: center;
}

.device-loading-state {
  display: grid;
  gap: 14px;
  min-height: 14rem;
  padding: 0.72rem 0.1rem 0.2rem;
}

.device-loading-state__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.device-loading-state__desktop {
  display: grid;
  gap: 10px;
}

.device-loading-state__mobile {
  display: none;
  gap: 12px;
}

.device-loading-table {
  display: grid;
  grid-template-columns: 0.38fr 1.28fr 1.22fr 1.1fr 0.84fr 0.8fr 0.8fr 0.92fr 1.12fr 1.08fr;
  gap: 12px;
  align-items: center;
}

.device-loading-table--header {
  padding: 0 0.82rem;
}

.device-loading-table--row {
  padding: 0.92rem 0.82rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.78);
}

.device-loading-mobile-card {
  display: grid;
  gap: 0.8rem;
  padding: 0.92rem 0.96rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.76);
}

.device-loading-mobile-card__header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 0.65rem;
  align-items: start;
}

.device-loading-mobile-card__heading {
  display: grid;
  gap: 0.3rem;
}

.device-loading-mobile-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.device-loading-mobile-card__info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.7rem 0.9rem;
}

.device-loading-mobile-card__field {
  display: grid;
  gap: 0.24rem;
}

.device-loading-pulse {
  position: relative;
  overflow: hidden;
  display: inline-flex;
  border-radius: 999px;
  background: rgba(220, 229, 241, 0.8);
}

.device-loading-pulse::after {
  content: '';
  position: absolute;
  inset: 0;
  transform: translateX(-100%);
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.9), transparent);
  animation: device-loading-shimmer 1.4s ease-in-out infinite;
}

.device-loading-line {
  height: 12px;
  width: 100%;
}

.device-loading-line--header {
  height: 10px;
}

.device-loading-line--key {
  width: 88%;
}

.device-loading-line--title {
  width: 84%;
}

.device-loading-line--meta {
  width: 72%;
}

.device-loading-line--short {
  width: 66%;
}

.device-loading-line--time {
  width: 78%;
}

.device-loading-line--label {
  width: 46%;
}

.device-loading-line--value {
  width: 84%;
}

.device-loading-pill {
  height: 24px;
  width: 90px;
}

.device-loading-pill--status {
  width: 72px;
}

.device-loading-square {
  width: 20px;
  height: 20px;
  border-radius: 6px;
}

.device-mobile-list {
  display: none;
}

.device-mobile-list__grid {
  display: grid;
  gap: 12px;
}

.device-mobile-card {
  display: grid;
  gap: 0.8rem;
  padding: 0.92rem 0.96rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.76);
}

.device-mobile-card__header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 0.65rem;
  align-items: start;
}

.device-mobile-card__heading {
  display: grid;
  gap: 0.2rem;
  min-width: 0;
}

.device-mobile-card__title {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.device-mobile-card__sub {
  overflow: hidden;
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-mobile-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.device-mobile-card__meta-item {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-height: 1.6rem;
  padding: 0.2rem 0.58rem;
  overflow: hidden;
  border-radius: var(--radius-pill);
  background: rgba(78, 89, 105, 0.08);
  color: var(--text-caption);
  font-size: 11.5px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-mobile-card__meta-item--success {
  background: rgba(46, 161, 103, 0.12);
  color: #1b7f51;
}

.device-mobile-card__meta-item--warning {
  background: rgba(226, 148, 46, 0.12);
  color: #9a5c12;
}

.device-mobile-card__meta-item--danger {
  background: rgba(216, 79, 69, 0.12);
  color: #ab3027;
}

.device-mobile-card__info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.7rem 0.9rem;
}

.device-mobile-card__field {
  display: grid;
  gap: 0.18rem;
  min-width: 0;
}

.device-mobile-card__field--full {
  grid-column: 1 / -1;
}

.device-mobile-card__field span {
  color: var(--text-caption-2);
  font-size: 11.5px;
  line-height: 1.4;
}

.device-mobile-card__field strong {
  overflow: hidden;
  color: var(--text-heading);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.52;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-mobile-card__address {
  display: -webkit-box;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.device-mobile-card__actions {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  justify-content: flex-start;
}

.device-mobile-card__actions :deep(.el-button) {
  margin-left: 0;
  padding-inline: 0.1rem;
}

.device-desktop-table {
  display: block;
}

.ops-pagination {
  margin-top: 16px;
}

.device-detail-stack {
  display: grid;
  gap: 16px;
}

.ops-drawer-stack {
  display: grid;
  gap: 16px;
}

.ops-drawer-note {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, transparent);
  border-radius: calc(var(--radius-lg) + 2px);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(245, 249, 255, 0.92)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 8%, transparent), transparent 42%);
}

.ops-drawer-note strong {
  color: var(--text-heading);
  font-size: 14px;
}

.ops-drawer-note span {
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.7;
}

.ops-drawer-section {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: rgba(255, 255, 255, 0.88);
}

.ops-drawer-section__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 16px;
}

.ops-drawer-section__header p {
  margin: 6px 0 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.65;
}

.ops-drawer-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
}

.ops-drawer-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.ops-drawer-grid__full {
  grid-column: 1 / -1;
}

@media (max-width: 900px) {
  .device-asset-view {
    padding: 16px;
  }

  .device-applied-filters {
    align-items: flex-start;
  }

  .device-applied-filters__clear {
    width: 100%;
    justify-content: flex-start;
  }

  .device-filter-toggle-row {
    align-items: stretch;
    flex-direction: column;
  }

  .device-filter-toggle-row__left {
    width: 100%;
  }

  .device-filter-toggle-row .ops-filter-actions {
    width: 100%;
    justify-content: stretch;
  }

  .device-loading-table {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ops-drawer-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .ops-filter-actions {
    justify-content: stretch;
  }
}

@media (max-width: 720px) {
  .device-mobile-list {
    display: block;
  }

  .device-desktop-table {
    display: none;
  }

  .device-loading-state__desktop {
    display: none;
  }

  .device-loading-state__mobile {
    display: grid;
  }

  .device-mobile-card__info,
  .device-loading-mobile-card__info {
    grid-template-columns: 1fr;
  }
}

@keyframes device-loading-shimmer {
  100% {
    transform: translateX(100%);
  }
}
</style>
