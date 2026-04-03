package com.ghlzm.iot.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 风险点设备待治理绑定实体
 */
@Data
@TableName("risk_point_device_pending_binding")
public class RiskPointDevicePendingBinding implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchNo;

    private String sourceFileName;

    private Integer sourceRowNo;

    private String riskPointName;

    private Long riskPointId;

    private String riskPointCode;

    private String deviceCode;

    private Long deviceId;

    private String deviceName;

    private String resolutionStatus;

    private String resolutionNote;

    private String metricIdentifier;

    private String metricName;

    private Long promotedBindingId;

    private Date promotedTime;

    private Long tenantId;

    private Long createBy;

    private Date createTime;

    private Long updateBy;

    private Date updateTime;

    private Integer deleted;
}
