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
})
