package com.ghlzm.iot.device.service.model;

/**
 * 指标标识符解析结果。
 */
public record MetricIdentifierResolution(
        String inputIdentifier,
        String canonicalIdentifier,
        String source
) {

    public static final String SOURCE_PUBLISHED_SNAPSHOT = "PUBLISHED_SNAPSHOT";
    public static final String SOURCE_CASE_INSENSITIVE_GUESS = "CASE_INSENSITIVE_GUESS";
    public static final String SOURCE_RAW_IDENTIFIER = "RAW_IDENTIFIER";

    public static MetricIdentifierResolution of(String inputIdentifier, String canonicalIdentifier, String source) {
        return new MetricIdentifierResolution(inputIdentifier, canonicalIdentifier, source);
    }
}
