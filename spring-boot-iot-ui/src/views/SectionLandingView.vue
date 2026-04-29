<template>
  <StandardPageShell
    class="section-landing page-stack"
    :show-breadcrumbs="accessibleCards.length > 0"
    :breadcrumbs="accessibleCards.length ? [{ label: config?.title || '接入智维' }] : []"
    :show-title="false"
  >
    <template v-if="accessibleCards.length">
      <IotAccessTabWorkspace v-if="usesSegmentedLanding" :items="landingTabs" default-key="asset" :sync-query="false">
        <template #default="{ activeKey }">
          <StandardWorkbenchPanel
            :title="config?.title || '接入智维'"
            :description="config?.description || '接入智维总览负责回答先去哪、再去哪、最后去哪修。'"
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

            <section v-if="config?.hubJudgement" class="section-landing__decision-tree">
              <span v-if="config?.hubLeadTitle" class="section-landing__decision-eyebrow">
                {{ config.hubLeadTitle }}
              </span>
              <strong class="section-landing__decision-title">{{ config.hubJudgement }}</strong>
              <p v-if="config?.hubLeadDescription" class="section-landing__decision-description">
                {{ config.hubLeadDescription }}
              </p>
              <ol v-if="config?.steps?.length" class="section-landing__decision-steps">
                <li v-for="step in config.steps" :key="step">
                  {{ step }}
                </li>
              </ol>
            </section>

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

      <StandardWorkbenchPanel
        v-else
        :title="config?.title || '接入智维'"
        :description="config?.description || '接入智维总览负责回答先去哪、再去哪、最后去哪修。'"
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

        <section v-if="config?.hubJudgement" class="section-landing__decision-tree">
          <span v-if="config?.hubLeadTitle" class="section-landing__decision-eyebrow">
            {{ config.hubLeadTitle }}
          </span>
          <strong class="section-landing__decision-title">{{ config.hubJudgement }}</strong>
          <p v-if="config?.hubLeadDescription" class="section-landing__decision-description">
            {{ config.hubLeadDescription }}
          </p>
          <ol v-if="config?.steps?.length" class="section-landing__decision-steps">
            <li v-for="step in config.steps" :key="step">
              {{ step }}
            </li>
          </ol>
        </section>

        <div v-if="filteredCards.length" class="section-landing__entry-list">
          <RouterLink
            v-for="card in filteredCards"
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
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import EmptyState from '@/components/EmptyState.vue'
import PanelCard from '@/components/PanelCard.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardPageShell from '@/components/StandardPageShell.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
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
const assetPaths = new Set(['/device-onboarding', '/products', '/devices'])
const usesSegmentedLanding = computed(() => config.value?.key === 'iot-access')

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

.section-landing__decision-tree {
  display: grid;
  gap: 0.45rem;
  margin-bottom: 0.96rem;
  padding: 0.96rem 1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 16%, var(--panel-border));
  border-radius: var(--radius-2xl);
  background: color-mix(in srgb, var(--bg-card) 90%, var(--brand-soft));
  box-shadow: var(--shadow-card-soft);
}

.section-landing__decision-eyebrow {
  color: var(--brand);
  font-size: var(--type-overline-size);
  letter-spacing: var(--font-letter-spacing-wide);
  text-transform: uppercase;
}

.section-landing__decision-title {
  color: var(--text-heading);
  font-size: var(--type-title-sm);
  line-height: 1.5;
}

.section-landing__decision-description {
  margin: 0;
  color: var(--text-secondary);
  font-size: var(--type-body-size);
  line-height: 1.7;
}

.section-landing__decision-steps {
  display: grid;
  gap: 0.4rem;
  margin: 0;
  padding-left: 1.1rem;
  color: var(--text-secondary);
  font-size: var(--type-body-size);
  line-height: 1.7;
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
