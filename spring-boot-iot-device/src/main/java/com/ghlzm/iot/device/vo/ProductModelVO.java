package com.ghlzm.iot.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 产品物模型视图对象。
 */
@Data
public class ProductModelVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productId;

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
