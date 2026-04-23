# 厂商字段映射规则自动建议设计

> 日期：2026-04-13
> 状态：已在会话内确认方向，待用户审核书面 spec
> 适用范围：`spring-boot-iot-device`、`/products -> 契约字段` 后续治理增强、文档体系
> 目标：在不自动写规则、不自动审批、不新增第二套治理入口的前提下，为 `iot_vendor_metric_mapping_rule` 提供基于运行时证据的“自动建议”读侧，降低零代码接入时手工录规则的成本。

## 1. 背景

截至 `2026-04-13`，仓库已经具备以下稳定事实：

1. `iot_vendor_metric_mapping_rule` 已成为一级治理对象，并支持 `PRODUCT / DEVICE_FAMILY / SCENARIO / PROTOCOL` 多 scope。
2. compare、apply、审批执行与运行时 `PAYLOAD_APPLY` 已接通该规则消费链。
3. resolver snapshot、canonical alias、运行态 evidence 与对象洞察/history/latest 已基本统一到同一套解析真相。
4. `PROTOCOL` scope 刚补齐 `family:<familyCode>` 协议族继承，运行时不再只能按单一 `mqtt-json` 粗粒度命中。

但还有一个明显缺口：

1. 规则已经能消费，却仍主要依赖人工录入。
2. 运行态证据 `iot_vendor_metric_evidence` 已经持续沉淀 `rawIdentifier / canonicalIdentifier / logicalChannelCode / evidenceCount / lastSeenTime`，但还没有被反向利用来生成治理建议。
3. 如果继续让一线只靠肉眼抄写 `rawIdentifier -> targetNormativeIdentifier`，后续“只配配置、不改代码”会长期停留在半自动状态。

因此，下一步最合适的最小切片不是“自动生效”，而是“自动建议”。

## 2. 目标

本轮只做以下事情：

1. 为单产品输出一份只读的映射规则建议清单。
2. 建议结果直接复用现有 `iot_vendor_metric_evidence`、已发布 canonical snapshot、现有规则表和规范场景判断。
3. 明确回答哪些候选可以直接建规则、哪些已被覆盖、哪些存在冲突、哪些当前证据不足。
4. 让后续前端或治理控制面可以直接消费该建议接口，但本轮不强依赖新前端页面。

## 3. 非目标

本轮明确不做：

1. 不自动插入 `iot_vendor_metric_mapping_rule`。
2. 不新增自动审批、自动发布或自动回滚。
3. 不把建议结果写入新表长期持久化。
4. 不在本轮直接建议跨产品共享 scope（`DEVICE_FAMILY / SCENARIO / PROTOCOL / TENANT_DEFAULT`）落库。
5. 不新增独立“零代码接入平台”页面。

说明：

- 虽然运行时已经支持协议族继承，但当前 `iot_vendor_metric_evidence` 未稳定保存 `familyCodes`、跨产品复用证据和更高阶治理语义，因此首版自动建议只输出 **产品内建议**，不直接建议共享 scope。

## 4. 方案比较

### 4.1 方案 A：自动落库规则

优点：

1. 看起来最快。

缺点：

1. 现有证据模型并不保证每条 `canonicalIdentifier` 都足够稳定。
2. 一旦直接落库，错误建议会立刻影响 compare、apply 和 `PAYLOAD_APPLY`。
3. 与当前“治理优先、审批留痕优先”的架构方向冲突。

结论：

- 拒绝。

### 4.2 方案 B：后端只读 preview，人工确认后再创建规则

优点：

1. 风险最小。
2. 与现有 `/products -> 契约字段` 治理流兼容，不需要第二套真相。
3. 可以先把建议生成逻辑跑通，再决定是否补前端工作区或一键建规则动作。

缺点：

1. 首轮只能解决“发现建议”的问题，不能一步到位完成治理闭环。

结论：

- 采用本方案。

### 4.3 方案 C：直接做“建议 + 一键创建 + 一键审批”

优点：

1. 理论上离“零代码接入”更近。

缺点：

1. 范围过大，同时会引入建议可信度、审批谱系、回滚、前端交互和审计口径问题。
2. 当前证据模型不足以支撑这一层自动化。

结论：

- 延后到后续阶段。

## 5. 设计结论

本轮固定采用：

1. **单产品、只读、非持久化** 的规则建议接口。
2. 建议来源只基于当前产品已有证据、已发布 canonical snapshot、规范场景和已存在规则。
3. 输出结果按“可直接建 / 已覆盖 / 有冲突 / 忽略”分层，而不是只给一堆原始候选。
4. 首版建议 scope 固定为 `PRODUCT`，不直接自动上提到共享 scope。

## 6. 数据来源

建议服务只允许读取以下正式来源：

1. `iot_vendor_metric_evidence`
   - 提供 `rawIdentifier / canonicalIdentifier / logicalChannelCode / evidenceCount / sampleValue / lastSeenTime / evidenceOrigin`
2. `iot_vendor_metric_mapping_rule`
   - 提供“当前是否已覆盖 / 是否存在冲突”
3. `PublishedProductContractSnapshotService`
   - 提供当前产品最近正式批次的 canonical identifiers
4. `ProductModelNormativeMatcher + NormativeMetricDefinitionService`
   - 提供当前产品所属规范场景，以及规范字段是否真实存在

本轮不读取：

1. 历史消息日志原文
2. 新增候选草稿表
3. 独立建议持久化表

## 7. 建议生成规则

### 7.1 候选池

候选池来自当前产品的 `iot_vendor_metric_evidence`，但必须满足：

1. `rawIdentifier` 非空
2. `canonicalIdentifier` 非空
3. `rawIdentifier != canonicalIdentifier`（忽略本就不需要映射的直同名字段）

### 7.2 canonical 合法性

建议结果中的 `targetNormativeIdentifier` 必须满足以下任一条件：

1. 命中已发布 resolver snapshot 的 canonical identifier
2. 命中当前规范场景 `NormativeMetricDefinition.identifier`

若两者都不命中，则该条 evidence 不进入“可建议”集合，只能标记为 `IGNORED_UNKNOWN_CANONICAL`。

### 7.3 首版推荐 scope

首版统一输出：

1. `recommendedScopeType = PRODUCT`

原因：

1. 当前 evidence 只在产品维度稳定沉淀。
2. 跨产品共享建议需要额外的协议族/场景/设备族交叉证据，不适合在本轮硬推。

### 7.4 状态分层

每条建议必须返回明确状态：

1. `READY_TO_CREATE`
   - 当前没有同签名规则，且 canonical 合法
2. `ALREADY_COVERED`
   - 已存在同签名、同目标规则
3. `CONFLICTS_WITH_EXISTING`
   - 已存在同签名、不同目标规则
4. `IGNORED_SAME_IDENTIFIER`
   - `rawIdentifier == canonicalIdentifier`
5. `IGNORED_UNKNOWN_CANONICAL`
   - canonical 未命中正式发布快照或规范字段
6. `LOW_CONFIDENCE`
   - 建议合法，但证据次数偏低，例如仅出现 `1` 次

### 7.5 置信度

本轮只提供轻量置信度，不引入复杂评分模型：

1. `high`
   - canonical 命中已发布 snapshot，且 `evidenceCount >= 3`
2. `medium`
   - canonical 命中规范字段，且 `evidenceCount >= 2`
3. `low`
   - 其他仍可展示但不建议直接创建的场景

置信度仅用于排序和提示，不自动改变治理动作。

## 8. 接口设计

建议新增只读接口：

- `GET /api/device/product/{productId}/vendor-mapping-rule-suggestions`

建议查询参数：

1. `includeCovered`
   - 是否返回 `ALREADY_COVERED`
2. `includeIgnored`
   - 是否返回 `IGNORED_*`
3. `minEvidenceCount`
   - 最小证据次数，默认 `1`

建议响应项至少包含：

1. `rawIdentifier`
2. `logicalChannelCode`
3. `targetNormativeIdentifier`
4. `recommendedScopeType`
5. `status`
6. `confidence`
7. `evidenceCount`
8. `sampleValue`
9. `valueType`
10. `evidenceOrigin`
11. `lastSeenTime`
12. `reason`
13. `existingRuleId`
14. `existingTargetNormativeIdentifier`

权限建议：

1. 沿用 `iot:product-contract:govern`

原因：

1. 该接口虽然只读，但直接服务治理动作，不适合作为公开读接口。

## 9. 服务边界

建议新增一个独立服务，例如：

- `VendorMetricMappingRuleSuggestionService`

职责只包括：

1. 读取 evidence
2. 校验 canonical 合法性
3. 对照现有规则生成状态
4. 输出排序后的建议列表

明确不承担：

1. 规则创建
2. 审批触发
3. 发布批次写入
4. 前端工作流状态保存

## 10. 排序规则

结果默认按以下顺序返回：

1. `READY_TO_CREATE`
2. `CONFLICTS_WITH_EXISTING`
3. `LOW_CONFIDENCE`
4. `ALREADY_COVERED`
5. `IGNORED_*`

同状态内再按：

1. `confidence`
2. `evidenceCount DESC`
3. `lastSeenTime DESC`
4. `rawIdentifier ASC`

## 11. 测试策略

至少覆盖以下测试：

1. evidence 命中已发布 canonical，生成 `READY_TO_CREATE`
2. evidence 与已存在规则完全一致，生成 `ALREADY_COVERED`
3. evidence 命中同签名不同目标，生成 `CONFLICTS_WITH_EXISTING`
4. `rawIdentifier == canonicalIdentifier`，生成 `IGNORED_SAME_IDENTIFIER`
5. canonical 不在 snapshot 与规范字段内，生成 `IGNORED_UNKNOWN_CANONICAL`
6. `evidenceCount = 1` 时输出 `LOW_CONFIDENCE`
7. query 参数对过滤行为生效

## 12. 文档影响

若实施本设计，后续至少需要同步：

1. `docs/02-业务功能与流程说明.md`
2. `docs/03-接口规范与接口清单.md`
3. `docs/08-变更记录与技术债清单.md`

如后续补前端工作台，再决定是否同步 `docs/15-前端优化与治理计划.md`。

## 13. 验收口径

实现完成后，必须能回答以下问题：

1. 当前产品有哪些 `rawIdentifier -> canonicalIdentifier` 候选值得建规则？
2. 哪些候选已经有规则，无需重复创建？
3. 哪些候选与现有规则冲突？
4. 哪些候选证据不足，只能继续观察？

并且必须保持以下边界：

1. 不自动创建规则
2. 不改动 compare/apply 现有真相
3. 不影响 `PAYLOAD_APPLY` 正常主链

## 14. 后续演进顺序

本 spec 落地后，建议的后续顺序是：

1. 自动建议规则
2. 场景继承
3. 独立审批与发布谱系
4. 更高阶的自动绑定/自动创建策略

原因：

1. 自动建议先解决“如何发现可配置项”
2. 场景继承再解决“如何把规则从产品提升为共享模板”
3. 审批与发布谱系最后解决“如何把自动化治理变成正式流程”
