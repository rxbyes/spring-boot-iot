import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import { pushVisitedTab } from '../stores/tabs';
import { usePermissionStore } from '../stores/permission';

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: {
      title: '登录',
      description: '平台统一登录入口。',
      requiresAuth: false,
      layout: 'blank',
      trackTab: false
    }
  },
  {
    path: '/',
    name: 'cockpit',
    component: () => import('../views/CockpitView.vue'),
    meta: {
      title: '监控总览',
      description: '平台实时运行总览与关键指标看板。',
      requiresAuth: true
    }
  },
  {
    path: '/products',
    name: 'products',
    component: () => import('../views/ProductWorkbenchView.vue'),
    meta: {
      title: '产品管理',
      description: '产品档案、协议和数据模型管理。',
      requiresAuth: true
    }
  },
  {
    path: '/devices',
    name: 'devices',
    component: () => import('../views/DeviceWorkbenchView.vue'),
    meta: {
      title: '设备管理',
      description: '设备注册、凭证维护与在线状态查看。',
      requiresAuth: true
    }
  },
  {
    path: '/reporting',
    name: 'reporting',
    component: () => import('../views/ReportWorkbenchView.vue'),
    meta: {
      title: '上报调试',
      description: '模拟 HTTP 上报并检查消息解析结果。',
      requiresAuth: true
    }
  },
  {
    path: '/insight',
    name: 'insight',
    component: () => import('../views/DeviceInsightView.vue'),
    meta: {
      title: '设备洞察',
      description: '设备属性、日志与运行态势分析。',
      requiresAuth: true
    }
  },
  {
    path: '/file-debug',
    name: 'file-debug',
    component: () => import('../views/FilePayloadDebugView.vue'),
    meta: {
      title: '文件调试',
      description: '文件类报文与固件分包调试能力。',
      requiresAuth: true
    }
  },
  {
    path: '/future-lab',
    name: 'future-lab',
    component: () => import('../views/FutureLabView.vue'),
    meta: {
      title: '未来实验室',
      description: '预研能力展示与扩展能力验证入口。',
      requiresAuth: true
    }
  },
  {
    path: '/alarm-center',
    name: 'alarm-center',
    component: () => import('../views/AlarmCenterView.vue'),
    meta: {
      title: '告警中心',
      description: '告警列表、详情、确认与抑制管理。',
      requiresAuth: true
    }
  },
  {
    path: '/event-disposal',
    name: 'event-disposal',
    component: () => import('../views/EventDisposalView.vue'),
    meta: {
      title: '事件处置',
      description: '事件工单派发、闭环与复盘管理。',
      requiresAuth: true
    }
  },
  {
    path: '/risk-point',
    name: 'risk-point',
    component: () => import('../views/RiskPointView.vue'),
    meta: {
      title: '风险点管理',
      description: '风险点 CRUD 与设备绑定维护。',
      requiresAuth: true
    }
  },
  {
    path: '/rule-definition',
    name: 'rule-definition',
    component: () => import('../views/RuleDefinitionView.vue'),
    meta: {
      title: '阈值规则',
      description: '阈值规则定义、测试和启停管理。',
      requiresAuth: true
    }
  },
  {
    path: '/linkage-rule',
    name: 'linkage-rule',
    component: () => import('../views/LinkageRuleView.vue'),
    meta: {
      title: '联动规则',
      description: '联动触发条件与动作配置管理。',
      requiresAuth: true
    }
  },
  {
    path: '/emergency-plan',
    name: 'emergency-plan',
    component: () => import('../views/EmergencyPlanView.vue'),
    meta: {
      title: '应急预案',
      description: '应急预案维护与联动绑定管理。',
      requiresAuth: true
    }
  },
  {
    path: '/report-analysis',
    name: 'report-analysis',
    component: () => import('../views/ReportAnalysisView.vue'),
    meta: {
      title: '分析报表',
      description: '风险趋势、告警统计、闭环与健康分析。',
      requiresAuth: true
    }
  },
  {
    path: '/organization',
    name: 'organization',
    component: () => import('../views/OrganizationView.vue'),
    meta: {
      title: '组织机构',
      description: '组织树维护与层级管理。',
      requiresAuth: true
    }
  },
  {
    path: '/user',
    name: 'user',
    component: () => import('../views/UserView.vue'),
    meta: {
      title: '用户管理',
      description: '用户档案、状态与重置密码管理。',
      requiresAuth: true
    }
  },
  {
    path: '/role',
    name: 'role',
    component: () => import('../views/RoleView.vue'),
    meta: {
      title: '角色管理',
      description: '角色、菜单与权限绑定管理。',
      requiresAuth: true
    }
  },
  {
    path: '/region',
    name: 'region',
    component: () => import('../views/RegionView.vue'),
    meta: {
      title: '区域管理',
      description: '行政区域树与地域配置管理。',
      requiresAuth: true
    }
  },
  {
    path: '/dict',
    name: 'dict',
    component: () => import('../views/DictView.vue'),
    meta: {
      title: '字典配置',
      description: '字典类型与字典项维护。',
      requiresAuth: true
    }
  },
  {
    path: '/channel',
    name: 'channel',
    component: () => import('../views/ChannelView.vue'),
    meta: {
      title: '通知渠道',
      description: '通知渠道配置、启停与测试管理。',
      requiresAuth: true
    }
  },
  {
    path: '/audit-log',
    name: 'audit-log',
    component: () => import('../views/AuditLogView.vue'),
    meta: {
      title: '审计日志',
      description: '系统关键操作审计查询。',
      requiresAuth: true
    }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  }
});

router.beforeEach((to, from, next) => {
  const permissionStore = usePermissionStore();
  const requiresAuth = to.meta.requiresAuth !== false;

  if (to.path === '/login') {
    if (permissionStore.isLoggedIn) {
      const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/';
      next(redirect);
      return;
    }
    next();
    return;
  }

  if (requiresAuth && !permissionStore.isLoggedIn) {
    next({
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    });
    return;
  }

  const requiredPermission = to.meta.permission as string | undefined;
  if (requiredPermission && !permissionStore.hasPermission(requiredPermission)) {
    next('/');
    return;
  }

  next();
});

router.afterEach((to) => {
  const title = String(to.meta.title || '监控总览');
  document.title = `${title} | Spring Boot IoT`;
  if (to.meta.trackTab === false) {
    return;
  }
  pushVisitedTab({
    path: to.path,
    title
  });
});

export default router;
