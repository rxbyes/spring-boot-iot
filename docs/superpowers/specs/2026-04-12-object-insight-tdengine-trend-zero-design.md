# 对象洞察台 TDengine 趋势全为 0 修复设计

**日期：** 2026-04-12  
**范围：** `spring-boot-iot-telemetry`、`spring-boot-iot-ui`（消费口径确认，无协议改动）  
**问题类型：** 对象洞察台历史趋势读链路缺陷 / TDengine 历史窗口查询缺失

---

## 1. 背景与问题

对象洞察台当前通过 `POST /api/telemetry/history/batch` 读取 TDengine 历史趋势，再由服务端按 `近一天 / 近一周 / 近一月 / 近一年` 聚桶并补零。用户反馈对象洞察台看到的属性趋势数据全部为 `0`，而真实数据来源是 TDengine 时序数据库。

本次排查确认：前端 `spring-boot-iot-ui/src/views/DeviceInsightView.vue` 仅消费接口返回的 `points[].buckets[]`，并没有在前端把真实值改成 `0`；真正的异常发生在后端历史查询链路。

---

## 2. 根因结论

### 2.1 直接根因

`TelemetryQueryServiceImpl` 在处理 `/api/telemetry/history/batch` 时，会先解析 `rangeCode` 并生成时间桶，但底层历史 reader 读取数据时没有把“最近窗口”下推到 SQL。

当前 `TelemetryRawHistoryReader` 的 TDengine raw 查询实际是：

- 按 `metric_id IN (...)` 过滤；
- `ORDER BY ts ASC`；
- `LIMIT 10000`；
- **没有按 `近一天/近一周/近一月/近一年` 增加时间范围过滤。**

当设备历史数据很多时，reader 会优先读到最早的一批历史点，而不是当前趋势窗口内的点。随后 `TelemetryQueryServiceImpl` 再把这些旧点按“最近时间桶”组装时，旧点全部落不到当前窗口，最终所有桶都被补零，于是对象洞察台看到“趋势全为 0”。

### 2.2 次级问题

同类风险并不只存在于 v2 raw 主读链：

- `NormalizedTelemetryHistoryReader`
- `LegacyTelemetryHistoryReader`

这两条回退链同样没有统一消费请求窗口。即使 v2 raw 修好，只要切到 fallback，仍可能继续出现“最新有值、趋势全 0”或“趋势只显示很久以前的点”。

### 2.3 影响范围

受影响的不只是 `/insight` 页面，只要消费 `/api/telemetry/history/batch` 的页面都存在同类风险。当前仓库内对象洞察台是明确依赖方，因此本次修复应落在后端统一历史查询服务，而不是前端页面兜底。

---

## 3. 目标

1. 修复 `/api/telemetry/history/batch` 在 TDengine 模式下的时间窗口读取口径。  
2. 让 `v2 raw`、`normalized fallback`、`legacy fallback` 三条历史读链全部按同一窗口过滤。  
3. 保持前端协议不变：仍返回 `deviceId / rangeCode / bucket / points[].buckets[]`。  
4. 继续保留服务端补零，但只对“当前窗口内缺值桶”补零，不再因为读错时间范围导致整段趋势都变成 0。  
5. 同步更新相关文档，明确历史查询现在会先按请求窗口过滤，再按桶补零。

---

## 4. 非目标

1. 不修改对象洞察台前端的接口协议、路由或图表交互。  
2. 不新增新的历史查询接口。  
3. 不引入新的聚合表读链替代当前 raw/history 读法。  
4. 不顺手重构 telemetry 全部 reader，只做本次问题所需的最小收口。  
5. 不改变“趋势默认优先按 `ingested_at/ts` 聚桶，缺失时回退 `reported_at`”的既有业务规则。

---

## 5. 设计方案

### 5.1 统一历史时间窗口模型

在 `TelemetryQueryServiceImpl` 内，现有 `RangeDefinition` 只表达了：

- `rangeCode`
- `bucketCode`
- `ChronoUnit`
- `slotCount`

本次把它扩展为“既能生成桶，也能生成查询窗口”的统一模型，使服务层能够同时得到：

- `bucketStart` 对齐逻辑
- `windowStart`
- `windowEnd`
- `slotCount`
- `bucket unit`

查询窗口定义：

- `1d`：最近 24 个小时桶
- `7d`：最近 7 个天桶
- `30d`：最近 30 个天桶
- `365d`：最近 12 个月桶

窗口边界由服务端统一计算，并传给所有历史 reader，避免各链路各自推断窗口。

### 5.2 v2 raw 历史读链修复

`TelemetryRawHistoryReader` 新增时间窗口参数，SQL 直接限制在当前窗口内：

- 优先按 `ingested_at` 过滤；
- 若某条历史点 `ingested_at` 为空，则允许按 `ts` / `reported_at` 回退命中；
- 结果仍按 `ts ASC` 排序；
- `LIMIT` 保留作为保护，但应作用于“窗口内数据”，而不是全历史。

这样可以保证 raw 主链返回的就是当前窗口内的点，而不是最早 10000 条点。

### 5.3 normalized fallback 读链修复

`NormalizedTelemetryHistoryReader` 新增时间窗口参数，兼容表查询改为只取当前窗口内：

- 按 `reported_at` 优先过滤；
- 若 `reported_at` 为空则回退 `ts`；
- 保持现有字段解析逻辑。

### 5.4 legacy fallback 读链修复

`LegacyTelemetryHistoryReader` 新增时间窗口参数，并把 legacy stable / legacy mapping 的历史读取限制到当前窗口。具体仍复用 legacy 现有映射逻辑，但不能再读取全历史后交给上层丢弃。

### 5.5 聚桶与补零规则保持不变

`TelemetryQueryServiceImpl.buildHistoryBatchResponse()` 继续负责：

- 按请求 `rangeCode` 生成标准桶；
- 把窗口内点映射到桶；
- 缺值桶补 `0` 且 `filled=true`；
- 真实点保持 `filled=false`。

也就是说，本次修复的关键不是改补零，而是确保“进入聚桶阶段的数据已经是正确窗口内的数据”。

---

## 6. 实施边界与涉及文件

### 后端核心文件

- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImpl.java`
  - 扩展范围定义与窗口计算
  - 将窗口参数传入各历史 reader

- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryRawHistoryReader.java`
  - 为 raw 历史查询增加窗口过滤 SQL

- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/NormalizedTelemetryHistoryReader.java`
  - 为兼容表查询增加窗口过滤 SQL

- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/LegacyTelemetryHistoryReader.java`
  - 为 legacy fallback 查询增加窗口过滤

### 测试文件

- `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImplTest.java`
  - 新增/调整测试，确保历史窗口被实际消费

如 legacy reader 已有独立测试入口，再按需补充其单测；若没有，则优先在 `TelemetryQueryServiceImplTest` 中锁定端到端服务口径。

### 文档文件

- `docs/03-接口规范与接口清单.md`
- `docs/04-数据库设计与初始化数据.md`
- `docs/08-变更记录与技术债清单.md`
- 如需同步首页摘要，再更新 `README.md` 与 `AGENTS.md`

---

## 7. 测试与验收设计

### 7.1 自动化测试

至少覆盖以下场景：

1. **历史很多，但最近窗口只有少量点时**  
   断言：返回结果命中最近窗口内真实点，而不是全 0。

2. **v2 raw 主链命中窗口过滤**  
   断言：旧窗口外历史点不会污染当前趋势桶。

3. **normalized fallback 命中窗口过滤**  
   断言：切到兼容表路径时，仍能返回当前窗口真实点。

4. **legacy fallback 命中窗口过滤**  
   断言：legacy 历史回退也不会把整段趋势打成 0。

5. **补零行为仍保留**  
   断言：窗口内无点的桶继续返回 `value=0, filled=true`，真实点仍是 `filled=false`。

### 7.2 真实环境抽查

若共享 dev 环境可用，则用真实 `application-dev.yml` 抽查至少一台历史点较多的设备：

- 先查 `/api/telemetry/history/batch`
- 再看 `/insight`
- 确认趋势不再全为 0，且仍保留缺值补零

如环境访问受阻，需明确报告环境阻塞，不回退到已废弃的 H2 验收路径。

---

## 8. 风险与回滚

### 风险

1. legacy reader 各产品历史时间列口径可能不完全一致；实现时需沿用现有 legacy 映射时间字段，不做额外语义改造。  
2. 若窗口过滤 SQL 写错，可能导致“真实点被过滤掉”，所以必须以单测先锁行为。  
3. 某些状态类点若值本身就是 `0`，不能被误判为补零；仍应严格依赖 `filled` 区分真实点与补零点。

### 回滚

若上线后发现窗口过滤影响 legacy 兼容场景，可只回滚对应 reader 的窗口过滤实现；不需要回滚前端。主接口协议不变，回滚成本可控。

---

## 9. 最终决策

采用“**统一修复三条历史读链**”方案：

- 根因在后端历史查询窗口缺失；
- 不做前端症状兜底；
- `v2 raw`、`normalized fallback`、`legacy fallback` 全部按统一时间窗口查询；
- 服务端继续负责聚桶和补零；
- 文档同步声明新的历史查询口径。

这样可以一次性解决对象洞察台趋势全 0 问题，并防止其他历史趋势消费方继续踩到同类坑。
