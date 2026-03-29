import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

const root = resolve(import.meta.dirname, '../../..')
const sourceFiles = [
  'src/components/PanelCard.vue',
  'src/components/StandardWorkbenchPanel.vue',
  'src/components/StandardDetailDrawer.vue',
  'src/components/StandardFormDrawer.vue',
  'src/components/ResponsePanel.vue',
  'src/components/DeviceListDrawer.vue',
  'src/components/iotAccess/IotAccessWorkbenchHero.vue',
  'src/components/iotAccess/IotAccessSignalDeck.vue'
]

function containsEyebrowContract(file: string) {
  const source = readFileSync(resolve(root, file), 'utf8')
  return /eyebrow\?:|:eyebrow=|v-if="eyebrow"|lead\.eyebrow/.test(source)
}

describe('eyebrow contract guard', () => {
  it('keeps eyebrow-capable shared components limited to the lowest-level shared shells', () => {
    const offenders = sourceFiles.filter(containsEyebrowContract)

    expect(offenders).toEqual([
      'src/components/PanelCard.vue',
      'src/components/StandardWorkbenchPanel.vue'
    ])
  })
})
