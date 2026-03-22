# AGENTS.md

## Project
spring-boot-iot

## Base package
com.ghlzm.iot

## Mission
Build and maintain a modular IoT gateway platform with Spring Boot 4 + Java 17, using the real shared development environment as the acceptance baseline.

## Current status
Phase 1-3 main flows are the long-term stability baseline. Phase 4 risk-platform capability is in progress but already has a usable real-environment baseline.

### Current reactor baseline
The current parent `pom.xml` activates 11 modules:
- `spring-boot-iot-common`
- `spring-boot-iot-framework`
- `spring-boot-iot-auth`
- `spring-boot-iot-system`
- `spring-boot-iot-device`
- `spring-boot-iot-protocol`
- `spring-boot-iot-message`
- `spring-boot-iot-rule`
- `spring-boot-iot-alarm`
- `spring-boot-iot-report`
- `spring-boot-iot-admin`

The repository still contains additional module directories such as `spring-boot-iot-gateway`, `spring-boot-iot-telemetry`, and `spring-boot-iot-ota`, but the active reactor is defined by the current parent `pom.xml`.

### Verified business baseline
The current verified baseline includes:
- product add/query
- device add/query
- HTTP simulated device reporting
- MQTT real access
- protocol decode through `mqtt-json`
- message log persistence
- latest property update
- device online status update
- alarm center (alarm list, alarm details, alarm confirmation, alarm suppression, alarm close)
- event disposal (event list, event details, work order dispatch, work order receive/start/complete, event feedback, event closure)
- risk point management (risk point CRUD, risk point binding)
- threshold rule configuration (rule CRUD)
- linkage rules and emergency plans (rule CRUD, plan CRUD)
- report analysis (risk trend, alarm statistics, event closure, device health)
- organization management (tree structure, CRUD)
- user management (user CRUD, password reset)
- role management (role CRUD, user-role query)
- region management
- dictionary management
- notification channel management
- audit log management

Current code also contains a risk monitoring baseline:
- `/risk-monitoring`
- `/risk-monitoring-gis`
- `spring-boot-iot-ui/src/api/riskMonitoring.ts`
- `spring-boot-iot-alarm/.../RiskMonitoringController.java`

But this risk monitoring baseline is not yet counted as delivered scope unless `docs/19-第四阶段交付边界与复验进展.md` is updated accordingly.

## Real-environment rule
All future acceptance work must use `spring-boot-iot-admin/src/main/resources/application-dev.yml` or environment-variable overrides of that file.

Do not reintroduce deprecated acceptance paths:
- old H2 acceptance profile
- standalone H2 schema acceptance script
- H2-only acceptance paths
- deprecated frontend browser-automation acceptance paths

When environment access is blocked, report the environment blocker explicitly. Do not replace real-environment acceptance with deprecated H2 fallback.

## Documentation maintenance rule
- Any frontend or backend change that affects behavior, APIs, workflows, page structure, startup steps, validation flow, configuration expectations, or product positioning must update the existing documentation in place.
- Always update the corresponding file under `docs/`.
- Always review whether `README.md` and `AGENTS.md` also need to be updated.
- Do not create duplicate replacement docs such as `README-v2.md`, `api-new.md`, `new-frontend-doc.md`, or similar files.
- This rule applies to all coding agents and coding models, including Codex, Qwen Code, and others.

## Workspace path compatibility rule
- The shared Windows 10 environment may use `E:\idea\ghatg\spring-boot-iot` as the workspace root.
- Other environments may use different absolute paths.
- Do not hardcode one absolute workspace path back into scripts or docs as the only valid path.
- Prefer deriving the repository root from the current script location, current working directory, or environment-specific configuration.
- When documentation needs an absolute-path example, explicitly label `E:\idea\ghatg\spring-boot-iot` as a Windows shared-environment example, not as a universal fixed path.

## Frontend encoding and consistency rule
- Any page or style change in `spring-boot-iot-ui` must preserve UTF-8 readability. Do not paste terminal-garbled text into `.vue`, `.ts`, `.css`, `.json`, or `.md` files.
- Before editing frontend files on Windows terminals, prefer UTF-8 viewing/verification (for example `chcp 65001` plus `Get-Content -Encoding UTF8`) so displayed text matches file contents.
- After editing frontend text content, labels, placeholders, comments, or documentation, always self-check for mojibake such as `鍒�`, `褰�`, `璇�`, `鐢�` and fix it before ending the task.
- New page optimization work must reuse the existing shared page patterns first: `PanelCard`, `StandardPagination`, `useServerPagination`, `StandardTableToolbar`, `StandardTableTextColumn`, `StandardDetailDrawer`, `StandardFormDrawer`, `StandardDrawerFooter`, `confirmAction`, shared global list styles, and existing design tokens. Do not add another page-local list/pagination/detail-dialog style when an existing standard pattern already fits.
- Overview, workbench, drawer, and confirmation interactions must stay on the unified brand/accent token system. Do not introduce another page-local blue/orange/purple palette unless the product requirement explicitly documents that exception.
- If a frontend change introduces or reveals style drift, duplicated list layouts, or inconsistent pagination behavior, record the issue and the prevention rule in `docs/15-前端优化与治理计划.md` before closing the task.

## Always read before coding
### Minimum set for all tasks
- README.md
- docs/README.md
- docs/01-系统概览与架构说明.md
- docs/02-业务功能与流程说明.md
- docs/03-接口规范与接口清单.md
- docs/04-数据库设计与初始化数据.md
- docs/07-部署运行与配置说明.md
- docs/08-变更记录与技术债清单.md

### Read additionally when relevant
- tests / acceptance / regression: docs/05-自动化测试与质量保障.md, docs/真实环境测试与验收手册.md, docs/21-业务功能清单与验收标准.md
- frontend work: docs/06-前端开发与CSS规范.md, docs/15-前端优化与治理计划.md
- MQTT / protocol / payload parsing: docs/05-protocol.md, docs/14-MQTTX真实环境联调手册.md
- observability / trace / notification: docs/11-可观测性、日志追踪与消息通知治理.md
- help center / system content governance: docs/12-帮助文档与系统内容治理.md
- multi-tenant / data permission / org scope: docs/13-数据权限与多租户模型.md
- Phase 4 scope or delivery boundary: docs/19-第四阶段交付边界与复验进展.md, docs/21-业务功能清单与验收标准.md
- stage planning / next iteration: docs/16-阶段规划与迭代路线图.md, docs/19-第四阶段交付边界与复验进展.md
- AI collaboration / takeover prompt: docs/09-GPT接管提示模板.md

### Do not treat as primary coding dependencies
- compatibility entry pages
- docs/archive/*
- historical issue ledgers / retrospective notes
- thin wrapper pages previously under docs/template/*; use docs/template/README.md instead

## Hard constraints
- Project name must remain: spring-boot-iot
- Base package must remain: com.ghlzm.iot
- Phase 1 must stay modular monolith
- `spring-boot-iot-admin` is the only startup module
- Do not break module boundaries
- Do not move persistence logic into protocol adapters
- Do not put business logic into controllers
- Do not introduce heavy dependencies unless clearly necessary

## Module boundaries
- `spring-boot-iot-common`: constants, exceptions, response models, utils
- `spring-boot-iot-framework`: config, security, redis, mybatis, global handlers
- `spring-boot-iot-auth`: authentication only
- `spring-boot-iot-system`: users, roles, organizations, regions, dictionaries, channels, audit
- `spring-boot-iot-device`: products, devices, shadows, properties, message logs
- `spring-boot-iot-gateway`: gateway and sub-device topology
- `spring-boot-iot-protocol`: protocol adapters, protocol models, codec
- `spring-boot-iot-message`: access entrypoints and dispatching only
- `spring-boot-iot-telemetry`: historical telemetry query and storage abstraction
- `spring-boot-iot-rule`: rule engine
- `spring-boot-iot-alarm`: alarm center, events, risk points, rules, plans, risk monitoring
- `spring-boot-iot-report`: report analysis
- `spring-boot-iot-ota`: ota upgrades
- `spring-boot-iot-admin`: application bootstrap

## Code style
- controller only handles request/response
- service handles orchestration
- mapper handles database access
- use `BizException` for business errors
- use `R` as unified API response
- keep naming consistent with docs
- add short Chinese comments in core logic when intent is not obvious
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
- build: `mvn -s .mvn/settings.xml clean install -DskipTests`
- run app (macOS/Linux, Windows CMD): `mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev`
- run app (Windows PowerShell): `mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run "-Dspring-boot.run.profiles=dev"`
- backend acceptance: `powershell -ExecutionPolicy Bypass -File scripts/start-backend-acceptance.ps1`
- frontend acceptance: `powershell -ExecutionPolicy Bypass -File scripts/start-frontend-acceptance.ps1`
- test: `mvn -s .mvn/settings.xml test`

## Phase execution order
### Phase 1
1. create Maven multi-module structure
2. add base infrastructure classes
3. add database entities and mappers
4. add services and controllers
5. implement HTTP reporting pipeline
6. verify property and message log persistence
7. verify online status update

### Phase 2
1. implement MQTT access skeleton
2. implement MQTT topic parsing
3. implement basic device authentication
4. implement device session and online status handling
5. complete real MQTT uplink verification
6. implement minimal MQTT downlink publishing
7. reserve sub-device topic parsing extension points

### Phase 3
1. implement command closure
2. implement gateway/sub-device business closure
3. implement basic rule engine

### Phase 4
1. implement alarm center baseline
2. implement event disposal baseline
3. implement risk point management
4. implement threshold rule configuration
5. implement linkage rules and emergency plans
6. implement report analysis
7. implement system management
8. deliver risk monitoring and GIS only when progress docs are updated accordingly

## Done definitions
### Phase 1
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` passes
- HTTP main flow real-environment acceptance passes with `application-dev.yml`
- the following APIs remain available:
  - `POST /device/product/add`
  - `GET /device/product/{id}`
  - `POST /device/add`
  - `GET /device/{id}`
  - `GET /device/code/{deviceCode}`
  - `POST /message/http/report`
  - `GET /device/{deviceCode}/properties`
  - `GET /device/{deviceCode}/message-logs`

### Phase 2
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` passes
- MQTT standard topic uplink acceptance passes in real environment
- legacy `$dp` compatibility acceptance passes in real environment
- MQTT downlink minimal publish acceptance passes in real environment

### Phase 3
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` passes
- command closure real-environment acceptance passes
- gateway/sub-device real-environment acceptance passes
- rule engine real-environment acceptance passes

### Phase 4
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` passes
- alarm center real-environment acceptance passes
- event disposal real-environment acceptance passes
- risk configuration real-environment acceptance passes
- report analysis real-environment acceptance passes
- system management real-environment acceptance passes
- `docs/19-第四阶段交付边界与复验进展.md` and `docs/21-业务功能清单与验收标准.md` are consistent with the actual delivered scope

## Known environment note
- `DeviceMessageServiceImplTest` may still fail on some JDK 17 environments because Mockito inline mock maker cannot self-attach its ByteBuddy agent.
- Treat that as a local test environment issue unless there is evidence of a real business regression.

## Auth baseline note (2026-03-16)
- `/api/auth/login` is the default login entry for web clients.
- `/message/http/report`, `/api/cockpit/**`, actuator and swagger/doc endpoints remain public.
- Other APIs are protected by JWT Bearer authentication by default.
- Frontend should attach `Authorization: Bearer <token>` after login and clear local auth state on `401`.
