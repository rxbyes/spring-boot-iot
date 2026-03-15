package com.ghlzm.iot.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户实体
 */
@Data
@TableName("sys_user")
public class User implements Serializable {
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
       * 用户名
       */
      private String username;

      /**
       * 密码
       */
      private String password;

      /**
       * 真实姓名
       */
      private String realName;

      /**
       * 手机号
       */
      private String phone;

      /**
       * 邮箱
       */
      private String email;

      /**
       * 头像
       */
      private String avatar;

      /**
       * 状态 1启用 0禁用
       */
      private Integer status;

      /**
       * 最后登录时间
       */
      private Date lastLoginTime;

      /**
       * 最后登录IP
       */
      private String lastLoginIp;

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
