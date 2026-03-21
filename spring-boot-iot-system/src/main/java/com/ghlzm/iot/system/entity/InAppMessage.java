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
 * 站内消息实体。
 */
@Data
@TableName("sys_in_app_message")
public class InAppMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /**
     * 消息类型：system/business/error
     */
    private String messageType;

    /**
     * 优先级：critical/high/medium/low
     */
    private String priority;

    private String title;

    private String summary;

    private String content;

    /**
     * 推送范围：all/role/user
     */
    private String targetType;

    /**
     * 目标角色编码，逗号分隔。
     */
    private String targetRoleCodes;

    /**
     * 目标用户 ID，逗号分隔。
     */
    private String targetUserIds;

    private String relatedPath;

    private String sourceType;

    private String sourceId;

    /**
     * 去重键，按 sourceType/sourceId/target/messageType 生成。
     */
    private String dedupKey;

    private Date publishTime;

    private Date expireTime;

    /**
     * 状态：1 发布中，0 停用。
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
