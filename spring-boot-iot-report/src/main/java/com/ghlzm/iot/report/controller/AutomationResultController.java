package com.ghlzm.iot.report.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.report.vo.AutomationResultArchiveFacetVO;
import com.ghlzm.iot.report.vo.AutomationResultArchiveRefreshVO;
import com.ghlzm.iot.report.service.AutomationResultQueryService;
import com.ghlzm.iot.report.vo.AutomationResultEvidenceContentVO;
import com.ghlzm.iot.report.vo.AutomationResultEvidenceItemVO;
import com.ghlzm.iot.report.vo.AutomationResultRunDetailVO;
import com.ghlzm.iot.report.vo.AutomationResultRunSummaryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/page")
    public R<PageResult<AutomationResultRunSummaryVO>> pageRuns(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String runnerType,
            @RequestParam(required = false) String packageCode,
            @RequestParam(required = false) String environmentCode,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo
    ) {
        return R.ok(automationResultQueryService.pageRuns(
                pageNum,
                pageSize,
                keyword,
                status,
                runnerType,
                packageCode,
                environmentCode,
                dateFrom,
                dateTo
        ));
    }

    @GetMapping("/recent")
    public R<List<AutomationResultRunSummaryVO>> listRecentRuns(
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        return R.ok(automationResultQueryService.listRecentRuns(limit));
    }

    @GetMapping("/facets")
    public R<AutomationResultArchiveFacetVO> listFacets() {
        return R.ok(automationResultQueryService.listFacets());
    }

    @PostMapping("/refresh-index")
    public R<AutomationResultArchiveRefreshVO> refreshIndex() {
        return R.ok(automationResultQueryService.refreshIndex());
    }

    @GetMapping("/{runId}")
    public R<AutomationResultRunDetailVO> getRunDetail(@PathVariable String runId) {
        return R.ok(automationResultQueryService.getRunDetail(runId));
    }

    @GetMapping("/{runId}/evidence")
    public R<List<AutomationResultEvidenceItemVO>> listRunEvidence(@PathVariable String runId) {
        return R.ok(automationResultQueryService.listRunEvidence(runId));
    }

    @GetMapping("/{runId}/evidence/content")
    public R<AutomationResultEvidenceContentVO> getEvidenceContent(
            @PathVariable String runId,
            @RequestParam String path
    ) {
        return R.ok(automationResultQueryService.getEvidenceContent(runId, path));
    }
}
