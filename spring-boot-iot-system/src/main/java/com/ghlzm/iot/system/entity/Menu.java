package com.ghlzm.iot.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sys_menu")
public class Menu implements Serializable {

      @Serial
      private static final long serialVersionUID = 1L;

      @TableId(value = "id", type = IdType.ASSIGN_ID)
      private Long id;

      private Long parentId;

      private String menuName;

      private String menuCode;

      private String path;

      private String component;

      private String icon;

      @TableField("meta_json")
      private String metaJson;

      private Integer sort;

      private Integer type;

      private Integer status;

      private Long createBy;

      private Date createTime;

      private Long updateBy;

      private Date updateTime;

      @TableLogic
      private Integer deleted;
}
