import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ShellAccountDrawers from '@/components/ShellAccountDrawers.vue'

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  props: ['modelValue', 'eyebrow', 'title', 'subtitle'],
  emits: ['update:modelValue'],
  template: `
    <section class="shell-account-drawer-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p>{{ subtitle }}</p>
      <slot />
      <slot name="footer" />
    </section>
  `
})

describe('ShellAccountDrawers', () => {
  it('uses one unified account center drawer plus password drawer in Chinese', () => {
    const wrapper = mount(ShellAccountDrawers, {
      props: {
        summary: {
          initial: 'A',
          name: '管理员',
          code: 'admin',
          type: '平台账号',
          roleName: '系统管理员',
          realName: '管理员',
          displayName: '管理员',
          phone: '13800000000',
          email: 'admin@example.com',
          authStatus: '已认证',
          loginMethods: '账号密码',
          primaryContact: '13800000000',
          tenantName: '默认租户',
          orgName: '平台治理中心',
          nickname: '平台总控官',
          dataScopeSummary: '全局',
          lastLoginTime: '2026-04-02 10:30:00',
          lastLoginIp: '10.10.10.8'
        },
        showAccountDialog: true,
        showChangePasswordDialog: true,
        passwordSubmitting: false,
        profileSubmitting: false
      },
      global: {
        stubs: {
          StandardFormDrawer: StandardFormDrawerStub,
          StandardButton: true,
          ElForm: true,
          ElFormItem: true,
          ElInput: true
        }
      }
    })

    const drawers = wrapper.findAllComponents(StandardFormDrawerStub)

    expect(drawers).toHaveLength(2)
    expect(drawers.every((item) => item.props('eyebrow') === undefined)).toBe(true)
    expect(wrapper.text()).toContain('账号中心')
    expect(wrapper.text()).toContain('修改密码')
    expect(wrapper.text()).toContain('基础资料')
    expect(wrapper.text()).toContain('机构与角色')
    expect(wrapper.text()).toContain('安全信息')
    expect(wrapper.text()).toContain('实名资料')
    expect(wrapper.text()).not.toContain('Account Center')
    expect(wrapper.text()).not.toContain('Account Security')
  })
})
