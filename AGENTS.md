# AGENTS.md

## Project
spring-boot-iot

## Base package
com.ghlzm.iot

## Mission
Build a modular IoT gateway platform with Spring Boot 4 + Java 17.

### Phase 1 (已完成)
Build a minimal runnable platform with:
- product management
- device management
- HTTP simulated device reporting
- protocol parsing
- message log persistence
- latest property updates
- device online status updates

### Phase 2 (已完成)
Build MQTT access capability with:
- MQTT接入骨架
- MQTT topic解析
- 设备认证基础版
- 设备会话与在线状态基础版
- MQTT上行真实联调
- MQTT下行最小发布能力
- 子设备topic规范与解析扩展点预留

### Phase 3 (已完成)
Build platform capability closure with:
- 指令闭环能力（命令记录与状态模型、MQTT回执接入）
- 网关/子设备业务闭环（静态拓扑、子设备上报/下发）
- 规则引擎基础版（单条件单动作规则）

### Phase 4 (进行中)
Build IoT risk monitoring and early warning platform with:
- 告警中心（告警列表、告警详情、告警确认、告警抑制、通知记录）
- 事件处置（事件列表、事件详情、工单派发、现场反馈、事件复盘）
- 风险点管理（风险点CRUD、风险点与设备绑定）
- 阈值规则配置（规则CRUD、规则测试）
- 联动规则与应急预案
- 分析报表（风险趋势分析、告警统计、事件闭环分析、设备健康分析）
- 系统管理（组织机构、用户管理、角色权限、区域管理、字典配置、通知渠道、审计日志）

## Current status
Phase 3 is complete. Phase 4 is in progress.

### Phase 4 Completed (2026-03-15)
- 告警中心基础能力（后端已完成，前端页面已完成）
- 事件处置基础能力（后端已完成，前端页面已完成）
- 风险点管理（后端已完成，前端页面已完成）
- 阈值规则配置（后端已完成，前端页面已完成）
- 联动规则与应急预案（后端已完成，前端页面已完成）
- 分析报表（后端已完成，前端页面已完成）
- 组织机构管理（后端已完成，前端页面已完成）
- 用户管理（后端已完成，前端页面已完成）
- SQL脚本已创建
- 后端代码编译通过
- 前端开发服务器运行正常

The current verified baseline includes:
- 11 active Phase 1-4 modules in the parent reactor
- `spring-boot-iot-admin` as the only startup module
- product add/query
- device add/query
- HTTP simulated device reporting
- MQTT real access
- protocol decode through `mqtt-json`
- message log persistence
- latest property update
- device online status update
- command record and status tracking
- gateway/sub-device topology
- basic rule engine
- alarm center (alarm list, alarm details, alarm confirmation, alarm suppression)
- event disposal (event list, event details, work order dispatch, event closure)
- risk point management (risk point CRUD, risk point binding)
- threshold rule configuration (rule CRUD, rule testing)
- linkage rules and emergency plans (rule CRUD, plan CRUD)
- report analysis (risk trend, alarm statistics, event closure, device health)
- organization management (tree structure, CRUD operations)

The current verified baseline includes:
- 10 active Phase 1-3 modules in the parent reactor
- `spring-boot-iot-admin` as the only startup module
- product add/query
- device add/query
- HTTP simulated device reporting
- MQTT real access
- protocol decode through `mqtt-json`
- message log persistence
- latest property update
- device online status update
- command record and status tracking
- gateway/sub-device topology
- basic rule engine

When continuing development after Phase 3:
- preserve the verified HTTP/MQTT reporting main flow
- preserve the verified command closure main flow
- preserve the verified gateway/sub-device topology main flow
- preserve the verified rule engine main flow
- do not regress the existing Phase 1-3 APIs and E2E path
- update docs when behavior, verification steps, or configuration expectations change

## Documentation maintenance rule
- Any frontend or backend change that affects behavior, APIs, workflows, page structure, startup steps, validation flow, configuration expectations, or product positioning must update the existing documentation in place.
- Always update the corresponding file under `docs/`.
- Always review whether `README.md` and `AGENTS.md` also need to be updated.
- Do not create duplicate replacement docs such as `README-v2.md`, `api-new.md`, `new-frontend-doc.md`, or similar files.
- This rule applies to all coding agents and coding models, including Codex, Qwen Code, and others.

## Always read before coding
- README.md
- docs/00-overview.md
- docs/01-architecture.md
- docs/02-module-structure.md
- docs/03-database.md
- docs/04-api.md
- docs/05-protocol.md
- docs/07-message-flow.md
- docs/11-codex-tasking.md
- docs/15-frontend-optimization-plan.md
- docs/16-phase3-roadmap.md
- docs/18-phase4-risk-platform-roadmap.md
- docs/codex-roadmap.md
- docs/codex-workflow.md
- docs/test-scenarios.md

## Hard constraints
- Project name must remain: spring-boot-iot
- Base package must remain: com.ghlzm.iot
- Phase 1 must stay modular monolith
- spring-boot-iot-admin is the only startup module
- Do not break module boundaries
- Do not move persistence logic into protocol adapters
- Do not put business logic into controllers
- Do not introduce heavy dependencies unless clearly necessary

## Module boundaries
- spring-boot-iot-common: constants, exceptions, response models, utils
- spring-boot-iot-framework: config, security, redis, mybatis, global handlers
- spring-boot-iot-auth: authentication only
- spring-boot-iot-system: users, roles, menus, tenants
- spring-boot-iot-device: products, devices, shadows, properties, message logs
- spring-boot-iot-gateway: gateway and sub-device topology
- spring-boot-iot-protocol: protocol adapters, protocol models, codec
- spring-boot-iot-message: access entrypoints and dispatching only
- spring-boot-iot-telemetry: historical telemetry query and storage abstraction
- spring-boot-iot-rule: rule engine
- spring-boot-iot-alarm: alarm center
- spring-boot-iot-ota: ota upgrades
- spring-boot-iot-admin: application bootstrap

## Code style
- controller only handles request/response
- service handles orchestration
- mapper handles database access
- use BizException for business errors
- use R as unified API response
- keep naming consistent with docs
- prefer small focused commits

## Before coding
1. summarize the task
2. list impacted modules
3. state implementation plan
4. note assumptions

## After coding
1. list changed files
2. explain what changed
3. explain how to run or test
4. list incomplete parts
5. update the existing docs in place if behavior changed
6. mention which docs were updated, including any updates to `README.md` and `AGENTS.md`

## Preferred commands
- build: mvn clean package -DskipTests
- run app: mvn -pl spring-boot-iot-admin spring-boot:run
- test: mvn test
- phase 1 e2e: mvn -pl spring-boot-iot-admin -am test -DskipTests=false -Dtest=DeviceHttpReportE2EIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false

## Phase 1 execution order
1. create Maven multi-module structure
2. add base infrastructure classes
3. add database entities and mappers
4. add services and controllers
5. implement HTTP reporting pipeline
6. verify property and message log persistence
7. verify online status update

## Phase 2 execution order
1. implement MQTT接入骨架
2. implement MQTT topic解析
3. implement 设备认证基础版
4. implement 设备会话与在线状态基础版
5. implement MQTT上行真实联调
6. implement MQTT下行最小发布能力
7. implement 子设备topic规范与解析扩展点预留

## Phase 3 execution order
1. implement 指令闭环能力
2. implement 网关/子设备业务闭环
3. implement 规则引擎基础版

## Phase 4 execution order (planning)
1. implement 告警中心基础能力
2. implement 事件处置基础能力
3. implement 风险点管理
4. implement 阈值规则配置
5. implement 联动规则与应急预案
6. implement 分析报表
7. implement 系统管理

## Phase 1 done definition
Phase 1 should continue to satisfy all of the following:
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` passes
- `DeviceHttpReportE2EIntegrationTest` passes
- the following APIs remain available:
  - `POST /device/product/add`
  - `GET /device/product/{id}`
  - `POST /device/add`
  - `GET /device/{id}`
  - `GET /device/code/{deviceCode}`
  - `POST /message/http/report`
  - `GET /device/{deviceCode}/properties`
  - `GET /device/{deviceCode}/message-logs`

## Phase 2 done definition
Phase 2 should continue to satisfy all of the following:
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` passes
- `DeviceMqttReportE2EIntegrationTest` passes
- MQTT上行接入验证通过
- MQTT下行发布验证通过

## Phase 3 done definition
Phase 3 should continue to satisfy all of the following:
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` passes
- 指令闭环E2E测试通过
- 网关/子设备E2E测试通过
- 规则引擎E2E测试通过

## Phase 4 done definition (planning)
Phase 4 should satisfy the following:
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` passes
- 告警中心E2E测试通过
- 事件处置E2E测试通过
- 风险配置E2E测试通过
- 报表分析E2E测试通过
- 组织机构管理E2E测试通过
- 用户管理E2E测试通过

## Known environment note
- `DeviceMessageServiceImplTest` may still fail on some JDK 17 environments because Mockito inline mock maker cannot self-attach its ByteBuddy agent.
- Treat that as a local test environment issue unless there is evidence of a real business regression.

## Phase 4 planning note
- Phase 4 will add new modules: `spring-boot-iot-event`, `spring-boot-iot-risk`, `spring-boot-iot-report`
- Phase 4 will extend existing modules: `spring-boot-iot-alarm`, `spring-boot-iot-system`
- Phase 4 will add new database tables: `iot_alarm_record`, `iot_event_record`, `iot_event_work_order`, `risk_point`, `rule_definition`, `linkage_rule`, `emergency_plan`, `sys_organization`, `sys_region`, `sys_dict`, `sys_audit_log`, `iot_notification_record`
- Phase 4 will add new frontend pages: AlarmCenterView, EventDisposalView, RiskConfigurationView, ReportAnalysisView, SystemManagementView, RealTimeMonitoringView

### Phase 4 Completed (2026-03-15) - Updated
- 告警中心基础能力（后端已完成，前端页面已完成）
- 事件处置基础能力（后端已完成，前端页面已完成）
- 风险点管理（后端已完成，前端页面已完成）
- 阈值规则配置（后端已完成，前端页面已完成）
- 联动规则与应急预案（后端已完成，前端页面已完成）
- 分析报表（后端已完成，前端页面已完成）
- 组织机构管理（后端已完成，前端页面已完成）
- 用户管理（后端已完成，前端页面已完成）
- 角色权限管理（后端已完成，前端页面已完成）
- 区域管理（后端已完成，前端页面已完成）
- 字典配置（后端已完成，前端页面已完成）
- 通知渠道（后端已完成，前端页面已完成）
- 审计日志（后端已完成，前端页面已完成）
- SQL脚本已创建
- 后端代码编译通过
- 前端开发服务器运行正常

The current verified baseline includes:
- 11 active Phase 1-4 modules in the parent reactor
- `spring-boot-iot-admin` as the only startup module
- product add/query
- device add/query
- HTTP simulated device reporting
- MQTT real access
- protocol decode through `mqtt-json`
- message log persistence
- latest property update
- device online status update
- command record and status tracking
- gateway/sub-device topology
- basic rule engine
- alarm center (alarm list, alarm details, alarm confirmation, alarm suppression)
- event disposal (event list, event details, work order dispatch, event closure)
- risk point management (risk point CRUD, risk point binding)
- threshold rule configuration (rule CRUD, rule testing)
- linkage rules and emergency plans (rule CRUD, plan CRUD)
- report analysis (risk trend, alarm statistics, event closure, device health)
- organization management (tree structure, CRUD operations)
- user management (user CRUD, password reset)
- role management (role CRUD, menu management)
