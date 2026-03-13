# 模块结构说明

## 父工程
spring-boot-iot

## 模块清单
- spring-boot-iot-common：常量、异常、响应体、工具类
- spring-boot-iot-framework：配置、安全、Redis、MyBatis Plus、全局异常
- spring-boot-iot-auth：登录认证
- spring-boot-iot-system：用户、角色、菜单、租户
- spring-boot-iot-device：产品、设备、属性、影子、消息日志
- spring-boot-iot-gateway：网关与子设备拓扑
- spring-boot-iot-protocol：协议适配器与消息模型
- spring-boot-iot-message：接入与消息分发
- spring-boot-iot-rule：规则引擎
- spring-boot-iot-telemetry：时序数据查询与存储
- spring-boot-iot-alarm：告警中心
- spring-boot-iot-ota：OTA 升级
- spring-boot-iot-job：定时任务
- spring-boot-iot-api：开放接口
- spring-boot-iot-admin：启动模块

## 标准包结构
com.ghlzm.iot.<module>
- controller
- service
- mapper
- entity
- dto
- vo
- convert
- enums

## 依赖约束
- protocol 不要强依赖业务 service
- message 只做接入与分发
- device 承担设备核心领域逻辑
- admin 为统一启动模块
