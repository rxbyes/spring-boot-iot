import request from '@/utils/request'

// 查询审计日志列表
export const listLogs = (params: any) => {
      return request({
            url: '/system/audit-log/list',
            method: 'get',
            params
      })
}

// 分页查询审计日志
export const pageLogs = (params: any) => {
      return request({
            url: '/system/audit-log/page',
            method: 'get',
            params
      })
}

// 根据ID查询审计日志
export const getAuditLogById = (id: number) => {
      return request({
            url: `/system/audit-log/get/${id}`,
            method: 'get'
      })
}

// 添加审计日志
export const addAuditLog = (data: any) => {
      return request({
            url: '/system/audit-log/add',
            method: 'post',
            data
      })
}

// 删除审计日志
export const deleteAuditLog = (id: number) => {
      return request({
            url: `/system/audit-log/delete/${id}`,
            method: 'delete'
      })
}
