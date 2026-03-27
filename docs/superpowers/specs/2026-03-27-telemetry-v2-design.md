# Telemetry V2 Design

**Date:** 2026-03-27
**Status:** Draft for review
**Scope:** `spring-boot-iot-telemetry` 主存储模型升级、legacy 异步兼容、多租户与大规模数据容量治理

## 1. 背景

当前 telemetry 基线已具备可用能力，但主模型仍然由 legacy 兼容需求驱动：

- 主写路径默认是 `legacy-compatible`
- legacy stable 写入由 `LegacyTdengineTelemetryWriter` 承接
- 未映射指标回退到 `iot_device_telemetry_point`
- `/api/telemetry/latest` 需要拼装 legacy stable 与回退表的结果

该模式适合真实环境过渡，但不适合作为未来长期主模型，主要问题：

1. 表结构按设备家族分裂，新增传感器需要持续补 stable / 列映射。
2. 当前写入为同步逐点 JDBC 调用，不适合高频、多设备增长。
3. latest、历史、统计、兼容写混在同一编排语义中，后续难以水平扩展。
4. 多租户当前只停留在字段保留层，未进入时序存储路由设计。

## 2. 目标

建设一套以标准点模型为主、legacy 异步镜像为辅的 telemetry v2 方案，满足：

1. 原始明细保留 180 天。
2. 长期仅保留 latest 与统计结果。
3. 同时支持单设备 latest / 历史曲线与跨风险点、区域、设备类型的批量统计。
4. 允许未来设备数量、高频采样、多租户逐步放量。
5. legacy stable 可通过配置开关整体下架，不再反向污染核心链路。

## 3. 非目标

1. 本轮不直接废弃现有 legacy stable。
2. 本轮不引入新的外部消息中间件，优先复用现有 Redis / MySQL / TDengine 体系。
3. 本轮不改变固定消息 Pipeline 阶段顺序。
4. 本轮不把多租户包装成“完整 SaaS 能力已交付”。

## 4. 容量与约束假设

### 4.1 规模假设

- 设备可能持续增长至高并发、多租户混合场景。
- 单设备可能出现：
  - `1` 条报文 / 分钟
  - `10` 条报文 / 分钟
- 单报文可能包含多个指标，典型值按 `10` 个指标估算。

### 4.2 容量结论

`日点数 = 设备数 × 每分钟报文数 × 每报文指标数 × 1440`

因此新方案必须满足：

1. raw 主存储足够瘦，避免重复存储过多静态字段。
2. 子表数量可控，不能按 `device + metric` 粒度无限膨胀。
3. latest 不能依赖 raw 全表或大范围扫描。
4. 汇总必须独立投影，不能长期直接扫 raw 支撑报表。
5. legacy 兼容必须异步化，不能成为主写入临界路径。

## 5. 总体架构

telemetry v2 采用四层结构：

1. **Raw Layer**
   - TDengine 标准点模型，作为唯一原始时序真源
   - 只保留 180 天
2. **Latest Layer**
   - Redis 热 latest + MySQL 持久 latest 双层投影
3. **Aggregate Layer**
   - TDengine 小时 / 天级汇总层
4. **Legacy Mirror Layer**
   - 老 stable 的异步镜像兼容层

主链路调整原则：

- `TELEMETRY_PERSIST` 成功判定只依赖 v2 raw 主写入
- latest / aggregate / legacy mirror 都在主写成功后异步投影
- legacy mirror 失败不回滚 raw / latest / aggregate

## 6. 新的 TDengine 主模型

### 6.1 分库建议

按数据语义拆库，减少冷热数据和不同查询模式相互影响：

- `iot_raw_measure_180d`
- `iot_raw_status_180d`
- `iot_raw_event_180d`
- `iot_agg_long`

数据库级策略建议：

- raw 库设置 `KEEP 180`
- agg 库设置更长保留期
- 根据真实环境能力评估 `CACHEMODEL`、`WAL_LEVEL`、压缩与副本策略

### 6.2 主超表分层

主超表固定为三类：

1. `iot_raw_measure_point`
   - 倾角、位移、雨量、泥位、裂缝、激光测距、GNSS 位移量等连续测量值
2. `iot_raw_status_point`
   - 电压、电量、信号、温湿度、部件状态、固件版本、在线状态等设备状态
3. `iot_raw_event_point`
   - ack、确认回执、状态切换事件、异常事件码等离散事件

### 6.3 子表粒度

child table 不按 `device + metric` 派生，而按 `tenant + device + stream_kind` 派生：

- `tb_m_<tenantId>_<deviceId>`
- `tb_s_<tenantId>_<deviceId>`
- `tb_e_<tenantId>_<deviceId>`

必要时再拆为高频 / 低频流，但默认不把 metric 拆成独立子表。

目的：

1. 控制子表总量增长速度。
2. 兼顾按设备维度的查询局部性。
3. 允许同一设备多指标共存于同一条时间轴。

### 6.4 行模型

采用单指标单行模型，但不再重复写大量静态字符串。

建议核心列：

- `ts`
- `metric_id`（`COMPOSITE KEY`）
- `reported_at`
- `ingested_at`
- `value_double`
- `value_long`
- `value_bool`
- `value_text`
- `quality_code`
- `alarm_flag`
- `trace_id`
- `session_id`
- `source_message_type`

建议 tag：

- `tenant_id`
- `device_id`
- `product_id`
- `sensor_group`
- `location_code`
- `risk_point_id`

补充说明：

- 运行时查询使用 `metric_id`，由 MySQL 元数据字典映射到 `metric_code / metric_name`。
- `device_code / product_key / metric_code / metric_name` 不在 raw 每行重复写入，避免体积膨胀。
- `trace_id / session_id` 仍保留在列中，便于排障和与主链路关联。

## 7. Latest Layer

latest 层分两级：

### 7.1 Redis 热 latest

- key：`iot:telemetry:latest:{tenantId}:{deviceId}`
- value：按 `metric_id -> latest snapshot` 存储
- 用途：
  - `/api/telemetry/latest`
  - 实时监测页
  - 设备详情页热点读取

### 7.2 MySQL 持久 latest

新增表：`iot_device_metric_latest`

建议唯一键：

- `(tenant_id, device_id, metric_id)`

主要字段：

- `tenant_id`
- `device_id`
- `product_id`
- `metric_id`
- `metric_code`
- `metric_name`
- `value_type`
- `value_double`
- `value_long`
- `value_bool`
- `value_text`
- `quality_code`
- `alarm_flag`
- `reported_at`
- `trace_id`
- `update_time`

定位：

- Redis 丢失后的回源
- 治理查询与核对
- 对现有 `iot_device_property` 的逐步替代基础

### 7.3 与现有 latest 的关系

- `iot_device_property` 继续保留为业务兼容 latest
- v2 latest 不依赖 `iot_device_property`
- `/api/telemetry/latest` 最终应先读 Redis latest，再回源 MySQL latest，再按配置决定是否使用 legacy fallback

## 8. Aggregate Layer

### 8.1 汇总层分类

- `iot_tel_measure_agg_1h`
- `iot_tel_measure_agg_1d`
- `iot_tel_status_agg_1h`
- `iot_tel_status_agg_1d`
- `iot_tel_event_agg_1h`
- `iot_tel_event_agg_1d`

### 8.2 测量类汇总字段

- `window_start`
- `metric_id`
- `sample_count`
- `min_value`
- `max_value`
- `avg_value`
- `sum_value`
- `first_value`
- `last_value`
- `abnormal_count`

### 8.3 状态类汇总字段

- `window_start`
- `metric_id`
- `sample_count`
- `last_status`
- `switch_count`
- `offline_count`
- `fault_count`

### 8.4 事件类汇总字段

- `window_start`
- `metric_id`
- `event_count`
- `success_count`
- `fail_count`
- `last_event_code`

用途：

- 风险趋势、设备健康、区域统计、长期报表全部优先走 aggregate

## 9. Legacy 兼容策略

### 9.1 兼容原则

- v2 raw 是唯一主写入真源
- legacy stable 只做异步镜像，不参与主写事务成功判定
- legacy read fallback 通过配置控制，可整体关闭

### 9.2 兼容实现

新增独立 legacy adapter 层：

- `TelemetryLegacyMirrorPublisher`
- `TelemetryLegacyMirrorProjector`
- `TelemetryLegacyReadAdapter`

现有类的未来定位：

- `LegacyTdengineTelemetryWriter`：legacy mirror writer
- `LegacyTdengineTelemetryReader`：legacy read fallback adapter
- `TdengineTelemetryStorageService`：旧 normalized fallback writer，逐步退场

### 9.3 兼容配置

legacy 兼容必须通过 `application-dev.yml` 开关控制：

```yaml
iot:
  telemetry:
    legacy-mirror:
      enabled: true
      mode: async
      retry-enabled: true
    read-routing:
      legacy-read-fallback-enabled: true
```

下架 legacy 时，只需关闭上述开关并清理适配器实现。

## 10. 异步投影链路

### 10.1 原则

主链路只做：

1. raw 主写入
2. 发布投影任务

投影链路异步处理：

- latest projector
- aggregate projector
- legacy mirror projector

### 10.2 投影任务建议字段

- `traceId`
- `sessionId`
- `tenantId`
- `deviceId`
- `productId`
- `reportedAt`
- `messageFingerprint`
- `projectionVersion`
- `metrics[]`

### 10.3 幂等键

建议：

- `traceId + deviceId + reportedAt + projectionVersion`

目标：

- 重放、补偿、重复消费时不会产生不可控脏数据

### 10.4 基础设施建议

优先复用现有基础设施，建议用 Redis Stream 或等价可靠内部队列，而不是简单 `@Async`：

- 可重试
- 可分组消费
- 可按 tenant / bucket 扩展
- 服务重启不直接丢任务

## 11. 多租户设计

### 11.1 现状

当前表结构和主数据广泛保留 `tenant_id`，但真实环境仍以默认租户基线为主。

### 11.2 v2 原则

telemetry v2 必须从第一天显式支持租户路由，而不是只保留字段。

新增路由层：

- `TenantTelemetryStorageRouter`

职责：

- 小租户走共享 TDengine 库
- 大租户可切换到独立库 / 独立实例
- 上层 telemetry service 不感知物理存储位置

### 11.3 路由覆盖范围

路由层必须同时参与：

- raw 写入
- latest 投影
- aggregate 投影
- read routing
- legacy mirror

## 12. 代码分层建议

`spring-boot-iot-telemetry` 内部建议新增 / 演进如下职责：

- `TelemetryWriteCoordinator`
- `TelemetryRawBatchWriter`
- `TelemetryProjectionDispatcher`
- `TelemetryLatestProjector`
- `TelemetryAggregateProjector`
- `TelemetryLegacyMirrorProjector`
- `TelemetryReadRouter`
- `TenantTelemetryStorageRouter`
- `TelemetryMetricDictionaryService`

现有 `TdengineTelemetryFacade` 逐步演进为统一读写路由门面，不再长期承载 legacy 主逻辑。

## 13. 配置方案

建议新增统一配置：

```yaml
iot:
  telemetry:
    primary-storage: tdengine-v2
    raw:
      enabled: true
      retention-days: 180
    latest:
      redis-enabled: true
      mysql-projection-enabled: true
    aggregate:
      enabled: true
      hourly-enabled: true
      daily-enabled: true
    legacy-mirror:
      enabled: true
      mode: async
      retry-enabled: true
    read-routing:
      latest-source: v2
      history-source: v2
      aggregate-source: v2
      legacy-read-fallback-enabled: true
    tenant-routing:
      mode: shared
```

## 14. 迁移路径

### Phase A

- 新建 v2 raw / latest / aggregate 结构
- 保留旧逻辑默认读口径
- 新链路先 shadow write / shadow project

### Phase B

- 切换 raw 主写入到 v2
- legacy stable 改为异步 mirror
- latest projector 与 aggregate projector 持续运行

### Phase C

- `/api/telemetry/latest` 灰度切到 v2 latest
- 历史查询灰度切到 v2 raw
- 报表与统计切到 aggregate

### Phase D

- 完成新旧对账
- 关闭 legacy read fallback
- 关闭 legacy mirror
- 删除 legacy 主分支依赖

## 15. 风险与防线

1. **风险：同步双写拖垮主链路**
   - 防线：legacy mirror 必须异步
2. **风险：raw 行体积膨胀**
   - 防线：字符串类标识转元数据字典，不在 raw 重复存储
3. **风险：latest 直接扫 raw**
   - 防线：Redis + MySQL 双层 latest 投影
4. **风险：子表数量爆炸**
   - 防线：按 `tenant + device + stream_kind` 控制子表
5. **风险：多租户后期改造代价高**
   - 防线：先引入 `TenantTelemetryStorageRouter`
6. **风险：legacy 下线困难**
   - 防线：legacy 全部通过独立 adapter + 配置开关隔离

## 16. 本轮实施范围建议

本轮优先落以下最小闭环：

1. v2 raw 主写入
2. latest 持久投影
3. legacy 异步 mirror
4. 新旧读路由配置骨架
5. 文档与真实环境配置同步

小时 / 天汇总、Redis 热 latest 与租户专库路由可在此基础上继续迭代，但接口和类边界要一次设计到位。
