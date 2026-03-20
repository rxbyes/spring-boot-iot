# 模块结构

## 模块清单
- spring-boot-iot-common
- spring-boot-iot-framework
- spring-boot-iot-auth
- spring-boot-iot-system
- spring-boot-iot-device
- spring-boot-iot-gateway
- spring-boot-iot-protocol
- spring-boot-iot-message
- spring-boot-iot-rule
- spring-boot-iot-telemetry
- spring-boot-iot-alarm
- spring-boot-iot-ota
- spring-boot-iot-job
- spring-boot-iot-api
- spring-boot-iot-admin

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
- protocol 不依赖业务 service
- message 仅接入与分发
- device 承担设备核心逻辑
- framework 负责配置与基础设施
- admin 作为统一启动模块
