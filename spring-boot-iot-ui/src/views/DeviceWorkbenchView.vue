<template>
  <div class="page-stack">
    <section class="two-column-grid">
      <PanelCard
        eyebrow="Device Provisioning"
        title="新增设备"
        description="对应 `POST /device/add`，用来验证产品绑定、认证字段和设备状态初始化。"
      >
        <form class="form-grid" @submit.prevent="handleCreateDevice">
          <div class="field-group">
            <label for="device-product-key">产品 Key</label>
            <el-input id="device-product-key" v-model="deviceForm.productKey" name="device_product_key" autocomplete="off" spellcheck="false" placeholder="例如 demo-product..." clearable />
          </div>
          <div class="field-group">
            <label for="device-name">设备名称</label>
            <el-input id="device-name" v-model="deviceForm.deviceName" name="device_name" placeholder="例如 演示设备-01..." clearable />
          </div>
          <div class="field-group">
            <label for="device-code">设备编码</label>
            <el-input id="device-code" v-model="deviceForm.deviceCode" name="device_code" autocomplete="off" spellcheck="false" placeholder="例如 demo-device-01..." clearable />
          </div>
          <div class="field-group">
            <label for="device-secret">设备密钥</label>
            <el-input id="device-secret" v-model="deviceForm.deviceSecret" type="password" show-password autocomplete="off" />
          </div>
          <div class="field-group">
            <label for="client-id">客户端 ID</label>
            <el-input id="client-id" v-model="deviceForm.clientId" name="client_id" autocomplete="off" spellcheck="false" clearable />
          </div>
          <div class="field-group">
            <label for="username">用户名</label>
            <el-input id="username" v-model="deviceForm.username" name="username" autocomplete="username" spellcheck="false" clearable />
          </div>
          <div class="field-group">
            <label for="password">密码</label>
            <el-input id="password" v-model="deviceForm.password" type="password" show-password autocomplete="off" />
          </div>
          <div class="field-group">
            <label for="firmware">固件版本</label>
            <el-input id="firmware" v-model="deviceForm.firmwareVersion" name="firmware_version" autocomplete="off" spellcheck="false" placeholder="例如 1.0.0..." clearable />
          </div>
          <div class="field-group">
            <label for="ip-address">IP 地址</label>
            <el-input id="ip-address" v-model="deviceForm.ipAddress" name="ip_address" inputmode="decimal" autocomplete="off" spellcheck="false" placeholder="例如 127.0.0.1..." clearable />
          </div>
          <div class="field-group">
            <label for="address">部署位置</label>
            <el-input id="address" v-model="deviceForm.address" name="device_address" placeholder="例如 lab-a..." clearable />
          </div>
          <div class="field-group" style="grid-column: 1 / -1;">
            <label for="metadata">扩展元数据</label>
            <el-input id="metadata" v-model="deviceForm.metadataJson" name="metadata_json" type="textarea" :rows="5" spellcheck="false" />
          </div>
          <div class="button-row" style="grid-column: 1 / -1;">
            <el-button class="primary-button" type="primary" native-type="submit" :loading="isCreating">
              {{ isCreating ? '创建设备中...' : '提交设备' }}
            </el-button>
            <el-button class="secondary-button" @click="resetForm">
              恢复演示数据
            </el-button>
          </div>
        </form>
      </PanelCard>

      <PanelCard
        eyebrow="Device Query"
        title="按 ID / 编码查询设备"
        description="用于检查设备是否已建档，以及在线状态、最近上报时间是否刷新。"
      >
        <div class="form-grid">
          <div class="field-group">
            <label for="query-device-id">设备 ID</label>
            <el-input id="query-device-id" v-model="queryId" name="query_device_id" inputmode="numeric" placeholder="例如 2001..." clearable />
          </div>
          <div class="field-group">
            <label for="query-device-code">设备编码</label>
            <el-input id="query-device-code" v-model="queryCode" name="query_device_code" autocomplete="off" spellcheck="false" placeholder="例如 demo-device-01..." clearable />
          </div>
        </div>
        <div class="button-row" style="margin-top: 1rem;">
          <el-button class="primary-button" type="primary" :loading="isQueryingId" @click="handleQueryById">
            {{ isQueryingId ? '查询中...' : '按 ID 查询' }}
          </el-button>
          <el-button class="secondary-button" :loading="isQueryingCode" @click="handleQueryByCode">
            {{ isQueryingCode ? '查询中...' : '按编码查询' }}
          </el-button>
        </div>

        <div v-if="currentDevice" class="info-grid" style="margin-top: 1rem;">
          <div class="info-chip">
            <span>设备名称</span>
            <strong>{{ currentDevice.deviceName }}</strong>
          </div>
          <div class="info-chip">
            <span>设备编码</span>
            <strong>{{ currentDevice.deviceCode }}</strong>
          </div>
          <div class="info-chip">
            <span>在线状态</span>
            <strong>{{ statusLabel(currentDevice.onlineStatus) }}</strong>
          </div>
          <div class="info-chip">
            <span>最近上报</span>
            <strong>{{ formatDateTime(currentDevice.lastReportTime) }}</strong>
          </div>
          <div class="info-chip">
            <span>固件版本</span>
            <strong>{{ currentDevice.firmwareVersion || '--' }}</strong>
          </div>
          <div class="info-chip">
            <span>部署位置</span>
            <strong>{{ currentDevice.address || '--' }}</strong>
          </div>
        </div>
      </PanelCard>
    </section>

    <div v-if="errorMessage" class="empty-state" aria-live="polite">{{ errorMessage }}</div>

    <section class="two-column-grid">
      <ResponsePanel
        eyebrow="Request"
        title="最后一次请求"
        description="校验设备建档和查询接口的入参与路由变量。"
        :body="lastRequest"
      />
      <ResponsePanel
        eyebrow="Response"
        title="最后一次响应"
        description="重点观察设备状态字段与最近上报时间。"
        :body="lastResponse"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';

import { addDevice, getDeviceByCode, getDeviceById } from '../api/iot';
import PanelCard from '../components/PanelCard.vue';
import ResponsePanel from '../components/ResponsePanel.vue';
import { recordActivity } from '../stores/activity';
import type { Device, DeviceAddPayload } from '../types/api';
import { formatDateTime, statusLabel } from '../utils/format';

const createDemoDevice = (): DeviceAddPayload => ({
  productKey: 'demo-product',
  deviceName: '演示设备-01',
  deviceCode: 'demo-device-01',
  deviceSecret: '123456',
  clientId: 'demo-device-01',
  username: 'demo-device-01',
  password: '123456',
  firmwareVersion: '1.0.0',
  ipAddress: '127.0.0.1',
  address: 'lab-a',
  metadataJson: JSON.stringify({ zone: 'lab-a', protocol: 'mqtt-json', owner: 'debug-console' }, null, 2)
});

const deviceForm = reactive<DeviceAddPayload>(createDemoDevice());
const queryId = ref('2001');
const queryCode = ref('demo-device-01');

const isCreating = ref(false);
const isQueryingId = ref(false);
const isQueryingCode = ref(false);
const errorMessage = ref('');
const currentDevice = ref<Device | null>(null);
const lastRequest = ref<unknown>({ tip: '创建设备或查询设备后会显示请求体。' });
const lastResponse = ref<unknown>({ tip: '接口响应会出现在这里。' });

function resetForm() {
  Object.assign(deviceForm, createDemoDevice());
}

async function handleCreateDevice() {
  isCreating.value = true;
  errorMessage.value = '';
  lastRequest.value = { method: 'POST', path: '/device/add', body: { ...deviceForm } };

  try {
    const response = await addDevice({ ...deviceForm });
    currentDevice.value = response.data;
    lastResponse.value = response;
    queryId.value = response.data?.id ? String(response.data.id) : queryId.value;
    queryCode.value = response.data.deviceCode;
    ElMessage.success(`设备 ${response.data.deviceCode} 创建成功`);
    recordActivity({
      module: '设备工作台',
      action: '新增设备',
      request: lastRequest.value,
      response,
      ok: true,
      detail: `已创建设备 ${response.data.deviceCode}`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    lastResponse.value = { ok: false, message: errorMessage.value };
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: '设备工作台',
      action: '新增设备',
      request: lastRequest.value,
      response: { message: errorMessage.value },
      ok: false,
      detail: `创建设备失败：${errorMessage.value}`
    });
  } finally {
    isCreating.value = false;
  }
}

async function handleQueryById() {
  isQueryingId.value = true;
  errorMessage.value = '';
  lastRequest.value = { method: 'GET', path: `/device/${queryId.value}` };

  try {
    const response = await getDeviceById(queryId.value);
    currentDevice.value = response.data;
    lastResponse.value = response;
    queryCode.value = response.data.deviceCode;
    ElMessage.success(`已查询到设备 ${response.data.deviceCode}`);
    recordActivity({
      module: '设备工作台',
      action: '按 ID 查询设备',
      request: lastRequest.value,
      response,
      ok: true,
      detail: `查询到设备 ${response.data.deviceCode}`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    lastResponse.value = { ok: false, message: errorMessage.value };
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: '设备工作台',
      action: '按 ID 查询设备',
      request: lastRequest.value,
      response: { message: errorMessage.value },
      ok: false,
      detail: `按 ID 查询失败：${errorMessage.value}`
    });
  } finally {
    isQueryingId.value = false;
  }
}

async function handleQueryByCode() {
  isQueryingCode.value = true;
  errorMessage.value = '';
  lastRequest.value = { method: 'GET', path: `/device/code/${queryCode.value}` };

  try {
    const response = await getDeviceByCode(queryCode.value);
    currentDevice.value = response.data;
    lastResponse.value = response;
    queryId.value = response.data?.id ? String(response.data.id) : queryId.value;
    ElMessage.success(`已查询到设备 ${response.data.deviceCode}`);
    recordActivity({
      module: '设备工作台',
      action: '按编码查询设备',
      request: lastRequest.value,
      response,
      ok: true,
      detail: `查询到设备 ${response.data.deviceCode}`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    lastResponse.value = { ok: false, message: errorMessage.value };
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: '设备工作台',
      action: '按编码查询设备',
      request: lastRequest.value,
      response: { message: errorMessage.value },
      ok: false,
      detail: `按编码查询失败：${errorMessage.value}`
    });
  } finally {
    isQueryingCode.value = false;
  }
}
</script>
