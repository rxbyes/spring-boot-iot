# spring-boot-iot

> 文档定位：项目总入口，提供当前交付基线、真实环境启动口径和核心文档导航。
> 适用角色：研发、运维、测试、交付、智能助手协作人员。
> 权威级别：一级入口。
> 上游来源：当前代码、`pom.xml`、`application-dev.yml`、核心权威文档。
> 下游消费：接手研发、环境启动、任务实施、帮助中心选题。
> 变更触发条件：交付边界、启动方式、最小阅读集、文档体系结构变化。
> 更新时间：2026-04-04

## 项目简介

`spring-boot-iot` 是一个基于 Spring Boot 4 + Java 17 的模块化单体 IoT 平台，当前覆盖：

- 设备接入：产品定义、设备资产、HTTP / MQTT 上报、协议解析、消息日志、最新属性、在线状态、最小下行
- 风险处置：实时监测、GIS 态势图、告警中心、事件协同、风险对象、阈值策略、联动编排、应急预案、运营分析
- 平台治理：组织、账号中心、用户、角色、菜单、区域、字典、通知渠道、站内消息、帮助文档、审计中心
- 质量与协作：质量工场（研发工场、执行中心、结果与基线中心，支持最近运行读取、证据清单/原文预览与兼容手工导入）、真实环境验收、智能助手接手模板、帮助中心消费层治理

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
- `spring-boot-iot-telemetry` 已进入 telemetry v2 基线；`application-dev.yml` / `application-prod.yml` 当前默认 `iot.telemetry.storage-type=tdengine`、`iot.telemetry.primary-storage=tdengine-v2`。标准化后的 `properties` 会在 `PAYLOAD_APPLY` 之后优先批量写入 TDengine v2 raw stable，再异步投影 MySQL latest、按配置镜像 legacy stable，并可在 `iot.telemetry.aggregate.enabled=true` 且 `iot.telemetry.aggregate.hourly-enabled=true` 时把 `MEASURE` 数值点位异步写入 `iot_agg_measure_hour`。应用启动时会自动补齐 `iot_device_telemetry_point` 兼容表和 `iot_raw_measure_point / iot_raw_status_point / iot_raw_event_point` 三张 raw stable；`iot_agg_measure_hour` 则继续要求先执行 `sql/init-tdengine.sql` 手动初始化，运行时只自动派生 `tb_ah_*` child table。
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
- 产品物模型设计器已于 2026-03-25 完成真实环境接口、数据库与页面复验；2026-04-04 起继续在 `/products` 内复用同一抽屉，默认治理流程升级为“规范证据 + 报文证据 + 正式基线 -> compare -> apply”。当前首批内置规范预设为 `landslide-integrated-tilt-accel-crack-v1`，工作台与抽屉默认按“倾角 / 加速度 / 裂缝一体机”预设进入规范优先治理，并默认勾选倾角核心字段；单设备样本 JSON 与 `property / event / service` 人工补录继续保留为辅助证据入口。compare 行当前会并列返回规范出处、单位、原始字段别名、运行期来源表与正式模型基线；确认后仍直接写入 `iot_product_model`，不新增平行草稿表，`model-candidates/confirm` 仅保留兼容新增入口。该增强当前仍作为设备中心下一阶段维护，不并入 Phase 4 已交付范围。
- 通知中心当前已具备 `system_error` 自动消息、工单相关自动消息，以及高优未读桥接既有通知渠道能力。
- 可观测当前已补齐规则化运维告警闭环：通过 `iot.observability.alerting` 在现有审计、MQTT 运行态、接入失败聚合和站内信桥接统计之上评估 4 类固定规则，并通过新场景 `observability_alert` 复用既有通知渠道。
- `message-flow` 时间线当前已纳入真实环境基线：每次 HTTP / MQTT 接入都会生成 `sessionId / traceId` 与 Redis 短期时间线，`/reporting` 与 `/message-trace` 共享同一条处理阶段复盘结果。
- `/message-trace` 当前已收口为 `链路追踪 / 失败归档` 同路由双模式：链路追踪列表通过共享工具条展示最近 1h / 24h 与失败摘要，失败样本继续复用失败归档详情与回跳排障，不再保留独立运维看板辅战区。
- `/message-trace` 详情抽屉当前已补齐 `Payload 对照` 三栏：固定同时展示 `原始 Payload / 解密后明文 / 解析结果`。其中原始 Payload 继续来自消息日志，解密明文与解析结果来自 `message-flow` 的 `PROTOCOL_DECODE.summary` 预览；若 Redis 时间线已过期，则继续保留原始 Payload，并对解密/解析两栏明确显示“无法恢复”占位。
- `接入智维` 页面当前统一减少重复导航层级：只保留全局壳层面包屑这一处“接入智维 / 当前模块”定位。单主列表页收口为“全局面包屑 + 主列表标题 + 共享筛选/工具条/结果区”两层语法；`/products` 主卡标题保留 `产品定义中心`，`/devices` / `/system-log` 分别保留 `设备资产中心` / `异常台账`。`/reporting`、`/message-trace` 与 `/file-debug` 这类真实页签诊断页则保留“全局面包屑 + 真实页签 + 业务标题”，不再额外渲染页内同路径面包屑、页内标题壳或跨页功能菜单。
- `/reporting` 当前已升级为“结果复盘优先”的链路验证中心：默认进入 `结果复盘`，通过共享诊断状态头统一表达 verdict，并把 `最近记录` 升级为诊断清单；HTTP 缺失、MQTT pending 超时、trace 过期和 Redis 异常四类 `message-flow` 降级态继续在同页稳定表达。
- `/in-app-message` 当前已在同页补齐“桥接效果运营”专区，可直接查看桥接成功率、待重试记录、渠道/来源分布、桥接日志与逐次尝试明细。
- 帮助中心当前按“权威资料层 + 消费层”治理：`docs/` 继续做权威源，`/api/system/help-doc/**` 做角色化消费层。

## 真实环境规则

- 唯一验收基线：`spring-boot-iot-admin/src/main/resources/application-dev.yml`
- 可通过环境变量覆盖数据库、Redis、MQTT、TDengine 和可观测配置
- `application-dev.yml` 与 `application-prod.yml` 当前默认 `iot.telemetry.storage-type=tdengine`、`iot.telemetry.primary-storage=tdengine-v2`、`iot.telemetry.read-routing.latest-source=v2`；`application-test.yml` 保持 `mysql`
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
