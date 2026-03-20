package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.NotificationChannel;

import java.util.List;

public interface NotificationChannelService extends IService<NotificationChannel> {

      NotificationChannel addChannel(NotificationChannel channel);

      List<NotificationChannel> listChannels(String channelName, String channelCode, String channelType);

      PageResult<NotificationChannel> pageChannels(String channelName, String channelCode, String channelType, Long pageNum, Long pageSize);

      NotificationChannel getByCode(String channelCode);

      void updateChannel(NotificationChannel channel);

      void deleteChannel(Long id);
}
