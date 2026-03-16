import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return;
          }

          if (id.includes('echarts')) {
            return 'vendor-echarts-core';
          }

          if (id.includes('zrender')) {
            return 'vendor-zrender';
          }

          if (id.includes('@element-plus/icons-vue')) {
            return 'vendor-element-icons';
          }

          if (id.includes('element-plus')) {
            if (id.includes('element-plus/es/components')) {
              if (id.includes('element-plus/es/components/table') || id.includes('element-plus/es/components/pagination') || id.includes('element-plus/es/components/descriptions') || id.includes('element-plus/es/components/statistic') || id.includes('element-plus/es/components/progress')) {
                return 'vendor-element-data';
              }

              if (id.includes('element-plus/es/components/form') || id.includes('element-plus/es/components/input') || id.includes('element-plus/es/components/select') || id.includes('element-plus/es/components/option') || id.includes('element-plus/es/components/radio') || id.includes('element-plus/es/components/date-picker')) {
                return 'vendor-element-form';
              }

              return 'vendor-element-basic';
            }

            return 'vendor-element-core';
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

          return 'vendor-misc';
        }
      }
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/__tests__/setup.ts',
    include: ['./src/__tests__/**/*.test.ts'],
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
      '/api': {
        target: 'http://localhost:9999',
        changeOrigin: true
      },
      '/device': {
        target: 'http://localhost:9999',
        changeOrigin: true
      },
      '/message': {
        target: 'http://localhost:9999',
        changeOrigin: true
      },
    }
  }
} as any);
