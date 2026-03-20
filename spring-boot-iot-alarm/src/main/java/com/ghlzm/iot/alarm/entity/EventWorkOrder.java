package com.ghlzm.iot.alarm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ghlzm.iot.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 事件工单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iot_event_work_order")
public class EventWorkOrder extends BaseEntity {

    /**
     * 事件ID
     */
    private Long eventId;

    /**
     * 事件编号
     */
    private String eventCode;

    /**
     * 工单编号
     */
    private String workOrderCode;

    /**
     * 工单类型
     */
    private String workOrderType;

    /**
     * 派发用户
     */
    private Long assignUser;

    /**
     * 接收用户
     */
    private Long receiveUser;

    /**
     * 接收时间
     */
    private String receiveTime;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 完成时间
     */
    private String completeTime;

    /**
     * 状态 0-待接收 1-已接收 2-处理中 3-已完成 4-已取消
     */
    private Integer status;

    /**
     * 现场反馈
     */
    private String feedback;

    /**
     * 照片URL（JSON数组）
     */
    private String photos;
}
