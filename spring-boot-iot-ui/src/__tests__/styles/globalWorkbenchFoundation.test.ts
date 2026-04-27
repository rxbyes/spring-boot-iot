import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const readUiFile = (relativePath: string) => {
  const cwd = process.cwd()
  const directPath = resolve(cwd, 'src', relativePath)
  const repoPath = resolve(cwd, 'spring-boot-iot-ui', 'src', relativePath)

  return readFileSync(existsSync(directPath) ? directPath : repoPath, 'utf8')
}

describe('global workbench foundation styles', () => {
  it('defines the shared workbench token aliases', () => {
    const tokensCss = readUiFile('styles/tokens.css')

    expect(tokensCss).toContain('--workbench-surface-bg:')
    expect(tokensCss).toContain('--workbench-surface-bg-strong:')
    expect(tokensCss).toContain('--workbench-table-header-bg:')
    expect(tokensCss).toContain('--workbench-table-row-hover-bg:')
    expect(tokensCss).toContain('--workbench-action-text:')
    expect(tokensCss).toContain('--workbench-status-danger-bg:')
  })

  it('applies the quieter workbench row-action grammar globally', () => {
    const globalCss = readUiFile('styles/global.css')

    expect(globalCss).toContain('.standard-row-actions--foundation')
    expect(globalCss).toContain('color: var(--workbench-action-text);')
    expect(globalCss).toContain('color: var(--workbench-action-hover-text);')
  })

  it('aligns Element Plus tables, tags, and pagination with the workbench foundation', () => {
    const overridesCss = readUiFile('styles/element-overrides.css')

    expect(overridesCss).toContain('--el-table-header-bg-color: var(--workbench-table-header-bg);')
    expect(overridesCss).not.toContain('.el-table__body tr:hover > td.el-table__cell {')
    expect(overridesCss).toContain(
      '.ops-workbench .el-table__body tr:hover > td.el-table__cell'
    )
    expect(overridesCss).toContain(
      '.standard-list-view .el-table__body tr:hover > td.el-table__cell'
    )
    expect(overridesCss).toContain(
      '.sys-mgmt-view .el-table__body tr:hover > td.el-table__cell'
    )
    expect(overridesCss).toContain('background: var(--workbench-table-row-hover-bg);')
    expect(overridesCss).toContain('.el-tag {')
    expect(overridesCss).toContain('box-shadow: none;')
    expect(overridesCss).toContain('.standard-pagination .el-pager li.is-active {')
    expect(overridesCss).toContain('background: var(--workbench-pagination-active-bg);')
  })
})
