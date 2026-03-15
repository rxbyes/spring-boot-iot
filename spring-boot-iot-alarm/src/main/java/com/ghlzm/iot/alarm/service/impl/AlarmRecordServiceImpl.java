package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.alarm.entity.AlarmRecord;
import com.ghlzm.iot.alarm.mapper.AlarmRecordMapper;
import com.ghlzm.iot.alarm.service.AlarmRecordService;
import com.ghlzm.iot.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警记录服务实现类
 */
@Service
public class AlarmRecordServiceImpl extends ServiceImpl<AlarmRecordMapper, AlarmRecord> implements AlarmRecordService {

    @Override
    public AlarmRecord addAlarm(AlarmRecord alarm) {
        // 设置默认值
        if (alarm.getStatus() == null) {
            alarm.setStatus(0); // 0-未确认
        }
        alarm.setCreateTime(LocalDateTime.now());
        alarm.setCreateBy(1L); // 默认系统用户
        this.save(alarm);
        return alarm;
    }

    @Override
    public List<AlarmRecord> listAlarms(String deviceCode, Integer status, String alarmLevel) {
        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(deviceCode != null, AlarmRecord::getDeviceCode, deviceCode);
        wrapper.eq(status != null, AlarmRecord::getStatus, status);
        wrapper.eq(alarmLevel != null, AlarmRecord::getAlarmLevel, alarmLevel);
        wrapper.orderByDesc(AlarmRecord::getTriggerTime);
        return this.list(wrapper);
    }

    @Override
    public AlarmRecord getRequiredById(Long id) {
        AlarmRecord alarm = this.getById(id);
        if (alarm == null) {
            throw new BizException("告警记录不存在");
        }
        return alarm;
    }

    @Transactional
    @Override
    public void confirmAlarm(Long id, Long confirmUser) {
        AlarmRecord alarm = this.getRequiredById(id);
        if (alarm.getStatus() != 0) {
            throw new BizException("告警已处理，无法确认");
        }
        alarm.setStatus(1); // 1-已确认
        alarm.setConfirmTime(LocalDateTime.now().toString());
        alarm.setConfirmUser(confirmUser);
        this.updateById(alarm);
    }

    @Transactional
    @Override
    public void suppressAlarm(Long id, Long suppressUser) {
        AlarmRecord alarm = this.getRequiredById(id);
        if (alarm.getStatus() != 0) {
            throw new BizException("告警已处理，无法抑制");
        }
        alarm.setStatus(2); // 2-已抑制
        alarm.setSuppressTime(LocalDateTime.now().toString());
        alarm.setSuppressUser(suppressUser);
        this.updateById(alarm);
    }

    @Transactional
    @Override
    public void closeAlarm(Long id, Long closeUser) {
        AlarmRecord alarm = this.getRequiredById(id);
        if (alarm.getStatus() == 3) {
            throw new BizException("告警已关闭");
        }
        alarm.setStatus(3); // 3-已关闭
        alarm.setCloseTime(LocalDateTime.now().toString());
        alarm.setCloseUser(closeUser);
        this.updateById(alarm);
    }
}
