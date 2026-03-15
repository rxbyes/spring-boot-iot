package com.ghlzm.iot.alarm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.alarm.entity.AlarmRecord;

import java.util.List;

/**
 * 告警记录服务
 */
public interface AlarmRecordService extends IService<AlarmRecord> {

    /**
     * 新增告警记录
     */
    AlarmRecord addAlarm(AlarmRecord alarm);

    /**
     * 查询告警列表
     */
    List<AlarmRecord> listAlarms(String deviceCode, Integer status, String alarmLevel);

    /**
     * 根据ID查询告警记录
     */
    AlarmRecord getRequiredById(Long id);

    /**
     * 确认告警
     */
    void confirmAlarm(Long id, Long confirmUser);

    /**
     * 抑制告警
     */
    void suppressAlarm(Long id, Long suppressUser);

    /**
     * 关闭告警
     */
    void closeAlarm(Long id, Long closeUser);
}
