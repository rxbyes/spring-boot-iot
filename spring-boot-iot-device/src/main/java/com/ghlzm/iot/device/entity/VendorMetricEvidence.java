package com.ghlzm.iot.device.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 厂商字段证据。
 */
@Data
@TableName("iot_vendor_metric_evidence")
public class VendorMetricEvidence {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private Long productId;

    private String parentDeviceCode;

    private String childDeviceCode;

    private String rawIdentifier;

    private String canonicalIdentifier;

    private String logicalChannelCode;

    private String evidenceOrigin;

    private String sampleValue;

    private String valueType;

    private Integer evidenceCount;

    private LocalDateTime lastSeenTime;

    private String metadataJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
