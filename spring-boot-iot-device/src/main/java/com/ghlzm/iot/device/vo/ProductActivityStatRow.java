package com.ghlzm.iot.device.vo;

import lombok.Data;

/**
 * 产品详情页活跃度统计聚合结果。
 */
@Data
public class ProductActivityStatRow {

    private Long productId;

    private Long todayActiveCount;

    private Long sevenDaysActiveCount;

    private Long thirtyDaysActiveCount;
}
