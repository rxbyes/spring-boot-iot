<template>
  <div class="section-landing">
    <PanelCard eyebrow="Section Home" :title="config?.title || '分组概览'" :description="config?.description || '当前分组暂无可展示信息。'">
      <div class="section-landing__hero">
        <div class="section-landing__hero-main">
          <p class="section-landing__hero-intro">{{ config?.intro || '当前分组用于承接该一级导航下的共性能力。' }}</p>

          <div v-if="heroTags.length" class="section-landing__hero-tags">
            <span v-for="tag in heroTags" :key="tag.label" class="section-landing__hero-tag">
              <small>{{ tag.label }}</small>
              <strong>{{ tag.value }}</strong>
            </span>
          </div>

          <div v-if="primaryCard" class="section-landing__hero-actions">
            <RouterLink :to="primaryCard.path" class="section-landing__hero-link section-landing__hero-link--primary">
              进入 {{ primaryCard.label }}
            </RouterLink>
            <RouterLink v-if="secondaryCard" :to="secondaryCard.path" class="section-landing__hero-link">
              查看 {{ secondaryCard.label }}
            </RouterLink>
          </div>
        </div>

        <div class="section-landing__stats">
          <div v-for="item in heroStats" :key="item.label" class="section-landing__stat">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.tip }}</small>
          </div>
        </div>
      </div>
    </PanelCard>

    <template v-if="accessibleCards.length">
      <PanelCard eyebrow="Common Entry" title="常用入口" description="首屏仅保留高频入口，减少重复的定位说明。">
        <div class="section-landing__grid">
          <RouterLink
            v-for="card in primaryCards"
            :key="card.path"
            :to="card.path"
            class="section-landing__card"
          >
            <span class="section-landing__badge">{{ buildBadge(card.label) }}</span>
            <div class="section-landing__card-main">
              <strong>{{ card.label }}</strong>
              <p>{{ card.description }}</p>
            </div>
            <small>进入页面</small>
          </RouterLink>
        </div>
      </PanelCard>

      <div class="section-landing__columns">
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

        <PanelCard eyebrow="Recommended" title="推荐操作" description="按准备、执行、复核的顺序进入，避免同一层级重复说明。">
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
        v-if="secondaryCards.length"
        eyebrow="Capability"
        title="全部能力"
        description="其余可进入页面统一收口在这里，避免正文与侧栏重复提示。"
      >
        <div class="section-landing__capability-list">
          <RouterLink
            v-for="card in secondaryCards"
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
import { getSectionHomeConfigByPath, matchSectionCardActivity, pickSectionActivities } from '../config/sectionHomes';
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
  return cards.filter((card) => permissionStore.hasRoutePermission(card.path));
});
const primaryCards = computed(() => accessibleCards.value.slice(0, 4));
const secondaryCards = computed(() => accessibleCards.value.slice(4));
const primaryCard = computed(() => primaryCards.value[0] || null);
const secondaryCard = computed(() => primaryCards.value[1] || secondaryCards.value[0] || null);

const heroTags = computed(() => {
  const userInfo = permissionStore.userInfo;
  return [
    { label: '角色', value: permissionStore.primaryRoleName || '未分配' },
    { label: '账号', value: userInfo?.accountType || '未标记' },
    { label: '实名', value: userInfo?.authStatus || '待完善' }
  ].filter((item) => Boolean(item.value));
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

const heroStats = computed(() => {
  return [
    {
      label: '可用入口',
      value: accessibleCards.value.length,
      tip: '当前账号可直接进入'
    },
    {
      label: '常用入口',
      value: primaryCards.value.length,
      tip: '首屏仅保留高频能力'
    },
    {
      label: '最近使用',
      value: recentActivities.value.length,
      tip: recentActivities.value.length > 0 ? '按本地操作痕迹推荐' : '尚未产生使用痕迹'
    }
  ];
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
  if (!primaryCard.value) {
    return undefined;
  }
  return {
    label: `进入${primaryCard.value.label}`,
    callback: () => router.push(primaryCard.value?.path || route.path)
  };
});

function buildBadge(label: string) {
  const value = label.trim();
  return value ? value.slice(0, 2) : '概览';
}
</script>

<style scoped>
.section-landing {
  display: grid;
  gap: 1rem;
}

.section-landing__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(16rem, 1fr);
  gap: 1.1rem;
  align-items: stretch;
}

.section-landing__hero-main {
  display: grid;
  gap: 1rem;
}

.section-landing__hero-intro {
  margin: 0;
  color: #5d6f89;
  line-height: 1.75;
}

.section-landing__hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.section-landing__hero-tag {
  display: grid;
  gap: 0.2rem;
  min-width: 6.8rem;
  padding: 0.8rem 0.95rem;
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(248, 250, 255, 0.98), rgba(241, 246, 255, 0.96));
  border: 1px solid #e4ecf8;
  box-shadow: 0 3px 10px rgba(31, 35, 41, 0.04);
}

.section-landing__hero-tag small {
  color: #7f8fa8;
  font-size: 0.72rem;
}

.section-landing__hero-tag strong {
  color: #1f3558;
  font-size: 0.92rem;
}

.section-landing__hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.section-landing__hero-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.5rem;
  padding: 0 0.92rem;
  border-radius: 999px;
  border: 1px solid #d7e3f7;
  color: #35527a;
  background: linear-gradient(180deg, #ffffff, #f7fbff);
  text-decoration: none;
  font-weight: 600;
  transition: all 160ms ease;
}

.section-landing__hero-link:hover {
  border-color: #bfd2f2;
  background: #edf4ff;
}

.section-landing__hero-link--primary {
  background: linear-gradient(135deg, var(--accent), var(--accent-bright));
  color: #fff;
  border-color: transparent;
  box-shadow: 0 6px 16px color-mix(in srgb, var(--accent) 16%, transparent);
}

.section-landing__stats {
  display: grid;
  gap: 0.85rem;
}

.section-landing__stat {
  display: grid;
  gap: 0.3rem;
  padding: 1rem;
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(248, 250, 255, 0.98), rgba(241, 246, 255, 0.96));
  border: 1px solid #e3ebf8;
  box-shadow: 0 3px 10px rgba(31, 35, 41, 0.04);
}

.section-landing__stat span {
  color: #7b8ba3;
  font-size: 0.76rem;
}

.section-landing__stat strong {
  color: #1f3558;
  font-size: 1.3rem;
}

.section-landing__stat small {
  color: #5d6f89;
  line-height: 1.5;
  font-size: 0.78rem;
}

.section-landing__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 0.9rem;
}

.section-landing__card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: start;
  gap: 0.8rem;
  padding: 1.05rem 1.1rem;
  border-radius: calc(var(--radius-lg) + 2px);
  border: 1px solid #e3eaf5;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.95));
  color: inherit;
  text-decoration: none;
  box-shadow: 0 4px 12px rgba(31, 35, 41, 0.05);
}

.section-landing__card:hover {
  border-color: #c8d7ef;
  box-shadow: 0 8px 18px rgba(31, 35, 41, 0.08);
  transform: translateY(-1px);
}

.section-landing__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.4rem;
  height: 2.4rem;
  border-radius: 0.8rem;
  background: color-mix(in srgb, var(--accent) 8%, transparent);
  color: var(--accent);
  font-size: 0.82rem;
  font-weight: 700;
}

.section-landing__card-main {
  display: grid;
  gap: 0.45rem;
}

.section-landing__card strong {
  color: #1f3558;
  font-size: 0.94rem;
}

.section-landing__card p {
  margin: 0;
  color: #5d6f89;
  line-height: 1.6;
  font-size: 0.8rem;
}

.section-landing__card small {
  color: var(--accent);
  font-size: 0.76rem;
  font-weight: 600;
}

.section-landing__columns {
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
  border: 1px solid #e7edf7;
  text-decoration: none;
  color: inherit;
  background: #fff;
  box-shadow: 0 3px 10px rgba(31, 35, 41, 0.04);
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
  color: #1f3558;
}

.section-landing__recent-item p,
.section-landing__recommend-item p,
.section-landing__capability-item span {
  margin: 0;
  color: #5d6f89;
  line-height: 1.6;
  font-size: 0.8rem;
}

.section-landing__recent-item small {
  color: #7f8fa8;
  white-space: nowrap;
}

.section-landing__recommend-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 0.8rem;
  align-items: start;
  padding: 0.95rem 1rem;
  border-radius: calc(var(--radius-lg) + 2px);
  border: 1px solid #e7edf7;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.95));
  box-shadow: 0 3px 10px rgba(31, 35, 41, 0.04);
}

.section-landing__recommend-stage {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 3rem;
  height: 1.8rem;
  padding: 0 0.7rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent) 8%, transparent);
  color: var(--accent);
  font-size: 0.76rem;
  font-weight: 700;
}

.section-landing__text-link {
  color: var(--accent);
  text-decoration: none;
  font-size: 0.8rem;
  font-weight: 600;
}

@media (max-width: 900px) {
  .section-landing__hero,
  .section-landing__columns {
    grid-template-columns: 1fr;
  }

  .section-landing__grid {
    grid-template-columns: 1fr;
  }

  .section-landing__card,
  .section-landing__recommend-item,
  .section-landing__recent-item {
    grid-template-columns: 1fr;
  }
}
</style>
