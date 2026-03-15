package com.ghlzm.iot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.system.entity.NotificationChannel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知渠道 Mapper
 */
@Mapper
public interface NotificationChannelMapper extends BaseMapper<NotificationChannel> {
}
