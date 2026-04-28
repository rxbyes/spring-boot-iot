package com.ghlzm.iot.alarm.vo;

import com.ghlzm.iot.alarm.entity.RuleDefinition;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RuleDefinitionEffectivePreviewVO {

      private Long tenantId;
      private Long riskMetricId;
      private String metricIdentifier;
      private Long productId;
      private String productType;
      private Long deviceId;
      private Long riskPointDeviceId;
      private Boolean hasMatchedRule = false;
      private String matchedScope;
      private String matchedScopeText;
      private String decision;
      private RuleDefinition matchedRule;
      private List<Candidate> candidates = new ArrayList<>();

      @Data
      public static class Candidate {
            private Long ruleId;
            private String ruleName;
            private String ruleScope;
            private String ruleScopeText;
            private String scopeTarget;
            private String metricIdentifier;
            private String metricName;
            private String expression;
            private String alarmLevel;
            private Integer status;
            private Integer priority;
            private Boolean matchedContext;
            private Boolean selected;
            private String reason;
      }
}
