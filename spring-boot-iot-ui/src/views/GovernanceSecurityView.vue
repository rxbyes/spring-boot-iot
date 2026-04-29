<template>
  <StandardPageShell class="governance-security-view">
    <StandardWorkbenchPanel
      title="权限与密钥治理"
      :description="`统一查看治理动作角色矩阵与设备密钥轮换台账，当前累计 ${pagination.total} 条轮换记录。`"
      show-filters
      show-toolbar
      show-pagination
    >
      <template #filters>
        <StandardListFilterHeader :model="filters">
          <template #primary>
            <el-form-item>
              <el-input v-model="filters.deviceCode" placeholder="设备编码" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="filters.productKey" placeholder="产品 Key" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="filters.rotationBatchId" placeholder="轮换批次号" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="query" @click="handleSearch">查询台账</StandardButton>
            <StandardButton action="reset" @click="handleReset">重置</StandardButton>
          </template>
        </StandardListFilterHeader>
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[
            `治理动作 ${matrixItems.length} 项`,
            `双人复核 ${dualControlCount} 项`,
            `轮换记录 ${pagination.total} 条`
          ]"
        >
          <template #right>
            <StandardButton action="refresh" link @click="handleRefresh">刷新全页</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <div class="governance-security-grid">
        <PanelCard
          title="治理权限矩阵"
          description="按治理动作展示执行权限、复核权限与默认角色建议，便于核对职责边界。"
        >
          <div
            v-loading="matrixLoading"
            class="governance-security-surface standard-list-surface"
            element-loading-text="正在刷新治理权限矩阵"
            element-loading-background="var(--loading-mask-bg)"
          >
            <el-table v-if="matrixItems.length" :data="matrixItems" border stripe style="width: 100%">
              <StandardTableTextColumn
                prop="actionName"
                label="治理动作"
                secondary-prop="domainName"
                :min-width="220"
              />
              <StandardTableTextColumn prop="operatorPermissionCode" label="执行权限" :min-width="220" />
              <StandardTableTextColumn prop="approverPermissionCode" label="复核权限" :min-width="220" />
              <el-table-column label="双人复核" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.dualControlRequired ? 'warning' : 'info'" round>
                    {{ row.dualControlRequired ? '需要' : '无需' }}
                  </el-tag>
                </template>
              </el-table-column>
              <StandardTableTextColumn prop="auditModule" label="审计模块" :min-width="160" />
            </el-table>

            <StandardInlineState
              v-else-if="matrixErrorMessage"
              tone="error"
              :message="matrixErrorMessage"
            />

            <div v-else class="governance-security-empty">
              <EmptyState title="暂无治理权限矩阵" description="后端尚未返回矩阵项，请先确认权限种子与角色配置。" />
            </div>
          </div>
        </PanelCard>

        <PanelCard
          title="密钥轮换台账"
          description="仅展示密钥摘要、执行人与复核人，不暴露任何明文密钥。"
        >
          <div
            v-loading="rotationLoading"
            class="governance-security-surface standard-list-surface"
            element-loading-text="正在刷新密钥轮换台账"
            element-loading-background="var(--loading-mask-bg)"
          >
            <el-table v-if="rotationLogList.length" :data="rotationLogList" border stripe style="width: 100%">
              <StandardTableTextColumn
                prop="deviceCode"
                label="设备"
                secondary-prop="productKey"
                :min-width="180"
              />
              <StandardTableTextColumn prop="rotationBatchId" label="轮换批次号" :min-width="190" />
              <StandardTableTextColumn prop="previousSecretDigest" label="旧密钥摘要" :min-width="180" />
              <StandardTableTextColumn prop="currentSecretDigest" label="新密钥摘要" :min-width="180" />
              <StandardTableTextColumn prop="rotatedBy" label="执行人" :width="100" />
              <StandardTableTextColumn prop="approvedBy" label="复核人" :width="100" />
              <StandardTableTextColumn prop="rotateTime" label="轮换时间" :min-width="170" />
              <StandardTableTextColumn prop="reason" label="轮换原因" :min-width="180" />
            </el-table>

            <StandardInlineState
              v-else-if="rotationErrorMessage"
              tone="error"
              :message="rotationErrorMessage"
            />

            <div v-else class="governance-security-empty">
              <EmptyState title="暂无密钥轮换记录" description="当前筛选条件下还没有轮换台账，可先重置筛选或稍后重试。" />
            </div>
          </div>
        </PanelCard>
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
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import {
  getGovernancePermissionMatrix,
  pageDeviceSecretRotationLogs,
  type DeviceSecretRotationLogPageItem,
  type GovernancePermissionMatrixItem
} from '@/api/governanceSecurity'
import { resolveRequestErrorMessage } from '@/api/request'
import EmptyState from '@/components/EmptyState.vue'
import PanelCard from '@/components/PanelCard.vue'
import StandardButton from '@/components/StandardButton.vue'
import StandardInlineState from '@/components/StandardInlineState.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardPageShell from '@/components/StandardPageShell.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import { useServerPagination } from '@/composables/useServerPagination'

const filters = reactive({
  deviceCode: '',
  productKey: '',
  rotationBatchId: ''
})

const appliedFilters = reactive({
  deviceCode: '',
  productKey: '',
  rotationBatchId: ''
})

const { pagination, applyPageResult, resetPage, setPageNum, setPageSize } = useServerPagination()

const matrixLoading = ref(false)
const rotationLoading = ref(false)
const matrixItems = ref<GovernancePermissionMatrixItem[]>([])
const rotationLogList = ref<DeviceSecretRotationLogPageItem[]>([])
const matrixErrorMessage = ref('')
const rotationErrorMessage = ref('')

const dualControlCount = computed(() => matrixItems.value.filter((item) => Boolean(item.dualControlRequired)).length)

async function loadMatrix() {
  matrixLoading.value = true
  matrixErrorMessage.value = ''
  try {
    const response = await getGovernancePermissionMatrix()
    matrixItems.value = response.code === 200 ? response.data || [] : []
  } catch (error) {
    matrixItems.value = []
    matrixErrorMessage.value = resolveRequestErrorMessage(error, '治理权限矩阵加载失败，请稍后重试。')
  } finally {
    matrixLoading.value = false
  }
}

async function loadRotationLogs() {
  rotationLoading.value = true
  rotationErrorMessage.value = ''
  try {
    const response = await pageDeviceSecretRotationLogs({
      deviceCode: appliedFilters.deviceCode,
      productKey: appliedFilters.productKey,
      rotationBatchId: appliedFilters.rotationBatchId,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    rotationLogList.value = response.code === 200 ? applyPageResult(response.data) : []
  } catch (error) {
    rotationLogList.value = []
    rotationErrorMessage.value = resolveRequestErrorMessage(error, '密钥轮换台账加载失败，请稍后重试。')
  } finally {
    rotationLoading.value = false
  }
}

function copyFilters() {
  appliedFilters.deviceCode = filters.deviceCode.trim()
  appliedFilters.productKey = filters.productKey.trim()
  appliedFilters.rotationBatchId = filters.rotationBatchId.trim()
}

function handleSearch() {
  resetPage()
  copyFilters()
  void loadRotationLogs()
}

function handleReset() {
  filters.deviceCode = ''
  filters.productKey = ''
  filters.rotationBatchId = ''
  appliedFilters.deviceCode = ''
  appliedFilters.productKey = ''
  appliedFilters.rotationBatchId = ''
  resetPage()
  void loadRotationLogs()
}

function handleRefresh() {
  void Promise.all([loadMatrix(), loadRotationLogs()])
}

function handleSizeChange(size: number) {
  setPageSize(size)
  void loadRotationLogs()
}

function handlePageChange(page: number) {
  setPageNum(page)
  void loadRotationLogs()
}

onMounted(async () => {
  try {
    await Promise.all([loadMatrix(), loadRotationLogs()])
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '权限与密钥治理加载失败，请稍后重试。'))
  }
})
</script>

<style scoped>
.governance-security-view {
  display: flex;
  flex-direction: column;
}

.governance-security-grid {
  display: grid;
  gap: 1rem;
}

.governance-security-surface {
  min-height: 18rem;
}

.governance-security-empty {
  padding: 1.25rem 0;
}

@media (min-width: 1280px) {
  .governance-security-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
