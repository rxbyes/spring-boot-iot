package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.vo.ProductModelGovernanceEvidenceVO;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
