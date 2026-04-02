package com.ghlzm.iot.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
       * 所属组织ID
       */
      private Long orgId;

      /**
       * 所属组织名称
       */
      private String orgName;

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
       * 风险点档案等级：level_1/level_2/level_3
       */
      private String riskPointLevel;

      /**
       * 当前风险态势等级：red-红色, orange-橙色, yellow-黄色, blue-蓝色
       */
      private String currentRiskLevel;

      /**
       * 历史风险等级兼容字段，过渡期保持与 currentRiskLevel 同步
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
