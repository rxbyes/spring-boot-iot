package com.ghlzm.iot.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 风险点实体
 */
@Data
@TableName("risk_point")
public class RiskPoint implements Serializable {
      private static final long serialVersionUID = 1L;

      /**
       * 主键ID
       */
      @TableId(type = IdType.AUTO)
      private Long id;

      /**
       * 风险点编号
       */
      private String riskPointCode;

      /**
       * 风险点名称
       */
      private String riskPointName;

      /**
       * 区域ID
       */
      private Long regionId;

      /**
       * 区域名称
       */
      private String regionName;

      /**
       * 负责人ID
       */
      private Long responsibleUser;

      /**
       * 负责人电话
       */
      private String responsiblePhone;

      /**
       * 风险等级：critical-严重, warning-警告, info-提醒
       */
      private String riskLevel;

      /**
       * 描述
       */
      private String description;

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
      private Date createTime;

      /**
       * 更新时间
       */
      private Date updateTime;

      /**
       * 创建人
       */
      @TableField(exist = false)
      private Long createBy;

      /**
       * 更新人
       */
      @TableField(exist = false)
      private Long updateBy;

      /**
       * 逻辑删除：0-未删除，1-已删除
       */
      private Integer deleted;
}
