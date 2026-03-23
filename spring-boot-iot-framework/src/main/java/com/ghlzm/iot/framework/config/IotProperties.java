package com.ghlzm.iot.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private Cors cors = new Cors();
    private Rule rule = new Rule();
    private Alarm alarm = new Alarm();
    private Observability observability = new Observability();
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
        private Security security = new Security();
        private Crypto crypto = new Crypto();

        @Data
        public static class Security {
            /**
             * 默认签名算法。
             * 当前支持 AES 和 MD5，其中 MD5 仅做兼容摘要，不是可逆加密。
             */
            private String defaultSignAlgorithm = "AES";
            /**
             * 时间戳允许偏差，单位秒。
             */
            private Integer allowedTimestampSkewSeconds = 300;
            /**
             * 防重放时间窗，单位秒。
             */
            private Integer replayWindowSeconds = 600;
            /**
             * 防重放 Redis Key 前缀。
             */
            private String replayKeyPrefix = "iot:protocol:replay:";
        }

        @Data
        public static class Crypto {
            /**
             * 多厂商、多算法协议安全配置。
             * key/iv 默认按 Base64 处理，避免直接在配置中出现不可见字节。
             */
            private Map<String, Merchant> merchants = new LinkedHashMap<>();

            @Data
            public static class Merchant {
                /**
                 * 解密算法编码，例如 DES / DESede。
                 * AES 仍优先走 spring.cloud.aes 的现有实现。
                 */
                private String algorithm;
                /**
                 * Base64 编码的密钥。
                 */
                private String key;
                /**
                 * Base64 编码的 IV，可选。
                 */
                private String iv;
                /**
                 * JCE transformation，例如 DES/CBC/PKCS5Padding。
                 */
                private String transformation;
                /**
                 * MD5 等签名场景使用的共享密钥，缺省时回退到 key。
                 */
                private String signatureSecret;
                /**
                 * 共享密钥与正文的拼接方式：
                 * KEY_PREFIX / KEY_SUFFIX / CONTENT_ONLY
                 */
                private String signatureJoinMode = "KEY_SUFFIX";
            }
        }
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
        /**
         * 基准站设备编码 -> 逻辑子设备编码 -> 实际子设备 deviceCode。
         * 当前用于兼容“基准站一包多测点”的历史上报场景。
         */
        private Map<String, Map<String, String>> subDeviceMappings = new LinkedHashMap<>();
    }

    @Data
    public static class Message {
        private String transport = "direct";
        private String upTopic = "iot.message.up";
        private String downTopic = "iot.message.down";
        private String eventTopic = "iot.message.event";
    }

    @Data
    public static class Cors {
        /**
         * 是否启用后端 CORS 支持。
         * 主要用于前端开发时的浏览器直连场景。
         */
        private Boolean enabled = Boolean.TRUE;
        /**
         * 允许的来源模式。
         * 使用 origin pattern 而不是 "*"，避免与 credentials 冲突。
         */
        private List<String> allowedOriginPatterns = List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        );
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        private Boolean allowCredentials = Boolean.TRUE;
        private Long maxAge = 3600L;
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
        private AutoClosure autoClosure = new AutoClosure();

        @Data
        public static class Notify {
            private Boolean emailEnabled;
            private Boolean webhookEnabled;
        }

        @Data
        public static class AutoClosure {
            private Boolean enabled = Boolean.FALSE;
            private Integer cooldownMinutes = 30;
            private BigDecimal yellow = BigDecimal.valueOf(5);
            private BigDecimal orange = BigDecimal.valueOf(10);
            private BigDecimal red = BigDecimal.valueOf(20);
        }
    }

    @Data
    public static class Observability {
        private Boolean systemErrorNotifyEnabled = Boolean.FALSE;
        private Integer notificationTimeoutMs = 3000;
        private Integer systemErrorNotifyCooldownSeconds = 300;
        private Console console = new Console();
        private InAppUnreadBridge inAppUnreadBridge = new InAppUnreadBridge();

        @Data
        public static class Console {
            private Boolean mybatisSqlEnabled = Boolean.FALSE;
            private Boolean mybatisSessionEnabled = Boolean.FALSE;
        }

        @Data
        public static class InAppUnreadBridge {
            private Boolean enabled = Boolean.FALSE;
            private Integer scanIntervalSeconds = 60;
            private Integer highThresholdMinutes = 30;
            private Integer criticalThresholdMinutes = 10;
        }
    }

    @Data
    public static class Ota {
        private Boolean enabled = Boolean.FALSE;
        /**
         * 文件/固件分包在 Redis 中的最小保留时长，单位小时。
         */
        private Integer fileSessionTtlHours = 24;
    }
}
