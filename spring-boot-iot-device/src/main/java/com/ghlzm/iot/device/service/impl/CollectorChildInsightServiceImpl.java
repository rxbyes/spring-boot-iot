package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.service.CollectorChildInsightService;
import com.ghlzm.iot.device.service.DeviceRelationService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.vo.CollectorChildInsightChildVO;
import com.ghlzm.iot.device.vo.CollectorChildInsightMetricVO;
import com.ghlzm.iot.device.vo.CollectorChildInsightOverviewVO;
import com.ghlzm.iot.device.vo.DeviceDetailVO;
import com.ghlzm.iot.device.vo.DeviceRelationVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    public CollectorChildInsightServiceImpl(DeviceService deviceService,
                                            DeviceRelationService deviceRelationService) {
        this.deviceService = deviceService;
        this.deviceRelationService = deviceRelationService;
    }

    @Override
    public CollectorChildInsightOverviewVO getOverview(Long currentUserId, String parentDeviceCode) {
        DeviceDetailVO parent = deviceService.getDetailByCode(currentUserId, parentDeviceCode);
        List<DeviceRelationVO> relations = deviceRelationService.listByParentDeviceCode(currentUserId, parentDeviceCode);
        List<CollectorChildInsightChildVO> children = new ArrayList<>();
        for (DeviceRelationVO relation : relations) {
            children.add(toChildOverview(currentUserId, parent, relation));
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
        overview.setChildren(children);
        return overview;
    }

    private CollectorChildInsightChildVO toChildOverview(Long currentUserId,
                                                         DeviceDetailVO parent,
                                                         DeviceRelationVO relation) {
        DeviceDetailVO child = deviceService.getDetailByCode(currentUserId, relation.getChildDeviceCode());
        List<DeviceProperty> properties = deviceService.listProperties(currentUserId, relation.getChildDeviceCode());

        CollectorChildInsightChildVO item = new CollectorChildInsightChildVO();
        item.setLogicalChannelCode(relation.getLogicalChannelCode());
        item.setChildDeviceCode(relation.getChildDeviceCode());
        item.setChildDeviceName(child.getDeviceName());
        item.setChildProductKey(firstNonBlank(relation.getChildProductKey(), child.getProductKey()));
        item.setCollectorLinkState(resolveCollectorLinkState(parent, child));
        item.setSensorStateValue(resolvePropertyValue(properties, SENSOR_STATE_IDENTIFIER));
        item.setLastReportTime(child.getLastReportTime());
        item.setMetrics(selectMonitoringMetrics(properties));
        return item;
    }

    private List<CollectorChildInsightMetricVO> selectMonitoringMetrics(List<DeviceProperty> properties) {
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
            metrics.add(metric);
        }
        return metrics;
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
