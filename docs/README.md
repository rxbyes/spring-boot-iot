# spring-boot-iot

## 项目简介
spring-boot-iot 是一个基于 Spring Boot 3 + Java 17 的物联网网关平台初始化工程模板，面向设备接入、协议适配、遥测数据处理、设备管理和平台化扩展场景。

## 当前目标
一期优先完成以下能力：

- Maven 多模块工程骨架
- 产品管理
- 设备管理
- HTTP 模拟设备上报
- 协议解析框架
- 消息日志落库
- 最新属性更新
- 设备在线状态更新

## 技术栈
- Java 17
- Spring Boot 3
- MyBatis Plus
- MySQL 8
- Redis
- HTTP / MQTT / TCP
- Maven 多模块

## 目录说明
```text
spring-boot-iot
├── README.md
├── docs/
├── sql/
├── docker/
├── config/
├── spring-boot-iot-common
├── spring-boot-iot-framework
├── spring-boot-iot-device
├── spring-boot-iot-protocol
├── spring-boot-iot-message
└── spring-boot-iot-admin
```

## 快速开始
### 1. 初始化数据库
执行：

- `sql/init.sql`
- `sql/init-data.sql`

### 2. 修改配置
根据本地环境修改：

- `config/application.yml`
- `config/application-dev.yml`

### 3. 启动依赖服务
可选使用 `docker/docker-compose.yml` 启动：

- MySQL
- Redis
- EMQX

### 4. 启动应用
启动类：

`com.ghlzm.iot.IotAdminApplication`

### 5. 联调测试
先调用 HTTP 模拟设备上报接口：

`POST /message/http/report`

## 建议后续补充
- docs/ 中补齐设计文档
- 导入 Maven 多模块骨架
- 完成 Product / Device / Message 主链路代码
- 接入 MQTT Broker
