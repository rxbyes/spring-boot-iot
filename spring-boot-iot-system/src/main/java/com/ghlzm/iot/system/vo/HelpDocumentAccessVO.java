package com.ghlzm.iot.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HelpDocumentAccessVO {

    private Long id;

    private String docCategory;

    private Integer sortNo;

    private String title;

    private String summary;

    private String content;

    private String keywords;

    private String relatedPaths;

    private boolean currentPathMatched;

    private List<String> keywordList = new ArrayList<>();

    private List<String> relatedPathList = new ArrayList<>();
}
