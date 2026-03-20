package com.ghlzm.iot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.system.entity.Dict;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典配置 Mapper
 */
@Mapper
public interface DictMapper extends BaseMapper<Dict> {
}
