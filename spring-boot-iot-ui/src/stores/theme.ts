import { defineStore } from 'pinia';

/**
 * 主题状态
 */
export interface ThemeState {
  mode: 'light' | 'dark';
  primaryColor: string;
  secondaryColor: string;
  backgroundColor: string;
  textColor: string;
}

/**
 * 主题状态管理
 */
export const useThemeStore = defineStore('theme', {
  state: (): ThemeState => ({
    mode: 'light',
    primaryColor: '#409EFF',
    secondaryColor: '#66B1FF',
    backgroundColor: '#F5F7FA',
    textColor: '#303133'
  }),
  getters: {
    /**
     * 获取主题模式
     */
    themeMode: (state) => state.mode,
    /**
     * 获取主题配置
     */
    themeConfig: (state) => ({
      mode: state.mode,
      colors: {
        primary: state.primaryColor,
        secondary: state.secondaryColor,
        background: state.backgroundColor,
        text: state.textColor
      }
    })
  },
  actions: {
    /**
     * 切换主题模式
     */
    toggleMode() {
      this.mode = this.mode === 'light' ? 'dark' : 'light';
    },
    /**
     * 设置主题模式
     */
    setMode(mode: 'light' | 'dark') {
      this.mode = mode;
    },
    /**
     * 设置主题颜色
     */
    setColors(colors: Partial<ThemeState>) {
      Object.assign(this, colors);
    },
    /**
     * 应用主题到文档
     */
    applyTheme() {
      const root = document.documentElement;
      const { mode, primaryColor, secondaryColor, backgroundColor, textColor } = this;

      if (mode === 'dark') {
        root.style.setProperty('--el-bg-color', '#0a0a0a');
        root.style.setProperty('--el-bg-color-page', '#1a1a1a');
        root.style.setProperty('--el-text-color-primary', '#ffffff');
        root.style.setProperty('--el-text-color-regular', '#cccccc');
        root.style.setProperty('--el-border-color', '#333333');
        root.style.setProperty('--el-fill-color', '#2a2a2a');
      } else {
        root.style.setProperty('--el-bg-color', backgroundColor);
        root.style.setProperty('--el-text-color-primary', textColor);
        root.style.setProperty('--el-text-color-regular', '#666666');
        root.style.setProperty('--el-border-color', '#dcdfe6');
        root.style.setProperty('--el-fill-color', '#f5f7fa');
      }

      root.style.setProperty('--el-color-primary', primaryColor);
      root.style.setProperty('--el-color-primary-light-3', secondaryColor);
    },
    /**
     * 从系统偏好设置加载主题
     */
    loadSystemPreference() {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      this.mode = prefersDark ? 'dark' : 'light';
    }
  }
});