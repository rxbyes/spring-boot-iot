package com.ghlzm.iot.device.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ghlzm.iot.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 13:48
 */
@Data
@TableName("iot_product")
@EqualsAndHashCode(callSuper = true, exclude = {
        "todayActiveCount",
        "sevenDaysActiveCount",
        "thirtyDaysActiveCount",
        "avgOnlineDuration",
        "maxOnlineDuration"
})
public class Product extends BaseEntity {

    private String productKey;
    private String productName;
    private String protocolCode;
    private Integer nodeType;
    private String dataFormat;
    private String manufacturer;
    private String description;
    private Integer status;

    @TableField(exist = false)
    private Long todayActiveCount;

    @TableField(exist = false)
    private Long sevenDaysActiveCount;

    @TableField(exist = false)
    private Long thirtyDaysActiveCount;

    @TableField(exist = false)
    private Long avgOnlineDuration;

    @TableField(exist = false)
    private Long maxOnlineDuration;
}
