package com.ghlzm.iot.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 产品详情视图，给产品定义中心返回完整维护信息和库存概览。
 */
@Data
public class ProductDetailVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String productKey;

    private String productName;

    private String protocolCode;

    private Integer nodeType;

    private String dataFormat;

    private String manufacturer;

    private String description;

    private Integer status;

    private Long deviceCount;

    private Long onlineDeviceCount;

    private LocalDateTime lastReportTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
