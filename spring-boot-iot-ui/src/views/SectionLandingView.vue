<template>
  <div class="section-landing page-stack">
    <template v-if="accessibleCards.length">
      <IotAccessPageShell :title="config?.title || '接入智维'">
        <template #actions>
          <div v-if="introActions.length" class="section-landing__intro-actions">
            <RouterLink
              v-for="action in introActions"
              :key="`${action.label}-${action.to}`"
              :to="action.to"
              class="section-landing__intro-action"
              :class="action.variant === 'primary' ? 'section-landing__intro-action--primary' : ''"
            >
              {{ action.label }}
            </RouterLink>
          </div>
        </template>
      </IotAccessPageShell>

      <IotAccessTabWorkspace :items="landingTabs" default-key="asset">
        <template #default="{ activeKey }">
          <StandardWorkbenchPanel
            title="页面入口"
            description="先筛入口，再进入对应业务页。"
            show-filters
          >
            <template #filters>
              <StandardListFilterHeader :model="{ keyword: landingKeyword }">
                <template #primary>
                  <el-form-item>
                    <el-input
                      v-model="landingKeyword"
                      placeholder="搜索页面名称或职责关键词"
                      clearable
                      prefix-icon="Search"
                    />
                  </el-form-item>
                </template>
              </StandardListFilterHeader>
            </template>

            <div class="section-landing__entry-list">
              <RouterLink
                v-for="card in groupedCards[activeKey] || []"
                :key="card.path"
                :to="card.path"
                class="section-landing__entry-item"
              >
                <strong>{{ card.label }}</strong>
                <span>{{ card.description }}</span>
              </RouterLink>
            </div>
          </StandardWorkbenchPanel>
        </template>
      </IotAccessTabWorkspace>
    </template>

    <PanelCard
      v-else
      eyebrow="Access Status"
      title="当前账号暂无可用入口"
      description="当前账号尚未配置该分组下的页面权限，请联系管理员确认菜单授权。"
    >
      <EmptyState
        title="暂无分组入口"
        description="当前账号没有该一级分组下的可用页面，暂时无法进入具体能力页。"
      />
    </PanelCard>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

import EmptyState from '../components/EmptyState.vue';
import PanelCard from '../components/PanelCard.vue';
import StandardListFilterHeader from '../components/StandardListFilterHeader.vue';
import StandardWorkbenchPanel from '../components/StandardWorkbenchPanel.vue';
import IotAccessPageShell from '../components/iotAccess/IotAccessPageShell.vue';
import IotAccessTabWorkspace from '../components/iotAccess/IotAccessTabWorkspace.vue';
import { getSectionHomeConfigByPath, sortByPreferredPaths } from '../utils/sectionWorkspaces';
import { usePermissionStore } from '../stores/permission';

const landingTabs = [
  { key: 'asset', label: '资产底座' },
  { key: 'diagnostics', label: '诊断排障' }
];

const route = useRoute();
const permissionStore = usePermissionStore();
const landingKeyword = ref('');

const config = computed(() => getSectionHomeConfigByPath(route.path));
const accessibleCards = computed(() => {
  const cards = config.value?.cards || [];
  const allowedCards = cards.filter((card) => permissionStore.hasRoutePermission(card.path));
  return sortByPreferredPaths(allowedCards, permissionStore.roleProfile.featuredPaths);
});
const assetPaths = new Set(['/products', '/devices']);
const filteredCards = computed(() => {
  const keyword = landingKeyword.value.trim().toLowerCase();
  if (!keyword) {
    return accessibleCards.value;
  }

  return accessibleCards.value.filter((card) =>
    [card.label, card.description, ...(card.keywords || [])]
      .join(' ')
      .toLowerCase()
      .includes(keyword)
  );
});
const groupedCards = computed(() => ({
  asset: filteredCards.value.filter((card) => assetPaths.has(card.path)),
  diagnostics: filteredCards.value.filter((card) => !assetPaths.has(card.path))
}));
const primaryCard = computed(() => accessibleCards.value[0] || null);
const secondaryCard = computed(() => accessibleCards.value[1] || null);
const primaryEntryAction = computed(() => {
  if (!primaryCard.value) {
    return null;
  }
  return {
    label: `进入 ${primaryCard.value.label}`,
    to: primaryCard.value.path
  };
});
const secondaryEntryAction = computed(() => {
  if (!secondaryCard.value) {
    return null;
  }
  return {
    label: secondaryCard.value.label,
    to: secondaryCard.value.path
  };
});
const introActions = computed(() => {
  const actions: Array<{ label: string; to: string; variant: 'primary' | 'secondary' }> = [];
  if (primaryCard.value) {
    actions.push({
      label: primaryCard.value.label,
      to: primaryCard.value.path,
      variant: 'primary'
    });
  }
  if (secondaryEntryAction.value) {
    actions.push({
      ...secondaryEntryAction.value,
      variant: 'secondary'
    });
  }
  return actions;
});
</script>

<style scoped>
.section-landing {
  display: grid;
  gap: 1rem;
}

.section-landing__intro-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.65rem;
}

.section-landing__intro-action {
  display: inline-flex;
  align-items: center;
  min-height: 2.5rem;
  padding: 0 0.92rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--panel-border);
  background: linear-gradient(180deg, #ffffff, #f7f9fc);
  color: var(--text-secondary);
  text-decoration: none;
  font-weight: 600;
}

.section-landing__intro-action--primary {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, var(--brand), var(--brand-bright));
  box-shadow: var(--shadow-brand);
}

.section-landing__entry-list {
  display: grid;
  gap: 0.8rem;
}

.section-landing__entry-item {
  display: grid;
  gap: 0.35rem;
  padding: 0.95rem 1rem;
  border-radius: calc(var(--radius-lg) + 2px);
  border: 1px solid var(--panel-border);
  text-decoration: none;
  color: inherit;
  background: #fff;
  box-shadow: var(--shadow-card-soft);
}

.section-landing__entry-item strong {
  color: var(--text-heading);
}

.section-landing__entry-item span {
  margin: 0;
  color: var(--text-caption);
  line-height: 1.6;
  font-size: 0.8rem;
}

@media (max-width: 900px) {
  .section-landing__intro-actions {
    justify-content: flex-start;
  }
}
</style>
