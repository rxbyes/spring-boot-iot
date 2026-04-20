<template>
  <IotAccessPageShell
    title="无代码接入台"
    description="统一查看接入案例、当前步骤、阻塞原因和下一步动作，首版先收口协议治理、产品治理和合同发布卡点。"
  >
    <template #actions>
      <StandardButton action="refresh" @click="handleRefresh">刷新列表</StandardButton>
    </template>

    <section class="device-onboarding-workbench__summary">
      <article class="device-onboarding-workbench__summary-card">
        <span>接入案例</span>
        <strong>{{ pagination.total }}</strong>
      </article>
      <article class="device-onboarding-workbench__summary-card">
        <span>阻塞案例</span>
        <strong>{{ blockedCount }}</strong>
      </article>
      <article class="device-onboarding-workbench__summary-card">
        <span>处理中</span>
        <strong>{{ inProgressCount }}</strong>
      </article>
      <article class="device-onboarding-workbench__summary-card">
        <span>已就绪</span>
        <strong>{{ readyCount }}</strong>
      </article>
    </section>

    <StandardWorkbenchPanel
      title="接入案例"
      description="先在这里看当前卡在哪一步，再跳到协议治理或产品治理处理，不再分散逐页排查。"
      show-filters
      show-toolbar
      show-pagination
    >
      <template #filters>
        <StandardListFilterHeader :model="filters">
          <template #primary>
            <label class="device-onboarding-workbench__filter-field">
              <span>快速搜索</span>
              <input
                v-model="filters.keyword"
                class="device-onboarding-workbench__input"
                placeholder="搜索案例名称、案例编码、场景或设备族"
                @keyup.enter="handleSearch"
              >
            </label>
            <label class="device-onboarding-workbench__filter-field">
              <span>状态</span>
              <select v-model="filters.status" class="device-onboarding-workbench__input">
                <option value="">全部</option>
                <option value="BLOCKED">阻塞</option>
                <option value="IN_PROGRESS">处理中</option>
                <option value="READY">已就绪</option>
              </select>
            </label>
            <label class="device-onboarding-workbench__filter-field">
              <span>步骤</span>
              <select v-model="filters.currentStep" class="device-onboarding-workbench__input">
                <option value="">全部</option>
                <option value="PROTOCOL_GOVERNANCE">协议治理</option>
                <option value="PRODUCT_GOVERNANCE">产品治理</option>
                <option value="CONTRACT_RELEASE">合同发布</option>
                <option value="ACCEPTANCE">接入验收</option>
              </select>
            </label>
          </template>
          <template #actions>
            <StandardButton action="query" @click="handleSearch">查询</StandardButton>
            <StandardButton action="reset" @click="handleReset">重置</StandardButton>
          </template>
        </StandardListFilterHeader>
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[
            `当前 ${pagination.total} 项`,
            `阻塞 ${blockedCount} 项`,
            `处理中 ${inProgressCount} 项`,
            `已就绪 ${readyCount} 项`
          ]"
        >
          <template #right>
            <StandardButton link @click="resetForm">{{ editingId == null ? '清空表单' : '取消编辑' }}</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <section class="device-onboarding-workbench__grid">
        <PanelCard
          :title="editingId == null ? '新建接入案例' : '编辑接入案例'"
          description="首版先登记案例、协议信息、产品和发布批次，不做模板包或自动审批。"
        >
          <div class="device-onboarding-workbench__form-grid">
            <label class="device-onboarding-workbench__field">
              <span>案例编码</span>
              <input
                data-testid="onboarding-case-code"
                v-model="form.caseCode"
                class="device-onboarding-workbench__input"
                placeholder="例如 CASE-9101"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>案例名称</span>
              <input
                data-testid="onboarding-case-name"
                v-model="form.caseName"
                class="device-onboarding-workbench__input"
                placeholder="例如 裂缝传感器接入"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>场景编码</span>
              <input
                v-model="form.scenarioCode"
                class="device-onboarding-workbench__input"
                placeholder="例如 phase1-crack"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>设备族</span>
              <input
                v-model="form.deviceFamily"
                class="device-onboarding-workbench__input"
                placeholder="例如 crack_sensor"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>协议族编码</span>
              <input
                v-model="form.protocolFamilyCode"
                class="device-onboarding-workbench__input"
                placeholder="例如 legacy-dp-crack"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>解密档案编码</span>
              <input
                v-model="form.decryptProfileCode"
                class="device-onboarding-workbench__input"
                placeholder="例如 aes-62000002"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>协议模板编码</span>
              <input
                v-model="form.protocolTemplateCode"
                class="device-onboarding-workbench__input"
                placeholder="例如 nf-crack-v1"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>产品 ID</span>
              <input
                v-model="form.productId"
                class="device-onboarding-workbench__input"
                placeholder="可选，填正式产品 ID"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>发布批次 ID</span>
              <input
                v-model="form.releaseBatchId"
                class="device-onboarding-workbench__input"
                placeholder="可选，填正式发布批次 ID"
              >
            </label>
            <label class="device-onboarding-workbench__field device-onboarding-workbench__field--wide">
              <span>备注</span>
              <textarea
                v-model="form.remark"
                class="device-onboarding-workbench__input device-onboarding-workbench__textarea"
                placeholder="记录当前厂商、样本或联调备注"
              />
            </label>
          </div>
          <div class="device-onboarding-workbench__form-actions">
            <StandardButton
              data-testid="onboarding-save"
              :disabled="saving"
              @click="handleSave"
            >
              {{ saving ? '保存中...' : (editingId == null ? '创建案例' : '保存修改') }}
            </StandardButton>
            <StandardButton action="reset" @click="resetForm">重置</StandardButton>
          </div>
        </PanelCard>

        <PanelCard
          title="流程判断"
          description="首版状态推导规则固定为：协议信息缺失 -> 产品缺失 -> 发布批次缺失 -> 进入验收。"
        >
          <ul class="device-onboarding-workbench__rules">
            <li>缺协议族、解密档案或协议模板时，阻塞在“协议治理”。</li>
            <li>协议信息齐全但未绑定产品时，阻塞在“产品治理”。</li>
            <li>已绑定产品但未发布正式合同批次时，停留在“合同发布”。</li>
            <li>发布批次已存在时，标记为“已就绪”，等待接入验收链接入。</li>
          </ul>
        </PanelCard>
      </section>

      <div v-if="loading && rows.length === 0" class="device-onboarding-workbench__empty">
        <strong>正在加载接入案例...</strong>
      </div>
      <div v-else-if="rows.length" class="device-onboarding-workbench__list">
        <article
          v-for="row in rows"
          :key="String(row.id)"
          class="device-onboarding-workbench__item"
        >
          <header class="device-onboarding-workbench__item-head">
            <div class="device-onboarding-workbench__item-title">
              <strong>{{ row.caseName }}</strong>
              <span>{{ row.caseCode }}</span>
            </div>
            <div class="device-onboarding-workbench__item-status">
              <span class="device-onboarding-workbench__badge">{{ statusLabel(row.status) }}</span>
              <span class="device-onboarding-workbench__step">{{ stepLabel(row.currentStep) }}</span>
            </div>
          </header>

          <p class="device-onboarding-workbench__item-copy">
            {{ primaryBlocker(row) }}
          </p>

          <dl class="device-onboarding-workbench__meta">
            <div>
              <dt>场景</dt>
              <dd>{{ row.scenarioCode || '--' }}</dd>
            </div>
            <div>
              <dt>设备族</dt>
              <dd>{{ row.deviceFamily || '--' }}</dd>
            </div>
            <div>
              <dt>协议族</dt>
              <dd>{{ row.protocolFamilyCode || '--' }}</dd>
            </div>
            <div>
              <dt>产品</dt>
              <dd>{{ row.productId ?? '--' }}</dd>
            </div>
          </dl>

          <div class="device-onboarding-workbench__actions">
            <StandardButton @click="handleEdit(row)">编辑</StandardButton>
            <StandardButton
              :data-testid="`onboarding-refresh-${row.id}`"
              :disabled="refreshingId === String(row.id)"
              @click="handleRefreshRow(row)"
            >
              {{ refreshingId === String(row.id) ? '刷新中...' : '刷新状态' }}
            </StandardButton>
            <StandardButton
              :data-testid="`onboarding-next-${row.id}`"
              :disabled="row.currentStep === 'ACCEPTANCE'"
              @click="handleNext(row)"
            >
              {{ nextActionLabel(row) }}
            </StandardButton>
          </div>
        </article>
      </div>
      <div v-else class="device-onboarding-workbench__empty">
        <strong>当前还没有接入案例</strong>
        <span>先创建案例，再根据当前步骤跳到协议治理或产品治理处理。</span>
      </div>

      <template #pagination>
        <StandardPagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </template>
    </StandardWorkbenchPanel>
  </IotAccessPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  createDeviceOnboardingCase,
  pageDeviceOnboardingCases,
  refreshDeviceOnboardingCaseStatus,
  updateDeviceOnboardingCase
} from '@/api/deviceOnboarding'
import { resolveRequestErrorMessage } from '@/api/request'
import PanelCard from '@/components/PanelCard.vue'
import StandardButton from '@/components/StandardButton.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import IotAccessPageShell from '@/components/iotAccess/IotAccessPageShell.vue'
import type {
  DeviceOnboardingCase,
  DeviceOnboardingCaseCreatePayload,
  DeviceOnboardingCasePageQuery,
  DeviceOnboardingCaseUpdatePayload
} from '@/types/api'
import { ElMessage } from '@/utils/message'
import { buildProductWorkbenchSectionPath } from '@/utils/productWorkbenchRoutes'

const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const refreshingId = ref('')
const rows = ref<DeviceOnboardingCase[]>([])
const editingId = ref<string | number | null>(null)

const pagination = reactive({
  total: 0,
  pageNum: 1,
  pageSize: 10
})

const filters = reactive<DeviceOnboardingCasePageQuery>({
  keyword: '',
  status: '',
  currentStep: '',
  pageNum: 1,
  pageSize: 10
})

const form = reactive({
  caseCode: '',
  caseName: '',
  scenarioCode: '',
  deviceFamily: '',
  protocolFamilyCode: '',
  decryptProfileCode: '',
  protocolTemplateCode: '',
  productId: '',
  releaseBatchId: '',
  remark: ''
})

const blockedCount = computed(() => rows.value.filter((row) => row.status === 'BLOCKED').length)
const inProgressCount = computed(() => rows.value.filter((row) => row.status === 'IN_PROGRESS').length)
const readyCount = computed(() => rows.value.filter((row) => row.status === 'READY').length)

onMounted(() => {
  void loadCases()
})

async function loadCases(): Promise<void> {
  loading.value = true
  try {
    const response = await pageDeviceOnboardingCases({
      ...filters,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    rows.value = response.data.records || []
    pagination.total = response.data.total || 0
    pagination.pageNum = response.data.pageNum || pagination.pageNum
    pagination.pageSize = response.data.pageSize || pagination.pageSize
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '加载接入案例失败'))
  } finally {
    loading.value = false
  }
}

function resetForm(): void {
  editingId.value = null
  form.caseCode = ''
  form.caseName = ''
  form.scenarioCode = ''
  form.deviceFamily = ''
  form.protocolFamilyCode = ''
  form.decryptProfileCode = ''
  form.protocolTemplateCode = ''
  form.productId = ''
  form.releaseBatchId = ''
  form.remark = ''
}

function handleEdit(row: DeviceOnboardingCase): void {
  editingId.value = row.id
  form.caseCode = row.caseCode || ''
  form.caseName = row.caseName || ''
  form.scenarioCode = row.scenarioCode || ''
  form.deviceFamily = row.deviceFamily || ''
  form.protocolFamilyCode = row.protocolFamilyCode || ''
  form.decryptProfileCode = row.decryptProfileCode || ''
  form.protocolTemplateCode = row.protocolTemplateCode || ''
  form.productId = row.productId == null ? '' : String(row.productId)
  form.releaseBatchId = row.releaseBatchId == null ? '' : String(row.releaseBatchId)
  form.remark = row.remark || ''
}

async function handleSave(): Promise<void> {
  saving.value = true
  try {
    if (editingId.value == null) {
      await createDeviceOnboardingCase(buildPayload())
      ElMessage.success('接入案例已创建')
    } else {
      await updateDeviceOnboardingCase(editingId.value, buildPayload())
      ElMessage.success('接入案例已更新')
    }
    resetForm()
    await loadCases()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '保存接入案例失败'))
  } finally {
    saving.value = false
  }
}

async function handleRefresh(): Promise<void> {
  await loadCases()
}

async function handleRefreshRow(row: DeviceOnboardingCase): Promise<void> {
  refreshingId.value = String(row.id)
  try {
    await refreshDeviceOnboardingCaseStatus(row.id)
    ElMessage.success('接入案例状态已刷新')
    await loadCases()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '刷新接入案例状态失败'))
  } finally {
    refreshingId.value = ''
  }
}

async function handleSearch(): Promise<void> {
  pagination.pageNum = 1
  await loadCases()
}

async function handleReset(): Promise<void> {
  filters.keyword = ''
  filters.status = ''
  filters.currentStep = ''
  pagination.pageNum = 1
  pagination.pageSize = 10
  await loadCases()
}

async function handlePageChange(pageNum: number): Promise<void> {
  pagination.pageNum = pageNum
  await loadCases()
}

async function handleSizeChange(pageSize: number): Promise<void> {
  pagination.pageSize = pageSize
  pagination.pageNum = 1
  await loadCases()
}

function handleNext(row: DeviceOnboardingCase): void {
  if (row.currentStep === 'PROTOCOL_GOVERNANCE') {
    void router.push({ path: '/protocol-governance' })
    return
  }
  if (row.currentStep === 'PRODUCT_GOVERNANCE' || row.currentStep === 'CONTRACT_RELEASE') {
    void router.push(
      row.productId == null
        ? '/products'
        : buildProductWorkbenchSectionPath(row.productId, 'contracts')
    )
  }
}

function primaryBlocker(row: DeviceOnboardingCase): string {
  return row.blockers?.[0] || '已具备进入验收条件'
}

function stepLabel(step: DeviceOnboardingCase['currentStep']): string {
  if (step === 'PROTOCOL_GOVERNANCE') {
    return '协议治理'
  }
  if (step === 'PRODUCT_GOVERNANCE') {
    return '产品治理'
  }
  if (step === 'CONTRACT_RELEASE') {
    return '合同发布'
  }
  return '接入验收'
}

function statusLabel(status: DeviceOnboardingCase['status']): string {
  if (status === 'BLOCKED') {
    return '阻塞'
  }
  if (status === 'IN_PROGRESS') {
    return '处理中'
  }
  return '已就绪'
}

function nextActionLabel(row: DeviceOnboardingCase): string {
  if (row.currentStep === 'PROTOCOL_GOVERNANCE') {
    return '前往协议治理'
  }
  if (row.currentStep === 'PRODUCT_GOVERNANCE' || row.currentStep === 'CONTRACT_RELEASE') {
    return '前往产品治理'
  }
  return '已具备验收条件'
}

function buildPayload(): DeviceOnboardingCaseCreatePayload | DeviceOnboardingCaseUpdatePayload {
  return {
    tenantId: 1,
    caseCode: form.caseCode,
    caseName: form.caseName,
    scenarioCode: normalizeOptionalText(form.scenarioCode),
    deviceFamily: normalizeOptionalText(form.deviceFamily),
    protocolFamilyCode: normalizeOptionalText(form.protocolFamilyCode),
    decryptProfileCode: normalizeOptionalText(form.decryptProfileCode),
    protocolTemplateCode: normalizeOptionalText(form.protocolTemplateCode),
    productId: normalizeOptionalNumber(form.productId),
    releaseBatchId: normalizeOptionalNumber(form.releaseBatchId),
    remark: normalizeOptionalText(form.remark)
  }
}

function normalizeOptionalText(value: string): string | null {
  const normalized = value.trim()
  return normalized ? normalized : null
}

function normalizeOptionalNumber(value: string): number | null {
  const normalized = value.trim()
  if (!normalized) {
    return null
  }
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : null
}
</script>

<style scoped>
.device-onboarding-workbench__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.9rem;
  margin-bottom: 0.9rem;
}

.device-onboarding-workbench__summary-card {
  display: grid;
  gap: 0.34rem;
  padding: 0.95rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-2xl);
  background: var(--bg-card);
  box-shadow: var(--shadow-card-soft);
}

.device-onboarding-workbench__summary-card span {
  color: var(--text-caption);
  font-size: 12px;
}

.device-onboarding-workbench__summary-card strong {
  color: var(--text-heading);
  font-size: 1.45rem;
}

.device-onboarding-workbench__filter-field,
.device-onboarding-workbench__field {
  display: grid;
  gap: 0.35rem;
  min-width: 0;
}

.device-onboarding-workbench__filter-field span,
.device-onboarding-workbench__field span {
  color: var(--text-caption);
  font-size: 12px;
}

.device-onboarding-workbench__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr);
  gap: 0.9rem;
  margin-bottom: 0.95rem;
}

.device-onboarding-workbench__form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

.device-onboarding-workbench__field--wide {
  grid-column: 1 / -1;
}

.device-onboarding-workbench__input {
  width: 100%;
  min-height: 2.5rem;
  padding: 0.7rem 0.8rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  color: var(--text-primary);
  box-sizing: border-box;
}

.device-onboarding-workbench__textarea {
  min-height: 5.6rem;
  resize: vertical;
}

.device-onboarding-workbench__form-actions {
  display: flex;
  gap: 0.7rem;
  margin-top: 0.9rem;
}

.device-onboarding-workbench__rules {
  margin: 0;
  padding-left: 1.1rem;
  color: var(--text-secondary);
  line-height: 1.8;
}

.device-onboarding-workbench__list {
  display: grid;
  gap: 0.9rem;
}

.device-onboarding-workbench__item {
  display: grid;
  gap: 0.75rem;
  padding: 1rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-2xl);
  background: var(--bg-card);
  box-shadow: var(--shadow-card-soft);
}

.device-onboarding-workbench__item-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.8rem;
}

.device-onboarding-workbench__item-title {
  display: grid;
  gap: 0.2rem;
}

.device-onboarding-workbench__item-title strong {
  color: var(--text-heading);
  font-size: 1rem;
}

.device-onboarding-workbench__item-title span {
  color: var(--text-caption);
  font-size: 12px;
}

.device-onboarding-workbench__item-status {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  justify-content: flex-end;
}

.device-onboarding-workbench__badge,
.device-onboarding-workbench__step {
  display: inline-flex;
  align-items: center;
  padding: 0.3rem 0.7rem;
  border-radius: var(--radius-pill);
  font-size: 12px;
}

.device-onboarding-workbench__badge {
  background: color-mix(in srgb, var(--warning) 12%, white);
  color: var(--text-heading);
}

.device-onboarding-workbench__step {
  background: color-mix(in srgb, var(--brand) 10%, white);
  color: var(--brand);
}

.device-onboarding-workbench__item-copy {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.device-onboarding-workbench__meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.7rem;
  margin: 0;
}

.device-onboarding-workbench__meta div {
  display: grid;
  gap: 0.24rem;
}

.device-onboarding-workbench__meta dt {
  color: var(--text-caption);
  font-size: 12px;
}

.device-onboarding-workbench__meta dd {
  margin: 0;
  color: var(--text-primary);
}

.device-onboarding-workbench__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.device-onboarding-workbench__empty {
  display: grid;
  gap: 0.4rem;
  padding: 1rem;
  border: 1px dashed var(--panel-border);
  border-radius: var(--radius-2xl);
  color: var(--text-secondary);
}

@media (max-width: 1100px) {
  .device-onboarding-workbench__summary,
  .device-onboarding-workbench__meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .device-onboarding-workbench__grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 720px) {
  .device-onboarding-workbench__summary,
  .device-onboarding-workbench__form-grid,
  .device-onboarding-workbench__meta {
    grid-template-columns: minmax(0, 1fr);
  }

  .device-onboarding-workbench__item-head {
    flex-direction: column;
  }

  .device-onboarding-workbench__item-status {
    justify-content: flex-start;
  }
}
</style>
