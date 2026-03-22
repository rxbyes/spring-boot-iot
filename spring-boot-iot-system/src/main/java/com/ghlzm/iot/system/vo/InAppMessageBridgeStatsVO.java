package com.ghlzm.iot.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class InAppMessageBridgeStatsVO {

    private String startTime;

    private String endTime;

    private Long totalBridgeCount = 0L;

    private Long successCount = 0L;

    private Long pendingRetryCount = 0L;

    private Long totalAttemptCount = 0L;

    private Double successRate = 0D;

    private List<TrendBucket> trend = new ArrayList<>();

    private List<ChannelBucket> channelBuckets = new ArrayList<>();

    private List<SourceTypeBucket> sourceTypeBuckets = new ArrayList<>();

    @Data
    public static class TrendBucket {

        private String date;

        private Long bridgeCount = 0L;

        private Long successCount = 0L;

        private Long pendingRetryCount = 0L;

        private Long totalAttemptCount = 0L;
    }

    @Data
    public static class ChannelBucket {

        private String key;

        private String label;

        private String channelType;

        private Long bridgeCount = 0L;

        private Long successCount = 0L;

        private Long pendingRetryCount = 0L;

        private Double successRate = 0D;
    }

    @Data
    public static class SourceTypeBucket {

        private String key;

        private String label;

        private Long bridgeCount = 0L;

        private Long successCount = 0L;

        private Long pendingRetryCount = 0L;

        private Double successRate = 0D;
    }
}
