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
 * 规范字段定义。
 */
@Data
@TableName("iot_normative_metric_definition")
public class NormativeMetricDefinition {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private String scenarioCode;

    private String deviceFamily;

    private String identifier;

    private String displayName;

    private String unit;

    private Integer precisionDigits;

    private String monitorContentCode;

    private String monitorTypeCode;

    private Integer riskEnabled;

    private Integer trendEnabled;

    private String metadataJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
