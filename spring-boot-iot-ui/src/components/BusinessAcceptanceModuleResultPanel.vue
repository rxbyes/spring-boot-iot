<template>
  <PanelCard title="模块结论与失败下钻" description="先看哪些模块没过，再一键展开到具体失败步骤、接口和页面动作。">
    <div v-if="modules.length === 0" class="business-acceptance-module-result-panel__empty">
      当前没有可展示的模块结果。
    </div>
    <template v-else>
      <div class="business-acceptance-module-result-panel__module-list">
        <button
          v-for="module in modules"
          :key="module.moduleCode"
          type="button"
          class="business-acceptance-module-result-panel__module"
          :class="{
            'business-acceptance-module-result-panel__module--active': module.moduleCode === activeModuleCode
          }"
          @click="$emit('select-module', module.moduleCode)"
        >
          <div>
            <strong>{{ module.moduleName }}</strong>
            <p>{{ module.failedScenarioCount }} 个失败场景</p>
          </div>
          <span :class="['business-acceptance-module-result-panel__status', `business-acceptance-module-result-panel__status--${module.status}`]">
            {{ statusLabel(module.status) }}
          </span>
        </button>
      </div>

      <div v-if="activeModule" class="business-acceptance-module-result-panel__detail">
        <div class="business-acceptance-module-result-panel__detail-header">
          <div>
            <h3>{{ activeModule.moduleName }}</h3>
            <p>建议方向：{{ activeModule.suggestedDirection || 'needsReview' }}</p>
          </div>
          <StandardButton action="query" text @click="toggleExpandAll">
            {{ expandAll ? '收起全部明细' : '展开全部明细' }}
          </StandardButton>
        </div>

        <div v-if="activeModule.failureDetails.length === 0" class="business-acceptance-module-result-panel__empty">
          当前模块没有失败明细。
        </div>
        <div
          v-if="activeModule.diagnosis"
          class="business-acceptance-module-result-panel__diagnosis"
        >
          <article>
            <h4>主分类</h4>
            <p>{{ activeModule.diagnosis.category || '其他' }}</p>
          </article>
          <article>
            <h4>判断理由</h4>
            <p>{{ activeModule.diagnosis.reason || '未命中已知规则，建议查看原始证据' }}</p>
          </article>
          <article>
            <h4>证据摘要</h4>
            <p>{{ activeModule.diagnosis.evidenceSummary || '未记录证据摘要' }}</p>
          </article>
        </div>
        <details
          v-for="detail in activeModule.failureDetails"
          :key="`${activeModule.moduleCode}-${detail.scenarioId}`"
          class="business-acceptance-module-result-panel__failure"
          :open="expandAll"
        >
          <summary>
            <strong>{{ detail.scenarioTitle }}</strong>
            <span>{{ detail.summary || '查看失败说明' }}</span>
          </summary>
          <div class="business-acceptance-module-result-panel__failure-grid">
            <article>
              <h4>失败步骤</h4>
              <p>{{ detail.stepLabel || '未记录' }}</p>
            </article>
            <article>
              <h4>接口</h4>
              <p>{{ detail.apiRef || '未记录' }}</p>
            </article>
            <article>
              <h4>页面动作</h4>
              <p>{{ detail.pageAction || '未记录' }}</p>
            </article>
            <article>
              <h4>摘要</h4>
              <p>{{ detail.summary || '未记录' }}</p>
            </article>
          </div>
        </details>
      </div>
    </template>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { BusinessAcceptanceModuleResult, BusinessAcceptanceStatus } from '@/types/businessAcceptance';
import PanelCard from './PanelCard.vue';
import StandardButton from './StandardButton.vue';

const props = defineProps<{
  modules: BusinessAcceptanceModuleResult[];
  activeModuleCode: string;
}>();

defineEmits<{
  (event: 'select-module', moduleCode: string): void;
}>();

const expandAll = ref(false);

const activeModule = computed(
  () => props.modules.find((item) => item.moduleCode === props.activeModuleCode) || props.modules[0] || null
);

function statusLabel(status: BusinessAcceptanceStatus) {
  if (status === 'passed') {
    return '通过';
  }
  if (status === 'blocked') {
    return '阻塞';
  }
  return '未通过';
}

function toggleExpandAll() {
  expandAll.value = !expandAll.value;
}
</script>

<style scoped>
.business-acceptance-module-result-panel__module-list {
  display: grid;
  gap: 0.7rem;
}

.business-acceptance-module-result-panel__module {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  width: 100%;
  padding: 0.9rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.98);
  text-align: left;
}

.business-acceptance-module-result-panel__module--active {
  border-color: color-mix(in srgb, var(--brand) 34%, var(--panel-border));
  box-shadow: var(--shadow-card);
}

.business-acceptance-module-result-panel__module strong {
  color: var(--text-heading);
}

.business-acceptance-module-result-panel__module p {
  margin: 0.28rem 0 0;
  color: var(--text-tertiary);
  font-size: 0.84rem;
}

.business-acceptance-module-result-panel__status {
  padding: 0.3rem 0.7rem;
  border-radius: var(--radius-pill);
  font-size: 0.8rem;
  font-weight: 700;
}

.business-acceptance-module-result-panel__status--passed {
  background: color-mix(in srgb, var(--success, #31a36a) 10%, white);
  color: color-mix(in srgb, var(--success, #31a36a) 78%, var(--text-heading));
}

.business-acceptance-module-result-panel__status--failed {
  background: color-mix(in srgb, var(--danger, #d84f45) 10%, white);
  color: color-mix(in srgb, var(--danger, #d84f45) 78%, var(--text-heading));
}

.business-acceptance-module-result-panel__status--blocked {
  background: color-mix(in srgb, var(--warning, #d69332) 13%, white);
  color: color-mix(in srgb, var(--warning, #d69332) 78%, var(--text-heading));
}

.business-acceptance-module-result-panel__detail {
  margin-top: 1rem;
  display: grid;
  gap: 0.9rem;
}

.business-acceptance-module-result-panel__detail-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.business-acceptance-module-result-panel__detail-header h3 {
  margin: 0;
  color: var(--text-heading);
}

.business-acceptance-module-result-panel__detail-header p {
  margin: 0.32rem 0 0;
  color: var(--text-tertiary);
}

.business-acceptance-module-result-panel__failure {
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--brand) 4%, white);
  padding: 0.95rem 1rem;
}

.business-acceptance-module-result-panel__diagnosis {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.business-acceptance-module-result-panel__diagnosis article {
  padding: 0.85rem 0.9rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.94);
}

.business-acceptance-module-result-panel__diagnosis h4 {
  margin: 0 0 0.42rem;
  color: var(--text-tertiary);
  font-size: 0.8rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.business-acceptance-module-result-panel__diagnosis p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.business-acceptance-module-result-panel__failure summary {
  display: grid;
  gap: 0.32rem;
  cursor: pointer;
  color: var(--text-heading);
}

.business-acceptance-module-result-panel__failure summary span {
  color: var(--text-secondary);
  font-size: 0.88rem;
}

.business-acceptance-module-result-panel__failure-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
  margin-top: 0.9rem;
}

.business-acceptance-module-result-panel__failure-grid article {
  padding: 0.85rem;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.92);
}

.business-acceptance-module-result-panel__failure-grid h4 {
  margin: 0 0 0.42rem;
  color: var(--text-tertiary);
  font-size: 0.8rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.business-acceptance-module-result-panel__failure-grid p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.business-acceptance-module-result-panel__empty {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--brand) 4%, white);
  color: var(--text-secondary);
}

@media (max-width: 900px) {
  .business-acceptance-module-result-panel__diagnosis,
  .business-acceptance-module-result-panel__failure-grid {
    grid-template-columns: 1fr;
  }

  .business-acceptance-module-result-panel__detail-header {
    flex-direction: column;
  }
}
</style>
