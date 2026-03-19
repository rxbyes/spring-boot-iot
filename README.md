# spring-boot-iot

## 项目简介
spring-boot-iot 是一个基于 Spring Boot 4 + Java 17 的模块化单体 IoT 网关平台，覆盖设备接入、协议适配、消息处理、设备资产管理，以及 Phase 4 风险运营能力。当前前端默认按 `接入智维 / 风险运营 / 风险策略 / 平台治理 / 质量工场` 五大工作台组织能力入口。

## 当前状态
- Phase 1-3 主链路已形成稳定基线：产品定义、设备资产、HTTP 上报、MQTT 上下行、协议解析、消息日志、最新属性、在线状态。
- Phase 4 当前已完成并进入真实环境验收基线的能力：告警运营台、事件协同台、风险对象中心、阈值策略、联动编排、应急预案库、运营分析中心，以及平台治理下的组织架构、账号中心、角色权限、导航编排、区域版图、数据字典、通知编排、审计中心；接入智维分区已补齐“异常观测台”和“链路追踪台”，用于按 `TraceId`、设备编码、产品标识与 Topic 查看 `system_error` 后台异常和接入链路问题。
- 2026-03-19 起，角色默认首页已按职责分化：业务/管理优先进入 `风险运营`，运维/研发优先进入 `接入智维`，超级管理员优先进入 `平台治理`。
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

## 核心文档入口（推荐）

为降低二次开发与 GPT 上下文加载成本，默认先读以下最小集：

1. [docs/README.md](docs/README.md)
2. [docs/01-系统概览与架构说明.md](docs/01-系统概览与架构说明.md)
3. [docs/02-业务功能与流程说明.md](docs/02-业务功能与流程说明.md)
4. [docs/03-接口规范与接口清单.md](docs/03-接口规范与接口清单.md)
5. [docs/04-数据库设计与初始化数据.md](docs/04-数据库设计与初始化数据.md)
6. [docs/07-部署运行与配置说明.md](docs/07-部署运行与配置说明.md)

按任务补充阅读：
- 测试与验收：`docs/05-自动化测试与质量保障.md`、`docs/test-scenarios.md`、`docs/21-business-functions-and-acceptance.md`
- 前端开发：`docs/06-前端开发与CSS规范.md`、`docs/15-frontend-optimization-plan.md`
- MQTT / 协议：`docs/05-protocol.md`、`docs/14-mqttx-live-runbook.md`
- Phase 4 交付边界：`docs/19-phase4-progress.md`、`docs/21-business-functions-and-acceptance.md`
- 历史冲突与技术债：`docs/08-变更记录与技术债清单.md`

如需把项目直接交给 GPT / Codex 类模型，可优先使用：
- `docs/09-GPT接管提示模板.md`

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
- `IOT_OBSERVABILITY_SYSTEM_ERROR_NOTIFY_ENABLED`
- `IOT_OBSERVABILITY_NOTIFICATION_TIMEOUT_MS`
- `IOT_OBSERVABILITY_SYSTEM_ERROR_NOTIFY_COOLDOWN_SECONDS`

## 系统异常审计与通知
- MQTT 启动失败、订阅失败、连接断开、消息分发失败等后台异步异常会自动写入 `sys_audit_log`，其中 `operation_type=system_error`、`request_method=MQTT`、`user_name=SYSTEM`。
- `/api/**` 与 `/message/**` 入口会统一生成或透传 `X-Trace-Id`，并同步写入 `sys_audit_log.trace_id` 与 `iot_device_message_log.trace_id`，用于跨 HTTP、MQTT、系统日志和消息日志串联排查。
- `system_error` 审计记录会补充 `trace_id`、`device_code`、`product_key`、`error_code`、`exception_class`，消息日志会补充 `trace_id`、`device_code`、`product_key`。
- 2026-03-17 起，Spring Boot 4 运行时的 JSON 兼容层统一按 Jackson 3 处理：`@RequestBody` 反序列化、自定义 `Long -> String` 序列化与系统日志 JSON 解析均使用 `JsonMapperBuilderCustomizer` / `JsonMapper`，`JsonFormat` 等注解仍沿用 `com.fasterxml.jackson.annotation`。
- 若共享开发库中的 `sys_audit_log` 仍缺少 `trace_id`、`device_code`、`product_key`、`error_code`、`exception_class` 等增强列，当前服务会按真实存在列动态降级写入和查询，避免角色管理、菜单管理等系统治理操作因审计写入失败而回滚；建议仍执行 `sql/upgrade/20260316_phase4_real_env_schema_alignment.sql` 以恢复完整检索能力。
- 如需开启系统异常自动通知，设置 `IOT_OBSERVABILITY_SYSTEM_ERROR_NOTIFY_ENABLED=true`。
- 当前自动通知支持 `webhook`、`wechat`、`feishu`、`dingtalk` 渠道类型；渠道 `config` JSON 至少需要 `url`，自动系统异常通知还要求 `scenes` 中包含 `system_error`。
- 可通过 `POST /api/system/channel/test/{channelCode}` 先发送测试消息，再触发 MQTT 异常验证审计与通知联动。

当前基线已丢弃以下旧路径，不再作为验收或回归标准：
- 旧 H2 验收 profile
- 独立 H2 schema 验收脚本
- H2 专用端到端验证链路
- 旧前端自动化验收链路

## 快速开始
1. 初始化数据库：执行 [sql/init.sql](sql/init.sql)
2. 如需初始化真实联调基础数据（含 IoT 主链路 + Phase 4 风险平台 + 系统管理核心样例），执行 `sql/init-data.sql`
   初始化后默认可用演示账号：`admin`、`biz_demo`、`manager_demo`、`ops_demo`、`dev_demo`，初始化默认密码均为 `123456`，角色说明见 [docs/04-数据库设计与初始化数据.md](docs/04-数据库设计与初始化数据.md)
3. 若是历史库升级（非新库初始化），按 [docs/04-数据库设计与初始化数据.md](docs/04-数据库设计与初始化数据.md) 执行 `sql/upgrade/` 增量脚本；菜单结构升级到五工作台时，需执行 `sql/upgrade/20260319_phase5_workspace_menu_refactor.sql`
   若共享开发库中的演示账号、角色名称或新补齐按钮菜单出现 `????` / `?????`，执行 `sql/upgrade/20260319_phase5_demo_text_repair.sql` 修复中文文本
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
- 上述 PowerShell 脚本已改为根据脚本自身位置自动定位仓库根目录，兼容 Windows 10 共享环境（常见路径示例：`E:\idea\ghatg\spring-boot-iot`）与其他本地工作区路径；脚本实现本身不应重新写死绝对路径。
- 自动化测试：
  ```bash
  mvn test
  ```
- 浏览器自动巡检：
  ```bash
  cd spring-boot-iot-ui
  npm run acceptance:browser
  ```
- 浏览器自动巡检计划预览：
  ```bash
  cd spring-boot-iot-ui
  npm run acceptance:browser:plan
  ```
- 配置驱动浏览器巡检：
  ```bash
  node scripts/auto/run-browser-acceptance.mjs --plan=config/automation/sample-web-smoke-plan.json --dry-run
  ```
- 配置驱动视觉基线刷新：
  ```bash
  node scripts/auto/run-browser-acceptance.mjs --plan=config/automation/sample-web-smoke-plan.json --update-baseline
  ```
- 视觉基线治理（离线审计 / 批量提升）：
  ```bash
  node scripts/auto/manage-visual-baselines.mjs --input=logs/acceptance/config-browser-visual-manifest-<timestamp>.json
  ```

说明：
- `start-backend-acceptance.ps1` 会先执行全量 `clean package -DskipTests`，再以 `dev` profile 启动可执行 jar。
- `start-backend-acceptance.ps1`、`start-frontend-acceptance.ps1`、`run-business-function-smoke.ps1`、`run-business-function-browser.ps1` 当前均按脚本目录自动推导工作区路径。
- 自动化测试保留单元测试和协议回归测试，但不再使用旧 H2 端到端链路作为系统验收结论。
- 浏览器自动巡检脚本现统一维护在 `scripts/auto/`，`spring-boot-iot-ui/scripts/business-browser-acceptance.mjs` 仅保留兼容包装入口。
- 新增配置驱动自动化工场：前端路由 `/automation-test` 可维护场景模板、步骤、接口断言并导出 JSON 计划。
- 自动化工场第二阶段已补齐“页面盘点 + 覆盖分析 + 一键脚手架生成”，可优先按当前授权菜单自动识别页面，并支持手工登记外部系统页面。
- 自动化工场第三阶段已补齐“插件式步骤注册 + 高级动作编排”，内置支持 `setChecked`、`uploadFile`、`tableRowAction`、`dialogAction` 等复杂页面动作。
- 自动化工场第四阶段已补齐“截图基线 + 视觉回归 + 失败截图对比报告”，前端计划支持配置 `baselineDir`、`assertScreenshot`、`screenshotTarget`、`threshold`、`fullPage`，执行器支持 `--update-baseline` 刷新基线。
- `scripts/auto/run-browser-acceptance.mjs` 新增 `--plan=...` 模式，可直接执行自动化工场导出的 JSON 计划，支持面向任意 Web 系统接入。
- 配置驱动回归报告现已额外输出 `config-browser-visual-manifest-<timestamp>.json`、`config-browser-visual-index-<timestamp>.html`、`config-browser-visual-failures-<timestamp>.html`，并汇总视觉断言、缺失基线提示、baseline / actual / diff 截图路径与差异比例，便于失败定位和回归分析。
- 新增 `scripts/auto/manage-visual-baselines.mjs` 视觉基线治理命令，支持读取 visual manifest 或 results JSON，按 `passed / mismatch / missing / updated` 筛选记录，输出治理 JSON / Markdown 报告，并在 `--mode=promote --apply` 下批量提升基线。
- `spring-boot-iot-ui/package.json` 已补充 `npm run acceptance:browser:update-baseline` 与 `npm run acceptance:browser:baseline:manage`，便于从前端工作目录直接执行基线刷新和治理。
- 正式执行 `npm run acceptance:browser` 时，会生成 `logs/acceptance/business-browser-*` 结果文件，并把失败问题自动追加到 `docs/22-automation-test-issues-20260316.md`。
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
- `GET /api/device/message-trace/page`
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
- [docs/03-接口规范与接口清单.md](docs/03-接口规范与接口清单.md)
- [docs/21-business-functions-and-acceptance.md](docs/21-business-functions-and-acceptance.md)

## 前端说明
前端目录：`spring-boot-iot-ui`

当前要求：
- Node `>=24.0.0`
- 推荐优先使用 `spring-boot-iot-ui/.nvmrc`
- 默认通过 Vite 代理访问 `http://127.0.0.1:9999`（可通过 `VITE_PROXY_TARGET` 覆盖）
- `dev` 配置默认放行 `localhost`、`127.0.0.1` 以及常见内网网段（`10.*`、`172.*`、`192.168.*`）的 Vite 开发来源，便于通过局域网地址访问前端并完成登录调试
- 当前不再保留旧前端自动化验收入口，仅保留 `vitest` 与真实环境页面验收
- 当前浏览器自动巡检主入口为 `scripts/auto/run-browser-acceptance.mjs`，支持按 `delivery`、`baseline` 两类场景执行，并预留未来功能巡检清单
- 当前前端一级导航已统一为 `接入智维 / 风险运营 / 风险策略 / 平台治理 / 质量工场` 五类工作台。
- 当前质量工场新增 `/automation-test` 自动化工场，用于前端配置场景模板并导出配置驱动浏览器巡检计划；当前已支持截图基线路径配置、页面/局部截图断言与视觉回归参数配置
- 当前公共壳层已统一为阿里云式浅色控制台布局：顶部采用“品牌 / 搜索 / 工具区 + 一级业务分区条”，左侧采用“分组摘要 + 二级菜单”，内容区采用标准页头与状态卡

## 已知说明
- `iot_device_message_log` 仍是当前物理表名；`iot_message_log` 作为兼容视图供文档和新功能统一命名使用，详见 [docs/04-数据库设计与初始化数据.md](docs/04-数据库设计与初始化数据.md)
- `sql/init.sql` 已整合当前业务基线全量表结构（IoT 主链路 + 风险平台 + 系统管理），新库初始化不再要求先跑 `sql/upgrade/`。
- 共享开发库若处于早期 Phase 4 半升级状态，风险监测联调前必须执行 `sql/upgrade/20260316_phase4_task3_risk_monitoring_schema_sync.sql`，否则 `/api/risk-monitoring/*` 会因缺少 `risk_point_device` 表、关键列，或旧版 `risk_point` 字段而阻塞
- 若真实库存在历史字段约束偏差（例如 `rule_definition.rule_code` / `linkage_rule.rule_code` / `emergency_plan.plan_code` 的旧版非空默认值），先执行 `sql/upgrade/20260316_phase4_real_env_schema_alignment.sql` 或运行 `python scripts/run-real-env-schema-sync.py`，再进行业务冒烟
- 若执行 `20260316_phase4_task10_dynamic_menu_auth.sql` 时出现 `1364 - Field 'menu_type' doesn't have a default`，请确认先执行了 `20260316_phase4_real_env_schema_alignment.sql`；当前仓库版本的 task10 脚本也已内置 `type/menu_type` 兼容处理。
- 若历史库里 `SUPER_ADMIN` 的真实主键不是 `92000005`，请使用当前仓库版本的 `20260316_phase4_task10_dynamic_menu_auth.sql`；若需把旧三大分区菜单升级为五工作台，请低峰执行 `sql/upgrade/20260319_phase5_workspace_menu_refactor.sql`；若仅需补齐导航编排按钮权限，可低峰执行 `sql/upgrade/20260317_phase4_menu_button_permission_backfill.sql`
- 若 Windows 终端链路曾把中文初始化数据写成 `????`，请执行 `sql/upgrade/20260319_phase5_demo_text_repair.sql`；该脚本使用 `SET NAMES utf8mb4` 与 utf8mb4 十六进制字面量回写中文，避免再次被终端编码污染
- `DeviceMessageServiceImplTest` 在部分 JDK 17 环境下可能因 Mockito inline mock maker 无法自附加 ByteBuddy agent 而失败，这属于本地测试环境限制，不直接视为业务回归
- `ReportServiceImpl` 已对接 `iot_alarm_record`、`iot_event_record`、`iot_device` 的真实聚合；当前设备健康口径基于 `online_status` 与 `last_report_time`，尚未扩展到更细的遥测健康评分模型

## 文档导航
- [docs/README.md](docs/README.md)
- [docs/01-系统概览与架构说明.md](docs/01-系统概览与架构说明.md)
- [docs/02-业务功能与流程说明.md](docs/02-业务功能与流程说明.md)
- [docs/03-接口规范与接口清单.md](docs/03-接口规范与接口清单.md)
- [docs/04-数据库设计与初始化数据.md](docs/04-数据库设计与初始化数据.md)
- [docs/05-自动化测试与质量保障.md](docs/05-自动化测试与质量保障.md)
- [docs/06-前端开发与CSS规范.md](docs/06-前端开发与CSS规范.md)
- [docs/07-部署运行与配置说明.md](docs/07-部署运行与配置说明.md)
- [docs/08-变更记录与技术债清单.md](docs/08-变更记录与技术债清单.md)
- [docs/09-GPT接管提示模板.md](docs/09-GPT接管提示模板.md)
- [docs/05-protocol.md](docs/05-protocol.md)
- [docs/14-mqttx-live-runbook.md](docs/14-mqttx-live-runbook.md)
- [docs/19-phase4-progress.md](docs/19-phase4-progress.md)
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
- 角色权限页已补齐菜单/按钮授权树，导航编排页负责维护结构并可直接跳转到角色授权
- 账号中心、角色权限、导航编排页的关键操作按钮已统一接入 `v-permission`，按数据库按钮权限码收口；导航编排页“前往角色授权”入口要求 `system:role:update`
- 当前基础角色分为：`业务人员`、`管理人员`、`运维人员`、`开发人员`、`超级管理人员`
- 当前共享环境下，手机号登录复用系统密码；微信扫码入口当前仅完成页面接入位，待后续接入真实微信开放平台
- 详细接口定义见 `docs/03-接口规范与接口清单.md`，详细验收步骤见 `docs/test-scenarios.md`
- `sql/init-data.sql` 默认补齐 5 个演示账号：`admin`、`biz_demo`、`manager_demo`、`ops_demo`、`dev_demo`，便于按角色快速登录演示五大工作台

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
