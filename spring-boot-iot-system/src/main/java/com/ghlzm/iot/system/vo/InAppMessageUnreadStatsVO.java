package com.ghlzm.iot.system.vo;

import lombok.Data;

@Data
public class InAppMessageUnreadStatsVO {

    private Long totalUnreadCount = 0L;

    private Long systemUnreadCount = 0L;

    private Long businessUnreadCount = 0L;

    private Long errorUnreadCount = 0L;
}
