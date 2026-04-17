package com.ghlzm.iot.protocol.mqtt.legacy.template.replay;

import com.ghlzm.iot.framework.protocol.template.dto.ProtocolTemplateReplayDTO;
import com.ghlzm.iot.framework.protocol.template.vo.ProtocolTemplateReplayVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDpTemplateReplayServiceImplTest {

    private final LegacyDpTemplateReplayService replayService = new LegacyDpTemplateReplayServiceImpl();

    @Test
    void replayShouldExtractCrackChildFromCollectorPayload() {
        ProtocolTemplateReplayDTO dto = new ProtocolTemplateReplayDTO();
        dto.setTemplateCode("legacy-dp-crack-v1");
        dto.setPayloadJson("""
                {
                  "L1_LF_1": {
                    "2026-04-05T08:23:10.000Z": 0.2136
                  }
                }
                """);

        ProtocolTemplateReplayVO result = replayService.replay(dto);

        assertTrue(Boolean.TRUE.equals(result.getMatched()));
        assertEquals("legacy-dp-crack-v1", result.getTemplateCode());
        assertEquals(1, result.getExtractedChildren().size());
        assertEquals("L1_LF_1", result.getExtractedChildren().get(0).getLogicalChannelCode());
    }

    @Test
    void replayShouldReturnMissWhenPayloadDoesNotMatchRequestedTemplate() {
        ProtocolTemplateReplayDTO dto = new ProtocolTemplateReplayDTO();
        dto.setTemplateCode("legacy-dp-crack-v1");
        dto.setPayloadJson("""
                {
                  "L1_SW_1": {
                    "2026-04-05T08:23:10.000Z": {
                      "dispsX": -0.0445,
                      "dispsY": 0.0293
                    }
                  }
                }
                """);

        ProtocolTemplateReplayVO result = replayService.replay(dto);

        assertFalse(Boolean.TRUE.equals(result.getMatched()));
        assertEquals(0, result.getExtractedChildren().size());
    }
}
