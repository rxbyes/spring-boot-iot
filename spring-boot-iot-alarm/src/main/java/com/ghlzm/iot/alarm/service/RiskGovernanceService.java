package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery;
import com.ghlzm.iot.alarm.vo.RiskGovernanceCoverageOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceDashboardOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceGapItemVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceReleaseBatchDiffVO;
import com.ghlzm.iot.alarm.vo.RiskMetricCatalogItemVO;
import com.ghlzm.iot.common.response.PageResult;
import java.util.List;

/**
 * 风险治理缺口服务。
 */
public interface RiskGovernanceService {

    PageResult<RiskGovernanceGapItemVO> listMissingBindings(RiskGovernanceGapQuery query);

    PageResult<RiskGovernanceGapItemVO> listMissingPolicies(RiskGovernanceGapQuery query);

    List<MissingPolicyAlertSignal> listMissingPolicyAlertSignals();

    PageResult<RiskMetricCatalogItemVO> pageMetricCatalogs(Long productId, Long releaseBatchId, Long pageNum, Long pageSize);

    RiskGovernanceReleaseBatchDiffVO compareReleaseBatches(Long baselineBatchId, Long targetBatchId);

    RiskMetricCatalogItemVO getMetricCatalog(Long id);

    RiskGovernanceCoverageOverviewVO getCoverageOverview(Long productId);

    RiskGovernanceDashboardOverviewVO getDashboardOverview();

    record MissingPolicyAlertSignal(String dimensionKey,
                                    String dimensionLabel,
                                    Long riskMetricId,
                                    String metricIdentifier,
                                    String metricName,
                                    long bindingCount,
                                    long riskPointCount) {
    }
}
