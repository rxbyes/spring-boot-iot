# spring-boot-iot

## 项目简介
spring-boot-iot 是一个基于 Spring Boot 4 + Java 17 的物联网网关平台项目模板，面向设备接入、协议适配、遥测数据处理、设备管理和平台能力扩展场景。

## 当前目标
一期优先完成：
- 产品管理
- 设备管理
- HTTP 模拟设备上报
- 协议解析框架
- 消息日志落库
- 最新属性更新
- 设备在线状态维护

## Phase 1 状态
- 已完成 `docs/codex-roadmap.md` 中 Phase 1 的 Task 1 ~ Task 6
- 已验证链路：产品/设备创建、HTTP 上报、消息日志写入、最新属性更新、设备在线状态更新
- 已补充自动化单元测试：`DeviceMessageServiceImplTest`

## 技术栈
- Java 17
- Spring Boot 4
- Maven 多模块
- MyBatis Plus
- MySQL 8
- TDengine
- Redis
- HTTP / MQTT / TCP

## 推荐目录
```text
spring-boot-iot
├── AGENTS.md
├── README.md
├── docs
├── sql
├── config
├── docker
├── spring-boot-iot-common
├── spring-boot-iot-framework
├── spring-boot-iot-auth
├── spring-boot-iot-system
├── spring-boot-iot-device
├── spring-boot-iot-protocol
├── spring-boot-iot-message
└── spring-boot-iot-admin
```

## 快速开始
1. 执行 sql/init.sql
2. 执行 sql/init-data.sql
3. 根据实际环境设置数据库/Redis/MQTT 配置
4. 启动应用：`mvn -pl spring-boot-iot-admin spring-boot:run`
5. 使用 docs/device-simulator.md 中的请求进行联调

## 关键配置
当前 `dev` 配置默认指向共享测试环境，也可以通过以下环境变量覆盖到你自己的环境：
- `IOT_MYSQL_URL` / `IOT_MYSQL_USERNAME` / `IOT_MYSQL_PASSWORD`
- `IOT_REDIS_HOST` / `IOT_REDIS_PORT` / `IOT_REDIS_PASSWORD` / `IOT_REDIS_DATABASE`
- `IOT_MQTT_BROKER_URL` / `IOT_MQTT_USERNAME` / `IOT_MQTT_PASSWORD`

如果要切回本地环境，可以把这些环境变量覆盖为本地连接信息，并使用 `sql/init.sql` 与 `sql/init-data.sql` 初始化本地库。

配置文件位置：
- `spring-boot-iot-admin/src/main/resources/application-*.yml`
- `config/application-*.yml`

## 测试
- 执行全部模块测试：`mvn test -DskipTests=false`
- 仅执行设备模块测试：`mvn -pl spring-boot-iot-device test -DskipTests=false`
- 执行端到端集成测试：
  `IOT_MYSQL_URL=... IOT_MYSQL_USERNAME=... IOT_MYSQL_PASSWORD=... IOT_REDIS_HOST=... IOT_REDIS_PORT=... IOT_REDIS_PASSWORD=... IOT_REDIS_DATABASE=... IOT_MQTT_BROKER_URL=... IOT_MQTT_USERNAME=... IOT_MQTT_PASSWORD=... mvn -pl spring-boot-iot-admin test -DskipTests=false -Dtest=DeviceHttpReportE2EIntegrationTest`

## 集成测试环境变量
- `IOT_MYSQL_URL`
- `IOT_MYSQL_USERNAME`
- `IOT_MYSQL_PASSWORD`
- `IOT_REDIS_HOST`
- `IOT_REDIS_PORT`
- `IOT_REDIS_PASSWORD`
- `IOT_REDIS_DATABASE`
- `IOT_MQTT_BROKER_URL`
- `IOT_MQTT_USERNAME`
- `IOT_MQTT_PASSWORD`

## 文档导航
- docs/00-overview.md
- docs/01-architecture.md
- docs/02-module-structure.md
- docs/03-database.md
- docs/04-api.md
- docs/05-protocol.md
- docs/07-message-flow.md
- docs/09-todo.md
- docs/11-codex-tasking.md
- docs/codex-roadmap.md
- docs/codex-workflow.md
- docs/test-scenarios.md
