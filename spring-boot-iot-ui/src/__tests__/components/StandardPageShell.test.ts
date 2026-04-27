import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { mount, RouterLinkStub } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import StandardPageShell from '@/components/StandardPageShell.vue'

describe('StandardPageShell', () => {
  it('renders the eyebrow, title, description, and breadcrumb hierarchy', () => {
    const wrapper = mount(StandardPageShell, {
      props: {
        eyebrow: 'PRODUCT DOSSIER',
        title: '产品经营工作台',
        description: '统一梳理产品台账、接入概况与治理节奏。',
        showBreadcrumbs: true,
        breadcrumbs: [
          { label: '产品中心', to: '/product' },
          { label: '产品经营工作台' }
        ]
      },
      global: {
        stubs: {
          RouterLink: RouterLinkStub
        }
      }
    })

    expect(wrapper.find('.standard-page-shell__eyebrow').text()).toBe('PRODUCT DOSSIER')
    expect(wrapper.classes()).toContain('standard-page-shell--workbench-foundation')
    expect(wrapper.find('.standard-page-shell__title').text()).toBe('产品经营工作台')
    expect(wrapper.find('.standard-page-shell__description').text()).toContain('统一梳理产品台账')
    expect(wrapper.findAll('.standard-page-shell__breadcrumb-item')).toHaveLength(2)
  })

  it('binds the shell headline to the shared song-style hierarchy tokens', () => {
    const source = readFileSync(
      resolve(import.meta.dirname, '../../components/StandardPageShell.vue'),
      'utf8'
    )

    expect(source).toContain('var(--type-overline-size)')
    expect(source).toContain('var(--type-title-1-size)')
    expect(source).toContain('var(--type-caption-size)')
    expect(source).toContain('var(--font-letter-spacing-wide)')
    expect(source).toContain('var(--font-letter-spacing-tight)')
  })

  it('balances the headline when actions are present', () => {
    const wrapper = mount(StandardPageShell, {
      props: {
        title: 'Workbench'
      },
      slots: {
        actions: '<button type="button">Create</button>'
      },
      global: {
        stubs: {
          RouterLink: RouterLinkStub
        }
      }
    })

    expect(wrapper.find('.standard-page-shell__headline').classes()).toContain(
      'standard-page-shell__headline--balanced'
    )
  })
})
