# Phase 3 路线图

本文档用于规划 Phase 3 的开发方向、优先级、模块影响范围、数据表扩展建议和任务拆解。

当前文档只做路线设计，不代表这些能力已经实现。

## 1. Phase 3 总目标

在 Phase 1 已完成的设备管理与 HTTP 主链路、以及 Phase 2 已完成的 MQTT 上下行基础上，进入"平台能力闭环"阶段。

Phase 3 的总体目标是：
- 把"设备上报 -> 平台处理 -> 平台下发 -> 设备反馈"逐步收敛成可追踪的业务闭环
- 把"直连设备能力"扩展为"网关 / 子设备能力"
- 为规则、告警、自动化联动建立最小可运行基础
- 保持当前模块化单体结构不被破坏，继续以 `spring-boot-iot-admin` 为唯一启动模块

## 1.1 前端优化目标

Phase 3 的前端优化目标是：
- 把"风险监测驾驶舱"的设计风格扩展到所有业务页面
- 实现统一的科技感视觉风格（暗色系 + 渐变 + 发光效果）
- 实现角色驱动的业务视图（一线人员/运维人员/管理人员）
- 实现数据驱动的风险/运维/产品评分系统
- 实现完整的业务链路（从数据采集到风险判定）

## 2. 当前项目已具备的能力基础

### 2.1 Phase 1 已完成基础
- 产品管理
- 设备管理
- HTTP 模拟设备上报
- 协议解析
- 消息日志持久化
- 最新属性更新
- 设备在线状态更新

### 2.2 Phase 2 已完成基础
- MQTT 接入骨架
- MQTT topic 解析
- 设备认证基础版
- 设备会话与在线状态基础版
- MQTT 上行真实联调
- MQTT 下行最小发布能力
- 子设备 topic 规范与解析扩展点预留

### 2.3 对 Phase 3 最有价值的现有积累
- 已有统一主链路：
  - `RawDeviceMessage -> UpMessageDispatcher -> ProtocolAdapter -> DeviceMessageService`
- 已有 MQTT 下行发布骨架
- 已有 topic 解析扩展点
- 已有设备在线状态与 Redis 会话基础
- 已有消息日志表和最新属性表，可作为规则和闭环能力的基础输入

## 3. Phase 3 候选方向

## 3.1 方向一：指令闭环能力

### 目标
- 让平台下发的 MQTT 指令具备最小可追踪闭环
- 能记录“已下发、待响应、成功、失败、超时”等最小状态
- 支持直连设备命令生命周期跟踪

### 影响模块
- `spring-boot-iot-message`
- `spring-boot-iot-protocol`
- `spring-boot-iot-device`
- `spring-boot-iot-admin`

### 需要新增或激活的模块
- 继续使用 `spring-boot-iot-message`
- 继续使用 `spring-boot-iot-protocol`
- 在 `spring-boot-iot-device` 中补最小命令记录服务
- 暂不要求激活 `spring-boot-iot-rule`

### 可能需要新增的数据表
- `iot_command_record`

建议字段：
- `id`
- `command_id`
- `device_id`
- `device_code`
- `product_key`
- `gateway_device_code`
- `sub_device_code`
- `topic`
- `command_type`
- `payload`
- `qos`
- `status`
- `send_time`
- `ack_time`
- `timeout_time`
- `error_message`

### 推荐优先级
- `P1`

### 风险点
- 设备侧 ACK 格式不统一
- 不同协议或 topic 的回执语义不一致
- 若过早引入复杂状态机会抬高整体复杂度

### 适合的实现策略
- 先做“最小命令闭环”
- 先只覆盖直连设备 MQTT
- 先只做单次发送 + 单次状态更新
- 暂不做重试、补偿、批量命令

## 3.2 方向二：网关 / 子设备业务闭环

### 目标
- 让当前已预留的子设备 topic 解析能力真正进入业务链路
- 建立网关、子设备、拓扑关系的最小运行模型
- 支持网关代子设备上报和下发的基础业务闭环

### 影响模块
- `spring-boot-iot-gateway`
- `spring-boot-iot-message`
- `spring-boot-iot-protocol`
- `spring-boot-iot-device`
- `spring-boot-iot-admin`

### 需要新增或激活的模块
- 激活 `spring-boot-iot-gateway`
- 继续使用 `spring-boot-iot-message`
- 继续使用 `spring-boot-iot-protocol`
- `spring-boot-iot-device` 需要最小配合查询接口

### 可能需要新增的数据表
- `iot_gateway`
- `iot_gateway_topology`

建议字段：

`iot_gateway`
- `id`
- `gateway_device_id`
- `gateway_device_code`
- `gateway_status`
- `last_sub_report_time`
- `remark`

`iot_gateway_topology`
- `id`
- `gateway_device_id`
- `gateway_device_code`
- `sub_device_id`
- `sub_device_code`
- `product_id`
- `product_key`
- `bind_status`
- `bind_time`
- `unbind_time`

### 推荐优先级
- `P2`

### 风险点
- 子设备编码归属与直连设备编码归属冲突
- 网关代发场景下在线状态语义更复杂
- 若拓扑、认证、下行三件事同时推进，容易失控

### 适合的实现策略
- 先做“最小拓扑关系 + topic 入链”
- 先不做复杂拓扑同步
- 先不做动态发现和自动注册
- 先只支持静态绑定的网关 / 子设备关系

## 3.3 方向三：规则引擎基础版

### 目标
- 让平台具备“属性变化 / 事件上报 -> 条件判断 -> 动作执行”的最小自动化能力
- 为后续告警、自动控制、联动通知建立基础框架

### 影响模块
- `spring-boot-iot-rule`
- `spring-boot-iot-device`
- `spring-boot-iot-message`
- `spring-boot-iot-telemetry`
- `spring-boot-iot-admin`

### 需要新增或激活的模块
- 激活 `spring-boot-iot-rule`
- 视方案决定是否同步激活 `spring-boot-iot-telemetry`
- 暂不强依赖 `spring-boot-iot-alarm`

### 可能需要新增的数据表
- `iot_rule_chain`
- 可选：`iot_rule_node`
- 可选：`iot_rule_trigger_record`

建议字段：

`iot_rule_chain`
- `id`
- `rule_code`
- `rule_name`
- `trigger_type`
- `trigger_config`
- `action_type`
- `action_config`
- `status`
- `tenant_id`

`iot_rule_trigger_record`
- `id`
- `rule_id`
- `device_code`
- `trigger_time`
- `trigger_payload`
- `execute_status`
- `error_message`

### 推荐优先级
- `P3`

### 风险点
- 规则条件表达式设计过早复杂化
- 若没有稳定的指令闭环和网关闭环，规则动作缺少可靠落点
- 若过早引入可视化编排，会偏离“最小可运行”目标

### 适合的实现策略
- 先做“单条件 + 单动作”的最小规则
- 先只支持属性阈值触发
- 动作先限定为：
  - 写规则触发记录
  - 触发一条平台下行命令
- 不先做复杂节点编排 UI

## 4. 推荐优先级

推荐优先级如下：

1. 指令闭环能力
2. 网关 / 子设备业务闭环
3. 规则引擎基础版

## 5. 推荐的 Phase 3 执行顺序

### 第一阶段：先补指令闭环
原因：
- 当前已经有 MQTT 下行发布骨架
- 技术链路最短，最容易形成“上报 + 下发 + 回执”的业务闭环
- 对现有模块冲击最小
- 能直接提升平台演示价值和调试能力

### 第二阶段：再做网关 / 子设备业务闭环
原因：
- Phase 2 已经把子设备 topic 规范和解析扩展点预留好了
- 这一步适合在命令闭环稳定后推进
- 可避免把“拓扑 + 认证 + 命令 + 规则”四件事同时推进

### 第三阶段：最后补规则引擎基础版
原因：
- 规则引擎依赖上游事件和下游动作都足够稳定
- 若命令闭环和网关链路未稳定，规则很容易沦为演示壳层
- 先把设备通信闭环做好，规则引擎的落地成本更低

## 6. 建议拆分的 Task

### Task 1：命令记录与状态模型
- 新增 `iot_command_record`
- 下行发布后写入命令记录
- 定义最小状态枚举

### Task 2：MQTT 回执接入与命令状态更新
- 识别 `property/reply`、`service/reply`
- 解析回执并回填命令状态
- 支持最小超时判定

### Task 3：网关与子设备静态拓扑
- 建立网关与子设备绑定关系
- 提供最小查询能力
- 只支持静态建档

### Task 4：子设备上报入链
- 让子设备 topic 进入业务链路
- 明确设备身份解析与归属策略
- 补最小联调样例

### Task 5：子设备下发入链
- 支持平台向子设备下发
- 由网关 topic 代理发送
- 先不做网关离线补发

### Task 6：规则引擎最小触发器
- 支持属性阈值触发
- 支持最小动作执行
- 写规则触发记录

### Task 7：规则动作联动下行命令
- 规则触发后调用现有下行发布能力
- 形成“规则 -> 指令”闭环

## 7. 哪些能力继续保持“最小可运行实现”

### 建议继续最小实现的能力
- 指令闭环
  - 先只支持单设备、单命令、单回执
- 子设备业务
  - 先只支持静态拓扑和基础上下行
- 规则引擎
  - 先只支持单条件、单动作

### 适合后续增强的能力
- 命令重试与补偿
- 指令批量调度
- 子设备动态发现与自动注册
- 图形化规则编排
- 告警中心联动
- OTA / 文件 / 数字孪生联动

## 8. 推荐先走的路线

我推荐先走“指令闭环能力”这条路线。

原因：
- 它直接建立在 Phase 2 已完成的 MQTT 下行发布能力之上
- 影响模块较少，风险最可控
- 最容易形成可演示的业务价值
- 做完后既能服务直连设备，也能为后续网关 / 子设备和规则引擎复用
- 若先做规则引擎或网关闭环，整体面会更大，且会反过来依赖命令结果追踪能力

## 9. Phase 3 规划结论

Phase 3 不建议一开始并行推进多个大方向。

更稳的方式是：
1. 先补命令闭环
2. 再接网关 / 子设备闭环
3. 最后接规则引擎基础版

这样可以在保持当前 Phase 1 / Phase 2 稳定成果的前提下，逐步把平台推向真正可运营的 IoT 网关平台。
