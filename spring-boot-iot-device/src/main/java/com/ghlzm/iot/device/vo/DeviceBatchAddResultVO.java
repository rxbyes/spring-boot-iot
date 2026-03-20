package com.ghlzm.iot.device.vo;

import lombok.Data;

import java.util.List;

/**
 * 批量导入结果汇总。
 */
@Data
public class DeviceBatchAddResultVO {

    private Integer totalCount;

    private Integer successCount;

    private Integer failureCount;

    private List<String> createdDeviceCodes;

    private List<DeviceBatchAddErrorVO> errors;
}
