import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { mount } from '@vue/test-utils'
import { afterAll, beforeAll, describe, expect, it } from 'vitest'

import StandardRowActions from '@/components/StandardRowActions.vue'

const injectedStyleNodes: HTMLStyleElement[] = []

beforeAll(() => {
  const elementPlusStyle = document.createElement('style')
  elementPlusStyle.textContent = '.el-button + .el-button { margin-left: 12px; }'
  document.head.appendChild(elementPlusStyle)
  injectedStyleNodes.push(elementPlusStyle)

  const globalStyle = document.createElement('style')
  globalStyle.textContent = readFileSync(resolve(import.meta.dirname, '../../styles/global.css'), 'utf8')
  document.head.appendChild(globalStyle)
  injectedStyleNodes.push(globalStyle)
})

afterAll(() => {
  for (const node of injectedStyleNodes) {
    node.remove()
  }
})

describe('StandardRowActions', () => {
  it('right-aligns shared table action cells to keep the trailing gutter consistent', () => {
    const host = document.createElement('div')
    host.innerHTML = `
      <div class="standard-row-actions-column">
        <div class="cell">
          <div class="standard-row-actions standard-row-actions--variant-table standard-row-actions--wide">
            <button class="el-button standard-button">详情</button>
            <button class="el-button standard-button">观测</button>
          </div>
        </div>
      </div>
    `
    document.body.appendChild(host)

    const cell = host.querySelector('.cell')
    expect(cell).not.toBeNull()
    expect(window.getComputedStyle(cell as Element).textAlign).toBe('right')

    host.remove()
  })

  it('uses the table variant and wide gap class for three-action table rows', () => {
    const wrapper = mount(StandardRowActions, {
      props: {
        variant: 'table',
        gap: 'wide'
      },
      slots: {
        default: '<button>详情</button><button>编辑</button><button>更多</button>'
      }
    })

    expect(wrapper.classes()).toContain('standard-row-actions')
    expect(wrapper.classes()).toContain('standard-row-actions--variant-table')
    expect(wrapper.classes()).toContain('standard-row-actions--wide')
  })

  it('adds the between-distribution class when table actions need equal spacing', () => {
    const wrapper = mount(StandardRowActions, {
      props: {
        variant: 'table',
        distribution: 'between'
      },
      slots: {
        default: '<button>详情</button><button>编辑</button><button>更多</button>'
      }
    })

    expect(wrapper.classes()).toContain('standard-row-actions--distribution-between')
  })

  it('resets Element Plus sibling button margins inside shared row actions', () => {
    const wrapper = mount(StandardRowActions, {
      props: {
        variant: 'table',
        distribution: 'between'
      },
      slots: {
        default: `
          <button class="el-button standard-button">详情</button>
          <button class="el-button standard-button">编辑</button>
          <button class="el-button standard-button">更多</button>
        `
      },
      attachTo: document.body
    })

    const buttons = wrapper.findAll('button')

    expect(window.getComputedStyle(buttons[1].element).marginLeft).toBe('0px')

    wrapper.unmount()
  })

  it('uses the card variant class for touch action rows', () => {
    const wrapper = mount(StandardRowActions, {
      props: {
        variant: 'card',
        gap: 'comfortable'
      }
    })

    expect(wrapper.classes()).toContain('standard-row-actions--variant-card')
    expect(wrapper.classes()).toContain('standard-row-actions--comfortable')
  })

  it('uses the editor variant as a wrapping action row', () => {
    const wrapper = mount(StandardRowActions, {
      props: {
        variant: 'editor',
        gap: 'comfortable',
        wrap: true
      }
    })

    expect(wrapper.classes()).toContain('standard-row-actions--variant-editor')
    expect(wrapper.classes()).toContain('standard-row-actions--comfortable')
    expect(wrapper.classes()).toContain('standard-row-actions--wrap')
  })
})
