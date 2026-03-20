package com.ghlzm.iot.alarm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ghlzm.iot.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 事件记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iot_event_record")
public class EventRecord extends BaseEntity {

    /**
     * 事件编号
     */
    private String eventCode;

    /**
     * 事件标题
     */
    private String eventTitle;

    /**
     * 告警ID
     */
    private Long alarmId;

    /**
     * 告警编号
     */
    private String alarmCode;

    /**
     * 告警等级
     */
    private String alarmLevel;

    /**
     * 风险等级
     */
    private String riskLevel;

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
     * 状态 0-待派发 1-已派发 2-处理中 3-待验收 4-已关闭 5-已取消
     */
    private Integer status;

    /**
     * 责任人
     */
    private Long responsibleUser;

    /**
     * 紧急程度
     */
    private String urgencyLevel;

    /**
     * 到场时限（分钟）
     */
    private Integer arrivalTimeLimit;

    /**
     * 完成时限（分钟）
     */
    private Integer completionTimeLimit;

    /**
     * 触发时间
     */
    private String triggerTime;

    /**
     * 派发时间
     */
    private String dispatchTime;

    /**
     * 派发用户
     */
    private Long dispatchUser;

    /**
     * 处理开始时间
     */
    private String startTime;

    /**
     * 处理完成时间
     */
    private String completeTime;

    /**
     * 关闭时间
     */
    private String closeTime;

    /**
     * 关闭用户
     */
    private Long closeUser;

    /**
     * 关闭原因
     */
    private String closeReason;

    /**
     * 复盘记录
     */
    private String reviewNotes;
}
