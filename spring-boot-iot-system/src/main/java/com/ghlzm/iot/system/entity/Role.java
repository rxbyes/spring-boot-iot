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
@TableName("sys_role")
public class Role implements Serializable {

      private static final long serialVersionUID = 1L;

      @TableId(value = "id", type = IdType.ASSIGN_ID)
      private Long id;

      private Long tenantId;

      private String roleName;

      private String roleCode;

      private String description;

      private Integer status;

      private Long createBy;

      private Date createTime;

      private Long updateBy;

      private Date updateTime;

      @TableLogic
      private Integer deleted;

      @TableField(exist = false)
      private List<Long> menuIds;
}
