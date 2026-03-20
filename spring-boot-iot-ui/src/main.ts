import { createApp } from 'vue';
import { createPinia } from 'pinia';

import App from './App.vue';
import router from './router/index';
import { registerDefaultInterceptors } from './api/interceptors';
import { permission } from './directives/permission';
import './styles/tokens.css';
import './styles/element-overrides.css';
import './styles/global.css';

const app = createApp(App);
const pinia = createPinia();

// 注册默认拦截器
registerDefaultInterceptors();

// 使用 Pinia 状态管理
app.use(pinia);

// 使用路由
app.use(router);

// 注册按钮权限指令
app.directive('permission', permission);

app.mount('#app');
