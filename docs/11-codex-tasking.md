# Codex 执行规则

## 基本约束
- 项目名必须为 spring-boot-iot
- 包名必须为 com.ghlzm.iot
- 一期保持模块化单体
- spring-boot-iot-admin 为唯一启动模块

## 模块边界
- protocol 不要强依赖业务 service
- message 只做接入和分发
- device 承担设备核心逻辑
- framework 负责配置与通用基础设施

## 开发优先级
1. 设备上报主链路
2. 产品管理
3. 设备管理
4. MQTT 真接入
5. 规则 / 告警 / OTA

## 输出要求
- 先给出计划再改代码
- 列出变更文件
- 遵循 docs 中的模块边界与命名规范
