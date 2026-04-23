package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.RiskMetricCatalogReadModel;
import com.ghlzm.iot.device.mapper.RiskMetricCatalogReadMapper;
import com.ghlzm.iot.device.service.CollectorChildInsightService;
import com.ghlzm.iot.device.service.DeviceRelationService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.DeviceTopologyRoleResolver;
import com.ghlzm.iot.device.vo.CollectorChildInsightChildVO;
import com.ghlzm.iot.device.vo.SensorStateHealth;
import com.ghlzm.iot.device.vo.CollectorChildInsightMetricVO;
import com.ghlzm.iot.device.vo.CollectorChildInsightOverviewVO;
import com.ghlzm.iot.device.vo.DeviceDetailVO;
import com.ghlzm.iot.device.vo.DeviceRelationVO;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 采集器子设备总览读侧实现。
 */
@Service
public class CollectorChildInsightServiceImpl implements CollectorChildInsightService {

    private static final String SENSOR_STATE_IDENTIFIER = "sensor_state";
    private static final String LINK_STATE_REACHABLE = "reachable";
    private static final String LINK_STATE_UNREACHABLE = "unreachable";
    private static final String LINK_STATE_UNKNOWN = "unknown";

    private final DeviceService deviceService;
    private final DeviceRelationService deviceRelationService;
    private final RiskMetricCatalogReadMapper riskMetricCatalogReadMapper;
    private final DeviceTopologyRoleResolver topologyRoleResolver;

    public CollectorChildInsightServiceImpl(DeviceService deviceService,
                                            DeviceRelationService deviceRelationService,
                                            RiskMetricCatalogReadMapper riskMetricCatalogReadMapper,
                                            DeviceTopologyRoleResolver topologyRoleResolver) {
        this.deviceService = deviceService;
        this.deviceRelationService = deviceRelationService;
        this.riskMetricCatalogReadMapper = riskMetricCatalogReadMapper;
        this.topologyRoleResolver = topologyRoleResolver;
    }

    @Override
    public CollectorChildInsightOverviewVO getOverview(Long currentUserId, String parentDeviceCode) {
        DeviceDetailVO parent = deviceService.getDetailByCode(currentUserId, parentDeviceCode);
        List<DeviceRelationVO> relations = deviceRelationService.listByParentDeviceCode(currentUserId, parentDeviceCode);
        List<CollectorChildInsightChildVO> children = new ArrayList<>();
        Map<Long, List<String>> recommendedMetricCache = new LinkedHashMap<>();
        for (DeviceRelationVO relation : relations) {
            children.add(toChildOverview(currentUserId, parent, relation, recommendedMetricCache));
        }

        CollectorChildInsightOverviewVO overview = new CollectorChildInsightOverviewVO();
        overview.setParentDeviceCode(parent.getDeviceCode());
        overview.setParentOnlineStatus(parent.getOnlineStatus());
        overview.setChildCount(children.size());
        overview.setReachableChildCount((int) children.stream()
                .filter(child -> LINK_STATE_REACHABLE.equals(child.getCollectorLinkState()))
                .count());
        overview.setSensorStateReportedCount((int) children.stream()
                .filter(child -> hasText(child.getSensorStateValue()))
                .count());
        overview.setMissingChildCount((int) children.stream()
                .filter(child -> SensorStateHealth.MISSING.equals(child.getSensorStateHealth()))
                .count());
        overview.setStaleChildCount((int) children.stream()
                .filter(child -> SensorStateHealth.STALE.equals(child.getSensorStateHealth()))
                .count());
        overview.setRecommendedMetricCount((int) children.stream()
                .map(CollectorChildInsightChildVO::getRecommendedMetricIdentifiers)
                .filter(list -> list != null && !list.isEmpty())
                .flatMap(List::stream)
                .distinct()
                .count());
        overview.setChildren(children);
        return overview;
    }

    @Override
    public List<String> listRecommendedMetrics(Long productId) {
        if (productId == null || riskMetricCatalogReadMapper == null) {
            return List.of();
        }
        List<RiskMetricCatalogReadModel> rows = riskMetricCatalogReadMapper.selectList(
                new LambdaQueryWrapper<RiskMetricCatalogReadModel>()
                        .eq(RiskMetricCatalogReadModel::getProductId, productId)
                        .eq(RiskMetricCatalogReadModel::getEnabled, 1)
                        .eq(RiskMetricCatalogReadModel::getDeleted, 0)
                        .eq(RiskMetricCatalogReadModel::getInsightEnabled, 1)
        );
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Set<String> identifiers = new LinkedHashSet<>();
        for (RiskMetricCatalogReadModel row : rows) {
            String identifier = normalizeIdentifier(row == null ? null : row.getContractIdentifier());
            if (identifier != null) {
                identifiers.add(identifier);
            }
        }
        return List.copyOf(identifiers);
    }

    private CollectorChildInsightChildVO toChildOverview(Long currentUserId,
                                                         DeviceDetailVO parent,
                                                         DeviceRelationVO relation,
                                                         Map<Long, List<String>> recommendedMetricCache) {
        DeviceDetailVO child = deviceService.getDetailByCode(currentUserId, relation.getChildDeviceCode());
        List<DeviceProperty> properties = deviceService.listProperties(currentUserId, relation.getChildDeviceCode());
        List<String> recommendedMetricIdentifiers = child == null || child.getProductId() == null
                ? List.of()
                : recommendedMetricCache.computeIfAbsent(child.getProductId(), this::listRecommendedMetrics);
        Set<String> recommendedMetricSet = new LinkedHashSet<>(recommendedMetricIdentifiers);

        CollectorChildInsightChildVO item = new CollectorChildInsightChildVO();
        item.setLogicalChannelCode(relation.getLogicalChannelCode());
        item.setChildDeviceCode(relation.getChildDeviceCode());
        item.setChildDeviceName(child.getDeviceName());
        item.setChildProductKey(firstNonBlank(relation.getChildProductKey(), child.getProductKey()));
        item.setCollectorLinkState(resolveCollectorLinkState(parent, child));
        item.setSensorStateValue(resolvePropertyValue(properties, SENSOR_STATE_IDENTIFIER));
        item.setSensorStateHealth(resolveSensorStateHealth(child, properties));
        item.setLastReportTime(child.getLastReportTime());
        List<CollectorChildInsightMetricVO> metrics = selectMonitoringMetrics(properties, recommendedMetricSet);
        item.setMetrics(metrics);
        item.setRecommendedMetricIdentifiers(metrics.stream()
                .filter(metric -> Boolean.TRUE.equals(metric.getRecommended()))
                .map(metric -> normalizeIdentifier(metric.getIdentifier()))
                .filter(value -> value != null)
                .distinct()
                .toList());
        return item;
    }

    private List<CollectorChildInsightMetricVO> selectMonitoringMetrics(List<DeviceProperty> properties,
                                                                        Set<String> recommendedMetricIdentifiers) {
        if (properties == null || properties.isEmpty()) {
            return List.of();
        }
        List<CollectorChildInsightMetricVO> metrics = new ArrayList<>();
        for (DeviceProperty property : properties) {
            String identifier = normalizeIdentifier(property == null ? null : property.getIdentifier());
            if (identifier == null || SENSOR_STATE_IDENTIFIER.equals(identifier)) {
                continue;
            }
            CollectorChildInsightMetricVO metric = new CollectorChildInsightMetricVO();
            metric.setIdentifier(property.getIdentifier());
            metric.setDisplayName(property.getPropertyName());
            metric.setPropertyValue(property.getPropertyValue());
            metric.setUnit(null);
            metric.setReportTime(property.getReportTime());
            metric.setRecommended(recommendedMetricIdentifiers != null
                    && recommendedMetricIdentifiers.contains(identifier));
            metrics.add(metric);
        }
        return metrics;
    }

    private SensorStateHealth resolveSensorStateHealth(DeviceDetailVO child, List<DeviceProperty> properties) {
        String sensorStateValue = resolvePropertyValue(properties, SENSOR_STATE_IDENTIFIER);
        if (!hasText(sensorStateValue)) {
            if (child == null || child.getLastReportTime() == null) {
                return SensorStateHealth.MISSING;
            }
            long hoursSinceReport = Duration.between(child.getLastReportTime(), LocalDateTime.now()).toHours();
            if (hoursSinceReport > 24) {
                return SensorStateHealth.STALE;
            }
            return SensorStateHealth.MISSING;
        }
        String normalized = sensorStateValue.trim().toLowerCase(Locale.ROOT);
        if ("1".equals(normalized) || "online".equals(normalized) || "正常".equals(normalized)) {
            return SensorStateHealth.REPORTED_NORMAL;
        }
        return SensorStateHealth.REPORTED_ABNORMAL;
    }

    private String resolvePropertyValue(List<DeviceProperty> properties, String identifier) {
        String normalizedIdentifier = normalizeIdentifier(identifier);
        if (normalizedIdentifier == null || properties == null || properties.isEmpty()) {
            return null;
        }
        for (DeviceProperty property : properties) {
            if (normalizedIdentifier.equals(normalizeIdentifier(property == null ? null : property.getIdentifier()))) {
                return property.getPropertyValue();
            }
        }
        return null;
    }

    private String resolveCollectorLinkState(DeviceDetailVO parent, DeviceDetailVO child) {
        Integer parentOnlineStatus = parent == null ? null : parent.getOnlineStatus();
        Integer childOnlineStatus = child == null ? null : child.getOnlineStatus();
        if (Integer.valueOf(1).equals(parentOnlineStatus) && Integer.valueOf(1).equals(childOnlineStatus)) {
            return LINK_STATE_REACHABLE;
        }
        if (Integer.valueOf(0).equals(parentOnlineStatus) || Integer.valueOf(0).equals(childOnlineStatus)) {
            return LINK_STATE_UNREACHABLE;
        }
        return LINK_STATE_UNKNOWN;
    }

    private String normalizeIdentifier(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String firstNonBlank(String preferred, String fallback) {
        return hasText(preferred) ? preferred : fallback;
    }
}
