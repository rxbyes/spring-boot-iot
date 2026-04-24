# 设备资产中心按产品类型动态能力设计

日期：2026-04-24

## 1. 背景与目标

设备资产中心需要按设备所属产品类型展示不同操作能力。当前产品已存在 `metadataJson.governance.productCapabilityType` 口径，可作为能力类型真相源：

- `COLLECTING`：采集型
- `MONITORING`：监测型
- `WARNING`：预警型
- `VIDEO`：视频型

本设计首版目标是：

1. 设备实例继承所属产品的能力类型，不在设备主档新增第二套类型真相。
2. 预警型继续按子品类区分广播喇叭、情报板、爆闪灯。
3. 视频型首版支持播放视频、停止播放、按方位角转向。
4. 所有设备操作走异步命令闭环：下发记录、等待反馈、成功/失败/超时。
5. 首版采用“产品元数据 + 后端内置能力预设”，后续再平滑演进为数据库能力注册表。

## 2. 受影响模块

- `spring-boot-iot-device`：产品能力解析、设备能力查询、命令记录读侧、反馈关联。
- `spring-boot-iot-message`：设备能力执行入口、参数校验、topic 与 payload 编排、MQTT 下发。
- `spring-boot-iot-protocol`：保留协议编解码边界；预警和视频文本指令只作为 payload adapter，不承载业务状态。
- `spring-boot-iot-ui`：设备资产中心能力面板、行操作入口、命令反馈展示。
- `schema/` 与文档：若扩展 `iot_command_record` 或新增命令反馈字段，必须按 schema registry 流程生成初始化 SQL 和文档。

## 3. 领域模型

### 3.1 产品元数据

继续使用产品 `metadataJson` 作为能力配置承载：

```json
{
  "governance": {
    "productCapabilityType": "WARNING",
    "warningDeviceKind": "BROADCAST"
  }
}
```

视频型示例：

```json
{
  "governance": {
    "productCapabilityType": "VIDEO",
    "videoDeviceKind": "PTZ_CAMERA"
  }
}
```

首版新增或明确的元数据字段：

- `governance.productCapabilityType`：产品能力大类，固定为 `COLLECTING / MONITORING / WARNING / VIDEO / UNKNOWN`。
- `governance.warningDeviceKind`：预警子品类，固定为 `BROADCAST / LED / FLASH`。
- `governance.videoDeviceKind`：视频子品类，首版可选 `FIXED_CAMERA / PTZ_CAMERA`，其中 PTZ 支持方位角转向。

### 3.2 能力编码

后端内置能力预设使用稳定编码：

| 能力编码 | 名称 | 适用范围 |
|---|---|---|
| `reboot` | 重启 | 监测型、预警型、可支持的视频型 |
| `power_switch` | 开关 | 监测型 |
| `firmware_upgrade` | 固件升级 | 监测型、可支持的其他类型 |
| `broadcast_play` | 播放广播内容 | 预警型广播 |
| `broadcast_stop` | 停止广播 | 预警型广播 |
| `broadcast_volume` | 音量控制 | 预警型广播 |
| `led_program` | 情报板节目控制 | 预警型情报板 |
| `led_stop` | 情报板关闭 | 预警型情报板 |
| `flash_control` | 爆闪灯控制 | 预警型爆闪灯 |
| `flash_stop` | 爆闪灯关闭 | 预警型爆闪灯 |
| `video_play` | 播放视频 | 视频型 |
| `video_stop` | 停止播放视频 | 视频型 |
| `video_turn_azimuth` | 按方位角转向 | 视频型 PTZ |

采集型首版默认只展示资产治理、拓扑和子设备相关入口；是否暴露重启或固件升级由后续产品元数据显式开关扩展，不在首版默认打开。

## 4. 后端 API 设计

### 4.1 查询设备能力

新增：

`GET /api/device/{deviceCode}/capabilities`

响应结构：

```json
{
  "deviceCode": "DEVICE001",
  "productId": 1,
  "productKey": "warning-broadcast-v1",
  "productCapabilityType": "WARNING",
  "subType": "BROADCAST",
  "onlineExecutable": true,
  "capabilities": [
    {
      "code": "broadcast_play",
      "name": "播放内容",
      "description": "向广播设备下发播报内容",
      "enabled": true,
      "requiresOnline": true,
      "paramsSchema": {
        "bNum": { "type": "integer", "label": "播报次数", "min": -1 },
        "content": { "type": "string", "label": "播报内容", "required": true },
        "volume": { "type": "integer", "label": "音量", "min": 0, "max": 100 }
      }
    }
  ]
}
```

读侧规则：

- 未登记设备不返回可执行能力，只返回空能力与不可执行原因。
- 产品停用、设备停用、设备未激活时，能力可展示但执行按钮禁用并给出原因。
- 需要在线的能力在设备离线时禁用；不需要在线的能力可保留扩展空间，但首版下行操作默认要求在线。

### 4.2 执行设备能力

新增：

`POST /api/device/{deviceCode}/capabilities/{capabilityCode}/execute`

请求示例：

```json
{
  "params": {
    "content": "前方道路施工，请减速慢行",
    "bNum": 1,
    "volume": 80
  }
}
```

响应示例：

```json
{
  "commandId": "1776999000000",
  "deviceCode": "DEVICE001",
  "capabilityCode": "broadcast_play",
  "status": "SENT",
  "topic": "/iot/broadcast/DEVICE001",
  "sentAt": "2026-04-24T10:50:00"
}
```

执行规则：

- Controller 只做请求/响应转换。
- Service 校验设备可见性、产品类型、能力是否允许、参数合法性和设备状态。
- message 模块负责编排 topic/payload 并发布 MQTT。
- 下发前创建 `iot_command_record`，发布成功后标记 `SENT`；发布失败标记 `FAILED`。
- `commandId` 首版使用毫秒时间串，与设备规范中的 `msgid` 一致。

### 4.3 命令记录查询

新增或扩展：

`GET /api/device/{deviceCode}/commands`

用于设备详情抽屉展示最近命令，支持 `capabilityCode / status / pageNum / pageSize`。

## 5. 指令适配设计

### 5.1 预警型广播

Topic：

- 下发：`/iot/broadcast/{deviceCode}`
- 反馈：`/broadcast/{deviceCode}/feedback` 或兼容 `/iot/broadcast/{deviceCode}/feedback`

payload：

- 重启：`$cmd=reboot&msgid={commandId}`
- 播放：`$cmd=broadcast&b_num={bNum}&b_size={contentLength}&b_content={content}&volume={volume}&msgid={commandId}`
- 停止：`$cmd=stop&msgid={commandId}`
- 音量：`$cmd=play&volume={volume}&msgid={commandId}`

反馈解析：

- `result=sucd` 标记 `SUCCESS`
- `result=fail` 标记 `FAILED`
- `msgid` 必须关联平台下发 `commandId`
- `message` 保存到 `replyPayload` 或错误摘要

### 5.2 情报板

Topic：

- 下发：`/iot/led/{deviceCode}`
- 反馈：`/iot/led/{deviceCode}/feedback`

payload：

- 控制：`$cmd=led&type={type}&brigh={brigh}&freq={freq}&msgid={commandId}`
- 关闭：`$cmd=stop&msgid={commandId}`
- 重启：`$cmd=reboot&msgid={commandId}`

参数边界：

- `type`：1-10
- `brigh`：1-8
- `freq`：1-4
- `duty`：规范中提到但示例未下发，首版预留，不作为必填。

### 5.3 爆闪灯

Topic：

- 下发：`/iot/flash/{deviceCode}`
- 反馈：`/iot/flash/{deviceCode}/feedback`

payload：

- 控制：`$cmd=flash&type={type}&brigh={brigh}&freq={freq}&msgid={commandId}`
- 关闭：`$cmd=stop&msgid={commandId}`
- 重启：`$cmd=reboot&msgid={commandId}`

参数边界：

- `type`：0-3
- `brigh`：1-8
- `freq`：1-4

### 5.4 视频型

首版能力：

- `video_play`：播放视频
- `video_stop`：停止播放视频
- `video_turn_azimuth`：按方位角转向

由于当前未提供视频设备厂商指令规范，首版设计先定义平台内统一能力语义和参数：

- `video_play` 参数：`streamUrl? / channel? / durationSeconds?`
- `video_stop` 参数：无或 `channel?`
- `video_turn_azimuth` 参数：`azimuth`，范围 `0-360`

具体 topic/payload 由后续视频设备协议规范补齐；在实现时若没有真实规范，应只完成能力呈现和 mockable adapter，不应伪造厂商协议为已验收能力。

## 6. 反馈闭环

命令状态流转：

`CREATED -> SENT -> SUCCESS / FAILED / TIMEOUT`

反馈处理要求：

- 反馈消息进入现有 MQTT 上行链路时，应识别为命令反馈，不进入普通属性 latest 写入。
- 反馈 parser 从 query-string 文本中解析 `$cmd / result / msgid / message`。
- `msgid` 与 `iot_command_record.command_id` 关联。
- 找不到命令记录时写入失败归档或审计日志，不影响正常上行主链。
- 定时超时扫描可作为第二步实现，首版至少预留 `timeoutTime` 和状态更新接口；若已实现调度，则按配置超时时间标记 `TIMEOUT`。

## 7. 前端交互设计

设备资产中心保持单主列表页，不新增路由。

### 7.1 列表行操作

- 保留现有 `详情 / 编辑 / 更多` 共享操作语法。
- 对有可执行能力的已登记设备，在 `更多` 中增加 `设备操作`。
- 不把每个能力直接平铺到表格行，避免操作列膨胀。

### 7.2 详情抽屉

在 `DeviceDetailWorkbench` 已登记设备正文中新增“设备能力与命令”区块：

- 顶部展示产品能力类型与子品类。
- 能力按钮按分组展示：基础维护、预警发布、视频控制。
- 点击能力打开 `StandardFormDrawer` 或轻量能力执行抽屉。
- 抽屉内根据后端 `paramsSchema` 渲染参数表单。
- 执行成功后显示命令号与“等待设备反馈”，并刷新最近命令列表。

### 7.3 命令台账展示

最近命令列表展示：

- 命令号
- 能力名称
- 状态
- 下发时间
- 反馈时间
- 反馈摘要
- Topic

状态文案：

- `SENT`：已下发，等待反馈
- `SUCCESS`：执行成功
- `FAILED`：执行失败
- `TIMEOUT`：反馈超时

## 8. 权限与安全

建议新增按钮权限：

- `iot:device-capability:view`
- `iot:device-capability:execute`
- `iot:device-command:view`

安全规则：

- 执行能力必须复用当前设备数据权限，不能跨租户或跨组织操作。
- 停用产品、停用设备、未激活设备拒绝下发。
- 请求参数必须后端校验，前端校验只做体验增强。
- 命令记录只保存必要请求与反馈摘要，不保存设备密钥或敏感明文。

## 9. 测试与验收

单元测试：

- 产品 metadata 能力解析。
- WARNING + BROADCAST/LED/FLASH 能力矩阵。
- VIDEO + PTZ 首版三能力矩阵。
- 各能力参数校验。
- query-string 指令 payload 编排。
- 反馈 parser 与 `msgid` 状态关联。

集成测试：

- `GET /api/device/{deviceCode}/capabilities`
- `POST /api/device/{deviceCode}/capabilities/{capabilityCode}/execute`
- 反馈上行后命令状态从 `SENT` 更新到 `SUCCESS/FAILED`

前端测试：

- 设备详情展示对应能力。
- 离线/停用状态禁用执行按钮。
- 广播、情报板、爆闪灯、视频转向参数表单渲染。
- 执行后命令列表刷新。

真实环境验收：

- 使用 `application-dev.yml` 或环境变量覆盖，不使用 H2。
- MQTT consumer 与 broker 健康后，再下发真实或模拟设备指令。
- 若真实设备反馈不可用，明确报告环境阻塞，不以 H2 或废弃浏览器验收替代。

## 10. 分阶段交付

### Phase 1：能力呈现与命令下发骨架

- 产品元数据解析。
- 设备能力查询 API。
- 预警型和视频型首版能力矩阵。
- 设备资产中心能力区块与执行入口。

### Phase 2：预警指令适配与反馈关联

- 广播、情报板、爆闪灯 topic/payload 编排。
- 反馈 parser。
- `msgid` 关联命令记录。
- 命令台账查询。

### Phase 3：视频型真实协议落地

- 按真实视频设备协议补齐 topic/payload。
- 播放、停止、方位角转向真实联调。
- 如需扩展俯仰、变焦、预置位、巡航，再追加能力编码。

## 11. 假设与边界

- 产品维度是能力类型唯一真相源，设备实例不维护独立能力类型。
- 预警型首版只覆盖广播喇叭、情报板、爆闪灯。
- 视频型首版只覆盖播放、停止、方位角转向。
- 采集型首版不默认展示下行控制能力。
- 暂不建设数据库能力注册表；内置预设必须保持代码结构清晰，方便后续迁移。
- 视频设备真实 topic/payload 规范尚未提供，不能把未联调协议描述为已完成能力。

## 12. 文档更新要求

后续实现若改变行为或接口，需要同步更新：

- `docs/02-业务功能与流程说明.md`
- `docs/03-接口规范与接口清单.md`
- `docs/04-数据库设计与初始化数据.md`（如 schema 变化）
- `docs/05-自动化测试与质量保障.md` 或真实环境验收手册（如新增验收流程）
- `docs/06-前端开发与CSS规范.md` / `docs/15-前端优化与治理计划.md`（如新增前端交互规则）
- `README.md` 与 `AGENTS.md` 是否需要同步说明

