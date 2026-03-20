package com.ghlzm.iot.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 字典项实体
 */
@Data
@TableName("sys_dict_item")
public class DictItem implements Serializable {
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
       * 字典ID
       */
      private Long dictId;

      /**
       * 项名称
       */
      private String itemName;

      /**
       * 项值
       */
      private String itemValue;

      /**
       * 项类型 string/number/boolean
       */
      private String itemType;

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
