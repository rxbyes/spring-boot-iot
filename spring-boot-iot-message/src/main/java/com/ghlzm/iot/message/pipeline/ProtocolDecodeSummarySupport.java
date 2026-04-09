package com.ghlzm.iot.message.pipeline;

import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStageResult;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpProtocolMetadata;

final class ProtocolDecodeSummarySupport {

    private ProtocolDecodeSummarySupport() {
    }

    static void append(MessageFlowStageResult result, DeviceUpMessage upMessage) {
        if (result == null || upMessage == null || upMessage.getProtocolMetadata() == null) {
            return;
        }
        DeviceUpProtocolMetadata metadata = upMessage.getProtocolMetadata();
        if (metadata.getDecryptedPayloadPreview() != null && !metadata.getDecryptedPayloadPreview().isBlank()) {
            result.getSummary().put("decryptedPayloadPreview", metadata.getDecryptedPayloadPreview());
        }
        if (metadata.getDecodedPayloadPreview() != null && !metadata.getDecodedPayloadPreview().isEmpty()) {
            result.getSummary().put("decodedPayloadPreview", metadata.getDecodedPayloadPreview());
        }
    }
}
