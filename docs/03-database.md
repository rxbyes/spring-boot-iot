# 数据库设计

## 数据库名
spring_boot_iot

## 系统表
- sys_user
- sys_role
- sys_user_role
- sys_menu
- sys_role_menu
- sys_tenant

## IoT 核心表
- iot_product
- iot_product_model
- iot_device
- iot_gateway
- iot_gateway_topology
- iot_device_shadow
- iot_device_property
- iot_command_record
- iot_rule_chain
- iot_alarm_record
- iot_ota_package
- iot_device_message_log

## 一期最小建表
- sys_user
- sys_role
- sys_user_role
- sys_tenant
- iot_product
- iot_product_model
- iot_device
- iot_device_property
- iot_device_message_log

## 核心表说明

### iot_product
用于定义产品模板与协议、节点类型、数据格式等。

### iot_product_model
用于定义物模型，包括属性、事件、服务。

### iot_device
用于存储设备实例，记录产品、协议、在线状态、激活状态、最后上报时间等。

### iot_device_property
用于存储设备最新属性值，便于快速查询。

### iot_device_message_log
用于存储原始上报日志，便于审计、排障和历史追踪。
