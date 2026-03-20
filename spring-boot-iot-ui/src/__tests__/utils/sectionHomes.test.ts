import { describe, expect, it } from 'vitest';

import { canAccessSectionHome, createSectionHomeNavItem, getSectionHomeConfigByPath, matchSectionCardActivity, pickSectionActivities, resolveSectionHomeConfig } from '@/utils/sectionWorkspaces';

describe('sectionHomes config', () => {
  it('resolves section home config by group key and label', () => {
    expect(resolveSectionHomeConfig('system-core', '系统管理')?.path).toBe('/system-management');
    expect(resolveSectionHomeConfig('risk-core', '风险处置')?.navLabel).toBe('运营总览');
  });

  it('creates overview navigation item for known group', () => {
    expect(createSectionHomeNavItem('iot-core', '设备接入')).toEqual({
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
});
