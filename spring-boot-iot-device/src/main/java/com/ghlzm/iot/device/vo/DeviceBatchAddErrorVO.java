package com.ghlzm.iot.device.vo;

import lombok.Data;

/**
 * 批量导入失败明细。
 */
@Data
public class DeviceBatchAddErrorVO {

    private Integer rowNo;

    private String deviceCode;

    private String message;
}
