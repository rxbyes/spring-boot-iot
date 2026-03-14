import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:9999';

  return {
    plugins: [vue()],
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
