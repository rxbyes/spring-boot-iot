# AGENTS.md

## 项目
spring-boot-iot

## 基础包
com.ghlzm.iot

## 使命
基于 Spring Boot 4 + Java 17 构建并持续维护一个模块化 IoT 网关平台，并以真实共享开发环境作为验收基线。

## 当前状态
第一至第三阶段主链路是长期稳定基线。第四阶段风险平台能力仍在推进中，但已经具备可用的真实环境基线。

### 当前构建模块基线
当前父 `pom.xml` 激活 `11` 个模块：
- `spring-boot-iot-common`
- `spring-boot-iot-framework`
- `spring-boot-iot-auth`
- `spring-boot-iot-system`
- `spring-boot-iot-device`
- `spring-boot-iot-protocol`
- `spring-boot-iot-message`
- `spring-boot-iot-rule`
- `spring-boot-iot-alarm`
- `spring-boot-iot-report`
- `spring-boot-iot-admin`

仓库里仍然保留了 `spring-boot-iot-gateway`、`spring-boot-iot-telemetry`、`spring-boot-iot-ota` 等额外模块目录，但当前活跃构建模块仍以父 `pom.xml` 为准。

### 已验证业务基线
当前已验证的基线包括：
- 产品新增 / 查询
- 设备新增 / 查询
- HTTP 模拟设备上报
- MQTT 真实接入
- 通过 `mqtt-json` 进行协议解码
- 消息日志持久化
- 最新属性更新
- 设备在线状态更新
- 告警中心（告警列表、告警详情、告警确认、告警抑制、告警关闭）
- 事件处置（事件列表、事件详情、工单派发、工单接收 / 开始 / 完成、事件反馈、事件关闭）
- 风险点管理（风险点增删改查、风险点绑定）
- 阈值规则配置（规则增删改查）
- 联动规则与应急预案（规则增删改查、预案增删改查）
- 报表分析（风险趋势、告警统计、事件闭环、设备健康）
- 组织管理（树结构、增删改查）
- 用户管理（用户增删改查、密码重置）
- 角色管理（角色增删改查、用户角色查询）
- 区域管理
- 字典管理
- 通知渠道管理
- 审计日志管理

当前代码中还包含风险监测基线：
- `/risk-monitoring`
- `/risk-monitoring-gis`
- `spring-boot-iot-ui/src/api/riskMonitoring.ts`
- `spring-boot-iot-alarm/.../RiskMonitoringController.java`

但除非 [docs/19-第四阶段交付边界与复验进展.md](docs/19-%E7%AC%AC%E5%9B%9B%E9%98%B6%E6%AE%B5%E4%BA%A4%E4%BB%98%E8%BE%B9%E7%95%8C%E4%B8%8E%E5%A4%8D%E9%AA%8C%E8%BF%9B%E5%B1%95.md) 已同步更新，否则这部分风险监测基线暂不计入正式交付范围。

## 真实环境规则
后续所有验收工作都必须使用 `spring-boot-iot-admin/src/main/resources/application-dev.yml`，或使用覆盖该文件的环境变量。

禁止重新引入以下已废弃验收路径：
- 旧 H2 验收 profile
- 独立 H2 schema 验收脚本
- 仅 H2 验收路径
- 已废弃的前端浏览器自动化验收路径

如果环境访问受阻，必须明确报告环境阻塞，不得用已废弃的 H2 回退链路替代真实环境验收。

## 文档维护规则
- 任何影响行为、API、流程、页面结构、启动步骤、校验流程、配置预期或产品定位的前后端改动，都必须原位更新现有文档。
- 必须同步更新 `docs/` 下对应文档。
- 必须检查 `README.md` 和 `AGENTS.md` 是否也需要同步更新。
- 不得创建 `README-v2.md`、`api-new.md`、`new-frontend-doc.md` 之类的平行替代文档。
- 该规则适用于所有编码助手和编码模型，包括 Codex、Qwen Code 等。

## 智能助手协作入口规则
- 你本人日常发任务时，优先使用 `docs/skills/ai-task-intake/SKILL.md` 中的“你本人专用版六条超短清单”。
- 如果六条超短清单装不下，再使用 `docs/09-GPT接管提示模板.md` 中的任务卡模板。
- 只有任务跨模块、跨验收、跨数据库，或短任务卡仍然装不下时，再使用 `docs/template/README.md` 索引的长模板。

## 工作区路径兼容规则
- 共享 Windows 10 环境的工作区根目录可能是 `E:\idea\ghatg\spring-boot-iot`。
- 其他环境可能使用不同的绝对路径。
- 不得把某一个绝对工作区路径写死回脚本或文档，作为唯一合法路径。
- 优先通过当前脚本位置、当前工作目录或环境配置推导仓库根目录。
- 当文档需要展示绝对路径示例时，必须明确标注 `E:\idea\ghatg\spring-boot-iot` 只是 Windows 共享环境示例，而不是通用固定路径。

## 前端编码与一致性规则
- 任何 `spring-boot-iot-ui` 下的页面或样式改动，都必须保证 UTF-8 可读，不得把终端乱码写进 `.vue`、`.ts`、`.css`、`.json`、`.md` 文件。
- 在 Windows 终端编辑前端文件前，优先使用 UTF-8 查看 / 校验方式，例如 `chcp 65001` 加 `Get-Content -Encoding UTF8`，确保终端显示内容与文件真实内容一致。
- 修改前端文本、标签、占位符、注释或文档后，必须自检是否出现 `鍒�`、`褰�`、`璇�`、`鐢�` 这类乱码，发现后必须修复。
- 新的页面优化工作必须优先复用现有共享页面模式：`PanelCard`、`StandardPagination`、`useServerPagination`、`StandardTableToolbar`、`StandardTableTextColumn`、`StandardDetailDrawer`、`StandardFormDrawer`、`StandardDrawerFooter`、`confirmAction`、共享全局列表样式和现有设计令牌。若现有标准模式已经适配，不要再新增页面私有列表 / 分页 / 详情弹层样式。
- 总览、工作台、抽屉和确认弹窗交互必须保持统一品牌 / 强调配色体系。除非产品需求明确记录例外，否则不要再为单页引入新的蓝 / 橙 / 紫色私有配色。
- 如果前端改动引入或暴露了样式漂移、重复列表布局或分页行为不一致问题，结束任务前必须把问题和预防规则记录到 `docs/15-前端优化与治理计划.md`。

## 编码前必读

### 所有任务的最小阅读集
- `README.md`
- `docs/README.md`
- `docs/01-系统概览与架构说明.md`
- `docs/02-业务功能与流程说明.md`
- `docs/03-接口规范与接口清单.md`
- `docs/04-数据库设计与初始化数据.md`
- `docs/07-部署运行与配置说明.md`
- `docs/08-变更记录与技术债清单.md`

### 按需补读
- 测试 / 验收 / 回归：`docs/05-自动化测试与质量保障.md`、`docs/真实环境测试与验收手册.md`、`docs/21-业务功能清单与验收标准.md`
- 前端工作：`docs/06-前端开发与CSS规范.md`、`docs/15-前端优化与治理计划.md`
- MQTT / 协议 / 载荷解析：`docs/05-protocol.md`、`docs/14-MQTTX真实环境联调手册.md`
- 可观测 / Trace / 通知：`docs/11-可观测性、日志追踪与消息通知治理.md`
- 帮助中心 / 系统内容治理：`docs/12-帮助文档与系统内容治理.md`
- 多租户 / 数据权限 / 组织范围：`docs/13-数据权限与多租户模型.md`
- 第四阶段范围或交付边界：`docs/19-第四阶段交付边界与复验进展.md`、`docs/21-业务功能清单与验收标准.md`
- 阶段规划 / 下一轮迭代：`docs/16-阶段规划与迭代路线图.md`、`docs/19-第四阶段交付边界与复验进展.md`
- 智能助手协作 / 接手模板：`docs/skills/ai-task-intake/SKILL.md`、`docs/09-GPT接管提示模板.md`、`docs/template/README.md`

### 不再视为主编码依赖
- 兼容入口页
- `docs/archive/*`
- 历史问题台账 / 复盘记录
- 之前位于 `docs/template/*` 下的薄包装页；如需入口请使用 `docs/template/README.md`

## 硬约束
- 项目名必须保持：`spring-boot-iot`
- 基础包必须保持：`com.ghlzm.iot`
- 第一阶段必须保持模块化单体
- `spring-boot-iot-admin` 是唯一启动模块
- 不得破坏模块边界
- 不得把持久化逻辑移入协议适配层
- 不得把业务逻辑写进 Controller
- 除非确有必要，不要引入重型依赖

## 模块边界
- `spring-boot-iot-common`：常量、异常、响应模型、工具类
- `spring-boot-iot-framework`：配置、安全、Redis、MyBatis、全局处理器
- `spring-boot-iot-auth`：只负责认证
- `spring-boot-iot-system`：用户、角色、组织、区域、字典、渠道、审计
- `spring-boot-iot-device`：产品、设备、影子、属性、消息日志
- `spring-boot-iot-gateway`：网关与子设备拓扑
- `spring-boot-iot-protocol`：协议适配器、协议模型、编解码
- `spring-boot-iot-message`：接入入口与分发，仅负责入口和调度
- `spring-boot-iot-telemetry`：历史遥测查询与存储抽象
- `spring-boot-iot-rule`：规则引擎
- `spring-boot-iot-alarm`：告警中心、事件、风险点、规则、预案、风险监测
- `spring-boot-iot-report`：报表分析
- `spring-boot-iot-ota`：OTA 升级
- `spring-boot-iot-admin`：应用装配与启动

## 代码风格
- Controller 只处理请求 / 响应
- Service 负责编排
- Mapper 负责数据库访问
- 业务错误使用 `BizException`
- 统一 API 响应使用 `R`
- 命名保持与文档一致
- 核心逻辑意图不明显时，补短小中文注释
- 优先提交小而聚焦的改动

## 编码前
1. 先总结任务
2. 列出受影响模块
3. 说明实现计划
4. 说明假设

## 编码后
1. 列出变更文件
2. 说明改了什么
3. 说明如何运行或验证
4. 列出未完成部分
5. 如果行为发生变化，必须原位更新现有文档
6. 说明本次更新了哪些文档，包括是否更新了 `README.md` 和 `AGENTS.md`

## 推荐命令
- 构建：`mvn -s .mvn/settings.xml clean install -DskipTests`
- 启动应用（macOS / Linux、Windows CMD）：`mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev`
- 启动应用（Windows PowerShell）：`mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run "-Dspring-boot.run.profiles=dev"`
- 后端验收：`powershell -ExecutionPolicy Bypass -File scripts/start-backend-acceptance.ps1`
- 前端验收：`powershell -ExecutionPolicy Bypass -File scripts/start-frontend-acceptance.ps1`
- 测试：`mvn -s .mvn/settings.xml test`

## 阶段执行顺序

### 第一阶段
1. 创建 Maven 多模块结构
2. 增加基础基础设施类
3. 增加数据库实体和 Mapper
4. 增加 Service 和 Controller
5. 实现 HTTP 上报链路
6. 验证属性和消息日志持久化
7. 验证在线状态更新

### 第二阶段
1. 实现 MQTT 接入骨架
2. 实现 MQTT Topic 解析
3. 实现基础设备认证
4. 实现设备会话和在线状态处理
5. 完成真实 MQTT 上行验收
6. 实现最小 MQTT 下行发布
7. 预留子设备 Topic 解析扩展点

### 第三阶段
1. 实现命令闭环
2. 实现网关 / 子设备业务闭环
3. 实现基础规则引擎

### 第四阶段
1. 实现告警中心基线
2. 实现事件处置基线
3. 实现风险点管理
4. 实现阈值规则配置
5. 实现联动规则和应急预案
6. 实现报表分析
7. 实现系统治理
8. 只有在进度文档同步更新后，才交付风险监测和 GIS

## 完成定义

### 第一阶段
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` 通过
- 使用 `application-dev.yml` 的 HTTP 主链路真实环境验收通过
- 以下 API 必须保持可用：
  - `POST /device/product/add`
  - `GET /device/product/{id}`
  - `POST /device/add`
  - `GET /device/{id}`
  - `GET /device/code/{deviceCode}`
  - `POST /message/http/report`
  - `GET /device/{deviceCode}/properties`
  - `GET /device/{deviceCode}/message-logs`

### 第二阶段
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` 通过
- 真实环境下 MQTT 标准 Topic 上行验收通过
- 真实环境下旧 `$dp` 兼容链路验收通过
- 真实环境下 MQTT 最小下行发布验收通过

### 第三阶段
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` 通过
- 命令闭环真实环境验收通过
- 网关 / 子设备真实环境验收通过
- 规则引擎真实环境验收通过

### 第四阶段
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` 通过
- 告警中心真实环境验收通过
- 事件处置真实环境验收通过
- 风险配置真实环境验收通过
- 报表分析真实环境验收通过
- 系统治理真实环境验收通过
- `docs/19-第四阶段交付边界与复验进展.md` 与 `docs/21-业务功能清单与验收标准.md` 必须与真实已交付范围保持一致

## 已知环境说明
- 在部分 JDK 17 环境下，`DeviceMessageServiceImplTest` 仍可能失败，因为 Mockito inline mock maker 无法自附加 ByteBuddy agent。
- 除非有真实业务回归证据，否则把它视为本地测试环境问题。

## 鉴权基线说明（2026-03-16）
- `/api/auth/login` 是 Web 客户端默认登录入口。
- `/message/http/report`、`/api/cockpit/**`、actuator 与 swagger / doc 端点继续保持公开。
- 其他 API 默认都受 JWT Bearer 鉴权保护。
- 前端在登录后应附加 `Authorization: Bearer <token>`，并在收到 `401` 时清理本地认证状态。
