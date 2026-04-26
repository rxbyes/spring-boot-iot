package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.service.ObservabilityEvidenceQueryService;
import com.ghlzm.iot.system.service.model.ObservabilityBusinessEventPageQuery;
import com.ghlzm.iot.system.service.model.ObservabilityMessageArchiveBatchPageQuery;
import com.ghlzm.iot.system.service.model.ObservabilityScheduledTaskPageQuery;
import com.ghlzm.iot.system.service.model.ObservabilitySlowSpanSummaryQuery;
import com.ghlzm.iot.system.service.model.ObservabilitySlowSpanTrendQuery;
import com.ghlzm.iot.system.service.model.ObservabilitySpanPageQuery;
import com.ghlzm.iot.system.vo.ObservabilityBusinessEventVO;
import com.ghlzm.iot.system.vo.ObservabilityMessageArchiveBatchCompareVO;
import com.ghlzm.iot.system.vo.ObservabilityMessageArchiveBatchReportPreviewVO;
import com.ghlzm.iot.system.vo.ObservabilityMessageArchiveBatchVO;
import com.ghlzm.iot.system.vo.ObservabilityScheduledTaskVO;
import com.ghlzm.iot.system.vo.ObservabilitySlowSpanSummaryVO;
import com.ghlzm.iot.system.vo.ObservabilitySlowSpanTrendVO;
import com.ghlzm.iot.system.vo.ObservabilitySpanVO;
import com.ghlzm.iot.system.vo.ObservabilityTraceEvidenceVO;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/observability")
public class ObservabilityEvidenceController {

    private final ObservabilityEvidenceQueryService observabilityEvidenceQueryService;

    public ObservabilityEvidenceController(ObservabilityEvidenceQueryService observabilityEvidenceQueryService) {
        this.observabilityEvidenceQueryService = observabilityEvidenceQueryService;
    }

    @GetMapping("/business-events/page")
    public R<PageResult<ObservabilityBusinessEventVO>> pageBusinessEvents(ObservabilityBusinessEventPageQuery query,
                                                                          Authentication authentication) {
        return R.ok(observabilityEvidenceQueryService.pageBusinessEvents(query, requireCurrentUserId(authentication)));
    }

    @GetMapping("/spans/page")
    public R<PageResult<ObservabilitySpanVO>> pageSpans(ObservabilitySpanPageQuery query,
                                                        Authentication authentication) {
        return R.ok(observabilityEvidenceQueryService.pageSpans(query, requireCurrentUserId(authentication)));
    }

    @GetMapping("/scheduled-tasks/page")
    public R<PageResult<ObservabilityScheduledTaskVO>> pageScheduledTasks(ObservabilityScheduledTaskPageQuery query,
                                                                          Authentication authentication) {
        return R.ok(observabilityEvidenceQueryService.pageScheduledTasks(query, requireCurrentUserId(authentication)));
    }

    @GetMapping("/message-archive-batches/page")
    public R<PageResult<ObservabilityMessageArchiveBatchVO>> pageMessageArchiveBatches(
            ObservabilityMessageArchiveBatchPageQuery query,
            Authentication authentication
    ) {
        return R.ok(observabilityEvidenceQueryService.pageMessageArchiveBatches(
                query,
                requireCurrentUserId(authentication)
        ));
    }

    @GetMapping("/message-archive-batches/report-preview")
    public R<ObservabilityMessageArchiveBatchReportPreviewVO> getMessageArchiveBatchReportPreview(
            String batchNo,
            Authentication authentication
    ) {
        return R.ok(observabilityEvidenceQueryService.getMessageArchiveBatchReportPreview(
                batchNo,
                requireCurrentUserId(authentication)
        ));
    }

    @GetMapping("/message-archive-batches/compare")
    public R<ObservabilityMessageArchiveBatchCompareVO> getMessageArchiveBatchCompare(
            String batchNo,
            Authentication authentication
    ) {
        return R.ok(observabilityEvidenceQueryService.getMessageArchiveBatchCompare(
                batchNo,
                requireCurrentUserId(authentication)
        ));
    }

    @GetMapping("/spans/slow-summary")
    public R<List<ObservabilitySlowSpanSummaryVO>> listSlowSpanSummaries(ObservabilitySlowSpanSummaryQuery query,
                                                                         Authentication authentication) {
        return R.ok(observabilityEvidenceQueryService.listSlowSpanSummaries(query, requireCurrentUserId(authentication)));
    }

    @GetMapping("/spans/slow-trends")
    public R<List<ObservabilitySlowSpanTrendVO>> listSlowSpanTrends(ObservabilitySlowSpanTrendQuery query,
                                                                    Authentication authentication) {
        return R.ok(observabilityEvidenceQueryService.listSlowSpanTrends(query, requireCurrentUserId(authentication)));
    }

    @GetMapping("/trace/{traceId}")
    public R<ObservabilityTraceEvidenceVO> getTraceEvidence(@PathVariable String traceId,
                                                            Authentication authentication) {
        return R.ok(observabilityEvidenceQueryService.getTraceEvidence(traceId, requireCurrentUserId(authentication)));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }
}
