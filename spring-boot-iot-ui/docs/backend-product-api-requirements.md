# 后端产品详情 API 修改需求与当前实现边界

## 接口信息

**接口名称**：根据ID查询产品详情  
**请求路径**：`GET /api/device/product/{id}`  
**请求方式**：GET  
**返回类型**：Product

## 当前返回字段

Product 对象包含以下字段：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | number | 是 | 产品编号 |
| productKey | string | 是 | 产品Key |
| productName | string | 是 | 产品名称 |
| protocolCode | string | 是 | 协议编码 |
| nodeType | number | 是 | 节点类型（1-直连设备，2-网关设备） |
| dataFormat | string | 否 | 数据格式 |
| manufacturer | string | 否 | 厂商 |
| description | string | 否 | 产品说明 |
| status | number | 否 | 产品状态（0-停用，1-启用） |
| deviceCount | number | 否 | 关联设备总数 |
| onlineDeviceCount | number | 否 | 在线设备数 |
| lastReportTime | string | 否 | 最近上报时间 |
| createTime | string | 否 | 创建时间 |
| updateTime | string | 否 | 更新时间 |

## 需要新增的字段

| 字段名 | 类型 | 说明 | 计算方式 |
|--------|------|------|----------|
| todayActiveCount | number | 今日活跃设备数 | 今天上报过数据的设备数量 |
| sevenDaysActiveCount | number | 7日活跃设备数 | 最近7天上报过数据的设备数量 |
| thirtyDaysActiveCount | number | 30日活跃设备数 | 最近30天上报过数据的设备数量 |
| avgOnlineDuration | number | 平均在线时长（分钟） | 设备平均每次在线时长（单位：分钟） |
| maxOnlineDuration | number | 最长在线时长（分钟） | 设备单次最长在线时长（单位：分钟） |

## 当前真实环境说明

- 已稳定交付：`todayActiveCount`、`sevenDaysActiveCount`、`thirtyDaysActiveCount`
- 已完成：`avgOnlineDuration`、`maxOnlineDuration`
- 当前真实环境返回口径：
  - `todayActiveCount / sevenDaysActiveCount / thirtyDaysActiveCount` 基于 `iot_device.last_report_time`
  - `avgOnlineDuration / maxOnlineDuration` 基于 `iot_device_online_session`
- 当前实现说明：
  - 会话开始：设备离线后首次重新上报
  - 会话结束：`last_report_time + iot.device.online-timeout-seconds` 超时推断，默认 `120` 秒
  - 历史老会话不回填，因此没有会话明细的产品仍可能返回 `null`

## 数据计算逻辑

### 1. 今日活跃设备数（目标方案）
```sql
SELECT COUNT(DISTINCT device_id) 
FROM device_report_log 
WHERE product_key = ? 
  AND DATE(report_time) = CURDATE();
```

### 2. 7日活跃设备数（目标方案）
```sql
SELECT COUNT(DISTINCT device_id) 
FROM device_report_log 
WHERE product_key = ? 
  AND report_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY);
```

### 3. 30日活跃设备数（目标方案）
```sql
SELECT COUNT(DISTINCT device_id) 
FROM device_report_log 
WHERE product_key = ? 
  AND report_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY);
```

### 4. 平均在线时长
```sql
SELECT AVG(TIMESTAMPDIFF(MINUTE, last_online_time, last_offline_time)) as avg_duration
FROM iot_device_online_session
WHERE product_id = ?
  AND online_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY);
```

### 5. 最长在线时长
```sql
SELECT MAX(TIMESTAMPDIFF(MINUTE, last_online_time, last_offline_time)) as max_duration
FROM iot_device_online_session
WHERE product_id = ?
  AND online_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY);
```

## 后端实现要求

1. 在 ProductDO/ProductEntity 实体类中添加以下字段：
   - todayActiveCount
   - sevenDaysActiveCount
   - thirtyDaysActiveCount
   - avgOnlineDuration
   - maxOnlineDuration

2. 在 ProductDTO/返回对象中添加以上字段

3. 在 getProductById 方法中，查询并填充活跃度统计数据

4. 如果活跃度数据不存在，返回 0 或 null；当前实现对无会话明细的在线时长返回 `null`

5. 当前实现建议索引：
```sql
CREATE INDEX idx_device_deleted_product_stats
ON iot_device(deleted, product_id, last_report_time, online_status);

CREATE INDEX idx_online_session_product_time
ON iot_device_online_session(deleted, product_id, online_time, offline_time);
```

## 返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 123,
    "productKey": "accept-http-product-01",
    "productName": "HTTP产品",
    "protocolCode": "http-json",
    "nodeType": 1,
    "dataFormat": "JSON",
    "manufacturer": "test",
    "description": null,
    "status": 1,
    "deviceCount": 10,
    "onlineDeviceCount": 5,
    "lastReportTime": "2026-03-21 17:00:00",
    "createTime": "2026-03-21 10:00:00",
    "updateTime": "2026-03-21 16:00:00",
    "todayActiveCount": 8,
    "sevenDaysActiveCount": 12,
    "thirtyDaysActiveCount": 15,
    "avgOnlineDuration": 120,
    "maxOnlineDuration": 720
  }
}
```
