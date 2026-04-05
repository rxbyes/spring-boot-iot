package com.ghlzm.iot.protocol.mqtt.legacy.template;

/**
 * legacy `$dp` 子设备协议模板定义。
 */
public interface LegacyDpChildTemplate {

    String getTemplateCode();

    boolean matches(LegacyDpChildTemplateContext context);

    LegacyDpChildTemplateExecutionResult execute(LegacyDpChildTemplateContext context);
}
