package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import com.ghlzm.iot.alarm.vo.RiskPointPendingMetricCandidateVO;
import com.ghlzm.iot.device.entity.Device;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 风险点待治理测点治理规则。
 */
final class RiskPointPendingMetricGovernanceRules {

    private static final Set<String> STATUS_FIELDS = Set.of(
            "ext_power_volt",
            "solar_volt",
            "battery_dump_energy",
            "battery_volt",
            "battery_voltage",
            "temp",
            "temp_out",
            "humidity",
            "humidity_out",
            "lon",
            "lat",
            "signal_4g",
            "signal_nb",
            "signal_db",
            "singal_nb",
            "singal_db",
            "sw_version",
            "sensor_state",
            "pa_state",
            "sound_state"
    );
    private static final Set<String> NON_AMBIGUOUS_CANONICAL_METRICS = Set.of(
            "dispsX",
            "dispsY",
            "gpsInitial",
            "gpsTotalX",
            "gpsTotalY",
            "gpsTotalZ",
            "gX",
            "gY",
            "gZ",
            "angle",
            "AZI",
            "PLX",
            "PLY",
            "PLZ",
            "SJX",
            "SJY",
            "SJZ",
            "SJValue",
            "OSP",
            "VSP",
            "freq",
            "wave",
            "amplitude",
            "energy",
            "ringing",
            "risetime",
            "risecount",
            "duration",
            "arrivaltime",
            "RMS",
            "ASL"
    );
    private static final Set<String> AMBIGUOUS_CANONICAL_METRICS = Set.of(
            "value",
            "temp",
            "totalValue",
            "X",
            "Y",
            "Z",
            "x",
            "y",
            "z",
            "speed"
    );
    private static final List<DeviceFamilyRule> DEVICE_FAMILIES = List.of(
            new DeviceFamilyRule(
                    "DEEP_DISPLACEMENT",
                    "深部位移",
                    Set.of("dispsX", "dispsY"),
                    Set.of("dispsX", "dispsY"),
                    Set.of("深部位移", "固定测斜", "测斜", "inclinometer")
            ),
            new DeviceFamilyRule(
                    "GNSS",
                    "GNSS位移",
                    Set.of("gpsInitial", "gpsTotalX", "gpsTotalY", "gpsTotalZ"),
                    Set.of("gpsInitial", "gpsTotalX", "gpsTotalY", "gpsTotalZ"),
                    Set.of("gnss", "北斗", "卫星")
            ),
            new DeviceFamilyRule(
                    "ACCELERATION",
                    "加速度",
                    Set.of("gX", "gY", "gZ"),
                    Set.of("gX", "gY", "gZ"),
                    Set.of("加速度", "acceleration")
            ),
            new DeviceFamilyRule(
                    "TILT",
                    "倾角",
                    Set.of("angle", "AZI"),
                    Set.of("X", "Y", "Z", "angle", "AZI"),
                    Set.of("倾角", "倾斜", "tilt")
            ),
            new DeviceFamilyRule(
                    "RADAR",
                    "雷达",
                    Set.of(),
                    Set.of("X", "Y", "Z", "speed"),
                    Set.of("雷达", "radar")
            ),
            new DeviceFamilyRule(
                    "CRACK",
                    "裂缝",
                    Set.of(),
                    Set.of("value"),
                    Set.of("裂缝", "crack")
            ),
            new DeviceFamilyRule(
                    "VIBRATION",
                    "振动",
                    Set.of("PLX", "PLY", "PLZ", "SJX", "SJY", "SJZ", "SJValue"),
                    Set.of("PLX", "PLY", "PLZ", "value", "SJX", "SJY", "SJZ", "SJValue"),
                    Set.of("振动", "微震", "振弦")
            ),
            new DeviceFamilyRule(
                    "ACOUSTIC",
                    "声学",
                    Set.of("OSP", "VSP", "freq", "wave", "amplitude", "energy", "ringing", "risetime", "risecount", "duration", "arrivaltime", "RMS", "ASL"),
                    Set.of("OSP", "VSP", "freq", "wave", "amplitude", "energy", "ringing", "risetime", "risecount", "duration", "arrivaltime", "RMS", "ASL"),
                    Set.of("地声", "次声", "声发射", "声学", "acoustic")
            ),
            new DeviceFamilyRule(
                    "TEMPERATURE",
                    "温度",
                    Set.of(),
                    Set.of("temp"),
                    Set.of("水温", "温度", "soil temperature", "temperature")
            ),
            new DeviceFamilyRule(
                    "HYDROLOGY",
                    "水文/压力",
                    Set.of(),
                    Set.of("value", "totalValue", "temp", "x", "y", "z", "speed"),
                    Set.of("水位", "孔压", "渗压", "土压", "雨量", "流速", "沉降", "标靶", "位移计", "压力")
            )
    );
    private static final Map<String, String> METRIC_DISPLAY_NAMES = Map.ofEntries(
            Map.entry("dispsX", "X向位移"),
            Map.entry("dispsY", "Y向位移"),
            Map.entry("gpsInitial", "初始位移"),
            Map.entry("gpsTotalX", "X向累计位移"),
            Map.entry("gpsTotalY", "Y向累计位移"),
            Map.entry("gpsTotalZ", "Z向累计位移"),
            Map.entry("gX", "X向加速度"),
            Map.entry("gY", "Y向加速度"),
            Map.entry("gZ", "Z向加速度"),
            Map.entry("X", "X向倾角"),
            Map.entry("Y", "Y向倾角"),
            Map.entry("Z", "Z向倾角"),
            Map.entry("angle", "倾角"),
            Map.entry("AZI", "方位角"),
            Map.entry("value", "监测值"),
            Map.entry("temp", "温度"),
            Map.entry("speed", "速度"),
            Map.entry("totalValue", "累计值")
    );
    private static final Map<String, String> METRIC_DOMAIN_LABELS = Map.ofEntries(
            Map.entry("dispsX", "深部位移"),
            Map.entry("dispsY", "深部位移"),
            Map.entry("gpsInitial", "GNSS位移"),
            Map.entry("gpsTotalX", "GNSS位移"),
            Map.entry("gpsTotalY", "GNSS位移"),
            Map.entry("gpsTotalZ", "GNSS位移"),
            Map.entry("gX", "加速度"),
            Map.entry("gY", "加速度"),
            Map.entry("gZ", "加速度"),
            Map.entry("PLX", "振动"),
            Map.entry("PLY", "振动"),
            Map.entry("PLZ", "振动"),
            Map.entry("SJX", "振动"),
            Map.entry("SJY", "振动"),
            Map.entry("SJZ", "振动"),
            Map.entry("SJValue", "振动"),
            Map.entry("OSP", "声学"),
            Map.entry("VSP", "声学"),
            Map.entry("freq", "声学"),
            Map.entry("wave", "声学"),
            Map.entry("amplitude", "声学"),
            Map.entry("energy", "声学"),
            Map.entry("ringing", "声学"),
            Map.entry("risetime", "声学"),
            Map.entry("risecount", "声学"),
            Map.entry("duration", "声学"),
            Map.entry("arrivaltime", "声学"),
            Map.entry("RMS", "声学"),
            Map.entry("ASL", "声学")
    );
    private static final Map<String, String> EXACT_CANONICAL_LOOKUP = buildExactCanonicalLookup();

    List<RiskPointPendingMetricCandidateVO> governCandidates(RiskPointDevicePendingBinding pending,
                                                             Device device,
                                                             List<RiskPointPendingMetricCandidateVO> rawCandidates) {
        DeviceFamilyRule familyRule = resolveFamily(
                pending,
                device == null ? null : device.getDeviceName(),
                rawCandidates
        );
        Map<String, GovernedCandidateAccumulator> governed = new LinkedHashMap<>();
        for (RiskPointPendingMetricCandidateVO candidate : rawCandidates == null ? List.<RiskPointPendingMetricCandidateVO>of() : rawCandidates) {
            String rawIdentifier = normalize(candidate == null ? null : candidate.getMetricIdentifier());
            if (rawIdentifier == null) {
                continue;
            }
            MetricResolution resolution = resolveMetric(familyRule, rawIdentifier);
            if (!resolution.allowed()) {
                continue;
            }
            governed.computeIfAbsent(
                            resolution.canonicalIdentifier(),
                            identifier -> new GovernedCandidateAccumulator(
                                    identifier,
                                    resolveDomainLabel(familyRule, identifier)
                            )
                    )
                    .merge(candidate, rawIdentifier, resolution.aliasIdentifier());
        }
        return governed.values().stream()
                .map(GovernedCandidateAccumulator::toCandidate)
                .toList();
    }

    String normalizePromotableMetricIdentifier(RiskPointDevicePendingBinding pending,
                                               String bundleDeviceName,
                                               List<RiskPointPendingMetricCandidateVO> candidates,
                                               String rawIdentifier) {
        DeviceFamilyRule familyRule = resolveFamily(pending, bundleDeviceName, candidates);
        MetricResolution resolution = resolveMetric(familyRule, rawIdentifier);
        return resolution.allowed() ? resolution.canonicalIdentifier() : null;
    }

    private DeviceFamilyRule resolveFamily(RiskPointDevicePendingBinding pending,
                                           String deviceName,
                                           List<RiskPointPendingMetricCandidateVO> candidates) {
        List<String> descriptors = new ArrayList<>();
        descriptors.add(normalize(pending == null ? null : pending.getRiskPointName()));
        descriptors.add(normalize(pending == null ? null : pending.getDeviceName()));
        descriptors.add(normalize(deviceName));

        DeviceFamilyRule bestRule = null;
        int bestScore = 0;
        boolean tied = false;
        for (DeviceFamilyRule familyRule : DEVICE_FAMILIES) {
            int score = 0;
            for (String descriptor : descriptors) {
                if (containsKeyword(descriptor, familyRule.keywords())) {
                    score += 4;
                }
            }
            for (RiskPointPendingMetricCandidateVO candidate : candidates == null ? List.<RiskPointPendingMetricCandidateVO>of() : candidates) {
                String leafIdentifier = extractLeaf(normalize(candidate == null ? null : candidate.getMetricIdentifier()));
                if (familyRule.strongMetrics().contains(leafIdentifier)) {
                    score += 6;
                }
            }
            if (score > bestScore) {
                bestRule = familyRule;
                bestScore = score;
                tied = false;
            } else if (score > 0 && score == bestScore) {
                tied = true;
            }
        }
        return bestScore > 0 && !tied ? bestRule : null;
    }

    private MetricResolution resolveMetric(DeviceFamilyRule familyRule, String rawIdentifier) {
        String normalizedIdentifier = normalize(rawIdentifier);
        if (normalizedIdentifier == null) {
            return MetricResolution.rejected();
        }
        String leafIdentifier = extractLeaf(normalizedIdentifier);
        if (leafIdentifier == null) {
            return MetricResolution.rejected();
        }
        if (familyRule != null && familyRule.allowedMetrics().contains(leafIdentifier)) {
            return MetricResolution.allowed(leafIdentifier, normalizedIdentifier.equals(leafIdentifier) ? null : normalizedIdentifier);
        }
        if (familyRule != null) {
            return MetricResolution.rejected();
        }
        if (NON_AMBIGUOUS_CANONICAL_METRICS.contains(leafIdentifier)) {
            return MetricResolution.allowed(leafIdentifier, normalizedIdentifier.equals(leafIdentifier) ? null : normalizedIdentifier);
        }
        if (AMBIGUOUS_CANONICAL_METRICS.contains(leafIdentifier)) {
            return MetricResolution.rejected();
        }
        String lowerLeaf = leafIdentifier.toLowerCase(Locale.ROOT);
        if (STATUS_FIELDS.contains(lowerLeaf)) {
            return MetricResolution.rejected();
        }
        return MetricResolution.rejected();
    }

    private String resolveDomainLabel(DeviceFamilyRule familyRule, String canonicalIdentifier) {
        if (familyRule != null && familyRule.allowedMetrics().contains(canonicalIdentifier)) {
            return familyRule.label();
        }
        return METRIC_DOMAIN_LABELS.get(canonicalIdentifier);
    }

    private boolean containsKeyword(String text, Collection<String> keywords) {
        String normalizedText = normalize(text);
        if (normalizedText == null) {
            return false;
        }
        String lowerText = normalizedText.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (lowerText.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String extractLeaf(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return null;
        }
        String trimmed = identifier.trim();
        int dotIndex = trimmed.lastIndexOf('.');
        String leaf = dotIndex >= 0 ? trimmed.substring(dotIndex + 1) : trimmed;
        return EXACT_CANONICAL_LOOKUP.getOrDefault(leaf, leaf);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static Map<String, String> buildExactCanonicalLookup() {
        Map<String, String> lookup = new LinkedHashMap<>();
        for (String metric : NON_AMBIGUOUS_CANONICAL_METRICS) {
            lookup.put(metric, metric);
        }
        for (String metric : AMBIGUOUS_CANONICAL_METRICS) {
            lookup.put(metric, metric);
        }
        return lookup;
    }

    private static final class GovernedCandidateAccumulator {
        private final String metricIdentifier;
        private final String domainLabel;
        private final LinkedHashSet<String> evidenceSources = new LinkedHashSet<>();
        private final LinkedHashSet<String> aliases = new LinkedHashSet<>();
        private String metricName;
        private String dataType;
        private LocalDateTime lastSeenTime;
        private String sampleValue;
        private int seenCount;
        private Integer recommendationScore;
        private String recommendationLevel;
        private String baseReasonSummary;

        private GovernedCandidateAccumulator(String metricIdentifier, String domainLabel) {
            this.metricIdentifier = metricIdentifier;
            this.domainLabel = domainLabel;
        }

        private void merge(RiskPointPendingMetricCandidateVO candidate, String rawIdentifier, String aliasIdentifier) {
            if (candidate == null) {
                return;
            }
            if (candidate.getEvidenceSources() != null) {
                evidenceSources.addAll(candidate.getEvidenceSources());
            }
            if (StringUtils.hasText(aliasIdentifier)) {
                aliases.add(aliasIdentifier);
            }
            String candidateName = normalizeText(candidate.getMetricName());
            if (shouldUseCandidateName(candidateName, rawIdentifier)) {
                metricName = candidateName;
            } else if (!StringUtils.hasText(metricName)) {
                metricName = METRIC_DISPLAY_NAMES.getOrDefault(metricIdentifier, metricIdentifier);
            }
            if (!StringUtils.hasText(dataType) && StringUtils.hasText(candidate.getDataType())) {
                dataType = candidate.getDataType().trim();
            }
            LocalDateTime candidateLastSeenTime = candidate.getLastSeenTime();
            if (candidateLastSeenTime != null && (lastSeenTime == null || candidateLastSeenTime.isAfter(lastSeenTime))) {
                lastSeenTime = candidateLastSeenTime;
                sampleValue = candidate.getSampleValue();
            } else if (sampleValue == null && StringUtils.hasText(candidate.getSampleValue())) {
                sampleValue = candidate.getSampleValue();
            }
            seenCount += candidate.getSeenCount() == null ? 0 : candidate.getSeenCount();
            if (isStrongerThanCurrent(candidate.getRecommendationLevel(), candidate.getRecommendationScore())) {
                recommendationLevel = candidate.getRecommendationLevel();
                recommendationScore = candidate.getRecommendationScore();
                baseReasonSummary = normalizeText(candidate.getReasonSummary());
            }
        }

        private RiskPointPendingMetricCandidateVO toCandidate() {
            RiskPointPendingMetricCandidateVO item = new RiskPointPendingMetricCandidateVO();
            item.setMetricIdentifier(metricIdentifier);
            item.setMetricName(StringUtils.hasText(metricName) ? metricName : METRIC_DISPLAY_NAMES.getOrDefault(metricIdentifier, metricIdentifier));
            item.setDataType(dataType);
            item.setEvidenceSources(new ArrayList<>(evidenceSources));
            item.setLastSeenTime(lastSeenTime);
            item.setSampleValue(sampleValue);
            item.setSeenCount(seenCount == 0 ? null : seenCount);
            item.setRecommendationLevel(recommendationLevel);
            item.setRecommendationScore(recommendationScore);
            item.setReasonSummary(buildReasonSummary());
            return item;
        }

        private boolean shouldUseCandidateName(String candidateName, String rawIdentifier) {
            if (!StringUtils.hasText(candidateName)) {
                return false;
            }
            if (!StringUtils.hasText(metricName)) {
                return !candidateName.equals(rawIdentifier);
            }
            return metricName.equals(metricIdentifier) && !candidateName.equals(rawIdentifier);
        }

        private boolean isStrongerThanCurrent(String candidateLevel, Integer candidateScore) {
            if (!StringUtils.hasText(candidateLevel)) {
                return !StringUtils.hasText(recommendationLevel);
            }
            int currentWeight = recommendationWeight(recommendationLevel);
            int candidateWeight = recommendationWeight(candidateLevel);
            if (candidateWeight != currentWeight) {
                return candidateWeight > currentWeight;
            }
            int currentScore = recommendationScore == null ? Integer.MIN_VALUE : recommendationScore;
            int nextScore = candidateScore == null ? Integer.MIN_VALUE : candidateScore;
            return nextScore >= currentScore;
        }

        private String buildReasonSummary() {
            List<String> parts = new ArrayList<>();
            if (StringUtils.hasText(domainLabel)) {
                parts.add(domainLabel + "规范测点");
            }
            if (StringUtils.hasText(baseReasonSummary)) {
                parts.add(baseReasonSummary);
            }
            if (!aliases.isEmpty()) {
                parts.add("原始证据字段：" + String.join("、", aliases));
            }
            return parts.isEmpty() ? null : String.join("；", parts);
        }

        private String normalizeText(String value) {
            return StringUtils.hasText(value) ? value.trim() : null;
        }

        private int recommendationWeight(String level) {
            if ("HIGH".equalsIgnoreCase(level)) {
                return 3;
            }
            if ("MEDIUM".equalsIgnoreCase(level)) {
                return 2;
            }
            if ("LOW".equalsIgnoreCase(level)) {
                return 1;
            }
            return 0;
        }
    }

    private record MetricResolution(boolean allowed, String canonicalIdentifier, String aliasIdentifier) {

        private static MetricResolution allowed(String canonicalIdentifier, String aliasIdentifier) {
            return new MetricResolution(true, canonicalIdentifier, aliasIdentifier);
        }

        private static MetricResolution rejected() {
            return new MetricResolution(false, null, null);
        }
    }

    private record DeviceFamilyRule(String code,
                                    String label,
                                    Set<String> strongMetrics,
                                    Set<String> allowedMetrics,
                                    Set<String> keywords) {
    }
}
