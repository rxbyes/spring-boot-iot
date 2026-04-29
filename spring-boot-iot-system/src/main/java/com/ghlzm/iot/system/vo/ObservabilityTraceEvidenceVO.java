package com.ghlzm.iot.system.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ObservabilityTraceEvidenceVO {

    private String traceId;
    private List<ObservabilityBusinessEventVO> businessEvents = new ArrayList<>();
    private List<ObservabilitySpanVO> spans = new ArrayList<>();
    private List<ObservabilityTraceEvidenceItemVO> timeline = new ArrayList<>();
}
