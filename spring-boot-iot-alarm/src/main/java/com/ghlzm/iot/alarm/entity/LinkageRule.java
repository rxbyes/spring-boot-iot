package com.ghlzm.iot.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 联动规则实体
 */
@Data
@TableName("linkage_rule")
public class LinkageRule implements Serializable {
      private static final long serialVersionUID = 1L;

      /**
       * 主键ID
       */
      @TableId(type = IdType.AUTO)
      private Long id;

      /**
       * 规则名称
       */
      private String ruleName;

      /**
       * 描述
       */
      private String description;

      /**
       * 触发条件（JSON格式）
       */
      private String triggerCondition;

      /**
       * 动作列表（JSON格式）
       */
      private String actionList;

      /**
       * 状态：0-启用，1-停用
       */
      private Integer status;

      /**
       * 租户ID
       */
      private Long tenantId;

      /**
       * 创建时间
       */
      private LocalDateTime createTime;

      /**
       * 更新时间
       */
      private LocalDateTime updateTime;

      /**
       * 创建人
       */
      private Long createBy;

      /**
       * 更新人
       */
      private Long updateBy;

      /**
       * 逻辑删除：0-未删除，1-已删除
       */
      private Integer deleted;
}
