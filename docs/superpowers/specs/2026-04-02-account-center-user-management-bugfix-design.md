# 账号中心与用户管理缺陷修复设计

> 日期：2026-04-02
> 范围：`spring-boot-iot-system`、`spring-boot-iot-ui`、相关权威文档

## 背景

账号中心模块在真实环境中暴露出 4 个问题：

1. 新建用户后，昵称与真实姓名出现乱码。
2. 新建用户时，“主机构”下拉只能选到当前一级节点，无法继续选择 2、3 级公司。
3. 编辑用户时，绑定多个角色会报“系统繁忙，请稍后重试”。
4. 删除用户后，列表条数不变。

## 已确认事实

- 共享环境 `sys_user`、`sys_user_role` 表都是 `utf8mb4`，最近新写入中文用户数据可正常落库。
- 当前用户管理页把机构和角色 ID 强制转成 `Number(...)`；共享环境中角色、机构主键已出现 19 位雪花 Long，前端会发生精度丢失。
- 当前 `/api/organization/tree` 已被定义为“查询侧数据权限树”，`ORG / SELF` 只返回当前节点，不适合直接作为“用户表单可写机构树”。
- 当前 `UserServiceImpl#buildUserQueryWrapper` 缺少 `deleted = 0` 过滤，导致逻辑删除后的用户仍会参与分页总数统计。
- 共享环境登录响应在 PowerShell 直读时存在中文解码异常现象，说明仍有必要补一层 UTF-8 编码硬化，避免账号中心中文字段在不同终端或代理链路中继续出现显示异常。

## 设计决策

### 1. 机构树拆分“查询可见范围”和“写入可选范围”

- 保留现有 `/api/organization/tree` 语义不变，继续服务于组织管理查询与详情读取。
- 新增“可写机构树”接口，专门给用户新增/编辑表单使用。
- 可写机构树口径：
  - `ALL / TENANT`：当前租户全部组织。
  - `ORG_AND_CHILDREN`：当前主机构及其子树。
  - `ORG`：当前主机构及其子树。
  - `SELF`：当前主机构节点。
- 用户新增/编辑时的 `orgId` 校验改为使用“可写机构范围”，不再复用“查询可见范围”。

### 2. 用户管理页全程按字符串处理 ID

- 前端保留 `IdType = string | number`，但 `UserView` 内部不再对组织、角色 ID 做 `Number(...)` 转换。
- 下拉选项、表单值、回显匹配全部统一使用 `String(id)` 对比。
- 这样可以避免 19 位 Long 被 JS Number 截断后导致：
  - 多角色绑定写错 roleId。
  - 机构下拉选项、回填与名称解析错位。

### 3. 用户分页补齐逻辑删除过滤

- 用户列表与分页查询都必须显式追加 `deleted = 0`。
- 删除后前端继续刷新当前分页，但后端总数必须基于未删除记录重新计算。

### 4. UTF-8 编码硬化

- 后端补一层统一 UTF-8 编码配置，确保 JSON 请求/响应在真实环境链路中显式按 UTF-8 处理。
- 不修改数据库字符集，不引入迁移脚本。

## 影响文件

- 后端：
  - `spring-boot-iot-system/.../PermissionService.java`
  - `spring-boot-iot-system/.../impl/PermissionServiceImpl.java`
  - `spring-boot-iot-system/.../OrganizationController.java`
  - `spring-boot-iot-system/.../UserServiceImpl.java`
  - `spring-boot-iot-ui/src/api/organization.ts`
  - 相关单测
- 前端：
  - `spring-boot-iot-ui/src/views/UserView.vue`
  - 相关 Vitest 用例
- 配置：
  - `spring-boot-iot-admin/src/main/resources/application-dev.yml`
  - 视需要同步 `application-prod.yml`、`application-test.yml`
- 文档：
  - `docs/02-业务功能与流程说明.md`
  - `docs/03-接口规范与接口清单.md`
  - `docs/07-部署运行与配置说明.md`
  - `docs/08-变更记录与技术债清单.md`

## 错误处理

- 机构越权仍返回业务错误，不放宽到全租户。
- 若前端收到空机构树，保留现有错误日志输出，不额外吞错。
- 多角色绑定失败应返回明确业务错误；不再因 Long 精度问题落入后端兜底 500。

## 测试策略

- 后端单测：
  - 验证用户分页追加 `deleted = 0`。
  - 验证不同数据范围下“可写机构 ID”计算。
- 前端单测：
  - 验证 `UserView` 不再把角色和机构 ID 强转成 `Number`。
  - 验证用户表单暴露新的“可写机构树”接口使用点。
- 真实环境验证：
  - 登录后新增用户，中文昵称/真实姓名回显正常。
  - 可选择当前主机构下 2、3 级机构。
  - 编辑用户可绑定多个 19 位角色 ID。
  - 删除用户后分页总数下降。
