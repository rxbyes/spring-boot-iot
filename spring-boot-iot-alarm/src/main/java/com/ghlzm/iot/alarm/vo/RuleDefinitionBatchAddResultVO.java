package com.ghlzm.iot.alarm.vo;

import java.util.ArrayList;
import java.util.List;

public class RuleDefinitionBatchAddResultVO {

    private int totalCount;

    private int successCount;

    private int failedCount;

    private List<Item> items = new ArrayList<>();

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class Item {

        private int index;

        private Long ruleId;

        private String ruleName;

        private String metricIdentifier;

        private Boolean success;

        private String message;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public Long getRuleId() {
            return ruleId;
        }

        public void setRuleId(Long ruleId) {
            this.ruleId = ruleId;
        }

        public String getRuleName() {
            return ruleName;
        }

        public void setRuleName(String ruleName) {
            this.ruleName = ruleName;
        }

        public String getMetricIdentifier() {
            return metricIdentifier;
        }

        public void setMetricIdentifier(String metricIdentifier) {
            this.metricIdentifier = metricIdentifier;
        }

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
