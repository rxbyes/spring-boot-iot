<template>
  <StandardFormDrawer
    v-model="visible"
    eyebrow="Device Replacement"
    title="更换设备"
    subtitle="录入新设备后，旧设备会自动转为停用并写入替换关系，便于库存台账和现场维护追溯。"
    size="48rem"
    @close="handleClose"
  >
    <div class="device-replace-stack">
      <div class="device-replace-note">
        <strong>替换说明</strong>
        <span>适用于现场换新、损坏返修和资产重编场景。提交后旧设备会自动离线并停用，新设备继承原有可复用主数据。</span>
      </div>

      <section v-if="device" class="device-replace-section">
        <div class="device-replace-section__header">
          <div>
            <h3>原设备概览</h3>
            <p>先确认当前要替换的是哪台设备，避免误把同产品下的其他库存设备停用。</p>
          </div>
        </div>
        <div class="device-replace-summary-grid">
          <div class="device-replace-summary-card">
            <span>设备编码</span>
            <strong>{{ device.deviceCode || '--' }}</strong>
          </div>
          <div class="device-replace-summary-card">
            <span>设备名称</span>
            <strong>{{ device.deviceName || '--' }}</strong>
          </div>
          <div class="device-replace-summary-card">
            <span>产品归属</span>
            <strong>{{ device.productKey || '--' }}</strong>
          </div>
          <div class="device-replace-summary-card">
            <span>当前状态</span>
            <strong>{{ device.deviceStatus === 0 ? '禁用' : '启用' }} / {{ device.onlineStatus === 1 ? '在线' : '离线' }}</strong>
          </div>
        </div>
      </section>

      <el-form ref="formRef" :model="formData" :rules="formRules" label-position="top" class="device-replace-form">
        <section class="device-replace-section">
          <div class="device-replace-section__header">
            <div>
              <h3>新设备基础档案</h3>
              <p>请填写新设备编码，并确认产品归属和设备名称。产品为空时会沿用原设备所属产品。</p>
            </div>
          </div>
          <div class="device-replace-grid">
            <el-form-item label="产品" prop="productKey">
              <el-select v-model="formData.productKey" filterable clearable placeholder="留空沿用原设备产品" :loading="productLoading">
                <el-option
                  v-for="product in productOptions"
                  :key="String(product.id)"
                  :label="`${product.productKey} - ${product.productName}`"
                  :value="product.productKey"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="新设备名称" prop="deviceName">
              <el-input v-model="formData.deviceName" placeholder="请输入新设备名称" />
            </el-form-item>
            <el-form-item label="新设备编码" prop="deviceCode">
              <el-input v-model="formData.deviceCode" placeholder="请输入新设备编码" />
            </el-form-item>
            <el-form-item label="部署位置" prop="address">
              <el-input v-model="formData.address" placeholder="请输入部署位置" />
            </el-form-item>
          </div>
        </section>

        <section class="device-replace-section">
          <div class="device-replace-section__header">
            <div>
              <h3>状态与认证字段</h3>
              <p>默认沿用原设备的认证字段，并把新设备设置为可用状态。若现场未完成安装，可先切换为未激活或禁用。</p>
            </div>
          </div>
          <div class="device-replace-grid">
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
              <el-input v-model="formData.firmwareVersion" placeholder="请输入固件版本" />
            </el-form-item>
            <el-form-item label="IP 地址" prop="ipAddress">
              <el-input v-model="formData.ipAddress" placeholder="请输入 IP 地址" />
            </el-form-item>
            <el-form-item label="设备密钥" prop="deviceSecret">
              <el-input v-model="formData.deviceSecret" placeholder="请输入设备密钥" />
            </el-form-item>
            <el-form-item label="Client ID" prop="clientId">
              <el-input v-model="formData.clientId" placeholder="请输入 Client ID" />
            </el-form-item>
            <el-form-item label="用户名" prop="username">
              <el-input v-model="formData.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="formData.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
          </div>
        </section>

        <section class="device-replace-section">
          <div class="device-replace-section__header">
            <div>
              <h3>扩展信息</h3>
              <p>metadataJson 可继续补充站点、批次、责任人等信息。系统会自动补入替换来源和替换时间。</p>
            </div>
          </div>
          <div class="device-replace-grid">
            <el-form-item label="metadataJson" prop="metadataJson" class="device-replace-grid__full">
              <el-input
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
        :confirm-loading="submitting"
        confirm-text="提交设备更换"
        @cancel="visible = false"
        @confirm="handleSubmit"
      >
        <el-button class="standard-drawer-footer__button standard-drawer-footer__button--ghost" @click="visible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          class="standard-drawer-footer__button standard-drawer-footer__button--primary"
          :loading="submitting"
          @click="handleSubmit"
        >
          提交设备更换
        </el-button>
      </StandardDrawerFooter>
    </template>
  </StandardFormDrawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { Device, DeviceReplacePayload, Product } from '@/types/api'
import StandardDrawerFooter from './StandardDrawerFooter.vue'
import StandardFormDrawer from './StandardFormDrawer.vue'

interface DeviceReplaceFormState extends DeviceReplacePayload {}

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    device: Device | null
    productOptions: Product[]
    productLoading?: boolean
    submitting?: boolean
  }>(),
  {
    productLoading: false,
    submitting: false
  }
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'submit', payload: DeviceReplacePayload): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const formRef = ref<FormInstance>()

const createDefaultFormData = (): DeviceReplaceFormState => ({
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

const formData = reactive<DeviceReplaceFormState>(createDefaultFormData())

const formRules: FormRules<DeviceReplaceFormState> = {
  deviceName: [{ required: true, message: '请输入新设备名称', trigger: 'blur' }],
  deviceCode: [{ required: true, message: '请输入新设备编码', trigger: 'blur' }],
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

watch(
  () => [props.modelValue, props.device] as const,
  ([visibleValue, device]) => {
    if (!visibleValue || !device) {
      return
    }
    Object.assign(formData, createDefaultFormData(), {
      productKey: device.productKey || '',
      deviceName: device.deviceName || '',
      deviceCode: '',
      deviceSecret: device.deviceSecret || '',
      clientId: device.clientId || '',
      username: device.username || '',
      password: device.password || '',
      activateStatus: 1,
      deviceStatus: 1,
      firmwareVersion: device.firmwareVersion || '',
      ipAddress: device.ipAddress || '',
      address: device.address || '',
      metadataJson: device.metadataJson || ''
    })
  }
)

function handleClose() {
  formRef.value?.clearValidate()
  Object.assign(formData, createDefaultFormData())
}

function handleSubmit() {
  formRef.value?.validate((valid) => {
    if (!valid) {
      return
    }
    emit('submit', {
      productKey: formData.productKey?.trim() || undefined,
      deviceName: formData.deviceName.trim(),
      deviceCode: formData.deviceCode.trim(),
      deviceSecret: formData.deviceSecret?.trim() || undefined,
      clientId: formData.clientId?.trim() || undefined,
      username: formData.username?.trim() || undefined,
      password: formData.password?.trim() || undefined,
      activateStatus: formData.activateStatus,
      deviceStatus: formData.deviceStatus,
      firmwareVersion: formData.firmwareVersion?.trim() || undefined,
      ipAddress: formData.ipAddress?.trim() || undefined,
      address: formData.address?.trim() || undefined,
      metadataJson: formData.metadataJson?.trim() || undefined
    })
  })
}
</script>

<style scoped>
.device-replace-stack {
  display: grid;
  gap: 16px;
}

.device-replace-note {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid color-mix(in srgb, var(--brand) 14%, transparent);
  border-radius: calc(var(--radius-lg) + 2px);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(245, 249, 255, 0.92)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 8%, transparent), transparent 42%);
}

.device-replace-note strong {
  color: var(--text-heading);
  font-size: 14px;
}

.device-replace-note span {
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.7;
}

.device-replace-form {
  display: grid;
  gap: 16px;
}

.device-replace-section {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: rgba(255, 255, 255, 0.88);
}

.device-replace-section__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 16px;
}

.device-replace-section__header p {
  margin: 6px 0 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.65;
}

.device-replace-summary-grid,
.device-replace-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
}

.device-replace-summary-card {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: calc(var(--radius-md) + 2px);
  border: 1px solid color-mix(in srgb, var(--brand) 10%, transparent);
  background: rgba(248, 251, 255, 0.92);
}

.device-replace-summary-card span {
  color: var(--text-caption);
  font-size: 12px;
}

.device-replace-summary-card strong {
  color: var(--text-heading);
  font-size: 15px;
  line-height: 1.5;
}

.device-replace-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.device-replace-grid__full {
  grid-column: 1 / -1;
}

@media (max-width: 900px) {
  .device-replace-summary-grid,
  .device-replace-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
