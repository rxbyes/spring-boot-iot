import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

function readSource() {
  return readFileSync(resolve(import.meta.dirname, '../../views/RoleView.vue'), 'utf8');
}

describe('RoleView governance contract', () => {
  it('renders the role data-scope field in both table and form', () => {
    const source = readSource();

    expect(source).toContain('label="数据范围"');
    expect(source).toContain('prop="dataScopeType"');
    expect(source).toContain('v-model="formData.dataScopeType"');
  });

  it('submits the role data-scope value together with role payloads', () => {
    const source = readSource();

    expect(source).toContain('dataScopeType:');
    expect(source).toContain('payload.id ? await updateRole(payload) : await addRole(payload)');
  });

  it('renders the role authorization drawer as summary plus full permission tree and current-node detail', () => {
    const source = readSource();

    expect(source).toContain('RoleAuthPermissionTreePanel');
    expect(source).toContain('RoleAuthNodeDetailPanel');
    expect(source).toContain('role-auth-summary-grid');
    expect(source).not.toContain('RoleAuthSelectedPagesPanel');
    expect(source).not.toContain('步骤 1：页面授权');
  });

  it('keeps all-level granted ids in a single local state and submits menuIds from that state', () => {
    const source = readSource();

    expect(source).toContain('const grantedMenuIds = ref<IdType[]>([])');
    expect(source).toContain('toggleMenuGrant(');
    expect(source).toContain('resolveGrantedMenuIds(');
    expect(source).toContain('menuIds: [...grantedMenuIds.value]');
  });

  it('keeps role and menu ids as IdType values instead of coercing them into unsafe numbers', () => {
    const source = readSource();

    expect(source).not.toContain('id: Number(res.data.id)');
    expect(source).not.toContain('typeof currentNode.value.parentId !== "number"');
    expect(source).toContain('const currentNodeId = ref<IdType | null>(null)');
  });
});
