package com.ghlzm.iot.report.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.report.service.BusinessAcceptanceService;
import com.ghlzm.iot.report.vo.BusinessAcceptanceAccountTemplateVO;
import com.ghlzm.iot.report.vo.BusinessAcceptancePackageVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceResultVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceRunLaunchVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceRunRequest;
import com.ghlzm.iot.report.vo.BusinessAcceptanceRunStatusVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 业务验收查询接口
 */
@RestController
@RequestMapping("/api/report/business-acceptance")
public class BusinessAcceptanceController {

    private final BusinessAcceptanceService businessAcceptanceService;

    public BusinessAcceptanceController(BusinessAcceptanceService businessAcceptanceService) {
        this.businessAcceptanceService = businessAcceptanceService;
    }

    @GetMapping("/packages")
    public R<List<BusinessAcceptancePackageVO>> listPackages() {
        return R.ok(businessAcceptanceService.listPackages());
    }

    @GetMapping("/account-templates")
    public R<List<BusinessAcceptanceAccountTemplateVO>> listAccountTemplates() {
        return R.ok(businessAcceptanceService.listAccountTemplates());
    }

    @PostMapping("/runs")
    public R<BusinessAcceptanceRunLaunchVO> launchRun(@RequestBody BusinessAcceptanceRunRequest request) {
        return R.ok(businessAcceptanceService.launchRun(request));
    }

    @GetMapping("/runs/{jobId}")
    public R<BusinessAcceptanceRunStatusVO> getRunStatus(@PathVariable String jobId) {
        return R.ok(businessAcceptanceService.getRunStatus(jobId));
    }

    @GetMapping("/results/{runId}")
    public R<BusinessAcceptanceResultVO> getRunResult(
            @PathVariable String runId,
            @RequestParam String packageCode
    ) {
        return R.ok(businessAcceptanceService.getRunResult(packageCode, runId));
    }
}
