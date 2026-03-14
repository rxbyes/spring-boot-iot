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
### 更新
- 补充设备管理 Service 与 Controller
- 补充 ProductModel 实体与 Mapper
- 将设备属性查询、消息日志查询下沉到 Service
- 增强 HTTP 上报链路的协议校验、产品匹配、属性更新与在线状态更新
- 放开 Phase 1 调试所需的 device/message 接口访问
- 修复 Boot 4 下 PathVariable/RequestParam 参数名绑定问题
- 补充 DeviceMessageServiceImpl 单元测试（成功链路与异常链路）
- 新增 DeviceHttpReportE2EIntegrationTest 端到端集成测试
- 将端到端测试拆分为成功链路、非法协议、不存在设备三条独立用例
- 新增 application-e2e.yml 测试配置，支持外部 MySQL / Redis / MQTT 环境变量注入
- 统一 application-*.yml 配置口径，支持环境变量覆盖并默认对齐 spring_boot_iot
- 修正 API 文档中的 payload JSON 示例转义问题
- 更新 Phase 1 路线图状态与测试场景文档
