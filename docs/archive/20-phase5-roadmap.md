# Phase 5 研发计划：平台增强与优化

## 概述

Phase 5 将在 Phase 4 完成的风险监测预警处置平台基础上，进行平台增强与优化，重点实现首页驾驶舱增强、风险监测模块、设备中心增强等功能，并进行全面的测试与优化。

## 时间计划

- **开始时间**: 2026-03-16
- **预计完成时间**: 2026-03-31
- **总工期**: 15 个工作日

## 任务分解

### 任务 1: 首页驾驶舱增强 (预计 3 个工作日)

#### 1.1 KPI 指标卡片

**后端实现**:
- 在 `spring-boot-iot-report` 模块中新增 `CockpitService`
- 新增 API:
  - `GET /report/cockpit/device-total` - 设备总数
  - `GET /report/cockpit/device-online` - 在线设备数
  - `GET /report/cockpit/today-alarm` - 今日告警数
  - `GET /report/cockpit/unclosed-event` - 未关闭事件数

**前端实现**:
- 在 `CockpitView.vue` 中新增 KPI 指标卡片组件
- 使用 `MetricCard` 组件展示数据
- 实现数据自动刷新（每 30 秒）

#### 1.2 图表模块

**后端实现**:
- 在 `CockpitService` 中新增方法:
  - `getRiskTrendData()` - 风险趋势数据（最近 7 天）
  - `getAlarmLevelDistribution()` - 告警等级分布数据

**前端实现**:
- 在 `CockpitView.vue` 中新增图表组件:
  - 风险趋势折线图（使用 ECharts）
  - 告警等级分布饼图（使用 ECharts）

#### 1.3 地图模块

**后端实现**:
- 在 `RiskPointService` 中新增方法:
  - `getRiskPointLocations()` - 获取风险点位置信息

**前端实现**:
- 在 `CockpitView.vue` 中新增地图组件:
  - 使用高德地图/百度地图展示风险点分布
  - 支持点击风险点查看详情

#### 1.4 列表模块

**后端实现**:
- 在 `AlarmRecordService` 中新增方法:
  - `getLatestAlarms(int limit)` - 获取最新告警
- 在 `EventRecordService` 中新增方法:
  - `getPendingEvents(int limit)` - 获取待处理事件

**前端实现**:
- 在 `CockpitView.vue` 中新增列表组件:
  - 最新告警列表
  - 待处理事件列表

### 任务 2: 风险监测模块 (预计 4 个工作日)

#### 2.1 实时监测列表

**后端实现**:
- 在 `spring-boot-iot-device` 模块中新增 `MonitoringService`
- 新增实体类 `DeviceMonitoringVO`:
  - 设备编码、设备名称、风险点、测点名称、当前值、单位、状态、最新上报时间、风险等级、是否告警
- 新增 API:
  - `GET /device/monitoring/list` - 实时监测列表
  - `GET /device/monitoring/export` - 导出监测数据

**前端实现**:
- 新增 `MonitoringView.vue` 页面
- 功能:
  - 实时监测列表（支持分页）
  - 搜索条件（设备名称、风险点、风险等级）
  - 数据自动刷新（每 10 秒）
  - 导出功能

#### 2.2 监测详情页面

**后端实现**:
- 在 `MonitoringService` 中新增方法:
  - `getDeviceMonitoringDetail(String deviceCode)` - 设备监测详情
  - `getDevicePropertyTrend(String deviceCode, String propertyCode, String startTime, String endTime)` - 属性趋势数据
  - `getDeviceAlarmHistory(String deviceCode, int limit)` - 设备告警历史

**前端实现**:
- 新增 `DeviceMonitoringDetail.vue` 页面
- 功能:
  - 设备信息展示
  - 当前监测数据表格
  - 最近 24 小时趋势图（ECharts 折线图）
  - 最近告警记录列表

### 任务 3: 设备中心增强 (预计 3 个工作日)

#### 3.1 产品物模型增强

**后端实现**:
- 在 `Product` 实体类中新增字段:
  - `List<ProductProperty> properties` - 属性列表
- 新增实体类 `ProductProperty`:
  - 属性 ID、产品 ID、属性编码、属性名称、数据类型、单位、是否风险监测项、默认阈值、排序、创建时间
- 新增 `ProductPropertyMapper`, `ProductPropertyService`, `ProductPropertyController`
- 新增 API:
  - `GET /device/product/{id}/properties` - 获取产品属性列表
  - `POST /device/product/{id}/property` - 添加产品属性
  - `PUT /device/product/property` - 更新产品属性
  - `DELETE /device/product/property/{id}` - 删除产品属性

**前端实现**:
- 在 `ProductWorkbenchView.vue` 中新增物模型管理标签页
- 功能:
  - 属性列表
  - 添加属性
  - 编辑属性
  - 删除属性
  - 设置风险监测项
  - 设置默认阈值

#### 3.2 设备管理增强

**后端实现**:
- 在 `Device` 实体类中新增字段:
  - `riskPointId` - 风险点 ID
  - `installLocation` - 安装位置
  - `runStatus` - 运行状态
- 在 `DeviceService` 中新增方法:
  - `bindRiskPoint(String deviceCode, Long riskPointId)` - 绑定风险点
  - `unbindRiskPoint(String deviceCode)` - 解绑风险点
  - `updateInstallLocation(String deviceCode, String installLocation)` - 更新安装位置
  - `updateRunStatus(String deviceCode, String runStatus)` - 更新运行状态

**前端实现**:
- 在 `DeviceWorkbenchView.vue` 中新增功能:
  - 绑定风险点
  - 设置安装位置
  - 设置运行状态

### 任务 4: 测试与验证 (预计 3 个工作日)

#### 4.1 告警中心真实环境验收

- 在真实环境联调中验证:
  - 告警列表查询
  - 告警详情查看
  - 告警确认操作
  - 告警抑制操作
  - 通知记录查看

#### 4.2 事件处置真实环境验收

- 在真实环境联调中验证:
  - 事件列表查询
  - 事件详情查看
  - 工单派发操作
  - 现场反馈操作
  - 事件关闭操作

#### 4.3 风险配置真实环境验收

- 在真实环境联调中验证:
  - 风险点 CRUD
  - 阈值规则 CRUD
  - 联动规则 CRUD
  - 应急预案 CRUD

#### 4.4 报表分析真实环境验收

- 在真实环境联调中验证:
  - 风险趋势分析
  - 告警统计分析
  - 事件闭环分析

#### 4.5 配置驱动自动化测试能力

- 建设前端自动化测试中心，支持通过页面配置目标系统、场景模板、步骤、接口断言与变量捕获。
- 浏览器巡检脚本支持 `--plan=...` 执行前端导出的 JSON 计划，沉淀统一报告与测试建议。
- 自动化测试中心第二阶段补齐页面盘点、覆盖分析、菜单驱动页面识别与一键脚手架生成，为后续 AI 自动补全场景提供页面资产基线。
- 自动化测试中心第三阶段补齐插件式步骤注册中心，并内置勾选切换、文件上传、表格行操作、弹窗动作等复杂交互步骤。
- 自动化测试中心第四阶段补齐截图基线、视觉回归、diff 图片索引页、失败截图明细页与批量基线治理命令，支持通过 `baselineDir + assertScreenshot` 在前端完成可视化配置，并通过 `--update-baseline` 刷新基线、通过 `manage-visual-baselines.mjs` 完成离线审计与批量提升。
- 先以当前 IoT 页面为内置样例，后续逐步扩展到任意带浏览器界面的业务系统。
  - 设备健康分析

#### 4.5 系统管理真实环境验收

- 在真实环境联调中验证:
  - 组织机构 CRUD
  - 用户 CRUD
  - 角色权限 CRUD
  - 区域管理 CRUD
  - 字典配置 CRUD
  - 通知渠道 CRUD
  - 审计日志查询

### 任务 5: 性能优化 (预计 1 个工作日)

#### 5.1 数据库查询优化

- 分析慢查询日志
- 为常用查询字段添加索引
- 优化复杂 SQL 查询
- 实施分页查询优化

#### 5.2 缓存策略优化

- 使用 Redis 缓存热点数据:
  - 产品详情
  - 设备详情
  - 字典数据
  - 配置数据
- 实施缓存过期策略
- 实施缓存更新策略

#### 5.3 前端加载优化

- 实施路由懒加载
- 实施组件懒加载
- 优化大列表渲染（虚拟滚动）
- 优化图表渲染性能

### 任务 6: 安全加固 (预计 1 个工作日)

#### 6.1 权限控制完善

- 完善菜单权限控制
- 完善按钮权限控制
- 完善数据权限控制
- 实施接口访问控制

#### 6.2 审计日志完善

- 完善操作日志记录
- 完善登录日志记录
- 完善异常日志记录
- 实施日志分析告警

#### 6.3 数据加密

- 敏感数据加密存储
- 传输数据加密
- 密码加密存储
- 密钥管理

## 交付物

### 代码交付物

1. **后端代码**
   - `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/CockpitService.java`
   - `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CockpitServiceImpl.java`
   - `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/MonitoringService.java`
   - `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/MonitoringServiceImpl.java`
   - `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/ProductProperty.java`
   - `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/ProductPropertyMapper.java`
   - `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductPropertyService.java`
   - `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductPropertyServiceImpl.java`
   - `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductPropertyController.java`

2. **前端代码**
   - `spring-boot-iot-ui/src/views/CockpitView.vue` (增强版)
   - `spring-boot-iot-ui/src/views/MonitoringView.vue`
   - `spring-boot-iot-ui/src/views/DeviceMonitoringDetail.vue`
   - `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue` (增强版)
   - `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue` (增强版)
   - `spring-boot-iot-ui/src/api/cockpit.ts`
   - `spring-boot-iot-ui/src/api/monitoring.ts`
   - `spring-boot-iot-ui/src/api/productProperty.ts`

3. **验收材料**
   - `docs/真实环境测试与验收手册.md`
   - `docs/19-第四阶段交付边界与复验进展.md`
   - `docs/21-业务功能清单与验收标准.md`
   - `scripts/start-backend-acceptance.ps1`
   - `scripts/start-frontend-acceptance.ps1`

4. **SQL 脚本**
   - `sql/upgrade/20260316_phase5_cockpit.sql`
   - `sql/upgrade/20260316_phase5_monitoring.sql`
   - `sql/upgrade/20260316_phase5_product_property.sql`

### 文档交付物

1. `docs/20-phase5-roadmap.md` - Phase 5 研发计划（本文档）
2. `docs/21-phase5-summary.md` - Phase 5 研发总结
3. `docs/22-performance-tuning-guide.md` - 性能调优指南
4. `docs/23-security-guide.md` - 安全加固指南
5. `docs/24-test-scenarios-phase5.md` - Phase 5 测试场景

## 验收标准

### 功能验收

1. **首页驾驶舱增强**
   - KPI 指标卡片正确显示
   - 图表数据正确渲染
   - 地图模块正常展示风险点
   - 列表模块正常展示最新告警和待处理事件

2. **风险监测模块**
   - 实时监测列表正常展示
   - 监测详情页面正常展示
   - 趋势图正常渲染
   - 告警历史正常展示

3. **设备中心增强**
   - 产品物模型管理正常
   - 设备风险点绑定正常
   - 设备安装位置和运行状态正常

### 性能验收

1. 首页加载时间 < 3 秒
2. 列表页面加载时间 < 2 秒
3. 图表渲染时间 < 1 秒
4. API 响应时间 < 500ms
5. 数据库查询时间 < 200ms

### 质量验收

1. 所有真实环境验收项通过
2. 代码覆盖率 > 80%
3. 无严重 Bug
4. 无安全漏洞

## 风险与应对

### 风险 1: 地图模块集成复杂

**应对措施**:
- 提前调研地图 SDK
- 预留充足时间
- 必要时寻求外部支持

### 风险 2: 性能优化效果不明显

**应对措施**:
- 先进行性能测试定位瓶颈
- 针对性优化
- 多次迭代优化

### 风险 3: 测试用例覆盖不全

**应对措施**:
- 详细分析业务场景
- 编写测试用例清单
- 进行用例评审

## 总结

Phase 5 将在 Phase 4 的基础上，进一步完善平台功能，提升用户体验，优化系统性能，加固系统安全。通过 Phase 5 的实施，平台将具备更完整的业务能力和更好的用户体验。
