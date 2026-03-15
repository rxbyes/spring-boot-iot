package com.ghlzm.iot.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 组织机构实体
 */
@Data
@TableName("sys_organization")
public class Organization implements Serializable {
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
       * 父ID
       */
      private Long parentId;

      /**
       * 组织名称
       */
      private String orgName;

      /**
       * 组织编码
       */
      private String orgCode;

      /**
       * 组织类型 dept/position/team
       */
      private String orgType;

      /**
       * 负责人ID
       */
      private Long leaderUserId;

      /**
       * 负责人姓名
       */
      private String leaderName;

      /**
       * 联系电话
       */
      private String phone;

      /**
       * 联系邮箱
       */
      private String email;

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
       * 子组织列表（树形结构使用）
       */
      private List<Organization> children;
}
