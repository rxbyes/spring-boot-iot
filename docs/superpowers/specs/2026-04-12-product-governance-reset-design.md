# 产品契约治理链重置设计

## 1. 背景

当前 `/products -> 契约字段` 工作区已经演进为“手动样本 compare -> 审批 -> 正式发布 -> 风险指标目录 -> 控制面追踪”的完整治理链。你这次的目标不是删除产品主档或设备主档，而是把产品定义中心恢复到“只保留产品和设备/子设备关系主数据，其余契约治理痕迹全部清空”的状态，以便后续按新的业务约定重新录入。

本次重置必须以真实环境 `application-dev.yml` 对应的共享 MySQL 基线为准，禁止回退到 H2 或其他废弃验收路径。

## 2. 目标

- 保留产品主档 `iot_product`
- 保留设备主档 `iot_device`
- 保留子设备关系主数据 `iot_device_relation`
- 保留规范字段库 `iot_normative_metric_definition`
- 保留固定复核策略 `sys_governance_approval_policy`
- 清空产品契约治理、风险指标目录、审批/工作项留痕及其直接派生数据
- 对 `iot_vendor_metric_mapping_rule` 做条件保留：保留激光测距、深部位移等涉及 `collector_child` 关系收口所需的映射；其余产品映射规则清理
- 清理后，产品定义中心只剩产品信息；进入工作台的 `契约字段` 页不应再显示历史正式字段、历史版本台账、历史风险指标目录或旧审批回执

## 3. 非目标

- 不删除产品记录
- 不删除设备记录
- 不删除 `iot_device_relation`
- 不删除规范字段定义和审批策略种子
- 不处理 TDengine 历史遥测数据
- 不重建产品、设备或子设备关系，只为后续人工重新录入腾空治理链

## 4. 约束与假设

- 当前分支必须保持在 `codex/dev`
- 实际执行库为 `application-dev.yml` 默认 MySQL：`rm_iot`
- `iot_device_relation` 是激光测距、深部位移等子设备关联的正式真相源，不能误删
- 若真实库中存在激光测距、深部位移产品的额外映射规则，且这些规则仍服务于 `collector_child` 场景，则需要保留
- 若审批/工作项/复盘记录与产品契约发布直接关联，则必须一起清理，否则控制面会保留旧治理痕迹

## 5. 数据范围设计

### 5.1 保留表

- `iot_product`
- `iot_device`
- `iot_device_relation`
- `iot_normative_metric_definition`
- `sys_governance_approval_policy`

### 5.2 需要清理的表

#### 5.2.1 契约正式真相与发布批次

- `iot_product_model`
- `iot_product_contract_release_batch`
- `iot_product_contract_release_snapshot`

#### 5.2.2 风险目录与下游绑定

- `risk_metric_catalog`
- `risk_metric_linkage_binding`
- `risk_metric_emergency_plan_binding`
- `risk_point_device`
- `rule_definition`

#### 5.2.3 治理证据与映射

- `iot_vendor_metric_evidence`
- `iot_vendor_metric_mapping_rule`

其中 `iot_vendor_metric_mapping_rule` 只保留满足以下条件的记录：

- 属于激光测距或深部位移产品
- 且确实用于 `collector_child` 相关场景的重新绑定基线

除上述保留规则外，其余产品映射规则全部删除。

#### 5.2.4 审批与控制面留痕

- `sys_governance_approval_order` 中 `PRODUCT_CONTRACT_RELEASE_APPLY`、`PRODUCT_CONTRACT_ROLLBACK` 相关记录
- `sys_governance_approval_transition` 中对应审批流转记录
- `iot_governance_work_item` 中由产品契约发布、批次、风险目录派生的治理任务
- `sys_governance_replay_feedback` 中与上述审批单、发布批次相关的复盘记录

### 5.3 产品主档内需要重置的派生配置

`iot_product.metadata_json` 中由正式契约派生出的对象洞察配置不能保留旧值，否则产品虽然没有正式字段，页面仍可能显示旧趋势指标或旧重点指标。执行时需把 `$.objectInsight` 清空或移除。

## 6. 推荐执行顺序

为避免外键语义和读侧残留，执行顺序按“最下游引用 -> 上游真相源”回收：

1. 查询并记录需保留的产品映射规则清单
2. 清理 `risk_metric_linkage_binding`、`risk_metric_emergency_plan_binding`
3. 清理 `risk_point_device`、`rule_definition`
4. 清理 `risk_metric_catalog`
5. 清理 `sys_governance_replay_feedback`
6. 清理 `iot_governance_work_item`
7. 清理 `sys_governance_approval_transition`
8. 清理 `sys_governance_approval_order`
9. 清理 `iot_product_contract_release_snapshot`
10. 清理 `iot_product_contract_release_batch`
11. 清理 `iot_vendor_metric_evidence`
12. 条件清理 `iot_vendor_metric_mapping_rule`
13. 清理 `iot_product_model`
14. 重置 `iot_product.metadata_json` 中的 `objectInsight`
15. 复核保留数据：`iot_product`、`iot_device`、`iot_device_relation`、保留的映射规则

## 7. 推荐实现方式

本次优先直接执行一组显式 SQL，而不是先开发通用后台功能。原因：

- 任务目标是一次性共享环境治理重置，不是面向终端用户的产品功能
- 当前仓库没有现成“按产品契约治理链重置但保留关系主数据”的脚本
- 直接 SQL 更容易逐表核对、留痕和必要时人工回滚

执行动作会先做只读盘点，再执行删除/更新 SQL，再做只读校验。

## 8. 校验标准

### 8.1 数据校验

- `iot_product` 记录仍在
- `iot_device` 记录仍在
- `iot_device_relation` 记录仍在
- `iot_product_model` 为空
- `iot_product_contract_release_batch` 为空
- `iot_product_contract_release_snapshot` 为空
- `risk_metric_catalog` 为空
- `risk_point_device` 为空
- `rule_definition` 为空
- `iot_vendor_metric_evidence` 为空
- `iot_vendor_metric_mapping_rule` 只剩保留白名单
- 契约发布/回滚相关审批单、流转、工作项、复盘记录已清空
- `iot_product.metadata_json.objectInsight` 已清空

### 8.2 页面预期

- 产品定义中心列表还能正常展示产品
- 打开任一产品工作台，`契约字段` 不再出现历史正式字段和发布台账
- `版本台账` 不再显示历史合同发布批次
- 治理控制面不再显示旧产品契约发布相关待办或审批痕迹

## 9. 风险与回退

### 9.1 主要风险

- 误删 `iot_device_relation` 或误删需要保留的子设备关系映射规则
- 只清上游表、未清控制面留痕，导致页面继续展示旧审批或旧任务
- 未清 `metadata_json.objectInsight`，导致对象洞察仍读取旧产品指标配置

### 9.2 防守措施

- 执行前先做只读盘点并导出待保留映射规则清单
- 执行 SQL 时按表分段，逐段校验行数变化
- 删除前记录每张表当前计数与关键保留记录
- 执行后再次核对 `iot_device_relation` 和保留映射规则数量

### 9.3 回退思路

本次是面向“重新按业务约定录入”的重置动作，逻辑上以重新录入为主，不设计自动数据回填。若执行中发现误删风险，立即停止后续 SQL，基于执行前盘点结果人工恢复保留数据。

## 10. 受影响模块与资产

- 后端运行库：`rm_iot`
- 页面语义：`/products -> 契约字段`、`/governance-approval`、`/governance-task`
- 文档：本次只新增设计文档；若后续沉淀为常规脚本或运维 SOP，再同步更新权威文档

## 11. 实施后的交付物

- 一组已执行的 SQL 清理动作
- 执行前后关键表计数对比
- 保留的 `collector_child` 关系与映射规则核对结果
- 页面级抽查结果：产品还在、契约治理链已清空
