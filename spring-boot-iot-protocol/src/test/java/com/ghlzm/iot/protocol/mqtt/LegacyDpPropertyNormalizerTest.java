package com.ghlzm.iot.protocol.mqtt;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDpPropertyNormalizerTest {

    @Test
    void shouldNormalizeCoreFamiliesWithStablePropertyNames() {
        Object familyResolver = newInstance(
                "com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpFamilyResolver",
                new Class<?>[0]
        );
        Object normalizer = newInstance(
                "com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpPropertyNormalizer",
                new Class<?>[]{familyResolver.getClass()},
                familyResolver
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("GW001", devicePayload());

        Object result = invoke(normalizer, "normalize", payload, "GW001");

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) invoke(result, "getProperties");
        @SuppressWarnings("unchecked")
        List<String> familyCodes = (List<String>) invoke(result, "getFamilyCodes");

        assertEquals("status", invoke(result, "getMessageType"));
        assertEquals(LocalDateTime.of(2026, 3, 20, 14, 24, 2), invoke(result, "getTimestamp"));
        assertEquals(List.of("S1_ZT_1", "L1_GP_1", "L1_QJ_1", "L1_JS_1", "L1_SW_1"), familyCodes);
        assertEquals(12.3, properties.get("S1_ZT_1.ext_power_volt"));
        assertEquals(3.2, properties.get("L1_GP_1.gpsTotalZ"));
        assertEquals(1.2, properties.get("L1_QJ_1.X"));
        assertEquals(0.11, properties.get("L1_JS_1.gX"));
        assertEquals(-0.0445, properties.get("L1_SW_1.dispsX"));
    }

    private Map<String, Object> devicePayload() {
        Map<String, Object> devicePayload = new LinkedHashMap<>();
        devicePayload.put("S1_ZT_1", timestampPayload(Map.of("ext_power_volt", 12.3, "sensor_state", 1)));
        devicePayload.put("L1_GP_1", timestampPayload(Map.of("gpsTotalZ", 3.2, "gpsTotalX", 9.9, "gpsTotalY", 0.5)));
        devicePayload.put("L1_QJ_1", timestampPayload(Map.of("X", 1.2, "Y", -0.4)));
        devicePayload.put("L1_JS_1", timestampPayload(Map.of("gX", 0.11, "gY", 0.22)));
        devicePayload.put("L1_SW_1", timestampPayload(Map.of("dispsX", -0.0445, "dispsY", 0.0293)));
        return devicePayload;
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
