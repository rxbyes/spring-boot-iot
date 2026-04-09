import { defineComponent, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ChannelView from '@/views/ChannelView.vue'

const { mockPageChannels, mockFetchChannelTypeOptions } = vi.hoisted(() => ({
  mockPageChannels: vi.fn(),
  mockFetchChannelTypeOptions: vi.fn()
}))

vi.mock('@/api/channel', () => ({
  CHANNEL_TYPES: [
    { label: '邮件', value: 'email' },
    { label: '短信', value: 'sms' },
    { label: 'Webhook', value: 'webhook' },
    { label: '微信', value: 'wechat' },
    { label: '飞书', value: 'feishu' },
    { label: '钉钉', value: 'dingtalk' }
  ],
  fetchChannelTypeOptions: mockFetchChannelTypeOptions,
  pageChannels: mockPageChannels,
  getChannelByCode: vi.fn(),
  addChannel: vi.fn(),
  updateChannel: vi.fn(),
  deleteChannel: vi.fn(),
  testChannel: vi.fn()
}))

describe('ChannelView', () => {
  beforeEach(() => {
    mockFetchChannelTypeOptions.mockReset()
    mockFetchChannelTypeOptions.mockResolvedValue([
      { label: 'Webhook', value: 'webhook', sortNo: 3 },
      { label: '微信', value: 'wechat', sortNo: 4 }
    ])
    mockPageChannels.mockReset()
    mockPageChannels.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: { total: 0, pageNum: 1, pageSize: 10, records: [] }
    })
  })

  it('loads dict-backed channel type options on mount', async () => {
    mount(ChannelView, {
      global: {
        directives: { loading: () => undefined },
        stubs: {
          StandardWorkbenchPanel: defineComponent({ template: '<div><slot name="filters" /><slot /></div>' }),
          StandardListFilterHeader: defineComponent({ template: '<div><slot name="primary" /><slot name="actions" /></div>' }),
          StandardTableToolbar: true,
          StandardButton: true,
          StandardAppliedFiltersBar: true,
          StandardPagination: true,
          StandardTableTextColumn: true,
          StandardWorkbenchRowActions: true,
          StandardFormDrawer: true,
          StandardDrawerFooter: true,
          CsvColumnSettingDialog: true,
          EmptyState: true,
          ElTable: true,
          ElTableColumn: true,
          ElForm: true,
          ElFormItem: true,
          ElInput: true,
          ElSelect: true,
          ElOption: true,
          ElTag: true,
          ElInputNumber: true,
          ElRadioGroup: true,
          ElRadio: true
        }
      }
    })

    await nextTick()
    await Promise.resolve()
    await nextTick()

    expect(mockFetchChannelTypeOptions).toHaveBeenCalledTimes(1)
  })
})
