# Database Schema Governance Design

**Date:** 2026-04-14  
**Status:** Approved in session  
**Audience:** 后端 / DBA / 运维 / 交付 / AI 协作  
**Scope:** 围绕 MySQL 与 TDengine 的初始化、注释治理、自动补齐、对象分级、表关系与血缘文档，建立统一数据库基线治理方案。

## 1. 背景

当前仓库中的数据库结构已经形成“多入口并存”状态：

1. MySQL 全量初始化主要依赖 [sql/init.sql](../../../sql/init.sql) 与 [sql/init-data.sql](../../../sql/init-data.sql)。
2. TDengine 初始化主要依赖 [sql/init-tdengine.sql](../../../sql/init-tdengine.sql)。
3. 历史环境对齐主要依赖 [scripts/run-real-env-schema-sync.py](../../../scripts/run-real-env-schema-sync.py)。
4. 运行时又额外存在分散的 schema support：
   - [spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetrySchemaSupport.java](../../../spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetrySchemaSupport.java)
   - [spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryV2SchemaSupport.java](../../../spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryV2SchemaSupport.java)
   - [spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryAggregateSchemaSupport.java](../../../spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryAggregateSchemaSupport.java)
   - 以及 `system/device` 模块中若干“探测真实库列是否存在”的 support 类。

本轮勘查得到以下明确现状：

1. [sql/init.sql](../../../sql/init.sql) 当前实际创建 `56` 张 MySQL 表，而 [docs/04-数据库设计与初始化数据.md](../../04-数据库设计与初始化数据.md) 仍写成 `55` 张，文档事实已漂移。
2. 当前 MySQL 基线中有 `9` 张表的表注释为纯英文：
   - `sys_governance_approval_order`
   - `sys_governance_approval_transition`
   - `sys_governance_approval_policy`
   - `sys_governance_replay_feedback`
   - `iot_product_contract_release_batch`
   - `iot_product_contract_release_snapshot`
   - `iot_product_metric_resolver_snapshot`
   - `risk_metric_catalog`
   - `iot_device_secret_rotation_log`
3. 当前 MySQL 基线中仍有 `188` 个字段没有注释，`134` 个字段注释为纯英文。
4. TDengine 侧当前没有列级 `COMMENT`，主要依赖 [sql/init-tdengine.sql](../../../sql/init-tdengine.sql) 中的脚本块说明来表达表和字段语义。
5. [scripts/run-real-env-schema-sync.py](../../../scripts/run-real-env-schema-sync.py) 已承担大量真实环境补齐职责，但它并不是从单一 schema 真相源生成，而是人工维护 `CREATE_TABLE_SQL / COLUMNS_TO_ADD / INDEXES_TO_ADD / VIEW_SQL` 四套结构。
6. 部分对象已出现“文档存在、初始化存在、代码主链路不消费”的边界模糊现象，例如 `risk_point_highway_detail` 当前主要停留在初始化数据和文档层，尚未看到清晰的运行时实体/服务链路。

用户本轮目标不是做一次性的字段补注释，而是建立一套后续可持续演进的数据库治理机制：

1. 所有表结构和字段备注统一收口为中文。
2. 对无用表做保守清理，形成 `保留 / 归档 / 待删` 分级，而不是直接粗暴删除。
3. 后续新建表、扩字段时，MySQL 与 TDengine 都要具备统一的自动初始化/自动补齐路径。
4. 梳理表关系、业务边界和血缘，形成长期可维护的权威文档。

## 2. 目标

本轮设计目标如下：

1. 建立一套 MySQL 与 TDengine 共享的 `schema baseline registry`，作为数据库对象的唯一真相源。
2. 统一收口数据库对象的结构、注释、索引、生命周期、业务域和关系定义。
3. 让 [sql/init.sql](../../../sql/init.sql)、[sql/init-tdengine.sql](../../../sql/init-tdengine.sql)、[scripts/run-real-env-schema-sync.py](../../../scripts/run-real-env-schema-sync.py) 与数据库关系文档都由同一份基线定义生成或至少强校验一致。
4. 把运行时自动补齐行为收口为“可幂等、低风险、显式白名单”的机制，而不是继续让各模块各自探测、各自补列。
5. 建立 `active / archived / pending_delete` 三层生命周期体系，支持后续保守清理无用表。
6. 在 [docs/04-数据库设计与初始化数据.md](../../04-数据库设计与初始化数据.md) 中形成稳定的表总览、业务边界、关系图和血缘说明。

## 3. 非目标

本轮不做以下事情：

1. 不直接引入 Flyway、Liquibase 或其它重型迁移框架。
2. 不在本轮设计阶段直接物理删除所有疑似无用表。
3. 不把“运行时自动补齐”扩展成可任意执行破坏性 DDL 的机制。
4. 不修改现有业务域职责边界，不借数据库治理顺带改 Controller / Service 业务语义。
5. 不把初始化样例数据塞入运行时 bootstrap。

## 4. 备选方案

### 4.1 方案 A：最小修补型

做法：

1. 直接修改 `init/init-data/init-tdengine/schema-sync`。
2. 顺手补中文注释。
3. 手工维护数据库关系文档。

优点：

1. 变更最小，落地最快。

问题：

1. 初始化 SQL、schema sync、运行时 schema support、文档仍是多套真相源。
2. 无法从根上解决“新增字段后要改四处”的漂移问题。
3. 无法为后续 MySQL/TDengine 统一自动化提供稳定基础。

### 4.2 方案 B：统一基线注册表型

做法：

1. 新增 schema baseline registry 作为唯一真相源。
2. 将 MySQL 表、视图、TDengine 表/stable、索引、字段、中文注释、生命周期、业务边界、上下游关系统一登记。
3. `init.sql`、`init-tdengine.sql`、schema sync、数据库关系文档统一由 registry 生成或强校验。
4. 运行时只保留低风险、幂等、显式白名单的自动补齐。

优点：

1. 能同时解决注释治理、保守清理、自动初始化、血缘文档四个目标。
2. 后续新增表/字段只需要先登记 registry，再由工具链统一产出。
3. 更适合当前项目“真实环境稳定优先、轻依赖、模块化单体”的基线。

问题：

1. 首轮需要重构现有脚本组织，工作量中等。

### 4.3 方案 C：迁移框架型

做法：

1. MySQL 引入 Flyway/Liquibase。
2. TDengine 另做自定义 migrator。
3. 所有 DDL 改为版本脚本管理。

优点：

1. 版本演进最强。

问题：

1. 对当前仓库侵入性最大。
2. TDengine 仍然需要自定义一套实现。
3. 超出本轮“在真实环境稳定基线内做数据库治理收口”的目标。

### 4.4 结论

本轮采用 **方案 B：统一基线注册表型**。

原因：

1. 它能在不引入重型依赖的前提下解决当前多入口漂移问题。
2. 它兼容现有 `init.sql + init-data.sql + init-tdengine.sql + run-real-env-schema-sync.py` 基线，不需要一下子推翻现有启动口径。
3. 它最适合承接用户已确认的两项边界：
   - 无用表按保守三级清单处理
   - MySQL 与 TDengine 都纳入统一自动化治理

## 5. 总体设计

### 5.1 一套真相源，四类产物

后续数据库治理只保留：

1. `1 套真相源`
   - schema baseline registry
2. `4 类产物`
   - MySQL 全量初始化
   - TDengine 全量初始化
   - 真实环境 schema sync
   - 数据库关系/血缘文档

### 5.2 Registry 必须描述的对象

registry 需要覆盖以下对象类型：

1. `mysql_table`
2. `mysql_view`
3. `tdengine_table`
4. `tdengine_stable`

每个对象都必须带以下元信息：

1. `name`
2. `storageType`
3. `domain`
4. `lifecycle`
5. `ownerModule`
6. `includedInInit`
7. `includedInSchemaSync`
8. `runtimeBootstrapMode`
9. `tableCommentZh`
10. `fields`
11. `indexes`
12. `relations`
13. `lineageRole`
14. `businessBoundary`

### 5.3 推荐的 registry 结构

本轮设计不强绑具体序列化格式，但建议使用仓库内可读、可 diff、易校验的声明式文件，例如：

1. `schema/mysql/*.yml`
2. `schema/tdengine/*.yml`
3. `schema/views/*.yml`

对象级字段建议如下：

```yaml
name: iot_product_contract_release_batch
storageType: mysql_table
domain: device
lifecycle: active
ownerModule: spring-boot-iot-device
includedInInit: true
includedInSchemaSync: true
runtimeBootstrapMode: schema_only
lineageRole: release_batch
tableCommentZh: 产品合同发布批次表
businessBoundary: |
  记录正式合同发布与回滚的批次元数据，
  作为产品合同、风险目录、审批执行和版本台账之间的发布真相。
fields:
  - name: id
    type: BIGINT NOT NULL
    commentZh: 批次主键
  - name: product_id
    type: BIGINT NOT NULL
    commentZh: 产品主键
  - name: release_status
    type: VARCHAR(16) NOT NULL DEFAULT 'RELEASED'
    commentZh: 发布状态（RELEASED/ROLLED_BACK）
indexes:
  - name: uk_product_release_batch
    kind: unique
    columns: [tenant_id, product_id, id]
relations:
  - type: belongs_to
    target: iot_product
    by: product_id
  - type: feeds
    target: risk_metric_catalog
    by: id -> release_batch_id
```

### 5.4 产物生成责任

基于 registry 统一生成或强校验：

1. [sql/init.sql](../../../sql/init.sql)
   - 只包含 `active` 的 MySQL 对象
2. [sql/init-tdengine.sql](../../../sql/init-tdengine.sql)
   - 只包含 `active` 的 TDengine 对象
3. [scripts/run-real-env-schema-sync.py](../../../scripts/run-real-env-schema-sync.py)
   - 改为基于 registry 同步对象、字段、索引、视图
4. [docs/04-数据库设计与初始化数据.md](../../04-数据库设计与初始化数据.md)
   - 自动回写或至少强校验对象数量、生命周期、关系图与血缘摘要

## 6. 生命周期分级与保守清理

### 6.1 生命周期定义

所有数据库对象必须显式标记生命周期：

1. `active`
   - 当前主链路或正式功能仍在消费
   - 进入主初始化基线
   - 进入 schema sync
   - 可进入运行时 bootstrap 白名单
2. `archived`
   - 当前不在主链路消费，但保留历史数据价值、导入价值或文档价值
   - 不再进入主初始化链路
   - 不进入默认运行时 bootstrap
   - 保留归档说明与必要的归档 SQL
3. `pending_delete`
   - 已确认无运行时消费、无保留价值
   - 进入待删清单
   - 首轮不直接物理删除

### 6.2 生命周期判定规则

判定规则固定为：

1. 命中 `Controller / Service / Mapper / 运行时 bootstrap / acceptance / seed` 真实消费链路的对象，默认 `active`
2. 只在 `sql/init*.sql`、文档、历史导入样例中出现，但代码主链路无实体/无服务消费的对象，优先评估为 `archived`
3. 既无代码消费，也无文档边界，也无历史保留价值的对象，进入 `pending_delete`

### 6.3 首批可疑对象处理原则

按本轮勘查结果：

1. `risk_point_highway_detail`
   - 当前只在初始化脚本、初始化样例和文档中明确出现
   - 主代码中未看到实体、Mapper、Service 或运行时消费链路
   - 首轮应列为 `archived candidate`
   - 需要业务确认“是否仍保留高速项目原始归档语义”
2. `risk_point_device_capability_binding`
   - 当前已进入风险点设备级正式绑定、审批执行、绑定维护与读取链路
   - 必须保持 `active`

### 6.4 清理产物

本轮不直接删表，而是形成三类清单：

1. `active objects`
2. `archived objects`
3. `pending_delete objects`

并在文档中明确回答：

1. 这张表属于哪个业务域
2. 为什么保留
3. 为什么归档
4. 为什么待删

## 7. 中文注释治理

### 7.1 MySQL 注释规则

所有 MySQL 表和字段注释统一改为中文。

统一规则：

1. 表注释必须使用中文业务名称。
2. 字段注释必须使用中文业务语义。
3. 审计字段也必须补齐中文注释，不再允许裸列：
   - `create_by`
   - `create_time`
   - `update_by`
   - `update_time`
   - `deleted`
4. 枚举字段采用“中文主释义 + 英文值补充”格式，例如：
   - `审批状态（PENDING/APPROVED/REJECTED/CANCELLED）`
   - `发布状态（RELEASED/ROLLED_BACK）`

### 7.2 TDengine 注释规则

由于当前 TDengine 脚本没有列级 `COMMENT`，统一收口为：

1. 脚本块级中文表说明
2. 脚本块级中文字段字典
3. 文档层逐表字段说明

换言之，TDengine 侧要达到的效果不是“语法级 comment 一致”，而是“语义说明唯一、稳定、自动同步”。

### 7.3 首批注释治理范围

首轮至少覆盖：

1. `9` 张纯英文表注释表
2. `188` 个缺注释字段
3. `134` 个纯英文字段注释
4. [docs/04-数据库设计与初始化数据.md](../../04-数据库设计与初始化数据.md) 中与对象数量、表名、边界说明不一致的内容

## 8. 自动初始化与自动补齐

### 8.1 三层机制

自动化收口为三层：

1. `全量初始化层`
   - 面向新库或重建库
   - 产物：`init.sql / init-data.sql / init-tdengine.sql`
2. `真实环境同步层`
   - 面向共享 dev、历史库、无法重建的现场库
   - 产物：`run-real-env-schema-sync.py`
3. `运行时最小自修复层`
   - 面向主链路必需、可幂等、低风险的结构补齐

### 8.2 MySQL 自动补齐边界

MySQL 运行时允许：

1. 补 `active` 表
2. 补 `active` 字段
3. 补 `active` 索引
4. 补 `active` 视图

MySQL 运行时禁止：

1. 自动补初始化样例数据
2. 自动补 `archived/pending_delete` 对象
3. 执行破坏性 DDL

### 8.3 TDengine 自动补齐边界

TDengine 运行时允许：

1. 补数据库
2. 补兼容表
3. 补 raw stable
4. 补 child table

TDengine 运行时限制：

1. `iot_agg_measure_hour` 这类需要环境版本确认的 stable，不进入默认自动创建
2. 这类对象由 registry 标记为 `manual_bootstrap_required`
3. 只有显式启用的 bootstrap 命令或专项同步脚本才可创建

### 8.4 对现有 support 的收口

当前分散的 support 需要逐步收口：

1. telemetry 模块保留必要的时序 bootstrap
2. `system/device` 模块中的“只读列探测 support”不再长期扩张
3. 后续统一下沉到单一 schema bootstrap 入口

### 8.5 新增表/字段的标准流程

后续新增表、扩字段的标准动作固定为：

1. 先登记 registry
2. 再生成或校验 MySQL/TDengine 初始化脚本
3. 再生成或校验 schema sync
4. 再更新数据库关系文档
5. 需要运行时自修复时，再进入 bootstrap 白名单

不允许继续直接改 `init.sql` 或手工往 schema sync 里追加补列逻辑而不登记 registry。

## 9. 文档、关系与血缘

### 9.1 文档落点

不新增平行主文档，继续使用：

1. 主入口：[docs/04-数据库设计与初始化数据.md](../../04-数据库设计与初始化数据.md)
2. 如内容过长，只允许把逐表明细下沉到 `docs/appendix/` 下的单一附录

### 9.2 docs/04 需要稳定维护的章节

建议在 [docs/04-数据库设计与初始化数据.md](../../04-数据库设计与初始化数据.md) 固定维护以下章节：

1. `数据库对象总览`
2. `业务域边界`
3. `表关系与血缘`
4. `归档与待删对象清单`

### 9.3 对象总览必须包含的字段

每个对象至少展示：

1. 表名
2. 存储类型
3. 业务域
4. 生命周期
5. 所属模块
6. 是否进入 init
7. 是否进入 schema sync
8. 是否允许运行时补齐
9. 备注

### 9.4 建议固化的主血缘链

至少固化以下 `5` 条主血缘链：

1. 产品契约血缘
   - `iot_product -> iot_product_model -> iot_product_contract_release_batch -> iot_product_contract_release_snapshot -> iot_product_metric_resolver_snapshot -> risk_metric_catalog`
2. 设备主档血缘
   - `iot_product -> iot_device -> iot_device_relation -> iot_device_property / iot_device_online_session / iot_device_message_log`
3. 遥测血缘
   - `iot_device_message_log -> TDengine raw -> iot_device_metric_latest -> history/latest/insight`
4. 风险闭环血缘
   - `risk_metric_catalog -> risk_point_device / risk_point_device_capability_binding -> rule_definition -> iot_alarm_record -> iot_event_record -> iot_event_work_order`
5. 治理审批血缘
   - `sys_governance_approval_policy -> sys_governance_approval_order -> sys_governance_approval_transition -> iot_governance_work_item / 领域执行结果`

### 9.5 业务边界表达方式

关系文档不只回答“谁关联谁”，还必须回答：

1. 这张表属于哪个业务域
2. 它是主真相、快照、投影、桥接还是历史归档
3. 它的上游是谁
4. 它的下游是谁
5. 是否允许继续扩字段
6. 是否处于清理观察名单

## 10. 实施分期

### 10.1 Phase 1：基线建模

1. 建立 schema registry 文件结构
2. 先覆盖 `active` 对象
3. 为每个对象补齐业务域、生命周期、中文注释与关系定义

### 10.2 Phase 2：产物收口

1. 让 `init.sql`、`init-tdengine.sql`、schema sync 从 registry 生成或校验一致
2. 修复对象数量漂移
3. 补齐中文注释

### 10.3 Phase 3：生命周期治理

1. 输出 `active / archived / pending_delete` 清单
2. 将 `archived` 对象从主初始化链路移出
3. 保留归档说明和必要的历史保留策略

### 10.4 Phase 4：文档和验证

1. 回写 [docs/04-数据库设计与初始化数据.md](../../04-数据库设计与初始化数据.md)
2. 生成关系/血缘摘要
3. 增加一致性校验脚本，确保对象数量、注释完整度、文档与 registry 一致

## 11. 风险与控制

### 11.1 风险：首轮 registry 改造范围偏大

控制：

1. 先只覆盖 `active` 对象
2. `archived` 与 `pending_delete` 清单先文档化，不立即大规模删对象

### 11.2 风险：自动补齐误扩展到业务数据

控制：

1. bootstrap 仅限结构对象
2. 样例数据和业务 seed 继续走初始化/专项同步

### 11.3 风险：TDengine 自动化过度

控制：

1. 对 `iot_agg_measure_hour` 这类环境敏感对象采用 `manual_bootstrap_required`
2. 不把所有 stable 一刀切纳入默认运行时自动创建

### 11.4 风险：文档再次漂移

控制：

1. 对对象数量、生命周期、注释完整度增加自动校验
2. 将 [docs/04-数据库设计与初始化数据.md](../../04-数据库设计与初始化数据.md) 纳入数据库治理变更的强制更新范围

## 12. 验收标准

设计落地后，应满足：

1. MySQL 与 TDengine 都存在统一的 schema baseline registry。
2. [sql/init.sql](../../../sql/init.sql)、[sql/init-tdengine.sql](../../../sql/init-tdengine.sql)、[scripts/run-real-env-schema-sync.py](../../../scripts/run-real-env-schema-sync.py) 与文档基于同一套对象定义生成或强校验一致。
3. 当前 `56` 张 MySQL 表、对象数量和生命周期在文档中可被准确回答。
4. 表和字段注释统一改为中文，至少消除当前 `9` 张纯英文表注释、`188` 个缺注释字段、`134` 个纯英文字段注释。
5. `active / archived / pending_delete` 清单可用于指导后续保守清理。
6. [docs/04-数据库设计与初始化数据.md](../../04-数据库设计与初始化数据.md) 能明确回答业务边界、表关系和主血缘链。
7. 后续新增表或字段时，团队存在统一流程，不再出现“只改 init.sql、不改 sync、不改文档”的多源漂移。
