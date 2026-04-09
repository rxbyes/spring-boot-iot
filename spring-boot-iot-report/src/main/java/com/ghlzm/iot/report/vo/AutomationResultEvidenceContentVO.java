package com.ghlzm.iot.report.vo;

import lombok.Data;

/**
 * 自动化运行证据预览内容
 */
@Data
public class AutomationResultEvidenceContentVO {

    private String path;
    private String fileName;
    private String category;
    private String content;
    private Boolean truncated;
}
