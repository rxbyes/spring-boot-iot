package com.ghlzm.iot.system.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.service.model.ObservabilityBusinessEventPageQuery;
import com.ghlzm.iot.system.service.model.ObservabilityMessageArchiveBatchPageQuery;
import com.ghlzm.iot.system.service.model.ObservabilityScheduledTaskPageQuery;
import com.ghlzm.iot.system.service.model.ObservabilitySlowSpanSummaryQuery;
import com.ghlzm.iot.system.service.model.ObservabilitySlowSpanTrendQuery;
import com.ghlzm.iot.system.service.model.ObservabilitySpanPageQuery;
import com.ghlzm.iot.system.vo.ObservabilityBusinessEventVO;
import com.ghlzm.iot.system.vo.ObservabilityMessageArchiveBatchReportPreviewVO;
import com.ghlzm.iot.system.vo.ObservabilityMessageArchiveBatchVO;
import com.ghlzm.iot.system.vo.ObservabilityScheduledTaskVO;
import com.ghlzm.iot.system.vo.ObservabilitySlowSpanSummaryVO;
import com.ghlzm.iot.system.vo.ObservabilitySlowSpanTrendVO;
import com.ghlzm.iot.system.vo.ObservabilitySpanVO;
import com.ghlzm.iot.system.vo.ObservabilityTraceEvidenceVO;
import java.util.List;

public interface ObservabilityEvidenceQueryService {

    PageResult<ObservabilityBusinessEventVO> pageBusinessEvents(ObservabilityBusinessEventPageQuery query, Long currentUserId);

    PageResult<ObservabilitySpanVO> pageSpans(ObservabilitySpanPageQuery query, Long currentUserId);

    PageResult<ObservabilityScheduledTaskVO> pageScheduledTasks(ObservabilityScheduledTaskPageQuery query,
                                                                Long currentUserId);

    PageResult<ObservabilityMessageArchiveBatchVO> pageMessageArchiveBatches(
            ObservabilityMessageArchiveBatchPageQuery query,
            Long currentUserId
    );

    ObservabilityMessageArchiveBatchReportPreviewVO getMessageArchiveBatchReportPreview(String batchNo, Long currentUserId);

    List<ObservabilitySlowSpanSummaryVO> listSlowSpanSummaries(ObservabilitySlowSpanSummaryQuery query, Long currentUserId);

    List<ObservabilitySlowSpanTrendVO> listSlowSpanTrends(ObservabilitySlowSpanTrendQuery query, Long currentUserId);

    ObservabilityTraceEvidenceVO getTraceEvidence(String traceId, Long currentUserId);
}
