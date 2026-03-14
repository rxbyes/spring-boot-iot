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
产品模板，定义协议、节点类型、数据格式。

### iot_product_model
物模型定义，包括属性、事件、服务。

### iot_device
设备实例，记录产品、协议、在线状态、最后上报时间等。

### iot_device_property
设备最新属性值，供快速查询。

### iot_device_message_log
原始上报消息日志，供排障与审计。
