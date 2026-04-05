package com.ghlzm.iot.device.vo;

import java.util.List;
import lombok.Data;

/**
 * 产品物模型治理 apply 回执项。
 */
@Data
public class ProductModelGovernanceAppliedItemVO {

    private String modelType;

    private String identifier;

    private String decision;

    private List<String> templateCodes;

    private List<String> canonicalizationStrategies;

    private List<String> childDeviceCodes;
}
