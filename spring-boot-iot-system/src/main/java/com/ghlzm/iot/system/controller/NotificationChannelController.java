package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.entity.NotificationChannel;
import com.ghlzm.iot.system.service.NotificationChannelService;
import com.ghlzm.iot.system.service.SystemErrorNotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/channel")
public class NotificationChannelController {

      private final NotificationChannelService notificationChannelService;
      private final SystemErrorNotificationService systemErrorNotificationService;

      public NotificationChannelController(NotificationChannelService notificationChannelService,
                                           SystemErrorNotificationService systemErrorNotificationService) {
            this.notificationChannelService = notificationChannelService;
            this.systemErrorNotificationService = systemErrorNotificationService;
      }

      @GetMapping("/list")
      public R<List<NotificationChannel>> listChannels(@RequestParam(required = false) String channelName,
                                                       @RequestParam(required = false) String channelCode,
                                                       @RequestParam(required = false) String channelType,
                                                       Authentication authentication) {
            return R.ok(notificationChannelService.listChannels(requireCurrentUserId(authentication), channelName, channelCode, channelType));
      }

      @GetMapping("/page")
      public R<PageResult<NotificationChannel>> pageChannels(@RequestParam(required = false) String channelName,
                                                             @RequestParam(required = false) String channelCode,
                                                             @RequestParam(required = false) String channelType,
                                                             @RequestParam(defaultValue = "1") Long pageNum,
                                                             @RequestParam(defaultValue = "10") Long pageSize,
                                                             Authentication authentication) {
            return R.ok(notificationChannelService.pageChannels(requireCurrentUserId(authentication), channelName, channelCode, channelType, pageNum, pageSize));
      }

      @GetMapping("/getByCode/{channelCode}")
      public R<NotificationChannel> getByCode(@PathVariable String channelCode, Authentication authentication) {
            return R.ok(notificationChannelService.getByCode(requireCurrentUserId(authentication), channelCode));
      }

      @PostMapping("/add")
      public R<NotificationChannel> addChannel(@RequestBody NotificationChannel channel, Authentication authentication) {
            return R.ok(notificationChannelService.addChannel(requireCurrentUserId(authentication), channel));
      }

      @PutMapping("/update")
      public R<Void> updateChannel(@RequestBody NotificationChannel channel, Authentication authentication) {
            notificationChannelService.updateChannel(requireCurrentUserId(authentication), channel);
            return R.ok();
      }

      @DeleteMapping("/delete/{id}")
      public R<Void> deleteChannel(@PathVariable Long id, Authentication authentication) {
            notificationChannelService.deleteChannel(requireCurrentUserId(authentication), id);
            return R.ok();
      }

      @PostMapping("/test/{channelCode}")
      public R<Void> testChannel(@PathVariable String channelCode, Authentication authentication) {
            systemErrorNotificationService.sendTestNotification(requireCurrentUserId(authentication), channelCode);
            return R.ok();
      }

      private Long requireCurrentUserId(Authentication authentication) {
            if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
                  throw new com.ghlzm.iot.common.exception.BizException(401, "未认证，请先登录");
            }
            return principal.userId();
      }
}
