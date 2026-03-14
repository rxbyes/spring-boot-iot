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
- 新增 `DeviceHttpReportE2EIntegrationTest`，覆盖产品新增、设备新增、HTTP 上报、属性查询、消息日志查询、非法协议、不存在设备
- 新增 `application-e2e.yml` 与 `schema-e2e.sql`，让 E2E 场景使用 H2 内存数据库独立运行
- 将 E2E 测试改为 `MockMvc` 驱动，规避当前环境随机端口监听限制
- 修复 Boot 4 下路径参数显式命名问题，确保查询接口在当前编译参数下可正常绑定

### 文档与配置同步
- 同步根目录 `config/application-dev.yml`、`application-test.yml`、`application-prod.yml` 与启动模块环境配置
- 更新 API 文档，反映当前已实现接口与错误返回
- 更新测试场景文档，补充 HTTP 上报主链路的自动化验证与手工联调步骤
