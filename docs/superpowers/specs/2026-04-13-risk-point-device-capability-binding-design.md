# 风险绑定工作台设备能力分流设计

## 背景

当前 `/risk-point -> 风险绑定工作台` 的正式绑定只支持“设备 + 测点”模型，默认所有正式绑定都落到 `risk_point_device`。这条链路适合监测型设备，但不适合预警型与视频类设备：

- **监测型**：具备正式测点能力，继续沿用 `risk_point_device`。
- **预警型**：没有测点能力，只需要设备级正式绑定，后续参与被动处置和设备级关联。
- **视频类**：当前也不做测点绑定，只做设备级正式绑定，同时为后续 AI 事件分析预留扩展能力。

## 目标

1. 监测型设备继续走“设备 + 测点”正式绑定，不破坏现有风险指标目录和阈值链路。
2. 预警型、视频类设备允许进入风险点正式绑定，但粒度改为“设备级正式绑定”。
3. 视频类设备在正式绑定结果里显式保留 `AI_EVENT_RESERVED` 扩展位，为后续 AI 事件分析接入留稳定真相源。
4. 风险绑定工作台、详情抽屉和绑定摘要统一能看懂两种正式绑定模式。

## 非目标

1. 本轮不把预警型、视频类绑定纳入 `risk_metric_catalog`、阈值策略、联动/预案覆盖率或自动风险判级。
2. 本轮不交付视频 AI 事件分析，只交付设备级正式绑定和扩展占位。
3. 本轮不重构 `risk_point_device` 为双粒度总表，避免影响现有监测型主链路。

## 方案对比

### 方案 A：继续用 `risk_point_device` 写伪测点

- 优点：改动最少。
- 缺点：会把预警型/视频类污染成假测点，误导风险指标目录、阈值、覆盖率和自动闭环。

**结论：拒绝。**

### 方案 B：把 `risk_point_device` 扩成双粒度总表

- 优点：表面上只有一张真相表。
- 缺点：需要修改唯一键、下游查询、风控读侧、覆盖率统计和审批载荷，回归面过大。

**结论：当前轮次不选。**

### 方案 C：新增设备级正式绑定真相表，与测点绑定并行

- 优点：监测型主链路保持不变；预警型/视频类语义清晰；视频 AI 后续可直接挂在设备级正式绑定上扩展。
- 缺点：需要新增一套设备级绑定表和聚合读逻辑。

**结论：采用。**

## 详细设计

### 1. 设备能力判定

前后端都引入统一设备能力类型：

- `MONITORING`
- `WARNING`
- `VIDEO`
- `UNKNOWN`

优先按 `productKey + productName` 关键词推断：

- `VIDEO`：`video / camera / ipc / 视频 / 摄像`
- `WARNING`：`warning / warn / 预警 / 声光 / 爆闪 / 广播 / 情报板 / 报警`
- `MONITORING`：`monitor / monitoring / 监测 / gnss / 位移 / 倾角 / 裂缝 / 雨量 / 水位 / 激光`
- 其余为 `UNKNOWN`

`UNKNOWN` 不直接视为失败：

- 如果当前设备存在 formal-metrics，则按监测型处理。
- 如果当前设备不存在 formal-metrics，则允许按设备级正式绑定处理。

### 2. 新增设备级正式绑定真相表

新增表：`risk_point_device_capability_binding`

只服务预警型、视频类和无测点能力设备的正式绑定，字段最小化：

- `risk_point_id`
- `device_id`
- `device_code`
- `device_name`
- `device_capability_type`
- `extension_status`
- 审计字段与逻辑删除字段

约束：

- 唯一键：`(risk_point_id, device_id)`
- 语义：一台设备在同一风险点下只能存在一条设备级正式绑定
- 与 `risk_point_device` 互斥：同一设备不能同时在同一风险点下既有设备级正式绑定，又有测点级正式绑定
- 与跨风险点占用互斥：同一设备不能被多个风险点占用

视频类设备写入时固定设置：

- `device_capability_type = VIDEO`
- `extension_status = AI_EVENT_RESERVED`

### 3. 后端写侧

#### 3.1 保留原正式测点绑定

`POST /api/risk-point/bind-device` 继续只服务监测型正式测点绑定。

#### 3.2 新增设备级正式绑定接口

新增：

- `POST /api/risk-point/bind-device-capability`

请求体：

- `riskPointId`
- `deviceId`
- `deviceCapabilityType`（可选，后端仍以设备实际能力为准）

校验规则：

1. 风险点、设备、组织归属、数据权限与现有绑定校验保持一致。
2. 若设备实际能力为 `MONITORING`，拒绝设备级正式绑定，要求走正式测点绑定。
3. 若设备已在当前风险点存在设备级绑定，返回“设备已绑定到该风险点”。
4. 若设备在当前风险点已有测点绑定，返回“当前设备已存在测点绑定，请先整机解绑后再切换为设备级正式绑定”。
5. 若设备在其他风险点存在任一正式绑定，继续拒绝跨风险点重复绑定。

#### 3.3 审批与治理工作项

设备级正式绑定继续沿用同一治理工作项编码和审批动作语义：

- `workItemCode = PENDING_RISK_BINDING`
- `actionCode = RISK_POINT_BIND_DEVICE`

区别只放进 bind payload：

- `bindingMode = DEVICE_ONLY`
- `deviceCapabilityType`
- `extensionStatus`

这样无需额外新增审批策略种类，也能保持既有审批链路。

#### 3.4 整机解绑

`POST /api/risk-point/unbind-device` 扩成“删除该设备在当前风险点下全部正式绑定真相”：

- 删除 `risk_point_device`
- 删除 `risk_point_device_capability_binding`

### 4. 后端读侧

#### 4.1 绑定摘要

`GET /api/risk-point/binding-summaries`

- `boundDeviceCount`：统计测点级绑定设备 + 设备级绑定设备的去重总数
- `boundMetricCount`：只统计 `risk_point_device` 的正式测点数
- `pendingBindingCount`：保持原 pending 逻辑

#### 4.2 绑定分组

`GET /api/risk-point/binding-groups/{riskPointId}`

统一返回两类分组：

- `bindingMode = METRIC`
- `bindingMode = DEVICE_ONLY`

补充字段：

- `deviceCapabilityType`
- `aiEventExpandable`
- `extensionStatus`

其中：

- `METRIC` 分组继续返回 `metrics[]`
- `DEVICE_ONLY` 分组固定 `metricCount = 0`、`metrics = []`

#### 4.3 可绑定设备候选

`GET /api/risk-point/bindable-devices/{riskPointId}`

候选设备补充：

- `deviceCapabilityType`
- `supportsMetricBinding`
- `aiEventExpandable`

同时收口占用规则：

- 其他风险点的测点绑定和设备级绑定都算占用
- 当前风险点下，只有已存在测点绑定的设备允许继续出现在候选中，以支持新增第二个测点
- 当前风险点下已存在设备级正式绑定的设备不再回显到候选，避免重复新增

### 5. 前端工作台

#### 5.1 新增正式绑定区

设备选中后分两类渲染：

- `MONITORING` / 有 formal-metrics：显示测点下拉，走原正式测点绑定
- `WARNING` / `VIDEO` / 无 formal-metrics：隐藏测点下拉，显示“设备级正式绑定”说明

按钮文案动态变化：

- 监测型：`新增正式绑定`
- 预警型/视频类：`新增设备级正式绑定`

设备说明文案：

- 预警型：`该设备无正式测点能力，将按设备级正式绑定收口，仅参与被动处置关联。`
- 视频类：`该设备当前按设备级正式绑定收口，并预留 AI 事件分析扩展位。`

#### 5.2 当前正式绑定区

监测型卡片继续显示：

- 测点列表
- 单测点删除
- 更换测点
- 整机解绑

设备级正式绑定卡片显示：

- `设备级正式绑定`
- `预警型` / `视频类`
- 视频类的 `AI 事件扩展预留`
- 只保留 `整机解绑`

#### 5.3 风险点详情抽屉

详情抽屉的“正式绑定设备”分组也统一展示两类绑定：

- 测点级正式绑定
- 设备级正式绑定

摘要里的“正式测点”仍只统计监测型测点数，不把预警型/视频类设备级绑定伪装成测点。

### 6. 错误处理

后端新增明确错误：

- `监测型设备必须选择正式测点，不能按设备级绑定`
- `当前设备已存在测点绑定，请先整机解绑后再切换为设备级正式绑定`

前端对设备级模式不再出现“请选择测点”提示。

### 7. 验收与测试

后端：

1. 监测型设备继续只能按 formal-metrics 绑定。
2. 预警型设备可按设备级正式绑定提交。
3. 视频类设备可按设备级正式绑定提交，并带出 `AI_EVENT_RESERVED`。
4. 绑定摘要与绑定分组能够同时聚合两类正式绑定。
5. 整机解绑会同时删除两类正式绑定。

前端：

1. 监测型设备显示测点选择。
2. 预警型设备隐藏测点选择并显示设备级正式绑定说明。
3. 视频类设备隐藏测点选择并显示 AI 扩展占位说明。
4. 当前正式绑定列表能正确渲染设备级正式绑定卡片。

