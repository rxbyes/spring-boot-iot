# 产品治理数据重置与产品经营台拆页设计

- 日期：2026-04-19
- 状态：已评审，待进入实施计划
- 适用仓库：`spring-boot-iot`
- 相关模块：`spring-boot-iot-device`、`spring-boot-iot-alarm`、`spring-boot-iot-ui`

## 1. 背景

当前 `/products` 已被收口为产品定义中心，同时又承接契约治理、映射规则治理、版本台账、回滚试算、对象洞察配置入口和风险目录联动入口。虽然这条路径在功能上闭环，但页面职责混叠严重，已经出现两类问题：

1. 产品经营台的认知负担过高，用户难以理解“样本提取、规则治理、发布批次、回滚、对象洞察”的先后关系。
2. 产品治理派生数据已经大量沉淀到合同发布、映射快照、风险目录和下游绑定中，不利于按“产品定义 + 设备资产”重新建立关系。

本次设计的目标不是删除主档，而是保留产品、设备和复合设备关系映射，清空产品治理派生业务数据，并把产品经营台重构为同一入口下的多个独立工作页。

## 2. 用户确认后的约束

### 2.1 保留对象

- 保留 `iot_product`
- 保留 `iot_device`
- 保留 `iot_device_relation`
- 保留现有设备与产品绑定关系，例如 `iot_device.product_id`

### 2.2 清理原则

- 只清空业务数据，不删除表结构
- 通过产品定义、设备资产重新建立关系
- `risk_metric_catalog` 清理后，其下游绑定和策略数据一起清掉
- 产品经营台改为保留 `/products` 入口，但拆成多个独立工作页
- `契约字段` 收窄为“字段提取与正式字段确认”页，不再承载映射规则和版本治理

### 2.3 本次不变部分

- 不删除产品主档、设备主档、复合设备关系映射
- 不变更设备接入主链路
- 不清理运行态遥测、latest、消息日志
- 不改为多入口菜单，不新增 `/products` 之外的一级业务菜单

## 3. 目标与非目标

### 3.1 目标

1. 重置产品治理派生数据，恢复到“只有产品和设备主档存在”的基线。
2. 保持设备与产品的当前绑定不变，避免资产主档失联。
3. 让 `/products` 回到“台账入口 + 产品详情工作页导航”的语义。
4. 把产品经营台拆成职责单一、边界明确的独立工作页。
5. 为后续重新建立契约字段、映射规则、发布批次和风险绑定提供干净起点。

### 3.2 非目标

- 不重做设备资产中心
- 不重构协议治理工作台
- 不清空产品、设备、风险点、联动规则、预案主档
- 不删除数据库表结构
- 不在本轮引入新的治理流程或新的审批模型

## 4. 方案比较

### 4.1 方案 A：保守重建（推荐）

- 保留 `iot_product`、`iot_device`、`iot_device_relation` 和现有设备-产品绑定
- 清空产品治理派生数据、风险目录和其下游绑定
- 保留跨域基础主档，例如 `risk_point`、`linkage_rule`、`emergency_plan`、`sys_governance_*`
- 前端将 `/products` 拆成同一入口下的多个独立工作页

优点：

- 风险可控，不会误伤资产主档
- 能快速回到“重新治理”的起点
- 数据恢复边界清晰

缺点：

- 库中仍保留基础治理设施和其他域数据，不是全库完全空白

### 4.2 方案 B：深度重建

- 在方案 A 基础上进一步清理产品治理相关审批、治理待办和更多历史痕迹

优点：

- 治理历史更干净

缺点：

- 会碰共享治理基础设施
- 风险显著上升，容易影响其他域

### 4.3 方案 C：双轨迁移

- 保留现状页面和数据，新增一套新路由和新治理基线，验证后再切换

优点：

- 回退最容易

缺点：

- 实现成本最高
- 与“删干净再重建”的目标不一致

### 4.4 推荐结论

采用方案 A。它满足“保留主档、清空派生数据、重新建立治理关系”的目标，同时避免共享真实环境被无边界破坏。

## 5. 数据重置设计

### 5.1 保留边界

保留以下表中的业务行：

- `iot_product`
- `iot_device`
- `iot_device_relation`
- `risk_point`
- `linkage_rule`
- `emergency_plan`
- `sys_governance_approval_policy`

保留以下关系：

- `iot_device.product_id`
- 复合设备父子映射及逻辑通道映射

### 5.2 清理对象

本次需要清空的产品治理派生数据包括：

- `iot_product_model`
- `iot_vendor_metric_evidence`
- `iot_vendor_metric_mapping_rule`
- `iot_vendor_metric_mapping_rule_snapshot`
- `iot_product_contract_release_batch`
- `iot_product_contract_release_snapshot`
- `iot_product_metric_resolver_snapshot`
- `risk_metric_catalog`
- `risk_metric_linkage_binding`
- `risk_metric_emergency_plan_binding`
- `risk_point_device`
- `risk_point_device_capability_binding`
- `risk_point_device_pending_binding`
- `risk_point_device_pending_promotion`

定向清理对象：

- `rule_definition` 中命中本次产品治理链的规则数据，仅按 `risk_metric_id` 或 `metric_identifier` 定向删除，不整表删除
- `iot_governance_work_item` 中属于产品治理和映射规则治理的待办
- `sys_governance_approval_order`、`sys_governance_approval_transition` 中属于产品合同发布和映射规则发布/回滚的审批记录
- `iot_device_onboarding_case.release_batch_id` 置空，并将流程状态回退到 `CONTRACT_RELEASE`

### 5.3 主档字段复位

虽然 `iot_product` 主档保留，但需要同步清理其治理型扩展信息，避免主档仍携带旧治理结果。至少包括：

- `metadata_json.objectInsight.customMetrics[]`
- 任何会继续驱动 `/products`、`/insight`、治理提示和工作台状态判断的产品经营扩展字段

原则是：保留产品身份，不保留旧治理态。

### 5.4 删除顺序

为避免外键或业务依赖残留，执行顺序固定为：

1. 删除风险闭环下游绑定：
   - `risk_metric_linkage_binding`
   - `risk_metric_emergency_plan_binding`
   - `risk_point_device_pending_promotion`
   - `risk_point_device_pending_binding`
   - `risk_point_device_capability_binding`
   - `risk_point_device`
2. 删除命中产品治理链的阈值规则数据：
   - `rule_definition`
3. 删除 `risk_metric_catalog`
4. 删除合同发布与解析快照：
   - `iot_product_metric_resolver_snapshot`
   - `iot_product_contract_release_snapshot`
   - `iot_product_contract_release_batch`
5. 删除映射治理数据：
   - `iot_vendor_metric_mapping_rule_snapshot`
   - `iot_vendor_metric_mapping_rule`
   - `iot_vendor_metric_evidence`
6. 删除 `iot_product_model`
7. 复位 `iot_product.metadata_json`
8. 复位跨域残留：
   - `iot_device_onboarding_case.release_batch_id`
   - `iot_governance_work_item`
   - `sys_governance_approval_order`
   - `sys_governance_approval_transition`

### 5.5 执行方式

本次不通过前端按钮，也不提供 HTTP 接口。统一采用仓库内管理脚本执行。

脚本要求：

- 位置：`scripts/`
- 运行环境：真实 `dev` 配置
- 模式：
  - `dry-run`
  - `backup`
  - `execute`
- 默认只允许 `dry-run`
- 真正执行必须同时提供 `--execute --confirm`
- 支持范围：
  - `全量`
  - `按 tenant`
  - `按 productIds`

操作约束：

- 优先使用 `DELETE` / `UPDATE`
- 禁止使用 `TRUNCATE`
- 执行前必须导出受影响数据摘要或备份
- 执行后必须输出删除计数、复位计数、跳过计数和异常明细

## 6. 重建后的业务顺序

重置完成后，产品治理的重建顺序固定为：

1. 保持产品、设备、设备关系映射不动
2. 重新建立 `契约字段`
3. 建立 `映射规则`
4. 形成 `发布批次`
5. 生成 `risk_metric_catalog`
6. 重新绑定风险点、阈值规则、联动和预案
7. 最后重新配置对象洞察重点指标

这条顺序明确表达“产品定义 -> 发布 -> 风险闭环 -> 洞察配置”的依赖链，不再把对象洞察和字段提取混成一个入口。

## 7. 产品经营台信息架构重构

### 7.1 路由策略

保留 `/products` 作为产品定义中心入口，但进入具体产品后，改为独立工作页：

- `/products`
- `/products/:productId/overview`
- `/products/:productId/devices`
- `/products/:productId/contracts`
- `/products/:productId/mapping-rules`
- `/products/:productId/releases`

### 7.2 页面职责

#### `/products`

作用：

- 产品台账入口
- 回答“有哪些产品、状态如何、是否需要进入具体工作页处理”

不再承担：

- 打开一个大而全的经营抽屉来承载全部治理动作

#### `/products/:productId/overview`

作用：

- 产品总览页

内容：

- 产品基础信息
- 当前设备数
- 当前正式字段数
- 最新发布批次摘要
- 当前治理状态
- 下一步建议动作入口

#### `/products/:productId/devices`

作用：

- 关联设备页

内容：

- 当前产品绑定的设备列表
- 在线/离线情况
- 复合设备关系入口
- 设备资产实际承载情况

#### `/products/:productId/contracts`

作用：

- 契约字段页

保留内容：

- 样本输入
- 识别结果
- 本次生效
- 当前已生效字段

明确移出内容：

- 映射规则建议
- 映射规则台账
- 版本台账
- 回滚试算

#### `/products/:productId/mapping-rules`

作用：

- 映射规则页

内容：

- 映射规则建议
- 映射规则台账
- 规则试命中
- 发布审批
- 回滚审批

#### `/products/:productId/releases`

作用：

- 版本台账页

内容：

- 发布批次列表
- 批次详情
- 跨批次字段差异
- 风险指标目录差异
- 回滚试算

### 7.3 设计原则

- `契约字段` 回到“字段提取与正式字段确认”的单一主语义
- `映射规则` 作为独立治理对象，不再附属在样本提取页中
- `版本台账` 与 `回滚试算` 都属于发布后治理，不再放在契约提取流程中
- `产品总览` 只负责聚合与导航，不承载深治理操作

## 8. 兼容与迁移策略

现有路径以 `/products?openProductId=...&workbenchView=...` 为中心，需要做一轮兼容跳转。

兼容规则：

- `overview` -> `/products/:productId/overview`
- `devices` -> `/products/:productId/devices`
- `models` -> `/products/:productId/contracts`
- `edit` -> 保留为编辑档案入口，后续可继续作为详情内动作，不强制独立工作页

受影响的深链来源至少包括：

- `治理任务分发`
- `/device-onboarding`
- `/insight` 引导文案
- 首页经营提示
- 自动化计划中的 `/products` 路由描述

## 9. 前端落地设计

### 9.1 新结构

建议新增统一详情壳层，例如 `ProductDetailShell`，只负责：

- 标识当前产品是谁
- 标识当前处于哪个工作页
- 提供切换到其他工作页的导航

在此基础上拆出现有工作区组件：

- `ProductOverviewWorkspace`
- `ProductDevicesWorkspace`
- `ProductContractsWorkspace`
- `ProductMappingRulesWorkspace`
- `ProductReleasesWorkspace`

### 9.2 现有组件处理

- `ProductWorkbenchView.vue` 从“台账 + 大抽屉工作台”重构为“台账入口 + 路由跳转入口”
- `ProductBusinessWorkbenchDrawer.vue` 先降级为兼容层，后续逐步移除
- `ProductModelDesignerWorkspace.vue` 只保留契约字段相关能力，映射规则和版本台账逻辑迁出

### 9.3 复用原则

前端继续复用现有共享模式：

- `StandardWorkbenchPanel`
- `PanelCard`
- `StandardTableToolbar`
- `StandardInlineState`
- `StandardDetailDrawer`
- `IotAccessPageShell`

不新增页面私有分页、筛选壳或新的颜色体系。

## 10. 后端与接口设计

### 10.1 可复用接口

当前已具备基础支撑的接口：

- `contracts`
  - compare / apply / models
- `mapping-rules`
  - vendor mapping rules 全套接口
- `releases`
  - release batches / impact / rollback
- `devices`
  - 设备分页查询

### 10.2 建议新增接口

建议补一个统一总览聚合接口：

- `GET /api/device/product/{productId}/overview-summary`

统一返回：

- 产品基础信息
- 设备数量摘要
- 正式字段数量
- 最新发布批次
- 当前治理状态
- 下一步建议动作

该接口用于让 `overview` 页不再散落多个读侧拼装逻辑。

### 10.3 清理脚本边界

清理逻辑不进入 Controller，不纳入业务运行主链路。统一作为管理脚本保存在 `scripts/` 下，由真实环境管理员手工执行。

## 11. 验收标准

### 11.1 脚本验收

- `dry-run` 可输出所有待清理对象数量，不写库
- `backup` 可产出备份文件
- `execute` 可按顺序删除并输出统计

### 11.2 数据验收

执行后应满足：

- `iot_product`、`iot_device`、`iot_device_relation` 仍保留
- 现有设备与产品绑定仍保留
- `iot_product_model`、发布批次、映射规则、证据、resolver、风险目录及下游绑定为空
- `iot_product.metadata_json` 中旧对象洞察配置已清空
- `iot_device_onboarding_case.release_batch_id` 不再悬挂旧发布批次

### 11.3 页面验收

- `/products/:productId/overview` 可正常进入
- `/products/:productId/contracts` 显示“未建立契约字段”的空态
- `/products/:productId/mapping-rules`、`/products/:productId/releases` 显示无数据空态，不报错
- `/insight` 不再展示旧重点指标配置
- 旧 `workbenchView` 深链仍能正确跳转到新路由

## 12. 文档更新要求

本设计进入实施后，至少同步更新以下文档：

- `README.md`
- `docs/02-业务功能与流程说明.md`
- `docs/03-接口规范与接口清单.md`
- `docs/04-数据库设计与初始化数据.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/15-前端优化与治理计划.md`
- `docs/21-业务功能清单与验收标准.md`

## 13. 风险与注意事项

- 共享真实环境中存在大量其他进行中的变更，本次必须限定为新增 spec 文档和后续明确范围内的实现，不得顺手回退现有脏工作区改动。
- 产品治理数据清空后，接入台、治理台、对象洞察和风险绑定会短时间进入空态，这是预期行为，不是故障。
- `rule_definition` 只能做产品治理链定向清理，不能整表清空，否则会误伤其他业务。
- 审批与待办只能按动作类型定向清理，不能整域删除。

## 14. 实施前置条件

进入实施前，需要完成：

1. 先编写实施计划，拆分为“数据清理脚本”和“前端路由拆页”两个子任务
2. 明确清理脚本使用的真实环境连接与备份目录
3. 确认前端兼容跳转保留周期

## 15. 结论

本次采用“保留主档、清空治理派生数据、拆分经营工作页”的重建方案。核心原则是：

- 主档保留
- 派生治理数据清空
- 契约字段收窄
- 映射规则、版本台账独立
- 风险闭环跟随正式发布重建

该设计将 `/products` 从“混杂的大工作区”重构为“清晰的产品治理入口 + 多工作页”，同时为重新建立产品定义与设备资产关系提供可执行的干净基线。
