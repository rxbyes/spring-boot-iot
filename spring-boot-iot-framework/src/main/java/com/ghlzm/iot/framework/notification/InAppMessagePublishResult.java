package com.ghlzm.iot.framework.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 站内消息内部发布结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InAppMessagePublishResult {

    private Long messageId;

    private String dedupKey;

    private boolean dedupKeyHit;

    private boolean created;
}
