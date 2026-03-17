package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.service.NotificationHttpClient;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * 基于 JDK HttpClient 的通知发送实现。
 */
@Component
public class JdkNotificationHttpClient implements NotificationHttpClient {

    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @Override
    public HttpResult postJson(String url, Map<String, String> headers, String body, Duration timeout) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }
        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        return new HttpResult(response.statusCode(), response.body());
    }
}
