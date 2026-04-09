<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    :title="record?.title || '帮助详情'"
    :subtitle="record?.summary || '统一查看帮助分类、关键词与全文内容。'"
    size="52rem"
    :loading="loading"
    :error-message="errorMessage || ''"
    :empty="!loading && !errorMessage && !record"
    empty-text="当前没有可展示的帮助详情"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <section class="detail-panel detail-panel--hero">
      <div class="detail-section-header">
        <div>
          <h3>文档概览</h3>
          <p>帮助详情继续保留当前页相关标识，并对关键字命中内容做安全高亮。</p>
        </div>
      </div>
      <div class="detail-summary-grid">
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">文档分类</span>
          <strong class="detail-summary-card__value">{{ getCategoryLabel(record?.docCategory) }}</strong>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">当前页相关</span>
          <strong class="detail-summary-card__value">{{ record?.currentPathMatched ? '是' : '否' }}</strong>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">关联页面</span>
          <strong class="detail-summary-card__value">{{ record?.relatedPathLabel || '未绑定页面' }}</strong>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">排序</span>
          <strong class="detail-summary-card__value">{{ record?.sortNo ?? 0 }}</strong>
        </article>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>关键词与范围</h3>
          <p>关键字命中覆盖标题、摘要、关键词和正文，不引入富文本注入风险。</p>
        </div>
      </div>
      <div class="detail-grid">
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">关键词</span>
          <div class="shell-help-detail__keywords">
            <el-tag v-for="keywordItem in record?.keywordList || []" :key="keywordItem" size="small" effect="plain">
              <ShellHighlightText :text="keywordItem" :keyword="highlightKeyword" />
            </el-tag>
            <span v-if="!(record?.keywordList || []).length" class="shell-help-detail__empty">未配置关键词</span>
          </div>
        </div>
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">关联页面</span>
          <strong class="detail-field__value detail-field__value--plain">{{ record?.relatedPathLabel || '-' }}</strong>
        </div>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>文档正文</h3>
          <p>正文保留多行排版，便于值班与治理侧直接阅读完整说明。</p>
        </div>
      </div>
      <div class="detail-grid">
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">摘要</span>
          <div class="detail-field__value detail-field__value--plain">
            <ShellHighlightText tag="div" :text="record?.summary || '-'" :keyword="highlightKeyword" />
          </div>
        </div>
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">正文</span>
          <div class="detail-field__value detail-field__value--pre">
            <ShellHighlightText tag="div" :text="record?.content || record?.summary || '-'" :keyword="highlightKeyword" />
          </div>
        </div>
      </div>
    </section>

    <template #footer>
      <StandardActionGroup>
        <StandardButton v-if="record?.primaryPath" action="confirm" @click="emit('navigate', record.primaryPath)">进入页面</StandardButton>
      </StandardActionGroup>
    </template>
  </StandardDetailDrawer>
</template>

<script setup lang="ts">
import type { HelpDocCategory } from '@/api/helpDoc'
import ShellHighlightText from '@/components/ShellHighlightText.vue'
import StandardActionGroup from '@/components/StandardActionGroup.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import type { ShellHelpDetailDrawerProps } from '@/types/shell'

withDefaults(defineProps<ShellHelpDetailDrawerProps>(), {
  highlightKeyword: ''
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'navigate', value: string): void
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
</script>

<style scoped>
.shell-help-detail__keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.shell-help-detail__empty {
  color: var(--text-tertiary);
}
</style>
