package com.ghlzm.iot.report.vo;

import lombok.Data;

import java.util.List;

/**
 * 业务验收运行请求
 */
@Data
public class BusinessAcceptanceRunRequest {

    private String packageCode;
    private String environmentCode;
    private String accountTemplateCode;
    private List<String> moduleCodes;
}
