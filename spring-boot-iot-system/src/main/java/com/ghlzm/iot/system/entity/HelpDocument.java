package com.ghlzm.iot.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 帮助文档实体。
 */
@Data
@TableName("sys_help_document")
public class HelpDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /**
     * 文档分类：business/technical/faq
     */
    private String docCategory;

    private String title;

    private String summary;

    private String content;

    /**
     * 关键词，逗号分隔。
     */
    private String keywords;

    /**
     * 关联页面路径，逗号分隔。
     */
    private String relatedPaths;

    /**
     * 可见角色编码，逗号分隔。
     */
    private String visibleRoleCodes;

    /**
     * 状态：1 启用，0 停用。
     */
    private Integer status;

    private Integer sortNo;

    private Long createBy;

    private Date createTime;

    private Long updateBy;

    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
