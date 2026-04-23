# 无代码设备接入 P0/P1 实施设计

> 日期：2026-04-18
> 状态：已在会话内确认方案，待用户审阅书面 spec
> 关联设计：`docs/superpowers/specs/2026-04-14-no-code-device-onboarding-roadmap-design.md`
> 适用范围：`spring-boot-iot-ui`、`spring-boot-iot-device`、`spring-boot-iot-system`、`spring-boot-iot-framework`、质量工场执行链、文档与真实环境验收口径
> 目标：在不推翻现有 `/products`、`/protocol-governance`、映射规则、合同发布、审批与运行时快照体系的前提下，把当前“零代码接入 Phase 1 的治理能力”收口为可操作、可验收、可复用的无代码接入闭环，并完成 P0/P1 范围设计。

## 1. 背景

截至 `2026-04-18`，仓库已经具备以下稳定事实：

1. `/products -> 契约字段` 已形成“样本输入 -> compare -> 映射规则建议/台账 -> apply -> 发布批次/版本台账”的治理工作区。
2. `/protocol-governance` 已形成“协议族定义 / 解密档案 / 协议模板”的草稿、命中试算、模板回放与发布入口。
3. 运行时已具备 `PRODUCT > DEVICE_FAMILY > SCENARIO > PROTOCOL > TENANT_DEFAULT` 的映射规则命中优先级。
4. 产品合同发布、映射规则发布、协议治理发布都已经接入现有审批与快照真相体系。
5. 质量工场与浏览器专项验收底座已稳定存在，可承接新的标准验收包。

同时，仓库也明确声明了当前边界：

1. 当前能力只覆盖裂缝、GNSS、激光测距、深部位移、雨量计等治理最小切片。
2. 当前能力**不代表任意设备厂商已经实现零代码接入**。
3. 当前流程仍偏“专家治理模式”，而不是“接入人员可直接操作的无代码接入闭环”。

因此，本轮不应再继续用零散增强去堆叠局部能力，而是要把已经完成的治理能力收口成一条清晰的接入主流程。

## 2. 问题定义

当前系统的关键问题不是“是否已经具备协议治理和字段治理能力”，而是：

1. 现有能力分散在 `/products`、`/protocol-governance`、审批台和质量工场之间，缺少统一接入入口。
2. 新设备接入时，平台不能直接回答“当前到哪一步、为什么卡住、下一步去哪里处理”。
3. 已有治理资产无法作为“模板包”复用，同类设备接入仍接近重复操作。
4. 接入是否成功仍主要依赖人工判断，缺少标准化验收回执和失败分层归因。

本设计要解决的是：在现有治理体系不推翻的前提下，把“协议治理 -> 样本验证 -> 映射规则采纳 -> 合同发布 -> 接入验收”编排成一条对接入人员可见、可操作、可回放的无代码接入闭环。

## 3. 目标与非目标

### 3.1 本轮目标

本轮 P0/P1 完成后，应达到以下目标：

1. 新接入一类主流监测传感器时，不需要修改 Java 代码。
2. 平台具备统一接入入口，可清晰表达接入阶段、阻塞原因和下一步动作。
3. 协议治理、产品治理、映射规则治理与标准验收能够通过同一条接入流程被串联。
4. 同类设备可通过模板包复用既有治理资产。
5. 接入成功与失败都能形成标准运行记录，而不是只依赖人工逐页检查。

### 3.2 本轮非目标

本轮明确不承诺以下事情：

1. 不追求“任意厂商、任意设备 100% 全自动零代码接入”。
2. 不新增一套平行的协议真相、合同真相或运行时真相。
3. 不把 `/products` 或 `/protocol-governance` 重写成新的总控平台。
4. 不实现 AI 自动生成并自动发布全部映射规则。
5. 不在本轮覆盖任意工业协议和所有长尾设备族。

## 4. 方案比较

### 4.1 方案 A：维持现状，只做零散补缺

做法：

1. 保持当前 `/products` 和 `/protocol-governance` 分散治理模式。
2. 针对具体设备族继续追加 compare、规则和页面提示。

优点：

1. 改动最小。
2. 风险低。

缺点：

1. 仍然缺少统一接入主流程。
2. 平台无法直接回答“当前接入状态”和“下一步动作”。
3. 无法形成可复制的规模化接入方式。

结论：

- 不采用。

### 4.2 方案 B：在现有治理能力之上建设最小无代码接入闭环，推荐

做法：

1. 新增统一接入工作台承接接入案例编排。
2. 复用现有协议治理、产品治理、审批与快照真相。
3. 复用质量工场执行底座承接标准接入验收。
4. 新增模板包作为已发布治理资产的组合引用。

优点：

1. 与当前仓库事实最一致。
2. 复用现有正式真相和工作台，实施风险可控。
3. 能把当前 Phase 1 能力提升为真实可用的平台能力。

缺点：

1. 仍然需要接受“限定场景的配置化接入”，而不是万能接入。
2. 需要补齐新的流程编排对象和统一页面。

结论：

- 采用本方案。

### 4.3 方案 C：直接建设通用零代码设备接入平台

做法：

1. 一次性补齐统一治理中心、DSL、自动映射、自动审批和全协议接入。

优点：

1. 理论上最接近终局形态。

缺点：

1. 范围失控。
2. 极易破坏当前稳定基线。
3. 真实环境难以分段验收和回滚。

结论：

- 不采用。

## 5. 设计结论

本轮固定采用“在现有治理能力之上建设最小无代码接入闭环”的方案，并拆成以下模块：

1. `P0-1`：统一接入工作台
2. `P0-2`：标准接入验收链
3. `P0-3`：接入模板包
4. `P1-1`：批量治理与批量验收

推荐顺序固定为：

1. 先完成统一接入入口，让平台能回答“当前在哪一步”
2. 再完成标准验收，让平台能回答“是否接入成功”
3. 然后沉淀模板包，让平台能回答“这类设备如何复用”
4. 最后再做批量能力，放大稳定流程而不是放大混乱

## 6. 模块设计

### 6.1 P0-1 统一接入工作台

#### 6.1.1 页面定位

新增 `接入智维` 子页，建议路由为 `/device-onboarding`，中文名固定为“无代码接入台”。

该页面不是新的协议治理页，也不是新的产品治理页，而是“接入总控入口”。它的职责只有四个：

1. 创建和查看接入案例
2. 展示当前步骤状态
3. 明确下一步动作和阻塞原因
4. 触发标准接入验收

复杂编辑动作仍留在现有页面：

1. 协议族、解密档案、协议模板编辑继续留在 `/protocol-governance`
2. compare、映射规则采纳、合同发布继续留在 `/products`
3. 审批详情继续留在 `/governance-approval`
4. 验收详情继续留在 `/automation-results`

#### 6.1.2 流程对象

新增编排对象：

- `iot_device_onboarding_case`

它只承接流程编排真相，不保存协议正式内容、合同正式内容或运行时正式内容。

建议字段：

1. `id`
2. `caseCode`
3. `caseName`
4. `tenantId`
5. `productId`
6. `templatePackId`
7. `scenarioCode`
8. `deviceFamily`
9. `protocolFamilyCode`
10. `decryptProfileCode`
11. `protocolTemplateCode`
12. `releaseBatchId`
13. `acceptanceRunId`
14. `currentStep`
15. `status`
16. `blockerSummaryJson`
17. `evidenceSummaryJson`
18. `createdBy`
19. `updatedBy`
20. `createdAt`
21. `updatedAt`

#### 6.1.3 流程步骤

无代码接入台主流程固定为：

1. 选择模板包或空白接入
2. 协议治理状态确认
3. 样本 compare 与映射规则治理
4. 合同发布
5. 接入验收
6. 结果归档

页面必须能在首屏直接回答：

1. 当前到哪一步
2. 为什么卡住
3. 下一步去哪里处理

### 6.2 P0-2 标准接入验收链

#### 6.2.1 设计原则

标准接入验收链必须直接复用现有质量工场执行底座，不新增第二套执行引擎。

无代码接入台只负责：

1. 触发验收
2. 回写 `runId`
3. 展示摘要结果
4. 深链到 `/automation-results`

#### 6.2.2 验收检查项

首版固定检查以下 8 项：

1. 协议族命中
2. 解密档案命中
3. 协议模板回放通过
4. compare 成功
5. 映射规则命中无冲突
6. 正式合同已发布
7. `latest / history / insight` 可读
8. 风险目录或风险链路符合当前产品能力边界

#### 6.2.3 验收结论与归因

验收结论只允许三类：

1. `PASSED`
2. `FAILED`
3. `BLOCKED`

失败归因必须固定落到以下层级之一：

1. 协议层
2. 解密层
3. 模板层
4. 映射层
5. 合同层
6. 读侧层
7. 风险层

### 6.3 P0-3 接入模板包

#### 6.3.1 页面与对象定位

新增 first-class 模板包对象：

- `iot_onboarding_template_pack`

首版可以使用单表 + JSON 结构承载，不急于拆成多张子表。

模板包的本质是“已发布治理资产的组合引用”，而不是新的快照中心。

#### 6.3.2 建议字段

1. `id`
2. `packCode`
3. `packName`
4. `scenarioCode`
5. `deviceFamily`
6. `status`
7. `versionNo`
8. `protocolFamilyCode`
9. `decryptProfileCode`
10. `protocolTemplateCode`
11. `defaultGovernanceConfigJson`
12. `defaultInsightConfigJson`
13. `defaultAcceptanceProfileJson`
14. `description`
15. `createdBy`
16. `updatedBy`
17. `createdAt`
18. `updatedAt`

#### 6.3.3 使用方式

模板包用于：

1. 新建接入案例时一键预填
2. 约束同类设备采用统一治理基线
3. 为后续批量接入提供复用资产

首版模板包只引用已发布对象，不复制以下内容：

1. 协议族正式定义
2. 解密档案正式定义
3. 协议模板正式定义
4. 产品合同正式快照
5. 映射规则正式快照

### 6.4 P1-1 批量治理与批量验收

批量能力必须建立在单案例流程稳定之后。

首版批量能力只做以下四件事：

1. 批量创建接入案例
2. 批量套用模板包
3. 批量触发验收
4. 批量查看结果与失败分组

以下能力不进入首版：

1. 批量发布审批
2. 批量复杂回滚
3. 批量自动修复
4. AI 自动生成并自动发布规则

## 7. 真相源边界

本轮最重要的原则是：不新增第二套业务真相。

各对象职责固定如下：

1. `iot_device_onboarding_case`
   - 只承接流程编排真相
   - 回答接入做到哪一步、卡在哪里、关联了哪些已发布对象
2. `iot_onboarding_template_pack`
   - 只承接复用模板真相
   - 本质是已发布治理资产的组合引用
3. 协议正式真相继续留在：
   - `iot_protocol_family_definition_snapshot`
   - `iot_protocol_decrypt_profile_snapshot`
   - `iot_protocol_template_definition_snapshot`
4. 合同与映射正式真相继续留在：
   - `iot_product_contract_release_batch`
   - `iot_product_metric_resolver_snapshot`
   - `iot_vendor_metric_mapping_rule_snapshot`
5. 验收正式真相继续留在现有质量工场运行记录中，接入案例只保存 `runId`

## 8. 失败处理设计

无代码接入台不能再用“系统繁忙”覆盖所有失败。

阻塞原因必须结构化落到 `iot_device_onboarding_case.blockerSummaryJson`，首版固定以下阻塞码：

1. `PROTOCOL_NOT_MATCHED`
2. `DECRYPT_PROFILE_NOT_MATCHED`
3. `TEMPLATE_REPLAY_FAILED`
4. `COMPARE_FAILED`
5. `MAPPING_CONFLICT`
6. `CONTRACT_NOT_PUBLISHED`
7. `ACCEPTANCE_FAILED`
8. `DEPENDENCY_BLOCKED`

每条阻塞信息至少包含：

1. `blockerCode`
2. `blockerStage`
3. `summary`
4. `recommendedAction`
5. `relatedObjectType`
6. `relatedObjectId`

页面首屏禁止展示长日志，只允许先回答：

1. 当前卡在哪一步
2. 为什么卡住
3. 去哪个页面处理

## 9. API 与页面落点建议

### 9.1 前端页面

新增页面：

1. `/device-onboarding`

复用页面：

1. `/protocol-governance`
2. `/products`
3. `/governance-approval`
4. `/automation-results`

### 9.2 后端接口

新增接入案例接口建议：

1. `GET /api/device/onboarding/cases`
2. `POST /api/device/onboarding/cases`
3. `GET /api/device/onboarding/cases/{caseId}`
4. `PUT /api/device/onboarding/cases/{caseId}`
5. `POST /api/device/onboarding/cases/{caseId}/start-acceptance`
6. `POST /api/device/onboarding/cases/{caseId}/refresh-status`

新增模板包接口建议：

1. `GET /api/device/onboarding/template-packs`
2. `POST /api/device/onboarding/template-packs`
3. `GET /api/device/onboarding/template-packs/{packId}`
4. `PUT /api/device/onboarding/template-packs/{packId}`

P1 批量接口建议：

1. `POST /api/device/onboarding/cases/batch-create`
2. `POST /api/device/onboarding/cases/batch-start-acceptance`

## 10. 测试与验收策略

### 10.1 后端测试

必须覆盖：

1. onboarding case 状态流转
2. 模板包引用校验
3. 接入案例与既有协议/合同/验收 `runId` 的关联逻辑
4. blocker 归类逻辑

### 10.2 前端测试

必须覆盖：

1. 无代码接入台步骤展示
2. 阻塞态展示
3. 深链动作
4. 模板包预填
5. 验收结果摘要展示

### 10.3 浏览器专项验收

首版真实环境至少覆盖：

1. 成功闭环 1 条
2. 模板缺失阻塞 1 条
3. 合同未发布阻塞 1 条

最小成功闭环步骤固定为：

1. 创建接入案例
2. 选择模板包
3. 跳转协议治理确认命中
4. 跳转产品治理完成 compare/apply
5. 返回无代码接入台触发验收
6. 查看统一结果

## 11. 分阶段完成定义

### 11.1 P0 完成定义

P0 完成后，平台至少要做到：

1. 接入人员可在 `/device-onboarding` 看到当前案例做到哪一步
2. 页面能明确回答为什么卡住和下一步去哪处理
3. 接入案例可一键触发标准验收并回写统一 `runId`
4. 平台能稳定区分协议、解密、模板、映射、合同、读侧、风险层失败
5. 同类设备可通过模板包完成一键预填

### 11.2 P1 完成定义

P1 完成后，平台至少要做到：

1. 同类设备支持批量创建接入案例
2. 同类设备支持批量套用模板包
3. 同类设备支持批量触发验收
4. 平台能按失败分组统一查看批量接入结果

## 12. 风险与约束

本轮实施必须遵守以下约束：

1. 不改现有 `/products` 与 `/protocol-governance` 的正式真相口径
2. 不在接入台内复制复杂编辑器
3. 不重做质量工场执行引擎
4. 不把模板包做成新的快照中心
5. 不把“限定场景的配置化接入”对外误写成“任意设备完全零代码接入”

## 13. 文档影响

当本设计进入实施并产生实际行为变更后，至少需要同步更新：

1. `README.md`
2. `docs/02-业务功能与流程说明.md`
3. `docs/03-接口规范与接口清单.md`
4. `docs/04-数据库设计与初始化数据.md`
5. `docs/05-自动化测试与质量保障.md`
6. `docs/07-部署运行与配置说明.md`
7. `docs/08-变更记录与技术债清单.md`
8. `docs/21-业务功能清单与验收标准.md`

本 spec 本身只定义方案，不直接改变上述运行基线。
