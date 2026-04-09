import type { ActivityEntry } from '../types/api';

export interface SectionHomeCard {
  path: string;
  label: string;
  description: string;
  short: string;
  keywords?: string[];
}

export interface SectionHomeConfig {
  key: string;
  path: string;
  navLabel: string;
  navCaption: string;
  navShort: string;
  title: string;
  description: string;
  intro: string;
  hubJudgement?: string;
  hubLeadTitle?: string;
  hubLeadDescription?: string;
  hubLeadPath?: string;
  menuTitle: string;
  menuHint: string;
  matchKeys: string[];
  matchLabels: string[];
  cards: SectionHomeCard[];
  steps: string[];
}

export interface WorkspaceNavItem {
  to: string;
  label: string;
  caption: string;
  short: string;
}

export interface WorkspaceNavGroup {
  key: string;
  label: string;
  description: string;
  menuTitle: string;
  menuHint: string;
  items: WorkspaceNavItem[];
}

export interface RouteMetaPreset {
  title: string;
  description: string;
  requiresAuth?: boolean;
  layout?: 'blank';
}

export interface WorkspaceCommandEntry {
  id: string;
  path: string;
  title: string;
  description: string;
  workspaceKey: string;
  workspaceLabel: string;
  short: string;
  type: 'overview' | 'page';
  keywords: string[];
}

export type CockpitRoleKey = 'frontline' | 'ops' | 'manager' | 'rd';

export interface RoleWorkbenchProfile {
  key: string;
  label: string;
  roleCodes: string[];
  roleNameKeywords: string[];
  defaultPath: string;
  preferredWorkspaceKeys: string[];
  featuredPaths: string[];
  cockpitRole: CockpitRoleKey;
  focusLabel: string;
  focusDescription: string;
}

const sectionHomeConfigs: SectionHomeConfig[] = [
  {
    key: 'iot-access',
    path: '/device-access',
    navLabel: '智维总览',
    navCaption: '查看接入智维分组能力与常用入口',
    navShort: '概',
    title: '接入智维',
    description: '接入智维总览只负责入口分组和快速判断，不再重复子页说明墙。',
    intro: '这里回答“先去哪处理”，子页只回答“在这个域里怎么做”。',
    hubJudgement: '先进入资产底座，再切换到诊断排障。',
    hubLeadTitle: '优先处理资产底座与最近异常联动',
    hubLeadDescription: '产品与设备先稳住，链路验证、异常观测、链路追踪、数据校验只保留强相关联动。',
    hubLeadPath: '/products',
    menuTitle: '接入智维',
    menuHint: '覆盖产品定义、设备资产、链路验证、异常观测、链路追踪与数据校验。',
    matchKeys: ['iot-access', 'device-access', 'iot-core'],
    matchLabels: ['接入智维', '设备接入'],
    cards: [
      { path: '/products', label: '产品定义中心', description: '维护产品台账、接入协议与设备归属基线。', short: '产', keywords: ['产品定义中心', '产品台账', '产品建档'] },
      { path: '/devices', label: '设备资产中心', description: '维护设备主数据、在线状态与认证字段。', short: '设', keywords: ['设备资产中心', '设备资产', '设备档案'] },
      { path: '/reporting', label: '链路验证中心', description: '按设备编码反查接入契约，执行 HTTP / MQTT 模拟上报并核验主链路解析结果。', short: '验', keywords: ['链路验证中心', '模拟上报', '接入验证', 'HTTP', 'MQTT', '设备反查'] },
      { path: '/system-log', label: '异常观测台', description: '排查 system_error、MQTT 异常和后台链路问题。', short: '观', keywords: ['异常观测台', 'system_error', '异常排查'] },
      { path: '/message-trace', label: '链路追踪台', description: '按 TraceId、设备编码和 Topic 串联接入链路，并切换查看失败归档。', short: '追', keywords: ['链路追踪台', 'TraceId', 'Topic', '失败归档', '接入失败'] },
      { path: '/file-debug', label: '数据校验台', description: '查看文件快照与固件聚合调试结果。', short: '校', keywords: ['数据校验台', '文件校验', '固件调试'] }
    ],
    steps: ['先完成产品定义与协议绑定。', '再维护设备资产与认证信息。', '通过链路验证、链路追踪和异常观测完成接入排障。']
  },
  {
    key: 'risk-ops',
    path: '/risk-disposal',
    navLabel: '运营总览',
    navCaption: '查看风险运营主链路与能力入口',
    navShort: '概',
    title: '风险运营',
    description: '围绕实时态势、告警协同、事件处置和运营复盘组织风险运营能力。',
    intro: '建议优先查看实时态势与告警协同，再进入事件处置、对象洞察和运营分析完成闭环复盘。',
    menuTitle: '风险运营',
    menuHint: '覆盖实时监测、告警运营、事件协同、对象洞察、GIS 态势与运营复盘。',
    matchKeys: ['risk-ops', 'risk-disposal', 'risk-core'],
    matchLabels: ['风险运营', '风险处置', '预警处置'],
    cards: [
      { path: '/risk-monitoring', label: '实时监测台', description: '查看监测清单、分级筛选与统一详情。', short: '监', keywords: ['实时监测台', '实时监测', '风险监测'] },
      { path: '/risk-monitoring-gis', label: 'GIS态势图', description: '查看点位空间态势与 GIS 风险联动。', short: '图', keywords: ['GIS态势图', 'GIS 风险态势', 'GIS'] },
      { path: '/alarm-center', label: '告警运营台', description: '查看告警列表、详情、确认、抑制和关闭。', short: '告', keywords: ['告警运营台', '告警中心', '告警'] },
      { path: '/event-disposal', label: '事件协同台', description: '跟进工单派发、流转、反馈与关闭。', short: '事', keywords: ['事件协同台', '事件处置', '工单'] },
      { path: '/insight', label: '对象洞察台', description: '查看监测对象属性、日志与研判线索。', short: '洞', keywords: ['对象洞察台', '监测对象工作台', '属性查看'] },
      { path: '/report-analysis', label: '运营分析中心', description: '查看趋势、统计与闭环分析结果。', short: '析', keywords: ['运营分析中心', '分析报表', '趋势分析'] }
    ],
    steps: ['先查看实时态势与告警清单。', '再进入事件协同与对象洞察处理重点问题。', '最后通过运营分析复核处置结果和业务趋势。']
  },
  {
    key: 'risk-config',
    path: '/risk-config',
    navLabel: '策略总览',
    navCaption: '查看风险策略分组能力与配置入口',
    navShort: '概',
    title: '风险策略',
    description: '围绕风险对象、阈值策略、联动编排和预案体系组织策略能力。',
    intro: '建议先维护风险对象与阈值策略，再补齐联动编排和应急预案，形成稳定的风险策略底座。',
    menuTitle: '风险策略',
    menuHint: '覆盖风险对象、阈值策略、联动编排与应急预案库。',
    matchKeys: ['risk-config'],
    matchLabels: ['风险策略', '风险配置'],
    cards: [
      { path: '/risk-point', label: '风险对象中心', description: '维护风险对象台账、设备绑定和监测范围。', short: '险', keywords: ['风险对象中心', '风险点管理', '风险对象'] },
      { path: '/rule-definition', label: '阈值策略', description: '维护阈值策略、测试与启停配置。', short: '阈', keywords: ['阈值策略', '阈值规则', '规则定义'] },
      { path: '/linkage-rule', label: '联动编排', description: '管理联动条件、动作链和通知编排。', short: '联', keywords: ['联动编排', '联动规则', '联动'] },
      { path: '/emergency-plan', label: '应急预案库', description: '维护应急预案、响应步骤与演练准备。', short: '预', keywords: ['应急预案库', '应急预案', '预案'] }
    ],
    steps: ['先建立风险对象与监测范围。', '再配置阈值策略与联动编排。', '最后补齐应急预案与响应准备。']
  },
  {
    key: 'system-governance',
    path: '/system-management',
    navLabel: '治理总览',
    navCaption: '查看平台治理分组能力与常用入口',
    navShort: '概',
    title: '平台治理',
    description: '围绕组织、账号、角色、导航、通知、帮助和审计组织平台治理能力。',
    intro: '建议先维护组织与账号，再做角色权限和导航编排，随后补齐控制面任务、通知渠道、帮助文档和审计闭环。',
    menuTitle: '平台治理',
    menuHint: '覆盖组织、账号、角色、导航、控制面任务、通知、帮助与审计中心。',
    matchKeys: ['system-governance', 'system-management', 'system-core'],
    matchLabels: ['平台治理', '系统管理', '系统治理'],
    cards: [
      { path: '/organization', label: '组织架构', description: '维护组织树、层级关系与责任主体。', short: '组', keywords: ['组织架构', '组织管理', '组织树'] },
      { path: '/user', label: '账号中心', description: '维护账号档案、状态、角色分配与改密。', short: '账', keywords: ['账号中心', '用户管理', '账号管理'] },
      { path: '/role', label: '角色权限', description: '维护角色职责与页面/按钮权限关系。', short: '角', keywords: ['角色权限', '角色管理', '授权'] },
      { path: '/menu', label: '导航编排', description: '维护菜单树、路由元数据和权限项。', short: '导', keywords: ['导航编排', '菜单管理', '菜单树'] },
      { path: '/region', label: '区域版图', description: '维护区域树、引用配置与筛选范围。', short: '区', keywords: ['区域版图', '区域管理', '区域树'] },
      { path: '/dict', label: '数据字典', description: '维护字典分类、编码和值域。', short: '字', keywords: ['数据字典', '字典配置', '字典'] },
      { path: '/channel', label: '通知编排', description: '维护通知渠道配置、启停和测试。', short: '通', keywords: ['通知编排', '通知渠道', '渠道配置'] },
      { path: '/in-app-message', label: '站内消息', description: '维护通知中心消费的系统、业务和错误事件消息。', short: '信', keywords: ['站内消息', '通知中心', '消息编排'] },
      { path: '/help-doc', label: '帮助文档', description: '维护帮助中心消费的业务、技术和 FAQ 资料。', short: '帮', keywords: ['帮助文档', '帮助中心', 'FAQ'] },
      { path: '/governance-task', label: '治理任务台', description: '统一查看待发布合同、风险绑定等控制面待办。', short: '任', keywords: ['治理任务台', '控制面任务', '待发布合同', '待绑定风险点'] },
      { path: '/governance-ops', label: '治理运维台', description: '统一查看字段漂移、合同差异和指标缺失等控制面告警。', short: '运', keywords: ['治理运维台', '控制面告警', '字段漂移', '合同差异'] },
      { path: '/governance-approval', label: '治理审批台', description: '统一查看审批主单、执行结果与状态流转。', short: '批', keywords: ['治理审批台', '审批主单', '双人复核'] },
      { path: '/audit-log', label: '审计中心', description: '查看治理侧业务审计与关键操作记录。', short: '审', keywords: ['审计中心', '业务日志', '审计日志'] }
    ],
    steps: ['先维护组织、区域和账号主数据。', '再通过角色权限与导航编排收口权限。', '随后通过治理任务台、治理运维台和治理审批台跟踪控制面执行。', '最后用审计中心复核治理流程。']
  },
  {
    key: 'quality-workbench',
    path: '/quality-workbench',
    navLabel: '工场总览',
    navCaption: '查看质量工场能力与专项入口',
    navShort: '概',
    title: '质量工场',
    description: '围绕业务验收、研发工场、执行组织与结果基线组织工程质量能力。',
    intro: '建议先判断当前要发起业务验收、编排研发自动化资产，还是复盘运行结果，再进入对应专项页。',
    menuTitle: '质量工场',
    menuHint: '覆盖业务验收、研发资产编排、执行组织与结果基线治理。',
    matchKeys: ['quality-workbench', 'quality-core'],
    matchLabels: ['质量工场', '测试工具'],
    cards: [
      { path: '/business-acceptance', label: '业务验收台', description: '按交付清单选择预置验收包并一键运行业务验收。', short: '验', keywords: ['业务验收台', '业务验收', '交付清单验收', '验收包'] },
      { path: '/rd-workbench', label: '研发工场', description: '面向研发的自动化资产编排主入口。', short: '研', keywords: ['研发工场', '研发自动化', '自动化资产编排'] },
      { path: '/automation-execution', label: '执行中心', description: '统一查看执行配置、命令预览和验收注册表依赖关系。', short: '执', keywords: ['执行中心', '执行配置', '验收注册表'] },
      { path: '/automation-results', label: '结果与基线中心', description: '统一导入运行结果、查看失败场景并维护质量建议与基线证据。', short: '果', keywords: ['结果与基线中心', '运行结果', '视觉基线'] }
    ],
    steps: ['业务验收优先进入业务验收台，按交付清单选择验收包并一键执行。', '研发再进入研发工场拆分盘点、模板、计划与交付任务。', '统一通过执行中心组织回归，并在结果与基线中心沉淀证据与改进建议。']
  },
  {
    key: 'rd-workbench',
    path: '/rd-workbench',
    navLabel: '研发总览',
    navCaption: '查看研发自动化资产编排主链路',
    navShort: '研',
    title: '研发工场',
    description: '围绕页面盘点、模板沉淀、计划编排与交付打包组织研发自动化资产能力。',
    intro: '建议先盘点页面和模板，再进入计划编排，最后整理交付包并转到执行中心。',
    menuTitle: '研发工场',
    menuHint: '覆盖研发自动化资产编排与交付准备。',
    matchKeys: ['rd-workbench', 'automation-rd'],
    matchLabels: ['研发工场', '研发自动化'],
    cards: [
      { path: '/rd-automation-inventory', label: '页面盘点台', description: '维护页面清单、覆盖缺口与人工补录页面。', short: '盘', keywords: ['页面盘点台', '页面清单', '覆盖缺口'] },
      { path: '/rd-automation-templates', label: '场景模板台', description: '沉淀页面冒烟、表单提交与列表详情模板。', short: '模', keywords: ['场景模板台', '场景模板', '模板沉淀'] },
      { path: '/rd-automation-plans', label: '计划编排台', description: '维护场景顺序、步骤、断言、导入与导出。', short: '编', keywords: ['计划编排台', '场景顺序', '计划导入导出'] },
      { path: '/rd-automation-handoff', label: '交付打包台', description: '整理计划摘要、执行建议、基线说明与验收备注。', short: '交', keywords: ['交付打包台', '执行建议', '验收备注'] }
    ],
    steps: ['先盘点页面。', '再沉淀模板。', '然后编排正式计划。', '最后整理交付包并转执行中心。']
  }
];

const specialRouteMetaPresets: Record<string, RouteMetaPreset> = {
  '/login': {
    title: '登录',
    description: '平台统一登录入口。',
    requiresAuth: false,
    layout: 'blank'
  },
  '/': {
    title: '平台首页',
    description: '五工作台统一入口与全局业务总览。',
    requiresAuth: true
  },
  '/future-lab': {
    title: '演进蓝图',
    description: '预研能力展示与未来扩展方向说明。',
    requiresAuth: true
  },
  '/automation-assets': {
    title: '自动化资产中心',
    description: '兼容旧入口，第一轮直接落到研发工场总览。',
    requiresAuth: true
  },
  '/automation-test': {
    title: '自动化工场',
    description: '兼容旧入口，第一轮直接落到研发工场总览。',
    requiresAuth: true
  }
};

const workspaceCompatibilityRouteMap: Record<string, string[]> = {
  '/automation-test': [
    '/rd-workbench',
    '/rd-automation-inventory',
    '/rd-automation-templates',
    '/rd-automation-plans',
    '/rd-automation-handoff',
    '/automation-execution',
    '/automation-results'
  ],
  '/automation-assets': [
    '/rd-workbench',
    '/rd-automation-inventory',
    '/rd-automation-templates',
    '/rd-automation-plans',
    '/rd-automation-handoff'
  ],
  '/rd-workbench': [
    '/rd-automation-inventory',
    '/rd-automation-templates',
    '/rd-automation-plans',
    '/rd-automation-handoff'
  ]
};

const hiddenCompatibilityWorkspacePaths = new Set(['/automation-assets', '/automation-test']);

const canonicalGroupNavOrders: Record<string, string[]> = {
  'quality-workbench': [
    '/quality-workbench',
    '/business-acceptance',
    '/rd-workbench',
    '/rd-automation-inventory',
    '/rd-automation-templates',
    '/rd-automation-plans',
    '/rd-automation-handoff',
    '/automation-execution',
    '/automation-results'
  ],
  'rd-workbench': [
    '/rd-workbench',
    '/rd-automation-inventory',
    '/rd-automation-templates',
    '/rd-automation-plans',
    '/rd-automation-handoff'
  ]
};

const guestRoleProfile: RoleWorkbenchProfile = {
  key: 'guest',
  label: '访客',
  roleCodes: [],
  roleNameKeywords: [],
  defaultPath: '/',
  preferredWorkspaceKeys: [],
  featuredPaths: ['/'],
  cockpitRole: 'frontline',
  focusLabel: '平台总览',
  focusDescription: '未登录时仅开放首页预览与登录入口。'
};

const generalRoleProfile: RoleWorkbenchProfile = {
  key: 'general',
  label: '通用角色',
  roleCodes: [],
  roleNameKeywords: [],
  defaultPath: '/risk-disposal',
  preferredWorkspaceKeys: ['risk-ops', 'risk-config'],
  featuredPaths: ['/risk-monitoring', '/alarm-center', '/event-disposal', '/report-analysis'],
  cockpitRole: 'frontline',
  focusLabel: '运营闭环',
  focusDescription: '优先关注实时态势、告警处置与运营复盘。'
};

const roleProfiles: RoleWorkbenchProfile[] = [
  {
    key: 'super-admin',
    label: '超级管理员',
    roleCodes: ['SUPER_ADMIN'],
    roleNameKeywords: ['超级管理员'],
    defaultPath: '/system-management',
    preferredWorkspaceKeys: ['system-governance', 'risk-ops', 'iot-access'],
    featuredPaths: ['/governance-task', '/governance-ops', '/user', '/role', '/audit-log'],
    cockpitRole: 'manager',
    focusLabel: '平台治理',
    focusDescription: '优先进入组织、权限、导航和审计治理核心能力。'
  },
  {
    key: 'business',
    label: '业务人员',
    roleCodes: ['BUSINESS_STAFF'],
    roleNameKeywords: ['业务'],
    defaultPath: '/risk-disposal',
    preferredWorkspaceKeys: ['risk-ops', 'risk-config'],
    featuredPaths: ['/risk-monitoring', '/alarm-center', '/event-disposal', '/report-analysis'],
    cockpitRole: 'frontline',
    focusLabel: '风险运营',
    focusDescription: '优先关注实时监测、告警研判、事件闭环与运营复盘。'
  },
  {
    key: 'manager',
    label: '管理人员',
    roleCodes: ['MANAGEMENT_STAFF'],
    roleNameKeywords: ['管理'],
    defaultPath: '/risk-disposal',
    preferredWorkspaceKeys: ['risk-ops', 'risk-config', 'system-governance'],
    featuredPaths: ['/governance-task', '/governance-ops', '/report-analysis', '/event-disposal', '/audit-log'],
    cockpitRole: 'manager',
    focusLabel: '经营统筹',
    focusDescription: '优先查看经营分析、闭环效率、策略覆盖和治理执行情况。'
  },
  {
    key: 'ops',
    label: '运维人员',
    roleCodes: ['OPS_STAFF'],
    roleNameKeywords: ['运维'],
    defaultPath: '/device-access',
    preferredWorkspaceKeys: ['iot-access', 'risk-ops'],
    featuredPaths: ['/devices', '/risk-monitoring', '/alarm-center', '/message-trace'],
    cockpitRole: 'ops',
    focusLabel: '接入稳定性',
    focusDescription: '优先排查设备在线、实时态势、告警队列和链路追踪。'
  },
  {
    key: 'developer',
    label: '开发人员',
    roleCodes: ['DEVELOPER_STAFF'],
    roleNameKeywords: ['开发', '研发'],
    defaultPath: '/device-access',
    preferredWorkspaceKeys: ['iot-access', 'risk-config', 'quality-workbench'],
    featuredPaths: ['/reporting', '/system-log', '/message-trace', '/quality-workbench'],
    cockpitRole: 'rd',
    focusLabel: '链路与质量',
    focusDescription: '优先联调接入链路、异常观测、消息追踪和自动化回归。'
  }
];

function normalizeText(value?: string | null): string {
  return (value || '').trim().toLowerCase();
}

function normalizePath(path?: string | null): string {
  const normalized = (path || '').trim().replace(/\/+$/, '');
  return normalized || '/';
}

export function expandWorkspaceAllowedPaths(paths?: string[] | null): string[] {
  const result = new Set<string>();
  const queue = (paths || []).map((item) => normalizePath(item));

  while (queue.length > 0) {
    const currentPath = queue.shift();
    if (!currentPath || result.has(currentPath)) {
      continue;
    }

    result.add(currentPath);
    const aliasedPaths = workspaceCompatibilityRouteMap[currentPath] || [];
    aliasedPaths.forEach((path) => {
      const normalizedPath = normalizePath(path);
      if (!result.has(normalizedPath)) {
        queue.push(normalizedPath);
      }
    });
  }

  return Array.from(result);
}

export function isCompatibilityWorkspacePath(path?: string | null): boolean {
  return hiddenCompatibilityWorkspacePaths.has(normalizePath(path));
}

function buildActivitySearchText(entry: Pick<ActivityEntry, 'title' | 'detail' | 'module' | 'action' | 'path'>): string {
  return [
    entry.title,
    entry.detail,
    entry.module,
    entry.action,
    entry.path
  ].map((item) => normalizeText(item)).filter(Boolean).join(' ');
}

function findSectionCardRecord(path?: string | null) {
  const normalizedPath = normalizePath(path);
  for (const config of sectionHomeConfigs) {
    const card = config.cards.find((item) => normalizePath(item.path) === normalizedPath);
    if (card) {
      return { config, card };
    }
  }
  return null;
}

function createSectionOverviewNavItem(config: SectionHomeConfig): WorkspaceNavItem {
  return {
    to: config.path,
    label: config.navLabel,
    caption: config.navCaption,
    short: config.navShort
  };
}

function createSectionCardNavItem(card: SectionHomeCard): WorkspaceNavItem {
  return {
    to: card.path,
    label: card.label,
    caption: card.description,
    short: card.short
  };
}

function pathAllowed(path: string, allowedPaths?: string[]): boolean {
  if (!allowedPaths || allowedPaths.length === 0) {
    return true;
  }
  const normalizedPath = normalizePath(path);
  const normalizedAllowed = new Set(expandWorkspaceAllowedPaths(allowedPaths).map((item) => normalizePath(item)));
  if (normalizedAllowed.has(normalizedPath)) {
    return true;
  }
  return canAccessSectionHome(normalizedPath, Array.from(normalizedAllowed));
}

export function listSectionHomeConfigs(): SectionHomeConfig[] {
  return sectionHomeConfigs;
}

export function getSectionHomeConfigByPath(path?: string | null): SectionHomeConfig | undefined {
  const normalizedPath = normalizePath(path);
  return sectionHomeConfigs.find((item) => item.path === normalizedPath);
}

export function resolveSectionHomeConfig(groupKey?: string | null, groupLabel?: string | null): SectionHomeConfig | undefined {
  const normalizedKey = normalizeText(groupKey);
  const normalizedLabel = normalizeText(groupLabel);
  return sectionHomeConfigs.find((item) => {
    return item.matchKeys.some((key) => normalizeText(key) === normalizedKey)
      || item.matchLabels.some((label) => normalizeText(label) === normalizedLabel)
      || normalizeText(item.key) === normalizedKey
      || normalizeText(item.title) === normalizedLabel;
  });
}

export function createSectionHomeNavItem(groupKey?: string | null, groupLabel?: string | null) {
  const config = resolveSectionHomeConfig(groupKey, groupLabel);
  if (!config) {
    return null;
  }
  return {
    to: config.path,
    label: config.navLabel,
    caption: config.navCaption,
    short: config.navShort
  };
}

export function listStaticNavigationGroups(): WorkspaceNavGroup[] {
  return sectionHomeConfigs.map((config) => ({
    key: config.key,
    label: config.title,
    description: config.description,
    menuTitle: config.menuTitle,
    menuHint: config.menuHint,
    items: config.cards.map((card) => ({
      to: card.path,
      label: card.label,
      caption: card.description,
      short: card.short
    }))
  }));
}

export function listCanonicalWorkspaceNavItems(
  groupKey?: string | null,
  groupLabel?: string | null,
  allowedPaths?: string[]
): WorkspaceNavItem[] {
  const config = resolveSectionHomeConfig(groupKey, groupLabel);
  if (!config) {
    return [];
  }

  const expandedAllowedPaths = expandWorkspaceAllowedPaths(allowedPaths);
  const entryPaths = canonicalGroupNavOrders[config.key]
    || [config.path, ...config.cards.map((card) => card.path)];
  const items: WorkspaceNavItem[] = [];
  const seenPaths = new Set<string>();

  entryPaths.forEach((path) => {
    const normalizedPath = normalizePath(path);
    if (seenPaths.has(normalizedPath) || !pathAllowed(normalizedPath, expandedAllowedPaths)) {
      return;
    }

    if (normalizedPath === config.path) {
      items.push(createSectionOverviewNavItem(config));
      seenPaths.add(normalizedPath);
      return;
    }

    const matchedCard = findSectionCardRecord(normalizedPath);
    if (!matchedCard) {
      return;
    }
    items.push(createSectionCardNavItem(matchedCard.card));
    seenPaths.add(normalizedPath);
  });

  return items;
}

export function getRouteMetaPreset(path?: string | null): RouteMetaPreset | undefined {
  const normalizedPath = normalizePath(path);
  const specialPreset = specialRouteMetaPresets[normalizedPath];
  if (specialPreset) {
    return specialPreset;
  }

  const sectionHome = getSectionHomeConfigByPath(normalizedPath);
  if (sectionHome) {
    return {
      title: sectionHome.title,
      description: sectionHome.description,
      requiresAuth: true
    };
  }

  const matchedCard = findSectionCardRecord(normalizedPath);
  if (matchedCard) {
    return {
      title: matchedCard.card.label,
      description: matchedCard.card.description,
      requiresAuth: true
    };
  }

  return undefined;
}

export function getWorkspaceCommandEntryByPath(path?: string | null): WorkspaceCommandEntry | undefined {
  const normalizedPath = normalizePath(path);
  return listWorkspaceCommandEntries().find((entry) => entry.path === normalizedPath);
}

export function listWorkspaceCommandEntries(allowedPaths?: string[]): WorkspaceCommandEntry[] {
  const entries: WorkspaceCommandEntry[] = [];

  sectionHomeConfigs.forEach((config) => {
    if (pathAllowed(config.path, allowedPaths)) {
      entries.push({
        id: `overview:${config.key}`,
        path: config.path,
        title: config.title,
        description: config.navCaption,
        workspaceKey: config.key,
        workspaceLabel: config.title,
        short: config.navShort,
        type: 'overview',
        keywords: [config.title, config.path, config.navLabel, config.description, ...config.matchLabels, ...config.matchKeys]
          .map((item) => normalizeText(item))
          .filter(Boolean)
      });
    }

    config.cards.forEach((card) => {
      if (!pathAllowed(card.path, allowedPaths)) {
        return;
      }
      entries.push({
        id: `page:${card.path}`,
        path: card.path,
        title: card.label,
        description: card.description,
        workspaceKey: config.key,
        workspaceLabel: config.title,
        short: card.short,
        type: 'page',
        keywords: [card.label, card.path, config.title, config.key, ...(card.keywords || []), ...config.matchLabels]
          .map((item) => normalizeText(item))
          .filter(Boolean)
      });
    });
  });

  return entries;
}

export function canAccessSectionHome(path: string, allowedPaths: string[]): boolean {
  const config = getSectionHomeConfigByPath(path);
  if (!config) {
    return false;
  }
  const normalizedAllowed = new Set(expandWorkspaceAllowedPaths(allowedPaths).map((item) => normalizePath(item)));
  return config.cards.some((card) => normalizedAllowed.has(normalizePath(card.path)));
}

export function matchSectionCardActivity(
  card: SectionHomeCard,
  entry: Pick<ActivityEntry, 'title' | 'detail' | 'module' | 'action' | 'path'>
): boolean {
  const searchText = buildActivitySearchText(entry);
  if (!searchText) {
    return false;
  }

  const keywords = [card.label, card.path, ...(card.keywords || [])]
    .map((item) => normalizeText(item))
    .filter(Boolean);

  return keywords.some((keyword) => searchText.includes(keyword));
}

export function pickSectionActivities(cards: SectionHomeCard[], entries: ActivityEntry[], limit = 4): ActivityEntry[] {
  if (cards.length === 0 || entries.length === 0 || limit <= 0) {
    return [];
  }

  const matchedEntries: ActivityEntry[] = [];
  const seenKeys = new Set<string>();

  entries.forEach((entry) => {
    if (matchedEntries.length >= limit) {
      return;
    }

    const matched = cards.some((card) => matchSectionCardActivity(card, entry));
    if (!matched) {
      return;
    }

    const dedupeKey = `${entry.path || ''}|${entry.title}|${entry.detail}`;
    if (seenKeys.has(dedupeKey)) {
      return;
    }

    seenKeys.add(dedupeKey);
    matchedEntries.push(entry);
  });

  return matchedEntries;
}

export function sortByPreferredPaths<T extends { path: string }>(entries: T[], preferredPaths: string[]): T[] {
  if (entries.length <= 1 || preferredPaths.length === 0) {
    return entries.slice();
  }

  const order = new Map(preferredPaths.map((path, index) => [normalizePath(path), index]));
  return entries.slice().sort((left, right) => {
    const leftRank = order.get(normalizePath(left.path));
    const rightRank = order.get(normalizePath(right.path));
    if (leftRank === undefined && rightRank === undefined) {
      return 0;
    }
    if (leftRank === undefined) {
      return 1;
    }
    if (rightRank === undefined) {
      return -1;
    }
    return leftRank - rightRank;
  });
}

export function resolveRoleWorkbenchProfile(
  roleCodes?: string[] | null,
  roleNames?: string[] | null,
  superAdmin = false
): RoleWorkbenchProfile {
  if (superAdmin) {
    return roleProfiles[0];
  }

  const normalizedCodes = new Set((roleCodes || []).map((item) => normalizeText(item)));
  const normalizedNames = (roleNames || []).map((item) => normalizeText(item)).filter(Boolean);

  if (normalizedCodes.size === 0 && normalizedNames.length === 0) {
    return guestRoleProfile;
  }

  const matchedProfile = roleProfiles.find((profile) => {
    const codeMatched = profile.roleCodes.some((item) => normalizedCodes.has(normalizeText(item)));
    const nameMatched = profile.roleNameKeywords.some((item) => {
      const keyword = normalizeText(item);
      return normalizedNames.some((name) => name.includes(keyword));
    });
    return codeMatched || nameMatched;
  });

  return matchedProfile || generalRoleProfile;
}

export function resolveRoleHomePath(
  roleCodes?: string[] | null,
  roleNames?: string[] | null,
  superAdmin = false,
  allowedPaths?: string[]
): string {
  const profile = resolveRoleWorkbenchProfile(roleCodes, roleNames, superAdmin);
  const candidates = [
    profile.defaultPath,
    ...profile.featuredPaths,
    ...profile.preferredWorkspaceKeys
      .map((workspaceKey) => resolveSectionHomeConfig(workspaceKey, workspaceKey)?.path)
      .filter((item): item is string => Boolean(item))
  ];

  const resolvedCandidate = candidates.find((path) => pathAllowed(path, allowedPaths));
  if (resolvedCandidate) {
    return normalizePath(resolvedCandidate);
  }

  if (allowedPaths && allowedPaths.length > 0) {
    const [firstPath] = sortByPreferredPaths(
      allowedPaths.map((path) => ({ path })),
      profile.featuredPaths
    );
    if (firstPath?.path) {
      return normalizePath(firstPath.path);
    }
  }

  return normalizePath(profile.defaultPath || '/');
}
