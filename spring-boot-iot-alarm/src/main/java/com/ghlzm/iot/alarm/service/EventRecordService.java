package com.ghlzm.iot.alarm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.alarm.entity.EventRecord;

import java.util.List;

/**
 * 事件记录服务
 */
public interface EventRecordService extends IService<EventRecord> {

    /**
     * 新增事件记录
     */
    EventRecord addEvent(EventRecord event);

    /**
     * 查询事件列表
     */
    List<EventRecord> listEvents(String deviceCode, Integer status, String riskLevel);

    /**
     * 根据ID查询事件记录
     */
    EventRecord getRequiredById(Long id);

    /**
     * 工单派发
     */
    void dispatchEvent(Long id, Long dispatchUser, Long receiveUser);

    /**
     * 事件关闭
     */
    void closeEvent(Long id, Long closeUser, String closeReason);

    /**
     * 更新现场反馈
     */
    void updateFeedback(Long eventId, String feedback);
}
