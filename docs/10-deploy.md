# 部署说明

## 开发环境要求
- Java 17
- Maven 3.9+
- MySQL 8.x
- Redis
- 可选：EMQX / Mosquitto

## 启动步骤
1. 创建数据库 spring_boot_iot
2. 导入 sql/init.sql 与 sql/init-data.sql
3. 修改 application-dev.yml 中数据库与 Redis 配置
4. 启动 IotAdminApplication

## 建议目录
- sql/
- docs/
- docker/

## 后续部署建议
- 开发环境使用 Docker Compose
- 生产环境建议拆分数据库、Redis、MQTT Broker
