<template>
  <component
    :is="wrapperComponent"
    v-bind="wrapperProps"
    v-on="wrapperListeners"
  >
    <div class="risk-point-binding-maintenance-drawer">
      <div class="ops-drawer-note">
        <strong>维护说明</strong>
        <span>这里维护的是风险点与设备、测点之间的正式绑定关系，不会修改设备主档、产品定义或厂家侧原始资料。</span>
      </div>

      <div v-if="!riskPointId" class="standard-list-empty-state">
        <EmptyState title="请选择风险点" description="从列表行内进入后，可在此查看正式绑定摘要，并继续维护设备与测点关系。" />
      </div>

      <template v-else>
        <section class="ops-drawer-section">
          <div class="ops-drawer-section__header">
            <div>
              <h3>{{ riskPointName || '未命名风险点' }}</h3>
              <p>{{ riskPointCode || '尚未生成风险点编号' }}</p>
            </div>
          </div>
          <div class="risk-point-binding-maintenance-drawer__summary">
            <span>所属组织 {{ orgName || '未配置组织' }}</span>
            <span>{{ bindingGroups.length }} 台已绑定设备</span>
            <span>{{ totalBoundMetricCount }} 个正式测点</span>
            <span>待治理 {{ pendingBindingCount }} 条</span>
          </div>
        </section>

        <section v-if="pendingBindingCount > 0" class="ops-drawer-section">
          <div class="risk-point-binding-maintenance-drawer__reminder">
            <strong>治理提醒</strong>
            <span>当前仍有 {{ pendingBindingCount }} 条待治理台账未转正式绑定。维护正式测点后，请继续回到“待治理转正”完成治理收口。</span>
          </div>
        </section>

        <section class="ops-drawer-section">
          <div class="ops-drawer-section__header">
            <div>
              <h3>新增正式绑定</h3>
              <p>{{ addSectionDescription }}</p>
            </div>
          </div>
          <div class="ops-drawer-grid">
            <el-form-item label="设备">
              <el-select
                v-model="addForm.deviceId"
                data-testid="binding-add-device"
                filterable
                placeholder="请输入设备编号或选择设备"
                :disabled="bindableDevices.length === 0 || addSubmitting"
                @change="handleAddDeviceChange"
              >
                <el-option
                  v-for="device in bindableDevices"
                  :key="String(device.id)"
                  :label="`${device.deviceCode} - ${device.deviceName}`"
                  :value="device.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item v-if="showAddMetricSelector" label="测点">
              <el-select
                v-model="addForm.metricIdentifier"
                data-testid="binding-add-metric"
                placeholder="请选择测点"
                :disabled="!addForm.deviceId || addMetricOptions.length === 0 || addSubmitting"
              >
                <el-option
                  v-for="metric in addMetricOptions"
                  :key="metric.identifier"
                  :label="metric.name"
                  :value="metric.identifier"
                />
              </el-select>
            </el-form-item>
          </div>
          <p
            v-if="addDeviceHint"
            class="risk-point-binding-maintenance-drawer__detail-tip"
          >
            {{ addDeviceHint }}
          </p>
          <div class="risk-point-binding-maintenance-drawer__actions">
            <button
              type="button"
              class="risk-point-binding-maintenance-drawer__button risk-point-binding-maintenance-drawer__button--primary"
              data-testid="binding-add-submit"
              :disabled="addSubmitting"
              @click="handleAddBinding"
            >
              {{ addSubmitLabel }}
            </button>
          </div>
        </section>

        <section class="ops-drawer-section">
          <div class="ops-drawer-section__header">
            <div>
              <h3>当前正式绑定</h3>
              <p>整机解绑会释放整台设备的正式绑定；单测点删除仅移除当前测点，不影响同设备下的其他绑定。</p>
            </div>
          </div>

          <div v-if="loading" class="risk-point-binding-maintenance-drawer__loading">
            正在加载正式绑定...
          </div>

          <div v-else-if="bindingGroups.length === 0" class="standard-list-empty-state">
            <EmptyState title="暂无正式绑定" description="当前风险点还没有正式绑定的设备和测点，可先通过上方表单补充。" />
          </div>

          <div v-else class="risk-point-binding-maintenance-drawer__group-list">
            <article
              v-for="group in bindingGroups"
              :key="String(group.deviceId)"
              class="risk-point-binding-maintenance-drawer__group-card"
              :data-testid="`binding-group-card-${group.deviceId}`"
            >
              <div class="risk-point-binding-maintenance-drawer__group-header">
                <div>
                  <h4>{{ group.deviceName || '未命名设备' }}</h4>
                  <p>
                    {{ group.deviceCode || '--' }}
                    ·
                    {{ isDeviceOnlyGroup(group) ? '设备级正式绑定' : `${group.metricCount} 个正式测点` }}
                  </p>
                </div>
                <button
                  type="button"
                  class="risk-point-binding-maintenance-drawer__button risk-point-binding-maintenance-drawer__button--danger"
                  :data-testid="`binding-unbind-device-${group.deviceId}`"
                  :disabled="actionLoadingKey === `unbind:${group.deviceId}`"
                  @click="handleWholeDeviceUnbind(group)"
                >
                  整机解绑
                </button>
              </div>

              <div v-if="isDeviceOnlyGroup(group)" class="risk-point-binding-maintenance-drawer__device-only-card">
                <strong>设备级正式绑定</strong>
                <p>{{ getGroupCapabilityLabel(group) }}</p>
                <p v-if="isAiEventReserved(group)">AI 事件扩展预留</p>
              </div>

              <div v-else class="risk-point-binding-maintenance-drawer__metric-list">
                <div
                  v-for="metric in group.metrics"
                  :key="String(metric.bindingId)"
                  class="risk-point-binding-maintenance-drawer__metric-row"
                >
                  <div class="risk-point-binding-maintenance-drawer__metric-main">
                    <div>
                      <strong>{{ metric.metricName || metric.metricIdentifier }}</strong>
                      <p>{{ metric.metricIdentifier }}</p>
                      <p v-if="metric.riskMetricId">目录指标 #{{ metric.riskMetricId }}</p>
                    </div>
                    <el-tag :data-testid="`binding-source-badge-${metric.bindingId}`" type="info">
                      {{ getBindingSourceLabel(metric.bindingSource) }}
                    </el-tag>
                  </div>

                  <div class="risk-point-binding-maintenance-drawer__metric-actions">
                    <button
                      type="button"
                      class="risk-point-binding-maintenance-drawer__button"
                      :data-testid="`binding-replace-open-${metric.bindingId}`"
                      :disabled="actionLoadingKey === `replace-open:${metric.bindingId}`"
                      @click="handleOpenReplace(group.deviceId, metric.bindingId)"
                    >
                      更换测点
                    </button>
                    <button
                      type="button"
                      class="risk-point-binding-maintenance-drawer__button risk-point-binding-maintenance-drawer__button--danger"
                      :data-testid="`binding-remove-${metric.bindingId}`"
                      :disabled="actionLoadingKey === `remove:${metric.bindingId}`"
                      @click="handleRemoveBinding(metric.bindingId, group.deviceName, metric.metricName || metric.metricIdentifier)"
                    >
                      删除测点
                    </button>
                  </div>

                  <div
                    v-if="activeReplaceBindingId === getIdKey(metric.bindingId)"
                    class="risk-point-binding-maintenance-drawer__replace-row"
                  >
                    <el-select
                      v-model="replaceSelectionMap[getIdKey(metric.bindingId)]"
                      :data-testid="`binding-replace-metric-${metric.bindingId}`"
                      placeholder="请选择替换后的测点"
                    >
                      <el-option
                        v-for="option in getReplaceOptions(group.deviceId, metric.bindingId)"
                        :key="option.identifier"
                        :label="option.name"
                        :value="option.identifier"
                      />
                    </el-select>
                    <button
                      type="button"
                      class="risk-point-binding-maintenance-drawer__button risk-point-binding-maintenance-drawer__button--primary"
                      :data-testid="`binding-replace-submit-${metric.bindingId}`"
                      :disabled="actionLoadingKey === `replace:${metric.bindingId}`"
                      @click="handleReplaceBinding(group.deviceId, metric.bindingId)"
                    >
                      确认替换
                    </button>
                  </div>
                </div>
              </div>
            </article>
          </div>
        </section>
      </template>
    </div>
  </component>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import type { DeviceMetricOption, DeviceOption, IdType } from '@/types/api'
import {
  bindDevice,
  bindDeviceCapability,
  listBindableDevices,
  listBindingGroups,
  listFormalBindingMetricOptions,
  removeBinding,
  replaceBinding,
  unbindDevice,
  type RiskPointBindingDeviceGroup,
  type RiskPointBindingMetric
} from '@/api/riskPoint'
import { confirmAction, isConfirmCancelled } from '@/utils/confirm'
import { ElMessage } from '@/utils/message'
import {
  getDeviceCapabilityLabel,
  getDeviceOnlyBindingButtonLabel,
  getDeviceOnlyBindingHint,
  isAiEventReserved,
  isDeviceOnlyBindingMode,
  resolveBindingGroupCapabilityType,
  supportsMetricBinding
} from '@/utils/riskPointDeviceBindingCapability'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    embedded?: boolean
    riskPointId?: IdType | null
    riskPointName?: string
    riskPointCode?: string
    orgName?: string
    pendingBindingCount?: number
  }>(),
  {
    riskPointId: undefined,
    embedded: false,
    riskPointName: '',
    riskPointCode: '',
    orgName: '',
    pendingBindingCount: 0
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  close: []
  updated: []
}>()

const loading = ref(false)
const addSubmitting = ref(false)
const actionLoadingKey = ref('')
const bindingGroups = ref<RiskPointBindingDeviceGroup[]>([])
const bindableDevices = ref<DeviceOption[]>([])
const selectedAddDevice = ref<DeviceOption | null>(null)
const addMetricOptions = ref<DeviceMetricOption[]>([])
const activeReplaceBindingId = ref<string | null>(null)
const metricOptionCache = reactive<Record<string, DeviceMetricOption[]>>({})
const replaceSelectionMap = reactive<Record<string, string>>({})
let latestDrawerLoadRequestId = 0
let latestAddMetricRequestId = 0
let latestReplaceMetricRequestId = 0

const addForm = reactive({
  deviceId: '' as '' | IdType,
  metricIdentifier: ''
})

const totalBoundMetricCount = computed(() =>
  bindingGroups.value.reduce((sum, group) => sum + Number(group.metricCount || group.metrics?.length || 0), 0)
)
const showAddMetricSelector = computed(() => supportsMetricBinding(selectedAddDevice.value))
const addSectionDescription = computed(() =>
  !selectedAddDevice.value
    ? '先选择设备，再按设备能力补充正式绑定。'
    : showAddMetricSelector.value
    ? '先选择设备，再从该设备已发布的正式目录字段中补充正式绑定。'
    : '先选择设备，再按设备级正式绑定收口预警型或视频类设备。'
)
const addSubmitLabel = computed(() => getDeviceOnlyBindingButtonLabel(selectedAddDevice.value))
const addDeviceHint = computed(() => {
  if (!addForm.deviceId || addSubmitting.value) {
    return ''
  }
  if (!showAddMetricSelector.value) {
    return getDeviceOnlyBindingHint(selectedAddDevice.value)
  }
  return addMetricOptions.value.length === 0 ? '当前设备所属产品暂无可用于风险绑定的正式目录字段。' : ''
})
const wrapperComponent = computed(() => (props.embedded ? 'div' : StandardFormDrawer))
const wrapperProps = computed(() =>
  props.embedded
    ? {}
    : {
        modelValue: props.modelValue,
        title: '维护绑定',
        subtitle: '查看正式绑定摘要，并继续维护设备与测点关系。',
        size: '42rem'
      }
)
const wrapperListeners = computed(() =>
  props.embedded
    ? {}
    : {
        'update:modelValue': handleModelValueChange,
        close: handleClose
      }
)

const bindingSourceLabelMap: Record<RiskPointBindingMetric['bindingSource'], string> = {
  MANUAL: '人工维护',
  PENDING_PROMOTION: '待治理转正',
  UNKNOWN: '来源未知'
}

const getBindingSourceLabel = (source: RiskPointBindingMetric['bindingSource']) => bindingSourceLabelMap[source] || bindingSourceLabelMap.UNKNOWN
const isDeviceOnlyGroup = (group: RiskPointBindingDeviceGroup) => isDeviceOnlyBindingMode(group)
const getGroupCapabilityLabel = (group: RiskPointBindingDeviceGroup) =>
  `${getDeviceCapabilityLabel(resolveBindingGroupCapabilityType(group))} · 设备级正式绑定`

const getIdKey = (value?: IdType | null) => {
  if (value === undefined || value === null || value === '') {
    return ''
  }
  return String(value)
}

const isSameId = (left?: IdType | null, right?: IdType | null) => {
  const leftKey = getIdKey(left)
  return leftKey !== '' && leftKey === getIdKey(right)
}

const getBoundMetricIdentifiers = (deviceId: IdType, excludeBindingId?: IdType) => {
  const group = bindingGroups.value.find((item) => isSameId(item.deviceId, deviceId))
  return new Set(
    (group?.metrics || [])
      .filter((metric) => !isSameId(metric.bindingId, excludeBindingId))
      .map((metric) => metric.metricIdentifier)
      .filter(Boolean)
  )
}

const getBindingMetricIdentifier = (deviceId: IdType, bindingId?: IdType) => {
  if (!bindingId) {
    return ''
  }
  return (
    bindingGroups.value
      .find((item) => isSameId(item.deviceId, deviceId))
      ?.metrics.find((metric) => isSameId(metric.bindingId, bindingId))
      ?.metricIdentifier || ''
  )
}

const filterAvailableMetricOptions = (deviceId: IdType, options: DeviceMetricOption[], excludeBindingId?: IdType) => {
  const boundMetricIdentifiers = getBoundMetricIdentifiers(deviceId, excludeBindingId)
  const currentMetricIdentifier = getBindingMetricIdentifier(deviceId, excludeBindingId)
  return options.filter(
    (option) => !boundMetricIdentifiers.has(option.identifier) && option.identifier !== currentMetricIdentifier
  )
}

const getMetricOptions = async (deviceId: IdType) => {
  const deviceIdKey = getIdKey(deviceId)
  if (!deviceIdKey) {
    return []
  }
  if (Object.prototype.hasOwnProperty.call(metricOptionCache, deviceIdKey)) {
    return metricOptionCache[deviceIdKey]
  }
  const res = await listFormalBindingMetricOptions(deviceIdKey)
  if (res.code !== 200) {
    metricOptionCache[deviceIdKey] = []
    return []
  }
  metricOptionCache[deviceIdKey] = res.data || []
  return metricOptionCache[deviceIdKey]
}

const isActiveDrawerRequest = (requestId: number, riskPointId?: IdType | null) =>
  requestId === latestDrawerLoadRequestId
  && props.modelValue
  && getIdKey(props.riskPointId) === getIdKey(riskPointId)

const resetDrawerState = () => {
  latestDrawerLoadRequestId += 1
  latestAddMetricRequestId += 1
  latestReplaceMetricRequestId += 1
  loading.value = false
  addSubmitting.value = false
  bindingGroups.value = []
  bindableDevices.value = []
  addMetricOptions.value = []
  addForm.deviceId = ''
  addForm.metricIdentifier = ''
  selectedAddDevice.value = null
  activeReplaceBindingId.value = null
  actionLoadingKey.value = ''
  Object.keys(metricOptionCache).forEach((key) => {
    delete metricOptionCache[key]
  })
  Object.keys(replaceSelectionMap).forEach((key) => {
    delete replaceSelectionMap[key]
  })
}

const loadBindingGroups = async (riskPointId: IdType, requestId: number) => {
  if (!riskPointId) {
    bindingGroups.value = []
    return
  }
  const res = await listBindingGroups(riskPointId)
  if (!isActiveDrawerRequest(requestId, riskPointId)) {
    return
  }
  bindingGroups.value = res.code === 200 ? res.data || [] : []
  refreshAddMetricOptionsForCurrentDevice()
}

const loadBindableDeviceOptions = async (riskPointId: IdType, requestId: number) => {
  if (!riskPointId) {
    bindableDevices.value = []
    return
  }
  const res = await listBindableDevices(riskPointId)
  if (!isActiveDrawerRequest(requestId, riskPointId)) {
    return
  }
  bindableDevices.value = res.code === 200 ? res.data || [] : []
  selectedAddDevice.value = bindableDevices.value.find((item) => isSameId(item.id, addForm.deviceId)) || null
}

const loadDrawerData = async () => {
  if (!props.riskPointId) {
    resetDrawerState()
    return
  }
  const riskPointId = props.riskPointId
  const requestId = ++latestDrawerLoadRequestId
  loading.value = true
  try {
    await Promise.all([loadBindingGroups(riskPointId, requestId), loadBindableDeviceOptions(riskPointId, requestId)])
  } catch (error) {
    if (isActiveDrawerRequest(requestId, riskPointId)) {
      console.error('加载风险点正式绑定维护数据失败', error)
      ElMessage.error(error instanceof Error ? error.message : '加载正式绑定维护数据失败')
    }
  } finally {
    if (isActiveDrawerRequest(requestId, riskPointId)) {
      loading.value = false
    }
  }
}

const handleMutationSuccess = async (message: string, resetState?: () => void) => {
  ElMessage.success(message)
  resetState?.()
  await loadDrawerData()
  emit('updated')
}

const getSelectedMetricOption = (deviceId: IdType, metricIdentifier: string) => {
  const options = metricOptionCache[getIdKey(deviceId)] || []
  return options.find((item) => item.identifier === metricIdentifier)
}

const refreshAddMetricOptionsForCurrentDevice = () => {
  if (!getIdKey(addForm.deviceId)) {
    return
  }
  addMetricOptions.value = filterAvailableMetricOptions(
    addForm.deviceId,
    metricOptionCache[getIdKey(addForm.deviceId)] || addMetricOptions.value
  )
  if (addForm.metricIdentifier && !addMetricOptions.value.some((item) => item.identifier === addForm.metricIdentifier)) {
    addForm.metricIdentifier = ''
  }
}

const handleAddDeviceChange = async (deviceId: string | number) => {
  const requestId = ++latestAddMetricRequestId
  addForm.metricIdentifier = ''
  addMetricOptions.value = []
  if (!deviceId) {
    selectedAddDevice.value = null
    return
  }
  const device = bindableDevices.value.find((item) => isSameId(item.id, deviceId)) || null
  selectedAddDevice.value = device
  if (!supportsMetricBinding(device)) {
    return
  }
  try {
    const options = filterAvailableMetricOptions(deviceId, await getMetricOptions(deviceId))
    if (
      requestId !== latestAddMetricRequestId
      || !props.modelValue
      || getIdKey(addForm.deviceId) !== getIdKey(deviceId)
    ) {
      return
    }
    addMetricOptions.value = options
  } catch (error) {
    if (requestId !== latestAddMetricRequestId || !props.modelValue) {
      return
    }
    console.error('加载新增测点选项失败', error)
    ElMessage.error(error instanceof Error ? error.message : '加载测点列表失败')
  }
}

const handleAddBinding = async () => {
  if (!props.riskPointId) {
    return
  }
  if (!addForm.deviceId) {
    ElMessage.warning('请先选择设备')
    return
  }
  if (showAddMetricSelector.value && !addForm.metricIdentifier) {
    ElMessage.warning('请选择要绑定的测点')
    return
  }

  try {
    addSubmitting.value = true
    const res = showAddMetricSelector.value
      ? await bindDevice({
          riskPointId: props.riskPointId,
          deviceId: addForm.deviceId,
          riskMetricId: getSelectedMetricOption(addForm.deviceId, addForm.metricIdentifier)?.riskMetricId ?? undefined,
          metricIdentifier: addForm.metricIdentifier,
          metricName: getSelectedMetricOption(addForm.deviceId, addForm.metricIdentifier)?.name || addForm.metricIdentifier
        })
      : await bindDeviceCapability({
          riskPointId: props.riskPointId,
          deviceId: addForm.deviceId,
          deviceCapabilityType: selectedAddDevice.value?.deviceCapabilityType || undefined
        })
    if (res.code !== 200) {
      ElMessage.error(res.msg || `${addSubmitLabel.value}失败`)
      return
    }
    await handleMutationSuccess(`${addSubmitLabel.value}成功`, () => {
      addForm.deviceId = ''
      addForm.metricIdentifier = ''
      addMetricOptions.value = []
    })
  } catch (error) {
    console.error('新增正式绑定失败', error)
    ElMessage.error(error instanceof Error ? error.message : '新增正式绑定失败')
  } finally {
    addSubmitting.value = false
  }
}

const handleWholeDeviceUnbind = async (group: RiskPointBindingDeviceGroup) => {
  if (!props.riskPointId) {
    return
  }
  try {
    actionLoadingKey.value = `unbind:${group.deviceId}`
    await confirmAction({
      title: '整机解绑',
      message: `确认解绑设备“${group.deviceName || group.deviceCode || group.deviceId}”下的全部正式绑定吗？`,
      confirmButtonText: '确认解绑'
    })
    const res = await unbindDevice(props.riskPointId, group.deviceId)
    if (res.code !== 200) {
      ElMessage.error(res.msg || '整机解绑失败')
      return
    }
    await handleMutationSuccess('整机解绑成功')
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('整机解绑失败', error)
    ElMessage.error(error instanceof Error ? error.message : '整机解绑失败')
  } finally {
    actionLoadingKey.value = ''
  }
}

const handleRemoveBinding = async (bindingId: IdType, deviceName: string, metricName: string) => {
  try {
    actionLoadingKey.value = `remove:${bindingId}`
    await confirmAction({
      title: '删除测点绑定',
      message: `确认删除设备“${deviceName || '--'}”下的测点“${metricName}”吗？`,
      confirmButtonText: '确认删除'
    })
    const res = await removeBinding(bindingId)
    if (res.code !== 200) {
      ElMessage.error(res.msg || '删除测点绑定失败')
      return
    }
    await handleMutationSuccess('删除测点绑定成功')
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('删除测点绑定失败', error)
    ElMessage.error(error instanceof Error ? error.message : '删除测点绑定失败')
  } finally {
    actionLoadingKey.value = ''
  }
}

const handleOpenReplace = async (deviceId: IdType, bindingId: IdType) => {
  const requestId = ++latestReplaceMetricRequestId
  const riskPointId = props.riskPointId
  try {
    actionLoadingKey.value = `replace-open:${bindingId}`
    const options = filterAvailableMetricOptions(deviceId, await getMetricOptions(deviceId), bindingId)
    if (
      requestId !== latestReplaceMetricRequestId
      || !props.modelValue
      || getIdKey(props.riskPointId) !== getIdKey(riskPointId)
    ) {
      return
    }
    const bindingIdKey = getIdKey(bindingId)
    activeReplaceBindingId.value = bindingIdKey
    replaceSelectionMap[bindingIdKey] = options[0]?.identifier || ''
  } catch (error) {
    if (
      requestId !== latestReplaceMetricRequestId
      || !props.modelValue
      || getIdKey(props.riskPointId) !== getIdKey(riskPointId)
    ) {
      return
    }
    console.error('加载替换测点选项失败', error)
    ElMessage.error(error instanceof Error ? error.message : '加载替换测点列表失败')
  } finally {
    if (requestId === latestReplaceMetricRequestId) {
      actionLoadingKey.value = ''
    }
  }
}

const getReplaceOptions = (deviceId: IdType, bindingId: IdType) =>
  filterAvailableMetricOptions(deviceId, metricOptionCache[getIdKey(deviceId)] || [], bindingId)

const handleReplaceBinding = async (deviceId: IdType, bindingId: IdType) => {
  const bindingIdKey = getIdKey(bindingId)
  const metricIdentifier = replaceSelectionMap[bindingIdKey]
  if (!metricIdentifier) {
    ElMessage.warning('请选择替换后的测点')
    return
  }

  const option = getSelectedMetricOption(deviceId, metricIdentifier)
  try {
    actionLoadingKey.value = `replace:${bindingId}`
    await confirmAction({
      title: '更换测点',
      message: `确认把当前正式绑定替换为测点“${option?.name || metricIdentifier}”吗？`,
      confirmButtonText: '确认替换'
    })
    const res = await replaceBinding(bindingId, {
      riskMetricId: option?.riskMetricId ?? undefined,
      metricIdentifier,
      metricName: option?.name || metricIdentifier
    })
    if (res.code !== 200) {
      ElMessage.error(res.msg || '替换测点失败')
      return
    }
    await handleMutationSuccess('替换测点成功', () => {
      activeReplaceBindingId.value = null
      delete replaceSelectionMap[bindingIdKey]
    })
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    console.error('替换测点失败', error)
    ElMessage.error(error instanceof Error ? error.message : '替换测点失败')
  } finally {
    actionLoadingKey.value = ''
  }
}

const handleModelValueChange = (value: boolean) => {
  emit('update:modelValue', value)
  if (!value) {
    resetDrawerState()
  }
}

const handleClose = () => {
  emit('close')
  resetDrawerState()
}

watch(
  [() => props.modelValue, () => props.riskPointId],
  ([visible, riskPointId]) => {
    if (!visible) {
      resetDrawerState()
      return
    }
    if (!riskPointId) {
      return
    }
    void loadDrawerData()
  },
  { immediate: true }
)
</script>

<style scoped>
.risk-point-binding-maintenance-drawer {
  display: grid;
  gap: 1rem;
}

.risk-point-binding-maintenance-drawer__summary {
  display: grid;
  gap: 0.5rem;
  color: var(--text-secondary);
}

.risk-point-binding-maintenance-drawer__summary span {
  color: var(--text-primary);
}

.risk-point-binding-maintenance-drawer__reminder {
  display: grid;
  gap: 0.5rem;
  padding: 0.875rem 1rem;
  border-radius: 1rem;
  background: color-mix(in srgb, var(--warning-color, #f59e0b) 10%, white);
  color: var(--text-primary);
}

.risk-point-binding-maintenance-drawer__actions {
  display: flex;
  justify-content: flex-end;
}

.risk-point-binding-maintenance-drawer__detail-tip {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.risk-point-binding-maintenance-drawer__button {
  border: 1px solid var(--border-color, #d0d5dd);
  background: white;
  color: var(--text-primary);
  border-radius: 999px;
  padding: 0.45rem 0.9rem;
  font-size: 0.875rem;
  cursor: pointer;
}

.risk-point-binding-maintenance-drawer__button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.risk-point-binding-maintenance-drawer__button--primary {
  background: var(--brand-primary, #d97706);
  border-color: var(--brand-primary, #d97706);
  color: white;
}

.risk-point-binding-maintenance-drawer__button--danger {
  color: var(--danger-color, #b42318);
  border-color: color-mix(in srgb, var(--danger-color, #b42318) 32%, white);
}

.risk-point-binding-maintenance-drawer__loading {
  color: var(--text-secondary);
}

.risk-point-binding-maintenance-drawer__group-list {
  display: grid;
  gap: 0.875rem;
}

.risk-point-binding-maintenance-drawer__group-card {
  display: grid;
  gap: 0.875rem;
  padding: 1rem;
  border: 1px solid var(--border-color, #d0d5dd);
  border-radius: 1rem;
  background: var(--surface-raised, #fffdf8);
}

.risk-point-binding-maintenance-drawer__group-header,
.risk-point-binding-maintenance-drawer__metric-main,
.risk-point-binding-maintenance-drawer__metric-actions,
.risk-point-binding-maintenance-drawer__replace-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.risk-point-binding-maintenance-drawer__metric-list {
  display: grid;
  gap: 0.75rem;
}

.risk-point-binding-maintenance-drawer__device-only-card {
  display: grid;
  gap: 0.35rem;
  padding: 0.85rem 0.95rem;
  border-radius: 0.9rem;
  background: color-mix(in srgb, var(--brand-primary, #d97706) 8%, white);
  color: var(--text-primary);
}

.risk-point-binding-maintenance-drawer__device-only-card p {
  margin: 0;
  color: var(--text-secondary);
}

.risk-point-binding-maintenance-drawer__metric-row {
  display: grid;
  gap: 0.625rem;
  padding: 0.875rem;
  border-radius: 0.875rem;
  background: white;
  border: 1px solid color-mix(in srgb, var(--border-color, #d0d5dd) 82%, white);
}

.risk-point-binding-maintenance-drawer__metric-main p {
  margin: 0.25rem 0 0;
  color: var(--text-secondary);
  font-size: 0.8125rem;
}

.risk-point-binding-maintenance-drawer__metric-actions {
  justify-content: flex-end;
}

.risk-point-binding-maintenance-drawer__replace-row {
  justify-content: stretch;
}

.risk-point-binding-maintenance-drawer__replace-row :deep(.el-select) {
  flex: 1;
}

@media (max-width: 768px) {
  .risk-point-binding-maintenance-drawer__group-header,
  .risk-point-binding-maintenance-drawer__metric-main,
  .risk-point-binding-maintenance-drawer__metric-actions,
  .risk-point-binding-maintenance-drawer__replace-row {
    align-items: stretch;
    flex-direction: column;
  }

  .risk-point-binding-maintenance-drawer__actions {
    justify-content: stretch;
  }

  .risk-point-binding-maintenance-drawer__actions .risk-point-binding-maintenance-drawer__button,
  .risk-point-binding-maintenance-drawer__replace-row .risk-point-binding-maintenance-drawer__button,
  .risk-point-binding-maintenance-drawer__group-header .risk-point-binding-maintenance-drawer__button {
    width: 100%;
  }
}
</style>
