package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.alarm.entity.AlarmRecord;
import com.ghlzm.iot.alarm.mapper.AlarmRecordMapper;
import com.ghlzm.iot.alarm.service.AlarmRecordService;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.observability.ObservabilityEventLogSupport;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警记录服务实现类
 */
@Service
public class AlarmRecordServiceImpl extends ServiceImpl<AlarmRecordMapper, AlarmRecord> implements AlarmRecordService {

    private static final Logger log = LoggerFactory.getLogger(AlarmRecordServiceImpl.class);

    @Override
    public AlarmRecord addAlarm(AlarmRecord alarm) {
        long startNs = System.nanoTime();
        try {
            // 设置默认值
            if (alarm.getStatus() == null) {
                alarm.setStatus(0); // 0-未确认
            }
            alarm.setCreateTime(LocalDateTime.now());
            alarm.setCreateBy(1L); // 默认系统用户
            this.save(alarm);
            log.info(buildAlarmSummary("create", "success", startNs, alarm, null, null));
            return alarm;
        } catch (RuntimeException ex) {
            log.warn(buildAlarmSummary("create", "failure", startNs, alarm, null, ex), ex);
            throw ex;
        }
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
        long startNs = System.nanoTime();
        AlarmRecord alarm = null;
        try {
            alarm = this.getRequiredById(id);
            if (alarm.getStatus() != 0) {
                throw new BizException("告警已处理，无法确认");
            }
            alarm.setStatus(1); // 1-已确认
            alarm.setConfirmTime(LocalDateTime.now().toString());
            alarm.setConfirmUser(confirmUser);
            this.updateById(alarm);
            log.info(buildAlarmSummary("confirm", "success", startNs, alarm, confirmUser, null));
        } catch (RuntimeException ex) {
            log.warn(buildAlarmSummary("confirm", "failure", startNs, alarm, confirmUser, ex), ex);
            throw ex;
        }
    }

    @Transactional
    @Override
    public void suppressAlarm(Long id, Long suppressUser) {
        long startNs = System.nanoTime();
        AlarmRecord alarm = null;
        try {
            alarm = this.getRequiredById(id);
            if (alarm.getStatus() != 0) {
                throw new BizException("告警已处理，无法抑制");
            }
            alarm.setStatus(2); // 2-已抑制
            alarm.setSuppressTime(LocalDateTime.now().toString());
            alarm.setSuppressUser(suppressUser);
            this.updateById(alarm);
            log.info(buildAlarmSummary("suppress", "success", startNs, alarm, suppressUser, null));
        } catch (RuntimeException ex) {
            log.warn(buildAlarmSummary("suppress", "failure", startNs, alarm, suppressUser, ex), ex);
            throw ex;
        }
    }

    @Transactional
    @Override
    public void closeAlarm(Long id, Long closeUser) {
        long startNs = System.nanoTime();
        AlarmRecord alarm = null;
        try {
            alarm = this.getRequiredById(id);
            if (alarm.getStatus() == 3) {
                throw new BizException("告警已关闭");
            }
            alarm.setStatus(3); // 3-已关闭
            alarm.setCloseTime(LocalDateTime.now().toString());
            alarm.setCloseUser(closeUser);
            this.updateById(alarm);
            log.info(buildAlarmSummary("close", "success", startNs, alarm, closeUser, null));
        } catch (RuntimeException ex) {
            log.warn(buildAlarmSummary("close", "failure", startNs, alarm, closeUser, ex), ex);
            throw ex;
        }
    }

    private String buildAlarmSummary(String action,
                                     String result,
                                     long startNs,
                                     AlarmRecord alarm,
                                     Long operatorUser,
                                     Throwable error) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", TraceContextHolder.getTraceId());
        details.put("action", action);
        details.put("alarmId", alarm == null ? null : alarm.getId());
        details.put("alarmCode", alarm == null ? null : alarm.getAlarmCode());
        details.put("deviceCode", alarm == null ? null : alarm.getDeviceCode());
        details.put("alarmLevel", alarm == null ? null : alarm.getAlarmLevel());
        details.put("status", alarm == null ? null : alarm.getStatus());
        details.put("operatorUser", operatorUser);
        if (error != null) {
            details.put("errorClass", error.getClass().getSimpleName());
            details.put("reason", error.getMessage());
        }
        return ObservabilityEventLogSupport.summary(
                "alarm_lifecycle",
                result,
                elapsedMillis(startNs),
                details
        );
    }

    private long elapsedMillis(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }
}
