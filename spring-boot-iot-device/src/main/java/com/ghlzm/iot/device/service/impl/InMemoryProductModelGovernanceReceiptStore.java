package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.service.ProductModelGovernanceReceiptStore;
import com.ghlzm.iot.device.vo.ProductModelProtocolTemplateEvidenceVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 当前阶段仅需支撑同一应用实例内 compare -> apply 的会话级模板回执复用。
 */
@Service
public class InMemoryProductModelGovernanceReceiptStore implements ProductModelGovernanceReceiptStore {

    private static final Duration RECEIPT_TTL = Duration.ofMinutes(30);

    private final ConcurrentMap<String, ReceiptEntry> store = new ConcurrentHashMap<>();

    @Override
    public void replaceProtocolTemplateEvidence(Long productId,
                                                Map<String, ProductModelProtocolTemplateEvidenceVO> evidenceByIdentifier) {
        String storeKey = resolveStoreKey(productId);
        if (storeKey == null) {
            return;
        }
        pruneExpiredEntries();
        if (evidenceByIdentifier == null || evidenceByIdentifier.isEmpty()) {
            store.remove(storeKey);
            return;
        }
        store.put(storeKey, new ReceiptEntry(copyEvidenceMap(evidenceByIdentifier), System.currentTimeMillis() + RECEIPT_TTL.toMillis()));
    }

    @Override
    public Map<String, ProductModelProtocolTemplateEvidenceVO> loadProtocolTemplateEvidence(Long productId) {
        String storeKey = resolveStoreKey(productId);
        if (storeKey == null) {
            return Map.of();
        }
        ReceiptEntry entry = store.get(storeKey);
        if (entry == null) {
            return Map.of();
        }
        if (entry.expiresAt() < System.currentTimeMillis()) {
            store.remove(storeKey, entry);
            return Map.of();
        }
        return copyEvidenceMap(entry.evidenceByIdentifier());
    }

    private void pruneExpiredEntries() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
    }

    private String resolveStoreKey(Long productId) {
        if (productId == null) {
            return null;
        }
        return productId + ":" + resolveUserScope();
    }

    private String resolveUserScope() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserPrincipal principal && principal.userId() != null) {
            return "user:" + principal.userId();
        }
        if (authentication != null && StringUtils.hasText(authentication.getName())
                && !"anonymousUser".equalsIgnoreCase(authentication.getName().trim())) {
            return "principal:" + authentication.getName().trim();
        }
        return "anonymous";
    }

    private Map<String, ProductModelProtocolTemplateEvidenceVO> copyEvidenceMap(
            Map<String, ProductModelProtocolTemplateEvidenceVO> source) {
        Map<String, ProductModelProtocolTemplateEvidenceVO> copied = new LinkedHashMap<>();
        source.forEach((identifier, evidence) -> {
            if (StringUtils.hasText(identifier) && evidence != null) {
                copied.put(identifier.trim(), copyEvidence(evidence));
            }
        });
        return copied;
    }

    private ProductModelProtocolTemplateEvidenceVO copyEvidence(ProductModelProtocolTemplateEvidenceVO source) {
        ProductModelProtocolTemplateEvidenceVO target = new ProductModelProtocolTemplateEvidenceVO();
        target.setTemplateCodes(copyList(source.getTemplateCodes()));
        target.setLogicalChannelCodes(copyList(source.getLogicalChannelCodes()));
        target.setChildDeviceCodes(copyList(source.getChildDeviceCodes()));
        target.setCanonicalizationStrategies(copyList(source.getCanonicalizationStrategies()));
        target.setStatusMirrorApplied(source.getStatusMirrorApplied());
        target.setParentRemovalKeys(copyList(source.getParentRemovalKeys()));
        target.setTemplateExecutionCount(source.getTemplateExecutionCount());
        target.setDecodeFailureCount(source.getDecodeFailureCount());
        return target;
    }

    private ArrayList<String> copyList(java.util.List<String> source) {
        return source == null ? null : new ArrayList<>(source);
    }

    private record ReceiptEntry(Map<String, ProductModelProtocolTemplateEvidenceVO> evidenceByIdentifier,
                                long expiresAt) {
    }
}
