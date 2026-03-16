<template>
  <div class="product-workbench-page">
    <!--  -->
    <div class="workbench-header">
      <div class="header-left">
        <h1 class="page-title">Ʒģ</h1>
        <span class="timestamp">{{ currentTime }}</span>
      </div>
      <div class="header-right">
        <el-radio-group v-model="currentRole" size="large">
          <el-radio-button value="field">з</el-radio-button>
          <el-radio-button value="ops">ά</el-radio-button>
          <el-radio-button value="manager"></el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- Ʒ״̬ -->
    <div class="product-banner" :class="`product-banner--${productSummary.tone}`">
      <div class="banner-content">
        <p class="banner-label">ǰƷ״̬</p>
        <strong class="banner-value">{{ productSummary.label }}</strong>
        <p class="banner-desc">{{ productSummary.description }}</p>
      </div>
      <div class="banner-score">
        <small>Ʒ</small>
        <strong>{{ productSummary.score }}</strong>
      </div>
    </div>

    <!-- ؼָ꿨Ƭ -->
    <div class="quad-grid">
      <MetricCard
        v-for="metric in roleMetrics[currentRole]"
        :key="metric.label"
        :label="metric.label"
        :value="metric.value"
        :badge="metric.badge"
      />
    </div>

    <!-- 빤 -->
    <div class="main-workarea">
      <!-- Ʒģ -->
      <div class="product-template">
        <h3 class="section-title">Ʒģ</h3>
        <div class="template-grid">
          <div class="template-card">
            <div class="template-header">
              <strong class="template-title">Ʒ</strong>
              <span class="template-tag">Provisioning</span>
            </div>
            <form class="form-grid" @submit.prevent="handleCreateProduct">
              <div class="field-group">
                <label for="product-key">Ʒ Key</label>
                <el-input
                  id="product-key"
                  v-model="productForm.productKey"
                  name="product_key"
                  placeholder=" demo-product..."
                  clearable
                />
              </div>
              <div class="field-group">
                <label for="product-name">Ʒ</label>
                <el-input
                  id="product-name"
                  v-model="productForm.productName"
                  name="product_name"
                  placeholder=" ʾƷ..."
                  clearable
                />
              </div>
              <div class="field-group">
                <label for="protocol-code">Э</label>
                <el-input
                  id="protocol-code"
                  v-model="productForm.protocolCode"
                  name="protocol_code"
                  placeholder=" mqtt-json..."
                  clearable
                />
              </div>
              <div class="field-group">
                <label for="node-type">ڵ</label>
                <el-select id="node-type" v-model="productForm.nodeType">
                  <el-option :value="1" label="1 - ֱ豸" />
                  <el-option :value="2" label="2 - 豸" />
                </el-select>
              </div>
              <div class="field-group">
                <label for="data-format">ݸʽ</label>
                <el-input id="data-format" v-model="productForm.dataFormat" name="data_format" placeholder=" JSON..." clearable />
              </div>
              <div class="field-group">
                <label for="manufacturer"></label>
                <el-input id="manufacturer" v-model="productForm.manufacturer" name="manufacturer" placeholder=" spring-boot-iot..." clearable />
              </div>
              <div class="field-group" style="grid-column: 1 / -1;">
                <label for="description">˵</label>
                <el-input id="description" v-model="productForm.description" type="textarea" :rows="3" />
              </div>
              <div class="button-row" style="grid-column: 1 / -1;">
                <el-button class="primary-button" type="primary" native-type="submit" :loading="isCreating">
                  {{ isCreating ? '...' : 'ύƷ' }}
                </el-button>
                <el-button class="secondary-button" @click="resetForm">
                  ָʾ
                </el-button>
              </div>
            </form>
          </div>

          <div class="template-card">
            <div class="template-header">
              <strong class="template-title"> ID ѯƷ</strong>
              <span class="template-tag">Lookup</span>
            </div>
            <form @submit.prevent="handleQueryProduct">
              <div class="form-grid">
                <div class="field-group">
                  <label for="query-product-id">Ʒ ID</label>
                  <el-input id="query-product-id" v-model="queryId" name="query_product_id" inputmode="numeric" placeholder=" 2001..." clearable />
                </div>
              </div>
              <div class="button-row" style="margin-top: 1rem;">
                <el-button class="primary-button" type="primary" native-type="submit" :loading="isQuerying">
                  {{ isQuerying ? 'ѯ...' : 'ѯƷ' }}
                </el-button>
              </div>
            </form>

            <div v-if="queryProduct" class="info-grid" style="margin-top: 1rem;">
              <div class="info-chip">
                <span>Ʒ Key</span>
                <strong>{{ queryProduct.productKey }}</strong>
              </div>
              <div class="info-chip">
                <span>Э</span>
                <strong>{{ queryProduct.protocolCode }}</strong>
              </div>
              <div class="info-chip">
                <span>ڵ</span>
                <strong>{{ queryProduct.nodeType }}</strong>
              </div>
              <div class="info-chip">
                <span></span>
                <strong>{{ queryProduct.manufacturer || '--' }}</strong>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ɫ -->
      <div class="role-quick-access">
        <h3 class="section-title">ɫ</h3>
        <div class="access-grid">
          <div
            v-for="action in roleActions[currentRole]"
            :key="action.title"
            class="action-card"
            @click="navigateTo(action.path)"
          >
            <div class="action-icon">{{ action.icon }}</div>
            <div class="action-content">
              <h4 class="action-title">{{ action.title }}</h4>
              <p class="action-desc">{{ action.desc }}</p>
            </div>
            <el-icon class="action-arrow"><arrow-right /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- ײϢ -->
    <div class="workbench-footer">
      <div class="footer-section">
        <h4>ǰ鶯</h4>
        <div class="action-list">
          <div
            v-for="item in productSummary.actions"
            :key="item"
            class="action-item"
          >
            <span class="action-badge">{{ productSummary.shortLabel }}</span>
            <p class="action-text">{{ item }}</p>
          </div>
        </div>
      </div>
      <div class="footer-section">
        <h4>Ʒ�����</h4>
        <div v-if="queryProduct" class="product-info-grid">
          <div class="info-chip">
            <span>Ʒ Key</span>
            <strong>{{ queryProduct.productKey }}</strong>
          </div>
          <div class="info-chip">
            <span>Ʒ</span>
            <strong>{{ queryProduct.productName }}</strong>
          </div>
          <div class="info-chip">
            <span>Э</span>
            <strong>{{ queryProduct.protocolCode }}</strong>
          </div>
          <div class="info-chip">
            <span>ڵ</span>
            <strong>{{ queryProduct.nodeType === 1 ? 'ֱ豸' : '豸' }}</strong>
          </div>
          <div class="info-chip">
            <span>ݸʽ</span>
            <strong>{{ queryProduct.dataFormat || 'JSON' }}</strong>
          </div>
          <div class="info-chip">
            <span></span>
            <strong>{{ queryProduct.manufacturer || '--' }}</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- ؼ -->
    <div class="data-panels">
      <PanelCard
        eyebrow="Request"
        title="һ"
        :body="lastRequest"
      />

      <PanelCard
        eyebrow="Response"
        title="һӦ"
        :body="lastResponse"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from '@/utils/message';
import { ArrowRight } from '@element-plus/icons-vue';

import { addProduct, getProductById } from '../api/iot';
import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import { recordActivity } from '../stores/activity';
import type { Product, ProductAddPayload } from '../types/api';

interface ProductSummary {
  score: string;
  label: string;
  shortLabel: string;
  tone: 'red' | 'orange' | 'yellow' | 'blue';
  description: string;
  actions: string[];
}

const router = useRouter();

// ɫл
const currentRole = ref<'field' | 'ops' | 'manager'>('field');

// ʱ
const currentTime = ref('');
const updateTime = () => {
  const now = new Date();
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};
setInterval(updateTime, 1000);
updateTime();

// Ʒ
const createDemoProduct = (): ProductAddPayload => ({
  productKey: 'demo-product',
  productName: 'ʾƷ',
  protocolCode: 'mqtt-json',
  nodeType: 1,
  dataFormat: 'JSON',
  manufacturer: 'spring-boot-iot',
  description: 'ǰ˵̨ĬϲƷģ'
});

const productForm = ref<ProductAddPayload>(createDemoProduct());
const queryId = ref('2001');

const isCreating = ref(false);
const isQuerying = ref(false);
const errorMessage = ref('');
const queryProduct = ref<Product | null>(null);
const lastRequest = ref<unknown>({ tip: 'ύѯʾ塣' });
const lastResponse = ref<unknown>({ tip: 'ӿӦ' });

// ƷժҪ
const productSummary = computed<ProductSummary>(() => {
  if (!queryProduct.value) {
    return {
      score: '--',
      label: '',
      shortLabel: 'NA',
      tone: 'blue',
      description: 'Ʒ ID ѯظòƷϸϢ',
      actions: ['Ʒ ID ѯƷģġ']
    };
  }

  let score = 0;
  const reasons: string[] = [];

  if (!queryProduct.value.protocolCode) {
    score += 25;
    reasons.push('ȱЭ');
  }

  if (!queryProduct.value.dataFormat) {
    score += 15;
    reasons.push('ȱݸʽ');
  }

  if (!queryProduct.value.manufacturer) {
    score += 10;
    reasons.push('ȱٳϢ');
  }

  if (!queryProduct.value.description) {
    score += 8;
    reasons.push('ȱٲƷ˵');
  }

  score = Math.min(score, 100);

  let tone: ProductSummary['tone'] = 'blue';
  let label = 'ɫƷ';
  let shortLabel = '';
  let description = 'ǰƷģʺΪĻģ塣';

  if (score >= 40) {
    tone = 'yellow';
    label = 'ɫƷ';
    shortLabel = '';
    description = 'ǰƷģڲȱʧ鲹ؼֶκٽ';
  } else if (score >= 10) {
    tone = 'orange';
    label = 'ɫƷ';
    shortLabel = '';
    description = 'ǰƷģҪصע鲹Э͸ʽá';
  }

  const actions = buildActions(tone);
  return {
    score: String(score),
    label,
    shortLabel,
    tone,
    description,
    reasons,
    actions
  };
});

// ɫָ
const roleMetrics = computed(() => [
  {
    label: 'ǰƷ״̬',
    value: productSummary.value.label,
    hint: productSummary.value.description,
    badge: {
      label: productSummary.value.shortLabel,
      tone: productSummary.value.tone === 'red'
        ? 'danger'
        : productSummary.value.tone === 'orange'
          ? 'warning'
          : productSummary.value.tone === 'yellow'
            ? 'warning'
            : 'brand'
    }
  },
  {
    label: 'Ʒ Key',
    value: queryProduct.value?.productKey || '--',
    hint: queryProduct.value?.productKey ? 'ǰƷΨһʶ' : 'ǰûвƷݡ',
    badge: { label: 'Key', tone: 'brand' }
  },
  {
    label: 'Э',
    value: queryProduct.value?.protocolCode || '--',
    hint: queryProduct.value?.protocolCode ? 'ǰƷʹõЭ롣' : 'ǰûЭ롣',
    badge: { label: 'Protocol', tone: queryProduct.value?.protocolCode ? 'success' : 'warning' }
  },
  {
    label: 'ڵ',
    value: queryProduct.value?.nodeType === 1 ? 'ֱ豸' : queryProduct.value?.nodeType === 2 ? '豸' : '--',
    hint: queryProduct.value?.nodeType === 1 ? 'ǰƷΪֱ豸͡' : 'ǰƷΪ豸͡',
    badge: { label: 'Type', tone: 'brand' }
  }
]);

// ɫ
const roleActions = {
  field: [
    { icon: '??', title: '豸ά', desc: '豸Զά', path: '/devices' },
    { icon: '??', title: 'HTTP ϱʵ', desc: 'ģ豸ϱ', path: '/reporting' },
    { icon: '??', title: 'յ㹤̨', desc: 'ռ봦', path: '/insight' },
    { icon: '??', title: '߲鿴', desc: 'ʷ', path: '/insight' }
  ],
  ops: [
    { icon: '??', title: 'ֵ', desc: 'Զ̵', path: '/devices' },
    { icon: '??', title: '豸Ѳ', desc: 'ź豸', path: '/devices' },
    { icon: '??', title: '̼', desc: 'ļ̼', path: '/file-debug' },
    { icon: '??', title: 'רⱨ', desc: 'շ봦ñ', path: '/insight' }
  ],
  manager: [
    { icon: '??', title: '̬', desc: 'λֲ', path: '/future-lab' },
    { icon: '??', title: 'ʷ', desc: '¼·', path: '/reporting' },
    { icon: '??', title: 'ݿ', desc: 'άͳƷ', path: '/future-lab' },
    { icon: '??', title: '', desc: 'AIɷ����', path: '/insight' }
  ]
};

// 
function buildActions(tone: ProductSummary['tone']) {
  const actions = ['Ⱥ˲ƷģãȷЭ͸ʽǷ'];

  if (tone === 'yellow') {
    actions.push('鲹ȱʧĹؼֶΣЭ롢ݸʽȡ');
  } else if (tone === 'orange') {
    actions.push('ȲЭݸʽá');
    actions.push('άͬ˲豸Ƿ롣');
  } else {
    actions.push('ǰƷģɼ豸');
  }

  return actions;
}

// 
const navigateTo = (path: string) => {
  router.push(path);
};

// ñ
function resetForm() {
  Object.assign(productForm.value, createDemoProduct());
}

// Ʒ
async function handleCreateProduct() {
  isCreating.value = true;
  errorMessage.value = '';
  lastRequest.value = { method: 'POST', path: '/device/product/add', body: { ...productForm.value } };

  try {
    const response = await addProduct({ ...productForm.value });
    queryProduct.value = response.data;
    lastResponse.value = response;
    if (response.data?.id) {
      queryId.value = String(response.data.id);
    }
    ElMessage.success(`Ʒ ${response.data.productKey} ɹ`);
    recordActivity({
      module: 'Ʒģ',
      action: 'Ʒ',
      request: lastRequest.value,
      response,
      ok: true,
      detail: `ѴƷ ${response.data.productKey}`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    lastResponse.value = { ok: false, message: errorMessage.value };
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: 'Ʒģ',
      action: 'Ʒ',
      request: lastRequest.value,
      response: { message: errorMessage.value },
      ok: false,
      detail: `ʧܣ${errorMessage.value}`
    });
  } finally {
    isCreating.value = false;
  }
}

// ѯƷ
async function handleQueryProduct() {
  isQuerying.value = true;
  errorMessage.value = '';
  lastRequest.value = { method: 'GET', path: `/device/product/${queryId.value}` };

  try {
    const response = await getProductById(queryId.value);
    queryProduct.value = response.data;
    lastResponse.value = response;
    ElMessage.success(`ѲѯƷ ${response.data.productKey}`);
    recordActivity({
      module: 'Ʒģ',
      action: 'ѯƷ',
      request: lastRequest.value,
      response,
      ok: true,
      detail: `ѯƷ ${response.data.productKey}`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    lastResponse.value = { ok: false, message: errorMessage.value };
    ElMessage.error(errorMessage.value);
    recordActivity({
      module: 'Ʒģ',
      action: 'ѯƷ',
      request: lastRequest.value,
      response: { message: errorMessage.value },
      ok: false,
      detail: `ѯʧܣ${errorMessage.value}`
    });
  } finally {
    isQuerying.value = false;
  }
}

// 
onMounted(() => {
  recordActivity({
    module: 'Ʒģ',
    action: 'ʹ̨',
    request: { path: '/products' },
    ok: true,
    detail: 'ûʲƷģ'
  });
});
</script>

<style scoped>
.product-workbench-page {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

/*  */
.workbench-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.97), rgba(247, 251, 255, 0.95)),
    radial-gradient(circle at 85% 20%, rgba(255, 106, 0, 0.12), transparent 30%);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.page-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.timestamp {
  font-family: var(--font-mono);
  font-size: 0.85rem;
  color: var(--brand-bright);
}

/* ɫл */
:deep(.el-radio-group) {
  --el-radio-button-checked-text-color: var(--brand-bright);
  --el-radio-button-checked-bg-color: rgba(255, 106, 0, 0.1);
  --el-radio-button-checked-border-color: var(--brand-bright);
}

:deep(.el-radio-button__inner) {
  background: #ffffff;
  border: 1px solid var(--panel-border);
  border-radius: 0.75rem;
  padding: 0.6rem 1.2rem;
  font-weight: 500;
  transition: all 180ms ease;
}

:deep(.el-radio-button__inner:hover) {
  border-color: var(--brand-bright);
  transform: translateY(-1px);
}

:deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
  background: rgba(255, 106, 0, 0.1);
  border-color: var(--brand-bright);
  box-shadow: 0 0 0 3px rgba(255, 106, 0, 0.12);
}

/* Ʒ״̬ */
.product-banner {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(82, 174, 255, 0.24);
  background:
    linear-gradient(165deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.94)),
    radial-gradient(circle at top right, rgba(30, 128, 255, 0.1), transparent 52%);
}

.banner-content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
}

.banner-label {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--text-tertiary);
  font-size: 0.76rem;
}

.banner-value {
  margin: 0;
  font-family: var(--font-display);
  font-size: 2.2rem;
  font-weight: 700;
  color: var(--text-primary);
}

.banner-desc {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.banner-score {
  text-align: right;
  min-width: 120px;
}

.banner-score small {
  display: block;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--text-tertiary);
  font-size: 0.76rem;
}

.banner-score strong {
  font-family: var(--font-display);
  font-size: 2.8rem;
  font-weight: 700;
  color: var(--text-primary);
}

.product-banner--red {
  border-color: rgba(255, 109, 109, 0.28);
}

.product-banner--red .banner-value {
  color: #ff6d6d;
}

.product-banner--orange {
  border-color: rgba(255, 179, 71, 0.28);
}

.product-banner--orange .banner-value {
  color: #ffb347;
}

.product-banner--yellow {
  border-color: rgba(255, 214, 102, 0.28);
}

.product-banner--yellow .banner-value {
  color: #ffd666;
}

.product-banner--blue {
  border-color: rgba(82, 174, 255, 0.24);
}

.product-banner--blue .banner-value {
  color: #52aaff;
}

/* Ĺָ */
.quad-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

/* 빤 */
.main-workarea {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 1rem;
}

/* Ʒģ */
.product-template {
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(165deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.94)),
    radial-gradient(circle at top right, rgba(30, 128, 255, 0.1), transparent 52%);
}

.section-title {
  margin: 0 0 1.25rem;
  font-size: 1.1rem;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.template-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.template-card {
  padding: 1.25rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
}

.template-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.template-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.template-tag {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--text-tertiary);
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  background: rgba(255, 106, 0, 0.08);
  color: var(--brand-bright);
}

.template-desc {
  margin: 0 0 1rem;
  color: var(--text-secondary);
  line-height: 1.7;
}

/* ɫ */
.role-quick-access {
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.97), rgba(247, 251, 255, 0.95)),
    radial-gradient(circle at 85% 20%, rgba(255, 106, 0, 0.12), transparent 30%);
}

.access-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
  cursor: pointer;
  transition: all 180ms ease;
}

.action-card:hover {
  border-color: var(--brand-bright);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 106, 0, 0.16);
}

.action-icon {
  width: 3rem;
  height: 3rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.75rem;
  background: rgba(255, 106, 0, 0.12);
  font-size: 1.5rem;
}

.action-content {
  flex: 1;
}

.action-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.action-desc {
  margin: 0.25rem 0 0;
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.action-arrow {
  color: var(--brand-bright);
  font-size: 1.2rem;
}

/* ײϢ */
.workbench-footer {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.97), rgba(247, 251, 255, 0.95)),
    radial-gradient(circle at 85% 20%, rgba(255, 106, 0, 0.12), transparent 30%);
}

.footer-section h4 {
  margin: 0 0 1rem;
  font-size: 0.9rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--text-tertiary);
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.action-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
}

.action-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 2.2rem;
  height: 2.2rem;
  border-radius: 0.9rem;
  background: rgba(30, 128, 255, 0.12);
  color: var(--brand-bright);
  font-family: var(--font-mono);
  flex-shrink: 0;
}

.action-text {
  margin: 0;
  font-size: 0.9rem;
  color: var(--text-secondary);
  line-height: 1.7;
}

.product-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.info-chip {
  padding: 0.9rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: #f8fbff;
}

.info-chip span {
  display: block;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--text-tertiary);
  margin-bottom: 0.25rem;
}

.info-chip strong {
  font-size: 0.95rem;
  color: var(--text-primary);
}

/* ؼ */
.data-panels {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

/* Ӧʽ */
@media (max-width: 1400px) {
  .main-workarea {
    grid-template-columns: 1fr;
  }

  .workbench-footer {
    grid-template-columns: 1fr;
  }

  .data-panels {
    grid-template-columns: 1fr;
  }

  .template-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .workbench-header {
    flex-direction: column;
    gap: 1rem;
  }

  .quad-grid,
  .access-grid {
    grid-template-columns: 1fr;
  }

  .banner-score {
    text-align: left;
  }
}
</style>


