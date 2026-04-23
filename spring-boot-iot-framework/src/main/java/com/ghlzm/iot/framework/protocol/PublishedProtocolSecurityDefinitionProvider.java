package com.ghlzm.iot.framework.protocol;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.protocol.entity.ProtocolDecryptProfileRecord;
import com.ghlzm.iot.framework.protocol.entity.ProtocolDecryptProfileSnapshot;
import com.ghlzm.iot.framework.protocol.entity.ProtocolFamilyDefinitionRecord;
import com.ghlzm.iot.framework.protocol.entity.ProtocolFamilyDefinitionSnapshot;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolDecryptProfileRecordMapper;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolDecryptProfileSnapshotMapper;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolFamilyDefinitionRecordMapper;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolFamilyDefinitionSnapshotMapper;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Component("publishedProtocolSecurityDefinitionProvider")
public class PublishedProtocolSecurityDefinitionProvider implements ProtocolSecurityDefinitionProvider {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private final ProtocolFamilyDefinitionRecordMapper familyRecordMapper;
    private final ProtocolFamilyDefinitionSnapshotMapper familySnapshotMapper;
    private final ProtocolDecryptProfileRecordMapper decryptProfileRecordMapper;
    private final ProtocolDecryptProfileSnapshotMapper decryptProfileSnapshotMapper;

    public PublishedProtocolSecurityDefinitionProvider(ProtocolFamilyDefinitionRecordMapper familyRecordMapper,
                                                       ProtocolFamilyDefinitionSnapshotMapper familySnapshotMapper,
                                                       ProtocolDecryptProfileRecordMapper decryptProfileRecordMapper,
                                                       ProtocolDecryptProfileSnapshotMapper decryptProfileSnapshotMapper) {
        this.familyRecordMapper = familyRecordMapper;
        this.familySnapshotMapper = familySnapshotMapper;
        this.decryptProfileRecordMapper = decryptProfileRecordMapper;
        this.decryptProfileSnapshotMapper = decryptProfileSnapshotMapper;
    }

    @Override
    public IotProperties.Protocol.FamilyDefinition getFamilyDefinition(String familyCode) {
        String normalizedFamilyCode = normalizeText(familyCode);
        if (!StringUtils.hasText(normalizedFamilyCode)) {
            return null;
        }
        ProtocolFamilyDefinitionRecord record = familyRecordMapper.selectLatestByFamilyCode(normalizedFamilyCode);
        if (record == null || record.getId() == null) {
            return null;
        }
        ProtocolFamilyDefinitionSnapshot snapshot =
                familySnapshotMapper.selectLatestPublishedByFamilyId(record.getId());
        if (snapshot == null || !StringUtils.hasText(snapshot.getSnapshotJson())) {
            return null;
        }
        return toFamilyDefinition(snapshot, record);
    }

    @Override
    public IotProperties.Protocol.DecryptProfile getDecryptProfile(String profileCode) {
        String normalizedProfileCode = normalizeText(profileCode);
        if (!StringUtils.hasText(normalizedProfileCode)) {
            return null;
        }
        ProtocolDecryptProfileRecord record = decryptProfileRecordMapper.selectLatestByProfileCode(normalizedProfileCode);
        if (record == null || record.getId() == null) {
            return null;
        }
        ProtocolDecryptProfileSnapshot snapshot =
                decryptProfileSnapshotMapper.selectLatestPublishedByProfileId(record.getId());
        if (snapshot == null || !StringUtils.hasText(snapshot.getSnapshotJson())) {
            return null;
        }
        return toDecryptProfile(snapshot, record);
    }

    @Override
    public Map<String, IotProperties.Protocol.FamilyDefinition> listFamilyDefinitions() {
        List<ProtocolFamilyDefinitionSnapshot> snapshots = familySnapshotMapper.selectList(
                new LambdaQueryWrapper<ProtocolFamilyDefinitionSnapshot>()
                        .eq(ProtocolFamilyDefinitionSnapshot::getDeleted, 0)
                        .eq(ProtocolFamilyDefinitionSnapshot::getLifecycleStatus, "PUBLISHED")
                        .orderByDesc(ProtocolFamilyDefinitionSnapshot::getPublishedVersionNo)
                        .orderByDesc(ProtocolFamilyDefinitionSnapshot::getId)
        );
        if (snapshots == null || snapshots.isEmpty()) {
            return Map.of();
        }
        Map<Long, ProtocolFamilyDefinitionSnapshot> latestSnapshots =
                latestByKey(snapshots, ProtocolFamilyDefinitionSnapshot::getFamilyId);
        Map<Long, ProtocolFamilyDefinitionRecord> records = loadFamilyRecords(latestSnapshots.keySet());
        LinkedHashMap<String, IotProperties.Protocol.FamilyDefinition> definitions = new LinkedHashMap<>();
        latestSnapshots.values().stream()
                .map(snapshot -> toFamilyDefinition(snapshot, records.get(snapshot.getFamilyId())))
                .filter(Objects::nonNull)
                .forEach(definition -> definitions.putIfAbsent(definition.getFamilyCode(), definition));
        return Map.copyOf(definitions);
    }

    @Override
    public Map<String, IotProperties.Protocol.DecryptProfile> listDecryptProfiles() {
        List<ProtocolDecryptProfileSnapshot> snapshots = decryptProfileSnapshotMapper.selectList(
                new LambdaQueryWrapper<ProtocolDecryptProfileSnapshot>()
                        .eq(ProtocolDecryptProfileSnapshot::getDeleted, 0)
                        .eq(ProtocolDecryptProfileSnapshot::getLifecycleStatus, "PUBLISHED")
                        .orderByDesc(ProtocolDecryptProfileSnapshot::getPublishedVersionNo)
                        .orderByDesc(ProtocolDecryptProfileSnapshot::getId)
        );
        if (snapshots == null || snapshots.isEmpty()) {
            return Map.of();
        }
        Map<Long, ProtocolDecryptProfileSnapshot> latestSnapshots =
                latestByKey(snapshots, ProtocolDecryptProfileSnapshot::getProfileId);
        Map<Long, ProtocolDecryptProfileRecord> records = loadDecryptRecords(latestSnapshots.keySet());
        LinkedHashMap<String, IotProperties.Protocol.DecryptProfile> profiles = new LinkedHashMap<>();
        latestSnapshots.values().stream()
                .map(snapshot -> toDecryptProfile(snapshot, records.get(snapshot.getProfileId())))
                .filter(Objects::nonNull)
                .forEach(profile -> profiles.putIfAbsent(profile.getProfileCode(), profile));
        return Map.copyOf(profiles);
    }

    private Map<Long, ProtocolFamilyDefinitionRecord> loadFamilyRecords(Collection<Long> familyIds) {
        if (familyIds == null || familyIds.isEmpty()) {
            return Map.of();
        }
        return familyRecordMapper.selectBatchIds(familyIds).stream()
                .filter(Objects::nonNull)
                .filter(record -> record.getId() != null)
                .collect(Collectors.toMap(ProtocolFamilyDefinitionRecord::getId, Function.identity()));
    }

    private Map<Long, ProtocolDecryptProfileRecord> loadDecryptRecords(Collection<Long> profileIds) {
        if (profileIds == null || profileIds.isEmpty()) {
            return Map.of();
        }
        return decryptProfileRecordMapper.selectBatchIds(profileIds).stream()
                .filter(Objects::nonNull)
                .filter(record -> record.getId() != null)
                .collect(Collectors.toMap(ProtocolDecryptProfileRecord::getId, Function.identity()));
    }

    private IotProperties.Protocol.FamilyDefinition toFamilyDefinition(ProtocolFamilyDefinitionSnapshot snapshot,
                                                                       ProtocolFamilyDefinitionRecord record) {
        try {
            FamilySnapshotPayload payload = OBJECT_MAPPER.readValue(snapshot.getSnapshotJson(), FamilySnapshotPayload.class);
            IotProperties.Protocol.FamilyDefinition definition = new IotProperties.Protocol.FamilyDefinition();
            definition.setFamilyCode(normalizeText(payload.familyCode()));
            definition.setProtocolCode(normalizeText(payload.protocolCode()));
            definition.setDisplayName(normalizeText(payload.displayName()));
            definition.setDecryptProfileCode(normalizeText(payload.decryptProfileCode()));
            definition.setSignAlgorithm(normalizeText(payload.signAlgorithm()));
            definition.setNormalizationStrategy(normalizeText(payload.normalizationStrategy()));
            definition.setEnabled(Boolean.TRUE);
            if (record != null && Objects.equals(record.getVersionNo(), snapshot.getPublishedVersionNo())) {
                if (!StringUtils.hasText(definition.getDisplayName())) {
                    definition.setDisplayName(normalizeText(record.getDisplayName()));
                }
                if (!StringUtils.hasText(definition.getSignAlgorithm())) {
                    definition.setSignAlgorithm(normalizeText(record.getSignAlgorithm()));
                }
                if (!StringUtils.hasText(definition.getNormalizationStrategy())) {
                    definition.setNormalizationStrategy(normalizeText(record.getNormalizationStrategy()));
                }
            }
            if (!StringUtils.hasText(definition.getFamilyCode())) {
                definition.setFamilyCode(normalizeText(record == null ? null : record.getFamilyCode()));
            }
            if (!StringUtils.hasText(definition.getProtocolCode())) {
                definition.setProtocolCode(normalizeText(record == null ? null : record.getProtocolCode()));
            }
            if (!StringUtils.hasText(definition.getDecryptProfileCode())) {
                definition.setDecryptProfileCode(normalizeText(record == null ? null : record.getDecryptProfileCode()));
            }
            return StringUtils.hasText(definition.getFamilyCode()) ? definition : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private IotProperties.Protocol.DecryptProfile toDecryptProfile(ProtocolDecryptProfileSnapshot snapshot,
                                                                   ProtocolDecryptProfileRecord record) {
        try {
            DecryptSnapshotPayload payload = OBJECT_MAPPER.readValue(snapshot.getSnapshotJson(), DecryptSnapshotPayload.class);
            IotProperties.Protocol.DecryptProfile profile = new IotProperties.Protocol.DecryptProfile();
            profile.setProfileCode(normalizeText(payload.profileCode()));
            profile.setAlgorithm(normalizeText(payload.algorithm()));
            profile.setMerchantSource(normalizeText(payload.merchantSource()));
            profile.setMerchantKey(normalizeText(payload.merchantKey()));
            profile.setTransformation(normalizeText(payload.transformation()));
            profile.setSignatureSecret(normalizeText(payload.signatureSecret()));
            profile.setEnabled(Boolean.TRUE);
            if (record != null && Objects.equals(record.getVersionNo(), snapshot.getPublishedVersionNo())) {
                if (!StringUtils.hasText(profile.getTransformation())) {
                    profile.setTransformation(normalizeText(record.getTransformation()));
                }
                if (!StringUtils.hasText(profile.getSignatureSecret())) {
                    profile.setSignatureSecret(normalizeText(record.getSignatureSecret()));
                }
            }
            if (!StringUtils.hasText(profile.getProfileCode())) {
                profile.setProfileCode(normalizeText(record == null ? null : record.getProfileCode()));
            }
            if (!StringUtils.hasText(profile.getAlgorithm())) {
                profile.setAlgorithm(normalizeText(record == null ? null : record.getAlgorithm()));
            }
            if (!StringUtils.hasText(profile.getMerchantSource())) {
                profile.setMerchantSource(normalizeText(record == null ? null : record.getMerchantSource()));
            }
            if (!StringUtils.hasText(profile.getMerchantKey())) {
                profile.setMerchantKey(normalizeText(record == null ? null : record.getMerchantKey()));
            }
            return StringUtils.hasText(profile.getProfileCode()) ? profile : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private <K, V> Map<K, V> latestByKey(List<V> values, Function<V, K> keyExtractor) {
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

    @SuppressWarnings("unchecked")
    private <T> T preferLatestSnapshot(T left, T right) {
        Comparator<T> comparator = Comparator
                .comparing((T value) -> publishedVersion(value), Comparator.nullsFirst(Integer::compareTo))
                .thenComparing(value -> snapshotId(value), Comparator.nullsFirst(Long::compareTo));
        return comparator.compare(left, right) >= 0 ? left : right;
    }

    private Integer publishedVersion(Object snapshot) {
        if (snapshot instanceof ProtocolFamilyDefinitionSnapshot familySnapshot) {
            return familySnapshot.getPublishedVersionNo();
        }
        if (snapshot instanceof ProtocolDecryptProfileSnapshot decryptSnapshot) {
            return decryptSnapshot.getPublishedVersionNo();
        }
        return null;
    }

    private Long snapshotId(Object snapshot) {
        if (snapshot instanceof ProtocolFamilyDefinitionSnapshot familySnapshot) {
            return familySnapshot.getId();
        }
        if (snapshot instanceof ProtocolDecryptProfileSnapshot decryptSnapshot) {
            return decryptSnapshot.getId();
        }
        return null;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record FamilySnapshotPayload(Long familyId,
                                         String familyCode,
                                         String protocolCode,
                                         String displayName,
                                         String decryptProfileCode,
                                         String signAlgorithm,
                                         String normalizationStrategy,
                                         Integer expectedVersionNo,
                                         String submitReason,
                                         Object execution) {
    }

    private record DecryptSnapshotPayload(Long profileId,
                                          String profileCode,
                                          String algorithm,
                                          String merchantSource,
                                          String merchantKey,
                                          String transformation,
                                          String signatureSecret,
                                          Integer expectedVersionNo,
                                          String submitReason,
                                          Object execution) {
    }
}
