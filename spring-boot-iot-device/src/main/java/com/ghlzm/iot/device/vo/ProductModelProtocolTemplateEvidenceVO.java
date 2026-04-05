package com.ghlzm.iot.device.vo;

import java.util.List;
import lombok.Data;

/**
 * 产品物模型治理可消费的协议模板证据快照。
 */
@Data
public class ProductModelProtocolTemplateEvidenceVO {

    private List<String> templateCodes;

    private List<String> logicalChannelCodes;

    private List<String> childDeviceCodes;

    private List<String> canonicalizationStrategies;

    private Boolean statusMirrorApplied;

    private List<String> parentRemovalKeys;

    private Integer templateExecutionCount;

    private Integer decodeFailureCount;
}
