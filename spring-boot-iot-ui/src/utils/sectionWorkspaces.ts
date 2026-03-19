import type { ActivityEntry } from '../types/api';

export interface SectionHomeCard {
  path: string;
  label: string;
  description: string;
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
  matchKeys: string[];
  matchLabels: string[];
  cards: SectionHomeCard[];
  steps: string[];
}

const sectionHomeConfigs: SectionHomeConfig[] = [
  {
    key: 'iot-access',
    path: '/device-access',
    navLabel: '智维总览',
    navCaption: '查看接入智维分组能力与常用入口',
    navShort: '概',
    title: '接入智维',
    description: '围绕产品定义、设备资产、链路验证和异常观测组织接入智维能力。',
    intro: '建议先完成产品定义与设备建档，再进入链路验证、链路追踪和异常观测做接入排障。',
    matchKeys: ['iot-access', 'device-access', 'iot-core'],
    matchLabels: ['接入智维', '设备接入'],
    cards: [
      { path: '/products', label: '产品定义中心', description: '维护产品台账、接入协议与设备归属基线。', keywords: ['产品定义中心', '产品台账', '产品建档'] },
      { path: '/devices', label: '设备资产中心', description: '维护设备主数据、在线状态与认证字段。', keywords: ['设备资产中心', '设备资产', '设备档案'] },
      { path: '/reporting', label: '链路验证中心', description: '发起 HTTP 上报并核验主链路解析结果。', keywords: ['链路验证中心', '模拟上报', '接入验证'] },
      { path: '/system-log', label: '异常观测台', description: '排查 `system_error`、MQTT 异常和后台链路问题。', keywords: ['异常观测台', 'system_error', '异常排查'] },
      { path: '/message-trace', label: '链路追踪台', description: '按 TraceId、设备编码和 Topic 串联接入链路。', keywords: ['链路追踪台', 'TraceId', 'Topic'] },
      { path: '/file-debug', label: '数据校验台', description: '查看文件快照与固件聚合调试结果。', keywords: ['数据校验台', '文件校验', '固件调试'] }
    ],
    steps: ['先完成产品建档与接入基线维护。', '再维护设备资产与认证信息。', '通过链路验证、链路追踪和异常观测完成接入排障。']
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
    matchKeys: ['risk-ops', 'risk-disposal', 'risk-core'],
    matchLabels: ['风险运营', '风险处置', '预警处置'],
    cards: [
      { path: '/risk-monitoring', label: '实时监测台', description: '查看监测清单、分级筛选与统一详情。', keywords: ['实时监测台', '实时监测', '风险监测'] },
      { path: '/risk-monitoring-gis', label: 'GIS态势图', description: '查看点位空间态势与 GIS 风险联动。', keywords: ['GIS态势图', 'GIS 风险态势', 'GIS'] },
      { path: '/alarm-center', label: '告警运营台', description: '查看告警列表、详情、确认、抑制和关闭。', keywords: ['告警运营台', '告警中心', '告警'] },
      { path: '/event-disposal', label: '事件协同台', description: '跟进工单派发、流转、反馈与关闭。', keywords: ['事件协同台', '事件处置', '工单'] },
      { path: '/insight', label: '对象洞察台', description: '查看监测对象属性、日志与研判线索。', keywords: ['对象洞察台', '监测对象工作台', '属性查看'] },
      { path: '/report-analysis', label: '运营分析中心', description: '查看趋势、统计与闭环分析结果。', keywords: ['运营分析中心', '分析报表', '趋势分析'] }
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
    matchKeys: ['risk-config'],
    matchLabels: ['风险策略', '风险配置'],
    cards: [
      { path: '/risk-point', label: '风险对象中心', description: '维护风险对象台账、设备绑定和监测范围。', keywords: ['风险对象中心', '风险点管理', '风险对象'] },
      { path: '/rule-definition', label: '阈值策略', description: '维护阈值策略、测试与启停配置。', keywords: ['阈值策略', '阈值规则', '规则定义'] },
      { path: '/linkage-rule', label: '联动编排', description: '管理联动条件、动作链和通知编排。', keywords: ['联动编排', '联动规则', '联动'] },
      { path: '/emergency-plan', label: '应急预案库', description: '维护应急预案、响应步骤与演练准备。', keywords: ['应急预案库', '应急预案', '预案'] }
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
    description: '围绕组织、账号、角色、导航、通知和审计组织平台治理能力。',
    intro: '建议先维护组织与账号，再做角色权限和导航编排，最后通过通知编排与审计中心完成治理闭环。',
    matchKeys: ['system-governance', 'system-management', 'system-core'],
    matchLabels: ['平台治理', '系统管理', '系统治理'],
    cards: [
      { path: '/organization', label: '组织架构', description: '维护组织树、层级关系与责任主体。', keywords: ['组织架构', '组织管理', '组织树'] },
      { path: '/user', label: '账号中心', description: '维护账号档案、状态、角色分配与改密。', keywords: ['账号中心', '用户管理', '账号管理'] },
      { path: '/role', label: '角色权限', description: '维护角色职责与页面/按钮权限关系。', keywords: ['角色权限', '角色管理', '授权'] },
      { path: '/menu', label: '导航编排', description: '维护菜单树、路由元数据和权限项。', keywords: ['导航编排', '菜单管理', '菜单树'] },
      { path: '/region', label: '区域版图', description: '维护区域树、引用配置与筛选范围。', keywords: ['区域版图', '区域管理', '区域树'] },
      { path: '/dict', label: '数据字典', description: '维护字典分类、编码和值域。', keywords: ['数据字典', '字典配置', '字典'] },
      { path: '/channel', label: '通知编排', description: '维护通知渠道配置、启停和测试。', keywords: ['通知编排', '通知渠道', '渠道配置'] },
      { path: '/audit-log', label: '审计中心', description: '查看治理侧业务审计与关键操作记录。', keywords: ['审计中心', '业务日志', '审计日志'] }
    ],
    steps: ['先维护组织、区域和账号主数据。', '再通过角色权限与导航编排收口权限。', '最后用通知编排与审计中心复核治理流程。']
  },
  {
    key: 'quality-workbench',
    path: '/quality-workbench',
    navLabel: '工场总览',
    navCaption: '查看质量工场能力与自动化入口',
    navShort: '概',
    title: '质量工场',
    description: '围绕自动化编排、回归计划和质量基线组织工程质量能力。',
    intro: '建议先在自动化工场沉淀巡检模板，再导出执行计划与质量报告，形成持续回归基线。',
    matchKeys: ['quality-workbench', 'quality-core'],
    matchLabels: ['质量工场', '测试工具'],
    cards: [
      { path: '/automation-test', label: '自动化工场', description: '维护巡检模板、执行计划和导出结果。', keywords: ['自动化工场', '自动化测试', '巡检模板'] }
    ],
    steps: ['先整理巡检模板与场景资产。', '再导出执行计划并组织回归。', '最后沉淀质量报告和视觉基线。']
  }
];

function normalizeText(value?: string | null): string {
  return (value || '').trim().toLowerCase();
}

function normalizePath(path?: string | null): string {
  const normalized = (path || '').trim().replace(/\/+$/, '');
  return normalized || '/';
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
      || item.matchLabels.some((label) => normalizeText(label) === normalizedLabel);
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

export function canAccessSectionHome(path: string, allowedPaths: string[]): boolean {
  const config = getSectionHomeConfigByPath(path);
  if (!config) {
    return false;
  }
  const normalizedAllowed = new Set(allowedPaths.map((item) => normalizePath(item)));
  return config.cards.some((card) => normalizedAllowed.has(normalizePath(card.path)));
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
