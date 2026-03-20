package com.ghlzm.iot.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审计日志实体
 */
@Data
@TableName("sys_audit_log")
public class AuditLog implements Serializable {
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
       * 操作用户ID
       */
      private Long userId;

      /**
       * 操作用户名称
       */
      private String userName;

      /**
       * 閾捐矾 traceId
       */
      private String traceId;

      /**
       * 鐩稿叧璁惧缂栫爜
       */
      private String deviceCode;

      /**
       * 鐩稿叧浜у搧 key
       */
      private String productKey;

      /**
       * 操作类型 insert/update/delete/select
       */
      private String operationType;

      /**
       * 操作模块
       */
      private String operationModule;

      /**
       * 操作方法
       */
      private String operationMethod;

      /**
       * 请求URL
       */
      private String requestUrl;

      /**
       * 请求方法
       */
      private String requestMethod;

      /**
       * 请求参数
       */
      private String requestParams;

      /**
       * 响应结果
       */
      private String responseResult;

      /**
       * 操作IP
       */
      private String ipAddress;

      /**
       * 操作地点
       */
      private String location;

      /**
       * 操作结果 1成功 0失败
       */
      private Integer operationResult;

      /**
       * 操作结果消息
       */
      private String resultMessage;

      /**
       * 閿欒鐮?
       */
      private String errorCode;

      /**
       * 寮傚父绫诲瀷
       */
      private String exceptionClass;

      /**
       * 操作时间
       */
      private Date operationTime;

      /**
       * 创建时间
       */
      private Date createTime;

      /**
       * 删除标记
       */
      @TableLogic
      private Integer deleted;
}
