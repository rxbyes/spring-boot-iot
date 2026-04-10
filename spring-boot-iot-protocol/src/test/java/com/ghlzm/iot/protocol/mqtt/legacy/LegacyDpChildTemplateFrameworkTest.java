package com.ghlzm.iot.protocol.mqtt.legacy;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDpChildTemplateFrameworkTest {

    private static final String TEMPLATE_PACKAGE = "com.ghlzm.iot.protocol.mqtt.legacy.template.";
    private static final String REGISTRY_CLASS = TEMPLATE_PACKAGE + "LegacyDpChildTemplateRegistry";
    private static final String MATCHER_CLASS = TEMPLATE_PACKAGE + "LegacyDpChildTemplateMatcher";
    private static final String EXECUTOR_CLASS = TEMPLATE_PACKAGE + "LegacyDpChildTemplateExecutor";
    private static final String CONTEXT_CLASS = TEMPLATE_PACKAGE + "LegacyDpChildTemplateContext";

    @Test
    void shouldExposeBuiltinWaveOneTemplatesInStableOrder() {
        Object registry = newInstance(REGISTRY_CLASS, new Class<?>[0]);

        @SuppressWarnings("unchecked")
        List<Object> templates = (List<Object>) invoke(registry, "listTemplates");
        List<String> templateCodes = templates.stream()
                .map(template -> String.valueOf(invoke(template, "getTemplateCode")))
                .toList();

        assertEquals(List.of("crack_child_template", "deep_displacement_child_template"), templateCodes);
    }

    @Test
    void shouldMatchAndExecuteCrackChildTemplate() {
        Object registry = newInstance(REGISTRY_CLASS, new Class<?>[0]);
        Object matcher = newInstance(MATCHER_CLASS, new Class<?>[]{loadClass(REGISTRY_CLASS)}, registry);
        Object executor = newInstance(EXECUTOR_CLASS, new Class<?>[0]);
        LegacyDpRelationRule relationRule = new LegacyDpRelationRule("L1_LF_1", "202018143", "LF_VALUE", "SENSOR_STATE");

        Map<String, Object> parentProperties = new LinkedHashMap<>();
        parentProperties.put("S1_ZT_1.sensor_state.L1_LF_1", 0);
        Object context = newInstance(
                CONTEXT_CLASS,
                new Class<?>[]{LegacyDpRelationRule.class, String.class, Object.class, Map.class},
                relationRule,
                "L1_LF_1",
                timestampPayload(10.86),
                parentProperties
        );

        Object matched = invoke(matcher, "match", context);
        assertTrue((Boolean) invoke(matched, "isPresent"));

        Object template = invoke(matched, "get");
        assertEquals("crack_child_template", invoke(template, "getTemplateCode"));

        Object execution = invoke(executor, "execute", template, context);

        @SuppressWarnings("unchecked")
        Map<String, Object> childProperties = (Map<String, Object>) invoke(execution, "childProperties");
        @SuppressWarnings("unchecked")
        List<String> parentRemovalKeys = (List<String>) invoke(execution, "parentRemovalKeys");
        @SuppressWarnings("unchecked")
        List<Object> metricEvidence = (List<Object>) invoke(execution, "metricEvidence");

        assertEquals(Map.of("value", 10.86, "sensor_state", 0), childProperties);
        assertEquals(List.of("L1_LF_1"), parentRemovalKeys);
        assertEquals("crack_child_template", invoke(execution, "templateCode"));
        assertEquals("LF_VALUE", invoke(execution, "canonicalizationStrategy"));
        assertEquals(Boolean.TRUE, invoke(execution, "statusMirrorApplied"));
        assertEquals(2, metricEvidence.size());
        assertEquals("value", invoke(metricEvidence.get(0), "getCanonicalIdentifier"));
        assertEquals("sensor_state", invoke(metricEvidence.get(1), "getCanonicalIdentifier"));
    }

    @Test
    void shouldMatchAndExecuteDeepDisplacementChildTemplate() {
        Object registry = newInstance(REGISTRY_CLASS, new Class<?>[0]);
        Object matcher = newInstance(MATCHER_CLASS, new Class<?>[]{loadClass(REGISTRY_CLASS)}, registry);
        Object executor = newInstance(EXECUTOR_CLASS, new Class<?>[0]);
        LegacyDpRelationRule relationRule = new LegacyDpRelationRule("L1_SW_1", "84330701", "LEGACY", "NONE");

        Object context = newInstance(
                CONTEXT_CLASS,
                new Class<?>[]{LegacyDpRelationRule.class, String.class, Object.class, Map.class},
                relationRule,
                "L1_SW_1",
                timestampPayload(Map.of("dispsX", -0.0445, "dispsY", 0.0293)),
                Map.of()
        );

        Object matched = invoke(matcher, "match", context);
        assertTrue((Boolean) invoke(matched, "isPresent"));

        Object template = invoke(matched, "get");
        assertEquals("deep_displacement_child_template", invoke(template, "getTemplateCode"));

        Object execution = invoke(executor, "execute", template, context);

        @SuppressWarnings("unchecked")
        Map<String, Object> childProperties = (Map<String, Object>) invoke(execution, "childProperties");
        @SuppressWarnings("unchecked")
        List<String> parentRemovalKeys = (List<String>) invoke(execution, "parentRemovalKeys");

        assertEquals(Map.of("dispsX", -0.0445, "dispsY", 0.0293), childProperties);
        assertEquals(List.of("L1_SW_1"), parentRemovalKeys);
        assertEquals("deep_displacement_child_template", invoke(execution, "templateCode"));
        assertEquals("LEGACY", invoke(execution, "canonicalizationStrategy"));
        assertEquals(Boolean.FALSE, invoke(execution, "statusMirrorApplied"));
    }

    @Test
    void shouldMirrorSensorStateWhenDeepDisplacementChildTemplateUsesSensorStateStrategy() {
        Object registry = newInstance(REGISTRY_CLASS, new Class<?>[0]);
        Object matcher = newInstance(MATCHER_CLASS, new Class<?>[]{loadClass(REGISTRY_CLASS)}, registry);
        Object executor = newInstance(EXECUTOR_CLASS, new Class<?>[0]);
        LegacyDpRelationRule relationRule = new LegacyDpRelationRule("L1_SW_1", "84330701", "LEGACY", "SENSOR_STATE");

        Map<String, Object> parentProperties = new LinkedHashMap<>();
        parentProperties.put("S1_ZT_1.sensor_state.L1_SW_1", 0);
        Object context = newInstance(
                CONTEXT_CLASS,
                new Class<?>[]{LegacyDpRelationRule.class, String.class, Object.class, Map.class},
                relationRule,
                "L1_SW_1",
                timestampPayload(Map.of("dispsX", -0.0445, "dispsY", 0.0293)),
                parentProperties
        );

        Object matched = invoke(matcher, "match", context);
        assertTrue((Boolean) invoke(matched, "isPresent"));

        Object template = invoke(matched, "get");
        Object execution = invoke(executor, "execute", template, context);

        @SuppressWarnings("unchecked")
        Map<String, Object> childProperties = (Map<String, Object>) invoke(execution, "childProperties");
        @SuppressWarnings("unchecked")
        List<String> parentRemovalKeys = (List<String>) invoke(execution, "parentRemovalKeys");
        @SuppressWarnings("unchecked")
        List<Object> metricEvidence = (List<Object>) invoke(execution, "metricEvidence");

        assertEquals(Map.of("dispsX", -0.0445, "dispsY", 0.0293, "sensor_state", 0), childProperties);
        assertEquals(List.of("L1_SW_1", "S1_ZT_1.sensor_state.L1_SW_1"), parentRemovalKeys);
        assertEquals(Boolean.TRUE, invoke(execution, "statusMirrorApplied"));
        assertEquals(3, metricEvidence.size());
        assertEquals("sensor_state", invoke(metricEvidence.get(2), "getCanonicalIdentifier"));
    }

    @Test
    void shouldLeaveUnsupportedPayloadShapeToCompatibilityFallback() {
        Object registry = newInstance(REGISTRY_CLASS, new Class<?>[0]);
        Object matcher = newInstance(MATCHER_CLASS, new Class<?>[]{loadClass(REGISTRY_CLASS)}, registry);
        LegacyDpRelationRule relationRule = new LegacyDpRelationRule("L1_SW_1", "84330701", "LEGACY", "NONE");

        Object context = newInstance(
                CONTEXT_CLASS,
                new Class<?>[]{LegacyDpRelationRule.class, String.class, Object.class, Map.class},
                relationRule,
                "L1_SW_1",
                timestampPayload(12.34),
                Map.of()
        );

        Object matched = invoke(matcher, "match", context);
        assertFalse((Boolean) invoke(matched, "isPresent"));
    }

    private Map<String, Object> timestampPayload(Object value) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("2026-04-05T08:23:10.000Z", value);
        return payload;
    }

    private Object newInstance(String className, Class<?>[] parameterTypes, Object... args) {
        try {
            Constructor<?> constructor = loadClass(className).getConstructor(parameterTypes);
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Expected class " + className + " with the planned constructor", ex);
        }
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new AssertionError("Expected class " + className, ex);
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
