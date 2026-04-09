# GNSS 语义契约与阈值策略 `riskMetricId` 收口设计

> 日期：2026-04-06
> 状态：已在会话内确认，可进入实施计划阶段
> 适用范围：`spring-boot-iot-device`、`spring-boot-iot-alarm`、`spring-boot-iot-protocol`、`spring-boot-iot-ui`、`/products`、`/rule-definition`
> 目标：在已完成 `phase1-crack` 裂缝最小切片的基础上，落第二波 `GNSS` 语义契约治理，并补齐阈值策略页对 `riskMetricId` 的一等消费能力

## 1. 背景

当前仓库已经形成三条稳定事实：

1. `phase1-crack` 裂缝切片已经跑通 `规范字段库 -> 厂商字段证据 -> 合同发布批次 -> 风险指标目录 -> 风险点/治理缺口消费` 最小闭环。
2. GNSS 相关业务语义已经散落在多处资产中，但尚未形成正式闭环：
   - [appendix/iot-field-governance-and-sop.md](../../appendix/iot-field-governance-and-sop.md) 已明确 `gpsInitial`、`gpsTotalX`、`gpsTotalY`、`gpsTotalZ` 的业务定义。
   - `RiskPointPendingMetricGovernanceRules` 已能把 `gpsInitial / gpsTotalX / gpsTotalY / gpsTotalZ` 识别为 GNSS 业务测点，并继续把 `gX / gY / gZ` 保持在谨慎态。
   - `ProductModelServiceImpl` 已有基础 GNSS 中文名与字段标签，但 `ProductModelNormativeMatcher` 还没有对应的正式场景编码。
3. 阈值策略运行面已经开始支持 `riskMetricId`：
   - `rule_definition` 表和 `RuleDefinition` 实体已经有 `risk_metric_id`。
   - `RiskPolicyResolver` 已优先支持 `riskMetricId + metricIdentifier` 的规则匹配。
   - 但 `/rule-definition` 页面和前端 API 仍主要围绕 `metricIdentifier` 工作，尚未把 `riskMetricId` 作为前台一等身份。

因此，当前真正未完成的不是再补一批裂缝字段，而是把已经被文档和规则承认的 `GNSS` 语义正式纳入语义契约治理链，并把阈值策略前台与后端的身份表达补齐。

## 2. 本波目标

本波只服务两个目标：

1. 把 `GNSS` 正式纳入与 `phase1-crack` 同口径的语义契约治理链。
2. 让阈值策略页开始稳定保存、展示和消费 `riskMetricId`，不再只把 `metricIdentifier` 当作唯一身份。

落地后固定主链路为：

`GNSS 规范字段 -> compare 规范对齐 -> iot_vendor_metric_evidence -> iot_product_model -> risk_metric_catalog -> 风险点绑定 -> 阈值策略(riskMetricId) -> 风险闭环`

## 3. 非目标

本波明确不做以下事情：

1. 不把 `深部位移` 一并纳入本次实现。
2. 不把 `GNSS 基准站` 的姿态类字段、平台计算链路或原始观测复杂对象一次性做完。
3. 不把 `gX / gY / gZ` 这类当前语义不稳定的字段直接转成正式 GNSS 合同字段。
4. 不新建“风险指标目录管理页”或“厂商零代码接入平台”。
5. 不改变 `iot_product_model` 作为正式合同落点、`risk_metric_catalog` 作为风险桥梁层落点的既有架构。

## 4. 方案比较与选型

### 4.1 方案 A：只做 GNSS compare/apply，不动阈值策略页

优点：

1. 改动最小。
2. 能较快把 GNSS 合同治理补上。

缺点：

1. `rule_definition.risk_metric_id` 继续处于“后端存在、前台弱感知”的半成品状态。
2. 风险策略层仍会被 `metricIdentifier` 单独驱动，达不到本轮希望的身份收口。

### 4.2 方案 B：GNSS 合同治理 + 阈值策略 `riskMetricId` 收口

优点：

1. 与当前双核心域设计一致，能把“合同发布”和“策略消费”真正串起来。
2. 风险策略页不需要大改结构，只需要把身份表达升级到目录指标层。
3. 能为后续 `深部位移`、更多族类复用同一套消费方式。

缺点：

1. 会同时涉及 `device / alarm / ui / docs` 多模块。
2. 需要补一轮前端类型、表单和设备测点选项联动。

### 4.3 方案 C：直接上“厂商零代码接入平台”

优点：

1. 表面上一步到位。

缺点：

1. 当前规范库、风险目录和前台消费口径都还没在 `GNSS` 层面收口，直接上平台会把问题推向更大范围。
2. 与本轮“按波次复制已验证方法论”的策略冲突。

### 4.4 选型

本波采用 `方案 B`：

1. 先把 `GNSS` 复制成第二个正式语义契约切片。
2. 同波补齐阈值策略页对 `riskMetricId` 的一等消费。
3. `深部位移` 与“零代码接入平台”留到后续独立规格与计划。

## 5. GNSS 语义契约设计

### 5.1 场景编码

新增正式场景编码：

- `phase2-gnss`

`ProductModelNormativeMatcher` 对产品的识别口径固定为：

1. `productKey / productName / manufacturer / description` 中出现 `GNSS`、`gnss`、`北斗`、`卫星位移` 等关键词时，优先识别为 `phase2-gnss`。
2. 若同时命中裂缝规则与 GNSS 规则，以更明确的 GNSS 语义优先。
3. 仅命中 `gX / gY / gZ` 一类通用字段而没有 GNSS 上下文时，不得误识别为 `phase2-gnss`。

### 5.2 规范字段最小集合

本波 GNSS 规范字段库只引入以下最小集合：

1. `gpsInitial`
   - 中文名：`GNSS 原始观测基础数据`
   - 允许进入正式合同：是
   - 允许进入风险目录：否
2. `gpsTotalX`
   - 中文名：`GNSS 累计位移 X`
   - 允许进入正式合同：是
   - 允许进入风险目录：是
3. `gpsTotalY`
   - 中文名：`GNSS 累计位移 Y`
   - 允许进入正式合同：是
   - 允许进入风险目录：是
4. `gpsTotalZ`
   - 中文名：`GNSS 累计位移 Z`
   - 允许进入正式合同：是
   - 允许进入风险目录：是
5. `sensor_state`
   - 中文名：`传感器状态`
   - 允许进入正式合同：是
   - 允许进入风险目录：否

统一约束：

1. `gpsInitial` 作为原始资料字段，不参与风险目录发布。
2. `gpsTotalX / gpsTotalY / gpsTotalZ` 才是 GNSS 风险闭环主指标。
3. `gX / gY / gZ`、`lat / lon`、通信、电源、固件类字段继续留在证据层或待人工复核，不自动提升为正式 GNSS 合同字段。

### 5.3 证据与 compare 规则

`/products` 的 compare/apply 继续复用当前裂缝切片的治理框架，但 GNSS 固化以下规则：

1. 直报 GNSS 产品：
   - 若样本中直接出现 `gpsInitial / gpsTotalX / gpsTotalY / gpsTotalZ`，直接按规范字段建 compare 行。
2. 复合设备场景：
   - 若业务样本中出现 `L1_GP_*` 逻辑测点，compare 主标识统一收口为最后一段规范字段，如 `gpsTotalX`。
   - `S1_ZT_1.sensor_state.<logicalChannelCode>` 继续归一为 `sensor_state`。
3. 证据层必须保留原始字段别名：
   - 例如 `L1_GP_1.gpsTotalX`
   - 例如厂商别名字段
4. 若样本只出现 `gX / gY / gZ` 而无明确 GNSS 语义：
   - compare 行不直接映射到规范字段
   - 继续保留在厂商字段证据或人工裁决态

### 5.4 apply 与发布

apply 后保持以下行为：

1. 正式合同仍写入 `iot_product_model`。
2. 厂商字段证据继续写入 `iot_vendor_metric_evidence`。
3. 发布批次继续写入 `iot_product_contract_release_batch`。
4. 只有 `risk_enabled=1` 的 `gpsTotalX / gpsTotalY / gpsTotalZ` 会同步进入 `risk_metric_catalog`。
5. `gpsInitial` 与 `sensor_state` 虽可进入正式合同，但不进入风险目录。

## 6. 阈值策略 `riskMetricId` 收口设计

### 6.1 总体原则

阈值策略从本波开始采用“双字段、单主身份”：

1. `riskMetricId` 是正式主身份。
2. `metricIdentifier` 保留为兼容字段与显示字段。
3. 新增或编辑规则时，若能拿到 `riskMetricId`，必须优先落该值。
4. 旧数据、旧接口、旧前端仍可用 `metricIdentifier` 回退，不做一次性强制迁移。

### 6.2 后端口径

本波后端固定以下规则：

1. `RuleDefinitionController` 和 `RuleDefinitionService` 支持接收 `riskMetricId`。
2. 分页与列表查询继续支持 `metricIdentifier`，并补充 `riskMetricId` 的透传与返回。
3. `RiskPolicyResolver` 继续保持：
   - 优先按 `riskMetricId`
   - 无目录指标时回退 `metricIdentifier`
4. 若请求同时传入 `riskMetricId` 与 `metricIdentifier`，且两者能解析到不同目录指标，返回明确业务错误，避免错绑规则。

### 6.3 设备测点选项

为避免阈值策略前台继续只能看到裸 `metricIdentifier`，本波把 `GET /api/device/{deviceId}/metrics` 的返回升级为：

1. `identifier`
2. `name`
3. `dataType`
4. `riskMetricId`（若该字段已发布到风险目录）

这样前台可以在已有设备测点选项链路上直接拿到目录指标主键，而不需要新增第二套目录查询接口。

### 6.4 前端工作台

`/rule-definition` 本波不重做页面结构，只做身份收口：

1. 前端 `RuleDefinition` 类型补齐 `riskMetricId`。
2. 表格展示和编辑表单都保留 `metricIdentifier / metricName`，并在有值时感知 `riskMetricId`。
3. 表单提交时，如果已知 `riskMetricId`，必须一并提交。
4. 从治理缺口、风险点绑定、设备测点选项等已有来源带入的目录指标，前台不得在提交时把 `riskMetricId` 丢掉。
5. 过滤条件仍先保留 `metricIdentifier`，本波不额外引入新的“目录指标筛选器”组件。

## 7. 模块边界

### 7.1 `spring-boot-iot-device`

负责：

1. GNSS 规范场景识别。
2. compare 规范匹配、字段裁决和证据沉淀。
3. apply 后发布批次与风险目录触发。
4. 设备测点选项补充 `riskMetricId`。

不负责：

1. 阈值规则持久化。
2. 风险策略表达式计算。

### 7.2 `spring-boot-iot-alarm`

负责：

1. `risk_metric_catalog` 发布与查询。
2. `rule_definition` 对 `riskMetricId` 的校验、保存和查询。
3. 风险点和策略解析继续优先消费目录指标身份。

不负责：

1. GNSS 规范字段比对本身。

### 7.3 `spring-boot-iot-protocol`

本波只承接必要兼容：

1. 不新增 GNSS 协议模板平台。
2. 如现有 GNSS 样本和 legacy `$dp` 证据已可供 compare 使用，则继续复用现状。
3. 只有当 compare 证据无法稳定保留原始别名时，才补最小元数据透传。

### 7.4 `spring-boot-iot-ui`

负责：

1. `/products` compare 行展示 GNSS 规范元信息。
2. `/rule-definition` 保留并提交 `riskMetricId`。
3. 已有视图结构保持稳定，不额外新增平行页面。

## 8. 错误处理与回退

本波固定以下兜底策略：

1. 若产品命中 `phase2-gnss`，但规范字段库未初始化，compare 返回明确业务错误，不默默回退为“全字段均可转正”。
2. 若 `riskMetricId` 无法找到有效目录指标：
   - 新增/编辑阈值策略时返回业务错误
   - 不允许写入悬空目录主键
3. 若前台只拿到 `metricIdentifier`，后端继续允许兼容保存，但视为旧链路回退，不影响既有规则执行。
4. 若 `gpsInitial` 被误用于风险目录发布，视为实现错误，测试必须卡住。

## 9. 验证与验收

本波至少覆盖以下验证：

1. 后端单测：
   - `ProductModelNormativeMatcher` 能识别 `phase2-gnss`
   - GNSS compare 行能返回 `normativeIdentifier / normativeName / riskReady / rawIdentifiers`
   - apply 后只把 `gpsTotalX / gpsTotalY / gpsTotalZ` 发布到 `risk_metric_catalog`
   - `RuleDefinition` 新增/更新/查询能保留 `riskMetricId`
   - `DeviceService.listMetricOptions` 能返回 `riskMetricId`
2. 前端 Vitest：
   - `/products` GNSS compare 展示正确
   - `/rule-definition` 编辑已有 `riskMetricId` 规则时不丢值
   - 有 `riskMetricId` 的表单提交会透传该字段
3. 文档同步：
   - `docs/02`、`docs/03`、`docs/04`、`docs/08`、`docs/21`
   - 视行为变化补充 `README.md`、`AGENTS.md`

## 10. 后续波次

本波完成后，后续顺序固定为：

1. `深部位移` 语义契约治理切片
2. 阈值策略更完整的目录指标选择体验
3. 厂商配置化接入与更大范围的“零代码接入平台”

这样可以保证每一波都建立在上一个已稳定闭环之上，而不是把所有问题堆到同一个“大平台重构”里。
