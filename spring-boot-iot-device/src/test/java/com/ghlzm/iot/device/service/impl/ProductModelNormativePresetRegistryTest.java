package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.vo.ProductModelGovernanceEvidenceVO;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductModelNormativePresetRegistryTest {

    @Test
    void integratedPresetShouldExposeNormativePropertyDefinitionsAndAliasMappings() {
        ProductModelNormativePresetRegistry registry = new ProductModelNormativePresetRegistry();

        List<ProductModelGovernanceEvidenceVO> definitions =
                registry.buildPropertyPreset("landslide-integrated-tilt-accel-crack-v1", List.of("L1_QJ_1.X", "S1_ZT_1.signal_4g"));

        assertEquals(2, definitions.size());
        assertEquals("normative", definitions.get(0).getEvidenceOrigin());
        assertEquals("L1_QJ_1.X", definitions.get(0).getIdentifier());
        assertEquals("°", definitions.get(0).getUnit());
        assertEquals("表 B.1", definitions.get(0).getNormativeSource());
        assertTrue(registry.findNormativeIdentifier("landslide-integrated-tilt-accel-crack-v1", "signal_4g").isPresent());
    }

    @Test
    void integratedPresetShouldCoverDefaultTiltIdentifiersAndOptionalExpansionFields() {
        ProductModelNormativePresetRegistry registry = new ProductModelNormativePresetRegistry();

        List<ProductModelGovernanceEvidenceVO> definitions = registry.buildPropertyPreset(
                "landslide-integrated-tilt-accel-crack-v1",
                List.of(
                        "L1_QJ_1.X",
                        "L1_QJ_1.Y",
                        "L1_QJ_1.Z",
                        "L1_QJ_1.angle",
                        "L1_QJ_1.AZI",
                        "L1_JS_1.gX",
                        "L1_JS_1.gY",
                        "L1_JS_1.gZ",
                        "L1_LF_1.value",
                        "S1_ZT_1.signal_4g"
                )
        );

        assertEquals(10, definitions.size());
        assertIterableEquals(
                List.of(
                        "L1_QJ_1.X",
                        "L1_QJ_1.Y",
                        "L1_QJ_1.Z",
                        "L1_QJ_1.angle",
                        "L1_QJ_1.AZI",
                        "L1_JS_1.gX",
                        "L1_JS_1.gY",
                        "L1_JS_1.gZ",
                        "L1_LF_1.value",
                        "S1_ZT_1.signal_4g"
                ),
                definitions.stream().map(ProductModelGovernanceEvidenceVO::getIdentifier).toList()
        );
        assertEquals("L1_QJ_1.Y", registry.findNormativeIdentifier("landslide-integrated-tilt-accel-crack-v1", "Y").orElseThrow());
        assertEquals("L1_JS_1.gX", registry.findNormativeIdentifier("landslide-integrated-tilt-accel-crack-v1", "gX").orElseThrow());
        assertEquals("L1_LF_1.value", registry.findNormativeIdentifier("landslide-integrated-tilt-accel-crack-v1", "value").orElseThrow());
    }

    @Test
    void integratedPresetShouldRespectExplicitEmptySelection() {
        ProductModelNormativePresetRegistry registry = new ProductModelNormativePresetRegistry();

        List<ProductModelGovernanceEvidenceVO> definitions =
                registry.buildPropertyPreset("landslide-integrated-tilt-accel-crack-v1", List.of());

        assertTrue(definitions.isEmpty());
    }
}
