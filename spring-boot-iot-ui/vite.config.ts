import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  // 默认使用 127.0.0.1，避免部分环境 localhost 解析到 IPv6 导致代理 500。
  const proxyTarget = (env.VITE_PROXY_TARGET || 'http://127.0.0.1:9999').trim();

  return {
  plugins: [
    vue(),
    Components({
      dts: false,
      resolvers: [ElementPlusResolver({ importStyle: 'css' })]
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  build: {
    chunkSizeWarningLimit: 700,
    rollupOptions: {
      output: {
        manualChunks(id: string) {
          if (!id.includes('node_modules')) {
            return;
          }

          // Split ECharts into stable groups to reduce single large chunk size.
          if (id.includes('zrender')) {
            return 'vendor-zrender';
          }

          if (id.includes('echarts/core')) {
            return 'vendor-echarts-core';
          }

          if (id.includes('echarts')) {
            return 'vendor-echarts-core';
          }

          if (id.includes('@element-plus/icons-vue')) {
            return 'vendor-element-icons';
          }

          // Collapse stable Element Plus shared dependencies and avoid scattered el-* micro chunks.
          if (
            id.includes('element-plus/es/hooks') ||
            id.includes('element-plus/es/utils') ||
            id.includes('element-plus/es/constants') ||
            id.includes('element-plus/es/tokens') ||
            id.includes('element-plus/es/directives') ||
            id.includes('element-plus/es/locale') ||
            id.includes('element-plus/es/components/config-provider') ||
            id.includes('@floating-ui') ||
            id.includes('@popperjs/core') ||
            id.includes('normalize-wheel-es') ||
            id.includes('async-validator') ||
            id.includes('lodash-unified') ||
            id.includes('dayjs')
          ) {
            return 'vendor-element-core';
          }

          if (
            id.includes('element-plus/es/components/button') ||
            id.includes('element-plus/es/components/dialog') ||
            id.includes('element-plus/es/components/form') ||
            id.includes('element-plus/es/components/input') ||
            id.includes('element-plus/es/components/input-number') ||
            id.includes('element-plus/es/components/loading') ||
            id.includes('element-plus/es/components/option') ||
            id.includes('element-plus/es/components/popper') ||
            id.includes('element-plus/es/components/radio') ||
            id.includes('element-plus/es/components/row') ||
            id.includes('element-plus/es/components/scrollbar') ||
            id.includes('element-plus/es/components/select') ||
            id.includes('element-plus/es/components/tag') ||
            id.includes('element-plus/es/components/tooltip')
          ) {
            return 'vendor-element-form';
          }

          if (
            id.includes('element-plus/es/components/checkbox') ||
            id.includes('element-plus/es/components/pagination') ||
            id.includes('element-plus/es/components/table')
          ) {
            return 'vendor-element-table';
          }

          if (
            id.includes('element-plus/es/components/card') ||
            id.includes('element-plus/es/components/col') ||
            id.includes('element-plus/es/components/descriptions') ||
            id.includes('element-plus/es/components/empty') ||
            id.includes('element-plus/es/components/progress') ||
            id.includes('element-plus/es/components/skeleton') ||
            id.includes('element-plus/es/components/statistic')
          ) {
            return 'vendor-element-panel';
          }

          if (id.includes('vue-router')) {
            return 'vendor-vue-router';
          }

          if (id.includes('pinia')) {
            return 'vendor-pinia';
          }

          if (id.includes('node_modules/vue/') || id.includes('/node_modules/@vue/')) {
            return 'vendor-vue';
          }
        }
      }
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/__tests__/setup.ts',
    include: ['./src/__tests__/**/*.test.ts'],
    server: {
      deps: {
        inline: ['element-plus']
      }
    },
    reporters: ['verbose'],
    outputDirectory: './coverage',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: [
        'node_modules/',
        'src/__tests__/'
      ]
    }
  },
    server: {
      host: '0.0.0.0',
      port: 5174,
      proxy: {
        '^/api(?:/|$)': {
          target: proxyTarget,
          changeOrigin: true
        }
      }
    }
  } as any;
});
