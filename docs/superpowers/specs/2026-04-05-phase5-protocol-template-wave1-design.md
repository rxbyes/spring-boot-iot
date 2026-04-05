# Phase 5 总路线约束下的第一波协议模板化设计

**Date:** 2026-04-05  
**Status:** Approved in-session for spec drafting  
**Audience:** 设备中心 / 协议接入 / 产品治理 / 后端实现 / 文档维护  
**Scope:** 在不破坏当前模块化单体、固定 Pipeline 和真实环境验收口径的前提下，为 Phase 5 建立“可持续扩展的协议模板执行底座”，并以裂缝 `L1_LF_*` 与深部位移 `L1_SW_*` 两类父子拆分场景作为第一波最小可执行闭环。

## 1. 背景

`spring-boot-iot` 当前已经具备以下稳定基线：

1. 固定接入主链路仍为：
   - `INGRESS -> TOPIC_ROUTE -> PROTOCOL_DECODE -> DEVICE_CONTRACT -> MESSAGE_LOG -> PAYLOAD_APPLY -> TELEMETRY_PERSIST -> DEVICE_STATE -> RISK_DISPATCH -> COMPLETE`
2. 设备关系主数据底座已在上一波落地：
   - `iot_device_relation`
   - `/api/device/relations`
   - 运行期 `DeviceRelationService`
3. legacy `$dp` 路径已经支持：
   - 关系主数据优先
   - `iot.device.sub-device-mappings` fallback
   - 裂缝采集中枢 `L1_LF_n -> child.value`
   - 父状态 `S1_ZT_1.sensor_state.L1_LF_n -> child.sensor_state`
4. 产品物模型治理已经具备：
   - 规范证据
   - 运行期候选
   - 手动提炼
   - compare / apply
   - 基于关系主数据的候选过滤与 `relation_child` 归一

但这还不是 [Phase 5 IoT 架构演进设计](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/superpowers/specs/2026-04-04-phase5-iot-architecture-evolution-design.md) 的完整落地。

当前最核心的剩余问题有三类：

1. 父子拆分已经能工作，但仍主要写在 `LegacyDpChildMessageSplitter` 的场景逻辑里，尚未形成正式的“协议模板执行框架”。
2. 如果继续新增厂家或采集中枢家族，协议层会继续增长为“一个场景一段 if/regex”的累积结构，回归成本线性增加。
3. 如果第一波只图阶段交付，不对齐总路线，后续治理流水线、模板配置化和高并发演进会再次被旧代码结构卡住。

因此，这一波不只是“把裂缝和深位移重构一下”，而是要在 Phase 5 总路线约束下，先搭出一层可持续复用的协议模板底座。

## 2. 与 Phase 5 总路线的关系

本设计不替代总设计，而是将总设计拆成“总路线不变，第一波先落协议模板化最小闭环”的可执行版本。

### 2.1 Phase 5 总路线仍保持四条主线

后续整体演进仍按以下四条主线推进：

1. 设备关系主数据持续补强
2. 协议模板引擎化
3. 治理流水线化
4. 数据面高并发优化

### 2.2 第一波只落第二条主线的最小可执行子集

第一波只做：

1. 在 `spring-boot-iot-protocol` 中引入协议模板执行骨架
2. 以内置代码注册方式接入首批模板
3. 让裂缝和深位移父子拆分不再依赖单点场景特判实现

第一波明确不做：

1. 前端模板管理页
2. 模板元数据持久化
3. 全量 legacy `$dp` 家族统一模板化
4. latest/telemetry/risk 的高并发热路径重构
5. 候选持久化区或独立 candidate 表

### 2.3 第一波必须为后续两条能力留接口

第一波代码虽然聚焦协议层，但必须显式为后续能力留出扩展点：

1. 为“模板配置化/模板管理化”预留可替换的注册器与匹配器
2. 为“治理面消费模板元信息”预留模板标识、canonical 来源、父字段保留策略、状态镜像策略等结构化输出

第一波不能写成“把旧 if/else 拆成两个新类”的短期重排，而必须成为后续总路线中的正式底座。

## 3. 目标

本设计服务以下目标：

1. 在 `spring-boot-iot-protocol` 内建立协议模板执行框架的最小闭环。
2. 把裂缝 `L1_LF_*` 与深部位移 `L1_SW_*` 两类父子拆分逻辑收敛为可注册模板，而不是继续内嵌在 splitter 里。
3. 保持当前对外 Pipeline、运行行为和已有验收口径不变。
4. 保持 `spring-boot-iot-device` 继续只负责关系主数据，不侵入协议模板实现。
5. 为后续模板配置化、治理流水线消费模板元信息预留接口，不做反向重构。

## 4. 非目标

本轮明确不做以下事情：

1. 不引入新的服务拆分或新的启动模块。
2. 不把模板管理做成数据库表或完整工作台。
3. 不把全部 legacy `$dp` 家族一次性纳入模板框架。
4. 不把业务规则、正式物模型判定或持久化逻辑放回协议模块。
5. 不触发 latest 合并写、telemetry 批量写、状态去抖或风险异步分发改造。
6. 不改变 `iot.device.sub-device-mappings` 的兼容 fallback 入口。

## 5. 方案对比与选型

### 5.1 方案 A：代码注册型模板框架

做法：

1. 在 `spring-boot-iot-protocol` 内定义统一模板接口
2. 用 Java 类内置注册首批模板
3. splitter 按“关系规则 + 逻辑 payload 结构”匹配模板并执行

优点：

1. 改动聚焦，最适合当前仓库结构
2. 单测最好写，行为最容易锁住
3. 不需要在第一波引入半 DSL 配置系统
4. 后续可以自然升级为配置注册或主数据注册

缺点：

1. 第一波扩模板仍需发版
2. 运营侧无法直接管理模板

### 5.2 方案 B：配置驱动模板

做法：

1. 把模板规则先挂到 `IotProperties`
2. 协议层按配置匹配结构和字段映射

优点：

1. 表面上更灵活

缺点：

1. 第一波很容易把配置写成难维护的半 DSL
2. 调试、回归和结构演进成本更高

### 5.3 方案 C：通用规则引擎或表达式驱动模板

做法：

1. 直接定义完整模板 DSL
2. 通过表达式引擎匹配逻辑通道、结构和字段映射

优点：

1. 长期抽象能力最强

缺点：

1. 明显超出第一波合理范围
2. 极易把本次任务扩成基础设施项目

### 5.4 选型

本轮采用 `方案 A：代码注册型模板框架`。

原因：

1. 它最贴合当前仓库的模块边界和测试方式
2. 它能先把结构骨架搭对，再为后续配置化留接口
3. 它不会把本轮任务扩成一个不落地的“模板平台”

## 6. 第一波范围与验收边界

### 6.1 第一波只覆盖两类正式模板

第一波只纳入以下两类模板：

1. `crack_child_template`
   - 匹配：`L1_LF_* + timestamp -> scalar`
   - 输出：`child.value`
   - 可选镜像：`child.sensor_state`
2. `deep_displacement_child_template`
   - 匹配：`L1_SW_* + timestamp -> object`
   - 输出：`child.dispsX / child.dispsY / ...`

### 6.2 第一波必须保持的兼容行为

第一波完成后，下列行为不得退化：

1. 裂缝采集中枢 `SK00EA0D1307986` 仍能按关系规则或 fallback 产生正确的 `childMessages`
2. 裂缝子设备属性键仍为 `value`，并可镜像 `sensor_state`
3. 深部位移基准站仍能拆出 `dispsX / dispsY`
4. 对无父子映射、且 payload 家族只包含单个 `L1_SW_*` 的单设备深位移终端，仍保持“折叠回当前设备属性”的现有兼容行为
5. 运行期解析顺序仍为：
   - `iot_device_relation` 优先
   - `iot.device.sub-device-mappings` fallback

### 6.3 第一波不要求的能力

第一波不要求：

1. 所有模板元信息都被前端可视化消费
2. 所有 legacy `$dp` 家族都进入模板注册中心
3. 无映射设备自动推导关系候选
4. 新模板能通过控制台热更新

## 7. 模块边界

第一波必须严格遵循现有模块边界。

### 7.1 `spring-boot-iot-device`

继续负责：

1. 设备关系主数据
2. 运行期关系规则读取
3. 子产品治理消费关系语义

明确不负责：

1. 协议模板匹配
2. 协议模板执行
3. 报文结构识别

### 7.2 `spring-boot-iot-protocol`

继续负责：

1. 解码
2. 关系路由执行
3. 模板匹配
4. 模板执行
5. 父子拆分

明确不负责：

1. 正式物模型治理判定
2. 风险业务规则
3. 数据持久化

### 7.3 `spring-boot-iot-message` / `spring-boot-iot-telemetry`

本轮不改变职责。

## 8. 目标代码结构

第一波建议在 `spring-boot-iot-protocol` 内引入 4 个固定角色。

### 8.1 Template Registry

职责：

1. 暴露当前所有可用模板
2. 第一波用内置代码注册模板
3. 后续可替换为配置注册或主数据注册

第一波要求：

1. 必须有稳定模板标识
2. 必须允许后续按顺序注册多个模板
3. 不依赖 Spring Bean 扫描做复杂自动发现

### 8.2 Template Matcher

职责：

1. 根据 `relationRule + logicalPayload + logicalCode` 判断命中模板
2. 把“结构识别”和“模板执行”解耦

第一波匹配维度：

1. 逻辑通道家族
2. payload 结构
   - `timestamp -> scalar`
   - `timestamp -> object`
3. 当前关系策略

### 8.3 Template Executor

职责：

1. 执行字段 canonicalize
2. 生成子设备属性
3. 输出状态镜像和父字段清理建议

第一波执行结果至少应包含：

1. `templateCode`
2. `childProperties`
3. `parentRemovalKeys`
4. `statusMirrorApplied`
5. `canonicalizationStrategy`

后续可继续扩展：

1. `parentRetentionStrategy`
2. `rawIdentifiers`
3. `canonicalIdentifiers`
4. `governanceHints`

### 8.4 Splitter Orchestrator

继续由现有 splitter 负责总编排，但职责要收窄为：

1. 读取关系规则
2. 遍历逻辑通道
3. 调用模板匹配器和执行器
4. 组装 `childMessages`
5. 根据模板结果清理父属性
6. 对“未命中模板但需要兼容处理”的场景走 fallback 逻辑

它不再直接持有裂缝和深位移的具体字段映射实现。

## 9. 第一波模板定义

### 9.1 裂缝模板

模板标识：

1. `crack_child_template`

命中条件：

1. `logicalCode` 命中 `L1_LF_*`
2. 逻辑测点 payload 是 `timestamp -> scalar`

执行语义：

1. 标量值收口为 `value`
2. 若父属性存在 `S1_ZT_1.sensor_state.<logicalCode>`，则按 `statusMirrorStrategy=SENSOR_STATE` 输出 `sensor_state`
3. 父属性中的 `L1_LF_*` 主监测值应从父结果中剔除
4. 父属性中的 `S1_ZT_1.*` 与 `S1_ZT_1.sensor_state.L1_LF_*` 保留

### 9.2 深部位移模板

模板标识：

1. `deep_displacement_child_template`

命中条件：

1. `logicalCode` 命中 `L1_SW_*`
2. 逻辑测点 payload 是 `timestamp -> object`

执行语义：

1. 子设备属性保持对象内字段名，如 `dispsX / dispsY`
2. 第一波不强行新增子状态镜像
3. 父属性中的该逻辑点子字段应从父结果中剔除

### 9.3 单设备深位移折叠兼容

该能力本轮继续保留，但不强制纳入首批模板注册中心。

原因：

1. 它本质上不是“父设备按关系拆子设备”
2. 它是“无关系命中时的协议兼容折叠”

因此第一波建议保持为 splitter 内的兼容 fallback，后续若要统一模板化，可作为单独的 `standalone_collapse_template` 再纳入。

## 10. 数据流设计

第一波 legacy `$dp` 数据流建议表达为：

1. `MqttJsonProtocolAdapter` 解码并产出 `LegacyDpNormalizeResult`
2. `LegacyDpChildMessageSplitter` 读取父设备关系规则
3. 对每条逻辑通道：
   - 读取最新逻辑 payload
   - 构造模板匹配上下文
   - 由 matcher 决定命中模板
   - 由 executor 输出子属性与父字段处理结果
4. splitter 汇总：
   - 生成 `childMessages`
   - 清理父侧应剔除字段
   - 保留兼容 fallback 处理
5. 返回给后续 `PAYLOAD_APPLY`

第一波不改变后续落库、状态更新和风险分发顺序。

## 11. 为后续整体规划预留的结构化能力

为了不偏离总路线，第一波必须显式预留以下扩展点。

### 11.1 为模板配置化预留

预留能力：

1. 模板标识稳定
2. 注册器可替换
3. 匹配器可替换
4. 执行器结果对象稳定

后续如果要从代码注册切换到配置注册，只应替换 registry/matcher，而不重写 splitter 总流程。

### 11.2 为治理流水线消费预留

预留能力：

1. 模板执行结果可输出 canonical 来源
2. 模板可显式表达父字段保留/剔除策略
3. 模板标识可成为治理面附加证据

后续治理面需要知道某个字段来自哪种模板时，不应再反向解析 splitter 内部逻辑。

### 11.3 为高并发热路径预留

第一波不直接做性能优化，但要避免写出阻断后续优化的结构。

因此要求：

1. 模板执行应保持无状态或近似无状态
2. 不在模板执行阶段引入持久化或业务查询
3. 不在模板内部做跨模块副作用

## 12. 测试策略

第一波测试以“行为不退化 + 新结构可回归”为准。

### 12.1 协议模板单测

新增或改造单测覆盖：

1. 裂缝模板命中
2. 深位移模板命中
3. 未命中模板时的兼容路径
4. 关系主数据优先，配置 fallback 次之

### 12.2 splitter 回归

现有协议回归至少继续覆盖：

1. 裂缝父子拆分
2. 深位移父子拆分
3. 单设备深位移折叠兼容
4. 关系优先级不变

### 12.3 适配器回归

`MqttJsonProtocolAdapterTest` 必须继续证明：

1. 关系规则仍然能穿透到 splitter
2. child messages 行为不退化

## 13. 文档要求

本轮若落代码，必须同步更新以下文档：

1. `docs/05-protocol.md`
   - 补充“协议模板执行框架”与首批模板口径
2. `docs/08-变更记录与技术债清单.md`
   - 记录第一波模板化完成情况与后续未完成项
3. 若接口或配置契约发生变化，再评估是否同步 `docs/03` / `README.md` / `AGENTS.md`

当前第一波默认不要求更新 `README.md` 与 `AGENTS.md`，除非实现过程中出现用户可见行为或协作规则变化。

## 14. 风险与控制

### 14.1 风险

1. 若模板抽象过重，会把第一波做成基础设施工程，导致无法尽快落代码
2. 若模板抽象过轻，只是把旧逻辑拆成两个类，后续治理和配置化仍接不上
3. 若把兼容行为一起强行模板化，第一波范围会失控

### 14.2 控制策略

1. 第一波只做两个正式模板
2. 兼容折叠逻辑保留为 fallback，不强行统一
3. 只抽象“模板注册/匹配/执行/编排”四个角色
4. 通过现有协议回归测试锁死行为

## 15. 结论

Phase 5 的第一波协议模板化，不应被理解为“单纯重构 `LegacyDpChildMessageSplitter`”，而应被理解为：

`在总路线约束下，为关系驱动 IoT 平台补上第一层可持续扩展的协议模板执行底座。`

这层底座第一波只覆盖裂缝和深位移父子拆分，但它必须同时满足三件事：

1. 当前行为不退化
2. 当前模块边界不被破坏
3. 后续模板配置化、治理流水线化和高并发演进都能自然接上

只有这样，第一波代码才不是阶段性补丁，而是 Phase 5 总设计中的正式前进一小步。
