<template>
  <div class="header-status">
    <div class="header-tools" aria-label="系统工具">
      <button
        type="button"
        class="tool-text"
        :class="{ 'tool-text--active': showNoticePanel }"
        aria-label="打开消息通知"
        :aria-expanded="showNoticePanel"
        :aria-controls="noticePanelId"
        @click="$emit('toggle-notice')"
      >
        消息通知
        <span v-if="unreadNoticeCount > 0" class="tool-text__badge">{{ unreadNoticeCount }}</span>
      </button>
      <button
        type="button"
        class="tool-text"
        :class="{ 'tool-text--active': showHelpPanel }"
        aria-label="打开帮助中心"
        :aria-expanded="showHelpPanel"
        :aria-controls="helpPanelId"
        @click="$emit('toggle-help')"
      >
        帮助中心
      </button>
    </div>
    <div class="account-chip" :title="headerIdentity">
      <span class="account-chip__avatar">{{ accountInitial }}</span>
      <span class="account-chip__meta">
        <strong>{{ headerAccountName }}</strong>
        <small>{{ headerRoleName }}</small>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  showNoticePanel: boolean;
  showHelpPanel: boolean;
  noticePanelId: string;
  helpPanelId: string;
  headerIdentity: string;
  headerAccountName: string;
  headerRoleName: string;
  accountInitial: string;
  unreadNoticeCount: number;
}>();

defineEmits<{
  (e: 'toggle-notice'): void;
  (e: 'toggle-help'): void;
}>();
</script>

<style scoped>
.header-status {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.55rem;
}

.header-tools {
  display: inline-flex;
  align-items: center;
  gap: 0.42rem;
}

.tool-text {
  min-height: 1.76rem;
  padding: 0 0.62rem;
  border-radius: var(--radius-xs);
  border: 1px solid var(--panel-border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 0.74rem;
  font-weight: 500;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.tool-text:hover {
  border-color: #c9d7ef;
  color: var(--accent);
  background: var(--bg-hover);
}

.tool-text--active {
  border-color: #a8c4ef;
  color: var(--accent);
  background: var(--bg-active);
}

.tool-text__badge {
  min-width: 1.04rem;
  height: 1.04rem;
  border-radius: 999px;
  padding: 0 0.24rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--danger);
  color: #fff;
  font-size: 0.64rem;
  font-weight: 700;
}

.account-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  min-height: 2rem;
  padding: 0.22rem 0.5rem 0.22rem 0.3rem;
  border-radius: var(--radius-xs);
  border: 1px solid var(--panel-border);
  background: var(--bg-card);
}

.account-chip__avatar {
  width: 1.4rem;
  height: 1.4rem;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.72rem;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(160deg, #4d8bff, #2f66da);
}

.account-chip__meta {
  display: grid;
  gap: 0.05rem;
  line-height: 1.2;
}

.account-chip__meta strong {
  font-size: 0.76rem;
  font-weight: 600;
  color: #1f3558;
}

.account-chip__meta small {
  font-size: 0.68rem;
  color: #5e769e;
}

@media (max-width: 900px) {
  .header-status {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }

  .header-tools {
    margin-left: auto;
  }
}

@media (max-width: 640px) {
  .account-chip {
    min-height: 1.76rem;
    padding-right: 0.28rem;
  }

  .account-chip__meta {
    display: none;
  }

  .tool-text {
    padding: 0 0.5rem;
  }
}
</style>
