package com.ghlzm.iot.report.service;

import com.ghlzm.iot.report.vo.AutomationResultRunDetailVO;
import com.ghlzm.iot.report.vo.AutomationResultRunSummaryVO;

import java.util.List;

/**
 * 自动化结果查询服务
 */
public interface AutomationResultQueryService {

    /**
     * 查询最近运行结果
     */
    List<AutomationResultRunSummaryVO> listRecentRuns(Integer limit);

    /**
     * 查询指定运行详情
     */
    AutomationResultRunDetailVO getRunDetail(String runId);
}
