<template>
  <div class="page-stack">
    <section class="two-column-grid">
      <PanelCard
        class="product-card"
        eyebrow="Product Provisioning"
        title="新增产品"
        description="按照后端 `POST /api/device/product/add` 所需字段直接构造产品模板。"
      >
        <form class="form-grid" @submit.prevent="handleCreateProduct">
          <div class="field-group">
            <label for="product-key">产品 Key</label>
            <el-input
              id="product-key"
              v-model="productForm.productKey"
              name="product_key"
              placeholder="例如 demo-product..."
              clearable
            />
          </div>
          <div class="field-group">
            <label for="product-name">产品名称</label>
            <el-input
              id="product-name"
              v-model="productForm.productName"
              name="product_name"
              placeholder="例如 演示产品..."
              clearable
            />
          </div>
          <div class="field-group">
            <label for="protocol-code">协议编码</label>
            <el-input
              id="protocol-code"
              v-model="productForm.protocolCode"
              name="protocol_code"
              placeholder="例如 mqtt-json..."
              clearable
            />
          </div>
          <div class="field-group">
            <label for="node-type">节点类型</label>
            <el-select id="node-type" v-model="productForm.nodeType">
              <el-option :value="1" label="1 - 直连设备" />
              <el-option :value="2" label="2 - 网关设备" />
            </el-select>
          </div>
          <div class="field-group">
            <label for="data-format">数据格式</label>
            <el-input id="data-format" v-model="productForm.dataFormat" name="data_format" placeholder="例如 JSON..." clearable />
          </div>
          <div class="field-group">
            <label for="manufacturer">厂商</label>
            <el-input id="manufacturer" v-model="productForm.manufacturer" name="manufacturer" placeholder="例如 Codex..." clearable />
          </div>
          <div class="field-group" style="grid-column: 1 / -1;">
            <label for="description">说明</label>
            <el-input id="description" v-model="productForm.description" type="textarea" :rows="5" />
          </div>
          <StandardActionGroup full-width>
            <el-button class="primary-button" type="primary" native-type="submit" :loading="isCreating">
              {{ isCreating ? '创建中...' : '提交产品' }}
            </el-button>
            <el-button class="secondary-button" @click="resetForm">
              恢复演示数据
            </el-button>
          </StandardActionGroup>
        </form>
      </PanelCard>

      <PanelCard
        class="product-card"
        eyebrow="Product Query"
        title="按 ID 查询产品"
        description="验证产品是否已经持久化，并且字段与预期一致。"
      >
        <form @submit.prevent="handleQueryProduct">
          <div class="form-grid">
            <div class="field-group">
              <label for="query-product-id">产品 ID</label>
              <el-input id="query-product-id" v-model="queryId" name="query_product_id" inputmode="numeric" placeholder="例如 2001..." clearable />
            </div>
          </div>
          <StandardActionGroup margin-top="sm">
            <el-button class="primary-button" type="primary" native-type="submit" :loading="isQuerying">
              {{ isQuerying ? '查询中...' : '查询产品' }}
            </el-button>
          </StandardActionGroup>
        </form>

        <StandardInfoGrid
          v-if="queryProduct"
          :items="queryProductInfoItems"
          style="margin-top: 1rem;"
        />
      </PanelCard>
    </section>

    <div v-if="errorMessage" class="empty-state" aria-live="polite">{{ errorMessage }}</div>

    <section class="two-column-grid">
      <ResponsePanel
        eyebrow="Request"
        title="最后一次请求"
        description="便于核对与后端 DTO 的字段命名和请求结构。"
        :body="lastRequest"
      />
      <ResponsePanel
        eyebrow="Response"
        title="最后一次响应"
        description="这里保留统一响应结构，方便排查 `R<T>` 返回内容。"
        :body="lastResponse"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';

import { addProduct, getProductById } from '../api/iot';
import PanelCard from '../components/PanelCard.vue';
import ResponsePanel from '../components/ResponsePanel.vue';
import StandardActionGroup from '../components/StandardActionGroup.vue';
import StandardInfoGrid from '../components/StandardInfoGrid.vue';
import { recordActivity } from '../stores/activity';
import type { Product, ProductAddPayload } from '../types/api';

const createDemoProduct = (): ProductAddPayload => ({
  productKey: 'demo-product',
  productName: '演示产品',
  protocolCode: 'mqtt-json',
  nodeType: 1,
  dataFormat: 'JSON',
  manufacturer: 'spring-boot-iot',
  description: '用于前端调试台联调的默认产品模板'
});

const productForm = reactive<ProductAddPayload>(createDemoProduct());
const queryId = ref('2001');

const isCreating = ref(false);
const isQuerying = ref(false);
const errorMessage = ref('');
const queryProduct = ref<Product | null>(null);
const lastRequest = ref<unknown>({ tip: '提交或查询后会显示请求体。' });
const lastResponse = ref<unknown>({ tip: '接口响应会出现在这里。' });

const queryProductInfoItems = computed(() => {
  if (!queryProduct.value) {
    return []
  }

  return [
    {
      key: 'product-key',
      label: '产品 Key',
      value: queryProduct.value.productKey
    },
    {
      key: 'protocol-code',
      label: '协议',
      value: queryProduct.value.protocolCode
    },
    {
      key: 'node-type',
      label: '节点类型',
      value: queryProduct.value.nodeType
    },
    {
      key: 'manufacturer',
      label: '厂商',
      value: queryProduct.value.manufacturer
    }
  ]
})

function resetForm() {
  Object.assign(productForm, createDemoProduct());
}

async function handleCreateProduct() {
  isCreating.value = true;
  errorMessage.value = '';
  lastRequest.value = { method: 'POST', path: '/api/device/product/add', body: { ...productForm } };

  try {
    const response = await addProduct({ ...productForm });
    queryProduct.value = response.data;
    lastResponse.value = response;
    if (response.data?.id) {
      queryId.value = String(response.data.id);
    }
    ElMessage.success(`产品 ${response.data.productKey} 创建成功`);
    recordActivity({
      module: '产品工作台',
      action: '新增产品',
      request: lastRequest.value,
      response,
      ok: true,
      detail: `已创建产品 ${response.data.productKey}`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    lastResponse.value = { ok: false, message: errorMessage.value };
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: '产品工作台',
      action: '新增产品',
      request: lastRequest.value,
      response: { message: errorMessage.value },
      ok: false,
      detail: `创建失败：${errorMessage.value}`
    });
  } finally {
    isCreating.value = false;
  }
}

async function handleQueryProduct() {
  isQuerying.value = true;
  errorMessage.value = '';
  lastRequest.value = { method: 'GET', path: `/api/device/product/${queryId.value}` };

  try {
    const response = await getProductById(queryId.value);
    queryProduct.value = response.data;
    lastResponse.value = response;
    ElMessage.success(`已查询到产品 ${response.data.productKey}`);
    recordActivity({
      module: '产品工作台',
      action: '查询产品',
      request: lastRequest.value,
      response,
      ok: true,
      detail: `查询到产品 ${response.data.productKey}`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    lastResponse.value = { ok: false, message: errorMessage.value };
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: '产品工作台',
      action: '查询产品',
      request: lastRequest.value,
      response: { message: errorMessage.value },
      ok: false,
      detail: `查询失败：${errorMessage.value}`
    });
  } finally {
    isQuerying.value = false;
  }
}
</script>

<style scoped>
.product-card {
  position: relative;
  overflow: hidden;
}

.product-card::after {
  content: '';
  position: absolute;
  inset: auto -6rem -7rem auto;
  width: 16rem;
  height: 16rem;
  background: radial-gradient(circle, color-mix(in srgb, var(--brand) 12%, transparent), transparent 62%);
  pointer-events: none;
}
</style>
