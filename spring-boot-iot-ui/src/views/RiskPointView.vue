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
              <el-input v-model="filters.riskPointCode" placeholder="风险点编号" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.riskLevel" placeholder="风险等级" clearable>
                <el-option
                  v-for="option in riskLevelOptions"
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
          :meta-items="[`已选 ${selectedRows.length} 项`, `启用 ${enabledCount} 项`, `红色 ${redCount} 项`, `待纳管 ${missingBindingTotal} 台`, `停用 ${disabledCount} 项`]"
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
            <StandardTableTextColumn prop="riskPointName" label="风险点名称" :min-width="180" />
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
            <el-table-column prop="riskLevel" label="风险等级" width="100">
              <template #default="{ row }">
                <el-tag :type="getRiskLevelType(row.riskLevel)" round>{{ getRiskLevelText(row.riskLevel) }}</el-tag>
              </template>
            </el-table-column>
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
          <span>{{ form.id ? '历史编号仅用于留档追踪；请同步核对组织、区域、负责人和等级信息。' : '保存后将自动生成系统编号；请先确认组织、区域和风险等级，再补齐负责人信息。' }}</span>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="ops-drawer-form">
          <section class="ops-drawer-section">
            <div class="ops-drawer-section__header">
              <div>
                <h3>基础信息</h3>
                <p>维护风险点主档、所属组织、所属区域与风险等级，为后续监测、处置与空间治理提供统一标识。</p>
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
                  node-key="id"
                  check-strictly
                  clearable
                  :props="{ label: 'regionName', children: 'children', value: 'id' }"
                  placeholder="请选择所属区域"
                />
              </el-form-item>
              <el-form-item label="风险等级" prop="riskLevel">
                <el-select v-model="form.riskLevel" placeholder="请选择风险等级">
                  <el-option
                    v-for="option in riskLevelOptions"
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
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
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
import { getDictByCode } from '@/api/dict';
import { listMissingBindings, type RiskGovernanceGapItem } from '@/api/riskGovernance';
import { listOrganizationTree } from '@/api/organization';
import type { Organization } from '@/api/organization';
import { listRegionTree } from '@/api/region';
import type { Region } from '@/api/region';
import { getUser } from '@/api/user';
import type { User } from '@/api/user';
import { listDeviceOptions, getDeviceMetricOptions } from '@/api/iot';
import type { DeviceMetricOption, DeviceOption } from '@/types/api';
import { resolveWorkbenchActionColumnWidth } from '@/utils/adaptiveActionColumn';
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm';
import { buildRiskLevelOptions, getRiskLevelTagType, getRiskLevelText as resolveRiskLevelText, type RiskLevelOption } from '@/utils/riskLevel';
import { pageRiskPointList, addRiskPoint, updateRiskPoint, deleteRiskPoint, bindDevice } from '../api/riskPoint';
import type { RiskPoint } from '../api/riskPoint';
import { formatDateTime } from '@/utils/format';

type RiskPointRowActionCommand = 'edit' | 'bind-device' | 'delete';

const loading = ref(false);
const formVisible = ref(false);
const bindDeviceVisible = ref(false);
const riskPointList = ref<RiskPoint[]>([]);
const organizationOptions = ref<Organization[]>([]);
const regionOptions = ref<Region[]>([]);
const userOptions = ref<User[]>([]);
const userOptionsLoading = ref(false);
const riskLevelOptions = ref<RiskLevelOption[]>([]);
const deviceList = ref<DeviceOption[]>([]);
const metricList = ref<DeviceMetricOption[]>([]);
const missingBindingItems = ref<RiskGovernanceGapItem[]>([]);
const tableRef = ref();
const selectedRows = ref<RiskPoint[]>([]);
const riskPointActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: [
    { command: 'edit', label: '编辑' },
    { command: 'bind-device', label: '绑定设备' },
    { command: 'delete', label: '删除' }
  ],
});

const filters = reactive({
  riskPointCode: '',
  riskLevel: '',
  status: '' as '' | number
});
const appliedFilters = reactive({
  riskPointCode: '',
  riskLevel: '',
  status: '' as '' | number
});

const { pagination, applyPageResult, resetPage, setPageSize, setPageNum } = useServerPagination();

const formRef = ref();
const formTitle = computed(() => (form.id ? '编辑风险点' : '新增风险点'));
const form = reactive({
  id: undefined as number | undefined,
  riskPointCode: '',
  riskPointName: '',
  orgId: '' as '' | number,
  orgName: '',
  regionId: '' as '' | number,
  regionName: '',
  responsibleUser: '' as '' | number,
  responsiblePhone: '',
  riskLevel: '',
  description: '',
  status: 0,
  createBy: undefined as number | undefined,
  updateBy: undefined as number | undefined
});

const rules = {
  riskPointName: [{ required: true, message: '请输入风险点名称', trigger: 'blur' }],
  orgId: [{ required: true, message: '请选择所属组织', trigger: 'change' }],
  regionId: [{ required: true, message: '请选择所属区域', trigger: 'change' }],
  riskLevel: [{ required: true, message: '请选择风险等级', trigger: 'change' }]
};

const bindForm = reactive({
  riskPointId: 0,
  riskPointName: '',
  deviceId: 0,
  deviceCode: '',
  deviceName: '',
  metricIdentifier: '',
  metricName: ''
});
const submitLoading = ref(false);
const riskPointAdvice = '优先核查高风险且已启用的风险点';
const missingBindingTotal = ref(0);
const knownUsers = reactive<Record<number, User>>({});
let latestListRequestId = 0;

const enabledCount = computed(() => riskPointList.value.filter((item) => item.status === 0).length);
const redCount = computed(() => riskPointList.value.filter((item) => item.riskLevel === 'red').length);
const disabledCount = computed(() => riskPointList.value.filter((item) => item.status === 1).length);
const hasRecords = computed(() => riskPointList.value.length > 0);
const showListSkeleton = computed(() => loading.value && !hasRecords.value);
const emptyStateTitle = computed(() => (hasAppliedFilters.value ? '没有符合条件的风险点' : '还没有风险对象'));
const emptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? '已生效筛选暂时没有匹配结果，可以调整条件，或者直接清空当前筛选。'
    : '当前还没有风险对象记录，先新增风险点，再继续设备绑定和策略治理。'
);
const selectedOrganization = computed(() =>
  form.orgId === '' ? null : findOrganizationById(organizationOptions.value, Number(form.orgId))
);
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

const loadOrganizationOptions = async () => {
  try {
    const res = await listOrganizationTree();
    if (res.code === 200) {
      organizationOptions.value = (res.data || []).filter((item) => item.status === 1);
    }
  } catch (error) {
    console.error('加载组织树失败', error);
    ElMessage.error(error instanceof Error ? error.message : '加载组织树失败');
  }
};

const findOrganizationById = (nodes: Organization[], targetId: number): Organization | null => {
  for (const node of nodes) {
    if (Number(node.id) === targetId) {
      return node;
    }
    const childMatch = node.children?.length ? findOrganizationById(node.children, targetId) : null;
    if (childMatch) {
      return childMatch;
    }
  }
  return null;
};

const findRegionById = (nodes: Region[], targetId: number): Region | null => {
  for (const node of nodes) {
    if (Number(node.id) === targetId) {
      return node;
    }
    const childMatch = node.children?.length ? findRegionById(node.children, targetId) : null;
    if (childMatch) {
      return childMatch;
    }
  }
  return null;
};

const loadRegionOptions = async () => {
  try {
    const res = await listRegionTree();
    if (res.code === 200) {
      regionOptions.value = (res.data || []).filter((item) => item.status === 1);
    }
  } catch (error) {
    console.error('加载区域树失败', error);
    ElMessage.error(error instanceof Error ? error.message : '加载区域树失败');
  }
};

const upsertKnownUser = (user?: User | null) => {
  if (!user?.id) {
    return;
  }
  knownUsers[Number(user.id)] = user;
};

const setResponsibleUserOptions = (users: Array<User | null | undefined>) => {
  const seenIds = new Set<number>();
  userOptions.value = users.filter((user): user is User => {
    if (!user?.id) {
      return false;
    }
    const userId = Number(user.id);
    if (seenIds.has(userId)) {
      return false;
    }
    seenIds.add(userId);
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

const fetchUserById = async (userId?: number) => {
  if (!userId) {
    return null;
  }
  const cachedUser = knownUsers[userId];
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
    console.error('加载用户详情失败', error);
  }
  return null;
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
    const leaderUserId = organization.leaderUserId ? Number(organization.leaderUserId) : undefined;
    let leaderUser = await fetchUserById(leaderUserId);
    if (!leaderUser) {
      leaderUser = buildOrganizationLeaderFallback(organization);
    }
    if (leaderUser) {
      nextUsers.push(leaderUser);
    }
    if (preserveSelection && form.responsibleUser) {
      const currentUserId = Number(form.responsibleUser);
      if (!leaderUser || Number(leaderUser.id) !== currentUserId) {
        const currentUser = await fetchUserById(currentUserId);
        if (currentUser) {
          nextUsers.push(currentUser);
        }
      }
    }
    setResponsibleUserOptions(nextUsers);

    if (preserveSelection && form.responsibleUser) {
      const selectedUserId = Number(form.responsibleUser);
      if (userOptions.value.some((user) => Number(user.id) === selectedUserId)) {
        return;
      }
    }

    if (leaderUser?.id) {
      form.responsibleUser = Number(leaderUser.id);
      form.responsiblePhone = leaderUser.phone || organization.phone || '';
      return;
    }

    form.responsibleUser = '';
    form.responsiblePhone = organization.phone || '';
  } finally {
    userOptionsLoading.value = false;
  }
};

const loadRiskLevelOptions = async () => {
  try {
    const res = await getDictByCode('risk_level');
    if (res.code === 200) {
      riskLevelOptions.value = buildRiskLevelOptions(res.data?.items || []);
      if (!form.id && !form.riskLevel) {
        form.riskLevel = riskLevelOptions.value[0]?.value || '';
      }
    }
  } catch (error) {
    console.error('加载风险等级字典失败', error);
    ElMessage.error(error instanceof Error ? error.message : '加载风险等级字典失败');
  }
};

const loadDeviceOptions = async () => {
  try {
    const res = await listDeviceOptions();
    if (res.code === 200) {
      deviceList.value = res.data || [];
    }
  } catch (error) {
    console.error('加载设备选项失败', error);
    ElMessage.error('加载设备列表失败');
  }
};

const loadMetricOptions = async (deviceId: string | number) => {
  try {
    const res = await getDeviceMetricOptions(deviceId);
    if (res.code === 200) {
      metricList.value = res.data || [];
    }
  } catch (error) {
    console.error('加载测点选项失败', error);
    ElMessage.error('加载测点列表失败');
  }
};

const getRiskLevelType = (level: string) => getRiskLevelTagType(level);

const getRiskLevelText = (level: string) => resolveRiskLevelText(level, riskLevelOptions.value);

const getResponsibleUserText = (row: Partial<RiskPoint>) => {
  if (!row.responsibleUser) {
    return '未指定负责人';
  }
  if (row.responsibleUserName) {
    return row.responsibleUserName;
  }
  const matchedUser = knownUsers[Number(row.responsibleUser)];
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
    { key: 'riskPointCode', label: '风险点编号' },
    { key: 'riskLevel', label: (value) => `风险等级：${getRiskLevelText(String(value || ''))}` },
    { key: 'status', label: (value) => `状态：${getStatusText(Number(value))}`, clearValue: '' as '' | number }
  ],
  defaults: {
    riskPointCode: '',
    riskLevel: '',
    status: '' as '' | number
  }
});

const loadRiskPointList = async () => {
  const requestId = ++latestListRequestId;
  loading.value = true;
  try {
    const [listResult, backlogResult] = await Promise.allSettled([
      pageRiskPointList({
        riskPointCode: appliedFilters.riskPointCode || undefined,
        riskLevel: appliedFilters.riskLevel || undefined,
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

    if (listResult.status === 'fulfilled' && listResult.value.code === 200) {
      riskPointList.value = applyPageResult(listResult.value.data);
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
    }

    if (backlogResult.status === 'fulfilled' && backlogResult.value.code === 200) {
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
    console.error('查询风险点列表失败', error);
    ElMessage.error(error instanceof Error ? error.message : '查询风险点列表失败');
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
  filters.riskPointCode = '';
  filters.riskLevel = '';
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
  { command: 'edit' as const, label: '编辑' },
  { command: 'bind-device' as const, label: '绑定设备' },
  { command: 'delete' as const, label: '删除' }
];

const handleRiskPointRowAction = (command: RiskPointRowActionCommand, row: RiskPoint) => {
  if (command === 'edit') {
    handleEdit(row);
    return;
  }
  if (command === 'bind-device') {
    handleBindDevice(row);
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

const handleRemoveAppliedFilter = (key: string) => {
  removeAppliedFilter(key);
  resetPage();
  clearSelection();
  void loadRiskPointList();
};

const handleClearAppliedFilters = () => {
  handleReset();
};

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
  form.riskLevel = riskLevelOptions.value[0]?.value || '';
  form.description = '';
  form.status = 0;
  form.createBy = undefined;
  form.updateBy = undefined;
  userOptions.value = [];
};

const resetBindForm = () => {
  bindForm.riskPointId = 0;
  bindForm.riskPointName = '';
  bindForm.deviceId = 0;
  bindForm.deviceCode = '';
  bindForm.deviceName = '';
  bindForm.metricIdentifier = '';
  bindForm.metricName = '';
  metricList.value = [];
};

const handleAdd = () => {
  resetRiskPointForm();
  formVisible.value = true;
};

const handleEdit = async (row: RiskPoint) => {
  form.id = row.id;
  form.riskPointCode = row.riskPointCode;
  form.riskPointName = row.riskPointName;
  form.orgId = row.orgId ? Number(row.orgId) : '';
  form.orgName = row.orgName || '';
  form.regionId = row.regionId ? Number(row.regionId) : '';
  form.regionName = row.regionName || '';
  form.responsibleUser = row.responsibleUser ? Number(row.responsibleUser) : '';
  form.responsiblePhone = row.responsiblePhone;
  form.riskLevel = row.riskLevel;
  form.description = row.description || '';
  form.status = row.status;
  form.createBy = row.createBy;
  form.updateBy = row.updateBy;
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
    console.error('删除风险点失败', error);
    ElMessage.error(error instanceof Error ? error.message : '删除风险点失败');
  }
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    submitLoading.value = true;
    const selectedOrganization = findOrganizationById(organizationOptions.value, Number(form.orgId));
    const selectedRegion = findRegionById(regionOptions.value, Number(form.regionId));
    form.orgName = selectedOrganization?.orgName || '';
    form.regionName = selectedRegion?.regionName || '';
    const payload = {
      ...form,
      orgId: form.orgId === '' ? undefined : Number(form.orgId),
      regionId: form.regionId === '' ? undefined : Number(form.regionId),
      responsibleUser: form.responsibleUser === '' ? undefined : Number(form.responsibleUser)
    };
    const res = form.id ? await updateRiskPoint(payload) : await addRiskPoint(payload);
    if (res.code === 200) {
      ElMessage.success(form.id ? '更新成功' : '新增成功');
      formVisible.value = false;
      void loadRiskPointList();
    }
  } catch (error) {
    console.error('提交表单失败', error);
    ElMessage.error(error instanceof Error ? error.message : '提交风险点失败');
  } finally {
    submitLoading.value = false;
  }
};

const handleBindDevice = async (row: RiskPoint) => {
  resetBindForm();
  bindForm.riskPointId = Number(row.id);
  bindForm.riskPointName = row.riskPointName;
  await loadDeviceOptions();
  bindDeviceVisible.value = true;
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
      metricIdentifier: selectedMetric.identifier,
      metricName: selectedMetric.name
    });
    if (res.code === 200) {
      ElMessage.success('绑定成功');
      bindDeviceVisible.value = false;
      void loadRiskPointList();
    }
  } catch (error) {
    console.error('绑定设备失败', error);
    ElMessage.error(error instanceof Error ? error.message : '绑定设备失败');
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
    const userId = Number(responsibleUser);
    const matchedUser = userOptions.value.find((item) => Number(item.id) === userId)
      || knownUsers[userId]
      || await fetchUserById(userId);
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
    if (!deviceId) {
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

onMounted(() => {
  syncAppliedFilters();
  void loadOrganizationOptions();
  void loadRegionOptions();
  void loadRiskLevelOptions();
  void loadRiskPointList();
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
</style>
