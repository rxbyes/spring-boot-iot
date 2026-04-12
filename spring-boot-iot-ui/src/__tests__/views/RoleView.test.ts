import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function readSource() {
  return readFileSync(resolve(import.meta.dirname, '../../views/RoleView.vue'), 'utf8')
}

describe('RoleView governance contract', () => {
  it('renders the role data-scope field in both table and form', () => {
    const source = readSource()

    expect(source).toContain('label="数据范围"')
    expect(source).toContain('prop="dataScopeType"')
    expect(source).toContain('v-model="formData.dataScopeType"')
  })

  it('submits the role data-scope value together with role payloads', () => {
    const source = readSource()

    expect(source).toContain('dataScopeType:')
    expect(source).toContain('payload.id ? await updateRole(payload) : await addRole(payload)')
  })

  it('renders the three-stage role authorization workspace instead of a mixed menu-button tree', () => {
    const source = readSource()

    expect(source).toContain('RoleAuthPageTreePanel')
    expect(source).toContain('RoleAuthSelectedPagesPanel')
    expect(source).toContain('RoleAuthButtonPanel')
    expect(source).toContain('步骤 1：页面授权')
    expect(source).toContain('步骤 2：已选页面')
    expect(source).toContain('当前页面按钮权限')
    expect(source).not.toContain('<h3>菜单与按钮授权</h3>')
  })

  it('recomposes submitted menu ids from selected pages and page-local buttons', () => {
    const source = readSource()

    expect(source).toContain('composeRoleGrantedMenuIds(')
    expect(source).toContain('selectedPageIds')
    expect(source).toContain('selectedButtonIdsByPage')
    expect(source).toContain('selectedButtonIdsByPage.value[pageId] = []')
  })
})
