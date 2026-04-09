package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.VendorMetricEvidence;
import com.ghlzm.iot.device.mapper.VendorMetricEvidenceMapper;
import com.ghlzm.iot.device.service.ProductMetricEvidenceService;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpProtocolMetadata;
import com.ghlzm.iot.protocol.core.model.ProtocolMetricEvidence;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 产品字段证据服务实现。
 */
@Service
public class ProductMetricEvidenceServiceImpl implements ProductMetricEvidenceService {

    private final VendorMetricEvidenceMapper vendorMetricEvidenceMapper;
    private final ProductModelNormativeMatcher normativeMatcher = new ProductModelNormativeMatcher();

    public ProductMetricEvidenceServiceImpl(VendorMetricEvidenceMapper vendorMetricEvidenceMapper) {
        this.vendorMetricEvidenceMapper = vendorMetricEvidenceMapper;
    }

    @Override
    public void replaceManualEvidence(Long productId, String scenarioCode, List<VendorMetricEvidence> evidences) {
        saveEvidence(productId, scenarioCode, evidences);
    }

    @Override
    public void captureRuntimeEvidence(Product product, DeviceUpMessage upMessage) {
        if (product == null || product.getId() == null || upMessage == null) {
            return;
        }
        DeviceUpProtocolMetadata metadata = upMessage.getProtocolMetadata();
        if (metadata == null || metadata.getMetricEvidence() == null || metadata.getMetricEvidence().isEmpty()) {
            return;
        }
        String scenarioCode = normativeMatcher.resolveScenarioCode(product);
        if (!StringUtils.hasText(scenarioCode)) {
            return;
        }
        List<VendorMetricEvidence> evidences = buildRuntimeEvidence(upMessage, metadata.getMetricEvidence());
        saveEvidence(product.getId(), scenarioCode, evidences);
    }

    private List<VendorMetricEvidence> buildRuntimeEvidence(DeviceUpMessage upMessage,
                                                            List<ProtocolMetricEvidence> metricEvidence) {
        if (metricEvidence == null || metricEvidence.isEmpty()) {
            return List.of();
        }
        String targetDeviceCode = normalizeText(upMessage.getDeviceCode());
        LocalDateTime lastSeenTime = upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp();
        List<VendorMetricEvidence> evidences = new ArrayList<>();
        for (ProtocolMetricEvidence item : metricEvidence) {
            if (item == null || !StringUtils.hasText(item.getRawIdentifier())) {
                continue;
            }
            if (!belongsToTargetDevice(targetDeviceCode, item)) {
                continue;
            }
            VendorMetricEvidence evidence = new VendorMetricEvidence();
            evidence.setRawIdentifier(item.getRawIdentifier());
            evidence.setCanonicalIdentifier(normalizeText(item.getCanonicalIdentifier()));
            evidence.setLogicalChannelCode(normalizeText(item.getLogicalChannelCode()));
            evidence.setParentDeviceCode(normalizeText(item.getParentDeviceCode()));
            evidence.setChildDeviceCode(normalizeText(item.getChildDeviceCode()));
            evidence.setEvidenceOrigin(normalizeText(item.getEvidenceOrigin()));
            evidence.setSampleValue(normalizeText(item.getSampleValue()));
            evidence.setValueType(normalizeText(item.getValueType()));
            evidence.setEvidenceCount(1);
            evidence.setLastSeenTime(lastSeenTime);
            evidences.add(evidence);
        }
        return evidences;
    }

    private boolean belongsToTargetDevice(String targetDeviceCode, ProtocolMetricEvidence evidence) {
        if (!StringUtils.hasText(targetDeviceCode)) {
            return false;
        }
        String childDeviceCode = normalizeText(evidence.getChildDeviceCode());
        if (StringUtils.hasText(childDeviceCode)) {
            return targetDeviceCode.equalsIgnoreCase(childDeviceCode);
        }
        String parentDeviceCode = normalizeText(evidence.getParentDeviceCode());
        if (StringUtils.hasText(parentDeviceCode)) {
            return targetDeviceCode.equalsIgnoreCase(parentDeviceCode);
        }
        return false;
    }

    private void saveEvidence(Long productId, String scenarioCode, List<VendorMetricEvidence> evidences) {
        if (productId == null || evidences == null || evidences.isEmpty()) {
            return;
        }
        for (VendorMetricEvidence evidence : evidences) {
            if (evidence == null || !StringUtils.hasText(evidence.getRawIdentifier())) {
                continue;
            }
            VendorMetricEvidence existing = vendorMetricEvidenceMapper.selectOne(new LambdaQueryWrapper<VendorMetricEvidence>()
                    .eq(VendorMetricEvidence::getDeleted, 0)
                    .eq(VendorMetricEvidence::getProductId, productId)
                    .eq(VendorMetricEvidence::getRawIdentifier, evidence.getRawIdentifier())
                    .eq(StringUtils.hasText(evidence.getLogicalChannelCode()), VendorMetricEvidence::getLogicalChannelCode, evidence.getLogicalChannelCode())
                    .isNull(!StringUtils.hasText(evidence.getLogicalChannelCode()), VendorMetricEvidence::getLogicalChannelCode));
            if (existing == null) {
                evidence.setProductId(productId);
                if (evidence.getEvidenceCount() == null || evidence.getEvidenceCount() <= 0) {
                    evidence.setEvidenceCount(1);
                }
                if (evidence.getLastSeenTime() == null) {
                    evidence.setLastSeenTime(LocalDateTime.now());
                }
                if (!StringUtils.hasText(evidence.getMetadataJson())) {
                    evidence.setMetadataJson(buildScenarioMetadata(scenarioCode));
                }
                vendorMetricEvidenceMapper.insert(evidence);
                continue;
            }
            existing.setCanonicalIdentifier(firstNonBlank(evidence.getCanonicalIdentifier(), existing.getCanonicalIdentifier()));
            existing.setParentDeviceCode(firstNonBlank(evidence.getParentDeviceCode(), existing.getParentDeviceCode()));
            existing.setChildDeviceCode(firstNonBlank(evidence.getChildDeviceCode(), existing.getChildDeviceCode()));
            existing.setLogicalChannelCode(firstNonBlank(evidence.getLogicalChannelCode(), existing.getLogicalChannelCode()));
            existing.setEvidenceOrigin(firstNonBlank(evidence.getEvidenceOrigin(), existing.getEvidenceOrigin()));
            existing.setSampleValue(firstNonBlank(evidence.getSampleValue(), existing.getSampleValue()));
            existing.setValueType(firstNonBlank(evidence.getValueType(), existing.getValueType()));
            existing.setEvidenceCount(firstPositive(existing.getEvidenceCount()) + firstPositive(evidence.getEvidenceCount()));
            existing.setLastSeenTime(firstNonNull(evidence.getLastSeenTime(), existing.getLastSeenTime(), LocalDateTime.now()));
            existing.setMetadataJson(firstNonBlank(evidence.getMetadataJson(), existing.getMetadataJson(), buildScenarioMetadata(scenarioCode)));
            vendorMetricEvidenceMapper.updateById(existing);
        }
    }

    private String buildScenarioMetadata(String scenarioCode) {
        if (!StringUtils.hasText(scenarioCode)) {
            return null;
        }
        return "{\"scenarioCode\":\"" + scenarioCode + "\"}";
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private int firstPositive(Integer value) {
        return value == null || value <= 0 ? 0 : value;
    }
}
