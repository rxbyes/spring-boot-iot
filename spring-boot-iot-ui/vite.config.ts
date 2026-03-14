import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:9999';

  return {
    plugins: [vue()],
    build: {
      chunkSizeWarningLimit: 850,
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes('node_modules/echarts')) {
              return 'echarts-vendor';
            }
            if (id.includes('node_modules/element-plus') || id.includes('node_modules/@element-plus')) {
              return 'element-plus-vendor';
            }
            if (id.includes('node_modules/vue') || id.includes('node_modules/vue-router')) {
              return 'vue-vendor';
            }
          }
        }
      }
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      proxy: {
        '/device': {
          target: proxyTarget,
          changeOrigin: true
        },
        '/message': {
          target: proxyTarget,
          changeOrigin: true
        }
      }
    }
  };
});
