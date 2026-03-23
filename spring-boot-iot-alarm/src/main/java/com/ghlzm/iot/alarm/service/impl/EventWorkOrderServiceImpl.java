package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.alarm.entity.EventWorkOrder;
import com.ghlzm.iot.alarm.mapper.EventWorkOrderMapper;
import com.ghlzm.iot.alarm.service.EventWorkOrderService;
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
 * 事件工单服务实现类
 */
@Service
public class EventWorkOrderServiceImpl extends ServiceImpl<EventWorkOrderMapper, EventWorkOrder> implements EventWorkOrderService {

    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final Long DEFAULT_OPERATOR_ID = 1L;
    private static final Logger log = LoggerFactory.getLogger(EventWorkOrderServiceImpl.class);

    private final InAppMessagePublisher inAppMessagePublisher;

    public EventWorkOrderServiceImpl(InAppMessagePublisher inAppMessagePublisher) {
        this.inAppMessagePublisher = inAppMessagePublisher;
    }

    @Override
    public List<EventWorkOrder> listWorkOrders(Long receiveUser, Integer status) {
        LambdaQueryWrapper<EventWorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(receiveUser != null, EventWorkOrder::getReceiveUser, receiveUser);
        wrapper.eq(status != null, EventWorkOrder::getStatus, status);
        wrapper.orderByDesc(EventWorkOrder::getCreateTime);
        return this.list(wrapper);
    }

    @Transactional
    @Override
    public void receiveWorkOrder(Long id, Long receiveUser) {
        long startNs = System.nanoTime();
        EventWorkOrder workOrder = null;
        try {
            workOrder = this.getById(id);
            if (workOrder == null) {
                throw new BizException("工单不存在");
            }
            if (!workOrder.getReceiveUser().equals(receiveUser)) {
                throw new BizException("工单未分配给当前用户");
            }
            if (workOrder.getStatus() != 0) {
                throw new BizException("工单已处理");
            }
            workOrder.setStatus(1); // 1-已接收
            workOrder.setReceiveTime(LocalDateTime.now().toString());
            this.updateById(workOrder);
            publishWorkOrderNotice(workOrder, "receive");
            log.info(buildWorkOrderSummary("receive", "success", startNs, workOrder, receiveUser, null));
        } catch (RuntimeException ex) {
            log.warn(buildWorkOrderSummary("receive", "failure", startNs, workOrder, receiveUser, ex), ex);
            throw ex;
        }
    }

    @Transactional
    @Override
    public void startProcessing(Long id, Long receiveUser) {
        long startNs = System.nanoTime();
        EventWorkOrder workOrder = null;
        try {
            workOrder = this.getById(id);
            if (workOrder == null) {
                throw new BizException("工单不存在");
            }
            if (!workOrder.getReceiveUser().equals(receiveUser)) {
                throw new BizException("工单未分配给当前用户");
            }
            if (workOrder.getStatus() != 1) {
                throw new BizException("工单状态不允许开始处理");
            }
            workOrder.setStatus(2); // 2-处理中
            workOrder.setStartTime(LocalDateTime.now().toString());
            this.updateById(workOrder);
            publishWorkOrderNotice(workOrder, "start");
            log.info(buildWorkOrderSummary("start", "success", startNs, workOrder, receiveUser, null));
        } catch (RuntimeException ex) {
            log.warn(buildWorkOrderSummary("start", "failure", startNs, workOrder, receiveUser, ex), ex);
            throw ex;
        }
    }

    @Transactional
    @Override
    public void completeProcessing(Long id, String feedback, String photos) {
        long startNs = System.nanoTime();
        EventWorkOrder workOrder = null;
        try {
            workOrder = this.getById(id);
            if (workOrder == null) {
                throw new BizException("工单不存在");
            }
            if (workOrder.getStatus() != 2) {
                throw new BizException("工单状态不允许完成处理");
            }
            workOrder.setStatus(3); // 3-已完成
            workOrder.setFeedback(feedback);
            workOrder.setPhotos(photos);
            workOrder.setCompleteTime(LocalDateTime.now().toString());
            this.updateById(workOrder);
            publishWorkOrderNotice(workOrder, "complete");
            log.info(buildWorkOrderSummary("complete", "success", startNs, workOrder, null, null));
        } catch (RuntimeException ex) {
            log.warn(buildWorkOrderSummary("complete", "failure", startNs, workOrder, null, ex), ex);
            throw ex;
        }
    }

    private void publishWorkOrderNotice(EventWorkOrder workOrder, String action) {
        Long targetUserId = switch (action) {
            case "receive", "start" -> workOrder.getReceiveUser();
            case "complete" -> workOrder.getAssignUser();
            default -> null;
        };
        if (targetUserId == null) {
            return;
        }
        String workOrderCode = workOrder.getWorkOrderCode() == null ? String.valueOf(workOrder.getId()) : workOrder.getWorkOrderCode();
        String eventCode = workOrder.getEventCode() == null ? String.valueOf(workOrder.getEventId()) : workOrder.getEventCode();
        String title = switch (action) {
            case "receive" -> "工单已接收";
            case "start" -> "工单处理中";
            case "complete" -> "工单已完成";
            default -> "工单状态更新";
        };
        String statusText = switch (action) {
            case "receive" -> "已接收";
            case "start" -> "处理中";
            case "complete" -> "已完成";
            default -> "状态更新";
        };
        String summary = switch (action) {
            case "receive" -> "工单 " + workOrderCode + " 已被接收，请关注后续处理进展。";
            case "start" -> "工单 " + workOrderCode + " 已开始处理，请持续跟进处置进度。";
            case "complete" -> "工单 " + workOrderCode + " 已完成处理，请尽快复核闭环结果。";
            default -> "工单 " + workOrderCode + " 状态已更新。";
        };
        String actionAdvice = "complete".equals(action)
                ? "请前往事件协同台复核反馈内容，并视情况推进事件关闭。"
                : "请前往事件协同台查看最新工单状态与处理记录。";
        inAppMessagePublisher.publish(InAppMessagePublishCommand.builder()
                .tenantId(DEFAULT_TENANT_ID)
                .messageType("business")
                .priority("complete".equals(action) ? "high" : "medium")
                .title(title)
                .summary(summary)
                .content("""
                        工单编号：%s
                        事件编号：%s
                        当前状态：%s
                        建议动作：%s
                        """.formatted(workOrderCode, eventCode, statusText, actionAdvice))
                .targetType("user")
                .targetUserIds(List.of(targetUserId))
                .relatedPath("/event-disposal")
                .sourceType("work_order")
                .sourceId(workOrder.getId() + ":" + action)
                .operatorId(DEFAULT_OPERATOR_ID)
                .build());
    }

    private String buildWorkOrderSummary(String action,
                                         String result,
                                         long startNs,
                                         EventWorkOrder workOrder,
                                         Long operatorUser,
                                         Throwable error) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", TraceContextHolder.getTraceId());
        details.put("action", action);
        details.put("workOrderId", workOrder == null ? null : workOrder.getId());
        details.put("workOrderCode", workOrder == null ? null : workOrder.getWorkOrderCode());
        details.put("eventId", workOrder == null ? null : workOrder.getEventId());
        details.put("eventCode", workOrder == null ? null : workOrder.getEventCode());
        details.put("status", workOrder == null ? null : workOrder.getStatus());
        details.put("assignUser", workOrder == null ? null : workOrder.getAssignUser());
        details.put("receiveUser", workOrder == null ? null : workOrder.getReceiveUser());
        details.put("operatorUser", operatorUser);
        if (error != null) {
            details.put("errorClass", error.getClass().getSimpleName());
            details.put("reason", error.getMessage());
        }
        return ObservabilityEventLogSupport.summary(
                "work_order_lifecycle",
                result,
                elapsedMillis(startNs),
                details
        );
    }

    private long elapsedMillis(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }
}
