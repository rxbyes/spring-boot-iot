# 固定系统级复核人设计

## 1. 背景

当前 `/products` 产品经营工作台在提交以下治理动作时，要求执行人手工填写 `复核人用户 ID`：

- 产品契约字段发布审批
- 产品契约字段回滚审批

现状问题：

1. 一线用户需要反复填写 `userId`，操作成本高，且容易输错。
2. 复核人是技术实现细节，不应要求业务用户理解和维护。
3. 当前审批链路已经沉淀到 `sys_governance_approval_order`，继续把复核人暴露为前端输入，和“治理控制面统一留痕”的设计目标冲突。
4. 后续若扩展多租户，单纯在前端传一个裸 `approverUserId` 会让审批策略分散到页面，不利于平台治理。

目标：

1. 保留双人复核和审批留痕。
2. 取消产品工作台中的“复核人用户 ID”手工输入。
3. 由系统统一托管固定复核人，并由后端在提交审批时自动解析。
4. 当前满足“全系统唯一固定 1 人负责物模型确认并提交审批”的业务诉求。
5. 为未来多租户或更多治理动作扩展保留兼容路径。

## 2. 现状约束

### 2.1 当前审批写侧

- `ProductModelController.applyGovernance(...)` 通过请求头 `X-Governance-Approver-Id` 接收复核人。
- `ProductContractReleaseController.rollbackBatch(...)` 同样通过请求头 `X-Governance-Approver-Id` 接收复核人。
- `GovernancePermissionGuard.requireDualControl(...)` 强制要求执行人与复核人不能是同一账号。
- `GovernanceApprovalServiceImpl` 将 `approver_user_id` 落入审批主单，并在审批通过/驳回时校验“仅当前复核人可执行”。

### 2.2 当前前端

- `ProductModelDesignerWorkspace.vue` 内部维护 `reviewer/approverUserId` 输入框。
- 提交审批、回滚审批、重新提交审批都依赖该输入值。

### 2.3 当前租户与权限基线

- `sys_user`、`sys_role`、`sys_governance_approval_order` 均带 `tenant_id`。
- `GovernanceApprovalQueryServiceImpl` 对普通账号按 `tenant_id` 过滤审批单。
- `SUPER_ADMIN` 在审批读侧不受租户过滤限制，可跨租户查看审批单。

结论：

1. “全系统唯一固定 1 人”不能通过前端写死 `userId` 来实现。
2. 若要在未来多租户场景中仍维持单一固定复核人，这个账号必须具备系统级治理身份，当前最稳妥的实现是 `SUPER_ADMIN`。
3. 更适合的平台实现不是“页面传参”，而是“后端审批策略解析”。

## 3. 方案选型

### 3.1 方案 A：代码写死固定 `approverUserId`

做法：

- 在控制器或服务中直接写死某个 `userId` 作为复核人。

优点：

- 开发量最小。

缺点：

- 强依赖环境数据，迁移环境时极易失效。
- 无法表达动作级治理策略。
- 与多租户、环境初始化、账号替换不兼容。

结论：

- 不采用。

### 3.2 方案 B：仅在数据库保存一个全局固定 `approverUserId`

做法：

- 在某处配置中保存一个固定用户主键，提交审批时读取。

优点：

- 比硬编码稳定。
- 可以移除前端输入。

缺点：

- 只能表达“一个固定人”，无法表达“哪个动作用哪个固定人”。
- 后续若扩到更多治理动作，需要继续叠加散落逻辑。

结论：

- 可用，但扩展性一般。

### 3.3 方案 C：引入治理审批策略表，当前只启用“全局固定复核人”

做法：

- 新增 `sys_governance_approval_policy`，按动作定义复核策略。
- 当前先仅支持 `approver_mode=FIXED_USER`。
- 当前只落全局策略，后续可平滑扩展租户级覆盖。

优点：

- 满足“全系统唯一固定 1 人”。
- 取消前端手输 `userId`。
- 后续可扩展到更多治理动作和多租户覆盖。
- 复核人仍然真实落库到审批单，不影响审计和治理控制面。

缺点：

- 比简单硬编码多一张表和一层解析逻辑。

结论：

- 采用本方案。

## 4. 目标设计

### 4.1 业务语义

当前仅对以下动作启用系统固定复核人：

- `PRODUCT_CONTRACT_RELEASE_APPLY`
- `PRODUCT_CONTRACT_ROLLBACK`

业务规则：

1. 产品经营工作台不再要求用户填写复核人用户 ID。
2. 系统在提交审批时自动选择固定复核人。
3. 固定复核人是系统级专职人员，当前必须使用具备 `SUPER_ADMIN` 身份的账号。
4. 固定复核人仍然在审批单中保存为真实 `approver_user_id`。
5. 执行人与固定复核人不得为同一账号。

### 4.2 数据模型

新增表：`sys_governance_approval_policy`

建议字段：

- `id`
- `tenant_id`
- `scope_type`
- `action_code`
- `approver_mode`
- `approver_user_id`
- `enabled`
- `remark`
- `create_by`
- `create_time`
- `update_by`
- `update_time`
- `deleted`

字段语义：

- `scope_type`：`GLOBAL` / `TENANT`
- `tenant_id`：
  - `GLOBAL` 时固定为 `0`
  - `TENANT` 时为对应租户 ID
- `action_code`：审批动作编码
- `approver_mode`：当前仅支持 `FIXED_USER`
- `approver_user_id`：固定复核人账号 ID
- `enabled`：是否启用

唯一约束建议：

- `uk_governance_approval_policy_scope_action (scope_type, tenant_id, action_code, deleted)`

索引建议：

- `idx_governance_approval_policy_enabled (enabled, scope_type, tenant_id, action_code, deleted)`
- `idx_governance_approval_policy_approver (approver_user_id, enabled, deleted)`

### 4.3 策略解析规则

新增只读服务：`GovernanceApprovalPolicyResolver`

入参：

- `actionCode`
- `operatorUserId`

解析顺序：

1. 取执行人的 `tenantId`
2. 先查 `TENANT + 当前 tenantId + actionCode + enabled=1`
3. 未命中则查 `GLOBAL + tenant_id=0 + actionCode + enabled=1`
4. 若仍未命中，返回明确业务错误

当前初始化策略：

- `GLOBAL / PRODUCT_CONTRACT_RELEASE_APPLY / FIXED_USER / <固定复核人>`
- `GLOBAL / PRODUCT_CONTRACT_ROLLBACK / FIXED_USER / <固定复核人>`

说明：

- 虽然当前业务要求“全系统唯一固定 1 人”，但表结构仍保留 `TENANT` 作用域，以便后续多租户扩展时不推翻本次设计。
- 当前功能上线时不开放前端维护页，仍由初始化 SQL 或 DBA 运维脚本维护该策略。

### 4.4 固定复核人校验规则

解析出 `approverUserId` 后，提交审批前必须校验：

1. 复核人存在且未删除。
2. 复核人账号启用。
3. 复核人具备 `SUPER_ADMIN` 角色。
4. 复核人具备当前动作要求的审批权限。
5. 复核人不等于当前执行人。

失败时返回明确错误：

- `未配置固定复核策略`
- `固定复核人不存在或已停用`
- `固定复核人缺少系统级审批权限`
- `当前执行人不能作为本次固定复核人`

### 4.5 后端接口变化

受影响接口：

- `POST /api/device/product/{productId}/model-governance/apply`
- `POST /api/device/product/contract-release-batches/{batchId}/rollback`
- `POST /api/system/governance-approval/{orderId}/resubmit`

调整方式：

1. 产品契约发布申请和回滚申请
   - 不再要求请求头 `X-Governance-Approver-Id`
   - 控制器内部调用 `GovernanceApprovalPolicyResolver` 自动解析固定复核人
2. 审批重提
   - 产品工作台不再要求页面传 `approverUserId`
   - 新增后端“按原动作重提并自动解析固定复核人”的入口，或在现有 `resubmit` 服务中允许缺省 `approverUserId` 时自动补齐

推荐做法：

- 保持系统审批台现有通用 `resubmit` 接口不变
- 仅在 `/products` 工作台使用的产品契约相关重提路径中封装专用后端接口
- 避免把“自动选择固定复核人”的语义污染到所有治理动作

推荐新增接口：

- `POST /api/device/product/governance-approval/{orderId}/resubmit`

行为：

1. 读取原审批单
2. 校验该审批单属于产品契约发布或回滚动作
3. 自动解析固定复核人
4. 调用现有 `GovernanceApprovalService.resubmitOrder(...)`

### 4.6 前端交互变化

`ProductModelDesignerWorkspace.vue` 调整：

1. 删除 `复核人用户 ID` 输入框。
2. 删除前端对 `approverUserId` 的本地校验和状态维护。
3. 发布审批、回滚审批、重提审批改为无参提交。
4. 在审批区展示只读信息：
   - `固定复核机制：系统自动分配`
   - 如接口返回了复核人摘要，则展示 `当前固定复核人：<姓名/账号>`

前端不再承担：

- 复核人选择
- 复核人 ID 合法性校验
- 复核人与执行人是否相同的判定

这些逻辑统一下沉到后端。

### 4.7 审批单与审计保持不变

以下语义必须保留：

1. `sys_governance_approval_order.approver_user_id` 继续保存真实复核人。
2. `sys_governance_approval_transition` 继续保存流转轨迹。
3. 治理审批台继续按审批单事实展示，而不是按“系统默认复核人”推断。
4. `releaseBatchId`、`rollback result`、风险指标目录联动等现有回执链路不受影响。

## 5. 数据流

### 5.1 契约字段发布审批

1. 执行人在 `/products` 点击“确认并提交审批”
2. 前端仅提交 apply payload
3. 后端根据动作 `PRODUCT_CONTRACT_RELEASE_APPLY` 解析固定复核策略
4. 后端校验固定复核人有效
5. 后端调用 `GovernanceApprovalService.submitAction(...)`
6. 审批单保存真实 `approver_user_id`
7. 前端刷新审批详情并展示状态

### 5.2 契约字段回滚审批

1. 执行人在 `/products` 点击“提交回滚审批”
2. 前端仅提交目标批次
3. 后端根据动作 `PRODUCT_CONTRACT_ROLLBACK` 解析固定复核策略
4. 后端保存审批单
5. 审批通过后按原有链路执行回滚

### 5.3 审批重提

1. 执行人在 `/products` 点击“重新提交”
2. 后端读取原审批单动作
3. 按该动作重新解析固定复核策略
4. 用解析结果调用重提服务

## 6. 多租户扩展策略

当前业务要求是“全系统唯一固定 1 人”，但设计需要兼容未来多租户：

1. 当前先只写 `GLOBAL` 策略。
2. 固定复核人账号必须是 `SUPER_ADMIN`，保证可跨租户访问审批读侧。
3. 后续若某个租户需要独立复核人，只需新增 `TENANT` 级策略，不需要修改前端和审批主链路。
4. 策略解析顺序固定为 `TENANT -> GLOBAL`，因此今天的“全局唯一人”不会阻断明天的“个别租户覆盖”。

不做的事：

1. 本轮不新增“策略管理页面”。
2. 本轮不把所有治理审批动作都改成固定复核人。
3. 本轮不改变治理审批台本身的通用审批交互。

## 7. 兼容与迁移

### 7.1 初始化数据

`sql/init.sql`：

- 新增 `sys_governance_approval_policy`

`sql/init-data.sql`：

- 插入固定复核人账号
  - 若现有 `admin` 已承担系统总控角色，可直接复用 `user_id=1`
  - 若业务要求专职账号，新增例如 `governance_reviewer`
- 插入两条全局策略

推荐：

- 新增专职系统账号 `governance_reviewer`
- 赋予 `SUPER_ADMIN` 或明确的系统级治理审批权限
- 将产品契约动作统一绑定到该账号

原因：

- 业务语义更清晰
- 便于审计区分“平台总管账号”和“专职复核账号”

### 7.2 接口兼容

为了降低联调风险，可采用两步兼容：

1. 第一阶段
   - 请求头 `X-Governance-Approver-Id` 仍兼容读取
   - 若未传则自动走固定复核策略
2. 第二阶段
   - 前端全部切换后，移除产品工作台对此头的依赖

本次实现建议直接完成第一阶段兼容，这样：

- 不影响治理审批台或其他潜在调用方
- `/products` 可以立即切到“无输入框”模式

## 8. 错误处理

新增业务错误文案建议：

- `当前动作未配置固定复核策略`
- `固定复核人不存在或已被删除`
- `固定复核人已停用`
- `固定复核人缺少系统级审批权限`
- `当前执行人不能同时作为固定复核人`
- `当前审批单不支持固定复核人自动重提`

前端展示原则：

1. 不再为复核人输入错误弹窗。
2. 仅展示后端返回的策略或账号异常。
3. 错误提示只弹一次，不重复追加通用“系统繁忙”。

## 9. 测试策略

### 9.1 后端单测

新增或扩展以下覆盖：

1. 固定策略命中全局配置时，发布审批提交成功
2. 固定策略命中全局配置时，回滚审批提交成功
3. 未配置策略时提交失败
4. 固定复核人不存在/停用/缺权限时提交失败
5. 执行人等于固定复核人时提交失败
6. 产品契约审批重提时，自动重新解析固定复核人
7. `TENANT` 策略优先于 `GLOBAL`

### 9.2 前端单测

1. 工作台不再渲染复核人输入框
2. 发布审批提交时不再传 `approverUserId`
3. 回滚审批提交时不再传 `approverUserId`
4. 重提审批时不再要求填写复核人
5. 后端返回固定复核策略异常时，页面展示明确错误

### 9.3 真实环境验收

1. 在 `codex/dev` 启动 `application-dev.yml`
2. 使用 `/products` 对一个产品发起契约发布审批
3. 确认无需输入复核人 ID
4. 在治理审批台确认审批单 `approver_user_id` 已自动写入固定复核人
5. 执行回滚审批并复验同样行为
6. 复验驳回后重提

## 10. 风险与控制

风险：

1. 固定复核人被停用或权限漂移后，产品审批会整体不可用。
2. 若复用 `admin`，容易让“平台总控”和“专职复核”审计语义混淆。
3. 若未来其他治理动作也跟进固定复核人，容易扩成“隐式全局逻辑”。

控制：

1. 用独立策略表，而不是代码硬编码。
2. 推荐使用专职系统账号，而不是复用 `admin`。
3. 只在产品契约发布/回滚场景启用，其他动作保持原行为。
4. 在错误文案中明确指出是“固定复核策略异常”。

## 11. 最终决策

本次采用：

1. 新增 `sys_governance_approval_policy`
2. 后端自动解析固定复核人
3. 当前只覆盖产品契约发布与回滚审批
4. 前端移除复核人手工输入
5. 固定复核人使用系统级专职账号，要求具备 `SUPER_ADMIN` 身份
6. 数据模型保留 `TENANT -> GLOBAL` 扩展能力，但当前只启用 `GLOBAL`

这是当前最稳妥的实现，既满足“全系统唯一固定 1 人”的业务诉求，也不会把后续多租户演进锁死在页面传参或硬编码账号上。
