package com.ghlzm.iot.alarm.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class DeviceThresholdOverviewVO {

    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private Long productId;
    private String productName;
    private int matchedMetricCount;
    private int missingMetricCount;
    private List<MetricItem> items = new ArrayList<>();

    @Data
    public static class MetricItem {
        private Long riskMetricId;
        private String metricIdentifier;
        private String metricName;
        private List<RuleItem> effectiveRules = new ArrayList<>();
        private List<RuleItem> bindingRules = new ArrayList<>();
        private List<RuleItem> deviceRules = new ArrayList<>();
        private List<RuleItem> productRules = new ArrayList<>();
        private List<RuleItem> fallbackRules = new ArrayList<>();
    }

    @Data
    public static class RuleItem {
        private Long ruleId;
        private String ruleName;
        private String ruleScope;
        private String ruleScopeText;
        private String expression;
        private String alarmLevel;
        private String sourceLabel;
        private String targetLabel;
        private Long riskPointDeviceId;
    }
}
