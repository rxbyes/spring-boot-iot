import { describe, expect, it } from 'vitest';

import type { ActivityEntry } from '@/types/api';
import {
  buildShellHelpPopoverContentFromApi,
  buildShellHelpPopoverContent,
  buildShellNoticePopoverContentFromApi,
  buildShellNoticePopoverContent
} from '@/utils/shellPanelContent';
import type { RoleWorkbenchProfile, WorkspaceNavGroup } from '@/utils/sectionWorkspaces';

function createRoleProfile(partial?: Partial<RoleWorkbenchProfile>): RoleWorkbenchProfile {
  return {
    key: 'business',
    label: '业务人员',
    roleCodes: ['BUSINESS_STAFF'],
    roleNameKeywords: ['业务'],
    defaultPath: '/risk-disposal',
    preferredWorkspaceKeys: ['risk-ops', 'risk-config'],
    featuredPaths: ['/risk-monitoring', '/alarm-center', '/event-disposal', '/report-analysis'],
    cockpitRole: 'frontline',
    focusLabel: '风险运营',
    focusDescription: '优先关注实时监测、告警研判、事件闭环与运营复盘。',
    ...partial
  };
}

function createActiveGroup(partial?: Partial<WorkspaceNavGroup>): WorkspaceNavGroup {
  return {
    key: 'risk-ops',
    label: '风险运营',
    description: 'desc',
    menuTitle: '风险运营',
    menuHint: 'hint',
    items: [
      { to: '/risk-monitoring', label: '实时监测台', caption: 'caption', short: '监' },
      { to: '/alarm-center', label: '告警运营台', caption: 'caption', short: '告' }
    ],
    ...partial
  };
}

describe('shellPanelContent', () => {
  it('filters help entries by current role and allowed paths', () => {
    const content = buildShellHelpPopoverContent({
      roleProfile: createRoleProfile(),
      homePath: '/risk-disposal',
      currentPath: '/alarm-center',
      activeGroup: createActiveGroup(),
      allowedPaths: ['/risk-monitoring', '/alarm-center', '/event-disposal', '/report-analysis', '/products', '/devices'],
      activities: []
    });

    const itemPaths = content.sections.flatMap((section) => section.items.map((item) => item.path).filter(Boolean));

    expect(content.title).toBe('帮助中心');
    expect(content.sections).toHaveLength(3);
    expect(content.footerActions?.map((action) => action.id)).toEqual(['help-view-more']);
    expect(itemPaths).not.toContain('/role');
    expect(itemPaths).not.toContain('/automation-test');
    expect(itemPaths).toContain('/alarm-center');
    expect(itemPaths).toContain('/products');
  });

  it('routes quality help entries through the overview and consolidated governance page', () => {
    const content = buildShellHelpPopoverContent({
      roleProfile: createRoleProfile({
        key: 'developer',
        label: '开发人员',
        roleCodes: ['DEVELOPER_STAFF'],
        roleNameKeywords: ['开发'],
        defaultPath: '/device-access',
        preferredWorkspaceKeys: ['iot-access', 'risk-config', 'quality-workbench'],
        featuredPaths: ['/reporting', '/system-log', '/message-trace', '/quality-workbench'],
        cockpitRole: 'rd',
        focusLabel: '链路与质量',
        focusDescription: '优先联调接入链路、异常观测、消息追踪和自动化回归。'
      }),
      homePath: '/quality-workbench',
      currentPath: '/quality-workbench',
      activeGroup: createActiveGroup({
        key: 'quality-workbench',
        label: '质量工场',
        items: [
          { to: '/business-acceptance', label: '业务验收台', caption: 'caption', short: '验' },
          { to: '/automation-governance', label: '自动化治理台', caption: 'caption', short: '治' }
        ]
      }),
      allowedPaths: ['/quality-workbench', '/business-acceptance', '/automation-governance', '/reporting'],
      activities: []
    });

    const itemPaths = content.sections.flatMap((section) => section.items.map((item) => item.path).filter(Boolean));
    const technicalSection = content.sections.find((section) => section.id === 'help-technical');

    expect(itemPaths).toContain('/quality-workbench');
    expect(itemPaths).not.toContain('/automation-test');
    expect(technicalSection?.items.map((item) => item.title)).toContain('HTTP / MQTT 联调指引');
    expect(technicalSection?.items.map((item) => item.description).join(' ')).toContain('排障起点');
    expect(technicalSection?.items.map((item) => item.description).join(' ')).toContain('自动化治理台');
  });

  it('places failed activities into the error notice section and keeps all notice sections', () => {
    const activities: ActivityEntry[] = [
      {
        id: 'activity-1',
        title: '链路验证失败',
        detail: 'HTTP 上报返回 500，需继续排查',
        createdAt: '2026-03-21T08:00:00Z',
        ok: false,
        path: '/reporting'
      },
      {
        id: 'activity-2',
        title: '告警列表已刷新',
        detail: '已查看最新告警队列',
        createdAt: '2026-03-21T09:00:00Z',
        ok: true,
        path: '/alarm-center'
      }
    ];

    const content = buildShellNoticePopoverContent({
      roleProfile: createRoleProfile({
        key: 'developer',
        label: '开发人员',
        roleCodes: ['DEVELOPER_STAFF'],
        roleNameKeywords: ['开发'],
        defaultPath: '/device-access',
        preferredWorkspaceKeys: ['iot-access', 'risk-config', 'quality-workbench'],
        featuredPaths: ['/reporting', '/system-log', '/message-trace', '/automation-governance'],
        cockpitRole: 'rd',
        focusLabel: '链路与质量',
        focusDescription: '优先联调接入链路、异常观测、消息追踪和自动化回归。'
      }),
      homePath: '/device-access',
      currentPath: '/reporting',
      activeGroup: createActiveGroup({
        key: 'iot-access',
        label: '接入智维',
        items: [
          { to: '/reporting', label: '链路验证中心', caption: 'caption', short: '验' },
          { to: '/system-log', label: '异常观测台', caption: 'caption', short: '观' }
        ]
      }),
      allowedPaths: ['/reporting', '/system-log', '/message-trace', '/automation-governance', '/devices'],
      activities
    });

    const errorSection = content.sections.find((section) => section.id === 'notice-error');

    expect(content.sections.map((section) => section.id)).toEqual(['notice-system', 'notice-business', 'notice-error']);
    expect(errorSection?.items.some((item) => item.title.includes('链路验证失败'))).toBe(true);
    expect(content.metrics.find((metric) => metric.id === 'notice-failure')?.value).toBe('1');
    expect(content.footerActions?.map((action) => action.id)).toEqual(['notice-view-more']);
  });

  it('maps remote notice messages into shell sections and unread metrics', () => {
    const content = buildShellNoticePopoverContentFromApi({
      roleProfile: createRoleProfile({
        key: 'ops',
        label: '运维人员',
        roleCodes: ['OPS_STAFF'],
        roleNameKeywords: ['运维'],
        defaultPath: '/device-access',
        preferredWorkspaceKeys: ['iot-access', 'risk-ops'],
        featuredPaths: ['/devices', '/message-trace', '/system-log'],
        cockpitRole: 'ops',
        focusLabel: '接入智维',
        focusDescription: '优先处理链路稳定性、设备在线与排障事项。'
      }),
      homePath: '/device-access',
      currentPath: '/message-trace',
      activeGroup: createActiveGroup({
        key: 'iot-access',
        label: '接入智维',
        items: [
          { to: '/devices', label: '设备资产中心', caption: 'caption', short: '设' },
          { to: '/message-trace', label: '链路追踪台', caption: 'caption', short: '追' }
        ]
      }),
      allowedPaths: ['/devices', '/message-trace', '/system-log'],
      activities: []
    }, [
      {
        id: '1',
        messageType: 'system',
        priority: 'critical',
        title: '系统维护窗口提醒',
        summary: '今晚 23:00 执行维护',
        relatedPath: '/system-log',
        read: false,
        publishTime: '2026-03-21T10:00:00Z'
      },
      {
        id: '2',
        messageType: 'error',
        priority: 'high',
        title: '接入链路异常排查提示',
        summary: '最近 30 分钟存在分发失败',
        relatedPath: '/message-trace',
        read: false,
        publishTime: '2026-03-21T11:00:00Z'
      }
    ], {
      totalUnreadCount: 2,
      systemUnreadCount: 1,
      businessUnreadCount: 0,
      errorUnreadCount: 1
    });

    expect(content.sections.map((section) => section.id)).toEqual(['notice-system', 'notice-business', 'notice-error']);
    expect(content.sections.find((section) => section.id === 'notice-system')?.items[0].title).toContain('系统维护窗口提醒');
    expect(content.sections.find((section) => section.id === 'notice-error')?.items[0].path).toBe('/message-trace');
    expect(content.metrics.find((metric) => metric.id === 'notice-unread')?.value).toBe('2');
    expect(content.metrics.find((metric) => metric.id === 'notice-error-unread')?.value).toBe('1');
    expect(content.footerActions?.map((action) => action.id)).toEqual(['notice-view-more', 'notice-mark-all-read']);
    expect(content.sections.find((section) => section.id === 'notice-error')?.items[0].resourceId).toBe('2');
  });

  it('maps remote help documents into shell sections and keeps current-path priority', () => {
    const content = buildShellHelpPopoverContentFromApi({
      roleProfile: createRoleProfile(),
      homePath: '/risk-disposal',
      currentPath: '/alarm-center',
      activeGroup: createActiveGroup(),
      allowedPaths: ['/alarm-center', '/event-disposal', '/products', '/devices'],
      activities: []
    }, [
      {
        id: '10',
        docCategory: 'business',
        sortNo: 1,
        title: '告警运营与事件协同业务手册',
        summary: '面向业务角色的闭环指引',
        currentPathMatched: true,
        keywordList: ['告警', '事件'],
        relatedPathList: ['/alarm-center', '/event-disposal']
      },
      {
        id: '11',
        docCategory: 'faq',
        sortNo: 2,
        title: '产品与设备建档 FAQ',
        summary: '统一解释产品与设备关系',
        currentPathMatched: false,
        keywordList: ['产品', '设备'],
        relatedPathList: ['/products', '/devices']
      }
    ]);

    expect(content.sections.map((section) => section.id)).toEqual(['help-business', 'help-technical-fallback', 'help-faq']);
    expect(content.sections.find((section) => section.id === 'help-business')?.items[0].path).toBe('/alarm-center');
    expect(content.sections.find((section) => section.id === 'help-faq')?.items[0].path).toBe('/products');
    expect(content.metrics.find((metric) => metric.id === 'help-docs')?.value).toBe('2');
    expect(content.footerActions?.map((action) => action.id)).toEqual(['help-view-more']);
    expect(content.sections.find((section) => section.id === 'help-business')?.items[0].resourceId).toBe('10');
  });
});
