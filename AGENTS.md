# AGENTS.md

## Project
spring-boot-iot

## Base package
com.ghlzm.iot

## Mission
Build a modular IoT gateway platform with Spring Boot 4 + Java 17.

Phase 1 must deliver a minimal runnable platform with:
- product management
- device management
- HTTP simulated device reporting
- protocol parsing
- message log persistence
- latest property updates
- device online status updates

## Current status
Phase 1 is complete.

The current verified baseline includes:
- 8 active Phase 1 modules in the parent reactor
- `spring-boot-iot-admin` as the only startup module
- product add/query
- device add/query
- HTTP simulated device reporting
- protocol decode through `mqtt-json`
- message log persistence
- latest property update
- device online status update

When continuing development after Phase 1:
- preserve the verified HTTP reporting main flow
- do not regress the existing Phase 1 APIs and E2E path
- update docs when behavior, verification steps, or configuration expectations change

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
5. update docs if behavior changed

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

## Known environment note
- `DeviceMessageServiceImplTest` may still fail on some JDK 17 environments because Mockito inline mock maker cannot self-attach its ByteBuddy agent.
- Treat that as a local test environment issue unless there is evidence of a real business regression.
