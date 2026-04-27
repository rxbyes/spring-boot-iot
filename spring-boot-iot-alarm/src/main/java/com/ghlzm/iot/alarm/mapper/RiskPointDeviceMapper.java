package com.ghlzm.iot.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 风险点与设备绑定Mapper
 */
@Mapper
public interface RiskPointDeviceMapper extends BaseMapper<RiskPointDevice> {
    @Select("""
            SELECT id,
                   risk_point_id,
                   device_id,
                   device_code,
                   device_name,
                   risk_metric_id,
                   metric_identifier,
                   metric_name,
                   default_threshold,
                   threshold_unit,
                   create_time,
                   update_time,
                   create_by,
                   update_by,
                   deleted
            FROM risk_point_device
            WHERE risk_point_id = #{riskPointId}
              AND device_id = #{deviceId}
              AND metric_identifier = #{metricIdentifier}
              AND deleted <> 0
            LIMIT 1
            """)
    RiskPointDevice findSoftDeletedBinding(@Param("riskPointId") Long riskPointId,
                                           @Param("deviceId") Long deviceId,
                                           @Param("metricIdentifier") String metricIdentifier);

    @Update("""
            UPDATE risk_point_device
            SET device_code = #{binding.deviceCode},
                device_name = #{binding.deviceName},
                risk_metric_id = #{binding.riskMetricId},
                metric_name = #{binding.metricName},
                default_threshold = #{binding.defaultThreshold},
                threshold_unit = #{binding.thresholdUnit},
                update_time = #{binding.updateTime},
                update_by = #{binding.updateBy},
                deleted = 0
            WHERE id = #{binding.id}
              AND risk_point_id = #{binding.riskPointId}
              AND device_id = #{binding.deviceId}
              AND metric_identifier = #{binding.metricIdentifier}
            """)
    int restoreSoftDeletedBinding(@Param("binding") RiskPointDevice binding);
}
