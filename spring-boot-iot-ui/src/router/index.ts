import { createRouter, createWebHistory } from 'vue-router';
import { pushVisitedTab } from '../stores/tabs';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'cockpit',
      component: () => import('../views/CockpitView.vue'),
      meta: {
        title: '风险监测驾驶舱',
        description: '总览风险态势、角色工作入口、平台能力映射与商业化演进方向。'
      }
    },
    {
      path: '/products',
      name: 'products',
      component: () => import('../views/ProductWorkbenchView.vue'),
      meta: {
        title: '产品模板中心',
        description: '创建产品并维护产品模板，为设备接入、协议解析和后续业务归属提供基础。'
      }
    },
    {
      path: '/devices',
      name: 'devices',
      component: () => import('../views/DeviceWorkbenchView.vue'),
      meta: {
        title: '设备运维中心',
        description: '创建设备、检索状态并观察在线与最近上报情况，为远程运维与阈值管理打底。'
      }
    },
    {
      path: '/reporting',
      name: 'reporting',
      component: () => import('../views/ReportWorkbenchView.vue'),
      meta: {
        title: '接入回放台',
        description: '模拟设备上报、推演 topic 和 payload，并回放现有接入主链路行为。'
      }
    },
    {
      path: '/insight',
      name: 'insight',
      component: () => import('../views/DeviceInsightView.vue'),
      meta: {
        title: '风险点工作台',
        description: '聚合设备详情、属性、消息日志与趋势入口，作为一线风险研判和上报的核心工作页。'
      }
    },
    {
      path: '/file-debug',
      name: 'file-debug',
      component: () => import('../views/FilePayloadDebugView.vue'),
      meta: {
        title: '文件与固件调试',
        description: '查看 C.3 文件快照和 C.4 固件聚合结果，服务运维与研发验证文件链路。'
      }
    },
    {
      path: '/future-lab',
      name: 'future-lab',
      component: () => import('../views/FutureLabView.vue'),
      meta: {
        title: '未来演进蓝图',
        description: '为点位图表、数字孪生、规则引擎、告警中心和 OTA 运维预留扩展契约。'
      }
    }
  ],
  scrollBehavior() {
    return { top: 0 };
  }
});

router.afterEach((to) => {
  document.title = `${String(to.meta.title || '调试台')} | Spring Boot IoT`;
  pushVisitedTab({
    path: to.path,
    title: String(to.meta.title || '调试台')
  });
});

export default router;
