<template>
  <div class="page-stack">
    <section class="two-column-grid">
      <PanelCard
        eyebrow="Product Provisioning"
        title="新增产品"
        description="按照后端 `POST /device/product/add` 所需字段直接构造产品模板。"
      >
        <form class="form-grid" @submit.prevent="handleCreateProduct">
          <div class="field-group">
            <label for="product-key">产品 Key</label>
            <input id="product-key" v-model="productForm.productKey" autocomplete="off" required />
          </div>
          <div class="field-group">
            <label for="product-name">产品名称</label>
            <input id="product-name" v-model="productForm.productName" autocomplete="off" required />
          </div>
          <div class="field-group">
            <label for="protocol-code">协议编码</label>
            <input id="protocol-code" v-model="productForm.protocolCode" autocomplete="off" required />
          </div>
          <div class="field-group">
            <label for="node-type">节点类型</label>
            <select id="node-type" v-model.number="productForm.nodeType">
              <option :value="1">1 - 直连设备</option>
              <option :value="2">2 - 网关设备</option>
            </select>
          </div>
          <div class="field-group">
            <label for="data-format">数据格式</label>
            <input id="data-format" v-model="productForm.dataFormat" autocomplete="off" />
          </div>
          <div class="field-group">
            <label for="manufacturer">厂商</label>
            <input id="manufacturer" v-model="productForm.manufacturer" autocomplete="off" />
          </div>
          <div class="field-group" style="grid-column: 1 / -1;">
            <label for="description">说明</label>
            <textarea id="description" v-model="productForm.description" />
          </div>
          <div class="button-row" style="grid-column: 1 / -1;">
            <button class="primary-button" type="submit" :disabled="isCreating">
              {{ isCreating ? '创建中...' : '提交产品' }}
            </button>
            <button class="secondary-button" type="button" @click="resetForm">
              恢复演示数据
            </button>
          </div>
        </form>
      </PanelCard>

      <PanelCard
        eyebrow="Product Query"
        title="按 ID 查询产品"
        description="验证产品是否已经持久化，并且字段与预期一致。"
      >
        <form @submit.prevent="handleQueryProduct">
          <div class="form-grid">
            <div class="field-group">
              <label for="query-product-id">产品 ID</label>
              <input id="query-product-id" v-model="queryId" inputmode="numeric" />
            </div>
          </div>
          <div class="button-row" style="margin-top: 1rem;">
            <button class="primary-button" type="submit" :disabled="isQuerying">
              {{ isQuerying ? '查询中...' : '查询产品' }}
            </button>
          </div>
        </form>

        <div v-if="queryProduct" class="info-grid" style="margin-top: 1rem;">
          <div class="info-chip">
            <span>产品 Key</span>
            <strong>{{ queryProduct.productKey }}</strong>
          </div>
          <div class="info-chip">
            <span>协议</span>
            <strong>{{ queryProduct.protocolCode }}</strong>
          </div>
          <div class="info-chip">
            <span>节点类型</span>
            <strong>{{ queryProduct.nodeType }}</strong>
          </div>
          <div class="info-chip">
            <span>厂商</span>
            <strong>{{ queryProduct.manufacturer || '--' }}</strong>
          </div>
        </div>
      </PanelCard>
    </section>

    <div v-if="errorMessage" class="empty-state">{{ errorMessage }}</div>

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
import { reactive, ref } from 'vue';

import { addProduct, getProductById } from '../api/iot';
import PanelCard from '../components/PanelCard.vue';
import ResponsePanel from '../components/ResponsePanel.vue';
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

function resetForm() {
  Object.assign(productForm, createDemoProduct());
}

async function handleCreateProduct() {
  isCreating.value = true;
  errorMessage.value = '';
  lastRequest.value = { method: 'POST', path: '/device/product/add', body: { ...productForm } };

  try {
    const response = await addProduct({ ...productForm });
    queryProduct.value = response.data;
    lastResponse.value = response;
    if (response.data?.id) {
      queryId.value = String(response.data.id);
    }
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
  lastRequest.value = { method: 'GET', path: `/device/product/${queryId.value}` };

  try {
    const response = await getProductById(queryId.value);
    queryProduct.value = response.data;
    lastResponse.value = response;
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
