import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { describe, expect, it } from 'vitest'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const srcRoot = path.resolve(__dirname, '..', '..')

function read(relativePath: string) {
  return fs.readFileSync(path.join(srcRoot, relativePath), 'utf8')
}

describe('font token contract', () => {
  it('uses a song-style font stack for both display and body tokens', () => {
    const tokensCss = read('styles/tokens.css')

    expect(tokensCss).toContain('--font-display: "Noto Serif SC"')
    expect(tokensCss).toContain('--font-body: "Noto Serif SC"')
    expect(tokensCss).toContain('"Songti SC"')
    expect(tokensCss).toContain('"STSong"')
    expect(tokensCss).toContain('serif;')
  })

  it('keeps the global page body and element-plus family bound to the body token', () => {
    const globalCss = read('styles/global.css')
    const elementOverridesCss = read('styles/element-overrides.css')

    expect(globalCss).toContain('font-family: var(--font-body);')
    expect(globalCss).toContain('font-family: var(--font-display);')
    expect(elementOverridesCss).toContain('--el-font-family: var(--font-body);')
  })

  it('defines a shared title-to-body hierarchy scale for song-style typography', () => {
    const tokensCss = read('styles/tokens.css')
    const globalCss = read('styles/global.css')
    const elementOverridesCss = read('styles/element-overrides.css')

    expect(tokensCss).toContain('--font-letter-spacing-tight:')
    expect(tokensCss).toContain('--font-letter-spacing-wide:')
    expect(tokensCss).toContain('--type-title-1-size:')
    expect(tokensCss).toContain('--type-title-2-size:')
    expect(tokensCss).toContain('--type-overline-size:')
    expect(tokensCss).toContain('--type-body-size:')
    expect(tokensCss).toContain('--type-caption-size:')
    expect(tokensCss).toContain('--type-toolbar-meta-size:')
    expect(globalCss).toContain('font-size: var(--type-body-size);')
    expect(globalCss).toContain('line-height: var(--type-body-line-height);')
    expect(globalCss).toContain('font-size: var(--type-title-1-size);')
    expect(globalCss).toContain('letter-spacing: var(--font-letter-spacing-tight);')
    expect(elementOverridesCss).toContain('--el-font-size-base: var(--type-body-size);')
  })

  it('preserves a dedicated monospace token for technical text', () => {
    const tokensCss = read('styles/tokens.css')

    expect(tokensCss).toContain('--font-mono:')
    expect(tokensCss).toContain('monospace;')
  })
})
