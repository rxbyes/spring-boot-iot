# 共享 dev 环境身份治理 schema sync 与登录恢复设计

> 日期：2026-04-02
> 范围：`scripts/run-real-env-schema-sync.py`、`docs/04-数据库设计与初始化数据.md`、`docs/07-部署运行与配置说明.md`、`docs/真实环境测试与验收手册.md`、`docs/08-变更记录与技术债清单.md`
> 主题：补齐共享 dev 环境身份治理字段缺口，恢复登录与鉴权上下文链路，并把真实环境处理口径同步回权威文档
> 状态：设计已确认，待书面 spec 审阅

## 1. 背景

2026-04-02 共享 `dev` 环境登录联调时，浏览器表面现象是“点击登录后停留在登录页”，后端真实报错为：

- `POST /api/auth/login`
- `Unknown column 'org_id' in 'field list'`
- 触发位置：`UserMapper` 查询 `sys_user`

进一步排查后，已经确认：

- 当前代码中的 `User` 实体已新增 `orgId` 映射，`BaseMapper` 查询 `sys_user` 时会自动选择 `org_id`
- 当前代码中的 `PermissionServiceImpl` 已依赖 `Role.dataScopeType` 组装 `UserAuthContextVO`
- `sql/init.sql` 与 `sql/init-data.sql` 已把 `sys_user.org_id`、`sys_role.data_scope_type` 纳入基线
- 但 `scripts/run-real-env-schema-sync.py` 尚未覆盖这两项身份治理字段

这意味着仓库内“代码基线 / 初始化 SQL 基线 / 真实环境补齐脚本基线”已经出现分叉。只要共享库缺少任一身份治理字段，登录、`/api/auth/me` 或账号中心都会继续暴露 `500`。

## 2. 目标

- 让共享 `dev` 环境通过仓库内现有 `schema sync` 入口自动补齐身份治理本轮必需字段
- 恢复当前代码基线下的登录链路与鉴权上下文链路，包括 `POST /api/auth/login`、`GET /api/auth/me` 和依赖 `authContext` 的前端登录跳转
- 把真实环境处理方式原位同步到运行说明、数据库文档、验收手册和变更记录，避免再次出现“代码已升级、共享库未对齐、排障文档也未提示”的断层

## 3. 非目标

- 不给后端新增“缺列自动兼容”业务逻辑
- 不回退 `User.orgId`、`Role.dataScopeType` 或 `UserAuthContextVO` 现有代码基线
- 不引入 Flyway、Liquibase 或新的平行迁移机制
- 不扩展为“大而全”的身份治理全量迁移，只收口到本次登录和鉴权上下文必需字段
- 不使用 H2 或其他已废弃验收路径替代共享 `dev` 环境处理

## 4. 当前事实与根因

### 4.1 登录失败的真实故障链路

当前登录失败不是浏览器扩展错误导致，而是后端数据库 schema 与代码不一致：

1. 前端调用 `POST /api/auth/login`
2. `AuthServiceImpl#resolveLoginUser` 调用 `UserServiceImpl#getByUsername`
3. `UserMapper.selectOne(...)` 因 `sys_user` 缺少 `org_id` 报 SQL 语法错误
4. 全局异常处理器返回 `500`
5. 前端停留在登录页并提示失败

### 4.2 登录成功后仍有潜在下一跳风险

即使只补 `sys_user.org_id`，登录链路仍会继续组装 `authContext`：

- `PermissionServiceImpl#getUserAuthContext` 依赖 `Role.dataScopeType`
- `UserAuthContextVO` 已承载 `orgId`、`orgName`、`dataScopeType`、`dataScopeSummary`
- `/api/auth/me` 与账号中心后续读取同样依赖该上下文

因此本轮不能只修 `sys_user.org_id`，还必须同步补齐 `sys_role.data_scope_type`，否则登录或后续鉴权上下文仍会在下一处缺列上失败。

### 4.3 现有环境补齐入口的缺口

`scripts/run-real-env-schema-sync.py` 当前已覆盖若干真实环境常见缺口，但文档和脚本都还未纳入身份治理本轮新增字段：

- `sys_user.org_id`
- `sys_role.data_scope_type`

所以当前最佳修复点不是业务代码，而是共享 `dev` 环境 schema sync 脚本。

## 5. 方案决策

采用“扩展现有真实环境 schema sync 脚本，并同步更新权威文档”的方案。

不采用的两条路径：

- 手工 SQL 热修复但不更新脚本：恢复快，但不能形成可重复执行的仓库基线
- 代码侧做缺列兼容：会把真实环境 schema 漂移固化为长期技术债，不符合当前项目“真实环境作为验收基线”的规则

## 6. Schema Sync 设计

### 6.1 `sys_user.org_id`

脚本在检测到 `sys_user` 存在但缺少 `org_id` 时，补齐：

- `org_id BIGINT DEFAULT NULL COMMENT '主机构ID'`
- 对应索引 `idx_user_org_id`

设计约束：

- 默认值保持 `NULL`
- 不对历史账号强行推断主机构
- 与 `sql/init.sql` 中当前表结构保持一致

这样可以保证：

- `UserMapper` 查询 `sys_user` 不再因缺列失败
- `UserAuthContextVO` 能正常承载 `orgId / orgName`
- 后续账号中心、组织归属展示与权限扩展继续沿用当前代码口径

### 6.2 `sys_role.data_scope_type`

脚本在检测到 `sys_role` 缺少 `data_scope_type` 时，补齐：

- `data_scope_type VARCHAR(32) NOT NULL DEFAULT 'TENANT' COMMENT '数据范围类型'`

补列后执行最小角色回填，口径与当前身份治理设计一致：

- `SUPER_ADMIN -> ALL`
- `MANAGEMENT_STAFF -> ORG_AND_CHILDREN`
- `BUSINESS_STAFF -> SELF`
- `OPS_STAFF -> TENANT`
- `DEVELOPER_STAFF -> TENANT`

未命中的历史角色保留默认 `TENANT`。

这样可以保证：

- `PermissionServiceImpl#resolveHighestScope` 有稳定输入
- `/api/auth/login` 返回的 `authContext` 不会因角色缺字段再报错
- `/api/auth/me` 与账号中心展示的数据范围摘要保持当前设计语义

### 6.3 幂等与回填策略

脚本仍然保持“只补缺口、不重建、不粗暴覆盖”的原则：

- 缺列时才补列
- 已存在的字段不重复执行 `ALTER`
- 角色范围回填只处理空值、默认值或明确需要按角色编码对齐的记录，不覆盖已经人工配置过的非空业务值

这样可以让脚本安全地多次运行，并继续服务共享环境反复校准场景。

### 6.4 范围边界

本轮 schema sync 只纳入与登录和鉴权上下文直接相关的身份治理字段：

- `sys_user.org_id`
- `sys_role.data_scope_type`

不顺手继续扩到更多身份治理未来字段，避免把当前修复任务变成宽泛迁移工程。

## 7. 文档同步设计

### 7.1 运行说明

在 `docs/07-部署运行与配置说明.md` 中更新 `run-real-env-schema-sync.py` 覆盖范围说明，明确脚本已可补齐：

- `sys_user.org_id`
- `sys_role.data_scope_type`

并强调当共享 `dev` 环境登录或鉴权上下文因缺列报错时，应优先执行该脚本对齐 schema。

### 7.2 数据库文档

在 `docs/04-数据库设计与初始化数据.md` 中补齐字段说明：

- `sys_user` 增加 `org_id` 说明
- `sys_role` 增加 `data_scope_type` 说明

确保数据库权威文档与 `sql/init.sql`、实体类、共享环境修复路径一致。

### 7.3 验收手册

在 `docs/真实环境测试与验收手册.md` 中补充登录/鉴权场景排障口径：

- 若 `POST /api/auth/login` 报 `Unknown column 'org_id' in 'field list'`，说明共享库尚未对齐身份治理字段
- 若 `/api/auth/me` 或账号中心相关接口报 `data_scope_type` 缺失，也应走同一 schema sync 脚本处理

这样测试、联调和交付人员能直接把“前端登录失败”映射到真实的后端 schema 原因。

### 7.4 变更记录

在 `docs/08-变更记录与技术债清单.md` 中记录本轮有效变更：

- 共享 `dev` 环境身份治理 schema sync 已补齐
- 登录与鉴权上下文恢复依赖该脚本对齐
- 本轮没有引入代码级旧库兼容分支，而是回到真实环境基线治理

## 8. 数据流与恢复路径

补齐 schema 后，登录恢复路径应回到当前代码既定流程：

1. 前端提交 `POST /api/auth/login`
2. `UserMapper` 正常读取 `sys_user`，返回带 `orgId` 的 `User`
3. `AuthServiceImpl` 完成密码校验与最近登录信息更新
4. `PermissionServiceImpl` 正常读取角色并计算 `dataScopeType`
5. 后端返回包含 `authContext` 的登录结果
6. 前端 `permissionStore.login(data)` 成功写入登录态
7. 路由跳转离开 `/login`
8. 后续 `GET /api/auth/me`、账号中心等继续复用同一鉴权上下文

## 9. 风险与错误处理

### 9.1 风险

- 若共享环境还缺少其他身份治理字段，可能在本轮补齐后暴露下一处问题
- 若历史角色编码与当前默认回填口径不一致，个别角色的数据范围摘要可能需要后续人工核对
- 若环境不可达或账号权限不足，真实环境对齐会被阻塞

### 9.2 处理原则

- 优先报告真实环境阻塞，不回退到 H2 或废弃链路
- 若脚本执行后仍有缺列，再按实际报错追加最小补齐，不预先扩展过大范围
- 文档中明确“共享库 schema 必须与当前代码和初始化基线一致”，避免再次把问题误判为前端故障

## 10. 验证设计

### 10.1 静态一致性验证

在实现阶段需要先确认三处口径一致：

- `sql/init.sql` / `sql/init-data.sql`
- Java 实体与鉴权上下文代码
- `run-real-env-schema-sync.py` 与权威文档

### 10.2 真实环境验证

在共享 `dev` 环境执行 `python scripts/run-real-env-schema-sync.py` 后，至少验证：

- `POST /api/auth/login`
- `GET /api/auth/me`
- 浏览器登录后能正常离开登录页
- 账号中心不再因身份治理字段缺失报 `500`

### 10.3 阻塞口径

如果共享 `dev` 环境不可访问、脚本执行权限受限或数据库账号不允许 `ALTER TABLE`，必须明确报告环境阻塞，不得使用本地替代链路冒充真实环境验收结果。

## 11. 受影响模块与文档

受影响模块：

- `spring-boot-iot-system`
- `spring-boot-iot-auth`

本轮实现入口：

- `scripts/run-real-env-schema-sync.py`

需要同步更新的权威文档：

- `docs/04-数据库设计与初始化数据.md`
- `docs/07-部署运行与配置说明.md`
- `docs/真实环境测试与验收手册.md`
- `docs/08-变更记录与技术债清单.md`

当前判断 `README.md` 与 `AGENTS.md` 不一定需要改动；实现完成后仍需按“行为/流程/配置是否变化”再做一次最终检查。
