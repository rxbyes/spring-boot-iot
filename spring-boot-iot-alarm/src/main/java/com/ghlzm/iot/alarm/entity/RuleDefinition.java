package com.ghlzm.iot.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 阈值规则配置实体
 */
@Data
@TableName("rule_definition")
public class RuleDefinition implements Serializable {
      @Serial
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
       * 测点标识符
       */
      private Long riskMetricId;

      /**
       * 测点标识符
       */
      private String metricIdentifier;

      /**
       * 测点名称
       */
      private String metricName;

      /**
       * 表达式（如：value > 100）
       */
      private String expression;

      /**
       * 持续时间（秒）
       */
      private Integer duration;

      /**
       * 告警等级：critical-严重, warning-警告, info-提醒
       */
      private String alarmLevel;

      /**
       * 通知方式：email,sms,wechat
       */
      private String notificationMethods;

      /**
       * 是否转事件：0-否，1-是
       */
      private Integer convertToEvent;

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
