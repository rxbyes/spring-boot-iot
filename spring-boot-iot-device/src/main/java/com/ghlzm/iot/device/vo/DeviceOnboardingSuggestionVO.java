package com.ghlzm.iot.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.List;

@Data
public class DeviceOnboardingSuggestionVO {

    private String traceId;

    private String deviceCode;

    private String deviceName;

    private String assetSourceType;

    private String productKey;

    private String protocolCode;

    private String lastFailureStage;

    private String lastErrorMessage;

    private String lastReportTopic;

    private String lastPayload;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long recommendedProductId;

    private String recommendedProductKey;

    private String recommendedProductName;

    private String recommendedFamilyCode;

    private String recommendedFamilyName;

    private String recommendedDecryptProfileCode;

    private String recommendedTemplateCode;

    private String recommendedTemplateName;

    private String suggestionStatus;

    private List<String> ruleGaps;
}
