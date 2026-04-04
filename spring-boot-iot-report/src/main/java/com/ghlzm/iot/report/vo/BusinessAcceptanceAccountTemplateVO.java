package com.ghlzm.iot.report.vo;

import lombok.Data;

import java.util.List;

/**
 * 业务验收账号模板
 */
@Data
public class BusinessAcceptanceAccountTemplateVO {

    private String templateCode;
    private String templateName;
    private String username;
    private String roleHint;
    private List<String> supportedEnvironments;
}
