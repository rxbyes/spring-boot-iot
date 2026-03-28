<template>
  <div class="section-landing page-stack">
    <template v-if="accessibleCards.length">
      <section class="section-landing__intro">
        <div class="section-landing__intro-copy">
          <h1 class="section-landing__intro-title">{{ config?.title || '分组概览' }}</h1>
          <p class="section-landing__intro-judgement">{{ sectionHeadline }}</p>
        </div>
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
      </section>

      <div class="section-landing__content-grid">
        <PanelCard eyebrow="Recent Activity" title="最近使用" description="优先回到刚处理过的功能，不重复展示导航路径。">
          <div v-if="recentActivities.length" class="section-landing__recent-list">
            <RouterLink
              v-for="item in recentActivities"
              :key="item.id"
              :to="item.path"
              class="section-landing__recent-item"
            >
              <div class="section-landing__recent-main">
                <strong>{{ item.title }}</strong>
                <p>{{ item.detail }}</p>
              </div>
              <small>{{ item.time }}</small>
            </RouterLink>
          </div>
          <EmptyState
            v-else
            title="暂无最近使用记录"
            description="当前分组还没有本地操作痕迹，建议先从常用入口开始。"
            :action="emptyAction"
          />
        </PanelCard>

        <PanelCard eyebrow="Recommended Flow" title="推荐处理顺序" description="按准备、执行、复核的顺序进入，避免同一层级重复说明。">
          <div class="section-landing__recommend-list">
            <article
              v-for="action in recommendedActions"
              :key="action.path"
              class="section-landing__recommend-item"
            >
              <span class="section-landing__recommend-stage">{{ action.stage }}</span>
              <div class="section-landing__recommend-main">
                <strong>{{ action.title }}</strong>
                <p>{{ action.description }}</p>
              </div>
              <RouterLink :to="action.path" class="section-landing__text-link">
                {{ action.buttonLabel }}
              </RouterLink>
            </article>
          </div>
        </PanelCard>
      </div>

      <PanelCard
        eyebrow="Capability"
        title="全部能力"
        description="其余可进入页面统一收口在这里，避免正文与侧栏重复提示。"
      >
        <div class="section-landing__capability-list">
          <RouterLink
            v-for="card in accessibleCards"
            :key="card.path"
            :to="card.path"
            class="section-landing__capability-item"
          >
            <strong>{{ card.label }}</strong>
            <span>{{ card.description }}</span>
          </RouterLink>
        </div>
      </PanelCard>
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
import { computed } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';

import EmptyState from '../components/EmptyState.vue';
import PanelCard from '../components/PanelCard.vue';
import {
  getSectionHomeConfigByPath,
  matchSectionCardActivity,
  pickSectionActivities,
  sortByPreferredPaths
} from '../utils/sectionWorkspaces';
import { activityEntries } from '../stores/activity';
import { usePermissionStore } from '../stores/permission';
import { formatDateTime } from '../utils/format';

const stageLabels = ['准备', '执行', '复核'];
const actionLabels = ['开始配置', '进入处理', '查看结果'];

const route = useRoute();
const router = useRouter();
const permissionStore = usePermissionStore();

const config = computed(() => getSectionHomeConfigByPath(route.path));
const accessibleCards = computed(() => {
  const cards = config.value?.cards || [];
  const allowedCards = cards.filter((card) => permissionStore.hasRoutePermission(card.path));
  return sortByPreferredPaths(allowedCards, permissionStore.roleProfile.featuredPaths);
});
const primaryCards = computed(() => accessibleCards.value.slice(0, 4));
const secondaryCards = computed(() => accessibleCards.value.slice(4));
const primaryCard = computed(() => primaryCards.value[0] || null);
const secondaryCard = computed(() => primaryCards.value[1] || secondaryCards.value[0] || null);
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
const sectionHeadline = computed(() =>
  config.value?.key === 'iot-access'
    ? '先处理资产底座，再进入链路诊断。'
    : config.value?.hubJudgement || '先判断优先域，再进入单域专台。'
);
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

const recentActivities = computed(() => {
  const matched = pickSectionActivities(accessibleCards.value, activityEntries.value, 4);
  return matched.map((item) => {
    const matchedCard = accessibleCards.value.find((card) => matchSectionCardActivity(card, item));
    return {
      id: item.id,
      title: item.title,
      detail: item.detail,
      time: formatDateTime(item.createdAt),
      path: item.path || matchedCard?.path || primaryCard.value?.path || route.path
    };
  });
});

const recommendedActions = computed(() => {
  const cards = accessibleCards.value.slice(0, 3);
  const steps = config.value?.steps || [];

  return cards.map((card, index) => ({
    path: card.path,
    stage: stageLabels[index] || `步骤 ${index + 1}`,
    title: steps[index] || `进入 ${card.label}`,
    description: card.description,
    buttonLabel: actionLabels[index] || '立即进入'
  }));
});

const emptyAction = computed(() => {
  if (!primaryEntryAction.value) {
    return undefined;
  }
  return {
    label: primaryEntryAction.value.label,
    callback: () => router.push(primaryEntryAction.value?.to || route.path)
  };
});
</script>

<style scoped>
.section-landing {
  display: grid;
  gap: 1rem;
}

.section-landing__intro {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.25rem 0;
}

.section-landing__intro-copy {
  min-width: 0;
}

.section-landing__intro-title {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.25rem;
}

.section-landing__intro-judgement {
  margin: 0.4rem 0 0;
  color: var(--text-caption);
  line-height: 1.6;
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

.section-landing__content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.section-landing__recent-list,
.section-landing__recommend-list,
.section-landing__capability-list {
  display: grid;
  gap: 0.8rem;
}

.section-landing__recent-item,
.section-landing__capability-item {
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

.section-landing__recent-item {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.9rem;
}

.section-landing__recent-main,
.section-landing__recommend-main {
  display: grid;
  gap: 0.4rem;
}

.section-landing__recent-item strong,
.section-landing__recommend-item strong,
.section-landing__capability-item strong {
  color: var(--text-heading);
}

.section-landing__recent-item p,
.section-landing__recommend-item p,
.section-landing__capability-item span {
  margin: 0;
  color: var(--text-caption);
  line-height: 1.6;
  font-size: 0.8rem;
}

.section-landing__recent-item small {
  color: var(--text-caption-2);
  white-space: nowrap;
}

.section-landing__recommend-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 0.8rem;
  align-items: start;
  padding: 0.95rem 1rem;
  border-radius: calc(var(--radius-lg) + 2px);
  border: 1px solid var(--panel-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.95));
  box-shadow: var(--shadow-card-soft);
}

.section-landing__recommend-stage {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 3rem;
  height: 1.8rem;
  padding: 0 0.7rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--brand) 10%, transparent);
  color: var(--brand);
  font-size: 0.76rem;
  font-weight: 700;
}

.section-landing__text-link {
  color: var(--brand);
  text-decoration: none;
  font-size: 0.8rem;
  font-weight: 600;
}

@media (max-width: 900px) {
  .section-landing__intro {
    flex-direction: column;
    align-items: stretch;
  }

  .section-landing__intro-actions {
    justify-content: flex-start;
  }

  .section-landing__content-grid {
    grid-template-columns: 1fr;
  }

  .section-landing__recommend-item,
  .section-landing__recent-item {
    grid-template-columns: 1fr;
  }
}
</style>
