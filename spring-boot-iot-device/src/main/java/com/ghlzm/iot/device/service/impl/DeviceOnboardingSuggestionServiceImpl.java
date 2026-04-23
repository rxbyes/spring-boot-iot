package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.DeviceOnboardingSuggestionQuery;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.DeviceOnboardingSuggestionService;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.UnregisteredDeviceRosterService;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import com.ghlzm.iot.device.vo.DeviceOnboardingSuggestionVO;
import com.ghlzm.iot.device.vo.DevicePageVO;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.protocol.ProtocolSecurityDefinitionProvider;
import com.ghlzm.iot.framework.protocol.template.entity.ProtocolTemplateDefinitionSnapshot;
import com.ghlzm.iot.framework.protocol.template.mapper.ProtocolTemplateDefinitionSnapshotMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class DeviceOnboardingSuggestionServiceImpl implements DeviceOnboardingSuggestionService {

    private static final String STATUS_READY = "READY";
    private static final String STATUS_PARTIAL = "PARTIAL";

    private final UnregisteredDeviceRosterService rosterService;
    private final ProductMapper productMapper;
    private final ProtocolSecurityDefinitionProvider protocolSecurityDefinitionProvider;
    private final ProtocolTemplateDefinitionSnapshotMapper protocolTemplateDefinitionSnapshotMapper;
    private final PublishedProductContractSnapshotService publishedProductContractSnapshotService;

    public DeviceOnboardingSuggestionServiceImpl(UnregisteredDeviceRosterService rosterService,
                                                 ProductMapper productMapper,
                                                 @Qualifier("publishedProtocolSecurityDefinitionProvider")
                                                 ProtocolSecurityDefinitionProvider protocolSecurityDefinitionProvider,
                                                 ProtocolTemplateDefinitionSnapshotMapper protocolTemplateDefinitionSnapshotMapper,
                                                 PublishedProductContractSnapshotService publishedProductContractSnapshotService) {
        this.rosterService = rosterService;
        this.productMapper = productMapper;
        this.protocolSecurityDefinitionProvider = protocolSecurityDefinitionProvider;
        this.protocolTemplateDefinitionSnapshotMapper = protocolTemplateDefinitionSnapshotMapper;
        this.publishedProductContractSnapshotService = publishedProductContractSnapshotService;
    }

    @Override
    public DeviceOnboardingSuggestionVO suggest(Long tenantId, DeviceOnboardingSuggestionQuery query) {
        String traceId = normalizeText(query == null ? null : query.getTraceId());
        if (!StringUtils.hasText(traceId)) {
            throw new BizException("traceId 不能为空");
        }
        DevicePageVO candidate = rosterService.findByTraceId(tenantId, traceId);
        if (candidate == null) {
            throw new BizException("未找到未登记设备线索: " + traceId);
        }

        DeviceOnboardingSuggestionVO suggestion = new DeviceOnboardingSuggestionVO();
        suggestion.setTraceId(traceId);
        suggestion.setDeviceCode(candidate.getDeviceCode());
        suggestion.setDeviceName(candidate.getDeviceName());
        suggestion.setAssetSourceType(candidate.getAssetSourceType());
        suggestion.setProductKey(candidate.getProductKey());
        suggestion.setProtocolCode(candidate.getProtocolCode());
        suggestion.setLastFailureStage(candidate.getLastFailureStage());
        suggestion.setLastErrorMessage(candidate.getLastErrorMessage());
        suggestion.setLastReportTopic(candidate.getLastReportTopic());
        suggestion.setLastPayload(candidate.getLastPayload());

        List<String> ruleGaps = new ArrayList<>();

        Product product = resolveProduct(tenantId, candidate.getProductKey());
        if (product == null) {
            if (StringUtils.hasText(candidate.getProductKey())) {
                ruleGaps.add("当前 productKey 尚未命中正式产品档案，请先完成产品建模。");
            } else {
                ruleGaps.add("当前 trace 未携带 productKey，暂时无法推荐产品。");
            }
        } else {
            suggestion.setRecommendedProductId(product.getId());
            suggestion.setRecommendedProductKey(product.getProductKey());
            suggestion.setRecommendedProductName(product.getProductName());
            PublishedProductContractSnapshot snapshot = publishedProductContractSnapshotService == null
                    ? PublishedProductContractSnapshot.empty(product.getId())
                    : publishedProductContractSnapshotService.getRequiredSnapshot(product.getId());
            if (snapshot == null || snapshot.releaseBatchId() == null) {
                ruleGaps.add("推荐产品尚未形成正式合同发布，转正前需先发布契约字段。");
            }
        }

        IotProperties.Protocol.FamilyDefinition family = resolveFamily(candidate);
        if (family == null) {
            ruleGaps.add("当前 trace 尚未命中已发布协议族，请先在协议治理台确认 family。");
        } else {
            suggestion.setRecommendedFamilyCode(normalizeText(family.getFamilyCode()));
            suggestion.setRecommendedFamilyName(normalizeText(family.getDisplayName()));
            suggestion.setRecommendedDecryptProfileCode(normalizeText(family.getDecryptProfileCode()));
            if (!StringUtils.hasText(suggestion.getRecommendedDecryptProfileCode())) {
                ruleGaps.add("推荐协议族缺少解密档案配置。");
            }
        }

        ProtocolTemplateDefinitionSnapshot template = resolveTemplate(suggestion.getRecommendedFamilyCode(), candidate.getProtocolCode());
        if (template == null) {
            ruleGaps.add("当前协议族尚未命中已发布协议模板，请先完成模板发布。");
        } else {
            suggestion.setRecommendedTemplateCode(normalizeText(template.getTemplateCode()));
            suggestion.setRecommendedTemplateName(resolveTemplateName(template));
        }

        if (!StringUtils.hasText(candidate.getLastPayload())) {
            ruleGaps.add("当前 trace 缺少样本 payload，建议补采样本后再确认接入建议。");
        }

        suggestion.setRuleGaps(List.copyOf(ruleGaps));
        suggestion.setSuggestionStatus(ruleGaps.isEmpty() ? STATUS_READY : STATUS_PARTIAL);
        return suggestion;
    }

    private Product resolveProduct(Long tenantId, String productKey) {
        String normalizedProductKey = normalizeText(productKey);
        if (!StringUtils.hasText(normalizedProductKey) || productMapper == null) {
            return null;
        }
        return productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getDeleted, 0)
                .eq(Product::getProductKey, normalizedProductKey)
                .eq(tenantId != null, Product::getTenantId, tenantId)
                .last("limit 1"));
    }

    private IotProperties.Protocol.FamilyDefinition resolveFamily(DevicePageVO candidate) {
        if (candidate == null || protocolSecurityDefinitionProvider == null) {
            return null;
        }
        String protocolCode = normalizeText(candidate.getProtocolCode());
        if (!StringUtils.hasText(protocolCode)) {
            return null;
        }
        Map<String, IotProperties.Protocol.FamilyDefinition> definitions =
                protocolSecurityDefinitionProvider.listFamilyDefinitions();
        if (definitions == null || definitions.isEmpty()) {
            return null;
        }
        return definitions.values().stream()
                .filter(Objects::nonNull)
                .filter(definition -> protocolCode.equalsIgnoreCase(normalizeText(definition.getProtocolCode())))
                .sorted(Comparator
                        .comparingInt((IotProperties.Protocol.FamilyDefinition definition) -> familyPriority(definition, candidate))
                        .reversed()
                        .thenComparing(definition -> normalizeSortable(definition.getFamilyCode())))
                .findFirst()
                .orElse(null);
    }

    private int familyPriority(IotProperties.Protocol.FamilyDefinition definition, DevicePageVO candidate) {
        int score = 0;
        String familyCode = normalizeText(definition == null ? null : definition.getFamilyCode());
        String topic = normalizeText(candidate == null ? null : candidate.getLastReportTopic());
        if (StringUtils.hasText(topic) && topic.contains("$dp") && StringUtils.hasText(familyCode) && familyCode.contains("legacy-dp")) {
            score += 20;
        }
        if (StringUtils.hasText(familyCode) && familyCode.equalsIgnoreCase(normalizeText(candidate == null ? null : candidate.getProtocolCode()))) {
            score += 5;
        }
        return score;
    }

    private ProtocolTemplateDefinitionSnapshot resolveTemplate(String familyCode, String protocolCode) {
        String normalizedFamilyCode = normalizeText(familyCode);
        String normalizedProtocolCode = normalizeText(protocolCode);
        if (!StringUtils.hasText(normalizedFamilyCode) || protocolTemplateDefinitionSnapshotMapper == null) {
            return null;
        }
        List<ProtocolTemplateDefinitionSnapshot> snapshots = protocolTemplateDefinitionSnapshotMapper.selectList(
                new LambdaQueryWrapper<ProtocolTemplateDefinitionSnapshot>()
                        .eq(ProtocolTemplateDefinitionSnapshot::getDeleted, 0)
                        .eq(ProtocolTemplateDefinitionSnapshot::getLifecycleStatus, "PUBLISHED")
                        .eq(ProtocolTemplateDefinitionSnapshot::getFamilyCode, normalizedFamilyCode)
                        .eq(StringUtils.hasText(normalizedProtocolCode), ProtocolTemplateDefinitionSnapshot::getProtocolCode, normalizedProtocolCode)
                        .orderByDesc(ProtocolTemplateDefinitionSnapshot::getPublishedVersionNo)
                        .orderByDesc(ProtocolTemplateDefinitionSnapshot::getId)
        );
        if (snapshots == null || snapshots.isEmpty()) {
            return null;
        }
        return snapshots.stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String resolveTemplateName(ProtocolTemplateDefinitionSnapshot snapshot) {
        String templateCode = normalizeText(snapshot == null ? null : snapshot.getTemplateCode());
        return StringUtils.hasText(templateCode) ? templateCode : null;
    }

    private String normalizeSortable(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? "~" : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
