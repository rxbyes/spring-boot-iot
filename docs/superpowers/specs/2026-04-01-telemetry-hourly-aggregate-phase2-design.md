# 遥测容量治理 Phase 2 小时聚合设计

## 背景

当前 telemetry v2 已具备：

1. TDengine raw 主写链路；
2. MySQL latest 投影；
3. `/api/telemetry/latest` 的 v2 读路由；
4. aggregate / cold archive 的写入扇出扩展缝。

但面向 `10 万台设备 + 1 分钟一条 + 10~20 指标 + 保留 3 年` 的容量目标，仅靠 raw 明细承接长时间窗趋势和报表，会导致查询窗口、存储成本和后续报表语义都不可持续。本阶段只补齐“小时聚合写入基线”，不同时扩展历史查询接口、报表读路由或冷归档实现。

## 目标

- 为 telemetry v2 增加可落库的小时聚合写入链路。
- 只覆盖 `MEASURE` 数值类指标，不把 `STATUS / EVENT` 混入小时聚合。
- 聚合 stable 统一通过 `sql/init-tdengine.sql` 手动初始化，不再增加应用启动自动建 stable 的行为。
- 聚合 child table 仍允许运行时按设备自动派生，保持现有设备级分表语法。

## 范围与非目标

### 本阶段范围

1. 新增 TDengine 小时聚合 stable 与文档说明。
2. 新增 aggregate schema support / table naming / projector 的真实实现。
3. 在现有 `TelemetryWriteFanoutService -> TelemetryAggregateProjector` 扇出链路上落地小时聚合写入。
4. 为小时聚合补齐最小测试与配置/文档同步。

### 明确不做

1. 不实现日聚合。
2. 不改 `/api/telemetry/latest`。
3. 不新增区间历史查询 API。
4. 不改报表模块读路径。
5. 不实现 cold archive 真正落库。
6. 不把 aggregate stable 接回启动自动初始化器。

## 设计选择

本轮采用单一方案：

1. `MEASURE` 小时聚合单独落地。

原因：

- `MEASURE` 的语义天然适合 `min/max/avg/last/count` 小时汇总；
- `STATUS` 更适合“最后值/跳变次数/在线时长”等独立语义，和测量类混算会制造歧义；
- `EVENT` 更适合保留 raw 明细或未来单独建事件统计模型，当前不应硬塞进通用小时聚合。

## 数据模型

### stable 命名

- stable：`iot_agg_measure_hour`
- child table：`tb_ah_<tenantId>_<deviceId>`

命名延续当前 raw 命名风格，只把前缀切换为 `ah`（aggregate hour）。

### stable 作用

- 承载设备维度、指标维度的小时级测量聚合结果；
- 面向未来长时间窗趋势、统计和报表读路径；
- 不替代 raw 明细，也不承担 latest 查询职责。

### 字段口径

- `ts`：小时窗口起点时间，同时作为行键时间；
- `metric_id`：标准化指标 ID，作为复合主键列；
- `metric_code` / `metric_name`：指标编码和显示名称；
- `value_type`：值类型，当前只允许数值类；
- `first_reported_at`：该小时窗口内最早真实上报时间；
- `last_reported_at`：该小时窗口内最新真实上报时间；
- `min_value_double`：最小值；
- `max_value_double`：最大值；
- `sum_value_double`：累计求和值；
- `last_value_double`：窗口内最后一条值；
- `sample_count`：样本数；
- `trace_id`：最后一次更新该聚合记录的 TraceId；
- `source_message_type`：来源消息类型；
- tags：`tenant_id`、`device_id`、`product_id`、`sensor_group`、`location_code`、`risk_point_id`。

说明：

- 本阶段不直接存 `avg_value_double`，统一使用 `sum_value_double / sample_count` 推导，避免滚动平均引入累计误差；
- `MEASURE` 中来自 `long` 或 `double` 的数值统一转为 `double` 聚合；
- 非数值点位直接跳过，不做降级写入。

## 写入链路

### 总体链路

1. `TelemetryWriteCoordinator` 继续只负责 raw 主写；
2. `TelemetryWriteFanoutService` 继续按开关分发 `AGGREGATE` 任务；
3. `TelemetryAggregateProjector` 从 no-op 升级为真实小时聚合投影器；
4. 投影器只处理 `TelemetryProjectionTask` 中的 `MEASURE` 点位；
5. 聚合 stable 要求已由运维/研发先执行 `sql/init-tdengine.sql` 手动初始化；
6. 聚合 child table 由运行时按设备自动 `CREATE TABLE IF NOT EXISTS`。

### 小时窗口规则

- 任意 `reported_at` 归整到所属小时起点；
- 示例：`2026-04-01 10:23:18` 归入 `2026-04-01 10:00:00`。

### 单次写入规则

同一批任务内，先按 `(tenantId, deviceId, metricId, windowStart)` 做内存归并，再写 TDengine：

- `first_reported_at`：取更早值；
- `last_reported_at`：取更晚值；
- `min_value_double`：取最小值；
- `max_value_double`：取最大值；
- `sum_value_double`：求和；
- `last_value_double`：取窗口内最后一条值；
- `sample_count`：累加；
- `trace_id`：保留最后一条点位的 TraceId。

### 对既有行为的影响

- raw 写入结果、latest 投影、legacy mirror 行为不变；
- aggregate 写失败时，仍保持 fanout 的非阻塞语义，不影响主链路返回；
- 若 aggregate stable 尚未手动初始化，projector 只记录摘要警告，不回退到启动自动建 stable。

## 组件边界

### 新增/扩展职责

1. `TelemetryAggregateProjector`
   - 负责过滤 `MEASURE` 点位；
   - 负责小时窗口归并；
   - 调用 aggregate schema support 保证 child table；
   - 执行聚合 upsert。

2. `TelemetryAggregateSchemaSupport`
   - 不负责创建 stable；
   - 只负责运行时确保 aggregate child table；
   - 要求 stable 已存在，否则返回明确失败日志。

3. `TelemetryAggregateTableNamingStrategy`
   - 统一管理 `iot_agg_measure_hour` 与 `tb_ah_*` 命名。

### 保持不变

1. `TdengineTelemetrySchemaInitializer`
   - 不接 aggregate stable 初始化。

2. `TelemetryV2SchemaSupport`
   - 继续只负责 raw stable / raw child table。

3. `TelemetryReadRouter`
   - 本阶段不新增 aggregate 读路由落地代码。

## SQL 与初始化策略

### 手动初始化

`sql/init-tdengine.sql` 需要新增：

1. `iot_agg_measure_hour` 的表介绍；
2. 字段含义；
3. child table 生成规则说明。

### 运行时自动初始化边界

- 允许：aggregate child table 自动派生；
- 不允许：aggregate stable 在应用启动或首次写入时自动创建。

这样可以把“正式基线由脚本管理”和“设备子表运行时派生”两个职责分开，避免 aggregate stable 在不同环境被代码悄悄拉起不同版本。

## 测试策略

需要补齐以下最小测试：

1. `TelemetryAggregateProjectorTest`
   - 仅聚合 `MEASURE`；
   - 跳过 `STATUS / EVENT`；
   - 同一小时窗口内正确汇总 `min/max/sum/last/count`；
   - stable 缺失或写入异常时不抛出到主链路。

2. `TelemetryAggregateSchemaSupportTest`
   - stable 已存在时可创建 child table；
   - child table 幂等创建；
   - stable 不存在时返回明确失败路径。

3. `TelemetryWriteFanoutServiceTest`
   - aggregate 开关开启时继续分发真实 aggregate 任务；
   - aggregate 下游失败不影响其它扇出。

## 文档同步

本阶段属于 TDengine 表结构、时序存储行为和配置/初始化口径变化，需同步更新：

- `docs/04-数据库设计与初始化数据.md`
- `docs/07-部署运行与配置说明.md`
- `docs/08-变更记录与技术债清单.md`

重点口径：

1. aggregate stable 必须通过 `sql/init-tdengine.sql` 手动初始化；
2. 运行时只自动创建 aggregate child table；
3. 当前 aggregate 仅覆盖 `MEASURE` 小时聚合，不包含日聚合与状态/事件聚合。

## 验证要求

完成实现后至少执行：

- `mvn -pl spring-boot-iot-telemetry -am "-DskipTests=false" "-Dmaven.test.skip=false" test`
- `mvn -pl spring-boot-iot-telemetry -am -DskipTests compile`
- `node scripts/docs/check-topology.mjs`

若文档拓扑校验仍失败，需要明确区分“本次改动引入的问题”与“仓库内既有无关失效链接”。
