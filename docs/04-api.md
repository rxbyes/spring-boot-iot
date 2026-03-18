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
- 前端 `spring-boot-iot-ui/src/api/request.ts` 也会在 `JSON.parse` 前兜底识别 `id/*Id` 的超大整数历史响应，并按字符串语义保留，避免审计日志等详情链路因精度丢失而误报“记录不存在”。

补充说明：
- 自 2026-03-17 起，后端 `@RequestBody` 与统一 JSON 序列化链路统一按 Spring Boot 4 / Jackson 3 兼容写法维护：运行时 `ObjectMapper` 使用 `JsonMapper` / `JsonMapperBuilderCustomizer`，但 `JsonFormat` 等注解仍沿用 `com.fasterxml.jackson.annotation`。

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
      "traceId": "20260317-abc123",
      "deviceCode": "demo-device-01",
      "productKey": "demo-product",
      "messageType": "property",
      "topic": "/sys/demo-product/demo-device-01/thing/property/post",
      "payload": "{\"messageType\":\"property\",\"properties\":{\"temperature\":26.5,\"humidity\":68}}"
    }
  ]
}
```

说明：
- 当前返回结果会补充 `traceId`、`deviceCode`、`productKey`，用于与 `/system-log`、`/message-trace` 页面联动。

### 查询消息追踪分页
`GET /api/device/message-trace/page`

支持的筛选参数：
- `deviceCode`：按设备编码精确匹配。
- `productKey`：按产品标识精确匹配。
- `traceId`：按链路 TraceId 精确匹配。
- `messageType`：按消息类型精确匹配。
- `topic`：按 Topic 模糊匹配。
- `pageNum` / `pageSize`：分页参数。

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": "1934862000000000001",
        "traceId": "20260317-abc123",
        "deviceCode": "demo-device-01",
        "productKey": "demo-product",
        "messageType": "property",
        "topic": "/sys/demo-product/demo-device-01/thing/property/post",
        "payload": "{\"messageType\":\"property\",\"properties\":{\"temperature\":26.5}}",
        "reportTime": "2026-03-17T10:30:00"
      }
    ]
  }
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
- 当前系统治理页已接入的按钮权限码包括：`system:user:add`、`system:user:update`、`system:user:delete`、`system:user:reset-password`、`system:role:add`、`system:role:update`、`system:role:delete`、`system:menu:add`、`system:menu:update`、`system:menu:delete`。
- 当用户未绑定任何启用角色时，登录接口仍返回 `code=200`；`authContext.roles`、`authContext.roleCodes`、`authContext.menus`、`authContext.permissions` 为空数组，`homePath` 回退为 `/`。

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

`authContext` 当前除 `menus`、`permissions`、`roles` 外，还补充以下账号展示字段：
- `phone`：当前用户手机号
- `email`：当前用户邮箱
- `accountType`：账号类型（当前按 `SUPER_ADMIN` 口径区分主账号 / 子账号）
- `authStatus`：实名信息状态说明
- `loginMethods`：可用登录方式列表（如 `账号登录`、`手机号登录`）

### 角色列表
`GET /api/role/list`

说明：
- 支持可选筛选参数：`roleName`、`roleCode`、`status`。
- 角色列表返回角色主数据；菜单授权详情通过角色详情接口获取。

### 角色详情
`GET /api/role/{id}`

说明：
- 返回角色主数据，同时附带 `menuIds`。
- `menuIds` 为当前角色已绑定的页面/按钮菜单 ID 集合，父级目录会在保存时由后端自动补齐。

### 新增角色
`POST /api/role/add`

请求体示例：
```json
{
  "roleName": "值班组长",
  "roleCode": "DUTY_MANAGER",
  "description": "负责值班统筹与处置协同",
  "status": 1,
  "menuIds": [93002001, 93002002, 93003201]
}
```

说明：
- `menuIds` 可同时传页面菜单与按钮权限。
- 角色管理页仅要求勾选页面/按钮节点，目录节点由后端自动补齐到 `sys_role_menu`。

### 更新角色
`PUT /api/role/update`

说明：
- 请求体与新增角色一致，但必须携带 `id`。
- 更新菜单授权后，相关用户重新登录即可刷新 `authContext.menus` 与按钮权限。

### 删除角色
`DELETE /api/role/{id}`

### 按用户查询角色
`GET /api/role/user/{userId}`

### 菜单树
`GET /api/menu/tree`

说明：
- 返回完整启用菜单树，供角色管理页做菜单授权。
- `type = 0/1` 为目录或页面，`type = 2` 为按钮权限。
- `meta_json` 会解析为 `meta` 对象返回。
- 当前前端角色授权树按“菜单管理维护结构、角色管理维护授权”的职责拆分：菜单页负责元数据维护，角色页负责勾选页面与按钮权限。
- 菜单管理页自身的操作按钮通过 `system:menu:add`、`system:menu:update`、`system:menu:delete` 控制可见性；“前往角色授权”入口通过 `system:role:update` 控制。

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

## 审计日志查询接口补充（2026-03-18）

### 审计日志列表
`GET /api/system/audit-log/list`

### 审计日志分页
`GET /api/system/audit-log/page`

支持的筛选参数：
- `userName`：按操作用户模糊匹配。
- `operationType`：按操作类型精确匹配；系统日志页固定使用 `system_error`。
- `traceId`：按链路 TraceId 精确匹配。
- `deviceCode`：按设备编码模糊匹配。
- `productKey`：按产品标识模糊匹配。
- `operationModule`：按操作模块模糊匹配。
- `requestMethod`：按请求方法/通道精确匹配，例如 `MQTT`、`SYSTEM`、`GET`、`POST`。
- `requestUrl`：按请求 URL、topic 或生命周期目标模糊匹配。
- `resultMessage`：按结果消息模糊匹配。
- `errorCode`：按异常编码精确匹配。
- `exceptionClass`：按异常类型模糊匹配。
- `operationResult`：按结果状态精确匹配（`1` 成功、`0` 失败）。
- `pageNum` / `pageSize`：分页参数，仅 `/page` 生效。
- `excludeSystemError`：是否排除 `operation_type=system_error`；业务日志页建议传 `true`，系统日志页不传并固定追加 `operationType=system_error`。

### 系统异常统计概览
`GET /api/system/audit-log/system-error/stats`

统计字段：
- `total`：异常总量（当前筛选条件下）。
- `todayCount`：今日异常数。
- `mqttCount`：`requestMethod=MQTT` 的异常数。
- `systemCount`：`requestMethod=SYSTEM` 的异常数。
- `distinctTraceCount`：去重后的链路数（`traceId`）。
- `distinctDeviceCount`：去重后的设备数（`deviceCode`）。
- `topModules` / `topExceptionClasses` / `topErrorCodes`：Top5 分桶统计。

### 业务审计统计概览
`GET /api/system/audit-log/business/stats`

说明：
- 该接口固定排除 `operation_type=system_error`，用于业务审计口径统计。
- 支持复用日志筛选参数（如 `userName`、`operationType`、`operationModule`、`traceId`、`operationResult` 等），不需要传分页参数。

统计字段：
- `total`：审计总量（当前筛选条件下）。
- `todayCount`：今日审计数。
- `successCount` / `failureCount`：按 `operationResult` 聚合的成功/失败数。
- `distinctUserCount`：去重后的操作用户数（`userName`）。
- `topModules` / `topUsers` / `topOperationTypes`：Top5 分桶统计。

前端入口约定：
- `/audit-log`：业务日志页，默认排除 `system_error`。
- `/system-log`：系统日志页，只展示 `system_error` 后台异常记录，并支持按 `TraceId`、设备编码、产品标识、异常编码、异常类型联查。
- `/message-trace`：消息追踪页，查询 `iot_device_message_log` 中的链路消息，并支持回跳 `/system-log`。

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
- `traceId`：优先复用请求头 `X-Trace-Id`，若未传则由后端自动生成，并同步写入 MDC 与线程上下文。
- `requestParams`：优先记录 query + 请求体（JSON/Form 等），不再仅记录 query string。
- `responseResult`：记录 `HTTP 状态码` + 响应体摘要（有内容时）。
- `operationMethod`：优先记录 `Controller#method`，无法解析时回退到匹配路由模板。
- HTTP 请求即使返回 `200`，只要统一响应体中的 `code != 200`，也会按失败写入审计结果。

系统异常事件口径：
- `operationType`：固定为 `system_error`。
- `userName`：固定为 `SYSTEM`。
- `requestMethod`：异步 MQTT 异常固定记为 `MQTT`。
- `requestUrl`：记录 MQTT topic，或 `startup` / `subscribe` / `connection` / `shutdown` 等生命周期目标。
- `traceId`：与同链路消息日志保持一致，用于跳转“消息追踪”页。
- `deviceCode` / `productKey`：尽量从异常上下文中提取，便于设备维度排障。
- `errorCode` / `exceptionClass`：补充后端异常码和异常类型，便于研发快速过滤。

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

## 系统治理分页接口补充（2026-03-17）

为解决系统治理模块“分页展示条数异常、后端仍全量查询”的问题，系统管理相关列表统一补充真实分页接口，前端改为按需加载。

统一分页返回 `data` 结构：
```json
{
  "total": 125,
  "pageNum": 1,
  "pageSize": 10,
  "records": []
}
```

新增或补齐的分页接口：
- `GET /api/user/page`
- `GET /api/role/page`
- `GET /api/dict/page`
- `GET /api/system/channel/page`
- `GET /api/organization/page`
- `GET /api/region/page`
- `GET /api/menu/page`
- `GET /api/system/audit-log/page`

参数说明：
- 用户分页：`username`、`phone`、`email`、`status`、`pageNum`、`pageSize`
- 角色分页：`roleName`、`roleCode`、`status`、`pageNum`、`pageSize`
- 字典分页：`dictName`、`dictCode`、`dictType`、`pageNum`、`pageSize`
- 通知渠道分页：`channelName`、`channelCode`、`channelType`、`pageNum`、`pageSize`
- 组织分页：`orgName`、`orgCode`、`status`、`pageNum`、`pageSize`
- 区域分页：`regionName`、`regionCode`、`regionType`、`pageNum`、`pageSize`
- 菜单分页：`menuName`、`menuCode`、`type`、`status`、`pageNum`、`pageSize`

树形页面加载约定：
- 组织机构、区域管理、菜单管理在“无筛选条件”时仅分页查询根节点。
- 展开树节点时，通过原有 `/list?parentId=...` 接口按需懒加载子节点，不再一次性拉整棵树。
- 进入筛选模式后，`/page` 返回扁平分页结果，用于避免搜索时再次退化为全量树查询。

兼容性说明：
- 原有 `/list`、`/tree` 接口继续保留，用于兼容既有调用链与授权树场景。
- `GET /api/menu/tree` 仍作为角色授权树专用接口；菜单管理页主列表已改用 `/api/menu/page` + `/api/menu/list?parentId=...`。

## 账号安全接口补充（2026-03-17）

### 修改当前账号密码
`POST /api/user/change-password`

请求体：
```json
{
  "id": "193847562001",
  "oldPassword": "123456",
  "newPassword": "abc123456"
}
```

说明：
- `id` 为当前登录用户 ID。
- 前端右上角头像菜单中的“修改密码”会调用该接口。
- 修改成功后，前端会清理当前登录态并要求重新登录。

## 消息追踪补充（2026-03-18）
- 当 MQTT 上行消息在前置校验阶段失败（如设备不存在、产品不匹配）时，后端会额外补写一条 `iot_device_message_log` 记录。
- 补写记录的 `messageType=dispatch_failed`，用于将 `sys_audit_log` 的 `system_error` 与 `/api/device/message-trace/page` 通过同一 `traceId` 进行链路关联。
- 设备不存在场景允许 `deviceId=0` 占位，并保留 `traceId/deviceCode/productKey/topic/payload` 供审计与排障。
