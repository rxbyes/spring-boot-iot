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
  border-radius: 2px;
  border: 1px solid #d8dfeb;
  background: #fff;
  color: #4b5565;
  font-size: 0.74rem;
  font-weight: 500;
  line-height: 1;
}

.tool-text:hover {
  border-color: #c9d7ef;
  color: #1677ff;
  background: #f7fbff;
}

.tool-text--active {
  border-color: #a8c4ef;
  color: #1677ff;
  background: #eef5ff;
}

.account-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  min-height: 2rem;
  padding: 0.22rem 0.5rem 0.22rem 0.3rem;
  border-radius: 2px;
  border: 1px solid #e6eaf0;
  background: #fff;
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
</style>
