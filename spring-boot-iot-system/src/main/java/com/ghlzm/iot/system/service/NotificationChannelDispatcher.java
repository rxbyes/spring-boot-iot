package com.ghlzm.iot.system.service;

import com.ghlzm.iot.system.entity.NotificationChannel;

import java.util.List;
import java.util.Map;

public interface NotificationChannelDispatcher {

    List<DispatchChannel> listSceneChannels(String scene);
    List<DispatchChannel> listSceneChannels(String scene, String opsAlertType);

    DispatchChannel requireTestChannel(String channelCode);
    DispatchChannel requireTestChannel(Long currentUserId, String channelCode);

    DispatchResult send(DispatchChannel dispatchChannel, NotificationEnvelope envelope);

    record DispatchChannel(NotificationChannel channel, ChannelConfig config) {
    }

    record ChannelConfig(String url,
                         Map<String, String> headers,
                         List<String> scenes,
                         List<String> opsAlertTypes,
                         int timeoutMs,
                         int minIntervalSeconds) {
    }

    record NotificationEnvelope(String title,
                                String plainText,
                                String markdownText,
                                Map<String, Object> genericPayload) {
    }

    record DispatchResult(boolean success,
                          Integer statusCode,
                          String responseBody,
                          String errorMessage) {
    }
}
