<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    eyebrow="Shell Notice Detail"
    :title="record?.title || '消息详情'"
    :subtitle="record?.summary || '统一查看消息分类、优先级、来源与正文。'"
    size="48rem"
    :loading="loading"
    :error-message="errorMessage || ''"
    :empty="!loading && !errorMessage && !record"
    empty-text="当前没有可展示的消息详情"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <section class="detail-panel detail-panel--hero">
      <div class="detail-section-header">
        <div>
          <h3>消息概览</h3>
          <p>消息详情不会自动标记已读，已读状态只在显式操作后更新。</p>
        </div>
      </div>
      <div class="detail-summary-grid">
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">消息分类</span>
          <strong class="detail-summary-card__value">{{ getMessageTypeLabel(record?.messageType) }}</strong>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">优先级</span>
          <strong class="detail-summary-card__value">{{ getPriorityLabel(record?.priority) }}</strong>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">阅读状态</span>
          <strong class="detail-summary-card__value">{{ record?.read ? '已读' : '未读' }}</strong>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">关联页面</span>
          <strong class="detail-summary-card__value">{{ record?.relatedPathLabel || '仅站内展示' }}</strong>
        </article>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>来源与投放</h3>
          <p>统一查看发布时间、来源类型和来源标识，便于追溯消息生产侧。</p>
        </div>
      </div>
      <div class="detail-grid">
        <div class="detail-field">
          <span class="detail-field__label">发布时间</span>
          <strong class="detail-field__value">{{ record?.publishTime || '-' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">已读时间</span>
          <strong class="detail-field__value">{{ record?.readTime || '-' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">来源类型</span>
          <strong class="detail-field__value">{{ record?.sourceType || '-' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">来源标识</span>
          <strong class="detail-field__value detail-field__value--plain">{{ record?.sourceId || '-' }}</strong>
        </div>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>消息正文</h3>
          <p>摘要用于顶部卡片，正文用于完整消费闭环与后续扩展。</p>
        </div>
      </div>
      <div class="detail-grid">
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">摘要</span>
          <strong class="detail-field__value detail-field__value--plain">{{ record?.summary || '-' }}</strong>
        </div>
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">正文</span>
          <div class="detail-field__value detail-field__value--pre">{{ record?.content || record?.summary || '-' }}</div>
        </div>
      </div>
    </section>

    <template #footer>
      <StandardActionGroup>
        <el-button v-if="record?.relatedPath" type="primary" @click="emit('navigate', record.relatedPath)">进入页面</el-button>
        <el-button v-if="record && !record.read" type="warning" plain @click="emit('markRead')">标记已读</el-button>
      </StandardActionGroup>
    </template>
  </StandardDetailDrawer>
</template>

<script setup lang="ts">
import type { InAppMessagePriority, InAppMessageType } from '@/api/inAppMessage'
import StandardActionGroup from '@/components/StandardActionGroup.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import type { ShellNoticeDetailDrawerProps } from '@/types/shell'

defineProps<ShellNoticeDetailDrawerProps>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'navigate', value: string): void
  (e: 'markRead'): void
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
</script>
