# spring-boot-iot

## 项目简介
spring-boot-iot 是一个基于 Spring Boot 4 + Java 17 的物联网网关平台项目模板，面向设备接入、协议适配、遥测数据处理、设备管理和平台能力扩展场景。

## Phase 1 已完成能力
当前仓库已经完成一期最小可运行平台，已落地并验证这些能力：
- 产品管理
- 设备管理
- HTTP 模拟设备上报
- `mqtt-json` 协议解析
- 消息日志落库
- 最新属性更新
- 设备在线状态维护

## Phase 1 状态
- 已完成 `docs/codex-roadmap.md` 中 Phase 1 的 Task 1 ~ Task 6
- 已验证链路：产品/设备创建、HTTP 上报、消息日志写入、最新属性更新、设备在线状态更新
- 已通过端到端验证：`DeviceHttpReportE2EIntegrationTest`
- 当前主链路可作为后续 MQTT 真接入、规则、告警、OTA 等阶段的基础

## 技术栈
- Java 17
- Spring Boot 4
- Maven 多模块
- MyBatis Plus
- MySQL 8
- TDengine
- Redis
- HTTP / MQTT / TCP

说明：
- Phase 1 当前只打通 HTTP 模拟设备上报
- MQTT 和 TCP 真接入仍属于后续阶段能力

## 当前模块
```text
spring-boot-iot
├── AGENTS.md
├── README.md
├── docs
├── sql
├── config
├── docker
├── spring-boot-iot-ui
├── spring-boot-iot-common
├── spring-boot-iot-framework
├── spring-boot-iot-auth
├── spring-boot-iot-system
├── spring-boot-iot-device
├── spring-boot-iot-protocol
├── spring-boot-iot-message
└── spring-boot-iot-admin
```

当前父 `pom.xml` 激活的 Phase 1 模块为：
- `spring-boot-iot-common`
- `spring-boot-iot-framework`
- `spring-boot-iot-auth`
- `spring-boot-iot-system`
- `spring-boot-iot-device`
- `spring-boot-iot-protocol`
- `spring-boot-iot-message`
- `spring-boot-iot-admin`

说明：
- `spring-boot-iot-ui` 是独立 Vue 3 调试前端工作区，不加入 Maven reactor。
- 页面结构参考 `vue-element-admin` 的后台导航和工作台组织方式，但使用 Vue 3 重新实现，并强化 IoT 科技感与调试属性。

## 快速开始
1. 执行 [sql/init.sql](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/sql/init.sql)
2. 如需示例数据，再执行 `sql/init-data.sql`
3. 根据实际环境设置数据库、Redis、MQTT 配置
4. 启动应用：`mvn -pl spring-boot-iot-admin spring-boot:run`
5. 参考 [docs/04-api.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/04-api.md) 或 [docs/device-simulator.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/device-simulator.md) 进行联调

## 已实现接口
- `POST /device/product/add`
- `GET /device/product/{id}`
- `POST /device/add`
- `GET /device/{id}`
- `GET /device/code/{deviceCode}`
- `POST /message/http/report`
- `GET /device/{deviceCode}/properties`
- `GET /device/{deviceCode}/message-logs`

## 关键配置
当前 `dev` 配置默认指向共享测试环境，也可以通过以下环境变量覆盖到你自己的环境：
- `IOT_MYSQL_URL` / `IOT_MYSQL_USERNAME` / `IOT_MYSQL_PASSWORD`
- `IOT_REDIS_HOST` / `IOT_REDIS_PORT` / `IOT_REDIS_PASSWORD` / `IOT_REDIS_DATABASE`
- `IOT_MQTT_BROKER_URL` / `IOT_MQTT_USERNAME` / `IOT_MQTT_PASSWORD`

如果要切回本地环境，可以把这些环境变量覆盖为本地连接信息，并使用 `sql/init.sql` 与 `sql/init-data.sql` 初始化本地库。

配置文件位置：
- `spring-boot-iot-admin/src/main/resources/application-*.yml`
- `config/application-*.yml`

## 构建与测试
- 构建：`mvn clean package -DskipTests`
- 运行：`mvn -pl spring-boot-iot-admin spring-boot:run`
- 全量测试：`mvn test -DskipTests=false`
- 一期 E2E：`mvn -pl spring-boot-iot-admin -am test -DskipTests=false -Dtest=DeviceHttpReportE2EIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`

## 联调验证
推荐按以下顺序验证一期主链路：
1. 新增产品
2. 新增设备
3. 调用 `POST /message/http/report`
4. 查询 `GET /device/{deviceCode}/properties`
5. 查询 `GET /device/{deviceCode}/message-logs`
6. 校验 `iot_device` 的在线状态和最近上报时间

完整步骤见：
- [docs/04-api.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/04-api.md)
- [docs/test-scenarios.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/test-scenarios.md)
- [docs/13-frontend-debug-console.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/13-frontend-debug-console.md)

## 前端调试台
前端目录：`spring-boot-iot-ui`

当前页面：
- 调试驾驶舱
- 产品工作台
- 设备工作台
- HTTP 上报实验台
- 设备洞察
- 未来实验室

启动方式：
1. 进入 `spring-boot-iot-ui`
2. 安装依赖：`npm install`
3. 启动开发环境：`npm run dev`

说明：
- 默认通过 Vite 代理访问 `http://localhost:9999`
- 可通过 `spring-boot-iot-ui/.env.example` 中的 `VITE_API_BASE_URL` 和 `VITE_PROXY_TARGET` 调整联调方式
- 当前页面已为图表、数字孪生、拓扑等二期功能预留入口

## 已知说明
- `DeviceHttpReportE2EIntegrationTest` 当前使用 H2 内存数据库，可以在本地直接运行
- `DeviceMessageServiceImplTest` 在部分 JDK 17 环境下可能因为 Mockito inline mock maker 无法自附加 agent 而失败，这属于测试环境限制，不是当前主链路编译阻塞

## 文档导航
- [docs/00-overview.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/00-overview.md)
- [docs/01-architecture.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/01-architecture.md)
- [docs/02-module-structure.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/02-module-structure.md)
- [docs/03-database.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/03-database.md)
- [docs/04-api.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/04-api.md)
- [docs/05-protocol.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/05-protocol.md)
- [docs/07-message-flow.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/07-message-flow.md)
- [docs/11-codex-tasking.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/11-codex-tasking.md)
- [docs/13-frontend-debug-console.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/13-frontend-debug-console.md)
- [docs/12-change-log.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/12-change-log.md)
- [docs/codex-roadmap.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/codex-roadmap.md)
- [docs/codex-workflow.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/codex-workflow.md)
- [docs/test-scenarios.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/test-scenarios.md)
