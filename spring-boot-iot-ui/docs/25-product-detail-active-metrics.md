# 产品详情页活跃度统计说明

## 一、当前交付范围

产品定义中心详情页已接入“设备活跃度”模块，当前真实环境已接入 5 个统计字段：

- `todayActiveCount`
- `sevenDaysActiveCount`
- `thirtyDaysActiveCount`

- `avgOnlineDuration`
- `maxOnlineDuration`

在线时长口径：

- 新增 `iot_device_online_session` 会话明细表，记录 `online_time / last_seen_time / offline_time / duration_minutes`。
- 设备首次进入在线状态时开启会话，超时无上报时按 `last_report_time + iot.device.online-timeout-seconds` 推断离线并闭会话。
- 历史老会话不做回填，因此没有会话记录的产品仍会返回 `null` 在线时长。

## 二、页面表现

### 2.1 模块位置

在产品详情页中，位于“产品档案”和“维护与治理”两个区域之间。

### 2.2 模块标题

- 主标题：`设备活跃度`
- 描述：`设备活跃趋势和在线时长分析`

### 2.3 当前展示字段

| 指标 | 说明 | 数据来源 |
|---|---|---|
| 今日活跃 | 今天上报过数据的设备数量 | `iot_device.last_report_time >= 今日 00:00:00` |
| 7日活跃 | 最近 7 天上报过数据的设备数量 | `iot_device.last_report_time >= 今日 00:00:00 - 7 天` |
| 30日活跃 | 最近 30 天上报过数据的设备数量 | `iot_device.last_report_time >= 今日 00:00:00 - 30 天` |
| 平均在线时长 | 近 30 天会话的平均在线分钟数 | `iot_device_online_session` |
| 最长在线时长 | 近 30 天会话的最大在线分钟数 | `iot_device_online_session` |

### 2.4 显示条件

- 当前前端在存在任一活跃度字段时显示该模块。
- 后端现已返回 3 个活跃设备数字段；当产品已有会话明细时，在线时长卡片也会一并展示。
- 若产品暂无会话明细，`avgOnlineDuration / maxOnlineDuration` 会返回 `null`，在线时长卡片自动隐藏。

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
    "thirtyDaysActiveCount": 15,
    "avgOnlineDuration": 120,
    "maxOnlineDuration": 720
  }
}
```

### 3.2 当前后端实现

- 聚合维度：`product_id`
- 活跃时间字段：`iot_device.last_report_time`
- 在线时长明细表：`iot_device_online_session`
- 聚合范围：单产品详情查询
- 查询入口：`spring-boot-iot-device` 模块内的 `ProductServiceImpl + DeviceMapper + DeviceOnlineSessionMapper`
- 性能依赖：复用 `idx_device_deleted_product_stats` 与新增 `idx_online_session_product_time`

## 四、前端状态

当前前端已具备以下实现，不需要新增结构性改动：

- `Product` 类型已预留活跃度字段
- 详情页模板已插入“设备活跃度”区块
- `hasActiveMetrics` 已负责显示开关
- `detailActiveMetrics` 已负责把后端字段转换成展示卡片
- 详情区块样式和响应式布局已就绪

本轮后端已补齐真实返回值；后续若要提高精度，可继续增加显式离线事件闭环，减少“超时推断离线”的误差。
