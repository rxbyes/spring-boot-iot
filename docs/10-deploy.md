# 部署说明

## 开发环境要求
- Java 17
- Maven 3.9+
- MySQL 8.x
- TDengine
- Redis
- 可选：EMQX / Mosquitto

## 启动步骤
1. 创建数据库 spring_boot_iot
2. 导入 sql/init.sql 与 sql/init-data.sql
3. 修改 application-dev.yml 中数据库与 Redis 配置
4. 启动 IotAdminApplication

## 后续建议
- 开发环境使用 Docker Compose
- 生产环境拆分数据库、Redis、MQTT Broker
