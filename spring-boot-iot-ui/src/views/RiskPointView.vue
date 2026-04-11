<template>
  <StandardPageShell class="risk-point-view">
    <StandardWorkbenchPanel
      title="风险对象中心"
      :description="`当前 ${pagination.total} 条风险点记录，支持档案维护和设备绑定。`"
      show-header-actions
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-notices
      show-toolbar
      show-pagination
    >
      <template #header-actions>
        <StandardButton action="add" @click="handleAdd">新增风险点</StandardButton>
      </template>

      <template #filters>
        <StandardListFilterHeader :model="filters">
          <template #primary>
            <el-form-item>
              <el-input
                v-model="filters.keyword"
                placeholder="快速搜索（风险点名称 / 风险点编号 / 所属区域）"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.riskPointLevel" placeholder="风险点等级" clearable>
                <el-option
                  v-for="option in riskPointLevelOptions"
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
        <div class="risk-point-notice-stack">
          <el-alert
            :title="riskPointAdvice"
            type="info"
            :closable="false"
            show-icon
            class="view-alert"
          />
          <el-alert
            v-if="missingBindingTotal > 0"
            :title="`待纳入风险对象 ${missingBindingTotal} 台，已有上报设备尚未形成风险监测绑定。`"
            type="warning"
            :closable="false"
            show-icon
            class="view-alert"
          >
            <ul class="risk-point-governance-list">
              <li v-for="item in missingBindingItems" :key="`${item.deviceId || item.deviceCode}`">
                <strong>{{ item.deviceCode || '--' }}</strong>
                <span>{{ item.deviceName || '未命名设备' }}</span>
                <span>最近上报 {{ formatDateTime(item.lastReportTime) }}</span>
              </li>
            </ul>
          </el-alert>
        </div>
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[`已选 ${selectedRows.length} 项`, `启用 ${enabledCount} 项`, `红色态势 ${redCount} 项`, `待纳管 ${missingBindingTotal} 台`, `停用 ${disabledCount} 项`]"
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
        element-loading-text="正在刷新风险点列表"
        element-loading-background="var(--loading-mask-bg)"
      >
        <div v-if="showListSkeleton" class="ops-list-loading-state" aria-live="polite" aria-busy="true">
          <div class="ops-list-loading-state__summary">
            <span v-for="item in 3" :key="item" class="ops-list-loading-pulse ops-list-loading-pill" />
          </div>
          <div class="ops-list-loading-table ops-list-loading-table--header">
            <span v-for="item in 6" :key="`risk-point-head-${item}`" class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--header" />
          </div>
          <div v-for="row in 5" :key="`risk-point-row-${row}`" class="ops-list-loading-table ops-list-loading-table--row">
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--wide" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-pill ops-list-loading-pill--status" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--medium" />
            <span class="ops-list-loading-pulse ops-list-loading-line ops-list-loading-line--short" />
          </div>
        </div>

        <template v-else-if="hasRecords">
          <el-table
            ref="tableRef"
            :data="riskPointList"
            border
            stripe
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="48" />
            <StandardTableTextColumn prop="riskPointCode" label="风险点编号" :width="150" />
            <StandardTableTextColumn prop="riskPointName" label="风险点名称" :min-width="180">
              <template #default="{ row }">
                <StandardActionLink
                  :data-testid="`risk-point-name-link-${row.id}`"
                  @click="openRiskPointDetail(row)"
                >
                  {{ row.riskPointName }}
                </StandardActionLink>
              </template>
            </StandardTableTextColumn>
            <el-table-column prop="orgName" label="所属组织" :min-width="160">
              <template #default="{ row }">
                <span>{{ row.orgName || '未配置组织' }}</span>
              </template>
            </el-table-column>
            <StandardTableTextColumn prop="regionName" label="所属区域" :min-width="140">
              <template #default="{ row }">
                <span>{{ row.regionName || '未配置区域' }}</span>
              </template>
            </StandardTableTextColumn>
            <StandardTableTextColumn prop="riskPointLevel" label="风险点等级" :width="120">
              <template #default="{ row }">
                <el-tag type="info" round>{{ getRiskPointLevelText(row.riskPointLevel) }}</el-tag>
              </template>
            </StandardTableTextColumn>
            <StandardTableTextColumn prop="currentRiskLevel" label="当前风险态势" :width="120">
              <template #default="{ row }">
                <el-tag :type="getCurrentRiskLevelType(row.currentRiskLevel || row.riskLevel)" round>
                  {{ getCurrentRiskLevelText(row.currentRiskLevel || row.riskLevel) }}
                </el-tag>
              </template>
            </StandardTableTextColumn>
            <StandardTableTextColumn prop="responsibleUser" label="负责人" :min-width="140">
              <template #default="{ row }">
                <span>{{ getResponsibleUserText(row) }}</span>
              </template>
            </StandardTableTextColumn>
            <StandardTableTextColumn prop="responsiblePhone" label="负责人电话" :width="140" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" round>{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <StandardTableTextColumn prop="createTime" label="创建时间" :width="180" />
            <el-table-column
              label="操作"
              :width="riskPointActionColumnWidth"
              fixed="right"
              class-name="standard-row-actions-column"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row }">
                <StandardWorkbenchRowActions
                  variant="table"
                  :direct-items="getRiskPointRowActions()"
                  :max-direct-items="3"
                  @command="(command) => handleRiskPointRowAction(command, row)"
                />
              </template>
            </el-table-column>
          </el-table>
        </template>

        <div v-else-if="!loading" class="standard-list-empty-state">
          <EmptyState :title="emptyStateTitle" :description="emptyStateDescription" />
          <div class="standard-list-empty-state__actions">
            <StandardButton v-if="hasAppliedFilters" action="reset" @click="handleClearAppliedFilters">清空筛选条件</StandardButton>
            <StandardButton v-else action="add" @click="handleAdd">新增风险点</StandardButton>
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
      subtitle="统一通过右侧抽屉维护风险点基础信息。"
      size="42rem"
      @close="handleFormClose"
    >
      <div class="ops-drawer-stack">
        <div class="ops-drawer-note">
          <strong>配置提示</strong>
          <span>{{ form.id ? '历史编号仅用于留档追踪；请同步核对组织、区域、负责人和档案等级信息。' : '保存后将自动生成系统编号；请先确认组织、区域和风险点等级，再补齐负责人信息。' }}</span>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="ops-drawer-form">
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>基础信息</h3>
                <p>维护风险点主档、所属组织、所属区域与档案等级，为后续监测、处置与空间治理提供统一标识。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item v-if="form.id" label="风险点编号">
                <el-input :model-value="form.riskPointCode || '--'" readonly />
              </el-form-item>
              <el-form-item label="风险点名称" prop="riskPointName">
                <el-input v-model="form.riskPointName" placeholder="请输入风险点名称" />
              </el-form-item>
              <el-form-item label="所属组织" prop="orgId">
                <el-tree-select
                  v-model="form.orgId"
                  :data="organizationOptions"
                  node-key="id"
                  check-strictly
                  clearable
                  :props="{ label: 'orgName', children: 'children', value: 'id' }"
                  placeholder="请选择所属组织"
                />
              </el-form-item>
              <el-form-item label="所属区域" prop="regionId">
                <el-tree-select
                  v-model="form.regionId"
                  :data="regionOptions"
                  :cache-data="regionOptionCache"
                  lazy
                  :load="loadRegionNode"
                  node-key="id"
                  check-strictly
                  clearable
                  :props="regionTreeProps"
                  placeholder="请选择所属区域"
                />
              </el-form-item>
              <el-form-item label="风险点等级" prop="riskPointLevel">
                <el-select v-model="form.riskPointLevel" placeholder="请选择风险点等级">
                  <el-option
                    v-for="option in riskPointLevelOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </el-form-item>
            </div>
          </section>

          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>治理信息</h3>
                <p>补齐负责人、责任电话、启停状态和风险说明，便于值班与治理人员快速确认风险点责任归属。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="负责人" prop="responsibleUser">
                <el-select
                  v-model="form.responsibleUser"
                  :placeholder="responsibleUserPlaceholder"
                  :loading="userOptionsLoading"
                  :disabled="!form.orgId || userOptionsLoading || userOptions.length === 0"
                  clearable
                >
                  <el-option
                    v-for="user in userOptions"
                    :key="user.id"
                    :label="user.realName || user.username"
                    :value="user.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="负责人电话" prop="responsiblePhone">
                <el-input v-model="form.responsiblePhone" placeholder="请输入负责人电话" />
              </el-form-item>
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="form.status">
                  <el-radio :value="0">启用</el-radio>
                  <el-radio :value="1">停用</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="描述" prop="description" class="ops-drawer-grid__full">
                <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入风险点描述、场景说明或治理备注" />
              </el-form-item>
              <el-form-item v-if="form.id" label="创建人编号">
                <el-input :model-value="form.createBy || '--'" readonly />
              </el-form-item>
              <el-form-item v-if="form.id" label="更新人编号">
                <el-input :model-value="form.updateBy || '--'" readonly />
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
      v-model="bindDeviceVisible"
      title="绑定设备"
      subtitle="统一通过右侧抽屉为风险点绑定设备与测点。"
      size="42rem"
      @close="handleBindDrawerClose"
    >
      <div class="ops-drawer-stack">
        <div class="ops-drawer-note">
          <strong>绑定提示</strong>
          <span>绑定完成后，风险对象会直接联动实时监测台、阈值策略和告警运营台，请确认设备与测点归属关系准确。</span>
        </div>
        <el-form :model="bindForm" label-position="top" class="ops-drawer-form">
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>绑定对象</h3>
                <p>确认当前风险点并选择要关联的设备、测点，形成后续监测链路。</p>
              </div>
            </div>
            <div class="ops-drawer-grid">
              <el-form-item label="风险点" class="ops-drawer-grid__full ops-drawer-readonly">
                <el-input v-model="bindForm.riskPointName" disabled />
              </el-form-item>
              <el-form-item label="设备">
                <el-select v-model="bindForm.deviceId" placeholder="请选择设备">
                  <el-option v-for="device in deviceList" :key="device.id" :label="device.deviceName" :value="device.id">
                    {{ device.deviceCode }} - {{ device.deviceName }}
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="测点">
                <el-select v-model="bindForm.metricIdentifier" placeholder="请选择测点">
                  <el-option v-for="metric in metricList" :key="metric.identifier" :label="metric.name" :value="metric.identifier">
                    {{ metric.name }}
                  </el-option>
                </el-select>
              </el-form-item>
            </div>
          </section>
        </el-form>
      </div>
      <template #footer>
        <StandardDrawerFooter
          :confirm-loading="submitLoading"
          @cancel="bindDeviceVisible = false"
          @confirm="handleBindSubmit"
        />
      </template>
    </StandardFormDrawer>

    <RiskPointDetailDrawer
      v-model="riskPointDetailVisible"
      :risk-point-id="detailRiskPoint?.id"
      :initial-risk-point="detailRiskPoint"
      :initial-summary="detailRiskPoint ? bindingSummaryMap[getIdKey(detailRiskPoint.id)] || null : null"
      @close="handleRiskPointDetailClose"
      @edit="handleEditFromDetail"
      @binding-workbench="handleBindingWorkbenchFromDetail"
    />

    <StandardFormDrawer
      v-model="bindingWorkbenchVisible"
      title="风险绑定工作台"
      :subtitle="bindingWorkbenchSubtitle"
      size="48rem"
      @close="handleBindingWorkbenchClose"
    >
      <div class="ops-drawer-stack">
        <div class="ops-drawer-note">
          <strong>工作台说明</strong>
          <span>统一在此查看正式绑定与待治理转正；正式绑定仍只维护 `risk_point_device`，待治理仍只处理 pending 台账与转正留痕。</span>
        </div>

        <section v-if="bindingWorkbenchRiskPoint" class="ops-drawer-section">
          <div class="ops-drawer-section__header">
            <div>
              <h3>{{ bindingWorkbenchRiskPoint.riskPointName || '未命名风险点' }}</h3>
              <p>{{ bindingWorkbenchRiskPoint.riskPointCode || '尚未生成风险点编号' }}</p>
            </div>
          </div>
          <div class="risk-point-binding-workbench__summary">
            <span>所属组织 {{ bindingWorkbenchRiskPoint.orgName || '未配置组织' }}</span>
            <span>{{ bindingWorkbenchSummary?.boundDeviceCount ?? 0 }} 台已绑定设备</span>
            <span>{{ bindingWorkbenchSummary?.boundMetricCount ?? 0 }} 个正式测点</span>
            <span>待治理 {{ bindingWorkbenchSummary?.pendingBindingCount ?? 0 }} 条</span>
          </div>
          <div class="risk-point-binding-workbench__switcher" role="tablist" aria-label="风险绑定工作台视图切换">
            <button
              type="button"
              class="risk-point-binding-workbench__switch"
              :class="{ 'is-active': bindingWorkbenchMode === 'formal' }"
              data-testid="binding-workbench-switch-formal"
              @click="handleBindingWorkbenchModeChange('formal')"
            >
              维护绑定
            </button>
            <button
              type="button"
              class="risk-point-binding-workbench__switch"
              :class="{ 'is-active': bindingWorkbenchMode === 'pending' }"
              data-testid="binding-workbench-switch-pending"
              @click="handleBindingWorkbenchModeChange('pending')"
            >
              待治理转正
            </button>
          </div>
        </section>

        <RiskPointBindingMaintenanceDrawer
          v-if="bindingWorkbenchMode === 'formal'"
          :model-value="bindingWorkbenchVisible"
          embedded
          :risk-point-id="bindingWorkbenchRiskPoint?.id"
          :risk-point-name="bindingWorkbenchRiskPoint?.riskPointName"
          :risk-point-code="bindingWorkbenchRiskPoint?.riskPointCode"
          :org-name="bindingWorkbenchRiskPoint?.orgName"
          :pending-binding-count="bindingWorkbenchSummary?.pendingBindingCount ?? 0"
          @updated="handleBindingWorkbenchUpdated"
        />

        <template v-else>
          <section class="ops-drawer-section">
          <div class="ops-drawer-section__header">
            <div>
              <h3>待治理记录</h3>
              <p>按当前风险点加载待治理设备记录，选择一条后查看候选测点与历史留痕。</p>
            </div>
          </div>
          <div v-if="pendingBindings.length === 0" class="standard-list-empty-state">
            <EmptyState title="暂无待治理记录" description="当前风险点下还没有需要人工转正的设备绑定。" />
          </div>
          <div v-else class="risk-point-pending-list">
            <button
              v-for="item in pendingBindings"
              :key="String(item.id)"
              type="button"
              class="risk-point-pending-list__item"
              :class="{ 'is-active': getIdKey(item.id) === getIdKey(pendingPromotionForm.pendingId) }"
              @click="handleSelectPendingRow(item)"
            >
              <strong>{{ item.deviceCode || '--' }}</strong>
              <span>{{ item.deviceName || '未命名设备' }}</span>
              <span>{{ item.resolutionStatus }}</span>
            </button>
          </div>
        </section>

        <section class="ops-drawer-section">
          <div class="ops-drawer-section__header">
            <div>
              <h3>推荐候选</h3>
              <p>点击候选卡片可加入或移出本轮转正清单，支持一次提交多个测点。</p>
            </div>
          </div>
          <div v-if="pendingCandidates.length === 0" class="standard-list-empty-state">
            <EmptyState title="暂无候选测点" description="当前待治理记录还没有可用于转正式绑定的测点候选。" />
          </div>
          <div v-else class="risk-point-pending-candidate-list">
            <button
              v-for="candidate in pendingCandidates"
              :key="candidate.metricIdentifier"
              type="button"
              class="risk-point-pending-candidate-list__item"
              :class="{ 'is-selected': isPendingMetricSelected(candidate.metricIdentifier) }"
              @click="togglePendingMetric(candidate)"
            >
              <div class="risk-point-pending-candidate-list__header">
                <strong>{{ candidate.metricName || candidate.metricIdentifier }}</strong>
                <span>{{ candidate.recommendationLevel || '--' }}</span>
              </div>
              <div class="risk-point-pending-candidate-list__meta">
                <span>{{ candidate.metricIdentifier }}</span>
                <span v-if="candidate.riskMetricId">目录指标 #{{ candidate.riskMetricId }}</span>
                <span>{{ (candidate.evidenceSources || []).join(' / ') }}</span>
              </div>
              <p v-if="candidate.reasonSummary" class="risk-point-pending-candidate-list__summary">{{ candidate.reasonSummary }}</p>
            </button>
          </div>
        </section>

        <section class="ops-drawer-section">
          <div class="ops-drawer-section__header">
            <div>
              <h3>提交设置</h3>
              <p>可补充本轮治理说明，并选择是否在本次操作后直接收口该 pending 记录。</p>
            </div>
          </div>
          <div class="ops-drawer-grid">
            <el-form-item label="治理备注" class="ops-drawer-grid__full">
              <el-input
                v-model="pendingPromotionForm.promotionNote"
                type="textarea"
                :rows="3"
                placeholder="请输入治理说明，可留空"
              />
            </el-form-item>
            <el-form-item label="收口方式">
              <el-radio-group v-model="pendingPromotionForm.completePending">
                <el-radio :value="true">本次收口</el-radio>
                <el-radio :value="false">继续保留待治理</el-radio>
              </el-radio-group>
            </el-form-item>
          </div>
        </section>

        <section v-if="pendingHistory.length > 0" class="ops-drawer-section">
          <div class="ops-drawer-section__header">
            <div>
              <h3>历史留痕</h3>
              <p>查看同一 pending 既往转正结果，辅助本轮人工判断。</p>
            </div>
          </div>
          <ul class="risk-point-pending-history">
            <li v-for="item in pendingHistory" :key="`${item.id || item.metricIdentifier}-${item.createTime || ''}`">
              <strong>{{ item.metricName || item.metricIdentifier || '--' }}</strong>
              <span>{{ item.promotionStatus || '--' }}</span>
              <span>{{ item.recommendationLevel || '--' }}</span>
            </li>
          </ul>
        </section>
        </template>
      </div>
      <template v-if="bindingWorkbenchMode === 'pending'" #footer>
        <StandardDrawerFooter
          :confirm-loading="submitLoading || pendingLoading"
          @cancel="closeBindingWorkbench"
          @confirm="handlePendingPromotionSubmit"
        />
      </template>
    </StandardFormDrawer>
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from '@/utils/message';
import EmptyState from '@/components/EmptyState.vue';
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue';
import StandardActionLink from '@/components/StandardActionLink.vue';
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue';
import StandardFormDrawer from '@/components/StandardFormDrawer.vue';
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardPageShell from '@/components/StandardPageShell.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue';
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue';
import RiskPointBindingMaintenanceDrawer from '@/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue';
import RiskPointDetailDrawer from '@/components/riskPoint/RiskPointDetailDrawer.vue';
import { isHandledRequestError, resolveRequestErrorMessage } from '@/api/request';
import { usePermissionStore } from '@/stores/permission';
import { useListAppliedFilters } from '@/composables/useListAppliedFilters';
import { useServerPagination } from '@/composables/useServerPagination';
import { listMissingBindings, type RiskGovernanceGapItem } from '@/api/riskGovernance';
import { listOrganizationTree } from '@/api/organization';
import type { Organization } from '@/api/organization';
import { listRegions } from '@/api/region';
import type { Region } from '@/api/region';
import { getUser } from '@/api/user';
import type { User } from '@/api/user';
import { getDeviceMetricOptions } from '@/api/iot';
import type { DeviceMetricOption, DeviceOption, IdType } from '@/types/api';
import { resolveWorkbenchActionColumnWidth } from '@/utils/adaptiveActionColumn';
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm';
import { getRiskLevelTagType, getRiskLevelText as resolveRiskLevelText, normalizeRiskLevel } from '@/utils/riskLevel';
import {
  fetchRiskPointLevelOptions,
  getRiskPointLevelText as resolveRiskPointLevelText,
  type RiskPointLevelOption
} from '@/utils/riskPointLevel';
import {
  pageRiskPointList,
  getRiskPointById,
  addRiskPoint,
  updateRiskPoint,
  deleteRiskPoint,
  bindDevice,
  listBindableDevices,
  listBindingSummaries,
  listPendingBindings,
  getPendingBindingCandidates,
  promotePendingBinding
} from '../api/riskPoint';
import type {
  RiskPoint,
  RiskPointBindingSummary,
  RiskPointPendingBindingItem,
  RiskPointPendingMetricCandidate,
  RiskPointPendingPromotionHistory
} from '../api/riskPoint';
import { formatDateTime } from '@/utils/format';

type RegionTreeOption = Partial<Region> & {
  id: Region['id'];
  regionName: string;
  children?: RegionTreeOption[];
  leaf?: boolean;
};
type LazyRegionTreeNode = {
  level: number;
  data?: RegionTreeOption;
};
type TreeResolveFn = (data: RegionTreeOption[]) => void;
type RiskPointBindingWorkbenchMode = 'formal' | 'pending';
type RiskPointRowActionCommand = 'detail' | 'edit' | 'binding-workbench' | 'delete';

const PROMOTABLE_PENDING_STATUSES = new Set(['PENDING_METRIC_GOVERNANCE', 'PARTIALLY_PROMOTED']);
const permissionStore = usePermissionStore();

const loading = ref(false);
const formVisible = ref(false);
const bindDeviceVisible = ref(false);
const riskPointDetailVisible = ref(false);
const bindingWorkbenchVisible = ref(false);
const bindingWorkbenchMode = ref<RiskPointBindingWorkbenchMode>('formal');
const handledGovernanceBindingRouteKey = ref('');
const riskPointList = ref<RiskPoint[]>([]);
const bindingSummaryMap = ref<Record<string, RiskPointBindingSummary>>({});
const organizationOptions = ref<Organization[]>([]);
const regionOptions = ref<RegionTreeOption[]>([]);
const regionOptionCache = ref<RegionTreeOption[]>([]);
const userOptions = ref<User[]>([]);
const userOptionsLoading = ref(false);
const riskPointLevelOptions = ref<RiskPointLevelOption[]>([]);
const deviceList = ref<DeviceOption[]>([]);
const metricList = ref<DeviceMetricOption[]>([]);
const pendingBindings = ref<RiskPointPendingBindingItem[]>([]);
const pendingCandidates = ref<RiskPointPendingMetricCandidate[]>([]);
const pendingHistory = ref<RiskPointPendingPromotionHistory[]>([]);
const pendingLoading = ref(false);
const missingBindingItems = ref<RiskGovernanceGapItem[]>([]);
const tableRef = ref();
const selectedRows = ref<RiskPoint[]>([]);
const detailRiskPoint = ref<RiskPoint | null>(null);
const bindingWorkbenchRiskPoint = ref<RiskPoint | null>(null);
const riskPointActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: [
    { command: 'detail', label: '详情' },
    { command: 'edit', label: '编辑' },
    { command: 'binding-workbench', label: '风险绑定' },
    { command: 'delete', label: '删除' }
  ],
});

const filters = reactive({
  keyword: '',
  riskPointLevel: '',
  status: '' as '' | number
});
const appliedFilters = reactive({
  keyword: '',
  riskPointLevel: '',
  status: '' as '' | number
});

const route = useRoute();
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum } = useServerPagination();

const formRef = ref();
const formTitle = computed(() => (form.id ? '编辑风险点' : '新增风险点'));
const form = reactive({
  id: undefined as IdType | undefined,
  riskPointCode: '',
  riskPointName: '',
  orgId: '' as '' | IdType,
  orgName: '',
  regionId: '' as '' | IdType,
  regionName: '',
  responsibleUser: '' as '' | IdType,
  responsiblePhone: '',
  riskPointLevel: '',
  description: '',
  status: 0,
  createBy: undefined as IdType | undefined,
  updateBy: undefined as IdType | undefined
});

const rules = {
  riskPointName: [{ required: true, message: '请输入风险点名称', trigger: 'blur' }],
  orgId: [{ required: true, message: '请选择所属组织', trigger: 'change' }],
  regionId: [{ required: true, message: '请选择所属区域', trigger: 'change' }],
  riskPointLevel: [{ required: true, message: '请选择风险点等级', trigger: 'change' }]
};

const bindForm = reactive({
  riskPointId: '' as '' | IdType,
  riskPointName: '',
  deviceId: '' as '' | IdType,
  deviceCode: '',
  deviceName: '',
  metricIdentifier: '',
  metricName: ''
});
const pendingPromotionForm = reactive({
  riskPointId: undefined as IdType | undefined,
  pendingId: undefined as IdType | undefined,
  selectedMetrics: [] as Array<{ riskMetricId?: IdType | null; metricIdentifier: string; metricName: string }>,
  completePending: true,
  promotionNote: ''
});
const submitLoading = ref(false);
const riskPointAdvice = '优先核查一级风险点和红色态势对象';
const missingBindingTotal = ref(0);
const knownUsers = reactive<Record<string, User>>({});
const regionRootsLoaded = ref(false);
const regionRootsLoading = ref(false);
let latestListRequestId = 0;

const getIdKey = (value?: IdType | null) => {
  if (value === undefined || value === null || value === '') {
    return '';
  }
  return String(value);
};

const isSameId = (left?: IdType | null, right?: IdType | null) => {
  const leftKey = getIdKey(left);
  if (!leftKey) {
    return false;
  }
  return leftKey === getIdKey(right);
};

const enabledCount = computed(() => riskPointList.value.filter((item) => item.status === 0).length);
const redCount = computed(() =>
  riskPointList.value.filter((item) => normalizeRiskLevel(item.currentRiskLevel || item.riskLevel) === 'red').length
);
const disabledCount = computed(() => riskPointList.value.filter((item) => item.status === 1).length);
const hasRecords = computed(() => riskPointList.value.length > 0);
const showListSkeleton = computed(() => loading.value && !hasRecords.value);
const bindingWorkbenchSummary = computed(() => {
  if (!bindingWorkbenchRiskPoint.value?.id) {
    return null;
  }
  return bindingSummaryMap.value[getIdKey(bindingWorkbenchRiskPoint.value.id)] || null;
});
const bindingWorkbenchSubtitle = computed(() =>
  bindingWorkbenchMode.value === 'pending'
    ? '查看系统推荐候选并提交一个或多个测点转正式绑定。'
    : '查看正式绑定摘要，并可在同一工作台切换到待治理转正。'
);
const emptyStateTitle = computed(() => (hasAppliedFilters.value ? '没有符合条件的风险点' : '还没有风险对象'));
const emptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? '已生效筛选暂时没有匹配结果，可以调整条件，或者直接清空当前筛选。'
    : '当前还没有风险对象记录，先新增风险点，再继续设备绑定和策略治理。'
);
const selectedOrganization = computed(() =>
  form.orgId === '' ? null : findOrganizationById(organizationOptions.value, form.orgId)
);
const regionTreeProps = {
  label: 'regionName',
  children: 'children',
  value: 'id',
  isLeaf: 'leaf'
};
const responsibleUserPlaceholder = computed(() => {
  if (!form.orgId) {
    return '请先选择所属组织';
  }
  if (userOptionsLoading.value) {
    return '正在加载机构负责人';
  }
  if (userOptions.value.length === 0) {
    return '当前机构未配置管理员';
  }
  return '请选择负责人';
});

const showRiskPointRequestError = (error: unknown, fallbackMessage: string) => {
  if (isHandledRequestError(error)) {
    return;
  }
  ElMessage.error(resolveRequestErrorMessage(error, fallbackMessage));
};

const logRiskPointRequestError = (context: string, error: unknown) => {
  if (isHandledRequestError(error)) {
    return;
  }
  console.error(context, error);
};

const loadOrganizationOptions = async () => {
  try {
    const res = await listOrganizationTree();
    if (res.code === 200) {
      organizationOptions.value = (res.data || []).filter((item) => item.status === 1);
      if (formVisible.value) {
        upsertOrganizationOption({
          id: form.orgId || undefined,
          orgName: form.orgName,
          phone: form.responsiblePhone
        });
      }
    }
  } catch (error) {
    logRiskPointRequestError('加载组织树失败', error);
    showRiskPointRequestError(error, '加载组织树失败');
  }
};

const findOrganizationById = (nodes: Organization[], targetId: IdType): Organization | null => {
  for (const node of nodes) {
    if (isSameId(node.id, targetId)) {
      return node;
    }
    const childMatch = node.children?.length ? findOrganizationById(node.children, targetId) : null;
    if (childMatch) {
      return childMatch;
    }
  }
  return null;
};

const findRegionById = <T extends { id?: Region['id']; children?: T[] }>(nodes: T[], targetId: IdType): T | null => {
  for (const node of nodes) {
    if (isSameId(node.id, targetId)) {
      return node;
    }
    const childMatch = node.children?.length ? findRegionById(node.children, targetId) : null;
    if (childMatch) {
      return childMatch;
    }
  }
  return null;
};

const toRegionTreeOption = (region: Partial<Region> & { id: Region['id']; regionName: string }): RegionTreeOption => {
  const children = Array.isArray(region.children)
    ? region.children.map((item) => toRegionTreeOption(item as RegionTreeOption))
    : undefined;
  return {
    ...region,
    children,
    leaf: !region.hasChildren
  };
};

const upsertOrganizationOption = (organization?: Partial<Organization> | null) => {
  if (!organization?.id || !organization.orgName) {
    return;
  }
  if (findOrganizationById(organizationOptions.value, organization.id)) {
    return;
  }
  organizationOptions.value = [
    ...organizationOptions.value,
    {
      id: organization.id,
      tenantId: organization.tenantId ?? '',
      parentId: organization.parentId ?? '',
      orgName: organization.orgName,
      orgCode: organization.orgCode || '',
      orgType: organization.orgType || 'dept',
      leaderUserId: organization.leaderUserId,
      leaderName: organization.leaderName || '',
      phone: organization.phone || '',
      email: organization.email || '',
      status: organization.status ?? 1,
      sortNo: organization.sortNo ?? 0,
      remark: organization.remark || '',
      createBy: (organization.createBy as number) ?? 0,
      createTime: organization.createTime || '',
      updateBy: (organization.updateBy as number) ?? 0,
      updateTime: organization.updateTime || '',
      deleted: organization.deleted ?? 0,
      children: organization.children,
      hasChildren: organization.hasChildren
    }
  ];
};

const upsertRegionCache = (region?: Partial<Region> | null) => {
  if (!region?.id || !region.regionName) {
    return;
  }
  const nextItem = toRegionTreeOption({
    id: region.id,
    regionName: region.regionName,
    parentId: region.parentId,
    hasChildren: region.hasChildren,
    status: region.status
  });
  const existingIndex = regionOptionCache.value.findIndex((item) => isSameId(item.id, region.id));
  if (existingIndex >= 0) {
    regionOptionCache.value.splice(existingIndex, 1, {
      ...regionOptionCache.value[existingIndex],
      ...nextItem
    });
    return;
  }
  regionOptionCache.value.push(nextItem);
};

const ensureRegionRootsLoaded = async () => {
  if (regionRootsLoaded.value || regionRootsLoading.value) {
    return;
  }
  regionRootsLoading.value = true;
  try {
    const res = await listRegions();
    if (res.code === 200) {
      regionOptions.value = (res.data || [])
        .filter((item) => item.status === 1)
        .map((item) => toRegionTreeOption(item));
      regionRootsLoaded.value = true;
    }
  } catch (error) {
    logRiskPointRequestError('加载区域根节点失败', error);
    showRiskPointRequestError(error, '加载区域根节点失败');
  } finally {
    regionRootsLoading.value = false;
  }
};

const loadRegionNode = async (node: LazyRegionTreeNode, resolve: TreeResolveFn) => {
  if (node.level === 0) {
    await ensureRegionRootsLoaded();
    resolve(regionOptions.value);
    return;
  }
  const parentId = node.data?.id;
  if (!parentId) {
    resolve([]);
    return;
  }

  try {
    const res = await listRegions(parentId);
    const children = (res.data || []).map((item) => toRegionTreeOption(item));
    if (node.data) {
      node.data.children = children;
    }
    resolve(children);
  } catch (error) {
    logRiskPointRequestError('加载区域子节点失败', error);
    showRiskPointRequestError(error, '加载区域子节点失败');
    resolve([]);
  }
};

const upsertKnownUser = (user?: User | null) => {
  const userKey = getIdKey(user?.id);
  if (!userKey) {
    return;
  }
  knownUsers[userKey] = user as User;
};

const setResponsibleUserOptions = (users: Array<User | null | undefined>) => {
  const seenIds = new Set<string>();
  userOptions.value = users.filter((user): user is User => {
    const userKey = getIdKey(user?.id);
    if (!userKey) {
      return false;
    }
    if (seenIds.has(userKey)) {
      return false;
    }
    seenIds.add(userKey);
    upsertKnownUser(user);
    return true;
  });
};

const buildOrganizationLeaderFallback = (organization: Organization): User | null => {
  if (!organization.leaderUserId) {
    return null;
  }
  const fallbackUser: User = {
    id: organization.leaderUserId,
    username: organization.leaderName || String(organization.leaderUserId),
    realName: organization.leaderName || String(organization.leaderUserId),
    phone: organization.phone || '',
    status: 1
  };
  upsertKnownUser(fallbackUser);
  return fallbackUser;
};

const fetchUserById = async (userId?: IdType) => {
  const userKey = getIdKey(userId);
  if (!userKey) {
    return null;
  }
  const cachedUser = knownUsers[userKey];
  if (cachedUser) {
    return cachedUser;
  }
  try {
    const res = await getUser(userId);
    if (res.code === 200 && res.data) {
      upsertKnownUser(res.data);
      return res.data;
    }
  } catch (error) {
    logRiskPointRequestError('加载用户详情失败', error);
  }
  return null;
};

const buildCurrentEditableUserFallback = async () => {
  const currentUserInfo = permissionStore.userInfo;
  if (!currentUserInfo?.id) {
    return null;
  }
  const fallbackUser: User = {
    id: currentUserInfo.id,
    orgId: currentUserInfo.orgId,
    orgName: currentUserInfo.orgName,
    username: currentUserInfo.username || currentUserInfo.displayName || String(currentUserInfo.id),
    realName: currentUserInfo.realName || currentUserInfo.displayName || currentUserInfo.username || String(currentUserInfo.id),
    phone: currentUserInfo.phone || '',
    status: 1
  };
  upsertKnownUser(fallbackUser);
  return fallbackUser;
};

const loadResponsibleOptionsByOrganization = async (preserveSelection = false) => {
  const organization = selectedOrganization.value;
  form.orgName = organization?.orgName || '';
  if (!organization) {
    userOptions.value = [];
    if (!preserveSelection) {
      form.responsibleUser = '';
      form.responsiblePhone = '';
    }
    return;
  }

  userOptionsLoading.value = true;
  try {
    const nextUsers: User[] = [];
    let defaultResponsibleUser = await fetchUserById(organization.leaderUserId);
    if (!defaultResponsibleUser) {
      defaultResponsibleUser = buildOrganizationLeaderFallback(organization);
    }
    if (!defaultResponsibleUser) {
      defaultResponsibleUser = await buildCurrentEditableUserFallback();
    }
    if (defaultResponsibleUser) {
      nextUsers.push(defaultResponsibleUser);
    }
    if (preserveSelection && form.responsibleUser) {
      const currentUserId = form.responsibleUser;
      if (!defaultResponsibleUser || !isSameId(defaultResponsibleUser.id, currentUserId)) {
        const currentUser = await fetchUserById(currentUserId);
        if (currentUser) {
          nextUsers.push(currentUser);
        }
      }
    }
    setResponsibleUserOptions(nextUsers);

    if (preserveSelection && form.responsibleUser) {
      const selectedUserKey = getIdKey(form.responsibleUser);
      if (userOptions.value.some((user) => getIdKey(user.id) === selectedUserKey)) {
        return;
      }
    }

    if (defaultResponsibleUser?.id) {
      form.responsibleUser = defaultResponsibleUser.id;
      form.responsiblePhone = defaultResponsibleUser.phone || organization.phone || permissionStore.userInfo?.phone || '';
      return;
    }

    form.responsibleUser = '';
    form.responsiblePhone = organization.phone || permissionStore.userInfo?.phone || '';
  } finally {
    userOptionsLoading.value = false;
  }
};

const loadRiskPointLevelOptions = async () => {
  try {
    riskPointLevelOptions.value = await fetchRiskPointLevelOptions();
    if (!form.id && !form.riskPointLevel) {
      form.riskPointLevel = riskPointLevelOptions.value[0]?.value || '';
    }
  } catch (error) {
    logRiskPointRequestError('加载风险点等级字典失败', error);
    showRiskPointRequestError(error, '加载风险点等级字典失败');
  }
};

const loadBindableDeviceOptions = async (riskPointId: string | number) => {
  try {
    const res = await listBindableDevices(riskPointId);
    if (res.code === 200) {
      deviceList.value = res.data || [];
    }
  } catch (error) {
    logRiskPointRequestError('加载可绑定设备失败', error);
    showRiskPointRequestError(error, '加载可绑定设备失败');
  }
};

const loadMetricOptions = async (deviceId: string | number) => {
  try {
    const res = await getDeviceMetricOptions(deviceId);
    if (res.code === 200) {
      metricList.value = res.data || [];
    }
  } catch (error) {
    logRiskPointRequestError('加载测点选项失败', error);
    showRiskPointRequestError(error, '加载测点列表失败');
  }
};

const getCurrentRiskLevelType = (level: string) => getRiskLevelTagType(level);

const getCurrentRiskLevelText = (level: string) => resolveRiskLevelText(level);

const getRiskPointLevelText = (level?: string) => resolveRiskPointLevelText(level, riskPointLevelOptions.value);

const getResponsibleUserText = (row: Partial<RiskPoint>) => {
  if (!row.responsibleUser) {
    return '未指定负责人';
  }
  if (row.responsibleUserName) {
    return row.responsibleUserName;
  }
  const matchedUser = knownUsers[getIdKey(row.responsibleUser)];
  return matchedUser?.realName || matchedUser?.username || String(row.responsibleUser);
};

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
    { key: 'keyword', label: '快速搜索' },
    { key: 'riskPointLevel', label: (value) => `风险点等级：${getRiskPointLevelText(String(value || ''))}` },
    { key: 'status', label: (value) => `状态：${getStatusText(Number(value))}`, clearValue: '' as '' | number }
  ],
  defaults: {
    keyword: '',
    riskPointLevel: '',
    status: '' as '' | number
  }
});

const loadBindingSummaries = async (rows: RiskPoint[], requestId = latestListRequestId) => {
  const riskPointIds = Array.from(
    new Set(
      rows
        .map((item) => getIdKey(item.id))
        .filter(Boolean)
    )
  );
  if (riskPointIds.length === 0) {
    if (requestId === latestListRequestId) {
      bindingSummaryMap.value = {};
    }
    return;
  }

  try {
    const res = await listBindingSummaries(riskPointIds);
    if (requestId !== latestListRequestId) {
      return;
    }
    if (res.code !== 200) {
      bindingSummaryMap.value = {};
      return;
    }
    bindingSummaryMap.value = (res.data || []).reduce<Record<string, RiskPointBindingSummary>>((acc, item) => {
      if (!item?.riskPointId) {
        return acc;
      }
      acc[getIdKey(item.riskPointId)] = item;
      return acc;
    }, {});
  } catch (error) {
    logRiskPointRequestError('加载风险点绑定概览失败', error);
    if (requestId === latestListRequestId) {
      bindingSummaryMap.value = {};
    }
  }
};

const loadRiskPointList = async () => {
  const requestId = ++latestListRequestId;
  loading.value = true;
  try {
    const [listResult, backlogResult] = await Promise.allSettled([
      pageRiskPointList({
        keyword: appliedFilters.keyword || undefined,
        riskPointLevel: appliedFilters.riskPointLevel || undefined,
        status: appliedFilters.status === '' ? undefined : Number(appliedFilters.status),
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      }),
      listMissingBindings({
        pageNum: 1,
        pageSize: 3
      })
    ]);
    if (requestId !== latestListRequestId) {
      return;
    }

    if (listResult.status === 'fulfilled' && listResult.value?.code === 200) {
      riskPointList.value = applyPageResult(listResult.value.data);
      await loadBindingSummaries(riskPointList.value, requestId);
      riskPointList.value.forEach((item) => {
        if (!item.responsibleUser) {
          return;
        }
        upsertKnownUser({
          id: item.responsibleUser,
          username: item.responsibleUserName || String(item.responsibleUser),
          realName: item.responsibleUserName || String(item.responsibleUser),
          phone: item.responsiblePhone || '',
          status: 1
        });
      });
    } else {
      riskPointList.value = [];
      bindingSummaryMap.value = {};
    }

    if (backlogResult.status === 'fulfilled' && backlogResult.value?.code === 200) {
      missingBindingItems.value = backlogResult.value.data.records ?? [];
      missingBindingTotal.value = backlogResult.value.data.total ?? 0;
    } else {
      missingBindingItems.value = [];
      missingBindingTotal.value = 0;
    }
  } catch (error) {
    if (requestId !== latestListRequestId) {
      return;
    }
    logRiskPointRequestError('查询风险点列表失败', error);
    showRiskPointRequestError(error, '查询风险点列表失败');
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
  void loadRiskPointList();
};

const handleReset = () => {
  filters.keyword = '';
  filters.riskPointLevel = '';
  filters.status = '';
  syncAppliedFilters();
  resetPage();
  clearSelection();
  void loadRiskPointList();
};

const handleSizeChange = (size: number) => {
  setPageSize(size);
  void loadRiskPointList();
};

const handlePageChange = (page: number) => {
  setPageNum(page);
  void loadRiskPointList();
};

const handleSelectionChange = (rows: RiskPoint[]) => {
  selectedRows.value = rows;
};

const getRiskPointRowActions = () => [
  { command: 'detail' as const, label: '详情' },
  { command: 'edit' as const, label: '编辑' },
  { command: 'binding-workbench' as const, label: '风险绑定' },
  { command: 'delete' as const, label: '删除' }
];

const handleRiskPointRowAction = (command: RiskPointRowActionCommand, row: RiskPoint) => {
  if (command === 'detail') {
    openRiskPointDetail(row);
    return;
  }
  if (command === 'edit') {
    handleEdit(row);
    return;
  }
  if (command === 'binding-workbench') {
    void openBindingWorkbench(row, 'formal');
    return;
  }
  handleDelete(row);
};

const clearSelection = () => {
  tableRef.value?.clearSelection?.();
  selectedRows.value = [];
};

const handleRefresh = () => {
  clearSelection();
  void loadRiskPointList();
};

const openRiskPointDetail = (row: RiskPoint) => {
  detailRiskPoint.value = row;
  riskPointDetailVisible.value = true;
};

const handleRiskPointDetailClose = () => {
  detailRiskPoint.value = null;
};

const handleEditFromDetail = async () => {
  const row = detailRiskPoint.value;
  riskPointDetailVisible.value = false;
  detailRiskPoint.value = null;
  if (row) {
    await handleEdit(row);
  }
};

const handleBindingWorkbenchFromDetail = async () => {
  const row = detailRiskPoint.value;
  riskPointDetailVisible.value = false;
  detailRiskPoint.value = null;
  if (row) {
    await openBindingWorkbench(row, 'formal');
  }
};

const openBindingWorkbench = async (row: RiskPoint, mode: RiskPointBindingWorkbenchMode = 'formal') => {
  bindingWorkbenchRiskPoint.value = row;
  bindingWorkbenchMode.value = mode;
  bindingWorkbenchVisible.value = true;
  if (mode !== 'pending') {
    resetPendingPromotionState();
    return;
  }
  resetPendingPromotionState();
  pendingPromotionForm.riskPointId = row.id;
  await loadPendingBindings();
};

const handleBindingWorkbenchModeChange = async (mode: RiskPointBindingWorkbenchMode) => {
  bindingWorkbenchMode.value = mode;
  if (mode !== 'pending' || !bindingWorkbenchRiskPoint.value) {
    return;
  }
  if (
    getIdKey(pendingPromotionForm.riskPointId) === getIdKey(bindingWorkbenchRiskPoint.value.id)
    && pendingBindings.value.length > 0
  ) {
    return;
  }
  resetPendingPromotionState();
  pendingPromotionForm.riskPointId = bindingWorkbenchRiskPoint.value.id;
  await loadPendingBindings();
};

const handleBindingWorkbenchUpdated = () => {
  void loadRiskPointList();
};

const closeBindingWorkbench = () => {
  bindingWorkbenchVisible.value = false;
  handleBindingWorkbenchClose();
};

const handleRemoveAppliedFilter = (key: string) => {
  removeAppliedFilter(key);
  resetPage();
  clearSelection();
  void loadRiskPointList();
};

const handleClearAppliedFilters = () => {
  handleReset();
};

function applyRouteQueryToFilters() {
  filters.keyword = parseRouteStringQuery(route.query.keyword);
  filters.riskPointLevel = parseRouteStringQuery(route.query.riskPointLevel);
  filters.status = parseRouteNumberQuery(route.query.status) ?? '';
}

function parseRouteStringQuery(value: unknown) {
  const raw = Array.isArray(value) ? value[0] : value;
  return typeof raw === 'string' ? raw.trim() : '';
}

function parseRouteBindingAction(value: unknown): 'pending-promotion' | 'maintain-binding' | '' {
  const text = parseRouteStringQuery(value);
  return text === 'pending-promotion' || text === 'maintain-binding' ? text : '';
}

function parseRouteNumberQuery(value: unknown) {
  const text = parseRouteStringQuery(value);
  if (!text) {
    return undefined;
  }
  const parsed = Number(text);
  return Number.isFinite(parsed) ? parsed : undefined;
}

const resetRiskPointForm = () => {
  form.id = undefined;
  form.riskPointCode = '';
  form.riskPointName = '';
  form.orgId = '';
  form.orgName = '';
  form.regionId = '';
  form.regionName = '';
  form.responsibleUser = '';
  form.responsiblePhone = '';
  form.riskPointLevel = riskPointLevelOptions.value[0]?.value || '';
  form.description = '';
  form.status = 0;
  form.createBy = undefined;
  form.updateBy = undefined;
  userOptions.value = [];
};

const resetBindForm = () => {
  bindForm.riskPointId = '';
  bindForm.riskPointName = '';
  bindForm.deviceId = '';
  bindForm.deviceCode = '';
  bindForm.deviceName = '';
  bindForm.metricIdentifier = '';
  bindForm.metricName = '';
  metricList.value = [];
};

const resetPendingPromotionState = () => {
  pendingBindings.value = [];
  pendingCandidates.value = [];
  pendingHistory.value = [];
  pendingLoading.value = false;
  pendingPromotionForm.riskPointId = undefined;
  pendingPromotionForm.pendingId = undefined;
  pendingPromotionForm.selectedMetrics = [];
  pendingPromotionForm.completePending = true;
  pendingPromotionForm.promotionNote = '';
};

const handleAdd = () => {
  resetRiskPointForm();
  formVisible.value = true;
  void ensureRegionRootsLoaded();
};

const handleEdit = async (row: RiskPoint) => {
  form.id = row.id;
  form.riskPointCode = row.riskPointCode;
  form.riskPointName = row.riskPointName;
  form.orgId = row.orgId || '';
  form.orgName = row.orgName || '';
  form.regionId = row.regionId || '';
  form.regionName = row.regionName || '';
  form.responsibleUser = row.responsibleUser || '';
  form.responsiblePhone = row.responsiblePhone;
  form.riskPointLevel = row.riskPointLevel || '';
  form.description = row.description || '';
  form.status = row.status;
  form.createBy = row.createBy;
  form.updateBy = row.updateBy;
  upsertOrganizationOption({
    id: row.orgId,
    orgName: row.orgName,
    leaderUserId: row.responsibleUser,
    leaderName: row.responsibleUserName || '',
    phone: row.responsiblePhone
  });
  upsertRegionCache({
    id: row.regionId,
    regionName: row.regionName,
    status: 1
  });
  if (row.responsibleUser) {
    upsertKnownUser({
      id: row.responsibleUser,
      username: row.responsibleUserName || String(row.responsibleUser),
      realName: row.responsibleUserName || String(row.responsibleUser),
      phone: row.responsiblePhone || '',
      status: 1
    });
  }
  formVisible.value = true;
  void ensureRegionRootsLoaded();
  await loadResponsibleOptionsByOrganization(true);
};

const handleDelete = async (row: RiskPoint) => {
  try {
    await confirmDelete('风险点', row.riskPointName);
    const res = await deleteRiskPoint(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      void loadRiskPointList();
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return;
    }
    logRiskPointRequestError('删除风险点失败', error);
    showRiskPointRequestError(error, '删除风险点失败');
  }
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  try {
    submitLoading.value = true;
    const selectedOrganization = form.orgId === '' ? null : findOrganizationById(organizationOptions.value, form.orgId);
    const selectedRegion = form.regionId === '' ? null : findRegionById(regionOptions.value, form.regionId)
      || (form.regionId === '' ? null : findRegionById(regionOptionCache.value, form.regionId));
    form.orgName = selectedOrganization?.orgName || '';
    form.regionName = selectedRegion?.regionName || form.regionName || '';
    const payload = {
      ...form,
      orgId: form.orgId === '' ? undefined : form.orgId,
      regionId: form.regionId === '' ? undefined : form.regionId,
      responsibleUser: form.responsibleUser === '' ? undefined : form.responsibleUser
    };
    const res = form.id ? await updateRiskPoint(payload) : await addRiskPoint(payload);
    if (res.code === 200) {
      ElMessage.success(form.id ? '更新成功' : '新增成功');
      formVisible.value = false;
      void loadRiskPointList();
    }
  } catch (error) {
    logRiskPointRequestError('提交表单失败', error);
    showRiskPointRequestError(error, '提交风险点失败');
  } finally {
    submitLoading.value = false;
  }
};

const handleBindDevice = async (row: RiskPoint) => {
  resetBindForm();
  bindForm.riskPointId = row.id;
  bindForm.riskPointName = row.riskPointName;
  await loadBindableDeviceOptions(bindForm.riskPointId);
  bindDeviceVisible.value = true;
};

const handleOpenPendingPromotion = async (row: RiskPoint) => {
  await openBindingWorkbench(row, 'pending');
};

const loadPendingBindings = async () => {
  if (!getIdKey(pendingPromotionForm.riskPointId)) {
    pendingBindings.value = [];
    pendingCandidates.value = [];
    pendingHistory.value = [];
    return;
  }
  try {
    pendingLoading.value = true;
    const res = await listPendingBindings({
      riskPointId: pendingPromotionForm.riskPointId,
      pageNum: 1,
      pageSize: 10
    });
    if (res.code !== 200) {
      pendingBindings.value = [];
      pendingCandidates.value = [];
      pendingHistory.value = [];
      return;
    }
    pendingBindings.value = res.data.records || [];
    const selectedPending = pendingBindings.value.find((item) =>
      getIdKey(item.id) === getIdKey(pendingPromotionForm.pendingId) && isPromotablePending(item)
    ) || pendingBindings.value.find((item) => isPromotablePending(item));
    if (!selectedPending) {
      pendingPromotionForm.pendingId = pendingBindings.value[0]?.id;
      pendingPromotionForm.selectedMetrics = [];
      pendingCandidates.value = [];
      pendingHistory.value = [];
      return;
    }
    await handleSelectPendingRow(selectedPending);
  } catch (error) {
    logRiskPointRequestError('加载待治理记录失败', error);
    showRiskPointRequestError(error, '加载待治理记录失败');
  } finally {
    pendingLoading.value = false;
  }
};

const handleSelectPendingRow = async (pending: RiskPointPendingBindingItem) => {
  pendingPromotionForm.pendingId = pending.id;
  pendingPromotionForm.selectedMetrics = [];
  if (!isPromotablePending(pending)) {
    pendingCandidates.value = [];
    pendingHistory.value = [];
    return;
  }
  try {
    const res = await getPendingBindingCandidates(pending.id);
    if (res.code !== 200) {
      pendingCandidates.value = [];
      pendingHistory.value = [];
      return;
    }
    pendingCandidates.value = res.data.candidates || [];
    pendingHistory.value = res.data.promotionHistory || res.data.history || [];
  } catch (error) {
    logRiskPointRequestError('加载待治理候选失败', error);
    showRiskPointRequestError(error, '加载待治理候选失败');
  }
};

const isPromotablePending = (pending: RiskPointPendingBindingItem | null | undefined) => {
  const status = pending?.resolutionStatus?.trim().toUpperCase();
  return status ? PROMOTABLE_PENDING_STATUSES.has(status) : false;
};

const isPendingMetricSelected = (metricIdentifier: string) =>
  pendingPromotionForm.selectedMetrics.some((item) => item.metricIdentifier === metricIdentifier);

const togglePendingMetric = (candidate: RiskPointPendingMetricCandidate) => {
  const nextMetric = {
    riskMetricId: candidate.riskMetricId,
    metricIdentifier: candidate.metricIdentifier,
    metricName: candidate.metricName || candidate.metricIdentifier
  };
  if (isPendingMetricSelected(nextMetric.metricIdentifier)) {
    pendingPromotionForm.selectedMetrics = pendingPromotionForm.selectedMetrics.filter(
      (item) => item.metricIdentifier !== nextMetric.metricIdentifier
    );
    return;
  }
  pendingPromotionForm.selectedMetrics = [...pendingPromotionForm.selectedMetrics, nextMetric];
};

const handlePendingPromotionSubmit = async () => {
  if (!pendingPromotionForm.pendingId || pendingPromotionForm.selectedMetrics.length === 0) {
    ElMessage.warning('请至少选择一个测点');
    return;
  }
  try {
    submitLoading.value = true;
    const res = await promotePendingBinding(pendingPromotionForm.pendingId, {
      metrics: pendingPromotionForm.selectedMetrics,
      completePending: pendingPromotionForm.completePending,
      promotionNote: pendingPromotionForm.promotionNote
    });
    if (res.code === 200) {
      ElMessage.success('待治理转正成功');
      await loadPendingBindings();
      void loadRiskPointList();
    }
  } catch (error) {
    logRiskPointRequestError('提交待治理转正失败', error);
    showRiskPointRequestError(error, '提交待治理转正失败');
  } finally {
    submitLoading.value = false;
  }
};

const handleBindSubmit = async () => {
  if (!bindForm.deviceId || !bindForm.metricIdentifier) {
    ElMessage.warning('请选择设备和测点');
    return;
  }
  try {
    submitLoading.value = true;
    const selectedDevice = deviceList.value.find((device) => String(device.id) === String(bindForm.deviceId));
    const selectedMetric = metricList.value.find((metric) => metric.identifier === bindForm.metricIdentifier);
    if (!selectedDevice || !selectedMetric) {
      ElMessage.warning('请选择有效的设备和测点');
      return;
    }
    const res = await bindDevice({
      riskPointId: bindForm.riskPointId,
      deviceId: bindForm.deviceId,
      deviceCode: selectedDevice.deviceCode,
      deviceName: selectedDevice.deviceName,
      riskMetricId: selectedMetric.riskMetricId ?? undefined,
      metricIdentifier: selectedMetric.identifier,
      metricName: selectedMetric.name
    });
    if (res.code === 200) {
      ElMessage.success('绑定成功');
      bindDeviceVisible.value = false;
      void loadRiskPointList();
    }
  } catch (error) {
    logRiskPointRequestError('绑定设备失败', error);
    showRiskPointRequestError(error, '绑定设备失败');
  } finally {
    submitLoading.value = false;
  }
};

const handleFormClose = () => {
  formRef.value?.clearValidate?.();
  resetRiskPointForm();
};

const handleBindDrawerClose = () => {
  resetBindForm();
};

const handleBindingWorkbenchClose = () => {
  bindingWorkbenchRiskPoint.value = null;
  bindingWorkbenchMode.value = 'formal';
  resetPendingPromotionState();
};

const resolveGovernanceBindingRouteContext = async () => {
  const openRiskPointId = parseRouteStringQuery(route.query.openRiskPointId);
  const bindingAction = parseRouteBindingAction(route.query.bindingAction);
  if (!openRiskPointId || !bindingAction) {
    handledGovernanceBindingRouteKey.value = '';
    return;
  }
  const routeKey = `${openRiskPointId}:${bindingAction}`;
  if (handledGovernanceBindingRouteKey.value === routeKey) {
    return;
  }

  let targetRiskPoint = riskPointList.value.find((item) => getIdKey(item.id) === openRiskPointId) || null;
  if (!targetRiskPoint) {
    try {
      const response = await getRiskPointById(openRiskPointId);
      if (response.code === 200 && response.data) {
        targetRiskPoint = response.data;
      }
    } catch (error) {
      logRiskPointRequestError('治理控制面风险点上下文补数失败', error);
      return;
    }
  }

  if (!targetRiskPoint) {
    return;
  }

  handledGovernanceBindingRouteKey.value = routeKey;
  await openBindingWorkbench(targetRiskPoint, bindingAction === 'maintain-binding' ? 'formal' : 'pending');
};

watch(
  () => form.orgId,
  async () => {
    if (!formVisible.value) {
      return;
    }
    await loadResponsibleOptionsByOrganization();
  }
);

watch(
  () => form.responsibleUser,
  async (responsibleUser) => {
    if (!formVisible.value || !responsibleUser) {
      return;
    }
    const matchedUser = userOptions.value.find((item) => isSameId(item.id, responsibleUser))
      || knownUsers[getIdKey(responsibleUser)]
      || await fetchUserById(responsibleUser);
    if (!matchedUser) {
      return;
    }
    upsertKnownUser(matchedUser);
    form.responsiblePhone = matchedUser.phone || selectedOrganization.value?.phone || '';
  }
);

watch(
  () => bindForm.deviceId,
  async (deviceId) => {
    bindForm.deviceCode = '';
    bindForm.deviceName = '';
    bindForm.metricIdentifier = '';
    bindForm.metricName = '';
    metricList.value = [];
    if (!getIdKey(deviceId)) {
      return;
    }
    const selectedDevice = deviceList.value.find((device) => String(device.id) === String(deviceId));
    if (selectedDevice) {
      bindForm.deviceCode = selectedDevice.deviceCode;
      bindForm.deviceName = selectedDevice.deviceName;
    }
    await loadMetricOptions(deviceId);
  }
);

watch(
  () => bindForm.metricIdentifier,
  (metricIdentifier) => {
    const selectedMetric = metricList.value.find((metric) => metric.identifier === metricIdentifier);
    bindForm.metricName = selectedMetric?.name || '';
  }
);

watch(
  () => [route.query.openRiskPointId, route.query.bindingAction],
  () => {
    if (!parseRouteStringQuery(route.query.openRiskPointId) || !parseRouteBindingAction(route.query.bindingAction)) {
      handledGovernanceBindingRouteKey.value = '';
      return;
    }
    void resolveGovernanceBindingRouteContext();
  }
);

onMounted(async () => {
  applyRouteQueryToFilters();
  syncAppliedFilters();
  void loadOrganizationOptions();
  void loadRiskPointLevelOptions();
  await loadRiskPointList();
  await resolveGovernanceBindingRouteContext();
});
</script>

<style scoped>
.risk-point-view {
  min-width: 0;
}

.risk-point-notice-stack,
.risk-point-governance-list {
  display: grid;
  gap: 0.75rem;
}

.risk-point-governance-list {
  margin: 0;
  padding-left: 1rem;
}

.risk-point-governance-list li {
  display: grid;
  gap: 0.15rem;
  color: var(--text-secondary);
}

.risk-point-governance-list strong {
  color: var(--text-primary);
}

.risk-point-binding-workbench__summary {
  display: grid;
  gap: 0.5rem;
  color: var(--text-secondary);
}

.risk-point-binding-workbench__summary span {
  color: var(--text-primary);
}

.risk-point-binding-workbench__switcher {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 0.75rem;
}

.risk-point-binding-workbench__switch {
  border: 1px solid var(--border-color, #d0d5dd);
  background: #fff;
  color: var(--text-primary);
  border-radius: 999px;
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  cursor: pointer;
}

.risk-point-binding-workbench__switch.is-active {
  border-color: var(--brand-primary, #d97706);
  background: color-mix(in srgb, var(--brand-primary, #d97706) 12%, white);
  color: var(--brand-primary, #d97706);
}

.risk-point-pending-list,
.risk-point-pending-candidate-list,
.risk-point-pending-history {
  display: grid;
  gap: 0.75rem;
}

.risk-point-pending-list__item,
.risk-point-pending-candidate-list__item {
  width: 100%;
  border: 1px solid var(--el-border-color);
  border-radius: 1rem;
  background: #fff;
  text-align: left;
  padding: 0.9rem 1rem;
  display: grid;
  gap: 0.35rem;
  cursor: pointer;
}

.risk-point-pending-list__item.is-active,
.risk-point-pending-candidate-list__item.is-selected {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--el-color-primary) 35%, white);
}

.risk-point-pending-list__item strong,
.risk-point-pending-candidate-list__item strong {
  color: var(--text-primary);
}

.risk-point-pending-candidate-list__header,
.risk-point-pending-candidate-list__meta {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.risk-point-pending-candidate-list__summary {
  margin: 0;
  color: var(--text-secondary);
}

.risk-point-pending-history {
  margin: 0;
  padding-left: 1rem;
}

.risk-point-pending-history li {
  display: grid;
  gap: 0.2rem;
}
</style>
