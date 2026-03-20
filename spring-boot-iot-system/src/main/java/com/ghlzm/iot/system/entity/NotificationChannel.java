package com.ghlzm.iot.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 通知渠道实体
 */
@Data
@TableName("sys_notification_channel")
public class NotificationChannel implements Serializable {
      @Serial
      private static final long serialVersionUID = 1L;

      /**
       * 主键
       */
      @TableId(value = "id", type = IdType.ASSIGN_ID)
      private Long id;

      /**
       * 租户ID
       */
      private Long tenantId;

      /**
       * 渠道名称
       */
      private String channelName;

      /**
       * 渠道编码
       */
      private String channelCode;

      /**
       * 渠道类型 email/sms/webhook/wechat/feishu/dingtalk
       */
      private String channelType;

      /**
       * 配置信息 JSON格式
       */
      private String config;

      /**
       * 状态 1启用 0禁用
       */
      private Integer status;

      /**
       * 排序
       */
      private Integer sortNo;

      /**
       * 备注
       */
      private String remark;

      /**
       * 创建人
       */
      private Long createBy;

      /**
       * 创建时间
       */
      private Date createTime;

      /**
       * 更新人
       */
      private Long updateBy;

      /**
       * 更新时间
       */
      private Date updateTime;

      /**
       * 删除标记
       */
      @TableLogic
      private Integer deleted;
}
