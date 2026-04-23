# 2026-04-23 L3/L4 自动归一（先治理侧后运行时）设计

## 1. 背景与目标

当前产品定义中心 `contracts compare/apply` 已具备按 `Lx_XX_n + 叶子字段` 的规范兜底能力，但覆盖类型仍偏少；运行时 `PAYLOAD_APPLY` 仍主要依赖发布快照和映射规则，对“新设备刚接入、规则尚未补齐”的场景不稳定。

本设计按同一阶段双波次推进：

1. Wave A（治理侧）：先保证 `compare/apply` 对 L3/L4 重点类型自动识别与归一稳定。
2. Wave B（运行时）：再把同一规则接入 `PAYLOAD_APPLY`，做到治理侧与运行时口径一致。

本期首批覆盖 5 类（来自附件规范）：

1. `L3_QW`：`value`
2. `L3_YL`：`value`、`totalValue`
3. `L3_DB`：`temp`、`value`
4. `L4_NW`：`value`
5. `L4_LD`：`X`、`Y`、`Z`、`speed`

## 2. 非目标

1. 不重构现有 Pipeline 阶段顺序。
2. 不新增平行草稿表或新治理主链路。
3. 不在本期扩展到所有监测类型；仅覆盖上述 5 类。
4. 不改变风险闭环边界：`riskEnabled=0` 继续只用于展示与治理，不入风险闭环。

## 3. 现状与问题

## 3.1 治理侧现状

`ProductModelServiceImpl.decorateCompareResultWithNormativeMetadata(...)` 已支持：

1. 场景命中（`listByScenario`）
2. 全局活动规范命中（`listActive`）
3. 原始标识兜底命中（`matchPropertyByRawIdentifier`）

但当前种子与测试对 L3/L4 的覆盖还不完整，尤其 `L3_DB`、`L4_LD` 未形成稳定回归。

## 3.2 运行时现状

`DevicePayloadApplyStageHandler.normalizeVendorMappedPayload(...)` 依赖 `VendorMetricMappingRuntimeService.resolveForRuntime(...)`。

`resolveForRuntime` 当前优先级为：

1. 已发布合同快照
2. 已发布映射规则快照
3. 草稿映射规则

若以上都未命中，则返回 `null`，最终保留原始标识写入 latest/下游，导致“compare 能识别，运行时不归一”的割裂。

## 4. 方案比较与选择

## 4.1 方案 A：治理侧 + 运行时均接入规范兜底（推荐）

优点：

1. 满足“先 1 后 2”目标。
2. compare 与 runtime 口径统一。
3. 新设备接入初期对人工映射规则依赖下降。

风险：

1. 运行时主链路改动需要严格回归。

## 4.2 方案 B：仅治理侧增强

优点：

1. 风险小，迭代快。

缺点：

1. 继续存在治理/运行时双口径。

## 4.3 方案 C：运行时继续只靠映射规则

优点：

1. 实现简单。

缺点：

1. 无法支撑“未建规则先可用”的扩展诉求。

结论：采用方案 A，并按 Wave A -> Wave B 分波实施。

## 5. 目标架构与优先级

统一归一优先级：

1. `Published Contract Snapshot`
2. `Published Mapping Snapshot`
3. `Draft Mapping Rule`
4. `Normative Prefix Fallback (Lx/Sx + XX + leaf)`
5. `Raw Identifier`（仅兜底保留）

说明：规范兜底永远不覆盖已发布快照和显式映射规则，只作为末端 fallback。

## 6. 详细设计

## 6.1 Wave A（治理侧）

### 6.1.1 数据种子

同步更新：

1. `sql/init-data.sql`
2. `scripts/run-real-env-schema-sync.py`

新增/补齐规范定义：

1. `phase3-weather`：`L3/QW/value`
2. `phase3-water-surface`：`L3/DB/temp`、`L3/DB/value`
3. `phase6-radar`：`L4/LD/X`、`Y`、`Z`、`speed`
4. 校验已有 `phase4-rain-gauge`（`L3/YL/value,totalValue`）
5. 校验已有 `phase5-mud-level`（`L4/NW`）

字段必须补齐：

1. `monitor_content_code`
2. `monitor_type_code`
3. `identifier`
4. `display_name`
5. `risk_enabled`

### 6.1.2 compare/apply 行为

`ProductModelNormativeMatcher` 保持现有结构，增强用例覆盖：

1. 对 `L4_NW_1`（标量）与 `L4_NW_1.value`（对象）均可识别。
2. 对 `L4_LD_1.{X|Y|Z|speed}` 可命中规范字段。
3. `L3_DB_1.temp/value` 与 `L3_QW_1.value` 可稳定命中。

`apply` 侧继续走 `normalizeApplyIdentifier`，同时加测试防止同语义双标识发布。

## 6.2 Wave B（运行时）

### 6.2.1 运行时兜底入口

改造 `VendorMetricMappingRuntimeServiceImpl.resolveForRuntime(...)`：

1. 保持现有前三层优先级不变。
2. 在 `resolveInternal(...)` miss 后新增 `resolveByNormativePrefixFallback(...)`。

### 6.2.2 fallback 逻辑

输入：

1. `product`
2. `rawIdentifier`
3. `logicalChannelCode`
4. `upMessage`（可选）

步骤：

1. 解析原始标识前缀 `Lx_XX_n` 或 `Sx_XX_n`。
2. 解析叶子字段（无叶子时按 `value` 尝试，仅对显式允许类型生效）。
3. 从 `listActive()` 中按 `monitorContentCode + monitorTypeCode + identifier` 匹配。
4. 命中后返回 `MappingResolution(ruleId=null, targetNormativeIdentifier=<canonical>, ...)`。
5. 未命中保持原行为（返回 `null`，最终保留 raw）。

### 6.2.3 PAYLOAD_APPLY 集成

`DevicePayloadApplyStageHandler` 不改主流程；复用 `resolveForRuntime` 新能力：

1. 既有快照/映射命中优先。
2. 命中 fallback 时自动归一写入 normalized properties。

## 7. 测试设计

## 7.1 单元测试

1. `ProductModelServiceImplTest`
   - 新增 5 类 compare 命中用例（标量与对象两类）。
2. `VendorMetricMappingRuntimeServiceImplTest`
   - 新增 5 类 `resolveForRuntime` fallback 命中用例。
   - 覆盖“无发布快照、无规则时命中 fallback”。
3. `DevicePayloadApplyStageHandlerTest`
   - 覆盖 `L3_DB`、`L4_LD` 在 `PAYLOAD_APPLY` 的归一写入。

## 7.2 回归命令

1. `mvn -pl spring-boot-iot-device -am test -Dtest=ProductModelServiceImplTest,VendorMetricMappingRuntimeServiceImplTest,DevicePayloadApplyStageHandlerTest -Dsurefire.failIfNoSpecifiedTests=false -DskipTests=false -Dmaven.test.skip=false`
2. `python3 -m unittest scripts.tests.test_run_real_env_schema_sync`

## 8. 风险与缓解

1. 风险：运行时归一改变 latest 标识。
   - 缓解：fallback 仅在快照/规则均未命中时生效。
2. 风险：历史标识并存导致“重复字段”。
   - 缓解：在 apply 与 compare 回归中增加“同语义单标识”断言。
3. 风险：规范种子与 sync 脚本不一致。
   - 缓解：SQL 与 Python 同步修改，并执行 schema sync 单测。

## 9. 验收标准

1. compare：5 类字段均可自动回填 `normativeIdentifier/normativeName/riskReady`。
2. apply：不出现同语义 path/bare 双标识同时发布。
3. runtime：无快照无规则时，`PAYLOAD_APPLY` 对 5 类可自动归一。
4. 测试：三组定向测试与 schema sync 测试全部通过。

## 10. 实施顺序

1. 补齐规范种子（SQL + sync）。
2. Wave A：治理侧用例先绿。
3. Wave B：运行时 fallback + handler 用例。
4. 文档同步：`docs/02`、`docs/08`、`docs/appendix`。
5. 执行定向回归并输出结果。
