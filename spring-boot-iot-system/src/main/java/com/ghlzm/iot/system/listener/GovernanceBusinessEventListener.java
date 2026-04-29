package com.ghlzm.iot.system.listener;

import com.ghlzm.iot.common.event.governance.ProductContractReleasedEvent;
import com.ghlzm.iot.common.event.governance.RiskMetricCatalogPublishedEvent;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.framework.observability.evidence.BusinessEventLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceRecorder;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceStatus;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GovernanceBusinessEventListener {

    private static final Long DEFAULT_TENANT_ID = 1L;

    private ObservabilityEvidenceRecorder evidenceRecorder = ObservabilityEvidenceRecorder.noop();

    @Autowired(required = false)
    public void setObservabilityEvidenceRecorder(ObservabilityEvidenceRecorder evidenceRecorder) {
        if (evidenceRecorder != null) {
            this.evidenceRecorder = evidenceRecorder;
        }
    }

    @EventListener
    public void onContractReleased(ProductContractReleasedEvent event) {
        if (event == null || event.productId() == null || event.releaseBatchId() == null) {
            return;
        }
        BusinessEventLogRecord record = new BusinessEventLogRecord();
        record.setTenantId(defaultTenantId(event.tenantId()));
        record.setTraceId(TraceContextHolder.currentOrCreate());
        record.setEventCode("product.contract.released");
        record.setEventName("正式合同发布完成");
        record.setDomainCode("product_contract");
        record.setActionCode("release");
        record.setObjectType("contract_release_batch");
        record.setObjectId(String.valueOf(event.releaseBatchId()));
        record.setActorUserId(event.operatorUserId());
        record.setResultStatus(ObservabilityEvidenceStatus.SUCCESS);
        record.setSourceType("PRODUCT_CONTRACT_RELEASE");
        record.setEvidenceType("contract_release_batch");
        record.setEvidenceId(String.valueOf(event.releaseBatchId()));
        record.setOccurredAt(LocalDateTime.now());
        record.getMetadata().putAll(buildContractMetadata(event));
        evidenceRecorder.recordBusinessEvent(record);
    }

    @EventListener
    public void onRiskMetricCatalogPublished(RiskMetricCatalogPublishedEvent event) {
        if (event == null || event.productId() == null || event.releaseBatchId() == null) {
            return;
        }
        BusinessEventLogRecord record = new BusinessEventLogRecord();
        record.setTenantId(defaultTenantId(event.tenantId()));
        record.setTraceId(TraceContextHolder.currentOrCreate());
        record.setEventCode("risk.metric_catalog.published");
        record.setEventName("风险指标目录发布完成");
        record.setDomainCode("risk_metric_catalog");
        record.setActionCode("publish");
        record.setObjectType("risk_metric_catalog");
        record.setObjectId(String.valueOf(event.releaseBatchId()));
        record.setActorUserId(event.operatorUserId());
        record.setResultStatus(ObservabilityEvidenceStatus.SUCCESS);
        record.setSourceType("RISK_METRIC_CATALOG");
        record.setEvidenceType("risk_metric_catalog");
        record.setEvidenceId(String.valueOf(event.releaseBatchId()));
        record.setOccurredAt(LocalDateTime.now());
        record.getMetadata().putAll(buildRiskCatalogMetadata(event));
        evidenceRecorder.recordBusinessEvent(record);
    }

    private Map<String, Object> buildContractMetadata(ProductContractReleasedEvent event) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("productId", event.productId());
        metadata.put("releaseBatchId", event.releaseBatchId());
        metadata.put("scenarioCode", event.scenarioCode());
        metadata.put("releasedIdentifiers", safeList(event.releasedIdentifiers()));
        metadata.put("releasedFieldCount", safeList(event.releasedIdentifiers()).size());
        metadata.put("approvalOrderId", event.approvalOrderId());
        return metadata;
    }

    private Map<String, Object> buildRiskCatalogMetadata(RiskMetricCatalogPublishedEvent event) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("productId", event.productId());
        metadata.put("releaseBatchId", event.releaseBatchId());
        metadata.put("publishedRiskMetricIds", safeList(event.publishedRiskMetricIds()));
        metadata.put("publishedRiskMetricCount", safeList(event.publishedRiskMetricIds()).size());
        metadata.put("retiredRiskMetricIds", safeList(event.retiredRiskMetricIds()));
        metadata.put("retiredRiskMetricCount", safeList(event.retiredRiskMetricIds()).size());
        return metadata;
    }

    private List<?> safeList(List<?> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private Long defaultTenantId(Long tenantId) {
        return tenantId == null || tenantId <= 0 ? DEFAULT_TENANT_ID : tenantId;
    }
}
