package com.ghlzm.iot.report.service;

import com.ghlzm.iot.report.vo.BusinessAcceptanceAccountTemplateVO;
import com.ghlzm.iot.report.vo.BusinessAcceptancePackageVO;

import java.util.List;

/**
 * 业务验收服务
 */
public interface BusinessAcceptanceService {

    List<BusinessAcceptancePackageVO> listPackages();

    List<BusinessAcceptanceAccountTemplateVO> listAccountTemplates();
}
