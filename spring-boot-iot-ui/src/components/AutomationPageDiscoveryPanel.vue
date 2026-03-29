<template>
  <PanelCard
    title="页面盘点与脚手架生成"
    description="优先读取当前授权菜单盘点页面，可一键补齐未覆盖页面的自动化脚手架，也支持手工登记外部系统页面。"
  >
    <template #actions>
      <StandardActionGroup gap="sm">
        <StandardButton action="refresh" @click="$emit('refresh')">刷新盘点</StandardButton>
        <StandardButton action="batch" plain @click="$emit('select-uncovered')">勾选未覆盖</StandardButton>
        <StandardButton action="confirm" @click="$emit('generate-selected')">生成勾选场景</StandardButton>
        <StandardButton action="batch" @click="$emit('generate-uncovered')">一键生成全部未覆盖</StandardButton>
        <StandardButton action="add" @click="$emit('open-manual-page')">新增自定义页面</StandardButton>
      </StandardActionGroup>
    </template>

    <div class="quad-grid automation-page-discovery__metrics">
      <MetricCard
        v-for="metric in metrics"
        :key="metric.label"
        class="automation-page-discovery__metric-card"
        :label="metric.label"
        :value="metric.value"
        :badge="metric.badge"
      />
    </div>

    <p class="automation-page-discovery__caption">
      盘点来源：{{ inventorySourceText }}。当前会按路由去重，并把“当前计划未覆盖”的页面标记出来。
    </p>

    <StandardTableToolbar
      compact
      :meta-items="[
        `当前结果 ${pageInventory.length} 页`,
        `已覆盖 ${pageInventory.filter((item) => isRouteCovered(item.route)).length} 页`,
        `待补齐 ${pageInventory.filter((item) => !isRouteCovered(item.route)).length} 页`
      ]"
    />

    <el-table
      ref="tableRef"
      :data="pageInventory"
      row-key="id"
      size="small"
      border
      @selection-change="$emit('selection-change', $event)"
    >
      <el-table-column type="selection" width="52" reserve-selection />
      <StandardTableTextColumn prop="title" label="页面" :min-width="170" />
      <StandardTableTextColumn prop="route" label="路由" :min-width="170" />
      <el-table-column label="来源" width="100">
        <template #default="{ row }">
          <el-tag :type="row.source === 'manual' ? 'warning' : row.source === 'menu' ? 'success' : 'info'">
            {{ buildInventorySourceLabel(row.source) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="推荐模板" width="120">
        <template #default="{ row }">
          <el-tag effect="plain">{{ buildTemplateLabel(row.recommendedTemplate) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="范围" width="100">
        <template #default="{ row }">
          <el-tag effect="plain" type="info">{{ row.scope }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="覆盖" width="100">
        <template #default="{ row }">
          <el-tag :type="isRouteCovered(row.route) ? 'success' : 'warning'">
            {{ isRouteCovered(row.route) ? '已覆盖' : '待补齐' }}
          </el-tag>
        </template>
      </el-table-column>
      <StandardTableTextColumn prop="readySelector" label="就绪选择器" :min-width="180" />
      <StandardTableTextColumn prop="matcher" label="首屏接口" :min-width="180">
        <template #default="{ row }">
          <span>{{ row.matcher || '—' }}</span>
        </template>
      </StandardTableTextColumn>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <StandardRowActions v-if="row.source === 'manual'" variant="table" gap="wide">
            <StandardActionLink @click="$emit('remove-manual-page', row.id)">
              删除
            </StandardActionLink>
          </StandardRowActions>
          <span v-else>—</span>
        </template>
      </el-table-column>
    </el-table>
  </PanelCard>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import MetricCard from './MetricCard.vue';
import PanelCard from './PanelCard.vue';
import StandardActionGroup from './StandardActionGroup.vue';
import StandardTableTextColumn from './StandardTableTextColumn.vue';
import StandardTableToolbar from './StandardTableToolbar.vue';
import type { AutomationPageInventoryItem, AutomationScenarioTemplateType } from '../types/automation';

type DiscoveryMetric = {
  label: string;
  value: string;
  badge?: {
    label: string;
    tone: 'brand' | 'success' | 'warning' | 'danger';
  };
};

type InventoryTableElement = {
  clearSelection?: () => void;
  toggleRowSelection?: (row: AutomationPageInventoryItem, selected?: boolean) => void;
};

defineProps<{
  metrics: DiscoveryMetric[];
  inventorySourceText: string;
  pageInventory: AutomationPageInventoryItem[];
  buildInventorySourceLabel: (source: AutomationPageInventoryItem['source']) => string;
  buildTemplateLabel: (template: AutomationScenarioTemplateType) => string;
  isRouteCovered: (route: string) => boolean;
}>();

defineEmits<{
  refresh: [];
  'select-uncovered': [];
  'generate-selected': [];
  'generate-uncovered': [];
  'open-manual-page': [];
  'selection-change': [rows: AutomationPageInventoryItem[]];
  'remove-manual-page': [id: string];
}>();

const tableRef = ref<InventoryTableElement | null>(null);

function clearSelection() {
  tableRef.value?.clearSelection?.();
}

function toggleRowSelection(row: AutomationPageInventoryItem, selected?: boolean) {
  tableRef.value?.toggleRowSelection?.(row, selected);
}

defineExpose({
  clearSelection,
  toggleRowSelection
});
</script>

<style scoped>
.automation-page-discovery__metrics {
  margin-bottom: 1rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.automation-page-discovery__metric-card {
  min-height: 7.25rem;
}

.automation-page-discovery__caption {
  margin: 0 0 1rem;
  color: var(--text-secondary);
  line-height: 1.7;
}

@media (max-width: 1024px) {
  .automation-page-discovery__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
