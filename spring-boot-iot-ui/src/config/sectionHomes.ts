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
    key: 'iot-core',
    path: '/device-access',
    navLabel: '接入概览',
    navCaption: '查看设备接入分组能力与常用入口',
    navShort: '概',
    title: '设备接入',
    description: '围绕产品台账、设备运维、接入验证和链路排障组织设备接入能力。',
    intro: '建议先完成产品建档与设备建档，再进入接入验证、消息追踪和系统日志做链路排查。',
    matchKeys: ['iot-core', 'device-access', 'iot-access'],
    matchLabels: ['设备接入'],
    cards: [
      { path: '/products', label: '产品定义中心', description: '维护产品台账、接入协议与设备归属基线。', keywords: ['产品定义中心', '产品台账', '产品建档'] },
      { path: '/devices', label: '设备资产中心', description: '维护设备主数据、在线状态与认证字段。', keywords: ['设备资产中心', '设备资产', '设备档案'] },
      { path: '/reporting', label: '接入验证中心', description: '发起 HTTP 上报并核验主链路解析结果。', keywords: ['HTTP 上报实验台', '模拟上报', '接入验证'] },
      { path: '/insight', label: '监测对象工作台', description: '查看属性、日志与研判线索的聚合结果。', keywords: ['风险点工作台', '监测对象工作台', '属性查看'] },
      { path: '/message-trace', label: '消息追踪', description: '按 TraceId、设备编码和 Topic 串联接入链路。', keywords: ['消息追踪', 'TraceId', 'Topic'] },
      { path: '/system-log', label: '系统日志', description: '排查 `system_error`、MQTT 异常和后台链路问题。', keywords: ['系统日志', 'system_error', '异常排查'] },
      { path: '/file-debug', label: '数据完整性校验', description: '查看文件快照与固件聚合调试结果。', keywords: ['数据完整性校验', '文件校验', '固件调试'] }
    ],
    steps: ['先完成产品建档与接入基线维护。', '再维护设备档案与认证信息。', '通过接入验证、消息追踪和系统日志完成链路排查。']
  },
  {
    key: 'risk-core',
    path: '/risk-disposal',
    navLabel: '处置概览',
    navCaption: '查看风险处置主链路与能力入口',
    navShort: '概',
    title: '风险处置',
    description: '围绕告警、事件、风险点、规则和报表组织闭环处置能力。',
    intro: '建议先确认风险点和规则配置，再进入告警中心、事件处置和分析报表完成闭环验证。',
    matchKeys: ['risk-core', 'risk-disposal', 'risk-management'],
    matchLabels: ['风险处置', '预警处置'],
    cards: [
      { path: '/alarm-center', label: '告警中心', description: '查看告警列表、详情、确认、抑制和关闭。', keywords: ['告警中心', '告警'] },
      { path: '/event-disposal', label: '事件处置', description: '跟进工单派发、流转、反馈与关闭。', keywords: ['事件处置', '工单', '事件'] },
      { path: '/risk-point', label: '风险点管理', description: '维护风险点台账、设备绑定和监测范围。', keywords: ['风险点工作台', '风险点管理', '风险点'] },
      { path: '/rule-definition', label: '阈值规则', description: '维护阈值规则、测试与启停策略。', keywords: ['阈值规则', '规则定义'] },
      { path: '/linkage-rule', label: '联动规则', description: '管理联动条件与动作编排。', keywords: ['联动规则', '联动'] },
      { path: '/emergency-plan', label: '应急预案', description: '维护应急预案与联动执行准备。', keywords: ['应急预案', '预案'] },
      { path: '/report-analysis', label: '分析报表', description: '查看趋势、统计与闭环分析结果。', keywords: ['分析报表', '报表分析', '趋势分析'] }
    ],
    steps: ['先维护风险点、阈值规则和联动规则。', '再进入告警中心与事件处置验证闭环。', '最后通过分析报表复核处置结果和业务趋势。']
  },
  {
    key: 'system-core',
    path: '/system-management',
    navLabel: '治理概览',
    navCaption: '查看系统管理分组能力与常用入口',
    navShort: '概',
    title: '系统管理',
    description: '围绕组织、用户、角色、菜单、通知和审计组织治理能力。',
    intro: '建议先维护组织和用户，再做角色/菜单授权，最后通过通知渠道与业务日志完成治理闭环。',
    matchKeys: ['system-core', 'system-management', 'system-governance'],
    matchLabels: ['系统管理', '系统治理'],
    cards: [
      { path: '/organization', label: '组织管理', description: '维护组织树、层级关系与责任主体。', keywords: ['组织管理', '组织树'] },
      { path: '/user', label: '用户管理', description: '维护用户档案、状态、角色分配与改密。', keywords: ['用户管理', '账号管理'] },
      { path: '/role', label: '角色管理', description: '维护角色与页面/按钮权限关系。', keywords: ['角色管理', '授权'] },
      { path: '/menu', label: '菜单管理', description: '维护菜单树和页面权限项。', keywords: ['菜单管理', '菜单树'] },
      { path: '/region', label: '区域管理', description: '维护区域树、引用配置与筛选范围。', keywords: ['区域管理', '区域树'] },
      { path: '/dict', label: '字典配置', description: '维护字典分类、编码和值域。', keywords: ['字典配置', '字典'] },
      { path: '/channel', label: '通知渠道', description: '维护通知渠道配置、启停和测试。', keywords: ['通知渠道', '渠道配置'] },
      { path: '/audit-log', label: '业务日志', description: '查看治理侧业务审计与关键操作记录。', keywords: ['业务日志', '审计日志'] },
      { path: '/automation-test', label: '自动化测试', description: '维护巡检模板、执行计划和导出结果。', keywords: ['自动化测试', '巡检模板'] }
    ],
    steps: ['先维护组织、区域和用户主数据。', '再通过角色管理与菜单管理收口权限。', '最后用通知渠道与业务日志复核治理流程。']
  },
  {
    key: 'risk-enhance',
    path: '/risk-enhance',
    navLabel: '增强概览',
    navCaption: '查看风险增强能力与试运行入口',
    navShort: '概',
    title: '风险增强',
    description: '围绕实时监测与 GIS 态势页组织风险增强能力。',
    intro: '该分组当前仍以阶段能力验证为主，可先看实时监测列表，再联动 GIS 态势页。',
    matchKeys: ['risk-enhance', 'risk-monitoring-enhance'],
    matchLabels: ['风险增强'],
    cards: [
      { path: '/risk-monitoring', label: '实时监测', description: '查看实时监测列表与统一详情抽屉。', keywords: ['实时监测', '风险监测'] },
      { path: '/risk-monitoring-gis', label: 'GIS 风险态势', description: '查看点位态势与 GIS 风险联动。', keywords: ['GIS 风险态势', 'GIS', '风险态势'] }
    ],
    steps: ['先进入实时监测列表查看绑定数据。', '再进入 GIS 态势页查看点位分布。', '当前能力以试运行和联调验证为主。']
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
