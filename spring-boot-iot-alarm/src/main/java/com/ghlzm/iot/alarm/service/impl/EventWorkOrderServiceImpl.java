package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.alarm.entity.EventWorkOrder;
import com.ghlzm.iot.alarm.mapper.EventWorkOrderMapper;
import com.ghlzm.iot.alarm.service.EventWorkOrderService;
import com.ghlzm.iot.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件工单服务实现类
 */
@Service
public class EventWorkOrderServiceImpl extends ServiceImpl<EventWorkOrderMapper, EventWorkOrder> implements EventWorkOrderService {

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
        EventWorkOrder workOrder = this.getById(id);
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
    }

    @Transactional
    @Override
    public void startProcessing(Long id, Long receiveUser) {
        EventWorkOrder workOrder = this.getById(id);
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
    }

    @Transactional
    @Override
    public void completeProcessing(Long id, String feedback, String photos) {
        EventWorkOrder workOrder = this.getById(id);
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
    }
}
