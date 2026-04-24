package com.ghlzm.iot.device.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceCapabilityExecuteDTO;
import com.ghlzm.iot.device.vo.CommandRecordPageItemVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityExecuteResultVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityOverviewVO;

public interface DeviceCapabilityService {

    DeviceCapabilityOverviewVO getCapabilities(Long currentUserId, String deviceCode);

    DeviceCapabilityExecuteResultVO execute(Long currentUserId,
                                            String deviceCode,
                                            String capabilityCode,
                                            DeviceCapabilityExecuteDTO dto);

    PageResult<CommandRecordPageItemVO> pageCommands(Long currentUserId,
                                                     String deviceCode,
                                                     String capabilityCode,
                                                     String status,
                                                     Long pageNum,
                                                     Long pageSize);
}
