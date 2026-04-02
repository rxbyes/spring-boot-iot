# 账号中心闭环与身份治理链路设计

> 日期：2026-04-02
> 范围：`spring-boot-iot-ui`、`spring-boot-iot-auth`、`spring-boot-iot-system`、`sql/init.sql`、`sql/init-data.sql`
> 主题：先把壳层账号中心做成真实可用闭环，再补齐用户-机构-角色-权限-数据范围的最小治理链路
> 状态：设计已确认，待书面 spec 审阅

## 1. 背景

当前平台治理与壳层身份体系存在三个直接问题：

- 右上角账号菜单里的 `账号中心 / 实名认证 / 登录方式 / 修改密码` 中，除改密外大多仍是静态说明抽屉，不是可验收的真实能力。
- 登录后的身份上下文目前只覆盖 `角色 / 菜单 / 按钮权限`，没有机构归属、租户摘要和数据范围表达，因此“按登录用户关联机构与权限”无法成立。
- `账号中心`、后台 `用户管理`、`角色权限`、`组织架构` 之间没有形成统一身份主数据链路，导致壳层能展示的信息和后台能维护的信息不一致。

同时，前端共享动作容器当前默认只直出 `2` 个动作，第三个动作会被折叠进 `更多`。这使得 `编辑 / 重置密码 / 删除` 一类高频三动作列表出现了不必要的二次点击，也和当前多页治理目标不一致。

## 2. 目标

- 把壳层 `账号中心` 从静态说明升级为真实可用闭环。
- 保持现有平台治理与壳层共享视觉风格，不引入新的强视觉或私有页面语法。
- 统一“操作列三动作直出、四动作及以上折叠”的共享规则，并覆盖系统内同类页面。
- 补齐 `用户 -> 主机构 -> 角色 -> 权限上下文 -> 数据范围摘要` 的最小治理模型。
- 让 `账号中心`、`用户管理`、`角色权限` 和 `组织架构` 共享同一套身份数据源，为后续多租户与数据权限扩展预留稳定基线。

## 3. 非目标

- 本轮不重做壳层账号区域的整体视觉语言，不采用新的强视觉 Hero、品牌刊头或营销式布局。
- 本轮不一次性改造所有业务查询为“严格按机构/数据范围过滤”。
- 本轮不引入“一人多机构”的复杂成员关系模型。
- 本轮不新增独立“租户治理中心”页面，只补最小租户只读能力满足账号中心和鉴权上下文需要。
- 本轮不引入外部实名认证审核流；如共享环境没有审核系统，仅保留真实资料字段与真实状态口径。

## 4. 用户已确认方向

对话中已经确认以下关键约束：

- 第一优先级：先把 `账号中心` 做成可用闭环。
- 第二优先级：把 `用户-机构-权限` 模型定下来，并落到系统治理链路。
- 视觉风格必须保持现有系统风格，简单、大气、稳定，继续复用共享抽屉和共享设计令牌。
- 不采用新的强设计稿，而是在现有抽屉语法中增强真实能力。

## 5. 当前事实与缺口

### 5.1 前端壳层

- `AppHeaderTools.vue` 当前仍提供 `账号中心 / 实名认证 / 登录方式 / 修改密码 / 退出登录` 五项入口。
- `ShellAccountDrawers.vue` 中，`账号中心 / 实名认证 / 登录方式` 基本都是静态展示或说明文案。
- `useShellAccountCenter.ts` 主要消费 `permissionStore.authContext`，但现有 `authContext` 中没有机构、租户和数据范围。

### 5.2 后端身份上下文

- `/api/auth/login` 与 `/api/auth/me` 通过 `PermissionServiceImpl#getUserAuthContext` 返回 `UserAuthContextVO`。
- 该 VO 当前只承载：
  - `userId / username / realName / displayName / phone / email`
  - `accountType / authStatus / loginMethods`
  - `roles / roleCodes / permissions / menus / homePath / superAdmin`
- 这足以支撑“菜单 / 按钮权限”，但不足以支撑“机构归属 / 数据范围 / 账号中心真实摘要”。

### 5.3 数据模型

- `sys_user` 表当前已有 `nickname`、`avatar`、`is_admin` 等字段，但 `User` 实体未完整映射。
- `sys_organization` 仅有 `leader_user_id`，没有“用户归属机构”字段或成员关系表。
- `sys_tenant` 已在 `sql/init.sql` 中存在，但当前代码里没有对应只读实体/Mapper/Service。
- 现有文档也明确承认：当前只完成了“角色-菜单-按钮权限”闭环，组织范围与数据范围过滤仍未形成完整闭环。

## 6. 方案决策

采用“现有风格下的统一账号中心抽屉 + 最小身份治理补齐 + 共享动作规则收口”方案。

### 6.1 为什么不采用仅补接口的快修方案

- 只给现有静态抽屉逐个接接口，会继续保留分散入口和割裂的阅读路径。
- 右上角入口越多，越容易继续出现“假能力说明页”。
- 不补机构归属和数据范围表达，账号中心仍然只能展示半套信息，后续还要返工。

### 6.2 为什么不做全量身份重构

- 一次性引入多机构、多数据范围、多业务查询过滤，会把第一优先级拖进大范围重构。
- 当前真实环境基线仍以默认租户、固定角色和治理域演示数据为主，适合先完成最小可验收闭环。

## 7. 交互设计

### 7.1 右上角账号菜单

统一收敛为三项：

- `账号中心`
- `修改密码`
- `退出登录`

移除单独的：

- `实名认证`
- `登录方式`

原因：

- 这两个入口当前只是静态说明，不应继续作为一级壳层动作。
- 它们的真实信息将并入 `账号中心` 抽屉内部，避免“说明页跳说明页”。

### 7.2 账号中心抽屉

继续复用：

- `StandardFormDrawer`
- `StandardDrawerFooter`
- 共享表单、描述项和轻量信息分区样式

不再新增强视觉大卡片，仅按现有风格组织四个轻量区块：

1. `基础资料`
   - 用户名：只读
   - 显示名称：可编辑，来源 `nickname`
   - 真实姓名：可编辑，来源 `real_name`
   - 手机号：可编辑
   - 邮箱：可编辑

2. `机构与角色`
   - 租户名称：只读
   - 主机构：只读
   - 角色摘要：只读
   - 数据范围摘要：只读

3. `安全信息`
   - 可用登录方式：只读
   - 最近登录时间：只读
   - 最近登录 IP：只读
   - 提供 `修改密码` 二级动作

4. `实名资料`
   - 继续复用真实姓名等字段
   - `实名状态` 只能表达真实口径，例如：
     - `未填写实名资料`
     - `已填写实名资料`
   - 不再展示“待认证接入说明”“后续补流程”等假状态文本

### 7.3 修改密码抽屉

- 保留独立抽屉，不与资料编辑混在同一表单。
- 成功后继续要求重新登录。
- 这一交互已存在，可继续复用。

## 8. 共享动作列规则

### 8.1 统一规则

共享动作拆分逻辑统一改为：

- 可执行动作 `<= 3`：全部直出
- 可执行动作 `> 3`：超出部分折叠到 `更多`

### 8.2 落点

规则优先落在共享层：

- `spring-boot-iot-ui/src/utils/adaptiveActionColumn.ts`
- `spring-boot-iot-ui/src/components/StandardWorkbenchRowActions.vue`

而不是只在 `UserView.vue` 单页特判。

### 8.3 影响

这会自动优化所有复用共享动作组件、且当前动作数为 `3` 的页面，不只账号中心。

## 9. 身份数据模型设计

### 9.1 用户主数据

`sys_user` 继续作为身份主表，并补齐以下字段映射：

- `nickname`
- `avatar`
- `is_admin`
- `remark`

同时新增：

- `org_id BIGINT DEFAULT NULL COMMENT '主机构ID'`

本轮含义固定为“当前账号主机构归属”。

### 9.2 机构归属模型

本轮采用：

- `一个账号 -> 一个主机构`

不采用：

- 多机构成员关系表
- 职位/团队多维从属关系

原因：

- 能满足壳层账号中心、用户治理和后续数据范围收口的最小闭环。
- 与当前共享环境单租户、演示账号基线匹配。
- 避免把第一优先级任务扩大成权限平台重做。

### 9.3 租户只读模型

新增最小只读支持：

- `Tenant` 实体
- `TenantMapper`

仅用于：

- 账号中心展示租户名
- `/api/auth/me` 返回租户摘要

本轮不做租户 CRUD。

### 9.4 角色数据范围

在 `sys_role` 上新增：

- `data_scope_type VARCHAR(32) NOT NULL DEFAULT 'TENANT'`

推荐枚举：

- `ALL`：全局
- `TENANT`：租户内全部
- `ORG_AND_CHILDREN`：本机构及下级
- `ORG`：仅本机构
- `SELF`：仅本人

本轮使用方式：

- `RoleView` 可维护该字段
- `PermissionServiceImpl` 在鉴权上下文中计算“最终数据范围摘要”
- 多角色场景取最宽权限，优先级：
  - `ALL`
  - `TENANT`
  - `ORG_AND_CHILDREN`
  - `ORG`
  - `SELF`

本轮暂不要求所有业务查询立即按该范围过滤，但必须把该模型正式纳入系统治理链路。

## 10. 鉴权上下文设计

`UserAuthContextVO` 需要扩展为真实账号摘要，新增字段：

- `tenantId`
- `tenantName`
- `orgId`
- `orgName`
- `nickname`
- `avatar`
- `lastLoginTime`
- `lastLoginIp`
- `dataScopeType`
- `dataScopeSummary`

保留现有字段：

- `userId`
- `username`
- `realName`
- `displayName`
- `phone`
- `email`
- `accountType`
- `authStatus`
- `loginMethods`
- `superAdmin`
- `homePath`
- `roleCodes`
- `permissions`
- `roles`
- `menus`

其中：

- `displayName` 优先级调整为 `nickname -> realName -> username`
- `accountType` 继续表达主账号/子账号，但必须来自真实字段判断，不能只靠文案推断
- `authStatus` 改成真实状态口径，不再使用“待接入”“待认证说明”之类的过渡文案

## 11. 接口设计

### 11.1 认证接口

保留：

- `POST /api/auth/login`
- `GET /api/auth/me`

改造目标：

- 返回扩展后的完整身份上下文
- 供菜单权限、壳层账号中心、角色首页和帮助/通知过滤统一消费

### 11.2 当前登录人资料接口

新增：

- `PUT /api/user/profile`

用途：

- 当前登录用户只修改自己的资料
- 仅允许编辑 `nickname / realName / phone / email / avatar`
- 不允许通过该接口修改角色、机构、状态等治理字段

### 11.3 密码接口

保留：

- `POST /api/user/change-password`

用途不变：

- 当前登录用户改密

### 11.4 用户治理接口

继续使用：

- `POST /api/user/add`
- `PUT /api/user/update`
- `GET /api/user/page`
- `GET /api/user/{id}`

但本轮补齐字段：

- `orgId`
- `roleIds`
- `nickname`
- `avatar`
- `remark`

## 12. 系统治理页面改造

### 12.1 用户管理 `UserView`

补齐：

- 所属机构筛选或展示字段
- 表单中的所属机构选择
- 角色绑定入口
- 显示名称等真实账号字段

操作列在共享规则升级后，`编辑 / 重置密码 / 删除` 应全部直出。

### 12.2 角色权限 `RoleView`

补齐：

- `dataScopeType` 维护入口
- 列表与表单中的范围摘要展示

### 12.3 组织架构 `OrganizationView`

本轮不重做组织树，但要保证：

- 用户可选主机构来自现有组织树数据
- 后续账号中心和用户页都能读取同一组织名

## 13. 受影响文件

后端重点文件：

- `spring-boot-iot-auth/src/main/java/com/ghlzm/iot/auth/service/impl/AuthServiceImpl.java`
- `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/UserAuthContextVO.java`
- `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/PermissionServiceImpl.java`
- `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/User.java`
- `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/UserServiceImpl.java`
- `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/UserController.java`
- `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/entity/Role.java`
- `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/RoleServiceImpl.java`
- `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/RoleController.java`
- 新增最小 `Tenant` 相关实体与 Mapper
- `sql/init.sql`
- `sql/init-data.sql`

前端重点文件：

- `spring-boot-iot-ui/src/components/AppHeaderTools.vue`
- `spring-boot-iot-ui/src/components/ShellAccountDrawers.vue`
- `spring-boot-iot-ui/src/composables/useShellAccountCenter.ts`
- `spring-boot-iot-ui/src/stores/permission.ts`
- `spring-boot-iot-ui/src/types/auth.ts`
- `spring-boot-iot-ui/src/types/shell.ts`
- `spring-boot-iot-ui/src/api/user.ts`
- `spring-boot-iot-ui/src/views/UserView.vue`
- `spring-boot-iot-ui/src/views/RoleView.vue`
- `spring-boot-iot-ui/src/utils/adaptiveActionColumn.ts`
- `spring-boot-iot-ui/src/components/StandardWorkbenchRowActions.vue`

文档文件：

- `README.md`
- `docs/02-业务功能与流程说明.md`
- `docs/03-接口规范与接口清单.md`
- `docs/04-数据库设计与初始化数据.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/13-数据权限与多租户模型.md`
- `docs/15-前端优化与治理计划.md`

## 14. 验证方式

### 14.1 前端

- 共享动作工具测试补齐：
  - `3` 动作全部直出
  - `4` 动作才出现 `更多`
- 壳层账号中心测试补齐：
  - `账号中心` 展示真实上下文
  - `实名认证 / 登录方式` 不再作为独立壳层动作
- `UserView` / `RoleView` 补新增字段和交互回归

### 14.2 后端

- `PermissionServiceImpl` 测试补扩展身份上下文
- `UserServiceImpl` 测试补：
  - `profile update`
  - `orgId`
  - `nickname`
  - `change password`
- 角色数据范围摘要计算补单测

### 14.3 真实环境

仍按 `application-dev.yml` 验收，不回退 H2：

- 登录后右上角账号菜单显示正确
- 账号中心可查看并修改当前用户资料
- 修改密码成功后重新登录
- 后台用户治理可维护所属机构与角色
- 后台角色治理可维护数据范围类型

## 15. 风险与控制

- 风险：补 `org_id` 和 `data_scope_type` 会触发 SQL、实体、接口和前端类型联动修改。
  - 控制：本轮明确采用“最小模型”，只补必要字段，不叠加多机构关系和全业务数据过滤。

- 风险：现有 `UserAuthContextVO` 被菜单、通知、帮助中心等多处消费，扩字段后容易出现前后端类型不一致。
  - 控制：统一以 `/api/auth/me` 和 `permissionStore` 为唯一身份上下文源，并补类型测试。

- 风险：共享动作规则改变后，多个页面操作列宽度会同步变化。
  - 控制：规则落在共享层，同时补共享列宽测试和页面快照/行为测试，避免局部回流。

- 风险：实名状态没有真实审核流，容易再次被写成假状态。
  - 控制：本轮将实名状态严格限制为“资料是否填写”这类真实可计算口径，禁止继续使用“待认证说明”。
