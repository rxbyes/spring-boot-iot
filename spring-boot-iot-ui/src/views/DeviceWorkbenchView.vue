<template>
  <div class="device-asset-view ops-workbench standard-list-view">
    <PanelCard
      eyebrow="Device Asset Workspace"
      title="设备资产中心"
      description="围绕设备台账、建档、维护与导出组织统一入口，帮助业务、运维和实施人员快速确认哪些设备已入库、当前状态如何、下一步该做什么。"
      class="ops-hero-card"
    >
      <template #actions>
        <div class="ops-hero-actions">
          <el-button v-permission="'iot:devices:add'" type="primary" @click="handleAdd">新增设备</el-button>
          <el-button v-permission="'iot:devices:import'" plain @click="handleOpenBatchImport">批量导入</el-button>
        </div>
      </template>
      <div class="ops-kpi-grid">
        <MetricCard label="设备总数" :value="String(pagination.total)" :badge="{ label: 'Asset', tone: 'brand' }" />
        <MetricCard label="当前页在线" :value="String(onlineCount)" :badge="{ label: 'Online', tone: 'success' }" />
        <MetricCard label="当前页已激活" :value="String(activatedCount)" :badge="{ label: 'Ready', tone: 'brand' }" />
        <MetricCard label="当前页停用" :value="String(disabledCount)" :badge="{ label: 'Review', tone: 'warning' }" />
      </div>
      <div class="ops-inline-note">
        当前交付已完成设备资产台账、详情查看、增改删、批量删除、批量导入、设备更换与列表导出闭环；远程控制、维修工单联动等能力继续保留在后续规划中。
      </div>
    </PanelCard>

    <PanelCard
      eyebrow="Device Filters"
      title="筛选条件"
      description="优先按设备 ID、产品 Key、设备编码和状态筛出需要补录、核查、停用或导出的设备资产。"
      class="ops-filter-card"
    >
      <el-form :model="searchForm" label-position="top" class="ops-filter-form">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="设备 ID">
              <el-input id="query-device-id" v-model="searchForm.deviceId" placeholder="请输入设备 ID" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="产品 Key">
              <el-input id="device-product-key" v-model="searchForm.productKey" placeholder="请输入产品 Key" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="设备编码">
              <el-input id="query-device-code" v-model="searchForm.deviceCode" placeholder="请输入设备编码" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="设备名称">
              <el-input id="filter-device-name" v-model="searchForm.deviceName" placeholder="请输入设备名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="在线状态">
              <el-select v-model="searchForm.onlineStatus" placeholder="请选择在线状态" clearable>
                <el-option label="在线" :value="1" />
                <el-option label="离线" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="激活状态">
              <el-select v-model="searchForm.activateStatus" placeholder="请选择激活状态" clearable>
                <el-option label="已激活" :value="1" />
                <el-option label="未激活" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="设备状态">
              <el-select v-model="searchForm.deviceStatus" placeholder="请选择设备状态" clearable>
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <div class="ops-filter-actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </el-form>
    </PanelCard>

    <PanelCard
      eyebrow="Device Inventory"
      title="设备资产列表"
      :description="`当前共 ${pagination.total} 条设备资产记录，支持详情、编辑、更换、删除、批量删除、批量导入和导出。`"
      class="ops-table-card"
    >
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

      <el-table ref="tableRef" v-loading="loading" :data="tableData" border stripe @selection-change="handleSelectionChange">
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

      <div class="ops-pagination">
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
      eyebrow="Device Asset Profile"
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
      eyebrow="Device Asset Form"
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type TableInstance } from 'element-plus'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import DeviceBatchImportDrawer from '@/components/DeviceBatchImportDrawer.vue'
import DeviceReplaceDrawer from '@/components/DeviceReplaceDrawer.vue'
import MetricCard from '@/components/MetricCard.vue'
import PanelCard from '@/components/PanelCard.vue'
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

interface DeviceFormState extends DeviceAddPayload {}

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
const editingDeviceId = ref<string | number | null>(null)

const tableData = ref<Device[]>([])
const selectedRows = ref<Device[]>([])
const productOptions = ref<Product[]>([])
const detailData = ref<Device | null>(null)
const batchImportResult = ref<DeviceBatchAddResult | null>(null)
const replacingDevice = ref<Device | null>(null)

const exportColumnDialogVisible = ref(false)
const exportColumnStorageKey = 'device-asset-view'

const searchForm = reactive<DeviceSearchForm>({
  deviceId: '',
  productKey: '',
  deviceCode: '',
  deviceName: '',
  onlineStatus: undefined,
  activateStatus: undefined,
  deviceStatus: undefined
})

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

const { pagination, applyPageResult, resetPage, setPageNum, setPageSize } = useServerPagination(10)

const formTitle = computed(() => (editingDeviceId.value ? '编辑设备' : '新增设备'))
const submitPermission = computed(() => (editingDeviceId.value ? 'iot:devices:update' : 'iot:devices:add'))
const detailTitle = computed(() => detailData.value?.deviceName || detailData.value?.deviceCode || '设备详情')
const onlineCount = computed(() => tableData.value.filter((item) => item.onlineStatus === 1).length)
const activatedCount = computed(() => tableData.value.filter((item) => item.activateStatus === 1).length)
const disabledCount = computed(() => tableData.value.filter((item) => item.deviceStatus === 0).length)
const metadataPreview = computed(() => prettyJson(detailData.value?.metadataJson || '{}'))

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

function applyRouteQueryToFilters() {
  searchForm.deviceId = typeof route.query.deviceId === 'string' ? route.query.deviceId : ''
  searchForm.productKey = typeof route.query.productKey === 'string' ? route.query.productKey : ''
  searchForm.deviceCode = typeof route.query.deviceCode === 'string' ? route.query.deviceCode : ''
  searchForm.deviceName = typeof route.query.deviceName === 'string' ? route.query.deviceName : ''
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

async function loadDevicePage() {
  loading.value = true
  try {
    const res = await deviceApi.pageDevices({
      deviceId: searchForm.deviceId || undefined,
      productKey: searchForm.productKey || undefined,
      deviceCode: searchForm.deviceCode || undefined,
      deviceName: searchForm.deviceName || undefined,
      onlineStatus: searchForm.onlineStatus,
      activateStatus: searchForm.activateStatus,
      deviceStatus: searchForm.deviceStatus,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res.code === 200 && res.data) {
      tableData.value = applyPageResult(res.data)
    }
  } catch (error) {
    console.error('获取设备分页失败', error)
  } finally {
    loading.value = false
  }
}

async function openDetail(id: string | number) {
  detailVisible.value = true
  detailLoading.value = true
  detailErrorMessage.value = ''
  detailData.value = null
  try {
    const res = await deviceApi.getDeviceById(id)
    if (res.code === 200) {
      detailData.value = res.data
    }
  } catch (error) {
    detailErrorMessage.value = error instanceof Error ? error.message : '加载设备详情失败'
  } finally {
    detailLoading.value = false
  }
}

async function loadEditableDetail(id: string | number) {
  const res = await deviceApi.getDeviceById(id)
  if (res.code === 200 && res.data) {
    resetFormData(res.data)
  }
}

function handleSearch() {
  resetPage()
  clearSelection()
  void loadDevicePage()
}

function handleReset() {
  searchForm.deviceId = ''
  searchForm.productKey = ''
  searchForm.deviceCode = ''
  searchForm.deviceName = ''
  searchForm.onlineStatus = undefined
  searchForm.activateStatus = undefined
  searchForm.deviceStatus = undefined
  resetPage()
  clearSelection()
  void loadDevicePage()
}

function handleRefresh() {
  clearSelection()
  void loadDevicePage()
}

function handleOpenBatchImport() {
  batchImportResult.value = null
  batchImportVisible.value = true
}

function handleAdd() {
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

async function handleDelete(row: Device) {
  try {
    await confirmDelete('设备', row.deviceName || row.deviceCode)
    await deviceApi.deleteDevice(row.id)
    ElMessage.success('删除成功')
    clearSelection()
    if (tableData.value.length === 1 && pagination.pageNum > 1) {
      setPageNum(pagination.pageNum - 1)
    }
    await loadDevicePage()
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
    if (deletingCount === tableData.value.length && pagination.pageNum > 1) {
      setPageNum(pagination.pageNum - 1)
    }
    await loadDevicePage()
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
      resetPage()
      clearSelection()
      await loadDevicePage()
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
    resetPage()
    clearSelection()
    await loadDevicePage()
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
    resetPage()
    await loadDevicePage()
  } catch (error) {
    console.error('提交设备失败', error)
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
  void loadDevicePage()
}

function handlePageChange(page: number) {
  setPageNum(page)
  clearSelection()
  void loadDevicePage()
}

watch(
  () => [route.query.deviceId, route.query.productKey, route.query.deviceCode, route.query.deviceName] as const,
  () => {
    applyRouteQueryToFilters()
    resetPage()
    clearSelection()
    void loadDevicePage()
  }
)

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

onMounted(async () => {
  applyRouteQueryToFilters()
  await loadProducts()
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

  .ops-drawer-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .ops-filter-actions {
    justify-content: stretch;
  }
}
</style>
