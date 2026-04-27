<template>
  <StandardPageShell class="device-asset-view">
    <StandardWorkbenchPanel
      title="设备资产中心"
      description="统一维护设备主数据、在线状态与登记信息。"
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-toolbar
      :show-inline-state="showListInlineState"
      show-pagination
    >
      <template #filters>
        <StandardListFilterHeader :model="searchForm">
          <template #primary>
            <!-- 快速搜索：支持设备编码、设备名称、产品 Key、产品名称关键词搜索 -->
            <el-form-item>
              <el-input
                id="quick-search"
                v-model="quickSearchKeyword"
                placeholder="快速搜索（设备编码、设备名称、产品 Key、产品名称）"
                clearable
                prefix-icon="Search"
                @keyup.enter="handleQuickSearch"
                @clear="handleClearQuickSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.onlineStatus" placeholder="在线状态" clearable>
                <el-option label="在线" :value="1" />
                <el-option label="离线" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.activateStatus" placeholder="激活状态" clearable>
                <el-option label="已激活" :value="1" />
                <el-option label="未激活" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.deviceStatus" placeholder="设备状态" clearable>
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.registrationStatus" placeholder="登记状态" clearable>
                <el-option label="已登记" :value="1" />
                <el-option label="未登记" :value="0" />
              </el-select>
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="query" @click="handleSearch">查询</StandardButton>
            <StandardButton action="reset" @click="handleReset">重置</StandardButton>
            <StandardButton v-permission="'iot:devices:add'" action="add" @click="handleAdd">新增设备</StandardButton>
            <StandardButton v-permission="'iot:devices:import'" action="batch" @click="handleOpenBatchImport">批量导入</StandardButton>
          </template>
        </StandardListFilterHeader>
        <!-- 快速搜索标签 -->
        <div v-if="quickSearchKeyword" class="device-quick-search-tag">
          <el-tag closable class="device-quick-search-tag__chip" @close="handleClearQuickSearch">
            快速搜索：{{ quickSearchKeyword }}
          </el-tag>
        </div>
      </template>

      <template #applied-filters>
        <StandardAppliedFiltersBar
          :tags="activeFilterTags"
          @remove="removeAppliedFilter"
          @clear="handleClearAppliedFilters"
        />
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[
            `已选 ${selectedRows.length} 项`,
            `已登记 ${registeredCount} 台`,
            `在线 ${onlineCount} 台`,
            `已激活 ${activatedCount} 台`,
            `停用 ${disabledCount} 台`
          ]"
        >
          <template #right>
            <StandardActionMenu
              label="更多操作"
              :items="deviceToolbarActions"
              @command="handleToolbarAction"
            />
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <template #inline-state>
        <StandardInlineState
          :message="workbenchInlineMessage"
          :tone="workbenchInlineTone"
        />
      </template>

      <div
        v-loading="loading && hasRecords"
        class="device-result-panel standard-list-surface"
        element-loading-text="正在刷新设备列表"
        element-loading-background="var(--loading-mask-bg)"
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
          <div class="device-mobile-list standard-mobile-record-list">
            <div class="device-mobile-list__grid standard-mobile-record-grid">
              <article
                v-for="row in tableData"
                :key="getDeviceRowKey(row)"
                class="device-mobile-card standard-mobile-record-card"
              >
                <div class="device-mobile-card__header">
                  <el-checkbox
                    :model-value="isRowSelected(row)"
                    :disabled="!isSelectableDeviceRow(row)"
                    @change="(checked) => handleMobileSelectionChange(row, Boolean(checked))"
                  />
                  <div class="device-mobile-card__heading">
                    <strong class="device-mobile-card__title">{{ row.deviceName || '--' }}</strong>
                    <span class="device-mobile-card__sub">{{ row.deviceCode || '--' }}</span>
                  </div>
                  <el-tag :type="row.onlineStatus === 1 ? 'success' : 'info'" round>{{ getOnlineStatusText(row.onlineStatus) }}</el-tag>
                </div>

                <div class="device-mobile-card__meta">
                  <span class="device-mobile-card__meta-item standard-mobile-record-card__meta-item" :title="formatTextValue(row.productKey)">{{ formatTextValue(row.productKey) }}</span>
                  <span
                    :class="[
                      'device-mobile-card__meta-item',
                      'standard-mobile-record-card__meta-item',
                      isRegisteredDeviceRow(row)
                        ? 'device-mobile-card__meta-item--success'
                        : 'device-mobile-card__meta-item--warning'
                    ]"
                  >
                    {{ getRegistrationStatusText(row.registrationStatus) }}
                  </span>
                  <span class="device-mobile-card__meta-item standard-mobile-record-card__meta-item">{{ getNodeTypeText(row.nodeType) }}</span>
                  <span
                    :class="[
                      'device-mobile-card__meta-item',
                      'standard-mobile-record-card__meta-item',
                      row.activateStatus === 1
                        ? 'device-mobile-card__meta-item--success'
                        : row.activateStatus === 0
                          ? 'device-mobile-card__meta-item--warning'
                          : ''
                    ]"
                  >
                    {{ getActivateStatusText(row.activateStatus) }}
                  </span>
                  <span
                    :class="[
                      'device-mobile-card__meta-item',
                      'standard-mobile-record-card__meta-item',
                      row.deviceStatus === 1
                        ? 'device-mobile-card__meta-item--success'
                        : row.deviceStatus === 0
                          ? 'device-mobile-card__meta-item--danger'
                          : ''
                    ]"
                  >
                    {{ getDeviceStatusText(row.deviceStatus) }}
                  </span>
                </div>

                <div class="device-mobile-card__info">
                  <div class="device-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">产品名称</span>
                    <strong class="standard-mobile-record-card__field-value">{{ formatTextValue(row.productName) }}</strong>
                  </div>
                  <div class="device-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">所属机构</span>
                    <strong class="standard-mobile-record-card__field-value">{{ formatTextValue(row.orgName) }}</strong>
                  </div>
                  <div class="device-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">父设备</span>
                    <strong class="standard-mobile-record-card__field-value">{{ formatDeviceRelationValue(row.parentDeviceName, row.parentDeviceCode) }}</strong>
                  </div>
                  <div class="device-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">网关设备</span>
                    <strong class="standard-mobile-record-card__field-value">{{ formatDeviceRelationValue(row.gatewayDeviceName, row.gatewayDeviceCode) }}</strong>
                  </div>
                  <div class="device-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">接入协议</span>
                    <strong class="standard-mobile-record-card__field-value">{{ formatTextValue(row.protocolCode) }}</strong>
                  </div>
                  <div class="device-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">固件版本</span>
                    <strong class="standard-mobile-record-card__field-value">{{ formatTextValue(row.firmwareVersion) }}</strong>
                  </div>
                  <div class="device-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">最近上报</span>
                    <strong class="standard-mobile-record-card__field-value">
                      {{ formatDeviceReportTime(row.lastReportTime, row.updateTime, row.createTime) }}
                    </strong>
                  </div>
                  <div class="device-mobile-card__field device-mobile-card__field--full">
                    <span class="standard-mobile-record-card__field-label">部署位置</span>
                    <strong class="standard-mobile-record-card__field-value device-mobile-card__address">{{ formatTextValue(row.address) }}</strong>
                  </div>
                </div>

                <StandardWorkbenchRowActions
                  variant="card"
                  class="device-mobile-card__actions"
                  :direct-items="getDeviceDirectActions(row)"
                  :menu-items="getDeviceRowActions(row)"
                  @command="(command) => handleRowAction(command, row)"
                />
              </article>
            </div>
          </div>

          <el-table ref="tableRef" class="device-desktop-table" :data="tableData" border stripe @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="48" :selectable="isSelectableDeviceRow" />
            <StandardTableTextColumn
              prop="deviceName"
              label="设备"
              :min-width="200"
              secondary-prop="deviceCode"
            />
            <StandardTableTextColumn prop="registrationStatus" label="登记状态" :width="110">
              <template #default="{ row }">
                <el-tag :type="row.registrationStatus === 1 ? 'success' : 'warning'" round>{{ getRegistrationStatusText(row.registrationStatus) }}</el-tag>
              </template>
            </StandardTableTextColumn>
            <StandardTableTextColumn prop="productKey" label="产品 Key" :min-width="160" />
            <StandardTableTextColumn prop="productName" label="产品名称" :min-width="160" />
            <StandardTableTextColumn prop="orgName" label="所属机构" :min-width="160" />
            <StandardTableTextColumn prop="protocolCode" label="协议" :width="120" />
            <el-table-column prop="onlineStatus" label="在线状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getOnlineStatusTagType(row.onlineStatus)" round>{{ getOnlineStatusText(row.onlineStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="activateStatus" label="激活状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getActivateStatusTagType(row.activateStatus)" round>{{ getActivateStatusText(row.activateStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deviceStatus" label="设备状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getDeviceStatusTagType(row.deviceStatus)" round>{{ getDeviceStatusText(row.deviceStatus) }}</el-tag>
              </template>
            </el-table-column>
            <StandardTableTextColumn prop="firmwareVersion" label="固件版本" :width="130" />
            <StandardTableTextColumn prop="lastReportTime" label="最近上报" :width="180">
              <template #default="{ row }">
                {{ formatDeviceReportTime(row.lastReportTime, row.updateTime, row.createTime) }}
              </template>
            </StandardTableTextColumn>
            <StandardTableTextColumn prop="address" label="部署位置" :min-width="180" />
            <StandardTableTextColumn prop="createTime" label="创建时间" :width="180">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </StandardTableTextColumn>
            <el-table-column
              label="操作"
              :width="deviceActionColumnWidth"
              fixed="right"
              class-name="standard-row-actions-column"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row }">
                <StandardWorkbenchRowActions
                  variant="table"
                  :direct-items="getDeviceDirectActions(row)"
                  :menu-items="getDeviceRowActions(row)"
                  @command="(command) => handleRowAction(command, row)"
                />
              </template>
            </el-table-column>
          </el-table>
        </template>

        <div v-else-if="!loading" class="device-empty-state">
          <EmptyState :title="emptyStateTitle" :description="emptyStateDescription" />
          <div class="device-empty-state__actions">
            <StandardButton v-if="hasAppliedFilters" action="reset" @click="handleClearAppliedFilters">清空筛选条件</StandardButton>
            <StandardButton v-else v-permission="'iot:devices:add'" action="add" @click="handleAdd">新增设备</StandardButton>
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

    <StandardDetailDrawer
      v-model="detailVisible"
      :title="detailTitle"
      :subtitle="detailSubtitle"
      :loading="detailLoading"
      :error-message="detailErrorMessage"
      :empty="!detailData"
    >
      <div v-if="detailData" class="device-detail-stack">
        <DeviceDetailWorkbench
          :device="detailData"
        />
      </div>

      <template #footer>
        <StandardDrawerFooter @cancel="detailVisible = false">
          <StandardButton action="cancel" class="standard-drawer-footer__button standard-drawer-footer__button--ghost" @click="detailVisible = false">
            关闭
          </StandardButton>
          <StandardButton
            action="confirm"
            class="standard-drawer-footer__button standard-drawer-footer__button--primary"
            :disabled="!canJumpToInsight(detailData)"
            @click="handleJumpToInsight(detailData)"
          >
            进入对象洞察台
          </StandardButton>
        </StandardDrawerFooter>
      </template>
    </StandardDetailDrawer>

    <DeviceCapabilityWorkbenchDrawer
      v-model="capabilityVisible"
      :device="capabilityDevice"
      :overview="capabilityOverview"
      :commands="commandRecords"
      :capability-loading="capabilityLoading"
      :command-loading="commandLoading"
      @execute-capability="handleExecuteCapability"
      @refresh-commands="handleRefreshCommands"
    />

    <DeviceCapabilityExecuteDrawer
      v-model="capabilityExecuteVisible"
      :device-code="capabilityDevice?.deviceCode || detailData?.deviceCode || ''"
      :capability="executingCapability"
      :submitting="capabilityExecuteSubmitting"
      @submit="handleExecuteCapabilitySubmit"
    />

    <DeviceOnboardingSuggestionDrawer
      v-model="suggestionVisible"
      :suggestion="onboardingSuggestion"
      :loading="suggestionLoading"
      :error-message="suggestionErrorMessage"
      :source-row="onboardingSuggestionSource"
    />

    <StandardFormDrawer
      v-model="formVisible"
      :title="formTitle"
      :subtitle="formSubtitle"
      size="44rem"
      @close="handleFormClose"
    >
      <div class="ops-drawer-stack">
        <div class="ops-drawer-note">
          <strong>维护提示</strong>
          <span>
            {{
              formMode === 'register'
                ? '当前记录来自未登记上报线索；请核对产品归属、设备编码、父子拓扑和认证字段，提交后会直接转成已登记设备。'
                : '设备列表先服务“库存可见、责任清晰、操作可追踪”。建议至少补齐产品归属、设备编码、激活状态、设备状态和部署位置。'
            }}
          </span>
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
                <h3>父子拓扑</h3>
                <p>{{ formRelationHint }}</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="父设备" prop="parentDeviceId" class="ops-drawer-grid__full">
                <el-select
                  v-model="formData.parentDeviceId"
                  filterable
                  clearable
                  placeholder="请选择父设备（选填）"
                  :loading="deviceOptionsLoading"
                >
                  <el-option
                    v-for="option in formParentOptions"
                    :key="String(option.id)"
                    :label="formatDeviceOptionLabel(option)"
                    :value="option.id"
                  />
                </el-select>
              </el-form-item>
            </div>
            <div class="device-form-relation-summary">
              <div class="device-form-relation-card">
                <span>当前父设备</span>
                <strong>{{ formatDeviceRelationValue(selectedFormParentOption?.deviceName, selectedFormParentOption?.deviceCode) }}</strong>
              </div>
              <div class="device-form-relation-card">
                <span>自动关联网关</span>
                <strong>{{ formGatewayPreview }}</strong>
              </div>
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
          :confirm-text="formSubmitText"
          @cancel="formVisible = false"
          @confirm="handleSubmit"
        >
          <StandardButton action="cancel" class="standard-drawer-footer__button standard-drawer-footer__button--ghost" @click="formVisible = false">
            取消
          </StandardButton>
          <StandardButton
            v-permission="submitPermission"
            action="confirm"
            class="standard-drawer-footer__button standard-drawer-footer__button--primary"
            :loading="submitLoading"
            @click="handleSubmit"
          >
            {{ formSubmitText }}
          </StandardButton>
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
      :device-options="replaceParentOptions"
      :product-loading="productLoading"
      :device-options-loading="deviceOptionsLoading"
      :refreshing="replaceRefreshing"
      :refresh-message="replaceRefreshMessage"
      :refresh-state="replaceRefreshState"
      :submitting="replaceSubmitting"
      @dirty-change="handleReplaceDirtyChange"
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
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type TableInstance } from 'element-plus'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import DeviceDetailWorkbench from '@/components/device/DeviceDetailWorkbench.vue'
import DeviceCapabilityWorkbenchDrawer from '@/components/device/DeviceCapabilityWorkbenchDrawer.vue'
import DeviceCapabilityExecuteDrawer from '@/components/device/DeviceCapabilityExecuteDrawer.vue'
import DeviceOnboardingSuggestionDrawer from '@/components/device/DeviceOnboardingSuggestionDrawer.vue'
import DeviceBatchImportDrawer from '@/components/DeviceBatchImportDrawer.vue'
import DeviceReplaceDrawer from '@/components/DeviceReplaceDrawer.vue'
import EmptyState from '@/components/EmptyState.vue'
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import StandardInlineState from '@/components/StandardInlineState.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardPageShell from '@/components/StandardPageShell.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import { accessErrorApi } from '@/api/accessError'
import { isHandledRequestError, resolveRequestErrorMessage } from '@/api/request'
import { deviceApi } from '@/api/device'
import { productApi } from '@/api/product'
import { useServerPagination } from '@/composables/useServerPagination'
import { usePermissionStore } from '@/stores/permission'
import type {
  CommandRecordPageItem,
  Device,
  DeviceAddPayload,
  DeviceAccessErrorLog,
  DeviceBatchAddPayload,
  DeviceBatchAddResult,
  DeviceCapability,
  DeviceCapabilityExecutePayload,
  DeviceCapabilityOverview,
  DeviceOnboardingBatchResult,
  DeviceOnboardingSuggestion,
  DeviceOption,
  DeviceReplacePayload,
  DeviceReplaceResult,
  Product
} from '@/types/api'
import {
  buildDevicePageCacheKey,
  cloneDeviceDetailCacheEntry,
  cloneDevicePageCacheEntry,
  createDeviceDetailCacheEntry,
  createDevicePageCacheEntry,
  deserializeDeviceDetailCacheEntries,
  deserializeDevicePageCacheEntries,
  getDeviceRowKey,
  getNextDevicePageQuery,
  isDeviceDetailCacheFresh,
  isDevicePageCacheFresh,
  matchesDeviceFilters,
  mergeLocalDeviceRow,
  prependLocalDeviceRow,
  removeLocalDeviceRow,
  removeSelectedDeviceSnapshot,
  replaceSelectedDeviceSnapshot,
  type DeviceDetailCacheEntry,
  type DevicePageCacheEntry,
  type DevicePageQuerySnapshot,
  resolveDevicePageLoadStrategy,
  serializeDeviceDetailCacheEntries,
  serializeDevicePageCacheEntries,
  shouldRefreshDeviceDetail
} from '@/views/deviceWorkbenchState'
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv'
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns'
import { confirmAction, confirmDelete, isConfirmCancelled } from '@/utils/confirm'
import { resolveWorkbenchActionColumnWidth } from '@/utils/adaptiveActionColumn'
import { formatDateTime, formatDeviceReportTime } from '@/utils/format'
import { describeDiagnosticSource, resolveDiagnosticContext } from '@/utils/iotAccessDiagnostics'

interface DeviceSearchForm {
  deviceId: string
  keyword: string
  productKey: string
  productName: string
  deviceCode: string
  deviceName: string
  onlineStatus: number | undefined
  activateStatus: number | undefined
  deviceStatus: number | undefined
  registrationStatus: number | undefined
}

type DeviceFilterKey = keyof DeviceSearchForm

interface DeviceFormState extends DeviceAddPayload {}

type DeviceFormMode = 'create' | 'edit' | 'register'

interface DevicePageLoadOptions {
  silent?: boolean
  force?: boolean
  silentMessage?: string
}

interface DeviceRowAction {
  key?: string
  command: 'replace' | 'insight' | 'delete' | 'suggestion' | 'capability'
  label: string
}

interface DeviceDirectAction {
  key?: string
  command: 'detail' | 'edit'
  label: string
}

interface DeviceToolbarAction {
  key?: string
  command: 'batch-activate' | 'batch-delete' | 'export-config' | 'export-selected' | 'export-current' | 'clear-selection'
  label: string
  disabled?: boolean
  divided?: boolean
}

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()
const tableRef = ref<TableInstance>()
const formRef = ref<FormInstance>()

const loading = ref(false)
const submitLoading = ref(false)
const productLoading = ref(false)
const deviceOptionsLoading = ref(false)
const formVisible = ref(false)
const formRefreshing = ref(false)
const detailVisible = ref(false)
const capabilityVisible = ref(false)
const batchImportVisible = ref(false)
const batchImportSubmitting = ref(false)
const replaceVisible = ref(false)
const suggestionVisible = ref(false)
const replaceSubmitting = ref(false)
const replaceRefreshing = ref(false)
const detailLoading = ref(false)
const detailRefreshing = ref(false)
const detailErrorMessage = ref('')
const detailRefreshErrorMessage = ref('')
const listRefreshMessage = ref('')
const listRefreshState = ref<'info' | 'error' | ''>('')
const diagnosticContext = computed(() => resolveDiagnosticContext(route.query as Record<string, unknown>))
const formRefreshMessage = ref('')
const formRefreshState = ref<'info' | 'warning' | 'error' | ''>('')
const replaceRefreshMessage = ref('')
const replaceRefreshState = ref<'info' | 'warning' | 'error' | ''>('')
const suggestionLoading = ref(false)
const suggestionErrorMessage = ref('')
const formMode = ref<DeviceFormMode>('create')
const editingDeviceId = ref<string | number | null>(null)
const replaceFormDirtySinceOpen = ref(false)

const tableData = ref<Device[]>([])
const selectedRows = ref<Device[]>([])
const productOptions = ref<Product[]>([])
const deviceOptions = ref<DeviceOption[]>([])
const detailData = ref<Device | null>(null)
const capabilityDevice = ref<Device | null>(null)
const capabilityOverview = ref<DeviceCapabilityOverview | null>(null)
const commandRecords = ref<CommandRecordPageItem[]>([])
const registerSourceRow = ref<Device | null>(null)
const batchImportResult = ref<DeviceBatchAddResult | null>(null)
const replacingDevice = ref<Device | null>(null)
const onboardingSuggestion = ref<DeviceOnboardingSuggestion | null>(null)
const onboardingSuggestionSource = ref<Device | null>(null)
const capabilityExecuteVisible = ref(false)
const capabilityExecuteSubmitting = ref(false)
const executingCapability = ref<DeviceCapability | null>(null)
const capabilityLoading = ref(false)
const commandLoading = ref(false)

const exportColumnDialogVisible = ref(false)
const exportColumnStorageKey = 'device-asset-view'
const defaultPageSize = 10
const advancedFilterKeys: readonly DeviceFilterKey[] = ['deviceId', 'onlineStatus', 'activateStatus', 'deviceStatus']
let latestListRequestId = 0
let latestDetailRequestId = 0
let latestCapabilityRequestSeed = 0
let latestCapabilityOverviewRequestId = 0
let latestCapabilityCommandRequestId = 0
let latestEditRequestId = 0
let latestReplaceRequestId = 0
let listAbortController: AbortController | null = null
let listPrefetchAbortController: AbortController | null = null
let detailAbortController: AbortController | null = null
let editAbortController: AbortController | null = null
let replaceAbortController: AbortController | null = null
let productLoadPromise: Promise<void> | null = null
let deviceOptionLoadPromise: Promise<void> | null = null
let routeLoadOptions: DevicePageLoadOptions | null = null
const deviceDetailCache = new Map<string, DeviceDetailCacheEntry>()
const devicePageCache = new Map<string, DevicePageCacheEntry>()
const deviceDetailCacheTtlMs = 5 * 60_000
const deviceDetailCacheLimit = 12
const deviceDetailCacheSessionStorageKey = 'iot.devices.detail-cache'
const devicePageCacheTtlMs = 30_000
const devicePageCacheLimit = 8
const devicePageCacheSessionStorageKey = 'iot.devices.page-cache'
let activeEditSessionId = 0
let activeReplaceSessionId = 0
let formDirtySinceOpen = false
let suppressFormDirtyTracking = false

const searchForm = reactive<DeviceSearchForm>({
  deviceId: '',
  keyword: '',
  productKey: '',
  productName: '',
  deviceCode: '',
  deviceName: '',
  onlineStatus: undefined,
  activateStatus: undefined,
  deviceStatus: undefined,
  registrationStatus: undefined
})
const appliedFilters = reactive<DeviceSearchForm>({
  deviceId: '',
  keyword: '',
  productKey: '',
  productName: '',
  deviceCode: '',
  deviceName: '',
  onlineStatus: undefined,
  activateStatus: undefined,
  deviceStatus: undefined,
  registrationStatus: undefined
})
const quickSearchKeyword = ref('')
const showAdvancedFilters = ref(false)

const createDefaultFormData = (): DeviceFormState => ({
  productKey: '',
  deviceName: '',
  deviceCode: '',
  parentDeviceId: null,
  parentDeviceCode: '',
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

const { pagination, applyPageResult, resetPage, setPageNum, setPageSize, setTotal } = useServerPagination(defaultPageSize)

const formTitle = computed(() => {
  if (formMode.value === 'edit') {
    return '编辑设备'
  }
  if (formMode.value === 'register') {
    return '登记设备'
  }
  return '新增设备'
})
const formSubtitle = computed(() =>
  formMode.value === 'register'
    ? '基于未登记上报线索补齐正式设备档案，提交后会直接转为已登记设备。'
    : '统一通过右侧抽屉维护设备主数据、父子拓扑、状态、认证字段和部署信息。'
)
const formSubmitText = computed(() => (formMode.value === 'edit' ? '保存设备变更' : '提交设备建档'))
const submitPermission = computed(() => (formMode.value === 'edit' ? 'iot:devices:update' : 'iot:devices:add'))
const detailTitle = computed(() => {
  if (detailData.value?.registrationStatus === 0) {
    return detailData.value.deviceCode || '未登记设备'
  }
  return detailData.value?.deviceName || detailData.value?.deviceCode || '设备详情'
})
const detailIsRegistered = computed(() => isRegisteredDeviceRow(detailData.value))
const detailSubtitle = computed(() =>
  detailIsRegistered.value
    ? '统一查看资产判断、部署台账、运行台账与建档补充。'
    : '当前设备仍未登记，详情按失败来源和最近载荷组织。'
)
const onlineCount = computed(() => tableData.value.filter((item) => item.onlineStatus === 1).length)
const activatedCount = computed(() => tableData.value.filter((item) => item.activateStatus === 1).length)
const disabledCount = computed(() => tableData.value.filter((item) => item.deviceStatus === 0).length)
const registeredCount = computed(() => tableData.value.filter((item) => item.registrationStatus !== 0).length)
const deviceOptionMap = computed(() => new Map(deviceOptions.value.map((option) => [normalizeIdKey(option.id), option])))
const selectedFormProduct = computed(() => productOptions.value.find((product) => product.productKey === formData.productKey) ?? null)
const selectedFormNodeType = computed(() => selectedFormProduct.value?.nodeType ?? null)
const selectedFormParentOption = computed(() => deviceOptionMap.value.get(normalizeIdKey(formData.parentDeviceId)))
const formParentOptions = computed(() => {
  const currentId = normalizeIdKey(editingDeviceId.value)
  return deviceOptions.value.filter((option) => {
    const optionId = normalizeIdKey(option.id)
    if (!currentId) {
      return true
    }
    if (optionId === currentId) {
      return false
    }
    return !isDescendantOption(option, currentId)
  })
})
const replaceParentOptions = computed(() => {
  const currentId = normalizeIdKey(replacingDevice.value?.id)
  return deviceOptions.value.filter((option) => normalizeIdKey(option.id) !== currentId)
})
const formRelationHint = computed(() =>
  selectedFormNodeType.value === 3
    ? '当前产品为网关子设备，选择父设备后会自动继承所属网关；如需解除关系，可直接清空。'
    : '如需维护父子资产结构，可在这里指定上级设备；未选择时表示当前设备独立建档。'
)
const formGatewayPreview = computed(() => resolveGatewayPreviewText(selectedFormParentOption.value, selectedFormNodeType.value))
const selectedRowKeySet = computed(() => new Set(selectedRows.value.map((item) => getDeviceRowKey(item)).filter(Boolean)))
const hasRecords = computed(() => tableData.value.length > 0)
const showListSkeleton = computed(() => loading.value && !hasRecords.value)
const diagnosticEntryMessage = computed(() => {
  if (!diagnosticContext.value) {
    return ''
  }
  const sourceLabel = describeDiagnosticSource(diagnosticContext.value.sourcePage)
  const traceLabel = diagnosticContext.value.traceId ? `Trace ${diagnosticContext.value.traceId}` : ''
  const deviceLabel = diagnosticContext.value.deviceCode ? `设备 ${diagnosticContext.value.deviceCode}` : ''
  return [sourceLabel ? `来自${sourceLabel}` : '', traceLabel, deviceLabel, '优先核对登记状态、在线态与失败来源。']
    .filter(Boolean)
    .join(' · ')
})
const workbenchInlineMessage = computed(() => listRefreshMessage.value || diagnosticEntryMessage.value)
const workbenchInlineTone = computed<'info' | 'error'>(() => (listRefreshState.value === 'error' ? 'error' : 'info'))
const showListInlineState = computed(() => Boolean(workbenchInlineMessage.value) && (hasRecords.value || Boolean(diagnosticEntryMessage.value)))
const selectedBatchActivatableRows = computed(() => selectedRows.value.filter((row) => canBatchActivateOnboarding(row)))
const deviceToolbarActions = computed<DeviceToolbarAction[]>(() => {
  const actions: DeviceToolbarAction[] = []

  if (permissionStore.hasPermission('iot:devices:add')) {
    actions.push({
      key: 'batch-activate',
      command: 'batch-activate',
      label: '批量转正式设备',
      disabled:
        selectedRows.value.length === 0 || selectedBatchActivatableRows.value.length !== selectedRows.value.length
    })
  }

  if (permissionStore.hasPermission('iot:devices:delete')) {
    actions.push({
      key: 'batch-delete',
      command: 'batch-delete',
      label: '批量删除',
      disabled: selectedRows.value.length === 0 || selectedRows.value.some((row) => !isRegisteredDeviceRow(row))
    })
  }

  if (permissionStore.hasPermission('iot:devices:export')) {
    actions.push({
      key: 'export-config',
      command: 'export-config',
      label: '导出列设置',
      divided: actions.length > 0
    })
    actions.push({
      key: 'export-selected',
      command: 'export-selected',
      label: '导出选中',
      disabled: selectedRows.value.length === 0
    })
    actions.push({
      key: 'export-current',
      command: 'export-current',
      label: '导出当前结果',
      disabled: tableData.value.length === 0
    })
  }

  actions.push({
    key: 'clear-selection',
    command: 'clear-selection',
    label: '清空选中',
    disabled: selectedRows.value.length === 0,
    divided: actions.length > 0
  })

  return actions
})
const advancedAppliedFilterCount = computed(() => countFilledFilters(appliedFilters, advancedFilterKeys))
const advancedFilterHint = computed(() => {
  if (showAdvancedFilters.value || advancedAppliedFilterCount.value === 0) {
    return ''
  }
  return `更多条件已生效 ${advancedAppliedFilterCount.value} 项`
})
const activeFilterTags = computed(() => {
  const tags: Array<{ key: DeviceFilterKey; label: string }> = []
  const deviceId = appliedFilters.deviceId.trim()
  if (deviceId) {
    tags.push({ key: 'deviceId', label: `设备 ID：${deviceId}` })
  }
  const keyword = appliedFilters.keyword.trim()
  const productKey = appliedFilters.productKey.trim()
  if (productKey && !keyword) {
    tags.push({ key: 'productKey', label: `产品 Key：${productKey}` })
  }
  const productName = appliedFilters.productName.trim()
  if (productName && !keyword) {
    tags.push({ key: 'productName', label: `产品名称：${productName}` })
  }
  const deviceCode = appliedFilters.deviceCode.trim()
  if (deviceCode && !keyword) {
    tags.push({ key: 'deviceCode', label: `设备编码：${deviceCode}` })
  }
  const deviceName = appliedFilters.deviceName.trim()
  if (deviceName && !keyword) {
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
  if (appliedFilters.registrationStatus !== undefined) {
    tags.push({ key: 'registrationStatus', label: `登记状态：${getRegistrationStatusText(appliedFilters.registrationStatus)}` })
  }
  return tags
})
const hasAppliedFilters = computed(() => Boolean(appliedFilters.keyword.trim()) || activeFilterTags.value.length > 0)
const emptyStateTitle = computed(() => {
  if (hasAppliedFilters.value) {
    return '没有符合条件的设备'
  }
  if (searchForm.registrationStatus === 0 || appliedFilters.registrationStatus === 0) {
    return '还没有未登记上报设备'
  }
  return '还没有设备资产'
})
const emptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? '已生效筛选暂时没有匹配结果，可以调整条件，或者直接清空当前筛选。'
    : searchForm.registrationStatus === 0 || appliedFilters.registrationStatus === 0
      ? '当前还没有命中未登记上报名单，可先切回全部或已登记视图继续排查。'
      : '当前还没有设备资产，先新增设备或批量导入，再继续做台账维护和状态核查。'
)

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
  { key: 'registrationStatus', label: '登记状态', formatter: (value) => getRegistrationStatusText(value as number | null | undefined) },
  { key: 'deviceCode', label: '设备编码' },
  { key: 'deviceName', label: '设备名称' },
  { key: 'parentDeviceCode', label: '父设备编码' },
  { key: 'parentDeviceName', label: '父设备名称' },
  { key: 'gatewayDeviceCode', label: '网关设备编码' },
  { key: 'gatewayDeviceName', label: '网关设备名称' },
  { key: 'productKey', label: '产品 Key' },
  { key: 'productName', label: '产品名称' },
  { key: 'protocolCode', label: '接入协议' },
  { key: 'assetSourceType', label: '数据来源', formatter: (value) => getSourceTypeText(value as string | null | undefined) },
  { key: 'nodeType', label: '节点类型', formatter: (value) => getNodeTypeText(Number(value)) },
  { key: 'onlineStatus', label: '在线状态', formatter: (value) => getOnlineStatusText(Number(value)) },
  { key: 'activateStatus', label: '激活状态', formatter: (value) => getActivateStatusText(Number(value)) },
  { key: 'deviceStatus', label: '设备状态', formatter: (value) => getDeviceStatusText(Number(value)) },
  { key: 'firmwareVersion', label: '固件版本' },
  { key: 'ipAddress', label: 'IP 地址' },
  { key: 'address', label: '部署位置' },
  {
    key: 'lastReportTime',
    label: '最近上报',
    formatter: (_value, row) => formatDeviceReportTime(row.lastReportTime, row.updateTime, row.createTime)
  },
  { key: 'createTime', label: '创建时间', formatter: (value) => formatDateTime(String(value || '')) }
]

const exportColumnOptions = toCsvColumnOptions(exportColumns)
const exportPresets = [
  { label: '默认模板', keys: exportColumns.map((column) => String(column.key)) },
  { label: '运维模板', keys: ['deviceCode', 'deviceName', 'parentDeviceCode', 'gatewayDeviceCode', 'productKey', 'onlineStatus', 'activateStatus', 'deviceStatus', 'lastReportTime', 'address'] },
  { label: '库存模板', keys: ['id', 'deviceCode', 'deviceName', 'parentDeviceCode', 'productKey', 'productName', 'deviceStatus', 'createTime'] }
]
const selectedExportColumnKeys = ref<string[]>(
  loadCsvColumnSelection(
    exportColumnStorageKey,
    exportColumns.map((column) => String(column.key))
  )
)

function getOnlineStatusText(value?: number | null) {
  if (value === undefined || value === null) {
    return '--'
  }
  return value === 1 ? '在线' : '离线'
}

function getActivateStatusText(value?: number | null) {
  if (value === undefined || value === null) {
    return '--'
  }
  return value === 1 ? '已激活' : '未激活'
}

function getDeviceStatusText(value?: number | null) {
  if (value === undefined || value === null) {
    return '--'
  }
  return value === 1 ? '启用' : '禁用'
}

function getRegistrationStatusText(value?: number | null) {
  return value === 0 ? '未登记' : '已登记'
}

function getSourceTypeText(value?: string | null) {
  if (value === 'access_error') {
    return '失败归档'
  }
  if (value === 'dispatch_failed') {
    return '失败轨迹'
  }
  return '设备主档'
}

function getOnlineStatusTagType(value?: number | null) {
  if (value === undefined || value === null) {
    return 'info'
  }
  return value === 1 ? 'success' : 'info'
}

function getActivateStatusTagType(value?: number | null) {
  if (value === undefined || value === null) {
    return 'info'
  }
  return value === 1 ? 'success' : 'warning'
}

function getDeviceStatusTagType(value?: number | null) {
  if (value === undefined || value === null) {
    return 'info'
  }
  return value === 1 ? 'success' : 'danger'
}

function getNodeTypeText(value?: number | null) {
  if (value === 1) {
    return '直连设备'
  }
  if (value === 2) {
    return '网关设备'
  }
  if (value === 3) {
    return '网关子设备'
  }
  return '--'
}

function formatTextValue(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '--'
  }
  return String(value)
}

function normalizeIdKey(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return ''
  }
  return String(value)
}

function formatDeviceRelationValue(name?: string | null, code?: string | null) {
  if (name && code) {
    return `${name} (${code})`
  }
  return name || code || '--'
}

function formatDeviceOptionLabel(option: DeviceOption) {
  const relationLabel = formatDeviceRelationValue(option.deviceName, option.deviceCode)
  const suffix = [option.productKey, getNodeTypeText(option.nodeType), option.deviceStatus === 0 ? '禁用' : '启用']
    .filter(Boolean)
    .join(' / ')
  return suffix ? `${relationLabel} - ${suffix}` : relationLabel
}

function isDescendantOption(option: DeviceOption, currentId: string) {
  let parentId = normalizeIdKey(option.parentDeviceId)
  const visited = new Set<string>()
  while (parentId) {
    if (parentId === currentId) {
      return true
    }
    if (visited.has(parentId)) {
      return false
    }
    visited.add(parentId)
    parentId = normalizeIdKey(deviceOptionMap.value.get(parentId)?.parentDeviceId)
  }
  return false
}

function resolveGatewayPreviewText(parentOption?: DeviceOption | null, nodeType?: number | null) {
  if (nodeType !== 3) {
    return '当前产品不是网关子设备'
  }
  if (!parentOption) {
    return '请选择父设备后自动带出'
  }
  if (parentOption.nodeType === 2) {
    return formatDeviceRelationValue(parentOption.deviceName, parentOption.deviceCode)
  }
  const gatewayOption = deviceOptionMap.value.get(normalizeIdKey(parentOption.gatewayId))
  if (gatewayOption) {
    return formatDeviceRelationValue(gatewayOption.deviceName, gatewayOption.deviceCode)
  }
  return '父设备链路中暂未识别到网关设备'
}

function hasFilledFilter(filters: DeviceSearchForm, key: DeviceFilterKey) {
  const value = filters[key]
  if (typeof value === 'string') {
    return value.trim() !== ''
  }
  return value !== undefined
}

function isRegisteredDeviceRow(row?: Partial<Device> | null) {
  return row?.registrationStatus !== 0
}

function isSelectableDeviceRow(row?: Device) {
  return isRegisteredDeviceRow(row) || canBatchActivateOnboarding(row)
}

function canEditDeviceRow(row?: Device | null) {
  return Boolean(row)
}

function hasEditPermissionForRow(row?: Device | null) {
  if (!row) {
    return false
  }
  return isRegisteredDeviceRow(row)
    ? permissionStore.hasPermission('iot:devices:update')
    : permissionStore.hasPermission('iot:devices:add')
}

function canReplaceDeviceRow(row?: Device | null) {
  return Boolean(row && isRegisteredDeviceRow(row))
}

function canDeleteDeviceRow(row?: Device | null) {
  return Boolean(row && isRegisteredDeviceRow(row) && row.id !== undefined && row.id !== null && row.id !== '')
}

function canJumpToInsight(row?: Device | null) {
  return Boolean(row?.deviceCode && isRegisteredDeviceRow(row))
}

function canSuggestOnboarding(row?: Device | null) {
  return Boolean(row?.lastTraceId && !isRegisteredDeviceRow(row))
}

function canBatchActivateOnboarding(row?: Device | null) {
  return Boolean(row?.lastTraceId && !isRegisteredDeviceRow(row))
}

function getDeviceDirectActions(row: Device): DeviceDirectAction[] {
  const actions: DeviceDirectAction[] = [{ key: 'detail', command: 'detail', label: '详情' }]

  if (canEditDeviceRow(row) && hasEditPermissionForRow(row)) {
    actions.push({ key: 'edit', command: 'edit', label: '编辑' })
  }

  return actions
}

function getDeviceRowActions(row: Device): DeviceRowAction[] {
  const actions: DeviceRowAction[] = []
  if (canSuggestOnboarding(row)) {
    actions.push({ key: 'suggestion', command: 'suggestion', label: '接入建议' })
  }
  if (isRegisteredDeviceRow(row) && permissionStore.hasPermission('iot:device-capability:view')) {
    actions.push({ key: 'capability', command: 'capability', label: '设备操作' })
  }
  if (canReplaceDeviceRow(row)) {
    if (permissionStore.hasPermission('iot:devices:replace')) {
      actions.push({ key: 'replace', command: 'replace', label: '更换' })
    }
    actions.push({ key: 'insight', command: 'insight', label: '洞察' })
    if (permissionStore.hasPermission('iot:devices:delete')) {
      actions.push({ key: 'delete', command: 'delete', label: '删除' })
    }
  }
  return actions
}

const deviceActionColumnWidth = computed(() => {
  const visibleRowWidths = tableData.value.map((row) =>
    resolveWorkbenchActionColumnWidth({
      directItems: getDeviceDirectActions(row),
      menuItems: getDeviceRowActions(row)
    })
  )
  const resolvedWidth =
    visibleRowWidths.length > 0
      ? Math.max(...visibleRowWidths)
      : resolveWorkbenchActionColumnWidth({
          directItems: [
            { command: 'detail', label: '详情' },
            ...(permissionStore.hasPermission('iot:devices:update') ? [{ command: 'edit', label: '编辑' }] : [])
          ],
          menuItems: [{ command: 'more', label: '更多' }]
        })

  return resolvedWidth
})

function countFilledFilters(filters: DeviceSearchForm, keys: readonly DeviceFilterKey[]) {
  return keys.reduce((count, key) => count + (hasFilledFilter(filters, key) ? 1 : 0), 0)
}

function applyQuickSearchKeywordToFilters() {
  const keyword = quickSearchKeyword.value.trim()
  searchForm.keyword = keyword
  searchForm.productKey = ''
  searchForm.productName = ''
  searchForm.deviceCode = ''
  searchForm.deviceName = ''
  if (!keyword) {
    return ''
  }

  return keyword
}

function syncQuickSearchKeywordFromFilters() {
  quickSearchKeyword.value = searchForm.keyword.trim()
}

function resetFormData(source?: Partial<Device>) {
  Object.assign(formData, createDefaultFormData(), {
    productKey: source?.productKey || '',
    deviceName: source?.deviceName || '',
    deviceCode: source?.deviceCode || '',
    parentDeviceId: source?.parentDeviceId ?? null,
    parentDeviceCode: '',
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

function applyFormDataWithoutDirty(source?: Partial<Device>) {
  suppressFormDirtyTracking = true
  try {
    resetFormData(source)
  } finally {
    suppressFormDirtyTracking = false
  }
}

function clearSelection() {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

function matchesCurrentFilters(device: Device) {
  return matchesDeviceFilters(device, {
    deviceId: appliedFilters.deviceId,
    keyword: appliedFilters.keyword,
    productKey: appliedFilters.productKey,
    productName: appliedFilters.productName,
    deviceCode: appliedFilters.deviceCode,
    deviceName: appliedFilters.deviceName,
    onlineStatus: appliedFilters.onlineStatus,
    activateStatus: appliedFilters.activateStatus,
    deviceStatus: appliedFilters.deviceStatus,
    registrationStatus: appliedFilters.registrationStatus
  })
}

function replaceSelectedRowSnapshot(device: Device) {
  selectedRows.value = replaceSelectedDeviceSnapshot(selectedRows.value, device)
}

function removeSelectedRowSnapshot(row?: Partial<Device> | null) {
  selectedRows.value = removeSelectedDeviceSnapshot(selectedRows.value, row)
}

function cacheVisibleDevicePage() {
  cacheDevicePage(buildCurrentDevicePageQuery(), {
    total: pagination.total,
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    records: tableData.value
  })
}

function rebuildVisibleDevicePageCache() {
  clearDevicePageCache()
  if (pagination.total <= 0 || tableData.value.length === 0) {
    return
  }
  cacheVisibleDevicePage()
}

function mergeLocalTableRow(device: Device) {
  const nextRows = mergeLocalDeviceRow(tableData.value, device)
  if (!nextRows) {
    return false
  }

  tableData.value = nextRows
  cacheVisibleDevicePage()
  replaceSelectedRowSnapshot(device)
  void syncTableSelection()
  return true
}

function prependLocalTableRow(device: Device) {
  tableData.value = prependLocalDeviceRow(tableData.value, device, pagination.pageSize)
  cacheVisibleDevicePage()
  replaceSelectedRowSnapshot(device)
  void syncTableSelection()
}

function removeLocalTableRow(row?: Partial<Device> | null) {
  const nextRows = removeLocalDeviceRow(tableData.value, row)
  if (!nextRows) {
    return false
  }

  tableData.value = nextRows
  cacheVisibleDevicePage()
  removeSelectedRowSnapshot(row)
  void syncTableSelection()
  return true
}

function removeLocalTableRows(rows: Array<Partial<Device> | null | undefined>) {
  const deletingKeys = new Set(rows.map((item) => getDeviceRowKey(item)).filter(Boolean))
  if (deletingKeys.size === 0) {
    return 0
  }

  const nextRows = tableData.value.filter((item) => !deletingKeys.has(getDeviceRowKey(item)))
  const removedCount = tableData.value.length - nextRows.length
  if (removedCount === 0) {
    return 0
  }

  tableData.value = nextRows
  selectedRows.value = selectedRows.value.filter((item) => !deletingKeys.has(getDeviceRowKey(item)))
  void syncTableSelection()
  return removedCount
}

function finalizeArchiveCreate(created: Device, sourceRow?: Device | null) {
  clearDeviceOptionCache()
  cacheDeviceDetail(created)
  clearSelection()

  let nextTotal = pagination.total
  const removedCount = sourceRow ? removeLocalTableRows([sourceRow]) : 0
  if (removedCount > 0) {
    nextTotal = Math.max(0, nextTotal - removedCount)
    removeCachedDeviceDetail(sourceRow)
  }

  const shouldInsertCreated = appliedFilters.registrationStatus !== 0 && matchesCurrentFilters(created)
  if (shouldInsertCreated) {
    nextTotal += 1
    if (pagination.pageNum === 1) {
      prependLocalTableRow(created)
    } else {
      rebuildVisibleDevicePageCache()
    }
  } else {
    rebuildVisibleDevicePageCache()
  }

  setTotal(nextTotal)
}

function handleSelectionChange(rows: Device[]) {
  selectedRows.value = rows.filter((row) => isSelectableDeviceRow(row))
}

function isRowSelected(row: Device) {
  return selectedRowKeySet.value.has(getDeviceRowKey(row))
}

function handleMobileSelectionChange(row: Device, checked: boolean) {
  if (!isSelectableDeviceRow(row)) {
    return
  }
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

function handleToolbarAction(command: string | number | object) {
  switch (command) {
    case 'batch-activate':
      void handleBatchActivate()
      break
    case 'batch-delete':
      void handleBatchDelete()
      break
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

function syncAppliedFilters() {
  appliedFilters.deviceId = searchForm.deviceId.trim()
  appliedFilters.keyword = searchForm.keyword.trim()
  appliedFilters.productKey = searchForm.productKey.trim()
  appliedFilters.productName = searchForm.productName.trim()
  appliedFilters.deviceCode = searchForm.deviceCode.trim()
  appliedFilters.deviceName = searchForm.deviceName.trim()
  appliedFilters.onlineStatus = searchForm.onlineStatus
  appliedFilters.activateStatus = searchForm.activateStatus
  appliedFilters.deviceStatus = searchForm.deviceStatus
  appliedFilters.registrationStatus = searchForm.registrationStatus
}

function clearSearchForm() {
  quickSearchKeyword.value = ''
  searchForm.deviceId = ''
  searchForm.keyword = ''
  searchForm.productKey = ''
  searchForm.productName = ''
  searchForm.deviceCode = ''
  searchForm.deviceName = ''
  searchForm.onlineStatus = undefined
  searchForm.activateStatus = undefined
  searchForm.deviceStatus = undefined
  searchForm.registrationStatus = undefined
  showAdvancedFilters.value = false
}

function clearListRefreshState() {
  listRefreshMessage.value = ''
  listRefreshState.value = ''
}

function clearFormRefreshState() {
  formRefreshing.value = false
  formRefreshMessage.value = ''
  formRefreshState.value = ''
}

function clearReplaceRefreshState() {
  replaceRefreshing.value = false
  replaceRefreshMessage.value = ''
  replaceRefreshState.value = ''
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
  searchForm.keyword = typeof route.query.keyword === 'string' ? route.query.keyword.trim() : ''
  searchForm.productKey = typeof route.query.productKey === 'string' ? route.query.productKey.trim() : ''
  searchForm.productName = typeof route.query.productName === 'string' ? route.query.productName.trim() : ''
  searchForm.deviceCode = typeof route.query.deviceCode === 'string' ? route.query.deviceCode.trim() : ''
  searchForm.deviceName = typeof route.query.deviceName === 'string' ? route.query.deviceName.trim() : ''
  syncQuickSearchKeywordFromFilters()
  searchForm.onlineStatus = parseRouteNumberQuery(route.query.onlineStatus)
  searchForm.activateStatus = parseRouteNumberQuery(route.query.activateStatus)
  searchForm.deviceStatus = parseRouteNumberQuery(route.query.deviceStatus)
  searchForm.registrationStatus = parseRouteNumberQuery(route.query.registrationStatus)
  showAdvancedFilters.value = countFilledFilters(searchForm, advancedFilterKeys) > 0
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
  key:
    | 'deviceId'
    | 'keyword'
    | 'productKey'
    | 'productName'
    | 'deviceCode'
    | 'deviceName'
    | 'onlineStatus'
    | 'activateStatus'
    | 'deviceStatus'
    | 'registrationStatus'
    | 'pageNum'
    | 'pageSize',
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
    normalizeQueryValue(route.query.keyword) === normalizeQueryValue(nextQuery.keyword) &&
    normalizeQueryValue(route.query.productKey) === normalizeQueryValue(nextQuery.productKey) &&
    normalizeQueryValue(route.query.productName) === normalizeQueryValue(nextQuery.productName) &&
    normalizeQueryValue(route.query.deviceCode) === normalizeQueryValue(nextQuery.deviceCode) &&
    normalizeQueryValue(route.query.deviceName) === normalizeQueryValue(nextQuery.deviceName) &&
    normalizeQueryValue(route.query.onlineStatus) === normalizeQueryValue(nextQuery.onlineStatus) &&
    normalizeQueryValue(route.query.activateStatus) === normalizeQueryValue(nextQuery.activateStatus) &&
    normalizeQueryValue(route.query.deviceStatus) === normalizeQueryValue(nextQuery.deviceStatus) &&
    normalizeQueryValue(route.query.registrationStatus) === normalizeQueryValue(nextQuery.registrationStatus) &&
    normalizeQueryValue(route.query.pageNum) === normalizeQueryValue(nextQuery.pageNum) &&
    normalizeQueryValue(route.query.pageSize) === normalizeQueryValue(nextQuery.pageSize)
  )
}

async function syncListRouteQuery(options: DevicePageLoadOptions = {}) {
  const nextQuery: Record<string, unknown> = { ...route.query }
  const trimmedDeviceId = searchForm.deviceId.trim()
  const trimmedKeyword = searchForm.keyword.trim()
  const trimmedProductKey = searchForm.productKey.trim()
  const trimmedProductName = searchForm.productName.trim()
  const trimmedDeviceCode = searchForm.deviceCode.trim()
  const trimmedDeviceName = searchForm.deviceName.trim()

  assignListQueryValue(nextQuery, 'deviceId', trimmedDeviceId || undefined)
  assignListQueryValue(nextQuery, 'keyword', trimmedKeyword || undefined)
  assignListQueryValue(nextQuery, 'productKey', trimmedProductKey || undefined)
  assignListQueryValue(nextQuery, 'productName', trimmedProductName || undefined)
  assignListQueryValue(nextQuery, 'deviceCode', trimmedDeviceCode || undefined)
  assignListQueryValue(nextQuery, 'deviceName', trimmedDeviceName || undefined)
  assignListQueryValue(nextQuery, 'onlineStatus', searchForm.onlineStatus)
  assignListQueryValue(nextQuery, 'activateStatus', searchForm.activateStatus)
  assignListQueryValue(nextQuery, 'deviceStatus', searchForm.deviceStatus)
  assignListQueryValue(nextQuery, 'registrationStatus', searchForm.registrationStatus)
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
  if (productOptions.value.length > 0) {
    return
  }
  if (productLoadPromise) {
    await productLoadPromise
    return
  }

  productLoading.value = true
  productLoadPromise = (async () => {
    try {
      const res = await productApi.getAllProducts()
      if (res.code === 200) {
        productOptions.value = res.data || []
        return
      }
      ElMessage.error(res.msg || '加载产品列表失败')
    } catch (error) {
      console.error('加载产品列表失败', error)
      if (!isHandledRequestError(error)) {
        ElMessage.error(resolveRequestErrorMessage(error, '加载产品列表失败'))
      }
    } finally {
      productLoading.value = false
      productLoadPromise = null
    }
  })()
  await productLoadPromise
}

async function loadDeviceOptions() {
  if (deviceOptions.value.length > 0) {
    return
  }
  if (deviceOptionLoadPromise) {
    await deviceOptionLoadPromise
    return
  }

  deviceOptionsLoading.value = true
  deviceOptionLoadPromise = (async () => {
    try {
      const res = await deviceApi.listDeviceOptions({ includeDisabled: true })
      if (res.code === 200) {
        deviceOptions.value = res.data || []
        return
      }
      ElMessage.error(res.msg || '加载父设备选项失败')
    } catch (error) {
      console.error('加载父设备选项失败', error)
      if (!isHandledRequestError(error)) {
        ElMessage.error(resolveRequestErrorMessage(error, '加载父设备选项失败'))
      }
    } finally {
      deviceOptionsLoading.value = false
      deviceOptionLoadPromise = null
    }
  })()
  await deviceOptionLoadPromise
}

function clearDeviceOptionCache() {
  deviceOptions.value = []
  deviceOptionLoadPromise = null
}

function buildCurrentDevicePageQuery(): DevicePageQuerySnapshot {
  return {
    deviceId: searchForm.deviceId.trim(),
    keyword: searchForm.keyword.trim(),
    productKey: searchForm.productKey.trim(),
    productName: searchForm.productName.trim(),
    deviceCode: searchForm.deviceCode.trim(),
    deviceName: searchForm.deviceName.trim(),
    onlineStatus: searchForm.onlineStatus,
    activateStatus: searchForm.activateStatus,
    deviceStatus: searchForm.deviceStatus,
    registrationStatus: searchForm.registrationStatus,
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  }
}

function buildDetailPreview(row: Device) {
  return {
    ...row,
    metadataJson: row.metadataJson ?? null
  }
}

function mergeUnregisteredDetailSnapshot(base: Partial<Device>, source: DeviceAccessErrorLog): Device {
  return {
    ...base,
    sourceRecordId: source.id,
    deviceCode: source.deviceCode || base.deviceCode || '',
    productKey: source.productKey || base.productKey || null,
    protocolCode: source.protocolCode || base.protocolCode || null,
    assetSourceType: 'access_error',
    registrationStatus: 0,
    lastFailureStage: source.failureStage || base.lastFailureStage || null,
    lastErrorMessage: source.errorMessage || base.lastErrorMessage || null,
    lastReportTopic: source.topic || base.lastReportTopic || null,
    lastTraceId: source.traceId || base.lastTraceId || null,
    lastPayload: source.rawPayload || base.lastPayload || null,
    lastReportTime: source.createTime || base.lastReportTime || null,
    createTime: source.createTime || base.createTime || null,
    updateTime: source.createTime || base.updateTime || null,
    deviceName: base.deviceName || '未登记设备'
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

function hydrateDeviceDetailCache() {
  const storage = getDevicePageSessionStorage()
  if (!storage) {
    return
  }

  const entries = deserializeDeviceDetailCacheEntries(
    storage.getItem(deviceDetailCacheSessionStorageKey),
    deviceDetailCacheTtlMs,
    deviceDetailCacheLimit
  )

  deviceDetailCache.clear()
  entries.forEach((entry) => {
    deviceDetailCache.set(entry.key, entry)
  })

  if (entries.length === 0) {
    try {
      storage.removeItem(deviceDetailCacheSessionStorageKey)
    } catch {
      // 忽略浏览器存储异常，避免阻断详情主流程
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

function persistDeviceDetailCache() {
  const storage = getDevicePageSessionStorage()
  if (!storage) {
    return
  }

  try {
    if (deviceDetailCache.size === 0) {
      storage.removeItem(deviceDetailCacheSessionStorageKey)
      return
    }

    storage.setItem(
      deviceDetailCacheSessionStorageKey,
      serializeDeviceDetailCacheEntries(deviceDetailCache.values(), deviceDetailCacheLimit)
    )
  } catch {
    // 忽略浏览器存储异常，避免阻断详情主流程
  }
}

function getCachedDevicePage(query: DevicePageQuerySnapshot) {
  const cacheKey = buildDevicePageCacheKey(query)
  return cloneDevicePageCacheEntry(devicePageCache.get(cacheKey))
}

function getDeviceDetailCacheKey(row?: Partial<Device> | null) {
  return getDeviceRowKey(row)
}

function getCachedDeviceDetail(row?: Partial<Device> | null) {
  const cacheKey = getDeviceDetailCacheKey(row)
  if (!cacheKey) {
    return null
  }
  const entry = cloneDeviceDetailCacheEntry(deviceDetailCache.get(cacheKey))
  if (!entry) {
    return null
  }
  if (!isDeviceDetailCacheFresh(entry, deviceDetailCacheTtlMs)) {
    deviceDetailCache.delete(cacheKey)
    persistDeviceDetailCache()
    return null
  }
  return { ...entry.detail }
}

function cacheDeviceDetail(device?: Device | null) {
  const cacheKey = getDeviceDetailCacheKey(device)
  if (!cacheKey || !device) {
    return
  }
  const entry = createDeviceDetailCacheEntry(device)
  deviceDetailCache.delete(cacheKey)
  deviceDetailCache.set(cacheKey, entry)

  while (deviceDetailCache.size > deviceDetailCacheLimit) {
    const oldestKey = deviceDetailCache.keys().next().value
    if (!oldestKey) {
      break
    }
    deviceDetailCache.delete(oldestKey)
  }

  persistDeviceDetailCache()
}

function removeCachedDeviceDetail(row?: Partial<Device> | null) {
  const cacheKey = getDeviceDetailCacheKey(row)
  if (!cacheKey) {
    return
  }
  deviceDetailCache.delete(cacheKey)
  persistDeviceDetailCache()
}

function resolveDetailSnapshot(row: Device, cachedDetail: Device | null) {
  if (cachedDetail) {
    return {
      ...cachedDetail,
      ...row,
      metadataJson: cachedDetail.metadataJson ?? row.metadataJson ?? null
    }
  }
  return buildDetailPreview(row)
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

function abortEditRequest() {
  editAbortController?.abort()
  editAbortController = null
}

function abortReplaceRequest() {
  replaceAbortController?.abort()
  replaceAbortController = null
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
        keyword: nextQuery.keyword || undefined,
        productKey: nextQuery.productKey || undefined,
        productName: nextQuery.productName || undefined,
        deviceCode: nextQuery.deviceCode || undefined,
        deviceName: nextQuery.deviceName || undefined,
        onlineStatus: nextQuery.onlineStatus,
        activateStatus: nextQuery.activateStatus,
        deviceStatus: nextQuery.deviceStatus,
        registrationStatus: nextQuery.registrationStatus,
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
        keyword: query.keyword || undefined,
        productKey: query.productKey || undefined,
        productName: query.productName || undefined,
        deviceCode: query.deviceCode || undefined,
        deviceName: query.deviceName || undefined,
        onlineStatus: query.onlineStatus,
        activateStatus: query.activateStatus,
        deviceStatus: query.deviceStatus,
        registrationStatus: query.registrationStatus,
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
      if (!isHandledRequestError(error)) {
        ElMessage.error(resolveRequestErrorMessage(error, '获取设备分页失败'))
      }
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

async function openDetail(target: Device | string | number) {
  const requestId = ++latestDetailRequestId
  const row = typeof target === 'object' ? target : null
  const detailId = row ? row.id : target
  const cachedDetail = row ? getCachedDeviceDetail(row) : null
  const detailSnapshot = row ? resolveDetailSnapshot(row, cachedDetail) : null

  abortDetailRequest()
  capabilityVisible.value = false
  detailVisible.value = true
  detailErrorMessage.value = ''
  detailRefreshErrorMessage.value = ''
  detailData.value = detailSnapshot
  detailLoading.value = !detailSnapshot

  if (row && !isRegisteredDeviceRow(row)) {
    detailLoading.value = false
    detailRefreshing.value = false
    if (!row.sourceRecordId) {
      return
    }

    const controller = new AbortController()
    detailAbortController = controller
    detailRefreshing.value = true

    try {
      const res = await accessErrorApi.getAccessErrorById(row.sourceRecordId)
      if (requestId !== latestDetailRequestId) {
        return
      }
      if (res.code === 200 && res.data) {
        detailData.value = mergeUnregisteredDetailSnapshot(detailSnapshot ?? row, res.data)
        detailErrorMessage.value = ''
        return
      }
      detailRefreshErrorMessage.value = res.msg || '未登记设备补充详情失败，当前先展示列表摘要。'
    } catch (error) {
      if (requestId !== latestDetailRequestId || isAbortError(error)) {
        return
      }
      detailRefreshErrorMessage.value =
        error instanceof Error ? error.message : '未登记设备补充详情失败，当前先展示列表摘要。'
    } finally {
      if (requestId === latestDetailRequestId) {
        detailRefreshing.value = false
      }
      if (detailAbortController === controller) {
        detailAbortController = null
      }
    }
    return
  }

  if (row && detailSnapshot && !shouldRefreshDeviceDetail(row, cachedDetail)) {
    detailRefreshing.value = false
    return
  }

  const controller = new AbortController()
  detailAbortController = controller
  detailRefreshing.value = Boolean(detailSnapshot)

  try {
    const res = await deviceApi.getDeviceById(detailId, {
      signal: controller.signal
    })
    if (requestId !== latestDetailRequestId) {
      return
    }
    if (res.code === 200 && res.data) {
      detailData.value = res.data
      cacheDeviceDetail(res.data)
      detailErrorMessage.value = ''
      return
    }
    if (!detailSnapshot) {
      detailErrorMessage.value = res.msg || '加载设备详情失败'
      return
    }
    detailRefreshErrorMessage.value = res.msg || '完整详情补充失败，当前先展示列表摘要。'
  } catch (error) {
    if (requestId !== latestDetailRequestId || isAbortError(error)) {
      return
    }
    if (detailSnapshot) {
      detailRefreshErrorMessage.value = error instanceof Error ? error.message : '完整详情补充失败，当前先展示列表摘要。'
      return
    }
    detailErrorMessage.value = error instanceof Error ? error.message : '加载设备详情失败'
  } finally {
    if (requestId === latestDetailRequestId) {
      detailLoading.value = false
      detailRefreshing.value = false
    }
    if (detailAbortController === controller) {
      detailAbortController = null
    }
  }
}

function resetCapabilityState() {
  capabilityOverview.value = null
  commandRecords.value = []
  capabilityLoading.value = false
  commandLoading.value = false
  capabilityExecuteVisible.value = false
  capabilityExecuteSubmitting.value = false
  executingCapability.value = null
  capabilityDevice.value = null
}

function openCapability(row: Device) {
  if (!isRegisteredDeviceRow(row) || !row.deviceCode) {
    return
  }
  const cachedDetail = getCachedDeviceDetail(row)
  detailVisible.value = false
  resetCapabilityState()
  capabilityDevice.value = resolveDetailSnapshot(row, cachedDetail)
  capabilityVisible.value = true
  const requestId = ++latestCapabilityRequestSeed
  latestCapabilityOverviewRequestId = requestId
  latestCapabilityCommandRequestId = requestId
  void refreshDeviceCapabilityContext(row.deviceCode, requestId)
}

async function refreshDeviceCapabilityContext(deviceCode: string, requestId: number) {
  if (!deviceCode || requestId !== latestCapabilityOverviewRequestId || requestId !== latestCapabilityCommandRequestId) {
    return
  }
  await Promise.all([
    loadDeviceCapabilityOverview(deviceCode, requestId),
    loadDeviceCapabilityRecords(deviceCode, requestId)
  ])
}

async function loadDeviceCapabilityOverview(deviceCode: string, requestId: number) {
  if (requestId !== latestCapabilityOverviewRequestId) {
    return
  }
  capabilityLoading.value = true
  try {
    const res = await deviceApi.getDeviceCapabilities(deviceCode)
    if (requestId !== latestCapabilityOverviewRequestId) {
      return
    }
    if (res.code === 200 && res.data) {
      capabilityOverview.value = res.data
      return
    }
    capabilityOverview.value = null
    if (res.msg) {
      ElMessage.error(res.msg)
    }
  } catch (error) {
    if (requestId !== latestCapabilityOverviewRequestId) {
      return
    }
    capabilityOverview.value = null
    if (!isHandledRequestError(error)) {
      ElMessage.error(resolveRequestErrorMessage(error, '获取设备能力失败'))
    }
  } finally {
    if (requestId === latestCapabilityOverviewRequestId) {
      capabilityLoading.value = false
    }
  }
}

async function loadDeviceCapabilityRecords(deviceCode: string, requestId: number) {
  if (requestId !== latestCapabilityCommandRequestId) {
    return
  }
  commandLoading.value = true
  try {
    const res = await deviceApi.pageDeviceCommands(deviceCode, {
      pageNum: 1,
      pageSize: 10
    })
    if (requestId !== latestCapabilityCommandRequestId) {
      return
    }
    if (res.code === 200 && res.data) {
      commandRecords.value = res.data.records || []
      return
    }
    commandRecords.value = []
    if (res.msg) {
      ElMessage.error(res.msg)
    }
  } catch (error) {
    if (requestId !== latestCapabilityCommandRequestId) {
      return
    }
    commandRecords.value = []
    if (!isHandledRequestError(error)) {
      ElMessage.error(resolveRequestErrorMessage(error, '获取设备命令失败'))
    }
  } finally {
    if (requestId === latestCapabilityCommandRequestId) {
      commandLoading.value = false
    }
  }
}

async function refreshEditableDetail(row: Device, editSessionId: number, cachedDetail: Device | null) {
  if (row.id === undefined || row.id === null || row.id === '') {
    clearFormRefreshState()
    return
  }
  if (!shouldRefreshDeviceDetail(row, cachedDetail)) {
    clearFormRefreshState()
    return
  }

  const deviceId = row.id
  const requestId = ++latestEditRequestId
  abortEditRequest()
  const controller = new AbortController()
  editAbortController = controller
  formRefreshing.value = true
  formRefreshState.value = 'info'
  formRefreshMessage.value = ''

  try {
    const res = await deviceApi.getDeviceById(deviceId, {
      signal: controller.signal
    })
    if (
      requestId !== latestEditRequestId ||
      editSessionId !== activeEditSessionId ||
      editingDeviceId.value !== deviceId
    ) {
      return
    }
    if (res.code === 200 && res.data) {
      cacheDeviceDetail(res.data)
      if (!formDirtySinceOpen) {
        applyFormDataWithoutDirty(res.data)
        formRef.value?.clearValidate()
        clearFormRefreshState()
      } else {
        formRefreshState.value = 'warning'
        formRefreshMessage.value = '最新设备档案已取回；你已开始编辑，当前未自动覆盖表单。'
      }
      return
    }
    formRefreshState.value = 'error'
    formRefreshMessage.value = res.msg || '最新设备档案补充失败，当前先保留已填入内容。'
  } catch (error) {
    if (
      requestId !== latestEditRequestId ||
      editSessionId !== activeEditSessionId ||
      editingDeviceId.value !== deviceId ||
      isAbortError(error)
    ) {
      return
    }
    formRefreshState.value = 'error'
    formRefreshMessage.value =
      error instanceof Error ? `最新设备档案补充失败：${error.message}` : '最新设备档案补充失败，当前先保留已填入内容。'
  } finally {
    if (requestId === latestEditRequestId) {
      formRefreshing.value = false
    }
    if (editAbortController === controller) {
      editAbortController = null
    }
  }
}

async function refreshReplacingDevice(row: Device, replaceSessionId: number, cachedDetail: Device | null) {
  if (row.id === undefined || row.id === null || row.id === '') {
    clearReplaceRefreshState()
    return
  }
  if (!shouldRefreshDeviceDetail(row, cachedDetail)) {
    clearReplaceRefreshState()
    return
  }

  const deviceId = row.id
  const requestId = ++latestReplaceRequestId
  abortReplaceRequest()
  const controller = new AbortController()
  replaceAbortController = controller
  replaceRefreshing.value = true
  replaceRefreshState.value = 'info'
  replaceRefreshMessage.value = ''

  try {
    const res = await deviceApi.getDeviceById(deviceId, {
      signal: controller.signal
    })
    if (
      requestId !== latestReplaceRequestId ||
      replaceSessionId !== activeReplaceSessionId ||
      normalizeIdKey(replacingDevice.value?.id) !== normalizeIdKey(deviceId)
    ) {
      return
    }
    if (res.code === 200 && res.data) {
      cacheDeviceDetail(res.data)
      if (!replaceFormDirtySinceOpen.value) {
        replacingDevice.value = res.data
        clearReplaceRefreshState()
      } else {
        replaceRefreshState.value = 'warning'
        replaceRefreshMessage.value = '最新设备档案已取回；你已开始填写替换表单，当前未自动覆盖已输入内容。'
      }
      return
    }
    replaceRefreshState.value = 'error'
    replaceRefreshMessage.value = res.msg || '最新设备档案补全失败，当前先保留已填入内容。'
  } catch (error) {
    if (
      requestId !== latestReplaceRequestId ||
      replaceSessionId !== activeReplaceSessionId ||
      normalizeIdKey(replacingDevice.value?.id) !== normalizeIdKey(deviceId) ||
      isAbortError(error)
    ) {
      return
    }
    replaceRefreshState.value = 'error'
    replaceRefreshMessage.value =
      error instanceof Error ? `最新设备档案补全失败：${error.message}` : '最新设备档案补全失败，当前先保留已填入内容。'
  } finally {
    if (requestId === latestReplaceRequestId) {
      replaceRefreshing.value = false
    }
    if (replaceAbortController === controller) {
      replaceAbortController = null
    }
  }
}

function handleSearch() {
  if (quickSearchKeyword.value.trim() || searchForm.keyword.trim()) {
    applyQuickSearchKeywordToFilters()
  }
  searchForm.deviceId = searchForm.deviceId.trim()
  searchForm.keyword = searchForm.keyword.trim()
  searchForm.productKey = searchForm.productKey.trim()
  searchForm.productName = searchForm.productName.trim()
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
  } else if (key === 'keyword') {
    searchForm.keyword = ''
    quickSearchKeyword.value = ''
  } else if (key === 'productKey') {
    searchForm.productKey = ''
  } else if (key === 'productName') {
    searchForm.productName = ''
  } else if (key === 'deviceCode') {
    searchForm.deviceCode = ''
  } else if (key === 'deviceName') {
    searchForm.deviceName = ''
  } else if (key === 'onlineStatus') {
    searchForm.onlineStatus = undefined
  } else if (key === 'activateStatus') {
    searchForm.activateStatus = undefined
  } else if (key === 'registrationStatus') {
    searchForm.registrationStatus = undefined
  } else {
    searchForm.deviceStatus = undefined
  }
  syncQuickSearchKeywordFromFilters()
  resetPage()
  clearSelection()
  void syncListRouteQuery()
}

function handleQuickSearch() {
  const keyword = applyQuickSearchKeywordToFilters()
  if (!keyword) {
    return
  }
  resetPage()
  clearSelection()
  void syncListRouteQuery()
}

function handleClearQuickSearch() {
  quickSearchKeyword.value = ''
  searchForm.keyword = ''
  searchForm.productKey = ''
  searchForm.productName = ''
  searchForm.deviceCode = ''
  searchForm.deviceName = ''
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

function handleAdd() {
  activeEditSessionId += 1
  abortEditRequest()
  formMode.value = 'create'
  registerSourceRow.value = null
  editingDeviceId.value = null
  formDirtySinceOpen = false
  clearFormRefreshState()
  applyFormDataWithoutDirty()
  formVisible.value = true
  formRef.value?.clearValidate()
  void loadProducts()
  void loadDeviceOptions()
}

function handleEdit(row: Device) {
  if (!canEditDeviceRow(row) || !hasEditPermissionForRow(row)) {
    return
  }
  const cachedDetail = getCachedDeviceDetail(row)
  const editSnapshot = resolveDetailSnapshot(row, cachedDetail)

  activeEditSessionId += 1
  const editSessionId = activeEditSessionId
  abortEditRequest()
  editingDeviceId.value = row.id
  formDirtySinceOpen = false
  clearFormRefreshState()
  applyFormDataWithoutDirty(editSnapshot)
  formVisible.value = true
  formRef.value?.clearValidate()
  void loadProducts()
  void loadDeviceOptions()

  if (isRegisteredDeviceRow(row) && row.id !== undefined && row.id !== null && row.id !== '') {
    formMode.value = 'edit'
    registerSourceRow.value = null
    editingDeviceId.value = row.id
    void refreshEditableDetail(row, editSessionId, cachedDetail)
    return
  }

  formMode.value = 'register'
  registerSourceRow.value = { ...row }
  editingDeviceId.value = null
}

function handleOpenDetail(row: Device) {
  void openDetail(row)
}

function handleReplaceDirtyChange(value: boolean) {
  replaceFormDirtySinceOpen.value = value
}

function buildReplacementSourceSnapshot(source: Device) {
  return {
    ...source,
    onlineStatus: 0,
    deviceStatus: 0
  }
}

function buildReplacementTargetSnapshot(source: Device, payload: DeviceReplacePayload, result: DeviceReplaceResult): Device {
  const targetProductKey = payload.productKey || source.productKey
  const matchedProduct = productOptions.value.find((item) => item.productKey === targetProductKey)
  const parentOption = deviceOptionMap.value.get(normalizeIdKey(payload.parentDeviceId))
  const gatewayOption =
    parentOption?.nodeType === 2
      ? parentOption
      : deviceOptionMap.value.get(normalizeIdKey(parentOption?.gatewayId))

  return {
    ...source,
    id: result.targetDeviceId,
    productId: matchedProduct?.id ?? source.productId,
    gatewayId: gatewayOption?.id ?? null,
    parentDeviceId: payload.parentDeviceId ?? null,
    productKey: targetProductKey,
    productName: matchedProduct?.productName || source.productName,
    gatewayDeviceCode: gatewayOption?.deviceCode ?? null,
    gatewayDeviceName: gatewayOption?.deviceName ?? null,
    parentDeviceCode: parentOption?.deviceCode ?? null,
    parentDeviceName: parentOption?.deviceName ?? null,
    deviceName: result.targetDeviceName || payload.deviceName,
    deviceCode: result.targetDeviceCode || payload.deviceCode,
    deviceSecret: payload.deviceSecret || '',
    clientId: payload.clientId || '',
    username: payload.username || '',
    password: payload.password || '',
    onlineStatus: 0,
    activateStatus: payload.activateStatus ?? source.activateStatus ?? 1,
    deviceStatus: payload.deviceStatus ?? 1,
    registrationStatus: 1,
    assetSourceType: 'registry',
    firmwareVersion: payload.firmwareVersion || '',
    ipAddress: payload.ipAddress || '',
    address: payload.address || '',
    metadataJson: payload.metadataJson || '',
    lastOnlineTime: '',
    lastOfflineTime: '',
    lastReportTime: '',
    createTime: '',
    updateTime: ''
  }
}

async function handleOpenReplaceLegacy(row: Device) {
  if (!canReplaceDeviceRow(row) || row.id === undefined || row.id === null || row.id === '') {
    ElMessage.warning('未登记设备暂不支持更换，请先完成建档。')
    return
  }
  const deviceId = row.id
  try {
    if (productOptions.value.length === 0) {
      await loadProducts()
    }
    void loadDeviceOptions()
    const res = await deviceApi.getDeviceById(deviceId)
    if (res.code === 200 && res.data) {
      cacheDeviceDetail(res.data)
      replacingDevice.value = res.data
      replaceVisible.value = true
      return
    }
    ElMessage.error(res.msg || '加载待更换设备失败')
  } catch (error) {
    console.error('加载待更换设备失败', error)
    if (!isHandledRequestError(error)) {
      ElMessage.error(resolveRequestErrorMessage(error, '加载待更换设备失败'))
    }
  }
}

async function handleOpenReplace(row: Device) {
  if (!canReplaceDeviceRow(row)) {
    ElMessage.warning('未登记设备暂不支持更换，请先完成建档。')
    return
  }
  const cachedDetail = getCachedDeviceDetail(row)
  const replaceSnapshot = resolveDetailSnapshot(row, cachedDetail)

  activeReplaceSessionId += 1
  const replaceSessionId = activeReplaceSessionId
  abortReplaceRequest()
  replaceFormDirtySinceOpen.value = false
  clearReplaceRefreshState()
  replacingDevice.value = replaceSnapshot
  replaceVisible.value = true
  void loadProducts()
  void loadDeviceOptions()
  void refreshReplacingDevice(row, replaceSessionId, cachedDetail)
}

function handleJumpToInsight(row?: Device | null) {
  if (!canJumpToInsight(row)) {
    return
  }
  void router.push({
    path: '/insight',
    query: {
      deviceCode: row.deviceCode
    }
  })
}

async function handleOpenOnboardingSuggestion(row: Device) {
  const traceId = row.lastTraceId?.trim()
  if (!traceId) {
    ElMessage.warning('当前线索缺少 Trace，暂时无法生成接入建议。')
    return
  }
  onboardingSuggestionSource.value = row
  onboardingSuggestion.value = null
  suggestionErrorMessage.value = ''
  suggestionVisible.value = true
  suggestionLoading.value = true
  try {
    const res = await deviceApi.getDeviceOnboardingSuggestion(traceId)
    if (res.code === 200 && res.data) {
      onboardingSuggestion.value = res.data
      return
    }
    suggestionErrorMessage.value = res.msg || '加载接入建议失败'
  } catch (error) {
    console.error('加载接入建议失败', error)
    suggestionErrorMessage.value = resolveRequestErrorMessage(error, '加载接入建议失败')
  } finally {
    suggestionLoading.value = false
  }
}

function handleRowAction(command: string | number | object, row: Device) {
  if (command === 'detail') {
    handleOpenDetail(row)
    return
  }
  if (command === 'edit') {
    handleEdit(row)
    return
  }
  if (command === 'replace') {
    void handleOpenReplace(row)
    return
  }
  if (command === 'insight') {
    handleJumpToInsight(row)
    return
  }
  if (command === 'suggestion') {
    void handleOpenOnboardingSuggestion(row)
    return
  }
  if (command === 'capability') {
    openCapability(row)
    return
  }
  if (command === 'delete') {
    void handleDelete(row)
  }
}

function handleExecuteCapability(capability: DeviceCapability) {
  if (!capabilityDevice.value || !capability) {
    return
  }
  executingCapability.value = capability
  capabilityExecuteVisible.value = true
}

function handleRefreshCommands() {
  if (!capabilityDevice.value?.deviceCode) {
    return
  }
  const requestId = ++latestCapabilityCommandRequestId
  void loadDeviceCapabilityRecords(capabilityDevice.value.deviceCode, requestId)
}

async function handleExecuteCapabilitySubmit(payload: DeviceCapabilityExecutePayload) {
  if (!capabilityDevice.value?.deviceCode || !executingCapability.value) {
    return
  }

  const deviceCode = capabilityDevice.value.deviceCode
  const capability = executingCapability.value
  capabilityExecuteSubmitting.value = true
  try {
    const res = await deviceApi.executeDeviceCapability(deviceCode, capability.code, payload)
    if (res.code !== 200 || !res.data) {
      ElMessage.error(res.msg || '设备能力下发失败')
      return
    }
    ElMessage.success(`指令已下发，等待设备反馈：${res.data.commandId}`)
    capabilityExecuteVisible.value = false
    executingCapability.value = null
    const requestId = ++latestCapabilityCommandRequestId
    await loadDeviceCapabilityRecords(deviceCode, requestId)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      ElMessage.error(resolveRequestErrorMessage(error, '设备能力下发失败'))
    }
  } finally {
    capabilityExecuteSubmitting.value = false
  }
}

async function handleDelete(row: Device) {
  if (!canDeleteDeviceRow(row)) {
    ElMessage.warning('未登记设备暂不支持删除，请先完成建档。')
    return
  }
  const deviceId = row.id as string | number
  try {
    await confirmDelete('设备', row.deviceName || row.deviceCode)
    await deviceApi.deleteDevice(deviceId)
    ElMessage.success('删除成功')
    clearDeviceOptionCache()
    removeCachedDeviceDetail(row)
    const shouldGoPrevPage = tableData.value.length === 1 && pagination.pageNum > 1
    const removedCount = removeLocalTableRows([row])
    if (removedCount > 0) {
      setTotal(pagination.total - removedCount)
      rebuildVisibleDevicePageCache()
    }
    if (shouldGoPrevPage) {
      setPageNum(pagination.pageNum - 1)
      await syncListRouteQuery({
        silent: true,
        force: true,
        silentMessage: '已删除设备，正在后台刷新列表。'
      })
      return
    }
    void loadDevicePage({
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
  const deletingRows = [...selectedRows.value]
  try {
    await confirmAction({
      title: '批量删除设备',
      message: `确认删除选中的 ${deletingCount} 台设备吗？删除后不可恢复。`,
      type: 'warning',
      confirmButtonText: '确认删除'
    })
    await deviceApi.batchDeleteDevices(
      selectedRows.value
        .map((item) => item.id)
        .filter((id): id is string | number => id !== undefined && id !== null && id !== '')
    )
    ElMessage.success('批量删除成功')
    clearDeviceOptionCache()
    deletingRows.forEach((item) => removeCachedDeviceDetail(item))
    const shouldGoPrevPage = deletingCount === tableData.value.length && pagination.pageNum > 1
    const removedCount = removeLocalTableRows(deletingRows)
    if (removedCount > 0) {
      setTotal(pagination.total - removedCount)
      rebuildVisibleDevicePageCache()
    }
    if (shouldGoPrevPage) {
      setPageNum(pagination.pageNum - 1)
      await syncListRouteQuery({
        silent: true,
        force: true,
        silentMessage: '已删除选中设备，正在后台刷新列表。'
      })
      return
    }
    void loadDevicePage({
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

async function handleBatchActivate() {
  if (selectedRows.value.length === 0) {
    return
  }
  if (selectedBatchActivatableRows.value.length !== selectedRows.value.length) {
    ElMessage.warning('批量转正只支持已选未登记且带 Trace 的线索，请调整后重试。')
    return
  }

  const traceIds = selectedBatchActivatableRows.value
    .map((row) => row.lastTraceId?.trim())
    .filter((traceId): traceId is string => Boolean(traceId))
  if (traceIds.length === 0) {
    ElMessage.warning('当前选中记录缺少 Trace，暂时无法批量转正。')
    return
  }

  try {
    await confirmAction({
      title: '批量转正式设备',
      message: `确认按当前接入建议将选中的 ${traceIds.length} 条线索转为正式设备吗？系统会拦截仍有规则缺口的记录。`,
      type: 'warning',
      confirmButtonText: '确认转正'
    })
    const res = await deviceApi.batchActivateOnboardingSuggestions({
      traceIds,
      confirmed: true
    })
    if (res.code !== 200 || !res.data) {
      ElMessage.error(res.msg || '批量转正式设备失败')
      return
    }

    await finishBatchActivation(res.data)
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    ElMessage.error(resolveRequestErrorMessage(error, '批量转正式设备失败'))
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
      clearDeviceOptionCache()
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
    if (!isHandledRequestError(error)) {
      ElMessage.error(resolveRequestErrorMessage(error, '批量导入设备失败'))
    }
  } finally {
    batchImportSubmitting.value = false
  }
}

async function finishBatchActivation(result: DeviceOnboardingBatchResult) {
  if (result.activatedCount > 0) {
    clearDeviceOptionCache()
    clearDevicePageCache()
    clearSelection()
    await loadDevicePage({
      silent: true,
      force: true,
      silentMessage: '已按确认建议转正式设备，正在后台刷新列表。'
    })
  }
  if (result.rejectedCount === 0) {
    ElMessage.success(`批量转正完成，共转正 ${result.activatedCount} 台设备`)
    return
  }
  ElMessage.warning(`批量转正完成，成功 ${result.activatedCount} 条，拦截 ${result.rejectedCount} 条`)
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
    clearDeviceOptionCache()
    const sourceSnapshot = buildReplacementSourceSnapshot(replacingDevice.value)
    const targetSnapshot = buildReplacementTargetSnapshot(replacingDevice.value, payload, res.data)
    const sourceStillMatches = matchesCurrentFilters(sourceSnapshot)
    const targetMatches = matchesCurrentFilters(targetSnapshot)
    const totalDelta = (sourceStillMatches ? 0 : -1) + (targetMatches ? 1 : 0)

    setTotal(pagination.total + totalDelta)
    if (sourceStillMatches) {
      mergeLocalTableRow(sourceSnapshot)
      cacheDeviceDetail(sourceSnapshot)
    } else {
      removeLocalTableRow(sourceSnapshot)
      removeCachedDeviceDetail(replacingDevice.value)
    }
    if (targetMatches && pagination.pageNum === 1) {
      prependLocalTableRow(targetSnapshot)
    }
    cacheDeviceDetail(targetSnapshot)
    replaceVisible.value = false
    replacingDevice.value = null
    rebuildVisibleDevicePageCache()
    void loadDevicePage({
      silent: true,
      force: true,
      silentMessage: '已提交设备更换，正在后台刷新列表。'
    })
    await openDetail(targetSnapshot)
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('设备更换失败', error)
    if (!isHandledRequestError(error)) {
      ElMessage.error(resolveRequestErrorMessage(error, '设备更换失败'))
    }
  } finally {
    replaceSubmitting.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  const submitMode = formMode.value
  submitLoading.value = true
  try {
    if (submitMode === 'edit') {
      const res = await deviceApi.updateDevice(editingDeviceId.value as string | number, { ...formData })
      clearDeviceOptionCache()
      cacheDeviceDetail(res.data)
      if (matchesCurrentFilters(res.data)) {
        mergeLocalTableRow(res.data)
      } else {
        if (removeLocalTableRow(res.data)) {
          setTotal(pagination.total - 1)
        }
      }
      rebuildVisibleDevicePageCache()
      ElMessage.success('更新成功')
    } else {
      const res = await deviceApi.addDevice({ ...formData })
      if (submitMode === 'register') {
        finalizeArchiveCreate(res.data, registerSourceRow.value)
        ElMessage.success('登记成功')
      } else {
        clearDeviceOptionCache()
        cacheDeviceDetail(res.data)
        clearSelection()
        if (matchesCurrentFilters(res.data)) {
          setTotal(pagination.total + 1)
          if (pagination.pageNum === 1) {
            prependLocalTableRow(res.data)
          } else {
            rebuildVisibleDevicePageCache()
          }
        } else {
          rebuildVisibleDevicePageCache()
        }
        ElMessage.success('新增成功')
      }
    }
    formVisible.value = false
    void loadDevicePage({
      silent: true,
      force: true,
      silentMessage:
        submitMode === 'edit'
          ? '已提交设备更新，正在后台刷新列表。'
          : submitMode === 'register'
            ? '已完成设备登记，正在后台刷新列表。'
            : '已新增设备，正在后台刷新列表。'
    })
  } catch (error) {
    console.error('提交设备失败', error)
    ElMessage.error(
      error instanceof Error
        ? submitMode === 'register'
          ? `登记失败：${error.message}`
          : error.message
        : submitMode === 'register'
          ? '登记失败'
          : '提交设备失败'
    )
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
  formMode.value = 'create'
  registerSourceRow.value = null
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
      route.query.keyword,
      route.query.productKey,
      route.query.productName,
      route.query.deviceCode,
      route.query.deviceName,
      route.query.onlineStatus,
      route.query.activateStatus,
      route.query.deviceStatus,
      route.query.registrationStatus,
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
  latestDetailRequestId += 1
  abortDetailRequest()
  detailLoading.value = false
  detailRefreshing.value = false
  detailErrorMessage.value = ''
  detailRefreshErrorMessage.value = ''
  detailData.value = null
})

watch(capabilityVisible, (visible) => {
  if (visible) {
    return
  }
  const requestId = ++latestCapabilityRequestSeed
  latestCapabilityOverviewRequestId = requestId
  latestCapabilityCommandRequestId = requestId
  resetCapabilityState()
})

watch(
  formData,
  () => {
    if (!formVisible.value || formMode.value === 'create' || suppressFormDirtyTracking) {
      return
    }
    formDirtySinceOpen = true
  },
  { deep: true, flush: 'sync' }
)

watch(batchImportVisible, (value) => {
  if (!value) {
    batchImportResult.value = null
  }
})

watch(replaceVisible, (value) => {
  if (!value) {
    activeReplaceSessionId += 1
    latestReplaceRequestId += 1
    abortReplaceRequest()
    clearReplaceRefreshState()
    replaceFormDirtySinceOpen.value = false
    replacingDevice.value = null
  }
})

onBeforeUnmount(() => {
  abortListRequest()
  abortListPrefetchRequest()
  abortDetailRequest()
  abortEditRequest()
  abortReplaceRequest()
})

onMounted(async () => {
  hydrateDeviceDetailCache()
  hydrateDevicePageCache()
  applyRouteQueryToFilters()
  await loadDevicePage()
})
</script>

<style scoped>
.device-asset-view {
  display: grid;
  gap: 0.72rem;
  min-width: 0;
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
  box-shadow: var(--shadow-inset-highlight-78);
}

.device-loading-mobile-card {
  display: grid;
  gap: 0.8rem;
  padding: 0.92rem 0.96rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  box-shadow: var(--shadow-inset-highlight-76);
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
  border-radius: var(--radius-pill);
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
  border-radius: var(--radius-md);
}

.device-mobile-list {
  display: none;
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
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-mobile-card__meta-item--success {
  background: color-mix(in srgb, var(--success) 12%, transparent);
  color: var(--success);
}

.device-mobile-card__meta-item--warning {
  background: color-mix(in srgb, var(--warning) 12%, transparent);
  color: var(--warning);
}

.device-mobile-card__meta-item--danger {
  background: color-mix(in srgb, var(--danger) 12%, transparent);
  color: var(--danger);
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

.device-mobile-card__field .standard-mobile-record-card__field-value {
  overflow: hidden;
  display: block;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-mobile-card__address {
  display: -webkit-box;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
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

.device-form-relation-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.device-form-relation-card {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: calc(var(--radius-md) + 2px);
  border: 1px solid color-mix(in srgb, var(--brand) 10%, transparent);
  background: rgba(248, 251, 255, 0.92);
}

.device-form-relation-card span {
  color: var(--text-caption);
  font-size: 12px;
}

.device-form-relation-card strong {
  color: var(--text-heading);
  font-size: 14px;
  line-height: 1.5;
}

@media (min-width: 721px) {
  .device-mobile-list {
    display: none !important;
  }

  .device-desktop-table {
    display: block !important;
  }
}

@media (max-width: 900px) {
  .device-loading-table {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ops-drawer-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .device-form-relation-summary {
    grid-template-columns: minmax(0, 1fr);
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
