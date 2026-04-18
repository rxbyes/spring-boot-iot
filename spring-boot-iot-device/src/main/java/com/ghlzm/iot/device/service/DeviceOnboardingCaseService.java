package com.ghlzm.iot.device.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseCreateDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseQueryDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseUpdateDTO;
import com.ghlzm.iot.device.vo.DeviceOnboardingCaseVO;

/**
 * 无代码接入案例服务。
 */
public interface DeviceOnboardingCaseService {

    PageResult<DeviceOnboardingCaseVO> pageCases(DeviceOnboardingCaseQueryDTO query);

    DeviceOnboardingCaseVO createCase(DeviceOnboardingCaseCreateDTO dto, Long operatorUserId);

    DeviceOnboardingCaseVO getCase(Long caseId);

    DeviceOnboardingCaseVO updateCase(Long caseId, DeviceOnboardingCaseUpdateDTO dto, Long operatorUserId);

    DeviceOnboardingCaseVO refreshStatus(Long caseId, Long operatorUserId);
}
