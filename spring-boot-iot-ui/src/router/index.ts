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
    path: '/products/:productId',
    redirect: (to) => `/products/${String(to.params.productId || '').trim()}/overview`
  },
  {
    path: '/products/:productId/overview',
    name: 'product-overview',
    component: () => import('../views/ProductDetailWorkbenchView.vue'),
    meta: routeMeta('/products', {
      title: '产品总览',
      description: '查看产品概览、正式字段规模与最新合同发布状态。'
    })
  },
  {
    path: '/products/:productId/devices',
    name: 'product-devices',
    component: () => import('../views/ProductDetailWorkbenchView.vue'),
    meta: routeMeta('/products', {
      title: '关联设备',
      description: '查看当前产品下的设备清单、在线状态与最近上报。'
    })
  },
  {
    path: '/products/:productId/contracts',
    name: 'product-contracts',
    component: () => import('../views/ProductDetailWorkbenchView.vue'),
    meta: routeMeta('/products', {
      title: '契约字段',
      description: '只保留样本输入、识别结果、本次生效与当前已生效字段。'
    })
  },
  {
    path: '/products/:productId/mapping-rules',
    name: 'product-mapping-rules',
    component: () => import('../views/ProductDetailWorkbenchView.vue'),
    meta: routeMeta('/products', {
      title: '映射规则',
      description: '集中维护厂商字段映射建议与映射规则台账。'
    })
  },
  {
    path: '/products/:productId/releases',
    name: 'product-releases',
    component: () => import('../views/ProductDetailWorkbenchView.vue'),
    meta: routeMeta('/products', {
      title: '版本台账',
      description: '查看发布批次、回滚试算与跨批次差异对账。'
    })
  },
  {
    path: '/protocol-governance',
    name: 'protocol-governance',
    component: () => import('../views/ProtocolGovernanceWorkbenchView.vue'),
    meta: routeMeta('/protocol-governance')
  },
  {
    path: '/device-onboarding',
    name: 'device-onboarding',
    component: () => import('../views/DeviceOnboardingWorkbenchView.vue'),
    meta: routeMeta('/device-onboarding')
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
    path: '/governance-approval',
    name: 'governance-approval',
    component: () => import('../views/GovernanceApprovalView.vue'),
    meta: routeMeta('/governance-approval')
  },
  {
    path: '/governance-security',
    name: 'governance-security',
    component: () => import('../views/GovernanceSecurityView.vue'),
    meta: routeMeta('/governance-security')
  },
  {
    path: '/governance-task',
    name: 'governance-task',
    component: () => import('../views/GovernanceTaskView.vue'),
    meta: routeMeta('/governance-task')
  },
  {
    path: '/governance-ops',
    name: 'governance-ops',
    component: () => import('../views/GovernanceOpsWorkbenchView.vue'),
    meta: routeMeta('/governance-ops')
  },
  {
    path: '/quality-workbench',
    name: 'quality-workbench',
    component: () => import('../views/QualityWorkbenchLandingView.vue'),
    meta: routeMeta('/quality-workbench')
  },
  {
    path: '/business-acceptance',
    name: 'business-acceptance',
    component: () => import('../views/BusinessAcceptanceWorkbenchView.vue'),
    meta: routeMeta('/business-acceptance')
  },
  {
    path: '/business-acceptance/results/:runId',
    name: 'business-acceptance-results',
    component: () => import('../views/BusinessAcceptanceResultView.vue'),
    meta: routeMeta('/business-acceptance', {
      title: '业务验收结果',
      description: '查看业务验收包的模块结论与失败明细。'
    })
  },
  {
    path: '/automation-governance',
    name: 'automation-governance',
    component: () => import('../views/AutomationGovernanceWorkbenchView.vue'),
    meta: routeMeta('/automation-governance')
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
    if (permissionStore.isLoggedIn) {
      return resolveRedirectTarget(permissionStore, to.fullPath);
    }
    return createLoginRedirect(to.fullPath);
  }

  if (requiresAuth && !permissionStore.isLoggedIn) {
    return createLoginRedirect(to.fullPath);
  }

  if (requiresAuth && !permissionStore.hasRoutePermission(to.path)) {
    return resolveRedirectTarget(permissionStore, to.fullPath);
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
