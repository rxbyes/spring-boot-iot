package com.ghlzm.iot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.device.entity.DeviceOnlineSession;
import com.ghlzm.iot.device.vo.ProductActivityStatRow;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeviceOnlineSessionMapper extends BaseMapper<DeviceOnlineSession> {

    @Select({
            "SELECT",
            "  #{productId} AS productId,",
            "  CAST(ROUND(AVG(TIMESTAMPDIFF(MINUTE, online_time, COALESCE(offline_time, #{statTime})))) AS SIGNED) AS avgOnlineDuration,",
            "  CAST(MAX(TIMESTAMPDIFF(MINUTE, online_time, COALESCE(offline_time, #{statTime}))) AS SIGNED) AS maxOnlineDuration",
            "FROM iot_device_online_session",
            "WHERE deleted = 0",
            "  AND product_id = #{productId}",
            "  AND online_time >= #{thirtyDaysStart}"
    })
    ProductActivityStatRow selectProductDurationStat(@Param("productId") Long productId,
                                                     @Param("thirtyDaysStart") LocalDateTime thirtyDaysStart,
                                                     @Param("statTime") LocalDateTime statTime);
}
