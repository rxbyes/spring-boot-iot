package com.ghlzm.iot.framework.protocol.template;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.framework.protocol.template.entity.ProtocolTemplateDefinitionRecord;
import com.ghlzm.iot.framework.protocol.template.entity.ProtocolTemplateDefinitionSnapshot;
import com.ghlzm.iot.framework.protocol.template.mapper.ProtocolTemplateDefinitionRecordMapper;
import com.ghlzm.iot.framework.protocol.template.mapper.ProtocolTemplateDefinitionSnapshotMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component("publishedProtocolTemplateDefinitionProvider")
public class PublishedProtocolTemplateDefinitionProvider implements ProtocolTemplateDefinitionProvider {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private final ProtocolTemplateDefinitionRecordMapper recordMapper;
    private final ProtocolTemplateDefinitionSnapshotMapper snapshotMapper;

    public PublishedProtocolTemplateDefinitionProvider(ProtocolTemplateDefinitionRecordMapper recordMapper,
                                                       ProtocolTemplateDefinitionSnapshotMapper snapshotMapper) {
        this.recordMapper = recordMapper;
        this.snapshotMapper = snapshotMapper;
    }

    @Override
    public List<ProtocolTemplateRuntimeDefinition> listPublishedDefinitions() {
        List<ProtocolTemplateDefinitionSnapshot> snapshots = snapshotMapper.selectList(
                new LambdaQueryWrapper<ProtocolTemplateDefinitionSnapshot>()
                        .eq(ProtocolTemplateDefinitionSnapshot::getDeleted, 0)
                        .eq(ProtocolTemplateDefinitionSnapshot::getLifecycleStatus, "PUBLISHED")
                        .orderByDesc(ProtocolTemplateDefinitionSnapshot::getPublishedVersionNo)
                        .orderByDesc(ProtocolTemplateDefinitionSnapshot::getId)
        );
        if (snapshots == null || snapshots.isEmpty()) {
            return List.of();
        }
        Map<Long, ProtocolTemplateDefinitionSnapshot> latestSnapshots =
                latestByKey(snapshots, ProtocolTemplateDefinitionSnapshot::getTemplateId);
        Map<Long, ProtocolTemplateDefinitionRecord> records = loadRecords(latestSnapshots.keySet());
        return latestSnapshots.values().stream()
                .map(snapshot -> toRuntimeDefinition(snapshot, records.get(snapshot.getTemplateId())))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ProtocolTemplateRuntimeDefinition::templateCode, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private Map<Long, ProtocolTemplateDefinitionRecord> loadRecords(Collection<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return Map.of();
        }
        return recordMapper.selectBatchIds(templateIds).stream()
                .filter(Objects::nonNull)
                .filter(record -> record.getId() != null)
                .collect(Collectors.toMap(ProtocolTemplateDefinitionRecord::getId, Function.identity()));
    }

    private ProtocolTemplateRuntimeDefinition toRuntimeDefinition(ProtocolTemplateDefinitionSnapshot snapshot,
                                                                  ProtocolTemplateDefinitionRecord record) {
        if (snapshot == null) {
            return null;
        }
        TemplateSnapshotPayload payload = readPayload(snapshot.getSnapshotJson());
        String templateCode = firstText(payload == null ? null : payload.templateCode(),
                record == null ? null : record.getTemplateCode(),
                snapshot.getTemplateCode());
        if (!StringUtils.hasText(templateCode)) {
            return null;
        }
        return new ProtocolTemplateRuntimeDefinition(
                templateCode,
                firstText(payload == null ? null : payload.familyCode(),
                        record == null ? null : record.getFamilyCode(),
                        snapshot.getFamilyCode()),
                firstText(payload == null ? null : payload.protocolCode(),
                        record == null ? null : record.getProtocolCode(),
                        snapshot.getProtocolCode()),
                firstText(payload == null ? null : payload.displayName(),
                        record == null ? null : record.getDisplayName()),
                firstText(payload == null ? null : payload.expressionJson(),
                        record == null ? null : record.getExpressionJson()),
                firstText(payload == null ? null : payload.outputMappingJson(),
                        record == null ? null : record.getOutputMappingJson()),
                snapshot.getPublishedVersionNo()
        );
    }

    private TemplateSnapshotPayload readPayload(String snapshotJson) {
        if (!StringUtils.hasText(snapshotJson)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(snapshotJson, TemplateSnapshotPayload.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private <K> Map<K, ProtocolTemplateDefinitionSnapshot> latestByKey(List<ProtocolTemplateDefinitionSnapshot> values,
                                                                       Function<ProtocolTemplateDefinitionSnapshot, K> keyExtractor) {
        return values.stream()
                .filter(Objects::nonNull)
                .filter(value -> keyExtractor.apply(value) != null)
                .collect(Collectors.toMap(
                        keyExtractor,
                        Function.identity(),
                        this::preferLatestSnapshot,
                        LinkedHashMap::new
                ));
    }

    private ProtocolTemplateDefinitionSnapshot preferLatestSnapshot(ProtocolTemplateDefinitionSnapshot left,
                                                                   ProtocolTemplateDefinitionSnapshot right) {
        Comparator<ProtocolTemplateDefinitionSnapshot> comparator = Comparator
                .comparing(ProtocolTemplateDefinitionSnapshot::getPublishedVersionNo, Comparator.nullsFirst(Integer::compareTo))
                .thenComparing(ProtocolTemplateDefinitionSnapshot::getId, Comparator.nullsFirst(Long::compareTo));
        return comparator.compare(left, right) >= 0 ? left : right;
    }

    private String firstText(String... values) {
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

    private record TemplateSnapshotPayload(Long templateId,
                                           String templateCode,
                                           String familyCode,
                                           String protocolCode,
                                           String displayName,
                                           String expressionJson,
                                           String outputMappingJson,
                                           String status) {
    }
}
