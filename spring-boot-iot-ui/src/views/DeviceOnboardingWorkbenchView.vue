<template>
  <IotAccessPageShell
    title="无代码接入台"
    description="统一查看接入案例、当前步骤、阻塞原因和下一步动作，首版继续收口协议治理、产品治理、合同发布和标准接入验收。"
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
          description="首版先登记案例、模板包、协议信息、产品、验收设备和发布批次，不做模板包审批或自动审批。"
        >
          <div class="device-onboarding-workbench__form-grid">
            <label class="device-onboarding-workbench__field device-onboarding-workbench__field--wide">
              <span>模板包</span>
              <div class="device-onboarding-workbench__template-apply">
                <select
                  v-model="form.templatePackId"
                  data-testid="template-pack-select"
                  class="device-onboarding-workbench__input"
                >
                  <option value="">不使用模板包</option>
                  <option
                    v-for="pack in templateRows"
                    :key="String(pack.id)"
                    :value="String(pack.id)"
                  >
                    {{ pack.packName }} / {{ pack.packCode }}
                  </option>
                </select>
                <StandardButton
                  v-permission="'iot:device-onboarding:template-pack'"
                  data-testid="apply-template-pack"
                  :disabled="!form.templatePackId"
                  @click="handleApplySelectedTemplatePack"
                >
                  应用模板
                </StandardButton>
              </div>
            </label>
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
                data-testid="onboarding-scenario-code"
                v-model="form.scenarioCode"
                class="device-onboarding-workbench__input"
                placeholder="例如 phase1-crack"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>设备族</span>
              <input
                data-testid="onboarding-device-family"
                v-model="form.deviceFamily"
                class="device-onboarding-workbench__input"
                placeholder="例如 crack_sensor"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>协议族编码</span>
              <input
                data-testid="onboarding-protocol-family-code"
                v-model="form.protocolFamilyCode"
                class="device-onboarding-workbench__input"
                placeholder="例如 legacy-dp-crack"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>解密档案编码</span>
              <input
                data-testid="onboarding-decrypt-profile-code"
                v-model="form.decryptProfileCode"
                class="device-onboarding-workbench__input"
                placeholder="例如 aes-62000002"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>协议模板编码</span>
              <input
                data-testid="onboarding-protocol-template-code"
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
            <label class="device-onboarding-workbench__field">
              <span>验收设备编码</span>
              <input
                data-testid="onboarding-device-code"
                v-model="form.deviceCode"
                class="device-onboarding-workbench__input"
                placeholder="例如 DEV-9101"
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
              v-permission="editingId == null ? 'iot:device-onboarding:create-case' : 'iot:device-onboarding:update-case'"
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
          :title="templateEditingId == null ? '模板包' : '编辑模板包'"
          description="模板包只保存已发布治理资产的组合引用，当前用于新建接入案例一键预填。"
        >
          <div class="device-onboarding-workbench__form-grid">
            <label class="device-onboarding-workbench__field">
              <span>模板包编码</span>
              <input
                data-testid="template-pack-code"
                v-model="templateForm.packCode"
                class="device-onboarding-workbench__input"
                placeholder="例如 PACK-CRACK-V1"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>模板包名称</span>
              <input
                data-testid="template-pack-name"
                v-model="templateForm.packName"
                class="device-onboarding-workbench__input"
                placeholder="例如 裂缝模板包"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>场景编码</span>
              <input
                data-testid="template-pack-scenario-code"
                v-model="templateForm.scenarioCode"
                class="device-onboarding-workbench__input"
                placeholder="例如 phase1-crack"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>设备族</span>
              <input
                data-testid="template-pack-device-family"
                v-model="templateForm.deviceFamily"
                class="device-onboarding-workbench__input"
                placeholder="例如 crack_sensor"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>协议族编码</span>
              <input
                data-testid="template-pack-protocol-family-code"
                v-model="templateForm.protocolFamilyCode"
                class="device-onboarding-workbench__input"
                placeholder="例如 legacy-dp-crack"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>解密档案编码</span>
              <input
                data-testid="template-pack-decrypt-profile-code"
                v-model="templateForm.decryptProfileCode"
                class="device-onboarding-workbench__input"
                placeholder="例如 aes-62000002"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>协议模板编码</span>
              <input
                data-testid="template-pack-protocol-template-code"
                v-model="templateForm.protocolTemplateCode"
                class="device-onboarding-workbench__input"
                placeholder="例如 nf-crack-v1"
              >
            </label>
            <label class="device-onboarding-workbench__field">
              <span>状态</span>
              <select v-model="templateForm.status" class="device-onboarding-workbench__input">
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
              </select>
            </label>
            <label class="device-onboarding-workbench__field device-onboarding-workbench__field--wide">
              <span>描述</span>
              <textarea
                v-model="templateForm.description"
                class="device-onboarding-workbench__input device-onboarding-workbench__textarea"
                placeholder="记录模板包覆盖的设备场景和资产边界"
              />
            </label>
          </div>
          <div class="device-onboarding-workbench__form-actions">
            <StandardButton
              v-permission="'iot:device-onboarding:template-pack'"
              data-testid="template-pack-save"
              :disabled="templateSaving"
              @click="handleSaveTemplatePack"
            >
              {{ templateSaving ? '保存中...' : (templateEditingId == null ? '创建模板包' : '保存模板包') }}
            </StandardButton>
            <StandardButton action="reset" @click="resetTemplateForm">重置模板包</StandardButton>
          </div>
          <div v-if="templateRows.length" class="device-onboarding-workbench__template-list">
            <article
              v-for="pack in templateRows"
              :key="String(pack.id)"
              class="device-onboarding-workbench__template-item"
            >
              <div class="device-onboarding-workbench__template-meta">
                <strong>{{ pack.packName }}</strong>
                <span>{{ pack.packCode }} / v{{ pack.versionNo || 1 }} / {{ pack.status }}</span>
                <small>{{ pack.scenarioCode || '--' }} / {{ pack.deviceFamily || '--' }}</small>
              </div>
              <div class="device-onboarding-workbench__template-actions">
                <StandardButton v-permission="'iot:device-onboarding:template-pack'" @click="handleApplyTemplatePack(pack)">预填案例</StandardButton>
                <StandardButton v-permission="'iot:device-onboarding:template-pack'" link @click="handleEditTemplatePack(pack)">编辑模板</StandardButton>
              </div>
            </article>
          </div>
        </PanelCard>
      </section>

      <PanelCard
        title="流程判断"
        description="首版状态推导规则固定为：协议信息缺失 -> 产品缺失 -> 发布批次缺失 -> 验收设备缺失 -> 进入标准接入验收。"
      >
        <ul class="device-onboarding-workbench__rules">
          <li>缺协议族、解密档案或协议模板时，阻塞在“协议治理”。</li>
          <li>协议信息齐全但未绑定产品时，阻塞在“产品治理”。</li>
          <li>已绑定产品但未发布正式合同批次时，停留在“合同发布”。</li>
          <li>正式批次已存在但缺验收设备编码时，阻塞在“接入验收”。</li>
          <li>验收设备齐备后可触发标准接入验收，并跳转自动化治理台的结果证据查看 runId 结果。</li>
        </ul>
      </PanelCard>

      <PanelCard
        title="批量操作"
        description="P1-1 首版只放大稳定流程：批量建档、批量套模板、批量触发验收，并按失败原因收口结果。"
      >
        <div class="device-onboarding-workbench__batch-grid">
          <label class="device-onboarding-workbench__field device-onboarding-workbench__field--wide">
            <span>批量建档输入</span>
            <textarea
              v-model="batchCreateInput"
              data-testid="onboarding-batch-input"
              class="device-onboarding-workbench__input device-onboarding-workbench__textarea"
              placeholder="每行一条：案例编码,案例名称,设备编码(可选)"
            />
            <small class="device-onboarding-workbench__batch-hint">格式固定为 `案例编码,案例名称,设备编码(可选)`，不在这里复制协议治理或产品治理编辑器。</small>
          </label>
          <label class="device-onboarding-workbench__field">
            <span>批量模板包</span>
            <select
              v-model="batchTemplatePackId"
              data-testid="onboarding-batch-template-pack-select"
              class="device-onboarding-workbench__input"
            >
              <option value="">请选择模板包</option>
              <option
                v-for="pack in templateRows"
                :key="`batch-${String(pack.id)}`"
                :value="String(pack.id)"
              >
                {{ pack.packName }} / {{ pack.packCode }}
              </option>
            </select>
          </label>
          <div class="device-onboarding-workbench__batch-selection">
            <span>已选择 {{ selectedCaseIds.length }} 个案例</span>
            <small>批量模板和批量验收都只作用于当前勾选案例。</small>
          </div>
        </div>
        <div class="device-onboarding-workbench__form-actions">
          <StandardButton
            v-permission="'iot:device-onboarding:batch-create'"
            data-testid="onboarding-batch-create"
            :disabled="batchCreating"
            @click="handleBatchCreate"
          >
            {{ batchCreating ? '批量创建中...' : '批量创建' }}
          </StandardButton>
          <StandardButton
            v-permission="'iot:device-onboarding:batch-apply-template'"
            data-testid="onboarding-batch-apply-template"
            :disabled="batchApplyingTemplate || selectedCaseIds.length === 0 || !batchTemplatePackId"
            @click="handleBatchApplyTemplate"
          >
            {{ batchApplyingTemplate ? '套用中...' : '批量套用模板' }}
          </StandardButton>
          <StandardButton
            v-permission="'iot:device-onboarding:start-acceptance'"
            data-testid="onboarding-batch-start-acceptance"
            :disabled="batchStartingAcceptance || selectedCaseIds.length === 0"
            @click="handleBatchStartAcceptance"
          >
            {{ batchStartingAcceptance ? '批量验收中...' : '批量触发验收' }}
          </StandardButton>
          <StandardButton link @click="clearBatchSelection">清空选择</StandardButton>
        </div>
        <section
          v-if="batchResult"
          data-testid="onboarding-batch-result"
          class="device-onboarding-workbench__batch-result"
        >
          <div class="device-onboarding-workbench__batch-summary">
            <strong>{{ batchResult.action || 'BATCH' }}</strong>
            <span>请求 {{ batchResult.requestedCount }} 条</span>
            <span>成功 {{ batchResult.successCount }} 条</span>
            <span>失败 {{ batchResult.failedCount }} 条</span>
          </div>
          <div v-if="batchResult.failureGroups.length" class="device-onboarding-workbench__batch-groups">
            <h4>失败分组</h4>
            <ul>
              <li
                v-for="group in batchResult.failureGroups"
                :key="group.failureKey || group.summary"
              >
                <strong>{{ group.summary }}</strong>
                <span>{{ group.count }} 条</span>
                <small v-if="group.caseCodes.length">涉及案例：{{ group.caseCodes.join('、') }}</small>
              </li>
            </ul>
          </div>
        </section>
      </PanelCard>

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
            <div class="device-onboarding-workbench__item-select">
              <input
                :data-testid="`onboarding-select-${row.id}`"
                type="checkbox"
                :value="String(row.id)"
                v-model="selectedCaseIds"
              >
              <div class="device-onboarding-workbench__item-title">
                <strong>{{ row.caseName }}</strong>
                <span>{{ row.caseCode }}</span>
              </div>
            </div>
            <div class="device-onboarding-workbench__item-status">
              <span class="device-onboarding-workbench__badge">{{ statusLabel(row.status) }}</span>
              <span class="device-onboarding-workbench__step">{{ stepLabel(row.currentStep) }}</span>
            </div>
          </header>

          <p class="device-onboarding-workbench__item-copy">
            {{ primaryBlocker(row) }}
          </p>

          <section
            v-if="row.acceptance"
            class="device-onboarding-workbench__acceptance"
          >
            <div class="device-onboarding-workbench__acceptance-head">
              <span class="device-onboarding-workbench__acceptance-badge">
                {{ acceptanceStatusLabel(row) }}
              </span>
              <span v-if="row.acceptance.runId" class="device-onboarding-workbench__acceptance-run">
                runId {{ row.acceptance.runId }}
              </span>
            </div>
            <strong>{{ row.acceptance.summary || '标准接入验收已触发' }}</strong>
            <span v-if="row.acceptance.failedLayers?.length">
              未通过层级：{{ row.acceptance.failedLayers.join('、') }}
            </span>
          </section>

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
            <div>
              <dt>设备编码</dt>
              <dd>{{ row.deviceCode || '--' }}</dd>
            </div>
          </dl>

          <div class="device-onboarding-workbench__actions">
            <StandardButton v-permission="'iot:device-onboarding:update-case'" @click="handleEdit(row)">编辑</StandardButton>
            <StandardButton
              v-permission="'iot:device-onboarding:refresh-status'"
              :data-testid="`onboarding-refresh-${row.id}`"
              :disabled="refreshingId === String(row.id)"
              @click="handleRefreshRow(row)"
            >
              {{ refreshingId === String(row.id) ? '刷新中...' : '刷新状态' }}
            </StandardButton>
            <StandardButton
              v-if="row.currentStep === 'ACCEPTANCE'"
              v-permission="'iot:device-onboarding:start-acceptance'"
              :data-testid="`onboarding-accept-${row.id}`"
              :disabled="acceptingId === String(row.id) || !canStartAcceptance(row)"
              @click="handleStartAcceptance(row)"
            >
              {{ acceptingId === String(row.id) ? '验收中...' : '触发验收' }}
            </StandardButton>
            <StandardButton
              v-if="canOpenAcceptance(row)"
              :data-testid="`onboarding-result-${row.id}`"
              @click="handleOpenAcceptance(row)"
            >
              查看结果
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
  batchApplyDeviceOnboardingCaseTemplate,
  batchCreateDeviceOnboardingCases,
  batchStartDeviceOnboardingCasesAcceptance,
  createOnboardingTemplatePack,
  createDeviceOnboardingCase,
  pageDeviceOnboardingCases,
  pageOnboardingTemplatePacks,
  refreshDeviceOnboardingCaseStatus,
  startDeviceOnboardingCaseAcceptance,
  updateOnboardingTemplatePack,
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
  DeviceOnboardingCaseBatchResult,
  DeviceOnboardingCase,
  DeviceOnboardingCaseCreatePayload,
  DeviceOnboardingCasePageQuery,
  DeviceOnboardingCaseUpdatePayload,
  OnboardingTemplatePack,
  OnboardingTemplatePackCreatePayload,
  OnboardingTemplatePackUpdatePayload
} from '@/types/api'
import { ElMessage } from '@/utils/message'
import { buildAutomationGovernanceEvidencePath } from '@/utils/automationGovernance'
import { buildProductWorkbenchSectionPath } from '@/utils/productWorkbenchRoutes'

const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const templateSaving = ref(false)
const batchCreating = ref(false)
const batchApplyingTemplate = ref(false)
const batchStartingAcceptance = ref(false)
const refreshingId = ref('')
const acceptingId = ref('')
const rows = ref<DeviceOnboardingCase[]>([])
const templateRows = ref<OnboardingTemplatePack[]>([])
const editingId = ref<string | number | null>(null)
const templateEditingId = ref<string | number | null>(null)
const selectedCaseIds = ref<string[]>([])
const batchCreateInput = ref('')
const batchTemplatePackId = ref('')
const batchResult = ref<DeviceOnboardingCaseBatchResult | null>(null)

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
  templatePackId: '',
  caseCode: '',
  caseName: '',
  scenarioCode: '',
  deviceFamily: '',
  protocolFamilyCode: '',
  decryptProfileCode: '',
  protocolTemplateCode: '',
  productId: '',
  releaseBatchId: '',
  deviceCode: '',
  remark: ''
})

const templateForm = reactive({
  packCode: '',
  packName: '',
  scenarioCode: '',
  deviceFamily: '',
  status: 'ACTIVE',
  protocolFamilyCode: '',
  decryptProfileCode: '',
  protocolTemplateCode: '',
  description: ''
})

const blockedCount = computed(() => rows.value.filter((row) => row.status === 'BLOCKED').length)
const inProgressCount = computed(() => rows.value.filter((row) => row.status === 'IN_PROGRESS').length)
const readyCount = computed(() => rows.value.filter((row) => row.status === 'READY').length)

onMounted(() => {
  void initializePage()
})

async function initializePage(): Promise<void> {
  await Promise.all([loadCases(), loadTemplatePacks()])
}

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

async function loadTemplatePacks(): Promise<void> {
  try {
    const response = await pageOnboardingTemplatePacks({
      tenantId: 1,
      pageNum: 1,
      pageSize: 20
    })
    templateRows.value = response.data.records || []
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '加载模板包失败'))
  }
}

function resetForm(): void {
  editingId.value = null
  form.templatePackId = ''
  form.caseCode = ''
  form.caseName = ''
  form.scenarioCode = ''
  form.deviceFamily = ''
  form.protocolFamilyCode = ''
  form.decryptProfileCode = ''
  form.protocolTemplateCode = ''
  form.productId = ''
  form.releaseBatchId = ''
  form.deviceCode = ''
  form.remark = ''
}

function handleEdit(row: DeviceOnboardingCase): void {
  editingId.value = row.id
  form.templatePackId = row.templatePackId == null ? '' : String(row.templatePackId)
  form.caseCode = row.caseCode || ''
  form.caseName = row.caseName || ''
  form.scenarioCode = row.scenarioCode || ''
  form.deviceFamily = row.deviceFamily || ''
  form.protocolFamilyCode = row.protocolFamilyCode || ''
  form.decryptProfileCode = row.decryptProfileCode || ''
  form.protocolTemplateCode = row.protocolTemplateCode || ''
  form.productId = row.productId == null ? '' : String(row.productId)
  form.releaseBatchId = row.releaseBatchId == null ? '' : String(row.releaseBatchId)
  form.deviceCode = row.deviceCode || ''
  form.remark = row.remark || ''
}

function resetTemplateForm(): void {
  templateEditingId.value = null
  templateForm.packCode = ''
  templateForm.packName = ''
  templateForm.scenarioCode = ''
  templateForm.deviceFamily = ''
  templateForm.status = 'ACTIVE'
  templateForm.protocolFamilyCode = ''
  templateForm.decryptProfileCode = ''
  templateForm.protocolTemplateCode = ''
  templateForm.description = ''
}

function handleEditTemplatePack(pack: OnboardingTemplatePack): void {
  templateEditingId.value = pack.id
  templateForm.packCode = pack.packCode || ''
  templateForm.packName = pack.packName || ''
  templateForm.scenarioCode = pack.scenarioCode || ''
  templateForm.deviceFamily = pack.deviceFamily || ''
  templateForm.status = pack.status || 'ACTIVE'
  templateForm.protocolFamilyCode = pack.protocolFamilyCode || ''
  templateForm.decryptProfileCode = pack.decryptProfileCode || ''
  templateForm.protocolTemplateCode = pack.protocolTemplateCode || ''
  templateForm.description = pack.description || ''
}

async function handleSaveTemplatePack(): Promise<void> {
  templateSaving.value = true
  try {
    if (templateEditingId.value == null) {
      await createOnboardingTemplatePack(buildTemplatePayload())
      ElMessage.success('模板包已创建')
    } else {
      await updateOnboardingTemplatePack(templateEditingId.value, buildTemplatePayload())
      ElMessage.success('模板包已更新')
    }
    resetTemplateForm()
    await loadTemplatePacks()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '保存模板包失败'))
  } finally {
    templateSaving.value = false
  }
}

function handleApplySelectedTemplatePack(): void {
  const selected = templateRows.value.find((pack) => String(pack.id) === form.templatePackId)
  if (!selected) {
    ElMessage.error('当前模板包不存在或尚未加载')
    return
  }
  handleApplyTemplatePack(selected)
}

function handleApplyTemplatePack(pack: OnboardingTemplatePack): void {
  form.templatePackId = String(pack.id)
  form.scenarioCode = pack.scenarioCode || ''
  form.deviceFamily = pack.deviceFamily || ''
  form.protocolFamilyCode = pack.protocolFamilyCode || ''
  form.decryptProfileCode = pack.decryptProfileCode || ''
  form.protocolTemplateCode = pack.protocolTemplateCode || ''
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

function clearBatchSelection(): void {
  selectedCaseIds.value = []
}

async function handleBatchCreate(): Promise<void> {
  batchCreating.value = true
  try {
    const items = parseBatchCreateInput()
    const response = await batchCreateDeviceOnboardingCases({ items })
    batchResult.value = response.data
    batchCreateInput.value = ''
    ElMessage.success(`批量创建已完成：成功 ${response.data.successCount} 条，失败 ${response.data.failedCount} 条`)
    await loadCases()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '批量创建接入案例失败'))
  } finally {
    batchCreating.value = false
  }
}

async function handleBatchApplyTemplate(): Promise<void> {
  batchApplyingTemplate.value = true
  try {
    const caseIds = selectedCaseIds.value.map((id) => Number(id))
    const templatePackId = normalizeOptionalNumber(batchTemplatePackId.value)
    if (!caseIds.length) {
      throw new Error('请至少选择一个接入案例')
    }
    if (templatePackId == null) {
      throw new Error('请先选择模板包')
    }
    const response = await batchApplyDeviceOnboardingCaseTemplate({
      caseIds,
      templatePackId
    })
    batchResult.value = response.data
    clearBatchSelection()
    ElMessage.success(`批量套用模板已完成：成功 ${response.data.successCount} 条，失败 ${response.data.failedCount} 条`)
    await loadCases()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '批量套用模板失败'))
  } finally {
    batchApplyingTemplate.value = false
  }
}

async function handleBatchStartAcceptance(): Promise<void> {
  batchStartingAcceptance.value = true
  try {
    const caseIds = selectedCaseIds.value.map((id) => Number(id))
    if (!caseIds.length) {
      throw new Error('请至少选择一个接入案例')
    }
    const response = await batchStartDeviceOnboardingCasesAcceptance({ caseIds })
    batchResult.value = response.data
    clearBatchSelection()
    ElMessage.success(`批量触发验收已完成：成功 ${response.data.successCount} 条，失败 ${response.data.failedCount} 条`)
    await loadCases()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '批量触发验收失败'))
  } finally {
    batchStartingAcceptance.value = false
  }
}

async function handleRefresh(): Promise<void> {
  await initializePage()
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

async function handleStartAcceptance(row: DeviceOnboardingCase): Promise<void> {
  acceptingId.value = String(row.id)
  try {
    await startDeviceOnboardingCaseAcceptance(row.id)
    ElMessage.success('标准接入验收已触发')
    await loadCases()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '触发标准接入验收失败'))
  } finally {
    acceptingId.value = ''
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

function canStartAcceptance(row: DeviceOnboardingCase): boolean {
  return row.currentStep === 'ACCEPTANCE'
    && row.status === 'READY'
    && row.acceptance?.status !== 'RUNNING'
    && !row.acceptance?.runId
}

function canOpenAcceptance(row: DeviceOnboardingCase): boolean {
  return Boolean(row.acceptance?.runId && acceptanceJumpPath(row))
}

function handleOpenAcceptance(row: DeviceOnboardingCase): void {
  const path = acceptanceJumpPath(row)
  if (!path) {
    return
  }
  void router.push(path)
}

function acceptanceJumpPath(row: DeviceOnboardingCase): string {
  if (row.acceptance?.jumpPath) {
    if (row.acceptance.jumpPath.startsWith('/automation-results')) {
      return buildAutomationGovernanceEvidencePath(row.acceptance.runId)
    }
    return row.acceptance.jumpPath
  }
  if (row.acceptance?.runId) {
    return buildAutomationGovernanceEvidencePath(row.acceptance.runId)
  }
  return ''
}

function acceptanceStatusLabel(row: DeviceOnboardingCase): string {
  if (row.acceptance?.status === 'RUNNING') {
    return '验收执行中'
  }
  if (row.acceptance?.status === 'PASSED') {
    return '验收通过'
  }
  if (row.acceptance?.status === 'BLOCKED') {
    return '验收阻塞'
  }
  if (row.acceptance?.status === 'FAILED') {
    return '验收失败'
  }
  return '待触发验收'
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
    templatePackId: normalizeOptionalNumber(form.templatePackId),
    productId: normalizeOptionalNumber(form.productId),
    releaseBatchId: normalizeOptionalNumber(form.releaseBatchId),
    deviceCode: normalizeOptionalText(form.deviceCode),
    remark: normalizeOptionalText(form.remark)
  }
}

function parseBatchCreateInput(): DeviceOnboardingCaseCreatePayload[] {
  const lines = batchCreateInput.value
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
  if (!lines.length) {
    throw new Error('请至少输入一条批量案例')
  }
  return lines.map((line, index) => {
    const parts = line.split(',').map((part) => part.trim())
    if (parts.length < 2 || !parts[0] || !parts[1]) {
      throw new Error(`第 ${index + 1} 行格式错误，请使用 案例编码,案例名称,设备编码(可选)`)
    }
    return {
      tenantId: 1,
      caseCode: parts[0],
      caseName: parts[1],
      scenarioCode: null,
      deviceFamily: null,
      protocolFamilyCode: null,
      decryptProfileCode: null,
      protocolTemplateCode: null,
      templatePackId: null,
      productId: null,
      releaseBatchId: null,
      deviceCode: parts[2] || null,
      remark: null
    }
  })
}

function buildTemplatePayload(): OnboardingTemplatePackCreatePayload | OnboardingTemplatePackUpdatePayload {
  return {
    tenantId: 1,
    packCode: templateForm.packCode,
    packName: templateForm.packName,
    scenarioCode: normalizeOptionalText(templateForm.scenarioCode),
    deviceFamily: normalizeOptionalText(templateForm.deviceFamily),
    status: normalizeOptionalText(templateForm.status),
    protocolFamilyCode: normalizeOptionalText(templateForm.protocolFamilyCode),
    decryptProfileCode: normalizeOptionalText(templateForm.decryptProfileCode),
    protocolTemplateCode: normalizeOptionalText(templateForm.protocolTemplateCode),
    description: normalizeOptionalText(templateForm.description)
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

.device-onboarding-workbench__template-apply {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.7rem;
  align-items: center;
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

.device-onboarding-workbench__template-list {
  display: grid;
  gap: 0.7rem;
  margin-top: 0.95rem;
}

.device-onboarding-workbench__template-item {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  padding: 0.8rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.72);
}

.device-onboarding-workbench__template-meta {
  display: grid;
  gap: 0.16rem;
}

.device-onboarding-workbench__template-meta strong {
  color: var(--text-heading);
}

.device-onboarding-workbench__template-meta span,
.device-onboarding-workbench__template-meta small {
  color: var(--text-caption);
}

.device-onboarding-workbench__template-actions {
  display: flex;
  gap: 0.6rem;
  align-items: center;
}

.device-onboarding-workbench__rules {
  margin: 0;
  padding-left: 1.1rem;
  color: var(--text-secondary);
  line-height: 1.8;
}

.device-onboarding-workbench__batch-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 0.8fr);
  gap: 0.8rem;
}

.device-onboarding-workbench__batch-hint {
  color: var(--text-caption);
  line-height: 1.6;
}

.device-onboarding-workbench__batch-selection {
  display: grid;
  gap: 0.3rem;
  align-content: start;
  padding: 0.85rem 0.9rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-xl);
  background: color-mix(in srgb, var(--brand) 4%, white);
}

.device-onboarding-workbench__batch-selection span {
  color: var(--text-heading);
  font-weight: 600;
}

.device-onboarding-workbench__batch-selection small {
  color: var(--text-caption);
}

.device-onboarding-workbench__batch-result {
  display: grid;
  gap: 0.75rem;
  margin-top: 0.9rem;
  padding: 0.9rem;
  border: 1px solid color-mix(in srgb, var(--brand) 14%, white);
  border-radius: var(--radius-xl);
  background: color-mix(in srgb, var(--brand) 4%, white);
}

.device-onboarding-workbench__batch-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem;
  color: var(--text-secondary);
}

.device-onboarding-workbench__batch-summary strong {
  color: var(--text-heading);
}

.device-onboarding-workbench__batch-groups {
  display: grid;
  gap: 0.5rem;
}

.device-onboarding-workbench__batch-groups h4 {
  margin: 0;
  color: var(--text-heading);
}

.device-onboarding-workbench__batch-groups ul {
  margin: 0;
  padding-left: 1.1rem;
  display: grid;
  gap: 0.45rem;
}

.device-onboarding-workbench__batch-groups li {
  display: grid;
  gap: 0.16rem;
}

.device-onboarding-workbench__batch-groups span,
.device-onboarding-workbench__batch-groups small {
  color: var(--text-secondary);
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

.device-onboarding-workbench__item-select {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  min-width: 0;
}

.device-onboarding-workbench__item-select input[type='checkbox'] {
  margin-top: 0.2rem;
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

.device-onboarding-workbench__acceptance {
  display: grid;
  gap: 0.35rem;
  padding: 0.85rem 0.9rem;
  border: 1px solid color-mix(in srgb, var(--brand) 14%, white);
  border-radius: var(--radius-xl);
  background: color-mix(in srgb, var(--brand) 4%, white);
}

.device-onboarding-workbench__acceptance-head {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
  align-items: center;
}

.device-onboarding-workbench__acceptance-badge,
.device-onboarding-workbench__acceptance-run {
  display: inline-flex;
  align-items: center;
  padding: 0.24rem 0.62rem;
  border-radius: var(--radius-pill);
  font-size: 12px;
}

.device-onboarding-workbench__acceptance-badge {
  background: color-mix(in srgb, var(--brand) 12%, white);
  color: var(--brand);
}

.device-onboarding-workbench__acceptance-run {
  background: color-mix(in srgb, var(--warning) 10%, white);
  color: var(--text-secondary);
}

.device-onboarding-workbench__acceptance strong {
  color: var(--text-heading);
}

.device-onboarding-workbench__acceptance span:last-child {
  color: var(--text-secondary);
}

.device-onboarding-workbench__meta {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
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

  .device-onboarding-workbench__batch-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 720px) {
  .device-onboarding-workbench__summary,
  .device-onboarding-workbench__form-grid,
  .device-onboarding-workbench__meta {
    grid-template-columns: minmax(0, 1fr);
  }

  .device-onboarding-workbench__template-apply,
  .device-onboarding-workbench__template-item {
    grid-template-columns: minmax(0, 1fr);
  }

  .device-onboarding-workbench__item-head {
    flex-direction: column;
  }

  .device-onboarding-workbench__template-item,
  .device-onboarding-workbench__template-actions,
  .device-onboarding-workbench__form-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .device-onboarding-workbench__item-status {
    justify-content: flex-start;
  }
}
</style>
