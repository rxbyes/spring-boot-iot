package com.ghlzm.iot.system.listener;

import com.ghlzm.iot.common.event.governance.RiskMetricCatalogPublishedEvent;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.service.GovernanceWorkItemService;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemCommand;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GovernanceWorkItemEventListener {

    private static final Long SYSTEM_OPERATOR_ID = 1L;

    private final GovernanceWorkItemService governanceWorkItemService;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public GovernanceWorkItemEventListener(GovernanceWorkItemService governanceWorkItemService) {
        this.governanceWorkItemService = governanceWorkItemService;
    }

    @EventListener
    public void onCatalogPublished(RiskMetricCatalogPublishedEvent event) {
        if (event == null || event.productId() == null) {
            return;
        }
        Long operatorUserId = SYSTEM_OPERATOR_ID;
        tryResolvePendingContractRelease(event.productId(), operatorUserId);
        governanceWorkItemService.openOrRefresh(new GovernanceWorkItemCommand(
                "PENDING_RISK_BINDING",
                "PRODUCT",
                event.productId(),
                event.productId(),
                null,
                event.releaseBatchId(),
                null,
                null,
                "RISK_BINDING",
                "目录已发布，待完成风险点绑定",
                buildCatalogSnapshotJson(event),
                "P1",
                operatorUserId
        ));
    }

    private void tryResolvePendingContractRelease(Long productId, Long operatorUserId) {
        try {
            governanceWorkItemService.resolve(
                    "PENDING_CONTRACT_RELEASE",
                    "PRODUCT",
                    productId,
                    operatorUserId,
                    "目录已发布"
            );
        } catch (BizException ignored) {
            // No pending contract-release work item exists yet; keep the binding task flow moving.
        }
    }

    private String buildCatalogSnapshotJson(RiskMetricCatalogPublishedEvent event) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("publishedRiskMetricIds", safeList(event.publishedRiskMetricIds()));
        snapshot.put("retiredRiskMetricIds", safeList(event.retiredRiskMetricIds()));
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<Long> safeList(List<Long> values) {
        return values == null ? List.of() : values.stream().filter(value -> value != null && value > 0).toList();
    }

}
