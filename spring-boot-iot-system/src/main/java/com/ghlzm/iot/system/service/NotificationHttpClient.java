package com.ghlzm.iot.system.service;

import java.time.Duration;
import java.util.Map;

/**
 * 通知 HTTP 客户端抽象。
 */
public interface NotificationHttpClient {

    HttpResult postJson(String url, Map<String, String> headers, String body, Duration timeout) throws Exception;

    record HttpResult(int statusCode, String responseBody) {
    }
}
