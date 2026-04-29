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
    expect(groups[0]?.items[0]?.label).toBe('无代码接入台');
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
    expect(getRouteMetaPreset('/governance-approval')).toMatchObject({
      title: '治理审批台'
    });
    expect(getRouteMetaPreset('/governance-security')).toMatchObject({
      title: '权限与密钥治理'
    });
    expect(getRouteMetaPreset('/automation-governance')).toMatchObject({
      title: '自动化治理台',
      description: '统一承接资产编排、执行配置与结果证据。'
    });
    expect(getRouteMetaPreset('/automation-test')).toBeUndefined();
  });

  it('adds governance approval and security governance into the system-governance workspace cards', () => {
    const config = getSectionHomeConfigByPath('/system-management');

    expect(config?.cards.some((item) => item.path === '/governance-approval')).toBe(true);
    expect(config?.cards.some((item) => item.path === '/governance-security')).toBe(true);
  });

  it('adds governance control-plane workbenches into the system-governance workspace cards', () => {
    const config = getSectionHomeConfigByPath('/system-management');

    expect(config?.cards.some((item) => item.path === '/governance-task')).toBe(true);
    expect(config?.cards.some((item) => item.path === '/governance-ops')).toBe(true);
    expect(getRouteMetaPreset('/governance-task')).toMatchObject({
      title: '治理任务台'
    });
    expect(getRouteMetaPreset('/governance-ops')).toMatchObject({
      title: '治理运维台'
    });
  });

  it('maps quality workbench to business acceptance plus automation governance', () => {
    const config = getSectionHomeConfigByPath('/quality-workbench');

    expect(config?.cards.map((item) => item.path)).toEqual([
      '/business-acceptance',
      '/automation-governance'
    ]);
    expect(getRouteMetaPreset('/business-acceptance')).toMatchObject({
      title: '业务验收台',
      description: '按交付清单选择预置验收包并一键运行业务验收。'
    });
    expect(getRouteMetaPreset('/automation-governance')).toMatchObject({
      title: '自动化治理台',
      description: '统一承接资产编排、执行配置与结果证据。'
    });
  });

  it('exposes updated iot access hub overview copy', () => {
    const config = getSectionHomeConfigByPath('/device-access');
    expect(config).toBeTruthy();
    expect(config?.intro).toContain('排障树哪一段');
    expect(config?.description).toBe('接入智维总览负责回答先去哪、再去哪、最后去哪修。');
    expect(config?.cards).toHaveLength(8);
    expect(config?.hubJudgement).toBe('先做链路验证，再按证据分流到诊断页，最后回产品或设备治理修正。');
    expect(config?.hubLeadTitle).toBe('标准排障路径');
    expect(config?.hubLeadDescription).toBe('标准排障路径固定为：链路验证 -> 链路追踪 / 异常观测 / 数据校验 -> 产品定义中心 / 设备资产中心。');
    expect(config?.hubLeadPath).toBe('/products');
    expect(config?.steps).toEqual([
      '先到链路验证中心拿到本次验证结果。',
      '再按问题类型进入链路追踪台、异常观测台或数据校验台。',
      '最后回产品定义中心或设备资产中心完成修正。'
    ]);
    expect(config?.cards.find((item) => item.path === '/device-onboarding')?.description).toBe('创建接入案例、查看阻塞原因，并跳到协议治理或产品工作台继续处理。');
    expect(config?.cards.find((item) => item.path === '/products')?.description).toBe('维护产品定义，并作为进入产品工作台的统一入口承接契约、映射和版本治理。');
    expect(config?.cards.find((item) => item.path === '/protocol-governance')?.description).toBe('维护跨产品复用的协议族、解密档案与协议模板。');
    expect(config?.cards.find((item) => item.path === '/reporting')?.description).toBe('排障起点：先发起模拟验证，再决定进入哪一条诊断分支。');
  });
});
