import { defineStore } from 'pinia';

const DEFAULT_LIGHT_THEME = {
  primaryColor: '#ff6a00',
  secondaryColor: '#ff8833',
  backgroundColor: '#f5f7fa',
  textColor: '#1f2329'
} as const;

export interface ThemeState {
  mode: 'light' | 'dark';
  primaryColor: string;
  secondaryColor: string;
  backgroundColor: string;
  textColor: string;
}

export const useThemeStore = defineStore('theme', {
  state: (): ThemeState => ({
    mode: 'light',
    primaryColor: DEFAULT_LIGHT_THEME.primaryColor,
    secondaryColor: DEFAULT_LIGHT_THEME.secondaryColor,
    backgroundColor: DEFAULT_LIGHT_THEME.backgroundColor,
    textColor: DEFAULT_LIGHT_THEME.textColor
  }),
  getters: {
    themeMode: (state) => state.mode,
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
    toggleMode() {
      this.mode = this.mode === 'light' ? 'dark' : 'light';
    },
    setMode(mode: 'light' | 'dark') {
      this.mode = mode;
    },
    setColors(colors: Partial<ThemeState>) {
      Object.assign(this, colors);
    },
    applyTheme() {
      const root = document.documentElement;
      const { mode, primaryColor, secondaryColor, backgroundColor, textColor } = this;

      // Use token variables as the single source of truth.
      root.style.setProperty('--primary', primaryColor);
      root.style.setProperty('--primary-bright', secondaryColor);
      root.style.setProperty('--primary-deep', 'color-mix(in srgb, var(--primary) 88%, black)');
      root.style.setProperty('--primary-light', 'color-mix(in srgb, var(--primary) 10%, transparent)');
      root.style.setProperty('--brand', 'var(--primary)');
      root.style.setProperty('--brand-bright', 'var(--primary-bright)');
      root.style.setProperty('--brand-deep', 'var(--primary-deep)');
      root.style.setProperty('--brand-light', 'var(--primary-light)');
      root.style.setProperty('--bg', backgroundColor);
      root.style.setProperty('--text-primary', textColor);

      // Keep Element Plus colors aligned to the same tokens.
      root.style.setProperty('--el-color-primary', 'var(--brand)');
      root.style.setProperty('--el-color-primary-light-3', 'color-mix(in srgb, var(--brand) 72%, white)');
      root.style.setProperty('--el-color-primary-light-5', 'color-mix(in srgb, var(--brand) 56%, white)');
      root.style.setProperty('--el-color-primary-light-7', 'color-mix(in srgb, var(--brand) 38%, white)');
      root.style.setProperty('--el-color-primary-dark-2', 'color-mix(in srgb, var(--brand) 86%, black)');

      if (mode === 'dark') {
        root.style.setProperty('--el-bg-color', '#111827');
        root.style.setProperty('--el-bg-color-page', '#0f172a');
        root.style.setProperty('--el-text-color-primary', '#f8fafc');
        root.style.setProperty('--el-text-color-regular', '#cbd5e1');
        root.style.setProperty('--el-border-color', '#334155');
        root.style.setProperty('--el-fill-color', '#1e293b');
      } else {
        root.style.setProperty('--el-bg-color', 'var(--bg-card)');
        root.style.setProperty('--el-bg-color-page', 'var(--bg)');
        root.style.setProperty('--el-text-color-primary', 'var(--text-primary)');
        root.style.setProperty('--el-text-color-regular', 'var(--text-secondary)');
        root.style.setProperty('--el-border-color', 'var(--panel-border)');
        root.style.setProperty('--el-fill-color', 'var(--bg-hover)');
      }
    },
    loadSystemPreference() {
      // Keep a stable light baseline for unified UI acceptance.
      this.mode = 'light';
    }
  }
});
