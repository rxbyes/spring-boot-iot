# 变更记录

## 2026-03-13
### 新增
- 项目总览
- 架构文档
- 模块结构
- 数据库设计
- API 文档
- 协议规范
- 物模型说明
- 消息流说明
- Codex 执行规则
- 路线图与测试场景

## 2026-03-14
### Phase 1 完成项
- 收敛一期模块为 `common`、`framework`、`auth`、`system`、`device`、`protocol`、`message`、`admin` 八个模块
- 保持 `spring-boot-iot-admin` 为唯一启动模块，其他一期模块作为模块化单体内部业务模块存在
- 补齐基础设施骨架：`R`、`PageResult`、`BizException`、`BaseEntity`、`GlobalExceptionHandler`、`SecurityConfig`、`IotProperties`
- 补齐一期核心实体与 Mapper：`Product`、`ProductModel`、`Device`、`DeviceProperty`、`DeviceMessageLog`
- 补齐产品、设备、设备属性、设备消息日志的最小 Service / Controller 骨架
- 打通一期 HTTP 上报主链路：HTTP 上报 -> 消息分发 -> 协议解析 -> 原始消息落库 -> 最新属性更新 -> 设备在线状态更新
- 修复 `UpMessageDispatcher` 冲突标记问题，恢复到可编译状态
- 为核心接入、分发、协议、服务代码补充中文注释，便于后续持续维护

### 测试与验证
- 新增 `DeviceMessageServiceImplTest`，覆盖上报成功链路与关键异常场景
- 新增一期 HTTP 主链路验收测试类，覆盖产品新增、设备新增、HTTP 上报、属性查询、消息日志查询、非法协议、不存在设备
- 新增旧版 H2 验收 profile 与 schema 脚本，用于当时的独立验收场景
- 将旧版主链路验收测试改为 `MockMvc` 驱动，规避当时环境随机端口监听限制
- 修复 Boot 4 下路径参数显式命名问题，确保查询接口在当前编译参数下可正常绑定

### 文档与配置同步
- 同步根目录 `config/application-dev.yml`、`application-test.yml`、`application-prod.yml` 与启动模块环境配置
- 更新 API 文档，反映当前已实现接口与错误返回
- 更新测试场景文档，补充 HTTP 上报主链路的自动化验证与手工联调步骤
- 新增 MQTTX 共享环境现场联调手册，统一 `application-dev.yml + MQTTX` 的联调口径

## 2026-03-15
### MQTT 真实联调收口
- 完成 Phase 2 Task 5 的真实 MQTT 上行联调
- 在共享 `dev` 环境中完成一次标准 topic 的真实发布与验证
- 新增 `docs/14-mqtt-live-runbook.md`，沉淀真实联调运行手册
- 在测试场景文档中补充一组真实成功样例，便于后续复用

### Phase 2 Task 6 最小下行发布
- 新增 `POST /message/mqtt/down/publish`，建立最小 MQTT 下行发布入口
- 新增 `DownMessageService` 与 `MqttDownMessagePublisher` 编排，按设备和产品信息自动推导推荐下行 topic
- 复用 `ProtocolAdapter.encode(...)` 编码 `DeviceDownMessage`
- 支持最小 QoS 与 retained 参数透传
- 在共享 `dev` 环境完成一次真实下行发布验证，独立订阅端已收到推荐 topic 消息

### Phase 2 Task 7 网关子设备 Topic 预留
- 为网关代子设备通信补充推荐 topic 规范，覆盖子设备上报和下发
- 扩展 `MqttTopicParser`，可识别直连设备 topic、子设备 topic 和历史 `$dp`
- 在 `RawDeviceMessage` 与 `ProtocolContext` 中预留 `routeType`、`gatewayDeviceCode`、`subDeviceCode`
- 保持默认订阅范围不变，避免在未落地子设备业务前影响当前直连设备链路

### Phase 2 文档收口
- 统一整理 MQTT API、协议规范、消息流、测试场景和现场运行手册
- 新增 `docs/15-phase2-summary.md`，汇总 Phase 2 已完成能力、未完成能力和交接口径
- 将仓库文档口径收敛到“可交接、可演示、可继续扩展”
