import { createApp } from 'vue';
import { createPinia } from 'pinia';
import {
  ElButton,
  ElCard,
  ElConfigProvider,
  ElDescriptions,
  ElDescriptionsItem,
  ElEmpty,
  ElIcon,
  ElInput,
  ElOption,
  ElProgress,
  ElRadioGroup,
  ElRadioButton,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
  ElStatistic,
  ElForm,
  ElFormItem,
  ElRow,
  ElCol,
  ElPagination,
  ElDialog,
  ElInputNumber
} from 'element-plus';

import App from './App.vue';
import router from './router';
import { registerDefaultInterceptors } from './api/interceptors';
import { permission } from './directives/permission';
import 'element-plus/es/components/button/style/css';
import 'element-plus/es/components/card/style/css';
import 'element-plus/es/components/config-provider/style/css';
import 'element-plus/es/components/descriptions/style/css';
import 'element-plus/es/components/empty/style/css';
import 'element-plus/es/components/input/style/css';
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/icon/style/css';
import 'element-plus/es/components/option/style/css';
import 'element-plus/es/components/progress/style/css';
import 'element-plus/es/components/radio-group/style/css';
import 'element-plus/es/components/radio-button/style/css';
import 'element-plus/es/components/select/style/css';
import 'element-plus/es/components/table/style/css';
import 'element-plus/es/components/tag/style/css';
import 'element-plus/es/components/statistic/style/css';
import 'element-plus/es/components/form/style/css';
import 'element-plus/es/components/form-item/style/css';
import 'element-plus/es/components/row/style/css';
import 'element-plus/es/components/col/style/css';
import 'element-plus/es/components/pagination/style/css';
import 'element-plus/es/components/dialog/style/css';
import 'element-plus/es/components/input-number/style/css';
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
app.component('ElIcon', ElIcon);
app.component('ElProgress', ElProgress);
app.component('ElRadioGroup', ElRadioGroup);
app.component('ElRadioButton', ElRadioButton);
app.component('ElSelect', ElSelect);
app.component('ElTable', ElTable);
app.component('ElTableColumn', ElTableColumn);
app.component('ElTag', ElTag);
app.component('ElStatistic', ElStatistic);
app.component('ElForm', ElForm);
app.component('ElFormItem', ElFormItem);
app.component('ElRow', ElRow);
app.component('ElCol', ElCol);
app.component('ElPagination', ElPagination);
app.component('ElDialog', ElDialog);
app.component('ElInputNumber', ElInputNumber);

// 注册默认拦截器
registerDefaultInterceptors();

// 使用 Pinia 状态管理
app.use(pinia);

// 使用路由
app.use(router);

app.mount('#app');

// 注册按钮权限指令
app.directive('permission', permission);
