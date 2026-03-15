package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.system.entity.NotificationChannel;
import com.ghlzm.iot.system.service.NotificationChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知渠道 Controller
 */
@RestController
@RequestMapping("/system/channel")
public class NotificationChannelController {

      @Autowired
      private NotificationChannelService notificationChannelService;

      /**
       * 查询通知渠道列表
       */
      @GetMapping("/list")
      public R<List<NotificationChannel>> listChannels() {
            List<NotificationChannel> channels = notificationChannelService.listChannels();
            return R.ok(channels);
      }

      /**
       * 根据渠道编码查询通知渠道
       */
      @GetMapping("/getByCode/{channelCode}")
      public R<NotificationChannel> getByCode(@PathVariable String channelCode) {
            NotificationChannel channel = notificationChannelService.getByCode(channelCode);
            return R.ok(channel);
      }

      /**
       * 添加通知渠道
       */
      @PostMapping("/add")
      public R<NotificationChannel> addChannel(@RequestBody NotificationChannel channel) {
            NotificationChannel result = notificationChannelService.addChannel(channel);
            return R.ok(result);
      }

      /**
       * 更新通知渠道
       */
      @PutMapping("/update")
      public R<Void> updateChannel(@RequestBody NotificationChannel channel) {
            notificationChannelService.updateChannel(channel);
            return R.ok();
      }

      /**
       * 删除通知渠道
       */
      @DeleteMapping("/delete/{id}")
      public R<Void> deleteChannel(@PathVariable Long id) {
            notificationChannelService.deleteChannel(id);
            return R.ok();
      }
}
