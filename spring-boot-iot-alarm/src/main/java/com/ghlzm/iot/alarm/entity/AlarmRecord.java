package com.ghlzm.iot.alarm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ghlzm.iot.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 告警记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iot_alarm_record")
public class AlarmRecord extends BaseEntity {

    /**
     * 告警编号
     */
    private String alarmCode;

    /**
     * 告警标题
     */
    private String alarmTitle;

    /**
     * 告警类型
     */
    private String alarmType;

    /**
     * 告警等级
     */
    private String alarmLevel;

    /**
     * 区域ID
     */
    private Long regionId;

    /**
     * 区域名称
     */
    private String regionName;

    /**
     * 风险点ID
     */
    private Long riskPointId;

    /**
     * 风险点名称
     */
    private String riskPointName;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 设备编码
     */
    private String deviceCode;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 测点名称
     */
    private String metricName;

    /**
     * 当前值
     */
    private String currentValue;

    /**
     * 阈值
     */
    private String thresholdValue;

    /**
     * 状态 0-未确认 1-已确认 2-已抑制 3-已关闭
     */
    private Integer status;

    /**
     * 触发时间
     */
    private String triggerTime;

    /**
     * 确认时间
     */
    private String confirmTime;

    /**
     * 确认用户
     */
    private Long confirmUser;

    /**
     * 抑制时间
     */
    private String suppressTime;

    /**
     * 抑制用户
     */
    private Long suppressUser;

    /**
     * 关闭时间
     */
    private String closeTime;

    /**
     * 关闭用户
     */
    private Long closeUser;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 规则名称
     */
    private String ruleName;
}
