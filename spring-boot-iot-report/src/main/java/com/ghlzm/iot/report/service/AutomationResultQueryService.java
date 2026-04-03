package com.ghlzm.iot.report.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.report.vo.AutomationResultEvidenceContentVO;
import com.ghlzm.iot.report.vo.AutomationResultEvidenceItemVO;
import com.ghlzm.iot.report.vo.AutomationResultRunDetailVO;
import com.ghlzm.iot.report.vo.AutomationResultRunSummaryVO;

import java.util.List;

/**
 * 自动化结果查询服务。
 */
public interface AutomationResultQueryService {

    /**
     * 分页查询历史运行台账。
     */
    PageResult<AutomationResultRunSummaryVO> pageRuns(
            Integer pageNum,
            Integer pageSize,
            String keyword,
            String status,
            String runnerType,
            String dateFrom,
            String dateTo
    );

    /**
     * 查询最近运行结果。
     */
    List<AutomationResultRunSummaryVO> listRecentRuns(Integer limit);

    /**
     * 查询指定运行详情。
     */
    AutomationResultRunDetailVO getRunDetail(String runId);

    /**
     * 查询指定运行关联的证据清单。
     */
    List<AutomationResultEvidenceItemVO> listRunEvidence(String runId);

    /**
     * 读取指定运行的证据文本预览。
     */
    AutomationResultEvidenceContentVO getEvidenceContent(String runId, String path);
}
