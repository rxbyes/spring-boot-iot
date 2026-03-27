package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private Map<String, Object> timestampPayload(Map<String, Object> value) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("2026-03-20T06:24:02.000Z", value);
        return payload;
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
