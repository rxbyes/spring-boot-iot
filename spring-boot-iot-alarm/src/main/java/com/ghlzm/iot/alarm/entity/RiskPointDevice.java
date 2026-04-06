package com.ghlzm.iot.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 风险点与设备绑定实体
 */
@Data
@TableName("risk_point_device")
public class RiskPointDevice implements Serializable {
      private static final long serialVersionUID = 1L;

      /**
       * 主键ID
       */
      @TableId(type = IdType.AUTO)
      private Long id;

      /**
       * 风险点ID
       */
      private Long riskPointId;

      /**
       * 设备ID
       */
      private Long deviceId;

      /**
       * 设备编码
       */
      private String deviceCode;

      /**
       * 设备名称
       */
      private String deviceName;

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
       * 默认阈值
       */
      private String defaultThreshold;

      /**
       * 阈值单位
       */
      private String thresholdUnit;

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
