package com.ghlzm.iot.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 风险点设备待治理绑定 Mapper
 */
@Mapper
public interface RiskPointDevicePendingBindingMapper extends BaseMapper<RiskPointDevicePendingBinding> {

    @Select("""
        SELECT *
        FROM risk_point_device_pending_binding
        WHERE id = #{id}
          AND deleted = 0
        FOR UPDATE
        """)
    RiskPointDevicePendingBinding selectByIdForUpdate(@Param("id") Long id);
}
