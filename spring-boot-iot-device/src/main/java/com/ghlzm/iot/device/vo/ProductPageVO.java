package com.ghlzm.iot.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 产品台账分页项，返回列表页需要展示的主数据与库存概览。
 */
@Data
public class ProductPageVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String productKey;

    private String productName;

    private String protocolCode;

    private Integer nodeType;

    private String dataFormat;

    private String manufacturer;

    private Integer status;

    private Long deviceCount;

    private Long onlineDeviceCount;

    private LocalDateTime lastReportTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
