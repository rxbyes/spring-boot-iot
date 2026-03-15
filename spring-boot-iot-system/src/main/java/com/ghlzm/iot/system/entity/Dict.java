package com.ghlzm.iot.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 字典配置实体
 */
@Data
@TableName("sys_dict")
public class Dict implements Serializable {
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
       * 字典名称
       */
      private String dictName;

      /**
       * 字典编码
       */
      private String dictCode;

      /**
       * 字典类型 text/number/boolean/date
       */
      private String dictType;

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

      /**
       * 字典项列表
       */
      @TableField(exist = false)
      private List<DictItem> items;
}
