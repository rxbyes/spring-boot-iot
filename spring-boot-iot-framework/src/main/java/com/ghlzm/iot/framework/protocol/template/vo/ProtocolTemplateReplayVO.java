package com.ghlzm.iot.framework.protocol.template.vo;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ProtocolTemplateReplayVO {

    private String templateCode;
    private String resolvedTemplateCode;
    private Boolean matched;
    private String summary;
    private List<ExtractedChild> extractedChildren;

    @Data
    public static class ExtractedChild {
        private String logicalChannelCode;
        private Map<String, Object> childProperties;
        private String canonicalizationStrategy;
        private Boolean statusMirrorApplied;
        private String rawPayload;
    }
}
