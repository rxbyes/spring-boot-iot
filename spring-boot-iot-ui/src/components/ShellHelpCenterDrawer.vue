<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    eyebrow="Shell Help"
    title="帮助中心"
    subtitle="按分类、关键字和当前页面上下文统一查看权限内帮助资料。"
    size="56rem"
    :loading="loading"
    :error-message="errorMessage || ''"
    :empty="!loading && !errorMessage && items.length === 0"
    empty-text="当前暂无可查看的帮助资料"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <section class="detail-panel detail-panel--hero">
      <div class="detail-section-header">
        <div>
          <h3>检索条件</h3>
          <p>完整帮助列表支持服务端分页检索，并保持当前页面相关资料优先排序。</p>
        </div>
      </div>
      <div class="shell-drawer-filters">
        <label class="shell-drawer-field">
          <span>资料分类</span>
          <el-select :model-value="activeFilter" placeholder="全部分类" @update:model-value="emit('update:activeFilter', $event)">
            <el-option label="全部分类" value="all" />
            <el-option label="业务类" value="business" />
            <el-option label="技术类" value="technical" />
            <el-option label="FAQ" value="faq" />
          </el-select>
        </label>
        <label class="shell-drawer-field shell-drawer-field--grow">
          <span>关键字</span>
          <div class="shell-search-row">
            <el-input
              :model-value="keyword"
              clearable
              placeholder="按标题、摘要、正文或关键词检索"
              @update:model-value="emit('update:keyword', $event || '')"
              @keyup.enter="emit('search')"
            />
            <el-button type="primary" @click="emit('search')">搜索</el-button>
          </div>
        </label>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>帮助列表</h3>
          <p>支持继续查看详情、进入关联页面，并对命中关键字与当前页相关资料做醒目标识。</p>
        </div>
      </div>

      <div class="shell-center-list">
        <article v-for="item in items" :key="item.id" class="shell-center-card" :data-matched="item.currentPathMatched ? 'matched' : 'normal'">
          <div class="shell-center-card__top">
            <div class="shell-center-card__title-group">
              <div class="shell-center-card__tags">
                <el-tag size="small" :type="categoryTagType(item.docCategory)">{{ getCategoryLabel(item.docCategory) }}</el-tag>
                <el-tag v-if="item.currentPathMatched" size="small" type="success" effect="plain">当前页相关</el-tag>
              </div>
              <strong><ShellHighlightText :text="item.title" :keyword="keyword" /></strong>
            </div>
            <span class="shell-center-card__time">排序 {{ item.sortNo ?? 0 }}</span>
          </div>

          <p class="shell-center-card__summary">
            <ShellHighlightText :text="item.summary || item.content || '权限内帮助资料已同步。'" :keyword="keyword" />
          </p>

          <div class="shell-center-card__meta">
            <span>{{ item.workspaceLabel || '帮助资料' }}</span>
            <span>{{ item.relatedPathLabel }}</span>
          </div>

          <div v-if="item.keywordList?.length" class="shell-keywords">
            <el-tag v-for="keywordItem in item.keywordList.slice(0, 4)" :key="keywordItem" size="small" effect="plain">
              <ShellHighlightText :text="keywordItem" :keyword="keyword" />
            </el-tag>
          </div>

          <StandardActionGroup gap="sm" marginTop="sm">
            <el-button type="primary" link @click="emit('select', item)">查看详情</el-button>
            <el-button v-if="item.primaryPath" type="primary" link @click="emit('navigate', item.primaryPath)">进入页面</el-button>
          </StandardActionGroup>
        </article>
      </div>

      <StandardPagination
        :current-page="pagination.pageNum"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        class="pagination"
        @current-change="emit('pageChange', $event)"
        @size-change="emit('pageSizeChange', $event)"
      />
    </section>

    <template #footer>
      <StandardActionGroup>
        <el-button @click="emit('refresh')">刷新</el-button>
      </StandardActionGroup>
    </template>
  </StandardDetailDrawer>
</template>

<script setup lang="ts">
import type { HelpDocCategory } from '@/api/helpDoc'
import ShellHighlightText from '@/components/ShellHighlightText.vue'
import StandardActionGroup from '@/components/StandardActionGroup.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import type { ShellHelpCenterDrawerProps, ShellHelpCenterEntry, ShellHelpFilter } from '@/types/shell'

defineProps<ShellHelpCenterDrawerProps>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'update:activeFilter', value: ShellHelpFilter): void
  (e: 'update:keyword', value: string): void
  (e: 'search'): void
  (e: 'pageChange', value: number): void
  (e: 'pageSizeChange', value: number): void
  (e: 'select', value: ShellHelpCenterEntry): void
  (e: 'navigate', value: string): void
  (e: 'refresh'): void
}>()

function getCategoryLabel(category?: HelpDocCategory | null) {
  switch (category) {
    case 'business':
      return '业务类'
    case 'technical':
      return '技术类'
    case 'faq':
      return 'FAQ'
    default:
      return '帮助资料'
  }
}

function categoryTagType(category?: HelpDocCategory | null) {
  switch (category) {
    case 'business':
      return 'primary'
    case 'technical':
      return 'warning'
    case 'faq':
      return 'success'
    default:
      return 'info'
  }
}
</script>

<style scoped>
.shell-drawer-filters {
  display: grid;
  grid-template-columns: 12rem minmax(0, 1fr);
  gap: 0.9rem;
}

.shell-drawer-field {
  display: grid;
  gap: 0.45rem;
}

.shell-drawer-field span {
  color: var(--text-caption-2);
  font-size: 0.76rem;
  font-weight: 600;
}

.shell-drawer-field--grow {
  min-width: 0;
}

.shell-search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.6rem;
}

.shell-center-list {
  display: grid;
  gap: 0.9rem;
}

.shell-center-card {
  padding: 0.95rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 250, 255, 0.92));
}

.shell-center-card[data-matched='matched'] {
  border-color: color-mix(in srgb, var(--success) 26%, var(--panel-border));
  box-shadow: inset 3px 0 0 color-mix(in srgb, var(--success) 60%, white);
}

.shell-center-card__top {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
}

.shell-center-card__title-group {
  min-width: 0;
  display: grid;
  gap: 0.45rem;
}

.shell-center-card__title-group strong {
  color: var(--text-heading);
  font-size: 0.96rem;
  line-height: 1.5;
}

.shell-center-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.38rem;
}

.shell-center-card__time {
  flex: none;
  color: var(--text-tertiary);
  font-size: 0.74rem;
  white-space: nowrap;
}

.shell-center-card__summary {
  margin: 0.72rem 0 0;
  color: var(--text-secondary);
  font-size: 0.82rem;
  line-height: 1.65;
}

.shell-center-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 0.8rem;
  margin-top: 0.7rem;
  color: var(--text-tertiary);
  font-size: 0.74rem;
}

.shell-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 0.38rem;
  margin-top: 0.7rem;
}

@media (max-width: 720px) {
  .shell-drawer-filters,
  .shell-search-row {
    grid-template-columns: 1fr;
  }

  .shell-center-card__top {
    flex-direction: column;
  }

  .shell-center-card__time {
    white-space: normal;
  }
}
</style>
