# 风险指标联动/预案显式绑定真相设计

> 日期：2026-04-08
> 状态：会话内已完成方案确认，待用户复核书面 spec 后进入 implementation plan
> 适用范围：`spring-boot-iot-alarm`、`spring-boot-iot-admin`、`spring-boot-iot-ui`、`sql`、`scripts`
> 目标：把当前联动覆盖率、预案覆盖率从“运行时语义猜测”升级为“风险指标与联动/预案之间的显式绑定真相”，在不新增前台维护入口的前提下，先收口后端数据真相与聚合读侧。

## 1. 背景

截至 2026-04-08，桥层对象模型的第一轮基础已经具备：

1. `iot_product_model` 继续作为正式合同真相表。
2. `risk_metric_catalog` 已经成为“正式合同 -> 风险闭环”的桥层真相。
3. `risk_point_device` 已开始优先通过 `risk_metric_id` 挂接正式风险指标。
4. `GET /api/risk-governance/coverage-overview` 和 `GET /api/risk-governance/dashboard-overview` 已能回答“合同 -> 指标 -> 绑定 -> 策略覆盖率到哪一步”。

但当前“联动覆盖率 / 预案覆盖率”的底层真相仍不稳定：

1. `RiskGovernanceServiceImpl` 在 `countLinkageCoveredMetrics(...)` 中通过 `triggerCondition` 文本、解析后的 JSON 与测点名称做猜测匹配。
2. `RiskGovernanceServiceImpl` 在 `countEmergencyPlanCoveredMetrics(...)` 中通过 `planName / description / responseSteps / contactList` 拼装文本后再猜测命中测点。
3. `dashboard-overview` 的 `pendingLinkageCount / pendingEmergencyPlanCount / pendingLinkagePlanCount / linkageCoverageRate / emergencyPlanCoverageRate / linkagePlanCoverageRate` 都建立在上述语义猜测之上。

这会带来三个持续问题：

1. 命中结果随规则文案和预案描述变化而漂移，管理驾驶舱和控制面无法稳定回答“到底覆盖了哪些指标”。
2. 后续做影响分析时，无法明确回答“删除某个联动规则/应急预案会影响哪些风险指标”。
3. 继续依赖语义猜测，会让“桥层对象已经显式化，但联动/预案还在隐式引用”这一技术债长期固化。

因此，本轮选择先做一刀“后端真相优先”：新增显式绑定表，保持现有联动规则页和预案页兼容，由后端在写入和历史补账阶段把隐式关系固化为正式关系。

## 2. 目标与非目标

### 2.1 本轮目标

1. 新增 `risk_metric_linkage_binding`，表达“某风险指标已被某联动规则覆盖”。
2. 新增 `risk_metric_emergency_plan_binding`，表达“某风险指标已被某应急预案覆盖”。
3. 把 `coverage-overview` 与 `dashboard-overview` 的联动/预案覆盖率切换为基于显式绑定表计算。
4. 保持现有 `/api/linkage-rule/*`、`/api/emergency-plan/*`、前端页面和表单结构兼容，不新增独立维护页。
5. 为历史已有规则和预案提供幂等补账路径，避免新表只覆盖“改过的新数据”。

### 2.2 本轮非目标

1. 不在本轮新增联动/预案绑定的前端专门工作台。
2. 不在本轮引入 `iot_governance_task` 正式任务对象。
3. 不把联动规则和应急预案的业务表整体改造为“必须显式传 `riskMetricIds` 才能保存”。
4. 不在本轮重写现有联动规则表达式 DSL，也不重做应急预案表单结构。
5. 不把覆盖率结果落成独立统计真相表。

## 3. 推荐方案

本轮采用“显式绑定真相 + 写时同步 + 过渡补账”的渐进方案。

核心原则如下：

1. **覆盖率最终只认绑定表**  
   联动覆盖率与预案覆盖率一旦切换完成，`RiskGovernanceServiceImpl` 不再在常规读路径里做文本猜测。

2. **现有写入口保持兼容**  
   `LinkageRuleController`、`EmergencyPlanController` 路径、入参和前端 API 先不变。

3. **语义推断仅限“写时生成绑定”或“历史补账”**  
   当前还没有前台显式选指标入口，所以本轮允许后端在联动/预案写入时使用现有语义匹配规则，算出命中的 `riskMetricId` 集合并落入绑定表；但读路径不再每次动态猜测。

4. **补账必须幂等、可重跑**  
   历史规则和预案在首轮升级后，必须能被一次性或按需重复同步到绑定表，不依赖人工逐条编辑保存。

## 4. 数据对象设计

### 4.1 `risk_metric_linkage_binding`

定位：风险指标与联动规则之间的显式绑定关系。

建议字段：

1. `id`
2. `tenant_id`
3. `risk_metric_id`
4. `linkage_rule_id`
5. `binding_status`：`ACTIVE / INACTIVE`
6. `binding_origin`：`AUTO_INFERRED / MANUAL_CONFIRMED / BACKFILL`
7. `create_by`
8. `create_time`
9. `update_by`
10. `update_time`
11. `deleted`

建议约束：

1. 主键：`id`
2. 唯一索引：`uk_risk_metric_linkage_active (tenant_id, risk_metric_id, linkage_rule_id, deleted)`
3. 查询索引：`idx_risk_metric_linkage_rule (linkage_rule_id, deleted)`、`idx_risk_metric_linkage_metric (risk_metric_id, deleted)`

### 4.2 `risk_metric_emergency_plan_binding`

定位：风险指标与应急预案之间的显式绑定关系。

建议字段：

1. `id`
2. `tenant_id`
3. `risk_metric_id`
4. `emergency_plan_id`
5. `binding_status`：`ACTIVE / INACTIVE`
6. `binding_origin`：`AUTO_INFERRED / MANUAL_CONFIRMED / BACKFILL`
7. `create_by`
8. `create_time`
9. `update_by`
10. `update_time`
11. `deleted`

建议约束：

1. 主键：`id`
2. 唯一索引：`uk_risk_metric_plan_active (tenant_id, risk_metric_id, emergency_plan_id, deleted)`
3. 查询索引：`idx_risk_metric_plan_rule (emergency_plan_id, deleted)`、`idx_risk_metric_plan_metric (risk_metric_id, deleted)`

### 4.3 与现有对象的关系

1. `risk_metric_catalog` 继续作为桥层真相；绑定表中的 `risk_metric_id` 必须优先引用该表主键。
2. `linkage_rule` 和 `emergency_plan` 继续作为联动编排与预案资源真相；绑定表不复制它们的规则内容，只保存关系。
3. `risk_point_device` 继续作为“风险点正式绑定”真相；联动/预案覆盖率只统计已经正式绑定到风险点的指标。

## 5. 写路径设计

### 5.1 联动规则写路径

当前入口仍是：

1. `POST /api/linkage-rule/add`
2. `POST /api/linkage-rule/update`
3. `POST /api/linkage-rule/delete/{id}`

新增内部同步流程：

1. 保存联动规则主记录。
2. 基于当前规则的 `triggerCondition`，复用现有“语义提取指标标识”的解析逻辑，匹配 `risk_metric_catalog` 中启用中的目录指标。
3. 产出本规则当前命中的 `riskMetricId` 集合。
4. 将该集合与 `risk_metric_linkage_binding` 现有关系做差异比对：
   - 新增的关系插入绑定表
   - 已不存在的关系标记 `deleted=1` 或 `binding_status=INACTIVE`
   - 仍然存在的关系刷新 `update_by/update_time`

删除规则时：

1. 保持现有删除语义不变。
2. 同步把该 `linkage_rule_id` 对应的绑定关系全部置为删除或失效。

### 5.2 应急预案写路径

当前入口仍是：

1. `POST /api/emergency-plan/add`
2. `POST /api/emergency-plan/update`
3. `POST /api/emergency-plan/delete/{id}`

新增内部同步流程：

1. 保存预案主记录。
2. 基于 `planName / description / responseSteps / contactList / alarmLevel`，复用现有“可搜索文本”拼装逻辑，匹配启用中的 `risk_metric_catalog`。
3. 产出该预案当前命中的 `riskMetricId` 集合。
4. 与 `risk_metric_emergency_plan_binding` 的现有关系做差异同步。

删除预案时：

1. 保持现有删除语义不变。
2. 同步使对应绑定关系失效。

### 5.3 语义推断的边界

本轮语义推断只允许出现在以下两类场景：

1. 联动规则/预案新增或更新后的绑定生成
2. 历史规则/预案补账

禁止再把语义推断保留在长期读路径中作为覆盖率真实来源。

## 6. 历史补账设计

由于真实环境里已经存在历史联动规则和应急预案，本轮需要补账。

推荐采用“幂等服务 + 首次读触发兜底”的组合方案：

1. 新增一个内部补账服务，例如 `RiskMetricActionBindingBackfillService`。
2. 该服务支持：
   - `rebuildAllLinkageBindings()`
   - `rebuildAllEmergencyPlanBindings()`
   - `rebuildBindingsForLinkageRule(linkageRuleId)`
   - `rebuildBindingsForEmergencyPlan(emergencyPlanId)`
3. `LinkageRuleServiceImpl` / `EmergencyPlanServiceImpl` 写后直接调用“单对象重建”。
4. `RiskGovernanceServiceImpl#getCoverageOverview` 与 `getDashboardOverview` 在首次发现“存在启用中的联动/预案，但绑定表为空”时，触发一次幂等补账兜底。

设计说明：

1. 这样可以避免升级后必须人工先执行脚本，读侧才能恢复正确结果。
2. 兜底补账只在“绑定表显著缺失”时触发，不应变成每次读请求都重建。
3. 后续若需要更稳定的运维入口，再单独补脚本或后台任务，不在本轮扩大范围。

## 7. 读路径设计

### 7.1 `coverage-overview`

当前 `coverage-overview` 只返回：

1. `contractPropertyCount`
2. `publishedRiskMetricCount`
3. `boundRiskMetricCount`
4. `ruleCoveredRiskMetricCount`
5. `contractMetricCoverageRate`
6. `bindingCoverageRate`
7. `ruleCoverageRate`

本轮建议扩展为同时返回：

1. `linkageCoveredRiskMetricCount`
2. `emergencyPlanCoveredRiskMetricCount`
3. `linkagePlanCoveredRiskMetricCount`
4. `linkageCoverageRate`
5. `emergencyPlanCoverageRate`
6. `linkagePlanCoverageRate`

计算口径：

1. 分母仍以“已正式绑定到风险点的风险指标数/维度数”为准。
2. 联动覆盖率只读 `risk_metric_linkage_binding`
3. 预案覆盖率只读 `risk_metric_emergency_plan_binding`
4. 联动+预案覆盖率按“每个正式绑定指标是否同时具备联动与预案绑定”计算，不再做文本猜测。

### 7.2 `dashboard-overview`

本轮保留现有字段不变，但底层切换为显式绑定表：

1. `pendingLinkageCount`
2. `pendingEmergencyPlanCount`
3. `pendingLinkagePlanCount`
4. `linkageCoverageRate`
5. `emergencyPlanCoverageRate`
6. `linkagePlanCoverageRate`

兼容原则：

1. `CockpitView` 无需改接口名和字段名。
2. 只改变底层真相来源，不改变管理驾驶舱当前展示协议。

## 8. 模块边界

### 8.1 `spring-boot-iot-alarm`

本轮主写模块。

建议新增或修改的对象：

1. 新增实体：
   - `RiskMetricLinkageBinding`
   - `RiskMetricEmergencyPlanBinding`
2. 新增 Mapper：
   - `RiskMetricLinkageBindingMapper`
   - `RiskMetricEmergencyPlanBindingMapper`
3. 新增同步服务：
   - `RiskMetricActionBindingSyncService`
   - `RiskMetricActionBindingBackfillService`
4. 修改：
   - `LinkageRuleServiceImpl`
   - `EmergencyPlanServiceImpl`
   - `RiskGovernanceServiceImpl`

### 8.2 `sql` 与 `scripts`

1. `sql/init.sql` 增加两张关系表。
2. `scripts/run-real-env-schema-sync.py` 增加历史环境幂等补表逻辑。

### 8.3 `spring-boot-iot-ui`

本轮原则上不新增联动/预案维护入口。

仅在必要时同步：

1. `riskGovernance.ts` 的 `coverage-overview` 类型
2. 驾驶舱或产品工作台若已消费新增字段，则补齐 typing

## 9. 风险与取舍

### 9.1 为什么本轮仍允许语义推断参与写时绑定

因为当前页面没有“显式选风险指标”的维护入口，如果本轮完全禁止语义推断，现有联动规则和应急预案将无法生成绑定真相，范围会立刻扩大到前台改造。

因此，本轮接受以下取舍：

1. 写时仍可用语义推断生成绑定
2. 读时只认绑定表

这比“读时每次重新猜”更稳定，也为下一轮补最小维护入口留出清晰边界。

### 9.2 绑定表与历史规则可能短暂不一致

在升级完成但补账尚未跑到之前，历史规则可能尚未形成绑定表记录。为此本轮已经设计了首次读兜底补账。

### 9.3 不引入统计真相表

覆盖率、缺口和驾驶舱 KPI 仍然实时聚合，不新增独立统计表，避免形成“绑定真相表 + KPI 真相表”双真相。

## 10. 验收口径

本轮完成后，至少要满足以下口径：

1. `coverage-overview` 与 `dashboard-overview` 的联动/预案覆盖率不再依赖运行时文本猜测。
2. 新增或编辑一条联动规则后，可在绑定表中看到对应 `riskMetricId` 关系。
3. 新增或编辑一条应急预案后，可在绑定表中看到对应 `riskMetricId` 关系。
4. 历史已有规则和预案可通过幂等补账进入绑定表，不要求人工逐条重存。
5. 现有联动规则页、应急预案页和驾驶舱页面路径保持兼容。
6. 文档能明确回答“联动覆盖率 / 预案覆盖率的正式真相来自哪里”。

## 11. 后续衔接

本轮完成后，下一层自然衔接为两条路线：

1. **最小维护入口**  
   在联动规则页和应急预案页补“显式选择风险指标”能力，把 `binding_origin` 从 `AUTO_INFERRED` 逐步升级为 `MANUAL_CONFIRMED`。

2. **正式任务对象**  
   在 `iot_governance_task` 落地后，把“待补联动预案”从聚合数字升级为可认领、可关闭、可复盘的正式任务。
