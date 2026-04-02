package com.ghlzm.iot.system.enums;

import org.springframework.util.StringUtils;

import java.util.Arrays;

public enum DataScopeType {

    ALL(5, "全局"),
    TENANT(4, "租户内全部"),
    ORG_AND_CHILDREN(3, "本机构及下级"),
    ORG(2, "仅本机构"),
    SELF(1, "仅本人");

    private final int priority;
    private final String label;

    DataScopeType(int priority, String label) {
        this.priority = priority;
        this.label = label;
    }

    public int getPriority() {
        return priority;
    }

    public String getLabel() {
        return label;
    }

    public static DataScopeType fromCode(String code) {
        if (!StringUtils.hasText(code)) {
            return TENANT;
        }
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(code))
                .findFirst()
                .orElse(TENANT);
    }

    public static DataScopeType pickHigher(DataScopeType left, DataScopeType right) {
        if (left == null) {
            return right == null ? TENANT : right;
        }
        if (right == null) {
            return left;
        }
        return left.priority >= right.priority ? left : right;
    }
}
