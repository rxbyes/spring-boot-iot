package com.ghlzm.iot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.vo.ProductDeviceStatRow;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 13:57
 */
@Mapper
public interface DeviceMapper extends BaseMapper<Device> {

    @Select({
            "<script>",
            "SELECT",
            "  product_id AS productId,",
            "  COUNT(1) AS deviceCount,",
            "  SUM(CASE WHEN online_status = 1 THEN 1 ELSE 0 END) AS onlineDeviceCount,",
            "  MAX(last_report_time) AS lastReportTime",
            "FROM iot_device",
            "WHERE deleted = 0",
            "  AND product_id IS NOT NULL",
            "  AND product_id IN",
            "  <foreach collection='productIds' item='productId' open='(' separator=',' close=')'>",
            "    #{productId}",
            "  </foreach>",
            "GROUP BY product_id",
            "</script>"
    })
    List<ProductDeviceStatRow> selectProductStats(@Param("productIds") Collection<Long> productIds);
}
