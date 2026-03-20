package com.ghlzm.iot.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 区域管理实体
 */
@Data
@TableName("sys_region")
public class Region implements Serializable {
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
       * 区域名称
       */
      private String regionName;

      /**
       * 区域编码
       */
      private String regionCode;

      /**
       * 父ID
       */
      private Long parentId;

      /**
       * 区域类型 province/city/district/street
       */
      private String regionType;

      /**
       * 经度
       */
      private BigDecimal longitude;

      /**
       * 纬度
       */
      private BigDecimal latitude;

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
       * 子节点
       */
      @TableField(exist = false)
      private List<Region> children;

      @TableField(exist = false)
      private Boolean hasChildren;
}
