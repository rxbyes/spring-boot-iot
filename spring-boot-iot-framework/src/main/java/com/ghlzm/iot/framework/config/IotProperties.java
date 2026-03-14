package com.ghlzm.iot.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * IOT 配置类
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 13:20
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "iot")
public class IotProperties {

    private Security security = new Security();
    private Tenant tenant = new Tenant();
    private Mqtt mqtt = new Mqtt();
    private Tcp tcp = new Tcp();
    private Protocol protocol = new Protocol();
    private Telemetry telemetry = new Telemetry();
    private Device device = new Device();
    private Message message = new Message();
    private Rule rule = new Rule();
    private Alarm alarm = new Alarm();
    private Ota ota = new Ota();

    @Data
    public static class Security {
        private String jwtSecret;
        private Integer tokenExpireSeconds;
        private String tokenHeader;
        private String tokenPrefix;
    }

    @Data
    public static class Tenant {
        private Boolean enabled = Boolean.TRUE;
        private Long defaultTenantId = 1L;
    }

    @Data
    public static class Mqtt {
        private Boolean enabled = Boolean.FALSE;
        private String brokerUrl;
        private String clientId;
        private String username;
        private String password;
        private Integer qos = 1;
        private Boolean cleanSession = Boolean.TRUE;
        private Integer connectionTimeout = 10;
        private Integer keepAliveInterval = 30;
        private List<String> defaultSubscribeTopics;
    }

    @Data
    public static class Tcp {
        private Boolean enabled = Boolean.FALSE;
        private Integer port = 18830;
        private Integer bossThreads = 1;
        private Integer workerThreads = 8;
        private Boolean soKeepalive = Boolean.TRUE;
        private Integer soBacklog = 1024;
    }

    @Data
    public static class Protocol {
        private String defaultCode = "mqtt-json";
        private Integer timeoutMillis = 5000;
        private Integer retryTimes = 3;
    }

    @Data
    public static class Telemetry {
        private String storageType = "mysql";
        private String latestCachePrefix = "iot:telemetry:latest:";
        private String tsPrefix = "iot:telemetry:ts:";
    }

    @Data
    public static class Device {
        private Integer onlineTimeoutSeconds = 120;
        private Boolean activateDefault = Boolean.TRUE;
    }

    @Data
    public static class Message {
        private String transport = "direct";
        private String upTopic = "iot.message.up";
        private String downTopic = "iot.message.down";
        private String eventTopic = "iot.message.event";
    }

    @Data
    public static class Rule {
        private Boolean enabled = Boolean.FALSE;
        private String expressionEngine = "spel";
    }

    @Data
    public static class Alarm {
        private Boolean enabled = Boolean.FALSE;
        private Notify notify = new Notify();

        @Data
        public static class Notify {
            private Boolean emailEnabled;
            private Boolean webhookEnabled;
        }
    }

    @Data
    public static class Ota {
        private Boolean enabled = Boolean.FALSE;
    }
}
