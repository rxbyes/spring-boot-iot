# 产品物模型规范证据与报文证据分批治理设计

**Date:** 2026-04-03  
**Status:** Approved in-session for spec drafting  
**Audience:** 设备中心 / 产品治理 / 前后端实现 / 文档维护  
**Scope:** 在 `/products -> 物模型双证据治理` 里，把现有“手动提炼 + 自动提炼”升级为“规范证据 + 报文证据 + 正式基线”的产品契约治理方法，并按产品族分批落地，首批固定为倾角 / 加速度 / 裂缝一体机的 `property` 正式模型治理。

## 1. 背景

当前仓库已经具备以下稳定基线：

1. `/products` 内已存在 `ProductModelDesignerWorkspace` 和 `ProductModelDesignerDrawer`，并形成 `compare -> apply -> iot_product_model` 的正式契约闭环。
2. 后端已提供：
   - `POST /api/device/product/{productId}/model-governance/compare`
   - `POST /api/device/product/{productId}/model-governance/apply`
3. 当前 compare 骨架按 `manualCandidate / runtimeCandidate / formalModel` 三侧表达证据，适合继续承载治理增强，而不是推翻重做。
4. 当前仓库已明确：
   - 正式模型唯一落点仍是 `iot_product_model`
   - 不新增草稿表、候选持久化表、模板实例表
   - `/products` 内默认治理流程仍以显式 compare / apply 为准
5. `docs/appendix/iot-field-governance-and-sop.md` 已经沉淀了地灾监测字段治理标准和一体机首批建议字段清单，具备转化为“规范证据”的直接基础。

当前问题不在于“系统能不能继续提炼候选”，而在于：

1. 现有“手动证据”更接近样本 JSON 和人工补录，尚未把行业技术规范作为正式目标骨架。
2. 报文里出现的字段天然带有厂家命名、历史兼容命名和调试字段噪音，不能直接等价为正式模型。
3. 如果直接把地灾技术规范全量一次性 apply 到产品，会把尚未被真实报文验证的字段一并带入正式契约。

因此，本轮要解决的核心问题是：把“技术规范定义了什么”和“真实设备稳定上报了什么”并列收口，再由人工显式确认哪些字段进入产品正式契约。

## 2. 目标

本设计服务以下目标：

1. 把 `/products` 内物模型治理的主判据，从“样本驱动”升级为“规范驱动 + 报文验证”。
2. 首批围绕单一产品族先做最小闭环，避免一口气把整个行业规范搬成正式模型。
3. 正式模型统一采用规范化 `identifier`，原始报文字段只保留在 compare 证据层。
4. 保持 `iot_product_model` 仍是唯一正式契约表，不新增平行草稿和模板实例体系。
5. 让同一治理方法在首批一体机跑通后，可复制到 `GNSS`、`深部位移` 等下一批产品族。

## 3. 非目标

本轮明确不做以下事情：

1. 不新增行业模板管理平台、模板表或模板实例表。
2. 不把整张地灾技术规范图片全文直接原样转成某一个产品的正式物模型。
3. 不允许自动证据绕过 compare / apply 直接写入 `iot_product_model`。
4. 不把 `event / service` 一并纳入首批正式 apply 范围。
5. 不把一体机、GNSS、深部位移等多类产品混成同一个正式产品模型。
6. 不新增一级路由，不拆出新的并行治理页。

## 4. 方案对比与选型

### 4.1 方案 A：单产品、单批次、属性优先

- 围绕一个具体产品执行治理。
- 首批只收口 `property`。
- 首批范围只覆盖 `L1 变形监测 + 设备状态参数`。
- 正式模型统一采用规范化 `identifier`。

优点：

1. 最贴合当前 `/products -> compare -> apply` 实现。
2. 风险最小，最容易形成第一批可真实 apply、可复验的正式契约。
3. 便于后续按同法复制到下一批产品族。

缺点：

1. 首批覆盖范围较窄，`L2/L3/L4` 不会立即进入正式模型。

### 4.2 方案 B：单产品、宽范围首批纳入

- 仍按具体产品治理，但首批尽量把规范图里所有相关字段一次纳入。

优点：

1. 短期覆盖面更大。

缺点：

1. 容易把暂时没有真实报文支撑的字段一并纳入正式模型。
2. compare 行、人工裁决量和误入库风险都会明显增加。

### 4.3 方案 C：先做行业通用模板，再映射产品

- 先沉淀行业通用标准模板，再映射到具体产品。

优点：

1. 抽象层次最高，长期复用价值大。

缺点：

1. 会偏离当前“选中产品即治理目标”的系统结构。
2. 容易把本轮任务扩成一套模板系统建设。

### 4.4 选型

本轮采用 `方案 A：单产品、单批次、属性优先`。

进一步固化为以下执行顺序：

1. 首批：`倾角 / 加速度 / 裂缝一体机`
2. 第二批：`GNSS`
3. 第三批：`深部位移`
4. 后续批次再按同一治理方法扩到其他产品族

## 5. 已确认决策

本次会话已明确确认以下设计决策：

1. 首批治理载体是“单一产品正式模型”，不是行业总模板。
2. 首批样板产品族固定为 `倾角 / 加速度 / 裂缝一体机`。
3. 首批正式 apply 只纳入 `property`，不纳入 `event / service`。
4. 首批字段范围固定为：
   - `L1 变形监测`
   - `设备状态参数`
5. 正式模型中的 `identifier` 直接采用规范化命名。
6. 原始报文字段名只作为 compare 证据和映射来源，不直接成为正式字段名。

## 6. 治理目标与范围边界

### 6.1 治理对象

本轮治理对象是 `/products` 中一个具体产品的正式物模型，而不是行业标准库。

### 6.2 首批正式合同范围

第一批只纳入 `property`，只收口以下两类：

1. `L1 变形监测`
2. `设备状态参数`

对一体机产品，首批推荐字段骨架固定为：

1. 倾角
   - `L1_QJ_1.X`
   - `L1_QJ_1.Y`
   - `L1_QJ_1.Z`
   - `L1_QJ_1.angle`
   - `L1_QJ_1.AZI`
2. 加速度
   - `L1_JS_1.gX`
   - `L1_JS_1.gY`
   - `L1_JS_1.gZ`
3. 裂缝
   - `L1_LF_1.value`
4. 设备状态
   - `S1_ZT_1.battery_dump_energy`
   - `S1_ZT_1.ext_power_volt`
   - `S1_ZT_1.solar_volt`
   - `S1_ZT_1.temp`
   - `S1_ZT_1.humidity`
   - `S1_ZT_1.signal_4g`
   - `S1_ZT_1.signal_NB`
   - `S1_ZT_1.signal_bd`
   - `S1_ZT_1.sw_version`
   - `S1_ZT_1.pa_state`
   - `S1_ZT_1.lon`
   - `S1_ZT_1.lat`
   - `S1_ZT_1.sensor_state.L1_QJ_1`
   - `S1_ZT_1.sensor_state.L1_JS_1`
   - `S1_ZT_1.sensor_state.L1_LF_1`

### 6.3 明确不做

1. 不把 `L2/L3/L4` 首批一起 apply。
2. 不把规范图片中所有条目无差别纳入正式模型。
3. 不因为报文里“出现过一次”就直接纳入正式模型。
4. 不因为规范里“写了字段”就绕过真实报文验证直接批量 apply。

## 7. 证据模型

### 7.1 四层证据角色

本轮治理统一拆成四层：

1. `规范证据`
   - 来源：地灾技术规范整理后的结构化字段清单。
   - 作用：定义正式目标字段骨架。
2. `辅助人工证据`
   - 来源：样本 JSON、人工补录说明、字段备注。
   - 作用：辅助人工理解，不高于规范证据。
3. `报文证据`
   - 来源：latest、消息日志、稳定上报字段。
   - 作用：验证规范字段是否真实、稳定存在。
4. `正式基线`
   - 来源：`iot_product_model`。
   - 作用：判断字段是否已存在以及是否需要修订。

首批主判据固定为：

1. `规范证据`
2. `报文证据`
3. `正式基线`

### 7.2 compare 行主键

compare 行围绕“规范化 `identifier`”建行，而不是围绕原始报文字段名建行。

例如：

1. `L1_QJ_1.X`
2. `L1_JS_1.gX`
3. `L1_LF_1.value`
4. `S1_ZT_1.temp`

原始报文字段名如 `x`、`angleX`、`g_x`、`temp` 等，只记录在该行证据快照中，作为来源和疑似映射说明。

### 7.3 首批规范证据资产

首批一体机规范证据来自 `docs/appendix/iot-field-governance-and-sop.md` 中的首批字段治理清单，并要求在 compare 前整理为结构化字段资产。

每条规范证据至少包含：

1. `identifier`
2. `modelName`
3. `dataType`
4. `unit`
5. `requiredFlag`
6. `specsJson`
7. `description`
8. `normativeSource`
   - 例如 `表 B.1`
   - 例如 `表 F.1`
9. `monitorContentCode`
10. `monitorTypeCode`
11. `sensorCode`

### 7.4 报文证据收口方式

首批报文证据只服务于 `property`，主要来自：

1. latest 属性快照
2. 消息日志中的稳定字段
3. 样本 JSON 的叶子路径

每条报文证据至少保留：

1. 观测到的原始字段名
2. 推断值类型
3. 最近上报时间
4. 来源链路或来源表
5. 命中次数或稳定度
6. 是否存在命名漂移风险

## 8. compare 规则

### 8.1 compare 优先级

首批 compare 的固定优先级为：

1. `规范证据` 定义目标字段
2. `报文证据` 验证该字段是否被真实设备稳定支撑
3. `正式基线` 判断是否已存在或需要修订

### 8.2 compare 状态语义

当前 compare 状态保持原有枚举，但语义升级如下：

1. `double_aligned`
   - 规范证据和报文证据都命中同一个规范化字段
   - 类型、单位、语义兼容
   - 正式模型中尚不存在该字段时，默认建议 `纳入新增`
2. `manual_only`
   - 首批语义等价于“仅规范命中”
   - 规范定义了字段，但当前缺少稳定报文支撑
   - 默认建议 `继续观察`
3. `runtime_only`
   - 报文里存在稳定字段，但当前规范清单没有对应目标字段
   - 默认建议 `继续观察`
4. `formal_exists`
   - 正式模型已存在对应规范字段
   - 当前没有明显定义冲突
   - 默认建议 `忽略`，必要时允许 `纳入修订`
5. `suspected_conflict`
   - 类型不一致、单位不一致、命名映射不清、一个原始字段疑似映射多个规范字段等
   - 默认建议 `人工裁决`
6. `evidence_insufficient`
   - 只有低置信度痕迹或只有辅助人工证据，没有形成规范 + 报文闭环
   - 默认不 apply

### 8.3 apply 决策原则

1. `create`
   - 必须至少有明确规范证据
   - 优先要求同时有稳定报文证据
2. `update`
   - 正式模型已存在
   - 规范证据明确要求修订
   - 报文证据不反对该修订
3. `runtime_only`
   - 不允许仅因“报文里出现过”就直接入正式模型
4. `manual_only`
   - 不允许仅因“规范里写了”就默认批量 apply

### 8.4 风险标记建议

首批 compare 行保留当前 `riskFlags` 结构，并重点使用以下标记：

1. `definition_mismatch`
2. `needs_review`
3. `formal_baseline`
4. `manual_missing`
5. `runtime_missing`
6. `suspected_match`

## 9. 工作台设计与交互流程

### 9.1 页面位置

继续复用现有：

1. `/products`
2. `ProductModelDesignerWorkspace`
3. `ProductModelDesignerDrawer`
4. `ProductModelGovernanceCompareTable`

不新增一级路由，不拆独立“行业规范治理页”。

### 9.2 首批工作流

首批治理顺序固定为：

1. 选择目标产品
2. 选择治理模板
   - 首批固定为 `倾角 / 加速度 / 裂缝一体机`
3. 装载规范字段清单
4. 拉取报文证据
5. compare -> 决策 -> apply

样本 JSON 继续保留，但降级为辅助核对工具，不再是首批 compare 的主入口。

### 9.3 证据入口区

证据入口区调整为两类主入口：

1. `规范证据入口`
   - 负责装载一体机首批规范字段骨架
2. `报文证据入口`
   - 负责拉取当前产品的真实上报证据

样本 JSON 入口作为辅助工具保留，用于以下场景：

1. 当前产品真实报文不足时的辅助核对
2. 某些字段路径需要人工解释时的补充说明

### 9.4 compare 表

首批 compare 表仍按 `property / event / service` 切换，但默认锁定在 `property`。

实现上继续复用当前三栏结构，但语义升级为：

1. `manualCandidate`
   - 首批默认承载 `规范证据`
   - 样本 JSON 和人工补录只作为该栏下的辅助来源
2. `runtimeCandidate`
   - 继续承载真实报文证据
3. `formalModel`
   - 继续承载 `iot_product_model` 正式基线

每一行至少展示：

1. `规范目标`
   - `identifier`
   - 中文名
   - 数据类型
   - 单位
   - 说明
2. `报文证据`
   - 原始字段名
   - 来源链路
   - 最近上报时间
   - 稳定度
3. `正式模型`
   - 当前是否已存在
   - 当前定义快照
4. `治理决策`
   - `纳入新增`
   - `纳入修订`
   - `继续观察`
   - `人工裁决`
   - `忽略`

### 9.5 默认建议动作

1. `double_aligned` -> `纳入新增`
2. `formal_exists` -> `忽略`
3. `manual_only` -> `继续观察`
4. `runtime_only` -> `继续观察`
5. `suspected_conflict` -> `人工裁决`
6. `evidence_insufficient` -> `继续观察`

### 9.6 正式确认区

正式确认区继续只承接：

1. `create`
2. `update`

以下状态不入库：

1. `继续观察`
2. `人工裁决`
3. `忽略`

## 10. 后端承载与数据边界

### 10.1 API 承载方式

继续复用：

1. `POST /api/device/product/{productId}/model-governance/compare`
2. `POST /api/device/product/{productId}/model-governance/apply`

不新增平行接口组。

### 10.2 compare 请求体增强方向

当前 `ProductModelGovernanceCompareDTO` 只有：

1. `manualExtract`
2. `manualDraftItems`
3. `includeRuntimeCandidates`

为承载首批规范治理，compare 请求体需要增加“规范驱动”入口。推荐最小增强如下：

1. `governanceMode`
   - `normative`
   - `generic`
2. `normativePresetCode`
   - 首批固定值建议：`landslide-integrated-tilt-accel-crack-v1`
3. `selectedNormativeIdentifiers`
   - 允许按模板勾选部分字段执行 compare
4. 保留现有 `manualExtract`
   - 仅作为辅助证据
5. 保留现有 `manualDraftItems`
   - 首批主要用于补充说明，不作为正式目标骨架主来源

### 10.3 证据快照增强方向

当前 `ProductModelGovernanceEvidenceVO` 需要最小补充首批规范治理所需的表达字段。推荐增加：

1. `evidenceOrigin`
   - `normative`
   - `sample_json`
   - `manual_draft`
   - `runtime`
   - `formal`
2. `unit`
3. `normativeSource`
4. `rawIdentifiers`
5. `monitorContentCode`
6. `monitorTypeCode`
7. `sensorCode`

### 10.4 数据边界

继续坚持：

1. 正式模型唯一落点是 `iot_product_model`
2. 不新增：
   - `iot_product_model_draft`
   - `iot_product_model_template`
   - `governance_session`
   - 其他中间态持久化表

如果需要携带规范出处、单位、监测内容编码、监测类型编码等信息，优先进入：

1. `specsJson`
2. `description`
3. compare 响应中的证据快照

而不是先扩正式表结构。

### 10.5 compare 侧实现重点

首批后端重点不在 CRUD，而在以下三件事：

1. 组装一体机规范字段清单
2. 将报文证据映射到规范化 `identifier`
3. 基于规范证据重算 `compareStatus / suggestedAction / riskFlags`

当前 `ProductModelGovernanceComparator` 的状态机骨架可以继续复用，不需要推翻。

## 11. 兼容与降级策略

1. 若当前产品暂时没有稳定报文证据：
   - compare 仍返回规范字段骨架
   - 多数行进入 `manual_only` 或 `evidence_insufficient`
   - 默认只能 `继续观察`
2. 若报文字段无法明确映射到规范字段：
   - compare 行进入 `runtime_only` 或 `suspected_conflict`
   - 保留原始字段名和来源表
3. 若正式模型中已存在历史字段名或漂移字段：
   - 不自动覆盖
   - 必须显式 `纳入修订` 或 `人工裁决`
4. 对未启用规范治理的产品，现有“通用双证据治理”路径继续保留，避免本轮改造破坏已有自由治理能力。

## 12. 文档回写要求

进入实现后，至少同步更新以下文档：

1. `docs/03-接口规范与接口清单.md`
   - compare / apply 新语义
2. `docs/04-数据库设计与初始化数据.md`
   - 规范字段如何进入 `iot_product_model`
3. `docs/08-变更记录与技术债清单.md`
   - 从“手动 + 自动”升级到“规范 + 报文”的治理口径
4. `docs/appendix/iot-field-governance-and-sop.md`
   - 首批一体机字段清单与 SOP

`README.md` 和 `AGENTS.md` 仅在项目级行为口径发生变化时再同步。

## 13. 首批验证要求

首批成功标准不以“字段越多越好”为准，而以“治理闭环成立”为准。

最少应验证：

1. 在 `/products` 中选定一体机产品后，compare 可以展示首批规范字段骨架。
2. 真实报文能够命中倾角、加速度、裂缝、设备状态中的至少一部分核心字段。
3. `double_aligned` 字段可以成功 apply 到 `iot_product_model`。
4. `runtime_only`、`manual_only`、`suspected_conflict` 不会默认错误写库。
5. apply 后的 `identifier` 使用规范化命名，而不是原始报文字段名。

## 14. 分批推进顺序

本轮固定采用按产品族逐步推进的方式：

1. 第一批：`倾角 / 加速度 / 裂缝一体机`
2. 第二批：`GNSS`
3. 第三批：`深部位移`
4. 后续再扩展到其他监测类型

每一批都遵循同一原则：

1. 先用规范证据定义目标字段骨架
2. 再用报文证据验证字段稳定性
3. 最后通过显式 compare / apply 进入正式模型

## 15. 设计结论

本轮不是要把产品物模型治理改造成一套新的行业模板平台，而是在现有 `/products` 双证据治理闭环内，把“规范证据”正式提升为产品契约的第一来源，把“报文证据”收口为验证与纠偏来源，并以“单产品、单批次、属性优先”的方式先跑通首批一体机正式模型治理。

一旦首批成立，后续 `GNSS`、`深部位移` 等产品族都应复用同一治理方法，而不是再各自发展新的治理入口。
