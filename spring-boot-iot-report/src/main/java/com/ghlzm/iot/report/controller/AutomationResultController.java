package com.ghlzm.iot.report.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.report.service.AutomationResultQueryService;
import com.ghlzm.iot.report.vo.AutomationResultRunDetailVO;
import com.ghlzm.iot.report.vo.AutomationResultRunSummaryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 自动化结果查询接口
 */
@RestController
@RequestMapping("/api/report/automation-results")
public class AutomationResultController {

    private final AutomationResultQueryService automationResultQueryService;

    public AutomationResultController(AutomationResultQueryService automationResultQueryService) {
        this.automationResultQueryService = automationResultQueryService;
    }

    @GetMapping("/recent")
    public R<List<AutomationResultRunSummaryVO>> listRecentRuns(
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        return R.ok(automationResultQueryService.listRecentRuns(limit));
    }

    @GetMapping("/{runId}")
    public R<AutomationResultRunDetailVO> getRunDetail(@PathVariable String runId) {
        return R.ok(automationResultQueryService.getRunDetail(runId));
    }
}
