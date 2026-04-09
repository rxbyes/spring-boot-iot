# 权限与密钥治理缺口收口设计

## 背景

当前仓库已经完成了第一版治理关键写权限、双人复核、审批主单与设备密钥轮换写链路：

- `GovernancePermissionCodes` 已定义规范库、合同发布/回滚、风险指标标注、阈值/联动/预案、密钥托管等权限码。
- `GovernancePermissionGuard` 已在合同发布/回滚、阈值/联动/预案写接口和设备密钥轮换链路执行权限校验与双人复核。
- `sys_governance_approval_order` / `sys_governance_approval_transition` 已承接合同发布与回滚审批状态机。
- `iot_device_secret_rotation_log` 已承接密钥轮换写侧留痕，并同步写入 `sys_audit_log`。

但仍有三个明显缺口没有收口：

1. 角色矩阵只体现在权限常量、SQL 种子和零散文档里，系统缺少稳定读侧去回答“谁能做什么、谁来复核什么”。
2. 密钥托管只有写接口，没有读侧台账，无法稳定复盘历史轮换批次。
3. 审计日志虽已记录关键动作，但治理侧缺少统一语义字段与模块口径，跨动作复盘仍需人工拼接。

本轮目标是补齐这三个缺口，同时保持现有审批状态机边界不扩散。

## 目标

- 新增统一的治理权限矩阵读侧，稳定输出执行/复核角色矩阵。
- 新增密钥托管读侧，分页查询设备密钥轮换批次。
- 统一关键治理动作审计口径，补齐动作类型、主体、执行人、复核人、结果摘要。
- 同步更新 SQL 种子和交付文档，使真实环境和文档口径一致。

## 非目标

- 本轮不把阈值、联动、预案、密钥轮换升级到 `governance-approval` 审批状态机。
- 本轮不新增明文密钥查看能力。
- 本轮不新增新的独立治理大页面；前端只做轻量入口与表格能力补齐。

## 方案对比

### 方案 A：缺口收口型，推荐

只新增读侧与审计统一化，保持现有写链路与审批边界不变。

优点：

- 风险最小，不会冲击现有合同审批链路。
- 与本轮用户目标完全对齐。
- 可以直接复用已有权限码、轮换日志表与审计表。

缺点：

- 阈值/联动/预案/密钥轮换仍然是“请求头双人复核后立即执行”。

### 方案 B：密钥治理升级型

在方案 A 基础上，把密钥轮换纳入审批主单。

优点：

- 密钥治理模型更统一。

缺点：

- 会新增审批动作、执行器、读侧适配和更多真实环境验收面，超出本轮范围。

### 方案 C：全量统一审批型

把阈值、联动、预案、密钥轮换全部纳入审批主单。

优点：

- 模型最一致。

缺点：

- 范围过大，已转化为新的主线项目，不适合作为当前缺口收口任务。

结论：采用方案 A。

## 架构设计

### 1. 权限矩阵读侧

在 `spring-boot-iot-system` 新增治理权限矩阵读服务和控制器接口：

- `GET /api/system/governance/permission-matrix`

接口直接返回固定治理动作清单，每个动作明确：

- `domainCode`
- `domainName`
- `actionCode`
- `actionName`
- `operatorPermissionCode`
- `approverPermissionCode`
- `defaultRoleCodes`
- `defaultApproverRoleCodes`
- `dualControlRequired`
- `auditModule`

该接口不做数据库真相表。矩阵真相继续来自：

- `GovernancePermissionCodes`
- `sql/init-data.sql` 的权限种子
- 固定角色编码映射

理由：

- 当前权限模型仍是菜单/按钮权限驱动，新增专用矩阵表会造成双真相。
- 本轮只需要稳定读侧回答“谁可以做什么”，不需要在线编辑矩阵。

### 2. 密钥托管读侧

在 `spring-boot-iot-device` 新增密钥托管读接口：

- `GET /api/device/secret-rotation-logs`

读取 `iot_device_secret_rotation_log`，支持按以下条件分页：

- `deviceCode`
- `productKey`
- `rotationBatchId`
- `rotatedBy`
- `approvedBy`
- `beginTime`
- `endTime`

返回字段限定为：

- `id`
- `deviceId`
- `deviceCode`
- `productKey`
- `rotationBatchId`
- `reason`
- `previousSecretDigest`
- `currentSecretDigest`
- `rotatedBy`
- `approvedBy`
- `rotateTime`

读侧必须只暴露摘要，不返回任何明文密钥或可逆材料。

### 3. 审计口径统一

本轮不改 `sys_audit_log` 表结构，统一通过已有字段收口治理语义：

- `operation_module`：固定为治理域模块，如 `device-secret-custody`、`governance-approval`、`risk-rule-definition`
- `operation_method`：固定写动作方法
- `request_params`：以稳定 JSON 结构写入主体与复核信息
- `result_message`：短摘要
- `trace_id` / `device_code` / `product_key`：尽可能沿用已有字段

密钥轮换写链路需要从当前手拼 JSON 升级为稳定 JSON 序列化，字段固定包括：

- `governanceAction`
- `rotationBatchId`
- `approverUserId`
- `reason`
- `dualControl`

同时，权限矩阵读侧要给出 `auditModule`，让前端和文档能把矩阵项与审计域直接对齐。

### 4. 前端表达

本轮前端只补轻量工作台，不新增独立重型治理系统：

- 在现有治理相关工作区中增加“权限与密钥治理”入口
- 页面展示两块：
  - 治理权限矩阵表
  - 密钥轮换记录表

矩阵表回答“谁能执行、谁能复核”；轮换表回答“什么时候、谁执行、谁复核、为什么轮换”。

### 5. SQL 与文档

`sql/init-data.sql` 继续作为角色矩阵默认种子来源，需要明确：

- 哪些角色默认拥有执行权限
- 哪些角色默认拥有复核权限

本轮不新增新的权限码，但会补充：

- 角色矩阵说明性种子文案
- 页面或菜单入口权限

文档至少同步更新：

- `docs/03-接口规范与接口清单.md`
- `docs/04-数据库设计与初始化数据.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/21-业务功能清单与验收标准.md`

必要时补充：

- `README.md`
- `AGENTS.md`

## 数据流

### 权限矩阵

1. 前端请求 `/api/system/governance/permission-matrix`
2. 后端按固定矩阵模板聚合权限码与默认角色映射
3. 返回矩阵项给页面与文档对齐展示

### 密钥轮换台账

1. 设备密钥轮换继续走现有 `POST /api/device/{id}/secret-rotate`
2. 服务侧写入 `iot_device_secret_rotation_log` 与 `sys_audit_log`
3. 前端通过 `/api/device/secret-rotation-logs` 读取台账分页

## 错误处理

- 权限矩阵读侧若配置缺失，返回业务错误，禁止静默回空矩阵。
- 密钥台账查询参数非法时返回标准 `BizException`。
- 读侧在无数据时返回空分页，不视为异常。
- 任何读侧都不得回退到硬编码假数据。

## 测试策略

后端：

- `GovernancePermissionMatrixController/Service` 单测
- `DeviceSecretRotationLogController/Service` 单测
- `DeviceSecretCustodyServiceImplTest` 扩充，验证审计参数稳定 JSON

前端：

- 权限与密钥治理视图或工作台定向测试

验收：

- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests`
- 本轮定向单测
- 真实 `dev` smoke：
  - `POST /api/auth/login`
  - `GET /api/system/governance/permission-matrix`
  - `GET /api/device/secret-rotation-logs?pageNum=1&pageSize=10`

## 风险与取舍

- 角色矩阵读侧继续依赖固定模板，而不是数据库真相表；优点是简单稳，代价是未来若要在线编辑矩阵仍需再做一轮模型升级。
- 密钥台账仍只提供摘要，不支持明文查看；这是有意的安全边界，不视为缺陷。
- 阈值/联动/预案/密钥轮换未纳入审批状态机，这一结论需要在文档里明确，避免被误解为“已统一审批化”。
