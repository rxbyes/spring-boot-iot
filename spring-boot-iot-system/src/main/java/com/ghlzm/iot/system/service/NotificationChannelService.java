package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.system.entity.NotificationChannel;

import java.util.List;

/**
 * 通知渠道 Service
 */
public interface NotificationChannelService extends IService<NotificationChannel> {

      /**
       * 添加通知渠道
       */
      NotificationChannel addChannel(NotificationChannel channel);

      /**
       * 查询通知渠道列表
       */
      List<NotificationChannel> listChannels();

      /**
       * 根据渠道编码查询通知渠道
       */
      NotificationChannel getByCode(String channelCode);

      /**
       * 更新通知渠道
       */
      void updateChannel(NotificationChannel channel);

      /**
       * 删除通知渠道
       */
      void deleteChannel(Long id);
}
