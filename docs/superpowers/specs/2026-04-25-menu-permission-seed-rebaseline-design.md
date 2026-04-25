# 菜单与按钮权限初始化基线重编设计

> 日期：2026-04-25
> 范围：`sys_menu`、`sys_role_menu` 初始化基础数据与对应文档/校验口径。
> 目标：按当前已实现页面、工作台入口、按钮权限与后端治理权限，重编一套完整、可复验、可重复执行的初始化权限基线。

## 背景

当前前端已经形成五大工作台、产品详情子路由、治理控制面、质量工场和设备操作抽屉等完整页面体系，但初始化菜单数据与实际页面/按钮能力存在漂移：

- 部分已实现页面只存在于前端路由或静态工作台配置中，没有在菜单数据中显式挂出，例如 `/device-access`、`/risk-disposal`、`/risk-config`、`/system-management`、`/quality-workbench`。
- 产品定义中心已经拆成 `/products/:productId/overview`、`/products/:productId/devices`、`/products/:productId/contracts`、`/products/:productId/mapping-rules`、`/products/:productId/releases` 五段子路由，但角色权限树中缺少对应页面/工作区权限表达。
- 部分页面按钮已落地但权限树缺少按钮节点，初扫确认的高可信缺口包括设备资产中心的 `iot:device-capability:view`。
- 初始化脚本中已经积累较多按钮权限 seed，但缺少统一的来源分类、排除规则和验证入口，后续继续靠手工补丁容易再次漂移。

本设计选择“全量重编型”：不是只补一个缺口，而是把页面、按钮、后端治理权限和角色授权重新整理为一套基线。

## 目标

1. 让所有当前正式页面都有菜单数据表达。登录页、重定向路由和明确标记为兼容/内部的入口可以排除，但必须有排除清单。
2. 让所有前端真实使用的按钮权限都能在角色权限树中看到，并能按角色授权。
3. 让后端治理关键写操作权限常量在 `sys_menu` 中有按钮权限节点，内部 Redis key、localStorage key、测试专用伪权限不进入权限树。
4. 重编 `sys_role_menu` 基线，保证角色拥有按钮权限时必然拥有父页面权限，拥有深层页面权限时必然拥有所属工作台路径。
5. 增加可重复执行的盘点/校验脚本，避免后续页面新增后再次出现“实现了但菜单看不到”。

## 非目标

- 不改变 `sys_menu`、`sys_role_menu` 表结构。
- 不改变当前登录、路由守卫、`PermissionService` 或前端 `permissionStore` 的鉴权机制。
- 不引入运行时自动发现菜单，也不把前端路由自动写库。
- 不修改业务页面交互或 UI 布局。
- 不使用 H2 或废弃验收链路验证菜单权限。

## 真相源

### 页面真相源

- `spring-boot-iot-ui/src/router/index.ts`：当前可访问路由全集。
- `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`：五大工作台总览、静态入口卡片、兼容入口和隐藏兼容路径。
- `sql/init-data.sql`：当前菜单 seed 与角色授权 seed。
- `docs/02-业务功能与流程说明.md`、`docs/21-业务功能清单与验收标准.md`：页面语义和交付边界。

### 按钮权限真相源

- 前端 `v-permission` 和 `permissionStore.hasPermission(...)` 字面量。
- 后端 `GovernancePermissionCodes` 与使用 `GovernancePermissionGuard` 的关键写接口。
- 当前 `sql/init-data.sql` 中已有按钮节点，作为兼容和历史授权参考。

### 排除源

以下字符串形态不得直接作为菜单权限：

- Redis key 前缀，例如 `iot:message-flow:*`、`iot:mqtt:consumer:leader`、`iot:invalid-report:*`。
- localStorage key 前缀，例如 `iot:device-capability-execute:last-params`、`iot:device-capability-execute:favorites`。
- 测试伪权限或动态测试串，例如带随机后缀的 `*:6260370286`。
- 登录页、redirect 路由、历史兼容入口中已明确隐藏且不需要角色树授权的路由。

## 菜单模型

### 目录层

保留五大一级目录作为 `type=0`：

- 接入智维
- 风险运营
- 风险策略
- 平台治理
- 质量工场

一级目录继续承担导航分组，不直接承载工作台总览 path。每个目录下新增一个显式总览页面节点，使 `/device-access`、`/risk-disposal`、`/risk-config`、`/system-management`、`/quality-workbench` 能在菜单与角色权限树中被看到。

### 页面层

页面节点使用 `type=1`，分为三类：

1. **主导航页面**：在侧边栏显示，例如产品定义中心、设备资产中心、告警运营台、角色权限等。
2. **总览页面**：工作台总览入口，例如接入智维总览、平台治理总览、质量工场总览。
3. **深链/工作区页面**：不一定在侧边栏显示，但必须进入权限树，例如产品详情五段子路由、业务验收结果页。

深链页面原则上 `visible=0,status=1`，用于授权和路由放行，不挤占主导航；主导航与总览页面 `visible=1,status=1`。

特殊路由按固定规则处理：

- `/` 是平台首页和路由守卫特例，继续不进入 `sys_menu`，但保留在排除清单中。
- `/future-lab` 若继续保留为可访问功能，则作为“演进蓝图”隐藏页面节点挂在质量工场下，`visible=0,status=1`，可授权但不进主侧边栏。
- `/automation-assets`、`/automation-test` 继续作为兼容页面节点保留，默认 `visible=0,status=1`，避免旧深链失效但不回流为主导航入口。

产品详情子路由统一挂在“产品定义中心”下，建议菜单码与权限码如下：

| 路由 | 菜单名 | 权限码 |
|---|---|---|
| `/products/:productId/overview` | 产品总览 | `iot:products:detail-overview` |
| `/products/:productId/devices` | 产品关联设备 | `iot:products:detail-devices` |
| `/products/:productId/contracts` | 契约字段 | `iot:products:detail-contracts` |
| `/products/:productId/mapping-rules` | 映射规则 | `iot:products:detail-mapping-rules` |
| `/products/:productId/releases` | 版本台账 | `iot:products:detail-releases` |

路由守卫当前按 `/products` 前缀兼容深链访问，本轮实施不改变守卫；新增页面节点主要服务权限树可见、角色授权完整和后续审计。

### 按钮层

按钮节点使用 `type=2`，统一挂到最接近的页面节点下。

按钮权限保留现有语义稳定的权限码，例如：

- 产品：`iot:products:add/update/delete/export`
- 设备：`iot:devices:add/update/delete/export/import/replace`
- 设备操作：新增 `iot:device-capability:view`
- 系统用户/角色/菜单/组织/区域/字典/渠道/站内消息/帮助文档：沿用现有 `system:*` 权限
- 治理关键写动作：沿用 `GovernancePermissionCodes` 中的 `iot:product-contract:*`、`iot:protocol-governance:*`、`risk:metric-catalog:*`、`risk:rule-definition:*`、`risk:linkage-rule:*`、`risk:emergency-plan:*`、`iot:secret-custody:*`

已有但未被前端直接使用的按钮权限不简单删除。实施时按三类处理：

1. 当前后端仍校验：保留。
2. 前端未显式控制但属于页面内审计/工作区能力：保留并补文档说明。
3. 已废弃粗粒度权限：标记 `deleted=1,status=0,visible=0`，并保留兼容说明。

## 角色授权模型

### 超级管理员

超级管理员授权所有未删除菜单与按钮节点。

### 业务人员

重点授权风险运营、业务验收和只读接入支撑：

- 风险运营总览、实时监测、GIS、告警、事件、对象洞察、运营分析。
- 质量工场中的业务验收台与结果查看。
- 产品/设备只读、导出、对象洞察跳转等必要支撑权限。
- 不授予系统治理写权限、协议治理写权限、产品契约发布/回滚权限。

### 管理人员

授权经营统筹、风险策略、平台治理和接入治理常用能力：

- 风险运营与风险策略全量页面。
- 平台治理中组织、账号、角色、导航、区域、字典、通知、帮助、审计、治理任务、治理运维、治理审批、权限与密钥治理。
- 产品/设备全量常用按钮。
- 治理关键写动作和复核类权限按当前管理职责授予。

### 运维人员

授权接入稳定性、风险处置和必要治理动作：

- 接入智维总览、无代码接入、协议治理、产品、设备、链路验证、异常观测、链路追踪、数据校验。
- 风险运营中实时监测、告警、事件、对象洞察。
- 设备操作、设备更换、导入导出、协议治理执行和必要复核权限。
- 不授予用户/角色/导航编排的管理写权限。

### 开发人员

授权联调、排障、质量工场和治理开发支撑：

- 接入智维全量诊断页。
- 产品契约治理、规范库维护、协议治理、映射规则与运行态显示治理相关权限。
- 质量工场全量研发资产、执行与结果页面。
- 风险策略写动作可按现有基线保留，但不默认授予系统账号/角色管理写权限。

## 数据流

1. 盘点脚本读取前端路由、工作台静态配置、前端权限字面量、后端治理权限常量和现有 SQL seed。
2. 脚本输出页面清单、按钮清单、排除清单和差异清单。
3. 人工维护一份菜单编排映射：菜单名、父级、排序、是否侧边栏可见、组件名、权限码、角色授权策略。
4. 根据映射重写 `sql/init-data.sql` 中 `sys_menu` 与 `sys_role_menu` 相关 seed。
5. 再次执行盘点脚本，确认无未分类路由、无未分类权限、无父子授权断裂。

## 错误处理与兼容

- 脚本遇到无法归类的路由或权限码时直接失败，输出文件路径与行号，不自动猜父级。
- 对动态路由必须写明 canonical path，例如 `/products/:productId/contracts`；授权检查仍沿用当前前缀放行逻辑。
- 现有兼容入口 `/automation-assets`、`/automation-test` 保留为兼容页面节点，但可设置为非主导航显示。
- `risk-enhance` 这类 redirect 入口不作为独立正式页面节点。
- 清理废弃权限时只软删除 seed 节点，不物理删除历史真实库数据。

## 验证

新增或整理一个菜单权限盘点脚本，至少校验：

1. `router/index.ts` 中除登录页、redirect 和排除清单外的路由，都有 `sys_menu` 页面节点或明确父页面放行说明。
2. `sectionWorkspaces.ts` 中所有工作台总览和卡片入口，都有菜单节点。
3. 前端 `v-permission` / `hasPermission` 中的业务权限码，都有 `type=2` 按钮节点。
4. `GovernancePermissionCodes` 中所有关键写权限，都有按钮节点。
5. 所有角色拥有按钮节点时，也拥有对应父页面节点。
6. 所有角色拥有深链页面节点时，也拥有所属一级工作台或主页面节点。
7. `sql/init-data.sql` 重复执行后不产生重复授权行，`ON DUPLICATE KEY UPDATE` 与变量式授权仍保持幂等。

真实环境验证继续使用 `application-dev.yml`，不启用 H2 回退链路。

## 文档同步

实施阶段至少同步更新：

- `docs/02-业务功能与流程说明.md`：更新菜单/工作台与角色可见入口口径。
- `docs/04-数据库设计与初始化数据.md`：更新 `sql/init-data.sql` 菜单权限 seed 说明。
- `docs/08-变更记录与技术债清单.md`：记录本次权限基线重编与残留排除项。
- `README.md` 与 `AGENTS.md`：检查是否需要更新当前菜单权限基线说明；若只改 seed 细节且入口规则不变，可记录为无需修改。

## 风险与约束

- 全量重编 `sys_role_menu` 会影响不同角色登录后的菜单可见性，必须用初始化脚本和真实环境账号复核。
- 深链页面如果设为 `visible=1` 会造成侧边栏过重，因此动态详情页默认只进入权限树，不进入主导航。
- 前端权限字面量中混有 Redis key、localStorage key 和测试伪权限，必须经过排除清单过滤。
- 当前工作区已有外部未提交修改，实施时只纳入本任务相关文件，不回滚或格式化无关文件。
