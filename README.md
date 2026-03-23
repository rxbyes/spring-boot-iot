# spring-boot-iot

> 文档定位：项目总入口，提供当前交付基线、真实环境启动口径和核心文档导航。
> 适用角色：研发、运维、测试、交付、智能助手协作人员。
> 权威级别：一级入口。
> 上游来源：当前代码、`pom.xml`、`application-dev.yml`、核心权威文档。
> 下游消费：接手研发、环境启动、任务实施、帮助中心选题。
> 变更触发条件：交付边界、启动方式、最小阅读集、文档体系结构变化。
> 更新时间：2026-03-23

## 项目简介

`spring-boot-iot` 是一个基于 Spring Boot 4 + Java 17 的模块化单体 IoT 平台，当前覆盖：

- 设备接入：产品定义、设备资产、HTTP / MQTT 上报、协议解析、消息日志、最新属性、在线状态、最小下行
- 风险处置：告警中心、事件协同、风险对象、阈值策略、联动编排、应急预案、运营分析
- 平台治理：组织、用户、角色、菜单、区域、字典、通知渠道、站内消息、帮助文档、审计中心
- 质量与协作：自动化工场、真实环境验收、智能助手接手模板、帮助中心消费层治理

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
- `spring-boot-iot-telemetry` 已恢复为活跃构建模块；标准化后的 `properties` 会在 `PAYLOAD_APPLY` 之后写入 TDengine 通用表 `iot_device_telemetry_point`，`reply` / 文件载荷 / 空属性消息跳过时序落库。
- `TELEMETRY_PERSIST` 当前采用非阻塞失败语义：TDengine 写失败只会把该步骤标记为 `FAILED` 并写结构化日志，不回滚 MySQL 消息日志、最新属性和设备在线状态。
- `GET /api/telemetry/latest` 当前已改为真实查询：`tdengine` 模式读取 TDengine 最新时序点，`mysql` 模式兼容回退到 `iot_device_property`。
- Phase 4 已完成并纳入真实环境验收基线的能力，以告警、事件、风险策略、报表分析、系统治理和系统内容治理为主。
- `/risk-monitoring`、`/risk-monitoring-gis` 已完成代码落地，但是否计入已交付范围，继续以 [docs/19-第四阶段交付边界与复验进展.md](docs/19-第四阶段交付边界与复验进展.md) 为准。
- 通知中心当前已具备 `system_error` 自动消息、工单相关自动消息，以及高优未读桥接既有通知渠道能力。
- 可观测当前已补齐规则化运维告警闭环：通过 `iot.observability.alerting` 在现有审计、MQTT 运行态、接入失败聚合和站内信桥接统计之上评估 4 类固定规则，并通过新场景 `observability_alert` 复用既有通知渠道。
- `message-flow` 时间线当前已纳入真实环境基线：每次 HTTP / MQTT 接入都会生成 `sessionId / traceId` 与 Redis 短期时间线，`/reporting` 与 `/message-trace` 共享同一条处理阶段复盘结果。
- `/message-trace` 当前已补齐 `message-flow` 完整运维看板：同页展示运行期总量、相关性命中、lookup 健康度、阶段性能表和最近会话辅助区。
- `/reporting` 当前已补齐“最近提交”恢复面板，并统一收口 HTTP 缺失、MQTT pending 超时、trace 过期和 Redis 异常四类 `message-flow` 降级态。
- `/in-app-message` 当前已在同页补齐“桥接效果运营”专区，可直接查看桥接成功率、待重试记录、渠道/来源分布、桥接日志与逐次尝试明细。
- 帮助中心当前按“权威资料层 + 消费层”治理：`docs/` 继续做权威源，`/api/system/help-doc/**` 做角色化消费层。

## 真实环境规则

- 唯一验收基线：`spring-boot-iot-admin/src/main/resources/application-dev.yml`
- 可通过环境变量覆盖数据库、Redis、MQTT、TDengine 和可观测配置
- `application-dev.yml` 与 `application-prod.yml` 当前默认 `iot.telemetry.storage-type=tdengine`；`application-test.yml` 保持 `mysql`
- Redis 当前同时承担 `message-flow` 时间线短期留存；若 Redis 不可用，本轮链路时间线验收视为环境阻塞，不回退到仅控制台日志模式
- 不允许回退到旧 H2 验收 profile、独立 H2 schema 脚本或 H2-only 验收路径
- 当真实环境访问受阻时，必须明确报告环境阻塞，不用降级链路替代验收结论

## 快速开始

### 1. 初始化数据库

```bash
mysql < sql/init.sql
mysql < sql/init-data.sql
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

### 4. 启动前端

```bash
cd spring-boot-iot-ui
npm install
npm run acceptance:dev
```

### 5. 执行验收

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
- 智能助手协作： [docs/skills/ai-task-intake/SKILL.md](docs/skills/ai-task-intake/SKILL.md)、[docs/09-GPT接管提示模板.md](docs/09-GPT接管提示模板.md)、[docs/template/README.md](docs/template/README.md)
- 不再作为编码主依赖：兼容入口页、`docs/archive/*`、历史台账页、`docs/template/*` 薄包装页

## 当前维护规则

1. 任何影响行为、接口、数据库、配置、流程、页面结构、帮助中心消费口径的变更，都必须同步更新权威文档。
2. 帮助中心不是第二真相源；若帮助中心与 `docs/` 冲突，以 `docs/` 与代码事实为准。
3. `docs/README.md` 只保留摘要映射和阅读导航，不再承载帮助治理细则；帮助中心治理规则统一收口到 [docs/12-帮助文档与系统内容治理.md](docs/12-帮助文档与系统内容治理.md)。
4. 旧入口文档和历史路线图默认进入 `docs/archive/`，不再作为主维护链路。
5. 日常向智能助手发任务时，优先使用 [docs/skills/ai-task-intake/SKILL.md](docs/skills/ai-task-intake/SKILL.md) 中“你本人专用版六条超短清单”；如果装不下，再使用 [docs/09-GPT接管提示模板.md](docs/09-GPT接管提示模板.md) 中的任务卡模板；只有跨模块、跨验收、跨数据库的大任务再使用 [docs/template/README.md](docs/template/README.md) 指向的长模板。

## 相关入口

- 文档总入口： [docs/README.md](docs/README.md)
- 可观测与通知治理： [docs/11-可观测性、日志追踪与消息通知治理.md](docs/11-可观测性、日志追踪与消息通知治理.md)
- 帮助文档与系统内容治理： [docs/12-帮助文档与系统内容治理.md](docs/12-帮助文档与系统内容治理.md)
- 阶段规划与迭代路线： [docs/16-阶段规划与迭代路线图.md](docs/16-阶段规划与迭代路线图.md)
- 历史归档入口： [docs/archive/README.md](docs/archive/README.md)
