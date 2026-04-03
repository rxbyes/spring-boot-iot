package com.ghlzm.iot.alarm.dto;

import lombok.Data;

/**
 * 待治理忽略请求。
 */
@Data
public class RiskPointPendingIgnoreRequest {

    private String ignoreNote;
}
