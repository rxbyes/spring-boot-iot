import type { HelpDocumentAccessRecord } from '../api/helpDoc';
import type { InAppMessageAccessRecord, InAppMessagePriority, InAppMessageType, InAppMessageUnreadStats } from '../api/inAppMessage';
import type { ActivityEntry } from '../types/api';
import type {
  ShellPopoverContent,
  ShellPopoverItem,
  ShellPopoverSection,
  ShellPopoverTone
} from '../types/shell';
import { formatDateTime, truncateText } from './format';
import {
  listWorkspaceCommandEntries,
  type RoleWorkbenchProfile,
  type WorkspaceCommandEntry,
  type WorkspaceNavGroup
} from './sectionWorkspaces';
import { normalizeRoutePath } from './routePath';

type NoticeCategory = 'system' | 'business' | 'error';
type HelpCategory = 'business' | 'technical' | 'faq';

interface ShellPanelContext {
  roleProfile: RoleWorkbenchProfile;
  homePath: string;
  currentPath: string;
  activeGroup: WorkspaceNavGroup;
  allowedPaths: string[];
  activities: ActivityEntry[];
}

interface HelpEntryDefinition {
  id: string;
  category: HelpCategory;
  title: string;
  description: string;
  path?: string;
  audienceKeys?: string[];
  relatedPaths?: string[];
}

const HELP_CATEGORY_META: Record<HelpCategory, { badge: string; tone: ShellPopoverTone }> = {
  business: { badge: '业务类', tone: 'brand' },
  technical: { badge: '技术类', tone: 'accent' },
  faq: { badge: 'FAQ', tone: 'warning' }
};

const NOTICE_CATEGORY_META: Record<NoticeCategory, { badge: string; tone: ShellPopoverTone }> = {
  system: { badge: '系统事件', tone: 'brand' },
  business: { badge: '业务事件', tone: 'accent' },
  error: { badge: '错误事件', tone: 'danger' }
};

const NOTICE_PRIORITY_LABEL: Record<InAppMessagePriority, string> = {
  critical: '紧急',
  high: '高优先',
  medium: '处理中',
  low: '常规'
};

const SYSTEM_ACTIVITY_KEYWORDS = ['角色', '权限', '菜单', '导航', '组织', '区域', '字典', '通知', '审计', '治理'];

const HELP_ENTRY_DEFINITIONS: HelpEntryDefinition[] = [
  {
    id: 'help-products-baseline',
    category: 'business',
    title: '产品建档口径',
    description: '明确 Product Key、协议、节点类型和厂商等接入基线，避免后续建档与联调口径漂移。',
    path: '/products',
    relatedPaths: ['/products', '/devices']
  },
  {
    id: 'help-devices-asset',
    category: 'business',
    title: '设备资产维护手册',
    description: '统一查看设备台账、在线状态、父子拓扑、批量导入和设备更换等日常动作。',
    path: '/devices',
    relatedPaths: ['/devices']
  },
  {
    id: 'help-risk-monitoring-duty',
    category: 'business',
    title: '实时监测值班指引',
    description: '按监测对象、等级和状态快速筛选当前风险态势，优先处理值班视角下的重点对象。',
    path: '/risk-monitoring',
    audienceKeys: ['business', 'manager', 'ops'],
    relatedPaths: ['/risk-monitoring', '/alarm-center', '/event-disposal']
  },
  {
    id: 'help-alarm-operations',
    category: 'business',
    title: '告警确认与关闭说明',
    description: '围绕告警来源、确认、抑制和关闭动作建立统一处理口径，减少处置分歧。',
    path: '/alarm-center',
    audienceKeys: ['business', 'manager', 'ops'],
    relatedPaths: ['/alarm-center', '/event-disposal']
  },
  {
    id: 'help-event-closure',
    category: 'business',
    title: '事件派工闭环手册',
    description: '统一跟踪派发、接收、开始、完成、反馈和关闭动作，保证事件闭环可复盘。',
    path: '/event-disposal',
    audienceKeys: ['business', 'manager', 'ops'],
    relatedPaths: ['/event-disposal']
  },
  {
    id: 'help-report-review',
    category: 'business',
    title: '运营分析解读口径',
    description: '对齐风险趋势、告警统计和事件闭环指标的阅读方式，便于管理复盘与业务汇报。',
    path: '/report-analysis',
    audienceKeys: ['business', 'manager', 'ops', 'super-admin'],
    relatedPaths: ['/report-analysis']
  },
  {
    id: 'help-risk-config',
    category: 'business',
    title: '风险对象与策略配置',
    description: '统一维护风险对象、阈值策略、联动编排与应急预案库，形成稳定的策略底座。',
    path: '/risk-point',
    audienceKeys: ['manager', 'ops', 'developer', 'super-admin'],
    relatedPaths: ['/risk-point', '/rule-definition', '/linkage-rule', '/emergency-plan']
  },
  {
    id: 'help-product-key-check',
    category: 'technical',
    title: '接入身份与协议核对',
    description: '排查 Product Key、协议编码、节点角色与数据格式是否和现场设备契约保持一致。',
    path: '/products',
    relatedPaths: ['/products', '/reporting']
  },
  {
    id: 'help-device-topology-check',
    category: 'technical',
    title: '设备拓扑与在线状态核对',
    description: '从设备资产中心核对父设备、关联网关、在线状态和最近上报时间，减少链路误判。',
    path: '/devices',
    relatedPaths: ['/devices', '/message-trace']
  },
  {
    id: 'help-reporting-link',
    category: 'technical',
    title: 'HTTP / MQTT 联调指引',
    description: '通过链路验证中心按设备编码反查接入契约，执行 HTTP / MQTT 明文或密文模拟上报，并核验格式化、协议识别与主链路解析。',
    path: '/reporting',
    audienceKeys: ['developer', 'ops', 'super-admin'],
    relatedPaths: ['/reporting']
  },
  {
    id: 'help-trace-troubleshooting',
    category: 'technical',
    title: 'TraceId 链路排障',
    description: '按 TraceId、设备编码和 Topic 串联接入链路，快速锁定设备上报和分发问题。',
    path: '/message-trace',
    audienceKeys: ['developer', 'ops', 'super-admin'],
    relatedPaths: ['/message-trace', '/system-log']
  },
  {
    id: 'help-system-error',
    category: 'technical',
    title: '异常观测与 system_error 排查',
    description: '集中查看 MQTT、订阅、消息分发等后台异步异常，缩短故障定位路径。',
    path: '/system-log',
    audienceKeys: ['developer', 'ops', 'super-admin'],
    relatedPaths: ['/system-log']
  },
  {
    id: 'help-channel-routing',
    category: 'technical',
    title: '通知渠道编排说明',
    description: '维护渠道配置、启停状态与测试发送，保证通知链路可用并可核查。',
    path: '/channel',
    audienceKeys: ['manager', 'super-admin'],
    relatedPaths: ['/channel']
  },
  {
    id: 'help-role-permission',
    category: 'technical',
    title: '角色权限配置说明',
    description: '通过角色与菜单授权控制可见页面、按钮权限以及帮助入口范围。',
    path: '/role',
    audienceKeys: ['manager', 'super-admin'],
    relatedPaths: ['/role', '/menu']
  },
  {
    id: 'help-automation-center',
    category: 'technical',
    title: '自动化工场使用说明',
    description: '编排回归计划、视觉基线和浏览器巡检流程，沉淀持续回归资产。',
    path: '/automation-test',
    audienceKeys: ['developer', 'super-admin'],
    relatedPaths: ['/automation-test']
  },
  {
    id: 'faq-home',
    category: 'faq',
    title: '为什么不同角色的首页不一样？',
    description: '首页和推荐入口会按角色职责、默认工作台和菜单授权自动切换，这是当前壳层基线。',
    path: '/'
  },
  {
    id: 'faq-permission',
    category: 'faq',
    title: '为什么我看不到某些页面或帮助条目？',
    description: '帮助中心只展示当前账号有权访问的入口；若缺失，请优先检查角色授权和菜单配置。',
    path: '/role',
    audienceKeys: ['manager', 'super-admin']
  },
  {
    id: 'faq-login',
    category: 'faq',
    title: '为什么访问时会跳回登录页？',
    description: '除白名单外接口默认受 JWT 保护，token 失效或接口返回 401 时会统一清理登录态并跳回登录。',
  },
  {
    id: 'faq-notice',
    category: 'faq',
    title: '为什么通知内容会随角色变化？',
    description: '通知中心会结合角色关注点、菜单权限、最近操作和失败记录聚合站内信息，避免推送无权限入口。'
  }
];

function createEntryMap(allowedPaths: string[]): Map<string, WorkspaceCommandEntry> {
  return new Map(
    listWorkspaceCommandEntries(allowedPaths).map((entry) => [normalizeRoutePath(entry.path), entry])
  );
}

function pathAccessible(path: string | undefined, entryMap: Map<string, WorkspaceCommandEntry>): boolean {
  if (!path) {
    return true;
  }
  return normalizeRoutePath(path) === '/' || entryMap.has(normalizeRoutePath(path));
}

function resolvePrimaryPath(path: string | undefined, fallbackPath: string): string | undefined {
  if (path) {
    return normalizeRoutePath(path);
  }
  return normalizeRoutePath(fallbackPath);
}

function buildItemMeta(parts: Array<string | undefined>): string {
  return parts.map((item) => String(item || '').trim()).filter(Boolean).join(' · ');
}

function dedupeItems(items: ShellPopoverItem[], limit: number): ShellPopoverItem[] {
  const result: ShellPopoverItem[] = [];
  const seenKeys = new Set<string>();

  items.forEach((item) => {
    if (result.length >= limit) {
      return;
    }
    const key = `${item.id}|${item.path || ''}|${item.title}`;
    if (seenKeys.has(key)) {
      return;
    }
    seenKeys.add(key);
    result.push(item);
  });

  return result;
}

function resolveHelpAudienceMatched(definition: HelpEntryDefinition, roleProfile: RoleWorkbenchProfile): boolean {
  if (roleProfile.key === 'super-admin') {
    return true;
  }
  if (!definition.audienceKeys || definition.audienceKeys.length === 0) {
    return true;
  }
  return definition.audienceKeys.includes(roleProfile.key);
}

function resolveHelpScore(
  definition: HelpEntryDefinition,
  context: ShellPanelContext,
  entryMap: Map<string, WorkspaceCommandEntry>
): number {
  let score = 0;
  const normalizedCurrentPath = normalizeRoutePath(context.currentPath);
  const normalizedDefinitionPath = definition.path ? normalizeRoutePath(definition.path) : '';

  if (resolveHelpAudienceMatched(definition, context.roleProfile)) {
    score += 8;
  }
  if (normalizedDefinitionPath && normalizedDefinitionPath === normalizedCurrentPath) {
    score += 10;
  }
  if (definition.relatedPaths?.some((path) => normalizeRoutePath(path) === normalizedCurrentPath)) {
    score += 6;
  }
  if (definition.path) {
    const matchedEntry = entryMap.get(normalizedDefinitionPath);
    if (matchedEntry && matchedEntry.workspaceKey === context.activeGroup.key) {
      score += 4;
    }
    if (context.roleProfile.featuredPaths.some((path) => normalizeRoutePath(path) === normalizedDefinitionPath)) {
      score += 5;
    }
    if (matchedEntry && context.roleProfile.preferredWorkspaceKeys.includes(matchedEntry.workspaceKey)) {
      score += 3;
    }
  }
  return score;
}

function buildHelpItem(
  definition: HelpEntryDefinition,
  entryMap: Map<string, WorkspaceCommandEntry>,
  fallbackPath: string
): ShellPopoverItem {
  const categoryMeta = HELP_CATEGORY_META[definition.category];
  const normalizedPath = resolvePrimaryPath(definition.path, fallbackPath);
  const entry = definition.path ? entryMap.get(normalizedPath || '') : undefined;

  return {
    id: definition.id,
    title: definition.title,
    description: truncateText(definition.description, 88),
    meta: buildItemMeta([
      entry?.workspaceLabel,
      definition.path ? '进入对应功能页' : '站内说明'
    ]),
    badge: categoryMeta.badge,
    path: definition.path ? normalizedPath : undefined,
    tone: categoryMeta.tone,
    actionLabel: '查看详情',
    categoryKey: definition.category
  };
}

function buildHelpFallbackSection(
  category: HelpCategory,
  context: ShellPanelContext
): ShellPopoverSection {
  const categoryMeta = HELP_CATEGORY_META[category];

  if (category === 'business') {
    return {
      id: 'help-business-fallback',
      title: '业务功能类',
      description: '按角色职责整理当前工作台的业务帮助入口。',
      items: [
        {
          id: 'help-business-home',
          title: `当前优先进入 ${context.activeGroup.label}`,
          description: truncateText(context.roleProfile.focusDescription, 88),
          meta: buildItemMeta([context.roleProfile.label, '角色推荐']),
          badge: categoryMeta.badge,
          path: normalizeRoutePath(context.homePath),
          tone: categoryMeta.tone,
          actionLabel: '查看详情',
          categoryKey: category
        }
      ]
    };
  }

  if (category === 'technical') {
    return {
      id: 'help-technical-fallback',
      title: '技术类',
      description: '优先给出当前可访问范围内的技术核查线索。',
      items: [
        {
          id: 'help-technical-current',
          title: '当前页操作说明',
          description: '当前账号暂无更多技术帮助入口，建议先从当前页面和角色推荐工作台开始核查。',
          meta: buildItemMeta([context.activeGroup.label, '权限内兜底']),
          badge: categoryMeta.badge,
          path: normalizeRoutePath(context.currentPath || context.homePath),
          tone: categoryMeta.tone,
          actionLabel: '查看详情',
          categoryKey: category
        }
      ]
    };
  }

  return {
    id: 'help-faq-fallback',
    title: '常见问题',
    description: '保留统一问题口径，减少不同角色对同一规则的理解偏差。',
    items: [
      {
        id: 'help-faq-basic',
        title: '为什么帮助中心没有某条入口？',
        description: '帮助条目默认按当前菜单授权过滤，不会向无权限账号展示不可进入的功能入口。',
        meta: buildItemMeta([context.roleProfile.label, '统一口径']),
        badge: categoryMeta.badge,
        tone: categoryMeta.tone,
        actionLabel: '查看详情',
        categoryKey: category
      }
    ]
  };
}

function buildHelpSections(
  context: ShellPanelContext,
  entryMap: Map<string, WorkspaceCommandEntry>
): { sections: ShellPopoverSection[]; accessibleCount: number; faqCount: number } {
  const accessibleDefinitions = HELP_ENTRY_DEFINITIONS
    .filter((definition) => resolveHelpAudienceMatched(definition, context.roleProfile))
    .filter((definition) => pathAccessible(definition.path, entryMap))
    .sort((left, right) => resolveHelpScore(right, context, entryMap) - resolveHelpScore(left, context, entryMap));

  const categoryDefinitions = {
    business: accessibleDefinitions.filter((definition) => definition.category === 'business'),
    technical: accessibleDefinitions.filter((definition) => definition.category === 'technical'),
    faq: accessibleDefinitions.filter((definition) => definition.category === 'faq')
  };

  const sections: ShellPopoverSection[] = (['business', 'technical', 'faq'] as HelpCategory[]).map((category) => {
    const definitions = categoryDefinitions[category].slice(0, 2);
    if (definitions.length === 0) {
      return buildHelpFallbackSection(category, context);
    }

    return {
      id: `help-${category}`,
      title: category === 'business' ? '业务功能类' : category === 'technical' ? '技术类' : '常见问题',
      description: category === 'business'
        ? '围绕当前角色最常用的业务流程与工作台入口组织。'
        : category === 'technical'
          ? '聚焦联调、排障、权限和通知编排等技术线索。'
          : '沉淀统一答复，减少常见问题反复确认。',
      items: definitions.map((definition) => buildHelpItem(definition, entryMap, context.homePath))
    };
  });

  return {
    sections,
    accessibleCount: accessibleDefinitions.length,
    faqCount: categoryDefinitions.faq.length
  };
}

function classifyActivity(entry: ActivityEntry, entryMap: Map<string, WorkspaceCommandEntry>): NoticeCategory {
  const combinedText = `${entry.title} ${entry.detail} ${entry.module || ''} ${entry.action || ''}`.toLowerCase();
  if (!entry.ok || /失败|异常|error|exception|timeout|失败记录/.test(combinedText)) {
    return 'error';
  }

  const normalizedPath = entry.path ? normalizeRoutePath(entry.path) : '';
  const workspaceEntry = normalizedPath ? entryMap.get(normalizedPath) : undefined;
  if (workspaceEntry?.workspaceKey === 'system-governance') {
    return 'system';
  }

  const systemMatched = SYSTEM_ACTIVITY_KEYWORDS.some((keyword) => combinedText.includes(keyword.toLowerCase()));
  return systemMatched ? 'system' : 'business';
}

function buildActivityNoticeItem(
  entry: ActivityEntry,
  category: NoticeCategory,
  entryMap: Map<string, WorkspaceCommandEntry>,
  fallbackPath: string
): ShellPopoverItem {
  const normalizedPath = entry.path ? normalizeRoutePath(entry.path) : '';
  const workspaceEntry = normalizedPath ? entryMap.get(normalizedPath) : undefined;
  const categoryMeta = NOTICE_CATEGORY_META[category];
  const preferredPath = workspaceEntry?.path || resolvePrimaryPath(entry.path, fallbackPath);

  return {
    id: `notice-activity-${entry.id}`,
    title: truncateText(entry.title || '最近操作', 32),
    description: truncateText(entry.detail || workspaceEntry?.description || '最近操作已写入站内消息，可进入相关页面继续查看。', 88),
    meta: buildItemMeta([
      workspaceEntry?.workspaceLabel || entry.tag,
      formatDateTime(entry.createdAt)
    ]),
    badge: entry.ok ? '最近操作' : '失败记录',
    path: preferredPath,
    tone: entry.ok ? categoryMeta.tone : 'danger',
    actionLabel: '查看详情',
    categoryKey: category
  };
}

function resolveRemoteNoticeTone(category: NoticeCategory, priority: InAppMessagePriority): ShellPopoverTone {
  if (category === 'error' || priority === 'critical') {
    return 'danger';
  }
  if (priority === 'high') {
    return 'warning';
  }
  return NOTICE_CATEGORY_META[category].tone;
}

function resolveRemoteNoticeWeight(priority: InAppMessagePriority): number {
  switch (priority) {
    case 'critical':
      return 4;
    case 'high':
      return 3;
    case 'medium':
      return 2;
    case 'low':
      return 1;
    default:
      return 0;
  }
}

function sortRemoteNoticeMessages(messages: InAppMessageAccessRecord[]): InAppMessageAccessRecord[] {
  return [...messages].sort((left, right) => {
    const weightDiff = resolveRemoteNoticeWeight(right.priority) - resolveRemoteNoticeWeight(left.priority)
    if (weightDiff !== 0) {
      return weightDiff
    }
    return String(right.publishTime || '').localeCompare(String(left.publishTime || ''))
  })
}

function buildRemoteNoticeItem(
  message: InAppMessageAccessRecord,
  entryMap: Map<string, WorkspaceCommandEntry>
): ShellPopoverItem {
  const category = (message.messageType || 'business') as NoticeCategory
  const normalizedPath = message.relatedPath ? normalizeRoutePath(message.relatedPath) : ''
  const workspaceEntry = normalizedPath ? entryMap.get(normalizedPath) : undefined
  const accessiblePath = normalizedPath && pathAccessible(normalizedPath, entryMap) ? normalizedPath : undefined
  return {
    id: `notice-remote-${message.id}`,
    title: truncateText(message.title || '站内消息', 34),
    description: truncateText(message.summary || message.content || '站内消息已同步到通知中心。', 88),
    meta: buildItemMeta([
      workspaceEntry?.workspaceLabel || message.sourceType || '站内消息',
      message.publishTime ? formatDateTime(message.publishTime) : '刚刚更新'
    ]),
    badge: NOTICE_PRIORITY_LABEL[message.priority] || NOTICE_PRIORITY_LABEL.medium,
    path: accessiblePath,
    tone: resolveRemoteNoticeTone(category, message.priority || 'medium'),
    actionLabel: '查看详情',
    categoryKey: category,
    resourceId: String(message.id),
    read: Boolean(message.read)
  }
}

function resolveRemoteHelpPath(
  document: HelpDocumentAccessRecord,
  entryMap: Map<string, WorkspaceCommandEntry>,
  currentPath: string
): string | undefined {
  const relatedPaths = (document.relatedPathList || []).map((path) => normalizeRoutePath(path))
  const normalizedCurrentPath = normalizeRoutePath(currentPath)
  const currentMatchedPath = relatedPaths.find((path) => path === normalizedCurrentPath && pathAccessible(path, entryMap))
  if (currentMatchedPath) {
    return currentMatchedPath
  }
  return relatedPaths.find((path) => pathAccessible(path, entryMap))
}

function buildRemoteHelpItem(
  document: HelpDocumentAccessRecord,
  category: HelpCategory,
  entryMap: Map<string, WorkspaceCommandEntry>,
  currentPath: string
): ShellPopoverItem {
  const primaryPath = resolveRemoteHelpPath(document, entryMap, currentPath)
  const workspaceEntry = primaryPath ? entryMap.get(primaryPath) : undefined
  const keywords = (document.keywordList || []).slice(0, 2).join(' / ')
  return {
    id: `help-remote-${document.id}`,
    title: document.title,
    description: truncateText(document.summary || document.content || '权限内帮助资料已同步。', 88),
    meta: buildItemMeta([
      workspaceEntry?.workspaceLabel,
      document.currentPathMatched ? '当前页相关' : '权限内资料',
      keywords
    ]),
    badge: HELP_CATEGORY_META[category].badge,
    path: primaryPath,
    tone: HELP_CATEGORY_META[category].tone,
    actionLabel: '查看详情',
    categoryKey: category,
    resourceId: String(document.id)
  }
}

function buildSystemNoticeFallbacks(
  context: ShellPanelContext,
  entryMap: Map<string, WorkspaceCommandEntry>
): ShellPopoverItem[] {
  const homePath = normalizeRoutePath(context.homePath);
  const homeEntry = entryMap.get(homePath);
  const governancePath = ['/channel', '/role', '/audit-log', '/menu']
    .map((path) => normalizeRoutePath(path))
    .find((path) => pathAccessible(path, entryMap));

  const governanceItem: ShellPopoverItem | undefined = governancePath
    ? {
        id: `notice-system-governance-${governancePath}`,
        title: governancePath === '/channel'
          ? '通知编排决定渠道配置与测试链路'
          : governancePath === '/role'
            ? '角色授权决定入口与帮助可见范围'
            : governancePath === '/audit-log'
              ? '审计中心用于回看治理侧关键操作'
              : '导航编排会影响头部入口的权限可见性',
        description: governancePath === '/channel'
          ? '站内消息之外的渠道测试与启停状态，统一在通知编排中维护。'
          : governancePath === '/role'
            ? '当前通知和帮助条目已按菜单授权过滤，不再展示无权限入口。'
            : governancePath === '/audit-log'
              ? '当治理侧出现变更争议时，优先回看审计中心中的关键操作记录。'
              : '菜单元数据变更后，命令面板、帮助推荐和工作台入口会同步收口。',
        meta: buildItemMeta([context.roleProfile.label, '治理提醒']),
        badge: '角色推送',
        path: governancePath,
        tone: 'brand',
        actionLabel: '查看详情',
        categoryKey: 'system'
      }
    : {
        id: 'notice-system-permission-filtered',
        title: '当前通知与帮助内容已按权限过滤',
        description: '当前账号只会看到有权访问的功能入口，避免展示无权限的跳转目标。',
        meta: buildItemMeta([context.roleProfile.label, '壳层规则']),
        badge: '系统规则',
        tone: 'brand',
        actionLabel: '查看详情',
        categoryKey: 'system'
      };

  return [
    {
      id: 'notice-system-home',
      title: `当前首页已按 ${context.roleProfile.label} 收口`,
      description: truncateText(context.roleProfile.focusDescription, 88),
      meta: buildItemMeta([homeEntry?.title || '平台首页', '角色焦点']),
      badge: '角色推送',
      path: homePath,
      tone: 'brand',
      actionLabel: '查看详情',
      categoryKey: 'system'
    },
    governanceItem
  ];
}

function buildBusinessNoticeFallbacks(
  context: ShellPanelContext,
  entryMap: Map<string, WorkspaceCommandEntry>
): ShellPopoverItem[] {
  const normalizedCurrentPath = normalizeRoutePath(context.currentPath || context.homePath);
  const currentEntry = entryMap.get(normalizedCurrentPath);
  const featuredItems = context.roleProfile.featuredPaths
    .map((path) => entryMap.get(normalizeRoutePath(path)))
    .filter((entry): entry is WorkspaceCommandEntry => Boolean(entry))
    .slice(0, 2)
    .map((entry, index) => ({
      id: `notice-business-featured-${index}-${entry.path}`,
      title: `重点关注：${entry.title}`,
      description: truncateText(entry.description, 88),
      meta: buildItemMeta([entry.workspaceLabel, context.roleProfile.label]),
      badge: '角色推送',
      path: entry.path,
      tone: 'accent' as const,
      actionLabel: '查看详情',
      categoryKey: 'business'
    }));

  const currentWorkspaceItem: ShellPopoverItem = {
    id: 'notice-business-current',
    title: `当前位于 ${context.activeGroup.label}`,
    description: truncateText(
      currentEntry?.description || '当前工作台已切换到本角色的主工作区，可优先从这里推进当前任务。',
      88
    ),
    meta: buildItemMeta([currentEntry?.title || context.activeGroup.label, '当前工作台']),
    badge: '当前焦点',
    path: normalizedCurrentPath,
    tone: 'accent',
    actionLabel: '查看详情',
    categoryKey: 'business'
  };

  return [currentWorkspaceItem, ...featuredItems];
}

function buildErrorNoticeFallback(context: ShellPanelContext, entryMap: Map<string, WorkspaceCommandEntry>): ShellPopoverItem {
  const fallbackPath = ['/system-log', '/message-trace', '/audit-log', '/alarm-center']
    .map((path) => normalizeRoutePath(path))
    .find((path) => pathAccessible(path, entryMap));

  if (fallbackPath) {
    return {
      id: `notice-error-fallback-${fallbackPath}`,
      title: fallbackPath === '/system-log'
        ? '异常观测台用于查看 system_error'
        : fallbackPath === '/message-trace'
          ? '链路追踪台可串联 TraceId 排障'
          : fallbackPath === '/audit-log'
            ? '审计中心可复盘治理侧失败操作'
            : '告警运营台可继续核查风险处置异常',
      description: fallbackPath === '/system-log'
        ? '当前暂无新的失败记录，可先关注后台异常池，确认是否存在 MQTT、订阅或分发故障。'
        : fallbackPath === '/message-trace'
          ? '当前暂无新的失败记录，可按 TraceId、设备编码和 Topic 继续做链路核查。'
          : fallbackPath === '/audit-log'
            ? '当前暂无新的失败记录，可优先复盘治理侧关键操作与权限调整记录。'
            : '当前暂无新的失败记录，可继续从告警与事件闭环状态观察潜在异常。',
      meta: buildItemMeta([context.roleProfile.label, '排障入口']),
      badge: '排障建议',
      path: fallbackPath,
      tone: 'danger',
      actionLabel: '查看详情',
      categoryKey: 'error'
    };
  }

  return {
    id: 'notice-error-empty',
    title: '当前暂无新的错误事件',
    description: '最近操作中未识别到新的失败记录，建议继续关注当前工作台的业务推进和异常提示。',
    meta: buildItemMeta([context.activeGroup.label, '排障兜底']),
    badge: '状态稳定',
    tone: 'success',
    actionLabel: '查看详情',
    categoryKey: 'error'
  };
}

function buildNoticeSections(
  context: ShellPanelContext,
  entryMap: Map<string, WorkspaceCommandEntry>
): { sections: ShellPopoverSection[]; failureCount: number } {
  const categorizedActivities = {
    system: [] as ActivityEntry[],
    business: [] as ActivityEntry[],
    error: [] as ActivityEntry[]
  };

  context.activities.forEach((entry) => {
    const category = classifyActivity(entry, entryMap);
    categorizedActivities[category].push(entry);
  });

  const systemItems = dedupeItems(
    [
      ...categorizedActivities.system.slice(0, 1).map((entry) =>
        buildActivityNoticeItem(entry, 'system', entryMap, context.homePath)
      ),
      ...buildSystemNoticeFallbacks(context, entryMap)
    ],
    2
  );

  const businessItems = dedupeItems(
    [
      ...categorizedActivities.business.slice(0, 1).map((entry) =>
        buildActivityNoticeItem(entry, 'business', entryMap, context.homePath)
      ),
      ...buildBusinessNoticeFallbacks(context, entryMap)
    ],
    2
  );

  const errorItems = dedupeItems(
    categorizedActivities.error.slice(0, 2).map((entry) =>
      buildActivityNoticeItem(entry, 'error', entryMap, context.homePath)
    ),
    2
  );

  if (errorItems.length === 0) {
    errorItems.push(buildErrorNoticeFallback(context, entryMap));
  }

  return {
    sections: [
      {
        id: 'notice-system',
        title: '系统事件',
        description: '聚合权限、导航、通知配置和壳层规则相关提醒。',
        items: systemItems
      },
      {
        id: 'notice-business',
        title: '业务事件',
        description: '结合角色关注路径和当前工作台，聚合本角色优先处理的业务消息。',
        items: businessItems
      },
      {
        id: 'notice-error',
        title: '错误事件',
        description: '统一展示失败记录、异常排障入口和兜底建议。',
        items: errorItems
      }
    ],
    failureCount: categorizedActivities.error.length
  };
}

export function buildShellNoticePopoverContent(context: ShellPanelContext): ShellPopoverContent {
  const entryMap = createEntryMap(context.allowedPaths);
  const { sections, failureCount } = buildNoticeSections(context, entryMap);

  return {
    title: '通知中心',
    subtitle: '按角色、工作台和最近操作聚合站内消息',
    summaryTitle: `${context.roleProfile.label} 站内消息已就绪`,
    summaryDescription: '通知中心会优先展示当前账号真正有权访问的系统、业务和错误事件，不再堆叠无权限入口。',
    metrics: [
      { id: 'notice-role-focus', label: '角色焦点', value: context.roleProfile.focusLabel, tone: 'brand' },
      { id: 'notice-workspace', label: '当前工作台', value: context.activeGroup.label, tone: 'accent' },
      { id: 'notice-recent', label: '最近操作', value: `${context.activities.length}`, tone: 'neutral' },
      {
        id: 'notice-failure',
        label: '失败记录',
        value: `${failureCount}`,
        tone: failureCount > 0 ? 'danger' : 'success'
      }
    ],
    sections,
    footerActions: [
      { id: 'notice-view-more', label: '查看更多', tone: 'brand' }
    ]
  };
}

export function buildShellNoticePopoverContentFromApi(
  context: ShellPanelContext,
  messages: InAppMessageAccessRecord[],
  unreadStats?: InAppMessageUnreadStats | null
): ShellPopoverContent {
  const entryMap = createEntryMap(context.allowedPaths)
  const sortedMessages = sortRemoteNoticeMessages(messages)
  const categoryMessages: Record<NoticeCategory, InAppMessageAccessRecord[]> = {
    system: sortedMessages.filter((message) => message.messageType === 'system'),
    business: sortedMessages.filter((message) => message.messageType === 'business'),
    error: sortedMessages.filter((message) => message.messageType === 'error')
  }

  const systemItems = categoryMessages.system.length > 0
    ? categoryMessages.system.slice(0, 2).map((message) => buildRemoteNoticeItem(message, entryMap))
    : buildSystemNoticeFallbacks(context, entryMap)

  const businessItems = categoryMessages.business.length > 0
    ? categoryMessages.business.slice(0, 2).map((message) => buildRemoteNoticeItem(message, entryMap))
    : buildBusinessNoticeFallbacks(context, entryMap)

  const errorItems = categoryMessages.error.length > 0
    ? categoryMessages.error.slice(0, 2).map((message) => buildRemoteNoticeItem(message, entryMap))
    : [buildErrorNoticeFallback(context, entryMap)]

  const totalUnreadCount = unreadStats?.totalUnreadCount
    ?? sortedMessages.filter((message) => !message.read).length
  const errorUnreadCount = unreadStats?.errorUnreadCount
    ?? categoryMessages.error.filter((message) => !message.read).length

  return {
    title: '通知中心',
    subtitle: '已接入站内消息接口，按角色与范围同步真实消息',
    summaryTitle: `${context.roleProfile.label} 站内消息已同步`,
    summaryDescription: '通知中心当前优先展示系统治理域下的真实站内消息，并继续保留角色聚焦、工作台上下文和权限过滤规则。',
    metrics: [
      { id: 'notice-role-focus', label: '角色焦点', value: context.roleProfile.focusLabel, tone: 'brand' },
      { id: 'notice-workspace', label: '当前工作台', value: context.activeGroup.label, tone: 'accent' },
      { id: 'notice-unread', label: '未读消息', value: `${totalUnreadCount}`, tone: totalUnreadCount > 0 ? 'warning' : 'success' },
      { id: 'notice-error-unread', label: '错误未读', value: `${errorUnreadCount}`, tone: errorUnreadCount > 0 ? 'danger' : 'success' }
    ],
    sections: [
      {
        id: 'notice-system',
        title: '系统事件',
        description: '同步系统治理、维护窗口和平台规则类消息。',
        items: systemItems
      },
      {
        id: 'notice-business',
        title: '业务事件',
        description: '同步业务提醒、待办推进和角色关注事项。',
        items: businessItems
      },
      {
        id: 'notice-error',
        title: '错误事件',
        description: '同步错误告警、排障提醒和异常复核入口。',
        items: errorItems
      }
    ],
    footerActions: [
      { id: 'notice-view-more', label: '查看更多', tone: 'brand' },
      { id: 'notice-mark-all-read', label: '全部已读', tone: 'warning', disabled: totalUnreadCount <= 0 }
    ]
  }
}

export function buildShellHelpPopoverContent(context: ShellPanelContext): ShellPopoverContent {
  const entryMap = createEntryMap(context.allowedPaths);
  const { sections, accessibleCount, faqCount } = buildHelpSections(context, entryMap);

  return {
    title: '帮助中心',
    subtitle: '按权限筛出业务、技术和常见问题资料',
    summaryTitle: '权限内帮助资料已自动整理',
    summaryDescription: `帮助中心会优先推荐与 ${context.activeGroup.label}、当前页面和角色关注路径相关的内容，避免把无权限入口混入帮助文档。`,
    metrics: [
      { id: 'help-role', label: '当前角色', value: context.roleProfile.label, tone: 'brand' },
      { id: 'help-docs', label: '可见资料', value: `${accessibleCount}`, tone: 'accent' },
      { id: 'help-workspace', label: '当前工作台', value: context.activeGroup.label, tone: 'success' },
      { id: 'help-faq', label: '常见问题', value: `${Math.max(faqCount, 1)}`, tone: 'warning' }
    ],
    sections,
    footerActions: [
      { id: 'help-view-more', label: '查看更多', tone: 'brand' }
    ]
  };
}

export function buildShellHelpPopoverContentFromApi(
  context: ShellPanelContext,
  documents: HelpDocumentAccessRecord[]
): ShellPopoverContent {
  const entryMap = createEntryMap(context.allowedPaths)
  const sortedDocuments = [...documents].sort((left, right) => {
    const pathMatchDiff = Number(Boolean(right.currentPathMatched)) - Number(Boolean(left.currentPathMatched))
    if (pathMatchDiff !== 0) {
      return pathMatchDiff
    }
    const sortDiff = Number(left.sortNo || 0) - Number(right.sortNo || 0)
    if (sortDiff !== 0) {
      return sortDiff
    }
    return String(left.title || '').localeCompare(String(right.title || ''))
  })

  const categoryDocuments: Record<HelpCategory, HelpDocumentAccessRecord[]> = {
    business: sortedDocuments.filter((document) => document.docCategory === 'business'),
    technical: sortedDocuments.filter((document) => document.docCategory === 'technical'),
    faq: sortedDocuments.filter((document) => document.docCategory === 'faq')
  }

  const sections: ShellPopoverSection[] = (['business', 'technical', 'faq'] as HelpCategory[]).map((category) => {
    const documentsInCategory = categoryDocuments[category].slice(0, 2)
    if (documentsInCategory.length === 0) {
      return buildHelpFallbackSection(category, context)
    }
    return {
      id: `help-${category}`,
      title: category === 'business' ? '业务功能类' : category === 'technical' ? '技术类' : '常见问题',
      description: category === 'business'
        ? '同步业务流程、运营闭环和角色常用资料。'
        : category === 'technical'
          ? '同步联调、排障、权限与治理相关资料。'
          : '同步 FAQ 与统一答复口径。',
      items: documentsInCategory.map((document) => buildRemoteHelpItem(document, category, entryMap, context.currentPath))
    }
  })

  const faqCount = categoryDocuments.faq.length

  return {
    title: '帮助中心',
    subtitle: '已接入帮助文档接口，按角色与菜单权限同步资料',
    summaryTitle: '权限内帮助资料已同步',
    summaryDescription: `帮助中心当前优先展示与 ${context.activeGroup.label} 和当前页面相关的真实帮助文档；若某类资料暂未配置，仍会保留壳层兜底说明。`,
    metrics: [
      { id: 'help-role', label: '当前角色', value: context.roleProfile.label, tone: 'brand' },
      { id: 'help-docs', label: '可见资料', value: `${sortedDocuments.length}`, tone: 'accent' },
      { id: 'help-workspace', label: '当前工作台', value: context.activeGroup.label, tone: 'success' },
      { id: 'help-faq', label: '常见问题', value: `${Math.max(faqCount, 1)}`, tone: 'warning' }
    ],
    sections,
    footerActions: [
      { id: 'help-view-more', label: '查看更多', tone: 'brand' }
    ]
  }
}
