export type AutomationScenarioScope = 'delivery' | 'baseline' | 'regression' | 'smoke' | string;

export type AutomationLocatorType = 'css' | 'placeholder' | 'role' | 'text' | 'label' | 'testId';

export type AutomationStepType =
  | 'fill'
  | 'click'
  | 'press'
  | 'setChecked'
  | 'selectOption'
  | 'uploadFile'
  | 'waitVisible'
  | 'triggerApi'
  | 'tableRowAction'
  | 'dialogAction'
  | 'assertText'
  | 'assertUrlIncludes'
  | 'assertScreenshot'
  | 'sleep'
  | string;

export interface AutomationLocator {
  type: AutomationLocatorType;
  value?: string;
  role?: string;
  name?: string;
  exact?: boolean;
  container?: string;
}

export interface AutomationApiCapture {
  variable: string;
  path: string;
}

export interface AutomationNestedAction {
  type: 'click' | 'press';
  locator: AutomationLocator;
  value?: string;
}

export interface AutomationInitialApi {
  label: string;
  matcher: string;
  optional?: boolean;
  timeout?: number;
}

export interface AutomationStep {
  id: string;
  label: string;
  type: AutomationStepType;
  locator?: AutomationLocator;
  value?: string;
  matcher?: string;
  optionText?: string;
  checked?: boolean;
  filePath?: string;
  rowText?: string;
  dialogTitle?: string;
  dialogAction?: 'waitVisible' | 'confirm' | 'cancel' | 'close' | 'custom';
  actionText?: string;
  screenshotTarget?: 'page' | 'locator';
  baselineName?: string;
  threshold?: number;
  fullPage?: boolean;
  timeout?: number;
  optional?: boolean;
  action?: AutomationNestedAction;
  captures?: AutomationApiCapture[];
}

export interface AutomationScenarioConfig {
  key: string;
  name: string;
  route: string;
  expectedPath?: string;
  scope: AutomationScenarioScope;
  readySelector?: string;
  requiresLogin?: boolean;
  description?: string;
  businessFlow?: string;
  featurePoints: string[];
  initialApis: AutomationInitialApi[];
  steps: AutomationStep[];
}

export interface AutomationTargetConfig {
  planName: string;
  frontendBaseUrl: string;
  backendBaseUrl: string;
  loginRoute: string;
  username: string;
  password: string;
  browserPath?: string;
  headless: boolean;
  issueDocPath: string;
  outputPrefix: string;
  baselineDir: string;
  scenarioScopes: string[];
  failScopes: string[];
}

export interface AutomationPlanDocument {
  version: string;
  createdAt: string;
  target: AutomationTargetConfig;
  tags: string[];
  plugins: string[];
  scenarios: AutomationScenarioConfig[];
}

export interface AutomationPlanSuggestion {
  level: 'success' | 'info' | 'warning';
  title: string;
  detail: string;
}

export interface AutomationScenarioPreview {
  key: string;
  name: string;
  route: string;
  scope: string;
  stepCount: number;
  apiCount: number;
  featureCount: number;
  hasAssertion: boolean;
}

export type AutomationScenarioTemplateType = 'pageSmoke' | 'formSubmit' | 'listDetail' | 'login';

export type AutomationPageDiscoverySource = 'menu' | 'static' | 'manual';

export type AutomationPageCategory =
  | 'login'
  | 'dashboard'
  | 'workspace'
  | 'list'
  | 'form'
  | 'analysis'
  | 'monitoring'
  | 'system'
  | 'custom';

export interface AutomationPageInventoryItem {
  id: string;
  route: string;
  title: string;
  caption: string;
  menuCode?: string;
  source: AutomationPageDiscoverySource;
  category: AutomationPageCategory;
  recommendedTemplate: AutomationScenarioTemplateType;
  scope: AutomationScenarioScope;
  requiresLogin: boolean;
  readySelector: string;
  matcher?: string;
  keywords: string[];
}

export interface AutomationPageCoverageSummary {
  totalPages: number;
  coveredPages: number;
  uncoveredPages: number;
  uncoveredRoutes: string[];
}
