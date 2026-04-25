package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.NormativeMetricDefinitionImportDTO;
import com.ghlzm.iot.device.dto.NormativeMetricDefinitionImportItemDTO;
import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.mapper.NormativeMetricDefinitionMapper;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import com.ghlzm.iot.device.vo.NormativeMetricDefinitionImportResultVO;
import com.ghlzm.iot.device.vo.NormativeMetricDefinitionImportRowVO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 规范字段定义服务实现。
 */
@Service
public class NormativeMetricDefinitionServiceImpl implements NormativeMetricDefinitionService {

    private final NormativeMetricDefinitionMapper mapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public NormativeMetricDefinitionServiceImpl(NormativeMetricDefinitionMapper mapper) {
        this(mapper, new ObjectMapper());
    }

    public NormativeMetricDefinitionServiceImpl(NormativeMetricDefinitionMapper mapper,
                                                ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    @Override
    public List<NormativeMetricDefinition> listByScenario(String scenarioCode) {
        if (!StringUtils.hasText(scenarioCode)) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<NormativeMetricDefinition>()
                .eq(NormativeMetricDefinition::getDeleted, 0)
                .eq(NormativeMetricDefinition::getScenarioCode, scenarioCode)
                .orderByAsc(NormativeMetricDefinition::getIdentifier));
    }

    @Override
    public List<NormativeMetricDefinition> listActive() {
        return mapper.selectList(new LambdaQueryWrapper<NormativeMetricDefinition>()
                .eq(NormativeMetricDefinition::getDeleted, 0)
                .orderByAsc(NormativeMetricDefinition::getScenarioCode)
                .orderByAsc(NormativeMetricDefinition::getIdentifier));
    }

    @Override
    public NormativeMetricDefinitionImportResultVO previewImport(NormativeMetricDefinitionImportDTO dto) {
        return buildImportPreview(dto, false);
    }

    @Override
    public NormativeMetricDefinitionImportResultVO applyImport(NormativeMetricDefinitionImportDTO dto) {
        NormativeMetricDefinitionImportResultVO preview = buildImportPreview(dto, false);
        if (preview.getConflictCount() != null && preview.getConflictCount() > 0) {
            throw new BizException("规范字段导入存在冲突，请先预检并修正");
        }
        List<NormalizedImportItem> normalizedItems = normalizeItems(dto);
        Map<Long, NormativeMetricDefinition> existingById = existingDefinitionsById(loadAllDefinitions());
        Map<Integer, NormativeMetricDefinitionImportRowVO> appliedRows = new HashMap<>();
        int appliedCount = 0;
        for (NormalizedImportItem item : normalizedItems) {
            NormativeMetricDefinitionImportRowVO previewRow = preview.getRows().stream()
                    .filter(row -> Objects.equals(row.getRowIndex(), item.rowIndex()))
                    .findFirst()
                    .orElse(null);
            if (previewRow == null || !"READY".equals(previewRow.getStatus())) {
                continue;
            }
            NormativeMetricDefinition entity = toEntity(item);
            NormativeMetricDefinition existing = item.id() == null ? null : existingById.get(item.id());
            if (existing == null) {
                mapper.insert(entity);
                previewRow.setStatus("APPLIED_CREATE");
                previewRow.setMessage("已创建规范字段");
            } else {
                mapper.updateById(entity);
                previewRow.setStatus("APPLIED_UPDATE");
                previewRow.setMessage("已更新规范字段");
            }
            appliedRows.put(item.rowIndex(), previewRow);
            appliedCount++;
        }
        preview.setAppliedCount(appliedCount);
        preview.setReadyCount(0);
        preview.setRows(preview.getRows().stream()
                .map(row -> appliedRows.getOrDefault(row.getRowIndex(), row))
                .toList());
        return preview;
    }

    private NormativeMetricDefinitionImportResultVO buildImportPreview(NormativeMetricDefinitionImportDTO dto,
                                                                       boolean includeAppliedCount) {
        List<NormalizedImportItem> items = normalizeItems(dto);
        List<NormativeMetricDefinition> existingDefinitions = loadAllDefinitions();
        Map<Long, NormativeMetricDefinition> existingById = existingDefinitionsById(existingDefinitions);
        Map<Long, List<NormalizedImportItem>> importById = new LinkedHashMap<>();
        Map<String, List<NormalizedImportItem>> importByFallbackKey = new LinkedHashMap<>();
        for (NormalizedImportItem item : items) {
            if (item.id() != null) {
                importById.computeIfAbsent(item.id(), ignored -> new ArrayList<>()).add(item);
            }
            if (item.active() && StringUtils.hasText(item.fallbackKey())) {
                importByFallbackKey.computeIfAbsent(item.fallbackKey(), ignored -> new ArrayList<>()).add(item);
            }
        }

        List<NormativeMetricDefinitionImportRowVO> rows = new ArrayList<>();
        int conflictCount = 0;
        int readyCount = 0;
        for (NormalizedImportItem item : items) {
            NormativeMetricDefinitionImportRowVO row = baseRow(item, existingById.containsKey(item.id()) ? "UPDATE" : "CREATE");
            List<String> errors = new ArrayList<>(item.errors());
            if (item.id() != null && importById.getOrDefault(item.id(), List.of()).size() > 1) {
                errors.add("导入数据中存在重复 id: " + item.id());
            }
            if (item.active() && StringUtils.hasText(item.fallbackKey())
                    && importByFallbackKey.getOrDefault(item.fallbackKey(), List.of()).size() > 1) {
                errors.add("导入数据中存在重复兜底键: " + item.fallbackKey());
            }
            if (item.active() && StringUtils.hasText(item.fallbackKey())
                    && hasExistingFallbackConflict(item, existingDefinitions)) {
                errors.add("与现有 active 规范字段兜底键冲突: " + item.fallbackKey());
            }

            if (errors.isEmpty()) {
                row.setStatus("READY");
                row.setMessage("可导入");
                readyCount++;
            } else {
                row.setStatus(resolveConflictStatus(errors));
                row.setMessage(String.join("；", errors));
                conflictCount++;
            }
            rows.add(row);
        }

        NormativeMetricDefinitionImportResultVO result = new NormativeMetricDefinitionImportResultVO();
        result.setTotalCount(items.size());
        result.setReadyCount(readyCount);
        result.setConflictCount(conflictCount);
        result.setAppliedCount(includeAppliedCount ? 0 : null);
        result.setRows(rows);
        return result;
    }

    private String resolveConflictStatus(List<String> errors) {
        String joined = String.join(" ", errors);
        if (joined.contains("重复 id")) {
            return "CONFLICT_DUPLICATE_ID";
        }
        if (joined.contains("兜底键")) {
            return "CONFLICT_DUPLICATE_FALLBACK_KEY";
        }
        return "INVALID_REQUIRED";
    }

    private List<NormalizedImportItem> normalizeItems(NormativeMetricDefinitionImportDTO dto) {
        List<NormativeMetricDefinitionImportItemDTO> sourceItems = dto == null || dto.getItems() == null
                ? List.of()
                : dto.getItems();
        List<NormalizedImportItem> items = new ArrayList<>();
        for (int i = 0; i < sourceItems.size(); i++) {
            items.add(normalizeItem(i + 1, sourceItems.get(i)));
        }
        return items;
    }

    private NormalizedImportItem normalizeItem(int rowIndex, NormativeMetricDefinitionImportItemDTO item) {
        NormativeMetricDefinitionImportItemDTO safeItem = item == null
                ? new NormativeMetricDefinitionImportItemDTO()
                : item;
        String scenarioCode = normalizeText(safeItem.getScenarioCode());
        String deviceFamily = normalizeUpper(safeItem.getDeviceFamily());
        String identifier = normalizeText(safeItem.getIdentifier());
        String displayName = normalizeText(safeItem.getDisplayName());
        String monitorContentCode = normalizeUpper(safeItem.getMonitorContentCode());
        String monitorTypeCode = normalizeUpper(safeItem.getMonitorTypeCode());
        String status = StringUtils.hasText(safeItem.getStatus())
                ? normalizeUpper(safeItem.getStatus())
                : "ACTIVE";
        List<String> errors = new ArrayList<>();
        requireText(errors, scenarioCode, "scenarioCode");
        requireText(errors, deviceFamily, "deviceFamily");
        requireText(errors, identifier, "identifier");
        requireText(errors, displayName, "displayName");
        requireText(errors, monitorContentCode, "monitorContentCode");
        requireText(errors, monitorTypeCode, "monitorTypeCode");

        return new NormalizedImportItem(
                rowIndex,
                safeItem.getId(),
                scenarioCode,
                deviceFamily,
                identifier,
                displayName,
                normalizeText(safeItem.getUnit()),
                defaultInt(safeItem.getPrecisionDigits(), 0),
                monitorContentCode,
                monitorTypeCode,
                defaultInt(safeItem.getRiskEnabled(), 0),
                defaultInt(safeItem.getTrendEnabled(), 1),
                StringUtils.hasText(safeItem.getMetricDimension())
                        ? normalizeText(safeItem.getMetricDimension())
                        : "measure",
                StringUtils.hasText(safeItem.getThresholdType())
                        ? normalizeText(safeItem.getThresholdType())
                        : "absolute",
                StringUtils.hasText(safeItem.getSemanticDirection())
                        ? normalizeUpper(safeItem.getSemanticDirection())
                        : "REFERENCE_ONLY",
                defaultInt(safeItem.getGisEnabled(), 0),
                defaultInt(safeItem.getInsightEnabled(), 1),
                defaultInt(safeItem.getAnalyticsEnabled(), 1),
                status,
                defaultInt(safeItem.getVersionNo(), 1),
                toMetadataJson(safeItem.getMetadataJson()),
                fallbackKey(monitorContentCode, monitorTypeCode, identifier),
                errors
        );
    }

    private NormativeMetricDefinitionImportRowVO baseRow(NormalizedImportItem item, String action) {
        NormativeMetricDefinitionImportRowVO row = new NormativeMetricDefinitionImportRowVO();
        row.setRowIndex(item.rowIndex());
        row.setId(item.id());
        row.setScenarioCode(item.scenarioCode());
        row.setDeviceFamily(item.deviceFamily());
        row.setIdentifier(item.identifier());
        row.setDisplayName(item.displayName());
        row.setMonitorContentCode(item.monitorContentCode());
        row.setMonitorTypeCode(item.monitorTypeCode());
        row.setFallbackKey(item.fallbackKey());
        row.setAction(action);
        return row;
    }

    private boolean hasExistingFallbackConflict(NormalizedImportItem item,
                                                List<NormativeMetricDefinition> existingDefinitions) {
        for (NormativeMetricDefinition definition : existingDefinitions) {
            if (definition == null || !isActive(definition.getStatus())) {
                continue;
            }
            if (item.id() != null && item.id().equals(definition.getId())) {
                continue;
            }
            String existingKey = fallbackKey(
                    definition.getMonitorContentCode(),
                    definition.getMonitorTypeCode(),
                    definition.getIdentifier()
            );
            if (item.fallbackKey().equals(existingKey)) {
                return true;
            }
        }
        return false;
    }

    private List<NormativeMetricDefinition> loadAllDefinitions() {
        List<NormativeMetricDefinition> definitions = mapper.selectList(new LambdaQueryWrapper<NormativeMetricDefinition>()
                .eq(NormativeMetricDefinition::getDeleted, 0));
        return definitions == null ? List.of() : definitions;
    }

    private Map<Long, NormativeMetricDefinition> existingDefinitionsById(List<NormativeMetricDefinition> definitions) {
        Map<Long, NormativeMetricDefinition> result = new HashMap<>();
        for (NormativeMetricDefinition definition : definitions) {
            if (definition != null && definition.getId() != null) {
                result.put(definition.getId(), definition);
            }
        }
        return result;
    }

    private NormativeMetricDefinition toEntity(NormalizedImportItem item) {
        NormativeMetricDefinition definition = new NormativeMetricDefinition();
        definition.setId(item.id());
        definition.setScenarioCode(item.scenarioCode());
        definition.setDeviceFamily(item.deviceFamily());
        definition.setIdentifier(item.identifier());
        definition.setDisplayName(item.displayName());
        definition.setUnit(item.unit());
        definition.setPrecisionDigits(item.precisionDigits());
        definition.setMonitorContentCode(item.monitorContentCode());
        definition.setMonitorTypeCode(item.monitorTypeCode());
        definition.setRiskEnabled(item.riskEnabled());
        definition.setTrendEnabled(item.trendEnabled());
        definition.setMetricDimension(item.metricDimension());
        definition.setThresholdType(item.thresholdType());
        definition.setSemanticDirection(item.semanticDirection());
        definition.setGisEnabled(item.gisEnabled());
        definition.setInsightEnabled(item.insightEnabled());
        definition.setAnalyticsEnabled(item.analyticsEnabled());
        definition.setStatus(item.status());
        definition.setVersionNo(item.versionNo());
        definition.setMetadataJson(item.metadataJson());
        definition.setDeleted(0);
        return definition;
    }

    private String toMetadataJson(Object metadataJson) {
        if (metadataJson == null) {
            return "{}";
        }
        if (metadataJson instanceof String text) {
            return StringUtils.hasText(text) ? text.trim() : "{}";
        }
        try {
            return objectMapper.writeValueAsString(metadataJson);
        } catch (JsonProcessingException ex) {
            throw new BizException("metadataJson 不是有效 JSON 对象");
        }
    }

    private void requireText(List<String> errors, String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            errors.add("缺少必填字段 " + fieldName);
        }
    }

    private String fallbackKey(String monitorContentCode, String monitorTypeCode, String identifier) {
        String content = normalizeUpper(monitorContentCode);
        String type = normalizeUpper(monitorTypeCode);
        String normalizedIdentifier = normalizeLower(identifier);
        if (!StringUtils.hasText(content) || !StringUtils.hasText(type) || !StringUtils.hasText(normalizedIdentifier)) {
            return null;
        }
        return content + "/" + type + "/" + normalizedIdentifier;
    }

    private boolean isActive(String status) {
        return !StringUtils.hasText(status) || "ACTIVE".equals(normalizeUpper(status));
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeLower(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private record NormalizedImportItem(
            int rowIndex,
            Long id,
            String scenarioCode,
            String deviceFamily,
            String identifier,
            String displayName,
            String unit,
            Integer precisionDigits,
            String monitorContentCode,
            String monitorTypeCode,
            Integer riskEnabled,
            Integer trendEnabled,
            String metricDimension,
            String thresholdType,
            String semanticDirection,
            Integer gisEnabled,
            Integer insightEnabled,
            Integer analyticsEnabled,
            String status,
            Integer versionNo,
            String metadataJson,
            String fallbackKey,
            List<String> errors
    ) {
        boolean active() {
            return "ACTIVE".equals(status);
        }
    }
}
