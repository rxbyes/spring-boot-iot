import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function readSource() {
  return readFileSync(resolve(import.meta.dirname, '../../views/UserView.vue'), 'utf8')
}

describe('UserView governance contract', () => {
  it('keeps three direct row actions and removes the page-private more-action fallback for users', () => {
    const source = readSource()

    expect(source).toContain(`actions.push({ command: "edit", label: "编辑" })`)
    expect(source).toContain(`actions.push({ command: "reset-password", label: "重置密码" })`)
    expect(source).toContain(`actions.push({ command: "delete", label: "删除" })`)
    expect(source).not.toContain('menuItems')
  })

  it('exposes nickname, primary organization and role binding fields in the user form', () => {
    const source = readSource()

    expect(source).toContain('label="昵称"')
    expect(source).toContain('label="主机构"')
    expect(source).toContain('label="角色绑定"')
    expect(source).toContain('v-model="formData.nickname"')
    expect(source).toContain('v-model="formData.orgId"')
    expect(source).toContain('v-model="formData.roleIds"')
  })
})
