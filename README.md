# spring-boot-iot 开发建设总结文档

## 1. 项目基本信息

* 项目名称：`spring-boot-iot`
* 基础包名：`com.ghlzm.iot`
* 技术路线：Spring Boot 3 + Java 17 + Maven 多模块
* 当前阶段目标：先完成模块化单体的一期建设，优先跑通设备接入、协议解析、数据入库、属性更新、设备在线状态更新等主链路。

## 2. 总体架构设计

### 2.1 架构目标

平台面向物联网网关与设备接入场景，兼顾以下能力：

* 多协议接入
* 统一物模型
* 设备与网关管理
* 消息流转与解耦
* 时序数据存储
* 告警与规则扩展
* 后续平滑演进微服务

### 2.2 总体分层

1. 接入层：MQTT、TCP、HTTP、WebSocket
2. 协议层：协议解析、编码、插件注册
3. 业务层：认证、系统管理、产品管理、设备管理、网关管理、规则、告警、OTA
4. 数据层：MySQL、Redis、时序库(TDengine)、对象存储
5. 运维层：日志、监控、链路追踪

### 2.3 当前一期推荐形态

* 模块化单体
* 一个启动模块：`spring-boot-iot-admin`
* 先通过 HTTP 模拟设备上报
* 协议解析与业务处理链路独立
* 后续可无缝扩展到 MQTT/TCP 真接入

## 3. 模块设计

当前规划模块如下：

* `spring-boot-iot-common`
* `spring-boot-iot-framework`
* `spring-boot-iot-auth`
* `spring-boot-iot-system`
* `spring-boot-iot-device`
* `spring-boot-iot-gateway`
* `spring-boot-iot-protocol`
* `spring-boot-iot-message`
* `spring-boot-iot-rule`
* `spring-boot-iot-telemetry`
* `spring-boot-iot-alarm`
* `spring-boot-iot-ota`
* `spring-boot-iot-job`
* `spring-boot-iot-api`
* `spring-boot-iot-admin`

### 3.1 一期优先模块

建议先优先落地这些模块：

* `spring-boot-iot-common`
* `spring-boot-iot-framework`
* `spring-boot-iot-auth`
* `spring-boot-iot-system`
* `spring-boot-iot-device`
* `spring-boot-iot-protocol`
* `spring-boot-iot-message`
* `spring-boot-iot-telemetry`
* `spring-boot-iot-admin`

## 4. 目录结构约定

### 4.1 父工程结构

```text
spring-boot-iot
├── pom.xml
├── README.md
├── spring-boot-iot-common
├── spring-boot-iot-framework
├── spring-boot-iot-auth
├── spring-boot-iot-system
├── spring-boot-iot-device
├── spring-boot-iot-gateway
├── spring-boot-iot-protocol
├── spring-boot-iot-message
├── spring-boot-iot-rule
├── spring-boot-iot-telemetry
├── spring-boot-iot-alarm
├── spring-boot-iot-ota
├── spring-boot-iot-job
├── spring-boot-iot-api
└── spring-boot-iot-admin
```

### 4.2 模块内包结构约定

```text
com.ghlzm.iot.xxx
├── controller
├── service
│   └── impl
├── mapper
├── entity
├── dto
├── vo
├── convert
├── enums
└── event
```

## 5. Maven 工程规范

### 5.1 父工程

父工程名称：`spring-boot-iot`

基础信息：

* `groupId`: `com.ghlzm.iot`
* `artifactId`: `spring-boot-iot`
* `version`: `1.0.0-SNAPSHOT`
* `packaging`: `pom`

### 5.2 技术版本建议

* Java 17
* Spring Boot 3.2.x
* Spring Cloud 2023.x
* Spring Cloud Alibaba 2023.x
* MyBatis Plus 3.5.x
* Redis / Redisson
* Knife4j 4.x
* Netty 4.1.x
* Eclipse Paho MQTT Client
* MapStruct
* Lombok

### 5.3 当前工程约束

* `spring-boot-iot-admin` 作为统一启动入口
* 协议模块避免强耦合业务模块
* 接入层只负责接入与转发，不承载重业务逻辑

## 6. 配置文件设计

### 6.1 资源目录

```text
spring-boot-iot-admin/src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
├── application-prod.yml
├── banner.txt
├── logback-spring.xml
└── mapper
```

### 6.2 公共配置内容

`application.yml` 已规划以下配置块：

* `server`
* `spring.application`
* `spring.profiles`
* `jackson`
* `mybatis-plus`
* `logging`
* `springdoc`
* `knife4j`
* `iot.security`
* `iot.tenant`
* `iot.mqtt`
* `iot.tcp`
* `iot.protocol`
* `iot.telemetry`
* `iot.device`
* `iot.message`
* `iot.rule`
* `iot.alarm`
* `iot.ota`
* `management`

### 6.3 自定义配置映射

建议通过 `IotProperties` 统一映射 `iot.*` 配置，避免项目内大量使用 `@Value`。

## 7. 数据库设计总结

### 7.1 数据库

建议数据库名：`spring_boot_iot`

### 7.2 系统表

已规划：

* `sys_user`
* `sys_role`
* `sys_user_role`
* `sys_menu`
* `sys_role_menu`
* `sys_tenant`

### 7.3 IoT 核心表

已规划：

* `iot_product`
* `iot_product_model`
* `iot_device`
* `iot_gateway`
* `iot_gateway_topology`
* `iot_device_shadow`
* `iot_device_property`
* `iot_command_record`
* `iot_rule_chain`
* `iot_alarm_record`
* `iot_ota_package`
* `iot_device_message_log`

### 7.4 一期建议最少建表

一期最少先建：

* `sys_user`
* `sys_role`
* `sys_user_role`
* `sys_tenant`
* `iot_product`
* `iot_product_model`
* `iot_device`
* `iot_device_property`
* `iot_device_message_log`

## 8. 已规划的基础代码骨架

### 8.1 common 模块

已设计基础类：

* `R`
* `PageResult`
* `BizException`
* `BaseEntity`
* `DeviceStatusEnum`
* `ProtocolTypeEnum`

### 8.2 framework 模块

已设计基础类：

* `GlobalExceptionHandler`
* `SecurityConfig`
* `MetaObjectHandlerConfig`
* `IotProperties`

### 8.3 device 模块

已设计实体与接口：

* `Product`
* `Device`
* `DeviceMessageLog`
* `DeviceProperty`
* `ProductMapper`
* `DeviceMapper`
* `DeviceMessageLogMapper`
* `DevicePropertyMapper`
* `ProductService`
* `ProductServiceImpl`
* `ProductController`
* `DevicePropertyController`
* `DeviceMessageLogController`

### 8.4 auth 模块

已设计：

* `LoginDTO`
* `AuthController`

### 8.5 protocol 模块

已设计：

* `ProtocolAdapter`
* `ProtocolContext`
* `DeviceUpMessage`
* `DeviceDownMessage`
* `RawDeviceMessage`
* `ProtocolAdapterRegistry`
* `MqttJsonProtocolAdapter`

### 8.6 message 模块

已设计：

* `DeviceReportRequest`
* `DeviceHttpController`
* `UpMessageDispatcher`

## 9. 一期设备上报主链路

### 9.1 当前实现目标

当前已规划一条可最小运行的设备上报链：

```text
设备 HTTP 上报
  -> DeviceHttpController
  -> UpMessageDispatcher
  -> ProtocolAdapterRegistry
  -> ProtocolAdapter.decode(...)
  -> DeviceMessageService
       ├── 写入 iot_device_message_log
       ├── 更新 iot_device_property
       └── 更新 iot_device 在线状态与最后上报时间
```

### 9.2 主链路类

核心处理类：

* `RawDeviceMessage`
* `DeviceHttpController`
* `UpMessageDispatcher`
* `ProtocolAdapterRegistry`
* `MqttJsonProtocolAdapter`
* `DeviceMessageService`
* `DeviceMessageServiceImpl`

### 9.3 主链路落库行为

设备上报成功后：

1. 新增一条 `iot_device_message_log`
2. 更新或新增 `iot_device_property`
3. 更新 `iot_device.online_status`
4. 更新 `iot_device.last_online_time`
5. 更新 `iot_device.last_report_time`

### 9.4 一期联调接口

#### HTTP 上报

* `POST /message/http/report`

#### 查询设备最新属性

* `GET /device/{deviceCode}/properties`

#### 查询设备消息日志

* `GET /device/{deviceCode}/message-logs`

## 10. 当前已完成内容总结

当前已明确并输出的建设内容包括：

1. 平台总体架构设计
2. 模块化分层方案
3. Maven 多模块父子工程结构
4. `pom.xml` 模板
5. 项目目录树模板
6. `application.yml`、`application-dev.yml`、`application-test.yml`、`application-prod.yml` 模板
7. `banner.txt`、`logback-spring.xml` 模板
8. 数据库表结构 SQL 初稿
9. 一期核心代码骨架
10. 设备 HTTP 上报主链路代码方案

## 11. 当前尚未完成但已规划的功能

以下功能尚未完全落地，建议作为后续持续开发清单。

### 11.1 认证与权限

* 真正的登录认证逻辑
* JWT 生成与校验完整实现
* RBAC 权限控制
* 菜单与角色权限联动
* 用户上下文与租户上下文完善

### 11.2 设备管理

* 产品分页查询
* 设备分页查询
* 设备编辑、删除、启用禁用
* 产品物模型增删改查
* 设备影子管理
* 子设备与父设备关系管理

### 11.3 协议体系

* TCP HEX 协议实现
* Modbus TCP/RTU 协议实现
* OPC UA 或自定义工业协议扩展
* 协议 SPI 插件化加载
* 协议编解码单元测试

### 11.4 消息接入

* MQTT 真接入代码
* Netty TCP 接入服务
* WebSocket 实时推送
* MQ 异步解耦
* 重试与死信机制

### 11.5 数据处理

* 时序库落地（InfluxDB / TDengine）
* 历史曲线查询
* 聚合统计
* 批量上报处理优化

### 11.6 规则与告警

* 规则链配置管理
* 表达式引擎接入
* 联动动作执行
* 告警记录生成与恢复
* 告警通知（邮件/Webhook）

### 11.7 网关能力

* 网关注册与心跳
* 网关拓扑关系管理
* 网关代子设备上报
* 边缘配置下发

### 11.8 OTA 能力

* 升级包管理
* 升级任务管理
* 分批升级与灰度升级
* 升级状态回传

### 11.9 运维能力

* Actuator 监控细化
* Prometheus / Grafana 对接
* ELK / EFK 日志采集
* TraceId 全链路日志

## 12. 后续开发优先级建议

### 第一阶段：先打通主链路

优先完成：

* 系统登录基础能力
* 产品管理
* 设备管理
* HTTP 模拟上报
* MQTT 真接入
* 协议解析
* 消息日志与最新属性更新

### 第二阶段：增强设备与协议能力

继续完成：

* 产品物模型完善
* 网关与子设备支持
* 多协议接入
* 设备影子
* 指令下发记录

### 第三阶段：平台化能力

继续完成：

* 规则引擎
* 告警中心
* 时序数据优化
* OTA 升级
* 多租户细化
* 开放 API

## 13. 开发规范建议

### 13.1 包结构规范

* controller 只做入参接收和结果返回
* service 承担业务编排
* mapper 只做数据访问
* protocol 负责协议转换
* message 负责接入和消息分发

### 13.2 命名规范

* 模块统一前缀：`spring-boot-iot-`
* 包统一前缀：`com.ghlzm.iot`
* Controller 以 `Controller` 结尾
* Service 以 `Service` 结尾
* Mapper 以 `Mapper` 结尾
* DTO / VO / Entity 职责分离

### 13.3 数据处理建议

* 原始报文必须落日志
* 最新属性与历史日志分开存储
* 接入层与业务层解耦
* 设备在线状态通过上报与心跳共同维护

## 14. 建议的下一步工作项

建议后续按如下顺序持续推进：

1. 建立完整 Git 仓库目录和父子模块
2. 导入全部 `pom.xml`
3. 创建 `IotAdminApplication`
4. 导入配置文件模板
5. 执行数据库初始化 SQL
6. 落地 common / framework / device / protocol / message 核心代码
7. 跑通 HTTP 上报链路
8. 接入 MQTT 真链路
9. 完成设备管理和产品管理接口
10. 再补规则、告警、网关、OTA 等高级能力

## 15. 建议维护方式

为了后续持续开发和完善未完成功能，建议在仓库中保留以下文档：

* `README.md`：项目启动说明
* `docs/architecture.md`：架构设计
* `docs/database.md`：数据库设计
* `docs/api.md`：接口文档
* `docs/todo.md`：待办功能清单
* `docs/protocols.md`：协议规范与适配说明

也建议把本文档作为主开发说明，持续追加：

* 已完成功能
* 待完成功能
* 技术债
* 版本迭代记录

## 16. 当前结论

目前这套 `spring-boot-iot` 方案已经形成了一套比较完整的一期建设蓝图，具备：

* 可执行的 Maven 多模块结构
* 明确的模块职责边界
* 统一的配置方案
* 可落地的数据库设计
* 核心代码骨架
* 设备上报主链路雏形

后续只需要围绕这份文档持续补全代码和数据库实现，就可以逐步把项目从“架构设计稿”推进到“可联调、可部署、可扩展”的物联网平台。
