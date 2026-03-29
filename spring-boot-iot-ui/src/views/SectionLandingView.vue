<template>
  <div class="section-landing page-stack">
    <template v-if="accessibleCards.length">
      <IotAccessPageShell
        show-breadcrumbs
        :breadcrumbs="[{ label: config?.title || '接入智维' }]"
        :show-title="false"
      />

      <IotAccessTabWorkspace :items="landingTabs" default-key="asset" :sync-query="false">
        <template #default="{ activeKey }">
          <StandardWorkbenchPanel
            title="页面入口"
            description="按职责筛选入口，再进入对应业务页。"
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

            <div v-if="groupedCards[activeKey]?.length" class="section-landing__entry-list">
              <RouterLink
                v-for="card in groupedCards[activeKey]"
                :key="card.path"
                :to="card.path"
                class="section-landing__entry-item"
              >
                <strong>{{ card.label }}</strong>
                <span>{{ card.description }}</span>
              </RouterLink>
            </div>

            <EmptyState
              v-else
              title="当前分组暂无可进入页面"
              description="可以调整关键词或联系管理员确认菜单授权。"
            />
          </StandardWorkbenchPanel>
        </template>
      </IotAccessTabWorkspace>
    </template>

    <PanelCard
      v-else
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
import { computed, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import EmptyState from '@/components/EmptyState.vue'
import PanelCard from '@/components/PanelCard.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import IotAccessPageShell from '@/components/iotAccess/IotAccessPageShell.vue'
import IotAccessTabWorkspace from '@/components/iotAccess/IotAccessTabWorkspace.vue'
import {
  getSectionHomeConfigByPath,
  sortByPreferredPaths
} from '@/utils/sectionWorkspaces'
import { usePermissionStore } from '@/stores/permission'

const route = useRoute()
const permissionStore = usePermissionStore()

const config = computed(() => getSectionHomeConfigByPath(route.path))
const landingKeyword = ref('')
const landingTabs = [
  { key: 'asset', label: '资产底座' },
  { key: 'diagnostics', label: '诊断排障' }
] as const
const assetPaths = new Set(['/products', '/devices'])

const accessibleCards = computed(() => {
  const cards = config.value?.cards || []
  const allowedCards = cards.filter((card) => permissionStore.hasRoutePermission(card.path))
  return sortByPreferredPaths(allowedCards, permissionStore.roleProfile.featuredPaths)
})

const filteredCards = computed(() => {
  const keyword = landingKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return accessibleCards.value
  }

  return accessibleCards.value.filter((card) =>
    [card.label, card.description, ...(card.keywords || [])]
      .join(' ')
      .toLowerCase()
      .includes(keyword)
  )
})

const groupedCards = computed<Record<string, typeof filteredCards.value>>(() => ({
  asset: filteredCards.value.filter((card) => assetPaths.has(card.path)),
  diagnostics: filteredCards.value.filter((card) => !assetPaths.has(card.path))
}))
</script>

<style scoped>
.section-landing {
  display: grid;
  gap: 0.95rem;
}

.section-landing__entry-list {
  display: grid;
  gap: 0.82rem;
}

.section-landing__entry-item {
  display: grid;
  gap: 0.34rem;
  padding: 0.92rem 0.96rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-2xl);
  background: var(--bg-card);
  color: inherit;
  text-decoration: none;
  box-shadow: var(--shadow-card-soft);
  transition:
    border-color var(--transition-base),
    transform var(--transition-base),
    box-shadow var(--transition-base);
}

.section-landing__entry-item:hover {
  border-color: color-mix(in srgb, var(--brand) 26%, var(--panel-border));
  transform: translateY(-1px);
  box-shadow: var(--shadow-card);
}

.section-landing__entry-item strong {
  color: var(--text-heading);
}

.section-landing__entry-item span {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.6;
}
</style>
