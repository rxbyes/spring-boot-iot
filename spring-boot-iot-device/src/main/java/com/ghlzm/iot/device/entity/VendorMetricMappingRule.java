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
 * 厂商字段映射规则。
 */
@Data
@TableName("iot_vendor_metric_mapping_rule")
public class VendorMetricMappingRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String scopeType;

    private Long productId;

    private String protocolCode;

    private String scenarioCode;

    private String deviceFamily;

    private String rawIdentifier;

    private String logicalChannelCode;

    private String relationConditionJson;

    private String normalizationRuleJson;

    private String targetNormativeIdentifier;

    private String status;

    private Integer versionNo;

    private Long approvalOrderId;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
