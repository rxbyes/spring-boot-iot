package com.ghlzm.iot.device.vo.messageflow;

import lombok.Data;

/**
 * message-flow session 聚合计数。
 */
@Data
public class MessageFlowSessionCountVO {

    private String transportMode;
    private String status;
    private Long count;
}
