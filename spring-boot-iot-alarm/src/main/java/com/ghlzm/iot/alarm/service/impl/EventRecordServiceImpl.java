package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.alarm.entity.EventRecord;
import com.ghlzm.iot.alarm.entity.EventWorkOrder;
import com.ghlzm.iot.alarm.mapper.EventRecordMapper;
import com.ghlzm.iot.alarm.mapper.EventWorkOrderMapper;
import com.ghlzm.iot.alarm.service.EventRecordService;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.observability.ObservabilityEventLogSupport;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.framework.notification.InAppMessagePublishCommand;
import com.ghlzm.iot.framework.notification.InAppMessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 事件记录服务实现类
 */
@Service
public class EventRecordServiceImpl extends ServiceImpl<EventRecordMapper, EventRecord> implements EventRecordService {

    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final Long DEFAULT_OPERATOR_ID = 1L;
    private static final Logger log = LoggerFactory.getLogger(EventRecordServiceImpl.class);

    private final EventWorkOrderMapper eventWorkOrderMapper;
    private final InAppMessagePublisher inAppMessagePublisher;

    public EventRecordServiceImpl(EventWorkOrderMapper eventWorkOrderMapper,
                                  InAppMessagePublisher inAppMessagePublisher) {
        this.eventWorkOrderMapper = eventWorkOrderMapper;
        this.inAppMessagePublisher = inAppMessagePublisher;
    }

    @Override
    public EventRecord addEvent(EventRecord event) {
        long startNs = System.nanoTime();
        try {
            if (event.getStatus() == null) {
                event.setStatus(0); // 0-待派发
            }
            event.setCreateTime(LocalDateTime.now());
            event.setCreateBy(1L); // 默认系统用户
            this.save(event);
            log.info(buildEventSummary("create", "success", startNs, event, null, null, null, null));
            return event;
        } catch (RuntimeException ex) {
            log.warn(buildEventSummary("create", "failure", startNs, event, null, null, null, ex), ex);
            throw ex;
        }
    }

    @Override
    public List<EventRecord> listEvents(String deviceCode, Integer status, String riskLevel) {
        LambdaQueryWrapper<EventRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(deviceCode != null, EventRecord::getDeviceCode, deviceCode);
        wrapper.eq(status != null, EventRecord::getStatus, status);
        wrapper.eq(riskLevel != null, EventRecord::getRiskLevel, riskLevel);
        wrapper.orderByDesc(EventRecord::getTriggerTime);
        return this.list(wrapper);
    }

    @Override
    public EventRecord getRequiredById(Long id) {
        EventRecord event = this.getById(id);
        if (event == null) {
            throw new BizException("事件记录不存在");
        }
        return event;
    }

    @Transactional
    @Override
    public void dispatchEvent(Long id, Long dispatchUser, Long receiveUser) {
        long startNs = System.nanoTime();
        EventRecord event = null;
        EventWorkOrder workOrder = null;
        try {
            event = this.getRequiredById(id);
            if (event.getStatus() != 0) {
                throw new BizException("事件已派发，无法再次派发");
            }
            event.setStatus(1); // 1-已派发
            event.setDispatchTime(LocalDateTime.now().toString());
            event.setDispatchUser(dispatchUser);
            this.updateById(event);

            // 创建工单记录
            workOrder = new EventWorkOrder();
            workOrder.setEventId(event.getId());
            workOrder.setEventCode(event.getEventCode());
            workOrder.setWorkOrderCode("WO-" + LocalDateTime.now().toString().replace("-", "").replace(":", "").replace("T", "") + (int)(Math.random() * 1000));
            workOrder.setWorkOrderType("event-dispatch");
            workOrder.setAssignUser(dispatchUser);
            workOrder.setReceiveUser(receiveUser);
            workOrder.setStatus(0); // 0-待接收
            workOrder.setCreateTime(LocalDateTime.now());
            workOrder.setCreateBy(1L);
            eventWorkOrderMapper.insert(workOrder);
            publishDispatchNotice(event, workOrder, receiveUser);
            log.info(buildEventSummary("dispatch", "success", startNs, event, workOrder, dispatchUser, receiveUser, null));
        } catch (RuntimeException ex) {
            log.warn(buildEventSummary("dispatch", "failure", startNs, event, workOrder, dispatchUser, receiveUser, ex), ex);
            throw ex;
        }
    }

    @Transactional
    @Override
    public void closeEvent(Long id, Long closeUser, String closeReason) {
        long startNs = System.nanoTime();
        EventRecord event = null;
        try {
            event = this.getRequiredById(id);
            if (event.getStatus() == 4) {
                throw new BizException("事件已关闭");
            }
            event.setStatus(4); // 4-已关闭
            event.setCloseTime(LocalDateTime.now().toString());
            event.setCloseUser(closeUser);
            event.setCloseReason(closeReason);
            this.updateById(event);
            log.info(buildEventSummary("close", "success", startNs, event, null, closeUser, null, null));
        } catch (RuntimeException ex) {
            log.warn(buildEventSummary("close", "failure", startNs, event, null, closeUser, null, ex), ex);
            throw ex;
        }
    }

    @Transactional
    @Override
    public void updateFeedback(Long eventId, String feedback) {
        long startNs = System.nanoTime();
        EventRecord event = null;
        EventWorkOrder workOrder = null;
        try {
            event = this.getRequiredById(eventId);
            if (event.getStatus() != 2) {
                throw new BizException("事件状态不允许更新反馈");
            }
            // 更新工单反馈
            LambdaQueryWrapper<EventWorkOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(EventWorkOrder::getEventId, eventId);
            workOrder = eventWorkOrderMapper.selectOne(wrapper);
            if (workOrder != null) {
                workOrder.setFeedback(feedback);
                workOrder.setStatus(3); // 3-已完成
                workOrder.setCompleteTime(LocalDateTime.now().toString());
                eventWorkOrderMapper.updateById(workOrder);
            }
            log.info(buildEventSummary("feedback", "success", startNs, event, workOrder, null, null, null));
        } catch (RuntimeException ex) {
            log.warn(buildEventSummary("feedback", "failure", startNs, event, workOrder, null, null, ex), ex);
            throw ex;
        }
    }

    private void publishDispatchNotice(EventRecord event, EventWorkOrder workOrder, Long receiveUser) {
        if (receiveUser == null) {
            return;
        }
        String eventCode = event.getEventCode() == null ? String.valueOf(event.getId()) : event.getEventCode();
        String workOrderCode = workOrder.getWorkOrderCode() == null ? String.valueOf(workOrder.getId()) : workOrder.getWorkOrderCode();
        inAppMessagePublisher.publish(InAppMessagePublishCommand.builder()
                .tenantId(DEFAULT_TENANT_ID)
                .messageType("business")
                .priority("high")
                .title("事件已派工，请及时接收")
                .summary("事件 " + eventCode + " 已派发至你，请前往事件协同台处理。")
                .content("""
                        事件编号：%s
                        工单编号：%s
                        当前状态：待接收
                        建议动作：请尽快进入事件协同台完成工单接收，并确认现场处理安排。
                        """.formatted(eventCode, workOrderCode))
                .targetType("user")
                .targetUserIds(List.of(receiveUser))
                .relatedPath("/event-disposal")
                .sourceType("event_dispatch")
                .sourceId(String.valueOf(event.getId()))
                .operatorId(DEFAULT_OPERATOR_ID)
                .build());
    }

    private String buildEventSummary(String action,
                                     String result,
                                     long startNs,
                                     EventRecord event,
                                     EventWorkOrder workOrder,
                                     Long operatorUser,
                                     Long receiveUser,
                                     Throwable error) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", TraceContextHolder.getTraceId());
        details.put("action", action);
        details.put("eventId", event == null ? null : event.getId());
        details.put("eventCode", event == null ? null : event.getEventCode());
        details.put("deviceCode", event == null ? null : event.getDeviceCode());
        details.put("riskLevel", event == null ? null : event.getRiskLevel());
        details.put("status", event == null ? null : event.getStatus());
        details.put("workOrderId", workOrder == null ? null : workOrder.getId());
        details.put("workOrderCode", workOrder == null ? null : workOrder.getWorkOrderCode());
        details.put("operatorUser", operatorUser);
        details.put("receiveUser", receiveUser);
        if (error != null) {
            details.put("errorClass", error.getClass().getSimpleName());
            details.put("reason", error.getMessage());
        }
        return ObservabilityEventLogSupport.summary(
                "event_lifecycle",
                result,
                elapsedMillis(startNs),
                details
        );
    }

    private long elapsedMillis(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }
}
