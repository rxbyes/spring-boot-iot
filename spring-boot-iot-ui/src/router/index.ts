import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import { usePermissionStore } from '../stores/permission';
import { getRouteMetaPreset } from '../utils/sectionWorkspaces';
import { appScrollBehavior } from './scrollBehavior';

function routeMeta(path: string, overrides: Record<string, unknown> = {}) {
  return {
    requiresAuth: true,
    ...(getRouteMetaPreset(path) || {}),
    ...overrides
  };
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: routeMeta('/login')
  },
  {
    path: '/',
    name: 'cockpit',
    component: () => import('../views/CockpitView.vue'),
    meta: routeMeta('/')
  },
  {
    path: '/device-access',
    name: 'device-access',
    component: () => import('../views/SectionLandingView.vue'),
    meta: routeMeta('/device-access')
  },
  {
    path: '/products',
    name: 'products',
    component: () => import('../views/ProductWorkbenchView.vue'),
    meta: routeMeta('/products')
  },
  {
    path: '/devices',
    name: 'devices',
    component: () => import('../views/DeviceWorkbenchView.vue'),
    meta: routeMeta('/devices')
  },
  {
    path: '/reporting',
    name: 'reporting',
    component: () => import('../views/ReportWorkbenchView.vue'),
    meta: routeMeta('/reporting')
  },
  {
    path: '/insight',
    name: 'insight',
    component: () => import('../views/DeviceInsightView.vue'),
    meta: routeMeta('/insight')
  },
  {
    path: '/file-debug',
    name: 'file-debug',
    component: () => import('../views/FilePayloadDebugView.vue'),
    meta: routeMeta('/file-debug')
  },
  {
    path: '/system-log',
    name: 'system-log',
    component: () => import('../views/AuditLogView.vue'),
    meta: routeMeta('/system-log')
  },
  {
    path: '/message-trace',
    name: 'message-trace',
    component: () => import('../views/MessageTraceView.vue'),
    meta: routeMeta('/message-trace')
  },
  {
    path: '/future-lab',
    name: 'future-lab',
    component: () => import('../views/FutureLabView.vue'),
    meta: routeMeta('/future-lab')
  },
  {
    path: '/risk-disposal',
    name: 'risk-disposal',
    component: () => import('../views/SectionLandingView.vue'),
    meta: routeMeta('/risk-disposal')
  },
  {
    path: '/risk-config',
    name: 'risk-config',
    component: () => import('../views/SectionLandingView.vue'),
    meta: routeMeta('/risk-config')
  },
  {
    path: '/alarm-center',
    name: 'alarm-center',
    component: () => import('../views/AlarmCenterView.vue'),
    meta: routeMeta('/alarm-center')
  },
  {
    path: '/event-disposal',
    name: 'event-disposal',
    component: () => import('../views/EventDisposalView.vue'),
    meta: routeMeta('/event-disposal')
  },
  {
    path: '/risk-point',
    name: 'risk-point',
    component: () => import('../views/RiskPointView.vue'),
    meta: routeMeta('/risk-point')
  },
  {
    path: '/rule-definition',
    name: 'rule-definition',
    component: () => import('../views/RuleDefinitionView.vue'),
    meta: routeMeta('/rule-definition')
  },
  {
    path: '/linkage-rule',
    name: 'linkage-rule',
    component: () => import('../views/LinkageRuleView.vue'),
    meta: routeMeta('/linkage-rule')
  },
  {
    path: '/emergency-plan',
    name: 'emergency-plan',
    component: () => import('../views/EmergencyPlanView.vue'),
    meta: routeMeta('/emergency-plan')
  },
  {
    path: '/report-analysis',
    name: 'report-analysis',
    component: () => import('../views/ReportAnalysisView.vue'),
    meta: routeMeta('/report-analysis')
  },
  {
    path: '/risk-monitoring',
    name: 'risk-monitoring',
    component: () => import('../views/RealTimeMonitoringView.vue'),
    meta: routeMeta('/risk-monitoring')
  },
  {
    path: '/risk-monitoring-gis',
    name: 'risk-monitoring-gis',
    component: () => import('../views/RiskGisView.vue'),
    meta: routeMeta('/risk-monitoring-gis')
  },
  {
    path: '/system-management',
    name: 'system-management',
    component: () => import('../views/SectionLandingView.vue'),
    meta: routeMeta('/system-management')
  },
  {
    path: '/organization',
    name: 'organization',
    component: () => import('../views/OrganizationView.vue'),
    meta: routeMeta('/organization')
  },
  {
    path: '/user',
    name: 'user',
    component: () => import('../views/UserView.vue'),
    meta: routeMeta('/user')
  },
  {
    path: '/role',
    name: 'role',
    component: () => import('../views/RoleView.vue'),
    meta: routeMeta('/role')
  },
  {
    path: '/menu',
    name: 'menu',
    component: () => import('../views/MenuView.vue'),
    meta: routeMeta('/menu')
  },
  {
    path: '/region',
    name: 'region',
    component: () => import('../views/RegionView.vue'),
    meta: routeMeta('/region')
  },
  {
    path: '/dict',
    name: 'dict',
    component: () => import('../views/DictView.vue'),
    meta: routeMeta('/dict')
  },
  {
    path: '/channel',
    name: 'channel',
    component: () => import('../views/ChannelView.vue'),
    meta: routeMeta('/channel')
  },
  {
    path: '/in-app-message',
    name: 'in-app-message',
    component: () => import('../views/InAppMessageView.vue'),
    meta: routeMeta('/in-app-message')
  },
  {
    path: '/help-doc',
    name: 'help-doc',
    component: () => import('../views/HelpDocView.vue'),
    meta: routeMeta('/help-doc')
  },
  {
    path: '/quality-workbench',
    name: 'quality-workbench',
    component: () => import('../views/SectionLandingView.vue'),
    meta: routeMeta('/quality-workbench')
  },
  {
    path: '/automation-assets',
    name: 'automation-assets',
    component: () => import('../views/AutomationAssetsView.vue'),
    meta: routeMeta('/automation-assets')
  },
  {
    path: '/automation-test',
    name: 'automation-test',
    component: () => import('../views/AutomationTestCenterView.vue'),
    meta: routeMeta('/automation-test')
  },
  {
    path: '/audit-log',
    name: 'audit-log',
    component: () => import('../views/AuditLogView.vue'),
    meta: routeMeta('/audit-log')
  },
  {
    path: '/risk-enhance',
    name: 'risk-enhance',
    redirect: '/risk-disposal'
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: appScrollBehavior
});

function resolveRedirectTarget(permissionStore: ReturnType<typeof usePermissionStore>, redirect?: string) {
  if (redirect && redirect.startsWith('/') && redirect !== '/login') {
    if (redirect === '/' || permissionStore.hasRoutePermission(redirect)) {
      return redirect;
    }
  }
  return permissionStore.homePath || '/';
}

function createLoginRedirect(fullPath: string) {
  if (!fullPath || fullPath === '/login') {
    return {
      path: '/login'
    };
  }
  return {
    path: '/login',
    query: {
      redirect: fullPath
    }
  };
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

  if (to.matched.length === 0) {
    permissionStore.logout();
    return createLoginRedirect(to.fullPath);
  }

  if (requiresAuth && !permissionStore.isLoggedIn) {
    return createLoginRedirect(to.fullPath);
  }

  if (requiresAuth && !permissionStore.hasRoutePermission(to.path)) {
    permissionStore.logout();
    return createLoginRedirect(to.fullPath);
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
});

export default router;
