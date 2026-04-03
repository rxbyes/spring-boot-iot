package com.ghlzm.iot.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingPromotion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风险点设备待治理转正明细 Mapper
 */
@Mapper
public interface RiskPointDevicePendingPromotionMapper extends BaseMapper<RiskPointDevicePendingPromotion> {
}
