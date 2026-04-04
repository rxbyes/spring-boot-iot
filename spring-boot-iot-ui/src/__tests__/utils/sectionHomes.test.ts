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
      'quality-workbench',
      'rd-workbench'
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
      description: '兼容旧入口，第一轮直接落到研发工场总览。'
    });
  });

  it('maps quality workbench to business acceptance plus rd and shared centers', () => {
    const config = getSectionHomeConfigByPath('/quality-workbench');

    expect(config?.cards.map((item) => item.path)).toEqual([
      '/business-acceptance',
      '/rd-workbench',
      '/automation-execution',
      '/automation-results'
    ]);
    expect(getRouteMetaPreset('/business-acceptance')).toMatchObject({
      title: '业务验收台',
      description: '按交付清单选择预置验收包并一键运行业务验收。'
    });
    expect(getRouteMetaPreset('/rd-workbench')).toMatchObject({
      title: '研发工场',
      description: '围绕页面盘点、模板沉淀、计划编排与交付打包组织研发自动化资产能力。'
    });
    expect(getRouteMetaPreset('/automation-assets')).toMatchObject({
      title: '自动化资产中心',
      description: '兼容旧入口，第一轮直接落到研发工场总览。'
    });
    expect(getRouteMetaPreset('/automation-execution')).toMatchObject({
      title: '执行中心',
      description: '统一查看执行配置、命令预览和验收注册表依赖关系。'
    });
    expect(getRouteMetaPreset('/automation-results')).toMatchObject({
      title: '结果与基线中心',
      description: '统一导入运行结果、查看失败场景并维护质量建议与基线证据。'
    });
  });

  it('maps rd workbench to inventory, templates, plans, and handoff pages', () => {
    const config = getSectionHomeConfigByPath('/rd-workbench');

    expect(config?.cards.map((item) => item.path)).toEqual([
      '/rd-automation-inventory',
      '/rd-automation-templates',
      '/rd-automation-plans',
      '/rd-automation-handoff'
    ]);
    expect(getRouteMetaPreset('/rd-automation-inventory')).toMatchObject({
      title: '页面盘点台',
      description: '维护页面清单、覆盖缺口与人工补录页面。'
    });
    expect(getRouteMetaPreset('/rd-automation-templates')).toMatchObject({
      title: '场景模板台',
      description: '沉淀页面冒烟、表单提交与列表详情模板。'
    });
    expect(getRouteMetaPreset('/rd-automation-plans')).toMatchObject({
      title: '计划编排台',
      description: '维护场景顺序、步骤、断言、导入与导出。'
    });
    expect(getRouteMetaPreset('/rd-automation-handoff')).toMatchObject({
      title: '交付打包台',
      description: '整理计划摘要、执行建议、基线说明与验收备注。'
    });
  });

  it('exposes updated iot access hub overview copy', () => {
    const config = getSectionHomeConfigByPath('/device-access');
    expect(config).toBeTruthy();
    expect(config?.intro).toContain('先去哪处理');
    expect(config?.description).toBe('接入智维总览只负责入口分组和快速判断，不再重复子页说明墙。');
    expect(config?.cards).toHaveLength(6);
    expect(config?.hubJudgement).toBe('先进入资产底座，再切换到诊断排障。');
    expect(config?.hubLeadTitle).toBe('优先处理资产底座与最近异常联动');
    expect(config?.hubLeadDescription).toBe('产品与设备先稳住，链路验证、异常观测、链路追踪、数据校验只保留强相关联动。');
    expect(config?.hubLeadPath).toBe('/products');
  });
});
