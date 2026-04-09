package com.ghlzm.iot.device.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceSecretRotationLogQuery;
import com.ghlzm.iot.device.vo.DeviceSecretRotationLogPageItemVO;

/**
 * 密钥托管轮换台账查询服务。
 */
public interface DeviceSecretRotationLogService {

    PageResult<DeviceSecretRotationLogPageItemVO> pageLogs(Long currentUserId,
                                                           DeviceSecretRotationLogQuery query,
                                                           Integer pageNum,
                                                           Integer pageSize);
}
