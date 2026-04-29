# 监测型产品风险指标目录补齐设计

> 历史说明：该设计文档记录的是 `2026-04-25` 的过渡方案。自 `2026-04-26` 起，风险指标目录的当前正式真相已切换为产品 `metadataJson.objectInsight.customMetrics[]` 中显式 `group=measure && enabled=true && includeInTrend=true` 的正式字段；“取消趋势展示”会同步把字段移出 `risk_metric_catalog`。下文保留为当日问题背景与过渡实现记录，不再代表当前发布真相。

**Date:** 2026-04-25  
**Status:** Historical draft, superseded on 2026-04-26 by explicit `设为监测数据` measure truth
**Audience:** 设备治理 / 风险对象中心 / 风险目录发布链路 / 测试验收 / 文档维护  
**Scope:** 记录修复监测型产品正式合同已经发布、但风险绑定正式目录为空的问题的过渡方案；当前正式范围与行为以后续 measure truth 方案为准。

## 1. 背景

风险点 `G6京藏高速K1623+400滑坡` 在新增正式绑定中选择设备 `CXH15522812 - 多维检测仪` 后，测点仍为空，并提示：

`当前设备所属产品暂无可用于风险绑定的正式目录字段。`

该设备所属产品为 `zhd-monitor-multi-displacement-v1`。共享开发环境中该产品已经存在正式 `property` 字段，例如：

1. `L1_LF_1.value`
2. `L1_QJ_1.angle / X / Y / Z / AZI`
3. `L1_JS_1.gX / gY / gZ`

风险绑定正式测点只允许消费 `risk_metric_catalog` 中已启用的正式目录字段。因此，页面没有测点并不是前端多选问题，而是产品合同发布到风险目录的桥层没有把该产品可风险闭环的正式字段发布出来。

## 2. 根因

当前目录发布链路为：

1. 产品合同发布产生 `ProductContractReleasedEvent`。
2. `ProductContractReleasedEventListener` 读取发布批次中的正式 `property` 合同。
3. 监听器写入 `iot_product_metric_resolver_snapshot`。
4. `RiskMetricCatalogPublishRule` 决定哪些正式字段允许进入风险目录。
5. `RiskMetricCatalogServiceImpl` 写入或退役 `risk_metric_catalog`。

已确认的实现偏差有三类：

1. `DefaultRiskMetricCatalogPublishRule` 当前把所有场景的可发布标识取并集，只要产品出现 `value` 就可能被发布，缺少场景或字段前缀边界。
2. `KeywordRiskMetricScenarioResolver` 主要识别裂缝与 GNSS，和设备侧规范匹配器支持的深部位移、雨量、泥位、水位、雷达等场景不完全一致。
3. 发布链路对全路径字段的风险语义识别不足，例如 `L1_LF_1.value` 应被识别为裂缝类 `value`，`L1_GP_1.gpsTotalX` 应被识别为 GNSS 类 `gpsTotalX`，但目录写入仍需要保留真实合同标识用于后续绑定。

## 3. 目标

本轮完成以下目标：

1. `zhd-monitor-multi-displacement-v1` 的 `L1_LF_1.value` 可以进入风险指标目录，设备 `CXH15522812` 在正式绑定中可选择该测点。
2. `nf-monitor-multi-displacement-v1` 的 `L1_LF_1.value` 可以进入风险指标目录。
3. `nf-monitor-gnss-monitor-v1` 的 `L1_GP_1.gpsTotalX / gpsTotalY / gpsTotalZ` 可以进入风险指标目录。
4. `nf-monitor-crack-meter-v1` 的裂缝字段可以稳定进入风险指标目录；若正式合同为 `L1_LF_1`，按裂缝 `value` 语义处理；若为 `L1_LF_1.value`，按同一裂缝 `value` 语义处理。
5. 风险目录发布规则从“全场景并集命中”收口为“字段级风险语义命中”，避免任意产品只要有 `value` 就误入风险闭环。
6. 已发布目录字段保持正式合同真实标识作为 `contractIdentifier`，同时用 `normativeIdentifier` 记录规范语义。

## 4. 非目标

本轮明确不做：

1. 不把 `zhd-monitor-tiltmeter-v1` 倾角仪纳入风险目录；倾角、加速度字段仍先保留对象洞察和治理语义，不进入风险闭环。
2. 不把 `nf-monitor-mud-level-meter-v1` 泥位计纳入风险目录；当前规范种子 `phase5-mud-level` 的风险闭环开关仍为关闭。
3. 不把 GNSS 基准站产品纳入风险目录；基准站当前没有正式风险测点字段，且业务定位不是风险点绑定主测点。
4. 不直接从 `iot_product_model` 绕过 `risk_metric_catalog` 给风险绑定页面兜底。
5. 不新增表结构，不调整 schema registry。
6. 不改变正式绑定多选交互；上一阶段已经支持多测点选择，本轮只保证候选目录正确出现。

## 5. 目标产品与字段

| 产品编号 | 本轮处理 | 可进入风险目录的正式字段 | 规范语义 | 说明 |
|---|---|---|---|---|
| `zhd-monitor-multi-displacement-v1` | 纳入 | `L1_LF_1.value` | `phase1-crack / value` | 多维设备中仅裂缝量进入风险闭环，倾角和加速度不进入 |
| `nf-monitor-multi-displacement-v1` | 纳入 | `L1_LF_1.value` | `phase1-crack / value` | 与中海达多维设备保持一致 |
| `nf-monitor-gnss-monitor-v1` | 纳入 | `L1_GP_1.gpsTotalX / gpsTotalY / gpsTotalZ` | `phase2-gnss / gpsTotalX/Y/Z` | `gpsInitial` 继续不进入风险闭环 |
| `nf-monitor-crack-meter-v1` | 纳入 | `L1_LF_1` 或 `L1_LF_1.value` | `phase1-crack / value` | 兼容历史正式字段形态 |
| `zhd-monitor-tiltmeter-v1` | 排除 | 无 | 无 | 缺少已确认风险闭环语义 |
| `nf-monitor-mud-level-meter-v1` | 排除 | 无 | 无 | 规范库 `risk_enabled=0` |
| GNSS 基准站产品 | 排除 | 无 | 无 | 当前不作为风险点监测测点 |

## 6. 方案比较

### 6.1 方案 A：按产品编号硬编码白名单

做法：

1. 在 `DefaultRiskMetricCatalogPublishRule` 中写死产品编号到字段列表。
2. 对本轮目标产品直接返回指定字段。

优点：

1. 实现最快。
2. 对当前设备问题能立即见效。

缺点：

1. 与平台“规范字段库 + 发布快照 + 风险目录”的长期路线冲突。
2. 新产品仍要继续改代码。
3. 多传感产品里的字段语义无法通用复用。

### 6.2 方案 B：字段级规范语义解析，推荐

做法：

1. 将风险目录发布规则从产品级猜测调整为字段级解析。
2. 优先识别正式全路径字段的 `Lx_XX_n` 前缀和叶子字段。
3. 命中规范库中 `risk_enabled=1` 的定义时，才允许发布风险目录。
4. 目录行保留真实 `contractIdentifier`，同时写入规范 `normativeIdentifier / sourceScenarioCode`。
5. 对短标识 `value / gpsTotalX / gpsTotalY / gpsTotalZ / dispsX / dispsY` 继续保留既有兼容。

优点：

1. 能解决多维设备这种“同一产品下有裂缝、倾角、加速度多类字段”的真实场景。
2. 避免所有 `value` 字段误入风险目录。
3. 复用现有规范字段库，不新增平行规则表。
4. 与设备侧 `Lx_XX_n + leaf` 兜底治理方向一致。

缺点：

1. 需要对发布规则、目录服务语义写入和测试做一次成体系收口。
2. 需要谨慎处理历史 `contractIdentifier=value` 与新全路径 `contractIdentifier=L1_LF_1.value` 的兼容。

### 6.3 方案 C：风险绑定接口直接读取正式合同并前端过滤

做法：

1. `formal-metrics` 在目录为空时读取 `iot_product_model`。
2. 前端或后端按字段名兜底筛选候选。

优点：

1. 页面很快能看到候选。

缺点：

1. 绕过 `risk_metric_catalog` 真相源。
2. 写侧仍会拒绝非目录字段，读写口径继续分裂。
3. 破坏已有“正式合同 -> 风险指标目录 -> 风险绑定”的桥层边界。

### 6.4 选型

采用 **方案 B：字段级规范语义解析**。

原因：

1. 它能直接解释并修复 `CXH15522812` 无测点的根因。
2. 它不会把倾角、加速度、泥位等尚未确认风险闭环语义的字段误放进绑定入口。
3. 它延续现有架构，不新增绕行链路。

## 7. 目标架构

本轮目标链路固定为：

`正式合同字段 -> 字段级风险语义解析 -> risk_metric_catalog -> formal-metrics -> 风险点正式绑定`

字段级风险语义解析规则：

1. 若字段是短标识，按既有规范标识识别，例如 `value`、`gpsTotalX`。
2. 若字段是全路径标识，解析逻辑通道与叶子字段，例如 `L1_LF_1.value`、`L1_GP_1.gpsTotalX`。
3. `L1_LF_*` 且叶子为空或叶子为 `value` 时，映射到 `phase1-crack / value`。
4. `L1_GP_*` 且叶子为 `gpsTotalX / gpsTotalY / gpsTotalZ` 时，映射到 `phase2-gnss / gpsTotalX/Y/Z`。
5. 既有 `dispsX / dispsY`、雨量 `value` 等已有场景继续保留，但必须经过字段语义或规范开关确认。
6. 任意字段只有在规范定义 `riskEnabled=1` 时才允许发布；没有定义或定义为关闭时不发布。

目录写入规则：

1. `contractIdentifier` 保存正式合同的真实标识，例如 `L1_LF_1.value`。
2. `normativeIdentifier` 保存规范语义，例如 `value`。
3. `sourceScenarioCode` 保存来源场景，例如 `phase1-crack`。
4. `riskMetricCode` 基于真实合同标识生成时需做安全字符归一，避免点号等字符影响编码稳定性。
5. 已存在旧目录行时，以真实 `contractIdentifier` 作为同一产品下的主匹配键；历史短标识行不强行删除，发布批次退役逻辑按当前发布批次的正式字段集合处理。

## 8. 组件边界

### 8.1 Alarm 模块

`spring-boot-iot-alarm` 负责风险目录发布与风险绑定消费：

1. `DefaultRiskMetricCatalogPublishRule`：决定哪些正式合同字段可发布。
2. `RiskMetricCatalogServiceImpl`：写入 `risk_metric_catalog`，补齐规范语义元数据。
3. `ProductContractReleasedEventListener`：在合同发布后写 resolver snapshot，并触发目录发布。
4. `KeywordRiskMetricScenarioResolver`：继续作为产品级场景兜底，但不再承担多传感产品的唯一判断依据。

### 8.2 Device 模块

`spring-boot-iot-device` 继续负责产品合同、规范库、发布快照与运行态字段归一：

1. 本轮不改变设备主链路。
2. 若实现过程中需要复用设备侧规范匹配语义，只做内部 helper 或相邻单测补齐，不把风险目录写逻辑下沉到设备域。
3. 当前工作区已有设备侧规范匹配器未提交改动，实现阶段必须先识别并保护这些改动，不得回退。

### 8.3 脚本与种子

本轮不改表结构。若真实共享环境已有正式合同但缺目录数据，需要通过现有真实环境同步脚本或产品发布事件链补齐：

1. `scripts/run-real-env-schema-sync.py` 可补齐规范种子、权限和历史共享环境缺口。
2. 若需要补目标产品的目录数据，应走“读取正式合同 -> 字段级解析 -> upsert `risk_metric_catalog`”的同一业务口径，不能写一份与 Java 规则分叉的 SQL 白名单。
3. `sql/init-data.sql` 只在需要补充演示基线时同步更新；不为本轮新增 schema 对象。

## 9. 数据流

### 9.1 新发布合同

1. 用户在 `/products/:productId/contracts` 发布正式合同。
2. 设备域落库 `iot_product_model` 与 `iot_product_contract_release_batch`。
3. 设备域发布 `ProductContractReleasedEvent`。
4. Alarm 监听器读取本批 `property` 合同。
5. 监听器写入 `iot_product_metric_resolver_snapshot`。
6. 发布规则逐字段解析风险语义，并返回可发布的真实合同标识。
7. 目录服务按真实合同标识写入 `risk_metric_catalog`，并记录规范语义。
8. 风险绑定 `formal-metrics` 返回目录字段。

### 9.2 历史共享环境补齐

1. 读取目标产品当前已发布正式 `property` 合同。
2. 使用同一字段级解析规则筛选风险字段。
3. 对缺失的目录行执行 upsert。
4. 对当前产品已不在最新正式合同中的旧目录行执行退役。
5. 用 `CXH15522812` 验证正式绑定候选出现 `L1_LF_1.value`。

## 10. 异常与兼容

### 10.1 全路径字段与短标识

若正式合同字段为 `L1_LF_1.value`：

1. 风险绑定展示真实合同标识和正式字段名。
2. 写侧绑定保存真实 `metricIdentifier=L1_LF_1.value`。
3. 目录语义保存 `normativeIdentifier=value`。

若正式合同字段为 `value`：

1. 保持历史行为，目录 `contractIdentifier=value`。
2. 语义仍为 `phase1-crack / value`。

### 10.2 无规范定义

若字段能从前缀猜出可能语义，但规范库没有对应定义：

1. 不发布目录。
2. 不让前端正式绑定绕过目录兜底。
3. 后续通过规范库补种子或产品合同重发解决。

### 10.3 多候选或歧义

若同一字段命中多个规范候选：

1. 不发布目录。
2. 在测试和日志中保留可定位原因。
3. 不自动选择第一个候选。

### 10.4 已有历史绑定

本轮不自动迁移历史绑定。若旧绑定使用短标识、新目录使用全路径标识：

1. 新增正式绑定走新目录。
2. 历史绑定兼容由现有绑定读侧与 resolver snapshot 继续承担。
3. 不在本轮批量改写 `risk_point_device`。

## 11. 测试策略

本轮采用 TDD：

1. 先为 `DefaultRiskMetricCatalogPublishRuleTest` 增加失败用例，覆盖多维产品只发布 `L1_LF_1.value`，不发布倾角/加速度字段。
2. 为 GNSS 全路径字段增加失败用例，确认只发布 `L1_GP_1.gpsTotalX/Y/Z`，不发布 `gpsInitial`。
3. 为任意非风险产品的 `value` 增加失败用例，确认不会因全场景并集误发布。
4. 为 `RiskMetricCatalogServiceImplTest` 增加失败用例，确认目录行保留真实 `contractIdentifier=L1_LF_1.value`，同时写入 `normativeIdentifier=value` 与 `sourceScenarioCode=phase1-crack`。
5. 为 `ProductContractReleasedEventListenerTest` 增加发布事件回归，确认正式全路径字段发布后可生成目录。
6. 若脚本补齐真实环境目录，增加脚本单测或最小 dry-run 断言，避免 Java 与 Python 规则分叉。

最小验证命令：

1. `mvn -pl spring-boot-iot-alarm -DskipTests=false -Dtest=DefaultRiskMetricCatalogPublishRuleTest,RiskMetricCatalogServiceImplTest,ProductContractReleasedEventListenerTest test`
2. 如触碰设备侧未提交改动，再执行对应设备域定向测试。
3. 如触碰脚本，再执行脚本对应 unittest。

## 12. 文档更新

实现落地时至少检查并更新：

1. `README.md`
2. `AGENTS.md`
3. `docs/02-业务功能与流程说明.md`
4. `docs/03-接口规范与接口清单.md`
5. `docs/04-数据库设计与初始化数据.md`
6. `docs/08-变更记录与技术债清单.md`
7. `docs/21-业务功能清单与验收标准.md`

文档需明确：

1. 多维设备当前仅裂缝量进入风险绑定正式目录。
2. GNSS 位移设备仅累计位移 X/Y/Z 进入风险绑定正式目录。
3. 倾角、加速度、泥位和 GNSS 基准站不属于本轮风险闭环范围。
4. 风险绑定候选继续以 `risk_metric_catalog` 为唯一读侧真相。

## 13. 验收标准

1. `CXH15522812` 所属产品目录补齐后，风险点新增正式绑定中测点可选择 `L1_LF_1.value` 对应正式字段。
2. 测点选择支持多选能力保持不回退。
3. `zhd-monitor-multi-displacement-v1` 与 `nf-monitor-multi-displacement-v1` 不会把 `L1_QJ_*`、`L1_JS_*` 发布到风险目录。
4. `nf-monitor-gnss-monitor-v1` 只发布 `L1_GP_1.gpsTotalX/Y/Z`。
5. `zhd-monitor-tiltmeter-v1`、`nf-monitor-mud-level-meter-v1` 和 GNSS 基准站产品仍无风险绑定正式测点。
6. 定向后端测试通过。
7. 如执行真实环境补齐，必须使用 `application-dev.yml` 所指真实共享环境，不得回退到 H2。

## 14. 风险与回归关注点

1. 如果目录服务继续把全路径字段归一成短标识作为 `contractIdentifier`，风险绑定会再次找不到真实正式字段。
2. 如果发布规则继续按场景并集匹配，倾角仪或其它产品的 `value` 字段可能被误发布。
3. 如果 Python 补齐脚本和 Java 发布规则各写一套白名单，后续真实环境和新发布链路会再次分叉。
4. 如果规范库缺少 `L1_LF / L1_GP` 对应定义，目录补齐应失败并暴露治理缺口，而不是绕过规范库直接发布。
