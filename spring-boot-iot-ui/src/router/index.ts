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
        title: '调试驾驶舱',
        description: '总览当前 Phase 1 主链路、最近联调记录和下一阶段预留能力。'
      }
    },
    {
      path: '/products',
      name: 'products',
      component: () => import('../views/ProductWorkbenchView.vue'),
      meta: {
        title: '产品工作台',
        description: '创建产品并验证产品模板是否可被设备建档与协议解析链路正确引用。'
      }
    },
    {
      path: '/devices',
      name: 'devices',
      component: () => import('../views/DeviceWorkbenchView.vue'),
      meta: {
        title: '设备工作台',
        description: '创建设备、按 ID 或编码检索设备，并观察在线与最近上报状态。'
      }
    },
    {
      path: '/reporting',
      name: 'reporting',
      component: () => import('../views/ReportWorkbenchView.vue'),
      meta: {
        title: 'HTTP 上报实验台',
        description: '模拟设备上报、推演 topic 和 payload，并回放主链路行为。'
      }
    },
    {
      path: '/insight',
      name: 'insight',
      component: () => import('../views/DeviceInsightView.vue'),
      meta: {
        title: '设备洞察',
        description: '聚合设备详情、最新属性与消息日志，作为后续图表与数字孪生的数据入口。'
      }
    },
    {
      path: '/file-debug',
      name: 'file-debug',
      component: () => import('../views/FilePayloadDebugView.vue'),
      meta: {
        title: '文件调试台',
        description: '查看 C.3 文件快照和 C.4 固件聚合结果，验证 Redis 文件消费链路。'
      }
    },
    {
      path: '/future-lab',
      name: 'future-lab',
      component: () => import('../views/FutureLabView.vue'),
      meta: {
        title: '未来实验室',
        description: '为点位图表、数字孪生、网关拓扑和规则引擎预留扩展契约。'
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
