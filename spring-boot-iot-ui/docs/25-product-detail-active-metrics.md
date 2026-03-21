# 产品详情页活跃度统计说明

## 一、当前交付范围

产品定义中心详情页已接入“设备活跃度”模块，当前交付范围仅包含 3 个活跃设备数：

- `todayActiveCount`
- `sevenDaysActiveCount`
- `thirtyDaysActiveCount`

本轮不交付：

- `avgOnlineDuration`
- `maxOnlineDuration`

原因：

- 当前真实环境只有 `iot_device.last_online_time / last_offline_time` 最新状态字段，没有独立在线会话明细表。
- `last_online_time` 会在设备每次上报时刷新，不能稳定代表“单次在线会话开始时间”。
- 如果继续在当前模型上计算在线时长，会把“最近一次上报”误当成“会话起点”，结果不可靠。

## 二、页面表现

### 2.1 模块位置

在产品详情页中，位于“产品档案”和“维护与治理”两个区域之间。

### 2.2 模块标题

- 主标题：`设备活跃度`
- 描述：`设备活跃趋势分析`

### 2.3 当前展示字段

| 指标 | 说明 | 数据来源 |
|---|---|---|
| 今日活跃 | 今天上报过数据的设备数量 | `iot_device.last_report_time >= 今日 00:00:00` |
| 7日活跃 | 最近 7 天上报过数据的设备数量 | `iot_device.last_report_time >= 今日 00:00:00 - 7 天` |
| 30日活跃 | 最近 30 天上报过数据的设备数量 | `iot_device.last_report_time >= 今日 00:00:00 - 30 天` |

### 2.4 显示条件

- 当前前端在存在任一活跃度字段时显示该模块。
- 因为后端现已稳定返回 3 个活跃设备数字段，所以模块会正常展示。
- 前端仍保留在线时长字段的可选兼容处理，但当前后端不会返回这两个字段，因此不会出现在线时长卡片。

## 三、接口口径

### 3.1 详情接口

- 请求：`GET /api/device/product/{id}`
- 返回增强字段：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 123,
    "productKey": "accept-http-product-01",
    "productName": "HTTP产品",
    "todayActiveCount": 8,
    "sevenDaysActiveCount": 12,
    "thirtyDaysActiveCount": 15
  }
}
```

### 3.2 当前后端实现

- 聚合维度：`product_id`
- 活跃时间字段：`iot_device.last_report_time`
- 聚合范围：单产品详情查询
- 查询入口：`spring-boot-iot-device` 模块内的 `ProductServiceImpl + DeviceMapper`
- 性能依赖：复用现有索引 `idx_device_deleted_product_stats (deleted, product_id, last_report_time, online_status)`

## 四、前端状态

当前前端已具备以下实现，不需要新增结构性改动：

- `Product` 类型已预留活跃度字段
- 详情页模板已插入“设备活跃度”区块
- `hasActiveMetrics` 已负责显示开关
- `detailActiveMetrics` 已负责把后端字段转换成展示卡片
- 详情区块样式和响应式布局已就绪

本轮仅补齐后端真实返回值。

## 五、后续扩展建议

如果后续必须交付“平均在线时长 / 最长在线时长”，建议先补以下能力，再开放接口字段：

1. 新增独立在线会话表或在线日志表。
2. 在设备上线、离线链路中持久化会话开始/结束时间。
3. 再按历史会话明细计算平均值和最大值。

在此之前，不建议继续基于 `iot_device` 最新状态表近似计算在线时长。
