import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import StandardRowActions from '@/components/StandardRowActions.vue'

describe('StandardRowActions', () => {
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
