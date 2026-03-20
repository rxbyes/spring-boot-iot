import { describe, expect, it } from 'vitest';

import {
  canAccessSectionHome,
  createSectionHomeNavItem,
  getRouteMetaPreset,
  getSectionHomeConfigByPath,
  listStaticNavigationGroups,
  matchSectionCardActivity,
  pickSectionActivities,
  resolveRoleHomePath,
  resolveRoleWorkbenchProfile,
  resolveSectionHomeConfig
} from '@/utils/sectionWorkspaces';

describe('sectionHomes config', () => {
  it('resolves section home config by group key and label', () => {
    expect(resolveSectionHomeConfig('system-governance', '系统治理')?.path).toBe('/system-management');
    expect(resolveSectionHomeConfig('risk-ops', '预警处置')?.navLabel).toBe('运营总览');
  });

  it('creates overview navigation item for known group', () => {
    expect(createSectionHomeNavItem('iot-access', '设备接入')).toEqual({
      to: '/device-access',
      label: '智维总览',
      caption: '查看接入智维分组能力与常用入口',
      short: '概'
    });
  });

  it('grants section home access when any child path is authorized', () => {
    expect(canAccessSectionHome('/system-management', ['/user'])).toBe(true);
    expect(canAccessSectionHome('/system-management', ['/risk-point'])).toBe(false);
    expect(getSectionHomeConfigByPath('/risk-disposal')?.title).toBe('风险运营');
  });

  it('builds static navigation groups from the shared workspace schema', () => {
    const groups = listStaticNavigationGroups();
    expect(groups.map((item) => item.key)).toEqual([
      'iot-access',
      'risk-ops',
      'risk-config',
      'system-governance',
      'quality-workbench'
    ]);
    expect(groups[0]?.items[0]?.label).toBe('产品定义中心');
  });

  it('matches activities by path and keywords for recent usage', () => {
    const config = getSectionHomeConfigByPath('/device-access');
    const cards = config?.cards || [];
    const reportCard = cards.find((item) => item.path === '/reporting');

    expect(reportCard).toBeTruthy();
    expect(matchSectionCardActivity(reportCard!, {
      title: 'HTTP 上报实验台 · 发送模拟上报',
      detail: '已向设备 demo-01 发送 property 报文',
      module: 'HTTP 上报实验台',
      action: '发送模拟上报',
      path: '/reporting'
    })).toBe(true);
  });

  it('picks deduplicated recent activities for a section', () => {
    const cards = getSectionHomeConfigByPath('/device-access')?.cards || [];
    const activities = pickSectionActivities(cards, [
      {
        id: '1',
        title: 'HTTP 上报实验台 · 发送模拟上报',
        detail: '已向设备 demo-01 发送 property 报文',
        module: 'HTTP 上报实验台',
        action: '发送模拟上报',
        ok: true,
        createdAt: '2026-03-17T10:00:00.000Z',
        path: '/reporting'
      },
      {
        id: '2',
        title: 'HTTP 上报实验台 · 发送模拟上报',
        detail: '已向设备 demo-01 发送 property 报文',
        module: 'HTTP 上报实验台',
        action: '发送模拟上报',
        ok: true,
        createdAt: '2026-03-17T10:01:00.000Z',
        path: '/reporting'
      },
      {
        id: '3',
        title: '账号中心 · 新增用户',
        detail: '已创建用户 demo',
        module: '账号中心',
        action: '新增用户',
        ok: true,
        createdAt: '2026-03-17T10:02:00.000Z',
        path: '/user'
      }
    ]);

    expect(activities).toHaveLength(1);
    expect(activities[0]?.path).toBe('/reporting');
  });

  it('resolves role profile and fallback home path by role', () => {
    const developerProfile = resolveRoleWorkbenchProfile(['DEVELOPER_STAFF'], ['开发人员'], false);
    expect(developerProfile.key).toBe('developer');
    expect(resolveRoleHomePath(['DEVELOPER_STAFF'], ['开发人员'], false, ['/reporting', '/system-log'])).toBe('/device-access');

    const managerProfile = resolveRoleWorkbenchProfile(['MANAGEMENT_STAFF'], ['管理人员'], false);
    expect(managerProfile.key).toBe('manager');
    expect(resolveRoleHomePath(['MANAGEMENT_STAFF'], ['管理人员'], false, ['/audit-log', '/report-analysis'])).toBe('/risk-disposal');

    const superAdminProfile = resolveRoleWorkbenchProfile(['SUPER_ADMIN'], ['超级管理员'], false);
    expect(superAdminProfile.key).toBe('super-admin');
  });

  it('resolves route meta from the shared workspace schema', () => {
    expect(getRouteMetaPreset('/risk-disposal')).toMatchObject({
      title: '风险运营',
      description: '围绕实时态势、告警协同、事件处置和运营复盘组织风险运营能力。'
    });
    expect(getRouteMetaPreset('/automation-test')).toMatchObject({
      title: '自动化工场',
      description: '维护巡检模板、执行计划和导出结果。'
    });
  });
});
