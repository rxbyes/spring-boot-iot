package com.ghlzm.iot.system.service;

import com.ghlzm.iot.framework.observability.BackendExceptionEvent;
import com.ghlzm.iot.system.entity.AuditLog;

/**
 * 系统异常自动通知服务。
 */
public interface SystemErrorNotificationService {

    void notifySystemError(BackendExceptionEvent event, AuditLog auditLog);

    void sendTestNotification(String channelCode);
    void sendTestNotification(Long currentUserId, String channelCode);
}
