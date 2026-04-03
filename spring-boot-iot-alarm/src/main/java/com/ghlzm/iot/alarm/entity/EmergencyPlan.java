package com.ghlzm.iot.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应急预案实体
 */
@Data
@TableName("emergency_plan")
public class EmergencyPlan implements Serializable {
      private static final long serialVersionUID = 1L;

      /**
       * 主键ID
       */
      @TableId(type = IdType.AUTO)
      private Long id;

      /**
       * 预案名称
       */
      private String planName;

      /**
       * 适用告警等级：red/orange/yellow/blue
       */
      private String alarmLevel;

      /**
       * 历史风险等级兼容字段
       */
      private String riskLevel;

      /**
       * 描述
       */
      private String description;

      /**
       * 响应步骤（JSON格式）
       */
      private String responseSteps;

      /**
       * 联系人列表（JSON格式）
       */
      private String contactList;

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
