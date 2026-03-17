# 真实环境测试与验收手册

更新时间：2026-03-17

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
- 使用自动同步方式时，可执行：`PYTHONPATH=.codex-runtime/pydeps python scripts/run-real-env-schema-sync.py`
- MQTT 客户端日志无异常
- 若本轮需要验证“系统异常自动通知”，额外设置 `IOT_OBSERVABILITY_SYSTEM_ERROR_NOTIFY_ENABLED=true`，并准备可接收请求的 webhook 渠道地址
- 前端代理默认指向 `http://127.0.0.1:9999`（可通过 `VITE_PROXY_TARGET` 覆盖）
- 若通过局域网地址访问 Vite 开发服务（如 `http://172.21.16.1:5174`），当前 `dev` 配置已默认放行 `10.*`、`172.*`、`192.168.*` 来源，登录接口不应再出现 `Invalid CORS request`

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
- 组织、用户、角色、区域、字典、通知渠道、审计日志
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
4. 查询 `/audit-log` 页面或 `sys_audit_log` 表，确认新增 `operation_type=system_error`、`request_method=MQTT`、`user_name=SYSTEM` 的审计记录。
5. 检查 webhook 接收端或群机器人，确认收到系统异常通知消息。

通过标准：
- `POST /api/system/channel/test/{channelCode}` 返回 `code=200`，且接收端收到测试通知。
- MQTT 异常触发后，`sys_audit_log` 新增对应 `system_error` 记录。
- 当渠道配置包含 `scenes:["system_error"]` 且应用开关开启时，接收端能收到自动系统异常通知。

## 9. 验收产物要求
每次真实环境验收至少保留以下产物：
- 启动命令或脚本记录
- HTTP 请求与响应记录
- MQTTX 发布 / 订阅截图
- 页面操作截图
- 关键 SQL 查询结果截图或导出
- 验收结论表：通过 / 不通过 / 待确认

## 10. 环境不可用时的处理原则
- 先确认是网络、数据库、Redis、MQTT 哪一层阻塞
- 若是 MQTT 链路异常或消息未落库，补查 `/audit-log` 页面或 `sys_audit_log` 表中的 `operation_type=system_error` 记录，优先定位 topic、设备编码、协议或 Broker 连接问题
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
- 统一输出 `logs/acceptance/business-browser-summary-<timestamp>.json`
- 统一输出 `logs/acceptance/business-browser-results-<timestamp>.json`
- 统一输出 `logs/acceptance/business-browser-report-<timestamp>.md`
- 统一输出 `logs/acceptance/business-browser-screenshots-<timestamp>/`

问题记录规则：
- 正式执行 `npm run acceptance:browser` 后，脚本会默认把本轮失败问题追加到 `docs/22-automation-test-issues-20260316.md`
- 如仅需生成结果文件、不追加问题文档，可执行：`node scripts/auto/run-browser-acceptance.mjs --no-append-issues`
- 默认仅 `delivery` 场景失败会返回非零退出码；如需扩大阻断范围，可使用 `--fail-scopes=delivery,baseline`
