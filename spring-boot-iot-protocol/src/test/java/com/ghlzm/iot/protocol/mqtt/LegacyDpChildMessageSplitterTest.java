package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.ProtocolMetricEvidence;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpChildMessageSplitter;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpNormalizeResult;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpRelationResolver;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpRelationRule;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplate;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplateContext;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplateExecutionResult;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplateRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDpChildMessageSplitterTest {

    @Test
    void shouldSplitMappedDeepDisplacementChildrenWithoutChangingChildPropertyNames() {
        IotProperties iotProperties = new IotProperties();
        IotProperties.Device device = new IotProperties.Device();
        device.setSubDeviceMappings(Map.of(
                "SK00FB0D1310195",
                Map.of("L1_SW_1", "84330701", "L1_SW_2", "84330695")
        ));
        iotProperties.setDevice(device);

        Object splitter = newInstance(
                "com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpChildMessageSplitter",
                new Class<?>[]{IotProperties.class},
                iotProperties
        );
        Object normalizeResult = newInstance(
                "com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpNormalizeResult",
                new Class<?>[0]
        );

        invoke(normalizeResult, "setProperties", new LinkedHashMap<>(Map.of(
                "L1_SW_1.dispsX", -0.0445,
                "L1_SW_1.dispsY", 0.0293,
                "L1_SW_2.dispsX", -0.0293,
                "L1_SW_2.dispsY", 0.0330
        )));
        invoke(normalizeResult, "setTimestamp", LocalDateTime.of(2026, 3, 20, 14, 24, 2));
        invoke(normalizeResult, "setMessageType", "property");
        invoke(normalizeResult, "setFamilyCodes", List.of("L1_SW_1", "L1_SW_2"));

        DeviceUpMessage parentMessage = new DeviceUpMessage();
        parentMessage.setTenantId("1");
        parentMessage.setProductKey("south_deep_displacement");
        parentMessage.setDeviceCode("SK00FB0D1310195");
        parentMessage.setMessageType("property");
        parentMessage.setTopic("$dp");
        parentMessage.setTimestamp(LocalDateTime.of(2026, 3, 20, 14, 24, 2));

        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> devicePayload = new LinkedHashMap<>();
        devicePayload.put("L1_SW_1", timestampPayload(Map.of("dispsX", -0.0445, "dispsY", 0.0293)));
        devicePayload.put("L1_SW_2", timestampPayload(Map.of("dispsX", -0.0293, "dispsY", 0.0330)));
        payload.put("SK00FB0D1310195", devicePayload);

        Object result = invoke(splitter, "split", payload, parentMessage, normalizeResult);

        @SuppressWarnings("unchecked")
        Map<String, Object> parentProperties = (Map<String, Object>) invoke(result, "getProperties");
        @SuppressWarnings("unchecked")
        List<DeviceUpMessage> childMessages = (List<DeviceUpMessage>) invoke(result, "getChildMessages");

        assertEquals(Boolean.TRUE, invoke(result, "getChildSplitApplied"));
        assertTrue(parentProperties == null || parentProperties.isEmpty());
        assertEquals(2, childMessages.size());
        assertEquals("84330701", childMessages.get(0).getDeviceCode());
        assertEquals(-0.0445, childMessages.get(0).getProperties().get("dispsX"));
        assertEquals(0.0293, childMessages.get(0).getProperties().get("dispsY"));
        assertEquals("84330695", childMessages.get(1).getDeviceCode());
        assertEquals(-0.0293, childMessages.get(1).getProperties().get("dispsX"));
        assertEquals(0.0330, childMessages.get(1).getProperties().get("dispsY"));
    }

    @Test
    void shouldCollapseSingleDeepDisplacementLogicalCodeWhenNoSubDeviceMappingExists() {
        IotProperties iotProperties = new IotProperties();
        Object splitter = newInstance(
                "com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpChildMessageSplitter",
                new Class<?>[]{IotProperties.class},
                iotProperties
        );
        Object normalizeResult = newInstance(
                "com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpNormalizeResult",
                new Class<?>[0]
        );

        invoke(normalizeResult, "setProperties", new LinkedHashMap<>(Map.of(
                "S1_ZT_1.temp", 26.4,
                "L1_SW_1.dispsX", -0.0445,
                "L1_SW_1.dispsY", 0.0293
        )));
        invoke(normalizeResult, "setTimestamp", LocalDateTime.of(2026, 3, 25, 22, 10, 35));
        invoke(normalizeResult, "setMessageType", "property");
        invoke(normalizeResult, "setFamilyCodes", List.of("S1_ZT_1", "L1_SW_1"));

        DeviceUpMessage parentMessage = new DeviceUpMessage();
        parentMessage.setTenantId("1");
        parentMessage.setDeviceCode("SK00EB0D1308310");
        parentMessage.setMessageType("property");
        parentMessage.setTopic("$dp");
        parentMessage.setTimestamp(LocalDateTime.of(2026, 3, 25, 22, 10, 35));

        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> devicePayload = new LinkedHashMap<>();
        devicePayload.put("L1_SW_1", timestampPayload(Map.of("dispsX", -0.0445, "dispsY", 0.0293)));
        payload.put("SK00EB0D1308310", devicePayload);

        Object result = invoke(splitter, "split", payload, parentMessage, normalizeResult);

        @SuppressWarnings("unchecked")
        Map<String, Object> parentProperties = (Map<String, Object>) invoke(result, "getProperties");
        @SuppressWarnings("unchecked")
        List<DeviceUpMessage> childMessages = (List<DeviceUpMessage>) invoke(result, "getChildMessages");

        assertEquals(Boolean.FALSE, invoke(result, "getChildSplitApplied"));
        assertTrue(childMessages == null || childMessages.isEmpty());
        assertEquals(-0.0445, parentProperties.get("dispsX"));
        assertEquals(0.0293, parentProperties.get("dispsY"));
        assertEquals(26.4, parentProperties.get("S1_ZT_1.temp"));
        assertTrue(!parentProperties.containsKey("L1_SW_1.dispsX"));
        assertTrue(!parentProperties.containsKey("L1_SW_1.dispsY"));
    }

    @Test
    void shouldKeepLogicalPrefixWhenLegacyPayloadContainsOtherSensorFamilies() {
        IotProperties iotProperties = new IotProperties();
        Object splitter = newInstance(
                "com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpChildMessageSplitter",
                new Class<?>[]{IotProperties.class},
                iotProperties
        );
        Object normalizeResult = newInstance(
                "com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpNormalizeResult",
                new Class<?>[0]
        );

        invoke(normalizeResult, "setProperties", new LinkedHashMap<>(Map.of(
                "S1_ZT_1.temp", 26.4,
                "L1_GP_1.gpsTotalZ", 3.2,
                "L1_SW_1.dispsX", -0.0445,
                "L1_SW_1.dispsY", 0.0293
        )));
        invoke(normalizeResult, "setTimestamp", LocalDateTime.of(2026, 3, 25, 22, 10, 35));
        invoke(normalizeResult, "setMessageType", "property");
        invoke(normalizeResult, "setFamilyCodes", List.of("S1_ZT_1", "L1_GP_1", "L1_SW_1"));

        DeviceUpMessage parentMessage = new DeviceUpMessage();
        parentMessage.setTenantId("1");
        parentMessage.setDeviceCode("GW001");
        parentMessage.setMessageType("property");
        parentMessage.setTopic("$dp");
        parentMessage.setTimestamp(LocalDateTime.of(2026, 3, 25, 22, 10, 35));

        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> devicePayload = new LinkedHashMap<>();
        devicePayload.put("L1_GP_1", timestampPayload(Map.of("gpsTotalZ", 3.2)));
        devicePayload.put("L1_SW_1", timestampPayload(Map.of("dispsX", -0.0445, "dispsY", 0.0293)));
        payload.put("GW001", devicePayload);

        Object result = invoke(splitter, "split", payload, parentMessage, normalizeResult);

        @SuppressWarnings("unchecked")
        Map<String, Object> parentProperties = (Map<String, Object>) invoke(result, "getProperties");

        assertEquals(Boolean.FALSE, invoke(result, "getChildSplitApplied"));
        assertEquals(-0.0445, parentProperties.get("L1_SW_1.dispsX"));
        assertEquals(0.0293, parentProperties.get("L1_SW_1.dispsY"));
        assertEquals(3.2, parentProperties.get("L1_GP_1.gpsTotalZ"));
        assertTrue(!parentProperties.containsKey("dispsX"));
        assertTrue(!parentProperties.containsKey("dispsY"));
    }

    @Test
    void shouldSplitMappedCrackChildrenToCanonicalValueAndMirrorSensorState() {
        IotProperties iotProperties = new IotProperties();
        IotProperties.Device device = new IotProperties.Device();
        device.setSubDeviceMappings(Map.of(
                "SK00EA0D1307986",
                crackChildMappings()
        ));
        iotProperties.setDevice(device);

        Object splitter = newInstance(
                "com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpChildMessageSplitter",
                new Class<?>[]{IotProperties.class},
                iotProperties
        );
        Object normalizeResult = newInstance(
                "com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpNormalizeResult",
                new Class<?>[0]
        );

        Map<String, Double> crackValues = crackValues();
        Map<String, Object> parentProperties = new LinkedHashMap<>();
        parentProperties.put("S1_ZT_1.ext_power_volt", 12.12);
        parentProperties.put("S1_ZT_1.signal_4g", 22);
        crackValues.forEach((logicalCode, value) -> {
            parentProperties.put(logicalCode, value);
            parentProperties.put("S1_ZT_1.sensor_state." + logicalCode, 0);
        });
        invoke(normalizeResult, "setProperties", parentProperties);
        invoke(normalizeResult, "setTimestamp", LocalDateTime.of(2026, 4, 4, 22, 10, 35));
        invoke(normalizeResult, "setMessageType", "property");
        invoke(normalizeResult, "setFamilyCodes", List.of(
                "S1_ZT_1",
                "L1_LF_1",
                "L1_LF_2",
                "L1_LF_3",
                "L1_LF_4",
                "L1_LF_5",
                "L1_LF_6",
                "L1_LF_7",
                "L1_LF_8",
                "L1_LF_9"
        ));

        DeviceUpMessage parentMessage = new DeviceUpMessage();
        parentMessage.setTenantId("1");
        parentMessage.setProductKey("south_rtu");
        parentMessage.setDeviceCode("SK00EA0D1307986");
        parentMessage.setMessageType("property");
        parentMessage.setTopic("$dp");
        parentMessage.setTimestamp(LocalDateTime.of(2026, 4, 4, 22, 10, 35));

        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> devicePayload = new LinkedHashMap<>();
        Map<String, Object> statusPayload = new LinkedHashMap<>();
        statusPayload.put("ext_power_volt", 12.12);
        statusPayload.put("signal_4g", 22);
        statusPayload.put("sensor_state", new LinkedHashMap<>(Map.of(
                "L1_LF_1", 0,
                "L1_LF_2", 0,
                "L1_LF_3", 0,
                "L1_LF_4", 0,
                "L1_LF_5", 0,
                "L1_LF_6", 0,
                "L1_LF_7", 0,
                "L1_LF_8", 0,
                "L1_LF_9", 0
        )));
        devicePayload.put("S1_ZT_1", timestampPayload(statusPayload));
        crackValues.forEach((logicalCode, value) -> devicePayload.put(logicalCode, timestampPayload(value)));
        payload.put("SK00EA0D1307986", devicePayload);

        Object result = invoke(splitter, "split", payload, parentMessage, normalizeResult);

        @SuppressWarnings("unchecked")
        Map<String, Object> splitParentProperties = (Map<String, Object>) invoke(result, "getProperties");
        @SuppressWarnings("unchecked")
        List<DeviceUpMessage> childMessages = (List<DeviceUpMessage>) invoke(result, "getChildMessages");

        assertEquals(Boolean.TRUE, invoke(result, "getChildSplitApplied"));
        assertEquals(9, childMessages.size());
        assertEquals(12.12, splitParentProperties.get("S1_ZT_1.ext_power_volt"));
        assertEquals(22, splitParentProperties.get("S1_ZT_1.signal_4g"));
        assertEquals(0, splitParentProperties.get("S1_ZT_1.sensor_state.L1_LF_1"));
        assertEquals(0, splitParentProperties.get("S1_ZT_1.sensor_state.L1_LF_9"));
        assertFalse(splitParentProperties.containsKey("L1_LF_1"));
        assertFalse(splitParentProperties.containsKey("L1_LF_9"));

        Map<String, Map<String, Object>> childPropertiesByDeviceCode = new HashMap<>();
        for (DeviceUpMessage childMessage : childMessages) {
            childPropertiesByDeviceCode.put(childMessage.getDeviceCode(), childMessage.getProperties());
        }

        assertEquals(Map.of("value", 10.86, "sensor_state", 0), childPropertiesByDeviceCode.get("202018143"));
        assertEquals(Map.of("value", 6.95, "sensor_state", 0), childPropertiesByDeviceCode.get("202018135"));
        assertEquals(Map.of("value", 2473.72, "sensor_state", 0), childPropertiesByDeviceCode.get("202018121"));
        assertEquals(Map.of("value", 2473.72, "sensor_state", 0), childPropertiesByDeviceCode.get("202018137"));
        assertEquals(Map.of("value", 6.73, "sensor_state", 0), childPropertiesByDeviceCode.get("202018142"));
        assertEquals(Map.of("value", 2473.72, "sensor_state", 0), childPropertiesByDeviceCode.get("202018130"));
        assertEquals(Map.of("value", 2473.72, "sensor_state", 0), childPropertiesByDeviceCode.get("202018127"));
        assertEquals(Map.of("value", 6.82, "sensor_state", 0), childPropertiesByDeviceCode.get("202018118"));
        assertEquals(Map.of("value", 10.8, "sensor_state", 0), childPropertiesByDeviceCode.get("202018139"));
    }

    @Test
    void shouldPreferRelationRegistryRuleOverLegacyConfigFallbackForCollectorPayload() {
        IotProperties iotProperties = new IotProperties();
        IotProperties.Device device = new IotProperties.Device();
        device.setSubDeviceMappings(Map.of(
                "SK00EA0D1307986",
                Map.of("L1_LF_1", "WRONG-CONFIG-CHILD")
        ));
        iotProperties.setDevice(device);

        LegacyDpRelationResolver relationResolver = parentDeviceCode -> List.of(
                new LegacyDpRelationRule("L1_LF_1", "202018143", "LF_VALUE", "SENSOR_STATE")
        );
        LegacyDpChildMessageSplitter splitter = new LegacyDpChildMessageSplitter(iotProperties, relationResolver);
        LegacyDpNormalizeResult normalizeResult = new LegacyDpNormalizeResult();

        Map<String, Object> parentProperties = new LinkedHashMap<>();
        parentProperties.put("L1_LF_1", 10.86);
        parentProperties.put("S1_ZT_1.sensor_state.L1_LF_1", 0);
        normalizeResult.setProperties(parentProperties);
        normalizeResult.setTimestamp(LocalDateTime.of(2026, 4, 4, 22, 10, 35));
        normalizeResult.setMessageType("property");
        normalizeResult.setFamilyCodes(List.of("S1_ZT_1", "L1_LF_1"));

        DeviceUpMessage parentMessage = new DeviceUpMessage();
        parentMessage.setTenantId("1");
        parentMessage.setProductKey("south_rtu");
        parentMessage.setDeviceCode("SK00EA0D1307986");
        parentMessage.setMessageType("property");
        parentMessage.setTopic("$dp");
        parentMessage.setTimestamp(LocalDateTime.of(2026, 4, 4, 22, 10, 35));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("SK00EA0D1307986", Map.of("L1_LF_1", timestampPayload(10.86)));

        LegacyDpNormalizeResult result = splitter.split(payload, parentMessage, normalizeResult);

        assertEquals(1, result.getChildMessages().size());
        assertEquals("202018143", result.getChildMessages().get(0).getDeviceCode());
        assertEquals(Map.of("value", 10.86, "sensor_state", 0), result.getChildMessages().get(0).getProperties());
    }

    @Test
    void shouldFallbackToCompatibilitySplitWhenNoTemplateMatches() {
        IotProperties iotProperties = new IotProperties();
        IotProperties.Device device = new IotProperties.Device();
        device.setSubDeviceMappings(Map.of(
                "GW001",
                Map.of("L4_NW_1", "NW_CHILD_01")
        ));
        iotProperties.setDevice(device);

        LegacyDpChildMessageSplitter splitter = new LegacyDpChildMessageSplitter(iotProperties);
        LegacyDpNormalizeResult normalizeResult = new LegacyDpNormalizeResult();
        normalizeResult.setProperties(new LinkedHashMap<>(Map.of("L4_NW_1", 36.5)));
        normalizeResult.setTimestamp(LocalDateTime.of(2026, 4, 5, 8, 23, 10));
        normalizeResult.setMessageType("property");
        normalizeResult.setFamilyCodes(List.of("L4_NW_1"));

        DeviceUpMessage parentMessage = new DeviceUpMessage();
        parentMessage.setTenantId("1");
        parentMessage.setProductKey("compat-sensor");
        parentMessage.setDeviceCode("GW001");
        parentMessage.setMessageType("property");
        parentMessage.setTopic("$dp");
        parentMessage.setTimestamp(LocalDateTime.of(2026, 4, 5, 8, 23, 10));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("GW001", Map.of("L4_NW_1", timestampPayload(36.5)));

        LegacyDpNormalizeResult result = splitter.split(payload, parentMessage, normalizeResult);

        assertEquals(Boolean.TRUE, result.getChildSplitApplied());
        assertTrue(result.getProperties() == null || result.getProperties().isEmpty());
        assertEquals(1, result.getChildMessages().size());
        assertEquals("NW_CHILD_01", result.getChildMessages().get(0).getDeviceCode());
        assertEquals(Map.of("L4_NW_1", 36.5), result.getChildMessages().get(0).getProperties());
    }

    @Test
    void shouldUseInjectedTemplateRegistryAndExposeTemplateEvidence() {
        IotProperties iotProperties = new IotProperties();
        LegacyDpRelationResolver relationResolver = parentDeviceCode -> List.of(
                new LegacyDpRelationRule("L1_SW_1", "CUSTOM-CHILD-01", "LEGACY", "NONE")
        );
        LegacyDpChildTemplate customTemplate = new LegacyDpChildTemplate() {
            @Override
            public String getTemplateCode() {
                return "custom_child_template";
            }

            @Override
            public boolean matches(LegacyDpChildTemplateContext context) {
                return context != null && "L1_SW_1".equals(context.logicalCode());
            }

            @Override
            public LegacyDpChildTemplateExecutionResult execute(LegacyDpChildTemplateContext context) {
                ProtocolMetricEvidence evidence = new ProtocolMetricEvidence();
                evidence.setRawIdentifier("L1_SW_1.dispsX");
                evidence.setCanonicalIdentifier("custom_axis");
                evidence.setLogicalChannelCode("L1_SW_1");
                evidence.setSampleValue("99.1");
                evidence.setValueType("double");
                evidence.setEvidenceOrigin("custom_template");
                return new LegacyDpChildTemplateExecutionResult(
                        "custom_child_template",
                        Map.of("custom_axis", 99.1),
                        List.of("L1_SW_1"),
                        false,
                        "CUSTOM",
                        LocalDateTime.of(2026, 4, 5, 9, 30, 0),
                        "{\"L1_SW_1\":{\"2026-04-05T01:30:00.000Z\":{\"dispsX\":99.1}}}",
                        List.of(evidence)
                );
            }
        };
        LegacyDpChildMessageSplitter splitter = new LegacyDpChildMessageSplitter(
                iotProperties,
                relationResolver,
                new LegacyDpChildTemplateRegistry(List.of(customTemplate))
        );
        LegacyDpNormalizeResult normalizeResult = new LegacyDpNormalizeResult();
        normalizeResult.setProperties(new LinkedHashMap<>(Map.of("L1_SW_1.dispsX", 99.1)));
        normalizeResult.setTimestamp(LocalDateTime.of(2026, 4, 5, 9, 30, 0));
        normalizeResult.setMessageType("property");
        normalizeResult.setFamilyCodes(List.of("L1_SW_1"));

        DeviceUpMessage parentMessage = new DeviceUpMessage();
        parentMessage.setTenantId("1");
        parentMessage.setProductKey("custom-product");
        parentMessage.setDeviceCode("GW_CUSTOM");
        parentMessage.setMessageType("property");
        parentMessage.setTopic("$dp");
        parentMessage.setTimestamp(LocalDateTime.of(2026, 4, 5, 9, 30, 0));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("GW_CUSTOM", Map.of("L1_SW_1", timestampPayload(Map.of("dispsX", 99.1))));

        LegacyDpNormalizeResult result = splitter.split(payload, parentMessage, normalizeResult);

        assertEquals(Boolean.TRUE, result.getChildSplitApplied());
        assertEquals(1, result.getChildMessages().size());
        assertEquals("CUSTOM-CHILD-01", result.getChildMessages().get(0).getDeviceCode());
        assertEquals(Map.of("custom_axis", 99.1), result.getChildMessages().get(0).getProperties());

        Object templateEvidence = invoke(result, "getTemplateEvidence");
        assertEquals(List.of("custom_child_template"), invoke(templateEvidence, "getTemplateCodes"));
        @SuppressWarnings("unchecked")
        List<Object> executions = (List<Object>) invoke(templateEvidence, "getExecutions");
        assertEquals(1, executions.size());
        assertEquals("custom_child_template", invoke(executions.get(0), "getTemplateCode"));
        assertEquals("L1_SW_1", invoke(executions.get(0), "getLogicalChannelCode"));
        assertEquals("CUSTOM-CHILD-01", invoke(executions.get(0), "getChildDeviceCode"));
        assertEquals("CUSTOM", invoke(executions.get(0), "getCanonicalizationStrategy"));
        assertEquals(Boolean.FALSE, invoke(executions.get(0), "getStatusMirrorApplied"));
        assertEquals(List.of("L1_SW_1"), invoke(executions.get(0), "getParentRemovalKeys"));

        @SuppressWarnings("unchecked")
        List<Object> metricEvidence = (List<Object>) invoke(result, "getMetricEvidence");
        assertEquals(1, metricEvidence.size());
        assertEquals("L1_SW_1.dispsX", invoke(metricEvidence.get(0), "getRawIdentifier"));
        assertEquals("custom_axis", invoke(metricEvidence.get(0), "getCanonicalIdentifier"));
        assertEquals("L1_SW_1", invoke(metricEvidence.get(0), "getLogicalChannelCode"));
        assertEquals("GW_CUSTOM", invoke(metricEvidence.get(0), "getParentDeviceCode"));
        assertEquals("CUSTOM-CHILD-01", invoke(metricEvidence.get(0), "getChildDeviceCode"));
        assertEquals("custom_template", invoke(metricEvidence.get(0), "getEvidenceOrigin"));
    }

    private Map<String, Object> timestampPayload(Object value) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("2026-03-20T06:24:02.000Z", value);
        return payload;
    }

    private Map<String, String> crackChildMappings() {
        return Map.of(
                "L1_LF_1", "202018143",
                "L1_LF_2", "202018135",
                "L1_LF_3", "202018121",
                "L1_LF_4", "202018137",
                "L1_LF_5", "202018142",
                "L1_LF_6", "202018130",
                "L1_LF_7", "202018127",
                "L1_LF_8", "202018118",
                "L1_LF_9", "202018139"
        );
    }

    private Map<String, Double> crackValues() {
        Map<String, Double> values = new LinkedHashMap<>();
        values.put("L1_LF_1", 10.86);
        values.put("L1_LF_2", 6.95);
        values.put("L1_LF_3", 2473.72);
        values.put("L1_LF_4", 2473.72);
        values.put("L1_LF_5", 6.73);
        values.put("L1_LF_6", 2473.72);
        values.put("L1_LF_7", 2473.72);
        values.put("L1_LF_8", 6.82);
        values.put("L1_LF_9", 10.8);
        return values;
    }

    private Object newInstance(String className, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> type = Class.forName(className);
            Constructor<?> constructor = type.getConstructor(parameterTypes);
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Expected class " + className + " with the planned constructor", ex);
        }
    }

    private Object invoke(Object target, String methodName, Object... args) {
        try {
            Method method = findMethod(target.getClass(), methodName, args.length);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Expected method " + methodName + " on " + target.getClass().getName(), ex);
        }
    }

    private Method findMethod(Class<?> type, String methodName, int argCount) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == argCount) {
                return method;
            }
        }
        throw new NoSuchMethodException(methodName);
    }
}
