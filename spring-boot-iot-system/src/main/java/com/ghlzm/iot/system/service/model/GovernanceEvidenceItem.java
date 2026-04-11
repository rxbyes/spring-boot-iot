package com.ghlzm.iot.system.service.model;

import lombok.Data;

@Data
public class GovernanceEvidenceItem {

    private String evidenceType;
    private String title;
    private String summary;
    private String sourceType;
    private String sourceId;
}
