package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.dto.DeviceSecretRotateDTO;
import com.ghlzm.iot.device.vo.DeviceSecretRotateResultVO;

/**
 * Device secret custody service.
 */
public interface DeviceSecretCustodyService {

    DeviceSecretRotateResultVO rotateDeviceSecret(Long currentUserId,
                                                  Long deviceId,
                                                  Long approverUserId,
                                                  DeviceSecretRotateDTO dto);
}
