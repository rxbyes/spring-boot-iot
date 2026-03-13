package com.ghlzm.iot.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * IOT 配置类
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 13:20
 */
@Data
@Component
@ConfigurationProperties(prefix = "iot")
public class IotProperties {

    private Security security;
    private Tenant tenant;
    private Mqtt mqtt;
    private Tcp tcp;
    private Protocol protocol;
    private Telemetry telemetry;
    private Device device;
    private Message message;
    private Rule rule;
    private Alarm alarm;
    private Ota ota;

    @Data
    public static class Security {
        private String jwtSecret;
        private Integer tokenExpireSeconds;
        private String tokenHeader;
        private String tokenPrefix;
    }

    @Data
    public static class Tenant {
        private Boolean enabled;
        private Long defaultTenantId;
    }

    @Data
    public static class Mqtt {
        private Boolean enabled;
        private String brokerUrl;
        private String clientId;
        private String username;
        private String password;
        private Integer qos;
        private Boolean cleanSession;
        private Integer connectionTimeout;
        private Integer keepAliveInterval;
        private List<String> defaultSubscribeTopics;
    }

    @Data
    public static class Tcp {
        private Boolean enabled;
        private Integer port;
        private Integer bossThreads;
        private Integer workerThreads;
        private Boolean soKeepalive;
        private Integer soBacklog;
    }

    @Data
    public static class Protocol {
        private String defaultCode;
        private Integer timeoutMillis;
        private Integer retryTimes;
    }

    @Data
    public static class Telemetry {
        private String storageType;
        private String latestCachePrefix;
        private String tsPrefix;
    }

    @Data
    public static class Device {
        private Integer onlineTimeoutSeconds;
        private Boolean activateDefault;
    }

    @Data
    public static class Message {
        private String transport;
        private String upTopic;
        private String downTopic;
        private String eventTopic;
    }

    @Data
    public static class Rule {
        private Boolean enabled;
        private String expressionEngine;
    }

    @Data
    public static class Alarm {
        private Boolean enabled;
        private Notify notify;

        @Data
        public static class Notify {
            private Boolean emailEnabled;
            private Boolean webhookEnabled;
        }
    }

    @Data
    public static class Ota {
        private Boolean enabled;
    }
}

