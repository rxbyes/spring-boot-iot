package com.ghlzm.iot.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 菜单实体
 */
@Data
@TableName("sys_menu")
public class Menu implements Serializable {
      @Serial
      private static final long serialVersionUID = 1L;

      /**
       * 主键
       */
      @TableId(value = "id", type = IdType.ASSIGN_ID)
      private Long id;

      /**
       * 父菜单ID
       */
      private Long parentId;

      /**
       * 菜单名称
       */
      private String menuName;

      /**
       * 菜单编码
       */
      private String menuCode;

      /**
       * 路由路径
       */
      private String path;

      /**
       * 组件路径
       */
      private String component;

      /**
       * 图标
       */
      private String icon;

      /**
       * 排序
       */
      private Integer sort;

      /**
       * 类型 1菜单 2按钮
       */
      private Integer type;

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
