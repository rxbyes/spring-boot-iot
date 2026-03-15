import { createRouter, createWebHistory } from 'vue-router';
import { pushVisitedTab } from '../stores/tabs';
import { usePermissionStore } from '../stores/permission';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'cockpit',
      component: () => import('../views/CockpitView.vue'),
      meta: {
        title: '风险监测驾驶舱',
        description: '总览风险态势、角色工作入口、平台能力映射与商业化演进方向。',
        requiresAuth: false
      }
    },
    {
      path: '/products',
      name: 'products',
      component: () => import('../views/ProductWorkbenchView.vue'),
      meta: {
        title: '产品模板中心',
        description: '创建产品并维护产品模板，为设备接入、协议解析和后续业务归属提供基础。',
        requiresAuth: false
      }
    },
    {
      path: '/devices',
      name: 'devices',
      component: () => import('../views/DeviceWorkbenchView.vue'),
      meta: {
        title: '设备运维中心',
        description: '创建设备、检索状态并观察在线与最近上报情况，为远程运维与阈值管理打底。',
        requiresAuth: false
      }
    },
    {
      path: '/reporting',
      name: 'reporting',
      component: () => import('../views/ReportWorkbenchView.vue'),
      meta: {
        title: '接入回放台',
        description: '模拟设备上报、推演 topic 和 payload，并回放现有接入主链路行为。',
        requiresAuth: false
      }
    },
    {
      path: '/insight',
      name: 'insight',
      component: () => import('../views/DeviceInsightView.vue'),
      meta: {
        title: '风险点工作台',
        description: '聚合设备详情、属性、消息日志与趋势入口，作为一线风险研判和上报的核心工作页。',
        requiresAuth: false
      }
    },
    {
      path: '/file-debug',
      name: 'file-debug',
      component: () => import('../views/FilePayloadDebugView.vue'),
      meta: {
        title: '文件与固件调试',
        description: '查看 C.3 文件快照和 C.4 固件聚合结果，服务运维与研发验证文件链路。',
        requiresAuth: false
      }
    },
    {
      path: '/future-lab',
      name: 'future-lab',
      component: () => import('../views/FutureLabView.vue'),
      meta: {
        title: '未来演进蓝图',
        description: '为点位图表、数字孪生、规则引擎、告警中心和 OTA 运维预留扩展契约。',
        requiresAuth: false
      }
    },
    {
      path: '/alarm-center',
      name: 'alarm-center',
      component: () => import('../views/AlarmCenterView.vue'),
      meta: {
        title: '告警中心',
        description: '实时告警列表、告警详情、告警确认、告警抑制、通知记录。',
        requiresAuth: false
      }
    },
    {
      path: '/event-disposal',
      name: 'event-disposal',
      component: () => import('../views/EventDisposalView.vue'),
      meta: {
        title: '事件处置',
        description: '事件列表、事件详情、工单派发、现场反馈、事件复盘。',
        requiresAuth: false
      }
    },
    {
      path: '/risk-point',
      name: 'risk-point',
      component: () => import('../views/RiskPointView.vue'),
      meta: {
        title: '风险点管理',
        description: '风险点CRUD、风险点与设备绑定、风险等级配置。',
        requiresAuth: false
      }
    },
    {
      path: '/rule-definition',
      name: 'rule-definition',
      component: () => import('../views/RuleDefinitionView.vue'),
      meta: {
        title: '阈值规则配置',
        description: '规则CRUD、规则测试、测点阈值配置。',
        requiresAuth: false
      }
    },
    {
      path: '/linkage-rule',
      name: 'linkage-rule',
      component: () => import('../views/LinkageRuleView.vue'),
      meta: {
        title: '联动规则',
        description: '联动规则CRUD、触发条件配置、动作列表配置。',
        requiresAuth: false
      }
    },
    {
      path: '/emergency-plan',
      name: 'emergency-plan',
      component: () => import('../views/EmergencyPlanView.vue'),
      meta: {
        title: '应急预案',
        description: '应急预案CRUD、响应步骤配置、联系人列表配置。',
        requiresAuth: false
      }
    },
    {
      path: '/report-analysis',
      name: 'report-analysis',
      component: () => import('../views/ReportAnalysisView.vue'),
      meta: {
        title: '分析报表',
        description: '风险趋势分析、告警统计分析、事件闭环分析、设备健康分析。',
        requiresAuth: false
      }
    },
    {
      path: '/organization',
      name: 'organization',
      component: () => import('../views/OrganizationView.vue'),
      meta: {
        title: '组织机构',
        description: '组织机构管理、部门树形结构、负责人配置。',
        requiresAuth: false
      }
    },
    {
      path: '/user',
      name: 'user',
      component: () => import('../views/UserView.vue'),
      meta: {
        title: '用户管理',
        description: '用户管理、用户CRUD、密码重置。',
        requiresAuth: false
      }
    },
    {
      path: '/role',
      name: 'role',
      component: () => import('../views/RoleView.vue'),
      meta: {
        title: '角色管理',
        description: '角色管理、角色CRUD、权限配置。',
        requiresAuth: false
      }
    },
    {
      path: '/region',
      name: 'region',
      component: () => import('../views/RegionView.vue'),
      meta: {
        title: '区域管理',
        description: '区域管理、区域CRUD、区域树形结构。',
        requiresAuth: false
      }
    },
    {
      path: '/dict',
      name: 'dict',
      component: () => import('../views/DictView.vue'),
      meta: {
        title: '字典配置',
        description: '字典配置管理、字典项管理、字典类型配置。',
        requiresAuth: false
      }
    },
    {
      path: '/channel',
      name: 'channel',
      component: () => import('../views/ChannelView.vue'),
      meta: {
        title: '通知渠道',
        description: '通知渠道管理、渠道类型配置、渠道CRUD。',
        requiresAuth: false
      }
    },
    {
      path: '/audit-log',
      name: 'audit-log',
      component: () => import('../views/AuditLogView.vue'),
      meta: {
        title: '审计日志',
        description: '审计日志管理、操作记录查询、操作详情查看。',
        requiresAuth: false
      }
    }
  ],
  scrollBehavior() {
    return { top: 0 };
  }
});

// 路由守卫：权限控制
router.beforeEach((to, from, next) => {
  const permissionStore = usePermissionStore();

  // 检查是否需要权限
  if (to.meta.requiresAuth) {
    // 检查用户是否登录
    if (!permissionStore.isLoggedIn) {
      // 当前前端仍处于商业化演示阶段，未登录用户统一回到公开驾驶舱首页。
      next('/');
      return;
    }

    const requiredPermission = to.meta.permission as string;

    // 检查用户是否有访问该路由的权限
    if (requiredPermission && !permissionStore.hasPermission(requiredPermission)) {
      // 无权限时回到公开驾驶舱首页，避免出现点击后无明显反馈。
      next('/');
      return;
    }
  }

  next();
});

router.afterEach((to) => {
  document.title = `${String(to.meta.title || '调试台')} | Spring Boot IoT`;
  pushVisitedTab({
    path: to.path,
    title: String(to.meta.title || '调试台')
  });
});

export default router;
