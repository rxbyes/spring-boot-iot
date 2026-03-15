package com.ghlzm.iot.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 角色实体
 */
@Data
@TableName("sys_role")
public class Role implements Serializable {
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
       * 角色名称
       */
      private String roleName;

      /**
       * 角色编码
       */
      private String roleCode;

      /**
       * 角色描述
       */
      private String description;

      /**
       * 状态 1启用 0禁用
       */
      private Integer status;

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
