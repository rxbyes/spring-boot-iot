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
      title: '平台首页',
      description: '监测预警平台的产品首页与业务总览。',
      requiresAuth: true,
      trackTab: false
    }
  },
  {
    path: '/device-access',
    name: 'device-access',
    component: () => import('../views/SectionLandingView.vue'),
    meta: {
      title: '设备接入',
      description: '设备接入分组总览与常用入口。',
      requiresAuth: true,
      trackTab: false
    }
  },
  {
    path: '/products',
    name: 'products',
    component: () => import('../views/ProductWorkbenchView.vue'),
    meta: {
      title: '产品模板中心',
      description: '产品模板建模、协议绑定与设备归属管理。',
      requiresAuth: true
    }
  },
  {
    path: '/devices',
    name: 'devices',
    component: () => import('../views/DeviceWorkbenchView.vue'),
    meta: {
      title: '设备运维中心',
      description: '设备建档、在线状态核查与基础运维。',
      requiresAuth: true
    }
  },
  {
    path: '/reporting',
    name: 'reporting',
    component: () => import('../views/ReportWorkbenchView.vue'),
    meta: {
      title: '接入验证中心',
      description: '模拟 HTTP 上报并核验接入链路解析结果。',
      requiresAuth: true
    }
  },
  {
    path: '/insight',
    name: 'insight',
    component: () => import('../views/DeviceInsightView.vue'),
    meta: {
      title: '监测对象工作台',
      description: '聚合设备属性、日志与监测对象研判线索。',
      requiresAuth: true
    }
  },
  {
    path: '/file-debug',
    name: 'file-debug',
    component: () => import('../views/FilePayloadDebugView.vue'),
    meta: {
      title: '数据完整性校验',
      description: '文件类报文与固件分包的完整性核验能力。',
      requiresAuth: true
    }
  },
  {
    path: '/system-log',
    name: 'system-log',
    component: () => import('../views/AuditLogView.vue'),
    meta: {
      title: '系统日志',
      description: '设备接入链路的系统异常定位与调试回看。',
      requiresAuth: true
    }
  },
  {
    path: '/message-trace',
    name: 'message-trace',
    component: () => import('../views/MessageTraceView.vue'),
    meta: {
      title: '消息追踪',
      description: '按 TraceId、设备编码与 Topic 排查设备接入链路。',
      requiresAuth: true
    }
  },
  {
    path: '/future-lab',
    name: 'future-lab',
    component: () => import('../views/FutureLabView.vue'),
    meta: {
      title: '演进蓝图',
      description: '预研能力展示与未来扩展方向说明。',
      requiresAuth: true
    }
  },
  {
    path: '/risk-disposal',
    name: 'risk-disposal',
    component: () => import('../views/SectionLandingView.vue'),
    meta: {
      title: '风险处置',
      description: '风险处置分组总览与闭环入口。',
      requiresAuth: true,
      trackTab: false
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
    path: '/risk-monitoring',
    name: 'risk-monitoring',
    component: () => import('../views/RealTimeMonitoringView.vue'),
    meta: {
      title: '实时监测',
      description: '风险监测实时列表与统一详情抽屉。',
      requiresAuth: true
    }
  },
  {
    path: '/risk-monitoring-gis',
    name: 'risk-monitoring-gis',
    component: () => import('../views/RiskGisView.vue'),
    meta: {
      title: 'GIS 风险态势',
      description: '基于 ECharts 的风险点位分布与详情联动。',
      requiresAuth: true
    }
  },
  {
    path: '/system-management',
    name: 'system-management',
    component: () => import('../views/SectionLandingView.vue'),
    meta: {
      title: '系统管理',
      description: '系统管理分组总览与治理入口。',
      requiresAuth: true,
      trackTab: false
    }
  },
  {
    path: '/organization',
    name: 'organization',
    component: () => import('../views/OrganizationView.vue'),
    meta: {
      title: '组织机构',
      description: '组织树维护与层级管理。',
      requiresAuth: true,
      trackTab: false
    }
  },
  {
    path: '/user',
    name: 'user',
    component: () => import('../views/UserView.vue'),
    meta: {
      title: '用户管理',
      description: '用户档案、状态与重置密码管理。',
      requiresAuth: true,
      trackTab: false
    }
  },
  {
    path: '/role',
    name: 'role',
    component: () => import('../views/RoleView.vue'),
    meta: {
      title: '角色管理',
      description: '角色、菜单与权限绑定管理。',
      requiresAuth: true,
      trackTab: false
    }
  },
  {
    path: '/menu',
    name: 'menu',
    component: () => import('../views/MenuView.vue'),
    meta: {
      title: '菜单管理',
      description: '菜单树维护与页面/按钮权限项管理。',
      requiresAuth: true,
      trackTab: false
    }
  },
  {
    path: '/region',
    name: 'region',
    component: () => import('../views/RegionView.vue'),
    meta: {
      title: '区域管理',
      description: '行政区域树与地域配置管理。',
      requiresAuth: true,
      trackTab: false
    }
  },
  {
    path: '/dict',
    name: 'dict',
    component: () => import('../views/DictView.vue'),
    meta: {
      title: '字典配置',
      description: '字典类型与字典项维护。',
      requiresAuth: true,
      trackTab: false
    }
  },
  {
    path: '/channel',
    name: 'channel',
    component: () => import('../views/ChannelView.vue'),
    meta: {
      title: '通知渠道',
      description: '通知渠道配置、启停与测试管理。',
      requiresAuth: true,
      trackTab: false
    }
  },
  {
    path: '/automation-test',
    name: 'automation-test',
    component: () => import('../views/AutomationTestCenterView.vue'),
    meta: {
      title: '自动化测试',
      description: '配置驱动的浏览器自动化编排、报告与测试建议中心。',
      requiresAuth: true,
      trackTab: false
    }
  },
  {
    path: '/audit-log',
    name: 'audit-log',
    component: () => import('../views/AuditLogView.vue'),
    meta: {
      title: '业务日志',
      description: '面向业务与治理侧的关键操作审计查询。',
      requiresAuth: true,
      trackTab: false
    }
  },
  {
    path: '/risk-enhance',
    name: 'risk-enhance',
    component: () => import('../views/SectionLandingView.vue'),
    meta: {
      title: '风险增强',
      description: '风险增强分组总览与试运行入口。',
      requiresAuth: true,
      trackTab: false
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

function resolveRedirectTarget(permissionStore: ReturnType<typeof usePermissionStore>, redirect?: string) {
  if (redirect && redirect.startsWith('/') && redirect !== '/login') {
    if (redirect === '/' || permissionStore.hasRoutePermission(redirect)) {
      return redirect;
    }
  }
  return permissionStore.homePath || '/';
}

router.beforeEach(async (to) => {
  const permissionStore = usePermissionStore();
  const requiresAuth = to.meta.requiresAuth !== false;

  if (permissionStore.isLoggedIn) {
    try {
      await permissionStore.ensureInitialized();
    } catch {
      if (to.path === '/login') {
        return true;
      }
      return {
        path: '/login',
        query: {
          redirect: to.fullPath
        }
      };
    }
  }

  if (to.path === '/login') {
    if (permissionStore.isLoggedIn) {
      return resolveRedirectTarget(
        permissionStore,
        typeof to.query.redirect === 'string' ? to.query.redirect : undefined
      );
    }
    return true;
  }

  if (requiresAuth && !permissionStore.isLoggedIn) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    };
  }

  if (requiresAuth && !permissionStore.hasRoutePermission(to.path)) {
    return resolveRedirectTarget(permissionStore);
  }

  const requiredPermission = to.meta.permission as string | undefined;
  if (requiredPermission && !permissionStore.hasPermission(requiredPermission)) {
    return resolveRedirectTarget(permissionStore);
  }

  return true;
});

router.afterEach((to) => {
  const title = String(to.meta.title || '平台首页');
  document.title = `${title} | 监测预警平台`;
  if (to.meta.trackTab === false) {
    return;
  }
  pushVisitedTab({
    path: to.path,
    title
  });
});

export default router;
