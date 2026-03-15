<template>
  <div class="product-workbench-page">
    <!-- 顶部导航栏 -->
    <div class="workbench-header">
      <div class="header-left">
        <h1 class="page-title">产品模板中心</h1>
        <span class="timestamp">{{ currentTime }}</span>
      </div>
      <div class="header-right">
        <el-radio-group v-model="currentRole" size="large">
          <el-radio-button value="field">研发</el-radio-button>
          <el-radio-button value="ops">运维</el-radio-button>
          <el-radio-button value="manager">管理</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 产品状态横幅 -->
    <div class="product-banner" :class="`product-banner--${productSummary.tone}`">
      <div class="banner-content">
        <p class="banner-label">当前产品状态</p>
        <strong class="banner-value">{{ productSummary.label }}</strong>
        <p class="banner-desc">{{ productSummary.description }}</p>
      </div>
      <div class="banner-score">
        <small>产品评分</small>
        <strong>{{ productSummary.score }}</strong>
      </div>
    </div>

    <!-- 关键指标卡片 -->
    <div class="quad-grid">
      <MetricCard
        v-for="metric in roleMetrics[currentRole]"
        :key="metric.label"
        :label="metric.label"
        :value="metric.value"
        :badge="metric.badge"
      />
    </div>

    <!-- 中央工作区域 -->
    <div class="main-workarea">
      <!-- 产品模板区域 -->
      <div class="product-template">
        <h3 class="section-title">产品模板配置</h3>
        <div class="template-grid">
          <div class="template-card">
            <div class="template-header">
              <strong class="template-title">新增产品</strong>
              <span class="template-tag">Provisioning</span>
            </div>
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
                <el-input id="manufacturer" v-model="productForm.manufacturer" name="manufacturer" placeholder="例如 spring-boot-iot..." clearable />
              </div>
              <div class="field-group" style="grid-column: 1 / -1;">
                <label for="description">说明</label>
                <el-input id="description" v-model="productForm.description" type="textarea" :rows="3" />
              </div>
              <div class="button-row" style="grid-column: 1 / -1;">
                <el-button class="primary-button" type="primary" native-type="submit" :loading="isCreating">
                  {{ isCreating ? '创建中...' : '提交产品' }}
                </el-button>
                <el-button class="secondary-button" @click="resetForm">
                  恢复演示数据
                </el-button>
              </div>
            </form>
          </div>

          <div class="template-card">
            <div class="template-header">
              <strong class="template-title">按 ID 查询产品</strong>
              <span class="template-tag">Lookup</span>
            </div>
            <form @submit.prevent="handleQueryProduct">
              <div class="form-grid">
                <div class="field-group">
                  <label for="query-product-id">产品 ID</label>
                  <el-input id="query-product-id" v-model="queryId" name="query_product_id" inputmode="numeric" placeholder="例如 2001..." clearable />
                </div>
              </div>
              <div class="button-row" style="margin-top: 1rem;">
                <el-button class="primary-button" type="primary" native-type="submit" :loading="isQuerying">
                  {{ isQuerying ? '查询中...' : '查询产品' }}
                </el-button>
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
          </div>
        </div>
      </div>

      <!-- 角色快捷入口 -->
      <div class="role-quick-access">
        <h3 class="section-title">角色快捷入口</h3>
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

    <!-- 底部信息 -->
    <div class="workbench-footer">
      <div class="footer-section">
        <h4>当前建议动作</h4>
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
        <h4>产品基础档案</h4>
        <div v-if="queryProduct" class="product-info-grid">
          <div class="info-chip">
            <span>产品 Key</span>
            <strong>{{ queryProduct.productKey }}</strong>
          </div>
          <div class="info-chip">
            <span>产品名称</span>
            <strong>{{ queryProduct.productName }}</strong>
          </div>
          <div class="info-chip">
            <span>协议编码</span>
            <strong>{{ queryProduct.protocolCode }}</strong>
          </div>
          <div class="info-chip">
            <span>节点类型</span>
            <strong>{{ queryProduct.nodeType === 1 ? '直连设备' : '网关设备' }}</strong>
          </div>
          <div class="info-chip">
            <span>数据格式</span>
            <strong>{{ queryProduct.dataFormat || 'JSON' }}</strong>
          </div>
          <div class="info-chip">
            <span>厂商</span>
            <strong>{{ queryProduct.manufacturer || '--' }}</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- 关键数据面板 -->
    <div class="data-panels">
      <PanelCard
        eyebrow="Request"
        title="最后一次请求"
        :body="lastRequest"
      />

      <PanelCard
        eyebrow="Response"
        title="最后一次响应"
        :body="lastResponse"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
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

// 角色切换
const currentRole = ref<'field' | 'ops' | 'manager'>('field');

// 时间戳
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

// 产品表单
const createDemoProduct = (): ProductAddPayload => ({
  productKey: 'demo-product',
  productName: '演示产品',
  protocolCode: 'mqtt-json',
  nodeType: 1,
  dataFormat: 'JSON',
  manufacturer: 'spring-boot-iot',
  description: '用于前端调试台联调的默认产品模板'
});

const productForm = ref<ProductAddPayload>(createDemoProduct());
const queryId = ref('2001');

const isCreating = ref(false);
const isQuerying = ref(false);
const errorMessage = ref('');
const queryProduct = ref<Product | null>(null);
const lastRequest = ref<unknown>({ tip: '提交或查询后会显示请求体。' });
const lastResponse = ref<unknown>({ tip: '接口响应会出现在这里。' });

// 产品摘要计算
const productSummary = computed<ProductSummary>(() => {
  if (!queryProduct.value) {
    return {
      score: '--',
      label: '待加载',
      shortLabel: 'NA',
      tone: 'blue',
      description: '请输入产品 ID 并查询，加载该产品的详细信息。',
      actions: ['输入产品 ID 并查询产品模板中心。']
    };
  }

  let score = 0;
  const reasons: string[] = [];

  if (!queryProduct.value.protocolCode) {
    score += 25;
    reasons.push('缺少协议编码');
  }

  if (!queryProduct.value.dataFormat) {
    score += 15;
    reasons.push('缺少数据格式');
  }

  if (!queryProduct.value.manufacturer) {
    score += 10;
    reasons.push('缺少厂商信息');
  }

  if (!queryProduct.value.description) {
    score += 8;
    reasons.push('缺少产品说明');
  }

  score = Math.min(score, 100);

  let tone: ProductSummary['tone'] = 'blue';
  let label = '蓝色产品';
  let shortLabel = '蓝';
  let description = '当前产品模板配置完整，适合作为开发和联调的基础模板。';

  if (score >= 40) {
    tone = 'yellow';
    label = '黄色产品';
    shortLabel = '黄';
    description = '当前产品模板存在部分缺失，建议补充关键字段后再进行联调。';
  } else if (score >= 10) {
    tone = 'orange';
    label = '橙色产品';
    shortLabel = '橙';
    description = '当前产品模板需要重点关注，建议补充协议和格式配置。';
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

// 角色指标
const roleMetrics = computed(() => [
  {
    label: '当前产品状态',
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
    label: '产品 Key',
    value: queryProduct.value?.productKey || '--',
    hint: queryProduct.value?.productKey ? '当前产品唯一标识符。' : '当前没有产品数据。',
    badge: { label: 'Key', tone: 'brand' }
  },
  {
    label: '协议编码',
    value: queryProduct.value?.protocolCode || '--',
    hint: queryProduct.value?.protocolCode ? '当前产品使用的协议编码。' : '当前没有协议编码。',
    badge: { label: 'Protocol', tone: queryProduct.value?.protocolCode ? 'success' : 'warning' }
  },
  {
    label: '节点类型',
    value: queryProduct.value?.nodeType === 1 ? '直连设备' : queryProduct.value?.nodeType === 2 ? '网关设备' : '--',
    hint: queryProduct.value?.nodeType === 1 ? '当前产品为直连设备类型。' : '当前产品为网关设备类型。',
    badge: { label: 'Type', tone: 'brand' }
  }
]);

// 角色快捷入口
const roleActions = {
  field: [
    { icon: '🔧', title: '设备运维中心', desc: '设备建档与远程运维', path: '/devices' },
    { icon: '📡', title: 'HTTP 上报实验', desc: '模拟设备上报测试', path: '/reporting' },
    { icon: '📊', title: '风险点工作台', desc: '风险监测与处置', path: '/insight' },
    { icon: '📈', title: '趋势曲线查看', desc: '分析属性与历史趋势', path: '/insight' }
  ],
  ops: [
    { icon: '⚙️', title: '阈值管理', desc: '参数配置与远程调整', path: '/devices' },
    { icon: '🔋', title: '设备巡检', desc: '离线与弱信号设备', path: '/devices' },
    { icon: '💾', title: '固件调试', desc: '文件与固件升级', path: '/file-debug' },
    { icon: '📋', title: '专题报告', desc: '风险分析与处置报告', path: '/insight' }
  ],
  manager: [
    { icon: '🌍', title: '区域态势', desc: '点位分布与风险热力', path: '/future-lab' },
    { icon: '🔍', title: '历史回溯', desc: '事件链路与审计', path: '/reporting' },
    { icon: '📈', title: '数据看板', desc: '多维度统计分析', path: '/future-lab' },
    { icon: '📄', title: '报告生成', desc: 'AI辅助生成分析报告', path: '/insight' }
  ]
};

// 构建动作
function buildActions(tone: ProductSummary['tone']) {
  const actions = ['先核查产品模板配置，确认协议和格式是否完整。'];

  if (tone === 'yellow') {
    actions.push('建议补充缺失的关键字段，如协议编码、数据格式等。');
  } else if (tone === 'orange') {
    actions.push('建议优先补充协议编码和数据格式配置。');
    actions.push('运维侧同步核查设备是否能正常接入。');
  } else {
    actions.push('当前产品模板配置完整，可继续进行设备联调。');
  }

  return actions;
}

// 导航
const navigateTo = (path: string) => {
  router.push(path);
};

// 重置表单
function resetForm() {
  Object.assign(productForm.value, createDemoProduct());
}

// 创建产品
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
    ElMessage.success(`产品 ${response.data.productKey} 创建成功`);
    recordActivity({
      module: '产品模板中心',
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
      module: '产品模板中心',
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

// 查询产品
async function handleQueryProduct() {
  isQuerying.value = true;
  errorMessage.value = '';
  lastRequest.value = { method: 'GET', path: `/device/product/${queryId.value}` };

  try {
    const response = await getProductById(queryId.value);
    queryProduct.value = response.data;
    lastResponse.value = response;
    ElMessage.success(`已查询到产品 ${response.data.productKey}`);
    recordActivity({
      module: '产品模板中心',
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
      module: '产品模板中心',
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

// 生命周期
onMounted(() => {
  recordActivity({
    module: '产品模板中心',
    action: '访问工作台',
    request: { path: '/products' },
    ok: true,
    detail: '用户访问产品模板中心'
  });
});
</script>

<style scoped>
.product-workbench-page {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

/* 顶部导航栏 */
.workbench-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(140deg, rgba(8, 13, 28, 0.95), rgba(5, 9, 18, 0.88)),
    radial-gradient(circle at 85% 20%, rgba(57, 241, 255, 0.16), transparent 28%);
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

/* 角色切换 */
:deep(.el-radio-group) {
  --el-radio-button-checked-text-color: var(--brand-bright);
  --el-radio-button-checked-bg-color: rgba(57, 241, 255, 0.1);
  --el-radio-button-checked-border-color: var(--brand-bright);
}

:deep(.el-radio-button__inner) {
  background: rgba(8, 13, 26, 0.9);
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
  background: rgba(57, 241, 255, 0.1);
  border-color: var(--brand-bright);
  box-shadow: 0 0 12px rgba(57, 241, 255, 0.3);
}

/* 产品状态横幅 */
.product-banner {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(82, 174, 255, 0.24);
  background:
    linear-gradient(160deg, rgba(10, 18, 38, 0.94), rgba(7, 12, 25, 0.88)),
    radial-gradient(circle at top right, rgba(44, 227, 255, 0.12), transparent 50%);
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

/* 四宫格指标 */
.quad-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

/* 中央工作区域 */
.main-workarea {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 1rem;
}

/* 产品模板区域 */
.product-template {
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(160deg, rgba(10, 18, 38, 0.94), rgba(7, 12, 25, 0.88)),
    radial-gradient(circle at top right, rgba(44, 227, 255, 0.12), transparent 50%);
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
  background: rgba(7, 12, 22, 0.88);
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
  background: rgba(57, 241, 255, 0.08);
  color: var(--brand-bright);
}

.template-desc {
  margin: 0 0 1rem;
  color: var(--text-secondary);
  line-height: 1.7;
}

/* 角色快捷入口 */
.role-quick-access {
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(140deg, rgba(8, 13, 28, 0.95), rgba(5, 9, 18, 0.88)),
    radial-gradient(circle at 85% 20%, rgba(57, 241, 255, 0.16), transparent 28%);
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
  background: rgba(7, 12, 22, 0.88);
  cursor: pointer;
  transition: all 180ms ease;
}

.action-card:hover {
  border-color: var(--brand-bright);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(57, 241, 255, 0.15);
}

.action-icon {
  width: 3rem;
  height: 3rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.75rem;
  background: rgba(57, 241, 255, 0.12);
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

/* 底部信息 */
.workbench-footer {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(140deg, rgba(8, 13, 28, 0.95), rgba(5, 9, 18, 0.88)),
    radial-gradient(circle at 85% 20%, rgba(57, 241, 255, 0.16), transparent 28%);
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
  background: rgba(7, 12, 22, 0.88);
}

.action-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 2.2rem;
  height: 2.2rem;
  border-radius: 0.9rem;
  background: rgba(43, 227, 255, 0.12);
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
  background: rgba(7, 12, 22, 0.88);
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

/* 关键数据面板 */
.data-panels {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

/* 响应式 */
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
