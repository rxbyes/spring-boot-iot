package com.ghlzm.iot.framework.notification;

/**
 * 站内消息内部发布器。
 */
public interface InAppMessagePublisher {

    InAppMessagePublishResult publish(InAppMessagePublishCommand command);
}
