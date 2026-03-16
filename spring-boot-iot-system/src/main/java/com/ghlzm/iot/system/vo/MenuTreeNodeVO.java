package com.ghlzm.iot.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MenuTreeNodeVO {

    private Long id;

    private Long parentId;

    private String menuName;

    private String menuCode;

    private String path;

    private String component;

    private String icon;

    private Integer sort;

    private Integer type;

    private MenuMetaVO meta;

    private List<MenuTreeNodeVO> children = new ArrayList<>();
}
