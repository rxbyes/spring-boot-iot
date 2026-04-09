package com.ghlzm.iot.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 风险点设备待治理转正明细实体
 */
@Data
@TableName("risk_point_device_pending_promotion")
public class RiskPointDevicePendingPromotion implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long pendingBindingId;

    private Long riskPointDeviceId;

    private Long riskPointId;

    private Long deviceId;

    private String deviceCode;

    private String deviceName;

    private String metricIdentifier;

    private String metricName;

    private String promotionStatus;

    private String recommendationLevel;

    private Integer recommendationScore;

    private String evidenceSnapshotJson;

    private String promotionNote;

    private Long operatorId;

    private String operatorName;

    private Long tenantId;

    private Long createBy;

    private Date createTime;

    private Long updateBy;

    private Date updateTime;

    private Integer deleted;
}
