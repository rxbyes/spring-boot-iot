<template>
  <div class="product-device-workspace">
    <div v-if="loading" class="device-state">{{ loadingText }}</div>
    <div v-else-if="errorMessage" class="device-state device-state--error">{{ errorMessage }}</div>
    <div v-else-if="empty" class="device-state">{{ emptyText }}</div>
    <div v-else class="device-workspace__content">
      <section class="device-workspace__registry-sheet device-workspace__registry-card">
        <header class="device-workspace__registry-heading">
          <strong>设备清单</strong>
        </header>

        <div class="device-workspace__table-shell">
          <el-table
            v-loading="devicesLoading"
            :data="devices"
            border
            stripe
            empty-text="当前产品还没有关联设备"
          >
            <StandardTableTextColumn
              prop="deviceName"
              label="设备"
              :min-width="220"
              secondary-prop="deviceCode"
            />
            <el-table-column prop="onlineStatus" label="在线状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.onlineStatus === 1 ? 'success' : 'danger'" round>
                  {{ row.onlineStatus === 1 ? '在线' : '离线' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="activateStatus" label="激活状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.activateStatus === 1 ? 'success' : 'info'" round>
                  {{ row.activateStatus === 1 ? '已激活' : '未激活' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="firmwareVersion" label="固件版本" width="120" show-overflow-tooltip />
            <el-table-column prop="lastReportTime" label="最近上报" width="160">
              <template #default="{ row }">
                <span>{{ formatDateTime(row.lastReportTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              :width="deviceLedgerActionColumnWidth"
              fixed="right"
              show-overflow-tooltip
              class-name="standard-row-actions-column"
            >
              <template #default="{ row }">
                <StandardWorkbenchRowActions
                  variant="table"
                  :direct-items="deviceLedgerRowActions"
                  @command="() => handleViewDevice(row)"
                />
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="pagination.total > 0" class="device-workspace__pagination">
          <StandardPagination
            :current-page="pagination.pageNum"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="handlePageChange"
            @size-change="handlePageSizeChange"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { toRefs } from 'vue'

import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue'
import type { Device } from '@/types/api'
import type { ServerPaginationState } from '@/composables/useServerPagination'
import { resolveWorkbenchActionColumnWidth } from '@/utils/adaptiveActionColumn'
import { formatDateTime } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    devices: Device[]
    pagination?: ServerPaginationState
    loading?: boolean
    loadingText?: string
    errorMessage?: string
    empty?: boolean
    emptyText?: string
    devicesLoading?: boolean
  }>(),
  {
    pagination: () => ({
      pageNum: 1,
      pageSize: 10,
      total: 0
    }),
    loading: false,
    loadingText: '正在加载设备...',
    errorMessage: '',
    empty: false,
    emptyText: '暂无设备数据',
    devicesLoading: false
  }
)

const {
  devices,
  pagination,
  loading,
  loadingText,
  errorMessage,
  empty,
  emptyText,
  devicesLoading
} = toRefs(props)

const emit = defineEmits<{
  (event: 'viewDevice', device: Device): void
  (event: 'page-change', page: number): void
  (event: 'page-size-change', size: number): void
}>()
const deviceLedgerRowActions = [{ command: 'view' as const, label: '查看' }]

const deviceLedgerActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: deviceLedgerRowActions
})

function handleViewDevice(device: Device) {
  emit('viewDevice', device)
}

function handlePageChange(page: number) {
  emit('page-change', page)
}

function handlePageSizeChange(size: number) {
  emit('page-size-change', size)
}
</script>

<style scoped>
.product-device-workspace,
.device-workspace__content,
.device-workspace__registry-sheet {
  display: grid;
}

.product-device-workspace,
.device-workspace__content {
  gap: 1rem;
}

.device-state {
  padding: 0.96rem 1rem;
  border: 1px solid var(--panel-border);
  background: linear-gradient(180deg, color-mix(in srgb, var(--brand-light) 16%, white), white);
  color: var(--text-secondary);
  font-size: 0.82rem;
  line-height: 1.7;
}

.device-state--error {
  color: color-mix(in srgb, var(--danger) 78%, var(--text-secondary));
  border-color: color-mix(in srgb, var(--danger) 20%, var(--panel-border));
}

.device-workspace__registry-sheet {
  gap: 0.88rem;
}

.device-workspace__pagination {
  display: flex;
  justify-content: flex-end;
}

.device-workspace__registry-card {
  padding: 0.86rem 0.92rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  background: linear-gradient(
    160deg,
    color-mix(in srgb, var(--brand-light) 14%, white) 0%,
    rgba(255, 255, 255, 0.98) 100%
  );
  box-shadow: 0 8px 20px -20px color-mix(in srgb, var(--brand) 40%, transparent);
}

.device-workspace__registry-heading {
  display: grid;
  justify-items: start;
}

.device-workspace__registry-heading strong {
  color: var(--text-heading);
  font-size: 1rem;
  line-height: 1.45;
}

.device-workspace__table-shell {
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--brand) 7%, var(--panel-border));
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand-light) 10%, white));
}

.device-workspace__table-shell :deep(.el-table) {
  background: transparent;
}

.device-workspace__table-shell :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.device-workspace__table-shell :deep(.el-table th.el-table__cell) {
  background: color-mix(in srgb, var(--brand-light) 12%, white);
  border-bottom-color: color-mix(in srgb, var(--brand) 8%, var(--panel-border));
}

.device-workspace__table-shell :deep(.el-table td.el-table__cell) {
  border-bottom-color: color-mix(in srgb, var(--brand) 6%, var(--panel-border));
}

.device-workspace__table-shell :deep(.el-table__row:hover) {
  background: color-mix(in srgb, var(--brand) 4%, transparent);
}
</style>
