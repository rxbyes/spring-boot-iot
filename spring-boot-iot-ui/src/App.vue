<template>
  <el-config-provider :locale="zhCn">
    <RouterView v-if="route.meta.layout === 'blank'" />
    <AppShell v-else />
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue';
import { RouterView, useRoute } from 'vue-router';
import zhCn from 'element-plus/es/locale/lang/zh-cn';

import { useThemeStore } from './stores/theme';
import AppShell from './components/AppShell.vue';

const themeStore = useThemeStore();
const route = useRoute();

// 监听主题变化
watch(() => themeStore.mode, () => {
  themeStore.applyTheme();
});

// 监听颜色变化
watch(() => ({
  primary: themeStore.primaryColor,
  secondary: themeStore.secondaryColor,
  background: themeStore.backgroundColor,
  text: themeStore.textColor
}), () => {
  themeStore.applyTheme();
}, { deep: true });

// 初始化主题
onMounted(() => {
  themeStore.applyTheme();
  themeStore.loadSystemPreference();
  themeStore.applyTheme();
});
</script>
