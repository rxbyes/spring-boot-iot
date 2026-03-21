<template>
  <div class="standard-applied-filters-bar">
    <span class="standard-applied-filters-bar__label">{{ label }}</span>
    <div class="standard-applied-filters-bar__list">
      <el-tag
        v-for="tag in tags"
        :key="tag.key"
        closable
        class="standard-applied-filters-bar__tag"
        @close="emit('remove', tag.key)"
      >
        {{ tag.label }}
      </el-tag>
    </div>
    <el-button link class="standard-applied-filters-bar__clear" @click="emit('clear')">
      {{ clearText }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
export interface StandardAppliedFilterTag {
  key: string
  label: string
}

withDefaults(
  defineProps<{
    tags: StandardAppliedFilterTag[]
    label?: string
    clearText?: string
  }>(),
  {
    label: '已生效筛选',
    clearText: '清空全部'
  }
)

const emit = defineEmits<{
  (e: 'remove', key: string): void
  (e: 'clear'): void
}>()
</script>

<style scoped>
.standard-applied-filters-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.55rem 0.75rem;
}

.standard-applied-filters-bar__label {
  color: var(--text-caption-2);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
}

.standard-applied-filters-bar__list {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 0.45rem;
  min-width: 0;
}

.standard-applied-filters-bar__tag {
  margin: 0;
}

.standard-applied-filters-bar__clear {
  margin-left: auto;
  padding-inline: 0.08rem;
}

@media (max-width: 900px) {
  .standard-applied-filters-bar {
    align-items: flex-start;
  }

  .standard-applied-filters-bar__clear {
    width: 100%;
    margin-left: 0;
    justify-content: flex-start;
  }
}
</style>
