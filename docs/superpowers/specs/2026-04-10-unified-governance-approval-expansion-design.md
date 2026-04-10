# 统一审批化扩展设计

**日期**: 2026-04-10

**目标**: 把当前仍停留在“请求头双人复核后立即执行”的四类关键治理写动作——阈值规则、联动规则、应急预案、设备密钥轮换——统一升级到现有 `governance-approval` 审批状态机，形成和合同发布/回滚一致的提交、审批、执行、审计、复盘闭环。

## 1. 背景与当前问题

截至当前版本，平台已经完成以下统一治理能力：

- 合同发布与合同回滚已进入 `sys_governance_approval_order + sys_governance_approval_transition` 状态机。
- 控制面已具备 `/governance-approval`、`/governance-task`、`/governance-ops`、`/governance-security` 四类治理工作台。
- `GovernancePermissionGuard` 已在规则、联动、预案、密钥轮换写接口上执行双人复核权限校验。

但当前仍存在明显不一致：

1. 合同发布/回滚是“提交审批 -> 审批通过后执行”。
2. 阈值规则、联动规则、应急预案、密钥轮换仍是“请求头指定复核人 -> 即时执行”。
3. 控制面审批台无法完整覆盖全部关键治理动作。
4. 审计与复盘口径存在双轨：部分动作有审批主单，部分动作只有即时执行留痕。

因此，本轮要把这四类关键治理动作全部并入现有审批主单状态机，收口为单一治理语义。

## 2. 范围

### 2.1 纳入本轮统一审批化的动作

- `POST /api/rule-definition/add`
- `POST /api/rule-definition/update`
- `POST /api/rule-definition/delete/{id}`
- `POST /api/linkage-rule/add`
- `POST /api/linkage-rule/update`
- `POST /api/linkage-rule/delete/{id}`
- `POST /api/emergency-plan/add`
- `POST /api/emergency-plan/update`
- `POST /api/emergency-plan/delete/{id}`
- `POST /api/device/{id}/secret-rotate`

### 2.2 不在本轮范围内的事项

- 不引入新的审批表或第二套 BPM/工作流引擎。
- 不引入多级会签、批量审批、并行审批。
- 不把四类对象全部升级成复杂 diff 审批器。
- 不调整现有合同发布/回滚审批主干状态机，只做扩容。
- 不在本轮引入外部 KMS。

## 3. 设计结论

推荐方案：**统一接入现有 `governance-approval` 状态机**。

理由：

1. 现有合同发布/回滚已经验证过该状态机口径。
2. 控制面、审计、回滚、复盘已经围绕审批主单形成稳定语义。
3. 若继续保留“即时双人复核”旁路，会让控制面长期处于双轨治理状态。
4. 相比重建通用流程引擎，直接扩容现有状态机改动边界更清晰、风险更低。

## 4. 完成定义

本轮完成后，四类动作统一满足以下行为：

1. 原有写接口保留原 URL，不新增平行“submit-*”接口。
2. 写接口不再直接修改业务表，而是创建审批主单。
3. 写接口统一返回审批回执，至少包含：
   - `approvalOrderId`
   - `approvalStatus=PENDING`
   - `executionPending=true`
   - 必要的业务摘要字段
4. 真实写库仅发生在 `POST /api/system/governance-approval/{orderId}/approve`。
5. `reject / cancel / resubmit` 对四类动作全部可用。
6. `/governance-approval` 详情抽屉可以查看四类动作的主体摘要、请求快照、执行结果和失败原因。
7. 原工作台页面可以识别“待审批 / 已通过 / 已驳回 / 已撤销 / 执行失败可复盘”等状态。

## 5. 分层设计

本轮统一审批化按四层收口：`提交层 / 审批编排层 / 执行器层 / 读侧展示层`。

### 5.1 提交层

职责：接收原业务写请求，把即时执行改为“创建审批主单”。

设计要求：

- 保留现有 Controller 路由与 DTO，避免前端入口整体重写。
- Controller/Service 不再直接更新 `rule_definition`、`linkage_rule`、`emergency_plan` 或设备密钥。
- 改为将当前请求封装成统一审批 payload，写入审批主单。
- 响应保留必要业务摘要，避免前端提交后只能看到纯审批号。

接口语义变化：

- 旧语义：提交即执行。
- 新语义：提交审批，等待复核通过后执行。

### 5.2 审批编排层

职责：继续复用现有审批主单与流转状态机，只扩展动作类型与 payload 结构。

继续复用：

- `sys_governance_approval_order`
- `sys_governance_approval_transition`
- `GovernanceApprovalServiceImpl`
- `GovernanceApprovalQueryServiceImpl`

新增 action code：

- `RULE_DEFINITION_ADD`
- `RULE_DEFINITION_UPDATE`
- `RULE_DEFINITION_DELETE`
- `LINKAGE_RULE_ADD`
- `LINKAGE_RULE_UPDATE`
- `LINKAGE_RULE_DELETE`
- `EMERGENCY_PLAN_ADD`
- `EMERGENCY_PLAN_UPDATE`
- `EMERGENCY_PLAN_DELETE`
- `DEVICE_SECRET_ROTATE`

审批状态机不新增新状态，继续沿用：

- `PENDING`
- `APPROVED`
- `REJECTED`
- `CANCELLED`

并继续支持：

- `REJECTED -> PENDING` 重提

### 5.3 执行器层

职责：把审批通过后的具体业务执行从 `GovernanceApprovalServiceImpl` 中解耦出来。

设计要求：

- 新增“审批动作执行器”注册机制，按 `actionCode` 路由。
- `GovernanceApprovalServiceImpl` 只负责：
  - 状态机流转
  - 审批权限校验
  - 调用执行器
  - 回写执行结果
- 业务执行下沉到对应模块的 executor：
  - 阈值规则 executor
  - 联动规则 executor
  - 应急预案 executor
  - 设备密钥轮换 executor

这样可避免把规则、联动、预案、密钥轮换逻辑继续堆进审批主服务，后续再接入更多动作也能复用同一扩展机制。

### 5.4 读侧展示层

职责：统一审批读侧与原业务工作台的状态展示。

要求：

- `/governance-approval` 详情抽屉新增四类动作的主体摘要解析。
- 规则页、联动页、预案页、密钥相关入口页面要能识别：
  - 当前对象存在待审批单
  - 已驳回且可重提
  - 已通过且已执行
  - 已通过但执行失败
- 不新增独立审批编辑器，审批仍在原对象页面发起。

## 6. Payload 设计

审批主单 `payloadJson` 继续沿用“`request + execution`”两段结构。

### 6.1 request 段

必须包含：

- `actionCode`
- `subjectType`
- `subjectId`
- `operatorUserId`
- `approverUserId`
- 原始请求 DTO 快照
- 页面/业务上下文摘要

对于 `update/delete`，还必须附带：

- 提交时主体的当前快照摘要
- 必要的版本/更新时间信息

目的：避免审批时对象已被别人修改，但控制面看不出“审批的是哪一版内容”。

### 6.2 execution 段

审批通过后必须回写：

- `executedAt`
- `success`
- `resultSummary`
- 业务结果摘要
- 失败原因（如失败）

对于设备密钥轮换，还应补充：

- `rotationBatchId`
- `deviceId`
- `deviceCode`
- `productKey`
- `reason`
- 摘要级执行结果

要求：只写摘要，不暴露明文密钥。

## 7. 权限与身份边界

### 7.1 提交审批时

- 执行人继续校验原有 `edit/rotate` 权限。
- 必须显式指定复核人。
- 创建审批单时即校验复核人是否具备对应 `approve` 权限。
- 执行人与复核人不得为同一账号。

### 7.2 审批动作时

- `approve/reject` 以当前登录审批人为准，不再通过请求头模拟审批人。
- `cancel/resubmit` 仅允许原执行人操作。

### 7.3 权限码

本轮不推翻现有权限矩阵，继续复用：

- `risk:rule-definition:edit / approve`
- `risk:linkage-rule:edit / approve`
- `risk:emergency-plan:edit / approve`
- `iot:secret-custody:rotate / approve`

控制面查看权限继续沿用治理控制面现有菜单/接口口径，不额外发明新体系。

## 8. 一致性与并发策略

### 8.1 审批快照一致性

审批单中必须保存提交快照，尤其是 `update/delete` 场景。

### 8.2 审批执行前二次校验

审批通过执行时，需要再次检查：

- 主体是否仍存在
- 主体关键版本是否仍与提交时一致
- 删除对象是否已经被删除
- 更新对象是否已被其他人修改

若不一致：

- 不做静默覆盖
- 不继续错误执行
- 将执行结果回写为失败，并保留失败原因

### 8.3 失败语义

两类失败必须区分：

1. **提交审批失败**：不创建审批单。
2. **审批通过但执行失败**：审批状态仍为 `APPROVED`，但 `payloadJson.execution.result` 中必须保留失败详情。

原因：

- 审批通过意味着“复核动作已完成”。
- 执行失败是业务执行层问题，不应伪装成“审批没通过”。

## 9. 页面交互设计原则

### 9.1 原业务页

规则、联动、预案、密钥轮换入口页面继续承担提交动作：

- 提交后展示审批回执
- 支持查看当前审批状态
- 对 `REJECTED` 提供重提入口
- 对 `APPROVED + 执行失败` 提供查看失败详情入口

### 9.2 治理审批台

审批台新增四类动作的详情表达：

- 主体名称/编号
- 目标操作类型（新增/更新/删除/轮换）
- 请求摘要
- 执行结果摘要
- 审批意见
- 失败原因（如有）

不要求本轮在审批台内直接编辑对象，只要求可查看、可审批、可复盘。

## 10. 模块边界

### 10.1 `spring-boot-iot-system`

负责：

- 审批主单与流转状态机
- 审批动作执行器注册与调度骨架
- 审批读侧查询

### 10.2 `spring-boot-iot-alarm`

负责：

- 阈值规则 executor
- 联动规则 executor
- 应急预案 executor
- 对应主体摘要和结果摘要组装

### 10.3 `spring-boot-iot-device`

负责：

- 设备密钥轮换 executor
- 密钥轮换摘要与审计留痕

### 10.4 `spring-boot-iot-ui`

负责：

- 原业务工作台提交后状态回执与重提入口
- 审批台详情展示升级
- 统一读侧状态文案收口

## 11. 文档更新要求

实现时必须原位同步更新至少以下文档：

- `docs/02-业务功能与流程说明.md`
- `docs/03-接口规范与接口清单.md`
- `docs/04-数据库设计与初始化数据.md`（如 payload/表语义有新增口径）
- `docs/08-变更记录与技术债清单.md`
- `docs/21-业务功能清单与验收标准.md`
- `README.md`、`AGENTS.md`（若行为说明发生变化）

## 12. 验收口径

本轮实现完成后，应至少满足：

1. 四类动作全部创建审批单，而非立即执行。
2. 审批通过后才真实写库。
3. 驳回、撤销、重提对四类动作全部可用。
4. 审批台可以统一查看这四类动作。
5. 原工作台可以显示审批结果和失败原因。
6. 审计与复盘不再区分“审批化动作”和“即时执行动作”两条治理语义。
