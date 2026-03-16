# spring-boot-iot

## 项目简介
spring-boot-iot 是一个基于 Spring Boot 4 + Java 17 的模块化单体 IoT 网关平台，覆盖设备接入、协议适配、消息处理、设备管理，以及 Phase 4 风险监测预警处置能力。

## 当前状态
- Phase 1-3 主链路已形成稳定基线：产品管理、设备管理、HTTP 上报、MQTT 上下行、协议解析、消息日志、最新属性、在线状态。
- Phase 4 当前已完成并进入真实环境验收基线的能力：告警中心、事件处置、风险点管理、阈值规则配置、联动规则、应急预案、分析报表、组织机构、用户、角色、菜单、区域、字典、通知渠道、审计日志。
- 当前代码基线还包含 `/risk-monitoring`、`/risk-monitoring-gis` 相关页面与接口，已完成代码落地，但按 [docs/19-phase4-progress.md](docs/19-phase4-progress.md) 仍待真实环境验收后再计入已交付范围。
- `spring-boot-iot-admin` 是唯一启动模块。
- 当前父 `pom.xml` 激活 11 个模块：
  - `spring-boot-iot-common`
  - `spring-boot-iot-framework`
  - `spring-boot-iot-auth`
  - `spring-boot-iot-system`
  - `spring-boot-iot-device`
  - `spring-boot-iot-protocol`
  - `spring-boot-iot-message`
  - `spring-boot-iot-rule`
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
2. 如需初始化真实联调基础数据（含 IoT 主链路 + Phase 4 风险平台 + 系统管理核心样例），执行 `sql/init-data.sql`
3. 若是历史库升级（非新库初始化），按 [docs/03-database.md](docs/03-database.md) 执行 `sql/upgrade/` 增量脚本
4. 启动后端：
   ```bash
   mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev
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
  mvn -s .mvn/settings.xml clean package -DskipTests
  ```
- 后端启动：
  ```bash
  mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev
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
- `start-backend-acceptance.ps1` 会先执行全量 `clean package -DskipTests`，再以 `dev` profile 启动可执行 jar。
- 自动化测试保留单元测试和协议回归测试，但不再使用旧 H2 端到端链路作为系统验收结论。
- 真实环境不可用时，只能记录为环境阻塞，不回退到旧验收路径替代验收。

## 当前核心接口
### IoT 基础
- `POST /api/device/product/add`
- `GET /api/device/product/{id}`
- `POST /api/device/add`
- `GET /api/device/{id}`
- `GET /api/device/code/{deviceCode}`
- `POST /message/http/report`
- `GET /api/device/{deviceCode}/properties`
- `GET /api/device/{deviceCode}/message-logs`
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
- `/api/menu/*`
- `/api/region/*`
- `/api/dict/*`
- `/api/system/channel/*`
- `/api/system/audit-log/*`

完整清单见：
- [docs/04-api.md](docs/04-api.md)
- [docs/21-business-functions-and-acceptance.md](docs/21-business-functions-and-acceptance.md)

## 前端说明
前端目录：`spring-boot-iot-ui`

当前要求：
- Node `>=24.0.0`
- 推荐优先使用 `spring-boot-iot-ui/.nvmrc`
- 默认通过 Vite 代理访问 `http://127.0.0.1:9999`（可通过 `VITE_PROXY_TARGET` 覆盖）
- 当前不再保留旧前端自动化验收入口，仅保留 `vitest` 与真实环境页面验收

## 已知说明
- `iot_device_message_log` 仍是当前物理表名；`iot_message_log` 作为兼容视图供文档和新功能统一命名使用，详见 [docs/03-database.md](docs/03-database.md)
- `sql/init.sql` 已整合当前业务基线全量表结构（IoT 主链路 + 风险平台 + 系统管理），新库初始化不再要求先跑 `sql/upgrade/`。
- 共享开发库若处于早期 Phase 4 半升级状态，风险监测联调前必须执行 `sql/upgrade/20260316_phase4_task3_risk_monitoring_schema_sync.sql`，否则 `/api/risk-monitoring/*` 会因缺少 `risk_point_device` 表、关键列，或旧版 `risk_point` 字段而阻塞
- 若真实库存在历史字段约束偏差（例如 `rule_definition.rule_code` / `linkage_rule.rule_code` / `emergency_plan.plan_code` 的旧版非空默认值），先执行 `sql/upgrade/20260316_phase4_real_env_schema_alignment.sql` 或运行 `python scripts/run-real-env-schema-sync.py`，再进行业务冒烟
- 若执行 `20260316_phase4_task10_dynamic_menu_auth.sql` 时出现 `1364 - Field 'menu_type' doesn't have a default`，请确认先执行了 `20260316_phase4_real_env_schema_alignment.sql`；当前仓库版本的 task10 脚本也已内置 `type/menu_type` 兼容处理。
- `DeviceMessageServiceImplTest` 在部分 JDK 17 环境下可能因 Mockito inline mock maker 无法自附加 ByteBuddy agent 而失败，这属于本地测试环境限制，不直接视为业务回归
- `ReportServiceImpl` 已对接 `iot_alarm_record`、`iot_event_record`、`iot_device` 的真实聚合；当前设备健康口径基于 `online_status` 与 `last_report_time`，尚未扩展到更细的遥测健康评分模型

## 文档导航
- [docs/03-database.md](docs/03-database.md)
- [docs/04-api.md](docs/04-api.md)
- [docs/05-protocol.md](docs/05-protocol.md)
- [docs/14-mqttx-live-runbook.md](docs/14-mqttx-live-runbook.md)
- [docs/19-phase4-progress.md](docs/19-phase4-progress.md)
- [docs/20-phase5-roadmap.md](docs/20-phase5-roadmap.md)
- [docs/21-business-functions-and-acceptance.md](docs/21-business-functions-and-acceptance.md)
- [docs/test-scenarios.md](docs/test-scenarios.md)

## 认证与访问控制（2026-03-16）
- 新增登录接口：`POST /api/auth/login`
- 新增当前用户接口：`GET /api/auth/me`
- 新增菜单树接口：`GET /api/menu/tree`
- 新增菜单管理接口：`GET /api/menu/list`、`GET /api/menu/{id}`、`POST /api/menu/add`、`PUT /api/menu/update`、`DELETE /api/menu/{id}`
- 前端新增独立登录页：`/login`
- 登录页支持账号密码登录、手机号登录，并提供微信扫码入口位
- 平台首页 `/` 允许游客访问；其余受保护页面未登录会跳转到 `/login`
- 除公共白名单外，后端接口默认要求携带 `Authorization: Bearer <token>`
- 当前公共接口：`/api/auth/login`、`/message/http/report`、`/api/cockpit/**`、`/actuator/**`、`/doc.html`、`/swagger-ui/**`、`/v3/api-docs/**`
- 前端已接入 token 持久化、`authContext` 恢复与请求头自动注入；当接口返回 `401` 时会清理本地登录态并跳回 `/login`
- 壳层菜单、左侧导航与按钮权限均已改为从 MySQL 动态读取，不再使用前端硬编码角色菜单
- 当前基础角色分为：`业务人员`、`管理人员`、`运维人员`、`开发人员`、`超级管理人员`
- 当前共享环境下，手机号登录复用系统密码；微信扫码入口当前仅完成页面接入位，待后续接入真实微信开放平台
- 详细接口定义见 `docs/04-api.md`，详细验收步骤见 `docs/test-scenarios.md`

## 真实环境业务冒烟脚本

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-business-function-smoke.ps1 -BaseUrl http://127.0.0.1:9999
```

脚本会输出：
- `logs/acceptance/business-function-smoke-<timestamp>.json`
- `logs/acceptance/business-function-summary-<timestamp>.json`
- `logs/acceptance/business-function-report-<timestamp>.md`

最新一次全链路通过报告（2026-03-16 19:11:12）：
- `logs/acceptance/business-function-smoke-20260316191059.json`
- `logs/acceptance/business-function-summary-20260316191059.json`
- `logs/acceptance/business-function-report-20260316191059.md`
