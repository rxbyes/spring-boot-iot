import { defineStore } from 'pinia';

const API_BASE_URL_STORAGE_KEY = 'spring-boot-iot.api-base-url';

function normalizeApiBaseUrl(url?: string | null) {
  return (url || '').trim().replace(/\/+$/, '');
}

function resolveInitialApiBaseUrl() {
  const envBaseUrl = normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL);

  if (typeof window === 'undefined') {
    return envBaseUrl;
  }

  const savedBaseUrl = normalizeApiBaseUrl(window.localStorage.getItem(API_BASE_URL_STORAGE_KEY));
  return savedBaseUrl || envBaseUrl;
}

/**
 * 运行时状态
 */
export const useRuntimeStore = defineStore('runtime', {
  state: () => ({
    // API基础URL
    apiBaseUrl: resolveInitialApiBaseUrl(),
    // 是否显示加载状态
    loading: false,
    // 加载计数
    loadingCount: 0,
    // 主题模式
    theme: 'light' as 'light' | 'dark',
    // 最近访问的页面
    recentPages: [] as string[]
  }),
  getters: {
    /**
     * 获取完整的API基础URL
     */
    fullApiBaseUrl: (state) => normalizeApiBaseUrl(state.apiBaseUrl),
    isProxyMode: (state) => !normalizeApiBaseUrl(state.apiBaseUrl)
  },
  actions: {
    /**
     * 显示加载状态
     */
    showLoading() {
      this.loadingCount++;
      if (this.loadingCount === 1) {
        this.loading = true;
      }
    },
    /**
     * 隐藏加载状态
     */
    hideLoading() {
      this.loadingCount--;
      if (this.loadingCount <= 0) {
        this.loadingCount = 0;
        this.loading = false;
      }
    },
    /**
     * 切换主题
     */
    toggleTheme() {
      this.theme = this.theme === 'light' ? 'dark' : 'light';
    },
    /**
     * 添加最近访问页面
     */
    addRecentPage(path: string) {
      // 移除已存在的页面
      this.recentPages = this.recentPages.filter(p => p !== path);
      // 添加到最前面
      this.recentPages.unshift(path);
      // 限制数量
      if (this.recentPages.length > 10) {
        this.recentPages.length = 10;
      }
    },
    /**
     * 清空最近访问页面
     */
    clearRecentPages() {
      this.recentPages = [];
    },
    /**
     * 设置API基础URL
     */
    updateApiBaseUrl(url: string) {
      const normalizedUrl = normalizeApiBaseUrl(url);
      this.apiBaseUrl = normalizedUrl;

      if (typeof window === 'undefined') {
        return;
      }

      if (normalizedUrl) {
        window.localStorage.setItem(API_BASE_URL_STORAGE_KEY, normalizedUrl);
        return;
      }

      window.localStorage.removeItem(API_BASE_URL_STORAGE_KEY);
    }
  }
});

/**
 * 全局运行时状态（兼容旧代码）
 */
export const runtimeState = {
  get apiBaseUrl() {
    const store = useRuntimeStore();
    return store.fullApiBaseUrl;
  },
  get isProxyMode() {
    const store = useRuntimeStore();
    return store.isProxyMode;
  }
};

/**
 * 是否使用代理模式
 */
export function isProxyMode() {
  const store = useRuntimeStore();
  return store.isProxyMode;
}

/**
 * 设置API基础URL
 */
export function setApiBaseUrl(url: string) {
  const store = useRuntimeStore();
  store.updateApiBaseUrl(url);
}
