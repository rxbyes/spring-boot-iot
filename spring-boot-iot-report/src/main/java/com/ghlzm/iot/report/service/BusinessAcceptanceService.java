package com.ghlzm.iot.report.service;

import com.ghlzm.iot.report.vo.BusinessAcceptanceAccountTemplateVO;
import com.ghlzm.iot.report.vo.BusinessAcceptancePackageVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceResultVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceRunLaunchVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceRunRequest;
import com.ghlzm.iot.report.vo.BusinessAcceptanceRunStatusVO;

import java.util.List;

/**
 * 业务验收服务
 */
public interface BusinessAcceptanceService {

    List<BusinessAcceptancePackageVO> listPackages();

    List<BusinessAcceptanceAccountTemplateVO> listAccountTemplates();

    BusinessAcceptanceRunLaunchVO launchRun(BusinessAcceptanceRunRequest request);

    BusinessAcceptanceRunStatusVO getRunStatus(String jobId);

    BusinessAcceptanceResultVO getRunResult(String packageCode, String runId);
}
