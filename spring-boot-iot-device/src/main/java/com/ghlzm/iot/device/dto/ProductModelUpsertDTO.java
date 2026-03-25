package com.ghlzm.iot.device.dto;

import lombok.Data;

/**
 * 产品物模型新增/编辑请求体。
 */
@Data
public class ProductModelUpsertDTO {

    private String modelType;

    private String identifier;

    private String modelName;

    private String dataType;

    private String specsJson;

    private String eventType;

    private String serviceInputJson;

    private String serviceOutputJson;

    private Integer sortNo;

    private Integer requiredFlag;

    private String description;
}
