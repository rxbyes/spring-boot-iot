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
3. 修改 config/application-dev.yml
4. 启动 IotAdminApplication
5. 使用 docs/device-simulator.md 中的请求进行联调

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
