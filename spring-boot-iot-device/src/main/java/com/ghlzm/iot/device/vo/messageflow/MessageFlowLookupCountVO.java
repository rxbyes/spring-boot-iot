package com.ghlzm.iot.device.vo.messageflow;

import lombok.Data;

/**
 * message-flow 查询计数。
 */
@Data
public class MessageFlowLookupCountVO {

    private String target;
    private String result;
    private Long count;
}
