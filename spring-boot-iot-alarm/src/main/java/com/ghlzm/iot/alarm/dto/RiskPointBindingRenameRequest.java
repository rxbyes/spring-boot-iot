package com.ghlzm.iot.alarm.dto;

import lombok.Data;

/**
 * Risk point metric binding display-name update request.
 */
@Data
public class RiskPointBindingRenameRequest {

    private String metricName;
}
