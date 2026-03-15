import request from '@/utils/request'

// 角色列表
export interface Role {
      id: number
      tenantId: number
      roleName: string
      roleCode: string
      description: string
      status: number
      createTime: string
      updateTime: string
}

// 查询角色列表
export function listRoles(params: {
      roleName?: string
      roleCode?: string
      status?: number
}) {
      return request({
            url: '/api/role/list',
            method: 'get',
            params
      })
}

// 根据ID查询角色
export function getRole(id: number) {
      return request({
            url: `/api/role/${id}`,
            method: 'get'
      })
}

// 添加角色
export function addRole(data: Role) {
      return request({
            url: '/api/role/add',
            method: 'post',
            data
      })
}

// 更新角色
export function updateRole(data: Role) {
      return request({
            url: '/api/role/update',
            method: 'put',
            data
      })
}

// 删除角色
export function deleteRole(id: number) {
      return request({
            url: `/api/role/${id}`,
            method: 'delete'
      })
}

// 查询用户角色列表
export function listUserRoles(userId: number) {
      return request({
            url: `/api/role/user/${userId}`,
            method: 'get'
      })
}
