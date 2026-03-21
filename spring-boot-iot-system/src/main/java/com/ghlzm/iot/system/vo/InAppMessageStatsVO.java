package com.ghlzm.iot.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class InAppMessageStatsVO {

    private String startTime;

    private String endTime;

    private Long totalDeliveryCount = 0L;

    private Long totalReadCount = 0L;

    private Long totalUnreadCount = 0L;

    private Double readRate = 0D;

    private List<TrendBucket> trend = new ArrayList<>();

    private List<Bucket> messageTypeBuckets = new ArrayList<>();

    private List<Bucket> sourceTypeBuckets = new ArrayList<>();

    private List<TopUnreadMessage> topUnreadMessages = new ArrayList<>();

    @Data
    public static class TrendBucket {

        private String date;

        private Long deliveryCount = 0L;

        private Long readCount = 0L;

        private Long unreadCount = 0L;
    }

    @Data
    public static class Bucket {

        private String key;

        private String label;

        private Long deliveryCount = 0L;

        private Long readCount = 0L;

        private Long unreadCount = 0L;

        private Double readRate = 0D;
    }

    @Data
    public static class TopUnreadMessage {

        private Long messageId;

        private String title;

        private String messageType;

        private String sourceType;

        private String publishTime;

        private Long deliveryCount = 0L;

        private Long readCount = 0L;

        private Long unreadCount = 0L;

        private Double unreadRate = 0D;
    }
}
