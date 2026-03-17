import type {
  AutomationInitialApi,
  AutomationLocator,
  AutomationPlanDocument,
  AutomationPlanSuggestion,
  AutomationScenarioConfig,
  AutomationScenarioPreview,
  AutomationStep
} from '../types/automation';

const AUTOMATION_PLAN_STORAGE_KEY = 'spring-boot-iot.automation-plan';

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
        value: '产品模板中心'
      })
    ]
  };
}

function createDeviceScenario(): AutomationScenarioConfig {
  return {
    key: 'device-workbench',
    name: '设备建档与查询',
    route: '/devices',
    scope: 'baseline',
    readySelector: '#device-product-key',
    description: '示例场景：基于产品编码建设设备档案并执行详情查询。',
    businessFlow: '设备建档、编码检索与详情验证',
    featurePoints: ['设备创建', '按 ID 查询', '按编码查询'],
    initialApis: [],
    steps: [
      createStep({
        id: 'device-fill-product',
        label: '填写产品编码',
        type: 'fill',
        locator: createCssLocator('#device-product-key'),
        value: 'autotest-product-${runToken}'
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
        label: '填写设备主键',
        type: 'fill',
        locator: createCssLocator('#query-device-id'),
        value: '${variables.deviceId}'
      }),
      createStep({
        id: 'device-query-by-id',
        label: '按 ID 查询设备',
        type: 'triggerApi',
        matcher: '/api/device/${variables.deviceId}',
        action: {
          type: 'click',
          locator: {
            type: 'role',
            role: 'button',
            name: '按 ID 查询',
            exact: true
          }
        }
      }),
      createStep({
        id: 'device-fill-query-code',
        label: '填写设备编码',
        type: 'fill',
        locator: createCssLocator('#query-device-code'),
        value: 'autotest-device-${runToken}'
      }),
      createStep({
        id: 'device-query-by-code',
        label: '按编码查询设备',
        type: 'triggerApi',
        matcher: '/api/device/code/autotest-device-${runToken}',
        action: {
          type: 'click',
          locator: {
            type: 'role',
            role: 'button',
            name: '按编码查询',
            exact: true
          }
        }
      }),
      createStep({
        id: 'device-assert-title',
        label: '断言当前位于设备页',
        type: 'assertText',
        locator: createCssLocator('[data-testid="console-page-title"]'),
        value: '设备运维中心'
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
    hasAssertion: scenario.steps.some((step: AutomationStep) => step.type === 'assertText' || step.type === 'assertUrlIncludes')
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
