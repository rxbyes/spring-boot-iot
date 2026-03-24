# Codex Roadmap - Phase 2 MQTT 接入

## 总目标

在 Phase 1 已有主链路基础上，增加真实 MQTT 设备接入能力，并复用现有业务处理链路：

RawDeviceMessage
-> UpMessageDispatcher
-> ProtocolAdapter
-> DeviceMessageService

要求：
- 不破坏现有 HTTP 上报链路
- 不提前实现规则、告警、OTA 等平台化能力
- 优先构建 MQTT 接入骨架与真实数据通路
- 尽量采用最小可运行实现

---

## Phase 2 Task 1：MQTT 接入骨架

### 目标
建立 MQTT 接入层基础骨架，并打通“MQTT 消息 -> 现有主链路”的桥接结构。

### 允许修改模块
- spring-boot-iot-message
- spring-boot-iot-protocol
- spring-boot-iot-device
- spring-boot-iot-admin（仅在确有必要时做最小配置接入）
- spring-boot-iot-framework（仅在确有必要时补充配置支持）

### 本轮建议新增类
#### message 模块
- MqttMessageConsumer
- MqttTopicRouter
- MqttConnectionListener（如果确有必要）
- MqttDownMessagePublisher（先给最小骨架）

#### device 模块
- DeviceAuthService
- DeviceSessionService

#### protocol 模块
- MqttTopicParser（如果 TopicRouter 需要拆分）
- MqttMessageTypeResolver（如果确有必要）

### 本轮必须完成
1. 能连接 MQTT Broker
2. 能订阅默认 topic
3. 收到 MQTT 消息后，组装 RawDeviceMessage
4. 能调用现有 UpMessageDispatcher
5. HTTP 主链路不能被破坏

### 本轮不要实现
- 复杂认证流程
- 规则引擎
- 告警
- OTA
- 网关子设备复杂逻辑
- MQTT 完整下行重试机制

### 验收标准
- 项目可编译
- MQTT 接入骨架已建立
- 现有 HTTP 接口仍能工作
- Codex 能清楚说明 MQTT 消息如何进入现有主链路

---

## Phase 2 Task 2：MQTT Topic 规范与解析

### 目标
规范并实现 MQTT topic 解析逻辑，让平台可从 topic 中提取 productKey、deviceCode、messageType。

### 参考规范
- /sys/{productKey}/{deviceCode}/thing/property/post
- /sys/{productKey}/{deviceCode}/thing/event/post
- /sys/{productKey}/{deviceCode}/thing/property/reply
- /sys/{productKey}/{deviceCode}/thing/service/reply
- /sys/{productKey}/{deviceCode}/thing/status/post

### 允许修改模块
- spring-boot-iot-message
- spring-boot-iot-protocol

### 本轮建议新增或完善
- MqttTopicRouter
- MqttTopicParser
- MqttMessageTypeResolver
- ProtocolContext 相关字段补充（如确有必要）

### 本轮必须完成
1. 从 topic 提取：
    - productKey
    - deviceCode
    - messageType
2. 将 topic 信息注入 RawDeviceMessage / ProtocolContext
3. 让 MqttJsonProtocolAdapter 能结合 topic + payload 生成统一 DeviceUpMessage

### 本轮不要实现
- 子设备复杂 topic
- 规则引擎联动
- 命令下发应答闭环

### 验收标准
- 给定标准 topic 能正确解析
- 能清晰区分 property / event / reply / status
- 解析结果可进入现有 UpMessageDispatcher 主链路

---

## Phase 2 Task 3：设备认证基础版

### 目标
实现最小可运行的设备 MQTT 认证逻辑。

### 推荐认证方案
先采用简单静态认证：
- clientId = deviceCode
- username = deviceCode
- password = deviceSecret

### 允许修改模块
- spring-boot-iot-device
- spring-boot-iot-message
- spring-boot-iot-admin（仅在确有必要时补配置）
- spring-boot-iot-framework（仅在确有必要时补配置支持）

### 本轮建议新增或完善
- DeviceAuthService
- DeviceAuthServiceImpl
- 设备认证相关最小配置
- 如 Broker 不支持直接嵌入校验，则先保留平台内验证入口骨架

### 本轮必须完成
1. 平台具备设备认证服务接口
2. 可以根据 deviceCode / password 校验设备合法性
3. 为 Broker 集成或平台消费侧校验预留清晰扩展点

### 本轮不要实现
- 动态签名
- nonce/timestamp 防重放
- 复杂令牌鉴权

### 验收标准
- 设备认证逻辑可被独立调用
- deviceCode / deviceSecret 校验成功与失败路径清晰
- 不破坏现有设备管理逻辑

---

## Phase 2 Task 4：设备会话与在线状态

### 目标
通过 Redis + 数据库维护设备 MQTT 会话和在线状态。

### 允许修改模块
- spring-boot-iot-device
- spring-boot-iot-framework
- spring-boot-iot-message

### 本轮建议新增
- DeviceSessionService
- DeviceSessionServiceImpl

### 本轮必须完成
1. 设备上线时写入 Redis 会话
2. 设备收到上报时刷新 lastSeenTime
3. 更新 iot_device 中的：
    - online_status
    - last_online_time
    - last_report_time
4. 预留下线接口或方法骨架

### 推荐 Redis Key
- iot:device:session:{deviceCode}

### 本轮不要实现
- 复杂会话表
- 多端互踢
- 集群会话一致性优化

### 验收标准
- 设备消息进入主链路时可刷新在线状态
- Redis 中存在会话信息
- 数据库状态同步更新

---

## Phase 2 Task 5：MQTT 上行链路联调

### 目标
验证真实 MQTT 上报能够复用现有主链路完成：
- 日志落库
- 属性更新
- 在线状态更新

### 允许修改模块
- spring-boot-iot-message
- spring-boot-iot-protocol
- spring-boot-iot-device
- spring-boot-iot-admin（如需最小测试配置）

### 本轮必须完成
1. EMQX / Mosquitto 环境可接入
2. MQTT 上报后进入：
    - MqttMessageConsumer
    - UpMessageDispatcher
    - MqttJsonProtocolAdapter
    - DeviceMessageServiceImpl
3. 验证：
    - iot_message_log 插入
    - iot_device_property 更新
    - iot_device 在线状态更新

### 本轮不要实现
- 下行命令闭环
- 网关子设备
- 复杂异常重试

### 验收标准
- 能提供一组 MQTT 发布样例
- 能通过查询接口看到属性和日志变化
- HTTP 链路与 MQTT 链路都可用

---

## Phase 2 Task 6：MQTT 下行发布骨架

### 目标
建立平台向设备下发 MQTT 消息的基础发布能力。

### 允许修改模块
- spring-boot-iot-message
- spring-boot-iot-protocol
- spring-boot-iot-device（如确有必要）
- spring-boot-iot-admin

### 本轮建议新增或完善
- MqttDownMessagePublisher
- 最小下行消息发布接口或服务

### 本轮必须完成
1. 平台可向指定 topic 发布消息
2. 支持最小 QoS 配置
3. 可调用 ProtocolAdapter.encode 进行编码（若当前只需最小骨架，也可先保留接口）

### 本轮不要实现
- 完整指令任务状态机
- 重试队列
- ACK 闭环

### 验收标准
- 能构造并发布一条测试下行消息
- 发布组件与主链路解耦清晰

---

## Phase 2 Task 7：网关与子设备 Topic 预留

### 目标
为未来网关代子设备接入预留 topic 规范和解析扩展点，但不做复杂业务实现。

### 推荐预留 topic
- /sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/post

### 本轮允许修改模块
- spring-boot-iot-message
- spring-boot-iot-protocol
- docs（若需要同步协议说明）

### 本轮必须完成
1. 为子设备 topic 解析预留结构
2. 不破坏现有直连设备 topic 解析

### 本轮不要实现
- 完整子设备管理逻辑
- 复杂拓扑关系联动

### 验收标准
- 架构上具备扩展点
- 当前代码对未来网关场景有清晰预留

---

## Phase 2 收口任务

### 目标
在 MQTT 骨架完成后，对文档、配置和测试做一次统一收口。

### 建议处理内容
1. 更新 docs/04-api.md
2. 更新 docs/05-protocol.md
3. 更新 docs/device-simulator.md
4. 更新 docs/真实环境测试与验收手册.md
5. 更新 docs/12-change-log.md
6. 整理 config 中未合并文件
7. 增加 MQTT 联调样例

### 验收标准
- 仓库进入 clean 状态
- 文档与代码一致
- MQTT / HTTP 两条链路都可演示
