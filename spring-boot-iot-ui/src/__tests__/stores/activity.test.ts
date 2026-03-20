import { describe, expect, it } from 'vitest';

import { normalizeActivityEntry } from '@/stores/activity';

describe('activity store helpers', () => {
  it('builds title from module and action when explicit title is missing', () => {
    expect(normalizeActivityEntry({
      module: '设备运维中心',
      action: '新增设备',
      detail: '已创建设备 demo-01'
    })).toMatchObject({
      title: '设备运维中心 · 新增设备',
      tag: '设备运维中心',
      ok: true
    });
  });

  it('keeps explicit title and path when provided', () => {
    expect(normalizeActivityEntry({
      title: '驾驶舱跳转 · /device-access',
      detail: '从管理视角进入设备接入',
      tag: 'cockpit',
      path: '/device-access'
    })).toMatchObject({
      title: '驾驶舱跳转 · /device-access',
      tag: 'cockpit',
      path: '/device-access',
      ok: true
    });
  });
});
