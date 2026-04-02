package com.ghlzm.iot.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@TableName("sys_user")
public class User implements Serializable {

      private static final long serialVersionUID = 1L;

      @TableId(value = "id", type = IdType.ASSIGN_ID)
      private Long id;

      private Long tenantId;

      private Long orgId;

      private String username;

      private String password;

      private String nickname;

      private String realName;

      private String phone;

      private String email;

      private String avatar;

      private Integer status;

      private Integer isAdmin;

      private Date lastLoginTime;

      private String lastLoginIp;

      private String remark;

      private Long createBy;

      private Date createTime;

      private Long updateBy;

      private Date updateTime;

      @TableLogic
      private Integer deleted;

      @TableField(exist = false)
      private String orgName;

      @TableField(exist = false)
      private List<Long> roleIds;

      @TableField(exist = false)
      private List<String> roleNames;
}
