package com.ghlzm.iot.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("iot_product_model")
public class ProductModel {

    private Long id;
    private Long tenantId;
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
    private Integer deleted;
}
