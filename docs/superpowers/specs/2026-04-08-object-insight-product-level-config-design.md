# 对象洞察台产品级配置化设计

> 日期：2026-04-08
> 主题：把对象洞察台的“系统自定义参数”和分析描述，从前端轻配置演进为“产品级正式配置 + 设备级兼容覆盖”的可维护方案

## 1. 背景

`/insight` 已在本轮前序改造中收口为“单设备对象洞察台”，并完成以下基线：

- 设备资产中心“更多 -> 洞察”可携带 `deviceCode` 进入
- 直开 `/insight` 时搜索框为空，不自动加载示例设备
- 趋势主数据统一改为 `POST /api/telemetry/history/batch`
- 趋势固定分为 `监测数据 / 状态数据`
- 样板设备 `SK00EB0D1308313` 已直配 `泥水位高程 / 传感器在线状态 / 剩余电量`
- 运行时出现 `humidity / signal_4g` 等状态字段时，页面可自动纳入“系统自定义参数”

当前剩余缺口是：

1. 自定义指标与分析描述还主要依赖前端注册表和设备级轻配置，缺少正式维护入口。
2. 同一产品下多台设备需要共用同一套洞察配置，但当前没有产品级承载层。
3. 若后续要给业务角色长期维护“湿度、4G 信号、额外状态项”的中文名称、趋势归属和分析文案，单靠设备级元数据会造成重复维护。

## 2. 目标

- 为对象洞察台建立“产品级正式配置”承载层。
- 让同一产品下设备可共用同一套自定义指标、展示名称、趋势归属和分析文案。
- 保持当前对象洞察页的兼容能力，不因为未配置产品而退化。
- 为未来的“设备级个别覆盖”预留读取顺序，但本轮不建设设备级配置入口。

## 3. 非目标

- 本轮不建设独立的对象洞察配置后台菜单、独立路由或独立子系统。
- 本轮不新增 `iot_product_insight_config` 独立表。
- 本轮不建设设备级对象洞察配置编辑入口。
- 本轮不引入表达式引擎、规则脚本或条件化渲染 DSL。
- 本轮不做批量模板分发、多产品复制或审批流。

## 4. 方案比较

### 4.1 方案 A：继续只用设备级 `metadataJson`

把当前轻配置继续放在 `iot_device.metadata_json.objectInsight`。

优点：

- 改动最小。
- 与当前前端读取逻辑兼容。

缺点：

- 同一产品下要重复维护。
- 无法形成“产品能力”的正式主档。
- 后续规模化维护成本高。

### 4.2 方案 B：产品主档新增 `metadata_json`

在 `iot_product` 增加 `metadata_json`，统一承载 `objectInsight` 配置；对象洞察读取时优先设备级覆盖，再回退产品级。

优点：

- 符合“产品级优先”的业务语义。
- 同产品设备共用配置，维护成本低。
- 兼容未来设备级覆盖，不推翻当前轻配置链路。
- 不需要扩成独立配置系统。

缺点：

- 需要补数据库字段、DTO/VO、产品编辑入口和对象洞察读取链路。

### 4.3 方案 C：独立配置表 + 专用 API

新增 `iot_product_insight_config` 和专用 CRUD 接口。

优点：

- 长期最清晰。
- 配置职责最独立。

缺点：

- 本轮范围明显扩大。
- 需要额外权限、页面、接口和文档治理。

## 5. 选型

本轮采用方案 B。

结论如下：

- 正式配置层放在 `iot_product.metadata_json.objectInsight`
- 设备级 `iot_device.metadata_json.objectInsight` 保留兼容读取，但本轮不提供编辑入口
- 对象洞察页按“设备级覆盖 > 产品级正式配置 > 前端注册表 > 运行时自动识别”读取

## 6. 数据结构设计

### 6.1 数据库存储

在 `iot_product` 新增字段：

- `metadata_json JSON NULL COMMENT '产品扩展元数据'`

### 6.2 配置结构

`metadata_json` 中对象洞察配置固定挂在：

```json
{
  "objectInsight": {
    "customMetrics": [
      {
        "identifier": "S1_ZT_1.humidity",
        "displayName": "相对湿度",
        "group": "status",
        "includeInTrend": true,
        "includeInExtension": true,
        "analysisTitle": "现场环境补充",
        "analysisTag": "系统自定义参数",
        "analysisTemplate": "{{label}}当前为{{value}}，可辅助判断现场环境湿润程度。",
        "enabled": true,
        "sortNo": 10
      }
    ]
  }
}
```

### 6.3 字段约束

`customMetrics[]` 每项字段语义如下：

- `identifier`：系统内部指标标识，必填
- `displayName`：客户可见中文名称，必填
- `group`：`measure | status`，必填
- `includeInTrend`：是否进入趋势查询，默认 `true`
- `includeInExtension`：是否进入“系统自定义参数”区，默认 `true`
- `analysisTitle`：分析块标题，可空
- `analysisTag`：分析块标签，可空
- `analysisTemplate`：分析描述模板，可空
- `enabled`：是否启用，默认 `true`
- `sortNo`：排序，可空

本轮约束：

- 单产品 `customMetrics` 上限 `20`
- 单条 `analysisTemplate` 长度上限 `300`
- 同一产品下 `identifier` 不允许重复

## 7. 后端设计

### 7.1 表结构与实体

需同步补齐：

- `sql/init.sql`
- `Product` 实体
- `ProductAddDTO`
- `ProductDetailVO`
- `ProductPageVO` 是否透出 `metadataJson` 由前端是否需要列表态消费决定；本轮不强制要求

### 7.2 API 设计

本轮不新增对象洞察专用 API，继续复用：

- `POST /api/device/product/add`
- `PUT /api/device/product/{id}`
- `GET /api/device/product/{id}`

产品主档的 `metadataJson` 作为标准字段随新增/编辑/详情一起维护。

### 7.3 服务校验

`ProductServiceImpl` 在新增和更新时增加以下校验：

- `metadataJson` 必须是合法 JSON
- 若存在 `objectInsight.customMetrics`，其结构必须合法
- `group` 只能为 `measure` 或 `status`
- `identifier`、`displayName` 不能为空
- `identifier` 不可重复
- `customMetrics` 数量不可超过 `20`
- `analysisTemplate` 长度不可超过 `300`

若校验失败，统一抛 `BizException`，不允许脏配置入库。

## 8. 前端设计

### 8.1 产品定义中心入口

配置入口放在现有 `ProductEditWorkspace`，新增一个独立正文分区：

- `对象洞察配置`

页面结构保持：

1. 基础档案
2. 接入基线
3. 补充说明
4. 对象洞察配置

### 8.2 配置交互形态

本轮采用“最小结构化表单行编辑”，不直接暴露原始 JSON 文本框。

每行配置一条自定义指标，字段包括：

- 指标标识符
- 中文名称
- 分组
- 是否进入趋势
- 是否进入系统自定义参数
- 分析标题
- 分析标签
- 分析描述模板
- 启用状态
- 排序

前端在提交前负责把结构化表单序列化到 `metadataJson.objectInsight.customMetrics`。

### 8.3 对象洞察页读取链路

对象洞察页加载单台设备时，读取顺序固定为：

1. `GET /api/device/code/{deviceCode}` 获取设备主档与设备级 `metadataJson`
2. 若设备存在 `productId`，调用 `GET /api/device/product/{id}` 获取产品级 `metadataJson`
3. 前端合并顺序：
   - `device.metadataJson.objectInsight`
   - `product.metadataJson.objectInsight`
   - 前端内置注册表
   - 运行时自动识别

其中：

- 设备级只做兼容覆盖
- 产品级是本轮正式维护入口

## 9. 降级与兼容

### 9.1 降级原则

对象洞察页不能因为配置异常而整页失败。

处理规则：

- 产品详情接口失败：继续按“设备级覆盖 + 注册表 + 自动识别”渲染
- `metadataJson` 为空：直接走现有逻辑
- 历史脏配置解析失败：忽略该配置并降级
- 配置指标当前设备无值：不展示值、不生成分析文案
- 趋势查询无数据：保持现有空序列或补零语义

### 9.2 兼容策略

- 新增字段默认允许为 `NULL`
- 旧产品无需补配置即可继续工作
- 旧设备级 `metadataJson.objectInsight` 继续有效
- 本轮不强制清理既有设备级轻配置

## 10. 测试与验收

### 10.1 后端测试

- `ProductServiceImpl` 新增/更新合法 `metadataJson`
- 非法 JSON 被拒绝
- 非法 `customMetrics` 结构被拒绝
- `GET /api/device/product/{id}` 可返回 `metadataJson`

### 10.2 前端测试

- `ProductEditWorkspace` 的自定义指标增删、校验、序列化
- `ProductWorkbenchView` 新增/编辑产品时正确提交 `metadataJson`
- `DeviceInsightView` 能读取产品级配置并合并展示
- 当设备级和产品级同时存在时，设备级覆盖产品级

### 10.3 业务验收

真实环境下至少完成以下验收：

1. 在产品定义中心编辑一个产品，新增 `humidity / signal_4g` 对象洞察配置
2. 进入同产品下设备的 `/insight`
3. 验证“系统自定义参数”显示配置后的中文名称
4. 验证趋势查询包含配置指标
5. 验证综合分析区使用配置后的分析模板
6. 验证未配置产品的对象洞察表现不退化

## 11. 风险与边界

### 11.1 当前风险

- 产品编辑页会新增一块配置表单，需要控制复杂度，避免重新长成“第二套治理工作台”
- 若共享环境历史库未补 `metadata_json`，对象洞察正式配置化会受环境阻塞
- 产品级配置和设备级覆盖并存时，必须把读取优先级写死并覆盖测试

### 11.2 本轮边界

本轮只交付：

- 产品主档补 `metadata_json`
- 产品编辑页补最小对象洞察配置区
- 对象洞察读取产品级配置
- 保留设备级兼容覆盖

本轮不交付：

- 独立对象洞察后台
- 独立配置表
- 设备级编辑入口
- 批量模板
- 审批与版本化治理

## 12. 结论

本轮对象洞察配置化正式收口为：

- 以 `iot_product.metadata_json.objectInsight` 为正式配置主档
- 以设备级 `metadataJson.objectInsight` 为兼容覆盖
- 以对象洞察页前端合并层实现“正式配置 + 兼容覆盖 + 注册表兜底 + 自动识别兜底”

这样既能把当前轻配置能力升级成“产品级正式配置”，又不会把本轮范围扩成单独的配置子系统。
