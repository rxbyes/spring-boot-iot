package com.ghlzm.iot.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 风险指标目录只读快照。
 */
@Data
@TableName("risk_metric_catalog")
public class RiskMetricCatalogReadModel {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long productId;

    private String contractIdentifier;

    private Integer insightEnabled;

    private Integer enabled;

    private Integer deleted;
}
