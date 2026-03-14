# 前端调试台

## 目标
为当前 `spring-boot-iot` Phase 1 后端能力提供一套独立的 Vue 3 调试前端，覆盖：
- 产品管理联调
- 设备管理联调
- HTTP 模拟设备上报
- 设备最新属性查询
- 设备消息日志查询

同时为后续这些方向预留前端扩展面：
- 点位数据图表
- 数字孪生
- 网关与子设备拓扑
- 规则 / 告警 / OTA 操作面板

## 目录
前端工作区位于：

`spring-boot-iot-ui`

说明：
- 该目录不加入 Maven reactor
- 采用 Vue 3 + Vite 风格工程结构
- 布局组织参考 `vue-element-admin` 的后台导航和工作台节奏
- 视觉设计采用更强的 IoT 科技感，突出协议、设备、状态和调试轨迹
- 当前已经接入 `Element Plus` 组件体系和 `ECharts` 图表能力

## 页面结构

### 1. 调试驾驶舱
- 展示当前主链路和最近调试记录
- 显示 Phase 1 核心能力、模块数、接口数和未来预留入口
- 作为统一入口跳转到具体工作台
- 增加类似后台系统的标签视图，保留最近访问页面上下文

### 2. 产品工作台
- 对接 `POST /device/product/add`
- 对接 `GET /device/product/{id}`
- 用于验证产品模板创建和读取

### 3. 设备工作台
- 对接 `POST /device/add`
- 对接 `GET /device/{id}`
- 对接 `GET /device/code/{deviceCode}`
- 用于验证设备建档、状态字段和最近上报时间

### 4. HTTP 上报实验台
- 对接 `POST /message/http/report`
- 支持快速切换属性、状态、事件三类示例 payload
- 自动推导推荐 topic
- 提供 curl 预览，方便与文档或终端联调命令对照

### 5. 设备洞察
- 对接 `GET /device/code/{deviceCode}`
- 对接 `GET /device/{deviceCode}/properties`
- 对接 `GET /device/{deviceCode}/message-logs`
- 聚合设备详情、属性快照和消息日志，作为图表与数字孪生前置数据面板
- 当前已增加基于最近消息日志的 `ECharts` 趋势图，后续可直接衔接时序库查询

### 6. 未来实验室
- 展示点位图表、数字孪生、拓扑与规则的预留设计位
- 说明当前 Phase 1 页面和未来模块的衔接方式

### 7. 文件调试台
- 对接 `GET /device/{deviceCode}/file-snapshots`
- 对接 `GET /device/{deviceCode}/firmware-aggregates`
- 用于验证表 C.3 文件快照和表 C.4 固件分包聚合是否已经被后端消费
- 展示 Redis 快照、分包进度、重组长度和 MD5 校验结果

## 运行方式
1. 启动后端：`mvn -pl spring-boot-iot-admin spring-boot:run`
2. 进入前端目录：`cd spring-boot-iot-ui`
3. 安装依赖：`npm install`
4. 启动开发环境：`npm run dev`

默认行为：
- 若 `VITE_API_BASE_URL` 为空，则前端走 Vite 本地代理
- 默认代理目标为 `http://localhost:9999`

可通过 `spring-boot-iot-ui/.env.example` 参考配置：
- `VITE_API_BASE_URL`
- `VITE_PROXY_TARGET`

## 当前设计原则
- 不改动后端主链路
- 前后端保持独立目录和清晰职责
- 页面优先服务联调效率，其次再承载展示感
- 所有未来功能入口都基于已有数据面板扩展，不推翻现有结构
- 文件 / 固件调试台优先服务 MQTT C.3 / C.4 联调，不提前扩展成完整 OTA 运维面板
- 关键查询状态逐步同步到 URL，方便分享调试链接和恢复现场

## 已知说明
- 当前仓库本地若仍停留在 Node 12，则无法直接安装这套前端依赖；需要先切换到 Node 20.19+ 再执行安装与构建
- 当前前端主要服务开发和联调场景，后续如需嵌入 `spring-boot-iot-admin` 静态资源，可再做打包接入
