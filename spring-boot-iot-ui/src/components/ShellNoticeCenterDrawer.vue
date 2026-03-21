<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    eyebrow="Shell Notice"
    title="通知中心"
    subtitle="按分类、未读状态和分页统一查看当前账号可消费的站内消息。"
    size="54rem"
    :loading="loading"
    :error-message="errorMessage || ''"
    :empty="!loading && !errorMessage && items.length === 0"
    empty-text="当前暂无可查看的站内消息"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <section class="detail-panel detail-panel--hero">
      <div class="detail-section-header">
        <div>
          <h3>筛选条件</h3>
          <p>顶部摘要只保留少量卡片，这里统一承接完整分页、未读筛选和消息详情入口。</p>
        </div>
      </div>
      <div class="shell-drawer-filters">
        <label class="shell-drawer-field">
          <span>消息分类</span>
          <el-select :model-value="activeFilter" placeholder="全部分类" @update:model-value="emit('update:activeFilter', $event)">
            <el-option label="全部分类" value="all" />
            <el-option label="系统事件" value="system" />
            <el-option label="业务事件" value="business" />
            <el-option label="错误事件" value="error" />
          </el-select>
        </label>
        <label class="shell-drawer-field shell-drawer-field--switch">
          <span>仅看未读</span>
          <el-switch :model-value="unreadOnly" @update:model-value="emit('update:unreadOnly', $event)" />
        </label>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>消息列表</h3>
          <p>支持继续查看详情、单条已读和关联页面跳转，未读状态不再随打开面板自动清空。</p>
        </div>
      </div>

      <div class="shell-center-list">
        <article v-for="item in items" :key="item.id" class="shell-center-card" :data-read="item.read ? 'read' : 'unread'">
          <div class="shell-center-card__top">
            <div class="shell-center-card__title-group">
              <div class="shell-center-card__tags">
                <el-tag size="small" :type="messageTypeTagType(item.messageType)">{{ getMessageTypeLabel(item.messageType) }}</el-tag>
                <el-tag size="small" effect="plain" :type="priorityTagType(item.priority)">{{ getPriorityLabel(item.priority) }}</el-tag>
                <el-tag v-if="!item.read" size="small" type="danger" effect="plain">未读</el-tag>
                <el-tag v-else size="small" type="success" effect="plain">已读</el-tag>
              </div>
              <strong>{{ item.title }}</strong>
            </div>
            <span class="shell-center-card__time">{{ item.publishTime || '刚刚更新' }}</span>
          </div>

          <p class="shell-center-card__summary">{{ item.summary || item.content || '站内消息已同步。' }}</p>

          <div class="shell-center-card__meta">
            <span>{{ item.workspaceLabel || '站内消息' }}</span>
            <span>{{ item.relatedPathLabel }}</span>
            <span v-if="item.sourceType">{{ item.sourceType }}</span>
          </div>

          <StandardActionGroup gap="sm" marginTop="sm">
            <el-button type="primary" link @click="emit('select', item)">查看详情</el-button>
            <el-button v-if="item.relatedPath" type="primary" link @click="emit('navigate', item.relatedPath)">进入页面</el-button>
            <el-button v-if="!item.read" type="warning" link @click="emit('read', item)">标记已读</el-button>
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
        <el-button type="warning" plain @click="emit('readAll')">全部已读</el-button>
      </StandardActionGroup>
    </template>
  </StandardDetailDrawer>
</template>

<script setup lang="ts">
import type { InAppMessagePriority, InAppMessageType } from '@/api/inAppMessage'
import StandardActionGroup from '@/components/StandardActionGroup.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import type { ShellNoticeCenterDrawerProps, ShellNoticeCenterEntry, ShellNoticeFilter } from '@/types/shell'

defineProps<ShellNoticeCenterDrawerProps>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'update:activeFilter', value: ShellNoticeFilter): void
  (e: 'update:unreadOnly', value: boolean): void
  (e: 'pageChange', value: number): void
  (e: 'pageSizeChange', value: number): void
  (e: 'select', value: ShellNoticeCenterEntry): void
  (e: 'navigate', value: string): void
  (e: 'read', value: ShellNoticeCenterEntry): void
  (e: 'readAll'): void
  (e: 'refresh'): void
}>()

function getMessageTypeLabel(type?: InAppMessageType | null) {
  switch (type) {
    case 'system':
      return '系统事件'
    case 'business':
      return '业务事件'
    case 'error':
      return '错误事件'
    default:
      return '站内消息'
  }
}

function getPriorityLabel(priority?: InAppMessagePriority | null) {
  switch (priority) {
    case 'critical':
      return '紧急'
    case 'high':
      return '高优先'
    case 'medium':
      return '处理中'
    case 'low':
      return '常规'
    default:
      return '常规'
  }
}

function messageTypeTagType(type?: InAppMessageType | null) {
  switch (type) {
    case 'system':
      return 'primary'
    case 'business':
      return 'success'
    case 'error':
      return 'danger'
    default:
      return 'info'
  }
}

function priorityTagType(priority?: InAppMessagePriority | null) {
  switch (priority) {
    case 'critical':
      return 'danger'
    case 'high':
      return 'warning'
    case 'medium':
      return 'primary'
    case 'low':
      return 'info'
    default:
      return 'info'
  }
}
</script>

<style scoped>
.shell-drawer-filters {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
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

.shell-drawer-field--switch {
  align-content: end;
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

.shell-center-card[data-read='unread'] {
  border-color: color-mix(in srgb, var(--warning) 26%, var(--panel-border));
  box-shadow: inset 3px 0 0 color-mix(in srgb, var(--warning) 65%, white);
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

@media (max-width: 720px) {
  .shell-drawer-filters {
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
