package com.ghlzm.iot.alarm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.alarm.entity.EventWorkOrder;

import java.util.List;

/**
 * 事件工单服务
 */
public interface EventWorkOrderService extends IService<EventWorkOrder> {

    /**
     * 查询工单列表
     */
    List<EventWorkOrder> listWorkOrders(Long receiveUser, Integer status);

    /**
     * 接收工单
     */
    void receiveWorkOrder(Long id, Long receiveUser);

    /**
     * 开始处理
     */
    void startProcessing(Long id, Long receiveUser);

    /**
     * 完成处理
     */
    void completeProcessing(Long id, String feedback, String photos);
}
