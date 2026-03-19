import type {
  AutomationInitialApi,
  AutomationLocator,
  AutomationPageCategory,
  AutomationPageCoverageSummary,
  AutomationPageInventoryItem,
  AutomationPlanDocument,
  AutomationPlanSuggestion,
  AutomationScenarioConfig,
  AutomationScenarioPreview,
  AutomationScenarioScope,
  AutomationScenarioTemplateType,
  AutomationStep
} from '../types/automation';
import type { MenuTreeNode } from '../types/auth';

const AUTOMATION_PLAN_STORAGE_KEY = 'spring-boot-iot.automation-plan';
const AUTOMATION_PAGE_INVENTORY_STORAGE_KEY = 'spring-boot-iot.automation-page-inventory';

interface AutomationInventorySeed {
  route: string;
  title: string;
  caption: string;
  readySelector?: string;
  matcher?: string;
  requiresLogin?: boolean;
  menuCode?: string;
}

const INVENTORY_SOURCE_PRIORITY: Record<string, number> = {
  static: 1,
  menu: 2,
  manual: 3
};

const STATIC_PAGE_SEEDS: AutomationInventorySeed[] = [
  {
    route: '/login',
    title: '登录',
    caption: '统一登录入口与会话初始化页面。',
    readySelector: '#login-submit',
    requiresLogin: false
  },
  {
    route: '/',
    title: '平台首页',
    caption: '平台总览、驾驶舱和业务入口。',
    matcher: '/api/cockpit/',
    requiresLogin: false
  },
  {
    route: '/products',
    title: '产品定义中心',
    caption: '产品模型建模、协议绑定与设备归属管理。',
    readySelector: '#product-key',
    matcher: '/api/device/product/'
  },
  {
    route: '/devices',
    title: '设备资产中心',
    caption: '设备建档、在线状态核查与资产运维。',
    readySelector: '#device-product-key',
    matcher: '/api/device/'
  },
  {
    route: '/reporting',
    title: '链路验证中心',
    caption: '模拟 HTTP 上报并校验接入链路解析结果。',
    matcher: '/api/message/http/report'
  },
  {
    route: '/insight',
    title: '对象洞察台',
    caption: '聚合设备属性、日志与监测对象研判线索。',
    matcher: '/api/device/'
  },
  {
    route: '/file-debug',
    title: '数据校验台',
    caption: '文件类报文与固件分包的完整性核验能力。'
  },
  {
    route: '/system-log',
    title: '异常观测台',
    caption: '设备接入链路的系统异常定位与调试回看。',
    matcher: '/api/system/audit-log/'
  },
  {
    route: '/future-lab',
    title: '演进蓝图',
    caption: '预研能力展示与未来扩展方向说明。'
  },
  {
    route: '/alarm-center',
    title: '告警运营台',
    caption: '告警列表、详情、确认与抑制管理。',
    matcher: '/api/alarm/'
  },
  {
    route: '/event-disposal',
    title: '事件协同台',
    caption: '事件工单派发、闭环与复盘管理。',
    matcher: '/api/event/'
  },
  {
    route: '/risk-point',
    title: '风险对象中心',
    caption: '风险点 CRUD 与设备绑定维护。',
    matcher: '/api/risk-point/'
  },
  {
    route: '/rule-definition',
    title: '阈值策略',
    caption: '阈值策略设计、测试和启停管理。',
    matcher: '/api/rule-definition/'
  },
  {
    route: '/linkage-rule',
    title: '联动编排',
    caption: '联动触发条件与动作配置管理。',
    matcher: '/api/linkage-rule/'
  },
  {
    route: '/emergency-plan',
    title: '应急预案库',
    caption: '应急预案维护与联动绑定管理。',
    matcher: '/api/emergency-plan/'
  },
  {
    route: '/report-analysis',
    title: '运营分析中心',
    caption: '风险趋势、告警统计、闭环与健康分析。',
    matcher: '/api/report/'
  },
  {
    route: '/risk-monitoring',
    title: '实时监测台',
    caption: '风险监测实时列表与统一详情抽屉。',
    matcher: '/api/risk-monitoring/'
  },
  {
    route: '/risk-monitoring-gis',
    title: 'GIS态势图',
    caption: '基于 ECharts 的风险点位分布与详情联动。',
    matcher: '/api/risk-monitoring/'
  },
  {
    route: '/organization',
    title: '组织架构',
    caption: '组织树维护与层级管理。',
    matcher: '/api/organization/'
  },
  {
    route: '/user',
    title: '账号中心',
    caption: '用户档案、状态与重置密码管理。',
    matcher: '/api/user/'
  },
  {
    route: '/role',
    title: '角色权限',
    caption: '角色、菜单与权限绑定管理。',
    matcher: '/api/role/'
  },
  {
    route: '/menu',
    title: '导航编排',
    caption: '菜单树维护与页面/按钮权限项管理。',
    matcher: '/api/menu/'
  },
  {
    route: '/region',
    title: '区域版图',
    caption: '行政区域树与地域配置管理。',
    matcher: '/api/region/'
  },
  {
    route: '/dict',
    title: '数据字典',
    caption: '字典类型与字典项维护。',
    matcher: '/api/dict/'
  },
  {
    route: '/channel',
    title: '通知编排',
    caption: '通知编排配置、启停与测试管理。',
    matcher: '/api/system/channel/'
  },
  {
    route: '/automation-test',
    title: '自动化工场',
    caption: '配置驱动的浏览器自动化编排、报告与测试建议中心。'
  },
  {
    route: '/audit-log',
    title: '审计中心',
    caption: '面向业务与治理侧的关键操作审计查询。',
    matcher: '/api/system/audit-log/'
  }
];

function deepClone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function cleanString(value?: string | null, fallback = ''): string {
  return (value || '').trim() || fallback;
}

function cleanArray(values: string[] | undefined, fallback: string[] = []): string[] {
  const next = (values || []).map((item) => item.trim()).filter(Boolean);
  return next.length ? next : [...fallback];
}

export function createAutomationId(prefix = 'auto'): string {
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

function normalizeRoute(route?: string | null): string {
  const trimmed = (route || '').trim();
  if (!trimmed || trimmed === '/') {
    return '/';
  }
  return `/${trimmed.replace(/^\/+/, '').replace(/\/+$/, '')}`;
}

function slugifyRoute(route: string): string {
  const normalized = normalizeRoute(route);
  if (normalized === '/') {
    return 'home';
  }
  return normalized
    .replace(/^\//, '')
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .toLowerCase();
}

function buildFingerprint(...parts: Array<string | undefined>): string {
  return parts
    .filter(Boolean)
    .join(' ')
    .trim()
    .toLowerCase();
}

function matchKeywords(text: string, keywords: string[]): boolean {
  return keywords.some((keyword) => text.includes(keyword.toLowerCase()));
}

function uniqueKeywords(values: string[] | undefined): string[] {
  return Array.from(new Set(cleanArray(values, [])));
}

function createCssLocator(value = ''): AutomationLocator {
  return {
    type: 'css',
    value
  };
}

function createInitialApi(label = '页面首屏接口', matcher = '/api/example/list'): AutomationInitialApi {
  return {
    label,
    matcher,
    optional: true,
    timeout: 15000
  };
}

function createStep(step: Partial<AutomationStep> = {}): AutomationStep {
  return {
    id: step.id || createAutomationId('step'),
    label: cleanString(step.label, '未命名步骤'),
    type: step.type || 'waitVisible',
    locator: step.locator ? deepClone(step.locator) : undefined,
    value: step.value,
    matcher: step.matcher,
    optionText: step.optionText,
    checked: step.checked,
    filePath: step.filePath,
    rowText: step.rowText,
    dialogTitle: step.dialogTitle,
    dialogAction: step.dialogAction,
    actionText: step.actionText,
    screenshotTarget: step.screenshotTarget,
    baselineName: step.baselineName,
    threshold: step.threshold,
    fullPage: step.fullPage,
    timeout: step.timeout,
    optional: step.optional ?? false,
    action: step.action ? deepClone(step.action) : undefined,
    captures: step.captures ? deepClone(step.captures) : []
  };
}

function createDefaultTarget() {
  return {
    planName: '监测预警平台自动化冒烟基线',
    frontendBaseUrl: 'http://127.0.0.1:5174',
    backendBaseUrl: 'http://127.0.0.1:9999',
    loginRoute: '/login',
    username: 'admin',
    password: '123456',
    browserPath: '',
    headless: true,
    issueDocPath: 'docs/22-automation-test-issues-20260316.md',
    outputPrefix: 'config-browser',
    baselineDir: 'config/automation/baselines',
    scenarioScopes: ['delivery', 'baseline'],
    failScopes: ['delivery']
  };
}

function createLoginScenario(): AutomationScenarioConfig {
  return {
    key: 'login',
    name: '登录与会话初始化',
    route: '/login',
    scope: 'delivery',
    readySelector: '#login-submit',
    requiresLogin: false,
    description: '初始化浏览器登录态，作为后续页面校验的前置场景。',
    businessFlow: '登录鉴权、token 获取与会话建立',
    featurePoints: ['登录页可达', '登录接口返回 200', '进入受保护工作台'],
    initialApis: [],
    steps: [
      createStep({
        id: 'login-fill-username',
        label: '填写账号',
        type: 'fill',
        locator: createCssLocator('#login-username'),
        value: '${target.username}'
      }),
      createStep({
        id: 'login-fill-password',
        label: '填写密码',
        type: 'fill',
        locator: createCssLocator('#login-password'),
        value: '${target.password}'
      }),
      createStep({
        id: 'login-submit',
        label: '提交登录并等待响应',
        type: 'triggerApi',
        matcher: '/api/auth/login',
        action: {
          type: 'click',
          locator: createCssLocator('#login-submit')
        }
      }),
      createStep({
        id: 'login-assert-title',
        label: '断言首页标题出现',
        type: 'assertText',
        locator: createCssLocator('[data-testid="console-page-title"]'),
        value: '平台首页'
      })
    ]
  };
}

function createProductScenario(): AutomationScenarioConfig {
  return {
    key: 'product-workbench',
    name: '产品创建与查询',
    route: '/products',
    scope: 'delivery',
    readySelector: '#product-key',
    description: '示例场景：创建产品并基于返回主键查询详情。',
    businessFlow: '产品模板建模、接口回写与主键查询',
    featurePoints: ['产品创建', '返回主键捕获', '按 ID 查询产品详情'],
    initialApis: [],
    steps: [
      createStep({
        id: 'product-fill-key',
        label: '填写产品编码',
        type: 'fill',
        locator: createCssLocator('#product-key'),
        value: 'autotest-product-${runToken}'
      }),
      createStep({
        id: 'product-fill-name',
        label: '填写产品名称',
        type: 'fill',
        locator: createCssLocator('#product-name'),
        value: '自动化产品 ${runToken}'
      }),
      createStep({
        id: 'product-fill-protocol',
        label: '填写协议编码',
        type: 'fill',
        locator: createCssLocator('#protocol-code'),
        value: 'mqtt-json'
      }),
      createStep({
        id: 'product-fill-format',
        label: '填写数据格式',
        type: 'fill',
        locator: createCssLocator('#data-format'),
        value: 'JSON'
      }),
      createStep({
        id: 'product-submit',
        label: '提交产品并捕获主键',
        type: 'triggerApi',
        matcher: '/api/device/product/add',
        action: {
          type: 'press',
          locator: createCssLocator('#data-format'),
          value: 'Enter'
        },
        captures: [
          {
            variable: 'productId',
            path: 'payload.data.id'
          }
        ]
      }),
      createStep({
        id: 'product-fill-query-id',
        label: '填写产品查询主键',
        type: 'fill',
        locator: createCssLocator('#query-product-id'),
        value: '${variables.productId}'
      }),
      createStep({
        id: 'product-query',
        label: '按主键查询产品',
        type: 'triggerApi',
        matcher: '/api/device/product/${variables.productId}',
        action: {
          type: 'press',
          locator: createCssLocator('#query-product-id'),
          value: 'Enter'
        }
      }),
      createStep({
        id: 'product-assert-title',
        label: '断言当前位于产品页',
        type: 'assertText',
        locator: createCssLocator('[data-testid="console-page-title"]'),
        value: '产品定义中心'
      })
    ]
  };
}

function createDeviceScenario(): AutomationScenarioConfig {
  return {
    key: 'device-workbench',
    name: '设备资产建档与库存校验',
    route: '/devices',
    scope: 'baseline',
    readySelector: '#device-product-key',
    description: '示例场景：在设备资产列表打开抽屉建档，并通过列表筛选验证设备已成功入库。',
    businessFlow: '设备建档、列表筛选与库存校验',
    featurePoints: ['设备创建', '列表筛选', '库存校验'],
    initialApis: [],
    steps: [
      createStep({
        id: 'device-open-create-drawer',
        label: '打开新增设备抽屉',
        type: 'click',
        locator: {
          type: 'role',
          role: 'button',
          name: '新增设备',
          exact: true
        }
      }),
      createStep({
        id: 'device-select-product',
        label: '选择产品',
        type: 'selectOption',
        locator: createCssLocator('#device-form-product-key'),
        optionText: 'autotest-product-${runToken}'
      }),
      createStep({
        id: 'device-fill-name',
        label: '填写设备名称',
        type: 'fill',
        locator: createCssLocator('#device-name'),
        value: '自动化设备 ${runToken}'
      }),
      createStep({
        id: 'device-fill-code',
        label: '填写设备编码',
        type: 'fill',
        locator: createCssLocator('#device-code'),
        value: 'autotest-device-${runToken}'
      }),
      createStep({
        id: 'device-fill-secret',
        label: '填写设备密钥',
        type: 'fill',
        locator: createCssLocator('#device-secret'),
        value: '123456'
      }),
      createStep({
        id: 'device-fill-client',
        label: '填写 ClientId',
        type: 'fill',
        locator: createCssLocator('#client-id'),
        value: 'autotest-device-${runToken}'
      }),
      createStep({
        id: 'device-fill-username',
        label: '填写设备用户名',
        type: 'fill',
        locator: createCssLocator('#username'),
        value: 'autotest-device-${runToken}'
      }),
      createStep({
        id: 'device-fill-password',
        label: '填写设备密码',
        type: 'fill',
        locator: createCssLocator('#password'),
        value: '123456'
      }),
      createStep({
        id: 'device-submit',
        label: '提交设备并捕获主键',
        type: 'triggerApi',
        matcher: '/api/device/add',
        action: {
          type: 'click',
          locator: {
            type: 'role',
            role: 'button',
            name: '提交设备建档',
            exact: true
          }
        },
        captures: [
          {
            variable: 'deviceId',
            path: 'payload.data.id'
          }
        ]
      }),
      createStep({
        id: 'device-fill-query-id',
        label: '填写设备主键筛选',
        type: 'fill',
        locator: createCssLocator('#query-device-id'),
        value: '${variables.deviceId}'
      }),
      createStep({
        id: 'device-query-by-id',
        label: '按 ID 查询设备',
        type: 'triggerApi',
        matcher: '/api/device/page?deviceId=${variables.deviceId}',
        action: {
          type: 'click',
          locator: {
            type: 'role',
            role: 'button',
            name: '查询',
            exact: true
          }
        }
      }),
      createStep({
        id: 'device-clear-query-id',
        label: '清空设备主键筛选',
        type: 'fill',
        locator: createCssLocator('#query-device-id'),
        value: ''
      }),
      createStep({
        id: 'device-fill-query-code',
        label: '填写设备编码筛选',
        type: 'fill',
        locator: createCssLocator('#query-device-code'),
        value: 'autotest-device-${runToken}'
      }),
      createStep({
        id: 'device-query-by-code',
        label: '按编码查询设备',
        type: 'triggerApi',
        matcher: '/api/device/page?deviceCode=autotest-device-${runToken}',
        action: {
          type: 'click',
          locator: {
            type: 'role',
            role: 'button',
            name: '查询',
            exact: true
          }
        }
      }),
      createStep({
        id: 'device-assert-code',
        label: '断言列表出现设备编码',
        type: 'assertText',
        locator: createCssLocator('body'),
        value: 'autotest-device-${runToken}'
      }),
      createStep({
        id: 'device-assert-title',
        label: '断言当前位于设备页',
        type: 'assertText',
        locator: createCssLocator('[data-testid="console-page-title"]'),
        value: '设备资产中心'
      })
    ]
  };
}

export function createDefaultAutomationPlan(): AutomationPlanDocument {
  return {
    version: '1.0.0',
    createdAt: new Date().toISOString(),
    target: createDefaultTarget(),
    tags: ['playwright', 'config-driven', 'web-smoke'],
    plugins: ['builtin-report', 'builtin-suggestion', 'capture-response'],
    scenarios: [createLoginScenario(), createProductScenario(), createDeviceScenario()]
  };
}

function normalizeStep(step: Partial<AutomationStep>, index: number): AutomationStep {
  const next = createStep(step);
  next.label = cleanString(next.label, `步骤 ${index + 1}`);
  return next;
}

function normalizeInitialApi(api: Partial<AutomationInitialApi>, index: number): AutomationInitialApi {
  return {
    label: cleanString(api.label, `首屏接口 ${index + 1}`),
    matcher: cleanString(api.matcher, '/api/example/list'),
    optional: api.optional ?? true,
    timeout: api.timeout ?? 15000
  };
}

function normalizeScenario(scenario: Partial<AutomationScenarioConfig>, index: number): AutomationScenarioConfig {
  const base = createPageSmokeScenario();
  return {
    key: cleanString(scenario.key, `scenario-${index + 1}`),
    name: cleanString(scenario.name, `页面场景 ${index + 1}`),
    route: cleanString(scenario.route, base.route),
    expectedPath: cleanString(scenario.expectedPath, '') || undefined,
    scope: cleanString(scenario.scope as string, base.scope),
    readySelector: cleanString(scenario.readySelector, ''),
    requiresLogin: scenario.requiresLogin ?? false,
    description: cleanString(scenario.description, ''),
    businessFlow: cleanString(scenario.businessFlow, ''),
    featurePoints: cleanArray(scenario.featurePoints, []),
    initialApis: Array.isArray(scenario.initialApis)
      ? scenario.initialApis.map((item: Partial<AutomationInitialApi>, apiIndex: number) => normalizeInitialApi(item, apiIndex))
      : [],
    steps: Array.isArray(scenario.steps) && scenario.steps.length > 0
      ? scenario.steps.map((item: Partial<AutomationStep>, stepIndex: number) => normalizeStep(item, stepIndex))
      : [createStep({ label: '等待页面主体', type: 'waitVisible', locator: createCssLocator('body') })]
  };
}

export function normalizeAutomationPlan(
  input?: Partial<AutomationPlanDocument> | null
): AutomationPlanDocument {
  const fallback = createDefaultAutomationPlan();
  if (!input) {
    return fallback;
  }

  const normalizedTarget = {
    ...fallback.target,
    ...(input.target || {})
  };
  normalizedTarget.planName = cleanString(normalizedTarget.planName, fallback.target.planName);
  normalizedTarget.frontendBaseUrl = cleanString(normalizedTarget.frontendBaseUrl, fallback.target.frontendBaseUrl);
  normalizedTarget.backendBaseUrl = cleanString(normalizedTarget.backendBaseUrl, fallback.target.backendBaseUrl);
  normalizedTarget.loginRoute = cleanString(normalizedTarget.loginRoute, fallback.target.loginRoute);
  normalizedTarget.username = cleanString(normalizedTarget.username, fallback.target.username);
  normalizedTarget.password = cleanString(normalizedTarget.password, fallback.target.password);
  normalizedTarget.issueDocPath = cleanString(normalizedTarget.issueDocPath, fallback.target.issueDocPath);
  normalizedTarget.outputPrefix = cleanString(normalizedTarget.outputPrefix, fallback.target.outputPrefix);
  normalizedTarget.baselineDir = cleanString(normalizedTarget.baselineDir, fallback.target.baselineDir);
  normalizedTarget.scenarioScopes = cleanArray(normalizedTarget.scenarioScopes, fallback.target.scenarioScopes);
  normalizedTarget.failScopes = cleanArray(normalizedTarget.failScopes, fallback.target.failScopes);
  normalizedTarget.browserPath = cleanString(normalizedTarget.browserPath, '');

  return {
    version: cleanString(input.version, fallback.version),
    createdAt: cleanString(input.createdAt, fallback.createdAt),
    target: normalizedTarget,
    tags: cleanArray(input.tags, fallback.tags),
    plugins: cleanArray(input.plugins, fallback.plugins),
    scenarios: Array.isArray(input.scenarios) && input.scenarios.length > 0
      ? input.scenarios.map((scenario: Partial<AutomationScenarioConfig>, index: number) => normalizeScenario(scenario, index))
      : fallback.scenarios
  };
}

export function cloneAutomationPlan(plan: AutomationPlanDocument): AutomationPlanDocument {
  return deepClone(plan);
}

export function createPageSmokeScenario(): AutomationScenarioConfig {
  return {
    key: `page-smoke-${createAutomationId('scenario')}`,
    name: '页面冒烟模板',
    route: '/replace-me',
    scope: 'baseline',
    readySelector: '#app',
    requiresLogin: false,
    description: '适用于任意 Web 页面快速接入的基础巡检模板。',
    businessFlow: '页面可达、首屏接口稳定、关键文案存在',
    featurePoints: ['页面可打开', '关键 DOM 可见', '首屏接口返回正常'],
    initialApis: [createInitialApi()],
    steps: [
      createStep({
        label: '等待页面根节点',
        type: 'waitVisible',
        locator: createCssLocator('body')
      }),
      createStep({
        label: '断言页面关键文案',
        type: 'assertText',
        locator: createCssLocator('body'),
        value: '请替换为页面关键文案'
      })
    ]
  };
}

export function createFormSubmitScenario(): AutomationScenarioConfig {
  return {
    key: `form-submit-${createAutomationId('scenario')}`,
    name: '表单提交模板',
    route: '/replace-form-page',
    scope: 'delivery',
    readySelector: '#replace-form-id',
    requiresLogin: false,
    description: '适用于新增/编辑表单类页面的快速建模。',
    businessFlow: '表单填写、提交、接口校验、结果断言',
    featurePoints: ['表单填写', '提交响应成功', '结果可回显'],
    initialApis: [],
    steps: [
      createStep({
        label: '填写主输入框',
        type: 'fill',
        locator: createCssLocator('#replace-form-id'),
        value: 'autotest-${runToken}'
      }),
      createStep({
        label: '点击提交并等待接口',
        type: 'triggerApi',
        matcher: '/api/replace/add',
        action: {
          type: 'click',
          locator: {
            type: 'role',
            role: 'button',
            name: '确定',
            exact: true
          }
        }
      }),
      createStep({
        label: '断言结果提示',
        type: 'assertText',
        locator: createCssLocator('body'),
        value: '成功'
      })
    ]
  };
}

export function createListDetailScenario(): AutomationScenarioConfig {
  return {
    key: `list-detail-${createAutomationId('scenario')}`,
    name: '列表详情模板',
    route: '/replace-list-page',
    scope: 'baseline',
    readySelector: '.el-table',
    requiresLogin: false,
    description: '适用于分页列表与详情抽屉/弹窗校验。',
    businessFlow: '列表加载、筛选查询、详情打开与回退',
    featurePoints: ['列表加载', '查询条件生效', '详情可打开'],
    initialApis: [createInitialApi('列表接口', '/api/replace/list')],
    steps: [
      createStep({
        label: '等待列表可见',
        type: 'waitVisible',
        locator: createCssLocator('.el-table')
      }),
      createStep({
        label: '点击首条详情按钮并等待接口',
        type: 'triggerApi',
        matcher: '/api/replace/',
        action: {
          type: 'click',
          locator: createCssLocator('button:has-text("详情")')
        }
      }),
      createStep({
        label: '断言详情弹窗内容',
        type: 'assertText',
        locator: createCssLocator('.el-dialog'),
        value: '详情'
      })
    ]
  };
}

export function duplicateScenario(scenario: AutomationScenarioConfig): AutomationScenarioConfig {
  const copy = deepClone(scenario);
  copy.key = `${copy.key}-copy-${Math.random().toString(36).slice(2, 6)}`;
  copy.name = `${copy.name}（副本）`;
  copy.steps = copy.steps.map((step: AutomationStep) => ({
    ...step,
    id: createAutomationId('step')
  }));
  return copy;
}

export function buildScenarioPreviews(plan: AutomationPlanDocument): AutomationScenarioPreview[] {
  return plan.scenarios.map((scenario) => ({
    key: scenario.key,
    name: scenario.name,
    route: scenario.route,
    scope: String(scenario.scope || ''),
    stepCount: scenario.steps.length,
    apiCount:
      scenario.initialApis.length + scenario.steps.filter((step: AutomationStep) => step.type === 'triggerApi').length,
    featureCount: scenario.featurePoints.length,
    hasAssertion: scenario.steps.some((step: AutomationStep) =>
      ['assertText', 'assertUrlIncludes', 'assertScreenshot'].includes(step.type)
    )
  }));
}

export function buildPlanSuggestions(plan: AutomationPlanDocument): AutomationPlanSuggestion[] {
  const suggestions: AutomationPlanSuggestion[] = [];
  const previews = buildScenarioPreviews(plan);

  const loginScenarioExists = plan.scenarios.some(
    (scenario) => scenario.key === 'login' || scenario.route === plan.target.loginRoute
  );
  if (!loginScenarioExists) {
    suggestions.push({
      level: 'warning',
      title: '缺少登录前置',
      detail: '建议至少保留一个登录或会话初始化场景，避免后续页面因鉴权跳转导致批量误判。'
    });
  }

  const noReadySelector = plan.scenarios.filter((scenario) => !cleanString(scenario.readySelector));
  if (noReadySelector.length > 0) {
    suggestions.push({
      level: 'warning',
      title: '页面就绪条件偏弱',
      detail: `以下场景建议补齐 readySelector：${noReadySelector.map((item: AutomationScenarioConfig) => item.name).join('、')}。`
    });
  }

  const noAssertions = previews.filter((item) => !item.hasAssertion);
  if (noAssertions.length > 0) {
    suggestions.push({
      level: 'warning',
      title: '断言覆盖不足',
      detail: `以下场景只有流程操作没有页面断言：${noAssertions.map((item: AutomationScenarioPreview) => item.name).join('、')}。`
    });
  }

  const apiWeakScenarios = previews.filter((item) => item.apiCount === 0);
  if (apiWeakScenarios.length > 0) {
    suggestions.push({
      level: 'info',
      title: '接口证据可继续补强',
      detail: `以下场景尚未声明任何首屏或触发接口：${apiWeakScenarios.map((item: AutomationScenarioPreview) => item.name).join('、')}。`
    });
  }

  const baselineVisualWeakScenarios = plan.scenarios.filter(
    (scenario) =>
      scenario.scope === 'baseline' &&
      !scenario.steps.some((step: AutomationStep) => step.type === 'assertScreenshot')
  );
  if (baselineVisualWeakScenarios.length > 0) {
    suggestions.push({
      level: 'info',
      title: '视觉回归可继续补强',
      detail: `以下 baseline 场景建议补充截图断言：${baselineVisualWeakScenarios
        .map((item: AutomationScenarioConfig) => item.name)
        .join('、')}。`
    });
  }

  if (plan.target.scenarioScopes.length < 2) {
    suggestions.push({
      level: 'info',
      title: '建议区分交付与基线场景',
      detail: '可以把高价值主链路放在 delivery，把页面稳定性巡检放在 baseline，便于控制退出码与阻断范围。'
    });
  }

  if (suggestions.length === 0) {
    suggestions.push({
      level: 'success',
      title: '计划结构完整',
      detail: '当前计划已经具备会话前置、接口校验与页面断言，可直接导出为 CLI 执行计划继续扩面。'
    });
  }

  return suggestions;
}

function inferPageCategory(route: string, title: string, caption: string): AutomationPageCategory {
  const fingerprint = buildFingerprint(route, title, caption);

  if (normalizeRoute(route) === '/login' || matchKeywords(fingerprint, ['登录', 'login'])) {
    return 'login';
  }
  if (normalizeRoute(route) === '/' || matchKeywords(fingerprint, ['首页', 'cockpit', '总览', '驾驶舱'])) {
    return 'dashboard';
  }
  if (matchKeywords(fingerprint, ['监测', '监控', 'gis', '地图'])) {
    return 'monitoring';
  }
  if (matchKeywords(fingerprint, ['报表', '分析', 'trend', '统计'])) {
    return 'analysis';
  }
  if (matchKeywords(fingerprint, ['组织', '用户', '角色', '菜单', '区域', '字典', '渠道', '日志', '审计'])) {
    return 'system';
  }
  if (matchKeywords(fingerprint, ['表单', '新增', '创建', '设备', '产品'])) {
    return 'form';
  }
  if (matchKeywords(fingerprint, ['列表', '管理', '工作台', '中心', '处置', '预案', '规则', '详情'])) {
    return 'list';
  }
  return 'workspace';
}

function inferRecommendedTemplate(
  category: AutomationPageCategory,
  route: string
): AutomationScenarioTemplateType {
  if (category === 'login' || normalizeRoute(route) === '/login') {
    return 'login';
  }
  if (category === 'form') {
    return 'formSubmit';
  }
  if (category === 'list' || category === 'system' || category === 'monitoring') {
    return 'listDetail';
  }
  return 'pageSmoke';
}

function inferScenarioScope(route: string, title: string): AutomationScenarioScope {
  const normalizedRoute = normalizeRoute(route);
  const fingerprint = buildFingerprint(route, title);
  if (
    normalizedRoute === '/login' ||
    normalizedRoute === '/' ||
    normalizedRoute === '/products' ||
    normalizedRoute === '/devices' ||
    normalizedRoute === '/reporting' ||
    matchKeywords(fingerprint, ['告警', '事件'])
  ) {
    return 'delivery';
  }
  return 'baseline';
}

function inferReadySelector(route: string, title: string): string {
  const normalizedRoute = normalizeRoute(route);
  if (normalizedRoute === '/login') {
    return '#login-submit';
  }
  if (normalizedRoute === '/products') {
    return '#product-key';
  }
  if (normalizedRoute === '/devices') {
    return '#device-product-key';
  }
  if (matchKeywords(buildFingerprint(route, title), ['列表', '管理'])) {
    return '.el-table, [data-testid="console-page-title"]';
  }
  return '[data-testid="console-page-title"]';
}

function inferApiMatcher(route: string): string {
  const normalizedRoute = normalizeRoute(route);
  const routeMatcherMap: Record<string, string> = {
    '/': '/api/cockpit/',
    '/products': '/api/device/product/',
    '/devices': '/api/device/',
    '/reporting': '/api/message/http/report',
    '/system-log': '/api/system/audit-log/',
    '/alarm-center': '/api/alarm/',
    '/event-disposal': '/api/event/',
    '/risk-point': '/api/risk-point/',
    '/rule-definition': '/api/rule-definition/',
    '/linkage-rule': '/api/linkage-rule/',
    '/emergency-plan': '/api/emergency-plan/',
    '/report-analysis': '/api/report/',
    '/risk-monitoring': '/api/risk-monitoring/',
    '/risk-monitoring-gis': '/api/risk-monitoring/',
    '/organization': '/api/organization/',
    '/user': '/api/user/',
    '/role': '/api/role/',
    '/menu': '/api/menu/',
    '/region': '/api/region/',
    '/dict': '/api/dict/',
    '/channel': '/api/system/channel/',
    '/audit-log': '/api/system/audit-log/'
  };
  return routeMatcherMap[normalizedRoute] || '';
}

function inferInventoryKeywords(
  route: string,
  title: string,
  caption: string,
  category: AutomationPageCategory
): string[] {
  const keywords = [route, title, category];

  if (caption) {
    caption
      .split(/[，。、,\s]+/)
      .map((item) => item.trim())
      .filter((item) => item.length >= 2 && item.length <= 8)
      .slice(0, 4)
      .forEach((item) => keywords.push(item));
  }

  return uniqueKeywords(keywords.filter(Boolean));
}

function normalizeInventoryItem(
  input: Partial<AutomationPageInventoryItem> & Pick<AutomationPageInventoryItem, 'route' | 'title'>,
  sourceFallback: AutomationPageInventoryItem['source'] = 'manual'
): AutomationPageInventoryItem {
  const route = normalizeRoute(input.route);
  const title = cleanString(input.title, route === '/' ? '平台首页' : route);
  const caption = cleanString(input.caption, `${title} 页面盘点项`);
  const category = input.category || inferPageCategory(route, title, caption);
  const recommendedTemplate =
    input.recommendedTemplate || inferRecommendedTemplate(category, route);

  return {
    id: cleanString(input.id, `inventory-${slugifyRoute(route) || createAutomationId('page')}`),
    route,
    title,
    caption,
    menuCode: cleanString(input.menuCode, '') || undefined,
    source: input.source || sourceFallback,
    category,
    recommendedTemplate,
    scope: (input.scope || inferScenarioScope(route, title)) as AutomationScenarioScope,
    requiresLogin: input.requiresLogin ?? (route !== '/login' && route !== '/'),
    readySelector: cleanString(input.readySelector, inferReadySelector(route, title)),
    matcher: cleanString(input.matcher, inferApiMatcher(route)) || undefined,
    keywords: uniqueKeywords(
      input.keywords && input.keywords.length > 0
        ? input.keywords
        : inferInventoryKeywords(route, title, caption, category)
    )
  };
}

function mergeInventoryPair(
  current: AutomationPageInventoryItem | undefined,
  next: AutomationPageInventoryItem
): AutomationPageInventoryItem {
  if (!current) {
    return next;
  }

  const currentPriority = INVENTORY_SOURCE_PRIORITY[current.source] || 0;
  const nextPriority = INVENTORY_SOURCE_PRIORITY[next.source] || 0;
  const preferred = nextPriority >= currentPriority ? next : current;
  const secondary = preferred === next ? current : next;

  return {
    ...secondary,
    ...preferred,
    id: preferred.id || secondary.id,
    caption: cleanString(preferred.caption, secondary.caption),
    readySelector: cleanString(preferred.readySelector, secondary.readySelector),
    matcher: cleanString(preferred.matcher, secondary.matcher) || undefined,
    keywords: uniqueKeywords([...(secondary.keywords || []), ...(preferred.keywords || [])])
  };
}

export function buildStaticPageInventory(): AutomationPageInventoryItem[] {
  return STATIC_PAGE_SEEDS.map((seed) =>
    normalizeInventoryItem(
      {
        id: `inventory-${slugifyRoute(seed.route)}`,
        route: seed.route,
        title: seed.title,
        caption: seed.caption,
        readySelector: seed.readySelector,
        matcher: seed.matcher,
        requiresLogin: seed.requiresLogin,
        menuCode: seed.menuCode,
        source: 'static'
      },
      'static'
    )
  );
}

export function buildMenuPageInventory(menus: MenuTreeNode[]): AutomationPageInventoryItem[] {
  const items: AutomationPageInventoryItem[] = [];

  const visit = (nodes: MenuTreeNode[]) => {
    nodes.forEach((node) => {
      if (node.type !== 2 && node.path) {
        items.push(
          normalizeInventoryItem(
            {
              id: `menu-${node.id}`,
              route: node.path,
              title: node.menuName || node.path,
              caption: node.meta?.caption || node.meta?.description || `${node.menuName || node.path} 页面`,
              menuCode: node.menuCode,
              source: 'menu'
            },
            'menu'
          )
        );
      }

      if (node.children?.length) {
        visit(node.children);
      }
    });
  };

  visit(menus || []);
  return mergeAutomationPageInventory(items);
}

export function mergeAutomationPageInventory(
  ...groups: AutomationPageInventoryItem[][]
): AutomationPageInventoryItem[] {
  const routeMap = new Map<string, AutomationPageInventoryItem>();

  groups.flat().forEach((item) => {
    if (!item?.route) {
      return;
    }
    const normalized = normalizeInventoryItem(item, item.source || 'manual');
    const key = normalizeRoute(normalized.route);
    routeMap.set(key, mergeInventoryPair(routeMap.get(key), normalized));
  });

  return Array.from(routeMap.values()).sort((left, right) => {
    const leftRoute = normalizeRoute(left.route);
    const rightRoute = normalizeRoute(right.route);
    if (leftRoute === '/login') {
      return -1;
    }
    if (rightRoute === '/login') {
      return 1;
    }
    if (leftRoute === '/') {
      return -1;
    }
    if (rightRoute === '/') {
      return 1;
    }
    return leftRoute.localeCompare(rightRoute, 'zh-Hans-CN');
  });
}

export function buildAutomationPageInventory(options: {
  menus?: MenuTreeNode[];
  manualPages?: AutomationPageInventoryItem[];
  includeStaticFallback?: boolean;
} = {}): AutomationPageInventoryItem[] {
  const menuInventory = options.menus?.length ? buildMenuPageInventory(options.menus) : [];
  const staticInventory =
    options.includeStaticFallback || menuInventory.length === 0 ? buildStaticPageInventory() : [];
  const manualInventory = (options.manualPages || []).map((item) =>
    normalizeInventoryItem(item, 'manual')
  );

  return mergeAutomationPageInventory(staticInventory, menuInventory, manualInventory);
}

function buildInventoryFeaturePoints(item: AutomationPageInventoryItem): string[] {
  switch (item.category) {
    case 'dashboard':
      return ['页面可达', '首屏看板稳定', '关键总览文案可见'];
    case 'login':
      return ['登录页可达', '登录动作可补充', '会话初始化成功'];
    case 'monitoring':
      return ['监测页面可达', '首屏数据正常', '关键态势信息可见'];
    case 'analysis':
      return ['报表页面可达', '统计区域可见', '首屏接口返回正常'];
    case 'system':
      return ['页面可达', '首屏列表稳定', '基础治理入口可用'];
    case 'form':
      return ['页面可达', '录入入口可补充', '提交流程待细化'];
    default:
      return ['页面可达', '首屏加载稳定', '关键文案可见'];
  }
}

function buildInventoryBusinessFlow(item: AutomationPageInventoryItem): string {
  switch (item.category) {
    case 'dashboard':
      return '打开页面、等待首屏接口、核对看板标题与关键卡片';
    case 'login':
      return '访问登录页、输入凭据、建立会话与进入受保护页面';
    case 'monitoring':
      return '进入监测页面、等待列表或地图加载、核对关键数据呈现';
    case 'analysis':
      return '进入报表页面、等待统计接口返回、核对图表或指标区';
    case 'system':
      return '进入治理页面、等待首屏列表加载、补充查询与详情动作';
    case 'form':
      return '进入业务页面、补充新增/编辑表单、核对提交结果';
    default:
      return '进入页面、等待主容器加载、核对路径与关键文案';
  }
}

export function createScenarioFromInventory(
  item: AutomationPageInventoryItem
): AutomationScenarioConfig {
  const normalizedItem = normalizeInventoryItem(item, item.source || 'manual');
  const normalizedRoute = normalizeRoute(normalizedItem.route);

  if (normalizedItem.recommendedTemplate === 'login' && normalizedRoute === '/login') {
    const loginScenario = createLoginScenario();
    return {
      ...loginScenario,
      name: `${normalizedItem.title}自动脚手架`,
      route: normalizedRoute,
      readySelector: normalizedItem.readySelector,
      requiresLogin: normalizedItem.requiresLogin,
      description: `基于${normalizedItem.source}页面盘点自动生成，建议继续补充真实登录选择器与断言。`,
      businessFlow: buildInventoryBusinessFlow(normalizedItem),
      featurePoints: buildInventoryFeaturePoints(normalizedItem)
    };
  }

  const steps: AutomationStep[] = [
    createStep({
      label: '等待页面主区域可见',
      type: 'waitVisible',
      locator: createCssLocator(normalizedItem.readySelector || 'body')
    })
  ];

  if (normalizedRoute !== '/') {
    steps.push(
      createStep({
        label: '断言页面路径',
        type: 'assertUrlIncludes',
        value: normalizedRoute
      })
    );
  }

  steps.push(
    createStep({
      label: '断言页面关键标题',
      type: 'assertText',
      locator: createCssLocator('body'),
      value: normalizedItem.title
    })
  );

  return {
    key: `inventory-${slugifyRoute(normalizedRoute)}`,
    name: `${normalizedItem.title}自动脚手架`,
    route: normalizedRoute,
    expectedPath: normalizedRoute === '/' ? undefined : normalizedRoute,
    scope: normalizedItem.scope,
    readySelector: normalizedItem.readySelector,
    requiresLogin: normalizedItem.requiresLogin,
    description: `基于${normalizedItem.source}页面盘点自动生成的冒烟脚手架，建议继续补充真实交互步骤、接口断言与变量捕获。`,
    businessFlow: buildInventoryBusinessFlow(normalizedItem),
    featurePoints: buildInventoryFeaturePoints(normalizedItem),
    initialApis: normalizedItem.matcher
      ? [createInitialApi('页面首屏接口', normalizedItem.matcher)]
      : [],
    steps
  };
}

export function collectScenarioRoutes(plan: AutomationPlanDocument): string[] {
  return Array.from(
    new Set(
      (plan.scenarios || [])
        .map((scenario) => normalizeRoute(scenario.route))
        .filter(Boolean)
    )
  );
}

export function buildPageCoverageSummary(
  plan: AutomationPlanDocument,
  inventory: AutomationPageInventoryItem[]
): AutomationPageCoverageSummary {
  const coveredRoutes = new Set(collectScenarioRoutes(plan));
  const uncoveredRoutes = inventory
    .map((item) => normalizeRoute(item.route))
    .filter((route) => !coveredRoutes.has(route));

  return {
    totalPages: inventory.length,
    coveredPages: inventory.length - uncoveredRoutes.length,
    uncoveredPages: uncoveredRoutes.length,
    uncoveredRoutes
  };
}

export function createManualInventoryItem(
  input: Partial<AutomationPageInventoryItem> = {}
): AutomationPageInventoryItem {
  return normalizeInventoryItem(
    {
      id: input.id || createAutomationId('manual-page'),
      route: input.route || '/external-page',
      title: input.title || '外部页面',
      caption: input.caption || '手工维护的页面盘点项。',
      source: 'manual',
      category: input.category || 'custom',
      recommendedTemplate: input.recommendedTemplate || 'pageSmoke',
      scope: input.scope || 'baseline',
      requiresLogin: input.requiresLogin ?? true,
      readySelector: input.readySelector || '[data-testid="console-page-title"]',
      matcher: input.matcher,
      menuCode: input.menuCode,
      keywords: input.keywords || []
    },
    'manual'
  );
}

export function buildAutomationCommand(planPath = 'config/automation/sample-web-smoke-plan.json'): string {
  return `node scripts/auto/run-browser-acceptance.mjs --plan=${planPath}`;
}

export function loadSavedAutomationPlan(): AutomationPlanDocument {
  if (typeof window === 'undefined') {
    return createDefaultAutomationPlan();
  }

  const raw = window.localStorage.getItem(AUTOMATION_PLAN_STORAGE_KEY);
  if (!raw) {
    return createDefaultAutomationPlan();
  }

  try {
    return normalizeAutomationPlan(JSON.parse(raw) as AutomationPlanDocument);
  } catch {
    window.localStorage.removeItem(AUTOMATION_PLAN_STORAGE_KEY);
    return createDefaultAutomationPlan();
  }
}

export function saveAutomationPlan(plan: AutomationPlanDocument): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(AUTOMATION_PLAN_STORAGE_KEY, JSON.stringify(normalizeAutomationPlan(plan)));
}

export function loadSavedAutomationInventory(): AutomationPageInventoryItem[] {
  if (typeof window === 'undefined') {
    return [];
  }

  const raw = window.localStorage.getItem(AUTOMATION_PAGE_INVENTORY_STORAGE_KEY);
  if (!raw) {
    return [];
  }

  try {
    const parsed = JSON.parse(raw) as AutomationPageInventoryItem[];
    return Array.isArray(parsed) ? parsed.map((item) => normalizeInventoryItem(item, 'manual')) : [];
  } catch {
    window.localStorage.removeItem(AUTOMATION_PAGE_INVENTORY_STORAGE_KEY);
    return [];
  }
}

export function saveAutomationInventory(items: AutomationPageInventoryItem[]): void {
  if (typeof window === 'undefined') {
    return;
  }
  const normalized = items.map((item) => normalizeInventoryItem(item, 'manual'));
  window.localStorage.setItem(AUTOMATION_PAGE_INVENTORY_STORAGE_KEY, JSON.stringify(normalized));
}

