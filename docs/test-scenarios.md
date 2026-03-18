# 真实环境测试与验收手册

更新时间：2026-03-18

## 1. 当前测试策略
当前项目统一采用“自动化回归 + 真实环境验收”双轨策略：
- 自动化测试继续保留单元测试、协议测试、部分服务测试，用于发现代码级回归。
- 系统功能是否通过验收，以 `spring-boot-iot-admin/src/main/resources/application-dev.yml` 对应的真实环境联调结果为准。
- 不再使用旧 H2 验收 profile、旧 schema 脚本、H2 内存库或旧前端自动化链路作为系统验收基线。

## 2. 保留的自动化测试
可继续使用的自动化测试包括：
- `mvn test`
- `mvn -pl spring-boot-iot-device test -DskipTests=false`
- `mvn -pl spring-boot-iot-protocol test -DskipTests=false -Dtest=MqttPayloadSecurityValidatorTest -Dsurefire.failIfNoSpecifiedTests=false`
- `mvn -pl spring-boot-iot-protocol test -DskipTests=false -Dtest=MqttBinaryFormatParserTest -Dsurefire.failIfNoSpecifiedTests=false`
- `mvn -pl spring-boot-iot-protocol test -DskipTests=false -Dtest=MqttJsonProtocolAdapterTest -Dsurefire.failIfNoSpecifiedTests=false`
- `mvn -pl spring-boot-iot-protocol test -DskipTests=false -Dtest=MqttPayloadDecryptorRegistryTest -Dsurefire.failIfNoSpecifiedTests=false`
- `mvn -pl spring-boot-iot-admin -am test -DskipTests=false -Dtest=MqttDeviceAesDataTests -Dsurefire.failIfNoSpecifiedTests=false`

说明：
- 这些测试只作为代码回归信号，不替代真实环境验收结论。
- `DeviceMessageServiceImplTest` 在部分 JDK 17 环境中可能受 Mockito inline agent 限制影响，应单独记录，不直接视为业务回归。

## 3. 真实环境启动
### 3.1 后端启动
统一命令：
```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

可选脚本：
```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-backend-acceptance.ps1
```
该脚本会先执行全量 `mvn -s .mvn/settings.xml clean package -DskipTests`，再用 `dev` profile 启动后端。

### 3.2 前端启动
前置条件：
- Node `>=24.0.0`
- 推荐使用 `spring-boot-iot-ui/.nvmrc`

启动命令：
```bash
cd spring-boot-iot-ui
npm install
npm run acceptance:dev
```

可选脚本：
```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-frontend-acceptance.ps1
```

### 3.3 环境核对
`application-dev.yml` 当前默认连接：
- MySQL：`8.130.107.120:3306/rm_iot`
- TDengine：`8.130.107.120:6041/iot`
- Redis：`8.130.107.120:6379/8`
- MQTT：`tcp://8.130.107.120:1883`

验收前先确认：
- 新库：已执行 `sql/init.sql`（如需样例数据再执行 `sql/init-data.sql`）
- 历史库：按需执行 `sql/upgrade/` 当前基线脚本
- 风险监测联调前，额外确认已执行 `sql/upgrade/20260316_phase4_task3_risk_monitoring_schema_sync.sql`
- 如共享开发库存在历史 Phase 4 早期结构偏差（缺列、缺表、旧约束），额外执行 `sql/upgrade/20260316_phase4_real_env_schema_alignment.sql`
- 如历史库菜单管理页仍缺少超管按钮权限，可补执行 `sql/upgrade/20260317_phase4_menu_button_permission_backfill.sql`
- 使用自动同步方式时，可执行：`PYTHONPATH=.codex-runtime/pydeps python scripts/run-real-env-schema-sync.py`
- MQTT 客户端日志无异常
- 若本轮需要验证“系统异常自动通知”，额外设置 `IOT_OBSERVABILITY_SYSTEM_ERROR_NOTIFY_ENABLED=true`，并准备可接收请求的 webhook 渠道地址
- 前端代理默认指向 `http://127.0.0.1:9999`（可通过 `VITE_PROXY_TARGET` 覆盖）
- 若通过局域网地址访问 Vite 开发服务（如 `http://172.21.16.1:5174`），当前 `dev` 配置已默认放行 `10.*`、`172.*`、`192.168.*` 来源，登录接口不应再出现 `Invalid CORS request`
- 用户管理、角色管理、菜单管理页的操作按钮按 `authContext.permissions` 显示；若角色未授权对应 `system:*:*` 按钮权限，则仅保留页面级只读访问；菜单页“前往角色授权”入口需额外校验 `system:role:update`

## 4. HTTP 主链路真实环境验收
### 步骤 1：创建产品
```bash
curl -X POST http://localhost:9999/api/device/product/add \
  -H "Content-Type: application/json" \
  -d '{
    "productKey":"accept-http-product-01",
    "productName":"验收产品-HTTP-01",
    "protocolCode":"mqtt-json",
    "nodeType":1,
    "dataFormat":"JSON"
  }'
```

### 步骤 2：创建设备
```bash
curl -X POST http://localhost:9999/api/device/add \
  -H "Content-Type: application/json" \
  -d '{
    "productKey":"accept-http-product-01",
    "deviceName":"验收设备-HTTP-01",
    "deviceCode":"accept-http-device-01",
    "deviceSecret":"123456",
    "clientId":"accept-http-device-01",
    "username":"accept-http-device-01",
    "password":"123456"
  }'
```

### 步骤 3：发送 HTTP 上报
```bash
curl -X POST http://localhost:9999/message/http/report \
  -H "Content-Type: application/json" \
  -d '{
    "protocolCode":"mqtt-json",
    "productKey":"accept-http-product-01",
    "deviceCode":"accept-http-device-01",
    "payload":"{\"messageType\":\"property\",\"properties\":{\"temperature\":26.5,\"humidity\":68}}",
    "topic":"/sys/accept-http-product-01/accept-http-device-01/thing/property/post",
    "clientId":"accept-http-device-01",
    "tenantId":"1"
  }'
```

### 步骤 4：查询验证
```bash
curl http://localhost:9999/api/device/accept-http-device-01/properties
curl http://localhost:9999/api/device/accept-http-device-01/message-logs
curl http://localhost:9999/api/device/code/accept-http-device-01
```

### 通过标准
- 接口返回 `code = 200`
- `properties` 返回最新属性值
- `message-logs` 返回对应 topic 和 payload
- `iot_message_log` 有新增记录
- `iot_device.online_status = 1`
- `last_online_time` 与 `last_report_time` 已刷新

## 5. MQTT 标准 Topic 真实环境验收
### 步骤 1：启动后端并确保 MQTT 已连接
建议附加唯一 `clientId`：
```bash
IOT_MQTT_CLIENT_ID=accept-mqtt-consumer-001 \
mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

### 步骤 2：使用 MQTTX 建立连接
- Host：`8.130.107.120`
- Port：`1883`
- Username：`emqx`
- Password：读取 `application-dev.yml` 中 `iot.mqtt.password`
- Client ID：如 `mqttx-accept-001`

### 步骤 3：发送标准 Topic 消息
Topic：
```text
/sys/accept-http-product-01/accept-http-device-01/thing/property/post
```

Payload：
```json
{"messageType":"property","properties":{"temperature":28.1,"humidity":59}}
```

### 通过标准
- 应用日志可见 MQTT 收到消息并进入统一主链路
- `GET /api/device/accept-http-device-01/properties` 可见最新值
- `GET /api/device/accept-http-device-01/message-logs` 返回标准 topic 记录
- `iot_device.online_status = 1`
- Redis 会话 `iot:device:session:accept-http-device-01` 刷新 `lastSeenTime`

## 6. MQTT 历史 `$dp` 真实环境验收
### 6.1 明文 JSON
Topic：`$dp`

Payload：
```json
{"accept-http-device-01":{"temperature":25.1,"humidity":61}}
```

### 6.2 嵌套遥测 JSON
```json
{"100054920":{"L1_QJ_1":{"2026-03-14T07:04:03.000Z":{"X":3.15,"Y":-5.14,"Z":83.97,"angle":-6.03}},"L1_JS_1":{"2026-03-14T07:04:03.000Z":{"gX":-0.04,"gY":0.18,"gZ":-0.04}}}}
```

### 6.3 加密包裹 JSON
按 [docs/05-protocol.md](05-protocol.md) 中 `header.appId + bodies.body` 格式发送，`appId` 使用 `62000001`。

### 通过标准
- `$dp` 报文可进入主链路
- `iot_message_log` 新增 `$dp` 记录
- 拍平后的属性进入 `iot_device_property`
- 设备在线状态刷新
- AES / DES / 3DES 兼容问题优先通过协议测试和运行日志定位

## 7. MQTT 下行最小发布验收
### 步骤 1：订阅设备下行 Topic
在 MQTTX 订阅：
```text
/sys/accept-http-product-01/accept-http-device-01/thing/property/set
```

### 步骤 2：调用下行发布接口
```bash
curl -X POST http://localhost:9999/message/mqtt/down/publish \
  -H "Content-Type: application/json" \
  -d '{
    "productKey":"accept-http-product-01",
    "deviceCode":"accept-http-device-01",
    "qos":1,
    "commandType":"property",
    "params":{
      "switch":1,
      "targetTemperature":23.0,
      "requestId":"accept-down-001"
    }
  }'
```

### 通过标准
- 发布接口返回 `code = 200`
- 返回体包含自动生成的推荐 topic
- MQTTX 订阅端能收到 JSON 下行消息
- 当前不要求设备 ACK，但需确认消息已实际送达 Broker

## 8. Phase 4 模块真实环境验收
Phase 4 统一按页面、接口、数据表三层核对：
- 页面/路由
- 关键接口
- 数据库落库结果

详细矩阵与 SQL 模板见：
- [docs/21-business-functions-and-acceptance.md](21-business-functions-and-acceptance.md)
- [docs/19-phase4-progress.md](19-phase4-progress.md)

本轮优先验收模块：
- 告警中心
- 事件处置
- 风险点管理
- 阈值规则配置
- 联动规则
- 应急预案
- 分析报表（当前以接口连通和页面可访问为主）
- 组织、用户、角色、区域、字典、通知渠道、业务日志
- 风险监测实时监测、GIS 风险态势（代码已完成；2026-03-16 已确认共享开发库仍需先执行 `20260316_phase4_task3_risk_monitoring_schema_sync.sql`，完成后再进行真实环境复验）

### 8.1 系统异常自动通知验证（2026-03-17）

前置条件：
- 已配置并启用一个 `webhook` / `wechat` / `feishu` / `dingtalk` 渠道。
- 渠道 `config` 至少包含 `url`，自动系统异常通知验证时需包含 `scenes:["system_error"]`。
- 应用已通过环境变量或 `application-dev.yml` 覆盖启用 `iot.observability.system-error-notify-enabled=true`。

建议步骤：
1. 先调用 `POST /api/system/channel/test/{channelCode}`，确认渠道测试消息可以成功送达。
2. 使用 MQTTX 向不存在设备的 topic 发送消息，例如 `/sys/demo-product/demo-device-02/thing/property/post`。
3. 观察应用日志中是否出现 `设备不存在: demo-device-02` 一类后台异常。
4. 查询 `/system-log` 页面或 `sys_audit_log` 表，确认新增 `operation_type=system_error`、`request_method=MQTT`、`user_name=SYSTEM` 的审计记录，并检查 `trace_id`、`device_code`、`product_key`、`exception_class` 等字段。
5. 打开 `/message-trace` 页面，带入第 4 步中的 `trace_id` 或设备编码，确认可查询到对应消息链路。
6. 检查 webhook 接收端或群机器人，确认收到系统异常通知消息。

通过标准：
- `POST /api/system/channel/test/{channelCode}` 返回 `code=200`，且接收端收到测试通知。
- MQTT 异常触发后，`sys_audit_log` 新增对应 `system_error` 记录。
- `sys_audit_log.trace_id` 与 `/message-trace` 页查询到的 `iot_message_log.trace_id` 可对应起来。
- 当渠道配置包含 `scenes:["system_error"]` 且应用开关开启时，接收端能收到自动系统异常通知。

### 8.2 日志能力最小验收清单（2026-03-18）

目标：
1. 业务人员可审计：业务日志可按成功/失败筛选并看到统计概览。
2. 开发人员可排障：系统日志可按异常维度筛选并联动消息追踪定位后端链路问题。

前置条件：
- 已登录系统并具备日志查看权限。
- 后端已启用真实环境配置并可访问数据库。

步骤 A（业务审计）：
1. 打开 `/audit-log` 页面，确认页面可正常加载列表。
2. 在筛选项选择“操作结果=失败”，点击查询。
3. 观察页面统计概览：应显示总量、今日、成功/失败、活跃用户。
4. 调用 `GET /api/system/audit-log/business/stats`（可带同样筛选参数），核对页面概览与接口结果一致。
5. 点开任一详情，确认请求/响应摘要可查看且敏感字段脱敏。

步骤 B（研发排障）：
1. 打开 `/system-log` 页面，确认仅展示 `operation_type=system_error` 记录。
2. 使用 `TraceId`、设备编码、产品标识、异常编码、异常类型、操作结果组合筛选。
3. 观察页面统计概览：应显示异常总量、今日、MQTT、链路数、高频模块。
4. 调用 `GET /api/system/audit-log/system-error/stats`（带同样筛选参数），核对页面概览与接口结果一致。
5. 选中一条系统异常，点击“追踪”跳转 `/message-trace`，确认可按 `traceId` 或设备维度查到对应消息链路。

通过标准：
- `/audit-log` 与 `/system-log` 的“操作结果”筛选都生效，并影响列表与统计概览。
- 业务统计接口 `GET /api/system/audit-log/business/stats` 返回 `code=200` 且字段完整。
- 系统统计接口 `GET /api/system/audit-log/system-error/stats` 返回 `code=200` 且字段完整。
- 系统日志到消息追踪的跳转可复现实例链路（TraceId 可串联 `sys_audit_log` 与 `iot_message_log`）。

## 9. 验收产物要求
每次真实环境验收至少保留以下产物：
- 启动命令或脚本记录
- HTTP 请求与响应记录
- MQTTX 发布 / 订阅截图
- 页面操作截图
- 关键 SQL 查询结果截图或导出
- 验收结论表：通过 / 不通过 / 待确认

## 10. 环境不可用时的处理原则
- 若共享开发库仍使用旧版 `sys_audit_log` 结构，继续联调时需额外确认：角色管理、菜单管理、用户管理的新增/更新请求不会再因审计表缺列返回 `500`；如需完整的 `trace_id`、设备编码、产品标识、异常编码检索能力，再执行 `sql/upgrade/20260316_phase4_real_env_schema_alignment.sql`。
- 先确认是网络、数据库、Redis、MQTT 哪一层阻塞
- 若是 MQTT 链路异常或消息未落库，补查 `/system-log` 页面或 `sys_audit_log` 表中的 `operation_type=system_error` 记录，并进一步通过 `/message-trace` 或 `iot_message_log` 按 `trace_id` / 设备编码 / Topic 回溯原始消息
- 记录具体报错、时间、影响范围
- 可继续执行 `mvn -s .mvn/settings.xml clean package -DskipTests`、`mvn -s .mvn/settings.xml test` 作为代码回归检查
- 不允许回退到旧 H2 验收配置、H2 内存库、旧前端自动化链路或历史验收用例来宣布“验收通过”

## 11. 登录与鉴权冒烟（真实环境）

### 页面前置核对
- 前端访问 `/login` 时，应显示独立登录页，而不是工作台内嵌登录表单。
- 页面应提供：微信扫码区、账号密码登录、手机号登录。
- 未登录可直接访问 `/` 首页；访问受保护页面时才跳转到 `/login`。
- 当前共享环境下，微信扫码区只验视觉入口与提示文案，不验真实扫码回调。

### 步骤 1：登录并获取 token
```bash
curl -X POST http://localhost:9999/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

通过标准：
- HTTP 状态码为 `200`
- 响应体 `code = 200`
- `data.token` 非空
- `data.authContext` 非空，且包含 `roles`、`menus`、`permissions`

### 步骤 2：手机号登录并获取 token
```bash
curl -X POST http://localhost:9999/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":"phone","phone":"13800138000","password":"123456"}'
```

通过标准：
- HTTP 状态码为 `200`
- 响应体 `code = 200`
- 响应体返回当前用户 `authContext`

### 步骤 4.1：验证菜单树接口
```bash
curl http://localhost:9999/api/menu/tree \
  -H "Authorization: Bearer <token>"
```

通过标准：
- HTTP 状态码为 `200`
- 响应体 `code = 200`
- `data` 为非空树结构，且包含 `meta`、`type`、`children`
- `data.token` 非空

说明：
- 手机号需与 `sys_user.phone` 一致。
- 当前共享环境手机号登录复用系统密码，不校验短信验证码。

### 步骤 3：无 token 访问受保护接口
```bash
curl http://localhost:9999/api/auth/me
```

通过标准：
- HTTP 状态码为 `401`

### 步骤 4：携带 token 访问受保护接口
```bash
curl http://localhost:9999/api/auth/me \
  -H "Authorization: Bearer <token>"
```

通过标准：
- HTTP 状态码为 `200`
- 响应体 `code = 200`

### 步骤 5：验证设备管理接口鉴权
```bash
curl http://localhost:9999/api/device/code/accept-http-device-01
curl http://localhost:9999/api/device/code/accept-http-device-01 \
  -H "Authorization: Bearer <token>"
```

通过标准：
- 不带 token 返回 `401`
- 带 token 返回非 `401`（有数据时 `200`）
- 前端登录态失效时，应清理本地 token 并跳回 `/login`

## 12. 业务功能自动冒烟脚本（真实环境）

执行命令：
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-business-function-smoke.ps1 -BaseUrl http://127.0.0.1:9999
```

输出产物：
1. `logs/acceptance/business-function-smoke-<timestamp>.json`
2. `logs/acceptance/business-function-summary-<timestamp>.json`
3. `logs/acceptance/business-function-report-<timestamp>.md`

最新全链路通过基线（2026-03-16 19:11:12）：
1. `logs/acceptance/business-function-smoke-20260316191059.json`
2. `logs/acceptance/business-function-summary-20260316191059.json`
3. `logs/acceptance/business-function-report-20260316191059.md`

说明：
1. 脚本会先调用登录接口获取 token，再对业务接口进行自动验收。
2. 脚本结果可直接回填到 `docs/21-business-functions-and-acceptance.md` 的打勾清单。
3. 若大量接口返回 `500`，优先检查真实库 schema 是否与当前代码一致；必要时先执行 `sql/upgrade/20260316_phase4_real_env_schema_alignment.sql` 或 `python scripts/run-real-env-schema-sync.py` 后再复测。

## 13. 浏览器自动巡检脚本（真实环境）

主入口位置：
- `scripts/auto/run-browser-acceptance.mjs`
- `spring-boot-iot-ui/scripts/business-browser-acceptance.mjs` 为兼容包装器

执行命令：
```bash
cd spring-boot-iot-ui
npm run acceptance:browser
```

计划预览（不启动浏览器）：
```bash
cd spring-boot-iot-ui
npm run acceptance:browser:plan
```

当前脚本能力：
- 按 `delivery`、`baseline` 两类场景分组执行现有功能浏览器巡检。
- 在脚本内部预留未来功能巡检清单，便于后续开发完成后直接纳管。
- 支持 `--plan=...` 加载配置驱动 JSON 计划，将浏览器巡检能力扩展到任意带 Web 界面的业务系统。
- 支持 `--update-baseline` 首次生成或刷新截图基线，适用于页面样式或组件视觉基准调整后的批量更新。
- 配置驱动计划支持 `target.baselineDir` 指定截图基线目录，默认示例为 `config/automation/baselines`。
- 配置驱动步骤支持 `assertScreenshot`，可对整页或 locator 局部区域执行视觉断言，并配置 `baselineName`、`threshold`、`fullPage`。
- 统一输出 `logs/acceptance/business-browser-summary-<timestamp>.json`
- 统一输出 `logs/acceptance/business-browser-results-<timestamp>.json`
- 统一输出 `logs/acceptance/business-browser-report-<timestamp>.md`
- 统一输出 `logs/acceptance/business-browser-screenshots-<timestamp>/`
- 配置驱动视觉断言额外输出 `logs/acceptance/config-browser-visual-manifest-<timestamp>.json`
- 配置驱动视觉断言额外输出 `logs/acceptance/config-browser-visual-index-<timestamp>.html`
- 配置驱动视觉断言额外输出 `logs/acceptance/config-browser-visual-failures-<timestamp>.html`
- 视觉回归失败时，报告会附带 baseline / actual / diff 图片路径、差异像素与差异比例，便于排查页面变更与样式回归。

问题记录规则：
- 正式执行 `npm run acceptance:browser` 后，脚本会默认把本轮失败问题追加到 `docs/22-automation-test-issues-20260316.md`
- 如仅需生成结果文件、不追加问题文档，可执行：`node scripts/auto/run-browser-acceptance.mjs --no-append-issues`
- 默认仅 `delivery` 场景失败会返回非零退出码；如需扩大阻断范围，可使用 `--fail-scopes=delivery,baseline`

### 13.1 配置驱动自动化测试中心

前端入口：
- 路由：`/automation-test`
- 页面：`spring-boot-iot-ui/src/views/AutomationTestCenterView.vue`

能力说明：
- 通过前端维护目标系统地址、登录信息、场景模板、页面步骤、接口 matcher、变量捕获与页面断言。
- 支持按当前登录用户的授权菜单自动盘点页面覆盖面，并计算“已覆盖 / 待补齐”页面。
- 支持把盘点结果一键生成页面冒烟脚手架；对外部系统可通过“新增自定义页面”补充页面清单后再生成脚手架。
- 执行器已升级为步骤注册中心，当前内置支持 `setChecked`、`uploadFile`、`tableRowAction`、`dialogAction` 等高频复杂动作，可继续扩展更多插件式步骤。
- 计划目标已支持维护 `baselineDir`，步骤已支持 `assertScreenshot`、`screenshotTarget`、`baselineName`、`threshold`、`fullPage`，可直接在前端完成视觉基线编排。
- 示例计划 `config/automation/sample-web-smoke-plan.json` 已内置 `device-assert-visual-page` 截图断言，可直接作为首个页面基线样例。
- 支持导出标准 JSON 计划，供 `scripts/auto/run-browser-acceptance.mjs --plan=...` 直接执行。
- 适合把当前 IoT 平台页面巡检、外部业务系统页面验证与后续扩面场景统一纳入一套执行骨架。

示例计划：
- `config/automation/sample-web-smoke-plan.json`

示例命令：
```bash
node scripts/auto/run-browser-acceptance.mjs --plan=config/automation/sample-web-smoke-plan.json --dry-run
node scripts/auto/run-browser-acceptance.mjs --plan=config/automation/sample-web-smoke-plan.json --update-baseline
node scripts/auto/run-browser-acceptance.mjs --plan=config/automation/sample-web-smoke-plan.json
node scripts/auto/manage-visual-baselines.mjs --input=logs/acceptance/config-browser-visual-manifest-<timestamp>.json
node scripts/auto/manage-visual-baselines.mjs --input=logs/acceptance/config-browser-visual-manifest-<timestamp>.json --mode=promote --status=missing,mismatch --apply
```

建议实践：
1. 先在自动化测试中心整理“业务点梳理”，再细化到步骤层，避免直接写低层选择器导致后续难维护。
2. 先使用“页面盘点与脚手架生成”补齐页面覆盖，再针对高价值页面继续补充真实交互步骤。
3. 对关键页面至少补齐 `readySelector + triggerApi + assertText/assertUrlIncludes` 三类证据。
4. 动态菜单环境下，新增路由授权后需重新登录，确保 `authContext.menus` 刷新后再执行浏览器巡检。
5. 首次建设视觉回归时，先执行 `--update-baseline` 生成截图基线；后续常规回归执行不带该参数，让报告专注暴露真实视觉差异。

### 13.2 截图基线与视觉回归

- 基线目录默认落在 `config/automation/baselines/<planSlug>/<scenarioKey>/<baselineName>.png`，适合按计划、场景、断言名称分层维护。
- `assertScreenshot` 支持两类目标：`page` 用于整页基线，`locator` 用于表格、图表、卡片等局部区域基线。
- 若未找到基线且未开启 `--update-baseline`，执行器会把该步骤标记为失败，并在报告中提示缺失基线。
- 若存在基线，执行器会输出 baseline / actual / diff 三类图片路径，并在 Markdown 报告中汇总视觉断言通过数、失败数、刷新数与缺失数。
- 每轮执行还会生成 visual manifest JSON、diff 图片索引页和失败截图明细页，便于测试、产品、设计协同查看视觉差异。
- 推荐将关键页面首屏、复杂表格、图表看板、弹窗结果页纳入视觉断言，和文本/API 断言形成互补证据链。

### 13.3 视觉基线治理命令

- `scripts/auto/manage-visual-baselines.mjs` 支持读取两类输入：配置驱动执行输出的 visual manifest JSON，或完整的 browser results JSON。
- 默认 `--mode=audit` 会按筛选条件输出治理 JSON 与 Markdown 报告，便于先做离线审计，再决定是否提升基线。
- `--status=passed,mismatch,missing,updated` 支持按视觉断言状态过滤；若不传且使用 `--mode=promote`，默认治理 `missing,mismatch`。
- `--scenario=...` 与 `--label=...` 可进一步按场景、步骤名称缩小治理范围，适合对单页或单组件做定向基线维护。
- 仅在明确评审通过后才执行 `--mode=promote --apply`，脚本会把 actual 图片复制为 baseline，并输出本轮提升记录。
- 前端工作目录已补充快捷命令：`cd spring-boot-iot-ui && npm run acceptance:browser:baseline:manage -- --input=../logs/acceptance/config-browser-visual-manifest-<timestamp>.json`
