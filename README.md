# spring-boot-iot

> 文档定位：项目总入口，提供当前交付基线、真实环境启动口径和核心文档导航。
> 适用角色：研发、运维、测试、交付、智能助手协作人员。
> 权威级别：一级入口。
> 上游来源：当前代码、`pom.xml`、`application-dev.yml`、核心权威文档。
> 下游消费：接手研发、环境启动、任务实施、帮助中心选题。
> 变更触发条件：交付边界、启动方式、最小阅读集、文档体系结构变化。
> 更新时间：2026-04-09

## 项目简介

`spring-boot-iot` 是一个基于 Spring Boot 4 + Java 17 的模块化单体 IoT 平台，当前覆盖：

- 设备接入：产品定义、设备资产、HTTP / MQTT 上报、协议解析、消息日志、最新属性、在线状态、最小下行
- 风险处置：实时监测、GIS 态势图、告警中心、事件协同、风险对象、阈值策略、联动编排、应急预案、运营分析
- 平台治理：组织、账号中心、用户、角色、菜单、区域、字典、治理任务台、治理运维台、治理审批台、权限与密钥治理、通知渠道、站内消息、帮助文档、审计中心
- 质量与协作：质量工场（业务验收台、研发工场、执行中心、结果与基线中心；业务验收台支持按预置验收包选择环境/账号模板/模块范围并一键查看是否通过，结果与基线中心支持最近运行读取、证据清单/原文预览与兼容手工导入）、真实环境验收、智能助手接手模板、帮助中心消费层治理

当前唯一启动模块是 `spring-boot-iot-admin`，当前父 `pom.xml` 激活 `12` 个模块：

- `spring-boot-iot-common`
- `spring-boot-iot-framework`
- `spring-boot-iot-auth`
- `spring-boot-iot-system`
- `spring-boot-iot-device`
- `spring-boot-iot-protocol`
- `spring-boot-iot-message`
- `spring-boot-iot-telemetry`
- `spring-boot-iot-rule`
- `spring-boot-iot-alarm`
- `spring-boot-iot-report`
- `spring-boot-iot-admin`

## 当前交付基线

- Phase 1~3 主链路已形成长期稳定基线。
- 设备接入主链路当前已重构为固定 `Pipeline`：HTTP / MQTT 统一按 `INGRESS -> TOPIC_ROUTE -> PROTOCOL_DECODE -> DEVICE_CONTRACT -> MESSAGE_LOG -> PAYLOAD_APPLY -> TELEMETRY_PERSIST -> DEVICE_STATE -> RISK_DISPATCH -> COMPLETE` 执行，不再依赖“旁路日志推断”判断阶段顺序。
- `spring-boot-iot-telemetry` 已进入 telemetry v2 基线；`application-dev.yml` / `application-prod.yml` 当前默认 `iot.telemetry.storage-type=tdengine`、`iot.telemetry.primary-storage=tdengine-v2`。标准化后的 `properties` 会在 `PAYLOAD_APPLY` 之后优先批量写入 TDengine v2 raw stable，再异步投影 MySQL latest、按配置镜像 legacy stable，并可在 `iot.telemetry.aggregate.enabled=true` 且 `iot.telemetry.aggregate.hourly-enabled=true` 时把 `MEASURE` 数值点位异步写入 `iot_agg_measure_hour`。应用启动时会自动补齐 `iot_device_telemetry_point` 兼容表和 `iot_raw_measure_point / iot_raw_status_point / iot_raw_event_point` 三张 raw stable；当前共享 TDengine 基线不再依赖 `COMPOSITE KEY`，raw 写入改由“以 `ingested_at` 为基准的唯一单调 `ts` 行键”保留同一次 `$dp` 上报中的多指标点位，并继续用 `reported_at` 保存设备真实采集时间。对象洞察等历史趋势默认优先按 `ingested_at/ts` 聚桶，不再把传感器采集时间直接当作趋势主时间轴。`iot_agg_measure_hour` 则继续要求先执行 `sql/init-tdengine.sql` 手动初始化，运行时只自动派生 `tb_ah_*` child table。
- `application-dev.yml`、`application-prod.yml`、`application-test.yml` 当前已显式固化 MySQL 主库 Hikari 基线，默认 `maximum-pool-size=30`、`minimum-idle=5`、`keepalive-time=300000`、`max-lifetime=1800000`、`leak-detection-threshold=20000`；dev 的 TDengine `slave_1` 也已补齐独立 Hikari 基线，避免共享环境继续沿用默认 `10` 连接池。
- `TELEMETRY_PERSIST` 当前采用非阻塞失败语义：TDengine 写失败只会把该步骤标记为 `FAILED` 并写结构化日志，不回滚 MySQL 消息日志、最新属性和设备在线状态。
- `GET /api/telemetry/latest` 当前已改为真实查询：`tdengine` 模式默认先读 telemetry v2 latest MySQL 投影表 `iot_device_metric_latest`，并在 `legacy-read-fallback-enabled=true` 时继续补齐 legacy stable + `iot_device_telemetry_point` 缺失指标；`mysql` 模式兼容回退到 `iot_device_property`。
- `POST /api/telemetry/migrate-history` 当前提供 TDengine 历史补迁入口：默认优先读取 `iot_device_telemetry_point` 标准化兼容表，缺失时再按 `specsJson.tdengineLegacy` 与 `metadataJson.tdengineLegacy` 映射回放 legacy stable；该迁移由业务代码手动触发，不会在启动阶段自动全量回灌历史数据。
- MQTT consumer 当前默认启用 Redis 租约式 `cluster-singleton`：同一套共享 MySQL / Redis / TDengine 环境里只允许 1 个 leader 节点订阅 Broker；standby 节点保持健康且仍可通过临时 publisher 客户端调用 `/api/message/mqtt/report/publish`。
- 设备离线超时调度当前也启用独立 Redis 单实例锁：默认通过 `iot:device:offline-timeout:leader` 只允许 1 个 leader 节点执行离线收口，避免共享环境多实例或旧节点把设备状态反复写回离线。
- 2026-03-27 起，MQTT 无效上报治理已进入默认基线：首批对 `DEVICE_NOT_FOUND` 与 `EMPTY_DECRYPTED_PAYLOAD` 执行 Redis 分钟桶计数与冷却抑制，`iot_device_access_error_log` 收口为失败样本归档，未登记设备最新态改由 `iot_device_invalid_report_state` 承载，设备补录 / 更换成功后会自动标记对应记录为已解封。
- Phase 4 已完成并纳入真实环境验收基线的能力，包括实时监测、GIS 态势图、告警、事件、风险策略、报表分析、系统治理和系统内容治理。
- 平台治理身份基线已补齐到“账号中心可用闭环 + 用户/机构/角色/数据范围基础模型”：右上角账号菜单统一为 `账号中心 / 修改密码 / 退出登录`；账号中心抽屉当前集中展示并维护 `基础资料 / 机构与角色 / 安全信息 / 实名资料`；`/api/auth/me` 统一返回租户、主机构、登录方式、最近登录和角色数据范围摘要，`PUT /api/user/profile` 用于当前登录用户维护昵称、实名、手机号、邮箱和头像。2026-04-03 起，角色治理以及 `区域版图 / 数据字典 / 通知编排 / 帮助文档 / 审计中心` 也已并入第一批查询侧收口：对应列表、分页、详情与新增/编辑/删除/测试接口统一按当前登录人的 `tenantId` 执行；帮助文档消费端 `access/list`、`access/page`、`access/{id}` 也已先按租户边界过滤，再叠加角色与页面路径权限。
- 当前共享开发环境已于 2026-03-24 完成 `/api/risk-monitoring/*`、`/risk-monitoring`、`/risk-monitoring-gis` 真实环境复验，风险监测基线正式纳入交付。
- 产品契约字段工作台已于 2026-04-05 收口为 `/products` 内的单页工作区。当前 `契约字段` 与 `契约字段描述` 在同页完成样本输入、识别结果、本次生效和正式字段查看，不再打开二层抽屉，也不再提供 `model-candidates`、规范预设、运行期自动提炼或 `manualDraftItems` 人工补录。样本类型固定为 `业务数据 / 状态数据`，设备结构固定为 `单台设备 / 复合设备`；单次只支持 `1` 台设备样本 JSON。复合设备模式需显式提交 `父设备编码 + 逻辑通道编码/子设备编码` 映射，平台按固定 `collector_child + LF_VALUE + SENSOR_STATE` 口径把业务样本归一为子产品 `value`，把 `S1_ZT_1.sensor_state.<logicalChannelCode>` 归一为 `sensor_state`，且不会把父终端 `temp / humidity / signal_4g` 等状态字段带入子产品。compare/apply 仍直接写入 `iot_product_model`，不新增平行草稿表。`2026-04-06` 起首批 `phase1-crack + phase2-gnss` 双场景已补齐规范字段库、厂商字段证据、合同发布批次和风险指标目录：裂缝场景当前治理 `value / sensor_state`，GNSS 场景当前治理 `gpsInitial / gpsTotalX / gpsTotalY / gpsTotalZ / sensor_state`，其中仅 `value` 与 `gpsTotalX / gpsTotalY / gpsTotalZ` 允许进入风险闭环。compare 行会补充 `normativeIdentifier / normativeName / riskReady / rawIdentifiers`，apply 成功后会返回 `releaseBatchId`。`2026-04-08` 起，审批执行真正落库后的发布批次还会补齐 `approvalOrderId / releaseReason / releaseStatus`，风险指标目录也会沉淀 `releaseBatchId / normativeIdentifier / riskCategory / metricRole / lifecycleStatus` 与扩展语义能力字段，供风险对象、对象洞察和运营分析统一按目录谱系读取。`2026-04-09` 起，`/products` 的 `当前已生效字段` 支持直接维护正式中文名称，对象洞察读取 `设备属性快照` 与 `属性趋势预览` 时会优先复用最新正式字段名；趋势时间维度固定为 `近一天 / 近一周 / 近一月 / 近一年` 四档，但页头不再重复渲染时间按钮，时间切换只保留在趋势卡内，默认进入 `近一天` 并按小时桶展示最近 `24` 小时；24 小时视图会收窄两端纯补零空窗，且不再额外渲染“支持按近一天查看设备监测数据趋势”说明和图前指标标签胶囊。对象洞察重要指标的正式真相源当前也已固定为 `iot_product.metadata_json.objectInsight.customMetrics[]`：产品定义中心新增/编辑表单、产品经营工作台 `编辑档案`，以及 `契约字段 -> 当前已生效字段` 的趋势快捷动作都会共用这同一份产品元数据；其中只允许正式 `property` 字段进入趋势快捷治理，`/insight` 运行时会严格消费 `enabled / includeInTrend / includeInExtension / sortNo`，禁用或取消趋势的字段不会进入趋势查询，`includeInExtension=false` 的重点趋势指标会按 `sortNo` 前置展示。`2026-04-09` 起，平台治理同时显式开放 `/governance-approval` 治理审批台与 `/governance-security` 权限与密钥治理页，并通过 `sql/init-data.sql` 默认回填管理/运维/开发角色菜单授权；后者默认展示治理权限矩阵和设备密钥轮换台账，只暴露密钥摘要与审计语义，不暴露明文。该能力当前只覆盖裂缝与 GNSS 治理最小切片，不代表任意设备厂商已经实现零代码接入。
- `2026-04-09` 起，平台治理控制面前端已补齐 `/governance-task` 与 `/governance-ops`，统一读取 `/api/governance/work-items` 与 `/api/governance/ops-alerts`；`/products` 中已具备正式工作项语义的“待发布合同 / 待绑定风险点”提示，以及首页管理视角相关待办都会深链到对应控制面工作台。当前首轮以前台列表与 query 驱动上下文查看为主，尚未单独补做共享 `dev` 环境页面复验，因此不把这两个页面额外记成新的已验收事实。
- 通知中心当前已具备 `system_error` 自动消息、工单相关自动消息，以及高优未读桥接既有通知渠道能力。
- 可观测当前已补齐规则化运维告警闭环：通过 `iot.observability.alerting` 在现有审计、MQTT 运行态、接入失败聚合和站内信桥接统计之上评估 4 类固定规则，并通过新场景 `observability_alert` 复用既有通知渠道。
- `message-flow` 时间线当前已纳入真实环境基线：每次 HTTP / MQTT 接入都会生成 `sessionId / traceId` 与 Redis 短期时间线，`/reporting` 与 `/message-trace` 共享同一条处理阶段复盘结果。
- `/message-trace` 当前已收口为 `链路追踪 / 失败归档` 同路由双模式：链路追踪列表通过共享工具条展示最近 1h / 24h 与失败摘要，首屏固定同一行展示 `快速搜索（TraceId / 设备编码 / 产品标识） / 消息类型 / Topic / 查询 / 重置`，不再保留该模式下的“更多筛选”切换；正常链路行内只保留 `详情`，失败样本继续复用失败归档详情、`追踪 / 观测` 等异常排障动作，不再保留独立运维看板辅战区。
- `/message-trace` 详情抽屉当前已切到客户视角收纳后的 `A 完整内容平衡版`：抽屉标题行承担轻刊头，正文完整保留 `消息概览 lead-sheet`、`链路信息（章内拆成 链路标识 / 接入上下文）`、`Payload 对照`、`处理时间线` 四个章节；首屏仍以附件终版的大标题 lead-sheet 和右侧指标列为准，但不再堆叠说明墙、排查建议或设备详情 hero。右侧 `上报时间 / Topic 节点 / 产品标识 / 日志 ID` 当前固定按 `2 x 2` 指标块对齐，默认保持单行截断避免换行炸版。`Payload 对照` 已从旧三栏常显改为 `原始 Payload / 解密后明文 / 解析结果` 三条纵向折叠板块，默认收起、按需展开，并为每个板块补齐复制动作；原始 Payload 来自消息日志详情恢复接口，解密明文与解析结果优先按当前协议从已落库消息日志重算，不再只依赖 `message-flow` 的 `PROTOCOL_DECODE.summary`。即使 Redis 时间线已过期或查询异常，仍可继续查看恢复后的明文与解析结果；若详情恢复接口返回空字符串或空对象，前端也会继续基于原始 Payload 做兜底展示，避免出现“原始有值但明文/解析为空”的误导状态。`处理时间线` 当前默认折叠，只保留状态摘要与降级提示；展开后才进入完整 Pipeline 复盘，不再用红色全局错误提示打断排查。
- `接入智维` 页面当前统一减少重复导航层级：只保留全局壳层面包屑这一处“接入智维 / 当前模块”定位。单主列表页收口为“全局面包屑 + 主列表标题 + 共享筛选/工具条/结果区”两层语法；`/products` 主卡标题保留 `产品定义中心`，`/devices` / `/system-log` 分别保留 `设备资产中心` / `异常台账`。`/reporting`、`/message-trace` 与 `/file-debug` 这类真实页签诊断页则保留“全局面包屑 + 真实页签 + 业务标题”，不再额外渲染页内同路径面包屑、页内标题壳或跨页功能菜单。
- `/reporting` 当前已升级为“结果复盘优先”的链路验证中心：默认进入 `结果复盘`，通过共享诊断状态头统一表达 verdict，并把 `最近记录` 升级为诊断清单；HTTP 缺失、MQTT pending 超时、trace 过期和 Redis 异常四类 `message-flow` 降级态继续在同页稳定表达。
- `/in-app-message` 当前已在同页补齐“桥接效果运营”专区，可直接查看桥接成功率、待重试记录、渠道/来源分布、桥接日志与逐次尝试明细。
- 帮助中心当前按“权威资料层 + 消费层”治理：`docs/` 继续做权威源，`/api/system/help-doc/**` 做角色化消费层。

## 真实环境规则

- 唯一验收基线：`spring-boot-iot-admin/src/main/resources/application-dev.yml`
- 可通过环境变量覆盖数据库、Redis、MQTT、TDengine 和可观测配置
- `application-dev.yml` 与 `application-prod.yml` 当前默认 `iot.telemetry.storage-type=tdengine`、`iot.telemetry.primary-storage=tdengine-v2`、`iot.telemetry.read-routing.latest-source=v2`、`iot.telemetry.read-routing.history-source=v2`；`application-test.yml` 保持 `mysql`
- 当前主库 Hikari 连接池不再依赖框架默认值；如共享环境再次出现 `HikariPool-1 - Connection is not available`，优先检查 `IOT_MYSQL_HIKARI_*` 覆盖值、慢 SQL 摘要与 `/actuator/metrics/hikaricp.connections.*`
- Redis 当前同时承担 `message-flow` 时间线短期留存和 MQTT consumer 领导权租约；若 Redis 不可用，本轮链路时间线 / MQTT 集群单消费者验收都视为环境阻塞，不回退到仅控制台日志模式
- 不允许回退到旧 H2 验收 profile、独立 H2 schema 脚本或 H2-only 验收路径
- 当真实环境访问受阻时，必须明确报告环境阻塞，不用降级链路替代验收结论

## 快速开始

### 1. 初始化数据库

```bash
mysql < sql/init.sql
mysql < sql/init-data.sql
```

TDengine 时序库可按需单独初始化：

```bash
taos -f sql/init-tdengine.sql
```

历史库升级请先阅读 [docs/04-数据库设计与初始化数据.md](docs/04-数据库设计与初始化数据.md)。

### 2. 安装后端依赖

若仓库存在 `.mvn/settings.xml`，可继续沿用 `-s .mvn/settings.xml`；若当前工作区未提供该文件，直接执行 plain `mvn`。

```bash
mvn -s .mvn/settings.xml clean install -DskipTests
```

### 3. 启动后端

macOS / Linux、Windows CMD：

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

Windows PowerShell：

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run "-Dspring-boot.run.profiles=dev"
```

若本轮需要严格验证刚修改过的上游模块是否已经进入运行时，优先改用 fat jar 启动：

```bash
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
java -jar spring-boot-iot-admin/target/spring-boot-iot-admin-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

- `spring-boot:run` 适合日常开发；跨模块真实环境取证时，fat jar 更容易避免 `device`、`alarm`、`framework` 等依赖模块仍引用旧类。

### 4. 启动前端

```bash
cd spring-boot-iot-ui
npm install
npm run acceptance:dev
```

### 5. 执行验收

- 本地最小质量门禁脚本：

```bash
node scripts/run-quality-gates.mjs
```

  - Windows 底层脚本：`powershell -ExecutionPolicy Bypass -File scripts/run-quality-gates.ps1`
  - macOS / Linux 底层脚本：`sh scripts/run-quality-gates.sh`

- 后端验收脚本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-backend-acceptance.ps1
```

  - 若本机 `9999` 端口被占用，可直接改用 `powershell -ExecutionPolicy Bypass -File scripts/start-backend-acceptance.ps1 -Port 10099`，或先设置 `IOT_BACKEND_ACCEPTANCE_PORT=10099` 再继续沿用原命令。
  - 脚本当前会在构建成功后把产物复制到 `logs/backend-runtime/` 下的运行副本再启动，避免上一轮验收进程锁住 `target/*.jar` 影响下一轮 `clean package`。

- message-flow 真实环境验收脚本：

```bash
python scripts/run-message-flow-acceptance.py --expired-trace-id <已过期TraceId>
```

- 前端验收脚本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-frontend-acceptance.ps1
```

- 真实环境验收手册： [docs/真实环境测试与验收手册.md](docs/真实环境测试与验收手册.md)

## 文档操作系统

### 默认最小阅读集

进入编码前，先读本文件，再补齐以下 `7` 份主文档：

1. [docs/README.md](docs/README.md)
2. [docs/01-系统概览与架构说明.md](docs/01-系统概览与架构说明.md)
3. [docs/02-业务功能与流程说明.md](docs/02-业务功能与流程说明.md)
4. [docs/03-接口规范与接口清单.md](docs/03-接口规范与接口清单.md)
5. [docs/04-数据库设计与初始化数据.md](docs/04-数据库设计与初始化数据.md)
6. [docs/07-部署运行与配置说明.md](docs/07-部署运行与配置说明.md)
7. [docs/08-变更记录与技术债清单.md](docs/08-变更记录与技术债清单.md)

### 按任务补充阅读

- 协议 / MQTT / 接入： [docs/05-protocol.md](docs/05-protocol.md)、[docs/14-MQTTX真实环境联调手册.md](docs/14-MQTTX真实环境联调手册.md)、[docs/真实环境测试与验收手册.md](docs/真实环境测试与验收手册.md)
- 前端： [docs/06-前端开发与CSS规范.md](docs/06-前端开发与CSS规范.md)、[docs/15-前端优化与治理计划.md](docs/15-前端优化与治理计划.md)
- 测试 / 验收： [docs/05-自动化测试与质量保障.md](docs/05-自动化测试与质量保障.md)、[docs/真实环境测试与验收手册.md](docs/真实环境测试与验收手册.md)、[docs/21-业务功能清单与验收标准.md](docs/21-业务功能清单与验收标准.md)
- 可观测 / 日志 / 通知： [docs/11-可观测性、日志追踪与消息通知治理.md](docs/11-可观测性、日志追踪与消息通知治理.md)
- 帮助中心 / 系统内容： [docs/12-帮助文档与系统内容治理.md](docs/12-帮助文档与系统内容治理.md)
- 多租户 / 数据权限： [docs/13-数据权限与多租户模型.md](docs/13-数据权限与多租户模型.md)
- Phase 边界 / 下阶段规划： [docs/19-第四阶段交付边界与复验进展.md](docs/19-第四阶段交付边界与复验进展.md)、[docs/16-阶段规划与迭代路线图.md](docs/16-阶段规划与迭代路线图.md)
- 智能助手协作： [docs/skills/ai-task-intake/SKILL.md](docs/skills/ai-task-intake/SKILL.md)、[docs/09-GPT接管提示模板.md](docs/09-GPT接管提示模板.md)、[docs/10-智能助手技能与任务选型指南.md](docs/10-智能助手技能与任务选型指南.md)、[docs/17-智能助手任务发起模板速查.md](docs/17-%E6%99%BA%E8%83%BD%E5%8A%A9%E6%89%8B%E4%BB%BB%E5%8A%A1%E5%8F%91%E8%B5%B7%E6%A8%A1%E6%9D%BF%E9%80%9F%E6%9F%A5.md)、[docs/template/README.md](docs/template/README.md)
- 不再作为编码主依赖：兼容入口页、`docs/archive/*`、历史台账页、`docs/template/*` 薄包装页

## 当前维护规则

1. 任何影响行为、接口、数据库、配置、流程、页面结构、帮助中心消费口径的变更，都必须同步更新权威文档。
2. 帮助中心不是第二真相源；若帮助中心与 `docs/` 冲突，以 `docs/` 与代码事实为准。
3. `docs/README.md` 只保留摘要映射和阅读导航，不再承载帮助治理细则；帮助中心治理规则统一收口到 [docs/12-帮助文档与系统内容治理.md](docs/12-帮助文档与系统内容治理.md)。
4. 旧入口文档和历史路线图默认进入 `docs/archive/`，不再作为主维护链路。
5. 日常向智能助手发任务时，优先使用 [docs/skills/ai-task-intake/SKILL.md](docs/skills/ai-task-intake/SKILL.md) 中“你本人专用版六条超短清单”；如果装不下，再使用 [docs/09-GPT接管提示模板.md](docs/09-GPT接管提示模板.md) 中的任务卡模板；只有跨模块、跨验收、跨数据库的大任务再使用 [docs/template/README.md](docs/template/README.md) 指向的长模板。
6. Git 分支治理：`master` 只作为生产分支，本地禁止直接开发、提交、合并或推送；日常编码、验证和交付准备默认只在 `codex/dev` 进行，若当前不在允许分支上，必须先停止并处理分支问题。

## 相关入口

- 文档总入口： [docs/README.md](docs/README.md)
- 智能助手技能选型： [docs/10-智能助手技能与任务选型指南.md](docs/10-智能助手技能与任务选型指南.md)
- 智能助手任务模板速查： [docs/17-智能助手任务发起模板速查.md](docs/17-%E6%99%BA%E8%83%BD%E5%8A%A9%E6%89%8B%E4%BB%BB%E5%8A%A1%E5%8F%91%E8%B5%B7%E6%A8%A1%E6%9D%BF%E9%80%9F%E6%9F%A5.md)
- 可观测与通知治理： [docs/11-可观测性、日志追踪与消息通知治理.md](docs/11-可观测性、日志追踪与消息通知治理.md)
- 帮助文档与系统内容治理： [docs/12-帮助文档与系统内容治理.md](docs/12-帮助文档与系统内容治理.md)
- 阶段规划与迭代路线： [docs/16-阶段规划与迭代路线图.md](docs/16-阶段规划与迭代路线图.md)
- 历史归档入口： [docs/archive/README.md](docs/archive/README.md)
