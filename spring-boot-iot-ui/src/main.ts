import { createApp } from 'vue';
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

app.use(router).mount('#app');
