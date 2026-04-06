package com.ghlzm.iot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.device.entity.RiskMetricCatalogReadModel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风险指标目录只读 Mapper。
 */
@Mapper
public interface RiskMetricCatalogReadMapper extends BaseMapper<RiskMetricCatalogReadModel> {
}
