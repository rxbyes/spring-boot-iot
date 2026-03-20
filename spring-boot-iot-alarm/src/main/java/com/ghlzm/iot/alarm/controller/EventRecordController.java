package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.EventRecord;
import com.ghlzm.iot.alarm.entity.EventWorkOrder;
import com.ghlzm.iot.alarm.service.EventRecordService;
import com.ghlzm.iot.alarm.service.EventWorkOrderService;
import com.ghlzm.iot.common.response.R;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 事件记录控制器
 */
@RestController
@RequestMapping("/api/event")
public class EventRecordController {

    private final EventRecordService eventRecordService;
    private final EventWorkOrderService eventWorkOrderService;

    public EventRecordController(EventRecordService eventRecordService, EventWorkOrderService eventWorkOrderService) {
        this.eventRecordService = eventRecordService;
        this.eventWorkOrderService = eventWorkOrderService;
    }

    /**
     * 新增事件记录
     */
    @PostMapping("/add")
    public R<EventRecord> add(@RequestBody EventRecord event) {
        return R.ok(eventRecordService.addEvent(event));
    }

    /**
     * 查询事件列表
     */
    @GetMapping("/list")
    public R<List<EventRecord>> list(
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String riskLevel) {
        return R.ok(eventRecordService.listEvents(deviceCode, status, riskLevel));
    }

    /**
     * 根据ID查询事件记录
     */
    @GetMapping("/{id}")
    public R<EventRecord> getById(@PathVariable("id") Long id) {
        return R.ok(eventRecordService.getRequiredById(id));
    }

    /**
     * 工单派发
     */
    @PostMapping("/{id}/dispatch")
    public R<Void> dispatch(
            @PathVariable("id") Long id,
            @RequestParam("dispatchUser") Long dispatchUser,
            @RequestParam("receiveUser") Long receiveUser) {
        eventRecordService.dispatchEvent(id, dispatchUser, receiveUser);
        return R.ok();
    }

    /**
     * 事件关闭
     */
    @PostMapping("/{id}/close")
    public R<Void> close(
            @PathVariable("id") Long id,
            @RequestParam("closeUser") Long closeUser,
            @RequestParam("closeReason") String closeReason) {
        eventRecordService.closeEvent(id, closeUser, closeReason);
        return R.ok();
    }

    /**
     * 更新现场反馈
     */
    @PostMapping("/{eventId}/feedback")
    public R<Void> updateFeedback(
            @PathVariable("eventId") Long eventId,
            @RequestParam("feedback") String feedback) {
        eventRecordService.updateFeedback(eventId, feedback);
        return R.ok();
    }

    /**
     * 查询工单列表
     */
    @GetMapping("/work-orders")
    public R<List<EventWorkOrder>> listWorkOrders(
            @RequestParam(required = false) Long receiveUser,
            @RequestParam(required = false) Integer status) {
        return R.ok(eventWorkOrderService.listWorkOrders(receiveUser, status));
    }

    /**
     * 接收工单
     */
    @PostMapping("/work-orders/{id}/receive")
    public R<Void> receiveWorkOrder(
            @PathVariable("id") Long id,
            @RequestParam("receiveUser") Long receiveUser) {
        eventWorkOrderService.receiveWorkOrder(id, receiveUser);
        return R.ok();
    }

    /**
     * 开始处理
     */
    @PostMapping("/work-orders/{id}/start")
    public R<Void> startProcessing(
            @PathVariable("id") Long id,
            @RequestParam("receiveUser") Long receiveUser) {
        eventWorkOrderService.startProcessing(id, receiveUser);
        return R.ok();
    }

    /**
     * 完成处理
     */
    @PostMapping("/work-orders/{id}/complete")
    public R<Void> completeProcessing(
            @PathVariable("id") Long id,
            @RequestParam("feedback") String feedback,
            @RequestParam(required = false) String photos) {
        eventWorkOrderService.completeProcessing(id, feedback, photos);
        return R.ok();
    }
}
