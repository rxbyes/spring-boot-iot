# 产品详情页活跃度统计需求

## 一、需求概述

在产品定义中心的详情页中添加设备活跃度统计模块，展示设备的活跃趋势和在线时长分析，帮助用户了解产品的设备使用情况。

## 二、功能描述

### 2.1 模块位置
在产品详情页中，位于"产品档案"和"维护与治理"两个区域之间。

### 2.2 模块标题
- 主标题：**设备活跃度**
- 描述：**设备活跃趋势和在线时长分析**

### 2.3 展示字段

| 指标 | 说明 | 数据类型 | 数据来源 |
|------|------|----------|----------|
| 今日活跃 | 今天上报过数据的设备数量 | number | 设备表，lastReportTime >= 今日00:00:00 |
| 7日活跃 | 最近7天上报过数据的设备数量 | number | 设备表，lastReportTime >= 今日-7天 |
| 30日活跃 | 最近30天上报过数据的设备数量 | number | 设备表，lastReportTime >= 今日-30天 |
| 平均在线时长 | 设备平均每次在线时长（小时） | number | 设备表，计算 onlineDuration 的平均值 |
| 最长在线时长 | 设备单次最长在线时长（小时） | number | 设备表，计算 onlineDuration 的最大值 |

### 2.4 显示条件
- 当 Product 数据包含至少一个活跃度统计字段（todayActiveCount, sevenDaysActiveCount, thirtyDaysActiveCount, avgOnlineDuration, maxOnlineDuration）时，显示该模块

## 三、技术实现

### 3.1 前端实现

#### API 接口修改
当前详情页使用 `getProductById(id)` 接口获取产品详情，需要在返回的数据中包含活跃度统计字段。

**请求：** `GET /api/products/{id}`

**响应数据结构：**
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
    // ============ 新增活跃度统计字段 ============
    "todayActiveCount": 8,
    "sevenDaysActiveCount": 12,
    "thirtyDaysActiveCount": 15,
    "avgOnlineDuration": 120,
    "maxOnlineDuration": 720
  }
}
```

#### 前端计算属性（已实现）
- `hasActiveMetrics` - 判断是否有活跃度数据
- `detailActiveMetrics` - 格式化活跃度数据用于展示

### 3.2 后端实现

#### SQL 查询示例

```sql
-- 获取今日活跃设备数
SELECT COUNT(DISTINCT device_id) 
FROM device_report_log 
WHERE product_key = ? 
  AND report_time >= CURDATE();

-- 获取7日活跃设备数
SELECT COUNT(DISTINCT device_id) 
FROM device_report_log 
WHERE product_key = ? 
  AND report_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY);

-- 获取30日活跃设备数
SELECT COUNT(DISTINCT device_id) 
FROM device_report_log 
WHERE product_key = ? 
  AND report_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY);

-- 获取平均在线时长（分钟）
SELECT AVG(TIMESTAMPDIFF(MINUTE, last_online_time, last_offline_time)) as avg_online_duration
FROM device_online_log 
WHERE product_key = ? 
  AND last_online_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY);

-- 获取最长在线时长（分钟）
SELECT MAX(TIMESTAMPDIFF(MINUTE, last_online_time, last_offline_time)) as max_online_duration
FROM device_online_log 
WHERE product_key = ? 
  AND last_online_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY);
```

#### Java Service 示例

```java
public class ProductActiveMetrics {
    private Long todayActiveCount;
    private Long sevenDaysActiveCount;
    private Long thirtyDaysActiveCount;
    private Integer avgOnlineDuration; // 分钟
    private Integer maxOnlineDuration; // 分钟
}

// 在 ProductServiceImpl 中
public ProductDetailDto getProductById(Long id) {
    Product product = productMapper.selectById(id);
    if (product == null) {
        throw new BusinessErrorException("产品不存在");
    }
    
    ProductDetailDto dto = BeanUtil.copyProperties(product, ProductDetailDto.class);
    
    // 查询活跃度统计
    ProductActiveMetrics metrics = activeMetricsMapper.getProductActiveMetrics(product.getProductKey());
    if (metrics != null) {
        dto.setTodayActiveCount(metrics.getTodayActiveCount());
        dto.setSevenDaysActiveCount(metrics.getSevenDaysActiveCount());
        dto.setThirtyDaysActiveCount(metrics.getThirtyDaysActiveCount());
        dto.setAvgOnlineDuration(metrics.getAvgOnlineDuration());
        dto.setMaxOnlineDuration(metrics.getMaxOnlineDuration());
    }
    
    return dto;
}
```

## 四、UI 设计

### 4.1 布局结构
```html
<section class="product-detail-zone product-detail-zone--overview">
  <header class="product-detail-zone__header">
    <span class="product-detail-zone__kicker">设备活跃度</span>
    <p class="product-detail-zone__intro">设备活跃趋势和在线时长分析。</p>
  </header>
  <div class="product-detail-active-grid">
    <article class="product-detail-active-metric">
      <span class="product-detail-active-metric__label">今日活跃</span>
      <strong class="product-detail-active-metric__value">12</strong>
      <p class="product-detail-active-metric__hint">今天上报过数据的设备数量</p>
    </article>
    <article class="product-detail-active-metric">
      <span class="product-detail-active-metric__label">7日活跃</span>
      <strong class="product-detail-active-metric__value">15</strong>
      <p class="product-detail-active-metric__hint">最近7天上报过数据的设备数量</p>
    </article>
    <article class="product-detail-active-metric">
      <span class="product-detail-active-metric__label">30日活跃</span>
      <strong class="product-detail-active-metric__value">18</strong>
      <p class="product-detail-active-metric__hint">最近30天上报过数据的设备数量</p>
    </article>
    <article class="product-detail-active-metric">
      <span class="product-detail-active-metric__label">平均在线时长</span>
      <strong class="product-detail-active-metric__value">2小时</strong>
      <p class="product-detail-active-metric__hint">设备平均每次在线时长</p>
    </article>
    <article class="product-detail-active-metric">
      <span class="product-detail-active-metric__label">最长在线时长</span>
      <strong class="product-detail-active-metric__value">12小时</strong>
      <p class="product-detail-active-metric__hint">设备单次最长在线时长</p>
    </article>
  </div>
</section>
```

### 4.2 响应式布局
- 桌面端（>= 721px）：2列网格布局
- 移动端（<= 720px）：1列布局

## 五、开发任务

### 5.1 后端开发任务
- [ ] 在数据库中创建活跃度统计相关的索引（lastReportTime, product_key）
- [ ] 实现活跃度统计查询 SQL
- [ ] 在 ProductDetailDto 中添加活跃度统计字段
- [ ] 修改 getProductById 方法，查询并返回活跃度统计
- [ ] 添加活跃度统计缓存（可选，减少数据库查询）

### 5.2 前端开发任务（已完成）
- [x] 在 Product 类型定义中添加活跃度统计字段
- [x] 添加活跃度统计区域的模板
- [x] 添加 hasActiveMetrics 和 detailActiveMetrics 计算属性
- [x] 添加活跃度统计相关的 CSS 样式

## 六、数据字典

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| todayActiveCount | number | 否 | 今日活跃设备数 |
| sevenDaysActiveCount | number | 否 | 7日活跃设备数 |
| thirtyDaysActiveCount | number | 否 | 30日活跃设备数 |
| avgOnlineDuration | number | 否 | 平均在线时长（分钟） |
| maxOnlineDuration | number | 否 | 最长在线时长（分钟） |

## 七、注意事项

1. 活跃度统计字段为可选字段，如果后端没有返回，前端应隐藏整个活跃度统计模块
2. 平均在线时长和最长在线时长的单位是分钟，前端显示时转换为小时（除以60）
3. 活跃度统计需要查询设备表的上报日志，可能影响查询性能，建议添加索引和缓存
4. 如果设备数据量很大，建议使用异步方式加载活跃度统计