package com.ghlzm.iot.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 阈值规则配置Mapper
 */
@Mapper
public interface RuleDefinitionMapper extends BaseMapper<RuleDefinition> {
}
