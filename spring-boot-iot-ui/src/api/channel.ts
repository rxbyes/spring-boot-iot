import request from '@/utils/request'

// 通知渠道类型
export const CHANNEL_TYPES = [
      { value: 'email', label: '邮箱' },
      { value: 'sms', label: '短信' },
      { value: 'wechat', label: '微信' },
      { value: 'feishu', label: '飞书' },
      { value: 'dingtalk', label: '钉钉' }
]

// 查询通知渠道列表
export const listChannels = () => {
      return request({
            url: '/system/channel/list',
            method: 'get'
      })
}

// 根据渠道编码查询通知渠道
export const getChannelByCode = (channelCode: string) => {
      return request({
            url: `/system/channel/getByCode/${channelCode}`,
            method: 'get'
      })
}

// 添加通知渠道
export const addChannel = (data: any) => {
      return request({
            url: '/system/channel/add',
            method: 'post',
            data
      })
}

// 更新通知渠道
export const updateChannel = (data: any) => {
      return request({
            url: '/system/channel/update',
            method: 'put',
            data
      })
}

// 删除通知渠道
export const deleteChannel = (id: number) => {
      return request({
            url: `/system/channel/delete/${id}`,
            method: 'delete'
      })
}
