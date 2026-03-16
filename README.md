# spring-boot-iot

## 项目简介
spring-boot-iot 是一个基于 Spring Boot 4 + Java 17 的模块化单体 IoT 网关平台，覆盖设备接入、协议适配、消息处理、设备管理，以及 Phase 4 风险监测预警处置能力。

## 当前状态
- Phase 1-3 主链路已形成稳定基线：产品管理、设备管理、HTTP 上报、MQTT 上下行、协议解析、消息日志、最新属性、在线状态。
- Phase 4 当前已完成并进入真实环境验收基线的能力：告警中心、事件处置、风险点管理、阈值规则配置、联动规则、应急预案、分析报表、组织机构、用户、角色、区域、字典、通知渠道、审计日志。
- 当前代码基线还包含 `/risk-monitoring`、`/risk-monitoring-gis` 相关页面与接口，但按 [docs/19-phase4-progress.md](docs/19-phase4-progress.md) 仍不计入本轮已交付范围。
- `spring-boot-iot-admin` 是唯一启动模块。
- 当前父 `pom.xml` 激活 10 个模块：
  - `spring-boot-iot-common`
  - `spring-boot-iot-framework`
  - `spring-boot-iot-auth`
  - `spring-boot-iot-system`
  - `spring-boot-iot-device`
  - `spring-boot-iot-protocol`
  - `spring-boot-iot-message`
  - `spring-boot-iot-alarm`
  - `spring-boot-iot-report`
  - `spring-boot-iot-admin`

## 真实环境验收基线
当前唯一验收环境基线是 [spring-boot-iot-admin/src/main/resources/application-dev.yml](spring-boot-iot-admin/src/main/resources/application-dev.yml)。

默认连接如下：
- MySQL：`8.130.107.120:3306/rm_iot`
- TDengine：`8.130.107.120:6041/iot`
- Redis：`8.130.107.120:6379`，database `8`
- MQTT Broker：`tcp://8.130.107.120:1883`

可通过环境变量覆盖：
- `IOT_MYSQL_URL` / `IOT_MYSQL_USERNAME` / `IOT_MYSQL_PASSWORD`
- `IOT_TDENGINE_URL` / `IOT_TDENGINE_USERNAME` / `IOT_TDENGINE_PASSWORD`
- `IOT_REDIS_HOST` / `IOT_REDIS_PORT` / `IOT_REDIS_PASSWORD` / `IOT_REDIS_DATABASE`
- `IOT_MQTT_BROKER_URL` / `IOT_MQTT_USERNAME` / `IOT_MQTT_PASSWORD` / `IOT_MQTT_CLIENT_ID`

当前基线已丢弃以下旧路径，不再作为验收或回归标准：
- 旧 H2 验收 profile
- 独立 H2 schema 验收脚本
- H2 专用端到端验证链路
- 旧前端自动化验收链路

## 快速开始
1. 初始化数据库：执行 [sql/init.sql](sql/init.sql)
2. 如需示例数据，再执行 `sql/init-data.sql`
3. 首次升级到当前验收基线时，按 [docs/03-database.md](docs/03-database.md) 依次执行 `sql/upgrade/` 下的升级脚本，至少包含 `20260316_iot_message_log_view.sql`
4. 启动后端：
   ```bash
   mvn -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev
   ```
5. 启动前端：
   ```bash
   cd spring-boot-iot-ui
   npm install
   npm run acceptance:dev
   ```
6. 按 [docs/test-scenarios.md](docs/test-scenarios.md) 和 [docs/21-business-functions-and-acceptance.md](docs/21-business-functions-and-acceptance.md) 进行真实环境验收

## 构建与验证
- 构建：
  ```bash
  mvn clean package -DskipTests
  ```
- 后端启动：
  ```bash
  mvn -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev
  ```
- 后端验收脚本：
  ```powershell
  powershell -ExecutionPolicy Bypass -File scripts/start-backend-acceptance.ps1
  ```
- 前端验收脚本：
  ```powershell
  powershell -ExecutionPolicy Bypass -File scripts/start-frontend-acceptance.ps1
  ```
- 自动化测试：
  ```bash
  mvn test
  ```

说明：
- 自动化测试保留单元测试和协议回归测试，但不再使用旧 H2 端到端链路作为系统验收结论。
- 真实环境不可用时，只能记录为环境阻塞，不回退到旧验收路径替代验收。

## 当前核心接口
### IoT 基础
- `POST /device/product/add`
- `GET /device/product/{id}`
- `POST /device/add`
- `GET /device/{id}`
- `GET /device/code/{deviceCode}`
- `POST /message/http/report`
- `GET /device/{deviceCode}/properties`
- `GET /device/{deviceCode}/message-logs`
- `POST /message/mqtt/down/publish`

### Phase 4 风险平台
- `/api/alarm/*`
- `/api/event/*`
- `/api/risk-point/*`
- `/api/rule-definition/*`
- `/api/linkage-rule/*`
- `/api/emergency-plan/*`
- `/api/report/*`
- `/api/organization/*`
- `/api/user/*`
- `/api/role/*`
- `/api/region/*`
- `/api/dict/*`
- `/system/channel/*`
- `/system/audit-log/*`

完整清单见：
- [docs/04-api.md](docs/04-api.md)
- [docs/21-business-functions-and-acceptance.md](docs/21-business-functions-and-acceptance.md)

## 前端说明
前端目录：`spring-boot-iot-ui`

当前要求：
- Node `>=24.0.0`
- 推荐优先使用 `spring-boot-iot-ui/.nvmrc`
- 默认通过 Vite 代理访问 `http://localhost:9999`
- 当前不再保留旧前端自动化验收入口，仅保留 `vitest` 与真实环境页面验收

## 已知说明
- `iot_device_message_log` 仍是当前物理表名；`iot_message_log` 作为兼容视图供文档和新功能统一命名使用，详见 [docs/03-database.md](docs/03-database.md)
- `DeviceMessageServiceImplTest` 在部分 JDK 17 环境下可能因 Mockito inline mock maker 无法自附加 ByteBuddy agent 而失败，这属于本地测试环境限制，不直接视为业务回归
- `ReportServiceImpl` 当前主要保证接口连通和页面可访问，统计准确性仍需在 Phase 5 补齐

## 文档导航
- [docs/03-database.md](docs/03-database.md)
- [docs/04-api.md](docs/04-api.md)
- [docs/05-protocol.md](docs/05-protocol.md)
- [docs/14-mqttx-live-runbook.md](docs/14-mqttx-live-runbook.md)
- [docs/19-phase4-progress.md](docs/19-phase4-progress.md)
- [docs/20-phase5-roadmap.md](docs/20-phase5-roadmap.md)
- [docs/21-business-functions-and-acceptance.md](docs/21-business-functions-and-acceptance.md)
- [docs/test-scenarios.md](docs/test-scenarios.md)
