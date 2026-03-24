<template>
  <StandardFormDrawer
    v-model="visible"
    eyebrow="Device Batch Import"
    title="批量导入设备"
    subtitle="通过 CSV 一次性补录设备资产主档，适合库存初始化、批次上新和现场回收录入。"
    size="58rem"
    @close="handleClose"
  >
    <div class="device-import-stack">
      <div class="device-import-note">
        <strong>导入说明</strong>
        <span>CSV 首行请使用模板字段名。必填字段是 <code>productKey</code>、<code>deviceName</code>、<code>deviceCode</code>；如需一次性建父子设备关系，可额外填写 <code>parentDeviceCode</code>。</span>
      </div>

      <section class="device-import-section">
        <div class="device-import-section__header">
          <div>
            <h3>模板与原始内容</h3>
            <p>支持直接粘贴 CSV，也支持读取本地 CSV 文件。建议先下载模板，再按批次整理设备信息。</p>
          </div>
          <div class="device-import-actions">
            <StandardRowActions variant="editor" gap="comfortable">
              <StandardActionLink @click="downloadTemplate">下载 CSV 模板</StandardActionLink>
              <StandardActionLink @click="triggerFileSelect">读取本地 CSV</StandardActionLink>
            </StandardRowActions>
            <input
              ref="fileInputRef"
              class="device-import-file-input"
              type="file"
              accept=".csv,text/csv"
              @change="handleFileChange"
            >
          </div>
        </div>
        <el-input
          v-model="csvText"
          type="textarea"
          :rows="12"
          placeholder="请粘贴 CSV 内容，例如：&#10;productKey,deviceName,deviceCode,parentDeviceCode,deviceSecret,clientId,username,password,activateStatus,deviceStatus,firmwareVersion,ipAddress,address,metadataJson"
        />
      </section>

      <section class="device-import-section">
        <div class="device-import-section__header">
          <div>
            <h3>字段约定</h3>
            <p><code>parentDeviceCode</code> 选填，填写已存在父设备编码即可建立父子关系；<code>activateStatus</code> 和 <code>deviceStatus</code> 仅支持 <code>1</code> 或 <code>0</code>；<code>metadataJson</code> 请输入合法 JSON。</p>
          </div>
        </div>
        <div class="device-import-rule-grid">
          <div class="device-import-rule-card">
            <span>必填</span>
            <strong>productKey / deviceName / deviceCode</strong>
          </div>
          <div class="device-import-rule-card">
            <span>父子关系</span>
            <strong>parentDeviceCode 选填且需已存在</strong>
          </div>
          <div class="device-import-rule-card">
            <span>状态字段</span>
            <strong>activateStatus / deviceStatus</strong>
          </div>
          <div class="device-import-rule-card">
            <span>扩展信息</span>
            <strong>metadataJson 支持站点、批次、责任人</strong>
          </div>
        </div>
      </section>

      <section class="device-import-section">
        <div class="device-import-section__header">
          <div>
            <h3>导入预览</h3>
            <p>提交前先确认设备编码、产品 Key 和状态字段是否正确，避免重复导入或错绑产品。</p>
          </div>
        </div>

        <el-alert
          v-if="parseState.errorMessages.length"
          type="warning"
          :closable="false"
          show-icon
          class="device-import-alert"
        >
          <template #title>
            发现 {{ parseState.errorMessages.length }} 个格式问题，请修正后再提交。
          </template>
          <div class="device-import-error-list">
            <span v-for="message in parseState.errorMessages" :key="message">{{ message }}</span>
          </div>
        </el-alert>

        <div class="device-import-summary">
          <span>已识别 {{ parseState.items.length }} 台设备</span>
          <span>预览 {{ previewRows.length }} 台</span>
          <span>模板字段 {{ templateColumnKeys.length }} 个</span>
        </div>

        <el-table v-if="previewRows.length" :data="previewRows" border stripe height="260">
          <el-table-column prop="rowNo" label="行号" width="72" />
          <el-table-column prop="productKey" label="产品 Key" min-width="160" />
          <el-table-column prop="deviceName" label="设备名称" min-width="150" />
          <el-table-column prop="deviceCode" label="设备编码" min-width="170" />
          <el-table-column prop="parentDeviceCode" label="父设备编码" min-width="170" />
          <el-table-column prop="activateStatus" label="激活状态" width="96">
            <template #default="{ row }">{{ row.activateStatus === 0 ? '未激活' : '已激活' }}</template>
          </el-table-column>
          <el-table-column prop="deviceStatus" label="设备状态" width="96">
            <template #default="{ row }">{{ row.deviceStatus === 0 ? '禁用' : '启用' }}</template>
          </el-table-column>
          <el-table-column prop="address" label="部署位置" min-width="180" />
        </el-table>
        <el-empty v-else description="粘贴 CSV 后，这里会展示可提交的设备预览。" />

        <p v-if="parseState.items.length > previewRows.length" class="device-import-hint">
          仅展示前 {{ previewRows.length }} 条预览，提交时会按全部 {{ parseState.items.length }} 条记录导入。
        </p>
      </section>

      <section v-if="result" class="device-import-section">
        <div class="device-import-section__header">
          <div>
            <h3>最近一次导入结果</h3>
            <p>批量导入支持部分成功。失败项可直接按行号回到 CSV 修正后重新提交。</p>
          </div>
        </div>

        <div class="device-import-result-grid">
          <div class="device-import-result-card">
            <span>总计</span>
            <strong>{{ result.totalCount }}</strong>
          </div>
          <div class="device-import-result-card">
            <span>成功</span>
            <strong>{{ result.successCount }}</strong>
          </div>
          <div class="device-import-result-card">
            <span>失败</span>
            <strong>{{ result.failureCount }}</strong>
          </div>
        </div>

        <el-alert
          v-if="result.createdDeviceCodes?.length"
          type="success"
          :closable="false"
          show-icon
          class="device-import-alert"
        >
          <template #title>
            已创建设备：{{ result.createdDeviceCodes.join('、') }}
          </template>
        </el-alert>

        <el-table v-if="result.errors.length" :data="result.errors" border stripe max-height="220">
          <el-table-column prop="rowNo" label="行号" width="72" />
          <el-table-column prop="deviceCode" label="设备编码" min-width="160" />
          <el-table-column prop="message" label="失败原因" min-width="260" />
        </el-table>
      </section>
    </div>

    <template #footer>
      <StandardDrawerFooter
        :confirm-loading="submitting"
        confirm-text="提交批量导入"
        @cancel="visible = false"
        @confirm="handleSubmit"
      />
    </template>
  </StandardFormDrawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { DeviceBatchAddPayload, DeviceBatchAddResult } from '@/types/api'
import StandardDrawerFooter from './StandardDrawerFooter.vue'
import StandardFormDrawer from './StandardFormDrawer.vue'
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv'

interface DeviceImportPreviewRow {
  rowNo: number
  productKey: string
  deviceName: string
  deviceCode: string
  parentDeviceCode?: string
  deviceSecret?: string
  clientId?: string
  username?: string
  password?: string
  activateStatus?: number
  deviceStatus?: number
  firmwareVersion?: string
  ipAddress?: string
  address?: string
  metadataJson?: string
}

interface ParseState {
  items: DeviceBatchAddPayload['items']
  previewRows: DeviceImportPreviewRow[]
  errorMessages: string[]
}

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    submitting?: boolean
    result?: DeviceBatchAddResult | null
  }>(),
  {
    submitting: false,
    result: null
  }
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'submit', payload: DeviceBatchAddPayload): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const fileInputRef = ref<HTMLInputElement | null>(null)
const csvText = ref('')

const templateColumns: CsvColumn<Record<string, string>>[] = [
  { key: 'productKey', label: 'productKey' },
  { key: 'deviceName', label: 'deviceName' },
  { key: 'deviceCode', label: 'deviceCode' },
  { key: 'parentDeviceCode', label: 'parentDeviceCode' },
  { key: 'deviceSecret', label: 'deviceSecret' },
  { key: 'clientId', label: 'clientId' },
  { key: 'username', label: 'username' },
  { key: 'password', label: 'password' },
  { key: 'activateStatus', label: 'activateStatus' },
  { key: 'deviceStatus', label: 'deviceStatus' },
  { key: 'firmwareVersion', label: 'firmwareVersion' },
  { key: 'ipAddress', label: 'ipAddress' },
  { key: 'address', label: 'address' },
  { key: 'metadataJson', label: 'metadataJson' }
]
const templateColumnKeys = templateColumns.map((column) => String(column.key))

const parseState = computed<ParseState>(() => parseCsvText(csvText.value))
const previewRows = computed(() => parseState.value.previewRows.slice(0, 8))

function handleClose() {
  csvText.value = ''
  resetFileInput()
}

function resetFileInput() {
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

function normalizeCellValue(value: string) {
  const trimmed = value.trim()
  return trimmed.length ? trimmed : undefined
}

function parseOptionalNumber(value: string, rowNo: number, fieldName: string, errorMessages: string[]) {
  const normalized = normalizeCellValue(value)
  if (normalized === undefined) {
    return undefined
  }
  if (!/^-?\d+$/.test(normalized)) {
    errorMessages.push(`第 ${rowNo} 行字段 ${fieldName} 只能填写数字`)
    return undefined
  }
  return Number(normalized)
}

function parseCsvRows(text: string) {
  const normalized = text.replace(/^\uFEFF/, '').replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  const rows: string[][] = []
  let row: string[] = []
  let cell = ''
  let inQuotes = false

  for (let index = 0; index < normalized.length; index += 1) {
    const current = normalized[index]
    if (current === '"') {
      if (inQuotes && normalized[index + 1] === '"') {
        cell += '"'
        index += 1
      } else {
        inQuotes = !inQuotes
      }
      continue
    }
    if (current === ',' && !inQuotes) {
      row.push(cell)
      cell = ''
      continue
    }
    if (current === '\n' && !inQuotes) {
      row.push(cell)
      if (row.some((item) => item.trim().length > 0)) {
        rows.push(row)
      }
      row = []
      cell = ''
      continue
    }
    cell += current
  }

  if (inQuotes) {
    return {
      rows: [],
      errorMessage: 'CSV 中存在未闭合的双引号，请检查 metadataJson 或文本字段。'
    }
  }

  row.push(cell)
  if (row.some((item) => item.trim().length > 0)) {
    rows.push(row)
  }

  return {
    rows,
    errorMessage: ''
  }
}

function parseCsvText(text: string): ParseState {
  const emptyState: ParseState = {
    items: [],
    previewRows: [],
    errorMessages: []
  }
  if (!text.trim()) {
    return emptyState
  }

  const { rows, errorMessage } = parseCsvRows(text)
  if (errorMessage) {
    return {
      ...emptyState,
      errorMessages: [errorMessage]
    }
  }
  if (!rows.length) {
    return emptyState
  }

  const headers = rows[0].map((header) => header.trim())
  const requiredHeaders = ['productKey', 'deviceName', 'deviceCode']
  const missingHeaders = requiredHeaders.filter((header) => !headers.includes(header))
  if (missingHeaders.length) {
    return {
      ...emptyState,
      errorMessages: [`CSV 缺少必要列：${missingHeaders.join('、')}`]
    }
  }

  const errorMessages: string[] = []
  const headerIndex = new Map(headers.map((header, index) => [header, index]))
  const items: DeviceBatchAddPayload['items'] = []
  const preview: DeviceImportPreviewRow[] = []

  rows.slice(1).forEach((cells, offset) => {
    const rowNo = offset + 2
    const readCell = (header: string) => cells[headerIndex.get(header) ?? -1] ?? ''
    const metadataJson = normalizeCellValue(readCell('metadataJson'))
    if (metadataJson) {
      try {
        JSON.parse(metadataJson)
      } catch {
        errorMessages.push(`第 ${rowNo} 行字段 metadataJson 不是合法 JSON`)
      }
    }

    const item = {
      productKey: readCell('productKey').trim(),
      deviceName: readCell('deviceName').trim(),
      deviceCode: readCell('deviceCode').trim(),
      parentDeviceCode: normalizeCellValue(readCell('parentDeviceCode')),
      deviceSecret: normalizeCellValue(readCell('deviceSecret')),
      clientId: normalizeCellValue(readCell('clientId')),
      username: normalizeCellValue(readCell('username')),
      password: normalizeCellValue(readCell('password')),
      activateStatus: parseOptionalNumber(readCell('activateStatus'), rowNo, 'activateStatus', errorMessages),
      deviceStatus: parseOptionalNumber(readCell('deviceStatus'), rowNo, 'deviceStatus', errorMessages),
      firmwareVersion: normalizeCellValue(readCell('firmwareVersion')),
      ipAddress: normalizeCellValue(readCell('ipAddress')),
      address: normalizeCellValue(readCell('address')),
      metadataJson
    }

    if (
      !item.productKey &&
      !item.deviceName &&
      !item.deviceCode &&
      !item.parentDeviceCode &&
      !item.deviceSecret &&
      !item.clientId &&
      !item.username &&
      !item.password &&
      item.activateStatus === undefined &&
      item.deviceStatus === undefined &&
      !item.firmwareVersion &&
      !item.ipAddress &&
      !item.address &&
      !item.metadataJson
    ) {
      return
    }

    items.push(item)
    preview.push({
      rowNo,
      ...item
    })
  })

  return {
    items,
    previewRows: preview,
    errorMessages
  }
}

function downloadTemplate() {
  downloadRowsAsCsv(
    '设备批量导入模板.csv',
    [
      {
        productKey: 'accept-http-product-01',
        deviceName: '北坡监测点-01',
        deviceCode: 'accept-http-device-02',
        parentDeviceCode: 'accept-http-gateway-01',
        deviceSecret: 'device-secret-02',
        clientId: 'client-device-02',
        username: 'device-user-02',
        password: 'device-password-02',
        activateStatus: '1',
        deviceStatus: '1',
        firmwareVersion: 'v1.0.0',
        ipAddress: '192.168.1.20',
        address: '北坡监测点 A 区',
        metadataJson: '{"site":"北坡监测点","batch":"20260319"}'
      }
    ],
    templateColumns
  )
}

function triggerFileSelect() {
  fileInputRef.value?.click()
}

async function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) {
    return
  }
  csvText.value = await file.text()
  resetFileInput()
}

function handleSubmit() {
  if (!parseState.value.items.length) {
    ElMessage.warning('请先粘贴或读取需要导入的 CSV 内容')
    return
  }
  if (parseState.value.errorMessages.length) {
    ElMessage.error('CSV 内容存在格式问题，请修正后再提交')
    return
  }
  emit('submit', { items: parseState.value.items })
}
</script>

<style scoped>
.device-import-stack {
  display: grid;
  gap: 16px;
}

.device-import-note {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid color-mix(in srgb, var(--brand) 14%, transparent);
  border-radius: calc(var(--radius-lg) + 2px);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(244, 248, 255, 0.92)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 8%, transparent), transparent 42%);
}

.device-import-note strong {
  color: var(--text-heading);
  font-size: 14px;
}

.device-import-note span {
  color: var(--text-caption);
  line-height: 1.7;
}

.device-import-note code {
  font-family: 'JetBrains Mono', 'Cascadia Code', monospace;
}

.device-import-section {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: rgba(255, 255, 255, 0.88);
}

.device-import-section__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.device-import-section__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 16px;
}

.device-import-section__header p {
  margin: 6px 0 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.65;
}

.device-import-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.device-import-file-input {
  display: none;
}

.device-import-rule-grid,
.device-import-result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.device-import-rule-card,
.device-import-result-card {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: calc(var(--radius-md) + 2px);
  border: 1px solid color-mix(in srgb, var(--brand) 10%, transparent);
  background: rgba(248, 251, 255, 0.92);
}

.device-import-rule-card span,
.device-import-result-card span {
  color: var(--text-caption);
  font-size: 12px;
}

.device-import-rule-card strong,
.device-import-result-card strong {
  color: var(--text-heading);
  font-size: 15px;
  line-height: 1.5;
}

.device-import-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: var(--text-caption);
  font-size: 13px;
}

.device-import-alert {
  margin-bottom: 0;
}

.device-import-error-list {
  display: grid;
  gap: 4px;
  margin-top: 6px;
}

.device-import-hint {
  margin: 0;
  color: var(--text-caption);
  font-size: 12px;
}

@media (max-width: 900px) {
  .device-import-section__header {
    flex-direction: column;
  }
}
</style>
