package com.ghlzm.iot.report.vo;

import lombok.Data;

/**
 * 自动化运行证据条目
 */
@Data
public class AutomationResultEvidenceItemVO {

    private String path;
    private String fileName;
    private String category;
    private String source;
}
