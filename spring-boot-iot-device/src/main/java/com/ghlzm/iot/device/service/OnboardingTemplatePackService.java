package com.ghlzm.iot.device.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.OnboardingTemplatePackCreateDTO;
import com.ghlzm.iot.device.dto.OnboardingTemplatePackPageQueryDTO;
import com.ghlzm.iot.device.dto.OnboardingTemplatePackUpdateDTO;
import com.ghlzm.iot.device.vo.OnboardingTemplatePackVO;

/**
 * 无代码接入模板包服务。
 */
public interface OnboardingTemplatePackService {

    PageResult<OnboardingTemplatePackVO> pagePacks(OnboardingTemplatePackPageQueryDTO query);

    OnboardingTemplatePackVO createPack(OnboardingTemplatePackCreateDTO dto, Long operatorUserId);

    OnboardingTemplatePackVO updatePack(Long packId, OnboardingTemplatePackUpdateDTO dto, Long operatorUserId);
}
