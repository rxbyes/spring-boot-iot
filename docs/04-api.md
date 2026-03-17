# API 文档

本文档记录当前已实现并已验证的 HTTP 调试接口，同时补充 Phase 2 已交付的 MQTT 上下行能力。

## 统一返回格式
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

说明：
- 自 2026-03-17 起，对于后端 `Long` 类型主键（如 `id`），响应统一按字符串返回，避免前端 JavaScript 对超大整数（雪花 ID）出现精度丢失。

## 异常返回约定
- 参数校验或参数绑定失败：返回 `code=400`，`msg` 为可读错误信息（如 `缺少必要参数: confirmUser`、`参数类型错误: status`、`请求体格式不正确`）。
- 业务异常：返回业务定义错误码与业务消息。
- 未知系统异常：返回 `code=500`，`msg=系统繁忙，请稍后再试`。

## 风险处置分页接口补充（2026-03-17）

为保证风险处置与配置模块在大数据量下保持一致体验，后端新增以下分页接口（同时保留原 `list` 接口兼容旧调用）：

- `GET /api/risk-point/page`
- `GET /api/rule-definition/page`
- `GET /api/linkage-rule/page`
- `GET /api/emergency-plan/page`

统一分页参数：
- `pageNum`：页码（默认 `1`）
- `pageSize`：每页条数（默认 `10`）
- 其他筛选参数与对应 `list` 接口保持一致

统一分页返回 `data` 结构：
```json
{
  "total": 125,
  "pageNum": 1,
  "pageSize": 10,
  "records": []
}
```

## 产品接口

### 新增产品
`POST /api/device/product/add`

请求体：
```json
{
  "productKey": "demo-product",
  "productName": "演示产品",
  "protocolCode": "mqtt-json",
  "nodeType": 1,
  "dataFormat": "JSON",
  "manufacturer": "Codex",
  "description": "演示产品"
}
```

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 2001,
    "productKey": "demo-product",
    "productName": "演示产品",
    "protocolCode": "mqtt-json"
  }
}
```

### 根据 ID 查询产品
`GET /api/device/product/{id}`

## 设备接口

### 新增设备
`POST /api/device/add`

请求体：
```json
{
  "productKey": "demo-product",
  "deviceName": "演示设备-01",
  "deviceCode": "demo-device-01",
  "deviceSecret": "123456",
  "clientId": "demo-device-01",
  "username": "demo-device-01",
  "password": "123456",
  "firmwareVersion": "1.0.0",
  "ipAddress": "127.0.0.1",
  "address": "lab-a"
}
```

### 根据 ID 查询设备
`GET /api/device/{id}`

### 根据 deviceCode 查询设备
`GET /api/device/code/{deviceCode}`

### 查询设备选项列表
`GET /api/device/list`

说明：
- 仅返回风险点绑定弹窗所需的最小设备字段。
- 当前用于风险点管理页与风险监测筛选项的真实数据装载。

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 3001,
      "productId": 2001,
      "deviceCode": "accept-http-device-01",
      "deviceName": "验收设备-HTTP-01",
      "onlineStatus": 1
    }
  ]
}
```

### 查询设备测点选项
`GET /api/device/{deviceId}/metrics`

说明：
- 优先返回设备所属产品的 `property` 物模型。
- 若物模型未维护，则回退到设备当前已产生的属性标识，保证风险点绑定可继续联调。

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "identifier": "temperature",
      "name": "温度",
      "dataType": "double"
    },
    {
      "identifier": "humidity",
      "name": "湿度",
      "dataType": "int"
    }
  ]
}
```

## 消息接入接口

### HTTP 模拟设备上报
`POST /message/http/report`

请求体：
```json
{
  "protocolCode": "mqtt-json",
  "productKey": "demo-product",
  "deviceCode": "demo-device-01",
  "payload": "{\"messageType\":\"property\",\"properties\":{\"temperature\":26.5,\"humidity\":68}}",
  "topic": "/sys/demo-product/demo-device-01/thing/property/post",
  "clientId": "demo-device-01",
  "tenantId": "1"
}
```

说明：
- HTTP 入口主要用于本地调试和回归。
- `protocolCode` 目前联调验证通过的是 `mqtt-json`。
- `payload` 当前以字符串形式承载原始 JSON。

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### MQTT 上行接入

MQTT 上行不提供额外 HTTP API，设备消息直接通过 Broker 进入：

- 直连设备标准 topic：
  - `/sys/{productKey}/{deviceCode}/thing/property/post`
  - `/sys/{productKey}/{deviceCode}/thing/event/post`
  - `/sys/{productKey}/{deviceCode}/thing/status/post`
- 历史兼容 topic：`$dp`
- 子设备预留 topic：
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/post`
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/event/post`
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/status/post`

标准 topic payload 示例：
```json
{
  "messageType": "property",
  "properties": {
    "temperature": 26.5,
    "humidity": 68
  }
}
```

`$dp` payload 示例：
```json
{
  "deviceCode": "demo-device-01",
  "temperature": 25.1,
  "humidity": 61
}
```

说明：
- MQTT 消息进入后仍走统一主链路：`RawDeviceMessage -> UpMessageDispatcher -> ProtocolAdapter -> DeviceMessageService`
- 当前已能识别直连设备 topic、历史 `$dp` 和子设备预留 topic
- 子设备 topic 目前只完成解析结构预留，不进入完整子设备业务处理
- 查询验证仍复用已有 HTTP 接口：
  - `GET /api/device/code/{deviceCode}`
  - `GET /api/device/{deviceCode}/properties`
  - `GET /api/device/{deviceCode}/message-logs`

### MQTT 下行发布
`POST /message/mqtt/down/publish`

请求体：
```json
{
  "productKey": "codex-down-product-02",
  "deviceCode": "codex-down-device-02",
  "qos": 1,
  "commandType": "property",
  "params": {
    "switch": 1,
    "targetTemperature": 23.0,
    "requestId": "task6-verify-001"
  }
}
```

说明：
- 当前由 `message` 模块负责下行发布，`protocol` 模块负责 `DeviceDownMessage` 编码。
- 若未显式传入 `topic`，系统按推荐规范自动拼接：
  - 属性下发：`/sys/{productKey}/{deviceCode}/thing/property/set`
  - 服务调用：`/sys/{productKey}/{deviceCode}/thing/service/{serviceIdentifier}/invoke`
- 子设备下行预留 topic：
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/set`
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/service/{serviceIdentifier}/invoke`
- 若未显式传入 `protocolCode`，默认继承设备绑定协议。
- 当前只建立最小发布能力，不实现 ACK、重试、状态机。
- 当前下行发布入口只面向直连设备；子设备下行 topic 仅做规范预留。

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "protocolCode": "mqtt-json",
    "topic": "/sys/codex-down-product-02/codex-down-device-02/thing/property/set",
    "qos": 1,
    "retained": false,
    "deviceCode": "codex-down-device-02",
    "productKey": "codex-down-product-02",
    "commandType": "property"
  }
}
```

## 设备属性与消息日志接口

### 查询设备最新属性
`GET /api/device/{deviceCode}/properties`

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "identifier": "temperature",
      "propertyName": "temperature",
      "propertyValue": "26.5",
      "valueType": "double"
    },
    {
      "identifier": "humidity",
      "propertyName": "humidity",
      "propertyValue": "68",
      "valueType": "int"
    }
  ]
}
```

### 查询设备消息日志
`GET /api/device/{deviceCode}/message-logs`

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "messageType": "property",
      "topic": "/sys/demo-product/demo-device-01/thing/property/post",
      "payload": "{\"messageType\":\"property\",\"properties\":{\"temperature\":26.5,\"humidity\":68}}"
    }
  ]
}
```

## 文件调试接口

### 查询设备文件快照
`GET /api/device/{deviceCode}/file-snapshots`

说明：
- 用于查看表 C.3 文件类消息在 Redis 中的最小持久化结果
- 当前返回文件描述、文件长度、Base64 文件流和更新时间

### 查询设备固件聚合结果
`GET /api/device/{deviceCode}/firmware-aggregates`

说明：
- 用于查看表 C.4 固件分包在 Redis 中的聚合状态
- 当前返回分包数量、已接收分包索引、重组结果、MD5 校验结果

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "transferId": "0-2-1024",
      "deviceCode": "demo-device-01",
      "dataSetId": "ota-firmware",
      "fileType": "bin",
      "receivedPacketCount": 2,
      "totalPackets": 2,
      "md5Matched": true
    }
  ]
}
```

## Phase 4 报表接口

### 风险趋势分析
`GET /api/report/risk-trend`

说明：
- 查询参数 `startDate`、`endDate` 可选，格式固定为 `YYYY-MM-DD`
- 未传日期区间时，后端按当前库内全部历史告警与事件记录聚合
- 返回数组项字段固定为 `date`、`alarmCount`、`eventCount`

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "date": "2026-03-15",
      "alarmCount": 3,
      "eventCount": 2
    }
  ]
}
```

### 告警统计
`GET /api/report/alarm-statistics`

说明：
- 查询参数 `startDate`、`endDate` 可选，格式固定为 `YYYY-MM-DD`
- 返回字段固定为 `total`、`critical`、`high`、`medium`、`low`
- 兼容历史字段 `count`、`criticalCount`、`warningCount`、`infoCount`

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 12,
    "critical": 2,
    "high": 5,
    "medium": 4,
    "low": 1
  }
}
```

### 事件闭环分析
`GET /api/report/event-closure`

说明：
- 查询参数 `startDate`、`endDate` 可选，格式固定为 `YYYY-MM-DD`
- 返回字段固定为 `total`、`closed`、`unclosed`
- 兼容历史字段 `count`、`pendingCount`、`processingCount`、`closedCount`、`avgProcessingTime`

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 9,
    "closed": 6,
    "unclosed": 3,
    "pendingCount": 2,
    "processingCount": 1,
    "closedCount": 6,
    "avgProcessingTime": 5.5
  }
}
```

### 设备健康分析
`GET /api/report/device-health`

说明：
- 当前基于 `iot_device.online_status` 与 `iot_device.last_report_time` 计算
- 返回字段固定为 `total`、`online`、`offline`、`onlineRate`、`healthy`、`warning`、`critical`
- 兼容历史字段 `totalCount`、`onlineCount`、`offlineCount`、`healthyCount`、`unhealthyCount`

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 20,
    "online": 16,
    "offline": 4,
    "onlineRate": 80.0,
    "healthy": 11,
    "warning": 5,
    "critical": 4
  }
}
```
## 典型错误返回

### 非法协议编码
```json
{
  "code": 500,
  "msg": "未找到协议适配器: bad-protocol"
}
```

### 设备不存在
```json
{
  "code": 500,
  "msg": "设备不存在: missing-device"
}
```

## 认证接口与鉴权规则（2026-03-16）

### 登录
`POST /api/auth/login`

请求体：
```json
{
  "username": "admin",
  "password": "123456"
}
```

账号密码模式说明：
- `loginType` 缺省或传 `account` 时，按 `username + password` 校验。
- 当前前端登录页路由为 `/login`，对应“账号密码登录”Tab。

手机号模式请求体：
```json
{
  "loginType": "phone",
  "phone": "13800138000",
  "password": "123456"
}
```

手机号模式说明：
- 当前共享环境使用“手机号 + 系统密码”统一登录，不引入短信验证码服务。
- 后端会先按 `phone` 查询 `sys_user`，再复用同一套密码校验与 JWT 签发逻辑。
- 登录页左侧已提供微信扫码视觉入口，但当前共享环境尚未接入微信开放平台回调与票据校验，因此该模式暂为接入占位，不计入真实环境验收通过项。

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "<jwt-token>",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "tokenHeader": "Authorization",
    "userId": 1,
    "username": "admin",
    "realName": "管理员",
    "authContext": {
      "userId": 1,
      "username": "admin",
      "realName": "管理员",
      "displayName": "管理员",
      "superAdmin": true,
      "homePath": "/products",
      "roleCodes": ["SUPER_ADMIN"],
      "permissions": ["system:user:add", "system:user:update", "system:role:add"],
      "roles": [
        {
          "id": 92000005,
          "roleCode": "SUPER_ADMIN",
          "roleName": "超级管理人员"
        }
      ],
      "menus": [
        {
          "id": 93000001,
          "menuName": "设备接入",
          "menuCode": "iot-access",
          "type": 0,
          "meta": {
            "description": "接入与运维",
            "menuTitle": "设备接入与运维",
            "menuHint": "管理产品模板、设备台账、上报回放与设备侧联调能力。"
          },
          "children": []
        }
      ]
    }
  }
}
```

补充说明：
- 登录成功后，前端应直接持久化 `token` 与 `authContext`。
- `authContext.menus` 为当前用户已授权的导航树；前端不再内置固定角色菜单。
- `authContext.permissions` 为当前用户按钮级权限码，当前用于 `v-permission`。

### 当前登录用户
`GET /api/auth/me`

请求头：
```text
Authorization: Bearer <jwt-token>
```

未携带或携带无效 token 时返回：
```json
{
  "code": 401,
  "msg": "未认证或登录已过期",
  "data": null
}
```

成功时返回与登录响应中的 `authContext` 同结构数据，用于刷新页面后重新恢复当前用户菜单、角色和按钮权限。

### 菜单树
`GET /api/menu/tree`

说明：
- 返回完整启用菜单树，供角色管理页做菜单授权。
- `type = 0/1` 为目录或页面，`type = 2` 为按钮权限。
- `meta_json` 会解析为 `meta` 对象返回。

### 菜单列表
`GET /api/menu/list`

说明：
- 支持可选筛选参数：`menuName`、`menuCode`、`type`、`status`。
- 按 `sort`、`id` 升序返回。

### 菜单详情
`GET /api/menu/{id}`

### 新增菜单
`POST /api/menu/add`

### 更新菜单
`PUT /api/menu/update`

### 删除菜单
`DELETE /api/menu/{id}`

说明：
- 存在子菜单时不允许删除。
- 被角色授权引用的菜单不允许删除。

### 鉴权规则
- 以下接口免登录：
  - `/api/auth/login`
  - `/message/http/report`
  - `/api/cockpit/**`
  - `/actuator/**`
  - `/doc.html`、`/swagger-ui/**`、`/v3/api-docs/**`
- 其余接口默认需要 `Authorization: Bearer <jwt-token>`
- 前端未登录访问受保护页面时，会统一跳转到 `/login`
- 前端登录后，顶部导航、左侧菜单、按钮权限均应以 `authContext` 为准，不再以页面硬编码角色配置为准

## 审计日志采集说明（2026-03-17）

适用范围：
- 自动采集范围保持不变：`/api/**`（排除 `/api/system/audit-log/**` 与 `/api/auth/login`）。
- 新增后台异常事件采集：MQTT 客户端启动失败、订阅失败、连接断开、消息分发失败等异步异常会写入 `sys_audit_log`。

采集字段增强：
- `requestParams`：优先记录 query + 请求体（JSON/Form 等），不再仅记录 query string。
- `responseResult`：记录 `HTTP 状态码` + 响应体摘要（有内容时）。
- `operationMethod`：优先记录 `Controller#method`，无法解析时回退到匹配路由模板。
- HTTP 请求即使返回 `200`，只要统一响应体中的 `code != 200`，也会按失败写入审计结果。

系统异常事件口径：
- `operationType`：固定为 `system_error`。
- `userName`：固定为 `SYSTEM`。
- `requestMethod`：异步 MQTT 异常固定记为 `MQTT`。
- `requestUrl`：记录 MQTT topic，或 `startup` / `subscribe` / `connection` / `shutdown` 等生命周期目标。

安全与容量控制：
- 对 `password/token/secret/authorization/accessToken/refreshToken/clientSecret` 等敏感字段自动脱敏（`***`）。
- 请求与响应内容按固定上限截断并追加 `...(truncated)`，避免超长日志影响查询与展示。

 审计详情接口：
  - `GET /api/system/audit-log/get/{id}`：返回增强后的 `requestParams`、`responseResult`、`resultMessage` 供前端详情页展示。
- 当 `{id}` 不存在或已删除时，返回 `code=404`、`msg=审计日志不存在或已删除`，前端不应继续展示空详情弹窗。

## 系统异常自动通知（2026-03-17）

适用范围：
- 自动通知当前仅支持 `webhook`、`wechat`、`feishu`、`dingtalk` 四类通知渠道。
- 只有开启 `iot.observability.system-error-notify-enabled=true` 后，后台 `system_error` 审计事件才会继续触发通知发送。

配置项：
- `iot.observability.system-error-notify-enabled`：是否启用系统异常自动通知，默认 `false`。
- `iot.observability.notification-timeout-ms`：通知 HTTP 请求超时时间，默认 `3000` 毫秒。
- `iot.observability.system-error-notify-cooldown-seconds`：同一渠道 + 同一异常签名的全局节流窗口，默认 `300` 秒。

通知渠道 `config` JSON 示例：
```json
{
  "url": "https://example.com/iot/webhook",
  "headers": {
    "Authorization": "Bearer demo-token"
  },
  "scenes": ["system_error"],
  "timeoutMs": 3000,
  "minIntervalSeconds": 300
}
```

说明：
- 自动系统异常通知要求 `config.url` 非空，且 `config.scenes` 或 `config.scene` 中包含 `system_error`。
- `timeoutMs`、`minIntervalSeconds` 可按渠道单独覆盖全局默认值。
- 通知发送失败只写应用日志，不会反向再写新的审计日志，避免递归异常风暴。

测试通知接口：
### 发送通知渠道测试消息
`POST /api/system/channel/test/{channelCode}`

说明：
- 用于手工验证指定渠道的 URL、鉴权头、机器人地址等配置是否可用。
- 测试发送不要求 `scenes` 包含 `system_error`，但仍要求渠道启用、类型受支持且 `config.url` 存在。

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

## Phase 4 风险监测 API

### 实时监测列表
`GET /api/risk-monitoring/realtime/list`

说明：
- 响应按统一 `ApiEnvelope<PageResult<RiskMonitoringListItem>>` 结构返回。
- 支持筛选参数 `regionId`、`riskPointId`、`deviceCode`、`riskLevel`、`onlineStatus`、`pageNum`、`pageSize`。
- 真实环境联调前需先执行 `sql/upgrade/20260316_phase4_task3_risk_monitoring_schema_sync.sql`；若共享开发库缺少 `risk_point_device` 表或关键列，接口会返回明确业务错误。

### 实时监测详情
`GET /api/risk-monitoring/realtime/{bindingId}`

说明：
- 返回当前监测详情、最近 24h 趋势、最近告警、最近事件四类数据。
- `bindingId` 对应风险点设备绑定表 `risk_point_device.id`。

### GIS 风险态势点位
`GET /api/risk-monitoring/gis/points`

说明：
- 响应按统一 `ApiEnvelope<RiskMonitoringGisPoint[]>` 结构返回。
- 支持可选区域参数 `regionId`。
- 当前仅用于 ECharts 点位态势图，不代表完整 GIS SDK / 地图底图集成。
