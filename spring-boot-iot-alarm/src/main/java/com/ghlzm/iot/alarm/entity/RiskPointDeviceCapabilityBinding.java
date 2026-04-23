package com.ghlzm.iot.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 风险点设备级正式绑定真相。
 */
@Data
@TableName("risk_point_device_capability_binding")
public class RiskPointDeviceCapabilityBinding implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long riskPointId;

    private Long deviceId;

    private String deviceCode;

    private String deviceName;

    private String deviceCapabilityType;

    private String extensionStatus;

    private Long tenantId;

    private Date createTime;

    private Date updateTime;

    private Long createBy;

    private Long updateBy;

    private Integer deleted;
}
