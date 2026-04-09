export type BusinessAcceptanceStatus =
  | 'neverRun'
  | 'running'
  | 'completed'
  | 'passed'
  | 'failed'
  | 'blocked'
  | string;

export interface BusinessAcceptancePackageModule {
  moduleCode: string;
  moduleName: string;
  suggestedDirection?: string;
  scenarioRefs: string[];
}

export interface BusinessAcceptanceLatestResult {
  runId?: string;
  status: BusinessAcceptanceStatus;
  updatedAt?: string;
  passedModuleCount: number;
  failedModuleCount: number;
  failedModuleNames: string[];
}

export interface BusinessAcceptancePackage {
  packageCode: string;
  packageName: string;
  description?: string;
  targetRoles: string[];
  supportedEnvironments: string[];
  defaultAccountTemplate: string;
  modules: BusinessAcceptancePackageModule[];
  latestResult?: BusinessAcceptanceLatestResult | null;
}

export interface BusinessAcceptanceAccountTemplate {
  templateCode: string;
  templateName: string;
  username: string;
  roleHint?: string;
  supportedEnvironments: string[];
}

export interface BusinessAcceptanceRunRequest {
  packageCode: string;
  environmentCode: string;
  accountTemplateCode: string;
  moduleCodes: string[];
}

export interface BusinessAcceptanceRunLaunch {
  jobId: string;
  status: BusinessAcceptanceStatus;
  runId?: string;
  startedAt?: string;
  errorMessage?: string;
}

export interface BusinessAcceptanceRunStatus {
  jobId: string;
  status: BusinessAcceptanceStatus;
  runId?: string;
  startedAt?: string;
  finishedAt?: string;
  errorMessage?: string;
}

export interface BusinessAcceptanceFailureDetail {
  scenarioId: string;
  scenarioTitle: string;
  stepLabel?: string;
  apiRef?: string;
  pageAction?: string;
  summary?: string;
}

export interface BusinessAcceptanceModuleResult {
  moduleCode: string;
  moduleName: string;
  status: BusinessAcceptanceStatus;
  failedScenarioCount: number;
  failedScenarioTitles: string[];
  suggestedDirection?: string;
  failureDetails: BusinessAcceptanceFailureDetail[];
}

export interface BusinessAcceptanceResult {
  status: BusinessAcceptanceStatus;
  passedModuleCount: number;
  failedModuleCount: number;
  failedModuleNames: string[];
  durationText?: string;
  runId: string;
  jumpToAutomationResultsPath?: string;
  modules: BusinessAcceptanceModuleResult[];
}
