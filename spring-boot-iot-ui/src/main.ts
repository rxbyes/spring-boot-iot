import { createApp } from 'vue';
import { createPinia } from 'pinia';
import {
  ElButton,
  ElCard,
  ElConfigProvider,
  ElDescriptions,
  ElDescriptionsItem,
  ElEmpty,
  ElInput,
  ElOption,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus';

import App from './App.vue';
import router from './router';
import { registerDefaultInterceptors } from './api/interceptors';
import 'element-plus/es/components/button/style/css';
import 'element-plus/es/components/card/style/css';
import 'element-plus/es/components/config-provider/style/css';
import 'element-plus/es/components/descriptions/style/css';
import 'element-plus/es/components/empty/style/css';
import 'element-plus/es/components/input/style/css';
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/option/style/css';
import 'element-plus/es/components/select/style/css';
import 'element-plus/es/components/table/style/css';
import 'element-plus/es/components/tag/style/css';
import './styles/tokens.css';
import './styles/element-overrides.css';
import './styles/global.css';

const app = createApp(App);
const pinia = createPinia();

app.component('ElButton', ElButton);
app.component('ElCard', ElCard);
app.component('ElConfigProvider', ElConfigProvider);
app.component('ElDescriptions', ElDescriptions);
app.component('ElDescriptionsItem', ElDescriptionsItem);
app.component('ElEmpty', ElEmpty);
app.component('ElInput', ElInput);
app.component('ElOption', ElOption);
app.component('ElSelect', ElSelect);
app.component('ElTable', ElTable);
app.component('ElTableColumn', ElTableColumn);
app.component('ElTag', ElTag);

// 注册默认拦截器
registerDefaultInterceptors();

// 使用 Pinia 状态管理
app.use(pinia);

// 使用路由
app.use(router);

app.mount('#app');
