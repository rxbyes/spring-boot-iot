# Phase 5 协议模板 Wave 2 治理接入设计

**Date:** 2026-04-05  
**Status:** Approved in-session for spec drafting  
**Audience:** 设备中心 / 协议接入 / 产品治理 / 前端实现 / 文档维护  
**Scope:** 在保持当前 `/products` 物模型治理抽屉、固定 Pipeline、模块化单体和真实环境验收口径不破坏的前提下，把 Wave 1 已落地的协议模板执行证据正式接入产品治理 compare / apply 链路。

## 1. 背景

[Phase 5 总路线约束下的第一波协议模板化设计](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/superpowers/specs/2026-04-05-phase5-protocol-template-wave1-design.md) 已经完成了最小协议模板闭环：

1. `spring-boot-iot-protocol` 已具备代码注册型模板底座。
2. 裂缝 `crack_child_template` 与深部位移 `deep_displacement_child_template` 已作为首批正式模板落地。
3. `LegacyDpChildMessageSplitter` 已收敛为“关系解析 + 模板匹配/执行 + fallback 编排”。
4. `DeviceUpProtocolMetadata.templateEvidence` 已能承载模板执行证据。
5. `message-flow` 和 `/message-trace` 已能展示模板证据。

但这还没有完成 Phase 5 总路线里“协议模板引擎化 -> 治理流水线化”的衔接。

当前仍存在的缺口是：

1. `/products` 物模型治理 compare/apply 仍主要消费 `iot_device_property`、`iot_device_message_log` 和手动证据。
2. 协议层已经知道某个字段来自哪个模板、是否是 canonical 输出、是否存在状态镜像，但治理面还无法直接消费这些解释性语义。
3. 用户在产品治理抽屉里虽然能看到“报文证据”，但还看不到“这个候选字段究竟是厂家原始叶子字段，还是协议模板执行后的子设备契约字段”。

因此 Wave 2 的任务不是再做一个独立模板页面，而是把 Wave 1 预留的模板证据接到现有治理抽屉里，让协议证据真正成为 compare / apply 的一部分。

## 2. 与 Phase 5 总路线的关系

### 2.1 本轮承接两条主线

本轮主要承接 Phase 5 总路线中的两条主线：

1. 协议模板引擎化
2. 治理流水线化

### 2.2 本轮只做治理接入，不扩散范围

本轮只做：

1. 让 `/api/device/product/{productId}/model-governance/compare` 能消费 runtime 侧的协议模板证据。
2. 让 `/products` 现有治理抽屉在“报文证据”卡片中解释模板来源、canonical 策略和父子拆分语义。
3. 让 `/api/device/product/{productId}/model-governance/apply` 返回最小必要的模板来源回执，供前端在现有抽屉中回显。

本轮明确不做：

1. 独立模板管理页
2. 模板元数据持久化
3. 新的治理入口或新的 compare 工作台
4. 新的正式模型表或平行草稿表
5. 全量 legacy `$dp` 家族继续模板化

## 3. 目标

本设计服务以下目标：

1. 让产品治理 compare 能理解 runtime 候选字段是否来自协议模板执行。
2. 让治理面能直接解释模板标识、canonicalization、状态镜像与父字段剔除口径，而不再依赖人工反推。
3. 保持 `/products` 当前抽屉和 compare/apply 主流程不变，只增强 runtime 证据解释力。
4. 让 apply 阶段能够把“本次采用了哪些模板来源”回显给用户，但不把模板元数据写进正式模型表。
5. 为后续更深的模板配置化和治理裁决规则预留接口，不在本轮引入第四条 compare 轴或全新状态机。

## 4. 非目标

本轮明确不做以下事情：

1. 不改变 `iot_product_model` 表结构。
2. 不把模板证据单独建表持久化。
3. 不改变 `ProductModelGovernanceComparator` 当前三侧 compare 基本模型：
   - 手动证据
   - 报文证据
   - 正式模型
4. 不新增模板主导的 compareStatus。
5. 不把 apply 变成自动裁决。
6. 不把 `/message-trace` 的协议模板证据页面迁移或复用成新的产品治理页面。

## 5. 方案对比与选型

### 5.1 方案 A：只在前端补充模板文案

做法：

1. 不改后端候选结构。
2. 前端仅在 compare 区增加静态说明文案或弱提示。

优点：

1. 改动小。

缺点：

1. 治理面拿不到真正的结构化模板证据。
2. 无法回答某个 runtime 候选具体来自哪种模板执行。

### 5.2 方案 B：增强现有治理 compare/apply，推荐

做法：

1. 后端在现有 runtime candidate 构建过程中聚合协议模板证据。
2. compare 响应中的 runtime evidence 增加模板证据快照。
3. apply 响应增加最小回执列表，供当前抽屉回显。

优点：

1. 贴合现有 `/products` 治理抽屉，不新增入口。
2. 与 Wave 1 的“为治理消费预留结构化模板证据”直接对接。
3. 用户可感知，但范围可控。

缺点：

1. 需要在 device 模块增加一层 runtime 协议证据聚合。

### 5.3 方案 C：把模板证据升级为 compare 第四轴

做法：

1. compare 结果从三侧升级为四侧。
2. 新增模板主导的 compareStatus 和裁决规则。

优点：

1. 长期抽象最完整。

缺点：

1. 明显超出本轮范围。
2. 会打散当前已稳定的 compare/apply 交互。

### 5.4 选型

本轮采用 `方案 B：增强现有治理 compare/apply`。

原因：

1. 它最符合“Wave 1 搭底座，Wave 2 接治理面”的推进节奏。
2. 它能让协议模板证据进入正式治理闭环，而不是继续停留在诊断页。
3. 它不要求重做页面结构或 compare 状态模型。

## 6. 当前代码基线与约束

### 6.1 当前治理链路

当前产品治理链路已经具备：

1. `ProductModelServiceImpl.compareGovernance(...)`
2. `ProductModelGovernanceComparator`
3. `/api/device/product/{productId}/model-governance/compare`
4. `/api/device/product/{productId}/model-governance/apply`
5. `/products` 中现有 `ProductModelDesignerDrawer` + `ProductModelGovernanceCompareTable`

当前 compare 结果固定围绕以下三侧展开：

1. `manualCandidate`
2. `runtimeCandidate`
3. `formalModel`

### 6.2 当前 runtime 候选来源

当前 runtime property candidate 主要来自：

1. `iot_device_property`
2. `iot_device_message_log`

并且：

1. `ProductModelServiceImpl.collectPropertyCandidates(...)` 已会结合 `DeviceRelationService` 过滤掉关系语义下由子设备拥有的父级字段。
2. runtime evidence 当前已经能返回：
   - `rawIdentifiers`
   - `sourceTables`
   - `evidenceCount`
   - `messageEvidenceCount`
   - `needsReview`

### 6.3 当前缺口

当前 runtime evidence 还不能表达：

1. 该候选是否来自协议模板执行结果
2. 来自哪个模板
3. 是否经过 canonicalization
4. 是否包含状态镜像
5. 模板建议剔除了哪些父字段

## 7. 总体设计

### 7.1 保持 compare 三侧不变

本轮不改变 compare 三侧结构：

1. 手动证据仍代表规范证据和人工补录证据。
2. 报文证据仍代表 runtime 侧自动提炼。
3. 正式模型仍代表当前 `iot_product_model` 基线。

模板证据只作为 `runtimeCandidate` 的增强信息进入 compare，不单独新增第四列。

### 7.2 引入“治理侧模板证据快照”

建议在 `spring-boot-iot-device` 中新增一组治理侧 VO/聚合对象，用来表达 runtime candidate 可消费的模板证据摘要。

建议新增嵌套结构：

1. `ProductModelProtocolTemplateEvidenceVO`
   - `templateCodes`
   - `logicalChannelCodes`
   - `childDeviceCodes`
   - `canonicalizationStrategies`
   - `statusMirrorApplied`
   - `parentRemovalKeys`
   - `templateExecutionCount`

接入方式：

1. `ProductModelCandidateVO` 增加可选字段 `protocolTemplateEvidence`
2. `ProductModelGovernanceEvidenceVO` 同步增加可选字段 `protocolTemplateEvidence`

这样 compare 仍是三侧结构，但 runtime evidence 能直接带出模板快照。

### 7.3 运行期模板证据聚合服务

建议在 `spring-boot-iot-device` 中增加一层专用聚合服务或 helper，职责是：

1. 基于产品下设备和近期 `iot_device_message_log` 读取最近真实报文。
2. 复用 `ProtocolAdapterRegistry` 和现有解码能力，对 runtime evidence 所需的报文执行轻量重解码。
3. 从 `DeviceUpProtocolMetadata.templateEvidence` 中提取模板执行快照。
4. 把模板执行快照按“当前产品、当前设备、当前字段标识”聚合成治理可消费的结构。

这层能力只服务产品治理，不进入接入主链路，不改线上写路径。

### 7.4 聚合原则

本轮聚合原则如下：

1. 只聚合当前 compare 窗口内已有 runtime evidence 命中的字段，不额外生成新的候选类型。
2. 只聚合当前产品相关设备的最近真实消息日志，不扩大到全库回扫。
3. 模板证据以“解释字段来源”为目的，不直接改变 compareStatus。
4. 同一 identifier 若命中多个模板执行，采用聚合展示：
   - 模板编码去重
   - 子设备编码去重
   - canonicalization 策略去重
   - `parentRemovalKeys` 聚合去重

### 7.5 字段关联规则

本轮建议采用以下字段关联口径：

1. 若 runtime 候选来自子设备已落库属性，如裂缝子设备 `value`、深位移子设备 `dispsX / dispsY`，则按该条消息解码后的 `protocolMetadata.templateEvidence` 直接挂到当前 identifier。
2. 若 runtime 候选是状态镜像字段，如 `sensor_state`，则允许挂接同一模板证据，但需显式标记 `statusMirrorApplied=true`，避免误判为主监测值。
3. 若字段没有命中模板执行证据，则保持当前 runtime evidence 口径不变，不强行补空模板结构。

### 7.6 对现有关系语义的约束

本轮不允许模板证据反向改写当前关系主数据。

也就是说：

1. 关系过滤仍以 `DeviceRelationService` 和现有 fallback 规则为准。
2. 模板证据只增强 runtime candidate 的解释力。
3. 模板证据不能在本轮直接产出新的关系候选或新的 child 绑定建议。

## 8. 后端设计

### 8.1 compare 返回体增强

`/api/device/product/{productId}/model-governance/compare` 的请求体保持不变。

响应体增强点：

1. `ProductModelGovernanceEvidenceVO.runtimeCandidate.protocolTemplateEvidence`
2. `ProductModelGovernanceEvidenceVO.manualCandidate.protocolTemplateEvidence`
   - 本轮默认保持为空
3. `ProductModelGovernanceEvidenceVO.formalModel.protocolTemplateEvidence`
   - 本轮默认保持为空

这样前端无需新增 compare 轴，只需读取 runtime evidence 的增强字段。

### 8.2 apply 返回体增强

建议扩展 `ProductModelGovernanceApplyResultVO`，新增最小回执列表，例如：

1. `appliedItems`
   - `modelType`
   - `identifier`
   - `decision`
   - `templateCodes`
   - `canonicalizationStrategies`
   - `childDeviceCodes`

作用：

1. 让前端在当前抽屉内回显“本次采用了哪些模板来源”。
2. 作为用户确认后的治理留痕摘要。

本轮仍不把这些信息入库到正式模型表。

### 8.3 代码落点建议

建议后端主要落在以下位置：

1. `ProductModelServiceImpl`
   - 继续负责 compare/apply 编排
2. 新增 runtime 协议模板证据聚合 helper/service
   - 负责日志重解码和模板证据聚合
3. `ProductModelCandidateVO`
4. `ProductModelGovernanceEvidenceVO`
5. `ProductModelGovernanceApplyResultVO`

`ProductModelGovernanceComparator` 保持职责边界：

1. 仍负责 compare 行合并
2. 不负责报文重解码

### 8.4 运行期重解码约束

为了控制成本，本轮重解码必须满足：

1. 只用于 compare 请求内的治理读路径。
2. 不引入新的写副作用。
3. 不访问跨模块业务写接口。
4. 若某条日志重解码失败，允许跳过该条模板证据，但不能让整个 compare 失败。
5. compare 结果要能区分“无模板证据”和“runtime 证据存在但模板证据读取失败/未命中”。

## 9. 前端设计

### 9.1 不新增页面

本轮前端继续只修改现有：

1. `ProductModelDesignerDrawer.vue`
2. `ProductModelGovernanceCompareTable.vue`
3. 对应 `types/api.ts`
4. 对应组件测试

不新增独立模板工作台，不改变 `/products` 的路由结构。

### 9.2 compare 展示增强

`ProductModelGovernanceCompareTable` 中“报文证据”卡片增加模板证据说明区。

建议展示内容：

1. 模板编码
2. 子设备样本
3. canonical 策略
4. 是否存在状态镜像
5. 父字段剔除摘要

展示原则：

1. 没有模板证据时不显示空白说明区。
2. 模板证据显示为解释性信息，不挤占当前标题、状态、建议动作层级。
3. 继续复用当前三列证据卡布局，不新增页签、不新增说明墙。

### 9.3 apply 回执回显

`ProductModelDesignerDrawer` 在 apply 成功后，除现有成功提示外，增加当前会话级最小回执展示。

建议展示内容：

1. 本次 create / update / skip 数量
2. 本次被纳入的 identifier
3. 若存在模板来源，则展示模板编码和子设备样本摘要

该回执只保留在当前抽屉会话内，不新增历史台账页。

## 10. API 与数据契约

### 10.1 compare 请求

本轮不改 compare 请求体：

1. `governanceMode`
2. `normativePresetCode`
3. `selectedNormativeIdentifiers`
4. `manualExtract`
5. `manualDraftItems`
6. `includeRuntimeCandidates`

### 10.2 compare 响应

新增 runtime evidence 可选字段：

1. `protocolTemplateEvidence`

结构要求：

1. 必须可空
2. 不得破坏现有字段命名
3. 同一结构同时用于后端 VO 和前端类型定义

### 10.3 apply 响应

在现有统计字段基础上新增可选回执列表：

1. `appliedItems`

本轮不改 apply 请求体。

## 11. 测试策略

### 11.1 后端单测

至少新增或改造以下测试：

1. runtime 模板证据聚合：
   - 裂缝 `value`
   - 裂缝 `sensor_state`
   - 深位移 `dispsX / dispsY`
2. compare 回应中 runtime evidence 挂接模板证据成功。
3. 模板证据缺失或单条日志重解码失败时 compare 不整体失败。
4. apply 回执能正确带出模板来源摘要。

### 11.2 前端测试

至少新增或改造以下测试：

1. `ProductModelGovernanceCompareTable` 能在 runtime card 中显示模板证据摘要。
2. 无模板证据时 compare 表仍保持当前展示。
3. `ProductModelDesignerDrawer` 能在 apply 后显示最小回执。

### 11.3 回归要求

本轮必须保证以下行为不退化：

1. 规范证据模式与通用双证据模式切换逻辑不变。
2. compareStatus 计算逻辑不变。
3. apply 仍只写正式模型，不写平行草稿。

## 12. 文档要求

本轮若落代码，必须同步更新以下文档：

1. `docs/02-业务功能与流程说明.md`
   - 补充 `/products` 中 runtime 模板证据已进入 compare/apply 解释链路
2. `docs/03-接口规范与接口清单.md`
   - 补充 compare/apply 响应体新增字段
3. `docs/05-protocol.md`
   - 补充 Wave 2：协议模板证据已进入产品治理消费
4. `docs/08-变更记录与技术债清单.md`
   - 记录 Wave 2 完成情况与仍未做项
5. `docs/15-前端优化与治理计划.md`
   - 如 compare 卡片结构发生可见变化，补充前端治理约束

当前默认不要求更新 `README.md` 与 `AGENTS.md`，除非实现过程中出现新的用户可见入口或协作规则变化。

## 13. 风险与控制

### 13.1 风险

1. 若把模板证据直接做成 compare 第四轴，会把当前治理抽屉复杂度拉高。
2. 若 runtime 重解码范围不加控制，compare 请求可能明显变慢。
3. 若模板证据直接参与 compareStatus 判定，可能打破当前已稳定的治理习惯。

### 13.2 控制策略

1. 本轮只增强 runtime evidence，不新增 compare 轴。
2. 模板证据默认只作解释，不主导 compareStatus。
3. 重解码失败按单条降级处理，不影响整体 compare。
4. apply 回执只做当前会话回显，不扩成历史治理台账。

## 14. 结论

Wave 2 的目标不是“把协议模板又展示一次”，而是：

`让协议模板执行证据正式成为 /products 物模型治理 compare / apply 的一部分。`

它应当做到三件事：

1. 当前治理抽屉不变形
2. 当前 compare/apply 主流程不退化
3. 运行期字段来源能够被解释为“来自哪种模板、经过何种 canonicalization、是否伴随状态镜像”

只有这样，Wave 1 的协议模板底座才真正接到了 Phase 5 的治理主线上。
