# MQTTX 真实环境联调手册

本手册用于在真实环境下通过 MQTTX 验证 MQTT 上报主链路。

## 1. 前提
- 后端使用 `spring-boot-iot-admin/src/main/resources/application-dev.yml` 或对应环境变量覆盖后的真实环境配置。
- MySQL、Redis、MQTT Broker 可连通。
- MQTT 客户端使用 MQTTX。
- 不再使用 H2 或旧端到端验收链路作为兜底。

## 2. 启动后端

```bash
IOT_MQTT_ENABLED=true \
mvn -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

关键日志应包含：
- MQTT 客户端连接成功
- 默认订阅 topic 成功
- 若未显式设置 `IOT_MQTT_CLIENT_ID`，日志中的有效 `clientId` 应带 `主机/PID` 运行时后缀
- 无 Broker 认证失败或重连异常

补充说明：
- 若使用 YAML 配置 `iot.mqtt.default-subscribe-topics`，历史 `$dp` 主题必须写成 `"$dp"`；未加引号时，运行时可能只剩 `/sys/**` 订阅，导致 `$dp` 回放与联调超时。

## 3. 准备测试数据
先创建产品和设备，确保产品协议为 `mqtt-json`。

若本轮需要查看 `/api/device/access-error/**` 失败归档，先确认历史库已执行：
- `sql/upgrade/20260322_phase5_device_access_error_archive.sql`
- 或 `python scripts/run-real-env-schema-sync.py`

若本轮联调的是南方测绘深部位移基准站 `SK00FB0D1310195`，还需要额外确认：
- `application-dev.yml` 已配置 `iot.device.sub-device-mappings.SK00FB0D1310195`
- 若共享库里还没有 8 个子设备与初始风险点，可先执行 `sql/upgrade/20260320_phase4_deep_displacement_sub_devices_bootstrap.sql`

产品示例：

```json
{
  "productKey": "demo-product",
  "productName": "演示产品",
  "protocolCode": "mqtt-json",
  "nodeType": 1,
  "dataFormat": "JSON"
}
```

设备示例：

```json
{
  "productKey": "demo-product",
  "deviceName": "演示设备-01",
  "deviceCode": "demo-device-01",
  "deviceSecret": "123456",
  "clientId": "demo-device-01",
  "username": "demo-device-01",
  "password": "123456"
}
```

## 4. MQTTX 连接参数
- Host：按 `application-dev.yml` 中 `iot.mqtt.broker-url`
- Port：按 Broker 配置
- Username：按 `IOT_MQTT_USERNAME`
- Password：按 `IOT_MQTT_PASSWORD`
- Client ID：建议唯一，例如 `mqttx-debug-001`

## 5. 标准 Topic 联调

Topic：

```text
/sys/demo-product/demo-device-01/thing/property/post
```

Payload：

```json
{"messageType":"property","properties":{"temperature":26.5,"humidity":68}}
```

发送后预期：
- 插入 1 条 `iot_message_log`
- 更新 `temperature`、`humidity`
- 刷新设备在线状态

## 6. 历史 `$dp` 联调

Topic：

```text
$dp
```

Payload：

```json
{"deviceCode":"demo-device-01","temperature":25.1,"humidity":61}
```

发送后预期：
- 插入 1 条 topic 为 `$dp` 的 `iot_message_log`
- 更新属性值
- 刷新设备在线状态

### 6.1 南方测绘深部位移类型 2 联调

适用场景：
- Topic 固定为 `$dp`
- 外层可能是 AES 包裹：`{"header":{"appId":"62000001"},"bodies":{"body":"..."}}`
- 解密后的真实负载遵循“类型字节 + 大端长度 + JSON 正文”格式
- 当前这台设备只走类型 `2`

解密后帧头示例：
- `[2, 1, 10, ...]`
- 第 1 字节 `2`：数据格式类型 2
- 第 2~3 字节 `0x01 0x0A`：正文长度 `266`
- 从第 4 字节开始按 UTF-8 读取 JSON 正文

明文结构示例：

```json
{
  "SK00FB0D1310195": {
    "L1_SW_1": {
      "2026-03-20T06:24:02.000Z": {
        "dispsX": -0.0445,
        "dispsY": 0.0293
      }
    }
  }
}
```

当前兼容结果：
- 基准站 `SK00FB0D1310195` 继续保留 `$dp` 原始日志和在线状态刷新
- `L1_SW_1` ~ `L1_SW_8` 会按 `iot.device.sub-device-mappings` 映射成真实子设备编码
- 子设备最新属性会按 `dispsX`、`dispsY` 写入 `iot_device_property`
- 平台查询时应直接用子设备 `device_code`，例如 `84330701`
- 若是未配置 `sub-device-mappings` 的单设备深部位移终端，且 payload 家族只包含父级 `S1_ZT_1` 与单个 `L1_SW_1` 逻辑测点，平台会直接在当前设备下写入 `dispsX`、`dispsY`，不再保留 `L1_SW_1.dispsX` 这类前缀属性

建议核对：
- `GET /api/device/84330701/properties`
- `GET /api/device/84330701/message-logs`
- `GET /api/device/code/84330701`
- `GET /api/device/code/SK00FB0D1310195`

通过标准：
- 基准站存在 `$dp` 消息日志
- 8 个子设备分别可查到 `dispsX` / `dispsY` 最新值
- 8 个子设备 `online_status`、`last_report_time` 被刷新
- 若已执行初始化脚本，可在风险点绑定中查到 8 个子设备与 `dispsX` / `dispsY`
- 若继续做红色闭环验收，`iot_alarm_record.remark.planId` 与 `iot_event_record.review_notes.emergencyPlan` 只允许命中深部位移场景化预案；若库里仅有无关通用预案，则应为空

## 7. HTTP / 数据验证

查询属性：

```bash
curl http://localhost:9999/device/demo-device-01/properties
```

查询消息日志：

```bash
curl http://localhost:9999/device/demo-device-01/message-logs
```

查询设备状态：

```bash
curl http://localhost:9999/device/code/demo-device-01
```

通过标准：
- `properties` 中可见最新属性值
- `message-logs` 中可见标准 topic 与 `$dp` 记录
- `onlineStatus = 1`
- `lastOnlineTime`、`lastReportTime` 已更新

## 8. 可选数据库验证
如需直接核对数据库，优先检查：
- `iot_device`
- `iot_device_property`
- `iot_message_log`

建议条件：
- `device_code = 'demo-device-01'`
- `topic in ('$dp', '/sys/demo-product/demo-device-01/thing/property/post')`

## 9. 可选 Redis 验证
如需直接核对 Redis，优先检查：

```text
iot:device:session:demo-device-01
```

预期字段：
- `deviceCode`
- `clientId`
- `topic`
- `connected`
- `connectTime`
- `lastSeenTime`

## 10. 常见排查
- MQTTX 已发送但数据库无变化：先看后端是否收到 MQTT 上行日志，再检查 `/system-log` 页面或 `sys_audit_log` 中是否新增 `operation_type=system_error` 的 MQTT 异常记录，然后继续核对 topic、设备编码、Broker 权限。
- 开始回放前，先访问 `/actuator/health/mqttConsumer`；若 `connected=false` 或 `subscribeTopics` 为空，先恢复 consumer 再做报文回放。
- 若本轮要验证 `$dp`，还要确认 `subscribeTopics` 中明确包含 `"$dp"`；若只剩 `/sys/**`，先检查 `application-dev.yml` 中是否把 `$dp` 写成了未加引号的裸值。
- 若已开启 `IOT_OBSERVABILITY_SYSTEM_ERROR_NOTIFY_ENABLED=true`：同步检查通知渠道 `config` 是否包含 `url` 与 `scenes:["system_error"]`，必要时先调用 `POST /api/system/channel/test/{channelCode}` 验证 webhook 侧可达性。
- 后端收到消息但未进入主链路：重点排查设备不存在、设备未绑定产品、设备协议为空、设备协议与产品协议不一致。
- 标准 topic 无效：检查 `productKey`、`deviceCode` 是否与数据库一致。
- `$dp` 无效：检查 payload 中的 `deviceCode` 是否存在，且设备协议是否为 `mqtt-json`。
- 对 `SK11%` 这类批量历史资产快速巡检时，可执行 `python scripts/audit-device-contracts.py --device-prefix SK11`，优先看 `MISSING_PRODUCT_BINDING`、`MISSING_DEVICE_PROTOCOL` 与 `DEVICE_PRODUCT_PROTOCOL_CONFLICT`。
- 修复清单导出可执行 `python scripts/audit-device-contracts.py --device-prefix SK11 --export-repair-csv logs/repair-checklists/sk11-device-contracts.csv`。
- 审核后的修复清单执行可使用 `python scripts/audit-device-contracts.py --repair-file logs/repair-checklists/sk11-device-contracts.csv --apply`。
- 标准 trace 回放可执行 `python scripts/replay-mqtt-trace.py --trace-id 97daeef362184d349330e48b4401d3ea`；若失败归档落库，优先查看 `contractSnapshot` 中的 `expectedProtocolCode / actualProtocolCode / protocolSource`。
- 若当前 dev 实例是通过本地 SOCKS 代理连接 Broker，trace 回放也要补上 `--socks-proxy-host`、`--socks-proxy-port`，必要时同时用 `--app-base-url`、`--mqtt-broker-url` 指向隔离验收实例，确保“检查健康的实例”和“发包的路径”一致。
- 若健康检查正常、代理路径也一致，但脚本仍只拿到 `PUBACK` 且查不到对应 payload 的 `iot_message_log`（兼容视图；底层来源 `iot_device_message_log`）/ `iot_device_access_error_log`，当前共享 Broker 可能不会把平台侧手工 publish 的 `$dp` 报文回投给 consumer；此时要切换到真实设备、专用模拟器或可控 Broker 环境继续验收。
- 同一深部位移样本出现两套告警/事件留痕：先核对共享环境是否仍有未升级旧版 consumer 同时在线；已升级实例会使用分布式锁，但无法约束旧实例继续按旧逻辑写入。
- 若切换到其他真实环境，只覆盖 `IOT_MQTT_*`、`IOT_MYSQL_*`、`IOT_REDIS_*` 环境变量，并同步核对 AES 商户密钥配置。
